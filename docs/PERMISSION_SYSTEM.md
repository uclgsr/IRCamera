# Enhanced Permission System Documentation

## Overview

The IRCamera app now includes a comprehensive permission handling system that ensures proper runtime permissions for all multi-sensor recording functionality. This system handles Camera, Bluetooth, Location, Storage, USB, and Notification permissions with graceful user experience and clear rationale dialogs.

## Key Features

### 1. Centralized Permission Management
- **`PermissionController.ensureAll()`**: Single method to request all required permissions
- **Permission Grouping**: Logically groups related permissions for better UX
- **Status Methods**: Convenient methods to check specific functionality availability

### 2. Graceful Permission Handling
- **Clear Rationale Dialogs**: Explains why each permission is needed
- **Partial Functionality**: Continues with limited features when non-critical permissions are denied
- **Retry Mechanisms**: Allows users to retry permission requests
- **Settings Integration**: Direct links to app settings for permanently denied permissions

### 3. Sensor-Specific Permission Checks
- **Camera Recording**: `canStartRecording()` - Camera + Storage permissions
- **Shimmer GSR**: `canConnectToShimmer()` - Bluetooth + Location permissions
- **Notifications**: `canShowNotifications()` - POST_NOTIFICATIONS (Android 13+)
- **USB Thermal Camera**: Device-specific USB permission requests

## Quick Start Guide

### Basic Integration

```kotlin
class YourActivity : FragmentActivity() {
    private lateinit var permissionController: PermissionController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize permission controller
        permissionController = PermissionController(this)
        permissionController.initialize()
        
        // Request all permissions at startup
        requestPermissions()
    }
    
    private fun requestPermissions() {
        permissionController.ensureAll { allGranted, deniedPermissions ->
            if (allGranted) {
                // All permissions granted - enable full functionality
                enableAllFeatures()
            } else {
                // Some permissions denied - enable partial functionality
                enablePartialFeatures(deniedPermissions)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
```

### Checking Specific Functionality

```kotlin
// Before starting video recording
if (permissionController.canStartRecording()) {
    startVideoRecording()
} else {
    showCameraPermissionDialog()
}

// Before connecting to Shimmer GSR sensor
if (permissionController.canConnectToShimmer()) {
    initializeShimmerConnection()
} else {
    showBluetoothPermissionDialog()
}

// Before showing recording status notifications
if (permissionController.canShowNotifications()) {
    showRecordingNotification()
} else {
    useAlternativeStatusIndicator()
}
```

### USB Device Permission (Thermal Camera)

```kotlin
// Request permission for specific USB device
val thermalCamera = usbManager.findThermalCamera()
permissionController.requestUsbPermission(thermalCamera) { granted, device ->
    if (granted && device != null) {
        initializeThermalCamera(device)
    } else {
        showThermalCameraUnavailable()
    }
}
```

### Battery Optimization Exemption

```kotlin
// Request battery optimization exemption for reliable background recording
permissionController.requestBatteryOptimizationExemption { exemptionGranted ->
    if (exemptionGranted) {
        Log.i(TAG, "Battery optimization disabled - reliable recording enabled")
    } else {
        Log.w(TAG, "Battery optimization active - recording may be interrupted")
        showBatteryOptimizationWarning()
    }
}
```

## Permission Categories

### 1. Camera & Audio Permissions
- **CAMERA**: Required for RGB video recording
- **RECORD_AUDIO**: Required for video with audio (optional)

**Rationale**: "Camera permission is required for RGB video recording during multi-sensor sessions."

### 2. Bluetooth & Location Permissions

#### Android 12+ (API 31+)
- **BLUETOOTH_SCAN**: Required to discover Shimmer GSR devices
- **BLUETOOTH_CONNECT**: Required to connect to Shimmer GSR devices
- **ACCESS_FINE_LOCATION** or **ACCESS_COARSE_LOCATION**: Required for BLE scanning

#### Legacy Android (API < 31)
- **BLUETOOTH**: Basic Bluetooth functionality
- **BLUETOOTH_ADMIN**: Advanced Bluetooth operations
- **ACCESS_COARSE_LOCATION** or **ACCESS_FINE_LOCATION**: Required for BLE scanning

**Rationale**: "Bluetooth and Location permissions are required to scan for and connect to Shimmer GSR sensors via Bluetooth Low Energy."

### 3. Storage Permissions

#### Android 13+ (API 33+)
- **READ_MEDIA_VIDEO**: Access to recorded videos
- **READ_MEDIA_IMAGES**: Access to captured images
- **READ_MEDIA_VISUAL_USER_SELECTED**: Partial media access

#### Legacy Android (API < 33)
- **WRITE_EXTERNAL_STORAGE**: Write recordings to storage
- **READ_EXTERNAL_STORAGE**: Read existing recordings

**Rationale**: "Storage permissions are required to save and manage multi-sensor recording data."

### 4. Foreground Service & Background Recording
- **FOREGROUND_SERVICE**: Run background recording service
- **FOREGROUND_SERVICE_CAMERA**: Camera access in background
- **FOREGROUND_SERVICE_MICROPHONE**: Microphone access in background
- **FOREGROUND_SERVICE_MEDIA_PROJECTION**: Screen recording capabilities

