#!/usr/bin/env python3
"""
High precision time synchronisation helpers for the PC controller.

This module provides:

* A lightweight UDP service that mirrors an NTP-style four-timestamp exchange.
  Clients send their monotonic send time (T0 in nanoseconds). The service
  captures its receive time (T1) and transmit time (T2), both in nanoseconds,
  and responds with a 16-byte big-endian payload containing those values. The
  client then records its receive time (T3) and can calculate offset and round
  trip delay with sub-millisecond resolution.
* An optional HTTP endpoint (`/time/calibration`) that allows devices to publish
  their computed calibration results (offset / RTT / drift) and fetch the best
  recent sample as a fallback. This endpoint is intentionally stateless from the
  HTTP perspective; all state is stored in-memory with a configurable TTL.

The service is optional. If binding fails the rest of the controller continues
to operate; time synchronisation will fall back to the legacy protocol path.
"""

from __future__ import annotations

import json
import logging
import socket
import struct
import threading
import time
from collections import deque
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Deque, Dict, Optional

logger = logging.getLogger(__name__)


@dataclass
class TimeSyncConfig:
    host: str = "0.0.0.0"
    port: int = 47017
    http_port: Optional[int] = None
    max_clients: int = 32
    ttl_seconds: float = 30.0
    history_size: int = 256


@dataclass
class CalibrationSample:
    timestamp: float
    reference_epoch_ms: float
    offset_ms: float
    round_trip_ms: float
    drift_ppm: float
    accuracy_ms: float

    def to_payload(self) -> Dict[str, float]:
        return {
            "referenceEpochMillis": float(self.reference_epoch_ms),
            "offsetMillis": float(self.offset_ms),
            "roundTripMillis": float(self.round_trip_ms),
            "driftPpm": float(self.drift_ppm),
            "accuracyMillis": float(self.accuracy_ms),
        }


class _TimeSyncHttpServer(ThreadingHTTPServer):
    """Thread-aware HTTP server exposing calibration endpoints."""

    daemon_threads = True

    def __init__(
        self,
        server_address: tuple[str, int],
        service: "TimeSyncService",
    ) -> None:
        self.service = service

        class _Handler(BaseHTTPRequestHandler):
            server: "_TimeSyncHttpServer"  # type: ignore[assignment]

            def do_GET(self) -> None:  # noqa: N802 (BaseHTTPRequestHandler signature)
                if self.path.rstrip("/") == "/time/calibration":
                    sample = self.server.service.best_calibration()
                    if sample is None:
                        self.send_response(HTTPStatus.NO_CONTENT)
                        self.end_headers()
                        return
                    payload = sample.to_payload()
                    body = json.dumps(payload).encode("utf-8")
                    self.send_response(HTTPStatus.OK)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Content-Length", str(len(body)))
                    self.end_headers()
                    self.wfile.write(body)
                    return
                self.send_response(HTTPStatus.NOT_FOUND)
                self.end_headers()

            def do_POST(self) -> None:  # noqa: N802 (BaseHTTPRequestHandler signature)
                if self.path.rstrip("/") != "/time/calibration":
                    self.send_response(HTTPStatus.NOT_FOUND)
                    self.end_headers()
                    return
                try:
                    length = int(self.headers.get("Content-Length", "0") or "0")
                except ValueError:
                    self.send_response(HTTPStatus.LENGTH_REQUIRED)
                    self.end_headers()
                    return
                data = self.rfile.read(length)
                try:
                    payload = json.loads(data.decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError) as exc:  # pragma: no cover - defensive
                    logger.debug("Invalid calibration payload: %s", exc)
                    self.send_response(HTTPStatus.BAD_REQUEST)
                    self.end_headers()
                    return
                if self.server.service.record_calibration(payload):
                    self.send_response(HTTPStatus.ACCEPTED)
                else:
                    self.send_response(HTTPStatus.BAD_REQUEST)
                self.end_headers()

            def log_message(self, format: str, *args) -> None:  # noqa: A003 - signature from base
                logger.debug("TimeSync HTTP: " + format, *args)

        super().__init__(server_address, _Handler)


