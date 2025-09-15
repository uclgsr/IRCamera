"""
Comprehensive unit tests for PC Controller data aggregation components
Tests data processing, synchronization, and export functionality
"""

import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from typing import Any, Dict, List
from unittest.mock import MagicMock, Mock, patch

import h5py
import numpy as np
import pandas as pd

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", ".."))

from ircamera_pc.data.aggregator import DataAggregator
from ircamera_pc.data.exporter import DataExporter, ExportFormat
from ircamera_pc.data.processor import DataProcessor
from ircamera_pc.data.validator import DataValidator


class TestDataAggregator(unittest.TestCase):
    """Comprehensive tests for DataAggregator functionality"""

    def setUp(self) -> None:
        """Set up test fixtures"""
        self.temp_dir = tempfile.mkdtemp()
        self.aggregator = DataAggregator(output_dir=self.temp_dir)

        # Sample sensor data
        self.sample_gsr_data = [
            {"timestamp": 1000000000, "gsr_microsiemens": 25.5, "raw_adc": 2048},
            {"timestamp": 1000001000, "gsr_microsiemens": 26.2, "raw_adc": 2100},
            {"timestamp": 1000002000, "gsr_microsiemens": 24.8, "raw_adc": 1980},
        ]

        self.sample_thermal_data = [
            {
                "timestamp": 1000000000,
                "temperature_matrix": [[25.5, 26.0], [25.8, 26.2]],
            },
            {
                "timestamp": 1000001000,
                "temperature_matrix": [[25.6, 26.1], [25.9, 26.3]],
            },
            {
                "timestamp": 1000002000,
                "temperature_matrix": [[25.7, 26.2], [26.0, 26.4]],
            },
        ]

        self.sample_sync_markers = [
            {"timestamp": 1000000500, "id": "STIMULUS_1", "type": "visual"},
            {"timestamp": 1000001500, "id": "STIMULUS_2", "type": "auditory"},
        ]

    def tearDown(self) -> Any:
        """Clean up test files"""
        import shutil

        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_initialization(self) -> Any:
        """Test aggregator initialization"""
        self.assertIsNotNone(self.aggregator)
        self.assertEqual(self.aggregator.output_dir, self.temp_dir)
        self.assertEqual(len(self.aggregator.get_active_sessions()), 0)

    def test_session_creation(self) -> Any:
        """Test session creation and management"""
        session_id = self.aggregator.create_session(
            session_name="TestSession",
            participant_id="P001",
            devices=["ANDROID_001", "ANDROID_002"],
        )

        self.assertIsNotNone(session_id)
        self.assertTrue(session_id.startswith("TestSession_"))

        sessions = self.aggregator.get_active_sessions()
        self.assertEqual(len(sessions), 1)
        self.assertEqual(sessions[0]["participant_id"], "P001")

    def test_data_ingestion(self) -> Any:
        """Test multi-modal data ingestion"""
        session_id = self.aggregator.create_session("DataTest", "P001")

        # Ingest GSR data
        result_gsr = self.aggregator.ingest_sensor_data(
            session_id, "ANDROID_001", "gsr", self.sample_gsr_data
        )
        self.assertTrue(result_gsr)

        # Ingest thermal data
        result_thermal = self.aggregator.ingest_sensor_data(
            session_id, "ANDROID_001", "thermal", self.sample_thermal_data
        )
        self.assertTrue(result_thermal)

        # Verify data was stored
        session_data = self.aggregator.get_session_data(session_id)
        self.assertIn("gsr", session_data)
        self.assertIn("thermal", session_data)

    def test_sync_marker_integration(self) -> Any:
        """Test sync marker processing and alignment"""
        session_id = self.aggregator.create_session("SyncTest", "P001")

        # Add sync markers
        for marker in self.sample_sync_markers:
            result = self.aggregator.add_sync_marker(session_id, marker)
            self.assertTrue(result)

        # Verify sync markers are stored
        markers = self.aggregator.get_sync_markers(session_id)
        self.assertEqual(len(markers), 2)
        self.assertEqual(markers[0]["id"], "STIMULUS_1")

    def test_temporal_alignment(self) -> Any:
        """Test temporal alignment of multi-modal data"""
        session_id = self.aggregator.create_session("AlignmentTest", "P001")

        # Ingest data from multiple devices with slight timing differences
        device1_data = [{"timestamp": 1000000000, "gsr_microsiemens": 25.5}]
        device2_data = [
            {"timestamp": 1000000050, "gsr_microsiemens": 26.0}
        ]  # 50ms offset

        self.aggregator.ingest_sensor_data(session_id, "DEVICE_1", "gsr", device1_data)
        self.aggregator.ingest_sensor_data(session_id, "DEVICE_2", "gsr", device2_data)

        # Perform temporal alignment
        aligned_data = self.aggregator.align_temporal_data(session_id, tolerance_ms=100)

        self.assertIsNotNone(aligned_data)
        # Should have aligned the data within tolerance

    def test_data_quality_validation(self) -> Any:
        """Test data quality validation and integrity checks"""
        session_id = self.aggregator.create_session("QualityTest", "P001")

        # Test with good data
        good_data = [
            {"timestamp": 1000000000, "gsr_microsiemens": 25.5, "raw_adc": 2048},
            {"timestamp": 1000001000, "gsr_microsiemens": 26.2, "raw_adc": 2100},
        ]

        quality_report = self.aggregator.validate_data_quality(
            session_id, "gsr", good_data
        )
        self.assertGreater(quality_report["quality_score"], 0.8)

        # Test with problematic data
        bad_data = [
            {
                "timestamp": 1000000000,
                "gsr_microsiemens": -1.0,
                "raw_adc": -1,
            },  # Invalid values
            {
                "timestamp": 999999999,
                "gsr_microsiemens": 25.5,
                "raw_adc": 2048,
            },  # Out of order
        ]

        bad_quality_report = self.aggregator.validate_data_quality(
            session_id, "gsr", bad_data
        )
        self.assertLess(bad_quality_report["quality_score"], 0.5)

    def test_real_time_processing(self) -> Any:
        """Test real-time data processing capabilities"""
        session_id = self.aggregator.create_session("RealTimeTest", "P001")

        # Enable real-time processing
        self.aggregator.enable_real_time_processing(session_id)

        # Stream data points individually
        for data_point in self.sample_gsr_data:
            result = self.aggregator.process_real_time_data(
                session_id, "ANDROID_001", "gsr", data_point
            )
            self.assertTrue(result)

        # Verify real-time statistics
        rt_stats = self.aggregator.get_real_time_statistics(session_id)
        self.assertIsNotNone(rt_stats)
        self.assertIn("data_points_processed", rt_stats)

    def test_concurrent_sessions(self) -> Any:
        """Test handling multiple concurrent sessions"""
        # Create multiple sessions
        sessions = []
        for i in range(5):
            session_id = self.aggregator.create_session(
                f"ConcurrentSession_{i}", f"P00{i}"
            )
            sessions.append(session_id)

        # Ingest data to all sessions concurrently
        import threading

        def ingest_to_session(session_id) -> Any:
            self.aggregator.ingest_sensor_data(
                session_id, "ANDROID_001", "gsr", self.sample_gsr_data
            )

        threads = []
        for session_id in sessions:
            thread = threading.Thread(target=ingest_to_session, args=(session_id,))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        # Verify all sessions have data
        for session_id in sessions:
            session_data = self.aggregator.get_session_data(session_id)
            self.assertIn("gsr", session_data)

    def test_memory_management(self) -> Any:
        """Test memory management with large datasets"""
        session_id = self.aggregator.create_session("MemoryTest", "P001")

        # Generate large dataset
        large_dataset = []
        for i in range(10000):
            large_dataset.append(
                {
                    "timestamp": 1000000000 + i * 1000,
                    "gsr_microsiemens": 25.0 + np.sin(i * 0.1),
                    "raw_adc": 2048 + int(100 * np.sin(i * 0.1)),
                }
            )

        # Ingest large dataset
        result = self.aggregator.ingest_sensor_data(
            session_id, "ANDROID_001", "gsr", large_dataset
        )
        self.assertTrue(result)

        # Verify memory usage is reasonable
        memory_stats = self.aggregator.get_memory_statistics()
        self.assertIsNotNone(memory_stats)


