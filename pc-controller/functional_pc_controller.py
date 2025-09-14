#!/usr/bin/env python3
"""
Functional PC Controller for Multi-Modal Physiological Sensing Platform

This is a working implementation that provides:
1. Real PyQt6 GUI interface
2. TCP server for Android device connections
3. Live GSR data visualization
4. Session recording and CSV export
5. Device management and control
"""

import csv
import json
import socket
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

try:
    from PyQt6.QtWidgets import (
        QApplication, QMainWindow, QVBoxLayout, QHBoxLayout,
        QWidget, QPushButton, QTextEdit, QLabel, QLineEdit,
        QTableWidget, QTableWidgetItem, QTabWidget, QGroupBox,
        QGridLayout, QProgressBar, QComboBox, QSpinBox
    )
    from PyQt6.QtCore import QThread, pyqtSignal, QTimer, Qt
    from PyQt6.QtGui import QFont, QColor, QPalette

    try:
        import pyqtgraph as pg

        PYQTGRAPH_AVAILABLE = True
    except ImportError:
        PYQTGRAPH_AVAILABLE = False

    GUI_AVAILABLE = True
except ImportError:
    GUI_AVAILABLE = False


class NetworkThread(QThread):
    """Handle TCP server for Android device connections"""

    device_connected = pyqtSignal(str, str)  # device_id, address
    device_disconnected = pyqtSignal(str)
    data_received = pyqtSignal(str, dict)  # device_id, data

    def __init__(self, port=8080):
        super().__init__()
        self.port = port
        self.server_socket = None
        self.running = False
        self.connections = {}  # device_id -> socket

    def run(self):
        """Run TCP server"""
        self.running = True
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        try:
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            print(f"PC Controller listening on port {self.port}")

            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    print(f"New connection from {address}")

                    # Start client handler thread
                    client_thread = threading.Thread(
                        target=self.handle_client,
                        args=(client_socket, address)
                    )
                    client_thread.daemon = True
                    client_thread.start()

                except socket.error:
                    if self.running:
                        print("Socket error in server")
                    break

        except Exception as e:
            print(f"Server error: {e}")
        finally:
            if self.server_socket:
                self.server_socket.close()

    def handle_client(self, client_socket, address):
        """Handle individual Android device connection"""
        device_id = None
        buffer = ""

        try:
            while self.running:
                data = client_socket.recv(1024).decode('utf-8')
                if not data:
                    break

                buffer += data
                lines = buffer.split('\n')
                buffer = lines[-1]  # Keep incomplete line in buffer

                for line in lines[:-1]:
                    if line.strip():
                        try:
                            message = json.loads(line.strip())

                            if message.get('type') == 'device_info':
                                device_id = message.get('device_id', f'device_{address[0]}')
                                self.connections[device_id] = client_socket
                                self.device_connected.emit(device_id, f"{address[0]}:{address[1]}")

                            if device_id:
                                self.data_received.emit(device_id, message)

                        except json.JSONDecodeError:
                            print(f"Invalid JSON from {address}: {line}")

        except Exception as e:
            print(f"Client handler error: {e}")
        finally:
            client_socket.close()
            if device_id:
                self.device_disconnected.emit(device_id)
                if device_id in self.connections:
                    del self.connections[device_id]

    def send_command(self, device_id: str, command: dict):
        """Send command to specific device"""
        if device_id in self.connections:
            try:
                message = json.dumps(command) + '\n'
                self.connections[device_id].send(message.encode('utf-8'))
                return True
            except Exception as e:
                print(f"Failed to send command to {device_id}: {e}")
        return False

    def stop(self):
        """Stop the server"""
        self.running = False
        if self.server_socket:
            self.server_socket.close()


