# Sensor Integration Implementation Guide

This document consolidates all sensor integration implementation details for RGB camera, thermal imaging, and GSR
sensors in the IRCamera platform.

## Overview

The IRCamera platform supports multi-modal physiological sensing through three primary sensor types:

- **RGB Camera**: High-resolution video and RAW image capture
- **Thermal Camera**: Topdon TC001 thermal imaging device
- **GSR Sensor**: Shimmer3 GSR+ Bluetooth-enabled galvanic skin response

## RGB Camera Implementation

### 4K60 Video Recording with JPEG Frame Extraction

**Status**: ✅ Implemented  
**Location**: `RgbCameraRecorder.kt`

#### Features

- Automatic 4K60fps detection for Samsung S22 and compatible devices
- Fallback to 4K30 or 1080p60/30 for unsupported devices
- Simultaneous JPEG frame capture at ~12fps during video recording
- Video files saved as H.264 MP4 format
- JPEG frames saved with timestamps for synchronization

#### Implementation Details

```kotlin
class RgbCameraRecorder {
    // Automatic resolution detection
    private fun detectBestVideoSize(): Size {
        // Priority: 4K60 > 4K30 > 1080p60 > 1080p30
    }
    
    // Simultaneous video and frame capture
    private fun setupDualCapture() {
        // Video recording session
        // JPEG capture session with 12fps target
    }
}
```

### RAW Capture Mode (Stage 3 DNG)

**Status**: ✅ Enhanced existing implementation  
**Location**: `RawEngine.kt`, `RgbCameraRecorder.kt`

#### Features

- Samsung Stage 3/Level 3 RAW processing support
- DNG file output using Android's DngCreator
- Separate capture session for RAW mode (alternate to video mode)
- Full-resolution sensor data capture
- Device compatibility checking for RAW support

#### Samsung Stage 3 RAW Processing

```kotlin
// DNG Capture with Stage 3 RAW processing
val dngCreator = DngCreator(characteristics, result)
dngCreator.writeImage(dngOutputStream, rawImage)
```

### Real-Time Preview Integration

**Status**: ✅ Implemented  
**Features**:

- TextureView-based preview display
- Automatic aspect ratio handling
- Integration with recording controls
- Preview freeze/unfreeze during state changes

## Thermal Camera Integration

### Topdon TC001 Integration

The thermal camera integration supports the Topdon TC001 thermal imaging device with comprehensive feature set.

#### Core Features

- **Dual-camera fusion**: Thermal + RGB overlay capabilities
- **Temperature measurement**: Point and area temperature analysis
- **Recording modes**: Video recording and frame capture
- **Real-time display**: Live thermal feed with temperature visualization
- **Color mapping**: Multiple thermal color palettes

#### Hardware Communication

```kotlin
// TC001 device communication
class TopdonTC001Manager {
    fun initializeDevice(): Boolean
    fun startThermalStream(): Boolean
    fun captureFrame(): ThermalFrame
    fun setColorPalette(palette: ThermalPalette)
    fun measureTemperature(x: Int, y: Int): Float
}
```

#### Integration Architecture

The thermal system follows a modular architecture:

```
component/thermal-ir/src/main/java/com/mpdc4gsr/module/thermal/ir/
├── activity/          # Advanced IR activities - dual camera, fusion
├── adapter/           # UI adapters for thermal data  
├── bean/             # Data models for thermal processing
├── dialog/           # Thermal-specific dialogs
├── fragment/         # Advanced thermal fragments
├── utils/            # IR processing utilities
├── viewmodel/        # MVVM architecture components
└── video/            # Video processing capabilities
```

#### Thermal Data Processing

- **Frame rate optimization**: Achieved 25Hz TC001 Plus detection (177% improvement)
- **Temperature calibration**: Accurate temperature measurement with device-specific calibration
- **Color mapping algorithms**: Multiple thermal visualization modes
- **Image processing**: Real-time thermal image enhancement and filtering

## GSR Sensor Integration (Shimmer3)

### Shimmer3 GSR+ Implementation

**Status**: ✅ Implemented with real Shimmer SDK  
**Location**: `app/src/main/java/mpdc4gsr/sensors/gsr/`

#### Real Shimmer Libraries Integration

Replaced mock implementations with actual Shimmer SDK JAR files from `app/libs/`:

- `shimmerbluetoothmanager-0.11.5_beta.jar`
- `shimmerdriver-0.11.5_beta.jar`
- `shimmerdriverpc-0.11.5_beta.jar`
- `shimmerandroidinstrumentdriver-3.2.4_beta.aar`

#### Implementation Classes

