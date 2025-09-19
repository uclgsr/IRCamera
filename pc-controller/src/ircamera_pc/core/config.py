"""
Configuration Manager for IRCamera PC Controller

Handles loading and management of application configuration from YAML files.
Provides centralized access to configuration settings across all modules.
"""

import yaml
from pathlib import Path
from typing import Any, Dict, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger


class ConfigManager:
    """Manages application configuration settings."""

    def __init__(self, config_path: Optional[str] = None):
        """
        Initialize configuration manager.

        Args:
            config_path: Path to configuration file. If None, uses default location.
        """
        if config_path is None:
            
            project_root = Path(__file__).parent.parent.parent.parent
            config_path = project_root / "config" / "config.yaml"

        self.config_path = Path(config_path)
        self._config: Dict[str, Any] = {}
        self._load_config()

    def _load_config(self) -> None:
        """Load configuration from YAML file."""
        try:
            if not self.config_path.exists():
                logger.warning(f"Config file not found: {self.config_path}")
                self._config = self._get_default_config()
                return

            with open(self.config_path, "r", encoding="utf-8") as file:
                self._config = yaml.safe_load(file) or {}

            logger.info(f"Configuration loaded from {self.config_path}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to load configuration: {e}")
            self._config = self._get_default_config()

    def _get_default_config(self) -> Dict[str, Any]:
        """Return default configuration if file loading fails."""
        return {
            "network": {
                "server_host": "127.0.0.1",  
                "server_port": 8080,
                "max_connections": 8,
                "heartbeat_interval": 5,
                "connection_timeout": 30,
            },
            "time_sync": {
                "sync_interval": 30,
                "target_accuracy_ms": 5,
                "max_offset_ms": 15,
            },
            "session": {
                "data_root": "./sessions",
                "max_session_duration": 7200,
                "metadata_format": "json",
                "auto_backup": True,
            },
            "gsr": {
                "default_mode": "local",
                "sampling_rate": 128,
                "bridged_latency_p95_ms": 150,
                "reconciliation_window_s": 1800,
            },
            "gui": {
                "theme": "default",
                "update_interval_ms": 100,
                "window_size": [1200, 800],
                "auto_save_layout": True,
            },
            "logging": {
                "level": "INFO",
                "file_rotation": "1 MB",
                "retention": "30 days",
                "console_output": True,
            },
        }

    def get(self, key: str, default: Optional[Any] = None) -> Any:
        """
        Get configuration value by dot-notation key.

        Args:
            key: Configuration key in dot notation (e.g.,
                'network.server_port')
            default: Default value if key not found

        Returns:
            Configuration value or default
        """
        try:
            keys = key.split(".")
            value = self._config

            for k in keys:
                value = value[k]

            return value

        except (KeyError, TypeError):
            return default

    def set(self, key: str, value: Any) -> None:
        """
        Set configuration value by dot-notation key.

        Args:
            key: Configuration key in dot notation
            value: Value to set
        """
        keys = key.split(".")
        config = self._config

        
        for k in keys[:-1]:
            if k not in config:
                config[k] = {}
            config = config[k]

        
        config[keys[-1]] = value
        logger.debug(f"Configuration updated: {key} = {value}")

    def save(self) -> None:
        """Save current configuration to file."""
        try:
            self.config_path.parent.mkdir(parents=True, exist_ok=True)

            with open(self.config_path, "w", encoding="utf-8") as file:
                yaml.dump(self._config, file, default_flow_style=False, indent=2)

            logger.info(f"Configuration saved to {self.config_path}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to save configuration: {e}")

    def reload(self) -> None:
        """Reload configuration from file."""
        self._load_config()

    def get_all(self) -> Dict[str, Any]:
        """Get entire configuration dictionary."""
        return self._config.copy()



config = ConfigManager()
