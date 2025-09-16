# Topdon TC001 Thermal Camera USB Permission Implementation

## Overview

This document describes the implementation of USB permission handling and thermal camera integration for the Topdon TC001 thermal camera in the IRCamera application.

## Problem Statement

The original implementation had the following issues:
1. **Missing USB Permission Flow**: The app did not properly request Android USB permissions for the TC001 device
2. **Incomplete Hardware Integration**: The thermal camera initialization was stubbed and non-functional
3. **No Hot-plug Support**: Device disconnection during recording was not handled gracefully
4. **Missing Frame Processing**: No actual thermal data capture or processing implementation
5. **No Preview Functionality**: No way to preview thermal frames on the device
6. **No Network Streaming**: No way to stream thermal data to PC for real-time monitoring

## Solution Implementation

### 1. USB Permission Flow Enhancement

#### Key Changes in `ThermalCameraRecorder.kt`:

```kotlin
private fun requestUsbPermission(device: UsbDevice) {
    val activity = getActivityFromContext(context)
    
    if (activity != null) {
        // Primary: Use DeviceTools for Activity-based permission request
        DeviceTools.requestUsb(activity, 0, device)
    } else {
        // Fallback: Direct UsbManager permission request
        val intent = Intent(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager?.requestPermission(device, pendingIntent)
    }
}
```

#### USB Device Filter Addition

Added TC001 device to `ir_device_filter.xml`:
```xml
<!-- Topdon TC001 Thermal Camera -->
<usb-device
    product-id="0x3702"
    vendor-id="0x4206" />
```

### 2. IRUVCTC SDK Integration

#### Real Hardware Initialization:

```kotlin
private suspend fun initializeRealThermalCamera(device: UsbDevice): Boolean {
    // Create IRUVCTC instance with proper callbacks
    val connectCallback = object : ConnectCallback {
        override fun onConnectComplete() {
            isIRCameraConnected = true
            emitStatus()
        }
        override fun onConnectError(error: String?) {
            isIRCameraConnected = false
            isSimulationMode = true
        }
    }
    
    iruvctc = IRUVCTC(
        IR_CAMERA_WIDTH, IR_CAMERA_HEIGHT, context,
        syncBitmap, CommonParams.DataFlowMode.IR_TEMP,
        connectCallback, usbMonitorCallback
    )
    
    // Set frame callback for thermal data processing
    iruvctc?.setIFrameCallBackListener { imageData, temperatureData, width, height ->
        if (isRecording && temperatureData != null) {
            processRealIRFrame(imageData, temperatureData, width, height)
        }
    }
    
    iruvctc?.registerUSB()
    return true
}
```

### 3. Thermal Data Processing

#### Real Thermal Frame Processing:

```kotlin
private fun processRealIRFrame(image: ByteArray?, temperature: ByteArray?, width: Int, height: Int) {
    val thermalData = processRealThermalData(temperature, width, height)
    
    if (isRecording) {
        saveRealIRThermalData(timestamp, frameNumber, thermalData)
    }
    
    // Generate preview bitmap for UI
    val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
    previewCallback?.onThermalFrame(previewBitmap, thermalData)
}
```

#### Temperature Data Conversion:

```kotlin
private fun processRealThermalData(temperatureBytes: ByteArray, width: Int, height: Int): ThermalFrameData {
    val temperatureMatrix = Array(height) { FloatArray(width) }
    
    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            // Convert 16-bit temperature data to Celsius
            val tempRaw = ((temperatureBytes[index * 2].toInt() and 0xFF) or
                          ((temperatureBytes[index * 2 + 1].toInt() and 0xFF) shl 8)).toShort()
            val tempCelsius = (tempRaw.toFloat() / 100.0f) - TEMPERATURE_OFFSET.toFloat()
            temperatureMatrix[y][x] = tempCelsius
        }
    }
    
    return ThermalFrameData(/* ... calculate min/max/avg temperatures ... */)
}
```

### 4. Thermal Preview Implementation

#### Color-Mapped Thermal Preview:

```kotlin
private fun generateThermalPreviewBitmap(thermalData: ThermalFrameData, width: Int, height: Int): Bitmap? {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    
    val tempRange = thermalData.maxTemperature - thermalData.minTemperature
    
    for (y in 0 until height) {
        for (x in 0 until width) {
            val temp = thermalData.temperatureMatrix[y][x]
            val normalized = ((temp - thermalData.minTemperature) / tempRange * 255).toInt()
            
            // Thermal color mapping: cold=blue, medium=green/yellow, hot=red
            val color = when {
                normalized < 85 -> Color.rgb(0, (normalized / 85f * 255).toInt(), 255)
                normalized < 170 -> Color.rgb(((normalized - 85) / 85f * 255).toInt(), 255, 
                                             (255 * (1 - (normalized - 85) / 85f)).toInt())
                else -> Color.rgb(255, (255 * (1 - (normalized - 170) / 85f)).toInt(), 0)
            }
            
            pixels[y * width + x] = color
        }
    }
    
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
```

### 5. Hot-plug Support

#### Device Connection/Disconnection Handling:

```kotlin
@Subscribe(threadMode = ThreadMode.BACKGROUND)
fun onDeviceConnectEvent(event: DeviceConnectEvent) {
    if (event.isConnect) {
        // Device reconnected - switch from simulation to hardware mode
        if (connectedDevice.isTcTsDevice()) {
            val success = initializeRealThermalCamera(connectedDevice)
            if (success && isRecording) {
                // Resume real thermal recording
                startRealIRCameraRecording(iruvctc)
            }
        }
    } else {
        // Device disconnected - gracefully switch to simulation mode
        if (isIRCameraConnected && iruvctc != null) {
            iruvctc?.stopPreview()
        }
        isSimulationMode = true
        isIRCameraConnected = false
        
        if (isRecording) {
            // Continue recording in simulation mode
            startSimulatedThermalRecording()
        }
    }
}
```

