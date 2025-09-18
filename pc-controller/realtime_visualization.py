#!/usr/bin/env python3
"""
Real-time Data Visualization for PC Controller
Implements PyQtGraph-based real-time plotting for GSR, thermal, and other sensor data
"""

import json
import numpy as np
import time
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any, Callable

try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout,
        QWidget, QPushButton, QLabel, QGroupBox, QGridLayout,
        QTabWidget, QTextEdit, QComboBox, QSpinBox, QCheckBox
    )
    from PyQt6.QtCore import QThread, pyqtSignal, QTimer, Qt
    from PyQt6.QtGui import QFont, QPixmap, QImage
    
    import pyqtgraph as pg
    from pyqtgraph import PlotWidget, ImageView
    
    GUI_AVAILABLE = True
    
except ImportError as e:
    print(f"GUI libraries not available: {e}")
    GUI_AVAILABLE = False

try:
    import cv2
    OPENCV_AVAILABLE = True
except ImportError:
    OPENCV_AVAILABLE = False

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


@dataclass
class SensorData:
    """Container for sensor data points"""
    timestamp: float
    value: float
    device_id: str
    sensor_type: str
    metadata: Dict[str, Any] = field(default_factory=dict)


class RealTimePlot(QWidget):
    """Real-time plot widget for sensor data"""
    
    def __init__(self, title: str = "Real-time Data", max_points: int = 1000):
        super().__init__()
        self.title = title
        self.max_points = max_points
        
        # Data storage
        self.data_buffers: Dict[str, deque] = {}  # device_id -> deque of SensorData
        self.plot_curves: Dict[str, Any] = {}     # device_id -> PlotDataItem
        self.colors = ['r', 'g', 'b', 'c', 'm', 'y', 'w']
        self.color_index = 0
        
        # Setup UI
        self._setup_ui()
        
        # Update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_plot)
        self.update_timer.start(50)  # 20 Hz update rate
        
        # Statistics
        self.total_points = 0
        self.last_update = time.time()
    
    def _setup_ui(self):
        """Setup the plot UI"""
        layout = QVBoxLayout(self)
        
        # Plot widget
        self.plot_widget = PlotWidget(title=self.title)
        self.plot_widget.setLabel('left', 'Value')
        self.plot_widget.setLabel('bottom', 'Time (s)')
        self.plot_widget.showGrid(x=True, y=True)
        self.plot_widget.addLegend()
        
        layout.addWidget(self.plot_widget)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        # Auto-scale checkbox
        self.auto_scale_cb = QCheckBox("Auto Scale")
        self.auto_scale_cb.setChecked(True)
        controls_layout.addWidget(self.auto_scale_cb)
        
        # Time window control
        controls_layout.addWidget(QLabel("Time Window (s):"))
        self.time_window_spin = QSpinBox()
        self.time_window_spin.setRange(10, 300)
        self.time_window_spin.setValue(60)
        controls_layout.addWidget(self.time_window_spin)
        
        # Clear button
        self.clear_btn = QPushButton("Clear")
        self.clear_btn.clicked.connect(self.clear_data)
        controls_layout.addWidget(self.clear_btn)
        
        # Statistics label
        self.stats_label = QLabel("Points: 0")
        controls_layout.addWidget(self.stats_label)
        
        controls_layout.addStretch()
        layout.addLayout(controls_layout)
    
    def add_data_point(self, device_id: str, timestamp: float, value: float, 
                      sensor_type: str = "unknown", metadata: Dict[str, Any] = None):
        """Add a data point to the plot"""
        
        # Create data point
        data_point = SensorData(
            timestamp=timestamp,
            value=value,
            device_id=device_id,
            sensor_type=sensor_type,
            metadata=metadata or {}
        )
        
        # Initialize buffer if needed
        if device_id not in self.data_buffers:
            self.data_buffers[device_id] = deque(maxlen=self.max_points)
            
            # Create plot curve
            color = self.colors[self.color_index % len(self.colors)]
            self.color_index += 1
            
            curve = self.plot_widget.plot(
                pen=pg.mkPen(color=color, width=2),
                name=f"{device_id} ({sensor_type})"
            )
            self.plot_curves[device_id] = curve
        
        # Add data point
        self.data_buffers[device_id].append(data_point)
        self.total_points += 1
    
    def _update_plot(self):
        """Update the plot with current data"""
        try:
            current_time = time.time()
            time_window = self.time_window_spin.value()
            
            for device_id, buffer in self.data_buffers.items():
                if not buffer:
                    continue
                
                curve = self.plot_curves.get(device_id)
                if not curve:
                    continue
                
                # Filter data within time window
                cutoff_time = current_time - time_window
                recent_data = [
                    point for point in buffer 
                    if point.timestamp >= cutoff_time
                ]
                
                if recent_data:
                    # Convert to arrays for plotting
                    times = np.array([point.timestamp for point in recent_data])
                    values = np.array([point.value for point in recent_data])
                    
                    # Normalize time to relative seconds
                    if len(times) > 0:
                        times = times - times[0]
                    
                    # Update curve
                    curve.setData(times, values)
            
            # Auto-scale if enabled
            if self.auto_scale_cb.isChecked():
                self.plot_widget.autoRange()
            
            # Update statistics
            self.stats_label.setText(f"Points: {self.total_points}")
            
            self.last_update = current_time
            
        except Exception as e:
            logger.error(f"Error updating plot: {e}")
    
    def clear_data(self):
        """Clear all data from the plot"""
        self.data_buffers.clear()
        for curve in self.plot_curves.values():
            curve.setData([], [])
        self.plot_curves.clear()
        self.total_points = 0
        self.color_index = 0
        logger.info("Plot data cleared")
    
    def export_data(self, filename: str):
        """Export current data to CSV file"""
        try:
            import csv
            
            with open(filename, 'w', newline='') as csvfile:
                writer = csv.writer(csvfile)
                writer.writerow(['device_id', 'timestamp', 'value', 'sensor_type', 'metadata'])
                
                for device_id, buffer in self.data_buffers.items():
                    for point in buffer:
                        writer.writerow([
                            point.device_id,
                            point.timestamp,
                            point.value,
                            point.sensor_type,
                            json.dumps(point.metadata)
                        ])
            
            logger.info(f"Data exported to {filename}")
            
        except Exception as e:
            logger.error(f"Error exporting data: {e}")


