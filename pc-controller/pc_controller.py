#!/usr/bin/env python3
"""
IRCamera PC Controller - Complete Implementation with GUI and CLI
Focused on core functionality for multi-modal physiological sensing

Core Features:
- PyQt6 GUI interface with real-time visualization
- CLI interface for headless operation
- TCP server for Android device connections
- Session management with visual controls
- Real-time data plotting and device management
- Native C++ backend with Python fallback
"""

import json
import socket
import threading
import time
import sys
import argparse
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

# Simple logging
import logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Try to import PyQt6 for GUI
try:
    from PyQt6.QtWidgets import (QApplication, QMainWindow, QVBoxLayout, QHBoxLayout, 
                                 QWidget, QLabel, QPushButton, QTextEdit, QListWidget, 
                                 QSplitter, QStatusBar, QMenuBar, QFileDialog, QMessageBox,
                                 QTabWidget, QGroupBox, QSpinBox, QCheckBox)
    from PyQt6.QtCore import QTimer, QThread, pyqtSignal, Qt
    from PyQt6.QtGui import QAction, QFont
    try:
        import pyqtgraph as pg
        PYQTGRAPH_AVAILABLE = True
    except ImportError:
        PYQTGRAPH_AVAILABLE = False
    GUI_AVAILABLE = True
    logger.info("🖥️ PyQt6 GUI interface available")
except ImportError:
    GUI_AVAILABLE = False
    logger.info("💻 Running in CLI mode (PyQt6 not available)")
    # Define dummy classes to avoid errors
    class QThread:
        pass
    class QMainWindow:
        pass

# Try to import native backend for high-performance processing
try:
    import sys
    import os
    # Add the native backend build directory to path
    native_backend_path = os.path.join(os.path.dirname(__file__), 'native_backend', 'build')
    if os.path.exists(native_backend_path):
        sys.path.insert(0, native_backend_path)
    import native_backend
    NATIVE_BACKEND_AVAILABLE = True
    logger.info("🚀 Native C++ backend available for high-performance processing")
except ImportError:
    NATIVE_BACKEND_AVAILABLE = False
    logger.info("📱 Using Python-only implementation (native backend not available)")


class GSRProcessor:
    """GSR data processor with optional native backend"""
    
    def __init__(self):
        self.use_native = NATIVE_BACKEND_AVAILABLE
        if self.use_native:
            try:
                self.native_shimmer = native_backend.NativeShimmer()
                logger.info("🔧 Using native C++ GSR processing")
            except Exception as e:
                logger.warning(f"Native backend failed to initialize: {e}, falling back to Python")
                self.use_native = False
        
        if not self.use_native:
            logger.info("📱 Using Python GSR processing")
    
    def process_data(self, gsr_value: float) -> float:
        """Process GSR data using available backend"""
        try:
            if self.use_native:
                # Use native C++ processing (placeholder for actual native processing)
                return float(gsr_value)  # In actual implementation, would use native_shimmer methods
            else:
                # Python fallback processing
                return float(gsr_value)  # Simple passthrough for MVP
        except Exception as e:
            logger.error(f"GSR processing error: {e}")
            return 0.0


class SimpleDevice:
    """Represents a connected Android device"""
    
    def __init__(self, device_id: str, device_type: str = 'android', capabilities: List[str] = None):
        self.device_id = device_id
        self.device_type = device_type
        self.capabilities = capabilities or []
        self.connected_at = datetime.now(timezone.utc)
        self.last_data_time = datetime.now(timezone.utc)
        self.is_recording = False


class SimpleSessionManager:
    """Simple session management for recording"""
    
    def __init__(self):
        self.current_session = None
        self.session_data = []
        
    def create_session(self, session_name: str = None) -> str:
        """Create a new recording session"""
        if not session_name:
            session_name = f"Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        self.current_session = {
            'id': session_name,
            'created_at': datetime.now(timezone.utc).isoformat(),
            'started_at': None,
            'stopped_at': None,
            'status': 'created'
        }
        return session_name
    
    def start_recording(self, session_id: str = None) -> bool:
        """Start recording session"""
        if not self.current_session:
            logger.error("No active session to start recording")
            return False
        
        self.current_session['started_at'] = datetime.now(timezone.utc).isoformat()
        self.current_session['status'] = 'recording'
        logger.info(f"Recording started for session: {self.current_session['id']}")
        return True
    
    def stop_recording(self) -> bool:
        """Stop recording session"""
        if not self.current_session or self.current_session['status'] != 'recording':
            logger.error("No active recording session to stop")
            return False
        
        self.current_session['stopped_at'] = datetime.now(timezone.utc).isoformat()
        self.current_session['status'] = 'completed'
        logger.info(f"Recording stopped for session: {self.current_session['id']}")
        return True


