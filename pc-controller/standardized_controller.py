#!/usr/bin/env python3
"""
Standardized Protocol PC Controller

Implements the PC-orchestrated multi-modal recording system using
the standardized networking protocol as specified in the issue.
"""

import json
import socket
import threading
import time
import re
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple


class SimpleLogger:
    def info(self, msg): print(f"[INFO] {datetime.now().strftime('%H:%M:%S')} - {msg}")

    def warning(self, msg): print(f"[WARN] {datetime.now().strftime('%H:%M:%S')} - {msg}")

    def error(self, msg): print(f"[ERROR] {datetime.now().strftime('%H:%M:%S')} - {msg}")

    def debug(self, msg): print(f"[DEBUG] {datetime.now().strftime('%H:%M:%S')} - {msg}")


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
                param_pattern = r'(\w+)=([^\s]+|"[^"]*")'
                matches = re.findall(param_pattern, param_str)

                for key, value in matches:
                    # Remove quotes if present
                    if value.startswith('"') and value.endswith('"'):
                        value = value[1:-1]
                    params[key] = value

            return {'type': msg_type, 'params': params}
        except Exception as e:
            logger.error(f"Error parsing message '{message}': {e}")
            return None


class DeviceConnection:
    """Manages connection to a single Android device"""

    def __init__(self, device_id: str, socket: socket.socket, address: Tuple[str, int]):
        self.device_id = device_id
        self.socket = socket
        self.address = address
        self.capabilities = []
        self.status = "connected"
        self.clock_offset_ms = 0
        self.last_sync_time = 0
        self.connected_at = time.time()

        # Set up socket for text communication
        self.socket_file = socket.makefile('rw', encoding='utf-8')

    def send_message(self, message: str) -> bool:
        """Send a text protocol message"""
        try:
            self.socket_file.write(message + '\n')
            self.socket_file.flush()
            logger.debug(f"Sent to {self.device_id}: {message}")
            return True
        except Exception as e:
            logger.error(f"Error sending message to {self.device_id}: {e}")
            return False

    def receive_message(self) -> Optional[str]:
        """Receive a text protocol message"""
        try:
            line = self.socket_file.readline()
            if line:
                line = line.strip()
                if line:
                    logger.debug(f"Received from {self.device_id}: {line}")
                    return line
            return None
        except Exception as e:
            logger.error(f"Error receiving message from {self.device_id}: {e}")
            return None

    def close(self):
        """Close the connection"""
        try:
            self.socket_file.close()
            self.socket.close()
        except Exception as e:
            logger.error(f"Error closing connection to {self.device_id}: {e}")


class DeviceRegistry:
    """Registry of connected devices"""

    def __init__(self):
        self.devices: Dict[str, DeviceConnection] = {}
        self.lock = threading.Lock()

    def add_device(self, connection: DeviceConnection):
        with self.lock:
            self.devices[connection.device_id] = connection
            logger.info(f"Device registered: {connection.device_id} from {connection.address}")

    def remove_device(self, device_id: str):
        with self.lock:
            if device_id in self.devices:
                connection = self.devices.pop(device_id)
                connection.close()
                logger.info(f"Device removed: {device_id}")

    def get_device(self, device_id: str) -> Optional[DeviceConnection]:
        with self.lock:
            return self.devices.get(device_id)

    def get_all_devices(self) -> List[DeviceConnection]:
        with self.lock:
            return list(self.devices.values())

    def update_device_capabilities(self, device_id: str, capabilities: List[str]):
        with self.lock:
            if device_id in self.devices:
                self.devices[device_id].capabilities = capabilities


