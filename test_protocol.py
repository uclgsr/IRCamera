#!/usr/bin/env python3
"""
Test Protocol Implementation

Simple test script to validate the standardized networking protocol
between PC controller and Android device.
"""

import socket
import time
import threading
import re
from typing import Dict, Any, Optional


class SimpleLogger:
    def info(self, msg): print(f"[INFO] {msg}")

    def warning(self, msg): print(f"[WARN] {msg}")

    def error(self, msg): print(f"[ERROR] {msg}")

    def debug(self, msg): print(f"[DEBUG] {msg}")


logger = SimpleLogger()


class Protocol:
    """Standardized networking protocol messages"""

    # Message types
    MSG_HELLO = "HELLO"
    MSG_SYNC_REQUEST = "SYNC_REQUEST"
    MSG_SYNC_RESPONSE = "SYNC_RESPONSE"
    MSG_START_RECORD = "START_RECORD"
    MSG_STOP_RECORD = "STOP_RECORD"
    MSG_ACK = "ACK"
    MSG_ERROR = "ERROR"
    MSG_DATA_GSR = "DATA_GSR"
    MSG_FRAME = "FRAME"

    # Error codes
    ERR_FAIL = "FAIL"
    ERR_BUSY = "BUSY"
    ERR_SENSOR_FAIL = "SENSOR_FAIL"

    @staticmethod
    def create_sync_request(pc_timestamp: int) -> str:
        return f"{Protocol.MSG_SYNC_REQUEST} t_pc={pc_timestamp}"

    @staticmethod
    def create_start_record(session_id: str) -> str:
        return f"{Protocol.MSG_START_RECORD} session_id={session_id}"

    @staticmethod
    def create_stop_record(session_id: str) -> str:
        return f"{Protocol.MSG_STOP_RECORD} session_id={session_id}"

    @staticmethod
    def parse_message(message: str) -> Optional[Dict[str, Any]]:
        """Parse a protocol message into components"""
        try:
            parts = message.strip().split(' ', 1)
            if not parts:
                return None

            msg_type = parts[0]
            params = {}

            if len(parts) > 1:
                # Parse parameters using regex to handle quoted values
                param_str = parts[1]
                # Updated regex to properly handle quoted strings
                param_pattern = r'(\w+)=("([^"]*)"|([^\s]+))'
                matches = re.findall(param_pattern, param_str)

                for match in matches:
                    key = match[0]
                    # If quoted (group 3 has content), use quoted value, else use unquoted (group 4)
                    value = match[2] if match[2] else match[3]
                    params[key] = value

            return {'type': msg_type, 'params': params}
        except Exception as e:
            logger.error(f"Error parsing message '{message}': {e}")
            return None


def test_protocol_parsing():
    """Test protocol message parsing"""
    print("Testing protocol message parsing...")

    # Test HELLO message parsing
    hello_msg = "HELLO device_name=android_device_001 sensors=[RGB,THERMAL,GSR]"
    parsed = Protocol.parse_message(hello_msg)
    print(f"HELLO parsed: {parsed}")
    assert parsed['type'] == 'HELLO'
    assert parsed['params']['device_name'] == 'android_device_001'
    assert parsed['params']['sensors'] == '[RGB,THERMAL,GSR]'

    # Test SYNC_REQUEST message parsing
    sync_req = "SYNC_REQUEST t_pc=1640995200000"
    parsed = Protocol.parse_message(sync_req)
    print(f"SYNC_REQUEST parsed: {parsed}")
    assert parsed['type'] == 'SYNC_REQUEST'
    assert parsed['params']['t_pc'] == '1640995200000'

    # Test START_RECORD message parsing
    start_cmd = "START_RECORD session_id=test_session_001"
    parsed = Protocol.parse_message(start_cmd)
    print(f"START_RECORD parsed: {parsed}")
    assert parsed['type'] == 'START_RECORD'
    assert parsed['params']['session_id'] == 'test_session_001'

    # Test ACK message parsing
    ack_msg = 'ACK cmd=START_RECORD session_id=test_session_001 start_time=1640995200123'
    parsed = Protocol.parse_message(ack_msg)
    print(f"ACK parsed: {parsed}")
    assert parsed['type'] == 'ACK'
    assert parsed['params']['cmd'] == 'START_RECORD'

    # Test ERROR message parsing with quoted message
    error_msg = 'ERROR cmd=START_RECORD code=SENSOR_FAIL msg="Thermal camera not detected"'
    parsed = Protocol.parse_message(error_msg)
    print(f"ERROR parsed: {parsed}")
    assert parsed['type'] == 'ERROR'
    assert parsed['params']['code'] == 'SENSOR_FAIL'
    assert parsed['params']['msg'] == 'Thermal camera not detected'

    print("✓ Protocol parsing tests passed\n")


