"""
Enhanced Session Manager for IRCamera PC Controller

Manages session lifecycle with device coordination according to Hub-and-Spoke architecture.
Implements session lifecycle management from the PC Hub Application MVP checklist.
"""

import asyncio
import json
import uuid
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from .config import config
from .device_manager import DeviceConnectionState, DeviceInfo, DeviceManager, DeviceRegistry


class SessionState(Enum):
    """Enhanced session states for the Hub-and-Spoke model."""

    IDLE = "idle"
    INITIALIZING = "initializing"
    ACTIVE = "active"
    RECORDING = "recording"
    STOPPING = "stopping"
    STOPPED = "stopped"
    COMPLETE = "complete"
    ERROR = "error"


@dataclass
class SessionConfiguration:
    """Configuration for a recording session."""

    session_name: str
    participant_id: Optional[str] = None
    recording_duration: Optional[int] = None  # seconds
    modalities: List[str] = field(default_factory=lambda: ["rgb", "thermal", "gsr"])
    quality_settings: Dict[str, Any] = field(default_factory=dict)
    sync_flash_enabled: bool = True
    auto_start_recording: bool = False
    notes: str = ""


@dataclass
class SessionMetadata:
    """Comprehensive session metadata structure."""

    session_id: str
    session_name: str
    state: SessionState
    configuration: SessionConfiguration

    # Timestamps
    created_at: datetime
    started_at: Optional[datetime] = None
    stopped_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None

    # Participants
    participating_devices: List[str] = field(default_factory=list)
    device_capabilities: Dict[str, Dict[str, Any]] = field(default_factory=dict)

    # Recording info
    duration_seconds: Optional[float] = None
    files_generated: List[Dict[str, Any]] = field(default_factory=list)
    sync_events: List[Dict[str, Any]] = field(default_factory=list)

    # Status tracking
    devices_acknowledged_start: List[str] = field(default_factory=list)
    devices_acknowledged_stop: List[str] = field(default_factory=list)
    devices_failed: List[Dict[str, Any]] = field(default_factory=list)

    # Metadata
    total_data_size: int = 0
    error_log: List[Dict[str, Any]] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        result = asdict(self)

        # Convert datetime objects to ISO format
        result['created_at'] = self.created_at.isoformat()
        if self.started_at:
            result['started_at'] = self.started_at.isoformat()
        if self.stopped_at:
            result['stopped_at'] = self.stopped_at.isoformat()
        if self.completed_at:
            result['completed_at'] = self.completed_at.isoformat()

        # Convert enum to string
        result['state'] = self.state.value

        return result


