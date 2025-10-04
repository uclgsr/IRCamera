#!/usr/bin/env python3
"""
Thermal Camera Recording Test (Real Integration)

Test: Conduct live recording with actual Topdon TC001 thermal camera
Output: Frame timestamp log with rate measurement and statistics
Subsystem: Thermal camera module
Chapters: Chapter 5 (real thermal capture) and Chapter 6 (thermal sensor performance)
"""

import json
import statistics
import sys
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Any


@dataclass
class FrameAnalysis:
    total_frames: int
    duration_seconds: float
    target_fps: float
    actual_fps: float
    fps_deviation: float
    min_interval_ms: float
    max_interval_ms: float
    mean_interval_ms: float
    std_interval_ms: float
    frame_drops_detected: int
    drop_threshold_ms: float


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    data_file_analyzed: str
    frame_analysis: Dict[str, any]
    passed: bool
    issues: List[str]


class ThermalRealCameraTest:
    """Test thermal camera recording with real TC001 hardware"""
    
    def __init__(self, data_file: str, target_fps: float = 25.0,
                 drop_threshold_ms: float = 100.0, output_dir: str = "output"):
        self.data_file = Path(data_file)
        self.target_fps = target_fps
        self.drop_threshold_ms = drop_threshold_ms
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
    def analyze_frame_log(self) -> Optional[FrameAnalysis]:
        """Analyze thermal frame log for rate and consistency"""
        if not self.data_file.exists():
            print(f"Error: Data file not found: {self.data_file}")
            return None
        
        print(f"Analyzing thermal frame log: {self.data_file}")
        
        timestamps = []
        
        # Read CSV file
        with open(self.data_file, 'r') as f:
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
                if len(parts) < 2:
                    continue
                    
                try:
                    # Second column is typically timestamp
                    timestamp_ms = float(parts[1])
                    timestamps.append(timestamp_ms)
                except (ValueError, IndexError) as e:
                    print(f"Warning: Failed to parse line {i}: {e}")
                    continue
        
        if len(timestamps) < 2:
            print("Error: Insufficient valid timestamps")
            return None
        
        print(f"Parsed {len(timestamps)} frames")
        
        # Calculate intervals
        intervals_ms = []
        for i in range(1, len(timestamps)):
            interval = timestamps[i] - timestamps[i-1]
            intervals_ms.append(interval)
        
        # Detect frame drops
        frame_drops = sum(1 for interval in intervals_ms 
                         if interval > self.drop_threshold_ms)
        
        # Calculate duration
        duration_seconds = (timestamps[-1] - timestamps[0]) / 1000.0
        
        # Calculate actual FPS
        actual_fps = len(timestamps) / duration_seconds if duration_seconds > 0 else 0
        fps_deviation = actual_fps - self.target_fps
        
        # Statistics
        min_interval = min(intervals_ms)
        max_interval = max(intervals_ms)
        mean_interval = statistics.mean(intervals_ms)
        std_interval = statistics.stdev(intervals_ms) if len(intervals_ms) > 1 else 0
        
        analysis = FrameAnalysis(
            total_frames=len(timestamps),
            duration_seconds=duration_seconds,
            target_fps=self.target_fps,
            actual_fps=actual_fps,
            fps_deviation=fps_deviation,
            min_interval_ms=min_interval,
            max_interval_ms=max_interval,
            mean_interval_ms=mean_interval,
            std_interval_ms=std_interval,
            frame_drops_detected=frame_drops,
            drop_threshold_ms=self.drop_threshold_ms
        )
        
        return analysis
    
    def run_test(self) -> TestResult:
        """Execute the thermal camera test"""
        print(f"\nStarting Thermal Real Camera Test...")
        
        start_time = datetime.now()
        start_timestamp = start_time.isoformat()
        
        # Analyze data file
        analysis = self.analyze_frame_log()
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        issues = []
        passed = True
        
        if analysis is None:
            issues.append("Failed to analyze data file")
            passed = False
        else:
            # Check for issues
            if abs(analysis.fps_deviation) > 5.0:
                issues.append(f"FPS deviation: {analysis.fps_deviation:.2f} Hz")
                passed = False
            
            if analysis.frame_drops_detected > 0:
                drop_pct = (analysis.frame_drops_detected / analysis.total_frames * 100)
                issues.append(f"Frame drops: {analysis.frame_drops_detected} ({drop_pct:.2f}%)")
                if drop_pct > 5.0:
                    passed = False
            
            if analysis.std_interval_ms > 20.0:
                issues.append(f"High interval variance: {analysis.std_interval_ms:.2f} ms")
        
        result = TestResult(
            test_name="Thermal Real Camera Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            data_file_analyzed=str(self.data_file),
            frame_analysis=asdict(analysis) if analysis else {},
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
        result_file = self.output_dir / f"thermal_real_result_{timestamp}.json"
        
        with open(result_file, 'w') as f:
            json.dump(asdict(result), f, indent=2)
        
        print(f"\nSaved result: {result_file}")
    
    def print_summary(self, result: TestResult, analysis: Optional[FrameAnalysis]) -> None:
        """Print test summary"""
        print("\n" + "="*60)
        print("THERMAL REAL CAMERA TEST SUMMARY")
        print("="*60)
        print(f"Test Name: {result.test_name}")
        print(f"Data File: {result.data_file_analyzed}")
        print(f"Analysis Duration: {result.duration_seconds:.2f} seconds")
        
        if analysis:
            print(f"\nFrame Analysis:")
            print(f"  Total Frames: {analysis.total_frames}")
            print(f"  Recording Duration: {analysis.duration_seconds:.2f} seconds")
            print(f"  Target FPS: {analysis.target_fps:.2f}")
            print(f"  Actual FPS: {analysis.actual_fps:.2f}")
            print(f"  FPS Deviation: {analysis.fps_deviation:.2f}")
            
            print(f"\nInterval Statistics:")
            print(f"  Min Interval: {analysis.min_interval_ms:.2f} ms")
            print(f"  Max Interval: {analysis.max_interval_ms:.2f} ms")
            print(f"  Mean Interval: {analysis.mean_interval_ms:.2f} ms")
            print(f"  Std Interval: {analysis.std_interval_ms:.2f} ms")
            
            print(f"\nFrame Drop Detection:")
            print(f"  Frame Drops: {analysis.frame_drops_detected}")
            print(f"  Drop Threshold: {analysis.drop_threshold_ms:.2f} ms")
        
        print(f"\nStatus: {'PASSED' if result.passed else 'FAILED'}")
        
        if result.issues:
            print(f"\nIssues Detected:")
            for issue in result.issues:
                print(f"  - {issue}")
        
        print("="*60)


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Thermal Real Camera Test')
    parser.add_argument('data_file', help='Path to thermal frame log CSV file')
    parser.add_argument('--fps', type=float, default=25.0,
                       help='Target frame rate in Hz (default: 25.0)')
    parser.add_argument('--drop-threshold', type=float, default=100.0,
                       help='Frame drop detection threshold in ms (default: 100.0)')
    
    args = parser.parse_args()
    
    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "thermal_tests"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Run test
    test = ThermalRealCameraTest(
        data_file=args.data_file,
        target_fps=args.fps,
        drop_threshold_ms=args.drop_threshold,
        output_dir=str(output_dir)
    )
    result = test.run_test()
    
    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
