#!/usr/bin/env python3
"""
Example PC Server with Time Synchronization Support

This is a complete example showing how to handle SYNC_INIT messages from Android.
"""

import logging
import socket
import sys
import time
from protocol_adapter import ProtocolAdapter
from sync_handler import SyncHandler

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class PCServer:
    """Simple PC server that handles Android connections and time sync
    
    Note: This is a simple example implementation that handles clients sequentially.
    For production use, consider using threading or asyncio for concurrent client handling.
    """

    def __init__(self, port=8080):
        self.port = port
        self.sync_handler = SyncHandler()
        self.protocol = ProtocolAdapter()
        self.running = False

    def start(self):
        """Start the server and listen for connections"""
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_socket.bind(('0.0.0.0', self.port))
        server_socket.listen(5)

        self.running = True
        logger.info(f"PC Server listening on port {self.port}")
        logger.info("Waiting for Android connections...")

        try:
            while self.running:
                client_socket, address = server_socket.accept()
                logger.info(f"Android device connected from {address}")

                # Handle this client in a blocking way for simplicity
                # For production, use threading or async/await for concurrent handling
                self.handle_client(client_socket, address)

        except KeyboardInterrupt:
            logger.info("Server shutdown requested")
        finally:
            server_socket.close()
            self.running = False

    def handle_client(self, client_socket, address):
        """Handle messages from an Android client"""
        device_id = f"{address[0]}:{address[1]}"

        try:
            # Set timeout for recv
            client_socket.settimeout(60.0)

            while self.running:
                # Receive message
                data = client_socket.recv(4096)
                if not data:
                    logger.info(f"Client {device_id} disconnected")
                    break

                message = data.decode('utf-8').strip()
                logger.info(f"Received from {device_id}: {message}")

                # Parse message
                parsed = self.protocol.parse_android_message(message)
                if not parsed:
                    logger.warning(f"Failed to parse message: {message}")
                    continue

                # Handle different message types
                response = self.handle_message(device_id, parsed, client_socket)

                if response:
                    logger.info(f"Sending to {device_id}: {response}")
                    client_socket.send(response.encode('utf-8'))

        except socket.timeout:
            logger.warning(f"Client {device_id} timed out")
        except Exception as e:
            logger.error(f"Error handling client {device_id}: {e}")
        finally:
            client_socket.close()
            logger.info(f"Connection closed for {device_id}")

    def handle_message(self, device_id, message, socket):
        """
        Handle a parsed protocol message.
        
        This is where SYNC_INIT and other messages are processed.
        """
        msg_type = message.type
        params = message.parameters

        if msg_type == 'SYNC_INIT':
            # Android is requesting time synchronization
            logger.info(f"SYNC_INIT from {device_id} - initiating time sync")
            self.sync_handler.handle_sync_init(device_id, socket)
            return None  # Response already sent by handler

        elif msg_type == 'SYNC_RESPONSE':
            # Android is responding to our SYNC_REQUEST
            try:
                # Validate required parameters are present
                if 't_pc' not in params or 't_ph' not in params:
                    logger.error(f"SYNC_RESPONSE missing required parameters from {device_id}")
                    return f"ERROR cmd=SYNC_RESPONSE code=FAIL msg=\"Missing t_pc or t_ph parameter\"\n"

                t_pc = int(params['t_pc'])
                t_ph = int(params['t_ph'])

                # Validate timestamps are not zero
                if t_pc == 0 or t_ph == 0:
                    logger.error(f"SYNC_RESPONSE has invalid zero timestamp from {device_id}")
                    return f"ERROR cmd=SYNC_RESPONSE code=FAIL msg=\"Invalid zero timestamp\"\n"

                sync_result = self.sync_handler.handle_sync_response(
                    device_id, t_pc, t_ph, socket
                )

                if sync_result:
                    logger.info(
                        f"Sync completed: offset={sync_result['offset_ms']}ms, "
                        f"rtt={sync_result['rtt_ms']}ms"
                    )

                return None  # Response already sent by handler

            except ValueError as e:
                logger.error(f"Invalid timestamp format in SYNC_RESPONSE from {device_id}: {e}")
                return f"ERROR cmd=SYNC_RESPONSE code=FAIL msg=\"Invalid timestamp format\"\n"
            except Exception as e:
                logger.error(f"Error processing SYNC_RESPONSE: {e}")
                return None

        elif msg_type == 'HELLO':
            # Android is announcing itself
            device_name = params.get('device_name', 'unknown')
            sensors = params.get('sensors', '[]')
            logger.info(f"Device {device_name} available with sensors: {sensors}")
            return f"ACK cmd=HELLO device_id={device_id}\n"

        elif msg_type == 'START_RECORD':
            session_id = params.get('session_id', 'unknown')
            logger.info(f"Recording start requested for session {session_id}")
            return f"ACK cmd=START_RECORD session_id={session_id}\n"

        elif msg_type == 'STOP_RECORD':
            session_id = params.get('session_id', 'unknown')
            logger.info(f"Recording stop requested for session {session_id}")
            return f"ACK cmd=STOP_RECORD session_id={session_id}\n"

        elif msg_type == 'DATA_GSR':
            # GSR data streaming
            timestamp = params.get('ts', 0)
            value = params.get('value', 0)
            logger.debug(f"GSR data: ts={timestamp}, value={value}")
            return None  # No response needed for data

        else:
            logger.warning(f"Unknown message type: {msg_type}")
            return f"ERROR cmd={msg_type} code=FAIL msg=\"Unknown command\"\n"


def main():
    """Main entry point"""
    port = 8080

    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"Invalid port: {sys.argv[1]}")
            sys.exit(1)

    print("=" * 60)
    print("PC Server with Time Synchronization Support")
    print("=" * 60)
    print(f"Port: {port}")
    print()
    print("This server automatically handles SYNC_INIT messages from Android.")
    print()
    print("Protocol flow:")
    print("  1. Android sends: SYNC_INIT")
    print("  2. PC responds: SYNC_REQUEST t_pc=<T1>")
    print("  3. Android sends: SYNC_RESPONSE t_pc=<T1> t_ph=<T2>")
    print("  4. PC responds: SYNC_RESULT t1=<T1> t2=<T2> t3=<T3> offset=<X> rtt=<Y>")
    print()
    print("Press Ctrl+C to stop the server")
    print("=" * 60)
    print()

    server = PCServer(port)
    server.start()


if __name__ == "__main__":
    main()
