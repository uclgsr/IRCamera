[![Copilot](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml/badge.svg?branch=dev)](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml)

# IRCamera Multi-Modal Thermal Sensing Platform

A Hub-and-Spoke architecture platform for multi-modal physiological sensing with thermal imaging, GSR, and RGB data
collection.

## Recent Update: Library Unification Analysis

**MAJOR FINDING**: The possibility of merging libapp, libir, and libui into a unified library has been **CONFIRMED AS FEASIBLE** with significant benefits:

### Unification Benefits
- **Simplified Dependencies**: 3 library modules → 1 unified libcore module
- **Cleaner Architecture**: Single dependency for all components  
- **Build Performance**: Reduced module overhead and faster compilation
- **Maintenance**: Centralized configuration and dependency management

### Technical Analysis Results
- **598 source files** successfully merged without namespace conflicts
- **No code changes required** - different root packages (com.mpdc4gsr.lib.core.*, com.infisense.usbir.*, com.github.mikephil.charting.*)
- **Resource conflicts resolved** - strings and dimensions merged successfully
- **Proof-of-concept libcore created** with unified build configuration

See [CHANGELOG.md](CHANGELOG.md) and [BACKLOG.md](BACKLOG.md) for detailed analysis and implementation roadmap.

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

### Library Structure (Current + Proposed)

**Current Structure:**
- **libapp** (247 files): Application framework, database, configuration
- **libir** (64 files): IR camera processing and hardware abstraction  
- **libui** (287 files): UI components and charting library

**Proposed Unified Structure:**
- **libcore** (598 files): All functionality unified in single module
- **Benefits**: Simplified builds, cleaner dependencies, easier maintenance

### Simplified Structure (Updated 2024)

This project has been significantly simplified for better maintainability:

- **PC Controller**: Reduced from 2000+ lines to 250 lines (87% reduction)
- **Library Modules**: Analysis complete for consolidation from 3 to 1 library (67% reduction potential)
- **Build System**: Reduced from 18 to 11 gradle files (39% reduction)

See [SIMPLIFIED_ARCHITECTURE.md](SIMPLIFIED_ARCHITECTURE.md) for complete details.

## Build System

### Current Build Commands
```bash
./dev.sh help              # Show all available commands
./dev.sh lint              # Run code linting
./dev.sh build-check       # Quick build validation
./dev.sh validate          # Comprehensive validation
```

### Library Dependencies (Current)
```kotlin
// Component dependencies (all components need all three libraries)
implementation(project(":libapp"))   // Application framework
implementation(project(":libir"))    // IR processing  
implementation(project(":libui"))    // UI components
```

### Proposed Unified Dependencies
```kotlin
// Simplified unified dependency (proposed)
implementation(project(":libcore"))  // All functionality unified
```

## Documentation

- [Simplified Architecture](SIMPLIFIED_ARCHITECTURE.md) - Current simplified project structure
- [Library Unification Analysis](CHANGELOG.md) - Feasibility study and technical analysis
- [Implementation Backlog](BACKLOG.md) - Roadmap for library unification
- [Copilot Setup Guide](COPILOT_SETUP.md) - GitHub Copilot configuration and usage
- [PC Controller Hub](pc-controller/) - Complete Hub implementation with MVP and GUI options
- [Samsung Stage3/Level3 RAW DNG](docs/SAMSUNG_STAGE3_RAW_DNG.md) - Advanced RAW capture documentation
- [Stage3 Integration Guide](docs/INTEGRATION_GUIDE_STAGE3.md) - Developer integration guide
- [Development Tools](dev.sh) - Use `./dev.sh help` for available commands

## Quick Development Setup

```bash
# Clone repository
git clone https://github.com/uclgsr/IRCamera.git
cd IRCamera

# Build current system
./gradlew clean build

# Run validation
./dev.sh validate

# Test library unification (experimental)
./gradlew :libcore:assembleDebug
```

## Contributing

This project benefits from the library unification analysis. When contributing:

1. Consider the unified libcore approach for new functionality
2. Test changes against both current and proposed architecture
3. Update documentation for architectural changes
4. Use GitHub Copilot for development assistance

## License

Research project - see individual component licenses for specific terms.
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
