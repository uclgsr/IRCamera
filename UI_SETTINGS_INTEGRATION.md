# UI Settings Integration Guide

## Overview
This document describes the complete UI integration of recording settings across all sensor configuration screens in the IRCamera application.

## Architecture

The settings integration follows the **MVVM (Model-View-ViewModel)** pattern:

```
UI Screen (View) 
    ↓ User interaction
ViewModel (StateFlow)
    ↓ Business logic
Repository (SharedPreferences)
    ↓ Persistence
Sensor Recorders (SDK Integration)
```

## Settings Screens

### 1. Recording Settings Screen

**Location**: `app/src/main/java/mpdc4gsr/feature/settings/ui/RecordingSettingsScreen.kt`

**ViewModel**: `RecordingSettingsViewModel`

**Repository**: `RecordingSettingsRepository`

**Settings Available**:
- **Auto Recording**: Start recording automatically when devices are connected
- **Recording Quality**: Ultra (4K), High (1080p), Medium (720p), Low (480p)
- **Video Frame Rate**: 15-60 fps slider
- **Audio Recording**: Enable/disable audio in video recordings
- **Simultaneous Recording**: Start all sensors simultaneously or sequentially
- **Timestamp Synchronization**: Sync timestamps across recordings

**UI Integration**:
```kotlin
@Composable
fun RecordingSettingsScreen(
    viewModel: RecordingSettingsViewModel = viewModel()
) {
    val settings by viewModel.recordingSettings.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // UI components bound to settings state
    SettingsToggle(
        checked = settings.audioEnabled,
        onCheckedChange = { viewModel.updateAudioEnabled(it) }
    )
}
```

**User Flow**:
1. User opens Recording Settings screen
2. ViewModel loads current settings from repository
3. UI displays current settings values
4. User changes a setting (e.g., toggles audio recording)
5. ViewModel updates repository immediately
6. Repository saves to SharedPreferences
7. Settings applied during next recording session

### 2. Thermal Settings Screen

**Location**: `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalSettingsScreen.kt`

**ViewModel**: `ThermalSettingsViewModel` (NEW)

**Repository**: `ThermalSettingsRepository`

**Settings Available**:
- **Frame Rate**: 10-30 fps slider for thermal video recording
- **Save Raw Images**: Save individual thermal frames during recording
- **Color Palette**: Iron, Rainbow, Gray, Hot, Cool
- **Temperature Unit**: Celsius, Fahrenheit, Kelvin
- **Temperature Range**: Auto, Custom ranges
- **Emissivity**: 0.1-1.0 slider for material emissivity correction
- **Auto Scale**: Automatically adjust temperature scale
- **Show Crosshair**: Display center point crosshair

**UI Integration**:
```kotlin
@Composable
fun ThermalSettingsScreen(
    viewModel: ThermalSettingsViewModel = viewModel()
) {
    val settings by viewModel.thermalSettings.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Recording settings
    SettingsSlider(
        value = settings.frameRate.toFloat(),
        valueRange = 10f..30f,
        onValueChange = { viewModel.updateFrameRate(it.toInt()) }
    )
    
    // Display settings
    SettingsDropdown(
        value = settings.palette,
        options = listOf("Iron", "Rainbow", "Gray", "Hot", "Cool"),
        onValueChange = { viewModel.updatePalette(it) }
    )
}
```

**User Flow**:
1. User opens Thermal Settings screen
2. ViewModel loads settings from ThermalSettingsRepository
3. UI displays current thermal camera configuration
4. User adjusts frame rate slider
5. ViewModel updates repository in real-time
6. Settings applied to VideoRecordFFmpeg during recording
7. Frame rate affects video quality and bitrate automatically

### 3. GSR Settings Screen

**Location**: `app/src/main/java/mpdc4gsr/feature/gsr/ui/GSRSettingsScreen.kt`

**ViewModel**: `GSRSettingsViewModel`

**Repository**: `GSRSettingsRepository`

**Settings Available**:
- **Sampling Rate**: 1-512 Hz slider for Shimmer3 GSR sampling
- **Device Name**: Display connected device name
- **Auto Reconnect**: Automatically reconnect after disconnection
- **Real-Time Monitoring**: Enable real-time data monitoring
- **Data Filtering**: Apply filtering to GSR data
- **Notifications**: Show data collection notifications
- **Buffer Size**: Data buffering configuration
- **Connection Timeout**: Timeout for device connections

