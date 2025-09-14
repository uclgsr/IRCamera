# Enhanced Hub-Spoke Architecture Implementation

This document describes the enhanced PC Controller (Hub) integration and NTP-like time synchronization protocol implemented for the Multi-Modal Physiological Sensing Platform.

## Overview

The system implements a complete Hub-and-Spoke architecture where:
- **Hub (PC Controller)**: Central coordinator managing sessions, time sync, and data aggregation
- **Spokes (Android Devices)**: Sensor nodes handling hardware interface and local recording

## Key Components Implemented

### 1. Enhanced Time Synchronization Service (`EnhancedTimeSyncService`)

**Location**: `pc-controller/src/ircamera_pc/sync/enhanced_timesync.py`

**Features**:
- **NTP-like Protocol**: Full 4-timestamp synchronization (t1, t2, t3, t4)
- **Clock Offset Calculation**: Precise offset measurement using NTP algorithm
- **Quality Assessment**: Real-time sync quality monitoring (EXCELLENT, GOOD, FAIR, POOR)
- **Statistics Tracking**: Median offset, P95 percentile, stability metrics
- **Target Accuracy**: <5ms synchronization tolerance as required

**Protocol Flow**:
```
Android (t1) ---> PC Controller (t2)
             <--- Response (t3) <--- PC Controller  
Android (t4) receives response

Clock Offset = ((t2 - t1) + (t3 - t4)) / 2
Round Trip Time = t4 - t1
Network Delay = RTT / 2
```

### 2. Hub Coordinator (`HubCoordinator`)

**Location**: `pc-controller/src/ircamera_pc/core/hub_coordinator.py`

**Features**:
- **Session Management**: Start/stop coordinated recording sessions
- **Device Orchestration**: Manage multiple Android devices simultaneously
- **Sync Markers**: Create temporal alignment markers across all devices
- **Quality Monitoring**: Continuous sync quality assessment
- **Flash Synchronization**: Visual sync signals for temporal verification

**Key Methods**:
- `start_recording_session()`: Begin synchronized recording across devices
- `stop_recording_session()`: End session with proper cleanup
- `create_sync_marker()`: Create temporal alignment points
- `send_flash_sync()`: Send visual synchronization signals
- `get_sync_quality_summary()`: Monitor overall system performance

### 3. Enhanced Network Server Integration

**Location**: `pc-controller/src/ircamera_pc/network/server.py`

**Enhancements**:
- Integrated enhanced time sync service into network server
- Removed duplicate/outdated time sync handlers  
- Added proper NTP-like protocol support
- Enhanced device synchronization monitoring

### 4. Android TimeManager Compatibility

**Location**: `app/src/main/java/com/topdon/tc001/utils/TimeManager.kt`

**Updates**:
- Enhanced protocol compatibility with PC Controller
- Support for new message format with `server_receive_time`/`server_send_time`
- Backward compatibility with legacy protocol
- Improved error handling and parsing

## Usage Example

### Starting the Hub Coordinator

```python
from ircamera_pc.core.hub_coordinator import HubCoordinator

# Initialize and start hub
hub = HubCoordinator()
await hub.start()

# Wait for Android devices to connect
devices = hub.get_connected_devices()

# Start synchronized recording session
session_id = await hub.start_recording_session(
    session_name="Research Session 1",
    participant_id="P001", 
    experiment_type="physiological_monitoring"
)

# Create synchronization markers
await hub.create_sync_marker(session_id, SyncMarkerType.CUSTOM_EVENT, {
    "event": "stimulus_presentation",
    "stimulus_type": "visual"
})

# Send flash sync for verification
await hub.send_flash_sync(session_id, duration_ms=100)

# Stop session
await hub.stop_recording_session(session_id)
```

### Android Time Synchronization

```kotlin
val timeManager = TimeManager.getInstance(context)

// Synchronize with PC Controller
val success = timeManager.synchronizeWithPC("192.168.1.100", 8080)

if (success) {
    // Get synchronized timestamp
    val syncedTimestamp = timeManager.getCurrentTimestampNs()
    
    // Check sync quality
    val quality = timeManager.getSyncQuality()
    Log.i("TimeSync", "Quality: ${quality.level}, Offset: ${quality.offsetNs}ns")
}
```

## Integration Demo

Run the included demo to see the system in action:

```bash
cd pc-controller
python -m ircamera_pc.examples.hub_spoke_demo
```

The demo will:
1. Start the Hub Coordinator
2. Wait for Android device connections
3. Monitor device synchronization quality
4. Start a demo recording session
5. Create sync markers and flash synchronization
6. Display real-time statistics
7. Stop session and show final metrics

## Sync Quality Requirements

The system meets the FR3 requirements:

- **Target Accuracy**: ≤5ms median offset
- **Quality Levels**:
  - EXCELLENT: ≤2ms offset
  - GOOD: ≤5ms offset  
  - FAIR: ≤10ms offset
  - POOR: >10ms offset

- **Monitoring**: Continuous sync quality assessment
- **Statistics**: Median, P95, stability tracking
- **Compliance**: Real-time target compliance verification

## Architecture Benefits

### Hub-and-Spoke Design
- **Centralized Control**: Single point of session coordination
- **Scalability**: Support multiple Android devices simultaneously  
- **Synchronization**: Precise temporal alignment across all nodes
- **Monitoring**: Real-time quality assessment and alerting

### NTP-like Time Protocol
- **High Precision**: 4-timestamp algorithm for accurate sync
- **Network Compensation**: Accounts for variable network delays
- **Quality Assessment**: Continuous monitoring of sync accuracy
- **Robustness**: Handles network jitter and clock drift

### Session Management
- **Coordinated Recording**: Synchronized start/stop across devices
- **Temporal Markers**: Precise event alignment for analysis
- **Quality Assurance**: Ensures sync requirements before recording
- **Data Integrity**: Session-level data organization and validation

## Testing and Validation

### Sync Accuracy Verification
1. Use flash sync commands to generate visual markers
2. Analyze recorded video timestamps across devices
3. Verify alignment within 5ms tolerance
4. Check P95 percentile compliance

### Session Coordination Testing
1. Start multi-device recording sessions
2. Create sync markers at known intervals
3. Verify marker timestamps in recorded data
4. Test device disconnect/reconnect scenarios

### Quality Monitoring
1. Monitor sync quality during long sessions
2. Test network instability scenarios
3. Verify automatic quality degradation alerts
4. Check drift compensation effectiveness

## Future Enhancements

The architecture supports easy extension for:
- LSL (Lab Streaming Layer) integration for real-time streaming
- AES256-GCM encryption via Android Keystore
- Native C++ backend optimization with PyBind11
- Enhanced data export and analysis pipelines
- Advanced quality metrics and alerting

This implementation provides a solid foundation for research-grade physiological data collection with precise temporal synchronization across multiple sensor modalities.