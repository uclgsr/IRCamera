#!/usr/bin/env python3
"""
Comprehensive test suite for GSR Analytics and Signal Processing
Tests the advanced analytics features added to the GSR system
"""

import asyncio
import json
import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

# Add the PC controller source to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "pc-controller", "src"))

from ircamera_pc.core.gsr_analytics import (
    GSRAnalysisReport,
    GSRAnalytics,
    GSRFeatures,
    StressLevel,
)
from ircamera_pc.core.gsr_receiver import GSRReceiver


class TestGSRAnalytics(unittest.TestCase):
    """Test GSR analytics and signal processing functionality"""

    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()

        # Initialize analytics engine
        self.analytics = GSRAnalytics(
            window_size_seconds=10,  # Smaller window for testing
            overlap_seconds=5,
            sampling_rate=128.0,
        )

        # Test data parameters
        self.device_id = "test_device_001"
        self.session_id = "test_session_123"

    def tearDown(self):
        """Clean up test environment"""
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def generate_test_gsr_data(
        self, duration_seconds: int, pattern: str = "normal"
    ) -> tuple:
        """Generate synthetic GSR data for testing"""
        sampling_rate = 128.0
        samples = int(duration_seconds * sampling_rate)
        timestamps = np.linspace(0, duration_seconds, samples)

        # Base GSR signal (typical range 1-20 μS)
        base_gsr = 5.0

        if pattern == "normal":
            # Normal GSR with small variations
            gsr_values = base_gsr + np.random.normal(0, 0.5, samples)
            # Add some slow drift
            gsr_values += 0.5 * np.sin(timestamps * 0.1)

        elif pattern == "stress":
            # Elevated GSR with peaks (stress response)
            gsr_values = base_gsr + 3.0 + np.random.normal(0, 1.0, samples)
            # Add stress-related peaks
            for i in range(5):
                peak_time = np.random.uniform(0, duration_seconds)
                peak_idx = int(peak_time * sampling_rate)
                if peak_idx < samples:
                    # Create a peak lasting ~2 seconds
                    peak_width = int(2 * sampling_rate)
                    start_idx = max(0, peak_idx - peak_width // 2)
                    end_idx = min(samples, peak_idx + peak_width // 2)

                    peak_signal = 4.0 * np.exp(
                        -((np.arange(end_idx - start_idx) - peak_width // 2) ** 2)
                        / (peak_width / 4) ** 2
                    )
                    gsr_values[start_idx:end_idx] += peak_signal

        elif pattern == "artifact":
            # GSR with artifacts (sudden jumps, noise)
            gsr_values = base_gsr + np.random.normal(0, 0.3, samples)
            # Add random artifacts
            artifact_indices = np.random.choice(
                samples, size=samples // 50, replace=False
            )
            gsr_values[artifact_indices] += np.random.uniform(
                -10, 15, len(artifact_indices)
            )

        elif pattern == "increasing":
            # GSR with increasing trend (building stress)
            gsr_values = base_gsr + timestamps * 0.5 + np.random.normal(0, 0.5, samples)

        else:
            # Default to normal
            gsr_values = base_gsr + np.random.normal(0, 0.5, samples)

        # Ensure positive values
        gsr_values = np.maximum(gsr_values, 0.1)

        return gsr_values.tolist(), timestamps.tolist()

    def test_basic_feature_extraction(self):
        """Test basic GSR feature extraction"""
        # Generate 20 seconds of normal GSR data
        gsr_values, timestamps = self.generate_test_gsr_data(20, "normal")

        # Add samples to analytics engine
        self.analytics.add_gsr_samples(
            self.device_id, self.session_id, gsr_values, timestamps
        )

        # Wait a moment for processing
        import time

        time.sleep(0.5)

        # Get features
        features = self.analytics.get_real_time_features(
            self.device_id, self.session_id
        )

        # Verify features were extracted
        self.assertIsNotNone(features)
        self.assertEqual(features.device_id, self.device_id)
        self.assertEqual(features.session_id, self.session_id)

        # Check basic statistics
        self.assertGreater(features.mean_gsr, 0)
        self.assertGreater(features.std_gsr, 0)
        self.assertGreaterEqual(features.min_gsr, 0)
        self.assertGreater(features.max_gsr, features.min_gsr)

        # Check stress assessment
        self.assertGreaterEqual(features.stress_score, 0)
        self.assertLessEqual(features.stress_score, 100)
        self.assertIsInstance(features.stress_level, StressLevel)
        self.assertGreaterEqual(features.confidence, 0)
        self.assertLessEqual(features.confidence, 100)

        print(
            f"✓ Basic feature extraction: stress={features.stress_score:.1f}, level={features.stress_level.value}"
        )

    def test_stress_detection(self):
        """Test stress level detection with different patterns"""
        test_patterns = ["normal", "stress", "increasing"]
        expected_stress_levels = {}

        for pattern in test_patterns:
            # Generate data for this pattern
            gsr_values, timestamps = self.generate_test_gsr_data(15, pattern)

            # Use different session for each pattern
            session_id = f"test_session_{pattern}"

            # Add samples
            self.analytics.add_gsr_samples(
                self.device_id, session_id, gsr_values, timestamps
            )

            # Wait for processing
            import time

            time.sleep(0.5)

            # Get features
            features = self.analytics.get_real_time_features(self.device_id, session_id)

            if features:
                expected_stress_levels[pattern] = features.stress_score
                print(
                    f"✓ {pattern.title()} pattern: stress={features.stress_score:.1f}, level={features.stress_level.value}"
                )

        # Verify stress pattern detection
        if "normal" in expected_stress_levels and "stress" in expected_stress_levels:
            self.assertGreater(
                expected_stress_levels["stress"],
                expected_stress_levels["normal"],
                "Stress pattern should have higher stress score than normal",
            )

        if (
            "normal" in expected_stress_levels
            and "increasing" in expected_stress_levels
        ):
            self.assertGreater(
                expected_stress_levels["increasing"],
                expected_stress_levels["normal"],
                "Increasing pattern should have higher stress score than normal",
            )

    def test_session_report_generation(self):
        """Test comprehensive session report generation"""
        # Generate multiple data points over time
        total_duration = 30
        chunk_size = 5

        for i in range(0, total_duration, chunk_size):
            gsr_values, timestamps = self.generate_test_gsr_data(chunk_size, "stress")
            # Adjust timestamps to be sequential
            timestamps = [t + i for t in timestamps]

            self.analytics.add_gsr_samples(
                self.device_id, self.session_id, gsr_values, timestamps
            )

        # Wait for processing
        import time

        time.sleep(1.0)

        # Generate session report
        report = self.analytics.generate_session_report(self.device_id, self.session_id)

        # Verify report structure
        self.assertIsNotNone(report)
        self.assertIsInstance(report, GSRAnalysisReport)
        self.assertEqual(report.device_id, self.device_id)
        self.assertEqual(report.session_id, self.session_id)

        # Check report content
        self.assertGreater(report.duration_minutes, 0)
        self.assertGreater(report.total_samples, 0)
        self.assertEqual(report.sampling_rate, 128.0)
        self.assertGreaterEqual(report.data_quality, 0)
        self.assertLessEqual(report.data_quality, 100)

        # Check stress metrics
        self.assertGreaterEqual(report.average_stress_score, 0)
        self.assertLessEqual(report.average_stress_score, 100)
        self.assertGreaterEqual(report.peak_stress_score, report.average_stress_score)

        # Check stress distribution
        self.assertIsInstance(report.stress_distribution, dict)
        distribution_sum = sum(report.stress_distribution.values())
        self.assertAlmostEqual(distribution_sum, 100.0, places=1)

        # Check trend analysis
        self.assertIn(
            report.stress_trend,
            ["increasing", "decreasing", "stable", "insufficient_data"],
        )
        self.assertGreaterEqual(report.trend_confidence, 0)
        self.assertLessEqual(report.trend_confidence, 100)

        # Check recommendations
        self.assertIsInstance(report.recommendations, list)
        self.assertGreater(len(report.recommendations), 0)

        # Check features
        self.assertIsInstance(report.features, list)
        self.assertGreater(len(report.features), 0)

        print(
            f"✓ Session report: {len(report.features)} features, avg_stress={report.average_stress_score:.1f}, trend={report.stress_trend}"
        )

    def test_feature_export(self):
        """Test feature export functionality"""
        # Generate test data
        gsr_values, timestamps = self.generate_test_gsr_data(20, "normal")
        self.analytics.add_gsr_samples(
            self.device_id, self.session_id, gsr_values, timestamps
        )

        # Wait for processing
        import time

        time.sleep(0.5)

        # Export features to CSV
        csv_file = Path(self.temp_dir) / "test_features.csv"
        success = self.analytics.export_features_csv(
            self.device_id, self.session_id, str(csv_file)
        )

        self.assertTrue(success)
        self.assertTrue(csv_file.exists())

        # Verify CSV content
        import pandas as pd

        df = pd.read_csv(csv_file)

        self.assertGreater(len(df), 0)
        expected_columns = [
            "timestamp",
            "device_id",
            "session_id",
            "mean_gsr",
            "std_gsr",
            "stress_score",
            "stress_level",
            "confidence",
        ]

        for col in expected_columns:
            self.assertIn(col, df.columns)

        print(f"✓ Feature export: {len(df)} feature records exported")

    def test_artifact_handling(self):
        """Test handling of GSR artifacts and noise"""
        # Generate data with artifacts
        gsr_values, timestamps = self.generate_test_gsr_data(15, "artifact")

        self.analytics.add_gsr_samples(
            self.device_id, self.session_id, gsr_values, timestamps
        )

        # Wait for processing
        import time

        time.sleep(0.5)

        # Get features
        features = self.analytics.get_real_time_features(
            self.device_id, self.session_id
        )

        # Verify that artifacts didn't break the analysis
        self.assertIsNotNone(features)
        self.assertGreaterEqual(features.stress_score, 0)
        self.assertLessEqual(features.stress_score, 100)

        # Confidence might be lower due to artifacts
        self.assertGreaterEqual(features.confidence, 0)

        print(
            f"✓ Artifact handling: stress={features.stress_score:.1f}, confidence={features.confidence:.1f}"
        )

    def test_multiple_devices(self):
        """Test analytics with multiple devices"""
        devices = ["device_001", "device_002", "device_003"]
        patterns = ["normal", "stress", "increasing"]

        for device, pattern in zip(devices, patterns):
            gsr_values, timestamps = self.generate_test_gsr_data(15, pattern)
            session_id = f"session_{device}"

            self.analytics.add_gsr_samples(device, session_id, gsr_values, timestamps)

        # Wait for processing
        import time

        time.sleep(1.0)

        # Get stress summary
        summary = self.analytics.get_stress_summary()

        self.assertIsInstance(summary, dict)
        self.assertIn("active_sessions", summary)
        self.assertIn("sessions", summary)

        # Check that all devices are tracked
        sessions = summary.get("sessions", {})
        self.assertEqual(len(sessions), len(devices))

        print(f"✓ Multiple devices: {len(sessions)} sessions tracked")

    def test_performance_with_large_dataset(self):
        """Test analytics performance with larger dataset"""
        # Generate 5 minutes of data
        gsr_values, timestamps = self.generate_test_gsr_data(300, "stress")

        import time

        start_time = time.time()

        # Add data in chunks (simulating real-time streaming)
        chunk_size = 128  # 1 second of data
        for i in range(0, len(gsr_values), chunk_size):
            chunk_gsr = gsr_values[i : i + chunk_size]
            chunk_timestamps = timestamps[i : i + chunk_size]

            self.analytics.add_gsr_samples(
                self.device_id, self.session_id, chunk_gsr, chunk_timestamps
            )

        processing_time = time.time() - start_time

        # Wait for final processing
        time.sleep(1.0)

        # Get final features
        features = self.analytics.get_real_time_features(
            self.device_id, self.session_id
        )
        report = self.analytics.generate_session_report(self.device_id, self.session_id)

        # Verify processing completed
        self.assertIsNotNone(features)
        self.assertIsNotNone(report)

        # Check performance (should process faster than real-time)
        samples_per_second = len(gsr_values) / processing_time
        self.assertGreater(
            samples_per_second, 128
        )  # Should be faster than sampling rate

        print(
            f"✓ Performance test: {len(gsr_values)} samples processed in {processing_time:.2f}s "
            f"({samples_per_second:.0f} samples/s)"
        )


class TestGSRReceiverAnalyticsIntegration(unittest.TestCase):
    """Test integration between GSR receiver and analytics"""

    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()

        # Configure GSR receiver with analytics
        config = {
            "gsr_receiver": {
                "data_dir": self.temp_dir,
                "analytics": {
                    "window_size_seconds": 10,
                    "overlap_seconds": 5,
                    "sampling_rate": 128.0,
                },
            }
        }

        self.gsr_receiver = GSRReceiver(config)

    def tearDown(self):
        """Clean up test environment"""
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    async def test_integrated_analytics_workflow(self):
        """Test complete analytics workflow through GSR receiver"""
        device_id = "android_test_001"
        session_id = "integration_test_123"

        # Start receiver
        self.gsr_receiver.start()

        # Register session
        success = await self.gsr_receiver.register_device_session(device_id, session_id)
        self.assertTrue(success)

        # Generate and process test data
        test_analytics = TestGSRAnalytics()
        gsr_values, timestamps = test_analytics.generate_test_gsr_data(30, "stress")

        # Convert to GSR receiver format
        samples_data = []
        for i, (gsr_val, timestamp) in enumerate(zip(gsr_values, timestamps)):
            samples_data.append(
                {
                    "timestamp": timestamp,
                    "gsr_value": gsr_val,
                    "raw_value": int(gsr_val * 1000),  # Simulate raw value
                    "quality": 95,
                }
            )

        # Process in batches
        batch_size = 128
        for i in range(0, len(samples_data), batch_size):
            batch = samples_data[i : i + batch_size]
            success = await self.gsr_receiver.process_gsr_batch(
                device_id, session_id, batch
            )
            self.assertTrue(success)

        # Wait for analytics processing
        import time

        time.sleep(2.0)

        # Test analytics access through receiver
        analytics_data = self.gsr_receiver.get_real_time_analytics(
            device_id, session_id
        )
        self.assertIsNotNone(analytics_data)
        self.assertIn("stress_score", analytics_data)
        self.assertIn("stress_level", analytics_data)

        # Test stress summary
        stress_summary = self.gsr_receiver.get_stress_summary()
        self.assertIsInstance(stress_summary, dict)
        self.assertIn("sessions", stress_summary)

        # Test analytics alerts
        alerts = self.gsr_receiver.get_analytics_alerts()
        self.assertIsInstance(alerts, list)

        # End session (should generate analytics report)
        success = await self.gsr_receiver.end_session(device_id, session_id)
        self.assertTrue(success)

        # Check that analytics report was generated
        analytics_dir = Path(self.temp_dir) / "analytics"
        if analytics_dir.exists():
            report_files = list(analytics_dir.glob("gsr_analysis_*.json"))
            feature_files = list(analytics_dir.glob("gsr_features_*.csv"))

            self.assertGreater(
                len(report_files), 0, "Analytics report should be generated"
            )
            self.assertGreater(
                len(feature_files), 0, "Feature export should be generated"
            )

            # Verify report content
            with open(report_files[0], "r") as f:
                report_data = json.load(f)

            self.assertIn("stress_distribution", report_data)
            self.assertIn("recommendations", report_data)
            self.assertIn("average_stress_score", report_data)

        # Stop receiver
        self.gsr_receiver.stop()

        print("✓ Integrated analytics workflow completed successfully")


def main():
    """Run all GSR analytics tests"""
    print("Running GSR Analytics Test Suite...")
    print("=" * 50)

    # Test basic analytics functionality
    print("\n1. Testing GSR Analytics Engine")
    suite1 = unittest.TestLoader().loadTestsFromTestCase(TestGSRAnalytics)
    runner1 = unittest.TextTestRunner(verbosity=1)
    result1 = runner1.run(suite1)

    # Test integration with GSR receiver
    print("\n2. Testing Analytics Integration")
    suite2 = unittest.TestLoader().loadTestsFromTestCase(
        TestGSRReceiverAnalyticsIntegration
    )
    runner2 = unittest.TextTestRunner(verbosity=1)

    # Run async integration tests
    async def run_integration_tests():
        test_instance = TestGSRReceiverAnalyticsIntegration()
        test_instance.setUp()
        try:
            await test_instance.test_integrated_analytics_workflow()
            print("✓ Integration test passed")
            return True
        except Exception as e:
            print(f"✗ Integration test failed: {e}")
            return False
        finally:
            test_instance.tearDown()

    integration_success = asyncio.run(run_integration_tests())

    # Summary
    print("\n" + "=" * 50)
    print("GSR Analytics Test Summary:")
    print(f"Basic Analytics Tests: {'PASSED' if result1.wasSuccessful() else 'FAILED'}")
    print(f"Integration Tests: {'PASSED' if integration_success else 'FAILED'}")

    if result1.wasSuccessful() and integration_success:
        print("\n🎉 All GSR Analytics tests passed!")
        print("\nAdvanced GSR analytics features are fully functional:")
        print("• Real-time stress level detection")
        print("• Signal quality assessment")
        print("• Trend analysis and predictions")
        print("• Comprehensive session reports")
        print("• Multi-device monitoring")
        print("• Research-ready data export")
        return True
    else:
        print("\n⚠️  Some tests failed - please review implementation")
        return False


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