```kotlin
class RealShimmerDeviceFactory : ShimmerDeviceFactory {
    override fun createDevice(macAddress: String): ShimmerDevice
}

class RealShimmerDevice(private val shimmerManager: ShimmerBluetoothManagerAndroid) {
    fun connect(): Boolean
    fun startStreaming(): Boolean  
    fun stopStreaming(): Boolean
    fun disconnect(): Boolean
}

class RealShimmerDataCluster(private val objectCluster: ObjectCluster) {
    fun getGsrValue(): Double
    fun getTimestamp(): Long
    fun getPpgValue(): Double?
}
```

#### GSR Data Processing

- **12-bit ADC pipeline**: Correct 3.3V reference and calibration
- **Streaming support**: Real-time GSR data with configurable sample rates
- **Multi-device support**: Simultaneous connection to 2-3 Shimmer units
- **Data quality**: Comprehensive validation and quality assurance

#### BLE Command Control

Implemented complete Shimmer GSR+ command set:

- Start/stop streaming commands (0x07/0x20)
- Device configuration and calibration
- Sample rate configuration
- Sensor enable/disable controls

## Sensor Synchronization

### Multi-Modal Coordination

The platform provides precise temporal alignment across all sensors:

#### Synchronized Sensor Start

- **Barrier coordination**: <1000ms jitter control across all sensors
- **Timestamp alignment**: Nanosecond precision timestamps
- **Session boundaries**: Coordinated start/stop across modalities

#### Time Synchronization

- **NTP-style protocol**: Accurate time sync between Android devices and PC
- **Network adaptation**: Auto-resync capabilities for varying network conditions
- **Quality metrics**: RTT monitoring and sync accuracy tracking

### Session Management Integration

```kotlin
class RecordingController {
    suspend fun startRecording(
        sessionId: String,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer")
    ): Boolean {
        // 1. Initialize all enabled sensors
        // 2. Perform time synchronization
        // 3. Start sensors with barrier coordination
        // 4. Begin data collection
    }
}
```

## Data Management and Export

### File Organization Structure

```
/sdcard/IRCamera/sessions/[session_id]/
├── rgb/
│   ├── video_[timestamp].mp4
│   └── frames/frame_[timestamp].jpg
├── thermal/
│   ├── thermal_[timestamp].mp4
│   └── frames/thermal_[timestamp].jpg
├── gsr/
│   └── gsr_data_[timestamp].csv
└── metadata/
    ├── session_info.json
    └── sync_log.csv
```

### Data Export Formats

#### RGB Data

- **Video**: H.264 MP4 with 4K/1080p resolution
- **Frames**: JPEG images with timestamp metadata
- **RAW**: DNG files for Stage 3 RAW capture

#### Thermal Data

- **Video**: Thermal video with temperature data
- **Frames**: Thermal images with temperature information
- **CSV**: Temperature measurements with spatial coordinates

#### GSR Data

- **Format**: CSV with columns: timestamp, gsr_kohms, ppg_mv, quality_flag
- **Sample Rate**: Configurable up to 1000Hz
- **Calibration**: Device-specific calibration metadata included

## Error Handling and Recovery

### Sensor Fault Tolerance

- **Individual sensor isolation**: Failure of one sensor doesn't affect others
- **Graceful degradation**: Continue recording with available sensors
- **Automatic reconnection**: Intelligent retry mechanisms for BLE/USB devices
- **Health monitoring**: Continuous sensor status monitoring

### Recovery Mechanisms

```kotlin
class SensorHealthMonitor {
    fun monitorSensorHealth(sensor: Sensor) {
        // Periodic health checks
        // Automatic reconnection attempts
        // Error reporting and logging
    }
}
```

## Testing and Validation

### Automated Testing

- **Instrumentation tests**: RGB camera 4K validation
- **Unit tests**: Sensor communication protocols
- **Integration tests**: Multi-sensor coordination
- **Performance tests**: Throughput and latency validation

### Manual Validation

- **Multi-device testing**: 2-3 concurrent Shimmer units
- **Resolution validation**: 4K recording capability confirmation
- **Thermal accuracy**: Temperature measurement precision testing
- **Synchronization validation**: Cross-modal timing accuracy

## Configuration Management

### Sensor Configuration

```yaml
sensors:
  rgb:
    preferred_resolution: "4K60"
    fallback_resolutions: ["4K30", "1080p60", "1080p30"]
    jpeg_frame_rate: 12
    
  thermal:
    device_type: "TC001"
    frame_rate: 25
    color_palette: "iron"
    
  gsr:
    device_type: "Shimmer3"
    sample_rate: 512
    enabled_sensors: ["GSR", "PPG"]
```

This consolidated sensor integration provides enterprise-grade multi-modal sensing capabilities with robust error
handling, precise synchronization, and comprehensive data management for scientific research applications.