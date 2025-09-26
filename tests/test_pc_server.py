#!/usr/bin/env python3
"""
Simple PC test server for validating bidirectional command/control networking.
This server accepts connections from Android clients and demonstrates the protocol.
"""

import json
import socket
import threading
import time
from datetime import datetime


class AndroidDeviceConnection:
    def __init__(self, client_socket, address):
        self.socket = client_socket
        self.address = address
        self.device_id = None
        self.connected = True

    def send_message(self, message):
        try:
            self.socket.send((message + '\n').encode('utf-8'))
            print(f"[{self.address}] Sent: {message}")
            return True
        except Exception as e:
            print(f"[{self.address}] Error sending message: {e}")
            return False

    def close(self):
        self.connected = False
        try:
            self.socket.close()
        except:
            pass


class PCTestServer:
    def __init__(self, port=8080):
        self.port = port
        self.server_socket = None
        self.running = False
        self.connections = []

    def start(self):
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('127.0.0.1', self.port))
            self.server_socket.listen(5)
            self.running = True

            print(f"PC Test Server started on port {self.port}")
            print("Waiting for Android client connections...")

            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    print(f"New connection from {address}")

                    connection = AndroidDeviceConnection(client_socket, address)
                    self.connections.append(connection)

                    # Start handler thread for this connection
                    thread = threading.Thread(target=self.handle_client, args=(connection,))
                    thread.daemon = True
                    thread.start()

                except socket.error as e:
                    if self.running:
                        print(f"Error accepting connection: {e}")

        except Exception as e:
            print(f"Error starting server: {e}")
        finally:
            self.cleanup()

    def handle_client(self, connection):
        try:
            buffer = ""
            while connection.connected and self.running:
                try:
                    data = connection.socket.recv(1024).decode('utf-8')
                    if not data:
                        break

                    buffer += data
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        line = line.strip()
                        if line:
                            self.process_message(connection, line)

                except socket.timeout:
                    continue
                except Exception as e:
                    print(f"[{connection.address}] Error receiving data: {e}")
                    break

        except Exception as e:
            print(f"[{connection.address}] Handler error: {e}")
        finally:
            print(f"[{connection.address}] Connection closed")
            connection.close()
            if connection in self.connections:
                self.connections.remove(connection)

    def process_message(self, connection, message):
        print(f"[{connection.address}] Received: {message}")

        try:
            # Handle different message types
            if message.startswith("HELLO"):
                self.handle_hello(connection, message)
            elif message.startswith("PING"):
                self.handle_ping(connection, message)
            elif message.startswith("SYNC-RESP"):
                self.handle_sync_response(connection, message)
            elif message.startswith("START-ACK"):
                self.handle_start_ack(connection, message)
            elif message.startswith("STOP-ACK"):
                self.handle_stop_ack(connection, message)
            elif message.startswith("STATUS"):
                self.handle_status(connection, message)
            elif message.startswith("ERROR"):
                self.handle_error(connection, message)
            elif message.startswith("{"):
                self.handle_json_message(connection, message)
            else:
                print(f"[{connection.address}] Unknown message type: {message}")

        except Exception as e:
            print(f"[{connection.address}] Error processing message: {e}")

    def handle_hello(self, connection, message):
        # Extract device info from HELLO message
        # Format: HELLO device_name=<ID> sensors=[RGB,THERMAL,GSR]
        parts = message.split()
        for part in parts[1:]:
            if part.startswith("device_name="):
                connection.device_id = part.split("=", 1)[1]

        print(f"[{connection.address}] Device registered: {connection.device_id}")

        # Send welcome message
        connection.send_message("HELLO-ACK")

        # Start demo command sequence
        threading.Thread(target=self.demo_command_sequence, args=(connection,)).start()

    def handle_ping(self, connection, message):
        # Respond to PING with PONG
        print(f"[{connection.address}] Ping received, sending pong")

    def handle_sync_response(self, connection, message):
        # Parse sync response
        # Format: SYNC-RESP t_pc=<T1> t_ph=<T_phone>
        print(f"[{connection.address}] Clock sync response: {message}")

    def handle_start_ack(self, connection, message):
        print(f"[{connection.address}] Recording start acknowledged: {message}")

    def handle_stop_ack(self, connection, message):
        print(f"[{connection.address}] Recording stop acknowledged: {message}")

    def handle_status(self, connection, message):
        print(f"[{connection.address}] Status update: {message}")

    def handle_error(self, connection, message):
        print(f"[{connection.address}] Error message: {message}")

    def handle_json_message(self, connection, message):
        try:
            data = json.loads(message)
            print(f"[{connection.address}] JSON message: {json.dumps(data, indent=2)}")
        except json.JSONDecodeError as e:
            print(f"[{connection.address}] Invalid JSON: {e}")

    def demo_command_sequence(self, connection):
        """Demonstrate the command protocol by sending test commands"""
        try:
            time.sleep(2)  # Wait for initial setup

            print(f"[{connection.address}] Starting demo command sequence")

            # 1. Send PING
            connection.send_message("PING")
            time.sleep(1)

            # 2. Send clock sync request
            pc_timestamp = int(time.time() * 1000)
            connection.send_message(f"SYNC t_pc={pc_timestamp}")
            time.sleep(1)

            # 3. Request status
            connection.send_message("GET_STATUS")
            time.sleep(2)

            # 4. Start recording
            connection.send_message("START")
            time.sleep(3)

            # 5. Request status while recording
            connection.send_message("GET_STATUS")
            time.sleep(2)

            # 6. Stop recording
            connection.send_message("STOP")
            time.sleep(1)

            # 7. Final status check
            connection.send_message("GET_STATUS")

            print(f"[{connection.address}] Demo command sequence completed")

        except Exception as e:
            print(f"[{connection.address}] Error in demo sequence: {e}")

    def broadcast_message(self, message):
        """Send message to all connected clients"""
        for connection in self.connections[:]:  # Create copy to avoid modification during iteration
            if connection.connected:
                if not connection.send_message(message):
                    connection.close()
                    if connection in self.connections:
                        self.connections.remove(connection)

    def stop(self):
        print("Stopping PC Test Server...")
        self.running = False
        if self.server_socket:
            self.server_socket.close()
        self.cleanup()

    def cleanup(self):
        for connection in self.connections[:]:
            connection.close()
        self.connections.clear()


def main():
    server = PCTestServer(port=8080)

    try:
        print("Starting PC Test Server for Android client validation")
        print("This server will accept connections and demonstrate the protocol")
        server.start()
    except KeyboardInterrupt:
        print("\nShutting down server...")
        server.stop()
    except Exception as e:
        print(f"Server error: {e}")
        server.stop()


if __name__ == "__main__":
    main()
