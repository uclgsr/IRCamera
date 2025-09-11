# IRCamera - Multi-Device Thermal Imaging Platform

[![Android Build](https://img.shields.io/badge/Android-Kotlin-green.svg)](https://developer.android.com/)
[![Thermal Devices](https://img.shields.io/badge/Thermal-TC001%20%7C%20HIK-orange.svg)](https://www.topdon.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A comprehensive thermal imaging platform supporting multiple thermal camera devices with real-time processing capabilities. Built for research, industrial, and commercial applications.

## 🎯 Overview

IRCamera is a modular thermal imaging platform designed for research, industrial monitoring, and commercial applications. The platform provides comprehensive capabilities for thermal data collection, analysis, and real-time processing across multiple devices.

### 🚀 Key Features

- **🔥 Multi-Device Support**: TC001, HIKVision thermal cameras with automatic detection
- **⚡ Real-Time Processing**: Live thermal processing and analytics
- **📊 Advanced Analytics**: Temperature monitoring and comprehensive reporting
- **🏗️ Modular Architecture**: Component-based design for scalability
- **📱 Mobile Application**: Feature-rich Android app with modern UI
- **🖥️ PC Controller**: Python-based hub for advanced processing

## 🏗️ Architecture

The platform uses a modular architecture with the following components:

### Android Application
- **Main App**: Enterprise mobile thermal imaging hub
- **Component Modules**: Specialized modules for different thermal cameras and features
- **Core Libraries**: Shared libraries for common functionality

### PC Controller (WIP)
- **Python Backend**: Advanced data processing and device coordination
- **Multi-Device Management**: Centralized control for thermal cameras
- **Data Analytics**: Comprehensive analysis and reporting tools

## 🚀 Quick Start

1. **Check project status**:
   ```bash
   ./status.sh
   ```

2. **Setup development environment**:
   ```bash
   ./dev.sh setup
   # or
   make setup
   ```

3. **Build the project**:
   ```bash
   ./dev.sh validate    # Run all validation checks
   ./dev.sh build       # Build only
   ```

4. **Development workflow**:
   ```bash
   ./dev.sh format      # Format code
   ./dev.sh lint        # Run linting
   ./dev.sh test        # Run tests
   ```

## 🔧 Development

### Prerequisites
- Android Studio Flamingo or later
- JDK 17+
- Android SDK 34
- Python 3.11+ (for PC controller)

### Development Tools
The project includes a unified development script and IDE integration:

| Tool | Description |
|------|-------------|
| `./dev.sh` | Main development script |
| `./status.sh` | Project health overview |
| `Makefile` | Make targets for common tasks |
| `.vscode/` | VS Code configuration |
| `.pre-commit-config.yaml` | Git hooks |

### Available Commands
```bash
./dev.sh help       # Show all available commands
./dev.sh validate   # Run complete validation (recommended)
./dev.sh format     # Format all code files
./dev.sh lint       # Run linting checks
./dev.sh build      # Build the Android app
./dev.sh test       # Run tests
./dev.sh clean      # Clean build artifacts
./dev.sh setup      # Setup development environment

# Alternative using Make
make validate       # Same as ./dev.sh validate
make build          # Same as ./dev.sh build
make help           # Show Makefile targets
```

## 📱 Supported Devices

### Thermal Cameras
- **Topdon TC001**: Primary thermal camera support
- **HIKVision**: Professional thermal cameras
- **Additional**: Extensible architecture for new devices

### Physiological Sensors
- **Shimmer3 GSR+**: Galvanic skin response monitoring
- **BLE Integration**: Wireless sensor connectivity

## 🔄 CI/CD

The project includes automated workflows:
- **Code Quality**: Automated formatting and linting
- **Build Validation**: Multi-variant builds and testing
- **Security**: Dependency vulnerability scanning
- **Releases**: Automated GitHub releases

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes and run `./dev.sh validate`
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request
        GSRIngest[GSR Data Ingestor<br/>Real-Time Processing]
        NetSync[Network Sync<br/>Cloud Integration]
        DataProc[Data Processing<br/>Advanced Analytics]
        MLHub[ML Processing Hub<br/>Training Pipeline]
    end

    subgraph "☁️ Enterprise Cloud Layer"
        AWS[AWS Services<br/>S3, Lambda, EC2]
        Azure[Azure Services<br/>Blob, Functions, VMs]
        Docker[Docker Containers<br/>Microservices]
        K8s[Kubernetes<br/>Orchestration]
    end

    MainApp --> UI
    MainApp --> Services
    MainApp --> ML
    UI --> ThermalIR
    UI --> ThermalLite
    UI --> GSRRec
    UI --> House
    UI --> Edit3D
    UI --> Transfer
    UI --> User
    UI --> Pseudo

    ThermalIR --> LibIR
    ThermalLite --> LibIR
    GSRRec --> LibCom
    House --> LibApp
    Edit3D --> LibMatrix
    Transfer --> LibCom
    User --> LibApp
    Pseudo --> LibIR
    Common --> LibUI

    Services --> BLE
    Services --> Sensors
    UI --> RangeSeek
    LibCom --> Cameras

    LibCom <-->|Secure Network Protocol| PCCore
    GSRIngest --> NetSync
    NetSync --> DataProc
    DataProc --> MLHub

    PCCore --> AWS
    NetSync --> Azure
    MLHub --> Docker
    DataProc --> K8s
```

## 📱 Component Architecture

### Android App Module Structure

```mermaid
graph LR
    subgraph "Core Application"
        App[Main App Module]
        Common[Common Library]
    end

    subgraph "Thermal Processing Components"
        TIR[thermal-ir]
        TLite[thermal-lite]
        Thermal[thermal]
        Pseudo[pseudo]
    end

    subgraph "Data & Analysis Components"
        GSR[gsr-recording]
        House[house]
        Edit3D[edit3d]
        Transfer[transfer]
    end

    subgraph "User Interface Components"
        User[user]
        CommonComp[CommonComponent]
        RangeSeek[RangeSeekBar]
    end

    subgraph "Core Libraries"
        LibApp[libapp]
        LibCom[libcom]
        LibIR[libir]
        LibUI[libui]
        LibHIK[libhik]
        LibMatrix[libmatrix]
        LibMenu[libmenu]
    end

    subgraph "Hardware Integration"
        BLE[BleModule]
    end

    App --> TIR
