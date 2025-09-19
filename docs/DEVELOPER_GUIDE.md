# IRCamera Developer Guide

## Overview

This guide provides comprehensive development instructions for the IRCamera Multi-Modal Thermal Sensing Platform. It consolidates setup procedures, development workflows, and troubleshooting information for contributors and maintainers.

## System Architecture

### Hub-and-Spoke Model

**IRCamera** implements a distributed sensing architecture:

- **Hub (PC Controller)**: Python-based central coordinator
- **Spokes (Android Sensor Nodes)**: Kotlin-based mobile sensor nodes  
- **Communication**: JSON over TCP with mDNS device discovery
- **Synchronization**: Precise temporal alignment across sensor modalities

### Repository Structure

```
IRCamera/
├── app/                           # Main Android application
│   └── src/main/java/mpdc4gsr/   # Android application code
├── pc-controller/                 # Python PC Hub implementation
│   ├── src/ircamera_pc/          # Main application package
│   ├── demo_mvp_components.py    # Component demonstration
│   ├── run_mvp_app.py            # Application launcher
│   └── requirements.txt          # Python dependencies
├── component/                     # Feature components
│   ├── thermal-ir/               # Thermal imaging component
│   ├── gsr-recording/            # GSR sensor component
│   ├── pseudo/                   # Simulation components
│   └── ...                       # Other sensor components
├── consolidated_libraries/        # Consolidated support libraries
│   ├── libcom/                   # Communication library
│   ├── libmatrix/               # Matrix operations library
│   └── libmenu/                 # Menu components library
├── libapp/                       # Application framework library
├── libir/                        # Core IR processing library  
├── libui/                        # User interface components library
├── BleModule/                    # Bluetooth Low Energy module
├── RangeSeekBar/                 # UI range selector component
├── docs/                         # **Consolidated Documentation**
├── scripts/                      # Build and utility scripts
└── .github/                      # CI/CD and GitHub configurations
```

## Development Environment Setup

### Prerequisites

#### Required Software
- **Java**: OpenJDK 8, 11, or 21 (auto-detected by Gradle)
- **Android SDK**: Android development environment
- **Python**: 3.12+ for PC Controller development
- **Git**: Version control system

#### Optional Tools
- **Android Studio**: Recommended for Android development
- **VS Code**: Optimized configuration included for multi-language development
- **PyCharm**: Python IDE alternative

### Quick Setup

#### 1. Repository Setup
```bash
# Clone repository
git clone https://github.com/uclgsr/IRCamera.git
cd IRCamera

# For VS Code users (auto-installs recommended extensions)
code .
```

#### 2. Android Development Setup
```bash
# Verify Gradle wrapper permissions
chmod +x ./gradlew

# Initial build check (Note: Currently fails due to known issues)
./dev.sh build-check

# Clean project
./gradlew clean
```

#### 3. PC Controller Setup
```bash
# Navigate to PC Controller
cd pc-controller

# Install Python dependencies
pip install PyQt6 loguru zeroconf numpy pandas h5py pyqtgraph

# Optional dependencies for full functionality
pip install scipy opencv-python bleak psutil

# Test PC Controller components
python demo_mvp_components.py

# Test basic functionality (some tests may fail due to missing features)
python test_mvp_simple.py
```

## Development Workflow

### Using Development Tools

The repository includes comprehensive development tools via `dev.sh`:

```bash
# Show all available commands
./dev.sh help

# Code quality checks
./dev.sh lint                    # Run linting checks (ktlint, checkstyle)
./dev.sh static                  # Run static analysis
./dev.sh validate                # Run all validation checks

# Build operations  
./dev.sh build-check             # Quick build validation
./dev.sh diagram                 # Generate architecture diagrams

# Usage examples with proper timeouts
./dev.sh validate                # Comprehensive validation (3+ minutes)
```

**⚠️ Important Timing Notes:**
- `./gradlew clean`: Takes 80-90 seconds, **NEVER CANCEL**
- Build attempts: 45-120 seconds, **NEVER CANCEL**  
- Always use timeout of 180+ seconds for Gradle operations

### Build System

#### Android Build (Currently Non-Functional)

**🚨 CRITICAL**: Android build currently **FAILS** due to missing ShimmerDevice class.

```bash
# Standard build attempts (will fail)
./gradlew assembleDebug          # Fails at BleModule compilation
./gradlew assembleRelease        # Fails at same point

# Enhanced build scripts (provide better error analysis)
./enhanced_build.sh              # Multiple build strategies
./build_for_testing.sh           # Most comprehensive error reporting
```

**Known Issue**: Build fails with "ShimmerDevice class not found" in:
- `BleModule/src/main/java/com/topdon/ble/ShimmerBleController.java`

#### PC Controller Build (Fully Functional)

```bash
cd pc-controller

# Test core framework (83% complete)
python demo_mvp_components.py

# Run MVP application
python run_mvp_app.py

# For headless environments
QT_QPA_PLATFORM=offscreen python run_mvp_app.py
```

### GitHub Copilot Integration

The repository is fully optimized for GitHub Copilot development assistance:

#### Core Copilot Configuration
- **`.github/copilot-instructions.md`**: Comprehensive project context
- **`.vscode/settings.json`**: Optimized VS Code settings
- **`.vscode/extensions.json`**: Recommended extensions including Copilot

#### Copilot-Aware Development

**Multi-language Support**: Optimized for Kotlin/Java (Android) and Python (PC Controller)

```kotlin
// Copilot understands project's GSR and thermal sensor integration
class GSRDataProcessor {
    // Suggestions aligned with existing Hub-and-Spoke patterns
}
```

```python
# Copilot knows about the Hub-and-Spoke architecture
class DeviceManager:
    # Context-aware suggestions for device management
```

**Build System Awareness**: Copilot understands project constraints:

```bash
# Copilot suggests appropriate timeouts for known long-running operations
./dev.sh lint                    # Known to work quickly
./gradlew clean                  # Copilot knows this takes 90+ seconds
```

#### Best Practices with Copilot
1. **Use Descriptive Comments**: Help Copilot understand your intent
2. **Reference Existing Patterns**: Copilot learns from the codebase
3. **Follow Project Conventions**: Leverage established architecture
4. **Context-Aware Naming**: Use descriptive variable and method names

## Testing & Validation

### Automated Testing

#### PC Controller Validation
```bash
cd pc-controller

# Component testing
python demo_mvp_components.py    # Demonstrates Hub-and-Spoke architecture

# Basic functionality tests
python test_mvp_simple.py        # Validates core components

# Full application test
python run_mvp_app.py             # Complete MVP functionality
```

**Test Results Status**:
- ✅ Configuration System (100%)
- ✅ Device Discovery Framework (100%)  
- ✅ Communication Protocol (100%)
- ✅ GUI Architecture (100%)
- ⚠️ Session Management (some missing methods)

#### Android Testing
**Current Status**: Cannot perform end-to-end Android testing until build issues are resolved.

```bash
# When build is fixed, install and test APK:
adb install app/build/outputs/apk/debug/app-debug.apk

# Monitor Android app logs:
adb logcat | grep IRCamera
```

### Manual Testing

#### PC Controller Integration Testing
```bash
# Test device discovery framework
python -c "
from src.ircamera_pc.network.discovery import DeviceDiscoveryService
service = DeviceDiscoveryService()
print('✅ Device discovery service loads successfully')
"

# Test session management
python -c "
from src.ircamera_pc.core.session_manager import SessionManager
manager = SessionManager()
print('✅ Session manager initialized')
"
```

#### Build Environment Validation
```bash
# Check Java version
java -version

# Check Gradle functionality
./gradlew tasks --all

# Check Python environment
python3 --version
pip list | grep PyQt6
```

## Code Quality & Standards

### Linting Configuration

#### Kotlin/Java (Android)
- **ktlint**: Kotlin code style enforcement
- **checkstyle**: Java code quality checks (currently not configured)

```bash
# Run Kotlin linting
./gradlew ktlintCheck

# Auto-fix Kotlin style issues
./gradlew ktlintFormat
```

#### Python (PC Controller)  
- **flake8**: Python code style and quality (install required)

```bash
# Install Python linting tools
pip install flake8 black

# Run Python linting (when flake8 available)
flake8 pc-controller/src/
```

### Code Organization Standards

#### Android Development
- **Module Structure**: Separate libraries for different functionalities
- **Component Architecture**: Feature-based component organization
- **Kotlin Guidelines**: Follow official Kotlin style guide
- **Resource Organization**: Structured resource management

#### Python Development
- **Package Structure**: Clean hierarchical package organization
- **Type Hints**: Use type annotations where beneficial
- **Documentation**: Comprehensive docstrings for public APIs
- **Error Handling**: Robust exception handling and logging

## Advanced Development Topics

### Native Backend Integration (PC Controller)

The PC Controller supports optional high-performance C++ backend:

```bash
cd pc-controller/native_backend

# Build native backend
mkdir -p build && cd build
cmake .. && make -j2

# Test native integration
cd .. 
python -c "
try:
    import native_gsr_processor
    print('✅ Native backend loaded successfully')
except ImportError:
    print('ℹ️ Falling back to Python implementation')
"
```

**Features**:
- High-performance GSR data processing
- Native Shimmer device support
- Automatic Python fallback if unavailable

### Communication Protocol Development

#### Protocol Specification
The system uses JSON messages over TCP for Hub-Spoke communication:

**Command Message Format**:
```json
{
  "message_id": "unique-identifier",
  "timestamp": "ISO-8601-timestamp",
  "sender_id": "pc_hub",
  "message_type": "command",
  "payload": {
    "action": "start_recording|stop_recording|sync_flash",
    "session_id": "session-identifier", 
    "configuration": {}
  }
}
```

#### Extending the Protocol
To add new command types:

1. **Define Message Type**: Add to `MessageType` enum
2. **Implement Handler**: Create handler in appropriate manager class
3. **Update Documentation**: Document new message format
4. **Test Integration**: Verify Hub-Spoke communication

### Performance Optimization

