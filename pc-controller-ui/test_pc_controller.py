#!/usr/bin/env python3
"""
Simple test script to demonstrate PC Session Controller functionality
without needing actual Android device connection.
"""

import time
import threading
import json
from src.pc_session_controller import SessionController, DeviceStatus


class MockAndroidDevice:
    """Mock Android device for testing PC controller"""
    
    def __init__(self, device_name: str, port: int = 8080):
        self.device_name = device_name
        self.port = port
        self.socket = None
        self.running = False
        
    def connect_to_controller(self, host: str = 'localhost'):
        """Connect to PC controller and simulate device behavior"""
        import socket
        
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((host, self.port))
            self.running = True
            
            print(f"Mock device {self.device_name} connected to PC controller")
            
            # Send initial status
            self.send_status_update()
            
            # Start simulation thread
            sim_thread = threading.Thread(target=self.simulation_loop, daemon=True)
            sim_thread.start()
            
            # Listen for commands
            self.listen_for_commands()
            
        except Exception as e:
            print(f"Failed to connect mock device: {e}")
            
    def send_status_update(self):
        """Send status update to PC controller"""
        status_msg = {
            'type': 'status_update',
            'status': 'Connected',
            'sensors': {
                'RGB': {'status': 'Connected', 'message': 'Mock RGB camera ready'},
                'Thermal': {'status': 'Connected', 'message': 'Mock thermal camera ready'},
                'GSR': {'status': 'Connected', 'message': 'Mock GSR sensor ready'}
            }
        }
        self.send_message(status_msg)
        
    def send_message(self, message):
        """Send JSON message to PC controller"""
        if self.socket:
            try:
                msg_str = json.dumps(message)
                self.socket.send(msg_str.encode('utf-8'))
            except Exception as e:
                print(f"Failed to send message: {e}")
                
    def simulation_loop(self):
        """Simulate sensor data"""
        gsr_value = 500.0
        
        while self.running:
            try:
                # Simulate GSR data
                import random
                gsr_value += random.uniform(-10, 10)
                gsr_value = max(200, min(800, gsr_value))  # Keep in reasonable range
                
                gsr_msg = {
                    'type': 'telemetry_gsr',
                    'value': gsr_value,
                    'timestamp': time.time()
                }
                self.send_message(gsr_msg)
                
                time.sleep(0.1)  # 10Hz GSR data
                
            except Exception as e:
                print(f"Simulation error: {e}")
                break
                
    def listen_for_commands(self):
        """Listen for commands from PC controller"""
        while self.running:
            try:
                data = self.socket.recv(4096)
                if not data:
                    break
                    
                command = json.loads(data.decode('utf-8'))
                self.handle_command(command)
                
            except Exception as e:
                print(f"Command listening error: {e}")
                break
                
        self.socket.close()
        
    def handle_command(self, command):
        """Handle command from PC controller"""
        cmd_type = command.get('type', '')
        
        if cmd_type == 'start_recording':
            session_id = command.get('session_id', 'unknown')
            print(f"Mock device {self.device_name} starting recording: {session_id}")
            
            # Send recording started response
            response = {
                'type': 'recording_started',
                'session_id': session_id,
                'timestamp': time.time()
            }
            self.send_message(response)
            
        elif cmd_type == 'stop_recording':
            print(f"Mock device {self.device_name} stopping recording")
            
            # Send recording stopped response  
            response = {
                'type': 'recording_stopped',
                'timestamp': time.time()
            }
            self.send_message(response)
            
        elif cmd_type == 'sync_request':
            pc_timestamp = command.get('pc_timestamp', time.time())
            device_timestamp = time.time()
            
            # Simulate some network delay
            offset_ms = int((device_timestamp - pc_timestamp) * 1000)
            rtt_ms = 5  # Mock 5ms round trip time
            
            response = {
                'type': 'sync_response',
                'offset_ms': offset_ms,
                'rtt_ms': rtt_ms,
                'timestamp': time.time()
            }
            self.send_message(response)
            
        elif cmd_type == 'status_request':
            self.send_status_update()


def test_pc_controller():
    """Test function to demonstrate PC controller with mock devices"""
    print("Starting PC Session Controller Test...")
    print("1. PC Controller will start first")
    print("2. Mock Android devices will connect after 3 seconds")
    print("3. Use the GUI to test Start All/Stop All/Sync functions")
    print("4. Close the PC Controller window to exit")
    print()
    
    # Start PC controller in separate thread
    controller_thread = threading.Thread(target=run_pc_controller, daemon=True)
    controller_thread.start()
    
    # Wait for PC controller to start
    time.sleep(3)
    
    # Create and connect mock devices
    devices = [
        MockAndroidDevice("Samsung-S22-Mock"),
        MockAndroidDevice("TestDevice-2")
    ]
    
    device_threads = []
    for device in devices:
        device_thread = threading.Thread(target=device.connect_to_controller, daemon=True)
        device_thread.start()
        device_threads.append(device_thread)
        time.sleep(1)  # Stagger connections
    
    # Keep main thread alive
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nTest interrupted by user")
        for device in devices:
            device.running = False


def run_pc_controller():
    """Run the PC controller"""
    try:
        app = SessionController()
        app.run()
    except Exception as e:
        print(f"PC Controller error: {e}")


if __name__ == "__main__":
    test_pc_controller()