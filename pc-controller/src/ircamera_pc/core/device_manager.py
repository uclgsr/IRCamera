"""
Device Manager for IRCamera PC Controller

Manages device registry, capabilities, and lifecycle according to Hub-and-Spoke architecture.
Implements device discovery and registration from the PC Hub Application MVP checklist.
"""

import asyncio
import time
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Callable, Dict, List, Optional, Set

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..network.discovery import DeviceType, DiscoveredDevice, NetworkDiscoveryService


class DeviceConnectionState(Enum):
    """Device connection states for the registry."""
    
    DISCOVERED = "discovered"
    ONLINE = "online"
    RECORDING = "recording"
    DISCONNECTED = "disconnected"
    ERROR = "error"


class ConnectionQuality(Enum):
    """Connection quality levels for monitoring."""
    
    EXCELLENT = "excellent"
    GOOD = "good"
    POOR = "poor"
    UNSTABLE = "unstable"


@dataclass
class DeviceCapabilities:
    """Device capability information."""
    
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
    """Comprehensive device information for the registry."""
    
    device_id: str
    device_name: str
    device_type: DeviceType
    ip_address: str
    port: int
    state: DeviceConnectionState
    capabilities: DeviceCapabilities
    
    # Connection metadata
    discovered_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    connected_at: Optional[datetime] = None
    last_heartbeat: Optional[datetime] = None
    connection_quality: ConnectionQuality = ConnectionQuality.GOOD
    
    # Statistics
    total_connections: int = 0
    total_disconnections: int = 0
    bytes_sent: int = 0
    bytes_received: int = 0
    
    # Session tracking
    current_session_id: Optional[str] = None
    sessions_participated: List[str] = field(default_factory=list)
    
    def __post_init__(self):
        """Ensure device_id is set if not provided."""
        if not self.device_id:
            self.device_id = f"{self.device_name}_{self.ip_address}_{self.port}"
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        result = asdict(self)
        result['discovered_at'] = self.discovered_at.isoformat()
        if self.connected_at:
            result['connected_at'] = self.connected_at.isoformat()
        if self.last_heartbeat:
            result['last_heartbeat'] = self.last_heartbeat.isoformat()
        return result