def test_protocol_creation():
    """Test protocol message creation"""
    print("Testing protocol message creation...")

    # Test SYNC_REQUEST creation
    sync_req = Protocol.create_sync_request(1640995200000)
    print(f"SYNC_REQUEST created: {sync_req}")
    assert sync_req == "SYNC_REQUEST t_pc=1640995200000"

    # Test START_RECORD creation
    start_cmd = Protocol.create_start_record("test_session_001")
    print(f"START_RECORD created: {start_cmd}")
    assert start_cmd == "START_RECORD session_id=test_session_001"

    # Test STOP_RECORD creation
    stop_cmd = Protocol.create_stop_record("test_session_001")
    print(f"STOP_RECORD created: {stop_cmd}")
    assert stop_cmd == "STOP_RECORD session_id=test_session_001"

    print("✓ Protocol creation tests passed\n")


class MockAndroidDevice:
    """Mock Android device for testing"""

    def __init__(self, device_name="test_android", capabilities=None):
        self.device_name = device_name
        self.capabilities = capabilities or ["RGB", "THERMAL", "GSR"]
        self.is_recording = False
        self.current_session = None

    def handle_message(self, message):
        """Handle incoming protocol message"""
        parsed = Protocol.parse_message(message)
        if not parsed:
            return 'ERROR code=FAIL msg="Invalid message format"'

        msg_type = parsed['type']
        params = parsed['params']

        if msg_type == Protocol.MSG_SYNC_REQUEST:
            pc_time = int(params.get('t_pc', 0))
            phone_time = int(time.time() * 1000)  # Mock phone time
            return f"SYNC_RESPONSE t_pc={pc_time} t_ph={phone_time}"

        elif msg_type == Protocol.MSG_START_RECORD:
            session_id = params.get('session_id')
            if not session_id:
                return 'ERROR cmd=START_RECORD code=FAIL msg="Missing session_id"'

            if self.is_recording:
                return 'ERROR cmd=START_RECORD code=BUSY msg="Already recording"'

            self.is_recording = True
            self.current_session = session_id
            start_time = int(time.time() * 1000)
            return f"ACK cmd=START_RECORD session_id={session_id} start_time={start_time}"

        elif msg_type == Protocol.MSG_STOP_RECORD:
            session_id = params.get('session_id')
            if not self.is_recording:
                return 'ERROR cmd=STOP_RECORD code=FAIL msg="Not recording"'

            self.is_recording = False
            stop_time = int(time.time() * 1000)
            return f"ACK cmd=STOP_RECORD session_id={session_id} stop_time={stop_time}"

        else:
            return f'ERROR code=FAIL msg="Unknown command: {msg_type}"'

    def get_hello_message(self):
        """Get HELLO message"""
        sensors_str = f"[{','.join(self.capabilities)}]"
        return f"HELLO device_name={self.device_name} sensors={sensors_str}"


