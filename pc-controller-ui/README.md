# PC Session Controller

Desktop GUI application for controlling IRCamera sensor nodes remotely.

## Features

### Session Control Panel
- **Start All / Stop All**: Control recording on all connected devices simultaneously
- **Sync Clocks**: Synchronize time with all devices for coordinated recording
- **Refresh Status**: Request latest status from all devices
- **Individual Device Control**: Start/stop/sync individual devices

### Device Management
- **Device List**: Shows all connected sensor nodes with real-time status
- **Sensor Status Display**: Individual status for RGB, Thermal, and GSR sensors
- **Time Synchronization**: Displays clock offset and round-trip time for each device
- **Connection Monitoring**: Real-time connection status and error handling

### Real-time Telemetry
- **GSR Signal Plot**: Live visualization of GSR sensor data (when matplotlib available)
- **Video Preview**: Frame preview from RGB cameras (future enhancement)
- **Thermal Data**: Temperature readings and thermal image preview (future enhancement)

### Session Logging
- **Event Log**: Comprehensive logging of all commands, responses, and events
- **Timestamped Entries**: All log entries include precise timestamps
- **Color Coding**: Error and warning messages highlighted
- **Log Export**: Save session logs to file for analysis

## Installation

### Requirements
- Python 3.7 or higher
- tkinter (usually included with Python)

### Optional Dependencies
Install for enhanced functionality:
```bash
pip install -r requirements.txt
```

### Basic Installation
For basic functionality (no plotting):
```bash
python pc_session_controller.py
```

## Usage

1. **Start the Controller**:
   ```bash
   cd pc-controller-ui/src
   python pc_session_controller.py
   ```

2. **Connect Devices**: The controller listens on port 8080 for device connections. Configure your Android devices to connect to the PC's IP address.

3. **Session Control**:
   - Use "Start All" to begin recording on all connected devices
   - Use "Sync Clocks" before recording for time coordination
   - Monitor device status in the device list
   - View real-time telemetry in the right panel
   - Check session log for detailed event information

## Network Protocol

The controller communicates with Android devices using JSON messages over TCP:

### Commands (PC → Device)
```json
{
  "type": "start_recording",
  "session_id": "session_12345",
  "timestamp": 1640995200.0
}
```

### Status Updates (Device → PC)
```json
{
  "type": "status_update",
  "status": "Connected",
  "sensors": {
    "RGB": {"status": "Connected", "message": ""},
    "Thermal": {"status": "Connected", "message": ""},
    "GSR": {"status": "Connected", "message": ""}
  }
}
```

### Telemetry Data (Device → PC)
```json
{
  "type": "telemetry_gsr",
  "value": 523.4,
  "timestamp": 1640995201.5
}
```

## Configuration

### Network Settings
- Default port: 8080
- Modify `self.server_port` in `SessionController.__init__()` to change port
- Ensure firewall allows connections on the chosen port

### Display Settings
- GSR plot shows last 30 seconds of data
- Device list updates every second
- Log entries are color-coded by severity

## Architecture

The application follows a multi-threaded architecture:
- **Main Thread**: GUI updates and user interaction
- **Server Thread**: Handles incoming device connections
- **Client Handler Threads**: One per connected device for message processing

## Troubleshooting

### Common Issues

1. **"Failed to start server"**: Port already in use or firewall blocking
   - Change port number or close other applications using the port
   - Check firewall settings

2. **Devices not connecting**: Network connectivity issues
   - Verify PC and Android device are on same network
   - Check IP address and port configuration
   - Ensure Android app has network permissions

3. **GSR plot not working**: matplotlib not installed
   - Install optional dependencies: `pip install matplotlib numpy`
   - Or use text-based GSR display as fallback

4. **Telemetry not updating**: Device not sending data
   - Check device sensor status in device list
   - Verify sensors are properly initialized on Android device
   - Check session log for error messages

## Development

### Adding New Features
- Extend `process_device_message()` for new message types
- Add UI elements in appropriate `setup_*_panel()` methods
- Follow existing threading patterns for network operations

### Testing
Run basic connectivity test:
```bash
python -c "import socket; s=socket.socket(); s.bind(('',8080)); print('Port 8080 available')"
```

## Future Enhancements

- Video frame preview with JPEG decoding
- Thermal image visualization with temperature mapping
- Export session data to CSV/JSON formats
- Multi-session recording support
- Advanced GSR signal analysis tools
- Camera parameter remote control integration