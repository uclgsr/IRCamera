#!/usr/bin/env python3
"""
Unified PC Controller with Android Protocol Compatibility

This controller bridges the gap between PC and Android implementations by:
1. Supporting both text-based (Android) and JSON protocols
2. Handling all Android message types (ACK, ERROR, SYNC_RESPONSE, etc.)
3. Implementing complete time synchronization
4. Managing device lifecycle properly

Compatible with Android Protocol.kt specification.
"""

import sys
import socket
import threading
import time
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional, Tuple
from protocol_adapter import ProtocolAdapter, parse_android, format_android

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
    print("PyQt6 not available - running in CLI mode")

class DeviceConnection:
    """Represents a connected Android device"""
    
    def __init__(self, device_id: str, socket: socket.socket, address: tuple):
        self.device_id = device_id
        self.socket = socket
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
    """Network thread with Android protocol support"""
    
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
            
            self._log(f"Server started on port {self.port}")
            
            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    self._log(f"New connection from {address}")
                    
                    # Handle in separate thread
                    thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, address),
                        daemon=True
                    )
                    thread.start()
                except Exception as e:
                    if self.running:
                        self._log(f"Accept error: {e}")
        except Exception as e:
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
            client_socket.settimeout(30.0)
            
            # Wait for HELLO message
            initial_data = client_socket.recv(4096).decode('utf-8')
            if not initial_data:
                return
            
            # Parse HELLO using protocol adapter
            json_msg = parse_android(initial_data.strip())
            
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
                client_socket.send((ack + '\n').encode('utf-8'))
                
                # Notify
                device_info = {
                    'device_id': device_id,
                    'device_name': device_name,
                    'sensors': sensors,
                    'address': address[0]
                }
                self._emit_signal('device_connected', device_id, device_info)
                self._log(f"Device registered: {device_name} (sensors: {', '.join(sensors)})")
                
                # Handle messages
                while self.running:
                    data = client_socket.recv(4096).decode('utf-8')
                    if not data:
                        break
                    
                    buffer += data
                    connection.bytes_received += len(data)
                    
                    # Process complete messages (newline-delimited)
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        if line.strip():
                            self._process_message(device_id, line.strip(), client_socket)
            else:
                self._log(f"No HELLO message from {address}")
        
        except Exception as e:
            self._log(f"Client error {address}: {e}")
        finally:
            # Cleanup
            with self.lock:
                if device_id in self.connections:
                    del self.connections[device_id]
            
            try:
                client_socket.close()
            except:
                pass
            
            self._emit_signal('device_disconnected', device_id, "Connection closed")
            self._log(f"Device disconnected: {device_id}")
    
    def _process_message(self, device_id: str, message: str, client_socket: socket.socket):
        """Process a message from Android device"""
        try:
            # Parse using protocol adapter
            json_msg = parse_android(message)
            if not json_msg:
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
        client_socket.send((sync_result + '\n').encode('utf-8'))
        
        self._log(f"Time sync completed for {device_id}: offset={offset_ms}ms, RTT={rtt_ms}ms")
    
    def _handle_ack(self, device_id: str, json_msg: dict):
        """Handle ACK message from Android"""
        cmd = json_msg.get('cmd', 'UNKNOWN')
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
        
        self._log(f"ERROR from {device_id} for {cmd}: {code} - {msg}")
    
    def _handle_frame(self, device_id: str, json_msg: dict):
        """Handle frame data (placeholder)"""
        frame_type = json_msg.get('frame_type', 'UNKNOWN')
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
                android_msg = format_android(json_msg)
                
                connection.socket.send((android_msg + '\n').encode('utf-8'))
                self._log(f"Sent to {device_id}: {android_msg}")
                return True
            except Exception as e:
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
        print(log_msg)
        if GUI_AVAILABLE and hasattr(self, 'log_message'):
            self.log_message.emit(log_msg)
    
    def get_connections(self) -> Dict[str, DeviceConnection]:
        """Get all active connections"""
        with self.lock:
            return dict(self.connections)
    
    def stop(self):
        """Stop the network thread"""
        self.running = False
        
        with self.lock:
            for connection in self.connections.values():
                try:
                    connection.socket.close()
                except:
                    pass
            self.connections.clear()
        
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass

class UnifiedPCController(QMainWindow if GUI_AVAILABLE else object):
    """Unified PC Controller with Android protocol compatibility"""
    
    def __init__(self):
        if GUI_AVAILABLE:
            super().__init__()
            self.setup_ui()
        
        self.network = NetworkThread(port=8080)
        
        if GUI_AVAILABLE:
            # Connect signals
            self.network.device_connected.connect(self.on_device_connected)
            self.network.device_disconnected.connect(self.on_device_disconnected)
            self.network.gsr_data_received.connect(self.on_gsr_data)
            self.network.log_message.connect(self.on_log_message)
            
            # Update timer
            self.update_timer = QTimer()
            self.update_timer.timeout.connect(self.update_display)
            self.update_timer.start(100)  # 10 Hz
        
        self.network.start()
    
    def setup_ui(self):
        """Setup GUI"""
        self.setWindowTitle("Unified PC Controller - Android Compatible")
        self.setGeometry(100, 100, 1200, 800)
        
        central = QWidget()
        self.setCentralWidget(central)
        layout = QHBoxLayout(central)
        
        # Left panel - Device list
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        
        # Device tree
        left_layout.addWidget(QLabel("Connected Devices:"))
        self.device_tree = QTreeWidget()
        self.device_tree.setHeaderLabels(["Property", "Value"])
        left_layout.addWidget(self.device_tree)
        
        # Controls
        controls = QGroupBox("Controls")
        controls_layout = QVBoxLayout(controls)
        
        self.start_btn = QPushButton("Start Recording")
        self.start_btn.clicked.connect(self.start_recording)
        controls_layout.addWidget(self.start_btn)
        
        self.stop_btn = QPushButton("Stop Recording")
        self.stop_btn.clicked.connect(self.stop_recording)
        self.stop_btn.setEnabled(False)
        controls_layout.addWidget(self.stop_btn)
        
        self.sync_btn = QPushButton("Sync Time")
        self.sync_btn.clicked.connect(self.sync_time)
        controls_layout.addWidget(self.sync_btn)
        
        left_layout.addWidget(controls)
        
        # Right panel - Data and logs
        right_panel = QWidget()
        right_layout = QVBoxLayout(right_panel)
        
        # GSR plot
        if 'pg' in sys.modules:
            self.gsr_plot = pg.PlotWidget(title="Real-Time GSR Data")
            self.gsr_plot.setLabel('left', 'GSR', units='μS')
            self.gsr_plot.setLabel('bottom', 'Time', units='s')
            self.gsr_curves = {}
            right_layout.addWidget(self.gsr_plot)
        
        # Log
        right_layout.addWidget(QLabel("Event Log:"))
        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        self.log_text.setMaximumHeight(200)
        right_layout.addWidget(self.log_text)
        
        # Add panels
        splitter = QSplitter(Qt.Orientation.Horizontal)
        splitter.addWidget(left_panel)
        splitter.addWidget(right_panel)
        splitter.setSizes([400, 800])
        layout.addWidget(splitter)
        
        self.statusBar().showMessage("Server started on port 8080")
    
    def on_device_connected(self, device_id: str, info: dict):
        """Handle device connection"""
        self.update_device_tree()
        self.log_text.append(f"Device connected: {info['device_name']}")
    
    def on_device_disconnected(self, device_id: str, reason: str):
        """Handle device disconnection"""
        self.update_device_tree()
        self.log_text.append(f"Device disconnected: {device_id}")
    
    def on_gsr_data(self, device_id: str, value: float, timestamp: float):
        """Handle GSR data"""
        # Update plot
        if hasattr(self, 'gsr_plot'):
            if device_id not in self.gsr_curves:
                self.gsr_curves[device_id] = self.gsr_plot.plot(pen='g', name=device_id)
            
            # Get data
            connections = self.network.get_connections()
            if device_id in connections:
                data = connections[device_id].gsr_data
                if data:
                    times = [t - data[0][0] for t, v in data]
                    values = [v for t, v in data]
                    self.gsr_curves[device_id].setData(times, values)
    
    def on_log_message(self, message: str):
        """Handle log message"""
        self.log_text.append(message)
    
    def update_device_tree(self):
        """Update device tree display"""
        self.device_tree.clear()
        
        connections = self.network.get_connections()
        for device_id, conn in connections.items():
            device_item = QTreeWidgetItem([conn.device_name, ""])
            
            # Add details
            QTreeWidgetItem(device_item, ["Device ID", device_id])
            QTreeWidgetItem(device_item, ["Address", conn.address[0]])
            QTreeWidgetItem(device_item, ["Status", conn.status])
            QTreeWidgetItem(device_item, ["Recording", "Yes" if conn.is_recording else "No"])
            QTreeWidgetItem(device_item, ["Sensors", ", ".join(conn.sensors)])
            QTreeWidgetItem(device_item, ["Messages", str(conn.message_count)])
            QTreeWidgetItem(device_item, ["Clock Offset", f"{conn.clock_offset_ms} ms"])
            QTreeWidgetItem(device_item, ["RTT", f"{conn.rtt_ms} ms"])
            QTreeWidgetItem(device_item, ["Sync Quality", conn.sync_quality])
            
            self.device_tree.addTopLevelItem(device_item)
            device_item.setExpanded(True)
    
    def update_display(self):
        """Periodic display update"""
        self.update_device_tree()
    
    def start_recording(self):
        """Start recording on all devices"""
        session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        connections = self.network.get_connections()
        for device_id in connections:
            self.network.start_recording(device_id, session_id)
        
        self.start_btn.setEnabled(False)
        self.stop_btn.setEnabled(True)
        self.log_text.append(f"Started recording: {session_id}")
    
    def stop_recording(self):
        """Stop recording on all devices"""
        connections = self.network.get_connections()
        for device_id, conn in connections.items():
            if conn.session_id:
                self.network.stop_recording(device_id, conn.session_id)
        
        self.start_btn.setEnabled(True)
        self.stop_btn.setEnabled(False)
        self.log_text.append("Stopped recording")
    
    def sync_time(self):
        """Sync time with all devices"""
        connections = self.network.get_connections()
        for device_id in connections:
            self.network.sync_time(device_id)
        
        self.log_text.append("Time sync initiated")
    
    def closeEvent(self, event):
        """Handle window close"""
        self.network.stop()
        event.accept()

def main():
    """Main entry point"""
    if GUI_AVAILABLE:
        app = QApplication(sys.argv)
        controller = UnifiedPCController()
        controller.show()
        sys.exit(app.exec())
    else:
        print("Running in CLI mode (PyQt6 not available)")
        controller = UnifiedPCController()
        
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nShutting down...")
            controller.network.stop()

if __name__ == '__main__':
    main()