class SessionManager:
    """Manages recording sessions"""

    def __init__(self, base_dir: str = "./sessions"):
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(exist_ok=True)
        self.current_session: Optional[str] = None
        self.recording_devices: List[str] = []
        self.session_start_time = 0

    def create_session(self, session_id: str = None) -> str:
        if not session_id:
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        session_dir = self.base_dir / session_id
        session_dir.mkdir(exist_ok=True)

        self.current_session = session_id
        logger.info(f"Created session: {session_id}")
        return session_id

    def start_recording(self, device_ids: List[str]):
        if not self.current_session:
            raise ValueError("No active session")

        self.recording_devices = device_ids
        self.session_start_time = time.time()
        logger.info(f"Started recording session {self.current_session} with {len(device_ids)} devices")

    def stop_recording(self):
        if self.current_session and self.recording_devices:
            duration = time.time() - self.session_start_time
            logger.info(f"Stopped recording session {self.current_session} after {duration:.2f} seconds")
            self.recording_devices = []

    def finalize_session(self):
        if self.current_session:
            logger.info(f"Finalized session: {self.current_session}")
            self.current_session = None


class TimeSyncManager:
    """Handles clock synchronization with devices"""

    @staticmethod
    def perform_sync(connection: DeviceConnection) -> bool:
        """Perform NTP-style time synchronization"""
        try:
            # Get current PC time in milliseconds
            t1 = int(time.time() * 1000)

            # Send sync request
            sync_request = Protocol.create_sync_request(t1)
            if not connection.send_message(sync_request):
                return False

            # Wait for response
            response = connection.receive_message()
            if not response:
                return False

            # Parse response
            parsed = Protocol.parse_message(response)
            if not parsed or parsed['type'] != Protocol.MSG_SYNC_RESPONSE:
                logger.error(f"Invalid sync response from {connection.device_id}: {response}")
                return False

            # Get timestamps
            t2 = int(time.time() * 1000)  # PC time when response received
            t_pc_echo = int(parsed['params'].get('t_pc', 0))
            t_phone = int(parsed['params'].get('t_ph', 0))

            # Verify echoed timestamp
            if abs(t_pc_echo - t1) > 1000:  # Allow 1 second tolerance
                logger.warning(f"Timestamp echo mismatch for {connection.device_id}")

            # Calculate clock offset (simplified NTP calculation)
            network_delay = t2 - t1
            clock_offset = t_phone - t1 - (network_delay // 2)

            connection.clock_offset_ms = clock_offset
            connection.last_sync_time = time.time()

            logger.info(f"Time sync completed for {connection.device_id}: "
                        f"offset={clock_offset}ms, delay={network_delay}ms")

            return True

        except Exception as e:
            logger.error(f"Time sync failed for {connection.device_id}: {e}")
            return False


class PCController:
    """Main PC controller for orchestrating multiple Android devices"""

    def __init__(self, port: int = 8080):
        self.port = port
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.device_registry = DeviceRegistry()
        self.session_manager = SessionManager()
        self.time_sync_manager = TimeSyncManager()

        # Background thread for device management
        self.device_handler_threads: Dict[str, threading.Thread] = {}

    def start(self):
        """Start the PC controller server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            self.running = True

            logger.info(f"PC Controller started on port {self.port}")

            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()

                    # Handle new device connection
                    thread = threading.Thread(
                        target=self._handle_new_device,
                        args=(client_socket, address),
                        daemon=True
                    )
                    thread.start()

                except Exception as e:
                    if self.running:
                        logger.error(f"Error accepting connection: {e}")

        except KeyboardInterrupt:
            logger.info("Shutdown requested by user")
        except Exception as e:
            logger.error(f"Server error: {e}")
        finally:
            self.stop()

    def stop(self):
        """Stop the PC controller"""
        self.running = False

        # Close all device connections
        for device_id in list(self.device_registry.devices.keys()):
            self.device_registry.remove_device(device_id)

        # Close server socket
        if self.server_socket:
            self.server_socket.close()

        logger.info("PC Controller stopped")

    def _handle_new_device(self, client_socket: socket.socket, address: Tuple[str, int]):
        """Handle a new device connection"""
        connection = None
        device_id = f"device_{address[0]}_{address[1]}"

        try:
            # Create temporary connection
            connection = DeviceConnection(device_id, client_socket, address)

            # Wait for HELLO message
            hello_msg = connection.receive_message()
            if not hello_msg:
                logger.warning(f"No HELLO message received from {address}")
                return

            # Parse HELLO message
            parsed = Protocol.parse_message(hello_msg)
            if not parsed or parsed['type'] != Protocol.MSG_HELLO:
                logger.warning(f"Invalid HELLO message from {address}: {hello_msg}")
                return

            # Extract device information
            device_name = parsed['params'].get('device_name', device_id)
            sensors_str = parsed['params'].get('sensors', '[]')

            # Parse sensors list (e.g., "[RGB,THERMAL,GSR]")
            sensors = []
            if sensors_str.startswith('[') and sensors_str.endswith(']'):
                sensors_list = sensors_str[1:-1].split(',')
                sensors = [s.strip() for s in sensors_list if s.strip()]

            # Update connection with actual device info
            connection.device_id = device_name
            connection.capabilities = sensors

            # Register device
            self.device_registry.add_device(connection)

            # Perform initial time synchronization
            logger.info(f"Performing initial time sync with {device_name}")
            self.time_sync_manager.perform_sync(connection)

            # Start device handler thread
            handler_thread = threading.Thread(
                target=self._handle_device_messages,
                args=(connection,),
                daemon=True
            )
            handler_thread.start()
            self.device_handler_threads[device_name] = handler_thread

            logger.info(f"Device {device_name} connected with capabilities: {sensors}")

        except Exception as e:
            logger.error(f"Error handling new device {address}: {e}")
            if connection:
                connection.close()

    def _handle_device_messages(self, connection: DeviceConnection):
        """Handle ongoing messages from a device"""
        try:
            while self.running and connection.device_id in self.device_registry.devices:
                message = connection.receive_message()
                if not message:
                    break

                # Parse and handle the message
                parsed = Protocol.parse_message(message)
                if parsed:
                    self._process_device_message(connection, parsed)
                else:
                    logger.warning(f"Failed to parse message from {connection.device_id}: {message}")

        except Exception as e:
            logger.error(f"Error handling messages from {connection.device_id}: {e}")
        finally:
            # Clean up disconnected device
            if connection.device_id in self.device_registry.devices:
                self.device_registry.remove_device(connection.device_id)

            if connection.device_id in self.device_handler_threads:
                del self.device_handler_threads[connection.device_id]

    def _process_device_message(self, connection: DeviceConnection, parsed_msg: Dict[str, Any]):
        """Process a parsed message from a device"""
        msg_type = parsed_msg['type']
        params = parsed_msg['params']

        if msg_type == Protocol.MSG_ACK:
            acked_cmd = params.get('cmd', 'unknown')
            logger.info(f"Device {connection.device_id} acknowledged: {acked_cmd}")

        elif msg_type == Protocol.MSG_ERROR:
            error_cmd = params.get('cmd', 'unknown')
            error_code = params.get('code', 'unknown')
            error_msg = params.get('msg', 'No message')
            logger.error(f"Device {connection.device_id} error for {error_cmd}: {error_code} - {error_msg}")

        elif msg_type == Protocol.MSG_DATA_GSR:
            timestamp = params.get('ts', 0)
            value = params.get('value', 0)
            logger.debug(f"GSR data from {connection.device_id}: {value} at {timestamp}")

        elif msg_type == Protocol.MSG_FRAME:
            # Handle frame header (binary data would follow)
            frame_type = params.get('type', 'unknown')
            timestamp = params.get('ts', 0)
            size = params.get('size', 0)
            logger.debug(f"Frame from {connection.device_id}: {frame_type} ({size} bytes) at {timestamp}")

        else:
            logger.debug(f"Unhandled message type from {connection.device_id}: {msg_type}")

    def start_recording_session(self, session_id: str = None) -> bool:
        """Start a recording session on all connected devices"""
        devices = self.device_registry.get_all_devices()
        if not devices:
            logger.error("No devices connected")
            return False

        # Create session
        if not session_id:
            session_id = self.session_manager.create_session()
        else:
            self.session_manager.current_session = session_id

        # Send start command to all devices
        success_count = 0
        device_ids = []

        for device in devices:
            start_cmd = Protocol.create_start_record(session_id)
            if device.send_message(start_cmd):
                device_ids.append(device.device_id)
                success_count += 1
            else:
                logger.error(f"Failed to send start command to {device.device_id}")

        if success_count > 0:
            self.session_manager.start_recording(device_ids)
            logger.info(f"Recording session {session_id} started on {success_count}/{len(devices)} devices")
            return True
        else:
            logger.error("Failed to start recording on any device")
            return False

    def stop_recording_session(self) -> bool:
        """Stop the current recording session on all devices"""
        if not self.session_manager.current_session:
            logger.error("No active recording session")
            return False

        session_id = self.session_manager.current_session
        devices = self.device_registry.get_all_devices()
        success_count = 0

        # Send stop command to all devices
        for device in devices:
            stop_cmd = Protocol.create_stop_record(session_id)
            if device.send_message(stop_cmd):
                success_count += 1
            else:
                logger.error(f"Failed to send stop command to {device.device_id}")

        self.session_manager.stop_recording()
        logger.info(f"Recording session {session_id} stopped on {success_count}/{len(devices)} devices")
        return success_count > 0

    def sync_all_devices(self) -> int:
        """Perform time synchronization with all connected devices"""
        devices = self.device_registry.get_all_devices()
        success_count = 0

        for device in devices:
            if self.time_sync_manager.perform_sync(device):
                success_count += 1

        logger.info(f"Time sync completed on {success_count}/{len(devices)} devices")
        return success_count

    def get_status(self) -> Dict[str, Any]:
        """Get current status of the PC controller"""
        devices = self.device_registry.get_all_devices()
        device_info = []

        for device in devices:
            device_info.append({
                'id': device.device_id,
                'address': f"{device.address[0]}:{device.address[1]}",
                'capabilities': device.capabilities,
                'status': device.status,
                'clock_offset_ms': device.clock_offset_ms,
                'connected_duration': time.time() - device.connected_at
            })

        return {
            'running': self.running,
            'connected_devices': len(devices),
            'devices': device_info,
            'current_session': self.session_manager.current_session,
            'recording_devices': len(self.session_manager.recording_devices)
        }


def main():
    """Main CLI interface for the PC controller"""
    controller = PCController()

    # Start server in background thread
    server_thread = threading.Thread(target=controller.start, daemon=True)
    server_thread.start()

    print("PC Controller for Multi-Modal Recording")
    print("Commands: start, stop, sync, status, quit")
    print()

    try:
        while True:
            try:
                command = input("> ").strip().lower()

                if command in ['quit', 'exit', 'q']:
                    break
                elif command == 'start':
                    controller.start_recording_session()
                elif command == 'stop':
                    controller.stop_recording_session()
                elif command == 'sync':
                    controller.sync_all_devices()
                elif command == 'status':
                    status = controller.get_status()
                    print(f"Running: {status['running']}")
                    print(f"Connected devices: {status['connected_devices']}")
                    print(f"Current session: {status['current_session']}")
                    print(f"Recording devices: {status['recording_devices']}")
                    for device in status['devices']:
                        print(f"  {device['id']}: {device['capabilities']} "
                              f"(offset: {device['clock_offset_ms']}ms)")
                elif command == 'help':
                    print("Commands:")
                    print("  start  - Start recording session on all devices")
                    print("  stop   - Stop current recording session")
                    print("  sync   - Synchronize time with all devices")
                    print("  status - Show current status")
                    print("  quit   - Exit the program")
                else:
                    print(f"Unknown command: {command}. Type 'help' for available commands.")

            except KeyboardInterrupt:
                break
            except Exception as e:
                logger.error(f"Command error: {e}")

    finally:
        controller.stop()


if __name__ == "__main__":
    main()
