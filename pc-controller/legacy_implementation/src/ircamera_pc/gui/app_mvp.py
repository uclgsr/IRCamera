import asyncio
import signal
import sys
from PyQt6.QtCore import QTimer
from PyQt6.QtWidgets import QApplication
from loguru import logger
from pathlib import Path
from typing import Optional

from .main_window_mvp import MVPMainWindow
from ..core.config import config
from ..core.device_manager import DeviceManager
from ..core.session_manager import AdvancedSessionManager
from ..network.websocket_server import WebSocketServer
from ..sync import AdvancedTimeSyncServer


class IRCameraHubApplication:

    def __init__(self):

        self.app: Optional[QApplication] = None
        self.main_window: Optional[MVPMainWindow] = None
        self.device_manager: Optional[DeviceManager] = None
        self.session_manager: Optional[AdvancedSessionManager] = None
        self.websocket_server: Optional[WebSocketServer] = None
        self.time_sync_server: Optional[AdvancedTimeSyncServer] = None

        self.base_session_dir = Path(config.get("sessions.base_directory", "./sessions"))
        self.server_port = config.get("network.server_port", 8080)
        self.time_sync_port = config.get("sync.server_port", 1234)

        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._loop_timer: Optional[QTimer] = None

    def initialize(self) -> bool:

        try:
            logger.info("Initializing IRCamera PC Controller Hub...")

            self.app = QApplication(sys.argv)
            self.app.setApplicationName("IRCamera PC Controller Hub")
            self.app.setApplicationVersion("1.0.0-MVP")

            self._setup_async_integration()

            self._initialize_core_components()

            self._initialize_gui()

            if not self._start_services():
                logger.error("Failed to start core services")
                return False

            logger.info("Hub application initialized successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to initialize application: {e}")
            return False

    def _setup_async_integration(self):

        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)

        self._loop_timer = QTimer()
        self._loop_timer.timeout.connect(self._process_async_events)
        self._loop_timer.start(10)

        logger.debug("Async event loop integration setup complete")

    def _process_async_events(self):

        if self._loop:

            for _ in range(10):
                if self._loop._ready:
                    self._loop._run_once()
                else:
                    break

    def _initialize_core_components(self):

        logger.info("Initializing core components...")

        self.base_session_dir.mkdir(parents=True, exist_ok=True)

        self.device_manager = DeviceManager()
        logger.info("Device manager initialized")

        self.session_manager = AdvancedSessionManager(
            device_manager=self.device_manager,
            base_session_dir=self.base_session_dir
        )
        logger.info("Session manager initialized")

        self._setup_component_integration()

        logger.info("Core components initialization complete")

    def _setup_component_integration(self):

        def on_device_status_change(device_id: str, device_info, event_type: str):
            logger.info(f"Device {device_id} status changed: {event_type}")

        self.device_manager.add_status_callback(on_device_status_change)

        def on_session_state_change(state, session):
            if session:
                logger.info(f"Session {session.session_id} state changed to: {state.value}")
            else:
                logger.info(f"Session state changed to: {state.value}")

        self.session_manager.add_state_callback(on_session_state_change)

        logger.debug("Component integration setup complete")

    def _initialize_gui(self):

        logger.info("Initializing GUI...")

        self.main_window = MVPMainWindow(
            device_manager=self.device_manager,
            session_manager=self.session_manager
        )

        self.main_window.show()

        self.main_window.logging_console.add_log_message("IRCamera PC Controller Hub started")
        self.main_window.logging_console.add_log_message("Initializing device discovery...")

        logger.info("GUI initialization complete")

    def _start_services(self) -> bool:

        logger.info("Starting core services...")

        try:

            async def start_device_manager():
                success = await self.device_manager.start()
                if success:
                    self.main_window.logging_console.add_log_message("Device discovery started")
                    logger.info("Device manager started successfully")
                else:
                    self.main_window.logging_console.add_log_message(
                        "Failed to start device discovery", "ERROR")
                    logger.error("Failed to start device manager")
                return success

            future = asyncio.ensure_future(start_device_manager())

            def handle_device_manager_result():
                if future.done():
                    try:
                        result = future.result()
                        if not result:
                            self.main_window.logging_console.add_log_message(
                                "Device manager startup failed", "ERROR")
                            return False
                    except Exception as e:
                        logger.error(f"Device manager startup error: {e}")
                        return False

                self._start_additional_services()
                return True

            if future.done():
                return handle_device_manager_result()
            else:

                QTimer.singleShot(100, handle_device_manager_result)
                logger.info("Device manager startup scheduled...")
                return True

        except Exception as e:
            logger.error(f"Failed to start services: {e}")
            return False

    def _start_additional_services(self) -> None:

        try:

            async def start_websocket():
                return await self._start_websocket_server()

            ws_future = asyncio.ensure_future(start_websocket())

            async def start_timesync():
                return await self._start_time_sync_server()

            ts_future = asyncio.ensure_future(start_timesync())

            def handle_websocket_result():
                if ws_future.done():
                    try:
                        ws_result = ws_future.result()
                        if ws_result:
                            logger.info("WebSocket server started successfully")
                            self.main_window.logging_console.add_log_message(
                                "WebSocket server started")
                        else:
                            logger.warning("WebSocket server failed to start")
                            self.main_window.logging_console.add_log_message(
                                "WebSocket server failed", "WARNING")
                    except Exception as e:
                        logger.error(f"WebSocket server error: {e}")

            def handle_timesync_result():
                if ts_future.done():
                    try:
                        ts_result = ts_future.result()
                        if ts_result:
                            logger.info("Time sync server started successfully")
                            self.main_window.logging_console.add_log_message(
                                "Time sync server started")
                        else:
                            logger.warning("Time sync server failed to start")
                            self.main_window.logging_console.add_log_message(
                                "Time sync server failed", "WARNING")
                    except Exception as e:
                        logger.error(f"Time sync server error: {e}")

            QTimer.singleShot(200, handle_websocket_result)
            QTimer.singleShot(300, handle_timesync_result)

        except Exception as e:
            logger.error(f"Error starting additional services: {e}")

    async def _start_websocket_server(self) -> bool:

        try:
            logger.info(f"Starting WebSocket server on port {self.server_port}...")

            self.websocket_server = WebSocketServer(
                host="0.0.0.0",
                port=self.server_port
            )

            success = await self.websocket_server.start()

            if success:
                logger.info(f"WebSocket server started successfully on port {self.server_port}")
                self.main_window.logging_console.add_log_message(
                    f"WebSocket server started on port {self.server_port}")
                return True
            else:
                logger.error("WebSocket server failed to start")
                return False

        except Exception as e:
            logger.error(f"Failed to start WebSocket server: {e}")
            return False

    async def _start_time_sync_server(self) -> bool:

        try:
            logger.info(f"Starting time sync server on port {self.time_sync_port}...")

            self.time_sync_server = AdvancedTimeSyncServer(port=self.time_sync_port)

            success = await self.time_sync_server.start()

            if success:
                logger.info(f"Time sync server started successfully on port {self.time_sync_port}")
                self.main_window.logging_console.add_log_message(
                    f"Time sync server started on port {self.time_sync_port}")
                return True
            else:
                logger.error("Time sync server failed to start")
                return False

        except Exception as e:
            logger.error(f"Failed to start time sync server: {e}")
            return False

    def run(self) -> int:

        if not self.app:
            logger.error("Application not initialized")
            return 1

        logger.info("Starting application event loop...")

        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        try:

            exit_code = self.app.exec()
            logger.info(f"Application exited with code: {exit_code}")
            return exit_code

        except KeyboardInterrupt:
            logger.info("Application interrupted by user")
            return 0
        except Exception as e:
            logger.error(f"Application error: {e}")
            return 1
        finally:
            self._cleanup()

    def _signal_handler(self, signum, frame):

        logger.info(f"Received signal {signum}, initiating shutdown...")

        if self.app:
            self.app.quit()

    def _cleanup(self):

        logger.info("Cleaning up application resources...")

        try:
            async def cleanup_async():

                if self.websocket_server:
                    await self.websocket_server.stop()
                    logger.info("WebSocket server stopped")

                if self.time_sync_server:
                    await self.time_sync_server.stop()
                    logger.info("Time sync server stopped")

                if self.device_manager:
                    await self.device_manager.stop()
                    logger.info("Device manager stopped")

            if self._loop and not self._loop.is_closed():
                self._loop.run_until_complete(cleanup_async())

            if self._loop_timer:
                self._loop_timer.stop()

            if self._loop and not self._loop.is_closed():
                self._loop.close()

            logger.info("Application cleanup complete")

        except Exception as e:
            logger.error(f"Error during cleanup: {e}")

    def get_device_manager(self) -> Optional[DeviceManager]:

        return self.device_manager

    def get_session_manager(self) -> Optional[AdvancedSessionManager]:

        return self.session_manager

    def get_main_window(self) -> Optional[MVPMainWindow]:

        return self.main_window


def main() -> int:
    logger.remove()
    logger.add(
        sys.stderr,
        level="INFO",
        format="<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
               "<level>{level: <8}</level> | "
               "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
               "<level>{message}</level>"
    )

    log_dir = Path("logs")
    log_dir.mkdir(exist_ok=True)

    logger.add(
        log_dir / "ircamera_hub_{time:YYYY-MM-DD}.log",
        level="DEBUG",
        rotation="1 day",
        retention="7 days",
        format="{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | {name}:{function}:{line} - {message}"
    )

    logger.info("Starting IRCamera PC Controller Hub Application")

    try:

        app = IRCameraHubApplication()

        if not app.initialize():
            logger.error("Application initialization failed")
            return 1

        return app.run()

    except Exception as e:
        logger.error(f"Unhandled application error: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
