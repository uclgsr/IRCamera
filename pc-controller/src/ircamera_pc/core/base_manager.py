"""
Consolidated Base Manager for IRCamera PC Controller

Provides a single, unified base class for all manager components
to eliminate code duplication and ensure consistent behavior.
"""

import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional

try:
    from abc import ABCMeta

    from PyQt6.QtCore import QObject as QtQObject
    from PyQt6.QtCore import pyqtSignal

    PYQT_AVAILABLE = True


    class QObjectMeta(type(QtQObject), ABCMeta):
        """Metaclass to resolve conflict between QObject and ABC"""


    class BaseManager(QtQObject, ABC, metaclass=QObjectMeta):
        """
        Unified base manager class for all IRCamera PC Controller components.

        Provides common functionality including:
        - Logging setup
        - State management
        - Error handling patterns
        - PyQt6 signal support
        """

        
        status_changed = pyqtSignal(str, dict)  
        error_occurred = pyqtSignal(str, str)  
        operation_completed = pyqtSignal(str, bool, str)  

        

        def __init__(self, name: str, parent: Optional[QtQObject] = None):
            super().__init__(parent)
            self._setup_base_manager(name)

        def _setup_base_manager(self, name: str):
            """Common setup for both PyQt and non-PyQt versions"""
            self._name = name
            self._logger = logging.getLogger(f"ircamera_pc.{name.lower()}")
            self._is_initialized = False
            self._state: Dict[str, Any] = {}
            self._last_error: Optional[str] = None

except ImportError:
    PYQT_AVAILABLE = False


    def pyqtSignal(*args, **kwargs) -> Any:
        """Mock pyqtSignal decorator"""

        def decorator(func) -> Any:
            return func

        return decorator


    class BaseManager(ABC):
        """
        Unified base manager class for all IRCamera PC Controller components.

        Provides common functionality including:
        - Logging setup
        - State management
        - Error handling patterns
        - No PyQt6 dependencies
        """

        
        status_changed = None
        error_occurred = None
        operation_completed = None

        def __init__(self, name: str, parent: Optional[Any] = None):
            self.parent = parent
            self._setup_base_manager(name)

        def _setup_base_manager(self, name: str):
            """Common setup for both PyQt and non-PyQt versions"""
            self._name = name
            self._logger = logging.getLogger(f"ircamera_pc.{name.lower()}")
            self._is_initialized = False
            self._state: Dict[str, Any] = {}
            self._last_error: Optional[str] = None


    @property
    def name(self) -> str:
        """Get manager name."""
        return self._name


    @property
    def logger(self) -> logging.Logger:
        """Get logger instance."""
        return self._logger


    @property
    def is_initialized(self) -> bool:
        """Check if manager is initialized."""
        return self._is_initialized


    @property
    def state(self) -> Dict[str, Any]:
        """Get current state dictionary."""
        return self._state.copy()


    @property
    def last_error(self) -> Optional[str]:
        """Get last error message."""
        return self._last_error


    @abstractmethod
    async def initialize(self) -> bool:
        """
        Initialize the manager.

        Returns:
            True if initialization successful, False otherwise
        """


    @abstractmethod
    async def cleanup(self) -> None:
        """Clean up manager resources."""


    def _set_state(self, key: str, value: Any) -> None:
        """
        Set state value and emit signal if available.

        Args:
            key: State key
            value: State value
        """
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
        """
        Handle error with logging and signal emission.

        Args:
            error_type: Type of error
            message: Error message
            exception: Optional exception instance
        """
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
        """
        Emit operation completion signal.

        Args:
            operation: Operation name
            success: Success status
            message: Optional result message
        """
        if success:
            self._logger.info(
                f"Operation '{operation}' completed successfully: {message}"
            )
        else:
            self._logger.warning(f"Operation '{operation}' failed: {message}")

        if PYQT_AVAILABLE:
            self.operation_completed.emit(operation, success, message)


    def _validate_state(self, required_keys: list) -> bool:
        """
        Validate that required state keys are present.

        Args:
            required_keys: List of required state keys

        Returns:
            True if all required keys present, False otherwise
        """
        missing_keys = [key for key in required_keys if key not in self._state]
        if missing_keys:
            self._handle_error(
                "STATE_VALIDATION",
                f"Missing required state keys: {missing_keys}",
            )
            return False
        return True


    def reset_state(self) -> None:
        """Reset manager state."""
        self._state.clear()
        self._last_error = None
        self._is_initialized = False
        self._logger.info(f"Manager '{self._name}' state reset")


class AsyncContextManager(BaseManager):
    """
    Base manager with async context manager support.
    """

    async def __aenter__(self):
        """Async context manager entry."""
        if await self.initialize():
            return self
        else:
            raise RuntimeError(f"Failed to initialize {self._name}")

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.cleanup()


class SingletonManager(BaseManager):
    """
    Base manager with singleton pattern support.
    """

    _instances: Dict[str, "SingletonManager"] = {}

    def __new__(cls, name: str, parent: Optional[Any] = None):
        """Ensure singleton instance per name."""
        if name not in cls._instances:
            instance = super().__new__(cls)
            cls._instances[name] = instance
        return cls._instances[name]

    @classmethod
    def get_instance(cls, name: str) -> Optional["SingletonManager"]:
        """Get existing singleton instance by name."""
        return cls._instances.get(name)

    @classmethod
    def clear_instances(cls) -> None:
        """Clear all singleton instances (mainly for testing)."""
        cls._instances.clear()
