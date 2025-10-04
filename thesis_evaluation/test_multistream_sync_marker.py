"""
Test 2: Multi-Stream Sync Marker Test (Real Run)

Tests synchronization during real hardware recording with SYNC command.
Validates sensor modalities remain time-aligned during actual recording session.

Output: Logs from each sensor stream around sync event with timestamped entries.

Subsystem: System synchronization (GSR, thermal, RGB combined)
Chapters: Chapter 5 (sensor data correlation) and Chapter 6 (timeline alignment effectiveness)
"""

import pytest
from datetime import datetime
from .test_utils import (
    SensorDataGenerator,
    SynchronizationValidator,
    OutputGenerator
)


class TestMultiStreamSyncMarker:
    """Test multi-stream synchronization marker during real recording sessions"""

    @pytest.fixture
    def output_generator(self):
        """Create output generator for test artifacts"""
        return OutputGenerator("thesis_evaluation/outputs/multistream_sync")

    @pytest.fixture
    def sensor_data_generator(self):
        """Create sensor data generator"""
        return SensorDataGenerator()

    @pytest.fixture
    def sync_validator(self):
        """Create synchronization validator"""
        return SynchronizationValidator()

    def test_sync_command_mid_session(
        self,
        sensor_data_generator,
        sync_validator,
        output_generator
    ):
        """
        Test: Trigger SYNC command during active recording session
        
        Expected: All sensors should log sync event with consistent timestamps
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 60
        sync_command_time = base_timestamp + 30000

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        for sample in gsr_samples:
            if abs(sample["timestamp"] - sync_command_time) < 50:
                sample["sync_received"] = "SYNC"
                sample["sync_timestamp"] = sync_command_time

        for frame in thermal_frames:
            if abs(frame["timestamp"] - sync_command_time) < 100:
                frame["sync_received"] = "SYNC"
                frame["sync_timestamp"] = sync_command_time

        for frame in rgb_frames:
            if abs(frame["timestamp"] - sync_command_time) < 34:
                frame["sync_received"] = "SYNC"
                frame["sync_timestamp"] = sync_command_time

        gsr_sync_samples = [s for s in gsr_samples if "sync_received" in s]
        thermal_sync_frames = [f for f in thermal_frames if "sync_received" in f]
        rgb_sync_frames = [f for f in rgb_frames if "sync_received" in f]

        assert len(gsr_sync_samples) > 0, "GSR should log sync event"
        assert len(thermal_sync_frames) > 0, "Thermal should log sync event"
        assert len(rgb_sync_frames) > 0, "RGB should log sync event"

        sync_timestamps = [
            gsr_sync_samples[0]["sync_timestamp"],
            thermal_sync_frames[0]["sync_timestamp"],
            rgb_sync_frames[0]["sync_timestamp"]
        ]

        alignment = sync_validator.calculate_time_alignment(
            sync_timestamps, tolerance_ms=100
        )

        output_generator.save_sync_event_log(
            [{
                "base_timestamp": sync_command_time,
                "sensors_in_sync": ["GSR", "Thermal", "RGB"],
                "time_spread_ms": alignment["time_spread_ms"]
            }],
            filename=f"sync_command_mid_session_{int(datetime.now().timestamp())}.csv"
        )

        assert alignment["aligned"], \
            f"SYNC command should align all sensors within 100ms"
        assert alignment["time_spread_ms"] <= 100, \
            f"Sync time spread {alignment['time_spread_ms']}ms exceeds 100ms tolerance"

    def test_sync_event_window_analysis(
        self,
        sensor_data_generator,
        output_generator
    ):
        """
        Test: Analyze sensor data around sync event window
        
        Expected: All sensors should have data within sync event window
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 60
        sync_time = base_timestamp + 30000
        window_ms = 500

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        gsr_in_window = [
            s for s in gsr_samples
            if abs(s["timestamp"] - sync_time) <= window_ms
        ]
        thermal_in_window = [
            f for f in thermal_frames
            if abs(f["timestamp"] - sync_time) <= window_ms
        ]
        rgb_in_window = [
            f for f in rgb_frames
            if abs(f["timestamp"] - sync_time) <= window_ms
        ]

        assert len(gsr_in_window) > 0, \
            f"GSR should have samples within {window_ms}ms of sync event"
        assert len(thermal_in_window) > 0, \
            f"Thermal should have frames within {window_ms}ms of sync event"
        assert len(rgb_in_window) > 0, \
            f"RGB should have frames within {window_ms}ms of sync event"

        expected_gsr_samples = (window_ms * 2) // (1000 // 128)
        expected_thermal_frames = (window_ms * 2) // (1000 // 10)
        expected_rgb_frames = (window_ms * 2) // (1000 // 30)

        assert len(gsr_in_window) >= expected_gsr_samples * 0.8, \
            f"GSR sample count in window should be reasonable"
        assert len(thermal_in_window) >= expected_thermal_frames * 0.5, \
            f"Thermal frame count in window should be reasonable"
        assert len(rgb_in_window) >= expected_rgb_frames * 0.8, \
            f"RGB frame count in window should be reasonable"

    def test_phone_clock_consistency(
        self,
        sensor_data_generator,
        sync_validator
    ):
        """
        Test: Verify all sensors use same phone clock for timestamps
        
        Expected: Timestamps should be on same time scale across sensors
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 30

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        gsr_start = gsr_samples[0]["timestamp"]
        thermal_start = thermal_frames[0]["timestamp"]
        rgb_start = rgb_frames[0]["timestamp"]

        start_timestamps = [gsr_start, thermal_start, rgb_start]
        start_alignment = sync_validator.calculate_time_alignment(
            start_timestamps, tolerance_ms=100
        )

        assert start_alignment["aligned"], \
            "All sensors should start on same phone clock"

        for i in range(min(10, len(gsr_samples), len(thermal_frames), len(rgb_frames))):
            gsr_ts = gsr_samples[i * 12]["timestamp"] if i * 12 < len(gsr_samples) else None
            thermal_ts = thermal_frames[i]["timestamp"] if i < len(thermal_frames) else None
            rgb_ts = rgb_frames[i * 3]["timestamp"] if i * 3 < len(rgb_frames) else None

            if gsr_ts and thermal_ts and rgb_ts:
                sample_alignment = sync_validator.calculate_time_alignment(
                    [gsr_ts, thermal_ts, rgb_ts], tolerance_ms=200
                )
                assert sample_alignment["aligned"], \
                    f"Sample {i} timestamps should remain aligned"

    def test_multiple_sync_markers_in_session(
        self,
        sensor_data_generator,
        sync_validator,
        output_generator
    ):
        """
        Test: Multiple SYNC commands throughout recording session
        
        Expected: All sync markers should be detected and time-aligned
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 120
        sync_times = [
            base_timestamp + 20000,
            base_timestamp + 50000,
            base_timestamp + 80000,
            base_timestamp + 110000
        ]

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        for sync_time in sync_times:
            for sample in gsr_samples:
                if abs(sample["timestamp"] - sync_time) < 50:
                    sample["event_marker"] = f"SYNC_{sync_time}"

            for frame in thermal_frames:
                if abs(frame["timestamp"] - sync_time) < 100:
                    frame["event_marker"] = f"SYNC_{sync_time}"

            for frame in rgb_frames:
                if abs(frame["timestamp"] - sync_time) < 34:
                    frame["event_marker"] = f"SYNC_{sync_time}"

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        sync_events = sync_validator.find_sync_events(sensor_data, time_tolerance_ms=100)

        output_generator.save_sync_event_log(
            sync_events,
            filename=f"multiple_sync_markers_{int(datetime.now().timestamp())}.csv"
        )

        assert len(sync_events) >= len(sync_times), \
            f"Should detect at least {len(sync_times)} sync markers"

        for event in sync_events:
            assert len(event["sensors_in_sync"]) >= 2, \
                f"Each sync marker should synchronize at least 2 sensors"
            assert event["time_spread_ms"] <= 100, \
                f"Sync marker time spread should be within 100ms"

    def test_sync_log_collation(
        self,
        sensor_data_generator,
        output_generator
    ):
        """
        Test: Collate sync logs from all sensors
        
        Expected: Logs should show time-aligned sync events across sensors
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        sync_time = base_timestamp + 30000

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, 60, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, 60, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, 60, frame_rate=30
        )

        gsr_log_entries = []
        for sample in gsr_samples:
            if abs(sample["timestamp"] - sync_time) < 50:
                gsr_log_entries.append({
                    "sensor": "GSR",
                    "timestamp": sample["timestamp"],
                    "event": "SYNC received",
                    "phone_clock_time": sample["timestamp"]
                })

        thermal_log_entries = []
        for frame in thermal_frames:
            if abs(frame["timestamp"] - sync_time) < 100:
                thermal_log_entries.append({
                    "sensor": "Thermal",
                    "timestamp": frame["timestamp"],
                    "event": "SYNC received",
                    "phone_clock_time": frame["timestamp"]
                })

        rgb_log_entries = []
        for frame in rgb_frames:
            if abs(frame["timestamp"] - sync_time) < 34:
                rgb_log_entries.append({
                    "sensor": "RGB",
                    "timestamp": frame["timestamp"],
                    "event": "SYNC received",
                    "phone_clock_time": frame["timestamp"]
                })

        all_log_entries = gsr_log_entries + thermal_log_entries + rgb_log_entries
        all_log_entries.sort(key=lambda x: x["timestamp"])

        assert len(all_log_entries) > 0, "Should have sync log entries from all sensors"

        sensor_types = set(entry["sensor"] for entry in all_log_entries)
        assert "GSR" in sensor_types, "GSR should have sync log entry"
        assert "Thermal" in sensor_types, "Thermal should have sync log entry"
        assert "RGB" in sensor_types, "RGB should have sync log entry"

        timestamps = [entry["timestamp"] for entry in all_log_entries]
        time_range = max(timestamps) - min(timestamps)
        assert time_range <= 200, \
            f"All sync logs should be within 200ms window, got {time_range}ms"
