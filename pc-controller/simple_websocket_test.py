#!/usr/bin/env python3
"""
Simple WebSocket server test - Phase 1 implementation
Tests only the WebSocket server without heavy dependencies
"""

import asyncio
import json
import ssl
import uuid
from datetime import datetime
from pathlib import Path

import websockets


class SimpleWebSocketServer:
    """Minimal WebSocket server for testing Phase 1 functionality"""

    def __init__(self, host="0.0.0.0", port=8443):
        self.host = host
        self.port = port
        self.clients = {}
        self.is_running = False

    async def start(self):
        """Start the WebSocket server"""
        print(f"Starting WebSocket server on {self.host}:{self.port}")

        try:
            # Start WebSocket server (without TLS for testing)
            self.server = await websockets.serve(
                self.handle_client,
                self.host,
                self.port,
                ssl=None,  # No TLS for simple test
                ping_interval=None,
                ping_timeout=None,
            )

            self.is_running = True
            print(
                f"WebSocket server started successfully on ws://{self.host}:{self.port}"
            )

        except Exception as e:
            print(f"Failed to start WebSocket server: {e}")
            raise

    async def stop(self):
        """Stop the WebSocket server"""
        if hasattr(self, "server"):
            self.server.close()
            await self.server.wait_closed()
        self.is_running = False
        print("WebSocket server stopped")

    async def handle_client(self, websocket, path):
        """Handle new WebSocket client connection"""
        client_id = str(uuid.uuid4())
        client_address = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"

        print(f"New WebSocket connection from {client_address} (ID: {client_id})")

        try:
            self.clients[client_id] = {
                "websocket": websocket,
                "address": client_address,
                "connected_at": datetime.now().isoformat(),
            }

            # Send welcome message
            welcome_message = {
                "message_type": "welcome",
                "server_version": "Phase 1 Test",
                "client_id": client_id,
                "timestamp": datetime.now().isoformat(),
            }
            await websocket.send(json.dumps(welcome_message))

            # Handle incoming messages
            async for message in websocket:
                try:
                    await self.process_message(client_id, message)
                except Exception as e:
                    print(f"Error processing message from {client_id}: {e}")

        except websockets.exceptions.ConnectionClosed:
            print(f"Client {client_id} disconnected")
        except Exception as e:
            print(f"Error handling client {client_id}: {e}")
        finally:
            if client_id in self.clients:
                del self.clients[client_id]
            print(f"Client {client_id} cleaned up")

    async def process_message(self, client_id, raw_message):
        """Process incoming message from client"""
        try:
            message = json.loads(raw_message)
            message_type = message.get("message_type", "unknown")

            print(f"Received message from {client_id}: {message_type}")

            # Echo response
            response = {
                "message_type": f"{message_type}_response",
                "original_message": message,
                "server_timestamp": datetime.now().isoformat(),
                "connected_clients": len(self.clients),
            }

            client = self.clients.get(client_id)
            if client:
                await client["websocket"].send(json.dumps(response))

        except json.JSONDecodeError:
            print(f"Invalid JSON from client {client_id}")
        except Exception as e:
            print(f"Error processing message: {e}")

    def get_status(self):
        """Get server status"""
        return {
            "running": self.is_running,
            "connected_clients": len(self.clients),
            "clients": list(self.clients.keys()),
        }


async def main():
    """Test the simple WebSocket server"""
    print("=== Phase 1 WebSocket Server Test ===")

    try:
        # Create and start WebSocket server
        server = SimpleWebSocketServer(host="0.0.0.0", port=8443)

        await server.start()

        print("\n=== Server Status ===")
        print(json.dumps(server.get_status(), indent=2))

        print("\n=== Waiting for connections ===")
        print("You can test with: websocat ws://localhost:8443")
        print("Or use a WebSocket client to connect to ws://localhost:8443")
        print("Press Ctrl+C to stop...")

        # Keep server running
        try:
            while server.is_running:
                await asyncio.sleep(1)

                # Print status every 10 seconds if there are clients
                if len(server.clients) > 0:
                    print(f"Active clients: {len(server.clients)}")

        except KeyboardInterrupt:
            print("\n=== Shutting down server ===")

    except Exception as e:
        print(f"Error: {e}")
        import traceback

        traceback.print_exc()
    finally:
        if "server" in locals():
            await server.stop()
        print("=== Test completed ===")


if __name__ == "__main__":
    asyncio.run(main())
