# Permission Handling Guide

## Overview

`PermissionTools` centralises runtime permission requests by exposing a lifecycle-aware controller that wraps the Android
`ActivityResultContracts.RequestMultiplePermissions` API. Permissions are modelled as feature areas
(`FeaturePermissionArea`) so that the same bundle of runtime grants is reused consistently across GSR, RGB, and thermal
workflows.

## Quick Start

```kotlin
class MyActivity : ComponentActivity() {
    private val permissionController = PermissionTools.controller(this)

    fun ensureCamera() {
        permissionController.requestFeature(FeaturePermissionArea.RGB_VIDEO) {
            openCamera()
        }
    }

    fun ensureBluetooth(onDenied: () -> Unit) {
        permissionController.requestFeature(
            FeaturePermissionArea.GSR_SENSORS,
            onGranted = { startBluetoothScan() },
            onDenied = { onDenied() },
        )
    }
}
```

### Checking state without prompting

```kotlin
val hasMedia = PermissionTools.hasPermissions(
    context = this,
    feature = FeaturePermissionArea.MEDIA_REVIEW,
)
```

## Feature Groups

Each area maps to the concrete runtime permissions needed on the current API level:

| Feature | Purpose | Typical Usage |
| --- | --- | --- |
| `GSR_SENSORS` | Bluetooth LE scan/connect (plus legacy location fallback) | Shimmer discovery and telemetry |
| `RGB_VIDEO` | Camera + microphone + scoped media | RGB capture alongside GSR |
| `THERMAL_IR` | Camera, microphone, media, nearby Wi-Fi | Thermal/IR capture and control |
| `MEDIA_REVIEW` | Read captured media (scoped storage) | Gallery/report review flows |
| `NOTIFICATIONS` | Post notifications (Android 13+) | Remote commands / alerts |

Compose onboarding already aggregates these groups so modal prompts align with the same feature definitions.

## Advanced Usage

Request multiple features at once:

```kotlin
permissionController.requestFeatures(
    features = listOf(FeaturePermissionArea.RGB_VIDEO, FeaturePermissionArea.GSR_SENSORS),
    onGranted = ::startSession,
    onDenied = { deniedPermissions ->
        showSnackbar("Missing: ${deniedPermissions.joinToString()}")
    },
)
```

The controller queues concurrent requests and delivers results in the order the prompts were launched.

## Removal of Deprecated APIs

Older helpers such as `requestCamera`, `requestBluetooth`, and
`PermissionTools.onRequestPermissionsResult` have been removed. Activities that still rely on the deprecated pattern
should migrate to the controller-based approach shown above.

For read-only checks (e.g., BLE scans), prefer the feature helper:

```kotlin
if (PermissionTools.hasBtPermission(context)) {
    BluetoothUtils.startLeScan(context)
}
```

This ensures permission logic stays in sync with new Android requirements without scattering individual manifest
strings throughout the codebase.