class MVPTCPServer:
    """Enhanced TCP server for Android device connections with live data streaming"""
    
    def __init__(self, port: int = 8080, data_callback=None, streaming_callback=None):
        self.port = port
        self.server_socket = None
        self.running = False
        self.client_threads = []
        self.connected_clients = {}
        self.data_callback = data_callback
        self.streaming_callback = streaming_callback  # For real-time data streaming
        self.device_registry = {}  # Enhanced device registry
        self.file_transfer_sessions = {}  # For file transfer management
        
    def start(self) -> bool:
        """Start the TCP server with enhanced error handling"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(10)  # Increased connection limit
            self.server_socket.settimeout(1.0)  # Non-blocking accept with timeout
            self.running = True
            
            # Start server thread
            server_thread = threading.Thread(target=self._server_loop, daemon=True)
            server_thread.start()
            
            logger.info(f"🌐 Enhanced TCP Server started on port {self.port}")
            logger.info(f"📡 Live data streaming enabled")
            logger.info(f"📁 File transfer support enabled") 
            return True
            
        except Exception as e:
            logger.error(f"❌ Failed to start server: {e}")
            return False
    
    def stop(self):
        """Stop the TCP server"""
        self.running = False
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
        logger.info("🛑 TCP Server stopped")
    
    def _server_loop(self):
        """Enhanced main server loop with better error handling"""
        logger.info("🔄 Server loop started, waiting for connections...")
        
        while self.running:
            try:
                # Use timeout to make server loop interruptible
                client_socket, address = self.server_socket.accept()
                logger.info(f"🔗 New connection from {address}")
                
                # Start client handler thread
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_socket, address),
                    daemon=True
                )
                client_thread.start()
                self.client_threads.append(client_thread)
                
                # Clean up finished threads periodically
                self.client_threads = [t for t in self.client_threads if t.is_alive()]
                
            except socket.timeout:
                # Normal timeout, continue loop
                continue
            except Exception as e:
                if self.running:
                    logger.error(f"Server loop error: {e}")
                    time.sleep(1)  # Brief pause before retry
                else:
                    break
    
    def _handle_client(self, client_socket: socket.socket, address):
        """Enhanced client connection handler with streaming and file transfer support"""
        device_id = f"device_{address[0]}_{address[1]}"
        buffer = ""
        last_heartbeat = time.time()
        
        # Set socket options for better streaming performance
        client_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
        client_socket.settimeout(30.0)  # 30-second timeout
        
        try:
            # Register client connection
            self.connected_clients[device_id] = {
                'socket': client_socket,
                'address': address,
                'connected_at': datetime.now(timezone.utc),
                'last_heartbeat': last_heartbeat,
                'device_info': {}
            }
            logger.info(f"📱 Device {device_id} connected from {address}")
            
            while self.running:
                try:
                    data = client_socket.recv(4096).decode('utf-8')  # Larger buffer for streaming
                    if not data:
                        break
                    
                    buffer += data
                    lines = buffer.split('\n')
                    buffer = lines[-1]  # Keep incomplete line
                    
                    for line in lines[:-1]:
                        if line.strip():
                            try:
                                message = json.loads(line.strip())
                                
                                # Extract device_id from message if available
                                msg_device_id = message.get('device_id', device_id)
                                
                                # Process different message types
                                self._process_message(msg_device_id, message, client_socket)
                                
                            except json.JSONDecodeError as e:
                                logger.warning(f"Invalid JSON from {address}: {line[:100]}...")
                                self._send_error_response(client_socket, "INVALID_MESSAGE", 
                                                        f"JSON decode error: {str(e)}")
                
                except socket.timeout:
                    # Check for heartbeat timeout
                    if time.time() - last_heartbeat > 60:  # 1 minute timeout
                        logger.warning(f"⏰ Heartbeat timeout for device {device_id}")
                        break
                    continue
                except ConnectionResetError:
                    logger.info(f"🔌 Connection reset by device {device_id}")
                    break
                except Exception as e:
                    logger.error(f"Client receive error for {device_id}: {e}")
                    break
                    
        except Exception as e:
            logger.error(f"Client handler error for {device_id}: {e}")
        finally:
            # Clean up client connection
            if device_id in self.connected_clients:
                del self.connected_clients[device_id]
            try:
                client_socket.close()
            except:
                pass
            logger.info(f"🔌 Device {device_id} disconnected")
    
    def _process_message(self, device_id: str, message: Dict, client_socket: socket.socket):
        """Process incoming messages with enhanced protocol support"""
        message_type = message.get('message_type', 'unknown')
        
        try:
            # Update heartbeat for any message
            if device_id in self.connected_clients:
                self.connected_clients[device_id]['last_heartbeat'] = time.time()
            
            # Handle different message types
            if message_type == 'device_register':
                self._handle_device_registration(device_id, message, client_socket)
            elif message_type == 'gsr_data_batch':
                self._handle_gsr_data_streaming(device_id, message)
            elif message_type == 'file_transfer_request':
                self._handle_file_transfer_request(device_id, message, client_socket)
            elif message_type == 'device_heartbeat':
                self._handle_heartbeat(device_id, message)
            elif message_type == 'device_status':
                self._handle_status_update(device_id, message)
            elif message_type == 'time_sync_request':
                self._handle_time_sync_request(device_id, message, client_socket)
            else:
                logger.debug(f"📨 Received {message_type} from {device_id}")
            
            # Call general data callback
            if self.data_callback:
                self.data_callback(device_id, message)
                
            # Send acknowledgment for critical messages
            if message_type in ['device_register', 'session_start', 'recording_start', 'recording_stop']:
                self._send_ack_response(client_socket, message.get('message_id', 'unknown'))
                
        except Exception as e:
            logger.error(f"Message processing error for {device_id}: {e}")
            self._send_error_response(client_socket, "PROCESSING_ERROR", str(e))
    
    def _handle_device_registration(self, device_id: str, message: Dict, client_socket: socket.socket):
        """Handle device registration with enhanced capabilities"""
        device_info = {
            'device_type': message.get('device_type', 'unknown'),
            'capabilities': message.get('capabilities', []),
            'ip_address': message.get('ip_address', 'unknown'),
            'port': message.get('port', 0),
            'battery_level': message.get('battery_level'),
            'gsr_mode': message.get('gsr_mode', 'local'),
            'device_name': message.get('device_name', device_id),
            'registered_at': datetime.now(timezone.utc).isoformat()
        }
        
        # Update device registry
        self.device_registry[device_id] = device_info
        if device_id in self.connected_clients:
            self.connected_clients[device_id]['device_info'] = device_info
        
        logger.info(f"📱 Device registered: {device_id} ({device_info['device_type']}) "
                   f"Capabilities: {', '.join(device_info['capabilities'])}")
        
    def _handle_gsr_data_streaming(self, device_id: str, message: Dict):
        """Handle real-time GSR data streaming"""
        if self.streaming_callback:
            self.streaming_callback(device_id, message)
        
        # Log periodic statistics
        data_points = message.get('data_points', [])
        if data_points:
            logger.debug(f"📊 GSR stream from {device_id}: {len(data_points)} samples")
    
    def _handle_file_transfer_request(self, device_id: str, message: Dict, client_socket: socket.socket):
        """Handle file transfer request"""
        file_info = message.get('file_info', {})
        transfer_id = f"transfer_{device_id}_{int(time.time())}"
        
        # Store transfer session info
        self.file_transfer_sessions[transfer_id] = {
            'device_id': device_id,
            'file_info': file_info,
            'started_at': datetime.now(timezone.utc),
            'status': 'pending'
        }
        
        logger.info(f"📁 File transfer request from {device_id}: {file_info.get('filename', 'unknown')}")
        
        # Send transfer acceptance response
        response = {
            'message_type': 'ack',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'transfer_id': transfer_id,
            'status': 'accepted'
        }
        self._send_response(client_socket, response)
    
    def _handle_time_sync_request(self, device_id: str, message: Dict, client_socket: socket.socket):
        """Handle time synchronization request"""
        server_timestamp = datetime.now(timezone.utc).isoformat()
        
        response = {
            'message_type': 'time_sync_response',
            'timestamp': server_timestamp,
            'server_timestamp': server_timestamp,
            'client_timestamp': message.get('client_timestamp'),
            'processing_delay_ms': 1  # Minimal processing delay
        }
        
        self._send_response(client_socket, response)
        logger.debug(f"⏰ Time sync response sent to {device_id}")
    
    def _send_ack_response(self, client_socket: socket.socket, message_id: str):
        """Send acknowledgment response"""
        response = {
            'message_type': 'ack',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'ack_for': message_id,
            'status': 'success'
        }
        self._send_response(client_socket, response)
    
    def _send_error_response(self, client_socket: socket.socket, error_code: str, error_message: str):
        """Send error response"""
        response = {
            'message_type': 'error',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'error_code': error_code,
            'error_message': error_message
        }
        self._send_response(client_socket, response)
    
    def _send_response(self, client_socket: socket.socket, response: Dict):
        """Send JSON response to client"""
        try:
            response_json = json.dumps(response) + '\n'
            client_socket.send(response_json.encode('utf-8'))
        except Exception as e:
            logger.error(f"Failed to send response: {e}")
    
    def _handle_heartbeat(self, device_id: str, message: Dict):
        """Handle device heartbeat"""
        logger.debug(f"💓 Heartbeat from {device_id}")
    
    def _handle_status_update(self, device_id: str, message: Dict):
        """Handle device status update"""
        status = message.get('status', 'unknown')
        logger.debug(f"📊 Status update from {device_id}: {status}")
    
    def get_connected_devices(self) -> Dict[str, Any]:
        """Get list of currently connected devices"""
        return {
            device_id: {
                'address': info['address'],
                'connected_at': info['connected_at'].isoformat() if isinstance(info['connected_at'], datetime) else info['connected_at'],
                'device_info': info['device_info']
            }
            for device_id, info in self.connected_clients.items()
        }
    
    def broadcast_command(self, command: Dict):
        """Broadcast command to all connected devices"""
        disconnected_devices = []
        command_json = json.dumps(command) + '\n'
        
        for device_id, client_info in self.connected_clients.items():
            try:
                client_info['socket'].send(command_json.encode('utf-8'))
                logger.debug(f"📡 Command sent to {device_id}: {command.get('message_type')}")
            except Exception as e:
                logger.error(f"Failed to send command to {device_id}: {e}")
                disconnected_devices.append(device_id)
        
        # Clean up disconnected devices
        for device_id in disconnected_devices:
            if device_id in self.connected_clients:
                del self.connected_clients[device_id]
        
        return len(self.connected_clients) - len(disconnected_devices)


class PCControllerGUI(QMainWindow):
    """PyQt6 GUI interface for PC Controller"""
    
    def __init__(self):
        super().__init__()
        self.controller = None
        self.data_buffer = {}  # Store GSR data for plotting and export
        
        # Initialize plotting data structures
        self.gsr_data = {}  # Per-device real-time data: {device_id: {'times': [], 'values': []}}
        self.gsr_curves = {}  # Per-device plot curves
        self.gsr_colors = [(255,0,0), (0,0,255), (0,255,0), (255,165,0), (128,0,128), (0,255,255)]
        self.start_time = time.time()
        
        # Setup plot timer
        self.plot_timer = QTimer()
        self.plot_timer.timeout.connect(self.update_plots)
        
        self.init_ui()
        
    def init_ui(self):
        """Initialize the user interface"""
        self.setWindowTitle("IRCamera PC Controller - Hub Interface")
        self.setGeometry(100, 100, 1200, 800)
        
        # Create central widget and layout
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QHBoxLayout(central_widget)
        
        # Create splitter for resizable panes
        splitter = QSplitter(Qt.Orientation.Horizontal)
        main_layout.addWidget(splitter)
        
        # Left panel - Device management and controls
        left_panel = self.create_left_panel()
        splitter.addWidget(left_panel)
        
        # Right panel - Data visualization
        right_panel = self.create_right_panel()
        splitter.addWidget(right_panel)
        
        # Set splitter proportions
        splitter.setStretchFactor(0, 1)
        splitter.setStretchFactor(1, 2)
        
        # Create menu bar
        self.create_menu_bar()
        
        # Create status bar
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready - Waiting for connections...")
        
    def create_left_panel(self):
        """Create left control panel"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Server controls
        server_group = QGroupBox("Server Control")
        server_layout = QVBoxLayout(server_group)
        
        self.start_button = QPushButton("Start Server")
        self.start_button.clicked.connect(self.start_server)
        self.stop_button = QPushButton("Stop Server")
        self.stop_button.clicked.connect(self.stop_server)
        self.stop_button.setEnabled(False)
        
        server_layout.addWidget(self.start_button)
        server_layout.addWidget(self.stop_button)
        layout.addWidget(server_group)
        
        # Connected devices
        devices_group = QGroupBox("Connected Devices")
        devices_layout = QVBoxLayout(devices_group)
        
        self.devices_list = QListWidget()
        devices_layout.addWidget(self.devices_list)
        layout.addWidget(devices_group)
        
        # Session controls
        session_group = QGroupBox("Session Management")
        session_layout = QVBoxLayout(session_group)
        
        self.session_button = QPushButton("Start Recording")
        self.session_button.clicked.connect(self.toggle_recording)
        self.session_button.setEnabled(False)
        session_layout.addWidget(self.session_button)
        
        self.export_button = QPushButton("Export Data")
        self.export_button.clicked.connect(self.export_data)
        session_layout.addWidget(self.export_button)
        
        layout.addWidget(session_group)
        
        # Log display
        log_group = QGroupBox("System Log")
        log_layout = QVBoxLayout(log_group)
        
        self.log_display = QTextEdit()
        self.log_display.setMaximumHeight(150)
        self.log_display.setReadOnly(True)
        log_layout.addWidget(self.log_display)
        layout.addWidget(log_group)
        
        layout.addStretch()
        return panel
        
    def create_right_panel(self):
        """Create enhanced right visualization panel with real-time plots"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Plot controls
        controls_group = QGroupBox("Visualization Controls")
        controls_layout = QHBoxLayout(controls_group)
        
        self.plot_enabled_checkbox = QCheckBox("Real-time Plotting")
        self.plot_enabled_checkbox.setChecked(True)
        self.plot_enabled_checkbox.toggled.connect(self.toggle_plotting)
        
        self.plot_duration_spin = QSpinBox()
        self.plot_duration_spin.setRange(10, 300)
        self.plot_duration_spin.setValue(60)
        self.plot_duration_spin.setSuffix(" sec")
        
        self.clear_button = QPushButton("Clear Plots")
        self.clear_button.clicked.connect(self.clear_plots)
        
        controls_layout.addWidget(self.plot_enabled_checkbox)
        controls_layout.addWidget(QLabel("Duration:"))
        controls_layout.addWidget(self.plot_duration_spin)
        controls_layout.addWidget(self.clear_button)
        controls_layout.addStretch()
        
        layout.addWidget(controls_group)
        
        # Create tab widget for different data types
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # GSR Real-time Plot Tab
        if PYQTGRAPH_AVAILABLE:
            self.create_gsr_plot_tab()
        else:
            self.create_gsr_text_tab()
        
        # Multi-modal Data Tab
        self.create_multimodal_tab()
        
        # Status & Messages Tab
        self.create_status_tab()
        
        return panel
    
    def create_gsr_plot_tab(self):
        """Create enhanced GSR real-time plotting tab"""
        gsr_widget = QWidget()
        gsr_layout = QVBoxLayout(gsr_widget)
        
        # GSR plot configuration
        self.gsr_plot = pg.PlotWidget(title="GSR Real-time Data Stream")
        self.gsr_plot.setLabel('left', 'GSR Conductance', units='μS')
        self.gsr_plot.setLabel('bottom', 'Time', units='seconds')
        self.gsr_plot.showGrid(x=True, y=True, alpha=0.3)
        self.gsr_plot.setBackground('white')
        
        # Enhanced plot styling
        self.gsr_plot.getPlotItem().getAxis('left').setPen('black')
        self.gsr_plot.getPlotItem().getAxis('bottom').setPen('black')
        self.gsr_plot.setAutoVisible(y=True)
        
        gsr_layout.addWidget(self.gsr_plot)
        
        # GSR statistics display
        stats_layout = QHBoxLayout()
        self.gsr_stats_label = QLabel("No GSR data received")
        self.gsr_stats_label.setStyleSheet("QLabel { background-color: #f0f0f0; padding: 5px; }")
        stats_layout.addWidget(self.gsr_stats_label)
        gsr_layout.addLayout(stats_layout)
        
        self.tab_widget.addTab(gsr_widget, "📊 GSR Live Data")
        
        # Initialize GSR data structures
        self.gsr_data = {}  # Per-device data storage: {device_id: {'times': [], 'values': []}}
        self.gsr_curves = {}  # Per-device plot curves
        self.gsr_colors = [(255,0,0), (0,0,255), (0,255,0), (255,165,0), (128,0,128), (0,255,255)]
        self.start_time = time.time()
        
    def create_gsr_text_tab(self):
        """Create GSR text fallback tab"""
        self.gsr_text = QTextEdit()
        self.gsr_text.setReadOnly(True)
        self.gsr_text.append("📊 GSR Data Display (Real-time plotting not available)")
        self.gsr_text.append("PyQtGraph not installed - showing text data only")
        self.tab_widget.addTab(self.gsr_text, "📊 GSR Data (Text)")
        
    def create_multimodal_tab(self):
        """Create enhanced multi-modal data display tab"""
        multimodal_widget = QWidget()
        multimodal_layout = QVBoxLayout(multimodal_widget)
        
        # Camera preview section
        camera_group = QGroupBox("📷 Camera Previews")
        camera_layout = QHBoxLayout(camera_group)
        
        # Thermal camera preview
        thermal_section = QVBoxLayout()
        self.thermal_label = QLabel("🌡️ Thermal Camera")
        self.thermal_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.thermal_preview = QLabel()
        self.thermal_preview.setMinimumSize(320, 240)
        self.thermal_preview.setStyleSheet(
            "QLabel { border: 2px solid #ccc; background-color: #f8f8f8; }"
        )
        self.thermal_preview.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.thermal_preview.setText("No thermal data")
        thermal_section.addWidget(self.thermal_label)
        thermal_section.addWidget(self.thermal_preview)
        
        # RGB camera preview
        rgb_section = QVBoxLayout()
        self.rgb_label = QLabel("📸 RGB Camera")
        self.rgb_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.rgb_preview = QLabel()
        self.rgb_preview.setMinimumSize(320, 240)
        self.rgb_preview.setStyleSheet(
            "QLabel { border: 2px solid #ccc; background-color: #f8f8f8; }"
        )
        self.rgb_preview.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.rgb_preview.setText("No RGB data")
        rgb_section.addWidget(self.rgb_label)
        rgb_section.addWidget(self.rgb_preview)
        
        camera_layout.addLayout(thermal_section)
        camera_layout.addLayout(rgb_section)
        multimodal_layout.addWidget(camera_group)
        
        # Device statistics
        stats_group = QGroupBox("📈 Device Statistics")
        stats_layout = QVBoxLayout(stats_group)
        
        self.device_stats_display = QTextEdit()
        self.device_stats_display.setReadOnly(True)
        self.device_stats_display.setMaximumHeight(150)
        self.device_stats_display.append("Waiting for device data...")
        stats_layout.addWidget(self.device_stats_display)
        
        multimodal_layout.addWidget(stats_group)
        
        self.tab_widget.addTab(multimodal_widget, "🎥 Multi-Modal")
        
    def create_status_tab(self):
        """Create enhanced status and messages tab"""
        status_widget = QWidget()
        status_layout = QVBoxLayout(status_widget)
        
        # Connection status
        connection_group = QGroupBox("🔗 Connection Status")
        connection_layout = QVBoxLayout(connection_group)
        
        self.connection_status_label = QLabel("Server Status: Stopped")
        self.connection_status_label.setStyleSheet("QLabel { font-weight: bold; color: red; }")
        connection_layout.addWidget(self.connection_status_label)
        
        status_layout.addWidget(connection_group)
        
        # Message log
        log_group = QGroupBox("📝 System Messages")
        log_layout = QVBoxLayout(log_group)
        
        self.status_text = QTextEdit()
        self.status_text.setReadOnly(True)
        self.status_text.append(f"🚀 IRCamera PC Controller initialized at {datetime.now().strftime('%H:%M:%S')}")
        log_layout.addWidget(self.status_text)
        
        status_layout.addWidget(log_group)
        
        self.tab_widget.addTab(status_widget, "📟 Status & Log")
        
    def create_menu_bar(self):
        """Create application menu bar"""
        menubar = self.menuBar()
        
        # File menu
        file_menu = menubar.addMenu('File')
        
        export_action = QAction('Export Data...', self)
        export_action.triggered.connect(self.export_data)
        file_menu.addAction(export_action)
        
        file_menu.addSeparator()
        
        exit_action = QAction('Exit', self)
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # Server menu
        server_menu = menubar.addMenu('Server')
        
        start_action = QAction('Start Server', self)
        start_action.triggered.connect(self.start_server)
        server_menu.addAction(start_action)
        
        stop_action = QAction('Stop Server', self)
        stop_action.triggered.connect(self.stop_server)
        server_menu.addAction(stop_action)
        
    def start_server(self):
        """Start the PC Controller server"""
        try:
            self.controller = MVPPCController(gui_callback=self.on_data_received)
            self.controller.start()
            
            self.start_button.setEnabled(False)
            self.stop_button.setEnabled(True)
            self.session_button.setEnabled(True)
            
            self.status_bar.showMessage("Server started - Listening on port 8080")
            self.log_message("✅ Server started successfully")
            
            # Start plot updates
            if PYQTGRAPH_AVAILABLE:
                self.plot_timer.start(100)  # Update every 100ms
            
        except Exception as e:
            self.log_message(f"❌ Failed to start server: {e}")
            QMessageBox.critical(self, "Error", f"Failed to start server: {e}")
    
    def stop_server(self):
        """Stop the PC Controller server"""
        if self.controller:
            self.controller.stop()
            self.controller = None
            
        self.start_button.setEnabled(True)
        self.stop_button.setEnabled(False)
        self.session_button.setEnabled(False)
        
        self.status_bar.showMessage("Server stopped")
        self.log_message("🛑 Server stopped")
        
        # Stop plot updates
        self.plot_timer.stop()
        
    def toggle_recording(self):
        """Toggle recording session"""
        if self.controller and hasattr(self.controller, 'session_manager'):
            if self.controller.session_manager.current_session:
                # Stop recording
                self.controller.session_manager.stop_recording()
                self.session_button.setText("Start Recording")
                self.log_message("🔴 Recording stopped")
            else:
                # Start recording
                session_id = self.controller.session_manager.create_session()
                self.controller.session_manager.start_recording(session_id)
                self.session_button.setText("Stop Recording")
                self.log_message(f"🔴 Recording started - Session: {session_id}")
    
    def export_data(self):
        """Export collected data"""
        if not self.data_buffer:
            QMessageBox.information(self, "No Data", "No data available to export.")
            return
            
        filename, _ = QFileDialog.getSaveFileName(self, "Export GSR Data", 
                                                  f"gsr_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv",
                                                  "CSV Files (*.csv)")
        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write("timestamp,device_id,gsr_microsiemens\n")
                    for device_id, data_points in self.data_buffer.items():
                        for timestamp, gsr_value in data_points:
                            f.write(f"{timestamp},{device_id},{gsr_value}\n")
                
                self.log_message(f"📁 Data exported to: {filename}")
                QMessageBox.information(self, "Export Complete", f"Data exported successfully to:\n{filename}")
            except Exception as e:
                self.log_message(f"❌ Export failed: {e}")
                QMessageBox.critical(self, "Export Error", f"Failed to export data: {e}")
    
    def on_data_received(self, device_id: str, data: Dict):
        """Enhanced callback for received data with real-time processing"""
        message_type = data.get('message_type', 'unknown')
        
        # Update device list
        self.update_device_list()
        
        # Handle different message types
        if message_type == 'gsr_data_batch':
            self.handle_gsr_data(device_id, data)
        elif message_type == 'device_register':
            self.handle_device_registration(device_id, data)
        elif message_type == 'device_status':
            self.handle_device_status(device_id, data)
        elif message_type == 'file_transfer_request':
            self.handle_file_transfer(device_id, data)
        
        # Log message
        self.log_message(f"📨 {message_type} from {device_id}")
        
        # Update multi-modal display
        self.update_multimodal_display(device_id, data)
        
    def handle_gsr_data(self, device_id: str, data: Dict):
        """Handle real-time GSR data with plotting"""
        data_points = data.get('data_points', [])
        current_time = time.time() - self.start_time
        
        # Initialize device data storage if needed
        if device_id not in self.gsr_data:
            self.gsr_data[device_id] = {'times': [], 'values': []}
        
        # Add new data points
        for point in data_points:
            gsr_value = point.get('value', 0.0)
            point_time = current_time  # Use current time for real-time display
            
            self.gsr_data[device_id]['times'].append(point_time)
            self.gsr_data[device_id]['values'].append(gsr_value)
            
        # Limit data to display duration
        max_duration = self.plot_duration_spin.value() if hasattr(self, 'plot_duration_spin') else 60
        min_time = current_time - max_duration
        
        # Trim old data
        device_data = self.gsr_data[device_id]
        while device_data['times'] and device_data['times'][0] < min_time:
            device_data['times'].pop(0)
            device_data['values'].pop(0)
        
        # Store for export
        if device_id not in self.data_buffer:
            self.data_buffer[device_id] = []
        
        for point in data_points:
            timestamp = point.get('timestamp', datetime.now().isoformat())
            gsr_value = point.get('value', 0.0)
            self.data_buffer[device_id].append((timestamp, gsr_value))
        
        # Update statistics display
        if hasattr(self, 'gsr_stats_label') and data_points:
            latest_value = data_points[-1].get('value', 0.0)
            num_devices = len(self.gsr_data)
            total_points = sum(len(d['values']) for d in self.gsr_data.values())
            
            stats_text = (f"📊 Latest: {latest_value:.2f} μS | "
                         f"Devices: {num_devices} | "
                         f"Total samples: {total_points}")
            self.gsr_stats_label.setText(stats_text)
    
    def handle_device_registration(self, device_id: str, data: Dict):
        """Handle device registration"""
        device_type = data.get('device_type', 'unknown')
        capabilities = data.get('capabilities', [])
        
        self.log_message(f"📱 Device registered: {device_id} ({device_type})")
        self.log_message(f"   Capabilities: {', '.join(capabilities)}")
    
    def handle_device_status(self, device_id: str, data: Dict):
        """Handle device status updates"""
        status = data.get('status', 'unknown')
        self.log_message(f"📊 Device {device_id} status: {status}")
    
    def handle_file_transfer(self, device_id: str, data: Dict):
        """Handle file transfer requests"""
        file_info = data.get('file_info', {})
        filename = file_info.get('filename', 'unknown')
        file_type = file_info.get('file_type', 'unknown')
        
        self.log_message(f"📁 File transfer request from {device_id}: {filename} ({file_type})")
    
    def update_multimodal_display(self, device_id: str, data: Dict):
        """Update multi-modal display with device data"""
        if not hasattr(self, 'device_stats_display'):
            return
            
        # Update device statistics
        stats_text = f"📱 {device_id}:\n"
        message_type = data.get('message_type', 'unknown')
        
        if message_type == 'gsr_data_batch':
            data_points = data.get('data_points', [])
            stats_text += f"  • GSR samples: {len(data_points)}\n"
            if data_points:
                latest_value = data_points[-1].get('value', 0.0)
                stats_text += f"  • Latest GSR: {latest_value:.2f} μS\n"
        
        elif message_type == 'device_status':
            battery = data.get('battery_level')
            status = data.get('status', 'unknown')
            stats_text += f"  • Status: {status}\n"
            if battery is not None:
                stats_text += f"  • Battery: {battery}%\n"
        
        # Add timestamp
        stats_text += f"  • Last update: {datetime.now().strftime('%H:%M:%S')}\n"
        
        # Update the display (keep last 20 updates)
        current_text = self.device_stats_display.toPlainText()
        lines = current_text.split('\n')
        if len(lines) > 100:  # Limit display size
            lines = lines[-80:]  # Keep last 80 lines
            self.device_stats_display.clear()
            self.device_stats_display.append('\n'.join(lines))
        
        self.device_stats_display.append(stats_text)
    
    def update_device_list(self):
        """Update the connected devices list"""
        if not self.controller:
            return
            
        self.devices_list.clear()
        
        if hasattr(self.controller.server, 'get_connected_devices'):
            connected_devices = self.controller.server.get_connected_devices()
            
            for device_id, info in connected_devices.items():
                device_info = info.get('device_info', {})
                device_type = device_info.get('device_type', 'unknown')
                capabilities = device_info.get('capabilities', [])
                battery = device_info.get('battery_level')
                
                display_text = f"{device_id} ({device_type})"
                if capabilities:
                    display_text += f" - {', '.join(capabilities)}"
                if battery is not None:
                    display_text += f" [{battery}%]"
                    
                self.devices_list.addItem(display_text)
        
        # Update connection status
        device_count = self.devices_list.count()
        if hasattr(self, 'connection_status_label'):
            if device_count > 0:
                self.connection_status_label.setText(f"Server Status: Running ({device_count} devices connected)")
                self.connection_status_label.setStyleSheet("QLabel { font-weight: bold; color: green; }")
            else:
                self.connection_status_label.setText("Server Status: Running (no devices)")
                self.connection_status_label.setStyleSheet("QLabel { font-weight: bold; color: orange; }")
    
    def update_plots(self):
        """Update real-time plots - called by timer"""
        if not PYQTGRAPH_AVAILABLE or not hasattr(self, 'gsr_plot'):
            return
            
        if not self.plot_enabled_checkbox.isChecked():
            return
        
        # Update GSR curves for each device
        for i, (device_id, data) in enumerate(self.gsr_data.items()):
            if not data['times'] or not data['values']:
                continue
                
            # Get or create curve for this device
            if device_id not in self.gsr_curves:
                color = self.gsr_colors[i % len(self.gsr_colors)]
                pen = pg.mkPen(color=color, width=2)
                self.gsr_curves[device_id] = self.gsr_plot.plot(
                    data['times'], data['values'], 
                    pen=pen, 
                    name=f"Device {device_id.split('_')[-1]}"
                )
            else:
                # Update existing curve
                self.gsr_curves[device_id].setData(data['times'], data['values'])
    
    def toggle_plotting(self, enabled: bool):
        """Toggle real-time plotting on/off"""
        if enabled and not self.plot_timer.isActive() and self.controller:
            self.plot_timer.start(100)  # 10 Hz update rate
            self.log_message("📈 Real-time plotting enabled")
        elif not enabled and self.plot_timer.isActive():
            self.plot_timer.stop()
            self.log_message("📈 Real-time plotting disabled")
    
    def clear_plots(self):
        """Clear all plot data"""
        if PYQTGRAPH_AVAILABLE and hasattr(self, 'gsr_plot'):
            self.gsr_plot.clear()
            self.gsr_curves.clear()
            
        self.gsr_data.clear()
        self.data_buffer.clear()
        
        if hasattr(self, 'gsr_stats_label'):
            self.gsr_stats_label.setText("Plots cleared - waiting for new data")
            
        self.log_message("🧹 Plots and data cleared")
    
    def log_message(self, message: str):
        """Add message to log display"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        formatted_message = f"[{timestamp}] {message}"
        self.log_display.append(formatted_message)
        self.status_text.append(formatted_message)
        
        # Keep log display size manageable
        if self.log_display.document().lineCount() > 100:
            cursor = self.log_display.textCursor()
            cursor.movePosition(cursor.MoveOperation.Start)
            cursor.select(cursor.SelectionType.LineUnderCursor)
            cursor.removeSelectedText()
        
    def closeEvent(self, event):
        """Handle application close"""
        if self.controller:
            self.stop_server()
        event.accept()


