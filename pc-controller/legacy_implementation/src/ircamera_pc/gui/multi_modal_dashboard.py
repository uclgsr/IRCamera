import numpy as np
import pyqtgraph as pg
import time
from PyQt6.QtCore import Qt, QTimer, pyqtSignal
from PyQt6.QtGui import QPixmap, QImage
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QGridLayout,
    QGroupBox, QLabel, QPushButton, QScrollArea, QSplitter,
    QFrame, QComboBox, QMessageBox, QInputDialog, QStatusBar
)
from dataclasses import dataclass
from datetime import datetime
from loguru import logger
from pyqtgraph import PlotWidget, ImageView
from typing import Dict, Optional, Tuple

from ..core.session import SessionManager
from ..core.timesync import TimeSyncService
from ..data.aggregator import DataAggregator
from ..data.hdf5_exporter import MultiModalHDF5Exporter
from ..network.websocket_server import WebSocketServer

pg.setConfigOptions(antialias=True, useOpenGL=True)


@dataclass
class DeviceStatus:
    device_id: str
    device_name: str
    android_version: str
    connection_time: datetime
    last_heartbeat: datetime
    is_recording: bool
    battery_level: int
    temperature: float

    rgb_camera_active: bool
    thermal_camera_active: bool
    gsr_sensor_active: bool

    network_latency_ms: float
    sync_offset_ms: float
    data_quality_score: float

    is_samsung_s22: bool = False
    thermal_throttling: bool = False


@dataclass
class SensorData:
    timestamp: float
    device_id: str
    sensor_type: str
    data: np.ndarray
    metadata: Dict


