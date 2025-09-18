#!/usr/bin/env python3
"""
Integrated PC Controller Application
Combines enhanced TCP server, real-time visualization, and native backend
"""

import sys
import os
import json
import time
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

# Add native backend to path
SCRIPT_DIR = Path(__file__).parent
NATIVE_BACKEND_DIR = SCRIPT_DIR / "native_backend" / "build"
if NATIVE_BACKEND_DIR.exists():
    sys.path.insert(0, str(NATIVE_BACKEND_DIR))

try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout,
        QWidget, QPushButton, QLabel, QTextEdit, QTabWidget,
        QGroupBox, QGridLayout, QStatusBar, QMenuBar, QMenu,
        QMessageBox, QFileDialog, QProgressBar, QSplitter
    )
    from PyQt6.QtCore import QThread, pyqtSignal, QTimer, Qt
    from PyQt6.QtGui import QFont, QAction, QPixmap
    
    GUI_AVAILABLE = True
    
except ImportError as e:
    print(f"PyQt6 not available: {e}")
    GUI_AVAILABLE = False

try:
    import native_backend
    NATIVE_BACKEND_AVAILABLE = True
    print("✅ Native backend loaded successfully")
except ImportError as e:
    print(f"⚠️  Native backend not available: {e}")
    NATIVE_BACKEND_AVAILABLE = False

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)

# Import our custom modules
from enhanced_tcp_server import EnhancedTCPServer, ConnectedDevice
from realtime_visualization import MultiModalVisualizationWidget

try:
    from src.ircamera_pc.core.session import SessionManager
    from src.ircamera_pc.network.discovery import NetworkDiscoveryService
    SESSION_MANAGEMENT_AVAILABLE = True
except ImportError as e:
    print(f"⚠️  Session management not available: {e}")
    SESSION_MANAGEMENT_AVAILABLE = False


