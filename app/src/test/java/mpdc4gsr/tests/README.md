# Kotlin Unit Tests

This directory contains all unit tests for the IRCamera application, organized in a single centralized location.

## Test Files

### Core Functionality Tests
- **LoggingFunctionalityTest.kt** - Tests for AppLogger functionality including log levels and structured logging
- **ErrorHandlingTest.kt** - Tests for ErrorHandler utility including safe execution and error handling
- **DeviceEventManagementTest.kt** - Tests for DeviceEventManager including device and socket connection state management

### Feature Tests
- **NetworkProtocolTest.kt** - Unit tests for network protocol message creation and parsing
- **GSRSensorReconnectionTest.kt** - Tests for GSR sensor reconnection logic and state management
- **RecordingSettingsTest.kt** - Tests for recording settings repository and preferences

### Sensor Tests
- **GSRBluetoothDiscoveryTest.kt** - Tests for GSR device Bluetooth discovery and connection validation
- **RgbCameraPerformanceTest.kt** - Tests for RGB camera performance including 4K recording capabilities
- **ThermalCameraUsbIntegrationTest.kt** - Tests for thermal camera USB integration

### Integration Tests
- **MultiSensorCoordinationTest.kt** - Integration tests for multi-sensor coordination (GSR + Camera + Thermal)

### UI Tests
- **FragmentToComposeMigrationTest.kt** - Tests for Fragment to Compose migration including UI components and state management

### Test Helpers
- **ComposeTestStubs.kt** - Test stubs and mock ViewModels for Compose tests

## Running Tests

Run all unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

Run specific test:
```bash
./gradlew :app:testDebugUnitTest --tests "mpdc4gsr.tests.LoggingFunctionalityTest"
```

Run tests with coverage:
```bash
./gradlew :app:testDebugUnitTestCoverage
```
