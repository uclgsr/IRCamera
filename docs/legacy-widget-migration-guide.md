# Legacy Widget Migration Guide

## Overview
All legacy View-based widgets have been migrated to modern Jetpack Compose components. This guide provides the mapping between old and new components.

## Migration Mappings

### 1. Delete Confirmation Dialog
**Legacy:** `DelPopup`
```kotlin
val popup = DelPopup(context)
popup.onDelListener = { /* handle delete */ }
popup.show(anchorView)
```

**New:** `DeleteConfirmationDialog`
```kotlin
@Composable
fun MyScreen() {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        DeleteConfirmationDialog(
            onConfirm = { /* handle delete */ },
            onDismiss = { showDialog = false }
        )
    }
}
```

### 2. Recording Status Indicator
**Legacy:** `RecordingStatusIndicator`
```kotlin
val indicator = RecordingStatusIndicator(context)
indicator.startRecording(sessionId, sensors)
indicator.updateSensorStatus(sensor, status)
```

**New:** `RecordingStatusIndicator` (Compose)
```kotlin
@Composable
fun MyScreen() {
    RecordingStatusIndicator(
        isRecording = true,
        sessionId = "session123",
        activeSensors = setOf("[THM]", "[CAM]", "[GSR]"),
        startTime = System.currentTimeMillis()
    )
}
```

### 3. Camera Status Widget
**Legacy:** `CameraStatusWidget`
```kotlin
val widget = CameraStatusWidget(context)
widget.initializeWithCamera(lifecycleOwner, useFrontCamera)
```

**New:** `CameraStatusWidget` (Compose)
```kotlin
@Composable
fun MyScreen() {
    CameraStatusWidget(
        cameraRecorder = cameraRecorder,
        onInitializeCamera = { /* initialize */ }
    )
}
```

### 4. Camera Settings View
**Legacy:** `CameraSettingsView`
```kotlin
val settings = CameraSettingsView(context)
settings.onExposureModeToggle = { autoMode -> /* handle */ }
settings.onFocusModeToggle = { autoMode -> /* handle */ }
```

**New:** `CameraSettingsPanel`
```kotlin
@Composable
fun MyScreen() {
    var isAutoExposure by remember { mutableStateOf(true) }
    var isAutoFocus by remember { mutableStateOf(true) }
    
    CameraSettingsPanel(
        isAutoExposure = isAutoExposure,
        isAutoFocus = isAutoFocus,
        onExposureModeToggle = { isAutoExposure = it },
        onFocusModeToggle = { isAutoFocus = it }
    )
}
```

### 5. Comprehensive Sensor Status Widget
**Legacy:** `ComprehensiveSensorStatusWidget`
```kotlin
val widget = ComprehensiveSensorStatusWidget(context)
widget.updateSensorStatus("thermal", SensorStatus.STREAMING)
widget.updateRecordingStatus(true, sessionId)
```

**New:** `ComprehensiveSensorStatusDashboard`
```kotlin
@Composable
fun MyScreen() {
    val sensors = remember {
        listOf(
            SensorState("thermal", "Thermal Camera", SensorType.THERMAL, SensorStatus.STREAMING),
            SensorState("rgb", "RGB Camera", SensorType.RGB, SensorStatus.CONNECTED)
        )
    }
    
    ComprehensiveSensorStatusDashboard(
        sensors = sensors,
        recordingState = RecordingState(isRecording = true, sessionId = "session123")
    )
}
```

### 6. Tap to Focus Preview
**Legacy:** `TapToFocusPreviewView`
```kotlin
val previewView = TapToFocusPreviewView(context)
previewView.onTapToFocus = { x, y -> /* handle focus */ }
addView(previewView)
```

**New:** `TapToFocusPreview`
```kotlin
@Composable
fun MyScreen() {
    TapToFocusPreview(
        onTapToFocus = { normalizedX, normalizedY ->
            // Handle tap-to-focus
        },
        previewViewConfig = { previewView ->
            // Configure CameraX PreviewView
        }
    )
}
```

### 7. Sensor Selection Dialog
**Legacy:** `SensorSelectionDialog`
```kotlin
SensorSelectionDialog.show(context) { selectedSensors ->
    // Handle selection
}
```

**New:** `SensorSelectionDialog` (Compose)
```kotlin
@Composable
fun MyScreen() {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        SensorSelectionDialog(
            availableSensors = availableSensors,
            selectedSensors = selectedSensors,
            onSensorsSelected = { /* handle */ },
            onDismiss = { showDialog = false }
        )
    }
}
```

### 8. Recording Controls Widget
**Legacy:** `RecordingControlsWidget`
```kotlin
val widget = RecordingControlsWidget(context)
widget.updateRecordingState(RecordingState.RECORDING)
```

**New:** `RecordingControlsCompose`
```kotlin
@Composable
fun MyScreen() {
    RecordingControls(
        recordingSession = RecordingSession(
            state = RecordingState.RECORDING,
            triggerSource = TriggerSource.LOCAL
        ),
        onStartRecording = { /* start */ },
        onStopRecording = { /* stop */ }
    )
}
```

## Benefits of Compose Versions

### 1. **Modern UI Framework**
- Material 3 design system
- Built-in animations and transitions
- Responsive layouts

### 2. **Better State Management**
- StateFlow and MutableState for reactive updates
- No manual view updates needed
- Automatic recomposition

### 3. **Simplified Code**
- Declarative UI instead of imperative
- Less boilerplate code
- Easier to test and maintain

### 4. **Performance**
- Smart recomposition
- Optimized rendering
- Better memory management

### 5. **Type Safety**
- Compile-time checks
- Better IDE support
- Refactoring is safer

## Migration Strategy

### Phase 1: ✅ Complete
- All Compose replacements created
- Feature parity achieved
- Documentation updated

### Phase 2: Validation (In Progress)
- Test Compose versions in all use cases
- Verify animations and interactions
- Ensure performance is acceptable

### Phase 3: Deprecation (Next)
1. Add `@Deprecated` annotations to legacy files
2. Add migration hints in deprecation messages
3. Update all existing usages

### Phase 4: Removal (Future)
1. Verify no usages remain
2. Remove legacy View files
3. Clean up unused imports

## Support

For questions or issues during migration, refer to:
- `docs/compose-modernization.md` - Overall migration status
- Material 3 documentation: https://m3.material.io/
- Jetpack Compose documentation: https://developer.android.com/jetpack/compose
