#!/usr/bin/env python3

import asyncio
import sys
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any

try:

    from PyQt6.QtCore import Qt, QTimer
    from PyQt6.QtGui import QColor, QFont
    from PyQt6.QtWidgets import (
        QCheckBox,
        QComboBox,
        QFileDialog,
        QGridLayout,
        QGroupBox,
        QHBoxLayout,
        QHeaderView,
        QLabel,
        QMessageBox,
        QProgressBar,
        QPushButton,
        QScrollArea,
        QSplitter,
        QTableWidget,
        QTableWidgetItem,
        QTabWidget,
        QTextEdit,
        QVBoxLayout,
        QWidget,
    )
    from pyqtgraph import PlotWidget, mkPen

except ImportError as e:
    print(f"Failed to import PyQt6 or pyqtgraph: {e}")
    sys.exit(1)


class GSRDeviceStatusWidget(QWidget):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.devices = {}
        self.init_ui()

        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_display)
        self.update_timer.start(1000)

    def init_ui(self) -> Any:

        layout = QVBoxLayout()

        title = QLabel("GSR Device Status")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(title)

        self.scroll_area = QScrollArea()
        self.scroll_widget = QWidget()
        self.scroll_layout = QVBoxLayout(self.scroll_widget)
        self.scroll_area.setWidget(self.scroll_widget)
        self.scroll_area.setWidgetResizable(True)
        layout.addWidget(self.scroll_area)

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

    def add_device(self, device_id: None = str, device_info: None = Dict) -> None:

        if device_id not in self.devices:
            device_widget = self.create_device_widget(device_id, device_info)
            self.devices[device_id] = device_widget
            self.scroll_layout.addWidget(device_widget)
        else:
            self.update_device_info(device_id, device_info)

    def create_device_widget(self, device_id: str, device_info: Dict) -> QGroupBox:

        group = QGroupBox(f"Device: {device_id}")
        layout = QGridLayout()

        layout.addWidget(QLabel("Device ID:"), 0, 0)
        device_id_label = QLabel(device_id)
        device_id_label.setStyleSheet("font-weight: bold;")
        layout.addWidget(device_id_label, 0, 1)

        layout.addWidget(QLabel("Session:"), 0, 2)
        session_label = QLabel(device_info.get("session_id", "N/A"))
        layout.addWidget(session_label, 0, 3)

        layout.addWidget(QLabel("Status:"), 1, 0)
        status_label = QLabel("Connected")
        status_label.setStyleSheet("color: green; font-weight: bold;")
        layout.addWidget(status_label, 1, 1)

        layout.addWidget(QLabel("Samples:"), 1, 2)
        samples_label = QLabel(str(device_info.get("sample_count", 0)))
        layout.addWidget(samples_label, 1, 3)

        layout.addWidget(QLabel("Quality:"), 2, 0)
        quality_bar = QProgressBar()
        quality_bar.setRange(0, 100)
        quality_bar.setValue(device_info.get("avg_quality", 0))
        layout.addWidget(quality_bar, 2, 1, 1, 3)

        network_stats = device_info.get("network_stats", {})
        layout.addWidget(QLabel("Packets:"), 3, 0)
        layout.addWidget(QLabel(str(network_stats.get("packets_received", 0))), 3, 1)
        layout.addWidget(QLabel("Errors:"), 3, 2)
        layout.addWidget(QLabel(str(network_stats.get("network_errors", 0))), 3, 3)

        group.device_id = device_id
        group.session_label = session_label
        group.status_label = status_label
        group.samples_label = samples_label
        group.quality_bar = quality_bar

        group.setLayout(layout)
        return group

    def update_device_info(
            self, device_id: None = str, device_info: None = Dict
    ) -> None:

        if device_id not in self.devices:
            return

        widget = self.devices[device_id]
        widget.session_label.setText(device_info.get("session_id", "N/A"))
        widget.samples_label.setText(str(device_info.get("sample_count", 0)))
        widget.quality_bar.setValue(int(device_info.get("avg_quality", 0)))

    def remove_device(self, device_id: None = str) -> None:

        if device_id in self.devices:
            widget = self.devices[device_id]
            self.scroll_layout.removeWidget(widget)
            widget.deleteLater()
            del self.devices[device_id]

    def update_display(self) -> None:

        pass

    def refresh_devices(self) -> Any:

        self.parent().refresh_gsr_devices()

    def export_selected_data(self) -> Any:

        export_dialog = GSRExportDialog(list(self.devices.keys()), self)
        export_dialog.exec()


