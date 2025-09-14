# PC-to-Phone Communication Testing Guide - UPDATED

This document provides instructions for testing the PC-to-Phone communication functionality that was
implemented to resolve the networking issues described in Issue #76.

## Overview

The PC-to-Phone communication enables the PC Controller (Hub) to remotely control the Android Sensor
Node (Spoke) through a JSON/TCP protocol. The Android app now includes a `NetworkServer` that
automatically starts when `RecordingService` initializes, listening for PC Controller connections.

## Architecture Changes

### Before (Issue #76)

- Network communication was largely unimplemented
- Android app wasn't starting TCP server consistently
- Protocol mismatch between PC and phone
- Command handling was stubbed or missing
- No automatic discovery/pairing

### After (Fix Implementation)

- **NetworkServer** runs TCP server on port 8080 automatically when RecordingService starts
- **Automatic command processing** for all PC Controller commands
- **Proper server-client architecture**: Android is SERVER, PC is CLIENT
- **Compatible protocol**: 4-byte message length + JSON payload (matches PC test script)
- **Real-time command handling** for registration, ping/pong, recording control, sync markers

## Key Architecture Change

**IMPORTANT**: The architecture is now:

- **Android Device**: Runs TCP SERVER on port 8080 (listens for connections)
- **PC Controller**: Acts as TCP CLIENT (connects to Android)

This resolves the core issue where "the phone never actually connects to the PC, nor listens for the
PC's connection."

## Testing Setup

### 1. Android Setup

1. **Build and install the IRCamera app** on your Android device
2. **Launch the app** and navigate to the Hub-Spoke Integration activity:
    - From main menu → More/Settings → Hub-Spoke Integration Demo
    - Or directly launch `HubSpokeIntegrationActivity`
3. **The NetworkServer starts automatically** when the activity loads or RecordingService
   initializes
4. **Note your Android device's IP address** (shown in the app UI or via Settings → About → Status)

### 2. PC Setup

1. **Navigate to the PC controller directory**:
   ```bash
   cd pc-controller/
   ```

2. **Your Android device should now be listening on port 8080**

## Running Tests

### Quick Connection Test

Test basic connectivity between PC and Android:

```bash
cd pc-controller/
python test_pc_to_phone.py --android-ip 192.168.1.100 --test connect
```

Replace `192.168.1.100` with your Android device's actual IP address.

### Full Protocol Test

Test all communication features:

```bash
python test_pc_to_phone.py --android-ip 192.168.1.100 --test all
```

This will test:

- ✅ Device registration handshake
- 🏓 Ping/pong communication
- 🔄 Sync marker commands
- 🎬 Remote recording start/stop

### Individual Feature Tests

Test specific functionality:

```bash
# Test only ping/pong
python test_pc_to_phone.py --android-ip 192.168.1.100 --test ping

# Test only recording control  
python test_pc_to_phone.py --android-ip 192.168.1.100 --test record

# Test only sync markers
python test_pc_to_phone.py --android-ip 192.168.1.100 --test sync
```

## Expected Output

### Successful Test Output

```
🔌 Connecting to Android NetworkServer at 192.168.1.100:8080
✅ Successfully connected to Android device!

🧪 Running test: all

🔍 Testing device registration...
📤 Sent: {"message_type": "enhanced_device_registration", ...}
📥 Received: {"message_type": "enhanced_registration_ack", ...}
✅ Device registration successful!

🏓 Testing ping/pong...
📤 Sent: {"message_type": "ping", ...}
📥 Received: {"message_type": "pong", ...}
✅ Ping/pong successful!

🎬 Testing remote recording start...
📤 Sent: {"message_type": "session_start_command", ...}

⏹️  Testing remote recording stop...
📤 Sent: {"message_type": "session_stop_command", ...}

📊 Test Results: 4/4 tests passed
🎉 All tests passed! PC-to-Phone communication is working.
```

## Manual Testing via UI

### Android Side (Hub-Spoke Integration Activity)

