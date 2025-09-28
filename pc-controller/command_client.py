#!/usr/bin/env python3
"""
Command client for communicating with Android IR Camera device.
Implements robust socket communication with proper message parsing.
"""

import socket
import json
import time
from typing import Optional, Dict, Any


class CommandClient:
    """
    Client for sending commands to Android IR Camera device via TCP socket.
    """
    
    def __init__(self, host: str = "192.168.1.100", port: int = 8080):
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None
        self.socket_file = None
        self.connected = False
        self.response_timeout = 10.0
    
    def connect_to_device(self) -> bool:
        """
        Connect to the Android device.
        Returns True if connection successful, False otherwise.
        """
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(self.response_timeout)
            self.socket.connect((self.host, self.port))
            
            # Create file-like objects for reading and writing
            # This allows us to use readline() for robust message parsing
            self.socket_file = self.socket.makefile('rw', buffering=1)
            
            self.connected = True
            print(f"Connected to device at {self.host}:{self.port}")
            return True
            
        except Exception as e:
            print(f"Failed to connect to device: {e}")
            self.disconnect()
            return False
    
    def disconnect(self):
        """Disconnect from the device and clean up resources."""
        self.connected = False
        
        if self.socket_file:
            try:
                self.socket_file.close()
            except Exception:
                pass
            self.socket_file = None
        
        if self.socket:
            try:
                self.socket.close()
            except Exception:
                pass
            self.socket = None
        
        print("Disconnected from device")
    
    def send_command(self, command: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Send a command to the device and wait for response.
        
        Args:
            command: Dictionary containing the command data
            
        Returns:
            Response dictionary if successful, None if failed
        """
        if not self.connected or not self.socket_file:
            print("Not connected to device")
            return None
        
        try:
            # Send command as JSON with newline terminator
            command_json = json.dumps(command)
            self.socket_file.write(command_json + '\n')
            self.socket_file.flush()
            
            print(f"Sent command: {command_json}")
            
            # Read response using readline() for robust parsing
            # This handles partial reads automatically by reading until newline
            response_line = self.socket_file.readline()
            
            if not response_line:
                print("No response received from device")
                return None
            
            # Parse JSON response
            response = response_line.strip()
            print(f"Received response: {response}")
            
            try:
                return json.loads(response)
            except json.JSONDecodeError as e:
                print(f"Failed to parse JSON response: {e}")
                return None
                
        except socket.timeout:
            print("Command timed out")
            return None
        except Exception as e:
            print(f"Error sending command: {e}")
            return None
    
    def start_recording(self, session_id: Optional[str] = None) -> bool:
        """
        Start recording on the device.
        
        Args:
            session_id: Optional session identifier
            
        Returns:
            True if successful, False otherwise
        """
        if not session_id:
            session_id = f"session_{int(time.time())}"
        
        command = {
            "message_type": "START_RECORD",
            "session_id": session_id,
            "timestamp": int(time.time() * 1000)
        }
        
        response = self.send_command(command)
        if response and response.get("status") == "success":
            print(f"Recording started successfully with session ID: {session_id}")
            return True
        else:
            print("Failed to start recording")
            return False
    
    def stop_recording(self, session_id: Optional[str] = None) -> bool:
        """
        Stop recording on the device.
        
        Args:
            session_id: Optional session identifier
            
        Returns:
            True if successful, False otherwise
        """
        command = {
            "message_type": "STOP_RECORD",
            "timestamp": int(time.time() * 1000)
        }
        
        if session_id:
            command["session_id"] = session_id
        
        response = self.send_command(command)
        if response and response.get("status") == "success":
            print("Recording stopped successfully")
            return True
        else:
            print("Failed to stop recording")
            return False
    
    def sync_time(self) -> Optional[Dict[str, Any]]:
        """
        Perform time synchronization with the device.
        
        Returns:
            Sync result dictionary if successful, None otherwise
        """
        pc_timestamp = int(time.time() * 1000)
        
        command = {
            "message_type": "SYNC_REQUEST",
            "t_pc": pc_timestamp,
            "timestamp": pc_timestamp
        }
        
        response = self.send_command(command)
        if response and response.get("status") == "success":
            print("Time sync completed successfully")
            return response
        else:
            print("Time sync failed")
            return None
    
    def get_device_status(self) -> Optional[Dict[str, Any]]:
        """
        Get the current status of the device.
        
        Returns:
            Status dictionary if successful, None otherwise
        """
        command = {
            "message_type": "GET_STATUS",
            "timestamp": int(time.time() * 1000)
        }
        
        response = self.send_command(command)
        if response and response.get("status") == "success":
            print("Device status retrieved successfully")
            return response
        else:
            print("Failed to get device status")
            return None


def main():
    """
    Main function for testing the command client.
    """
    import argparse
    
    parser = argparse.ArgumentParser(description='Command client for IR Camera Android device')
    parser.add_argument('--host', default='192.168.1.100', help='Device IP address')
    parser.add_argument('--port', type=int, default=8080, help='Device port')
    parser.add_argument('--command', choices=['start', 'stop', 'sync', 'status'], 
                       help='Command to execute')
    
    args = parser.parse_args()
    
    client = CommandClient(args.host, args.port)
    
    if not client.connect_to_device():
        return 1
    
    try:
        if args.command == 'start':
            success = client.start_recording()
        elif args.command == 'stop':
            success = client.stop_recording()
        elif args.command == 'sync':
            result = client.sync_time()
            success = result is not None
        elif args.command == 'status':
            result = client.get_device_status()
            success = result is not None
        else:
            print("Interactive mode - type 'help' for commands")
            # Interactive mode
            while True:
                try:
                    user_input = input("Command> ").strip().lower()
                    if user_input in ['quit', 'exit']:
                        break
                    elif user_input == 'help':
                        print("Available commands: start, stop, sync, status, quit")
                    elif user_input == 'start':
                        client.start_recording()
                    elif user_input == 'stop':
                        client.stop_recording()
                    elif user_input == 'sync':
                        client.sync_time()
                    elif user_input == 'status':
                        client.get_device_status()
                    else:
                        print("Unknown command. Type 'help' for available commands.")
                except KeyboardInterrupt:
                    break
            success = True
        
        return 0 if success else 1
        
    finally:
        client.disconnect()


if __name__ == "__main__":
    exit(main())