# Gradle Tasks Guide

This project has been simplified to use **release-only builds** with no product flavors or debug variants.

## Build Configuration

The build system has been streamlined:
- **No product flavors** - simplified build configuration
- **Release builds only** - debug variants disabled for production focus
- **Single APK output** - optimized for deployment

## Available Build Tasks

### Release Build (Production)
- ✅ `:app:assembleRelease` - Build production APK
- ✅ `:app:bundleRelease` - Build AAB bundle for Play Store

### Common Tasks
- ✅ `:app:clean` - Clean build artifacts
- ✅ `:app:tasks` - List all available tasks

## Build Commands

To see all available tasks for the app module:
```bash
./gradlew :app:tasks --all
```

To build release APK:
```bash
./gradlew :app:assembleRelease
```

To build release bundle:
```bash
./gradlew :app:bundleRelease
```

To clean and build:
```bash
./gradlew clean :app:assembleRelease
```

## Build Scripts

Use the provided build scripts for convenience:
- `build_apk_google_script.bat` - Windows release build
- `build_production_apk.sh` - Linux/macOS release build with full verification