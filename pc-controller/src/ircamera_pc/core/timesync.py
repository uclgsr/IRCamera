"""
Time Synchronization Service for IRCamera PC Controller

Provides SNTP-like time synchronization service for Android devices.
Implements FR3: Time Synchronisation Service requirements.
"""

import asyncio
import struct
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from .config import config


@dataclass
class TimeSyncStats:
    """Time synchronization statistics for a device."""

    device_id: str
    last_sync: Optional[datetime] = None
    offset_ms: float = 0.0
    round_trip_ms: float = 0.0
    sync_count: int = 0
    median_offset_ms: float = 0.0
    p95_offset_ms: float = 0.0
    recent_offsets: List[float] = None

    def __post_init__(self):
        if self.recent_offsets is None:
            self.recent_offsets = []


class TimeSyncService:
    """
    Time synchronization service for maintaining clock synchronization across devices.

    Implements the Time Synchronisation Service functional requirement (FR3):
    - Runs SNTP-like service for devices to calibrate against PC's clock
    - Maintains millisecond-level accuracy (target: 5ms median, 15ms p95)
    - Provides periodic synchronization and statistics
    """

    def __init__(self):
        """Initialize time synchronization service."""
        self._server_socket: Optional[asyncio.DatagramTransport] = None
        self._protocol: Optional[TimeSyncProtocol] = None
        self._device_stats: Dict[str, TimeSyncStats] = {}
        self._is_running = False

        # Configuration
        self._sync_interval = config.get("time_sync.sync_interval", 30)
        self._target_accuracy_ms = config.get("time_sync.target_accuracy_ms", 5)
        self._max_offset_ms = config.get("time_sync.max_offset_ms", 15)
        self._history_size = 100  # Keep last 100 sync measurements

        logger.info("Time Synchronization Service initialized")

    async def start(self, host: str = "127.0.0.1", port: int = 8123) -> None:
        """
        Start the time synchronization service.

        Args:
            host: Host to bind to
            port: Port to bind to
        """
        if self._is_running:
            logger.warning("Time sync service is already running")
            return

        try:
            loop = asyncio.get_event_loop()

            # Create UDP server
            transport, protocol = await loop.create_datagram_endpoint(
                lambda: TimeSyncProtocol(self), local_addr=(host, port)
            )

            self._server_socket = transport
            self._protocol = protocol
            self._is_running = True

            logger.info(f"Time sync service started on {host}:{port}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to start time sync service: {e}")
            raise

    async def stop(self) -> None:
        """Stop the time synchronization service."""
        if not self._is_running:
            return

        if self._server_socket:
            self._server_socket.close()

        self._is_running = False
        logger.info("Time sync service stopped")

    def handle_sync_request(
        self, device_id: str, request_data: bytes, addr: Tuple[str, int]
    ) -> bytes:
        """
        Handle time synchronization request from device.

        Args:
            device_id: Device identifier
            request_data: Request data from device
            addr: Device address

        Returns:
            Response data with time information
        """
        try:
            # Parse request
            if len(request_data) < 16:
                logger.warning(f"Invalid sync request from {device_id}: too short")
                return b""

            # Extract client timestamp (when request was sent)
            client_send_time = struct.unpack("!Q", request_data[:8])[0] / 1000.0

            # Get current time
            server_time = time.time()
            server_time_ms = int(server_time * 1000)

            # Create response
            response = struct.pack(
                "!QQ",
                int(client_send_time * 1000),
                server_time_ms,  # Echo client time
            )  # Server time

            # Update statistics
            self._update_device_stats(device_id, client_send_time, server_time)

            logger.debug(f"Time sync response sent to {device_id} at {addr}")
            return response

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error handling sync request from {device_id}: {e}")
            return b""

    def _update_device_stats(
        self, device_id: str, client_time: float, server_time: float
    ) -> None:
        """Update synchronization statistics for device."""
        if device_id not in self._device_stats:
            self._device_stats[device_id] = TimeSyncStats(device_id=device_id)

        stats = self._device_stats[device_id]

        # Calculate offset (positive means client is ahead)
        offset_ms = (client_time - server_time) * 1000

        # Update stats
        stats.last_sync = datetime.now(timezone.utc)
        stats.offset_ms = offset_ms
        stats.sync_count += 1

        # Maintain history of recent offsets
        stats.recent_offsets.append(abs(offset_ms))
        if len(stats.recent_offsets) > self._history_size:
            stats.recent_offsets.pop(0)

        # Calculate median and p95
        if stats.recent_offsets:
            sorted_offsets = sorted(stats.recent_offsets)
            n = len(sorted_offsets)

            # Median
            if n % 2 == 0:
                stats.median_offset_ms = (
                    sorted_offsets[n // 2 - 1] + sorted_offsets[n // 2]
                ) / 2
            else:
                stats.median_offset_ms = sorted_offsets[n // 2]

            # P95
            p95_index = int(0.95 * n)
            stats.p95_offset_ms = sorted_offsets[min(p95_index, n - 1)]

        # Log accuracy warnings
        if stats.median_offset_ms > self._target_accuracy_ms:
            logger.warning(
                f"Device {device_id} median offset"
                "{stats.median_offset_ms:.1f}ms"
                f"exceeds target {self._target_accuracy_ms}ms"
            )

        if stats.p95_offset_ms > self._max_offset_ms:
            logger.warning(
                f"Device {device_id} p95 offset {stats.p95_offset_ms:.1f}ms "
                f"exceeds threshold {self._max_offset_ms}ms"
            )

        logger.debug(
            f"Time sync stats for {device_id}: "
            f"offset={offset_ms:.1f}ms, "
            f"median={stats.median_offset_ms:.1f}ms, "
            f"p95={stats.p95_offset_ms:.1f}ms"
        )

    def get_device_stats(self, device_id: str) -> Optional[TimeSyncStats]:
        """Get synchronization statistics for device."""
        return self._device_stats.get(device_id)

    def get_all_stats(self) -> Dict[str, TimeSyncStats]:
        """Get synchronization statistics for all devices."""
        return self._device_stats.copy()

    def is_device_synchronized(self, device_id: str) -> bool:
        """
        Check if device is properly synchronized.

        Args:
            device_id: Device identifier

        Returns:
            True if device meets synchronization criteria
        """
        stats = self._device_stats.get(device_id)
        if not stats or not stats.last_sync:
            return False

        # Check if sync is recent
        time_since_sync = (datetime.now(timezone.utc) - stats.last_sync).total_seconds()
        if time_since_sync > self._sync_interval * 2:
            return False

        # Check accuracy
        return (
            stats.median_offset_ms <= self._target_accuracy_ms
            and stats.p95_offset_ms <= self._max_offset_ms
        )

    def get_synchronization_quality(self) -> Dict[str, any]:
        """
        Get overall synchronization quality metrics.

        Returns:
            Dictionary with quality metrics
        """
        if not self._device_stats:
            return {
                "total_devices": 0,
                "synchronized_devices": 0,
                "synchronization_rate": 0.0,
                "overall_median_offset_ms": 0.0,
                "overall_p95_offset_ms": 0.0,
            }

        synchronized_count = sum(
            1
            for device_id in self._device_stats
            if self.is_device_synchronized(device_id)
        )

        # Calculate overall metrics
        all_offsets = []
        for stats in self._device_stats.values():
            all_offsets.extend(stats.recent_offsets)

        overall_median = 0.0
        overall_p95 = 0.0

        if all_offsets:
            sorted_offsets = sorted(all_offsets)
            n = len(sorted_offsets)

            # Overall median
            if n % 2 == 0:
                overall_median = (
                    sorted_offsets[n // 2 - 1] + sorted_offsets[n // 2]
                ) / 2
            else:
                overall_median = sorted_offsets[n // 2]

            # Overall P95
            p95_index = int(0.95 * n)
            overall_p95 = sorted_offsets[min(p95_index, n - 1)]

        return {
            "total_devices": len(self._device_stats),
            "synchronized_devices": synchronized_count,
            "synchronization_rate": (
                synchronized_count / len(self._device_stats)
                if self._device_stats
                else 0.0
            ),
            "overall_median_offset_ms": overall_median,
            "overall_p95_offset_ms": overall_p95,
        }

    @property
    def is_running(self) -> bool:
        """Check if service is running."""
        return self._is_running


class TimeSyncProtocol(asyncio.DatagramProtocol):
    """UDP protocol for time synchronization."""

    def __init__(self, service: TimeSyncService):
        """Initialize protocol with reference to service."""
        self.service = service
        self.transport: Optional[asyncio.DatagramTransport] = None

    def connection_made(self, transport: asyncio.DatagramTransport) -> None:
        """Called when connection is made."""
        self.transport = transport
        logger.debug("Time sync protocol connection made")

    def datagram_received(self, data: bytes, addr: Tuple[str, int]) -> None:
        """
        Handle received datagram.

        Args:
            data: Received data
            addr: Sender address
        """
        try:
            # Extract device ID from data
            if len(data) < 16:
                logger.warning(f"Invalid time sync request from {addr}: too short")
                return

            # Simple protocol: first 8 bytes timestamp, next 8 bytes device ID hash
            device_id_hash = struct.unpack("!Q", data[8:16])[0]
            device_id = f"device_{device_id_hash:016x}"

            # Handle sync request
            response = self.service.handle_sync_request(device_id, data, addr)

            if response and self.transport:
                self.transport.sendto(response, addr)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error processing time sync datagram from {addr}: {e}")

    def error_received(self, exc: Exception) -> None:
        """Handle protocol errors."""
        logger.error(f"Time sync protocol error: {exc}")

    def connection_lost(self, exc: Optional[Exception]) -> None:
        """Handle connection lost."""
        if exc:
            logger.error(f"Time sync connection lost: {exc}")
        else:
            logger.debug("Time sync connection closed")
