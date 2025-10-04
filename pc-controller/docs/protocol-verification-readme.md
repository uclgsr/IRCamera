# Protocol Verification Guide

## Overview

This directory contains comprehensive tests and documentation verifying that the PC-Android communication protocol works
correctly. The verification confirms that:

- PC commands are correctly sent to Android
- Android correctly parses and processes commands
- Android triggers appropriate sensor actions (start/stop recording)
- Responses (ACK, ERROR) are correctly handled
- Time synchronization works properly

## Files

### Test Files

1. **`test_protocol_compatibility.py`** - Tests the protocol adapter
    - Verifies bidirectional message conversion (text ↔ JSON)
    - Tests all message types can be parsed
    - Tests parameter extraction

2. **`test_protocol_verification.py`** - Comprehensive integration test
    - Simulates Android device
    - Tests complete command flow
    - Verifies responses
    - Tests error handling
    - **RUN THIS to verify the protocol works**

3. **`ProtocolIntegrationTest.kt`** (in app/src/androidTest/)
    - Android-side protocol tests
    - Tests protocol message parsing
    - Tests ProtocolHandler integration
    - Verifies command callbacks work

### Documentation

1. **`PROTOCOL_VERIFICATION_REPORT.md`** - Complete verification report
    - Test results and findings
    - Protocol flow diagrams
    - Integration verification
    - Performance metrics

2. **`PROTOCOL_FLOW.txt`** - Protocol flow diagrams
    - Message sequence diagrams
    - Command examples
    - Response examples

3. **`PROTOCOL_VERIFICATION_README.md`** - This file

### Example Code

1. **`example_pc_control.py`** - Simple usage example
    - Shows how to connect to Android
    - Demonstrates time sync
    - Shows start/stop recording
    - Ready-to-use template

## Running the Tests

### PC-Side Tests

```bash
# Run protocol compatibility tests
cd pc-controller
python3 test_protocol_compatibility.py

# Run comprehensive verification tests (RECOMMENDED)
python3 test_protocol_verification.py
```

**Expected Output:**

```
======================================================================
PROTOCOL VERIFICATION TEST SUMMARY
======================================================================
Tests run: 7
Successes: 7
Failures: 0
Errors: 0

 ALL PROTOCOL VERIFICATION TESTS PASSED 
```

### Android Tests

```bash
# Run Android instrumented tests (requires connected Android device or emulator)
./gradlew connectedAndroidTest --tests "mpdc4gsr.feature.network.ProtocolIntegrationTest"
```

## Using the Example

### Prerequisites

1. Android device with RecordingService running
2. Android device connected to same network as PC
3. Know the Android device's IP address

### Running the Example

```bash
cd pc-controller

# Edit example_pc_control.py and set ANDROID_IP to your device's IP
# Then run:
python3 example_pc_control.py

# Or provide IP as command line argument:
python3 example_pc_control.py 192.168.1.100
```

The example will:

1. Connect to the Android device
2. Perform time synchronization
3. Start a recording session
4. Record for 10 seconds
5. Stop the recording session

## Protocol Message Reference

### Commands (PC → Android)

```
SYNC_REQUEST t_pc=<timestamp_ms>
START_RECORD session_id=<session_id>
STOP_RECORD session_id=<session_id>
SYNC_RESULT t1=<t1> t2=<t2> t3=<t3> offset=<offset_ms> rtt=<rtt_ms>
```

### Responses (Android → PC)

```
HELLO device_name=<device_id> sensors=[<sensor_list>]
SYNC_RESPONSE t_pc=<t_pc> t_ph=<t_phone>
ACK cmd=<command> [additional_params]
ERROR cmd=<command> code=<error_code> msg="<error_message>"
```

### Data Messages (Android → PC)

```
DATA_GSR ts=<timestamp> value=<gsr_value>
FRAME type=<RGB|THERMAL> ts=<timestamp> size=<bytes>
```

## Verification Checklist

Use this checklist to verify the protocol works in your environment:

