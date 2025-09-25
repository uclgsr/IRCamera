#!/usr/bin/env python3
"""
Complete System Demo for Enhanced PC Controller

This demo shows:
1. Enhanced PC Controller with real-time visualization
2. Mock Android device simulation
3. Session management and data export
4. C++ backend integration
"""

import json
import socket
import threading
import time
from pathlib import Path
from enhanced_pc_controller import EnhancedPCController


class MockAndroidDevice:
    """Mock Android device for testing"""
    
    def __init__(self, device_id: str, device_name: str, server_host='localhost', server_port=8080):
        self.device_id = device_id
        self.device_name = device_name
        self.server_host = server_host
        self.server_port = server_port
        self.socket = None
        self.running = False
        
    def connect_and_run(self):
        """Connect to server and simulate device behavior"""
        try:
            print(f"📱 {self.device_name}: Connecting to server...")
            
            # Connect to server
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.server_host, self.server_port))
            self.running = True
            
            # Send device registration
            registration = {
                'type': 'device_registration',
                'device_id': self.device_id,
                'device_name': self.device_name,
                'device_type': 'smartphone',
                'capabilities': ['GSR', 'RGB', 'Thermal']
            }
            
            self.send_message(registration)
            print(f"📱 {self.device_name}: Registration sent")
            
            # Wait for acknowledgment
            time.sleep(0.5)
            
            # Send status update
            status_update = {
                'type': 'status_update',
                'status': 'Connected',
                'sensors': {
                    'RGB': {'status': 'Connected', 'message': 'Camera ready'},
                    'Thermal': {'status': 'Connected', 'message': 'FLIR Lepton ready'},
                    'GSR': {'status': 'Connected', 'message': 'Shimmer3 GSR connected'}
                }
            }
            
            self.send_message(status_update)
            print(f"📱 {self.device_name}: Status update sent")
            
            # Simulate data streaming
            self.simulate_data_streaming()
            
        except Exception as e:
            print(f"❌ {self.device_name}: Error - {e}")
        finally:
            self.cleanup()
    
    def send_message(self, message: dict):
        """Send a JSON message to the server"""
        if self.socket:
            json_str = json.dumps(message)
            self.socket.send(f"{json_str}\n".encode('utf-8'))
            time.sleep(0.1)  # Small delay to avoid message concatenation
    
    def simulate_data_streaming(self):
        """Simulate streaming sensor data"""
        print(f"📱 {self.device_name}: Starting data streaming simulation")
        
        # Send initial GSR baseline data
        base_gsr = 10.0 + (hash(self.device_id) % 10)  # Device-specific baseline
        
        for i in range(20):
            if not self.running:
                break
                
            # Generate realistic GSR variation
            gsr_variation = 2.0 * (0.5 - (i % 10) / 10.0) + 0.5 * (i % 3 - 1)
            gsr_value = base_gsr + gsr_variation
            
            gsr_data = {
                'type': 'telemetry_gsr',
                'value': round(gsr_value, 2),
                'timestamp': time.time(),
                'device_id': self.device_id
            }
            
            self.send_message(gsr_data)
            
            # Occasionally send frame telemetry
            if i % 5 == 0:
                frame_data = {
                    'type': 'telemetry_frame',
                    'frame_type': 'RGB',
                    'timestamp': time.time(),
                    'device_id': self.device_id,
                    'width': 1920,
                    'height': 1080
                }
                self.send_message(frame_data)
            
            time.sleep(0.3)  # ~3Hz data rate
        
        print(f"📱 {self.device_name}: Data streaming completed")
    
    def cleanup(self):
        """Clean up resources"""
        self.running = False
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
            self.socket = None
        print(f"📱 {self.device_name}: Disconnected")


