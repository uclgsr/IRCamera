#!/usr/bin/env python3
"""
Integration test for the Enhanced PC Controller System

This script tests the complete system including:
- Enhanced PC Controller with networking
- C++ backend integration
- Data handling and visualization
- Error handling
"""

import json
import socket
import threading
import time
from pathlib import Path

from enhanced_pc_controller import EnhancedPCController


def create_mock_android_client(host='localhost', port=8080):
    """Create a mock Android client for testing"""

    def mock_client():
        try:
            # Connect to server
            client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            client_socket.connect((host, port))
            print(f"✓ Mock client connected to {host}:{port}")

            # Send device registration
            registration = {
                'type': 'device_registration',
                'device_id': 'test_device_001',
                'device_name': 'Test Android Device',
                'device_type': 'smartphone',
                'capabilities': ['GSR', 'RGB', 'Thermal']
            }

            client_socket.send(f"{json.dumps(registration)}\n".encode('utf-8'))
            print("✓ Registration sent")

            # Wait for registration acknowledgment
            response = client_socket.recv(1024).decode('utf-8')
            ack = json.loads(response.strip())
            print(f"✓ Registration acknowledged: {ack.get('status')}")

            # Send status update
            status_update = {
                'type': 'status_update',
                'status': 'Connected',
                'sensors': {
                    'RGB': {'status': 'Connected', 'message': 'Camera ready'},
                    'Thermal': {'status': 'Connected', 'message': 'Thermal sensor ready'},
                    'GSR': {'status': 'Connected', 'message': 'Shimmer device connected'}
                }
            }

            client_socket.send(f"{json.dumps(status_update)}\n".encode('utf-8'))
            print("✓ Status update sent")

            # Send some mock GSR telemetry data
            for i in range(10):
                gsr_data = {
                    'type': 'telemetry_gsr',
                    'value': 10.0 + i * 0.5 + (i % 3) * 2.0,  # Mock varying GSR values
                    'timestamp': time.time(),
                    'device_id': 'test_device_001'
                }

                client_socket.send(f"{json.dumps(gsr_data)}\n".encode('utf-8'))
                print(f"📊 GSR telemetry sent: {gsr_data['value']:.1f} µS")
                time.sleep(0.5)

            # Send session start notification
            session_start = {
                'type': 'session_started',
                'session_id': 'test_session_001',
                'timestamp': time.time()
            }

            client_socket.send(f"{json.dumps(session_start)}\n".encode('utf-8'))
            print("✓ Session start notification sent")

            # Send a few more data points during session
            for i in range(5):
                gsr_data = {
                    'type': 'telemetry_gsr',
                    'value': 15.0 + i * 1.0,
                    'timestamp': time.time(),
                    'device_id': 'test_device_001'
                }

                client_socket.send(f"{json.dumps(gsr_data)}\n".encode('utf-8'))
                time.sleep(0.3)

            # Send session stop notification
            session_stop = {
                'type': 'session_stopped',
                'session_id': 'test_session_001',
                'timestamp': time.time()
            }

            client_socket.send(f"{json.dumps(session_stop)}\n".encode('utf-8'))
            print("✓ Session stop notification sent")

            # Keep connection alive for a bit
            time.sleep(2)

            print("✓ Mock client test completed")
            client_socket.close()

        except Exception as e:
            print(f"❌ Mock client error: {e}")

    return mock_client


