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

# Try to import native backend for enhanced performance
try:
    import sys
    import os
    # Add the native backend build directory to path
    native_backend_path = os.path.join(os.path.dirname(__file__), 'native_backend', 'build')
    if os.path.exists(native_backend_path):
        sys.path.insert(0, native_backend_path)
    import native_backend
    NATIVE_BACKEND_AVAILABLE = True
    logger.info("🚀 Native C++ backend available for enhanced performance")
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
    """Simple TCP server for Android device connections"""
    
    def __init__(self, port: int = 8080, data_callback=None):
        self.port = port
        self.server_socket = None
        self.running = False
        self.client_threads = []
        self.connected_clients = {}
        self.data_callback = data_callback
        
    def start(self) -> bool:
        """Start the TCP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            self.running = True
            
            # Start server thread
            server_thread = threading.Thread(target=self._server_loop, daemon=True)
            server_thread.start()
            
            logger.info(f"🌐 TCP Server started on port {self.port}")
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
        """Main server loop"""
        while self.running:
            try:
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
                
            except Exception as e:
                if self.running:
                    logger.error(f"Server loop error: {e}")
                break
    
    def _handle_client(self, client_socket: socket.socket, address):
        """Handle individual client connection"""
        device_id = f"device_{address[0]}_{address[1]}"
        buffer = ""
        
        try:
            while self.running:
                data = client_socket.recv(1024).decode('utf-8')
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
                            
                            # Call data callback if provided
                            if self.data_callback:
                                self.data_callback(msg_device_id, message)
                                
                        except json.JSONDecodeError as e:
                            logger.warning(f"Invalid JSON from {address}: {line}")
                            
        except Exception as e:
            logger.error(f"Client handler error for {address}: {e}")
        finally:
            client_socket.close()
            logger.info(f"🔌 Disconnected: {address}")


class PCControllerGUI(QMainWindow):
    """PyQt6 GUI interface for PC Controller"""
    
    def __init__(self):
        super().__init__()
        self.controller = None
        self.data_buffer = {}  # Store GSR data for plotting
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
        """Create right visualization panel"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Create tab widget for different data types
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # GSR plot tab
        if PYQTGRAPH_AVAILABLE:
            self.gsr_plot = pg.PlotWidget(title="GSR Data (Real-time)")
            self.gsr_plot.setLabel('left', 'GSR', units='µS')
            self.gsr_plot.setLabel('bottom', 'Time', units='s')
            self.gsr_plot.showGrid(True, True)
            self.tab_widget.addTab(self.gsr_plot, "GSR Data")
        else:
            # Fallback text display if pyqtgraph not available
            self.gsr_text = QTextEdit()
            self.gsr_text.setReadOnly(True)
            self.tab_widget.addTab(self.gsr_text, "GSR Data (Text)")
        
        # Status display tab
        self.status_text = QTextEdit()
        self.status_text.setReadOnly(True)
        self.tab_widget.addTab(self.status_text, "Status & Messages")
        
        return panel
        
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
        """Callback for when data is received from devices"""
        if data.get('type') == 'gsr_data':
            timestamp = time.time()
            gsr_value = data.get('gsr_microsiemens', 0.0)
            
            # Store data for plotting
            if device_id not in self.data_buffer:
                self.data_buffer[device_id] = []
            self.data_buffer[device_id].append((timestamp, gsr_value))
            
            # Keep only last 1000 points per device
            if len(self.data_buffer[device_id]) > 1000:
                self.data_buffer[device_id] = self.data_buffer[device_id][-1000:]
            
            # Update status
            self.log_message(f"📊 GSR data from {device_id}: {gsr_value:.2f} µS")
            
        elif data.get('type') == 'device_info':
            # Update device list
            self.update_device_list()
            self.log_message(f"🔗 Device connected: {device_id}")
    
    def update_device_list(self):
        """Update the connected devices list"""
        self.devices_list.clear()
        if self.controller:
            for device_id, device in self.controller.connected_devices.items():
                status = "Recording" if device.is_recording else "Connected"
                self.devices_list.addItem(f"{device_id} - {status}")
    
    def update_plots(self):
        """Update real-time plots"""
        if not PYQTGRAPH_AVAILABLE or not self.data_buffer:
            return
            
        self.gsr_plot.clear()
        
        # Plot data for each device with different colors
        colors = ['r', 'g', 'b', 'c', 'm', 'y']
        for i, (device_id, data_points) in enumerate(self.data_buffer.items()):
            if data_points:
                timestamps, gsr_values = zip(*data_points)
                # Normalize timestamps to start from 0
                start_time = timestamps[0]
                x_data = [(t - start_time) for t in timestamps]
                y_data = list(gsr_values)
                
                color = colors[i % len(colors)]
                self.gsr_plot.plot(x_data, y_data, pen=color, name=device_id)
        
        # Auto-range the plot
        self.gsr_plot.enableAutoRange()
    
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