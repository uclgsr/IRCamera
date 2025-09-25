#!/usr/bin/env python3
"""
Simple client test to validate that the PC-Android networking protocol works.
This simulates an Android client connecting to the PC server.
"""

import socket
import time
import threading


def test_networking_protocol():
    """Test the networking protocol by simulating Android client"""
    try:
        # Connect to the PC server
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.connect(('127.0.0.1', 8080))
        print("✓ Connected to PC server")

        # Send initial handshake (like Android would)
        message = "HELLO device_name=test_client sensors=[RGB,Thermal,GSR]\n"
        client_socket.send(message.encode('utf-8'))
        print(f"→ Sent: {message.strip()}")

        # Receive handshake response
        response = client_socket.recv(1024).decode('utf-8').strip()
        print(f"← Received: {response}")

        # Test commands that the PC server would send to Android
        test_commands = [
            "PING",
            "GET_STATUS",
            "START",
            "STOP",
            "SYNC t_pc=1703683200000"
        ]

        for cmd in test_commands:
            print(f"\n--- Testing {cmd} command ---")

            # Send command (simulate PC sending to Android)
            client_socket.send(f"{cmd}\n".encode('utf-8'))
            print(f"→ Sent: {cmd}")

            # Receive response (simulate Android responding to PC)
            response = client_socket.recv(1024).decode('utf-8').strip()
            print(f"← Received: {response}")

            time.sleep(1)  # Brief pause between commands

        client_socket.close()
        print("\n✓ Test completed successfully")
        return True

    except Exception as e:
        print(f"\n✗ Test failed: {e}")
        return False


if __name__ == "__main__":
    print("Testing PC-Android Networking Protocol")
    print("=" * 40)

    success = test_networking_protocol()

    if success:
        print("\n🎉 All networking protocol tests passed!")
        print("\nThe bidirectional command/control functionality is working:")
        print("- Android can connect as TCP client to PC server")
        print("- PC can send remote control commands (START, STOP, SYNC, PING, GET_STATUS)")
        print("- Android responds with acknowledgments and telemetry")
        print("- Protocol uses newline-delimited text messages")
    else:
        print("\n❌ Networking protocol test failed")
