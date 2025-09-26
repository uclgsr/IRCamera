from PyQt6.QtCore import QTimer, pyqtSignal, pyqtSlot
from PyQt6.QtGui import QFont
from PyQt6.QtWidgets import (
    QComboBox,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPushButton,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)
from typing import Any, Dict, List, Optional

try:
    from .plotting_widgets import DataAggregationWidget, MultiModalDashboard
except ImportError:

    logging.warning("Plotting widgets not available - using placeholder classes")


    class MultiModalDashboard(QWidget):
        def __init__(self):
            super().__init__()
            self.setMinimumSize(400, 300)


    class DataAggregationWidget(QWidget):
        def __init__(self):
            super().__init__()
            self.setMinimumSize(200, 150)

        def set_sync_quality(self, quality) -> None:
            pass


class DeviceListWidget(QWidget):
    device_selected = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self.devices: Dict[str, Dict] = {}
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        self.device_list = QListWidget()
        self.device_list.itemClicked.connect(self._on_item_clicked)
        layout.addWidget(self.device_list)

        self.status_label = QLabel("No devices connected")
        layout.addWidget(self.status_label)

    def update_devices(self, devices: List[Dict[str, Any]]) -> None:

        self.device_list.clear()
        self.devices.clear()

        for device in devices:
            device_id = device.get("device_id", "Unknown")
            device_type = device.get("device_type", "Unknown")
            status = device.get("status", "Unknown")

            self.devices[device_id] = device

            item_text = f"{device_id} ({device_type}) - {status}"
            item = QListWidgetItem(item_text)

            if status == "connected":
                item.setStyleSheet("color: green;")
            elif status == "recording":
                item.setStyleSheet("color: cyan;")
            else:
                item.setStyleSheet("color: red;")

            self.device_list.addItem(item)

        count = len(devices)
        connected_count = sum(1 for d in devices if d.get("status") == "connected")
        self.status_label.setText(f"{count} devices ({connected_count} connected)")

    def _on_item_clicked(self, item):

        device_id = item.text().split(" ")[0]
        self.device_selected.emit(device_id)


class SessionControlWidget(QWidget):
    start_session_requested = pyqtSignal()
    stop_session_requested = pyqtSignal()
    new_session_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        self.new_session_btn = QPushButton("New Session")
        self.new_session_btn.clicked.connect(self.new_session_requested.emit)
        layout.addWidget(self.new_session_btn)

        self.start_btn = QPushButton("Start Recording")
        self.start_btn.clicked.connect(self.start_session_requested.emit)
        layout.addWidget(self.start_btn)

        self.stop_btn = QPushButton("Stop Recording")
        self.stop_btn.clicked.connect(self.stop_session_requested.emit)
        self.stop_btn.setEnabled(False)
        layout.addWidget(self.stop_btn)

        self.session_label = QLabel("No active session")
        layout.addWidget(self.session_label)

        self.timer_label = QLabel("00:00:00")
        self.timer_label.setStyleSheet("font-size: 16px; font-weight: bold;")
        layout.addWidget(self.timer_label)

    def update_state(self, session, has_devices) -> None:

        if session:
            self.session_label.setText(f"Session: {session.name}")

            if session.state == "recording":
                self.start_btn.setEnabled(False)
                self.stop_btn.setEnabled(True)
                self.new_session_btn.setEnabled(False)
            else:
                self.start_btn.setEnabled(has_devices)
                self.stop_btn.setEnabled(False)
                self.new_session_btn.setEnabled(True)
        else:
            self.session_label.setText("No active session")
            self.start_btn.setEnabled(False)
            self.stop_btn.setEnabled(False)
            self.new_session_btn.setEnabled(True)
            self.timer_label.setText("00:00:00")


