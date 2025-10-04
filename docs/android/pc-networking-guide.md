# PC Networking and Control Interface - User Guide

## Overview

The IRCamera Android app now includes a fully functional PC networking and control interface that allows a PC to remotely control recording sessions and receive live data from the sensors.

## Architecture

The implementation consists of:

1. **Android Side (RecordingService)**
   - TCP server running on port 8080
   - Automatically starts when the app launches
   - Accepts connections from PC controllers
   - Handles START_RECORD, STOP_RECORD, SYNC, and STATUS commands
   - Sends real-time sensor data (GSR, thermal, RGB) to connected PCs

2. **PC Side (pc_controller.py)**
   - Python-based controller with GUI or CLI interface
   - Connects to Android devices on port 8080
   - Sends commands to control recordings
   - Receives and displays real-time sensor data

## Protocol

The system uses a text-based protocol over TCP with the following message types:

### PC to Android Commands

1. **START_RECORD** - Begin recording session
   ```
   START_RECORD session_id=<session_id>
   ```

2. **STOP_RECORD** - End recording session
   ```
   STOP_RECORD session_id=<session_id>
   ```

3. **SYNC_REQUEST** - Time synchronization
   ```
   SYNC_REQUEST t_pc=<timestamp_ms>
   ```

4. **STATUS** - Request device status
   ```
   STATUS
   ```

### Android to PC Responses

1. **HELLO** - Sent on connection
   ```
   HELLO device_name=<device_id> sensors=[RGB,THERMAL,GSR]
   ```

2. **ACK** - Command acknowledgment
   ```
   ACK cmd=<command> [additional_info]
   ```

3. **ERROR** - Command failure
   ```
   ERROR cmd=<command> code=<error_code> msg=<message>
   ```

4. **SYNC_RESPONSE** - Time sync response
   ```
   SYNC_RESPONSE t_pc=<T1> t_ph=<T_phone>
   ```

5. **DATA_GSR** - GSR sensor data
   ```
   DATA_GSR timestamp=<time> gsr=<value>
   ```

6. **FRAME** - Camera frame data (base64 encoded)
   ```
   FRAME type=<RGB|THERMAL> timestamp=<time> data=<base64>
   ```

## Quick Start

### On Android

1. Build and install the app:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. Launch the app - RecordingService starts automatically and listens on port 8080

3. Check the notification tray for "Listening for PC Controller on port 8080"

### On PC

1. Install dependencies:
   ```bash
   cd pc-controller
   pip install -r requirements.txt
   ```

2. Configure connection in `config.yaml`:
   ```yaml
   network:
     port: 8080
     bind_address: "0.0.0.0"
   ```

3. Run the controller:
   ```bash
   # GUI mode (if PyQt6 is installed)
   python3 pc_controller.py
   
   # CLI mode (works without GUI dependencies)
   python3 pc_controller.py --no-gui
   ```

4. Connect to Android device:
   - GUI: Click "Add Device" and enter Android device's IP address
   - CLI: Use the `connect <ip_address>` command

5. Start recording:
   - GUI: Click "Start Recording" button
   - CLI: Use the `start` command

## Network Configuration

### Finding Android Device IP

On the Android device:
```
Settings -> Network & Internet -> Wi-Fi -> [Your Network] -> Advanced -> IP address
```

Or use adb:
```bash
adb shell ip addr show wlan0 | grep inet
```

### Firewall Configuration

Ensure port 8080 is not blocked:

**Android:**
- No configuration needed (app has INTERNET permission)

**PC (Linux):**
```bash
sudo ufw allow 8080/tcp
```

**PC (Windows):**
```powershell
netsh advfirewall firewall add rule name="IRCamera" dir=in action=allow protocol=TCP localport=8080
```

## Usage Examples

### Basic Recording Session

1. Connect PC to Android device
2. PC sends: `START_RECORD session_id=session_001`
3. Android responds: `ACK cmd=START_RECORD session_id=session_001`
4. Recording begins, data streams to PC
5. PC sends: `STOP_RECORD session_id=session_001`
6. Android responds: `ACK cmd=STOP_RECORD`
7. Recording ends, files saved on Android

### Time Synchronization

1. PC sends: `SYNC_REQUEST t_pc=1234567890`
2. Android immediately responds: `SYNC_RESPONSE t_pc=1234567890 t_ph=1234567895`
3. PC calculates offset and RTT
4. PC sends: `SYNC_RESULT t1=<T1> t2=<T2> t3=<T3> offset=<offset> rtt=<rtt>`
5. Android adjusts timestamps accordingly

### Multi-Device Recording

The PC controller supports multiple simultaneous device connections:

1. Connect to multiple Android devices
2. Send `START_RECORD` to all devices with the same session_id
3. All devices record simultaneously with synchronized timestamps
4. Send `STOP_RECORD` to all devices to end session

## Testing

### Test PC Connection

Use the command client to test basic connectivity:

```bash
cd pc-controller
python3 command_client.py
```

Then:
```
> connect <android_ip> 8080
> status
> start session_test
> stop
```

### Run Protocol Tests

```bash
cd pc-controller/tests
python3 test_protocol_verification.py
```

## Troubleshooting

### Connection Refused

- Verify Android device IP address
- Ensure both devices are on the same network
- Check firewall settings
- Verify RecordingService is running (check notification)

### No Data Received

- Check that recording was started successfully
- Verify ACK was received after START_RECORD
- Check network bandwidth (streaming can be data-intensive)

### Port Already in Use

If port 8080 is in use, you can change it by modifying:
- Android: `Protocol.DEFAULT_PORT` in `Protocol.kt`
- PC: `port` in `config.yaml`

## Implementation Details

### Key Components

**Android:**
- `RecordingService` - Main service managing recording and networking
- `NetworkServer` - TCP server accepting PC connections
- `ProtocolHandler` - Parses and dispatches protocol messages
- `Protocol` - Protocol message definitions and utilities

**PC:**
- `pc_controller.py` - Main controller application
- `command_client.py` - Command-line client for testing
- `protocol_adapter.py` - Protocol message parsing/formatting

### Data Flow

```
PC Controller (8080)
    |
    | TCP Connection
    v
Android RecordingService
    |
    +-> NetworkServer (accepts connection)
    |
    +-> ProtocolHandler (processes commands)
    |
    +-> RecordingController (manages sensors)
    |
    +-> GSRRecorder, ThermalRecorder, RgbCameraRecorder
    |
    +-> Data streams back to PC
```

## References

- Protocol specification: `app/src/main/java/mpdc4gsr/feature/network/data/Protocol.kt`
- PC controller: `pc-controller/pc_controller.py`
- Command client: `pc-controller/command_client.py`
- Integration tests: `app/src/androidTest/java/mpdc4gsr/feature/network/ProtocolIntegrationTest.kt`
