"""
Advanced Time Synchronization Service - PC Controller Side

Implements NTP-like time synchronization protocol to match Android TimeManager
for precise temporal alignment in the Hub-and-Spoke architecture.
"""

import asyncio
import json
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple, Set
from collections import deque, defaultdict
import statistics

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger


@dataclass
class TimeSyncMeasurement:
    """Individual time synchronization measurement."""

    t1_client_send: float  # Client send timestamp
    t2_server_receive: float  # Server receive timestamp  
    t3_server_send: float  # Server send timestamp
    t4_client_receive: Optional[float] = None  # Client receive timestamp (if available)

    round_trip_time_ns: Optional[float] = None
    clock_offset_ns: Optional[float] = None
    network_delay_ns: Optional[float] = None
    measurement_time: float = field(default_factory=time.time)

    def calculate_ntp_values(self):
        """Calculate NTP-style timing values if all timestamps are available."""
        if self.t4_client_receive is not None:
            # Full 4-timestamp NTP calculation
            self.round_trip_time_ns = (self.t4_client_receive - self.t1_client_send)
            self.network_delay_ns = self.round_trip_time_ns / 2
            self.clock_offset_ns = ((self.t2_server_receive - self.t1_client_send) +
                                    (self.t3_server_send - self.t4_client_receive)) / 2
        else:
            # Simple 2-timestamp calculation
            self.clock_offset_ns = self.t2_server_receive - self.t1_client_send


@dataclass
class DeviceTimeSyncStats:
    """Time synchronization statistics for a device."""

    device_id: str
    last_sync_time: Optional[float] = None
    sync_count: int = 0

    # Current sync quality
    current_offset_ns: float = 0.0
    current_rtt_ms: float = 0.0

    # Historical measurements (keep last 100)
    measurements: deque = field(default_factory=lambda: deque(maxlen=100))

    # Quality statistics
    median_offset_ms: float = 0.0
    p95_offset_ms: float = 0.0
    median_rtt_ms: float = 0.0
    offset_stability_ms: float = 0.0  # Standard deviation of offsets

    # Sync quality assessment
    is_synchronized: bool = False
    sync_quality: str = "UNKNOWN"  # EXCELLENT, GOOD, FAIR, POOR

    def update_stats(self, stale_threshold_s: int = 60):
        """Update statistical measures from recent measurements."""
        if not self.measurements:
            return

        # Extract values
        offsets_ms = []
        rtts_ms = []

        for measurement in self.measurements:
            if measurement.clock_offset_ns is not None:
                offsets_ms.append(abs(measurement.clock_offset_ns) / 1_000_000)
            if measurement.round_trip_time_ns is not None:
                rtts_ms.append(measurement.round_trip_time_ns / 1_000_000)

        # Calculate statistics
        if offsets_ms:
            self.median_offset_ms = statistics.median(offsets_ms)
            if len(offsets_ms) >= 20:  # Need enough samples for P95
                self.p95_offset_ms = statistics.quantiles(offsets_ms, n=20)[18]  # 95th percentile
            else:
                self.p95_offset_ms = max(offsets_ms)

            # Calculate stability (standard deviation)
            if len(offsets_ms) > 1:
                self.offset_stability_ms = statistics.stdev(offsets_ms)

        if rtts_ms:
            self.median_rtt_ms = statistics.median(rtts_ms)

        # Assess sync quality
        self._assess_sync_quality(stale_threshold_s)

    def _assess_sync_quality(self, stale_threshold_s: int = 60):
        """Assess synchronization quality based on current metrics."""
        # Check if recently synced (configurable threshold)
        if (self.last_sync_time is None or
                time.time() - self.last_sync_time > stale_threshold_s):
            self.is_synchronized = False
            self.sync_quality = "STALE"
            return

        # Quality based on median offset
        if self.median_offset_ms <= 2.0:
            self.sync_quality = "EXCELLENT"
            self.is_synchronized = True
        elif self.median_offset_ms <= 5.0:
            self.sync_quality = "GOOD"
            self.is_synchronized = True
        elif self.median_offset_ms <= 10.0:
            self.sync_quality = "FAIR"
            self.is_synchronized = True
        else:
            self.sync_quality = "POOR"
            self.is_synchronized = False


