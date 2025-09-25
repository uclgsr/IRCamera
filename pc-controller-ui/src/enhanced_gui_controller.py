#!/usr/bin/env python3
"""
Enhanced GUI PC Controller with Real-time Visualization

This implementation provides:
1. Professional GUI interface using tkinter
2. Real-time GSR plotting with matplotlib
3. Device management panel
4. Session control interface
5. Live telemetry display
6. Data export capabilities
7. Error handling and status monitoring
"""

import json
import socket
import sys
import threading
import time
import tkinter as tk
from datetime import datetime
from pathlib import Path
from tkinter import ttk, scrolledtext, messagebox, filedialog
from typing import Dict, List, Optional, Tuple
import queue

# Import matplotlib for real-time plotting
try:
    import matplotlib.pyplot as plt
    from matplotlib.backends.backend_tkagg import FigureCanvasTkinter, NavigationToolbar2Tk
    from matplotlib.figure import Figure
    import numpy as np
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

# Import the enhanced controller
sys.path.append(str(Path(__file__).parent.parent.parent / "pc-controller"))
from enhanced_pc_controller import EnhancedPCController, DeviceInfo, SessionInfo


class DeviceStatusWidget:
    """Widget for displaying device status"""
    
    def __init__(self, parent):
        self.frame = ttk.LabelFrame(parent, text="Connected Devices", padding=10)
        self.tree = ttk.Treeview(self.frame, columns=('Name', 'Type', 'Status', 'Recording', 'Data Count'), show='tree headings', height=6)
        
        # Configure columns
        self.tree.heading('#0', text='Device ID')
        self.tree.heading('Name', text='Name')
        self.tree.heading('Type', text='Type')
        self.tree.heading('Status', text='Status')
        self.tree.heading('Recording', text='Recording')
        self.tree.heading('Data Count', text='Data Packets')
        
        self.tree.column('#0', width=120)
        self.tree.column('Name', width=150)
        self.tree.column('Type', width=80)
        self.tree.column('Status', width=100)
        self.tree.column('Recording', width=80)
        self.tree.column('Data Count', width=100)
        
        # Scrollbar
        scrollbar = ttk.Scrollbar(self.frame, orient=tk.VERTICAL, command=self.tree.yview)
        self.tree.configure(yscrollcommand=scrollbar.set)
        
        # Pack widgets
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
    
    def update_devices(self, device_status: Dict[str, Dict]):
        """Update the device list"""
        # Clear existing items
        for item in self.tree.get_children():
            self.tree.delete(item)
        
        # Add current devices
        for device_id, status in device_status.items():
            self.tree.insert('', 'end', iid=device_id, text=device_id,
                           values=(
                               status.get('device_name', 'Unknown'),
                               status.get('device_type', 'Unknown'),
                               status.get('status', 'Unknown'),
                               'Recording' if status.get('recording', False) else 'Idle',
                               status.get('data_packets_received', 0)
                           ))


class SensorStatusWidget:
    """Widget for displaying sensor status details"""
    
    def __init__(self, parent):
        self.frame = ttk.LabelFrame(parent, text="Sensor Status", padding=10)
        
        # Create treeview for sensor details
        self.tree = ttk.Treeview(self.frame, columns=('Status', 'Message'), show='tree headings', height=8)
        
        self.tree.heading('#0', text='Device / Sensor')
        self.tree.heading('Status', text='Status')
        self.tree.heading('Message', text='Message')
        
        self.tree.column('#0', width=200)
        self.tree.column('Status', width=100)
        self.tree.column('Message', width=200)
        
        # Scrollbar
        scrollbar = ttk.Scrollbar(self.frame, orient=tk.VERTICAL, command=self.tree.yview)
        self.tree.configure(yscrollcommand=scrollbar.set)
        
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
    
    def update_sensors(self, device_status: Dict[str, Dict]):
        """Update sensor status display"""
        # Clear existing items
        for item in self.tree.get_children():
            self.tree.delete(item)
        
        # Add devices and their sensors
        for device_id, status in device_status.items():
            device_name = status.get('device_name', device_id)
            device_item = self.tree.insert('', 'end', text=device_name, values=('', ''))
            
            sensors = status.get('sensors', {})
            for sensor_name, sensor_info in sensors.items():
                self.tree.insert(device_item, 'end', text=f"  {sensor_name}",
                               values=(
                                   sensor_info.get('status', 'Unknown'),
                                   sensor_info.get('message', '')
                               ))
            
            # Expand device nodes
            self.tree.item(device_item, open=True)


