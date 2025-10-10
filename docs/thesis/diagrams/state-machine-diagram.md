# State Machine Diagram (Session Control)

## Figure 4.7: System State Management and Session Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Idle : Application Start
    
    Idle --> Discovering : User initiates connection
    Discovering --> Idle : Discovery timeout/cancelled
    Discovering --> Connecting : Device found
    
    Connecting --> Idle : Connection failed
    Connecting --> Connected : TCP handshake + HELLO received
    
    Connected --> Configuring : User starts session setup
    Configuring --> Connected : Configuration cancelled
    Configuring --> Ready : All sensors configured
    
    Ready --> Starting : START_RECORD command sent
    Starting --> Recording : All sensors ACK received
    Starting --> Error : Sensor initialization failed
    
    Recording --> Stopping : STOP_RECORD command sent
    Recording --> Error : Critical sensor failure
    Recording --> Recording : Normal operation (heartbeat)
    
    Stopping --> Finalizing : All sensors stopped
    Finalizing --> Connected : Data saved successfully
    
    Error --> Recovering : Automatic recovery attempt
    Error --> Connected : Manual recovery/reset
    Recovering --> Connected : Recovery successful  
    Recovering --> Error : Recovery failed
    
    Connected --> Idle : Disconnect/shutdown
    Ready --> Idle : Disconnect/shutdown
    Error --> Idle : Force disconnect
```

## Detailed State Descriptions

### State Definitions and Triggers

```mermaid
flowchart TB
    subgraph "Idle State"
        A[No active connections<br/>UI shows device discovery panel<br/>Network scanner inactive]
    end
    
    subgraph "Discovering State" 
        B[mDNS broadcast scanning<br/>TCP port probes (8080)<br/>Device list population<br/>Timeout: 30 seconds]
    end
    
    subgraph "Connecting State"
        C[TCP socket establishment<br/>SSL/TLS handshake if enabled<br/>HELLO message exchange<br/>Device authentication]
    end
    
    subgraph "Connected State"
        D[Persistent TCP connection<br/>Heartbeat every 2 seconds<br/>Device capabilities known<br/>UI shows device status]
    end
    
    subgraph "Configuring State"
        E[Sensor parameter setup<br/>Session directory creation<br/>Storage space validation<br/>Quality checks enabled]
    end
    
    subgraph "Ready State"
        F[All sensors initialized<br/>Waiting for record command<br/>Preview streams active<br/>Storage buffers allocated]
    end
    
    subgraph "Starting State"
        G[START_RECORD broadcast<br/>Waiting for sensor ACKs<br/>Synchronization protocol<br/>Timeout: 10 seconds]
    end
    
    subgraph "Recording State" 
        H[Active data capture<br/>All sensors streaming<br/>File I/O operations<br/>Quality monitoring active]
    end
    
    subgraph "Stopping State"
        I[STOP_RECORD broadcast<br/>Graceful sensor shutdown<br/>Buffer flushing<br/>Final timestamp sync]
    end
    
    subgraph "Finalizing State"
        J[CSV file completion<br/>Metadata.json creation<br/>Integrity verification<br/>Storage cleanup]
    end
    
    subgraph "Error State"
        K[Critical failure detected<br/>Emergency data preservation<br/>Error logging active<br/>Recovery procedures]
    end
    
    subgraph "Recovering State"
        L[Automatic reconnection<br/>Sensor reinitialization<br/>State restoration<br/>Data continuity check]
    end
```

## State Transition Conditions

### Normal Operation Flow

```mermaid
graph TB
    subgraph "Happy Path State Transitions"
        S1[User clicks 'Connect Device'] --> T1[mDNS discovery initiated]
        T1 --> S2[Device IP addresses found]
        S2 --> T2[TCP connection attempt]
        T2 --> S3[HELLO handshake completed]
        S3 --> T3[User configures session]
        T3 --> S4[Sensors initialized successfully]
        S4 --> T4[User clicks 'Start Recording']
        T4 --> S5[All sensors confirm ready]
        S5 --> T5[Recording proceeds normally]
        T5 --> S6[User clicks 'Stop Recording']
        S6 --> T6[Graceful shutdown completed]
        T6 --> S7[Files saved, session closed]
    end
