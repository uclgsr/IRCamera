# Phase 1: Permissions Handling Implementation

**Note:** This documentation has been consolidated into the main docs directory.  
Please refer to: [docs/developer-guides/ui-components-guide.md](/docs/developer-guides/ui-components-guide.md)

---

This directory contains the comprehensive permissions handling system for the Multi-Modal
Physiological Sensing Platform, specifically designed for Samsung S22 (Android 12+) multi-sensor
recording.

## Overview

The permissions system ensures all necessary runtime permissions are obtained for:

- **RGB Camera & Audio**: Video recording with sound
- **Bluetooth & Location**: Shimmer3 GSR sensor BLE scanning
- **USB**: Topdon thermal camera hot-plug support
- **Storage**: File management (legacy and Android 13+ scoped media)
- **Foreground Services & Notifications**: Background recording operation
- **Battery Optimization**: Reliable long-duration recording

## Key Components

### PermissionController.kt

Centralized permission management class that handles:

- Android version-aware permission requests
- USB device permission handling
- Battery optimization exemption
- User-friendly explanations and error dialogs
- Graceful handling of permission denials

### PermissionUtils.kt

Utility functions for permission validation and checking.

## Usage Example

```kotlin
class MyActivity : FragmentActivity() {
    private lateinit var permissionController: PermissionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionController = PermissionController(this)
        permissionController.initialize()

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!permissionController.hasAllRequiredPermissions()) {
            permissionController.requestAllPermissions { allGranted, deniedPermissions ->
                if (allGranted) {

                    startRecording()
                } else {

                    handleMissingPermissions(deniedPermissions)
                }
            }
        } else {

            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onUsbDeviceAttached(device: UsbDevice) {

        permissionController.requestUsbPermission(device) { granted, device ->
            if (granted) {

                initializeThermalCamera(device)
            } else {

                showUsbPermissionError()
            }
        }
    }
}
```

## Supported Permissions

### Camera & Audio

- `CAMERA` - RGB video recording
- `RECORD_AUDIO` - Video with sound

### Bluetooth & Location

- `BLUETOOTH_SCAN` (Android 12+) - BLE device discovery
- `BLUETOOTH_CONNECT` (Android 12+) - BLE device connections
- `BLUETOOTH` + `BLUETOOTH_ADMIN` (Legacy) - Older Android BLE support
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - Required for BLE scanning

### Storage

- `WRITE_EXTERNAL_STORAGE` + `READ_EXTERNAL_STORAGE` (Legacy)
- `READ_MEDIA_VIDEO` + `READ_MEDIA_IMAGES` + `READ_MEDIA_VISUAL_USER_SELECTED` (Android 13+)

### Foreground Services

- `FOREGROUND_SERVICE` - Background operation
- `FOREGROUND_SERVICE_CAMERA` - Camera background access
- `FOREGROUND_SERVICE_MICROPHONE` - Microphone background access
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Media projection background access

### Notifications & Battery

- `POST_NOTIFICATIONS` (Android 13+) - Recording status notifications
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Battery optimization exemption

## Key Features

### Android Version Awareness

The system automatically detects Android version and requests appropriate permissions:

- Android 12+: Modern Bluetooth permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`)
- Android 13+: Scoped media permissions (`READ_MEDIA_*`)
- Legacy: Older permission models for compatibility

### USB Hot-Plug Support

Integrates with existing USB permission infrastructure to handle thermal camera connections:

- Detects USB device attachment
- Requests permission using `UsbManager.requestPermission()`
- Uses existing `DeviceBroadcastReceiver` infrastructure

### User Experience

- Clear explanations of why permissions are needed
- Friendly permission names (e.g., "Camera (for RGB video recording)")
- Graceful handling of denied permissions
- Direct links to app settings for manual permission management

### Error Handling

- Distinguishes between critical and optional permissions
- Provides fallback options when possible
- Clear error messages and recovery suggestions

## Testing Guidelines

1. **Fresh Install Testing**: Install app on device that has never had it before
2. **Permission Denial**: Deny various permissions to test error handling
3. **USB Hot-Plug**: Test thermal camera connection with permission flow
4. **Android Version Testing**: Test on different Android versions (11, 12, 13+)
5. **Battery Optimization**: Test exemption request flow

## Integration Notes

This permissions system is designed to integrate with:

- `MultiModalRecordingActivity` - Main recording interface
- `RecordingService` - Background recording service
- `ThermalCameraRecorder` - USB thermal camera handling
- `GSRSensorRecorder` - Bluetooth GSR sensor handling
- `RGBCameraRecorder` - Camera video recording

The system follows Android best practices and is designed to be maintainable and extensible for
future sensor additions.
