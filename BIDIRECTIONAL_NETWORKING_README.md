# Bidirectional Command/Control Networking Implementation

This document describes the implementation of bidirectional command/control networking for the IRCamera Android
application, allowing remote control from a PC.

## Overview

The implementation enables the Android app (Galaxy S22) to connect as a **client** to a PC-based host over either
Wi-Fi (TCP) or Bluetooth (RFCOMM). The phone initiates the connection to a known PC server address/port or Bluetooth
service. This allows the PC to remotely control recording on the phone by sending commands.

## Architecture

### Key Components

1. **CommandConnection Interface** (`app/src/main/java/mpdc4gsr/network/CommandConnection.kt`)
    - Common interface for both TCP and Bluetooth connections
    - Manages connection states and message callbacks

2. **TcpClient** (`app/src/main/java/mpdc4gsr/network/TcpClient.kt`)
    - Handles Wi-Fi TCP connections to PC server
    - Implements socket communication with timeout handling
    - Background threads for reading/writing messages

3. **BluetoothClient** (`app/src/main/java/mpdc4gsr/network/BluetoothClient.kt`)
    - Handles Bluetooth RFCOMM connections to PC server
    - Uses standard Serial Port Profile (SPP)
    - Manages Bluetooth adapter and device pairing

4. **NetworkManager** (`app/src/main/java/mpdc4gsr/network/NetworkManager.kt`)
    - High-level coordinator for connection lifecycle
    - Decides which transport to use (TCP or Bluetooth)
    - Provides unified interface for other components

5. **CommandHandler** (`app/src/main/java/mpdc4gsr/network/CommandHandler.kt`)
    - Processes incoming commands from PC
    - Executes actions on RecordingController
    - Generates responses and telemetry

6. **RecordingService Integration** (`app/src/main/java/mpdc4gsr/core/RecordingService.kt`)
    - Service actions for client-side PC connections
    - Manages NetworkManager lifecycle
    - Provides binder access for activities

## Supported Commands

The implementation supports the following commands as specified in the issue:

### START Command

- **Format**: `START` or `START session_id=<id>`
- **Action**: Starts recording session using RecordingController
- **Response**: `START-ACK session_id=<id>` or error message
- **Validation**: Prevents multiple concurrent sessions

### STOP Command

- **Format**: `STOP` or `STOP session_id=<id>`
- **Action**: Stops current recording session
- **Response**: `STOP-ACK msg="Recording session stopped"` or error message

### SYNC Command

- **Format**: `SYNC` or `SYNC t_pc=<timestamp>`
- **Action**: Clock synchronization handshake
- **Response**: `SYNC-RESP t_pc=<pc_time> t_ph=<phone_time>`
- **Purpose**: Enables timestamp alignment between PC and phone

### PING Command

- **Format**: `PING`
- **Action**: Keep-alive check
- **Response**: `PONG`
- **Purpose**: Connection health monitoring

### GET_STATUS Command

- **Format**: `GET_STATUS`
- **Action**: Query current system status
- **Response**: JSON object with recording state, sensors, uptime
- **Example**: `STATUS {"status":"recording","elapsed_sec":42,"sensors":["GSR","RGB","Thermal"]}`

## Connection Methods

### Wi-Fi TCP Connection

```kotlin
// Connect to PC server via Wi-Fi
RecordingService.connectToPCClient(context, "192.168.1.100", 8080)
```

### Bluetooth RFCOMM Connection

```kotlin
// Connect to PC server via Bluetooth
val bluetoothDevice: BluetoothDevice = // ... get paired device
RecordingService.connectToPCBluetooth(context, bluetoothDevice)
```

## Message Protocol

The implementation uses a newline-delimited text protocol for simplicity:

- Each command/message is terminated with `\n`
- Human-readable format for debugging
- JSON support for complex data structures
- Error messages include error codes and descriptions

### Example Message Flow

```
Phone -> PC: HELLO device_name=android_12345 sensors=[RGB,Thermal,GSR]
PC -> Phone: HELLO-ACK
PC -> Phone: PING
Phone -> PC: PONG
PC -> Phone: START
Phone -> PC: START-ACK session_id=session_20241227_001
Phone -> PC: STATUS Recording started at 1703683200000, session: session_20241227_001, sensors: [RGB,Thermal,GSR]
PC -> Phone: STOP
Phone -> PC: STOP-ACK msg="Recording session stopped"
```

