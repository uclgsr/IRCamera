# PC-Orchestrated Multi-Modal Recording Implementation

This document describes the implementation of the standardized networking protocol for PC-orchestrated multi-modal
recording as specified in issue #78.

## Architecture Overview

The system implements a **Hub-and-Spoke** architecture where:

- **PC Controller (Hub)**: Coordinates recording sessions across multiple Android devices
- **Android Devices (Spokes)**: Act as sensor nodes collecting RGB, thermal, and GSR data
- **Communication**: Text-based protocol over TCP with binary data support

## Protocol Specification

### Message Format

All messages follow the format: `MESSAGE_TYPE param1=value1 param2=value2`

### Message Types

#### 1. HELLO (Android → PC)

Sent immediately upon connection to identify the device and its capabilities.

```
HELLO device_name=android_device_001 sensors=[RGB,THERMAL,GSR]
```

#### 2. SYNC_REQUEST (PC → Android)

Initiates NTP-style time synchronization.

```
SYNC_REQUEST t_pc=1640995200000
```

#### 3. SYNC_RESPONSE (Android → PC)

Response to sync request with phone's timestamp.

```
SYNC_RESPONSE t_pc=1640995200000 t_ph=1640995199950
```

#### 4. START_RECORD (PC → Android)

Commands device to begin recording session.

```
START_RECORD session_id=session_20240115_143022
```

#### 5. STOP_RECORD (PC → Android)

Commands device to end recording session.

```
STOP_RECORD session_id=session_20240115_143022
```

#### 6. ACK (Android → PC)

Acknowledges successful command execution.

```
ACK cmd=START_RECORD session_id=session_20240115_143022 start_time=1640995200123
```

#### 7. ERROR (Android → PC)

Reports command execution failure.

```
ERROR cmd=START_RECORD code=SENSOR_FAIL msg="Thermal camera not detected"
```

#### 8. DATA_GSR (Android → PC)

Live GSR sensor data update.

```
DATA_GSR ts=1640995200000 value=2.5
```

#### 9. FRAME (Android → PC)

Binary frame data header (followed by binary data).

```
FRAME type=rgb ts=1640995200000 size=65536
[binary JPEG data follows]
```

## Implementation Components

### Android Components

#### 1. Protocol.kt

- Defines protocol message constants and formats
- Provides message creation and parsing utilities
- Handles quoted parameter parsing

#### 2. NetworkServer.kt

- TCP server listening on port 8080
- Text-based message handling with binary frame support
- Connection management for single PC client

#### 3. ProtocolHandler.kt

- Processes incoming protocol messages
- Handles command callbacks (start/stop recording, sync)
- Manages response generation

#### 4. NetworkConnectionManager.kt

- Connection state management
- Automatic reconnection with exponential backoff
- Connection timeout monitoring (30 seconds)
- Error recovery and notification

#### 5. RecordingService.kt Integration

- Integrates protocol handler with existing recording infrastructure
- Time synchronization via TimeManager
- Preview streaming coordination

### PC Controller Components

#### 1. standardized_controller.py

- Full PC controller implementation
- Device registry and connection management
- Session management with unique session IDs
- Time synchronization with offset calculation

#### 2. Protocol Class

- Message parsing and creation utilities
- Parameter extraction with quoted string support
- Error code definitions

#### 3. DeviceConnection Class

- Per-device connection management
- Message sending/receiving
- Connection health monitoring

#### 4. SessionManager Class

- Recording session lifecycle management
- Multi-device coordination
- Session metadata tracking

## Clock Synchronization

The system implements a simplified NTP-style synchronization:

1. **PC → Android**: `SYNC_REQUEST t_pc=T1`
2. **Android → PC**: `SYNC_RESPONSE t_pc=T1 t_ph=T_phone`
3. **PC calculates**: `offset = T_phone - T1 - (network_delay/2)`

The Android device can use this offset to align its timestamps with the PC's reference time.

## Error Handling and Recovery

### Connection Management

- Automatic server restart on connection loss
- Exponential backoff reconnection (1s, 2s, 4s, 8s, max 30s)
- Maximum 5 reconnection attempts before giving up
- Connection timeout monitoring