class EnhancedSessionManager:
    """
    Enhanced session manager with device coordination.
    
    Implements session lifecycle management requirements:
    - Session creation and initialization
    - Start/stop recording workflows  
    - Device acknowledgment handling
    - Session metadata management
    - Error handling and recovery
    """

    # Constants
    MIN_SUCCESS_RATE_FOR_START = 0.5  # Minimum success rate (50%) to consider start successful
    MIN_SUCCESS_RATE_FOR_STOP = 0.5  # Minimum success rate (50%) to consider stop successful

    def __init__(self, device_manager: DeviceManager, base_session_dir: Path):
        """
        Initialize session manager.
        
        Args:
            device_manager: Device manager instance
            base_session_dir: Base directory for session storage
        """
        self.device_manager = device_manager
        self.base_session_dir = Path(base_session_dir)
        self.base_session_dir.mkdir(parents=True, exist_ok=True)

        # Session state
        self.current_session: Optional[SessionMetadata] = None
        self.session_history: Dict[str, SessionMetadata] = {}

        # Callbacks
        self._state_callbacks: List[Callable[[SessionState, Optional[SessionMetadata]], None]] = []
        self._device_ack_callbacks: List[Callable[[str, str, bool], None]] = []

        # Acknowledgment tracking
        self._pending_acknowledgments: Dict[str, Dict[str, Any]] = {}
        self._ack_timeout = 10.0  # seconds

        # Recovery settings
        self._max_start_retries = 3
        self._max_stop_retries = 2

    def add_state_callback(self, callback: Callable[
        [SessionState, Optional[SessionMetadata]], None]) -> None:
        """Add callback for session state changes."""
        self._state_callbacks.append(callback)

    def remove_state_callback(self, callback: Callable[
        [SessionState, Optional[SessionMetadata]], None]) -> None:
        """Remove session state callback."""
        if callback in self._state_callbacks:
            self._state_callbacks.remove(callback)

    def add_device_ack_callback(self, callback: Callable[[str, str, bool], None]) -> None:
        """Add callback for device acknowledgments (device_id, command, success)."""
        self._device_ack_callbacks.append(callback)

    def _notify_state_callbacks(self, state: SessionState,
                                session: Optional[SessionMetadata] = None) -> None:
        """Notify all callbacks of session state change."""
        for callback in self._state_callbacks:
            try:
                callback(state, session or self.current_session)
            except Exception as e:
                logger.error(f"Error in session state callback: {e}")

    def _notify_ack_callbacks(self, device_id: str, command: str, success: bool) -> None:
        """Notify all callbacks of device acknowledgment."""
        for callback in self._device_ack_callbacks:
            try:
                callback(device_id, command, success)
            except Exception as e:
                logger.error(f"Error in device ack callback: {e}")

    def create_session(
            self,
            session_name: str,
            configuration: Optional[SessionConfiguration] = None
    ) -> str:
        """
        Create a new recording session.
        
        Args:
            session_name: Name for the session
            configuration: Session configuration (optional)
            
        Returns:
            Session ID
            
        Raises:
            RuntimeError: If session is already active
        """
        if self.current_session and self.current_session.state in [
            SessionState.ACTIVE,
            SessionState.RECORDING,
            SessionState.INITIALIZING
        ]:
            raise RuntimeError("Cannot create session: another session is active")

        # Generate session ID and setup configuration
        session_id = f"session_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S')}_{uuid.uuid4().hex[:8]}"

        if configuration is None:
            configuration = SessionConfiguration(session_name=session_name)

        # Create session metadata
        session_metadata = SessionMetadata(
            session_id=session_id,
            session_name=session_name,
            state=SessionState.INITIALIZING,
            configuration=configuration,
            created_at=datetime.now(timezone.utc)
        )

        # Create session directory
        session_dir = self.base_session_dir / session_id
        session_dir.mkdir(parents=True, exist_ok=True)

        # Store session
        self.current_session = session_metadata
        self.session_history[session_id] = session_metadata

        # Log session creation
        logger.info(f"Created session: {session_id} ({session_name})")
        self._log_session_event("session_created", {"session_id": session_id, "name": session_name})

        # Transition to ACTIVE state
        self._update_session_state(SessionState.ACTIVE)

        # Save initial metadata
        self._save_session_metadata()

        return session_id

    async def start_recording(self, device_filter: Optional[List[str]] = None) -> bool:
        """
        Start recording on all connected devices.
        
        Args:
            device_filter: Optional list of specific device IDs to record with
            
        Returns:
            True if recording started successfully
            
        Raises:
            RuntimeError: If no session is active or no devices available
        """
        if not self.current_session:
            raise RuntimeError("No active session to start recording")

        if self.current_session.state != SessionState.ACTIVE:
            raise RuntimeError(f"Cannot start recording in state: {self.current_session.state}")

        # Get available devices
        available_devices = self.device_manager.get_registry().get_online_devices()

        if device_filter:
            available_devices = [d for d in available_devices if d.device_id in device_filter]

        if not available_devices:
            raise RuntimeError("No online devices available for recording")

        # Update participating devices
        self.current_session.participating_devices = [d.device_id for d in available_devices]
        self.current_session.device_capabilities = {
            d.device_id: asdict(d.capabilities) for d in available_devices
        }

        # Clear previous acknowledgments
        self.current_session.devices_acknowledged_start.clear()
        self.current_session.devices_failed.clear()

        logger.info(
            f"Starting recording for session {self.current_session.session_id} "
            f"with {len(available_devices)} devices"
        )

        # Update state to recording
        self._update_session_state(SessionState.RECORDING)
        self.current_session.started_at = datetime.now(timezone.utc)

        # TODO: Send start recording commands to devices
        # This would integrate with the network server to send JSON commands
        success = await self._send_start_commands_to_devices(available_devices)

        if success:
            self._log_session_event("recording_started", {
                "device_count": len(available_devices),
                "devices": [d.device_id for d in available_devices]
            })
            self._save_session_metadata()
            return True
        else:
            # Rollback on failure
            self._update_session_state(SessionState.ACTIVE)
            self.current_session.started_at = None
            self._log_session_event("recording_start_failed", {})
            return False

    async def stop_recording(self) -> bool:
        """
        Stop recording on all devices.
        
        Returns:
            True if recording stopped successfully
        """
        if not self.current_session:
            logger.warning("No active session to stop recording")
            return False

        if self.current_session.state != SessionState.RECORDING:
            logger.warning(f"Cannot stop recording in state: {self.current_session.state}")
            return False

        logger.info(f"Stopping recording for session {self.current_session.session_id}")

        # Update state
        self._update_session_state(SessionState.STOPPING)

        # Clear previous stop acknowledgments  
        self.current_session.devices_acknowledged_stop.clear()

        # TODO: Send stop recording commands to devices
        success = await self._send_stop_commands_to_devices()

        # Always mark as stopped (even if some devices failed to respond)
        self.current_session.stopped_at = datetime.now(timezone.utc)

        if self.current_session.started_at:
            duration = (
                        self.current_session.stopped_at - self.current_session.started_at).total_seconds()
            self.current_session.duration_seconds = duration

        self._update_session_state(SessionState.STOPPED)

        if success:
            self._log_session_event("recording_stopped", {})
        else:
            self._log_session_event("recording_stop_partial_failure", {})

        self._save_session_metadata()
        return success

    def finalize_session(self) -> bool:
        """
        Finalize the current session.
        
        Returns:
            True if finalization successful
        """
        if not self.current_session:
            logger.warning("No active session to finalize")
            return False

        if self.current_session.state not in [SessionState.STOPPED, SessionState.ERROR]:
            logger.warning(f"Cannot finalize session in state: {self.current_session.state}")
            return False

        logger.info(f"Finalizing session {self.current_session.session_id}")

        # Mark completion
        self.current_session.completed_at = datetime.now(timezone.utc)
        self._update_session_state(SessionState.COMPLETE)

        # Log finalization
        self._log_session_event("session_finalized", {
            "duration": self.current_session.duration_seconds,
            "devices": len(self.current_session.participating_devices),
            "files": len(self.current_session.files_generated)
        })

        # Save final metadata
        self._save_session_metadata()

        return True

    def reset_for_next_session(self) -> None:
        """Reset session manager for next session."""
        if self.current_session and self.current_session.state not in [
            SessionState.COMPLETE, SessionState.ERROR
        ]:
            logger.warning("Resetting session manager with active session")

        self.current_session = None
        self._pending_acknowledgments.clear()

        logger.info("Session manager reset for next session")

    async def _send_start_commands_to_devices(self, devices: List[DeviceInfo]) -> bool:
        """
        Send start recording commands to devices.
        
        Args:
            devices: List of devices to send commands to
            
        Returns:
            True if all devices acknowledged successfully
        """
        # TODO: Integrate with network server to send actual JSON commands
        # For now, simulate the process

        success_count = 0

        for device in devices:
            try:
                # Simulate command sending and acknowledgment
                await asyncio.sleep(0.1)  # Simulate network delay

                # Mark device as acknowledged (simulation)
                self.current_session.devices_acknowledged_start.append(device.device_id)

                # Update device state in registry
                self.device_manager.get_registry().update_device_state(
                    device.device_id, DeviceConnectionState.RECORDING
                )

                success_count += 1
                logger.debug(f"Start command acknowledged by device: {device.device_id}")

                self._notify_ack_callbacks(device.device_id, "start_recording", True)

            except Exception as e:
                logger.error(f"Failed to send start command to device {device.device_id}: {e}")
                self.current_session.devices_failed.append({
                    "device_id": device.device_id,
                    "command": "start_recording",
                    "error": str(e),
                    "timestamp": datetime.now(timezone.utc).isoformat()
                })
                self._notify_ack_callbacks(device.device_id, "start_recording", False)

        success_rate = success_count / len(devices) if devices else 0
        logger.info(
            f"Start command success rate: {success_count}/{len(devices)} ({success_rate:.1%})")

        # Consider success if at least minimum threshold of devices responded
        return success_rate >= self.MIN_SUCCESS_RATE_FOR_START

    async def _send_stop_commands_to_devices(self) -> bool:
        """
        Send stop recording commands to devices.
        
        Returns:
            True if all devices acknowledged successfully
        """
        if not self.current_session.participating_devices:
            return True

        # TODO: Integrate with network server to send actual JSON commands
        # For now, simulate the process

        success_count = 0

        for device_id in self.current_session.participating_devices:
            try:
                # Simulate command sending and acknowledgment
                await asyncio.sleep(0.1)  # Simulate network delay

                # Mark device as acknowledged (simulation)
                self.current_session.devices_acknowledged_stop.append(device_id)

                # Update device state in registry
                self.device_manager.get_registry().update_device_state(
                    device_id, DeviceConnectionState.ONLINE
                )

                success_count += 1
                logger.debug(f"Stop command acknowledged by device: {device_id}")

                self._notify_ack_callbacks(device_id, "stop_recording", True)

            except Exception as e:
                logger.error(f"Failed to send stop command to device {device_id}: {e}")
                self.current_session.devices_failed.append({
                    "device_id": device_id,
                    "command": "stop_recording",
                    "error": str(e),
                    "timestamp": datetime.now(timezone.utc).isoformat()
                })
                self._notify_ack_callbacks(device_id, "stop_recording", False)

        device_count = len(self.current_session.participating_devices)
        success_rate = success_count / device_count if device_count else 0
        logger.info(
            f"Stop command success rate: {success_count}/{device_count} ({success_rate:.1%})")

        # Consider success if at least minimum threshold of devices responded
        return success_rate >= self.MIN_SUCCESS_RATE_FOR_STOP

    def _update_session_state(self, new_state: SessionState) -> None:
        """Update session state and notify callbacks."""
        if not self.current_session:
            return

        old_state = self.current_session.state
        self.current_session.state = new_state

        logger.info(
            f"Session {self.current_session.session_id} state: {old_state.value} -> {new_state.value}")
        self._notify_state_callbacks(new_state)

    def _log_session_event(self, event_type: str, data: Dict[str, Any]) -> None:
        """Log session event to metadata."""
        if not self.current_session:
            return

        event = {
            "type": event_type,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "data": data
        }

        # Fix: Use sync_events directly instead of dynamically adding event_log
        self.current_session.sync_events.append(event)

    def _save_session_metadata(self) -> None:
        """Save session metadata to file."""
        if not self.current_session:
            return

        try:
            session_dir = self.base_session_dir / self.current_session.session_id
            metadata_file = session_dir / "session_metadata.json"

            with open(metadata_file, 'w') as f:
                json.dump(self.current_session.to_dict(), f, indent=2)

            logger.debug(f"Saved session metadata: {metadata_file}")

        except Exception as e:
            logger.error(f"Failed to save session metadata: {e}")

    # Getters and Status Methods

    def get_current_session(self) -> Optional[SessionMetadata]:
        """Get current session metadata."""
        return self.current_session

    def get_session_history(self) -> Dict[str, SessionMetadata]:
        """Get all session history."""
        return self.session_history.copy()

    def get_session_by_id(self, session_id: str) -> Optional[SessionMetadata]:
        """Get session by ID."""
        return self.session_history.get(session_id)

    def is_session_active(self) -> bool:
        """Check if a session is currently active."""
        return (self.current_session is not None and
                self.current_session.state in [SessionState.ACTIVE, SessionState.RECORDING,
                                               SessionState.STOPPING])

    def is_recording(self) -> bool:
        """Check if currently recording."""
        return (self.current_session is not None and
                self.current_session.state == SessionState.RECORDING)

    def get_session_directory(self, session_id: Optional[str] = None) -> Optional[Path]:
        """
        Get session directory path.
        
        Args:
            session_id: Session ID (uses current session if None)
            
        Returns:
            Session directory path or None if not found
        """
        if session_id is None:
            if not self.current_session:
                return None
            session_id = self.current_session.session_id

        return self.base_session_dir / session_id

    def get_session_stats(self) -> Dict[str, Any]:
        """Get session statistics."""
        stats = {
            "total_sessions": len(self.session_history),
            "current_session_id": self.current_session.session_id if self.current_session else None,
            "current_state": self.current_session.state.value if self.current_session else None,
            "sessions_by_state": {}
        }

        # Count sessions by state
        for session in self.session_history.values():
            state = session.state.value
            stats["sessions_by_state"][state] = stats["sessions_by_state"].get(state, 0) + 1

        return stats
