"""
Simple logger fallback for when loguru is not available

Provides basic logging functionality for development and testing.
"""

import logging
import sys

# Configure basic logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s:%(funcName)s:%(lineno)d"
    "- %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("ircamera_pc.log"),
    ],
)


class SimpleLogger:
    """Simple logger that mimics loguru interface."""

    def __init__(self, name: str = "ircamera_pc"):
        self._logger = logging.getLogger(name)

    def debug(self, message: str, *args, **kwargs) -> None:
        self._logger.debug(message, *args)

    def info(self, message: str, *args, **kwargs) -> None:
        self._logger.info(message, *args)

    def warning(self, message: str, *args, **kwargs) -> None:
        self._logger.warning(message, *args)

    def error(self, message: str, *args, **kwargs) -> None:
        self._logger.error(message, *args)

    def critical(self, message: str, *args, **kwargs) -> None:
        self._logger.critical(message, *args)

    def remove(self, *args, **kwargs) -> None:
        """Remove handler - no-op for simple logger."""

    def add(self, *args, **kwargs) -> None:
        """Add handler - no-op for simple logger."""


# Create global logger instance
logger = SimpleLogger()


# Make it available at module level
def debug(message: str, *args, **kwargs) -> None:
    logger.debug(message, *args, **kwargs)


def info(message: str, *args, **kwargs) -> None:
    logger.info(message, *args, **kwargs)


def warning(message: str, *args, **kwargs) -> None:
    logger.warning(message, *args, **kwargs)


def error(message: str, *args, **kwargs) -> None:
    logger.error(message, *args, **kwargs)


def critical(message: str, *args, **kwargs) -> None:
    logger.critical(message, *args, **kwargs)


def get_logger(name: str = "ircamera_pc") -> SimpleLogger:
    """Get a logger instance for the given name."""
    return SimpleLogger(name)
