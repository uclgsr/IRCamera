#!/usr/bin/env python3
"""
Demo Script: PC-Orchestrated Multi-Modal Recording

Demonstrates the standardized networking protocol implementation
for coordinating recording sessions between PC and Android devices.

This script simulates a PC controller that:
1. Accepts connections from Android devices
2. Performs time synchronization
3. Starts/stops coordinated recording sessions
4. Monitors live sensor data streams
"""

import sys
import os

sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from pc_controller.standardized_controller import PCController
import threading
import time


def demo_interactive_session():
    """Run an interactive demo session"""
    print("=== PC-Orchestrated Multi-Modal Recording Demo ===")
    print()
    print("This demo shows the standardized networking protocol in action.")
    print("To use this demo:")
    print("1. Start this script")
    print("2. Connect Android device(s) to the same network")
    print("3. Launch the IRCamera app on Android")
    print("4. The app will automatically connect and send HELLO message")
    print("5. Use the commands below to control recording sessions")
    print()

    controller = PCController(port=8080)

    # Start server in background
    server_thread = threading.Thread(target=controller.start, daemon=True)
    server_thread.start()

    print("PC Controller started on port 8080")
    print("Waiting for Android device connections...")
    print()
    print("Available commands:")
    print("  devices  - Show connected devices")
    print("  sync     - Synchronize time with all devices")
    print("  start    - Start recording session on all devices")
    print("  stop     - Stop current recording session")
    print("  status   - Show detailed status")
    print("  help     - Show this help")
    print("  quit     - Exit demo")
    print()

    try:
        while True:
            try:
                command = input("demo> ").strip().lower()

                if command in ['quit', 'exit', 'q']:
                    break

                elif command == 'devices':
                    status = controller.get_status()
                    devices = status['devices']
                    if devices:
                        print(f"Connected devices ({len(devices)}):")
                        for device in devices:
                            capabilities = ', '.join(device['capabilities'])
                            duration = int(device['connected_duration'])
                            offset = device['clock_offset_ms']
                            print(f"  • {device['id']}")
                            print(f"    Address: {device['address']}")
                            print(f"    Capabilities: {capabilities}")
                            print(f"    Connected: {duration}s ago")
                            print(f"    Clock offset: {offset}ms")
                    else:
                        print("No devices connected yet.")
                        print("Make sure IRCamera app is running on Android device(s).")

                elif command == 'sync':
                    synced = controller.sync_all_devices()
                    if synced > 0:
                        print(f"Time synchronization completed on {synced} device(s)")

                        # Show updated offsets
                        status = controller.get_status()
                        for device in status['devices']:
                            offset = device['clock_offset_ms']
                            print(f"  {device['id']}: {offset}ms offset")
                    else:
                        print("No devices available for synchronization")

                elif command == 'start':
                    success = controller.start_recording_session()
                    if success:
                        session_id = controller.session_manager.current_session
                        print(f"Recording session '{session_id}' started successfully!")
                        print("All connected devices are now recording.")
                        print("Use 'stop' command to end the session.")
                    else:
                        print("Failed to start recording session")
                        print("Make sure devices are connected and not already recording.")

                elif command == 'stop':
                    success = controller.stop_recording_session()
                    if success:
                        print("Recording session stopped successfully!")
                        print("All devices have stopped recording.")
                    else:
                        print("Failed to stop recording session or no active session.")

                elif command == 'status':
                    status = controller.get_status()
                    print(f"PC Controller Status:")
                    print(f"  Running: {status['running']}")
                    print(f"  Connected devices: {status['connected_devices']}")
                    print(f"  Current session: {status['current_session'] or 'None'}")
                    print(f"  Recording devices: {status['recording_devices']}")
                    print()

                    if status['devices']:
                        print("Device Details:")
                        for device in status['devices']:
                            print(f"  {device['id']}:")
                            print(f"    Status: {device['status']}")
                            print(f"    Capabilities: {', '.join(device['capabilities'])}")
                            print(f"    Clock offset: {device['clock_offset_ms']}ms")
                            print(f"    Connected: {int(device['connected_duration'])}s")

                elif command == 'help':
                    print("Commands:")
                    print("  devices  - List connected Android devices")
                    print("  sync     - Perform time synchronization")
                    print("  start    - Begin coordinated recording session")
                    print("  stop     - End current recording session")
                    print("  status   - Show detailed system status")
                    print("  help     - Show this help message")
                    print("  quit     - Exit the demo")

                elif command == '':
                    continue  # Empty input, just show prompt again

                else:
                    print(f"Unknown command: {command}")
                    print("Type 'help' for available commands.")

                print()  # Add blank line after each command

            except KeyboardInterrupt:
                print("\nShutting down...")
                break
            except Exception as e:
                print(f"Command error: {e}")
                import traceback
                traceback.print_exc()

    finally:
        print("Stopping PC Controller...")
        controller.stop()
        print("Demo completed.")


def demo_automated_session():
    """Run an automated demo session"""
    print("=== Automated Demo Session ===")
    print("This will run a simulated session for 30 seconds")
    print("Connect Android devices now...")

    controller = PCController(port=8080)

    # Start server in background
    server_thread = threading.Thread(target=controller.start, daemon=True)
    server_thread.start()

    print("PC Controller started, waiting for connections...")

    try:
        # Wait for connections
        for i in range(10):
            status = controller.get_status()
            if status['connected_devices'] > 0:
                break
            time.sleep(1)
            print(f"Waiting for connections... ({i + 1}/10)")

        status = controller.get_status()
        if status['connected_devices'] == 0:
            print("No devices connected. Make sure IRCamera app is running.")
            return

        print(f"Found {status['connected_devices']} device(s)!")

        # Sync time
        print("Synchronizing time with devices...")
        synced = controller.sync_all_devices()
        print(f"Time sync completed on {synced} device(s)")

        # Start recording
        print("Starting coordinated recording session...")
        if controller.start_recording_session():
            session_id = controller.session_manager.current_session
            print(f"Recording session '{session_id}' started!")

            # Record for 10 seconds
            for i in range(10):
                print(f"Recording... {i + 1}/10 seconds")
                time.sleep(1)

            # Stop recording
            print("Stopping recording session...")
            controller.stop_recording_session()
            print("Session completed successfully!")
        else:
            print("Failed to start recording session")

    finally:
        controller.stop()


if __name__ == "__main__":
    print("PC-Orchestrated Multi-Modal Recording System")
    print("Choose demo mode:")
    print("1. Interactive mode (recommended)")
    print("2. Automated demo")
    print()

    try:
        choice = input("Enter choice (1 or 2): ").strip()

        if choice == '1':
            demo_interactive_session()
        elif choice == '2':
            demo_automated_session()
        else:
            print("Invalid choice. Running interactive mode...")
            demo_interactive_session()

    except KeyboardInterrupt:
        print("\nDemo cancelled by user.")
    except Exception as e:
        print(f"Demo error: {e}")
        import traceback

        traceback.print_exc()
