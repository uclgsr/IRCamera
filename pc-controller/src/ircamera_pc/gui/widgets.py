"""
PyQt6-based GUI widgets for IRCamera PC Controller

This module contains all the user interface widgets for the PC Controller
application, providing device management, network control, and system
integration with real-time data visualization.
"""

from datetime import datetime
from typing import Dict, List, Optional

import pyqtgraph as pg
from PyQt6.QtCore import QTimer, pyqtSignal, Qt
from PyQt6.QtGui import QFont
from PyQt6.QtWidgets import (
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QPushButton,
    QTextEdit,
    QVBoxLayout,
    QWidget,
    QProgressBar,
    QFrame,
    QGridLayout,
    QSpacerItem,
    QSizePolicy
)

from ..network.server import DeviceInfo, DeviceState


class DeviceListWidget(QWidget):
    """Device list widget with status indicators and management controls."""

    device_selected = pyqtSignal(str)  # device_id
    connect_requested = pyqtSignal(str)  # device_id
    disconnect_requested = pyqtSignal(str)  # device_id
    refresh_requested = pyqtSignal() # Signal to refresh the list

    def __init__(self):
        super().__init__()
        self._devices: Dict[str, DeviceInfo] = {}
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the device list UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("Connected Devices")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Device list
        self._device_list = QListWidget()
        self._device_list.itemClicked.connect(self._on_device_selected)
        layout.addWidget(self._device_list)

        # Control buttons
        button_layout = QHBoxLayout()
        self._connect_btn = QPushButton("Connect")
        self._disconnect_btn = QPushButton("Disconnect")
        self._refresh_btn = QPushButton("Refresh")

        self._connect_btn.clicked.connect(self._on_connect_clicked)
        self._disconnect_btn.clicked.connect(self._on_disconnect_clicked)
        self._refresh_btn.clicked.connect(self._on_refresh_clicked)

        button_layout.addWidget(self._connect_btn)
        button_layout.addWidget(self._disconnect_btn)
        button_layout.addWidget(self._refresh_btn)
        layout.addLayout(button_layout)

        # Initially disable buttons
        self._connect_btn.setEnabled(False)
        self._disconnect_btn.setEnabled(False)

    def update_devices(self, devices: List[DeviceInfo]):
        """Update the device list display."""
        current_selection = self._device_list.currentItem()
        selected_id = current_selection.data(Qt.ItemDataRole.UserRole) if current_selection else None

        self._devices.clear()
        self._device_list.clear()

        new_item_to_select = None
        for device in devices:
            self._devices[device.device_id] = device

            # Create list item with status indicator
            status_icon = self._get_status_icon(device.state)
            item_text = f"{status_icon} {device.device_name} ({device.ip_address})"

            item = QListWidgetItem(item_text)
            item.setData(Qt.ItemDataRole.UserRole, device.device_id)
            self._device_list.addItem(item)
            if device.device_id == selected_id:
                new_item_to_select = item

        if new_item_to_select:
            self._device_list.setCurrentItem(new_item_to_select)
            self._on_device_selected(new_item_to_select) # Refresh button state

    def _get_status_icon(self, state: DeviceState) -> str:
        """Get status icon for device state."""
        icons = {
            DeviceState.CONNECTED: "🟢",
            DeviceState.CONNECTING: "🟡",
            DeviceState.RECORDING: "🔴",
            DeviceState.DISCONNECTED: "⚫",
            DeviceState.ERROR: "❌"
        }
        return icons.get(state, "❓")

    def _on_device_selected(self, item: QListWidgetItem):
        """Handle device selection."""
        device_id = item.data(Qt.ItemDataRole.UserRole)
        device = self._devices.get(device_id)

        if device:
            self.device_selected.emit(device_id)
            # Update button states
            is_connected = device.state in [DeviceState.CONNECTED, DeviceState.RECORDING]
            self._connect_btn.setEnabled(not is_connected)
            self._disconnect_btn.setEnabled(is_connected)

    def _on_connect_clicked(self):
        """Handle connect button click."""
        current_item = self._device_list.currentItem()
        if current_item:
            device_id = current_item.data(Qt.ItemDataRole.UserRole)
            self.connect_requested.emit(device_id)

    def _on_disconnect_clicked(self):
        """Handle disconnect button click."""
        current_item = self._device_list.currentItem()
        if current_item:
            device_id = current_item.data(Qt.ItemDataRole.UserRole)
            self.disconnect_requested.emit(device_id)

    def _on_refresh_clicked(self):
        """Handle refresh button click."""
        self.refresh_requested.emit()


