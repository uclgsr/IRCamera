"""Simple logger implementation for MVP"""


class SimpleLogger:
    """Simple logger implementation that mimics loguru interface"""

    def __init__(self):
        pass

    def info(self, message: str) -> None:
        pass

    def debug(self, message: str) -> None:
        pass

    def warning(self, message: str) -> None:
        pass

    def error(self, message: str) -> None:
        pass


logger = SimpleLogger()
