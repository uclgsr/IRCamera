import logging
import sys
from typing import Any

try:
    from loguru import logger
except ImportError:

    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
    logger = logging.getLogger(__name__)

try:
    from ..core.config import config
except ImportError:
    # Mock config for headless mode
    class MockConfig:
        def __getattr__(self, name):
            return "default"

    config = MockConfig()

try:
    from PyQt6.QtCore import QObject, pyqtSignal

    GUI_AVAILABLE = True
except ImportError:
    GUI_AVAILABLE = False

    # Mock classes for headless mode
    class QObject:
        pass

    def pyqtSignal(*args, **kwargs):
        return None

class LogHandler(QObject):
    log_message = pyqtSignal(str, str, str)

    def __init__(self):
        super().__init__()

    def write(self, record) -> Any:
        level = record["level"].name
        message = record["message"]
        timestamp = record["time"].strftime("%Y-%m-%d %H:%M:%S")

        self.log_message.emit(level, message, timestamp)

def setup_logging() -> LogHandler:
    logger.remove()

    log_level = config.get("logging.level", "INFO")
    console_output = config.get("logging.console_output", True)
    file_rotation = config.get("logging.file_rotation", "1 MB")
    retention = config.get("logging.retention", "30 days")

    if console_output:
        logger.add(
            sys.stdout,
            level=log_level,
            format=(
                "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
                "<level>{level: <8}</level> | "
                "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan>"
                "-"
                "<level>{message}</level>"
            ),
        )

    logger.add(
        "logs/ircamera_pc.log",
        level=log_level,
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8}"
               "| {name}:{function}:{line} - {message}",
        rotation=file_rotation,
        retention=retention,
        compression="zip",
    )

    gui_handler = LogHandler()

    def gui_sink(record) -> Any:
        try:

            if hasattr(record, "level"):

                level = record.level.name
                message = record.message
                timestamp = record.time.strftime("%H:%M:%S")
            elif hasattr(record, "get"):

                level = record.get("level", {}).get("name", "INFO")
                message = record.get("message", "")
                timestamp = (
                    record.get("time", "").strftime("%H:%M:%S")
                    if record.get("time")
                    else ""
                )
            else:

                level = "INFO"
                message = str(record)
                timestamp = ""
            gui_handler.log_message.emit(level, message, timestamp)
        except Exception:

            try:
                gui_handler.log_message.emit("INFO", str(record), "")
            except Exception:

                pass

    try:
        logger.add(gui_sink, level=log_level)
    except Exception:

        pass

    logger.info("Logging system initialized")
    return gui_handler

def get_app_icon() -> Any:
    return None

def apply_theme(app: Any, theme_name: str = "default") -> Any:
    if theme_name == "dark":
        dark_style = """
        QMainWindow {
            background-color: #2d2d2d;
            color: #ffffff;
        }

        QWidget {
            background-color: #2d2d2d;
            color: #ffffff;
        }

        QGroupBox {
            font-weight: bold;
            border: 2px solid #555555;
            border-radius: 5px;
            margin-top: 1ex;
            padding-top: 10px;
            color: #ffffff;
        }

        QGroupBox::title {
            subcontrol-origin: margin;
            left: 10px;
            padding: 0 5px 0 5px;
        }

        QPushButton {
            background-color: #404040;
            border: 1px solid #666666;
            border-radius: 3px;
            padding: 6px;
            min-width: 80px;
            color: #ffffff;
        }

        QPushButton:hover {
            background-color: #4a4a4a;
        }

        QPushButton:pressed {
            background-color: #353535;
        }

        QPushButton:disabled {
            color: #888888;
            background-color: #2d2d2d;
        }

        QPushButton.primary {
            background-color: #0078d4;
            color: white;
            font-weight: bold;
        }

        QPushButton.primary:hover {
            background-color: #106ebe;
        }

        QPushButton.primary:pressed {
            background-color: #005a9e;
        }

        QPushButton.danger {
            background-color: #d13438;
            color: white;
            font-weight: bold;
        }

        QPushButton.danger:hover {
            background-color: #c4292e;
        }

        QPushButton.danger:pressed {
            background-color: #a01e22;
        }

        QListWidget {
            border: 1px solid #555555;
            border-radius: 3px;
            background-color: #353535;
            color: #ffffff;
        }

        QListWidget::item {
            padding: 8px;
            border-bottom: 1px solid #555555;
        }

        QListWidget::item:selected {
            background-color: #0078d4;
            color: white;
        }

        QTextEdit {
            border: 1px solid #555555;
            border-radius: 3px;
            background-color: #353535;
            color: #ffffff;
        }

        QLabel {
            color: #ffffff;
        }

        QStatusBar {
            border-top: 1px solid #555555;
            background-color: #404040;
            color: #ffffff;
        }

        QFrame {
            border: 1px solid #555555;
            background-color: #353535;
        }
        """

        app.setStyleSheet(dark_style)

def format_file_size(size_bytes: int) -> str:
    if size_bytes == 0:
        return "0 B"

    for unit in ["B", "KB", "MB", "GB", "TB"]:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0

    return f"{size_bytes:.1f} PB"

def format_duration(seconds: float) -> str:
    total_seconds = int(seconds)
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    secs = total_seconds % 60

    if hours > 0:
        return f"{hours:02d}:{minutes:02d}:{secs:02d}"
    else:
        return f"{minutes:02d}:{secs:02d}"

def get_status_color(status: str) -> str:
    status = status.lower()

    if status in ["connected", "ok", "active", "recording"]:
        return "green"
    elif status in ["warning", "disconnected", "error"]:
        return "red"
    elif status in ["idle", "waiting", "pending"]:
        return "orange"
    else:
        return "gray"

def validate_session_name(name: str) -> tuple[bool, str]:
    if not name or not name.strip():
        return True, ""

    name = name.strip()

    if len(name) > 100:
        return False, "Session name must be 100 characters or less"

    invalid_chars = ["<", ">", ":", '"', "|", "?", "*", "/", "\\"]
    for char in invalid_chars:
        if char in name:
            return False, f"Session name cannot contain '{char}'"

    return True, ""

def confirm_action(parent, title: str, message: str) -> bool:
    from PyQt6.QtWidgets import QMessageBox

    reply = QMessageBox.question(
        parent,
        title,
        message,
        QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        QMessageBox.StandardButton.No,
    )

    return reply == QMessageBox.StandardButton.Yes
