#!/usr/bin/env python3
"""
Test script to validate NetworkController integration with MainActivity via ViewModel.
This script tests the PC-to-Android JSON command protocol integration.
"""

import json
import socket
import sys
import time
import argparse

def test_network_integration(host="192.168.1.100", port=8080):
    """Test NetworkController integration with JSON commands"""
    
    print(f"🔍 Testing NetworkController integration at {host}:{port}")
    
    try:
        # Connect to Android NetworkController
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((host, port))
        
        # Read welcome message
        welcome = sock.recv(1024).decode()
        print(f"✅ Connected successfully!")
        print(f"📱 Welcome message: {welcome.strip()}")
        
        # Test 1: Send ping command
        print("\n🏓 Testing ping command...")
        ping_cmd = json.dumps({"command": "ping"})
        sock.send((ping_cmd + '\n').encode())
        
        response = sock.recv(1024).decode()
        ping_result = json.loads(response.strip())
        print(f"📤 Sent: {ping_cmd}")
        print(f"📥 Response: {ping_result}")
        
        # Test 2: Get status
        print("\n📊 Testing get_status command...")
        status_cmd = json.dumps({"command": "get_status"})
        sock.send((status_cmd + '\n').encode())
        
        response = sock.recv(1024).decode()
        status_result = json.loads(response.strip())
        print(f"📤 Sent: {status_cmd}")
        print(f"📥 Response: {status_result}")
        
        # Test 3: Start recording command (integration test)
        print("\n🎥 Testing start_recording command (ViewModel integration)...")
        start_cmd = json.dumps({
            "command": "start_recording",
            "session_id": "INTEGRATION_TEST_001",
            "modalities": ["thermal", "GSR"],
            "saveImages": True,
            "participantId": "TEST_PARTICIPANT",
            "studyName": "NetworkController Integration Test"
        })
        sock.send((start_cmd + '\n').encode())
        
        response = sock.recv(1024).decode()
        start_result = json.loads(response.strip())
        print(f"📤 Sent: {start_cmd}")
        print(f"📥 Response: {start_result}")
        
        # Wait a moment
        time.sleep(2)
        
        # Test 4: Stop recording command
        print("\n⏹️  Testing stop_recording command...")
        stop_cmd = json.dumps({"command": "stop_recording"})
        sock.send((stop_cmd + '\n').encode())
        
        response = sock.recv(1024).decode()
        stop_result = json.loads(response.strip())
        print(f"📤 Sent: {stop_cmd}")
        print(f"📥 Response: {stop_result}")
        
        sock.close()
        
        # Validate integration
        success_indicators = [
            ping_result.get('status') == 'pong',
            'connected_clients' in status_result.get('data', {}),
            start_result.get('status') == 'recording_started',
            stop_result.get('status') == 'recording_stopped'
        ]
        
        if all(success_indicators):
            print("\n✅ NetworkController Integration: PASSED")
            print("✅ MainActivity → ViewModel → NetworkController flow working!")
            print("✅ JSON command handlers properly trigger recording actions")
            return True
        else:
            print("\n❌ NetworkController Integration: PARTIAL")
            print("⚠️  Some commands may not be properly integrated with MainActivity")
            return False
            
    except ConnectionRefusedError:
        print(f"❌ Connection refused to {host}:{port}")
        print("💡 Make sure the Android app is running and NetworkController is started")
        return False
    except socket.timeout:
        print(f"⏰ Connection timeout to {host}:{port}")
        print("💡 Check network connectivity and firewall settings")
        return False
    except json.JSONDecodeError as e:
        print(f"❌ Invalid JSON response: {e}")
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return False

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Test NetworkController integration")
    parser.add_argument("--host", default="192.168.1.100", help="Android device IP address")
    parser.add_argument("--port", type=int, default=8080, help="NetworkController port")
    
    args = parser.parse_args()
    
    success = test_network_integration(args.host, args.port)
    sys.exit(0 if success else 1)
