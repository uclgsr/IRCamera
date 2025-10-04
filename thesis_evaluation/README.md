# Thesis Evaluation - Network Command Handling and Control Tests

This directory contains automated tests for evaluating the network command handling and control capabilities of the IRCamera system. These tests are designed to validate the PC-to-phone control loop and provide detailed metrics for thesis documentation.

## Overview

The tests implement the requirements specified in the thesis evaluation issue for Chapter 5 (demonstrating successful remote session control and responsiveness) and Chapter 6 (discussing control efficiency and overall evaluation).

## Test Suite

### Test 1: Remote Start/Stop Command Test
**File:** `test_1_remote_start_stop.py`

**Purpose:** Validates the basic PC-to-phone control loop by sending START and STOP commands in sequence.

**Output:**
- Detailed event log with timestamps
- Command latency measurements (PC send time to phone action completion)
- JSON report and human-readable text log

**Key Metrics:**
- START command latency
- STOP command latency
- Total session duration
- Connection establishment time

**Usage:**
```bash
python test_1_remote_start_stop.py --device-ip <ANDROID_IP> [--port 8080] [--duration 10]
```

**Example:**
```bash
python test_1_remote_start_stop.py --device-ip 192.168.1.100 --duration 15
```

---

### Test 2: Command Latency and Throughput Metric Test
**File:** `test_2_command_latency_throughput.py`

**Purpose:** Measures how quickly and consistently the system processes incoming commands under normal conditions.

**Output:**
- Table with PC send time, Phone receive time, and Action completed time for each command
- Statistical analysis (min/max/avg/median latency)
- Throughput measurements (commands per second)

**Key Metrics:**
- Round-trip latency for each command type (PING, SYNC, GET_STATUS, START, STOP)
- Throughput under burst conditions
- Command success rate

**Usage:**
```bash
python test_2_command_latency_throughput.py --device-ip <ANDROID_IP> [--port 8080] [--iterations 10]
```

**Example:**
```bash
python test_2_command_latency_throughput.py --device-ip 192.168.1.100 --iterations 20
```

---

### Test 3: Edge-case Command Handling Log Test
**File:** `test_3_edge_case_commands.py`

**Purpose:** Stress-tests the command parser with out-of-order and invalid commands to verify robustness.

**Test Scenarios:**
1. Send STOP when no recording is active
2. Send duplicate START commands rapidly
3. Send burst of SYNC requests back-to-back
4. Send invalid/malformed commands
5. Send commands in wrong sequence

**Output:**
- System log detailing how each unexpected command is handled
- Success/failure status for each edge case
- Error handling verification

**Key Metrics:**
- Number of edge cases tested
- System stability (no crashes)
- Appropriate error responses

**Usage:**
```bash
python test_3_edge_case_commands.py --device-ip <ANDROID_IP> [--port 8080]
```

**Example:**
```bash
python test_3_edge_case_commands.py --device-ip 192.168.1.100
```

---

### Test 4: Multi-command Sequence Automation Test
**File:** `test_4_multi_command_sequence.py`

**Purpose:** Automates full-session scenarios with complex command sequences to ensure realistic usage patterns work correctly.

**Scenarios:**
1. **Basic recording with periodic sync:** START -> SYNC (every 3s) -> STOP
2. **Recording with pause:** START -> SYNC -> STATUS -> (pause) -> SYNC -> STATUS -> STOP
3. **Intensive command sequence:** Rapid succession of commands (PING, SYNC, STATUS)
4. **Complete workflow:** Full lifecycle including pre-flight checks and post-stop verification

**Output:**
- Combined timeline of all events
- JSON array with command events and sensor status changes
- Timeline visualization showing command events over time

**Key Metrics:**
- Total commands executed
- Timeline of all events with relative timestamps
- Success rate for complex scenarios

**Usage:**
```bash
python test_4_multi_command_sequence.py --device-ip <ANDROID_IP> [--port 8080] [--scenario all|1|2|3|4]
```

**Examples:**
```bash
# Run all scenarios
python test_4_multi_command_sequence.py --device-ip 192.168.1.100 --scenario all

# Run specific scenario
python test_4_multi_command_sequence.py --device-ip 192.168.1.100 --scenario 3
```

---

## Prerequisites

### System Requirements
- Python 3.7 or higher
- Network connectivity to Android device
- Android device running IRCamera app with network server enabled

### Python Dependencies
Install required packages:
```bash
pip install -r ../requirements_thesis.txt
```

Or manually:
```bash
pip install typing-extensions
```

