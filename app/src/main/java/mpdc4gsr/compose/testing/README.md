# Testing Suite Compose Migration

This directory contains the Compose migration of the IRCamera Testing Suite, providing modern UI
implementations of all testing activities.

## Overview

The Testing Suite has been migrated from traditional XML-based layouts to Jetpack Compose, offering:

- **Modern UI**: Material 3 design with consistent theming
- **Reactive State Management**: Real-time test status updates
- **Enhanced UX**: Better visualization of test progress and results
- **Reusable Components**: Shared testing UI components across all tests

## Architecture

### Shared Components (`TestingComponents.kt`)

- **TestResultCard**: Displays individual test cases with status and controls
- **TestStatusIcon**: Visual indicators for test states (pending, running, passed, failed)
- **TestProgressIndicator**: Overall test suite progress visualization
- **TestMetricsDisplay**: Performance metrics and test data visualization

### Test Activities

#### BLE Integration Tests

- **BLEIntegrationTestComposeActivity**: BLE connectivity and data streaming tests
- Tests: Permissions, Discovery, Connection, Streaming, Reconnection

#### GSR Sensor Tests

- **GSRBenchTestComposeActivity**: GSR performance benchmarking
- Tests: Connection, Calibration, Data Quality, Performance, Stress Testing

#### Camera System Tests

- **RgbCameraTestComposeActivity**: Camera functionality and controls
- Tests: Permissions, Capabilities, 4K Recording, Tap-to-Focus, Manual Controls, RAW Capture

#### Synchronization Tests

- **CrossModalSyncTestComposeActivity**: Multi-sensor synchronization
- Tests: GSR-Thermal Sync, GSR-RGB Sync, Thermal-RGB Sync, Triple Sync, Timestamp Accuracy

#### Session Management Tests

- **SessionLifecycleTestComposeActivity**: Recording session lifecycle
- Tests: Creation, Multi-sensor Start, Pause/Resume, Graceful Stop, Error Recovery, Data Integrity

#### Performance Tests

- **ParallelRecordingTestComposeActivity**: Multi-sensor parallel recording
- Tests: Sensor Initialization, Synchronized Start/Stop, Data Collection, Buffer Management, Error
  Handling

### Hub Activity

**TestingSuiteHubActivity** provides:

- Centralized access to all testing modules
- Category-based filtering (BLE, GSR, Camera, Synchronization, Performance)
- Search functionality
- Both Compose and Legacy activity launching
- Priority-based organization

## Usage

### Launching the Testing Suite

```kotlin
// From any activity
val intent = Intent(this, TestingSuiteHubActivity::class.java)
startActivity(intent)
```

### Integration with Main Launcher

The Testing Suite Hub is integrated into the main `ComposeMigrationLauncherActivity`:

```kotlin
LauncherCard(
    title = "Testing Suite Hub",
    subtitle = "Comprehensive testing dashboard with 14+ test activities",
    icon = Icons.Default.BugReport,
    onClick = { 
        startActivity(Intent(this, TestingSuiteHubActivity::class.java))
    }
)
```

## Test Categories

### BLE Integration

- BLE connectivity testing
- Device discovery and pairing
- Data streaming validation
- Reconnection handling

### GSR Sensors

- Performance benchmarking
- Data quality validation
- Calibration testing
- Stress testing

### Camera Systems

- RGB camera functionality
- 4K recording capabilities
- Manual controls testing
- RAW capture validation

### Synchronization

- Multi-sensor sync testing
- Timestamp accuracy validation
- Cross-modal coordination
- Drift detection and correction

### Performance

- Session lifecycle management
- Parallel recording coordination
- Buffer management
- Error recovery

## Features

### Real-time Updates

- Live test progress indicators
- Real-time sensor status monitoring
- Dynamic metrics visualization
- Event logging and tracking

### Enhanced UX

- Material 3 design language
- Consistent theming with LibUnifiedTheme
- Intuitive test controls
- Clear result visualization

### Comprehensive Testing

- Individual test execution
- Full test suite automation
- Performance metrics collection
- Error tracking and reporting

## Integration with Legacy

Each Compose test activity maintains compatibility with its legacy counterpart:

- **Compose Version**: Modern UI with enhanced features
- **Legacy Version**: Original XML-based implementation
- **Hub Access**: Both versions accessible from TestingSuiteHubActivity

## Development Notes

### Adding New Tests

1. Create new test activity extending `ComponentActivity`
2. Use shared `TestingComponents` for consistency
3. Implement proper state management with `remember` and `mutableStateOf`
4. Add to `TestingSuiteHubActivity` module list
5. Categorize appropriately for filtering

### Best Practices

- Use `lifecycleScope.launch` for async operations
- Implement proper error handling with try-catch
- Log test progress with appropriate log levels
- Update UI state reactively
- Follow Material 3 design patterns

## Testing Philosophy

All tests follow the MVP approach:

- **Real functionality**: No stub implementations
- **Actual hardware integration**: Tests real sensors and devices
- **Evidence collection**: Quantitative metrics for validation
- **Comprehensive coverage**: All critical paths tested
- **Error resilience**: Robust error handling and recovery

## Performance Considerations

- Efficient state management with Compose state APIs
- Minimal recomposition through proper state scoping
- Coroutine-based async operations
- Memory-efficient test data handling
- Battery-optimized test execution