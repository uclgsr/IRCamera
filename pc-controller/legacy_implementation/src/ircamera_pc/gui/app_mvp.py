"""
MVP Application Entry Point for IRCamera PC Controller Hub

Integrates all components and provides the main application framework.
Implements the architecture and module setup requirements from the PC Hub Application MVP checklist.
"""

import asyncio
import signal
import sys
from pathlib import Path
from typing import Optional

from loguru import logger
from PyQt6.QtCore import QTimer
from PyQt6.QtWidgets import QApplication, QMessageBox

from ..core.config import config
from ..core.device_manager import DeviceManager
from ..core.session_manager import AdvancedSessionManager
from ..network.websocket_server import WebSocketServer
from ..sync import AdvancedTimeSyncServer
from .main_window_mvp import MVPMainWindow


class IRCameraHubApplication:
    """
    Main Hub Application coordinating all components.
    
    Implements the Hub-and-Spoke architecture with:
    - Device discovery and management
    - Session lifecycle coordination
    - Network communication
    - GUI interface
    """

    def __init__(self):
        """Initialize the Hub application."""
        self.app: Optional[QApplication] = None
        self.main_window: Optional[MVPMainWindow] = None
        self.device_manager: Optional[DeviceManager] = None
        self.session_manager: Optional[AdvancedSessionManager] = None
        self.websocket_server: Optional[WebSocketServer] = None
        self.time_sync_server: Optional[AdvancedTimeSyncServer] = None

        # Configuration
        self.base_session_dir = Path(config.get("sessions.base_directory", "./sessions"))
        self.server_port = config.get("network.server_port", 8080)
        self.time_sync_port = config.get("sync.server_port", 1234)

        # Async event loop integration
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._loop_timer: Optional[QTimer] = None

    def initialize(self) -> bool:
        """
        Initialize all application components.
        
        Returns:
            True if initialization successful
        """
        try:
            logger.info("Initializing IRCamera PC Controller Hub...")

            # Create Qt Application
            self.app = QApplication(sys.argv)
            self.app.setApplicationName("IRCamera PC Controller Hub")
            self.app.setApplicationVersion("1.0.0-MVP")

            # Setup async event loop integration
            self._setup_async_integration()

            # Initialize core components
            self._initialize_core_components()

            # Initialize GUI
            self._initialize_gui()

            # Start services
            if not self._start_services():
                logger.error("Failed to start core services")
                return False

            logger.info("Hub application initialized successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to initialize application: {e}")
            return False

    def _setup_async_integration(self):
        """Setup async event loop integration with Qt."""
        # Create event loop for async operations
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)

        # Setup timer to process async events
        self._loop_timer = QTimer()
        self._loop_timer.timeout.connect(self._process_async_events)
        self._loop_timer.start(10)  # Process every 10ms

        logger.debug("Async event loop integration setup complete")

    def _process_async_events(self):
        """Process pending async events."""
        if self._loop:
            # Process up to 10 callbacks per timer tick to avoid blocking GUI
            for _ in range(10):
                if self._loop._ready:
                    self._loop._run_once()
                else:
                    break

    def _initialize_core_components(self):
        """Initialize core application components."""
        logger.info("Initializing core components...")

        # Create base session directory
        self.base_session_dir.mkdir(parents=True, exist_ok=True)

        # Initialize device manager
        self.device_manager = DeviceManager()
        logger.info("Device manager initialized")

        # Initialize session manager
        self.session_manager = AdvancedSessionManager(
            device_manager=self.device_manager,
            base_session_dir=self.base_session_dir
        )
        logger.info("Session manager initialized")

        # Setup inter-component callbacks
        self._setup_component_integration()

        logger.info("Core components initialization complete")

    def _setup_component_integration(self):
        """Setup integration between components."""

        # Device manager callbacks
        def on_device_status_change(device_id: str, device_info, event_type: str):
            logger.info(f"Device {device_id} status changed: {event_type}")

        self.device_manager.add_status_callback(on_device_status_change)

        # Session manager callbacks
        def on_session_state_change(state, session):
            if session:
                logger.info(f"Session {session.session_id} state changed to: {state.value}")
            else:
                logger.info(f"Session state changed to: {state.value}")

        self.session_manager.add_state_callback(on_session_state_change)

        logger.debug("Component integration setup complete")

    def _initialize_gui(self):
        """Initialize GUI components."""
        logger.info("Initializing GUI...")

        # Create main window
        self.main_window = MVPMainWindow(
            device_manager=self.device_manager,
            session_manager=self.session_manager
        )

        # Setup window
        self.main_window.show()

        # Add initial log messages to GUI
        self.main_window.logging_console.add_log_message("IRCamera PC Controller Hub started")
        self.main_window.logging_console.add_log_message("Initializing device discovery...")

        logger.info("GUI initialization complete")

    def _start_services(self) -> bool:
        """
        Start core services.
        
        Returns:
            True if all services started successfully
        """
        logger.info("Starting core services...")

        try:
            # Start device manager (includes discovery service)
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

            # Schedule async startup with non-blocking approach
            future = asyncio.ensure_future(start_device_manager())

            # Use callback instead of blocking loop
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

                # Start other services after device manager is ready
                self._start_additional_services()
                return True

            # Check if already complete, otherwise schedule callback
            if future.done():
                return handle_device_manager_result()
            else:
                # Schedule check for next event loop iteration
                QTimer.singleShot(100, handle_device_manager_result)
                logger.info("Device manager startup scheduled...")
                return True  # Return True to continue with UI, services will start async

        except Exception as e:
            logger.error(f"Failed to start services: {e}")
            return False

    def _start_additional_services(self) -> None:
        """Start additional services after device manager is ready."""
        try:
            # Start WebSocket server
            async def start_websocket():
                return await self._start_websocket_server()

            ws_future = asyncio.ensure_future(start_websocket())

            # Start time sync service  
            async def start_timesync():
                return await self._start_time_sync_server()

            ts_future = asyncio.ensure_future(start_timesync())

            # Use callbacks instead of blocking
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

            # Schedule callbacks
            QTimer.singleShot(200, handle_websocket_result)
            QTimer.singleShot(300, handle_timesync_result)

        except Exception as e:
            logger.error(f"Error starting additional services: {e}")

    async def _start_websocket_server(self) -> bool:
        """
        Start the WebSocket server for device communication.
        
        Returns:
            True if started successfully
        """
        try:
            logger.info(f"Starting WebSocket server on port {self.server_port}...")

            # Create WebSocket server instance
            self.websocket_server = WebSocketServer(
                host="0.0.0.0",
                port=self.server_port
            )

            # Start the server
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
        """
        Start the time synchronization server.
        
        Returns:
            True if started successfully
        """
        try:
            logger.info(f"Starting time sync server on port {self.time_sync_port}...")

            # Create time sync server instance
            self.time_sync_server = AdvancedTimeSyncServer(port=self.time_sync_port)

            # Start the server
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
        """
        Run the application.
        
        Returns:
            Application exit code
        """
        if not self.app:
            logger.error("Application not initialized")
            return 1

        logger.info("Starting application event loop...")

        # Setup signal handling for graceful shutdown
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        try:
            # Run Qt event loop
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
        """Handle system signals for graceful shutdown."""
        logger.info(f"Received signal {signum}, initiating shutdown...")

        if self.app:
            self.app.quit()

    def _cleanup(self):
        """Cleanup resources on application exit."""
        logger.info("Cleaning up application resources...")

        try:
            async def cleanup_async():
                # Stop WebSocket server
                if self.websocket_server:
                    await self.websocket_server.stop()
                    logger.info("WebSocket server stopped")

                # Stop time sync server  
                if self.time_sync_server:
                    await self.time_sync_server.stop()
                    logger.info("Time sync server stopped")

                # Stop device manager
                if self.device_manager:
                    await self.device_manager.stop()
                    logger.info("Device manager stopped")

            # Run cleanup in event loop
            if self._loop and not self._loop.is_closed():
                self._loop.run_until_complete(cleanup_async())

            # Stop event loop timer
            if self._loop_timer:
                self._loop_timer.stop()

            # Close event loop
            if self._loop and not self._loop.is_closed():
                self._loop.close()

            logger.info("Application cleanup complete")

        except Exception as e:
            logger.error(f"Error during cleanup: {e}")

    # Public interface methods

    def get_device_manager(self) -> Optional[DeviceManager]:
        """Get device manager instance."""
        return self.device_manager

    def get_session_manager(self) -> Optional[AdvancedSessionManager]:
        """Get session manager instance."""
        return self.session_manager

    def get_main_window(self) -> Optional[MVPMainWindow]:
        """Get main window instance."""
        return self.main_window


def main() -> int:
    """
    Main entry point for the IRCamera PC Controller Hub application.
    
    Returns:
        Application exit code
    """
    # Configure logging
    logger.remove()  # Remove default handler
    logger.add(
        sys.stderr,
        level="INFO",
        format="<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
               "<level>{level: <8}</level> | "
               "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
               "<level>{message}</level>"
    )

    # Add file logging
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
        # Create and initialize application
        app = IRCameraHubApplication()

        if not app.initialize():
            logger.error("Application initialization failed")
            return 1

        # Run application
        return app.run()

    except Exception as e:
        logger.error(f"Unhandled application error: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
