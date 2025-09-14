"""
Hub Coordinator for Multi-Modal Physiological Sensing Platform

Central coordinator for the Hub-and-Spoke architecture, managing device
synchronization, session coordination, and data aggregation across Android
Sensor Nodes (Spokes) from the PC Controller (Hub).
"""

import asyncio
import json
import time
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional, Set, Callable

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from .config import config
from ..network.server import NetworkServer, DeviceInfo
from ..sync import EnhancedTimeSyncService


class SessionState(Enum):
    """Recording session states."""
    IDLE = "idle"
    PREPARING = "preparing"
    RECORDING = "recording"
    STOPPING = "stopping"
    COMPLETED = "completed"
    ERROR = "error"


class SyncMarkerType(Enum):
    """Types of synchronization markers."""
    SESSION_START = "session_start"
    SESSION_END = "session_end"
    FLASH_SYNC = "flash_sync"
    CUSTOM_EVENT = "custom_event"
    PAUSE_MARKER = "pause_marker"
    RESUME_MARKER = "resume_marker"


@dataclass
class RecordingSession:
    """Recording session information."""
    
    session_id: str
    session_name: str
    start_time: float
    state: SessionState = SessionState.IDLE
    
    # Device participation
    participating_devices: Set[str] = field(default_factory=set)
    synchronized_devices: Set[str] = field(default_factory=set)
    
    # Session metadata
    participant_id: Optional[str] = None
    experiment_type: Optional[str] = None
    notes: Optional[str] = None
    
    # Timing and sync
    sync_markers: List[Dict[str, Any]] = field(default_factory=list) 
    session_duration: Optional[float] = None
    end_time: Optional[float] = None
    
    # Quality metrics
    sync_quality_stats: Dict[str, Any] = field(default_factory=dict)
    data_collection_stats: Dict[str, Any] = field(default_factory=dict)