class GSRPlotWidget(PlotWidget):

    def __init__(self, parent=None, update_rate: int = 100):
        super().__init__(parent)
        self.update_rate = update_rate
        self.buffer_size = update_rate * 30

        self.device_buffers: Dict[str, Tuple[np.ndarray, np.ndarray]] = {}
        self.device_curves: Dict[str, pg.PlotCurveItem] = {}
        self.colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD']

        self.setup_plot()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_plots)
        self.update_timer.start(1000 // update_rate)

    def setup_plot(self):

        self.setLabel('left', 'GSR (μS)', color='white', size='12pt')
        self.setLabel('bottom', 'Time (seconds)', color='white', size='12pt')
        self.setTitle('Real-Time GSR Data (100Hz)', color='white', size='14pt')

        self.setBackground('black')
        self.showGrid(x=True, y=True, alpha=0.3)

        self.enableAutoRange('y', True)
        self.setXRange(-30, 0)
        self.setYRange(0, 50)

        self.addLegend()

    def add_device(self, device_id: str, device_name: str):

        if device_id not in self.device_buffers:
            time_buffer = np.zeros(self.buffer_size)
            gsr_buffer = np.zeros(self.buffer_size)
            self.device_buffers[device_id] = (time_buffer, gsr_buffer)

            color = self.colors[len(self.device_curves) % len(self.colors)]
            curve = self.plot(time_buffer, gsr_buffer, pen=color, name=device_name)
            self.device_curves[device_id] = curve

    def remove_device(self, device_id: str):

        if device_id in self.device_buffers:
            del self.device_buffers[device_id]

        if device_id in self.device_curves:
            self.removeItem(self.device_curves[device_id])
            del self.device_curves[device_id]

    def add_gsr_data(self, device_id: str, timestamp: float, gsr_value: float):

        if device_id not in self.device_buffers:
            return

        time_buffer, gsr_buffer = self.device_buffers[device_id]

        time_buffer[:-1] = time_buffer[1:]
        gsr_buffer[:-1] = gsr_buffer[1:]

        current_time = time.time()
        relative_time = timestamp - current_time

        time_buffer[-1] = relative_time
        gsr_buffer[-1] = gsr_value

    def update_plots(self):

        for device_id, curve in self.device_curves.items():
            if device_id in self.device_buffers:
                time_buffer, gsr_buffer = self.device_buffers[device_id]
                curve.setData(time_buffer, gsr_buffer)


class ThermalVideoWidget(ImageView):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.device_id: Optional[str] = None
        self.temperature_range = (20.0, 40.0)

        self.setup_thermal_display()

    def setup_thermal_display(self):

        colormap = pg.colormap.get('thermal')
        self.setColorMap(colormap)

        self.ui.histogram.hide()
        self.ui.roiBtn.hide()
        self.ui.menuBtn.hide()

        self.setLevels(*self.temperature_range)

    def set_device(self, device_id: str, device_name: str):

        self.device_id = device_id

    def update_thermal_frame(self, thermal_data: np.ndarray,
                             temperature_range: Tuple[float, float]):

        if thermal_data is not None:

            if temperature_range != self.temperature_range:
                self.temperature_range = temperature_range
                self.setLevels(*temperature_range)

            self.setImage(thermal_data, autoRange=False, autoLevels=False)


class RGBVideoWidget(QLabel):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.device_id: Optional[str] = None
        self.frame_count = 0
        self.fps_timer = QTimer()
        self.fps_timer.timeout.connect(self.calculate_fps)
        self.fps_timer.start(1000)

        self.last_fps_time = time.time()
        self.current_fps = 0.0

        self.setup_display()

    def setup_display(self):

        self.setMinimumSize(320, 240)
        self.setScaledContents(True)
        self.setStyleSheet("""
            QLabel {
                border: 2px solid #555;
                background-color: black;
                color: white;
            }
        """)

        self.setText("RGB Camera\nNo Device Connected")
        self.setAlignment(Qt.AlignmentFlag.AlignCenter)

    def set_device(self, device_id: str, device_name: str):

        self.device_id = device_id
        self.setText(f"RGB Camera\n{device_name}\nConnecting...")

    def update_rgb_frame(self, rgb_data: np.ndarray):

        if rgb_data is not None:
            height, width, channels = rgb_data.shape
            bytes_per_line = channels * width

            q_image = QImage(rgb_data.data, width, height, bytes_per_line,
                             QImage.Format.Format_RGB888)
            pixmap = QPixmap.fromImage(q_image)

            self.setPixmap(pixmap)
            self.frame_count += 1

    def calculate_fps(self):

        current_time = time.time()
        time_diff = current_time - self.last_fps_time

        if time_diff > 0:
            self.current_fps = self.frame_count / time_diff

        self.frame_count = 0
        self.last_fps_time = current_time


class DeviceStatusWidget(QGroupBox):

    def __init__(self, parent=None):
        super().__init__("Connected Devices", parent)
        self.device_status_widgets: Dict[str, QWidget] = {}

        self.setup_ui()

    def setup_ui(self):

        self.layout = QVBoxLayout(self)

        self.scroll_area = QScrollArea()
        self.scroll_widget = QWidget()
        self.scroll_layout = QVBoxLayout(self.scroll_widget)

        self.scroll_area.setWidget(self.scroll_widget)
        self.scroll_area.setWidgetResizable(True)

        self.layout.addWidget(self.scroll_area)

        self.no_devices_label = QLabel("No devices connected")
        self.no_devices_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.scroll_layout.addWidget(self.no_devices_label)

    def add_device(self, device_status: DeviceStatus):

        device_id = device_status.device_id

        if self.no_devices_label.isVisible():
            self.no_devices_label.hide()

        device_widget = self.create_device_widget(device_status)
        self.device_status_widgets[device_id] = device_widget
        self.scroll_layout.addWidget(device_widget)

    def remove_device(self, device_id: str):

        if device_id in self.device_status_widgets:
            widget = self.device_status_widgets[device_id]
            self.scroll_layout.removeWidget(widget)
            widget.deleteLater()
            del self.device_status_widgets[device_id]

        if not self.device_status_widgets:
            self.no_devices_label.show()

    def create_device_widget(self, status: DeviceStatus) -> QWidget:

        widget = QFrame()
        widget.setFrameStyle(QFrame.Shape.StyledPanel)
        widget.setStyleSheet("""
            QFrame {
                border: 2px solid #444;
                border-radius: 8px;
                padding: 8px;
                margin: 4px;
            }
        """)

        layout = QGridLayout(widget)

        device_label = QLabel(f"<b>{status.device_name}</b>")
        if status.is_samsung_s22:
            device_label.setText(
                f"<b>{status.device_name}</b> <span style='color: #4ECDC4;'>[Samsung S22]</span>")
        layout.addWidget(device_label, 0, 0, 1, 2)

        layout.addWidget(QLabel(f"ID: {status.device_id}"), 1, 0)
        layout.addWidget(QLabel(f"Android: {status.android_version}"), 1, 1)

        battery_label = QLabel(f"Battery: {status.battery_level}%")
        if status.battery_level < 20:
            battery_label.setStyleSheet("color: #FF6B6B;")
        elif status.battery_level < 50:
            battery_label.setStyleSheet("color: #FFEAA7;")
        else:
            battery_label.setStyleSheet("color: #96CEB4;")
        layout.addWidget(battery_label, 2, 0)

        temp_label = QLabel(f"Temp: {status.temperature:.1f}°C")
        if status.temperature > 45:
            temp_label.setStyleSheet("color: #FF6B6B;")
        layout.addWidget(temp_label, 2, 1)

        sensor_frame = QFrame()
        sensor_layout = QHBoxLayout(sensor_frame)

        rgb_indicator = self.create_status_indicator("RGB", status.rgb_camera_active)
        thermal_indicator = self.create_status_indicator("Thermal", status.thermal_camera_active)
        gsr_indicator = self.create_status_indicator("GSR", status.gsr_sensor_active)

        sensor_layout.addWidget(rgb_indicator)
        sensor_layout.addWidget(thermal_indicator)
        sensor_layout.addWidget(gsr_indicator)

        layout.addWidget(sensor_frame, 3, 0, 1, 2)

        layout.addWidget(QLabel(f"Latency: {status.network_latency_ms:.1f}ms"), 4, 0)
        layout.addWidget(QLabel(f"Sync: ±{status.sync_offset_ms:.1f}ms"), 4, 1)

        quality_label = QLabel(f"Quality: {status.data_quality_score:.1f}%")
        if status.data_quality_score >= 95:
            quality_label.setStyleSheet("color: #96CEB4;")
        elif status.data_quality_score >= 85:
            quality_label.setStyleSheet("color: #FFEAA7;")
        else:
            quality_label.setStyleSheet("color: #FF6B6B;")
        layout.addWidget(quality_label, 5, 0, 1, 2)

        if status.is_samsung_s22 and status.thermal_throttling:
            warning_label = QLabel("⚠️ Thermal throttling active")
            warning_label.setStyleSheet("color: #FF6B6B;")
            layout.addWidget(warning_label, 6, 0, 1, 2)

        return widget

    def create_status_indicator(self, name: str, active: bool) -> QWidget:

        widget = QFrame()
        layout = QVBoxLayout(widget)
        layout.setContentsMargins(4, 4, 4, 4)

        status_label = QLabel("●")
        status_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        if active:
            status_label.setStyleSheet("color: #96CEB4; font-size: 16px;")
        else:
            status_label.setStyleSheet("color: #555; font-size: 16px;")

        name_label = QLabel(name)
        name_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        name_label.setStyleSheet("font-size: 10px;")

        layout.addWidget(status_label)
        layout.addWidget(name_label)

        return widget

    def update_device_status(self, device_status: DeviceStatus):

        device_id = device_status.device_id
        if device_id in self.device_status_widgets:
            self.remove_device(device_id)
            self.add_device(device_status)


class SessionControlPanel(QGroupBox):
    start_recording_signal = pyqtSignal(str)
    stop_recording_signal = pyqtSignal()
    inject_sync_marker_signal = pyqtSignal(str)

    def __init__(self, parent=None):
        super().__init__("Session Control", parent)
        self.is_recording = False
        self.session_start_time: Optional[datetime] = None

        self.setup_ui()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_recording_status)
        self.update_timer.start(1000)

    def setup_ui(self):

        layout = QVBoxLayout(self)

        participant_layout = QHBoxLayout()
        participant_layout.addWidget(QLabel("Participant ID:"))
        self.participant_id_input = QComboBox()
        self.participant_id_input.setEditable(True)
        self.participant_id_input.addItems(["P001", "P002", "P003"])
        participant_layout.addWidget(self.participant_id_input)
        layout.addLayout(participant_layout)

        control_layout = QHBoxLayout()

        self.start_button = QPushButton("Start Recording")
        self.start_button.setStyleSheet("""
            QPushButton {
                background-color: #96CEB4;
                color: black;
                font-weight: bold;
                padding: 8px;
                border-radius: 4px;
            }
            QPushButton:hover {
                background-color: #7FB69A;
            }
        """)
        self.start_button.clicked.connect(self.start_recording)

        self.stop_button = QPushButton("Stop Recording")
        self.stop_button.setStyleSheet("""
            QPushButton {
                background-color: #FF6B6B;
                color: white;
                font-weight: bold;
                padding: 8px;
                border-radius: 4px;
            }
            QPushButton:hover {
                background-color: #E85A5A;
            }
        """)
        self.stop_button.clicked.connect(self.stop_recording)
        self.stop_button.setEnabled(False)

        control_layout.addWidget(self.start_button)
        control_layout.addWidget(self.stop_button)
        layout.addLayout(control_layout)

        sync_layout = QHBoxLayout()
        sync_layout.addWidget(QLabel("Sync Markers:"))

        self.flash_sync_button = QPushButton("Flash Sync")
        self.flash_sync_button.clicked.connect(lambda: self.inject_sync_marker("flash"))

        self.audio_sync_button = QPushButton("Audio Beep")
        self.audio_sync_button.clicked.connect(lambda: self.inject_sync_marker("audio"))

        self.custom_sync_button = QPushButton("Custom Event")
        self.custom_sync_button.clicked.connect(self.inject_custom_marker)

        sync_layout.addWidget(self.flash_sync_button)
        sync_layout.addWidget(self.audio_sync_button)
        sync_layout.addWidget(self.custom_sync_button)
        layout.addLayout(sync_layout)

        self.status_label = QLabel("Ready to record")
        self.status_label.setStyleSheet("font-weight: bold; color: #96CEB4;")
        layout.addWidget(self.status_label)

        self.duration_label = QLabel("Duration: 00:00:00")
        layout.addWidget(self.duration_label)

    def start_recording(self):

        participant_id = self.participant_id_input.currentText().strip()
        if not participant_id:
            QMessageBox.warning(self, "Invalid Input", "Please enter a valid Participant ID")
            return

        self.is_recording = True
        self.session_start_time = datetime.now()

        self.start_button.setEnabled(False)
        self.stop_button.setEnabled(True)
        self.participant_id_input.setEnabled(False)

        self.status_label.setText("🔴 RECORDING")
        self.status_label.setStyleSheet("font-weight: bold; color: #FF6B6B;")

        self.flash_sync_button.setEnabled(True)
        self.audio_sync_button.setEnabled(True)
        self.custom_sync_button.setEnabled(True)

        self.start_recording_signal.emit(participant_id)

    def stop_recording(self):

        self.is_recording = False
        self.session_start_time = None

        self.start_button.setEnabled(True)
        self.stop_button.setEnabled(False)
        self.participant_id_input.setEnabled(True)

        self.status_label.setText("Processing data...")
        self.status_label.setStyleSheet("font-weight: bold; color: #FFEAA7;")

        self.flash_sync_button.setEnabled(False)
        self.audio_sync_button.setEnabled(False)
        self.custom_sync_button.setEnabled(False)

        self.stop_recording_signal.emit()

        QTimer.singleShot(3000, self.reset_status)

    def reset_status(self):

        self.status_label.setText("Ready to record")
        self.status_label.setStyleSheet("font-weight: bold; color: #96CEB4;")
        self.duration_label.setText("Duration: 00:00:00")

    def inject_sync_marker(self, marker_type: str):

        if self.is_recording:
            self.inject_sync_marker_signal.emit(marker_type)

    def inject_custom_marker(self):

        if self.is_recording:
            text, ok = QInputDialog.getText(self, "Custom Sync Marker", "Enter marker description:")
            if ok and text.strip():
                self.inject_sync_marker_signal.emit(f"custom:{text.strip()}")

    def update_recording_status(self):

        if self.is_recording and self.session_start_time:
            elapsed = datetime.now() - self.session_start_time
            hours, remainder = divmod(elapsed.total_seconds(), 3600)
            minutes, seconds = divmod(remainder, 60)

            duration_str = f"Duration: {int(hours):02d}:{int(minutes):02d}:{int(seconds):02d}"
            self.duration_label.setText(duration_str)


