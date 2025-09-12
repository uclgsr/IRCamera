#!/usr/bin/env python3
"""
Comprehensive Test Suite for Enhanced Networking Features

Tests all the enhanced networking components:
- Security Manager (TLS/SSL, certificates, authentication)
- Discovery Service (mDNS/Zeroconf)
- Reliable Messaging (ACK/NACK, retry logic)
- Network Server integration
"""

import asyncio
import sys
import tempfile
import time
import uuid
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

# Add the src directory to Python path for imports
sys.path.insert(0, str(Path(__file__).parent / "src"))

from ircamera_pc.network.discovery import DeviceType, NetworkDiscoveryService
from ircamera_pc.network.messaging import (
    MessageCallback,
    MessagePriority,
    ReliableMessageService,
)
from ircamera_pc.network.security import SecurityManager
from ircamera_pc.network.server import NetworkServer

try:
    from loguru import logger
except ImportError:
    import logging

    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


class EnhancedNetworkingTests:
    """Comprehensive test suite for enhanced networking features."""

    def __init__(self):
        """Initialize the test suite."""
        self.temp_dir = None
        self.test_results = {}

    async def run_all_tests(self):
        """Run all enhanced networking tests."""
        logger.info("=== Enhanced Networking Test Suite ===")

        # Create temporary directory for test certificates
        with tempfile.TemporaryDirectory() as temp_dir:
            self.temp_dir = temp_dir

            # Run test categories
            await self.test_security_manager()
            await self.test_discovery_service()
            await self.test_reliable_messaging()
            await self.test_network_server_integration()

            # Report results
            self.report_results()

        return all(self.test_results.values())

    async def test_security_manager(self):
        """Test SecurityManager functionality."""
        logger.info("Testing Security Manager...")

        try:
            # Initialize security manager with test directory
            with patch.object(SecurityManager, "__init__", self._mock_security_init):
                security_manager = SecurityManager()
                security_manager.cert_dir = Path(self.temp_dir)
                security_manager.ca_cert_path = (
                    security_manager.cert_dir / "ca_cert.pem"
                )
                security_manager.ca_key_path = security_manager.cert_dir / "ca_key.pem"
                security_manager.server_cert_path = (
                    security_manager.cert_dir / "server_cert.pem"
                )
                security_manager.server_key_path = (
                    security_manager.cert_dir / "server_key.pem"
                )
                security_manager.device_certificates = {}
                security_manager.auth_tokens = {}

            # Test initialization
            assert (
                security_manager.initialize()
            ), "Security manager initialization failed"
            logger.info("✓ Security manager initialization")

            # Test certificate generation
            assert (
                security_manager.ca_cert_path.exists()
            ), "CA certificate not generated"
            assert (
                security_manager.server_cert_path.exists()
            ), "Server certificate not generated"
            logger.info("✓ Certificate generation")

            # Test SSL context creation
            ssl_context = security_manager.create_ssl_context()
            assert ssl_context is not None, "SSL context creation failed"
            logger.info("✓ SSL context creation")

            # Test auth token generation and validation
            device_id = "test_device_123"
            token = security_manager.generate_auth_token(device_id)
            assert token is not None, "Auth token generation failed"

            is_valid, validated_device_id = security_manager.validate_auth_token(token)
            assert is_valid, "Auth token validation failed"
            assert validated_device_id == device_id, "Device ID mismatch in token"
            logger.info("✓ Auth token generation and validation")

            # Test token expiry
            expired_token = security_manager.generate_auth_token(
                device_id, duration_minutes=-1
            )
            is_valid, _ = security_manager.validate_auth_token(expired_token)
            assert not is_valid, "Expired token should be invalid"
            logger.info("✓ Token expiry handling")

            # Test token cleanup
            security_manager.cleanup_expired_tokens()
            logger.info("✓ Token cleanup")

            self.test_results["security_manager"] = True
            logger.info("✅ Security Manager tests passed")

        except Exception as e:
            logger.error(f"❌ Security Manager tests failed: {e}")
            self.test_results["security_manager"] = False

    async def test_discovery_service(self):
        """Test NetworkDiscoveryService functionality."""
        logger.info("Testing Discovery Service...")

        try:
            discovery_service = NetworkDiscoveryService()

            # Test initialization
            assert (
                not discovery_service.is_running
            ), "Discovery service should not be running initially"
            logger.info("✓ Initial state")

            # Test device type determination
            device_type = discovery_service._determine_device_type(
                "_topdon-thermal._tcp.local.", {b"device_type": b"THERMAL_CAMERA_TS004"}
            )
            assert (
                device_type == DeviceType.THERMAL_CAMERA_TS004
            ), "Device type determination failed"
            logger.info("✓ Device type determination")

            # Test local IP detection
            local_ip = discovery_service._get_local_ip()
            assert local_ip is not None, "Local IP detection failed"
            assert local_ip != "0.0.0.0", "Invalid local IP"
            logger.info(f"✓ Local IP detection: {local_ip}")

            # Test discovery listener management
            test_callback = lambda event, device: None
            discovery_service.add_discovery_listener(test_callback)
            assert (
                test_callback in discovery_service.discovery_listeners
            ), "Listener not added"

            discovery_service.remove_discovery_listener(test_callback)
            assert (
                test_callback not in discovery_service.discovery_listeners
            ), "Listener not removed"
            logger.info("✓ Discovery listener management")

            # Test fallback discovery (since zeroconf might not be available)
            if not discovery_service._check_zeroconf_available():
                result = await discovery_service._start_fallback_discovery()
                assert result, "Fallback discovery failed"
                await discovery_service.stop_discovery()
                logger.info("✓ Fallback discovery")
            else:
                logger.info("✓ Zeroconf available for full testing")

            self.test_results["discovery_service"] = True
            logger.info("✅ Discovery Service tests passed")

        except Exception as e:
            logger.error(f"❌ Discovery Service tests failed: {e}")
            self.test_results["discovery_service"] = False

    async def test_reliable_messaging(self):
        """Test ReliableMessageService functionality."""
        logger.info("Testing Reliable Messaging...")

        try:
            messaging_service = ReliableMessageService()

            # Test initialization
            assert (
                not messaging_service.is_running
            ), "Messaging service should not be running initially"

            # Set up mock transport
            sent_messages = []

            async def mock_transport(host, port, message):
                sent_messages.append((host, port, message))
                return True

            messaging_service.set_transport(mock_transport)
            assert messaging_service.transport is not None, "Transport not set"
            logger.info("✓ Transport configuration")

            # Test service initialization
            assert (
                await messaging_service.initialize()
            ), "Messaging service initialization failed"
            assert messaging_service.is_running, "Messaging service should be running"
            logger.info("✓ Service initialization")

            # Test message handler registration
            response_messages = []

            def test_handler(message):
                response_messages.append(message)
                return {"status": "handled", "message_id": message.get("test_id")}

            messaging_service.register_message_handler("test_message", test_handler)
            assert (
                "test_message" in messaging_service.message_handlers
            ), "Handler not registered"
            logger.info("✓ Message handler registration")

            # Test message sending
            message_id = await messaging_service.send_message(
                target_host="192.168.1.100",
                target_port=8080,
                message_type="test_message",
                content={"test_data": "hello"},
                priority=MessagePriority.HIGH,
            )
            assert message_id is not None, "Message ID not returned"
            assert len(messaging_service.pending_messages) > 0, "Message not queued"
            logger.info("✓ Message sending")

            # Wait for message processing
            await asyncio.sleep(1)
            assert len(sent_messages) > 0, "Message not sent by transport"
            logger.info("✓ Message processing")

            # Test acknowledgment handling
            await messaging_service.handle_acknowledgment(message_id, True)
            assert (
                message_id not in messaging_service.pending_messages
            ), "Message not removed after ACK"
            logger.info("✓ Acknowledgment handling")

            # Test incoming message handling
            test_message = {
                "message_id": str(uuid.uuid4()),
                "message_type": "test_message",
                "test_id": "test_123",
            }
            response = await messaging_service.handle_incoming_message(test_message)
            assert len(response_messages) > 0, "Incoming message not handled"
            logger.info("✓ Incoming message handling")

            # Test queue sizes
            queue_sizes = messaging_service.get_queue_sizes()
            assert isinstance(queue_sizes, dict), "Queue sizes not returned as dict"
            logger.info("✓ Queue size monitoring")

            # Test service shutdown
            await messaging_service.shutdown()
            assert (
                not messaging_service.is_running
            ), "Service should not be running after shutdown"
            logger.info("✓ Service shutdown")

            self.test_results["reliable_messaging"] = True
            logger.info("✅ Reliable Messaging tests passed")

        except Exception as e:
            logger.error(f"❌ Reliable Messaging tests failed: {e}")
            self.test_results["reliable_messaging"] = False

    async def test_network_server_integration(self):
        """Test NetworkServer integration with enhanced features."""
        logger.info("Testing Network Server Integration...")

        try:
            # Create server with mocked enhanced services
            with (
                patch("ircamera_pc.network.server.SecurityManager") as MockSecurity,
                patch(
                    "ircamera_pc.network.server.NetworkDiscoveryService"
                ) as MockDiscovery,
                patch(
                    "ircamera_pc.network.server.ReliableMessageService"
                ) as MockMessaging,
            ):

                # Configure mocks
                mock_security = MockSecurity.return_value
                mock_security.initialize.return_value = True
                mock_security.create_ssl_context.return_value = MagicMock()

                mock_discovery = MockDiscovery.return_value
                mock_discovery.start_discovery = AsyncMock(return_value=True)
                mock_discovery.stop_discovery = AsyncMock()

                mock_messaging = MockMessaging.return_value
                mock_messaging.initialize = AsyncMock(return_value=True)
                mock_messaging.shutdown = AsyncMock()

                # Create and test server
                server = NetworkServer()

                # Test enhanced service setup
                assert hasattr(
                    server, "_security_manager"
                ), "Security manager not initialized"
                assert hasattr(
                    server, "_discovery_service"
                ), "Discovery service not initialized"
                assert hasattr(
                    server, "_messaging_service"
                ), "Messaging service not initialized"
                logger.info("✓ Enhanced services initialization")

                # Test server configuration
                assert (
                    server._secure_port == server._port + 1
                ), "Secure port not configured correctly"
                logger.info("✓ Server configuration")

                # Test message handlers setup
                expected_handlers = ["device_auth", "message_ack", "message_nack"]
                for handler in expected_handlers:
                    assert (
                        handler in server._message_handlers
                    ), f"Handler {handler} not registered"
                logger.info("✓ Enhanced message handlers")

                # Test service integration methods
                assert hasattr(
                    server, "_handle_secure_client"
                ), "Secure client handler missing"
                assert hasattr(
                    server, "_send_message_to_device"
                ), "Message transport method missing"
                assert hasattr(
                    server, "_on_device_discovered"
                ), "Discovery callback missing"
                logger.info("✓ Integration methods")

                # Test reliable messaging integration
                assert hasattr(
                    server, "send_reliable_message_to_device"
                ), "Reliable messaging method missing"
                logger.info("✓ Reliable messaging integration")

            self.test_results["network_server_integration"] = True
            logger.info("✅ Network Server Integration tests passed")

        except Exception as e:
            logger.error(f"❌ Network Server Integration tests failed: {e}")
            self.test_results["network_server_integration"] = False

    def _mock_security_init(self, *args, **kwargs):
        """Mock SecurityManager __init__ to avoid config dependencies."""
        pass

    def report_results(self):
        """Report test results summary."""
        logger.info("\n=== Test Results Summary ===")

        total_tests = len(self.test_results)
        passed_tests = sum(self.test_results.values())
        failed_tests = total_tests - passed_tests

        for test_name, passed in self.test_results.items():
            status = "✅ PASS" if passed else "❌ FAIL"
            logger.info(f"{test_name}: {status}")

        logger.info(
            f"\nTotal: {total_tests}, Passed: {passed_tests}, Failed: {failed_tests}"
        )

        if passed_tests == total_tests:
            logger.info("🎉 All tests passed!")
        else:
            logger.warning(f"⚠️  {failed_tests} test(s) failed")


async def main():
    """Main test runner."""
    logger.info("Starting Enhanced Networking Test Suite...")

    test_suite = EnhancedNetworkingTests()

    try:
        success = await test_suite.run_all_tests()

        if success:
            logger.info("✅ All enhanced networking tests completed successfully")
            return 0
        else:
            logger.error("❌ Some tests failed")
            return 1

    except Exception as e:
        logger.error(f"Test suite error: {e}")
        return 1


if __name__ == "__main__":
    # Run the test suite
    try:
        exit_code = asyncio.run(main())
        sys.exit(exit_code)
    except KeyboardInterrupt:
        logger.info("Test suite interrupted by user")
        sys.exit(0)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)
