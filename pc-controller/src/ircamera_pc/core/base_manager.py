

import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional

try:
    from abc import ABCMeta

    from PyQt6.QtCore import QObject as QtQObject
    from PyQt6.QtCore import pyqtSignal

    PYQT_AVAILABLE = True


    class QObjectMeta(type(QtQObject), ABCMeta):
        


    class BaseManager(QtQObject, ABC, metaclass=QObjectMeta):
        

        
        status_changed = pyqtSignal(str, dict)  
        error_occurred = pyqtSignal(str, str)  
        operation_completed = pyqtSignal(str, bool, str)  

        

        def __init__(self, name: str, parent: Optional[QtQObject] = None):
            super().__init__(parent)
            self._setup_base_manager(name)

        def _setup_base_manager(self, name: str):
            
            self._name = name
            self._logger = logging.getLogger(f"ircamera_pc.{name.lower()}")
            self._is_initialized = False
            self._state: Dict[str, Any] = {}
            self._last_error: Optional[str] = None

except ImportError:
    PYQT_AVAILABLE = False


    def pyqtSignal(*args, **kwargs) -> Any:
        

        def decorator(func) -> Any:
            return func

        return decorator


    class BaseManager(ABC):
        

        
        status_changed = None
        error_occurred = None
        operation_completed = None

        def __init__(self, name: str, parent: Optional[Any] = None):
            self.parent = parent
            self._setup_base_manager(name)

        def _setup_base_manager(self, name: str):
            
            self._name = name
            self._logger = logging.getLogger(f"ircamera_pc.{name.lower()}")
            self._is_initialized = False
            self._state: Dict[str, Any] = {}
            self._last_error: Optional[str] = None


    @property
    def name(self) -> str:
        
        return self._name


    @property
    def logger(self) -> logging.Logger:
        
        return self._logger


    @property
    def is_initialized(self) -> bool:
        
        return self._is_initialized


    @property
    def state(self) -> Dict[str, Any]:
        
        return self._state.copy()


    @property
    def last_error(self) -> Optional[str]:
        
        return self._last_error


    @abstractmethod
    async def initialize(self) -> bool:
        


    @abstractmethod
    async def cleanup(self) -> None:
        


    def _set_state(self, key: str, value: Any) -> None:
        
        old_value = self._state.get(key)
        self._state[key] = value

        if old_value != value and PYQT_AVAILABLE:
            self.status_changed.emit(key, {key: value})


    def _handle_error(
            self,
            error_type: str,
            message: str,
            exception: Optional[Exception] = None,
    ) -> None:
        
        self._last_error = message

        if exception:
            self._logger.error(f"{error_type}: {message}", exc_info=exception)
        else:
            self._logger.error(f"{error_type}: {message}")

        if PYQT_AVAILABLE:
            self.error_occurred.emit(error_type, message)


    def _emit_operation_result(
            self, operation: str, success: bool, message: str = ""
    ) -> None:
        
        if success:
            self._logger.info(
                f"Operation '{operation}' completed successfully: {message}"
            )
        else:
            self._logger.warning(f"Operation '{operation}' failed: {message}")

        if PYQT_AVAILABLE:
            self.operation_completed.emit(operation, success, message)


    def _validate_state(self, required_keys: list) -> bool:
        
        missing_keys = [key for key in required_keys if key not in self._state]
        if missing_keys:
            self._handle_error(
                "STATE_VALIDATION",
                f"Missing required state keys: {missing_keys}",
            )
            return False
        return True


    def reset_state(self) -> None:
        
        self._state.clear()
        self._last_error = None
        self._is_initialized = False
        self._logger.info(f"Manager '{self._name}' state reset")


class AsyncContextManager(BaseManager):
    

    async def __aenter__(self):
        
        if await self.initialize():
            return self
        else:
            raise RuntimeError(f"Failed to initialize {self._name}")

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        
        await self.cleanup()


class SingletonManager(BaseManager):
    

    _instances: Dict[str, "SingletonManager"] = {}

    def __new__(cls, name: str, parent: Optional[Any] = None):
        
        if name not in cls._instances:
            instance = super().__new__(cls)
            cls._instances[name] = instance
        return cls._instances[name]

    @classmethod
    def get_instance(cls, name: str) -> Optional["SingletonManager"]:
        
        return cls._instances.get(name)

    @classmethod
    def clear_instances(cls) -> None:
        
        cls._instances.clear()
