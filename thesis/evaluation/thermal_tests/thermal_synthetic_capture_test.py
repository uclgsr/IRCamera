#!/usr/bin/env python3
"""
Thermal Frame Capture Verification Test (Synthetic)

Test: Run system in dummy thermal-camera mode with fake frame generator
Output: Frame timestamp log with interval verification
Subsystem: Thermal camera recording (simulation)
Chapters: Chapter 5 (thermal pipeline in simulation) and Chapter 6 (performance without real data)
"""

import json
import math
import statistics
import sys
import time
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any


@dataclass
class FrameCapture:
    frame_number: int
    timestamp_ms: int
    interval_from_previous_ms: float


@dataclass
class FrameAnalysis:
    total_frames: int
    expected_frames: int
    duration_seconds: float
    target_fps: float
    actual_fps: float
    fps_deviation: float
    min_interval_ms: float
    max_interval_ms: float
    mean_interval_ms: float
    std_interval_ms: float
    frames_within_tolerance: int
    tolerance_ms: float


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    frame_analysis: Dict[str, Any]
    passed: bool
    output_file: str


class MockThermalCamera:
    """Mock thermal camera generating synthetic frames"""
    
    def __init__(self, fps: float = 5.0, width: int = 384, height: int = 288):
        self.fps = fps
        self.width = width
        self.height = height
        self.frame_count = 0
        
    def capture_frame(self) -> tuple[int, int]:
        """Capture a synthetic thermal frame"""
        self.frame_count += 1
        timestamp_ms = int(time.time() * 1000)
        return self.frame_count, timestamp_ms


