# Quick Start Guide - Thesis Evaluation Tests

Run these commands to execute all evaluation tests for the thesis deliverable.

## Prerequisites

```bash
cd /home/runner/work/IRCamera/IRCamera
pip install -r pc-controller/requirements.txt
```

## 1. Synthetic Clock Offset Test (No Hardware Required)

```bash
cd thesis_evaluation/synthetic_clock_offset
python3 test_synthetic_sync.py
```

**Output:**
- `synthetic_sync_low_latency.csv` - Low latency test results
- `synthetic_sync_high_latency.csv` - High latency test results  
- `synthetic_sync_high_jitter.csv` - High jitter test results

**Expected Results:**
- 100% success rate
- Offset measurement error <1ms
- Sync quality: EXCELLENT/GOOD

## 2. Sensor Start Alignment Test (Synthetic)

```bash
cd thesis_evaluation/sensor_start_alignment
python3 test_sensor_start_alignment.py
```

**Output:**
- `sensor_start_alignment_synthetic.csv` - Startup latency results
- `sensor_start_alignment_synthetic.json` - Detailed JSON log

**Expected Results:**
- GSR startup: ~50ms
- Thermal startup: ~120ms
- RGB startup: ~80ms
- Max difference: ~200ms (acceptable threshold)

## 3. Real Hardware Tests (Requires Android Device)

### Real Hardware Clock Offset Test

```bash
cd thesis_evaluation/real_hardware_clock_offset
python3 test_real_hardware_sync.py 8080
```

Then connect Android device and start recording. Press Ctrl+C when done.

**Output:**
- `real_hardware_sync_results.csv`
- `real_hardware_sync_results.json`

### Real Hardware Sensor Alignment Analysis

1. Record a session on Android
2. Pull the data:
```bash
adb pull /sdcard/Android/data/com.uclgsr.ircamera/files/<session_id>/ ./real_session_data/
```
3. Analyze:
```bash
cd thesis_evaluation/sensor_start_alignment
python3 test_sensor_start_alignment.py ./real_session_data <session_id>
```

**Output:**
- `sensor_start_alignment_real_hardware.csv`
- `sensor_start_alignment_real_hardware.json`

## Results Location

All test results are saved in the respective test directories:
- `thesis_evaluation/synthetic_clock_offset/*.csv`
- `thesis_evaluation/sensor_start_alignment/*.csv`
- `thesis_evaluation/real_hardware_clock_offset/*.csv`

## For Thesis Chapter 5 & 6

Use the CSV files to create:
- Tables of timing precision metrics
- Graphs showing offset distribution
- Analysis of synchronization quality
- Comparison of synthetic vs real hardware results
