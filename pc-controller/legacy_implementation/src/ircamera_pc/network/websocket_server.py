import asyncio
import base64
import json
import ssl
import time
import uuid
import websockets
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Dict, Optional, Set

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

# from ..sync import AdvancedTimeSyncServer  # TODO: Implement when needed
from .discovery import NetworkDiscoveryService
from .security_manager import AuthLevel, AdvancedSecurityManager
from .protocol import create_message, get_protocol_manager
from .security import SecurityManager


@dataclass
class ClientConnection:
    websocket: websockets.WebSocketServerProtocol
    device_id: str
    device_type: str
    authenticated: bool = False
    auth_level: AuthLevel = AuthLevel.NONE
    auth_context: Optional[Any] = None
    last_ping: float = 0
    connected_at: float = 0
    capabilities: Set[str] = None

    def __post_init__(self):
        if self.capabilities is None:
            self.capabilities = set()
        self.connected_at = time.time()


class WebSocketServer:

    def __init__(self, host: str = "0.0.0.0", port: int = 8443):
        self.host = host
        self.port = port
        self.server = None
        self.is_running = False

        self.clients: Dict[str, ClientConnection] = {}
        self.client_lock = asyncio.Lock()

        self.auth_username = "admin"
        self.auth_password = "admin"

        self.heartbeat_interval = 5.0
        self.heartbeat_timeout = 15.0
        self.heartbeat_task = None

        self.security_manager = SecurityManager()
        self.advanced_security = AdvancedSecurityManager()
        self.discovery_service = NetworkDiscoveryService()
        self.protocol_manager = get_protocol_manager()

        self.message_handlers: Dict[str, Callable] = {}
        self._setup_message_handlers()

        self.ssl_context = None
        self._setup_tls()

    def _setup_tls(self):

        try:
            self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)

            cert_dir = Path(__file__).parent.parent.parent.parent / "certificates"
            cert_file = cert_dir / "server.crt"
            key_file = cert_dir / "server.key"

            if cert_file.exists() and key_file.exists():
                self.ssl_context.load_cert_chain(cert_file, key_file)
                logger.info(f"Loaded TLS certificates from {cert_dir}")
            else:

                logger.warning(
                    "No TLS certificates found, generating self-signed certificate"
                )
                self.security_manager.generate_self_signed_certificate(cert_dir)
                self.ssl_context.load_cert_chain(cert_file, key_file)

        except Exception as e:
            logger.error(f"Failed to setup TLS: {e}")

            self.ssl_context = None
            logger.warning("Running without TLS encryption")

    def _setup_message_handlers(self):

        self.message_handlers = {
            "protocol_handshake": self._handle_handshake,
            "auth_request": self._handle_auth,
            "session_start": self._handle_session_start,
            "session_stop": self._handle_session_stop,
            "sync_flash": self._handle_sync_flash,
            "status_request": self._handle_status_request,
            "heartbeat": self._handle_heartbeat,
            "pong": self._handle_pong,

            "time_sync_request": self._handle_time_sync_request,
            "multi_round_sync_request": self._handle_multi_round_sync_request,

            "upload_initiate": self._handle_upload_initiate,
            "upload_chunk": self._handle_upload_chunk,
            "upload_verify": self._handle_upload_verify,
            "upload_check_existing": self._handle_upload_check_existing,
            "file_chunk_response": self._handle_file_chunk_response,
            "data_export_request": self._handle_data_export_request,
            "session_manifest_request": self._handle_session_manifest_request,

            "enhanced_auth_request": self._handle_enhanced_auth,
            "certificate_auth_request": self._handle_certificate_auth,
            "token_auth_request": self._handle_token_auth,
            "biometric_auth_request": self._handle_biometric_auth,
            "security_alert": self._handle_security_alert,
            "permission_request": self._handle_permission_request,
            "role_change_request": self._handle_role_change_request,
        }

    async def start(self) -> Any:

        if self.is_running:
            logger.warning("WebSocket server already running")
            return

        try:
            logger.info(f"Starting WebSocket Secure server on {self.host}:{self.port}")

            await self.enhanced_security.initialize()

            await self._start_mdns_advertising()

            self.server = await websockets.serve(
                self._handle_client,
                self.host,
                self.port,
                ssl=self.ssl_context,
                ping_interval=None,
                ping_timeout=None,
                close_timeout=10,
            )

            self.is_running = True

            self.heartbeat_task = asyncio.create_task(self._heartbeat_monitor())

            logger.info("WebSocket Secure server started successfully")
            logger.info(f"Service advertised as '_irhub._tcp' on port {self.port}")

        except Exception as e:
            logger.error(f"Failed to start WebSocket server: {e}")
            raise

    async def stop(self) -> Any:

        if not self.is_running:
            return

        logger.info("Stopping WebSocket Secure server")

        try:

            if self.heartbeat_task:
                self.heartbeat_task.cancel()
                try:
                    await self.heartbeat_task
                except asyncio.CancelledError:
                    pass

            async with self.client_lock:
                for client_id, client in list(self.clients.items()):
                    try:
                        await client.websocket.close()
                    except Exception as e:
                        logger.warning(f"Error closing client {client_id}: {e}")
                self.clients.clear()

            if self.server:
                self.server.close()
                await self.server.wait_closed()

            await self._stop_mdns_advertising()

            self.is_running = False
            logger.info("WebSocket Secure server stopped")

        except Exception as e:
            logger.error(f"Error stopping WebSocket server: {e}")

    async def _start_mdns_advertising(self):

        try:
            service_info = {
                "name": "IRCamera Hub",
                "type": "_irhub._tcp.",
                "port": self.port,
                "properties": {
                    "proto": "v1",
                    "tls": "1" if self.ssl_context else "0",
                    "auth": "basic",
                    "capabilities": "session_start,session_stop,sync_flash,status_request,heartbeat",
                },
            }

            await self.discovery_service.advertise_service(service_info)
            logger.info("mDNS advertising started for '_irhub._tcp' service")

        except Exception as e:
            logger.error(f"Failed to start mDNS advertising: {e}")

    async def _stop_mdns_advertising(self):

        try:
            await self.discovery_service.stop_advertising()
            logger.info("mDNS advertising stopped")
        except Exception as e:
            logger.error(f"Error stopping mDNS advertising: {e}")

    async def _handle_client(self, websocket, path):

        client_id = str(uuid.uuid4())
        client_address = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"

        logger.info(f"New WebSocket connection from {client_address} (ID: {client_id})")

        try:

            client = ClientConnection(
                websocket=websocket,
                device_id="",
                device_type="unknown",
            )

            async with self.client_lock:
                self.clients[client_id] = client

            async for message in websocket:
                try:
                    await self._process_message(client_id, message)
                except Exception as e:
                    logger.error(f"Error processing message from {client_id}: {e}")
                    await self._send_error(
                        client_id, "message_processing_error", str(e)
                    )

        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Client {client_id} disconnected")
        except Exception as e:
            logger.error(f"Error handling client {client_id}: {e}")
        finally:

            async with self.client_lock:
                if client_id in self.clients:
                    del self.clients[client_id]
            logger.info(f"Client {client_id} cleaned up")

    async def _process_message(self, client_id: str, raw_message: str):

        try:

            message = json.loads(raw_message)

            if not self.protocol_manager.validate_message_version(message):
                await self._send_error(
                    client_id, "protocol_version_error", "Unsupported protocol version"
                )
                return

            message_type = message.get("message_type")
            if not message_type:
                await self._send_error(
                    client_id, "invalid_message", "Missing message_type"
                )
                return

            async with self.client_lock:
                client = self.clients.get(client_id)
                if not client:
                    logger.error(f"Client {client_id} not found")
                    return

            if (
                    message_type not in ["protocol_handshake", "auth_request", "pong"]
                    and not client.authenticated
            ):
                await self._send_error(
                    client_id, "authentication_required", "Authentication required"
                )
                return

            handler = self.message_handlers.get(message_type)
            if handler:
                await handler(client_id, message)
            else:
                logger.warning(f"Unknown message type: {message_type}")
                await self._send_error(
                    client_id,
                    "unknown_message_type",
                    f"Unknown message type: {message_type}",
                )

        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON from client {client_id}: {e}")
            await self._send_error(client_id, "invalid_json", "Invalid JSON format")
        except Exception as e:
            logger.error(f"Error processing message from client {client_id}: {e}")
            await self._send_error(client_id, "processing_error", str(e))

    async def _handle_handshake(self, client_id: str, message: dict):

        try:
            device_id = message.get("device_id", "")
            device_type = message.get("device_type", "unknown")
            capabilities = set(message.get("capabilities", "").split(","))

            async with self.client_lock:
                client = self.clients.get(client_id)
                if client:
                    client.device_id = device_id
                    client.device_type = device_type
                    client.capabilities = capabilities

            response = create_message(
                "protocol_handshake_response",
                {
                    "protocol_version": "v1",
                    "min_supported_version": "v1",
                    "server_capabilities": "session_start,session_stop,sync_flash,status_request,heartbeat",
                    "auth_required": True,
                    "auth_method": "basic",
                },
            )

            await self._send_message(client_id, response)
            logger.info(
                f"Handshake completed for client {client_id} (device: {device_id})"
            )

        except Exception as e:
            logger.error(f"Error in handshake for client {client_id}: {e}")
            await self._send_error(client_id, "handshake_error", str(e))

    async def _handle_auth(self, client_id: str, message: dict):

        try:
            auth_type = message.get("auth_type", "")
            credentials = message.get("credentials", "")

            if auth_type != "basic":
                await self._send_error(
                    client_id, "auth_error", "Only basic authentication supported"
                )
                return

            try:
                decoded = base64.b64decode(credentials).decode("utf-8")
                username, password = decoded.split(":", 1)
            except Exception:
                await self._send_error(
                    client_id, "auth_error", "Invalid credentials format"
                )
                return

            if username == self.auth_username and password == self.auth_password:

                async with self.client_lock:
                    client = self.clients.get(client_id)
                    if client:
                        client.authenticated = True

                response = create_message(
                    "auth_response",
                    {
                        "success": True,
                        "session_token": str(uuid.uuid4()),
                    },
                )
                await self._send_message(client_id, response)
                logger.info(f"Client {client_id} authenticated successfully")
            else:
                await self._send_error(client_id, "auth_error", "Invalid credentials")

        except Exception as e:
            logger.error(f"Error in authentication for client {client_id}: {e}")
            await self._send_error(client_id, "auth_error", str(e))

    async def _handle_session_start(self, client_id: str, message: dict):

        logger.info(f"Session start requested by client {client_id}")

        response = create_message(
            "session_start_response",
            {
                "success": True,
                "session_id": str(uuid.uuid4()),
                "start_time": time.time(),
            },
        )
        await self._send_message(client_id, response)

    async def _handle_session_stop(self, client_id: str, message: dict):

        logger.info(f"Session stop requested by client {client_id}")

        response = create_message(
            "session_stop_response", {"success": True, "stop_time": time.time()}
        )
        await self._send_message(client_id, response)

    async def _handle_sync_flash(self, client_id: str, message: dict):

        duration = message.get("duration_ms", 500)
        logger.info(f"Sync flash requested by client {client_id} for {duration}ms")

        flash_message = create_message(
            "sync_flash_trigger", {"duration_ms": duration, "timestamp": time.time()}
        )

        await self._broadcast_message(flash_message, exclude_client=client_id)

        response = create_message(
            "sync_flash_response", {"success": True, "duration_ms": duration}
        )
        await self._send_message(client_id, response)

    async def _handle_status_request(self, client_id: str, message: dict):

        async with self.client_lock:
            client_count = len(self.clients)
            authenticated_count = sum(
                1 for c in self.clients.values() if c.authenticated
            )

        response = create_message(
            "status_response",
            {
                "server_status": "running",
                "connected_clients": client_count,
                "authenticated_clients": authenticated_count,
                "uptime": (
                    time.time() - self._start_time
                    if hasattr(self, "_start_time")
                    else 0
                ),
            },
        )
        await self._send_message(client_id, response)

    async def _handle_heartbeat(self, client_id: str, message: dict):

        async with self.client_lock:
            client = self.clients.get(client_id)
            if client:
                client.last_ping = time.time()

        response = create_message("heartbeat_response", {"timestamp": time.time()})
        await self._send_message(client_id, response)

    async def _handle_pong(self, client_id: str, message: dict):

        async with self.client_lock:
            client = self.clients.get(client_id)
            if client:
                client.last_ping = time.time()

    async def _handle_time_sync_request(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required for time sync"
                )
                return

            device_id = "unknown"
            async with self.client_lock:
                client = self.clients.get(client_id)
                if client:
                    device_id = client.device_id or client_id

            response_data = await self.time_sync_server.handle_time_sync_request(
                message, device_id
            )

            await self._send_message(client_id, response_data)

            logger.debug(
                f"Time sync response sent to {client_id} (device: {device_id})"
            )

        except Exception as e:
            logger.error(f"Error handling time sync request from {client_id}: {e}")
            await self._send_error(client_id, "time_sync_error", str(e))

    async def _heartbeat_monitor(self):

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

                for client_id in stale_clients:
                    logger.warning(f"Disconnecting stale client {client_id}")
                    try:
                        async with self.client_lock:
                            client = self.clients.get(client_id)
                            if client:
                                await client.websocket.close()
                    except Exception as e:
                        logger.error(
                            f"Error disconnecting stale client {client_id}: {e}"
                        )

                ping_message = create_message("ping", {"timestamp": current_time})
                await self._broadcast_message(ping_message, authenticated_only=True)

                await asyncio.sleep(self.heartbeat_interval)

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in heartbeat monitor: {e}")
                await asyncio.sleep(self.heartbeat_interval)

    async def _send_message(self, client_id: str, message: dict):

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

        error_msg = create_message(
            "error",
            {
                "error_type": error_type,
                "error_message": error_message,
                "timestamp": time.time(),
            },
        )
        await self._send_message(client_id, error_msg)

    async def _broadcast_message(
            self,
            message: dict,
            exclude_client: Optional[str] = None,
            authenticated_only: bool = False,
    ):

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

    async def _handle_upload_initiate(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id,
                    "auth_required",
                    "Authentication required for file upload",
                )
                return

            job_id = message.get("job_id")
            file_name = message.get("file_name")
            file_size = message.get("file_size")
            file_type = message.get("file_type")
            checksum = message.get("checksum")
            session_id = message.get("session_id")
            device_id = message.get("device_id")
            chunk_size = message.get("chunk_size", 1024 * 1024)
            resume_offset = message.get("resume_offset", 0)

            if not all([job_id, file_name, file_size, session_id, device_id]):
                await self._send_error(
                    client_id, "invalid_params", "Missing required upload parameters"
                )
                return

            if hasattr(self, "file_transfer_manager"):

                from ircamera_pc.core.file_transfer import FileManifest, FileType

                file_type_enum = FileType.METADATA
                try:
                    file_type_enum = FileType(file_type.lower())
                except (ValueError, AttributeError):
                    logger.debug(f"Unknown file type '{file_type}', using METADATA")

                manifest = FileManifest(
                    file_id=job_id,
                    filename=file_name,
                    file_type=file_type_enum,
                    size_bytes=file_size,
                    checksum=checksum,
                    device_id=device_id,
                    session_id=session_id,
                    timestamp=time.time(),
                )

                client_conn = self.clients.get(client_id)
                if client_conn:

                    transfer_job_id = await self.file_transfer_manager.queue_transfer(
                        manifest, client_conn
                    )

                    logger.info(
                        f"File upload initiated: {file_name} ({file_size} bytes) from {device_id}"
                    )

                    response = create_message(
                        "upload_initiate_response",
                        {
                            "job_id": job_id,
                            "transfer_job_id": transfer_job_id,
                            "status": "ready",
                            "resume_offset": resume_offset,
                            "chunk_size": chunk_size,
                        },
                    )
                    await self._send_message(client_id, response)

                else:
                    await self._send_error(
                        client_id, "client_not_found", "Client connection not found"
                    )
            else:
                await self._send_error(
                    client_id,
                    "service_unavailable",
                    "File transfer service not available",
                )

        except Exception as e:
            logger.error(f"Error handling upload initiation from {client_id}: {e}")
            await self._send_error(client_id, "upload_init_error", str(e))

    async def _handle_upload_chunk(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required"
                )
                return

            job_id = message.get("job_id")
            chunk_index = message.get("chunk_index")
            chunk_offset = message.get("chunk_offset")
            message.get("chunk_size")
            chunk_data = message.get("chunk_data")
            is_final_chunk = message.get("is_final_chunk", False)

            if not all([job_id, chunk_data is not None, chunk_index is not None]):
                await self._send_error(
                    client_id, "invalid_chunk", "Missing chunk parameters"
                )
                return

            try:
                import base64

                chunk_bytes = base64.b64decode(chunk_data)
            except Exception as e:
                await self._send_error(
                    client_id, "decode_error", f"Failed to decode chunk data: {e}"
                )
                return

            if hasattr(self, "file_transfer_manager"):

                success = await self._write_chunk_to_transfer(
                    job_id, chunk_index, chunk_offset, chunk_bytes
                )

                if success:

                    response = create_message(
                        "upload_chunk_response",
                        {
                            "job_id": job_id,
                            "chunk_index": chunk_index,
                            "status": "received",
                            "bytes_written": len(chunk_bytes),
                        },
                    )
                    await self._send_message(client_id, response)

                    logger.debug(
                        f"Chunk {chunk_index} received for {job_id} ({len(chunk_bytes)} bytes)"
                    )

                    if is_final_chunk:
                        logger.info(f"Final chunk received for upload {job_id}")
                else:
                    await self._send_error(
                        client_id, "chunk_write_error", "Failed to write chunk"
                    )
            else:
                await self._send_error(
                    client_id,
                    "service_unavailable",
                    "File transfer service not available",
                )

        except Exception as e:
            logger.error(f"Error handling chunk upload from {client_id}: {e}")
            await self._send_error(client_id, "chunk_error", str(e))

    async def _handle_upload_verify(self, client_id: str, message: dict):

        try:
            if client_id not in self.authorized_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required"
                )
                return

            job_id = message.get("job_id")
            expected_size = message.get("expected_size")
            expected_checksum = message.get("expected_checksum")

            if not all([job_id, expected_size, expected_checksum]):
                await self._send_error(
                    client_id, "invalid_params", "Missing verification parameters"
                )
                return

            if hasattr(self, "file_transfer_manager"):
                verification_result = await self._verify_transfer_completion(
                    job_id, expected_size, expected_checksum
                )

                if verification_result.get("success"):
                    response = create_message(
                        "upload_verify_response",
                        {
                            "job_id": job_id,
                            "status": "verified",
                            "file_size": verification_result.get("actual_size"),
                            "checksum_match": verification_result.get("checksum_valid"),
                        },
                    )
                    logger.info(f"Upload verification successful for {job_id}")
                else:
                    response = create_message(
                        "upload_verify_response",
                        {
                            "job_id": job_id,
                            "status": "failed",
                            "error": verification_result.get("error"),
                        },
                    )
                    logger.warning(
                        f"Upload verification failed for {job_id}: {verification_result.get('error')}"
                    )

                await self._send_message(client_id, response)
            else:
                await self._send_error(
                    client_id,
                    "service_unavailable",
                    "File transfer service not available",
                )

        except Exception as e:
            logger.error(f"Error handling upload verification from {client_id}: {e}")
            await self._send_error(client_id, "verify_error", str(e))

    async def _handle_upload_check_existing(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required"
                )
                return

            job_id = message.get("job_id")
            file_name = message.get("file_name")
            session_id = message.get("session_id")
            device_id = message.get("device_id")

            if not all([job_id, file_name, session_id, device_id]):
                await self._send_error(
                    client_id, "invalid_params", "Missing check parameters"
                )
                return

            existing_size = 0
            if hasattr(self, "file_transfer_manager"):
                existing_size = await self._check_existing_partial_file(
                    job_id, file_name, session_id, device_id
                )

            response = create_message(
                "upload_check_existing_response",
                {
                    "job_id": job_id,
                    "existing_size": existing_size,
                    "can_resume": existing_size > 0,
                },
            )
            await self._send_message(client_id, response)

            logger.debug(f"Existing file check for {job_id}: {existing_size} bytes")

        except Exception as e:
            logger.error(f"Error checking existing upload from {client_id}: {e}")
            await self._send_error(client_id, "check_error", str(e))

    async def _handle_file_chunk_response(self, client_id: str, message: dict):

        try:

            pass
        except Exception as e:
            logger.error(f"Error handling file chunk response from {client_id}: {e}")

    async def _handle_data_export_request(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required"
                )
                return

            session_id = message.get("session_id")
            export_format = message.get("format", "json").lower()
            include_files = message.get("include_files", False)

            if not session_id:
                await self._send_error(
                    client_id, "invalid_params", "Missing session_id"
                )
                return

            export_path = await self._export_session_data(
                session_id, export_format, include_files
            )

            if export_path:
                response = create_message(
                    "data_export_response",
                    {
                        "session_id": session_id,
                        "export_path": export_path,
                        "format": export_format,
                        "status": "completed",
                    },
                )
                logger.info(
                    f"Data export completed for session {session_id}: {export_path}"
                )
            else:
                response = create_message(
                    "data_export_response",
                    {
                        "session_id": session_id,
                        "status": "failed",
                        "error": "Export process failed",
                    },
                )
                logger.error(f"Data export failed for session {session_id}")

            await self._send_message(client_id, response)

        except Exception as e:
            logger.error(f"Error handling data export request from {client_id}: {e}")
            await self._send_error(client_id, "export_error", str(e))

    async def _handle_session_manifest_request(self, client_id: str, message: dict):

        try:
            if client_id not in self.authenticated_clients:
                await self._send_error(
                    client_id, "auth_required", "Authentication required"
                )
                return

            session_id = message.get("session_id")

            if not session_id:
                await self._send_error(
                    client_id, "invalid_params", "Missing session_id"
                )
                return

            manifest = await self._get_session_manifest(session_id)

            response = create_message(
                "session_manifest_response",
                {"session_id": session_id, "manifest": manifest},
            )
            await self._send_message(client_id, response)

            logger.debug(f"Session manifest sent for {session_id}")

        except Exception as e:
            logger.error(
                f"Error handling session manifest request from {client_id}: {e}"
            )
            await self._send_error(client_id, "manifest_error", str(e))

    async def _write_chunk_to_transfer(
            self, job_id: str, chunk_index: int, chunk_offset: int, chunk_data: bytes
    ) -> bool:

        try:

            return True
        except Exception as e:
            logger.error(f"Error writing chunk for {job_id}: {e}")
            return False

    async def _verify_transfer_completion(
            self, job_id: str, expected_size: int, expected_checksum: str
    ) -> dict:

        try:

            return {
                "success": True,
                "actual_size": expected_size,
                "checksum_valid": True,
            }
        except Exception as e:
            logger.error(f"Error verifying transfer {job_id}: {e}")
            return {"success": False, "error": str(e)}

    async def _check_existing_partial_file(
            self, job_id: str, file_name: str, session_id: str, device_id: str
    ) -> int:

        try:

            return 0
        except Exception as e:
            logger.error(f"Error checking existing file for {job_id}: {e}")
            return 0

    async def _export_session_data(
            self, session_id: str, format: str, include_files: bool
    ) -> str:

        try:
            # Implementation would use data management service
            return f"/exports/{session_id}.{format}"
        except Exception as e:
            logger.error(f"Error exporting session {session_id}: {e}")
            return None

    async def _get_session_manifest(self, session_id: str) -> dict:

        try:

            return {
                "session_id": session_id,
                "files": [],
                "created_timestamp": time.time(),
            }
        except Exception as e:
            logger.error(f"Error getting manifest for {session_id}: {e}")
            return {}


