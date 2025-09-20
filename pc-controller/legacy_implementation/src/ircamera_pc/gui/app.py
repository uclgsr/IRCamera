import asyncio
import os
import signal
import sys
from typing import Optional, Any

from loguru import logger

GUI_AVAILABLE = True
try:

    if "DISPLAY" not in os.environ and "QT_QPA_PLATFORM" not in os.environ:
        os.environ["QT_QPA_PLATFORM"] = "offscreen"

    from PyQt6.QtCore import QTimer
    from PyQt6.QtWidgets import QApplication
except ImportError as e:
    logger.warning(f"GUI libraries not available, running in headless mode: {e}")
    GUI_AVAILABLE = False


    # Mock classes for headless mode
    class QApplication:
        def __init__(self, *args):
            pass

        def setApplicationName(self, name) -> None:
            pass

        def setApplicationVersion(self, version) -> None:
            pass

        def setStyleSheet(self, style) -> None:
            pass

        def exec(self) -> Any:
            return 0


    class QTimer:
        def __init__(self):
            self.timeout_func = None

        def timeout(self) -> Any:
            return self

        def connect(self, func) -> Any:
            self.timeout_func = func

        def start(self, interval) -> Any:
            pass

        def stop(self) -> Any:
            pass

from ..core import SessionManager, config
from ..core.admin_privileges import AdminPrivilegesManager
from ..core.bluetooth_manager import BluetoothManager
from ..core.calibration import CameraCalibrator
from ..core.file_transfer import FileTransferManager
from ..core.gsr_ingestor import GSRIngestor
from ..core.timesync import TimeSyncService
from ..core.wifi_manager import WiFiManager
from ..network.websocket_server import WebSocketServer
from .utils import setup_logging

if GUI_AVAILABLE:
    from .main_window import MainWindow
else:

    class MainWindow:
        def __init__(self, *args, **kwargs):
            logger.info("MainWindow created in headless mode")

        def show(self) -> Any:
            pass

        def resize(self, w, h) -> Any:
            pass


