import base64
import json
import tempfile
import time
import unittest
from pathlib import Path

from pc_controller import ConnectionContext, NetworkEvent, PCControllerCore


class DummyContext:
    def __init__(self, connection_id: str = "conn-1"):
        self.connection_id = connection_id
        self.address = ("127.0.0.1", 5100)
        self._device_id = None
        self.sent_messages = []

    @property
    def device_id(self):
        return self._device_id

    def set_device_id(self, device_id: str):
        self._device_id = device_id

    def send(self, message):
        self.sent_messages.append(message)


def make_gsr_packet(sequence: int = 1, timestamp_ms: int = 1000, raw_value: int = 400) -> bytes:
    return bytes(
        [
            0xAA,
            0x55,
            (sequence >> 8) & 0xFF,
            sequence & 0xFF,
            (timestamp_ms >> 24) & 0xFF,
            (timestamp_ms >> 16) & 0xFF,
            (timestamp_ms >> 8) & 0xFF,
            timestamp_ms & 0xFF,
            (raw_value >> 8) & 0xFF,
            raw_value & 0xFF,
        ]
    )


class ControllerCoreTestCase(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.TemporaryDirectory()
        self.storage_path = Path(self.tmpdir.name)
        self.controller = PCControllerCore(self.storage_path)
        self.context = DummyContext()

    def tearDown(self):
        self.controller._stop_local_sensors()
        self.tmpdir.cleanup()

    def test_device_registration_sends_ack(self):
        event = NetworkEvent(
            "message",
            self.context,
            {"type": "hello", "device_id": "android-01", "sensors": ["GSR", "RGB"]},
        )
        self.controller._handle_network_event(event)

        self.assertIn("android-01", self.controller.devices)
        self.assertTrue(self.context.sent_messages)
        self.assertEqual(self.context.sent_messages[0]["type"], "hello_ack")

    def test_gsr_packet_updates_statistics(self):
        self.controller._handle_network_event(
            NetworkEvent("message", self.context, {"type": "hello", "device_id": "android-01"})
        )
        packet = make_gsr_packet(raw_value=500)
        message = {
            "type": "telemetry_gsr",
            "device_id": "android-01",
            "packet_b64": base64.b64encode(packet).decode("ascii"),
        }
        self.controller._handle_network_event(NetworkEvent("message", self.context, message))

        record = self.controller.devices["android-01"]
        self.assertGreater(record.gsr_stats["count"], 0)
        self.assertAlmostEqual(record.gsr_stats["mean"], 5.0, places=2)

    def test_session_storage_writes_csv(self):
        self.controller._handle_network_event(
            NetworkEvent("message", self.context, {"type": "hello", "device_id": "android-01"})
        )
        session_id = self.controller.start_recording("session_test")
        packet = make_gsr_packet(raw_value=450)
        message = {
            "type": "telemetry_gsr",
            "device_id": "android-01",
            "packet_b64": base64.b64encode(packet).decode("ascii"),
        }
        self.controller._handle_network_event(NetworkEvent("message", self.context, message))
        self.controller.stop_recording()

        csv_path = self.controller.get_session_dir(session_id) / "android-01_gsr.csv"
        self.assertTrue(csv_path.exists())
        contents = csv_path.read_text().strip().splitlines()
        self.assertGreaterEqual(len(contents), 2)  # header + at least one sample
        summary_path = self.controller.get_session_dir(session_id) / "summary.json"
        summary = json.loads(summary_path.read_text())
        self.assertIn("manifest", summary)
        self.assertIn("files", summary["manifest"])

    def test_mark_event_records_in_summary(self):
        session_id = self.controller.start_recording("session_events")
        self.controller.mark_event("calibration_start", {"pattern": "checkerboard"})
        # Ensure event propagation async callbacks settle
        time.sleep(0.05)
        self.controller.stop_recording()

        summary_path = self.controller.get_session_dir(session_id) / "summary.json"
        data = json.loads(summary_path.read_text())
        events = data.get("events", [])
        self.assertTrue(any(event["code"] == "calibration_start" for event in events))

    def test_offline_command_replay(self):
        self.controller._handle_network_event(
            NetworkEvent("message", self.context, {"type": "hello", "device_id": "android-01"})
        )
        record = self.controller.devices["android-01"]
        record.status = "disconnected"

        self.controller.broadcast_command("sync_request")
        queue = self.controller._command_queue.get("android-01")
        self.assertIsNotNone(queue)
        self.assertGreaterEqual(len(queue), 1)

        sent_messages = []

        class DummyWorker:
            def __init__(self):
                self.connection_id = "conn-2"
                self.device_id = "android-01"
                self._address = ("127.0.0.1", 60000)

            @property
            def address(self):
                return self._address

            def send_json(self, message):
                sent_messages.append(message)

        worker = DummyWorker()
        context = ConnectionContext(server=self.controller._network, worker=worker)
        replayed = self.controller._drain_offline_queue("android-01", context)

        self.assertTrue(replayed)
        self.assertTrue(any(msg.get("command") == "sync_request" for msg in sent_messages))
        self.assertTrue(any("commandId" in msg for msg in sent_messages))
        self.assertTrue(any("payload" in msg for msg in sent_messages))
        self.assertNotIn("android-01", self.controller._command_queue)


if __name__ == "__main__":
    unittest.main()
