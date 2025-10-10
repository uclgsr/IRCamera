import json
import socket
import time
import unittest
import warnings
from pathlib import Path

from sensor_manager import SensorConfig, SensorManager
from stimulus_controller import StimulusController
from time_sync_service import TimeSyncConfig, TimeSyncService


class SensorManagerTestCase(unittest.TestCase):
    def test_simulated_sensor_generates_samples(self):
        samples = []

        def callback(sensor_id: str, timestamp: float, value: float):
            samples.append((sensor_id, timestamp, value))

        manager = SensorManager(callback, {"sim": SensorConfig(name="Simulated")})
        started = list(manager.start_streaming())
        self.assertIn("sim", started)
        time.sleep(0.05)
        manager.stop_streaming()
        self.assertGreater(len(samples), 0)
        ids = {sensor for sensor, _, _ in samples}
        self.assertEqual(ids, {"sim"})


class StimulusControllerTestCase(unittest.TestCase):
    def test_event_logging(self):
        recorded = []

        def notifier(code: str, payload):
            recorded.append((code, payload))

        controller = StimulusController(notifier)
        event = controller.flash_sync(intensity=0.8, duration_s=0.3)
        controller.audio_beep()
        controller.custom_marker("test", {"value": 42})

        self.assertEqual(recorded[0][0], "flash_sync")
        events = controller.dump()
        self.assertEqual(events[0].code, "flash_sync")
        self.assertGreater(events[0].timestamp, 0)
        self.assertIn("duration_s", events[0].payload)
        self.assertEqual(recorded[-1][0], "test")


class TimeSyncServiceTestCase(unittest.TestCase):
    def test_time_sync_udp_round_trip(self):
        service = TimeSyncService(TimeSyncConfig(host="127.0.0.1", port=0))
        started = service.start()
        self.assertTrue(started)
        try:
            sock = service._socket  # type: ignore[attr-defined]
            self.assertIsNotNone(sock)
            addr = sock.getsockname()

            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as client:
                client.settimeout(1.0)
                client.sendto(b"ping", addr)
                data, _ = client.recvfrom(4096)
            payload = json.loads(data.decode("utf-8"))
            self.assertEqual(payload["type"], "time_sync")
            self.assertIn("sequence", payload)
        finally:
            service.stop()
            time.sleep(0.05)


if __name__ == "__main__":
    unittest.main()
warnings.filterwarnings("ignore", category=ResourceWarning)
