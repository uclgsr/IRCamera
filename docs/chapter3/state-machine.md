# Software State Machine for Recording Control

## Figure 3.2: State Machine - Android Recording Application

This state diagram depicts the states of the Android recording app (Idle, Ready, Recording, Syncing, Stopped) and transitions triggered by events such as START or STOP commands from the PC, or sensor connection events.

```mermaid
stateDiagram-v2
    [*] --> Disconnected: Application Start
    
    Disconnected --> Connecting: TCP Connect Initiated<br/>(User action or auto-discovery)
    Connecting --> Disconnected: Connection Failed<br/>(Timeout or network error)
    Connecting --> Idle: TCP Established +<br/>HELLO Handshake Complete
    
    Idle --> Initializing: START_RECORD Command<br/>from PC
    Idle --> Disconnected: Connection Lost<br/>or User Disconnect
    
    Initializing --> InitializingThermal: Initialize Sensors<br/>(Parallel)
    Initializing --> InitializingGSR: Initialize Sensors<br/>(Parallel)
    Initializing --> InitializingRGB: Initialize Sensors<br/>(Parallel)
    
    state "Initializing Thermal" as InitializingThermal {
        [*] --> CheckUSB: Detect TC001
        CheckUSB --> InitSDK: USB Device Found<br/>VID/PID Match
        InitSDK --> ThermalReady: SDK Init Success
        CheckUSB --> ThermalFailed: No Device or<br/>Init Failed
    }
    
    state "Initializing GSR" as InitializingGSR {
        [*] --> ScanBLE: Discover Shimmer3
        ScanBLE --> ConnectBLE: Device Found
        ConnectBLE --> GSRReady: BLE Connected +<br/>Streaming Started
        ScanBLE --> GSRFailed: No Device or<br/>Connection Failed
    }
    
    state "Initializing RGB" as InitializingRGB {
        [*] --> CheckCamera: Request Camera
        CheckCamera --> ConfigCamera: Permission Granted
        ConfigCamera --> RGBReady: CameraX Initialized<br/>1920x1080@30fps
        CheckCamera --> RGBFailed: Permission Denied or<br/>Camera Error
    }
    
    InitializingThermal --> Ready: All Sensors Ready<br/>(Sync Join Point)
    InitializingGSR --> Ready: All Sensors Ready<br/>(Sync Join Point)
    InitializingRGB --> Ready: All Sensors Ready<br/>(Sync Join Point)
    
    InitializingThermal --> Error: Critical Sensor Failed<br/>(Thermal required)
    InitializingGSR --> Error: Critical Sensor Failed<br/>(GSR optional)
    InitializingRGB --> Error: Critical Sensor Failed<br/>(RGB required)
    
    Ready --> Recording: All ACKs Sent +<br/>Final Sync Complete
    Ready --> Error: Timeout Waiting<br/>for Sensor Ready
    
    Recording --> Recording: Continuous Data Capture<br/>- Thermal: 25 Hz CSV<br/>- GSR: 128 Hz CSV<br/>- RGB: 30 fps MP4
    Recording --> Recording: Heartbeat to PC<br/>(Every 2 seconds)
    Recording --> Syncing: SYNC_REQUEST from PC
    Recording --> Stopping: STOP_RECORD Command<br/>from PC
    Recording --> Error: Critical Failure<br/>(Sensor disconnect,<br/>storage full, crash)
    
    Syncing --> Syncing: Exchange Timestamps<br/>Calculate Clock Offset
    Syncing --> Recording: SYNC_RESPONSE Sent<br/>Clock Adjusted
    Syncing --> Error: Sync Failed<br/>(Drift > threshold)
    
    Stopping --> Finalizing: All Sensors Stopped<br/>Buffers Flushed
    Stopping --> Error: Graceful Stop Failed<br/>(Sensor hang)
    
    Finalizing --> Finalizing: Close CSV Files<br/>Finalize Video<br/>Generate metadata.json
    Finalizing --> Transferring: Files Ready<br/>Transfer Initiated
    
    Transferring --> Transferring: Send Files to PC<br/>(Chunked Transfer)<br/>Progress Updates
    Transferring --> Idle: Transfer Complete<br/>PC ACK Received
    Transferring --> Error: Transfer Failed<br/>(Network lost,<br/>PC unavailable)
    
    Error --> Recovering: Auto-Recovery Attempt<br/>(Exponential Backoff)<br/>500ms → 8s max
    Error --> Idle: Manual Recovery<br/>(User Reset or<br/>PC Command)
    Error --> Disconnected: Fatal Error<br/>(Force Disconnect)
    
    Recovering --> Idle: Recovery Successful<br/>(Connection Restored)
    Recovering --> Recording: Recovery Successful<br/>(Session Resumed)
    Recovering --> Error: Recovery Failed<br/>(Max Retries Exceeded)
    
    note right of Disconnected
        No active connection
        UI shows "Connect" button
        Network scanner inactive
    end note
    
    note right of Idle
        TCP connected to PC
        HELLO exchanged
        Waiting for commands
        Heartbeat active (2s)
    end note
    
    note right of Ready
        All sensors initialized
        Storage allocated
        Waiting for final START
        Preview streams active
    end note
    
    note right of Recording
        Active data capture
        All sensors streaming
        Files being written
        Quality monitoring ON
    end note
    
    note right of Error
        Critical failure detected
        Emergency data save
        Error logging active
        Recovery procedures
    end note
```

