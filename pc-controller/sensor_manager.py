#!/usr/bin/env python3
"""
Sensor orchestration helpers for the PC controller.

This module focuses on FR1/FR2 requirements:
    * Discovery and control of multiple Shimmer3 (GSR) sensors over BLE.
    * Optional simulation mode that produces deterministic dummy data when no
      physical hardware is present.
    * A single coordination point that the rest of the PC controller can use
      to start/stop streaming in lock-step with Android devices.

The implementation intentionally hides hardware specifics behind lightweight
protocol-neutral abstractions so that new sensors can be introduced without
touching the controller core. Real Shimmer hardware support relies on the
`bleak` package; if it is not available the module degrades gracefully to
simulation-only mode.
"""

from __future__ import annotations

import logging
import math
import threading
import time
from dataclasses import dataclass, field
from typing import Callable, Dict, Iterable, Optional

try:  # Optional dependency for BLE access
    from bleak import BleakClient, BleakScanner  # type: ignore

    BLE_AVAILABLE = True
except ImportError:  # pragma: no cover - exercised when bleak is missing
    BleakClient = object  # type: ignore
    BleakScanner = object  # type: ignore
    BLE_AVAILABLE = False

logger = logging.getLogger(__name__)

# Shimmer3 UUIDs (public information from vendor documentation)
SHIMMER_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
SHIMMER_STREAM_UUID = "0000fff4-0000-1000-8000-00805f9b34fb"

SampleCallback = Callable[[str, float, float], None]


@dataclass
class SensorConfig:
    """Metadata required to connect to a physical sensor."""

    name: str
    address: Optional[str] = None
    stream_uuid: str = SHIMMER_STREAM_UUID
    sample_rate_hz: float = 128.0
    metadata: Dict[str, str] = field(default_factory=dict)


class BaseSensorSource:
    """Shared functionality for physical and simulated sensor feeds."""

    def __init__(self, sensor_id: str, config: SensorConfig, callback: SampleCallback):
        self.sensor_id = sensor_id
        self.config = config
        self._callback = callback
        self._running = threading.Event()
        self._thread: Optional[threading.Thread] = None

    @property
    def is_running(self) -> bool:
        return self._running.is_set()

    def start(self) -> None:
        if self.is_running:
            return
        self._running.set()
        self._thread = threading.Thread(target=self._run, name=f"{self.sensor_id}-worker", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._running.clear()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=2.0)
        self._thread = None

    # ------------------------------------------------------------------ hooks
    def _run(self) -> None:  # pragma: no cover - implemented by subclasses
        raise NotImplementedError

    def _publish(self, timestamp: float, value: float) -> None:
        try:
            self._callback(self.sensor_id, timestamp, value)
        except Exception:  # pragma: no cover - defensive logging
            logger.exception("Sensor callback for %s failed", self.sensor_id)


class SimulatedGSRSensorSource(BaseSensorSource):
    """Simple deterministic data generator operating at the configured rate."""

    def __init__(self, sensor_id: str, config: SensorConfig, callback: SampleCallback):
        super().__init__(sensor_id, config, callback)
        self._phase = 0.0

    def _run(self) -> None:
        period = 1.0 / max(self.config.sample_rate_hz, 1.0)
        logger.info("Starting simulated sensor %s at %.2f Hz", self.sensor_id, self.config.sample_rate_hz)
        next_tick = time.perf_counter()
        while self._running.is_set():
            now = time.time()
            # Compose a composite waveform to keep the dataset interesting.
            base = 4.5 + 0.5 * math.sin(self._phase)
            modulation = 0.2 * math.sin(self._phase / 3.0)
            noise = 0.05 * math.sin(self._phase * 1.7)
            value = base + modulation + noise
            self._publish(now, value)

            self._phase += (2.0 * math.pi) * period / max(period, 1e-3)
            next_tick += period
            delay = next_tick - time.perf_counter()
            if delay > 0:
                time.sleep(delay)
            else:
                next_tick = time.perf_counter()

        logger.info("Stopped simulated sensor %s", self.sensor_id)


class Shimmer3SensorSource(BaseSensorSource):
    """
    BLE-backed Shimmer3 stream reader.

    The implementation listens for notifications on the configured characteristic
    and converts the micro-siemens payload to floating point values. The payload
    format is a 16-bit big-endian integer representing conductance*100.
    """

    def __init__(self, sensor_id: str, config: SensorConfig, callback: SampleCallback):
        if not BLE_AVAILABLE:
            raise RuntimeError("bleak is required for Shimmer3SensorSource")
        super().__init__(sensor_id, config, callback)
        self._client: Optional[BleakClient] = None

    def _run(self) -> None:
        assert BLE_AVAILABLE  # for mypy / readability
        address = self.config.address
        if not address:
            logger.info("No BLE address provided for %s, attempting auto discovery", self.sensor_id)
            address = self._discover_address()

        if not address:
            logger.error("Unable to locate Shimmer3 sensor %s", self.sensor_id)
            self._running.clear()
            return

        logger.info("Connecting to Shimmer3 sensor %s (%s)", self.sensor_id, address)
        loop = threading.Event()

        def handle_notification(_: int, data: bytearray) -> None:
            if not data:
                return
            timestamp = time.time()
            try:
                if len(data) >= 2:
                    raw = int.from_bytes(data[:2], byteorder="big", signed=False)
                    value = raw / 100.0
                else:
                    value = data[0]
            except Exception as exc:
                logger.warning("Failed to parse Shimmer payload from %s: %s", self.sensor_id, exc)
                return
            self._publish(timestamp, float(value))

        try:
            with BleakClient(address, timeout=15.0) as client:  # type: ignore[call-arg]
                self._client = client
                self._client.start_notify(self.config.stream_uuid, handle_notification)  # type: ignore[arg-type]
                loop.wait()  # Wait until stop() clears the flag
                self._client.stop_notify(self.config.stream_uuid)  # type: ignore[arg-type]
        except Exception as exc:
            logger.error("BLE error while streaming from %s: %s", self.sensor_id, exc)
        finally:
            self._client = None
            self._running.clear()
            loop.set()
            logger.info("Disconnected from Shimmer3 sensor %s", self.sensor_id)

    def stop(self) -> None:
        super().stop()
        if self._client:
            try:
                self._client.disconnect()  # type: ignore[attr-defined]
            except Exception:  # pragma: no cover - best effort cleanup
                logger.debug("Ignoring BLE disconnect failure for %s", self.sensor_id)

    def _discover_address(self) -> Optional[str]:
        assert BLE_AVAILABLE
        try:
            devices = BleakScanner.discover(timeout=5.0)  # type: ignore[attr-defined]
        except Exception as exc:
            logger.error("BLE discovery failed while locating %s: %s", self.sensor_id, exc)
            return None

        for device in devices:
            # Heuristic: Shimmer devices advertise "Shimmer" in the name.
            if "shimmer" in device.name.lower():  # type: ignore[attr-defined]
                return device.address  # type: ignore[attr-defined]
        return None


