import asyncio
import time
from dataclasses import dataclass, field
from typing import Dict, Optional, Set
from collections import deque, defaultdict
import statistics

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger


@dataclass
class TimeSyncMeasurement:
    t1_client_send: float
    t2_server_receive: float
    t3_server_send: float
    t4_client_receive: Optional[float] = None

    round_trip_time_ns: Optional[float] = None
    clock_offset_ns: Optional[float] = None
    network_delay_ns: Optional[float] = None
    measurement_time: float = field(default_factory=time.time)

    def calculate_ntp_values(self):

        if self.t4_client_receive is not None:

            self.round_trip_time_ns = (self.t4_client_receive - self.t1_client_send)
            self.network_delay_ns = self.round_trip_time_ns / 2
            self.clock_offset_ns = ((self.t2_server_receive - self.t1_client_send) +
                                    (self.t3_server_send - self.t4_client_receive)) / 2
        else:

            self.clock_offset_ns = self.t2_server_receive - self.t1_client_send


@dataclass
class DeviceTimeSyncStats:
    device_id: str
    last_sync_time: Optional[float] = None
    sync_count: int = 0

    current_offset_ns: float = 0.0
    current_rtt_ms: float = 0.0

    measurements: deque = field(default_factory=lambda: deque(maxlen=100))

    median_offset_ms: float = 0.0
    p95_offset_ms: float = 0.0
    median_rtt_ms: float = 0.0
    offset_stability_ms: float = 0.0

    is_synchronized: bool = False
    sync_quality: str = "UNKNOWN"

    def update_stats(self, stale_threshold_s: int = 60):

        if not self.measurements:
            return

        offsets_ms = []
        rtts_ms = []

        for measurement in self.measurements:
            if measurement.clock_offset_ns is not None:
                offsets_ms.append(abs(measurement.clock_offset_ns) / 1_000_000)
            if measurement.round_trip_time_ns is not None:
                rtts_ms.append(measurement.round_trip_time_ns / 1_000_000)

        if offsets_ms:
            self.median_offset_ms = statistics.median(offsets_ms)
            if len(offsets_ms) >= 20:
                self.p95_offset_ms = statistics.quantiles(offsets_ms, n=20)[18]
            else:
                self.p95_offset_ms = max(offsets_ms)

            if len(offsets_ms) > 1:
                self.offset_stability_ms = statistics.stdev(offsets_ms)

        if rtts_ms:
            self.median_rtt_ms = statistics.median(rtts_ms)

        self._assess_sync_quality(stale_threshold_s)

    def _assess_sync_quality(self, stale_threshold_s: int = 60):

        if (self.last_sync_time is None or
                time.time() - self.last_sync_time > stale_threshold_s):
            self.is_synchronized = False
            self.sync_quality = "STALE"
            return

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

    def __init__(self, stale_threshold_s: int = 60):

        self._device_stats: Dict[str, DeviceTimeSyncStats] = {}
        self._active_sessions: Dict[str, Set[str]] = defaultdict(
            set)
        self._is_running = False

        self._stale_threshold_s = stale_threshold_s
        self._target_accuracy_ms = 5.0
        self._excellent_threshold_ms = 2.0
        self._good_threshold_ms = 5.0
        self._fair_threshold_ms = 10.0

        self._stats_update_task: Optional[asyncio.Task] = None

        logger.info("Advanced Time Synchronization Service initialized")

    async def start(self) -> None:

        if self._is_running:
            logger.warning("Advanced time sync service already running")
            return

        self._is_running = True

        self._stats_update_task = asyncio.create_task(self._periodic_stats_update())

        logger.info("Advanced Time Synchronization Service started")

    async def stop(self) -> None:

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

        try:

            client_timestamp = message.get("client_timestamp")
            if client_timestamp is None:
                return {
                    "message_type": "error",
                    "error": "Missing client_timestamp"
                }

            t2_server_receive = time.time_ns()

            measurement = TimeSyncMeasurement(
                t1_client_send=client_timestamp,
                t2_server_receive=t2_server_receive
            )

            t3_server_send = time.time_ns()
            measurement.t3_server_send = t3_server_send

            self._record_measurement(device_id, measurement)

            response = {
                "message_type": "time_sync_response",
                "client_timestamp": client_timestamp,
                "server_receive_time": t2_server_receive,
                "server_send_time": t3_server_send,
                "server_timestamp": t3_server_send,
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

        if device_id not in self._device_stats:
            self._device_stats[device_id] = DeviceTimeSyncStats(device_id=device_id)

        stats = self._device_stats[device_id]

        stats.measurements.append(measurement)
        stats.sync_count += 1
        stats.last_sync_time = time.time()

        if measurement.clock_offset_ns is not None:
            stats.current_offset_ns = measurement.clock_offset_ns
        if measurement.round_trip_time_ns is not None:
            stats.current_rtt_ms = measurement.round_trip_time_ns / 1_000_000

        stats.update_stats()

        if not stats.is_synchronized:
            logger.warning(f"Device {device_id} sync quality {stats.sync_quality}: " +
                           f"offset={stats.median_offset_ms:.1f}ms")
        elif stats.sync_quality in ["EXCELLENT", "GOOD"]:
            logger.debug(f"Device {device_id} sync quality {stats.sync_quality}: " +
                         f"offset={stats.median_offset_ms:.1f}ms")

    async def register_session(self, session_id: str, device_id: str) -> bool:

        try:
            self._active_sessions[session_id].add(device_id)

            if device_id not in self._device_stats:
                self._device_stats[device_id] = DeviceTimeSyncStats(device_id=device_id)

            logger.info(f"Registered time sync session {session_id} for device {device_id}")
            return True

        except Exception as e:
            logger.error(f"Error registering sync session {session_id}: {e}")
            return False

    async def end_session(self, session_id: str) -> bool:

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

        return self._device_stats.get(device_id)

    def get_all_sync_stats(self) -> Dict[str, DeviceTimeSyncStats]:

        return self._device_stats.copy()

    def get_sync_quality_summary(self) -> Dict[str, any]:

        if not self._device_stats:
            return {
                "total_devices": 0,
                "synchronized_devices": 0,
                "sync_rate": 0.0,
                "quality_distribution": {},
                "overall_median_offset_ms": 0.0,
                "target_compliance": False
            }

        quality_counts = {"EXCELLENT": 0, "GOOD": 0, "FAIR": 0, "POOR": 0, "STALE": 0}
        synchronized_count = 0
        all_offsets = []

        for stats in self._device_stats.values():
            stats.update_stats()
            quality_counts[stats.sync_quality] += 1

            if stats.is_synchronized:
                synchronized_count += 1

            if stats.median_offset_ms > 0:
                all_offsets.append(stats.median_offset_ms)

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

        while self._is_running:
            try:

                for stats in self._device_stats.values():
                    stats.update_stats()

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
                await asyncio.sleep(60)

    def is_device_synchronized(self, device_id: str, max_age_seconds: float = None) -> bool:

        stats = self._device_stats.get(device_id)
        if not stats:
            return False

        if max_age_seconds is None:
            max_age_seconds = self._stale_threshold_s

        if (stats.last_sync_time is None or
                time.time() - stats.last_sync_time > max_age_seconds):
            return False

        return (stats.is_synchronized and
                stats.median_offset_ms <= self._target_accuracy_ms)

    @property
    def is_running(self) -> bool:

        return self._is_running

    @property
    def active_sessions(self) -> Dict[str, str]:

        return self._active_sessions.copy()
