#!/usr/bin/env python3
"""
Sensor Start Time Alignment Test

This test measures the timestamp at which each sensor actually begins logging data
when a recording START command is issued. It records the startup latency for each
sensor (GSR, thermal, RGB) relative to the START trigger to validate that all
modalities initiate nearly simultaneously.

Test Type: Multi-sensor timing coordination
Output: Synchronized log/table of sensor start-up latencies (ms)
Subsystem: Multi-sensor timing coordination
Thesis Chapters: Chapter 5 (timing alignment), Chapter 6 (startup delays)
"""

import sys
import os
import time
import csv
import json
import statistics
from datetime import datetime
from typing import List, Dict, Optional
from dataclasses import dataclass


@dataclass
class SensorStartEvent:
    """Records when a sensor started after START command"""
    sensor_name: str
    start_trigger_time_ms: int
    first_sample_time_ms: int
    startup_latency_ms: int
    sensor_type: str
    session_id: str


class SimulatedSensor:
    """Simulates a sensor with configurable startup delay"""
    
    def __init__(
        self,
        name: str,
        sensor_type: str,
        startup_delay_ms: float,
        sampling_rate_hz: float = 10.0
    ):
        self.name = name
        self.sensor_type = sensor_type
        self.startup_delay_ms = startup_delay_ms
        self.sampling_rate_hz = sampling_rate_hz
        self.is_recording = False
        self.first_sample_time = None
        
    def start_recording(self, trigger_time_ms: int):
        """Start recording with simulated delay"""
        time.sleep(self.startup_delay_ms / 1000.0)
        self.first_sample_time = int(time.time() * 1000)
        self.is_recording = True
        return self.first_sample_time
    
    def stop_recording(self):
        """Stop recording"""
        self.is_recording = False
        self.first_sample_time = None


