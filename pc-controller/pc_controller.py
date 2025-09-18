#!/usr/bin/env python3
"""
IRCamera PC Controller - MVP Version
Focused on core functionality for multi-modal physiological sensing

Core MVP Features:
- TCP server for Android device connections
- Basic session management
- Simple data logging
- Minimal GUI or headless operation
"""

import json
import socket
import threading
import time
import sys
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

# Simple logging
import logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class SimpleDevice:
    """Simple device representation for MVP"""
    def __init__(self, device_id: str, address: str, capabilities: List[str] = None):
        self.device_id = device_id
        self.address = address
        self.capabilities = capabilities or []
        self.connected_at = datetime.now(timezone.utc)
        self.data_count = 0


class MVPTCPServer:
    """Minimal TCP server for MVP functionality"""
    
    def __init__(self, port: int = 8080):
        self.port = port
        self.running = False
        self.server_socket = None
        self.devices: Dict[str, SimpleDevice] = {}
        self.device_lock = threading.Lock()
        
    def start(self) -> bool:
        """Start the TCP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            
            self.running = True
            logger.info(f"MVP TCP server listening on port {self.port}")
            
            # Start accepting connections
            accept_thread = threading.Thread(target=self._accept_connections, daemon=True)
            accept_thread.start()
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            return False
    
    def stop(self):
        """Stop the server"""
        self.running = False
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
        logger.info("MVP TCP server stopped")
    
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
        """Handle client connection"""
        device_id = None
        buffer = ""
        
        try:
            while self.running:
                data = client_socket.recv(1024).decode('utf-8')
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
                                device_id = message.get('device_id', f'device_{address[0]}')
                                capabilities = message.get('capabilities', [])
                                
                                with self.device_lock:
                                    self.devices[device_id] = SimpleDevice(
                                        device_id, f"{address[0]}:{address[1]}", capabilities
                                    )
                                
                                logger.info(f"Device registered: {device_id} with capabilities: {capabilities}")
                                
                                # Send acknowledgment
                                response = {
                                    "type": "device_register_ack",
                                    "status": "success",
                                    "device_id": device_id
                                }
                                self._send_message(client_socket, response)
                            
                            # Handle data
                            elif device_id and message.get('type') in ['gsr_data', 'heartbeat']:
                                with self.device_lock:
                                    if device_id in self.devices:
                                        self.devices[device_id].data_count += 1
                                
                                # Log GSR data
                                if message.get('type') == 'gsr_data':
                                    gsr_value = message.get('gsr_microsiemens', 0)
                                    logger.info(f"GSR data from {device_id}: {gsr_value:.2f}µS")
                                
                        except json.JSONDecodeError:
                            logger.warning(f"Invalid JSON from {address}")
        
        except Exception as e:
            logger.error(f"Client handler error: {e}")
        
        finally:
            # Clean up
            if device_id:
                with self.device_lock:
                    if device_id in self.devices:
                        del self.devices[device_id]
                        logger.info(f"Device {device_id} disconnected")
            
            try:
                client_socket.close()
            except:
                pass
    
    def _send_message(self, client_socket: socket.socket, message: Dict[str, Any]) -> bool:
        """Send JSON message to client"""
        try:
            json_str = json.dumps(message) + '\n'
            client_socket.send(json_str.encode('utf-8'))
            return True
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
            return False
    
    def get_connected_devices(self) -> Dict[str, SimpleDevice]:
        """Get currently connected devices"""
        with self.device_lock:
            return dict(self.devices)


class SimpleSession:
    """Simple session management for MVP"""
    
    def __init__(self):
        self.session_id = None
        self.session_name = None
        self.start_time = None
        self.is_recording = False
        
    def create_session(self, name: str) -> str:
        """Create a new session"""
        self.session_id = f"session_{int(time.time())}"
        self.session_name = name
        logger.info(f"Session created: {name} [{self.session_id}]")
        return self.session_id
    
    def start_recording(self) -> bool:
        """Start recording session"""
        if not self.session_id:
            return False
        
        self.start_time = time.time()
        self.is_recording = True
        logger.info(f"Recording started for session: {self.session_name}")
        return True
    
    def stop_recording(self) -> bool:
        """Stop recording session"""
        if not self.is_recording:
            return False
        
        duration = time.time() - self.start_time if self.start_time else 0
        self.is_recording = False
        logger.info(f"Recording stopped for session: {self.session_name} (duration: {duration:.1f}s)")
        return True


class MVPPCController:
    """MVP PC Controller - Minimal viable implementation"""
    
    def __init__(self):
        self.tcp_server = MVPTCPServer()
        self.session = SimpleSession()
        logger.info("MVP PC Controller initialized")
    
    def start_server(self) -> bool:
        """Start the TCP server"""
        return self.tcp_server.start()
    
    def stop_server(self):
        """Stop the TCP server"""
        self.tcp_server.stop()
    
    def create_session(self, name: str = None) -> str:
        """Create a recording session"""
        if not name:
            name = f"MVP_Session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        return self.session.create_session(name)
    
    def start_recording(self) -> bool:
        """Start recording"""
        return self.session.start_recording()
    
    def stop_recording(self) -> bool:
        """Stop recording"""
        return self.session.stop_recording()
    
    def get_status(self) -> Dict[str, Any]:
        """Get controller status"""
        devices = self.tcp_server.get_connected_devices()
        return {
            "server_running": self.tcp_server.running,
            "connected_devices": len(devices),
            "device_list": list(devices.keys()),
            "session_active": self.session.session_id is not None,
            "recording": self.session.is_recording
        }
    
    def run_demo(self, duration: int = 30):
        """Run MVP demonstration"""
        logger.info(f"Starting MVP demo for {duration} seconds")
        
        if not self.start_server():
            logger.error("Failed to start server")
            return
        
        # Create session
        self.create_session("MVP Demo")
        
        start_time = time.time()
        try:
            while time.time() - start_time < duration:
                # Print status every 10 seconds
                if int(time.time() - start_time) % 10 == 0:
                    status = self.get_status()
                    logger.info(f"Status: {status}")
                
                time.sleep(1)
        
        except KeyboardInterrupt:
            logger.info("Demo interrupted")
        
        finally:
            self.stop_server()
            logger.info("MVP demo completed")


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='IRCamera PC Controller MVP')
    parser.add_argument('--duration', type=int, default=30, help='Demo duration in seconds')
    
    args = parser.parse_args()
    
    print("🚀 IRCamera PC Controller MVP")
    print("=" * 30)
    
    controller = MVPPCController()
    
    try:
        controller.run_demo(args.duration)
    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    finally:
        controller.stop_server()


if __name__ == "__main__":
    main()