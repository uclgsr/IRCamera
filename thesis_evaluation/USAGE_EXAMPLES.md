# Thesis Evaluation Tests - Usage Examples

## Quick Start

### Install Dependencies

```bash
# Install pytest (required)
pip install pytest

# Optional: Install for enhanced reporting
pip install pytest-html pytest-cov
```

### Run All Tests

```bash
# Using the test runner script
python3 thesis_evaluation/run_tests.py

# Or using pytest directly
python3 -m pytest thesis_evaluation/ -v
```

## Running Specific Test Categories

### Test 1: Cross-Sensor Timeline Alignment

```bash
# Run all alignment tests
python3 -m pytest thesis_evaluation/test_cross_sensor_alignment.py -v

# Run specific test
python3 -m pytest thesis_evaluation/test_cross_sensor_alignment.py::TestCrossSensorAlignment::test_inject_synthetic_event_at_10_seconds -v
```

**Output:** Check `thesis_evaluation/outputs/cross_sensor_alignment/`
- `synthetic_event_10s_*.csv` - Event logs with timestamps
- `synthetic_event_timeline_*.json` - Combined sensor timelines

### Test 2: Multi-Stream Sync Marker

```bash
# Run all sync marker tests
python3 -m pytest thesis_evaluation/test_multistream_sync_marker.py -v

# Run specific test
python3 -m pytest thesis_evaluation/test_multistream_sync_marker.py::TestMultiStreamSyncMarker::test_sync_command_mid_session -v
```

**Output:** Check `thesis_evaluation/outputs/multistream_sync/`
- `sync_command_mid_session_*.csv` - SYNC event logs
- `multiple_sync_markers_*.csv` - Multiple SYNC command logs

### Test 3: Session Duration Consistency

```bash
# Run all duration tests
python3 -m pytest thesis_evaluation/test_session_duration_consistency.py -v

# Run specific test
python3 -m pytest thesis_evaluation/test_session_duration_consistency.py::TestSessionDurationConsistency::test_complete_session_report -v
```

**Output:** Check `thesis_evaluation/outputs/session_duration/`
- `complete_session_report_*.csv` - Full session analysis
- `start_time_consistency_*.csv` - Start time analysis
- `end_time_consistency_*.csv` - End time analysis

## Interpreting Test Outputs

### Sync Event Log (CSV)

```csv
base_timestamp,sensors_in_sync,time_spread_ms
1759578760607,"RGB,GSR,Thermal",47
```

**Interpretation:**
- `base_timestamp`: Event detection time (milliseconds)
- `sensors_in_sync`: List of sensors that detected the event
- `time_spread_ms`: Time difference between first and last sensor (should be ≤100ms)

### Session Duration Report (CSV)

```csv
sensor,start_time,end_time,duration_ms,sample_count
GSR,1759578750785,1759579050777,299992,38400
Thermal,1759578750805,1759579050705,299900,3000
RGB,1759578750795,1759579050761,299966,9000

Consistency Analysis
metric,value
consistent,True
start_time_spread_ms,20
end_time_spread_ms,72
duration_variance_ms,92
```

**Interpretation:**
- **Sensor rows**: Individual sensor timing information
  - `start_time`: Session start timestamp
  - `end_time`: Session end timestamp
  - `duration_ms`: Total recording duration
  - `sample_count`: Number of samples/frames recorded
  
- **Consistency Analysis**:
  - `consistent`: Overall consistency verdict (True/False)
  - `start_time_spread_ms`: Max difference in start times (should be ≤100ms)
  - `end_time_spread_ms`: Max difference in end times (should be ≤1000ms)
  - `duration_variance_ms`: Max variance in durations (should be ≤1000ms)

## Advanced Usage

### Generate HTML Report

```bash
python3 thesis_evaluation/run_tests.py --html

# View report at: thesis_evaluation/reports/test_report.html
```

### Run with Coverage

```bash
python3 thesis_evaluation/run_tests.py --cov

# View coverage report at: thesis_evaluation/reports/coverage/index.html
```

### Run Tests Quietly

```bash
python3 -m pytest thesis_evaluation/ -q
```

### Run Single Test with Detailed Output

```bash
python3 -m pytest thesis_evaluation/test_cross_sensor_alignment.py::TestCrossSensorAlignment::test_inject_synthetic_event_at_10_seconds -vv -s
```

## Using Test Outputs for Thesis

### Chapter 5: Results

**Demonstrating Synchronized Sensor Behavior:**

1. Run alignment tests:
   ```bash
   python3 -m pytest thesis_evaluation/test_cross_sensor_alignment.py -v
   ```

2. Collect outputs from `thesis_evaluation/outputs/cross_sensor_alignment/`

3. In thesis, present:
   - Sync event logs showing all sensors detecting events within 100ms
   - Timeline charts showing event markers across sensors
   - GSR spike detection at synthetic event times

**Example thesis text:**
> "To validate cross-sensor synchronization, we injected a synthetic reference event at t=10.0s across all data streams. As shown in Table X.Y, all three sensors (GSR, thermal, and RGB) detected the event within 47ms of each other, well within the 100ms tolerance requirement."

### Chapter 6: Discussion & Evaluation

**Analyzing Timeline Alignment Effectiveness:**

1. Run multi-stream sync tests:
   ```bash
   python3 -m pytest thesis_evaluation/test_multistream_sync_marker.py -v
   ```

2. Collect SYNC command logs from `thesis_evaluation/outputs/multistream_sync/`

3. In thesis, discuss:
   - Effectiveness of SYNC commands during live recording
   - Phone clock consistency across sensors
   - Real-world synchronization performance

**Evaluating Session Duration Consistency:**

1. Run duration consistency tests:
   ```bash
   python3 -m pytest thesis_evaluation/test_session_duration_consistency.py -v
   ```

2. Collect duration reports from `thesis_evaluation/outputs/session_duration/`

3. In thesis, analyze:
   - Start/end time alignment across sensors
   - Detection of sensor lag or lead
   - Sample count validation against expected rates

## Troubleshooting

### Tests Not Found

```bash
# Ensure you're in the project root directory
cd /path/to/IRCamera

# Run from root
python3 -m pytest thesis_evaluation/
```

### Import Errors

```bash
# Install pytest
pip install pytest

# Verify installation
python3 -m pytest --version
```

### Output Files Not Generated

```bash
# Check that output directories exist
ls thesis_evaluation/outputs/

# If missing, pytest will create them automatically
# Run tests again
python3 -m pytest thesis_evaluation/ -v
```

### Permission Errors

```bash
# Make run_tests.py executable
chmod +x thesis_evaluation/run_tests.py

# Run with python3 directly
python3 thesis_evaluation/run_tests.py
```

## Expected Test Results

All 19 tests should pass:
- ✓ 6 cross-sensor alignment tests (100% pass rate)
- ✓ 5 multi-stream sync marker tests (100% pass rate)
- ✓ 8 session duration consistency tests (100% pass rate)

**Total test execution time:** ~0.6 seconds

**Output files generated:** ~33 CSV files + JSON files

## Summary

These tests provide comprehensive validation of multi-sensor data consistency for thesis chapters 5 and 6. All tests generate detailed output artifacts that can be directly referenced in thesis text, tables, and figures to demonstrate system performance and validate design decisions.
