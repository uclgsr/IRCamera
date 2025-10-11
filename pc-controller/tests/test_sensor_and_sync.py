import json
import socket
import struct
import time
import unittest
import warnings
from http.client import HTTPConnection
from pathlib import Path

from sensor_manager import SensorConfig, SensorManager, load_sensor_config
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

    def test_simulation_mode_toggle(self):
        samples = []

        def callback(sensor_id: str, timestamp: float, value: float):
            samples.append((sensor_id, timestamp, value))

        cfg = {"phys": SensorConfig(name="Physical", address=None)}
        manager = SensorManager(callback, cfg)
        manager.enable_simulation_mode()
        self.assertTrue(manager.is_simulation_enabled())
        self.assertIn("phys", manager.active_sensors())

        manager.add_simulated_sensor("extra", sample_rate_hz=64.0)
        self.assertIn("extra", manager.active_sensors())

        started = list(manager.start_streaming())
        self.assertIn("extra", started)
        time.sleep(0.02)
        manager.stop_streaming()
        self.assertTrue(samples)

        manager.disable_simulation_mode()
        self.assertFalse(manager.is_simulation_enabled())

    def test_load_sensor_config(self):
        sample_cfg = {
            "alpha": {
                "name": "Alpha",
                "address": "AA:BB:CC:DD:EE:FF",
                "sample_rate_hz": 200,
                "metadata": {"channel": "1"},
            }
        }
        configs = load_sensor_config(sample_cfg)
        self.assertIn("alpha", configs)
        self.assertEqual(configs["alpha"].name, "Alpha")
        self.assertEqual(configs["alpha"].metadata["channel"], "1")


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

    def test_dump_and_clear(self):
        controller = StimulusController(lambda *_: None)
        controller.flash_sync()
        controller.audio_beep()
        controller.stimulus_video(Path("video.mp4"))
        controller.custom_marker("marker")

        self.assertEqual(len(controller.dump()), 4)
        controller.clear()
        self.assertEqual(controller.dump(), [])


class TimeSyncServiceTestCase(unittest.TestCase):
    def test_time_sync_udp_round_trip(self):
        service = TimeSyncService(TimeSyncConfig(host="127.0.0.1", port=0))
        started = service.start()
        self.assertTrue(started)
        try:
            sock = service._udp_socket  # type: ignore[attr-defined]
            self.assertIsNotNone(sock)
            addr = sock.getsockname()

            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as client:
                client.settimeout(1.0)
                client.sendto(struct.pack("!Q", time.perf_counter_ns()), addr)
                data, _ = client.recvfrom(64)
            self.assertEqual(len(data), 16)
            t1, t2 = struct.unpack("!QQ", data)
            self.assertGreater(t2, t1)
        finally:
            service.stop()
            time.sleep(0.05)

    def test_time_sync_http_calibration_round_trip(self):
        config = TimeSyncConfig(host="127.0.0.1", port=0, http_port=0, ttl_seconds=5.0)
        service = TimeSyncService(config)
        started = service.start()
        self.assertTrue(started)
        try:
            http_server = service._http_server  # type: ignore[attr-defined]
            self.assertIsNotNone(http_server)
            host, port = http_server.server_address

            conn = HTTPConnection(host, port, timeout=1.0)
            payload = {
                "referenceEpochMillis": time.time() * 1000.0,
                "offsetMillis": 4.2,
                "roundTripMillis": 8.4,
                "driftPpm": 0.5,
                "accuracyMillis": 6.1,
            }
            conn.request(
                "POST",
                "/time/calibration",
                body=json.dumps(payload),
                headers={"Content-Type": "application/json"},
            )
            response = conn.getresponse()
            self.assertEqual(response.status, 202)
            response.read()

            conn.request("GET", "/time/calibration")
            response = conn.getresponse()
            self.assertEqual(response.status, 200)
            data = json.loads(response.read().decode("utf-8"))
            self.assertAlmostEqual(data["offsetMillis"], payload["offsetMillis"])
            self.assertAlmostEqual(data["roundTripMillis"], payload["roundTripMillis"])
            self.assertAlmostEqual(data["driftPpm"], payload["driftPpm"])
            conn.close()
        finally:
            service.stop()
            time.sleep(0.05)


if __name__ == "__main__":
    unittest.main()
warnings.filterwarnings("ignore", category=ResourceWarning)
