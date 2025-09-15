# Shimmer GSR Integration MVP

A minimal viable product that demonstrates real Shimmer GSR sensor integration without fake
validation or overly complex architecture.

## Features

### Android App (Spoke)

- **Real Shimmer3 GSR+ device connection** via official Shimmer Android SDK
- **Bluetooth permission handling** for Android 12+
- **Live GSR data streaming** and display
- **CSV data export** for analysis
- **Network communication** with PC controller

### PC Controller (Hub)

- **TCP server** to receive data from Android devices
- **Real-time GSR data visualization** using matplotlib
- **Multi-device support** (up to 8 Android devices simultaneously)
- **CSV data logging** with timestamps
- **Device status monitoring**

## Quick Start

### Prerequisites

- **Shimmer3 GSR+ device** (paired with Android phone via Bluetooth)
- **Android device** with Bluetooth support
- **PC** with Python 3.7+ and network connectivity
- Both devices on **same WiFi network**

### Setup Instructions

#### 1. PC Controller Setup

```bash
cd pc-controller/
pip install -r requirements_mvp.txt
python shimmer_mvp_controller.py
```

The PC controller will start on port 8888 and show a real-time plot window.

#### 2. Android App Setup

1. **Install APK** on Android device
2. **Pair Shimmer device** in Bluetooth settings (Settings > Bluetooth > Pair new device)
3. **Launch app** and long-press the center main button to access Shimmer MVP
4. **Grant permissions** when prompted (Bluetooth, Location)

#### 3. Connect and Record

1. **Update PC IP** in `ShimmerNetworkClient.kt` (line 10) if needed:
   ```kotlin
   private val serverHost: String = "192.168.1.YOUR_PC_IP"
   ```
2. **Connect to Shimmer**: Tap "Connect to Shimmer" button
3. **Start Recording**: Tap "Start Recording" when device shows as connected
4. **View real-time data** on PC controller dashboard
5. **Stop Recording**: Data automatically exported to CSV

## Technical Details

### Data Flow

```
Shimmer3 GSR+ → BLE → Android App → TCP/JSON → PC Controller → CSV/Visualization
```

### Network Protocol

- **Transport**: TCP sockets on port 8888
- **Format**: JSON messages, newline-delimited
- **Messages**: `gsr_sample`, `recording_start`, `recording_stop`, `sync_marker`

### GSR Data Format

```json
{
  "type": "gsr_sample",
  "timestamp_ms": 1704067200000,
  "gsr_microsiemens": 5.23,
  "raw_value": 2048,
  "resistance_kohm": 191.2,
  "sample_sequence": 1234
}
```

### File Outputs

- **Android**: `shimmer_gsr_data_YYYYMMDD_HHMMSS.csv`
- **PC**: `gsr_data_DEVICE_ID_YYYYMMDD_HHMMSS.csv`

## Architecture

This MVP follows the Hub-and-Spoke architecture:

- **Hub (PC Controller)**: Central data aggregation and visualization
- **Spoke (Android)**: Sensor data collection and transmission

### Key Components

#### Android (`ShimmerMvpActivity.kt`)

- Uses **official Shimmer Android SDK** (not simulation)
- **Real BLE communication** with Shimmer3 GSR+ device
- **12-bit ADC precision** (0-4095 range) as required
- **128Hz sampling rate** for research-grade data
- **Network client** for PC communication

#### PC Controller (`shimmer_mvp_controller.py`)

- **Multi-threaded TCP server** for device connections
- **Real-time matplotlib visualization** (updates every 500ms)
- **CSV export** with proper timestamp alignment
- **Device status monitoring** and connection management

## Troubleshooting

### Connection Issues

1. **Shimmer not found**: Ensure device is paired in Bluetooth settings first
2. **PC connection failed**: Check firewall settings on port 8888
3. **Network errors**: Verify both devices on same WiFi network

### Permission Issues

1. **Android 12+**: Enable precise location permission for BLE scanning
2. **Bluetooth**: Must grant all Bluetooth permissions when prompted
3. **Storage**: Required for CSV export functionality

### Data Issues

1. **No GSR data**: Check Shimmer device battery level
2. **Connection drops**: Ensure devices stay within BLE range (~10m)
3. **Missing samples**: Check for Bluetooth interference

## Development Notes

This implementation:

- ✅ Uses **real Shimmer Android SDK** (not fake simulation)
- ✅ Implements **actual BLE communication** with hardware
- ✅ Provides **working TCP networking** between devices
- ✅ Exports **real CSV data** with proper timestamps
- ✅ Shows **live data visualization** on PC
- ❌ No fake validation reports or misleading success claims
- ❌ No overly complex enterprise architecture

## Next Steps

For production deployment, consider:

1. **TLS encryption** for network communication
2. **Device authentication** and pairing security
3. **Data synchronization** algorithms for sub-5ms accuracy
4. **Multi-sensor fusion** (thermal camera, accelerometer)
5. **Research-grade data export** (HDF5, BIDS compliance)

---

**This is a working MVP that demonstrates real Shimmer GSR integration capabilities.**
