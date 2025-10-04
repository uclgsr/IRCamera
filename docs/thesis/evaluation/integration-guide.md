# Integration Guide for Thesis Evaluation Tests

This guide explains how to integrate the robustness tests into the main IRCamera application.

## Overview

The thesis evaluation tests are currently standalone Kotlin files in the `docs/thesis-evaluation/robustness_tests/`
directory. They follow the same patterns as existing test activities in the app but are not yet compiled or integrated
into the app's build system.

## Integration Options

### Option 1: Manual Integration (Recommended for Thesis Work)

Copy the test files to the main app's test activity directory:

```bash
# Copy files to the main test activity location
cp docs/thesis-evaluation/robustness_tests/*.kt app/src/main/java/mpdc4gsr/feature/testing/ui/

# Update package declaration in each file
# Change: package thesis_evaluation.robustness_tests
# To: package mpdc4gsr.feature.testing.ui
```

After copying, update the package declaration at the top of each file from:

```kotlin
package thesis_evaluation.robustness_tests
```

to:

```kotlin
package mpdc4gsr.feature.testing.ui
```

### Option 2: Add to Testing Suite Hub

Integrate the tests into the existing TestingSuiteHubActivity:

1. Copy files as in Option 1
2. Open `app/src/main/java/mpdc4gsr/feature/testing/ui/TestingSuiteHubActivity.kt`
3. Add test entries to the `testingModules` list:

```kotlin
// In TestingSuiteHubActivity.kt, add to the testingModules list:

// Robustness Tests
TestingModule(
    id = "gsr_reconnection_simulated",
    title = "GSR Reconnection (Simulated)",
    description = "Test GSR disconnection and auto-reconnection (simulated)",
    icon = Icons.Default.Bluetooth,
    composeActivity = GSRReconnectionSimulatedTest::class.java,
    category = TestCategory.BLE_INTEGRATION,
    priority = TestPriority.HIGH
),
TestingModule(
    id = "gsr_reconnection_real",
    title = "GSR Reconnection (Real Hardware)",
    description = "Test GSR disconnection with real hardware",
    icon = Icons.Default.BluetoothDisabled,
    composeActivity = GSRReconnectionRealHardwareTest::class.java,
    category = TestCategory.BLE_INTEGRATION,
    priority = TestPriority.HIGH
),
TestingModule(
    id = "thermal_disconnect",
    title = "Thermal Camera Disconnect",
    description = "Test thermal camera USB disconnection handling",
    icon = Icons.Default.UsbOff,
    composeActivity = ThermalCameraDisconnectionTest::class.java,
    category = TestCategory.CAMERA_SYSTEMS,
    priority = TestPriority.HIGH
),
TestingModule(
    id = "network_drop",
    title = "Network Connection Drop",
    description = "Test system behavior when network connection is lost",
    icon = Icons.Default.SignalWifiOff,
    composeActivity = NetworkConnectionDropTest::class.java,
    category = TestCategory.NETWORK,
    priority = TestPriority.HIGH
),
TestingModule(
    id = "sensor_isolation",
    title = "Sensor Failure Isolation",
    description = "Test that failure in one sensor doesn't affect others",
    icon = Icons.Default.ErrorOutline,
    composeActivity = SensorFailureIsolationTest::class.java,
    category = TestCategory.SYSTEM,
    priority = TestPriority.HIGH
),
```

### Option 3: Keep Separate (Current State)

The tests can remain in the `docs/thesis-evaluation/` directory as reference implementations and documentation. This is
useful for:

- Including in thesis appendices
- Reference for understanding robustness testing approach
- Future development of similar tests
- Documentation of test methodology

## Running Tests Without Integration

The test files can be used as:

1. **Code Templates**: Copy and modify for specific testing needs
2. **Documentation**: Understand how robustness tests should be structured
3. **Reference**: See what events to log and metrics to capture
4. **Specifications**: Define expected behavior for each test scenario

## Test Dependencies

All tests depend on existing app components:

- `mpdc4gsr.core.utils.AppLogger` - For logging
- `mpdc4gsr.feature.network.data.RecordingController` - For recording control
- `mpdc4gsr.feature.gsr.data.GSRSensorRecorder` - For GSR tests
- `mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder` - For thermal tests
- `mpdc4gsr.core.data.UnifiedNetworkController` - For network tests
- `com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme` - For UI theme