```

### Error Handling and Recovery

```mermaid
graph TB
    subgraph "Error Conditions and Recovery Paths"
        E1[Network Connection Lost] --> R1[Exponential backoff retry<br/>Initial: 500ms, Max: 8s]
        E2[Sensor Hardware Failure] --> R2[Fallback to simulation mode<br/>Continue with available sensors]
        E3[Storage Full/Write Error] --> R3[Emergency save to alternate location<br/>User notification]
        E4[Synchronization Drift >5ms] --> R4[Re-synchronization protocol<br/>Timestamp correction]
        E5[Unexpected Process Termination] --> R5[Session recovery on restart<br/>Data integrity check]
        
        R1 --> D1{Connection Restored?}
        R2 --> D2{Critical Sensors Available?}
        R3 --> D3{Emergency Save Successful?}
        R4 --> D4{Sync Within Tolerance?}
        R5 --> D5{Data Recoverable?}
        
        D1 -->|Yes| Recovery1[Resume operation]
        D1 -->|No| Failure1[Force disconnect]
        
        D2 -->|Yes| Recovery2[Continue recording]
        D2 -->|No| Failure2[Abort session]
        
        D3 -->|Yes| Recovery3[Continue with reduced quality]
        D3 -->|No| Failure3[Critical failure]
        
        D4 -->|Yes| Recovery4[Recording continues]
        D4 -->|No| Failure4[Session restart required]
        
        D5 -->|Yes| Recovery5[Session restored]
        D5 -->|No| Failure5[Data loss reported]
    end
```

## State Machine Implementation

### PC Controller State Management

```python
class SessionStateMachine:
    def __init__(self):
        self.current_state = SessionState.IDLE
        self.devices = {}
        self.session_data = None
        
    def transition_to(self, new_state: SessionState, context: dict = None):
        """Handle state transitions with logging and validation"""
        logger.info(f"State transition: {self.current_state} -> {new_state}")
        
        # Pre-transition validation
        if not self._validate_transition(new_state):
            raise InvalidTransitionError(f"Cannot transition from {self.current_state} to {new_state}")
        
        # State-specific cleanup
        self._cleanup_current_state()
        
        # Execute transition
        self.current_state = new_state
        self._initialize_new_state(context)
        
        # Post-transition actions
        self._notify_state_change()
```

### Android State Synchronization

```kotlin
class AndroidStateMachine {
    private var currentState = DeviceState.DISCONNECTED
    private val stateListeners = mutableListOf<StateChangeListener>()
    
    fun handleCommand(command: String, params: Map<String, String>) {
        when (command) {
            "START_RECORD" -> {
                if (currentState == DeviceState.READY) {
                    transitionTo(DeviceState.RECORDING, params)
                    sendAck(command, "status=recording_started")
                } else {
                    sendError(command, "INVALID_STATE", "Device not ready for recording")
                }
            }
            "STOP_RECORD" -> {
                if (currentState == DeviceState.RECORDING) {
                    transitionTo(DeviceState.STOPPING, params) 
                    initiateSensorShutdown()
                } else {
                    sendError(command, "INVALID_STATE", "No active recording")
                }
            }
        }
    }
}
```

## State Persistence and Recovery

### Session State Persistence

```json
{
  "session_state": {
    "current_state": "RECORDING",
    "session_id": "session_20241215_1430",
    "start_timestamp": 1703441234567,
    "active_devices": [
      {
        "device_id": "Samsung_S22_001",
        "state": "RECORDING",
        "sensors": [
          "thermal",
          "gsr",
          "rgb"
        ],
        "last_heartbeat": 1703441234580
      }
    ],
    "error_recovery": {
      "retry_count": 0,
      "last_error": null,
      "recovery_strategy": "GRACEFUL_DEGRADATION"
    }
  }
}
```

This state machine design ensures robust operation with clear state boundaries, comprehensive error
handling, and automatic recovery mechanisms suitable for long-duration research recording sessions.







