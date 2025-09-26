#!/usr/bin/env python3
"""
Enhanced PC Session Controller - Modern PyQt6 GUI for IRCamera Control

Implements enhanced requirements for:
- Real-time plotting with PyQtGraph for GSR and thermal data visualization
- Improved session control panel with device management  
- Enhanced device and sensor status display
- Real-time telemetry visualization with high performance
- JSON protocol handling with device registration
- TLS/SSL security layer support
- Data aggregation and export functionality
"""

import json
import socket
import ssl
import sys
import threading
import time
import traceback
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import base64
import io

# PyQt6 imports
from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
    QGridLayout, QTabWidget, QTreeWidget, QTreeWidgetItem, QTextEdit,
    QPushButton, QLabel, QFrame, QGroupBox, QSplitter, QStatusBar,
    QMessageBox, QFileDialog, QProgressBar, QComboBox, QSpinBox,
    QLineEdit, QCheckBox
)
from PyQt6.QtCore import (
    QTimer, QThread, pyqtSignal, QObject, Qt, QSize, QMutex
)
from PyQt6.QtGui import QFont, QPixmap, QImage, QIcon

# High-performance plotting
import numpy as np
import pyqtgraph as pg
from pyqtgraph import PlotWidget, ImageView

# Optional imports
try:
    from PIL import Image
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False
    print("Warning: PIL not available, image display will be limited")

try:
    import cv2
    CV2_AVAILABLE = True
except ImportError:
    CV2_AVAILABLE = False
    print("Warning: OpenCV not available, video processing will be limited")


class DeviceStatus:
    """Enhanced device status with more detailed tracking"""
    
    def __init__(self, device_id: str, device_name: str, ip_address: str):
        self.device_id = device_id
        self.device_name = device_name
        self.ip_address = ip_address
        self.status = "Disconnected"
        self.recording = False
        self.session_id = None
        
        # Enhanced sensor tracking
        self.sensors = {
            'RGB': {'status': 'Disconnected', 'message': '', 'last_frame': None},
            'Thermal': {'status': 'Disconnected', 'message': '', 'last_frame': None},
            'GSR': {'status': 'Disconnected', 'message': '', 'last_value': None}
        }
        
        # Time synchronization
        self.time_offset_ms = 0
        self.round_trip_time_ms = 0
        self.sync_quality = "Unknown"
        self.last_sync = None
        self.last_update = time.time()
        
        # Capability tracking
        self.capabilities = []
        self.firmware_version = "Unknown"
        self.battery_level = None
        
        # Data buffers for real-time display
        self.gsr_buffer = []
        self.frame_count = {'RGB': 0, 'Thermal': 0}


