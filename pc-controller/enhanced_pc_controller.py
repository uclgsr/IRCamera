#!/usr/bin/env python3
"""
Enhanced PC Controller for IRCamera Multi-Modal Recording System

This implementation addresses the key requirements:
1. Complete TCP Server/Protocol with JSON message handling
2. Device registration and management
3. Session control and coordination
4. Real-time data visualization
5. Error handling and robustness
6. Data export capabilities
"""

import json
import socket
import threading
import time
import queue
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
import logging

# Try to import optional visualization libraries
try:
    import matplotlib.pyplot as plt
    import matplotlib.animation as animation
    import numpy as np

    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

try:
    import sys
    import os

    # Add the native backend path if available
    native_backend_path = Path(__file__).parent / "legacy_implementation" / "native_backend" / "build"
    if native_backend_path.exists():
        sys.path.insert(0, str(native_backend_path))
        import native_backend

        HAS_NATIVE_BACKEND = True
    else:
        HAS_NATIVE_BACKEND = False
except ImportError:
    HAS_NATIVE_BACKEND = False


class EnhancedLogger:
    def __init__(self, name: str = "PCController"):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(logging.INFO)

        if not self.logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter(
                '[%(levelname)s] %(asctime)s - %(name)s - %(message)s',
                datefmt='%H:%M:%S'
            )
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)

    def info(self, msg): self.logger.info(msg)

    def warning(self, msg): self.logger.warning(msg)

    def error(self, msg): self.logger.error(msg)

    def debug(self, msg): self.logger.debug(msg)


class DeviceInfo:
    """Enhanced device information tracking"""

    def __init__(self, device_id: str, connection: 'DeviceConnection'):
        self.device_id = device_id
        self.connection = connection
        self.device_type = "Unknown"
        self.device_name = "Unknown Device"
        self.status = "Connected"
        self.recording = False
        self.last_heartbeat = time.time()
        self.sensors = {
            'RGB': {'status': 'Unknown', 'message': ''},
            'Thermal': {'status': 'Unknown', 'message': ''},
            'GSR': {'status': 'Unknown', 'message': ''}
        }
        self.session_id = None
        self.data_packets_received = 0
        self.last_data_time = None


class SessionInfo:
    """Session tracking and management"""

    def __init__(self, session_id: str):
        self.session_id = session_id
        self.start_time = time.time()
        self.devices = []
        self.recording = False
        self.data_count = 0
        self.output_dir = None

    def add_device(self, device_id: str):
        if device_id not in self.devices:
            self.devices.append(device_id)

    def get_duration(self) -> float:
        return time.time() - self.start_time


class DataBuffer:
    """Thread-safe data buffer for real-time visualization"""

    def __init__(self, max_size: int = 1000):
        self.max_size = max_size
        self.gsr_data = []
        self.timestamps = []
        self.lock = threading.Lock()

    def add_gsr_sample(self, timestamp: float, value: float):
        with self.lock:
            self.timestamps.append(timestamp)
            self.gsr_data.append(value)

            # Keep only recent data
            if len(self.gsr_data) > self.max_size:
                self.gsr_data = self.gsr_data[-self.max_size:]
                self.timestamps = self.timestamps[-self.max_size:]

    def get_recent_data(self, count: int = None) -> Tuple[List[float], List[float]]:
        with self.lock:
            if count is None:
                return self.timestamps.copy(), self.gsr_data.copy()
            else:
                return self.timestamps[-count:], self.gsr_data[-count:]


class DeviceConnection:
    """Enhanced device connection management"""

    def __init__(self, socket: socket.socket, address: Tuple[str, int]):
        self.socket = socket
        self.address = address
        self.connected = True
        self.lock = threading.Lock()

        # Setup socket timeout for non-blocking operations
        self.socket.settimeout(1.0)

    def send_json_message(self, message: Dict[str, Any]) -> bool:
        """Send a JSON message to the device"""
        try:
            with self.lock:
                json_str = json.dumps(message)
                self.socket.send(f"{json_str}\n".encode('utf-8'))
                return True
        except Exception as e:
            logging.error(f"Error sending message to {self.address}: {e}")
            self.connected = False
            return False

    def receive_json_message(self, timeout: float = 1.0) -> Optional[Dict[str, Any]]:
        """Receive a JSON message from the device"""
        try:
            self.socket.settimeout(timeout)
            data = self.socket.recv(4096).decode('utf-8').strip()
            if data:
                return json.loads(data)
        except socket.timeout:
            return None
        except Exception as e:
            logging.warning(f"Error receiving message from {self.address}: {e}")
            self.connected = False
        return None

    def close(self):
        """Close the connection"""
        self.connected = False
        try:
            self.socket.close()
        except:
            pass


