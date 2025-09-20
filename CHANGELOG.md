# Changelog

All notable changes to the IRCamera project will be documented in this file.

## [Unreleased]

### Removed
- **BREAKING**: Removed redundant RangeSeekBar standalone module
  - The standalone RangeSeekBar module was redundant with libui seekbar implementation
  - ColorPickDialog now uses a simplified local RangeSeekBar implementation in libapp
  - Removed external dependency and simplified project structure
  - Updated all documentation to reflect module removal

### Changed
- Updated project architecture diagrams and documentation
- Simplified build system by removing unnecessary module dependencies

## Previous Changes
- See git history for detailed changes prior to this changelog