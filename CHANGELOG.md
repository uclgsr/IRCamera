# Changelog

All notable changes to the IRCamera project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed
- **BREAKING**: Removed redundant RangeSeekBar standalone module
  - The standalone RangeSeekBar module was redundant with libui seekbar implementation
  - ColorPickDialog now uses a simplified local RangeSeekBar implementation in libapp
  - Removed external dependency and simplified project structure
  - Updated all documentation to reflect module removal

### Changed
- **MAJOR**: Merged thermal, thermal-lite modules into thermalunified as main thermal module
- thermalunified now contains all thermal imaging functionality:
  - Basic thermal imaging (from thermal module)
  - Advanced IR with dual-camera fusion (existing thermal-ir features)  
  - Lightweight USB camera control (from thermal-lite module)
- Consolidated thermal module dependencies and build configuration
- Updated package structure to organize different thermal capabilities
- Removed separate thermal and thermal-lite modules from build configuration

### Added
- Comprehensive feasibility analysis for thermal modules merger
- THERMAL_MODULES_ANALYSIS.md documenting merger strategy
- Documentation for thermal, thermal-ir, and thermal-lite functionality comparison
- Enhanced thermalunified module with combined capabilities
- Updated project architecture diagrams and documentation
- Simplified build system by removing unnecessary module dependencies

### Analysis
- thermal module: Basic thermal imaging (38 files, simple menu interface) - MERGED
- thermal-ir module: Advanced IR with dual-camera fusion (152 files, comprehensive features) - MAIN
- thermal-lite module: Lightweight USB camera control (33 files, direct hardware access) - MERGED
## Previous Changes
- See git history for detailed changes prior to this changelog

### Implementation
- Merged all thermal functionality into single thermal-ir module
- Maintains separate namespaces for different thermal camera types
- Preserves hardware-specific implementations under organized structure
- Single consolidated build and dependency configuration

## [Previous]
- Existing thermal imaging capabilities across three specialized modules
- Hub-and-Spoke architecture with PC Controller and Android nodes
- Multi-modal sensor integration (thermal, GSR, RGB)