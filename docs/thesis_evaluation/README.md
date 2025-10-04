# Thesis Evaluation Tests

This directory contains evaluation tests for the thesis deliverable, specifically focused on timing precision and synchronization.

## Overview

Three test suites are provided to evaluate different aspects of time synchronization and sensor coordination:

1. **Synthetic Clock Offset Measurement** - Simulated time-sync handshake tests
2. **Real Hardware Clock Offset Measurement** - Actual sync protocol with PC and phone
3. **Sensor Start Time Alignment** - Multi-sensor startup latency measurement

## Test Structure

```
docs/thesis_evaluation/
├── README.md                              # This file
├── synthetic_clock_offset/                # Simulated sync tests
│   └── test_synthetic_sync.py
├── real_hardware_clock_offset/            # Real hardware sync tests
│   └── test_real_hardware_sync.py
└── sensor_start_alignment/                # Sensor timing tests
    └── test_sensor_start_alignment.py
```

## Prerequisites

### Python Dependencies

Install required Python packages:

```bash
cd pc-controller
pip install -r requirements.txt
```

### For Real Hardware Tests

- Android device with IRCamera app installed
- PC and Android on the same network
- USB debugging enabled (for log analysis)

## Test 1: Synthetic Clock Offset Measurement

**Purpose:** Evaluate timing precision and jitter in a controlled simulated environment.

**Output:** CSV log files with sync metrics (offset, RTT, quality)

**Thesis Relevance:** Chapter 5 (sync accuracy), Chapter 6 (timing alignment)

### Running the Test

```bash
cd docs/thesis_evaluation/synthetic_clock_offset
python test_synthetic_sync.py
```

### What It Does

- Simulates multiple SYNC command exchanges between PC and Android
- Tests different network conditions (low/high latency, variable jitter)
- Measures:
  - Clock offset (ms)
  - Round-trip time (RTT)
  - Measurement accuracy
  - Sync quality distribution

### Output Files

- `synthetic_sync_low_latency.csv` - Results for low latency scenario
- `synthetic_sync_high_latency.csv` - Results for high latency scenario
- `synthetic_sync_high_jitter.csv` - Results for high jitter scenario

### Expected Results

- Mean offset measurement error: <5ms
- RTT for local network: 10-50ms
- Sync quality: Mostly EXCELLENT/GOOD

## Test 2: Real Hardware Clock Offset Measurement

**Purpose:** Measure actual clock offset and drift with real hardware.

**Output:** CSV and JSON timestamp logs of PC vs phone times

**Thesis Relevance:** Chapter 5 (real-world precision), Chapter 6 (temporal accuracy)

### Running the Test

1. **Start the test server on PC:**

```bash
cd docs/thesis_evaluation/real_hardware_clock_offset
python test_real_hardware_sync.py [port]
```

Default port: 8080

2. **Connect Android device:**
   - Open IRCamera app
   - Go to Network settings
   - Enter PC IP address and port
   - Tap "Connect"

3. **Start recording session:**
   - The app will automatically send SYNC_INIT
   - Multiple syncs will be performed during the session

4. **Stop test:**
   - Press Ctrl+C to stop and save results

### What It Does

- Accepts real Android device connection
- Handles SYNC_INIT messages from device
- Performs full time synchronization protocol
- Logs all sync events with precise timestamps
- Analyzes clock drift over time

### Output Files

- `real_hardware_sync_results.csv` - Tabular sync results
- `real_hardware_sync_results.json` - Detailed JSON log

### Expected Results

- Clock offset: Depends on device/network (typically <100ms)
- RTT: 10-50ms on local network
- Clock drift: <10ms over 10 minutes

## Test 3: Sensor Start Time Alignment

**Purpose:** Validate that all sensors start nearly simultaneously.

**Output:** Table of sensor startup latencies relative to START trigger

**Thesis Relevance:** Chapter 5 (timing alignment), Chapter 6 (startup delays)

### Running Synthetic Test

```bash
cd docs/thesis_evaluation/sensor_start_alignment
python test_sensor_start_alignment.py
```

### Running Real Hardware Analysis

1. **Record a session on Android device**

2. **Pull the session data:**

```bash
adb pull /sdcard/Android/data/com.uclgsr.ircamera/files/<session_id>/ ./real_session_data/
```

3. **Analyze the logs:**

```bash
cd docs/thesis_evaluation/sensor_start_alignment
python test_sensor_start_alignment.py ./real_session_data <session_id>
```

### What It Does

**Synthetic mode:**
- Simulates GSR, Thermal, and RGB sensors
- Measures startup latency for each sensor
- Calculates cross-sensor synchronization

**Real hardware mode:**
- Reads actual sensor data files
- Extracts first timestamp from each sensor
- Calculates relative startup latencies

### Output Files

- `sensor_start_alignment_synthetic.csv` - Synthetic test results
- `sensor_start_alignment_synthetic.json` - JSON format
- `sensor_start_alignment_real_hardware.csv` - Real hardware results
- `sensor_start_alignment_real_hardware.json` - JSON format

### Expected Results

- GSR startup: 50-100ms
- Thermal camera startup: 80-200ms
- RGB camera startup: 50-150ms
- Max difference between sensors: <200ms (acceptable)
- Synchronization quality: EXCELLENT (>90% within threshold)

