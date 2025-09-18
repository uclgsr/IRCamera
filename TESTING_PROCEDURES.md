# Testing Procedures for All Modalities and Features

This document provides comprehensive testing procedures for the IRCamera Multi-Modal Physiological Sensing Platform to ensure system reliability and generate evidence for Chapter 5 evaluation.

## Overview

The testing strategy includes:
- **Unit tests** for specific components (where feasible)
- **Integration tests** that simulate real usage scenarios
- **System tests** for end-to-end validation
- **Robustness tests** for error handling and recovery

## Test Execution Environment

### Prerequisites
- Samsung Galaxy S22 (or compatible device) with Android API level 30+
- Shimmer3 GSR+ device for BLE testing
- Topdon TC001 thermal camera for USB integration testing
- PC with Wi-Fi connectivity for network testing
- All required permissions granted

### Build and Run Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run comprehensive system demo
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.topdon.tc001/.demo.ComprehensiveSystemDemo

# Test PC controller components
cd pc-controller && python3 demo_mvp_components.py
```

## 1. Shimmer GSR Tests

### 1.1 Device Discovery and Connection
**Test ID:** GSR_001  
**Objective:** Verify Shimmer GSR device discovery and connection capabilities

**Procedure:**
1. Power on Shimmer GSR device and ensure it's in pairing mode
2. Launch app and navigate to GSR configuration (`ShimmerConfigActivity`)
3. Tap "Scan for Devices" button
4. Wait for device discovery (up to 30 seconds)

**Expected Results:**
- Device appears in scan list with signal strength indicator
- Device can be selected for connection
- Connection establishes within 10 seconds
- UI transitions to "connected" state with green indicator
- GSR CSV file creation begins with real-time data population

**Validation Script:** `app/src/test/java/com/topdon/tc001/sensors/gsr/GSRDiscoveryTest.kt`

### 1.2 Data Acquisition Integrity
**Test ID:** GSR_002  
**Objective:** Validate GSR data quality and integrity

**Procedure:**
1. Establish connection with Shimmer device (from GSR_001)
2. Start recording session for 2 minutes
3. Apply stimulus (touch GSR leads, vary pressure)
4. Stop recording and examine output files

**Expected Results:**
- Monotonically increasing timestamps aligned with system clock (±1 second)
- GSR values respond to applied stimuli
- No corrupt entries or obvious outliers
- Data pipeline validates from sensor to CSV file

**Validation Script:** `app/src/test/java/com/topdon/tc001/sensors/gsr/GSRDataIntegrityTest.kt`

### 1.3 Reconnection and Robustness
**Test ID:** GSR_003  
**Objective:** Test connection robustness and recovery

**Procedure:**
1. Start recording with connected Shimmer device
2. Simulate connection drop (power off device or move out of range)
3. Wait 30 seconds, then restore connection
4. Verify automatic reconnection behavior

**Expected Results:**
- App detects disconnection and logs appropriately
- Automatic reconnection attempts (up to configured retries)
- Recording continues with timestamp gap if reconnection succeeds
- App remains stable without crashes
- Other sensors unaffected by GSR disconnection

**Validation Script:** `app/src/test/java/com/topdon/tc001/sensors/gsr/GSRRobustnessTest.kt`

### 1.4 Multiple Sessions & Cleanup
**Test ID:** GSR_004  
**Objective:** Verify session management and resource cleanup

**Procedure:**
1. Complete a recording session and stop
2. Start new recording session immediately
3. Verify device reconnection and data logging
4. Check Bluetooth connection management

**Expected Results:**
- Shimmer device available for new session without app restart
- New CSV file created for second session
- Proper Bluetooth connection cleanup between sessions
- No resource leaks detected

## 2. RGB Camera (Video/Frames) Tests

### 2.1 Camera Permission and Initialization
**Test ID:** RGB_001  
**Objective:** Validate camera permission flow and initialization

**Procedure:**
1. Install app on fresh device or clear app data
2. Launch app and attempt to start recording
3. Test permission denial and approval scenarios

**Expected Results:**
- App requests Camera permission on first run
- Permission denial shows appropriate error message
- Permission approval enables camera functionality
- Camera preview visible when permissions granted

**Validation Script:** `app/src/test/java/com/topdon/tc001/camera/RGBCameraRecorderCriticalIssuesTest.kt`

### 2.2 Preview and Recording Start
**Test ID:** RGB_002  
**Objective:** Verify camera recording functionality

**Procedure:**
1. Grant camera permissions
2. Start recording session
3. Verify preview and recording indicators
4. Check file creation in real-time

**Expected Results:**
- Camera preview displays correctly
- Recording starts without error
- Video file and JPEG frames created in `frames/` folder
- UI shows recording status with timer

### 2.3 Resolution and Performance
**Test ID:** RGB_003  
**Objective:** Validate recording quality and performance

**Procedure:**
1. Start recording session
2. Monitor performance for 2 minutes
3. Check video properties and frame rate
4. Monitor device temperature and CPU usage

**Expected Results:**
- 4K resolution (3840x2160) achieved if supported
- Target 30 FPS maintained without significant drops
- No UI freezes or crashes
- No thermal throttling in short sessions

**Validation Script:** `app/src/test/java/com/topdon/tc001/sensors/rgb/RGBPerformanceTest.kt`

### 2.4 Frame Capture Validation
**Test ID:** RGB_004  
**Objective:** Verify frame capture system accuracy

**Procedure:**
1. Record 10-second session
2. Examine output files after stopping
3. Validate frame count and timing

**Expected Results:**
- ~300 JPEG frames for 10-second recording at 30fps
- `rgb_frames.csv` contains correct frame count and timestamps
- Images are uncorrupted and sequential
- Timestamps increase consistently and align with video timeframe

### 2.5 Stop and Restart
**Test ID:** RGB_005  
**Objective:** Test session management and resource cleanup

**Procedure:**
1. Record 30-second session and stop
2. Immediately start another 30-second session
3. Monitor resource usage and camera availability

**Expected Results:**
- Second recording works as well as first
- New video file and frames created
- Camera resources properly released between sessions
- No memory leaks detected

## 3. Thermal Camera (IR) Tests

### 3.1 Hardware Connection
**Test ID:** THERMAL_001  
**Objective:** Validate USB thermal camera connection

**Procedure:**
1. Connect Topdon TC001 to phone via USB-C
2. Launch app and check device recognition
3. Grant USB permissions when prompted

**Expected Results:**
- Camera LED indicates power connection
- USB permission dialog appears
- App detects and initializes thermal camera
- "Thermal camera connected" indicator shown

### 3.2 Thermal Stream Start
**Test ID:** THERMAL_002  
**Objective:** Verify real thermal data capture

**Procedure:**
1. Connect thermal camera (from THERMAL_001)
2. Start recording session
3. Point camera at heat source (hand, hot water)
4. Verify real data capture vs simulation

**Expected Results:**
- Real thermal frames captured (not simulation)
- PNG files created in `thermal_images/` folder
- Images reflect actual heat patterns
- `thermal_data.csv` populated with temperature data

**Validation Script:** `app/src/test/java/com/topdon/tc001/sensors/thermal/ThermalCameraUSBPermissionTest.kt`

### 3.3 Data Logging
**Test ID:** THERMAL_003  
**Objective:** Validate thermal data logging accuracy

**Procedure:**
1. Record thermal data for 1 minute
2. Examine output files and data consistency
3. Verify temperature readings with known heat sources

**Expected Results:**
- CSV entries match image file count
- Timestamps are consistent and monotonic
- Temperature values change appropriately with scene
- Min/max temperature fields populated correctly

### 3.4 Hot Plugging Scenarios
**Test ID:** THERMAL_004  
**Objective:** Test USB connection robustness

**Procedure:**
1. Start recording with thermal camera
2. Unplug camera during recording
3. Wait 10 seconds and plug back in
4. Verify app behavior and recovery

**Expected Results:**
- App detects USB disconnection without crashing
- Switches to simulation mode or stops thermal capture
- Reattachment detection via BroadcastReceiver
- Recording continues with other modalities unaffected

### 3.5 No Hardware Scenario
**Test ID:** THERMAL_005  
**Objective:** Test graceful fallback behavior

**Procedure:**
1. Start recording session without thermal camera connected
2. Verify app behavior with missing hardware

**Expected Results:**
- App uses simulated thermal data with warning
- Other modalities continue normally
- No blocking dialogs or infinite waits
- Clear indication of simulation mode

### 3.6 Performance Check
**Test ID:** THERMAL_006  
**Objective:** Validate thermal capture performance

**Procedure:**
1. Record thermal data for 5 minutes at 10 FPS
2. Monitor frame rate consistency and app responsiveness
3. Check storage I/O performance

**Expected Results:**
- Consistent ~10 Hz capture rate maintained
- App remains responsive during capture
- Frame timestamps show minimal gaps
- Storage keeps up with data rate

## 4. Networking and Communication Tests

### 4.1 Connection Establishment
**Test ID:** NETWORK_001  
**Objective:** Validate PC-phone network connection

**Procedure:**
1. Launch PC controller application
2. Start mobile app on same Wi-Fi network
3. Start network server on phone
4. Connect PC to phone via auto-discovery or manual IP

**Expected Results:**
- PC discovers phone via mDNS (if implemented)
- TCP connection establishes successfully
- "Client connected" logged on phone
- "Connected to device" logged on PC

**Validation Script:** `app/src/test/java/com/topdon/tc001/network/NetworkControllerTest.kt`

### 4.2 Live Data Streaming
**Test ID:** NETWORK_002  
**Objective:** Verify real-time data transmission

**Procedure:**
1. Establish PC-phone connection (from NETWORK_001)
2. Start recording session with all sensors
3. Monitor PC application for incoming data
4. Create observable events (GSR stimulation, camera movement)

**Expected Results:**
- PC receives and displays data in near real-time
- Data values match phone's local recordings
- Observable events appear on PC within 500ms
- Plots/displays update continuously

### 4.3 Latency Measurement
**Test ID:** NETWORK_003  
**Objective:** Measure end-to-end latency

**Procedure:**
1. Set up PC-phone connection
2. Create sharp motion in front of camera
3. Measure delay until motion appears on PC
4. Record multiple measurements for consistency

**Expected Results:**
- Latency consistently under 500ms
- Typical range 200-300ms over Wi-Fi
- Measurements documented for evaluation

### 4.4 Disconnects and Reconnects
**Test ID:** NETWORK_004  
**Objective:** Test network robustness

**Procedure:**
1. Start streaming session
2. Temporarily disable Wi-Fi on phone
3. Re-enable Wi-Fi after 30 seconds
4. Test PC application crash scenarios

**Expected Results:**
- Phone continues local recording during network outage
- PC detects disconnection gracefully
- Reconnection possible after network restoration
- No data corruption in local files

### 4.5 Multiple Device/Session Handling
**Test ID:** NETWORK_005  
**Objective:** Test session management

**Procedure:**
1. Complete one PC-phone session
2. Start new session immediately
3. Verify proper session separation

**Expected Results:**
- PC properly finalizes first session
- New session starts with fresh data plots
- No mixing of session data
- Connection reuse or re-establishment works

## 5. Permission Handling Tests

### 5.1 Initial Launch Permissions
**Test ID:** PERMISSION_001  
**Objective:** Validate systematic permission requests

**Procedure:**
1. Install app on fresh device
2. Launch app and observe permission requests
3. Test denial scenarios for each permission
4. Grant permissions and verify functionality recovery

**Expected Results:**
- Systematic permission requests (Camera, Location, Bluetooth)
- Graceful handling of permission denials
- Clear explanations for required permissions
- Functionality recovers when permissions granted

**Validation Script:** `app/src/test/java/com/topdon/tc001/permissions/PermissionControllerTest.kt`

### 5.2 Runtime Toggle
**Test ID:** PERMISSION_002  
**Objective:** Test dynamic permission changes

**Procedure:**
1. Start recording with all permissions granted
2. Disable Bluetooth via system settings
3. Observe app behavior and recovery
4. Re-enable permission and test functionality

**Expected Results:**
- App detects permission changes
- Affected features fail gracefully with warnings
- Other features continue unaffected
- Functionality recovers when permission restored

### 5.3 USB Permission Persistence
**Test ID:** PERMISSION_003  
**Objective:** Test USB permission behavior

**Procedure:**
1. Connect thermal camera and grant USB permission
2. Disconnect and reconnect camera
3. Test permission persistence across app restarts

**Expected Results:**
- Permission persists within same session
- Fresh app launch may require new permission
- Permission behavior documented for usability

### 5.4 Background Operation
**Test ID:** PERMISSION_004  
**Objective:** Validate foreground service operation

**Procedure:**
1. Start recording session
2. Press home button to background app
3. Verify recording continues for 30 seconds
4. Return to app and check status

**Expected Results:**
- Recording continues in background
- "Recording in progress" notification visible
- Timer/counters advance while backgrounded
- Foreground service prevents termination

## 6. Multi-Modal Integration Tests (End-to-End)

### 6.1 Full Session Test
**Test ID:** INTEGRATION_001  
**Objective:** Complete multi-modal recording validation

**Procedure:**
1. Connect all devices (Shimmer, thermal camera, PC)
2. Grant all permissions
3. Start 2-minute recording with all modalities
4. Monitor system performance and stability
5. Verify all output files after completion

**Expected Results:**
- All three data streams operate concurrently
- No interference between modalities
- Device remains responsive throughout
- Complete data files for video, frames, thermal images, GSR CSV
- Timestamps overlap across all modalities

### 6.2 Synchronization Spot-Check
**Test ID:** INTEGRATION_002  
**Objective:** Verify cross-modality timing synchronization

**Procedure:**
1. Start multi-modal recording
2. Create synchronization event (clap hands at known time)
3. Examine data files for timing correlation
4. Calculate timestamp differences across modalities

**Expected Results:**
- Synchronization event visible in video frames
- GSR data continues smoothly at event time
- Cross-modality timestamps within ±100ms
- No large timing discrepancies detected

**Validation Script:** `app/src/test/java/com/topdon/tc001/test/SynchronizationTestActivity.kt`

### 6.3 Stress Test (Long Duration)
**Test ID:** INTEGRATION_003  
**Objective:** Test system stability under extended operation

**Procedure:**
1. Start recording with all sensors active
2. Record for 15-30 minutes (based on available time)
3. Monitor memory usage, temperature, battery
4. Verify complete data files after extended run

**Expected Results:**
- No crashes or memory leaks
- All files written completely and playable
- App responds to "Stop" command promptly
- Performance metrics documented

### 6.4 Crash Recovery Test
**Test ID:** INTEGRATION_004  
**Objective:** Validate crash recovery mechanisms

**Procedure:**
1. Start recording session
2. Force-stop app via `adb shell am force-stop`
3. Restart app and check recovery behavior
4. Verify session cleanup and device release

**Expected Results:**
- CrashRecoveryManager detects incomplete session
- Previous session marked as failed
- All device locks released
- New recording session possible without device reboot

**Validation Script:** `app/src/test/java/com/topdon/tc001/recovery/CrashRecoveryManagerTest.kt`

## 7. Automated Test Suite

### Unit Test Execution
```bash
# Run all unit tests
./gradlew test

