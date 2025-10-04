"""
Utility functions for thesis evaluation tests
Provides data generation, validation, and reporting utilities
"""

import csv
import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple, Any, Optional


class SensorDataGenerator:
    """Generate simulated sensor data for testing"""

    @staticmethod
    def generate_gsr_samples(
        start_time_ms: int,
        duration_seconds: int,
        sampling_rate: int = 128,
        base_value: float = 15.0
    ) -> List[Dict[str, Any]]:
        """Generate GSR samples at specified rate"""
        samples = []
        interval_ms = 1000.0 / sampling_rate

        for i in range(sampling_rate * duration_seconds):
            timestamp = int(start_time_ms + (i * interval_ms))
            value = base_value + (i % 100) * 0.05

            samples.append({
                "timestamp": timestamp,
                "gsr_value": value,
                "conductance": 1.0 / value,
                "sample_index": i
            })

        return samples

    @staticmethod
    def generate_thermal_frames(
        start_time_ms: int,
        duration_seconds: int,
        frame_rate: int = 10
    ) -> List[Dict[str, Any]]:
        """Generate thermal camera frames at specified rate"""
        frames = []
        interval_ms = 1000.0 / frame_rate

        for i in range(frame_rate * duration_seconds):
            timestamp = int(start_time_ms + (i * interval_ms))

            frames.append({
                "timestamp": timestamp,
                "frame_index": i,
                "min_temp": 18.0 + (i % 5) * 0.5,
                "max_temp": 35.0 + (i % 10) * 0.3,
                "avg_temp": 25.0 + (i % 7) * 0.2
            })

        return frames

    @staticmethod
    def generate_rgb_frames(
        start_time_ms: int,
        duration_seconds: int,
        frame_rate: int = 30
    ) -> List[Dict[str, Any]]:
        """Generate RGB camera frames at specified rate"""
        frames = []
        interval_ms = 1000.0 / frame_rate

        for i in range(frame_rate * duration_seconds):
            timestamp = int(start_time_ms + (i * interval_ms))

            frames.append({
                "timestamp": timestamp,
                "frame_number": i,
                "resolution": "3840x2160"
            })

        return frames

    @staticmethod
    def inject_synthetic_event(
        gsr_samples: List[Dict[str, Any]],
        thermal_frames: List[Dict[str, Any]],
        rgb_frames: List[Dict[str, Any]],
        event_time_ms: int,
        marker_type: str = "spike"
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]], List[Dict[str, Any]]]:
        """Inject a synchronized marker event across all sensor streams"""

        gsr_with_event = []
        for sample in gsr_samples:
            modified_sample = sample.copy()
            if abs(sample["timestamp"] - event_time_ms) < 50:
                if marker_type == "spike":
                    modified_sample["gsr_value"] = sample["gsr_value"] * 1.5
                modified_sample["event_marker"] = "SYNC_EVENT"
            gsr_with_event.append(modified_sample)

        thermal_with_event = []
        for frame in thermal_frames:
            modified_frame = frame.copy()
            if abs(frame["timestamp"] - event_time_ms) < 50:
                modified_frame["event_marker"] = "SYNC_EVENT"
            thermal_with_event.append(modified_frame)

        rgb_with_event = []
        for frame in rgb_frames:
            modified_frame = frame.copy()
            if abs(frame["timestamp"] - event_time_ms) < 50:
                modified_frame["event_marker"] = "SYNC_EVENT"
            rgb_with_event.append(modified_frame)

        return gsr_with_event, thermal_with_event, rgb_with_event


class SynchronizationValidator:
    """Validate synchronization across sensor streams"""

    @staticmethod
    def find_sync_events(
        sensor_data: Dict[str, List[Dict[str, Any]]],
        time_tolerance_ms: int = 100
    ) -> List[Dict[str, Any]]:
        """Find synchronization events across all sensor streams"""
        sync_events = []
        processed_times = set()

        all_timestamps = []
        for sensor, data in sensor_data.items():
            for sample in data:
                if "event_marker" in sample:
                    all_timestamps.append({
                        "sensor": sensor,
                        "timestamp": sample["timestamp"],
                        "data": sample
                    })

        all_timestamps.sort(key=lambda x: x["timestamp"])

        for ts in all_timestamps:
            base_time = ts["timestamp"]
            
            if base_time in processed_times:
                continue
            
            matching_timestamps = [ts]
            matching_sensors = [ts["sensor"]]

            for other_ts in all_timestamps:
                if other_ts["sensor"] not in matching_sensors:
                    if abs(other_ts["timestamp"] - base_time) <= time_tolerance_ms:
                        matching_timestamps.append(other_ts)
                        matching_sensors.append(other_ts["sensor"])

            if len(matching_sensors) >= 2:
                timestamps_in_group = [mt["timestamp"] for mt in matching_timestamps]
                time_spread = max(timestamps_in_group) - min(timestamps_in_group)
                
                sync_events.append({
                    "base_timestamp": base_time,
                    "sensors_in_sync": list(set(matching_sensors)),
                    "time_spread_ms": time_spread
                })
                
                for mt in matching_timestamps:
                    processed_times.add(mt["timestamp"])

        return sync_events

    @staticmethod
    def calculate_time_alignment(
        timestamps: List[int],
        tolerance_ms: int = 100
    ) -> Dict[str, Any]:
        """Calculate time alignment statistics"""
        if not timestamps:
            return {"aligned": False, "error": "No timestamps provided"}

        min_ts = min(timestamps)
        max_ts = max(timestamps)
        spread = max_ts - min_ts

        return {
            "aligned": spread <= tolerance_ms,
            "min_timestamp": min_ts,
            "max_timestamp": max_ts,
            "time_spread_ms": spread,
            "tolerance_ms": tolerance_ms,
            "within_tolerance": spread <= tolerance_ms
        }


