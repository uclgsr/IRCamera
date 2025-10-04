#!/usr/bin/env python3
"""
Main Test Runner for Thesis Evaluation

Executes all data recording correctness and performance tests
Generates comprehensive report for Chapter 5 and Chapter 6
"""

import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any


class TestRunner:
    """Main test runner for all thesis evaluation tests"""
    
    def __init__(self, base_dir: str = None):
        if base_dir is None:
            self.base_dir = Path(__file__).parent.parent
        else:
            self.base_dir = Path(base_dir)
        
        self.output_dir = self.base_dir / "output"
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        self.test_results: List[Dict[str, Any]] = []
        
    def run_test(self, test_script: Path, test_name: str, 
                 args: List[str] = None) -> Dict[str, Any]:
        """Run a single test and capture results"""
        print(f"\n{'='*60}")
        print(f"Running: {test_name}")
        print(f"{'='*60}")
        
        start_time = datetime.now()
        
        # Build command
        cmd = [sys.executable, str(test_script)]
        if args:
            cmd.extend(args)
        
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300  # 5 minute timeout
            )
            
            end_time = datetime.now()
            duration = (end_time - start_time).total_seconds()
            
            # Print output
            print(result.stdout)
            if result.stderr:
                print("STDERR:", result.stderr)
            
            test_result = {
                'test_name': test_name,
                'script': str(test_script),
                'start_time': start_time.isoformat(),
                'end_time': end_time.isoformat(),
                'duration_seconds': duration,
                'exit_code': result.returncode,
                'passed': result.returncode == 0,
                'stdout': result.stdout,
                'stderr': result.stderr
            }
            
            return test_result
            
        except subprocess.TimeoutExpired:
            print(f"ERROR: Test timed out after 5 minutes")
            return {
                'test_name': test_name,
                'script': str(test_script),
                'start_time': start_time.isoformat(),
                'end_time': datetime.now().isoformat(),
                'duration_seconds': 300.0,
                'exit_code': -1,
                'passed': False,
                'error': 'Test timed out'
            }
        except Exception as e:
            print(f"ERROR: {e}")
            return {
                'test_name': test_name,
                'script': str(test_script),
                'start_time': start_time.isoformat(),
                'end_time': datetime.now().isoformat(),
                'duration_seconds': 0.0,
                'exit_code': -1,
                'passed': False,
                'error': str(e)
            }
    
    def run_synthetic_tests(self) -> None:
        """Run all synthetic data tests"""
        print("\n" + "="*60)
        print("RUNNING SYNTHETIC DATA TESTS")
        print("="*60)
        
        # GSR Synthetic Integrity Test
        test_script = self.base_dir / "gsr_tests" / "gsr_synthetic_integrity_test.py"
        if test_script.exists():
            result = self.run_test(test_script, "GSR Synthetic Integrity Test")
            self.test_results.append(result)
        else:
            print(f"Warning: Test script not found: {test_script}")
        
        # Thermal Synthetic Capture Test
        test_script = self.base_dir / "thermal_tests" / "thermal_synthetic_capture_test.py"
        if test_script.exists():
            result = self.run_test(test_script, "Thermal Synthetic Capture Test")
            self.test_results.append(result)
        else:
            print(f"Warning: Test script not found: {test_script}")
    
    def run_real_sensor_tests(self, data_files: Dict[str, str] = None) -> None:
        """Run tests with real sensor data"""
        print("\n" + "="*60)
        print("RUNNING REAL SENSOR DATA TESTS")
        print("="*60)
        
        if data_files is None:
            print("Note: Real sensor tests require data files. Skipping.")
            print("To run real sensor tests, provide data file paths.")
            return
        
        # GSR Real Sensor Continuity Test
        if 'gsr_csv' in data_files:
            test_script = self.base_dir / "gsr_tests" / "gsr_real_sensor_continuity_test.py"
            if test_script.exists():
                result = self.run_test(
                    test_script,
                    "GSR Real Sensor Continuity Test",
                    [data_files['gsr_csv']]
                )
                self.test_results.append(result)
        
        # Thermal Real Camera Test
        if 'thermal_csv' in data_files:
            test_script = self.base_dir / "thermal_tests" / "thermal_real_camera_test.py"
            if test_script.exists():
                result = self.run_test(
                    test_script,
                    "Thermal Real Camera Test",
                    [data_files['thermal_csv']]
                )
                self.test_results.append(result)
        
        # RGB Video Performance Test
        if 'rgb_video' in data_files and 'rgb_frames_csv' in data_files:
            test_script = self.base_dir / "rgb_tests" / "rgb_video_performance_test.py"
            if test_script.exists():
                result = self.run_test(
                    test_script,
                    "RGB Video Performance Test",
                    [data_files['rgb_video'], data_files['rgb_frames_csv']]
                )
                self.test_results.append(result)
    
    def run_integrity_tests(self, session_dir: str = None) -> None:
        """Run file integrity validation tests"""
        print("\n" + "="*60)
        print("RUNNING FILE INTEGRITY TESTS")
        print("="*60)
        
        if session_dir is None:
            print("Note: File integrity test requires session directory. Skipping.")
            return
        
        test_script = self.base_dir / "data_integrity" / "file_integrity_validator.py"
        if test_script.exists():
            result = self.run_test(
                test_script,
                "File Integrity Validation Test",
                [session_dir]
            )
            self.test_results.append(result)
    
    def generate_summary_report(self) -> None:
        """Generate comprehensive summary report"""
        print("\n" + "="*60)
        print("GENERATING SUMMARY REPORT")
        print("="*60)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"test_suite_report_{timestamp}.json"
        
        # Create summary
        total_tests = len(self.test_results)
        passed_tests = sum(1 for r in self.test_results if r['passed'])
        failed_tests = total_tests - passed_tests
        
        summary = {
            'report_generated': datetime.now().isoformat(),
            'total_tests': total_tests,
            'passed': passed_tests,
            'failed': failed_tests,
            'pass_rate': (passed_tests / total_tests * 100) if total_tests > 0 else 0,
            'test_results': self.test_results
        }
        
        # Save report
        with open(report_file, 'w') as f:
            json.dump(summary, f, indent=2)
        
        print(f"\nSummary Report saved to: {report_file}")
        
        # Print summary
        print("\n" + "="*60)
        print("TEST SUITE SUMMARY")
        print("="*60)
        print(f"Total Tests: {total_tests}")
        print(f"Passed: {passed_tests}")
        print(f"Failed: {failed_tests}")
        print(f"Pass Rate: {summary['pass_rate']:.1f}%")
        
        print(f"\nTest Results:")
        for result in self.test_results:
            status = "PASS" if result['passed'] else "FAIL"
            print(f"  [{status}] {result['test_name']} ({result['duration_seconds']:.2f}s)")
        
        print("="*60)
    
    def run_all(self, data_files: Dict[str, str] = None, 
                session_dir: str = None) -> bool:
        """Run all tests and generate reports"""
        print("\n" + "="*60)
        print("THESIS EVALUATION TEST SUITE")
        print("Data Recording Correctness and Performance")
        print("="*60)
        print(f"Start Time: {datetime.now().isoformat()}")
        
        # Run synthetic tests (always available)
        self.run_synthetic_tests()
        
        # Run real sensor tests (if data files provided)
        self.run_real_sensor_tests(data_files)
        
        # Run integrity tests (if session directory provided)
        self.run_integrity_tests(session_dir)
        
        # Generate summary report
        self.generate_summary_report()
        
        # Return overall success
        all_passed = all(r['passed'] for r in self.test_results)
        return all_passed


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Run all thesis evaluation tests',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run only synthetic tests
  python3 run_all_tests.py
  
  # Run all tests with real data
  python3 run_all_tests.py \\
    --gsr-csv /path/to/gsr_data.csv \\
    --thermal-csv /path/to/thermal_frames.csv \\
    --rgb-video /path/to/video.mp4 \\
    --rgb-frames-csv /path/to/rgb_frames.csv \\
    --session-dir /path/to/session_directory
        """
    )
    
    parser.add_argument('--gsr-csv', help='Path to GSR data CSV file')
    parser.add_argument('--thermal-csv', help='Path to thermal frame log CSV')
    parser.add_argument('--rgb-video', help='Path to RGB video file (MP4)')
    parser.add_argument('--rgb-frames-csv', help='Path to RGB frames log CSV')
    parser.add_argument('--session-dir', help='Path to session directory for integrity check')
    
    args = parser.parse_args()
    
    # Prepare data files dict
    data_files = {}
    if args.gsr_csv:
        data_files['gsr_csv'] = args.gsr_csv
    if args.thermal_csv:
        data_files['thermal_csv'] = args.thermal_csv
    if args.rgb_video:
        data_files['rgb_video'] = args.rgb_video
    if args.rgb_frames_csv:
        data_files['rgb_frames_csv'] = args.rgb_frames_csv
    
    # Run tests
    runner = TestRunner()
    all_passed = runner.run_all(
        data_files=data_files if data_files else None,
        session_dir=args.session_dir
    )
    
    # Exit with appropriate code
    sys.exit(0 if all_passed else 1)


if __name__ == "__main__":
    main()
