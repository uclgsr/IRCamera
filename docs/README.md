# IRCamera Multi-Modal Thermal Sensing Platform - Documentation Hub

[![Copilot](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml/badge.svg?branch=dev)](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml)

## Documentation Overview

This is the central documentation hub for the IRCamera Multi-Modal Thermal Sensing Platform. All documentation has been
consolidated and organized for easy navigation and maintenance.

## System Architecture

**IRCamera** implements a **Hub-and-Spoke architecture** for distributed multi-modal physiological sensing:

- **Hub (PC Controller)**: Python-based central coordinator with PyQt6 GUI
- **Spoke (Android Sensor Node)**: Kotlin-based mobile sensor nodes
- **Communication**: JSON-based TCP with mDNS discovery
- **Purpose**: Scientific data acquisition and machine learning analysis

### Key Features

- **Multi-Modal Sensing**: Thermal imaging, GSR (Galvanic Skin Response), and RGB video capture
- **Synchronized Recording**: Precise temporal alignment across all sensor modalities
- **Distributed Processing**: Hub-and-Spoke architecture for scalable sensor networks
- **Real-Time Communication**: TCP/JSON protocol with automatic device discovery
- **Professional GUI**: Comprehensive PyQt6 interface for researchers

## Documentation Structure

### Quick Start Guides

| Document                                  | Purpose                                        | Audience                 |
|-------------------------------------------|------------------------------------------------|--------------------------|
| **[User Guide](USER_GUIDE.md)**           | End-user instructions for operating the system | Researchers, Operators   |
| **[Developer Guide](DEVELOPER_GUIDE.md)** | Development setup and procedures               | Developers, Contributors |

### Technical Documentation

| Document                                                      | Purpose                                  | Audience        |
|---------------------------------------------------------------|------------------------------------------|-----------------|
| **[API Reference](API_REFERENCE.md)**                         | Complete module and API documentation    | Developers      |
| **[Architecture Guide](ARCHITECTURE.md)**                     | System architecture and design decisions | Technical Users |
| **[Networking & Integration](NETWORKING_AND_INTEGRATION.md)** | Consolidated networking implementation   | Developers      |
| **[Sensor Integration](SENSOR_INTEGRATION.md)**               | Consolidated sensor implementation guide | Developers      |

### Hardware Integration

| Document                                                    | Purpose                            | Audience            |
|-------------------------------------------------------------|------------------------------------|---------------------|
| **[TC001 Testing Guide](TC001_TESTING_GUIDE.md)**           | Topdon TC001 thermal camera guide  | Hardware Developers |
| **[Samsung Stage3 RAW DNG](SAMSUNG_STAGE3_RAW_DNG.md)**     | Advanced RAW capture documentation | Camera Developers   |
| **[Stage3 Integration Guide](INTEGRATION_GUIDE_STAGE3.md)** | Developer integration guide        | System Integrators  |

### Module Documentation

| Document                                      | Purpose                           | Audience   |
|-----------------------------------------------|-----------------------------------|------------|
| **[Module Documentation](modules/README.md)** | Consolidated module documentation | Developers |

## Quick Start

### For End Users (Researchers)

1. Read the **[User Guide](USER_GUIDE.md)** for operating instructions
2. Follow the **[Installation Guide](INSTALLATION_GUIDE.md)** for setup
3. Review the **[Hardware Integration](HARDWARE_INTEGRATION.md)** guide for device setup

### For Developers

1. Follow the **[Developer Guide](DEVELOPER_GUIDE.md)** for environment setup
2. Review the **[Architecture Guide](ARCHITECTURE.md)** to understand the system design
3. Consult the **[API Reference](API_REFERENCE.md)** for implementation details
4. Use the **[Build Guide](BUILD_GUIDE.md)** for compilation and deployment

### GitHub Copilot Users

- The repository is fully optimized for **GitHub Copilot** development assistance
- See the **[Developer Guide](DEVELOPER_GUIDE.md)** for Copilot-specific setup instructions
- Project-aware context helps Copilot understand the Hub-and-Spoke architecture

## Development Tools

The repository includes comprehensive development tools accessible via:

```bash
# Show all available development commands
./dev.sh help

# Quick development tasks
./dev.sh lint          # Code linting and style checks
./dev.sh build-check    # Fast build validation
./dev.sh validate       # Comprehensive validation suite
./dev.sh diagram        # Generate architecture diagrams
```

## Repository Structure

```
IRCamera/
+-- app/                    # Main Android application
+-- pc-controller/          # Python PC Hub application  
+-- component/             # Feature components (thermal, GSR, etc.)
+-- consolidated_libraries/ # Consolidated support libraries (libcom, libmatrix, libmenu)
+-- libapp/                # Application framework library
+-- libir/                 # Core IR processing library
+-- libui/                 # User interface components library
+-- BleModule/             # Bluetooth Low Energy module
+-- docs/                  # **THIS DOCUMENTATION HUB**
+-- scripts/               # Build and utility scripts
+-- .github/               # CI/CD workflows and configurations
+-- README.md              # Project overview and quick start
```

## Support & Troubleshooting

### Common Issues

- **Build Failures**: See [Developer Guide - Known Issues & Workarounds](DEVELOPER_GUIDE.md#known-issues-workarounds)
- **Device Connection**: See [Hardware Integration](HARDWARE_INTEGRATION.md#troubleshooting)
- **PC Controller Issues**: See [User Guide](USER_GUIDE.md#troubleshooting)

### Getting Help

1. Check the relevant documentation section above
2. Review existing GitHub issues and discussions
3. Use the development tools: `./dev.sh help`
4. For new issues, provide detailed error logs and system information

## Project Status

- **Android Build**: Currently fails due to missing ShimmerDevice class -
  see [Developer Guide](DEVELOPER_GUIDE.md#known-issues)
- **PC Controller**: Fully functional MVP implementation - see [User Guide](USER_GUIDE.md)
- **Integration**: Hub-and-Spoke testing requires working Android APK
- **Documentation**: [DONE] **Fully Consolidated** (this effort)

## Contributing

Please refer to the **[Developer Guide](DEVELOPER_GUIDE.md)** for:

- Development environment setup
- Code style guidelines
- Testing procedures
- Contribution workflows

---

**Last Updated**: Current consolidation effort  
**Version**: Documentation Hub v1.0  
**Status**: ACTIVE - Active Development