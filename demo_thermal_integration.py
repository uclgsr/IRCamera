#!/usr/bin/env python3
"""
Thermal Camera Integration Demo Script

This script demonstrates the PC-Android command interface for thermal camera integration.
It shows how a PC can remotely control the Android app's thermal recording functionality.

Key Features Demonstrated:
1. TCP connection to Android app
2. START/STOP recording commands
3. Time synchronization (SYNC)
4. Status monitoring
5. Error handling for thermal camera issues

Usage:
    python3 demo_thermal_integration.py [android_ip] [port]
    
    android_ip: IP address of Android device (default: 192.168.1.100)
    port: TCP port (default: 8080)
"""

import json
import socket
import sys
import time
from datetime import datetime


class ThermalIntegrationDemo:
    def __init__(self, android_ip="192.168.1.100", port=8080):
        self.android_ip = android_ip
        self.port = port
        self.socket = None
        self.connected = False

    def connect(self):
        """Establish TCP connection to Android app"""
        try:
            print(f"Connecting to Android device at {self.android_ip}:{self.port}...")
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10)
            self.socket.connect((self.android_ip, self.port))
            self.connected = True
            print("✅ Connected to Android thermal recording app")
            return True
        except Exception as e:
            print(f"❌ Connection failed: {e}")
            return False

    def send_command(self, command_dict):
        """Send JSON command to Android app"""
        if not self.connected:
            print("❌ Not connected to Android device")
            return None

        try:
            command_json = json.dumps(command_dict)
            print(f"📤 Sending: {command_json}")

            self.socket.send(command_json.encode('utf-8') + b'\n')

            # Receive response
            response = self.socket.recv(4096).decode('utf-8').strip()
            print(f"📥 Received: {response}")

            return json.loads(response) if response else None
        except Exception as e:
            print(f"❌ Command failed: {e}")
            return None

    def perform_time_sync(self):
        """Demonstrate PC-Android time synchronization"""
        print("\n🕐 Performing time synchronization...")

        # Step 1: PC sends sync request with timestamp T1
        t1 = int(time.time() * 1000)  # PC timestamp in milliseconds
        sync_request = {
            "command": "sync_request",
            "t_pc": t1
        }

        response = self.send_command(sync_request)
        if response and response.get("status") == "sync_response":
            t2 = response.get("t_ph")  # Android timestamp
            t3 = int(time.time() * 1000)  # PC receive timestamp

            # Calculate offset and RTT
            rtt = t3 - t1
            offset = t2 - t1 - (rtt // 2)

            print(f"⏱️  Time sync complete:")
            print(f"   PC->Android latency: {rtt}ms")
            print(f"   Clock offset: {offset}ms")

            return {"offset": offset, "rtt": rtt}
        else:
            print("❌ Time sync failed")
            return None

    def start_thermal_recording(self, session_id=None):
        """Start thermal camera recording with multi-modal sensors"""
        if not session_id:
            session_id = f"thermal_session_{int(time.time())}"

        print(f"\n🎥 Starting thermal recording session: {session_id}")

        start_command = {
            "command": "start_recording",
            "session_id": session_id,
            "modalities": ["RGB", "THERMAL", "GSR"],  # Multi-modal recording
            "saveImages": True,
            "samplingRate": 64,
            "studyName": "thermal_integration_demo"
        }

        response = self.send_command(start_command)
        if response and response.get("status") == "recording_started":
            print("✅ Thermal recording started successfully")
            print(f"   Session ID: {response.get('data', {}).get('session_id')}")
            print(f"   Active sensors: {response.get('data', {}).get('modalities')}")
            return True
        elif response and response.get("status") == "error":
            error_msg = response.get("message", "Unknown error")
            print(f"❌ Recording failed: {error_msg}")

            # Handle thermal camera specific errors
            if "thermal" in error_msg.lower():
                print("   🔥 Thermal camera issue detected")
                print("   - Check TC001 USB connection")
                print("   - Verify USB permissions granted")
                print("   - System may fallback to simulation mode")

            return False
        else:
            print("❌ Unexpected response from Android app")
            return False

    def monitor_recording_status(self, duration_seconds=10):
        """Monitor recording status and sensor health"""
        print(f"\n📊 Monitoring recording for {duration_seconds} seconds...")

        for i in range(duration_seconds):
            status_request = {"command": "get_status"}
            response = self.send_command(status_request)

            if response:
                print(f"   Status: {response.get('recording_state', 'unknown')}")

                # Check sensor status
                sensors = response.get('sensors', {})
                thermal_status = sensors.get('thermal', 'unknown')
                gsr_status = sensors.get('gsr', 'unknown')
                rgb_status = sensors.get('rgb', 'unknown')

                print(f"   Sensors: RGB={rgb_status}, Thermal={thermal_status}, GSR={gsr_status}")

                # Check for thermal camera issues
                if thermal_status in ['disconnected', 'error', 'simulation']:
                    print(f"   ⚠️  Thermal camera status: {thermal_status}")

            time.sleep(1)

    def stop_thermal_recording(self, session_id):
        """Stop thermal camera recording"""
        print(f"\n⏹️  Stopping recording session: {session_id}")

        stop_command = {
            "command": "stop_recording",
            "session_id": session_id
        }

        response = self.send_command(stop_command)
        if response and response.get("status") == "recording_stopped":
            print("✅ Recording stopped successfully")

            # Show session summary
            data = response.get("data", {})
            duration = data.get("duration_ms", 0) / 1000.0
            print(f"   Duration: {duration:.1f} seconds")
            print(f"   Files saved to: {data.get('session_directory', 'unknown')}")

            return True
        else:
            print("❌ Failed to stop recording")
            return False

    def run_thermal_integration_demo(self):
        """Run complete thermal camera integration demonstration"""
        print("🚀 Thermal Camera Integration Demo")
        print("=" * 50)

        # Step 1: Connect to Android app
        if not self.connect():
            return False

        # Step 2: Perform time synchronization
        sync_result = self.perform_time_sync()

        # Step 3: Start thermal recording
        session_id = f"demo_session_{int(time.time())}"
        if not self.start_thermal_recording(session_id):
            print("❌ Demo failed - could not start recording")
            return False

        # Step 4: Monitor recording
        self.monitor_recording_status(10)

        # Step 5: Stop recording
        self.stop_thermal_recording(session_id)

        print("\n✅ Thermal integration demo completed successfully!")
        print("\nKey features demonstrated:")
        print("  ✓ PC-Android TCP communication")
        print("  ✓ START/STOP recording commands")
        print("  ✓ Multi-modal sensor coordination (RGB + Thermal + GSR)")
        print("  ✓ Time synchronization")
        print("  ✓ Status monitoring")
        print("  ✓ Error handling for thermal camera issues")

        return True

    def disconnect(self):
        """Close connection to Android app"""
        if self.socket:
            self.socket.close()
            self.connected = False
            print("🔌 Disconnected from Android device")


def main():
    # Parse command line arguments
    android_ip = sys.argv[1] if len(sys.argv) > 1 else "192.168.1.100"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 8080

    print(f"Thermal Camera Integration Demo")
    print(f"Target: {android_ip}:{port}")
    print()

    # Create and run demo
    demo = ThermalIntegrationDemo(android_ip, port)

    try:
        success = demo.run_thermal_integration_demo()
        exit_code = 0 if success else 1
    except KeyboardInterrupt:
        print("\n⚠️  Demo interrupted by user")
        exit_code = 1
    except Exception as e:
        print(f"\n❌ Demo failed with error: {e}")
        exit_code = 1
    finally:
        demo.disconnect()

    sys.exit(exit_code)


if __name__ == "__main__":
    main()
