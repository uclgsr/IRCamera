# TC001 Thermal Camera Integration Testing Guide

## Overview

This document provides comprehensive testing coverage for the TC001 Topdon thermal camera integration, addressing the
four key areas requested:

1. **USB permission flow with real TC001 hardware**
2. **Verify actual 10Hz frame capture rate**
3. **Test camera disconnect/reconnect scenarios**
4. **Validate that other sensors continue when thermal fails**

## Testing Architecture

### Unit Tests (JVM)

**Location**: `app/src/test/java/mpdc4gsr/sensors/thermal/TC001IntegrationTest.kt`

Mock-based tests that verify logic without requiring actual hardware:

- USB permission flow logic
- Frame rate timing calculations
- Error handling and recovery mechanisms
- Multi-sensor independence validation

### Instrumentation Tests (Android Device)

**Location**: `app/src/androidTest/java/mpdc4gsr/sensors/thermal/TC001HardwareIntegrationTest.kt`

Real device tests that validate hardware integration:

- Actual TC001 device detection and permission flow
- Real-world frame rate measurement
- Physical disconnect/reconnect scenarios
- File system outputs validation

### Test Automation Script

**Location**: `scripts/test_tc001_integration.sh`

Automated test runner that:

- Checks prerequisites (ADB, device connection)
- Detects TC001 hardware presence
- Runs appropriate test suites
- Collects results and logs
- Provides manual testing guidance

## Test Coverage Details

### 1. USB Permission Flow Testing

#### Unit Tests:

```kotlin
@Test
fun `test TC001 USB permission flow - device detected`()
```

- Verifies permission request logic
- Tests permission granted scenarios
- Validates fallback to simulation mode

#### Instrumentation Tests:

```kotlin
@Test
fun testRealTC001USBPermissionFlow()
```

- Detects actual TC001 device (VID: 0x2744, PID: 0x0001)
- Validates real USB permission status
- Requires manual permission grant in system dialog

#### Manual Validation:

1. Connect TC001 to Android device
2. Launch IRCamera app
3. Verify USB permission dialog appears
4. Grant permission and verify thermal camera initializes

### 2. Frame Rate Verification Testing

#### Unit Tests:

```kotlin
@Test
fun `test TC001 frame capture rate validation - 10Hz target`()
```

- Validates frame timing intervals (100ms target)
- Tests consecutive error counting
- Verifies frame rate calculations

#### Instrumentation Tests:

```kotlin
@Test
fun testRealTC001FrameCaptureRate()
```

- Measures actual frame rate over 3-second period
- Validates 8-12Hz range (accounts for real hardware variations)
- Analyzes frame timing consistency

#### Manual Validation:

```bash
# Monitor frame rate in real-time
adb logcat -s ThermalCameraRecorder:I | grep "frame rate"
```

### 3. Disconnect/Reconnect Scenario Testing

#### Unit Tests:

```kotlin
@Test
fun `test TC001 disconnect during recording - graceful handling`()
@Test
fun `test TC001 multiple disconnect-reconnect cycles`()
```

- Simulates USB device removal/addition
- Validates recording continuity
- Tests automatic recovery mechanisms

#### Instrumentation Tests:

```kotlin
@Test
fun testRealTC001DisconnectReconnectScenarios()
```

- Guides manual disconnect/reconnect testing
- Monitors recording continuity during hardware changes
- Validates graceful error handling

#### Manual Validation:

1. Start multi-sensor recording
2. Unplug TC001 during recording
3. Verify recording continues (simulation mode)
4. Reconnect TC001
5. Verify recording continues without crashes

### 4. Sensor Independence Testing

#### Unit Tests:

```kotlin
@Test
fun `test other sensors continue when TC001 fails - GSR and RGB unaffected`()
@Test
fun `test sensor failure recovery - thermal does not crash app`()
```

- Simulates thermal failures during multi-sensor recording
- Validates other sensors remain unaffected
- Tests error isolation mechanisms

#### Instrumentation Tests:

```kotlin
@Test
fun testOtherSensorsContinueWhenThermalFails()
```

- Simulates thermal failure scenarios
- Validates multi-sensor recording continuity
- Tests app stability during thermal errors

#### Manual Validation:

1. Start recording with all sensors (GSR + Thermal + RGB)
2. Force thermal camera error (unplug/power off TC001)
3. Verify other sensors continue recording
4. Check individual sensor data files for completeness
5. Verify no app crashes occur

## Running the Tests

### Automated Test Execution

```bash
# Run the comprehensive test suite
./scripts/test_tc001_integration.sh

# Options available:
# 1) Unit tests only
# 2) Instrumentation tests only  
# 3) All automated tests
# 4) Manual testing guidance
# 5) Full test suite with results collection
```

### Individual Test Execution

#### Unit Tests:

```bash
./gradlew :app:testDebugUnitTest --tests="mpdc4gsr.sensors.thermal.TC001IntegrationTest"
```

#### Instrumentation Tests:

```bash
./gradlew :app:connectedDebugAndroidTest --tests="mpdc4gsr.sensors.thermal.TC001HardwareIntegrationTest"
```

## Expected Test Outcomes

### ✅ Success Criteria

1. **No App Crashes**: All tests complete without application crashes
2. **Graceful Fallbacks**: Thermal errors switch to simulation mode automatically
3. **Multi-sensor Protection**: GSR and RGB recording unaffected by thermal issues
4. **Frame Rate Achievement**: 10Hz (±20%) frame rate with real TC001 hardware
5. **File Generation**: PNG images saved to `thermal_images/` directory
6. **Data Logging**: CSV files contain proper temperature telemetry
7. **USB Hot-plug**: Automatic device detection and permission handling

### 📊 Performance Benchmarks

- **Frame Rate**: 8-12 Hz with real hardware
- **Frame Consistency**: <50ms standard deviation in frame intervals
- **Error Tolerance**: <10% frame processing error rate
- **Recovery Time**: <2 seconds from error to simulation mode switch
- **File Output**: PNG files with valid thermal data and timestamps

### 🛡️ Error Resilience

- **Consecutive Errors**: Max 10 before simulation mode switch
- **Permission Denial**: Automatic fallback to simulation mode
- **Device Disconnection**: Graceful recording continuation
- **SDK Failures**: Isolated error handling without affecting other sensors

## Hardware Requirements

### Minimum Requirements:

- Android device with USB OTG support
- Android 7.0 (API 24) or higher
- Available USB port for TC001 connection
- Developer options and USB debugging enabled

### Recommended Setup:

- TC001 Topdon thermal camera
- USB OTG adapter/cable
- Device with ample storage for test data
- ADB installed on development machine

## Test Data Analysis

### Generated Artifacts:

- **CSV Files**: Temperature data with timestamps
- **PNG Images**: Thermal frame captures (if hardware available)
- **Log Files**: Detailed operation logs for debugging
- **Test Reports**: JUnit XML reports with pass/fail status

### Analysis Tools:

```bash
# Monitor thermal operation in real-time
adb logcat -s ThermalCameraRecorder:* | grep -E "(frame|temperature|error)"

# Analyze frame rate performance  
grep "frame rate" tc001_test_results_*/thermal_logcat.txt

# Check for error patterns
grep -E "(error|exception|crash)" tc001_test_results_*/thermal_logcat.txt
```

## Troubleshooting

### Common Issues:

1. **TC001 Not Detected**:
    - Check USB OTG cable connection
    - Verify device VID/PID (0x2744/0x0001)
    - Grant USB permission when prompted

2. **Frame Rate Below Target**:
    - Check system performance (CPU/memory usage)
    - Verify USB connection stability
    - Monitor for thermal throttling

3. **Test Failures**:
    - Check device logs for specific errors
    - Verify test prerequisites are met
    - Ensure sufficient storage space for test data

### Debug Commands:

```bash
# List connected USB devices
adb shell lsusb

# Check thermal camera status
adb logcat -s ThermalCameraRecorder:D

# Monitor system resources
adb shell top | grep irCamera
```

## Integration with CI/CD

### Automated Testing:

- Unit tests run on every PR
- Instrumentation tests run on hardware test farm
- Performance regression detection
- Test result reporting and artifacts collection

### Quality Gates:

- All unit tests must pass
- Hardware tests must pass on at least one TC001-equipped device
- Frame rate performance within acceptable range
- No memory leaks or crashes detected

This comprehensive testing approach ensures the TC001 thermal camera integration is robust, reliable, and ready for
production use while maintaining system stability and multi-sensor compatibility.