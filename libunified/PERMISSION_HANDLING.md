# Permission Handling Guide

**Note:** This documentation has been consolidated into the main docs directory.  
Please refer to: [docs/developer-guides/permission-handling-guide.md](/docs/developer-guides/permission-handling-guide.md)

---

## Overview

The `PermissionTools` utility class provides a centralized way to request runtime permissions in the app. It uses the
standard `ActivityCompat.requestPermissions()` API which requires activities/fragments to handle permission results.

## Usage in Activities/Fragments

### 1. Request Permissions

Use the provided utility methods to request permissions:

```kotlin
class MyActivity : FragmentActivity() {
    
    fun requestCameraPermission() {
        PermissionTools.requestCamera(this) {
            // Permission granted - proceed with camera operations
            openCamera()
        }
    }
    
    fun requestBluetoothPermission() {
        PermissionTools.requestBluetooth(this, isBtFirst = true, object : PermissionTools.Callback {
            override fun onResult(allGranted: Boolean) {
                if (allGranted) {
                    // All permissions granted
                    startBluetoothScan()
                }
            }
            
            override fun onNever(isJump: Boolean) {
                // User permanently denied permissions
                if (isJump) {
                    // User was navigated to settings
                } else {
                    // User cancelled
                }
            }
        })
    }
}
```

### 2. Handle Permission Results

**IMPORTANT**: You **must** override `onRequestPermissionsResult()` in your Activity/Fragment and forward the result to
`PermissionTools`:

```kotlin
class MyActivity : FragmentActivity() {
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward the result to PermissionTools
        PermissionTools.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
```

## Available Permission Methods

- `requestRecordAudio(activity, callback)` - Request microphone permission
- `requestCamera(activity, callback)` - Request camera permission
- `requestLocation(activity, callback)` - Request location permissions
- `requestImageRead(activity, callback)` - Request media images permission
- `requestFile(activity, callback)` - Request file access permissions
- `requestBluetooth(activity, isBtFirst, callback)` - Request Bluetooth and location permissions
- `hasBtPermission(context)` - Check if Bluetooth permissions are granted

## Implementation Details

The implementation uses:

- `ActivityCompat.requestPermissions()` for requesting permissions
- `ContextCompat.checkSelfPermission()` for checking permission status
- Callbacks stored with `WeakReference` to avoid memory leaks
- Automatic permission checking before requesting (no-op if already granted)
- Material Design 3 dialogs for permission rationale and settings navigation

## Migration Notes

This implementation replaces the previous `registerForActivityResult()` approach which was incorrectly called
dynamically. The current implementation follows Android best practices by using the standard permission request API that
doesn't require registration during initialization.
