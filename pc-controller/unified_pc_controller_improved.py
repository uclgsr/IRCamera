#!/usr/bin/env python3
"""
Improved Unified PC Controller with Best Practices

Improvements over unified_pc_controller.py:
1. Proper exception handling (no bare except)
2. Socket timeouts to prevent hanging
3. Encoding error handling
4. Logging framework instead of print
5. Maximum message size limits
6. Graceful shutdown
"""

import sys
import socket
import threading
import time
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional, Tuple
from protocol_adapter import ProtocolAdapter

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Try to import PyQt6 for GUI
try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
        QTextEdit, QPushButton, QLabel, QTreeWidget, QTreeWidgetItem,
        QGroupBox, QSpinBox, QCheckBox, QSplitter, QTabWidget
    )
    from PyQt6.QtCore import QThread, pyqtSignal, Qt, QTimer
    import pyqtgraph as pg
    GUI_AVAILABLE = True
except ImportError:
    GUI_AVAILABLE = False
    logger.info("PyQt6 not available - running in CLI mode")


# Configuration constants
SOCKET_TIMEOUT = 30.0  # seconds
MAX_MESSAGE_SIZE = 1024 * 1024  # 1MB
MAX_CONNECTIONS = 100
RECV_BUFFER_SIZE = 4096


class DeviceConnection:
    """Represents a connected Android device"""
    
    def __init__(self, device_id: str, sock: socket.socket, address: tuple):
        self.device_id = device_id
        self.socket = sock
        self.address = address
        self.device_name = device_id
        self.sensors = []
        self.connected_at = time.time()
        self.last_heartbeat = time.time()
        self.status = "Connected"
        self.session_id = None
        self.is_recording = False
        
        # Time synchronization data
        self.clock_offset_ms = 0
        self.rtt_ms = 0
        self.sync_quality = "Unknown"
        self.last_sync_time = None
        
        # Data buffers
        self.gsr_data = []  # [(timestamp, value), ...]
        self.message_count = 0
        self.bytes_received = 0


