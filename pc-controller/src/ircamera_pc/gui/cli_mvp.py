#!/usr/bin/env python3
"""
IRCamera PC Controller Hub - MVP CLI Interface
Command-line interface for headless environments
"""

import sys
import time
from datetime import datetime

try:
    from loguru import logger
except ImportError:
    from ..utils.simple_logger import logger

from ..core.config import config
from ..core.session import SessionManager


class MVPCLI:
    """Command-line interface for MVP functionality"""

    def __init__(self):
        self.session_manager = SessionManager()
        self.current_session = None

    def run(self):
        """Run the interactive CLI"""
        print("=" * 70)
        print("IRCamera PC Controller Hub - MVP CLI Interface")
        print("=" * 70)
        print(f"Version: {config.get('version', 'MVP-1.0.0')}")
        print(f"Server Port: {config.get('network.server_port', 8080)}")
        print(f"Discovery Port: {config.get('network.discovery_port', 8081)}")
        print("=" * 70)
        print()

        self.show_help()

        while True:
            try:
                command = input("\nMVP> ").strip().lower()

                if command in ['exit', 'quit', 'q']:
                    print("Shutting down MVP CLI...")
                    if self.current_session:
                        self.cmd_stop_session()
                    break
                elif command in ['help', 'h', '?']:
                    self.show_help()
                elif command == 'status':
                    self.cmd_status()
                elif command == 'config':
                    self.cmd_config()
                elif command == 'create':
                    self.cmd_create_session()
                elif command == 'start':
                    self.cmd_start_session()
                elif command == 'stop':
                    self.cmd_stop_session()
                elif command == 'discover':
                    self.cmd_discover_devices()
                elif command == 'demo':
                    self.cmd_demo()
                elif command == '':
                    continue
                else:
                    print(f"Unknown command: {command}")
                    print("Type 'help' for available commands")

            except KeyboardInterrupt:
                print("\n\nShutting down MVP CLI...")
                if self.current_session:
                    self.cmd_stop_session()
                break
            except EOFError:
                break

        return 0

    def show_help(self):
        """Show available commands"""
        print("Available commands:")
        print("  help     - Show this help message")
        print("  status   - Show current system status")
        print("  config   - Show configuration")
        print("  create   - Create a new session")
        print("  start    - Start current session recording")
        print("  stop     - Stop current session recording")
        print("  discover - Simulate device discovery")
        print("  demo     - Run automated demo")
        print("  quit     - Exit the CLI")

    def cmd_status(self):
        """Show current status"""
        print("\n--- System Status ---")
        current = self.session_manager.get_current_session()
        if current:
            print(f"Active Session: {current.name}")
            print(f"Session ID: {current.session_id}")
            print(f"State: {current.state}")
            print(f"Created: {current.created_at}")
            if current.started_at:
                print(f"Started: {current.started_at}")
            print(f"Devices: {len(current.devices)}")
            print(f"Events: {len(current.sync_events)}")
        else:
            print("No active session")

        print(f"Data Root: {self.session_manager._data_root}")

    def cmd_config(self):
        """Show configuration"""
        print("\n--- Configuration ---")
        all_config = config.get_all()
        for section, values in all_config.items():
            print(f"{section}:")
            if isinstance(values, dict):
                for key, value in values.items():
                    print(f"  {key}: {value}")
            else:
                print(f"  {values}")

    def cmd_create_session(self):
        """Create a new session"""
        if self.current_session and self.current_session.state != 'completed':
            print("⚠ Please stop the current session first")
            return

        session_name = input("Session name (or press Enter for auto): ").strip()
        if not session_name:
            session_name = None

        try:
            session = self.session_manager.create_session(session_name)
            self.current_session = session
            print(f"✓ Session created: {session.name}")
            print(f"  Session ID: {session.session_id}")
            print(f"  Directory: {self.session_manager.get_session_directory(session.session_id)}")
        except Exception as e:
            print(f"✗ Failed to create session: {e}")

    def cmd_start_session(self):
        """Start session recording"""
        if not self.current_session:
            print("⚠ No session created. Use 'create' command first")
            return

        try:
            self.session_manager.start_session()
            print(f"✓ Recording started for session: {self.current_session.name}")
            self.current_session = self.session_manager.get_current_session()
        except Exception as e:
            print(f"✗ Failed to start recording: {e}")

    def cmd_stop_session(self):
        """Stop session recording"""
        if not self.current_session:
            print("⚠ No active session to stop")
            return

        try:
            final_session = self.session_manager.end_session()
            if final_session:
                print(f"✓ Session stopped: {final_session.name}")
                print(f"  Duration: {final_session.duration_seconds:.2f} seconds")
                print(f"  Events recorded: {len(final_session.sync_events)}")
                print(f"  Devices connected: {len(final_session.devices)}")
                self.current_session = None
            else:
                print("⚠ No session was active")
        except Exception as e:
            print(f"✗ Failed to stop session: {e}")

    def cmd_discover_devices(self):
        """Simulate device discovery"""
        print("🔍 Discovering devices...")

        # Simulate discovery time
        for i in range(3):
            print("   Scanning...", end='', flush=True)
            time.sleep(0.5)
            print(f" {i + 1}/3")

        # Simulate found devices
        devices = [
            {"name": "Android-GSR-001", "ip": "192.168.1.100", "capabilities": ["rgb", "gsr"]},
            {"name": "Android-Thermal-002", "ip": "192.168.1.101", "capabilities": ["thermal", "gsr"]},
            {"name": "Shimmer-GSR-003", "ip": "192.168.1.102", "capabilities": ["gsr"]}
        ]

        print(f"\n✓ Found {len(devices)} devices:")
        for device in devices:
            caps = ", ".join(device["capabilities"])
            print(f"  • {device['name']} ({device['ip']}) - {caps}")

        # Add to current session if available
        if self.current_session:
            for device in devices:
                self.session_manager.add_device(device)
            print(f"\n✓ Added {len(devices)} devices to current session")

    def cmd_demo(self):
        """Run automated demonstration"""
        print("\n🚀 Running MVP Demo...")
        print("This will create a session, simulate device discovery, and record data")

        try:
            # Create session
            print("\n1. Creating session...")
            session = self.session_manager.create_session("Demo Session")
            self.current_session = session
            print(f"   ✓ Created: {session.name}")

            # Start recording
            print("\n2. Starting recording...")
            self.session_manager.start_session()
            print("   ✓ Recording started")

            # Simulate device discovery
            print("\n3. Discovering devices...")
            devices = [
                {"name": "Android-Demo-001", "ip": "192.168.1.100", "type": "ANDROID_NODE"},
                {"name": "Shimmer-Demo-001", "ip": "192.168.1.102", "type": "GSR_SENSOR"}
            ]

            for device in devices:
                self.session_manager.add_device(device)
                print(f"   ✓ Connected: {device['name']}")
                time.sleep(0.5)

            # Add events
            print("\n4. Recording events...")
            events = [
                ("device_connected", {"device": "Android-Demo-001"}),
                ("sensor_calibrated", {"sensor": "GSR", "value": 2.5}),
                ("recording_quality_check", {"quality": "excellent"}),
                ("data_sample", {"samples": 1280, "rate": "128Hz"})
            ]

            for event_type, event_data in events:
                self.session_manager.add_sync_event(event_type, event_data)
                print(f"   ✓ Event: {event_type}")
                time.sleep(0.3)

            # Stop recording
            print("\n5. Stopping recording...")
            time.sleep(1)
            final_session = self.session_manager.end_session()

            if final_session:
                print(f"   ✓ Demo completed successfully!")
                print(f"   • Duration: {final_session.duration_seconds:.2f} seconds")
                print(f"   • Devices: {len(final_session.devices)}")
                print(f"   • Events: {len(final_session.sync_events)}")
                print(
                    f"   • Session file: {self.session_manager.get_session_directory(final_session.session_id)}/metadata.json")

            self.current_session = None
            print("\n🎉 MVP Demo completed successfully!")

        except Exception as e:
            print(f"\n✗ Demo failed: {e}")


def main():
    """Main entry point for CLI interface"""
    try:
        cli = MVPCLI()
        return cli.run()
    except Exception as e:
        logger.error(f"CLI application failed: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
