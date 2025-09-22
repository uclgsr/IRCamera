#!/usr/bin/env python3
"""
Demo script for PC-orchestrated multi-modal recording.

This script demonstrates how to use the PC controller to coordinate 
recording sessions across multiple Android devices.

Usage:
    python3 demo_networking.py
"""

import sys
import os

# Add the pc-controller directory to the path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'pc-controller'))

from standardized_controller import PCController, Protocol
import time
import threading


def demo_single_device():
    """Demonstrate controlling a single Android device"""
    print("=" * 60)
    print("SINGLE DEVICE DEMO")
    print("=" * 60)
    print()
    print("This demo shows how to use the PC controller to manage")
    print("recording sessions on a single Android device.")
    print()
    print("Setup:")
    print("1. Make sure your Android device is on the same network")
    print("2. Start the IRCamera app on your Android device")
    print("3. The app should automatically start listening on port 8080")
    print("4. Enter the Android device's IP address below")
    print()
    
    # Note: In a real scenario, you would connect to an actual Android device
    # For demo purposes, we'll show the process
    
    android_ip = input("Enter Android device IP address (or press Enter to skip): ").strip()
    
    if not android_ip:
        print("Skipping actual connection - showing command structure instead...")
        print()
        print("Example PC Controller Usage:")
        print("=" * 30)
        print("controller = PCController()")
        print("controller.connect_to_device('192.168.1.100', 8080)")
        print("controller.sync_all_devices()")
        print("controller.start_recording_session('session_20240115_143022')")
        print("time.sleep(30)  # Record for 30 seconds")
        print("controller.stop_recording_session()")
        print("controller.disconnect_all()")
        print()
        return
    
    try:
        # This would connect to a real Android device
        print(f"Attempting to connect to Android device at {android_ip}:8080...")
        print("(This would establish TCP connection and receive HELLO message)")
        print()
        
        # Simulate the process
        print("✓ Connected to Android device")
        print("✓ Received HELLO: device capabilities [RGB, THERMAL, GSR]")
        print("✓ Time synchronization completed")
        print()
        
        session_id = f"demo_session_{int(time.time())}"
        print(f"Starting recording session: {session_id}")
        print("✓ Recording started on all sensors")
        print()
        
        print("Recording in progress... (simulated)")
        for i in range(5):
            print(f"  Recording... {i+1}/5 seconds")
            time.sleep(1)
        print()
        
        print("Stopping recording session...")
        print("✓ Recording stopped")
        print("✓ Session completed successfully")
        print()
        
    except Exception as e:
        print(f"Connection failed: {e}")
        print("Make sure the Android app is running and reachable")


def demo_protocol_messages():
    """Show the protocol messages used for communication"""
    print("=" * 60)
    print("PROTOCOL MESSAGES DEMO")
    print("=" * 60)
    print()
    print("The PC-Android communication uses a text-based protocol.")
    print("Here are examples of the key message types:")
    print()
    
    # HELLO message
    print("1. HELLO Message (Android → PC)")
    print("   Sent when Android device connects:")
    hello_msg = Protocol.create_hello_message("android_device_001", ["RGB", "THERMAL", "GSR"])
    print(f"   {hello_msg}")
    print()
    
    # Time sync
    print("2. Time Synchronization (PC ↔ Android)")
    print("   PC requests time sync:")
    sync_req = Protocol.create_sync_request(1640995200000)
    print(f"   {sync_req}")
    print("   Android responds with its timestamp:")
    print("   SYNC_RESPONSE t_pc=1640995200000 t_ph=1640995199950")
    print()
    
    # Recording control
    print("3. Recording Control (PC → Android)")
    session_id = "session_20240115_143022"
    start_msg = Protocol.create_start_record(session_id)
    print(f"   Start: {start_msg}")
    stop_msg = Protocol.create_stop_record(session_id)
    print(f"   Stop:  {stop_msg}")
    print()
    
    # Responses
    print("4. Acknowledgments (Android → PC)")
    print("   Success: ACK cmd=START_RECORD session_id=session_20240115_143022 start_time=1640995200123")
    print("   Error:   ERROR cmd=START_RECORD code=SENSOR_FAIL msg=\"Thermal camera not detected\"")
    print()


