# Recording Settings Integration

This document describes how recording settings are integrated and applied to sensors.

## Architecture

### RecordingSettingsRepository
- **Location**: `app/src/main/java/mpdc4gsr/feature/settings/data/RecordingSettingsRepository.kt`
- **Purpose**: Centralized repository for recording settings using Repository pattern
- **Storage**: SharedPreferences via PreferenceManager
- **Singleton**: Provides global access via `getInstance(context)`

### Settings Flow
1. User modifies settings in RecordingSettingsScreen
2. RecordingSettingsViewModel updates via RecordingSettingsRepository
3. Settings are persisted to SharedPreferences
4. Recording controllers read settings when starting recording

## Settings Applied

### RGB Camera Recorder
**File**: `app/src/main/java/mpdc4gsr/core/data/RgbCameraRecorder.kt`

Settings applied:
- **Recording Quality**: Maps to video resolution and bitrate
  - Ultra: 4K (3840x2160) @ 60fps, 50Mbps
  - High: 1080p (1920x1080) @ 30fps, 20Mbps
  - Medium: 720p (1280x720) @ 30fps, 10Mbps
  - Low: 480p (854x480) @ 24fps, 5Mbps
- **Video Frame Rate**: Applied to video recording (15-60 fps)
- **Audio Enabled**: Controls audio recording in video

**Code Locations**:
- Settings loaded in: `initialize()` method (line ~222)
- Video configuration: `optimizeVideoConfiguration()` method (line ~533)
- Audio configuration: `startRecording()` method (line ~1074)

### Main Recording Controller
**File**: `app/src/main/java/mpdc4gsr/feature/network/data/MainRecordingController.kt`

Settings applied:
- **Simultaneous Recording**: Controls whether sensors start simultaneously or sequentially
- **Timestamp Sync**: Logged for future synchronization features

**Code Location**: `startRecording()` method (line ~60)

### GSR Sensor Recorder
**File**: `app/src/main/java/mpdc4gsr/feature/gsr/data/GSRSensorRecorder.kt`

GSR has its own settings repository (GSRSettingsRepository) which manages:
- Sampling rate
- Device settings
- Calibration parameters

These are configured separately in GSRSettingsScreen.

### Thermal Recorder
**File**: `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalRecorder.kt`

Currently no user-configurable settings applied. Thermal recording is controlled by:
- saveImages parameter (boolean)
- Frame rate determined by thermal camera hardware

## Verification

### RecordingSettingsValidator
**Location**: `app/src/main/java/mpdc4gsr/feature/settings/data/RecordingSettingsValidator.kt`

Utility class that:
- Validates and logs settings at recording start
- Verifies settings are applied correctly
- Provides detailed logging for debugging

### Logs
When recording starts, check logcat for:
```
RecordingSettingsValidator: ========== Recording Settings Validation ==========
RecordingSettingsValidator: Auto Recording: false
RecordingSettingsValidator: Recording Quality: High
RecordingSettingsValidator: Video Frame Rate: 30 fps
RecordingSettingsValidator: Audio Enabled: true
...
RgbCameraRecorder: Recording config from settings: 1920x1080@30fps, audio=true
MainRecordingController: Starting recording with settings: simultaneousRecording=true
```

## Testing

### Unit Tests
- `RecordingSettingsRepositoryTest.kt`: Tests quality configurations and settings persistence

### Manual Testing
1. Open RecordingSettingsScreen
2. Modify settings (Quality, Frame Rate, Audio)
3. Start recording
4. Check logcat for settings validation logs
5. Verify video file properties match settings

## Future Enhancements

- [ ] Add thermal frame rate configuration
- [ ] Implement timestamp sync logic
- [ ] Add auto-recording feature
- [ ] Configure sensor data format (CSV vs other formats)
- [ ] Add settings export/import for backup
