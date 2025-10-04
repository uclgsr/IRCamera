# Kotlin Instrumentation Tests

This directory contains all Android instrumentation tests for the IRCamera application, organized in a single centralized location.

## Test Files

### Network Integration Tests
- **NetworkProtocolIntegrationTest.kt** - Integration tests for PC-Android protocol communication including command handling and time synchronization

### Camera Tests
- **RgbCamera4KRecordingInstrumentationTest.kt** - Instrumentation tests for RGB camera including 4K video recording and burst still capture

### Hardware Integration Tests
- **ThermalCameraTC001HardwareTest.kt** - Hardware integration tests for Topdon TC001 thermal camera on actual Android devices

## Running Tests

Run all instrumentation tests:
```bash
./gradlew :app:connectedDebugAndroidTest
```

Run specific test:
```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=mpdc4gsr.tests.NetworkProtocolIntegrationTest
```

Run tests on specific device:
```bash
adb devices  # List connected devices
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=mpdc4gsr.tests.ThermalCameraTC001HardwareTest
```

## Note

Most instrumentation tests require actual hardware (USB cameras, Bluetooth sensors) and are currently disabled with `@Ignore` annotation. Enable them when running on devices with the required hardware.