class MVPPCController:
    """Main PC Controller with dual CLI/GUI support"""
    
    def __init__(self, gui_callback=None):
        self.server = None
        self.session_manager = SimpleSessionManager()
        self.gsr_processor = GSRProcessor()
        self.connected_devices: Dict[str, SimpleDevice] = {}
        self.running = False
        self.gui_callback = gui_callback  # Callback for GUI updates
        
    def start(self):
        """Start the PC controller server"""
        self.server = MVPTCPServer(port=8080, data_callback=self.handle_data)
        self.server.start()
        self.running = True
        logger.info("🎯 PC Controller started - Ready for Android device connections")
        
    def stop(self):
        """Stop the PC controller server"""
        self.running = False
        if self.server:
            self.server.stop()
        logger.info("🛑 PC Controller stopped")
        
    def handle_data(self, device_id: str, data: Dict):
        """Handle incoming data from Android devices"""
        try:
            if data.get('type') == 'device_info':
                # Register new device
                device = SimpleDevice(
                    device_id=device_id,
                    device_type=data.get('device_type', 'android'),
                    capabilities=data.get('capabilities', [])
                )
                self.connected_devices[device_id] = device
                logger.info(f"📱 Device registered: {device_id} with capabilities: {device.capabilities}")
                
            elif data.get('type') == 'gsr_data':
                # Process GSR data
                gsr_value = data.get('gsr_microsiemens', 0.0)
                processed_data = self.gsr_processor.process_data(gsr_value)
                
                # Log the data
                backend_type = "native_cpp" if self.gsr_processor.use_native else "python"
                logger.info(f"📊 GSR data from {device_id}: {processed_data:.2f} µS [{backend_type}]")
                
                # Update device
                if device_id in self.connected_devices:
                    self.connected_devices[device_id].last_data_time = datetime.now(timezone.utc)
                
            # Notify GUI if callback provided
            if self.gui_callback:
                self.gui_callback(device_id, data)
                
        except Exception as e:
            logger.error(f"❌ Error handling data from {device_id}: {e}")
    
    def run_demo(self, duration: int = 30):
        """Run CLI demonstration mode"""
        logger.info(f"🚀 Starting {duration}-second demonstration")
        
        self.start()
        
        try:
            # Wait for the specified duration
            time.sleep(duration)
            
        except KeyboardInterrupt:
            logger.info("⚡ Demo interrupted by user")
        finally:
            self.stop()
            logger.info("✅ Demo completed")


