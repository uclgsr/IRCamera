"""
Protocol Manager for IRCamera PC Controller

Manages JSON-based communication protocol definition and message validation.
"""

import json
import jsonschema
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from loguru import logger
from pathlib import Path
from typing import Any, Dict, List, Optional


class ValidationError(Exception):
    """Protocol validation error."""


class MessageDirection(Enum):
    """Message direction types."""

    PC_TO_DEVICE = "pc_to_device"
    DEVICE_TO_PC = "device_to_pc"
    BIDIRECTIONAL = "bidirectional"


@dataclass
class MessageDefinition:
    """Definition of a message type."""

    name: str
    direction: MessageDirection
    description: str
    required_fields: List[str]
    optional_fields: List[str]
    schema: Dict[str, Any]


class ProtocolManager:
    """
    Manages the JSON-based communication protocol for IRCamera.

    Provides:
    - Protocol definition loading and parsing
    - Message validation against JSON schemas
    - Message construction helpers
    - Protocol version management
    """

    def __init__(self, protocol_file: Optional[Path] = None):
        """
        Initialize protocol manager.

        Args:
            protocol_file: Path to protocol.json file. If None, uses default
                location.
        """
        self._protocol_file = protocol_file
        self._protocol_def: Optional[Dict[str, Any]] = None
        self._message_definitions: Dict[str, MessageDefinition] = {}
        self._validator_cache: Dict[str, jsonschema.protocols.Validator] = {}

        self._load_protocol()

    def _load_protocol(self) -> None:
        """Load and parse the protocol definition."""
        if self._protocol_file is None:
            
            current_dir = Path(__file__).parent
            config_dir = current_dir.parent.parent.parent / "config"
            self._protocol_file = config_dir / "protocol.json"

        try:
            with open(self._protocol_file, "r", encoding="utf-8") as f:
                self._protocol_def = json.load(f)

            self._parse_message_definitions()
            logger.info(f"Protocol definition loaded: {self.get_protocol_info()}")

        except FileNotFoundError:
            logger.error(f"Protocol file not found: {self._protocol_file}")
            raise
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in protocol file: {e}")
            raise
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to load protocol definition: {e}")
            raise

    def _parse_message_definitions(self) -> None:
        """Parse message definitions from protocol."""
        if not self._protocol_def:
            return

        message_types = self._protocol_def.get("message_types", {})

        for category, messages in message_types.items():
            for msg_name, msg_def in messages.items():
                direction = MessageDirection(msg_def.get("direction", "bidirectional"))

                definition = MessageDefinition(
                    name=msg_name,
                    direction=direction,
                    description=msg_def.get("description", ""),
                    required_fields=msg_def.get("required_fields", []),
                    optional_fields=msg_def.get("optional_fields", []),
                    schema=msg_def.get("schema", {}),
                )

                self._message_definitions[msg_name] = definition

        logger.info(f"Loaded {len(self._message_definitions)}" "message definitions")

    def get_protocol_info(self) -> Dict[str, Any]:
        """Get protocol information."""
        if not self._protocol_def:
            return {}

        protocol_info = self._protocol_def.get("protocol", {})
        return {
            "name": protocol_info.get("name", "Unknown"),
            "version": protocol_info.get("version", "Unknown"),
            "description": protocol_info.get("description", ""),
            "message_types": len(self._message_definitions),
        }

    def get_message_types(self) -> List[str]:
        """Get list of available message types."""
        return list(self._message_definitions.keys())

    def get_message_definition(self, message_type: str) -> Optional[MessageDefinition]:
        """Get definition for a message type."""
        return self._message_definitions.get(message_type)

    def validate_message(self, message: Dict[str, Any], strict: bool = True) -> bool:
        """
        Validate a message against the protocol.

        Args:
            message: Message to validate
            strict: If True, raise exception on validation error

        Returns:
            True if message is valid

        Raises:
            ValidationError: If validation fails and strict=True
        """
        try:
            
            if not isinstance(message, dict):
                raise ValidationError("Message must be a dictionary")

            message_type = message.get("message_type")
            if not message_type:
                raise ValidationError("Message must have 'message_type' field")

            
            msg_def = self._message_definitions.get(message_type)
            if not msg_def:
                raise ValidationError(f"Unknown message type: {message_type}")

            
            validator = self._get_validator(message_type, msg_def.schema)
            validator.validate(message)

            
            self._validate_timestamp(message)

            return True

        except (jsonschema.ValidationError, ValidationError) as e:
            error_msg = f"Message validation failed: {e}"
            if strict:
                raise ValidationError(error_msg)
            else:
                logger.warning(error_msg)
                return False
        except (OSError, ValueError, RuntimeError) as e:
            error_msg = f"Unexpected validation error: {e}"
            if strict:
                raise ValidationError(error_msg)
            else:
                logger.error(error_msg)
                return False

    def _get_validator(
            self, message_type: str, schema: Dict[str, Any]
    ) -> jsonschema.protocols.Validator:
        """Get cached validator for message type."""
        if message_type not in self._validator_cache:
            
            complete_schema = self._add_common_fields(schema)
            validator = jsonschema.Draft7Validator(complete_schema)
            self._validator_cache[message_type] = validator

        return self._validator_cache[message_type]

    def _add_common_fields(self, schema: Dict[str, Any]) -> Dict[str, Any]:
        """Add common fields to message schema."""
        if self._protocol_def is None:
            return schema

        common_fields = self._protocol_def.get("common_fields", {})

        
        complete_schema = json.loads(json.dumps(schema))

        
        if "properties" not in complete_schema:
            complete_schema["properties"] = {}

        for field_name, field_def in common_fields.items():
            if field_name not in complete_schema["properties"]:
                complete_schema["properties"][field_name] = {
                    "type": field_def.get("type", "string"),
                    "description": field_def.get("description", ""),
                }

                
                if "format" in field_def:
                    complete_schema["properties"][field_name]["format"] = field_def[
                        "format"
                    ]

        
        if "required" not in complete_schema:
            complete_schema["required"] = []

        for field_name, field_def in common_fields.items():
            if (
                    field_def.get("required", False)
                    and field_name not in complete_schema["required"]
            ):
                complete_schema["required"].append(field_name)

        return complete_schema

    def _validate_timestamp(self, message: Dict[str, Any]) -> None:
        """Validate message timestamp."""
        timestamp_str = message.get("timestamp")
        if not timestamp_str:
            return  

        try:
            
            timestamp = datetime.fromisoformat(timestamp_str.replace("Z", "+00:00"))

            
            now = datetime.now(timezone.utc)
            if self._protocol_def is None:
                tolerance_ms = 5000
            else:
                tolerance_ms = self._protocol_def.get("validation", {}).get(
                    "timestamp_tolerance_ms", 5000
                )
            tolerance = abs((timestamp - now).total_seconds() * 1000)

            if tolerance > tolerance_ms:
                logger.warning(f"Timestamp tolerance exceeded: {tolerance:.0f}ms")

        except ValueError as e:
            raise ValidationError(f"Invalid timestamp format: {e}")

    def create_message(self, message_type: str, **kwargs) -> Dict[str, Any]:
        """
        Create a message of the specified type.

        Args:
            message_type: Type of message to create
            **kwargs: Message fields

        Returns:
            Complete message dictionary

        Raises:
            ValidationError: If message cannot be created
        """
        msg_def = self._message_definitions.get(message_type)
        if not msg_def:
            raise ValidationError(f"Unknown message type: {message_type}")

        
        message = {
            "message_type": message_type,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

        
        message.update(kwargs)

        
        self.validate_message(message, strict=True)

        return message

    def get_transport_config(self) -> Dict[str, Any]:
        """Get transport configuration from protocol."""
        return self._protocol_def.get("transport", {})

    def get_validation_config(self) -> Dict[str, Any]:
        """Get validation configuration from protocol."""
        return self._protocol_def.get("validation", {})

    def reload_protocol(self) -> None:
        """Reload protocol definition from file."""
        self._message_definitions.clear()
        self._validator_cache.clear()
        self._load_protocol()
        logger.info("Protocol definition reloaded")



_protocol_manager: Optional[ProtocolManager] = None


def get_protocol_manager() -> ProtocolManager:
    """Get global protocol manager instance."""
    global _protocol_manager
    if _protocol_manager is None:
        _protocol_manager = ProtocolManager()
    return _protocol_manager


def validate_message(message: Dict[str, Any], strict: bool = True) -> bool:
    """Validate a message using the global protocol manager."""
    return get_protocol_manager().validate_message(message, strict)


def create_message(message_type: str, **kwargs) -> Dict[str, Any]:
    """Create a message using the global protocol manager."""
    return get_protocol_manager().create_message(message_type, **kwargs)