#### Android Performance
- **Memory Management**: Efficient resource cleanup
- **Threading**: Proper background processing for sensors
- **Battery Optimization**: Minimize battery drain during recording
- **Storage Efficiency**: Optimized data storage formats

#### PC Controller Performance  
- **Multi-threading**: Background network operations
- **GUI Responsiveness**: Non-blocking UI updates
- **Memory Efficiency**: Efficient data structure usage
- **Network Optimization**: Optimized TCP communication

## Known Issues & Workarounds

### Critical Issues

#### 1. Android Build Failure (BLOCKING)
**Issue**: ShimmerDevice class not found in BleModule
- **Status**: BLOCKING - no APK can be generated
- **Impact**: Cannot test Android functionality  
- **Location**: `BleModule/src/main/java/com/topdon/ble/ShimmerBleController.java`
- **Resolution**: Need to implement or import ShimmerDevice class

#### 2. PC Controller GUI (Headless Systems)
**Issue**: GUI requires display server
- **Status**: WORKING with workaround
- **Workaround**: Use `QT_QPA_PLATFORM=offscreen` for headless operation
- **Testing**: Use demo scripts for validation

#### 3. Session Management (Partial Implementation)
**Issue**: Some methods missing in SessionManager
- **Status**: PARTIAL - core functionality works
- **Impact**: Some PC controller tests fail
- **Usage**: Use working components, avoid missing methods

### Build Environment Issues

#### Gradle Configuration Cache
```bash
# If configuration cache issues occur:
./gradlew clean --no-configuration-cache --no-build-cache
```

#### Dependency Resolution
```bash
# If dependency issues occur:
./gradlew --refresh-dependencies
./gradlew clean build --no-configuration-cache
```

#### Java Version Compatibility
```bash
# Check Java version compatibility:
java -version
./gradlew --version

# If version issues, use compatible JDK:
export JAVA_HOME=/path/to/compatible/jdk
```

### Validation Workarounds

#### Build Validation Without Full Build
```bash
# Use development tools instead of full build:
./dev.sh lint                    # Code style validation
./dev.sh static                  # Static analysis
./dev.sh validate                # Comprehensive checks (except build)
```

#### PC Controller Testing Without GUI
```bash
# Test components without GUI dependencies:
cd pc-controller
python -c "
import sys
sys.path.append('src')
from ircamera_pc.core.config import ConfigurationManager
print('✅ Core modules load successfully')
"
```

## Deployment & Distribution

### Android APK Generation
**Current Status**: Not available due to build issues

```bash
# When build is fixed:
./gradlew assembleRelease         # Production APK
./gradlew assembleDebug           # Development APK

# APK location:
# app/build/outputs/apk/release/app-release.apk
# app/build/outputs/apk/debug/app-debug.apk
```

### PC Controller Distribution
```bash
cd pc-controller

# Create distributable package
python -m pip install --upgrade build
python -m build

# Or create simple distribution
zip -r ircamera-pc-controller.zip src/ requirements.txt run_mvp_app.py
```

### Docker Deployment (Future)
```dockerfile
# Potential Docker setup for PC Controller
FROM python:3.12-slim
WORKDIR /app
COPY pc-controller/requirements.txt .
RUN pip install -r requirements.txt
COPY pc-controller/ .
CMD ["python", "run_mvp_app.py"]
```

## Performance Monitoring

### Build Performance Tracking
```bash
# Monitor build times:
time ./gradlew clean             # Expect: 80-90 seconds
time ./gradlew assembleDebug     # Expect: 45-120 seconds (when working)

# Gradle performance optimization:
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"
```

### Runtime Performance Monitoring
```bash
# PC Controller performance:
python -c "
import psutil
import time
process = psutil.Process()
print(f'Memory: {process.memory_info().rss / 1024 / 1024:.1f} MB')
print(f'CPU: {process.cpu_percent()}%')
"

# Android performance monitoring:
adb shell top -p $(adb shell pidof com.topdon.ircamera)
```

## Contributing Guidelines

### Code Contribution Workflow
1. **Fork Repository**: Create personal fork on GitHub
2. **Create Branch**: Use descriptive branch names
3. **Follow Standards**: Adhere to existing code style
4. **Test Changes**: Validate with development tools
5. **Submit PR**: Provide clear description of changes

### Development Best Practices
1. **Minimal Changes**: Make surgical, precise modifications
2. **Preserve Functionality**: Don't break existing working code
3. **Test Early**: Use `./dev.sh validate` frequently
4. **Document Changes**: Update documentation for significant changes

### GitHub Copilot Best Practices
1. **Context Awareness**: Reference existing patterns in comments
2. **Architecture Alignment**: Maintain Hub-and-Spoke design principles
3. **Build Constraints**: Respect known build limitations and timeouts
4. **Error Handling**: Follow established error handling patterns

---

**Status**: ✅ Complete Developer Documentation  
**Last Updated**: Documentation Consolidation v1.0  
**Build Status**: Android (FAILING), PC Controller (WORKING)  
**Next Steps**: Resolve ShimmerDevice build issue for full functionality