## Integration with Thesis

### Chapter 5: Results

These tests provide quantitative data for:

- **Timing precision metrics** (offset accuracy, jitter)
- **Synchronization quality distribution** (EXCELLENT/GOOD/FAIR/POOR)
- **Network performance impact** (RTT, latency effects)
- **Multi-sensor coordination** (startup alignment)

### Chapter 6: Discussion & Conclusions

The tests support discussion of:

- **Clock synchronization accuracy** in real-world conditions
- **Temporal alignment** across multiple sensor modalities
- **System limitations** (network jitter, sensor delays)
- **Recommendations** for deployment scenarios

## Interpreting Results

### Sync Quality Ratings

- **EXCELLENT:** RTT <50ms - Ideal for precise synchronization
- **GOOD:** RTT 50-100ms - Acceptable for most use cases
- **FAIR:** RTT 100-200ms - May impact temporal accuracy
- **POOR:** RTT >200ms - Requires network improvement

### Acceptable Thresholds

- **Clock offset measurement error:** <10ms
- **Cross-sensor startup difference:** <200ms
- **Clock drift rate:** <1ms/minute

## Troubleshooting

### Synthetic Tests Fail

- Check that `pc-controller` directory is accessible
- Ensure `sync_handler.py` is present
- Verify Python version (3.7+)

### Real Hardware Test: Device Won't Connect

- Verify PC and Android are on same network
- Check firewall settings (allow port 8080)
- Ensure IRCamera app has network permissions
- Try different port: `python test_real_hardware_sync.py 8081`

### Real Hardware Test: No SYNC_INIT Received

- Start a recording session in the app
- Check Android logs: `adb logcat | grep SYNC_INIT`
- Verify network configuration in app settings

### Sensor Alignment: Can't Find Sensor Files

- Ensure session completed successfully
- Check file paths in Android app logs
- Verify files were pulled from correct directory
- Look for: `gsr_data.csv`, `thermal_data.csv`, `rgb_timestamps.csv`

## Quick Start

For a complete evaluation run:

```bash
# 1. Synthetic tests (no hardware needed)
cd docs/thesis_evaluation/synthetic_clock_offset
python test_synthetic_sync.py

cd ../sensor_start_alignment
python test_sensor_start_alignment.py

# 2. Real hardware tests (requires Android device)
cd ../real_hardware_clock_offset
python test_real_hardware_sync.py
# Connect Android device and start recording
# Press Ctrl+C when done

# 3. All results will be saved as CSV and JSON files
```

## References

- Time Synchronization Implementation: `../TESTING_TIME_SYNC.md`
- Sync Handler Source: `../pc-controller/sync_handler.py`
- Protocol Specification: `../TIME_SYNC_IMPLEMENTATION_SUMMARY.md`
Multi-sensor data consistency tests for thesis evaluation (Chapters 5 and 6).

## Test Categories

### 1. Cross-Sensor Timeline Alignment (Synthetic Event)
Tests synchronized reference event injection across all data streams.

**Purpose**: Verify that all sensors record events simultaneously within acceptable tolerance.

**Output**: Combined log/chart showing GSR spike, thermal marker, and RGB timestamp at injected event time.

**Subsystem**: Multi-sensor integration (timing consistency)

**Chapters**: Chapter 5 (synchronized sensor behavior) and Chapter 6 (cross-modal synchronization)

### 2. Multi-Stream Sync Marker Test (Real Run)
Tests synchronization during real hardware recording with SYNC command.

**Purpose**: Verify sensor modalities remain time-aligned during actual recording session.

**Output**: Logs from each sensor stream around sync event with timestamped entries.

**Subsystem**: System synchronization (GSR, thermal, RGB combined)

**Chapters**: Chapter 5 (sensor data correlation) and Chapter 6 (timeline alignment effectiveness)

### 3. Session Duration Consistency Check
Tests recording duration consistency across all sensor streams.

**Purpose**: Verify all streams covered the same overall period.

**Output**: CSV/table with start/end timestamps from each sensor log.

**Subsystem**: Session controller (start/stop coordination)

**Chapters**: Chapter 5 (concurrent sensor operation) and Chapter 6 (sensor lag/lead analysis)

## Running Tests

```bash
# Run all thesis evaluation tests
python -m pytest docs/thesis_evaluation/

# Run specific test module
python -m pytest docs/thesis_evaluation/test_cross_sensor_alignment.py -v

# Run with detailed output
python -m pytest docs/thesis_evaluation/ -v --tb=short

# Generate test report
python -m pytest docs/thesis_evaluation/ --html=docs/thesis_evaluation/report.html
```

## Test Requirements

- Python 3.8+
- pytest
- pandas (optional, for enhanced reporting)
- numpy (optional, for data analysis)

Install dependencies:
```bash
pip install -r requirements_thesis.txt
```

## Test Data

Tests use both simulated and real session data:
- Simulated data: Generated within tests for controlled scenarios
- Real data: Located in `pc-controller/exports/` directory

## Test Outputs

Test outputs are saved to:
- `docs/thesis_evaluation/outputs/` - Generated test artifacts
- `docs/thesis_evaluation/reports/` - Test result reports
- `docs/thesis_evaluation/charts/` - Visualization outputs (if matplotlib available)
