#!/usr/bin/env python3
"""
Test 3: Edge-case Command Handling Log Test

This test stress-tests the command parser with out-of-order or invalid
commands to ensure the system handles them gracefully without crashing.

Test scenarios:
1. Send STOP when no recording is active
2. Send duplicate START commands rapidly
3. Send burst of SYNC requests back-to-back
4. Send invalid/malformed commands
5. Send commands with missing parameters
6. Send commands in wrong sequence

Output: System log detailing how each unexpected command is handled.

Subsystem: Command handling logic
Chapters: Chapter 5 (stability under unusual command sequences)
          Chapter 6 (reliability and error-handling conclusions)
"""

import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'pc-controller'))

from command_client import CommandClient

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class EdgeCaseCommandTest:
    """Test edge-case command handling"""

    def __init__(self, device_ip: str, port: int = 8080):
        self.device_ip = device_ip
        self.port = port
        self.client = CommandClient(timeout=15)
        self.test_results: List[Dict] = []

    def execute_test_case(self, test_name: str, test_func) -> Dict:
        """Execute a test case and record results"""
        logger.info(f"\n{'=' * 80}")
        logger.info(f"Test Case: {test_name}")
        logger.info(f"{'=' * 80}")

        start_time = datetime.now()

        try:
            result = test_func()
            end_time = datetime.now()

            test_result = {
                'test_name': test_name,
                'start_time': start_time.isoformat(),
                'end_time': end_time.isoformat(),
                'duration_seconds': (end_time - start_time).total_seconds(),
                'success': True,
                'result': result,
                'error': None
            }
        except Exception as e:
            end_time = datetime.now()
            logger.error(f"Test case failed with exception: {e}")

            test_result = {
                'test_name': test_name,
                'start_time': start_time.isoformat(),
                'end_time': end_time.isoformat(),
                'duration_seconds': (end_time - start_time).total_seconds(),
                'success': False,
                'result': None,
                'error': str(e)
            }

        self.test_results.append(test_result)
        return test_result

    def test_stop_without_active_session(self, device_id: str) -> Dict:
        """Test sending STOP when no recording is active"""
        logger.info("Sending STOP command when no session is active")

        response = self.client.send_command(device_id, 'STOP', {
            'session_id': 'nonexistent_session'
        })

        result = {
            'command': 'STOP',
            'scenario': 'No active session',
            'response': response,
            'expected_behavior': 'Command should be ignored or return error',
            'actual_behavior': 'Received response: ' + str(response)
        }

        logger.info(f"Response: {response}")

        if response and response.startswith('ERROR'):
            result['handled_correctly'] = True
            logger.info("System correctly rejected STOP without active session")
        elif response and 'STOP-ACK' in response:
            result['handled_correctly'] = False
            logger.warning("System accepted STOP without active session (unexpected)")
        else:
            result['handled_correctly'] = 'unknown'
            logger.warning("Unclear response from system")

        return result

    def test_duplicate_start_commands(self, device_id: str) -> Dict:
        """Test sending duplicate START commands rapidly"""
        logger.info("Sending duplicate START commands rapidly")

        session_id = f"duplicate_test_{int(time.time())}"
        responses = []

        # Send first START
        logger.info("Sending first START command")
        response1 = self.client.send_command(device_id, 'START', {
            'session_id': session_id
        })
        responses.append({'attempt': 1, 'response': response1})
        logger.info(f"First START response: {response1}")

        # Send second START immediately
        time.sleep(0.1)
        logger.info("Sending second START command (duplicate)")
        response2 = self.client.send_command(device_id, 'START', {
            'session_id': session_id
        })
        responses.append({'attempt': 2, 'response': response2})
        logger.info(f"Second START response: {response2}")

        # Send third START
        time.sleep(0.1)
        logger.info("Sending third START command (duplicate)")
        response3 = self.client.send_command(device_id, 'START', {
            'session_id': session_id
        })
        responses.append({'attempt': 3, 'response': response3})
        logger.info(f"Third START response: {response3}")

        # Clean up - send STOP
        time.sleep(0.5)
        logger.info("Cleaning up - sending STOP")
        stop_response = self.client.send_command(device_id, 'STOP', {
            'session_id': session_id
        })

        result = {
            'command': 'START (duplicate)',
            'scenario': 'Multiple START commands for same session',
            'responses': responses,
            'cleanup_response': stop_response,
            'expected_behavior': 'First should succeed, subsequent should be rejected or ignored',
            'actual_behavior': f'Got {len(responses)} responses'
        }

        # Check if system handled duplicates correctly
        if response2 and ('ERROR' in response2 or 'ALREADY' in response2):
            result['handled_correctly'] = True
            logger.info("System correctly rejected duplicate START commands")
        else:
            result['handled_correctly'] = False
            logger.warning("System may not have properly handled duplicate START commands")

        return result

    def test_sync_burst(self, device_id: str) -> Dict:
        """Test burst of SYNC requests back-to-back"""
        logger.info("Sending burst of SYNC requests")

        sync_count = 10
        responses = []

        burst_start = time.time_ns()

        for i in range(sync_count):
            response = self.client.send_command(device_id, 'SYNC', {
                't_pc': int(time.time() * 1000)
            })
            responses.append({
                'iteration': i + 1,
                'response': response,
                'timestamp': datetime.now().isoformat()
            })
            # No delay - send as fast as possible

        burst_end = time.time_ns()
        burst_duration_ms = (burst_end - burst_start) / 1e6

        successful = sum(1 for r in responses if r['response'] is not None)

        result = {
            'command': 'SYNC (burst)',
            'scenario': f'{sync_count} SYNC commands back-to-back',
            'burst_duration_ms': burst_duration_ms,
            'total_commands': sync_count,
            'successful_responses': successful,
            'failed_responses': sync_count - successful,
            'responses': responses,
            'expected_behavior': 'All commands should be processed or queued',
            'actual_behavior': f'{successful}/{sync_count} successful'
        }

        if successful == sync_count:
            result['handled_correctly'] = True
            logger.info(f"All {sync_count} SYNC commands processed successfully")
        else:
            result['handled_correctly'] = False
            logger.warning(f"Only {successful}/{sync_count} SYNC commands succeeded")

        return result

    def test_invalid_commands(self, device_id: str) -> Dict:
        """Test sending invalid/malformed commands"""
        logger.info("Sending invalid/malformed commands")

        test_cases = []

        # Test 1: Unknown command
        logger.info("Test: Unknown command")
        response = self.client.send_command(device_id, 'INVALID_COMMAND')
        test_cases.append({
            'command': 'INVALID_COMMAND',
            'description': 'Unknown command',
            'response': response,
            'handled_correctly': response is not None and 'ERROR' in response
        })

        # Test 2: Empty command
        logger.info("Test: Empty command")
        response = self.client.send_command(device_id, '')
        test_cases.append({
            'command': '',
            'description': 'Empty command',
            'response': response,
            'handled_correctly': response is not None and response.startswith('ERROR')
        })

        # Test 3: Command with special characters
        logger.info("Test: Command with special characters")
        response = self.client.send_command(device_id, 'START@#$%')
        test_cases.append({
            'command': 'START@#$%',
            'description': 'Command with special characters',
            'response': response,
            'handled_correctly': response is not None
        })

        result = {
            'command': 'Various invalid commands',
            'scenario': 'Testing malformed/invalid command handling',
            'test_cases': test_cases,
            'expected_behavior': 'System should reject or handle gracefully without crashing',
            'total_tests': len(test_cases),
            'handled_correctly_count': sum(1 for tc in test_cases if tc['handled_correctly'])
        }

        logger.info(
            f"Invalid command tests: {result['handled_correctly_count']}/{result['total_tests']} handled correctly")

        return result

    def test_wrong_sequence(self, device_id: str) -> Dict:
        """Test commands in wrong sequence"""
        logger.info("Testing commands in wrong sequence")

        sequence_log = []
        validation_results = []

        # Try to STOP before START
        logger.info("1. Sending STOP before any START")
        response = self.client.send_command(device_id, 'STOP')

        # Validate: Should get error or indicate no active session
        stop_before_start_valid = response and (
                    'ERROR' in response or 'not active' in response.lower() or 'no session' in response.lower())
        validation_results.append({
            'test': 'STOP before START',
            'expected': 'Error response or indication of no active session',
            'passed': stop_before_start_valid
        })

        sequence_log.append({
            'step': 1,
            'action': 'STOP before START',
            'response': response,
            'validation_passed': stop_before_start_valid,
            'timestamp': datetime.now().isoformat()
        })

        time.sleep(0.2)

        # Start a session
        session_id = f"sequence_test_{int(time.time())}"
        logger.info("2. Sending START to begin session")
        response = self.client.send_command(device_id, 'START', {
            'session_id': session_id
        })

        # Validate: Should get acknowledgment
        start_valid = response and ('ACK' in response or 'START' in response)
        validation_results.append({
            'test': 'START session',
            'expected': 'Acknowledgment of session start',
            'passed': start_valid
        })

        sequence_log.append({
            'step': 2,
            'action': 'START session',
            'response': response,
            'validation_passed': start_valid,
            'timestamp': datetime.now().isoformat()
        })

        time.sleep(0.2)

        # Try to START again while recording
        logger.info("3. Sending START again while session is active")
        response = self.client.send_command(device_id, 'START', {
            'session_id': f"another_session_{int(time.time())}"
        })

        # Validate: Should get error indicating already recording
        duplicate_start_valid = response and (
                    'ERROR' in response or 'already' in response.lower() or 'active' in response.lower())
        validation_results.append({
            'test': 'Duplicate START',
            'expected': 'Error response indicating session already active',
            'passed': duplicate_start_valid
        })

        sequence_log.append({
            'step': 3,
            'action': 'START while already recording',
            'response': response,
            'validation_passed': duplicate_start_valid,
            'timestamp': datetime.now().isoformat()
        })

        time.sleep(0.2)

        # Stop the session
        logger.info("4. Sending STOP to end session")
        response = self.client.send_command(device_id, 'STOP', {
            'session_id': session_id
        })

        # Validate: Should get acknowledgment
        stop_valid = response and ('ACK' in response or 'STOP' in response)
        validation_results.append({
            'test': 'STOP session',
            'expected': 'Acknowledgment of session stop',
            'passed': stop_valid
        })

        sequence_log.append({
            'step': 4,
            'action': 'STOP session',
            'response': response,
            'validation_passed': stop_valid,
            'timestamp': datetime.now().isoformat()
        })

        time.sleep(0.2)

        # Try to STOP again
        logger.info("5. Sending STOP again after session ended")
        response = self.client.send_command(device_id, 'STOP', {
            'session_id': session_id
        })

        # Validate: Should get error or indicate no active session
        duplicate_stop_valid = response and (
                    'ERROR' in response or 'not active' in response.lower() or 'no session' in response.lower())
        validation_results.append({
            'test': 'STOP after session ended',
            'expected': 'Error response or indication of no active session',
            'passed': duplicate_stop_valid
        })

        sequence_log.append({
            'step': 5,
            'action': 'STOP after session ended',
            'response': response,
            'validation_passed': duplicate_stop_valid,
            'timestamp': datetime.now().isoformat()
        })

        # Calculate validation summary
        total_validations = len(validation_results)
        passed_validations = sum(1 for v in validation_results if v['passed'])

        result = {
            'command': 'Sequence test',
            'scenario': 'Commands in wrong sequence',
            'sequence_log': sequence_log,
            'validation_results': validation_results,
            'validation_summary': {
                'total': total_validations,
                'passed': passed_validations,
                'failed': total_validations - passed_validations
            },
            'expected_behavior': 'System should handle out-of-sequence commands gracefully with appropriate error responses',
            'actual_behavior': f'{passed_validations}/{total_validations} validations passed'
        }

        logger.info(f"Sequence validation: {passed_validations}/{total_validations} tests passed")

        return result

    def run_test(self) -> Dict:
        """Execute all edge-case tests"""
        logger.info("=" * 80)
        logger.info("TEST 3: Edge-case Command Handling Log Test")
        logger.info("=" * 80)

        test_start_time = datetime.now()

        # Connect to device
        logger.info(f"\nConnecting to device {self.device_ip}:{self.port}")
        if not self.client.connect_to_device(self.device_ip, self.port):
            return self._generate_report(
                success=False,
                error=f"Could not connect to {self.device_ip}:{self.port}"
            )

        device_id = f"{self.device_ip}:{self.port}"
        logger.info("Connected successfully\n")

        # Run test cases
        self.execute_test_case(
            "Test 1: STOP without active session",
            lambda: self.test_stop_without_active_session(device_id)
        )

        time.sleep(1)

        self.execute_test_case(
            "Test 2: Duplicate START commands",
            lambda: self.test_duplicate_start_commands(device_id)
        )

        time.sleep(1)

        self.execute_test_case(
            "Test 3: SYNC command burst",
            lambda: self.test_sync_burst(device_id)
        )

        time.sleep(1)

        self.execute_test_case(
            "Test 4: Invalid/malformed commands",
            lambda: self.test_invalid_commands(device_id)
        )

        time.sleep(1)

        self.execute_test_case(
            "Test 5: Commands in wrong sequence",
            lambda: self.test_wrong_sequence(device_id)
        )

        # Disconnect
        self.client.disconnect_device(device_id)

        test_end_time = datetime.now()
        total_duration = (test_end_time - test_start_time).total_seconds()

        return self._generate_report(success=True, total_duration=total_duration)

    def _generate_report(self, success: bool, error: str = None,
                         total_duration: float = None) -> Dict:
        """Generate test report"""
        report = {
            'test_name': 'Edge-case Command Handling Log Test',
            'test_id': 'test_3',
            'timestamp': datetime.now().isoformat(),
            'success': success,
            'test_results': self.test_results,
            'summary': self._generate_summary()
        }

        if error:
            report['error'] = error

        if total_duration:
            report['total_duration_seconds'] = total_duration

        return report

    def _generate_summary(self) -> Dict:
        """Generate summary from test results"""
        summary = {
            'total_test_cases': len(self.test_results),
            'successful_test_cases': sum(1 for t in self.test_results if t['success']),
            'failed_test_cases': sum(1 for t in self.test_results if not t['success'])
        }

        return summary

    def save_report(self, report: Dict, output_file: str = None):
        """Save test report to JSON file"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"test_3_edge_cases_report_{timestamp}.json"

        output_path = Path(__file__).parent / output_file

        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)

        logger.info(f"\nReport saved to: {output_path}")

        # Create readable log format
        log_file = output_path.with_suffix('.txt')
        with open(log_file, 'w') as f:
            f.write("=" * 100 + "\n")
            f.write("TEST 3: Edge-case Command Handling Log Test - Results\n")
            f.write("=" * 100 + "\n\n")

            for test_result in report['test_results']:
                f.write(f"\nTest Case: {test_result['test_name']}\n")
                f.write(f"Start Time: {test_result['start_time']}\n")
                f.write(f"End Time: {test_result['end_time']}\n")
                f.write(f"Duration: {test_result['duration_seconds']:.2f} seconds\n")
                f.write(f"Success: {test_result['success']}\n")

                if test_result['error']:
                    f.write(f"Error: {test_result['error']}\n")

                if test_result['result']:
                    f.write("\nResult Details:\n")
                    f.write(json.dumps(test_result['result'], indent=2))
                    f.write("\n")

                f.write("-" * 100 + "\n")

            f.write("\n" + "=" * 100 + "\n")
            f.write("Summary\n")
            f.write("=" * 100 + "\n")
            f.write(json.dumps(report['summary'], indent=2))
            f.write("\n")

        logger.info(f"Readable log saved to: {log_file}")


def main():
    """Main entry point for test"""
    import argparse

    parser = argparse.ArgumentParser(
        description='Test 3: Edge-case Command Handling Log Test'
    )
    parser.add_argument(
        '--device-ip',
        required=True,
        help='IP address of Android device'
    )
    parser.add_argument(
        '--port',
        type=int,
        default=8080,
        help='Port number (default: 8080)'
    )
    parser.add_argument(
        '--output',
        help='Output file name for report (optional)'
    )

    args = parser.parse_args()

    # Run test
    test = EdgeCaseCommandTest(args.device_ip, args.port)
    report = test.run_test()

    # Save report
    test.save_report(report, args.output)

    # Print summary
    logger.info("\n" + "=" * 80)
    logger.info("TEST RESULTS")
    logger.info("=" * 80)
    logger.info(f"Success: {report['success']}")

    if report['success']:
        summary = report['summary']
        logger.info(f"Total Test Cases: {summary['total_test_cases']}")
        logger.info(f"Successful: {summary['successful_test_cases']}")
        logger.info(f"Failed: {summary['failed_test_cases']}")
    else:
        logger.error(f"Error: {report.get('error', 'Unknown error')}")
        sys.exit(1)

    logger.info("\nTest completed successfully!")


if __name__ == "__main__":
    main()