class SessionDurationAnalyzer:
    """Analyze session duration consistency across sensors"""

    @staticmethod
    def extract_session_boundaries(
        sensor_data: Dict[str, List[Dict[str, Any]]]
    ) -> Dict[str, Dict[str, int]]:
        """Extract start and end times for each sensor stream"""
        boundaries = {}

        for sensor, data in sensor_data.items():
            if not data:
                boundaries[sensor] = {
                    "start_time": None,
                    "end_time": None,
                    "duration_ms": 0,
                    "sample_count": 0
                }
                continue

            timestamps = [sample["timestamp"] for sample in data]
            start_time = min(timestamps)
            end_time = max(timestamps)

            boundaries[sensor] = {
                "start_time": start_time,
                "end_time": end_time,
                "duration_ms": end_time - start_time,
                "sample_count": len(data)
            }

        return boundaries

    @staticmethod
    def validate_duration_consistency(
        boundaries: Dict[str, Dict[str, int]],
        tolerance_ms: int = 1000
    ) -> Dict[str, Any]:
        """Validate that all sensor streams have consistent durations"""
        start_times = [b["start_time"] for b in boundaries.values() if b["start_time"]]
        end_times = [b["end_time"] for b in boundaries.values() if b["end_time"]]
        durations = [b["duration_ms"] for b in boundaries.values()]

        if not start_times or not end_times:
            return {
                "consistent": False,
                "error": "Missing timestamp data"
            }

        start_spread = max(start_times) - min(start_times)
        end_spread = max(end_times) - min(end_times)
        duration_variance = max(durations) - min(durations)

        return {
            "consistent": start_spread <= tolerance_ms and end_spread <= tolerance_ms,
            "start_time_spread_ms": start_spread,
            "end_time_spread_ms": end_spread,
            "duration_variance_ms": duration_variance,
            "tolerance_ms": tolerance_ms,
            "sensors_analyzed": list(boundaries.keys()),
            "start_aligned": start_spread <= tolerance_ms,
            "end_aligned": end_spread <= tolerance_ms
        }


class OutputGenerator:
    """Generate test output artifacts"""

    def __init__(self, output_dir: str = "thesis_evaluation/outputs"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def save_sync_event_log(
        self,
        sync_events: List[Dict[str, Any]],
        filename: str = "sync_events.csv"
    ):
        """Save synchronization events to CSV"""
        output_path = self.output_dir / filename

        with open(output_path, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["base_timestamp", "sensors_in_sync", "time_spread_ms"])

            for event in sync_events:
                writer.writerow([
                    event["base_timestamp"],
                    ",".join(event["sensors_in_sync"]),
                    event["time_spread_ms"]
                ])

        return str(output_path)

    def save_duration_report(
        self,
        boundaries: Dict[str, Dict[str, int]],
        consistency: Dict[str, Any],
        filename: str = "session_duration_report.csv"
    ):
        """Save session duration analysis to CSV"""
        output_path = self.output_dir / filename

        with open(output_path, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow([
                "sensor", "start_time", "end_time", "duration_ms", "sample_count"
            ])

            for sensor, data in boundaries.items():
                writer.writerow([
                    sensor,
                    data["start_time"],
                    data["end_time"],
                    data["duration_ms"],
                    data["sample_count"]
                ])

            writer.writerow([])
            writer.writerow(["Consistency Analysis"])
            writer.writerow(["metric", "value"])
            writer.writerow(["consistent", consistency["consistent"]])
            writer.writerow(["start_time_spread_ms", consistency["start_time_spread_ms"]])
            writer.writerow(["end_time_spread_ms", consistency["end_time_spread_ms"]])
            writer.writerow(["duration_variance_ms", consistency["duration_variance_ms"]])

        return str(output_path)

    def save_combined_timeline(
        self,
        sensor_data: Dict[str, List[Dict[str, Any]]],
        filename: str = "combined_timeline.json"
    ):
        """Save combined sensor timeline to JSON"""
        output_path = self.output_dir / filename

        timeline = {
            "timestamp": datetime.now().isoformat(),
            "sensors": {}
        }

        for sensor, data in sensor_data.items():
            timeline["sensors"][sensor] = {
                "sample_count": len(data),
                "samples": data[:10]
            }

        with open(output_path, 'w') as f:
            json.dump(timeline, f, indent=2)

        return str(output_path)
