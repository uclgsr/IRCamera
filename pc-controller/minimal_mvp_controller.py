#!/usr/bin/env python3
"""
Minimal MVP PC Controller
Focused on core functionality without complex dependencies
"""

import json
import socket
import threading
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)

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


class MinimalTCPServer:
    """Minimal TCP server for MVP functionality"""
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8080):
        self.host = host
        self.port = port
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.connected_devices: Dict[str, Dict[str, Any]] = {}
        self.device_lock = threading.Lock()
        
    def start(self) -> bool:
        """Start the minimal TCP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            
            self.running = True
            logger.info(f"Minimal TCP server listening on {self.host}:{self.port}")
            
            # Start accepting connections in background
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
        logger.info("Minimal TCP server stopped")
    
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
            while self.running:
                try:
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
                                    
                                    with self.device_lock:
                                        self.connected_devices[device_id] = {
                                            'address': f"{address[0]}:{address[1]}",
                                            'capabilities': message.get('capabilities', []),
                                            'socket': client_socket,
                                            'last_seen': time.time()
                                        }
                                    
                                    logger.info(f"Device registered: {device_id}")
                                    
                                    # Send acknowledgment
                                    response = {
                                        "type": "device_register_ack",
                                        "status": "success",
                                        "device_id": device_id,
                                        "timestamp": datetime.now(timezone.utc).isoformat()
                                    }
                                    self._send_message(client_socket, response)
                                
                                # Handle data
                                elif device_id and message.get('type') in ['gsr_data', 'heartbeat']:
                                    logger.debug(f"Data received from {device_id}: {message.get('type')}")
                                    
                                    # Update last seen
                                    with self.device_lock:
                                        if device_id in self.connected_devices:
                                            self.connected_devices[device_id]['last_seen'] = time.time()
                                
                            except json.JSONDecodeError:
                                logger.warning(f"Invalid JSON from {address}")
                
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
    
    def _send_message(self, client_socket: socket.socket, message: Dict[str, Any]) -> bool:
        """Send JSON message to client"""
        try:
            json_str = json.dumps(message) + '\n'
            client_socket.send(json_str.encode('utf-8'))
            return True
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
            return False
    
    def get_connected_devices(self) -> Dict[str, Any]:
        """Get currently connected devices"""
        with self.device_lock:
            return dict(self.connected_devices)


class MinimalPCController:
    """Minimal PC Controller for MVP"""
    
    def __init__(self):
        self.tcp_server = MinimalTCPServer()
        self.session_manager = None
        self.discovery_service = None
        
        # Initialize available components
        if SESSION_AVAILABLE:
            try:
                self.session_manager = SessionManager()
                logger.info("Session manager initialized")
            except Exception as e:
                logger.error(f"Failed to initialize session manager: {e}")
        
        if DISCOVERY_AVAILABLE:
            try:
                self.discovery_service = NetworkDiscoveryService()
                logger.info("Discovery service initialized")
            except Exception as e:
                logger.error(f"Failed to initialize discovery service: {e}")
    
    def start(self):
        """Start the PC Controller"""
        logger.info("Starting Minimal PC Controller...")
        
        if not self.tcp_server.start():
            logger.error("Failed to start TCP server")
            return False
        
        return True
    
    def stop(self):
        """Stop the PC Controller"""
        logger.info("Stopping Minimal PC Controller...")
        self.tcp_server.stop()
    
    def create_session(self, name: str) -> bool:
        """Create a recording session"""
        if not self.session_manager:
            logger.warning("Session manager not available")
            return False
        
        try:
            session = self.session_manager.create_session(name)
            logger.info(f"Session created: {session.name}")
            return True
        except Exception as e:
            logger.error(f"Failed to create session: {e}")
            return False
    
    def get_status(self) -> Dict[str, Any]:
        """Get controller status"""
        devices = self.tcp_server.get_connected_devices()
        
        return {
            "server_running": self.tcp_server.running,
            "connected_devices": len(devices),
            "device_list": list(devices.keys()),
            "session_manager_available": self.session_manager is not None,
            "discovery_service_available": self.discovery_service is not None
        }
    
    def run_demo(self, duration: int = 30):
        """Run a demonstration"""
        logger.info(f"Starting MVP demo for {duration} seconds...")
        
        if not self.start():
            logger.error("Failed to start controller")
            return
        
        # Create session if available
        if self.session_manager:
            self.create_session(f"MVP_Demo_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
        
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
            self.stop()
            logger.info("MVP demo completed")


def main():
    """Main entry point"""
    print("🚀 IRCamera PC Controller - Minimal MVP")
    print("=" * 40)
    
    controller = MinimalPCController()
    
    try:
        controller.run_demo(30)
    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    finally:
        controller.stop()


if __name__ == "__main__":
    main()