These are already present in the app, so no additional dependencies are needed.

## Test Output Locations

When integrated and run, tests will output log files to:

```
/storage/emulated/0/Android/data/com.csl.irCamera/files/docs/thesis/evaluation/
```

File naming:

- `gsr_reconnection_simulated_<timestamp>.log`
- `gsr_reconnection_real_<timestamp>.log`
- `thermal_disconnect_<timestamp>.log`
- `network_drop_<timestamp>.log`
- `sensor_isolation_<timestamp>.log`

## Adding Tests to AndroidManifest.xml

If you integrate the tests as activities, add them to the manifest:

```xml
<!-- In app/src/main/AndroidManifest.xml -->
<activity
    android:name=".feature.testing.ui.GSRReconnectionSimulatedTest"
    android:exported="false"
    android:theme="@style/Theme.AppCompat.DayNight" />
<activity
    android:name=".feature.testing.ui.GSRReconnectionRealHardwareTest"
    android:exported="false"
    android:theme="@style/Theme.AppCompat.DayNight" />
<activity
    android:name=".feature.testing.ui.ThermalCameraDisconnectionTest"
    android:exported="false"
    android:theme="@style/Theme.AppCompat.DayNight" />
<activity
    android:name=".feature.testing.ui.NetworkConnectionDropTest"
    android:exported="false"
    android:theme="@style/Theme.AppCompat.DayNight" />
<activity
    android:name=".feature.testing.ui.SensorFailureIsolationTest"
    android:exported="false"
    android:theme="@style/Theme.AppCompat.DayNight" />
```

## Verification After Integration

After integration, verify the tests:

1. **Build the app:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Check for compilation errors:**
   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

3. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

4. **Run tests through Testing Suite Hub:**
    - Open IRCamera app
    - Navigate to Testing Suite Hub
    - Select robustness test category
    - Run individual tests

## Test Execution Flow

Each test follows this general pattern:

1. **Initialization**: Set up test components (recorders, controllers)
2. **Start Recording**: Begin sensor data collection
3. **Induce Failure**: Trigger the specific failure condition
4. **Monitor Response**: Track system behavior and logging
5. **Measure Recovery**: Time reconnection attempts and success
6. **Complete Test**: Stop recording and generate report
7. **Output Results**: Save log file with metrics

## Thesis Integration

These tests directly support thesis requirements:

### Chapter 5 (Implementation)

- Demonstrate system behavior under failure conditions
- Show logging and error handling implementation
- Prove graceful degradation capabilities

### Chapter 6 (Evaluation)

- Provide quantitative metrics (timings, sample counts)
- Evidence of robustness and fault tolerance
- Data for assessing system resilience
- Identify areas for improvement

## Best Practices

When using these tests:

1. **Start with Simulated Tests**: These are consistent and repeatable
2. **Progress to Real Hardware Tests**: More realistic but variable results
3. **Run Multiple Iterations**: Average results for statistical significance
4. **Document All Results**: Keep all log files for thesis evidence
5. **Compare Baseline**: Measure against system without failures

## Troubleshooting

Common issues and solutions:

**Issue**: Test files don't compile after copying
**Solution**: Ensure package declaration matches destination directory

**Issue**: Test crashes on start
**Solution**: Check that all required components are initialized

**Issue**: Log files not created
**Solution**: Verify app has WRITE_EXTERNAL_STORAGE permission

**Issue**: Real hardware tests don't detect disconnection
**Solution**: Ensure proper Bluetooth/USB permissions and device pairing

## Future Enhancements

Potential improvements:

1. **Automated Test Execution**: Script to run all tests in sequence
2. **Report Generation**: Parse log files and generate summary reports
3. **Statistical Analysis**: Analyze multiple test runs for reliability metrics
4. **Continuous Testing**: Integrate into CI/CD pipeline
5. **Stress Testing**: Extended duration tests for long-term stability

## Support

For questions or issues:

- Review existing test implementations in `app/src/main/java/mpdc4gsr/feature/testing/ui/`
- Check Android logcat for runtime errors
- Refer to README.md in docs/thesis-evaluation directory
- Consult thesis requirements document
