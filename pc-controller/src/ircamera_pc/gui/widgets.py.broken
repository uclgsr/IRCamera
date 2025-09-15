"""
Custom widgets for IRCamera PC Controller GUI

Specialized UI components for device management and session control.
"""

from PyQt6.QtWidgets import (
    QWidget,
    QVBoxLayout,
    QHBoxLayout,
    QPushButton,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QGroupBox,
    QGridLayout,
    QFrame,
    QLineEdit,
    QSpinBox,
    QMessageBox,
)
from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtGui import QFont, QColor
from typing import Dict, Any, Optional
from datetime import datetime

from ..network.server import DeviceInfo, DeviceState
from ..core.session import SessionMetadata, SessionState


class DeviceListWidget(QListWidget):
    """
    Custom widget for displaying connected devices with status indicators.
    """

    device_selected = pyqtSignal(str)  # device_id

    def __init__(self):
        """Initialize device list widget."
        super().__init__()
        self.setMinimumHeight(200)
        self._device_items: Dict[str, QListWidgetItem] = {}

        # Connect selection signal
        self.itemClicked.connect(self._on_item_clicked)

    def update_devices(self, devices: Dict[str, DeviceInfo]) -> None:
        """
        Update the device list with current device information.

        Args:
            devices: Dictionary mapping device_id to DeviceInfo
        """
        # Remove devices that are no longer connected
        for device_id in list(self._device_items.keys()):
            if device_id not in devices:
                item = self._device_items.pop(device_id)
                row = self.row(item)
                self.takeItem(row)

        # Add or update existing devices
        for device_id, device_info in devices.items():
            if device_id in self._device_items:
                self._update_device_item(device_id, device_info)
            else:
                self._add_device_item(device_id, device_info)

    def _add_device_item(self,
        device_id: str,
        device_info: DeviceInfo) -> None:
        """Add new device item to list."""
        item = QListWidgetItem()
        item.setData(Qt.ItemDataRole.UserRole, device_id)

        self._device_items[device_id] = item
        self.addItem(item)
        self._update_device_item(device_id, device_info)

    def _update_device_item(self,
        device_id: str,
        device_info: DeviceInfo) -> None:
        """Update existing device item."""
        item = self._device_items[device_id]

        # Create status text
        status_text = self._format_device_status(device_info)
        item.setText(status_text)

        # Set color based on state
        if device_info.state == DeviceState.CONNECTED.value:
            item.setForeground(QColor(0, 120, 0))  # Green
        elif device_info.state == DeviceState.RECORDING.value:
            item.setForeground(QColor(200, 0, 0))  # Red
        elif device_info.state == DeviceState.ERROR.value:
            item.setForeground(QColor(200, 100, 0))  # Orange
        else:
            item.setForeground(QColor(100, 100, 100))  # Gray

    def _format_device_status(self, device_info: DeviceInfo) -> str:
        """Format device information for display."""
        lines = []

        # Device name and type
        name_line = f"📱 {device_info.device_id}"
        if device_info.is_gsr_leader:
            name_line += " (GSR Leader)"
        lines.append(name_line)

        # Device type and capabilities
        lines.append(f"   Type: {device_info.device_type}")

        if device_info.capabilities:
            caps = ", ".join(device_info.capabilities)
            lines.append(f"   Capabilities: {caps}")

        # Status and battery
        status_parts = [f"State: {device_info.state}"]

        if device_info.battery_level is not None:
            battery_icon = "🔋" if device_info.battery_level > 20 else "🪫"
            status_parts.append(f"Battery: {device_info.battery_level}%{battery_icon}")

        lines.append(f"   {', '.join(status_parts)}")

        # Connection info
        if device_info.last_heartbeat:
            try:
                last_hb = datetime.fromisoformat(
                    device_info.last_heartbeat.replace("Z", "+00:00")
                )
                time_since = (
                    datetime.now().replace(tzinfo=last_hb.tzinfo) - last_hb
                ).total_seconds()
                lines.append(f"   Last heartbeat: {time_since:.0f}s ago")
            except (ValueError, TypeError, AttributeError):
                lines.append(f"   Last heartbeat: {device_info.last_heartbeat}")

        return "\n".join(lines)

    def _on_item_clicked(self, item: QListWidgetItem) -> None:
        """Handle item click."""
        device_id = item.data(Qt.ItemDataRole.UserRole)
        if device_id:
            self.device_selected.emit(device_id)


class SessionControlWidget(QWidget):
    """
    Widget for session control operations.
    """

    new_session_requested = pyqtSignal()
    start_session_requested = pyqtSignal()
    stop_session_requested = pyqtSignal()

    def __init__(self):
        """Initialize session control widget."""
        super().__init__()
        self._setup_ui()

        # Current state
        self._current_session: Optional[SessionMetadata] = None
        self._has_devices = False

    def _setup_ui(self) -> None:
        """Set up the user interface."""
        layout = QVBoxLayout(self)

        # Session info display
        info_frame = QFrame()
        info_frame.setFrameStyle(QFrame.Box)
        info_layout = QVBoxLayout(info_frame)

        self.session_name_label = QLabel("No active session")
        self.session_name_label.setFont(QFont("Arial", 10, QFont.Bold))
        info_layout.addWidget(self.session_name_label)

        self.session_status_label = QLabel("Create a new session to begin")
        info_layout.addWidget(self.session_status_label)

        layout.addWidget(info_frame)

        # Control buttons
        button_layout = QVBoxLayout()

        self.new_session_btn = QPushButton("New Session")
        self.new_session_btn.clicked.connect(self.new_session_requested.emit)
        button_layout.addWidget(self.new_session_btn)

        self.start_session_btn = QPushButton("Start Recording")
        self.start_session_btn.clicked.connect(self.start_session_requested.emit)
        self.start_session_btn.setProperty("class", "primary")
        button_layout.addWidget(self.start_session_btn)

        self.stop_session_btn = QPushButton("Stop Recording")
        self.stop_session_btn.clicked.connect(self.stop_session_requested.emit)
        self.stop_session_btn.setProperty("class", "danger")
        button_layout.addWidget(self.stop_session_btn)

        layout.addLayout(button_layout)

    def update_state(
        self, session: Optional[SessionMetadata], has_devices: bool
    ) -> None:
        """
        Update widget state based on current session and device status.

        Args:
            session: Current session metadata
            has_devices: Whether any devices are connected
        """
        self._current_session = session
        self._has_devices = has_devices

        # Update display
        if session:
            self.session_name_label.setText(f"Session: {session.name}")

            if session.state == SessionState.IDLE.value:
                self.session_status_label.setText("Ready to start recording")
            elif session.state == SessionState.ACTIVE.value:
                self.session_status_label.setText(
                    "Session active - preparing to record"
                )
            elif session.state == SessionState.RECORDING.value:
                self.session_status_label.setText("🔴 Recording in progress")
            else:
                self.session_status_label.setText(f"Status: {session.state}")
        else:
            self.session_name_label.setText("No active session")
            self.session_status_label.setText("Create a new session to begin")

        # Update button states
        self._update_button_states()

    def _update_button_states(self) -> None:
        """Update button enabled/disabled states."""
        has_idle_session = (
            self._current_session
            and self._current_session.state == SessionState.IDLE.value
        )

        is_recording = self._current_session and self._current_session.state in [
            SessionState.ACTIVE.value,
            SessionState.RECORDING.value,
        ]

        # New session: enabled when no active session or session is completed
        self.new_session_btn.setEnabled(
            not self._current_session
            or self._current_session.state
            in [SessionState.COMPLETED.value, SessionState.ERROR.value]
        )

        # Start recording: enabled when idle session exists and devices are connected
        self.start_session_btn.setEnabled(has_idle_session and self._has_devices)

        # Stop recording: enabled when recording is active
        self.stop_session_btn.setEnabled(is_recording)


class StatusDisplayWidget(QWidget):
    """
    Widget for displaying system status information.
    """

    def __init__(self):
        """Initialize status display widget."""
        super().__init__()
        self._setup_ui()

    def _setup_ui(self) -> None:
        """Set up the user interface."""
        layout = QGridLayout(self)

        # Time synchronization status
        layout.addWidget(QLabel("Time Synchronization:"), 0, 0)
        self.sync_quality_label = QLabel("No data")
        layout.addWidget(self.sync_quality_label, 0, 1)

        self.sync_median_label = QLabel("Median offset: --")
        layout.addWidget(self.sync_median_label, 1, 0, 1, 2)

        self.sync_p95_label = QLabel("P95 offset: --")
        layout.addWidget(self.sync_p95_label, 2, 0, 1, 2)

        # Session information
        layout.addWidget(QLabel("Current Session:"), 3, 0)
        self.session_info_label = QLabel("No active session")
        layout.addWidget(self.session_info_label, 3, 1)

        self.session_duration_label = QLabel("Duration: --:--:--")
        layout.addWidget(self.session_duration_label, 4, 0, 1, 2)

        # GSR leader status
        layout.addWidget(QLabel("GSR Leader:"), 5, 0)
        self.gsr_leader_label = QLabel("No leader")
        layout.addWidget(self.gsr_leader_label, 5, 1)

        # Stretch at bottom
        layout.setRowStretch(6, 1)

    def update_time_sync_stats(self, stats: Dict[str, Any]) -> None:
        """Update time synchronization statistics display."""
        total_devices = stats.get("total_devices", 0)
        sync_devices = stats.get("synchronized_devices", 0)
        sync_rate = stats.get("synchronization_rate", 0) * 100

        if total_devices > 0:
            self.sync_quality_label.setText(
                f"{sync_devices}/{total_devices} devices ({sync_rate:.0f}%)"
            )

            # Color coding based on sync quality
            if sync_rate >= 90:
                color = "green"
            elif sync_rate >= 70:
                color = "orange"
            else:
                color = "red"

            self.sync_quality_label.setStyleSheet(f"color: {color}; font-weight: bold;")
        else:
            self.sync_quality_label.setText("No devices")
            self.sync_quality_label.setStyleSheet("color: gray;")

        # Update offset displays
        median_offset = stats.get("overall_median_offset_ms", 0)
        p95_offset = stats.get("overall_p95_offset_ms", 0)

        self.sync_median_label.setText(f"Median offset: {median_offset:.1f} ms")
        self.sync_p95_label.setText(f"P95 offset: {p95_offset:.1f} ms")

    def update_session_info(self, session: SessionMetadata) -> None:
        """Update session information display."""
        self.session_info_label.setText(f"{session.name} ({session.state})")

        # Calculate and display duration
        if session.started_at:
            try:
                start_time = datetime.fromisoformat(
                    session.started_at.replace("Z", "+00:00")
                )

                if session.ended_at:
                    end_time = datetime.fromisoformat(
                        session.ended_at.replace("Z", "+00:00")
                    )
                    duration = end_time - start_time
                else:
                    duration = (
                        datetime.now().replace(tzinfo=start_time.tzinfo) - start_time
                    )

                # Format duration as HH:MM:SS
                total_seconds = int(duration.total_seconds())
                hours = total_seconds // 3600
                minutes = (total_seconds % 3600) // 60
                seconds = total_seconds % 60

                self.session_duration_label.setText(
                    f"Duration: {hours:02d}:{minutes:02d}:{seconds:02d}"
                )

            except (OSError, ValueError, RuntimeError):
                self.session_duration_label.setText("Duration: --:--:--")
        else:
            self.session_duration_label.setText("Duration: --:--:--")

    def update_gsr_leader_info(self,
        leader_info: Optional[DeviceInfo]) -> None:
        """Update GSR leader information display."""
        if leader_info:
            self.gsr_leader_label.setText(
                f"{leader_info.device_id} ({leader_info.gsr_mode})"
            )
        else:
            self.gsr_leader_label.setText("No leader")


class BluetoothControlWidget(QGroupBox):
    """
    Widget for Bluetooth device discovery and connection management.
    """

    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str)  # device_address
    disconnect_requested = pyqtSignal(str)  # device_address

    def __init__(self):
        """Initialize Bluetooth control widget."""
        super().__init__("Bluetooth Control")
        self.setMinimumHeight(300)

        self._devices: Dict[str, Any] = {}  # address -> BluetoothDevice
        self._setup_ui()

    def _setup_ui(self) -> None:
        """Setup the user interface."""
        layout = QVBoxLayout()

        # Control buttons
        button_layout = QHBoxLayout()

        self.scan_button = QPushButton("Scan for Devices")
        self.scan_button.clicked.connect(self.scan_requested.emit)
        button_layout.addWidget(self.scan_button)

        self.status_label = QLabel("Ready")
        self.status_label.setStyleSheet("color: gray; font-style: italic;")
        button_layout.addWidget(self.status_label)

        button_layout.addStretch()
        layout.addLayout(button_layout)

        # Device list
        self.device_list = QListWidget()
        self.device_list.setMinimumHeight(200)
        self.device_list.itemDoubleClicked.connect(self._on_device_double_clicked)
        layout.addWidget(self.device_list)

        # Connection status
        connection_layout = QHBoxLayout()

        self.connect_button = QPushButton("Connect")
        self.connect_button.setEnabled(False)
        self.connect_button.clicked.connect(self._on_connect_clicked)
        connection_layout.addWidget(self.connect_button)

        self.disconnect_button = QPushButton("Disconnect")
        self.disconnect_button.setEnabled(False)
        self.disconnect_button.clicked.connect(self._on_disconnect_clicked)
        connection_layout.addWidget(self.disconnect_button)

        connection_layout.addStretch()
        layout.addLayout(connection_layout)

        self.setLayout(layout)

    def update_devices(self, devices: list) -> None:
        """Update the list of discovered Bluetooth devices."""
        self.device_list.clear()
        self._devices.clear()

        for device in devices:
            self._devices[device.address] = device

            # Create list item
            item_text = f"{device.name} ({device.address})"
            if device.is_ircamera:
                item_text += " [IR Camera]"

            item = QListWidgetItem(item_text)

            # Set color based on device type and connection state
            if device.is_ircamera:
                item.setBackground(QColor(220,
                    255,
                    220))  # Light green for IRCamera

            if (
                hasattr(device, "connection_state")
                and device.connection_state.value == "connected"
            ):
                item.setBackground(QColor(200,
                    255,
                    200))  # Green for connected
                item.setFont(QFont("", 9, QFont.Weight.Bold))

            self.device_list.addItem(item)

    def set_scanning_status(self, scanning: bool) -> None:
        """Update scanning status."""
        if scanning:
            self.scan_button.setText("Scanning...")
            self.scan_button.setEnabled(False)
            self.status_label.setText("Scanning for devices...")
            self.status_label.setStyleSheet("color: blue; font-style: italic;")
        else:
            self.scan_button.setText("Scan for Devices")
            self.scan_button.setEnabled(True)
            self.status_label.setText("Ready")
            self.status_label.setStyleSheet("color: gray; font-style: italic;")

    def set_connection_status(self, address: str, connected: bool) -> None:
        """Update connection status for a device."""
        if connected:
            self.status_label.setText(f"Connected to {address}")
            self.status_label.setStyleSheet("color: green; font-weight: bold;")
            self.disconnect_button.setEnabled(True)
        else:
            self.status_label.setText("Ready")
            self.status_label.setStyleSheet("color: gray; font-style: italic;")
            self.disconnect_button.setEnabled(False)

    def set_error_status(self, message: str) -> None:
        """Display error status."""
        self.status_label.setText(f"Error: {message}")
        self.status_label.setStyleSheet("color: red; font-weight: bold;")

    def _on_device_double_clicked(self, item: QListWidgetItem) -> None:
        """Handle device double-click."""
        self._on_connect_clicked()

    def _on_connect_clicked(self) -> None:
        """Handle connect button click."""
        current_item = self.device_list.currentItem()
        if not current_item:
            return

        # Extract address from item text
        item_text = current_item.text()
        address = item_text.split("(")[1].split(")")[0]

        self.connect_requested.emit(address)
        self.connect_button.setEnabled(False)

    def _on_disconnect_clicked(self) -> None:
        """Handle disconnect button click."""
        current_item = self.device_list.currentItem()
        if not current_item:
            return

        # Extract address from item text
        item_text = current_item.text()
        address = item_text.split("(")[1].split(")")[0]

        self.disconnect_requested.emit(address)


class WiFiControlWidget(QGroupBox):
    """
    Widget for WiFi network scanning and connection management.
    """

    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str, str)  # ssid, password
    disconnect_requested = pyqtSignal()
    hotspot_start_requested = pyqtSignal(str, str, int)  # ssid, password, channel
    hotspot_stop_requested = pyqtSignal()

    def __init__(self):
        """Initialize WiFi control widget."""
        super().__init__("WiFi Control")
        self.setMinimumHeight(400)

        self._networks: Dict[str, Any] = {}  # ssid -> WiFiNetwork
        self._current_connection: Optional[str] = None
        self._setup_ui()

    def _setup_ui(self) -> None:
        """Setup the user interface."""
        layout = QVBoxLayout()

        # Control buttons
        button_layout = QHBoxLayout()

        self.scan_button = QPushButton("Scan Networks")
        self.scan_button.clicked.connect(self.scan_requested.emit)
        button_layout.addWidget(self.scan_button)

        self.status_label = QLabel("Ready")
        self.status_label.setStyleSheet("color: gray; font-style: italic;")
        button_layout.addWidget(self.status_label)

        button_layout.addStretch()
        layout.addLayout(button_layout)

        # Network list
        self.network_list = QListWidget()
        self.network_list.setMinimumHeight(200)
        self.network_list.itemSelectionChanged.connect(
            self._on_network_selection_changed
        )
        layout.addWidget(self.network_list)

        # Connection controls
        connection_group = QGroupBox("Connection")
        connection_layout = QVBoxLayout()

        # Connection buttons
        conn_button_layout = QHBoxLayout()

        self.connect_button = QPushButton("Connect")
        self.connect_button.setEnabled(False)
        self.connect_button.clicked.connect(self._on_connect_clicked)
        conn_button_layout.addWidget(self.connect_button)

        self.disconnect_button = QPushButton("Disconnect")
        self.disconnect_button.setEnabled(False)
        self.disconnect_button.clicked.connect(self.disconnect_requested.emit)
        conn_button_layout.addWidget(self.disconnect_button)

        conn_button_layout.addStretch()
        connection_layout.addLayout(conn_button_layout)

        # Password input (initially hidden)
        self.password_label = QLabel("Password:")
        self.password_input = QLineEdit()
        self.password_input.setEchoMode(QLineEdit.EchoMode.Password)
        self.password_input.setPlaceholderText("Enter network password")

        password_layout = QHBoxLayout()
        password_layout.addWidget(self.password_label)
        password_layout.addWidget(self.password_input)
        connection_layout.addLayout(password_layout)

        # Hide password controls initially
        self.password_label.setVisible(False)
        self.password_input.setVisible(False)

        connection_group.setLayout(connection_layout)
        layout.addWidget(connection_group)

        # Hotspot controls
        hotspot_group = QGroupBox("Mobile Hotspot")
        hotspot_layout = QVBoxLayout()

        # Hotspot buttons
        hotspot_button_layout = QHBoxLayout()

        self.start_hotspot_button = QPushButton("Start Hotspot")
        self.start_hotspot_button.clicked.connect(self._on_start_hotspot_clicked)
        hotspot_button_layout.addWidget(self.start_hotspot_button)

        self.stop_hotspot_button = QPushButton("Stop Hotspot")
        self.stop_hotspot_button.setEnabled(False)
        self.stop_hotspot_button.clicked.connect(self.hotspot_stop_requested.emit)
        hotspot_button_layout.addWidget(self.stop_hotspot_button)

        hotspot_button_layout.addStretch()
        hotspot_layout.addLayout(hotspot_button_layout)

        # Hotspot configuration
        config_layout = QGridLayout()

        config_layout.addWidget(QLabel("SSID:"), 0, 0)
        self.hotspot_ssid_input = QLineEdit("IRCamera_PC_Controller")
        config_layout.addWidget(self.hotspot_ssid_input, 0, 1)

        config_layout.addWidget(QLabel("Password:"), 1, 0)
        self.hotspot_password_input = QLineEdit("IRCamera123")
        self.hotspot_password_input.setEchoMode(QLineEdit.EchoMode.Password)
        config_layout.addWidget(self.hotspot_password_input, 1, 1)

        config_layout.addWidget(QLabel("Channel:"), 2, 0)
        self.hotspot_channel_input = QSpinBox()
        self.hotspot_channel_input.setRange(1, 13)
        self.hotspot_channel_input.setValue(6)
        config_layout.addWidget(self.hotspot_channel_input, 2, 1)

        hotspot_layout.addLayout(config_layout)

        self.hotspot_status_label = QLabel("Hotspot stopped")
        self.hotspot_status_label.setStyleSheet("color: gray; font-style: italic;")
        hotspot_layout.addWidget(self.hotspot_status_label)

        hotspot_group.setLayout(hotspot_layout)
        layout.addWidget(hotspot_group)

        self.setLayout(layout)

    def update_networks(self, networks: list) -> None:
        """Update the list of available WiFi networks."""
        self.network_list.clear()
        self._networks.clear()

        for network in networks:
            self._networks[network.ssid] = network

            # Create list item
            signal_bars = self._get_signal_bars(network.signal_strength)
            security_icon = "🔒" if network.security_type.value"
                "!= "open" else "🔓"

            item_text = f"{security_icon} {network.ssid} {signal_bars}"
            if network.is_ircamera_hotspot:
                item_text += " [IR Camera]"

            item = QListWidgetItem(item_text)

            # Set color based on network type
            if network.is_ircamera_hotspot:
                item.setBackground(QColor(220,
                    255,
                    220))  # Light green for IRCamera

            self.network_list.addItem(item)

    def set_scanning_status(self, scanning: bool) -> None:
        """Update scanning status."""
        if scanning:
            self.scan_button.setText("Scanning...")
            self.scan_button.setEnabled(False)
            self.status_label.setText("Scanning for networks...")
            self.status_label.setStyleSheet("color: blue; font-style: italic;")
        else:
            self.scan_button.setText("Scan Networks")
            self.scan_button.setEnabled(True)
            self.status_label.setText("Ready")
            self.status_label.setStyleSheet("color: gray; font-style: italic;")

    def set_connection_status(
        self, ssid: str, connected: bool, ip_address: str = None
    ) -> None:
        """Update WiFi connection status."""
        if connected:
            self._current_connection = ssid
            status_text = f"Connected to {ssid}"
            if ip_address:
                status_text += f" ({ip_address})"
            self.status_label.setText(status_text)
            self.status_label.setStyleSheet("color: green; font-weight: bold;")
            self.disconnect_button.setEnabled(True)
            self.connect_button.setEnabled(False)
        else:
            self._current_connection = None
            self.status_label.setText("Ready")
            self.status_label.setStyleSheet("color: gray; font-style: italic;")
            self.disconnect_button.setEnabled(False)
            self.connect_button.setEnabled(True)

    def set_hotspot_status(self, state: str, message: str = None) -> None:
        """Update hotspot status."""
        state_colors = {
            "stopped": "color: gray; font-style: italic;",
            "starting": "color: blue; font-style: italic;",
            "running": "color: green; font-weight: bold;",
            "stopping": "color: orange; font-style: italic;",
            "error": "color: red; font-weight: bold;",
        }

        self.hotspot_status_label.setText(message or f"Hotspot {state}")
        self.hotspot_status_label.setStyleSheet(state_colors.get(state, ""))

        if state == "running":
            self.start_hotspot_button.setEnabled(False)
            self.stop_hotspot_button.setEnabled(True)
        else:
            self.start_hotspot_button.setEnabled(True)
            self.stop_hotspot_button.setEnabled(False)

    def set_error_status(self, message: str) -> None:
        """Display error status."""
        self.status_label.setText(f"Error: {message}")
        self.status_label.setStyleSheet("color: red; font-weight: bold;")

    def _get_signal_bars(self, signal_strength: int) -> str:
        """Convert signal strength to visual bars."""
        if signal_strength >= -50:
            return "📶📶📶📶"
        elif signal_strength >= -60:
            return "📶📶📶"
        elif signal_strength >= -70:
            return "📶📶"
        elif signal_strength >= -80:
            return "📶"
        else:
            return "📵"

    def _on_network_selection_changed(self) -> None:
        """Handle network selection change."""
        current_item = self.network_list.currentItem()
        if not current_item:
            self.connect_button.setEnabled(False)
            self._hide_password_input()
            return

        # Extract SSID from item text
        item_text = current_item.text()
        ssid = self._extract_ssid(item_text)

        if ssid and ssid in self._networks:
            network = self._networks[ssid]
            self.connect_button.setEnabled(True)

            # Show password input for secured networks
            if network.security_type.value != "open":
                self._show_password_input()
            else:
                self._hide_password_input()

    def _show_password_input(self) -> None:
        """Show password input controls."""
        self.password_label.setVisible(True)
        self.password_input.setVisible(True)
        self.password_input.setFocus()

    def _hide_password_input(self) -> None:
        """Hide password input controls."""
        self.password_label.setVisible(False)
        self.password_input.setVisible(False)
        self.password_input.clear()

    def _extract_ssid(self, item_text: str) -> Optional[str]:
        """Extract SSID from list item text."""
        # Remove security icon and signal bars
        parts = item_text.split(" ")
        if len(parts) >= 2:
            # Remove first part (security icon) and last part (signal bars)
            ssid_parts = parts[1:-1]
            # Handle case where SSID contains spaces
            if "[IR Camera]" in item_text:
                # Remove "[IR Camera]" marker
                ssid_parts = [
                    part for part in ssid_parts if part != "[IR" and part != "Camera]"
                ]
            return " ".join(ssid_parts)
        return None

    def _on_connect_clicked(self) -> None:
        """Handle connect button click."""
        current_item = self.network_list.currentItem()
        if not current_item:
            return

        ssid = self._extract_ssid(current_item.text())
        if not ssid:
            return

        password = self.password_input.text() if self.password_input.isVisible() else ""
        self.connect_requested.emit(ssid, password)
        self.connect_button.setEnabled(False)

    def _on_start_hotspot_clicked(self) -> None:
        """Handle start hotspot button click."""
        ssid = self.hotspot_ssid_input.text()
        password = self.hotspot_password_input.text()
        channel = self.hotspot_channel_input.value()

        if not ssid or not password:
            QMessageBox.warning(
                self,
                "Invalid Configuration",
                "Please provide both SSID and password for the hotspot.",
            )
            return

        self.hotspot_start_requested.emit(ssid, password, channel)


class SystemIntegrationWidget(QGroupBox):
    """
    Widget for system integration status and administrator privileges.
    """

    elevation_requested = pyqtSignal(str)  # reason

    def __init__(self):
        """Initialize system integration widget."""
        super().__init__("System Integration")
        self.setMinimumHeight(200)

        self._setup_ui()

    def _setup_ui(self) -> None:
        """Setup the user interface."""
        layout = QVBoxLayout()

        # Privilege status
        privilege_layout = QHBoxLayout()

        self.privilege_label = QLabel("Privilege Level:")
        privilege_layout.addWidget(self.privilege_label)

        self.privilege_status = QLabel("Unknown")
        self.privilege_status.setStyleSheet("font-weight: bold;")
        privilege_layout.addWidget(self.privilege_status)

        privilege_layout.addStretch()

        self.elevate_button = QPushButton("Request Administrator")
        self.elevate_button.clicked.connect(self._on_elevate_clicked)
        privilege_layout.addWidget(self.elevate_button)

        layout.addLayout(privilege_layout)

        # Permissions status
        permissions_group = QGroupBox("System Permissions")
        permissions_layout = QGridLayout()

        self.permission_labels = {}
        permissions = [
            ("network_config", "Network Configuration"),
            ("bluetooth_control", "Bluetooth Control"),
            ("service_management", "Service Management"),
            ("registry_access", "Registry Access"),
            ("hardware_access", "Hardware Access"),
            ("firewall_control", "Firewall Control"),
        ]

        for i, (key, label) in enumerate(permissions):
            permissions_layout.addWidget(QLabel(f"{label}:"), i, 0)

            status_label = QLabel("Unknown")
            self.permission_labels[key] = status_label
            permissions_layout.addWidget(status_label, i, 1)

        permissions_group.setLayout(permissions_layout)
        layout.addWidget(permissions_group)

        # Status messages
        self.status_label = QLabel("System integration status unknown")
        self.status_label.setStyleSheet("color: gray; font-style: italic;")
        layout.addWidget(self.status_label)

        self.setLayout(layout)

    def update_privilege_level(self, level: str) -> None:
        """Update privilege level display."""
        level_colors = {
            "user": "color: orange;",
            "elevated": "color: blue;",
            "admin": "color: green;",
            "system": "color: purple;",
            "unknown": "color: gray;",
        }

        self.privilege_status.setText(level.title())
        self.privilege_status.setStyleSheet(
            f"font-weight: bold; {level_colors.get(level, '')}"
        )

        # Update elevate button
        if level in ["elevated", "admin", "system"]:
            self.elevate_button.setText("Administrator Access")
            self.elevate_button.setEnabled(False)
            self.elevate_button.setStyleSheet("color: green;")
        else:
            self.elevate_button.setText("Request Administrator")
            self.elevate_button.setEnabled(True)
            self.elevate_button.setStyleSheet("")

    def update_permissions(self, permissions: Dict[str, bool]) -> None:
        """Update system permissions status."""
        for key, has_permission in permissions.items():
            if key in self.permission_labels:
                label = self.permission_labels[key]
                if has_permission:
                    label.setText("✅ Available")
                    label.setStyleSheet("color: green;")
                else:
                    label.setText("❌ Denied")
                    label.setStyleSheet("color: red;")

    def set_status_message(self, message: str, is_error: bool = False) -> None:
        """Set status message."""
        self.status_label.setText(message)
        if is_error:
            self.status_label.setStyleSheet("color: red; font-weight: bold;")
        else:
            self.status_label.setStyleSheet("color: gray; font-style: italic;")

    def _on_elevate_clicked(self) -> None:
        """Handle elevate button click."""
        self.elevation_requested.emit(
            "Full system integration for IRCamera PC Controller"
        )
