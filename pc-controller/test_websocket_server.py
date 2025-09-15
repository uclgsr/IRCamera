#!/usr/bin/env python3
"""
Test script for WebSocket server - Phase 1 implementation
"""

import asyncio
import os
import sys
from pathlib import Path

# Add src directory to Python path
src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))

from ircamera_pc.network.websocket_server import WebSocketServer


async def main():
    """Test the WebSocket server"""
    print("Starting WebSocket server test...")

    try:
        # Create and start WebSocket server
        server = WebSocketServer(host="0.0.0.0", port=8443)

        print("Starting WebSocket server on port 8443...")
        await server.start()

        print("WebSocket server started successfully!")
        print("Waiting for connections... (Press Ctrl+C to stop)")

        # Keep server running
        try:
            while server.is_running:
                await asyncio.sleep(1)
        except KeyboardInterrupt:
            print("\nShutting down server...")

    except Exception as e:
        print(f"Error: {e}")
        import traceback

        traceback.print_exc()
    finally:
        if "server" in locals():
            await server.stop()
        print("WebSocket server stopped.")


if __name__ == "__main__":
    asyncio.run(main())