class MultiModalDashboard(QMainWindow):

    def __init__(self):
        super().__init__()

        self.session_manager: Optional[SessionManager] = None
        self.websocket_server: Optional[WebSocketServer] = None
        self.time_sync_service: Optional[TimeSyncService] = None
        self.data_aggregator: Optional[DataAggregator] = None
        self.hdf5_exporter: Optional[MultiModalHDF5Exporter] = None

        self.connected_devices: Dict[str, DeviceStatus] = {}

        self.gsr_plot_widget: Optional[GSRPlotWidget] = None
        self.thermal_video_widget: Optional[ThermalVideoWidget] = None
        self.rgb_video_widget: Optional[RGBVideoWidget] = None
        self.device_status_widget: Optional[DeviceStatusWidget] = None
        self.session_control_panel: Optional[SessionControlPanel] = None

        self.setup_ui()
        self.setup_services()
        self.connect_signals()

        self.showMaximized()

    def setup_ui(self):

        self.setWindowTitle("Multi-Modal Physiological Sensing Platform - PC Controller")
        self.setMinimumSize(1400, 900)

        self.setStyleSheet("""
            QMainWindow {
                background-color: #2b2b2b;
                color: white;
            }
            QGroupBox {
                font-weight: bold;
                border: 2px solid #555;
                border-radius: 5px;
                margin: 5px;
                padding-top: 10px;
            }
            QGroupBox::title {
                subcontrol-origin: margin;
                left: 10px;
                padding: 0 5px 0 5px;
            }
        """)

        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        main_layout = QHBoxLayout(central_widget)

        left_panel = QWidget()
        left_panel.setFixedWidth(350)
        left_layout = QVBoxLayout(left_panel)

        self.device_status_widget = DeviceStatusWidget()
        left_layout.addWidget(self.device_status_widget)

        self.session_control_panel = SessionControlPanel()
        left_layout.addWidget(self.session_control_panel)

        main_layout.addWidget(left_panel)

        right_panel = QWidget()
        right_layout = QVBoxLayout(right_panel)

        camera_splitter = QSplitter(Qt.Orientation.Horizontal)

        rgb_group = QGroupBox("RGB Camera Feed")
        rgb_layout = QVBoxLayout(rgb_group)
        self.rgb_video_widget = RGBVideoWidget()
        rgb_layout.addWidget(self.rgb_video_widget)
        camera_splitter.addWidget(rgb_group)

        thermal_group = QGroupBox("Thermal Camera Feed")
        thermal_layout = QVBoxLayout(thermal_group)
        self.thermal_video_widget = ThermalVideoWidget()
        thermal_layout.addWidget(self.thermal_video_widget)
        camera_splitter.addWidget(thermal_group)

        camera_splitter.setSizes([500, 500])
        right_layout.addWidget(camera_splitter)

        gsr_group = QGroupBox("Real-Time GSR Data (100Hz)")
        gsr_layout = QVBoxLayout(gsr_group)
        self.gsr_plot_widget = GSRPlotWidget(update_rate=100)
        gsr_layout.addWidget(self.gsr_plot_widget)
        right_layout.addWidget(gsr_group)

        right_layout.setStretchFactor(camera_splitter, 1)
        right_layout.setStretchFactor(gsr_group, 1)

        main_layout.addWidget(right_panel)
        main_layout.setStretchFactor(right_panel, 1)

        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready - Waiting for device connections")

    def setup_services(self):

        try:

            self.session_manager = SessionManager()

            self.time_sync_service = TimeSyncService()

            self.data_aggregator = DataAggregator()

            self.websocket_server = WebSocketServer(port=8080)

            logger.info("All core services initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize services: {e}")
            QMessageBox.critical(self, "Initialization Error", f"Failed to start services: {e}")

    def connect_signals(self):

        if self.session_control_panel:
            self.session_control_panel.start_recording_signal.connect(self.on_start_recording)
            self.session_control_panel.stop_recording_signal.connect(self.on_stop_recording)
            self.session_control_panel.inject_sync_marker_signal.connect(self.on_inject_sync_marker)

    def on_device_connected(self, device_info: Dict):

        device_id = device_info.get('device_id')
        device_name = device_info.get('device_name', 'Unknown Device')

        device_status = DeviceStatus(
            device_id=device_id,
            device_name=device_name,
            android_version=device_info.get('android_version', 'Unknown'),
            connection_time=datetime.now(),
            last_heartbeat=datetime.now(),
            is_recording=False,
            battery_level=device_info.get('battery_level', 100),
            temperature=device_info.get('temperature', 25.0),
            rgb_camera_active=False,
            thermal_camera_active=False,
            gsr_sensor_active=False,
            network_latency_ms=0.0,
            sync_offset_ms=0.0,
            data_quality_score=100.0,
            is_samsung_s22='s22' in device_name.lower() or 'SM-S90' in device_info.get('model', ''),
            thermal_throttling=False
        )

        self.connected_devices[device_id] = device_status

        self.device_status_widget.add_device(device_status)
        self.gsr_plot_widget.add_device(device_id, device_name)

        if device_id not in [self.rgb_video_widget.device_id, self.thermal_video_widget.device_id]:
            if not self.rgb_video_widget.device_id:
                self.rgb_video_widget.set_device(device_id, device_name)
            elif not self.thermal_video_widget.device_id:
                self.thermal_video_widget.set_device(device_id, device_name)

        self.status_bar.showMessage(f"Device connected: {device_name} ({device_id})")
        logger.info(f"Device connected: {device_name} ({device_id})")

    def on_device_disconnected(self, device_id: str):

        if device_id in self.connected_devices:
            device_name = self.connected_devices[device_id].device_name

            del self.connected_devices[device_id]

            self.device_status_widget.remove_device(device_id)
            self.gsr_plot_widget.remove_device(device_id)

            if self.rgb_video_widget.device_id == device_id:
                self.rgb_video_widget.set_device(None, "")
                self.rgb_video_widget.setText("RGB Camera\nNo Device Connected")

            if self.thermal_video_widget.device_id == device_id:
                self.thermal_video_widget.set_device(None, "")

            self.status_bar.showMessage(f"Device disconnected: {device_name}")
            logger.info(f"Device disconnected: {device_name} ({device_id})")

    def on_sensor_data_received(self, sensor_data: SensorData):

        device_id = sensor_data.device_id
        sensor_type = sensor_data.sensor_type

        if device_id in self.connected_devices:

            self.connected_devices[device_id].last_heartbeat = datetime.now()

            if sensor_type == 'gsr':
                gsr_value = float(sensor_data.data)
                timestamp = sensor_data.timestamp
                self.gsr_plot_widget.add_gsr_data(device_id, timestamp, gsr_value)

                self.connected_devices[device_id].gsr_sensor_active = True

            elif sensor_type == 'thermal':
                if self.thermal_video_widget.device_id == device_id:
                    temperature_range = sensor_data.metadata.get('temperature_range', (20.0, 40.0))
                    self.thermal_video_widget.update_thermal_frame(sensor_data.data,
                                                                   temperature_range)

                self.connected_devices[device_id].thermal_camera_active = True

            elif sensor_type == 'rgb':
                if self.rgb_video_widget.device_id == device_id:
                    self.rgb_video_widget.update_rgb_frame(sensor_data.data)

                self.connected_devices[device_id].rgb_camera_active = True

            self.device_status_widget.update_device_status(self.connected_devices[device_id])

    def on_start_recording(self, participant_id: str):

        try:
            if self.session_manager:
                session_id = self.session_manager.start_session(participant_id)

            self.hdf5_exporter = MultiModalHDF5Exporter(session_id)

            for device_id in self.connected_devices:
                self.send_recording_command(device_id, "start", {"participant_id": participant_id})
                self.connected_devices[device_id].is_recording = True

            self.status_bar.showMessage(f"Recording started for participant: {participant_id}")
            logger.info(f"Recording session started: {participant_id}")

        except Exception as e:
            logger.error(f"Failed to start recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to start recording: {e}")

    def on_stop_recording(self):

        try:

            for device_id in self.connected_devices:
                self.send_recording_command(device_id, "stop", {})
                self.connected_devices[device_id].is_recording = False

            if self.session_manager:
                self.session_manager.stop_session()

            if self.hdf5_exporter:
                export_path = self.hdf5_exporter.finalize_export()
                self.status_bar.showMessage(f"Recording stopped. Data exported to: {export_path}")

            logger.info("Recording session stopped")

        except Exception as e:
            logger.error(f"Failed to stop recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to stop recording: {e}")

    def on_inject_sync_marker(self, marker_type: str):

        try:
            timestamp = time.time()

            for device_id in self.connected_devices:
                if self.connected_devices[device_id].is_recording:
                    self.send_sync_marker(device_id, marker_type, timestamp)

            self.status_bar.showMessage(f"Sync marker injected: {marker_type}")
            logger.info(f"Sync marker injected: {marker_type} at {timestamp}")

        except Exception as e:
            logger.error(f"Failed to inject sync marker: {e}")

    def send_recording_command(self, device_id: str, command: str, parameters: Dict):

        if self.websocket_server:
            message = {
                'type': 'recording_command',
                'command': command,
                'parameters': parameters,
                'timestamp': time.time()
            }

    def send_sync_marker(self, device_id: str, marker_type: str, timestamp: float):

        if self.websocket_server:
            message = {
                'type': 'sync_marker',
                'marker_type': marker_type,
                'timestamp': timestamp
            }

    def closeEvent(self, event):

        if self.session_control_panel and self.session_control_panel.is_recording:
            self.on_stop_recording()

        if self.websocket_server:
            self.websocket_server.stop()

        logger.info("Multi-Modal Dashboard closed")
        event.accept()


def main():
    import sys
    from PyQt6.QtWidgets import QApplication

    app = QApplication(sys.argv)
    app.setApplicationName("Multi-Modal Physiological Sensing Platform")
    app.setApplicationVersion("3.0.0")
    app.setOrganizationName("Research Lab")

    dashboard = MultiModalDashboard()
    dashboard.show()

    return app.exec()


if __name__ == "__main__":
    sys.exit(main())
