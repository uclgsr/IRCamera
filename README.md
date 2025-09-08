# MPDC4GSR - Multi-Modal Physiological Sensing Platform

[![Android Build](https://img.shields.io/badge/Android-Release--Only-green.svg)](https://developer.android.com/)
[![Shimmer3](https://img.shields.io/badge/Shimmer3-GSR%20Integrated-blue.svg)](https://www.shimmersensing.com/)
[![PC Controller](https://img.shields.io/badge/PC%20Controller-PyQt6-orange.svg)](https://www.qt.io/)

A comprehensive multi-modal physiological sensing platform that synchronously records RGB video, thermal imagery, and GSR (Galvanic Skin Response) data for research applications.

## 🎯 Overview

The MPDC4GSR platform consists of two main components:
- **Android Sensor Node**: Mobile data collection with RGB/thermal cameras and Shimmer3 GSR sensors
- **PC Controller**: Central hub for device management, data aggregation, and real-time monitoring

### Key Features

- **Multi-Modal Recording**: Synchronized RGB (4K60FPS), thermal, and GSR data capture
- **Real-Time Synchronization**: Nanosecond precision timing across all data streams  
- **Shimmer3 Integration**: Official SDK integration for authentic GSR data acquisition
- **Network Discovery**: Automatic device pairing and remote control capabilities
- **Professional Research**: Designed for physiological stress analysis and machine learning research

## 🚀 Quick Start

### Android App
```bash
# Build and install the Android application
./gradlew :app:assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

### PC Controller
```bash
# Set up the Python environment
cd pc-controller
pip install -r requirements.txt
python src/main.py
```

### Basic Usage
1. **Pair Devices**: Ensure Android device and PC are on the same WiFi network
2. **Connect Shimmer3**: Pair your Shimmer3 GSR sensor via Bluetooth
3. **Start Recording**: Launch recording session from PC Controller or Android app
4. **Data Collection**: Multi-modal data streams to synchronized session folders

## 📱 System Requirements

### Android Device
- Android 5.0+ (API 21+)
- Bluetooth 4.0+ for Shimmer3 connectivity
- Camera2 API support (Level 3 preferred for RAW capture)
- 8GB+ storage for recording sessions

### PC Controller
- Python 3.11+
- PyQt6 GUI framework
- Windows/Linux/macOS support
- Network connectivity for device discovery

### Hardware Sensors
- **Shimmer3 GSR+**: Galvanic skin response sensor
- **Thermal Camera**: FLIR/Topdon compatible USB cameras
- **RGB Camera**: Built-in Android camera or external USB cameras

## 🏗️ Architecture

### Hub-and-Spoke Model
```
┌─────────────────┐    WiFi/TCP     ┌──────────────────┐
│   PC Controller │◄──────────────►│  Android Sensor  │
│     (Hub)       │                │     (Spoke)      │
└─────────────────┘                └──────────────────┘
         │                                   │
         ▼                                   ▼
┌─────────────────┐                ┌──────────────────┐
│ Data Aggregation│                │ Sensor Hardware  │
│ Time Sync       │                │ • RGB Camera     │
│ Session Control │                │ • Thermal Camera │
│ Real-time Plots │                │ • Shimmer3 GSR   │
└─────────────────┘                └──────────────────┘
```

### Data Synchronization
- **Time Sync Protocol**: NTP-like handshake for sub-5ms accuracy
- **Flash Sync**: Visual synchronization for post-processing verification
- **Unified Timestamps**: All data streams use synchronized nanosecond timestamps

## 📊 Data Output

Each recording session generates:
```
session_YYYYMMDD_HHMMSS/
├── rgb_video.mp4           # 4K60FPS H.264 video
├── raw_images/             # 30fps DNG raw captures  
├── thermal_video.mp4       # Infrared video stream
├── gsr_data.csv           # 128Hz GSR measurements
├── sync_events.csv        # Cross-modal sync markers
└── session_metadata.json  # Session configuration
```

## 🔧 Development

### Building from Source
```bash
# Clone repository
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera

# Android build (release-only configuration)
./gradlew clean :app:assembleRelease

# PC Controller setup
cd pc-controller
pip install -e .
python src/main.py
```

### Key Technologies
- **Android**: Kotlin, CameraX, BLE, MVVM Architecture
- **PC Controller**: Python, PyQt6, OpenCV, PyBind11 C++ backend
- **Communication**: TLS-secured TCP/IP with JSON messaging
- **Sensors**: Official Shimmer Android API, USB Video Class (UVC)

## 📚 Documentation

- **[Quick Start Guide](docs/QUICK_START.md)** - Essential setup and usage
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Development procedures and architecture  
- **[User Manual](docs/USER_MANUAL.md)** - Complete user documentation
- **[API Reference](docs/API_REFERENCE.md)** - Protocol and SDK documentation
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

## 🤝 Contributing

We welcome contributions to the MPDC4GSR platform:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

See **[CONTRIBUTING.md](docs/CONTRIBUTING.md)** for detailed guidelines.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Shimmer Research** for official Android SDK and GSR sensor integration
- **Qt/PyQt** for cross-platform GUI framework
- **Android CameraX** for advanced camera capabilities
- **Research Community** for physiological sensing requirements and validation

---

**MPDC4GSR** - Multi-Modal Physiological Data Collection for GSR Analysis  
*Professional-grade research platform for synchronized physiological sensing*