class GSRPlotWidget(QWidget):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.data_buffers = {}
        self.plot_items = {}
        self.colors = ["r", "g", "b", "c", "m", "y", "w"]
        self.color_index = 0

        self.init_ui()

        self.plot_timer = QTimer()
        self.plot_timer.timeout.connect(self.update_plots)
        self.plot_timer.start(100)

    def init_ui(self) -> Any:

        layout = QVBoxLayout()

        header_layout = QHBoxLayout()

        title = QLabel("Real-time GSR Data")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        header_layout.addWidget(title)

        header_layout.addStretch()

        self.auto_scale_cb = QCheckBox("Auto Scale")
        self.auto_scale_cb.setChecked(True)
        header_layout.addWidget(self.auto_scale_cb)

        self.clear_btn = QPushButton("Clear")
        self.clear_btn.clicked.connect(self.clear_plots)
        header_layout.addWidget(self.clear_btn)

        layout.addLayout(header_layout)

        self.plot_widget = PlotWidget()
        self.plot_widget.setLabel("left", "GSR (µS)")
        self.plot_widget.setLabel("bottom", "Time")
        self.plot_widget.showGrid(x=True, y=True)
        self.plot_widget.setYRange(0, 50)

        layout.addWidget(self.plot_widget)

        self.legend_layout = QHBoxLayout()
        layout.addLayout(self.legend_layout)

        self.setLayout(layout)

    def add_device_data(
            self, device_id: None = str, timestamp: None = float, gsr_value: None = float
    ) -> None:

        if device_id not in self.data_buffers:
            self.data_buffers[device_id] = ([], [])

            color = self.colors[self.color_index % len(self.colors)]
            pen = mkPen(color=color, width=2)
            plot_item = self.plot_widget.plot(pen=pen, name=device_id)
            self.plot_items[device_id] = plot_item
            self.color_index += 1

            legend_label = QLabel(f"● {device_id}")
            legend_label.setStyleSheet(f"color: {color}; font-weight: bold;")
            self.legend_layout.addWidget(legend_label)

        timestamps, values = self.data_buffers[device_id]
        timestamps.append(timestamp)
        values.append(gsr_value)

        if len(timestamps) > 1000:
            timestamps.pop(0)
            values.pop(0)

    def update_plots(self) -> None:

        current_time = datetime.now().timestamp()

        for device_id, (timestamps, values) in self.data_buffers.items():
            if timestamps and device_id in self.plot_items:
                relative_times = [current_time - ts for ts in timestamps]
                relative_times.reverse()
                values_copy = list(reversed(values))

                self.plot_items[device_id].setData(relative_times, values_copy)

        if self.auto_scale_cb.isChecked():
            self.plot_widget.autoRange()

    def clear_plots(self) -> None:

        self.data_buffers.clear()
        self.plot_widget.clear()
        self.plot_items.clear()

        for i in reversed(range(self.legend_layout.count())):
            child = self.legend_layout.itemAt(i).widget()
            if child:
                child.deleteLater()

    def remove_device(self, device_id: None = str) -> None:

        if device_id in self.data_buffers:
            del self.data_buffers[device_id]

        if device_id in self.plot_items:
            self.plot_widget.removeItem(self.plot_items[device_id])
            del self.plot_items[device_id]


