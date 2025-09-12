"""
WebSocket Secure (WSS) Server for IRCamera PC Controller
Phase 1 Implementation - PC as WSS server, phones as WebSocket clients

Implements:
- WebSocket Secure server on port 8443 with TLS
- mDNS advertising as '_irhub._tcp' service  
- Basic authentication (admin/admin for development)
- WebSocket heartbeat with PING every 5s
- Concurrent client connection handling
"""

import asyncio
import json
import ssl
import time
import uuid
import websockets
import base64
from datetime import datetime, timezone
from typing import Dict, Set, Optional, Any, Callable
from dataclasses import dataclass
from pathlib import Path

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..core.config import config
from .protocol import create_message, validate_message, get_protocol_manager
from .security import SecurityManager
from .discovery import NetworkDiscoveryService


@dataclass
class ClientConnection:
    """WebSocket client connection information"""
    websocket: websockets.WebSocketServerProtocol
    device_id: str
    device_type: str
    authenticated: bool = False
    last_ping: float = 0
    connected_at: float = 0
    capabilities: Set[str] = None
    
    def __post_init__(self):
        if self.capabilities is None:
            self.capabilities = set()
        self.connected_at = time.time()


class WebSocketServer:
    """
    WebSocket Secure server for PC-to-phone communication
    Phase 1 implementation with TLS, basic auth, and heartbeat
    """
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8443):
        self.host = host
        self.port = port
        self.server = None
        self.is_running = False
        
        # Client management
        self.clients: Dict[str, ClientConnection] = {}
        self.client_lock = asyncio.Lock()
        
        # Authentication
        self.auth_username = "admin"
        self.auth_password = "admin"  # Development credentials
        
        # Heartbeat configuration
        self.heartbeat_interval = 5.0  # 5 seconds
        self.heartbeat_timeout = 15.0  # 15 seconds silence = disconnect
        self.heartbeat_task = None
        
        # Services
        self.security_manager = SecurityManager()
        self.discovery_service = NetworkDiscoveryService()
        self.protocol_manager = get_protocol_manager()
        
        # Message handlers
        self.message_handlers: Dict[str, Callable] = {}
        self._setup_message_handlers()
        
        # TLS configuration
        self.ssl_context = None
        self._setup_tls()

    def _setup_tls(self):
        """Setup TLS/SSL configuration for WebSocket Secure"""
        try:
            self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            
            # Load certificates - use self-signed for development
            cert_dir = Path(__file__).parent.parent.parent.parent / "certificates"
            cert_file = cert_dir / "server.crt"
            key_file = cert_dir / "server.key"
            
            if cert_file.exists() and key_file.exists():
                self.ssl_context.load_cert_chain(cert_file, key_file)
                logger.info(f"Loaded TLS certificates from {cert_dir}")
            else:
                # Generate self-signed certificate for development
                logger.warning("No TLS certificates found, generating self-signed certificate")
                self.security_manager.generate_self_signed_certificate(cert_dir)
                self.ssl_context.load_cert_chain(cert_file, key_file)
                
        except Exception as e:
            logger.error(f"Failed to setup TLS: {e}")
            # Fallback to no TLS for development
            self.ssl_context = None
            logger.warning("Running without TLS encryption")

    def _setup_message_handlers(self):
        """Setup message type handlers"""
        self.message_handlers = {
            "protocol_handshake": self._handle_handshake,
            "auth_request": self._handle_auth,
            "session_start": self._handle_session_start,
            "session_stop": self._handle_session_stop,
            "sync_flash": self._handle_sync_flash,
            "status_request": self._handle_status_request,
            "heartbeat": self._handle_heartbeat,
            "pong": self._handle_pong
        }

    async def start(self):
        """Start the WebSocket Secure server"""
        if self.is_running:
            logger.warning("WebSocket server already running")
            return
            
        try:
            logger.info(f"Starting WebSocket Secure server on {self.host}:{self.port}")
            
            # Start mDNS advertising
            await self._start_mdns_advertising()
            
            # Start WebSocket server
            self.server = await websockets.serve(
                self._handle_client,
                self.host,
                self.port,
                ssl=self.ssl_context,
                ping_interval=None,  # We handle our own heartbeat
                ping_timeout=None,
                close_timeout=10
            )
            
            self.is_running = True
            
            # Start heartbeat monitoring
            self.heartbeat_task = asyncio.create_task(self._heartbeat_monitor())
            
            logger.info(f"WebSocket Secure server started successfully")
            logger.info(f"Service advertised as '_irhub._tcp' on port {self.port}")
            
        except Exception as e:
            logger.error(f"Failed to start WebSocket server: {e}")
            raise

    async def stop(self):
        """Stop the WebSocket server"""
        if not self.is_running:
            return
            
        logger.info("Stopping WebSocket Secure server")
        
        try:
            # Stop heartbeat monitoring
            if self.heartbeat_task:
                self.heartbeat_task.cancel()
                try:
                    await self.heartbeat_task
                except asyncio.CancelledError:
                    pass
            
            # Disconnect all clients
            async with self.client_lock:
                for client_id, client in list(self.clients.items()):
                    try:
                        await client.websocket.close()
                    except Exception as e:
                        logger.warning(f"Error closing client {client_id}: {e}")
                self.clients.clear()
            
            # Stop server
            if self.server:
                self.server.close()
                await self.server.wait_closed()
            
            # Stop mDNS advertising
            await self._stop_mdns_advertising()
            
            self.is_running = False
            logger.info("WebSocket Secure server stopped")
            
        except Exception as e:
            logger.error(f"Error stopping WebSocket server: {e}")

    async def _start_mdns_advertising(self):
        """Start mDNS/Zeroconf advertising"""
        try:
            service_info = {
                "name": "IRCamera Hub",
                "type": "_irhub._tcp.",
                "port": self.port,
                "properties": {
                    "proto": "v1",
                    "tls": "1" if self.ssl_context else "0",
                    "auth": "basic",
                    "capabilities": "session_start,session_stop,sync_flash,status_request,heartbeat"
                }
            }
            
            await self.discovery_service.advertise_service(service_info)
            logger.info("mDNS advertising started for '_irhub._tcp' service")
            
        except Exception as e:
            logger.error(f"Failed to start mDNS advertising: {e}")

    async def _stop_mdns_advertising(self):
        """Stop mDNS advertising"""
        try:
            await self.discovery_service.stop_advertising()
            logger.info("mDNS advertising stopped")
        except Exception as e:
            logger.error(f"Error stopping mDNS advertising: {e}")

    async def _handle_client(self, websocket, path):
        """Handle new WebSocket client connection"""
        client_id = str(uuid.uuid4())
        client_address = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        
        logger.info(f"New WebSocket connection from {client_address} (ID: {client_id})")
        
        try:
            # Create client connection object
            client = ClientConnection(
                websocket=websocket,
                device_id="",  # Will be set during handshake
                device_type="unknown"
            )
            
            async with self.client_lock:
                self.clients[client_id] = client
            
            # Handle client messages
            async for message in websocket:
                try:
                    await self._process_message(client_id, message)
                except Exception as e:
                    logger.error(f"Error processing message from {client_id}: {e}")
                    await self._send_error(client_id, "message_processing_error", str(e))
                    
        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Client {client_id} disconnected")
        except Exception as e:
            logger.error(f"Error handling client {client_id}: {e}")
        finally:
            # Clean up client
            async with self.client_lock:
                if client_id in self.clients:
                    del self.clients[client_id]
            logger.info(f"Client {client_id} cleaned up")

    async def _process_message(self, client_id: str, raw_message: str):
        """Process incoming message from client"""
        try:
            # Parse JSON message
            message = json.loads(raw_message)
            
            # Validate protocol version
            if not self.protocol_manager.validate_message_version(message):
                await self._send_error(client_id, "protocol_version_error", "Unsupported protocol version")
                return
            
            message_type = message.get("message_type")
            if not message_type:
                await self._send_error(client_id, "invalid_message", "Missing message_type")
                return
            
            # Get client
            async with self.client_lock:
                client = self.clients.get(client_id)
                if not client:
                    logger.error(f"Client {client_id} not found")
                    return
            
            # Check authentication for protected messages
            if message_type not in ["protocol_handshake", "auth_request", "pong"] and not client.authenticated:
                await self._send_error(client_id, "authentication_required", "Authentication required")
                return
            
            # Handle message
            handler = self.message_handlers.get(message_type)
            if handler:
                await handler(client_id, message)
            else:
                logger.warning(f"Unknown message type: {message_type}")
                await self._send_error(client_id, "unknown_message_type", f"Unknown message type: {message_type}")
                
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON from client {client_id}: {e}")
            await self._send_error(client_id, "invalid_json", "Invalid JSON format")
        except Exception as e:
            logger.error(f"Error processing message from client {client_id}: {e}")
            await self._send_error(client_id, "processing_error", str(e))

    async def _handle_handshake(self, client_id: str, message: dict):
        """Handle protocol handshake"""
        try:
            device_id = message.get("device_id", "")
            device_type = message.get("device_type", "unknown")
            capabilities = set(message.get("capabilities", "").split(","))
            
            # Update client info
            async with self.client_lock:
                client = self.clients.get(client_id)
                if client:
                    client.device_id = device_id
                    client.device_type = device_type
                    client.capabilities = capabilities
            
            # Send handshake response
            response = create_message("protocol_handshake_response", {
                "protocol_version": "v1",
                "min_supported_version": "v1",
                "server_capabilities": "session_start,session_stop,sync_flash,status_request,heartbeat",
                "auth_required": True,
                "auth_method": "basic"
            })
            
            await self._send_message(client_id, response)
            logger.info(f"Handshake completed for client {client_id} (device: {device_id})")
            
        except Exception as e:
            logger.error(f"Error in handshake for client {client_id}: {e}")
            await self._send_error(client_id, "handshake_error", str(e))

    async def _handle_auth(self, client_id: str, message: dict):
        """Handle authentication request"""
        try:
            auth_type = message.get("auth_type", "")
            credentials = message.get("credentials", "")
            
            if auth_type != "basic":
                await self._send_error(client_id, "auth_error", "Only basic authentication supported")
                return
            
            # Decode basic auth credentials
            try:
                decoded = base64.b64decode(credentials).decode('utf-8')
                username, password = decoded.split(':', 1)
            except Exception:
                await self._send_error(client_id, "auth_error", "Invalid credentials format")
                return
            
            # Validate credentials
            if username == self.auth_username and password == self.auth_password:
                # Mark client as authenticated
                async with self.client_lock:
                    client = self.clients.get(client_id)
                    if client:
                        client.authenticated = True
                
                # Send success response
                response = create_message("auth_response", {
                    "success": True,
                    "session_token": str(uuid.uuid4())  # Simple session token
                })
                await self._send_message(client_id, response)
                logger.info(f"Client {client_id} authenticated successfully")
            else:
                await self._send_error(client_id, "auth_error", "Invalid credentials")
                
        except Exception as e:
            logger.error(f"Error in authentication for client {client_id}: {e}")
            await self._send_error(client_id, "auth_error", str(e))

    async def _handle_session_start(self, client_id: str, message: dict):
        """Handle session start request"""
        logger.info(f"Session start requested by client {client_id}")
        
        response = create_message("session_start_response", {
            "success": True,
            "session_id": str(uuid.uuid4()),
            "start_time": time.time()
        })
        await self._send_message(client_id, response)

    async def _handle_session_stop(self, client_id: str, message: dict):
        """Handle session stop request"""
        logger.info(f"Session stop requested by client {client_id}")
        
        response = create_message("session_stop_response", {
            "success": True,
            "stop_time": time.time()
        })
        await self._send_message(client_id, response)

    async def _handle_sync_flash(self, client_id: str, message: dict):
        """Handle sync flash request"""
        duration = message.get("duration_ms", 500)
        logger.info(f"Sync flash requested by client {client_id} for {duration}ms")
        
        # Broadcast sync flash to all authenticated clients
        flash_message = create_message("sync_flash_trigger", {
            "duration_ms": duration,
            "timestamp": time.time()
        })
        
        await self._broadcast_message(flash_message, exclude_client=client_id)
        
        response = create_message("sync_flash_response", {
            "success": True,
            "duration_ms": duration
        })
        await self._send_message(client_id, response)

    async def _handle_status_request(self, client_id: str, message: dict):
        """Handle status request"""
        async with self.client_lock:
            client_count = len(self.clients)
            authenticated_count = sum(1 for c in self.clients.values() if c.authenticated)
        
        response = create_message("status_response", {
            "server_status": "running",
            "connected_clients": client_count,
            "authenticated_clients": authenticated_count,
            "uptime": time.time() - self._start_time if hasattr(self, '_start_time') else 0
        })
        await self._send_message(client_id, response)

    async def _handle_heartbeat(self, client_id: str, message: dict):
        """Handle heartbeat from client"""
        async with self.client_lock:
            client = self.clients.get(client_id)
            if client:
                client.last_ping = time.time()
        
        # Send heartbeat response
        response = create_message("heartbeat_response", {
            "timestamp": time.time()
        })
        await self._send_message(client_id, response)

    async def _handle_pong(self, client_id: str, message: dict):
        """Handle pong response from client"""
        async with self.client_lock:
            client = self.clients.get(client_id)
            if client:
                client.last_ping = time.time()

    async def _heartbeat_monitor(self):
        """Monitor client heartbeats and disconnect stale clients"""
        while self.is_running:
            try:
                current_time = time.time()
                stale_clients = []
                
                async with self.client_lock:
                    for client_id, client in self.clients.items():
                        if client.authenticated and client.last_ping > 0:
                            time_since_ping = current_time - client.last_ping
                            if time_since_ping > self.heartbeat_timeout:
                                stale_clients.append(client_id)
                
                # Disconnect stale clients
                for client_id in stale_clients:
                    logger.warning(f"Disconnecting stale client {client_id}")
                    try:
                        async with self.client_lock:
                            client = self.clients.get(client_id)
                            if client:
                                await client.websocket.close()
                    except Exception as e:
                        logger.error(f"Error disconnecting stale client {client_id}: {e}")
                
                # Send ping to all authenticated clients
                ping_message = create_message("ping", {"timestamp": current_time})
                await self._broadcast_message(ping_message, authenticated_only=True)
                
                await asyncio.sleep(self.heartbeat_interval)
                
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in heartbeat monitor: {e}")
                await asyncio.sleep(self.heartbeat_interval)

    async def _send_message(self, client_id: str, message: dict):
        """Send message to specific client"""
        try:
            async with self.client_lock:
                client = self.clients.get(client_id)
                if not client:
                    logger.error(f"Cannot send message to unknown client {client_id}")
                    return
            
            json_message = json.dumps(message)
            await client.websocket.send(json_message)
            
        except Exception as e:
            logger.error(f"Error sending message to client {client_id}: {e}")

    async def _send_error(self, client_id: str, error_type: str, error_message: str):
        """Send error message to client"""
        error_msg = create_message("error", {
            "error_type": error_type,
            "error_message": error_message,
            "timestamp": time.time()
        })
        await self._send_message(client_id, error_msg)

    async def _broadcast_message(self, message: dict, exclude_client: str = None, authenticated_only: bool = False):
        """Broadcast message to all connected clients"""
        json_message = json.dumps(message)
        
        async with self.client_lock:
            for client_id, client in self.clients.items():
                if client_id == exclude_client:
                    continue
                    
                if authenticated_only and not client.authenticated:
                    continue
                
                try:
                    await client.websocket.send(json_message)
                except Exception as e:
                    logger.error(f"Error broadcasting to client {client_id}: {e}")

# Convenience function to create server instance
def create_websocket_server(host: str = "0.0.0.0", port: int = 8443) -> WebSocketServer:
    """Create a WebSocket Secure server instance"""
    return WebSocketServer(host, port)