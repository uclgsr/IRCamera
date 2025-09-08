"""
PyQt6-based GUI widgets for IRCamera PC Controller

This module contains all the user interface widgets for the PC Controller
application, providing device management, network control, and system
integration.
"""

from typing import Dict, List, Optional
from PyQt6.QtCore import pyqtSignal, QTimer
from PyQt6.QtWidgets import (
    QLabel, QWidget, QVBoxLayout, QHBoxLayout, QListWidget, QListWidgetItem,
    QPushButton, QProgressBar, QGroupBox, QTextEdit, QComboBox
)
from .plotting_widgets import MultiModalDashboard, DataAggregationWidget


class DeviceListWidget(QWidget):
    """Widget for displaying and managing connected devices."""
    
    device_selected = pyqtSignal(str)
    
    def __init__(self):
        super().__init__()
        self.devices: Dict[str, Dict] = {}
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up the device list UI."""
        layout = QVBoxLayout(self)
        
        self.device_list = QListWidget()
        self.device_list.itemClicked.connect(self._on_item_clicked)
        layout.addWidget(self.device_list)
        
        # Device status summary
        self.status_label = QLabel("No devices connected")
        layout.addWidget(self.status_label)
        
    def update_devices(self, devices: List[Dict]):
        """Update the device list with current devices."""
        # Clear current list
        self.device_list.clear()
        self.devices.clear()
        
        for device in devices:
            device_id = device.get('device_id', 'Unknown')
            device_type = device.get('device_type', 'Unknown')
            status = device.get('status', 'Unknown')
            
            self.devices[device_id] = device
            
            # Create list item
            item_text = f"{device_id} ({device_type}) - {status}"
            item = QListWidgetItem(item_text)
            
            # Color coding based on status
            if status == 'connected':
                item.setStyleSheet("color: green;")
            elif status == 'recording':
                item.setStyleSheet("color: cyan;")
            else:
                item.setStyleSheet("color: red;")
                
            self.device_list.addItem(item)
            
        # Update status summary
        count = len(devices)
        connected_count = sum(1 for d in devices if d.get('status') == 'connected')
        self.status_label.setText(f"{count} devices ({connected_count} connected)")
        
    def _on_item_clicked(self, item):
        """Handle device selection."""
        device_id = item.text().split(' ')[0]  # Extract device ID
        self.device_selected.emit(device_id)


class SessionControlWidget(QWidget):
    """Widget for session management and control."""
    
    start_session_requested = pyqtSignal()
    stop_session_requested = pyqtSignal()
    new_session_requested = pyqtSignal()
    
    def __init__(self):
        super().__init__()
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up the session control UI."""
        layout = QVBoxLayout(self)
        
        # Session buttons
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
        
        # Session info
        self.session_label = QLabel("No active session")
        layout.addWidget(self.session_label)
        
        # Recording timer
        self.timer_label = QLabel("00:00:00")
        self.timer_label.setStyleSheet("font-size: 16px; font-weight: bold;")
        layout.addWidget(self.timer_label)
        
    def update_state(self, session, has_devices):
        """Update widget state based on session and device status."""
        if session:
            self.session_label.setText(f"Session: {session.name}")
            
            if session.state == 'recording':
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
    """Widget for displaying system status and statistics."""
    
    def __init__(self):
        super().__init__()
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up the status display UI."""
        layout = QVBoxLayout(self)
        
        # Time sync status
        sync_group = QGroupBox("Time Synchronization")
        sync_layout = QVBoxLayout(sync_group)
        
        self.sync_quality_label = QLabel("Quality: --")
        self.sync_offset_label = QLabel("Max Offset: --")
        self.sync_devices_label = QLabel("Sync Devices: 0")
        
        sync_layout.addWidget(self.sync_quality_label)
        sync_layout.addWidget(self.sync_offset_label)
        sync_layout.addWidget(self.sync_devices_label)
        
        layout.addWidget(sync_group)
        
        # Session status
        session_group = QGroupBox("Session Status")
        session_layout = QVBoxLayout(session_group)
        
        self.session_name_label = QLabel("Name: --")
        self.session_duration_label = QLabel("Duration: --")
        self.session_data_size_label = QLabel("Data Size: --")
        
        session_layout.addWidget(self.session_name_label)
        session_layout.addWidget(self.session_duration_label)
        session_layout.addWidget(self.session_data_size_label)
        
        layout.addWidget(session_group)
        
        # Data aggregation widget
        self.data_aggregation = DataAggregationWidget()
        layout.addWidget(self.data_aggregation)
        
    def update_time_sync_stats(self, stats):
        """Update time synchronization statistics."""
        if stats:
            quality = stats.get('synchronization_rate', 0) * 100
            max_offset = stats.get('max_offset_ms', 0)
            device_count = stats.get('total_devices', 0)
            
            self.sync_quality_label.setText(f"Quality: {quality:.1f}%")
            self.sync_offset_label.setText(f"Max Offset: {max_offset:.1f}ms")
            self.sync_devices_label.setText(f"Sync Devices: {device_count}")
            
            # Update data aggregation widget
            self.data_aggregation.set_sync_quality(quality)
            
    def update_session_info(self, session):
        """Update session information."""
        if session:
            self.session_name_label.setText(f"Name: {session.name}")
            self.session_duration_label.setText(f"Duration: {session.duration_seconds:.1f}s")
            # Data size would need to be calculated based on actual data
            self.session_data_size_label.setText("Data Size: --")


class SystemIntegrationWidget(QWidget):
    """Widget for system integration controls."""
    
    elevation_requested = pyqtSignal(str)
    
    def __init__(self):
        super().__init__()
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up the system integration UI."""
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
        
    def update_privilege_level(self, level: str):
        """Update privilege level display."""
        self.privilege_label.setText(f"Privilege Level: {level}")
        
    def update_permissions(self, permissions: Dict):
        """Update permission status."""
        # Show permission status in tooltip or separate area
        perm_text = "\n".join([f"{k}: {'✓' if v else '✗'}" for k, v in permissions.items()])
        self.setToolTip(f"Permissions:\n{perm_text}")
        
    def set_status_message(self, message: str, is_error: bool = False):
        """Set status message."""
        self.status_label.setText(message)
        color = "red" if is_error else "green"
        self.status_label.setStyleSheet(f"color: {color};")


