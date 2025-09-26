import numpy as np
import pyqtgraph as pg
import time
from PyQt6.QtCore import QTimer, pyqtSignal
from PyQt6.QtGui import QPixmap
from PyQt6.QtWidgets import QGridLayout, QLabel, QVBoxLayout, QWidget
from collections import deque
from typing import Any, Dict, List, Optional


class GSRPlotWidget(pg.PlotWidget):
    data_updated = pyqtSignal(float, float)

    def __init__(self, max_points: int = 10000, time_window: float = 30.0):

        super().__init__()

        self.max_points = max_points
        self.time_window = time_window

        self.gsr_data: Dict[str, deque] = (
            {}
        )
        self.plot_items: Dict[str, pg.PlotDataItem] = {}
        self.sync_markers: List[pg.InfiniteLine] = []

        self._setup_plot()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_plot)
        self.update_timer.start(50)

    def _setup_plot(self) -> None:

        self.setLabel("left", "GSR (µS)", color="white", size="12pt")
        self.setLabel("bottom", "Time (s)", color="white", size="12pt")
        self.setTitle("Real-time GSR Data", color="white", size="14pt")

        self.showGrid(x=True, y=True, alpha=0.3)
        self.setBackground("black")

        self.enableAutoRange(axis="y")
        self.setXRange(-self.time_window, 0)

        self.addLegend()

    def add_device(self, device_id: str, color: Optional[str] = None) -> None:

        if device_id in self.gsr_data:
            return

        self.gsr_data[device_id] = deque(maxlen=self.max_points)

        if color is None:
            colors = ["cyan", "yellow", "magenta", "green", "red", "blue"]
            color_idx = len(self.plot_items) % len(colors)
            color = colors[color_idx]

        plot_item = self.plot(
            pen=pg.mkPen(color=color, width=2), name=f"GSR {device_id}"
        )
        self.plot_items[device_id] = plot_item

        
    def remove_device(self, device_id: str) -> None:

        if device_id not in self.gsr_data:
            return

        if device_id in self.plot_items:
            self.removeItem(self.plot_items[device_id])
            del self.plot_items[device_id]

        del self.gsr_data[device_id]

        
    def add_gsr_data(
            self, device_id: str, timestamp_ns: int, gsr_microsiemens: float
    ) -> None:

        if device_id not in self.gsr_data:
            self.add_device(device_id)

        current_time = time.time()
        relative_time = (timestamp_ns / 1e9) - current_time

        self.gsr_data[device_id].append((relative_time, gsr_microsiemens))

        self.data_updated.emit(relative_time, gsr_microsiemens)

    def add_sync_marker(
            self, timestamp_ns: int, label: str = "Sync", color: str = "white"
    ) -> None:

        current_time = time.time()
        relative_time = (timestamp_ns / 1e9) - current_time

        marker = pg.InfiniteLine(
            pos=relative_time,
            angle=90,
            pen=pg.mkPen(color=color, width=2, style=2),
            label=label,
        )

        self.addItem(marker)
        self.sync_markers.append(marker)

        self._cleanup_old_markers()

    def _update_plot(self) -> None:

        time.time()

        for device_id, data_deque in self.gsr_data.items():
            if not data_deque or device_id not in self.plot_items:
                continue

            times = []
            values = []

            for timestamp, gsr_value in data_deque:
                relative_time = timestamp
                if relative_time >= -self.time_window:
                    times.append(relative_time)
                    values.append(gsr_value)

            if times and values:
                self.plot_items[device_id].setData(times, values)

        self.setXRange(-self.time_window, 0)

    def _cleanup_old_markers(self) -> None:

        time.time()

        markers_to_remove = []
        for marker in self.sync_markers:
            marker_time = marker.pos()[0]
            if marker_time < -self.time_window:
                markers_to_remove.append(marker)

        for marker in markers_to_remove:
            self.removeItem(marker)
            self.sync_markers.remove(marker)

    def clear_data(self) -> None:

        for device_id in list(self.gsr_data.keys()):
            self.remove_device(device_id)

        for marker in self.sync_markers:
            self.removeItem(marker)
        self.sync_markers.clear()


class VideoPreviewWidget(QLabel):
    frame_updated = pyqtSignal(int, int)

    def __init__(self, device_id: str, device_type: str = "RGB"):

        super().__init__()

        self.device_id = device_id
        self.device_type = device_type

        self.frame_count = 0
        self.last_fps_time = time.time()
        self.current_fps = 0.0

        self._setup_widget()

        self.fps_timer = QTimer()
        self.fps_timer.timeout.connect(self._calculate_fps)
        self.fps_timer.start(1000)

    def _setup_widget(self) -> None:

        self.setMinimumSize(320, 240)
        self.setStyleSheet(

        )

        self.setText(
            f"{self.device_type} Camera\n{self.device_id}\nWaiting for frames..."
        )

    def update_frame(self, frame_data: np.ndarray) -> None:

        if frame_data is None or frame_data.size == 0:
            return

        try:

            if len(frame_data.shape) == 3:
                height, width, channels = frame_data.shape
                if channels == 3:

                    rgb_frame = frame_data[:, :, ::-1]
                    pixmap = QPixmap.fromImage(
                        pg.makeQImage(rgb_frame, transpose=False)
                    )
                else:
                    pixmap = QPixmap.fromImage(
                        pg.makeQImage(frame_data, transpose=False)
                    )
            else:

                pixmap = QPixmap.fromImage(pg.makeQImage(frame_data, transpose=False))

            scaled_pixmap = pixmap.scaled(
                self.size(),
                aspectRatioMode=1,
                transformMode=1,
            )

            self.setPixmap(scaled_pixmap)

            self.frame_count += 1
            height, width = frame_data.shape[:2]
            self.frame_updated.emit(width, height)

        except Exception as e:
            
    def _calculate_fps(self) -> None:

        current_time = time.time()
        time_diff = current_time - self.last_fps_time

        if time_diff > 0:
            self.current_fps = self.frame_count / time_diff

        self.frame_count = 0
        self.last_fps_time = current_time

        self.setToolTip(
            f"{self.device_type} Camera {self.device_id}\\nFPS: {self.current_fps:.1f}"
        )

    def get_fps(self) -> float:

        return self.current_fps

    def set_status_text(self, text: str) -> None:

        self.setText(f"{self.device_type} Camera\\n{self.device_id}\\n{text}")


