#!/usr/bin/env python3
"""
Simple PC Controller Example

This script demonstrates how to:
1. Connect to an Android device
2. Perform time synchronization
3. Start a recording session
4. Wait for recording
5. Stop the recording session

This is a minimal example showing the verified protocol in action.
"""

import socket
import time
import sys
from protocol_adapter import ProtocolAdapter

class SimplePCController:
    """Simple PC controller for Android recording"""
    
    def __init__(self, android_ip: str = '192.168.1.100', android_port: int = 8081):
        self.android_ip = android_ip
        self.android_port = android_port
        self.socket = None
        self.adapter = ProtocolAdapter()
        self.connected = False
        
    def connect(self) -> bool:
        """Connect to Android device"""
        try:
            print(f"Connecting to Android at {self.android_ip}:{self.android_port}...")
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)
            self.socket.connect((self.android_ip, self.android_port))
            
            # Receive HELLO message
            hello = self._receive_message()
            if hello and 'HELLO' in hello:
                print(f" Connected! Android says: {hello}")
                json_hello = self.adapter.android_to_json(hello)
                print(f"  Device: {json_hello.get('device_name', 'unknown')}")
                print(f"  Sensors: {json_hello.get('sensors', [])}")
                self.connected = True
                return True
            else:
                print(f" Failed to receive HELLO message")
                return False
                
        except Exception as e:
            print(f" Connection failed: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from Android device"""
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        self.connected = False
        print("Disconnected from Android")
    
    def sync_time(self) -> bool:
        """Perform time synchronization with Android"""
        if not self.connected:
            print(" Not connected to Android")
            return False
        
        try:
            print("\nPerforming time synchronization...")
            
            # Send SYNC_REQUEST
            t1 = int(time.time() * 1000)
            self._send_command('SYNC_REQUEST', t_pc=t1)
            
            # Receive SYNC_RESPONSE
            sync_resp = self._receive_message()
            if not sync_resp or 'SYNC_RESPONSE' not in sync_resp:
                print(" Failed to receive SYNC_RESPONSE")
                return False
            
            # Parse response
            json_sync = self.adapter.android_to_json(sync_resp)
            t2 = json_sync['t_ph']
            t3 = int(time.time() * 1000)
            
            # Calculate offset and RTT (NTP algorithm)
            rtt = t3 - t1
            offset = int((t2 - t1 - rtt / 2))
            
            print(f" Time synchronized")
            print(f"  Round-trip time: {rtt}ms")
            print(f"  Clock offset: {offset}ms")
            
            # Send SYNC_RESULT back to Android
            self._send_command('SYNC_RESULT', 
                             t1=t1, t2=t2, t3=t3, 
                             offset=offset, rtt=rtt)
            
            return True
            
        except Exception as e:
            print(f" Time sync failed: {e}")
            return False
    
    def start_recording(self, session_id: str) -> bool:
        """Start recording on Android"""
        if not self.connected:
            print(" Not connected to Android")
            return False
        
        try:
            print(f"\nStarting recording session: {session_id}")
            
            # Send START_RECORD
            self._send_command('START_RECORD', session_id=session_id)
            
            # Receive response
            response = self._receive_message()
            if not response:
                print(" No response from Android")
                return False
            
            # Check if ACK or ERROR
            json_resp = self.adapter.android_to_json(response)
            
            if json_resp['type'] == 'ACK':
                print(f" Recording started successfully")
                print(f"  Response: {response}")
                return True
            elif json_resp['type'] == 'ERROR':
                print(f" Recording failed to start")
                print(f"  Error code: {json_resp.get('code', 'UNKNOWN')}")
                print(f"  Error message: {json_resp.get('msg', 'No message')}")
                return False
            else:
                print(f" Unexpected response: {response}")
                return False
                
        except Exception as e:
            print(f" Start recording failed: {e}")
            return False
    
    def stop_recording(self, session_id: str) -> bool:
        """Stop recording on Android"""
        if not self.connected:
            print(" Not connected to Android")
            return False
        
        try:
            print(f"\nStopping recording session: {session_id}")
            
            # Send STOP_RECORD
            self._send_command('STOP_RECORD', session_id=session_id)
            
            # Receive response
            response = self._receive_message()
            if not response:
                print(" No response from Android")
                return False
            
            # Check if ACK or ERROR
            json_resp = self.adapter.android_to_json(response)
            
            if json_resp['type'] == 'ACK':
                print(f" Recording stopped successfully")
                print(f"  Response: {response}")
                return True
            elif json_resp['type'] == 'ERROR':
                print(f" Recording failed to stop")
                print(f"  Error code: {json_resp.get('code', 'UNKNOWN')}")
                print(f"  Error message: {json_resp.get('msg', 'No message')}")
                return False
            else:
                print(f" Unexpected response: {response}")
                return False
                
        except Exception as e:
            print(f" Stop recording failed: {e}")
            return False
    
    def _send_command(self, command_type: str, **params):
        """Send command to Android"""
        json_msg = {'type': command_type}
        json_msg.update(params)
        android_msg = self.adapter.json_to_android(json_msg)
        self.socket.send((android_msg + '\n').encode('utf-8'))
    
    def _receive_message(self, timeout: float = 5.0) -> str:
        """Receive message from Android"""
        self.socket.settimeout(timeout)
        data = self.socket.recv(4096).decode('utf-8')
        if data and '\n' in data:
            return data.split('\n')[0].strip()
        return data.strip() if data else None

def main():
    """Main example function"""
    print("="*70)
    print("Simple PC Controller Example")
    print("="*70)
    print()
    print("This example demonstrates:")
    print("  1. Connecting to Android device")
    print("  2. Time synchronization")
    print("  3. Starting a recording session")
    print("  4. Stopping the recording session")
    print()
    
    # Configuration
    ANDROID_IP = '192.168.1.100'  # Change this to your Android device IP
    ANDROID_PORT = 8081
    SESSION_ID = f"example_session_{int(time.time())}"
    RECORDING_DURATION = 10  # seconds
    
    print(f"Configuration:")
    print(f"  Android IP: {ANDROID_IP}")
    print(f"  Android Port: {ANDROID_PORT}")
    print(f"  Session ID: {SESSION_ID}")
    print(f"  Recording Duration: {RECORDING_DURATION}s")
    print()
    
    # Check if user wants to override defaults
    if len(sys.argv) > 1:
        ANDROID_IP = sys.argv[1]
        print(f"Using Android IP from command line: {ANDROID_IP}")
    
    # Create controller
    controller = SimplePCController(ANDROID_IP, ANDROID_PORT)
    
    try:
        # Step 1: Connect
        if not controller.connect():
            print("\n Failed to connect to Android device")
            print("\nTroubleshooting:")
            print("  1. Check if Android device is on the same network")
            print("  2. Check if RecordingService is started on Android")
            print("  3. Check if the IP address is correct")
            print("  4. Check if port 8081 is not blocked by firewall")
            return 1
        
        # Step 2: Time sync
        if not controller.sync_time():
            print("\n Time synchronization failed")
            return 1
        
        # Step 3: Start recording
        if not controller.start_recording(SESSION_ID):
            print("\n Failed to start recording")
            return 1
        
        # Step 4: Record for specified duration
        print(f"\nRecording for {RECORDING_DURATION} seconds...")
        for i in range(RECORDING_DURATION):
            print(f"  {i+1}/{RECORDING_DURATION}s", end='\r')
            time.sleep(1)
        print()
        
        # Step 5: Stop recording
        if not controller.stop_recording(SESSION_ID):
            print("\n Failed to stop recording")
            return 1
        
        print("\n" + "="*70)
        print(" Example completed successfully! ")
        print("="*70)
        print()
        print("The recording session has been completed.")
        print(f"Session ID: {SESSION_ID}")
        print("Check the Android device for recorded data.")
        
        return 0
        
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")
        # Try to stop recording gracefully
        try:
            controller.stop_recording(SESSION_ID)
        except:
            pass
        return 1
        
    except Exception as e:
        print(f"\n Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        return 1
        
    finally:
        controller.disconnect()

if __name__ == '__main__':
    sys.exit(main())
