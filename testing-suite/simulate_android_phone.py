#!/usr/bin/env python3
"""
Simulate Android Phone TCP Client

This script simulates an Android phone connecting to the PC controller,
implementing the Android protocol specified in Protocol.kt.

Usage:
    python simulate_android_phone.py --host localhost --port 8080
"""

import socket
import time
import sys
import argparse
from typing import Optional


class AndroidPhoneSimulator:
    """Simulates an Android phone TCP client"""
    
    def __init__(self, host: str = 'localhost', port: int = 8080, device_id: str = None):
        self.host = host
        self.port = port
        self.device_id = device_id or f"android_sim_{int(time.time())}"
        self.socket: Optional[socket.socket] = None
        self.connected = False
        
    def connect(self) -> bool:
        """Connect to PC controller server"""
        try:
            print(f"[INFO] Connecting to PC controller at {self.host}:{self.port}...")
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"[SUCCESS] Connected to {self.host}:{self.port}")
            return True
        except socket.timeout:
            print(f"[ERROR] Connection timeout to {self.host}:{self.port}")
            return False
        except ConnectionRefusedError:
            print(f"[ERROR] Connection refused by {self.host}:{self.port}")
            print("[HINT] Make sure the PC controller server is running")
            return False
        except Exception as e:
            print(f"[ERROR] Failed to connect: {e}")
            return False
    
    def send_hello(self) -> bool:
        """Send HELLO message to PC controller"""
        try:
            # Format: HELLO device_name=<ID> sensors=[RGB,THERMAL,GSR]
            sensors = "RGB,THERMAL,GSR"
            hello_msg = f"HELLO device_name={self.device_id} sensors=[{sensors}]\n"
            
            print(f"[SEND] {hello_msg.strip()}")
            self.socket.send(hello_msg.encode('utf-8'))
            
            # Wait for ACK
            response = self.socket.recv(4096).decode('utf-8').strip()
            print(f"[RECV] {response}")
            
            if "ACK" in response:
                print("[SUCCESS] HELLO handshake completed")
                return True
            else:
                print("[WARNING] Expected ACK but got different response")
                return False
                
        except socket.timeout:
            print("[ERROR] Timeout waiting for ACK")
            return False
        except Exception as e:
            print(f"[ERROR] Failed to send HELLO: {e}")
            return False
    
    def send_message(self, message: str) -> Optional[str]:
        """Send a message and wait for response"""
        try:
            if not message.endswith('\n'):
                message += '\n'
            
            print(f"[SEND] {message.strip()}")
            self.socket.send(message.encode('utf-8'))
            
            # Wait for response with timeout
            self.socket.settimeout(5.0)
            response = self.socket.recv(4096).decode('utf-8').strip()
            print(f"[RECV] {response}")
            
            return response
            
        except socket.timeout:
            print("[WARNING] Timeout waiting for response")
            return None
        except Exception as e:
            print(f"[ERROR] Failed to send message: {e}")
            return None
    
    def test_sync_protocol(self) -> bool:
        """Test time synchronization protocol"""
        print("\n=== Testing Time Synchronization ===")
        try:
            # PC should send SYNC_REQUEST
            # We'll wait for it or simulate receiving it
            pc_timestamp = int(time.time() * 1000)
            
            # Simulate receiving SYNC_REQUEST from PC
            # In real scenario, we'd receive this
            print("[INFO] Simulating SYNC_REQUEST from PC...")
            
            # Send SYNC_RESPONSE
            phone_timestamp = int(time.time() * 1000)
            sync_response = f"SYNC_RESPONSE t_pc={pc_timestamp} t_ph={phone_timestamp}\n"
            print(f"[SEND] {sync_response.strip()}")
            self.socket.send(sync_response.encode('utf-8'))
            
            # Wait for SYNC_RESULT
            self.socket.settimeout(5.0)
            response = self.socket.recv(4096).decode('utf-8').strip()
            print(f"[RECV] {response}")
            
            if "SYNC_RESULT" in response or "ACK" in response:
                print("[SUCCESS] Time synchronization completed")
                return True
            else:
                print("[WARNING] Unexpected response to sync")
                return False
                
        except Exception as e:
            print(f"[ERROR] Sync protocol failed: {e}")
            return False
    
    def test_recording_commands(self) -> bool:
        """Test recording start/stop commands"""
        print("\n=== Testing Recording Commands ===")
        
        # Test START_RECORD
        session_id = f"session_{int(time.time())}"
        start_msg = f"START_RECORD session_id={session_id}"
        response = self.send_message(start_msg)
        
        if not response or "ACK" not in response:
            print("[ERROR] START_RECORD failed")
            return False
        
        # Wait a bit
        time.sleep(1)
        
        # Test STOP_RECORD
        stop_msg = f"STOP_RECORD session_id={session_id}"
        response = self.send_message(stop_msg)
        
        if not response or "ACK" not in response:
            print("[ERROR] STOP_RECORD failed")
            return False
        
        print("[SUCCESS] Recording commands test passed")
        return True
    
    def send_gsr_data(self, num_samples: int = 5) -> bool:
        """Send simulated GSR data"""
        print(f"\n=== Sending {num_samples} GSR Data Samples ===")
        
        for i in range(num_samples):
            timestamp = int(time.time() * 1000)
            gsr_value = 15.5 + (i * 0.1)  # Simulate increasing GSR
            
            # Format: DATA_GSR timestamp=<T> value=<V> session_id=<SID>
            gsr_msg = f"DATA_GSR timestamp={timestamp} value={gsr_value} session_id=test_session"
            response = self.send_message(gsr_msg)
            
            if response and "ACK" in response:
                print(f"[SUCCESS] GSR sample {i+1}/{num_samples} acknowledged")
            else:
                print(f"[WARNING] GSR sample {i+1}/{num_samples} not acknowledged")
            
            time.sleep(0.5)  # 2 Hz sample rate
        
        return True
    
    def run_full_test(self) -> bool:
        """Run full connection and protocol test"""
        print("\n" + "="*60)
        print("Android Phone Simulator - Full Connection Test")
        print("="*60)
        
        # Step 1: Connect
        if not self.connect():
            print("\n[FAILED] Could not connect to PC controller")
            return False
        
        # Step 2: HELLO handshake
        if not self.send_hello():
            print("\n[FAILED] HELLO handshake failed")
            self.disconnect()
            return False
        
        # Step 3: Test sync protocol (optional)
        # self.test_sync_protocol()
        
        # Step 4: Test recording commands
        if not self.test_recording_commands():
            print("\n[FAILED] Recording commands test failed")
            self.disconnect()
            return False
        
        # Step 5: Send some GSR data
        self.send_gsr_data(num_samples=3)
        
        # Success!
        print("\n" + "="*60)
        print("[SUCCESS] All tests passed!")
        print("Phone and PC controller can communicate successfully")
        print("="*60)
        
        self.disconnect()
        return True
    
    def disconnect(self):
        """Disconnect from server"""
        if self.socket:
            try:
                self.socket.close()
                print("[INFO] Disconnected from server")
            except Exception:
                pass
        self.connected = False


def main():
    parser = argparse.ArgumentParser(
        description='Simulate Android phone connecting to PC controller'
    )
    parser.add_argument(
        '--host',
        default='localhost',
        help='PC controller host address (default: localhost)'
    )
    parser.add_argument(
        '--port',
        type=int,
        default=8080,
        help='PC controller port (default: 8080)'
    )
    parser.add_argument(
        '--device-id',
        default=None,
        help='Device ID for simulation (default: auto-generated)'
    )
    parser.add_argument(
        '--quick',
        action='store_true',
        help='Quick test: just connect and HELLO handshake'
    )
    
    args = parser.parse_args()
    
    # Create simulator
    simulator = AndroidPhoneSimulator(
        host=args.host,
        port=args.port,
        device_id=args.device_id
    )
    
    # Run test
    if args.quick:
        # Quick test
        success = simulator.connect() and simulator.send_hello()
        simulator.disconnect()
    else:
        # Full test
        success = simulator.run_full_test()
    
    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
