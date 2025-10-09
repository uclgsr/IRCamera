#!/usr/bin/env python3
import json
import os
import socket
import sys
import tempfile
import threading
import time
import unittest
import warnings
from pathlib import Path
from typing import TextIO

warnings.simplefilter("ignore", ResourceWarning)

try:
    from pc_controller import PCControllerCore, ControllerEvent
except ImportError:  # pragma: no cover - fallback for direct execution
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    from pc_controller import PCControllerCore, ControllerEvent


def _get_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def _read_line(reader: TextIO) -> str:
    line = reader.readline()
    if not line:
        raise RuntimeError("Connection closed while waiting for data")
    return line.strip()


def _send_line(sock: socket.socket, message: str) -> None:
    data = (message.rstrip("\n") + "\n").encode("utf-8")
    sock.sendall(data)


class TimeSyncControllerIntegrationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.controller = PCControllerCore(Path(self.tmp.name))
        self.events = []
        self._event_lock = threading.Lock()

        def listener(event: ControllerEvent) -> None:
            with self._event_lock:
                self.events.append(event)

        self.controller.register_listener(listener)
        self.host = "127.0.0.1"
        self.port = _get_free_port()
        self.controller.start_server(host=self.host, port=self.port, use_ssl=False)

    def tearDown(self) -> None:
        if self.controller is not None:
            self.controller.stop_server()
            time.sleep(0.05)
            self.controller = None
        self.tmp.cleanup()

    def test_full_time_sync_round_trip(self) -> None:
        device_id = "device_sync_test"
        with socket.create_connection((self.host, self.port), timeout=3) as client:
            client.settimeout(3)
            reader = client.makefile("r", encoding="utf-8", newline="\n")
            _send_line(client, f"HELLO device_id={device_id} sensors=[RGB]")
            hello_ack = _read_line(reader)
            self.assertTrue(hello_ack.startswith("{"))
            ack_payload = json.loads(hello_ack)
            self.assertEqual(ack_payload.get("type"), "hello_ack")

            _send_line(client, "SYNC_INIT")
            sync_request = _read_line(reader)
            self.assertTrue(sync_request.startswith("SYNC_REQUEST"))
            parts = dict(
                token.split("=", 1) for token in sync_request.split()[1:] if "=" in token
            )
            self.assertIn("t_pc", parts)
            t1 = int(parts["t_pc"])

            phone_timestamp = t1 + 5
            _send_line(client, f"SYNC_RESPONSE t_pc={t1} t_ph={phone_timestamp}")
            sync_result = _read_line(reader)
            self.assertTrue(sync_result.startswith("SYNC_RESULT"))
            result_tokens = dict(
                token.split("=", 1) for token in sync_result.split()[1:] if "=" in token
            )
            self.assertEqual(int(result_tokens["t1"]), t1)
            self.assertEqual(int(result_tokens["t2"]), phone_timestamp)
            reader.close()

        deadline = time.time() + 2.0
        while time.time() < deadline:
            with self._event_lock:
                if any(event.type == "time_sync_completed" for event in self.events):
                    break
            time.sleep(0.05)

        with self._event_lock:
            event_types = [event.type for event in self.events]
        self.assertIn("time_sync_started", event_types)
        self.assertIn("time_sync_completed", event_types)

        record = self.controller.devices.get(device_id)
        self.assertIsNotNone(record, "Device record should be registered")
        info = record.info
        self.assertIn("last_sync_offset_ms", info)
        self.assertIn("last_sync_rtt_ms", info)
        self.assertIn("last_sync_timestamp", info)

        self.controller.stop_server()
        time.sleep(0.05)
        self.controller = None


if __name__ == "__main__":  # pragma: no cover
    unittest.main()