## State Definitions

### Disconnected
- **Description**: No active connection to PC orchestrator
- **Entry Actions**: Clear device state, stop all sensors
- **Exit Actions**: None
- **Transitions**: User initiates connection → Connecting

### Connecting
- **Description**: TCP connection establishment in progress
- **Entry Actions**: Open socket to PC:8080, send connection request
- **Invariants**: Connection timeout = 30 seconds
- **Exit Actions**: If successful, register device with PC
- **Transitions**: 
  - Success → Idle
  - Failure/Timeout → Disconnected

### Idle
- **Description**: Connected to PC, awaiting commands
- **Entry Actions**: Send HELLO message with device capabilities
- **Invariants**: Heartbeat every 2 seconds, maintain TCP connection
- **Exit Actions**: None
- **Transitions**:
  - START_RECORD command → Initializing
  - Connection lost → Disconnected

### Initializing
- **Description**: Sensor initialization in parallel
- **Entry Actions**: 
  - Request runtime permissions (CAMERA, BLUETOOTH, STORAGE)
  - Create session directory
  - Initialize TimeManager with PC clock offset
- **Substates**: Thermal, GSR, RGB initialization (parallel)
- **Timeout**: 10 seconds per sensor
- **Exit Actions**: Send ACK messages for each ready sensor
- **Transitions**:
  - All critical sensors ready → Ready
  - Any critical sensor fails → Error

### Ready
- **Description**: All sensors initialized, waiting for final sync
- **Entry Actions**: Allocate storage buffers, start preview streams
- **Invariants**: All sensors in standby mode
- **Exit Actions**: None
- **Transitions**:
  - Final sync complete → Recording
  - Timeout (10s) → Error

### Recording
- **Description**: Active multi-modal data capture
- **Entry Actions**: 
  - Start all sensor streams
  - Open CSV files (thermal, GSR)
  - Start video recording (RGB)
  - Begin quality monitoring
- **Invariants**: 
  - Data logged continuously
  - Heartbeat to PC every 2s
  - File size monitored
  - Battery level checked
- **Active Operations**:
  - Thermal: 25 Hz → CSV (timestamp_ns, 256x192 matrix)
  - GSR: 128 Hz → CSV (timestamp_ns, microsiemens)
  - RGB: 30 fps → MP4 (H.264 encoded)
- **Exit Actions**: None (cleanup in next state)
- **Transitions**:
  - SYNC_REQUEST → Syncing (temporary)
  - STOP_RECORD → Stopping
  - Critical failure → Error

