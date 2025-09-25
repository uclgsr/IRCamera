# Samsung Stage3/Level3 RAW DNG Recording

This document describes the Samsung Stage3/Level3 RAW DNG recording feature implementation for the IRCamera multi-modal
sensing platform.

## Overview

The Samsung Stage3/Level3 RAW DNG recording feature enables high-quality RAW image capture directly from Samsung's image
processing pipeline at the Stage3/Level3 level. This provides maximum raw data preservation with minimal processing for
scientific applications and machine learning research.

## Technical Implementation

### Stage3/Level3 Processing Pipeline

Samsung's image processing pipeline consists of multiple stages:

- **Stage 1**: Basic sensor readout and defect correction
- **Stage 2**: Lens shading correction and black level subtraction
- **Stage 3/Level 3**: Advanced ISP processing with noise reduction, edge enhancement, and color correction

Our implementation accesses the Stage3/Level3 data before heavy processing is applied, providing:

- Maximum dynamic range preservation
- Minimal noise reduction interference
- Raw color data suitable for scientific analysis
- Full 12-bit sensor data retention

### Key Features

#### DNG Creation

- Uses Android's `DngCreator` API for proper Adobe DNG 1.4 format compliance
- Embeds full camera characteristics metadata
- Includes capture result information for post-processing
- Maintains compatibility with standard RAW processing software

#### Samsung-Specific Optimizations

```kotlin
// Disable automatic processing for Stage3/Level3 capture
set(CaptureRequest.CONTROL_MODE, CONTROL_MODE_OFF)
set(CaptureRequest.NOISE_REDUCTION_MODE, NOISE_REDUCTION_MODE_OFF)
set(CaptureRequest.EDGE_MODE, EDGE_MODE_OFF)
set(CaptureRequest.COLOR_CORRECTION_MODE, COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
set(CaptureRequest.TONEMAP_MODE, TONEMAP_MODE_CONTRAST_CURVE)
```

#### File Naming Convention

Stage3/Level3 DNG files use the naming pattern:

```
${sessionId}_raw_stage3_${timestamp}.dng
```

This allows easy identification of Stage3/Level3 processed files.

## Usage

### Enabling Stage3/Level3 Processing

1. **Via Camera Settings UI**: Toggle the "Samsung Stage3/Level3 RAW" switch in camera settings
2. **Programmatically**:
   ```kotlin
   camera2System.configureStage3Processing(true)
   ```

### Device Compatibility

The feature is automatically enabled on supported Samsung devices:

- Samsung Galaxy S22 series (SM-S901B, SM-S906B, SM-S908B)
- Samsung Galaxy S23 series (SM-S911B, SM-S916B, SM-S918B)
- Other Samsung devices with RAW capability

### File Output

When Stage3/Level3 processing is enabled:

- **Success**: Creates proper DNG file with Samsung Stage3/Level3 metadata
- **Fallback**: Creates raw binary file if DNG creation fails
- **Error handling**: Logs detailed error information and attempts graceful degradation

## API Reference

### RawEngine Class

#### Methods

- `setStage3ProcessingEnabled(enabled: Boolean)`: Enable/disable Stage3/Level3 processing
- `isStage3ProcessingEnabled(): Boolean`: Check current processing mode
- `setup(..., enableStage3: Boolean)`: Configure engine with Stage3/Level3 support

### Camera2System Class

#### Methods

- `configureStage3Processing(enabled: Boolean)`: Configure Samsung Stage3/Level3 processing
- `isStage3ProcessingEnabled(): Boolean`: Check if Stage3/Level3 processing is enabled

### CameraSettingsView Class

#### Methods

- `setStage3ProcessingEnabled(enabled: Boolean)`: Set UI toggle state
- `setStage3ProcessingVisible(visible: Boolean)`: Show/hide Stage3/Level3 toggle

## Integration Example

```kotlin
// Configure Camera2 system for Stage3/Level3 processing
val camera2System = Camera2System(context, textureView)

// Enable Stage3/Level3 processing
camera2System.configureStage3Processing(true)

// Set up camera settings UI
cameraSettingsView.setStage3ProcessingVisible(true)
cameraSettingsView.onStage3ProcessingToggle = { enabled ->
    camera2System.configureStage3Processing(enabled)
}

// Start RAW capture with Stage3/Level3 processing
camera2System.startRecording()
```

## Troubleshooting

### Common Issues

1. **DNG Creation Fails**: Falls back to raw binary automatically
2. **Stage3/Level3 Not Available**: Check device compatibility
3. **Metadata Missing**: Ensure camera characteristics are available

### Logs and Debugging

Enable debug logging to monitor Stage3/Level3 processing:

```
adb logcat | grep -E "(RawEngine|Camera2System)" | grep -i stage3
```

## Performance Considerations

- Stage3/Level3 processing may increase capture time
- DNG files are larger than JPEG but smaller than uncompressed RAW
- Memory usage increases with DNG metadata embedding
- Consider storage space requirements for extended recording sessions

## Future Enhancements

- Samsung Camera SDK integration for advanced features
- Hardware-specific optimizations for different Samsung chipsets
- Extended metadata embedding for research applications
- Integration with Samsung Expert RAW APIs when available