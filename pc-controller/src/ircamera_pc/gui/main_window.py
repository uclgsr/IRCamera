"""
Main Window for IRCamera PC Controller

Provides the main researcher interface with device monitoring and session control.
"""

import asyncio
import time
from datetime import datetime
from typing import Optional

from loguru import logger
from PyQt6.QtCore import Qt, QTimer, pyqtSignal
from PyQt6.QtGui import QFont
from PyQt6.QtWidgets import (
    QGroupBox,
    QHBoxLayout,
    QInputDialog,
    QLabel,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QSplitter,
    QStatusBar,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from ..core.session import SessionManager, SessionState
from ..core.timesync import TimeSyncService
from ..network.websocket_server import WebSocketServer
from .widgets import (
    DeviceListWidget,
    SessionControlWidget,
    StatusDisplayWidget,
    SystemIntegrationWidget,
)


class MainWindow(QMainWindow):
    """
    Main application window for IRCamera PC Controller.

    Implements the GUI requirements from FR6:
    - Device list with status indicators
    - Session start/stop controls
    - Real-time monitoring displays
    - Recording status and elapsed time
    - Device disconnect alerts
    """

    # Custom signals
    session_started = pyqtSignal(str)
    session_stopped = pyqtSignal(str)
    sync_flash_triggered = pyqtSignal()

    def __init__(
        self,
        session_manager: SessionManager,
        websocket_server: WebSocketServer,
        time_sync_service: TimeSyncService,
        gsr_ingestor=None,
        file_transfer_manager=None,
        camera_calibrator=None,
        bluetooth_manager=None,
        wifi_manager=None,
        admin_privileges_manager=None,
    ):
        """
        Initialize main window with all components.

        Args:
            session_manager: Session management service
            network_server: Network server for device communication
            time_sync_service: Time synchronization service
            gsr_ingestor: GSR data ingestor (optional)
            file_transfer_manager: File transfer manager (optional)
            camera_calibrator: Camera calibration service (optional)
            bluetooth_manager: Bluetooth device manager (optional)
            wifi_manager: WiFi network manager (optional)
            admin_privileges_manager: Administrator privileges manager (optional)
        """
        super().__init__()

        # Core services
        self.session_manager = session_manager
        self.websocket_server = websocket_server
        self.time_sync_service = time_sync_service

        # Enhanced components (optional)
        self.gsr_ingestor = gsr_ingestor
        self.file_transfer_manager = file_transfer_manager
        self.camera_calibrator = camera_calibrator
        self.bluetooth_manager = bluetooth_manager
        self.wifi_manager = wifi_manager
        self.admin_privileges_manager = admin_privileges_manager

        # GUI components
        self.device_list_widget: Optional[DeviceListWidget] = None
        self.session_control_widget: Optional[SessionControlWidget] = None
        self.status_display_widget: Optional[StatusDisplayWidget] = None
        self.log_display: Optional[QTextEdit] = None

        # Sync control buttons
        self.sync_flash_btn: Optional[QPushButton] = None
        self.sync_mark_btn: Optional[QPushButton] = None

        # New GUI components for system integration
        self.bluetooth_control_widget = None
        self.wifi_control_widget = None
        self.system_integration_widget = None

        # State tracking
        self._current_session_id: Optional[str] = None
        self._session_start_time: Optional[datetime] = None
        self._update_timer: Optional[QTimer] = None

        self._setup_ui()
        self._setup_connections()
        self._setup_network_callbacks()
        self._setup_system_integration_callbacks()
        self._start_ui_updates()

        logger.info("Main window initialized with system integration features")

    def _setup_ui(self) -> None:
        """Set up the user interface."""
        self.setWindowTitle("IRCamera PC Controller - System Integration")
        self.setMinimumSize(1200, 800)  # Increased size for new features

        # Central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        # Main layout
        main_layout = QHBoxLayout(central_widget)

        # Create splitter for resizable panes
        splitter = QSplitter(Qt.Orientation.Horizontal)
        main_layout.addWidget(splitter)

        # Left pane - Device management and session control
        left_pane = self._create_left_pane()
        splitter.addWidget(left_pane)

        # Center pane - System integration controls
        center_pane = self._create_center_pane()
        splitter.addWidget(center_pane)

        # Right pane - Status and logs
        right_pane = self._create_right_pane()
        splitter.addWidget(right_pane)

        # Set splitter proportions
        splitter.setStretchFactor(0, 1)  # Left pane
        splitter.setStretchFactor(1, 1)  # Center pane (system integration)
        splitter.setStretchFactor(2, 1)  # Right pane

        # Status bar
        self._setup_status_bar()

        # Set initial state - will be updated by timer once UI is fully initialized
        # self._update_ui_state()

    def _create_left_pane(self) -> QWidget:
        """Create the left pane with device management and session controls."""
        pane = QWidget()
        layout = QVBoxLayout(pane)

        # Device management section
        device_group = QGroupBox("Connected Devices")
        device_layout = QVBoxLayout(device_group)

        self.device_list_widget = DeviceListWidget()
        device_layout.addWidget(self.device_list_widget)

        layout.addWidget(device_group)

        # Session control section
        session_group = QGroupBox("Session Control")
        session_layout = QVBoxLayout(session_group)

        self.session_control_widget = SessionControlWidget()
        session_layout.addWidget(self.session_control_widget)

        layout.addWidget(session_group)

        # Sync controls section
        sync_group = QGroupBox("Synchronization")
        sync_layout = QVBoxLayout(sync_group)

        self.sync_flash_btn = QPushButton("Flash Sync")
        self.sync_flash_btn.setToolTip("Send visual sync flash to all devices")
        self.sync_flash_btn.clicked.connect(self._on_sync_flash_clicked)
        sync_layout.addWidget(self.sync_flash_btn)

        self.sync_mark_btn = QPushButton("Add Sync Mark")
        self.sync_mark_btn.setToolTip("Add synchronization marker to timeline")
        self.sync_mark_btn.clicked.connect(self._on_sync_mark_clicked)
        sync_layout.addWidget(self.sync_mark_btn)

        layout.addWidget(sync_group)

        # Stretch at bottom
        layout.addStretch()

        return pane

    def _create_center_pane(self) -> QWidget:
        """Create the center pane with system integration controls."""
        pane = QWidget()
        layout = QVBoxLayout(pane)

        # System integration widget
        if self.admin_privileges_manager:
            self.system_integration_widget = SystemIntegrationWidget()
            layout.addWidget(self.system_integration_widget)

        # Bluetooth control widget
        if self.bluetooth_manager:
            from .widgets import BluetoothControlWidget

            self.bluetooth_control_widget = BluetoothControlWidget()
            layout.addWidget(self.bluetooth_control_widget)

        # WiFi control widget
        if self.wifi_manager:
            from .widgets import WiFiControlWidget

            self.wifi_control_widget = WiFiControlWidget()
            layout.addWidget(self.wifi_control_widget)

        # If no system integration features available, show a message
        if not any(
            [
                self.admin_privileges_manager,
                self.bluetooth_manager,
                self.wifi_manager,
            ]
        ):
            placeholder = QLabel("System integration features not available")
            placeholder.setAlignment(Qt.AlignmentFlag.AlignCenter)
            placeholder.setStyleSheet(
                "color: gray; font-style: italic; font-size: 14px;"
            )
            layout.addWidget(placeholder)

        # Stretch at bottom
        layout.addStretch()

        return pane

    def _create_right_pane(self) -> QWidget:
        """Create the right pane with status displays and logs."""
        pane = QWidget()
        layout = QVBoxLayout(pane)

        # Status display section
        status_group = QGroupBox("System Status")
        status_layout = QVBoxLayout(status_group)

        self.status_display_widget = StatusDisplayWidget()
        status_layout.addWidget(self.status_display_widget)

        layout.addWidget(status_group)

        # Log display section
        log_group = QGroupBox("System Log")
        log_layout = QVBoxLayout(log_group)

        self.log_display = QTextEdit()
        self.log_display.setReadOnly(True)
        # Use setDocument to limit log lines for QTextEdit
        doc = self.log_display.document()
        doc.setMaximumBlockCount(1000)
        self.log_display.setFont(QFont("Consolas", 9))
        log_layout.addWidget(self.log_display)

        layout.addWidget(log_group)

        return pane

    def _setup_status_bar(self) -> None:
        """Set up the status bar."""
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)

        # Status labels
        self.devices_label = QLabel("Devices: 0")
        self.session_label = QLabel("No active session")
        self.sync_label = QLabel("Time sync: OK")

        self.status_bar.addWidget(self.devices_label)
        self.status_bar.addPermanentWidget(self.sync_label)
        self.status_bar.addPermanentWidget(self.session_label)

    def _setup_connections(self) -> None:
        """Set up signal connections."""
        # Session control connections
        if self.session_control_widget:
            self.session_control_widget.start_session_requested.connect(
                self._on_start_session_requested
            )
            self.session_control_widget.stop_session_requested.connect(
                self._on_stop_session_requested
            )
            self.session_control_widget.new_session_requested.connect(
                self._on_new_session_requested
            )

        # Device list connections
        if self.device_list_widget:
            self.device_list_widget.device_selected.connect(self._on_device_selected)

    def _setup_network_callbacks(self) -> None:
        """Set up WebSocket server event callbacks - Phase 1 implementation."""
        # WebSocket server handles callbacks through message handlers internally
        # For now, we'll implement basic device tracking through the server's client management
        logger.info("WebSocket server callbacks configured")

    def _setup_system_integration_callbacks(self) -> None:
        """Set up system integration callbacks and connections."""
        # Bluetooth manager callbacks
        if self.bluetooth_manager and self.bluetooth_control_widget:
            # Connect widget signals to manager methods
            self.bluetooth_control_widget.scan_requested.connect(
                lambda: self.bluetooth_manager.start_scanning(continuous=False)
            )
            self.bluetooth_control_widget.connect_requested.connect(
                lambda addr: asyncio.create_task(
                    self.bluetooth_manager.connect_device(addr)
                )
            )
            self.bluetooth_control_widget.disconnect_requested.connect(
                lambda addr: asyncio.create_task(
                    self.bluetooth_manager.disconnect_device(addr)
                )
            )

            # Connect manager signals to widget updates
            self.bluetooth_manager.device_discovered.connect(
                lambda device: self._update_bluetooth_devices()
            )
            self.bluetooth_manager.device_connected.connect(
                lambda addr, name: self.bluetooth_control_widget.set_connection_status(
                    addr, True
                )
            )
            self.bluetooth_manager.device_disconnected.connect(
                lambda addr,
                    reason: self.bluetooth_control_widget.set_connection_status(
                    addr, False
                )
            )
            self.bluetooth_manager.error_occurred.connect(
                lambda op, err: self.bluetooth_control_widget.set_error_status(err)
            )

        # WiFi manager callbacks
        if self.wifi_manager and self.wifi_control_widget:
            # Connect widget signals to manager methods
            self.wifi_control_widget.scan_requested.connect(
                lambda: self.wifi_manager.start_scanning(continuous=False)
            )
            self.wifi_control_widget.connect_requested.connect(
                lambda ssid, pwd: asyncio.create_task(
                    self.wifi_manager.connect_to_network(ssid, pwd)
                )
            )
            self.wifi_control_widget.disconnect_requested.connect(
                lambda: asyncio.create_task(self.wifi_manager.disconnect_from_network())
            )
            self.wifi_control_widget.hotspot_start_requested.connect(
                lambda ssid, pwd, ch: asyncio.create_task(
                    self.wifi_manager.start_hotspot(ssid, pwd, ch)
                )
            )
            self.wifi_control_widget.hotspot_stop_requested.connect(
                lambda: asyncio.create_task(self.wifi_manager.stop_hotspot())
            )

            # Connect manager signals to widget updates
            self.wifi_manager.networks_discovered.connect(
                lambda networks: self.wifi_control_widget.update_networks(networks)
            )
            self.wifi_manager.network_connected.connect(
                lambda ssid, ip: self.wifi_control_widget.set_connection_status(
                    ssid, True, ip
                )
            )
            self.wifi_manager.network_disconnected.connect(
                lambda ssid, reason: self.wifi_control_widget.set_connection_status(
                    ssid, False
                )
            )
            self.wifi_manager.hotspot_state_changed.connect(
                lambda state, msg: self.wifi_control_widget.set_hotspot_status(
                    state.value, msg
                )
            )
            self.wifi_manager.error_occurred.connect(
                lambda op, err: self.wifi_control_widget.set_error_status(err)
            )

        # Admin privileges manager callbacks
        if self.admin_privileges_manager and self.system_integration_widget:
            # Connect widget signals to manager methods
            self.system_integration_widget.elevation_requested.connect(
                lambda reason: self.admin_privileges_manager.request_elevation(reason)
            )

            # Connect manager signals to widget updates
            self.admin_privileges_manager.privilege_changed.connect(
                lambda level: self.system_integration_widget.update_privilege_level(
                    level.value
                )
            )
            self.admin_privileges_manager.system_ready.connect(
                lambda perms: self.system_integration_widget.update_permissions(
                    {
                        "network_config": perms.network_config,
                        "bluetooth_control": perms.bluetooth_control,
                        "service_management": perms.service_management,
                        "registry_access": perms.registry_access,
                        "hardware_access": perms.hardware_access,
                        "firewall_control": perms.firewall_control,
                    }
                )
            )
            self.admin_privileges_manager.elevation_completed.connect(
                lambda result, msg: self.system_integration_widget.set_status_message(
                    msg, result.value == "failed"
                )
            )

    def _start_ui_updates(self) -> None:
        """Start periodic UI updates."""
        self._update_timer = QTimer()
        self._update_timer.timeout.connect(self._update_displays)
        self._update_timer.start(1000)  # Update every second

    def _update_displays(self) -> None:
        """Update all display components."""
        try:
            # Update device count - WebSocket server clients
            connected_clients = (
                len(self.websocket_server.clients) if self.websocket_server else 0
            )
            self.devices_label.setText(f"Devices: {connected_clients}")

            # Update device list
            if self.device_list_widget:
                self.device_list_widget.update_devices(connected_devices)

            # Update status display
            if self.status_display_widget:
                self.status_display_widget.update_time_sync_stats(
                    self.time_sync_service.get_synchronization_quality()
                )

                current_session = self.session_manager.get_current_session()
                if current_session:
                    self.status_display_widget.update_session_info(current_session)

            # Update session status in status bar
            current_session = self.session_manager.get_current_session()
            if current_session:
                if current_session.state == SessionState.RECORDING.value:
                    if self._session_start_time:
                        elapsed = datetime.now() - self._session_start_time
                        elapsed_str = str(elapsed).split(".")[0]  # Remove microseconds
                        self.session_label.setText(f"Recording: {elapsed_str}")
                    else:
                        self.session_label.setText("Recording: --:--:--")
                else:
                    self.session_label.setText(
                        f"Session: {current_session.name}" "({current_session.state})"
                    )
            else:
                self.session_label.setText("No active session")

            # Update sync status
            sync_quality = self.time_sync_service.get_synchronization_quality()
            if sync_quality["total_devices"] > 0:
                sync_rate = sync_quality["synchronization_rate"] * 100
                if sync_rate >= 90:
                    self.sync_label.setText(f"Time sync: OK ({sync_rate:.0f}%)")
                elif sync_rate >= 70:
                    self.sync_label.setText(f"Time sync: WARNING ({sync_rate:.0f}%)")
                else:
                    self.sync_label.setText(f"Time sync: ERROR ({sync_rate:.0f}%)")
            else:
                self.sync_label.setText("Time sync: No devices")

            # Update UI state
            self._update_ui_state()

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error updating displays: {e}")

    def _update_ui_state(self) -> None:
        """Update UI component enabled/disabled"
        "state based on current state."""
        current_session = self.session_manager.get_current_session()
        has_devices = (
            len(self.websocket_server.clients) > 0 if self.websocket_server else False
        )

        # Update session control state
        if self.session_control_widget:
            self.session_control_widget.update_state(current_session, has_devices)

        # Update sync controls
        can_sync = (
            current_session
            and current_session.state
            in [SessionState.ACTIVE.value, SessionState.RECORDING.value]
            and has_devices
        )

        # Only update buttons if they exist and are not None
        if hasattr(self, "sync_flash_btn") and self.sync_flash_btn is not None:
            self.sync_flash_btn.setEnabled(can_sync)
        if hasattr(self, "sync_mark_btn") and self.sync_mark_btn is not None:
            self.sync_mark_btn.setEnabled(can_sync)

    # Event handlers
    def _on_start_session_requested(self) -> None:
        """Handle session start request."""
        try:
            current_session = self.session_manager.get_current_session()
            if not current_session:
                self._show_error(
                    "No session created", "Please create a new session first."
                )
                return

            if current_session.state != SessionState.IDLE.value:
                self._show_error(
                    "Cannot start session",
                    f"Session is in {current_session.state} state.",
                )
                return

            # Start the session
            self.session_manager.start_session()
            self.session_manager.begin_recording()
            self._session_start_time = datetime.now()

            # Send start command to all devices
            # Send session start command to all connected clients via WebSocket
            if self.websocket_server:
                import asyncio

                from ..network.protocol import create_message

                session_message = create_message(
                    "session_start",
                    {
                        "session_id": current_session.session_id,
                        "timestamp": time.time(),
                    },
                )

                asyncio.create_task(
                    self.websocket_server._broadcast_message(session_message)
                )

            self._current_session_id = current_session.session_id
            self.session_started.emit(current_session.session_id)

            logger.info(f"Session started: {current_session.name}")
            self._add_log_message(f"Session started: {current_session.name}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error starting session: {e}")
            self._show_error("Error", f"Failed to start session: {e}")

    def _on_stop_session_requested(self) -> None:
        """Handle session stop request."""
        try:
            current_session = self.session_manager.get_current_session()
            if not current_session:
                return

            # Send stop command to all devices
            # Send session stop command to all connected clients via WebSocket
            if self.websocket_server:
                import asyncio

                from ..network.protocol import create_message

                session_message = create_message(
                    "session_stop",
                    {
                        "session_id": current_session.session_id,
                        "timestamp": time.time(),
                    },
                )

                asyncio.create_task(
                    self.websocket_server._broadcast_message(session_message)
                )

            # End the session
            ended_session = self.session_manager.end_session()
            self._session_start_time = None

            self.session_stopped.emit(ended_session.session_id)

            logger.info(f"Session stopped: {ended_session.name}")
            self._add_log_message(
                f"Session stopped: {ended_session.name} "
                f"(duration: {ended_session.duration_seconds:.1f}s)"
            )

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error stopping session: {e}")
            self._show_error("Error", f"Failed to stop session: {e}")

    def _on_new_session_requested(self) -> None:
        """Handle new session creation request."""
        try:
            # Get session name from user
            name, ok = QInputDialog.getText(
                self,
                "New Session",
                "Enter session name (leave empty for auto-generated):",
            )

            if not ok:
                return

            session_name = name.strip() if name.strip() else None

            # Create new session
            session = self.session_manager.create_session(session_name)

            logger.info(f"New session created: {session.name}")
            self._add_log_message(f"New session created: {session.name}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error creating session: {e}")
            self._show_error("Error", f"Failed to create session: {e}")

    def _on_sync_flash_clicked(self) -> None:
        """Handle sync flash button click."""
        try:
            # Send sync flash to all connected clients via WebSocket
            if self.websocket_server:
                import asyncio

                from ..network.protocol import create_message

                sync_message = create_message(
                    "sync_flash_trigger", {"duration_ms": 500, "timestamp": time.time()}
                )

                asyncio.create_task(
                    self.websocket_server._broadcast_message(sync_message)
                )

                # Add sync event to session
                current_session = self.session_manager.get_current_session()
                if current_session:
                    self.session_manager.add_sync_event("flash")

                self.sync_flash_triggered.emit()

                logger.info("Sync flash sent to all devices")
                self._add_log_message("Sync flash sent to all devices")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error sending sync flash: {e}")
            self._show_error("Error", f"Failed to send sync flash: {e}")

    def _on_sync_mark_clicked(self) -> None:
        """Handle sync mark button click."""
        try:
            # Get mark description from user
            description, ok = QInputDialog.getText(
                self, "Sync Mark", "Enter sync mark description:"
            )

            if not ok or not description.strip():
                return

            # Send sync mark to all connected clients via WebSocket
            if self.websocket_server:
                import asyncio

                from ..network.protocol import create_message

                mark_message = create_message(
                    "sync_mark",
                    {
                        "mark_type": "manual_mark",
                        "description": description,
                        "timestamp": time.time(),
                    },
                )

                asyncio.create_task(
                    self.websocket_server._broadcast_message(mark_message)
                )

                # Add sync event to session
                current_session = self.session_manager.get_current_session()
                if current_session:
                    self.session_manager.add_sync_event(
                        "manual_mark", {"description": description}
                    )

                logger.info(f"Sync mark added: {description}")
                self._add_log_message(f"Sync mark added: {description}")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error adding sync mark: {e}")
            self._show_error("Error", f"Failed to add sync mark: {e}")

    def _on_device_selected(self, device_id: str) -> None:
        """Handle device selection in list - WebSocket client."""
        if self.websocket_server and device_id in self.websocket_server.clients:
            client = self.websocket_server.clients[device_id]
            logger.debug(
                f"WebSocket client selected: {device_id} ({client.device_type})"
            )

    # Network event handlers
    def _on_device_connected(self, device_info: DeviceInfo) -> None:
        """Handle device connection."""
        logger.info(f"Device connected: {device_info.device_id}")
        self._add_log_message(
            f"Device connected: {device_info.device_id}"
            ""
            f"({device_info.device_type})"
        )

        # Add device to current session if active
        current_session = self.session_manager.get_current_session()
        if current_session:
            self.session_manager.add_device(device_info.to_dict())

    def _on_device_disconnected(self, device_info: DeviceInfo) -> None:
        """Handle device disconnection."""
        logger.warning(f"Device disconnected: {device_info.device_id}")
        self._add_log_message(f"Device disconnected: {device_info.device_id}")

        # Show alert for important devices
        if device_info.is_gsr_leader:
            self._show_warning(
                "GSR Leader Disconnected",
                f"GSR leader device {device_info.device_id} has disconnected. "
                "A new leader will be elected if available.",
            )

    def _on_device_status_updated(self, device_info: DeviceInfo) -> None:
        """Handle device status update."""
        logger.debug(f"Device status updated: {device_info.device_id}")

    # Utility methods
    def _add_log_message(self, message: str) -> None:
        """Add message to log display."""
        if self.log_display:
            timestamp = datetime.now().strftime("%H:%M:%S")
            formatted_message = f"[{timestamp}] {message}"
            self.log_display.append(formatted_message)

    def _show_error(self, title: str, message: str) -> None:
        """Show error message box."""
        QMessageBox.critical(self, title, message)

    def _show_warning(self, title: str, message: str) -> None:
        """Show warning message box."""
        QMessageBox.warning(self, title, message)

    def _show_info(self, title: str, message: str) -> None:
        """Show information message box."""
        QMessageBox.information(self, title, message)

    def _update_bluetooth_devices(self) -> None:
        """Update Bluetooth device list in the UI."""
        if self.bluetooth_manager and self.bluetooth_control_widget:
            devices = self.bluetooth_manager.discovered_devices
            self.bluetooth_control_widget.update_devices(devices)

    def _update_wifi_networks(self) -> None:
        """Update WiFi network list in the UI."""
        if self.wifi_manager and self.wifi_control_widget:
            networks = self.wifi_manager.available_networks
            self.wifi_control_widget.update_networks(networks)

    def closeEvent(self, event) -> None:
        """Handle window close event."""
        # Stop any active session
        current_session = self.session_manager.get_current_session()
        if current_session and current_session.state in [
            SessionState.ACTIVE.value,
            SessionState.RECORDING.value,
        ]:
            reply = QMessageBox.question(
                self,
                "Active Session",
                "A recording session is active. Stop it before closing?",
                QMessageBox.Yes | QMessageBox.No | QMessageBox.Cancel,
            )

            if reply == QMessageBox.Cancel:
                event.ignore()
                return
            elif reply == QMessageBox.Yes:
                self._on_stop_session_requested()

        # Stop UI updates
        if self._update_timer:
            self._update_timer.stop()

        event.accept()