class SensorStartAlignmentTest:
    """Test harness for sensor start time alignment measurement"""
    
    def __init__(self, output_dir: str = "."):
        self.output_dir = output_dir
        self.results: List[SensorStartEvent] = []
        
    def run_synthetic_test(
        self,
        num_trials: int = 10,
        use_realistic_delays: bool = True
    ) -> List[SensorStartEvent]:
        """
        Run synthetic sensor start alignment test.
        
        Args:
            num_trials: Number of recording sessions to test
            use_realistic_delays: Use realistic sensor startup delays
            
        Returns:
            List of sensor start events
        """
        print("Starting synthetic sensor start alignment test")
        print(f"Number of trials: {num_trials}")
        print("-" * 60)
        
        results = []
        
        for trial in range(num_trials):
            session_id = f"session_{trial+1:03d}"
            print(f"\nTrial {trial+1}/{num_trials} (Session: {session_id})")
            
            trial_results = self._run_single_trial(session_id, use_realistic_delays)
            results.extend(trial_results)
            
            self._print_trial_results(trial_results)
        
        self.results = results
        return results
    
    def _run_single_trial(
        self,
        session_id: str,
        use_realistic_delays: bool
    ) -> List[SensorStartEvent]:
        """Run a single trial with all sensors"""
        import random
        
        # Create simulated sensors with realistic or random delays
        if use_realistic_delays:
            sensors = [
                SimulatedSensor("GSR_Shimmer", "physiological", 50.0, 51.2),
                SimulatedSensor("Thermal_TOPDON", "thermal", 120.0, 25.0),
                SimulatedSensor("RGB_Camera", "camera", 80.0, 30.0),
            ]
        else:
            sensors = [
                SimulatedSensor("GSR_Shimmer", "physiological", 
                              random.uniform(30, 100), 51.2),
                SimulatedSensor("Thermal_TOPDON", "thermal", 
                              random.uniform(80, 200), 25.0),
                SimulatedSensor("RGB_Camera", "camera", 
                              random.uniform(50, 150), 30.0),
            ]
        
        # Record START trigger time
        trigger_time_ms = int(time.time() * 1000)
        print(f"  START command issued at: {trigger_time_ms}")
        
        # Start all sensors and measure their actual start times
        trial_results = []
        for sensor in sensors:
            first_sample_time = sensor.start_recording(trigger_time_ms)
            startup_latency = first_sample_time - trigger_time_ms
            
            event = SensorStartEvent(
                sensor_name=sensor.name,
                start_trigger_time_ms=trigger_time_ms,
                first_sample_time_ms=first_sample_time,
                startup_latency_ms=startup_latency,
                sensor_type=sensor.sensor_type,
                session_id=session_id
            )
            
            trial_results.append(event)
            sensor.stop_recording()
        
        return trial_results
    
    def _print_trial_results(self, trial_results: List[SensorStartEvent]):
        """Print results for a single trial"""
        print("  Sensor startup latencies:")
        for event in trial_results:
            print(f"    {event.sensor_name:20s}: {event.startup_latency_ms:6d}ms")
        
        if len(trial_results) > 1:
            latencies = [e.startup_latency_ms for e in trial_results]
            max_diff = max(latencies) - min(latencies)
            print(f"  Max startup difference: {max_diff}ms")
    
    def analyze_real_hardware_logs(
        self,
        log_directory: str,
        session_id: str
    ) -> Optional[List[SensorStartEvent]]:
        """
        Analyze real hardware sensor logs to determine start times.
        
        This reads actual sensor data files from a recording session and
        extracts the first timestamp from each sensor to calculate startup
        latencies.
        
        Args:
            log_directory: Directory containing sensor data files
            session_id: Session identifier
            
        Returns:
            List of sensor start events or None if failed
        """
        print(f"\nAnalyzing real hardware logs for session: {session_id}")
        print(f"Log directory: {log_directory}")
        print("-" * 60)
        
        if not os.path.exists(log_directory):
            print(f"Error: Log directory does not exist: {log_directory}")
            return None
        
        sensor_files = {
            'gsr_data.csv': ('GSR_Shimmer', 'physiological'),
            'thermal_data.csv': ('Thermal_TOPDON', 'thermal'),
            'rgb_timestamps.csv': ('RGB_Camera', 'camera'),
        }
        
        events = []
        first_timestamps = {}
        
        # Extract first timestamp from each sensor file
        for filename, (sensor_name, sensor_type) in sensor_files.items():
            filepath = os.path.join(log_directory, filename)
            
            if not os.path.exists(filepath):
                print(f"Warning: Sensor file not found: {filename}")
                continue
            
            first_ts = self._extract_first_timestamp(filepath, sensor_name)
            if first_ts:
                first_timestamps[sensor_name] = first_ts
                print(f"  {sensor_name:20s}: First sample at {first_ts}ms")
        
        if not first_timestamps:
            print("Error: No sensor timestamps found")
            return None
        
        # Use earliest timestamp as the reference (START trigger approximation)
        reference_time = min(first_timestamps.values())
        print(f"\nReference time (earliest sensor): {reference_time}ms")
        
        # Calculate startup latencies relative to reference
        print("\nStartup latencies relative to first sensor:")
        for filename, (sensor_name, sensor_type) in sensor_files.items():
            if sensor_name in first_timestamps:
                first_ts = first_timestamps[sensor_name]
                latency = first_ts - reference_time
                
                event = SensorStartEvent(
                    sensor_name=sensor_name,
                    start_trigger_time_ms=reference_time,
                    first_sample_time_ms=first_ts,
                    startup_latency_ms=latency,
                    sensor_type=sensor_type,
                    session_id=session_id
                )
                events.append(event)
                print(f"  {sensor_name:20s}: +{latency}ms")
        
        return events
    
    def _extract_first_timestamp(
        self,
        filepath: str,
        sensor_name: str
    ) -> Optional[int]:
        """Extract first timestamp from a sensor data CSV file"""
        try:
            with open(filepath, 'r') as f:
                reader = csv.DictReader(f)
                
                # Get first data row
                first_row = next(reader, None)
                if not first_row:
                    return None
                
                # Try different timestamp column names
                timestamp_columns = [
                    'timestamp', 'timestampMs', 'synchronizedTimestampMs',
                    'time', 'timestamp_ms', 'ts'
                ]
                
                for col in timestamp_columns:
                    if col in first_row:
                        return int(float(first_row[col]))
                
                print(f"Warning: No timestamp column found in {filepath}")
                print(f"Available columns: {list(first_row.keys())}")
                return None
                
        except Exception as e:
            print(f"Error reading {filepath}: {e}")
            return None
    
    def save_results_to_csv(
        self,
        filename: str = "sensor_start_alignment_results.csv"
    ):
        """Save test results to CSV file"""
        filepath = os.path.join(self.output_dir, filename)
        
        if not self.results:
            print("No results to save")
            return None
        
        with open(filepath, 'w', newline='') as csvfile:
            fieldnames = [
                'session_id', 'sensor_name', 'sensor_type',
                'start_trigger_time_ms', 'first_sample_time_ms',
                'startup_latency_ms'
            ]
            
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            
            for event in self.results:
                writer.writerow({
                    'session_id': event.session_id,
                    'sensor_name': event.sensor_name,
                    'sensor_type': event.sensor_type,
                    'start_trigger_time_ms': event.start_trigger_time_ms,
                    'first_sample_time_ms': event.first_sample_time_ms,
                    'startup_latency_ms': event.startup_latency_ms
                })
        
        print(f"\nResults saved to CSV: {filepath}")
        return filepath
    
    def save_results_to_json(
        self,
        filename: str = "sensor_start_alignment_results.json"
    ):
        """Save test results to JSON file"""
        filepath = os.path.join(self.output_dir, filename)
        
        if not self.results:
            print("No results to save")
            return None
        
        results_dict = {
            'test_type': 'sensor_start_alignment',
            'timestamp': datetime.now().isoformat(),
            'num_sessions': len(set(e.session_id for e in self.results)),
            'num_events': len(self.results),
            'results': [
                {
                    'session_id': e.session_id,
                    'sensor_name': e.sensor_name,
                    'sensor_type': e.sensor_type,
                    'start_trigger_time_ms': e.start_trigger_time_ms,
                    'first_sample_time_ms': e.first_sample_time_ms,
                    'startup_latency_ms': e.startup_latency_ms
                }
                for e in self.results
            ]
        }
        
        with open(filepath, 'w') as jsonfile:
            json.dump(results_dict, jsonfile, indent=2)
        
        print(f"Results saved to JSON: {filepath}")
        return filepath
    
    def generate_summary_report(self):
        """Generate statistical summary of sensor start alignment"""
        if not self.results:
            print("No results to analyze")
            return
        
        print("\n" + "=" * 60)
        print("SENSOR START ALIGNMENT TEST SUMMARY")
        print("=" * 60)
        
        # Group by sensor
        sensors = {}
        for event in self.results:
            if event.sensor_name not in sensors:
                sensors[event.sensor_name] = []
            sensors[event.sensor_name].append(event.startup_latency_ms)
        
        print(f"\nTotal sessions tested: {len(set(e.session_id for e in self.results))}")
        print(f"Total sensor readings: {len(self.results)}")
        print()
        
        # Per-sensor statistics
        print("Per-Sensor Startup Latency Statistics:")
        print()
        
        for sensor_name, latencies in sorted(sensors.items()):
            print(f"{sensor_name}:")
            print(f"  Mean: {statistics.mean(latencies):.2f}ms")
            print(f"  Median: {statistics.median(latencies):.2f}ms")
            print(f"  Std Dev: {statistics.stdev(latencies) if len(latencies) > 1 else 0:.2f}ms")
            print(f"  Min: {min(latencies):.2f}ms")
            print(f"  Max: {max(latencies):.2f}ms")
            print()
        
        # Cross-sensor alignment analysis
        sessions = {}
        for event in self.results:
            if event.session_id not in sessions:
                sessions[event.session_id] = []
            sessions[event.session_id].append(event)
        
        max_diffs = []
        for session_id, events in sessions.items():
            if len(events) > 1:
                latencies = [e.startup_latency_ms for e in events]
                max_diff = max(latencies) - min(latencies)
                max_diffs.append(max_diff)
        
        if max_diffs:
            print("Cross-Sensor Synchronization:")
            print(f"  Mean max difference: {statistics.mean(max_diffs):.2f}ms")
            print(f"  Median max difference: {statistics.median(max_diffs):.2f}ms")
            print(f"  Worst case difference: {max(max_diffs):.2f}ms")
            print(f"  Best case difference: {min(max_diffs):.2f}ms")
            print()
            
            # Check if synchronization is acceptable (within 200ms)
            acceptable_threshold_ms = 200
            acceptable_count = sum(1 for d in max_diffs if d <= acceptable_threshold_ms)
            acceptable_pct = (acceptable_count / len(max_diffs)) * 100
            
            print(f"Synchronization Quality:")
            print(f"  Sessions within {acceptable_threshold_ms}ms: {acceptable_count}/{len(max_diffs)} ({acceptable_pct:.1f}%)")
            
            if acceptable_pct >= 90:
                print(f"  Quality: EXCELLENT")
            elif acceptable_pct >= 70:
                print(f"  Quality: GOOD")
            elif acceptable_pct >= 50:
                print(f"  Quality: ACCEPTABLE")
            else:
                print(f"  Quality: POOR - Needs improvement")
        
        print("=" * 60)