class RealTimePlotWidget:
    """Widget for real-time GSR plotting"""
    
    def __init__(self, parent):
        self.frame = ttk.LabelFrame(parent, text="Real-time GSR Data", padding=10)
        
        if HAS_MATPLOTLIB:
            # Create matplotlib figure
            self.fig = Figure(figsize=(10, 4), dpi=100)
            self.ax = self.fig.add_subplot(111)
            self.ax.set_title('GSR Signal (µS)')
            self.ax.set_xlabel('Time (s)')
            self.ax.set_ylabel('GSR (µS)')
            self.ax.grid(True)
            
            # Create canvas
            self.canvas = FigureCanvasTkinter(self.fig, master=self.frame)
            self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
            
            # Navigation toolbar
            toolbar = NavigationToolbar2Tk(self.canvas, self.frame)
            toolbar.update()
            
            # Initialize plot line
            self.line, = self.ax.plot([], [], 'b-', linewidth=2)
            
            # Data storage
            self.timestamps = []
            self.gsr_values = []
            self.max_points = 300  # Show last 30 seconds at 10Hz
        else:
            # Fallback text display
            self.text_area = scrolledtext.ScrolledText(self.frame, height=10, width=60)
            self.text_area.pack(fill=tk.BOTH, expand=True)
            self.text_area.insert(tk.END, "Matplotlib not available - GSR data will be shown as text\n")
    
    def update_data(self, timestamp: float, gsr_value: float):
        """Update the plot with new GSR data"""
        if HAS_MATPLOTLIB:
            # Add new data point
            self.timestamps.append(timestamp)
            self.gsr_values.append(gsr_value)
            
            # Keep only recent data
            if len(self.timestamps) > self.max_points:
                self.timestamps = self.timestamps[-self.max_points:]
                self.gsr_values = self.gsr_values[-self.max_points:]
            
            # Update plot if we have data
            if self.timestamps and self.gsr_values:
                # Convert to relative timestamps
                start_time = self.timestamps[0]
                rel_times = [(t - start_time) for t in self.timestamps]
                
                self.line.set_data(rel_times, self.gsr_values)
                
                # Update axes limits
                if rel_times:
                    self.ax.set_xlim(0, max(30, max(rel_times)))
                if self.gsr_values:
                    y_min, y_max = min(self.gsr_values), max(self.gsr_values)
                    y_range = max(0.1, y_max - y_min)  # Prevent division by zero
                    self.ax.set_ylim(y_min - 0.1 * y_range, y_max + 0.1 * y_range)
                
                self.canvas.draw()
        else:
            # Fallback text display
            time_str = datetime.fromtimestamp(timestamp).strftime('%H:%M:%S.%f')[:-3]
            self.text_area.insert(tk.END, f"{time_str}: {gsr_value:.2f} µS\n")
            self.text_area.see(tk.END)
            
            # Keep text area from growing too large
            lines = self.text_area.get('1.0', tk.END).split('\n')
            if len(lines) > 100:
                self.text_area.delete('1.0', f'{len(lines) - 100}.0')


