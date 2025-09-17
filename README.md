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

- [Copilot Setup Guide](COPILOT_SETUP.md) - GitHub Copilot configuration and usage
- [PC Controller MVP](pc-controller/README_MVP.md) - Hub implementation details
- [Development Tools](dev.sh) - Use `./dev.sh help` for available commands
- [Project Documentation](docs/) - Comprehensive technical documentation
