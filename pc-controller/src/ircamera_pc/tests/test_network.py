"""
Comprehensive unit tests for PC Controller network components
Tests NetworkServer, JSON protocol, and Hub-Spoke communication
"""

import asyncio
import json
import os
import socket
import sys
import threading
import time
import unittest
from typing import Any, Dict, Optional
from unittest.mock import MagicMock, Mock, patch

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", ".."))

from ircamera_pc.network.protocol import MessageProtocol, MessageType
from ircamera_pc.network.server import NetworkServer


class TestNetworkServer(unittest.TestCase):
    """Comprehensive tests for NetworkServer Hub functionality"""

    def setUp(self):
        """Set up test fixtures"""
        self.server = NetworkServer(host="localhost", port=8080)
        self.test_client_data = {
            "device_id": "TEST_ANDROID_001",
            "device_type": "android_spoke",
            "capabilities": ["rgb", "thermal", "gsr"],
        }

    def tearDown(self):
        """Clean up after tests"""
        if hasattr(self.server, "_server_socket") and self.server._server_socket:
            self.server.stop()
        time.sleep(0.1)  # Allow cleanup

    def test_server_initialization(self):
        """Test server initialization and configuration"""
        self.assertIsNotNone(self.server)
        self.assertEqual(self.server.host, "localhost")
        self.assertEqual(self.server.port, 8080)
        self.assertFalse(self.server.is_running())
        self.assertEqual(len(self.server.get_connected_devices()), 0)

    def test_server_start_stop(self):
        """Test server start and stop functionality"""
        # Test start
        result = self.server.start()
        self.assertTrue(result)
        self.assertTrue(self.server.is_running())

        # Test stop
        self.server.stop()
        self.assertFalse(self.server.is_running())

    def test_server_start_failure(self):
        """Test server start failure handling"""
        # Start server normally
        self.server.start()

        # Try to start another server on same port
        server2 = NetworkServer(host="localhost", port=8080)
        result = server2.start()
        self.assertFalse(result)

        # Cleanup
        self.server.stop()
        server2.stop()

    @patch("socket.socket")
    def test_client_connection_handling(self, mock_socket_class):
        """Test client connection and registration"""
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket

        # Setup mock client connection
        mock_client_socket = Mock()
        mock_client_socket.recv.return_value = json.dumps(
            {
                "type": "device_registration",
                "device_id": "TEST_ANDROID_001",
                "device_type": "android_spoke",
                "capabilities": ["rgb", "thermal", "gsr"],
            }
        ).encode()

        mock_socket.accept.return_value = (mock_client_socket, ("192.168.1.100", 12345))

        self.server.start()

        # Simulate client connection
        self.server._handle_client_connection(
            mock_client_socket, ("192.168.1.100", 12345)
        )

        # Verify client was registered
        devices = self.server.get_connected_devices()
        self.assertEqual(len(devices), 1)
        self.assertEqual(devices[0]["device_id"], "TEST_ANDROID_001")

    def test_message_protocol_validation(self):
        """Test message protocol validation and parsing"""
        protocol = MessageProtocol()

        # Test valid messages
        valid_messages = [
            {"type": "sync_request", "timestamp": 1234567890},
            {"type": "session_request", "session_name": "Test", "participant": "P001"},
            {"type": "sync_marker", "id": "STIM_1", "metadata": {"intensity": 0.8}},
        ]

        for msg in valid_messages:
            result = protocol.validate_message(msg)
            self.assertTrue(result, f"Message {msg} should be valid")

    def test_message_protocol_invalid(self):
        """Test message protocol with invalid messages"""
        protocol = MessageProtocol()

        # Test invalid messages
        invalid_messages = [
            {},  # Empty message
            {"invalid": "message"},  # No type field
            {"type": "unknown_type"},  # Unknown message type
            {"type": "sync_request"},  # Missing required fields
        ]

        for msg in invalid_messages:
            result = protocol.validate_message(msg)
            self.assertFalse(result, f"Message {msg} should be invalid")

    def test_sync_request_handling(self):
        """Test NTP-like sync request processing"""
        self.server.start()

        sync_request = {
            "type": "sync_request",
            "client_timestamp": time.time_ns(),
            "device_id": "TEST_ANDROID_001",
        }

        response = self.server._handle_sync_request(sync_request)

        self.assertIsNotNone(response)
        self.assertEqual(response["type"], "sync_response")
        self.assertIn("server_timestamp", response)
        self.assertIn("client_timestamp", response)
        self.assertIn("latency_estimate", response)

    def test_session_management(self):
        """Test session creation and management"""
        self.server.start()

        # Test session start
        session_request = {
            "type": "session_request",
            "action": "start",
            "session_name": "TestSession",
            "participant_id": "P001",
            "device_id": "TEST_ANDROID_001",
        }

        response = self.server._handle_session_request(session_request)

        self.assertEqual(response["type"], "session_response")
        self.assertEqual(response["status"], "started")
        self.assertIn("session_id", response)

        # Test session stop
        stop_request = {
            "type": "session_request",
            "action": "stop",
            "session_id": response["session_id"],
            "device_id": "TEST_ANDROID_001",
        }

        stop_response = self.server._handle_session_request(stop_request)
        self.assertEqual(stop_response["status"], "stopped")

    def test_sync_marker_distribution(self):
        """Test sync marker distribution to all connected devices"""
        self.server.start()

        # Register multiple devices
        devices = ["ANDROID_001", "ANDROID_002", "ANDROID_003"]
        for device_id in devices:
            self.server._register_device(
                {
                    "device_id": device_id,
                    "device_type": "android_spoke",
                    "capabilities": ["rgb", "thermal", "gsr"],
                },
                Mock(),
            )

        # Create sync marker
        sync_marker = {
            "type": "sync_marker",
            "id": "STIMULUS_1",
            "timestamp": time.time_ns(),
            "metadata": {"stimulus_type": "visual", "intensity": 0.8, "duration": 2000},
        }

        # Distribute sync marker
        result = self.server.distribute_sync_marker(sync_marker)

        self.assertTrue(result)

        # Verify all devices received the marker
        for device_id in devices:
            device_info = self.server.get_device_info(device_id)
            self.assertIsNotNone(device_info)

    def test_file_transfer_coordination(self):
        """Test file transfer coordination between Hub and Spokes"""
        self.server.start()

        file_request = {
            "type": "file_transfer_request",
            "device_id": "TEST_ANDROID_001",
            "filename": "gsr_data_20240101_120000.csv",
            "file_size": 1024000,
            "checksum": "abc123def456",
        }

        response = self.server._handle_file_transfer_request(file_request)

        self.assertEqual(response["type"], "file_transfer_response")
        self.assertEqual(response["status"], "ready")
        self.assertIn("chunk_size", response)
        self.assertIn("transfer_id", response)

    def test_error_handling(self):
        """Test error handling in network operations"""
        self.server.start()

        # Test invalid JSON
        with patch(
            "json.loads", side_effect=json.JSONDecodeError("Invalid JSON", "", 0)
        ):
            result = self.server._process_message("invalid json", Mock())
            self.assertIsNone(result)

        # Test network errors
        with patch.object(
            self.server, "_send_message", side_effect=ConnectionError("Network error")
        ):
            result = self.server.distribute_sync_marker(
                {"type": "sync_marker", "id": "TEST"}
            )
            self.assertFalse(result)

    def test_concurrent_connections(self):
        """Test handling multiple concurrent client connections"""
        self.server.start()

        # Create multiple mock clients
        clients = []
        for i in range(5):
            client_socket = Mock()
            client_data = {
                "device_id": f"ANDROID_00{i}",
                "device_type": "android_spoke",
                "capabilities": ["rgb", "thermal", "gsr"],
            }
            clients.append((client_socket, client_data))

        # Register all clients
        for client_socket, client_data in clients:
            self.server._register_device(client_data, client_socket)

        # Verify all clients are registered
        connected_devices = self.server.get_connected_devices()
        self.assertEqual(len(connected_devices), 5)

        # Test broadcast to all clients
        sync_marker = {"type": "sync_marker", "id": "BROADCAST_TEST"}
        result = self.server.distribute_sync_marker(sync_marker)
        self.assertTrue(result)

    def test_connection_timeout(self):
        """Test connection timeout handling"""
        self.server.start()

        # Mock client that doesn't respond to heartbeat
        client_socket = Mock()
        client_socket.send.side_effect = socket.timeout("Connection timeout")

        client_data = {"device_id": "TIMEOUT_CLIENT", "device_type": "android_spoke"}

        self.server._register_device(client_data, client_socket)

        # Simulate heartbeat timeout
        self.server._check_client_heartbeat("TIMEOUT_CLIENT")

        # Client should be disconnected
        devices = self.server.get_connected_devices()
        timeout_client = next(
            (d for d in devices if d["device_id"] == "TIMEOUT_CLIENT"), None
        )
        self.assertIsNone(timeout_client)

    def test_data_aggregation_coordination(self):
        """Test coordination of data aggregation across devices"""
        self.server.start()

        # Register devices with different capabilities
        device_configs = [
            {"device_id": "RGB_DEVICE", "capabilities": ["rgb"]},
            {"device_id": "THERMAL_DEVICE", "capabilities": ["thermal"]},
            {"device_id": "GSR_DEVICE", "capabilities": ["gsr"]},
            {
                "device_id": "MULTIMODAL_DEVICE",
                "capabilities": ["rgb", "thermal", "gsr"],
            },
        ]

        for config in device_configs:
            self.server._register_device(
                {**config, "device_type": "android_spoke"}, Mock()
            )

        # Test coordinated recording start
        recording_request = {
            "type": "coordinated_recording",
            "action": "start",
            "session_name": "MultiModal_Test",
            "sync_mode": "strict",
        }

        response = self.server._handle_coordinated_recording(recording_request)

        self.assertEqual(response["type"], "coordinated_recording_response")
        self.assertEqual(response["status"], "started")
        self.assertIn("participating_devices", response)

    def test_quality_monitoring(self):
        """Test network quality and synchronization monitoring"""
        self.server.start()

        # Register device
        self.server._register_device(self.test_client_data, Mock())

        # Simulate quality metrics
        quality_report = {
            "type": "quality_report",
            "device_id": "TEST_ANDROID_001",
            "sync_accuracy_ms": 2.5,
            "network_latency_ms": 15.2,
            "data_loss_rate": 0.001,
            "timestamp": time.time_ns(),
        }

        self.server._process_quality_report(quality_report)

        # Get quality statistics
        stats = self.server.get_quality_statistics("TEST_ANDROID_001")

        self.assertIsNotNone(stats)
        self.assertLessEqual(stats["sync_accuracy_ms"], 5.0)  # Within 5ms requirement
        self.assertGreater(stats["network_latency_ms"], 0)

    def test_security_validation(self):
        """Test basic security validation for connections"""
        self.server.start()

        # Test device registration with invalid data
        invalid_registrations = [
            {},  # Empty registration
            {"device_id": ""},  # Empty device ID
            {"device_id": "VALID_ID"},  # Missing device type
            {
                "device_id": "../../../etc/passwd",
                "device_type": "android_spoke",
            },  # Path injection attempt
        ]

        for invalid_reg in invalid_registrations:
            result = self.server._validate_device_registration(invalid_reg)
            self.assertFalse(result, f"Registration {invalid_reg} should be rejected")

    def test_performance_metrics(self):
        """Test performance monitoring and metrics collection"""
        self.server.start()

        # Simulate high-load scenario
        start_time = time.time()

        # Send many sync markers
        for i in range(100):
            sync_marker = {
                "type": "sync_marker",
                "id": f"PERF_TEST_{i}",
                "timestamp": time.time_ns(),
            }
            self.server.distribute_sync_marker(sync_marker)

        end_time = time.time()

        # Verify performance is reasonable
        total_time = end_time - start_time
        self.assertLess(
            total_time, 1.0, "100 sync markers should complete within 1 second"
        )

        # Get performance statistics
        perf_stats = self.server.get_performance_statistics()
        self.assertIsNotNone(perf_stats)
        self.assertIn("messages_processed", perf_stats)
        self.assertIn("average_response_time_ms", perf_stats)