class NetworkThread(QThread if GUI_AVAILABLE else threading.Thread):
    """Network thread with Android protocol support and best practices"""
    
    if GUI_AVAILABLE:
        device_connected = pyqtSignal(str, dict)
        device_disconnected = pyqtSignal(str, str)
        message_received = pyqtSignal(str, dict)
        gsr_data_received = pyqtSignal(str, float, float)
        frame_received = pyqtSignal(str, str, bytes)
        log_message = pyqtSignal(str)
    
    def __init__(self, port: int = 8080):
        super().__init__()
        self.port = port
        self.running = False
        self.server_socket = None
        self.connections: Dict[str, DeviceConnection] = {}
        self.lock = threading.Lock()
        self.adapter = ProtocolAdapter()
        
        # Time sync tracking
        self.pending_syncs = {}  # device_id -> (t1, t2)
    
    def run(self):
        """Main network loop"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(10)
            self.running = True
            
            logger.info(f"Server started on port {self.port}")
            self._log(f"Server started on port {self.port}")
            
            while self.running:
                try:
                    # Check connection limit
                    with self.lock:
                        if len(self.connections) >= MAX_CONNECTIONS:
                            logger.warning(f"Connection limit reached: {MAX_CONNECTIONS}")
                            time.sleep(0.1)
                            continue
                    
                    client_socket, address = self.server_socket.accept()
                    logger.info(f"New connection from {address}")
                    self._log(f"New connection from {address}")
                    
                    # Handle in separate thread
                    thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, address),
                        daemon=True
                    )
                    thread.start()
                except OSError as e:
                    if self.running:
                        logger.error(f"Accept error: {e}")
                        self._log(f"Accept error: {e}")
        except OSError as e:
            logger.error(f"Server error: {e}")
            self._log(f"Server error: {e}")
        finally:
            self.stop()
    
    def _handle_client(self, client_socket: socket.socket, address: tuple):
        """Handle individual client connection"""
        device_id = f"device_{address[0]}_{address[1]}"
        connection = None
        buffer = ""
        
        try:
            # Set socket timeout to prevent hanging
            client_socket.settimeout(SOCKET_TIMEOUT)
            
            # Wait for HELLO message
            try:
                initial_data = client_socket.recv(RECV_BUFFER_SIZE).decode('utf-8', errors='replace')
            except socket.timeout:
                logger.warning(f"Timeout waiting for HELLO from {address}")
                return
            except UnicodeDecodeError as e:
                logger.error(f"Encoding error from {address}: {e}")
                return
            
            if not initial_data:
                return
            
            # Parse HELLO using protocol adapter
            json_msg = self.adapter.android_to_json(initial_data.strip())
            
            if json_msg and json_msg.get('type') == 'HELLO':
                device_name = json_msg.get('device_name', device_id)
                sensors = json_msg.get('sensors', [])
                
                # Create connection
                connection = DeviceConnection(device_id, client_socket, address)
                connection.device_name = device_name
                connection.sensors = sensors
                
                with self.lock:
                    self.connections[device_id] = connection
                
                # Send ACK
                ack = self.adapter.create_ack('HELLO', device_id=device_id)
                try:
                    client_socket.send((ack + '\n').encode('utf-8'))
                except (OSError, socket.error) as e:
                    logger.error(f"Failed to send ACK to {device_id}: {e}")
                    return
                
                # Notify
                device_info = {
                    'device_id': device_id,
                    'device_name': device_name,
                    'sensors': sensors,
                    'address': address[0]
                }
                self._emit_signal('device_connected', device_id, device_info)
                logger.info(f"Device registered: {device_name} (sensors: {', '.join(sensors)})")
                self._log(f"Device registered: {device_name} (sensors: {', '.join(sensors)})")
                
                # Handle messages
                while self.running:
                    try:
                        data = client_socket.recv(RECV_BUFFER_SIZE).decode('utf-8', errors='replace')
                    except socket.timeout:
                        # Check if device is still alive
                        if time.time() - connection.last_heartbeat > SOCKET_TIMEOUT * 2:
                            logger.warning(f"Device {device_id} timed out")
                            break
                        continue
                    except UnicodeDecodeError as e:
                        logger.error(f"Encoding error from {device_id}: {e}")
                        continue
                    except (OSError, socket.error) as e:
                        logger.error(f"Socket error from {device_id}: {e}")
                        break
                    
                    if not data:
                        break
                    
                    buffer += data
                    
                    # Check buffer size limit
                    if len(buffer) > MAX_MESSAGE_SIZE:
                        logger.error(f"Message size limit exceeded from {device_id}")
                        break
                    
                    with self.lock:
                        if device_id in self.connections:
                            self.connections[device_id].bytes_received += len(data)
                    
                    # Process complete messages (newline-delimited)
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        if line.strip():
                            self._process_message(device_id, line.strip(), client_socket)
            else:
                logger.warning(f"No HELLO message from {address}")
                self._log(f"No HELLO message from {address}")
        
        except (OSError, socket.error) as e:
            logger.error(f"Client error {address}: {e}")
            self._log(f"Client error {address}: {e}")
        except Exception as e:
            logger.exception(f"Unexpected error handling {address}: {e}")
            self._log(f"Unexpected error handling {address}: {e}")
        finally:
            # Cleanup
            with self.lock:
                if device_id in self.connections:
                    del self.connections[device_id]
            
            try:
                client_socket.shutdown(socket.SHUT_RDWR)
            except (OSError, socket.error):
                pass
            
            try:
                client_socket.close()
            except (OSError, socket.error):
                pass
            
            self._emit_signal('device_disconnected', device_id, "Connection closed")
            logger.info(f"Device disconnected: {device_id}")
            self._log(f"Device disconnected: {device_id}")
    
    def _process_message(self, device_id: str, message: str, client_socket: socket.socket):
        """Process a message from Android device"""
        try:
            # Parse using protocol adapter
            json_msg = self.adapter.android_to_json(message)
            if not json_msg:
                logger.warning(f"Failed to parse message from {device_id}: {message}")
                self._log(f"Failed to parse message from {device_id}: {message}")
                return
            
            msg_type = json_msg.get('type')
            
            with self.lock:
                connection = self.connections.get(device_id)
                if connection:
                    connection.message_count += 1
                    connection.last_heartbeat = time.time()
            
            # Handle different message types
            if msg_type == 'DATA_GSR':
                self._handle_gsr_data(device_id, json_msg)
            
            elif msg_type == 'SYNC_RESPONSE':
                self._handle_sync_response(device_id, json_msg, client_socket)
            
            elif msg_type == 'ACK':
                self._handle_ack(device_id, json_msg)
            
            elif msg_type == 'ERROR':
                self._handle_error(device_id, json_msg)
            
            elif msg_type == 'FRAME':
                self._handle_frame(device_id, json_msg)
            
            else:
                # Generic message handling
                self._emit_signal('message_received', device_id, json_msg)
        
        except Exception as e:
            logger.exception(f"Error processing message from {device_id}: {e}")
            self._log(f"Error processing message from {device_id}: {e}")
    
    def _handle_gsr_data(self, device_id: str, json_msg: dict):
        """Handle GSR data message"""
        value = json_msg.get('value', 0.0)
        timestamp = json_msg.get('ts', time.time())
        
        with self.lock:
            connection = self.connections.get(device_id)
            if connection:
                connection.gsr_data.append((timestamp, value))
                # Keep last 1000 samples
                if len(connection.gsr_data) > 1000:
                    connection.gsr_data.pop(0)
        
        self._emit_signal('gsr_data_received', device_id, value, timestamp)
    
    def _handle_sync_response(self, device_id: str, json_msg: dict, client_socket: socket.socket):
        """Handle time sync response from Android"""
        t_pc = json_msg.get('t_pc', 0)  # T1 - PC sent sync request
        t_ph = json_msg.get('t_ph', 0)  # T2 - Phone received and responded
        t3 = int(time.time() * 1000)     # T3 - PC received response
        
        # Calculate offset and RTT (NTP algorithm)
        rtt_ms = t3 - t_pc
        offset_ms = int((t_ph - t_pc - rtt_ms / 2))
        
        with self.lock:
            connection = self.connections.get(device_id)
            if connection:
                connection.clock_offset_ms = offset_ms
                connection.rtt_ms = rtt_ms
                connection.sync_quality = "Good" if rtt_ms < 50 else "Fair" if rtt_ms < 100 else "Poor"
                connection.last_sync_time = time.time()
        
        # Send SYNC_RESULT back to Android
        sync_result = self.adapter.create_sync_result(t_pc, t_ph, t3, offset_ms, rtt_ms)
        try:
            client_socket.send((sync_result + '\n').encode('utf-8'))
        except (OSError, socket.error) as e:
            logger.error(f"Failed to send SYNC_RESULT to {device_id}: {e}")
        
        logger.info(f"Time sync completed for {device_id}: offset={offset_ms}ms, RTT={rtt_ms}ms")
        self._log(f"Time sync completed for {device_id}: offset={offset_ms}ms, RTT={rtt_ms}ms")
    
    def _handle_ack(self, device_id: str, json_msg: dict):
        """Handle ACK message from Android"""
        cmd = json_msg.get('cmd', 'UNKNOWN')
        logger.info(f"ACK from {device_id} for command: {cmd}")
        self._log(f"ACK from {device_id} for command: {cmd}")
        
        # Update connection state based on ACK
        if cmd == 'START_RECORD':
            with self.lock:
                connection = self.connections.get(device_id)
                if connection:
                    connection.is_recording = True
                    connection.session_id = json_msg.get('session_id')
        
        elif cmd == 'STOP_RECORD':
            with self.lock:
                connection = self.connections.get(device_id)
                if connection:
                    connection.is_recording = False
    
    def _handle_error(self, device_id: str, json_msg: dict):
        """Handle ERROR message from Android"""
        cmd = json_msg.get('cmd', 'UNKNOWN')
        code = json_msg.get('code', 'UNKNOWN')
        msg = json_msg.get('msg', 'No message')
        
        logger.error(f"ERROR from {device_id} for {cmd}: {code} - {msg}")
        self._log(f"ERROR from {device_id} for {cmd}: {code} - {msg}")
    
    def _handle_frame(self, device_id: str, json_msg: dict):
        """Handle frame data (placeholder)"""
        frame_type = json_msg.get('frame_type', 'UNKNOWN')
        logger.debug(f"Frame received from {device_id}: {frame_type}")
        self._log(f"Frame received from {device_id}: {frame_type}")
    
    def send_command(self, device_id: str, command: str, **params) -> bool:
        """Send command to Android device"""
        with self.lock:
            connection = self.connections.get(device_id)
            if not connection:
                return False
            
            try:
                # Create message using protocol adapter
                json_msg = {'type': command, **params}
                android_msg = self.adapter.json_to_android(json_msg)
                
                connection.socket.send((android_msg + '\n').encode('utf-8'))
                logger.info(f"Sent to {device_id}: {android_msg}")
                self._log(f"Sent to {device_id}: {android_msg}")
                return True
            except (OSError, socket.error) as e:
                logger.error(f"Failed to send to {device_id}: {e}")
                self._log(f"Failed to send to {device_id}: {e}")
                return False
    
    def start_recording(self, device_id: str, session_id: str) -> bool:
        """Start recording on Android device"""
        return self.send_command(device_id, 'START_RECORD', session_id=session_id)
    
    def stop_recording(self, device_id: str, session_id: str) -> bool:
        """Stop recording on Android device"""
        return self.send_command(device_id, 'STOP_RECORD', session_id=session_id)
    
    def sync_time(self, device_id: str) -> bool:
        """Initiate time synchronization with Android device"""
        t1 = int(time.time() * 1000)
        return self.send_command(device_id, 'SYNC_REQUEST', t_pc=t1)
    
    def _emit_signal(self, signal_name: str, *args):
        """Emit PyQt signal if available"""
        if GUI_AVAILABLE and hasattr(self, signal_name):
            signal = getattr(self, signal_name)
            signal.emit(*args)
    
    def _log(self, message: str):
        """Log message"""
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        log_msg = f"[{timestamp}] {message}"
        if GUI_AVAILABLE and hasattr(self, 'log_message'):
            self.log_message.emit(log_msg)
    
    def get_connections(self) -> Dict[str, DeviceConnection]:
        """Get all active connections"""
        with self.lock:
            return dict(self.connections)
    
    def stop(self):
        """Stop the network thread gracefully"""
        logger.info("Stopping network thread")
        self.running = False
        
        # Close server socket first
        if self.server_socket:
            try:
                self.server_socket.shutdown(socket.SHUT_RDWR)
            except (OSError, socket.error):
                pass
            
            try:
                self.server_socket.close()
            except (OSError, socket.error):
                pass
        
        # Then close all client connections
        with self.lock:
            for connection in self.connections.values():
                try:
                    connection.socket.shutdown(socket.SHUT_RDWR)
                except (OSError, socket.error):
                    pass
                
                try:
                    connection.socket.close()
                except (OSError, socket.error):
                    pass
            self.connections.clear()


# Main controller class would be the same as unified_pc_controller.py
# Just use NetworkThread from this improved version

if __name__ == '__main__':
    logger.info("Starting improved unified PC controller")
    # ... rest of main() implementation
