#!/usr/bin/env python3
"""
IRCamera PC Controller
======================

Desktop controller for the multi-modal sensing system. Provides:
  * TCP server with newline-delimited JSON framing (legacy text protocol is
    supported through `protocol_adapter.ProtocolAdapter`).
  * Device registry and session management.
  * Optional acceleration via the PyBind11 native backend for parsing Shimmer
    GSR packets and computing statistics.
  * Persistence helpers to store telemetry and uploaded artefacts for later
    export or analysis.
  * CLI entry point (GUI hooked in later in this file when PyQt6 is available).
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import logging
import shutil
import socket
import ssl
import threading
import time
import uuid
from collections import deque, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Deque, Dict, Iterable, List, Optional, Tuple

try:
    import yaml  # type: ignore
except ImportError:  # pragma: no cover - optional dependency
    yaml = None  # type: ignore[assignment]

try:
    import enhanced_native_backend as native_backend  # type: ignore

    NATIVE_BACKEND_AVAILABLE = True
except ImportError:  # pragma: no cover - optional dependency
    native_backend = None
    NATIVE_BACKEND_AVAILABLE = False

from protocol_adapter import ProtocolAdapter
from sensor_manager import SensorManager, SensorConfig, load_sensor_config
from stimulus_controller import StimulusController, StimulusEvent
from sync_handler import SyncHandler
from time_sync_service import TimeSyncConfig, TimeSyncService

logger = logging.getLogger("pc_controller")

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8080
RECV_BUFFER_SIZE = 64 * 1024
SOCKET_TIMEOUT = 1.0


@dataclass
class DeviceRecord:
    """Runtime view of a connected device."""

    device_id: str
    connection_id: str
    address: str
    status: str = "connected"
    capabilities: Dict[str, Any] = field(default_factory=dict)
    sensors: List[str] = field(default_factory=list)
    last_seen: float = field(default_factory=time.time)
    session_active: bool = False
    gsr_samples: Deque[Tuple[float, float]] = field(default_factory=lambda: deque(maxlen=2048))
    gsr_stats: Dict[str, Any] = field(default_factory=dict)
    rgb_frame_b64: Optional[str] = None
    thermal_frame_b64: Optional[str] = None
    info: Dict[str, Any] = field(default_factory=dict)

    def snapshot(self) -> Dict[str, Any]:
        """Return serialisable summary used by the UI/CLI."""
        return {
            "device_id": self.device_id,
            "connection_id": self.connection_id,
            "address": self.address,
            "status": self.status,
            "last_seen": self.last_seen,
            "capabilities": self.capabilities,
            "sensors": list(self.sensors),
            "session_active": self.session_active,
            "gsr_stats": self.gsr_stats,
            "info": self.info,
        }


@dataclass
class SessionInfo:
    session_id: str
    started_at: float
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ControllerEvent:
    type: str
    device_id: Optional[str] = None
    payload: Dict[str, Any] = field(default_factory=dict)


@dataclass
class _ActiveFile:
    path: Path
    hasher: hashlib._Hash
    size: int = 0
    chunks: int = 0


class SessionStorage:
    """Persistence helper for session telemetry and uploaded artefacts."""

    def __init__(self, root: Path):
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self._lock = threading.Lock()
        self._active_files: Dict[Tuple[str, str, str], _ActiveFile] = {}
        self._manifests: Dict[str, Dict[str, Any]] = {}
        self._event_logs: Dict[str, Path] = {}

    def _session_dir(self, session_id: str) -> Path:
        path = self.root / session_id
        path.mkdir(parents=True, exist_ok=True)
        return path

    def start_session(self, session_id: str, metadata: Dict[str, Any]) -> Path:
        session_dir = self._session_dir(session_id)
        info_file = session_dir / "session.json"
        with self._lock:
            started_at = time.time()
            payload = {"session_id": session_id, "started_at": started_at, **metadata}
            info_file.write_text(json.dumps(payload, indent=2))
            self._manifests[session_id] = {
                "session_id": session_id,
                "started_at": started_at,
                "files": {},
                "events": [],
                "gsr_samples": {},
            }
        return session_dir

    def get_session_dir(self, session_id: str) -> Path:
        """Return the directory for the session (creates it if missing)."""
        return self._session_dir(session_id)

    def finish_session(self, session_id: str, summary: Dict[str, Any]) -> None:
        session_dir = self._session_dir(session_id)
        summary_file = session_dir / "summary.json"
        with self._lock:
            manifest = self._manifests.get(session_id)
            if manifest:
                manifest_path = session_dir / "manifest.json"
                manifest_path.write_text(json.dumps(manifest, indent=2))
                summary["manifest_path"] = str(manifest_path)
                summary = {**summary, "manifest": manifest}
            summary["finished_at"] = time.time()
            summary_file.write_text(json.dumps(summary, indent=2))

    def append_gsr_sample(self, session_id: str, device_id: str, timestamp: float, value: float) -> None:
        session_dir = self._session_dir(session_id)
        file_path = session_dir / f"{device_id}_gsr.csv"
        line = f"{timestamp:.6f},{value:.6f}\n"

        with self._lock:
            header_needed = not file_path.exists()
            with file_path.open("a", encoding="utf-8") as handle:
                if header_needed:
                    handle.write("timestamp,value\n")
                handle.write(line)
            manifest = self._manifests.setdefault(
                session_id, {"session_id": session_id, "files": {}, "events": [], "gsr_samples": {}}
            )
            gsr_stats = manifest.setdefault("gsr_samples", {}).setdefault(
                device_id, {"count": 0, "last_timestamp": timestamp}
            )
            gsr_stats["count"] = int(gsr_stats.get("count", 0)) + 1
            gsr_stats["last_timestamp"] = timestamp

    def append_session_event(self, session_id: str, event: Dict[str, Any]) -> None:
        session_dir = self._session_dir(session_id)
        events_path = session_dir / "events.jsonl"
        with self._lock:
            with events_path.open("a", encoding="utf-8") as handle:
                handle.write(json.dumps(event) + "\n")
            manifest = self._manifests.setdefault(
                session_id, {"session_id": session_id, "files": {}, "events": [], "gsr_samples": {}}
            )
            manifest.setdefault("events", []).append(event)
            self._event_logs[session_id] = events_path

    def begin_file(self, session_id: str, device_id: str, filename: str) -> Path:
        session_dir = self._session_dir(session_id)
        device_dir = session_dir / device_id
        device_dir.mkdir(parents=True, exist_ok=True)
        target_path = device_dir / filename
        with self._lock:
            target_path.write_bytes(b"")
            active = _ActiveFile(path=target_path, hasher=hashlib.sha256())
            self._active_files[(session_id, device_id, filename)] = active
            manifest = self._manifests.setdefault(
                session_id, {"session_id": session_id, "files": {}, "events": [], "gsr_samples": {}}
            )
            manifest.setdefault("files", {})[f"{device_id}/{filename}"] = {
                "device": device_id,
                "filename": filename,
                "size": 0,
                "chunks": 0,
                "sha256": None,
                "received_at": time.time(),
            }
        return target_path

    def append_file_chunk(self, session_id: str, device_id: str, filename: str, chunk: bytes) -> None:
        key = (session_id, device_id, filename)
        with self._lock:
            active = self._active_files.get(key)
            if active is None:
                self.begin_file(session_id, device_id, filename)
                active = self._active_files[key]
            with active.path.open("ab") as handle:
                handle.write(chunk)
            active.hasher.update(chunk)
            active.size += len(chunk)
            active.chunks += 1
            manifest = self._manifests.setdefault(
                session_id, {"session_id": session_id, "files": {}, "events": [], "gsr_samples": {}}
            )
            entry = manifest.setdefault("files", {}).setdefault(
                f"{device_id}/{filename}",
                {"device": device_id, "filename": filename, "size": 0, "chunks": 0, "sha256": None},
            )
            entry["size"] = active.size
            entry["chunks"] = active.chunks

    def finalize_file(self, session_id: str, device_id: str, filename: str) -> Optional[str]:
        key = (session_id, device_id, filename)
        with self._lock:
            active = self._active_files.pop(key, None)
            if not active:
                return None
            digest = active.hasher.hexdigest()
            manifest = self._manifests.setdefault(
                session_id, {"session_id": session_id, "files": {}, "events": [], "gsr_samples": {}}
            )
            entry = manifest.setdefault("files", {}).setdefault(
                f"{device_id}/{filename}",
                {"device": device_id, "filename": filename},
            )
            entry.update(
                {
                    "sha256": digest,
                    "size": active.size,
                    "chunks": active.chunks,
                    "completed_at": time.time(),
                }
            )
            return digest


class ConnectionWorker(threading.Thread):
    """Manages socket IO for a single connected device."""

    def __init__(
            self,
            server: "NetworkServer",
            sock: socket.socket,
            address: Tuple[str, int],
            connection_id: str,
            adapter: ProtocolAdapter,
    ):
        super().__init__(daemon=True)
        self._server = server
        self._socket = sock
        self._socket.settimeout(SOCKET_TIMEOUT)
        self._address = address
        self.connection_id = connection_id
        self.device_id: Optional[str] = None
        self._adapter = adapter
        self._buffer = bytearray()
        self._send_lock = threading.Lock()
        self._running = threading.Event()
        self._running.set()

    @property
    def address(self) -> Tuple[str, int]:
        return self._address

    def set_device_id(self, device_id: str) -> None:
        self.device_id = device_id

    def stop(self) -> None:
        self._running.clear()
        try:
            self._socket.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        self._socket.close()

    def send_json(self, message: Dict[str, Any]) -> None:
        payload = json.dumps(message, separators=(",", ":")).encode("utf-8") + b"\n"
        with self._send_lock:
            try:
                self._socket.sendall(payload)
            except OSError as exc:  # pragma: no cover
                logger.warning("Failed to send to %s: %s", self.connection_id, exc)

    def send_bytes(self, data: bytes) -> None:
        with self._send_lock:
            try:
                self._socket.sendall(data)
            except OSError as exc:  # pragma: no cover
                logger.warning("Failed to send to %s: %s", self.connection_id, exc)

    def send_text(self, message: str) -> None:
        payload = message if message.endswith("\n") else f"{message}\n"
        self.send_bytes(payload.encode("utf-8"))

    def run(self) -> None:
        self._server._on_worker_started(self)
        try:
            while self._running.is_set():
                try:
                    data = self._socket.recv(RECV_BUFFER_SIZE)
                except socket.timeout:
                    continue
                except OSError:
                    break

                if not data:
                    break

                self._buffer.extend(data)
                while True:
                    newline_pos = self._buffer.find(b"\n")
                    if newline_pos == -1:
                        break

                    raw = self._buffer[:newline_pos]
                    del self._buffer[: newline_pos + 1]
                    raw = raw.strip()
                    if not raw:
                        continue

                    self._server._on_raw_message(self, raw)
        finally:
            self._server._on_worker_stopped(self)


@dataclass
class ConnectionContext:
    server: "NetworkServer"
    worker: ConnectionWorker

    @property
    def connection_id(self) -> str:
        return self.worker.connection_id

    @property
    def device_id(self) -> Optional[str]:
        return self.worker.device_id

    @property
    def address(self) -> Tuple[str, int]:
        return self.worker.address

    def set_device_id(self, device_id: str) -> None:
        self.server._associate_device(self.worker, device_id)

    def send(self, message: Dict[str, Any]) -> None:
        self.worker.send_json(message)

    def send_text(self, message: str) -> None:
        self.worker.send_text(message)

    def send_bytes(self, data: bytes) -> None:
        self.worker.send_bytes(data)


class _SyncSocketProxy:
    """Adapter that exposes a socket-like interface backed by a ConnectionContext."""

    def __init__(self, context: ConnectionContext):
        self._context = context

    def send(self, data: bytes) -> int:
        self._context.send_bytes(data)
        return len(data)


@dataclass
class NetworkEvent:
    type: str
    context: ConnectionContext
    payload: Optional[Dict[str, Any]] = None
    error: Optional[Exception] = None


class NetworkServer:
    """Accepts TCP connections and dispatches messages to the controller core."""

    def __init__(
            self,
            host: str,
            port: int,
            use_ssl: bool,
            ssl_context: Optional[ssl.SSLContext],
            event_callback: Callable[[NetworkEvent], None],
    ):
        self._host = host
        self._port = port
        self._use_ssl = use_ssl
        self._ssl_context = ssl_context
        self._event_callback = event_callback
        self._adapter = ProtocolAdapter()
        self._socket: Optional[socket.socket] = None
        self._accept_thread: Optional[threading.Thread] = None
        self._running = threading.Event()
        self._clients: Dict[str, ConnectionWorker] = {}
        self._device_index: Dict[str, ConnectionWorker] = {}
        self._lock = threading.Lock()

    def start(self) -> None:
        if self._running.is_set():
            raise RuntimeError("server already running")

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((self._host, self._port))
        sock.listen()

        self._socket = sock
        self._running.set()
        self._accept_thread = threading.Thread(target=self._accept_loop, daemon=True)
        self._accept_thread.start()
        logger.info(
            "Server listening on %s:%d (%s)",
            self._host,
            self._port,
            "TLS" if self._use_ssl else "TCP",
        )

    def stop(self) -> None:
        self._running.clear()

        if self._socket:
            try:
                self._socket.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            self._socket.close()
            self._socket = None

        with self._lock:
            workers = list(self._clients.values())

        for worker in workers:
            worker.stop()

        if self._accept_thread:
            self._accept_thread.join(timeout=1.0)
            self._accept_thread = None

    def send(self, device_id: str, message: Dict[str, Any]) -> bool:
        with self._lock:
            worker = self._device_index.get(device_id)
        if not worker:
            return False
        worker.send_json(message)
        return True

    def broadcast(self, message: Dict[str, Any]) -> int:
        with self._lock:
            workers = list(self._clients.values())
        for worker in workers:
            worker.send_json(message)
        return len(workers)

    def _accept_loop(self) -> None:
        assert self._socket is not None
        while self._running.is_set():
            try:
                client_sock, address = self._socket.accept()
            except OSError:
                if self._running.is_set():
                    logger.exception("Socket accept failed")
                break

            if self._use_ssl and self._ssl_context:
                try:
                    client_sock = self._ssl_context.wrap_socket(client_sock, server_side=True)
                except ssl.SSLError as exc:
                    logger.warning("TLS handshake failed from %s:%d: %s", address[0], address[1], exc)
                    client_sock.close()
                    continue

            connection_id = uuid.uuid4().hex
            worker = ConnectionWorker(self, client_sock, address, connection_id, self._adapter)
            with self._lock:
                self._clients[connection_id] = worker
            worker.start()

    def _associate_device(self, worker: ConnectionWorker, device_id: str) -> None:
        with self._lock:
            worker.set_device_id(device_id)
            self._device_index[device_id] = worker

    def _on_worker_started(self, worker: ConnectionWorker) -> None:
        context = ConnectionContext(self, worker)
        self._event_callback(NetworkEvent("transport_connected", context))

    def _on_raw_message(self, worker: ConnectionWorker, raw: bytes) -> None:
        context = ConnectionContext(self, worker)
        try:
            text = raw.decode("utf-8")
            if text.startswith("{"):
                payload = json.loads(text)
            else:
                payload = self._adapter.android_to_json(text)
                if payload is None:
                    raise ValueError("Unable to parse message")
        except Exception as exc:
            logger.debug("Malformed message from %s: %s (%s)", worker.connection_id, raw, exc)
            self._event_callback(
                NetworkEvent(
                    "protocol_error",
                    context,
                    {"raw": raw.decode("utf-8", errors="replace")},
                    exc,
                )
            )
            return

        self._event_callback(NetworkEvent("message", context, payload))

    def _on_worker_stopped(self, worker: ConnectionWorker) -> None:
        context = ConnectionContext(self, worker)
        with self._lock:
            self._clients.pop(worker.connection_id, None)
            if worker.device_id:
                self._device_index.pop(worker.device_id, None)
        self._event_callback(NetworkEvent("transport_disconnected", context))

    def _on_worker_error(self, worker: ConnectionWorker, error: Exception) -> None:
        context = ConnectionContext(self, worker)
        self._event_callback(NetworkEvent("transport_error", context, error=error))


class PCControllerCore:
    """Coordinates networking, device state, sessions, and persistence."""

    def __init__(self, storage_dir: Path, config: Optional[Dict[str, Any]] = None):
        self.storage_dir = storage_dir
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.config = config or {}
        self.devices: Dict[str, DeviceRecord] = {}
        self._lock = threading.Lock()
        self._listeners: List[Callable[[ControllerEvent], None]] = []
        self._session: Optional[SessionInfo] = None
        self._session_events: List[Dict[str, Any]] = []
        self._storage = SessionStorage(self.storage_dir)
        self._network: Optional[NetworkServer] = None
        self._sync_handler = SyncHandler()
        self._stimulus = StimulusController(self._notify_stimulus_event)
        self._time_sync_service = TimeSyncService(self._build_time_sync_config())
        self._device_offsets: Dict[str, float] = {}
        self._sensor_manager = self._build_sensor_manager()
        self._local_sensors_running = False
        self._command_queue: Dict[str, List[Dict[str, Any]]] = defaultdict(list)

    # ---------------------------------------------------------------- builders
    def _build_time_sync_config(self) -> TimeSyncConfig:
        cfg = self.config.get("time_sync_service", {}) or self.config.get("time_sync", {})
        try:
            host = str(cfg.get("host", "0.0.0.0"))
        except AttributeError:
            host = "0.0.0.0"
        port = int(cfg.get("port", 47017))
        max_clients = int(cfg.get("max_clients", 32))
        ttl = float(cfg.get("ttl_seconds", cfg.get("sync_interval", 30)))
        return TimeSyncConfig(host=host, port=port, max_clients=max_clients, ttl_seconds=ttl)

    def _build_sensor_manager(self) -> SensorManager:
        raw_sensors = self.config.get("sensors", {})
        sensor_map: Dict[str, SensorConfig] = {}
        if isinstance(raw_sensors, dict):
            try:
                sensor_map = load_sensor_config(raw_sensors)
            except Exception as exc:
                logger.warning("Failed to parse sensor configuration: %s", exc)
                sensor_map = {}

        if not sensor_map:
            # Provide a default simulated channel to satisfy FR1 simulation mode.
            default_rate = float(self.config.get("gsr", {}).get("sampling_rate", 128.0))
            sensor_map["sim_gsr"] = SensorConfig(name="Simulated GSR", sample_rate_hz=default_rate)

        manager = SensorManager(self._on_local_sensor_sample, sensor_map)
        simulation_cfg = self.config.get("simulation", {})
        if isinstance(simulation_cfg, dict) and simulation_cfg.get("force", False):
            manager.enable_simulation_mode()
        return manager

    @staticmethod
    def _extract_field(message: Dict[str, Any], key: str, default: Any = None) -> Any:
        if key in message:
            return message.get(key, default)
        payload = message.get("payload")
        if isinstance(payload, dict):
            return payload.get(key, default)
        return default

    def _start_local_sensors(self) -> List[str]:
        started = list(self._sensor_manager.start_streaming())
        if not started:
            return started
        with self._lock:
            for sensor_id in started:
                device_id = f"sensor_{sensor_id}"
                record = self.devices.get(device_id)
                if record is None:
                    record = DeviceRecord(
                        device_id=device_id,
                        connection_id="local",
                        address="localhost",
                        status="streaming",
                        capabilities={"sensors": ["GSR"], "origin": "pc"},
                        sensors=["GSR"],
                    )
                    self.devices[device_id] = record
                record.status = "streaming"
                record.session_active = True
                record.last_seen = time.time()
        self._local_sensors_running = True
        return started

    def _stop_local_sensors(self) -> None:
        self._sensor_manager.stop_streaming()
        self._local_sensors_running = False
        with self._lock:
            for record in self.devices.values():
                if record.connection_id == "local":
                    record.status = "standby"
                    record.session_active = False

    # ---------------------------------------------------------------- stimuli
    def _notify_stimulus_event(self, code: str, payload: Dict[str, Any]) -> None:
        timestamp = float(payload.get("timestamp", time.time()))
        details = {k: v for k, v in payload.items() if k != "timestamp"}
        marker = {"code": code, "timestamp": timestamp, "details": details}
        if self._session:
            marker["session_id"] = self._session.session_id
            self._storage.append_session_event(self._session.session_id, marker)
        self._session_events.append(marker)
        self._emit(ControllerEvent("stimulus_event", payload=marker))

        command_payload = {"code": code, "timestamp": timestamp, "payload": details}
        if self._session:
            command_payload["session_id"] = self._session.session_id
        if self._network:
            self.broadcast_command("event_marker", **command_payload)

    def trigger_flash_sync(self, intensity: float = 1.0, duration_s: float = 0.25) -> StimulusEvent:
        event = self._stimulus.flash_sync(intensity=intensity, duration_s=duration_s)
        self.broadcast_command(
            "flash_sync",
            intensity=float(intensity),
            duration_s=float(duration_s),
            timestamp=event.timestamp,
        )
        return event

    def trigger_audio_beep(self, frequency_hz: float = 440.0, duration_s: float = 0.2) -> StimulusEvent:
        event = self._stimulus.audio_beep(frequency_hz=frequency_hz, duration_s=duration_s)
        self.broadcast_command(
            "audio_beep",
            frequency_hz=float(frequency_hz),
            duration_s=float(duration_s),
            timestamp=event.timestamp,
        )
        return event

    def trigger_stimulus_video(self, path: Path, autoplay: bool = True) -> StimulusEvent:
        event = self._stimulus.stimulus_video(path=path, autoplay=autoplay)
        self.broadcast_command(
            "stimulus_video",
            path=str(path),
            autoplay=bool(autoplay),
            timestamp=event.timestamp,
        )
        return event

    def mark_event(self, label: str, extra: Optional[Dict[str, Any]] = None) -> StimulusEvent:
        event = self._stimulus.custom_marker(label, extra)
        return event

    # ---------------------------------------------------------------- sensors
    def _on_local_sensor_sample(self, sensor_id: str, timestamp: float, value: float) -> None:
        device_id = f"sensor_{sensor_id}"
        with self._lock:
            record = self.devices.get(device_id)
            if record is None:
                record = DeviceRecord(
                    device_id=device_id,
                    connection_id="local",
                    address="localhost",
                    status="streaming",
                    capabilities={"sensors": ["GSR"], "origin": "pc"},
                    sensors=["GSR"],
                )
                self.devices[device_id] = record
            record.last_seen = time.time()
            record.status = "streaming" if self._local_sensors_running else "standby"
            record.session_active = bool(self._session)
            record.gsr_samples.append((timestamp, value))
            record.gsr_stats = self._compute_gsr_stats(list(record.gsr_samples))
            if len(record.gsr_samples) >= 2:
                prev_ts = record.gsr_samples[-2][0]
                delta = max(timestamp - prev_ts, 1e-6)
                record.info["last_sample_interval_ms"] = delta * 1000.0
                record.info["instant_sample_rate_hz"] = 1.0 / delta
            record.info["samples_received"] = int(record.info.get("samples_received", 0)) + 1

        if self._session:
            self._storage.append_gsr_sample(self._session.session_id, device_id, timestamp, value)

        self._emit(
            ControllerEvent(
                "telemetry_gsr",
                device_id=device_id,
                payload={"timestamp": timestamp, "value": value, "source": "pc_sensor"},
            )
        )

    def is_simulation_enabled(self) -> bool:
        return self._sensor_manager.is_simulation_enabled()

    def set_simulation_mode(self, enabled: bool) -> bool:
        restart = self._local_sensors_running and self._session is not None
        if restart:
            self._stop_local_sensors()
        if enabled:
            self._sensor_manager.enable_simulation_mode()
        else:
            self._sensor_manager.disable_simulation_mode()
        if restart and self._session:
            self._start_local_sensors()
        self.broadcast_command("set_simulation_mode", enabled=bool(enabled))
        self._emit(ControllerEvent("simulation_mode_changed", payload={"enabled": enabled}))
        return enabled

    def toggle_simulation_mode(self) -> bool:
        return self.set_simulation_mode(not self.is_simulation_enabled())


    def start_server(self, host: str = DEFAULT_HOST, port: int = DEFAULT_PORT, use_ssl: bool = False) -> None:
        if self._network:
            raise RuntimeError("server already running")

        ssl_context = None
        if use_ssl:
            ssl_context = self._build_ssl_context()

        self._network = NetworkServer(host, port, use_ssl, ssl_context, self._handle_network_event)
        self._network.start()
        self._time_sync_service.start()

    def stop_server(self) -> None:
        if self._network:
            self._network.stop()
            self._network = None
        self._time_sync_service.stop()

    def register_listener(self, listener: Callable[[ControllerEvent], None]) -> None:
        self._listeners.append(listener)

    def _emit(self, event: ControllerEvent) -> None:
        for listener in list(self._listeners):
            try:
                listener(event)
            except Exception:  # pragma: no cover
                logger.exception("Listener failed")

    def list_devices(self) -> List[Dict[str, Any]]:
        with self._lock:
            return [record.snapshot() for record in self.devices.values()]

    def get_session(self) -> Optional[SessionInfo]:
        return self._session

    def get_storage_dir(self) -> Path:
        return self.storage_dir

    def get_session_dir(self, session_id: str) -> Path:
        return self._storage.get_session_dir(session_id)

    def _queue_command_for_offline_devices(self, message: Dict[str, Any]) -> None:
        snapshot = dict(message)
        with self._lock:
            for device_id, record in self.devices.items():
                if record.connection_id in {"", None} or record.connection_id == "local" or record.status == "connected":
                    continue
                queue = self._command_queue.setdefault(device_id, [])
                queue.append(snapshot.copy())
                if len(queue) > 32:
                    queue.pop(0)

    def _drain_offline_queue(self, device_id: str, context: ConnectionContext) -> bool:
        with self._lock:
            queued = list(self._command_queue.pop(device_id, []))
        for message in queued:
            context.send(message)
            self._emit(ControllerEvent("command_replayed", device_id=device_id, payload=message))
        return bool(queued)

    def _sync_device_state(self, device_id: str, context: ConnectionContext) -> None:
        if not self._session:
            return
        sync_command = self._build_command_message(
            "start_recording",
            requires_ack=False,
            session_id=self._session.session_id,
            metadata=self._session.metadata,
        )
        sync_command["replay"] = True
        context.send(sync_command)
        self._emit(ControllerEvent("command_replayed", device_id=device_id, payload=sync_command))
        for event in self._session_events:
            if event.get("code") in {"session_started"}:
                continue
            payload = {
                "code": event.get("code"),
                "timestamp": event.get("timestamp"),
                "payload": event.get("details", {}),
            }
            session_id = event.get("session_id") or (self._session.session_id if self._session else None)
            if session_id:
                payload["session_id"] = session_id
            marker_command = self._build_command_message(
                "event_marker",
                requires_ack=False,
                **payload,
            )
            marker_command["replay"] = True
            context.send(marker_command)
            self._emit(ControllerEvent("command_replayed", device_id=device_id, payload=marker_command))

    def _build_command_message(
        self,
        command: str,
        *,
        requires_ack: bool = True,
        **parameters: Any,
    ) -> Dict[str, Any]:
        return {
            "type": "command",
            "command": command,
            "commandId": str(uuid.uuid4()),
            "requiresAck": requires_ack,
            "timestamp": time.time(),
            "payload": parameters,
            "parameters": parameters,
        }

    def send_command(self, device_id: str, command: str, **parameters: Any) -> bool:
        message = self._build_command_message(command, **parameters)
        with self._lock:
            record = self.devices.get(device_id)
            if record and record.status != "connected":
                queue = self._command_queue.setdefault(device_id, [])
                queue.append(message.copy())
                if len(queue) > 32:
                    queue.pop(0)
                return False
        return self._network.send(device_id, message) if self._network else False

    def broadcast_command(self, command: str, **parameters: Any) -> int:
        message = self._build_command_message(command, **parameters)
        self._queue_command_for_offline_devices(message)
        return self._network.broadcast(message) if self._network else 0

    def start_recording(self, session_id: Optional[str] = None, metadata: Optional[Dict[str, Any]] = None) -> str:
        if self._session:
            raise RuntimeError("session already active")

        if session_id is None:
            session_id = time.strftime("session_%Y%m%d_%H%M%S")
        metadata = metadata or {}

        self._session = SessionInfo(session_id=session_id, started_at=time.time(), metadata=metadata)
        self._storage.start_session(session_id, metadata)
        self._session_events.clear()
        start_event = {"code": "session_started", "timestamp": self._session.started_at, "session_id": session_id}
        self._session_events.append(start_event)
        self._storage.append_session_event(session_id, start_event)
        local_sensors = self._start_local_sensors()
        self._emit(ControllerEvent("session_started", payload={"session_id": session_id}))
        if local_sensors:
            self._emit(
                ControllerEvent(
                    "local_sensors_started",
                    payload={"session_id": session_id, "sensors": local_sensors},
                )
            )
        self.broadcast_command("start_recording", session_id=session_id, metadata=metadata)
        return session_id

    def stop_recording(self) -> Optional[str]:
        if not self._session:
            return None

        session_id = self._session.session_id

        duration = time.time() - self._session.started_at
        summary = {
            "session_id": session_id,
            "duration_seconds": duration,
            "devices": [record.snapshot() for record in self.devices.values()],
            "events": list(self._session_events),
        }
        stop_event = {
            "code": "session_stopped",
            "timestamp": time.time(),
            "session_id": session_id,
            "duration_seconds": duration,
        }
        self._session_events.append(stop_event)
        self._storage.append_session_event(session_id, stop_event)
        summary["events"] = list(self._session_events)
        self._storage.finish_session(session_id, summary)
        self._stop_local_sensors()
        self.broadcast_command("stop_recording", session_id=session_id)
        self._emit(ControllerEvent("session_stopped", payload={"session_id": session_id, "duration": duration}))
        self._session = None
        return session_id

    # Network events ---------------------------------------------------------
    def _handle_network_event(self, event: NetworkEvent) -> None:
        context = event.context

        if event.type == "transport_connected":
            self._emit(
                ControllerEvent(
                    "transport_connected",
                    device_id=context.device_id,
                    payload={"connection_id": context.connection_id, "address": context.address},
                )
            )
            return

        if event.type == "transport_disconnected":
            self._handle_disconnect(context)
            return

        if event.type == "protocol_error":
            self._emit(
                ControllerEvent(
                    "protocol_error",
                    device_id=context.device_id,
                    payload={"raw": event.payload or {}, "error": str(event.error)},
                )
            )
            return

        if event.type == "message" and event.payload is not None:
            self._handle_message(context, event.payload)

    def _handle_disconnect(self, context: ConnectionContext) -> None:
        device_id = context.device_id
        if not device_id:
            self._emit(
                ControllerEvent(
                    "transport_disconnected",
                    payload={"connection_id": context.connection_id, "address": context.address},
                )
            )
            return

        with self._lock:
            record = self.devices.get(device_id)
            if record:
                record.status = "disconnected"
                record.session_active = bool(self._session)
                record.last_seen = time.time()

        self._emit(
            ControllerEvent(
                "device_disconnected",
                device_id=device_id,
                payload={"connection_id": context.connection_id},
            )
        )

    def _handle_message(self, context: ConnectionContext, message: Dict[str, Any]) -> None:
        msg_type = str(message.get("type", "")).lower()
        device_id = message.get("device_id") or context.device_id

        if msg_type in {"hello", "register_device"}:
            device_id = self._register_device(context, message)
            context.send(
                {
                    "type": "hello_ack",
                    "server_time": time.time(),
                    "session_id": self._session.session_id if self._session else None,
                }
            )
            return

        if msg_type == "sync_init":
            self._handle_sync_init(context, message, device_id)
            return

        if msg_type == "sync_response":
            self._handle_sync_response(context, message, device_id)
            return

        if msg_type == "sync_result":
            self._emit(ControllerEvent("sync_result", device_id=device_id, payload=message))
            return

        if not device_id:
            logger.debug("Dropping message without device_id: %s", message)
            return

        if msg_type == "status_update":
            self._apply_status_update(device_id, context, message)
        elif msg_type in {"telemetry_gsr", "data_gsr"}:
            self._handle_gsr(device_id, message)
        elif msg_type == "command_ack":
            self._emit(
                ControllerEvent(
                    "command_ack",
                    device_id=device_id,
                    payload=message,
                )
            )
        elif msg_type in {"frame_rgb", "frame"}:
            self._handle_frame(device_id, message, frame_type="rgb")
        elif msg_type == "frame_thermal":
            self._handle_frame(device_id, message, frame_type="thermal")
        elif msg_type == "session_event":
            self._handle_session_event(device_id, message)
        elif msg_type == "heartbeat":
            self._touch_device(device_id)
        elif msg_type in {"event_marker", "stimulus_marker"}:
            self._handle_event_marker(device_id, message)
        elif msg_type == "telemetry_update":
            self._handle_telemetry_update(message)
        elif msg_type == "file_begin":
            self._handle_file_begin(device_id, message)
        elif msg_type == "file_chunk":
            self._handle_file_chunk(device_id, message)
        elif msg_type == "file_end":
            self._handle_file_end(device_id, message)
        elif msg_type == "error":
            self._emit(ControllerEvent("device_error", device_id=device_id, payload=message))
        else:
            self._emit(ControllerEvent("unhandled_message", device_id=device_id, payload=message))

    def _handle_sync_init(
            self,
            context: ConnectionContext,
            message: Dict[str, Any],
            device_id: Optional[str],
    ) -> None:
        resolved_id = (
                device_id
                or message.get("device_id")
                or context.device_id
                or f"sync_{context.connection_id[:8]}"
        )
        if context.device_id is None:
            try:
                context.set_device_id(resolved_id)
            except Exception:  # pragma: no cover - defensive, should not happen
                logger.debug("Failed to associate device id %s with context", resolved_id)

        proxy = _SyncSocketProxy(context)
        try:
            success = self._sync_handler.handle_sync_init(resolved_id, proxy)
        except Exception as exc:
            logger.error("Failed to initiate time sync for %s: %s", resolved_id, exc)
            self._emit(
                ControllerEvent(
                    "time_sync_failed",
                    device_id=resolved_id,
                    payload={"stage": "init", "error": str(exc)},
                )
            )
            return

        if success:
            self._touch_device(resolved_id)
            self._emit(ControllerEvent("time_sync_started", device_id=resolved_id))
        else:
            self._emit(
                ControllerEvent(
                    "time_sync_failed",
                    device_id=resolved_id,
                    payload={"stage": "init", "error": "sync_handler_rejected"},
                )
            )

    def _handle_sync_response(
            self,
            context: ConnectionContext,
            message: Dict[str, Any],
            device_id: Optional[str],
    ) -> None:
        resolved_id = device_id or message.get("device_id") or context.device_id
        if resolved_id is None:
            logger.warning("SYNC_RESPONSE without identifiable device: %s", message)
            return

        t_pc = message.get("t_pc")
        t_ph = message.get("t_ph")
        if t_pc is None or t_ph is None:
            logger.warning("SYNC_RESPONSE missing timestamps from %s: %s", resolved_id, message)
            return

        try:
            t_pc_ms = int(t_pc)
            t_ph_ms = int(t_ph)
        except (TypeError, ValueError):
            logger.warning("Invalid SYNC_RESPONSE timestamps from %s: %s", resolved_id, message)
            return

        proxy = _SyncSocketProxy(context)
        try:
            result = self._sync_handler.handle_sync_response(resolved_id, t_pc_ms, t_ph_ms, proxy)
        except Exception as exc:
            logger.error("Failed to process SYNC_RESPONSE from %s: %s", resolved_id, exc)
            self._emit(
                ControllerEvent(
                    "time_sync_failed",
                    device_id=resolved_id,
                    payload={"stage": "response", "error": str(exc)},
                )
            )
            return

        if result:
            self._touch_device(resolved_id)
            with self._lock:
                record = self.devices.get(resolved_id)
                if record:
                    record.info["last_sync_offset_ms"] = result.get("offset_ms")
                    record.info["last_sync_rtt_ms"] = result.get("rtt_ms")
                    record.info["last_sync_timestamp"] = result.get("timestamp")
                self._device_offsets[resolved_id] = float(result.get("offset_ms", 0.0))
            self._emit(ControllerEvent("time_sync_completed", device_id=resolved_id, payload=result))
        else:
            self._emit(
                ControllerEvent(
                    "time_sync_failed",
                    device_id=resolved_id,
                    payload={"stage": "response", "error": "sync_handler_no_result"},
                )
            )

    def _register_device(self, context: ConnectionContext, message: Dict[str, Any]) -> str:
        device_id = message.get("device_id") or f"device_{context.connection_id[:8]}"
        context.set_device_id(device_id)

        with self._lock:
            record = self.devices.get(device_id)
            if record is None:
                record = DeviceRecord(
                    device_id=device_id,
                    connection_id=context.connection_id,
                    address=f"{context.address[0]}:{context.address[1]}",
                )
                self.devices[device_id] = record
            else:
                record.connection_id = context.connection_id
                record.address = f"{context.address[0]}:{context.address[1]}"

            record.status = "connected"
            record.session_active = bool(self._session)
            record.last_seen = time.time()

            capabilities = message.get("capabilities") or {}
            if isinstance(capabilities, dict):
                record.capabilities.update(capabilities)

            sensors = message.get("sensors") or capabilities.get("sensors")
            if isinstance(sensors, list):
                record.sensors = sensors

            extra = {k: v for k, v in message.items() if k not in {"type", "device_id", "capabilities", "sensors"}}
            record.info.update(extra)

        self._emit(
            ControllerEvent(
                "device_registered",
                device_id=device_id,
                payload={"record": record.snapshot()},
            )
        )
        replayed = self._drain_offline_queue(device_id, context)
        if self._session and not replayed:
            self._sync_device_state(device_id, context)
        return device_id

    def _apply_status_update(self, device_id: str, context: ConnectionContext, message: Dict[str, Any]) -> None:
        with self._lock:
            record = self.devices.setdefault(
                device_id,
                DeviceRecord(
                    device_id=device_id,
                    connection_id=context.connection_id,
                    address=f"{context.address[0]}:{context.address[1]}",
                ),
            )
            record.status = message.get("status", record.status)
            record.last_seen = time.time()
            if "sensors" in message and isinstance(message["sensors"], list):
                record.sensors = message["sensors"]
            extra = {k: v for k, v in message.items() if k not in {"type", "device_id", "status", "sensors"}}
            record.info.update(extra)

        self._emit(ControllerEvent("status_update", device_id=device_id, payload=message))

    def _touch_device(self, device_id: str) -> None:
        with self._lock:
            record = self.devices.get(device_id)
            if record:
                record.last_seen = time.time()

    def _handle_gsr(self, device_id: str, message: Dict[str, Any]) -> None:
        timestamp = float(message.get("timestamp", time.time()))
        value = message.get("value")
        sequence = message.get("sequence")

        if "packet_b64" in message:
            packet_bytes = base64.b64decode(message["packet_b64"])
            try:
                value, timestamp, sequence = self._parse_gsr_packet(packet_bytes, timestamp)
            except ValueError as exc:
                self._emit(ControllerEvent("telemetry_error", device_id=device_id, payload={"error": str(exc)}))
                return

        if value is None:
            self._emit(
                ControllerEvent(
                    "telemetry_error",
                    device_id=device_id,
                    payload={"error": "Missing GSR value"},
                )
            )
            return

        with self._lock:
            record = self.devices.get(device_id)
            if not record:
                record = DeviceRecord(device_id=device_id, connection_id="", address="")
                self.devices[device_id] = record
            record.last_seen = time.time()
            record.gsr_samples.append((timestamp, float(value)))
            record.gsr_stats = self._compute_gsr_stats(list(record.gsr_samples))
            if len(record.gsr_samples) >= 2:
                prev_ts = record.gsr_samples[-2][0]
                delta = max(timestamp - prev_ts, 1e-6)
                record.info["last_sample_interval_ms"] = delta * 1000.0
                record.info["instant_sample_rate_hz"] = 1.0 / delta
            record.info["samples_received"] = int(record.info.get("samples_received", 0)) + 1

        if self._session:
            self._storage.append_gsr_sample(self._session.session_id, device_id, timestamp, float(value))

        payload = {"timestamp": timestamp, "value": float(value)}
        if sequence is not None:
            payload["sequence"] = sequence

        self._emit(ControllerEvent("telemetry_gsr", device_id=device_id, payload=payload))

    def _parse_gsr_packet(self, packet: bytes, fallback_timestamp: float) -> Tuple[float, float, Optional[int]]:
        if not packet:
            raise ValueError("Empty GSR packet")

        if NATIVE_BACKEND_AVAILABLE:
            parsed = native_backend.parse_gsr_packet(packet)
            timestamp = parsed.timestamp_ms / 1000.0 if parsed.timestamp_ms else fallback_timestamp
            return float(parsed.conductance_us), timestamp, int(parsed.sequence)

        if len(packet) < 10 or packet[0] != 0xAA or packet[1] != 0x55:
            raise ValueError("Invalid GSR packet header (expected 0xAA55)")

        raw_value = (packet[8] << 8) | packet[9]
        conductance = raw_value * 0.01
        timestamp_ms = ((packet[4] << 24) | (packet[5] << 16) | (packet[6] << 8) | packet[7])
        timestamp = timestamp_ms / 1000.0 if timestamp_ms else fallback_timestamp
        sequence = (packet[2] << 8) | packet[3]
        return conductance, timestamp, sequence

    def _compute_gsr_stats(self, samples: List[Tuple[float, float]]) -> Dict[str, Any]:
        if not samples:
            return {"mean": 0.0, "min": 0.0, "max": 0.0, "stddev": 0.0, "count": 0}

        values = [value for _, value in samples]

        if NATIVE_BACKEND_AVAILABLE:
            stats = native_backend.compute_gsr_statistics(values)
            return {
                "mean": float(stats.mean_us),
                "min": float(stats.min_us),
                "max": float(stats.max_us),
                "stddev": float(stats.stddev_us),
                "count": int(stats.sample_count),
            }

        count = len(values)
        mean = sum(values) / count
        min_val = min(values)
        max_val = max(values)
        variance = sum((v - mean) ** 2 for v in values) / count
        return {"mean": mean, "min": min_val, "max": max_val, "stddev": variance ** 0.5, "count": count}

    def _handle_frame(self, device_id: str, message: Dict[str, Any], frame_type: str) -> None:
        frame_data = message.get("frame") or message.get("frame_b64") or message.get("image")
        if not isinstance(frame_data, str):
            self._emit(
                ControllerEvent(
                    "frame_error",
                    device_id=device_id,
                    payload={"error": f"Missing {frame_type} frame data"},
                )
            )
            return

        with self._lock:
            record = self.devices.get(device_id)
            if record:
                if frame_type == "rgb":
                    record.rgb_frame_b64 = frame_data
                else:
                    record.thermal_frame_b64 = frame_data
                record.last_seen = time.time()

        self._emit(
            ControllerEvent(
                f"frame_{frame_type}",
                device_id=device_id,
                payload={"frame_b64": frame_data, "timestamp": message.get("timestamp", time.time())},
            )
        )

    def _handle_session_event(self, device_id: str, message: Dict[str, Any]) -> None:
        event = message.get("event")
        session_id = message.get("session_id")

        if event == "started" and session_id and not self._session:
            self._session = SessionInfo(session_id=session_id, started_at=time.time(),
                                        metadata=message.get("metadata", {}))
            self._storage.start_session(session_id, self._session.metadata)
            self._emit(ControllerEvent("session_started", payload={"session_id": session_id, "source": device_id}))

        if event == "stopped" and self._session and session_id == self._session.session_id:
            duration = time.time() - self._session.started_at
            summary = {
                "session_id": session_id,
                "duration_seconds": duration,
                "devices": [record.snapshot() for record in self.devices.values()],
            }
            self._storage.finish_session(session_id, summary)
            self._emit(ControllerEvent("session_stopped", payload={"session_id": session_id, "source": device_id}))
            self._session = None

        with self._lock:
            record = self.devices.get(device_id)
            if record:
                record.session_active = event == "started"

    def _handle_event_marker(self, device_id: str, message: Dict[str, Any]) -> None:
        code = (
            self._extract_field(message, "code")
            or self._extract_field(message, "marker_id")
            or self._extract_field(message, "label")
            or "event"
        )
        ts_value = self._extract_field(message, "timestamp")
        try:
            timestamp = float(ts_value) if ts_value is not None else time.time()
        except (TypeError, ValueError):
            timestamp = time.time()

        info = {
            k: v
            for k, v in message.items()
            if k not in {"type", "code", "label", "timestamp", "device_id", "session_id", "payload"}
        }
        payload = message.get("payload")
        if isinstance(payload, dict):
            for key, value in payload.items():
                info.setdefault(key, value)

        session_id = self._extract_field(message, "session_id") or (self._session.session_id if self._session else None)
        marker = {"code": code, "timestamp": timestamp, "device_id": device_id, "details": info}
        if session_id:
            marker["session_id"] = session_id
            self._storage.append_session_event(session_id, marker)
        self._session_events.append(marker)
        self._emit(ControllerEvent("event_marker", device_id=device_id, payload=marker))

    def _handle_telemetry_update(self, message: Dict[str, Any]) -> None:
        payload = message.get("payload")
        if not isinstance(payload, dict):
            return

        for device_key, metrics in payload.items():
            if not isinstance(metrics, dict):
                continue

            with self._lock:
                record = self.devices.get(device_key)
                if record is None:
                    record = DeviceRecord(
                        device_id=device_key,
                        connection_id="telemetry",
                        address="n/a",
                        status="telemetry",
                        capabilities={"telemetry_only": True},
                    )
                    self.devices[device_key] = record
                record.info.update(metrics)
                record.last_seen = time.time()
                frame_rate = metrics.get("frame_rate")
                if isinstance(frame_rate, (int, float)) and frame_rate:
                    record.status = "streaming"

            self._emit(ControllerEvent("telemetry_update", device_id=device_key, payload=metrics))

    def _handle_file_begin(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = self._extract_field(message, "session_id") or (self._session.session_id if self._session else None)
        filename = self._extract_field(message, "filename")
        if not session_id or not filename:
            self._emit(
                ControllerEvent(
                    "file_transfer_error",
                    device_id=device_id,
                    payload={"error": "Missing session_id/filename"},
                )
            )
            return

        path = self._storage.begin_file(session_id, device_id, filename)
        payload = {"session_id": session_id, "filename": filename, "path": str(path)}
        modality = self._extract_field(message, "modality")
        size_bytes = self._extract_field(message, "size_bytes")
        chunk_size = self._extract_field(message, "chunk_size")
        estimated_chunks = self._extract_field(message, "estimated_chunks")
        mime_type = self._extract_field(message, "mime_type")
        if modality is not None:
            payload["modality"] = modality
        if size_bytes is not None:
            payload["size_bytes"] = size_bytes
        if chunk_size is not None:
            payload["chunk_size"] = chunk_size
        if estimated_chunks is not None:
            payload["estimated_chunks"] = estimated_chunks
        if mime_type is not None:
            payload["mime_type"] = mime_type
        self._emit(
            ControllerEvent(
                "file_transfer_begin",
                device_id=device_id,
                payload=payload,
            )
        )

    def _handle_file_chunk(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = self._extract_field(message, "session_id") or (self._session.session_id if self._session else None)
        filename = self._extract_field(message, "filename")
        data_b64 = self._extract_field(message, "data")

        if not session_id or not filename or not isinstance(data_b64, str):
            self._emit(
                ControllerEvent(
                    "file_transfer_error",
                    device_id=device_id,
                    payload={"error": "Invalid file chunk metadata"},
                )
            )
            return

        chunk = base64.b64decode(data_b64)
        self._storage.append_file_chunk(session_id, device_id, filename, chunk)
        chunk_payload = {
            "session_id": session_id,
            "filename": filename,
            "size": len(chunk),
        }
        chunk_index = self._extract_field(message, "chunk_index")
        if chunk_index is not None:
            chunk_payload["chunk_index"] = chunk_index
        modality = self._extract_field(message, "modality")
        if modality is not None:
            chunk_payload["modality"] = modality
        self._emit(
            ControllerEvent(
                "file_transfer_chunk",
                device_id=device_id,
                payload=chunk_payload,
            )
        )

    def _handle_file_end(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = self._extract_field(message, "session_id") or (self._session.session_id if self._session else None)
        filename = self._extract_field(message, "filename")
        if not session_id or not filename:
            return
        digest = self._storage.finalize_file(session_id, device_id, filename)
        payload = {"session_id": session_id, "filename": filename}
        size_bytes = self._extract_field(message, "size_bytes")
        chunks = self._extract_field(message, "chunks")
        modality = self._extract_field(message, "modality")
        sha_from_message = self._extract_field(message, "sha256") or self._extract_field(message, "checksum")
        if size_bytes is not None:
            payload["size_bytes"] = size_bytes
        if chunks is not None:
            payload["chunks"] = chunks
        if modality is not None:
            payload["modality"] = modality
        if digest:
            payload["sha256"] = digest
        elif sha_from_message:
            payload["sha256"] = sha_from_message
        self._emit(
            ControllerEvent(
                "file_transfer_end",
                device_id=device_id,
                payload=payload,
            )
        )

    def _build_ssl_context(self) -> Optional[ssl.SSLContext]:
        cert_dir = Path(__file__).parent / "certificates"
        cert_path = cert_dir / "server.crt"
        key_path = cert_dir / "server.key"

        if not cert_path.exists() or not key_path.exists():
            logger.warning("TLS requested but certificate/key not found in %s", cert_dir)
            return None

        context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        context.load_cert_chain(certfile=str(cert_path), keyfile=str(key_path))
        return context


def handle_cli_command(controller: "PCControllerCore", raw: str) -> Tuple[bool, List[str]]:
    raw = raw.strip()
    if not raw:
        return False, []

    parts = raw.split()
    cmd = parts[0].lower()
    lines: List[str] = []

    if cmd in {"quit", "exit"}:
        return True, lines

    if cmd == "help":
        lines.append(
            "Commands: help, devices, status, start, stop, flash, beep, marker <label>, simulate [on|off|toggle], quit"
        )
        return False, lines

    if cmd == "devices":
        for device in controller.list_devices():
            lines.append(json.dumps(device, indent=2))
        return False, lines

    if cmd == "status":
        session = controller.get_session()
        if session:
            lines.append(f"Active session: {session.session_id} (started {time.ctime(session.started_at)})")
        else:
            lines.append("No active session")
        lines.append(f"Simulation: {'on' if controller.is_simulation_enabled() else 'off'}")
        return False, lines

    if cmd == "start":
        session_id = controller.start_recording()
        lines.append(f"Session {session_id} started")
        return False, lines

    if cmd == "stop":
        session_id = controller.stop_recording()
        if session_id:
            lines.append(f"Session {session_id} stopped")
        else:
            lines.append("No active session to stop")
        return False, lines

    if cmd == "flash":
        event = controller.trigger_flash_sync()
        lines.append(f"Flash sync triggered at {event.timestamp:.2f}s")
        return False, lines

    if cmd == "beep":
        event = controller.trigger_audio_beep()
        lines.append(f"Audio sync beep triggered at {event.timestamp:.2f}s")
        return False, lines

    if cmd == "marker":
        label = raw[len(parts[0]) :].strip()
        if not label:
            label = time.strftime("marker_%H%M%S")
        event = controller.mark_event(label)
        lines.append(f"Marker '{label}' recorded at {event.timestamp:.2f}s")
        return False, lines

    if cmd == "simulate":
        arg = parts[1].lower() if len(parts) > 1 else "toggle"
        if arg in {"on", "enable"}:
            controller.set_simulation_mode(True)
            lines.append("Simulation mode enabled")
        elif arg in {"off", "disable"}:
            controller.set_simulation_mode(False)
            lines.append("Simulation mode disabled")
        else:
            state = controller.toggle_simulation_mode()
            lines.append(f"Simulation mode {'enabled' if state else 'disabled'}")
        return False, lines

    lines.append("Unknown command. Type 'help' for options.")
    return False, lines


def run_cli(controller: PCControllerCore) -> None:
    """Blocking CLI loop for manual interaction."""

    def printer(event: ControllerEvent) -> None:
        logger.info("EVENT %s (%s): %s", event.type, event.device_id, event.payload)

    controller.register_listener(printer)

    print("IRCamera PC Controller (CLI mode)")
    print("Type 'help' for available commands.\n")

    try:
        while True:
            raw = input("> ")
            should_exit, output_lines = handle_cli_command(controller, raw)
            for line in output_lines:
                print(line)
            if should_exit:
                break
    except KeyboardInterrupt:
        print("\nStopping controller...")
    finally:
        controller.stop_server()


try:  # pragma: no cover - GUI path exercised in integration tests
    from gui_app import GUI_AVAILABLE, run_gui  # type: ignore[assignment]
except Exception:  # pragma: no cover - GUI optional
    GUI_AVAILABLE = False

    def run_gui(_: "PCControllerCore") -> None:
        raise RuntimeError("PyQt6 + pyqtgraph are required for the GUI")



def parse_args(argv: Optional[Iterable[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="IRCamera PC Controller")
    parser.add_argument("--host", default=DEFAULT_HOST, help="Server host (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="Server port (default: 8080)")
    parser.add_argument(
        "--ssl",
        action="store_true",
        help="Enable TLS (requires certificates/server.crt and server.key)",
    )
    parser.add_argument("--storage-dir", default=str(Path("pc_data")), help="Directory for session storage")
    parser.add_argument("--cli", action="store_true", help="Force CLI even if GUI is available")
    parser.add_argument("--log-level", default="INFO", help="Logging level (DEBUG, INFO, ...)")
    parser.add_argument("--config", help="Path to YAML/JSON configuration file")
    parser.add_argument(
        "--simulate-sensors",
        action="store_true",
        help="Force simulation mode for local sensors (overrides config)",
    )
    return parser.parse_args(argv)


def main(argv: Optional[Iterable[str]] = None) -> None:
    args = parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level.upper(), logging.INFO),
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    config_data: Dict[str, Any] = {}
    if args.config:
        config_path = Path(args.config).expanduser()
        if config_path.exists():
            try:
                text = config_path.read_text(encoding="utf-8")
                suffix = config_path.suffix.lower()
                if suffix in {".yaml", ".yml"} and yaml is not None:
                    config_data = yaml.safe_load(text) or {}
                elif suffix == ".json":
                    config_data = json.loads(text)
                elif yaml is not None:
                    config_data = yaml.safe_load(text) or {}
                else:
                    config_data = json.loads(text)
            except Exception as exc:
                logger.error("Failed to load config %s: %s", config_path, exc)
                config_data = {}
        else:
            logger.warning("Config file %s not found; proceeding with defaults", config_path)
    if args.simulate_sensors:
        simulation_cfg = config_data.setdefault("simulation", {})
        simulation_cfg["force"] = True

    controller = PCControllerCore(Path(args.storage_dir).expanduser(), config=config_data)
    controller.start_server(host=args.host, port=args.port, use_ssl=args.ssl)

    if args.cli:
        run_cli(controller)
        return

    if not GUI_AVAILABLE:
        logger.info("PyQt6 not available; falling back to CLI mode")
        run_cli(controller)
        return

    run_gui(controller)


if __name__ == "__main__":  # pragma: no cover
    main()
