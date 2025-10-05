#!/usr/bin/env python3
"""
IRCamera PC Controller - Unified Desktop Application

This is the single, definitive PC controller for the IRCamera multi-modal recording system.
It consolidates all previous implementations into one comprehensive solution.

Features:
- Modern PyQt6 GUI with real-time visualization
- Protocol compatibility (legacy text + modern JSON)
- Multi-device support with SSL/TLS security
- Real-time GSR plotting and image preview
- Session management and data export
- C++ backend integration (optional)

Usage: python3 pc_controller.py
"""

import argparse
import base64
import io
import json
import logging
import os
import re
import socket
import ssl
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple, Union

# Logging setup
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# GUI dependency detection
PYQTGRAPH_AVAILABLE = False
try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout, QWidget, QPushButton,
        QTextEdit, QListWidget, QSplitter, QStatusBar, QFileDialog, QMessageBox,
        QTabWidget, QGroupBox, QLabel, QSpinBox, QCheckBox, QTreeWidget, QTreeWidgetItem
    )
    from PyQt6.QtCore import QTimer, QThread, pyqtSignal, Qt
    from PyQt6.QtGui import QPixmap, QAction

    try:
        import pyqtgraph as pg

        PYQTGRAPH_AVAILABLE = True
    except ImportError:
        PYQTGRAPH_AVAILABLE = False

    GUI_AVAILABLE = True
    logger.info("️ PyQt6 GUI interface available")
except ImportError:
    GUI_AVAILABLE = False
    logger.info(" Running in CLI mode (PyQt6 not available)")


    # Dummy classes for CLI mode
    class QThread:
        pass


    class QMainWindow:
        pass

# Additional dependencies
try:
    import numpy as np

    NUMPY_AVAILABLE = True
except ImportError:
    NUMPY_AVAILABLE = False

try:
    from PIL import Image

    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False

# OpenCV for native webcam support
OPENCV_AVAILABLE = False
try:
    import cv2

    OPENCV_AVAILABLE = True
    logger.info(" OpenCV webcam support available")
except ImportError:
    logger.info(" No native webcam support (OpenCV not available)")

# Optional C++ backend
NATIVE_BACKEND_AVAILABLE = False
try:
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'pc-controller/enhanced_native_backend'))
    import enhanced_native_backend

    NATIVE_BACKEND_AVAILABLE = True
    logger.info(" C++ backend available for high-performance processing")
except ImportError:
    logger.info(" Using Python backend (C++ backend not available)")

# Network constants
CLIENT_SOCKET_TIMEOUT = 30.0  # Socket timeout in seconds for client connections


class WebcamCapture:
    """Native webcam capture using OpenCV for PC-side video recording"""

    def __init__(self):
        self.capture = None
        self.is_capturing = False
        self.frame_count = 0

    def start_capture(self, camera_id=0, width=640, height=480):
        """Start webcam capture"""
        if not OPENCV_AVAILABLE:
            logger.warning("OpenCV not available, webcam capture disabled")
            return False

        try:
            self.capture = cv2.VideoCapture(camera_id)
            if not self.capture.isOpened():
                logger.error(f"Failed to open camera {camera_id}")
                return False

            # Set resolution
            self.capture.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self.capture.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

            self.is_capturing = True
            self.frame_count = 0
            logger.info(f"Started webcam capture: {width}x{height}")
            return True

        except Exception as e:
            logger.error(f"Failed to start webcam capture: {e}")
            return False

    def capture_frame(self):
        """Capture a single frame and return as JPEG bytes"""
        if not self.is_capturing or not self.capture:
            return None

        try:
            ret, frame = self.capture.read()
            if not ret:
                return None

            self.frame_count += 1

            # Convert to JPEG
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
            return buffer.tobytes()

        except Exception as e:
            logger.error(f"Frame capture failed: {e}")
            return None

    def stop_capture(self):
        """Stop webcam capture"""
        self.is_capturing = False
        if self.capture:
            self.capture.release()
            self.capture = None
        logger.info(f"Stopped webcam capture (captured {self.frame_count} frames)")


