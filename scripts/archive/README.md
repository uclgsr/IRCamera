# Build Script Archive

## [LIST] Legacy Build Scripts

This directory contains the original build scripts that have been replaced by the unified cross-platform build system.

## [REFRESH] Script Consolidation

### Archived Windows Batch Files

**build_apk_google_script.bat**
- **Purpose**: Google variant release build
- **Replaced by**: `../build.sh -v google`
- **Lines**: 12
- **Functionality**: Basic Google APK build with Chinese output messages

**build_apk_topdon_script.bat** 
- **Purpose**: Topdon variant release build
- **Replaced by**: `../build.sh -v topdon`
- **Lines**: 12  
- **Functionality**: Basic Topdon APK build with Chinese output messages

**build_release_google_apk_script.bat**
- **Purpose**: Google release build (identical to build_apk_google_script.bat)
- **Replaced by**: `../build.sh -t release -v google`
- **Lines**: 12
- **Functionality**: Duplicate of Google variant build

**build_release_topdon_apk_script.bat**
- **Purpose**: Topdon release build (identical to build_apk_topdon_script.bat)
- **Replaced by**: `../build.sh -t release -v topdon`
- **Lines**: 12
- **Functionality**: Duplicate of Topdon variant build

### Archived Shell Scripts

**enhanced_build.sh**
- **Purpose**: Enhanced build with performance optimization
- **Replaced by**: `../build.sh -f` (includes all features)
- **Lines**: 147
- **Functionality**: Gradle optimization, error handling, PC Controller building

**build_for_testing.sh**
- **Purpose**: Comprehensive build with detailed error analysis
- **Replaced by**: `../build.sh -f` (includes comprehensive error analysis)
- **Lines**: 110
- **Functionality**: Multiple build strategies, error analysis, troubleshooting

## [DATA] Consolidation Benefits

### Before: 6 Separate Scripts
- **4 Batch Files**: 80% identical content, Windows-only compatibility
- **2 Shell Scripts**: Overlapping functionality, different approaches
- **Total Lines**: ~293 lines of code across 6 files
- **Maintenance**: 6 separate files to update for changes
- **Platform Support**: Limited cross-platform compatibility

### After: 1 Unified Script
- **1 Cross-Platform Script**: Works on Windows (Git Bash), Linux, macOS
- **Configurable Options**: All functionality accessible via command-line arguments
- **Total Lines**: 335 lines (more features, better error handling)
- **Maintenance**: Single file to maintain and update
- **Platform Support**: Full cross-platform compatibility

### Feature Comparison

| Feature | Legacy Scripts | Unified Script |
|---------|---------------|----------------|
| **Google Variant** | Separate .bat file | `-v google` option |
| **Topdon Variant** | Separate .bat file | `-v topdon` option |
| **Debug Build** | Not available | `-t debug` option |
| **Clean Build** | Manual process | `-c` option |
| **Error Analysis** | build_for_testing.sh only | Always included with `-f` |
| **Fallback Strategies** | enhanced_build.sh only | Always included with `-f` |
| **Cross-Platform** | Shell scripts only | All platforms supported |
| **Help System** | None | `--help` option |
| **Performance Optimization** | enhanced_build.sh only | Always included |

## [LAUNCH] Migration Examples

### Old Usage -> New Usage

```bash
# Windows batch file usage (old)
build_apk_google_script.bat

# New unified usage
../build.sh -v google
```

```bash
# Enhanced build script (old)
./enhanced_build.sh

# New unified equivalent
../build.sh -f
```

```bash
# Testing build script (old) 
./build_for_testing.sh

# New unified equivalent with same comprehensive error analysis
../build.sh -f
```

## [SEARCH] Technical Details

### Legacy Batch File Template
The 4 batch files were nearly identical, differing only in output messages:

```batch
@echo off
chcp 65001

call gradlew clean

echo "Start Compilation[variant]Version"
call gradlew :app:assembleRelease

echo "Build Package Complete，apkFile in Root Directoryoutputs/"
echo "[variant]VersionAPKCompleted"

pause
```

### Legacy Shell Script Features

**enhanced_build.sh** provided:
- Gradle optimization with memory settings
- PC Controller integration
- Performance monitoring
- Better error reporting

**build_for_testing.sh** provided:
- Multiple fallback strategies
- Dependency issue detection
- Comprehensive error analysis  
- Build log analysis

**All features consolidated into** `../build.sh` **with enhanced capabilities**

## [LIST] File Manifest

```
scripts/archive/
|---- build_apk_google_script.bat       # 12 lines, Google variant
|---- build_apk_topdon_script.bat       # 12 lines, Topdon variant  
|---- build_release_google_apk_script.bat # 12 lines, duplicate Google
|---- build_release_topdon_apk_script.bat # 12 lines, duplicate Topdon
|---- enhanced_build.sh                 # 147 lines, enhanced features
|---- build_for_testing.sh              # 110 lines, testing focus
`---- README.md                         # This file
```

## [WARNING] Usage Notice

**These scripts are preserved for historical reference only.**

- ❌ **Do not use these archived scripts** - they lack modern features and cross-platform support
- [DONE] **Use the unified build script** - `../build.sh` for all build operations
- [BOOKS] **Documentation available** - See `../../docs/BUILD_GUIDE.md` for complete usage instructions

---

**Archive Date**: Documentation Consolidation v1.0  
**Status**: Historical Reference Only  
**Replacement**: `../build.sh` - Unified Cross-Platform Build Script