class StatusDisplayWidget(QWidget):

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        sync_group = QGroupBox("Time Synchronization")
        sync_layout = QVBoxLayout(sync_group)

        self.sync_quality_label = QLabel("Quality: --")
        self.sync_offset_label = QLabel("Max Offset: --")
        self.sync_devices_label = QLabel("Sync Devices: 0")

        sync_layout.addWidget(self.sync_quality_label)
        sync_layout.addWidget(self.sync_offset_label)
        sync_layout.addWidget(self.sync_devices_label)

        layout.addWidget(sync_group)

        session_group = QGroupBox("Session Status")
        session_layout = QVBoxLayout(session_group)

        self.session_name_label = QLabel("Name: --")
        self.session_duration_label = QLabel("Duration: --")
        self.session_data_size_label = QLabel("Data Size: --")

        session_layout.addWidget(self.session_name_label)
        session_layout.addWidget(self.session_duration_label)
        session_layout.addWidget(self.session_data_size_label)

        layout.addWidget(session_group)

        self.data_aggregation = DataAggregationWidget()
        layout.addWidget(self.data_aggregation)

    def update_time_sync_stats(self, stats) -> None:

        if stats:
            quality = stats.get("synchronization_rate", 0) * 100
            max_offset = stats.get("max_offset_ms", 0)
            device_count = stats.get("total_devices", 0)

            self.sync_quality_label.setText(f"Quality: {quality:.1f}%")
            self.sync_offset_label.setText(f"Max Offset: {max_offset:.1f}ms")
            self.sync_devices_label.setText(f"Sync Devices: {device_count}")

            self.data_aggregation.set_sync_quality(quality)

    def update_session_info(self, session) -> None:

        if session:
            self.session_name_label.setText(f"Name: {session.name}")
            self.session_duration_label.setText(
                f"Duration: {session.duration_seconds:.1f}s"
            )

            self.session_data_size_label.setText("Data Size: --")


class SystemIntegrationWidget(QWidget):
    elevation_requested = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)

        self.privilege_label = QLabel("Privilege Level: User")
        layout.addWidget(self.privilege_label)

        self.elevate_btn = QPushButton("Request Admin Privileges")
        self.elevate_btn.clicked.connect(
            lambda: self.elevation_requested.emit("System configuration")
        )
        layout.addWidget(self.elevate_btn)

        self.status_label = QLabel("Ready")
        layout.addWidget(self.status_label)

    def update_privilege_level(self, level: str) -> None:
        self.privilege_label.setText(f"Privilege Level: {level}")

    def update_permissions(self, permissions: None = Dict) -> None:
        perm_text = "\n".join(
            [f"{k}: {'✓' if v else '✗'}" for k, v in permissions.items()]
        )
        self.setToolTip(f"Permissions:\n{perm_text}")

    def set_status_message(self, message: str, is_error: bool = False) -> None:
        self.status_label.setText(message)
        color = "red" if is_error else "green"
        self.status_label.setStyleSheet(f"color: {color};")


class BluetoothControlWidget(QWidget):
    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str)
    disconnect_requested = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        scan_layout = QHBoxLayout()
        self.scan_btn = QPushButton("Scan for Devices")
        self.scan_btn.clicked.connect(self.scan_requested.emit)
        scan_layout.addWidget(self.scan_btn)

        layout.addLayout(scan_layout)

        self.device_list = QListWidget()
        layout.addWidget(self.device_list)

        conn_layout = QHBoxLayout()
        self.connect_btn = QPushButton("Connect")
        self.connect_btn.clicked.connect(self._on_connect_clicked)
        self.disconnect_btn = QPushButton("Disconnect")
        self.disconnect_btn.clicked.connect(self._on_disconnect_clicked)

        conn_layout.addWidget(self.connect_btn)
        conn_layout.addWidget(self.disconnect_btn)
        layout.addLayout(conn_layout)

        self.status_label = QLabel("Ready")
        layout.addWidget(self.status_label)

    def _on_connect_clicked(self):

        current_item = self.device_list.currentItem()
        if current_item:
            device_addr = current_item.text().split(" ")[0]
            self.connect_requested.emit(device_addr)

    def _on_disconnect_clicked(self):

        current_item = self.device_list.currentItem()
        if current_item:
            device_addr = current_item.text().split(" ")[0]
            self.disconnect_requested.emit(device_addr)

    def update_devices(self, devices) -> None:

        self.device_list.clear()
        for device in devices:
            addr = device.get("address", "Unknown")
            name = device.get("name", "Unknown Device")
            self.device_list.addItem(f"{addr} - {name}")

    def set_connection_status(self, device_addr: str, connected: bool) -> None:

        status = "Connected" if connected else "Disconnected"
        self.status_label.setText(f"{device_addr}: {status}")

    def set_error_status(self, error: str) -> None:

        self.status_label.setText(f"Error: {error}")
        self.status_label.setStyleSheet("color: red;")