class GSRStatisticsWidget(QWidget):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.init_ui()

        self.stats_timer = QTimer()
        self.stats_timer.timeout.connect(self.update_statistics)
        self.stats_timer.start(5000)

    def init_ui(self) -> Any:

        layout = QVBoxLayout()

        title = QLabel("GSR Session Statistics")
        title.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        layout.addWidget(title)

        self.stats_table = QTableWidget()
        self.stats_table.setColumnCount(6)
        self.stats_table.setHorizontalHeaderLabels(
            ["Device ID", "Session", "Samples", "Avg Quality", "Duration", "Status"]
        )

        header = self.stats_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)

        layout.addWidget(self.stats_table)

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

    def update_statistics(self, session_stats: Optional[Dict[str, Any]] = None) -> None:

        if not session_stats:
            return

        self.stats_table.setRowCount(len(session_stats))

        total_samples = 0
        total_quality = 0
        quality_count = 0

        for row, (session_key, stats) in enumerate(session_stats.items()):
            if not stats:
                continue

            device_id = stats.get("device_id", "Unknown")
            self.stats_table.setItem(row, 0, QTableWidgetItem(device_id))

            session_id = stats.get("session_id", "Unknown")
            self.stats_table.setItem(row, 1, QTableWidgetItem(session_id))

            sample_count = stats.get("sample_count", 0)
            total_samples += sample_count
            self.stats_table.setItem(row, 2, QTableWidgetItem(str(sample_count)))

            avg_quality = stats.get("quality_stats", {}).get("avg_quality", 0)
            if avg_quality > 0:
                quality_count += 1
                total_quality += avg_quality
            self.stats_table.setItem(row, 3, QTableWidgetItem(f"{avg_quality:.1f}%"))

            start_time = stats.get("start_time", 0)
            if start_time > 0:
                duration = datetime.now().timestamp() - start_time
                duration_str = str(timedelta(seconds=int(duration)))
                self.stats_table.setItem(row, 4, QTableWidgetItem(duration_str))
            else:
                self.stats_table.setItem(row, 4, QTableWidgetItem("N/A"))

            self.stats_table.setItem(row, 5, QTableWidgetItem("Active"))

        self.total_devices_label.setText(f"Total Devices: {len(session_stats)}")
        self.total_samples_label.setText(f"Total Samples: {total_samples:,}")

        if quality_count > 0:
            avg_quality = total_quality / quality_count
            self.avg_quality_label.setText(f"Average Quality: {avg_quality:.1f}%")
        else:
            self.avg_quality_label.setText("Average Quality: N/A")


class GSRExportDialog(QWidget):

    def __init__(self, available_devices: List[str], parent=None):
        super().__init__(parent)
        self.available_devices = available_devices
        self.setWindowTitle("Export GSR Data")
        self.resize(400, 300)
        self.init_ui()

    def init_ui(self) -> Any:

        layout = QVBoxLayout()

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

        format_group = QGroupBox("Export Format")
        format_layout = QVBoxLayout()

        self.format_combo = QComboBox()
        self.format_combo.addItems(["CSV", "JSON", "HDF5"])
        format_layout.addWidget(self.format_combo)

        format_group.setLayout(format_layout)
        layout.addWidget(format_group)

        button_layout = QHBoxLayout()

        export_btn = QPushButton("Export")
        export_btn.clicked.connect(self.export_data)
        button_layout.addWidget(export_btn)

        cancel_btn = QPushButton("Cancel")
        cancel_btn.clicked.connect(self.close)
        button_layout.addWidget(cancel_btn)

        layout.addLayout(button_layout)
        self.setLayout(layout)

    def export_data(self) -> Any:

        selected_devices = [
            device_id
            for device_id, cb in self.device_checkboxes.items()
            if cb.isChecked()
        ]

        if not selected_devices:
            QMessageBox.warning(self, "Warning", "Please select at least one device.")
            return

        format_str = self.format_combo.currentText().lower()

        export_dir = QFileDialog.getExistingDirectory(self, "Select Export Directory")

        if not export_dir:
            return

        if hasattr(self.parent(), "export_gsr_data"):
            self.parent().export_gsr_data(selected_devices, format_str, export_dir)

        self.close()


