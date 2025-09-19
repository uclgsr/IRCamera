[![Copilot](https://github.com/buccancs/IRCamera/actions/workflows/copilot.yml/badge.svg?branch=dev)](https://github.com/buccancs/IRCamera/actions/workflows/copilot.yml)

# IRCamera Multi-Modal Thermal Sensing Platform

A Hub-and-Spoke architecture platform for multi-modal physiological sensing with thermal imaging, GSR, and RGB data collection.

## 🤖 GitHub Copilot Support

This repository is fully configured for GitHub Copilot development assistance:

- **Multi-language Support**: Optimized for Kotlin/Java (Android) and Python (PC Controller)
- **Project-aware Context**: Copilot understands the Hub-and-Spoke architecture and build constraints
- **Ready-to-use Configurations**: VS Code settings, debug configurations, and development tasks included

**Quick Start with Copilot:**
```bash
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera
code .  # VS Code will prompt to install recommended extensions
```

For detailed setup instructions, see [COPILOT_SETUP.md](COPILOT_SETUP.md).

## 🏗️ Architecture

- **Hub (PC Controller)**: Python-based central coordinator with PyQt6 GUI
- **Spoke (Android Sensor Node)**: Kotlin-based mobile sensor nodes
- **Communication**: JSON-based TCP with mDNS discovery
- **Purpose**: Scientific data acquisition and machine learning analysis

## 📚 Documentation

**All documentation has been consolidated into the [Documentation Hub](docs/README.md) for easy navigation and maintenance.**

### Quick Start Guides
- **[User Guide](docs/USER_GUIDE.md)** - Complete operating instructions for researchers
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Development setup and GitHub Copilot integration
- **[Installation Guide](docs/README.md#quick-start)** - System setup and configuration

### Technical Documentation  
- **[API Reference](docs/API_REFERENCE.md)** - Complete module and API documentation
- **[Architecture Guide](docs/ARCHITECTURE.md)** - System architecture and design decisions
- **[Build System](scripts/build.sh)** - Unified cross-platform build script

### Development Tools
- **[Development Tools](dev.sh)** - Use `./dev.sh help` for available commands
- **[Build Script](scripts/build.sh)** - Unified build system replacing old .bat files