# Run specific test suites
./gradlew test --tests "*GSR*"
./gradlew test --tests "*RGB*"  
./gradlew test --tests "*Thermal*"
./gradlew test --tests "*Network*"
```

### Integration Test Scripts
```bash
# Run comprehensive system tests
./gradlew connectedAndroidTest

# Run PC controller tests
cd pc-controller && python3 -m pytest test_mvp_*.py

# Network connectivity tests
cd pc-controller && python3 test_android_preview_client.py
```

### Performance Monitoring
- Use Android Profiler in Android Studio
- Monitor CPU, memory, and network usage
- Document thermal performance on Samsung Galaxy S22
- Record frame rates and timing accuracy

## 8. Test Results Documentation

### Data Collection
For each test, collect:
- Pass/fail status
- Performance measurements (latency, frame rates, etc.)
- Error logs and screenshots
- Resource usage statistics
- User experience observations

### Evidence for Chapter 5
- Test execution logs with timestamps
- Sample data files from multi-modal sessions
- Performance graphs and measurements
- Screenshots of UI behavior
- Network communication logs
- Synchronization accuracy measurements

### Known Issues Documentation
- Android build failures due to ShimmerDevice class (BleModule)
- PC controller dependency installation requirements
- Hardware compatibility limitations

## 9. Continuous Integration

### Automated Testing Pipeline
```yaml
# .github/workflows/test.yml
name: Test Suite
on: [push, pull_request]
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run Unit Tests
        run: ./gradlew test
      
  pc-controller-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.12'
      - name: Install Dependencies
        run: cd pc-controller && pip install -r requirements.txt
      - name: Run PC Tests
        run: cd pc-controller && python3 -m pytest
```

### Test Coverage Reports
- Generate coverage reports with JaCoCo
- Monitor test coverage trends
- Ensure critical paths are tested

## Conclusion

This comprehensive testing procedure ensures:
- Systematic validation of all modalities
- Robustness testing for real-world scenarios
- Performance benchmarking for evaluation
- Evidence generation for thesis Chapter 5
- Continuous quality assurance

Each test includes specific validation criteria and expected outcomes, enabling both manual execution for development and automated execution for continuous integration.