class EnhancedGUIController:
    """Enhanced GUI PC Controller Application"""
    
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("IRCamera Enhanced PC Controller")
        self.root.geometry("1400x900")
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        
        # PC Controller instance
        self.pc_controller = EnhancedPCController(port=8080)
        
        # GUI update queue for thread-safe updates
        self.gui_queue = queue.Queue()
        
        # Current session info
        self.current_session_id = None
        self.session_start_time = None
        
        # Setup GUI
        self.setup_ui()
        self.setup_controller_callbacks()
        
        # Start background threads
        self.start_background_threads()
        
        # Start GUI update timer
        self.update_gui_timer()
        
        print("Enhanced GUI Controller initialized")
    
    def setup_ui(self):
        """Setup the GUI interface"""
        # Main container
        main_frame = ttk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Top control panel
        self.setup_control_panel(main_frame)
        
        # Middle content area
        content_frame = ttk.Frame(main_frame)
        content_frame.pack(fill=tk.BOTH, expand=True, pady=(10, 0))
        
        # Left panel - Device management
        left_panel = ttk.Frame(content_frame)
        left_panel.pack(side=tk.LEFT, fill=tk.BOTH, expand=False, padx=(0, 10))
        
        self.device_widget = DeviceStatusWidget(left_panel)
        self.device_widget.frame.pack(fill=tk.BOTH, expand=True)
        
        self.sensor_widget = SensorStatusWidget(left_panel)
        self.sensor_widget.frame.pack(fill=tk.BOTH, expand=True, pady=(10, 0))
        
        # Right panel - Visualization and logs
        right_panel = ttk.Frame(content_frame)
        right_panel.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)
        
        # Notebook for tabs
        self.notebook = ttk.Notebook(right_panel)
        self.notebook.pack(fill=tk.BOTH, expand=True)
        
        # Real-time plot tab
        plot_frame = ttk.Frame(self.notebook)
        self.notebook.add(plot_frame, text="Real-time GSR")
        self.plot_widget = RealTimePlotWidget(plot_frame)
        self.plot_widget.frame.pack(fill=tk.BOTH, expand=True)
        
        # Session log tab
        log_frame = ttk.Frame(self.notebook)
        self.notebook.add(log_frame, text="Session Log")
        self.setup_log_panel(log_frame)
        
        # Data export tab
        export_frame = ttk.Frame(self.notebook)
        self.notebook.add(export_frame, text="Data Export")
        self.setup_export_panel(export_frame)
        
        # Status bar
        self.setup_status_bar(main_frame)
    
    def setup_control_panel(self, parent):
        """Setup the control panel"""
        control_frame = ttk.LabelFrame(parent, text="Session Control", padding=10)
        control_frame.pack(fill=tk.X)
        
        # Session info
        info_frame = ttk.Frame(control_frame)
        info_frame.pack(fill=tk.X, pady=(0, 10))
        
        ttk.Label(info_frame, text="Current Session:").pack(side=tk.LEFT)
        self.session_label = ttk.Label(info_frame, text="No active session", foreground="gray")
        self.session_label.pack(side=tk.LEFT, padx=(10, 0))
        
        # Control buttons
        button_frame = ttk.Frame(control_frame)
        button_frame.pack(fill=tk.X)
        
        self.start_btn = ttk.Button(button_frame, text="Start Recording", command=self.start_recording)
        self.start_btn.pack(side=tk.LEFT, padx=(0, 10))
        
        self.stop_btn = ttk.Button(button_frame, text="Stop Recording", command=self.stop_recording, state=tk.DISABLED)
        self.stop_btn.pack(side=tk.LEFT, padx=(0, 10))
        
        ttk.Button(button_frame, text="Refresh Devices", command=self.refresh_devices).pack(side=tk.LEFT, padx=(0, 10))
        
        ttk.Button(button_frame, text="Export Data", command=self.export_data).pack(side=tk.LEFT, padx=(0, 10))
        
        # Server status
        server_frame = ttk.Frame(control_frame)
        server_frame.pack(side=tk.RIGHT)
        
        ttk.Label(server_frame, text="Server:").pack(side=tk.LEFT)
        self.server_status = ttk.Label(server_frame, text="Starting...", foreground="orange")
        self.server_status.pack(side=tk.LEFT, padx=(5, 0))
    
    def setup_log_panel(self, parent):
        """Setup the session log panel"""
        log_frame = ttk.LabelFrame(parent, text="Session Log", padding=10)
        log_frame.pack(fill=tk.BOTH, expand=True)
        
        self.log_text = scrolledtext.ScrolledText(log_frame, height=15, width=60)
        self.log_text.pack(fill=tk.BOTH, expand=True)
        
        # Log control buttons
        log_control_frame = ttk.Frame(log_frame)
        log_control_frame.pack(fill=tk.X, pady=(10, 0))
        
        ttk.Button(log_control_frame, text="Clear Log", command=self.clear_log).pack(side=tk.LEFT)
        ttk.Button(log_control_frame, text="Save Log", command=self.save_log).pack(side=tk.LEFT, padx=(10, 0))
    
    def setup_export_panel(self, parent):
        """Setup the data export panel"""
        export_frame = ttk.LabelFrame(parent, text="Data Export Options", padding=10)
        export_frame.pack(fill=tk.BOTH, expand=True)
        
        # Export format selection
        format_frame = ttk.Frame(export_frame)
        format_frame.pack(fill=tk.X, pady=(0, 10))
        
        ttk.Label(format_frame, text="Export Format:").pack(side=tk.LEFT)
        self.export_format = tk.StringVar(value="csv")
        ttk.Radiobutton(format_frame, text="CSV", variable=self.export_format, value="csv").pack(side=tk.LEFT, padx=(10, 0))
        ttk.Radiobutton(format_frame, text="JSON", variable=self.export_format, value="json").pack(side=tk.LEFT, padx=(10, 0))
        
        # Export options
        options_frame = ttk.Frame(export_frame)
        options_frame.pack(fill=tk.X, pady=(0, 10))
        
        self.include_metadata = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="Include metadata", variable=self.include_metadata).pack(anchor=tk.W)
        
        self.include_device_info = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="Include device information", variable=self.include_device_info).pack(anchor=tk.W)
        
        # Export buttons
        export_btn_frame = ttk.Frame(export_frame)
        export_btn_frame.pack(fill=tk.X)
        
        ttk.Button(export_btn_frame, text="Export Session Data", command=self.export_session_data).pack(side=tk.LEFT)
        ttk.Button(export_btn_frame, text="Export All Data", command=self.export_all_data).pack(side=tk.LEFT, padx=(10, 0))
        
        # Export status
        self.export_status = ttk.Label(export_frame, text="", foreground="green")
        self.export_status.pack(pady=(10, 0))
    
    def setup_status_bar(self, parent):
        """Setup the status bar"""
        status_frame = ttk.Frame(parent)
        status_frame.pack(fill=tk.X, pady=(10, 0))
        
        self.status_text = ttk.Label(status_frame, text="Ready", relief=tk.SUNKEN, anchor=tk.W)
        self.status_text.pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        # Connection count
        self.connection_count = ttk.Label(status_frame, text="Devices: 0", relief=tk.SUNKEN)
        self.connection_count.pack(side=tk.RIGHT, padx=(10, 0))
    
    def setup_controller_callbacks(self):
        """Setup callbacks for PC controller events"""
        def on_device_connected(device_info):
            self.gui_queue.put(('device_connected', device_info))
        
        def on_device_disconnected(device_info):
            self.gui_queue.put(('device_disconnected', device_info))
        
        def on_data_received(device_info, message):
            self.gui_queue.put(('data_received', device_info, message))
        
        self.pc_controller.on_device_connected = on_device_connected
        self.pc_controller.on_device_disconnected = on_device_disconnected
        self.pc_controller.on_data_received = on_data_received
    
    def start_background_threads(self):
        """Start background threads"""
        # PC Controller server thread
        self.server_thread = threading.Thread(target=self.run_server, daemon=True)
        self.server_thread.start()
    
    def run_server(self):
        """Run the PC controller server"""
        try:
            self.gui_queue.put(('server_status', 'Running'))
            self.pc_controller.start()
        except Exception as e:
            self.gui_queue.put(('server_error', str(e)))
    
    def update_gui_timer(self):
        """Process GUI updates from background threads"""
        try:
            while True:
                event = self.gui_queue.get_nowait()
                self.process_gui_event(event)
        except queue.Empty:
            pass
        
        # Update device status
        self.update_device_displays()
        
        # Schedule next update
        self.root.after(100, self.update_gui_timer)  # Update every 100ms
    
    def process_gui_event(self, event):
        """Process a GUI event from the queue"""
        event_type = event[0]
        
        if event_type == 'device_connected':
            device_info = event[1]
            self.log_message(f"Device connected: {device_info.device_name} ({device_info.device_id})")
            self.status_text.config(text=f"Device connected: {device_info.device_name}")
        
        elif event_type == 'device_disconnected':
            device_info = event[1]
            self.log_message(f"Device disconnected: {device_info.device_name} ({device_info.device_id})")
            self.status_text.config(text=f"Device disconnected: {device_info.device_name}")
        
        elif event_type == 'data_received':
            device_info, message = event[1], event[2]
            self.handle_data_message(device_info, message)
        
        elif event_type == 'server_status':
            status = event[1]
            self.server_status.config(text=status, foreground="green")
        
        elif event_type == 'server_error':
            error = event[1]
            self.server_status.config(text=f"Error: {error}", foreground="red")
            self.log_message(f"Server error: {error}")
    
    def handle_data_message(self, device_info, message):
        """Handle data messages from devices"""
        msg_type = message.get('type')
        
        if msg_type == 'telemetry_gsr':
            # Update real-time plot
            timestamp = message.get('timestamp', time.time())
            value = message.get('value', 0.0)
            self.plot_widget.update_data(timestamp, value)
        
        elif msg_type == 'session_started':
            session_id = message.get('session_id')
            self.current_session_id = session_id
            self.session_start_time = time.time()
            self.session_label.config(text=session_id, foreground="green")
            self.start_btn.config(state=tk.DISABLED)
            self.stop_btn.config(state=tk.NORMAL)
            self.log_message(f"Session started: {session_id}")
        
        elif msg_type == 'session_stopped':
            if self.current_session_id:
                self.log_message(f"Session stopped: {self.current_session_id}")
                self.current_session_id = None
                self.session_start_time = None
                self.session_label.config(text="No active session", foreground="gray")
                self.start_btn.config(state=tk.NORMAL)
                self.stop_btn.config(state=tk.DISABLED)
    
    def update_device_displays(self):
        """Update device status displays"""
        device_status = self.pc_controller.get_device_status()
        
        # Update device list
        self.device_widget.update_devices(device_status)
        
        # Update sensor status
        self.sensor_widget.update_sensors(device_status)
        
        # Update connection count
        self.connection_count.config(text=f"Devices: {len(device_status)}")
    
    def start_recording(self):
        """Start a recording session"""
        if self.pc_controller.start_recording_session():
            self.log_message("Recording session start command sent to devices")
            self.status_text.config(text="Starting recording session...")
        else:
            messagebox.showwarning("No Devices", "No devices connected to start recording")
    
    def stop_recording(self):
        """Stop the current recording session"""
        if self.pc_controller.stop_recording_session():
            self.log_message("Recording session stop command sent to devices")
            self.status_text.config(text="Stopping recording session...")
        else:
            messagebox.showwarning("No Session", "No active recording session to stop")
    
    def refresh_devices(self):
        """Refresh device list"""
        self.update_device_displays()
        self.status_text.config(text="Device list refreshed")
    
    def export_data(self):
        """Export current session data"""
        try:
            format_type = self.export_format.get()
            output_file = self.pc_controller.export_session_data(format_type=format_type)
            
            if output_file:
                self.log_message(f"Data exported to: {output_file}")
                self.export_status.config(text=f"Exported to: {output_file.name}")
                messagebox.showinfo("Export Complete", f"Data exported to:\n{output_file}")
            else:
                messagebox.showwarning("Export Failed", "No data available to export")
        
        except Exception as e:
            error_msg = f"Export error: {e}"
            self.log_message(error_msg)
            messagebox.showerror("Export Error", error_msg)
    
    def export_session_data(self):
        """Export current session data with file dialog"""
        if not self.current_session_id:
            messagebox.showwarning("No Session", "No active session to export")
            return
        
        # File dialog for save location
        format_type = self.export_format.get()
        filename = filedialog.asksaveasfilename(
            defaultextension=f".{format_type}",
            filetypes=[(f"{format_type.upper()} files", f"*.{format_type}"), ("All files", "*.*")],
            initialname=f"{self.current_session_id}_export.{format_type}"
        )
        
        if filename:
            try:
                output_file = Path(filename)
                self.pc_controller.export_session_data(self.current_session_id, format_type)
                
                self.log_message(f"Session data exported to: {output_file}")
                self.export_status.config(text=f"Exported: {output_file.name}")
                messagebox.showinfo("Export Complete", f"Session data exported to:\n{output_file}")
            
            except Exception as e:
                error_msg = f"Export error: {e}"
                self.log_message(error_msg)
                messagebox.showerror("Export Error", error_msg)
    
    def export_all_data(self):
        """Export all collected data"""
        # This would export all sessions and data
        messagebox.showinfo("Export All", "Export all functionality would be implemented here")
    
    def log_message(self, message: str):
        """Add a message to the session log"""
        timestamp = datetime.now().strftime('%H:%M:%S')
        log_entry = f"[{timestamp}] {message}\n"
        
        self.log_text.insert(tk.END, log_entry)
        self.log_text.see(tk.END)
    
    def clear_log(self):
        """Clear the session log"""
        self.log_text.delete(1.0, tk.END)
    
    def save_log(self):
        """Save the session log to file"""
        filename = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")],
            initialname=f"session_log_{int(time.time())}.txt"
        )
        
        if filename:
            try:
                with open(filename, 'w') as f:
                    f.write(self.log_text.get(1.0, tk.END))
                
                messagebox.showinfo("Log Saved", f"Session log saved to:\n{filename}")
            
            except Exception as e:
                messagebox.showerror("Save Error", f"Error saving log:\n{e}")
    
    def on_closing(self):
        """Handle application closing"""
        if messagebox.askokcancel("Quit", "Do you want to quit the PC Controller?"):
            self.pc_controller.stop()
            self.root.destroy()
    
    def run(self):
        """Run the GUI application"""
        self.root.mainloop()


def main():
    """Main function"""
    print("🚀 Starting Enhanced GUI PC Controller...")
    
    app = EnhancedGUIController()
    
    try:
        app.run()
    except KeyboardInterrupt:
        print("\n👋 Application interrupted")
    except Exception as e:
        print(f"❌ Application error: {e}")
    finally:
        print("🔧 Cleanup complete")


if __name__ == "__main__":
    main()