class ThermalImageView(QWidget):
    """Widget for displaying thermal camera images"""
    
    def __init__(self):
        super().__init__()
        self.setup_ui()
        self.frame_count = 0
        
    def setup_ui(self):
        """Setup the thermal image UI"""
        layout = QVBoxLayout(self)
        
        # Image view
        self.image_view = ImageView()
        self.image_view.ui.roiBtn.hide()
        self.image_view.ui.menuBtn.hide()
        layout.addWidget(self.image_view)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        # Frame counter
        self.frame_label = QLabel("Frames: 0")
        controls_layout.addWidget(self.frame_label)
        
        # Colormap selection
        controls_layout.addWidget(QLabel("Colormap:"))
        self.colormap_combo = QComboBox()
        self.colormap_combo.addItems(['thermal', 'viridis', 'plasma', 'inferno', 'magma'])
        self.colormap_combo.currentTextChanged.connect(self._update_colormap)
        controls_layout.addWidget(self.colormap_combo)
        
        controls_layout.addStretch()
        layout.addLayout(controls_layout)
    
    def display_thermal_frame(self, image_data: np.ndarray, timestamp: float = None):
        """Display a thermal frame"""
        try:
            if image_data is None or image_data.size == 0:
                return
            
            # Ensure proper format
            if image_data.dtype != np.float32:
                image_data = image_data.astype(np.float32)
            
            # Update image
            self.image_view.setImage(image_data, autoRange=True, autoLevels=True)
            
            self.frame_count += 1
            self.frame_label.setText(f"Frames: {self.frame_count}")
            
        except Exception as e:
            logger.error(f"Error displaying thermal frame: {e}")
    
    def display_thermal_from_base64(self, base64_data: str, timestamp: float = None):
        """Display thermal frame from base64 encoded data"""
        try:
            import base64
            
            # Decode base64
            image_bytes = base64.b64decode(base64_data)
            
            # Convert to numpy array
            nparr = np.frombuffer(image_bytes, np.uint8)
            
            # Decode image
            if OPENCV_AVAILABLE:
                # Use OpenCV for JPEG decoding
                image = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
                if image is not None:
                    self.display_thermal_frame(image, timestamp)
            else:
                logger.warning("OpenCV not available, cannot decode thermal image")
                
        except Exception as e:
            logger.error(f"Error decoding thermal image: {e}")
    
    def _update_colormap(self, colormap_name: str):
        """Update the colormap for thermal display"""
        try:
            # PyQtGraph colormap mapping
            colormap_map = {
                'thermal': 'thermal',
                'viridis': 'viridis',
                'plasma': 'plasma',
                'inferno': 'inferno',
                'magma': 'magma'
            }
            
            if colormap_name in colormap_map:
                # Note: PyQtGraph colormap support varies by version
                pass
                
        except Exception as e:
            logger.error(f"Error updating colormap: {e}")


