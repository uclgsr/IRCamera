import sys
from typing import Any

try:
    from ..core.config import config
except ImportError:
    # Mock config for headless mode
    class MockConfig:
        def get(self, key, default=None):
            return default
        
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
        pass


def setup_logging() -> LogHandler:
    gui_handler = LogHandler()
    
    def gui_sink(record) -> Any:
        pass
    
    return gui_handler


def get_app_icon() -> Any:
    return None


def apply_theme(app: Any, theme_name: str = "default") -> Any:
    return app


def format_file_size(size_bytes: int) -> str:
    if size_bytes == 0:
        return "0B"
    size_names = ["B", "KB", "MB", "GB", "TB"]
    import math
    i = int(math.floor(math.log(size_bytes, 1024)))
    p = math.pow(1024, i)
    s = round(size_bytes / p, 2)
    return f"{s} {size_names[i]}"


def format_duration(seconds: float) -> str:
    hours = int(seconds // 3600)
    minutes = int((seconds % 3600) // 60)
    secs = int(seconds % 60)
    if hours > 0:
        return f"{hours:02d}:{minutes:02d}:{secs:02d}"
    else:
        return f"{minutes:02d}:{secs:02d}"


def setup_logging() -> LogHandler:
    gui_handler = LogHandler()
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
    try:
        from PyQt6.QtWidgets import QMessageBox

        reply = QMessageBox.question(
            parent,
            title,
            message,
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
            QMessageBox.StandardButton.No,
        )

        return reply == QMessageBox.StandardButton.Yes
    except ImportError:
        return True