### Error Codes

- `FAIL`: General failure
- `BUSY`: Device already recording
- `SENSOR_FAIL`: Sensor hardware issue
- `THERMAL_NOT_FOUND`: Thermal camera not available
- `GSR_NOT_FOUND`: GSR sensor not available

### Graceful Degradation

- Local recording continues if PC connection lost
- Preview streaming automatically disabled/enabled
- Sensor failures reported without crashing

## Usage Examples

### Starting the PC Controller

```python
python3 demo_pc_controller.py
```

### Basic Session Flow

1. Start PC controller
2. Launch IRCamera app on Android device(s)
3. Devices automatically connect and send HELLO
4. PC performs time synchronization
5. PC starts coordinated recording session
6. All devices record simultaneously
7. PC stops session when complete

### Testing the Protocol

```python
python3 test_protocol.py
```

## File Structure

```
app/src/main/java/mpdc4gsr/network/
├── Protocol.kt                    # Protocol constants and utilities
├── NetworkServer.kt               # TCP server implementation
├── ProtocolHandler.kt             # Message processing
├── NetworkConnectionManager.kt    # Connection management
└── (existing files...)

pc-controller/
├── standardized_controller.py     # Full PC implementation
└── (existing files...)

demo_pc_controller.py              # Interactive demo
test_protocol.py                   # Protocol tests
```

## Integration Points

### Existing Systems

- **RecordingController**: Handles actual recording start/stop
- **TimeManager**: Provides synchronized timestamps
- **PreviewStreamer**: Sends live preview data
- **SessionManager**: Manages recording sessions

### Network Flow

```
Android Device                    PC Controller
     |                                |
     |--- HELLO (capabilities) ------>|
     |<-- SYNC_REQUEST (time) --------|
     |--- SYNC_RESPONSE (time) ------>|
     |<-- START_RECORD (session) -----|
     |--- ACK (started) ------------->|
     |--- DATA_GSR (live) ----------->|
     |--- FRAME (preview) ----------->|
     |<-- STOP_RECORD (session) ------|
     |--- ACK (stopped) ------------->|
```

## Configuration

### Android

- Server port: 8080 (configurable)
- Connection timeout: 30 seconds
- Max reconnection attempts: 5
- Preview frame rate: 1-5 FPS (configurable)

### PC Controller

- Default port: 8080
- Device discovery: Automatic via HELLO messages
- Session ID format: `session_YYYYMMDD_HHMMSS`
- Time sync retry: 3 rounds per device

## Troubleshooting

### Common Issues

1. **Connection Failures**
    - Check firewall settings on both devices
    - Ensure devices are on same network
    - Verify port 8080 is not blocked

2. **Time Sync Issues**
    - Check network latency (<100ms recommended)
    - Ensure both devices have accurate system time
    - Monitor for clock drift during long sessions

3. **Recording Start Failures**
    - Verify all sensors are available
    - Check storage space on Android device
    - Ensure permissions are granted

### Debug Information

- Android: Check RecordingService logs
- PC: Use interactive demo for real-time status
- Network: Monitor connection state changes

## Future Enhancements

1. **Multi-PC Support**: Allow multiple PC controllers
2. **Advanced Sync**: Implement full NTP algorithm with drift compensation
3. **Encrypted Communication**: Add TLS/SSL support for secure connections
4. **Discovery Protocol**: mDNS-based device discovery
5. **File Transfer**: Automated post-session file retrieval
6. **Quality Metrics**: Connection quality monitoring and reporting

## Testing

The implementation includes comprehensive testing:

- Protocol message parsing/creation validation
- Mock device simulation for flow testing
- Socket communication verification
- Interactive demo for real device validation

Run tests with: `python3 test_protocol.py`
Run demo with: `python3 demo_pc_controller.py`

## Compliance

This implementation fully addresses the requirements specified in issue #78:

- ✅ Standardized protocol implementation
- ✅ PC-orchestrated session coordination
- ✅ Clock synchronization between devices
- ✅ Live preview streaming support
- ✅ Error handling and recovery
- ✅ Multi-device session management
- ✅ Robust networking with reconnection