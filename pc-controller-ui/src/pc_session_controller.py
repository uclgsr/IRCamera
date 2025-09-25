#!/usr/bin/env python3
"""
PC Session Controller - Desktop GUI for IRCamera Control
Implements requirements for:
- Session control panel with device list
- Device and sensor status display
- Real-time telemetry visualization
- Command acknowledgment and session logging
"""

import json
import socket
import sys
import threading
import time
import tkinter as tk
from datetime import datetime
from tkinter import ttk, scrolledtext, messagebox
from typing import Dict, List, Optional, Tuple

try:
    import matplotlib.pyplot as plt
    from matplotlib.backends.backend_tkagg import FigureCanvasTkinter
    import numpy as np

    MATPLOTLIB_AVAILABLE = True
except ImportError:
    MATPLOTLIB_AVAILABLE = False
    print("Warning: matplotlib not available, GSR plotting will be disabled")


class DeviceStatus:
    """Represents the status of a connected device"""

    def __init__(self, device_id: str, device_name: str, ip_address: str):
        self.device_id = device_id
        self.device_name = device_name
        self.ip_address = ip_address
        self.status = "Disconnected"
        self.recording = False
        self.sensors = {
            'RGB': {'status': 'Disconnected', 'message': ''},
            'Thermal': {'status': 'Disconnected', 'message': ''},
            'GSR': {'status': 'Disconnected', 'message': ''}
        }
        self.time_offset_ms = 0
        self.round_trip_time_ms = 0
        self.last_update = time.time()