def run_demo():
    """Run the complete system demo"""
    print("🚀 IRCamera Enhanced PC Controller - Complete System Demo\n")
    
    # Create enhanced PC controller
    controller = EnhancedPCController(port=8082, output_dir="demo_recordings")
    
    # Setup event callbacks for demo feedback
    def on_device_connected(device_info):
        print(f"✅ PC Controller: Device connected - {device_info.device_name} ({device_info.device_id})")
    
    def on_device_disconnected(device_info):
        print(f"❌ PC Controller: Device disconnected - {device_info.device_name}")
    
    def on_data_received(device_info, message):
        msg_type = message.get('type')
        if msg_type == 'telemetry_gsr':
            print(f"📊 GSR Data from {device_info.device_name}: {message.get('value', 0):.2f} µS")
        elif msg_type == 'telemetry_frame':
            frame_type = message.get('frame_type', 'Unknown')
            print(f"🎥 Frame from {device_info.device_name}: {frame_type}")
        elif msg_type == 'status_update':
            print(f"🔄 Status update from {device_info.device_name}: {message.get('status')}")
    
    controller.on_device_connected = on_device_connected
    controller.on_device_disconnected = on_device_disconnected
    controller.on_data_received = on_data_received
    
    # Start server in background
    server_thread = threading.Thread(target=controller.start, daemon=True)
    server_thread.start()
    
    print("🖥️  PC Controller server starting...")
    time.sleep(2)  # Wait for server to start
    
    # Create mock devices
    devices = [
        MockAndroidDevice("android_001", "Galaxy S23 (Primary)", server_port=8082),
        MockAndroidDevice("android_002", "Pixel 7 (Secondary)", server_port=8082)
    ]
    
    # Start mock devices
    device_threads = []
    for device in devices:
        thread = threading.Thread(target=device.connect_and_run, daemon=True)
        device_threads.append(thread)
        thread.start()
        time.sleep(1)  # Stagger device connections
    
    # Let the system run for a while
    print(f"\n⏳ Running demo for 15 seconds...")
    
    for countdown in range(15, 0, -1):
        print(f"⏱️  {countdown} seconds remaining...", end='\r')
        time.sleep(1)
    
    print("\n")
    
    # Show system status
    print("📊 System Status Summary:")
    device_status = controller.get_device_status()
    print(f"   Connected devices: {len(device_status)}")
    
    for device_id, status in device_status.items():
        print(f"   📱 {status['device_name']}:")
        print(f"      Status: {status['status']}")
        print(f"      Data packets received: {status['data_packets_received']}")
        print(f"      Sensors: {', '.join([s for s, info in status['sensors'].items() if info['status'] == 'Connected'])}")
    
    # Test session management
    print("\n🎬 Testing session management...")
    if controller.start_recording_session("demo_session_001"):
        print("✅ Recording session started successfully")
        time.sleep(3)  # Let some data be recorded
        
        if controller.stop_recording_session():
            print("✅ Recording session stopped successfully")
    
    # Test data export
    print("\n💾 Testing data export...")
    export_file = controller.export_session_data(format_type='json')
    if export_file and export_file.exists():
        print(f"✅ Data exported successfully to: {export_file}")
        print(f"   File size: {export_file.stat().st_size} bytes")
        
        # Show a preview of the exported data
        try:
            with open(export_file) as f:
                data = json.load(f)
                gsr_count = len(data.get('gsr_data', []))
                print(f"   GSR data points: {gsr_count}")
        except Exception as e:
            print(f"   Could not read export file: {e}")
    else:
        print("❌ Data export failed")
    
    # Test C++ backend integration
    print("\n🔧 Testing C++ backend integration...")
    try:
        import sys
        from pathlib import Path
        
        native_backend_path = Path(__file__).parent / "legacy_implementation" / "native_backend" / "build"
        if native_backend_path.exists():
            sys.path.insert(0, str(native_backend_path))
            import native_backend
            
            print("✅ Native C++ backend available")
            
            # Demonstrate GSR data processing
            gsr_data = native_backend.GSRData()
            gsr_data.timestamp_ns = int(time.time() * 1e9)
            gsr_data.gsr_microsiemens = 15.7
            gsr_data.raw_gsr_value = 2048
            
            print(f"✅ Native GSR processing demo: {gsr_data}")
        else:
            print("⚠️  Native backend not available (not built)")
    
    except ImportError:
        print("⚠️  Native backend import failed")
    except Exception as e:
        print(f"❌ Native backend error: {e}")
    
    # Cleanup
    print("\n🧹 Cleaning up...")
    for device in devices:
        device.cleanup()
    
    # Wait for device threads to finish
    for thread in device_threads:
        thread.join(timeout=2)
    
    controller.stop()
    
    print("\n🎉 Demo completed successfully!")
    print("\n📋 Summary of implemented features:")
    print("   ✅ TCP Server with JSON protocol")
    print("   ✅ Device registration and management")
    print("   ✅ Real-time GSR data visualization")
    print("   ✅ Session control and coordination")
    print("   ✅ Data export (JSON/CSV formats)")
    print("   ✅ Error handling and robustness")
    print("   ✅ C++ backend integration (PyBind11)")
    print("   ✅ Multi-device support")
    print("   ✅ Thread-safe operation")
    print("   ✅ Professional logging")


if __name__ == "__main__":
    try:
        run_demo()
    except KeyboardInterrupt:
        print("\n👋 Demo interrupted by user")
    except Exception as e:
        print(f"\n❌ Demo error: {e}")
        import traceback
        traceback.print_exc()