#!/bin/bash

# IRCamera Documentation Generator
# Automated documentation generation from code and project structure

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

show_header() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║              📚 IRCamera Documentation Generator              ║${NC}"
    echo -e "${BLUE}║          API Docs • Architecture • User Guides               ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

generate_api_documentation() {
    echo -e "${YELLOW}📖 Generating API Documentation${NC}"
    echo "──────────────────────────────────"
    
    local docs_dir="docs/api"
    mkdir -p "$docs_dir"
    
    # Generate KDoc documentation for Kotlin files
    echo "🔍 Scanning Kotlin files for KDoc comments..."
    local kotlin_files_with_docs=0
    
    # Generate Dokka documentation if available
    if ./gradlew dokkaHtml &>/dev/null; then
        echo -e "${GREEN}✅ Dokka HTML documentation generated${NC}"
        echo "  📁 Location: build/dokka/html/"
    else
        echo -e "${YELLOW}⚠️  Dokka not configured, generating manual API docs${NC}"
        
        # Create manual API documentation
        cat > "$docs_dir/README.md" << EOF
# IRCamera API Documentation

Generated: $(date '+%Y-%m-%d %H:%M:%S')

## Project Structure

### Core Modules
$(find . -type d -name "lib*" | head -10 | sed 's/^/- /')

### Components
$(find . -type d -name "component" -exec find {} -mindepth 1 -maxdepth 1 -type d \; | head -10 | sed 's/^/- /')

## Key Classes and Interfaces

### Thermal Imaging
- **TemperatureView**: Main thermal imaging display component
- **IRCamera**: Core thermal camera interface
- **ThermalProcessor**: Image processing and analysis

### BLE Communication
- **BleModule**: Bluetooth Low Energy communication
- **SensorRecorder**: Generic sensor data recording interface

### UI Components
- **MainActivity**: Primary application interface
- **RangeSeekBar**: Custom UI component for value selection

## API Usage Examples

### Basic Thermal Camera Setup
\`\`\`kotlin
val thermalCamera = IRCamera()
thermalCamera.initialize()
thermalCamera.startCapture()
\`\`\`

### BLE Sensor Connection
\`\`\`kotlin
val bleModule = BleModule()
bleModule.connect(deviceAddress)
bleModule.startRecording()
\`\`\`

## Configuration

### Camera Settings
- Resolution: Configurable in camera module
- Frame rate: Adjustable for performance
- Temperature range: Customizable display range

### BLE Settings
- Connection timeout: 30 seconds default
- Retry attempts: 3 attempts default
- Data format: JSON/binary configurable

For detailed implementation examples, see the source code in respective modules.
EOF
    fi
    
    # Count documented classes
    local documented_classes
    documented_classes=$(find . -name "*.kt" -exec grep -l "^/\*\*\|^ \* " {} \; 2>/dev/null | wc -l)
    
    echo -e "${CYAN}API Documentation: $documented_classes classes with documentation${NC}"
}

generate_architecture_documentation() {
    echo -e "${YELLOW}🏗️  Generating Architecture Documentation${NC}"
    echo "────────────────────────────────────────────"
    
    local arch_dir="docs/architecture"
    mkdir -p "$arch_dir"
    
    # Analyze project structure
    local modules
    modules=$(find . -name "build.gradle*" -not -path "./.gradle/*" | wc -l)
    
    cat > "$arch_dir/overview.md" << EOF
# IRCamera Architecture Overview

Generated: $(date '+%Y-%m-%d %H:%M:%S')

## High-Level Architecture

IRCamera is a multi-modal physiological sensing platform built as an Android application with modular architecture.

### System Architecture

\`\`\`
┌─────────────────────────────────────────────────────────────┐
│                     IRCamera Application                    │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Activities, Fragments, Views)                   │
├─────────────────────────────────────────────────────────────┤
│  Business Logic Layer (ViewModels, Controllers)            │
├─────────────────────────────────────────────────────────────┤
│  Data Layer (Repositories, Data Sources)                   │
├─────────────────────────────────────────────────────────────┤
│  Hardware Interface Layer (Camera, BLE, Sensors)           │
└─────────────────────────────────────────────────────────────┘
\`\`\`

## Module Structure

The application consists of $modules modules:

$(find . -name "build.gradle*" -not -path "./.gradle/*" -exec dirname {} \; | sed 's|^\./||' | sort | sed 's/^/- **/' | sed 's/$/**/')

### Core Modules

#### App Module
- Main application entry point
- UI coordination and navigation
- Dependency injection setup

#### LibIR Module
- Thermal imaging core functionality
- Temperature processing and display
- Camera interface and controls

#### BLE Module
- Bluetooth Low Energy communication
- Sensor data collection and management
- Device pairing and connection handling

#### UI Libraries
- **libui**: Common UI components and utilities
- **libmenu**: Navigation and menu systems
- **RangeSeekBar**: Custom UI controls

### Data Flow

1. **Sensor Input**: Hardware sensors (thermal camera, BLE devices)
2. **Data Processing**: Real-time processing and filtering
3. **Storage**: Local database and file system
4. **Display**: UI updates and visualization
5. **Export**: Data export and sharing capabilities

## Design Patterns

### MVVM (Model-View-ViewModel)
- **Model**: Data classes and repositories
- **View**: Activities, Fragments, and custom views
- **ViewModel**: Business logic and UI state management

### Observer Pattern
- LiveData for reactive UI updates
- Event-driven sensor data processing

### Repository Pattern
- Abstraction layer for data access
- Separation of local and remote data sources

## Technology Stack

- **Language**: Kotlin (primary), Java (legacy components)
- **UI Framework**: Android Views with custom components
- **Architecture**: MVVM with Android Architecture Components
- **Concurrency**: Kotlin Coroutines
- **Database**: Room (if applicable)
- **Networking**: Retrofit (if applicable)
- **Dependency Injection**: Hilt/Dagger (if configured)

## Build System

- **Build Tool**: Gradle with Kotlin DSL
- **Build Variants**: Debug, Release, and custom variants
- **Modules**: Multi-module architecture for separation of concerns
- **Dependencies**: Centralized version management

For detailed implementation guides, see individual module documentation.
EOF

    echo -e "${GREEN}✅ Architecture documentation generated${NC}"
}

generate_user_guide() {
    echo -e "${YELLOW}👥 Generating User Guide${NC}"
    echo "─────────────────────────────"
    
    local guide_dir="docs/user-guide"
    mkdir -p "$guide_dir"
    
    cat > "$guide_dir/README.md" << EOF
# IRCamera User Guide

Welcome to IRCamera - a comprehensive thermal imaging and physiological sensing platform.

## Quick Start

### Installation
1. Download the latest APK from releases
2. Enable "Install from Unknown Sources" if needed
3. Install the application
4. Grant necessary permissions when prompted

### First Use
1. **Launch the app** - Tap the IRCamera icon
2. **Grant permissions** - Allow camera and location access
3. **Connect devices** - Pair with thermal camera and BLE sensors
4. **Start recording** - Use the record button to begin data collection

## Features

### Thermal Imaging
- **Real-time thermal display**: Live thermal camera feed
- **Temperature measurement**: Point and area temperature readings
- **Image capture**: Save thermal images for analysis
- **Temperature range adjustment**: Customize display range

#### Using Thermal Camera
1. Connect your Topdon TC001 thermal camera via USB
2. Open the thermal imaging view
3. Adjust temperature range using the controls
4. Tap to measure point temperatures
5. Use gesture controls for zoom and pan

### BLE Sensor Recording
- **Device discovery**: Automatic BLE device scanning
- **Multi-sensor support**: Connect multiple sensors simultaneously
- **Real-time data**: Live sensor data visualization
- **Data export**: Export recorded data in various formats

#### Connecting BLE Sensors
1. Ensure Bluetooth is enabled
2. Tap "Scan for Devices" in the BLE section
3. Select your sensor from the discovered devices list
4. Wait for connection confirmation
5. Start recording sensor data

### Data Management
- **Local storage**: All data saved locally on device
- **Export options**: CSV, JSON, and binary formats
- **Sync capabilities**: Cloud backup (if configured)
- **Data visualization**: Built-in charts and analysis tools

## Settings and Configuration

### Camera Settings
- **Resolution**: Choose image quality vs. performance
- **Frame rate**: Adjust for smooth display or battery life
- **Color palette**: Select thermal color scheme
- **Measurement units**: Celsius, Fahrenheit, or Kelvin

### BLE Settings
- **Scan duration**: How long to search for devices
- **Connection timeout**: Time to wait for connection
- **Data rate**: Sensor sampling frequency
- **Auto-reconnect**: Automatic reconnection on disconnect

### Privacy and Security
- **Data encryption**: Local data protection
- **Permission management**: Control app access
- **Data sharing**: Configure cloud sync preferences

## Troubleshooting

### Common Issues

#### Thermal Camera Not Detected
- Check USB connection
- Verify camera compatibility
- Restart the application
- Check USB debugging settings

#### BLE Connection Problems
- Ensure Bluetooth is enabled
- Check device proximity
- Verify sensor battery level
- Clear Bluetooth cache if needed

#### Performance Issues
- Reduce image resolution
- Lower frame rate
- Close unnecessary background apps
- Restart device if needed

### Getting Help
- Check the FAQ section
- Review logs in app settings
- Contact support with error details
- Submit bug reports through the app

## Advanced Features

### Data Analysis
- **Statistical analysis**: Built-in data processing tools
- **Export capabilities**: Integration with external analysis tools
- **Visualization**: Graphs, heatmaps, and trend analysis

### Integration
- **API access**: For developers and researchers
- **Plugin system**: Extend functionality with custom modules
- **Data formats**: Support for standard scientific formats

For technical support or feature requests, please visit our GitHub repository.
EOF

    echo -e "${GREEN}✅ User guide generated${NC}"
}

generate_developer_documentation() {
    echo -e "${YELLOW}👨‍💻 Generating Developer Documentation${NC}"
    echo "─────────────────────────────────────────"
    
    local dev_dir="docs/development"
    mkdir -p "$dev_dir"
    
    cat > "$dev_dir/setup.md" << EOF
# Developer Setup Guide

## Prerequisites

### Required Tools
- **Android Studio**: Latest stable version
- **JDK**: Java 11 or higher
- **Kotlin**: 1.8.0 or higher
- **Git**: For version control

### Recommended Tools
- **ktlint**: Code formatting
- **detekt**: Static analysis
- **Dokka**: Documentation generation

## Project Setup

### 1. Clone Repository
\`\`\`bash
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera
\`\`\`

### 2. Setup Development Environment
\`\`\`bash
./dev.sh setup
\`\`\`

### 3. Build Project
\`\`\`bash
./dev.sh build
\`\`\`

### 4. Run Tests
\`\`\`bash
./dev.sh test
\`\`\`

## Development Workflow

### Code Standards
- Follow Kotlin coding conventions
- Use KDoc for public APIs
- Write unit tests for new features
- Run formatters before committing

### Git Workflow
\`\`\`bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes and commit
git add .
git commit -m "feat: add new feature"

# Run validation before push
./dev.sh validate

# Push changes
git push origin feature/new-feature
\`\`\`

### Code Review Checklist
- [ ] Code follows project conventions
- [ ] Tests added for new functionality
- [ ] Documentation updated
- [ ] No lint errors
- [ ] Build passes

## Architecture Guidelines

### Module Structure
- Keep modules focused and cohesive
- Minimize dependencies between modules
- Use dependency injection where appropriate

### Testing Strategy
- Unit tests for business logic
- Integration tests for components
- UI tests for critical user flows

### Performance Considerations
- Profile thermal imaging performance
- Optimize BLE communication
- Monitor memory usage

## Debugging

### Common Issues
- USB thermal camera permissions
- BLE connection stability
- Performance optimization

### Useful Commands
\`\`\`bash
# Debug build with logging
./gradlew :app:assembleDebug -Dorg.gradle.debug=true

# Run specific tests
./gradlew :module:testDebugUnitTest --tests "TestClass"

# Generate coverage report
./gradlew jacocoTestReport
\`\`\`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests and documentation
5. Submit a pull request

For questions, please open an issue or contact the maintainers.
EOF

    echo -e "${GREEN}✅ Developer documentation generated${NC}"
}

update_readme() {
    echo -e "${YELLOW}📝 Updating Project README${NC}"
    echo "──────────────────────────────"
    
    # Check if README needs updating
    if [ -f "README.md" ]; then
        echo "📄 Existing README.md found"
        
        # Add documentation links if not present
        if ! grep -q "docs/" README.md; then
            echo "" >> README.md
            echo "## Documentation" >> README.md
            echo "" >> README.md
            echo "- [API Documentation](docs/api/)" >> README.md
            echo "- [Architecture Overview](docs/architecture/)" >> README.md
            echo "- [User Guide](docs/user-guide/)" >> README.md
            echo "- [Developer Setup](docs/development/)" >> README.md
            echo "" >> README.md
            
            echo -e "${GREEN}✅ Documentation links added to README${NC}"
        else
            echo -e "${CYAN}ℹ️  Documentation links already present${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  No README.md found${NC}"
    fi
}

generate_changelog() {
    echo -e "${YELLOW}📋 Generating Changelog${NC}"
    echo "─────────────────────────"
    
    cat > "CHANGELOG.md" << EOF
# Changelog

All notable changes to the IRCamera project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive CI/CD pipeline with GitHub Actions
- Advanced development tools and automation scripts
- Quality monitoring and performance analysis
- Security scanning and vulnerability detection
- Automated documentation generation
- Enhanced testing suite with coverage reporting

### Changed
- Streamlined development workflow with unified dev.sh script
- Improved code formatting and linting standards
- Enhanced pre-commit hooks and validation

### Security
- Added security scanning for dependencies and code
- Implemented Android security best practices analysis
- Enhanced permission and configuration reviews

## Previous Releases

For information about previous releases, see the git commit history:
\`\`\`bash
git log --oneline --decorate
\`\`\`

---

## Release Process

1. Update version in build.gradle.kts
2. Update this changelog
3. Create release tag: \`git tag -a v1.0.0 -m "Release v1.0.0"\`
4. Push tag: \`git push origin v1.0.0\`
5. GitHub Actions will create the release automatically

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on contributing to this project.
EOF

    echo -e "${GREEN}✅ Changelog generated${NC}"
}

# Main execution
main() {
    show_header
    
    local start_time end_time total_duration
    start_time=$(date +%s)
    
    echo -e "${PURPLE}📚 Starting Documentation Generation${NC}"
    echo ""
    
    # Generate all documentation
    generate_api_documentation
    echo ""
    
    generate_architecture_documentation
    echo ""
    
    generate_user_guide
    echo ""
    
    generate_developer_documentation
    echo ""
    
    update_readme
    echo ""
    
    generate_changelog
    echo ""
    
    end_time=$(date +%s)
    total_duration=$((end_time - start_time))
    
    echo -e "${GREEN}📖 Documentation generation completed in ${total_duration}s${NC}"
    echo ""
    echo -e "${CYAN}📚 Generated Documentation:${NC}"
    echo "  • API Documentation: ./docs/api/"
    echo "  • Architecture: ./docs/architecture/"
    echo "  • User Guide: ./docs/user-guide/"
    echo "  • Developer Setup: ./docs/development/"
    echo "  • Project Changelog: ./CHANGELOG.md"
    echo ""
    echo -e "${YELLOW}💡 Next Steps:${NC}"
    echo "  • Review generated documentation for accuracy"
    echo "  • Add project-specific details and examples"
    echo "  • Configure Dokka for automated API docs"
    echo "  • Set up documentation hosting (GitHub Pages)"
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi