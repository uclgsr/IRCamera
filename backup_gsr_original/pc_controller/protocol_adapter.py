#!/usr/bin/env python3
"""
Protocol Adapter for PC-Android Communication

This module provides bidirectional protocol translation between:
- Android's text-based protocol (key=value pairs)
- PC's JSON-based protocol

Implements the Android Protocol.kt specification.
"""

import json
import re
import time
from dataclasses import dataclass
from typing import Dict, Optional, List, Any


@dataclass
class ProtocolMessage:
    """Represents a parsed protocol message"""
    type: str
    parameters: Dict[str, str]
    raw: str


class ProtocolAdapter:
    """
    Bidirectional protocol adapter for PC-Android communication.
    
    Supports both text-based (Android) and JSON-based (PC) protocols.
    """

    # Message type mappings: Android -> PC internal names
    ANDROID_TO_PC_TYPES = {
        'HELLO': 'HELLO',
        'SYNC_INIT': 'SYNC_INIT',
        'SYNC_REQUEST': 'SYNC_REQUEST',
        'SYNC_RESPONSE': 'SYNC_RESPONSE',
        'SYNC_RESULT': 'SYNC_RESULT',
        'START_RECORD': 'START_RECORD',
        'STOP_RECORD': 'STOP_RECORD',
        'ACK': 'ACK',
        'ERROR': 'ERROR',
        'DATA_GSR': 'DATA_GSR',
        'FRAME': 'FRAME',
    }

    # Reverse mapping: PC -> Android
    PC_TO_ANDROID_TYPES = {v: k for k, v in ANDROID_TO_PC_TYPES.items()}

    def __init__(self):
        """Initialize the protocol adapter"""
        self.message_count = 0
        self.parse_errors = 0

    def parse_android_message(self, message: str) -> Optional[ProtocolMessage]:
        """
        Parse Android's text-based protocol message.
        
        Format: COMMAND key1=value1 key2=value2 ...
        Supports quoted values: key="quoted value"
        Supports array syntax: sensors=[A,B,C]
        
        Args:
            message: Raw text message from Android
            
        Returns:
            ProtocolMessage object or None if parsing fails
        """
        try:
            message = message.strip()
            if not message:
                return None

            # Split into command and parameters
            parts = message.split(' ', 1)
            message_type = parts[0]

            # Parse parameters
            parameters = {}
            if len(parts) > 1:
                parameters = self._parse_parameters(parts[1])

            return ProtocolMessage(
                type=message_type,
                parameters=parameters,
                raw=message
            )

        except Exception as e:
            self.parse_errors += 1
            print(f"Error parsing Android message '{message}': {e}")
            return None

    def _parse_parameters(self, param_string: str) -> Dict[str, str]:
        """
        Parse key=value parameters from text.
        
        Handles:
        - Simple: key=value
        - Quoted: key="quoted value"
        - Arrays: sensors=[A,B,C]
        """
        params = {}

        # Regex to match key=value, key="value", or key=[array]
        pattern = r'(\w+)=("([^"]*)"|(\[[^\]]*\])|([^\s]+))'

        for match in re.finditer(pattern, param_string):
            key = match.group(1)

            # Check which group captured the value
            if match.group(3) is not None:  # Quoted value
                value = match.group(3)
            elif match.group(4) is not None:  # Array value
                value = match.group(4)
            else:  # Unquoted value
                value = match.group(5)

            params[key] = value

        return params

    def android_to_json(self, android_message: str) -> Optional[Dict[str, Any]]:
        """
        Convert Android text message to JSON format for PC processing.
        
        Args:
            android_message: Text message from Android
            
        Returns:
            JSON-compatible dict or None if parsing fails
        """
        parsed = self.parse_android_message(android_message)
        if not parsed:
            return None

        self.message_count += 1

        # Build JSON message
        json_msg = {
            'type': parsed.type,
            'timestamp': time.time()
        }

        # Add all parameters as JSON fields
        for key, value in parsed.parameters.items():
            # Convert array syntax [A,B,C] to list
            if value.startswith('[') and value.endswith(']'):
                items = value[1:-1].split(',')
                json_msg[key] = [item.strip() for item in items if item.strip()]
            else:
                # Try to convert to number if possible
                try:
                    if '.' in value:
                        json_msg[key] = float(value)
                    else:
                        json_msg[key] = int(value)
                except (ValueError, AttributeError):
                    json_msg[key] = value

        return json_msg

    def json_to_android(self, json_msg: Dict[str, Any]) -> str:
        """
        Convert JSON message to Android text protocol format.
        
        Args:
            json_msg: JSON message dict
            
        Returns:
            Text protocol message string
        """
        # Get message type
        msg_type = json_msg.get('type', 'UNKNOWN')

        # Convert to Android naming convention (uppercase)
        if msg_type in self.PC_TO_ANDROID_TYPES:
            msg_type = self.PC_TO_ANDROID_TYPES[msg_type]
        else:
            msg_type = msg_type.upper()

        # Build parameter string
        params = []
        for key, value in json_msg.items():
            if key in ['type', 'timestamp']:
                continue

            # Format value
            if isinstance(value, list):
                formatted_value = '[' + ','.join(str(v) for v in value) + ']'
            elif isinstance(value, str) and ' ' in value:
                formatted_value = f'"{value}"'
            else:
                formatted_value = str(value)

            params.append(f'{key}={formatted_value}')

        # Combine
        if params:
            return f"{msg_type} {' '.join(params)}"
        else:
            return msg_type

    def create_hello_response(self, device_id: str) -> str:
        """Create ACK response for HELLO message"""
        return f"ACK cmd=HELLO device_id={device_id}"

    def create_sync_result(self, t1: int, t2: int, t3: int, offset_ms: int, rtt_ms: int) -> str:
        """
        Create SYNC_RESULT message for Android.
        
        Args:
            t1: PC timestamp when sync request sent (ms)
            t2: Android timestamp when sync response received (ms)
            t3: PC timestamp when sync response received (ms)
            offset_ms: Calculated clock offset (ms)
            rtt_ms: Round-trip time (ms)
        """
        return f"SYNC_RESULT t1={t1} t2={t2} t3={t3} offset={offset_ms} rtt={rtt_ms}"

    def create_ack(self, command: str, **kwargs) -> str:
        """
        Create ACK message for successful command execution.
        
        Args:
            command: Command being acknowledged
            **kwargs: Additional parameters to include
        """
        params = [f"cmd={command}"]
        for key, value in kwargs.items():
            params.append(f"{key}={value}")
        return f"ACK {' '.join(params)}"

    def create_error(self, command: str, error_code: str, message: str) -> str:
        """
        Create ERROR message.
        
        Args:
            command: Command that failed
            error_code: Error code (FAIL, BUSY, etc.)
            message: Human-readable error message
        """
        return f'ERROR cmd={command} code={error_code} msg="{message}"'

    def get_stats(self) -> Dict[str, int]:
        """Get protocol adapter statistics"""
        return {
            'messages_processed': self.message_count,
            'parse_errors': self.parse_errors
        }


# Global adapter instance
_adapter = ProtocolAdapter()


def parse_android(message: str) -> Optional[Dict[str, Any]]:
    """Parse Android text message to JSON (convenience function)"""
    return _adapter.android_to_json(message)


def format_android(json_msg: Dict[str, Any]) -> str:
    """Format JSON message as Android text (convenience function)"""
    return _adapter.json_to_android(json_msg)


def get_adapter() -> ProtocolAdapter:
    """Get global protocol adapter instance"""
    return _adapter


if __name__ == '__main__':
    # Test the protocol adapter
    adapter = ProtocolAdapter()

    print("Testing Protocol Adapter")
    print("=" * 70)

    # Test 1: Parse HELLO
    print("\nTest 1: Parse HELLO message")
    android_hello = 'HELLO device_name=android_001 sensors=[GSR,RGB,THERMAL]'
    json_hello = adapter.android_to_json(android_hello)
    print(f"Android: {android_hello}")
    print(f"JSON:    {json.dumps(json_hello, indent=2)}")

    # Test 2: Parse START_RECORD
    print("\nTest 2: Parse START_RECORD message")
    android_start = 'START_RECORD session_id=session_20240101_120000'
    json_start = adapter.android_to_json(android_start)
    print(f"Android: {android_start}")
    print(f"JSON:    {json.dumps(json_start, indent=2)}")

    # Test 3: Parse DATA_GSR
    print("\nTest 3: Parse DATA_GSR message")
    android_gsr = 'DATA_GSR ts=1234567890 value=5.5'
    json_gsr = adapter.android_to_json(android_gsr)
    print(f"Android: {android_gsr}")
    print(f"JSON:    {json.dumps(json_gsr, indent=2)}")

    # Test 4: Parse SYNC_RESPONSE
    print("\nTest 4: Parse SYNC_RESPONSE message")
    android_sync = 'SYNC_RESPONSE t_pc=1234567890 t_ph=1234567895'
    json_sync = adapter.android_to_json(android_sync)
    print(f"Android: {android_sync}")
    print(f"JSON:    {json.dumps(json_sync, indent=2)}")

    # Test 5: Create ACK
    print("\nTest 5: Create ACK message")
    ack = adapter.create_ack('START_RECORD', session_id='session_123')
    print(f"ACK:     {ack}")

    # Test 6: Create SYNC_RESULT
    print("\nTest 6: Create SYNC_RESULT message")
    sync_result = adapter.create_sync_result(1000, 1005, 1010, 5, 10)
    print(f"SYNC:    {sync_result}")

    # Test 7: Create ERROR
    print("\nTest 7: Create ERROR message")
    error = adapter.create_error('START_RECORD', 'SENSOR_FAIL', 'GSR sensor not connected')
    print(f"ERROR:   {error}")

    # Test 8: JSON to Android
    print("\nTest 8: Convert JSON to Android format")
    json_msg = {
        'type': 'START_RECORD',
        'session_id': 'test_session',
        'timestamp': 1234567890
    }
    android_msg = adapter.json_to_android(json_msg)
    print(f"JSON:    {json.dumps(json_msg)}")
    print(f"Android: {android_msg}")

    print("\n" + "=" * 70)
    print(f"Statistics: {adapter.get_stats()}")