class SessionControlWidget(QWidget):
    """Session control widget for recording management."""

    session_start_requested = pyqtSignal(str)  # session_name
    session_stop_requested = pyqtSignal()
    sync_flash_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._recording = False
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the session control UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("Session Control")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Session status
        self._status_label = QLabel("Ready")
        self._status_label.setStyleSheet("color: green; font-weight: bold;")
        layout.addWidget(self._status_label)

        # Recording timer
        self._timer_label = QLabel("00:00:00")
        self._timer_label.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        layout.addWidget(self._timer_label)

        # Control buttons
        button_layout = QHBoxLayout()
        self._start_btn = QPushButton("Start Recording")
        self._stop_btn = QPushButton("Stop Recording")
        self._sync_btn = QPushButton("Sync Flash")

        self._start_btn.clicked.connect(self._on_start_clicked)
        self._stop_btn.clicked.connect(self._on_stop_clicked)
        self._sync_btn.clicked.connect(self._on_sync_clicked)

        self._start_btn.setStyleSheet("background-color: green; color: white;")
        self._stop_btn.setStyleSheet("background-color: red; color: white;")
        self._sync_btn.setStyleSheet("background-color: blue; color: white;")

        button_layout.addWidget(self._start_btn)
        button_layout.addWidget(self._stop_btn)
        button_layout.addWidget(self._sync_btn)
        layout.addLayout(button_layout)

        # Initially disable stop button
        self._stop_btn.setEnabled(False)

        # Timer for updating recording duration
        self._recording_timer = QTimer()
        self._recording_timer.timeout.connect(self._update_timer)
        self._recording_start_time = None

    def _on_start_clicked(self):
        """Handle start recording button click."""
        session_name = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        self.session_start_requested.emit(session_name)

    def _on_stop_clicked(self):
        """Handle stop recording button click."""
        self.session_stop_requested.emit()

    def _on_sync_clicked(self):
        """Handle sync flash button click."""
        self.sync_flash_requested.emit()

    def set_recording_state(self, recording: bool):
        """Update UI to reflect recording state."""
        self._recording = recording

        if recording:
            self._status_label.setText("RECORDING")
            self._status_label.setStyleSheet("color: red; font-weight: bold;")
            self._start_btn.setEnabled(False)
            self._stop_btn.setEnabled(True)
            self._recording_start_time = datetime.now()
            self._recording_timer.start(1000)  # Update every second
        else:
            self._status_label.setText("Ready")
            self._status_label.setStyleSheet("color: green; font-weight: bold;")
            self._start_btn.setEnabled(True)
            self._stop_btn.setEnabled(False)
            self._recording_timer.stop()
            self._timer_label.setText("00:00:00")

    def _update_timer(self):
        """Update the recording timer display."""
        if self._recording_start_time:
            elapsed = datetime.now() - self._recording_start_time
            hours, remainder = divmod(elapsed.total_seconds(), 3600)
            minutes, seconds = divmod(remainder, 60)
            self._timer_label.setText(f"{int(hours):02d}:{int(minutes):02d}:{int(seconds):02d}")


class StatusDisplayWidget(QWidget):
    """Real-time status display with network and system metrics."""

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the status display UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("System Status")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Metrics grid
        metrics_layout = QGridLayout()

        # Network metrics
        self._network_status = QLabel("Disconnected")
        self._connected_devices = QLabel("0")
        self._data_rate = QLabel("0.0 KB/s")
        self._latency = QLabel("-- ms")
        
        self._network_status.setStyleSheet("color: red; font-weight: bold;")

        metrics_layout.addWidget(QLabel("Network Status:"), 0, 0)
        metrics_layout.addWidget(self._network_status, 0, 1)
        metrics_layout.addWidget(QLabel("Connected Devices:"), 1, 0)
        metrics_layout.addWidget(self._connected_devices, 1, 1)
        metrics_layout.addWidget(QLabel("Data Rate:"), 2, 0)
        metrics_layout.addWidget(self._data_rate, 2, 1)
        metrics_layout.addWidget(QLabel("Network Latency:"), 3, 0)
        metrics_layout.addWidget(self._latency, 3, 1)

        layout.addLayout(metrics_layout)

        # System resources
        self._cpu_progress = QProgressBar()
        self._memory_progress = QProgressBar()
        self._cpu_progress.setFormat("%p%")
        self._memory_progress.setFormat("%p%")

        layout.addWidget(QLabel("CPU Usage:"))
        layout.addWidget(self._cpu_progress)
        layout.addWidget(QLabel("Memory Usage:"))
        layout.addWidget(self._memory_progress)

        # Add spacer
        layout.addItem(QSpacerItem(20, 40, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Expanding))

    def update_system_metrics(self, cpu_percent: float, mem_percent: float):
        """Update system resource display."""
        self._cpu_progress.setValue(int(cpu_percent))
        self._memory_progress.setValue(int(mem_percent))

    def update_network_status(self, connected: bool, device_count: int, data_rate: float, latency: float):
        """Update network status display."""
        status_text = "Connected" if connected else "Disconnected"
        self._network_status.setText(status_text)
        self._network_status.setStyleSheet(f"color: {'green' if connected else 'red'}; font-weight: bold;")

        self._connected_devices.setText(f"{device_count}")
        self._data_rate.setText(f"{data_rate:.1f} KB/s")
        self._latency.setText(f"{latency:.1f} ms" if latency >= 0 else "-- ms")


