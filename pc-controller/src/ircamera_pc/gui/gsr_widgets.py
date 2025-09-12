#!/usr/bin/env python3
"""
GSR Monitoring Widgets for IRCamera PC Controller

Real-time GSR data visualization and device management widgets for the hub-spoke
Multi-Modal Physiological Sensing Platform.
"""

import asyncio
import sys
from typing import Dict, List, Optional, Tuple
from pathlib import Path
import numpy as np
from datetime import datetime, timedelta

try:
    from PyQt6.QtWidgets import (
        QWidget, QVBoxLayout, QHBoxLayout, QGridLayout,
        QLabel, QPushButton, QComboBox, QSpinBox, QDoubleSpinBox,
        QTextEdit, QProgressBar, QGroupBox, QFrame, QScrollArea,
        QTabWidget, QTableWidget, QTableWidgetItem, QHeaderView,
        QSplitter, QMessageBox, QFileDialog, QCheckBox
    )
    from PyQt6.QtCore import (
        Qt, QTimer, pyqtSignal, QThread, pyqtSlot, QMutex
    )
    from PyQt6.QtGui import QPixmap, QFont, QColor, QPalette
    
    # Import PyQtGraph for real-time plotting
    import pyqtgraph as pg
    from pyqtgraph import PlotWidget, mkPen, PlotDataItem
    
except ImportError as e:
    print(f"Failed to import PyQt6 or pyqtgraph: {e}")
    sys.exit(1)

from loguru import logger


class GSRDeviceStatusWidget(QWidget):
    """Widget showing status of connected GSR devices"""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.devices = {}
        self.init_ui()
        
        # Update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_display)
        self.update_timer.start(1000)  # Update every second
    
    def init_ui(self):
        """Initialize the user interface"""
        layout = QVBoxLayout()
        
        # Title
        title = QLabel("GSR Device Status")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(title)
        
        # Device list area
        self.scroll_area = QScrollArea()
        self.scroll_widget = QWidget()
        self.scroll_layout = QVBoxLayout(self.scroll_widget)
        self.scroll_area.setWidget(self.scroll_widget)
        self.scroll_area.setWidgetResizable(True)
        layout.addWidget(self.scroll_area)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        self.refresh_btn = QPushButton("Refresh")
        self.refresh_btn.clicked.connect(self.refresh_devices)
        controls_layout.addWidget(self.refresh_btn)
        
        self.export_btn = QPushButton("Export Data")
        self.export_btn.clicked.connect(self.export_selected_data)
        controls_layout.addWidget(self.export_btn)
        
        controls_layout.addStretch()
        layout.addLayout(controls_layout)
        
        self.setLayout(layout)
    
    def add_device(self, device_id: str, device_info: Dict):
        """Add or update device display"""
        if device_id not in self.devices:
            device_widget = self.create_device_widget(device_id, device_info)
            self.devices[device_id] = device_widget
            self.scroll_layout.addWidget(device_widget)
        else:
            self.update_device_info(device_id, device_info)
    
    def create_device_widget(self, device_id: str, device_info: Dict) -> QGroupBox:
        """Create widget for a single GSR device"""
        group = QGroupBox(f"Device: {device_id}")
        layout = QGridLayout()
        
        # Device ID
        layout.addWidget(QLabel("Device ID:"), 0, 0)
        device_id_label = QLabel(device_id)
        device_id_label.setStyleSheet("font-weight: bold;")
        layout.addWidget(device_id_label, 0, 1)
        
        # Session ID
        layout.addWidget(QLabel("Session:"), 0, 2)
        session_label = QLabel(device_info.get("session_id", "N/A"))
        layout.addWidget(session_label, 0, 3)
        
        # Connection status
        layout.addWidget(QLabel("Status:"), 1, 0)
        status_label = QLabel("Connected")
        status_label.setStyleSheet("color: green; font-weight: bold;")
        layout.addWidget(status_label, 1, 1)
        
        # Sample count
        layout.addWidget(QLabel("Samples:"), 1, 2)
        samples_label = QLabel(str(device_info.get("sample_count", 0)))
        layout.addWidget(samples_label, 1, 3)
        
        # Quality metrics
        layout.addWidget(QLabel("Quality:"), 2, 0)
        quality_bar = QProgressBar()
        quality_bar.setRange(0, 100)
        quality_bar.setValue(device_info.get("avg_quality", 0))
        layout.addWidget(quality_bar, 2, 1, 1, 3)
        
        # Network stats
        network_stats = device_info.get("network_stats", {})
        layout.addWidget(QLabel("Packets:"), 3, 0)
        layout.addWidget(QLabel(str(network_stats.get("packets_received", 0))), 3, 1)
        layout.addWidget(QLabel("Errors:"), 3, 2)
        layout.addWidget(QLabel(str(network_stats.get("network_errors", 0))), 3, 3)
        
        # Store references for updates
        group.device_id = device_id
        group.session_label = session_label
        group.status_label = status_label
        group.samples_label = samples_label
        group.quality_bar = quality_bar
        
        group.setLayout(layout)
        return group
    
    def update_device_info(self, device_id: str, device_info: Dict):
        """Update device information display"""
        if device_id not in self.devices:
            return
        
        widget = self.devices[device_id]
        widget.session_label.setText(device_info.get("session_id", "N/A"))
        widget.samples_label.setText(str(device_info.get("sample_count", 0)))
        widget.quality_bar.setValue(int(device_info.get("avg_quality", 0)))
    
    def remove_device(self, device_id: str):
        """Remove device from display"""
        if device_id in self.devices:
            widget = self.devices[device_id]
            self.scroll_layout.removeWidget(widget)
            widget.deleteLater()
            del self.devices[device_id]
    
    def update_display(self):
        """Update display with latest data"""
        # This will be called by the parent to update with fresh data
        pass
    
    def refresh_devices(self):
        """Refresh device list"""
        # Emit signal to parent to refresh data
        self.parent().refresh_gsr_devices()
    
    def export_selected_data(self):
        """Export data for selected devices"""
        # Open export dialog
        export_dialog = GSRExportDialog(list(self.devices.keys()), self)
        export_dialog.exec()


