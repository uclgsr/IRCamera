#!/usr/bin/env python3
"""
Simple PC client to test Android phone preview streaming.

This script connects to the Android app's TCP server and receives live preview 
data including camera frames and sensor readings.
"""

import json
import socket
import struct
import base64
import time
import sys
from threading import Thread
from pathlib import Path
from typing import Optional, Dict, Any
import argparse


class AndroidPreviewClient:
    """Simple client to connect to Android preview streaming server."""

    def __init__(self, host: str, port: int = 8080):
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None
        self.connected = False
        self.running = False

        # Statistics
        self.frames_received = 0
        self.sensor_messages_received = 0
        self.bytes_received = 0
        self.start_time = time.time()

    def connect(self) -> bool:
        """Connect to Android server."""
        try:
            print(f"Connecting to Android device at {self.host}:{self.port}...")
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)  # 10 second timeout
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"Connected successfully!")
            return True
        except Exception as e:
            print(f"Failed to connect: {e}")
            return False

    def disconnect(self):
        """Disconnect from server."""
        self.running = False
        self.connected = False
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
            self.socket = None
        print("Disconnected from Android device")

    def send_message(self, message: Dict[str, Any]) -> bool:
        """Send JSON message to Android device."""
        if not self.connected or not self.socket:
            return False

        try:
            message_json = json.dumps(message)
            message_bytes = message_json.encode('utf-8')

            # Send length-prefixed message (same format as Android)
            self.socket.send(struct.pack('!I', len(message_bytes)))
            self.socket.send(message_bytes)

            print(f"Sent command: {message.get('message_type', 'unknown')}")
            return True
        except Exception as e:
            print(f"Failed to send message: {e}")
            return False

    def receive_message(self) -> Optional[Dict[str, Any]]:
        """Receive JSON message from Android device."""
        if not self.connected or not self.socket:
            return None

        try:
            # Read message length
            length_data = self.socket.recv(4)
            if len(length_data) != 4:
                return None

            message_length = struct.unpack('!I', length_data)[0]
            if message_length > 10 * 1024 * 1024:  # 10MB limit
                print(f"Message too large: {message_length} bytes")
                return None

            # Read message data
            message_data = b''
            while len(message_data) < message_length:
                chunk = self.socket.recv(message_length - len(message_data))
                if not chunk:
                    return None
                message_data += chunk

            self.bytes_received += len(message_data) + 4

            # Parse JSON
            message_json = message_data.decode('utf-8')
            return json.loads(message_json)

        except socket.timeout:
            return None  # Normal timeout
        except Exception as e:
            print(f"Error receiving message: {e}")
            return None

    def handle_preview_frame(self, message: Dict[str, Any]):
        """Handle received preview frame."""
        frame_type = message.get('frame_type', 'unknown')
        width = message.get('width', 0)
        height = message.get('height', 0)
        data_size = message.get('data_size_bytes', 0)

        # Optionally save frame to file
        if message.get('data_base64'):
            try:
                frame_data = base64.b64decode(message['data_base64'])
                filename = f"preview_{frame_type}_{int(time.time())}.jpg"
                with open(filename, 'wb') as f:
                    f.write(frame_data)
                print(
                    f"Saved {frame_type} frame: {width}x{height} ({data_size} bytes) -> {filename}")
            except Exception as e:
                print(f"Failed to save {frame_type} frame: {e}")

        self.frames_received += 1

    def handle_sensor_data(self, message: Dict[str, Any]):
        """Handle received sensor data."""
        data = message.get('data', {})
        gsr_value = data.get('gsr_microsiemens')
        recording_status = data.get('recording_status', 'unknown')

        print(f"Sensor data - GSR: {gsr_value}µS, Status: {recording_status}")
        self.sensor_messages_received += 1

    def print_statistics(self):
        """Print reception statistics."""
        elapsed = time.time() - self.start_time
        if elapsed > 0:
            frame_rate = self.frames_received / elapsed
            data_rate = self.bytes_received / elapsed / 1024  # KB/s
            print(f"\nStatistics:")
            print(f"  Connected for: {elapsed:.1f}s")
            print(f"  Frames received: {self.frames_received} ({frame_rate:.1f} fps)")
            print(f"  Sensor messages: {self.sensor_messages_received}")
            print(f"  Data rate: {data_rate:.1f} KB/s")

    def run_listener(self):
        """Main message listening loop."""
        self.running = True
        print("Listening for messages from Android device...")

        while self.running and self.connected:
            message = self.receive_message()
            if message is None:
                continue

            message_type = message.get('message_type', 'unknown')

            if message_type == 'preview_frame':
                self.handle_preview_frame(message)
            elif message_type == 'sensor_data':
                self.handle_sensor_data(message)
            else:
                print(f"Received {message_type}: {message}")

    def send_commands(self):
        """Send test commands to Android device."""
        print("\nSending test commands...")

        # Send device registration
        self.send_message({
            "message_type": "enhanced_device_registration",
            "timestamp_ns": int(time.time() * 1_000_000_000)
        })

        time.sleep(1)

        # Request status
        self.send_message({
            "message_type": "status_request",
            "timestamp_ns": int(time.time() * 1_000_000_000)
        })

        time.sleep(1)

        # Configure preview streaming
        self.send_message({
            "message_type": "configure_preview_streaming",
            "frame_interval_ms": 1000,
            "sensor_interval_ms": 1000,
            "preview_width": 320,
            "preview_height": 240,
            "jpeg_quality": 70,
            "timestamp_ns": int(time.time() * 1_000_000_000)
        })

        time.sleep(1)

        # Start preview streaming
        self.send_message({
            "message_type": "start_preview_streaming",
            "timestamp_ns": int(time.time() * 1_000_000_000)
        })


def main():
    parser = argparse.ArgumentParser(description='Android Preview Client')
    parser.add_argument('host', help='Android device IP address')
    parser.add_argument('--port', type=int, default=8080, help='Port number (default: 8080)')
    parser.add_argument('--duration', type=int, default=30,
                        help='Test duration in seconds (default: 30)')

    args = parser.parse_args()

    client = AndroidPreviewClient(args.host, args.port)

    try:
        if not client.connect():
            return 1

        # Start listener thread
        listener_thread = Thread(target=client.run_listener, daemon=True)
        listener_thread.start()

        # Send commands
        client.send_commands()

        # Wait for specified duration
        print(f"\nReceiving data for {args.duration} seconds...")
        time.sleep(args.duration)

        # Print statistics
        client.print_statistics()

    except KeyboardInterrupt:
        print("\nInterrupted by user")
    except Exception as e:
        print(f"Error: {e}")
        return 1
    finally:
        client.disconnect()

    return 0


if __name__ == '__main__':
    sys.exit(main())
