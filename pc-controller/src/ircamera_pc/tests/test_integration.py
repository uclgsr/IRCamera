"""
Comprehensive integration tests for PC Controller Hub-and-Spoke architecture
Tests complete system integration, multi-device coordination, and end-to-end workflows
"""

import asyncio
import json
import os
import shutil
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from typing import Any, Dict, List, Optional
from unittest.mock import AsyncMock, MagicMock, Mock, patch

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", ".."))

from ircamera_pc.core.session_manager import SessionManager
from ircamera_pc.data.aggregator import DataAggregator
from ircamera_pc.gui.main_window import MainWindow
from ircamera_pc.network.server import NetworkServer


class TestEndToEndIntegration(unittest.TestCase):
    """End-to-end integration tests for complete Hub-and-Spoke system"""

    def setUp(self) -> None:
        """Set up integration test environment"""
        self.temp_dir = tempfile.mkdtemp()

        # Initialize core components
        self.network_server = NetworkServer(host="localhost", port=8080)
        self.data_aggregator = DataAggregator(output_dir=self.temp_dir)
        self.session_manager = SessionManager()

        # Test devices
        self.test_devices = [
            {
                "device_id": "ANDROID_001",
                "device_type": "android_spoke",
                "capabilities": ["rgb", "thermal", "gsr"],
                "location": "192.168.1.101",
            },
            {
                "device_id": "ANDROID_002",
                "device_type": "android_spoke",
                "capabilities": ["rgb", "gsr"],
                "location": "192.168.1.102",
            },
            {
                "device_id": "ANDROID_003",
                "device_type": "android_spoke",
                "capabilities": ["thermal", "gsr"],
                "location": "192.168.1.103",
            },
        ]

    def tearDown(self) -> Any:
        """Clean up integration test environment"""
        if self.network_server.is_running():
            self.network_server.stop()
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_complete_system_startup_shutdown(self) -> Any:
        """Test complete system startup and shutdown sequence"""
        # Step 1: Start network server
        server_started = self.network_server.start()
        self.assertTrue(server_started, "Network server should start successfully")

        # Step 2: Initialize data aggregator
        aggregator_initialized = self.data_aggregator.initialize()
        self.assertTrue(aggregator_initialized, "Data aggregator should initialize")

        # Step 3: Start session manager
        session_started = self.session_manager.start()
        self.assertTrue(session_started, "Session manager should start")

        # Verify system is running
        self.assertTrue(self.network_server.is_running())
        self.assertTrue(self.session_manager.is_active())

        # Step 4: Graceful shutdown
        self.session_manager.stop()
        self.data_aggregator.shutdown()
        self.network_server.stop()

        # Verify clean shutdown
        self.assertFalse(self.network_server.is_running())
        self.assertFalse(self.session_manager.is_active())

    def test_multi_device_discovery_and_registration(self) -> Any:
        """Test discovery and registration of multiple Android devices"""
        self.network_server.start()

        registered_devices = []

        # Simulate devices connecting
        for device_config in self.test_devices:
            # Mock device connection
            mock_socket = Mock()

            # Device sends registration message
            registration_msg = {"type": "device_registration", **device_config}

            # Process registration
            response = self.network_server._handle_device_registration(
                registration_msg, mock_socket
            )

            self.assertEqual(response["status"], "registered")
            registered_devices.append(device_config["device_id"])

        # Verify all devices registered
        connected_devices = self.network_server.get_connected_devices()
        self.assertEqual(len(connected_devices), 3)

        for device_id in registered_devices:
            device_info = self.network_server.get_device_info(device_id)
            self.assertIsNotNone(device_info)

    def test_coordinated_multi_modal_session(self) -> Any:
        """Test coordinated multi-modal recording session across devices"""
        self.network_server.start()
        self.data_aggregator.initialize()

        # Register devices
        self._register_test_devices()

        # Step 1: Create coordinated session
        session_config = {
            "session_name": "Integration_Test_Session",
            "participant_id": "P001",
            "study_protocol": "multi_modal_stress_test",
            "devices": [d["device_id"] for d in self.test_devices],
            "sync_mode": "strict",
            "recording_duration": 300,  # 5 minutes
        }

        session_id = self.session_manager.create_session(session_config)
        self.assertIsNotNone(session_id)

        # Step 2: Coordinate device preparation
        preparation_results = {}
        for device in self.test_devices:
            device_id = device["device_id"]
            prep_msg = {
                "type": "session_preparation",
                "session_id": session_id,
                "device_role": "primary" if device_id == "ANDROID_001" else "secondary",
                "sensors_config": self._get_sensor_config(device["capabilities"]),
            }

            # Simulate device acknowledgment
            prep_response = self._simulate_device_response(device_id, prep_msg)
            preparation_results[device_id] = prep_response["status"] == "ready"

        # Verify all devices are prepared
        self.assertTrue(
            all(preparation_results.values()),
            "All devices should be prepared for recording",
        )

        # Step 3: Start coordinated recording
        start_timestamp = time.time_ns()

        start_command = {
            "type": "coordinated_start",
            "session_id": session_id,
            "sync_timestamp": start_timestamp,
            "countdown_ms": 3000,  # 3-second countdown
        }

        # Send start command to all devices
        start_results = {}
        for device in self.test_devices:
            device_id = device["device_id"]
            start_response = self._simulate_device_response(device_id, start_command)
            start_results[device_id] = start_response["status"] == "recording"

        self.assertTrue(
            all(start_results.values()),
            "All devices should start recording successfully",
        )

        # Step 4: Simulate experimental events with sync markers
        experimental_events = [
            {"time_offset": 1.0, "event": "baseline_start", "type": "protocol"},
            {
                "time_offset": 30.0,
                "event": "stimulus_1",
                "type": "visual",
                "intensity": 0.8,
            },
            {"time_offset": 35.0, "event": "response_window_1", "type": "input"},
            {
                "time_offset": 60.0,
                "event": "stimulus_2",
                "type": "auditory",
                "frequency": 440,
            },
            {"time_offset": 65.0, "event": "response_window_2", "type": "input"},
            {"time_offset": 90.0, "event": "baseline_end", "type": "protocol"},
        ]

        for event in experimental_events:
            time.sleep(event["time_offset"] / 10)  # Accelerated for testing

            sync_marker = {
                "type": "sync_marker",
                "id": f"EVENT_{event['event']}",
                "timestamp": time.time_ns(),
                "metadata": {k: v for k, v in event.items() if k != "time_offset"},
            }

            # Distribute sync marker to all devices
            distribution_success = self.network_server.distribute_sync_marker(
                sync_marker
            )
            self.assertTrue(
                distribution_success,
                f"Sync marker for {event['event']} should be distributed",
            )

            # Record in data aggregator
            self.data_aggregator.add_sync_marker(session_id, sync_marker)

        # Step 5: Stop coordinated recording
        stop_command = {
            "type": "coordinated_stop",
            "session_id": session_id,
            "final_sync_timestamp": time.time_ns(),
        }

        stop_results = {}
        for device in self.test_devices:
            device_id = device["device_id"]
            stop_response = self._simulate_device_response(device_id, stop_command)
            stop_results[device_id] = stop_response["status"] == "stopped"

        self.assertTrue(
            all(stop_results.values()), "All devices should stop recording successfully"
        )

        # Step 6: Verify session data integrity
        session_data = self.session_manager.get_session_data(session_id)
        self.assertIsNotNone(session_data)
        self.assertEqual(len(session_data["sync_markers"]), len(experimental_events))

        # Verify temporal alignment
        sync_markers = session_data["sync_markers"]
        for i in range(1, len(sync_markers)):
            time_diff = sync_markers[i]["timestamp"] - sync_markers[i - 1]["timestamp"]
            self.assertGreater(
                time_diff, 0, "Sync markers should be temporally ordered"
            )

    def test_file_transfer_coordination(self) -> Any:
        """Test coordinated file transfer from multiple devices"""
        self.network_server.start()
        self.data_aggregator.initialize()
        self._register_test_devices()

        # Create test session with recorded data
        session_id = self.session_manager.create_session(
            {"session_name": "FileTransfer_Test", "participant_id": "P001"}
        )

        # Simulate recorded files from each device
        test_files = {}
        for device in self.test_devices:
            device_id = device["device_id"]
            device_files = []

            # Generate test files based on device capabilities
            for capability in device["capabilities"]:
                filename = f"{capability}_data_{device_id}_{session_id}.csv"
                file_content = self._generate_test_file_content(capability)
                device_files.append(
                    {
                        "filename": filename,
                        "content": file_content,
                        "size": len(file_content.encode()),
                        "checksum": hash(file_content),
                    }
                )

            test_files[device_id] = device_files

        # Coordinate file transfer requests
        transfer_requests = []
        for device_id, files in test_files.items():
            for file_info in files:
                transfer_request = {
                    "type": "file_transfer_request",
                    "device_id": device_id,
                    "session_id": session_id,
                    "filename": file_info["filename"],
                    "size": file_info["size"],
                    "checksum": file_info["checksum"],
                }

                response = self.network_server._handle_file_transfer_request(
                    transfer_request
                )
                self.assertEqual(response["status"], "ready")

                transfer_requests.append(
                    {
                        "request": transfer_request,
                        "response": response,
                        "content": file_info["content"],
                    }
                )

        # Simulate file transfers
        successful_transfers = 0
        for transfer in transfer_requests:
            # Simulate chunked transfer
            content = transfer["content"].encode()
            chunk_size = transfer["response"]["chunk_size"]

            for offset in range(0, len(content), chunk_size):
                chunk = content[offset : offset + chunk_size]

                chunk_msg = {
                    "type": "file_chunk",
                    "transfer_id": transfer["response"]["transfer_id"],
                    "offset": offset,
                    "size": len(chunk),
                    "data": chunk.hex(),  # Hex encode for JSON
                }

                chunk_response = self.network_server._handle_file_chunk(chunk_msg)
                self.assertEqual(chunk_response["status"], "received")

            # Finalize transfer
            finalize_msg = {
                "type": "file_transfer_complete",
                "transfer_id": transfer["response"]["transfer_id"],
            }

            final_response = self.network_server._handle_file_transfer_complete(
                finalize_msg
            )
            if final_response["status"] == "completed":
                successful_transfers += 1

        # Verify all files transferred successfully
        expected_file_count = sum(len(files) for files in test_files.values())
        self.assertEqual(
            successful_transfers,
            expected_file_count,
            "All files should transfer successfully",
        )

        # Verify files are available in data aggregator
        aggregated_files = self.data_aggregator.get_session_files(session_id)
        self.assertEqual(len(aggregated_files), expected_file_count)

    def test_real_time_monitoring_and_quality_assurance(self) -> Any:
        """Test real-time monitoring and quality assurance during recording"""
        self.network_server.start()
        self.data_aggregator.initialize()
        self._register_test_devices()

        # Enable real-time monitoring
        monitoring_config = {
            "sync_accuracy_threshold_ms": 5.0,
            "data_loss_threshold_percent": 1.0,
            "network_latency_threshold_ms": 50.0,
            "quality_check_interval_s": 10.0,
        }

        self.network_server.enable_real_time_monitoring(monitoring_config)

        # Create and start session
        session_id = self.session_manager.create_session(
            {"session_name": "QualityMonitoring_Test", "participant_id": "P001"}
        )

        # Simulate recording with quality reports
        quality_reports = []
        simulation_duration = 30  # seconds

        for second in range(simulation_duration):
            time.sleep(0.1)  # Accelerated simulation

            for device in self.test_devices:
                device_id = device["device_id"]

                # Generate realistic quality metrics
                quality_report = {
                    "type": "quality_report",
                    "device_id": device_id,
                    "session_id": session_id,
                    "timestamp": time.time_ns(),
                    "metrics": {
                        "sync_accuracy_ms": 2.5
                        + (second % 3) * 0.5,  # Slight variation
                        "network_latency_ms": 15.0 + (second % 5) * 2.0,
                        "data_loss_rate": 0.001 * (1 + second % 2),
                        "sensor_data_rate_hz": {
                            sensor: 10.0 + (second % 4) * 0.5
                            for sensor in device["capabilities"]
                        },
                    },
                }

                # Process quality report
                processing_result = self.network_server._process_quality_report(
                    quality_report
                )
                self.assertTrue(
                    processing_result,
                    f"Quality report should be processed for {device_id}",
                )

                quality_reports.append(quality_report)

        # Analyze quality trends
        quality_stats = self.network_server.get_quality_statistics_summary()

        self.assertIsNotNone(quality_stats)
        self.assertIn("overall_sync_accuracy", quality_stats)
        self.assertIn("network_performance", quality_stats)
        self.assertIn("data_integrity", quality_stats)

        # Verify quality meets requirements
        self.assertLessEqual(
            quality_stats["overall_sync_accuracy"]["average_ms"],
            5.0,
            "Average sync accuracy should meet 5ms requirement",
        )
        self.assertGreaterEqual(
            quality_stats["data_integrity"]["completeness"],
            0.99,
            "Data completeness should be >99%",
        )

    def test_error_recovery_and_resilience(self) -> Any:
        """Test system error recovery and resilience mechanisms"""
        self.network_server.start()
        self.data_aggregator.initialize()
        self._register_test_devices()

        # Create test session
        session_id = self.session_manager.create_session(
            {"session_name": "ErrorRecovery_Test", "participant_id": "P001"}
        )

        # Test scenarios
        error_scenarios = [
            {
                "name": "device_disconnection",
                "description": "Device disconnects mid-session",
                "device_id": "ANDROID_002",
                "recovery_expected": True,
            },
            {
                "name": "network_congestion",
                "description": "High network latency simulation",
                "affected_devices": ["ANDROID_001", "ANDROID_003"],
                "recovery_expected": True,
            },
            {
                "name": "corrupted_sync_marker",
                "description": "Corrupted sync marker data",
                "corruption_type": "invalid_json",
                "recovery_expected": True,
            },
        ]

        recovery_results = {}

        for scenario in error_scenarios:
            scenario_name = scenario["name"]

            # Inject error condition
            if scenario_name == "device_disconnection":
                # Simulate device disconnection
                device_id = scenario["device_id"]
                self.network_server._simulate_device_disconnect(device_id)

                # Wait for detection
                time.sleep(2)

                # Attempt recovery (device reconnection)
                reconnection_success = self.network_server._simulate_device_reconnect(
                    device_id
                )
                recovery_results[scenario_name] = reconnection_success

            elif scenario_name == "network_congestion":
                # Simulate high latency
                original_latency = self.network_server.get_average_latency()
                self.network_server._inject_latency_simulation(high_latency_ms=200)

                # Check if system adapts
                time.sleep(3)

                # Remove latency simulation
                self.network_server._remove_latency_simulation()
                adapted_latency = self.network_server.get_average_latency()

                recovery_results[scenario_name] = (
                    adapted_latency < original_latency * 1.5
                )

            elif scenario_name == "corrupted_sync_marker":
                # Send corrupted sync marker
                corrupted_marker = "invalid json data"

                processing_result = self.network_server._process_raw_message(
                    corrupted_marker, Mock()
                )

                # Should handle gracefully without crashing
                recovery_results[scenario_name] = processing_result is False

        # Verify recovery mechanisms worked
        for scenario in error_scenarios:
            if scenario["recovery_expected"]:
                self.assertTrue(
                    recovery_results[scenario["name"]],
                    f"Recovery should succeed for {scenario['name']}",
                )

    def test_performance_under_load(self) -> Any:
        """Test system performance under various load conditions"""
        self.network_server.start()
        self.data_aggregator.initialize()

        # Register larger number of devices for load testing
        load_test_devices = []
        for i in range(10):  # 10 devices for load test
            device = {
                "device_id": f"LOAD_DEVICE_{i:02d}",
                "device_type": "android_spoke",
                "capabilities": ["rgb", "thermal", "gsr"],
                "location": f"192.168.1.{100 + i}",
            }
            load_test_devices.append(device)

            # Register device
            mock_socket = Mock()
            registration_msg = {"type": "device_registration", **device}
            self.network_server._handle_device_registration(
                registration_msg, mock_socket
            )

        # Performance test scenarios
        load_scenarios = [
            {"name": "high_sync_rate", "sync_markers_per_sec": 50, "duration_sec": 10},
            {
                "name": "many_devices",
                "devices_count": 10,
                "sync_rate": 10,
                "duration_sec": 15,
            },
            {
                "name": "large_messages",
                "message_size_kb": 100,
                "message_rate": 5,
                "duration_sec": 10,
            },
        ]

        performance_results = {}

        for scenario in load_scenarios:
            start_time = time.time()
            scenario_name = scenario["name"]

            if scenario_name == "high_sync_rate":
                # High frequency sync markers
                sync_count = 0
                target_count = (
                    scenario["sync_markers_per_sec"] * scenario["duration_sec"]
                )

                for i in range(target_count):
                    sync_marker = {
                        "type": "sync_marker",
                        "id": f"LOAD_SYNC_{i}",
                        "timestamp": time.time_ns(),
                    }

                    success = self.network_server.distribute_sync_marker(sync_marker)
                    if success:
                        sync_count += 1

                    time.sleep(1.0 / scenario["sync_markers_per_sec"])

                performance_results[scenario_name] = {
                    "success_rate": sync_count / target_count,
                    "actual_rate": sync_count / scenario["duration_sec"],
                }

            elif scenario_name == "many_devices":
                # Messages from many devices simultaneously
                message_count = 0
                total_messages = (
                    scenario["devices_count"]
                    * scenario["sync_rate"]
                    * scenario["duration_sec"]
                )

                # Use threading to simulate concurrent device messages
                def send_device_messages(device_id) -> Any:
                    nonlocal message_count
                    for i in range(scenario["sync_rate"] * scenario["duration_sec"]):
                        message = {
                            "type": "status_update",
                            "device_id": device_id,
                            "recording": True,
                            "timestamp": time.time_ns(),
                        }

                        success = self.network_server._process_device_message(message)
                        if success:
                            message_count += 1

                        time.sleep(1.0 / scenario["sync_rate"])

                threads = []
                for device in load_test_devices:
                    thread = threading.Thread(
                        target=send_device_messages, args=(device["device_id"],)
                    )
                    threads.append(thread)
                    thread.start()

                for thread in threads:
                    thread.join()

                performance_results[scenario_name] = {
                    "success_rate": message_count / total_messages,
                    "messages_per_sec": message_count / scenario["duration_sec"],
                }

            end_time = time.time()
            performance_results[scenario_name]["actual_duration"] = (
                end_time - start_time
            )

        # Verify performance requirements
        for scenario_name, results in performance_results.items():
            self.assertGreaterEqual(
                results["success_rate"],
                0.95,
                f"Success rate should be >95% for {scenario_name}",
            )

    # Helper methods
    def _register_test_devices(self):
        """Register all test devices with the network server"""
        for device in self.test_devices:
            mock_socket = Mock()
            registration_msg = {"type": "device_registration", **device}
            response = self.network_server._handle_device_registration(
                registration_msg, mock_socket
            )
            self.assertEqual(response["status"], "registered")

    def _get_sensor_config(self, capabilities):
        """Generate sensor configuration based on device capabilities"""
        config = {}
        for capability in capabilities:
            if capability == "rgb":
                config["rgb"] = {"resolution": "1080p", "fps": 30, "format": "mp4"}
            elif capability == "thermal":
                config["thermal"] = {
                    "resolution": "160x120",
                    "fps": 10,
                    "format": "csv",
                }
            elif capability == "gsr":
                config["gsr"] = {"sampling_rate": 10, "format": "csv"}
        return config

    def _simulate_device_response(self, device_id, message):
        """Simulate device response to Hub commands"""
        response = {
            "type": f"{message['type']}_response",
            "device_id": device_id,
            "status": "success",
            "timestamp": time.time_ns(),
        }

        # Customize response based on message type
        if message["type"] == "session_preparation":
            response["status"] = "ready"
        elif message["type"] == "coordinated_start":
            response["status"] = "recording"
        elif message["type"] == "coordinated_stop":
            response["status"] = "stopped"

        return response

    def _generate_test_file_content(self, sensor_type):
        """Generate realistic test file content for different sensor types"""
        if sensor_type == "gsr":
            content = "timestamp,gsr_microsiemens,raw_adc\n"
            for i in range(100):
                timestamp = 1000000000 + i * 100000
                gsr_value = 25.0 + 5 * (i % 10) / 10
                adc_value = 2048 + int(100 * (i % 10) / 10)
                content += f"{timestamp},{gsr_value},{adc_value}\n"

        elif sensor_type == "thermal":
            content = "timestamp,avg_temp,min_temp,max_temp\n"
            for i in range(100):
                timestamp = 1000000000 + i * 100000
                avg_temp = 25.0 + 3 * (i % 8) / 8
                min_temp = avg_temp - 1.0
                max_temp = avg_temp + 1.0
                content += f"{timestamp},{avg_temp},{min_temp},{max_temp}\n"

        elif sensor_type == "rgb":
            # Simulate metadata file for RGB video
            content = "timestamp,frame_number,exposure_ms,iso\n"
            for i in range(100):
                timestamp = 1000000000 + i * 33333  # 30 fps
                frame_num = i + 1
                exposure = 33.3
                iso = 100
                content += f"{timestamp},{frame_num},{exposure},{iso}\n"

        else:
            content = "timestamp,value\n"
            for i in range(100):
                timestamp = 1000000000 + i * 100000
                value = i * 0.1
                content += f"{timestamp},{value}\n"

        return content


if __name__ == "__main__":
    unittest.main(verbosity=2)
