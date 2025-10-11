import base64
import json
import socket
import tempfile
import time
import unittest
from pathlib import Path

from pc_controller import PCControllerCore


def _find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


class PCControllerIntegrationTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.tmpdir = tempfile.TemporaryDirectory()
        storage_dir = Path(self.tmpdir.name) / "pc_data"
        config = {
            "simulation": {"force": True},
            "transfer": {"verify_checksums": True, "max_retries": 1},
        }
        self.controller = PCControllerCore(storage_dir, config=config)
        self.port = _find_free_port()
        self.controller.start_server(host="127.0.0.1", port=self.port, use_ssl=False)
        # Give the background listener a moment to boot
        time.sleep(0.05)

    def tearDown(self) -> None:
        try:
            self.controller.stop_server()
        finally:
            time.sleep(0.05)
            self.tmpdir.cleanup()

    def _connect_device(self):
        sock = socket.create_connection(("127.0.0.1", self.port), timeout=2.0)
        sock.settimeout(1.0)
        reader = sock.makefile("r", encoding="utf-8", newline="\n")
        return sock, reader

    def _read_json_line(self, reader):
        try:
            line = reader.readline()
        except socket.timeout:
            return None
        if not line:
            return None
        line = line.strip()
        return json.loads(line) if line else None

    def test_full_session_flow(self) -> None:
        sock, reader = self._connect_device()
        try:
            hello = {"type": "hello", "device_id": "android-test", "sensors": ["GSR", "RGB"]}
            sock.sendall((json.dumps(hello) + "\n").encode("utf-8"))
            ack = self._read_json_line(reader)
            self.assertIsNotNone(ack)
            self.assertEqual(ack.get("type"), "hello_ack")

            session_id = self.controller.start_recording("integration_suite")
            start_command = self._read_json_line(reader)
            self.assertIsNotNone(start_command)
            self.assertEqual(start_command.get("command"), "start_recording")
            self.assertIn("commandId", start_command)
            payload = start_command.get("payload", {})
            self.assertEqual(payload.get("session_id"), "integration_suite")
            sock.sendall(
                (
                    json.dumps(
                        {
                            "type": "command_ack",
                            "commandId": start_command.get("commandId"),
                            "status": "ok",
                        }
                    )
                    + "\n"
                ).encode("utf-8")
            )

            telemetry = {
                "type": "telemetry_gsr",
                "device_id": "android-test",
                "timestamp": time.time(),
                "value": 4.2,
            }
            sock.sendall((json.dumps(telemetry) + "\n").encode("utf-8"))

            payload = base64.b64encode(b"integration-data").decode("ascii")
            sock.sendall(
                (
                    json.dumps(
                        {
                            "type": "file_begin",
                            "session_id": session_id,
                            "filename": "data.bin",
                        }
                    )
                    + "\n"
                ).encode("utf-8")
            )
            sock.sendall(
                (
                    json.dumps(
                        {
                            "type": "file_chunk",
                            "session_id": session_id,
                            "filename": "data.bin",
                            "data": payload,
                        }
                    )
                    + "\n"
                ).encode("utf-8")
            )
            sock.sendall(
                (
                    json.dumps(
                        {
                            "type": "file_end",
                            "session_id": session_id,
                            "filename": "data.bin",
                        }
                    )
                    + "\n"
                ).encode("utf-8")
            )

            time.sleep(0.1)
            self.controller.stop_recording()
            stop_command = self._read_json_line(reader)
            self.assertIsNotNone(stop_command)
            self.assertEqual(stop_command.get("command"), "stop_recording")
            self.assertEqual(stop_command.get("payload", {}).get("session_id"), session_id)
            sock.sendall(
                (
                    json.dumps(
                        {
                            "type": "command_ack",
                            "commandId": stop_command.get("commandId"),
                            "status": "ok",
                        }
                    )
                    + "\n"
                ).encode("utf-8")
            )
        finally:
            try:
                reader.close()
            finally:
                sock.close()
        time.sleep(0.1)

        session_dir = self.controller.get_session_dir(session_id)
        summary_path = session_dir / "summary.json"
        self.assertTrue(summary_path.exists())
        summary = json.loads(summary_path.read_text())
        manifest = summary.get("manifest", {})
        files = manifest.get("files", {})
        self.assertIn("android-test/data.bin", files)
        entry = files["android-test/data.bin"]
        self.assertGreater(entry.get("size", 0), 0)
        self.assertTrue(entry.get("sha256"))

        # GSR CSV should exist with at least one sample
        gsr_csv = session_dir / "android-test_gsr.csv"
        self.assertTrue(gsr_csv.exists())
        rows = gsr_csv.read_text().strip().splitlines()
        self.assertGreaterEqual(len(rows), 2)

        # Events log should contain markers for session start/stop
        events_path = session_dir / "events.jsonl"
        self.assertTrue(events_path.exists())
        events = [json.loads(line) for line in events_path.read_text().splitlines()]
        codes = {event["code"] for event in events}
        self.assertIn("session_started", codes)
        self.assertIn("session_stopped", codes)


if __name__ == "__main__":
    unittest.main()