class MultiModalDashboard(QWidget):

    def __init__(self):

        super().__init__()

        self.gsr_plot = None
        self.video_widgets: Dict[str, VideoPreviewWidget] = {}

        self._setup_layout()

    def _setup_layout(self) -> None:

        self.layout = QGridLayout(self)

        self.gsr_plot = GSRPlotWidget()
        self.layout.addWidget(self.gsr_plot, 0, 0, 2, 2)

        self.video_row = 0
        self.video_col = 2

    def add_gsr_device(self, device_id: str, color: Optional[str] = None) -> None:

        if self.gsr_plot:
            self.gsr_plot.add_device(device_id, color)

    def add_gsr_data(
            self, device_id: str, timestamp_ns: int, gsr_microsiemens: float
    ) -> None:

        if self.gsr_plot:
            self.gsr_plot.add_gsr_data(device_id, timestamp_ns, gsr_microsiemens)

    def add_video_device(
            self, device_id: str, device_type: str = "RGB"
    ) -> VideoPreviewWidget:

        if device_id in self.video_widgets:
            return self.video_widgets[device_id]

        widget = VideoPreviewWidget(device_id, device_type)
        self.video_widgets[device_id] = widget

        self._add_video_widget_to_grid(widget)

        logger.info(f"Added video device {device_id} ({device_type})")
        return widget

    def remove_video_device(self, device_id: str) -> None:

        if device_id not in self.video_widgets:
            return

        widget = self.video_widgets[device_id]
        self.layout.removeWidget(widget)
        widget.deleteLater()

        del self.video_widgets[device_id]

        self._reorganize_video_grid()

        
    def _add_video_widget_to_grid(self, widget: VideoPreviewWidget) -> None:

        num_videos = len(self.video_widgets)

        if num_videos <= 4:
            grid_row = (num_videos - 1) // 2
            grid_col = (num_videos - 1) % 2
            self.layout.addWidget(widget, grid_row, self.video_col + grid_col)
        else:

            grid_row = num_videos - 1
            grid_col = 0
            self.layout.addWidget(widget, grid_row, self.video_col + grid_col)

    def _reorganize_video_grid(self) -> None:

        for widget in self.video_widgets.values():
            self.layout.removeWidget(widget)

        for i, widget in enumerate(self.video_widgets.values()):
            grid_row = i // 2
            grid_col = i % 2
            self.layout.addWidget(widget, grid_row, self.video_col + grid_col)

    def add_sync_marker(self, timestamp_ns: int, label: str = "Sync") -> None:

        if self.gsr_plot:
            self.gsr_plot.add_sync_marker(timestamp_ns, label)

    def clear_all_data(self) -> None:

        if self.gsr_plot:
            self.gsr_plot.clear_data()

        for widget in self.video_widgets.values():
            widget.set_status_text("Cleared")

    def get_device_fps(self, device_id: str) -> float:

        if device_id in self.video_widgets:
            return self.video_widgets[device_id].get_fps()
        return 0.0


class DataAggregationWidget(QWidget):

    def __init__(self):

        super().__init__()

        self.stats_labels: Dict[str, QLabel] = {}
        self._setup_layout()

    def _setup_layout(self) -> None:

        layout = QVBoxLayout(self)

        stats = [
            "Total Devices",
            "GSR Devices",
            "Video Devices",
            "Sync Quality",
            "Data Rate (MB/s)",
            "Buffer Usage (%)",
            "Dropped Frames",
            "Last Sync (s ago)",
        ]

        for stat in stats:
            label = QLabel(f"{stat}: --")
            label.setStyleSheet("color: white; font-size: 12px; padding: 2px;")
            self.stats_labels[stat] = label
            layout.addWidget(label)

    def update_stats(self, stats: Dict[str, Any]) -> None:

        for stat_name, value in stats.items():
            if stat_name in self.stats_labels:
                if isinstance(value, float):
                    self.stats_labels[stat_name].setText(f"{stat_name}: {value:.2f}")
                else:
                    self.stats_labels[stat_name].setText(f"{stat_name}: {value}")

    def set_sync_quality(self, quality_percent: float) -> None:

        label = self.stats_labels.get("Sync Quality")
        if label:
            if quality_percent >= 95:
                color = "green"
            elif quality_percent >= 85:
                color = "yellow"
            else:
                color = "red"

            label.setText(f"Sync Quality: {quality_percent:.1f}%")
            label.setStyleSheet(
                f"color: {color}; font-size: 12px; padding: 2px; font-weight: bold;"
            )