class EnhancedPCController:
    """Enhanced PC Controller with complete functionality"""

    def __init__(self, port: int = 8080, output_dir: str = "pc_recordings"):
        self.port = port
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)

        self.logger = EnhancedLogger("EnhancedPCController")

        # Server state
        self.running = False
        self.server_socket: Optional[socket.socket] = None

        # Device management
        self.devices: Dict[str, DeviceInfo] = {}
        self.device_lock = threading.Lock()

        # Session management
        self.current_session: Optional[SessionInfo] = None
        self.session_lock = threading.Lock()

        # Data buffers for visualization
        self.data_buffer = DataBuffer()

        # Background threads
        self.heartbeat_thread = None
        self.visualization_thread = None

        # Event callbacks
        self.on_device_connected = None
        self.on_device_disconnected = None
        self.on_data_received = None

        # Visualization
        if HAS_MATPLOTLIB:
            self.setup_visualization()

        self.logger.info(f"Enhanced PC Controller initialized on port {port}")
        if HAS_NATIVE_BACKEND:
            self.logger.info("Native C++ backend available")
        if HAS_MATPLOTLIB:
            self.logger.info("Real-time plotting available")

    def setup_visualization(self):
        """Setup real-time plotting"""
        if not HAS_MATPLOTLIB:
            return

        plt.ion()  # Enable interactive mode
        self.fig, self.ax = plt.subplots(figsize=(10, 6))
        self.ax.set_title('Real-time GSR Data')
        self.ax.set_xlabel('Time (s)')
        self.ax.set_ylabel('GSR (µS)')
        self.line, = self.ax.plot([], [], 'b-', linewidth=2)
        self.ax.grid(True)

    def start(self):
        """Start the PC controller server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            self.running = True

            self.logger.info(f"Server started on port {self.port}")

            # Start background threads
            self.heartbeat_thread = threading.Thread(target=self._heartbeat_loop, daemon=True)
            self.heartbeat_thread.start()

            if HAS_MATPLOTLIB:
                self.visualization_thread = threading.Thread(target=self._visualization_loop, daemon=True)
                self.visualization_thread.start()

            # Main server loop
            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    self.logger.info(f"New connection from {address}")

                    # Handle new connection in separate thread
                    connection_thread = threading.Thread(
                        target=self._handle_device_connection,
                        args=(client_socket, address),
                        daemon=True
                    )
                    connection_thread.start()

                except Exception as e:
                    if self.running:
                        self.logger.error(f"Error accepting connection: {e}")

        except KeyboardInterrupt:
            self.logger.info("Shutdown requested by user")
        except Exception as e:
            self.logger.error(f"Server error: {e}")
        finally:
            self.stop()

    def stop(self):
        """Stop the PC controller"""
        self.running = False

        # Close all device connections
        with self.device_lock:
            for device in self.devices.values():
                device.connection.close()
            self.devices.clear()

        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass

        self.logger.info("PC Controller stopped")

    def _handle_device_connection(self, client_socket: socket.socket, address: Tuple[str, int]):
        """Handle a new device connection"""
        connection = DeviceConnection(client_socket, address)
        device_id = None

        try:
            # Wait for device registration
            reg_message = connection.receive_json_message(timeout=10.0)
            if not reg_message or reg_message.get('type') != 'device_registration':
                self.logger.warning(f"Invalid registration from {address}")
                return

            device_id = reg_message.get('device_id')
            if not device_id:
                self.logger.warning(f"Missing device_id from {address}")
                return

            # Create device info
            device_info = DeviceInfo(device_id, connection)
            device_info.device_type = reg_message.get('device_type', 'Unknown')
            device_info.device_name = reg_message.get('device_name', 'Unknown Device')

            # Register device
            with self.device_lock:
                self.devices[device_id] = device_info

            self.logger.info(f"Device registered: {device_id} ({device_info.device_name})")

            # Send registration acknowledgment
            ack_message = {
                'type': 'registration_ack',
                'device_id': device_id,
                'server_time': time.time(),
                'status': 'success'
            }
            connection.send_json_message(ack_message)

            # Callback for device connected
            if self.on_device_connected:
                self.on_device_connected(device_info)

            # Handle ongoing messages from this device
            self._handle_device_messages(device_info)

        except Exception as e:
            self.logger.error(f"Error handling device {address}: {e}")
        finally:
            # Clean up disconnected device
            if device_id:
                with self.device_lock:
                    if device_id in self.devices:
                        device_info = self.devices[device_id]
                        del self.devices[device_id]
                        self.logger.info(f"Device disconnected: {device_id}")

                        # Callback for device disconnected
                        if self.on_device_disconnected:
                            self.on_device_disconnected(device_info)

            connection.close()

    def _handle_device_messages(self, device_info: DeviceInfo):
        """Handle ongoing messages from a device"""
        while device_info.connection.connected and self.running:
            try:
                message = device_info.connection.receive_json_message(timeout=2.0)
                if not message:
                    continue

                self._process_device_message(device_info, message)

            except Exception as e:
                self.logger.error(f"Error processing message from {device_info.device_id}: {e}")
                break

    def _process_device_message(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Process a message from a device"""
        msg_type = message.get('type')
        device_info.last_heartbeat = time.time()

        if msg_type == 'status_update':
            self._handle_status_update(device_info, message)
        elif msg_type == 'telemetry_gsr':
            self._handle_gsr_telemetry(device_info, message)
        elif msg_type == 'telemetry_frame':
            self._handle_frame_telemetry(device_info, message)
        elif msg_type == 'session_started':
            self._handle_session_started(device_info, message)
        elif msg_type == 'session_stopped':
            self._handle_session_stopped(device_info, message)
        elif msg_type == 'heartbeat':
            # Just update last heartbeat time (already done above)
            pass
        else:
            self.logger.warning(f"Unknown message type from {device_info.device_id}: {msg_type}")

        # Callback for data received
        if self.on_data_received:
            self.on_data_received(device_info, message)

    def _handle_status_update(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Handle device status update"""
        device_info.status = message.get('status', 'Unknown')
        sensors = message.get('sensors', {})

        for sensor_name, sensor_info in sensors.items():
            if sensor_name in device_info.sensors:
                device_info.sensors[sensor_name].update(sensor_info)

        self.logger.debug(f"Status update from {device_info.device_id}: {device_info.status}")

    def _handle_gsr_telemetry(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Handle GSR telemetry data"""
        value = message.get('value', 0.0)
        timestamp = message.get('timestamp', time.time())

        device_info.data_packets_received += 1
        device_info.last_data_time = time.time()

        # Add to data buffer for visualization
        self.data_buffer.add_gsr_sample(timestamp, value)

        # Process with native backend if available
        if HAS_NATIVE_BACKEND:
            try:
                gsr_data = native_backend.GSRData()
                gsr_data.timestamp_ns = int(timestamp * 1e9)
                gsr_data.gsr_microsiemens = value
                # Could do additional processing here
            except Exception as e:
                self.logger.warning(
                    f"Native backend processing error for device {device_info.device_id} "
                    f"while processing GSR telemetry (timestamp={timestamp}, value={value}): {e}"
                )

    def _handle_frame_telemetry(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Handle frame telemetry (RGB/Thermal)"""
        frame_type = message.get('frame_type', 'unknown')
        timestamp = message.get('timestamp', time.time())

        device_info.data_packets_received += 1
        device_info.last_data_time = time.time()

        self.logger.debug(f"Frame telemetry from {device_info.device_id}: {frame_type}")

    def _handle_session_started(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Handle session started notification"""
        session_id = message.get('session_id')
        device_info.session_id = session_id
        device_info.recording = True

        with self.session_lock:
            if not self.current_session or self.current_session.session_id != session_id:
                self.current_session = SessionInfo(session_id)
                self.current_session.output_dir = self.output_dir / session_id
                self.current_session.output_dir.mkdir(exist_ok=True)

            self.current_session.add_device(device_info.device_id)
            self.current_session.recording = True

        self.logger.info(f"Session started: {session_id} on device {device_info.device_id}")

    def _handle_session_stopped(self, device_info: DeviceInfo, message: Dict[str, Any]):
        """Handle session stopped notification"""
        device_info.recording = False

        self.logger.info(f"Session stopped on device {device_info.device_id}")

    def _heartbeat_loop(self):
        """Background thread to monitor device heartbeats"""
        while self.running:
            try:
                current_time = time.time()
                disconnected_devices = []

                with self.device_lock:
                    for device_id, device_info in self.devices.items():
                        # Check for stale connections (no heartbeat for 30 seconds)
                        if current_time - device_info.last_heartbeat > 30.0:
                            disconnected_devices.append(device_id)

                # Remove disconnected devices
                for device_id in disconnected_devices:
                    with self.device_lock:
                        if device_id in self.devices:
                            device_info = self.devices[device_id]
                            device_info.connection.close()
                            del self.devices[device_id]
                            self.logger.warning(f"Device timed out: {device_id}")

                time.sleep(5.0)  # Check every 5 seconds

            except Exception as e:
                self.logger.error(f"Heartbeat loop error: {e}")
                time.sleep(1.0)

    def _visualization_loop(self):
        """Background thread for real-time visualization"""
        if not HAS_MATPLOTLIB:
            return

        while self.running:
            try:
                timestamps, gsr_values = self.data_buffer.get_recent_data(300)  # Last 30 seconds at 10Hz

                if timestamps and gsr_values:
                    # Convert to relative timestamps (seconds from start)
                    if timestamps:
                        start_time = timestamps[0]
                        rel_times = [(t - start_time) for t in timestamps]

                        self.line.set_data(rel_times, gsr_values)

                        # Update plot limits
                        if rel_times:
                            self.ax.set_xlim(0, max(30, max(rel_times)))  # At least 30 seconds
                        if gsr_values:
                            y_min, y_max = min(gsr_values), max(gsr_values)
                            y_range = y_max - y_min
                            self.ax.set_ylim(y_min - 0.1 * y_range, y_max + 0.1 * y_range)

                        plt.draw()
                        plt.pause(0.1)

                time.sleep(0.1)  # Update at 10Hz

            except Exception as e:
                self.logger.error(f"Visualization error: {e}")
                time.sleep(1.0)

    def start_recording_session(self, session_id: str = None) -> bool:
        """Start a recording session on all connected devices"""
        if not session_id:
            session_id = f"session_{int(time.time())}"

        message = {
            'type': 'start_recording',
            'session_id': session_id,
            'timestamp': time.time()
        }

        success_count = 0
        with self.device_lock:
            for device_info in self.devices.values():
                if device_info.connection.send_json_message(message):
                    success_count += 1

        if success_count > 0:
            with self.session_lock:
                self.current_session = SessionInfo(session_id)
                self.current_session.output_dir = self.output_dir / session_id
                self.current_session.output_dir.mkdir(exist_ok=True)

            self.logger.info(f"Recording session started: {session_id} ({success_count} devices)")
            return True

        return False

    def stop_recording_session(self) -> bool:
        """Stop the current recording session"""
        if not self.current_session:
            return False

        message = {
            'type': 'stop_recording',
            'session_id': self.current_session.session_id,
            'timestamp': time.time()
        }

        success_count = 0
        with self.device_lock:
            for device_info in self.devices.values():
                if device_info.recording and device_info.connection.send_json_message(message):
                    success_count += 1

        self.logger.info(f"Recording session stopped: {self.current_session.session_id}")
        return success_count > 0

    def get_device_status(self) -> Dict[str, Dict[str, Any]]:
        """Get status of all connected devices"""
        status = {}
        with self.device_lock:
            for device_id, device_info in self.devices.items():
                status[device_id] = {
                    'device_name': device_info.device_name,
                    'device_type': device_info.device_type,
                    'status': device_info.status,
                    'recording': device_info.recording,
                    'sensors': device_info.sensors.copy(),
                    'data_packets_received': device_info.data_packets_received,
                    'last_data_time': device_info.last_data_time,
                    'connection_duration': time.time() - device_info.last_heartbeat
                }
        return status

    def export_session_data(self, session_id: str = None, format_type: str = 'csv') -> Optional[Path]:
        """Export session data to file"""
        if not session_id and self.current_session:
            session_id = self.current_session.session_id

        if not session_id:
            return None

        output_file = self.output_dir / f"{session_id}_export.{format_type}"

        try:
            if format_type == 'csv':
                timestamps, gsr_values = self.data_buffer.get_recent_data()

                with open(output_file, 'w') as f:
                    f.write("timestamp,gsr_value\n")
                    for ts, val in zip(timestamps, gsr_values):
                        f.write(f"{ts},{val}\n")

            elif format_type == 'json':
                timestamps, gsr_values = self.data_buffer.get_recent_data()

                export_data = {
                    'session_id': session_id,
                    'export_time': time.time(),
                    'gsr_data': [
                        {'timestamp': ts, 'value': val}
                        for ts, val in zip(timestamps, gsr_values)
                    ]
                }

                with open(output_file, 'w') as f:
                    json.dump(export_data, f, indent=2)

            self.logger.info(f"Session data exported to {output_file}")
            return output_file

        except Exception as e:
            self.logger.error(f"Error exporting session data: {e}")
            return None


def main():
    """Main function for standalone execution"""
    controller = EnhancedPCController(port=8080)

    # Setup event callbacks
    def on_device_connected(device_info):
        print(f"✓ Device connected: {device_info.device_name} ({device_info.device_id})")

    def on_device_disconnected(device_info):
        print(f"✗ Device disconnected: {device_info.device_name} ({device_info.device_id})")

    def on_data_received(device_info, message):
        if message.get('type') == 'telemetry_gsr':
            print(f" GSR data from {device_info.device_id}: {message.get('value'):.2f} µS")

    controller.on_device_connected = on_device_connected
    controller.on_device_disconnected = on_device_disconnected
    controller.on_data_received = on_data_received

    try:
        print("\n Enhanced PC Controller starting...")
        print(f"📡 Listening on port {controller.port}")
        print("💡 Connect your Android devices to start recording")
        print("⏹️  Press Ctrl+C to stop\n")

        controller.start()

    except KeyboardInterrupt:
        print("\n👋 Shutting down...")
        controller.stop()


if __name__ == "__main__":
    main()
