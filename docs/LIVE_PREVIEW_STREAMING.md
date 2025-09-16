# Live Preview Streaming - Phone to PC Communication

This document describes the live preview streaming functionality that allows the Android app to stream low-bandwidth preview data to a PC Controller.

## Overview

The Android app acts as a **TCP server** on port 8080, accepting connections from PC clients. Once connected, the app can stream:

- **Downscaled camera frames** (RGB and thermal) as JPEG images
- **Real-time sensor readings** (GSR values)
- **Recording status updates**

## Architecture

### Phone (Server) Side
- **NetworkServer**: Handles TCP socket server and client connections
- **PreviewStreamer**: Manages preview data streaming logic
- **PreviewDataAdapter**: Polls sensor data and feeds it to PreviewStreamer
- **RecordingService**: Coordinates all components

### PC (Client) Side
- Simple TCP client that connects to phone's IP address
- Receives JSON messages with base64-encoded image data
- Can send commands to control streaming

## Message Protocol

All messages are JSON formatted with length prefixes:

### Message Format
```
[4-byte length][JSON message bytes]
```

### PC to Phone Commands

#### Start Preview Streaming
```json
{
    "message_type": "start_preview_streaming",
    "timestamp_ns": 1234567890000000
}
```

#### Stop Preview Streaming
```json
{
    "message_type": "stop_preview_streaming", 
    "timestamp_ns": 1234567890000000
}
```

#### Configure Streaming Parameters
```json
{
    "message_type": "configure_preview_streaming",
    "frame_interval_ms": 1000,
    "sensor_interval_ms": 1000,
    "preview_width": 320,
    "preview_height": 240,
    "jpeg_quality": 70,
    "timestamp_ns": 1234567890000000
}
```

### Phone to PC Data Messages

#### Preview Frame
```json
{
    "message_type": "preview_frame",
    "timestamp_ns": 1234567890000000,
    "frame_type": "rgb" | "thermal",
    "width": 320,
    "height": 240,
    "format": "jpeg",
    "quality": 70,
    "data_base64": "base64-encoded-jpeg-data",
    "data_size_bytes": 12345
}
```

#### Sensor Data
```json
{
    "message_type": "sensor_data",
    "timestamp_ns": 1234567890000000,
    "data": {
        "gsr_microsiemens": 12.5,
        "recording_status": "RECORDING" | "CONNECTED" | "IDLE",
        "client_count": 1
    }
}
```

## Configuration

### Default Settings
- **Server Port**: 8080
- **Frame Interval**: 1000ms (1 FPS)
- **Sensor Interval**: 1000ms (1 Hz)
- **Preview Size**: 320x240 pixels
- **JPEG Quality**: 70%

### Network Requirements
- Android device and PC must be on the same Wi-Fi network
- PC firewall should allow outgoing connections (default on Windows)
- Android app will display its IP address for PC connection

## Usage

### Android App
1. Start the app and ensure RecordingService is running
2. The TCP server automatically starts and listens on port 8080
3. App notification shows "Listening for PC Controller on port 8080"
4. When PC connects, streaming starts automatically
5. Preview data polling begins when PC is connected

### PC Client
1. Find the Android device's IP address (displayed in app)
2. Run the test client:
   ```bash
   python3 test_android_preview_client.py <android_ip>
   ```
3. Client will connect and request preview streaming
4. Received frames are saved as JPEG files
5. Real-time sensor data is displayed in console

## Testing

Use the provided test client to verify functionality:

```bash
cd pc-controller
python3 test_android_preview_client.py 192.168.1.100 --duration 60
```

This will:
- Connect to Android device at 192.168.1.100:8080
- Send configuration and start commands
- Receive preview data for 60 seconds
- Save received frames as JPEG files
- Display statistics

## Error Handling

### Connection Management
- Android continues recording even if PC disconnects
- Automatic reconnection support on PC side
- Graceful handling of network interruptions

### Resource Management
- Preview frames are downscaled to minimize bandwidth
- Automatic bitmap cleanup to prevent memory leaks
- Background thread isolation prevents UI blocking

### Fallback Behavior
- If no preview data available, no frames are sent
- Sensor data continues even without camera frames
- Recording status updates continue regardless of sensor states

## Performance Considerations

### Bandwidth Usage
- RGB frames ~5-15KB each at 320x240 JPEG 70%
- Thermal frames ~3-8KB each at 256x192 JPEG 70%
- Sensor data ~200 bytes per message
- Total: ~1-2 KB/s at 1 FPS

### Resource Impact
- Minimal CPU overhead for preview generation
- Low memory usage with automatic cleanup
- Background processing doesn't affect recording performance

## Integration Points

### For Camera Systems
```kotlin
// Get preview adapter from service
val adapter = recordingService.getPreviewDataAdapter()

// Feed RGB camera frame
adapter.updateRgbFrame(rgbBitmap)

// Feed thermal camera frame  
adapter.updateThermalFrameDirect(thermalBitmap)
```

### For Sensor Systems
```kotlin
// Update GSR value directly
adapter.updateGsrValueDirect(gsrValue)

// Or set recorder reference for automatic polling
adapter.setGsrRecorder(gsrSensorRecorder)
```

### For Thermal Camera
```kotlin
// Set thermal camera manager for automatic polling
adapter.setThermalCameraManager(cameraPreviewManager)
```

## Future Enhancements

- **TLS encryption** for secure communication
- **Authentication** mechanism for PC clients
- **Multi-client support** for multiple PC connections
- **Adaptive quality** based on network conditions
- **Real-time video streaming** using WebRTC or similar
- **PC GUI application** for better user experience