class SessionController:
    """Main PC Session Controller GUI Application"""

    def __init__(self):
        self.root = tk.Tk()
        self.root.title("IRCamera PC Session Controller")
        self.root.geometry("1200x800")
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)

        # Data
        self.devices: Dict[str, DeviceStatus] = {}
        self.session_active = False
        self.session_start_time = None
        self.gsr_data_buffer = []
        self.max_gsr_samples = 300  # Show last 30 seconds at 10Hz

        # Network
        self.server_socket = None
        self.client_connections = {}
        self.server_port = 8080
        self.server_running = False

        # Setup GUI
        self.setup_ui()
        self.start_server()

    def setup_ui(self):
        """Initialize the GUI components"""
        # Main container
        main_frame = ttk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # Control panel (top)
        self.setup_control_panel(main_frame)

        # Device list (left)
        self.setup_device_panel(main_frame)

        # Telemetry visualization (right)
        self.setup_telemetry_panel(main_frame)

        # Session log (bottom)
        self.setup_log_panel(main_frame)

    def setup_control_panel(self, parent):
        """Setup main control buttons"""
        control_frame = ttk.LabelFrame(parent, text="Session Control", padding=10)
        control_frame.pack(fill=tk.X, pady=(0, 10))

        button_frame = ttk.Frame(control_frame)
        button_frame.pack()

        # Main control buttons
        self.start_button = ttk.Button(
            button_frame, text="Start All", command=self.start_all_recording
        )
        self.start_button.pack(side=tk.LEFT, padx=5)

        self.stop_button = ttk.Button(
            button_frame, text="Stop All", command=self.stop_all_recording
        )
        self.stop_button.pack(side=tk.LEFT, padx=5)

        self.sync_button = ttk.Button(
            button_frame, text="Sync Clocks", command=self.sync_all_clocks
        )
        self.sync_button.pack(side=tk.LEFT, padx=5)

        self.refresh_button = ttk.Button(
            button_frame, text="Refresh Status", command=self.refresh_all_status
        )
        self.refresh_button.pack(side=tk.LEFT, padx=5)

        # Session status
        status_frame = ttk.Frame(control_frame)
        status_frame.pack(fill=tk.X, pady=(10, 0))

        self.session_status_label = ttk.Label(
            status_frame, text="Session Status: Idle", font=("Arial", 12, "bold")
        )
        self.session_status_label.pack(side=tk.LEFT)

        self.session_timer_label = ttk.Label(
            status_frame, text="00:00:00", font=("Arial", 12)
        )
        self.session_timer_label.pack(side=tk.RIGHT)

    def setup_device_panel(self, parent):
        """Setup device list and status display"""
        # Create horizontal container
        content_frame = ttk.Frame(parent)
        content_frame.pack(fill=tk.BOTH, expand=True)

        # Device panel (left side)
        device_frame = ttk.LabelFrame(content_frame, text="Connected Devices", padding=10)
        device_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(0, 5))

        # Device list with scrollbar
        list_frame = ttk.Frame(device_frame)
        list_frame.pack(fill=tk.BOTH, expand=True)

        # Treeview for device list
        columns = ("Device", "Status", "RGB", "Thermal", "GSR", "Time Sync")
        self.device_tree = ttk.Treeview(list_frame, columns=columns, show="headings", height=10)

        # Configure columns
        for col in columns:
            self.device_tree.heading(col, text=col)
            self.device_tree.column(col, width=100, minwidth=80)

        # Scrollbar
        scrollbar = ttk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.device_tree.yview)
        self.device_tree.configure(yscrollcommand=scrollbar.set)

        self.device_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # Individual device controls
        controls_frame = ttk.Frame(device_frame)
        controls_frame.pack(fill=tk.X, pady=(10, 0))

        ttk.Button(controls_frame, text="Start Selected",
                   command=self.start_selected_device).pack(side=tk.LEFT, padx=5)
        ttk.Button(controls_frame, text="Stop Selected",
                   command=self.stop_selected_device).pack(side=tk.LEFT, padx=5)
        ttk.Button(controls_frame, text="Sync Selected",
                   command=self.sync_selected_device).pack(side=tk.LEFT, padx=5)

    def setup_telemetry_panel(self, parent):
        """Setup real-time telemetry visualization"""
        content_frame = parent.winfo_children()[-1]  # Get the content frame

        telemetry_frame = ttk.LabelFrame(content_frame, text="Real-Time Telemetry", padding=10)
        telemetry_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=(5, 0))

        # Notebook for different telemetry views
        self.telemetry_notebook = ttk.Notebook(telemetry_frame)
        self.telemetry_notebook.pack(fill=tk.BOTH, expand=True)

        # GSR Plot Tab
        if MATPLOTLIB_AVAILABLE:
            self.setup_gsr_plot_tab()
        else:
            self.setup_gsr_text_tab()

        # Video Preview Tab
        self.setup_video_preview_tab()

        # Thermal Data Tab
        self.setup_thermal_data_tab()

    def setup_gsr_plot_tab(self):
        """Setup GSR signal plotting"""
        gsr_frame = ttk.Frame(self.telemetry_notebook)
        self.telemetry_notebook.add(gsr_frame, text="GSR Signal")

        # Create matplotlib figure
        self.gsr_fig, self.gsr_ax = plt.subplots(figsize=(6, 4))
        self.gsr_ax.set_title("GSR Signal (Real-time)")
        self.gsr_ax.set_xlabel("Time (seconds)")
        self.gsr_ax.set_ylabel("GSR Value")
        self.gsr_ax.grid(True)

        # Initialize plot line
        self.gsr_line, = self.gsr_ax.plot([], [], 'b-', linewidth=1)
        self.gsr_ax.set_xlim(0, 30)  # Show last 30 seconds
        self.gsr_ax.set_ylim(0, 1000)  # Adjust based on GSR range

        # Embed plot in tkinter
        self.gsr_canvas = FigureCanvasTkinter(self.gsr_fig, gsr_frame)
        self.gsr_canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

        # GSR stats
        stats_frame = ttk.Frame(gsr_frame)
        stats_frame.pack(fill=tk.X, pady=(5, 0))

        self.gsr_stats_label = ttk.Label(stats_frame, text="GSR: No data")
        self.gsr_stats_label.pack(side=tk.LEFT)

    def setup_gsr_text_tab(self):
        """Setup GSR data display as text (fallback when matplotlib not available)"""
        gsr_frame = ttk.Frame(self.telemetry_notebook)
        self.telemetry_notebook.add(gsr_frame, text="GSR Data")

        self.gsr_text = scrolledtext.ScrolledText(gsr_frame, height=10, width=40)
        self.gsr_text.pack(fill=tk.BOTH, expand=True)

        # GSR stats
        stats_frame = ttk.Frame(gsr_frame)
        stats_frame.pack(fill=tk.X, pady=(5, 0))

        self.gsr_stats_label = ttk.Label(stats_frame, text="GSR: No data")
        self.gsr_stats_label.pack(side=tk.LEFT)

    def setup_video_preview_tab(self):
        """Setup video frame preview"""
        video_frame = ttk.Frame(self.telemetry_notebook)
        self.telemetry_notebook.add(video_frame, text="Video Preview")

        self.video_preview_label = ttk.Label(video_frame, text="No video feed", font=("Arial", 14))
        self.video_preview_label.pack(expand=True)

        # Video stats
        video_stats_frame = ttk.Frame(video_frame)
        video_stats_frame.pack(fill=tk.X, pady=(5, 0))

        self.video_stats_label = ttk.Label(video_stats_frame, text="Video: No frames received")
        self.video_stats_label.pack()

    def setup_thermal_data_tab(self):
        """Setup thermal data display"""
        thermal_frame = ttk.Frame(self.telemetry_notebook)
        self.telemetry_notebook.add(thermal_frame, text="Thermal Data")

        self.thermal_preview_label = ttk.Label(thermal_frame, text="No thermal data", font=("Arial", 14))
        self.thermal_preview_label.pack(expand=True)

        # Thermal stats
        thermal_stats_frame = ttk.Frame(thermal_frame)
        thermal_stats_frame.pack(fill=tk.X, pady=(5, 0))

        self.thermal_stats_label = ttk.Label(thermal_stats_frame, text="Thermal: No data")
        self.thermal_stats_label.pack()

    def setup_log_panel(self, parent):
        """Setup session logging console"""
        log_frame = ttk.LabelFrame(parent, text="Session Log", padding=10)
        log_frame.pack(fill=tk.BOTH, expand=True, pady=(10, 0))

        # Log text area
        self.log_text = scrolledtext.ScrolledText(
            log_frame, height=8, wrap=tk.WORD,
            font=("Consolas", 9)
        )
        self.log_text.pack(fill=tk.BOTH, expand=True)

        # Log controls
        log_controls = ttk.Frame(log_frame)
        log_controls.pack(fill=tk.X, pady=(5, 0))

        ttk.Button(log_controls, text="Clear Log",
                   command=self.clear_log).pack(side=tk.LEFT)
        ttk.Button(log_controls, text="Save Log",
                   command=self.save_log).pack(side=tk.LEFT, padx=(5, 0))

    def start_server(self):
        """Start the TCP server for device connections"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('', self.server_port))
            self.server_socket.listen(5)
            self.server_running = True

            self.log_message(f"Server started on port {self.server_port}")

            # Start server thread
            server_thread = threading.Thread(target=self.server_loop, daemon=True)
            server_thread.start()

            # Start GUI update timer
            self.update_gui()

        except Exception as e:
            self.log_message(f"Failed to start server: {e}", "ERROR")
            messagebox.showerror("Server Error", f"Failed to start server: {e}")

    def server_loop(self):
        """Main server loop to handle client connections"""
        while self.server_running:
            try:
                self.server_socket.settimeout(1.0)  # Add timeout to allow checking server_running
                client_socket, address = self.server_socket.accept()
                self.log_message(f"New connection from {address[0]}:{address[1]}")

                # Start client handler thread
                client_thread = threading.Thread(
                    target=self.handle_client,
                    args=(client_socket, address),
                    daemon=True
                )
                client_thread.start()

            except socket.timeout:
                continue  # Check server_running flag
            except Exception as e:
                if self.server_running:
                    self.log_message(f"Server error: {e}", "ERROR")

    def handle_client(self, client_socket, address):
        """Handle individual client connection"""
        device_id = f"{address[0]}:{address[1]}"
        self.client_connections[device_id] = client_socket

        # Create device status
        device = DeviceStatus(device_id, f"Device-{address[0]}", address[0])
        device.status = "Connected"
        self.devices[device_id] = device

        try:
            while self.server_running:
                client_socket.settimeout(1.0)  # Add timeout
                try:
                    data = client_socket.recv(4096)
                    if not data:
                        break

                    # Process received data
                    try:
                        message = json.loads(data.decode('utf-8'))
                        self.process_device_message(device_id, message)
                    except json.JSONDecodeError:
                        self.log_message(f"Invalid JSON from {device_id}: {data.decode('utf-8', errors='ignore')}",
                                         "WARNING")

                except socket.timeout:
                    continue  # Check server_running flag

        except Exception as e:
            self.log_message(f"Error handling client {device_id}: {e}", "ERROR")
        finally:
            # Cleanup
            client_socket.close()
            if device_id in self.client_connections:
                del self.client_connections[device_id]
            if device_id in self.devices:
                self.devices[device_id].status = "Disconnected"

    def process_device_message(self, device_id: str, message: dict):
        """Process incoming message from device"""
        device = self.devices.get(device_id)
        if not device:
            return

        msg_type = message.get('type', '')

        if msg_type == 'status_update':
            # Update device status
            device.status = message.get('status', 'Unknown')
            sensors = message.get('sensors', {})
            for sensor_name, sensor_data in sensors.items():
                if sensor_name in device.sensors:
                    device.sensors[sensor_name].update(sensor_data)
            device.last_update = time.time()

        elif msg_type == 'telemetry_gsr':
            # GSR telemetry data
            gsr_value = message.get('value', 0)
            timestamp = message.get('timestamp', time.time())
            self.gsr_data_buffer.append((timestamp, gsr_value))

            # Keep buffer size manageable
            if len(self.gsr_data_buffer) > self.max_gsr_samples:
                self.gsr_data_buffer.pop(0)

        elif msg_type == 'recording_started':
            device.recording = True
            session_id = message.get('session_id', 'Unknown')
            self.log_message(f"Recording started on {device.device_name}: {session_id}")

        elif msg_type == 'recording_stopped':
            device.recording = False
            self.log_message(f"Recording stopped on {device.device_name}")

        elif msg_type == 'sync_response':
            device.time_offset_ms = message.get('offset_ms', 0)
            device.round_trip_time_ms = message.get('rtt_ms', 0)
            self.log_message(
                f"Time sync complete for {device.device_name}: offset={device.time_offset_ms}ms, RTT={device.round_trip_time_ms}ms")

    def send_command_to_device(self, device_id: str, command: dict) -> bool:
        """Send command to specific device"""
        if device_id not in self.client_connections:
            self.log_message(f"Device {device_id} not connected", "WARNING")
            return False

        try:
            client_socket = self.client_connections[device_id]
            message = json.dumps(command).encode('utf-8')
            client_socket.send(message)
            return True
        except Exception as e:
            self.log_message(f"Failed to send command to {device_id}: {e}", "ERROR")
            return False

    def start_all_recording(self):
        """Start recording on all connected devices"""
        if not self.devices:
            messagebox.showwarning("No Devices", "No devices connected")
            return

        session_id = f"session_{int(time.time())}"
        success_count = 0

        for device_id in self.devices:
            command = {
                'type': 'start_recording',
                'session_id': session_id,
                'timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                success_count += 1

        self.log_message(f"Start recording command sent to {success_count}/{len(self.devices)} devices")

        if success_count > 0:
            self.session_active = True
            self.session_start_time = time.time()

    def stop_all_recording(self):
        """Stop recording on all connected devices"""
        success_count = 0

        for device_id in self.devices:
            command = {
                'type': 'stop_recording',
                'timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                success_count += 1

        self.log_message(f"Stop recording command sent to {success_count}/{len(self.devices)} devices")

        if success_count > 0:
            self.session_active = False
            self.session_start_time = None

    def sync_all_clocks(self):
        """Synchronize clocks with all devices"""
        success_count = 0

        for device_id in self.devices:
            command = {
                'type': 'sync_request',
                'pc_timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                success_count += 1

        self.log_message(f"Clock sync command sent to {success_count}/{len(self.devices)} devices")

    def refresh_all_status(self):
        """Request status update from all devices"""
        success_count = 0

        for device_id in self.devices:
            command = {
                'type': 'status_request',
                'timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                success_count += 1

        self.log_message(f"Status request sent to {success_count}/{len(self.devices)} devices")

    def start_selected_device(self):
        """Start recording on selected device"""
        selection = self.device_tree.selection()
        if not selection:
            messagebox.showwarning("No Selection", "Please select a device")
            return

        item = self.device_tree.item(selection[0])
        device_name = item['values'][0]

        # Find device by name
        device_id = None
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break

        if device_id:
            session_id = f"session_{device_id}_{int(time.time())}"
            command = {
                'type': 'start_recording',
                'session_id': session_id,
                'timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                self.log_message(f"Start recording command sent to {device_name}")

    def stop_selected_device(self):
        """Stop recording on selected device"""
        selection = self.device_tree.selection()
        if not selection:
            messagebox.showwarning("No Selection", "Please select a device")
            return

        item = self.device_tree.item(selection[0])
        device_name = item['values'][0]

        # Find device by name
        device_id = None
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break

        if device_id:
            command = {
                'type': 'stop_recording',
                'timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                self.log_message(f"Stop recording command sent to {device_name}")

    def sync_selected_device(self):
        """Sync clock with selected device"""
        selection = self.device_tree.selection()
        if not selection:
            messagebox.showwarning("No Selection", "Please select a device")
            return

        item = self.device_tree.item(selection[0])
        device_name = item['values'][0]

        # Find device by name
        device_id = None
        for did, device in self.devices.items():
            if device.device_name == device_name:
                device_id = did
                break

        if device_id:
            command = {
                'type': 'sync_request',
                'pc_timestamp': time.time()
            }
            if self.send_command_to_device(device_id, command):
                self.log_message(f"Clock sync command sent to {device_name}")

    def update_gui(self):
        """Update GUI elements periodically"""
        # Update device list
        self.update_device_list()

        # Update GSR display
        if MATPLOTLIB_AVAILABLE:
            self.update_gsr_plot()
        else:
            self.update_gsr_text()

        # Update session timer
        self.update_session_timer()

        # Schedule next update
        self.root.after(1000, self.update_gui)

    def update_device_list(self):
        """Update the device list display"""
        # Clear existing items
        for item in self.device_tree.get_children():
            self.device_tree.delete(item)

        # Add current devices
        for device in self.devices.values():
            rgb_status = device.sensors['RGB']['status']
            thermal_status = device.sensors['Thermal']['status']
            gsr_status = device.sensors['GSR']['status']

            time_sync = f"{device.time_offset_ms}ms" if device.time_offset_ms != 0 else "Not synced"

            status_text = "Recording" if device.recording else device.status

            self.device_tree.insert('', 'end', values=(
                device.device_name,
                status_text,
                rgb_status,
                thermal_status,
                gsr_status,
                time_sync
            ))

    def update_gsr_plot(self):
        """Update GSR signal plot"""
        if not self.gsr_data_buffer:
            return

        try:
            # Extract time and values
            times = [item[0] for item in self.gsr_data_buffer]
            values = [item[1] for item in self.gsr_data_buffer]

            # Convert to relative time (seconds from start)
            if times:
                base_time = times[0]
                rel_times = [(t - base_time) for t in times]

                # Update plot
                self.gsr_line.set_data(rel_times, values)

                # Update axes
                if rel_times:
                    self.gsr_ax.set_xlim(max(0, rel_times[-1] - 30), rel_times[-1] + 1)

                if values:
                    min_val, max_val = min(values), max(values)
                    range_val = max_val - min_val
                    if range_val > 0:
                        self.gsr_ax.set_ylim(min_val - range_val * 0.1, max_val + range_val * 0.1)

                # Update stats
                if values:
                    latest_val = values[-1]
                    avg_val = sum(values) / len(values)
                    self.gsr_stats_label.config(text=f"GSR: Current={latest_val:.1f}, Avg={avg_val:.1f}")

                # Refresh canvas
                self.gsr_canvas.draw_idle()

        except Exception as e:
            self.log_message(f"Error updating GSR plot: {e}", "ERROR")

    def update_gsr_text(self):
        """Update GSR text display (fallback)"""
        if not self.gsr_data_buffer:
            return

        try:
            # Show recent GSR values as text
            recent_values = self.gsr_data_buffer[-10:]  # Last 10 values
            text_lines = []

            for timestamp, value in recent_values:
                time_str = datetime.fromtimestamp(timestamp).strftime("%H:%M:%S")
                text_lines.append(f"{time_str}: {value:.2f}")

            # Update text widget
            self.gsr_text.delete(1.0, tk.END)
            self.gsr_text.insert(tk.END, "\n".join(text_lines))

            # Update stats
            if self.gsr_data_buffer:
                latest_val = self.gsr_data_buffer[-1][1]
                values = [item[1] for item in self.gsr_data_buffer]
                avg_val = sum(values) / len(values)
                self.gsr_stats_label.config(text=f"GSR: Current={latest_val:.1f}, Avg={avg_val:.1f}")

        except Exception as e:
            self.log_message(f"Error updating GSR text: {e}", "ERROR")

    def update_session_timer(self):
        """Update session timer display"""
        if self.session_active and self.session_start_time:
            elapsed = time.time() - self.session_start_time
            hours = int(elapsed // 3600)
            minutes = int((elapsed % 3600) // 60)
            seconds = int(elapsed % 60)

            timer_text = f"{hours:02d}:{minutes:02d}:{seconds:02d}"
            self.session_timer_label.config(text=timer_text)
            self.session_status_label.config(text="Session Status: Recording")
        else:
            self.session_timer_label.config(text="00:00:00")
            self.session_status_label.config(text="Session Status: Idle")

    def log_message(self, message: str, level: str = "INFO"):
        """Add message to session log"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_line = f"[{timestamp}] {level}: {message}\n"

        self.log_text.insert(tk.END, log_line)
        self.log_text.see(tk.END)

        # Color coding
        if level == "ERROR":
            # Find the line we just inserted and color it red
            line_start = self.log_text.index("end-1c linestart")
            line_end = self.log_text.index("end-1c lineend")
            self.log_text.tag_add("error", line_start, line_end)
            self.log_text.tag_config("error", foreground="red")
        elif level == "WARNING":
            line_start = self.log_text.index("end-1c linestart")
            line_end = self.log_text.index("end-1c lineend")
            self.log_text.tag_add("warning", line_start, line_end)
            self.log_text.tag_config("warning", foreground="orange")

    def clear_log(self):
        """Clear the session log"""
        self.log_text.delete(1.0, tk.END)

    def save_log(self):
        """Save session log to file"""
        from tkinter import filedialog

        filename = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")]
        )

        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write(self.log_text.get(1.0, tk.END))
                self.log_message(f"Log saved to {filename}")
            except Exception as e:
                self.log_message(f"Failed to save log: {e}", "ERROR")
                messagebox.showerror("Save Error", f"Failed to save log: {e}")

    def on_closing(self):
        """Handle application closing"""
        self.server_running = False

        # Close all client connections
        for client_socket in self.client_connections.values():
            try:
                client_socket.close()
            except:
                pass

        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass

        self.root.destroy()

    def run(self):
        """Start the GUI application"""
        self.log_message("PC Session Controller started")
        self.log_message(f"Listening for device connections on port {self.server_port}")
        self.root.mainloop()


def main():
    """Main entry point"""
    try:
        app = SessionController()
        app.run()
    except KeyboardInterrupt:
        print("\nApplication interrupted by user")
    except Exception as e:
        print(f"Application error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