class AdvancedTimeSyncService:
    """
    Advanced Time Synchronization Service for PC Controller Hub.
    
    Implements comprehensive NTP-like protocol matching Android TimeManager
    to achieve <5ms clock offset precision (not round-trip time measurement accuracy) across Hub-Spoke architecture.
    """

    def __init__(self, stale_threshold_s: int = 60):
        """Initialize advanced time synchronization service."""
        self._device_stats: Dict[str, DeviceTimeSyncStats] = {}
        self._active_sessions: Dict[str, Set[str]] = defaultdict(
            set)  # session_id -> set of device_ids
        self._is_running = False

        # Configurable sync quality thresholds
        self._stale_threshold_s = stale_threshold_s
        self._target_accuracy_ms = 5.0
        self._excellent_threshold_ms = 2.0
        self._good_threshold_ms = 5.0
        self._fair_threshold_ms = 10.0

        # Statistics monitoring
        self._stats_update_task: Optional[asyncio.Task] = None

        logger.info("Advanced Time Synchronization Service initialized")

    async def start(self) -> None:
        """Start the advanced time synchronization service."""
        if self._is_running:
            logger.warning("Advanced time sync service already running")
            return

        self._is_running = True

        # Start periodic statistics update
        self._stats_update_task = asyncio.create_task(self._periodic_stats_update())

        logger.info("Advanced Time Synchronization Service started")

    async def stop(self) -> None:
        """Stop the advanced time synchronization service."""
        if not self._is_running:
            return

        self._is_running = False

        if self._stats_update_task:
            self._stats_update_task.cancel()
            try:
                await self._stats_update_task
            except asyncio.CancelledError:
                pass

        logger.info("Advanced Time Synchronization Service stopped")

    async def handle_time_sync_request(self, message: Dict[str, any], device_id: str) -> Dict[
        str, any]:
        """
        Handle time synchronization request with full NTP-like protocol.
        
        Args:
            message: Sync request message with client_timestamp
            device_id: Device identifier
            
        Returns:
            Time sync response with server timestamps
        """
        try:
            # Extract client send timestamp (t1)
            client_timestamp = message.get("client_timestamp")
            if client_timestamp is None:
                return {
                    "message_type": "error",
                    "error": "Missing client_timestamp"
                }

            # Record server receive time (t2) - when we received the request
            t2_server_receive = time.time_ns()

            # Create measurement record
            measurement = TimeSyncMeasurement(
                t1_client_send=client_timestamp,
                t2_server_receive=t2_server_receive
            )

            # Record server send time (t3) - when we send the response
            t3_server_send = time.time_ns()
            measurement.t3_server_send = t3_server_send

            # Store measurement for this device
            self._record_measurement(device_id, measurement)

            # Create NTP-like response
            response = {
                "message_type": "time_sync_response",
                "client_timestamp": client_timestamp,  # Echo t1
                "server_receive_time": t2_server_receive,  # t2
                "server_send_time": t3_server_send,  # t3
                "server_timestamp": t3_server_send,  # For compatibility
                "processing_delay_ns": t3_server_send - t2_server_receive,
                "sync_session_id": message.get("session_id", "unknown")
            }

            logger.debug(f"Time sync response for {device_id}: " +
                         f"t1={client_timestamp}, t2={t2_server_receive}, t3={t3_server_send}")

            return response

        except Exception as e:
            logger.error(f"Error handling time sync request from {device_id}: {e}")
            return {
                "message_type": "error",
                "error": f"Time sync processing error: {e}"
            }

    def _record_measurement(self, device_id: str, measurement: TimeSyncMeasurement) -> None:
        """Record time sync measurement for device."""
        # Get or create device stats
        if device_id not in self._device_stats:
            self._device_stats[device_id] = DeviceTimeSyncStats(device_id=device_id)

        stats = self._device_stats[device_id]

        # Add measurement and update counters
        stats.measurements.append(measurement)
        stats.sync_count += 1
        stats.last_sync_time = time.time()

        # Update current values
        if measurement.clock_offset_ns is not None:
            stats.current_offset_ns = measurement.clock_offset_ns
        if measurement.round_trip_time_ns is not None:
            stats.current_rtt_ms = measurement.round_trip_time_ns / 1_000_000

        # Update statistics
        stats.update_stats()

        # Log sync quality if poor
        if not stats.is_synchronized:
            logger.warning(f"Device {device_id} sync quality {stats.sync_quality}: " +
                           f"offset={stats.median_offset_ms:.1f}ms")
        elif stats.sync_quality in ["EXCELLENT", "GOOD"]:
            logger.debug(f"Device {device_id} sync quality {stats.sync_quality}: " +
                         f"offset={stats.median_offset_ms:.1f}ms")

    async def register_session(self, session_id: str, device_id: str) -> bool:
        """Register a session for time synchronization tracking."""
        try:
            self._active_sessions[session_id].add(device_id)

            # Initialize device stats if needed
            if device_id not in self._device_stats:
                self._device_stats[device_id] = DeviceTimeSyncStats(device_id=device_id)

            logger.info(f"Registered time sync session {session_id} for device {device_id}")
            return True

        except Exception as e:
            logger.error(f"Error registering sync session {session_id}: {e}")
            return False

    async def end_session(self, session_id: str) -> bool:
        """End a time synchronization session for all devices."""
        try:
            device_ids = self._active_sessions.pop(session_id, set())
            if device_ids:
                logger.info(f"Ended time sync session {session_id} for devices {list(device_ids)}")
                return True
            return False

        except Exception as e:
            logger.error(f"Error ending sync session {session_id}: {e}")
            return False

    def get_device_sync_stats(self, device_id: str) -> Optional[DeviceTimeSyncStats]:
        """Get time synchronization statistics for a device."""
        return self._device_stats.get(device_id)

    def get_all_sync_stats(self) -> Dict[str, DeviceTimeSyncStats]:
        """Get time synchronization statistics for all devices."""
        return self._device_stats.copy()

    def get_sync_quality_summary(self) -> Dict[str, any]:
        """Get overall synchronization quality summary."""
        if not self._device_stats:
            return {
                "total_devices": 0,
                "synchronized_devices": 0,
                "sync_rate": 0.0,
                "quality_distribution": {},
                "overall_median_offset_ms": 0.0,
                "target_compliance": False
            }

        # Count by quality level
        quality_counts = {"EXCELLENT": 0, "GOOD": 0, "FAIR": 0, "POOR": 0, "STALE": 0}
        synchronized_count = 0
        all_offsets = []

        for stats in self._device_stats.values():
            stats.update_stats()  # Ensure up-to-date
            quality_counts[stats.sync_quality] += 1

            if stats.is_synchronized:
                synchronized_count += 1

            if stats.median_offset_ms > 0:
                all_offsets.append(stats.median_offset_ms)

        # Calculate overall metrics  
        overall_median = statistics.median(all_offsets) if all_offsets else 0.0
        target_compliance = overall_median <= self._target_accuracy_ms

        return {
            "total_devices": len(self._device_stats),
            "synchronized_devices": synchronized_count,
            "sync_rate": synchronized_count / len(self._device_stats),
            "quality_distribution": quality_counts,
            "overall_median_offset_ms": overall_median,
            "target_compliance": target_compliance,
            "target_accuracy_ms": self._target_accuracy_ms
        }

    async def _periodic_stats_update(self) -> None:
        """Periodically update device statistics."""
        while self._is_running:
            try:
                # Update all device stats
                for stats in self._device_stats.values():
                    stats.update_stats()

                # Log summary every 5 minutes
                await asyncio.sleep(300)

                if self._device_stats:
                    summary = self.get_sync_quality_summary()
                    logger.info(
                        f"Time Sync Summary: {summary['synchronized_devices']}/{summary['total_devices']} " +
                        f"devices synchronized, overall offset: {summary['overall_median_offset_ms']:.1f}ms")

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in periodic stats update: {e}")
                await asyncio.sleep(60)  # Continue after error

    def is_device_synchronized(self, device_id: str, max_age_seconds: float = None) -> bool:
        """
        Check if device is properly synchronized within time tolerance.
        
        Args:
            device_id: Device identifier
            max_age_seconds: Maximum age of last sync in seconds (uses instance default if None)
            
        Returns:
            True if device is synchronized within requirements
        """
        stats = self._device_stats.get(device_id)
        if not stats:
            return False

        # Use instance default if not provided
        if max_age_seconds is None:
            max_age_seconds = self._stale_threshold_s

        # Check sync age
        if (stats.last_sync_time is None or
                time.time() - stats.last_sync_time > max_age_seconds):
            return False

        # Check sync quality
        return (stats.is_synchronized and
                stats.median_offset_ms <= self._target_accuracy_ms)

    @property
    def is_running(self) -> bool:
        """Check if service is running."""
        return self._is_running

    @property
    def active_sessions(self) -> Dict[str, str]:
        """Get active session mappings."""
        return self._active_sessions.copy()
