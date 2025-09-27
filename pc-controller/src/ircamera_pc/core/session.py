"""Session management system for IRCamera PC Controller Hub"""

import json
import uuid
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from .config import config


class SessionState(Enum):
    """Session states"""
    IDLE = "idle"
    ACTIVE = "active"
    RECORDING = "recording"
    STOPPING = "stopping"
    COMPLETED = "completed"
    ERROR = "error"


@dataclass
class SessionMetadata:
    """Session metadata container"""
    session_id: str
    name: str
    state: str
    created_at: str
    started_at: Optional[str] = None
    ended_at: Optional[str] = None
    duration_seconds: Optional[float] = None
    gsr_mode: str = "local"
    devices: List[Dict[str, Any]] = None
    files: List[Dict[str, Any]] = None
    sync_events: List[Dict[str, Any]] = None
    calibration_data: Dict[str, Any] = None

    def __post_init__(self):
        if self.devices is None:
            self.devices = []
        if self.files is None:
            self.files = []
        if self.sync_events is None:
            self.sync_events = []
        if self.calibration_data is None:
            self.calibration_data = {}


class SessionManager:
    """Session management for MVP implementation"""

    def __init__(self):
        self._current_session: Optional[SessionMetadata] = None
        self._session_history: List[str] = []
        self._data_root = Path(config.get("sessions.base_directory", "./sessions"))
        self._ensure_data_root()

        logger.info("Session Manager initialized")

    def _ensure_data_root(self) -> None:
        """Ensure session data root directory exists"""
        self._data_root.mkdir(parents=True, exist_ok=True)
        logger.debug(f"Session data root: {self._data_root}")

    def create_session(self, name: Optional[str] = None) -> SessionMetadata:
        """Create a new session"""
        if self._current_session and self._current_session.state in [
            SessionState.ACTIVE.value,
            SessionState.RECORDING.value,
        ]:
            raise ValueError("Cannot create new session: another session is active")

        session_id = str(uuid.uuid4())
        if name is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            name = f"session_{timestamp}"

        self._current_session = SessionMetadata(
            session_id=session_id,
            name=name,
            state=SessionState.IDLE.value,
            created_at=datetime.now(timezone.utc).isoformat(),
            gsr_mode=config.get("gsr.default_mode", "local"),
        )

        session_dir = self._get_session_directory(session_id)
        session_dir.mkdir(parents=True, exist_ok=True)

        self._save_metadata()
        self._session_history.append(session_id)

        logger.info(f"Session created: {name} [{session_id}]")
        return self._current_session

    def _get_session_directory(self, session_id: str) -> Path:
        """Get session directory path"""
        return self._data_root / session_id

    def get_session_directory(self, session_id: str) -> Path:
        """Get session directory path (public method)"""
        return self._get_session_directory(session_id)

    def _save_metadata(self) -> None:
        """Save session metadata to file"""
        if not self._current_session:
            return

        session_dir = self._get_session_directory(self._current_session.session_id)
        metadata_file = session_dir / "metadata.json"

        try:
            with open(metadata_file, "w", encoding="utf-8") as f:
                json.dump(asdict(self._current_session), f, indent=2)
            logger.debug(f"Session metadata saved: {metadata_file}")
        except Exception as e:
            logger.error(f"Failed to save metadata: {e}")

    def start_session(self) -> None:
        """Start the current session"""
        if not self._current_session:
            raise ValueError("No session to start")

        self._current_session.state = SessionState.ACTIVE.value
        self._current_session.started_at = datetime.now(timezone.utc).isoformat()
        self._save_metadata()

        logger.info(f"Session started: {self._current_session.name}")

    def get_session(self, session_id: str) -> Optional[SessionMetadata]:
        """Get session by ID"""
        if self._current_session and self._current_session.session_id == session_id:
            return self._current_session

        # Try to load from disk
        return self.load_session(session_id)

    def load_session(self, session_id: str) -> Optional[SessionMetadata]:
        """Load session from disk"""
        session_dir = self._get_session_directory(session_id)
        metadata_file = session_dir / "metadata.json"

        if not metadata_file.exists():
            return None

        try:
            with open(metadata_file, "r", encoding="utf-8") as f:
                data = json.load(f)

            # Convert dict back to SessionMetadata
            return SessionMetadata(**data)
        except Exception as e:
            logger.error(f"Failed to load session {session_id}: {e}")
            return None

    def get_current_session(self) -> Optional[SessionMetadata]:
        """Get current active session"""
        return self._current_session

    def get_session_state(self) -> Optional[str]:
        """Get current session state"""
        if self._current_session:
            return self._current_session.state
        return None

    def end_session(self) -> Optional[SessionMetadata]:
        """End current session"""
        if not self._current_session:
            return None

        self._current_session.state = SessionState.COMPLETED.value
        self._current_session.ended_at = datetime.now(timezone.utc).isoformat()

        if self._current_session.started_at:
            started = datetime.fromisoformat(self._current_session.started_at.replace('Z', '+00:00'))
            ended = datetime.fromisoformat(self._current_session.ended_at.replace('Z', '+00:00'))
            self._current_session.duration_seconds = (ended - started).total_seconds()

        self._save_metadata()

        session = self._current_session
        self._current_session = None

        logger.info(f"Session ended: {session.name}")
        return session

    def add_device(self, device_info: Dict[str, Any]) -> None:
        """Add device to current session"""
        if not self._current_session:
            raise ValueError("No active session")

        self._current_session.devices.append(device_info)
        self._save_metadata()
        logger.info(f"Device added to session: {device_info.get('name', 'unknown')}")

    def add_sync_event(self, event_type: str, event_data: Dict[str, Any] = None) -> None:
        """Add synchronization event to current session"""
        if not self._current_session:
            raise ValueError("No active session")

        sync_event = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "event_type": event_type,
            "data": event_data or {},
        }

        self._current_session.sync_events.append(sync_event)
        self._save_metadata()
        logger.info(f"Sync event added: {event_type}")
