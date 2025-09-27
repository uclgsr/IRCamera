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
    IDLE = "idle"
    ACTIVE = "active"
    RECORDING = "recording"
    STOPPING = "stopping"
    COMPLETED = "completed"
    ERROR = "error"


@dataclass
class SessionMetadata:
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

    def __init__(self):

        self._current_session: Optional[SessionMetadata] = None
        self._session_history: List[str] = []
        self._data_root = Path(config.get("session.data_root", "./sessions"))
        self._ensure_data_root()

        logger.info("Session Manager initialized")

    def _ensure_data_root(self) -> None:

        self._data_root.mkdir(parents=True, exist_ok=True)
        logger.debug(f"Session data root: {self._data_root}")

    def create_session(self, name: Optional[str] = None) -> SessionMetadata:

        if self._current_session and self._current_session.state in [
            SessionState.ACTIVE.value,
            SessionState.RECORDING.value,
        ]:
            raise ValueError("Cannot create new session:" "another session is active")

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

    def start_session(self) -> None:

        if not self._current_session:
            raise ValueError("No session to start")

        if self._current_session.state != SessionState.IDLE.value:
            raise ValueError(
                f"Cannot start session in state: {self._current_session.state}"
            )

        self._current_session.state = SessionState.ACTIVE.value
        self._current_session.started_at = datetime.now(timezone.utc).isoformat()

        self._save_metadata()

        logger.info(f"Session started: {self._current_session.name}")

    def begin_recording(self) -> None:

        if not self._current_session:
            raise ValueError("No active session")

        if self._current_session.state != SessionState.ACTIVE.value:
            raise ValueError(
                f"Cannot begin recording in state: {self._current_session.state}"
            )

        self._current_session.state = SessionState.RECORDING.value
        self._save_metadata()

        logger.info(f"Recording started for session: {self._current_session.name}")

    def end_session(self) -> SessionMetadata:

        if not self._current_session:
            raise ValueError("No session to end")

        if self._current_session.started_at:
            start_time = datetime.fromisoformat(
                self._current_session.started_at.replace("Z", "+00:00")
            )
            end_time = datetime.now(timezone.utc)
            duration = (end_time - start_time).total_seconds()
            self._current_session.duration_seconds = duration

        self._current_session.state = SessionState.COMPLETED.value
        self._current_session.ended_at = datetime.now(timezone.utc).isoformat()

        self._save_metadata()

        logger.info(
            f"Session ended: {self._current_session.name} "
            f"(duration: {self._current_session.duration_seconds:.1f}s)"
        )

        completed_session = self._current_session
        self._current_session = None

        return completed_session

    def add_device(self, device_info: Dict[str, Any]) -> None:

        if not self._current_session:
            raise ValueError("No active session")

        self._current_session.devices.append(
            {"added_at": datetime.now(timezone.utc).isoformat(), **device_info}
        )

        self._save_metadata()
        logger.debug(
            f"Device added to session: {device_info.get('device_id', 'unknown')}"
        )

    def add_file(self, file_info: Dict[str, Any]) -> None:

        if not self._current_session:
            raise ValueError("No active session")

        self._current_session.files.append(
            {"added_at": datetime.now(timezone.utc).isoformat(), **file_info}
        )

        self._save_metadata()
        logger.debug(f"File added to session: {file_info.get('filename', 'unknown')}")

    def add_sync_event(
            self, event_type: str, event_data: Dict[str, Any] = None
    ) -> None:

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

    def get_current_session(self) -> Optional[SessionMetadata]:

        return self._current_session

    def get_session_state(self) -> Optional[str]:

        if self._current_session:
            return self._current_session.state
        return None

    def get_session(self, session_id: str) -> Optional[SessionMetadata]:

        if self._current_session and self._current_session.session_id == session_id:
            return self._current_session

        return self.load_session(session_id)

    def get_session_directory(self, session_id: Optional[str] = None) -> Path:

        if session_id is None:
            if not self._current_session:
                raise ValueError("No current session")
            session_id = self._current_session.session_id

        return self._get_session_directory(session_id)

    def _get_session_directory(self, session_id: str) -> Path:

        return self._data_root / session_id

    def _save_metadata(self) -> None:

        if not self._current_session:
            return

        metadata_file = (
                self._get_session_directory(self._current_session.session_id)
                / "metadata.json"
        )

        try:
            with open(metadata_file, "w", encoding="utf-8") as f:
                json.dump(
                    asdict(self._current_session),
                    f,
                    indent=2,
                    ensure_ascii=False,
                )

            logger.debug(f"Session metadata saved: {metadata_file}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to save session metadata: {e}")

    def load_session(self, session_id: str) -> Optional[SessionMetadata]:

        metadata_file = self._get_session_directory(session_id) / "metadata.json"

        try:
            if not metadata_file.exists():
                logger.warning(f"Session metadata not found: {session_id}")
                return None

            with open(metadata_file, "r", encoding="utf-8") as f:
                data = json.load(f)

            return SessionMetadata(**data)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to load session metadata: {e}")
            return None

    def list_sessions(self) -> List[str]:

        sessions = []

        try:
            for item in self._data_root.iterdir():
                if item.is_dir() and (item / "metadata.json").exists():
                    sessions.append(item.name)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to list sessions: {e}")

        return sorted(sessions)