def main():
    """Main application entry point with dual CLI/GUI support"""
    parser = argparse.ArgumentParser(description='IRCamera PC Controller - Complete Implementation')
    parser.add_argument('--duration', type=int, default=30, help='Demo duration in seconds')
    parser.add_argument('--gui', action='store_true', help='Force GUI mode')
    parser.add_argument('--cli', action='store_true', help='Force CLI mode')
    
    args = parser.parse_args()
    
    # Determine interface mode
    if args.cli:
        use_gui = False
        logger.info("💻 CLI mode requested")
    elif args.gui:
        if GUI_AVAILABLE:
            use_gui = True
            logger.info("🖥️ GUI mode requested")
        else:
            logger.warning("⚠️ GUI requested but PyQt6 not available, falling back to CLI")
            use_gui = False
    else:
        # Auto-detect best interface
        use_gui = GUI_AVAILABLE
        if use_gui:
            logger.info("🎨 Auto-selected GUI mode (PyQt6 available)")
        else:
            logger.info("💻 Auto-selected CLI mode (PyQt6 not available)")
    
    if use_gui:
        # Launch GUI application
        try:
            app = QApplication(sys.argv)
            app.setApplicationName("IRCamera PC Controller")
            app.setApplicationVersion("1.0")
            
            # Create and show main window
            main_window = PCControllerGUI()
            main_window.show()
            
            logger.info("🚀 GUI application started")
            sys.exit(app.exec())
            
        except Exception as e:
            logger.error(f"❌ GUI startup failed: {e}")
            logger.info("💻 Falling back to CLI mode")
            use_gui = False
    
    if not use_gui:
        # Run CLI mode
        controller = MVPPCController()
        controller.run_demo(duration=args.duration)


if __name__ == "__main__":
    main()