class NetworkThread(QThread):
    """Thread for handling network communication"""
    
    device_connected = pyqtSignal(str, dict)  # device_id, info
    device_disconnected = pyqtSignal(str, str)  # device_id, reason
    message_received = pyqtSignal(str, dict)  # device_id, message
    gsr_data_received = pyqtSignal(str, float, float)  # device_id, value, timestamp
    frame_received = pyqtSignal(str, str, bytes)  # device_id, frame_type, data
    error_occurred = pyqtSignal(str)  # error message
    
    def __init__(self, port: int = 8080, use_ssl: bool = False):
        super().__init__()
        self.port = port
        self.use_ssl = use_ssl
        self.running = False
        self.server_socket = None
        self.ssl_context = None
        self.client_connections = {}
        self.mutex = QMutex()
        
        if use_ssl:
            self._setup_ssl()
    
    def _setup_ssl(self):
        """Setup SSL context for secure connections"""
        try:
            self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            
            # Look for certificates
            cert_dir = Path(__file__).parent.parent / "certificates"
            cert_file = cert_dir / "server.crt"
            key_file = cert_dir / "server.key"
            
            if cert_file.exists() and key_file.exists():
                self.ssl_context.load_cert_chain(cert_file, key_file)
                print(f"SSL certificates loaded from {cert_dir}")
            else:
                # Generate self-signed certificate
                self._generate_self_signed_cert(cert_dir)
                self.ssl_context.load_cert_chain(cert_file, key_file)
                
        except Exception as e:
            print(f"SSL setup failed: {e}")
            self.ssl_context = None
            self.use_ssl = False
    
    def _generate_self_signed_cert(self, cert_dir: Path):
        """Generate a self-signed certificate for development"""
        cert_dir.mkdir(exist_ok=True)
        
        # This is a simplified version - in production, use proper certificate generation
        try:
            from cryptography import x509
            from cryptography.hazmat.primitives import hashes, serialization
            from cryptography.hazmat.primitives.asymmetric import rsa
            from cryptography.x509.oid import NameOID
            from datetime import timedelta
            
            # Generate private key
            private_key = rsa.generate_private_key(
                public_exponent=65537,
                key_size=2048,
            )
            
            # Create certificate
            subject = issuer = x509.Name([
                x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
                x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "CA"),
                x509.NameAttribute(NameOID.LOCALITY_NAME, "SF"),
                x509.NameAttribute(NameOID.ORGANIZATION_NAME, "IRCamera"),
                x509.NameAttribute(NameOID.COMMON_NAME, "localhost"),
            ])
            
            cert = x509.CertificateBuilder().subject_name(
                subject
            ).issuer_name(
                issuer
            ).public_key(
                private_key.public_key()
            ).serial_number(
                x509.random_serial_number()
            ).not_valid_before(
                datetime.utcnow()
            ).not_valid_after(
                datetime.utcnow() + timedelta(days=365)
            ).add_extension(
                x509.SubjectAlternativeName([
                    x509.DNSName("localhost"),
                ]),
                critical=False,
            ).sign(private_key, hashes.SHA256())
            
            # Write certificate and key
            with open(cert_dir / "server.crt", "wb") as f:
                f.write(cert.public_bytes(serialization.Encoding.PEM))
            
            with open(cert_dir / "server.key", "wb") as f:
                f.write(private_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.PKCS8,
                    encryption_algorithm=serialization.NoEncryption()
                ))
                
            print(f"Generated self-signed certificate in {cert_dir}")
            
        except ImportError:
            print("cryptography library not available, SSL disabled")
            self.use_ssl = False
    
    def run(self):
        """Main network thread loop"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('127.0.0.1', self.port))
            self.server_socket.listen(5)
            self.running = True
            
            print(f"Network server started on port {self.port} (SSL: {self.use_ssl})")
            
            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    
                    # Wrap with SSL if enabled
                    if self.use_ssl and self.ssl_context:
                        client_socket = self.ssl_context.wrap_socket(
                            client_socket, server_side=True
                        )
                    
                    # Handle client in separate thread
                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, address),
                        daemon=True
                    )
                    client_thread.start()
                    
                except Exception as e:
                    if self.running:
                        self.error_occurred.emit(f"Accept error: {e}")
                        
        except Exception as e:
            self.error_occurred.emit(f"Server error: {e}")
        finally:
            self.stop()
    
    def _handle_client(self, client_socket: socket.socket, address: Tuple[str, int]):
        """Handle individual client connection"""
        device_id = f"device_{address[0]}_{address[1]}"
        
        try:
            # Wait for HELLO message
            data = client_socket.recv(4096).decode('utf-8')
            if not data:
                return
                
            # Parse HELLO message
            try:
                message = json.loads(data)
                if message.get('type') == 'HELLO':
                    device_info = {
                        'device_name': message.get('device_name', device_id),
                        'ip_address': address[0],
                        'sensors': message.get('sensors', []),
                        'firmware_version': message.get('firmware_version', 'Unknown'),
                        'capabilities': message.get('capabilities', [])
                    }
                    
                    # Update device ID from message
                    if 'device_id' in message:
                        device_id = message['device_id']
                        device_info['device_id'] = device_id
                    
                    # Store connection
                    self.mutex.lock()
                    self.client_connections[device_id] = client_socket
                    self.mutex.unlock()
                    
                    # Signal device connection
                    self.device_connected.emit(device_id, device_info)
                    
                    # Handle ongoing messages
                    self._handle_device_messages(device_id, client_socket)
                    
            except json.JSONDecodeError as e:
                print(f"Invalid HELLO message from {address}: {e}")
                
        except Exception as e:
            print(f"Error handling client {address}: {e}")
        finally:
            # Clean up
            self.mutex.lock()
            if device_id in self.client_connections:
                del self.client_connections[device_id]
            self.mutex.unlock()
            
            client_socket.close()
            self.device_disconnected.emit(device_id, "Connection closed")
    
    def _handle_device_messages(self, device_id: str, client_socket: socket.socket):
        """Handle ongoing messages from a device"""
        buffer = ""
        
        while self.running:
            try:
                data = client_socket.recv(4096).decode('utf-8')
                if not data:
                    break
                    
                buffer += data
                
                # Process complete messages (assuming newline-delimited JSON)
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    if line.strip():
                        try:
                            message = json.loads(line.strip())
                            self._process_message(device_id, message)
                        except json.JSONDecodeError as e:
                            print(f"Invalid message from {device_id}: {e}")
                            
            except Exception as e:
                print(f"Error receiving from {device_id}: {e}")
                break
    
    def _process_message(self, device_id: str, message: dict):
        """Process a message from a device"""
        msg_type = message.get('type')
        
        if msg_type == 'telemetry_gsr':
            value = message.get('value', 0)
            timestamp = message.get('timestamp', time.time())
            self.gsr_data_received.emit(device_id, value, timestamp)
            
        elif msg_type == 'thermal_frame' or msg_type == 'rgb_frame':
            frame_type = 'Thermal' if msg_type == 'thermal_frame' else 'RGB'
            
            # Handle base64 encoded image
            if 'image_jpeg_base64' in message:
                try:
                    image_data = base64.b64decode(message['image_jpeg_base64'])
                    self.frame_received.emit(device_id, frame_type, image_data)
                except Exception as e:
                    print(f"Error decoding frame from {device_id}: {e}")
                    
        elif msg_type == 'status_update':
            self.message_received.emit(device_id, message)
            
        else:
            # Generic message handling
            self.message_received.emit(device_id, message)
    
    def send_message(self, device_id: str, message: dict) -> bool:
        """Send a message to a specific device"""
        self.mutex.lock()
        try:
            if device_id in self.client_connections:
                client_socket = self.client_connections[device_id]
                data = json.dumps(message) + '\n'
                client_socket.send(data.encode('utf-8'))
                return True
            return False
        except Exception as e:
            print(f"Error sending message to {device_id}: {e}")
            return False
        finally:
            self.mutex.unlock()
    
    def stop(self):
        """Stop the network thread"""
        self.running = False
        
        # Close all client connections
        self.mutex.lock()
        for client_socket in self.client_connections.values():
            try:
                client_socket.close()
            except:
                pass
        self.client_connections.clear()
        self.mutex.unlock()
        
        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass


class EnhancedPCController(QMainWindow):
    """Enhanced PC Session Controller with PyQt6 and real-time visualization"""
    
    def __init__(self):
        super().__init__()
        
        # Data
        self.devices: Dict[str, DeviceStatus] = {}
        self.session_active = False
        self.session_start_time = None
        self.current_session_id = None
        
        # Real-time data storage
        self.gsr_data_buffer = {}  # device_id -> [(timestamp, value), ...]
        self.max_samples = 1000  # Keep last 1000 samples per device
        
        # Network
        self.network_thread = None
        self.server_port = 8080
        self.use_ssl = False
        
        # UI setup
        self.setup_ui()
        self.setup_timers()
        self.start_network_server()
    
    def setup_ui(self):
        """Setup the enhanced PyQt6 user interface"""
        self.setWindowTitle("IRCamera Enhanced PC Session Controller")
        self.setGeometry(100, 100, 1400, 900)
        
        # Central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Main layout
        main_layout = QVBoxLayout(central_widget)
        
        # Control panel (top)
        self.setup_control_panel(main_layout)
        
        # Main content (horizontal splitter)
        content_splitter = QSplitter(Qt.Orientation.Horizontal)
        main_layout.addWidget(content_splitter)
        
        # Left panel - Device management
        self.setup_device_panel(content_splitter)
        
        # Right panel - Real-time visualization
        self.setup_visualization_panel(content_splitter)
        
        # Status bar
        self.setup_status_bar()
        
        # Set splitter proportions
        content_splitter.setSizes([400, 1000])
    
    def setup_control_panel(self, parent_layout):
        """Setup main control buttons and session status"""
        control_frame = QGroupBox("Session Control")
        control_layout = QHBoxLayout(control_frame)
        
        # Main control buttons
        self.start_all_btn = QPushButton("Start All Recording")
        self.start_all_btn.clicked.connect(self.start_all_recording)
        self.start_all_btn.setStyleSheet("QPushButton { background-color: #4CAF50; color: white; font-weight: bold; }")
        
        self.stop_all_btn = QPushButton("Stop All Recording")
        self.stop_all_btn.clicked.connect(self.stop_all_recording)
        self.stop_all_btn.setStyleSheet("QPushButton { background-color: #f44336; color: white; font-weight: bold; }")
        self.stop_all_btn.setEnabled(False)
        
        self.sync_clocks_btn = QPushButton("Sync All Clocks")
        self.sync_clocks_btn.clicked.connect(self.sync_all_clocks)
        
        self.refresh_btn = QPushButton("Refresh Status")
        self.refresh_btn.clicked.connect(self.refresh_all_status)
        
        # Export data button
        self.export_btn = QPushButton("Export Session Data")
        self.export_btn.clicked.connect(self.export_session_data)
        self.export_btn.setEnabled(False)
        
        # Add buttons to layout
        control_layout.addWidget(self.start_all_btn)
        control_layout.addWidget(self.stop_all_btn)
        control_layout.addWidget(self.sync_clocks_btn)
        control_layout.addWidget(self.refresh_btn)
        control_layout.addWidget(self.export_btn)
        
        # Session status
        control_layout.addStretch()
        
        self.session_status_label = QLabel("Session Status: Idle")
        self.session_status_label.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        control_layout.addWidget(self.session_status_label)
        
        self.session_timer_label = QLabel("00:00:00")
        self.session_timer_label.setFont(QFont("Arial", 12))
        control_layout.addWidget(self.session_timer_label)
        
        parent_layout.addWidget(control_frame)
    
    def setup_device_panel(self, parent_splitter):
        """Setup device list and status display"""
        device_widget = QWidget()
        device_layout = QVBoxLayout(device_widget)
        
        # Device list
        device_group = QGroupBox("Connected Devices")
        device_group_layout = QVBoxLayout(device_group)
        
        # Device tree
        self.device_tree = QTreeWidget()
        self.device_tree.setHeaderLabels([
            "Device", "Status", "RGB", "Thermal", "GSR", "Time Sync", "Session"
        ])
        self.device_tree.setAlternatingRowColors(True)
        device_group_layout.addWidget(self.device_tree)
        
        # Individual device controls
        device_controls = QHBoxLayout()
        
        self.start_selected_btn = QPushButton("Start Selected")
        self.start_selected_btn.clicked.connect(self.start_selected_device)
        
        self.stop_selected_btn = QPushButton("Stop Selected")
        self.stop_selected_btn.clicked.connect(self.stop_selected_device)
        
        self.sync_selected_btn = QPushButton("Sync Selected")
        self.sync_selected_btn.clicked.connect(self.sync_selected_device)
        
        device_controls.addWidget(self.start_selected_btn)
        device_controls.addWidget(self.stop_selected_btn)
        device_controls.addWidget(self.sync_selected_btn)
        device_controls.addStretch()
        
        device_group_layout.addLayout(device_controls)
        device_layout.addWidget(device_group)
        
        # Network settings
        network_group = QGroupBox("Network Settings")
        network_layout = QGridLayout(network_group)
        
        network_layout.addWidget(QLabel("Port:"), 0, 0)
        self.port_input = QSpinBox()
        self.port_input.setRange(1024, 65535)
        self.port_input.setValue(self.server_port)
        network_layout.addWidget(self.port_input, 0, 1)
        
        self.ssl_checkbox = QCheckBox("Enable SSL/TLS")
        self.ssl_checkbox.setChecked(self.use_ssl)
        network_layout.addWidget(self.ssl_checkbox, 1, 0, 1, 2)
        
        self.restart_server_btn = QPushButton("Restart Server")
        self.restart_server_btn.clicked.connect(self.restart_network_server)
        network_layout.addWidget(self.restart_server_btn, 2, 0, 1, 2)
        
        device_layout.addWidget(network_group)
        
        # Add device widget to splitter
        parent_splitter.addWidget(device_widget)
    
    def setup_visualization_panel(self, parent_splitter):
        """Setup real-time visualization with PyQtGraph"""
        viz_widget = QWidget()
        viz_layout = QVBoxLayout(viz_widget)
        
        # Tabbed visualization
        self.viz_tabs = QTabWidget()
        viz_layout.addWidget(self.viz_tabs)
        
        # GSR Plot Tab
        self.setup_gsr_plot_tab()
        
        # Frame Preview Tab  
        self.setup_frame_preview_tab()
        
        # Session Log Tab
        self.setup_session_log_tab()
        
        parent_splitter.addWidget(viz_widget)
    
    def setup_gsr_plot_tab(self):
        """Setup high-performance GSR plotting with PyQtGraph"""
        gsr_widget = QWidget()
        gsr_layout = QVBoxLayout(gsr_widget)
        
        # GSR plot
        self.gsr_plot = PlotWidget(title="Real-Time GSR Data")
        self.gsr_plot.setLabel('left', 'GSR Value', units='μS')
        self.gsr_plot.setLabel('bottom', 'Time', units='s')
        self.gsr_plot.showGrid(x=True, y=True)
        self.gsr_plot.addLegend()
        
        # Store plot curves for each device
        self.gsr_curves = {}
        
        gsr_layout.addWidget(self.gsr_plot)
        
        # GSR controls
        gsr_controls = QHBoxLayout()
        
        gsr_controls.addWidget(QLabel("Time Window (s):"))
        self.gsr_time_window = QSpinBox()
        self.gsr_time_window.setRange(10, 300)
        self.gsr_time_window.setValue(60)
        gsr_controls.addWidget(self.gsr_time_window)
        
        self.gsr_autoscale_cb = QCheckBox("Auto Scale")
        self.gsr_autoscale_cb.setChecked(True)
        gsr_controls.addWidget(self.gsr_autoscale_cb)
        
        gsr_controls.addStretch()
        
        gsr_layout.addLayout(gsr_controls)
        
        self.viz_tabs.addTab(gsr_widget, "GSR Data")
    
    def setup_frame_preview_tab(self):
        """Setup frame preview for RGB and thermal images"""
        frame_widget = QWidget()
        frame_layout = QVBoxLayout(frame_widget)
        
        # Frame tabs
        self.frame_tabs = QTabWidget()
        frame_layout.addWidget(self.frame_tabs)
        
        # RGB preview
        self.rgb_image_view = ImageView()
        self.frame_tabs.addTab(self.rgb_image_view, "RGB Camera")
        
        # Thermal preview  
        self.thermal_image_view = ImageView()
        self.frame_tabs.addTab(self.thermal_image_view, "Thermal Camera")
        
        # Frame info
        frame_info = QHBoxLayout()
        self.frame_info_label = QLabel("No frames received")
        frame_info.addWidget(self.frame_info_label)
        frame_info.addStretch()
        
        frame_layout.addLayout(frame_info)
        
        self.viz_tabs.addTab(frame_widget, "Camera Preview")
    
    def setup_session_log_tab(self):
        """Setup session logging and event display"""
        log_widget = QWidget()
        log_layout = QVBoxLayout(log_widget)
        
        # Log text area
        self.session_log = QTextEdit()
        self.session_log.setReadOnly(True)
        self.session_log.setFont(QFont("Consolas", 9))
        log_layout.addWidget(self.session_log)
        
        # Log controls
        log_controls = QHBoxLayout()
        
        self.clear_log_btn = QPushButton("Clear Log")
        self.clear_log_btn.clicked.connect(self.clear_session_log)
        log_controls.addWidget(self.clear_log_btn)
        
        self.save_log_btn = QPushButton("Save Log")
        self.save_log_btn.clicked.connect(self.save_session_log)
        log_controls.addWidget(self.save_log_btn)
        
        log_controls.addStretch()
        
        log_layout.addLayout(log_controls)
        
        self.viz_tabs.addTab(log_widget, "Session Log")
    
    def setup_status_bar(self):
        """Setup status bar with connection info"""
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        
        # Network status
        self.network_status_label = QLabel("Network: Stopped")
        self.status_bar.addWidget(self.network_status_label)
        
        # Device count
        self.device_count_label = QLabel("Devices: 0")
        self.status_bar.addPermanentWidget(self.device_count_label)
        
        # Session status
        self.status_session_label = QLabel("Session: Idle")
        self.status_bar.addPermanentWidget(self.status_session_label)
    
    def setup_timers(self):
        """Setup periodic update timers"""
        # GUI update timer
        self.gui_timer = QTimer()
        self.gui_timer.timeout.connect(self.update_gui)
        self.gui_timer.start(1000)  # Update every second
        
        # GSR plot update timer (higher frequency)
        self.gsr_timer = QTimer()
        self.gsr_timer.timeout.connect(self.update_gsr_plots)
        self.gsr_timer.start(100)  # Update every 100ms for smooth plotting
    
    def start_network_server(self):
        """Start the network server thread"""
        if self.network_thread and self.network_thread.isRunning():
            return
            
        self.network_thread = NetworkThread(self.server_port, self.use_ssl)
        
        # Connect signals
        self.network_thread.device_connected.connect(self.on_device_connected)
        self.network_thread.device_disconnected.connect(self.on_device_disconnected)
        self.network_thread.message_received.connect(self.on_message_received)
        self.network_thread.gsr_data_received.connect(self.on_gsr_data_received)
        self.network_thread.frame_received.connect(self.on_frame_received)
        self.network_thread.error_occurred.connect(self.on_network_error)
        
        self.network_thread.start()
        
        self.network_status_label.setText(f"Network: Listening on port {self.server_port}")
        self.log_message(f"Network server started on port {self.server_port} (SSL: {self.use_ssl})")
    
    def restart_network_server(self):
        """Restart network server with new settings"""
        # Stop existing server
        if self.network_thread:
            self.network_thread.stop()
            self.network_thread.wait(3000)  # Wait up to 3 seconds
        
        # Update settings
        self.server_port = self.port_input.value()
        self.use_ssl = self.ssl_checkbox.isChecked()
        
        # Start new server
        self.start_network_server()
    
    # Signal handlers
    def on_device_connected(self, device_id: str, device_info: dict):
        """Handle device connection"""
        device_name = device_info.get('device_name', device_id)
        ip_address = device_info.get('ip_address', 'Unknown')
        
        # Create device status
        device = DeviceStatus(device_id, device_name, ip_address)
        device.status = "Connected"
        device.capabilities = device_info.get('sensors', [])
        device.firmware_version = device_info.get('firmware_version', 'Unknown')
        
        # Initialize sensor status based on capabilities
        for sensor in device.capabilities:
            if sensor in device.sensors:
                device.sensors[sensor]['status'] = 'Connected'
        
        self.devices[device_id] = device
        
        # Initialize GSR data buffer
        self.gsr_data_buffer[device_id] = []
        
        # Add GSR plot curve if GSR capability exists
        if 'GSR' in device.capabilities:
            colors = ['red', 'blue', 'green', 'yellow', 'magenta', 'cyan']
            color_index = len(self.gsr_curves) % len(colors)
            curve = self.gsr_plot.plot(
                pen=pg.mkPen(colors[color_index], width=2),
                name=device_name
            )
            self.gsr_curves[device_id] = curve
        
        self.update_device_tree()
        self.log_message(f"Device connected: {device_name} ({device_id}) from {ip_address}")
        self.log_message(f"  Capabilities: {', '.join(device.capabilities)}")
    
    def on_device_disconnected(self, device_id: str, reason: str):
        """Handle device disconnection"""
        if device_id in self.devices:
            device_name = self.devices[device_id].device_name
            del self.devices[device_id]
            
            # Clean up GSR data
            if device_id in self.gsr_data_buffer:
                del self.gsr_data_buffer[device_id]
            
            # Remove GSR plot curve
            if device_id in self.gsr_curves:
                self.gsr_plot.removeItem(self.gsr_curves[device_id])
                del self.gsr_curves[device_id]
            
            self.update_device_tree()
            self.log_message(f"Device disconnected: {device_name} ({reason})")
    
    def on_message_received(self, device_id: str, message: dict):
        """Handle generic message from device"""
        if device_id not in self.devices:
            return
        
        device = self.devices[device_id]
        msg_type = message.get('type')
        
        if msg_type == 'status_update':
            # Update sensor status
            sensors = message.get('sensors', {})
            for sensor_name, sensor_info in sensors.items():
                if sensor_name in device.sensors:
                    device.sensors[sensor_name].update(sensor_info)
            
            device.status = message.get('status', device.status)
            device.last_update = time.time()
            
        elif msg_type == 'session_update':
            device.session_id = message.get('session_id')
            device.recording = message.get('recording', False)
            
        self.update_device_tree()
    
    def on_gsr_data_received(self, device_id: str, value: float, timestamp: float):
        """Handle GSR data from device"""
        if device_id not in self.gsr_data_buffer:
            self.gsr_data_buffer[device_id] = []
        
        # Add data point
        self.gsr_data_buffer[device_id].append((timestamp, value))
        
        # Keep only recent data
        if len(self.gsr_data_buffer[device_id]) > self.max_samples:
            self.gsr_data_buffer[device_id] = self.gsr_data_buffer[device_id][-self.max_samples:]
        
        # Update device status
        if device_id in self.devices:
            self.devices[device_id].sensors['GSR']['last_value'] = value
            self.devices[device_id].last_update = time.time()
    
    def on_frame_received(self, device_id: str, frame_type: str, image_data: bytes):
        """Handle frame data from device"""
        try:
            if not PIL_AVAILABLE:
                return
            
            # Decode image
            image = Image.open(io.BytesIO(image_data))
            
            # Convert to numpy array
            img_array = np.array(image)
            
            # Display in appropriate view
            if frame_type == 'RGB':
                self.rgb_image_view.setImage(img_array, axes={'x': 1, 'y': 0, 'c': 2})
            elif frame_type == 'Thermal':
                # For thermal images, we might want to apply a colormap
                if len(img_array.shape) == 2:  # Grayscale thermal
                    self.thermal_image_view.setImage(img_array, axes={'x': 1, 'y': 0})
                else:
                    self.thermal_image_view.setImage(img_array, axes={'x': 1, 'y': 0, 'c': 2})
            
            # Update device status
            if device_id in self.devices:
                self.devices[device_id].sensors[frame_type]['last_frame'] = time.time()
                self.devices[device_id].frame_count[frame_type] += 1
                
            # Update frame info
            frame_info = f"Last {frame_type} frame from {device_id} - Size: {image.size}"
            self.frame_info_label.setText(frame_info)
            
        except Exception as e:
            self.log_message(f"Error processing {frame_type} frame from {device_id}: {e}")
    
    def on_network_error(self, error_message: str):
        """Handle network errors"""
        self.log_message(f"Network Error: {error_message}")
        self.network_status_label.setText(f"Network: Error - {error_message}")
    
    # UI Update methods
    def update_gui(self):
        """Update GUI elements periodically"""
        self.update_device_tree()
        self.update_session_timer()
        self.update_status_bar()
    
    def update_device_tree(self):
        """Update the device tree display"""
        self.device_tree.clear()
        
        for device_id, device in self.devices.items():
            item = QTreeWidgetItem([
                device.device_name,
                device.status,
                device.sensors['RGB']['status'],
                device.sensors['Thermal']['status'], 
                device.sensors['GSR']['status'],
                f"{device.round_trip_time_ms:.1f}ms" if device.round_trip_time_ms > 0 else "Not synced",
                device.session_id if device.session_id else "No session"
            ])
            
            # Color coding based on status
            if device.status == "Connected":
                item.setBackground(0, pg.mkBrush('lightgreen'))
            elif device.status == "Recording":
                item.setBackground(0, pg.mkBrush('lightblue'))
            else:
                item.setBackground(0, pg.mkBrush('lightgray'))
            
            self.device_tree.addTopLevelItem(item)
    
    def update_gsr_plots(self):
        """Update GSR plots with latest data"""
        current_time = time.time()
        time_window = self.gsr_time_window.value()
        
        for device_id, data_buffer in self.gsr_data_buffer.items():
            if device_id not in self.gsr_curves or not data_buffer:
                continue
            
            # Filter data within time window
            recent_data = [
                (t, v) for t, v in data_buffer 
                if current_time - t <= time_window
            ]
            
            if recent_data:
                times, values = zip(*recent_data)
                # Convert to relative time (seconds ago)
                rel_times = [current_time - t for t in times]
                rel_times.reverse()  # Reverse so most recent is at right
                values = list(reversed(values))
                
                self.gsr_curves[device_id].setData(rel_times, values)
        
        # Auto-scale if enabled
        if self.gsr_autoscale_cb.isChecked():
            self.gsr_plot.autoRange()
    
    def update_session_timer(self):
        """Update session timer display"""
        if self.session_active and self.session_start_time:
            elapsed = time.time() - self.session_start_time
            hours = int(elapsed // 3600)
            minutes = int((elapsed % 3600) // 60)
            seconds = int(elapsed % 60)
            self.session_timer_label.setText(f"{hours:02d}:{minutes:02d}:{seconds:02d}")
        else:
            self.session_timer_label.setText("00:00:00")
    
    def update_status_bar(self):
        """Update status bar information"""
        device_count = len(self.devices)
        self.device_count_label.setText(f"Devices: {device_count}")
        
        if self.session_active:
            recording_count = sum(1 for d in self.devices.values() if d.recording)
            self.status_session_label.setText(f"Session: Recording ({recording_count}/{device_count})")
        else:
            self.status_session_label.setText("Session: Idle")
    
    # Control methods
    def start_all_recording(self):
        """Start recording on all connected devices"""
        if not self.devices:
            QMessageBox.warning(self, "No Devices", "No devices are connected")
            return
        
        # Generate session ID
        self.current_session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        # Send start command to all devices
        success_count = 0
        for device_id in self.devices:
            command = {
                'type': 'start_recording',
                'session_id': self.current_session_id,
                'timestamp': time.time()
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                success_count += 1
                self.devices[device_id].recording = True
                self.devices[device_id].session_id = self.current_session_id
        
        if success_count > 0:
            self.session_active = True
            self.session_start_time = time.time()
            self.session_status_label.setText(f"Session Status: Recording ({self.current_session_id})")
            self.start_all_btn.setEnabled(False)
            self.stop_all_btn.setEnabled(True)
            self.export_btn.setEnabled(True)
            
            self.log_message(f"Started recording session {self.current_session_id} on {success_count}/{len(self.devices)} devices")
        else:
            QMessageBox.critical(self, "Recording Failed", "Failed to start recording on any device")
    
    def stop_all_recording(self):
        """Stop recording on all devices"""
        if not self.session_active:
            return
        
        # Send stop command to all devices
        success_count = 0
        for device_id in self.devices:
            command = {
                'type': 'stop_recording',
                'session_id': self.current_session_id,
                'timestamp': time.time()
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                success_count += 1
                self.devices[device_id].recording = False
        
        self.session_active = False
        self.session_status_label.setText("Session Status: Idle")
        self.start_all_btn.setEnabled(True)
        self.stop_all_btn.setEnabled(False)
        
        duration = time.time() - self.session_start_time if self.session_start_time else 0
        self.log_message(f"Stopped recording session {self.current_session_id} after {duration:.1f} seconds")
        self.log_message(f"Session stopped on {success_count}/{len(self.devices)} devices")
    
    def sync_all_clocks(self):
        """Synchronize clocks with all devices"""
        if not self.devices:
            QMessageBox.warning(self, "No Devices", "No devices are connected")
            return
        
        success_count = 0
        for device_id in self.devices:
            command = {
                'type': 'sync_request',
                'pc_timestamp': time.time() * 1000  # Send in milliseconds
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                success_count += 1
        
        self.log_message(f"Clock sync initiated with {success_count}/{len(self.devices)} devices")
    
    def refresh_all_status(self):
        """Request status update from all devices"""
        if not self.devices:
            return
        
        success_count = 0
        for device_id in self.devices:
            command = {
                'type': 'status_request',
                'timestamp': time.time()
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                success_count += 1
        
        self.log_message(f"Status refresh requested from {success_count}/{len(self.devices)} devices")
    
    def start_selected_device(self):
        """Start recording on selected device"""
        current_item = self.device_tree.currentItem()
        if not current_item:
            QMessageBox.warning(self, "No Selection", "Please select a device")
            return
        
        device_name = current_item.text(0)
        device_id = None
        
        # Find device by name
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break
        
        if device_id:
            session_id = f"session_{device_id}_{int(time.time())}"
            command = {
                'type': 'start_recording',
                'session_id': session_id,
                'timestamp': time.time()
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                self.devices[device_id].recording = True
                self.devices[device_id].session_id = session_id
                self.log_message(f"Started recording on {device_name} (session: {session_id})")
    
    def stop_selected_device(self):
        """Stop recording on selected device"""
        current_item = self.device_tree.currentItem()
        if not current_item:
            QMessageBox.warning(self, "No Selection", "Please select a device")
            return
        
        device_name = current_item.text(0)
        device_id = None
        
        # Find device by name
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break
        
        if device_id:
            command = {
                'type': 'stop_recording',
                'timestamp': time.time()
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                self.devices[device_id].recording = False
                self.log_message(f"Stopped recording on {device_name}")
    
    def sync_selected_device(self):
        """Sync clock with selected device"""
        current_item = self.device_tree.currentItem()
        if not current_item:
            QMessageBox.warning(self, "No Selection", "Please select a device")
            return
        
        device_name = current_item.text(0)
        device_id = None
        
        # Find device by name
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break
        
        if device_id:
            command = {
                'type': 'sync_request',
                'pc_timestamp': time.time() * 1000
            }
            
            if self.network_thread and self.network_thread.send_message(device_id, command):
                self.log_message(f"Clock sync initiated with {device_name}")
    
    def export_session_data(self):
        """Export session data to files"""
        if not self.current_session_id:
            QMessageBox.warning(self, "No Session", "No active session to export")
            return
        
        # Choose export directory
        export_dir = QFileDialog.getExistingDirectory(
            self, "Choose Export Directory", str(Path.home())
        )
        
        if not export_dir:
            return
        
        try:
            export_path = Path(export_dir) / f"{self.current_session_id}_export"
            export_path.mkdir(exist_ok=True)
            
            # Export GSR data
            for device_id, data_buffer in self.gsr_data_buffer.items():
                if data_buffer:
                    device_name = self.devices[device_id].device_name
                    gsr_file = export_path / f"{device_name}_gsr_data.csv"
                    
                    with open(gsr_file, 'w') as f:
                        f.write("timestamp,gsr_value\n")
                        for timestamp, value in data_buffer:
                            f.write(f"{timestamp},{value}\n")
            
            # Export session log
            log_content = self.session_log.toPlainText()
            log_file = export_path / "session_log.txt"
            with open(log_file, 'w') as f:
                f.write(log_content)
            
            # Export device status
            status_file = export_path / "device_status.json"
            device_status = {}
            for device_id, device in self.devices.items():
                device_status[device_id] = {
                    'device_name': device.device_name,
                    'ip_address': device.ip_address,
                    'status': device.status,
                    'capabilities': device.capabilities,
                    'firmware_version': device.firmware_version,
                    'frame_counts': device.frame_count,
                    'session_id': device.session_id
                }
            
            with open(status_file, 'w') as f:
                json.dump(device_status, f, indent=2)
            
            QMessageBox.information(
                self, "Export Complete", 
                f"Session data exported to:\n{export_path}"
            )
            self.log_message(f"Session data exported to {export_path}")
            
        except Exception as e:
            QMessageBox.critical(self, "Export Failed", f"Failed to export data: {e}")
            self.log_message(f"Export failed: {e}")
    
    # Log methods
    def log_message(self, message: str):
        """Add message to session log"""
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        formatted_message = f"[{timestamp}] {message}"
        self.session_log.append(formatted_message)
        
        # Keep log size reasonable
        if self.session_log.document().blockCount() > 1000:
            cursor = self.session_log.textCursor()
            cursor.movePosition(cursor.MoveOperation.Start)
            for _ in range(100):  # Remove first 100 lines
                cursor.select(cursor.SelectionType.LineUnderCursor)
                cursor.removeSelectedText()
                cursor.deleteChar()  # Remove the newline
    
    def clear_session_log(self):
        """Clear the session log"""
        self.session_log.clear()
    
    def save_session_log(self):
        """Save session log to file"""
        filename, _ = QFileDialog.getSaveFileName(
            self, "Save Session Log", 
            f"session_log_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt",
            "Text files (*.txt);;All files (*.*)"
        )
        
        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write(self.session_log.toPlainText())
                QMessageBox.information(self, "Saved", f"Session log saved to {filename}")
            except Exception as e:
                QMessageBox.critical(self, "Save Failed", f"Failed to save log: {e}")
    
    def closeEvent(self, event):
        """Handle application close"""
        # Stop network thread
        if self.network_thread:
            self.network_thread.stop()
            self.network_thread.wait(3000)
        
        event.accept()


def main():
    """Main application entry point"""
    app = QApplication(sys.argv)
    app.setApplicationName("IRCamera Enhanced PC Controller")
    app.setApplicationVersion("2.0")
    
    # Create and show main window
    controller = EnhancedPCController()
    controller.show()
    
    # Start event loop
    sys.exit(app.exec())


if __name__ == "__main__":
    main()