class GSRPlotWidget(QWidget):
    """Real-time GSR data plotting widget"""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.data_buffers = {}  # device_id -> (timestamps, values)
        self.plot_items = {}    # device_id -> PlotDataItem
        self.colors = ['r', 'g', 'b', 'c', 'm', 'y', 'w']
        self.color_index = 0
        
        self.init_ui()
        
        # Data update timer
        self.plot_timer = QTimer()
        self.plot_timer.timeout.connect(self.update_plots)
        self.plot_timer.start(100)  # Update every 100ms
    
    def init_ui(self):
        """Initialize the plotting interface"""
        layout = QVBoxLayout()
        
        # Title and controls
        header_layout = QHBoxLayout()
        
        title = QLabel("Real-time GSR Data")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        header_layout.addWidget(title)
        
        header_layout.addStretch()
        
        # Plot controls
        self.auto_scale_cb = QCheckBox("Auto Scale")
        self.auto_scale_cb.setChecked(True)
        header_layout.addWidget(self.auto_scale_cb)
        
        self.clear_btn = QPushButton("Clear")
        self.clear_btn.clicked.connect(self.clear_plots)
        header_layout.addWidget(self.clear_btn)
        
        layout.addLayout(header_layout)
        
        # Plot widget
        self.plot_widget = PlotWidget()
        self.plot_widget.setLabel('left', 'GSR (µS)')
        self.plot_widget.setLabel('bottom', 'Time')
        self.plot_widget.showGrid(x=True, y=True)
        self.plot_widget.setYRange(0, 50)  # Typical GSR range
        
        layout.addWidget(self.plot_widget)
        
        # Legend area
        self.legend_layout = QHBoxLayout()
        layout.addLayout(self.legend_layout)
        
        self.setLayout(layout)
    
    def add_device_data(self, device_id: str, timestamp: float, gsr_value: float):
        """Add data point for a device"""
        if device_id not in self.data_buffers:
            self.data_buffers[device_id] = ([], [])
            
            # Create plot item
            color = self.colors[self.color_index % len(self.colors)]
            pen = mkPen(color=color, width=2)
            plot_item = self.plot_widget.plot(pen=pen, name=device_id)
            self.plot_items[device_id] = plot_item
            self.color_index += 1
            
            # Add legend
            legend_label = QLabel(f"● {device_id}")
            legend_label.setStyleSheet(f"color: {color}; font-weight: bold;")
            self.legend_layout.addWidget(legend_label)
        
        timestamps, values = self.data_buffers[device_id]
        timestamps.append(timestamp)
        values.append(gsr_value)
        
        # Keep only last 1000 points for performance
        if len(timestamps) > 1000:
            timestamps.pop(0)
            values.pop(0)
    
    def update_plots(self):
        """Update all plot lines"""
        current_time = datetime.now().timestamp()
        
        for device_id, (timestamps, values) in self.data_buffers.items():
            if timestamps and device_id in self.plot_items:
                # Convert timestamps to relative time (seconds ago)
                relative_times = [current_time - ts for ts in timestamps]
                relative_times.reverse()  # Make it ascending
                values_copy = list(reversed(values))
                
                self.plot_items[device_id].setData(relative_times, values_copy)
        
        # Auto-scale if enabled
        if self.auto_scale_cb.isChecked():
            self.plot_widget.autoRange()
    
    def clear_plots(self):
        """Clear all plot data"""
        self.data_buffers.clear()
        self.plot_widget.clear()
        self.plot_items.clear()
        
        # Clear legend
        for i in reversed(range(self.legend_layout.count())):
            child = self.legend_layout.itemAt(i).widget()
            if child:
                child.deleteLater()
    
    def remove_device(self, device_id: str):
        """Remove device from plots"""
        if device_id in self.data_buffers:
            del self.data_buffers[device_id]
        
        if device_id in self.plot_items:
            self.plot_widget.removeItem(self.plot_items[device_id])
            del self.plot_items[device_id]


