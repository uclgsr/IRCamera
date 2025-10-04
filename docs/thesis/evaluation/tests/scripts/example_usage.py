#!/usr/bin/env python3
"""
Example Usage of Thesis Evaluation Tests

This script demonstrates how to use the test suite programmatically
and customize test parameters for specific scenarios.
"""

from pathlib import Path

# Use relative imports to avoid sys.path manipulation
# This assumes the script is run from the thesis_evaluation/tests directory
# or that the package is properly installed
try:
    from gsr_tests.gsr_synthetic_integrity_test import GSRSyntheticIntegrityTest, MockGSRSensor
    from thermal_tests.thermal_synthetic_capture_test import ThermalSyntheticCaptureTest
    from gsr_tests.gsr_real_sensor_continuity_test import GSRRealSensorContinuityTest
    from thermal_tests.thermal_real_camera_test import ThermalRealCameraTest
except ImportError:
    # Fallback for running directly - add parent to path only as last resort
    import sys

    sys.path.insert(0, str(Path(__file__).parent.parent))
    from gsr_tests.gsr_synthetic_integrity_test import GSRSyntheticIntegrityTest, MockGSRSensor
    from thermal_tests.thermal_synthetic_capture_test import ThermalSyntheticCaptureTest
    from gsr_tests.gsr_real_sensor_continuity_test import GSRRealSensorContinuityTest
    from thermal_tests.thermal_real_camera_test import ThermalRealCameraTest


def example_1_synthetic_gsr_test():
    """Example 1: Run GSR synthetic test with custom parameters"""
    print("\n" + "=" * 60)
    print("EXAMPLE 1: Custom GSR Synthetic Test")
    print("=" * 60)

    # Create test with custom duration
    output_dir = Path(__file__).parent.parent / "output" / "examples"
    output_dir.mkdir(parents=True, exist_ok=True)

    test = GSRSyntheticIntegrityTest(
        duration_seconds=5,  # Shorter test
        output_dir=str(output_dir)
    )

    result = test.run_test()

    print(f"\nTest completed: {'PASSED' if result.passed else 'FAILED'}")
    print(f"Samples generated: {result.samples_generated}")
    print(f"Mean error: {result.mean_error:.6f} µS")


def example_2_custom_mock_sensor():
    """Example 2: Create custom mock sensor with different parameters"""
    print("\n" + "=" * 60)
    print("EXAMPLE 2: Custom Mock GSR Sensor")
    print("=" * 60)

    # Create sensor with custom wave parameters
    sensor = MockGSRSensor(
        sampling_rate_hz=256.0,  # Higher rate
        base_conductance=15.0,  # Different baseline
        amplitude=10.0,  # Larger amplitude
        frequency_hz=1.0  # Faster oscillation
    )

    # Generate some samples
    print("\nFirst 10 samples:")
    for i in range(10):
        conductance, resistance = sensor.generate_sample()
        expected = sensor.get_expected_value(i)
        print(f"  Sample {i}: {conductance:.2f} µS (expected: {expected:.2f} µS)")


def example_3_thermal_test_custom_fps():
    """Example 3: Run thermal test with different frame rate"""
    print("\n" + "=" * 60)
    print("EXAMPLE 3: Thermal Test at Different FPS")
    print("=" * 60)

    output_dir = Path(__file__).parent.parent / "output" / "examples"
    output_dir.mkdir(parents=True, exist_ok=True)

    # Test at 10 Hz instead of 5 Hz
    test = ThermalSyntheticCaptureTest(
        duration_seconds=5,
        target_fps=10.0,
        tolerance_ms=25.0,  # Tighter tolerance
        output_dir=str(output_dir)
    )

    result = test.run_test()

    print(f"\nTest completed: {'PASSED' if result.passed else 'FAILED'}")


