# Shimmer3 GSR SDK Integration Alignment Guide

## SDK Version Information

The project integrates with the official Shimmer SDK:

- **shimmerdriver**: 0.11.5_beta.jar
- **shimmerbluetoothmanager**: 0.11.5_beta.jar
- **shimmerdriverpc**: 0.11.5_beta.jar
- **shimmerandroidinstrumentdriver**: 3.2.4_beta.aar

## Message Handler Constants

The Shimmer Android SDK uses a Handler-based message passing architecture. Message types are identified by `msg.what`:

| Constant                        | Value | Description                                                    |
|---------------------------------|-------|----------------------------------------------------------------|
| MESSAGE_STATE_CHANGE            | 0     | Connection state changed (connecting, connected, disconnected) |
| MESSAGE_READ                    | 2     | Data packet received with ObjectCluster                        |
| MESSAGE_ACK_RECEIVED            | 4     | Acknowledgment received from device                            |
| MESSAGE_DEVICE_NAME             | 5     | Device name information received                               |
| MESSAGE_STOP_STREAMING_COMPLETE | 9     | Streaming stopped successfully                                 |
| MESSAGE_PACKET_LOSS_DETECTED    | 11    | Data packet loss detected                                      |
| MESSAGE_TOAST                   | 999   | Toast notification message                                     |

### State Values

Connection states are passed in `msg.arg1` for MESSAGE_STATE_CHANGE:

| State            | Value | Description            |
|------------------|-------|------------------------|
| STATE_NONE       | 0     | Device disconnected    |
| STATE_CONNECTING | 1     | Connection in progress |
| STATE_CONNECTED  | 2     | Device connected       |
| STATE_STREAMING  | 3     | Device streaming data  |

## ObjectCluster Data Extraction

The Shimmer SDK provides sensor data through the `ObjectCluster` object. Data is retrieved using:

```kotlin
objectCluster.getFormatClusterValue(sensorName: String, format: String)
```

### Sensor Names and Formats

| Sensor          | Name String       | Format | Description                 |
|-----------------|-------------------|--------|-----------------------------|
| GSR Raw         | "GSR"             | "RAW"  | Raw ADC value (0-4095)      |
| GSR Calibrated  | "GSR Conductance" | "CAL"  | Conductance in microsiemens |
| PPG             | "PPG_A13"         | "CAL"  | Photoplethysmogram value    |
| Timestamp       | "Timestamp"       | "CAL"  | Device timestamp            |
| Accelerometer X | "Accelerometer X" | "CAL"  | X-axis acceleration (m/s²)  |
| Accelerometer Y | "Accelerometer Y" | "CAL"  | Y-axis acceleration (m/s²)  |
| Accelerometer Z | "Accelerometer Z" | "CAL"  | Z-axis acceleration (m/s²)  |

### Example Usage

```kotlin
// Extract GSR conductance (calibrated)
val gsrConductance = objectCluster.getFormatClusterValue("GSR Conductance", "CAL")?.toDouble() ?: 0.0

// Extract raw GSR ADC value
val gsrRaw = objectCluster.getFormatClusterValue("GSR", "RAW")?.toInt() ?: 0

// Extract timestamp
val timestamp = objectCluster.getFormatClusterValue("Timestamp", "CAL")?.toLong() ?: 0L
```

## GSR Sensor Configuration

### Sampling Rate Configuration

```kotlin
shimmer.setSamplingRateShimmer(rate: Double)
```

Valid range: 1.0 - 512.0 Hz  
Recommended for GSR: 128.0 Hz

### GSR Range Configuration

```kotlin
shimmer.writeGSRRange(range: Int)
```

| Range Value | Description              |
|-------------|--------------------------|
| 0           | Auto range (recommended) |
| 1           | 10kΩ - 56kΩ              |
| 2           | 56kΩ - 220kΩ             |
| 3           | 220kΩ - 680kΩ            |
| 4           | 680kΩ - 4.7MΩ            |

### Enabling Sensors

```kotlin
shimmer.writeEnabledSensors(sensorBitmap: Long)
```

Note: This method is deprecated but still functional. Use `Shimmer.SENSOR_GSR` constant for GSR sensor.

## GSR Calculation Formulas

### Hardware Specifications

- **ADC Resolution**: 12-bit (0-4095)
- **Reference Voltage (Vref)**: 3.0V
- **Reference Resistance (Rref)**: 40,200Ω (40.2kΩ)

### ADC to Voltage Conversion

```
Vout = (ADC_value / 4095) × 3.0V
```

