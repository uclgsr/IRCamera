import asyncio
import time
import uuid
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional, Set, Callable

try:
    except ImportError:
    
from .config import config
from ..network.server import NetworkServer, DeviceInfo


class SessionState(Enum):
    IDLE = "idle"
    PREPARING = "preparing"
    RECORDING = "recording"
    STOPPING = "stopping"
    COMPLETED = "completed"
    ERROR = "error"


class SyncMarkerType(Enum):
    SESSION_START = "session_start"
    SESSION_END = "session_end"
    FLASH_SYNC = "flash_sync"
    CUSTOM_EVENT = "custom_event"
    PAUSE_MARKER = "pause_marker"
    RESUME_MARKER = "resume_marker"


@dataclass
class RecordingSession:
    session_id: str
    session_name: str
    start_time: float
    state: SessionState = SessionState.IDLE

    participating_devices: Set[str] = field(default_factory=set)
    synchronized_devices: Set[str] = field(default_factory=set)

    participant_id: Optional[str] = None
    experiment_type: Optional[str] = None
    notes: Optional[str] = None

    sync_markers: List[Dict[str, Any]] = field(default_factory=list)
    session_duration: Optional[float] = None
    end_time: Optional[float] = None

    sync_quality_stats: Dict[str, Any] = field(default_factory=dict)
    data_collection_stats: Dict[str, Any] = field(default_factory=dict)