### Syncing
- **Description**: Time synchronization in progress (doesn't stop recording)
- **Entry Actions**: Record t2 (receive timestamp), prepare t3 (send timestamp)
- **Invariants**: Recording continues in background
- **Exit Actions**: Apply clock drift correction if needed
- **Transitions**:
  - SYNC_RESPONSE sent → Recording
  - Sync failed (drift > 5ms) → Error

### Stopping
- **Description**: Graceful sensor shutdown
- **Entry Actions**: 
  - Send stop command to all sensors
  - Flush data buffers
  - Final timestamp sync
- **Timeout**: 5 seconds
- **Exit Actions**: Log final statistics
- **Transitions**:
  - All stopped → Finalizing
  - Timeout → Error (force cleanup)

### Finalizing
- **Description**: File completion and metadata generation
- **Entry Actions**:
  - Close CSV files
  - Finalize MP4 video
  - Generate metadata.json (session_id, timestamps, file sizes)
  - Calculate integrity checksums
- **Invariants**: No new data written
- **Exit Actions**: Mark files ready for transfer
- **Transitions**:
  - Files ready → Transferring
  - Validation failed → Error

### Transferring
- **Description**: Bulk file transfer to PC
- **Entry Actions**: 
  - Open transfer socket
  - Send file manifest
  - Begin chunked transfer
- **Invariants**: 
  - Progress updates every 10%
  - Network timeout = 60s
- **Exit Actions**: Verify PC acknowledgment
- **Transitions**:
  - Transfer complete → Idle
  - Network failure → Error

### Error
- **Description**: Error condition with recovery attempts
- **Entry Actions**: 
  - Log error details
  - Emergency save of buffered data
  - Notify PC of error
- **Error Types**:
  - SENSOR_DISCONNECT: Shimmer3 BLE lost
  - STORAGE_FULL: Insufficient space
  - NETWORK_LOST: TCP connection dropped
  - SYNC_DRIFT: Clock drift > 5ms
  - CRITICAL_FAILURE: Unrecoverable error
- **Recovery Strategy**: Exponential backoff (500ms, 1s, 2s, 4s, 8s max)
- **Exit Actions**: Clear error state if recovered
- **Transitions**:
  - Auto-recovery attempt → Recovering
  - Manual reset → Idle
  - Fatal error → Disconnected

### Recovering
- **Description**: Automatic recovery procedures
- **Entry Actions**:
  - Attempt reconnection (if network error)
  - Reinitialize sensors (if sensor error)
  - Restore session state
- **Max Retries**: 3 attempts
- **Exit Actions**: Send recovery status to PC
- **Transitions**:
  - Success (no session) → Idle
  - Success (session active) → Recording
  - Failed → Error

## State Transition Events

### Commands (from PC)
- `START_RECORD session_id=<id>`: Initiate recording session
- `STOP_RECORD`: End current recording
- `SYNC_REQUEST ts=<timestamp>`: Clock synchronization
- `DISCONNECT`: Graceful disconnect

### Hardware Events
- `USB_DEVICE_ATTACHED`: TC001 thermal camera connected
- `USB_DEVICE_DETACHED`: TC001 disconnected
- `BLE_DEVICE_DISCOVERED`: Shimmer3 found
- `BLE_CONNECTED`: Shimmer3 paired and ready
- `BLE_DISCONNECTED`: Shimmer3 connection lost

### System Events
- `NETWORK_AVAILABLE`: Wi-Fi/TCP connection established
- `NETWORK_LOST`: Connection to PC lost
- `STORAGE_LOW`: < 10% free space
- `BATTERY_LOW`: < 15% remaining

## Fault Handling

### Sensor Disconnection During Recording
If a sensor disconnects (e.g., Shimmer3 BLE lost):
1. Log warning to PC
2. Continue recording with remaining sensors
3. Mark affected data stream as incomplete
4. Attempt auto-reconnection in background

### Network Loss During Recording
If TCP connection to PC is lost:
1. Transition to Error state
2. Continue local recording
3. Queue commands and status updates
4. Attempt reconnection (exponential backoff)
5. When reconnected, sync state and flush queue

### Storage Full
If storage becomes full during recording:
1. Immediate transition to Error state
2. Gracefully stop all sensors
3. Finalize existing files
4. Notify PC of partial session
5. Require manual intervention

This state machine ensures robust operation with clear boundaries, comprehensive error handling, and automatic recovery suitable for long-duration research recording sessions.