class TestDataExporter(unittest.TestCase):
    """Tests for data export functionality"""

    def setUp(self) -> None:
        """Set up test fixtures"""
        self.temp_dir = tempfile.mkdtemp()
        self.exporter = DataExporter(output_dir=self.temp_dir)

        # Sample session data
        self.session_data = {
            "session_id": "TEST_SESSION_001",
            "participant_id": "P001",
            "gsr_data": pd.DataFrame(
                {
                    "timestamp": [1000000000, 1000001000, 1000002000],
                    "gsr_microsiemens": [25.5, 26.2, 24.8],
                    "raw_adc": [2048, 2100, 1980],
                }
            ),
            "thermal_data": pd.DataFrame(
                {
                    "timestamp": [1000000000, 1000001000, 1000002000],
                    "avg_temperature": [25.5, 25.8, 26.0],
                }
            ),
            "sync_markers": [
                {"timestamp": 1000000500, "id": "STIMULUS_1", "type": "visual"},
                {"timestamp": 1000001500, "id": "STIMULUS_2", "type": "auditory"},
            ],
        }

    def tearDown(self) -> Any:
        """Clean up test files"""
        import shutil

        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_csv_export(self) -> Any:
        """Test CSV export functionality"""
        output_file = os.path.join(self.temp_dir, "test_export.csv")

        result = self.exporter.export_to_csv(self.session_data, output_file)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(output_file))

        # Verify CSV content
        exported_df = pd.read_csv(output_file)
        self.assertEqual(len(exported_df), 3)
        self.assertIn("gsr_microsiemens", exported_df.columns)

    def test_excel_export(self) -> Any:
        """Test Excel export with multiple sheets"""
        output_file = os.path.join(self.temp_dir, "test_export.xlsx")

        result = self.exporter.export_to_excel(self.session_data, output_file)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(output_file))

        # Verify Excel content
        with pd.ExcelFile(output_file) as xl:
            self.assertIn("GSR_Data", xl.sheet_names)
            self.assertIn("Thermal_Data", xl.sheet_names)
            self.assertIn("Sync_Markers", xl.sheet_names)

    def test_hdf5_export(self) -> Any:
        """Test HDF5 export for large datasets"""
        output_file = os.path.join(self.temp_dir, "test_export.h5")

        result = self.exporter.export_to_hdf5(self.session_data, output_file)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(output_file))

        # Verify HDF5 content
        with h5py.File(output_file, "r") as f:
            self.assertIn("gsr_data", f.keys())
            self.assertIn("thermal_data", f.keys())
            self.assertEqual(len(f["gsr_data"]), 3)

    def test_json_export(self) -> Any:
        """Test JSON export for metadata and annotations"""
        output_file = os.path.join(self.temp_dir, "test_export.json")

        result = self.exporter.export_to_json(self.session_data, output_file)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(output_file))

        # Verify JSON content
        with open(output_file, "r") as f:
            exported_data = json.load(f)
            self.assertEqual(exported_data["session_id"], "TEST_SESSION_001")
            self.assertIn("sync_markers", exported_data)

    def test_export_format_validation(self) -> Any:
        """Test export format validation"""
        valid_formats = [
            ExportFormat.CSV,
            ExportFormat.EXCEL,
            ExportFormat.HDF5,
            ExportFormat.JSON,
        ]

        for fmt in valid_formats:
            self.assertTrue(self.exporter.is_format_supported(fmt))

        # Test invalid format
        self.assertFalse(self.exporter.is_format_supported("invalid_format"))

    def test_batch_export(self) -> Any:
        """Test batch export of multiple sessions"""
        sessions = []
        for i in range(3):
            session = self.session_data.copy()
            session["session_id"] = f"BATCH_SESSION_{i:03d}"
            sessions.append(session)

        output_dir = os.path.join(self.temp_dir, "batch_export")
        os.makedirs(output_dir, exist_ok=True)

        result = self.exporter.batch_export(sessions, output_dir, ExportFormat.CSV)
        self.assertTrue(result)

        # Verify all sessions were exported
        exported_files = os.listdir(output_dir)
        self.assertEqual(len(exported_files), 3)