class DataProcessor:
    """High-performance data processing with C++ backend integration"""

    def __init__(self):
        self.use_cpp_backend = NATIVE_BACKEND_AVAILABLE
        if self.use_cpp_backend:
            try:
                self.cpp_processor = enhanced_native_backend.DataProcessor()
                logger.info("Using C++ backend for data processing")
            except Exception as e:
                logger.warning(f"C++ backend initialization failed: {e}, falling back to Python")
                self.use_cpp_backend = False

    def process_gsr_data(self, raw_value: float, timestamp: float) -> Dict[str, Any]:
        """Process GSR data using high-performance backend if available"""
        if self.use_cpp_backend:
            try:
                # Use C++ backend for processing
                result = self.cpp_processor.process_gsr_sample(raw_value, timestamp)
                return {
                    'timestamp': timestamp,
                    'raw_value': raw_value,
                    'processed_value': result.gsr_microsiemens,
                    'quality': result.quality_score,
                    'artifacts_detected': result.has_artifacts
                }
            except Exception as e:
                logger.warning(f"C++ processing failed: {e}, using Python fallback")

        # Python fallback processing
        return self._process_gsr_python(raw_value, timestamp)

    def _process_gsr_python(self, raw_value: float, timestamp: float) -> Dict[str, Any]:
        """Python fallback for GSR processing"""
        # Simple processing - convert to microsiemens
        processed_value = raw_value * 0.00012207  # Shimmer3+ GSR+ calibration

        # Basic artifact detection
        artifacts_detected = abs(raw_value) > 4000 or raw_value < 0
        quality = 0.8 if not artifacts_detected else 0.3

        return {
            'timestamp': timestamp,
            'raw_value': raw_value,
            'processed_value': processed_value,
            'quality': quality,
            'artifacts_detected': artifacts_detected
        }

    def apply_filters(self, data: List[float], filter_type: str = "lowpass") -> List[float]:
        """Apply signal filters using C++ backend if available"""
        if self.use_cpp_backend and len(data) > 10:
            try:
                if filter_type == "lowpass":
                    return self.cpp_processor.apply_lowpass_filter(data, 5.0, 50.0)
                elif filter_type == "highpass":
                    return self.cpp_processor.apply_highpass_filter(data, 0.5, 50.0)
                elif filter_type == "notch":
                    return self.cpp_processor.apply_notch_filter(data, 50.0, 50.0)
            except Exception as e:
                logger.warning(f"C++ filtering failed: {e}, using Python fallback")

        # Python fallback - simple moving average for lowpass
        if filter_type == "lowpass" and len(data) > 5:
            window = 5
            filtered = []
            for i in range(len(data)):
                start = max(0, i - window // 2)
                end = min(len(data), i + window // 2 + 1)
                filtered.append(sum(data[start:end]) / (end - start))
            return filtered

        return data


class Protocol:
    """Unified protocol handler supporting both legacy text and modern JSON protocols"""

    # Legacy text protocol commands
    MSG_HELLO = "HELLO"
    MSG_SYNC_REQUEST = "SYNC_REQUEST"
    MSG_SYNC_RESPONSE = "SYNC_RESPONSE"
    MSG_SYNC_RESULT = "SYNC_RESULT"
    MSG_START_RECORD = "START_RECORD"
    MSG_STOP_RECORD = "STOP_RECORD"
    MSG_ACK = "ACK"
    MSG_ERROR = "ERROR"
    MSG_DATA_GSR = "DATA_GSR"
    MSG_FRAME = "FRAME"

    # JSON protocol message types
    JSON_HELLO = "hello"
    JSON_STATUS_UPDATE = "status_update"
    JSON_TELEMETRY_GSR = "telemetry_gsr"
    JSON_FRAME_RGB = "frame_rgb"
    JSON_FRAME_THERMAL = "frame_thermal"
    JSON_START_RECORDING = "start_recording"
    JSON_STOP_RECORDING = "stop_recording"
    JSON_SYNC_REQUEST = "sync_request"
    JSON_SYNC_RESPONSE = "sync_response"

    @staticmethod
    def is_json_message(message: str) -> bool:
        """Check if message is JSON format"""
        try:
            json.loads(message.strip())
            return True
        except (json.JSONDecodeError, ValueError):
            return False

    @staticmethod
    def parse_message(message: str) -> Dict[str, Any]:
        """Parse message in either legacy text or JSON format"""
        message = message.strip()

        if Protocol.is_json_message(message):
            # Parse JSON protocol
            try:
                return json.loads(message)
            except json.JSONDecodeError as e:
                logger.error(f"Failed to parse JSON message: {e}")
                return {"type": "error", "message": "Invalid JSON"}
        else:
            # Parse legacy text protocol
            return Protocol._parse_legacy_message(message)

    @staticmethod
    def _parse_legacy_message(message: str) -> Dict[str, Any]:
        """Parse legacy text protocol message"""
        parts = message.split()
        if not parts:
            return {"type": "error", "message": "Empty message"}

        msg_type = parts[0]
        params = {}

        # Extract parameters (key=value or "quoted value")
        for part in parts[1:]:
            if '=' in part:
                key, value = part.split('=', 1)
                # Handle quoted values
                if value.startswith('"') and value.endswith('"'):
                    value = value[1:-1]
                params[key] = value

        # Convert to unified format
        result = {"type": msg_type.lower(), "legacy": True}
        result.update(params)

        return result

    @staticmethod
    def create_message(msg_type: str, **params) -> str:
        """Create message in JSON format"""
        message = {"type": msg_type, "timestamp": time.time()}
        message.update(params)
        return json.dumps(message)


class DeviceStatus:
    """Enhanced device status tracking"""

    def __init__(self, device_id: str, address: str = ""):
        self.device_id = device_id
        self.address = address
        self.status = "Disconnected"
        self.recording = False
        self.protocol_version = "unknown"
        self.last_update = time.time()

        # Multi-sensor status
        self.sensors = {
            'RGB': {'status': 'Disconnected', 'message': ''},
            'Thermal': {'status': 'Disconnected', 'message': ''},
            'GSR': {'status': 'Disconnected', 'message': ''}
        }

        # Time synchronization
        self.time_offset_ms = 0
        self.round_trip_time_ms = 0
        self.sync_quality = "unknown"

        # Data buffers
        self.gsr_data = []
        self.max_buffer_size = 1000

    def update_from_message(self, message: Dict[str, Any]):
        """Update status from received message"""
        self.last_update = time.time()

        if message.get('type') == 'hello':
            self.status = "Connected"
            self.protocol_version = message.get('protocol_version', 'legacy' if message.get('legacy') else 'json')

        elif message.get('type') == 'status_update':
            self.status = message.get('status', self.status)
            if 'sensors' in message:
                self.sensors.update(message['sensors'])

        elif message.get('type') == 'telemetry_gsr':
            self.add_gsr_data(message.get('value', 0), message.get('timestamp', time.time()))

    def add_gsr_data(self, value: float, timestamp: float):
        """Add GSR data point to buffer"""
        self.gsr_data.append((timestamp, value))
        if len(self.gsr_data) > self.max_buffer_size:
            self.gsr_data.pop(0)


class NetworkThread(QThread if GUI_AVAILABLE else threading.Thread):
    """Network handling thread with SSL support"""

    # Signals for GUI updates
    if GUI_AVAILABLE:
        device_connected = pyqtSignal(str, str)
        device_disconnected = pyqtSignal(str, str)
        message_received = pyqtSignal(str, dict)
        log_message = pyqtSignal(str)

    def __init__(self, port: int = 8080, use_ssl: bool = False):
        super().__init__()
        self.port = port
        self.use_ssl = use_ssl
        self.running = False
        self.server_socket = None
        self.ssl_context = None
        self.client_connections = {}

        if use_ssl:
            self._setup_ssl()

    def _setup_ssl(self):
        """Setup SSL context with self-signed certificates"""
        try:
            self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)

            # Check for existing certificates
            cert_dir = Path(__file__).parent / "certificates"
            cert_file = cert_dir / "server.crt"
            key_file = cert_dir / "server.key"

            if cert_file.exists() and key_file.exists():
                self.ssl_context.load_cert_chain(cert_file, key_file)
                logger.info(f"Loaded existing SSL certificates from {cert_dir}")
            else:
                # Generate self-signed certificate
                logger.info("Generating self-signed SSL certificate...")
                cert_dir.mkdir(exist_ok=True)
                self._generate_self_signed_cert(cert_file, key_file)
                self.ssl_context.load_cert_chain(cert_file, key_file)

        except Exception as e:
            logger.error(f"Failed to setup SSL: {e}")
            self.ssl_context = None
            self.use_ssl = False

    def _generate_self_signed_cert(self, cert_file: Path, key_file: Path):
        """Generate self-signed certificate for development"""
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
                    x509.IPAddress("127.0.0.1"),
                ]),
                critical=False,
            ).sign(private_key, hashes.SHA256())

            # Write files
            with open(cert_file, "wb") as f:
                f.write(cert.public_bytes(serialization.Encoding.PEM))

            with open(key_file, "wb") as f:
                f.write(private_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.PKCS8,
                    encryption_algorithm=serialization.NoEncryption()
                ))

            logger.info(f"Generated self-signed certificate: {cert_file}")

        except ImportError:
            logger.error("cryptography library not available, cannot generate SSL certificate")
            raise

    def start_server(self):
        """Start the network server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(10)

            protocol = "SSL/TLS" if self.use_ssl else "TCP"
            logger.info(f" PC Controller server started on port {self.port} ({protocol})")

            self.running = True
            self.start()

        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            raise

    def stop_server(self):
        """Stop the network server"""
        self.running = False
        if self.server_socket:
            self.server_socket.close()

    def run(self):
        """Main network thread loop"""
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()

                # Set socket timeout to prevent hanging
                client_socket.settimeout(CLIENT_SOCKET_TIMEOUT)
                
                # Apply SSL wrapping if enabled
                if self.ssl_context:
                    client_socket = self.ssl_context.wrap_socket(client_socket, server_side=True)

                # Handle client in separate thread
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_socket, address),
                    daemon=True
                )
                client_thread.start()

            except Exception as e:
                if self.running:
                    logger.error(f"Error accepting connection: {e}")

    def _handle_client(self, client_socket: socket.socket, address: Tuple[str, int]):
        """Handle individual client connection"""
        client_id = f"{address[0]}:{address[1]}"
        logger.info(f" Device connected: {client_id}")

        if GUI_AVAILABLE:
            self.device_connected.emit(client_id, str(address))

        try:
            self.client_connections[client_id] = client_socket

            while self.running:
                try:
                    # Receive message
                    data = client_socket.recv(4096)
                    if not data:
                        break

                    message_str = data.decode('utf-8').strip()
                    if not message_str:
                        continue

                    # Parse message using unified protocol
                    message = Protocol.parse_message(message_str)

                    logger.info(f" Received from {client_id}: {message.get('type', 'unknown')}")

                    if GUI_AVAILABLE:
                        self.message_received.emit(client_id, message)

                    # Handle specific message types
                    self._process_message(client_id, client_socket, message)

                except socket.timeout:
                    continue
                except Exception as e:
                    logger.error(f"Error handling message from {client_id}: {e}")
                    break

        except Exception as e:
            logger.error(f"Error handling client {client_id}: {e}")
        finally:
            client_socket.close()
            if client_id in self.client_connections:
                del self.client_connections[client_id]

            logger.info(f" Device disconnected: {client_id}")
            if GUI_AVAILABLE:
                self.device_disconnected.emit(client_id, "Connection closed")

    def _process_message(self, client_id: str, client_socket: socket.socket, message: Dict[str, Any]):
        """Process received message and send appropriate response"""
        msg_type = message.get('type', '').lower()

        if msg_type in ['hello', Protocol.MSG_HELLO.lower()]:
            # Send acknowledgment
            response = Protocol.create_message("hello_ack", server_time=time.time())
            self._send_message(client_socket, response)

        elif msg_type in ['sync_request', Protocol.MSG_SYNC_REQUEST.lower()]:
            # Handle time synchronization
            client_time = message.get('client_time', time.time())
            server_time = time.time()
            response = Protocol.create_message("sync_response",
                                               client_time=client_time,
                                               server_time=server_time)
            self._send_message(client_socket, response)

    def _send_message(self, client_socket: socket.socket, message: str):
        """Send message to client"""
        try:
            client_socket.send(message.encode('utf-8'))
        except Exception as e:
            logger.error(f"Failed to send message: {e}")

    def broadcast_command(self, command: str, **params):
        """Broadcast command to all connected devices"""
        message = Protocol.create_message(command, **params)

        for client_id, client_socket in self.client_connections.items():
            try:
                self._send_message(client_socket, message)
                logger.info(f" Sent {command} to {client_id}")
            except Exception as e:
                logger.error(f"Failed to send {command} to {client_id}: {e}")


class PCController(QMainWindow if GUI_AVAILABLE else object):
    """Main PC Controller application - unified implementation"""

    def __init__(self):
        if GUI_AVAILABLE:
            super().__init__()

        self.devices = {}  # Dict[str, DeviceStatus]
        self.network_thread = None
        self.current_session_id = None

        # Configuration
        self.config = {
            'port': 8080,
            'use_ssl': False,
            'gsr_plot_window': 30,  # seconds
            'auto_export': True,
            'export_format': ['csv', 'json']
        }

        if GUI_AVAILABLE:
            self._setup_gui()
        else:
            self._run_cli()

    def _setup_gui(self):
        """Setup PyQt6 GUI"""
        self.setWindowTitle("IRCamera PC Controller - Unified")
        self.setGeometry(100, 100, 1200, 800)

        # Central widget with tabs
        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        layout = QVBoxLayout(central_widget)

        # Control panel
        control_panel = self._create_control_panel()
        layout.addWidget(control_panel)

        # Main content tabs
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)

        # Device management tab
        self.device_tab = self._create_device_tab()
        self.tab_widget.addTab(self.device_tab, "Devices")

        # Visualization tab
        if PYQTGRAPH_AVAILABLE:
            self.viz_tab = self._create_visualization_tab()
            self.tab_widget.addTab(self.viz_tab, "Visualization")

        # Session log tab
        self.log_tab = self._create_log_tab()
        self.tab_widget.addTab(self.log_tab, "Session Log")

        # Status bar
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready")

        # Menu bar
        self._setup_menu()

        # Timer for updates
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_displays)
        self.update_timer.start(100)  # 10Hz updates

    def _create_control_panel(self):
        """Create main control panel"""
        panel = QGroupBox("Session Control")
        layout = QHBoxLayout(panel)

        # Server controls
        self.start_server_btn = QPushButton("Start Server")
        self.start_server_btn.clicked.connect(self._start_server)
        layout.addWidget(self.start_server_btn)

        self.stop_server_btn = QPushButton("Stop Server")
        self.stop_server_btn.clicked.connect(self._stop_server)
        self.stop_server_btn.setEnabled(False)
        layout.addWidget(self.stop_server_btn)

        # Session controls
        self.start_recording_btn = QPushButton("Start Recording")
        self.start_recording_btn.clicked.connect(self._start_recording)
        self.start_recording_btn.setEnabled(False)
        layout.addWidget(self.start_recording_btn)

        self.stop_recording_btn = QPushButton("Stop Recording")
        self.stop_recording_btn.clicked.connect(self._stop_recording)
        self.stop_recording_btn.setEnabled(False)
        layout.addWidget(self.stop_recording_btn)

        # Sync button
        self.sync_btn = QPushButton("Sync All Devices")
        self.sync_btn.clicked.connect(self._sync_devices)
        self.sync_btn.setEnabled(False)
        layout.addWidget(self.sync_btn)

        # Export button
        self.export_btn = QPushButton("Export Data")
        self.export_btn.clicked.connect(self._export_data)
        layout.addWidget(self.export_btn)

        # SSL checkbox
        self.ssl_checkbox = QCheckBox("Use SSL/TLS")
        layout.addWidget(self.ssl_checkbox)

        # Port spinner
        layout.addWidget(QLabel("Port:"))
        self.port_spinner = QSpinBox()
        self.port_spinner.setRange(1024, 65535)
        self.port_spinner.setValue(8080)
        layout.addWidget(self.port_spinner)

        # Webcam controls (if OpenCV available)
        if OPENCV_AVAILABLE:
            self.webcam_btn = QPushButton("Start PC Webcam")
            self.webcam_btn.clicked.connect(self._toggle_webcam)
            layout.addWidget(self.webcam_btn)

        return panel

    def _create_device_tab(self):
        """Create device management tab"""
        widget = QWidget()
        layout = QVBoxLayout(widget)

        # Device list
        self.device_tree = QTreeWidget()
        self.device_tree.setHeaderLabels(["Device", "Status", "Protocol", "Sensors"])
        layout.addWidget(self.device_tree)

        return widget

    def _create_visualization_tab(self):
        """Create visualization tab with PyQtGraph"""
        widget = QWidget()
        layout = QVBoxLayout(widget)

        # GSR plot
        self.gsr_plot_widget = pg.PlotWidget(title="Real-time GSR Data")
        self.gsr_plot_widget.setLabel('left', 'GSR Value')
        self.gsr_plot_widget.setLabel('bottom', 'Time (s)')
        self.gsr_plot_widget.showGrid(True, True)
        layout.addWidget(self.gsr_plot_widget)

        # Image display area
        image_layout = QHBoxLayout()

        # RGB preview
        self.rgb_label = QLabel("RGB Camera Feed")
        self.rgb_label.setMinimumSize(320, 240)
        self.rgb_label.setStyleSheet("border: 1px solid gray;")
        image_layout.addWidget(self.rgb_label)

        # Thermal preview
        self.thermal_label = QLabel("Thermal Camera Feed")
        self.thermal_label.setMinimumSize(320, 240)
        self.thermal_label.setStyleSheet("border: 1px solid gray;")
        image_layout.addWidget(self.thermal_label)

        layout.addLayout(image_layout)

        return widget

    def _create_log_tab(self):
        """Create session log tab"""
        widget = QWidget()
        layout = QVBoxLayout(widget)

        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        layout.addWidget(self.log_text)

        return widget

    def _setup_menu(self):
        """Setup application menu"""
        menubar = self.menuBar()

        # File menu
        file_menu = menubar.addMenu('File')

        export_action = QAction('Export Session Data', self)
        export_action.triggered.connect(self._export_data)
        file_menu.addAction(export_action)

        file_menu.addSeparator()

        exit_action = QAction('Exit', self)
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)

        # Tools menu
        tools_menu = menubar.addMenu('Tools')

        settings_action = QAction('Settings', self)
        settings_action.triggered.connect(self._show_settings)
        tools_menu.addAction(settings_action)

    def _start_server(self):
        """Start the network server"""
        try:
            port = self.port_spinner.value()
            use_ssl = self.ssl_checkbox.isChecked()

            self.network_thread = NetworkThread(port, use_ssl)

            # Connect signals
            self.network_thread.device_connected.connect(self._on_device_connected)
            self.network_thread.device_disconnected.connect(self._on_device_disconnected)
            self.network_thread.message_received.connect(self._on_message_received)
            self.network_thread.log_message.connect(self._log_message)

            self.network_thread.start_server()

            # Update UI
            self.start_server_btn.setEnabled(False)
            self.stop_server_btn.setEnabled(True)
            self.start_recording_btn.setEnabled(True)
            self.sync_btn.setEnabled(True)

            self.status_bar.showMessage(f"Server running on port {port}")
            self._log_message(f"Server started on port {port} ({'SSL/TLS' if use_ssl else 'TCP'})")

        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to start server: {e}")

    def _stop_server(self):
        """Stop the network server"""
        if self.network_thread:
            self.network_thread.stop_server()
            self.network_thread = None

        # Update UI
        self.start_server_btn.setEnabled(True)
        self.stop_server_btn.setEnabled(False)
        self.start_recording_btn.setEnabled(False)
        self.stop_recording_btn.setEnabled(False)
        self.sync_btn.setEnabled(False)

        self.status_bar.showMessage("Server stopped")
        self._log_message("Server stopped")

    def _start_recording(self):
        """Start recording session on all devices"""
        self.current_session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        if self.network_thread:
            self.network_thread.broadcast_command("start_recording", session_id=self.current_session_id)

        self.start_recording_btn.setEnabled(False)
        self.stop_recording_btn.setEnabled(True)

        self._log_message(f"Started recording session: {self.current_session_id}")

    def _stop_recording(self):
        """Stop recording session on all devices"""
        if self.network_thread and self.current_session_id:
            self.network_thread.broadcast_command("stop_recording", session_id=self.current_session_id)

        self.start_recording_btn.setEnabled(True)
        self.stop_recording_btn.setEnabled(False)

        self._log_message(f"Stopped recording session: {self.current_session_id}")

        # Auto-export if enabled
        if self.config.get('auto_export', True):
            self._export_data()

    def _sync_devices(self):
        """Synchronize time with all connected devices"""
        if self.network_thread:
            self.network_thread.broadcast_command("sync_request", server_time=time.time())

        self._log_message("Time synchronization requested for all devices")

    def _export_data(self):
        """Export collected data"""
        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            export_dir = Path(f"exports/session_{timestamp}")
            export_dir.mkdir(parents=True, exist_ok=True)

            # Export device data
            for device_id, device in self.devices.items():
                # Export GSR data to CSV
                if device.gsr_data and 'csv' in self.config.get('export_format', []):
                    csv_file = export_dir / f"{device_id}_gsr.csv"
                    with open(csv_file, 'w') as f:
                        f.write("timestamp,gsr_value\n")
                        for timestamp, value in device.gsr_data:
                            f.write(f"{timestamp},{value}\n")

                # Export device status to JSON
                if 'json' in self.config.get('export_format', []):
                    json_file = export_dir / f"{device_id}_status.json"
                    status_data = {
                        'device_id': device.device_id,
                        'address': device.address,
                        'protocol_version': device.protocol_version,
                        'sensors': device.sensors,
                        'sync_info': {
                            'time_offset_ms': device.time_offset_ms,
                            'round_trip_time_ms': device.round_trip_time_ms,
                            'sync_quality': device.sync_quality
                        }
                    }
                    with open(json_file, 'w') as f:
                        json.dump(status_data, f, indent=2)

            # Export session log
            log_file = export_dir / "session_log.txt"
            with open(log_file, 'w') as f:
                f.write(self.log_text.toPlainText())

            self._log_message(f"Data exported to {export_dir}")
            QMessageBox.information(self, "Export Complete", f"Data exported to {export_dir}")

        except Exception as e:
            error_msg = f"Export failed: {e}"
            self._log_message(error_msg)
            QMessageBox.critical(self, "Export Error", error_msg)

    def _show_settings(self):
        """Show settings dialog"""
        QMessageBox.information(self, "Settings", "Settings dialog would be implemented here")

    def _on_device_connected(self, device_id: str, address: str):
        """Handle device connection"""
        self.devices[device_id] = DeviceStatus(device_id, address)
        self._update_device_tree()
        self._log_message(f"Device connected: {device_id} ({address})")

    def _on_device_disconnected(self, device_id: str, reason: str):
        """Handle device disconnection"""
        if device_id in self.devices:
            del self.devices[device_id]
        self._update_device_tree()
        self._log_message(f"Device disconnected: {device_id} ({reason})")

    def _on_message_received(self, device_id: str, message: Dict[str, Any]):
        """Handle received message from device"""
        if device_id in self.devices:
            self.devices[device_id].update_from_message(message)
            self._update_device_tree()

        # Handle specific message types
        msg_type = message.get('type', '').lower()

        if msg_type == 'frame_rgb' and 'image_jpeg_base64' in message:
            self._update_rgb_preview(message['image_jpeg_base64'])

        elif msg_type == 'frame_thermal' and 'image_jpeg_base64' in message:
            self._update_thermal_preview(message['image_jpeg_base64'])

    def _update_device_tree(self):
        """Update device tree widget"""
        self.device_tree.clear()

        for device_id, device in self.devices.items():
            item = QTreeWidgetItem([
                device_id,
                device.status,
                device.protocol_version,
                f"RGB: {device.sensors['RGB']['status']}, "
                f"Thermal: {device.sensors['Thermal']['status']}, "
                f"GSR: {device.sensors['GSR']['status']}"
            ])

            # Color coding
            if device.status == "Connected":
                item.setForeground(0, Qt.GlobalColor.green)
            elif device.status == "Disconnected":
                item.setForeground(0, Qt.GlobalColor.red)
            else:
                item.setForeground(0, Qt.GlobalColor.yellow)

            self.device_tree.addTopLevelItem(item)

    def _update_displays(self):
        """Update real-time displays"""
        if PYQTGRAPH_AVAILABLE and hasattr(self, 'gsr_plot_widget'):
            self._update_gsr_plot()

    def _update_gsr_plot(self):
        """Update GSR plot with latest data"""
        self.gsr_plot_widget.clear()

        current_time = time.time()
        window_start = current_time - self.config.get('gsr_plot_window', 30)

        for device_id, device in self.devices.items():
            if device.gsr_data:
                # Filter data within time window
                recent_data = [(t, v) for t, v in device.gsr_data if t >= window_start]

                if recent_data:
                    timestamps, values = zip(*recent_data)
                    # Convert to relative time (seconds ago)
                    rel_times = [current_time - t for t in timestamps]

                    self.gsr_plot_widget.plot(rel_times, values,
                                              pen=pg.mkPen(color=(255, 0, 0), width=2),
                                              name=device_id)

    def _update_rgb_preview(self, base64_image: str):
        """Update RGB camera preview"""
        if PIL_AVAILABLE and hasattr(self, 'rgb_label'):
            try:
                image_bytes = base64.b64decode(base64_image)
                image = Image.open(io.BytesIO(image_bytes))

                # Convert to QPixmap and display
                image_rgb = image.convert('RGB')
                qimage = image_rgb.toqpixmap()

                # Scale to fit label
                scaled_pixmap = qimage.scaled(self.rgb_label.size(), Qt.AspectRatioMode.KeepAspectRatio)
                self.rgb_label.setPixmap(scaled_pixmap)

            except Exception as e:
                logger.error(f"Failed to update RGB preview: {e}")

    def _update_thermal_preview(self, base64_image: str):
        """Update thermal camera preview"""
        if PIL_AVAILABLE and hasattr(self, 'thermal_label'):
            try:
                image_bytes = base64.b64decode(base64_image)
                image = Image.open(io.BytesIO(image_bytes))

                # Convert to QPixmap and display
                image_rgb = image.convert('RGB')
                qimage = image_rgb.toqpixmap()

                # Scale to fit label
                scaled_pixmap = qimage.scaled(self.thermal_label.size(), Qt.AspectRatioMode.KeepAspectRatio)
                self.thermal_label.setPixmap(scaled_pixmap)

            except Exception as e:
                logger.error(f"Failed to update thermal preview: {e}")

    def _log_message(self, message: str):
        """Add message to session log"""
        timestamp = datetime.now().strftime('%H:%M:%S')
        log_entry = f"[{timestamp}] {message}"

        if hasattr(self, 'log_text'):
            self.log_text.append(log_entry)

        logger.info(message)

    def _run_cli(self):
        """Run in CLI mode when GUI is not available"""
        logger.info("️ Starting PC Controller in CLI mode")

        # Start network server
        self.network_thread = NetworkThread(self.config['port'], self.config['use_ssl'])

        try:
            self.network_thread.start_server()

            logger.info("Server started. Press Ctrl+C to stop.")

            # Simple CLI loop
            while True:
                try:
                    command = input("\nEnter command (start/stop/sync/quit): ").strip().lower()

                    if command == 'start':
                        session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
                        self.network_thread.broadcast_command("start_recording", session_id=session_id)
                        logger.info(f"Started recording session: {session_id}")

                    elif command == 'stop':
                        self.network_thread.broadcast_command("stop_recording")
                        logger.info("Stopped recording session")

                    elif command == 'sync':
                        self.network_thread.broadcast_command("sync_request", server_time=time.time())
                        logger.info("Time synchronization requested")

                    elif command == 'quit':
                        break

                    else:
                        logger.info("Available commands: start, stop, sync, quit")

                except KeyboardInterrupt:
                    break
                except Exception as e:
                    logger.error(f"Command error: {e}")

        finally:
            if self.network_thread:
                self.network_thread.stop_server()
            logger.info("PC Controller stopped")


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description="IRCamera PC Controller - Unified")
    parser.add_argument('--port', type=int, default=8080, help='Server port (default: 8080)')
    parser.add_argument('--ssl', action='store_true', help='Enable SSL/TLS encryption')
    parser.add_argument('--cli', action='store_true', help='Force CLI mode')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose logging')

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    # Print banner
    print("=" * 60)
    print("IRCamera PC Controller - Unified Implementation")
    print("=" * 60)
    print(f"Version: 1.0")
    print(f"GUI Available: {GUI_AVAILABLE}")
    print(f"PyQtGraph Available: {PYQTGRAPH_AVAILABLE}")
    print(f"C++ Backend Available: {NATIVE_BACKEND_AVAILABLE}")
    print(f"SSL Support: {args.ssl}")
    print("=" * 60)

    if GUI_AVAILABLE and not args.cli:
        # Run GUI application
        app = QApplication(sys.argv)
        controller = PCController()
        controller.show()

        # Set configuration from args
        controller.config['port'] = args.port
        controller.config['use_ssl'] = args.ssl

        sys.exit(app.exec())
    else:
        # Run CLI application
        controller = PCController()
        controller.config['port'] = args.port
        controller.config['use_ssl'] = args.ssl


if __name__ == "__main__":
    main()
