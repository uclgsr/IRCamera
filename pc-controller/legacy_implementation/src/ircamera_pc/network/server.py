import asyncio
import json
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
from ..sync import EnhancedTimeSyncService
from .discovery import NetworkDiscoveryService
from .messaging import MessagePriority, ReliableMessageService
from .protocol import (
    ValidationError,
    create_message,
    get_protocol_manager,
    validate_message,
)
from .security import SecurityManager

class DeviceState(Enum):
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    RECORDING = "recording"
    ERROR = "error"

class MessageType(Enum):
    DEVICE_REGISTER = "device_register"
    DEVICE_HEARTBEAT = "device_heartbeat"
    DEVICE_STATUS = "device_status"

    SESSION_START = "session_start"
    SESSION_STOP = "session_stop"
    RECORDING_START = "recording_start"
    RECORDING_STOP = "recording_stop"

    SYNC_MARK = "sync_mark"
    SYNC_FLASH = "sync_flash"

    FILE_TRANSFER_REQUEST = "file_transfer_request"
    FILE_TRANSFER_COMPLETE = "file_transfer_complete"

    GSR_STREAM_REGISTER = "stream_registration"
    GSR_DATA = "gsr_data"
    GSR_QUALITY_METRICS = "quality_metrics"
    TIME_SYNC_REQUEST = "time_sync_request"
    TIME_SYNC_RESPONSE = "time_sync_response"

    ACK = "ack"
    ERROR = "error"

@dataclass
class DeviceInfo:
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
        return asdict(self)