class MultiModalVisualizationWidget(QWidget):
    """Combined widget for multi-modal sensor visualization"""
    
    def __init__(self):
        super().__init__()
        self.setup_ui()
        
        # Data processing
        self.data_processors: Dict[str, Callable] = {
            'gsr_data': self._process_gsr_data,
            'thermal_frame': self._process_thermal_frame,
            'rgb_frame': self._process_rgb_frame,
            'heartbeat': self._process_heartbeat
        }
    
    def setup_ui(self):
        """Setup the multi-modal visualization UI"""
        layout = QVBoxLayout(self)
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # GSR plot tab
        self.gsr_plot = RealTimePlot("GSR Data (µS)", max_points=2000)
        self.tab_widget.addTab(self.gsr_plot, "GSR")
        
        # Thermal image tab
        self.thermal_view = ThermalImageView()
        self.tab_widget.addTab(self.thermal_view, "Thermal")
        
        # Additional sensor plots
        self.ppg_plot = RealTimePlot("PPG Data", max_points=1000)
        self.tab_widget.addTab(self.ppg_plot, "PPG")
        
        self.accel_plot = RealTimePlot("Accelerometer Data", max_points=1000)
        self.tab_widget.addTab(self.accel_plot, "Accelerometer")
        
        # Statistics text
        self.stats_text = QTextEdit()
        self.stats_text.setMaximumHeight(100)
        self.stats_text.setReadOnly(True)
        layout.addWidget(self.stats_text)
        
        # Update timer for statistics
        self.stats_timer = QTimer()
        self.stats_timer.timeout.connect(self._update_statistics)
        self.stats_timer.start(1000)  # Update every second
    
    def process_device_data(self, device_id: str, data: Dict[str, Any]):
        """Process incoming data from devices"""
        try:
            data_type = data.get('type')
            timestamp = time.time()
            
            # Try to parse timestamp from data
            if 'timestamp' in data:
                try:
                    # Parse ISO format timestamp
                    dt = datetime.fromisoformat(data['timestamp'].replace('Z', '+00:00'))
                    timestamp = dt.timestamp()
                except:
                    pass
            
            # Route to appropriate processor
            processor = self.data_processors.get(data_type)
            if processor:
                processor(device_id, data, timestamp)
            else:
                logger.debug(f"No processor for data type: {data_type}")
                
        except Exception as e:
            logger.error(f"Error processing device data: {e}")
    
    def _process_gsr_data(self, device_id: str, data: Dict[str, Any], timestamp: float):
        """Process GSR sensor data"""
        try:
            gsr_value = data.get('gsr_microsiemens', 0.0)
            
            self.gsr_plot.add_data_point(
                device_id=device_id,
                timestamp=timestamp,
                value=gsr_value,
                sensor_type="GSR",
                metadata={'raw_value': data.get('gsr_raw', 0)}
            )
            
            # Also process PPG if available
            if 'ppg_raw' in data:
                ppg_value = data.get('ppg_raw', 0.0)
                self.ppg_plot.add_data_point(
                    device_id=device_id,
                    timestamp=timestamp,
                    value=ppg_value,
                    sensor_type="PPG"
                )
            
        except Exception as e:
            logger.error(f"Error processing GSR data: {e}")
    
    def _process_thermal_frame(self, device_id: str, data: Dict[str, Any], timestamp: float):
        """Process thermal camera frame"""
        try:
            image_data = data.get('image_base64')
            if image_data:
                self.thermal_view.display_thermal_from_base64(image_data, timestamp)
                
        except Exception as e:
            logger.error(f"Error processing thermal frame: {e}")
    
    def _process_rgb_frame(self, device_id: str, data: Dict[str, Any], timestamp: float):
        """Process RGB camera frame"""
        try:
            # TODO: Implement RGB frame display
            logger.debug(f"RGB frame received from {device_id}")
            
        except Exception as e:
            logger.error(f"Error processing RGB frame: {e}")
    
    def _process_heartbeat(self, device_id: str, data: Dict[str, Any], timestamp: float):
        """Process heartbeat message"""
        try:
            # Just log heartbeat, no visualization needed
            logger.debug(f"Heartbeat from {device_id}")
            
        except Exception as e:
            logger.error(f"Error processing heartbeat: {e}")
    
    def _update_statistics(self):
        """Update statistics display"""
        try:
            stats_text = "📊 Real-time Visualization Statistics\n"
            stats_text += f"GSR Data Points: {self.gsr_plot.total_points}\n"
            stats_text += f"Thermal Frames: {self.thermal_view.frame_count}\n"
            stats_text += f"PPG Data Points: {self.ppg_plot.total_points}\n"
            stats_text += f"Accelerometer Points: {self.accel_plot.total_points}\n"
            
            self.stats_text.setPlainText(stats_text)
            
        except Exception as e:
            logger.error(f"Error updating statistics: {e}")
    
    def clear_all_data(self):
        """Clear all visualization data"""
        self.gsr_plot.clear_data()
        self.ppg_plot.clear_data()
        self.accel_plot.clear_data()
        self.thermal_view.frame_count = 0
        logger.info("All visualization data cleared")
    
    def export_all_data(self, base_filename: str):
        """Export all data to files"""
        try:
            self.gsr_plot.export_data(f"{base_filename}_gsr.csv")
            self.ppg_plot.export_data(f"{base_filename}_ppg.csv")
            self.accel_plot.export_data(f"{base_filename}_accel.csv")
            logger.info(f"All data exported with base filename: {base_filename}")
            
        except Exception as e:
            logger.error(f"Error exporting data: {e}")


