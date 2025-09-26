import asyncio
import signal
import sys
from PyQt6.QtCore import QTimer
from PyQt6.QtWidgets import QApplication
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
            
            self.app = QApplication(sys.argv)
            self.app.setApplicationName("IRCamera PC Controller Hub")
            self.app.setApplicationVersion("1.0.0-MVP")

            self._setup_async_integration()

            self._initialize_core_components()

            self._initialize_gui()

            if not self._start_services():
                                return False

                        return True

        except Exception as e:
                        return False

    def _setup_async_integration(self):

        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)

        self._loop_timer = QTimer()
        self._loop_timer.timeout.connect(self._process_async_events)
        self._loop_timer.start(10)

        
    def _process_async_events(self):

        if self._loop:

            for _ in range(10):
                if self._loop._ready:
                    self._loop._run_once()
                else:
                    break

    def _initialize_core_components(self):

        
        self.base_session_dir.mkdir(parents=True, exist_ok=True)

        self.device_manager = DeviceManager()
        
        self.session_manager = AdvancedSessionManager(
            device_manager=self.device_manager,
            base_session_dir=self.base_session_dir
        )
        
        self._setup_component_integration()

        
    def _setup_component_integration(self):

        def on_device_status_change(device_id: str, device_info, event_type: str):
            
        self.device_manager.add_status_callback(on_device_status_change)

        def on_session_state_change(state, session):
            if session:
                            else:
                
        self.session_manager.add_state_callback(on_session_state_change)

        
    def _initialize_gui(self):

        
        self.main_window = MVPMainWindow(
            device_manager=self.device_manager,
            session_manager=self.session_manager
        )

        self.main_window.show()

        self.main_window.logging_console.add_log_message("IRCamera PC Controller Hub started")
        self.main_window.logging_console.add_log_message("Initializing device discovery...")

        
    def _start_services(self) -> bool:

        
        try:

            async def start_device_manager():
                success = await self.device_manager.start()
                if success:
                    self.main_window.logging_console.add_log_message("Device discovery started")
                                    else:
                    self.main_window.logging_console.add_log_message(
                        "Failed to start device discovery", "ERROR")
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
                                                return False

                self._start_additional_services()
                return True

            if future.done():
                return handle_device_manager_result()
            else:

                QTimer.singleShot(100, handle_device_manager_result)
                                return True

        except Exception as e:
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
                                                        self.main_window.logging_console.add_log_message(
                                "WebSocket server started")
                        else:
                                                        self.main_window.logging_console.add_log_message(
                                "WebSocket server failed", "WARNING")
                    except Exception as e:
                        
            def handle_timesync_result():
                if ts_future.done():
                    try:
                        ts_result = ts_future.result()
                        if ts_result:
                                                        self.main_window.logging_console.add_log_message(
                                "Time sync server started")
                        else:
                                                        self.main_window.logging_console.add_log_message(
                                "Time sync server failed", "WARNING")
                    except Exception as e:
                        
            QTimer.singleShot(200, handle_websocket_result)
            QTimer.singleShot(300, handle_timesync_result)

        except Exception as e:
            
    async def _start_websocket_server(self) -> bool:

        try:
            
            self.websocket_server = WebSocketServer(
                host="0.0.0.0",
                port=self.server_port
            )

            success = await self.websocket_server.start()

            if success:
                                self.main_window.logging_console.add_log_message(
                    f"WebSocket server started on port {self.server_port}")
                return True
            else:
                                return False

        except Exception as e:
                        return False

    async def _start_time_sync_server(self) -> bool:

        try:
            
            self.time_sync_server = AdvancedTimeSyncServer(port=self.time_sync_port)

            success = await self.time_sync_server.start()

            if success:
                                self.main_window.logging_console.add_log_message(
                    f"Time sync server started on port {self.time_sync_port}")
                return True
            else:
                                return False

        except Exception as e:
                        return False

    def run(self) -> int:

        if not self.app:
                        return 1

        
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        try:

            exit_code = self.app.exec()
                        return exit_code

        except KeyboardInterrupt:
                        return 0
        except Exception as e:
                        return 1
        finally:
            self._cleanup()

    def _signal_handler(self, signum, frame):

        
        if self.app:
            self.app.quit()

    def _cleanup(self):

        
        try:
            async def cleanup_async():

                if self.websocket_server:
                    await self.websocket_server.stop()
                    
                if self.time_sync_server:
                    await self.time_sync_server.stop()
                    
                if self.device_manager:
                    await self.device_manager.stop()
                    
            if self._loop and not self._loop.is_closed():
                self._loop.run_until_complete(cleanup_async())

            if self._loop_timer:
                self._loop_timer.stop()

            if self._loop and not self._loop.is_closed():
                self._loop.close()

            
        except Exception as e:
            
    def get_device_manager(self) -> Optional[DeviceManager]:

        return self.device_manager

    def get_session_manager(self) -> Optional[AdvancedSessionManager]:

        return self.session_manager

    def get_main_window(self) -> Optional[MVPMainWindow]:

        return self.main_window


def main() -> int:
        
    log_dir = Path("logs")
    log_dir.mkdir(exist_ok=True)

    
    
    try:

        app = IRCameraHubApplication()

        if not app.initialize():
                        return 1

        return app.run()

    except Exception as e:
                return 1


if __name__ == "__main__":
    sys.exit(main())
