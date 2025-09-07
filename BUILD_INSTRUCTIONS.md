# Build Instructions - MPDC4GSR Multi-Modal Physiological Sensing Platform

This document provides comprehensive build instructions for the IRCamera project, which has been rebranded and enhanced as the **Multi-Modal Physiological Sensing Platform (MPDC4GSR)**.

## ⚠️ Important: Release-Only Build Configuration

**This project uses a simplified, release-only build configuration:**
- ❌ **No debug builds** - Debug variants have been disabled
- ❌ **No product flavors** - Simplified to single release configuration  
- ✅ **Release builds only** - Production-ready APK generation

## Quick Start

### Windows Users
```bash
# Use the provided batch script
build_apk_google_script.bat
```

### Linux/macOS Users
```bash
# Use the comprehensive build script
./build_production_apk.sh
```

### Manual Build Command
```bash
# Clean and build release APK
./gradlew clean :app:assembleRelease
```

## Available Gradle Tasks

### Core Build Tasks
| Task | Description |
|------|-------------|
| `./gradlew :app:assembleRelease` | Build production APK |
| `./gradlew :app:bundleRelease` | Build AAB for Play Store |
| `./gradlew clean` | Clean all build artifacts |
| `./gradlew :app:tasks` | List all available app tasks |

### Verification Commands
```bash
# Check available tasks (should show NO debug tasks)
./gradlew :app:tasks | grep -i debug
# Expected output: (empty - no debug tasks)

# Verify release task exists
./gradlew :app:tasks | grep assembleRelease
# Expected output: assembleRelease - Assembles main output for variant release
```

## Build Output

Successful builds generate:
- **APK Location**: `app/build/outputs/apk/release/app-release.apk`
- **APK Size**: ~117MB (multi-modal dependencies included)
- **Features**: RGB+Thermal+GSR recording, Shimmer3 SDK, RAW image capture

## Common Issues & Solutions

### ❌ Error: "Cannot locate tasks that match ':app:assembleDevDebug' or ':app:assembleProdDebug'"
**Cause**: Using outdated task names from previous product flavor configuration.

**Solution**: Use the correct release-only task names:
```bash
# ❌ Wrong (old product flavor tasks)
./gradlew :app:assembleDevDebug
./gradlew :app:assembleProdDebug
./gradlew :app:assembleProdRelease

# ✅ Correct (current release-only tasks)  
./gradlew :app:assembleRelease
```

### ❌ Build Performance Issues
**Solution**: The build system is optimized for performance:
```bash
# Use parallel builds with optimized JVM settings
./gradlew :app:assembleRelease --parallel --build-cache
```

### ❌ Missing Dependencies
**Solution**: Ensure all required SDKs are available:
- Android SDK 34
- NDK (arm64-v8a support)
- Java 17
- Kotlin 1.9.x

## Development Features

### Multi-Modal Recording Capabilities
- **RGB Video**: 4K60FPS recording with CameraX
- **RAW Images**: 30fps DNG capture (Samsung Level 3)
- **GSR Data**: 128Hz Shimmer3 Bluetooth integration  
- **Thermal Video**: IR camera integration
- **Synchronization**: Nanosecond precision timing

### Build Optimizations
- Release-only configuration eliminates debug overhead
- Enhanced Gradle caching and parallel execution
- Optimized APK packaging with ProGuard/R8
- Production signing configuration included

## Deployment

### Manual Testing
```bash
# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk
```

### Production Deployment
Use the generated APK or AAB for distribution:
- **Direct APK**: For sideloading and enterprise distribution
- **AAB Bundle**: For Google Play Store deployment

## Support

For build issues:
1. Check this document for common solutions
2. Verify your development environment meets requirements
3. Use `./gradlew clean` to reset build state
4. Review the comprehensive `build_production_apk.sh` script for advanced build configurations

---

**Project**: Multi-Modal Physiological Sensing Platform (MPDC4GSR)  
**Build System**: Android Gradle Plugin 8.11.0, Gradle 8.14  
**Target Platform**: Android 5.0+ (API 21+), optimized for Samsung S22