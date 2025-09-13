"""
GUI utilities for IRCamera PC Controller

Utility functions and classes for GUI components.
"""

import sys

from loguru import logger
from ..core.config import config
from typing import Any

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
    """Custom log handler that emits Qt signals for GUI integration."""

    log_message = pyqtSignal(str, str, str)  # level, message, timestamp

    def __init__(self):
        """Initialize log handler."""
        super().__init__()

    def write(self, record) -> Any:
        """Write log record."""
        # Extract relevant information from loguru record
        level = record["level"].name
        message = record["message"]
        timestamp = record["time"].strftime("%Y-%m-%d %H:%M:%S")

        # Emit signal for GUI components
        self.log_message.emit(level, message, timestamp)


def setup_logging() -> LogHandler:
    """
    Set up logging configuration for the application.

    Returns:
        LogHandler instance for GUI integration
    """
    # Remove default handler
    logger.remove()

    # Get logging configuration
    log_level = config.get("logging.level", "INFO")
    console_output = config.get("logging.console_output", True)
    file_rotation = config.get("logging.file_rotation", "1 MB")
    retention = config.get("logging.retention", "30 days")

    # Set up console logging if enabled
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

    # Set up file logging
    logger.add(
        "logs/ircamera_pc.log",
        level=log_level,
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8}"
        "| {name}:{function}:{line} - {message}",
        rotation=file_rotation,
        retention=retention,
        compression="zip",
    )

    # Create and configure GUI log handler
    gui_handler = LogHandler()

    # Add custom sink for GUI integration only if GUI is available
    def gui_sink(record) -> Any:
        try:
            # Handle both dict and Record object formats
            if hasattr(record, "level"):
                # New loguru Record object format
                level = record.level.name
                message = record.message
                timestamp = record.time.strftime("%H:%M:%S")
            elif hasattr(record, "get"):
                # Dictionary format for backwards compatibility
                level = record.get("level", {}).get("name", "INFO")
                message = record.get("message", "")
                timestamp = (
                    record.get("time", "").strftime("%H:%M:%S")
                    if record.get("time")
                    else ""
                )
            else:
                # Fallback for unknown formats
                level = "INFO"
                message = str(record)
                timestamp = ""
            gui_handler.log_message.emit(level, message, timestamp)
        except Exception:
            # Fallback for any formatting issues - handle both modes
            try:
                gui_handler.log_message.emit("INFO", str(record), "")
            except Exception:
                # Silently ignore GUI logging errors in headless mode
                pass

    # Only add GUI sink if GUI components are available
    try:
        logger.add(gui_sink, level=log_level)
    except Exception:
        # GUI not available, skip GUI logging
        pass

    logger.info("Logging system initialized")
    return gui_handler


def get_app_icon() -> Any:
    """
    Get application icon.

    Returns:
        QIcon or None if no icon available
    """
    # For now, return None - icon can be added later
    return None


def apply_theme(app: Any, theme_name: str = "default") -> Any:
    """
    Apply theme to the Qt application.

    Args:
        app: QApplication instance
        theme_name: Theme name to apply
    """
    if theme_name == "dark":
        # Dark theme stylesheet
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

    # Default theme is handled by the main app stylesheet


def format_file_size(size_bytes: int) -> str:
    """
    Format file size in human-readable format.

    Args:
        size_bytes: Size in bytes

    Returns:
        Formatted size string
    """
    if size_bytes == 0:
        return "0 B"

    for unit in ["B", "KB", "MB", "GB", "TB"]:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0

    return f"{size_bytes:.1f} PB"


def format_duration(seconds: float) -> str:
    """
    Format duration in human-readable format.

    Args:
        seconds: Duration in seconds

    Returns:
        Formatted duration string (HH:MM:SS)
    """
    total_seconds = int(seconds)
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    secs = total_seconds % 60

    if hours > 0:
        return f"{hours:02d}:{minutes:02d}:{secs:02d}"
    else:
        return f"{minutes:02d}:{secs:02d}"


def get_status_color(status: str) -> str:
    """
    Get color for status display.

    Args:
        status: Status string

    Returns:
        CSS color value
    """
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
    """
    Validate session name.

    Args:
        name: Session name to validate

    Returns:
        Tuple of (is_valid, error_message)
    """
    if not name or not name.strip():
        return True, ""  # Empty names are allowed (auto-generated)

    name = name.strip()

    # Check length
    if len(name) > 100:
        return False, "Session name must be 100 characters or less"

    # Check for invalid characters
    invalid_chars = ["<", ">", ":", '"', "|", "?", "*", "/", "\\"]
    for char in invalid_chars:
        if char in name:
            return False, f"Session name cannot contain '{char}'"

    return True, ""


def confirm_action(parent, title: str, message: str) -> bool:
    """
    Show confirmation dialog.

    Args:
        parent: Parent widget
        title: Dialog title
        message: Confirmation message

    Returns:
        True if user confirmed, False otherwise
    """
    from PyQt6.QtWidgets import QMessageBox

    reply = QMessageBox.question(
        parent,
        title,
        message,
        QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        QMessageBox.StandardButton.No,
    )

    return reply == QMessageBox.StandardButton.Yes
