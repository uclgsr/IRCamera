# Thesis Evaluation Tests

This directory contains automated tests for data recording correctness and performance evaluation as per thesis requirements (Chapter 5 and Chapter 6).

## Test Structure

### GSR Tests (`gsr_tests/`)
- `gsr_synthetic_integrity_test.py` - GSR data integrity test with synthetic signal
- `gsr_real_sensor_continuity_test.py` - GSR logging continuity with real Shimmer3 sensor

### Thermal Tests (`thermal_tests/`)
- `thermal_synthetic_capture_test.py` - Thermal frame capture verification with synthetic data
- `thermal_real_camera_test.py` - Thermal camera recording test with real TC001

### RGB Tests (`rgb_tests/`)
- `rgb_video_performance_test.py` - RGB video/frame capture performance test

### Data Integrity (`data_integrity/`)
- `file_integrity_validator.py` - Validates all recorded data files

### Scripts (`scripts/`)
- `run_all_tests.py` - Main test runner
- `analyze_results.py` - Generates analysis reports for Chapter 5 and 6

### Output (`output/`)
- Test results, logs, and generated reports

## Running Tests

### Run All Tests
```bash
python3 scripts/run_all_tests.py
```

### Run Individual Tests
```bash
python3 gsr_tests/gsr_synthetic_integrity_test.py
python3 thermal_tests/thermal_synthetic_capture_test.py
```

## Test Outputs

Each test generates:
1. Timestamped log files
2. Recorded data files (CSV, images, video)
3. Analysis reports (JSON format)
4. Summary statistics

## Requirements

- Python 3.8+
- pandas, numpy, matplotlib (for analysis)
- pytest (for test framework)
- Android device with app installed (for real sensor tests)

Install requirements:
```bash
pip install -r ../requirements_thesis.txt
```

## Test Descriptions

### 1. GSR Data Integrity Test (Synthetic)
- Uses mock GSR sensor with known signal pattern (sine wave)
- Validates recorded values match expected pattern
- Computes error metrics
- Output: CSV log with analysis report

### 2. GSR Logging Continuity (Real Sensor)
- Records with actual Shimmer3 GSR sensor
- Monitors sample rate consistency
- Detects gaps and missed samples
- Output: Timestamped CSV with rate analysis

### 3. Thermal Frame Capture Verification (Synthetic)
- Uses dummy thermal camera mode
- Verifies frame count and timing
- Tests at 5Hz for 10 seconds (~50 frames)
- Output: Frame timestamps and statistics

### 4. Thermal Camera Recording Test (Real)
- Records with Topdon TC001 (if available)
- Measures actual frame rate (~25Hz)
- Logs frame timestamps
- Output: Frame log with interval statistics

### 5. RGB Video/Frame Capture Performance
- Records video with phone camera
- Extracts JPEG frames simultaneously
- Validates frame count and timing
- Output: MP4 video, JPEG frames, frame log

### 6. Recorded Data File Integrity Check
- Validates all output files from tests
- Checks CSV completeness
- Verifies video file integrity
- Output: Validation report (JSON)
