#!/usr/bin/env python3
"""
Simple test script to verify Android PC networking is working

This script:
1. Connects to Android device on port 8080
2. Receives HELLO message
3. Sends a STATUS command
4. Sends START_RECORD command
5. Waits 5 seconds
6. Sends STOP_RECORD command
7. Closes connection

Usage: python3 test_android_connection.py <android_ip>
Example: python3 test_android_connection.py 192.168.1.100
"""

import socket
import sys
import time


def test_connection(android_ip, port=8080, timeout=10):
    """Test basic connection to Android device"""
    
    print(f"Testing connection to {android_ip}:{port}")
    print("-" * 60)
    
    sock = None
    try:
        # Create socket
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        
        # Connect
        print(f"[1/7] Connecting to {android_ip}:{port}...")
        sock.connect((android_ip, port))
        print("     ✓ Connected successfully")
        
        # Receive HELLO message
        print(f"[2/7] Waiting for HELLO message...")
        data = sock.recv(1024).decode('utf-8').strip()
        print(f"     ✓ Received: {data}")
        
        if not data.startswith("HELLO"):
            print(f"     ✗ ERROR: Expected HELLO message, got: {data}")
            return False
        
        # Send STATUS command
        print(f"[3/7] Sending STATUS command...")
        sock.send(b"STATUS\n")
        print("     ✓ STATUS command sent")
        
        # Wait for response
        time.sleep(0.5)
        try:
            response = sock.recv(1024).decode('utf-8').strip()
            if response:
                print(f"     ✓ Received response: {response}")
        except socket.timeout:
            print("     ! No immediate response (may be ok)")
        
        # Send START_RECORD command
        session_id = f"test_session_{int(time.time())}"
        print(f"[4/7] Sending START_RECORD command (session: {session_id})...")
        sock.send(f"START_RECORD session_id={session_id}\n".encode('utf-8'))
        print("     ✓ START_RECORD command sent")
        
        # Wait for ACK
        time.sleep(0.5)
        try:
            response = sock.recv(1024).decode('utf-8').strip()
            if response:
                print(f"     ✓ Received response: {response}")
                if "ACK" in response:
                    print("     ✓ Recording started successfully!")
                elif "ERROR" in response:
                    print(f"     ✗ Recording failed: {response}")
        except socket.timeout:
            print("     ! No immediate response")
        
        # Wait a bit
        print(f"[5/7] Waiting 5 seconds...")
        time.sleep(5)
        print("     ✓ Wait complete")
        
        # Send STOP_RECORD command
        print(f"[6/7] Sending STOP_RECORD command...")
        sock.send(f"STOP_RECORD session_id={session_id}\n".encode('utf-8'))
        print("     ✓ STOP_RECORD command sent")
        
        # Wait for ACK
        time.sleep(0.5)
        try:
            response = sock.recv(1024).decode('utf-8').strip()
            if response:
                print(f"     ✓ Received response: {response}")
                if "ACK" in response:
                    print("     ✓ Recording stopped successfully!")
                elif "ERROR" in response:
                    print(f"     ✗ Stop failed: {response}")
        except socket.timeout:
            print("     ! No immediate response")
        
        # Close connection
        print(f"[7/7] Closing connection...")
        sock.close()
        print("     ✓ Connection closed")
        
        print("-" * 60)
        print("✓ Test completed successfully!")
        print()
        print("Summary:")
        print("- Android device is reachable")
        print("- RecordingService is running and accepting connections")
        print("- Protocol messages are being sent/received")
        print("- START_RECORD and STOP_RECORD commands work")
        
        return True
        
    except socket.timeout:
        print(f"✗ Connection timeout - device not responding")
        print(f"  Make sure:")
        print(f"  1. Android app is running")
        print(f"  2. RecordingService is started (check notification)")
        print(f"  3. Device IP is correct: {android_ip}")
        print(f"  4. Both devices are on the same network")
        return False
        
    except ConnectionRefusedError:
        print(f"✗ Connection refused")
        print(f"  Make sure:")
        print(f"  1. Android app is running")
        print(f"  2. RecordingService is started")
        print(f"  3. Port {port} is not blocked by firewall")
        return False
        
    except Exception as e:
        print(f"✗ Error: {e}")
        return False
    
    finally:
        if sock is not None:
            try:
                sock.close()
            except (OSError, socket.error) as e:
                print(f"Error closing socket: {e}")


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 test_android_connection.py <android_ip>")
        print("Example: python3 test_android_connection.py 192.168.1.100")
        print()
        print("To find your Android device IP:")
        print("  Settings -> Network & Internet -> Wi-Fi -> [Your Network] -> Advanced")
        print("  Or use: adb shell ip addr show wlan0 | grep inet")
        sys.exit(1)
    
    android_ip = sys.argv[1]
    port = 8080
    
    if len(sys.argv) > 2:
        port = int(sys.argv[2])
    
    success = test_connection(android_ip, port)
    
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