- [ ] PC protocol compatibility tests pass
- [ ] PC verification tests pass (all 7 tests)
- [ ] Android integration tests pass
- [ ] Can connect to real Android device
- [ ] Time synchronization works
- [ ] START_RECORD command triggers recording
- [ ] STOP_RECORD command stops recording
- [ ] ACK messages received correctly
- [ ] ERROR messages received correctly
- [ ] Complete session flow works end-to-end

## Troubleshooting

### Connection Issues

**Problem:** Cannot connect to Android device

**Solutions:**

1. Verify Android device is on same network
2. Check Android device IP address (Settings → About Phone → Status)
3. Ensure RecordingService is started on Android
4. Check firewall settings (port 8081 must be open)
5. Try connecting with `telnet <android_ip> 8081` to test basic connectivity

### No Response from Android

**Problem:** Commands sent but no response received

**Solutions:**

1. Check Android logcat for error messages: `adb logcat -s RecordingService ProtocolHandler NetworkServer`
2. Verify Android device is not in deep sleep
3. Check socket timeout settings
4. Verify message format is correct

### Recording Doesn't Start

**Problem:** ACK received but recording doesn't actually start

**Solutions:**

1. Check if sensors are connected (GSR, cameras)
2. Check if permissions are granted (camera, Bluetooth, storage)
3. Check RecordingService logs for error messages
4. Verify session directory is writable
5. Check if already recording (will return BUSY error)

### Time Sync Issues

**Problem:** Time sync fails or gives large offset

**Solutions:**

1. Check network latency (should be < 100ms for good sync)
2. Verify clocks on both devices are approximately correct
3. Perform multiple sync rounds and average the results
4. Check for network congestion

## Integration Notes

### RecordingService Integration

The protocol is integrated with `RecordingService.kt`:

- **START_RECORD** → Calls `startRecordingSessionWithTrigger(sessionId, TriggerSource.REMOTE_PC)`
- **STOP_RECORD** → Calls `stopRecordingSessionWithTrigger(TriggerSource.REMOTE_PC)`
- **SYNC_REQUEST** → Returns current time from `TimeManager`

This means protocol commands **actually trigger recording** on the sensors.

### Protocol Handler

The `ProtocolHandler` class processes incoming messages and calls appropriate callbacks. It's configured in
`RecordingService` with:

```kotlin
protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
    override suspend fun onStartRecording(sessionId: String): CommandResult { ... }
    override suspend fun onStopRecording(sessionId: String): CommandResult { ... }
    override suspend fun onSyncRequest(pcTimestamp: Long): SyncResult { ... }
})
```

### Network Server

The `NetworkServer` handles TCP connections and message passing:

- Listens on port 8081
- Sends HELLO message on connection
- Passes received messages to message flow
- Sends responses back to PC

## Best Practices

### For Development

1. **Always run tests** before making protocol changes
2. **Test with real devices** in addition to mocks
3. **Log all protocol messages** during development
4. **Handle errors gracefully** with proper ERROR messages
5. **Use time sync** before starting recordings

### For Production

1. **Implement retry logic** for failed commands
2. **Add timeouts** for all socket operations
3. **Validate all parameters** before processing
4. **Log protocol errors** for debugging
5. **Monitor connection state** and reconnect if needed

### For Testing

1. **Test all error cases** (already recording, not recording, etc.)
2. **Test with network delays** to ensure robustness
3. **Test concurrent operations** if applicable
4. **Test long-running sessions** to check stability
5. **Test cleanup** on abnormal disconnection

## Performance Characteristics

Based on test results:

- **Connection time**: < 200ms
- **Message round-trip**: ~100ms on localhost
- **Time sync RTT**: ~100ms on localhost
- **Command processing**: < 10ms

Real-world network performance will vary based on:

- Network latency
- WiFi signal strength
- Network congestion
- Device processing load

## Support

For issues or questions:

1. Check the `PROTOCOL_VERIFICATION_REPORT.md` for detailed information
2. Review the test code for examples
3. Check Android logcat for error messages
4. Refer to `PROTOCOL_FLOW.txt` for protocol details

## License

This is part of the IRCamera project.
