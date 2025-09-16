# Multi-Modal Data Synchronization Guide

## Overview

This guide explains how the IRCamera system achieves precise timestamp alignment across multiple sensor modalities (thermal camera, GSR sensor, RGB video) for research and machine learning applications.

## Synchronization Architecture

### Common Start Time Approach

The system implements a **common start time** approach where all sensor modalities reference the same session start timestamp:

1. **SessionMetadata Creation**: When recording begins, a `SessionMetadata` object captures:
   - Wall clock time (UTC milliseconds): `sessionStartTimestampMs`
   - Monotonic clock time (nanoseconds): `sessionStartMonotonicNs` 
   - Human-readable ISO timestamp: `sessionStartIso`

2. **Modality Distribution**: Each sensor recorder receives the same `SessionMetadata` object, ensuring identical timing references.

3. **Per-Sample Timestamping**: Every data sample includes:
   - **Wall clock timestamp**: Absolute UTC time for human reference
   - **Relative timestamp**: Milliseconds since session start (monotonic-based)
   - **Monotonic timestamp**: Raw monotonic nanoseconds for precise intervals

### Monotonic Clock Protection

The system uses `SystemClock.elapsedRealtimeNanos()` for measuring time intervals to avoid issues with system clock adjustments:

- **Session timing**: Monotonic clock captures session start and calculates relative timestamps
- **Interval measurement**: All duration calculations use monotonic clock differences
- **Clock drift protection**: Independent of system time changes, NTP updates, or timezone adjustments

## Data File Formats

### Session Metadata (session_metadata.json)

```json
{
  "sessionId": "Session_20241201_143022",
  "sessionStartTimestampMs": 1701432624123,
  "sessionStartMonotonicNs": 154823947123456789,
  "sessionStartIso": "2024-12-01T14:30:24.123Z",
  "recordingDurationMs": 60000,
  "modalityFiles": {
    "thermal": "thermal_stats_Session_20241201_143022.csv",
    "gsr": "gsr_data_Session_20241201_143022.csv",
    "rgb_video": "rgb_video_Session_20241201_143022.mp4"
  },
  "syncEvents": [
    {
      "eventType": "session_start",
      "timestampMs": 1701432624123,
      "monotonicOffsetNs": 0
    }
  ]
}
```

### Thermal Data CSV Format

```csv
# Multi-Modal Recording Session Timing Information
# Session ID: Session_20241201_143022
# Session Start: 2024-12-01T14:30:24.123Z (1701432624123ms UTC)
# Monotonic Start: 154823947123456789ns
# Timing Source: android_monotonic_realtime
#
# Timestamps in this file are:
#   - Wall clock: UTC milliseconds since epoch
#   - Relative: milliseconds since session start (monotonic)
#   - Monotonic: nanoseconds since boot (for interval calculation)
#
timestamp_wall_ms,timestamp_relative_ms,timestamp_monotonic_ns,frame_sequence,min_temp_c,avg_temp_c,max_temp_c,pixel_count
1701432624223,100,154823947223456789,1,18.5,25.2,32.1,76800
1701432624256,133,154823947256456789,2,18.3,25.4,32.3,76800
```

### GSR Data CSV Format

```csv
# Multi-Modal Recording Session Timing Information
# Session ID: Session_20241201_143022
# Session Start: 2024-12-01T14:30:24.123Z (1701432624123ms UTC)
# Monotonic Start: 154823947123456789ns
# Timing Source: samsung_s22_ground_truth_with_monotonic
#
# GSR Data Columns:
#   timestamp_wall_ms: Wall clock time (UTC)
#   timestamp_relative_ms: Milliseconds since session start (monotonic)
#   timestamp_monotonic_ns: Raw monotonic nanoseconds for precise intervals
#   gsr_microsiemens: Galvanic skin response in microsiemens
#   gsr_raw_12bit: Raw ADC value (0-4095)
#   ppg_raw: Raw PPG sensor value
#
timestamp_wall_ms,timestamp_relative_ms,timestamp_monotonic_ns,gsr_microsiemens,gsr_raw_12bit,ppg_raw,quality_score,connection_rssi
1701432624135,12,154823947135456789,15.2,622,2048,0.95,-45
1701432624143,20,154823947143456789,15.4,631,2052,0.94,-46
```

## Data Alignment Process

### Step 1: Load Session Metadata

```python
import json
with open('session_metadata.json', 'r') as f:
    metadata = json.load(f)

session_start_ms = metadata['sessionStartTimestampMs']
session_start_monotonic_ns = metadata['sessionStartMonotonicNs']
```

### Step 2: Use Relative Timestamps as Common Time Base

All sensor data includes `timestamp_relative_ms` which represents milliseconds since session start using monotonic clock intervals. This provides the common timeline for alignment:

```python
# Thermal data alignment
thermal_df['common_time'] = thermal_df['timestamp_relative_ms']

# GSR data alignment  
gsr_df['common_time'] = gsr_df['timestamp_relative_ms']

# Video frame timing (derived)
video_start_offset_ms = 50  # If video started 50ms after session
video_frame_times = [video_start_offset_ms + (frame_num * 1000/fps) 
                    for frame_num in range(total_frames)]
```

