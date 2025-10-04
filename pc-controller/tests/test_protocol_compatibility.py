#!/usr/bin/env python3
"""
Test suite for PC-Android protocol compatibility

Tests the protocol adapter and unified controller against Android Protocol.kt specification.
"""

import socket
import threading
import time
import unittest

from protocol_adapter import ProtocolAdapter


class TestProtocolAdapter(unittest.TestCase):
    """Test protocol adapter functionality"""

    def setUp(self):
        self.adapter = ProtocolAdapter()

    def test_parse_hello_message(self):
        """Test parsing HELLO message from Android"""
        android_msg = 'HELLO device_name=android_001 sensors=[GSR,RGB,THERMAL]'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'HELLO')
        self.assertEqual(json_msg['device_name'], 'android_001')
        self.assertEqual(json_msg['sensors'], ['GSR', 'RGB', 'THERMAL'])

    def test_parse_start_record(self):
        """Test parsing START_RECORD message"""
        android_msg = 'START_RECORD session_id=session_20240101_120000'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'START_RECORD')
        self.assertEqual(json_msg['session_id'], 'session_20240101_120000')

    def test_parse_stop_record(self):
        """Test parsing STOP_RECORD message"""
        android_msg = 'STOP_RECORD session_id=session_123'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'STOP_RECORD')
        self.assertEqual(json_msg['session_id'], 'session_123')

    def test_parse_data_gsr(self):
        """Test parsing DATA_GSR message"""
        android_msg = 'DATA_GSR ts=1234567890 value=5.5'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'DATA_GSR')
        self.assertEqual(json_msg['ts'], 1234567890)
        self.assertAlmostEqual(json_msg['value'], 5.5, places=1)

    def test_parse_sync_request(self):
        """Test parsing SYNC_REQUEST message"""
        android_msg = 'SYNC_REQUEST t_pc=1234567890'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'SYNC_REQUEST')
        self.assertEqual(json_msg['t_pc'], 1234567890)

    def test_parse_sync_response(self):
        """Test parsing SYNC_RESPONSE message"""
        android_msg = 'SYNC_RESPONSE t_pc=1234567890 t_ph=1234567895'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'SYNC_RESPONSE')
        self.assertEqual(json_msg['t_pc'], 1234567890)
        self.assertEqual(json_msg['t_ph'], 1234567895)

    def test_parse_ack(self):
        """Test parsing ACK message"""
        android_msg = 'ACK cmd=START_RECORD session_id=session_123'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'ACK')
        self.assertEqual(json_msg['cmd'], 'START_RECORD')
        self.assertEqual(json_msg['session_id'], 'session_123')

    def test_parse_error(self):
        """Test parsing ERROR message"""
        android_msg = 'ERROR cmd=START_RECORD code=SENSOR_FAIL msg="GSR not connected"'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'ERROR')
        self.assertEqual(json_msg['cmd'], 'START_RECORD')
        self.assertEqual(json_msg['code'], 'SENSOR_FAIL')
        self.assertEqual(json_msg['msg'], 'GSR not connected')

    def test_parse_quoted_values(self):
        """Test parsing messages with quoted values"""
        android_msg = 'ERROR cmd=TEST msg="This is a test message"'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['msg'], 'This is a test message')

    def test_parse_array_values(self):
        """Test parsing array syntax"""
        android_msg = 'HELLO sensors=[A,B,C,D]'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['sensors'], ['A', 'B', 'C', 'D'])

    def test_create_ack(self):
        """Test creating ACK message"""
        ack = self.adapter.create_ack('START_RECORD', session_id='test_123')

        self.assertIn('ACK', ack)
        self.assertIn('cmd=START_RECORD', ack)
        self.assertIn('session_id=test_123', ack)

    def test_create_sync_result(self):
        """Test creating SYNC_RESULT message"""
        sync_result = self.adapter.create_sync_result(1000, 1005, 1010, 5, 10)

        self.assertIn('SYNC_RESULT', sync_result)
        self.assertIn('t1=1000', sync_result)
        self.assertIn('t2=1005', sync_result)
        self.assertIn('t3=1010', sync_result)
        self.assertIn('offset=5', sync_result)
        self.assertIn('rtt=10', sync_result)

    def test_create_error(self):
        """Test creating ERROR message"""
        error = self.adapter.create_error('START_RECORD', 'FAIL', 'Test error')

        self.assertIn('ERROR', error)
        self.assertIn('cmd=START_RECORD', error)
        self.assertIn('code=FAIL', error)
        self.assertIn('msg="Test error"', error)

    def test_json_to_android(self):
        """Test converting JSON to Android format"""
        json_msg = {
            'type': 'START_RECORD',
            'session_id': 'test_session'
        }
        android_msg = self.adapter.json_to_android(json_msg)

        self.assertIn('START_RECORD', android_msg)
        self.assertIn('session_id=test_session', android_msg)

    def test_bidirectional_conversion(self):
        """Test round-trip conversion"""
        original = 'DATA_GSR ts=1234567890 value=5.5'

        # Android -> JSON -> Android
        json_msg = self.adapter.android_to_json(original)
        back_to_android = self.adapter.json_to_android(json_msg)

        # Parse both and compare
        json1 = self.adapter.android_to_json(original)
        json2 = self.adapter.android_to_json(back_to_android)

        self.assertEqual(json1['type'], json2['type'])
        self.assertAlmostEqual(json1['value'], json2['value'], places=1)

    def test_malformed_message(self):
        """Test handling malformed messages"""
        malformed = 'INVALID MESSAGE FORMAT'
        json_msg = self.adapter.android_to_json(malformed)

        # Should return something, not crash
        self.assertIsNotNone(json_msg)

    def test_empty_message(self):
        """Test handling empty message"""
        json_msg = self.adapter.android_to_json('')
        self.assertIsNone(json_msg)

    def test_message_with_no_params(self):
        """Test message with no parameters"""
        android_msg = 'HEARTBEAT'
        json_msg = self.adapter.android_to_json(android_msg)

        self.assertIsNotNone(json_msg)
        self.assertEqual(json_msg['type'], 'HEARTBEAT')


class TestProtocolCompatibility(unittest.TestCase):
    """Test compatibility with Android Protocol.kt specification"""

    def test_all_android_message_types(self):
        """Test that all Android message types can be parsed"""
        adapter = ProtocolAdapter()

        test_messages = [
            'HELLO device_name=test sensors=[GSR]',
            'SYNC_REQUEST t_pc=123',
            'SYNC_RESPONSE t_pc=123 t_ph=456',
            'SYNC_RESULT t1=1 t2=2 t3=3 offset=0 rtt=10',
            'START_RECORD session_id=s123',
            'STOP_RECORD session_id=s123',
            'ACK cmd=START_RECORD',
            'ERROR cmd=START_RECORD code=FAIL msg="error"',
            'DATA_GSR ts=123 value=5.5',
            'FRAME type=RGB size=1024',
        ]

        for msg in test_messages:
            json_msg = adapter.android_to_json(msg)
            self.assertIsNotNone(json_msg, f"Failed to parse: {msg}")
            self.assertIn('type', json_msg, f"No type in parsed message: {msg}")

    def test_message_type_mapping(self):
        """Test message type name mapping"""
        adapter = ProtocolAdapter()

        # Android types should be preserved
        self.assertEqual(adapter.ANDROID_TO_PC_TYPES['HELLO'], 'HELLO')
        self.assertEqual(adapter.ANDROID_TO_PC_TYPES['START_RECORD'], 'START_RECORD')
        self.assertEqual(adapter.ANDROID_TO_PC_TYPES['DATA_GSR'], 'DATA_GSR')

    def test_parameter_parsing_accuracy(self):
        """Test accurate parameter parsing"""
        adapter = ProtocolAdapter()

        # Test integer parameters
        msg = adapter.android_to_json('TEST ts=1234567890')
        self.assertEqual(msg['ts'], 1234567890)
        self.assertIsInstance(msg['ts'], int)

        # Test float parameters
        msg = adapter.android_to_json('TEST value=5.5')
        self.assertAlmostEqual(msg['value'], 5.5, places=1)
        self.assertIsInstance(msg['value'], float)

        # Test string parameters
        msg = adapter.android_to_json('TEST name=test_device')
        self.assertEqual(msg['name'], 'test_device')
        self.assertIsInstance(msg['name'], str)


class TestNetworkProtocol(unittest.TestCase):
    """Test network protocol implementation"""

    def test_message_delimiter(self):
        """Test that messages are newline-delimited"""
        adapter = ProtocolAdapter()

        # Multiple messages in buffer
        buffer = "HELLO device_name=test\nDATA_GSR ts=123 value=5.5\n"
        messages = buffer.strip().split('\n')

        self.assertEqual(len(messages), 2)

        json1 = adapter.android_to_json(messages[0])
        json2 = adapter.android_to_json(messages[1])

        self.assertEqual(json1['type'], 'HELLO')
        self.assertEqual(json2['type'], 'DATA_GSR')


def run_tests():
    """Run all tests"""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # Add test cases
    suite.addTests(loader.loadTestsFromTestCase(TestProtocolAdapter))
    suite.addTests(loader.loadTestsFromTestCase(TestProtocolCompatibility))
    suite.addTests(loader.loadTestsFromTestCase(TestNetworkProtocol))

    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    # Print summary
    print("\n" + "=" * 70)
    print("Protocol Compatibility Test Summary")
    print("=" * 70)
    print(f"Tests run: {result.testsRun}")
    print(f"Successes: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print("=" * 70)

    return result.wasSuccessful()


if __name__ == '__main__':
    import sys

    success = run_tests()
    sys.exit(0 if success else 1)
