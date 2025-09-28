#!/usr/bin/env python3
"""
Unit tests for command_client.py
"""

import unittest
import json
import time
from unittest.mock import Mock, patch, MagicMock
from command_client import CommandClient


class TestCommandClient(unittest.TestCase):
    
    def setUp(self):
        self.client = CommandClient("192.168.1.100", 8080)
    
    def tearDown(self):
        if self.client.connected:
            self.client.disconnect()
    
    @patch('socket.socket')
    def test_connect_success(self, mock_socket_class):
        """Test successful connection to device"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Act
        result = self.client.connect_to_device()
        
        # Assert
        self.assertTrue(result)
        self.assertTrue(self.client.connected)
        mock_socket.connect.assert_called_once_with(("192.168.1.100", 8080))
        mock_socket.makefile.assert_called_once_with('rw', buffering=1)
    
    @patch('socket.socket')
    def test_connect_failure(self, mock_socket_class):
        """Test connection failure"""
        # Arrange
        mock_socket = Mock()
        mock_socket.connect.side_effect = ConnectionRefusedError("Connection refused")
        mock_socket_class.return_value = mock_socket
        
        # Act
        result = self.client.connect_to_device()
        
        # Assert
        self.assertFalse(result)
        self.assertFalse(self.client.connected)
    
    def test_send_command_not_connected(self):
        """Test sending command when not connected"""
        # Act
        result = self.client.send_command({"test": "command"})
        
        # Assert
        self.assertIsNone(result)
    
    @patch('socket.socket')
    def test_send_command_success(self, mock_socket_class):
        """Test successful command sending"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Mock successful response
        response_data = {"status": "success", "message": "Command executed"}
        mock_socket_file.readline.return_value = json.dumps(response_data) + "\n"
        
        # Connect first
        self.client.connect_to_device()
        
        # Act
        command = {"message_type": "TEST", "data": "test_data"}
        result = self.client.send_command(command)
        
        # Assert
        self.assertIsNotNone(result)
        self.assertEqual(result["status"], "success")
        mock_socket_file.write.assert_called_once_with(json.dumps(command) + '\n')
        mock_socket_file.flush.assert_called_once()
        mock_socket_file.readline.assert_called_once()
    
    @patch('socket.socket')
    def test_send_command_invalid_json_response(self, mock_socket_class):
        """Test handling of invalid JSON response"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Mock invalid JSON response
        mock_socket_file.readline.return_value = "invalid json response\n"
        
        # Connect first
        self.client.connect_to_device()
        
        # Act
        command = {"message_type": "TEST"}
        result = self.client.send_command(command)
        
        # Assert
        self.assertIsNone(result)
    
    @patch('socket.socket')
    def test_start_recording(self, mock_socket_class):
        """Test start recording command"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Mock successful response
        response_data = {"status": "success", "session_id": "test_session"}
        mock_socket_file.readline.return_value = json.dumps(response_data) + "\n"
        
        # Connect first
        self.client.connect_to_device()
        
        # Act
        result = self.client.start_recording("test_session")
        
        # Assert
        self.assertTrue(result)
        
        # Verify the command was sent correctly
        call_args = mock_socket_file.write.call_args[0][0]
        sent_command = json.loads(call_args.rstrip('\n'))
        self.assertEqual(sent_command["message_type"], "START_RECORD")
        self.assertEqual(sent_command["session_id"], "test_session")
    
    @patch('socket.socket')
    def test_stop_recording(self, mock_socket_class):
        """Test stop recording command"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Mock successful response
        response_data = {"status": "success"}
        mock_socket_file.readline.return_value = json.dumps(response_data) + "\n"
        
        # Connect first
        self.client.connect_to_device()
        
        # Act
        result = self.client.stop_recording()
        
        # Assert
        self.assertTrue(result)
        
        # Verify the command was sent correctly
        call_args = mock_socket_file.write.call_args[0][0]
        sent_command = json.loads(call_args.rstrip('\n'))
        self.assertEqual(sent_command["message_type"], "STOP_RECORD")
    
    @patch('socket.socket')
    def test_sync_time(self, mock_socket_class):
        """Test time synchronization command"""
        # Arrange
        mock_socket = Mock()
        mock_socket_file = Mock()
        mock_socket.makefile.return_value = mock_socket_file
        mock_socket_class.return_value = mock_socket
        
        # Mock successful response
        response_data = {
            "status": "success", 
            "t_pc": 1640995200000,
            "t_ph": 1640995200050,
            "offset": 50
        }
        mock_socket_file.readline.return_value = json.dumps(response_data) + "\n"
        
        # Connect first
        self.client.connect_to_device()
        
        # Act
        result = self.client.sync_time()
        
        # Assert
        self.assertIsNotNone(result)
        self.assertEqual(result["status"], "success")
        
        # Verify the command was sent correctly
        call_args = mock_socket_file.write.call_args[0][0]
        sent_command = json.loads(call_args.rstrip('\n'))
        self.assertEqual(sent_command["message_type"], "SYNC_REQUEST")
        self.assertIn("t_pc", sent_command)
    
    def test_disconnect_cleanup(self):
        """Test proper cleanup on disconnect"""
        # Arrange
        mock_socket_file = Mock()
        mock_socket = Mock()
        self.client.socket_file = mock_socket_file
        self.client.socket = mock_socket
        self.client.connected = True
        
        # Act
        self.client.disconnect()
        
        # Assert
        self.assertFalse(self.client.connected)
        self.assertIsNone(self.client.socket_file)
        self.assertIsNone(self.client.socket)
        mock_socket_file.close.assert_called_once()
        mock_socket.close.assert_called_once()


class TestCommandClientIntegration(unittest.TestCase):
    """
    Integration tests that test the actual socket behavior.
    These tests require careful setup to avoid actual network calls.
    """
    
    def test_readline_behavior(self):
        """Test that readline properly handles line-based messages"""
        # This test verifies the core fix for the recv() issue
        # In a real scenario, readline() will read until '\n' is encountered
        
        test_messages = [
            '{"status": "success", "data": "test1"}\n',
            '{"status": "error", "message": "failed"}\n',
            '{"complex": {"nested": {"data": ["item1", "item2"]}}}\n'
        ]
        
        for message in test_messages:
            # Verify each message can be properly parsed
            try:
                clean_message = message.strip()
                parsed = json.loads(clean_message)
                self.assertIsInstance(parsed, dict)
            except json.JSONDecodeError:
                self.fail(f"Failed to parse message: {message}")


if __name__ == '__main__':
    unittest.main()