class IntegrationManagementWidget(QWidget):
    integration_status_changed = pyqtSignal(str, bool)
    hub_connection_requested = pyqtSignal()
    spoke_discovery_requested = pyqtSignal()
    sync_test_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self.logger = logging.getLogger(__name__)
        self._setup_ui()
        self._setup_monitoring()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        title = QLabel("Hub-Spoke Integration Manager")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(title)

        conn_group = QGroupBox("Connection Status")
        conn_layout = QVBoxLayout(conn_group)

        self.hub_status_label = QLabel("Hub: Disconnected")
        self.spoke_count_label = QLabel("Active Spokes: 0")
        self.sync_status_label = QLabel("Time Sync: Not Available")

        conn_layout.addWidget(self.hub_status_label)
        conn_layout.addWidget(self.spoke_count_label)
        conn_layout.addWidget(self.sync_status_label)

        layout.addWidget(conn_group)

        btn_group = QGroupBox("Integration Controls")
        btn_layout = QHBoxLayout(btn_group)

        self.hub_connect_btn = QPushButton("Connect to Hub")
        self.hub_connect_btn.clicked.connect(self.hub_connection_requested.emit)

        self.discover_spokes_btn = QPushButton("Discover Spokes")
        self.discover_spokes_btn.clicked.connect(self.spoke_discovery_requested.emit)

        self.sync_test_btn = QPushButton("Test Sync")
        self.sync_test_btn.clicked.connect(self.sync_test_requested.emit)

        btn_layout.addWidget(self.hub_connect_btn)
        btn_layout.addWidget(self.discover_spokes_btn)
        btn_layout.addWidget(self.sync_test_btn)

        layout.addWidget(btn_group)

        metrics_group = QGroupBox("Real-time Metrics")
        metrics_layout = QVBoxLayout(metrics_group)

        self.data_rate_label = QLabel("Data Rate: -- MB/s")
        self.latency_label = QLabel("Network Latency: -- ms")
        self.error_count_label = QLabel("Error Count: 0")

        metrics_layout.addWidget(self.data_rate_label)
        metrics_layout.addWidget(self.latency_label)
        metrics_layout.addWidget(self.error_count_label)

        layout.addWidget(metrics_group)

        log_group = QGroupBox("Status Log")
        log_layout = QVBoxLayout(log_group)

        self.status_log = QTextEdit()
        self.status_log.setMaximumHeight(100)
        self.status_log.setReadOnly(True)
        log_layout.addWidget(self.status_log)

        layout.addWidget(log_group)

    def _setup_monitoring(self):

        self.monitor_timer = QTimer()
        self.monitor_timer.timeout.connect(self._update_metrics)
        self.monitor_timer.start(1000)

    @pyqtSlot()
    def _update_metrics(self):

        pass

    def update_hub_status(self, connected: bool = False, address: str = "") -> None:

        if connected:
            self.hub_status_label.setText(f"Hub: Connected ({address})")
            self.hub_status_label.setStyleSheet("color: green;")
            self.hub_connect_btn.setText("Disconnect Hub")
        else:
            self.hub_status_label.setText("Hub: Disconnected")
            self.hub_status_label.setStyleSheet("color: red;")
            self.hub_connect_btn.setText("Connect to Hub")

    def update_spoke_count(
            self, count: int = 0, active_spokes: Optional[List[str]] = None
    ) -> None:

        self.spoke_count_label.setText(f"Active Spokes: {count}")
        if count > 0:
            self.spoke_count_label.setStyleSheet("color: green;")
            if active_spokes:
                tooltip = "Active Spokes:\n" + "\n".join(active_spokes)
                self.spoke_count_label.setToolTip(tooltip)
        else:
            self.spoke_count_label.setStyleSheet("color: gray;")

    def update_sync_status(
            self, synchronized: bool = False, max_offset_ms: float = 0
    ) -> None:

        if synchronized:
            self.sync_status_label.setText(
                f"Time Sync: Active (±{max_offset_ms:.1f}ms)"
            )
            if max_offset_ms <= 5.0:
                self.sync_status_label.setStyleSheet("color: green;")
            else:
                self.sync_status_label.setStyleSheet("color: orange;")
        else:
            self.sync_status_label.setText("Time Sync: Not Available")
            self.sync_status_label.setStyleSheet("color: red;")

    def update_metrics(
            self, data_rate_mbps: float, latency_ms: float, error_count: int
    ) -> None:

        self.data_rate_label.setText(f"Data Rate: {data_rate_mbps:.2f} MB/s")
        self.latency_label.setText(f"Network Latency: {latency_ms:.1f} ms")
        self.error_count_label.setText(f"Error Count: {error_count}")

        if latency_ms < 10:
            self.latency_label.setStyleSheet("color: green;")
        elif latency_ms < 50:
            self.latency_label.setStyleSheet("color: orange;")
        else:
            self.latency_label.setStyleSheet("color: red;")

    def add_status_message(self, message: str, level: str = "INFO") -> None:

        timestamp = QTimer().time().toString("hh:mm:ss")
        formatted_msg = f"[{timestamp}] {level}: {message}"

        if level == "ERROR":
            color = "red"
        elif level == "WARNING":
            color = "orange"
        else:
            color = "black"

        self.status_log.append(f"<span style='color: {color};'>{formatted_msg}</span>")

        cursor = self.status_log.textCursor()
        cursor.movePosition(cursor.MoveOperation.End)
        self.status_log.setTextCursor(cursor)

        self.integration_status_changed.emit(message, level == "ERROR")

    def set_integration_error(self, error: str) -> None:

        self.add_status_message(f"Integration Error: {error}", "ERROR")
        self.
        if "critical" in error.lower() or "fatal" in error.lower():
            QMessageBox.critical(self, "Critical Integration Error", error)


