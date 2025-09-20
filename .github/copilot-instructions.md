# IRCamera Multi-Modal Thermal Sensing Platform

NEVER USE EMOJIS. ONLY ASCI SAFE

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected
information that does not match the info here.

## Working Effectively

### Bootstrap and Build Process

- **CRITICAL**: Android build currently FAILS due to missing ShimmerDevice class in BleModule. Do not attempt full
  builds without addressing this.
- Clean project: `./gradlew clean` -- takes ~1m23s. NEVER CANCEL. Set timeout to 180+ seconds.
- Build attempts: `./gradlew assembleDebug` or `./gradlew assembleRelease` -- takes ~1-2 minutes. NEVER CANCEL. Set
  timeout to 300+ seconds.
- **KNOWN ISSUE**: Build fails with "ShimmerDevice class not found" errors in
  BleModule/src/main/java/com/topdon/ble/ShimmerBleController.java
- **WORKAROUND**: Use enhanced build scripts that attempt multiple strategies:
    - `./enhanced_build.sh` -- tries multiple build approaches with fallbacks
    - `./build_for_testing.sh` -- comprehensive build script with error analysis
    - Both scripts take 1-2 minutes. NEVER CANCEL. Set timeout to 300+ seconds.

### PC Controller Setup

- Navigate to pc-controller directory: `cd pc-controller`
- Install dependencies:
  `pip3 install PyQt6 loguru zeroconf numpy pandas h5py pyqtgraph scipy opencv-python bleak psutil`
- Test functionality: `python3 demo_mvp_components.py` -- demonstrates Hub-and-Spoke architecture
- **GUI Mode**: Requires display server. For headless: `QT_QPA_PLATFORM=offscreen python3 run_mvp_app.py`
- **MVP Test**: `python3 test_mvp_simple.py` -- validates core components (some tests expect missing features)

### Development Tools

- Linting and validation: `./dev.sh help` shows available commands
- Quick validation: `./dev.sh build-check` -- fast build validation (still fails due to ShimmerDevice issue)
- Full validation: `./dev.sh validate` -- runs lint, static analysis, and build checks
- Generate docs: `./dev.sh diagram` -- creates architecture diagrams

## Validation

### Android Build Status

- **CURRENT STATE**: Android build FAILS consistently
- **ERROR**: Cannot find symbol ShimmerDevice in BleModule
- **IMPACT**: APK cannot be generated until BleModule compilation issues are resolved
- **TESTING**: Cannot perform end-to-end Android testing until build succeeds
- **TIMELINE**: Build failures occur after ~45-60 seconds of compilation

### PC Controller Validation

- **FUNCTIONAL**: Core framework 83% complete based on demo_mvp_components.py
- **WORKING**: Configuration system, device discovery framework, communication protocol, GUI architecture
- **ISSUES**: Session management has some missing methods (get_session), GUI requires display server
- **TESTING**: Use demo scripts and simple tests to validate functionality

### Manual Testing Requirements

- **Android**: Cannot install/test until build issues resolved
- **PC Controller**: Test with `demo_mvp_components.py` and component tests
- **Integration**: Hub-and-Spoke testing requires working Android APK

## Build Timing and Timeouts

### Critical Timing Information

- **Gradle clean**: 80-90 seconds. Set timeout to 180+ seconds. NEVER CANCEL.
- **Android build attempts**: 45-120 seconds before failure. Set timeout to 300+ seconds. NEVER CANCEL.
- **PC dependency installation**: 30-60 seconds for full requirements. Set timeout to 120+ seconds.
- **Configuration phase**: 15-20 seconds for Gradle configuration cache.

### Build Strategies (All Currently Fail)

1. **Standard build**: `./gradlew assembleRelease` - Fails at BleModule compilation
2. **Debug build**: `./gradlew assembleDebug` - Fails at same point
3. **Enhanced script**: `./enhanced_build.sh` - Tries multiple approaches, all fail
4. **Testing script**: `./build_for_testing.sh` - Most comprehensive, provides detailed error analysis

## Common Tasks

### Repository Structure (Key Locations)

```
IRCamera/
+-- app/                    # Main Android application
+-- BleModule/             # BLE/Shimmer integration (BROKEN - missing ShimmerDevice)
+-- component/             # Feature components (thermal, GSR, pseudo, etc.)
+-- consolidated_libraries/ # Consolidated support libraries (libcom, libmatrix, libmenu)
+-- libapp/                # Application framework library
+-- libir/                 # Core IR processing library
+-- libui/                 # User interface components library
+-- pc-controller/         # Python PC Hub application (WORKING)
+-- scripts/               # Build and utility scripts
+-- dev.sh                # Development tools
+-- docs/                 # Comprehensive documentation
+-- .github/              # CI/CD workflows
```

### Frequently Used Commands

- `./gradlew clean` -- NEVER CANCEL, 90s timeout minimum
- `./dev.sh build-check` -- Quick build validation
- `cd pc-controller && python3 demo_mvp_components.py` -- Test PC controller
- `find . -name "*.apk"` -- Locate generated APKs (none currently due to build failure)

### Documentation Locations

- `pc-controller/README_MVP.md` -- Comprehensive PC controller documentation
- `docs/modules/` -- Individual component documentation
- `docs/latex/appendix_A.tex` -- Installation and setup procedures
- `.github/workflows/super-linter.yml` -- CI linting configuration

## Troubleshooting

### Known Issues and Status

1. **Android Build Failure**: ShimmerDevice class not found in BleModule
    - **Status**: BLOCKING - no APK can be generated
    - **Impact**: Cannot test Android functionality
    - **Resolution**: Need to implement or import ShimmerDevice class

2. **PC Controller GUI**: Requires display server
    - **Status**: WORKING with workaround
    - **Workaround**: Use `QT_QPA_PLATFORM=offscreen` for headless operation
    - **Testing**: Use demo scripts for validation

3. **Incomplete Session Management**: Some methods missing in SessionManager
    - **Status**: PARTIAL - core functionality works
    - **Impact**: Some PC controller tests fail
    - **Usage**: Use working components, avoid missing methods

### Build Environment Requirements

- **Java**: OpenJDK 8, 11, or 21 (auto-detected)
- **Gradle**: 8.14 (auto-downloaded)
- **Android SDK**: Required for Android builds (dependency downloads indicate presence)
- **Python**: 3.12+ for PC controller

### Validation Steps Before Committing

- **CRITICAL**: DO NOT run `./gradlew build` - it will fail and waste time
- Run `./dev.sh lint` for code style validation
- Test PC controller with `cd pc-controller && python3 demo_mvp_components.py`
- Validate individual components before attempting full builds
- Document any build workarounds or dependency fixes

## Project Context

This is a Multi-Modal Physiological Sensing Platform implementing a Hub-and-Spoke architecture:

- **Hub (PC Controller)**: Python-based central coordinator with PyQt6 GUI
- **Spoke (Android Sensor Node)**: Kotlin-based mobile sensor nodes with thermal/GSR/RGB capabilities
- **Integration**: JSON-based TCP communication with mDNS discovery
- **Purpose**: Scientific data acquisition for research and machine learning analysis

The Android build is currently non-functional due to missing dependencies, but the PC controller has a comprehensive MVP
implementation that can be tested and developed independently.