def example_4_analyze_existing_data():
    """Example 4: Analyze existing GSR data file"""
    print("\n" + "=" * 60)
    print("EXAMPLE 4: Analyze Existing GSR Data")
    print("=" * 60)

    # Check if sample data exists
    sample_data = Path(__file__).parent.parent / "output" / "sample_data" / "sample_gsr_data.csv"

    if not sample_data.exists():
        print("Sample data not found. Run this after creating sample data.")
        return

    output_dir = Path(__file__).parent.parent / "output" / "examples"
    output_dir.mkdir(parents=True, exist_ok=True)

    # Analyze with custom parameters
    test = GSRRealSensorContinuityTest(
        data_file=str(sample_data),
        expected_rate_hz=128.0,
        gap_threshold_ms=15.0,  # More sensitive gap detection
        output_dir=str(output_dir)
    )

    result = test.run_test()

    print(f"\nTest completed: {'PASSED' if result.passed else 'FAILED'}")
    if result.issues:
        print("Issues found:")
        for issue in result.issues:
            print(f"  - {issue}")


def example_5_batch_analysis():
    """Example 5: Batch analyze multiple GSR files"""
    print("\n" + "=" * 60)
    print("EXAMPLE 5: Batch Analysis")
    print("=" * 60)

    # Find all GSR CSV files in output directory
    output_base = Path(__file__).parent.parent / "output"
    gsr_files = list(output_base.glob("**/gsr_*.csv"))

    if not gsr_files:
        print("No GSR files found for batch analysis")
        return

    print(f"Found {len(gsr_files)} GSR files to analyze\n")

    output_dir = output_base / "examples"
    output_dir.mkdir(parents=True, exist_ok=True)

    results = []
    for gsr_file in gsr_files:
        print(f"Analyzing: {gsr_file.name}")

        test = GSRRealSensorContinuityTest(
            data_file=str(gsr_file),
            expected_rate_hz=128.0,
            output_dir=str(output_dir)
        )

        result = test.run_test()
        results.append({
            'file': gsr_file.name,
            'passed': result.passed,
            'issues': result.issues
        })
        print()

    # Summary
    print("\nBatch Analysis Summary:")
    passed = sum(1 for r in results if r['passed'])
    print(f"  Total files: {len(results)}")
    print(f"  Passed: {passed}")
    print(f"  Failed: {len(results) - passed}")


def example_6_custom_validation():
    """Example 6: Custom validation logic"""
    print("\n" + "=" * 60)
    print("EXAMPLE 6: Custom Validation Logic")
    print("=" * 60)

    # Create a test and access its data
    output_dir = Path(__file__).parent.parent / "output" / "examples"
    output_dir.mkdir(parents=True, exist_ok=True)

    test = GSRSyntheticIntegrityTest(
        duration_seconds=3,
        output_dir=str(output_dir)
    )

    # Run test to generate samples
    result = test.run_test()

    # Custom validation: check for specific patterns
    print("\nCustom Analysis:")

    # Analyze sample distribution
    conductances = [s.conductance_us for s in test.samples]
    min_conductance = min(conductances)
    max_conductance = max(conductances)

    print(f"  Conductance range: {min_conductance:.2f} - {max_conductance:.2f} µS")
    print(f"  Range span: {max_conductance - min_conductance:.2f} µS")

    # Check for expected sine wave characteristics
    # Should oscillate around base value
    expected_range = 10.0  # amplitude * 2
    actual_range = max_conductance - min_conductance

    if abs(actual_range - expected_range) < 0.1:
        print("  ✓ Sine wave amplitude is correct")
    else:
        print(f"  ✗ Amplitude mismatch (expected ~{expected_range:.2f})")


def main():
    """Run all examples"""
    print("=" * 60)
    print("THESIS EVALUATION TEST SUITE - USAGE EXAMPLES")
    print("=" * 60)

    examples = [
        ("Synthetic GSR Test", example_1_synthetic_gsr_test),
        ("Custom Mock Sensor", example_2_custom_mock_sensor),
        ("Thermal Test Custom FPS", example_3_thermal_test_custom_fps),
        ("Analyze Existing Data", example_4_analyze_existing_data),
        ("Batch Analysis", example_5_batch_analysis),
        ("Custom Validation", example_6_custom_validation),
    ]

    for name, example_func in examples:
        try:
            example_func()
        except Exception as e:
            print(f"\nError in {name}: {e}")

        input("\nPress Enter to continue to next example...")

    print("\n" + "=" * 60)
    print("All examples completed!")
    print("=" * 60)


if __name__ == "__main__":
    main()