1. **Launch the activity**: Navigate to Hub-Spoke Integration Demo
2. **Enter PC IP**: Input your PC's IP address in the connection field
3. **Connect**: Tap "Connect" to establish connection with PC
4. **Monitor status**: Watch connection status and sync quality indicators
5. **Test recording**: Use start/stop buttons for coordinated recording

### PC Side (Manual Commands)

You can also send commands manually using the Python script or implement your own PC controller
using the message protocol.

## Message Protocol Reference

The communication uses JSON messages over TCP. Here are the key message types:

### PC → Android Commands

```json

{
    "message_type": "session_start_command",
    "session_directory": "/path/to/session",
    "device_id": "pc_controller",
    "timestamp_ns": 1640995200000000000
}

{
    "message_type": "session_stop_command",
    "device_id": "pc_controller",
    "timestamp_ns": 1640995200000000000
}

{
    "message_type": "sync_marker_command", 
    "marker_type": "event_name",
    "timestamp_ns": 1640995200000000000,
    "metadata": {"key": "value"}
}

{
    "message_type": "ping",
    "device_id": "pc_controller", 
    "timestamp_ns": 1640995200000000000
}
```

### Android → PC Responses

```json

{
    "message_type": "enhanced_registration_ack",
    "device_id": "android_device_id",
    "status": "registered"
}

{
    "message_type": "pong",
    "device_id": "android_device_id",
    "timestamp_ns": 1640995200000000000  
}

{
    "message_type": "recording_status",
    "session_stats": {...},
    "sensor_status": {...}
}
```

## Troubleshooting

### Connection Issues

**❌ Connection refused**

- Verify Android app is running and RecordingService is started
- Check that the correct port (8080) is being used
- Ensure both devices are on the same WiFi network

**❌ Connection timeout**

- Double-check the Android device IP address
- Test network connectivity with `ping <android-ip>`
- Check firewall settings on both devices

**❌ No response to commands**

- Check Android app logs: `adb logcat | grep RecordingService`
- Verify message format matches the protocol exactly
- Ensure the RecordingService has properly initialized

### Android App Issues

**Service not starting**

- Check app permissions (camera, storage, etc.)
- Look for initialization errors in logs
- Restart the app or reboot the device

**Commands not processed**

- Check that the service is bound to the network client
- Verify JSON message parsing in logs
- Look for command processing errors

## Implementation Details

### Key Classes Modified

1. **`RecordingService.kt`**:
    - Added `EnhancedNetworkClient` integration
    - Added command processing methods (`handlePCCommand`)
    - Added connection management via intents

2. **`HubSpokeIntegrationActivity.kt`**:
    - Modified to use RecordingService for connections
    - Updated UI to reflect service-managed connection state

3. **`RecordingController.kt`**:
    - Added `getActiveSensorCount()` method for status reporting

### Network Protocol Features

- **Persistent Connection**: RecordingService maintains connection across app lifecycle
- **Automatic Command Processing**: Commands are processed without user intervention
- **Status Reporting**: Real-time sensor and recording status sent to PC
- **Error Recovery**: Connection monitoring and automatic retry logic
- **Time Synchronization**: Built-in timestamp synchronization between devices

## Validation Checklist

Use this checklist to verify the fix is working:

- [ ] Android app successfully starts RecordingService
- [ ] PC can connect to Android device on port 8080
- [ ] Device registration handshake completes successfully
- [ ] Ping/pong communication works bidirectionally
- [ ] PC can remotely start recording on Android
- [ ] PC can remotely stop recording on Android
- [ ] Sync markers are processed and applied correctly
- [ ] Connection survives app backgrounding/foregrounding
- [ ] Error conditions are handled gracefully
- [ ] Network disconnections are detected and reported

## Next Steps

After validating the basic communication:

1. **Integrate with full PC Controller application**
2. **Add automatic device discovery using mDNS/Zeroconf**
3. **Implement file transfer for recorded data**
4. **Add TLS encryption for secure communication**
5. **Expand command set for additional sensor control**

The foundation for PC-to-Phone communication is now in place and should resolve the core networking
issues described in Issue #76.
