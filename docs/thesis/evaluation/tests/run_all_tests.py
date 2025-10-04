#!/usr/bin/env python3
"""
Master Test Runner - Network Command Handling and Control Tests

Runs all thesis evaluation tests in sequence and generates a combined report.
"""

import argparse
import json
import logging
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class MasterTestRunner:
    """Runs all thesis evaluation tests"""

    def __init__(self, device_ip: str, port: int = 8080):
        self.device_ip = device_ip
        self.port = port
        self.test_dir = Path(__file__).parent
        self.test_results: List[Dict] = []
        self.start_time = None
        self.end_time = None

    def run_test(self, test_script: str, test_name: str,
                 additional_args: List[str] = None) -> Dict:
        """Run a single test script and capture results"""
        logger.info("=" * 80)
        logger.info(f"Running {test_name}")
        logger.info("=" * 80)

        test_path = self.test_dir / test_script

        if not test_path.exists():
            logger.error(f"Test script not found: {test_path}")
            return {
                'test_name': test_name,
                'test_script': test_script,
                'success': False,
                'error': 'Test script not found',
                'timestamp': datetime.now().isoformat()
            }

        # Build command
        cmd = [
            sys.executable,
            str(test_path),
            '--device-ip', self.device_ip,
            '--port', str(self.port)
        ]

        if additional_args:
            cmd.extend(additional_args)

        test_start = time.time()

        try:
            # Run test
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300  # 5 minute timeout
            )

            test_end = time.time()
            duration = test_end - test_start

            success = result.returncode == 0

            test_result = {
                'test_name': test_name,
                'test_script': test_script,
                'success': success,
                'return_code': result.returncode,
                'duration_seconds': duration,
                'stdout': result.stdout,
                'stderr': result.stderr,
                'timestamp': datetime.now().isoformat()
            }

            if success:
                logger.info(f"{test_name} completed successfully in {duration:.2f}s")
            else:
                logger.error(f"{test_name} failed with return code {result.returncode}")
                logger.error(f"Error output: {result.stderr}")

            return test_result

        except subprocess.TimeoutExpired as e:
            test_end = time.time()
            duration = test_end - test_start

            logger.error(f"{test_name} timed out after {duration:.2f}s")

            return {
                'test_name': test_name,
                'test_script': test_script,
                'success': False,
                'error': 'Test timed out',
                'duration_seconds': duration,
                'stdout': e.stdout,
                'stderr': e.stderr,
                'timestamp': datetime.now().isoformat()
            }

        except Exception as e:
            test_end = time.time()
            duration = test_end - test_start

            logger.error(f"{test_name} failed with exception: {e}")

            return {
                'test_name': test_name,
                'test_script': test_script,
                'success': False,
                'error': str(e),
                'duration_seconds': duration,
                'timestamp': datetime.now().isoformat()
            }

    def run_all_tests(self, skip_tests: List[str] = None) -> Dict:
        """Run all tests in sequence"""
        logger.info("=" * 80)
        logger.info("THESIS EVALUATION - MASTER TEST RUNNER")
        logger.info("=" * 80)
        logger.info(f"Device IP: {self.device_ip}")
        logger.info(f"Port: {self.port}")
        logger.info("")

        self.start_time = datetime.now()
        skip_tests = skip_tests or []

        # Test 1: Remote Start/Stop
        if 'test_1' not in skip_tests:
            result = self.run_test(
                'test_1_remote_start_stop.py',
                'Test 1: Remote Start/Stop Command Test',
                ['--duration', '10']
            )
            self.test_results.append(result)
            time.sleep(2)  # Pause between tests

        # Test 2: Command Latency and Throughput
        if 'test_2' not in skip_tests:
            result = self.run_test(
                'test_2_command_latency_throughput.py',
                'Test 2: Command Latency and Throughput Metric Test',
                ['--iterations', '10']
            )
            self.test_results.append(result)
            time.sleep(2)

        # Test 3: Edge-case Command Handling
        if 'test_3' not in skip_tests:
            result = self.run_test(
                'test_3_edge_case_commands.py',
                'Test 3: Edge-case Command Handling Log Test'
            )
            self.test_results.append(result)
            time.sleep(2)

        # Test 4: Multi-command Sequence
        if 'test_4' not in skip_tests:
            result = self.run_test(
                'test_4_multi_command_sequence.py',
                'Test 4: Multi-command Sequence Automation Test',
                ['--scenario', 'all']
            )
            self.test_results.append(result)

        self.end_time = datetime.now()

        return self._generate_master_report()

    def _generate_master_report(self) -> Dict:
        """Generate master report from all test results"""
        total_duration = (self.end_time - self.start_time).total_seconds()

        successful = sum(1 for r in self.test_results if r.get('success', False))
        failed = len(self.test_results) - successful

        report = {
            'master_report': True,
            'test_suite': 'Network Command Handling and Control Tests',
            'start_time': self.start_time.isoformat(),
            'end_time': self.end_time.isoformat(),
            'total_duration_seconds': total_duration,
            'device_ip': self.device_ip,
            'port': self.port,
            'summary': {
                'total_tests': len(self.test_results),
                'successful_tests': successful,
                'failed_tests': failed,
                'success_rate': successful / len(self.test_results) * 100 if self.test_results else 0
            },
            'test_results': self.test_results
        }

        return report

    def save_master_report(self, report: Dict):
        """Save master report to file"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        # JSON report
        json_file = self.test_dir / f"master_report_{timestamp}.json"
        with open(json_file, 'w') as f:
            json.dump(report, f, indent=2)

        logger.info(f"\nMaster report saved to: {json_file}")

        # Human-readable report
        text_file = self.test_dir / f"master_report_{timestamp}.txt"
        with open(text_file, 'w') as f:
            f.write("=" * 100 + "\n")
            f.write("THESIS EVALUATION - MASTER TEST REPORT\n")
            f.write("Network Command Handling and Control Tests\n")
            f.write("=" * 100 + "\n\n")

            f.write(f"Start Time: {report['start_time']}\n")
            f.write(f"End Time: {report['end_time']}\n")
            f.write(f"Total Duration: {report['total_duration_seconds']:.2f} seconds\n")
            f.write(f"Device IP: {report['device_ip']}\n")
            f.write(f"Port: {report['port']}\n\n")

            f.write("=" * 100 + "\n")
            f.write("SUMMARY\n")
            f.write("=" * 100 + "\n")
            summary = report['summary']
            f.write(f"Total Tests: {summary['total_tests']}\n")
            f.write(f"Successful: {summary['successful_tests']}\n")
            f.write(f"Failed: {summary['failed_tests']}\n")
            f.write(f"Success Rate: {summary['success_rate']:.1f}%\n\n")

            f.write("=" * 100 + "\n")
            f.write("TEST RESULTS\n")
            f.write("=" * 100 + "\n\n")

            for i, result in enumerate(report['test_results'], 1):
                f.write(f"\n{i}. {result['test_name']}\n")
                f.write("-" * 100 + "\n")
                f.write(f"Test Script: {result['test_script']}\n")
                f.write(f"Success: {result.get('success', False)}\n")
                f.write(f"Duration: {result.get('duration_seconds', 0):.2f} seconds\n")
                f.write(f"Timestamp: {result.get('timestamp', 'N/A')}\n")

                if not result.get('success', False):
                    f.write(f"Return Code: {result.get('return_code', 'N/A')}\n")
                    if 'error' in result:
                        f.write(f"Error: {result['error']}\n")
                    if 'stderr' in result and result['stderr']:
                        f.write(f"Error Output:\n{result['stderr']}\n")

                f.write("\n")

            f.write("=" * 100 + "\n")
            f.write("END OF REPORT\n")
            f.write("=" * 100 + "\n")

        logger.info(f"Human-readable report saved to: {text_file}")

    def print_summary(self, report: Dict):
        """Print summary to console"""
        logger.info("\n" + "=" * 80)
        logger.info("TEST EXECUTION SUMMARY")
        logger.info("=" * 80)

        summary = report['summary']
        logger.info(f"Total Tests: {summary['total_tests']}")
        logger.info(f"Successful: {summary['successful_tests']}")
        logger.info(f"Failed: {summary['failed_tests']}")
        logger.info(f"Success Rate: {summary['success_rate']:.1f}%")
        logger.info(f"Total Duration: {report['total_duration_seconds']:.2f} seconds")

        logger.info("\nIndividual Test Results:")
        for result in report['test_results']:
            status = "PASS" if result.get('success', False) else "FAIL"
            duration = result.get('duration_seconds', 0)
            logger.info(f"  [{status}] {result['test_name']} ({duration:.2f}s)")

        logger.info("\n" + "=" * 80)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description='Master Test Runner for Network Command Handling Tests'
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
        '--skip',
        nargs='+',
        choices=['test_1', 'test_2', 'test_3', 'test_4'],
        help='Skip specific tests'
    )

    args = parser.parse_args()

    # Run tests
    runner = MasterTestRunner(args.device_ip, args.port)
    report = runner.run_all_tests(skip_tests=args.skip)

    # Save report
    runner.save_master_report(report)

    # Print summary
    runner.print_summary(report)

    # Exit with appropriate code
    if report['summary']['failed_tests'] > 0:
        logger.error("\nSome tests failed!")
        sys.exit(1)
    else:
        logger.info("\nAll tests passed successfully!")
        sys.exit(0)


if __name__ == "__main__":
    main()
