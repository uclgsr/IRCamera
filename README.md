[![Copilot](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml/badge.svg?branch=dev)](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml)

# IRCamera Multi-Modal Thermal Sensing Platform

A Hub-and-Spoke architecture platform for multi-modal physiological sensing with thermal imaging, GSR, and RGB data
collection.

## GitHub Copilot Support

This repository is fully configured for GitHub Copilot development assistance:

- **Multi-language Support**: Optimized for Kotlin/Java (Android) and Python (PC Controller)
- **Project-aware Context**: Copilot understands the Hub-and-Spoke architecture and build constraints
- **Ready-to-use Configurations**: VS Code settings, debug configurations, and development tasks included

**Quick Start with Copilot:**

```bash
git clone https://github.com/uclgsr/IRCamera.git
cd IRCamera
code .  # VS Code will prompt to install recommended extensions
```

For detailed setup instructions, see [COPILOT_SETUP.md](COPILOT_SETUP.md).

## Architecture

- **Hub (PC Controller)**: Python-based central coordinator with PyQt6 GUI
- **Spoke (Android Sensor Node)**: Kotlin-based mobile sensor nodes
- **Communication**: JSON-based TCP with mDNS discovery
- **Purpose**: Scientific data acquisition and machine learning analysis

### Simplified Structure (Updated 2024)

This project has been significantly simplified for better maintainability:

- **PC Controller**: Reduced from 2000+ lines to 250 lines (87% reduction)
- **Library Modules**: Consolidated from 6 to 3 libraries (50% reduction)
- **Build System**: Reduced from 18 to 11 gradle files (39% reduction)

See [SIMPLIFIED_ARCHITECTURE.md](SIMPLIFIED_ARCHITECTURE.md) for complete details.

## Documentation

- [Simplified Architecture](SIMPLIFIED_ARCHITECTURE.md) - Current simplified project structure
- [Copilot Setup Guide](COPILOT_SETUP.md) - GitHub Copilot configuration and usage
- [PC Controller Hub](pc-controller/) - Complete Hub implementation with MVP and GUI options
- [Samsung Stage3/Level3 RAW DNG](docs/SAMSUNG_STAGE3_RAW_DNG.md) - Advanced RAW capture documentation
- [Stage3 Integration Guide](docs/INTEGRATION_GUIDE_STAGE3.md) - Developer integration guide
- [Development Tools](dev.sh) - Use `./dev.sh help` for available commands
- [Project Documentation](docs/) - Comprehensive technical documentation

## Key Features

### Advanced Camera Capabilities

- **Samsung Stage3/Level3 RAW DNG Recording**: Direct access to Samsung's image processing pipeline for maximum raw data
  preservation
- **Multi-Modal Synchronization**: Synchronized RGB, thermal, and GSR data collection
- **Scientific Data Quality**: 12-bit RAW sensor data with minimal processing interference
- **Flexible Processing**: Toggle between Standard and Samsung Stage3/Level3 processing modes

### Hub-and-Spoke Architecture

- **PC Controller Hub**: Central coordination and data aggregation
- **Android Sensor Nodes**: Mobile data collection points
- **TCP/mDNS Communication**: Automatic discovery and reliable data streaming
- **Session Management**: Comprehensive recording session control

## Changelog

### 2024-01-XX - TS004Repository Import Fix
- Fixed missing TS004Repository import compilation errors
- Removed TS004Repository dependencies from thermal-ir module files:
  - IRVideoGSYActivity.kt
  - IRGalleryFragment.kt
  - IRGalleryViewModel.kt
- Added appropriate fallback behavior for removed functionality
- All TS004Repository functionality has been systematically removed from the codebase