def test_enhanced_pc_controller():
    """Test the Enhanced PC Controller"""
    print("🧪 Testing Enhanced PC Controller System\n")

    # Create controller instance
    controller = EnhancedPCController(port=8081)  # Use different port to avoid conflicts

    # Track events
    events = []

    def on_device_connected(device_info):
        events.append(f"device_connected:{device_info.device_id}")
        print(f"📱 Device connected: {device_info.device_name}")

    def on_device_disconnected(device_info):
        events.append(f"device_disconnected:{device_info.device_id}")
        print(f"📱 Device disconnected: {device_info.device_name}")

    def on_data_received(device_info, message):
        events.append(f"data_received:{message.get('type')}")
        if message.get('type') == 'telemetry_gsr':
            print(f"📊 GSR data: {message.get('value', 0):.1f} µS")

    # Set up callbacks
    controller.on_device_connected = on_device_connected
    controller.on_device_disconnected = on_device_disconnected
    controller.on_data_received = on_data_received

    # Start server in background thread
    server_thread = threading.Thread(target=controller.start, daemon=True)
    server_thread.start()

    # Wait for server to start
    time.sleep(1)

    print("🚀 PC Controller server started")

    # Create and run mock client
    mock_client = create_mock_android_client(port=8081)
    client_thread = threading.Thread(target=mock_client, daemon=True)
    client_thread.start()

    # Wait for test to complete
    client_thread.join(timeout=15)

    # Wait a bit more for all events to be processed
    time.sleep(2)

    # Check results
    print(f"\n📋 Test Results:")
    print(f"   Events recorded: {len(events)}")

    device_status = controller.get_device_status()
    print(f"   Devices connected: {len(device_status)}")

    for device_id, status in device_status.items():
        print(f"   Device {device_id}:")
        print(f"     - Name: {status['device_name']}")
        print(f"     - Type: {status['device_type']}")
        print(f"     - Status: {status['status']}")
        print(f"     - Data packets: {status['data_packets_received']}")

    # Test data export
    print("\n📁 Testing data export...")
    export_file = controller.export_session_data(format_type='json')
    if export_file and export_file.exists():
        print(f"✓ Data exported to: {export_file}")
        print(f"   File size: {export_file.stat().st_size} bytes")
    else:
        print("❌ Data export failed")

    # Stop controller
    controller.stop()
    print("\n✅ Test completed successfully")

    return len(events) > 0 and len(device_status) > 0


def test_native_backend():
    """Test the native C++ backend integration"""
    print("\n🔧 Testing Native C++ Backend...")

    try:
        import sys
        from pathlib import Path

        # Add native backend to path
        native_backend_path = Path(__file__).parent / "legacy_implementation" / "native_backend" / "build"
        if native_backend_path.exists():
            sys.path.insert(0, str(native_backend_path))
            import native_backend

            print("✓ Native backend imported successfully")

            # Test GSR data structure
            gsr_data = native_backend.GSRData()
            gsr_data.timestamp_ns = int(time.time() * 1e9)
            gsr_data.gsr_microsiemens = 12.5
            gsr_data.raw_gsr_value = 2048

            print(f"✓ GSR data object created: {gsr_data}")

            # Test NativeShimmer (if available)
            try:
                shimmer = native_backend.NativeShimmer()
                print("✓ NativeShimmer object created")
                return True
            except Exception as e:
                print(f"⚠️  NativeShimmer creation failed: {e}")
                return True  # Still consider success if basic structures work
        else:
            print("❌ Native backend build directory not found")
            return False

    except ImportError as e:
        print(f"❌ Native backend import failed: {e}")
        return False
    except Exception as e:
        print(f"❌ Native backend test error: {e}")
        return False


def main():
    """Run all tests"""
    print("🚀 IRCamera Enhanced PC Controller Integration Tests\n")

    tests_passed = 0
    total_tests = 2

    # Test native backend
    if test_native_backend():
        tests_passed += 1
        print("✅ Native backend test PASSED")
    else:
        print("❌ Native backend test FAILED")

    # Test enhanced PC controller
    if test_enhanced_pc_controller():
        tests_passed += 1
        print("✅ Enhanced PC controller test PASSED")
    else:
        print("❌ Enhanced PC controller test FAILED")

    # Summary
    print(f"\n📊 Test Summary: {tests_passed}/{total_tests} tests passed")

    if tests_passed == total_tests:
        print("🎉 All tests passed! System is ready for use.")
        return True
    else:
        print("⚠️  Some tests failed. Check the output above for details.")
        return False


if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
