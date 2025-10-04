# Thesis Evaluation Tests - Usage Instructions

## Overview

This test suite evaluates the data recording correctness and performance of the IRCamera system for thesis chapters 5 and 6. Tests cover GSR sensor recording, thermal camera capture, RGB video recording, and data file integrity.

## Quick Start

### 1. Run Synthetic Tests (No Hardware Required)

These tests generate synthetic data and validate the recording pipeline:

```bash
cd thesis_evaluation/tests
python3 scripts/run_all_tests.py
```

This will:
- Generate synthetic GSR data with known sine wave pattern
- Simulate thermal frame capture at 5 Hz
- Validate synthetic data integrity
- Generate test reports

### 2. Run Complete Test Suite (With Real Data)

After recording sessions with actual hardware, analyze the data:

```bash
cd thesis_evaluation/tests
python3 scripts/run_all_tests.py \
  --gsr-csv /path/to/gsr_session.csv \
  --thermal-csv /path/to/thermal_frames.csv \
  --rgb-video /path/to/video.mp4 \
  --rgb-frames-csv /path/to/rgb_frames.csv \
  --session-dir /path/to/session_directory
```

### 3. Generate Analysis Reports

After running tests, generate formatted reports for thesis chapters:

```bash
python3 scripts/analyze_results.py
```

Reports are saved in `output/analysis/`:
- `chapter5_recording_tests.txt` - Implementation results
- `chapter6_performance_evaluation.txt` - Performance evaluation

## Test Descriptions

### GSR Tests

#### 1. GSR Synthetic Integrity Test
**Purpose**: Validate GSR recording pipeline with known signal pattern  
**Input**: None (generates synthetic data)  
**Output**: CSV with sine wave pattern, error metrics  
**Run**: `python3 gsr_tests/gsr_synthetic_integrity_test.py`

**Expected Results**:
- 1280 samples (10 seconds at 128 Hz)
- Near-zero error (< 0.001 µS)
- RMSE < 0.001 µS

#### 2. GSR Real Sensor Continuity Test
**Purpose**: Analyze real Shimmer3 GSR sensor recording continuity  
**Input**: GSR CSV data file from actual recording  
**Output**: Rate analysis report with gap detection  
**Run**: `python3 gsr_tests/gsr_real_sensor_continuity_test.py <csv_file>`

**Expected Results**:
- Average rate: 128 ± 5 Hz
- No gaps > 20 ms
- Sample loss < 5%

**Arguments**:
- `--rate` - Expected sampling rate (default: 128.0 Hz)
- `--gap-threshold` - Gap detection threshold (default: 20.0 ms)

### Thermal Tests

#### 3. Thermal Synthetic Capture Test
**Purpose**: Validate thermal frame capture timing in simulation mode  
**Input**: None (generates synthetic frames)  
**Output**: Frame log with timing statistics  
**Run**: `python3 thermal_tests/thermal_synthetic_capture_test.py`

**Expected Results**:
- 50 frames (10 seconds at 5 Hz)
- FPS deviation < 0.5 Hz
- Consistent frame intervals (200 ± 50 ms)

#### 4. Thermal Real Camera Test
**Purpose**: Analyze Topdon TC001 thermal camera recording  
**Input**: Thermal frame log CSV from actual recording  
**Output**: FPS analysis with drop detection  
**Run**: `python3 thermal_tests/thermal_real_camera_test.py <csv_file>`

**Expected Results**:
- Target FPS: ~25 Hz
- FPS deviation < 5 Hz
- Frame drops < 5%

**Arguments**:
- `--fps` - Target frame rate (default: 25.0 Hz)
- `--drop-threshold` - Drop detection threshold (default: 100.0 ms)

### RGB Tests

#### 5. RGB Video Performance Test
**Purpose**: Validate RGB video recording and frame capture  
**Input**: MP4 video file and frame capture CSV  
**Output**: Video metadata and frame loss analysis  
**Run**: `python3 rgb_tests/rgb_video_performance_test.py <video_file> <frames_csv>`

**Expected Results**:
- Video file size > 0.1 MB
- Frame loss < 5%
- Interval variance < 50 ms

**Arguments**:
- `--fps` - Target frame rate (default: 30.0 Hz)
- `--duration` - Expected duration (default: 60.0 seconds)

### Data Integrity Tests

#### 6. File Integrity Validator
**Purpose**: Validate all recorded data files for completeness  
**Input**: Session directory with recorded files  
**Output**: Integrity report for all files  
**Run**: `python3 data_integrity/file_integrity_validator.py <session_dir>`

**Validates**:
- CSV files: header, row count, completeness
- Video files: file size, codec, duration
- Image files: format, dimensions

## Test Output Files

### CSV Data Files
- `gsr_synthetic_YYYYMMDD_HHMMSS.csv` - Synthetic GSR samples
- `thermal_synthetic_YYYYMMDD_HHMMSS.csv` - Synthetic thermal frames

