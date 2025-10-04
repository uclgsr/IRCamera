"""
Test 3: Session Duration Consistency Check

Tests recording duration consistency across all sensor streams.
Validates all streams covered the same overall period.

Output: CSV/table with start/end timestamps from each sensor log.

Subsystem: Session controller (start/stop coordination)
Chapters: Chapter 5 (concurrent sensor operation) and Chapter 6 (sensor lag/lead analysis)

Tolerance Specifications:
    - Start time tolerance: ±100ms
    - End time tolerance: ±1000ms

Expected Sampling Rates:
    - GSR: 128 Hz
    - Thermal: 10 Hz
    - RGB: 30 Hz
"""

import pytest
from datetime import datetime
from .test_utils import (
    SensorDataGenerator,
    SessionDurationAnalyzer,
    OutputGenerator
)


class TestSessionDurationConsistency:
    """Test session duration consistency across all sensor streams"""

    @pytest.fixture
    def output_generator(self):
        """Create output generator for test artifacts"""
        return OutputGenerator("docs/thesis/evaluation/outputs/session_duration")

    @pytest.fixture
    def sensor_data_generator(self):
        """Create sensor data generator"""
        return SensorDataGenerator()

    @pytest.fixture
    def duration_analyzer(self):
        """Create session duration analyzer"""
        return SessionDurationAnalyzer()

    def test_session_start_time_consistency(
        self,
        sensor_data_generator,
        duration_analyzer,
        output_generator
    ):
        """
        Test: Compare session start times across all sensors
        
        Expected: All sensors should start within 100ms of each other
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp + 15, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp + 25, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)
        consistency = duration_analyzer.validate_duration_consistency(
            boundaries, tolerance_ms=100
        )

        output_generator.save_duration_report(
            boundaries,
            consistency,
            filename=f"start_time_consistency_{int(datetime.now().timestamp())}.csv"
        )

        assert consistency["start_aligned"], \
            f"Session start times should be aligned within 100ms, " \
            f"spread={consistency['start_time_spread_ms']}ms"

        start_times = [b["start_time"] for b in boundaries.values()]
        assert max(start_times) - min(start_times) <= 100, \
            "Start time spread exceeds 100ms tolerance"

    def test_session_end_time_consistency(
        self,
        sensor_data_generator,
        duration_analyzer,
        output_generator
    ):
        """
        Test: Compare session end times across all sensors
        
        Expected: All sensors should end within 100ms of STOP command
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300
        stop_command_time = base_timestamp + (duration_seconds * 1000)

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)
        consistency = duration_analyzer.validate_duration_consistency(
            boundaries, tolerance_ms=1000
        )

        output_generator.save_duration_report(
            boundaries,
            consistency,
            filename=f"end_time_consistency_{int(datetime.now().timestamp())}.csv"
        )

        assert consistency["end_aligned"], \
            f"Session end times should be aligned, spread={consistency['end_time_spread_ms']}ms"

        for sensor, data in boundaries.items():
            end_time_diff = abs(data["end_time"] - stop_command_time)
            assert end_time_diff <= 1000, \
                f"{sensor} end time differs from STOP command by {end_time_diff}ms"

    def test_session_duration_matching(
        self,
        sensor_data_generator,
        duration_analyzer
    ):
        """
        Test: Verify all sensors recorded for the same duration
        
        Expected: Duration variance should be minimal across sensors
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)

        durations = [b["duration_ms"] for b in boundaries.values()]
        expected_duration = duration_seconds * 1000

        for sensor, boundary in boundaries.items():
            duration_diff = abs(boundary["duration_ms"] - expected_duration)
            assert duration_diff <= 1000, \
                f"{sensor} duration differs by {duration_diff}ms from expected {expected_duration}ms"

        duration_variance = max(durations) - min(durations)
        assert duration_variance <= 1000, \
            f"Duration variance {duration_variance}ms exceeds 1000ms tolerance"

    def test_complete_session_report(
        self,
        sensor_data_generator,
        duration_analyzer,
        output_generator
    ):
        """
        Test: Generate complete session duration report
        
        Expected: Report should show start, end, and duration for all sensors
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp + 20, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp + 10, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)
        consistency = duration_analyzer.validate_duration_consistency(boundaries)

        report_path = output_generator.save_duration_report(
            boundaries,
            consistency,
            filename=f"complete_session_report_{int(datetime.now().timestamp())}.csv"
        )

        assert report_path is not None, "Should generate report file"

        for sensor, boundary in boundaries.items():
            assert boundary["start_time"] is not None, \
                f"{sensor} should have valid start time"
            assert boundary["end_time"] is not None, \
                f"{sensor} should have valid end time"
            assert boundary["duration_ms"] > 0, \
                f"{sensor} should have positive duration"
            assert boundary["sample_count"] > 0, \
                f"{sensor} should have samples"

    def test_detect_sensor_lag(
        self,
        sensor_data_generator,
        duration_analyzer
    ):
        """
        Test: Detect if any sensor lags behind others at session start
        
        Expected: Flag sensors that start more than 100ms after others
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp + 150, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp + 50, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)

        start_times = {
            sensor: boundary["start_time"]
            for sensor, boundary in boundaries.items()
        }

        min_start_time = min(start_times.values())
        lagging_sensors = {
            sensor: start_time - min_start_time
            for sensor, start_time in start_times.items()
            if start_time - min_start_time > 100
        }

        assert "Thermal" in lagging_sensors, \
            "Thermal sensor should be detected as lagging"
        assert lagging_sensors["Thermal"] == 150, \
            f"Thermal lag should be 150ms, got {lagging_sensors['Thermal']}ms"

    def test_detect_sensor_lead(
        self,
        sensor_data_generator,
        duration_analyzer
    ):
        """
        Test: Detect if any sensor stops early (leads at session end)
        
        Expected: Flag sensors that stop more than 100ms before others
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 300

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds - 1, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)

        end_times = {
            sensor: boundary["end_time"]
            for sensor, boundary in boundaries.items()
        }

        max_end_time = max(end_times.values())
        leading_sensors = {
            sensor: max_end_time - end_time
            for sensor, end_time in end_times.items()
            if max_end_time - end_time > 100
        }

        assert "Thermal" in leading_sensors, \
            "Thermal sensor should be detected as stopping early"
        assert leading_sensors["Thermal"] >= 900, \
            f"Thermal should stop ~1000ms early"

    def test_sample_count_validation(
        self,
        sensor_data_generator,
        duration_analyzer
    ):
        """
        Test: Validate sample counts match expected rates
        
        Expected: Sample counts should match sensor rates and duration
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 60

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)

        expected_gsr_samples = 128 * duration_seconds
        expected_thermal_frames = 10 * duration_seconds
        expected_rgb_frames = 30 * duration_seconds

        gsr_count = boundaries["GSR"]["sample_count"]
        thermal_count = boundaries["Thermal"]["sample_count"]
        rgb_count = boundaries["RGB"]["sample_count"]

        assert abs(gsr_count - expected_gsr_samples) <= expected_gsr_samples * 0.05, \
            f"GSR sample count should be within 5% of expected"
        assert abs(thermal_count - expected_thermal_frames) <= expected_thermal_frames * 0.05, \
            f"Thermal frame count should be within 5% of expected"
        assert abs(rgb_count - expected_rgb_frames) <= expected_rgb_frames * 0.05, \
            f"RGB frame count should be within 5% of expected"

    def test_long_session_duration_tracking(
        self,
        sensor_data_generator,
        duration_analyzer,
        output_generator
    ):
        """
        Test: Track duration consistency for long recording sessions
        
        Expected: Consistency should hold for extended duration
        """
        base_timestamp = int(datetime.now().timestamp() * 1000)
        duration_seconds = 3600

        gsr_samples = sensor_data_generator.generate_gsr_samples(
            base_timestamp, duration_seconds, sampling_rate=128
        )
        thermal_frames = sensor_data_generator.generate_thermal_frames(
            base_timestamp, duration_seconds, frame_rate=10
        )
        rgb_frames = sensor_data_generator.generate_rgb_frames(
            base_timestamp, duration_seconds, frame_rate=30
        )

        sensor_data = {
            "GSR": gsr_samples,
            "Thermal": thermal_frames,
            "RGB": rgb_frames
        }

        boundaries = duration_analyzer.extract_session_boundaries(sensor_data)
        consistency = duration_analyzer.validate_duration_consistency(
            boundaries, tolerance_ms=1000
        )

        output_generator.save_duration_report(
            boundaries,
            consistency,
            filename=f"long_session_{int(datetime.now().timestamp())}.csv"
        )

        assert consistency["consistent"], \
            "Long session should maintain consistency across sensors"

        for sensor, boundary in boundaries.items():
            duration_minutes = boundary["duration_ms"] / 60000
            assert duration_minutes >= 59.5, \
                f"{sensor} should record for at least 59.5 minutes in 60 minute session"