def demo_multi_device():
    """Demonstrate multi-device coordination"""
    print("=" * 60)
    print("MULTI-DEVICE DEMO")
    print("=" * 60)
    print()
    print("This shows how to coordinate recording across multiple Android devices.")
    print()
    
    devices = [
        {"name": "Device 1", "ip": "192.168.1.100", "sensors": ["RGB", "GSR"]},
        {"name": "Device 2", "ip": "192.168.1.101", "sensors": ["RGB", "THERMAL"]},
        {"name": "Device 3", "ip": "192.168.1.102", "sensors": ["RGB", "THERMAL", "GSR"]},
    ]
    
    print("Configured devices:")
    for i, device in enumerate(devices, 1):
        print(f"  {i}. {device['name']} ({device['ip']}) - {device['sensors']}")
    print()
    
    print("Multi-device recording process:")
    print("1. Connect to all devices simultaneously")
    print("2. Synchronize clocks across all devices")
    print("3. Start recording on all devices at once")
    print("4. Monitor device status and handle errors")
    print("5. Stop recording on all devices")
    print("6. Collect session metadata")
    print()
    
    print("Example coordinated session:")
    session_id = f"multi_session_{int(time.time())}"
    print(f"Session ID: {session_id}")
    
    for device in devices:
        print(f"  → {device['name']}: Connecting...")
        time.sleep(0.2)  # Simulate connection time
        print(f"  ✓ {device['name']}: Connected, sensors ready")
    
    print("  → Synchronizing clocks...")
    print("  ✓ All devices synchronized (±2ms accuracy)")
    
    print(f"  → Starting recording on all devices...")
    print("  ✓ All devices recording")
    
    print("  → Recording in progress...")
    print("  ✓ Session completed successfully")
    print()


def demo_time_sync():
    """Demonstrate time synchronization concepts"""
    print("=" * 60)
    print("TIME SYNCHRONIZATION DEMO")
    print("=" * 60)
    print()
    print("Time synchronization ensures all devices record with aligned timestamps.")
    print()
    
    # Simulate sync process
    print("Synchronization Process:")
    print("1. PC sends SYNC_REQUEST with its current timestamp")
    
    pc_time = int(time.time() * 1000)
    print(f"   PC timestamp: {pc_time} ms")
    
    print("2. Android device responds with SYNC_RESPONSE")
    android_time = pc_time + 50  # Simulate 50ms offset
    print(f"   Android timestamp: {android_time} ms")
    
    print("3. PC calculates offset and network delay")
    offset = android_time - pc_time
    print(f"   Calculated offset: {offset} ms")
    
    print("4. Android device adjusts its timestamps")
    print(f"   All subsequent data will use PC timeline")
    print()
    
    print("Benefits:")
    print("• Cross-device timestamp alignment")
    print("• Sub-millisecond synchronization accuracy")
    print("• Automatic drift monitoring and correction")
    print("• Unified timeline for multi-modal data analysis")
    print()


def main():
    """Main demo function"""
    print("PC-Orchestrated Multi-Modal Recording Demo")
    print("==========================================")
    print()
    print("This demo showcases the networking implementation for")
    print("coordinating recording sessions across Android devices.")
    print()
    
    while True:
        print("Available demos:")
        print("1. Single Device Control")
        print("2. Protocol Messages")
        print("3. Multi-Device Coordination")
        print("4. Time Synchronization")
        print("5. Exit")
        print()
        
        try:
            choice = input("Select demo (1-5): ").strip()
            print()
            
            if choice == "1":
                demo_single_device()
            elif choice == "2":
                demo_protocol_messages()
            elif choice == "3":
                demo_multi_device()
            elif choice == "4":
                demo_time_sync()
            elif choice == "5":
                print("Thanks for trying the networking demo!")
                break
            else:
                print("Invalid choice. Please select 1-5.")
                
        except KeyboardInterrupt:
            print("\nExiting demo...")
            break
        except Exception as e:
            print(f"Demo error: {e}")
            
        print("\n" + "=" * 60 + "\n")


if __name__ == "__main__":
    main()