class TestDataProcessor(unittest.TestCase):
    """Tests for data processing and analysis"""

    def setUp(self) -> None:
        """Set up test fixtures"""
        self.processor = DataProcessor()

        # Generate synthetic physiological data
        timestamps = np.arange(0, 60000, 100)  # 10 minutes at 10Hz
        self.gsr_data = pd.DataFrame(
            {
                "timestamp": timestamps,
                "gsr_microsiemens": 25.0
                + 5.0 * np.sin(timestamps / 10000)
                + np.random.normal(0, 0.5, len(timestamps)),
            }
        )

    def test_signal_filtering(self) -> Any:
        """Test signal filtering and noise reduction"""
        # Apply low-pass filter
        filtered_data = self.processor.apply_lowpass_filter(
            self.gsr_data["gsr_microsiemens"], cutoff_freq=1.0, sampling_rate=10.0
        )

        self.assertEqual(len(filtered_data), len(self.gsr_data))

        # Filtered signal should have reduced noise
        original_std = np.std(self.gsr_data["gsr_microsiemens"])
        filtered_std = np.std(filtered_data)
        self.assertLess(filtered_std, original_std)

    def test_artifact_detection(self) -> Any:
        """Test artifact detection in physiological signals"""
        # Introduce artificial artifacts
        corrupted_data = self.gsr_data["gsr_microsiemens"].copy()
        corrupted_data.iloc[100:105] = 100.0  # Spike artifact
        corrupted_data.iloc[200:210] = 0.0  # Drop artifact

        artifacts = self.processor.detect_artifacts(
            corrupted_data, method="statistical", threshold=3.0
        )

        self.assertGreater(len(artifacts), 0)
        # Should detect both artifacts
        self.assertTrue(any(95 <= idx <= 110 for idx in artifacts))
        self.assertTrue(any(195 <= idx <= 215 for idx in artifacts))

    def test_feature_extraction(self) -> Any:
        """Test physiological feature extraction"""
        features = self.processor.extract_features(self.gsr_data["gsr_microsiemens"])

        expected_features = [
            "mean",
            "std",
            "min",
            "max",
            "median",
            "peak_count",
            "zero_crossings",
            "energy",
        ]

        for feature in expected_features:
            self.assertIn(feature, features)
            self.assertIsInstance(features[feature], (int, float))

    def test_synchronization_analysis(self) -> Any:
        """Test cross-signal synchronization analysis"""
        # Create synthetic synchronized signals
        t = np.arange(0, 1000, 1)
        signal1 = np.sin(t / 50)
        signal2 = np.sin((t / 50) + 0.1)  # Slight phase shift

        correlation = self.processor.compute_cross_correlation(signal1, signal2)
        coherence = self.processor.compute_coherence(signal1, signal2, fs=1000)

        self.assertIsNotNone(correlation)
        self.assertIsNotNone(coherence)
        self.assertGreater(np.max(correlation), 0.8)  # High correlation expected

    def test_statistical_analysis(self) -> Any:
        """Test statistical analysis of sensor data"""
        stats = self.processor.compute_statistics(self.gsr_data["gsr_microsiemens"])

        expected_stats = [
            "count",
            "mean",
            "std",
            "min",
            "max",
            "median",
            "q25",
            "q75",
            "skewness",
            "kurtosis",
        ]

        for stat in expected_stats:
            self.assertIn(stat, stats)

    def test_quality_metrics(self) -> Any:
        """Test data quality metrics calculation"""
        quality_metrics = self.processor.calculate_quality_metrics(
            self.gsr_data["gsr_microsiemens"],
            expected_range=(15.0, 35.0),
            sampling_rate=10.0,
        )

        self.assertIn("completeness", quality_metrics)
        self.assertIn("validity", quality_metrics)
        self.assertIn("consistency", quality_metrics)
        self.assertBetween(quality_metrics["completeness"], 0.0, 1.0)

    def assertBetween(self, value, min_val, max_val) -> Any:
        """Helper assertion for range checking"""
        self.assertGreaterEqual(value, min_val)
        self.assertLessEqual(value, max_val)


