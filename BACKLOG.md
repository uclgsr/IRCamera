# Project Backlog

## High Priority - TC001 Thermal Camera Integration Enhancement - COMPLETED ✅

### TASK: TC001 Hardware Integration - Commit 4b1c7a9
**Status**: COMPLETED ✅
- [x] Replace stub/reflection approach with real Topdon TC001 SDK calls in ThermalCameraRecorder
- [x] Implement proper SDK initialization in ThermalCameraRecorder.startRecording()
- [x] Add native library loading for TC001 SDK with graceful fallback
- [x] Configure USB device opening with correct mode (256×192 IR resolution)
- [x] Move SDK initialization to background thread (already using withContext(Dispatchers.IO))

### TASK: Continuous Frame Capture Implementation - COMPLETED ✅
**Status**: COMPLETED ✅
- [x] Implement 10Hz continuous thermal frame capture loop
- [x] Register IFrameCallback for SDK frame delivery
- [x] Create background Handler for captureThermalFrame() calls with 100ms intervals
- [x] Add frame conversion to Bitmap and PNG saving to thermal_images/ directory
- [x] Log temperature telemetry (min/max) to thermal CSV with system timestamps

### TASK: USB Permission and Device Management - COMPLETED ✅
**Status**: COMPLETED ✅
- [x] Enhance USB permission flow in ThermalCameraRecorder (already implemented)
- [x] BroadcastReceiver for USB_DEVICE_ATTACHED/DETACHED already exists (ThermalUsbReceiver)
- [x] Add TC001-specific VID/PID verification (0x2744/0x0001)
- [x] Handle graceful camera open/close on permission grant/detach
- [x] AndroidManifest.xml already has TC001 USB device filter configured

### TASK: Error Handling and Robustness - COMPLETED ✅
**Status**: COMPLETED ✅
- [x] Add try-catch blocks around all SDK calls
- [x] Implement graceful fallback when camera fails or disconnects
- [x] Ensure other sensors continue recording on thermal failure
- [x] Add user notification via Toast for camera errors
- [x] Prevent app crashes from thermal thread failures

## High Priority - Kotlin Compilation Fixes ✅ COMPLETED

### EPIC: BLE Core Module Compilation Error Resolution - COMPLETED


**Status**: COMPLETED ✅

#### Compilation Error Fixes (COMPLETED)
- [x] Fix AppHolder11.kt Activity lifecycle callback parameter type mismatches
- [x] Fix CheckableItem111.kt interface implementation and property overrides
- [x] Fix CheckableParcelable111.kt constructor and method access issues
- [x] Fix PermissionsRequester11.kt collection type compatibility
- [x] Fix Observable11.kt and ObserverMethodHelper11.kt reflection API usage
- [x] Update MethodInfo11.kt to use proper Kotlin property access patterns
- [x] Address nullable type handling and type safety improvements

- [x] Fix RequestCallback interface redeclaration (BleCallbacks.kt vs RequestCallback.kt) 
- [x] Fix ByteUtil.kt type mismatch errors (Int to Float conversions on lines 78, 87)
- [x] Fix DefaultLogger.kt override issues with Logger interface
- [x] Fix HexUtil.kt byte type argument mismatches (java.lang.Byte vs kotlin.Byte)
- [x] Create missing callback package structure in ble-core module
- [x] Test compilation success for ble-core module

## Current Priority - BLE Core Module Development

### EPIC: BLE Core Module Stabilization - IN PROGRESS

**Status**: IN PROGRESS ⚠️

#### BLE Core Compilation Fixes (COMPLETED)
- [x] Fix GenericRequest.kt compilation errors
- [x] Resolve RequestBuilder interface issues
- [x] Fix Request interface implementation
- [x] Add missing RequestCallback interface
- [x] Handle device property initialization correctly
- [x] Fix enum class merge conflicts (RequestType, ConnectionState)

#### BLE Core Next Steps (PENDING)
- [ ] Fix remaining ConnectionImpl compilation issues
- [ ] Add proper test coverage for GenericRequest
- [ ] Validate BLE operations end-to-end
- [ ] Review and clean up unused/legacy BLE code

## High Priority - Compilation Fixes ✅ COMPLETED

### BLE Core Compilation Issue - COMPLETED ✅
- [x] Fix WriteOptions.Builder private field access issue
- [x] Change Builder fields from private to internal visibility
- [x] Verify compilation of WriteOptions class works correctly
- [x] Update documentation
## Critical Priority - BLE Core Module Issues ✅ COMPLETED

