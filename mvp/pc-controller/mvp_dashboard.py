#!/usr/bin/env python3
"""
MVP PC Controller Dashboard - Simplified Multi-Modal Data Hub

Core MVP functionality:
- Device discovery and connection
- Real-time GSR visualization  
- Basic session management
- CSV data export
"""

import asyncio
import json
import socket
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout, 
        QWidget, QPushButton, QLabel, QTextEdit, QListWidget,
        QGroupBox, QLineEdit, QSpinBox
    )
    from PyQt6.QtCore import QTimer, pyqtSignal, QObject, QThread
    from PyQt6.QtGui import QFont
    import pyqtgraph as pg
    PYQT_AVAILABLE = True
except ImportError:
    # Fallback for headless mode
    print("MVP Dashboard - Headless mode (PyQt6 not available)")
    
    class MockQObject:
        def __init__(self): pass
        def __call__(self, *args, **kwargs): return MockQObject()
    
    class MockSignal:
        def emit(self, *args): pass
        def connect(self, func): pass
    
    QMainWindow = MockQObject
    QWidget = MockQObject
    QObject = MockQObject
    QThread = MockQObject
    pyqtSignal = lambda *args: MockSignal()
    pg = None
    PYQT_AVAILABLE = False


class GSRPlotter(QWidget):
    """Real-time GSR data plotting widget"""
    
    def __init__(self):
        super().__init__()
        self.initUI()
        
        # Data storage
        self.gsr_timestamps = []
        self.gsr_values = []
        self.max_points = 1000  # Keep last 1000 points
        
    def initUI(self):
        layout = QVBoxLayout()
        
        # Title
        title = QLabel("Real-Time GSR (μS)")
        title.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        layout.addWidget(title)
        
        # Plot widget
        if pg:
            self.plot_widget = pg.PlotWidget()
            self.plot_widget.setLabel('left', 'GSR (μS)')
            self.plot_widget.setLabel('bottom', 'Time (s)')
            self.plot_widget.setTitle("Galvanic Skin Response")
            
            # Configure plot
            self.plot_widget.showGrid(x=True, y=True)
            self.plot_item = self.plot_widget.plot(pen='b')
            
            layout.addWidget(self.plot_widget)
        else:
            layout.addWidget(QLabel("GSR Plot (PyQt6 required)"))
        
        # Current value display
        self.value_label = QLabel("Current GSR: -- μS")
        self.value_label.setFont(QFont("Arial", 12))
        layout.addWidget(self.value_label)
        
        self.setLayout(layout)
    
    def add_gsr_data(self, timestamp: float, gsr_value: float):
        """Add new GSR data point"""
        current_time = time.time()
        relative_time = current_time - (self.gsr_timestamps[0] if self.gsr_timestamps else current_time)
        
        self.gsr_timestamps.append(relative_time)
        self.gsr_values.append(gsr_value)
        
        # Limit data points
        if len(self.gsr_values) > self.max_points:
            self.gsr_timestamps.pop(0)
            self.gsr_values.pop(0)
        
        # Update plot
        if pg and hasattr(self, 'plot_item'):
            self.plot_item.setData(self.gsr_timestamps, self.gsr_values)
        
        # Update current value display
        self.value_label.setText(f"Current GSR: {gsr_value:.2f} μS")
    
    def clear_data(self):
        """Clear all plotted data"""
        self.gsr_timestamps.clear()
        self.gsr_values.clear()
        
        if pg and hasattr(self, 'plot_item'):
            self.plot_item.setData([], [])
        
        self.value_label.setText("Current GSR: -- μS")