class GSRMainWidget(QWidget):

    def __init__(self, network_server=None, parent=None):
        super().__init__(parent)
        self.network_server = network_server
        self.init_ui()

        self.data_timer = QTimer()
        self.data_timer.timeout.connect(self.update_gsr_data)
        self.data_timer.start(1000)

    def init_ui(self) -> Any:

        layout = QVBoxLayout()

        self.tab_widget = QTabWidget()

        self.device_widget = GSRDeviceStatusWidget()
        self.tab_widget.addTab(self.device_widget, "Device Status")

        self.plot_widget = GSRPlotWidget()
        self.tab_widget.addTab(self.plot_widget, "Real-time Data")

        self.stats_widget = GSRStatisticsWidget()
        self.tab_widget.addTab(self.stats_widget, "Statistics")

        layout.addWidget(self.tab_widget)
        self.setLayout(layout)

    def update_gsr_data(self) -> None:

        if not self.network_server:
            return

        try:

            session_stats = self.network_server.get_gsr_session_stats()

            for session_key, stats in session_stats.items():
                if stats:
                    device_id = stats.get("device_id")
                    if device_id:
                        self.device_widget.add_device(device_id, stats)

            self.stats_widget.update_statistics(session_stats)

            self.update_real_time_plots()

    def update_real_time_plots(self) -> None:
        """Update real-time plots (placeholder for real data)"""

        import random
        import time

        current_time = time.time()
        for device_id in ["device_001", "device_002"]:
            gsr_value = 10 + random.random() * 20
            self.plot_widget.add_device_data(device_id, current_time, gsr_value)

    def refresh_gsr_devices(self) -> Any:

        self.update_gsr_data()

    def export_gsr_data(
            self, device_ids: Any = List[str], format_str: Any = str, export_dir: Any = str
    ) -> Any:

        if not self.network_server:
            QMessageBox.warning(
                self, "Warning", "No network server available for export."
            )
            return

        try:

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
                    self,
                    "Export Complete",
                    f"Exported {len(exported_files)} files to {export_dir}",
                )
            else:
                QMessageBox.warning(self, "Export Failed", "No data was exported.")