class FunctionalPCController(QMainWindow):
    """Main PC Controller Application"""

    def __init__(self):
        super().__init__()
        self.setWindowTitle("Multi-Modal Physiological Sensing Platform - PC Controller")
        self.setGeometry(100, 100, 1200, 800)

        # Data storage
        self.devices = {}  # device_id -> device_info
        self.gsr_data = {}  # device_id -> list of (timestamp, gsr_value)
        self.current_session = None
        self.session_start_time = None

        # Network
        self.network_thread = NetworkThread(port=8080)
        self.network_thread.device_connected.connect(self.on_device_connected)
        self.network_thread.device_disconnected.connect(self.on_device_disconnected)
        self.network_thread.data_received.connect(self.on_data_received)

        self.setup_ui()
        self.start_server()

    def setup_ui(self):
        """Setup the user interface"""
        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        # Create tab widget
        tabs = QTabWidget()

        # Dashboard tab
        dashboard_tab = self.create_dashboard_tab()
        tabs.addTab(dashboard_tab, "Dashboard")

        # Devices tab
        devices_tab = self.create_devices_tab()
        tabs.addTab(devices_tab, "Devices")

        # Data tab
        data_tab = self.create_data_tab()
        tabs.addTab(data_tab, "Data")

        # Layout
        layout = QVBoxLayout()
        layout.addWidget(tabs)
        central_widget.setLayout(layout)

        # Status bar
        self.statusBar().showMessage("PC Controller Ready - Listening for Android devices...")

    def create_dashboard_tab(self) -> QWidget:
        """Create main dashboard tab"""
        widget = QWidget()
        layout = QVBoxLayout()

        # Session controls
        session_group = QGroupBox("Session Control")
        session_layout = QHBoxLayout()

        self.session_label = QLabel("No active session")
        self.session_label.setStyleSheet("font-weight: bold; color: #e74c3c;")

        self.start_button = QPushButton("Start Recording")
        self.start_button.clicked.connect(self.start_session)
        self.start_button.setStyleSheet(
            "QPushButton { background-color: #27ae60; color: white; font-weight: bold; padding: 8px; }")

        self.stop_button = QPushButton("Stop Recording")
        self.stop_button.clicked.connect(self.stop_session)
        self.stop_button.setEnabled(False)
        self.stop_button.setStyleSheet(
            "QPushButton { background-color: #e74c3c; color: white; font-weight: bold; padding: 8px; }")

        session_layout.addWidget(self.session_label)
        session_layout.addStretch()
        session_layout.addWidget(self.start_button)
        session_layout.addWidget(self.stop_button)
        session_group.setLayout(session_layout)

        # Device status
        device_group = QGroupBox("Connected Devices")
        device_layout = QVBoxLayout()

        self.device_status_label = QLabel("No devices connected")
        self.device_status_label.setStyleSheet("color: #7f8c8d;")
        device_layout.addWidget(self.device_status_label)

        device_group.setLayout(device_layout)

        # GSR visualization
        if PYQTGRAPH_AVAILABLE:
            gsr_group = QGroupBox("Live GSR Data")
            gsr_layout = QVBoxLayout()

            self.gsr_plot = pg.PlotWidget(title="GSR (μS)")
            self.gsr_plot.setLabel('left', 'GSR (μS)')
            self.gsr_plot.setLabel('bottom', 'Time (s)')
            self.gsr_plot.showGrid(x=True, y=True)

            gsr_layout.addWidget(self.gsr_plot)
            gsr_group.setLayout(gsr_layout)
        else:
            gsr_group = QGroupBox("Live GSR Data")
            gsr_layout = QVBoxLayout()
            gsr_layout.addWidget(QLabel("PyQtGraph not available for plotting"))
            gsr_group.setLayout(gsr_layout)

        # Add to main layout
        layout.addWidget(session_group)
        layout.addWidget(device_group)
        if PYQTGRAPH_AVAILABLE:
            layout.addWidget(gsr_group)

        widget.setLayout(layout)
        return widget

    def create_devices_tab(self) -> QWidget:
        """Create devices management tab"""
        widget = QWidget()
        layout = QVBoxLayout()

        # Device table
        self.device_table = QTableWidget()
        self.device_table.setColumnCount(4)
        self.device_table.setHorizontalHeaderLabels(["Device ID", "Address", "Status", "Last Data"])

        layout.addWidget(QLabel("Connected Android Devices:"))
        layout.addWidget(self.device_table)

        # Commands
        commands_group = QGroupBox("Device Commands")
        commands_layout = QGridLayout()

        sync_button = QPushButton("Send Sync Flash")
        sync_button.clicked.connect(self.send_sync_flash)

        test_button = QPushButton("Test Connection")
        test_button.clicked.connect(self.test_connections)

        commands_layout.addWidget(sync_button, 0, 0)
        commands_layout.addWidget(test_button, 0, 1)
        commands_group.setLayout(commands_layout)

        layout.addWidget(commands_group)

        widget.setLayout(layout)
        return widget

    def create_data_tab(self) -> QWidget:
        """Create data management tab"""
        widget = QWidget()
        layout = QVBoxLayout()

        # Export controls
        export_group = QGroupBox("Data Export")
        export_layout = QHBoxLayout()

        export_button = QPushButton("Export to CSV")
        export_button.clicked.connect(self.export_data)

        self.export_status = QLabel("No data to export")

        export_layout.addWidget(export_button)
        export_layout.addWidget(self.export_status)
        export_layout.addStretch()

        export_group.setLayout(export_layout)

        # Data summary
        summary_group = QGroupBox("Data Summary")
        summary_layout = QVBoxLayout()

        self.data_summary = QTextEdit()
        self.data_summary.setReadOnly(True)
        self.data_summary.setMaximumHeight(200)

        summary_layout.addWidget(self.data_summary)
        summary_group.setLayout(summary_layout)

        layout.addWidget(export_group)
        layout.addWidget(summary_group)
        layout.addStretch()

        widget.setLayout(layout)
        return widget

    def start_server(self):
        """Start the network server"""
        self.network_thread.start()

    def start_session(self):
        """Start recording session"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.current_session = f"session_{timestamp}"
        self.session_start_time = time.time()

        # Clear previous data
        self.gsr_data.clear()

        # Update UI
        self.session_label.setText(f"Active: {self.current_session}")
        self.session_label.setStyleSheet("font-weight: bold; color: #27ae60;")
        self.start_button.setEnabled(False)
        self.stop_button.setEnabled(True)

        # Send start command to all devices
        start_command = {
            "type": "session_start",
            "session_id": self.current_session,
            "timestamp": datetime.now().isoformat()
        }

        for device_id in self.devices.keys():
            self.network_thread.send_command(device_id, start_command)

        self.statusBar().showMessage(f"Recording session started: {self.current_session}")

    def stop_session(self):
        """Stop recording session"""
        if not self.current_session:
            return

        # Send stop command to all devices
        stop_command = {
            "type": "session_stop",
            "session_id": self.current_session,
            "timestamp": datetime.now().isoformat()
        }

        for device_id in self.devices.keys():
            self.network_thread.send_command(device_id, stop_command)

        # Update UI
        self.session_label.setText(f"Completed: {self.current_session}")
        self.session_label.setStyleSheet("font-weight: bold; color: #f39c12;")
        self.start_button.setEnabled(True)
        self.stop_button.setEnabled(False)

        self.statusBar().showMessage(f"Recording session completed: {self.current_session}")

        # Auto-export data
        self.export_data()

        self.current_session = None
        self.session_start_time = None

    def send_sync_flash(self):
        """Send synchronization flash command to all devices"""
        sync_command = {
            "type": "sync_flash",
            "timestamp": datetime.now().isoformat()
        }

        for device_id in self.devices.keys():
            success = self.network_thread.send_command(device_id, sync_command)
            if success:
                self.statusBar().showMessage(f"Sync flash sent to {device_id}")

    def test_connections(self):
        """Test connection to all devices"""
        test_command = {
            "type": "test_connection",
            "timestamp": datetime.now().isoformat()
        }

        for device_id in self.devices.keys():
            success = self.network_thread.send_command(device_id, test_command)
            status = "✓" if success else "✗"
            self.statusBar().showMessage(f"Connection test {status} {device_id}")

    def export_data(self):
        """Export collected data to CSV"""
        if not self.gsr_data:
            self.export_status.setText("No data to export")
            return

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"gsr_data_{timestamp}.csv"

        try:
            with open(filename, 'w', newline='') as csvfile:
                writer = csv.writer(csvfile)
                writer.writerow(["timestamp", "device_id", "gsr_value", "session_id"])

                for device_id, data_points in self.gsr_data.items():
                    for timestamp, gsr_value in data_points:
                        writer.writerow(
                            [timestamp, device_id, gsr_value, self.current_session or "unknown"])

            self.export_status.setText(f"Exported to {filename}")
            self.statusBar().showMessage(f"Data exported to {filename}")

        except Exception as e:
            self.export_status.setText(f"Export failed: {e}")

    def on_device_connected(self, device_id: str, address: str):
        """Handle device connection"""
        self.devices[device_id] = {
            "address": address,
            "connected_at": datetime.now(),
            "last_data": None
        }

        self.update_device_display()
        self.statusBar().showMessage(f"Device connected: {device_id} ({address})")

    def on_device_disconnected(self, device_id: str):
        """Handle device disconnection"""
        if device_id in self.devices:
            del self.devices[device_id]

        self.update_device_display()
        self.statusBar().showMessage(f"Device disconnected: {device_id}")

    def on_data_received(self, device_id: str, data: dict):
        """Handle incoming data from devices"""
        if device_id in self.devices:
            self.devices[device_id]["last_data"] = datetime.now()

        # Handle GSR data
        if data.get("type") == "gsr_data":
            gsr_value = data.get("gsr_value", 0)
            timestamp = time.time()

            if device_id not in self.gsr_data:
                self.gsr_data[device_id] = []

            self.gsr_data[device_id].append((timestamp, gsr_value))

            # Update plot if available
            if PYQTGRAPH_AVAILABLE and hasattr(self, 'gsr_plot'):
                self.update_gsr_plot()

        self.update_device_display()
        self.update_data_summary()

    def update_device_display(self):
        """Update device status display"""
        count = len(self.devices)
        if count == 0:
            self.device_status_label.setText("No devices connected")
        else:
            device_list = ", ".join(self.devices.keys())
            self.device_status_label.setText(f"{count} device(s): {device_list}")

        # Update device table
        self.device_table.setRowCount(len(self.devices))

        for row, (device_id, info) in enumerate(self.devices.items()):
            self.device_table.setItem(row, 0, QTableWidgetItem(device_id))
            self.device_table.setItem(row, 1, QTableWidgetItem(info["address"]))
            self.device_table.setItem(row, 2, QTableWidgetItem("Connected"))

            last_data = info.get("last_data")
            last_data_str = last_data.strftime("%H:%M:%S") if last_data else "No data"
            self.device_table.setItem(row, 3, QTableWidgetItem(last_data_str))

    def update_gsr_plot(self):
        """Update GSR plot with latest data"""
        if not PYQTGRAPH_AVAILABLE or not hasattr(self, 'gsr_plot'):
            return

        self.gsr_plot.clear()

        for device_id, data_points in self.gsr_data.items():
            if data_points:
                timestamps, gsr_values = zip(*data_points[-100:])  # Last 100 points

                # Convert to relative time
                if self.session_start_time:
                    rel_timestamps = [(t - self.session_start_time) for t in timestamps]
                else:
                    rel_timestamps = list(range(len(timestamps)))

                self.gsr_plot.plot(rel_timestamps, gsr_values, pen=pg.mkPen(width=2),
                                   name=device_id)

    def update_data_summary(self):
        """Update data summary display"""
        summary = f"Session: {self.current_session or 'None'}\n"
        summary += f"Connected Devices: {len(self.devices)}\n\n"

        for device_id, data_points in self.gsr_data.items():
            if data_points:
                latest_gsr = data_points[-1][1]
                count = len(data_points)
                summary += f"{device_id}: {count} samples, latest GSR: {latest_gsr:.1f} μS\n"

        self.data_summary.setPlainText(summary)

    def closeEvent(self, event):
        """Handle application closing"""
        self.network_thread.stop()
        self.network_thread.wait(3000)  # Wait up to 3 seconds
        event.accept()


def main():
    """Main application entry point"""
    if not GUI_AVAILABLE:
        print("PyQt6 not available. Install with: pip install PyQt6")
        print("Optional: pip install pyqtgraph (for live GSR plotting)")
        return 1

    app = QApplication(sys.argv)
    app.setApplicationName("PC Controller")
    app.setOrganizationName("Multi-Modal Sensing Platform")

    # Set application style
    app.setStyle('Fusion')

    window = FunctionalPCController()
    window.show()

    return app.exec()


if __name__ == "__main__":
    sys.exit(main())