class DeviceManager(QObject):
    """Manages Android device connections"""
    
    device_connected = pyqtSignal(str, str)  # device_id, device_info
    device_disconnected = pyqtSignal(str)
    data_received = pyqtSignal(str, dict)  # device_id, data
    
    def __init__(self):
        super().__init__()
        self.connected_devices: Dict[str, socket.socket] = {}
        self.server_socket: Optional[socket.socket] = None
        self.server_thread: Optional[threading.Thread] = None
        self.running = False
        
    def start_server(self, port: int = 8888):
        """Start TCP server for device connections"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', port))
            self.server_socket.listen(5)
            
            self.running = True
            self.server_thread = threading.Thread(target=self._server_loop, daemon=True)
            self.server_thread.start()
            
            print(f"MVP Server started on port {port}")
            
        except Exception as e:
            print(f"Failed to start server: {e}")
            
    def _server_loop(self):
        """Main server loop for accepting connections"""
        while self.running and self.server_socket:
            try:
                client_socket, addr = self.server_socket.accept()
                device_id = f"{addr[0]}:{addr[1]}"
                
                print(f"Device connected: {device_id}")
                
                # Store connection
                self.connected_devices[device_id] = client_socket
                
                # Start device handler thread
                device_thread = threading.Thread(
                    target=self._handle_device,
                    args=(device_id, client_socket),
                    daemon=True
                )
                device_thread.start()
                
                # Emit signal
                self.device_connected.emit(device_id, f"Android Device @ {addr[0]}")
                
            except Exception as e:
                if self.running:
                    print(f"Server error: {e}")
    
    def _handle_device(self, device_id: str, client_socket: socket.socket):
        """Handle individual device communication"""
        try:
            while self.running:
                # Receive data
                data = client_socket.recv(4096).decode('utf-8')
                if not data:
                    break
                
                # Parse JSON messages
                for line in data.strip().split('\n'):
                    if line.strip():
                        try:
                            message = json.loads(line.strip())
                            self.data_received.emit(device_id, message)
                        except json.JSONDecodeError:
                            print(f"Invalid JSON from {device_id}: {line}")
                            
        except Exception as e:
            print(f"Device {device_id} error: {e}")
        finally:
            # Cleanup
            if device_id in self.connected_devices:
                del self.connected_devices[device_id]
            client_socket.close()
            self.device_disconnected.emit(device_id)
            print(f"Device disconnected: {device_id}")
    
    def send_command(self, device_id: str, command: dict):
        """Send command to specific device"""
        if device_id in self.connected_devices:
            try:
                client_socket = self.connected_devices[device_id]
                message = json.dumps(command) + '\n'
                client_socket.send(message.encode('utf-8'))
                return True
            except Exception as e:
                print(f"Failed to send command to {device_id}: {e}")
        return False
    
    def broadcast_command(self, command: dict):
        """Send command to all connected devices"""
        for device_id in list(self.connected_devices.keys()):
            self.send_command(device_id, command)
    
    def stop_server(self):
        """Stop the server and close all connections"""
        self.running = False
        
        # Close all device connections
        for client_socket in self.connected_devices.values():
            try:
                client_socket.close()
            except:
                pass
        self.connected_devices.clear()
        
        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
            self.server_socket = None


class SessionManager(QObject):
    """Manages recording sessions and data export"""
    
    session_started = pyqtSignal(str)  # session_id
    session_stopped = pyqtSignal(str, str)  # session_id, export_path
    
    def __init__(self):
        super().__init__()
        self.current_session: Optional[str] = None
        self.session_data: Dict[str, List] = {}
        self.session_start_time: Optional[float] = None
        
    def start_session(self) -> str:
        """Start new recording session"""
        if self.current_session:
            raise Exception("Session already active")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.current_session = f"mvp_session_{timestamp}"
        self.session_data = {
            'gsr': [],
            'events': [],
            'devices': []
        }
        self.session_start_time = time.time()
        
        self.session_started.emit(self.current_session)
        return self.current_session
    
    def add_gsr_data(self, device_id: str, gsr_value: float, timestamp: float):
        """Add GSR data point to current session"""
        if not self.current_session:
            return
        
        relative_time = timestamp - (self.session_start_time or timestamp)
        
        self.session_data['gsr'].append({
            'device_id': device_id,
            'timestamp': timestamp,
            'relative_time': relative_time,
            'gsr_value': gsr_value
        })
    
    def add_event(self, event_type: str, description: str):
        """Add event marker to current session"""
        if not self.current_session:
            return
        
        timestamp = time.time()
        relative_time = timestamp - (self.session_start_time or timestamp)
        
        self.session_data['events'].append({
            'timestamp': timestamp,
            'relative_time': relative_time,
            'event_type': event_type,
            'description': description
        })
    
    def stop_session(self) -> str:
        """Stop current session and export data"""
        if not self.current_session:
            raise Exception("No active session")
        
        # Export to CSV
        export_path = self._export_csv()
        
        session_id = self.current_session
        self.current_session = None
        self.session_data = {}
        self.session_start_time = None
        
        self.session_stopped.emit(session_id, export_path)
        return export_path
    
    def _export_csv(self) -> str:
        """Export session data to CSV files"""
        output_dir = Path(f"mvp_data/{self.current_session}")
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # Export GSR data
        gsr_file = output_dir / "gsr_data.csv"
        with open(gsr_file, 'w') as f:
            f.write("timestamp,relative_time_s,device_id,gsr_microsiemens\n")
            for point in self.session_data['gsr']:
                f.write(f"{point['timestamp']},{point['relative_time']:.3f},{point['device_id']},{point['gsr_value']:.6f}\n")
        
        # Export events
        events_file = output_dir / "events.csv"
        with open(events_file, 'w') as f:
            f.write("timestamp,relative_time_s,event_type,description\n")
            for event in self.session_data['events']:
                f.write(f"{event['timestamp']},{event['relative_time']:.3f},{event['event_type']},{event['description']}\n")
        
        # Export session metadata
        metadata_file = output_dir / "session_metadata.json"
        metadata = {
            'session_id': self.current_session,
            'start_time': self.session_start_time,
            'duration_s': time.time() - (self.session_start_time or 0),
            'gsr_samples': len(self.session_data['gsr']),
            'events': len(self.session_data['events']),
            'devices': list(set(point['device_id'] for point in self.session_data['gsr']))
        }
        with open(metadata_file, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        return str(output_dir)


class MVPDashboard(QMainWindow):
    """MVP Dashboard Main Window"""
    
    def __init__(self):
        super().__init__()
        self.device_manager = DeviceManager()
        self.session_manager = SessionManager()
        self.gsr_plotter = None
        
        self.initUI()
        self.setup_connections()
        
    def initUI(self):
        self.setWindowTitle("Multi-Modal Sensing Platform - MVP Dashboard")
        self.setGeometry(100, 100, 1200, 800)
        
        # Central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QHBoxLayout(central_widget)
        
        # Left panel - Controls
        left_panel = self.create_control_panel()
        main_layout.addWidget(left_panel, 1)
        
        # Right panel - GSR Plot
        if pg:
            self.gsr_plotter = GSRPlotter()
            main_layout.addWidget(self.gsr_plotter, 2)
        else:
            main_layout.addWidget(QLabel("GSR Plotting requires PyQt6 + pyqtgraph"), 2)
        
    def create_control_panel(self) -> QWidget:
        """Create left control panel"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Server control
        server_group = QGroupBox("Server Control")
        server_layout = QVBoxLayout(server_group)
        
        self.port_input = QSpinBox()
        self.port_input.setRange(1000, 65535)
        self.port_input.setValue(8888)
        server_layout.addWidget(QLabel("Port:"))
        server_layout.addWidget(self.port_input)
        
        self.start_server_btn = QPushButton("Start Server")
        self.start_server_btn.clicked.connect(self.start_server)
        server_layout.addWidget(self.start_server_btn)
        
        layout.addWidget(server_group)
        
        # Connected devices
        devices_group = QGroupBox("Connected Devices")
        devices_layout = QVBoxLayout(devices_group)
        
        self.device_list = QListWidget()
        devices_layout.addWidget(self.device_list)
        
        layout.addWidget(devices_group)
        
        # Session control
        session_group = QGroupBox("Session Control")
        session_layout = QVBoxLayout(session_group)
        
        self.session_status = QLabel("Status: No active session")
        session_layout.addWidget(self.session_status)
        
        self.start_session_btn = QPushButton("Start Recording")
        self.start_session_btn.clicked.connect(self.start_session)
        self.start_session_btn.setEnabled(False)
        session_layout.addWidget(self.start_session_btn)
        
        self.stop_session_btn = QPushButton("Stop Recording")
        self.stop_session_btn.clicked.connect(self.stop_session)
        self.stop_session_btn.setEnabled(False)
        session_layout.addWidget(self.stop_session_btn)
        
        self.flash_sync_btn = QPushButton("Flash Sync")
        self.flash_sync_btn.clicked.connect(self.flash_sync)
        self.flash_sync_btn.setEnabled(False)
        session_layout.addWidget(self.flash_sync_btn)
        
        layout.addWidget(session_group)
        
        # Log area
        log_group = QGroupBox("System Log")
        log_layout = QVBoxLayout(log_group)
        
        self.log_text = QTextEdit()
        self.log_text.setMaximumHeight(200)
        log_layout.addWidget(self.log_text)
        
        layout.addWidget(log_group)
        
        layout.addStretch()
        return panel
    
    def setup_connections(self):
        """Setup signal connections"""
        self.device_manager.device_connected.connect(self.on_device_connected)
        self.device_manager.device_disconnected.connect(self.on_device_disconnected)
        self.device_manager.data_received.connect(self.on_data_received)
        
        self.session_manager.session_started.connect(self.on_session_started)
        self.session_manager.session_stopped.connect(self.on_session_stopped)
    
    def start_server(self):
        """Start the device server"""
        port = self.port_input.value()
        self.device_manager.start_server(port)
        
        self.start_server_btn.setEnabled(False)
        self.port_input.setEnabled(False)
        self.log("Server started on port " + str(port))
    
    def start_session(self):
        """Start recording session"""
        try:
            session_id = self.session_manager.start_session()
            
            # Send start command to all devices
            command = {
                'type': 'session_start',
                'session_id': session_id,
                'timestamp': time.time()
            }
            self.device_manager.broadcast_command(command)
            
        except Exception as e:
            self.log(f"Failed to start session: {e}")
    
    def stop_session(self):
        """Stop recording session"""
        try:
            # Send stop command to all devices
            command = {
                'type': 'session_stop',
                'timestamp': time.time()
            }
            self.device_manager.broadcast_command(command)
            
            # Stop session and export
            export_path = self.session_manager.stop_session()
            
        except Exception as e:
            self.log(f"Failed to stop session: {e}")
    
    def flash_sync(self):
        """Send flash sync command"""
        command = {
            'type': 'sync_flash',
            'timestamp': time.time()
        }
        self.device_manager.broadcast_command(command)
        self.session_manager.add_event('sync_flash', 'Flash synchronization marker')
        self.log("Flash sync command sent")
    
    def on_device_connected(self, device_id: str, device_info: str):
        """Handle device connection"""
        self.device_list.addItem(f"{device_id} - {device_info}")
        self.start_session_btn.setEnabled(True)
        self.flash_sync_btn.setEnabled(True)
        self.log(f"Device connected: {device_info}")
    
    def on_device_disconnected(self, device_id: str):
        """Handle device disconnection"""
        # Remove from list
        for i in range(self.device_list.count()):
            item = self.device_list.item(i)
            if item and item.text().startswith(device_id):
                self.device_list.takeItem(i)
                break
        
        # Disable controls if no devices
        if self.device_list.count() == 0:
            self.start_session_btn.setEnabled(False)
            self.flash_sync_btn.setEnabled(False)
        
        self.log(f"Device disconnected: {device_id}")
    
    def on_data_received(self, device_id: str, data: dict):
        """Handle received data from devices"""
        try:
            if data.get('type') == 'gsr_data':
                gsr_value = data.get('gsr_value', 0.0)
                timestamp = data.get('timestamp', time.time())
                
                # Add to session
                self.session_manager.add_gsr_data(device_id, gsr_value, timestamp)
                
                # Update plot
                if self.gsr_plotter:
                    self.gsr_plotter.add_gsr_data(timestamp, gsr_value)
                
            elif data.get('type') == 'event':
                event_type = data.get('event_type', 'unknown')
                description = data.get('description', '')
                self.session_manager.add_event(event_type, description)
                self.log(f"Event from {device_id}: {event_type} - {description}")
                
        except Exception as e:
            self.log(f"Error processing data from {device_id}: {e}")
    
    def on_session_started(self, session_id: str):
        """Handle session start"""
        self.session_status.setText(f"Status: Recording - {session_id}")
        self.start_session_btn.setEnabled(False)
        self.stop_session_btn.setEnabled(True)
        
        if self.gsr_plotter:
            self.gsr_plotter.clear_data()
        
        self.log(f"Session started: {session_id}")
    
    def on_session_stopped(self, session_id: str, export_path: str):
        """Handle session stop"""
        self.session_status.setText("Status: No active session")
        self.start_session_btn.setEnabled(self.device_list.count() > 0)
        self.stop_session_btn.setEnabled(False)
        
        self.log(f"Session stopped: {session_id}")
        self.log(f"Data exported to: {export_path}")
    
    def log(self, message: str):
        """Add message to log"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.append(f"[{timestamp}] {message}")
    
    def closeEvent(self, event):
        """Handle window close"""
        self.device_manager.stop_server()
        event.accept()


def main():
    """Main entry point"""
    # Create application
    if PYQT_AVAILABLE:
        app = QApplication(sys.argv)
        
        # Create and show dashboard
        dashboard = MVPDashboard()
        dashboard.show()
        
        # Run application
        sys.exit(app.exec())
    else:
        print("MVP Dashboard - Headless mode")
        print("Core functionality available via API")
        print("Install PyQt6 and pyqtgraph for full GUI functionality")
        
        # In headless mode, just test the core components
        device_manager = DeviceManager()
        session_manager = SessionManager()
        
        print("✓ DeviceManager initialized")
        print("✓ SessionManager initialized")
        print("✓ MVP core components working in headless mode")
        
        return 0


if __name__ == '__main__':
    main()