# Demo application
class VisualizationDemo(QMainWindow):
    """Demo application for real-time visualization"""
    
    def __init__(self):
        super().__init__()
        self.setWindowTitle("PC Controller - Real-time Visualization Demo")
        self.setGeometry(100, 100, 1200, 800)
        
        # Central widget
        self.visualization_widget = MultiModalVisualizationWidget()
        self.setCentralWidget(self.visualization_widget)
        
        # Demo timer for generating sample data
        self.demo_timer = QTimer()
        self.demo_timer.timeout.connect(self._generate_demo_data)
        self.demo_timer.start(100)  # 10 Hz demo data
        
        self.demo_time = 0
    
    def _generate_demo_data(self):
        """Generate demo data for visualization"""
        import random
        
        self.demo_time += 0.1
        
        # Generate synthetic GSR data
        gsr_data = {
            'type': 'gsr_data',
            'gsr_microsiemens': 5.0 + 2.0 * np.sin(self.demo_time * 0.5) + random.uniform(-0.5, 0.5),
            'gsr_raw': int(1000 + 200 * np.sin(self.demo_time * 0.5)),
            'ppg_raw': int(500 + 100 * np.sin(self.demo_time * 2.0)),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
        self.visualization_widget.process_device_data("demo_device", gsr_data)
        
        # Generate synthetic thermal data occasionally
        if self.demo_time % 1.0 < 0.1:  # Every second
            thermal_image = np.random.rand(64, 64) * 255
            self.visualization_widget.thermal_view.display_thermal_frame(thermal_image)


def demo_visualization():
    """Run the visualization demo"""
    if not GUI_AVAILABLE:
        print("❌ GUI libraries not available")
        return
    
    app = QApplication([])
    
    # Apply dark theme
    app.setStyle('Fusion')
    palette = app.palette()
    palette.setColor(palette.ColorRole.Window, pg.QtGui.QColor(53, 53, 53))
    palette.setColor(palette.ColorRole.WindowText, pg.QtGui.QColor(255, 255, 255))
    app.setPalette(palette)
    
    # Create and show demo
    demo = VisualizationDemo()
    demo.show()
    
    print("🎬 Real-time visualization demo started!")
    print("📊 You should see synthetic GSR, PPG, and thermal data")
    print("🎮 Use the controls to adjust time windows and clear data")
    
    app.exec()


if __name__ == "__main__":
    demo_visualization()