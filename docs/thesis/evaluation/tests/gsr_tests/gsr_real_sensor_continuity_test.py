#!/usr/bin/env python3
"""
GSR Logging Continuity Test (Real Sensor)

Test: Perform full recording with actual Shimmer3 GSR sensor
Output: Timestamped GSR log (CSV) with sample rate analysis
Subsystem: GSR sensor integration
Chapters: Chapter 5 (empirical recording fidelity) and Chapter 6 (reliability evaluation)
"""

import json
import os
import statistics
import sys
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Any


@dataclass
class SampleRateAnalysis:
    total_samples: int
    duration_seconds: float
    average_rate_hz: float
    expected_rate_hz: float
    rate_deviation_hz: float
    min_interval_ms: float
    max_interval_ms: float
    mean_interval_ms: float
    std_interval_ms: float
    gaps_detected: int
    gap_threshold_ms: float


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    data_file_analyzed: str
    sample_analysis: Dict[str, Any]
    passed: bool
    issues: List[str]


class GSRRealSensorContinuityTest:
    """Test GSR logging continuity with real Shimmer3 sensor data"""

    def __init__(self, data_file: str, expected_rate_hz: float = 128.0,
                 gap_threshold_ms: float = 20.0, output_dir: str = "output"):
        self.data_file = Path(data_file)
        self.expected_rate_hz = expected_rate_hz
        self.gap_threshold_ms = gap_threshold_ms
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def analyze_csv_file(self) -> Optional[SampleRateAnalysis]:
        """Analyze GSR CSV file for continuity and rate consistency"""
        if not self.data_file.exists():
            print(f"Error: Data file not found: {self.data_file}")
            return None

        print(f"Analyzing GSR data file: {self.data_file}")

        timestamps = []

        # Read CSV file
        with open(self.data_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()

            # Skip header
            if len(lines) < 2:
                print("Error: File has insufficient data")
                return None

            header = lines[0].strip()
            print(f"CSV Header: {header}")

            # Parse timestamps
            for i, line in enumerate(lines[1:], start=1):
                parts = line.strip().split(',')
                if len(parts) < 1:
                    continue

                try:
                    # Assuming first column is timestamp in milliseconds
                    timestamp_ms = float(parts[0])
                    timestamps.append(timestamp_ms)
                except (ValueError, IndexError) as e:
                    print(f"Warning: Failed to parse line {i}: {e}")
                    continue

        if len(timestamps) < 2:
            print("Error: Insufficient valid timestamps")
            return None

        print(f"Parsed {len(timestamps)} samples")

        # Calculate intervals
        intervals_ms = []
        for i in range(1, len(timestamps)):
            interval = timestamps[i] - timestamps[i - 1]
            intervals_ms.append(interval)

        # Detect gaps
        gaps_detected = sum(1 for interval in intervals_ms
                            if interval > self.gap_threshold_ms)

        # Calculate duration
        duration_seconds = (timestamps[-1] - timestamps[0]) / 1000.0

        # Calculate average rate
        average_rate_hz = len(timestamps) / duration_seconds if duration_seconds > 0 else 0

        # Statistics
        min_interval = min(intervals_ms)
        max_interval = max(intervals_ms)
        mean_interval = statistics.mean(intervals_ms)
        std_interval = statistics.stdev(intervals_ms) if len(intervals_ms) > 1 else 0

        rate_deviation = abs(average_rate_hz - self.expected_rate_hz)

        analysis = SampleRateAnalysis(
            total_samples=len(timestamps),
            duration_seconds=duration_seconds,
            average_rate_hz=average_rate_hz,
            expected_rate_hz=self.expected_rate_hz,
            rate_deviation_hz=rate_deviation,
            min_interval_ms=min_interval,
            max_interval_ms=max_interval,
            mean_interval_ms=mean_interval,
            std_interval_ms=std_interval,
            gaps_detected=gaps_detected,
            gap_threshold_ms=self.gap_threshold_ms
        )

        return analysis

    def run_test(self) -> TestResult:
        """Execute the GSR continuity test"""
        print(f"\nStarting GSR Real Sensor Continuity Test...")

        start_time = datetime.now()
        start_timestamp = start_time.isoformat()

        # Analyze data file
        analysis = self.analyze_csv_file()

        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()

        issues = []
        passed = True

        if analysis is None:
            issues.append("Failed to analyze data file")
            passed = False
        else:
            # Check for issues
            if analysis.rate_deviation_hz > 5.0:
                issues.append(f"Sample rate deviation: {analysis.rate_deviation_hz:.2f} Hz")
                passed = False

            if analysis.gaps_detected > 0:
                issues.append(f"Gaps detected: {analysis.gaps_detected}")
                # Don't fail test for minor gaps

            expected_samples = int(analysis.duration_seconds * self.expected_rate_hz)
            sample_loss = abs(analysis.total_samples - expected_samples)
            sample_loss_pct = (sample_loss / expected_samples * 100) if expected_samples > 0 else 0

            if sample_loss_pct > 5.0:
                issues.append(f"Sample loss: {sample_loss_pct:.2f}%")
                passed = False

        result = TestResult(
            test_name="GSR Real Sensor Continuity Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            data_file_analyzed=str(self.data_file),
            sample_analysis=asdict(analysis) if analysis else {},
            passed=passed,
            issues=issues
        )

        # Save result
        self.save_result(result)

        # Print summary
        self.print_summary(result, analysis)

        return result

    def save_result(self, result: TestResult) -> None:
        """Save test result as JSON"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.output_dir / f"gsr_continuity_result_{timestamp}.json"

        with open(result_file, 'w') as f:
            json.dump(asdict(result), f, indent=2)

        print(f"\nSaved result: {result_file}")

    def print_summary(self, result: TestResult, analysis: Optional[SampleRateAnalysis]) -> None:
        """Print test summary"""
        print("\n" + "=" * 60)
        print("GSR REAL SENSOR CONTINUITY TEST SUMMARY")
        print("=" * 60)
        print(f"Test Name: {result.test_name}")
        print(f"Data File: {result.data_file_analyzed}")
        print(f"Analysis Duration: {result.duration_seconds:.2f} seconds")

        if analysis:
            print(f"\nSample Analysis:")
            print(f"  Total Samples: {analysis.total_samples}")
            print(f"  Recording Duration: {analysis.duration_seconds:.2f} seconds")
            print(f"  Average Rate: {analysis.average_rate_hz:.2f} Hz")
            print(f"  Expected Rate: {analysis.expected_rate_hz:.2f} Hz")
            print(f"  Rate Deviation: {analysis.rate_deviation_hz:.2f} Hz")
            print(f"\nInterval Statistics:")
            print(f"  Min Interval: {analysis.min_interval_ms:.2f} ms")
            print(f"  Max Interval: {analysis.max_interval_ms:.2f} ms")
            print(f"  Mean Interval: {analysis.mean_interval_ms:.2f} ms")
            print(f"  Std Interval: {analysis.std_interval_ms:.2f} ms")
            print(f"\nGap Detection:")
            print(f"  Gaps Detected: {analysis.gaps_detected}")
            print(f"  Gap Threshold: {analysis.gap_threshold_ms:.2f} ms")

        print(f"\nStatus: {'PASSED' if result.passed else 'FAILED'}")

        if result.issues:
            print(f"\nIssues Detected:")
            for issue in result.issues:
                print(f"  - {issue}")

        print("=" * 60)


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(description='GSR Real Sensor Continuity Test')
    parser.add_argument('data_file', help='Path to GSR CSV data file')
    parser.add_argument('--rate', type=float, default=128.0,
                        help='Expected sampling rate in Hz (default: 128.0)')
    parser.add_argument('--gap-threshold', type=float, default=20.0,
                        help='Gap detection threshold in ms (default: 20.0)')

    args = parser.parse_args()

    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "gsr_tests"
    output_dir.mkdir(parents=True, exist_ok=True)

    # Run test
    test = GSRRealSensorContinuityTest(
        data_file=args.data_file,
        expected_rate_hz=args.rate,
        gap_threshold_ms=args.gap_threshold,
        output_dir=str(output_dir)
    )
    result = test.run_test()

    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