class TimeSyncService:
    """High precision UDP responder with optional calibration API."""

    def __init__(self, config: TimeSyncConfig):
        self.config = config
        self._udp_socket: Optional[socket.socket] = None
        self._udp_thread: Optional[threading.Thread] = None
        self._running = threading.Event()
        self._sequence = 0

        self._http_server: Optional[_TimeSyncHttpServer] = None
        self._http_thread: Optional[threading.Thread] = None

        self._calibration_lock = threading.Lock()
        self._calibrations: Deque[CalibrationSample] = deque(maxlen=max(8, config.history_size))

    # ------------------------------------------------------------------ lifecycle
    def start(self) -> bool:
        """Start UDP (and optional HTTP) services."""
        udp_started = self._start_udp()
        http_started = self._start_http()
        return udp_started and (http_started or self.config.http_port is None)

    def stop(self) -> None:
        """Stop all background services."""
        self._running.clear()
        self._stop_udp()
        self._stop_http()

    # ---------------------------------------------------------------- UDP service
    def _start_udp(self) -> bool:
        if self._running.is_set():
            return True

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.bind((self.config.host, self.config.port))
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 128 * 1024)
        except OSError as exc:
            logger.warning(
                "TimeSyncService UDP unavailable on %s:%d (%s)",
                self.config.host,
                self.config.port,
                exc,
            )
            return False

        self._udp_socket = sock
        self._running.set()
        self._udp_thread = threading.Thread(target=self._serve_udp, name="time-sync-udp", daemon=True)
        self._udp_thread.start()
        logger.info("TimeSyncService UDP listening on %s:%d", self.config.host, self.config.port)
        return True

    def _stop_udp(self) -> None:
        if self._udp_socket:
            try:
                try:
                    self._udp_socket.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
                self._udp_socket.close()
            except OSError:
                pass
            self._udp_socket = None
        if self._udp_thread and self._udp_thread.is_alive():
            self._udp_thread.join(timeout=1.5)
        self._udp_thread = None

    def _serve_udp(self) -> None:
        assert self._udp_socket is not None
        sock = self._udp_socket
        while self._running.is_set():
            try:
                sock.settimeout(1.0)
                payload, addr = sock.recvfrom(64)
            except socket.timeout:
                continue
            except OSError:
                break

            if not payload:
                continue

            try:
                (client_send_ns,) = struct.unpack("!Q", payload[:8])
            except struct.error:
                logger.debug("Ignoring malformed time sync payload from %s", addr)
                continue

            t1 = time.perf_counter_ns()
            self._sequence = (self._sequence + 1) & 0xFFFFFFFF

            # Simulate minimal processing delay before responding.
            t2 = time.perf_counter_ns()

            response = struct.pack("!QQ", t1, t2)

            try:
                sock.sendto(response, addr)
            except OSError:
                logger.debug("Failed to respond to time sync request from %s", addr)
                continue

            logger.debug(
                "Time sync sample seq=%d from %s: client_send_ns=%d, t1=%d, t2=%d",
                self._sequence,
                addr,
                client_send_ns,
                t1,
                t2,
            )

    # --------------------------------------------------------------- HTTP service
    def _start_http(self) -> bool:
        if self.config.http_port is None:
            return True
        try:
            server = _TimeSyncHttpServer((self.config.host, self.config.http_port), self)
        except OSError as exc:
            logger.warning(
                "TimeSyncService HTTP unavailable on %s:%s (%s)",
                self.config.host,
                self.config.http_port,
                exc,
            )
            return False

        self._http_server = server
        self._http_thread = threading.Thread(target=server.serve_forever, name="time-sync-http", daemon=True)
        self._http_thread.start()
        logger.info(
            "TimeSyncService HTTP listening on %s:%d",
            self.config.host,
            self.config.http_port,
        )
        return True

    def _stop_http(self) -> None:
        if self._http_server:
            try:
                self._http_server.shutdown()
                self._http_server.server_close()
            except OSError:
                pass
            self._http_server = None
        if self._http_thread and self._http_thread.is_alive():
            self._http_thread.join(timeout=1.5)
        self._http_thread = None

    # ------------------------------------------------------------ calibrations API
    def record_calibration(self, payload: Dict[str, float]) -> bool:
        """Store calibration sample sent by clients; returns True if accepted."""
        try:
            sample = CalibrationSample(
                timestamp=time.time(),
                reference_epoch_ms=float(payload["referenceEpochMillis"]),
                offset_ms=float(payload["offsetMillis"]),
                round_trip_ms=float(payload["roundTripMillis"]),
                drift_ppm=float(payload.get("driftPpm", 0.0)),
                accuracy_ms=float(payload.get("accuracyMillis", 0.0)),
            )
        except (KeyError, TypeError, ValueError):
            logger.debug("Rejected calibration payload: %s", payload)
            return False

        with self._calibration_lock:
            self._calibrations.append(sample)
            self._purge_calibrations_locked()
        logger.debug(
            "Recorded calibration offset=%.3fms rtt=%.3fms accuracy=%.3fms",
            sample.offset_ms,
            sample.round_trip_ms,
            sample.accuracy_ms,
        )
        return True

    def best_calibration(self) -> Optional[CalibrationSample]:
        """Return the best (lowest accuracy) calibration within TTL."""
        with self._calibration_lock:
            self._purge_calibrations_locked()
            if not self._calibrations:
                return None
            return min(self._calibrations, key=lambda sample: (sample.accuracy_ms, sample.round_trip_ms))

    def _purge_calibrations_locked(self) -> None:
        cutoff = time.time() - self.config.ttl_seconds
        while self._calibrations and self._calibrations[0].timestamp < cutoff:
            self._calibrations.popleft()


__all__ = ["TimeSyncService", "TimeSyncConfig", "CalibrationSample"]