class DeviceRegistry:
    """
    Central registry for managing discovered and connected devices.
    Maintains device state and provides discovery integration.
    """
    
    def __init__(self):
        """Initialize the device registry."""
        self.devices: Dict[str, DeviceInfo] = {}
        self._callbacks: List[Callable[[str, DeviceInfo, str], None]] = []
        self._heartbeat_timeout = 30.0  # seconds
        self._discovery_service: Optional[NetworkDiscoveryService] = None
        
    def add_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:
        """
        Add callback for device status changes.
        
        Args:
            callback: Function called with (device_id, device_info, event_type)
        """
        self._callbacks.append(callback)
    
    def remove_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:
        """Remove status change callback."""
        if callback in self._callbacks:
            self._callbacks.remove(callback)
    
    def _notify_callbacks(self, device_id: str, device_info: DeviceInfo, event_type: str) -> None:
        """Notify all callbacks of device status change."""
        for callback in self._callbacks:
            try:
                callback(device_id, device_info, event_type)
            except Exception as e:
                logger.error(f"Error in device status callback: {e}")
    
    def register_device(self, discovered_device: DiscoveredDevice) -> str:
        """
        Register a discovered device in the registry.
        
        Args:
            discovered_device: Device discovered via mDNS
            
        Returns:
            Device ID for the registered device
        """
        device_id = f"{discovered_device.service_name}_{discovered_device.ip_address}_{discovered_device.port}"
        
        # Parse capabilities from discovery attributes
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
        
        # Check if device already exists
        if device_id in self.devices:
            # Update existing device
            existing = self.devices[device_id]
            existing.ip_address = discovered_device.ip_address
            existing.port = discovered_device.port
            existing.discovered_at = datetime.now(timezone.utc)
            existing.state = DeviceConnectionState.DISCOVERED
            logger.info(f"Updated existing device: {device_id}")
            self._notify_callbacks(device_id, existing, "updated")
        else:
            # Add new device
            self.devices[device_id] = device_info
            logger.info(f"Registered new device: {device_id} ({device_info.device_type.name})")
            self._notify_callbacks(device_id, device_info, "discovered")
        
        return device_id
    
    def _parse_capabilities(self, attributes: Dict[str, str]) -> DeviceCapabilities:
        """Parse capabilities from discovery attributes."""
        capabilities = DeviceCapabilities()
        
        # Parse capabilities string (comma-separated)
        caps_str = attributes.get("capabilities", "")
        caps_list = [cap.strip() for cap in caps_str.split(",") if cap.strip()]
        
        # Map capability strings to boolean flags
        capabilities.supports_rgb_camera = "rgb_camera" in caps_list
        capabilities.supports_thermal_camera = "thermal_camera" in caps_list
        capabilities.supports_gsr_sensor = "gsr_sensor" in caps_list
        capabilities.supports_file_transfer = "file_transfer" in caps_list
        capabilities.supports_time_sync = "time_sync" in caps_list
        
        # Parse resolution
        capabilities.max_resolution = attributes.get("max_resolution", "1080p")
        
        # Parse sampling rates
        rates_str = attributes.get("sampling_rates", "")
        if rates_str:
            try:
                capabilities.sampling_rates = [int(r.strip()) for r in rates_str.split(",")]
            except ValueError:
                capabilities.sampling_rates = []
        
        return capabilities
    
    def update_device_state(self, device_id: str, new_state: DeviceConnectionState) -> bool:
        """
        Update device connection state.
        
        Args:
            device_id: ID of device to update
            new_state: New connection state
            
        Returns:
            True if update successful, False if device not found
        """
        if device_id not in self.devices:
            logger.warning(f"Cannot update state for unknown device: {device_id}")
            return False
        
        device = self.devices[device_id]
        old_state = device.state
        device.state = new_state
        
        # Update connection timestamps
        if new_state == DeviceConnectionState.ONLINE:
            device.connected_at = datetime.now(timezone.utc)
            device.total_connections += 1
        elif old_state == DeviceConnectionState.ONLINE and new_state == DeviceConnectionState.DISCONNECTED:
            device.total_disconnections += 1
        
        logger.info(f"Device {device_id} state changed: {old_state.value} -> {new_state.value}")
        self._notify_callbacks(device_id, device, "state_changed")
        
        return True
    
    def update_heartbeat(self, device_id: str) -> bool:
        """
        Update device heartbeat timestamp.
        
        Args:
            device_id: ID of device
            
        Returns:
            True if update successful, False if device not found
        """
        if device_id not in self.devices:
            return False
        
        self.devices[device_id].last_heartbeat = datetime.now(timezone.utc)
        return True
    
    def remove_device(self, device_id: str, reason: str = "manual") -> bool:
        """
        Remove device from registry.
        
        Args:
            device_id: ID of device to remove
            reason: Reason for removal
            
        Returns:
            True if removal successful, False if device not found
        """
        if device_id not in self.devices:
            return False
        
        device_info = self.devices[device_id]
        del self.devices[device_id]
        
        logger.info(f"Removed device {device_id} from registry. Reason: {reason}")
        self._notify_callbacks(device_id, device_info, "removed")
        
        return True
    
    def get_device(self, device_id: str) -> Optional[DeviceInfo]:
        """Get device information by ID."""
        return self.devices.get(device_id)
    
    def get_devices_by_state(self, state: DeviceConnectionState) -> List[DeviceInfo]:
        """Get all devices in a specific state."""
        return [device for device in self.devices.values() if device.state == state]
    
    def get_devices_by_type(self, device_type: DeviceType) -> List[DeviceInfo]:
        """Get all devices of a specific type."""
        return [device for device in self.devices.values() if device.device_type == device_type]
    
    def get_recording_devices(self) -> List[DeviceInfo]:
        """Get all devices currently recording."""
        return self.get_devices_by_state(DeviceConnectionState.RECORDING)
    
    def get_online_devices(self) -> List[DeviceInfo]:
        """Get all online devices."""
        return self.get_devices_by_state(DeviceConnectionState.ONLINE)
    
    def check_heartbeat_timeouts(self) -> List[str]:
        """
        Check for devices with heartbeat timeouts.
        
        Returns:
            List of device IDs that have timed out
        """
        current_time = datetime.now()
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
        """Get all registered devices."""
        return self.devices.copy()
    
    def get_device_count(self) -> int:
        """Get total number of registered devices."""
        return len(self.devices)
    
    def get_device_count_by_state(self, state: DeviceConnectionState) -> int:
        """Get count of devices in a specific state."""
        return len(self.get_devices_by_state(state))