### TASK: UUID Import Fix - COMPLETED
**Status**: COMPLETED ✅
- [x] Fixed missing `import java.util.UUID` statement in Request.kt interface
- [x] Resolved "Unresolved reference 'UUID'" compilation errors
- [x] Verified consistency with other BLE core module files
- [x] Updated documentation to reflect fix


## High Priority - Build System Maintenance ✅ COMPLETED

### EPIC: Gradle Build System Standardization - COMPLETED

**Status**: COMPLETED ✅

#### Build System Standardization (COMPLETED)
- [x] Fix settings.gradle.kts to remove non-existent modules (thermal, thermal-ir, thermal-lite, RangeSeekBar)
- [x] Standardize all build.gradle.kts files with consistent structure
- [x] Replace hardcoded dependency versions with version catalog references
- [x] Add missing build features (viewBinding, testInstrumentationRunner) to all modules
- [x] Unify Android configuration (compileSdk, buildTypes, compileOptions) across modules
- [x] Fix module dependencies (BleModule -> ble-core, remove defunct lib* modules)
- [x] Correct AAR file paths in component modules
- [x] Verify XML namespace usage (all using standard Android namespaces)
- [x] Simplify Gradle tasks (clean, build, buildAll, buildRelease, buildDebug)

## High Priority - Library Unification

### EPIC: Merge libapp, libir, libui into Unified Library

**Status**: COMPLETED ✅

#### Phase 1: Foundation (COMPLETED)
- [x] Create minimal working libcore with essential functionality
- [x] Resolve build dependency conflicts for JAR/AAR libraries  
- [x] Test basic libcore compilation and functionality
- [x] Create migration guide for components

#### Phase 2: Component Migration (COMPLETED)

- [x] Migrate thermal-lite component to use libcore (pilot)
- [x] Migrate thermal component to use libcore
- [x] Migrate thermal-ir component to use libcore
- [x] Migrate gsr-recording component to use libcore
- [x] Migrate user component to use libcore
- [x] Update main app to use libcore

#### Phase 3: Deprecation and Cleanup (COMPLETED)

- [x] Remove libapp, libir, libui modules from build
- [x] Update settings.gradle.kts to exclude deprecated modules
- [x] Clean up build configurations
- [x] Verify all functionality works with unified library

#### Phase 4: Documentation and Architecture

- [ ] Update ARCHITECTURE.md with new libcore structure
- [ ] Update API_REFERENCE.md with unified API documentation
- [ ] Update MERMAID_DIAGRAMS.md with simplified architecture
- [ ] Update README.md with new build instructions

## Medium Priority - Build System Optimization

### Gradle Build Improvements

- [ ] Optimize libcore build configuration
- [ ] Implement proper dependency management for native libraries
- [ ] Add build caching optimizations
- [ ] Create build validation scripts

### Development Tools

- [ ] Update dev.sh script for libcore support
- [ ] Create migration validation tools
- [ ] Add automated testing for libcore functionality

## Low Priority - Future Enhancements

### Architecture Improvements

- [ ] Consider splitting libcore into smaller focused modules if needed
- [ ] Implement proper separation of concerns within libcore
- [ ] Add module boundaries and API contracts

### Performance Optimization

- [ ] Profile unified library performance vs separate libraries
- [ ] Optimize resource usage and memory footprint
- [ ] Implement lazy loading for optional components

## Technical Debt

### Resolved Issues

- ✅ Resource conflicts between libraries (strings, dimensions)
- ✅ Namespace conflicts analysis (no conflicts found)
- ✅ Dependency relationship mapping

### Outstanding Issues

- ⚠️ Complex JAR/AAR library conflicts in build system
- ⚠️ KSP annotation processing optimization needed
- ⚠️ Native library dependency resolution

## Research and Analysis

### Completed Analysis

- ✅ Library structure and size analysis (598 total source files)
- ✅ Dependency graph mapping and circular dependency check
- ✅ Namespace conflict analysis across three libraries
- ✅ Build system impact assessment
- ✅ Resource conflict identification and resolution

### Benefits Quantified

- **Build Simplification**: 3 library dependencies → 1 unified dependency
- **Module Reduction**: 67% reduction in library modules
- **Maintenance**: Centralized configuration and dependency management
- **Performance**: Reduced build overhead and faster compilation