class GSRStatisticsWidget(QWidget):
    """Widget showing GSR statistics and analytics"""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.init_ui()
        
        # Update timer
        self.stats_timer = QTimer()
        self.stats_timer.timeout.connect(self.update_statistics)
        self.stats_timer.start(5000)  # Update every 5 seconds
    
    def init_ui(self):
        """Initialize statistics display"""
        layout = QVBoxLayout()
        
        # Title
        title = QLabel("GSR Session Statistics")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(title)
        
        # Statistics table
        self.stats_table = QTableWidget()
        self.stats_table.setColumnCount(6)
        self.stats_table.setHorizontalHeaderLabels([
            "Device ID", "Session", "Samples", "Avg Quality", "Duration", "Status"
        ])
        
        # Make table stretch to fill width
        header = self.stats_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        
        layout.addWidget(self.stats_table)
        
        # Summary stats
        summary_layout = QHBoxLayout()
        
        self.total_devices_label = QLabel("Total Devices: 0")
        summary_layout.addWidget(self.total_devices_label)
        
        self.total_samples_label = QLabel("Total Samples: 0")
        summary_layout.addWidget(self.total_samples_label)
        
        self.avg_quality_label = QLabel("Average Quality: 0%")
        summary_layout.addWidget(self.avg_quality_label)
        
        summary_layout.addStretch()
        layout.addLayout(summary_layout)
        
        self.setLayout(layout)
    
    def update_statistics(self, session_stats: Optional[Dict] = None):
        """Update statistics display"""
        if not session_stats:
            return
        
        # Clear and repopulate table
        self.stats_table.setRowCount(len(session_stats))
        
        total_samples = 0
        total_quality = 0
        quality_count = 0
        
        for row, (session_key, stats) in enumerate(session_stats.items()):
            if not stats:
                continue
                
            # Device ID
            device_id = stats.get("device_id", "Unknown")
            self.stats_table.setItem(row, 0, QTableWidgetItem(device_id))
            
            # Session ID
            session_id = stats.get("session_id", "Unknown")
            self.stats_table.setItem(row, 1, QTableWidgetItem(session_id))
            
            # Sample count
            sample_count = stats.get("sample_count", 0)
            total_samples += sample_count
            self.stats_table.setItem(row, 2, QTableWidgetItem(str(sample_count)))
            
            # Average quality
            avg_quality = stats.get("quality_stats", {}).get("avg_quality", 0)
            if avg_quality > 0:
                quality_count += 1
                total_quality += avg_quality
            self.stats_table.setItem(row, 3, QTableWidgetItem(f"{avg_quality:.1f}%"))
            
            # Duration
            start_time = stats.get("start_time", 0)
            if start_time > 0:
                duration = datetime.now().timestamp() - start_time
                duration_str = str(timedelta(seconds=int(duration)))
                self.stats_table.setItem(row, 4, QTableWidgetItem(duration_str))
            else:
                self.stats_table.setItem(row, 4, QTableWidgetItem("N/A"))
            
            # Status
            self.stats_table.setItem(row, 5, QTableWidgetItem("Active"))
        
        # Update summary
        self.total_devices_label.setText(f"Total Devices: {len(session_stats)}")
        self.total_samples_label.setText(f"Total Samples: {total_samples:,}")
        
        if quality_count > 0:
            avg_quality = total_quality / quality_count
            self.avg_quality_label.setText(f"Average Quality: {avg_quality:.1f}%")
        else:
            self.avg_quality_label.setText("Average Quality: N/A")


