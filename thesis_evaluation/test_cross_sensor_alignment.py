"""
Test 1: Cross-Sensor Timeline Alignment (Synthetic Event)

Tests synchronized reference event injection across all data streams.
Validates that all sensors record events simultaneously within acceptable tolerance.

Output: Combined log/chart showing GSR spike, thermal marker, and RGB timestamp
        at injected event time.

Subsystem: Multi-sensor integration (timing consistency)
Chapters: Chapter 5 (synchronized sensor behavior) and Chapter 6 (cross-modal synchronization)
"""

import pytest
from datetime import datetime
from .test_utils import (
    SensorDataGenerator,
    SynchronizationValidator,
    OutputGenerator
)


class TestCrossSensorAlignment:
    """Test cross-sensor timeline alignment with synthetic event injection"""

    @pytest.fixture
    def output_generator(self):
        """Create output generator for test artifacts"""
        return OutputGenerator("thesis_evaluation/outputs/cross_sensor_alignment")

    @pytest.fixture
    def sensor_data_generator(self):
        """Create sensor data generator"""
        return SensorDataGenerator()

    @pytest.fixture
    def sync_validator(self):
        """Create synchronization validator"""
        return SynchronizationValidator()

    def test_inject_synthetic_event_at_10_seconds(
        self,
        sensor_data_generator,
        sync_validator,
        output_generator
    ):
        """
        Test: Inject a synchronized reference event at t=10.0s across all sensors
        
        Expected: All sensors should record the event within 100ms tolerance
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 15
        event_time_ms = base_timestamp + 10000

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        gsr_with_event, thermal_with_event, rgb_with_event = \
            sensor_data_generator.inject_synthetic_event(
                gsr_samples, thermal_frames, rgb_frames,
                event_time_ms, marker_type="spike"
            )

        sensor_data = {
            "GSR": gsr_with_event,
            "Thermal": thermal_with_event,
            "RGB": rgb_with_event
        }

        sync_events = sync_validator.find_sync_events(sensor_data, time_tolerance_ms=100)

        output_generator.save_sync_event_log(
            sync_events,
            filename=f"synthetic_event_10s_{int(datetime.now().timestamp())}.csv"
        )
        output_generator.save_combined_timeline(
            sensor_data,
            filename=f"synthetic_event_timeline_{int(datetime.now().timestamp())}.json"
        )

        assert len(sync_events) > 0, "Should detect at least one synchronization event"

        event_at_10s = [e for e in sync_events if abs(e["base_timestamp"] - event_time_ms) < 100]
        assert len(event_at_10s) > 0, "Should detect event at 10 seconds"

        primary_event = event_at_10s[0]
        assert len(primary_event["sensors_in_sync"]) >= 2, \
            "At least 2 sensors should be synchronized at the event"
        assert primary_event["time_spread_ms"] <= 100, \
            f"Time spread should be within 100ms tolerance, got {primary_event['time_spread_ms']}ms"

    def test_multiple_synthetic_events(
        self,
        sensor_data_generator,
        sync_validator,
        output_generator
    ):
        """
        Test: Inject multiple synchronized events throughout the session
        
        Expected: All events should be detected with proper synchronization
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 30
        event_times = [
            base_timestamp + 5000,
            base_timestamp + 10000,
            base_timestamp + 15000,
            base_timestamp + 20000
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

        for event_time in event_times:
            gsr_samples, thermal_frames, rgb_frames = \
                sensor_data_generator.inject_synthetic_event(
                    gsr_samples, thermal_frames, rgb_frames,
                    event_time, marker_type="spike"
                )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        sync_events = sync_validator.find_sync_events(sensor_data, time_tolerance_ms=100)

        output_generator.save_sync_event_log(
            sync_events,
            filename=f"multiple_events_{int(datetime.now().timestamp())}.csv"
        )

        assert len(sync_events) >= len(event_times), \
            f"Should detect at least {len(event_times)} events, found {len(sync_events)}"

        for event in sync_events:
            assert event["time_spread_ms"] <= 100, \
                f"Event at {event['base_timestamp']} has time spread {event['time_spread_ms']}ms"

    def test_gsr_spike_detection_at_event(
        self,
        sensor_data_generator,
        output_generator
    ):
        """
        Test: Verify GSR spike is detected at synthetic event time
        
        Expected: GSR value should increase at event marker
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 15
        event_time_ms = base_timestamp + 10000

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )

        baseline_values = [s["gsr_value"] for s in gsr_samples[:100]]
        avg_baseline = sum(baseline_values) / len(baseline_values)

        gsr_with_event, _, _ = sensor_data_generator.inject_synthetic_event(
            gsr_samples, [], [],
            event_time_ms, marker_type="spike"
        )

        event_samples = [
            s for s in gsr_with_event
            if "event_marker" in s and s["event_marker"] == "SYNC_EVENT"
        ]

        assert len(event_samples) > 0, "Should have GSR samples with event marker"

        for sample in event_samples:
            assert sample["gsr_value"] > avg_baseline * 1.2, \
                f"GSR spike should be at least 20% above baseline at event"

    def test_thermal_frame_marker_at_event(
        self,
        sensor_data_generator
    ):
        """
        Test: Verify thermal frame marker is present at synthetic event time
        
        Expected: Thermal frame should have event marker at injection time
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 15
        event_time_ms = base_timestamp + 10000

        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )

        _, thermal_with_event, _ = sensor_data_generator.inject_synthetic_event(
            [], thermal_frames, [],
            event_time_ms
        )

        event_frames = [
            f for f in thermal_with_event
            if "event_marker" in f and f["event_marker"] == "SYNC_EVENT"
        ]

        assert len(event_frames) > 0, "Should have thermal frames with event marker"

        for frame in event_frames:
            time_diff = abs(frame["timestamp"] - event_time_ms)
            assert time_diff < 100, \
                f"Event frame timestamp should be within 100ms of event time, got {time_diff}ms"

    def test_rgb_timestamp_mark_at_event(
        self,
        sensor_data_generator
    ):
        """
        Test: Verify RGB frame timestamp mark at synthetic event time
        
        Expected: RGB frame should have event marker at injection time
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 15
        event_time_ms = base_timestamp + 10000

        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        _, _, rgb_with_event = sensor_data_generator.inject_synthetic_event(
            [], [], rgb_frames,
            event_time_ms
        )

        event_frames = [
            f for f in rgb_with_event
            if "event_marker" in f and f["event_marker"] == "SYNC_EVENT"
        ]

        assert len(event_frames) > 0, "Should have RGB frames with event marker"

        for frame in event_frames:
            time_diff = abs(frame["timestamp"] - event_time_ms)
            assert time_diff <= 34, \
                f"RGB frame timestamp should be within one frame (34ms) of event, got {time_diff}ms"

    def test_cross_modal_synchronization_tolerance(
        self,
        sensor_data_generator,
        sync_validator
    ):
        """
        Test: Validate synchronization tolerance across all modalities
        
        Expected: All sensors should be synchronized within specified tolerance
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        event_time_ms = base_timestamp + 10000

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, 15, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, 15, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, 15, frame_rate=30
        )

        gsr_with_event, thermal_with_event, rgb_with_event = \
            sensor_data_generator.inject_synthetic_event(
                gsr_samples, thermal_frames, rgb_frames,
                event_time_ms
            )

        event_timestamps = []
        for sample in gsr_with_event:
            if "event_marker" in sample:
                event_timestamps.append(sample["timestamp"])
                break

        for frame in thermal_with_event:
            if "event_marker" in frame:
                event_timestamps.append(frame["timestamp"])
                break

        for frame in rgb_with_event:
            if "event_marker" in frame:
                event_timestamps.append(frame["timestamp"])
                break

        alignment = sync_validator.calculate_time_alignment(
            event_timestamps, tolerance_ms=100
        )

        assert alignment["aligned"], \
            f"Sensors should be aligned within 100ms tolerance"
        assert alignment["time_spread_ms"] <= 100, \
            f"Time spread {alignment['time_spread_ms']}ms exceeds 100ms tolerance"
