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
    import enhanced_native_backend as native_backend  # type: ignore

    NATIVE_BACKEND_AVAILABLE = True
except ImportError:  # pragma: no cover - optional dependency
    native_backend = None
    NATIVE_BACKEND_AVAILABLE = False

from protocol_adapter import ProtocolAdapter
from sync_handler import SyncHandler

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


class SessionStorage:
    """Persistence helper for session telemetry and uploaded artefacts."""

    def __init__(self, root: Path):
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self._lock = threading.Lock()
        self._active_files: Dict[Tuple[str, str, str], Path] = {}

    def _session_dir(self, session_id: str) -> Path:
        path = self.root / session_id
        path.mkdir(parents=True, exist_ok=True)
        return path

    def start_session(self, session_id: str, metadata: Dict[str, Any]) -> Path:
        session_dir = self._session_dir(session_id)
        info_file = session_dir / "session.json"
        with self._lock:
            info_file.write_text(json.dumps({"session_id": session_id, **metadata}, indent=2))
        return session_dir

    def get_session_dir(self, session_id: str) -> Path:
        """Return the directory for the session (creates it if missing)."""
        return self._session_dir(session_id)

    def finish_session(self, session_id: str, summary: Dict[str, Any]) -> None:
        session_dir = self._session_dir(session_id)
        summary_file = session_dir / "summary.json"
        with self._lock:
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

    def begin_file(self, session_id: str, device_id: str, filename: str) -> Path:
        session_dir = self._session_dir(session_id)
        device_dir = session_dir / device_id
        device_dir.mkdir(parents=True, exist_ok=True)
        target_path = device_dir / filename
        with self._lock:
            target_path.write_bytes(b"")
            self._active_files[(session_id, device_id, filename)] = target_path
        return target_path

    def append_file_chunk(self, session_id: str, device_id: str, filename: str, chunk: bytes) -> None:
        key = (session_id, device_id, filename)
        with self._lock:
            path = self._active_files.get(key)
            if path is None:
                path = self.begin_file(session_id, device_id, filename)
            with path.open("ab") as handle:
                handle.write(chunk)

    def finalize_file(self, session_id: str, device_id: str, filename: str) -> None:
        with self._lock:
            self._active_files.pop((session_id, device_id, filename), None)


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

    def __init__(self, storage_dir: Path):
        self.storage_dir = storage_dir
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.devices: Dict[str, DeviceRecord] = {}
        self._lock = threading.Lock()
        self._listeners: List[Callable[[ControllerEvent], None]] = []
        self._session: Optional[SessionInfo] = None
        self._storage = SessionStorage(self.storage_dir)
        self._network: Optional[NetworkServer] = None
        self._sync_handler = SyncHandler()

    def start_server(self, host: str = DEFAULT_HOST, port: int = DEFAULT_PORT, use_ssl: bool = False) -> None:
        if self._network:
            raise RuntimeError("server already running")

        ssl_context = None
        if use_ssl:
            ssl_context = self._build_ssl_context()

        self._network = NetworkServer(host, port, use_ssl, ssl_context, self._handle_network_event)
        self._network.start()

    def stop_server(self) -> None:
        if self._network:
            self._network.stop()
            self._network = None

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

    def send_command(self, device_id: str, command: str, **parameters: Any) -> bool:
        message = {
            "type": "command",
            "command": command,
            "parameters": parameters,
            "timestamp": time.time(),
        }
        return self._network.send(device_id, message) if self._network else False

    def broadcast_command(self, command: str, **parameters: Any) -> int:
        message = {
            "type": "command",
            "command": command,
            "parameters": parameters,
            "timestamp": time.time(),
        }
        return self._network.broadcast(message) if self._network else 0

    def start_recording(self, session_id: Optional[str] = None, metadata: Optional[Dict[str, Any]] = None) -> str:
        if self._session:
            raise RuntimeError("session already active")

        if session_id is None:
            session_id = time.strftime("session_%Y%m%d_%H%M%S")
        metadata = metadata or {}

        self._session = SessionInfo(session_id=session_id, started_at=time.time(), metadata=metadata)
        self._storage.start_session(session_id, metadata)
        self._emit(ControllerEvent("session_started", payload={"session_id": session_id}))
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
        }
        self._storage.finish_session(session_id, summary)
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
                record.session_active = False
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
        elif msg_type in {"frame_rgb", "frame"}:
            self._handle_frame(device_id, message, frame_type="rgb")
        elif msg_type == "frame_thermal":
            self._handle_frame(device_id, message, frame_type="thermal")
        elif msg_type == "session_event":
            self._handle_session_event(device_id, message)
        elif msg_type == "heartbeat":
            self._touch_device(device_id)
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

    def _handle_file_begin(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = message.get("session_id") or (self._session.session_id if self._session else None)
        filename = message.get("filename")
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
        self._emit(
            ControllerEvent(
                "file_transfer_begin",
                device_id=device_id,
                payload={"session_id": session_id, "filename": filename, "path": str(path)},
            )
        )

    def _handle_file_chunk(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = message.get("session_id") or (self._session.session_id if self._session else None)
        filename = message.get("filename")
        data_b64 = message.get("data")

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
        self._emit(
            ControllerEvent(
                "file_transfer_chunk",
                device_id=device_id,
                payload={"session_id": session_id, "filename": filename, "size": len(chunk)},
            )
        )

    def _handle_file_end(self, device_id: str, message: Dict[str, Any]) -> None:
        session_id = message.get("session_id") or (self._session.session_id if self._session else None)
        filename = message.get("filename")
        if not session_id or not filename:
            return
        self._storage.finalize_file(session_id, device_id, filename)
        self._emit(
            ControllerEvent(
                "file_transfer_end",
                device_id=device_id,
                payload={"session_id": session_id, "filename": filename},
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


def run_cli(controller: PCControllerCore) -> None:
    """Blocking CLI loop for manual interaction."""

    def printer(event: ControllerEvent) -> None:
        logger.info("EVENT %s (%s): %s", event.type, event.device_id, event.payload)

    controller.register_listener(printer)

    print("IRCamera PC Controller (CLI mode)")
    print("Type 'help' for available commands.\n")

    try:
        while True:
            command = input("> ").strip().lower()
            if command in {"quit", "exit"}:
                break

            if command == "help":
                print("Commands: help, devices, status, start, stop, quit")
                continue

            if command == "devices":
                for device in controller.list_devices():
                    print(json.dumps(device, indent=2))
                continue

            if command == "status":
                session = controller.get_session()
                if session:
                    print(f"Active session: {session.session_id} (started {time.ctime(session.started_at)})")
                else:
                    print("No active session")
                continue

            if command == "start":
                session_id = controller.start_recording()
                print(f"Session {session_id} started")
                continue

            if command == "stop":
                session_id = controller.stop_recording()
                if session_id:
                    print(f"Session {session_id} stopped")
                else:
                    print("No active session to stop")
                continue

            print("Unknown command. Type 'help' for options.")

    except KeyboardInterrupt:
        print("\nStopping controller...")
    finally:
        controller.stop_server()


try:  # pragma: no cover - GUI path exercised in integration tests
    from PyQt6.QtCore import QObject, Qt, QTimer, pyqtSignal
    from PyQt6.QtGui import QPixmap
    from PyQt6.QtWidgets import (
        QApplication,
        QFileDialog,
        QHBoxLayout,
        QLabel,
        QMainWindow,
        QMessageBox,
        QPushButton,
        QSplitter,
        QTabWidget,
        QTextEdit,
        QTreeWidget,
        QTreeWidgetItem,
        QVBoxLayout,
        QWidget,
    )

    import pyqtgraph as pg

    GUI_AVAILABLE = True


    class ControllerBridge(QObject):
        event_received = pyqtSignal(object)

        def __init__(self, controller: PCControllerCore):
            super().__init__()
            controller.register_listener(self._forward)

        def _forward(self, event: ControllerEvent) -> None:
            self.event_received.emit(event)


    class MainWindow(QMainWindow):
        def __init__(self, controller: PCControllerCore):
            super().__init__()
            self.controller = controller
            self.bridge = ControllerBridge(controller)
            self.bridge.event_received.connect(self._handle_event)

            self.device_items: Dict[str, QTreeWidgetItem] = {}
            self.gsr_history: Dict[str, Deque[Tuple[float, float]]] = defaultdict(lambda: deque(maxlen=600))
            self.gsr_curves: Dict[str, pg.PlotDataItem] = {}
            self._rgb_pixmap: Optional[QPixmap] = None
            self._thermal_pixmap: Optional[QPixmap] = None

            self._setup_ui()

            self._plot_timer = QTimer(self)
            self._plot_timer.timeout.connect(self._refresh_plot)
            self._plot_timer.start(250)

        def _setup_ui(self) -> None:
            self.setWindowTitle("IRCamera PC Controller")
            self.resize(1200, 800)

            central = QWidget()
            self.setCentralWidget(central)
            root_layout = QVBoxLayout(central)

            controls = QHBoxLayout()
            root_layout.addLayout(controls)

            self.start_btn = QPushButton("Start Session")
            self.start_btn.clicked.connect(self._start_session)
            controls.addWidget(self.start_btn)

            self.stop_btn = QPushButton("Stop Session")
            self.stop_btn.clicked.connect(self._stop_session)
            self.stop_btn.setEnabled(False)
            controls.addWidget(self.stop_btn)

            self.sync_btn = QPushButton("Sync Devices")
            self.sync_btn.clicked.connect(self._sync_devices)
            controls.addWidget(self.sync_btn)

            self.export_btn = QPushButton("Export Data")
            self.export_btn.clicked.connect(self._export_data)
            controls.addWidget(self.export_btn)

            controls.addStretch(1)

            splitter = QSplitter()
            root_layout.addWidget(splitter, stretch=1)

            self.device_tree = QTreeWidget()
            self.device_tree.setHeaderLabels(
                ["Device", "Status", "Sensors", "Last Update", "Session", "Last Sync"]
            )
            self.device_tree.setColumnWidth(0, 160)
            self.device_tree.setColumnWidth(1, 90)
            self.device_tree.setColumnWidth(2, 140)
            self.device_tree.setColumnWidth(3, 120)
            self.device_tree.setColumnWidth(4, 80)
            self.device_tree.setColumnWidth(5, 180)
            splitter.addWidget(self.device_tree)

            self.tabs = QTabWidget()
            splitter.addWidget(self.tabs)

            # Telemetry tab
            telemetry_tab = QWidget()
            telemetry_layout = QVBoxLayout(telemetry_tab)
            self.gsr_plot = pg.PlotWidget(title="GSR (µS)")
            self.gsr_plot.setLabel("left", "Conductance", units="µS")
            self.gsr_plot.setLabel("bottom", "Seconds")
            self.gsr_plot.addLegend()
            telemetry_layout.addWidget(self.gsr_plot)
            self.tabs.addTab(telemetry_tab, "Telemetry")

            # Frames tab
            frames_tab = QWidget()
            frames_layout = QHBoxLayout(frames_tab)
            self.rgb_label = QLabel("RGB frame")
            self.rgb_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
            self.rgb_label.setMinimumSize(320, 240)
            frames_layout.addWidget(self.rgb_label)
            self.thermal_label = QLabel("Thermal frame")
            self.thermal_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
            self.thermal_label.setMinimumSize(320, 240)
            frames_layout.addWidget(self.thermal_label)
            self.tabs.addTab(frames_tab, "Frames")

            # Log tab
            log_tab = QWidget()
            log_layout = QVBoxLayout(log_tab)
            self.log_text = QTextEdit()
            self.log_text.setReadOnly(True)
            log_layout.addWidget(self.log_text)
            self.tabs.addTab(log_tab, "Log")

        def _start_session(self) -> None:
            try:
                session_id = self.controller.start_recording()
                QMessageBox.information(self, "Session Started", f"Session {session_id} started.")
                self.start_btn.setEnabled(False)
                self.stop_btn.setEnabled(True)
            except Exception as exc:  # pragma: no cover
                QMessageBox.critical(self, "Error", str(exc))

        def _stop_session(self) -> None:
            session_id = self.controller.stop_recording()
            if session_id:
                QMessageBox.information(self, "Session Stopped", f"Session {session_id} stopped.")
            self.start_btn.setEnabled(True)
            self.stop_btn.setEnabled(False)

        def _sync_devices(self) -> None:
            count = self.controller.broadcast_command("sync_request", server_time=time.time())
            self._append_log(f"Sync request sent to {count} device(s)")

        def _export_data(self) -> None:
            # Pick session directory (active session preferred, otherwise latest)
            session = self.controller.get_session()
            storage_dir = self.controller.get_storage_dir()
            if session:
                session_id = session.session_id
            else:
                session_dirs = [p for p in storage_dir.iterdir() if p.is_dir()]
                if not session_dirs:
                    QMessageBox.information(self, "Export", "No sessions available yet.")
                    return
                session_dirs.sort(key=lambda p: p.stat().st_mtime, reverse=True)
                session_id = session_dirs[0].name

            source_dir = self.controller.get_session_dir(session_id)
            default_path = storage_dir / f"{session_id}.zip"
            save_path, _ = QFileDialog.getSaveFileName(
                self,
                "Export Session Archive",
                str(default_path),
                "Zip Archives (*.zip)",
            )
            if not save_path:
                return

            target = Path(save_path)
            base = target.with_suffix("")
            shutil.make_archive(str(base), "zip", root_dir=source_dir)
            QMessageBox.information(self, "Export Complete", f"Session archived to {base}.zip")

        def _handle_event(self, event: ControllerEvent) -> None:
            handlers = {
                "device_registered": self._on_device_registered,
                "status_update": self._on_status_update,
                "device_disconnected": self._on_device_disconnected,
                "telemetry_gsr": self._on_gsr_sample,
                "frame_rgb": lambda e: self._update_image(e, frame_type="rgb"),
                "frame_thermal": lambda e: self._update_image(e, frame_type="thermal"),
                "session_started": self._on_session_started,
                "session_stopped": self._on_session_stopped,
                "time_sync_started": self._on_time_sync_started,
                "time_sync_completed": self._on_time_sync_completed,
                "time_sync_failed": self._on_time_sync_failed,
            }
            handler = handlers.get(event.type, self._on_generic_event)
            handler(event)

        def _on_device_registered(self, event: ControllerEvent) -> None:
            payload = event.payload.get("record", {})
            self._update_device_row(payload)
            self._append_log(f"Device {event.device_id} registered.")

        def _on_status_update(self, event: ControllerEvent) -> None:
            payload = event.payload
            snapshot = self.controller.devices.get(
                event.device_id).snapshot() if event.device_id in self.controller.devices else payload
            self._update_device_row(snapshot)

        def _on_device_disconnected(self, event: ControllerEvent) -> None:
            record = self.controller.devices.get(event.device_id)
            if record:
                self._update_device_row(record.snapshot())
            self._append_log(f"Device {event.device_id} disconnected.")

        def _on_session_started(self, event: ControllerEvent) -> None:
            self.start_btn.setEnabled(False)
            self.stop_btn.setEnabled(True)
            self._append_log(f"Session {event.payload.get('session_id')} started.")

        def _on_session_stopped(self, event: ControllerEvent) -> None:
            self.start_btn.setEnabled(True)
            self.stop_btn.setEnabled(False)
            self._append_log(f"Session {event.payload.get('session_id')} stopped.")

        def _on_gsr_sample(self, event: ControllerEvent) -> None:
            history = self.gsr_history[event.device_id]
            history.append((event.payload["timestamp"], event.payload["value"]))

        def _on_time_sync_started(self, event: ControllerEvent) -> None:
            device = event.device_id or "unknown device"
            self._append_log(f"Time sync started for {device}.")

        def _on_time_sync_completed(self, event: ControllerEvent) -> None:
            device = event.device_id or "unknown device"
            payload = event.payload or {}
            offset = payload.get("offset_ms")
            rtt = payload.get("rtt_ms")
            details = ""
            if offset is not None and rtt is not None:
                details = f" offset={offset} ms, RTT={rtt} ms"
            self._append_log(f"Time sync completed for {device}.{details}")
            if event.device_id and event.device_id in self.controller.devices:
                snapshot = self.controller.devices[event.device_id].snapshot()
                self._update_device_row(snapshot)

        def _on_time_sync_failed(self, event: ControllerEvent) -> None:
            device = event.device_id or "unknown device"
            reason = ""
            if event.payload and event.payload.get("error"):
                reason = f": {event.payload.get('error')}"
            self._append_log(f"Time sync failed for {device}{reason}.")

        def _on_generic_event(self, event: ControllerEvent) -> None:
            self._append_log(f"{event.type}: {event.payload}")

        def _update_device_row(self, snapshot: Dict[str, Any]) -> None:
            device_id = snapshot.get("device_id")
            if not device_id:
                return
            item = self.device_items.get(device_id)
            if not item:
                item = QTreeWidgetItem([device_id, "", "", "", "", ""])
                self.device_tree.addTopLevelItem(item)
                self.device_items[device_id] = item
            item.setText(1, snapshot.get("status", "unknown"))
            sensors = ", ".join(snapshot.get("sensors", []))
            item.setText(2, sensors)
            last_seen = snapshot.get("last_seen")
            if last_seen:
                item.setText(3, time.strftime("%H:%M:%S", time.localtime(last_seen)))
            item.setText(4, "Yes" if snapshot.get("session_active") else "No")
            info = snapshot.get("info") or {}
            offset = info.get("last_sync_offset_ms")
            rtt = info.get("last_sync_rtt_ms")
            timestamp = info.get("last_sync_timestamp")
            if offset is not None and rtt is not None:
                if timestamp:
                    ts_str = time.strftime("%H:%M:%S", time.localtime(timestamp))
                    item.setText(5, f"{offset} ms / {rtt} ms ({ts_str})")
                else:
                    item.setText(5, f"{offset} ms / {rtt} ms")
            else:
                item.setText(5, "n/a")

        def _update_image(self, event: ControllerEvent, frame_type: str) -> None:
            frame_b64 = event.payload.get("frame_b64")
            if not isinstance(frame_b64, str):
                return
            image_bytes = base64.b64decode(frame_b64)
            pixmap = QPixmap()
            if not pixmap.loadFromData(image_bytes):
                return

            if frame_type == "rgb":
                self._rgb_pixmap = pixmap
                self._apply_pixmap(self.rgb_label, pixmap)
            else:
                self._thermal_pixmap = pixmap
                self._apply_pixmap(self.thermal_label, pixmap)

        def _refresh_plot(self) -> None:
            for device_id, history in self.gsr_history.items():
                if device_id not in self.gsr_curves:
                    color = pg.intColor(len(self.gsr_curves))
                    curve = self.gsr_plot.plot(pen=pg.mkPen(color=color, width=2), name=device_id)
                    self.gsr_curves[device_id] = curve

                if not history:
                    continue

                base_ts = history[0][0]
                xs = [ts - base_ts for ts, _ in history]
                ys = [value for _, value in history]
                self.gsr_curves[device_id].setData(xs, ys)

        def _apply_pixmap(self, label: QLabel, pixmap: Optional[QPixmap]) -> None:
            if not pixmap:
                label.setText("No frame")
                label.setPixmap(QPixmap())
                return
            scaled = pixmap.scaled(label.size(), Qt.AspectRatioMode.KeepAspectRatio,
                                   Qt.TransformationMode.SmoothTransformation)
            label.setPixmap(scaled)

        def resizeEvent(self, event) -> None:  # type: ignore[override]
            super().resizeEvent(event)
            self._apply_pixmap(self.rgb_label, self._rgb_pixmap)
            self._apply_pixmap(self.thermal_label, self._thermal_pixmap)

        def _append_log(self, message: str) -> None:
            timestamp = time.strftime("%H:%M:%S")
            self.log_text.append(f"[{timestamp}] {message}")


    def run_gui(controller: PCControllerCore) -> None:
        app = QApplication.instance() or QApplication([])
        window = MainWindow(controller)
        window.show()
        app.exec()

except Exception:  # pragma: no cover - GUI optional
    GUI_AVAILABLE = False


    def run_gui(_: PCControllerCore) -> None:
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
    return parser.parse_args(argv)


def main(argv: Optional[Iterable[str]] = None) -> None:
    args = parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level.upper(), logging.INFO),
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    controller = PCControllerCore(Path(args.storage_dir).expanduser())
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
