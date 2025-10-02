# SDK Integration Enhancement: Topdon and Shimmer SDK Full Utilization

## Overview

This document describes the enhanced integration of Topdon TC001 (Thermal Camera) and Shimmer3 GSR+ SDKs to ensure full
utilization of their capabilities through user-configurable settings.

## Shimmer3 GSR+ SDK Integration

### SDK Capabilities Utilized

#### 1. **Configurable Sampling Rate**

**File**: `app/src/main/java/mpdc4gsr/feature/gsr/data/GSRSensorRecorder.kt`

The Shimmer SDK supports sampling rates from 1 Hz to 512 Hz. We now dynamically configure this based on
GSRSettingsRepository:

```kotlin
// Load GSR settings from repository
gsrSettingsRepository = GSRSettingsRepository(context)
val gsrSettings = gsrSettingsRepository?.gsrSettings?.value

// Apply sampling rate with validation
effectiveSamplingRate = gsrSettings?.samplingRate?.toDouble() ?: samplingRateHz.toDouble()
effectiveSamplingRate = effectiveSamplingRate.coerceIn(SHIMMER_MIN_SAMPLING_RATE, SHIMMER_MAX_SAMPLING_RATE)

// Configure Shimmer device
device.setSamplingRateShimmer(effectiveSamplingRate)
```

**Benefits**:

- Users can configure sampling rate from 1-512 Hz via GSRSettingsScreen
- Higher sampling rates provide better temporal resolution for GSR measurements
- Lower sampling rates reduce power consumption and data storage
- Sampling rate is validated against Shimmer SDK limits

#### 2. **Enhanced Data Processing**

The integration uses:

- ObjectCluster processing for structured data extraction
- Real-time GSR value calibration from RAW ADC values
- PPG (Photoplethysmography) signal extraction when available
- Resistance calculation: `resistance = 1000.0 / conductance`

#### 3. **Settings Applied from GSRSettingsRepository**

- `samplingRate`: Shimmer device sampling frequency
- `enableFiltering`: Data filtering configuration
- `bufferSize`: Data buffering for batch processing
- `enableRealTimeMonitoring`: Real-time data streaming
- `autoReconnect`: Automatic reconnection on disconnect

**Code Location**: Lines 202-240 in GSRSensorRecorder.kt

### Shimmer SDK Methods Utilized

1. **Device Configuration**:
    - `setSamplingRateShimmer(rate)`: Dynamic sampling rate configuration
    - `startStreaming()`: Start data acquisition
    - `stopStreaming()`: Stop data acquisition

2. **Data Processing**:
    - `ObjectCluster.getFormatClusterValue()`: Extract calibrated sensor values
    - Data channels: GSR, PPG, Timestamp

3. **Connection Management**:
    - Connection state callbacks
    - Automatic reconnection logic
    - Device health monitoring

## Topdon TC001 Thermal Camera SDK Integration

### SDK Capabilities Utilized

#### 1. **Configurable Frame Rate**

**File**: `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/video/VideoRecordFFmpeg.kt`

The Topdon SDK (via FFmpeg integration) now supports configurable frame rates:

```kotlin
class VideoRecordFFmpeg(
    // ... other parameters
    private var customFrameRate: Int = 25,
    private var customBitrate: Int = 1500000
) : VideoRecord() {
    
    // Apply custom frame rate to recorder
    recorder!!.frameRate = customFrameRate.toDouble()
    recorder!!.videoBitrate = customBitrate
}
```

**Benefits**:

- Frame rate range: 10-30 fps (thermal camera hardware limitation)
- Lower frame rates reduce data size
- Higher frame rates capture faster thermal changes
- Bitrate automatically adjusted based on frame rate

#### 2. **ThermalSettingsRepository**

**File**: `app/src/main/java/mpdc4gsr/feature/thermal/data/ThermalSettingsRepository.kt`

New repository for thermal camera settings:

```kotlin
data class ThermalSettings(
    val frameRate: Int = 25,
    val saveRawImages: Boolean = false,
    val palette: String = "Iron",
    val temperatureUnit: String = "Celsius",
    val emissivity: Float = 0.95f,
    val autoScale: Boolean = true,
    val showCrosshair: Boolean = true,
    val temperatureRange: String = "Auto"
)
```

**Settings Applied**:

- `frameRate`: Video recording frame rate (10-30 fps)
- `saveRawImages`: Save individual thermal frames as images
- `palette`: Color mapping for thermal visualization
- `temperatureUnit`: Celsius/Fahrenheit/Kelvin
- `emissivity`: Material emissivity correction (0.1-1.0)
- `autoScale`: Automatic temperature scale adjustment
- `showCrosshair`: Display center point crosshair
- `temperatureRange`: Temperature measurement range

**Code Location**: Lines 86-87, 243-248 in VideoRecordFFmpeg.kt

#### 3. **Thermal Video Configuration**

The repository provides optimized video configurations:

```kotlin
fun getThermalVideoConfig(): ThermalVideoConfig {
    val settings = getSettings()
    val frameRate = settings.frameRate.coerceIn(10, 30)
    
    // Automatic bitrate adjustment
    val bitrate = when {
        frameRate <= 15 -> 800_000    // 800 kbps for low FPS
        frameRate <= 25 -> 1_500_000  // 1.5 Mbps for medium FPS
        else -> 2_000_000             // 2 Mbps for high FPS
    }
    
    return ThermalVideoConfig(frameRate, bitrate)
}
```

### Topdon SDK Methods Utilized

1. **Video Recording**:
    - `FFmpegFrameRecorder`: Video encoding with custom parameters
    - Frame rate configuration: `recorder.frameRate`
    - Bitrate configuration: `recorder.videoBitrate`
    - Codec selection: H.264/MPEG4

2. **Temperature Processing**:
    - Raw thermal data extraction
    - Temperature statistics (min, avg, max)
    - Pixel-level temperature analysis

## Integration with Recording Settings

### Cross-Module Integration

The recording settings now integrate with both SDKs:

**RecordingSettingsRepository** (main settings):

- Video quality, frame rate, audio settings
- Applied to RGB camera

**GSRSettingsRepository** (GSR-specific):

- Sampling rate, filtering, buffering
- Applied to Shimmer3 GSR sensor

**ThermalSettingsRepository** (thermal-specific):

- Frame rate, palette, emissivity
- Applied to Topdon thermal camera

### Settings Priority

1. **GSR Sensor**: GSRSettingsRepository takes precedence
    - Sampling rate from GSRSettingsRepository.samplingRate
    - Falls back to constructor parameter if not set

2. **Thermal Camera**: ThermalSettingsRepository takes precedence
    - Frame rate from ThermalSettingsRepository.frameRate
    - Falls back to default 25 fps if not set

3. **RGB Camera**: RecordingSettingsRepository settings
    - Quality, FPS, audio from RecordingSettingsRepository

## Verification

### Logs to Check

**GSR/Shimmer**:

```
GSRSensorRecorder: GSR Settings loaded: samplingRate=128Hz, filtering=true, bufferSize=1024
GSRSensorRecorder: Effective sampling rate for Shimmer: 128.0Hz (within Shimmer range: 1.0-512.0 Hz)
GSRSensorRecorder: Shimmer sampling rate configured: 128.0Hz
GSRSensorRecorder: Shimmer streaming started successfully with 128.0Hz sampling rate
```

**Thermal/Topdon**:

```
ThermalRecorder: Thermal settings loaded: frameRate=25fps, saveImages=false, palette=Iron
VideoRecordFFmpeg: Thermal video recorder configured: 25fps, 1500000bps
```

### Testing

1. **GSR Sampling Rate**:
    - Go to GSR Settings
    - Change sampling rate (e.g., 64 Hz, 128 Hz, 256 Hz)
    - Start recording
    - Check logs for applied sampling rate
    - Verify CSV file has correct sample rate

2. **Thermal Frame Rate**:
    - Configure thermal frame rate in settings
    - Start thermal recording
    - Check logs for applied frame rate
    - Verify video file frame rate matches setting

## SDK Documentation References

### Shimmer3 SDK

- **Official Docs**: ShimmerEngineering GitHub
- **Key Classes**:
    - `ShimmerDevice`: Device control
    - `ObjectCluster`: Data structure for sensor readings
    - `ShimmerBluetoothManagerAndroid`: Connection management

### Topdon TC001 SDK

- **Official Docs**: Topdon/IRCamera GitHub (reference implementation)
- **Key Components**:
    - FFmpeg integration for video encoding
    - Raw thermal data processing
    - Temperature calibration

## Future Enhancements

### Shimmer SDK

- [ ] GSR range configuration (auto/manual)
- [ ] Multiple sensor channels (GSR + PPG + accelerometer)
- [ ] Advanced filtering algorithms
- [ ] Real-time data visualization

### Topdon SDK

- [ ] Advanced color palettes
- [ ] Region of Interest (ROI) tracking
- [ ] Temperature alarms/alerts
- [ ] Multi-point temperature measurement

## Code Locations Summary

| Feature                     | File                           | Lines              |
|-----------------------------|--------------------------------|--------------------|
| GSR Sampling Rate Config    | GSRSensorRecorder.kt           | 202-240, 1642-1672 |
| GSR Settings Repository     | GSRSettingsRepository.kt       | 1-150              |
| Thermal Frame Rate Config   | VideoRecordFFmpeg.kt           | 86-87, 243-249     |
| Thermal Settings Repository | ThermalSettingsRepository.kt   | 1-132              |
| Settings Integration        | RecordingSettingsRepository.kt | All                |

## Backward Compatibility

All changes maintain backward compatibility:

- Default values used if settings not configured
- Existing code continues to work with hardcoded values
- Settings repositories are optional enhancements
- SDK methods gracefully handle missing configurations
