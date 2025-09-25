# Session Orchestration and Lifecycle Management Implementation

## Overview

This document details the implemented session orchestration and lifecycle management system for the IRCamera multi-modal
recording application. The implementation provides comprehensive session control, fault tolerance, and remote command
support as specified in the requirements.

## Architecture Components

### 1. RecordingController - Central Session Orchestrator

**Enhanced Features:**

- **Session State Machine**: Implements IDLE → STARTING → RECORDING → STOPPING → STOPPED states
- **Trigger Source Tracking**: Differentiates between LOCAL_UI, REMOTE_PC, LOCAL_NOTIFICATION, etc.
- **Partial Sensor Support**: Continues recording even if some sensors fail to start
- **Health Monitoring**: Continuously monitors active sensors and attempts reconnection
- **Session Events**: Tracks all significant events during session lifecycle

**Key Methods:**

```kotlin
suspend fun startRecording(
    sessionId: String? = null,
    participantId: String? = null,
    studyName: String? = null,
    enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer"),
    triggerSource: TriggerSource = TriggerSource.LOCAL_UI
): Boolean

suspend fun stopRecording(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean

fun generateSessionManifest(): SessionManifest
```

### 2. RecordingService - Background Service with Remote Commands

**Enhanced Features:**

- **Remote Command Handler**: Processes START/STOP commands from PC Controller
- **Foreground Service Management**: Proper notification lifecycle for recording sessions
- **Crash Recovery Integration**: Checks for crashed sessions on startup
- **Session Manifest Generation**: Creates comprehensive session reports

**Remote Command Support:**

```kotlin
// PC Controller can send START command
override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
    val success = startRecordingSessionWithTrigger(sessionId, RecordingController.TriggerSource.REMOTE_PC)
    return ProtocolHandler.CommandResult(success, "Recording started via PC command")
}

// PC Controller can send STOP command  
override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
    val success = stopRecordingSessionWithTrigger(RecordingController.TriggerSource.REMOTE_PC)
    return ProtocolHandler.CommandResult(success, "Recording stopped via PC command")
}
```

### 3. Session State Machine

```
IDLE
  ↓ startRecording()
STARTING (validating prerequisites, initializing sensors)
  ↓ (if any sensor starts successfully) 
RECORDING (active recording, health monitoring)
  ↓ stopRecording() or error
STOPPING (coordinated sensor shutdown)
  ↓
STOPPED_COMPLETED / STOPPED_FAILED / STOPPED_INCOMPLETE
  ↓
IDLE
```

### 4. Sensor Health and Reconnection Logic

**Health Monitoring:**

- Continuous monitoring of active sensors during recording
- Automatic detection of sensor dropouts
- Health status tracking with consecutive failure counts

**Reconnection Strategy:**

- **GSR/Shimmer**: Bluetooth reconnection with 3 retry attempts
- **Thermal Camera**: USB reconnection handling
- **RGB Camera**: Error recovery (usually always available)
- **Backoff Strategy**: Exponential delay between reconnection attempts

### 5. Session Manifest and Event Logging

**Session Manifest Contents:**

```kotlin
data class SessionManifest(
    val sessionId: String,
    val startTime: Long,
    val stopTime: Long?,
    val duration: Long?,
    val triggerSource: TriggerSource,
    val sensorActivitySummary: Map<String, SensorActivityInfo>,
    val events: List<SessionEvent>,
    val errors: List<String>,
    val warnings: List<String>,
    val fileReferences: Map<String, String>,
    val sessionState: SessionState
)
```

**Event Types Tracked:**

- `SESSION_START_REQUESTED` - Start command received
- `SENSOR_START_SUCCESS` - Individual sensor started
- `SENSOR_START_FAILED` - Sensor failed to start
- `SENSOR_DROPOUT` - Sensor stopped unexpectedly
- `SENSOR_RECONNECTION_ATTEMPT` - Reconnection attempted
- `SENSOR_RECONNECTION_SUCCESS` - Reconnection successful
- `SESSION_FINALIZED` - Recording session completed

## Crash Recovery Integration

**Startup Recovery Check:**

```kotlin
private suspend fun checkForCrashedSessionsOnStartup() {
    val crashRecoveryResult = crashRecoveryManager.checkForCrashedSessions()
    if (crashRecoveryResult.hasCrashedSession) {
        val recoveredSession = crashRecoveryResult.recoveredSession!!
        // Preserve partial data and generate recovery report
        crashRecoveryManager.recoverCrashedSession(recoveredSession)
    }
}
```

**Runtime Protection:**

- Active session tracking in SharedPreferences
- Session marked as active at start, completed/failed at stop
- Recovery of partial data if app crashes mid-session

## Partial Failure Handling

**Fault Tolerance Design:**

- **Continue if ANY sensor starts**: Session proceeds with available sensors
- **Sensor dropout handling**: Recording continues with remaining sensors
- **Graceful degradation**: UI shows which sensors are active/inactive
- **Automatic recovery**: Attempts to reconnect failed sensors during recording

