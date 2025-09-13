"""
Network Server for IRCamera PC Controller

Manages JSON-based communication with Android devices using formal protocol
definition. Implements FR2: Synchronised Multi-Modal Recording and FR7:
Device Synchronisation with enhanced security and discovery features.
"""

import asyncio
import json
import ssl
import time
import uuid
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Callable, Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..core.config import config
from ..core.gsr_receiver import GSRReceiver
from .discovery import NetworkDiscoveryService
from .messaging import MessageCallback, MessagePriority, ReliableMessageService
from .protocol import (
    ValidationError,
    create_message,
    get_protocol_manager,
    validate_message,
)
from .security import SecurityManager


class DeviceState(Enum):
    """Device connection states."""

    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    RECORDING = "recording"
    ERROR = "error"


class MessageType(Enum):
    """Message types for device communication."""

    # Device lifecycle
    DEVICE_REGISTER = "device_register"
    DEVICE_HEARTBEAT = "device_heartbeat"
    DEVICE_STATUS = "device_status"

    # Session control
    SESSION_START = "session_start"
    SESSION_STOP = "session_stop"
    RECORDING_START = "recording_start"
    RECORDING_STOP = "recording_stop"

    # Synchronization
    SYNC_MARK = "sync_mark"
    SYNC_FLASH = "sync_flash"

    # File transfer
    FILE_TRANSFER_REQUEST = "file_transfer_request"
    FILE_TRANSFER_COMPLETE = "file_transfer_complete"

    # GSR data streaming
    GSR_STREAM_REGISTER = "stream_registration"
    GSR_DATA = "gsr_data"
    GSR_QUALITY_METRICS = "quality_metrics"
    TIME_SYNC_REQUEST = "time_sync_request"
    TIME_SYNC_RESPONSE = "time_sync_response"

    # Responses
    ACK = "ack"
    ERROR = "error"


@dataclass
class DeviceInfo:
    """Information about a connected device."""

    device_id: str
    device_type: str
    capabilities: List[str]
    ip_address: str
    port: int
    state: str = DeviceState.CONNECTED.value
    last_heartbeat: Optional[str] = None
    battery_level: Optional[int] = None
    is_gsr_leader: bool = False
    gsr_mode: str = "local"

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return asdict(self)


