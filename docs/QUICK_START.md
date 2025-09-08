# Quick Start Guide - MPDC4GSR Platform

Get up and running with the Multi-Modal Physiological Sensing Platform in under 10 minutes.

## 🎯 Prerequisites

### Required Hardware
- **Android Device**: API 21+ with Camera2 support (Samsung S22 recommended)
- **PC**: Windows/Linux/macOS with Python 3.11+
- **Shimmer3 GSR Sensor**: For physiological data collection
- **WiFi Network**: For device synchronization

### Required Software
- **Android Studio** (for development) or **APK installer**
- **Python 3.11+** with pip
- **Git** for repository cloning

## 📱 Android App Setup

### Option 1: Install Pre-built APK
```bash
# Download and install the release APK
adb install app/build/outputs/apk/release/app-release.apk
```

### Option 2: Build from Source
```bash
# Clone repository
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera

# Build release APK (no debug builds available)
./gradlew clean :app:assembleRelease

# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk
```

## 🖥️ PC Controller Setup

### Install Dependencies
```bash
# Navigate to PC controller directory
cd pc-controller

# Create virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # Linux/macOS
# OR
venv\Scripts\activate     # Windows

# Install required packages
pip install -r requirements.txt
```

### Launch PC Controller
```bash
# Start the application
python src/main.py
```

## 🔗 Device Pairing

### Step 1: Network Connection
1. Ensure both Android device and PC are on the **same WiFi network**
2. Note the PC's IP address for manual connection if needed

### Step 2: Shimmer3 Pairing
1. **Power on** your Shimmer3 GSR sensor
2. On Android device: **Settings > Bluetooth > Scan for devices**
3. **Pair** with "Shimmer3-XXXX" device
4. Grant **Bluetooth permissions** when prompted by the app

### Step 3: Automatic Discovery
1. **Launch** PC Controller application
2. **Launch** Android MPDC4GSR app  
3. Devices should **automatically discover** each other
4. **Confirm pairing** when prompted on both devices

## 🎬 First Recording Session

### Quick Recording Test

#### On PC Controller:
1. **Open** the PC Controller application
2. **Wait** for Android device to appear in device list
3. **Click** "Start Recording" button
4. **Specify** session name and participant ID

#### On Android Device:
1. **Open** MPDC4GSR app
2. **Verify** Shimmer3 connection status (green indicator)
3. **Confirm** recording start when prompted
4. **Monitor** real-time data streams

#### Recording Controls:
- **Sync Flash**: Press to trigger visual synchronization markers
- **Stop Recording**: End session and save all data streams
- **Session Status**: Monitor recording duration and data rates

### Verify Data Output

After recording, check the session folder:
```
IRCamera_Sessions/session_YYYYMMDD_HHMMSS/
├── rgb_video.mp4           # 4K video recording
├── raw_images/             # DNG raw captures
├── thermal_video.mp4       # Infrared data  
├── gsr_data.csv           # Shimmer3 GSR data
├── sync_events.csv        # Synchronization markers
└── session_metadata.json  # Session information
```

## ⚙️ Essential Configuration

### Android App Settings
```kotlin
// Access via long-press on app title or settings menu
- Recording Quality: 4K60FPS / 1080p30FPS
- RAW Capture: Enable for research-grade imaging
- GSR Sampling: 128Hz (automatic with Shimmer3)
- Storage Location: Internal/External SD card
```

### PC Controller Settings
```python
# Configuration in pc-controller/config/settings.json
{
    "discovery_port": 8080,
    "max_devices": 4,
    "sync_tolerance_ms": 5,
    "data_export_format": "HDF5"
}
```

## 🛠️ Common Issues & Quick Fixes

### ❌ "Shimmer3 not detected"
**Solution**: 
1. Verify Bluetooth pairing in Android settings
2. Grant all Bluetooth permissions to MPDC4GSR app
3. Restart Shimmer3 device (power cycle)

### ❌ "PC Controller not visible"
**Solution**:
1. Ensure both devices on same WiFi network
2. Check firewall settings (allow port 8080)
3. Restart both applications

### ❌ "Build failed: assembleDevDebug not found"
**Solution**:
```bash
# Use correct release-only command
./gradlew :app:assembleRelease
```

### ❌ "Camera permission denied"  
**Solution**:
1. Go to Android Settings > Apps > MPDC4GSR
2. Grant Camera, Storage, and Location permissions
3. Restart the application

## 📊 Data Quality Verification

### Quick Checks
1. **GSR Data**: Open `gsr_data.csv` - should show 128 samples/second
2. **Video Quality**: Play `rgb_video.mp4` - verify 4K resolution
3. **Sync Events**: Check `sync_events.csv` for flash markers
4. **File Sizes**: Typical session (5 min):
   - RGB Video: ~300MB (4K60FPS)
   - RAW Images: ~1.8GB (30fps DNG)
   - GSR Data: ~2MB (128Hz CSV)

### Timestamp Validation
```bash
# Verify synchronization accuracy
grep "SYNC_FLASH" sync_events.csv
# Should show timestamps aligned within 5ms across all streams
```

## 🎯 Next Steps

Once your quick start is successful:

1. **[User Manual](USER_MANUAL.md)** - Complete feature documentation
2. **[Developer Guide](DEVELOPER_GUIDE.md)** - Advanced configuration and development  
3. **[Troubleshooting](TROUBLESHOOTING.md)** - Detailed problem resolution
4. **[API Reference](API_REFERENCE.md)** - Integration and automation

## 💡 Pro Tips

- **Battery**: Use power adapter during long recording sessions
- **Storage**: Monitor available space - 5min session ≈ 2GB data
- **Network**: Use 5GHz WiFi for better performance
- **Calibration**: Run sync flash before each session for verification

---

**Ready to start collecting multi-modal physiological data!** 🚀

*For technical support, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md) or open an issue on GitHub.*