**Example Scenario:**

1. User starts recording with RGB, Thermal, GSR enabled
2. Thermal camera fails to connect (USB not plugged in)
3. RGB and GSR start successfully → Recording proceeds
4. Mid-session: GSR disconnects (Bluetooth range issue)
5. System attempts automatic reconnection
6. If reconnection fails: Recording continues with RGB only
7. Session manifest documents all events for analysis

## Remote PC Command Integration

**Command Flow:**

1. PC Controller sends START command via network
2. RecordingService receives command in ProtocolHandler
3. Service calls `startRecordingSessionWithTrigger(sessionId, REMOTE_PC)`
4. RecordingController processes with REMOTE_PC trigger source
5. Session events and manifest record remote trigger source
6. PC receives acknowledgment with session status

**Unified Command Logic:**
Both local UI and remote PC commands use the same underlying session orchestration logic, ensuring consistent behavior
regardless of trigger source.

## Session Orchestration Benefits

### 1. **Single-Session Design**

- Only one recording session can be active at a time
- Prevents resource conflicts and data corruption
- Clear session boundaries and lifecycle management

### 2. **Comprehensive Event Logging**

- Every session event is timestamped and logged
- Reconnection attempts and failures documented
- Complete audit trail for research analysis

### 3. **Fault Tolerance**

- Partial sensor availability doesn't prevent data collection
- Automatic recovery from transient failures
- Graceful handling of hardware disconnections

### 4. **Remote Control Support**

- PC Controller can start/stop sessions remotely
- Consistent behavior across trigger sources
- Status feedback for remote operations

### 5. **Crash Recovery**

- Partial data preserved even if app crashes
- Clear marking of incomplete sessions
- Recovery reports for troubleshooting

## Usage Examples

### Local UI Start

```kotlin
val success = recordingController.startRecording(
    sessionId = "study_001_participant_123",
    participantId = "P123",
    studyName = "Stress Response Study",
    triggerSource = TriggerSource.LOCAL_UI
)
```

### Remote PC Start (via Protocol Handler)

```json
{
    "command": "start_recording",
    "session_id": "remote_session_001",
    "modalities": ["RGB", "Thermal", "Shimmer"],
    "options": {
        "participant_id": "P456",
        "study_name": "Remote Study"
    }
}
```

### Session Manifest Example

```json
{
    "sessionId": "study_001_participant_123",
    "startTime": 1679834400000,
    "stopTime": 1679834700000,
    "duration": 300000,
    "triggerSource": "REMOTE_PC",
    "sensorActivitySummary": {
        "RGB": {
            "sensorName": "RGB",
            "wasActive": true,
            "startedSuccessfully": true,
            "finalStatus": "COMPLETED"
        },
        "Thermal": {
            "sensorName": "Thermal", 
            "wasActive": false,
            "startedSuccessfully": false,
            "finalStatus": "INACTIVE",
            "errorMessages": ["USB device not found"]
        },
        "Shimmer": {
            "sensorName": "Shimmer",
            "wasActive": true,
            "startedSuccessfully": true,
            "dropouts": [
                {
                    "timestampMs": 1679834450000,
                    "reason": "Bluetooth disconnection",
                    "durationMs": 5000
                }
            ],
            "reconnections": [
                {
                    "timestampMs": 1679834455000,
                    "attemptNumber": 1,
                    "successful": true,
                    "delayMs": 1000
                }
            ],
            "finalStatus": "COMPLETED"
        }
    },
    "events": [
        {
            "eventType": "SESSION_START_REQUESTED",
            "timestampMs": 1679834400000,
            "triggerSource": "REMOTE_PC",
            "success": true
        },
        {
            "eventType": "SENSOR_START_FAILED",
            "timestampMs": 1679834401000,
            "sensorId": "Thermal",
            "success": false,
            "errorMessage": "USB device not found"
        },
        {
            "eventType": "SENSOR_DROPOUT",
            "timestampMs": 1679834450000,
            "sensorId": "Shimmer",
            "success": false,
            "errorMessage": "Bluetooth disconnection"
        },
        {
            "eventType": "SENSOR_RECONNECTION_SUCCESS",
            "timestampMs": 1679834455000,
            "sensorId": "Shimmer",
            "success": true
        }
    ],
    "sessionState": "STOPPED_COMPLETED"
}
```

## Implementation Status

✅ **Completed Features:**

- Session lifecycle state machine
- Trigger source tracking and differentiation
- Partial sensor failure handling
- Sensor health monitoring and reconnection
- Session event logging and manifest generation
- Crash recovery integration
- Remote PC command support
- Foreground service notification management
- Common timebase synchronization integration

🔄 **Integration Points:**

- Works with existing TimeSynchronizationService for unified timestamps
- Integrates with CrashRecoveryManager for session recovery
- Uses existing SessionDirectoryManager for file organization
- Compatible with current network protocol handlers

This implementation provides a robust, fault-tolerant session orchestration system that meets all the specified
requirements while maintaining compatibility with the existing codebase architecture.