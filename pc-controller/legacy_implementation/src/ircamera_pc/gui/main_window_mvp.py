

import asyncio
from datetime import datetime
from typing import Dict, Optional

from loguru import logger
from PyQt6.QtCore import Qt, QTimer, pyqtSignal, pyqtSlot
from PyQt6.QtGui import QFont
from PyQt6.QtWidgets import (
    QComboBox,
    QGroupBox,
    QHBoxLayout,
    QHeaderView,
    QInputDialog,
    QLabel,
    QLineEdit,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QStatusBar,
    QTabWidget,
    QTableWidget,
    QTableWidgetItem,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from ..core.device_manager import DeviceConnectionState, DeviceInfo, DeviceManager, DeviceType
from ..core.session_manager import AdvancedSessionManager, SessionConfiguration, SessionState


class DeviceDashboardWidget(QWidget):

    device_connect_requested = pyqtSignal(str)
    device_disconnect_requested = pyqtSignal(str)
    device_refresh_requested = pyqtSignal()
    manual_add_requested = pyqtSignal()

    def __init__(self):

        super().__init__()
        self.device_manager: Optional[DeviceManager] = None
        self._setup_ui()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_device_list)
        self.update_timer.start(2000)

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        header_layout = QHBoxLayout()

        title_label = QLabel("Device Dashboard")
        title_label.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        header_layout.addWidget(title_label)

        header_layout.addStretch()

        self.refresh_btn = QPushButton("Refresh")
        self.refresh_btn.clicked.connect(self.device_refresh_requested.emit)
        header_layout.addWidget(self.refresh_btn)

        self.add_manual_btn = QPushButton("Add Device")
        self.add_manual_btn.clicked.connect(self.manual_add_requested.emit)
        header_layout.addWidget(self.add_manual_btn)

        layout.addLayout(header_layout)

        self.device_table = QTableWidget()
        self.device_table.setColumnCount(6)
        self.device_table.setHorizontalHeaderLabels([
            "Device Name", "Type", "IP Address", "Status", "Capabilities", "Actions"
        ])

        header = self.device_table.horizontalHeader()
        header.setSectionResizeMode(0, QHeaderView.ResizeMode.ResizeToContents)
        header.setSectionResizeMode(1, QHeaderView.ResizeMode.ResizeToContents)
        header.setSectionResizeMode(2, QHeaderView.ResizeMode.ResizeToContents)
        header.setSectionResizeMode(3, QHeaderView.ResizeMode.ResizeToContents)
        header.setSectionResizeMode(4, QHeaderView.ResizeMode.Stretch)
        header.setSectionResizeMode(5, QHeaderView.ResizeMode.ResizeToContents)

        self.device_table.setAlternatingRowColors(True)
        self.device_table.setSelectionBehavior(QTableWidget.SelectionBehavior.SelectRows)

        layout.addWidget(self.device_table)

        self.status_label = QLabel("No devices discovered")
        self.status_label.setStyleSheet("color: gray; padding: 5px;")
        layout.addWidget(self.status_label)

    def set_device_manager(self, device_manager: DeviceManager):

        self.device_manager = device_manager

        self.device_manager.add_status_callback(self._on_device_status_changed)

        self._update_device_list()

    def _on_device_status_changed(self, device_id: str, device_info: DeviceInfo, event_type: str):

        logger.debug(f"Device status changed: {device_id} - {event_type}")

    @pyqtSlot()
    def _update_device_list(self):

        if not self.device_manager:
            return

        try:

            devices = self.device_manager.get_registry().get_all_devices()

            self.device_table.setRowCount(len(devices))

            for row, (device_id, device) in enumerate(devices.items()):

                name_item = QTableWidgetItem(device.device_name)
                self.device_table.setItem(row, 0, name_item)

                type_item = QTableWidgetItem(device.device_type.name)
                self.device_table.setItem(row, 1, type_item)

                ip_item = QTableWidgetItem(f"{device.ip_address}:{device.port}")
                self.device_table.setItem(row, 2, ip_item)

                status_item = QTableWidgetItem(device.state.value.title())
                if device.state == DeviceConnectionState.ONLINE:
                    status_item.setBackground(Qt.GlobalColor.green)
                elif device.state == DeviceConnectionState.RECORDING:
                    status_item.setBackground(Qt.GlobalColor.blue)
                elif device.state == DeviceConnectionState.ERROR:
                    status_item.setBackground(Qt.GlobalColor.red)
                elif device.state == DeviceConnectionState.DISCONNECTED:
                    status_item.setBackground(Qt.GlobalColor.lightGray)
                else:
                    status_item.setBackground(Qt.GlobalColor.yellow)

                self.device_table.setItem(row, 3, status_item)

                caps = []
                if device.capabilities.supports_rgb_camera:
                    caps.append("RGB")
                if device.capabilities.supports_thermal_camera:
                    caps.append("Thermal")
                if device.capabilities.supports_gsr_sensor:
                    caps.append("GSR")

                caps_item = QTableWidgetItem(", ".join(caps))
                self.device_table.setItem(row, 4, caps_item)

                actions_widget = QWidget()
                actions_layout = QHBoxLayout(actions_widget)
                actions_layout.setContentsMargins(2, 2, 2, 2)

                if device.state == DeviceConnectionState.DISCOVERED:
                    connect_btn = QPushButton("Connect")
                    connect_btn.clicked.connect(
                        lambda checked, d_id=device_id: self.device_connect_requested.emit(d_id))
                    actions_layout.addWidget(connect_btn)
                elif device.state == DeviceConnectionState.ONLINE:
                    disconnect_btn = QPushButton("Disconnect")
                    disconnect_btn.clicked.connect(
                        lambda checked, d_id=device_id: self.device_disconnect_requested.emit(d_id))
                    actions_layout.addWidget(disconnect_btn)

                actions_layout.addStretch()
                self.device_table.setCellWidget(row, 5, actions_widget)

            self._update_status_summary(devices)

        except Exception as e:
            logger.error(f"Error updating device list: {e}")

    def _update_status_summary(self, devices: Dict[str, DeviceInfo]):

        if not devices:
            self.status_label.setText("No devices discovered")
            return

        state_counts = {}
        for device in devices.values():
            state = device.state.value
            state_counts[state] = state_counts.get(state, 0) + 1

        status_parts = []
        total = len(devices)
        status_parts.append(f"Total: {total}")

        for state, count in state_counts.items():
            status_parts.append(f"{state.title()}: {count}")

        self.status_label.setText(" | ".join(status_parts))