class DeviceManager:
    """
    High-level device manager coordinating discovery and registry.
    Implements the device discovery and registration requirements from the MVP checklist.
    """
    
    def __init__(self):
        """Initialize the device manager."""
        self.registry = DeviceRegistry()
        self.discovery_service = NetworkDiscoveryService()
        self._running = False
        self._monitoring_task: Optional[asyncio.Task] = None
        
        # Setup discovery callbacks
        self.discovery_service.add_discovery_listener(self._on_device_discovered)
        self.discovery_service.add_discovery_listener(self._on_device_lost)
    
    async def start(self) -> bool:
        """
        Start device management services.
        
        Returns:
            True if started successfully
        """
        try:
            # Start discovery service
            if not await self.discovery_service.start_discovery():
                logger.error("Failed to start discovery service")
                return False
            
            # Start monitoring
            self._running = True
            self._monitoring_task = asyncio.create_task(self._monitoring_loop())
            
            logger.info("Device manager started successfully")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start device manager: {e}")
            return False
    
    async def stop(self) -> None:
        """Stop device management services."""
        try:
            self._running = False
            
            # Stop monitoring task
            if self._monitoring_task:
                self._monitoring_task.cancel()
                try:
                    await self._monitoring_task
                except asyncio.CancelledError:
                    pass
            
            # Stop discovery service
            await self.discovery_service.stop_discovery()
            
            logger.info("Device manager stopped")
            
        except Exception as e:
            logger.error(f"Error stopping device manager: {e}")
    
    async def _monitoring_loop(self) -> None:
        """Background monitoring loop for device health."""
        while self._running:
            try:
                # Check for heartbeat timeouts
                timed_out_devices = self.registry.check_heartbeat_timeouts()
                
                # Mark timed out devices as disconnected
                for device_id in timed_out_devices:
                    self.registry.update_device_state(device_id, DeviceConnectionState.DISCONNECTED)
                
                # Sleep before next check
                await asyncio.sleep(5.0)  # Check every 5 seconds
                
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in device monitoring loop: {e}")
                await asyncio.sleep(5.0)
    
    def _on_device_discovered(self, event_type: str, discovered_device: DiscoveredDevice) -> None:
        """Handle device discovery events."""
        if event_type == "discovered":  # Fix: Changed from "device_discovered" to "discovered"
            device_id = self.registry.register_device(discovered_device)
            logger.info(f"Device discovered and registered: {device_id}")
    
    def _on_device_lost(self, event_type: str, lost_device: DiscoveredDevice) -> None:
        """Handle device loss events."""
        if event_type == "lost":  # Fix: Changed from "device_lost" to "lost"
            # Fix: Parameter is now DiscoveredDevice, not service_name string
            service_name = lost_device.service_name
            # Find device by service name
            for device_id, device in self.registry.get_all_devices().items():
                if device.device_name == service_name:
                    self.registry.update_device_state(device_id, DeviceConnectionState.DISCONNECTED)
                    logger.info(f"Device lost: {device_id}")
                    break
    
    def add_device_manually(self, ip_address: str, port: int, device_name: str) -> Optional[str]:
        """
        Manually add device if mDNS fails.
        
        Args:
            ip_address: Device IP address
            port: Device port
            device_name: Device name
            
        Returns:
            Device ID if added successfully, None otherwise
        """
        try:
            # Create discovered device manually
            discovered_device = DiscoveredDevice(
                service_name=device_name,
                service_type="_ircamera._tcp.local.",
                ip_address=ip_address,
                port=port,
                device_type=DeviceType.ANDROID_SENSOR_NODE,  # Default type
                attributes={},
                discovered_at=datetime.now(),
                last_seen=datetime.now()
            )
            
            device_id = self.registry.register_device(discovered_device)
            logger.info(f"Manually added device: {device_id}")
            return device_id
            
        except Exception as e:
            logger.error(f"Failed to manually add device: {e}")
            return None
    
    async def refresh_discovery(self) -> None:
        """Refresh device discovery."""
        await self.discovery_service.refresh_discovery()
    
    def get_registry(self) -> DeviceRegistry:
        """Get the device registry."""
        return self.registry
    
    def add_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:
        """Add callback for device status changes."""
        self.registry.add_status_callback(callback)
    
    def remove_status_callback(self, callback: Callable[[str, DeviceInfo, str], None]) -> None:
        """Remove device status callback."""
        self.registry.remove_status_callback(callback)
    
    async def connect_to_device(self, device_id: str) -> bool:
        """
        Connect to a specific device.
        
        Args:
            device_id: ID of the device to connect to
            
        Returns:
            True if connection successful, False otherwise
        """
        try:
            device_info = self.registry.get_device(device_id)
            if not device_info:
                logger.error(f"Device not found in registry: {device_id}")
                return False
            
            # Update device state to connecting
            self.registry.update_device_state(device_id, DeviceConnectionState.ONLINE)
            
            # TODO: Implement actual network connection logic
            # For now, just mark as connected if device exists
            logger.info(f"Connected to device: {device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to connect to device {device_id}: {e}")
            self.registry.update_device_state(device_id, DeviceConnectionState.ERROR)
            return False