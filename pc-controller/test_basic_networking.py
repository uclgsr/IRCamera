#!/usr/bin/env python3
"""
Basic Test for Enhanced Networking Core Features

Tests just the core enhanced networking components without dependencies.
"""

import asyncio
import sys
import tempfile
import time
import uuid
from pathlib import Path

import pytest

# Add the src directory to Python path for imports
sys.path.insert(0, str(Path(__file__).parent / "src"))


# Simple logger fallback
class SimpleLogger:
    def info(self, msg):
        print(f"INFO: {msg}")

    def debug(self, msg):
        print(f"DEBUG: {msg}")

    def warning(self, msg):
        print(f"WARNING: {msg}")

    def error(self, msg):
        print(f"ERROR: {msg}")


logger = SimpleLogger()


# Mock config for testing
class MockConfig:
    def get(self, key, default=None):
        config_map = {
            "security.cert_directory": "certificates",
            "messaging.base_retry_delay": 1.0,
            "messaging.max_retry_delay": 30.0,
            "messaging.default_timeout": 30.0,
            "messaging.cleanup_interval": 60.0,
            "network.discovery_port": 8081,
            "version": "1.0.0",
        }
        return config_map.get(key, default)


config = MockConfig()

# Import our modules with mocked dependencies
sys.modules["ircamera_pc.core.config"] = type("module", (), {"config": config})()
sys.modules["ircamera_pc.utils.simple_logger"] = type(
    "module", (), {"logger": logger}
)()

try:
    from ircamera_pc.network.messaging import MessagePriority, ReliableMessageService
    from ircamera_pc.network.security import SecurityManager

    security_available = True
    messaging_available = True
except ImportError as e:
    logger.error(f"Import error: {e}")
    security_available = False
    messaging_available = False


import pytest


@pytest.mark.asyncio
async def test_security_manager():
    """Test SecurityManager basic functionality."""
    if not security_available:
        logger.error("SecurityManager not available")
        return False

    logger.info("Testing SecurityManager...")

    try:
        with tempfile.TemporaryDirectory() as temp_dir:
            # Mock the SecurityManager to avoid config dependencies
            security_manager = SecurityManager()
            security_manager.cert_dir = Path(temp_dir)
            security_manager.ca_cert_path = security_manager.cert_dir / "ca_cert.pem"
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
            result = security_manager.initialize()
            logger.info(f"✓ SecurityManager initialization: {result}")

            if result:
                # Test auth token generation
                device_id = "test_device_123"
                token = security_manager.generate_auth_token(device_id)
                logger.info(f"✓ Generated auth token: {token[:20]}...")

                # Test token validation
                is_valid, validated_device_id = security_manager.validate_auth_token(
                    token
                )
                logger.info(
                    f"✓ Token validation: {is_valid}, Device: {validated_device_id}"
                )

                # Test SSL context
                ssl_context = security_manager.create_ssl_context()
                logger.info(f"✓ SSL context created: {ssl_context.protocol}")

                return True
            else:
                logger.error("SecurityManager initialization failed")
                return False

    except Exception as e:
        logger.error(f"SecurityManager test failed: {e}")
        return False


@pytest.mark.asyncio
async def test_reliable_messaging():
    """Test ReliableMessageService basic functionality."""
    if not messaging_available:
        logger.error("ReliableMessageService not available")
        return False

    logger.info("Testing ReliableMessageService...")

    try:
        messaging_service = ReliableMessageService()

        # Set up mock transport
        sent_messages = []

        async def mock_transport(host, port, message):
            sent_messages.append((host, port, message))
            return True

        messaging_service.set_transport(mock_transport)
        logger.info("✓ Transport configured")

        # Test initialization
        result = await messaging_service.initialize()
        logger.info(f"✓ Messaging service initialization: {result}")

        if result:
            # Test message handler registration
            def test_handler(message):
                return {"status": "handled"}

            messaging_service.register_message_handler("test_message", test_handler)
            logger.info("✓ Message handler registered")

            # Test message sending
            message_id = await messaging_service.send_message(
                target_host="192.168.1.100",
                target_port=8080,
                message_type="test_message",
                content={"test_data": "hello"},
                priority=MessagePriority.HIGH,
            )
            logger.info(f"✓ Message sent: {message_id}")

            # Wait for processing
            await asyncio.sleep(1)

            # Check if message was processed
            logger.info(f"✓ Messages sent by transport: {len(sent_messages)}")

            # Test acknowledgment
            await messaging_service.handle_acknowledgment(message_id, True)
            logger.info("✓ Acknowledgment handled")

            # Test shutdown
            await messaging_service.shutdown()
            logger.info("✓ Service shutdown completed")

            return True
        else:
            logger.error("ReliableMessageService initialization failed")
            return False

    except Exception as e:
        logger.error(f"ReliableMessageService test failed: {e}")
        return False


async def main():
    """Run basic tests."""
    logger.info("=== Basic Enhanced Networking Tests ===")

    tests_passed = 0
    total_tests = 2

    # Test SecurityManager
    if await test_security_manager():
        tests_passed += 1
        logger.info("✅ SecurityManager tests passed")
    else:
        logger.error("❌ SecurityManager tests failed")

    # Test ReliableMessageService
    if await test_reliable_messaging():
        tests_passed += 1
        logger.info("✅ ReliableMessageService tests passed")
    else:
        logger.error("❌ ReliableMessageService tests failed")

    # Report results
    logger.info("\n=== Test Results ===")
    logger.info(f"Passed: {tests_passed}/{total_tests}")

    if tests_passed == total_tests:
        logger.info("🎉 All basic tests passed!")
        return 0
    else:
        logger.error(f"⚠️ {total_tests - tests_passed} test(s) failed")
        return 1


if __name__ == "__main__":
    try:
        exit_code = asyncio.run(main())
        sys.exit(exit_code)
    except KeyboardInterrupt:
        logger.info("Tests interrupted by user")
        sys.exit(0)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)
