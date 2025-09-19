#!/usr/bin/env python3
"""
Simplified IRCamera PC Controller MVP

Single-file implementation consolidating the core functionality
recommended in OVER_ENGINEERED_ANALYSIS.md
"""

import json
import socket
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any

# Simple logging without external dependencies
class SimpleLogger:
    def info(self, msg): print(f"[INFO] {datetime.now().strftime('%H:%M:%S')} - {msg}")
    def warning(self, msg): print(f"[WARN] {datetime.now().strftime('%H:%M:%S')} - {msg}")
    def error(self, msg): print(f"[ERROR] {datetime.now().strftime('%H:%M:%S')} - {msg}")

logger = SimpleLogger()

class DeviceRegistry:
    """Simple device registry for connected Android devices"""
    
    def __init__(self):
        self.devices: Dict[str, Dict[str, Any]] = {}
        self.lock = threading.Lock()
    
    def register_device(self, device_id: str, ip: str, capabilities: List[str]):
        with self.lock:
            self.devices[device_id] = {
                'ip': ip,
                'capabilities': capabilities,
                'status': 'connected',
                'last_seen': time.time()
            }
        logger.info(f"Device registered: {device_id} ({ip}) - {capabilities}")
    
    def get_devices(self) -> Dict[str, Dict[str, Any]]:
        with self.lock:
            return self.devices.copy()
    
    def update_device_status(self, device_id: str, status: str):
        with self.lock:
            if device_id in self.devices:
                self.devices[device_id]['status'] = status
                self.devices[device_id]['last_seen'] = time.time()

class SessionManager:
    """Simple session management"""
    
    def __init__(self, base_dir: str = "./sessions"):
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(exist_ok=True)
        self.current_session: Optional[str] = None
        self.session_devices: List[str] = []
    
    def create_session(self, session_id: str = None) -> str:
        if not session_id:
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        session_dir = self.base_dir / session_id
        session_dir.mkdir(exist_ok=True)
        
        self.current_session = session_id
        logger.info(f"Created session: {session_id}")
        return session_id
    
    def start_recording(self, devices: List[str]):
        if not self.current_session:
            raise ValueError("No active session")
        
        self.session_devices = devices
        logger.info(f"Started recording with {len(devices)} devices")
    
    def stop_recording(self):
        if self.current_session:
            logger.info(f"Stopped recording session: {self.current_session}")
            self.session_devices = []
    
    def finalize_session(self):
        if self.current_session:
            logger.info(f"Finalized session: {self.current_session}")
            self.current_session = None

class SimpleTCPServer:
    """Basic TCP server for device communication"""
    
    def __init__(self, port: int = 8080):
        self.port = port
        self.running = False
        self.socket = None
        self.clients = {}
        self.device_registry = DeviceRegistry()
        self.session_manager = SessionManager()
    
    def handle_client(self, client_socket, address):
        """Handle individual client connection"""
        client_id = f"{address[0]}:{address[1]}"
        logger.info(f"Client connected: {client_id}")
        
        try:
            while self.running:
                data = client_socket.recv(4096)
                if not data:
                    break
                
                try:
                    message = json.loads(data.decode('utf-8'))
                    response = self.handle_message(message, client_id)
                    
                    if response:
                        client_socket.send(json.dumps(response).encode('utf-8'))
                        
                except json.JSONDecodeError:
                    logger.error(f"Invalid JSON from {client_id}")
                except Exception as e:
                    logger.error(f"Error handling message from {client_id}: {e}")
                    
        except Exception as e:
            logger.error(f"Client handler error for {client_id}: {e}")
        finally:
            client_socket.close()
            logger.info(f"Client disconnected: {client_id}")
    
    def handle_message(self, message: Dict[str, Any], client_id: str) -> Optional[Dict[str, Any]]:
        """Handle incoming message from device"""
        msg_type = message.get('type')
        
        if msg_type == 'device_register':
            device_id = message.get('device_id', client_id)
            capabilities = message.get('capabilities', [])
            ip = client_id.split(':')[0]
            
            self.device_registry.register_device(device_id, ip, capabilities)
            return {'type': 'ack', 'status': 'registered'}
        
        elif msg_type == 'heartbeat':
            device_id = message.get('device_id', client_id)
            self.device_registry.update_device_status(device_id, 'connected')
            return {'type': 'ack'}
        
        elif msg_type == 'start_recording':
            session_id = self.session_manager.create_session()
            devices = list(self.device_registry.get_devices().keys())
            self.session_manager.start_recording(devices)
            
            # Send start command to all devices
            for device_id in devices:
                self.device_registry.update_device_status(device_id, 'recording')
            
            return {
                'type': 'start_recording',
                'session_id': session_id,
                'timestamp': datetime.now().isoformat()
            }
        
        elif msg_type == 'stop_recording':
            self.session_manager.stop_recording()
            
            # Update all devices status
            for device_id in self.device_registry.get_devices():
                self.device_registry.update_device_status(device_id, 'connected')
            
            return {'type': 'stop_recording', 'timestamp': datetime.now().isoformat()}
        
        elif msg_type == 'data':
            # Simple data logging (could be enhanced for actual data processing)
            logger.info(f"Data received from {client_id}: {len(str(message))} bytes")
            return {'type': 'ack'}
        
        return None
    
    def start(self):
        """Start the TCP server"""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            self.socket.bind(('0.0.0.0', self.port))
            self.socket.listen(5)
            self.running = True
            
            logger.info(f"MVP TCP Server started on port {self.port}")
            
            while self.running:
                try:
                    client_socket, address = self.socket.accept()
                    client_thread = threading.Thread(
                        target=self.handle_client,
                        args=(client_socket, address)
                    )
                    client_thread.daemon = True
                    client_thread.start()
                    
                except Exception as e:
                    if self.running:
                        logger.error(f"Accept error: {e}")
                        
        except KeyboardInterrupt:
            logger.info("Server shutdown requested")
        except Exception as e:
            logger.error(f"Server error: {e}")
        finally:
            self.stop()
    
    def stop(self):
        """Stop the server"""
        self.running = False
        if self.socket:
            self.socket.close()
        logger.info("Server stopped")
    
    def get_status(self):
        """Get server status for CLI"""
        devices = self.device_registry.get_devices()
        return {
            'running': self.running,
            'connected_devices': len(devices),
            'devices': devices,
            'current_session': self.session_manager.current_session
        }

def print_status(server):
    """Print current server status"""
    status = server.get_status()
    print(f"\n{'='*50}")
    print(f"IRCamera MVP PC Controller Status")
    print(f"{'='*50}")
    print(f"Server running: {status['running']}")
    print(f"Connected devices: {status['connected_devices']}")
    print(f"Current session: {status['current_session'] or 'None'}")
    
    if status['devices']:
        print(f"\nDevices:")
        for device_id, info in status['devices'].items():
            print(f"  {device_id}: {info['status']} - {info['capabilities']}")
    print()

def main():
    """Main application entry point"""
    print("IRCamera PC Controller - Simplified MVP")
    print("Press Ctrl+C to stop, 's' + Enter for status")
    
    server = SimpleTCPServer(port=8080)
    
    # Start status thread
    def status_monitor():
        while server.running:
            time.sleep(10)
            if server.running:
                devices = len(server.device_registry.get_devices())
                if devices > 0:
                    logger.info(f"Status: {devices} devices connected")
    
    status_thread = threading.Thread(target=status_monitor)
    status_thread.daemon = True
    status_thread.start()
    
    try:
        server.start()
    except KeyboardInterrupt:
        print("\nShutting down...")
        server.stop()

if __name__ == "__main__":
    main()