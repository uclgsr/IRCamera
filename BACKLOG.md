# Project Backlog

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