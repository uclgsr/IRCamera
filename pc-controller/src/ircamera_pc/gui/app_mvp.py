#!/usr/bin/env python3
"""
IRCamera PC Controller Hub - MVP GUI Application
Integrates the session controller GUI with the MVP framework
"""

import sys
import threading
import time
from pathlib import Path

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..core.config import config
from ..core.session import SessionManager


def main():
    """Main entry point for the MVP GUI application"""
    try:
        logger.info("Starting IRCamera PC Controller Hub - MVP GUI Application")
        
        # Initialize core components
        session_manager = SessionManager()
        logger.info("Session manager initialized")
        
        # Try to import and run the GUI components
        try:
            # Import the existing PC session controller
            import importlib.util
            
            # Load the session controller from pc-controller-ui
            # Define the relative path from this file to the session controller
            RELATIVE_CONTROLLER_PATH = Path("../../../pc-controller-ui/src/pc_session_controller.py")
            controller_path = (Path(__file__).resolve().parent / RELATIVE_CONTROLLER_PATH).resolve()
            
            if controller_path.exists():
                spec = importlib.util.spec_from_file_location("pc_session_controller", controller_path)
                session_controller_module = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(session_controller_module)
                
                logger.info("Session controller GUI loaded successfully")
                
                # Run the session controller GUI
                if hasattr(session_controller_module, 'main'):
                    logger.info("Launching session controller GUI...")
                    return session_controller_module.main()
                else:
                    # Create and run SessionController directly
                    logger.info("Creating session controller instance...")
                    controller = session_controller_module.SessionController()
                    controller.start_server()
                    controller.run()
                    return 0
                    
            else:
                logger.warning(f"Session controller not found at {controller_path}")
                return run_fallback_gui(session_manager)
                
        except Exception as e:
            logger.error(f"Failed to load session controller GUI: {e}")
            return run_fallback_gui(session_manager)
            
    except Exception as e:
        logger.error(f"MVP GUI application failed: {e}")
        return 1