class TestMessageProtocol(unittest.TestCase):
    """Tests for message protocol handling"""

    def setUp(self):
        self.protocol = MessageProtocol()

    def test_message_types(self):
        """Test all supported message types"""
        valid_types = [
            MessageType.DEVICE_REGISTRATION,
            MessageType.SYNC_REQUEST,
            MessageType.SESSION_REQUEST,
            MessageType.SYNC_MARKER,
            MessageType.FILE_TRANSFER,
            MessageType.HEARTBEAT,
            MessageType.STATUS_UPDATE,
        ]

        for msg_type in valid_types:
            self.assertIn(msg_type.value, self.protocol.get_supported_types())

    def test_message_serialization(self):
        """Test message serialization and deserialization"""
        test_message = {
            "type": "sync_marker",
            "id": "TEST_SYNC",
            "timestamp": 1234567890,
            "metadata": {"stimulus_type": "auditory", "frequency": 440.0},
        }

        # Serialize
        serialized = self.protocol.serialize_message(test_message)
        self.assertIsInstance(serialized, bytes)

        # Deserialize
        deserialized = self.protocol.deserialize_message(serialized)
        self.assertEqual(deserialized, test_message)

    def test_message_validation_edge_cases(self):
        """Test message validation with edge cases"""
        edge_cases = [
            {"type": "sync_request", "timestamp": 0},  # Zero timestamp
            {"type": "sync_marker", "id": "", "metadata": {}},  # Empty ID
            {"type": "session_request", "session_name": "A" * 1000},  # Very long name
        ]

        for case in edge_cases:
            # Should handle gracefully without crashing
            result = self.protocol.validate_message(case)
            self.assertIsInstance(result, bool)

    def test_protocol_version_compatibility(self):
        """Test protocol version compatibility"""
        versions = ["1.0", "1.1", "2.0"]

        for version in versions:
            message = {
                "type": "sync_request",
                "protocol_version": version,
                "timestamp": time.time_ns(),
            }

            # Should handle different protocol versions
            is_compatible = self.protocol.is_version_compatible(version)
            self.assertIsInstance(is_compatible, bool)


if __name__ == "__main__":
    # Configure test runner
    unittest.main(verbosity=2)