**Rationale**: "Background recording permissions ensure continuous multi-sensor data collection during long recording sessions."

### 5. Notification Permissions (Android 13+)
- **POST_NOTIFICATIONS**: Show recording status notifications

**Rationale**: "Notification permission allows the app to show recording status and important alerts."

### 6. USB Host Feature & Device Permissions
- **USB Host Feature**: Declared in manifest for thermal camera support
- **Per-device USB permissions**: Runtime permission for each USB thermal camera

**Rationale**: "USB permissions are required to connect to thermal imaging cameras for temperature data collection."

## Error Handling Strategies

### 1. Critical vs Non-Critical Permissions

**Critical Permissions** (app cannot function without):
- CAMERA (for video recording)
- BLUETOOTH_SCAN + BLUETOOTH_CONNECT (for GSR sensor)
- ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION (for BLE scanning)

**Non-Critical Permissions** (reduced functionality):
- RECORD_AUDIO (silent video recording possible)
- POST_NOTIFICATIONS (alternative status indicators)
- Storage permissions (app-specific storage fallback)

### 2. Graceful Degradation

```kotlin
private fun updateUIForPartialPermissions(deniedPermissions: List<String>) {
    val canRecord = permissionController.canStartRecording()
    val canConnectShimmer = permissionController.canConnectToShimmer()
    
    // Update UI based on available permissions
    recordingButton.isEnabled = canRecord
    shimmerButton.isEnabled = canConnectShimmer
    
    // Show specific warnings
    if (!canRecord) showCameraWarning()
    if (!canConnectShimmer) showShimmerWarning()
}
```

### 3. Permanently Denied Permissions

The system automatically detects permanently denied permissions and:
1. Shows detailed explanation of required functionality
2. Provides direct link to app settings
3. Offers "Continue Limited" option for non-critical permissions
4. Exits app if critical permissions are permanently denied

## Testing Guidelines

### 1. Fresh Install Testing
1. Install app on device with no previous permissions
2. Launch app and verify permission request sequence
3. Test all permission scenarios:
   - Grant all permissions
   - Deny individual permissions
   - Deny all permissions
   - Permanently deny permissions

### 2. Permission State Testing
```kotlin
// Test different permission states
fun testPermissionStates() {
    // Test with no permissions
    assertFalse(permissionController.canStartRecording())
    assertFalse(permissionController.canConnectToShimmer())
    
    // Test with camera only
    grantCameraPermission()
    assertTrue(permissionController.hasCameraPermission())
    
    // Test with bluetooth only
    grantBluetoothPermissions()
    assertTrue(permissionController.canConnectToShimmer())
}
```

### 3. USB Permission Testing
1. Connect thermal camera device
2. Launch app - should show USB permission dialog
3. Test grant/deny scenarios
4. Verify device connection after permission grant

## Best Practices

### 1. Request Permissions at Right Time
- **On App Launch**: Critical permissions (camera, bluetooth)
- **Before Recording**: Verify recording permissions
- **Before USB Access**: USB device permissions
- **Before Background Recording**: Battery optimization exemption

### 2. Provide Clear Context
- Always explain why permissions are needed
- Show specific benefits to user
- Provide alternatives when possible

### 3. Handle All Scenarios
- Permission granted ✓
- Permission denied ✓
- Permission permanently denied ✓
- Partial permissions ✓
- No permissions ✓

### 4. Update UI Appropriately
- Disable unavailable features
- Show clear status messages
- Provide retry options
- Guide users to solutions

## FileProvider Configuration

The app uses secure FileProvider for sharing recorded files:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/provider_paths" />
</provider>
```

This enables secure sharing of recordings without requiring broad storage permissions.

## Troubleshooting

### Common Issues

1. **Bluetooth scanning fails on Android 12+**
   - Ensure BLUETOOTH_SCAN and ACCESS_FINE_LOCATION are granted
   - Check that location services are enabled on device

2. **USB thermal camera not detected**
   - Verify USB host feature in manifest
   - Check device compatibility with thermal camera
   - Ensure USB permission is granted for specific device

3. **Background recording interrupted**
   - Request battery optimization exemption
   - Check foreground service permissions
   - Verify notification channel setup

4. **File sharing fails**
   - Verify FileProvider configuration
   - Check provider_paths.xml configuration
   - Ensure proper URI generation

### Debug Commands

```kotlin
// Log current permission status
Log.i(TAG, "Permission Status: ${permissionController.getPermissionStatusMessage()}")

// Check specific permissions
Log.i(TAG, "Can start recording: ${permissionController.canStartRecording()}")
Log.i(TAG, "Can connect Shimmer: ${permissionController.canConnectToShimmer()}")
Log.i(TAG, "Battery optimized: ${!permissionController.isBatteryOptimizationDisabled()}")
```

This comprehensive permission system ensures that the IRCamera app provides a smooth, user-friendly experience while maintaining security and proper functionality across all Android versions.