class NetworkServer:

    def __init__(self):

        self._server: Optional[asyncio.Server] = None
        self._secure_server: Optional[asyncio.Server] = None
        self._clients: Dict[str, asyncio.StreamWriter] = {}
        self._devices: Dict[str, DeviceInfo] = {}
        self._message_handlers: Dict[str, Callable] = {}
        self._heartbeat_task: Optional[asyncio.Task] = None
        self._is_running = False

        self._security_manager = SecurityManager()
        self._discovery_service = NetworkDiscoveryService()
        self._messaging_service = ReliableMessageService()
        self._enhanced_timesync = EnhancedTimeSyncService()

        self._gsr_receiver = GSRReceiver(config.get("gsr_receiver", {}))

        self._protocol = get_protocol_manager()

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

        framing = transport_config.get("message_framing", {})
        self._max_message_size = framing.get(
            "max_message_size", 1024 * 1024
        )

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

        self._messaging_service.set_transport(self._send_message_to_device)

        self._discovery_service.add_discovery_listener(self._on_device_discovered)

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

        self._message_handlers = {
            "device_register": self._handle_device_register,
            "device_heartbeat": self._handle_device_heartbeat,
            "device_status": self._handle_device_status,
            "file_transfer_complete": self._handle_file_transfer_complete,
            "time_sync_request": self._handle_time_sync_request,
            "gsr_data_batch": self._handle_gsr_data_batch,
            "gsr_leader_election": self._handle_gsr_leader_election,

            "stream_registration": self._handle_gsr_stream_registration,
            "gsr_data": self._handle_gsr_data_stream,
            "quality_metrics": self._handle_gsr_quality_metrics,
            "heartbeat": self._handle_gsr_heartbeat,
            "stream_end": self._handle_gsr_stream_end,

            "device_auth": self._handle_device_auth,
            "message_ack": self._handle_message_ack,
            "message_nack": self._handle_message_nack,
        }

    async def start(self) -> bool:

        if self._is_running:
            logger.warning("Network server is already running")
            return True

        try:
            logger.info("Starting enhanced network server...")

            if not self._security_manager.initialize():
                logger.error("Failed to initialize security manager")
                return False

            if not await self._messaging_service.initialize():
                logger.error("Failed to initialize messaging service")
                return False

            if not await self._discovery_service.start_discovery():
                logger.warning(
                    "Discovery service failed to start - continuing without discovery"
                )

            await self._gsr_receiver.start()
            logger.info("GSR receiver started for hub-spoke communication")

            await self._enhanced_timesync.start()
            logger.info("Enhanced time synchronization service started")

            self._server = await asyncio.start_server(
                self._handle_client,
                self._host,
                self._port,
                limit=2 ** 16,
            )

            ssl_context = self._security_manager.create_ssl_context(
                for_client_auth=True
            )
            self._secure_server = await asyncio.start_server(
                self._handle_secure_client,
                self._host,
                self._secure_port,
                ssl=ssl_context,
                limit=2 ** 16,
            )

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

        if not self._is_running:
            return

        logger.info("Stopping enhanced network server...")
        self._is_running = False

        await self._messaging_service.shutdown()
        await self._discovery_service.stop_discovery()

        await self._gsr_receiver.stop()
        logger.info("GSR receiver stopped")

        await self._enhanced_timesync.stop()
        logger.info("Enhanced time synchronization service stopped")

        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            try:
                await self._heartbeat_task
            except asyncio.CancelledError:
                pass

        for client in self._clients.values():
            client.close()
            await client.wait_closed()

        self._clients.clear()

        if self._server:
            self._server.close()
            await self._server.wait_closed()

        if self._secure_server:
            self._secure_server.close()
            await self._secure_server.wait_closed()

        logger.info("Enhanced network server stopped")
        self._devices.clear()

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

        addr = writer.get_extra_info("peername")
        connection_type = "secure" if is_secure else "plaintext"
        logger.info(f"Client connected from {addr} ({connection_type})")

        try:
            while True:

                length_data = await reader.readexactly(4)
                message_length = int.from_bytes(length_data, "big")

                if message_length > self._max_message_size:
                    logger.warning(
                        f"Message too large from {addr}:" "{message_length} bytes"
                    )
                    break

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

        try:

            if not validate_message(message, strict=False):
                await self._send_error(writer, "Message validation failed")
                return

            message_type = message.get("message_type")
            message_id = message.get("message_id", str(uuid.uuid4()))

            if not message_type:
                await self._send_error(writer, "Missing message_type field", message_id)
                return

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

        try:
            device_id = message.get("device_id")
            device_type = message.get("device_type", "unknown")
            capabilities = message.get("capabilities", [])

            if not device_id:
                return {
                    "type": MessageType.ERROR.value,
                    "error": "Missing device_id",
                }

            if len(self._devices) >= self._max_connections:
                return {
                    "type": MessageType.ERROR.value,
                    "error": "Maximum connections exceeded",
                }

            addr = writer.get_extra_info("peername")

            device_info = DeviceInfo(
                device_id=device_id,
                device_type=device_type,
                capabilities=capabilities,
                ip_address=addr[0],
                port=addr[1],
                last_heartbeat=datetime.now(timezone.utc).isoformat(),
            )

            if "gsr_sensor" in capabilities and not any(
                    d.is_gsr_leader for d in self._devices.values()
            ):
                device_info.is_gsr_leader = True
                device_info.gsr_mode = config.get("gsr.default_mode", "local")
                logger.info(f"Device {device_id} elected as GSR leader")

            self._devices[device_id] = device_info
            self._clients[device_id] = writer

            logger.info(
                f"Device registered: {device_id} ({device_type})"
                "with capabilities: {capabilities}"
            )

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

        device_id = message.get("device_id")

        if device_id in self._devices:
            self._devices[device_id].last_heartbeat = datetime.now(
                timezone.utc
            ).isoformat()

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

        device_id = message.get("device_id")

        if device_id in self._devices:
            device = self._devices[device_id]

            if "status" in message:
                device.state = message["status"]
            if "battery_level" in message:
                device.battery_level = message["battery_level"]

            logger.debug(f"Status update from {device_id}: {message}")

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

        device_id = message.get("device_id")
        transfer_id = message.get("transfer_id")
        status = message.get("status")

        logger.info(f"File transfer {status} from {device_id}: {transfer_id}")

        return create_message("ack", ack_for="file_transfer_complete", status="success")

    async def _handle_time_sync_request(
            self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:

        try:
            device_id = message.get("device_id", "unknown")

            response = await self._enhanced_timesync.handle_time_sync_request(message, device_id)

            message_id = message.get("message_id")
            if message_id:
                response["message_id"] = message_id

            return response

        except Exception as e:
            logger.error(f"Error in time sync handler: {e}")
            return create_message(
                "error",
                error_code="SYNC_ERROR",
                error_message=f"Time sync error: {e}"
            )

    async def _handle_gsr_data_batch(
            self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:

        device_id = message.get("device_id")
        message.get("session_id")
        data_points = message.get("data_points", [])

        logger.debug(
            f"Received GSR data batch from {device_id}: {len(data_points)} points"
        )

        try:
            from ..data import get_data_aggregator

            aggregator = get_data_aggregator()

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

                await aggregator.add_gsr_data_point(enhanced_point)

            self._update_realtime_gsr_visualization(device_id, data_points)

            logger.info(
                f"Successfully processed {len(data_points)} GSR points from {device_id}"
            )

        except ImportError:
            logger.warning(
                "Data aggregator not available, trying fallback GSR ingestor"
            )

            try:
                from ..core.gsr_ingestor import GSRIngestor, GSRSample

                gsr_samples = []
                for point in data_points:
                    sample = GSRSample(
                        timestamp=point.get("timestamp", time.time()),
                        value=point.get("value", 0.0),
                        quality=point.get("quality", 100),
                        device_id=device_id,
                    )
                    gsr_samples.append(sample)

                if not hasattr(self, "_gsr_ingestor"):
                    self._gsr_ingestor = GSRIngestor()

                await self._gsr_ingestor.process_data_batch(
                    session_id=message.get("session_id"),
                    device_id=device_id,
                    samples=gsr_samples,
                )

                logger.debug(f"Forwarded {len(gsr_samples)} GSR samples to ingestor")

            except Exception as e:
                logger.warning(f"GSR ingestor also failed, storing data to buffer: {e}")

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

        device_id = message.get("device_id")
        election_type = message.get("election_type")
        priority_score = message.get("priority_score", 0)

        logger.info(
            f"GSR leader election from {device_id}: {election_type}"
            "(score: {priority_score})"
        )

        if election_type == "candidate" and device_id in self._devices:

            current_leader = None
            for did, device in self._devices.items():
                if device.is_gsr_leader:
                    current_leader = device
                    break

            if not current_leader or priority_score > 0.8:

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

        if device_id in self._devices:
            device_info = self._devices[device_id]
            device_info.state = DeviceState.DISCONNECTED.value

            self._clients.pop(device_id, None)

            logger.info(f"Device disconnected: {device_id}")

            if self._on_device_disconnected:
                self._on_device_disconnected(device_info)

            if device_info.is_gsr_leader:
                await self._handle_gsr_leader_disconnect(device_id)

    async def _handle_gsr_leader_disconnect(self, device_id: str) -> None:

        logger.warning(f"GSR leader {device_id} disconnected")

        for did, device in self._devices.items():
            if (
                    did != device_id
                    and device.state == DeviceState.CONNECTED.value
                    and "gsr_sensor" in device.capabilities
            ):

                device.is_gsr_leader = True
                device.gsr_mode = config.get("gsr.default_mode", "local")

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

        command = create_message(
            "session_start",
            session_id=session_id,
            session_name=session_name or f"Session_{session_id[:8]}",
        )

        logger.info(f"Starting recording session {session_id} on all devices")
        return await self.broadcast_command(command)

    async def stop_recording_session(self, session_id: str) -> Dict[str, bool]:

        command = create_message("session_stop", session_id=session_id)

        logger.info(f"Stopping recording session {session_id} on all devices")
        return await self.broadcast_command(command)

    async def send_sync_flash(self, duration_ms: int = 100) -> Dict[str, bool]:

        command = create_message(
            "sync_flash", duration_ms=duration_ms, intensity=1.0, color="white"
        )

        logger.info("Sending sync flash to all devices")
        return await self.broadcast_command(command)

    async def send_sync_mark(
            self, mark_type: str, metadata: Dict[str, Any] = None
    ) -> Dict[str, bool]:

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

        error_response = create_message(
            "error", error_code="INVALID_MESSAGE", error_message=error_message
        )

        if message_id:
            error_response["message_id"] = message_id

        await self._send_message(writer, error_response)

    def set_device_connected_callback(
            self, callback: Callable[[DeviceInfo], None]
    ) -> None:

        self._on_device_connected = callback

    def set_device_disconnected_callback(
            self, callback: Callable[[DeviceInfo], None]
    ) -> None:

        self._on_device_disconnected = callback

    def set_device_status_update_callback(
            self, callback: Callable[[DeviceInfo], None]
    ) -> None:

        self._on_device_status_update = callback

    def get_connected_devices(self) -> Dict[str, DeviceInfo]:

        return {
            did: device
            for did, device in self._devices.items()
            if device.state != DeviceState.DISCONNECTED.value
        }

    def get_device_info(self, device_id: str) -> Optional[DeviceInfo]:

        return self._devices.get(device_id)

    def get_gsr_leader(self) -> Optional[DeviceInfo]:

        for device in self._devices.values():
            if device.is_gsr_leader and device.state != DeviceState.DISCONNECTED.value:
                return device
        return None

    async def _handle_secure_client(
            self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:

        peer_addr = writer.get_extra_info("peername")
        logger.info(f"Secure client connected from {peer_addr}")

        await self._handle_client(reader, writer, is_secure=True)

    async def _send_message_to_device(
            self, host: str, port: int, message: Dict[str, Any]
    ) -> bool:

        try:

            target_device = None
            for device in self._devices.values():
                if device.ip_address == host:
                    target_device = device
                    break

            if not target_device:
                logger.warning(f"No device found for {host}:{port}")
                return False

            await self._send_to_client(target_device.device_id, message)
            return True

        except Exception as e:
            logger.error(f"Failed to send message to {host}:{port}: {e}")
            return False

    async def _on_device_discovered(self, event_type: str, device) -> None:

        try:
            if event_type == "discovered":
                logger.info(
                    f"Discovered device: {device.service_name} ({device.device_type.value}) at {device.ip_address}:{device.port}"
                )

                auto_connect = config.get("network.auto_connect_discovered", False)
                if auto_connect:
                    logger.debug(
                        f"Auto-connecting to discovered device: {device.service_name}"
                    )

            elif event_type == "lost":
                logger.info(f"Lost device: {device.service_name}")

        except Exception as e:
            logger.error(f"Error handling device discovery event: {e}")

    async def _handle_device_auth(
            self, message: Dict[str, Any], device_id: str
    ) -> Dict[str, Any]:

        try:
            auth_token = message.get("auth_token")
            certificate_data = message.get("certificate")

            if certificate_data:

                cert_bytes = certificate_data.encode("utf-8")
                is_valid, device_type = (
                    self._security_manager.validate_device_certificate(cert_bytes)
                )

                if is_valid:

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

        await self._messaging_service.handle_acknowledgment(
            message.get("original_message_id", ""), True
        )
        return None

    async def _handle_message_nack(
            self, message: Dict[str, Any], device_id: str
    ) -> Optional[Dict[str, Any]]:

        await self._messaging_service.handle_acknowledgment(
            message.get("original_message_id", ""),
            False,
            message.get("error_message", "Unknown error"),
        )
        return None

    async def _handle_reliable_session_start(
            self, message: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:

        try:
            session_id = message.get("session_id")
            if session_id:
                logger.info(f"Reliable session start received: {session_id}")

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

        try:
            session_id = message.get("session_id")
            if session_id:
                logger.info(f"Reliable session stop received: {session_id}")

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

        try:
            flash_id = message.get("flash_id")
            if flash_id:
                logger.info(f"Reliable sync flash received: {flash_id}")

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

        return self._is_running

    def _calculate_network_latency(self, device_id: str) -> float:

        device = self._devices.get(device_id)
        if device and hasattr(device, "last_heartbeat"):
            current_time = datetime.now()
            if device.last_heartbeat:
                latency_ms = (
                                     current_time - device.last_heartbeat
                             ).total_seconds() * 500
                return min(latency_ms, 1000.0)
        return 50.0

    def _calculate_data_hash(self, data_point: Dict[str, Any]) -> str:

        import hashlib

        hash_data = (
            f"{data_point.get('timestamp_ns', 0)}"
            f"{data_point.get('gsr_raw', 0)}"
            f"{data_point.get('ppg_raw', 0)}"
        )

        return hashlib.md5(hash_data.encode()).hexdigest()[:8]

    def _update_realtime_gsr_visualization(
            self, device_id: str, data_points: List[Dict[str, Any]]
    ) -> None:

        try:

            if data_points:
                latest_point = data_points[-1]
                gsr_value = latest_point.get("gsr_microsiemens", 0)
                logger.debug(f"Real-time GSR from {device_id}: {gsr_value:.4f} µS")

        except Exception as e:
            logger.debug(f"Real-time visualization update failed: {e}")

    def _buffer_gsr_data(
            self, device_id: str, data_points: List[Dict[str, Any]]
    ) -> None:

        if not hasattr(self, "_gsr_data_buffer"):
            self._gsr_data_buffer = {}

        if device_id not in self._gsr_data_buffer:
            self._gsr_data_buffer[device_id] = []

        timestamped_points = []
        for point in data_points:
            enhanced_point = point.copy()
            enhanced_point["reception_timestamp_ns"] = time.time_ns()
            timestamped_points.append(enhanced_point)

        self._gsr_data_buffer[device_id].extend(timestamped_points)

        max_buffer_size = 10000
        if len(self._gsr_data_buffer[device_id]) > max_buffer_size:
            self._gsr_data_buffer[device_id] = self._gsr_data_buffer[device_id][
                -max_buffer_size:
            ]

        logger.debug(
            f"Buffered {len(data_points)} GSR points from {device_id}, "
            f"buffer size: {len(self._gsr_data_buffer[device_id])}"
        )

    async def _handle_gsr_stream_registration(
            self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Dict[str, Any]:

        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")
            stream_type = message.get("stream_type")

            if not all([device_id, session_id, stream_type]):
                return {"message_type": "error", "error": "Missing required fields"}

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

        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")
            samples = message.get("samples", [])

            if not device_id or not samples:
                logger.warning("Invalid GSR data stream message")
                return None

            success = await self._gsr_receiver.process_gsr_batch(
                device_id, session_id, samples
            )

            if success:
                logger.debug(
                    f"Processed GSR batch: {len(samples)} samples from {device_id}"
                )
            else:
                logger.warning(f"Failed to process GSR batch from {device_id}")

            return None

        except Exception as e:
            logger.error(f"Error handling GSR data stream: {e}")
            return None

    async def _handle_gsr_quality_metrics(
            self, message: Dict[str, Any], writer: asyncio.StreamWriter
    ) -> Optional[Dict[str, Any]]:

        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not device_id:
                return None

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

        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not device_id:
                return None

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

        try:
            device_id = message.get("device_id")
            session_id = message.get("session_id")

            if not all([device_id, session_id]):
                return {"message_type": "error", "error": "Missing required fields"}

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

    def get_gsr_session_stats(self) -> Dict[str, Any]:

        try:
            return self._gsr_receiver.get_all_session_stats()
        except Exception as e:
            logger.error(f"Error getting GSR session stats: {e}")
            return {}

    async def export_gsr_session_data(
            self, device_id: str, session_id: str, format: str = "csv"
    ) -> Optional[str]:

        try:
            export_path = await self._gsr_receiver.export_session_data(
                device_id, session_id, format
            )
            return str(export_path) if export_path else None
        except Exception as e:
            logger.error(f"Error exporting GSR session data: {e}")
            return None

    def get_time_sync_stats(self, device_id: str = None) -> Dict[str, Any]:

        if device_id:
            stats = self._enhanced_timesync.get_device_sync_stats(device_id)
            return asdict(stats) if stats else {}
        else:
            return self._enhanced_timesync.get_sync_quality_summary()

    def get_all_time_sync_stats(self) -> Dict[str, Any]:

        all_stats = self._enhanced_timesync.get_all_sync_stats()
        return {
            device_id: asdict(stats)
            for device_id, stats in all_stats.items()
        }

    def is_device_time_synchronized(self, device_id: str) -> bool:

        return self._enhanced_timesync.is_device_synchronized(device_id)

    async def register_time_sync_session(self, session_id: str, device_id: str) -> bool:

        return await self._enhanced_timesync.register_session(session_id, device_id)

    async def end_time_sync_session(self, session_id: str) -> bool:

        return await self._enhanced_timesync.end_session(session_id)