class GSRExportDialog(QWidget):
    """Dialog for exporting GSR data"""
    
    def __init__(self, available_devices: List[str], parent=None):
        super().__init__(parent)
        self.available_devices = available_devices
        self.setWindowTitle("Export GSR Data")
        self.resize(400, 300)
        self.init_ui()
    
    def init_ui(self):
        """Initialize export dialog"""
        layout = QVBoxLayout()
        
        # Device selection
        device_group = QGroupBox("Select Devices")
        device_layout = QVBoxLayout()
        
        self.device_checkboxes = {}
        for device_id in self.available_devices:
            cb = QCheckBox(device_id)
            cb.setChecked(True)
            self.device_checkboxes[device_id] = cb
            device_layout.addWidget(cb)
        
        device_group.setLayout(device_layout)
        layout.addWidget(device_group)
        
        # Export format
        format_group = QGroupBox("Export Format")
        format_layout = QVBoxLayout()
        
        self.format_combo = QComboBox()
        self.format_combo.addItems(["CSV", "JSON", "HDF5"])
        format_layout.addWidget(self.format_combo)
        
        format_group.setLayout(format_layout)
        layout.addWidget(format_group)
        
        # Buttons
        button_layout = QHBoxLayout()
        
        export_btn = QPushButton("Export")
        export_btn.clicked.connect(self.export_data)
        button_layout.addWidget(export_btn)
        
        cancel_btn = QPushButton("Cancel")
        cancel_btn.clicked.connect(self.close)
        button_layout.addWidget(cancel_btn)
        
        layout.addLayout(button_layout)
        self.setLayout(layout)
    
    def export_data(self):
        """Perform data export"""
        selected_devices = [
            device_id for device_id, cb in self.device_checkboxes.items()
            if cb.isChecked()
        ]
        
        if not selected_devices:
            QMessageBox.warning(self, "Warning", "Please select at least one device.")
            return
        
        format_str = self.format_combo.currentText().lower()
        
        # Get export directory
        export_dir = QFileDialog.getExistingDirectory(
            self, "Select Export Directory"
        )
        
        if not export_dir:
            return
        
        # Notify parent to perform export
        if hasattr(self.parent(), 'export_gsr_data'):
            self.parent().export_gsr_data(selected_devices, format_str, export_dir)
        
        self.close()


class GSRMainWidget(QWidget):
    """Main GSR monitoring widget combining all components"""
    
    def __init__(self, network_server=None, parent=None):
        super().__init__(parent)
        self.network_server = network_server
        self.init_ui()
        
        # Data update timer
        self.data_timer = QTimer()
        self.data_timer.timeout.connect(self.update_gsr_data)
        self.data_timer.start(1000)  # Update every second
    
    def init_ui(self):
        """Initialize main GSR interface"""
        layout = QVBoxLayout()
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        
        # Device status tab
        self.device_widget = GSRDeviceStatusWidget()
        self.tab_widget.addTab(self.device_widget, "Device Status")
        
        # Real-time plotting tab
        self.plot_widget = GSRPlotWidget()
        self.tab_widget.addTab(self.plot_widget, "Real-time Data")
        
        # Statistics tab
        self.stats_widget = GSRStatisticsWidget()
        self.tab_widget.addTab(self.stats_widget, "Statistics")
        
        layout.addWidget(self.tab_widget)
        self.setLayout(layout)
    
    def update_gsr_data(self):
        """Update GSR data from network server"""
        if not self.network_server:
            return
        
        try:
            # Get session statistics
            session_stats = self.network_server.get_gsr_session_stats()
            
            # Update device status
            for session_key, stats in session_stats.items():
                if stats:
                    device_id = stats.get("device_id")
                    if device_id:
                        self.device_widget.add_device(device_id, stats)
            
            # Update statistics
            self.stats_widget.update_statistics(session_stats)
            
            # Update plots with simulated real-time data
            # In real implementation, this would come from the GSR receiver
            self.update_real_time_plots()
            
        except Exception as e:
            logger.error(f"Error updating GSR data: {e}")
    
    def update_real_time_plots(self):
        """Update real-time plots (placeholder for real data)"""
        # This would be replaced with actual real-time data from GSR receiver
        import random
        import time
        
        # Simulate data for demonstration
        current_time = time.time()
        for device_id in ["device_001", "device_002"]:
            gsr_value = 10 + random.random() * 20  # Simulate GSR data
            self.plot_widget.add_device_data(device_id, current_time, gsr_value)
    
    def refresh_gsr_devices(self):
        """Refresh GSR device data"""
        # Force immediate update
        self.update_gsr_data()
    
    def export_gsr_data(self, device_ids: List[str], format_str: str, export_dir: str):
        """Export GSR data for selected devices"""
        if not self.network_server:
            QMessageBox.warning(self, "Warning", "No network server available for export.")
            return
        
        try:
            # Export data for each selected device
            exported_files = []
            
            for device_id in device_ids:
                # This would need session_id in real implementation
                session_id = "current_session"  # Placeholder
                
                export_path = asyncio.run(
                    self.network_server.export_gsr_session_data(
                        device_id, session_id, format_str
                    )
                )
                
                if export_path:
                    exported_files.append(export_path)
            
            if exported_files:
                QMessageBox.information(
                    self, "Export Complete", 
                    f"Exported {len(exported_files)} files to {export_dir}"
                )
            else:
                QMessageBox.warning(self, "Export Failed", "No data was exported.")
                
        except Exception as e:
            logger.error(f"Error exporting GSR data: {e}")
            QMessageBox.critical(self, "Export Error", f"Export failed: {str(e)}")