### Result Files (JSON)
- `gsr_synthetic_result_*.json` - GSR synthetic test results
- `gsr_continuity_result_*.json` - GSR real sensor analysis
- `thermal_synthetic_result_*.json` - Thermal synthetic results
- `thermal_real_result_*.json` - Thermal real camera analysis
- `rgb_video_result_*.json` - RGB video analysis
- `file_integrity_result_*.json` - File integrity report
- `test_suite_report_*.json` - Complete suite summary

### Analysis Reports
- `chapter5_recording_tests.txt` - Chapter 5 documentation
- `chapter6_performance_evaluation.txt` - Chapter 6 evaluation

## Interpreting Results

### Pass/Fail Criteria

**GSR Synthetic Test**:
- ✓ PASS: Error < 0.001 µS
- ✗ FAIL: Error ≥ 0.001 µS

**GSR Real Sensor Test**:
- ✓ PASS: Rate deviation < 5 Hz, sample loss < 5%
- ✗ FAIL: Rate deviation ≥ 5 Hz or sample loss ≥ 5%

**Thermal Tests**:
- ✓ PASS: FPS deviation < 0.5 Hz (synthetic) or < 5 Hz (real)
- ✗ FAIL: FPS deviation exceeds threshold

**RGB Video Test**:
- ✓ PASS: Frame loss < 5%, file size > 0.1 MB
- ✗ FAIL: Frame loss ≥ 5% or corrupted file

**File Integrity**:
- ✓ PASS: All files valid and complete
- ✗ FAIL: Any file missing, empty, or corrupted

### Performance Metrics

Key metrics reported for thesis:

**GSR Recording**:
- Sample rate accuracy (deviation from 128 Hz)
- Timestamp consistency
- Gap detection
- Data completeness

**Thermal Recording**:
- Frame rate accuracy
- Frame interval consistency
- Frame drop detection
- Recording duration

**RGB Recording**:
- Video file integrity
- Frame capture rate
- Frame loss percentage
- File size validation

## Example Workflows

### Workflow 1: Initial Development Testing

```bash
# Run synthetic tests to validate pipeline
cd thesis_evaluation/tests
python3 scripts/run_all_tests.py

# Generate initial reports
python3 scripts/analyze_results.py
```

### Workflow 2: Hardware Validation

```bash
# 1. Record session with app (GSR + Thermal + RGB)
# 2. Copy recorded files to test machine
# 3. Run complete test suite

cd thesis_evaluation/tests
python3 scripts/run_all_tests.py \
  --gsr-csv ~/recordings/session_20241004/gsr_data.csv \
  --thermal-csv ~/recordings/session_20241004/thermal_frames.csv \
  --rgb-video ~/recordings/session_20241004/video.mp4 \
  --rgb-frames-csv ~/recordings/session_20241004/rgb_frames.csv \
  --session-dir ~/recordings/session_20241004

# 4. Generate thesis reports
python3 scripts/analyze_results.py

# 5. Review reports in output/analysis/
```

### Workflow 3: Continuous Testing

```bash
# Run tests after each recording session
for session in ~/recordings/session_*; do
    echo "Testing $session..."
    python3 scripts/run_all_tests.py \
        --gsr-csv "$session/gsr_data.csv" \
        --session-dir "$session"
done

# Generate aggregated analysis
python3 scripts/analyze_results.py
```

## Troubleshooting

### Common Issues

**Issue**: `No test results found`  
**Solution**: Run tests first with `run_all_tests.py`

**Issue**: `File not found: <data_file>`  
**Solution**: Verify file paths are correct and files exist

**Issue**: `ffprobe not available`  
**Solution**: Install ffmpeg for video metadata extraction:
```bash
# Ubuntu/Debian
sudo apt-get install ffmpeg

# macOS
brew install ffmpeg
```

**Issue**: Test times out  
**Solution**: Tests have 5-minute timeout. For large files, run individual tests directly.

### Debug Mode

Run individual tests with Python's verbose mode:

```bash
python3 -v gsr_tests/gsr_synthetic_integrity_test.py
```

### Viewing Raw Results

All results are stored as JSON files in `output/` subdirectories. View with:

```bash
cat output/gsr_tests/gsr_synthetic_result_*.json | python3 -m json.tool
```

## Requirements

- Python 3.8+
- Standard library only (no external dependencies for core tests)
- Optional: ffmpeg (for video metadata extraction)
- Optional: Pillow (for image dimension validation)

## For Thesis Documentation

### Chapter 5: Implementation Results

Use the `chapter5_recording_tests.txt` report which includes:
- Test execution results
- Sample counts and rates
- Frame counts and timing
- File validation results

### Chapter 6: Evaluation and Discussion

Use the `chapter6_performance_evaluation.txt` report which includes:
- Performance metrics summary
- Deviation from requirements
- Pass/fail analysis
- System reliability assessment

### Including in Thesis

Example citation format:

```
The GSR recording pipeline was validated using synthetic data 
(1280 samples at 128 Hz) with a mean error of 0.000 µS and 
RMSE of 0.000 µS (see Appendix A, Test Suite Report).
```

## Support

For issues or questions:
1. Check test output logs in `output/` directories
2. Review error messages in JSON result files
3. Verify input file formats match expected structure
4. Consult README.md for additional information
