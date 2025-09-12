#!/usr/bin/env python3
"""
Simple test script to validate PC-to-Phone communication for IRCamera project.

This script demonstrates the basic communication protocol between the PC Controller
and Android Sensor Node (Spoke) as described in the issue.

Usage:
    python test_pc_to_phone.py --android-ip 192.168.1.100 --port 8080
"""

import argparse
import json
import socket
import threading
import time
from typing import Any, Dict


class PCControllerTest:
    """Simple PC Controller test for Android communication"""

    def __init__(self, android_ip: str, port: int = 8080):
        self.android_ip = android_ip
        self.port = port
        self.socket = None
        self.connected = False

    def connect_to_android(self) -> bool:
        """Attempt to connect to Android device"""
        try:
            print(
                f"Attempting to connect to Android device at {self.android_ip}:{self.port}"
            )

            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)  # 10 second timeout

            self.socket.connect((self.android_ip, self.port))
            self.connected = True

            print("✅ Successfully connected to Android device!")
            return True

        except ConnectionRefusedError:
            print(
                "❌ Connection refused - Android app may not be running or not listening on this port"
            )
            return False
        except socket.timeout:
            print("❌ Connection timeout - Check IP address and network connectivity")
            return False
        except Exception as e:
            print(f"❌ Connection error: {e}")
            return False

    def send_message(self, message: Dict[str, Any]) -> bool:
        """Send a JSON message to Android device"""
        if not self.connected or not self.socket:
            print("❌ Not connected to Android device")
            return False

        try:
            message_json = json.dumps(message)
            message_bytes = message_json.encode("utf-8")

            # Send message length first (4 bytes)
            self.socket.send(len(message_bytes).to_bytes(4, byteorder="big"))
            # Then send the message
            self.socket.send(message_bytes)

            print(f"📤 Sent: {message_json}")
            return True

        except Exception as e:
            print(f"❌ Error sending message: {e}")
            self.connected = False
            return False

    def receive_message(self) -> Dict[str, Any]:
        """Receive a JSON message from Android device"""
        if not self.connected or not self.socket:
            return {}

        try:
            # Read message length first
            length_bytes = self.socket.recv(4)
            if len(length_bytes) != 4:
                return {}

            message_length = int.from_bytes(length_bytes, byteorder="big")

            # Read the full message
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

        except Exception as e:
            print(f"❌ Error receiving message: {e}")

        return {}

    def test_device_registration(self) -> bool:
        """Test device registration protocol"""
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
            # Wait for acknowledgment
            time.sleep(1)
            response = self.receive_message()

            if response and response.get("message_type") == "enhanced_registration_ack":
                print("✅ Device registration successful!")
                return True
            else:
                print("❌ Device registration failed - no acknowledgment received")
                return False

        return False

    def test_start_recording(self) -> bool:
        """Test remote recording start command"""
        print("\n🎬 Testing remote recording start...")

        session_dir = f"/storage/emulated/0/Android/data/com.topdon.tc001/files/test_session_{int(time.time())}"

        start_command = {
            "message_type": "session_start_command",
            "session_directory": session_dir,
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        return self.send_message(start_command)

    def test_stop_recording(self) -> bool:
        """Test remote recording stop command"""
        print("\n⏹️  Testing remote recording stop...")

        stop_command = {
            "message_type": "session_stop_command",
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        return self.send_message(stop_command)

    def test_sync_marker(self) -> bool:
        """Test sync marker command"""
        print("\n🔄 Testing sync marker...")

        sync_command = {
            "message_type": "sync_marker_command",
            "marker_type": "test_sync_event",
            "timestamp_ns": int(time.time() * 1_000_000_000),
            "metadata": {"source": "pc_controller_test", "event_type": "manual_test"},
        }

        return self.send_message(sync_command)

    def test_ping(self) -> bool:
        """Test ping/pong communication"""
        print("\n🏓 Testing ping/pong...")

        ping_message = {
            "message_type": "ping",
            "device_id": "pc_controller_test",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        if self.send_message(ping_message):
            time.sleep(1)
            response = self.receive_message()

            if response and response.get("message_type") == "pong":
                print("✅ Ping/pong successful!")
                return True
            else:
                print("❌ No pong response received")
                return False

        return False

    def disconnect(self):
        """Disconnect from Android device"""
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        self.connected = False
        print("🔌 Disconnected from Android device")


def main():
    parser = argparse.ArgumentParser(
        description="Test PC-to-Phone communication for IRCamera"
    )
    parser.add_argument(
        "--android-ip", required=True, help="IP address of Android device"
    )
    parser.add_argument(
        "--port", type=int, default=8080, help="Port number (default: 8080)"
    )
    parser.add_argument(
        "--test",
        choices=["all", "connect", "register", "record", "sync", "ping", "stress"],
        default="all",
        help="Which test to run",
    )

    args = parser.parse_args()

    controller = PCControllerTest(args.android_ip, args.port)

    try:
        # Connect to Android device
        if not controller.connect_to_android():
            print("\n❌ Cannot proceed without connection to Android device")
            print("\nTroubleshooting:")
            print("1. Make sure IRCamera app is running on Android device")
            print("2. Check that RecordingService is started")
            print("3. Verify IP address and network connectivity")
            print("4. Check firewall settings on both devices")
            return 1

        print(f"\n🧪 Running test: {args.test}")

        success_count = 0
        total_tests = 0

        if args.test in ["all", "register"]:
            total_tests += 1
            if controller.test_device_registration():
                success_count += 1

        if args.test in ["all", "ping"]:
            total_tests += 1
            if controller.test_ping():
                success_count += 1

        if args.test in ["all", "sync"]:
            total_tests += 1
            if controller.test_sync_marker():
                success_count += 1

        if args.test in ["all", "record"]:
            total_tests += 2  # start + stop
            if controller.test_start_recording():
                success_count += 1

                # Wait a bit before stopping
                print("⏳ Waiting 3 seconds before stopping recording...")
                time.sleep(3)

                if controller.test_stop_recording():
                    success_count += 1

        if args.test in ["all", "stress"]:
            print("🔥 Testing stress communication (20 rapid ping/pong cycles)...")
            total_tests += 1
            stress_success = True

            for i in range(20):
                if not controller.test_ping_pong():
                    stress_success = False
                    break
                time.sleep(0.1)  # Brief delay between stress tests

            if stress_success:
                success_count += 1
                print("✅ Stress test passed!")
            else:
                print("❌ Stress test failed!")

        if args.test == "connect":
            print("✅ Connection test successful!")
            success_count = 1
            total_tests = 1

        print(f"\n📊 Test Results: {success_count}/{total_tests} tests passed")

        if success_count == total_tests:
            print("🎉 All tests passed! PC-to-Phone communication is working.")
            return 0
        else:
            print("⚠️  Some tests failed. Check Android app logs for details.")
            return 1

    except KeyboardInterrupt:
        print("\n⏹️  Test interrupted by user")
        return 1

    finally:
        controller.disconnect()


if __name__ == "__main__":
    exit(main())