### Android Device Setup
1. Launch the IRCamera app on Android device
2. Ensure the network server is running (auto-starts with app)
3. Note the device's IP address (shown in app or via network settings)
4. Ensure port 8080 is accessible (default port)

---

## Running the Tests

### Quick Start
1. Ensure Android device is on the same network as PC
2. Note the Android device IP address
3. Run individual tests:

```bash
# Test 1: Basic start/stop
python test_1_remote_start_stop.py --device-ip 192.168.1.100

# Test 2: Latency measurement
python test_2_command_latency_throughput.py --device-ip 192.168.1.100

# Test 3: Edge cases
python test_3_edge_case_commands.py --device-ip 192.168.1.100

# Test 4: Full workflow
python test_4_multi_command_sequence.py --device-ip 192.168.1.100
```

### Run All Tests
Use the master test runner:
```bash
python run_all_tests.py --device-ip 192.168.1.100
```

---

## Output Files

Each test generates two types of output files:

### JSON Reports
- Machine-readable format with complete test data
- Includes timestamps, measurements, and detailed results
- Suitable for automated analysis and visualization

**Files:**
- `test_1_start_stop_report_<timestamp>.json`
- `test_2_latency_throughput_report_<timestamp>.json`
- `test_3_edge_cases_report_<timestamp>.json`
- `test_4_multi_sequence_report_<timestamp>.json`

### Text Logs
- Human-readable format
- Event logs, tables, and summaries
- Suitable for direct inclusion in thesis documentation

**Files:**
- `test_1_start_stop_report_<timestamp>.txt`
- `test_2_latency_throughput_report_<timestamp>.txt`
- `test_3_edge_cases_report_<timestamp>.txt`
- `test_4_multi_sequence_report_<timestamp>_timeline.txt`

---

## Interpreting Results

### Success Criteria

**Test 1 (Start/Stop):**
- Connection established successfully
- START command acknowledged
- STOP command acknowledged
- Latency < 100ms for local network

**Test 2 (Latency/Throughput):**
- Average latency < 50ms for PING commands
- Average latency < 100ms for other commands
- Throughput > 5 commands/second
- No command failures

**Test 3 (Edge Cases):**
- System handles invalid commands without crashing
- Appropriate error responses for out-of-order commands
- Duplicate commands properly rejected
- All test cases complete successfully

**Test 4 (Multi-command Sequence):**
- All scenarios execute without errors
- Commands processed in correct order
- No missed or lost commands
- Timeline shows consistent behavior

### Performance Benchmarks

Based on typical local network conditions:
- **Excellent:** < 20ms latency, > 10 commands/sec
- **Good:** 20-50ms latency, 5-10 commands/sec
- **Acceptable:** 50-100ms latency, 2-5 commands/sec
- **Poor:** > 100ms latency, < 2 commands/sec

---

## Troubleshooting

### Connection Issues
**Problem:** Cannot connect to Android device

**Solutions:**
- Verify device IP address is correct
- Check both devices are on same network
- Ensure app is running on Android
- Check firewall settings
- Try using device IP instead of hostname

### Command Failures
**Problem:** Commands not acknowledged or timing out

**Solutions:**
- Increase timeout with longer `--duration` or `--iterations`
- Check Android app logs for errors
- Verify network stability
- Restart Android app

### Test Hangs
**Problem:** Test appears to hang

**Solutions:**
- Check Android device is responsive
- Verify network connection is stable
- Use Ctrl+C to interrupt and retry
- Check Android app hasn't crashed

---

## For Thesis Documentation

### Chapter 5: Results
Use the output files to demonstrate:
- Successful remote session control
- Measured responsiveness and latency
- System stability under various conditions
- Quantitative performance metrics

### Chapter 6: Evaluation
Use the test results to discuss:
- Control efficiency (from latency measurements)
- Reliability (from edge-case handling)
- Orchestration capabilities (from multi-command sequences)
- Overall system performance evaluation

### Suggested Visualizations
1. **Latency Box Plots:** Show distribution of latencies by command type
2. **Timeline Diagrams:** Visualize command sequences from Test 4
3. **Performance Tables:** Min/max/avg latencies from Test 2
4. **Edge Case Matrix:** Show pass/fail for each edge case scenario

---

## Contributing

These tests are designed to be run independently. To add new test scenarios:

1. Create a new test file following the naming convention
2. Implement using the CommandClient class
3. Generate both JSON and text outputs
4. Update this README with test description
5. Add to run_all_tests.py if creating a new test suite

---

## License

This test suite is part of the IRCamera project. See main project LICENSE for details.

---

## Contact

For issues or questions about these tests, please refer to the main IRCamera repository issue tracker.
