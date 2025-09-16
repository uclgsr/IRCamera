# File I/O and Session Management Improvements

This document describes the enhanced data writing, file I/O, and session management system implemented to address the requirements in the problem statement.

## Overview

The improvements provide a comprehensive solution for:
- Standardized session folder structure and naming conventions
- Enhanced file naming with consistent patterns across all sensors
- Improved write performance with buffered I/O for high-volume data
- Robust session cleanup and failed session management
- Storage monitoring and low-space handling
- Better session metadata with comprehensive tracking

## Session Directory Structure

### Standardized Format
```
sessions/
└── {sessionId}/
    ├── session_metadata.json
    ├── sync_markers.csv
    ├── RGB/
    │   └── rgb_video.mp4
    ├── Thermal/
    │   ├── thermal_frames.csv
    │   ├── thermal_metadata_data.csv
    │   └── thermal_calibration.json
    └── Shimmer/
        └── shimmer_data.csv
```

### Session ID Format
Enhanced session IDs follow the pattern: `yyyyMMdd_HHmmss_SSS_deviceModel_uuid`

Examples:
- `20231201_143022_456_SM_G998B_a1b2c3d4`
- `20231201_143022_456_Pixel7_e5f6g7h8`

This format provides:
- **Chronological sorting** with date/time prefix
- **Millisecond precision** for unique identification
- **Device identification** for multi-device studies
- **UUID suffix** to prevent conflicts

## File Naming Conventions

### Standard File Names (Constants in SessionDirectoryManager)
- **RGB Video**: `rgb_video.mp4`
- **Shimmer Data**: `shimmer_data.csv`
- **Thermal Frames**: `thermal_frames.raw` (or `.csv` for readable format)
- **Thermal Metadata**: `thermal_metadata.csv`
- **Session Metadata**: `session_metadata.json`
- **Sync Markers**: `sync_markers.csv`

### Session Metadata Format
```json
{
  "session_id": "20231201_143022_456_SM_G998B_a1b2c3d4",
  "start_time": 1701434622456,
  "end_time": 1701434922789,
  "duration_ms": 300333,
  "device_model": "SM-G998B",
  "device_manufacturer": "samsung",
  "app_version": "2.1.0",
  "enabled_sensors": ["RGB", "Thermal", "Shimmer"],
  "participant_id": "P001",
  "study_name": "Stress Response Study",
  "status": "COMPLETED",
  "files": {
    "rgb_video": {
      "exists": true,
      "size_bytes": 52428800,
      "path": "/storage/.../RGB/rgb_video.mp4"
    },
    "shimmer_data": {
      "exists": true,
      "size_bytes": 1048576,
      "path": "/storage/.../Shimmer/shimmer_data.csv"
    },
    "thermal_frames": {
      "exists": true,
      "size_bytes": 10485760,
      "path": "/storage/.../Thermal/thermal_frames.csv"
    }
  },
  "errors": {
    "thermal_sensor": "Connection timeout after 30s"
  },
  "metadata": {
    "custom_field": "custom_value"
  }
}
```

## Improved Write Performance

### BufferedDataWriter Class
High-performance data writer with:
- **Configurable buffer sizes** (default 8KB)
- **Periodic flushing** (default 1000ms)
- **Non-blocking queue-based writes** (max 10K items)
- **Proper resource cleanup** with coroutine management
- **Write statistics** and performance monitoring

#### Usage Example
```kotlin
val writer = BufferedDataWriter(
    outputFile = File(sessionDir, "high_volume_data.csv"),
    bufferSize = 16384,           // 16KB buffer
    flushIntervalMs = 500L,       // Flush every 500ms
    maxQueueSize = 5000           // Queue up to 5000 lines
)

writer.start()
writer.writeLine("timestamp,value1,value2")
writer.writeLine("1701434622456,123.45,678.90")
writer.stop() // Ensures all data is written and files are closed
```

### CSVBufferedWriter Class
Specialized CSV writer with:
- **Header management** with automatic writing
- **CSV formatting** with proper escaping
- **Type-safe row writing** with automatic conversion

#### Usage Example
```kotlin
val csvWriter = CSVBufferedWriter(
    outputFile = File(sessionDir, "sensor_data.csv"),
    headers = listOf("timestamp", "sensor_value", "quality"),
    flushIntervalMs = 1000L
)

csvWriter.startWithHeaders()
csvWriter.writeRow(listOf(System.nanoTime(), 123.45, "good"))
csvWriter.stop()
```

## Storage Management

### Storage Monitoring
The system continuously monitors storage space:
- **Minimum threshold**: 500MB free space required
- **Warning threshold**: 1GB free space triggers warning
- **Pre-recording checks**: Storage validated before starting sessions
- **Low storage handling**: Graceful degradation with user notification

### Storage Status Information
```kotlin
data class StorageStatus(
    val availableMB: Long,        // Available space in MB
    val totalMB: Long,            // Total space in MB  
    val isLowStorage: Boolean,    // Below 500MB threshold
    val shouldWarn: Boolean       // Below 1GB threshold
) {
    val usagePercentage: Int      // 0-100% usage
    val formattedAvailable: String // "1.5 GB" or "512 MB"
}
```

## Session Cleanup and Management

### Failed Session Detection
Sessions are marked as failed when:
- No metadata file exists and total data < 1KB
- Status is "FAILED" or "ERROR" with no significant data files (>10KB)
- All sensors failed to start during session creation

### Automatic Cleanup
- **On app startup**: Failed sessions are automatically removed
- **User-initiated**: Manual cleanup via SessionManagerActivity
- **Safe deletion**: Only removes sessions meeting failure criteria
- **Confirmation required**: User must confirm deletion of valid sessions

