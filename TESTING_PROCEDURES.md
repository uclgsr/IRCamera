# Comprehensive Testing Procedures for IRCamera Multi-Modal Platform

## Overview

This document provides systematic testing procedures for all modalities and features of the IRCamera Multi-Modal Physiological Sensing Platform. The testing strategy focuses on **MVP functionality** with **no stub implementations**, ensuring real hardware validation and evidence collection for thesis Chapter 5.

## Testing Philosophy

- **MVP-First Approach**: All tests validate actual functionality, no placeholder or stub implementations
- **Hardware Integration**: Real device testing with Shimmer3 GSR+, Topdon TC001, Samsung Galaxy S22
- **Multi-Modal Coordination**: Cross-sensor synchronization and integration validation
- **Evidence Collection**: Quantitative metrics for thesis evaluation
- **Production Readiness**: Comprehensive error handling and performance validation

## Test Categories

### 1. Shimmer GSR Tests

#### 1.1 Device Discovery and Connection
**Objective**: Validate Shimmer3 GSR+ device discovery and connection flow

**Prerequisites**:
- Shimmer3 GSR+ device powered on and in range
- Android device with Bluetooth enabled
- Location permissions granted

**Test Procedure**:
1. Launch `GSRDeviceManagementActivity`
2. Tap "Scan for Devices" button
3. Verify device appears in scan list with proper device name and MAC address
4. Select device from list
5. Verify connection establishment and "Connected" status indicator
6. Check that GSR CSV file creation begins

**Expected Results**:
- Device appears in scan list within 10 seconds
- Connection establishes within 5 seconds
- UI transitions to "Connected" state
- Real-time data streaming begins
- CSV file starts populating with timestamp and GSR values

**Validation Criteria**:
- No connection timeouts or errors
- Proper BLE service discovery
- Correct device pairing if required

#### 1.2 Data Acquisition Integrity
**Objective**: Validate GSR data quality and real-time acquisition

**Test Procedure**:
1. Establish connection to Shimmer GSR device
2. Start recording session for 5 minutes
3. Apply stimuli (touch GSR leads, vary pressure) during recording
4. Stop recording and examine CSV file

**Expected Results**:
- Timestamps monotonically increasing
- GSR values respond to applied stimuli
- No data gaps or corrupt entries
- Sampling rate consistency (128 Hz target)

**Validation Criteria**:
- Timestamp alignment within ±100ms of system clock
- GSR value changes correlate with applied stimuli
- Data continuity without missing samples

#### 1.3 Reconnection and Robustness
**Objective**: Test connection stability and automatic recovery

**Test Procedure**:
1. Start recording session with Shimmer connected
2. Turn off Shimmer device during recording
3. Verify app detects disconnection
4. Power on Shimmer device after 30 seconds
5. Verify automatic reconnection attempt
6. Check data continuity after reconnection

**Expected Results**:
- App detects disconnection within 10 seconds
- Automatic reconnection attempts up to configured retries
- Data logging resumes after successful reconnection
- App remains stable during connection loss

### 2. RGB Camera Tests

#### 2.1 Camera Permission and Initialization
**Objective**: Validate camera permission flow and initialization

**Test Procedure**:
1. Fresh app install or clear app data
2. Launch camera recording activity
3. Deny camera permission when prompted
4. Attempt to start recording
5. Grant permission and retry recording

**Expected Results**:
- Permission request appears on first camera access
- Recording fails gracefully when permission denied
- Clear error message displayed to user
- Recording succeeds after permission granted

#### 2.2 4K Recording Performance
**Objective**: Validate 4K video recording on Samsung Galaxy S22

**Test Procedure**:
1. Set camera to 4K recording mode (3840x2160)
2. Start recording session
3. Record for 2 minutes continuously
4. Monitor device temperature and performance
5. Stop recording and verify file integrity

**Expected Results**:
- 4K resolution achieved (3840x2160)
- Stable 30 FPS recording
- No thermal throttling within 2 minutes
- Video file playable and complete

**Performance Metrics**:
- Frame rate: 30 FPS ±2%
- Resolution: 3840x2160 confirmed
- File size: ~60MB per minute (H.264 encoding)
- Temperature increase: <10°C above ambient

#### 2.3 Frame Capture Validation
**Objective**: Verify parallel video and frame capture system

**Test Procedure**:
1. Start recording with frame capture enabled
2. Record 10-second session
3. Check `frames/` directory for JPEG images
4. Verify `rgb_frames.csv` file generation
5. Validate frame count and timestamps

**Expected Results**:
- ~300 JPEG frames for 10-second recording at 30fps
- Frames are sequential and uncorrupted
- CSV timestamps align with video timeline
- Frame numbers are consecutive

### 3. Thermal Camera Tests

#### 3.1 Hardware Connection (Topdon TC001)
**Objective**: Validate USB thermal camera connection and permissions

**Test Procedure**:
1. Connect Topdon TC001 via USB-C to Samsung Galaxy S22
2. Launch thermal recording activity
3. Grant USB device permission when prompted
4. Verify thermal camera initialization
5. Check for "Thermal camera connected" indicator

**Expected Results**:
- USB device permission dialog appears
- Thermal camera LED indicates power/connection
- App recognizes device and initializes successfully
- Real thermal data streaming begins

#### 3.2 Real-Time Thermal Capture
**Objective**: Validate actual thermal data capture vs simulation

**Test Procedure**:
1. Start thermal recording with Topdon TC001 connected
2. Point camera at distinct heat sources (hot mug, hand, cool surface)
3. Record for 2 minutes
4. Check `thermal_images/` directory for PNG files
5. Verify `thermal_data.csv` contains temperature data

