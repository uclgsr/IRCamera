# Recording Settings Verification Guide

## Summary
This PR implements recording settings integration to ensure that user preferences in the Recording Settings screen actually affect the recording behavior and sensor configuration.

## Changes Made

### 1. RecordingSettingsRepository (NEW)
**File**: `app/src/main/java/mpdc4gsr/feature/settings/data/RecordingSettingsRepository.kt`

- Centralized repository for recording settings using Repository Pattern
- Singleton pattern for global access
- Manages SharedPreferences persistence
- Provides quality configuration mapping:
  - Ultra: 4K (3840x2160) @ 60fps, 50Mbps
  - High: 1080p (1920x1080) @ 30fps, 20Mbps
  - Medium: 720p (1280x720) @ 30fps, 10Mbps
  - Low: 480p (854x480) @ 24fps, 5Mbps

### 2. RecordingSettingsViewModel (UPDATED)
**File**: `app/src/main/java/mpdc4gsr/feature/settings/presentation/RecordingSettingsViewModel.kt`

- Now uses RecordingSettingsRepository instead of direct SharedPreferences
- Maintains reactive StateFlow from repository
- Cleaner separation of concerns

### 3. RgbCameraRecorder (UPDATED)
**File**: `app/src/main/java/mpdc4gsr/core/data/RgbCameraRecorder.kt`

**Changes**:
- Loads recording settings in `initialize()` method
- Applies quality settings in `optimizeVideoConfiguration()`:
  - Uses quality config from repository
  - Respects user's frame rate preference
  - Falls back to device capabilities when needed
- Applies audio settings in video preparation:
  - Checks audioEnabled setting before enabling audio
  - Logs audio configuration
- Added logging for settings validation

**Lines Changed**:
- Added constants for 720p and 480p resolutions
- Line 222: Load settings during initialization
- Line 533-570: Apply quality and FPS settings
- Line 889-912: Add settings validation logging
- Line 1074-1085: Apply audio settings

### 4. MainRecordingController (UPDATED)
**File**: `app/src/main/java/mpdc4gsr/feature/network/data/MainRecordingController.kt`

**Changes**:
- Added RecordingSettingsRepository instance
- Reads simultaneousRecording setting
- Starts sensors simultaneously or sequentially based on setting
- Added delay between sensors when sequential mode is enabled
- Logs settings at recording start

**Lines Changed**:
- Line 9: Added delay import
- Line 54: Added repository instance
- Line 60-146: Apply simultaneousRecording setting

### 5. RecordingSettingsValidator (NEW)
**File**: `app/src/main/java/mpdc4gsr/feature/settings/data/RecordingSettingsValidator.kt`

- Utility object for validating settings application
- Provides detailed logging at recording start
- Can verify settings were applied correctly
- Helps debug configuration issues

### 6. RecordingSettingsRepositoryTest (NEW)
**File**: `app/src/test/java/mpdc4gsr/feature/settings/data/RecordingSettingsRepositoryTest.kt`

- Unit tests for repository quality configurations
- Tests default settings
- Tests quality config for all quality levels

### 7. Documentation (NEW)
**File**: `docs/RECORDING_SETTINGS_INTEGRATION.md`

- Comprehensive documentation of settings architecture
- Explains settings flow from UI to sensors
- Details which settings apply to which sensors
- Provides debugging guide with log examples

## How to Verify Settings Work

### Method 1: Check Logs
1. Open the app and navigate to Settings > Recording Settings
2. Change settings:
   - Set Recording Quality to "Low" or "High"
   - Set Video Frame Rate to 15 or 60
   - Toggle Audio Recording on/off
   - Toggle Simultaneous Recording
3. Navigate to recording screen and start recording
4. Check logcat for these log tags:
   ```
   RecordingSettingsValidator
   RgbCameraRecorder
   MainRecordingController
   ```
5. Look for logs showing:
   ```
   RecordingSettingsValidator: Recording Quality: High
   RecordingSettingsValidator: Video Frame Rate: 30 fps
   RecordingSettingsValidator: Audio Enabled: true
   RgbCameraRecorder: Recording config from settings: 1920x1080@30fps, audio=true
   MainRecordingController: Starting recording with settings: simultaneousRecording=true
   ```

