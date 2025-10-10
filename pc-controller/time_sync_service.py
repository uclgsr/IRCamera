#!/usr/bin/env python3
"""
Standalone time synchronisation service (FR3, NFR2).

The Android devices already support the SYNC_* command exchange handled by
`SyncHandler`, but FR3 also requires an always-on reference clock that devices
can consult even outside the session protocol. This module implements a tiny
NTP-like UDP server that responds with the current PC time and a monotonic
sequence number so clients can correct for packet reordering.

The service is lightweight and optional. If the configured UDP port is already
in use the controller logs a warning and continues operating with the protocol
based sync path only.
"""

from __future__ import annotations

import json
import logging
import socket
import threading
import time
from dataclasses import dataclass
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class TimeSyncConfig:
    host: str = "0.0.0.0"
    port: int = 47017
    max_clients: int = 32
    ttl_seconds: float = 30.0


class TimeSyncService:
    """Simple UDP responder that mirrors a subset of the NTP exchange."""

    def __init__(self, config: TimeSyncConfig):
        self.config = config
        self._socket: Optional[socket.socket] = None
        self._thread: Optional[threading.Thread] = None
        self._running = threading.Event()
        self._sequence = 0

    # ---------------------------------------------------------------- lifecycle
    def start(self) -> bool:
        if self._running.is_set():
            return True

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.bind((self.config.host, self.config.port))
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 128 * 1024)
        except OSError as exc:
            logger.warning("TimeSyncService unavailable on %s:%d (%s)", self.config.host, self.config.port, exc)
            return False

        self._socket = sock
        self._running.set()
        self._thread = threading.Thread(target=self._serve, name="time-sync-service", daemon=True)
        self._thread.start()
        logger.info("TimeSyncService listening on %s:%d", self.config.host, self.config.port)
        return True

    def stop(self) -> None:
        self._running.clear()
        if self._socket:
            try:
                try:
                    self._socket.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
                self._socket.close()
            except OSError:
                pass
            self._socket = None
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=1.5)
        self._thread = None

    # ---------------------------------------------------------------- internals
    def _serve(self) -> None:
        assert self._socket is not None
        sock = self._socket
        while self._running.is_set():
            try:
                sock.settimeout(1.0)
                payload, addr = sock.recvfrom(4096)
            except socket.timeout:
                continue
            except OSError:
                break

            if not payload:
                continue

            receive_time = time.time()
            monotonic_time = time.perf_counter()
            self._sequence = (self._sequence + 1) & 0xFFFFFFFF

            response = {
                "type": "time_sync",
                "server_receive_unix": receive_time,
                "server_monotonic": monotonic_time,
                "responded_unix": time.time(),
                "sequence": self._sequence,
            }

            try:
                sock.sendto(json.dumps(response).encode("utf-8"), addr)
            except OSError:
                logger.debug("Failed to respond to time sync request from %s", addr)


__all__ = ["TimeSyncService", "TimeSyncConfig"]