class BluetoothControlWidget(QWidget):
    """Widget for Bluetooth device management."""
    
    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str)
    disconnect_requested = pyqtSignal(str)
    
    def __init__(self):
        super().__init__()
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up Bluetooth control UI."""
        layout = QVBoxLayout(self)
        
        # Scan controls
        scan_layout = QHBoxLayout()
        self.scan_btn = QPushButton("Scan for Devices")
        self.scan_btn.clicked.connect(self.scan_requested.emit)
        scan_layout.addWidget(self.scan_btn)
        
        layout.addLayout(scan_layout)
        
        # Device list
        self.device_list = QListWidget()
        layout.addWidget(self.device_list)
        
        # Connection controls
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
        """Handle connect button click."""
        current_item = self.device_list.currentItem()
        if current_item:
            device_addr = current_item.text().split(' ')[0]
            self.connect_requested.emit(device_addr)
            
    def _on_disconnect_clicked(self):
        """Handle disconnect button click."""
        current_item = self.device_list.currentItem()
        if current_item:
            device_addr = current_item.text().split(' ')[0]
            self.disconnect_requested.emit(device_addr)
            
    def update_devices(self, devices):
        """Update discovered devices list."""
        self.device_list.clear()
        for device in devices:
            addr = device.get('address', 'Unknown')
            name = device.get('name', 'Unknown Device')
            self.device_list.addItem(f"{addr} - {name}")
            
    def set_connection_status(self, device_addr: str, connected: bool):
        """Update connection status for a device."""
        status = "Connected" if connected else "Disconnected"
        self.status_label.setText(f"{device_addr}: {status}")
        
    def set_error_status(self, error: str):
        """Set error status."""
        self.status_label.setText(f"Error: {error}")
        self.status_label.setStyleSheet("color: red;")


class WiFiControlWidget(QWidget):
    """Widget for WiFi network management."""
    
    scan_requested = pyqtSignal()
    connect_requested = pyqtSignal(str, str)  # ssid, password
    disconnect_requested = pyqtSignal()
    hotspot_start_requested = pyqtSignal(str, str, int)  # ssid, password, channel
    hotspot_stop_requested = pyqtSignal()
    
    def __init__(self):
        super().__init__()
        self._setup_ui()
        
    def _setup_ui(self):
        """Set up WiFi control UI."""
        layout = QVBoxLayout(self)
        
        # Network list
        self.network_list = QComboBox()
        layout.addWidget(self.network_list)
        
        # Scan button
        self.scan_btn = QPushButton("Scan Networks")
        self.scan_btn.clicked.connect(self.scan_requested.emit)
        layout.addWidget(self.scan_btn)
        
        # Connection controls
        self.connect_btn = QPushButton("Connect to Network")
        layout.addWidget(self.connect_btn)
        
        self.disconnect_btn = QPushButton("Disconnect")
        self.disconnect_btn.clicked.connect(self.disconnect_requested.emit)
        layout.addWidget(self.disconnect_btn)
        
        # Hotspot controls
        hotspot_group = QGroupBox("Mobile Hotspot")
        hotspot_layout = QVBoxLayout(hotspot_group)
        
        self.hotspot_start_btn = QPushButton("Start Hotspot")
        self.hotspot_stop_btn = QPushButton("Stop Hotspot")
        
        hotspot_layout.addWidget(self.hotspot_start_btn)
        hotspot_layout.addWidget(self.hotspot_stop_btn)
        
        layout.addWidget(hotspot_group)
        
        self.status_label = QLabel("Ready")
        layout.addWidget(self.status_label)
        
    def update_networks(self, networks):
        """Update available networks list."""
        self.network_list.clear()
        for network in networks:
            ssid = network.get('ssid', 'Unknown')
            signal = network.get('signal_strength', 0)
            self.network_list.addItem(f"{ssid} ({signal}%)")
            
    def set_connection_status(self, ssid: str, connected: bool, ip: str = ""):
        """Update connection status."""
        if connected:
            self.status_label.setText(f"Connected to {ssid} ({ip})")
        else:
            self.status_label.setText(f"Disconnected from {ssid}")
            
    def set_hotspot_status(self, active: bool, message: str = ""):
        """Update hotspot status."""
        status = "Active" if active else "Inactive"
        self.status_label.setText(f"Hotspot: {status} {message}")
        
    def set_error_status(self, error: str):
        """Set error status."""
        self.status_label.setText(f"Error: {error}")
        self.status_label.setStyleSheet("color: red;")
        super().__init__()
        self._label = QLabel(
            "Bluetooth control temporarily unavailable - GUI under repair"
        )


class WiFiControlWidget(QWidget):
    """Simple placeholder WiFi control widget."""

    def __init__(self):
        super().__init__()
        self._label = QLabel("WiFi control temporarily unavailable - GUI under repair")


class SystemIntegrationWidget(QWidget):
    """Simple placeholder system integration widget."""

    elevation_requested = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self._label = QLabel(
            "System integration temporarily unavailable - GUI under repair"
        )
