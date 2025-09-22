# Changelog

## [2.2.1] - SmartRefreshLayout Dependency Resolution Fix (2024-12-21)

### Fixed
- **SmartRefreshLayout Dependency Resolution**: Fixed 401 Unauthorized errors for `com.scwang.smart:refresh-layout-kernel:2.1.0` and `com.scwang.smart:refresh-header-classics:2.1.0`
- **Maven Coordinates Correction**: Updated to use correct group ID `io.github.scwang90` instead of `com.scwang.smart` in version catalog
- **Pull-to-refresh Functionality**: Resolved build failures affecting IRGalleryFragment, PDFListFragment, and PDFListActivity
- **JitPack Resolution Issues**: Migrated SmartRefreshLayout dependencies from JitPack to Maven Central for reliable resolution

### Changed  
- **Version Catalog**: Updated `refresh-layout-kernel` and `refresh-header-classics` library definitions to use `io.github.scwang90` group ID
- **Repository Resolution**: SmartRefreshLayout now resolved from Maven Central instead of JitPack

### Technical Details
- **Root Cause**: JitPack returning 401 Unauthorized for `com.scwang.smart` artifacts
- **Solution**: Use official Maven Central artifacts with group ID `io.github.scwang90`  
- **Backwards Compatibility**: Package names in code remain unchanged (`com.scwang.smart.refresh.layout.*`)
- **Affected Modules**: `component:thermalunified` (primary usage)

## [2.3.0] - Sensor Timestamp Synchronization Unification (2024-12-23)

### Added
- **Unified Timestamp System**: All sensors now use consistent TimestampManager instead of mixed System.nanoTime()/SystemClock approaches
- **SessionSync Markers**: Added automatic SessionSync event logging at session start for cross-sensor alignment verification
- **Drift Analysis Logging**: Enhanced TimeSynchronizationService with device timestamp drift monitoring capabilities
- **Cross-Device Sync Documentation**: Enhanced NTP-like handshake with better logging and sync quality reporting
- **Timestamp Verification Activity**: Added TimestampSyncVerificationActivity for manual testing of multi-modal timestamp alignment
- **Wall-Clock Conversion**: Added convertMonotonicToWallClock method for consistent epoch time conversion

### Changed
- **RGB Camera Recorder**: Replaced all System.nanoTime() calls with TimestampManager.getCurrentTimestampNanos()
- **Thermal Recorder**: Unified timestamp usage to TimestampManager for consistent time base
- **GSR Sensor Recorder**: Updated to use unified timestamp system for synchronization compatibility
- **Enhanced NTP Protocol**: Improved PC-Phone synchronization with better quality metrics and drift monitoring
- **Session Metadata**: Enhanced with automatic sync event creation for all recording modalities

### Fixed
- **Timestamp Inconsistency**: Resolved mixed timestamp sources across sensors (System.nanoTime vs SystemClock.elapsedRealtimeNanos)
- **Cross-Sensor Alignment**: All modalities now share the same time base for post-processing alignment verification
- **Sync Quality Reporting**: Added detailed logging of network latency and clock offset for troubleshooting

### Technical Details
- **Unified Time Base**: All sensors use System.currentTimeMillis() epoch time with TimestampManager for consistency
- **SessionSync Events**: Every sensor logs start event with timestamp for post-hoc verification within millisecond tolerance
- **NTP Enhancement**: PC-Phone handshake includes quality metrics and automatic drift detection
- **Verification Tests**: Manual test activity simulates sharp multi-modal events (e.g., hand clap) for alignment validation


## [2.2.0] - Kotlin Compilation Error Fixes (2024-12-21)

### Fixed
- **AppHolder11.kt**: Added missing PackageInfo import, corrected Activity lifecycle callback parameter types, fixed context property return type, and fixed Holder singleton initialization
- **CheckableItem111.kt**: Fixed isChecked property override issue and return type compatibility
- **CheckableParcelable111.kt**: Fixed method access patterns using direct property access instead of deprecated getter/setter methods  
- **PermissionsRequester11.kt**: Fixed collection type mismatch in method parameters
- **Observable11.kt**: Fixed MethodInfo property access using Kotlin property syntax
- **ObserverMethodHelper11.kt**: Fixed reflection API calls using Kotlin property access patterns
- **MethodInfo11.kt**: Updated to use Kotlin reflection property access

