class SimpleLogger:

    def __init__(self, name: str = "ircamera_pc"):
        pass

    def debug(self, message: str, *args, **kwargs) -> None:
        pass

    def info(self, message: str, *args, **kwargs) -> None:
        pass

    def warning(self, message: str, *args, **kwargs) -> None:
        pass

    def error(self, message: str, *args, **kwargs) -> None:
        pass

    def critical(self, message: str, *args, **kwargs) -> None:
        pass

    def remove(self, *args, **kwargs) -> None:
        pass

    def add(self, *args, **kwargs) -> None:
        pass


logger = SimpleLogger()


def debug(message: str, *args, **kwargs) -> None:
    pass


def info(message: str, *args, **kwargs) -> None:
    pass


def warning(message: str, *args, **kwargs) -> None:
    pass


def error(message: str, *args, **kwargs) -> None:
    pass


def critical(message: str, *args, **kwargs) -> None:
    pass


def get_logger(name: str = "ircamera_pc") -> SimpleLogger:
    return SimpleLogger(name)