class HubCoordinator:

    def __init__(self):

        self._network_server = NetworkServer()
        self._is_running = False

        self._active_sessions: Dict[str, RecordingSession] = {}
        self._session_history: List[str] = []

        self._sync_tolerance_ms = config.get("sync.tolerance_ms", 5.0)
        self._min_sync_quality = config.get("sync.min_quality", "GOOD")
        self._sync_check_interval = config.get("sync.check_interval_s", 10.0)

        self._sync_monitor_task: Optional[asyncio.Task] = None

        self._session_callbacks: Dict[str, List[Callable]] = {
            "session_started": [],
            "session_stopped": [],
            "device_sync_lost": [],
            "sync_marker_created": []
        }

        self._setup_network_callbacks()

        
    def _setup_network_callbacks(self) -> None:

        self._network_server.set_device_connected_callback(self._on_device_connected)
        self._network_server.set_device_disconnected_callback(self._on_device_disconnected)
        self._network_server.set_device_status_update_callback(self._on_device_status_update)

    async def start(self) -> bool:

        if self._is_running:
                        return True

        try:

            if not await self._network_server.start():
                                return False

            self._sync_monitor_task = asyncio.create_task(self._monitor_synchronization())

            self._is_running = True

                        
            return True

        except Exception as e:
                        await self.stop()
            return False

    async def stop(self) -> None:

        if not self._is_running:
            return

                self._is_running = False

        for session_id in list(self._active_sessions.keys()):
            try:
                await self.stop_recording_session(session_id)
            except Exception as e:
                
        if self._sync_monitor_task:
            self._sync_monitor_task.cancel()
            try:
                await self._sync_monitor_task
            except asyncio.CancelledError:
                pass

        await self._network_server.stop()

        
    async def start_recording_session(
            self,
            session_name: str,
            participant_id: Optional[str] = None,
            experiment_type: Optional[str] = None,
            notes: Optional[str] = None,
            target_devices: Optional[List[str]] = None
    ) -> Optional[str]:

        try:
            session_id = str(uuid.uuid4())

            logger.info(f"Starting recording session '{session_name}' (ID: {session_id})")

            connected_devices = self._network_server.get_connected_devices()
            if target_devices:
                participating_devices = {
                    did for did in target_devices
                    if did in connected_devices
                }
            else:
                participating_devices = set(connected_devices.keys())

            if not participating_devices:
                                return None

            logger.info(f"Session will include {len(participating_devices)} devices: " +
                        f"{list(participating_devices)}")

            sync_ready_devices = set()
            for device_id in participating_devices:
                if self._network_server.is_device_time_synchronized(device_id):
                    sync_ready_devices.add(device_id)
                else:
                    
            if len(sync_ready_devices) < len(participating_devices):
                logger.warning(f"Only {len(sync_ready_devices)}/{len(participating_devices)} " +
                               "devices are properly synchronized")

                if not sync_ready_devices:
                                        return None

            session = RecordingSession(
                session_id=session_id,
                session_name=session_name,
                start_time=time.time(),
                state=SessionState.PREPARING,
                participating_devices=participating_devices,
                synchronized_devices=sync_ready_devices,
                participant_id=participant_id,
                experiment_type=experiment_type,
                notes=notes
            )

            self._active_sessions[session_id] = session

            await asyncio.gather(*(
                self._network_server.register_time_sync_session(session_id, device_id)
                for device_id in participating_devices
            ))

            session.state = SessionState.RECORDING
            results = await self._network_server.start_recording_session(session_id, session_name)

            successful_devices = {
                device_id for device_id, success in results.items()
                if success
            }

            if not successful_devices:
                                session.state = SessionState.ERROR
                return None

            if len(successful_devices) < len(participating_devices):
                logger.warning(
                    f"Recording started on {len(successful_devices)}/{len(participating_devices)} devices")

                session.participating_devices = successful_devices

            await self.create_sync_marker(session_id, SyncMarkerType.SESSION_START, {
                "session_name": session_name,
                "participant_id": participant_id,
                "participating_devices": list(successful_devices),
                "synchronized_devices": list(sync_ready_devices)
            })

            logger.info(f"Recording session '{session_name}' started successfully " +
                        f"on {len(successful_devices)} devices")

            self._trigger_session_callback("session_started", session)

            return session_id

        except Exception as e:
                        return None

    async def stop_recording_session(self, session_id: str) -> bool:

        try:
            session = self._active_sessions.get(session_id)
            if not session:
                                return False

            if session.state != SessionState.RECORDING:
                                return False

            logger.info(f"Stopping recording session '{session.session_name}' (ID: {session_id})")

            session.state = SessionState.STOPPING

            await self.create_sync_marker(session_id, SyncMarkerType.SESSION_END, {
                "session_duration": time.time() - session.start_time,
                "total_sync_markers": len(session.sync_markers)
            })

            results = await self._network_server.stop_recording_session(session_id)

            sum(1 for success in results.values() if success)

            session.end_time = time.time()
            session.session_duration = session.end_time - session.start_time
            session.state = SessionState.COMPLETED

            session.sync_quality_stats = self._network_server.get_time_sync_stats()

            await self._network_server.end_time_sync_session(session_id)

            self._session_history.append(session_id)

            logger.info(f"Recording session '{session.session_name}' stopped successfully " +
                        f"(duration: {session.session_duration:.1f}s, sync_markers: {len(session.sync_markers)})")

            self._trigger_session_callback("session_stopped", session)

            return True

        except Exception as e:
                        return False

    async def create_sync_marker(
            self,
            session_id: str,
            marker_type: SyncMarkerType,
            metadata: Dict[str, Any] = None
    ) -> Optional[str]:

        try:
            session = self._active_sessions.get(session_id)
            if not session:
                                return None

            marker_id = str(uuid.uuid4())
            timestamp = time.time_ns()

            marker = {
                "marker_id": marker_id,
                "marker_type": marker_type.value,
                "timestamp_ns": timestamp,
                "session_time": timestamp - (session.start_time * 1_000_000_000),
                "metadata": metadata or {}
            }

            results = await self._network_server.send_sync_mark(
                marker_type.value,
                {**marker, "session_id": session_id}
            )

            session.sync_markers.append(marker)

            successful_marks = sum(1 for success in results.values() if success)

            logger.info(f"Sync marker '{marker_type.value}' created for session {session_id} " +
                        f"(delivered to {successful_marks}/{len(session.participating_devices)} devices)")

            self._trigger_session_callback("sync_marker_created", marker)

            return marker_id

        except Exception as e:
                        return None

    async def send_flash_sync(self, session_id: str, duration_ms: int = 100) -> bool:

        try:
            session = self._active_sessions.get(session_id)
            if not session:
                                return False

            results = await self._network_server.send_sync_flash(duration_ms)

            await self.create_sync_marker(session_id, SyncMarkerType.FLASH_SYNC, {
                "duration_ms": duration_ms,
                "devices_flashed": list(results.keys())
            })

            successful_flashes = sum(1 for success in results.values() if success)

            logger.info(
                f"Flash sync sent to {successful_flashes}/{len(session.participating_devices)} " +
                f"devices in session {session_id}")

            return successful_flashes > 0

        except Exception as e:
                        return False

    async def _monitor_synchronization(self) -> None:

        while self._is_running:
            try:
                await asyncio.sleep(self._sync_check_interval)

                connected_devices = self._network_server.get_connected_devices()

                for device_id in connected_devices:
                    sync_stats = self._network_server.get_time_sync_stats(device_id)

                    if sync_stats:

                        if not sync_stats.get("is_synchronized", False):
                                                        self._trigger_session_callback("device_sync_lost", {
                                "device_id": device_id,
                                "sync_stats": sync_stats
                            })

                        quality = sync_stats.get("sync_quality", "UNKNOWN")
                        if quality in ["POOR", "STALE"]:
                            offset_ms = sync_stats.get("median_offset_ms", 0)
                            
            except asyncio.CancelledError:
                break
            except Exception as e:
                
    def _on_device_connected(self, device_info: DeviceInfo) -> None:

        logger.info(f"Device connected: {device_info.device_id} ({device_info.device_type})")

    def _on_device_disconnected(self, device_info: DeviceInfo) -> None:

        
        for session in self._active_sessions.values():
            if device_info.device_id in session.participating_devices:
                
    def _on_device_status_update(self, device_info: DeviceInfo) -> None:

        
    def _trigger_session_callback(self, event_type: str, data: Any) -> None:

        try:
            for callback in self._session_callbacks.get(event_type, []):
                callback(data)
        except Exception as e:
            
    def get_connected_devices(self) -> Dict[str, DeviceInfo]:

        return self._network_server.get_connected_devices()

    def get_active_sessions(self) -> Dict[str, RecordingSession]:

        return self._active_sessions.copy()

    def get_session(self, session_id: str) -> Optional[RecordingSession]:

        return self._active_sessions.get(session_id)

    def get_sync_quality_summary(self) -> Dict[str, Any]:

        return self._network_server.get_time_sync_stats()

    def get_device_sync_stats(self, device_id: str) -> Dict[str, Any]:

        return self._network_server.get_time_sync_stats(device_id)

    def is_device_synchronized(self, device_id: str) -> bool:

        return self._network_server.is_device_time_synchronized(device_id)

    def add_session_callback(self, event_type: str, callback: Callable) -> None:

        if event_type in self._session_callbacks:
            self._session_callbacks[event_type].append(callback)

    def remove_session_callback(self, event_type: str, callback: Callable) -> None:

        if event_type in self._session_callbacks:
            try:
                self._session_callbacks[event_type].remove(callback)
            except ValueError:
                pass

    @property
    def is_running(self) -> bool:

        return self._is_running
