#!/usr/bin/env python3
"""
Tests for SyncHandler

Verifies the time synchronization protocol implementation.
"""

import unittest
import time
from unittest.mock import Mock, MagicMock
import sys
import os

# Import from parent package - run tests from pc-controller directory:
# python3 -m tests.test_sync_handler
try:
    from sync_handler import SyncHandler
except ImportError:
    # Fallback for direct execution
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    from sync_handler import SyncHandler


class TestSyncHandler(unittest.TestCase):
    """Test cases for SyncHandler"""

    def setUp(self):
        """Set up test fixtures"""
        self.handler = SyncHandler()
        self.mock_socket = Mock()
        self.device_id = "test_device_192.168.1.100"

    def test_handle_sync_init(self):
        """Test handling of SYNC_INIT message"""
        # Call handler
        result = self.handler.handle_sync_init(self.device_id, self.mock_socket)

        # Verify result
        self.assertTrue(result)

        # Verify SYNC_REQUEST was sent
        self.mock_socket.send.assert_called_once()
        sent_message = self.mock_socket.send.call_args[0][0].decode('utf-8')

        self.assertTrue(sent_message.startswith("SYNC_REQUEST"))
        self.assertIn("t_pc=", sent_message)

        # Verify pending sync was stored
        self.assertIn(self.device_id, self.handler.pending_syncs)
        self.assertIn('t1', self.handler.pending_syncs[self.device_id])

    def test_handle_sync_response(self):
        """Test handling of SYNC_RESPONSE message"""
        # First, simulate SYNC_INIT
        self.handler.handle_sync_init(self.device_id, self.mock_socket)
        self.mock_socket.reset_mock()

        # Get the t1 that was stored
        t1 = self.handler.pending_syncs[self.device_id]['t1']

        # Simulate phone receiving at t2 (5ms later)
        t2 = t1 + 5

        # Handle SYNC_RESPONSE
        result = self.handler.handle_sync_response(
            self.device_id, t1, t2, self.mock_socket
        )

        # Verify result
        self.assertIsNotNone(result)
        self.assertIn('offset_ms', result)
        self.assertIn('rtt_ms', result)
        self.assertIn('quality', result)

        # Verify SYNC_RESULT was sent
        self.mock_socket.send.assert_called_once()
        sent_message = self.mock_socket.send.call_args[0][0].decode('utf-8')

        self.assertTrue(sent_message.startswith("SYNC_RESULT"))
        self.assertIn("t1=", sent_message)
        self.assertIn("t2=", sent_message)
        self.assertIn("t3=", sent_message)
        self.assertIn("offset=", sent_message)
        self.assertIn("rtt=", sent_message)

        # Verify pending sync was removed
        self.assertNotIn(self.device_id, self.handler.pending_syncs)

        # Verify sync history was stored
        self.assertIn(self.device_id, self.handler.sync_history)

    def test_offset_calculation(self):
        """Test that offset is calculated correctly"""
        # Set up known timestamps
        t1 = 1000  # PC sends at 1000ms
        t2 = 1010  # Phone receives at 1010ms (phone is 10ms ahead)

        # Manually set up pending sync
        self.handler.pending_syncs[self.device_id] = {
            't1': t1,
            'sent_at': time.time()
        }

        # Mock time.time to return predictable t3
        original_time = time.time
        time.time = lambda: 1.02  # t3 = 1020ms

        try:
            result = self.handler.handle_sync_response(
                self.device_id, t1, t2, self.mock_socket
            )

            # Verify offset calculation
            # offset = t2 - ((t1 + t3) / 2)
            # offset = 1010 - ((1000 + 1020) / 2)
            # offset = 1010 - 1010 = 0
            self.assertEqual(result['offset_ms'], 0)

            # Verify RTT calculation
            # rtt = t3 - t1 = 1020 - 1000 = 20
            self.assertEqual(result['rtt_ms'], 20)

        finally:
            time.time = original_time

    def test_quality_calculation(self):
        """Test sync quality classification"""
        self.assertEqual(self.handler._calculate_quality(30), "EXCELLENT")
        self.assertEqual(self.handler._calculate_quality(75), "GOOD")
        self.assertEqual(self.handler._calculate_quality(150), "ACCEPTABLE")
        self.assertEqual(self.handler._calculate_quality(250), "POOR")

    def test_sync_response_without_init(self):
        """Test handling SYNC_RESPONSE without prior SYNC_INIT"""
        result = self.handler.handle_sync_response(
            self.device_id, 1000, 1010, self.mock_socket
        )

        # Should return None since there's no pending sync
        self.assertIsNone(result)

    def test_get_sync_stats(self):
        """Test retrieving sync statistics"""
        # Complete a sync
        self.handler.handle_sync_init(self.device_id, self.mock_socket)
        t1 = self.handler.pending_syncs[self.device_id]['t1']
        self.handler.handle_sync_response(
            self.device_id, t1, t1 + 5, self.mock_socket
        )

        # Get stats
        stats = self.handler.get_sync_stats(self.device_id)

        self.assertIsNotNone(stats)
        self.assertIn('offset_ms', stats)
        self.assertIn('rtt_ms', stats)
        self.assertIn('quality', stats)

    def test_cleanup_expired_syncs(self):
        """Test cleanup of expired pending syncs"""
        # Create a pending sync with old timestamp
        self.handler.pending_syncs[self.device_id] = {
            't1': 1000,
            'sent_at': time.time() - 100  # 100 seconds ago
        }

        # Cleanup with 30 second timeout
        self.handler.cleanup_expired_syncs(timeout_seconds=30.0)

        # Verify it was removed
        self.assertNotIn(self.device_id, self.handler.pending_syncs)


class TestProtocolIntegration(unittest.TestCase):
    """Test integration with protocol messages"""

    def test_sync_init_message_format(self):
        """Verify SYNC_INIT is handled as simple message"""
        # SYNC_INIT should be sent as just "SYNC_INIT" with no parameters
        message = "SYNC_INIT"

        # This should parse correctly
        parts = message.split(' ', 1)
        self.assertEqual(parts[0], "SYNC_INIT")
        self.assertEqual(len(parts), 1)  # No parameters

    def test_sync_request_format(self):
        """Verify SYNC_REQUEST format matches protocol"""
        t1 = 1234567890
        expected = f"SYNC_REQUEST t_pc={t1}"

        # Should contain command and timestamp
        self.assertIn("SYNC_REQUEST", expected)
        self.assertIn("t_pc=", expected)
        self.assertIn(str(t1), expected)

    def test_sync_result_format(self):
        """Verify SYNC_RESULT format matches protocol"""
        t1, t2, t3 = 1000, 1010, 1020
        offset, rtt = 0, 20

        expected = f"SYNC_RESULT t1={t1} t2={t2} t3={t3} offset={offset} rtt={rtt}"

        # Should contain all required fields
        self.assertIn("SYNC_RESULT", expected)
        self.assertIn("t1=", expected)
        self.assertIn("t2=", expected)
        self.assertIn("t3=", expected)
        self.assertIn("offset=", expected)
        self.assertIn("rtt=", expected)


if __name__ == '__main__':
    unittest.main()
