# Thesis Evaluation Tests - Robustness to Disconnections and Failures

This directory contains evaluation tests for assessing the system's robustness to various types of disconnections and failures as specified in the thesis requirements.

## Test Suite Overview

The robustness test suite includes the following test activities:

### 1. GSR Sensor Reconnection Test (Simulated)
**File:** `robustness_tests/GSRReconnectionSimulatedTest.kt`

**Purpose:** Emulate a Bluetooth disconnection of the Shimmer3 GSR during a session in a controlled way.

**Test Procedure:**
- Simulates disconnect after 20 seconds of recording
- Simulates reconnection after 40 seconds
- Measures data gap duration
- Tracks reconnection attempts
- Logs all events with timestamps

**Expected Output:**
- Log file with timestamped events
- GSR disconnected event
- Reconnection attempts
- GSR reconnected event
- Data gap measurement

**Evaluation Chapters:** Chapter 5 (system behavior), Chapter 6 (resilience evaluation)

### 2. GSR Sensor Reconnection Test (Real Hardware)
**File:** `robustness_tests/GSRReconnectionRealHardwareTest.kt`

**Purpose:** Perform a live run where the GSR device is intentionally turned off or taken out of range mid-recording.

**Test Procedure:**
1. Start recording with GSR device connected
2. After 15-30 seconds, turn off GSR device or move out of range
3. Wait for disconnection detection
4. Turn device back on or move back in range
5. Observe auto-reconnection
6. Stop recording to complete test

**Expected Output:**
- Runtime log with disconnection detection
- Reconnection attempts (if auto-reconnect implemented)
- Time gap in GSR CSV data
- Success/failure of automatic reconnection

**Evaluation Chapters:** Chapter 5 (real scenario), Chapter 6 (robustness conclusions)

### 3. Thermal Camera Disconnection Handling
**File:** `robustness_tests/ThermalCameraDisconnectionTest.kt`

**Purpose:** Test graceful handling of USB thermal camera physical disconnection during recording.

**Test Procedure:**
1. Connect USB thermal camera
2. Start recording session
3. After 15-30 seconds, physically unplug the USB camera
4. Observe system behavior
5. Verify app does not crash
6. Verify other sensors continue recording
7. Stop recording to complete test

**Expected Output:**
- System log with thermal camera disconnected event
- Graceful error handling (no crash)
- Continuation of other sensor streams
- Switch to simulation mode
- Frame count before and after disconnect

**Evaluation Chapters:** Chapter 5 (hardware removal behavior), Chapter 6 (fault tolerance)

### 4. Network Connection Drop Test
**File:** `robustness_tests/NetworkConnectionDropTest.kt`

**Purpose:** Test system behavior when PC control network connection is lost during active session.

**Test Procedure:**
1. Start recording with PC controller connected
2. After 15-30 seconds, disconnect PC (close connection or shut down)
3. Observe network connection loss detection
4. Verify recording continues on phone
5. Wait to observe reconnection attempts
6. Stop recording manually to complete test

**Expected Output:**
- Network debug log with connection loss timestamp
- Client disconnected message
- Recording continuation without interruption
- Duration of recording after network drop
- Reconnection attempts

**Evaluation Chapters:** Chapter 5 (network failure behavior), Chapter 6 (failsafe discussion)

### 5. Sensor Failure Isolation Test
**File:** `robustness_tests/SensorFailureIsolationTest.kt`

**Purpose:** Verify that failure in one sensor module does not affect other sensors.

**Test Procedure:**
1. Select which sensor to fail (GSR, Camera, Thermal, or Audio)
2. Start test with all sensors recording
3. After 5 seconds, induce failure in selected sensor
4. Observe failure containment
5. Verify other sensors continue recording
6. Test completes after demonstrating isolation

**Expected Output:**
- Log showing error in affected sensor
- Confirmation that other sensors continue
- Sample counts for each sensor before and after failure
- Verification of failure containment

**Evaluation Chapters:** Chapter 5 (robustness evidence), Chapter 6 (stability conclusions)

## Running the Tests

### Prerequisites
- Android device with Bluetooth and USB support
- Shimmer3 GSR device (for GSR tests)
- USB thermal camera (for thermal tests)
- PC controller for network tests (optional)

### Installation
These test activities should be integrated into the main app as part of the testing suite.

### Accessing Tests
The tests can be accessed through the Testing Suite Hub Activity in the main application.

### Test Output
All tests generate log files in the device's external storage under:
```
/storage/emulated/0/Android/data/com.csl.irCamera/files/thesis_evaluation/
```

Log file naming convention:
- `gsr_reconnection_simulated_<timestamp>.log`
- `gsr_reconnection_real_<timestamp>.log`
- `thermal_disconnect_<timestamp>.log`
- `network_drop_<timestamp>.log`
- `sensor_isolation_<timestamp>.log`

## Log File Format

Each log file contains timestamped events in the following format:
```
HH:mm:ss.SSS | EVENT_TYPE | STATE_INFO | DESCRIPTION
```

Example:
```
14:23:45.123 | GSR_DISCONNECTED | DISCONNECTED | GSR sensor disconnected (simulated)
14:23:50.456 | RECONNECTION_ATTEMPT | RECONNECTING | Attempting automatic reconnection (attempt 1)
14:24:05.789 | GSR_RECONNECTED | CONNECTED | GSR sensor reconnected after 4 attempts
```

## Test Metrics

Each test collects and reports specific metrics:

### GSR Reconnection Tests
- Total test duration
- Disconnect duration
- Data gap duration (milliseconds)
- Number of reconnection attempts
- Success/failure of reconnection

### Thermal Camera Test
- Time to disconnect
- Frames captured before disconnect
- Frames in simulation mode after disconnect
- System crash status (should be false)
- Graceful handling status

### Network Connection Test
- Time to network drop
- Recording duration after drop
- Number of reconnection attempts
- Data loss status (should be false)
- Recording continuation status

### Sensor Isolation Test
- Time to failure induction
- Failure containment status
- Other sensors continuation status
- Sample counts before/after failure for each sensor

## Integration with Thesis

These tests directly support the thesis evaluation requirements:

1. **Chapter 5 (Implementation):** Demonstrates how the system handles disconnections and failures
2. **Chapter 6 (Evaluation):** Provides measurable data on system resilience and robustness
3. **Subsystems Tested:** GSR/Bluetooth, Thermal camera USB, Network control, Overall system

## Notes

- All tests log extensively to both the test output file and Android logcat
- Tests are designed to be non-destructive and safe to run multiple times
- Simulated tests provide consistent, reproducible results
- Real hardware tests provide realistic evaluation data
- Tests follow the same UI patterns as existing test activities in the app

## Future Enhancements

Potential improvements for future work:
- Automated test execution and report generation
- Integration with CI/CD pipeline
- Statistical analysis of multiple test runs
- Comparison with baseline performance metrics
- Extended duration stress tests