class GSRAnalyticsWidget(QWidget):

    def __init__(self, gsr_receiver=None, parent=None):
        super().__init__(parent)
        self.gsr_receiver = gsr_receiver
        self.analytics_data = {}
        self.stress_history = {}
        self.init_ui()

        self.analytics_timer = QTimer()
        self.analytics_timer.timeout.connect(self.update_analytics)
        self.analytics_timer.start(2000)

    def init_ui(self) -> Any:

        layout = QVBoxLayout(self)

        title = QLabel("GSR Analytics & Stress Monitoring")
        title.setFont(QFont("Arial", 16, QFont.Weight.Bold))
        title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        layout.addWidget(title)

        splitter = QSplitter(Qt.Orientation.Horizontal)
        layout.addWidget(splitter)

        left_widget = QWidget()
        left_layout = QVBoxLayout(left_widget)

        stress_group = QGroupBox("Current Stress Levels")
        stress_layout = QVBoxLayout(stress_group)

        self.stress_table = QTableWidget(0, 4)
        self.stress_table.setHorizontalHeaderLabels(
            ["Device", "Stress Score", "Level", "Confidence"]
        )
        self.stress_table.horizontalHeader().setStretchLastSection(True)
        stress_layout.addWidget(self.stress_table)

        left_layout.addWidget(stress_group)

        alerts_group = QGroupBox("Analytics Alerts")
        alerts_layout = QVBoxLayout(alerts_group)

        self.alerts_text = QTextEdit()
        self.alerts_text.setMaximumHeight(120)
        self.alerts_text.setReadOnly(True)
        alerts_layout.addWidget(self.alerts_text)

        left_layout.addWidget(alerts_group)

        rec_group = QGroupBox("Recommendations")
        rec_layout = QVBoxLayout(rec_group)

        self.recommendations_text = QTextEdit()
        self.recommendations_text.setMaximumHeight(100)
        self.recommendations_text.setReadOnly(True)
        rec_layout.addWidget(self.recommendations_text)

        left_layout.addWidget(rec_group)

        splitter.addWidget(left_widget)

        right_widget = QWidget()
        right_layout = QVBoxLayout(right_widget)

        chart_group = QGroupBox("Stress Trend Analysis")
        chart_layout = QVBoxLayout(chart_group)

        self.stress_plot = PlotWidget()
        self.stress_plot.setLabel("left", "Stress Score", units="0-100")
        self.stress_plot.setLabel("bottom", "Time", units="seconds")
        self.stress_plot.setTitle("Real-time Stress Levels")
        self.stress_plot.showGrid(x=True, y=True)
        self.stress_plot.setYRange(0, 100)

        self.stress_curves = {}
        self.color_index = 0

        chart_layout.addWidget(self.stress_plot)
        right_layout.addWidget(chart_group)

        features_group = QGroupBox("GSR Features")
        features_layout = QGridLayout(features_group)

        self.feature_labels = {}
        feature_names = [
            ("Mean GSR", "μS"),
            ("Peak Frequency", "/min"),
            ("Rising Time", "%"),
            ("Rapid Changes", "count"),
            ("Trend Slope", "μS/s"),
            ("Spectral Entropy", "bits"),
        ]

        for i, (name, unit) in enumerate(feature_names):
            label = QLabel(f"{name}:")
            value_label = QLabel("--")
            value_label.setStyleSheet("font-weight: bold; color: blue;")
            unit_label = QLabel(unit)

            row = i // 2
            col = (i % 2) * 3

            features_layout.addWidget(label, row, col)
            features_layout.addWidget(value_label, row, col + 1)
            features_layout.addWidget(unit_label, row, col + 2)

            self.feature_labels[name] = value_label

        right_layout.addWidget(features_group)

        summary_group = QGroupBox("Session Summary")
        summary_layout = QVBoxLayout(summary_group)

        self.summary_table = QTableWidget(0, 3)
        self.summary_table.setHorizontalHeaderLabels(
            ["Metric", "Value", "Interpretation"]
        )
        self.summary_table.horizontalHeader().setStretchLastSection(True)
        self.summary_table.setMaximumHeight(150)
        summary_layout.addWidget(self.summary_table)

        right_layout.addWidget(summary_group)

        splitter.addWidget(right_widget)
        splitter.setSizes([300, 500])

        button_layout = QHBoxLayout()

        self.export_analytics_btn = QPushButton("Export Analytics")
        self.export_analytics_btn.clicked.connect(self.export_analytics_data)
        button_layout.addWidget(self.export_analytics_btn)

        self.clear_history_btn = QPushButton("Clear History")
        self.clear_history_btn.clicked.connect(self.clear_stress_history)
        button_layout.addWidget(self.clear_history_btn)

        button_layout.addStretch()

        self.auto_scroll_cb = QCheckBox("Auto-scroll Charts")
        self.auto_scroll_cb.setChecked(True)
        button_layout.addWidget(self.auto_scroll_cb)

        layout.addLayout(button_layout)

    def update_analytics(self) -> None:

        if not self.gsr_receiver:
            return

        try:
            stress_summary = self.gsr_receiver.get_stress_summary()
            self.update_stress_table(stress_summary)

            alerts = self.gsr_receiver.get_analytics_alerts()
            self.update_alerts_display(alerts)

            self.update_stress_plots(stress_summary)

            self.update_feature_display()

    def update_stress_table(self, stress_summary: None = Dict) -> None:

        sessions = stress_summary.get("sessions", {})

        self.stress_table.setRowCount(len(sessions))

        for row, (session_key, data) in enumerate(sessions.items()):
            device_id = data.get("device_id", "Unknown")
            stress_score = data.get("latest_stress_score", 0)
            stress_level = data.get("latest_stress_level", "unknown")
            confidence = data.get("confidence", 0)

            stress_item = QTableWidgetItem(f"{stress_score:.1f}")
            if stress_score > 80:
                stress_item.setBackground(QColor(255, 200, 200))
            elif stress_score > 60:
                stress_item.setBackground(QColor(255, 255, 200))
            else:
                stress_item.setBackground(QColor(200, 255, 200))

            level_item = QTableWidgetItem(stress_level.replace("_", " ").title())
            confidence_item = QTableWidgetItem(f"{confidence:.1f}%")

            self.stress_table.setItem(row, 0, QTableWidgetItem(device_id))
            self.stress_table.setItem(row, 1, stress_item)
            self.stress_table.setItem(row, 2, level_item)
            self.stress_table.setItem(row, 3, confidence_item)

    def update_alerts_display(self, alerts: None = List[Dict]) -> None:

        if not alerts:
            self.alerts_text.setText("No alerts")
            return

        alert_text = ""
        for alert in alerts[-5:]:
            timestamp = datetime.fromtimestamp(alert["timestamp"]).strftime("%H:%M:%S")
            device = alert["device_id"]
            message = alert["message"]
            alert_text += f"[{timestamp}] {device}: {message}\n"

        self.alerts_text.setText(alert_text)

        cursor = self.alerts_text.textCursor()
        cursor.movePosition(cursor.MoveOperation.End)
        self.alerts_text.setTextCursor(cursor)

    def update_stress_plots(self, stress_summary: None = Dict) -> None:

        current_time = datetime.now().timestamp()

        sessions = stress_summary.get("sessions", {})

        for session_key, data in sessions.items():
            device_id = data.get("device_id", "Unknown")
            stress_score = data.get("latest_stress_score", 0)

            if device_id not in self.stress_history:
                self.stress_history[device_id] = {"times": [], "scores": []}

                color = ["r", "g", "b", "c", "m", "y"][self.color_index % 6]
                self.stress_curves[device_id] = self.stress_plot.plot(
                    pen=mkPen(color, width=2), name=device_id
                )
                self.color_index += 1

            self.stress_history[device_id]["times"].append(current_time)
            self.stress_history[device_id]["scores"].append(stress_score)

            if len(self.stress_history[device_id]["times"]) > 100:
                self.stress_history[device_id]["times"] = self.stress_history[
                    device_id
                ]["times"][-100:]
                self.stress_history[device_id]["scores"] = self.stress_history[
                    device_id
                ]["scores"][-100:]

            if device_id in self.stress_curves:
                times = self.stress_history[device_id]["times"]
                scores = self.stress_history[device_id]["scores"]

                if times:
                    start_time = times[0]
                    rel_times = [(t - start_time) for t in times]
                    self.stress_curves[device_id].setData(rel_times, scores)

        if self.auto_scroll_cb.isChecked() and self.stress_history:
            all_times = []
            for history in self.stress_history.values():
                all_times.extend(history["times"])

            if all_times:
                latest_time = max(all_times)
                earliest_time = min(all_times)
                time_range = latest_time - earliest_time

                if time_range > 0:
                    self.stress_plot.setXRange(0, time_range, padding=0.1)

    def update_feature_display(self) -> None:

        if not self.gsr_receiver or not self.stress_table.currentRow() >= 0:
            return

        try:

            row = self.stress_table.currentRow()
            if row < 0 or row >= self.stress_table.rowCount():
                return

            device_item = self.stress_table.item(row, 0)
            if not device_item:
                return

            device_id = device_item.text()

            active_sessions = getattr(self.gsr_receiver, "active_sessions", {})
            session_id = None

            for session_key, session in active_sessions.items():
                if session.device_id == device_id:
                    session_id = session.session_id
                    break

            if not session_id:
                return

            analytics = self.gsr_receiver.get_real_time_analytics(device_id, session_id)
            if not analytics:
                return

            feature_mapping = {
                "Mean GSR": analytics.get("mean_gsr", 0),
                "Peak Frequency": analytics.get("peak_frequency", 0),
                "Rising Time": analytics.get("rising_time", 0),
                "Rapid Changes": analytics.get("rapid_changes", 0),
                "Trend Slope": analytics.get("trend_slope", 0),
                "Spectral Entropy": 0,
            }

            for name, value in feature_mapping.items():
                if name in self.feature_labels:
                    if isinstance(value, float):
                        self.feature_labels[name].setText(f"{value:.2f}")
                    else:
                        self.feature_labels[name].setText(str(value))

            self.update_session_summary(analytics)

    def update_session_summary(self, analytics: None = Dict) -> None:

        summary_data = [
            (
                "Current Stress Score",
                f"{analytics.get('stress_score', 0):.1f}/100",
                self.interpret_stress_score(analytics.get("stress_score", 0)),
            ),
            (
                "Confidence Level",
                f"{analytics.get('confidence', 0):.1f}%",
                self.interpret_confidence(analytics.get("confidence", 0)),
            ),
            (
                "GSR Trend",
                f"{analytics.get('trend_slope', 0):.3f} μS/s",
                self.interpret_trend(analytics.get("trend_slope", 0)),
            ),
            (
                "Signal Stability",
                f"{analytics.get('rapid_changes', 0)} changes",
                self.interpret_stability(analytics.get("rapid_changes", 0)),
            ),
        ]

        self.summary_table.setRowCount(len(summary_data))

        for row, (metric, value, interpretation) in enumerate(summary_data):
            self.summary_table.setItem(row, 0, QTableWidgetItem(metric))
            self.summary_table.setItem(row, 1, QTableWidgetItem(value))
            self.summary_table.setItem(row, 2, QTableWidgetItem(interpretation))

    def interpret_stress_score(self, score: float) -> str:

        if score > 80:
            return "Very High - Consider intervention"
        elif score > 60:
            return "High - Monitor closely"
        elif score > 40:
            return "Moderate - Normal range"
        elif score > 20:
            return "Low - Relaxed state"
        else:
            return "Very Low - Calm state"

    def interpret_confidence(self, confidence: float) -> str:

        if confidence > 80:
            return "High confidence"
        elif confidence > 60:
            return "Good confidence"
        elif confidence > 40:
            return "Moderate confidence"
        else:
            return "Low confidence - Check signal quality"

    def interpret_trend(self, slope: float) -> str:

        if abs(slope) < 0.001:
            return "Stable"
        elif slope > 0.001:
            return "Increasing stress"
        else:
            return "Decreasing stress"

    def interpret_stability(self, rapid_changes: int) -> str:

        if rapid_changes > 30:
            return "Very unstable"
        elif rapid_changes > 15:
            return "Moderately unstable"
        elif rapid_changes > 5:
            return "Slightly unstable"
        else:
            return "Stable"

    def export_analytics_data(self) -> Any:

        try:
            filename, _ = QFileDialog.getSaveFileName(
                self,
                "Export Analytics Data",
                f"gsr_analytics_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json",
                "JSON files (*.json);;CSV files (*.csv);;All files (*.*)",
            )

            if not filename:
                return

            if not self.gsr_receiver:
                QMessageBox.warning(self, "No Data", "No GSR receiver available")
                return

            stress_summary = self.gsr_receiver.get_stress_summary()
            alerts = self.gsr_receiver.get_analytics_alerts()

            export_data = {
                "timestamp": datetime.now().isoformat(),
                "stress_summary": stress_summary,
                "alerts": alerts,
                "stress_history": {
                    device_id: {
                        "times": [
                            datetime.fromtimestamp(t).isoformat()
                            for t in history["times"]
                        ],
                        "scores": history["scores"],
                    }
                    for device_id, history in self.stress_history.items()
                },
            }

            if filename.endswith(".json"):
                import json

                with open(filename, "w") as f:
                    json.dump(export_data, f, indent=2)
            else:

                import pandas as pd

                csv_data = []
                for device_id, history in self.stress_history.items():
                    for time_val, score in zip(history["times"], history["scores"]):
                        csv_data.append(
                            {
                                "device_id": device_id,
                                "timestamp": datetime.fromtimestamp(
                                    time_val
                                ).isoformat(),
                                "stress_score": score,
                            }
                        )

                df = pd.DataFrame(csv_data)
                df.to_csv(filename, index=False)

            QMessageBox.information(
                self, "Export Complete", f"Analytics data exported to:\n{filename}"
            )

    def clear_stress_history(self) -> None:

        self.stress_history.clear()

        for curve in self.stress_curves.values():
            self.stress_plot.removeItem(curve)
        self.stress_curves.clear()
        self.color_index = 0

        for label in self.feature_labels.values():
            label.setText("--")

        self.summary_table.setRowCount(0)


class GSRMainTabWidget(QTabWidget):

    def __init__(self, gsr_receiver=None, parent=None):
        super().__init__(parent)
        self.gsr_receiver = gsr_receiver
        self.init_ui()

    def init_ui(self) -> Any:
        self.device_widget = GSRDeviceStatusWidget()
        self.addTab(self.device_widget, "Device Status")

        # self.monitor_widget = GSRMonitorWidget(self.gsr_receiver)
        # self.addTab(self.monitor_widget, "Real-time Monitor")

        self.analytics_widget = GSRAnalyticsWidget(self.gsr_receiver)
        self.addTab(self.analytics_widget, "Analytics & Stress")

        # self.export_widget = GSRDataExportWidget(self.gsr_receiver)
        # self.addTab(self.export_widget, "Data Export")

    def set_gsr_receiver(self, gsr_receiver) -> None:
        self.gsr_receiver = gsr_receiver
        # self.monitor_widget.gsr_receiver = gsr_receiver
        self.analytics_widget.gsr_receiver = gsr_receiver
        # self.export_widget.gsr_receiver = gsr_receiver
