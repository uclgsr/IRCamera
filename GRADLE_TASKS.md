# Gradle Tasks Guide

This project uses product flavors, which affects the available Gradle task names.

## Product Flavors

The app module has a "prod" product flavor defined, which means all build tasks include the flavor name.

## Common Task Names

Instead of generic task names, use the flavor-specific variants:

### Compilation Tasks
- ❌ `:app:compileDebugSources` (ambiguous - doesn't exist)
- ✅ `:app:compileProdDebugSources` (correct)

### Build Tasks  
- ❌ `:app:assembleDebug` (ambiguous - doesn't exist)
- ✅ `:app:assembleProdDebug` (correct)

### Full Build
- ✅ `:app:assembleProdRelease` (release build)

## Available Tasks

To see all available tasks for the app module:
```bash
./gradlew :app:tasks --all
```

To build debug variant:
```bash
./gradlew assembleProdDebug
```

To build release variant:
```bash
./gradlew assembleProdRelease
```