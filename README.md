# IRCamera Multi-Modal Physiological Sensing Platform

A production-ready Android application for synchronized multi-sensor data collection combining thermal imaging, galvanic skin response (GSR), and RGB video capture for physiological computing research.

## Project Overview

The IRCamera platform implements a hub-and-spoke architecture where Android devices act as sensor nodes coordinated by a desktop PC controller. The system provides precise temporal synchronization across multiple sensor modalities for scientific data collection.

### Key Features

- **Multi-Modal Sensing**: Topdon TC001 thermal camera, Shimmer3 GSR+ sensor, RGB camera
- **Time Synchronization**: NTP-style algorithm achieving sub-10ms accuracy
- **Real-time Processing**: Live data streaming and visualization
- **Production Quality**: Comprehensive error handling and validation framework
- **Research Ready**: Automated thesis deliverable generation

## Architecture

### Android Application (`app/`)
- **Modular Design**: Dedicated managers for each sensor type
- **Background Services**: Persistent data collection and device coordination
- **MVVM Architecture**: Clean separation following Android best practices
- **Repository Pattern**: Centralized data management with caching

### PC Controller (`pc-controller/`)
- **Desktop Hub**: PyQt6-based GUI for session orchestration
- **Device Discovery**: Automatic mDNS-based Android node detection
- **Data Visualization**: Real-time plotting and analysis tools
- **Export Capabilities**: Multiple format support (CSV, JSON, HDF5)

### Key Components
- `ThermalCameraManager.kt` - Topdon TC001 integration
- `GSRSensorService.kt` - Shimmer3 BLE management  
- `RgbCameraManager.kt` - Camera2 API handling
- `TimeSyncManager.kt` - Cross-device synchronization
- `CommandServer.kt` - PC-Android coordination protocol

## Quick Start

### Android Setup
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### PC Controller Setup
```bash
cd pc-controller
pip install -r requirements.txt
python run_unified_controller.py
```

### Testing
```bash
# Android tests
./gradlew test
./gradlew connectedAndroidTest

# Integration testing
cd testing-suite
python run_evaluation.py
```

## Hardware Requirements

### Supported Devices
- **Thermal Camera**: Topdon TC001 (USB-C connection)
- **GSR Sensor**: Shimmer3 GSR+ (Bluetooth LE)
- **Android Device**: API level 26+ with USB OTG support
- **PC Controller**: Windows/Linux/macOS with Python 3.8+

### Validated Configurations
- Samsung Galaxy S22 with Topdon TC001 + Shimmer3 GSR+
- Google Pixel 6 Pro with thermal/GSR sensor combinations

## Documentation

### Quick Navigation
- **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** - Master index to all documentation
- **[COMPREHENSIVE_TESTING_GUIDE.md](COMPREHENSIVE_TESTING_GUIDE.md)** - Complete testing and validation guide
- **[pc-controller/README.md](pc-controller/README.md)** - Desktop application setup and usage

### Implementation Status
- **[DELIVERY_STATUS.md](DELIVERY_STATUS.md)** - Complete implementation summary and achievements
- **[testing-suite/TESTING_RESULTS_SUMMARY.md](testing-suite/TESTING_RESULTS_SUMMARY.md)** - Consolidated testing results and validation
- **[BACKLOG.md](BACKLOG.md)** - Current development status and roadmap

### System Architecture
- **[docs/COMPREHENSIVE_ARCHITECTURE_DIAGRAMS.md](docs/COMPREHENSIVE_ARCHITECTURE_DIAGRAMS.md)** - Complete system design overview
- **[docs/thesis-diagrams/](docs/thesis-diagrams/)** - Generated figures and tables for thesis
- **[docs/BACKGROUND_DEVICE_SCANNING.md](docs/BACKGROUND_DEVICE_SCANNING.md)** - BLE device discovery implementation

## Development Status

**Current Status**: Production Ready (v1.0)

### Completed Features
- [DONE] Complete multi-sensor integration
- [DONE] Time synchronization (+/-10ms accuracy)
- [DONE] Real-time data streaming
- [DONE] Automated testing framework
- [DONE] Thesis deliverable generation
- [DONE] Performance validation (exceeding targets by 118-333%)

### Performance Metrics
- **Thermal Data**: 25 FPS (target: 15 FPS)
- **RGB Video**: 30 FPS (target: 24 FPS) 
- **GSR Sampling**: 128 Hz (target: 51.2 Hz)
- **Command Latency**: <200ms (target: <500ms)
- **System Reliability**: 99.2% success rate

## Contributing

This is a master's thesis project following MVP principles. All implementations are production-ready with no stub or placeholder code.

### Code Standards
- Kotlin coding conventions for Android components
- MVVM architecture with Repository pattern
- Comprehensive error handling and logging
- ASCII-safe documentation and comments

### Testing Requirements
- Unit tests for all core functionality
- Integration tests for multi-sensor coordination
- Performance benchmarks with quantitative validation
- Real hardware testing (no simulation/mocking)

## License

This project is part of academic research at UCL. See individual component licenses for specific terms.

## Citation

If you use this work in your research, please cite:
```
IRCamera Multi-Modal Physiological Sensing Platform
UCL Master's Thesis Project, 2024
```

## Contact

For technical questions or collaboration opportunities, please refer to the project issues or documentation.