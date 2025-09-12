#!/usr/bin/env python3
"""
Comprehensive Test Runner for Hub-and-Spoke Multi-Modal Physiological Sensing Platform
Executes unit tests, integration tests, and performance tests across both Android and PC Controller
"""

import argparse
import json
import os
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple


@dataclass
class TestResult:
    name: str
    passed: bool
    duration: float
    details: str
    coverage: Optional[float] = None


@dataclass
class TestSuite:
    name: str
    description: str
    command: str
    working_dir: str
    timeout: int = 300  # 5 minutes default


class ComprehensiveTestRunner:
    """Main test runner that orchestrates all test suites"""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.results: List[TestResult] = []

        # Define all test suites
        self.test_suites = self._define_test_suites()

    def _define_test_suites(self) -> List[TestSuite]:
        """Define all test suites to be executed"""
        return [
            # Android Unit Tests
            TestSuite(
                name="android_unit_tests",
                description="Android Unit Tests (RecordingController, TimeManager, NetworkClient, GSRSensorRecorder)",
                command="./gradlew test --info",
                working_dir=str(self.project_root),
                timeout=600,
            ),
            # Android Integration Tests
            TestSuite(
                name="android_integration_tests",
                description="Android Integration Tests (Hub-Spoke Communication, Multi-Modal Coordination)",
                command="./gradlew connectedAndroidTest --info",
                working_dir=str(self.project_root),
                timeout=900,
            ),
            # Android Performance Tests
            TestSuite(
                name="android_performance_tests",
                description="Android Performance Tests (Throughput, Latency, Resource Usage)",
                command="./gradlew connectedBenchmarkAndroidTest --info",
                working_dir=str(self.project_root),
                timeout=1200,
            ),
            # PC Controller Unit Tests
            TestSuite(
                name="pc_unit_tests",
                description="PC Controller Unit Tests (Network Server, Data Aggregation, Protocol)",
                command="python -m pytest src/ircamera_pc/tests/test_network.py src/ircamera_pc/tests/test_data_aggregation.py -v --cov=ircamera_pc --cov-report=html",
                working_dir=str(self.project_root / "pc-controller"),
                timeout=300,
            ),
            # PC Controller Integration Tests
            TestSuite(
                name="pc_integration_tests",
                description="PC Controller Integration Tests (End-to-End Hub-Spoke Workflows)",
                command="python -m pytest src/ircamera_pc/tests/test_integration.py -v --cov=ircamera_pc --cov-append",
                working_dir=str(self.project_root / "pc-controller"),
                timeout=600,
            ),
            # Cross-Platform Integration Tests
            TestSuite(
                name="cross_platform_tests",
                description="Cross-Platform Integration Tests (Android-PC Communication)",
                command="python integration_example.py --test-mode",
                working_dir=str(self.project_root / "pc-controller"),
                timeout=300,
            ),
            # Vendor SDK Integration Validation
            TestSuite(
                name="vendor_sdk_validation",
                description="Vendor SDK Integration Validation (Shimmer, IR Camera, Real Hardware)",
                command="./gradlew testDebugUnitTest --tests '*GSRSensorRecorderTest*' --info",
                working_dir=str(self.project_root),
                timeout=300,
            ),
            # System Validation Tests
            TestSuite(
                name="system_validation",
                description="System Validation (Component Verification, Build Validation)",
                command="python test_components.py",
                working_dir=str(self.project_root / "pc-controller"),
                timeout=120,
            ),
        ]

    def run_all_tests(
        self,
        include_performance: bool = True,
        include_integration: bool = True,
        parallel: bool = False,
    ) -> Tuple[int, int]:
        """
        Run all test suites

        Args:
            include_performance: Whether to run performance tests (slower)
            include_integration: Whether to run integration tests
            parallel: Whether to run tests in parallel (experimental)

        Returns:
            Tuple of (passed_count, total_count)
        """
        print("🚀 Starting Comprehensive Test Suite for Hub-and-Spoke Architecture")
        print("=" * 80)

        # Filter test suites based on options
        suites_to_run = self._filter_test_suites(
            include_performance, include_integration
        )

        start_time = time.time()
        passed_count = 0

        for suite in suites_to_run:
            print(f"\n📋 Running: {suite.name}")
            print(f"   Description: {suite.description}")
            print(f"   Command: {suite.command}")
            print("-" * 60)

            result = self._run_test_suite(suite)
            self.results.append(result)

            if result.passed:
                print(f"✅ {suite.name} PASSED ({result.duration:.2f}s)")
                passed_count += 1
            else:
                print(f"❌ {suite.name} FAILED ({result.duration:.2f}s)")
                if result.details:
                    print(f"   Details: {result.details[:200]}...")

        total_time = time.time() - start_time

        # Generate comprehensive report
        self._generate_report(total_time)

        print(f"\n🏁 Test Execution Complete!")
        print(f"   Total Time: {total_time:.2f} seconds")
        print(f"   Results: {passed_count}/{len(suites_to_run)} test suites passed")

        return passed_count, len(suites_to_run)

    def _filter_test_suites(
        self, include_performance: bool, include_integration: bool
    ) -> List[TestSuite]:
        """Filter test suites based on execution options"""
        filtered_suites = []

        for suite in self.test_suites:
            # Skip performance tests if not requested
            if not include_performance and "performance" in suite.name:
                print(f"⏭️  Skipping performance test: {suite.name}")
                continue

            # Skip integration tests if not requested
            if not include_integration and "integration" in suite.name:
                print(f"⏭️  Skipping integration test: {suite.name}")
                continue

            filtered_suites.append(suite)

        return filtered_suites

    def _run_test_suite(self, suite: TestSuite) -> TestResult:
        """Run a single test suite and capture results"""
        start_time = time.time()

        try:
            # Execute the test command
            process = subprocess.Popen(
                suite.command,
                shell=True,
                cwd=suite.working_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                universal_newlines=True,
            )

            stdout, _ = process.communicate(timeout=suite.timeout)
            duration = time.time() - start_time

            # Determine if test passed based on exit code and output
            passed = process.returncode == 0

            # Extract coverage information if available
            coverage = self._extract_coverage(stdout)

            return TestResult(
                name=suite.name,
                passed=passed,
                duration=duration,
                details=stdout[-1000:] if stdout else "",  # Last 1000 chars
                coverage=coverage,
            )

        except subprocess.TimeoutExpired:
            duration = time.time() - start_time
            return TestResult(
                name=suite.name,
                passed=False,
                duration=duration,
                details=f"Test suite timed out after {suite.timeout} seconds",
            )

        except Exception as e:
            duration = time.time() - start_time
            return TestResult(
                name=suite.name,
                passed=False,
                duration=duration,
                details=f"Error executing test suite: {str(e)}",
            )

    def _extract_coverage(self, output: str) -> Optional[float]:
        """Extract coverage percentage from test output"""
        lines = output.split("\n")
        for line in lines:
            if "coverage" in line.lower() and "%" in line:
                # Try to extract percentage
                import re

                match = re.search(r"(\d+(?:\.\d+)?)%", line)
                if match:
                    return float(match.group(1))
        return None

    def _generate_report(self, total_time: float):
        """Generate comprehensive test report"""
        report_dir = self.project_root / "test_reports"
        report_dir.mkdir(exist_ok=True)

        # Generate JSON report
        json_report = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_duration": total_time,
            "summary": {
                "total_suites": len(self.results),
                "passed": sum(1 for r in self.results if r.passed),
                "failed": sum(1 for r in self.results if not r.passed),
                "average_coverage": self._calculate_average_coverage(),
            },
            "results": [
                {
                    "name": r.name,
                    "passed": r.passed,
                    "duration": r.duration,
                    "coverage": r.coverage,
                    "details": r.details[:500] if r.details else None,
                }
                for r in self.results
            ],
        }

        json_file = report_dir / "comprehensive_test_report.json"
        with open(json_file, "w") as f:
            json.dump(json_report, f, indent=2)

        # Generate HTML report
        html_report = self._generate_html_report(json_report)
        html_file = report_dir / "comprehensive_test_report.html"
        with open(html_file, "w") as f:
            f.write(html_report)

        print(f"\n📊 Reports Generated:")
        print(f"   JSON: {json_file}")
        print(f"   HTML: {html_file}")

    def _calculate_average_coverage(self) -> Optional[float]:
        """Calculate average coverage across all tests"""
        coverages = [r.coverage for r in self.results if r.coverage is not None]
        return sum(coverages) / len(coverages) if coverages else None

    def _generate_html_report(self, json_report: Dict) -> str:
        """Generate HTML test report"""
        html = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Hub-and-Spoke Comprehensive Test Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .header {{ background: #f0f0f0; padding: 20px; border-radius: 8px; }}
        .summary {{ display: flex; gap: 20px; margin: 20px 0; }}
        .metric {{ background: #e8f4f8; padding: 15px; border-radius: 5px; text-align: center; }}
        .passed {{ background: #d4edda; color: #155724; }}
        .failed {{ background: #f8d7da; color: #721c24; }}
        .test-result {{ margin: 10px 0; padding: 10px; border-radius: 5px; }}
        .details {{ font-size: 0.8em; color: #666; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>🏗️ Hub-and-Spoke Architecture Test Report</h1>
        <p>Generated: {json_report['timestamp']}</p>
        <p>Total Duration: {json_report['total_duration']:.2f} seconds</p>
    </div>
    
    <div class="summary">
        <div class="metric">
            <h3>Total Suites</h3>
            <div style="font-size: 2em;">{json_report['summary']['total_suites']}</div>
        </div>
        <div class="metric passed">
            <h3>Passed</h3>
            <div style="font-size: 2em;">{json_report['summary']['passed']}</div>
        </div>
        <div class="metric failed">
            <h3>Failed</h3>
            <div style="font-size: 2em;">{json_report['summary']['failed']}</div>
        </div>
        <div class="metric">
            <h3>Coverage</h3>
            <div style="font-size: 2em;">
                {json_report['summary']['average_coverage']:.1f}%
            </div>
        </div>
    </div>
    
    <h2>Test Results</h2>
    """

        for result in json_report["results"]:
            status_class = "passed" if result["passed"] else "failed"
            status_icon = "✅" if result["passed"] else "❌"

            html += f"""
    <div class="test-result {status_class}">
        <h3>{status_icon} {result['name']} ({result['duration']:.2f}s)</h3>
        {f"<p>Coverage: {result['coverage']:.1f}%</p>" if result['coverage'] else ""}
        {f'<div class="details">{result["details"]}</div>' if result['details'] else ""}
    </div>
            """

        html += """
</body>
</html>
        """

        return html


def main():
    """Main entry point for comprehensive test runner"""
    parser = argparse.ArgumentParser(
        description="Comprehensive test runner for Hub-and-Spoke architecture"
    )

    parser.add_argument(
        "--no-performance",
        action="store_true",
        help="Skip performance tests (faster execution)",
    )

    parser.add_argument(
        "--no-integration",
        action="store_true",
        help="Skip integration tests (unit tests only)",
    )

    parser.add_argument(
        "--parallel", action="store_true", help="Run tests in parallel (experimental)"
    )

    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path(__file__).parent,
        help="Root directory of the project",
    )

    args = parser.parse_args()

    # Initialize and run tests
    runner = ComprehensiveTestRunner(args.project_root)

    passed_count, total_count = runner.run_all_tests(
        include_performance=not args.no_performance,
        include_integration=not args.no_integration,
        parallel=args.parallel,
    )

    # Exit with appropriate code
    success_rate = passed_count / total_count if total_count > 0 else 0

    if success_rate >= 0.9:  # 90% success rate required
        print(f"\n🎉 Test suite PASSED with {success_rate:.1%} success rate")
        sys.exit(0)
    else:
        print(f"\n💥 Test suite FAILED with {success_rate:.1%} success rate")
        print("   Required: 90% success rate for passing")
        sys.exit(1)


if __name__ == "__main__":
    main()