class IRCameraApp:

    def __init__(self):

        self.config = config

        self.session_manager = SessionManager()
        self.time_sync_service = TimeSyncService()
        self.websocket_server = WebSocketServer()

        self.gsr_ingestor = GSRIngestor(self.config)
        self.file_transfer_manager = FileTransferManager(self.config)
        self.camera_calibrator = CameraCalibrator(self.config)

        self.admin_privileges_manager = AdminPrivilegesManager()
        self.bluetooth_manager = BluetoothManager()
        self.wifi_manager = WiFiManager()

        self.qt_app: Optional[QApplication] = None
        self.main_window: Optional[MainWindow] = None

        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._timer: Optional[QTimer] = None

        logger.info(
            "IRCamera Application initialized with system integration features"
        )
        components = [
            "Session Manager",
            "Time Sync",
            "WebSocket Server",
            "GSR Ingestor",
            "File Transfer",
            "Camera Calibrator",
            "Admin Privileges",
            "Bluetooth Manager",
            "WiFi Manager",
        ]
        logger.info(f"Components: {', '.join(components)}")

    def setup_qt_app(self) -> None:

        if not GUI_AVAILABLE:
            logger.info("Running in headless mode - GUI not available")
            self.qt_app = QApplication(sys.argv)
            return

        if self.qt_app is None:
            self.qt_app = QApplication(sys.argv)
            self.qt_app.setApplicationName("IRCamera PC Controller")
            self.qt_app.setApplicationVersion("0.1.0")

            self.qt_app.setStyleSheet(

            )

        if GUI_AVAILABLE:
            self.main_window = MainWindow(
                session_manager=self.session_manager,
                websocket_server=self.websocket_server,
                time_sync_service=self.time_sync_service,
                gsr_ingestor=self.gsr_ingestor,
                file_transfer_manager=self.file_transfer_manager,
                camera_calibrator=self.camera_calibrator,
                bluetooth_manager=self.bluetooth_manager,
                wifi_manager=self.wifi_manager,
                admin_privileges_manager=self.admin_privileges_manager,
            )

            window_size = config.get("gui.window_size", [1400, 900])
            self.main_window.resize(window_size[0], window_size[1])
        else:
            logger.info("Main window not created - running in headless mode")

        logger.info("Qt application set up")

    def setup_event_loop_integration(self) -> None:

        try:
            self._loop = asyncio.get_event_loop()
        except RuntimeError:
            self._loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self._loop)

        self._timer = QTimer()
        self._timer.timeout.connect(self._process_async_events)

        update_interval = config.get("gui.update_interval_ms", 100)
        self._timer.start(update_interval)

        logger.debug("Event loop integration set up")

    def _process_async_events(self) -> None:

        if self._loop:
            try:

                for _ in range(10):
                    if self._loop._ready:
                        handle = self._loop._ready.popleft()
                        if not handle.cancelled():
                            handle._run()
                    else:
                        break
            except (OSError, ValueError, RuntimeError) as e:
                logger.error(f"Error processing async events: {e}")

    async def start_services(self) -> None:

        try:

            await self.time_sync_service.start()

            await self.websocket_server.start()

            logger.info("All services started successfully")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to start services: {e}")
            raise

    async def stop_services(self) -> None:

        try:

            if self.wifi_manager:
                await self.wifi_manager.cleanup()

            if self.bluetooth_manager:
                await self.bluetooth_manager.cleanup()

            await self.websocket_server.stop()
            await self.time_sync_service.stop()

            logger.info("All services and system integration managers stopped")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error stopping services: {e}")

    def run(self) -> int:

        try:

            setup_logging()

            self.setup_qt_app()

            self.setup_event_loop_integration()

            signal.signal(signal.SIGINT, self._handle_signal)
            signal.signal(signal.SIGTERM, self._handle_signal)

            asyncio.run_coroutine_threadsafe(self.start_services(), self._loop)

            if GUI_AVAILABLE and self.main_window:
                self.main_window.show()
            else:
                logger.info("Running in headless mode - no GUI window to show")

            logger.info("IRCamera PC Controller started")

            if GUI_AVAILABLE:
                return self.qt_app.exec_()
            else:

                logger.info("Headless mode - press Ctrl+C to stop")
                try:
                    while True:
                        if self._loop:
                            self._process_async_events()
                        import time

                        time.sleep(0.1)
                except KeyboardInterrupt:
                    logger.info("Keyboard interrupt received, shutting down...")
                    return 0

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Application error: {e}")
            return 1
        finally:

            if self._timer:
                self._timer.stop()

            if self._loop and not self._loop.is_closed():
                try:
                    future = asyncio.run_coroutine_threadsafe(
                        self.stop_services(), self._loop
                    )
                    future.result(timeout=5)
                except (OSError, ValueError, RuntimeError) as e:
                    logger.error(f"Error during cleanup: {e}")

    def _handle_signal(self, signum: int, frame) -> None:

        logger.info(f"Received signal {signum}, shutting down...")

        if self.qt_app:
            self.qt_app.quit()


def main() -> int:
    import argparse

    parser = argparse.ArgumentParser(
        description="IRCamera PC Controller - Multi-Modal Physiological Sensing Platform Hub"
    )
    parser.add_argument(
        "--version", action="version", version="IRCamera PC Controller v1.0.0"
    )
    parser.add_argument("--config", help="Path to configuration file")
    parser.add_argument("--debug", action="store_true", help="Enable debug logging")
    parser.add_argument(
        "--headless", action="store_true", help="Run in headless mode (no GUI)"
    )

    args = parser.parse_args()

    if args.debug:
        logger.info("Debug mode enabled")

    app = IRCameraApp()

    if args.headless:
        logger.info("Running in headless mode - network services only")

        parser.print_help()
        return 0

    return app.run()


if __name__ == "__main__":
    sys.exit(main())
