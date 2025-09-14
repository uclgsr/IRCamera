#!/usr/bin/env python3
"""
Comprehensive validation script for PC-to-Phone communication testing.

This script automates all hardware testing phases and generates a detailed validation report.
"""

import argparse
import json
import logging
import socket
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Tuple


class ValidationReport:
    """Collects and formats validation test results"""

    def __init__(self):
        self.tests = []
        self.start_time = datetime.now()
        self.device_info = {}

    def add_test(self, name: str, result: bool, duration: float, details: str = ""):
        """Add a test result to the report"""
        self.tests.append(
            {
                "name": name,
                "result": result,
                "duration": duration,
                "details": details,
                "timestamp": datetime.now(),
            }
        )

    def set_device_info(self, info: Dict[str, Any]):
        """Set device information for the report"""
        self.device_info = info

    def generate_report(self) -> str:
        """Generate a comprehensive validation report"""
        total_tests = len(self.tests)
        passed_tests = sum(1 for t in self.tests if t["result"])
        failed_tests = total_tests - passed_tests

        report = """
# PC-to-Phone Communication Validation Report

**Generated**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**Test Duration**: {(datetime.now() - self.start_time).total_seconds():.1f} seconds

## Test Environment

**Device Information**:
- Android IP: {self.device_info.get('ip', 'Unknown')}
- Port: {self.device_info.get('port', 8080)}
- Connection Type: {self.device_info.get('connection_type', 'WiFi')}

## Test Summary

- **Total Tests**: {total_tests}
- **Passed**: {passed_tests} ✅
- **Failed**: {failed_tests} ❌
- **Success Rate**: {(passed_tests/total_tests*100):.1f}%

## Detailed Test Results

"""

        for test in self.tests:
            status = "✅ PASS" if test["result"] else "❌ FAIL"
            report += f"### {test['name']}\n"
            report += f"- **Status**: {status}\n"
            report += f"- **Duration**: {test['duration']:.3f}s\n"
            report += f"- **Timestamp**: {test['timestamp'].strftime('%H:%M:%S')}\n"
            if test["details"]:
                report += f"- **Details**: {test['details']}\n"
            report += "\n"

        # Performance analysis
        report += "## Performance Analysis\n\n"
        connection_tests = [t for t in self.tests if "connection" in t["name"].lower()]
        if connection_tests:
            avg_connection_time = sum(t["duration"] for t in connection_tests) / len(
                connection_tests
            )
            report += f"- **Average Connection Time**: {avg_connection_time:.3f}s\n"

        response_tests = [t for t in self.tests if "ping" in t["name"].lower()]
        if response_tests:
            avg_response_time = sum(t["duration"] for t in response_tests) / len(
                response_tests
            )
            report += f"- **Average Response Time**: {avg_response_time:.3f}s\n"

        # Recommendations
        report += "\n## Recommendations\n\n"
        if failed_tests > 0:
            report += "❌ **Issues Found**: Review failed tests and address underlying problems.\n"
        if passed_tests == total_tests:
            report += (
                "✅ **All Tests Passed**: Implementation is ready for production use.\n"
            )

        return report