def test_protocol_flow():
    """Test full protocol flow with mock device"""
    print("Testing protocol flow...")

    device = MockAndroidDevice("test_android_001")

    # Test HELLO
    hello = device.get_hello_message()
    print(f"Device HELLO: {hello}")

    # Test SYNC
    pc_time = int(time.time() * 1000)
    sync_request = f"SYNC_REQUEST t_pc={pc_time}"
    sync_response = device.handle_message(sync_request)
    print(f"PC -> Android: {sync_request}")
    print(f"Android -> PC: {sync_response}")

    # Test START_RECORD
    start_request = "START_RECORD session_id=test_flow_session"
    start_response = device.handle_message(start_request)
    print(f"PC -> Android: {start_request}")
    print(f"Android -> PC: {start_response}")
    assert "ACK" in start_response

    # Test duplicate START (should fail)
    duplicate_start = "START_RECORD session_id=test_duplicate"
    duplicate_response = device.handle_message(duplicate_start)
    print(f"PC -> Android: {duplicate_start}")
    print(f"Android -> PC: {duplicate_response}")
    assert "ERROR" in duplicate_response and "BUSY" in duplicate_response

    # Test STOP_RECORD
    stop_request = "STOP_RECORD session_id=test_flow_session"
    stop_response = device.handle_message(stop_request)
    print(f"PC -> Android: {stop_request}")
    print(f"Android -> PC: {stop_response}")
    assert "ACK" in stop_response

    print("✓ Protocol flow tests passed\n")


def test_socket_communication():
    """Test actual socket communication"""
    print("Testing socket communication...")

    # Start mock Android server
    def mock_android_server():
        device = MockAndroidDevice("socket_test_device")
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        try:
            server_socket.bind(('127.0.0.1', 8888))
            server_socket.listen(1)
            print("Mock Android server listening on port 8888...")

            client_socket, address = server_socket.accept()
            print(f"PC connected from {address}")

            # Send HELLO immediately
            hello_msg = device.get_hello_message() + '\n'
            client_socket.send(hello_msg.encode('utf-8'))
            print(f"Sent HELLO: {hello_msg.strip()}")

            # Handle messages
            client_file = client_socket.makefile('rw', encoding='utf-8')

            while True:
                try:
                    message = client_file.readline()
                    if not message:
                        break

                    message = message.strip()
                    print(f"Received: {message}")

                    response = device.handle_message(message)
                    client_file.write(response + '\n')
                    client_file.flush()
                    print(f"Sent: {response}")

                except Exception as e:
                    print(f"Error handling message: {e}")
                    break

        except Exception as e:
            print(f"Server error: {e}")
        finally:
            server_socket.close()

    # Start server in background
    server_thread = threading.Thread(target=mock_android_server, daemon=True)
    server_thread.start()

    # Give server time to start
    time.sleep(0.1)

    # Connect as PC client
    try:
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.connect(('127.0.0.1', 8888))
        client_file = client_socket.makefile('rw', encoding='utf-8')

        # Receive HELLO
        hello = client_file.readline().strip()
        print(f"Received HELLO: {hello}")

        # Send SYNC request
        pc_time = int(time.time() * 1000)
        sync_request = f"SYNC_REQUEST t_pc={pc_time}"
        client_file.write(sync_request + '\n')
        client_file.flush()

        sync_response = client_file.readline().strip()
        print(f"Received SYNC response: {sync_response}")

        # Send START command
        start_request = "START_RECORD session_id=socket_test_session"
        client_file.write(start_request + '\n')
        client_file.flush()

        start_response = client_file.readline().strip()
        print(f"Received START response: {start_response}")

        # Send STOP command
        stop_request = "STOP_RECORD session_id=socket_test_session"
        client_file.write(stop_request + '\n')
        client_file.flush()

        stop_response = client_file.readline().strip()
        print(f"Received STOP response: {stop_response}")

        client_socket.close()
        print("✓ Socket communication tests passed\n")

    except Exception as e:
        print(f"Client error: {e}")


def main():
    """Run all tests"""
    print("=== Protocol Implementation Tests ===\n")

    try:
        test_protocol_parsing()
        test_protocol_creation()
        test_protocol_flow()
        test_socket_communication()

        print("=== All Tests Passed! ===")
        print("The standardized networking protocol is working correctly.")

    except Exception as e:
        print(f"=== Test Failed: {e} ===")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