class SensorManager:
    """High-level registry that coordinates multiple sensor sources."""

    def __init__(self, callback: SampleCallback, config_map: Dict[str, SensorConfig]):
        self._callback = callback
        self._config_map = config_map
        self._sources: Dict[str, BaseSensorSource] = {}
        self._lock = threading.Lock()
        self._simulation_enabled = False

    # ---------------------------------------------------------------- utilities
    def ensure_sources(self) -> None:
        """Instantiate sensor sources as per configuration."""
        with self._lock:
            for sensor_id, config in self._config_map.items():
                if sensor_id in self._sources:
                    continue
                try:
                    source: BaseSensorSource
                    if config.address or BLE_AVAILABLE:
                        source = Shimmer3SensorSource(sensor_id, config, self._callback)
                    else:
                        source = SimulatedGSRSensorSource(sensor_id, config, self._callback)
                    self._sources[sensor_id] = source
                except Exception as exc:
                    logger.warning("Falling back to simulation for %s: %s", sensor_id, exc)
                    self._sources[sensor_id] = SimulatedGSRSensorSource(sensor_id, config, self._callback)

    def add_simulated_sensor(self, sensor_id: str, sample_rate_hz: float = 128.0) -> None:
        config = SensorConfig(name=f"Simulated {sensor_id}", sample_rate_hz=sample_rate_hz)
        with self._lock:
            self._sources[sensor_id] = SimulatedGSRSensorSource(sensor_id, config, self._callback)

    # ---------------------------------------------------------------- lifecycle
    def start_streaming(self) -> Iterable[str]:
        """Start available sensors and return the IDs that successfully started."""
        self.ensure_sources()
        started = []
        with self._lock:
            for sensor_id, source in self._sources.items():
                if not source.is_running:
                    source.start()
                started.append(sensor_id)
        return started

    def stop_streaming(self) -> None:
        with self._lock:
            for source in self._sources.values():
                source.stop()

    # ---------------------------------------------------------------- simulation
    def enable_simulation_mode(self) -> None:
        """Force all configured sensors to use simulation."""
        with self._lock:
            if self._simulation_enabled:
                return
            new_sources: Dict[str, BaseSensorSource] = {}
            for sensor_id, config in self._config_map.items():
                new_sources[sensor_id] = SimulatedGSRSensorSource(sensor_id, config, self._callback)
            self._sources = new_sources
            self._simulation_enabled = True
        logger.info("Simulation mode enabled for %d sensors", len(self._sources))

    def disable_simulation_mode(self) -> None:
        with self._lock:
            if not self._simulation_enabled:
                return
            self._sources.clear()
            self._simulation_enabled = False
        logger.info("Simulation mode disabled; physical sensors will reconnect on next start")

    def is_simulation_enabled(self) -> bool:
        with self._lock:
            return self._simulation_enabled

    # ---------------------------------------------------------------- telemetry
    def active_sensors(self) -> Dict[str, SensorConfig]:
        """Return a snapshot of known sensors and their configuration."""
        with self._lock:
            return {sensor_id: source.config for sensor_id, source in self._sources.items()}


def load_sensor_config(raw: Dict[str, Dict[str, object]]) -> Dict[str, SensorConfig]:
    """Utility helper to convert dict data (from YAML/JSON) into SensorConfig."""
    result: Dict[str, SensorConfig] = {}
    for sensor_id, cfg in raw.items():
        result[sensor_id] = SensorConfig(
            name=str(cfg.get("name", sensor_id)),
            address=cfg.get("address"),  # type: ignore[arg-type]
            stream_uuid=str(cfg.get("stream_uuid", SHIMMER_STREAM_UUID)),
            sample_rate_hz=float(cfg.get("sample_rate_hz", 128.0)),
            metadata={k: str(v) for k, v in cfg.get("metadata", {}).items()},  # type: ignore[dict-item]
        )
    return result


__all__ = [
    "BaseSensorSource",
    "SensorConfig",
    "SensorManager",
    "Shimmer3SensorSource",
    "SimulatedGSRSensorSource",
    "SampleCallback",
    "load_sensor_config",
]