### Voltage to Resistance Conversion

```
R = Rref × (Vref - Vout) / Vout
R = 40200 × (3.0 - Vout) / Vout (Ohms)
```

### Resistance to Conductance Conversion

```
G = 1,000,000 / R (microsiemens)
```

### Valid Ranges

- **Raw ADC**: 1 - 4095
- **Resistance**: 10kΩ - 4.7MΩ
- **Conductance**: 0.1 - 100 µS

## File Locations

### Core Integration Files

1. **ShimmerDeviceManager.kt**
    - Location: `app/src/main/java/mpdc4gsr/core/data/`
    - Purpose: BLE device discovery and connection management
    - Key Features: Multi-device support, reconnection logic

2. **Shimmer3GSRRecorder.kt**
    - Location: `app/src/main/java/mpdc4gsr/core/data/`
    - Purpose: Main GSR recording orchestration
    - Key Features: Session management, CSV output, data quality monitoring

3. **GSRSensorRecorder.kt**
    - Location: `app/src/main/java/mpdc4gsr/feature/gsr/data/`
    - Purpose: Sensor-level data recording and processing
    - Key Features: ObjectCluster extraction, network streaming

4. **RealShimmerDeviceFactory.kt**
    - Location: `app/src/main/java/mpdc4gsr/feature/gsr/data/`
    - Purpose: Shimmer device wrapper and data handler
    - Key Features: Handler setup, state management, data callbacks

5. **ShimmerApiBridge.kt**
    - Location: `component/gsr-recording/src/main/java/com/mpdc4gsr/gsr/service/`
    - Purpose: GSR calculation bridge
    - Key Features: Raw to calibrated conversion, fallback processing

6. **GSRCalculationUtils.kt**
    - Location: `app/src/main/java/mpdc4gsr/feature/gsr/data/`
    - Purpose: GSR calculation utilities
    - Key Features: Signal quality assessment, validation

## Best Practices

### 1. Handler Usage

Always create Handler on main looper for Shimmer communication:

```kotlin
val shimmerHandler = object : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: android.os.Message) {
        when (msg.what) {
            0 -> handleStateChange(msg)
            2 -> handleDataPacket(msg)
            // ... other message types
        }
    }
}
```

### 2. ObjectCluster Data Extraction

Always provide fallback values:

```kotlin
val value = objectCluster.getFormatClusterValue("Sensor Name", "Format")?.toDouble() ?: 0.0
```

### 3. Connection State Management

Monitor state changes and handle disconnections:

```kotlin
when (state) {
    0 -> handleDisconnected()
    1 -> handleConnecting()
    2 -> handleConnected()
    3 -> handleStreaming()
}
```

### 4. Data Validation

Validate sensor data before processing:

```kotlin
if (gsrRaw in 1..4095 && gsrConductance > 0) {
    // Process valid data
}
```

## References

1. **Official Shimmer Documentation**:
    - [Shimmer Sensing](https://www.shimmersensing.com/)
    - [ConsensysPRO User Guide](https://shimmersensing.com/wp-content/uploads/2021/06/ConsensysPRO-User-Guide.pdf)

2. **Source Repositories**:
    - [Shimmer-Java-Android-API](https://github.com/ShimmerEngineering/Shimmer-Java-Android-API)
    - [ShimmerAndroidAPI](https://github.com/ShimmerEngineering/ShimmerAndroidAPI)

3. **SDK Integration**:
    - SDK JARs located in: `app/libs/`
    - Android AAR located in: `app/libs/`

## Troubleshooting

### Common Issues

1. **ObjectCluster returns null**
    - Ensure sensor is enabled via `writeEnabledSensors()`
    - Check device is streaming with `isStreaming()`
    - Verify sensor name and format strings

2. **Connection fails**
    - Check Bluetooth permissions granted
    - Verify device is paired and in range
    - Ensure device not connected to another app

3. **Invalid GSR values**
    - Check raw ADC value is in range 1-4095
    - Verify GSR range setting matches hardware
    - Ensure proper sensor placement on skin

4. **Packet loss detected**
    - Reduce distance between device and phone
    - Lower sampling rate if needed
    - Check for Bluetooth interference

## Maintenance Notes

- The `writeEnabledSensors()` method shows a deprecation warning but remains functional in SDK 0.11.5_beta
- Message handler constants are hardcoded as the SDK may not export all constants publicly
- ObjectCluster sensor names and formats must match exactly (case-sensitive)
- Always test with actual Shimmer3 GSR hardware to validate integration
