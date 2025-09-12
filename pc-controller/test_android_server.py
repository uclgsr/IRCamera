#!/usr/bin/env python3
"""
Test script to validate that the Android NetworkServer implementation works correctly.

This script simulates the PC Controller side and tests all the commands that the
NetworkServer should handle.
"""

import json
import socket
import threading
import time
from typing import Any, Dict, Optional


class AndroidServerTester:
    """Test client to validate Android NetworkServer implementation"""

    def __init__(self, android_ip: str = "127.0.0.1", port: int = 8080):
        self.android_ip = android_ip
        self.port = port
        self.socket = None
        self.connected = False

    def connect_to_android(self) -> bool:
        """Connect to Android NetworkServer"""
        try:
            print(
                f"🔌 Connecting to Android NetworkServer at {self.android_ip}:{self.port}"
            )

            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)

            self.socket.connect((self.android_ip, self.port))
            self.connected = True

            print("✅ Successfully connected to Android NetworkServer!")
            return True

        except ConnectionRefusedError:
            print("❌ Connection refused - Android NetworkServer may not be running")
            return False
        except socket.timeout:
            print("❌ Connection timeout - Check server is running and accessible")
            return False
        except Exception as e:
            print(f"❌ Connection error: {e}")
            return False

    def send_message(self, message: Dict[str, Any]) -> bool:
        """Send message using NetworkServer protocol (4-byte length + JSON)"""
        if not self.connected or not self.socket:
            print("❌ Not connected to Android NetworkServer")
            return False

        try:
            message_json = json.dumps(message)
            message_bytes = message_json.encode("utf-8")

            # Send length first (4 bytes, big-endian) then message
            self.socket.send(len(message_bytes).to_bytes(4, byteorder="big"))
            self.socket.send(message_bytes)

            print(f"📤 Sent: {message_json}")
            return True

        except Exception as e:
            print(f"❌ Error sending message: {e}")
            self.connected = False
            return False

    def receive_message(self, timeout: float = 5.0) -> Optional[Dict[str, Any]]:
        """Receive message using NetworkServer protocol"""
        if not self.connected or not self.socket:
            return None

        try:
            self.socket.settimeout(timeout)

            # Read message length (4 bytes, big-endian)
            length_bytes = self.socket.recv(4)
            if len(length_bytes) != 4:
                return None

            message_length = int.from_bytes(length_bytes, byteorder="big")

            # Read the message
            message_bytes = b""
            while len(message_bytes) < message_length:
                chunk = self.socket.recv(message_length - len(message_bytes))
                if not chunk:
                    break
                message_bytes += chunk

            if len(message_bytes) == message_length:
                message_json = message_bytes.decode("utf-8")
                message = json.loads(message_json)
                print(f"📥 Received: {message_json}")
                return message

        except socket.timeout:
            print("⏰ Timeout waiting for response")
        except Exception as e:
            print(f"❌ Error receiving message: {e}")

        return None

    def test_device_registration(self) -> bool:
        """Test enhanced device registration"""
        print("\n🔍 Testing device registration...")

        registration_message = {
            "message_type": "enhanced_device_registration",
            "device_id": "pc_controller_test",
            "capabilities": {
                "recording_coordination": True,
                "time_synchronization": True,
                "command_processing": True,
            },
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        if self.send_message(registration_message):
            response = self.receive_message()

            if response and response.get("message_type") == "enhanced_registration_ack":
                print("✅ Device registration successful!")
                return True
            else:
                print(
                    f"❌ Device registration failed - unexpected response: {response}"
                )
                return False

        return False

    def test_ping_pong(self) -> bool:
        """Test ping/pong communication"""
        print("\n🏓 Testing ping/pong...")

        ping_message = {
            "message_type": "ping",
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        if self.send_message(ping_message):
            response = self.receive_message()

            if response and response.get("message_type") == "pong":
                print("✅ Ping/pong successful!")
                return True
            else:
                print(f"❌ Ping/pong failed - unexpected response: {response}")
                return False

        return False

    def test_recording_control(self) -> bool:
        """Test remote recording start/stop"""
        print("\n🎬 Testing recording control...")

        # Test start command
        session_dir = f"/tmp/test_session_{int(time.time())}"
        start_command = {
            "message_type": "session_start_command",
            "session_directory": session_dir,
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        success = True

        if self.send_message(start_command):
            response = self.receive_message()
            if response and response.get("message_type") == "session_start_response":
                print("✅ Recording start successful!")
            else:
                print(f"❌ Recording start failed - unexpected response: {response}")
                success = False
        else:
            success = False

        # Wait a bit
        time.sleep(1)

        # Test stop command
        stop_command = {
            "message_type": "session_stop_command",
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        if self.send_message(stop_command):
            response = self.receive_message()
            if response and response.get("message_type") == "session_stop_response":
                print("✅ Recording stop successful!")
            else:
                print(f"❌ Recording stop failed - unexpected response: {response}")
                success = False
        else:
            success = False

        return success

    def test_sync_marker(self) -> bool:
        """Test sync marker command"""
        print("\n🔄 Testing sync marker...")

        sync_command = {
            "message_type": "sync_marker_command",
            "marker_type": "test_sync_event",
            "timestamp_ns": int(time.time() * 1_000_000_000),
            "metadata": {
                "source": "pc_controller_test",
                "event_type": "validation_test",
            },
        }

        if self.send_message(sync_command):
            response = self.receive_message()

            if response and response.get("message_type") == "sync_marker_response":
                print("✅ Sync marker successful!")
                return True
            else:
                print(f"❌ Sync marker failed - unexpected response: {response}")
                return False

        return False

    def test_status_request(self) -> bool:
        """Test status request"""
        print("\n📊 Testing status request...")

        status_request = {
            "message_type": "status_request",
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        if self.send_message(status_request):
            response = self.receive_message()

            if response and response.get("message_type") == "status_response":
                print(f"✅ Status request successful! Response: {response}")
                return True
            else:
                print(f"❌ Status request failed - unexpected response: {response}")
                return False

        return False

    def disconnect(self):
        """Disconnect from Android NetworkServer"""
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        self.connected = False
        print("🔌 Disconnected from Android NetworkServer")


def main():
    print("🧪 Android NetworkServer Validation Test")
    print("=" * 50)

    tester = AndroidServerTester()

    try:
        # Connect
        if not tester.connect_to_android():
            print("\n❌ Cannot proceed without connection to Android NetworkServer")
            print(
                "\nNote: This test expects the Android app NetworkServer to be running."
            )
            print(
                "In the actual implementation, the NetworkServer starts automatically"
            )
            print("when the RecordingService is initialized.")
            return 1

        # Run tests
        tests = [
            ("Device Registration", tester.test_device_registration),
            ("Ping/Pong", tester.test_ping_pong),
            ("Recording Control", tester.test_recording_control),
            ("Sync Marker", tester.test_sync_marker),
            ("Status Request", tester.test_status_request),
        ]

        passed = 0
        total = len(tests)

        for test_name, test_func in tests:
            try:
                if test_func():
                    passed += 1
                else:
                    print(f"❌ {test_name} test failed")
            except Exception as e:
                print(f"❌ {test_name} test error: {e}")

        print(f"\n📊 Test Results: {passed}/{total} tests passed")

        if passed == total:
            print(
                "🎉 All tests passed! NetworkServer implementation is working correctly."
            )
            return 0
        else:
            print("⚠️  Some tests failed. Check Android NetworkServer implementation.")
            return 1

    except KeyboardInterrupt:
        print("\n⏹️  Test interrupted by user")
        return 1

    finally:
        tester.disconnect()


if __name__ == "__main__":
    exit(main())
