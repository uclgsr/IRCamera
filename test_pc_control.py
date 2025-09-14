#!/usr/bin/env python3
"""
Test script for IRCamera PC Remote Control Protocol

This script demonstrates how to send JSON commands to the IRCamera Android app
for remote multi-modal recording control.

Usage Examples:
    # Start multi-modal recording with all sensors
    python test_pc_control.py --host 192.168.1.100 --command start --session-id "TEST_001" --participant-id "P001"
    
    # Start thermal-only recording  
    python test_pc_control.py --host 192.168.1.100 --command start --modalities thermal --session-id "THERMAL_TEST"
    
    # Stop current recording
    python test_pc_control.py --host 192.168.1.100 --command stop
    
    # Check device status
    python test_pc_control.py --host 192.168.1.100 --command status
    
    # Test connectivity
    python test_pc_control.py --host 192.168.1.100 --command ping

Prerequisites:
    1. IRCamera Android app must be running
    2. Android device must be on the same network
    3. NetworkController must be initialized (happens automatically in MainActivity)

Protocol Details:
    - Transport: TCP/IP with JSON messages  
    - Port: 8080 (default)
    - Commands: start_recording, stop_recording, ping, get_status
    - Response: JSON acknowledgment with status
"""

import json
import socket
import sys
import argparse
import time
from typing import Dict, Any, Optional

class IRCameraController:
    """PC Controller for IRCamera Android app via JSON protocol"""
    
    def __init__(self, host: str, port: int = 8080):
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None
    
    def connect(self) -> bool:
        """Connect to IRCamera Android app"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10)
            self.socket.connect((self.host, self.port))
            print(f"✅ Connected to IRCamera app at {self.host}:{self.port}")
            
            # Wait for welcome message
            welcome = self.socket.recv(1024).decode('utf-8')
            if welcome:
                response = json.loads(welcome.strip())
                print(f"📱 {response.get('message', 'Connected')}")
            
            return True
        except Exception as e:
            print(f"❌ Failed to connect: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from IRCamera app"""
        if self.socket:
            self.socket.close()
            self.socket = None
            print("🔌 Disconnected")
    
    def send_command(self, command: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Send JSON command and receive response"""
        if not self.socket:
            print("❌ Not connected")
            return None
        
        try:
            # Send command
            command_json = json.dumps(command)
            print(f"📤 Sending: {command_json}")
            self.socket.send(command_json.encode('utf-8') + b'\n')
            
            # Receive response
            response_data = self.socket.recv(4096).decode('utf-8').strip()
            print(f"📥 Received: {response_data}")
            
            response = json.loads(response_data)
            return response
            
        except Exception as e:
            print(f"❌ Communication error: {e}")
            return None
    
    def start_recording(self, session_id: str = None, modalities: list = None, 
                       save_images: bool = True, participant_id: str = None,
                       study_name: str = None) -> bool:
        """Start multi-modal recording session"""
        if not session_id:
            session_id = f"PC_SESSION_{int(time.time())}"
        
        if not modalities:
            modalities = ["thermal", "GSR"]
        
        command = {
            "command": "start_recording",
            "session_id": session_id,
            "modalities": modalities,
            "saveImages": save_images
        }
        
        if participant_id:
            command["participantId"] = participant_id
        
        if study_name:
            command["studyName"] = study_name
        
        response = self.send_command(command)
        
        if response and response.get("status") == "recording_started":
            print(f"🎥 Recording started successfully!")
            print(f"📋 Session ID: {session_id}")
            print(f"📊 Modalities: {modalities}")
            return True
        else:
            print(f"❌ Failed to start recording: {response}")
            return False
    
    def stop_recording(self) -> bool:
        """Stop current recording session"""
        command = {"command": "stop_recording"}
        
        response = self.send_command(command)
        
        if response and response.get("status") == "recording_stopped":
            print("⏹️ Recording stopped successfully!")
            return True
        else:
            print(f"❌ Failed to stop recording: {response}")
            return False
    
    def ping(self) -> bool:
        """Test connection with ping command"""
        command = {"command": "ping"}
        
        response = self.send_command(command)
        
        if response and response.get("status") == "pong":
            print("🏓 Pong! Connection is healthy")
            return True
        else:
            print(f"❌ Ping failed: {response}")
            return False
    
    def get_status(self) -> Optional[Dict[str, Any]]:
        """Get server status"""
        command = {"command": "get_status"}
        
        response = self.send_command(command)
        
        if response and response.get("status") == "status":
            print("📊 Server Status:")
            data = response.get("data", {})
            for key, value in data.items():
                print(f"   {key}: {value}")
            return data
        else:
            print(f"❌ Failed to get status: {response}")
            return None

def main():
    parser = argparse.ArgumentParser(description="IRCamera PC Remote Control Test")
    parser.add_argument("--host", required=True, help="Android device IP address")
    parser.add_argument("--port", type=int, default=8080, help="Port number (default: 8080)")
    parser.add_argument("--command", choices=["start", "stop", "ping", "status"], 
                       required=True, help="Command to send")
    parser.add_argument("--session-id", help="Session ID for recording")
    parser.add_argument("--modalities", nargs="+", default=["thermal", "GSR"],
                       help="Recording modalities")
    parser.add_argument("--participant-id", help="Participant ID")
    parser.add_argument("--study-name", help="Study name")
    parser.add_argument("--no-images", action="store_true", help="Don't save images")
    
    args = parser.parse_args()
    
    print(f"🚀 IRCamera PC Remote Control Test")
    print(f"📱 Target: {args.host}:{args.port}")
    print(f"📋 Command: {args.command}")
    print("-" * 50)
    
    # Create controller and connect
    controller = IRCameraController(args.host, args.port)
    
    if not controller.connect():
        return 1
    
    try:
        success = False
        
        if args.command == "start":
            success = controller.start_recording(
                session_id=args.session_id,
                modalities=args.modalities,
                save_images=not args.no_images,
                participant_id=args.participant_id,
                study_name=args.study_name
            )
        elif args.command == "stop":
            success = controller.stop_recording()
        elif args.command == "ping":
            success = controller.ping()
        elif args.command == "status":
            success = controller.get_status() is not None
        
        return 0 if success else 1
        
    finally:
        controller.disconnect()

if __name__ == "__main__":
    sys.exit(main())