### Changed
- **Reflection API Usage**: Updated all reflection method calls to use Kotlin property syntax (method.name vs method.getName())
- **Type Safety**: Improved nullable type handling across observer and lifecycle callback patterns


## [2.1.1] - BLE Core Compilation Fixes (2024-12-21)

### Fixed
- **RequestCallback Interface**: Fixed redeclaration error by creating separate callback interfaces in ble-core/callback package
- **ByteUtil Type Mismatches**: Fixed Int to Float conversion errors in bytesToFloat functions (lines 78, 87)  
- **DefaultLogger Override Issues**: Fixed Logger interface implementation with proper property overrides
- **HexUtil Byte Type Conflicts**: Fixed java.lang.Byte vs kotlin.Byte type mismatches in uniteBytes function
- **Missing Callback Package**: Created missing ble-core/src/main/java/com/mpdc4gsr/ble/core/callback/ directory structure

### Added
- RequestCallback interface for BLE request handling
- BleCallbacks interfaces for connection and characteristic callbacks

## [2.1.1] - BLE Core GenericRequest Fix (2024-12-21)

### Fixed
- **GenericRequest Compilation**: Fixed all compilation errors in GenericRequest.kt
- **Interface Implementation**: Properly implemented Request interface with override modifiers
- **Property Initialization**: Fixed uninitialized properties and added proper defaults
- **RequestBuilder**: Enhanced RequestBuilder interface with generic type parameter and required properties
- **ConnectionState/RequestType**: Resolved merge conflict markers in enum classes
- **Device Property**: Fixed device property handling with custom getter/setter pattern
- **RequestCallback**: Added missing RequestCallback interface for BLE operations

### Added
- **RequestCallback Interface**: Created proper callback interface for BLE request handling

## [2.1.2] - BLE Core WriteOptions Fix (2024-12-21)

### Fixed
- **WriteOptions.Builder**: Fixed private field access issue in WriteOptions constructor by changing Builder field visibility from private to internal
- Resolved compilation errors: Cannot access packageWriteDelayMillis, requestWriteDelayMillis, packageSize, isWaitWriteResult, writeType, useMtuAsPackageSize

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

### MAJOR IMPLEMENTATION COMPLETE

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

- **libunified** - Unified core library combining libapp, libir, and libui functionality
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
- **Total unified**: 598 source files in single libunified module

### Implementation Status

- DONE: Created unified libunified structure
- DONE: Merged all source code without namespace conflicts
- DONE: Resolved resource conflicts
- DONE: Build system refined for complex dependencies
- COMPLETE: Ready for phased migration approach

### Implementation Results (COMPLETED)

- Phase 1: Created minimal working libunified - COMPLETED
- Phase 2: Migrated components to use libunified - COMPLETED
- Phase 3: Deprecated original libraries - COMPLETED
- Phase 4: Updated documentation and diagrams - IN PROGRESS

---

## Documentation Update - ASCII Safety and Current State Reflection (2024-12-22)

**Commit**: c7769bc

### Changes Made
- Removed all emoji characters from markdown documentation (91 occurrences)
- Updated all references from libcore to libunified to reflect actual implementation
- Updated MERMAID diagrams to show completed migration status instead of proposed
- Fixed repository structure documentation to reflect current BLE module organization
- Updated API documentation to show unified library architecture
- Converted all status indicators to ASCII-safe text equivalents

### Files Updated
- README.md: Removed emojis, updated library references
- MERMAID_DIAGRAMS.md: Updated architecture diagrams, removed emojis, showed completion
- BACKLOG.md: Updated all library references, removed emojis, marked tasks complete
- docs/API_REFERENCE.md: Updated to show unified library structure
- docs/DEVELOPER_GUIDE.md: Updated repository structure, removed emojis
- docs/README.md: Removed emojis from status indicators
- docs/modules/README.md: Removed emojis from headers
- dev.sh: Removed emojis from generated diagrams

### Key Corrections
- libcore -> libunified (reflecting actual implementation)
- Proposed status -> Completed status in migration diagrams  
- Old three-library structure -> Current unified structure
- BleModule -> ble-core, ble-shimmer, ble-topdon (current modular structure)