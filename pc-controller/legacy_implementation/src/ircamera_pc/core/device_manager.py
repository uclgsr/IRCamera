

import asyncio
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Callable, Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..network.discovery import DeviceType, DiscoveredDevice, NetworkDiscoveryService


class DeviceConnectionState(Enum):

    DISCOVERED = "discovered"
    ONLINE = "online"
    RECORDING = "recording"
    DISCONNECTED = "disconnected"
    ERROR = "error"


class ConnectionQuality(Enum):

    EXCELLENT = "excellent"
    GOOD = "good"
    POOR = "poor"
    UNSTABLE = "unstable"


@dataclass
class DeviceCapabilities:

    supports_rgb_camera: bool = False
    supports_thermal_camera: bool = False
    supports_gsr_sensor: bool = False
    supports_file_transfer: bool = False
    supports_time_sync: bool = False
    max_resolution: str = "1080p"
    sampling_rates: List[int] = field(default_factory=list)
    custom_capabilities: Dict[str, Any] = field(default_factory=dict)


@dataclass
class DeviceInfo:

    device_id: str
    device_name: str
    device_type: DeviceType
    ip_address: str
    port: int
    state: DeviceConnectionState
    capabilities: DeviceCapabilities

    discovered_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    connected_at: Optional[datetime] = None
    last_heartbeat: Optional[datetime] = None
    connection_quality: ConnectionQuality = ConnectionQuality.GOOD

    total_connections: int = 0
    total_disconnections: int = 0
    bytes_sent: int = 0
    bytes_received: int = 0

    current_session_id: Optional[str] = None
    sessions_participated: List[str] = field(default_factory=list)

    def __post_init__(self):

        if not self.device_id:
            self.device_id = f"{self.device_name}_{self.ip_address}_{self.port}"

    def to_dict(self) -> Dict[str, Any]:

        result = asdict(self)
        result['discovered_at'] = self.discovered_at.isoformat()
        if self.connected_at:
            result['connected_at'] = self.connected_at.isoformat()
        if self.last_heartbeat:
            result['last_heartbeat'] = self.last_heartbeat.isoformat()
        return result


class DeviceRegistry:

    def __init__(self):

        self.devices: Dict[str, DeviceInfo] = {}
        self._callbacks: List[Callable[[str, DeviceInfo, str], None]] = []
        self._heartbeat_timeout = 30.0
        self._discovery_service: Optional[NetworkDiscoveryService] = None

    def add_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:

        self._callbacks.append(callback)

    def remove_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:

        if callback in self._callbacks:
            self._callbacks.remove(callback)

    def _notify_callbacks(self, device_id: str, device_info: DeviceInfo, event_type: str) -> None:

        for callback in self._callbacks:
            try:
                callback(device_id, device_info, event_type)
            except Exception as e:
                logger.error(f"Error in device status callback: {e}")

    def register_device(self, discovered_device: DiscoveredDevice) -> str:

        device_id = f"{discovered_device.service_name}_{discovered_device.ip_address}_{discovered_device.port}"

        capabilities = self._parse_capabilities(discovered_device.attributes)

        device_info = DeviceInfo(
            device_id=device_id,
            device_name=discovered_device.service_name,
            device_type=discovered_device.device_type,
            ip_address=discovered_device.ip_address,
            port=discovered_device.port,
            state=DeviceConnectionState.DISCOVERED,
            capabilities=capabilities
        )

        if device_id in self.devices:

            existing = self.devices[device_id]
            existing.ip_address = discovered_device.ip_address
            existing.port = discovered_device.port
            existing.discovered_at = datetime.now(timezone.utc)
            existing.state = DeviceConnectionState.DISCOVERED
            logger.info(f"Updated existing device: {device_id}")
            self._notify_callbacks(device_id, existing, "updated")
        else:

            self.devices[device_id] = device_info
            logger.info(f"Registered new device: {device_id} ({device_info.device_type.name})")
            self._notify_callbacks(device_id, device_info, "discovered")

        return device_id

    def _parse_capabilities(self, attributes: Dict[str, str]) -> DeviceCapabilities:

        capabilities = DeviceCapabilities()

        caps_str = attributes.get("capabilities", "")
        caps_list = [cap.strip() for cap in caps_str.split(",") if cap.strip()]

        capabilities.supports_rgb_camera = "rgb_camera" in caps_list
        capabilities.supports_thermal_camera = "thermal_camera" in caps_list
        capabilities.supports_gsr_sensor = "gsr_sensor" in caps_list
        capabilities.supports_file_transfer = "file_transfer" in caps_list
        capabilities.supports_time_sync = "time_sync" in caps_list

        capabilities.max_resolution = attributes.get("max_resolution", "1080p")

        rates_str = attributes.get("sampling_rates", "")
        if rates_str:
            try:
                capabilities.sampling_rates = [int(r.strip()) for r in rates_str.split(",")]
            except ValueError:
                capabilities.sampling_rates = []

        return capabilities

    def update_device_state(self, device_id: str, new_state: DeviceConnectionState) -> bool:

        if device_id not in self.devices:
            logger.warning(f"Cannot update state for unknown device: {device_id}")
            return False

        device = self.devices[device_id]
        old_state = device.state
        device.state = new_state

        if new_state == DeviceConnectionState.ONLINE:
            device.connected_at = datetime.now(timezone.utc)
            device.total_connections += 1
        elif old_state == DeviceConnectionState.ONLINE and new_state == DeviceConnectionState.DISCONNECTED:
            device.total_disconnections += 1

        logger.info(f"Device {device_id} state changed: {old_state.value} -> {new_state.value}")
        self._notify_callbacks(device_id, device, "state_changed")

        return True

    def update_heartbeat(self, device_id: str) -> bool:

        if device_id not in self.devices:
            return False

        self.devices[device_id].last_heartbeat = datetime.now(timezone.utc)
        return True

    def remove_device(self, device_id: str, reason: str = "manual") -> bool:

        if device_id not in self.devices:
            return False

        device_info = self.devices[device_id]
        del self.devices[device_id]

        logger.info(f"Removed device {device_id} from registry. Reason: {reason}")
        self._notify_callbacks(device_id, device_info, "removed")

        return True

    def get_device(self, device_id: str) -> Optional[DeviceInfo]:

        return self.devices.get(device_id)

    def get_devices_by_state(self, state: DeviceConnectionState) -> List[DeviceInfo]:

        return [device for device in self.devices.values() if device.state == state]

    def get_devices_by_type(self, device_type: DeviceType) -> List[DeviceInfo]:

        return [device for device in self.devices.values() if device.device_type == device_type]

    def get_recording_devices(self) -> List[DeviceInfo]:

        return self.get_devices_by_state(DeviceConnectionState.RECORDING)

    def get_online_devices(self) -> List[DeviceInfo]:

        return self.get_devices_by_state(DeviceConnectionState.ONLINE)

    def check_heartbeat_timeouts(self) -> List[str]:

        current_time = datetime.now(timezone.utc)
        timed_out_devices = []

        for device_id, device in self.devices.items():
            if device.last_heartbeat and device.state == DeviceConnectionState.ONLINE:
                time_since_heartbeat = (current_time - device.last_heartbeat).total_seconds()
                if time_since_heartbeat > self._heartbeat_timeout:
                    timed_out_devices.append(device_id)
                    logger.warning(
                        f"Device {device_id} heartbeat timeout "
                        f"({time_since_heartbeat:.1f}s > {self._heartbeat_timeout}s)"
                    )

        return timed_out_devices

    def get_all_devices(self) -> Dict[str, DeviceInfo]:

        return self.devices.copy()

    def get_device_count(self) -> int:

        return len(self.devices)

    def get_device_count_by_state(self, state: DeviceConnectionState) -> int:

        return len(self.get_devices_by_state(state))