class NetworkServer:
    """
    Enhanced network server for device communication and coordination.

    Implements device communication requirements with enhanced security:
    - JSON/TCP/IP command protocol for Android devices with TLS support
    - Automatic device discovery via mDNS/Zeroconf
    - Device registration and heartbeat monitoring
    - Reliable message delivery with acknowledgments
    - Synchronised start/stop commands across all devices
    - Sync signal broadcasting (flash cues)
    - Device fault detection and recovery
    - TLS encryption and device authentication
    """

    def __init__(self):
        """Initialize enhanced network server."""
        self._server: Optional[asyncio.Server] = None
        self._secure_server: Optional[asyncio.Server] = None
        self._clients: Dict[str, asyncio.StreamWriter] = {}
        self._devices: Dict[str, DeviceInfo] = {}
        self._message_handlers: Dict[str, Callable] = {}
        self._heartbeat_task: Optional[asyncio.Task] = None
        self._is_running = False

        # Enhanced networking services
        self._security_manager = SecurityManager()
        self._discovery_service = NetworkDiscoveryService()
        self._messaging_service = ReliableMessageService()

        # GSR data receiver for hub-spoke communication
        self._gsr_receiver = GSRReceiver(config.get("gsr_receiver", {}))

        # Protocol manager
        self._protocol = get_protocol_manager()

        # Configuration
        transport_config = self._protocol.get_transport_config()
        self._host = config.get(
            "network.server_host", transport_config.get("host", "127.0.0.1")
        )
        self._port = config.get(
            "network.server_port", transport_config.get("port", 8080)
        )
        self._secure_port = config.get("network.secure_port", self._port + 1)
        self._max_connections = config.get("network.max_connections", 8)

        connection_config = transport_config.get("connection", {})
        self._heartbeat_interval = config.get(
            "network.heartbeat_interval",
            connection_config.get("heartbeat_interval_s", 5),
        )
        self._connection_timeout = config.get(
            "network.connection_timeout",
            connection_config.get("timeout_s", 30),
        )

        # Get max message size from protocol
        framing = transport_config.get("message_framing", {})
        self._max_message_size = framing.get(
            "max_message_size", 1024 * 1024
        )  # 1MB default

        # Event callbacks
        self._on_device_connected: Optional[Callable] = None
        self._on_device_disconnected: Optional[Callable] = None
        self._on_device_status_update: Optional[Callable] = None

        self._setup_message_handlers()
        self._setup_enhanced_services()
        protocol_version = self._protocol.get_protocol_info()["version"]
        logger.info(
            f"Enhanced Network Server initialized with protocol {protocol_version}"
        )

    def _setup_enhanced_services(self) -> None:
        """Set up enhanced networking services."""
        # Configure messaging service transport
        self._messaging_service.set_transport(self._send_message_to_device)

        # Register discovery listener
        self._discovery_service.add_discovery_listener(self._on_device_discovered)

        # Register reliable message handlers
        self._messaging_service.register_message_handler(
            "session_start", self._handle_reliable_session_start
        )
        self._messaging_service.register_message_handler(
            "session_stop", self._handle_reliable_session_stop
        )
        self._messaging_service.register_message_handler(
            "sync_flash", self._handle_reliable_sync_flash
        )

    def _setup_message_handlers(self) -> None:
        """Set up message handlers for different message types."""
        self._message_handlers = {
            "device_register": self._handle_device_register,
            "device_heartbeat": self._handle_device_heartbeat,
            "device_status": self._handle_device_status,
            "file_transfer_complete": self._handle_file_transfer_complete,
            "time_sync_request": self._handle_time_sync_request,
            "gsr_data_batch": self._handle_gsr_data_batch,
            "gsr_leader_election": self._handle_gsr_leader_election,
            # Enhanced GSR streaming handlers
            "stream_registration": self._handle_gsr_stream_registration,
            "gsr_data": self._handle_gsr_data_stream,
            "quality_metrics": self._handle_gsr_quality_metrics,
            "heartbeat": self._handle_gsr_heartbeat,
            "stream_end": self._handle_gsr_stream_end,
            # Enhanced message types
            "device_auth": self._handle_device_auth,
            "message_ack": self._handle_message_ack,
            "message_nack": self._handle_message_nack,
        }

    async def start(self) -> bool:
        """Start the enhanced network server with security and discovery."""
        if self._is_running:
            logger.warning("Network server is already running")
            return True

        try:
            logger.info("Starting enhanced network server...")

            # Initialize security manager
            if not self._security_manager.initialize():
                logger.error("Failed to initialize security manager")
                return False

            # Initialize messaging service
            if not await self._messaging_service.initialize():
                logger.error("Failed to initialize messaging service")
                return False

            # Start discovery service
            if not await self._discovery_service.start_discovery():
                logger.warning(
                    "Discovery service failed to start - continuing without discovery"
                )

            # Start GSR receiver for hub-spoke communication
            await self._gsr_receiver.start()
            logger.info("GSR receiver started for hub-spoke communication")

            # Start plaintext server
            self._server = await asyncio.start_server(
                self._handle_client,
                self._host,
                self._port,
                limit=2**16,  # 64KB buffer
            )

            # Start secure server with TLS
            ssl_context = self._security_manager.create_ssl_context(
                for_client_auth=True
            )
            self._secure_server = await asyncio.start_server(
                self._handle_secure_client,
                self._host,
                self._secure_port,
                ssl=ssl_context,
                limit=2**16,  # 64KB buffer
            )

            # Start heartbeat monitoring
            self._heartbeat_task = asyncio.create_task(self._monitor_heartbeats())

            self._is_running = True

            addr = self._server.sockets[0].getsockname()
            secure_addr = self._secure_server.sockets[0].getsockname()
            logger.info(
                f"Network server started on {addr[0]}:{addr[1]} (plaintext) and {secure_addr[0]}:{secure_addr[1]} (TLS)"
            )
            logger.info(
                "Enhanced networking features: TLS encryption, mDNS discovery, "
                "reliable messaging"
            )

            return True

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to start network server: {e}")
            await self.stop()
            return False

    async def stop(self) -> None:
        """Stop the enhanced network server."""
        if not self._is_running:
            return

        logger.info("Stopping enhanced network server...")
        self._is_running = False

        # Stop enhanced services
        await self._messaging_service.shutdown()
        await self._discovery_service.stop_discovery()

        # Stop GSR receiver
        await self._gsr_receiver.stop()
        logger.info("GSR receiver stopped")

        # Cancel heartbeat monitoring
        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            try:
                await self._heartbeat_task
            except asyncio.CancelledError:
                pass

        # Close all client connections
        for client in self._clients.values():
            client.close()
            await client.wait_closed()

        self._clients.clear()

        # Close servers
        if self._server:
            self._server.close()
            await self._server.wait_closed()

        if self._secure_server:
            self._secure_server.close()
            await self._secure_server.wait_closed()

        logger.info("Enhanced network server stopped")
        self._devices.clear()

        # Close server
        if self._server:
            self._server.close()
            await self._server.wait_closed()

        logger.info("Network server stopped")

    async def _handle_client(
        self,
        reader: asyncio.StreamReader,
        writer: asyncio.StreamWriter,
        is_secure: bool = False,
    ) -> None:
        """Handle new client connection."""
        addr = writer.get_extra_info("peername")
        connection_type = "secure" if is_secure else "plaintext"
        logger.info(f"Client connected from {addr} ({connection_type})")

        try:
            while True:
                # Read message length (4 bytes)
                length_data = await reader.readexactly(4)
                message_length = int.from_bytes(length_data, "big")

                if message_length > self._max_message_size:
                    logger.warning(
                        f"Message too large from {addr}:" "{message_length} bytes"
                    )
                    break

                # Read message data
                message_data = await reader.readexactly(message_length)

                try:
                    message = json.loads(message_data.decode("utf-8"))
                    await self._process_message(message, writer)

                except json.JSONDecodeError as e:
                    logger.warning(f"Invalid JSON from {addr}: {e}")
                    await self._send_error(writer, "Invalid JSON format")

        except asyncio.IncompleteReadError:
            logger.debug(f"Client {addr} disconnected")
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error handling client {addr}: {e}")
        finally:
            # Clean up client
            device_id = None
            for did, client in self._clients.items():
                if client == writer:
                    device_id = did
                    break

            if device_id:
                await self._handle_device_disconnect(device_id)

            writer.close()
            await writer.wait_closed()

    async def _process_message(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> None:
        """Process incoming message from device using protocol validation."""
        try:
            # Validate message against protocol
            if not validate_message(message, strict=False):
                await self._send_error(writer, "Message validation failed")
                return

            message_type = message.get("message_type")
            message_id = message.get("message_id", str(uuid.uuid4()))

            if not message_type:
                await self._send_error(writer, "Missing message_type field", message_id)
                return

            # Handle message using protocol-aware handlers
            if message_type in self._message_handlers:
                response = await self._message_handlers[message_type](message, writer)
                if response:
                    response["message_id"] = message_id
                    await self._send_message(writer, response)
            else:
                logger.warning(f"Unknown message type: {message_type}")
                await self._send_error(
                    writer, f"Unknown message type: {message_type}", message_id
                )

        except ValidationError as e:
            logger.warning(f"Protocol validation error: {e}")
            await self._send_error(writer, f"Protocol validation error: {e}")
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error processing message: {e}")
            await self._send_error(writer, str(e))

    async def _handle_device_register(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle device registration."""
        try:
            device_id = message.get("device_id")
            device_type = message.get("device_type", "unknown")
            capabilities = message.get("capabilities", [])

            if not device_id:
                return {
                    "type": MessageType.ERROR.value,
                    "error": "Missing device_id",
                }

            # Check connection limit
            if len(self._devices) >= self._max_connections:
                return {
                    "type": MessageType.ERROR.value,
                    "error": "Maximum connections exceeded",
                }

            # Get client address
            addr = writer.get_extra_info("peername")

            # Create device info
            device_info = DeviceInfo(
                device_id=device_id,
                device_type=device_type,
                capabilities=capabilities,
                ip_address=addr[0],
                port=addr[1],
                last_heartbeat=datetime.now(timezone.utc).isoformat(),
            )

            # Determine GSR leader
            if "gsr_sensor" in capabilities and not any(
                d.is_gsr_leader for d in self._devices.values()
            ):
                device_info.is_gsr_leader = True
                device_info.gsr_mode = config.get("gsr.default_mode", "local")
                logger.info(f"Device {device_id} elected as GSR leader")

            # Store device and client
            self._devices[device_id] = device_info
            self._clients[device_id] = writer

            logger.info(
                f"Device registered: {device_id} ({device_type})"
                "with capabilities: {capabilities}"
            )

            # Notify callback
            if self._on_device_connected:
                self._on_device_connected(device_info)

            return create_message("ack", ack_for="device_register", status="success")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error handling device registration: {e}")
            return create_message(
                "error",
                error_code="RESOURCE_UNAVAILABLE",
                error_message=str(e),
            )

    async def _handle_device_heartbeat(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle device heartbeat using protocol format."""
        device_id = message.get("device_id")

        if device_id in self._devices:
            self._devices[device_id].last_heartbeat = datetime.now(
                timezone.utc
            ).isoformat()

            # Update device status if provided
            if "battery_level" in message:
                self._devices[device_id].battery_level = message["battery_level"]

            logger.debug(f"Heartbeat from {device_id}")
            return create_message("ack", ack_for="device_heartbeat", status="success")

        return create_message(
            "error",
            error_code="DEVICE_BUSY",
            error_message="Device" "not registered",
        )

    async def _handle_device_status(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle device status update using protocol format."""
        device_id = message.get("device_id")

        if device_id in self._devices:
            device = self._devices[device_id]

            # Update status fields
            if "status" in message:
                device.state = message["status"]
            if "battery_level" in message:
                device.battery_level = message["battery_level"]

            logger.debug(f"Status update from {device_id}: {message}")

            # Notify callback
            if self._on_device_status_update:
                self._on_device_status_update(device)

            return create_message("ack", ack_for="device_status", status="success")

        return create_message(
            "error",
            error_code="DEVICE_BUSY",
            error_message="Device" "not registered",
        )

    async def _handle_file_transfer_complete(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle file transfer completion notification"
        "using protocol format."""
        device_id = message.get("device_id")
        transfer_id = message.get("transfer_id")
        status = message.get("status")

        logger.info(f"File transfer {status} from {device_id}: {transfer_id}")

        return create_message("ack", ack_for="file_transfer_complete", status="success")

    async def _handle_time_sync_request(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle time synchronization request using protocol format."""
        message.get("device_id")
        client_timestamp = message.get("client_timestamp")

        server_timestamp = datetime.now(timezone.utc).isoformat()

        return create_message(
            "time_sync_response",
            server_timestamp=server_timestamp,
            client_timestamp=client_timestamp,
            processing_delay_ms=1.0,
        )  # Minimal processing delay

    async def _handle_gsr_data_batch(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle GSR data batch using protocol format."""
        device_id = message.get("device_id")
        message.get("session_id")
        data_points = message.get("data_points", [])

        logger.debug(
            f"Received GSR data batch from {device_id}: {len(data_points)} points"
        )

        # Forward to enhanced GSR data ingestion system
        try:
            from ..data import get_data_aggregator

            # Get the data aggregator instance for real-time processing
            aggregator = get_data_aggregator()

            # Process each GSR data point with enhanced metadata
            for point in data_points:
                enhanced_point = {
                    "device_id": device_id,
                    "timestamp_ns": point.get("timestamp_ns"),
                    "gsr_raw": point.get("gsr_raw"),
                    "gsr_microsiemens": point.get("gsr_microsiemens"),
                    "ppg_raw": point.get("ppg_raw"),
                    "ppg_value": point.get("ppg_value"),
                    "quality_score": point.get("quality_score", 100.0),
                    "sync_marker": point.get("sync_marker", False),
                    "session_metadata": {
                        "network_latency_ms": self._calculate_network_latency(
                            device_id
                        ),
                        "reception_timestamp_ns": time.time_ns(),
                        "data_integrity_hash": self._calculate_data_hash(point),
                    },
                }

                # Add to aggregator with device synchronization
                await aggregator.add_gsr_data_point(enhanced_point)

            # Update real-time visualization if available
            self._update_realtime_gsr_visualization(device_id, data_points)

            logger.info(
                f"Successfully processed {len(data_points)} GSR points from {device_id}"
            )

        except ImportError:
            logger.warning(
                "Data aggregator not available, trying fallback GSR ingestor"
            )

            # Fallback to GSR ingestor for processing
            try:
                from ..core.gsr_ingestor import GSRIngestor, GSRMode, GSRSample

                # Convert data points to GSR samples
                gsr_samples = []
                for point in data_points:
                    sample = GSRSample(
                        timestamp=point.get("timestamp", time.time()),
                        value=point.get("value", 0.0),
                        quality=point.get("quality", 100),
                        device_id=device_id,
                    )
                    gsr_samples.append(sample)

                # Get or create GSR ingestor instance
                if not hasattr(self, "_gsr_ingestor"):
                    self._gsr_ingestor = GSRIngestor()

                # Process the data batch
                await self._gsr_ingestor.process_data_batch(
                    session_id=message.get("session_id"),
                    device_id=device_id,
                    samples=gsr_samples,
                )

                logger.debug(f"Forwarded {len(gsr_samples)} GSR samples to ingestor")

            except Exception as e:
                logger.warning(f"GSR ingestor also failed, storing data to buffer: {e}")
                # Final fallback to simple storage
                self._buffer_gsr_data(device_id, data_points)

        except Exception as e:
            logger.error(f"Failed to process GSR data from {device_id}: {e}")
            return create_message(
                "ack", ack_for="gsr_data_batch", status="error", error=str(e)
            )

        return create_message("ack", ack_for="gsr_data_batch", status="success")

    async def _handle_gsr_leader_election(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle GSR leader election using protocol format."""
        device_id = message.get("device_id")
        election_type = message.get("election_type")
        priority_score = message.get("priority_score", 0)

        logger.info(
            f"GSR leader election from {device_id}: {election_type}"
            "(score: {priority_score})"
        )

        if election_type == "candidate" and device_id in self._devices:
            # Simple leader election - highest priority score wins
            current_leader = None
            for did, device in self._devices.items():
                if device.is_gsr_leader:
                    current_leader = device
                    break

            if not current_leader or priority_score > 0.8:  # High priority threshold
                # Elect new leader
                if current_leader:
                    current_leader.is_gsr_leader = False

                self._devices[device_id].is_gsr_leader = True
                logger.info(f"New GSR leader elected: {device_id}")

                return create_message(
                    "gsr_leader_election",
                    device_id="pc_controller",
                    election_type="leader",
                    priority_score=1.0,
                )

        return create_message("ack", ack_for="gsr_leader_election", status="success")

    async def _handle_device_disconnect(self, device_id: str) -> None:
        """Handle device disconnection."""
        if device_id in self._devices:
            device_info = self._devices[device_id]
            device_info.state = DeviceState.DISCONNECTED.value

            # Remove from active connections
            self._clients.pop(device_id, None)

            logger.info(f"Device disconnected: {device_id}")

            # Notify callback
            if self._on_device_disconnected:
                self._on_device_disconnected(device_info)

            # Handle GSR leader disconnection
            if device_info.is_gsr_leader:
                await self._handle_gsr_leader_disconnect(device_id)

    async def _handle_gsr_leader_disconnect(self, device_id: str) -> None:
        """Handle GSR leader disconnection by electing new leader."""
        logger.warning(f"GSR leader {device_id} disconnected")

        # Find new GSR leader
        for did, device in self._devices.items():
            if (
                did != device_id
                and device.state == DeviceState.CONNECTED.value
                and "gsr_sensor" in device.capabilities
            ):

                device.is_gsr_leader = True
                device.gsr_mode = config.get("gsr.default_mode", "local")

                # Notify new leader
                if did in self._clients:
                    await self._send_message(
                        self._clients[did],
                        {
                            "type": "gsr_leader_assignment",
                            "is_leader": True,
                            "mode": device.gsr_mode,
                        },
                    )

                logger.info(f"New GSR leader elected: {did}")
                break

    async def _monitor_heartbeats(self) -> None:
        """Monitor device heartbeats and handle timeouts."""
        while self._is_running:
            try:
                current_time = datetime.now(timezone.utc)

                for device_id, device in list(self._devices.items()):
                    if not device.last_heartbeat:
                        continue

                    last_heartbeat = datetime.fromisoformat(
                        device.last_heartbeat.replace("Z", "+00:00")
                    )
                    time_since_heartbeat = (
                        current_time - last_heartbeat
                    ).total_seconds()

                    if time_since_heartbeat > self._connection_timeout:
                        logger.warning(f"Device {device_id} heartbeat timeout")
                        await self._handle_device_disconnect(device_id)

                await asyncio.sleep(self._heartbeat_interval)

            except asyncio.CancelledError:
                break
            except (OSError, ValueError, RuntimeError) as e:
                logger.error(f"Error in heartbeat monitoring: {e}")
                await asyncio.sleep(self._heartbeat_interval)

    async def broadcast_command(
        self,
        command: Dict[str, Any],
        target_devices: Optional[List[str]] = None,
    ) -> Dict[str, bool]:
        """
        Broadcast command to devices.

        Args:
            command: Command to broadcast
            target_devices: List of device IDs to target. If None, broadcasts to all.

        Returns:
            Dictionary mapping device_id to success status
        """
        results = {}

        devices_to_target = target_devices or list(self._clients.keys())

        for device_id in devices_to_target:
            if device_id in self._clients:
                try:
                    await self._send_message(self._clients[device_id], command)
                    results[device_id] = True
                    logger.debug(
                        f"Command sent to {device_id}: " "{command.get('message_type')}"
                    )
                except (OSError, ValueError, RuntimeError) as e:
                    logger.error(f"Failed to send command to {device_id}: {e}")
                    results[device_id] = False
            else:
                results[device_id] = False

        return results

    async def start_recording_session(
        self, session_id: str, session_name: Optional[str] = None
    ) -> Dict[str, bool]:
        """Start recording session on all devices using protocol format."""
        command = create_message(
            "session_start",
            session_id=session_id,
            session_name=session_name or f"Session_{session_id[:8]}",
        )

        logger.info(f"Starting recording session {session_id} on all devices")
        return await self.broadcast_command(command)

    async def stop_recording_session(self, session_id: str) -> Dict[str, bool]:
        """Stop recording session on all devices using protocol format."""
        command = create_message("session_stop", session_id=session_id)

        logger.info(f"Stopping recording session {session_id} on all devices")
        return await self.broadcast_command(command)

    async def send_sync_flash(self, duration_ms: int = 100) -> Dict[str, bool]:
        """Send sync flash command to all devices using protocol format."""
        command = create_message(
            "sync_flash", duration_ms=duration_ms, intensity=1.0, color="white"
        )

        logger.info("Sending sync flash to all devices")
        return await self.broadcast_command(command)

    async def send_sync_mark(
        self, mark_type: str, metadata: Dict[str, Any] = None
    ) -> Dict[str, bool]:
        """Send sync mark to all devices using protocol format."""
        command = create_message(
            "sync_mark",
            mark_type=mark_type,
            mark_id=str(uuid.uuid4()),
            metadata=metadata or {},
        )

        logger.info(f"Sending sync mark '{mark_type}' to all devices")
        return await self.broadcast_command(command)

    async def _send_message(
        self, writer: asyncio.StreamWriter, message: Dict[str, Any]
    ) -> None:
        """Send JSON message to client."""
        try:
            message_data = json.dumps(message).encode("utf-8")
            length_data = len(message_data).to_bytes(4, "big")

            writer.write(length_data + message_data)
            await writer.drain()

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to send message: {e}")
            raise

    async def _send_error(
        self,
        writer: asyncio.StreamWriter,
        error_message: str,
        message_id: Optional[str] = None,
    ) -> None:
        """Send error response to client using protocol format."""
        error_response = create_message(
            "error", error_code="INVALID_MESSAGE", error_message=error_message
        )

        if message_id:
            error_response["message_id"] = message_id

        await self._send_message(writer, error_response)

    # Event callback setters
    def set_device_connected_callback(
        self, callback: Callable[[DeviceInfo], None]
    ) -> None:
        """Set callback for device connection events."""
        self._on_device_connected = callback

    def set_device_disconnected_callback(
        self, callback: Callable[[DeviceInfo], None]
    ) -> None:
        """Set callback for device disconnection events."""
        self._on_device_disconnected = callback

    def set_device_status_update_callback(
        self, callback: Callable[[DeviceInfo], None]
    ) -> None:
        """Set callback for device status updates."""
        self._on_device_status_update = callback

    # Property accessors
    def get_connected_devices(self) -> Dict[str, DeviceInfo]:
        """Get all connected devices."""
        return {
            did: device
            for did, device in self._devices.items()
            if device.state != DeviceState.DISCONNECTED.value
        }

    def get_device_info(self, device_id: str) -> Optional[DeviceInfo]:
        """Get device information."""
        return self._devices.get(device_id)

    def get_gsr_leader(self) -> Optional[DeviceInfo]:
        """Get current GSR leader device."""
        for device in self._devices.values():
            if device.is_gsr_leader and device.state != DeviceState.DISCONNECTED.value:
                return device
        return None

    # Enhanced networking methods
    async def _handle_secure_client(
        self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:
        """Handle secure client connections with TLS."""
        peer_addr = writer.get_extra_info("peername")
        logger.info(f"Secure client connected from {peer_addr}")

        # Handle the same way as regular clients but with security context
        await self._handle_client(reader, writer, is_secure=True)

    async def _send_message_to_device(
        self, host: str, port: int, message: Dict[str, Any]
    ) -> bool:
        """
        Send message to a specific device (transport for reliable messaging).

        Args:
            host: Target device IP address
            port: Target device port
            message: Message data to send

        Returns:
            bool: True if message was sent successfully
        """
        try:
            # Find device by IP address
            target_device = None
            for device in self._devices.values():
                if device.ip_address == host:
                    target_device = device
                    break

            if not target_device:
                logger.warning(f"No device found for {host}:{port}")
                return False

            # Send message to device
            await self._send_to_client(target_device.device_id, message)
            return True

        except Exception as e:
            logger.error(f"Failed to send message to {host}:{port}: {e}")
            return False

    async def _on_device_discovered(self, event_type: str, device) -> None:
        """Handle device discovery events."""
        try:
            if event_type == "discovered":
                logger.info(
                    f"Discovered device: {device.service_name} ({device.device_type.value}) at {device.ip_address}:{device.port}"
                )

                # Optionally auto-connect to discovered devices
                auto_connect = config.get("network.auto_connect_discovered", False)
                if auto_connect:
                    logger.debug(
                        f"Auto-connecting to discovered device: {device.service_name}"
                    )
                    # Could implement auto-connection logic here

            elif event_type == "lost":
                logger.info(f"Lost device: {device.service_name}")

        except Exception as e:
            logger.error(f"Error handling device discovery event: {e}")

    async def _handle_device_auth(
        self, message: Dict[str, Any], device_id: str
    ) -> Dict[str, Any]:
        """Handle device authentication request."""
        try:
            auth_token = message.get("auth_token")
            certificate_data = message.get("certificate")

            if certificate_data:
                # Validate device certificate
                cert_bytes = certificate_data.encode("utf-8")
                is_valid, device_type = (
                    self._security_manager.validate_device_certificate(cert_bytes)
                )

                if is_valid:
                    # Generate auth token for the device
                    token = self._security_manager.generate_auth_token(device_id)

                    return create_message(
                        "auth_response",
                        {
                            "success": True,
                            "auth_token": token,
                            "device_type": device_type,
                            "secure_port": self._secure_port,
                        },
                    )
                else:
                    return create_message(
                        "auth_response",
                        {"success": False, "error": "Certificate validation failed"},
                    )
            elif auth_token:
                # Validate existing token
                is_valid, token_device_id = self._security_manager.validate_auth_token(
                    auth_token
                )

                if is_valid and token_device_id == device_id:
                    return create_message(
                        "auth_response", {"success": True, "token_valid": True}
                    )
                else:
                    return create_message(
                        "auth_response",
                        {"success": False, "error": "Token validation failed"},
                    )
            else:
                return create_message(
                    "auth_response",
                    {"success": False, "error": "No authentication data provided"},
                )

        except Exception as e:
            logger.error(f"Error handling device authentication: {e}")
            return create_message(
                "auth_response",
                {"success": False, "error": f"Authentication error: {e}"},
            )

    async def _handle_message_ack(
        self, message: Dict[str, Any], device_id: str
    ) -> Optional[Dict[str, Any]]:
        """Handle message acknowledgment."""
        await self._messaging_service.handle_acknowledgment(
            message.get("original_message_id", ""), True
        )
        return None

    async def _handle_message_nack(
        self, message: Dict[str, Any], device_id: str
    ) -> Optional[Dict[str, Any]]:
        """Handle message negative acknowledgment."""
        await self._messaging_service.handle_acknowledgment(
            message.get("original_message_id", ""),
            False,
            message.get("error_message", "Unknown error"),
        )
        return None

    async def _handle_reliable_session_start(
        self, message: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        """Handle reliable session start message."""
        try:
            session_id = message.get("session_id")
            if session_id:
                logger.info(f"Reliable session start received: {session_id}")
                # Process session start logic here
                return {
                    "message_type": "session_start_ack",
                    "session_id": session_id,
                    "status": "accepted",
                }
        except Exception as e:
            logger.error(f"Error handling reliable session start: {e}")
        return None

    async def _handle_reliable_session_stop(
        self, message: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        """Handle reliable session stop message."""
        try:
            session_id = message.get("session_id")
            if session_id:
                logger.info(f"Reliable session stop received: {session_id}")
                # Process session stop logic here
                return {
                    "message_type": "session_stop_ack",
                    "session_id": session_id,
                    "status": "acknowledged",
                }
        except Exception as e:
            logger.error(f"Error handling reliable session stop: {e}")
        return None

    async def _handle_reliable_sync_flash(
        self, message: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        """Handle reliable sync flash message."""
        try:
            flash_id = message.get("flash_id")
            if flash_id:
                logger.info(f"Reliable sync flash received: {flash_id}")
                # Process sync flash logic here
                return {
                    "message_type": "sync_flash_ack",
                    "flash_id": flash_id,
                    "status": "executed",
                }
        except Exception as e:
            logger.error(f"Error handling reliable sync flash: {e}")
        return None

    async def send_reliable_message_to_device(
        self,
        device_id: str,
        message_type: str,
        content: Dict[str, Any],
        priority: MessagePriority = MessagePriority.NORMAL,
        timeout_seconds: float = 30.0,
    ) -> str:
        """
        Send a reliable message to a specific device.

        Args:
            device_id: Target device ID
            message_type: Type of message
            content: Message content
            priority: Message priority
            timeout_seconds: Message timeout

        Returns:
            str: Message ID for tracking
        """
        device = self._devices.get(device_id)
        if not device:
            raise ValueError(f"Device {device_id} not found")

        return await self._messaging_service.send_message(
            target_host=device.ip_address,
            target_port=device.port or self._port,
            message_type=message_type,
            content=content,
            priority=priority,
            timeout_seconds=timeout_seconds,
        )

    @property
    def is_running(self) -> bool:
        """Check if server is running."""
        return self._is_running

    def _calculate_network_latency(self, device_id: str) -> float:
        """Calculate network latency for a device."""
        # Simple latency estimation based on heartbeat timing
        device = self._devices.get(device_id)
        if device and hasattr(device, "last_heartbeat"):
            current_time = datetime.now()
            if device.last_heartbeat:
                # Estimate round-trip time based on heartbeat response
                latency_ms = (
                    current_time - device.last_heartbeat
                ).total_seconds() * 500  # Rough estimate
                return min(latency_ms, 1000.0)  # Cap at 1 second
        return 50.0  # Default estimate

    def _calculate_data_hash(self, data_point: Dict[str, Any]) -> str:
        """Calculate integrity hash for data verification."""
        import hashlib

        # Create hash from critical data fields
        hash_data = (
            f"{data_point.get('timestamp_ns', 0)}"
            f"{data_point.get('gsr_raw', 0)}"
            f"{data_point.get('ppg_raw', 0)}"
        )

        return hashlib.md5(hash_data.encode()).hexdigest()[:8]

    def _update_realtime_gsr_visualization(
        self, device_id: str, data_points: List[Dict[str, Any]]
    ) -> None:
        """Update real-time GSR visualization if available."""
        try:
            # This would interface with the PyQtGraph plotting widgets
            # For now, just log the data summary
            if data_points:
                latest_point = data_points[-1]
                gsr_value = latest_point.get("gsr_microsiemens", 0)
                logger.debug(f"Real-time GSR from {device_id}: {gsr_value:.4f} µS")

                # In a full implementation, this would:
                # 1. Send data to GUI plotting thread
                # 2. Update real-time charts
                # 3. Trigger alarms if values exceed thresholds
                # 4. Update device status indicators

        except Exception as e:
            logger.debug(f"Real-time visualization update failed: {e}")

    def _buffer_gsr_data(
        self, device_id: str, data_points: List[Dict[str, Any]]
    ) -> None:
        """Fallback method to buffer GSR data when aggregator is unavailable."""
        if not hasattr(self, "_gsr_data_buffer"):
            self._gsr_data_buffer = {}

        if device_id not in self._gsr_data_buffer:
            self._gsr_data_buffer[device_id] = []

        # Add timestamp for when data was received
        timestamped_points = []
        for point in data_points:
            enhanced_point = point.copy()
            enhanced_point["reception_timestamp_ns"] = time.time_ns()
            timestamped_points.append(enhanced_point)

        self._gsr_data_buffer[device_id].extend(timestamped_points)

        # Limit buffer size to prevent memory issues
        max_buffer_size = 10000  # Keep last 10k points per device
        if len(self._gsr_data_buffer[device_id]) > max_buffer_size:
            self._gsr_data_buffer[device_id] = self._gsr_data_buffer[device_id][
                -max_buffer_size:
            ]

        logger.debug(
            f"Buffered {len(data_points)} GSR points from {device_id}, "
            f"buffer size: {len(self._gsr_data_buffer[device_id])}"
        )

    # Enhanced GSR Streaming Handlers for Hub-Spoke Communication

    async def _handle_gsr_stream_registration(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle GSR stream registration from Android device"""
        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")
            stream_type = message.get("stream_type")

            if not all([device_id, session_id, stream_type]):
                return {"message_type": "error", "error": "Missing required fields"}

            # Register with GSR receiver
            success = await self._gsr_receiver.register_device_session(
                device_id, session_id
            )

            if success:
                logger.info(f"Registered GSR stream: {device_id}/{session_id}")
                return {
                    "message_type": "ack",
                    "status": "registered",
                    "server_time": time.time(),
                }
            else:
                return {"message_type": "error", "error": "Registration failed"}

        except Exception as e:
            logger.error(f"Error handling GSR stream registration: {e}")
            return {"message_type": "error", "error": str(e)}

    async def _handle_gsr_data_stream(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Optional[Dict[str, Any]]:
        """Handle real-time GSR data stream from Android device"""
        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")
            samples = message.get("samples", [])

            if not device_id or not samples:
                logger.warning("Invalid GSR data stream message")
                return None

            # Process GSR batch with receiver
            success = await self._gsr_receiver.process_gsr_batch(
                device_id, session_id, samples
            )

            if success:
                logger.debug(
                    f"Processed GSR batch: {len(samples)} samples from {device_id}"
                )
            else:
                logger.warning(f"Failed to process GSR batch from {device_id}")

            # No explicit response needed for streaming data
            return None

        except Exception as e:
            logger.error(f"Error handling GSR data stream: {e}")
            return None

    async def _handle_gsr_quality_metrics(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Optional[Dict[str, Any]]:
        """Handle GSR quality metrics from Android device"""
        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not device_id:
                return None

            # Process quality metrics with receiver
            success = await self._gsr_receiver.handle_quality_metrics(
                device_id, session_id, message
            )

            if success:
                logger.debug(f"Processed quality metrics from {device_id}")

            return None

        except Exception as e:
            logger.error(f"Error handling GSR quality metrics: {e}")
            return None

    async def _handle_gsr_heartbeat(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Optional[Dict[str, Any]]:
        """Handle GSR heartbeat from Android device"""
        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not device_id:
                return None

            # Process heartbeat with receiver
            success = await self._gsr_receiver.handle_heartbeat(
                device_id, session_id, message
            )

            if success:
                logger.debug(f"Processed GSR heartbeat from {device_id}")

            return None

        except Exception as e:
            logger.error(f"Error handling GSR heartbeat: {e}")
            return None

    async def _handle_gsr_stream_end(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle GSR stream end notification from Android device"""
        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not all([device_id, session_id]):
                return {"message_type": "error", "error": "Missing required fields"}

            # End session with GSR receiver
            success = await self._gsr_receiver.end_session(device_id, session_id)

            if success:
                logger.info(f"Ended GSR stream: {device_id}/{session_id}")
                return {
                    "message_type": "ack",
                    "status": "stream_ended",
                    "server_time": time.time(),
                }
            else:
                return {"message_type": "error", "error": "Failed to end stream"}

        except Exception as e:
            logger.error(f"Error handling GSR stream end: {e}")
            return {"message_type": "error", "error": str(e)}

    async def _handle_time_sync_request(
        self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:
        """Handle time synchronization request from Android device"""
        try:
            client_timestamp = message.get("client_timestamp")

            if client_timestamp is None:
                return {"message_type": "error", "error": "Missing client_timestamp"}

            # Server timestamp in nanoseconds
            server_timestamp = time.time_ns()

            return {
                "message_type": "time_sync_response",
                "client_timestamp": client_timestamp,
                "server_timestamp": server_timestamp,
                "server_time": time.time(),
            }

        except Exception as e:
            logger.error(f"Error handling time sync request: {e}")
            return {"message_type": "error", "error": str(e)}

    def get_gsr_session_stats(self) -> Dict[str, Any]:
        """Get GSR session statistics for monitoring"""
        try:
            return self._gsr_receiver.get_all_session_stats()
        except Exception as e:
            logger.error(f"Error getting GSR session stats: {e}")
            return {}

    async def export_gsr_session_data(
        self, device_id: str, session_id: str, format: str = "csv"
    ) -> Optional[str]:
        """Export GSR session data to file"""
        try:
            export_path = await self._gsr_receiver.export_session_data(
                device_id, session_id, format
            )
            return str(export_path) if export_path else None
        except Exception as e:
            logger.error(f"Error exporting GSR session data: {e}")
            return None
