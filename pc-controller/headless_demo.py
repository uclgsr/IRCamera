#!/usr/bin/env python3
"""
Headless PC Controller Demo
Demonstrates core functionality without GUI for testing
"""

import sys
import json
import time
import threading
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

# Add native backend to path
SCRIPT_DIR = Path(__file__).parent
NATIVE_BACKEND_DIR = SCRIPT_DIR / "native_backend" / "build"
if NATIVE_BACKEND_DIR.exists():
    sys.path.insert(0, str(NATIVE_BACKEND_DIR))

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

try:
    from src.ircamera_pc.core.session import SessionManager
    SESSION_MANAGEMENT_AVAILABLE = True
    print("✅ Session management available")
except ImportError as e:
    print(f"⚠️  Session management not available: {e}")
    SESSION_MANAGEMENT_AVAILABLE = False


class HeadlessPCController:
    """Headless PC Controller for testing without GUI"""
    
    def __init__(self):
        self.tcp_server: Optional[EnhancedTCPServer] = None
        self.session_manager: Optional[Any] = None
        self.native_shimmer: Optional[Any] = None
        
        # Connected devices
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        
        # Statistics
        self.total_data_points = 0
        self.session_start_time = None
        self.demo_running = True
        
        # Initialize components
        self.initialize_components()
        
        logger.info("✅ Headless PC Controller initialized")
    
    def initialize_components(self):
        """Initialize core components"""
        try:
            # Initialize session manager
            if SESSION_MANAGEMENT_AVAILABLE:
                self.session_manager = SessionManager()
                logger.info("✅ Session manager initialized")
            
            # Initialize native backend
            if NATIVE_BACKEND_AVAILABLE:
                self.native_shimmer = native_backend.NativeShimmer()
                logger.info("✅ Native Shimmer backend initialized")
                
                # Test native backend functions
                ports = native_backend.get_available_serial_ports()
                logger.info(f"📋 Available serial ports: {ports}")
                
                shimmer_port = native_backend.detect_shimmer_device()
                if shimmer_port:
                    logger.info(f"🔬 Shimmer detected on: {shimmer_port}")
                else:
                    logger.info("🔬 No Shimmer device detected")
            
        except Exception as e:
            logger.error(f"Error initializing components: {e}")
    
    def start_tcp_server(self) -> bool:
        """Start the enhanced TCP server"""
        try:
            if self.tcp_server and self.tcp_server.running:
                return True
            
            self.tcp_server = EnhancedTCPServer(port=8080, use_tls=False)
            
            # Setup callbacks
            self.tcp_server.on_device_connected = self.on_device_connected
            self.tcp_server.on_device_disconnected = self.on_device_disconnected
            self.tcp_server.on_data_received = self.on_data_received
            self.tcp_server.on_error = self.on_server_error
            
            if self.tcp_server.start():
                logger.info("✅ TCP server started on port 8080")
                return True
            else:
                logger.error("❌ Failed to start TCP server")
                return False
                
        except Exception as e:
            logger.error(f"Error starting TCP server: {e}")
            return False
    
    def stop_tcp_server(self):
        """Stop the TCP server"""
        try:
            if self.tcp_server:
                self.tcp_server.stop()
                self.tcp_server = None
                
            self.connected_devices.clear()
            logger.info("✅ TCP server stopped")
            
        except Exception as e:
            logger.error(f"Error stopping TCP server: {e}")
    
    def on_device_connected(self, device_id: str, device: ConnectedDevice):
        """Handle device connection"""
        self.connected_devices[device_id] = device
        logger.info(f"✅ Device connected: {device_id} from {device.address}")
        logger.info(f"   Capabilities: {device.capabilities}")
    
    def on_device_disconnected(self, device_id: str):
        """Handle device disconnection"""
        if device_id in self.connected_devices:
            del self.connected_devices[device_id]
        logger.info(f"❌ Device disconnected: {device_id}")
    
    def on_data_received(self, device_id: str, data: Dict[str, Any]):
        """Handle incoming data from devices"""
        try:
            self.total_data_points += 1
            
            # Log interesting data types
            data_type = data.get('type', 'unknown')
            if data_type in ['gsr_data', 'thermal_frame', 'heartbeat']:
                if data_type == 'heartbeat':
                    logger.debug(f"💓 Heartbeat from {device_id}")
                elif data_type == 'gsr_data':
                    gsr_value = data.get('gsr_microsiemens', 0)
                    logger.info(f"📊 GSR data from {device_id}: {gsr_value:.2f}µS")
                elif data_type == 'thermal_frame':
                    logger.info(f"🌡️  Thermal frame from {device_id}")
                    
        except Exception as e:
            logger.error(f"Error processing data from {device_id}: {e}")
    
    def on_server_error(self, context: str, error: Exception):
        """Handle server errors"""
        logger.error(f"Server error in {context}: {error}")
    
    def create_session(self) -> bool:
        """Create a new recording session"""
        try:
            if not SESSION_MANAGEMENT_AVAILABLE or not self.session_manager:
                logger.warning("Session management not available")
                return False
            
            session_name = f"Headless_Demo_Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            session = self.session_manager.create_session(session_name)
            
            logger.info(f"✅ Session created: {session.name}")
            logger.info(f"   Session ID: {session.session_id}")
            logger.info(f"   Session state: {session.state}")
            return True
            
        except Exception as e:
            logger.error(f"Error creating session: {e}")
            return False
    
    def start_recording(self) -> bool:
        """Start recording session"""
        try:
            if SESSION_MANAGEMENT_AVAILABLE and self.session_manager:
                self.session_manager.start_session()
            
            # Send start recording command to all devices
            if self.tcp_server:
                command = {
                    "type": "start_recording",
                    "session_id": "headless_demo_session",
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                sent_count = self.tcp_server.broadcast_command(command)
                logger.info(f"📡 Start recording sent to {sent_count} devices")
            
            self.session_start_time = time.time()
            logger.info("✅ Recording started")
            return True
            
        except Exception as e:
            logger.error(f"Error starting recording: {e}")
            return False
    
    def stop_recording(self) -> bool:
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
                logger.info(f"✅ Session completed: {session.name}")
            else:
                logger.info("✅ Recording stopped")
            
            self.session_start_time = None
            return True
            
        except Exception as e:
            logger.error(f"Error stopping recording: {e}")
            return False
    
    def get_statistics(self) -> Dict[str, Any]:
        """Get system statistics"""
        stats = {
            "total_data_points": self.total_data_points,
            "connected_devices": len(self.connected_devices),
            "native_backend_available": NATIVE_BACKEND_AVAILABLE,
            "session_management_available": SESSION_MANAGEMENT_AVAILABLE,
        }
        
        if self.tcp_server:
            server_stats = self.tcp_server.get_statistics()
            stats.update(server_stats)
        
        if self.session_start_time:
            stats["recording_duration"] = time.time() - self.session_start_time
        
        return stats
    
    def print_status(self):
        """Print current system status"""
        print("\n" + "="*60)
        print("📊 PC CONTROLLER SYSTEM STATUS")
        print("="*60)
        
        # Basic status
        print(f"🌐 TCP Server: {'Running' if self.tcp_server and self.tcp_server.running else 'Stopped'}")
        print(f"📱 Connected devices: {len(self.connected_devices)}")
        print(f"📊 Total data points: {self.total_data_points}")
        
        # Component status
        print(f"🔬 Native backend: {'Available' if NATIVE_BACKEND_AVAILABLE else 'Not available'}")
        print(f"📋 Session management: {'Available' if SESSION_MANAGEMENT_AVAILABLE else 'Not available'}")
        
        # Device details
        if self.connected_devices:
            print("\n📱 Connected Devices:")
            for device_id, device in self.connected_devices.items():
                print(f"  • {device_id}: {device.address}")
                print(f"    Capabilities: {', '.join(device.capabilities)}")
                print(f"    Data count: {device.data_count}")
                print(f"    Recording: {'Yes' if device.is_recording else 'No'}")
        
        # Server statistics
        if self.tcp_server:
            server_stats = self.tcp_server.get_statistics()
            print(f"\n🌐 Server Statistics:")
            print(f"  • Uptime: {server_stats.get('uptime_seconds', 0):.1f}s")
            print(f"  • Total connections: {server_stats.get('total_connections', 0)}")
            print(f"  • Total messages: {server_stats.get('total_messages', 0)}")
        
        # Session info
        if SESSION_MANAGEMENT_AVAILABLE and self.session_manager:
            current_session = self.session_manager.get_current_session()
            if current_session:
                print(f"\n📋 Current Session:")
                print(f"  • Name: {current_session.name}")
                print(f"  • State: {current_session.state}")
                print(f"  • Created: {current_session.created_at}")
        
        if self.session_start_time:
            duration = time.time() - self.session_start_time
            print(f"⏱️ Recording duration: {duration:.1f}s")
        
        print("="*60)
    
    def run_demo(self, duration: int = 30):
        """Run a demonstration for specified duration"""
        logger.info(f"🚀 Starting PC Controller demo for {duration} seconds")
        
        # Start TCP server
        if not self.start_tcp_server():
            logger.error("❌ Failed to start server, exiting demo")
            return
        
        # Create a session
        if not self.create_session():
            logger.warning("⚠️ Failed to create session, continuing without session management")
        
        # Start recording
        time.sleep(2)  # Give server time to start
        self.start_recording()
        
        # Run demo loop
        start_time = time.time()
        last_status_time = start_time
        
        try:
            while time.time() - start_time < duration:
                current_time = time.time()
                
                # Print status every 10 seconds
                if current_time - last_status_time >= 10:
                    self.print_status()
                    last_status_time = current_time
                
                # Simulate some data processing
                if self.connected_devices:
                    # Devices are connected, normal operation
                    time.sleep(1)
                else:
                    # No devices connected, show waiting message
                    if int(current_time - start_time) % 10 == 0:
                        logger.info("📱 Waiting for Android device connections on port 8080...")
                    time.sleep(1)
                
        except KeyboardInterrupt:
            logger.info("🛑 Demo interrupted by user")
        
        finally:
            # Clean shutdown
            logger.info("🛑 Stopping demo...")
            self.stop_recording()
            time.sleep(1)
            self.stop_tcp_server()
            
            # Final status
            self.print_status()
            
            logger.info("✅ Demo completed successfully")


def simulate_android_client():
    """Simulate an Android client connecting to test the server"""
    import socket
    import random
    
    try:
        time.sleep(5)  # Wait for server to start
        
        # Connect to server
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.connect(('localhost', 8080))
        
        # Register device
        device_info = {
            "type": "device_register",
            "device_id": "demo_android_device",
            "device_type": "ANDROID_NODE",
            "capabilities": ["gsr", "rgb", "thermal"],
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        message = json.dumps(device_info) + '\n'
        client_socket.send(message.encode('utf-8'))
        
        # Receive registration response
        response = client_socket.recv(1024).decode('utf-8')
        print(f"📱 Client received: {response.strip()}")
        
        # Send periodic data
        for i in range(20):
            # Send GSR data
            gsr_data = {
                "type": "gsr_data",
                "gsr_microsiemens": 5.0 + random.uniform(-1.0, 1.0),
                "gsr_raw": random.randint(800, 1200),
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            
            message = json.dumps(gsr_data) + '\n'
            client_socket.send(message.encode('utf-8'))
            
            # Send heartbeat occasionally
            if i % 5 == 0:
                heartbeat = {
                    "type": "heartbeat",
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                message = json.dumps(heartbeat) + '\n'
                client_socket.send(message.encode('utf-8'))
            
            time.sleep(1)
        
        client_socket.close()
        print("📱 Simulated Android client disconnected")
        
    except Exception as e:
        print(f"❌ Simulated client error: {e}")


def main():
    """Main demonstration entry point"""
    print("🚀 IRCamera PC Controller - Headless Demo")
    print("="*50)
    
    # Create controller
    controller = HeadlessPCController()
    
    # Start simulated Android client in background
    client_thread = threading.Thread(target=simulate_android_client, daemon=True)
    client_thread.start()
    
    # Run demo for 30 seconds
    controller.run_demo(duration=30)
    
    print("✅ Headless demo completed!")
    print("\nNext steps:")
    print("- Connect real Android devices to port 8080")
    print("- Use integrated_pc_controller.py for full GUI experience")
    print("- Check native backend with real Shimmer devices")


if __name__ == "__main__":
    main()