class TestDataValidator(unittest.TestCase):
    """Tests for data validation functionality"""

    def setUp(self) -> None:
        """Set up test fixtures"""
        self.validator = DataValidator()

    def test_timestamp_validation(self) -> Any:
        """Test timestamp validation and monotonicity"""
        # Valid timestamps
        valid_timestamps = [1000000000, 1000001000, 1000002000]
        result = self.validator.validate_timestamps(valid_timestamps)
        self.assertTrue(result["is_valid"])
        self.assertTrue(result["is_monotonic"])

        # Invalid timestamps (out of order)
        invalid_timestamps = [1000001000, 1000000000, 1000002000]
        result = self.validator.validate_timestamps(invalid_timestamps)
        self.assertFalse(result["is_monotonic"])

    def test_gsr_data_validation(self) -> Any:
        """Test GSR-specific data validation"""
        # Valid GSR data
        valid_gsr = [
            {"timestamp": 1000000000, "gsr_microsiemens": 25.5, "raw_adc": 2048},
            {"timestamp": 1000001000, "gsr_microsiemens": 26.2, "raw_adc": 2100},
        ]

        result = self.validator.validate_gsr_data(valid_gsr)
        self.assertTrue(result["is_valid"])
        self.assertEqual(result["invalid_count"], 0)

        # Invalid GSR data
        invalid_gsr = [
            {
                "timestamp": 1000000000,
                "gsr_microsiemens": -5.0,
                "raw_adc": -1,
            },  # Negative values
            {
                "timestamp": 1000001000,
                "gsr_microsiemens": 150.0,
                "raw_adc": 5000,
            },  # Out of range
        ]

        result = self.validator.validate_gsr_data(invalid_gsr)
        self.assertFalse(result["is_valid"])
        self.assertEqual(result["invalid_count"], 2)

    def test_thermal_data_validation(self) -> Any:
        """Test thermal camera data validation"""
        # Valid thermal data
        valid_thermal = [
            {
                "timestamp": 1000000000,
                "temperature_matrix": [[25.5, 26.0], [25.8, 26.2]],
            }
        ]

        result = self.validator.validate_thermal_data(valid_thermal)
        self.assertTrue(result["is_valid"])

        # Invalid thermal data (wrong matrix dimensions)
        invalid_thermal = [
            {
                "timestamp": 1000000000,
                "temperature_matrix": [
                    [25.5, 26.0, 27.0],
                    [25.8],
                ],  # Inconsistent dimensions
            }
        ]

        result = self.validator.validate_thermal_data(invalid_thermal)
        self.assertFalse(result["is_valid"])

    def test_sync_marker_validation(self) -> Any:
        """Test sync marker validation"""
        # Valid sync markers
        valid_markers = [
            {"timestamp": 1000000500, "id": "STIMULUS_1", "type": "visual"},
            {"timestamp": 1000001500, "id": "STIMULUS_2", "type": "auditory"},
        ]

        result = self.validator.validate_sync_markers(valid_markers)
        self.assertTrue(result["is_valid"])
        self.assertTrue(result["unique_ids"])

        # Invalid sync markers (duplicate IDs)
        invalid_markers = [
            {"timestamp": 1000000500, "id": "STIMULUS_1", "type": "visual"},
            {
                "timestamp": 1000001500,
                "id": "STIMULUS_1",
                "type": "auditory",
            },  # Duplicate ID
        ]

        result = self.validator.validate_sync_markers(invalid_markers)
        self.assertFalse(result["unique_ids"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
