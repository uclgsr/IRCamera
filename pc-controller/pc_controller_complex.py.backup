#!/usr/bin/env python3
"""
IRCamera PC Controller
Main application for multi-modal physiological sensing data collection

This is the primary PC Controller application that provides:
- TCP server for Android device connections
- Session management and data recording
- Real-time data visualization
- Device discovery and management
- Native backend integration for high-performance processing
"""

import json
import socket
import threading
import time
import sys
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

# Add native backend to path
SCRIPT_DIR = Path(__file__).parent
NATIVE_BACKEND_DIR = SCRIPT_DIR / "native_backend" / "build"
if NATIVE_BACKEND_DIR.exists():
    sys.path.insert(0, str(NATIVE_BACKEND_DIR))

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)

# Optional GUI support
try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout,
        QWidget, QPushButton, QLabel, QTextEdit, QTabWidget,
        QGroupBox, QGridLayout, QStatusBar, QMessageBox
    )
    from PyQt6.QtCore import QThread, pyqtSignal, QTimer, Qt
    from PyQt6.QtGui import QFont, QAction
    GUI_AVAILABLE = True
except ImportError:
    GUI_AVAILABLE = False
    logger.info("GUI not available, running in headless mode")

# Core system imports
try:
    from src.ircamera_pc.core.session import SessionManager
    SESSION_AVAILABLE = True
except ImportError:
    SESSION_AVAILABLE = False
    logger.warning("Session management not available")

try:
    from src.ircamera_pc.network.discovery import NetworkDiscoveryService
    DISCOVERY_AVAILABLE = True
except ImportError:
    DISCOVERY_AVAILABLE = False
    logger.warning("Discovery service not available")

try:
    import native_backend
    NATIVE_BACKEND_AVAILABLE = True
    logger.info("Native backend available")
except ImportError:
    NATIVE_BACKEND_AVAILABLE = False
    logger.warning("Native backend not available")