### Step 3: Find Simultaneous Events

```python
def find_simultaneous_events(thermal_df, gsr_df, window_ms=50):
    simultaneous = []
    
    for _, thermal_row in thermal_df.iterrows():
        thermal_time = thermal_row['common_time']
        
        # Find GSR samples within time window
        gsr_matches = gsr_df[
            (gsr_df['common_time'] >= thermal_time - window_ms) &
            (gsr_df['common_time'] <= thermal_time + window_ms)
        ]
        
        if not gsr_matches.empty:
            gsr_match = gsr_matches.iloc[0]
            simultaneous.append({
                'time': thermal_time,
                'thermal_temp': thermal_row['avg_temp_c'],
                'gsr_value': gsr_match['gsr_microsiemens'],
                'sync_accuracy_ms': abs(gsr_match['common_time'] - thermal_time)
            })
    
    return simultaneous
```

## Synchronization Validation

### Flash Sync Test

The system includes a synchronization test that creates simultaneous events across all modalities:

1. **Screen Flash**: Bright white screen flash visible to RGB camera
2. **Thermal Signature**: Screen produces heat signature in thermal camera  
3. **GSR Sync Marker**: Software sync marker logged in GSR data
4. **Timestamp Correlation**: All events should align within 50-100ms

### Running the Test

```kotlin
// In SynchronizationTestActivity
recordingController.addSyncMarker(
    "FLASH_SYNC",
    SystemClock.elapsedRealtimeNanos(),
    mapOf("event" to "screen_flash")
)
```

### Validation Criteria

- **Excellent**: All modalities align within 5ms
- **Good**: All modalities align within 50ms  
- **Acceptable**: All modalities align within 100ms
- **Poor**: Alignment exceeds 100ms (system needs calibration)

## Using the Synchronization Script

The included Python script automates data alignment and validation:

```bash
# Basic alignment
python sync_data_streams.py /path/to/session/directory

# Export aligned data
python sync_data_streams.py /path/to/session/directory --export

# Custom time window
python sync_data_streams.py /path/to/session/directory --window 25
```

### Script Output

```
=== Multi-Modal Data Synchronization Report ===
Session ID: Session_20241201_143022
Session Start: 2024-12-01T14:30:24.123Z
Recording Duration: 60000ms

Thermal Data: 1800 frames over 59850.5ms
GSR Data: 7680 samples over 59900.2ms
Sync Events: 5 recorded
  - session_start at 0.0ms
  - FLASH_SYNC at 15234.5ms
  - session_end at 59876.3ms

Simultaneous Events: 892 found
Average time difference: 12.3ms
Max time difference: 47.8ms
Sync quality: GOOD
```

## Best Practices

### For Researchers

1. **Always use relative timestamps** (`timestamp_relative_ms`) as your primary time axis
2. **Check session metadata** for absolute timing references if needed
3. **Validate sync quality** using the provided script before analysis
4. **Account for modality-specific delays** (e.g., camera processing latency)

### For Developers

1. **Use monotonic clocks** for all interval measurements
2. **Capture session metadata** at the start of every recording
3. **Include comprehensive timing headers** in all data files
4. **Test synchronization regularly** using the Flash Sync test
5. **Log sync events** for validation and debugging

## Troubleshooting

### Poor Synchronization Quality

1. **Check system load**: High CPU usage can cause timing delays
2. **Verify sensor sampling rates**: Ensure consistent data rates
3. **Review network latency**: For networked sensors, check connection quality
4. **Test monotonic clock stability**: Validate `SystemClock.elapsedRealtime()` behavior

### Missing Timestamps

1. **Legacy data format**: Check if files contain old CSV format without relative timestamps
2. **Session metadata missing**: Verify `SessionMetadata` was passed to all recorders
3. **Recording interruption**: Check if session was stopped cleanly

### Large Time Differences

1. **Clock drift**: System may have adjusted time during recording
2. **Processing delays**: Some sensors may have buffering/processing latency
3. **Threading issues**: Ensure timestamping happens in real-time threads

## Implementation Details

### Key Classes

- **`SessionMetadata`**: Manages session timing and metadata
- **`RecordingController`**: Coordinates multi-modal recording with synchronized start
- **`TimeUtil`**: Provides monotonic clock functions and timing utilities
- **`SynchronizationTestActivity`**: Validates cross-modal timing accuracy

### Timing Precision

- **Monotonic clock**: Nanosecond precision using `SystemClock.elapsedRealtimeNanos()`
- **Wall clock**: Millisecond precision using `System.currentTimeMillis()`
- **Target accuracy**: Sub-millisecond capture, 5ms alignment across modalities
- **Acceptable range**: Up to 100ms for research applications

This synchronization system enables precise multi-modal data fusion for research applications while providing robust tools for validation and post-processing alignment.