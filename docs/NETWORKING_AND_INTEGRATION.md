# Networking and Integration Implementation Guide

This document consolidates all networking, session orchestration, and time synchronization implementation details for the IRCamera platform.

## Architecture Overview

The IRCamera system implements a **Hub-and-Spoke** architecture where:

- **PC Controller (Hub)**: Coordinates recording sessions across multiple Android devices
- **Android Devices (Spokes)**: Act as sensor nodes collecting RGB, thermal, and GSR data  
- **Communication**: Text-based and JSON protocol over TCP with binary data support

## Networking Protocol Implementation

### Message Format

All messages follow the format: `MESSAGE_TYPE param1=value1 param2=value2`

### Core Message Types

#### 1. HELLO (Android → PC)
Sent immediately upon connection to identify the device and its capabilities.
```
HELLO device_name=android_device_001 sensors=[RGB,THERMAL,GSR]
```

#### 2. SYNC_REQUEST (PC → Android)
Initiates NTP-style time synchronization between PC and Android device.
```
SYNC_REQUEST timestamp=1699123456789
```

#### 3. START_RECORD (PC → Android)
Commands device to begin recording session.
```
START_RECORD session_id=session_20231104_123456 participant_id=P001 study_name=experiment1
```

#### 4. STOP_RECORD (PC → Android)
Commands device to stop current recording session.
```
STOP_RECORD session_id=session_20231104_123456
```

#### 5. GET_STATUS (PC → Android)
Requests current device status and sensor availability.
```
GET_STATUS
```

### Response Messages

#### ACK Messages
All commands receive acknowledgment responses:
```
START_RECORD_ACK session_id=session_20231104_123456 status=SUCCESS
STOP_RECORD_ACK session_id=session_20231104_123456 status=SUCCESS
```

#### STATUS Response
Provides device status in JSON format:
```
STATUS {"device_name":"android_001","sensors":{"RGB":"READY","THERMAL":"READY","GSR":"CONNECTED"},"recording":false}
```

## Session Orchestration Implementation

### RecordingController - Central Session Orchestrator

The `RecordingController` manages the complete lifecycle of recording sessions with the following enhanced features:

#### Session State Machine
Implements a robust state machine: `IDLE → STARTING → RECORDING → STOPPING → STOPPED`

#### Trigger Source Tracking
Differentiates between various trigger sources:
- `LOCAL_UI`: Started from Android app UI
- `REMOTE_PC`: Started from PC controller command
- `LOCAL_NOTIFICATION`: Started from notification action
- `SCHEDULED`: Started from scheduled task

#### Key Implementation Methods

```kotlin
suspend fun startRecording(
    sessionId: String? = null,
    participantId: String? = null,
    studyName: String? = null,
    enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer"),
    triggerSource: TriggerSource = TriggerSource.LOCAL_UI
): Boolean

suspend fun stopRecording(): Boolean

fun getRecordingState(): RecordingState
```

#### Partial Sensor Support
The system continues recording even if some sensors fail to start, providing:
- Graceful degradation
- Health monitoring for active sensors
- Automatic reconnection attempts
- Session event tracking

## Time Synchronization Implementation

### TimeSyncManager

Location: `app/src/main/java/mpdc4gsr/sync/TimeSyncManager.kt`

#### NTP-Style Protocol
Implements Network Time Protocol-style synchronization:

1. PC sends `SYNC_REQUEST` with timestamp t1
2. Android captures receive timestamp t2 immediately
3. Android sends `SYNC_RESULT` with t2 and current timestamp t3
4. PC receives response with timestamp t4
5. Calculate offset: `((t2 - t1) + (t3 - t4)) / 2`
6. Calculate RTT: `(t4 - t1) - (t3 - t2)`

#### Comprehensive Logging
Creates `timesync_log.csv` with detailed sync metrics:
```csv
sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms
```

#### Session Integration
- Automatic initialization at session start
- Periodic sync during recording (configurable interval)
- Final sync at session completion
- Non-blocking coroutine-based execution

### Sync Quality Metrics

The system tracks:
- Round-trip time (RTT) for network quality assessment
- Clock offset calculations for timestamp correction
- Sync success/failure rates
- Network adaptation for varying conditions

## Network Communication Layer

### TcpClient Implementation

The Android `TcpClient` provides:
- Automatic reconnection with exponential backoff
- Message queuing during disconnection
- Thread-safe communication
- Error handling and recovery

### CommandHandler Architecture

Command processing follows a modular pattern:

```kotlin
interface CommandHandler {
    suspend fun handleCommand(command: String, parameters: Map<String, String>): String
}

class SimpleCommandHandler : CommandHandler {
    // Implements basic command set
}

class EnhancedCommandHandler : CommandHandler {
    // Implements full feature set with advanced capabilities
}
```

## Integration Points

### MainActivity Integration
- Network status monitoring
- Command routing to appropriate handlers
- UI state updates based on network events

### RecordingService Integration
- Automatic time sync at session boundaries
- Network event propagation to recording components
- Background operation support

### Sensor Integration
- Unified sensor start/stop coordination
- Status reporting to PC controller
- Error isolation preventing cascade failures

## Error Handling and Recovery

### Network Recovery
- Automatic reconnection with intelligent backoff
- Command retry mechanisms
- Graceful degradation when PC unavailable

### Session Recovery
- Session state persistence
- Recovery from interrupted sessions
- Data integrity validation

### Fault Isolation
- Individual sensor failure isolation
- Partial session continuation
- Comprehensive error reporting

## Testing and Validation

### Protocol Testing
The system includes comprehensive testing:
- Mock PC server for Android testing
- Protocol compliance validation
- Performance benchmarking
- Error condition simulation

### Integration Testing
- Multi-device session coordination
- Network reliability testing
- Time synchronization accuracy validation
- Recovery scenario testing

## Configuration Management

### Network Configuration
```yaml
network:
  pc_discovery_port: 8080
  connection_timeout_ms: 5000
  retry_attempts: 3
  heartbeat_interval_ms: 30000

sync:
  interval_ms: 60000
  timeout_ms: 2000
  max_retries: 5
```

### Session Configuration
```yaml
session:
  default_sensors: ["RGB", "Thermal", "GSR"]
  auto_sync_enabled: true
  partial_sensor_support: true
  max_session_duration_hours: 2
```

This consolidated implementation provides a robust, scalable foundation for multi-modal physiological sensing research with enterprise-grade reliability and performance.