class WiFiControlWidget(QWidget):
    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str, str)
    disconnect_requested = pyqtSignal()
    hotspot_start_requested = pyqtSignal(str, str, int)
    hotspot_stop_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):

        layout = QVBoxLayout(self)

        self.network_list = QComboBox()
        layout.addWidget(self.network_list)

        self.scan_btn = QPushButton("Scan Networks")
        self.scan_btn.clicked.connect(self.scan_requested.emit)
        layout.addWidget(self.scan_btn)

        self.connect_btn = QPushButton("Connect to Network")
        layout.addWidget(self.connect_btn)

        self.disconnect_btn = QPushButton("Disconnect")
        self.disconnect_btn.clicked.connect(self.disconnect_requested.emit)
        layout.addWidget(self.disconnect_btn)

        hotspot_group = QGroupBox("Mobile Hotspot")
        hotspot_layout = QVBoxLayout(hotspot_group)

        self.hotspot_start_btn = QPushButton("Start Hotspot")
        self.hotspot_stop_btn = QPushButton("Stop Hotspot")

        hotspot_layout.addWidget(self.hotspot_start_btn)
        hotspot_layout.addWidget(self.hotspot_stop_btn)

        layout.addWidget(hotspot_group)

        self.status_label = QLabel("Ready")
        layout.addWidget(self.status_label)

    def update_networks(self, networks) -> None:

        self.network_list.clear()
        for network in networks:
            ssid = network.get("ssid", "Unknown")
            signal = network.get("signal_strength", 0)
            self.network_list.addItem(f"{ssid} ({signal}%)")

    def set_connection_status(self, ssid: str, connected: bool, ip: str = "") -> None:

        if connected:
            self.status_label.setText(f"Connected to {ssid} ({ip})")
        else:
            self.status_label.setText(f"Disconnected from {ssid}")

    def set_hotspot_status(self, active: bool, message: str = "") -> None:

        status = "Active" if active else "Inactive"
        self.status_label.setText(f"Hotspot: {status} {message}")

    def set_error_status(self, error: str) -> None:

        self.status_label.setText(f"Error: {error}")
        self.status_label.setStyleSheet("color: red;")