class HubCoordinator:
    """
    Central Hub Coordinator for Multi-Modal Physiological Sensing Platform.
    
    Coordinates the Hub-and-Spoke architecture by:
    - Managing device discovery and connection
    - Synchronizing time across all devices with <5ms accuracy
    - Orchestrating recording sessions across multiple spokes
    - Ensuring data temporal alignment and quality
    - Providing session management and monitoring
    """
    
    def __init__(self):
        """Initialize hub coordinator."""
        self._network_server = NetworkServer()
        self._is_running = False
        
        # Session management
        self._active_sessions: Dict[str, RecordingSession] = {}
        self._session_history: List[str] = []  # Keep session IDs for history
        
        # Synchronization requirements
        self._sync_tolerance_ms = config.get("sync.tolerance_ms", 5.0)
        self._min_sync_quality = config.get("sync.min_quality", "GOOD")
        self._sync_check_interval = config.get("sync.check_interval_s", 10.0)
        
        # Monitoring
        self._sync_monitor_task: Optional[asyncio.Task] = None
        
        # Event callbacks
        self._session_callbacks: Dict[str, List[Callable]] = {
            "session_started": [],
            "session_stopped": [],
            "device_sync_lost": [],
            "sync_marker_created": []
        }
        
        self._setup_network_callbacks()
        
        logger.info("Hub Coordinator initialized")
    
    def _setup_network_callbacks(self) -> None:
        """Setup network server event callbacks."""
        self._network_server.set_device_connected_callback(self._on_device_connected)
        self._network_server.set_device_disconnected_callback(self._on_device_disconnected)
        self._network_server.set_device_status_update_callback(self._on_device_status_update)
    
    async def start(self) -> bool:
        """Start the hub coordinator."""
        if self._is_running:
            logger.warning("Hub coordinator already running")
            return True
        
        try:
            # Start network server
            if not await self._network_server.start():
                logger.error("Failed to start network server")
                return False
            
            # Start synchronization monitoring
            self._sync_monitor_task = asyncio.create_task(self._monitor_synchronization())
            
            self._is_running = True
            
            logger.info("Hub Coordinator started successfully")
            logger.info(f"Sync requirements: tolerance={self._sync_tolerance_ms}ms, " +
                       f"min_quality={self._min_sync_quality}")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start hub coordinator: {e}")
            await self.stop()
            return False
    
    async def stop(self) -> None:
        """Stop the hub coordinator."""
        if not self._is_running:
            return
        
        logger.info("Stopping hub coordinator...")
        self._is_running = False
        
        # Stop active sessions
        for session_id in list(self._active_sessions.keys()):
            try:
                await self.stop_recording_session(session_id)
            except Exception as e:
                logger.warning(f"Error stopping session {session_id}: {e}")
        
        # Cancel monitoring
        if self._sync_monitor_task:
            self._sync_monitor_task.cancel()
            try:
                await self._sync_monitor_task
            except asyncio.CancelledError:
                pass
        
        # Stop network server
        await self._network_server.stop()
        
        logger.info("Hub Coordinator stopped")
    
    async def start_recording_session(
        self,
        session_name: str,
        participant_id: Optional[str] = None,
        experiment_type: Optional[str] = None,
        notes: Optional[str] = None,
        target_devices: Optional[List[str]] = None
    ) -> Optional[str]:
        """
        Start a synchronized recording session across all or specified devices.
        
        Args:
            session_name: Human-readable session name
            participant_id: Participant identifier
            experiment_type: Type of experiment 
            notes: Additional notes
            target_devices: Specific devices to include (None = all connected)
            
        Returns:
            Session ID if successful, None otherwise
        """
        try:
            session_id = str(uuid.uuid4())
            
            logger.info(f"Starting recording session '{session_name}' (ID: {session_id})")
            
            # Get participating devices
            connected_devices = self._network_server.get_connected_devices()
            if target_devices:
                participating_devices = {
                    did for did in target_devices 
                    if did in connected_devices
                }
            else:
                participating_devices = set(connected_devices.keys())
            
            if not participating_devices:
                logger.error("No devices available for recording session")
                return None
            
            logger.info(f"Session will include {len(participating_devices)} devices: " +
                       f"{list(participating_devices)}")
            
            # Check synchronization quality
            sync_ready_devices = set()
            for device_id in participating_devices:
                if self._network_server.is_device_time_synchronized(device_id):
                    sync_ready_devices.add(device_id)
                else:
                    logger.warning(f"Device {device_id} not properly synchronized")
            
            if len(sync_ready_devices) < len(participating_devices):
                logger.warning(f"Only {len(sync_ready_devices)}/{len(participating_devices)} " +
                              "devices are properly synchronized")
                
                # Allow proceeding if we have at least one synced device
                if not sync_ready_devices:
                    logger.error("No devices are properly synchronized - cannot start session")
                    return None
            
            # Create session record
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
            
            # Register time sync sessions for all devices concurrently
            await asyncio.gather(*(
                self._network_server.register_time_sync_session(session_id, device_id)
                for device_id in participating_devices
            ))
            
            # Send session start command to devices
            session.state = SessionState.RECORDING
            results = await self._network_server.start_recording_session(session_id, session_name)
            
            # Check results
            successful_devices = {
                device_id for device_id, success in results.items()
                if success
            }
            
            if not successful_devices:
                logger.error("Failed to start recording on any device")
                session.state = SessionState.ERROR
                return None
            
            if len(successful_devices) < len(participating_devices):
                logger.warning(f"Recording started on {len(successful_devices)}/{len(participating_devices)} devices")
                # Update participating devices to only include successful ones
                session.participating_devices = successful_devices
            
            # Create session start sync marker
            await self.create_sync_marker(session_id, SyncMarkerType.SESSION_START, {
                "session_name": session_name,
                "participant_id": participant_id,
                "participating_devices": list(successful_devices),
                "synchronized_devices": list(sync_ready_devices)
            })
            
            logger.info(f"Recording session '{session_name}' started successfully " +
                       f"on {len(successful_devices)} devices")
            
            # Notify callbacks
            self._trigger_session_callback("session_started", session)
            
            return session_id
            
        except Exception as e:
            logger.error(f"Error starting recording session: {e}")
            return None
    
    async def stop_recording_session(self, session_id: str) -> bool:
        """
        Stop a recording session across all participating devices.
        
        Args:
            session_id: Session identifier
            
        Returns:
            True if successful
        """
        try:
            session = self._active_sessions.get(session_id)
            if not session:
                logger.warning(f"Session {session_id} not found")
                return False
            
            if session.state != SessionState.RECORDING:
                logger.warning(f"Session {session_id} not in recording state: {session.state}")
                return False
            
            logger.info(f"Stopping recording session '{session.session_name}' (ID: {session_id})")
            
            session.state = SessionState.STOPPING
            
            # Create session end sync marker
            await self.create_sync_marker(session_id, SyncMarkerType.SESSION_END, {
                "session_duration": time.time() - session.start_time,
                "total_sync_markers": len(session.sync_markers)
            })
            
            # Send session stop command to devices  
            results = await self._network_server.stop_recording_session(session_id)
            
            # Check results
            successful_stops = sum(1 for success in results.values() if success)
            
            # Finalize session
            session.end_time = time.time()
            session.session_duration = session.end_time - session.start_time
            session.state = SessionState.COMPLETED
            
            # Collect final sync quality stats
            session.sync_quality_stats = self._network_server.get_time_sync_stats()
            
            # End time sync sessions
            for device_id in session.participating_devices:
                await self._network_server.end_time_sync_session(session_id)
            
            # Move to history
            self._session_history.append(session_id)
            
            logger.info(f"Recording session '{session.session_name}' stopped successfully " +
                       f"(duration: {session.session_duration:.1f}s, sync_markers: {len(session.sync_markers)})")
            
            # Notify callbacks
            self._trigger_session_callback("session_stopped", session)
            
            return True
            
        except Exception as e:
            logger.error(f"Error stopping recording session {session_id}: {e}")
            return False
    
    async def create_sync_marker(
        self,
        session_id: str,
        marker_type: SyncMarkerType,
        metadata: Dict[str, Any] = None
    ) -> Optional[str]:
        """
        Create a synchronization marker across all devices in session.
        
        Args:
            session_id: Session identifier
            marker_type: Type of sync marker
            metadata: Additional marker metadata
            
        Returns:
            Marker ID if successful
        """
        try:
            session = self._active_sessions.get(session_id)
            if not session:
                logger.warning(f"Session {session_id} not found for sync marker")
                return None
            
            marker_id = str(uuid.uuid4())
            timestamp = time.time_ns()
            
            # Create sync marker record
            marker = {
                "marker_id": marker_id,
                "marker_type": marker_type.value,
                "timestamp_ns": timestamp,
                "session_time": timestamp - (session.start_time * 1_000_000_000),
                "metadata": metadata or {}
            }
            
            # Send sync marker to all devices
            results = await self._network_server.send_sync_mark(
                marker_type.value,
                {**marker, "session_id": session_id}
            )
            
            # Record marker in session
            session.sync_markers.append(marker)
            
            successful_marks = sum(1 for success in results.values() if success)
            
            logger.info(f"Sync marker '{marker_type.value}' created for session {session_id} " +
                       f"(delivered to {successful_marks}/{len(session.participating_devices)} devices)")
            
            # Notify callbacks
            self._trigger_session_callback("sync_marker_created", marker)
            
            return marker_id
            
        except Exception as e:
            logger.error(f"Error creating sync marker for session {session_id}: {e}")
            return None
    
    async def send_flash_sync(self, session_id: str, duration_ms: int = 100) -> bool:
        """
        Send flash synchronization signal to all devices in session.
        
        Args:
            session_id: Session identifier 
            duration_ms: Flash duration in milliseconds
            
        Returns:
            True if successful
        """
        try:
            session = self._active_sessions.get(session_id)
            if not session:
                logger.warning(f"Session {session_id} not found for flash sync")
                return False
            
            # Send flash command
            results = await self._network_server.send_sync_flash(duration_ms)
            
            # Create corresponding sync marker
            await self.create_sync_marker(session_id, SyncMarkerType.FLASH_SYNC, {
                "duration_ms": duration_ms,
                "devices_flashed": list(results.keys())
            })
            
            successful_flashes = sum(1 for success in results.values() if success)
            
            logger.info(f"Flash sync sent to {successful_flashes}/{len(session.participating_devices)} " +
                       f"devices in session {session_id}")
            
            return successful_flashes > 0
            
        except Exception as e:
            logger.error(f"Error sending flash sync for session {session_id}: {e}")
            return False
    
    async def _monitor_synchronization(self) -> None:
        """Monitor device synchronization quality."""
        while self._is_running:
            try:
                await asyncio.sleep(self._sync_check_interval)
                
                # Check sync quality for all connected devices
                connected_devices = self._network_server.get_connected_devices()
                
                for device_id in connected_devices:
                    sync_stats = self._network_server.get_time_sync_stats(device_id)
                    
                    if sync_stats:
                        # Check if device lost synchronization
                        if not sync_stats.get("is_synchronized", False):
                            logger.warning(f"Device {device_id} lost synchronization")
                            self._trigger_session_callback("device_sync_lost", {
                                "device_id": device_id,
                                "sync_stats": sync_stats
                            })
                        
                        # Log poor sync quality
                        quality = sync_stats.get("sync_quality", "UNKNOWN")
                        if quality in ["POOR", "STALE"]:
                            offset_ms = sync_stats.get("median_offset_ms", 0)
                            logger.warning(f"Device {device_id} sync quality {quality}: " +
                                          f"offset={offset_ms:.1f}ms")
                
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in synchronization monitoring: {e}")
    
    def _on_device_connected(self, device_info: DeviceInfo) -> None:
        """Handle device connection event."""
        logger.info(f"Device connected: {device_info.device_id} ({device_info.device_type})")
    
    def _on_device_disconnected(self, device_info: DeviceInfo) -> None:
        """Handle device disconnection event."""
        logger.warning(f"Device disconnected: {device_info.device_id}")
        
        # Check if device was part of active sessions
        for session in self._active_sessions.values():
            if device_info.device_id in session.participating_devices:
                logger.warning(f"Device {device_info.device_id} disconnected during session " +
                              f"'{session.session_name}'")
    
    def _on_device_status_update(self, device_info: DeviceInfo) -> None:
        """Handle device status update."""
        logger.debug(f"Device status update: {device_info.device_id} -> {device_info.state}")
    
    def _trigger_session_callback(self, event_type: str, data: Any) -> None:
        """Trigger session event callbacks."""
        try:
            for callback in self._session_callbacks.get(event_type, []):
                callback(data)
        except Exception as e:
            logger.error(f"Error in session callback for {event_type}: {e}")
    
    # Public interface methods
    
    def get_connected_devices(self) -> Dict[str, DeviceInfo]:
        """Get all connected devices."""
        return self._network_server.get_connected_devices()
    
    def get_active_sessions(self) -> Dict[str, RecordingSession]:
        """Get all active recording sessions."""
        return self._active_sessions.copy()
    
    def get_session(self, session_id: str) -> Optional[RecordingSession]:
        """Get specific session information."""
        return self._active_sessions.get(session_id)
    
    def get_sync_quality_summary(self) -> Dict[str, Any]:
        """Get overall synchronization quality summary."""
        return self._network_server.get_time_sync_stats()
    
    def get_device_sync_stats(self, device_id: str) -> Dict[str, Any]:
        """Get synchronization statistics for specific device."""
        return self._network_server.get_time_sync_stats(device_id)
    
    def is_device_synchronized(self, device_id: str) -> bool:
        """Check if a specific device is synchronized."""
        return self._network_server.is_device_time_synchronized(device_id)
    
    def add_session_callback(self, event_type: str, callback: Callable) -> None:
        """Add session event callback."""
        if event_type in self._session_callbacks:
            self._session_callbacks[event_type].append(callback)
    
    def remove_session_callback(self, event_type: str, callback: Callable) -> None:
        """Remove session event callback."""
        if event_type in self._session_callbacks:
            try:
                self._session_callbacks[event_type].remove(callback)
            except ValueError:
                pass
    
    @property
    def is_running(self) -> bool:
        """Check if hub coordinator is running."""
        return self._is_running