class IntegratedPCController(QMainWindow):
    """Main integrated PC Controller application window"""
    
    def __init__(self):
        super().__init__()
        
        self.setWindowTitle("IRCamera PC Controller Hub - Integrated Application")
        self.setGeometry(100, 100, 1400, 900)
        
        # Core components
        self.tcp_server: Optional[EnhancedTCPServer] = None
        self.session_manager: Optional[Any] = None
        self.discovery_service: Optional[Any] = None
        self.native_shimmer: Optional[Any] = None
        
        # Connected devices
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        
        # Statistics
        self.total_data_points = 0
        self.session_start_time = None
        
        # Setup UI
        self.setup_ui()
        self.setup_menu()
        self.setup_status_bar()
        
        # Initialize components
        self.initialize_components()
        
        # Update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_status)
        self.update_timer.start(1000)  # Update every second
        
        logger.info("✅ Integrated PC Controller initialized")
    
    def setup_ui(self):
        """Setup the main user interface"""
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Main layout
        main_layout = QHBoxLayout(central_widget)
        
        # Create splitter for resizable panes
        splitter = QSplitter(Qt.Orientation.Horizontal)
        main_layout.addWidget(splitter)
        
        # Left panel - Controls and status
        left_panel = self.create_control_panel()
        splitter.addWidget(left_panel)
        
        # Right panel - Visualization
        self.visualization_widget = MultiModalVisualizationWidget()
        splitter.addWidget(self.visualization_widget)
        
        # Set initial sizes (30% left, 70% right)
        splitter.setSizes([400, 900])
    
    def create_control_panel(self) -> QWidget:
        """Create the left control panel"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Server controls
        server_group = QGroupBox("Server Control")
        server_layout = QGridLayout(server_group)
        
        self.start_server_btn = QPushButton("Start TCP Server")
        self.start_server_btn.clicked.connect(self.start_tcp_server)
        server_layout.addWidget(self.start_server_btn, 0, 0)
        
        self.stop_server_btn = QPushButton("Stop TCP Server")
        self.stop_server_btn.clicked.connect(self.stop_tcp_server)
        self.stop_server_btn.setEnabled(False)
        server_layout.addWidget(self.stop_server_btn, 0, 1)
        
        self.server_status_label = QLabel("Server: Stopped")
        server_layout.addWidget(self.server_status_label, 1, 0, 1, 2)
        
        layout.addWidget(server_group)
        
        # Device management
        device_group = QGroupBox("Connected Devices")
        device_layout = QVBoxLayout(device_group)
        
        self.device_list = QTextEdit()
        self.device_list.setMaximumHeight(150)
        self.device_list.setReadOnly(True)
        device_layout.addWidget(self.device_list)
        
        self.refresh_devices_btn = QPushButton("Refresh Device List")
        self.refresh_devices_btn.clicked.connect(self.refresh_device_list)
        device_layout.addWidget(self.refresh_devices_btn)
        
        layout.addWidget(device_group)
        
        # Session management
        session_group = QGroupBox("Session Management")
        session_layout = QGridLayout(session_group)
        
        self.create_session_btn = QPushButton("Create Session")
        self.create_session_btn.clicked.connect(self.create_session)
        session_layout.addWidget(self.create_session_btn, 0, 0)
        
        self.start_recording_btn = QPushButton("Start Recording")
        self.start_recording_btn.clicked.connect(self.start_recording)
        self.start_recording_btn.setEnabled(False)
        session_layout.addWidget(self.start_recording_btn, 0, 1)
        
        self.stop_recording_btn = QPushButton("Stop Recording")
        self.stop_recording_btn.clicked.connect(self.stop_recording)
        self.stop_recording_btn.setEnabled(False)
        session_layout.addWidget(self.stop_recording_btn, 1, 0)
        
        self.session_status_label = QLabel("No active session")
        session_layout.addWidget(self.session_status_label, 2, 0, 1, 2)
        
        layout.addWidget(session_group)
        
        # Native backend controls
        if NATIVE_BACKEND_AVAILABLE:
            native_group = QGroupBox("Native GSR Sensor")
            native_layout = QGridLayout(native_group)
            
            self.detect_shimmer_btn = QPushButton("Detect Shimmer")
            self.detect_shimmer_btn.clicked.connect(self.detect_shimmer)
            native_layout.addWidget(self.detect_shimmer_btn, 0, 0)
            
            self.connect_shimmer_btn = QPushButton("Connect Shimmer")
            self.connect_shimmer_btn.clicked.connect(self.connect_shimmer)
            self.connect_shimmer_btn.setEnabled(False)
            native_layout.addWidget(self.connect_shimmer_btn, 0, 1)
            
            self.shimmer_status_label = QLabel("Shimmer: Disconnected")
            native_layout.addWidget(self.shimmer_status_label, 1, 0, 1, 2)
            
            layout.addWidget(native_group)
        
        # System statistics
        stats_group = QGroupBox("System Statistics")
        stats_layout = QVBoxLayout(stats_group)
        
        self.stats_text = QTextEdit()
        self.stats_text.setMaximumHeight(120)
        self.stats_text.setReadOnly(True)
        stats_layout.addWidget(self.stats_text)
        
        layout.addWidget(stats_group)
        
        # Control buttons
        control_layout = QHBoxLayout()
        
        self.clear_data_btn = QPushButton("Clear All Data")
        self.clear_data_btn.clicked.connect(self.clear_all_data)
        control_layout.addWidget(self.clear_data_btn)
        
        self.export_data_btn = QPushButton("Export Data")
        self.export_data_btn.clicked.connect(self.export_data)
        control_layout.addWidget(self.export_data_btn)
        
        layout.addLayout(control_layout)
        
        layout.addStretch()
        return panel
    
    def setup_menu(self):
        """Setup the menu bar"""
        menubar = self.menuBar()
        
        # File menu
        file_menu = menubar.addMenu('File')
        
        export_action = QAction('Export Session Data...', self)
        export_action.triggered.connect(self.export_data)
        file_menu.addAction(export_action)
        
        file_menu.addSeparator()
        
        exit_action = QAction('Exit', self)
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # Server menu
        server_menu = menubar.addMenu('Server')
        
        start_server_action = QAction('Start TCP Server', self)
        start_server_action.triggered.connect(self.start_tcp_server)
        server_menu.addAction(start_server_action)
        
        stop_server_action = QAction('Stop TCP Server', self)
        stop_server_action.triggered.connect(self.stop_tcp_server)
        server_menu.addAction(stop_server_action)
        
        # Help menu
        help_menu = menubar.addMenu('Help')
        
        about_action = QAction('About', self)
        about_action.triggered.connect(self.show_about)
        help_menu.addAction(about_action)
    
    def setup_status_bar(self):
        """Setup the status bar"""
        self.status_bar = self.statusBar()
        self.status_bar.showMessage("Ready - PC Controller Hub initialized")
        
        # Progress bar for long operations
        self.progress_bar = QProgressBar()
        self.progress_bar.setVisible(False)
        self.status_bar.addPermanentWidget(self.progress_bar)
    
    def initialize_components(self):
        """Initialize core components"""
        try:
            # Initialize session manager
            if SESSION_MANAGEMENT_AVAILABLE:
                self.session_manager = SessionManager()
                logger.info("✅ Session manager initialized")
            
            # Initialize discovery service
            if SESSION_MANAGEMENT_AVAILABLE:
                self.discovery_service = NetworkDiscoveryService()
                logger.info("✅ Network discovery service initialized")
            
            # Initialize native backend
            if NATIVE_BACKEND_AVAILABLE:
                # Create Shimmer instance
                self.native_shimmer = native_backend.NativeShimmer()
                logger.info("✅ Native Shimmer backend initialized")
            
        except Exception as e:
            logger.error(f"Error initializing components: {e}")
            QMessageBox.warning(self, "Initialization Error", f"Some components failed to initialize:\n{e}")
    
    def start_tcp_server(self):
        """Start the enhanced TCP server"""
        try:
            if self.tcp_server and self.tcp_server.running:
                return
            
            self.tcp_server = EnhancedTCPServer(port=8080, use_tls=False)
            
            # Setup callbacks
            self.tcp_server.on_device_connected = self.on_device_connected
            self.tcp_server.on_device_disconnected = self.on_device_disconnected
            self.tcp_server.on_data_received = self.on_data_received
            self.tcp_server.on_error = self.on_server_error
            
            if self.tcp_server.start():
                self.server_status_label.setText("Server: Running on port 8080")
                self.start_server_btn.setEnabled(False)
                self.stop_server_btn.setEnabled(True)
                self.status_bar.showMessage("TCP server started successfully")
                logger.info("✅ TCP server started")
            else:
                QMessageBox.critical(self, "Server Error", "Failed to start TCP server")
                
        except Exception as e:
            logger.error(f"Error starting TCP server: {e}")
            QMessageBox.critical(self, "Server Error", f"Failed to start server:\n{e}")
    
    def stop_tcp_server(self):
        """Stop the TCP server"""
        try:
            if self.tcp_server:
                self.tcp_server.stop()
                self.tcp_server = None
                
            self.server_status_label.setText("Server: Stopped")
            self.start_server_btn.setEnabled(True)
            self.stop_server_btn.setEnabled(False)
            self.connected_devices.clear()
            self.refresh_device_list()
            self.status_bar.showMessage("TCP server stopped")
            logger.info("✅ TCP server stopped")
            
        except Exception as e:
            logger.error(f"Error stopping TCP server: {e}")
    
    def on_device_connected(self, device_id: str, device: ConnectedDevice):
        """Handle device connection"""
        self.connected_devices[device_id] = device
        self.refresh_device_list()
        self.status_bar.showMessage(f"Device connected: {device_id}")
        logger.info(f"✅ Device connected: {device_id} from {device.address}")
    
    def on_device_disconnected(self, device_id: str):
        """Handle device disconnection"""
        if device_id in self.connected_devices:
            del self.connected_devices[device_id]
        self.refresh_device_list()
        self.status_bar.showMessage(f"Device disconnected: {device_id}")
        logger.info(f"❌ Device disconnected: {device_id}")
    
    def on_data_received(self, device_id: str, data: Dict[str, Any]):
        """Handle incoming data from devices"""
        try:
            # Process data through visualization widget
            self.visualization_widget.process_device_data(device_id, data)
            self.total_data_points += 1
            
            # Log interesting data types
            data_type = data.get('type', 'unknown')
            if data_type in ['gsr_data', 'thermal_frame']:
                logger.debug(f"📊 {data_type} from {device_id}")
                
        except Exception as e:
            logger.error(f"Error processing data from {device_id}: {e}")
    
    def on_server_error(self, context: str, error: Exception):
        """Handle server errors"""
        logger.error(f"Server error in {context}: {error}")
        self.status_bar.showMessage(f"Server error: {error}")
    
    def refresh_device_list(self):
        """Refresh the connected device list display"""
        device_text = ""
        if self.connected_devices:
            for device_id, device in self.connected_devices.items():
                status = "🔴 Recording" if device.is_recording else "🟢 Connected"
                device_text += f"{status} {device_id}\n"
                device_text += f"   Address: {device.address}\n"
                device_text += f"   Capabilities: {', '.join(device.capabilities)}\n"
                device_text += f"   Data count: {device.data_count}\n\n"
        else:
            device_text = "No devices connected"
        
        self.device_list.setPlainText(device_text)
    
    def create_session(self):
        """Create a new recording session"""
        try:
            if not SESSION_MANAGEMENT_AVAILABLE or not self.session_manager:
                QMessageBox.warning(self, "Session Error", "Session management not available")
                return
            
            session_name = f"PC_Controller_Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            session = self.session_manager.create_session(session_name)
            
            self.session_status_label.setText(f"Session: {session.name[:30]}...")
            self.start_recording_btn.setEnabled(True)
            self.status_bar.showMessage(f"Session created: {session.name}")
            logger.info(f"✅ Session created: {session.name}")
            
        except Exception as e:
            logger.error(f"Error creating session: {e}")
            QMessageBox.critical(self, "Session Error", f"Failed to create session:\n{e}")
    
    def start_recording(self):
        """Start recording session"""
        try:
            if SESSION_MANAGEMENT_AVAILABLE and self.session_manager:
                self.session_manager.start_session()
            
            # Send start recording command to all devices
            if self.tcp_server:
                command = {
                    "type": "start_recording",
                    "session_id": "current_session",
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                sent_count = self.tcp_server.broadcast_command(command)
                logger.info(f"📡 Start recording sent to {sent_count} devices")
            
            self.session_start_time = time.time()
            self.start_recording_btn.setEnabled(False)
            self.stop_recording_btn.setEnabled(True)
            self.session_status_label.setText("Session: Recording...")
            self.status_bar.showMessage("Recording started")
            
        except Exception as e:
            logger.error(f"Error starting recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to start recording:\n{e}")
    
    def stop_recording(self):
        """Stop recording session"""
        try:
            # Send stop recording command to all devices
            if self.tcp_server:
                command = {
                    "type": "stop_recording",
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                sent_count = self.tcp_server.broadcast_command(command)
                logger.info(f"📡 Stop recording sent to {sent_count} devices")
            
            if SESSION_MANAGEMENT_AVAILABLE and self.session_manager:
                session = self.session_manager.end_session()
                self.session_status_label.setText(f"Session completed: {session.name[:30]}...")
            else:
                self.session_status_label.setText("Recording stopped")
            
            self.start_recording_btn.setEnabled(True)
            self.stop_recording_btn.setEnabled(False)
            self.session_start_time = None
            self.status_bar.showMessage("Recording stopped")
            
        except Exception as e:
            logger.error(f"Error stopping recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to stop recording:\n{e}")
    
    def detect_shimmer(self):
        """Detect connected Shimmer device"""
        if not NATIVE_BACKEND_AVAILABLE:
            QMessageBox.warning(self, "Native Backend", "Native backend not available")
            return
        
        try:
            # Get available ports
            ports = native_backend.get_available_serial_ports()
            detected_port = native_backend.detect_shimmer_device()
            
            if detected_port:
                self.shimmer_status_label.setText(f"Shimmer detected: {detected_port}")
                self.connect_shimmer_btn.setEnabled(True)
                self.status_bar.showMessage(f"Shimmer detected on {detected_port}")
                logger.info(f"✅ Shimmer detected on {detected_port}")
            else:
                self.shimmer_status_label.setText("No Shimmer detected")
                QMessageBox.information(self, "Shimmer Detection", 
                                      f"No Shimmer device detected.\nAvailable ports: {ports}")
                
        except Exception as e:
            logger.error(f"Error detecting Shimmer: {e}")
            QMessageBox.critical(self, "Detection Error", f"Failed to detect Shimmer:\n{e}")
    
    def connect_shimmer(self):
        """Connect to Shimmer device"""
        if not NATIVE_BACKEND_AVAILABLE or not self.native_shimmer:
            return
        
        try:
            detected_port = native_backend.detect_shimmer_device()
            if not detected_port:
                QMessageBox.warning(self, "Connection Error", "No Shimmer device detected")
                return
            
            if self.native_shimmer.connect(detected_port):
                self.shimmer_status_label.setText("Shimmer: Connected")
                self.status_bar.showMessage("Shimmer connected successfully")
                logger.info("✅ Shimmer connected successfully")
                
                # Start streaming if desired
                if self.native_shimmer.start_streaming():
                    logger.info("✅ Shimmer streaming started")
                    
            else:
                error = self.native_shimmer.get_last_error()
                QMessageBox.critical(self, "Connection Error", 
                                   f"Failed to connect to Shimmer:\n{error}")
                
        except Exception as e:
            logger.error(f"Error connecting to Shimmer: {e}")
            QMessageBox.critical(self, "Connection Error", f"Failed to connect to Shimmer:\n{e}")
    
    def clear_all_data(self):
        """Clear all visualization data"""
        try:
            self.visualization_widget.clear_all_data()
            self.total_data_points = 0
            self.status_bar.showMessage("All data cleared")
            logger.info("✅ All visualization data cleared")
            
        except Exception as e:
            logger.error(f"Error clearing data: {e}")
    
    def export_data(self):
        """Export session data"""
        try:
            # Get export filename
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            default_name = f"pc_controller_session_{timestamp}"
            
            filename, _ = QFileDialog.getSaveFileName(
                self, "Export Session Data", default_name, "CSV Files (*.csv);;All Files (*)"
            )
            
            if filename:
                # Remove extension if provided, we'll add our own
                base_filename = str(Path(filename).with_suffix(''))
                
                # Export visualization data
                self.visualization_widget.export_all_data(base_filename)
                
                # Export session metadata if available
                if SESSION_MANAGEMENT_AVAILABLE and self.session_manager:
                    current_session = self.session_manager.get_current_session()
                    if current_session:
                        metadata_file = f"{base_filename}_metadata.json"
                        with open(metadata_file, 'w') as f:
                            json.dump(current_session.__dict__, f, indent=2, default=str)
                
                self.status_bar.showMessage(f"Data exported to {base_filename}_*.csv")
                QMessageBox.information(self, "Export Complete", 
                                      f"Session data exported to:\n{base_filename}_*.csv")
                
        except Exception as e:
            logger.error(f"Error exporting data: {e}")
            QMessageBox.critical(self, "Export Error", f"Failed to export data:\n{e}")
    
    def update_status(self):
        """Update system status and statistics"""
        try:
            stats_text = "🖥️ PC Controller Hub Statistics\n"
            stats_text += "="*40 + "\n"
            
            # Server statistics
            if self.tcp_server:
                server_stats = self.tcp_server.get_statistics()
                uptime = server_stats.get('uptime_seconds', 0)
                stats_text += f"🌐 Server uptime: {uptime:.1f}s\n"
                stats_text += f"🔗 Total connections: {server_stats.get('total_connections', 0)}\n"
                stats_text += f"📱 Current devices: {server_stats.get('current_devices', 0)}\n"
                stats_text += f"🔴 Recording devices: {server_stats.get('recording_devices', 0)}\n"
                stats_text += f"📊 Total messages: {server_stats.get('total_messages', 0)}\n"
            else:
                stats_text += "🌐 Server: Stopped\n"
            
            stats_text += "\n"
            
            # Session statistics
            stats_text += f"📈 Total data points: {self.total_data_points}\n"
            if self.session_start_time:
                duration = time.time() - self.session_start_time
                stats_text += f"⏱️ Recording time: {duration:.1f}s\n"
            
            # Native backend statistics
            if NATIVE_BACKEND_AVAILABLE and self.native_shimmer:
                if self.native_shimmer.is_connected():
                    stats_text += f"🔬 Shimmer: Connected\n"
                    if self.native_shimmer.is_streaming():
                        stats_text += f"📡 Shimmer: Streaming at {self.native_shimmer.get_sampling_rate()}Hz\n"
                else:
                    stats_text += f"🔬 Shimmer: Disconnected\n"
            
            self.stats_text.setPlainText(stats_text)
            
        except Exception as e:
            logger.error(f"Error updating status: {e}")
    
    def show_about(self):
        """Show about dialog"""
        about_text = """
        IRCamera PC Controller Hub
        
        Integrated Desktop Application for Multi-Modal Physiological Sensing
        
        Features:
        • Enhanced TCP server with TLS support
        • Real-time data visualization
        • Native C++ backend for high-performance processing
        • Session management and data export
        • Device discovery and management
        
        Components Status:
        """
        
        about_text += f"• GUI: {'✅ Available' if GUI_AVAILABLE else '❌ Not available'}\n"
        about_text += f"• Native Backend: {'✅ Available' if NATIVE_BACKEND_AVAILABLE else '❌ Not available'}\n"
        about_text += f"• Session Management: {'✅ Available' if SESSION_MANAGEMENT_AVAILABLE else '❌ Not available'}\n"
        
        QMessageBox.about(self, "About PC Controller Hub", about_text)
    
    def closeEvent(self, event):
        """Handle application close event"""
        try:
            # Stop server if running
            if self.tcp_server and self.tcp_server.running:
                self.tcp_server.stop()
                
            # Disconnect native backend
            if NATIVE_BACKEND_AVAILABLE and self.native_shimmer:
                if self.native_shimmer.is_connected():
                    self.native_shimmer.disconnect()
                    
            logger.info("✅ PC Controller Hub closed gracefully")
            event.accept()
            
        except Exception as e:
            logger.error(f"Error during shutdown: {e}")
            event.accept()


def main():
    """Main application entry point"""
    if not GUI_AVAILABLE:
        print("❌ PyQt6 not available - cannot run GUI application")
        return
    
    app = QApplication(sys.argv)
    
    # Set application properties
    app.setApplicationName("IRCamera PC Controller Hub")
    app.setApplicationVersion("1.0.0")
    app.setOrganizationName("IRCamera Team")
    
    # Apply dark theme
    app.setStyle('Fusion')
    
    # Create and show main window
    window = IntegratedPCController()
    window.show()
    
    print("🚀 IRCamera PC Controller Hub started!")
    print("📱 Ready to accept Android device connections")
    print("🔬 Native GSR processing available" if NATIVE_BACKEND_AVAILABLE else "⚠️  Native backend not available")
    
    # Run application
    return app.exec()


if __name__ == "__main__":
    sys.exit(main())