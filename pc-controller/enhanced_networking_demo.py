#!/usr/bin/env python3
"""
Enhanced Networking Example for IRCamera PC Controller

Demonstrates the complete enhanced networking implementation including:
- TLS/SSL security with certificate management
- mDNS/Zeroconf device discovery
- NTP-like time synchronization
- Reliable messaging with acknowledgments
- Integration with the existing PC controller infrastructure
"""

import asyncio
import sys
from pathlib import Path

# Add the src directory to Python path for imports
sys.path.insert(0, str(Path(__file__).parent / "src"))

from ircamera_pc.network.server import NetworkServer
from ircamera_pc.network.security import SecurityManager
from ircamera_pc.network.discovery import NetworkDiscoveryService, DeviceType
from ircamera_pc.network.messaging import ReliableMessageService, MessagePriority, MessageCallback
from ircamera_pc.core.timesync import TimeSyncService

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


class EnhancedNetworkingDemo:
    """Demonstrates enhanced networking features of the PC Controller."""
    
    def __init__(self):
        """Initialize the demo."""
        self.server = NetworkServer()
        self.security_manager = SecurityManager()
        self.discovery_service = NetworkDiscoveryService()
        self.messaging_service = ReliableMessageService()
        self.time_sync_service = TimeSyncService()
        
        self.is_running = False
        
    async def start_demo(self):
        """Start the enhanced networking demonstration."""
        try:
            logger.info("=== Enhanced PC Controller Networking Demo ===")
            
            # Step 1: Initialize security
            logger.info("1. Initializing security manager...")
            if not self.security_manager.initialize():
                logger.error("Failed to initialize security manager")
                return False
            logger.info("✓ Security manager initialized with TLS certificates")
            
            # Step 2: Start network server with enhanced features
            logger.info("2. Starting enhanced network server...")
            if not await self.server.start():
                logger.error("Failed to start network server")
                return False
            logger.info("✓ Network server started with TLS and discovery")
            
            # Step 3: Start discovery service
            logger.info("3. Starting device discovery...")
            if await self.discovery_service.start_discovery():
                logger.info("✓ mDNS/Zeroconf discovery service started")
            else:
                logger.warning("Discovery service failed - continuing without discovery")
            
            # Step 4: Initialize reliable messaging
            logger.info("4. Initializing reliable messaging...")
            self.messaging_service.set_transport(self._message_transport)
            if await self.messaging_service.initialize():
                logger.info("✓ Reliable messaging service initialized")
            else:
                logger.error("Failed to initialize messaging service")
                return False
            
            # Step 5: Wait for device connections and demonstrate features
            logger.info("5. Waiting for device connections...")
            await self._demonstrate_features()
            
            self.is_running = True
            return True
            
        except Exception as e:
            logger.error(f"Demo initialization failed: {e}")
            await self.stop_demo()
            return False
    
    async def stop_demo(self):
        """Stop the demonstration."""
        if not self.is_running:
            return
            
        logger.info("Stopping enhanced networking demo...")
        self.is_running = False
        
        # Stop all services
        await self.messaging_service.shutdown()
        await self.discovery_service.stop_discovery()
        await self.server.stop()
        
        logger.info("Demo stopped")
    
    async def _demonstrate_features(self):
        """Demonstrate the enhanced networking features."""
        demo_duration = 60  # Run demo for 60 seconds
        start_time = asyncio.get_event_loop().time()
        
        logger.info(f"Running networking demonstration for {demo_duration} seconds...")
        logger.info("Connect Android devices to see enhanced features in action")
        
        while asyncio.get_event_loop().time() - start_time < demo_duration:
            try:
                # Check for discovered devices
                discovered_devices = await self.discovery_service.get_discovered_devices()
                if discovered_devices:
                    logger.info(f"Discovered {len(discovered_devices)} devices:")
                    for device in discovered_devices:
                        logger.info(f"  - {device.service_name} ({device.device_type.value}) at {device.ip_address}:{device.port}")
                
                # Check connected devices
                connected_devices = self.server.get_connected_devices()
                if connected_devices:
                    logger.info(f"Connected devices: {len(connected_devices)}")
                    
                    # Demonstrate reliable messaging
                    for device in connected_devices:
                        try:
                            message_id = await self.server.send_reliable_message_to_device(
                                device.device_id,
                                "heartbeat_check",
                                {"timestamp": asyncio.get_event_loop().time()},
                                MessagePriority.NORMAL
                            )
                            logger.debug(f"Sent reliable heartbeat to {device.device_id}: {message_id}")
                        except Exception as e:
                            logger.warning(f"Failed to send reliable message to {device.device_id}: {e}")
                
                # Check messaging service health
                pending_count = self.messaging_service.get_pending_message_count()
                if pending_count > 0:
                    logger.debug(f"Pending messages: {pending_count}")
                
                # Clean up expired tokens
                self.security_manager.cleanup_expired_tokens()
                
                await asyncio.sleep(5)  # Check every 5 seconds
                
            except Exception as e:
                logger.error(f"Error in demo loop: {e}")
                await asyncio.sleep(1)
        
        logger.info("Demonstration completed")
    
    async def _message_transport(self, host: str, port: int, message: dict) -> bool:
        """Transport function for reliable messaging."""
        try:
            # This would normally send the message via the actual network connection
            # For demo purposes, we'll just log it
            logger.debug(f"Transport: Sending message to {host}:{port} - {message.get('message_type')}")
            
            # Simulate successful delivery
            return True
            
        except Exception as e:
            logger.error(f"Transport error to {host}:{port}: {e}")
            return False
    
    def demonstrate_security_features(self):
        """Demonstrate security features."""
        logger.info("=== Security Features Demo ===")
        
        # Generate auth token
        device_id = "demo_device_123"
        token = self.security_manager.generate_auth_token(device_id)
        logger.info(f"Generated auth token for {device_id}: {token[:20]}...")
        
        # Validate token
        is_valid, validated_device_id = self.security_manager.validate_auth_token(token)
        logger.info(f"Token validation: {is_valid}, Device ID: {validated_device_id}")
        
        # SSL context creation
        ssl_context = self.security_manager.create_ssl_context()
        logger.info(f"SSL context created: {ssl_context.protocol}")
    
    async def demonstrate_discovery_features(self):
        """Demonstrate discovery features."""
        logger.info("=== Discovery Features Demo ===")
        
        # Get discovered devices by type
        thermal_cameras = await self.discovery_service.get_devices_by_type(DeviceType.THERMAL_CAMERA_TS004)
        android_nodes = await self.discovery_service.get_devices_by_type(DeviceType.ANDROID_SENSOR_NODE)
        
        logger.info(f"Thermal cameras: {len(thermal_cameras)}")
        logger.info(f"Android sensor nodes: {len(android_nodes)}")
        
        for camera in thermal_cameras:
            logger.info(f"  Thermal: {camera.service_name} at {camera.ip_address}:{camera.port}")
        
        for node in android_nodes:
            logger.info(f"  Android: {node.service_name} at {node.ip_address}:{node.port}")


async def main():
    """Main demonstration function."""
    demo = EnhancedNetworkingDemo()
    
    try:
        # Show security features
        demo.demonstrate_security_features()
        
        # Start the networking demo
        if await demo.start_demo():
            logger.info("Enhanced networking demo is running...")
            logger.info("Press Ctrl+C to stop")
            
            # Show discovery features
            await demo.demonstrate_discovery_features()
            
            # Keep running until interrupted
            try:
                while demo.is_running:
                    await asyncio.sleep(1)
            except KeyboardInterrupt:
                logger.info("Received interrupt signal")
        else:
            logger.error("Failed to start demo")
            return 1
            
    except Exception as e:
        logger.error(f"Demo error: {e}")
        return 1
    finally:
        await demo.stop_demo()
    
    return 0


if __name__ == "__main__":
    # Run the demonstration
    try:
        exit_code = asyncio.run(main())
        sys.exit(exit_code)
    except KeyboardInterrupt:
        logger.info("Demo interrupted by user")
        sys.exit(0)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)