## Integration with Recording System

The networking layer integrates seamlessly with the existing recording architecture:

1. **Single-Session Compatibility**: Enforces one-session-at-a-time rule
2. **RecordingController Integration**: Uses existing `startRecording()` and `stopRecording()` methods
3. **Event Notifications**: Sends session start/stop events to PC
4. **Error Handling**: Reports sensor failures and system errors

## Threading and Concurrency

- **Background I/O**: All socket operations run on background threads
- **Non-blocking UI**: Main thread never blocks on network operations
- **Thread-safe Messaging**: Synchronized access to output streams
- **Coroutine Integration**: Uses Kotlin coroutines for async operations

## Error Handling and Recovery

- **Connection Timeouts**: Configurable timeouts for connection attempts
- **Reconnection Logic**: Automatic cleanup and reconnection support
- **Graceful Degradation**: Continues operation if network unavailable
- **Error Reporting**: Detailed error messages sent to PC

## Testing and Validation

### Test Activity

- **NetworkClientTestActivity**: Demonstrates functionality
- Accessible via launcher as "Network Client Test"
- Tests Wi-Fi and Bluetooth connections
- Logs all operations for debugging

### PC Test Server

- **test_pc_server.py**: Simple Python server for testing
- Accepts Android client connections
- Demonstrates full command protocol
- Run with: `python3 test_pc_server.py`

### Usage Example

1. **Start PC Test Server**:
   ```bash
   cd /path/to/IRCamera
   python3 test_pc_server.py
   ```

2. **Launch Android App**:
    - Open "Network Client Test" from launcher
    - App will attempt connection to 192.168.1.100:8080
    - Check logs for connection status and message exchange

3. **Observe Protocol**:
    - Server will send demo command sequence
    - Android app responds to each command
    - Recording sessions can be controlled remotely

## Configuration

### Default Settings

- **TCP Port**: 8080
- **Connection Timeout**: 10 seconds
- **Read Timeout**: 30 seconds
- **Bluetooth UUID**: Standard SPP UUID (00001101-0000-1000-8000-00805F9B34FB)

### Customization

- Change default IP/port in RecordingService constants
- Modify timeout values in TcpClient/BluetoothClient
- Add custom commands in CommandHandler

## Security Considerations

- **Local Network**: Designed for trusted local networks
- **No Authentication**: Currently no authentication mechanism
- **Clear Text**: Messages sent in plain text
- **Bluetooth Pairing**: Requires device pairing for Bluetooth connections

## Future Enhancements

- **TLS Support**: Encrypted TCP connections
- **Authentication**: Token-based authentication
- **Discovery Protocol**: Automatic PC server discovery
- **Multiple Sessions**: Support for concurrent sessions
- **Advanced Telemetry**: Real-time sensor data streaming

## Files Modified/Created

### New Files

- `app/src/main/java/mpdc4gsr/network/CommandConnection.kt`
- `app/src/main/java/mpdc4gsr/network/TcpClient.kt`
- `app/src/main/java/mpdc4gsr/network/BluetoothClient.kt`
- `app/src/main/java/mpdc4gsr/network/NetworkManager.kt`
- `app/src/main/java/mpdc4gsr/network/CommandHandler.kt`
- `app/src/main/java/mpdc4gsr/activities/NetworkClientTestActivity.kt`
- `test_pc_server.py`

### Modified Files

- `app/src/main/java/mpdc4gsr/core/RecordingService.kt` - Added NetworkManager integration
- `app/src/main/AndroidManifest.xml` - Added test activity

## Permissions Required

The following permissions are already present in AndroidManifest.xml:

- `INTERNET`: For TCP connections
- `ACCESS_NETWORK_STATE`: For network status
- `BLUETOOTH_CONNECT`: For Bluetooth connections (Android 12+)
- `BLUETOOTH`: For Bluetooth Classic (legacy)

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure PC server is running and accessible
2. **Bluetooth Pairing**: Device must be paired before connection
3. **Permission Denied**: Check Bluetooth permissions on Android 12+
4. **Network Unreachable**: Verify IP address and network connectivity

### Debug Logging

- Enable verbose logging in NetworkManager and clients
- Check Android logs with tag filters: "NetworkManager", "TcpClient", "BluetoothClient"
- PC server provides detailed connection logs

This implementation provides a robust foundation for bidirectional command/control networking while maintaining
compatibility with the existing recording architecture.