class SessionControlWidget(QWidget):

    create_session_requested = pyqtSignal(str, dict)
    start_recording_requested = pyqtSignal()
    stop_recording_requested = pyqtSignal()
    finalize_session_requested = pyqtSignal()
    reset_session_requested = pyqtSignal()

    def __init__(self):

        super().__init__()
        self.session_manager: Optional[AdvancedSessionManager] = None
        self._setup_ui()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_session_display)
        self.update_timer.start(1000)

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        title_label = QLabel("Session Control Panel")
        title_label.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        layout.addWidget(title_label)

        session_group = QGroupBox("Current Session")
        session_layout = QVBoxLayout(session_group)

        self.session_info_label = QLabel("No active session")
        self.session_info_label.setFont(QFont("Arial", 10))
        session_layout.addWidget(self.session_info_label)

        self.session_status_label = QLabel("Status: IDLE")
        self.session_status_label.setFont(QFont("Arial", 10, QFont.Weight.Bold))
        session_layout.addWidget(self.session_status_label)

        self.recording_timer_label = QLabel("Duration: --:--:--")
        session_layout.addWidget(self.recording_timer_label)

        layout.addWidget(session_group)

        controls_group = QGroupBox("Controls")
        controls_layout = QVBoxLayout(controls_group)

        session_btn_layout = QHBoxLayout()

        self.create_session_btn = QPushButton("Create Session")
        self.create_session_btn.clicked.connect(self._create_session_clicked)
        session_btn_layout.addWidget(self.create_session_btn)

        self.finalize_session_btn = QPushButton("Finalize Session")
        self.finalize_session_btn.clicked.connect(self.finalize_session_requested.emit)
        self.finalize_session_btn.setEnabled(False)
        session_btn_layout.addWidget(self.finalize_session_btn)

        self.reset_session_btn = QPushButton("Reset")
        self.reset_session_btn.clicked.connect(self.reset_session_requested.emit)
        session_btn_layout.addWidget(self.reset_session_btn)

        controls_layout.addLayout(session_btn_layout)

        recording_btn_layout = QHBoxLayout()

        self.start_recording_btn = QPushButton("Start Recording")
        self.start_recording_btn.clicked.connect(self.start_recording_requested.emit)
        self.start_recording_btn.setEnabled(False)
        self.start_recording_btn.setStyleSheet(
            "QPushButton { background-color: green; color: white; font-weight: bold; }")
        recording_btn_layout.addWidget(self.start_recording_btn)

        self.stop_recording_btn = QPushButton("Stop Recording")
        self.stop_recording_btn.clicked.connect(self.stop_recording_requested.emit)
        self.stop_recording_btn.setEnabled(False)
        self.stop_recording_btn.setStyleSheet(
            "QPushButton { background-color: red; color: white; font-weight: bold; }")
        recording_btn_layout.addWidget(self.stop_recording_btn)

        controls_layout.addLayout(recording_btn_layout)

        layout.addWidget(controls_group)

        participation_group = QGroupBox("Device Participation")
        participation_layout = QVBoxLayout(participation_group)

        self.participation_label = QLabel("No devices selected")
        participation_layout.addWidget(self.participation_label)

        layout.addWidget(participation_group)

        layout.addStretch()

    def set_session_manager(self, session_manager: AdvancedSessionManager):

        self.session_manager = session_manager

        self.session_manager.add_state_callback(self._on_session_state_changed)

        self._update_session_display()

    def _create_session_clicked(self):

        session_name, ok = QInputDialog.getText(
            self,
            "Create Session",
            "Enter session name:"
        )

        if ok and session_name.strip():

            config = {
                "modalities": ["rgb", "thermal", "gsr"],
                "auto_start": False
            }
            self.create_session_requested.emit(session_name.strip(), config)

    def _on_session_state_changed(self, state: SessionState, session):

        logger.debug(f"Session state changed to: {state.value}")

    @pyqtSlot()
    def _update_session_display(self):

        if not self.session_manager:
            return

        try:
            session = self.session_manager.get_current_session()

            if session:

                self.session_info_label.setText(
                    f"Session: {session.session_name} ({session.session_id})")

                status_text = f"Status: {session.state.value.upper()}"
                if session.state == SessionState.RECORDING:
                    self.session_status_label.setStyleSheet("color: red; font-weight: bold;")
                elif session.state == SessionState.ACTIVE:
                    self.session_status_label.setStyleSheet("color: green; font-weight: bold;")
                elif session.state == SessionState.ERROR:
                    self.session_status_label.setStyleSheet("color: orange; font-weight: bold;")
                else:
                    self.session_status_label.setStyleSheet("color: blue; font-weight: bold;")

                self.session_status_label.setText(status_text)

                if session.started_at and session.state == SessionState.RECORDING:
                    duration = (datetime.now() - session.started_at.replace(
                        tzinfo=None)).total_seconds()
                    hours = int(duration // 3600)
                    minutes = int((duration % 3600) // 60)
                    seconds = int(duration % 60)
                    self.recording_timer_label.setText(
                        f"Duration: {hours:02d}:{minutes:02d}:{seconds:02d}")
                elif session.duration_seconds:
                    duration = session.duration_seconds
                    hours = int(duration // 3600)
                    minutes = int((duration % 3600) // 60)
                    seconds = int(duration % 60)
                    self.recording_timer_label.setText(
                        f"Final Duration: {hours:02d}:{minutes:02d}:{seconds:02d}")
                else:
                    self.recording_timer_label.setText("Duration: --:--:--")

                if session.participating_devices:
                    device_count = len(session.participating_devices)
                    self.participation_label.setText(f"{device_count} devices participating")
                else:
                    self.participation_label.setText("No devices selected")

                self._update_button_states(session.state)

            else:

                self.session_info_label.setText("No active session")
                self.session_status_label.setText("Status: IDLE")
                self.session_status_label.setStyleSheet("color: gray;")
                self.recording_timer_label.setText("Duration: --:--:--")
                self.participation_label.setText("No devices selected")

                self._update_button_states(SessionState.IDLE)

        except Exception as e:
            logger.error(f"Error updating session display: {e}")

    def _update_button_states(self, session_state: SessionState):

        self.create_session_btn.setEnabled(session_state == SessionState.IDLE)

        self.start_recording_btn.setEnabled(session_state == SessionState.ACTIVE)
        self.stop_recording_btn.setEnabled(session_state == SessionState.RECORDING)

        self.finalize_session_btn.setEnabled(
            session_state in [SessionState.STOPPED, SessionState.ERROR])
        self.reset_session_btn.setEnabled(session_state != SessionState.RECORDING)


class LoggingConsoleWidget(QWidget):

    def __init__(self):

        super().__init__()
        self.max_lines = 1000
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        header_layout = QHBoxLayout()

        title_label = QLabel("System Log")
        title_label.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        header_layout.addWidget(title_label)

        header_layout.addStretch()

        clear_btn = QPushButton("Clear")
        clear_btn.clicked.connect(self._clear_log)
        header_layout.addWidget(clear_btn)

        layout.addLayout(header_layout)

        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        self.log_text.setFont(QFont("Consolas", 9))
        self.log_text.setMaximumBlockCount(self.max_lines)

        layout.addWidget(self.log_text)

    def add_log_message(self, message: str, level: str = "INFO"):

        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        formatted_message = f"[{timestamp}] {level}: {message}"

        if level == "ERROR":
            color = "red"
        elif level == "WARNING":
            color = "orange"
        elif level == "DEBUG":
            color = "gray"
        else:
            color = "black"

        self.log_text.append(f'<span style="color: {color}">{formatted_message}</span>')

        cursor = self.log_text.textCursor()
        cursor.movePosition(cursor.MoveOperation.End)
        self.log_text.setTextCursor(cursor)

    def _clear_log(self):

        self.log_text.clear()


class MVPMainWindow(QMainWindow):

    def __init__(self, device_manager: DeviceManager, session_manager: AdvancedSessionManager):

        super().__init__()

        self.device_manager = device_manager
        self.session_manager = session_manager

        self._setup_ui()
        self._setup_connections()
        self._setup_status_bar()

        self.status_timer = QTimer()
        self.status_timer.timeout.connect(self._update_status)
        self.status_timer.start(1000)

    def _setup_ui(self):

        self.setWindowTitle("IRCamera PC Controller Hub - MVP")
        self.setMinimumSize(1200, 800)

        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        layout = QVBoxLayout(central_widget)

        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)

        self._create_dashboard_tab()

        self._create_logs_tab()

        self._create_device_management_tab()

    def _create_dashboard_tab(self):

        dashboard_widget = QWidget()
        layout = QHBoxLayout(dashboard_widget)

        left_widget = QWidget()
        left_layout = QVBoxLayout(left_widget)

        self.device_dashboard = DeviceDashboardWidget()
        self.device_dashboard.set_device_manager(self.device_manager)
        left_layout.addWidget(self.device_dashboard)

        layout.addWidget(left_widget, 2)

        right_widget = QWidget()
        right_layout = QVBoxLayout(right_widget)

        self.session_control = SessionControlWidget()
        self.session_control.set_session_manager(self.session_manager)
        right_layout.addWidget(self.session_control)

        status_group = QGroupBox("System Status")
        status_layout = QVBoxLayout(status_group)

        self.network_status_label = QLabel("Network: Initializing...")
        self.discovery_status_label = QLabel("Discovery: Initializing...")
        self.time_sync_status_label = QLabel("Time Sync: Not Available")

        status_layout.addWidget(self.network_status_label)
        status_layout.addWidget(self.discovery_status_label)
        status_layout.addWidget(self.time_sync_status_label)

        right_layout.addWidget(status_group)
        right_layout.addStretch()

        layout.addWidget(right_widget, 1)

        self.tab_widget.addTab(dashboard_widget, "Dashboard")

    def _create_logs_tab(self):

        self.logging_console = LoggingConsoleWidget()
        self.tab_widget.addTab(self.logging_console, "Logs")

    def _create_device_management_tab(self):

        device_mgmt_widget = QWidget()
        layout = QVBoxLayout(device_mgmt_widget)

        manual_group = QGroupBox("Manual Device Management")
        manual_layout = QVBoxLayout(manual_group)

        add_device_layout = QHBoxLayout()

        from PyQt6.QtWidgets import QLineEdit, QComboBox

        add_device_layout.addWidget(QLabel("IP Address:"))
        self.manual_ip_input = QLineEdit()
        self.manual_ip_input.setPlaceholderText("e.g., 192.168.1.100")
        add_device_layout.addWidget(self.manual_ip_input)

        add_device_layout.addWidget(QLabel("Port:"))
        self.manual_port_input = QLineEdit()
        self.manual_port_input.setPlaceholderText("8080")
        self.manual_port_input.setText("8080")
        add_device_layout.addWidget(self.manual_port_input)

        add_device_layout.addWidget(QLabel("Type:"))
        self.manual_type_combo = QComboBox()
        self.manual_type_combo.addItems(
            ["ANDROID_SENSOR_NODE"]) # TS004/TC007 device support removed
        add_device_layout.addWidget(self.manual_type_combo)

        self.add_manual_device_btn = QPushButton("Add Device")
        self.add_manual_device_btn.clicked.connect(self._on_add_manual_device)
        add_device_layout.addWidget(self.add_manual_device_btn)

        manual_layout.addLayout(add_device_layout)
        layout.addWidget(manual_group)

        details_group = QGroupBox("Device Details")
        details_layout = QVBoxLayout(details_group)

        details_info_label = QLabel(
            "Detailed device information and diagnostics will be shown here.")
        details_layout.addWidget(details_info_label)

        layout.addWidget(details_group)

        layout.addStretch()

        self.tab_widget.addTab(device_mgmt_widget, "Device Management")

    def _setup_connections(self):

        self.device_dashboard.device_connect_requested.connect(self._connect_device)
        self.device_dashboard.device_disconnect_requested.connect(self._disconnect_device)
        self.device_dashboard.device_refresh_requested.connect(self._refresh_devices)
        self.device_dashboard.manual_add_requested.connect(self._add_device_manually)

        self.session_control.create_session_requested.connect(self._create_session)
        self.session_control.start_recording_requested.connect(self._start_recording)
        self.session_control.stop_recording_requested.connect(self._stop_recording)
        self.session_control.finalize_session_requested.connect(self._finalize_session)
        self.session_control.reset_session_requested.connect(self._reset_session)

    def _setup_status_bar(self):

        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)

        self.device_count_label = QLabel("Devices: 0")
        self.status_bar.addPermanentWidget(self.device_count_label)

        self.session_status_label = QLabel("Session: IDLE")
        self.status_bar.addPermanentWidget(self.session_status_label)

        self.status_bar.showMessage("IRCamera PC Controller Hub initialized")

    @pyqtSlot(str)
    def _connect_device(self, device_id: str):

        try:

            registry = self.device_manager.get_registry()
            device_info = registry.get_device(device_id)

            if not device_info:
                self.logging_console.add_log_message(f"Device {device_id} not found in registry",
                                                     "ERROR")
                return

            async def connect_async():
                success = await self.device_manager.connect_to_device(device_id)
                if success:
                    self.logging_console.add_log_message(f"Connected to device: {device_id}")
                    self.status_bar.showMessage(f"Device {device_id} connected", 3000)
                    self._update_device_display()
                else:
                    self.logging_console.add_log_message(
                        f"Failed to connect to device: {device_id}", "ERROR")

            asyncio.ensure_future(connect_async())

        except Exception as e:
            self.logging_console.add_log_message(f"Error connecting device {device_id}: {e}",
                                                 "ERROR")
            logger.error(f"Error connecting device: {e}")

    @pyqtSlot(str)
    def _disconnect_device(self, device_id: str):

        try:
            registry = self.device_manager.get_registry()
            success = registry.update_device_state(device_id, DeviceConnectionState.DISCONNECTED)

            if success:
                self.logging_console.add_log_message(f"Disconnected device: {device_id}")
                self.status_bar.showMessage(f"Device {device_id} disconnected", 3000)

        except Exception as e:
            self.logging_console.add_log_message(f"Error disconnecting device {device_id}: {e}",
                                                 "ERROR")
            logger.error(f"Error disconnecting device: {e}")

    @pyqtSlot()
    def _refresh_devices(self):

        try:

            asyncio.create_task(self.device_manager.refresh_discovery())
            self.logging_console.add_log_message("Device discovery refreshed")
            self.status_bar.showMessage("Refreshing device discovery...", 2000)

        except Exception as e:
            self.logging_console.add_log_message(f"Error refreshing devices: {e}", "ERROR")
            logger.error(f"Error refreshing devices: {e}")

    @pyqtSlot()
    def _add_device_manually(self):

        self._show_manual_device_dialog()

    @pyqtSlot()
    def _on_add_manual_device(self):

        try:

            ip_address = self.manual_ip_input.text().strip()
            port_text = self.manual_port_input.text().strip()
            device_type = self.manual_type_combo.currentText()

            if not ip_address:
                QMessageBox.warning(self, "Invalid Input", "Please enter an IP address.")
                return

            try:
                port = int(port_text) if port_text else 8080
                if not (1 <= port <= 65535):
                    raise ValueError("Port out of range")
            except ValueError:
                QMessageBox.warning(self, "Invalid Input",
                                    "Please enter a valid port number (1-65535).")
                return

            device_id = f"manual_{ip_address}_{port}"

            device_type_mapping = {
                "ANDROID_SENSOR_NODE": DeviceType.ANDROID_SENSOR_NODE,
                # TS004/TC007 device support removed
            }
            device_type_enum = device_type_mapping.get(device_type, DeviceType.ANDROID_SENSOR_NODE)

            from datetime import datetime
            from ..network.discovery import DiscoveredDevice

            discovered_device = DiscoveredDevice(
                service_name=f"manual_{ip_address}_{port}",
                service_type="_ircamera._tcp.local.",
                ip_address=ip_address,
                port=port,
                device_type=device_type_enum,
                attributes={"manual": "true"},
                discovered_at=datetime.now(),
                last_seen=datetime.now()
            )

            registry = self.device_manager.get_registry()
            device_id = registry.register_device(discovered_device)

            self.manual_ip_input.clear()
            self.manual_port_input.setText("8080")

            self._update_device_display()
            self.logging_console.add_log_message(f"Manually added device: {device_id}")
            self.status_bar.showMessage(f"Device {device_id} added manually", 3000)

        except Exception as e:
            self.logging_console.add_log_message(f"Error adding manual device: {e}", "ERROR")
            logger.error(f"Error adding manual device: {e}")
            QMessageBox.critical(self, "Error", f"Failed to add device: {e}")

    def _show_manual_device_dialog(self):

        from PyQt6.QtWidgets import QDialog, QFormLayout, QDialogButtonBox

        dialog = QDialog(self)
        dialog.setWindowTitle("Add Device Manually")
        dialog.setModal(True)

        layout = QFormLayout(dialog)

        ip_input = QLineEdit()
        ip_input.setPlaceholderText("192.168.1.100")
        layout.addRow("IP Address:", ip_input)

        port_input = QLineEdit()
        port_input.setText("8080")
        layout.addRow("Port:", port_input)

        name_input = QLineEdit()
        name_input.setPlaceholderText("Optional device name")
        layout.addRow("Device Name:", name_input)

        type_combo = QComboBox()
        type_combo.addItems(["ANDROID_SENSOR_NODE"]) # TS004/TC007 device support removed
        layout.addRow("Device Type:", type_combo)

        buttons = QDialogButtonBox(
            QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        buttons.accepted.connect(dialog.accept)
        buttons.rejected.connect(dialog.reject)
        layout.addWidget(buttons)

        if dialog.exec() == QDialog.DialogCode.Accepted:
            try:
                ip = ip_input.text().strip()
                port = int(port_input.text().strip()) if port_input.text().strip() else 8080
                name = name_input.text().strip()
                device_type = type_combo.currentText()

                if not ip:
                    QMessageBox.warning(self, "Invalid Input", "Please enter an IP address.")
                    return

                device_id = f"manual_{ip}_{port}"
                if name:
                    device_id = f"manual_{name}_{ip}_{port}"

                device_type_mapping = {
                    "ANDROID_SENSOR_NODE": DeviceType.ANDROID_SENSOR_NODE,
                    # TS004/TC007 device support removed
                }
                device_type_enum = device_type_mapping.get(device_type,
                                                           DeviceType.ANDROID_SENSOR_NODE)

                from datetime import datetime
                from ..network.discovery import DiscoveredDevice

                discovered_device = DiscoveredDevice(
                    service_name=name if name else f"manual_{ip}_{port}",
                    service_type="_ircamera._tcp.local.",
                    ip_address=ip,
                    port=port,
                    device_type=device_type_enum,
                    attributes={"manual": "true", "name": name} if name else {"manual": "true"},
                    discovered_at=datetime.now(),
                    last_seen=datetime.now()
                )

                registry = self.device_manager.get_registry()
                device_id = registry.register_device(discovered_device)

                self._update_device_display()
                self.logging_console.add_log_message(f"Manually added device: {device_id}")

            except Exception as e:
                QMessageBox.critical(self, "Error", f"Failed to add device: {e}")

    @pyqtSlot(str, dict)
    def _create_session(self, session_name: str, config: dict):

        try:
            session_config = SessionConfiguration(
                session_name=session_name,
                modalities=config.get("modalities", ["rgb", "thermal", "gsr"]),
                auto_start_recording=config.get("auto_start", False)
            )

            session_id = self.session_manager.create_session(session_name, session_config)

            self.logging_console.add_log_message(f"Created session: {session_name} ({session_id})")
            self.status_bar.showMessage(f"Session created: {session_name}", 3000)

        except Exception as e:
            self.logging_console.add_log_message(f"Error creating session: {e}", "ERROR")
            QMessageBox.critical(self, "Session Creation Error", str(e))
            logger.error(f"Error creating session: {e}")

    @pyqtSlot()
    def _start_recording(self):

        try:

            online_devices = self.device_manager.get_registry().get_online_devices()
            if not online_devices:
                QMessageBox.warning(
                    self,
                    "No Devices Available",
                    "No online devices available for recording. Please connect devices first."
                )
                return

            asyncio.create_task(self._async_start_recording())

        except Exception as e:
            self.logging_console.add_log_message(f"Error starting recording: {e}", "ERROR")
            QMessageBox.critical(self, "Recording Start Error", str(e))
            logger.error(f"Error starting recording: {e}")

    async def _async_start_recording(self):

        try:
            success = await self.session_manager.start_recording()

            if success:
                self.logging_console.add_log_message("Recording started successfully")
                self.status_bar.showMessage("Recording started", 3000)
            else:
                self.logging_console.add_log_message("Failed to start recording on some devices",
                                                     "WARNING")
                self.status_bar.showMessage("Recording started with partial failures", 5000)

        except Exception as e:
            self.logging_console.add_log_message(f"Recording start failed: {e}", "ERROR")
            logger.error(f"Recording start failed: {e}")

    @pyqtSlot()
    def _stop_recording(self):

        try:
            asyncio.create_task(self._async_stop_recording())

        except Exception as e:
            self.logging_console.add_log_message(f"Error stopping recording: {e}", "ERROR")
            logger.error(f"Error stopping recording: {e}")

    async def _async_stop_recording(self):

        try:
            success = await self.session_manager.stop_recording()

            if success:
                self.logging_console.add_log_message("Recording stopped successfully")
                self.status_bar.showMessage("Recording stopped", 3000)
            else:
                self.logging_console.add_log_message("Failed to stop recording on some devices",
                                                     "WARNING")

        except Exception as e:
            self.logging_console.add_log_message(f"Recording stop failed: {e}", "ERROR")
            logger.error(f"Recording stop failed: {e}")

    @pyqtSlot()
    def _finalize_session(self):

        try:
            success = self.session_manager.finalize_session()

            if success:
                self.logging_console.add_log_message("Session finalized successfully")
                self.status_bar.showMessage("Session finalized", 3000)
            else:
                self.logging_console.add_log_message("Failed to finalize session", "ERROR")

        except Exception as e:
            self.logging_console.add_log_message(f"Error finalizing session: {e}", "ERROR")
            logger.error(f"Error finalizing session: {e}")

    @pyqtSlot()
    def _reset_session(self):

        try:

            if self.session_manager.is_session_active():
                reply = QMessageBox.question(
                    self,
                    "Reset Session",
                    "Are you sure you want to reset the current session? Unsaved data may be lost.",
                    QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No
                )

                if reply != QMessageBox.StandardButton.Yes:
                    return

            self.session_manager.reset_for_next_session()
            self.logging_console.add_log_message("Session manager reset")
            self.status_bar.showMessage("Session reset", 2000)

        except Exception as e:
            self.logging_console.add_log_message(f"Error resetting session: {e}", "ERROR")
            logger.error(f"Error resetting session: {e}")

    @pyqtSlot()
    def _update_status(self):

        try:

            if self.device_manager:
                registry = self.device_manager.get_registry()
                device_count = registry.get_device_count()
                online_count = registry.get_device_count_by_state(DeviceConnectionState.ONLINE)
                recording_count = registry.get_device_count_by_state(
                    DeviceConnectionState.RECORDING)

                device_text = f"Devices: {device_count} ({online_count} online, {recording_count} recording)"
                self.device_count_label.setText(device_text)

            if self.session_manager:
                session = self.session_manager.get_current_session()
                if session:
                    session_text = f"Session: {session.state.value.upper()}"
                else:
                    session_text = "Session: IDLE"

                self.session_status_label.setText(session_text)

            # TODO: Add actual network and discovery status
            self.network_status_label.setText("Network: Active")
            self.discovery_status_label.setText("Discovery: Running")

        except Exception as e:
            logger.error(f"Error updating status: {e}")

    def closeEvent(self, event):

        if self.session_manager and self.session_manager.is_recording():
            reply = QMessageBox.question(
                self,
                "Close Application",
                "Recording is currently active. Are you sure you want to close the application?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No
            )

            if reply != QMessageBox.StandardButton.Yes:
                event.ignore()
                return

        try:
            if self.device_manager:
                asyncio.create_task(self.device_manager.stop())

            self.logging_console.add_log_message("Application closing...")

        except Exception as e:
            logger.error(f"Error during shutdown: {e}")

        event.accept()