class ThermalSyntheticCaptureTest:
    """Test thermal frame capture with synthetic data"""
    
    def __init__(self, duration_seconds: int = 10, target_fps: float = 5.0,
                 tolerance_ms: float = 50.0, output_dir: str = "output"):
        self.duration_seconds = duration_seconds
        self.target_fps = target_fps
        self.tolerance_ms = tolerance_ms
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.frames: List[FrameCapture] = []
        
    def run_test(self) -> TestResult:
        """Execute the thermal synthetic capture test"""
        print(f"Starting Thermal Synthetic Capture Test...")
        print(f"Duration: {self.duration_seconds} seconds")
        print(f"Target FPS: {self.target_fps}")
        
        start_time = datetime.now()
        start_timestamp = start_time.isoformat()
        
        # Create mock camera
        camera = MockThermalCamera(fps=self.target_fps)
        expected_frames = int(self.duration_seconds * self.target_fps)
        frame_interval_ms = 1000.0 / self.target_fps
        
        print(f"Expected frames: {expected_frames}")
        print(f"Target interval: {frame_interval_ms:.2f} ms")
        
        # Capture frames
        test_start_ms = int(time.time() * 1000)
        last_timestamp_ms = test_start_ms
        
        while len(self.frames) < expected_frames:
            # Wait for next frame time
            elapsed_ms = int(time.time() * 1000) - test_start_ms
            expected_frame_num = int(elapsed_ms / frame_interval_ms)
            
            if expected_frame_num >= len(self.frames):
                frame_num, timestamp_ms = camera.capture_frame()
                interval = timestamp_ms - last_timestamp_ms if self.frames else 0
                
                frame = FrameCapture(
                    frame_number=frame_num,
                    timestamp_ms=timestamp_ms,
                    interval_from_previous_ms=interval
                )
                self.frames.append(frame)
                last_timestamp_ms = timestamp_ms
                
                # Progress indicator
                if frame_num % 10 == 0 or frame_num == expected_frames:
                    print(f"  Captured {frame_num}/{expected_frames} frames...")
            
            # Small sleep to avoid busy waiting
            time.sleep(0.001)
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        print(f"Captured {len(self.frames)} frames in {duration:.2f} seconds")
        
        # Analyze results
        analysis = self.analyze_frames()
        
        # Test passes if frame count matches and timing is within tolerance
        frame_count_ok = abs(analysis.total_frames - analysis.expected_frames) <= 2
        fps_deviation_ok = abs(analysis.fps_deviation) < 0.5
        passed = frame_count_ok and fps_deviation_ok
        
        # Save output files
        csv_file = self.save_csv()
        
        result = TestResult(
            test_name="Thermal Synthetic Capture Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            frame_analysis=asdict(analysis),
            passed=passed,
            output_file=str(csv_file)
        )
        
        # Save result
        self.save_result(result)
        
        # Print summary
        self.print_summary(result, analysis)
        
        return result
    
    def analyze_frames(self) -> FrameAnalysis:
        """Analyze captured frames"""
        if len(self.frames) < 2:
            raise ValueError("Insufficient frames for analysis")
        
        # Calculate intervals (skip first frame which has 0 interval)
        intervals_ms = [f.interval_from_previous_ms for f in self.frames[1:]]
        
        # Calculate duration
        duration_seconds = (self.frames[-1].timestamp_ms - 
                          self.frames[0].timestamp_ms) / 1000.0
        
        # Calculate actual FPS
        actual_fps = len(self.frames) / duration_seconds if duration_seconds > 0 else 0
        fps_deviation = actual_fps - self.target_fps
        
        # Statistics
        min_interval = min(intervals_ms)
        max_interval = max(intervals_ms)
        mean_interval = statistics.mean(intervals_ms)
        std_interval = statistics.stdev(intervals_ms) if len(intervals_ms) > 1 else 0
        
        # Count frames within tolerance
        expected_interval = 1000.0 / self.target_fps
        frames_within_tolerance = sum(
            1 for interval in intervals_ms
            if abs(interval - expected_interval) <= self.tolerance_ms
        )
        
        expected_frames = int(self.duration_seconds * self.target_fps)
        
        return FrameAnalysis(
            total_frames=len(self.frames),
            expected_frames=expected_frames,
            duration_seconds=duration_seconds,
            target_fps=self.target_fps,
            actual_fps=actual_fps,
            fps_deviation=fps_deviation,
            min_interval_ms=min_interval,
            max_interval_ms=max_interval,
            mean_interval_ms=mean_interval,
            std_interval_ms=std_interval,
            frames_within_tolerance=frames_within_tolerance,
            tolerance_ms=self.tolerance_ms
        )
    
    def save_csv(self) -> Path:
        """Save frame captures to CSV file"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        csv_file = self.output_dir / f"thermal_synthetic_{timestamp}.csv"
        
        with open(csv_file, 'w', encoding='utf-8') as f:
            # Write header
            f.write("frame_number,timestamp_ms,interval_from_previous_ms\n")
            
            # Write frames
            for frame in self.frames:
                f.write(f"{frame.frame_number},{frame.timestamp_ms},"
                       f"{frame.interval_from_previous_ms:.2f}\n")
        
        print(f"Saved CSV: {csv_file}")
        return csv_file
    
    def save_result(self, result: TestResult) -> None:
        """Save test result as JSON"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.output_dir / f"thermal_synthetic_result_{timestamp}.json"
        
        with open(result_file, 'w', encoding='utf-8') as f:
            json.dump(asdict(result), f, indent=2)
        
        print(f"Saved result: {result_file}")
    
    def print_summary(self, result: TestResult, analysis: FrameAnalysis) -> None:
        """Print test summary"""
        print("\n" + "="*60)
        print("THERMAL SYNTHETIC CAPTURE TEST SUMMARY")
        print("="*60)
        print(f"Test Name: {result.test_name}")
        print(f"Duration: {result.duration_seconds:.2f} seconds")
        
        print(f"\nFrame Analysis:")
        print(f"  Total Frames: {analysis.total_frames}")
        print(f"  Expected Frames: {analysis.expected_frames}")
        print(f"  Recording Duration: {analysis.duration_seconds:.2f} seconds")
        print(f"  Target FPS: {analysis.target_fps:.2f}")
        print(f"  Actual FPS: {analysis.actual_fps:.2f}")
        print(f"  FPS Deviation: {analysis.fps_deviation:.2f}")
        
        print(f"\nInterval Statistics:")
        print(f"  Min Interval: {analysis.min_interval_ms:.2f} ms")
        print(f"  Max Interval: {analysis.max_interval_ms:.2f} ms")
        print(f"  Mean Interval: {analysis.mean_interval_ms:.2f} ms")
        print(f"  Std Interval: {analysis.std_interval_ms:.2f} ms")
        
        print(f"\nConsistency:")
        print(f"  Frames Within Tolerance: {analysis.frames_within_tolerance}/{len(self.frames)-1}")
        print(f"  Tolerance: ±{analysis.tolerance_ms:.2f} ms")
        
        print(f"\nStatus: {'PASSED' if result.passed else 'FAILED'}")
        print(f"Output File: {result.output_file}")
        print("="*60)


def main():
    """Main entry point"""
    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "thermal_tests"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Run test
    test = ThermalSyntheticCaptureTest(
        duration_seconds=10,
        target_fps=5.0,
        tolerance_ms=50.0,
        output_dir=str(output_dir)
    )
    result = test.run_test()
    
    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
