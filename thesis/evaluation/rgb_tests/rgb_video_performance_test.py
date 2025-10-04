#!/usr/bin/env python3
"""
RGB Video/Frame Capture Performance Test

Test: Use phone's RGB camera to record video with concurrent frame extraction
Output: Video file, JPEG frames, and frame timestamp log with performance analysis
Subsystem: RGB camera recording
Chapters: Chapter 5 (video recording results) and Chapter 6 (recording throughput assessment)
"""

import json
import os
import statistics
import subprocess
import sys
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Any


@dataclass
class VideoMetadata:
    file_path: str
    file_size_mb: float
    duration_seconds: float
    frame_count: int
    fps: float
    resolution: str
    codec: str


@dataclass
class FrameAnalysis:
    total_frames: int
    expected_frames: int
    frame_loss_count: int
    frame_loss_percentage: float
    min_interval_ms: float
    max_interval_ms: float
    mean_interval_ms: float
    std_interval_ms: float


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    video_metadata: Optional[Dict[str, any]]
    frame_analysis: Dict[str, any]
    passed: bool
    issues: List[str]
    output_files: Dict[str, str]


class RGBVideoPerformanceTest:
    """Test RGB video and frame capture performance"""
    
    def __init__(self, video_file: str, frames_csv: str, target_fps: float = 30.0,
                 expected_duration: float = 60.0, output_dir: str = "output"):
        self.video_file = Path(video_file)
        self.frames_csv = Path(frames_csv)
        self.target_fps = target_fps
        self.expected_duration = expected_duration
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
    def analyze_video_metadata(self) -> Optional[VideoMetadata]:
        """Extract video file metadata using ffprobe if available"""
        if not self.video_file.exists():
            print(f"Error: Video file not found: {self.video_file}")
            return None
        
        print(f"Analyzing video file: {self.video_file}")
        
        # Get file size
        file_size_mb = self.video_file.stat().st_size / (1024 * 1024)
        
        try:
            # Try to use ffprobe for detailed metadata
            result = subprocess.run(
                ['ffprobe', '-v', 'quiet', '-print_format', 'json',
                 '-show_streams', '-show_format', str(self.video_file)],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                data = json.loads(result.stdout)
                
                # Extract video stream info
                video_stream = None
                for stream in data.get('streams', []):
                    if stream.get('codec_type') == 'video':
                        video_stream = stream
                        break
                
                if video_stream:
                    fps_parts = video_stream.get('r_frame_rate', '0/1').split('/')
                    fps = float(fps_parts[0]) / float(fps_parts[1]) if len(fps_parts) == 2 else 0
                    
                    duration = float(data.get('format', {}).get('duration', 0))
                    frame_count = int(video_stream.get('nb_frames', 0))
                    
                    width = video_stream.get('width', 0)
                    height = video_stream.get('height', 0)
                    resolution = f"{width}x{height}"
                    
                    codec = video_stream.get('codec_name', 'unknown')
                    
                    metadata = VideoMetadata(
                        file_path=str(self.video_file),
                        file_size_mb=file_size_mb,
                        duration_seconds=duration,
                        frame_count=frame_count,
                        fps=fps,
                        resolution=resolution,
                        codec=codec
                    )
                    
                    print(f"Video metadata extracted: {resolution} @ {fps:.2f} fps")
                    return metadata
        
        except (FileNotFoundError, subprocess.TimeoutExpired, json.JSONDecodeError) as e:
            print(f"ffprobe not available or failed: {e}")
        
        # Fallback: basic metadata
        metadata = VideoMetadata(
            file_path=str(self.video_file),
            file_size_mb=file_size_mb,
            duration_seconds=0,
            frame_count=0,
            fps=0,
            resolution="unknown",
            codec="unknown"
        )
        
        print(f"Basic metadata: file size = {file_size_mb:.2f} MB")
        return metadata
    
    def analyze_frame_log(self) -> Optional[FrameAnalysis]:
        """Analyze frame capture log"""
        if not self.frames_csv.exists():
            print(f"Error: Frame log not found: {self.frames_csv}")
            return None
        
        print(f"Analyzing frame log: {self.frames_csv}")
        
        timestamps = []
        
        # Read CSV file
        with open(self.frames_csv, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
            if len(lines) < 2:
                print("Error: File has insufficient data")
                return None
            
            # Parse timestamps (assuming timestamp is in one of the columns)
            for i, line in enumerate(lines[1:], start=1):
                parts = line.strip().split(',')
                if len(parts) < 1:
                    continue
                    
                try:
                    # Try first column as timestamp
                    timestamp_ms = float(parts[0])
                    timestamps.append(timestamp_ms)
                except (ValueError, IndexError):
                    continue
        
        if len(timestamps) < 2:
            print("Error: Insufficient valid timestamps")
            return None
        
        print(f"Parsed {len(timestamps)} frame timestamps")
        
        # Calculate intervals
        intervals_ms = []
        for i in range(1, len(timestamps)):
            interval = timestamps[i] - timestamps[i-1]
            intervals_ms.append(interval)
        
        # Calculate expected frames
        duration_seconds = (timestamps[-1] - timestamps[0]) / 1000.0
        expected_frames = int(duration_seconds * self.target_fps)
        
        # Calculate frame loss
        frame_loss = max(0, expected_frames - len(timestamps))
        frame_loss_pct = (frame_loss / expected_frames * 100) if expected_frames > 0 else 0
        
        # Statistics
        min_interval = min(intervals_ms)
        max_interval = max(intervals_ms)
        mean_interval = statistics.mean(intervals_ms)
        std_interval = statistics.stdev(intervals_ms) if len(intervals_ms) > 1 else 0
        
        analysis = FrameAnalysis(
            total_frames=len(timestamps),
            expected_frames=expected_frames,
            frame_loss_count=frame_loss,
            frame_loss_percentage=frame_loss_pct,
            min_interval_ms=min_interval,
            max_interval_ms=max_interval,
            mean_interval_ms=mean_interval,
            std_interval_ms=std_interval
        )
        
        return analysis
    
    def run_test(self) -> TestResult:
        """Execute the RGB video performance test"""
        print(f"\nStarting RGB Video Performance Test...")
        
        start_time = datetime.now()
        start_timestamp = start_time.isoformat()
        
        # Analyze video file
        video_metadata = self.analyze_video_metadata()
        
        # Analyze frame log
        frame_analysis = self.analyze_frame_log()
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        issues = []
        passed = True
        
        # Check video file
        if video_metadata is None:
            issues.append("Video file not found or corrupted")
            passed = False
        elif video_metadata.file_size_mb < 0.1:
            issues.append("Video file too small, may be corrupted")
            passed = False
        
        # Check frame analysis
        if frame_analysis is None:
            issues.append("Frame log not found or invalid")
            passed = False
        else:
            if frame_analysis.frame_loss_percentage > 5.0:
                issues.append(f"Frame loss: {frame_analysis.frame_loss_percentage:.2f}%")
                passed = False
            
            if frame_analysis.std_interval_ms > 50.0:
                issues.append(f"High interval variance: {frame_analysis.std_interval_ms:.2f} ms")
        
        result = TestResult(
            test_name="RGB Video Performance Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            video_metadata=asdict(video_metadata) if video_metadata else None,
            frame_analysis=asdict(frame_analysis) if frame_analysis else {},
            passed=passed,
            issues=issues,
            output_files={
                'video': str(self.video_file),
                'frames_log': str(self.frames_csv)
            }
        )
        
        # Save result
        self.save_result(result)
        
        # Print summary
        self.print_summary(result, video_metadata, frame_analysis)
        
        return result
    
    def save_result(self, result: TestResult) -> None:
        """Save test result as JSON"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.output_dir / f"rgb_video_result_{timestamp}.json"
        
        with open(result_file, 'w') as f:
            json.dump(asdict(result), f, indent=2)
        
        print(f"\nSaved result: {result_file}")
    
    def print_summary(self, result: TestResult, 
                     video_metadata: Optional[VideoMetadata],
                     frame_analysis: Optional[FrameAnalysis]) -> None:
        """Print test summary"""
        print("\n" + "="*60)
        print("RGB VIDEO PERFORMANCE TEST SUMMARY")
        print("="*60)
        print(f"Test Name: {result.test_name}")
        print(f"Analysis Duration: {result.duration_seconds:.2f} seconds")
        
        if video_metadata:
            print(f"\nVideo Metadata:")
            print(f"  File: {video_metadata.file_path}")
            print(f"  Size: {video_metadata.file_size_mb:.2f} MB")
            print(f"  Duration: {video_metadata.duration_seconds:.2f} seconds")
            print(f"  Frame Count: {video_metadata.frame_count}")
            print(f"  FPS: {video_metadata.fps:.2f}")
            print(f"  Resolution: {video_metadata.resolution}")
            print(f"  Codec: {video_metadata.codec}")
        
        if frame_analysis:
            print(f"\nFrame Capture Analysis:")
            print(f"  Total Frames: {frame_analysis.total_frames}")
            print(f"  Expected Frames: {frame_analysis.expected_frames}")
            print(f"  Frame Loss: {frame_analysis.frame_loss_count} ({frame_analysis.frame_loss_percentage:.2f}%)")
            print(f"\nInterval Statistics:")
            print(f"  Min Interval: {frame_analysis.min_interval_ms:.2f} ms")
            print(f"  Max Interval: {frame_analysis.max_interval_ms:.2f} ms")
            print(f"  Mean Interval: {frame_analysis.mean_interval_ms:.2f} ms")
            print(f"  Std Interval: {frame_analysis.std_interval_ms:.2f} ms")
        
        print(f"\nStatus: {'PASSED' if result.passed else 'FAILED'}")
        
        if result.issues:
            print(f"\nIssues Detected:")
            for issue in result.issues:
                print(f"  - {issue}")
        
        print("="*60)


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='RGB Video Performance Test')
    parser.add_argument('video_file', help='Path to recorded video file (MP4)')
    parser.add_argument('frames_csv', help='Path to frame capture log CSV')
    parser.add_argument('--fps', type=float, default=30.0,
                       help='Target frame rate in Hz (default: 30.0)')
    parser.add_argument('--duration', type=float, default=60.0,
                       help='Expected recording duration in seconds (default: 60.0)')
    
    args = parser.parse_args()
    
    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "rgb_tests"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Run test
    test = RGBVideoPerformanceTest(
        video_file=args.video_file,
        frames_csv=args.frames_csv,
        target_fps=args.fps,
        expected_duration=args.duration,
        output_dir=str(output_dir)
    )
    result = test.run_test()
    
    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
