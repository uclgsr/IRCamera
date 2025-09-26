import yaml
from pathlib import Path
from typing import Any, Dict, Optional

try:
    except ImportError:
    

class ConfigManager:

    def __init__(self, config_path: Optional[str] = None):

        if config_path is None:
            project_root = Path(__file__).parent.parent.parent.parent
            config_path = project_root / "config" / "config.yaml"

        self.config_path = Path(config_path)
        self._config: Dict[str, Any] = {}
        self._load_config()

    def _load_config(self) -> None:

        try:
            if not self.config_path.exists():
                                self._config = self._get_default_config()
                return

            with open(self.config_path, "r", encoding="utf-8") as file:
                self._config = yaml.safe_load(file) or {}

            
        except (OSError, ValueError, RuntimeError) as e:
                        self._config = self._get_default_config()

    def _get_default_config(self) -> Dict[str, Any]:

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

        try:
            keys = key.split(".")
            value = self._config

            for k in keys:
                value = value[k]

            return value

        except (KeyError, TypeError):
            return default

    def set(self, key: str, value: Any) -> None:

        keys = key.split(".")
        config = self._config

        for k in keys[:-1]:
            if k not in config:
                config[k] = {}
            config = config[k]

        config[keys[-1]] = value
        
    def save(self) -> None:

        try:
            self.config_path.parent.mkdir(parents=True, exist_ok=True)

            with open(self.config_path, "w", encoding="utf-8") as file:
                yaml.dump(self._config, file, default_flow_style=False, indent=2)

            
        except (OSError, ValueError, RuntimeError) as e:
            
    def reload(self) -> None:

        self._load_config()

    def get_all(self) -> Dict[str, Any]:

        return self._config.copy()


config = ConfigManager()