class RealTimePlotWidget(QWidget):
    """Real-time plotting widget using PyQtGraph for sensor data visualization."""

    def __init__(self, title: str = "Real-time Data", max_samples: int = 1000):
        super().__init__()
        self._title = title
        self._max_samples = max_samples
        self._data_buffer = []
        self._time_buffer = []
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the plotting UI."""
        layout = QVBoxLayout(self)

        # Create plot widget
        self._plot_widget = pg.PlotWidget()
        self._plot_widget.setTitle(self._title, size="12pt")
        self._plot_widget.setLabel('left', 'Value')
        self._plot_widget.setLabel('bottom', 'Time (s)')
        self._plot_widget.showGrid(x=True, y=True)
        self._plot_widget.setBackground('w')

        # Create plot line
        pen = pg.mkPen(color=(0, 0, 255), width=2)
        self._plot_line = self._plot_widget.plot([], [], pen=pen)

        layout.addWidget(self._plot_widget)

    def add_data_point(self, value: float, timestamp: Optional[float] = None):
        """Add a new data point to the plot."""
        if timestamp is None:
            timestamp = datetime.now().timestamp()

        self._data_buffer.append(value)
        self._time_buffer.append(timestamp)

        # Keep only the most recent samples
        if len(self._data_buffer) > self._max_samples:
            self._data_buffer.pop(0)
            self._time_buffer.pop(0)

        # Update plot
        if self._time_buffer:
            # Convert to relative time (seconds from start)
            base_time = self._time_buffer[0]
            relative_times = [t - base_time for t in self._time_buffer]
            self._plot_line.setData(relative_times, self._data_buffer)

    def clear_data(self):
        """Clear all data from the plot."""
        self._data_buffer.clear()
        self._time_buffer.clear()
        self._plot_line.setData([], [])


class BluetoothControlWidget(QWidget):
    """Bluetooth control and management widget."""

    bluetooth_toggle_requested = pyqtSignal(bool)  # enable/disable
    device_scan_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._bluetooth_enabled = False
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the Bluetooth control UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("Bluetooth Control")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Status indicator
        self._status_label = QLabel("Bluetooth: Disabled")
        self._status_label.setStyleSheet("color: red;")
        layout.addWidget(self._status_label)

        # Control buttons
        button_layout = QHBoxLayout()
        self._toggle_btn = QPushButton("Enable Bluetooth")
        self._scan_btn = QPushButton("Scan Devices")

        self._toggle_btn.clicked.connect(self._on_toggle_clicked)
        self._scan_btn.clicked.connect(self._on_scan_clicked)

        button_layout.addWidget(self._toggle_btn)
        button_layout.addWidget(self._scan_btn)
        layout.addLayout(button_layout)

        # Initially disable scan button
        self._scan_btn.setEnabled(False)

    def _on_toggle_clicked(self):
        """Handle Bluetooth toggle button click."""
        new_state = not self._bluetooth_enabled
        self.bluetooth_toggle_requested.emit(new_state)

    def _on_scan_clicked(self):
        """Handle device scan button click."""
        self.device_scan_requested.emit()

    def set_bluetooth_state(self, enabled: bool):
        """Update Bluetooth state display."""
        self._bluetooth_enabled = enabled

        if enabled:
            self._status_label.setText("Bluetooth: Enabled")
            self._status_label.setStyleSheet("color: green;")
            self._toggle_btn.setText("Disable Bluetooth")
            self._scan_btn.setEnabled(True)
        else:
            self._status_label.setText("Bluetooth: Disabled")
            self._status_label.setStyleSheet("color: red;")
            self._toggle_btn.setText("Enable Bluetooth")
            self._scan_btn.setEnabled(False)


class WiFiControlWidget(QWidget):
    """WiFi control and monitoring widget."""

    wifi_toggle_requested = pyqtSignal(bool)  # enable/disable
    network_scan_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._wifi_enabled = True  # Usually enabled by default
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the WiFi control UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("WiFi Control")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Status and info
        self._status_label = QLabel("WiFi: Enabled")
        self._status_label.setStyleSheet("color: green;")
        layout.addWidget(self._status_label)

        self._network_label = QLabel("Network: Not connected")
        layout.addWidget(self._network_label)

        self._ip_label = QLabel("IP: ---.---.---.---")
        layout.addWidget(self._ip_label)

        # Control buttons
        button_layout = QHBoxLayout()
        self._toggle_btn = QPushButton("Disable WiFi")
        self._scan_btn = QPushButton("Scan Networks")

        self._toggle_btn.clicked.connect(self._on_toggle_clicked)
        self._scan_btn.clicked.connect(self._on_scan_clicked)

        button_layout.addWidget(self._toggle_btn)
        button_layout.addWidget(self._scan_btn)
        layout.addLayout(button_layout)

    def _on_toggle_clicked(self):
        """Handle WiFi toggle button click."""
        new_state = not self._wifi_enabled
        self.wifi_toggle_requested.emit(new_state)

    def _on_scan_clicked(self):
        """Handle network scan button click."""
        self.network_scan_requested.emit()

    def set_wifi_state(self, enabled: bool, network_name: str = "", ip_address: str = ""):
        """Update WiFi state display."""
        self._wifi_enabled = enabled

        if enabled:
            self._status_label.setText("WiFi: Enabled")
            self._status_label.setStyleSheet("color: green;")
            self._toggle_btn.setText("Disable WiFi")
            self._scan_btn.setEnabled(True)
        else:
            self._status_label.setText("WiFi: Disabled")
            self._status_label.setStyleSheet("color: red;")
            self._toggle_btn.setText("Enable WiFi")
            self._scan_btn.setEnabled(False)

        self._network_label.setText(f"Network: {network_name or 'Not connected'}")
        self._ip_label.setText(f"IP: {ip_address or '---.---.---.---'}")


class SystemIntegrationWidget(QWidget):
    """System integration and elevation control widget."""

    elevation_requested = pyqtSignal(str)  # operation_name
    system_info_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the system integration UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("System Integration")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Admin status
        self._admin_status = QLabel("Admin: Not elevated")
        self._admin_status.setStyleSheet("color: orange;")
        layout.addWidget(self._admin_status)

        # System info
        self._system_info = QTextEdit()
        self._system_info.setMaximumHeight(100)
        self._system_info.setReadOnly(True)
        layout.addWidget(self._system_info)

        # Control buttons
        button_layout = QHBoxLayout()
        self._elevate_btn = QPushButton("Request Admin")
        self._refresh_btn = QPushButton("Refresh Info")

        self._elevate_btn.clicked.connect(self._on_elevate_clicked)
        self._refresh_btn.clicked.connect(self._on_refresh_clicked)

        button_layout.addWidget(self._elevate_btn)
        button_layout.addWidget(self._refresh_btn)
        layout.addLayout(button_layout)

    def _on_elevate_clicked(self):
        """Handle admin elevation request."""
        self.elevation_requested.emit("system_admin")

    def _on_refresh_clicked(self):
        """Handle system info refresh."""
        self.system_info_requested.emit()

    def set_admin_status(self, elevated: bool):
        """Update admin elevation status."""
        if elevated:
            self._admin_status.setText("Admin: Elevated")
            self._admin_status.setStyleSheet("color: green;")
            self._elevate_btn.setText("Admin Active")
            self._elevate_btn.setEnabled(False)
        else:
            self._admin_status.setText("Admin: Not elevated")
            self._admin_status.setStyleSheet("color: orange;")
            self._elevate_btn.setText("Request Admin")
            self._elevate_btn.setEnabled(True)

    def update_system_info(self, info: str):
        """Update system information display."""
        self._system_info.setText(info)


class CalibrationUtilityWidget(QWidget):
    """Widget for camera calibration utilities."""
    
    start_calibration_requested = pyqtSignal()
    load_profile_requested = pyqtSignal()

    def __init__(self):
        super().__init__()
        self._setup_ui()

    def _setup_ui(self):
        """Initialize the calibration utility UI."""
        layout = QVBoxLayout(self)

        # Header
        header = QLabel("Calibration Utilities")
        header.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(header)

        # Status label
        self._status_label = QLabel("Status: No profile loaded")
        layout.addWidget(self._status_label)
        
        # Control buttons
        button_layout = QHBoxLayout()
        self._start_btn = QPushButton("Start Calibration")
        self._load_btn = QPushButton("Load Profile")
        
        self._start_btn.clicked.connect(self.start_calibration_requested)
        self._load_btn.clicked.connect(self.load_profile_requested)
        
        button_layout.addWidget(self._start_btn)
        button_layout.addWidget(self._load_btn)
        layout.addLayout(button_layout)
        
        layout.addItem(QSpacerItem(20, 40, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Expanding))

    def set_status(self, message: str, is_error: bool = False):
        """Update the status label for the user."""
        self._status_label.setText(f"Status: {message}")
        color = "red" if is_error else "black"
        self._status_label.setStyleSheet(f"color: {color};")