class ComprehensiveValidator:
    """Comprehensive validation of PC-to-Phone communication"""

    def __init__(self, android_ip: str, port: int = 8080):
        self.android_ip = android_ip
        self.port = port
        self.socket = None
        self.report = ValidationReport()
        self.logger = self._setup_logging()

    def _setup_logging(self) -> logging.Logger:
        """Setup logging for detailed test tracking"""
        logging.basicConfig(
            level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
        )
        return logging.getLogger(__name__)

    def _send_message(self, message: Dict[str, Any]) -> Tuple[bool, str, float]:
        """Send a JSON message and get response with timing"""
        start_time = time.time()
        try:
            # Send message with length prefix
            message_json = json.dumps(message)
            message_bytes = message_json.encode("utf-8")
            length_prefix = len(message_bytes).to_bytes(4, byteorder="big")
            self.socket.send(length_prefix + message_bytes)

            # Receive response
            length_data = self.socket.recv(4)
            if len(length_data) != 4:
                return False, "Invalid response length prefix", time.time() - start_time

            response_length = int.from_bytes(length_data, byteorder="big")
            response_data = self.socket.recv(response_length)

            if len(response_data) != response_length:
                return False, "Incomplete response data", time.time() - start_time

            response = json.loads(response_data.decode("utf-8"))
            duration = time.time() - start_time
            return True, json.dumps(response, indent=2), duration

        except Exception as e:
            duration = time.time() - start_time
            return False, f"Communication error: {str(e)}", duration

    def test_connection(self) -> bool:
        """Test basic TCP connection to Android device"""
        self.logger.info(f"Testing connection to {self.android_ip}:{self.port}")
        start_time = time.time()

        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)
            self.socket.connect((self.android_ip, self.port))

            duration = time.time() - start_time
            self.report.add_test(
                "Basic Connection",
                True,
                duration,
                f"Successfully connected to {self.android_ip}:{self.port}",
            )
            return True

        except Exception as e:
            duration = time.time() - start_time
            self.report.add_test(
                "Basic Connection", False, duration, f"Connection failed: {str(e)}"
            )
            return False

    def test_device_registration(self) -> bool:
        """Test device registration handshake"""
        self.logger.info("Testing device registration")

        registration_message = {
            "message_type": "enhanced_device_registration",
            "device_id": "validation_pc",
            "device_type": "pc_controller",
            "capabilities": ["remote_control", "data_sync"],
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        success, response, duration = self._send_message(registration_message)

        if success:
            try:
                response_data = json.loads(response)
                if response_data.get("message_type") == "enhanced_registration_ack":
                    self.report.add_test(
                        "Device Registration",
                        True,
                        duration,
                        "Registration handshake successful",
                    )
                    return True
                else:
                    self.report.add_test(
                        "Device Registration",
                        False,
                        duration,
                        f"Unexpected response type: {response_data.get('message_type')}",
                    )
                    return False
            except json.JSONDecodeError:
                self.report.add_test(
                    "Device Registration", False, duration, "Invalid JSON response"
                )
                return False
        else:
            self.report.add_test("Device Registration", False, duration, response)
            return False

    def test_ping_pong(self, count: int = 5) -> bool:
        """Test ping/pong communication multiple times"""
        self.logger.info(f"Testing ping/pong communication ({count} iterations)")

        all_successful = True
        total_duration = 0

        for i in range(count):
            ping_message = {
                "message_type": "ping",
                "device_id": "validation_pc",
                "sequence": i,
                "timestamp_ns": int(time.time() * 1_000_000_000),
            }

            success, response, duration = self._send_message(ping_message)
            total_duration += duration

            if success:
                try:
                    response_data = json.loads(response)
                    if response_data.get("message_type") != "pong":
                        all_successful = False
                        break
                except json.JSONDecodeError:
                    all_successful = False
                    break
            else:
                all_successful = False
                break

        avg_duration = total_duration / count
        self.report.add_test(
            f"Ping/Pong Communication ({count}x)",
            all_successful,
            avg_duration,
            f"Average response time: {avg_duration:.3f}s",
        )

        return all_successful

    def test_recording_control(self) -> bool:
        """Test remote recording start/stop control"""
        self.logger.info("Testing recording control")

        # Test recording start
        start_message = {
            "message_type": "session_start_command",
            "session_directory": "/tmp/validation_session",
            "device_id": "validation_pc",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        success, response, duration = self._send_message(start_message)

        if not success:
            self.report.add_test(
                "Recording Start Command",
                False,
                duration,
                f"Start command failed: {response}",
            )
            return False

        self.report.add_test(
            "Recording Start Command",
            True,
            duration,
            "Recording start command sent successfully",
        )

        # Wait a moment, then test recording stop
        time.sleep(2)

        stop_message = {
            "message_type": "session_stop_command",
            "device_id": "validation_pc",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        success, response, duration = self._send_message(stop_message)

        self.report.add_test(
            "Recording Stop Command",
            success,
            duration,
            (
                "Recording stop command sent successfully"
                if success
                else f"Stop command failed: {response}"
            ),
        )

        return success

    def test_sync_markers(self) -> bool:
        """Test sync marker functionality"""
        self.logger.info("Testing sync markers")

        sync_message = {
            "message_type": "sync_marker_command",
            "marker_type": "validation_marker",
            "timestamp_ns": int(time.time() * 1_000_000_000),
            "metadata": {"test_id": "comprehensive_validation", "marker_sequence": 1},
        }

        success, response, duration = self._send_message(sync_message)

        self.report.add_test(
            "Sync Marker Processing",
            success,
            duration,
            (
                "Sync marker sent successfully"
                if success
                else f"Sync marker failed: {response}"
            ),
        )

        return success

    def test_status_request(self) -> bool:
        """Test status request functionality"""
        self.logger.info("Testing status request")

        status_message = {
            "message_type": "status_request",
            "device_id": "validation_pc",
            "timestamp_ns": int(time.time() * 1_000_000_000),
        }

        success, response, duration = self._send_message(status_message)

        if success:
            try:
                response_data = json.loads(response)
                has_status = (
                    "session_stats" in response_data or "sensor_status" in response_data
                )
                self.report.add_test(
                    "Status Request",
                    has_status,
                    duration,
                    (
                        "Status response received with valid data"
                        if has_status
                        else "Status response missing expected fields"
                    ),
                )
                return has_status
            except json.JSONDecodeError:
                self.report.add_test(
                    "Status Request", False, duration, "Invalid JSON response"
                )
                return False
        else:
            self.report.add_test(
                "Status Request", False, duration, f"Status request failed: {response}"
            )
            return False

    def test_stress_communication(self, message_count: int = 20) -> bool:
        """Test communication under stress with multiple rapid messages"""
        self.logger.info(f"Testing stress communication ({message_count} messages)")

        successful_messages = 0
        total_duration = 0

        for i in range(message_count):
            ping_message = {
                "message_type": "ping",
                "device_id": "validation_pc",
                "sequence": i,
                "timestamp_ns": int(time.time() * 1_000_000_000),
            }

            success, response, duration = self._send_message(ping_message)
            total_duration += duration

            if success:
                successful_messages += 1

            # Small delay to avoid overwhelming the system
            time.sleep(0.05)

        success_rate = successful_messages / message_count
        avg_duration = total_duration / message_count

        stress_passed = success_rate >= 0.95  # Allow 5% failure rate for stress test

        self.report.add_test(
            f"Stress Communication ({message_count} messages)",
            stress_passed,
            avg_duration,
            f"Success rate: {success_rate:.1%}, Avg response: {avg_duration:.3f}s",
        )

        return stress_passed

    def run_all_tests(self) -> bool:
        """Run all validation tests in sequence"""
        self.logger.info("Starting comprehensive validation")

        # Set device info for report
        self.report.set_device_info(
            {"ip": self.android_ip, "port": self.port, "connection_type": "WiFi"}
        )

        # Test sequence
        tests = [
            ("Connection", self.test_connection),
            ("Device Registration", self.test_device_registration),
            ("Ping/Pong", lambda: self.test_ping_pong(5)),
            ("Recording Control", self.test_recording_control),
            ("Sync Markers", self.test_sync_markers),
            ("Status Request", self.test_status_request),
            ("Stress Communication", lambda: self.test_stress_communication(20)),
        ]

        overall_success = True

        for test_name, test_func in tests:
            print(f"\n🧪 Running {test_name} test...")
            try:
                result = test_func()
                status = "✅ PASSED" if result else "❌ FAILED"
                print(f"   {status}")
                if not result:
                    overall_success = False
            except Exception as e:
                print(f"   ❌ ERROR: {str(e)}")
                self.logger.error(f"{test_name} test error: {str(e)}")
                overall_success = False

        return overall_success

    def cleanup(self):
        """Clean up resources"""
        if self.socket:
            self.socket.close()

    def get_report(self) -> str:
        """Get the validation report"""
        return self.report.generate_report()


def main():
    parser = argparse.ArgumentParser(
        description="Comprehensive PC-to-Phone communication validation"
    )
    parser.add_argument(
        "--android-ip", required=True, help="IP address of Android device"
    )
    parser.add_argument(
        "--port", type=int, default=8080, help="Port number (default: 8080)"
    )
    parser.add_argument("--output", help="Output file for validation report")
    parser.add_argument("--verbose", action="store_true", help="Enable verbose logging")

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    print("🚀 Starting comprehensive validation of PC-to-Phone communication")
    print(f"📱 Target Android device: {args.android_ip}:{args.port}")
    print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    validator = ComprehensiveValidator(args.android_ip, args.port)

    try:
        overall_success = validator.run_all_tests()

        print("\n📊 Generating validation report...")
        report = validator.get_report()

        if args.output:
            with open(args.output, "w") as f:
                f.write(report)
            print(f"📄 Report saved to: {args.output}")
        else:
            print("\n" + "=" * 80)
            print(report)
            print("=" * 80)

        if overall_success:
            print(
                "\n🎉 All tests passed! PC-to-Phone communication is working correctly."
            )
            sys.exit(0)
        else:
            print("\n❌ Some tests failed. Review the report for details.")
            sys.exit(1)

    except KeyboardInterrupt:
        print("\n⚠️  Validation interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n💥 Validation failed with error: {str(e)}")
        sys.exit(1)
    finally:
        validator.cleanup()


if __name__ == "__main__":
    main()