**UI Integration**:
```kotlin
@Composable
fun GSRSettingsScreen(
    viewModel: GSRSettingsViewModel = viewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    val gsrSettings = uiState.gsrSettings
    val deviceSettings = uiState.deviceSettings
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Sampling rate configuration
    SettingsSlider(
        value = gsrSettings.samplingRate.toFloat(),
        valueRange = 1f..512f,
        onValueChange = { viewModel.updateSamplingRate(it.toInt()) }
    )
    
    // Device settings
    SettingsToggle(
        checked = deviceSettings.autoReconnect,
        onCheckedChange = { 
            viewModel.updateDeviceSettings(
                deviceSettings.copy(autoReconnect = it)
            )
        }
    )
}
```

**User Flow**:
1. User opens GSR Settings screen
2. ViewModel loads GSR and device settings from repository
3. UI displays current Shimmer3 configuration
4. User adjusts sampling rate (e.g., from 128 Hz to 256 Hz)
5. ViewModel updates GSRSettingsRepository
6. Settings saved to SharedPreferences
7. During recording, GSRSensorRecorder reads sampling rate
8. Shimmer SDK configured with `setSamplingRateShimmer()`

## Navigation to Settings

### From Main Menu

Users can access settings through the main settings screen:

**SettingsScreen.kt** provides navigation to all settings screens:

```kotlin
// Navigate to Recording Settings
onNavigateToRecordingSettings?.invoke()

// Navigate to Thermal Settings (via thermal menu)
// Navigate to GSR Settings (via GSR menu)
```

### Direct Access

Settings screens can also be accessed directly from:
- Recording screen (quick access to recording settings)
- Thermal camera screen (thermal settings shortcut)
- GSR sensor screen (device configuration)

## State Management

### StateFlow Pattern

All settings use Kotlin StateFlow for reactive UI updates:

```kotlin
// In ViewModel
private val _settings = MutableStateFlow(Settings())
val settings: StateFlow<Settings> = _settings.asStateFlow()

// In Composable
val settings by viewModel.settings.collectAsState()
```

**Benefits**:
- **Reactive**: UI automatically updates when settings change
- **Lifecycle-aware**: No memory leaks with Compose integration
- **Type-safe**: Compile-time checking of setting values
- **Testable**: Easy to unit test ViewModel logic

### Settings Persistence

All settings are persisted using SharedPreferences:

```kotlin
// Save
prefs.edit().putInt("thermal_frame_rate", frameRate).apply()

// Load
val frameRate = prefs.getInt("thermal_frame_rate", 25)
```

**Persistence Keys**:
- Recording: `recording_*` prefix
- Thermal: `thermal_*` prefix
- GSR: `gsr_*` prefix

## Settings Application Flow

### Recording Settings → RGB Camera

1. User sets "Recording Quality" to "High" in UI
2. RecordingSettingsViewModel updates RecordingSettingsRepository
3. Repository saves quality = "High" to SharedPreferences
4. During recording, RgbCameraRecorder calls `repository.getSettings()`
5. RgbCameraRecorder applies 1920x1080 resolution, 30fps, 20Mbps bitrate
6. Video recorded with user-selected quality

### Thermal Settings → Topdon SDK

1. User sets "Frame Rate" to 25 fps in UI
2. ThermalSettingsViewModel updates ThermalSettingsRepository
3. Repository saves frameRate = 25 to SharedPreferences
4. During recording, ThermalRecorder loads settings from repository
5. VideoRecordFFmpeg receives customFrameRate = 25
6. FFmpeg encoder configured: `recorder.frameRate = 25.0`
7. Thermal video recorded at 25 fps

### GSR Settings → Shimmer SDK

1. User sets "Sampling Rate" to 256 Hz in UI
2. GSRSettingsViewModel updates GSRSettingsRepository
3. Repository saves samplingRate = 256 to SharedPreferences
4. During recording, GSRSensorRecorder loads settings
5. Sampling rate validated: 256 Hz (within 1-512 Hz range)
6. Shimmer device configured: `device.setSamplingRateShimmer(256.0)`
7. GSR data sampled at 256 Hz

## Validation

### UI-Level Validation

Settings are validated at the UI level:

```kotlin
// Frame rate constrained by slider
SettingsSlider(
    valueRange = 10f..30f,  // Hardware limit
    ...
)

// Sampling rate constrained
SettingsSlider(
    valueRange = 1f..512f,  // SDK limit
    ...
)
```

### Repository-Level Validation

Additional validation in repositories:

```kotlin
// ThermalSettingsRepository
fun updateFrameRate(frameRate: Int) {
    val validated = frameRate.coerceIn(10, 30)
    prefs.edit().putInt(KEY_FRAME_RATE, validated).apply()
}

// GSRSensorRecorder
effectiveSamplingRate = effectiveSamplingRate.coerceIn(
    SHIMMER_MIN_SAMPLING_RATE,  // 1.0 Hz
    SHIMMER_MAX_SAMPLING_RATE   // 512.0 Hz
)
```

## Testing UI Integration

### Manual Testing

1. **Recording Settings**:
   - Open Recording Settings screen
   - Change Quality to "Low"
   - Change Frame Rate to 15 fps
   - Toggle Audio Recording off
   - Start recording
   - Verify video is 480p @ 15fps with no audio

2. **Thermal Settings**:
   - Open Thermal Settings screen
   - Set Frame Rate to 15 fps
   - Enable Save Raw Images
   - Change Palette to "Rainbow"
   - Start thermal recording
   - Verify 15 fps in video properties
   - Check for individual thermal frame images

3. **GSR Settings**:
   - Open GSR Settings screen
   - Set Sampling Rate to 64 Hz
   - Enable Data Filtering
   - Start GSR recording
   - Check logs for "Shimmer sampling rate configured: 64.0Hz"
   - Verify CSV has ~64 samples per second

### Verification Logs

Check logcat for settings application:

```
RecordingSettingsValidator: Recording Quality: Low
RecordingSettingsValidator: Video Frame Rate: 15 fps
RecordingSettingsValidator: Audio Enabled: false

ThermalRecorder: Thermal settings loaded: frameRate=15fps, saveImages=true
VideoRecordFFmpeg: Thermal video recorder configured: 15fps, 800000bps

GSRSensorRecorder: GSR Settings loaded: samplingRate=64Hz
GSRSensorRecorder: Shimmer sampling rate configured: 64.0Hz
```

## UI Components

### Reusable Settings Components

All settings screens use common components from `mpdc4gsr.core.ui.components.settings`:

- **SettingsCard**: Grouped settings with title and icon
- **SettingsToggle**: Boolean on/off switch
- **SettingsSlider**: Numeric value slider with unit display
- **SettingsDropdown**: Single selection from options list
- **SettingsRow**: Read-only setting display

Example:
```kotlin
SettingsCard(
    title = "Recording Settings",
    icon = Icons.Default.Videocam
) {
    SettingsSlider(
        label = "Frame Rate",
        value = settings.frameRate.toFloat(),
        valueRange = 10f..30f,
        onValueChange = { viewModel.updateFrameRate(it.toInt()) },
        unit = " fps"
    )
}
```

## Troubleshooting

### Settings Not Persisting

**Symptom**: Changes lost after app restart

**Solution**: 
- Verify `viewModel.initialize(context)` is called in LaunchedEffect
- Check SharedPreferences keys match between save and load
- Ensure `.apply()` is called after edit

### Settings Not Applied to Recording

**Symptom**: Recording uses default values

**Solution**:
- Check sensor recorder loads settings from repository
- Verify repository singleton is used (not new instances)
- Check logs for settings loading messages
- Ensure settings are read before recording starts

### UI Not Updating

**Symptom**: UI shows old values after changing settings

**Solution**:
- Verify `collectAsState()` is used in Composable
- Check ViewModel updates StateFlow when settings change
- Ensure repository emits to StateFlow after save

## Future Enhancements

- [ ] Settings export/import for backup
- [ ] Settings profiles (presets for different use cases)
- [ ] Settings validation with error messages
- [ ] Settings reset to defaults button
- [ ] Settings search/filter
- [ ] Settings history/undo
- [ ] Cloud sync for settings across devices

## Related Documentation

- `RECORDING_SETTINGS_INTEGRATION.md` - Technical details
- `SDK_INTEGRATION_ENHANCEMENT.md` - SDK utilization
- `SDK_INTEGRATION_VERIFICATION.md` - Verification report
- `SETTINGS_VERIFICATION_GUIDE.md` - Testing procedures