### Method 2: Verify Video File Properties
1. Change Recording Quality setting
2. Record a short video
3. Check video file properties:
   - Low quality: Should be ~854x480 resolution
   - Medium quality: Should be ~1280x720 resolution
   - High quality: Should be ~1920x1080 resolution
   - Ultra quality: Should be ~3840x2160 resolution
4. Check audio track:
   - If Audio Recording was ON: Video should have audio track
   - If Audio Recording was OFF: Video should have NO audio track

### Method 3: Check Session Metadata
1. Start recording with specific settings
2. Stop recording
3. Navigate to session directory
4. Open CSV files and check metadata headers
5. Verify recording_config includes correct resolution and fps

## Expected Behavior

### Before Changes
- Recording settings were displayed but not applied
- All recordings used hardcoded defaults
- No way to control quality, frame rate, or audio

### After Changes
- Recording Quality setting controls video resolution and bitrate
- Video Frame Rate setting controls recording FPS (within device limits)
- Audio Enabled setting controls audio recording
- Simultaneous Recording controls sensor start timing
- All settings are logged for verification
- Settings persist across app restarts

## Settings That Are Now Applied

✅ **Recording Quality** - Controls video resolution and bitrate
✅ **Video Frame Rate** - Controls recording FPS
✅ **Audio Recording** - Controls audio in video
✅ **Simultaneous Recording** - Controls sensor start timing

## Settings Planned for Future

⏳ **Auto Recording** - Automatically start when devices connect
⏳ **Timestamp Synchronization** - Enhanced sync logic
⏳ **Video Format** - Support different codecs
⏳ **Audio Format** - Support different audio codecs
⏳ **Sensor Data Format** - Support formats beyond CSV

## Sensor-Specific Notes

### RGB Camera
- Fully integrated with recording settings
- Quality, FPS, and audio settings all applied
- Device capabilities still respected (can't force 4K on non-4K device)

### Thermal Camera
- No user-configurable settings at this time
- Frame rate determined by thermal camera hardware
- saveImages parameter controlled programmatically

### GSR Sensor
- Has its own settings repository (GSRSettingsRepository)
- Sampling rate and device settings managed separately
- No integration needed with main recording settings

## Testing Checklist

- [ ] Settings are persisted across app restarts
- [ ] Quality setting changes video resolution
- [ ] Frame rate setting changes video FPS (within limits)
- [ ] Audio toggle works correctly
- [ ] Simultaneous recording toggle affects timing
- [ ] Logs show settings being loaded and applied
- [ ] Video files match expected properties
- [ ] Settings work on different devices (4K vs non-4K)

## Known Limitations

1. Device capabilities override settings (can't force 4K on non-4K device)
2. Frame rate limited by device hardware (some devices can't do 60fps)
3. Audio requires RECORD_AUDIO permission
4. Some settings planned but not yet implemented (see Future section above)

## Debugging Tips

If settings don't seem to apply:

1. Check logcat for "RecordingSettingsValidator" logs
2. Verify SharedPreferences are being saved (check with Device File Explorer)
3. Ensure app has necessary permissions (camera, audio, storage)
4. Check device capabilities (4K, 60fps support)
5. Look for error logs in RgbCameraRecorder initialization

## Code Review Focus Areas

1. **RecordingSettingsRepository**: Singleton pattern and SharedPreferences usage
2. **RgbCameraRecorder**: Settings application in optimizeVideoConfiguration
3. **MainRecordingController**: Simultaneous vs sequential sensor starting
4. **Logging**: Comprehensive logging for debugging
5. **Tests**: Unit test coverage for repository

## Related Files

- RecordingSettingsScreen.kt - UI for settings
- SessionMetadata.kt - Session data with sync events
- GSRSettingsRepository.kt - GSR-specific settings
- RECORDING_SETTINGS_INTEGRATION.md - Technical documentation