### 6. Data Storage Format

#### CSV Output Files:

1. **thermal_data.csv** - Frame-level statistics:
   ```csv
   timestamp_ns,frame_number,min_temp_c,max_temp_c,avg_temp_c,center_temp_c,ambient_temp_c,emissivity,reflected_temp_c
   ```

2. **thermal_frames.csv** - Full temperature matrix per frame:
   ```csv
   timestamp_ns,frame_number,temp_0,temp_1,...,temp_49151
   ```

3. **thermal_calibration.json** - Metadata and calibration info:
   ```json
   {
     "sensor_id": "thermal_camera_1",
     "thermal_resolution": {"width": 256, "height": 192},
     "frame_rate": 9.0,
     "simulation_mode": false,
     "device_info": "Real Thermal Camera Hardware using IRUVCTC"
   }
   ```

### 7. Network Streaming Integration

#### Real-time Thermal Streaming to PC:

```kotlin
// Enable network streaming
val networkServer = NetworkServer(context, 8080)
networkServer.start()
thermalRecorder.enableNetworkStreaming(networkServer)

// Thermal frames are automatically sent to connected PC clients
// at ~2 FPS (every 5th frame) as JSON messages with base64-encoded JPEG images
```

#### Network Message Format:

```json
{
  "type": "thermal_frame",
  "sensor_id": "thermal_camera_1",
  "frame_number": 12345,
  "timestamp_ms": 1640995200000,
  "width": 256,
  "height": 192,
  "min_temp_c": "23.45",
  "max_temp_c": "45.67",
  "avg_temp_c": "34.56",
  "center_temp_c": "35.12",
  "image_jpeg_base64": "/9j/4AAQSkZJRgABAQEAYABgAAD...",
  "simulation_mode": false
}
```

#### PC Client Implementation:

A Python client (`pc-thermal-viewer.py`) connects to the Android app and displays live thermal frames:

```python
# Connect to Android app
viewer = ThermalViewer(android_ip="192.168.1.2", android_port=8080)
viewer.start()

# Receives thermal frames and displays them with OpenCV
# Shows temperature range, frame number, and real/simulation mode
```

## Usage Example

```kotlin
val thermalRecorder = ThermalCameraRecorder(context)
val networkServer = NetworkServer(context, 8080)

// Set up preview callback
thermalRecorder.setThermalPreviewCallback { bitmap, temperatureData ->
    // Update UI with thermal preview
    thermalImageView.setImageBitmap(bitmap)
}

// Enable network streaming for PC clients
networkServer.start()
thermalRecorder.enableNetworkStreaming(networkServer)

// Initialize (handles USB permission automatically)
val success = thermalRecorder.initialize()

// Start recording
val sessionDir = File(context.filesDir, "thermal_session_${System.currentTimeMillis()}")
thermalRecorder.startRecording(sessionDir.absolutePath)

// Monitor network connections
networkServer.connectionStateFlow.collect { connected ->
    if (connected) {
        Log.i(TAG, "PC client connected - streaming thermal data")
    }
}

// Monitor status
thermalRecorder.getStatusFlow().collect { status ->
    Log.i(TAG, "Recording: ${status.isRecording}, Frames: ${status.samplesRecorded}")
}

// Handle errors
thermalRecorder.getErrorFlow().collect { error ->
    Log.e(TAG, "Thermal error: ${error.errorMessage}")
}
```

## Testing

The implementation includes comprehensive tests covering:

1. **USB Permission Flow**: Device detection, permission requests, and fallback scenarios
2. **Simulation Mode**: Frame generation and data recording without hardware
3. **Hot-plug Support**: Device disconnection and reconnection during recording
4. **Thermal Preview**: Bitmap generation and color mapping
5. **Data Integrity**: CSV file creation and thermal calibration
6. **Network Streaming**: PC client connection and thermal frame transmission

## Key Benefits

1. **Robust USB Permission Handling**: Automatic permission requests with proper fallback mechanisms
2. **Seamless Hardware Integration**: Direct integration with Topdon IRUVCTC SDK
3. **Graceful Degradation**: Automatic fallback to simulation mode when hardware unavailable
4. **Hot-plug Resilience**: Continues operation even if device is disconnected during recording
5. **Real-time Preview**: Live thermal preview with color mapping for user feedback
6. **Comprehensive Data Capture**: Full temperature matrices with frame-level statistics
7. **Network Streaming**: Real-time thermal data streaming to PC clients at ~2 FPS with temperature overlay
8. **Cross-platform Integration**: Works with both real hardware and simulation mode

## Dependencies

- **IRUVCTC SDK**: Topdon's thermal camera SDK (included as .aar)
- **DeviceTools**: Existing USB permission utility
- **EventBus**: For USB device connection/permission events
- **NetworkServer**: TCP server for PC client communication
- **Coroutines**: For asynchronous operations and background processing
- **JSON**: For network message formatting
- **Base64**: For image encoding in network messages

## Device Compatibility

- **Primary Target**: Topdon TC001 Thermal Camera (VID: 0x4206, PID: 0x3702)
- **Resolution**: 256x192 pixels
- **Frame Rate**: ~9 FPS
- **Temperature Range**: -20°C to +400°C
- **USB Interface**: USB UVC (Video Class) device