### Session Manager Enhancements
- **Storage information display** in subtitle bar
- **Low storage warnings** with Toast notifications
- **Cleanup statistics** showing number of sessions removed
- **Enhanced session filtering** by status and data type

## Enhanced Recording Controller API

### New startRecording Method
```kotlin
suspend fun startRecording(
    sessionId: String? = null,               // Optional custom session ID
    participantId: String? = null,           // Participant identifier
    studyName: String? = null,               // Study name for organization
    enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer")
): Boolean
```

### Storage Integration
- **Pre-recording validation**: Checks available storage before starting
- **Error handling**: Proper cleanup on storage or sensor failures
- **Metadata tracking**: Comprehensive session status and error logging
- **Sensor failure handling**: Partial recording support with detailed error tracking

## Sensor-Specific Improvements

### Thermal Camera Recorder
- **Buffered CSV writing** for thermal summary data (500ms flush)
- **High-performance frame data** writing (16KB buffer, 1s flush)
- **Standard file paths** using SessionDirectoryManager
- **Optimized temperature matrix** serialization

#### Thermal Data Files
1. **thermal_metadata_data.csv**: Summary data per frame
   ```csv
   timestamp_ns,frame_number,min_temp_c,max_temp_c,avg_temp_c,center_temp_c,ambient_temp_c,emissivity,reflected_temp_c
   1701434622456000000,1,18.5,42.3,25.7,23.2,25.0,0.95,23.0
   ```

2. **thermal_frames.csv**: Full temperature matrix per frame
   ```csv
   timestamp_ns,frame_number,temp_0,temp_1,...,temp_49151
   1701434622456000000,1,18.5,18.7,...,42.3
   ```

### Future Sensor Updates
The BufferedDataWriter and SessionDirectoryManager are designed for easy integration with:
- **RGB Camera Recorder**: Video metadata and frame extraction logs
- **Shimmer GSR Recorder**: High-frequency GSR and PPG data
- **Additional sensors**: Extensible design for new sensor types

## Testing and Validation

### Comprehensive Test Suite
- **SessionDirectoryManagerTest**: 15+ test cases covering all functionality
- **Storage validation**: Free space calculation and formatting
- **Session lifecycle**: Creation, update, and cleanup testing
- **Failed session detection**: Edge cases and cleanup validation
- **File path standardization**: Sensor-specific directory mapping

### Integration Testing Scenarios
1. **Back-to-back sessions**: Start/stop/start cycles
2. **Storage exhaustion**: Low storage handling
3. **Sensor failures**: Partial session cleanup
4. **Power interruption**: Failed session detection and cleanup
5. **Multi-device testing**: Session ID uniqueness across devices

## Performance Optimizations

### Write Performance
- **Reduced I/O overhead**: Buffered writing vs. direct file writes  
- **Non-blocking operations**: Queue-based architecture prevents UI blocking
- **Configurable flush intervals**: Balance between data safety and performance
- **Bulk write operations**: Batch processing for high-volume data

### Memory Management
- **Fixed-size buffers**: Prevent memory leaks with bounded queues
- **Coroutine-based cleanup**: Proper resource management
- **Lazy initialization**: Resources allocated only when needed
- **Write statistics**: Monitor performance and detect issues

## Migration Guide

### For Existing Code
1. **Replace direct File operations** with SessionDirectoryManager
2. **Use BufferedDataWriter** for high-volume CSV data
3. **Update session creation** to use new RecordingController API
4. **Add storage checks** before starting recordings
5. **Implement proper cleanup** in sensor stop methods

### Example Migration
**Before:**
```kotlin
val sessionDir = File(filesDir, "session_${timestamp}")
sessionDir.mkdirs()
val csvFile = File(sessionDir, "data.csv")
val writer = FileWriter(csvFile)
writer.write("header\n")
writer.write("data\n")
writer.close()
```

**After:**
```kotlin
val sessionManager = SessionDirectoryManager(context)
val sessionId = sessionManager.generateSessionId()
val sessionDir = sessionManager.createSessionDirectory(sessionId)
val csvWriter = CSVBufferedWriter(
    File(sessionDir.rootDir, "data.csv"),
    listOf("header")
)
csvWriter.startWithHeaders()
csvWriter.writeRow(listOf("data"))
csvWriter.stop()
```

## Configuration Options

### SessionDirectoryManager Settings
```kotlin
companion object {
    // Storage thresholds
    private const val MIN_FREE_SPACE_MB = 500L
    private const val WARNING_FREE_SPACE_MB = 1000L
    
    // Standard subdirectory names
    private const val RGB_SUBDIR = "RGB"
    private const val THERMAL_SUBDIR = "Thermal"
    private const val SHIMMER_SUBDIR = "Shimmer"
    
    // Standard file names
    const val RGB_VIDEO_FILE = "rgb_video.mp4"
    const val SHIMMER_DATA_FILE = "shimmer_data.csv"
    const val THERMAL_FRAMES_FILE = "thermal_frames.raw"
    const val SESSION_METADATA_FILE = "session_metadata.json"
}
```

### BufferedDataWriter Settings
```kotlin
// Configurable parameters
val writer = BufferedDataWriter(
    outputFile = file,
    bufferSize = 8192,        // 8KB default buffer
    flushIntervalMs = 1000L,  // 1 second flush interval
    maxQueueSize = 10000      // 10K item queue limit
)
```

This enhanced file I/O system provides a robust, performant, and maintainable foundation for multi-modal sensor data recording with proper session management and cleanup capabilities.
