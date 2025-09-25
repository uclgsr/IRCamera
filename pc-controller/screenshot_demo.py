#!/usr/bin/env python3
"""
Screenshot demo for the Enhanced GUI Controller

This script creates a minimal GUI window to demonstrate the interface
"""

import tkinter as tk
from tkinter import ttk
import threading
import time

def create_demo_gui():
    """Create a demo GUI window"""
    root = tk.Tk()
    root.title("IRCamera Enhanced PC Controller - Demo")
    root.geometry("1200x800")
    
    # Main container
    main_frame = ttk.Frame(root)
    main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
    
    # Control panel
    control_frame = ttk.LabelFrame(main_frame, text="Session Control", padding=10)
    control_frame.pack(fill=tk.X)
    
    # Session info
    info_frame = ttk.Frame(control_frame)
    info_frame.pack(fill=tk.X, pady=(0, 10))
    
    ttk.Label(info_frame, text="Current Session:").pack(side=tk.LEFT)
    session_label = ttk.Label(info_frame, text="demo_session_001", foreground="green")
    session_label.pack(side=tk.LEFT, padx=(10, 0))
    
    # Control buttons
    button_frame = ttk.Frame(control_frame)
    button_frame.pack(fill=tk.X)
    
    ttk.Button(button_frame, text="Start Recording").pack(side=tk.LEFT, padx=(0, 10))
    ttk.Button(button_frame, text="Stop Recording").pack(side=tk.LEFT, padx=(0, 10))
    ttk.Button(button_frame, text="Refresh Devices").pack(side=tk.LEFT, padx=(0, 10))
    ttk.Button(button_frame, text="Export Data").pack(side=tk.LEFT, padx=(0, 10))
    
    # Server status
    server_frame = ttk.Frame(control_frame)
    server_frame.pack(side=tk.RIGHT)
    
    ttk.Label(server_frame, text="Server:").pack(side=tk.LEFT)
    server_status = ttk.Label(server_frame, text="Running", foreground="green")
    server_status.pack(side=tk.LEFT, padx=(5, 0))
    
    # Content area
    content_frame = ttk.Frame(main_frame)
    content_frame.pack(fill=tk.BOTH, expand=True, pady=(10, 0))
    
    # Left panel - Device management
    left_panel = ttk.Frame(content_frame)
    left_panel.pack(side=tk.LEFT, fill=tk.BOTH, expand=False, padx=(0, 10))
    
    # Device list
    device_frame = ttk.LabelFrame(left_panel, text="Connected Devices", padding=10)
    device_frame.pack(fill=tk.BOTH, expand=True)
    
    device_tree = ttk.Treeview(device_frame, columns=('Name', 'Status', 'Recording'), show='tree headings', height=6)
    device_tree.heading('#0', text='Device ID')
    device_tree.heading('Name', text='Name')
    device_tree.heading('Status', text='Status')
    device_tree.heading('Recording', text='Recording')
    
    # Add demo devices
    device_tree.insert('', 'end', text='android_001', values=('Galaxy S23', 'Connected', 'Recording'))
    device_tree.insert('', 'end', text='android_002', values=('Pixel 7', 'Connected', 'Idle'))
    
    device_tree.pack(fill=tk.BOTH, expand=True)
    
    # Sensor status
    sensor_frame = ttk.LabelFrame(left_panel, text="Sensor Status", padding=10)
    sensor_frame.pack(fill=tk.BOTH, expand=True, pady=(10, 0))
    
    sensor_tree = ttk.Treeview(sensor_frame, columns=('Status', 'Message'), show='tree headings', height=8)
    sensor_tree.heading('#0', text='Device / Sensor')
    sensor_tree.heading('Status', text='Status')
    sensor_tree.heading('Message', text='Message')
    
    # Add demo sensor data
    device1 = sensor_tree.insert('', 'end', text='Galaxy S23')
    sensor_tree.insert(device1, 'end', text='  GSR', values=('Connected', 'Shimmer3 ready'))
    sensor_tree.insert(device1, 'end', text='  RGB', values=('Connected', 'Camera ready'))
    sensor_tree.insert(device1, 'end', text='  Thermal', values=('Connected', 'FLIR ready'))
    
    device2 = sensor_tree.insert('', 'end', text='Pixel 7')
    sensor_tree.insert(device2, 'end', text='  GSR', values=('Connected', 'Shimmer3 ready'))
    sensor_tree.insert(device2, 'end', text='  RGB', values=('Connected', 'Camera ready'))
    sensor_tree.insert(device2, 'end', text='  Thermal', values=('Disconnected', 'Not available'))
    
    sensor_tree.item(device1, open=True)
    sensor_tree.item(device2, open=True)
    sensor_tree.pack(fill=tk.BOTH, expand=True)
    
    # Right panel - Visualization
    right_panel = ttk.Frame(content_frame)
    right_panel.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)
    
    # Notebook for tabs
    notebook = ttk.Notebook(right_panel)
    notebook.pack(fill=tk.BOTH, expand=True)
    
    # Real-time plot tab
    plot_frame = ttk.Frame(notebook)
    notebook.add(plot_frame, text="Real-time GSR")
    
    plot_label_frame = ttk.LabelFrame(plot_frame, text="Real-time GSR Data", padding=10)
    plot_label_frame.pack(fill=tk.BOTH, expand=True)
    
    # Simulate plot area
    plot_canvas = tk.Canvas(plot_label_frame, bg='white', height=300)
    plot_canvas.pack(fill=tk.BOTH, expand=True)
    
    # Draw a simple mock plot
    plot_canvas.create_text(400, 150, text="📊 Real-time GSR Plot\n(Matplotlib Integration)", font=('Arial', 16), fill='blue')
    plot_canvas.create_line(50, 200, 750, 100, fill='red', width=2)
    plot_canvas.create_text(400, 50, text="GSR (µS)", font=('Arial', 12))
    plot_canvas.create_text(50, 280, text="Time (s)", font=('Arial', 12))
    
    # Session log tab
    log_frame = ttk.Frame(notebook)
    notebook.add(log_frame, text="Session Log")
    
    log_label_frame = ttk.LabelFrame(log_frame, text="Session Log", padding=10)
    log_label_frame.pack(fill=tk.BOTH, expand=True)
    
    log_text = tk.Text(log_label_frame)
    log_text.pack(fill=tk.BOTH, expand=True)
    
    # Add demo log entries
    log_entries = [
        "[15:30:00] Server started on port 8080",
        "[15:30:15] Device connected: Galaxy S23 (android_001)",
        "[15:30:16] Device connected: Pixel 7 (android_002)",
        "[15:30:30] Recording session started: demo_session_001",
        "[15:30:31] GSR data from Galaxy S23: 12.5 µS",
        "[15:30:32] GSR data from Pixel 7: 14.2 µS",
        "[15:30:33] Frame from Galaxy S23: RGB",
        "[15:30:34] GSR data from Galaxy S23: 13.1 µS",
        "[15:30:35] GSR data from Pixel 7: 14.8 µS"
    ]
    
    for entry in log_entries:
        log_text.insert(tk.END, entry + "\n")
    
    # Data export tab
    export_frame = ttk.Frame(notebook)
    notebook.add(export_frame, text="Data Export")
    
    export_label_frame = ttk.LabelFrame(export_frame, text="Data Export Options", padding=10)
    export_label_frame.pack(fill=tk.BOTH, expand=True)
    
    ttk.Label(export_label_frame, text="Export Format:").pack(anchor=tk.W)
    format_frame = ttk.Frame(export_label_frame)
    format_frame.pack(anchor=tk.W, pady=(5, 10))
    
    ttk.Radiobutton(format_frame, text="CSV", value="csv").pack(side=tk.LEFT)
    ttk.Radiobutton(format_frame, text="JSON", value="json").pack(side=tk.LEFT, padx=(10, 0))
    
    ttk.Button(export_label_frame, text="Export Session Data").pack(pady=10)
    
    # Status bar
    status_frame = ttk.Frame(main_frame)
    status_frame.pack(fill=tk.X, pady=(10, 0))
    
    status_text = ttk.Label(status_frame, text="Ready - 2 devices connected", relief=tk.SUNKEN, anchor=tk.W)
    status_text.pack(side=tk.LEFT, fill=tk.X, expand=True)
    
    connection_count = ttk.Label(status_frame, text="Devices: 2", relief=tk.SUNKEN)
    connection_count.pack(side=tk.RIGHT, padx=(10, 0))
    
    print("Demo GUI created - close window to exit")
    
    return root

def main():
    """Main function"""
    print("🎯 Creating Enhanced PC Controller GUI Demo...")
    
    root = create_demo_gui()
    
    # Start GUI
    try:
        root.mainloop()
    except KeyboardInterrupt:
        print("Demo interrupted")

if __name__ == "__main__":
    main()