class DeviceManager:

    def __init__(self):

        self.registry = DeviceRegistry()
        self.discovery_service = NetworkDiscoveryService()
        self._running = False
        self._monitoring_task: Optional[asyncio.Task] = None

        self.discovery_service.add_discovery_listener(self._on_device_discovered)
        self.discovery_service.add_discovery_listener(self._on_device_lost)

    async def start(self) -> bool:

        try:

            if not await self.discovery_service.start_discovery():
                logger.error("Failed to start discovery service")
                return False

            self._running = True
            self._monitoring_task = asyncio.create_task(self._monitoring_loop())

            logger.info("Device manager started successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to start device manager: {e}")
            return False

    async def stop(self) -> None:

        try:
            self._running = False

            if self._monitoring_task:
                self._monitoring_task.cancel()
                try:
                    await self._monitoring_task
                except asyncio.CancelledError:
                    pass

            await self.discovery_service.stop_discovery()

            logger.info("Device manager stopped")

        except Exception as e:
            logger.error(f"Error stopping device manager: {e}")

    async def _monitoring_loop(self) -> None:

        while self._running:
            try:

                timed_out_devices = self.registry.check_heartbeat_timeouts()

                for device_id in timed_out_devices:
                    self.registry.update_device_state(device_id, DeviceConnectionState.DISCONNECTED)

                await asyncio.sleep(5.0)

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in device monitoring loop: {e}")
                await asyncio.sleep(5.0)

    def _on_device_discovered(self, event_type: str, discovered_device: DiscoveredDevice) -> None:

        if event_type == "discovered":
            device_id = self.registry.register_device(discovered_device)
            logger.info(f"Device discovered and registered: {device_id}")

    def _on_device_lost(self, event_type: str, lost_device: DiscoveredDevice) -> None:

        if event_type == "lost":

            service_name = lost_device.service_name

            for device_id, device in self.registry.get_all_devices().items():
                if device.device_name == service_name:
                    self.registry.update_device_state(device_id, DeviceConnectionState.DISCONNECTED)
                    logger.info(f"Device lost: {device_id}")
                    break

    def add_device_manually(self, ip_address: str, port: int, device_name: str) -> Optional[str]:

        try:

            discovered_device = DiscoveredDevice(
                service_name=device_name,
                service_type="_ircamera._tcp.local.",
                ip_address=ip_address,
                port=port,
                device_type=DeviceType.ANDROID_SENSOR_NODE,
                attributes={},
                discovered_at=datetime.now(timezone.utc),
                last_seen=datetime.now()
            )

            device_id = self.registry.register_device(discovered_device)
            logger.info(f"Manually added device: {device_id}")
            return device_id

        except Exception as e:
            logger.error(f"Failed to manually add device: {e}")
            return None

    async def refresh_discovery(self) -> None:

        await self.discovery_service.refresh_discovery()

    def get_registry(self) -> DeviceRegistry:

        return self.registry

    def add_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:

        self.registry.add_status_callback(callback)

    def remove_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:

        self.registry.remove_status_callback(callback)

    async def connect_to_device(self, device_id: str) -> bool:

        try:
            device_info = self.registry.get_device(device_id)
            if not device_info:
                logger.error(f"Device not found in registry: {device_id}")
                return False

            self.registry.update_device_state(device_id, DeviceConnectionState.ONLINE)

            # TODO: Implement actual network connection logic

            logger.info(f"Connected to device: {device_id}")
            return True

        except Exception as e:
            logger.error(f"Failed to connect to device {device_id}: {e}")
            self.registry.update_device_state(device_id, DeviceConnectionState.ERROR)
            return False
