import json
import tempfile
import time
import unittest
from pathlib import Path

from pc_controller import PCControllerCore, handle_cli_command


class CLIHandlerTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        storage = Path(self.tmp.name) / "pc_data"
        config = {"simulation": {"force": True}}
        self.controller = PCControllerCore(storage, config=config)

    def tearDown(self) -> None:
        try:
            self.controller.stop_recording()
        except Exception:
            pass
        finally:
            self.controller._stop_local_sensors()
            self.tmp.cleanup()

    def _run(self, command: str):
        should_exit, lines = handle_cli_command(self.controller, command)
        return should_exit, lines

    def test_status_command(self):
        should_exit, lines = self._run("status")
        self.assertFalse(should_exit)
        self.assertIn("Simulation:", lines[-1])

    def test_start_stop_commands(self):
        _, start_lines = self._run("start")
        self.assertTrue(any("Session" in line and "started" in line for line in start_lines))
        _, stop_lines = self._run("stop")
        self.assertTrue(any("stopped" in line for line in stop_lines))

    def test_flash_beep_marker(self):
        _, flash_lines = self._run("flash")
        self.assertTrue(flash_lines and "Flash sync" in flash_lines[0])

        _, beep_lines = self._run("beep")
        self.assertTrue(beep_lines and "Audio sync" in beep_lines[0])

        _, marker_lines = self._run("marker custom")
        self.assertTrue(marker_lines and "custom" in marker_lines[0])

    def test_simulation_toggle(self):
        _, lines = self._run("simulate toggle")
        self.assertTrue(lines and "Simulation mode" in lines[0])
        _, lines = self._run("simulate off")
        self.assertTrue(lines and "disabled" in lines[0])
        _, lines = self._run("simulate on")
        self.assertTrue(lines and "enabled" in lines[0])

    def test_devices_command(self):
        _, lines = self._run("devices")
        # Should return JSON per device (may be empty list)
        for line in lines:
            json.loads(line)

    def test_help_and_quit(self):
        _, lines = self._run("help")
        self.assertTrue(lines and "Commands" in lines[0])
        should_exit, lines = self._run("quit")
        self.assertTrue(should_exit)
        self.assertFalse(lines)

    def test_unknown_command(self):
        _, lines = self._run("unknown_cmd")
        self.assertIn("Unknown command", lines[0])


if __name__ == "__main__":
    unittest.main()
