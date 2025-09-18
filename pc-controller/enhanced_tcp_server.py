#!/usr/bin/env python3
"""
Enhanced TCP Server for PC Controller
Implements high-performance real-time data streaming with error handling
"""

import asyncio
import json
import logging
import socket
import ssl
import threading
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Callable, Any

try:
    from loguru import logger
except ImportError:
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


@dataclass
class ConnectedDevice:
    """Represents a connected Android device"""
    device_id: str
    address: str
    socket: socket.socket
    last_heartbeat: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    is_recording: bool = False
    capabilities: List[str] = field(default_factory=list)
    data_count: int = 0
    connection_time: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


class EnhancedTCPServer:
    """Enhanced TCP server with real-time data streaming and robust error handling"""
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8080, use_tls: bool = False):
        self.host = host
        self.port = port
        self.use_tls = use_tls
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.ssl_context: Optional[ssl.SSLContext] = None
        
        # Device management
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        self.device_lock = threading.Lock()
        
        # Callbacks
        self.on_device_connected: Optional[Callable[[str, ConnectedDevice], None]] = None
        self.on_device_disconnected: Optional[Callable[[str], None]] = None
        self.on_data_received: Optional[Callable[[str, Dict[str, Any]], None]] = None
        self.on_error: Optional[Callable[[str, Exception], None]] = None
        
        # Statistics
        self.total_connections = 0
        self.total_messages = 0
        self.start_time = None
        
        # Heartbeat monitoring
        self.heartbeat_timeout = 30.0  # seconds
        self.heartbeat_thread: Optional[threading.Thread] = None
        
        # TLS setup
        if use_tls:
            self._setup_tls()
    
    def _setup_tls(self):
        """Setup TLS/SSL configuration"""
        try:
            self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            
            # Look for certificates
            cert_dir = Path(__file__).parent / "certificates"
            cert_file = cert_dir / "server.crt"
            key_file = cert_dir / "server.key"
            
            if cert_file.exists() and key_file.exists():
                self.ssl_context.load_cert_chain(cert_file, key_file)
                logger.info(f"Loaded TLS certificates from {cert_dir}")
            else:
                logger.warning("TLS certificates not found, generating self-signed...")
                self._generate_self_signed_cert(cert_dir)
                if cert_file.exists() and key_file.exists():
                    self.ssl_context.load_cert_chain(cert_file, key_file)
                    
        except Exception as e:
            logger.error(f"Failed to setup TLS: {e}")
            self.ssl_context = None
            self.use_tls = False
    
    def _generate_self_signed_cert(self, cert_dir: Path):
        """Generate self-signed certificate for development"""
        try:
            import subprocess
            cert_dir.mkdir(exist_ok=True)
            
            # Generate self-signed certificate
            cmd = [
                "openssl", "req", "-x509", "-newkey", "rsa:4096",
                "-keyout", str(cert_dir / "server.key"),
                "-out", str(cert_dir / "server.crt"),
                "-days", "365", "-nodes",
                "-subj", "/C=US/ST=CA/L=SF/O=IRCamera/CN=localhost"
            ]
            
            subprocess.run(cmd, check=True, capture_output=True)
            logger.info("Generated self-signed certificate")
            
        except Exception as e:
            logger.error(f"Failed to generate certificate: {e}")
    
    def start(self) -> bool:
        """Start the enhanced TCP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            
            # Enable TCP keep-alive for better connection monitoring
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            
            # Bind and listen
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(10)  # Increased backlog
            
            protocol = "TLS" if self.use_tls else "TCP"
            logger.info(f"Enhanced {protocol} server listening on {self.host}:{self.port}")
            
            self.running = True
            self.start_time = datetime.now(timezone.utc)
            
            # Start heartbeat monitoring
            self.heartbeat_thread = threading.Thread(target=self._heartbeat_monitor, daemon=True)
            self.heartbeat_thread.start()
            
            # Start accepting connections
            accept_thread = threading.Thread(target=self._accept_connections, daemon=True)
            accept_thread.start()
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            if self.on_error:
                self.on_error("server_start", e)
            return False
    
    def stop(self):
        """Stop the server gracefully"""
        logger.info("Stopping enhanced TCP server...")
        self.running = False
        
        # Close all device connections
        with self.device_lock:
            for device in list(self.connected_devices.values()):
                self._disconnect_device(device.device_id)
        
        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except Exception as e:
                logger.error(f"Error closing server socket: {e}")
        
        logger.info("Enhanced TCP server stopped")
    
    def _accept_connections(self):
        """Accept incoming connections"""
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()
                
                # Apply TLS if enabled
                if self.use_tls and self.ssl_context:
                    try:
                        client_socket = self.ssl_context.wrap_socket(
                            client_socket, server_side=True
                        )
                    except Exception as e:
                        logger.error(f"TLS handshake failed with {address}: {e}")
                        client_socket.close()
                        continue
                
                self.total_connections += 1
                logger.info(f"New connection from {address} (total: {self.total_connections})")
                
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
                    if self.on_error:
                        self.on_error("accept_connection", e)
    
    def _handle_client(self, client_socket: socket.socket, address: tuple):
        """Handle individual client connection with enhanced error handling"""
        device_id = None
        buffer = ""
        
        try:
            # Set socket timeout for reads
            client_socket.settimeout(1.0)
            
            while self.running:
                try:
                    data = client_socket.recv(4096).decode('utf-8')
                    if not data:
                        break
                    
                    buffer += data
                    lines = buffer.split('\n')
                    buffer = lines[-1]  # Keep incomplete line
                    
                    for line in lines[:-1]:
                        if line.strip():
                            try:
                                message = json.loads(line.strip())
                                self.total_messages += 1
                                
                                # Handle device registration
                                if message.get('type') == 'device_register':
                                    device_id = self._register_device(message, client_socket, address)
                                
                                # Process data if device is registered
                                if device_id and device_id in self.connected_devices:
                                    self._process_device_message(device_id, message)
                                
                            except json.JSONDecodeError as e:
                                logger.warning(f"Invalid JSON from {address}: {line[:100]}...")
                                # Send error response
                                error_response = {
                                    "type": "error",
                                    "message": "Invalid JSON format",
                                    "timestamp": datetime.now(timezone.utc).isoformat()
                                }
                                self._send_message(client_socket, error_response)
                                
                except socket.timeout:
                    # Timeout is normal, continue loop
                    continue
                except Exception as e:
                    logger.error(f"Error processing data from {address}: {e}")
                    break
        
        except Exception as e:
            logger.error(f"Client handler error for {address}: {e}")
        
        finally:
            # Clean up device connection
            if device_id:
                self._disconnect_device(device_id)
            
            try:
                client_socket.close()
            except:
                pass
            
            logger.info(f"Client {address} disconnected")
    
    def _register_device(self, message: Dict[str, Any], client_socket: socket.socket, address: tuple) -> Optional[str]:
        """Register a new device"""
        try:
            device_id = message.get('device_id')
            if not device_id:
                logger.warning(f"Device registration missing device_id from {address}")
                return None
            
            capabilities = message.get('capabilities', [])
            if isinstance(capabilities, str):
                capabilities = capabilities.split(',')
            
            with self.device_lock:
                device = ConnectedDevice(
                    device_id=device_id,
                    address=f"{address[0]}:{address[1]}",
                    socket=client_socket,
                    capabilities=capabilities
                )
                self.connected_devices[device_id] = device
            
            # Send registration acknowledgment
            ack_response = {
                "type": "device_register_ack",
                "status": "success",
                "device_id": device_id,
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            self._send_message(client_socket, ack_response)
            
            logger.info(f"Device registered: {device_id} from {address} with capabilities: {capabilities}")
            
            # Notify callback
            if self.on_device_connected:
                self.on_device_connected(device_id, device)
            
            return device_id
            
        except Exception as e:
            logger.error(f"Error registering device from {address}: {e}")
            return None
    
    def _process_device_message(self, device_id: str, message: Dict[str, Any]):
        """Process message from registered device"""
        try:
            with self.device_lock:
                if device_id not in self.connected_devices:
                    return
                
                device = self.connected_devices[device_id]
                device.last_heartbeat = datetime.now(timezone.utc)
                device.data_count += 1
            
            message_type = message.get('type')
            
            # Handle heartbeat
            if message_type == 'heartbeat':
                # Send heartbeat response
                response = {
                    "type": "heartbeat_ack",
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                self._send_message(device.socket, response)
            
            # Handle recording status updates
            elif message_type == 'recording_status':
                with self.device_lock:
                    device.is_recording = message.get('recording', False)
                logger.info(f"Device {device_id} recording status: {device.is_recording}")
            
            # Notify callback for all message types
            if self.on_data_received:
                self.on_data_received(device_id, message)
                
        except Exception as e:
            logger.error(f"Error processing message from {device_id}: {e}")
            if self.on_error:
                self.on_error(f"process_message_{device_id}", e)
    
    def _send_message(self, client_socket: socket.socket, message: Dict[str, Any]) -> bool:
        """Send JSON message to client with error handling"""
        try:
            json_str = json.dumps(message) + '\n'
            client_socket.send(json_str.encode('utf-8'))
            return True
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
            return False
    
    def send_command(self, device_id: str, command: Dict[str, Any]) -> bool:
        """Send command to specific device"""
        with self.device_lock:
            if device_id not in self.connected_devices:
                logger.warning(f"Device {device_id} not connected")
                return False
            
            device = self.connected_devices[device_id]
            
        # Add timestamp to command
        command['timestamp'] = datetime.now(timezone.utc).isoformat()
        
        return self._send_message(device.socket, command)
    
    def broadcast_command(self, command: Dict[str, Any]) -> int:
        """Broadcast command to all connected devices"""
        sent_count = 0
        command['timestamp'] = datetime.now(timezone.utc).isoformat()
        
        with self.device_lock:
            for device in self.connected_devices.values():
                if self._send_message(device.socket, command):
                    sent_count += 1
        
        return sent_count
    
    def _disconnect_device(self, device_id: str):
        """Disconnect and clean up device"""
        with self.device_lock:
            if device_id in self.connected_devices:
                device = self.connected_devices.pop(device_id)
                try:
                    device.socket.close()
                except:
                    pass
                
                logger.info(f"Device disconnected: {device_id}")
                
                # Notify callback
                if self.on_device_disconnected:
                    self.on_device_disconnected(device_id)
    
    def _heartbeat_monitor(self):
        """Monitor device heartbeats and disconnect stale connections"""
        while self.running:
            try:
                now = datetime.now(timezone.utc)
                stale_devices = []
                
                with self.device_lock:
                    for device_id, device in self.connected_devices.items():
                        time_since_heartbeat = (now - device.last_heartbeat).total_seconds()
                        if time_since_heartbeat > self.heartbeat_timeout:
                            stale_devices.append(device_id)
                
                # Disconnect stale devices
                for device_id in stale_devices:
                    logger.warning(f"Device {device_id} heartbeat timeout, disconnecting")
                    self._disconnect_device(device_id)
                
                time.sleep(5.0)  # Check every 5 seconds
                
            except Exception as e:
                logger.error(f"Heartbeat monitor error: {e}")
    
    def get_connected_devices(self) -> Dict[str, ConnectedDevice]:
        """Get copy of currently connected devices"""
        with self.device_lock:
            return dict(self.connected_devices)
    
    def get_statistics(self) -> Dict[str, Any]:
        """Get server statistics"""
        uptime = None
        if self.start_time:
            uptime = (datetime.now(timezone.utc) - self.start_time).total_seconds()
        
        with self.device_lock:
            device_count = len(self.connected_devices)
            recording_count = sum(1 for d in self.connected_devices.values() if d.is_recording)
        
        return {
            "uptime_seconds": uptime,
            "total_connections": self.total_connections,
            "current_devices": device_count,
            "recording_devices": recording_count,
            "total_messages": self.total_messages,
            "use_tls": self.use_tls,
            "heartbeat_timeout": self.heartbeat_timeout
        }


# Demonstration function
def demo_enhanced_server():
    """Demonstrate the enhanced TCP server"""
    
    def on_device_connected(device_id: str, device: ConnectedDevice):
        print(f"✅ Device connected: {device_id} from {device.address}")
        print(f"   Capabilities: {device.capabilities}")
    
    def on_device_disconnected(device_id: str):
        print(f"❌ Device disconnected: {device_id}")
    
    def on_data_received(device_id: str, data: Dict[str, Any]):
        print(f"📊 Data from {device_id}: {data.get('type', 'unknown')} "
              f"at {data.get('timestamp', 'no timestamp')}")
    
    def on_error(context: str, error: Exception):
        print(f"⚠️  Error in {context}: {error}")
    
    # Create and configure server
    server = EnhancedTCPServer(port=8080, use_tls=False)
    server.on_device_connected = on_device_connected
    server.on_device_disconnected = on_device_disconnected
    server.on_data_received = on_data_received
    server.on_error = on_error
    
    if server.start():
        print("🚀 Enhanced TCP server started successfully!")
        print("📱 Waiting for Android device connections...")
        print("🔄 Server statistics will be shown every 30 seconds")
        
        try:
            while True:
                time.sleep(30)
                stats = server.get_statistics()
                print(f"\n📈 Server Statistics:")
                print(f"   Uptime: {stats['uptime_seconds']:.1f}s")
                print(f"   Total connections: {stats['total_connections']}")
                print(f"   Current devices: {stats['current_devices']}")
                print(f"   Recording devices: {stats['recording_devices']}")
                print(f"   Total messages: {stats['total_messages']}")
                
        except KeyboardInterrupt:
            print("\n🛑 Shutting down server...")
            server.stop()
    else:
        print("❌ Failed to start server")


if __name__ == "__main__":
    demo_enhanced_server()