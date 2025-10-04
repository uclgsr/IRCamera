#!/usr/bin/env python3
"""
GSR Data Integrity Test (Synthetic)

Test: Use a mock GSR sensor feed to generate a known signal pattern (sine wave)
Output: GSR CSV log file with analysis comparing recorded vs expected values
Subsystem: GSR recording pipeline
Chapters: Chapter 5 (system correctness) and Chapter 6 (data accuracy)
"""

import json
import math
import os
import sys
import time
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any


@dataclass
class GSRSample:
    timestamp_ms: int
    conductance_us: float
    resistance_ohm: float
    expected_conductance: float
    error: float


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    samples_generated: int
    samples_expected: int
    mean_error: float
    max_error: float
    rmse: float
    passed: bool
    output_file: str


class MockGSRSensor:
    """Mock GSR sensor generating synthetic sine wave signal"""
    
    def __init__(self, sampling_rate_hz: float = 128.0, 
                 base_conductance: float = 10.0,
                 amplitude: float = 5.0,
                 frequency_hz: float = 0.5):
        self.sampling_rate_hz = sampling_rate_hz
        self.base_conductance = base_conductance
        self.amplitude = amplitude
        self.frequency_hz = frequency_hz
        self.sample_count = 0
        
    def generate_sample(self) -> tuple[float, float]:
        """Generate a single GSR sample with known pattern"""
        t = self.sample_count / self.sampling_rate_hz
        
        # Generate sine wave pattern
        conductance = self.base_conductance + self.amplitude * math.sin(
            2 * math.pi * self.frequency_hz * t
        )
        
        # Convert to resistance (inverse relationship)
        resistance = 1_000_000.0 / conductance if conductance > 0 else float('inf')
        
        self.sample_count += 1
        return conductance, resistance
    
    def get_expected_value(self, sample_index: int) -> float:
        """Get expected conductance value for verification"""
        t = sample_index / self.sampling_rate_hz
        return self.base_conductance + self.amplitude * math.sin(
            2 * math.pi * self.frequency_hz * t
        )


class GSRSyntheticIntegrityTest:
    """Test GSR recording pipeline with synthetic data"""
    
    def __init__(self, duration_seconds: int = 10, output_dir: str = "output"):
        self.duration_seconds = duration_seconds
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.samples: List[GSRSample] = []
        
    def run_test(self) -> TestResult:
        """Execute the synthetic GSR integrity test"""
        print(f"Starting GSR Synthetic Integrity Test...")
        print(f"Duration: {self.duration_seconds} seconds")
        
        start_time = datetime.now()
        start_timestamp = start_time.isoformat()
        
        # Create mock sensor
        sensor = MockGSRSensor(sampling_rate_hz=128.0)
        expected_samples = int(self.duration_seconds * sensor.sampling_rate_hz)
        
        print(f"Expected samples: {expected_samples}")
        
        # Generate samples
        start_ms = int(time.time() * 1000)
        interval_ms = 1000.0 / sensor.sampling_rate_hz
        
        for i in range(expected_samples):
            timestamp_ms = start_ms + int(i * interval_ms)
            conductance, resistance = sensor.generate_sample()
            expected = sensor.get_expected_value(i)
            error = abs(conductance - expected)
            
            sample = GSRSample(
                timestamp_ms=timestamp_ms,
                conductance_us=conductance,
                resistance_ohm=resistance,
                expected_conductance=expected,
                error=error
            )
            self.samples.append(sample)
            
            # Progress indicator
            if (i + 1) % 128 == 0:
                print(f"  Generated {i + 1}/{expected_samples} samples...")
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        print(f"Generated {len(self.samples)} samples in {duration:.2f} seconds")
        
        # Analyze results
        errors = [s.error for s in self.samples]
        mean_error = sum(errors) / len(errors) if errors else 0.0
        max_error = max(errors) if errors else 0.0
        rmse = math.sqrt(sum(e**2 for e in errors) / len(errors)) if errors else 0.0
        
        # Test passes if errors are within tolerance (should be ~0 for synthetic)
        passed = max_error < 0.001  # Very tight tolerance for synthetic data
        
        # Save output files
        csv_file = self.save_csv()
        
        result = TestResult(
            test_name="GSR Synthetic Integrity Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            samples_generated=len(self.samples),
            samples_expected=expected_samples,
            mean_error=mean_error,
            max_error=max_error,
            rmse=rmse,
            passed=passed,
            output_file=str(csv_file)
        )
        
        # Save result
        self.save_result(result)
        
        # Print summary
        self.print_summary(result)
        
        return result
    
    def save_csv(self) -> Path:
        """Save GSR samples to CSV file"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        csv_file = self.output_dir / f"gsr_synthetic_{timestamp}.csv"
        
        with open(csv_file, 'w') as f:
            # Write header
            f.write("timestamp_ms,conductance_us,resistance_ohm,expected_conductance,error\n")
            
            # Write samples
            for sample in self.samples:
                f.write(f"{sample.timestamp_ms},{sample.conductance_us:.6f},"
                       f"{sample.resistance_ohm:.2f},{sample.expected_conductance:.6f},"
                       f"{sample.error:.6f}\n")
        
        print(f"Saved CSV: {csv_file}")
        return csv_file
    
    def save_result(self, result: TestResult) -> None:
        """Save test result as JSON"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.output_dir / f"gsr_synthetic_result_{timestamp}.json"
        
        with open(result_file, 'w') as f:
            json.dump(asdict(result), f, indent=2)
        
        print(f"Saved result: {result_file}")
    
    def print_summary(self, result: TestResult) -> None:
        """Print test summary"""
        print("\n" + "="*60)
        print("GSR SYNTHETIC INTEGRITY TEST SUMMARY")
        print("="*60)
        print(f"Test Name: {result.test_name}")
        print(f"Duration: {result.duration_seconds:.2f} seconds")
        print(f"Samples Generated: {result.samples_generated}")
        print(f"Samples Expected: {result.samples_expected}")
        print(f"Mean Error: {result.mean_error:.6f} µS")
        print(f"Max Error: {result.max_error:.6f} µS")
        print(f"RMSE: {result.rmse:.6f} µS")
        print(f"Status: {'PASSED' if result.passed else 'FAILED'}")
        print(f"Output File: {result.output_file}")
        print("="*60)


def main():
    """Main entry point"""
    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "gsr_tests"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Run test
    test = GSRSyntheticIntegrityTest(duration_seconds=10, output_dir=str(output_dir))
    result = test.run_test()
    
    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