def main():
    """Main entry point for sensor start alignment test"""
    print("Sensor Start Time Alignment Test")
    print("=" * 60)
    print()
    
    output_dir = os.path.dirname(__file__)
    if not output_dir:
        output_dir = "."
    
    test = SensorStartAlignmentTest(output_dir)
    
    # Run synthetic test
    print("Running SYNTHETIC test with simulated sensors...")
    print()
    test.run_synthetic_test(num_trials=20, use_realistic_delays=True)
    test.save_results_to_csv("sensor_start_alignment_synthetic.csv")
    test.save_results_to_json("sensor_start_alignment_synthetic.json")
    test.generate_summary_report()
    
    # Instructions for real hardware test
    print("\n" + "=" * 60)
    print("REAL HARDWARE TEST INSTRUCTIONS")
    print("=" * 60)
    print()
    print("To test with real hardware:")
    print("1. Start a recording session on the Android device")
    print("2. Let it run for a few seconds and stop")
    print("3. Pull the session data from Android:")
    print("   adb pull /sdcard/Android/data/<package>/files/<session_id>/ ./real_session_data/")
    print("4. Run this script with the session directory:")
    print("   python test_sensor_start_alignment.py ./real_session_data <session_id>")
    print()
    print("Example:")
    print("   python test_sensor_start_alignment.py ./real_session_data session_20240101_120000")
    
    # If real hardware log directory provided, analyze it
    if len(sys.argv) > 1:
        log_dir = sys.argv[1]
        session_id = sys.argv[2] if len(sys.argv) > 2 else "unknown_session"
        
        print("\n" + "=" * 60)
        print("Analyzing REAL HARDWARE logs...")
        print("=" * 60)
        
        real_events = test.analyze_real_hardware_logs(log_dir, session_id)
        if real_events:
            test.results = real_events
            test.save_results_to_csv("sensor_start_alignment_real_hardware.csv")
            test.save_results_to_json("sensor_start_alignment_real_hardware.json")
            test.generate_summary_report()


if __name__ == "__main__":
    main()
