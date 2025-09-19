"""
GUI modules for IRCamera PC Controller

This package contains the PyQt6-based user interface components.
"""

import os

# Check if GUI is available
GUI_AVAILABLE = True
try:
    # Set Qt platform to offscreen if no display is available
    if "DISPLAY" not in os.environ and "QT_QPA_PLATFORM" not in os.environ:
        os.environ["QT_QPA_PLATFORM"] = "offscreen"

    # Test PyQt6 imports
    from PyQt6.QtCore import Qt

    # Import app components if GUI is available
    from .app import IRCameraApp, main
except ImportError:
    GUI_AVAILABLE = False


    # Create dummy main function for headless mode
    def main(args=None):
        import sys
        import logging

        # Try loguru first, fallback to standard logging
        try:
            from loguru import logger
            logger.remove()
            logger.add(sys.stderr, format="{time} | {level} | {name}:{function}:{line} - {message}",
                       colorize=False)
            logger.info("IRCamera Application initialized")
            logger.info("Running in headless mode - GUI not available")
        except ImportError:
            # Fallback to standard logging
            logging.basicConfig(level=logging.INFO,
                                format="%(asctime)s | %(levelname)s | %(message)s")
            logger = logging.getLogger(__name__)
            logger.info("IRCamera Application initialized")
            logger.info("Running in headless mode - GUI not available")

        return 0


    # Create dummy app class
    class IRCameraApp:
        def __init__(self, *args, **kwargs):
            pass

        def exec(self):
            return 0

from .utils import (
    format_duration,
    format_file_size,
    setup_logging,
)

# Only import GUI components if available
if GUI_AVAILABLE:
    try:
        from .main_window import MainWindow
        from .widgets import (
            DeviceListWidget,
            SessionControlWidget,
            StatusDisplayWidget,
        )

        __all__ = [
            "IRCameraApp",
            "main",
            "MainWindow",
            "DeviceListWidget",
            "SessionControlWidget",
            "StatusDisplayWidget",
            "setup_logging",
            "format_file_size",
            "format_duration",
        ]
    except ImportError:
        __all__ = [
            "IRCameraApp",
            "main",
            "setup_logging",
            "format_file_size",
            "format_duration",
        ]
else:
    __all__ = [
        "IRCameraApp",
        "main",
        "setup_logging",
        "format_file_size",
        "format_duration",
    ]
