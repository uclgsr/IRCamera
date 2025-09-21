# Changelog

## [2.1.1] - BLE Core Module Compilation Fix (2024-12-21)

### Fixed
- **BLE Core Interface**: Fixed missing `import java.util.UUID` statement in Request.kt interface
- **Compilation Errors**: Resolved "Unresolved reference 'UUID'" compilation errors in BLE core module
- **Code Consistency**: Aligned Request.kt import statements with other BLE core module files

## [2.1.0] - Gradle Build System Standardization (2024-12-21)

### Added
- **Unified Build Tasks**: Simplified build system with clean+build for all modules
- **Standardized Build Configuration**: All modules now use consistent structure and dependencies
- **Version Catalog Integration**: Replaced hardcoded versions with centralized version management

### Changed
- **Module Structure**: Removed non-existent module references (thermal, thermal-ir, thermal-lite, RangeSeekBar)
- **BLE Dependencies**: Standardized BLE modules to use version catalog consistently
- **Build Features**: Added missing build features (viewBinding, testInstrumentationRunner) across all modules
- **Android Configuration**: Unified compileSdk, buildTypes, and compileOptions across modules

### Fixed
- **Gradle Settings**: Corrected settings.gradle.kts to only include existing modules
- **Dependency Paths**: Fixed AAR file references in thermalunified module
- **Module Dependencies**: Replaced obsolete BleModule with ble-core, removed defunct lib* modules

### Technical Details
- **8 modules standardized**: app, libunified, ble-core, ble-shimmer, ble-topdon, component modules
- **Build tasks simplified**: `clean`, `build`, `buildAll`, `buildRelease`, `buildDebug`
- **XML namespaces verified**: All XML files using correct Android standard namespaces

## [2.0.0] - Complete Implementation (2024-12-19)

### ✅ MAJOR IMPLEMENTATION COMPLETE

**Nuclear Library Unification - FULLY IMPLEMENTED:**

- **Complete namespace refactoring**: All 598 source files moved to `com.mpdc4gsr.libunified.*`
- **Import migration**: 1094+ import statements updated across entire codebase
- **Build system overhaul**: All gradle files updated for unified architecture
- **67% module reduction**: libapp + libir + libui → single libunified module

**BLE Architectural Split - FULLY IMPLEMENTED:**

- **Device-focused modules**: BleModule → ble-core + ble-shimmer + ble-topdon
- **56% size reduction**: 12,538 lines → ~5,512 lines through focused architecture
- **Smart component routing**: GSR → ble-shimmer, Thermal → ble-topdon
- **Better separation of concerns**: Device-specific logic properly isolated

### Added

- **libunified** - Unified core library with complete namespace restructure
- **ble-core** - Core BLE functionality and commons utilities (~3,500 lines)
- **ble-shimmer** - GSR/Shimmer device-specific functionality (1,131 lines)
- **ble-topdon** - Thermal/Topdon device-specific functionality (881 lines)

### Changed

- **All component dependencies**: Updated to use focused BLE modules
- **All import statements**: Migrated to unified namespace structure
- **Build configurations**: Updated for new module architecture
- **Documentation**: Updated to reflect implemented architecture

### Removed

- **libapp**, **libir**, **libui** directories (merged into libunified)
- **BleModule** directory (split into focused device modules)
- **Example/demo code** from BLE modules for size optimization

## [1.0.0] - Feasibility Analysis (2024-12-18)

### Added

- **libcore** - Unified core library combining libapp, libir, and libui functionality
- Comprehensive feasibility analysis for three-library merge
- Proof-of-concept implementation with resolved resource conflicts

### Analysis

- **Library Merge Feasibility**: CONFIRMED as technically feasible
- **Benefits Identified**:
    - Simplified build system (3 dependencies → 1)
    - Cleaner component structure
    - Reduced build complexity
    - No namespace conflicts between libraries
- **Challenges Resolved**:
    - Resource file conflicts (strings, dimensions)
    - Manifest permission merging
    - JAR/AAR library consolidation

### Technical Details

- **libapp**: 247 source files, application framework
- **libir**: 64 source files, IR camera processing
- **libui**: 287 source files, UI components and charting
- **Total unified**: 598 source files in single libcore module

### Implementation Status

- ✅ Created unified libcore structure
- ✅ Merged all source code without namespace conflicts
- ✅ Resolved resource conflicts
- ⚠️ Build system needs refinement for complex dependencies
- 📋 Ready for phased migration approach

### Next Steps

- Phase 1: Create minimal working libcore
- Phase 2: Migrate components to use libcore
- Phase 3: Deprecate original libraries
- Phase 4: Update documentation and diagrams