class PCControllerTCPServer:
    """TCP server for handling Android device connections"""
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8080):
        self.host = host
        self.port = port
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.connected_devices: Dict[str, Dict[str, Any]] = {}
        self.device_lock = threading.Lock()
        self.data_callback: Optional[callable] = None
        
    def set_data_callback(self, callback: callable):
        """Set callback for data received from devices"""
        self.data_callback = callback
        
    def start(self) -> bool:
        """Start the TCP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(10)
            
            self.running = True
            logger.info(f"PC Controller TCP server listening on {self.host}:{self.port}")
            
            # Start accepting connections in background
            accept_thread = threading.Thread(target=self._accept_connections, daemon=True)
            accept_thread.start()
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start TCP server: {e}")
            return False
    
    def stop(self):
        """Stop the TCP server"""
        self.running = False
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
        
        # Close all device connections
        with self.device_lock:
            for device_info in self.connected_devices.values():
                try:
                    device_info['socket'].close()
                except:
                    pass
            self.connected_devices.clear()
        
        logger.info("PC Controller TCP server stopped")
    
    def _accept_connections(self):
        """Accept incoming connections"""
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()
                logger.info(f"New connection from {address}")
                
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
                break
    
    def _handle_client(self, client_socket: socket.socket, address: tuple):
        """Handle individual client connection"""
        device_id = None
        buffer = ""
        
        try:
            # Set socket timeout
            client_socket.settimeout(1.0)
            
            while self.running:
                try:
                    data = client_socket.recv(4096).decode('utf-8')
                    if not data:
                        break
                    
                    buffer += data
                    lines = buffer.split('\n')
                    buffer = lines[-1]
                    
                    for line in lines[:-1]:
                        if line.strip():
                            try:
                                message = json.loads(line.strip())
                                
                                # Handle device registration
                                if message.get('type') == 'device_register':
                                    device_id = self._register_device(message, client_socket, address)
                                
                                # Handle data messages
                                elif device_id and device_id in self.connected_devices:
                                    self._process_device_message(device_id, message)
                                
                            except json.JSONDecodeError:
                                logger.warning(f"Invalid JSON from {address}: {line[:100]}...")
                
                except socket.timeout:
                    continue
                except Exception as e:
                    logger.error(f"Error processing data from {address}: {e}")
                    break
        
        except Exception as e:
            logger.error(f"Client handler error for {address}: {e}")
        
        finally:
            # Clean up device
            if device_id:
                with self.device_lock:
                    if device_id in self.connected_devices:
                        del self.connected_devices[device_id]
                        logger.info(f"Device {device_id} disconnected")
            
            try:
                client_socket.close()
            except:
                pass
    
    def _register_device(self, message: Dict[str, Any], client_socket: socket.socket, address: tuple) -> Optional[str]:
        """Register a new device"""
        try:
            device_id = message.get('device_id', f'device_{address[0]}_{int(time.time())}')
            capabilities = message.get('capabilities', [])
            if isinstance(capabilities, str):
                capabilities = capabilities.split(',')
            
            with self.device_lock:
                self.connected_devices[device_id] = {
                    'address': f"{address[0]}:{address[1]}",
                    'capabilities': capabilities,
                    'socket': client_socket,
                    'last_seen': time.time(),
                    'data_count': 0
                }
            
            # Send registration acknowledgment
            response = {
                "type": "device_register_ack",
                "status": "success",
                "device_id": device_id,
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            self._send_message(client_socket, response)
            
            logger.info(f"Device registered: {device_id} from {address} with capabilities: {capabilities}")
            return device_id
            
        except Exception as e:
            logger.error(f"Error registering device from {address}: {e}")
            return None
    
    def _process_device_message(self, device_id: str, message: Dict[str, Any]):
        """Process message from registered device"""
        try:
            with self.device_lock:
                if device_id in self.connected_devices:
                    self.connected_devices[device_id]['last_seen'] = time.time()
                    self.connected_devices[device_id]['data_count'] += 1
            
            # Call data callback if set
            if self.data_callback:
                self.data_callback(device_id, message)
            
            message_type = message.get('type')
            
            # Log important message types
            if message_type == 'gsr_data':
                gsr_value = message.get('gsr_microsiemens', 0)
                logger.debug(f"GSR data from {device_id}: {gsr_value:.2f}µS")
            elif message_type == 'thermal_frame':
                logger.debug(f"Thermal frame from {device_id}")
            elif message_type == 'heartbeat':
                logger.debug(f"Heartbeat from {device_id}")
                
        except Exception as e:
            logger.error(f"Error processing message from {device_id}: {e}")
    
    def _send_message(self, client_socket: socket.socket, message: Dict[str, Any]) -> bool:
        """Send JSON message to client"""
        try:
            json_str = json.dumps(message) + '\n'
            client_socket.send(json_str.encode('utf-8'))
            return True
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
            return False
    
    def broadcast_command(self, command: Dict[str, Any]) -> int:
        """Broadcast command to all connected devices"""
        sent_count = 0
        command['timestamp'] = datetime.now(timezone.utc).isoformat()
        
        with self.device_lock:
            for device_info in self.connected_devices.values():
                if self._send_message(device_info['socket'], command):
                    sent_count += 1
        
        return sent_count
    
    def get_connected_devices(self) -> Dict[str, Any]:
        """Get currently connected devices"""
        with self.device_lock:
            return dict(self.connected_devices)


class PCController:
    """Main PC Controller application"""
    
    def __init__(self):
        self.tcp_server = PCControllerTCPServer()
        self.session_manager = None
        self.discovery_service = None
        self.native_shimmer = None
        
        # Statistics
        self.total_data_points = 0
        self.session_start_time = None
        
        # Initialize available components
        self._initialize_components()
        
        # Set up data callback
        self.tcp_server.set_data_callback(self._on_data_received)
        
        logger.info("PC Controller initialized")
    
    def _initialize_components(self):
        """Initialize available system components"""
        # Session manager
        if SESSION_AVAILABLE:
            try:
                self.session_manager = SessionManager()
                logger.info("Session manager initialized")
            except Exception as e:
                logger.error(f"Failed to initialize session manager: {e}")
        
        # Discovery service
        if DISCOVERY_AVAILABLE:
            try:
                self.discovery_service = NetworkDiscoveryService()
                logger.info("Discovery service initialized")
            except Exception as e:
                logger.error(f"Failed to initialize discovery service: {e}")
        
        # Native backend
        if NATIVE_BACKEND_AVAILABLE:
            try:
                self.native_shimmer = native_backend.NativeShimmer()
                logger.info("Native Shimmer backend initialized")
            except Exception as e:
                logger.error(f"Failed to initialize native backend: {e}")
    
    def _on_data_received(self, device_id: str, message: Dict[str, Any]):
        """Handle data received from devices"""
        self.total_data_points += 1
        
        # Process different types of data
        message_type = message.get('type')
        if message_type == 'gsr_data':
            self._process_gsr_data(device_id, message)
        elif message_type == 'thermal_frame':
            self._process_thermal_data(device_id, message)
    
    def _process_gsr_data(self, device_id: str, data: Dict[str, Any]):
        """Process GSR sensor data"""
        try:
            gsr_value = data.get('gsr_microsiemens', 0)
            timestamp = data.get('timestamp', datetime.now(timezone.utc).isoformat())
            
            logger.info(f"GSR data from {device_id}: {gsr_value:.2f}µS at {timestamp}")
            
            # Process with native backend if available
            if self.native_shimmer and NATIVE_BACKEND_AVAILABLE:
                # Could process through native backend for analysis
                pass
                
        except Exception as e:
            logger.error(f"Error processing GSR data: {e}")
    
    def _process_thermal_data(self, device_id: str, data: Dict[str, Any]):
        """Process thermal camera data"""
        try:
            timestamp = data.get('timestamp', datetime.now(timezone.utc).isoformat())
            logger.info(f"Thermal frame from {device_id} at {timestamp}")
            
        except Exception as e:
            logger.error(f"Error processing thermal data: {e}")
    
    def start_server(self) -> bool:
        """Start the PC Controller server"""
        return self.tcp_server.start()
    
    def stop_server(self):
        """Stop the PC Controller server"""
        self.tcp_server.stop()
    
    def create_session(self, name: Optional[str] = None) -> bool:
        """Create a new recording session"""
        if not self.session_manager:
            logger.warning("Session manager not available")
            return False
        
        try:
            if not name:
                name = f"PC_Controller_Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            
            session = self.session_manager.create_session(name)
            logger.info(f"Session created: {session.name}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to create session: {e}")
            return False
    
    def start_recording(self) -> bool:
        """Start recording session"""
        try:
            if self.session_manager:
                self.session_manager.start_session()
            
            # Send start recording command to all devices
            command = {
                "type": "start_recording",
                "session_id": "current_session",
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            sent_count = self.tcp_server.broadcast_command(command)
            
            self.session_start_time = time.time()
            logger.info(f"Recording started, command sent to {sent_count} devices")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start recording: {e}")
            return False
    
    def stop_recording(self) -> bool:
        """Stop recording session"""
        try:
            # Send stop recording command to all devices
            command = {
                "type": "stop_recording",
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            sent_count = self.tcp_server.broadcast_command(command)
            
            if self.session_manager:
                session = self.session_manager.end_session()
                logger.info(f"Session completed: {session.name}")
            
            self.session_start_time = None
            logger.info(f"Recording stopped, command sent to {sent_count} devices")
            return True
            
        except Exception as e:
            logger.error(f"Failed to stop recording: {e}")
            return False
    
    def get_status(self) -> Dict[str, Any]:
        """Get controller status"""
        devices = self.tcp_server.get_connected_devices()
        
        status = {
            "server_running": self.tcp_server.running,
            "connected_devices": len(devices),
            "device_list": list(devices.keys()),
            "total_data_points": self.total_data_points,
            "session_manager_available": self.session_manager is not None,
            "discovery_service_available": self.discovery_service is not None,
            "native_backend_available": NATIVE_BACKEND_AVAILABLE,
            "gui_available": GUI_AVAILABLE
        }
        
        if self.session_start_time:
            status["recording_duration"] = time.time() - self.session_start_time
        
        return status
    
    def run_headless_demo(self, duration: int = 30):
        """Run headless demonstration"""
        logger.info(f"Starting PC Controller headless demo for {duration} seconds...")
        
        if not self.start_server():
            logger.error("Failed to start server")
            return
        
        # Create session if available
        if self.session_manager:
            self.create_session(f"Demo_Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
        
        start_time = time.time()
        last_status_time = start_time
        
        try:
            while time.time() - start_time < duration:
                current_time = time.time()
                
                # Print status every 10 seconds
                if current_time - last_status_time >= 10:
                    status = self.get_status()
                    logger.info(f"Status: {status}")
                    last_status_time = current_time
                
                time.sleep(1)
        
        except KeyboardInterrupt:
            logger.info("Demo interrupted by user")
        
        finally:
            self.stop_server()
            logger.info("Headless demo completed")


class PCControllerGUI:
    """GUI interface for PC Controller"""
    
    def __init__(self):
        if not GUI_AVAILABLE:
            raise ImportError("GUI not available - PyQt6 not installed")
        
        # Import here to avoid issues when GUI not available
        from PyQt6.QtWidgets import QMainWindow
        
        # Create the main window
        self.main_window = QMainWindow()
        self.controller = PCController()
        self.main_window.setWindowTitle("IRCamera PC Controller")
        self.main_window.setGeometry(100, 100, 800, 600)
        self.setup_ui()
        
        # Update timer
        from PyQt6.QtCore import QTimer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_status)
        self.update_timer.start(1000)
    
    def show(self):
        """Show the GUI window"""
        self.main_window.show()
    
    def setup_ui(self):
        """Setup the GUI interface"""
        from PyQt6.QtWidgets import QWidget, QVBoxLayout, QGroupBox, QGridLayout, QPushButton, QLabel, QTextEdit
        
        central_widget = QWidget()
        self.main_window.setCentralWidget(central_widget)
        layout = QVBoxLayout(central_widget)
        
        # Server controls
        server_group = QGroupBox("Server Control")
        server_layout = QGridLayout(server_group)
        
        self.start_btn = QPushButton("Start Server")
        self.start_btn.clicked.connect(self.start_server)
        server_layout.addWidget(self.start_btn, 0, 0)
        
        self.stop_btn = QPushButton("Stop Server")
        self.stop_btn.clicked.connect(self.stop_server)
        self.stop_btn.setEnabled(False)
        server_layout.addWidget(self.stop_btn, 0, 1)
        
        self.server_status = QLabel("Server: Stopped")
        server_layout.addWidget(self.server_status, 1, 0, 1, 2)
        
        layout.addWidget(server_group)
        
        # Device list
        device_group = QGroupBox("Connected Devices")
        device_layout = QVBoxLayout(device_group)
        
        self.device_list = QTextEdit()
        self.device_list.setMaximumHeight(150)
        self.device_list.setReadOnly(True)
        device_layout.addWidget(self.device_list)
        
        layout.addWidget(device_group)
        
        # Session controls
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
        
        self.session_status = QLabel("No active session")
        session_layout.addWidget(self.session_status, 2, 0, 1, 2)
        
        layout.addWidget(session_group)
        
        # Status display
        self.status_text = QTextEdit()
        self.status_text.setMaximumHeight(200)
        self.status_text.setReadOnly(True)
        layout.addWidget(self.status_text)
    
    def start_server(self):
        """Start the server"""
        if self.controller.start_server():
            self.server_status.setText("Server: Running")
            self.start_btn.setEnabled(False)
            self.stop_btn.setEnabled(True)
        else:
            from PyQt6.QtWidgets import QMessageBox
            QMessageBox.critical(self.main_window, "Error", "Failed to start server")
    
    def stop_server(self):
        """Stop the server"""
        self.controller.stop_server()
        self.server_status.setText("Server: Stopped")
        self.start_btn.setEnabled(True)
        self.stop_btn.setEnabled(False)
    
    def create_session(self):
        """Create a recording session"""
        if self.controller.create_session():
            self.session_status.setText("Session: Created")
            self.start_recording_btn.setEnabled(True)
    
    def start_recording(self):
        """Start recording"""
        if self.controller.start_recording():
            self.session_status.setText("Session: Recording")
            self.start_recording_btn.setEnabled(False)
            self.stop_recording_btn.setEnabled(True)
    
    def stop_recording(self):
        """Stop recording"""
        if self.controller.stop_recording():
            self.session_status.setText("Session: Completed")
            self.start_recording_btn.setEnabled(True)
            self.stop_recording_btn.setEnabled(False)
    
    def update_status(self):
        """Update the status display"""
        try:
            status = self.controller.get_status()
            devices = self.controller.tcp_server.get_connected_devices()
            
            # Update device list
            device_text = ""
            for device_id, device_info in devices.items():
                device_text += f"• {device_id}: {device_info['address']}\n"
                device_text += f"  Capabilities: {', '.join(device_info['capabilities'])}\n"
                device_text += f"  Data count: {device_info['data_count']}\n\n"
            
            if not device_text:
                device_text = "No devices connected"
            
            self.device_list.setPlainText(device_text)
            
            # Update status
            status_text = f"Connected devices: {status['connected_devices']}\n"
            status_text += f"Total data points: {status['total_data_points']}\n"
            status_text += f"Session manager: {'Available' if status['session_manager_available'] else 'Not available'}\n"
            status_text += f"Native backend: {'Available' if status['native_backend_available'] else 'Not available'}\n"
            
            if 'recording_duration' in status:
                status_text += f"Recording duration: {status['recording_duration']:.1f}s\n"
            
            self.status_text.setPlainText(status_text)
            
        except Exception as e:
            logger.error(f"Error updating GUI status: {e}")


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='IRCamera PC Controller')
    parser.add_argument('--gui', action='store_true', help='Run with GUI (requires PyQt6)')
    parser.add_argument('--headless', action='store_true', help='Run headless demo')
    parser.add_argument('--duration', type=int, default=30, help='Demo duration in seconds')
    
    args = parser.parse_args()
    
    print("🚀 IRCamera PC Controller")
    print("=" * 40)
    
    if args.gui and GUI_AVAILABLE:
        app = QApplication(sys.argv)
        window = PCControllerGUI()
        window.show()
        print("GUI mode started")
        return app.exec()
    
    elif args.headless or not GUI_AVAILABLE:
        controller = PCController()
        try:
            controller.run_headless_demo(args.duration)
        except KeyboardInterrupt:
            print("\n🛑 Interrupted by user")
        finally:
            controller.stop_server()
    
    else:
        print("Usage:")
        print("  --gui      : Run with GUI interface")
        print("  --headless : Run headless demo")
        print("  --duration : Demo duration (seconds)")


if __name__ == "__main__":
    main()