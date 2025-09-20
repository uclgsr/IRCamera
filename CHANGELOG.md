# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed
- Fixed all Kotlin compilation warnings in libapp module
  - Resolved String? to String type mismatch in GuideInterface.kt
  - Added null safety checks for ByteArray? parameters in RingBuffer.kt
  - Removed redundant null checks on non-null properties in UsbBuffer.kt
  - Added @OptIn annotation for ExperimentalUnsignedTypes in ByteUtils.kt
  - Fixed unsafe calls on nullable Array<File> receiver in FileUtils.kt
  - Applied fixes to duplicate files in com.mpdc4gsr package

### Changed
- Improved type safety across matrix processing components
- Enhanced null safety handling for file operations

### Technical Details
- No breaking changes to existing functionality
- All fixes maintain backward compatibility
- Compilation now produces zero warnings for libapp module