**Expected Results**:
- PNG files created at ~10 FPS
- Temperature values change based on scene
- Real thermal patterns visible in saved images
- CSV data shows temperature variations

#### 3.3 Hot-Plugging Scenarios
**Objective**: Test USB connection stability during operation

**Test Procedure**:
1. Start recording with thermal camera connected
2. Unplug camera during recording
3. Verify app detects disconnection
4. Plug camera back in after 10 seconds
5. Check if app resumes thermal capture

**Expected Results**:
- App detects disconnection gracefully
- No app crash or freeze
- Automatic fallback to simulation mode
- Optional: Resume real capture on reconnection

### 4. Network Communication Tests

#### 4.1 PC-Android Connection Establishment
**Objective**: Validate network communication between PC controller and Android app

**Test Procedure**:
1. Start PC controller application
2. Launch Android app on same Wi-Fi network
3. Initiate connection from PC to Android
4. Verify successful socket connection
5. Check for "Client connected" status

**Expected Results**:
- PC discovers Android device (mDNS or manual IP)
- Socket connection establishes within 5 seconds
- Both devices show "Connected" status
- Ready for live data streaming

#### 4.2 Live Data Streaming
**Objective**: Test real-time data transmission from Android to PC

**Test Procedure**:
1. Establish PC-Android connection
2. Start multi-modal recording (GSR + Camera + Thermal)
3. Monitor PC controller for incoming data streams
4. Apply stimuli and verify live updates on PC
5. Measure end-to-end latency

**Expected Results**:
- Live GSR data updates on PC plots
- Video frame preview on PC (reduced frame rate)
- Thermal data streaming
- End-to-end latency: 200-500ms
- Data values match between Android logs and PC display

### 5. Multi-Modal Integration Tests

#### 5.1 Full Session Test
**Objective**: Validate all modalities working simultaneously

**Test Procedure**:
1. Connect Shimmer GSR device
2. Connect Topdon thermal camera
3. Establish PC controller connection
4. Start recording all modalities simultaneously
5. Record for 5 minutes with various stimuli
6. Stop recording and verify all data streams

**Expected Results**:
- All three data streams operate without interference
- No performance degradation or crashes
- Complete data files for all modalities
- Timestamps synchronized across all sensors (±100ms)

#### 5.2 Synchronization Verification
**Objective**: Validate timestamp synchronization across sensors

**Test Procedure**:
1. Start full multi-modal recording
2. Create synchronization event (hand clap in view of camera)
3. Note exact timestamp of event
4. Stop recording and analyze data files
5. Verify event timing across all modalities

**Expected Results**:
- Video shows clap at timestamp T
- GSR data continuous at timestamp T (no artifact expected)
- Thermal data shows hand movement at timestamp T
- All timestamps within ±100ms of each other

### 6. Performance and Stress Tests

#### 6.1 Long Duration Recording
**Objective**: Test system stability during extended operation

**Test Procedure**:
1. Start full multi-modal recording
2. Record continuously for 30 minutes
3. Monitor device temperature, memory usage, battery
4. Apply periodic stimuli throughout session
5. Verify data integrity after completion

**Expected Results**:
- No crashes or memory leaks
- Continuous data logging without gaps
- Device temperature remains within safe limits
- All files readable and complete

#### 6.2 Crash Recovery Test
**Objective**: Validate system recovery from unexpected failures

**Test Procedure**:
1. Start multi-modal recording session
2. Force-stop app using `adb shell am force-stop`
3. Restart app
4. Verify crash detection and recovery
5. Start new recording session

**Expected Results**:
- App detects incomplete session on restart
- Proper cleanup of device connections
- New session starts successfully
- No lingering sensor connections

## Testing Infrastructure

### Automated Test Execution
- **Unit Tests**: Critical component validation
- **Integration Tests**: Cross-sensor coordination
- **Performance Tests**: Resource usage monitoring
- **Hardware Tests**: Real device interaction

### Test Data Collection
- **Quantitative Metrics**: Performance measurements, timing data
- **Qualitative Observations**: User experience, error handling
- **Evidence Artifacts**: Screenshots, log files, data samples

### Hardware Requirements
- **Android Device**: Samsung Galaxy S22 (primary target)
- **GSR Sensor**: Shimmer3 GSR+ with proper electrode setup
- **Thermal Camera**: Topdon TC001 with USB-C connection
- **PC Controller**: Windows/Linux system with Python 3.8+
- **Network**: Wi-Fi network for PC-Android communication

## Success Criteria

### Functional Requirements
- [ ] All sensors discoverable and connectable
- [ ] Real-time data acquisition from all modalities
- [ ] Multi-modal coordination without interference
- [ ] Network communication with PC controller
- [ ] Proper error handling and recovery

### Performance Requirements
- [ ] GSR sampling at 128 Hz
- [ ] Camera recording at 4K/30fps
- [ ] Thermal capture at 10 FPS
- [ ] Network latency <500ms
- [ ] Cross-sensor synchronization ±100ms

### Quality Requirements
- [ ] No crashes during normal operation
- [ ] Graceful handling of connection failures
- [ ] Complete data file generation
- [ ] Proper resource cleanup
- [ ] User-friendly error messages

## Evidence Collection for Thesis

This testing framework generates comprehensive evidence for thesis Chapter 5:
- **Quantitative Data**: Performance metrics, timing measurements, error rates
- **Validation Results**: Functional requirement compliance
- **Integration Proof**: Multi-modal coordination success
- **Hardware Compatibility**: Real device operation confirmation
- **Production Readiness**: System reliability and robustness

All test results will be systematically documented and analyzed to provide thorough evaluation of the IRCamera Multi-Modal Physiological Sensing Platform.