import asyncio
import platform
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import get_logger

    logger = get_logger(__name__)

try:
    from PyQt6.QtCore import QTimer, pyqtSignal

    from .base_manager import BaseManager

    PYQT_AVAILABLE = True

except ImportError:
    from .base_manager import BaseManager

    PYQT_AVAILABLE = False

try:
    from bleak import BleakClient, BleakGATTCharacteristic, BleakScanner
    from bleak.backends.device import BLEDevice

    BLUETOOTH_AVAILABLE = True
except ImportError:
    logger.warning(
        "Bluetooth dependencies not available." "Install 'bleak' for Bluetooth support"
    )
    BLUETOOTH_AVAILABLE = False

    BLEDevice = object
    BleakClient = object
    BleakGATTCharacteristic = object
    BleakScanner = object

try:
    if platform.system() == "Windows":
        pass

        CLASSIC_BT_AVAILABLE = True
    else:
        CLASSIC_BT_AVAILABLE = False
except ImportError:
    CLASSIC_BT_AVAILABLE = False

class BluetoothDeviceType(Enum):
    BLE = "ble"
    CLASSIC = "classic"
    UNKNOWN = "unknown"

class ConnectionState(Enum):
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    ERROR = "error"

@dataclass
class BluetoothDevice:
    address: str
    name: str
    device_type: BluetoothDeviceType
    rssi: int
    services: List[str]
    last_seen: datetime
    is_ircamera: bool = False
    connection_state: ConnectionState = ConnectionState.DISCONNECTED

