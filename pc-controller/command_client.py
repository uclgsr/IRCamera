#!/usr/bin/env python3
"""
CommandClient - PC side command sender for multi-modal recording system

This module handles sending commands to Android devices and managing
the PC-orchestrated recording sessions.
"""

import json
import logging
import socket
import time
from datetime import datetime
from typing import Dict, List, Optional, Any, Tuple

logger = logging.getLogger(__name__)


class CommandClient:
    """PC-side client for sending commands to Android devices"""

    def __init__(self, timeout: int = 10):
        self.timeout = timeout
        self.connected_devices = {}
        self.command_log: List[Dict[str, Any]] = []
        self.command_id_counter = 0

    @staticmethod
    def _is_ack(response: Any, expected_command: str) -> bool:
        if response is None:
            return False
        if isinstance(response, dict):
            status = str(response.get('status', '')).lower()
            if status in {'ok', 'success', 'ack'}:
                return True
            command = str(response.get('command', '')).lower()
            if command == expected_command.lower():
                return True
            result = str(response.get('result', '')).lower()
            if result in {'ok', 'success'}:
                return True
            return False
        response_text = str(response).upper()
        return expected_command.upper() in response_text or 'ACK' in response_text

    def connect_to_device(self, device_ip: str, port: int = 8080) -> bool:
        """Connect to an Android device"""
        try:
            logger.info(f"Connecting to device at {device_ip}:{port}")

            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(self.timeout)
            sock.connect((device_ip, port))

            device_id = f"{device_ip}:{port}"
            self.connected_devices[device_id] = {
                'socket': sock,
                'ip': device_ip,
                'port': port,
                'connected_at': time.time(),
                'last_command': None
            }

            logger.info(f"Successfully connected to device {device_id}")
            return True

        except Exception as e:
            logger.error(f"Failed to connect to {device_ip}:{port} - {e}")
            return False

    def send_command(self, device_id: str, command: str, params: Dict[str, Any] = None) -> Optional[str]:
        """Send a command to a specific device"""
        if device_id not in self.connected_devices:
            logger.error(f"Device {device_id} not connected")
            return None

        try:
            device = self.connected_devices[device_id]
            sock = device['socket']

            # Create command message
            self.command_id_counter += 1
            message: Dict[str, Any] = {
                'type': 'command',
                'message_id': f"cmd_{self.command_id_counter}",
                'command': command,
                'parameters': params or {},
                'timestamp': time.time(),
                'sender': 'pc_command_client'
            }
            if params:
                # Preserve backward compatibility for clients expecting top-level fields.
                message.update(params)

            # Send command
            message_json = json.dumps(message)
            sock.send(message_json.encode('utf-8') + b'\n')

            # Wait for response (read until newline)
            response_data = b""
            while b'\n' not in response_data:
                chunk = sock.recv(1024)
                if not chunk:
                    break
            response_data += chunk
            response_text = response_data.decode().strip()

            try:
                response = json.loads(response_text)
            except json.JSONDecodeError:
                response = response_text

            # Log command
            log_entry = {
                'timestamp': datetime.now().isoformat(),
                'device_id': device_id,
                'command': command,
                'message_id': message['message_id'],
                'response': response,
                'params': params or {}
            }
            self.command_log.append(log_entry)
            device['last_command'] = log_entry

            logger.info(f"Command {command} -> {device_id}: {response}")
            return response

        except Exception as e:
            logger.error(f"Error sending command {command} to {device_id}: {e}")
            return None

    def send_sync_command(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Send time synchronization command and measure timing"""
        if device_id not in self.connected_devices:
            return None

        try:
            t1 = time.time_ns()
            response = self.send_command(
                device_id,
                'SYNC_REQUEST',
                {
                    'pc_timestamp': t1,
                    'pc_address': self._get_local_ip()
                }
            )
            t4 = time.time_ns()

            if not isinstance(response, dict):
                logger.error(f"Sync failed for {device_id}: {response}")
                return None

            sync_result = {
                'device_id': device_id,
                'pc_send_time': t1,
                'pc_receive_time': t4,
                'round_trip_time_ns': t4 - t1,
                'response': response,
                'timestamp': datetime.now().isoformat()
            }
            logger.info(f"Sync with {device_id} completed: RTT={sync_result['round_trip_time_ns'] / 1e6:.2f}ms")
            return sync_result

        except Exception as e:
            logger.error(f"Sync command failed for {device_id}: {e}")
            return None

    def start_recording_session(self, session_id: str, device_ids: List[str] = None,
                                configuration: Dict[str, Any] = None) -> Dict[str, bool]:
        """Start coordinated recording on specified devices"""
        if device_ids is None:
            device_ids = list(self.connected_devices.keys())

        if configuration is None:
            configuration = {
                'modalities': ['rgb', 'thermal', 'gsr'],
                'duration': 300,  # 5 minutes default
                'sync_flash': True
            }

        logger.info(f"Starting recording session '{session_id}' on {len(device_ids)} devices")

        results = {}
        start_time = time.time_ns()

        # Send START command to all devices
        for device_id in device_ids:
            response = self.send_command(device_id, 'START_RECORD', {
                'session_id': session_id,
                'configuration': configuration,
                'start_timestamp': start_time
            })

            results[device_id] = self._is_ack(response, 'START_RECORD')

        # Log session start
        session_log = {
            'session_id': session_id,
            'start_time': datetime.now().isoformat(),
            'start_timestamp_ns': start_time,
            'devices': device_ids,
            'configuration': configuration,
            'results': results
        }
        self.command_log.append(session_log)

        success_count = sum(1 for success in results.values() if success)
        logger.info(f"Recording session started: {success_count}/{len(device_ids)} devices successful")

        return results

    def stop_recording_session(self, device_ids: List[str] = None) -> Dict[str, bool]:
        """Stop coordinated recording on specified devices"""
        if device_ids is None:
            device_ids = list(self.connected_devices.keys())

        logger.info(f"Stopping recording session on {len(device_ids)} devices")

        results = {}
        stop_time = time.time_ns()

        # Send STOP command to all devices
        for device_id in device_ids:
            response = self.send_command(device_id, 'STOP_RECORD', {
                'stop_timestamp': stop_time
            })

            results[device_id] = self._is_ack(response, 'STOP_RECORD')

        success_count = sum(1 for success in results.values() if success)
        logger.info(f"Recording session stopped: {success_count}/{len(device_ids)} devices successful")

        return results

    def get_device_status(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get current status from a device"""
        response = self.send_command(device_id, 'STATUS_REQUEST')

        if response:
            if isinstance(response, dict):
                return {
                    'device_id': device_id,
                    'status': response.get('status', 'unknown'),
                    'response': response,
                    'timestamp': datetime.now().isoformat()
                }
            if 'STATUS-ACK' in str(response):
                # Legacy string-based acknowledgement
                return {
                    'device_id': device_id,
                    'status': 'connected',
                    'response': response,
                    'timestamp': datetime.now().isoformat()
                }

        return None

    def sync_all_devices(self) -> Dict[str, Dict[str, Any]]:
        """Perform time synchronization with all connected devices"""
        logger.info(f"Synchronizing time with {len(self.connected_devices)} devices")

        sync_results = {}

        for device_id in self.connected_devices.keys():
            sync_result = self.send_sync_command(device_id)
            sync_results[device_id] = sync_result

        return sync_results

    def get_command_log(self) -> List[Dict[str, Any]]:
        """Get the command log for analysis"""
        return self.command_log.copy()

    def get_connected_devices(self) -> Dict[str, Dict[str, Any]]:
        """Get list of connected devices with their info"""
        device_info = {}

        for device_id, device in self.connected_devices.items():
            device_info[device_id] = {
                'ip': device['ip'],
                'port': device['port'],
                'connected_at': datetime.fromtimestamp(device['connected_at']).isoformat(),
                'last_command': device.get('last_command')
            }

        return device_info

    def disconnect_device(self, device_id: str) -> bool:
        """Disconnect from a specific device"""
        if device_id in self.connected_devices:
            try:
                self.connected_devices[device_id]['socket'].close()
                del self.connected_devices[device_id]
                logger.info(f"Disconnected from device {device_id}")
                return True
            except Exception as e:
                logger.error(f"Error disconnecting from {device_id}: {e}")

        return False

    def disconnect_all(self):
        """Disconnect from all devices"""
        for device_id in list(self.connected_devices.keys()):
            self.disconnect_device(device_id)

    def _get_local_ip(self) -> str:
        """Get local IP address for sync purposes"""
        try:
            # Connect to a dummy address to get local IP
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(("8.8.8.8", 80))
                return s.getsockname()[0]
        except Exception:
            return "127.0.0.1"

    def __del__(self):
        """Cleanup on destruction"""
        self.disconnect_all()


if __name__ == "__main__":
    # Simple test of command client
    logging.basicConfig(level=logging.INFO,
                        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    client = CommandClient()

    print("CommandClient test mode")
    print("Available commands:")
    print("  connect <ip> [port] - Connect to Android device")
    print("  sync [device_id] - Synchronize time")
    print("  start <session_id> - Start recording")
    print("  stop - Stop recording")
    print("  status [device_id] - Get device status")
    print("  devices - List connected devices")
    print("  log - Show command log")
    print("  quit - Exit")

    while True:
        try:
            cmd = input("\n> ").strip().split()
            if not cmd:
                continue

            if cmd[0] == 'quit':
                break
            elif cmd[0] == 'connect':
                ip = cmd[1]
                port = int(cmd[2]) if len(cmd) > 2 else 8080
                client.connect_to_device(ip, port)
            elif cmd[0] == 'sync':
                if len(cmd) > 1:
                    client.send_sync_command(cmd[1])
                else:
                    client.sync_all_devices()
            elif cmd[0] == 'start':
                session_id = cmd[1] if len(cmd) > 1 else f"session_{int(time.time())}"
                client.start_recording_session(session_id)
            elif cmd[0] == 'stop':
                client.stop_recording_session()
            elif cmd[0] == 'status':
                if len(cmd) > 1:
                    status = client.get_device_status(cmd[1])
                    print(f"Status: {status}")
                else:
                    for device_id in client.connected_devices:
                        status = client.get_device_status(device_id)
                        print(f"{device_id}: {status}")
            elif cmd[0] == 'devices':
                devices = client.get_connected_devices()
                for device_id, info in devices.items():
                    print(f"{device_id}: {info}")
            elif cmd[0] == 'log':
                log = client.get_command_log()
                for entry in log[-10:]:  # Show last 10 entries
                    print(f"{entry}")
            else:
                print(f"Unknown command: {cmd[0]}")

        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"Error: {e}")

    client.disconnect_all()
    print("CommandClient test completed")