def run_fallback_gui(session_manager):
    """Run a simple fallback GUI when the main GUI is not available"""
    try:
        import tkinter as tk
        from tkinter import ttk, messagebox
        
        logger.info("Running fallback GUI interface")
        
        class FallbackGUI:
            def __init__(self):
                self.root = tk.Tk()
                self.root.title("IRCamera PC Controller Hub - MVP")
                self.root.geometry("600x400")
                
                self.session_manager = session_manager
                self.setup_ui()
                
            def setup_ui(self):
                # Main frame
                main_frame = ttk.Frame(self.root, padding="10")
                main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
                
                # Title
                title_label = ttk.Label(main_frame, text="IRCamera PC Controller Hub - MVP", 
                                      font=('Arial', 16, 'bold'))
                title_label.grid(row=0, column=0, columnspan=2, pady=(0, 20))
                
                # Status
                ttk.Label(main_frame, text="Status:").grid(row=1, column=0, sticky=tk.W, pady=5)
                self.status_label = ttk.Label(main_frame, text="Ready", foreground="green")
                self.status_label.grid(row=1, column=1, sticky=tk.W, pady=5)
                
                # Session controls
                session_frame = ttk.LabelFrame(main_frame, text="Session Management", padding="10")
                session_frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=10)
                
                ttk.Button(session_frame, text="Create Session", 
                          command=self.create_session).grid(row=0, column=0, padx=5)
                ttk.Button(session_frame, text="Start Recording", 
                          command=self.start_recording).grid(row=0, column=1, padx=5)
                ttk.Button(session_frame, text="Stop Recording", 
                          command=self.stop_recording).grid(row=0, column=2, padx=5)
                
                # Device discovery
                device_frame = ttk.LabelFrame(main_frame, text="Device Management", padding="10")
                device_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=10)
                
                ttk.Button(device_frame, text="Discover Devices", 
                          command=self.discover_devices).grid(row=0, column=0, padx=5)
                ttk.Button(device_frame, text="Refresh Status", 
                          command=self.refresh_status).grid(row=0, column=1, padx=5)
                
                # Info display
                info_frame = ttk.LabelFrame(main_frame, text="System Information", padding="10")
                info_frame.grid(row=4, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=10)
                
                self.info_text = tk.Text(info_frame, height=10, width=70)
                scrollbar = ttk.Scrollbar(info_frame, orient="vertical", command=self.info_text.yview)
                self.info_text.configure(yscrollcommand=scrollbar.set)
                
                self.info_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
                scrollbar.grid(row=0, column=1, sticky=(tk.N, tk.S))
                
                # Configure grid weights
                main_frame.columnconfigure(1, weight=1)
                main_frame.rowconfigure(4, weight=1)
                info_frame.columnconfigure(0, weight=1)
                info_frame.rowconfigure(0, weight=1)
                
                self.root.columnconfigure(0, weight=1)
                self.root.rowconfigure(0, weight=1)
                
                # Initialize display
                self.update_info("IRCamera PC Controller Hub initialized")
                self.update_info(f"Configuration loaded: {config.get('version', 'MVP-1.0.0')}")
                self.update_info(f"Server port: {config.get('network.server_port', 8080)}")
                
            def create_session(self):
                try:
                    session = self.session_manager.create_session("MVP GUI Session")
                    self.update_info(f"Session created: {session.name} [{session.session_id}]")
                    self.status_label.config(text="Session Created", foreground="blue")
                except Exception as e:
                    self.update_info(f"Failed to create session: {e}")
                    messagebox.showerror("Error", f"Failed to create session: {e}")
                    
            def start_recording(self):
                try:
                    if self.session_manager.get_current_session():
                        self.session_manager.start_session()
                        self.update_info("Recording started")
                        self.status_label.config(text="Recording", foreground="red")
                    else:
                        messagebox.showwarning("Warning", "Please create a session first")
                except Exception as e:
                    self.update_info(f"Failed to start recording: {e}")
                    messagebox.showerror("Error", f"Failed to start recording: {e}")
                    
            def stop_recording(self):
                try:
                    session = self.session_manager.end_session()
                    if session:
                        self.update_info(f"Recording stopped. Session completed: {session.name}")
                        self.status_label.config(text="Ready", foreground="green")
                    else:
                        messagebox.showwarning("Warning", "No active session to stop")
                except Exception as e:
                    self.update_info(f"Failed to stop recording: {e}")
                    messagebox.showerror("Error", f"Failed to stop recording: {e}")
                    
            def discover_devices(self):
                self.update_info("Discovering devices via mDNS...")
                self.status_label.config(text="Discovering", foreground="orange")
                
                # Simulate device discovery
                threading.Thread(target=self._discover_devices_thread, daemon=True).start()
                
            def _discover_devices_thread(self):
                try:
                    time.sleep(2)  # Simulate discovery time
                    devices = [
                        "Android-GSR-001 (192.168.1.100) - RGB, GSR",
                        "Android-Thermal-002 (192.168.1.101) - Thermal, GSR", 
                        "Shimmer-GSR-003 (192.168.1.102) - GSR"
                    ]
                    
                    self.root.after(0, lambda: self.update_info(f"Found {len(devices)} devices:"))
                    for device in devices:
                        self.root.after(0, lambda d=device: self.update_info(f"  • {d}"))
                    
                    self.root.after(0, lambda: self.status_label.config(text="Ready", foreground="green"))
                    
                except Exception as e:
                    self.root.after(0, lambda: self.update_info(f"Device discovery failed: {e}"))
                    
            def refresh_status(self):
                self.update_info("System status refreshed")
                current_session = self.session_manager.get_current_session()
                if current_session:
                    self.update_info(f"Active session: {current_session.name} ({current_session.state})")
                else:
                    self.update_info("No active session")
                    
            def update_info(self, message):
                timestamp = time.strftime("%H:%M:%S")
                self.info_text.insert(tk.END, f"[{timestamp}] {message}\n")
                self.info_text.see(tk.END)
                
            def run(self):
                try:
                    self.root.mainloop()
                    return 0
                except KeyboardInterrupt:
                    logger.info("GUI application interrupted by user")
                    return 0
                except Exception as e:
                    logger.error(f"GUI application error: {e}")
                    return 1
        
        gui = FallbackGUI()
        return gui.run()
        
    except ImportError:
        logger.error("GUI frameworks not available - trying CLI interface")
        return run_cli_interface(session_manager)
    except Exception as e:
        logger.error(f"Fallback GUI failed: {e}")
        return run_cli_interface(session_manager)


def run_cli_interface(session_manager):
    """Run CLI interface when GUI is not available"""
    try:
        from .cli_mvp import MVPCLI
        logger.info("Starting CLI interface")
        cli = MVPCLI()
        return cli.run()
    except Exception as e:
        logger.error(f"CLI interface failed: {e}")
        return run_headless_mode(session_manager)


def run_headless_mode(session_manager):
    """Run in headless mode when no GUI is available"""
    logger.info("Running in headless mode")
    
    try:
        # Create a demonstration session
        session = session_manager.create_session("Headless MVP Session")
        logger.info(f"Created session: {session.name} [{session.session_id}]")
        
        # Start the session
        session_manager.start_session()
        logger.info("Session started")
        
        # Simulate some activity
        logger.info("Simulating device discovery and data collection...")
        time.sleep(3)
        
        # Add some sample events
        session_manager.add_sync_event("device_discovered", {"device": "Android-GSR-001", "ip": "192.168.1.100"})
        session_manager.add_sync_event("recording_started", {"modalities": ["rgb", "gsr"]})
        
        logger.info("MVP demonstration complete")
        
        # End the session
        final_session = session_manager.end_session()
        if final_session:
            logger.info(f"Session completed: {final_session.name}")
            logger.info(f"Duration: {final_session.duration_seconds} seconds")
            
        return 0
        
    except Exception as e:
        logger.error(f"Headless mode failed: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())