class BluetoothManager(BaseManager):
    if PYQT_AVAILABLE:
        device_discovered = pyqtSignal(BluetoothDevice)
        device_connected = pyqtSignal(str, str)
        device_disconnected = pyqtSignal(str, str)
        data_received = pyqtSignal(str, bytes)
        scan_completed = pyqtSignal(int)
        error_occurred = pyqtSignal(str, str)

    IRCAMERA_SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
    IRCAMERA_DATA_CHARACTERISTIC = "87654321-4321-4321-4321-cba987654321"

    def __init__(self):
        super().__init__("bluetooth_manager")
        self._devices: Dict[str, BluetoothDevice] = {}
        self._connections: Dict[str, BleakClient] = {}
        self._scanning = False

        if PYQT_AVAILABLE:
            self._scan_timer = QTimer()
            self._scan_timer.timeout.connect(self._periodic_scan)
        else:
            self._scan_timer = None

        if not BLUETOOTH_AVAILABLE:
            logger.error(
                "Bluetooth functionality not available" "- missing dependencies"
            )

    def _emit_signal(self, signal_name: str, *args):

        if PYQT_AVAILABLE and hasattr(self, signal_name):
            signal = getattr(self, signal_name)
            signal.emit(*args)

    @property
    def is_available(self) -> bool:

        return BLUETOOTH_AVAILABLE

    @property
    def discovered_devices(self) -> List[BluetoothDevice]:

        return list(self._devices.values())

    @property
    def connected_devices(self) -> List[BluetoothDevice]:

        return [
            device
            for device in self._devices.values()
            if device.connection_state == ConnectionState.CONNECTED
        ]

    def start_scanning(self, continuous: bool = False, interval: int = 10) -> None:

        if not self.is_available:
            self._emit_signal("error_occurred", "scan", "Bluetooth not available")
            return

        if self._scanning:
            logger.warning("Already scanning for devices")
            return

        self._scanning = True
        logger.info("Starting Bluetooth device scan")

        asyncio.create_task(self._scan_devices())

        if continuous and self._scan_timer:
            self._scan_timer.start(interval * 1000)

    def stop_scanning(self) -> None:

        self._scanning = False
        if self._scan_timer:
            self._scan_timer.stop()
        logger.info("Stopped Bluetooth device scanning")

    async def _scan_devices(self) -> None:

        try:
            logger.debug("Scanning for BLE devices...")
            devices = await BleakScanner.discover(timeout=5.0)

            discovered_count = 0
            for device in devices:
                if device.address not in self._devices or self._should_update_device(
                        device
                ):
                    bt_device = self._create_bluetooth_device(device)
                    self._devices[device.address] = bt_device
                    self._emit_signal("device_discovered", bt_device)
                    discovered_count += 1
                    logger.debug(
                        f"Discovered device: {device.name}" "({device.address})"
                    )

            self._emit_signal("scan_completed", discovered_count)
            logger.info(f"Scan completed - found {discovered_count}" "new devices")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error during device scan: {e}")
            self._emit_signal("error_occurred", "scan", str(e))

    def _periodic_scan(self) -> None:

        if self._scanning:
            asyncio.create_task(self._scan_devices())

    def _should_update_device(self, device: BLEDevice) -> bool:

        if device.address not in self._devices:
            return True

        existing = self._devices[device.address]

        return abs(existing.rssi - (device.rssi or -100)) > 10 or (
                not existing.name and device.name
        )

    def _create_bluetooth_device(self, device: BLEDevice) -> BluetoothDevice:

        is_ircamera = self._is_ircamera_device(device)

        return BluetoothDevice(
            address=device.address,
            name=device.name or "Unknown Device",
            device_type=BluetoothDeviceType.BLE,
            rssi=device.rssi or -100,
            services=[],
            last_seen=datetime.now(),
            is_ircamera=is_ircamera,
        )

    def _is_ircamera_device(self, device: BLEDevice) -> bool:

        if not device.name:
            return False

        name_lower = device.name.lower()
        ircamera_patterns = [
            "ircamera",
            "thermal",
            "flir",
            "seek",
            "hikvision",
        ]

        return any(pattern in name_lower for pattern in ircamera_patterns)

    async def connect_device(self, address: str) -> bool:

        if not self.is_available:
            self._emit_signal("error_occurred", "connect", "Bluetooth not available")
            return False

        if address not in self._devices:
            self._emit_signal(
                "error_occurred", "connect", f"Device {address} not found"
            )
            return False

        device = self._devices[address]
        device.connection_state = ConnectionState.CONNECTING

        try:
            logger.info(f"Connecting to device {device.name} ({address})")

            client = BleakClient(address)
            await client.connect()

            services = await client.get_services()
            device.services = [str(service.uuid) for service in services]

            if self.IRCAMERA_SERVICE_UUID in device.services:
                device.is_ircamera = True
                logger.info(f"IRCamera service detected on {device.name}")

            self._connections[address] = client
            device.connection_state = ConnectionState.CONNECTED

            self._emit_signal("device_connected", address, device.name)
            logger.info(f"Successfully connected to {device.name}")

            if device.is_ircamera:
                await self._setup_ircamera_notifications(client)

            return True

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to connect to {address}: {e}")
            device.connection_state = ConnectionState.ERROR
            self._emit_signal("error_occurred", "connect", str(e))
            return False

    async def disconnect_device(self, address: str) -> None:

        if address not in self._connections:
            logger.warning(f"Device {address} not connected")
            return

        try:
            client = self._connections[address]
            await client.disconnect()
            del self._connections[address]

            if address in self._devices:
                device = self._devices[address]
                device.connection_state = ConnectionState.DISCONNECTED
                self._emit_signal("device_disconnected", address, "User initiated")
                logger.info(f"Disconnected from {device.name}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error disconnecting from {address}: {e}")
            self._emit_signal("error_occurred", "disconnect", str(e))

    async def send_data(self, address: str, data: bytes) -> bool:

        if address not in self._connections:
            self._emit_signal(
                "error_occurred", "send", f"Device {address} not connected"
            )
            return False

        if address not in self._devices or not self._devices[address].is_ircamera:
            self._emit_signal(
                "error_occurred",
                "send",
                f"Device {address}" "is not an IRCamera",
            )
            return False

        try:
            client = self._connections[address]
            await client.write_gatt_char(self.IRCAMERA_DATA_CHARACTERISTIC, data)
            logger.debug(f"Sent {len(data)} bytes to {address}")
            return True

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error sending data to {address}: {e}")
            self._emit_signal("error_occurred", "send", str(e))
            return False

    async def _setup_ircamera_notifications(self, client: BleakClient) -> None:

        try:
            await client.start_notify(
                self.IRCAMERA_DATA_CHARACTERISTIC,
                self._handle_ircamera_notification,
            )
            logger.debug("IRCamera notifications enabled")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to enable IRCamera notifications: {e}")

    def _handle_ircamera_notification(
            self, sender: BleakGATTCharacteristic, data: bytearray
    ) -> None:

        try:
            address = sender.service.client.address
            self._emit_signal("data_received", address, bytes(data))
            logger.debug(f"Received {len(data)} bytes from {address}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error handling notification: {e}")

    def get_device_info(self, address: str) -> Optional[BluetoothDevice]:

        return self._devices.get(address)

    def clear_devices(self) -> None:

        connected_addresses = set(self._connections.keys())
        self._devices = {
            addr: device
            for addr, device in self._devices.items()
            if addr in connected_addresses
        }
        logger.info("Cleared discovered devices list")

    async def cleanup(self) -> None:

        self.stop_scanning()

        for address in list(self._connections.keys()):
            await self.disconnect_device(address)

        logger.info("Bluetooth manager cleanup completed")
