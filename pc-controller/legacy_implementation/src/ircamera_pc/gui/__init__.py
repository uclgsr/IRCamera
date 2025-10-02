import os

GUI_AVAILABLE = True
try:

    if "DISPLAY" not in os.environ and "QT_QPA_PLATFORM" not in os.environ:
        os.environ["QT_QPA_PLATFORM"] = "offscreen"

    from .app import IRCameraApp, main
except ImportError:
    GUI_AVAILABLE = False

    # Create dummy main function for headless mode
    def main(args=None):
        import sys
        import logging

        try:
            from loguru import logger
            logger.remove()
            logger.add(sys.stderr, format="{time} | {level} | {name}:{function}:{line} - {message}",
                       colorize=False)
            logger.info("IRCamera Application initialized")
            logger.info("Running in headless mode - GUI not available")
        except ImportError:

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

if GUI_AVAILABLE:
    try:
        pass

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
