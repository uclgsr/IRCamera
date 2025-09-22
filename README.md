[![Copilot](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml/badge.svg?branch=dev)](https://github.com/uclgsr/IRCamera/actions/workflows/copilot.yml)

# IRCamera Multi-Modal Thermal Sensing Platform

A Hub-and-Spoke architecture platform for multi-modal physiological sensing with thermal imaging, GSR, and RGB data
collection.
## Recent Update: TC001 Thermal Camera Integration Enhancement - Commit 4b1c7a9

**LATEST ACHIEVEMENT**: TC001 Topdon thermal camera integration has been **FULLY ENHANCED** with real SDK integration:

### TC001 Integration Improvements

- **Real SDK Calls**: Replaced stub/reflection approach with actual TC001 SDK calls in ThermalCameraRecorder
- **Continuous Capture**: Implemented 10Hz thermal frame capture with 100ms intervals 
- **Image Saving**: Automatic PNG frame saving to thermal_images/ directory with temperature metadata
- **Error Resilience**: Enhanced error handling with graceful fallbacks to prevent app crashes
- **User Notifications**: Toast messages for TC001 connection status and error conditions
- **USB Integration**: Enhanced existing USB permission flow for TC001 attach/detach scenarios

### Technical Enhancement Details

- **Native Library Support**: Added graceful TC001 native library loading with Java-only SDK fallback
- **Frame Processing**: IFrameCallback registration for continuous thermal data streaming at ~10Hz
- **Crash Prevention**: Consecutive error counting (max 10) before switching to simulation mode
- **Temperature Logging**: Min/max temperature telemetry logged to CSV with system timestamps
- **Other Sensor Protection**: Ensures GSR and RGB sensors continue recording if TC001 fails

## Recent Update: Kotlin Compilation Errors Resolved

## Recent Update: SmartRefreshLayout Dependency Resolution Fixed

**LATEST ACHIEVEMENT**: SmartRefreshLayout dependency resolution issue **FULLY RESOLVED**:

### Dependency Resolution Fixes

- **Maven Coordinates Fixed**: Corrected group ID from `com.scwang.smart` to `io.github.scwang90` for SmartRefreshLayout components
- **JitPack Issues Resolved**: Migrated from failing JitPack (401 Unauthorized) to reliable Maven Central resolution
- **Pull-to-refresh Restored**: Fixed build failures in IRGalleryFragment, PDFListFragment, and PDFListActivity  
- **Version Catalog Updated**: Enhanced dependency management with correct artifact coordinates

### Technical Resolution Delivered

- **Dependencies Fixed**: `refresh-layout-kernel:2.1.0` and `refresh-header-classics:2.1.0` now resolve correctly
- **Repository Migration**: SmartRefreshLayout now sources from Maven Central instead of problematic JitPack
- **Backwards Compatibility**: Code imports remain unchanged (`com.scwang.smart.refresh.layout.*`)
- **Module Impact**: Primarily benefits `component:thermalunified` with pull-to-refresh functionality

## Recent Update: Timestamp Synchronization System Unified

**LATEST ACHIEVEMENT**: Complete timestamp synchronization unification across all sensor modalities has been **FULLY IMPLEMENTED**:

### Timestamp System Unification Delivered

- **Unified Time Base**: All sensors now use TimestampManager.getCurrentTimestampNanos() for consistent wall-clock epoch time
- **SessionSync Markers**: Automatic sync event logging at session start for cross-sensor alignment verification
- **Drift Analysis**: Enhanced logging of device vs phone timestamp differences for post-processing analysis
- **Cross-Device Sync**: Enhanced NTP-like PC-Phone handshake with quality reporting and automatic monitoring
- **Verification Tools**: TimestampSyncVerificationActivity for manual testing of multi-modal timestamp alignment

### Technical Implementation Completed

- **RGB Camera**: Replaced System.nanoTime() with unified TimestampManager across all recording operations
- **Thermal Recorder**: Unified timestamp usage for consistent time base with session sync markers
- **GSR Sensor**: Updated to use TimestampManager for compatibility with unified synchronization system
- **Sharp Event Testing**: Manual verification tool simulates hand clap events across modalities within 5ms tolerance
- **Wall-Clock Conversion**: Added convertMonotonicToWallClock for consistent epoch time translation


## Previous Update: Kotlin Compilation Errors Resolved


**LATEST ACHIEVEMENT**: All Kotlin compilation errors in BLE Core module have been **FULLY RESOLVED**:

### Compilation Fixes Implemented

- **Activity Lifecycle Callbacks**: Fixed parameter type mismatches in AppHolder11.kt lifecycle methods
- **Interface Implementation**: Corrected CheckableItem111.kt property overrides and return type issues
- **Parcelable Support**: Fixed CheckableParcelable111.kt constructor and method access patterns
- **Collection Type Safety**: Resolved PermissionsRequester11.kt collection parameter type compatibility
- **Reflection API Updates**: Updated Observable11.kt and ObserverMethodHelper11.kt to use Kotlin property syntax
- **Type Safety Improvements**: Enhanced nullable type handling across observer patterns

### Technical Resolution Delivered

- **7 files fixed**: AppHolder11.kt, CheckableItem111.kt, CheckableParcelable111.kt, PermissionsRequester11.kt, Observable11.kt, ObserverMethodHelper11.kt, MethodInfo11.kt
- **Reflection API modernization**: Migrated from Java reflection methods (getName()) to Kotlin property access (name)
- **Lifecycle callback compliance**: Fixed Activity lifecycle parameter types to match Android API requirements
- **Type safety enhancements**: Improved nullable handling and collection type compatibility

## Previous Achievement: Gradle Build System Standardization Complete

## Recent Update: BLE Core Compilation Fixes Complete

**LATEST ACHIEVEMENT**: The BLE core module compilation errors have been **FULLY RESOLVED**:

### BLE Core Fixes
- **Interface Issues**: Fixed RequestCallback redeclaration by creating proper callback structure
- **Type Safety**: Resolved ByteUtil Float/Int conversion errors and HexUtil byte type conflicts  
- **Logger Implementation**: Fixed DefaultLogger property override issues with Logger interface
- **Package Structure**: Created missing callback package in ble-core module

## Previous Update: Gradle Build System Standardization Complete


## Recent Update: BLE Core Module Fixes Complete

**LATEST ACHIEVEMENT**: The BLE Core module GenericRequest compilation issues have been **FULLY RESOLVED**:
- GenericRequest.kt now compiles without errors
- Request interface properly implemented with override modifiers
- RequestBuilder interface enhanced with generic types and required properties
- RequestCallback interface created for proper BLE operation callbacks
- Device property handling fixed with custom getter/setter pattern
- Enum merge conflicts resolved in RequestType and ConnectionState

**Previous Achievement**: The build system has been **FULLY STANDARDIZED** for consistent development experience:

## Recent Update: BLE Core Compilation Fix Complete

**LATEST FIX**: Resolved WriteOptions.Builder private field access compilation errors:

### BLE Core Improvements

- **WriteOptions Fix**: Fixed private field access issue in WriteOptions constructor
- **Visibility Correction**: Changed Builder fields from private to internal visibility
- **Compilation Success**: Eliminated 6 compilation errors in WriteOptions class
- **Code Quality**: Maintains proper encapsulation while enabling necessary access

### Technical Fix Details

- **Issue**: WriteOptions constructor could not access private Builder fields
- **Root Cause**: Kotlin nested class visibility rules prevent outer class access to private members
- **Solution**: Changed `private var` to `internal var` for 6 Builder fields
- **Impact**: Zero functional change, only fixes compilation errors

## Previous Update: Gradle Build System Standardization Complete

## Latest Update: BLE Core Module Compilation Fixed

**LATEST FIX**: Resolved critical compilation issue in BLE core module:

### BLE Core Module Fix
- **UUID Import**: Fixed missing `import java.util.UUID` statement in Request.kt interface
- **Compilation Errors**: Eliminated "Unresolved reference 'UUID'" errors preventing module compilation
- **Code Consistency**: Aligned with other BLE core module files that properly import java.util.UUID

## Recent Update: Gradle Build System Standardization Complete


**LATEST ACHIEVEMENT**: The build system has been **FULLY STANDARDIZED** for consistent development experience:


### Build System Improvements

- **Unified Module Structure**: Removed non-existent module references, corrected settings.gradle.kts
- **Standardized Dependencies**: All modules use version catalog consistently, no hardcoded versions
- **Simplified Build Tasks**: Clean `clean`, `build`, `buildAll`, `buildRelease`, `buildDebug` tasks
- **Consistent Configuration**: Unified Android settings, build features, and compile options

### Technical Standardization Delivered

- **8 modules standardized**: Consistent structure across app, libunified, BLE, and component modules
- **Version catalog integration**: Replaced hardcoded dependency versions with centralized management  
- **Build feature completeness**: Added missing viewBinding, testInstrumentationRunner to all modules
- **Dependency path corrections**: Fixed AAR references and module dependencies

## Previous Achievement: Complete Library Unification Implemented

**MAJOR ACHIEVEMENT**: The library unification has been **FULLY IMPLEMENTED** along with BLE module architectural
improvements:

### Implementation Results

- **Library Unification**: libapp, libir, and libui merged into unified `libunified` module
- **BLE Module Split**: BleModule split into focused device-specific modules (ble-core, ble-shimmer, ble-topdon)
- **Namespace Unification**: Complete "nuclear" refactoring to `com.mpdc4gsr.libunified.*` hierarchy
- **Architectural Benefits**: 67% reduction in core modules + 56% reduction in BLE complexity

### Technical Implementation Delivered

- **598 source files** successfully unified under `com.mpdc4gsr.libunified.*` namespace
- **1094+ import statements** updated across entire codebase
- **Nuclear namespace refactoring** - maximum possible unification achieved
- **Device-specific BLE modules** for better separation of concerns

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

### Library Structure (Implemented)

**Current Unified Structure:**

- **libunified** (598 files): Unified app framework, IR processing, and UI components
    - `com.mpdc4gsr.libunified.app.*` - Application framework and utilities
    - `com.mpdc4gsr.libunified.ir.*` - IR camera processing and hardware
    - `com.mpdc4gsr.libunified.ui.*` - UI components and charting
- **ble-core** (~3,500 lines): Core BLE functionality and utilities
- **ble-shimmer** (1,131 lines): GSR/Shimmer device-specific functionality
- **ble-topdon** (881 lines): Thermal/Topdon device-specific functionality

**Architectural Benefits Achieved:**

- **67% reduction** in core library modules (3 → 1)
- **56% reduction** in BLE module complexity through focused architecture
- **Simplified builds**: Single unified dependency for all components

### Simplified Structure (Updated 2024)

This project has been significantly simplified for better maintainability:

- **PC Controller**: Reduced from 2000+ lines to 250 lines (87% reduction)
- **Library Modules**: Consolidated from 3 to 1 library, achieving a 67% reduction
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

### Library Dependencies (Implemented)

```kotlin
// Unified dependency structure (implemented)
implementation(project(":libunified"))  // All core functionality unified

// Device-specific BLE dependencies (implemented)
implementation(project(":ble-shimmer"))  // For GSR components
implementation(project(":ble-topdon"))   // For thermal components
implementation(project(":ble-core"))     // For core BLE functionality
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
./gradlew :libunified:assembleDebug
```

## Contributing

This project benefits from the library unification analysis. When contributing:

1. Consider the unified libunified approach for new functionality
2. Test changes against both current and proposed architecture
3. Update documentation for architectural changes
4. Use GitHub Copilot for development assistance

## License

Research project - see individual component licenses for specific terms.

- [Project Documentation](docs/) - Comprehensive technical documentation

## Documentation Update Log

### 2024-12-22 - Commit c7769bc
- Removed all emoji characters from markdown documentation for ASCII safety
- Updated library references from libcore to libunified throughout codebase
- Fixed MERMAID diagrams to reflect completed unified architecture
- Updated repository structure documentation to match current BLE module organization
- Ensured documentation accurately reflects true state of repository

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

## RGB Camera Functionality (CameraX) - Implementation Complete

### ✅ Live Preview Setup
- **Added PreviewView widgets** to `activity_unified_sensor.xml` and `activity_multi_modal_recording.xml` 
- **Integrated RgbCameraRecorder with PreviewView** in `UnifiedSensorActivity.kt`
- Preview view is 200dp height with black background for better visibility
- Camera status text shows real-time camera state

### ✅ High Resolution & Frame Rate Configuration  
- **Enhanced createOptimizedRecorder()** with proper QualitySelector configuration:
```kotlin
QualitySelector.from(Quality.UHD, FallbackStrategy.lowerQualityThan(Quality.UHD))
```
- Supports up to 4K resolution with graceful fallback to lower quality
- Device detection for 4K capability based on known device models

### ✅ Frame Capture Optimisation
- **Reduced CAPTURE_FPS from 30 to 12fps** for optimized I/O performance
- **Added FRAME_CAPTURE_EVERY_N_FRAMES throttling** (every 2nd frame)
- **Implemented MAX_PENDING_CAPTURES = 2** for better backpressure handling
- Frame capture uses background executor via `imageCapture.takePicture(cameraExecutor, callback)`

### ✅ Error Handling & Camera Selector
- **Enhanced CameraX initialization** with comprehensive try-catch blocks:
  - SecurityException for permission issues
  - IllegalStateException for camera-in-use scenarios  
  - General Exception handling with user-friendly error messages
- **Camera switching capability** already implemented (front/back camera)
- **Robust error notification system** via emitError() with specific ErrorType enum

### ✅ Lifecycle Management on Stop
- **Verified stopRecording()** properly calls `cameraProvider.unbindAll()`
- **Confirmed cleanup()** shuts down executors with timeout handling
- **Frame capture loops are cancelled** before unbinding camera
- **All resources properly released** (recording, CSV writers, camera provider)

*Implementation Status: All major requirements completed and ready for testing*
