#!/usr/bin/env python3
"""
Comprehensive Protocol Verification Test

This test verifies the complete communication flow between PC and Android:
1. PC sends commands (START_RECORD, STOP_RECORD, SYNC_REQUEST)
2. Android correctly parses and processes them
3. Android sends appropriate responses (ACK, ERROR)
4. PC correctly parses Android responses
5. Bidirectional message flow works correctly

This simulates what happens when PC Controller sends commands and verifies
the Android app would correctly interpret and respond to them.
"""

import json
import socket
import threading
import time
import unittest
from typing import Optional, List, Tuple

from protocol_adapter import ProtocolAdapter


class MockAndroidDevice:
    """
    Mock Android device that implements the Protocol.kt specification.
    This simulates Android's ProtocolHandler and NetworkServer behavior.
    """

    def __init__(self, port: int = 8081):
        self.port = port
        self.adapter = ProtocolAdapter()
        self.server_socket = None
        self.client_socket = None
        self.running = False
        self.received_messages = []
        self.sent_messages = []

        # Simulated state
        self.is_recording = False
        self.current_session_id = None
        self.device_id = "mock_android_001"
        self.sensors = ["GSR", "RGB", "THERMAL"]

    def start(self):
        """Start mock Android server"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(('127.0.0.1', self.port))
        self.server_socket.listen(1)
        self.running = True

        # Accept connection in thread
        accept_thread = threading.Thread(target=self._accept_connection)
        accept_thread.daemon = True
        accept_thread.start()

    def _accept_connection(self):
        """Accept PC connection and send HELLO"""
        try:
            self.client_socket, addr = self.server_socket.accept()

            # Send HELLO message immediately (as Android does)
            hello_msg = f"HELLO device_name={self.device_id} sensors=[{','.join(self.sensors)}]\n"
            self.client_socket.send(hello_msg.encode('utf-8'))
            self.sent_messages.append(hello_msg.strip())

            # Listen for messages
            buffer = ""
            while self.running:
                data = self.client_socket.recv(1024).decode('utf-8')
                if not data:
                    break

                buffer += data

                # Process complete messages (newline-delimited)
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    if line.strip():
                        self._process_message(line.strip())
        except Exception as e:
            if self.running:
                print(f"Mock Android error: {e}")

    def _process_message(self, message: str):
        """Process message from PC (simulates ProtocolHandler)"""
        self.received_messages.append(message)

        # Parse message
        parsed = self.adapter.parse_android_message(message)
        if not parsed:
            return

        msg_type = parsed.type
        params = parsed.parameters

        # Simulate Android's ProtocolHandler.processMessage behavior
        response = None

        if msg_type == 'START_RECORD':
            session_id = params.get('session_id', '')
            if self.is_recording:
                # Already recording - send ERROR
                response = self.adapter.create_error(
                    'START_RECORD',
                    'BUSY',
                    'Already recording'
                )
            else:
                # Start recording - send ACK
                self.is_recording = True
                self.current_session_id = session_id
                response = self.adapter.create_ack('START_RECORD', session_id=session_id)

        elif msg_type == 'STOP_RECORD':
            session_id = params.get('session_id', '')
            if not self.is_recording:
                # Not recording - send ERROR
                response = self.adapter.create_error(
                    'STOP_RECORD',
                    'FAIL',
                    'Not recording'
                )
            else:
                # Stop recording - send ACK
                self.is_recording = False
                response = self.adapter.create_ack('STOP_RECORD', session_id=session_id)

        elif msg_type == 'SYNC_REQUEST':
            t_pc = int(params.get('t_pc', 0))
            t_ph = int(time.time() * 1000)  # Current time in ms
            response = f"SYNC_RESPONSE t_pc={t_pc} t_ph={t_ph}"

        elif msg_type == 'SYNC_RESULT':
            # Android doesn't respond to SYNC_RESULT, just processes it
            response = None

        # Send response if any
        if response:
            try:
                self.client_socket.send((response + '\n').encode('utf-8'))
                self.sent_messages.append(response)
            except Exception as e:
                print(f"Failed to send response: {e}")

    def stop(self):
        """Stop mock Android server"""
        self.running = False
        if self.client_socket:
            try:
                self.client_socket.close()
            except (OSError, socket.error):
                pass
        if self.server_socket:
            try:
                self.server_socket.close()
            except (OSError, socket.error):
                pass


class TestProtocolVerification(unittest.TestCase):
    """Test complete protocol communication flow"""

    def setUp(self):
        """Set up mock Android device and PC client"""
        self.mock_android = MockAndroidDevice(port=8081)
        self.mock_android.start()
        time.sleep(0.2)  # Give server time to start

        self.adapter = ProtocolAdapter()
        self.pc_socket = None
        self.received_messages = []

    def tearDown(self):
        """Clean up"""
        if self.pc_socket:
            try:
                self.pc_socket.close()
            except (OSError, socket.error):
                pass
        self.mock_android.stop()
        time.sleep(0.1)

    def connect_to_android(self) -> bool:
        """Connect PC to mock Android device"""
        try:
            self.pc_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.pc_socket.settimeout(5.0)
            self.pc_socket.connect(('127.0.0.1', 8081))
            return True
        except Exception as e:
            print(f"Connection failed: {e}")
            return False

    def send_command(self, command_type: str, **params) -> bool:
        """Send command from PC to Android"""
        try:
            # Create message in Android text protocol format
            json_msg = {'type': command_type}
            json_msg.update(params)
            android_msg = self.adapter.json_to_android(json_msg)

            self.pc_socket.send((android_msg + '\n').encode('utf-8'))
            return True
        except Exception as e:
            print(f"Send failed: {e}")
            return False

    def receive_response(self, timeout: float = 2.0) -> Optional[str]:
        """Receive response from Android"""
        try:
            self.pc_socket.settimeout(timeout)
            data = self.pc_socket.recv(1024).decode('utf-8')
            if data:
                # Get first complete message
                if '\n' in data:
                    line = data.split('\n')[0].strip()
                    if line:
                        self.received_messages.append(line)
                        return line
            return None
        except socket.timeout:
            return None
        except Exception as e:
            print(f"Receive failed: {e}")
            return None

    def test_01_connection_and_hello(self):
        """Test 1: PC connects and receives HELLO message"""
        print("\n" + "=" * 70)
        print("TEST 1: Connection and HELLO message")
        print("=" * 70)

        # Connect to Android
        connected = self.connect_to_android()
        self.assertTrue(connected, "PC should connect to Android")

        # Receive HELLO message
        hello = self.receive_response()
        self.assertIsNotNone(hello, "PC should receive HELLO message from Android")

        # Parse HELLO
        json_hello = self.adapter.android_to_json(hello)
        self.assertIsNotNone(json_hello, "HELLO message should parse correctly")
        self.assertEqual(json_hello['type'], 'HELLO')
        self.assertEqual(json_hello['device_name'], 'mock_android_001')
        self.assertIn('GSR', json_hello['sensors'])

        print(f" PC connected successfully")
        print(f" Received HELLO: {hello}")
        print(f" Parsed: device={json_hello['device_name']}, sensors={json_hello['sensors']}")

    def test_02_start_recording_success(self):
        """Test 2: PC sends START_RECORD, Android starts recording and sends ACK"""
        print("\n" + "=" * 70)
        print("TEST 2: START_RECORD command - Success case")
        print("=" * 70)

        # Connect
        self.connect_to_android()
        self.receive_response()  # Consume HELLO

        # Send START_RECORD
        session_id = "test_session_001"
        sent = self.send_command('START_RECORD', session_id=session_id)
        self.assertTrue(sent, "PC should send START_RECORD command")
        print(f" PC sent: START_RECORD session_id={session_id}")

        # Wait a bit for Android to process
        time.sleep(0.1)

        # Verify Android received and processed the command
        self.assertGreater(len(self.mock_android.received_messages), 0,
                           "Android should receive the command")
        received = self.mock_android.received_messages[-1]
        self.assertIn('START_RECORD', received)
        self.assertIn(session_id, received)
        print(f" Android received: {received}")

        # Verify Android started recording
        self.assertTrue(self.mock_android.is_recording,
                        "Android should start recording")
        self.assertEqual(self.mock_android.current_session_id, session_id,
                         "Android should store session ID")
        print(f" Android recording state: {self.mock_android.is_recording}")

        # Receive ACK
        ack = self.receive_response()
        self.assertIsNotNone(ack, "PC should receive ACK response")
        self.assertIn('ACK', ack)
        self.assertIn('START_RECORD', ack)
        self.assertIn(session_id, ack)
        print(f" PC received ACK: {ack}")

        # Parse ACK
        json_ack = self.adapter.android_to_json(ack)
        self.assertEqual(json_ack['type'], 'ACK')
        self.assertEqual(json_ack['cmd'], 'START_RECORD')
        print(f" ACK parsed correctly")

    def test_03_start_recording_while_recording(self):
        """Test 3: PC sends START_RECORD while already recording - Android sends ERROR"""
        print("\n" + "=" * 70)
        print("TEST 3: START_RECORD command - Already recording (ERROR case)")
        print("=" * 70)

        # Connect and start recording first
        self.connect_to_android()
        self.receive_response()  # Consume HELLO

        # Start recording
        self.send_command('START_RECORD', session_id='session_001')
        self.receive_response()  # Consume ACK
        time.sleep(0.1)

        # Try to start again
        sent = self.send_command('START_RECORD', session_id='session_002')
        self.assertTrue(sent, "PC should send second START_RECORD")
        print(f" PC sent: START_RECORD session_id=session_002 (while already recording)")

        time.sleep(0.1)

        # Receive ERROR
        error = self.receive_response()
        self.assertIsNotNone(error, "PC should receive ERROR response")
        self.assertIn('ERROR', error)
        self.assertIn('BUSY', error)
        print(f" PC received ERROR: {error}")

        # Parse ERROR
        json_error = self.adapter.android_to_json(error)
        self.assertEqual(json_error['type'], 'ERROR')
        self.assertEqual(json_error['cmd'], 'START_RECORD')
        self.assertEqual(json_error['code'], 'BUSY')
        print(f" ERROR parsed correctly: {json_error['msg']}")

    def test_04_stop_recording_success(self):
        """Test 4: PC sends STOP_RECORD, Android stops recording and sends ACK"""
        print("\n" + "=" * 70)
        print("TEST 4: STOP_RECORD command - Success case")
        print("=" * 70)

        # Connect and start recording
        self.connect_to_android()
        self.receive_response()  # Consume HELLO
        session_id = "test_session_003"
        self.send_command('START_RECORD', session_id=session_id)
        self.receive_response()  # Consume ACK
        time.sleep(0.1)

        # Send STOP_RECORD
        sent = self.send_command('STOP_RECORD', session_id=session_id)
        self.assertTrue(sent, "PC should send STOP_RECORD command")
        print(f" PC sent: STOP_RECORD session_id={session_id}")

        time.sleep(0.1)

        # Verify Android stopped recording
        self.assertFalse(self.mock_android.is_recording,
                         "Android should stop recording")
        print(f" Android recording state: {self.mock_android.is_recording}")

        # Receive ACK
        ack = self.receive_response()
        self.assertIsNotNone(ack, "PC should receive ACK response")
        self.assertIn('ACK', ack)
        self.assertIn('STOP_RECORD', ack)
        print(f" PC received ACK: {ack}")

    def test_05_stop_recording_not_recording(self):
        """Test 5: PC sends STOP_RECORD when not recording - Android sends ERROR"""
        print("\n" + "=" * 70)
        print("TEST 5: STOP_RECORD command - Not recording (ERROR case)")
        print("=" * 70)

        # Connect (without starting recording)
        self.connect_to_android()
        self.receive_response()  # Consume HELLO

        # Send STOP_RECORD
        sent = self.send_command('STOP_RECORD', session_id='session_004')
        self.assertTrue(sent, "PC should send STOP_RECORD command")
        print(f" PC sent: STOP_RECORD (while not recording)")

        time.sleep(0.1)

        # Receive ERROR
        error = self.receive_response()
        self.assertIsNotNone(error, "PC should receive ERROR response")
        self.assertIn('ERROR', error)
        print(f" PC received ERROR: {error}")

        # Parse ERROR
        json_error = self.adapter.android_to_json(error)
        self.assertEqual(json_error['type'], 'ERROR')
        self.assertEqual(json_error['cmd'], 'STOP_RECORD')
        print(f" ERROR parsed correctly: {json_error['msg']}")

    def test_06_time_sync(self):
        """Test 6: PC sends SYNC_REQUEST, Android responds with SYNC_RESPONSE"""
        print("\n" + "=" * 70)
        print("TEST 6: Time synchronization (SYNC_REQUEST/SYNC_RESPONSE)")
        print("=" * 70)

        # Connect
        self.connect_to_android()
        self.receive_response()  # Consume HELLO

        # Send SYNC_REQUEST
        t1 = int(time.time() * 1000)
        sent = self.send_command('SYNC_REQUEST', t_pc=t1)
        self.assertTrue(sent, "PC should send SYNC_REQUEST")
        print(f" PC sent: SYNC_REQUEST t_pc={t1}")

        time.sleep(0.1)

        # Receive SYNC_RESPONSE
        sync_resp = self.receive_response()
        self.assertIsNotNone(sync_resp, "PC should receive SYNC_RESPONSE")
        self.assertIn('SYNC_RESPONSE', sync_resp)
        print(f" PC received: {sync_resp}")

        # Parse SYNC_RESPONSE
        json_sync = self.adapter.android_to_json(sync_resp)
        self.assertEqual(json_sync['type'], 'SYNC_RESPONSE')
        self.assertEqual(json_sync['t_pc'], t1, "Should echo back t_pc")
        self.assertGreater(json_sync['t_ph'], 0, "Should include Android timestamp")
        print(f" SYNC_RESPONSE parsed: t_pc={json_sync['t_pc']}, t_ph={json_sync['t_ph']}")

        # Calculate offset and RTT (NTP algorithm)
        t3 = int(time.time() * 1000)
        t2 = json_sync['t_ph']
        rtt = t3 - t1
        offset = int((t2 - t1 - rtt / 2))
        print(f" Time sync calculated: offset={offset}ms, rtt={rtt}ms")

        # Send SYNC_RESULT back to Android
        sent = self.send_command('SYNC_RESULT', t1=t1, t2=t2, t3=t3,
                                 offset=offset, rtt=rtt)
        self.assertTrue(sent, "PC should send SYNC_RESULT")
        print(f" PC sent: SYNC_RESULT with calculated offset and RTT")

        time.sleep(0.1)

        # Verify Android received SYNC_RESULT
        self.assertIn('SYNC_RESULT', self.mock_android.received_messages[-1])
        print(f" Android received and processed SYNC_RESULT")

    def test_07_complete_session_flow(self):
        """Test 7: Complete recording session flow"""
        print("\n" + "=" * 70)
        print("TEST 7: Complete recording session flow")
        print("=" * 70)

        # Connect
        self.connect_to_android()
        hello = self.receive_response()
        print(f"1. Connected and received HELLO: {hello}")

        # Time sync
        t1 = int(time.time() * 1000)
        self.send_command('SYNC_REQUEST', t_pc=t1)
        sync_resp = self.receive_response()
        print(f"2. Time synchronized: {sync_resp}")

        # Send SYNC_RESULT
        json_sync = self.adapter.android_to_json(sync_resp)
        t2 = json_sync['t_ph']
        t3 = int(time.time() * 1000)
        rtt = t3 - t1
        offset = int((t2 - t1 - rtt / 2))
        self.send_command('SYNC_RESULT', t1=t1, t2=t2, t3=t3, offset=offset, rtt=rtt)
        print(f"3. Sent SYNC_RESULT to Android")

        time.sleep(0.1)

        # Start recording
        session_id = "complete_session_test"
        self.send_command('START_RECORD', session_id=session_id)
        start_ack = self.receive_response()
        self.assertIn('ACK', start_ack)
        self.assertTrue(self.mock_android.is_recording)
        print(f"4. Recording started: {start_ack}")

        # Simulate recording for a moment
        time.sleep(0.2)
        print(f"5. Recording in progress...")

        # Stop recording
        self.send_command('STOP_RECORD', session_id=session_id)
        stop_ack = self.receive_response()
        self.assertIn('ACK', stop_ack)
        self.assertFalse(self.mock_android.is_recording)
        print(f"6. Recording stopped: {stop_ack}")

        print(f"\n Complete session flow executed successfully")
        print(f"  - Messages sent by PC: {len(self.mock_android.received_messages)}")
        print(f"  - Messages sent by Android: {len(self.mock_android.sent_messages)}")


def run_verification_tests():
    """Run all verification tests"""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # Add test cases in order
    suite.addTests(loader.loadTestsFromTestCase(TestProtocolVerification))

    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    # Print summary
    print("\n" + "=" * 70)
    print("PROTOCOL VERIFICATION TEST SUMMARY")
    print("=" * 70)
    print(f"Tests run: {result.testsRun}")
    print(f"Successes: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")

    if result.wasSuccessful():
        print("\n ALL PROTOCOL VERIFICATION TESTS PASSED ")
        print("\nVerified:")
        print("  - PC can connect to Android")
        print("  - Android sends HELLO message")
        print("  - PC can send START_RECORD command")
        print("  - Android starts recording and sends ACK")
        print("  - PC can send STOP_RECORD command")
        print("  - Android stops recording and sends ACK")
        print("  - PC can perform time synchronization")
        print("  - Android handles error cases correctly")
        print("  - Complete session flow works end-to-end")
    else:
        print("\n SOME TESTS FAILED ")

    print("=" * 70)

    return result.wasSuccessful()


if __name__ == '__main__':
    import sys

    success = run_verification_tests()
    sys.exit(0 if success else 1)