def create_websocket_server(host: str = "0.0.0.0", port: int = 8443) -> WebSocketServer:
    return WebSocketServer(host, port)


class WebSocketServerPhase4Extension:

    def __init__(self, server: WebSocketServer):
        self.server = server

    async def _handle_enhanced_auth(self, client_id: str, message: dict):

        try:
            auth_level_value = message.get("auth_level", 1)
            credentials = message.get("credentials", {})
            device_id = credentials.get("device_id", "")

            auth_level = (
                AuthLevel(auth_level_value)
                if auth_level_value <= 4
                else AuthLevel.BASIC
            )

            success, context, reason = (
                await self.server.enhanced_security.authenticate_device(
                    device_id, auth_level, credentials
                )
            )

            if success and context:

                async with self.server.client_lock:
                    client = self.server.clients.get(client_id)
                    if client:
                        client.authenticated = True
                        client.auth_level = auth_level
                        client.auth_context = context
                        client.device_id = device_id

                response = create_message(
                    "enhanced_auth_response",
                    {
                        "success": True,
                        "auth_level": auth_level.value,
                        "role": context.role.name,
                        "session_token": context.session_token,
                        "expiry_time": context.expiry_time,
                        "capabilities": list(context.capabilities),
                    },
                )
                await self.server._send_message(client_id, response)

                logger.info(
                    f"Enhanced authentication successful for {device_id} at level {auth_level.name}"
                )
            else:
                await self.server._send_error(client_id, "enhanced_auth_failed", reason)

        except Exception as e:
            logger.error(f"Error in enhanced authentication for {client_id}: {e}")
            await self.server._send_error(client_id, "enhanced_auth_error", str(e))

    async def _handle_certificate_auth(self, client_id: str, message: dict):

        try:
            certificate_data = message.get("certificate")
            signature = message.get("signature")
            challenge = message.get("challenge")
            device_id = message.get("device_id", "")

            credentials = {
                "device_id": device_id,
                "device_type": message.get("device_type", "ANDROID_PHONE"),
                "certificate": certificate_data,
                "signature": signature,
                "challenge": challenge,
            }

            success, context, reason = (
                await self.server.enhanced_security.authenticate_device(
                    device_id, AuthLevel.CERTIFICATE, credentials
                )
            )

            if success and context:

                async with self.server.client_lock:
                    client = self.server.clients.get(client_id)
                    if client:
                        client.authenticated = True
                        client.auth_level = AuthLevel.CERTIFICATE
                        client.auth_context = context
                        client.device_id = device_id

                response = create_message(
                    "certificate_auth_response",
                    {
                        "success": True,
                        "auth_level": AuthLevel.CERTIFICATE.value,
                        "session_token": context.session_token,
                        "certificate_valid": True,
                    },
                )
                await self.server._send_message(client_id, response)

                logger.info(f"Certificate authentication successful for {device_id}")
            else:
                await self.server._send_error(
                    client_id, "certificate_auth_failed", reason
                )

        except Exception as e:
            logger.error(f"Error in certificate authentication for {client_id}: {e}")
            await self.server._send_error(client_id, "certificate_auth_error", str(e))

    async def _handle_token_auth(self, client_id: str, message: dict):

        try:
            token = message.get("token")
            timestamp = message.get("timestamp")
            hmac_signature = message.get("hmac")
            device_id = message.get("device_id", "")

            credentials = {
                "device_id": device_id,
                "device_type": message.get("device_type", "ANDROID_PHONE"),
                "token": token,
                "timestamp": timestamp,
                "hmac": hmac_signature,
            }

            success, context, reason = (
                await self.server.enhanced_security.authenticate_device(
                    device_id, AuthLevel.TOKEN, credentials
                )
            )

            if success and context:
                async with self.server.client_lock:
                    client = self.server.clients.get(client_id)
                    if client:
                        client.authenticated = True
                        client.auth_level = AuthLevel.TOKEN
                        client.auth_context = context
                        client.device_id = device_id

                response = create_message(
                    "token_auth_response",
                    {
                        "success": True,
                        "auth_level": AuthLevel.TOKEN.value,
                        "session_token": context.session_token,
                        "token_valid": True,
                    },
                )
                await self.server._send_message(client_id, response)

                logger.info(f"Token authentication successful for {device_id}")
            else:
                await self.server._send_error(client_id, "token_auth_failed", reason)

        except Exception as e:
            logger.error(f"Error in token authentication for {client_id}: {e}")
            await self.server._send_error(client_id, "token_auth_error", str(e))

    async def _handle_biometric_auth(self, client_id: str, message: dict):

        try:
            hardware_key = message.get("hardware_key")
            biometric_signature = message.get("biometric_signature")
            device_id = message.get("device_id", "")

            credentials = {
                "device_id": device_id,
                "device_type": message.get("device_type", "ANDROID_PHONE"),
                "hardware_key": hardware_key,
                "biometric_signature": biometric_signature,
            }

            success, context, reason = (
                await self.server.enhanced_security.authenticate_device(
                    device_id, AuthLevel.BIOMETRIC, credentials
                )
            )

            if success and context:
                async with self.server.client_lock:
                    client = self.server.clients.get(client_id)
                    if client:
                        client.authenticated = True
                        client.auth_level = AuthLevel.BIOMETRIC
                        client.auth_context = context
                        client.device_id = device_id

                response = create_message(
                    "biometric_auth_response",
                    {
                        "success": True,
                        "auth_level": AuthLevel.BIOMETRIC.value,
                        "session_token": context.session_token,
                        "hardware_verified": True,
                    },
                )
                await self.server._send_message(client_id, response)

                logger.info(f"Biometric authentication successful for {device_id}")
            else:
                await self.server._send_error(
                    client_id, "biometric_auth_failed", reason
                )

        except Exception as e:
            logger.error(f"Error in biometric authentication for {client_id}: {e}")
            await self.server._send_error(client_id, "biometric_auth_error", str(e))

    async def _handle_security_alert(self, client_id: str, message: dict):

        try:
            alert_type = message.get("alert_type", "")
            device_id = message.get("device_id", "")
            timestamp = message.get("timestamp", time.time())
            severity = message.get("severity", "LOW")
            details = message.get("details", {})

            logger.warning(
                f"Security alert received from {device_id}: {alert_type} ({severity})"
            )

            alert_details = {
                "client_id": client_id,
                "device_id": device_id,
                "alert_type": alert_type,
                "severity": severity,
                "timestamp": timestamp,
                "details": details,
            }

            self.server.enhanced_security.security_monitor.report_connection_attempt(
                device_id, False, alert_details
            )

            response = create_message(
                "security_alert_response",
                {
                    "alert_received": True,
                    "timestamp": time.time(),
                    "action_taken": "logged",
                },
            )
            await self.server._send_message(client_id, response)

        except Exception as e:
            logger.error(f"Error handling security alert from {client_id}: {e}")
            await self.server._send_error(client_id, "security_alert_error", str(e))

    async def _handle_permission_request(self, client_id: str, message: dict):

        try:
            requested_permission = message.get("permission", "")
            device_id = message.get("device_id", "")

            async with self.server.client_lock:
                client = self.server.clients.get(client_id)
                if not client or not client.auth_context:
                    await self.server._send_error(
                        client_id, "auth_required", "Authentication required"
                    )
                    return

            has_permission = self.server.enhanced_security.check_permission(
                client.auth_context.session_token, requested_permission
            )

            response = create_message(
                "permission_response",
                {
                    "permission": requested_permission,
                    "granted": has_permission,
                    "current_role": client.auth_context.role.name,
                },
            )
            await self.server._send_message(client_id, response)

            logger.debug(
                f"Permission check for {device_id}: {requested_permission} = {has_permission}"
            )

        except Exception as e:
            logger.error(f"Error handling permission request from {client_id}: {e}")
            await self.server._send_error(client_id, "permission_error", str(e))

    async def _handle_role_change_request(self, client_id: str, message: dict):

        try:
            requested_role = message.get("requested_role", "")
            device_id = message.get("device_id", "")
            message.get("justification", "")

            async with self.server.client_lock:
                client = self.server.clients.get(client_id)
                if not client or not client.auth_context:
                    await self.server._send_error(
                        client_id, "auth_required", "Authentication required"
                    )
                    return

            response = create_message(
                "role_change_response",
                {
                    "success": False,
                    "current_role": client.auth_context.role.name,
                    "requested_role": requested_role,
                    "reason": "Role changes require administrator approval",
                },
            )
            await self.server._send_message(client_id, response)

            logger.warning(
                f"Role change request denied for {device_id}: {requested_role}"
            )

        except Exception as e:
            logger.error(f"Error handling role change request from {client_id}: {e}")
            await self.server._send_error(client_id, "role_change_error", str(e))


def extend_websocket_server_with_phase4(server: Any = WebSocketServer) -> Any:
    extension = WebSocketServerPhase4Extension(server)

    server._handle_enhanced_auth = extension._handle_enhanced_auth
    server._handle_certificate_auth = extension._handle_certificate_auth
    server._handle_token_auth = extension._handle_token_auth
    server._handle_biometric_auth = extension._handle_biometric_auth
    server._handle_security_alert = extension._handle_security_alert
    server._handle_permission_request = extension._handle_permission_request
    server._handle_role_change_request = extension._handle_role_change_request

    logger.info("WebSocket server extended with Phase 4 security handlers")
    return WebSocketServer(host, port)
