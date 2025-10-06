# Thermal Camera Recognition Fix

## Problem Statement

The app was not recognizing the thermal camera when it was already connected during app startup. Users would see the app
enter simulation mode even though a physical Topdon TC001 thermal camera was connected via USB.

## Root Cause Analysis

The issue had multiple contributing factors:

1. **USB Enumeration Timing**: When the app initializes, the USB system might not have fully enumerated all connected
   devices yet. The initial scan in `ThermalCameraRecorder.initialize()` could happen before the USB system is ready.

2. **No Initial Device Detection**: The Android USB system only broadcasts `ACTION_USB_DEVICE_ATTACHED` intents when a
   device is **physically attached while the app is running**. If a device is already connected when the app starts, no
   broadcast is sent.

3. **Missing Retry Logic**: The original code scanned once for USB devices and immediately fell back to simulation mode
   if nothing was found, with no retry mechanism.

4. **Permission Request Bug**: In `onDevicePermissionRequested()`, when permission was not yet granted, the code would
   set simulation mode and emit an error instead of actually requesting the USB permission.

## Solution

### 1. Added Delayed Retry in Initialization

**File**: `ThermalCameraRecorder.kt`

Added a 500ms delay and retry in the `initialize()` method to give the USB system time to enumerate devices:

```kotlin
val deviceFound = scanForThermalCameraDevicesWithPermissions()

if (!deviceFound) {
    Log.w(TAG, "No thermal cameras found on initial scan, will retry after delay")
    delay(500)
    
    val retryFound = scanForThermalCameraDevicesWithPermissions()
    if (!retryFound) {
        // Fall back to simulation mode
        isSimulationMode = true
    } else {
        Log.i(TAG, "Thermal camera found on retry attempt")
    }
}
```

### 2. Added Manual Rescan Functionality

**File**: `ThermalCameraRecorder.kt`

Created a new public `rescanForThermalCamera()` method that can be called to manually scan for USB devices:

```kotlin
suspend fun rescanForThermalCamera(): Boolean {
    // Scans all connected USB devices
    // If thermal camera found with permission, initializes it
    // If found without permission, requests permission
    // Returns true if camera was successfully initialized
}
```

This method:

- Iterates through all connected USB devices
- Identifies thermal cameras using `device.isTcTsDevice()`
- Checks for USB permissions
- Initializes the camera if permission is granted
- Requests permission if not granted

### 3. Fixed Permission Request Handler

**File**: `ThermalCameraRecorder.kt`

Fixed the `onDevicePermissionRequested()` method to actually request permission instead of immediately failing:

```kotlin
if (permissionGranted) {
    // Initialize camera
} else {
    Log.i(TAG, "USB permission not yet granted, requesting permission")
    requestUsbPermission(device)  // Actually request it!
}
```

### 4. Added ViewModel Integration

**File**: `ThermalCameraViewModel.kt`

Added `rescanForThermalCamera()` method to expose the rescan functionality to the UI layer:

```kotlin
fun rescanForThermalCamera() {
    viewModelScope.launch {
        val found = thermalRecorder?.rescanForThermalCamera() ?: false
        // Update UI state based on result
    }
}
```

### 5. Added Automatic Rescan on Screen Appearance

**File**: `ThermalMonitorScreen.kt`

Added a `LaunchedEffect` that automatically triggers a camera rescan 1 second after the screen appears:

```kotlin
LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(1000)
    viewModel.rescanForThermalCamera()
}
```

This handles the case where:

- The app starts with camera already connected
- The user navigates to the thermal camera screen
- After 1 second, it checks again for connected cameras

## Behavior Flow

### Scenario 1: Camera Already Connected at App Start

1. App starts → `ThermalCameraRecorder.initialize()` called
2. Initial USB scan (might miss device if USB system not ready)
3. Wait 500ms
4. Retry USB scan (should find device now)
5. If found: Initialize camera
6. If not found: Enter simulation mode
7. User navigates to ThermalMonitorScreen
8. After 1 second: `rescanForThermalCamera()` called
9. If camera found: Exit simulation mode, initialize camera
10. User sees real thermal camera feed

### Scenario 2: Camera Plugged In After App Start

1. App starts in simulation mode (no camera)
2. User plugs in thermal camera
3. `ThermalUsbReceiver` receives `ACTION_USB_DEVICE_ATTACHED`
4. Broadcasts device connection event via `DeviceEventManager`
5. `ThermalCameraRecorder.observeDeviceEvents()` receives event
6. If permission granted: Initialize camera immediately
7. If no permission: Request USB permission
8. User grants permission
9. `onDevicePermissionRequested()` called
10. Camera initialized, exit simulation mode

### Scenario 3: User Navigates to Camera Screen

1. User on main screen
2. Thermal camera connected but not detected yet
3. User taps "Thermal Camera" button
4. `ThermalMonitorScreen` appears
5. After 1 second: Automatic rescan triggered
6. Camera found and initialized
7. User sees thermal camera feed immediately

## Testing

### Manual Testing Steps

1. **Test 1: Camera Already Connected**
    - Connect thermal camera to device
    - Start the app
    - Navigate to Thermal Camera screen
    - Verify: Camera should be detected within 1-2 seconds

2. **Test 2: Hot Plug**
    - Start app without camera
    - Navigate to Thermal Camera screen (simulation mode)
    - Plug in thermal camera
    - Verify: Camera should be detected and initialized
    - Verify: UI should switch from simulation to real camera

3. **Test 3: Permission Flow**
    - Connect camera for first time
    - Start app
    - Verify: USB permission dialog appears
    - Grant permission
    - Verify: Camera initializes successfully

### Expected Log Messages

Successful initialization:

```
ThermalCameraRecorder: Initializing thermal camera for sensor thermal_camera_1
ThermalCameraRecorder: Found 3 USB devices, scanning for thermal cameras
ThermalCameraRecorder: Found thermal camera device: TC001 (VID=bda, PID=5830)
ThermalCameraRecorder: USB permission already granted for thermal camera
ThermalCameraRecorder: Real thermal camera initialized successfully
```

Retry successful:

```
ThermalCameraRecorder: No thermal cameras found on initial scan, will retry after delay
ThermalCameraRecorder: Found 3 USB devices during rescan
ThermalCameraRecorder: Found thermal camera during rescan: TC001
ThermalCameraRecorder: Successfully initialized thermal camera from rescan
```

## Files Modified

1. `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalCameraRecorder.kt`
    - Added `rescanForThermalCamera()` method
    - Added retry logic in `initialize()`
    - Fixed `onDevicePermissionRequested()` to request permission

2. `app/src/main/java/mpdc4gsr/feature/thermal/presentation/ThermalCameraViewModel.kt`
    - Added `rescanForThermalCamera()` method

3. `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalMonitorScreen.kt`
    - Added `LaunchedEffect` for automatic rescan

## Impact

- **Improved User Experience**: Camera now recognized reliably when already connected
- **Reduced Support Issues**: Users no longer need to restart app multiple times
- **Better Reliability**: Multiple fallback mechanisms ensure camera detection
- **Minimal Performance Impact**: 500ms delay only on initial startup, 1s delay on screen navigation

## Future Improvements

1. Add visual feedback during camera detection (loading spinner)
2. Add manual "Scan for Camera" button in UI
3. Implement exponential backoff for retry attempts
4. Add telemetry to track detection success rates
5. Consider background USB monitoring service for instant detection
