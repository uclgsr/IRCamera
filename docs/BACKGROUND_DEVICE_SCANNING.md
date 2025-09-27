# Background Device Scanning

This document describes the background device scanning feature implemented for continuous BLE device discovery.

## Overview

The background device scanning system provides continuous, automated discovery of BLE devices (particularly GSR sensors) without requiring user interaction. The system runs as a foreground service and intelligently manages scanning intervals to minimize battery impact.

## Architecture

### Core Components

1. **BackgroundDeviceScanningService** - Main foreground service that handles continuous scanning
2. **BackgroundScanningManager** - Lifecycle-aware manager for service control
3. **BackgroundScanHelper** - Simple utility functions for basic service control
4. **BleDeviceManager** - Enhanced with device count and status methods

### Key Features

- **Continuous Scanning**: Automatically scans for BLE devices at regular intervals
- **Battery Optimization**: Uses smart interval management (2-5 minutes between scans)
- **Foreground Service**: Runs persistently with user-visible notification
- **Intelligent Intervals**: Adjusts scan frequency based on device discovery results
- **Integration**: Works with existing BleDeviceManager and device filtering

## Implementation Details

### Service Configuration

- **Scan Duration**: 30 seconds per scan cycle
- **Normal Interval**: 2 minutes between scans when devices are found
- **Idle Interval**: 5 minutes between scans when no devices are found
- **Service Type**: `FOREGROUND_SERVICE_CONNECTED_DEVICE`

### Permissions Required

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## Usage Examples

### Simple Usage

```kotlin
// Start background scanning
BackgroundScanHelper.startBackgroundScanning(context)

// Stop background scanning
BackgroundScanHelper.stopBackgroundScanning(context)

// Pause/Resume
BackgroundScanHelper.pauseBackgroundScanning(context)
BackgroundScanHelper.resumeBackgroundScanning(context)
```

### Advanced Usage with Manager

```kotlin
val manager = BackgroundScanningManager(context)
lifecycle.addObserver(manager)

manager.setStatusCallback(object : BackgroundScanningManager.ServiceStatusCallback {
    override fun onServiceConnected(service: BackgroundDeviceScanningService) {
        // Service connected, can get status
        val status = service.getStatus()
    }
    
    override fun onServiceDisconnected() {
        // Service disconnected
    }
    
    override fun onServiceStatusChanged(status: BackgroundDeviceScanningService.ServiceStatus) {
        // Status updated
    }
})

manager.startBackgroundScanning()
```

### Integration with Existing Workflow

The background scanning integrates seamlessly with the existing BLE infrastructure:

1. Uses the same `BleDeviceManager` instance
2. Respects existing GSR device filtering logic
3. Maintains device information that can be accessed later
4. Provides continuous device discovery without interrupting manual scans

## User Interface Integration

### Menu Integration

Background scanning controls are integrated into existing activities:

- **ShimmerConfigActivity**: Menu options to start/stop background scanning
- **BackgroundScanningDemoActivity**: Dedicated test interface

### Notification

The service provides a persistent notification with:
- Current scanning status
- Scan count and last device count
- Pause/Resume and Stop actions

## Battery Optimization

The system implements several battery optimization strategies:

1. **Adaptive Intervals**: Longer intervals when no devices are found
2. **Partial Wake Lock**: Minimal power usage during scanning
3. **Short Scan Duration**: Only 30 seconds per scan cycle
4. **Pause Capability**: Can be paused during active app usage

## Testing

### Demo Activity

`BackgroundScanningDemoActivity` provides a comprehensive test interface:
- Start/Stop/Pause/Resume controls
- Real-time status updates
- Scan count and device count display

### Integration Tests

`BackgroundScanningIntegrationTest` provides examples for:
- App launch integration
- Battery-aware scanning
- Recording session workflow
- Active use management

## Configuration

### Scan Timing

```kotlin
private const val SCAN_DURATION_MS = 30000L      // 30 seconds
private const val SCAN_INTERVAL_MS = 120000L     // 2 minutes
private const val IDLE_SCAN_INTERVAL_MS = 300000L // 5 minutes
```

### Service Actions

```kotlin
const val ACTION_START_SCANNING = "mpdc4gsr.action.START_BACKGROUND_SCANNING"
const val ACTION_STOP_SCANNING = "mpdc4gsr.action.STOP_BACKGROUND_SCANNING"
const val ACTION_PAUSE_SCANNING = "mpdc4gsr.action.PAUSE_BACKGROUND_SCANNING"
const val ACTION_RESUME_SCANNING = "mpdc4gsr.action.RESUME_BACKGROUND_SCANNING"
```

## Best Practices

1. **Start Early**: Begin background scanning when app launches
2. **Pause During Active Use**: Reduce scanning when user is actively using BLE features
3. **Battery Awareness**: Consider battery level when enabling/disabling scanning
4. **User Control**: Provide clear user controls for starting/stopping scanning
5. **Status Feedback**: Show scanning status in the UI

## Troubleshooting

### Common Issues

1. **Service Not Starting**: Check permissions, especially `FOREGROUND_SERVICE_CONNECTED_DEVICE`
2. **No Devices Found**: Verify BLE is enabled and location permissions granted
3. **High Battery Usage**: Check scan intervals and consider pausing during active use
4. **Service Stops**: Android may kill background services; the service is configured with `START_STICKY` for restart

### Debugging

Enable debug logging:
```kotlin
private const val TAG = "BackgroundDeviceScanning"
Log.d(TAG, "Your debug message")
```

## Future Enhancements

Potential improvements to consider:

1. **Machine Learning**: Predict optimal scan times based on user patterns
2. **Geofencing**: Enable scanning only in specific locations
3. **Device-Specific Intervals**: Different intervals for different device types
4. **Network Integration**: Share discovered devices across network
5. **Advanced Battery Management**: Integration with Android's battery optimization APIs

## Files Modified/Created

- `BackgroundDeviceScanningService.kt` - Main service implementation
- `BackgroundScanningManager.kt` - Service lifecycle manager
- `BackgroundScanHelper.kt` - Simple utility functions
- `BackgroundScanningDemoActivity.kt` - Test/demo interface
- `BackgroundScanningIntegrationTest.kt` - Integration examples
- `BleDeviceManager.kt` - Enhanced with status methods
- `AndroidManifest.xml` - Service and permission declarations
- `ShimmerConfigActivity.kt` - Menu integration