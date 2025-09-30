# Activity Consolidation Plan

## Overview
This document tracks the consolidation of duplicate activities across the IRCamera codebase.

## Context
As documented in NAVIGATION_ARCHITECTURE_ANALYSIS.md:
- **5 different MainActivity implementations** causing confusion
- **4 conflicting navigation paradigms** (UnifiedNavigation, IRCameraNavigation, NavigationManager with duplicates, DemoNavigationScreen)

This plan addresses the MainActivity and activity consolidation. Navigation paradigm consolidation is a separate concern.

## Duplicate Activities Identified

### 1. MainActivity Variants
- **MainActivity.kt** (34 lines) - PRIMARY - Production Compose implementation
- **MainActivityAlternative.kt** (665 lines) - BACKUP - Experimental features
- **MainActivityLegacy.kt** (396 lines) - BACKUP - Legacy fragment-based
- **MainComposeActivity.kt** (164 lines) - REMOVE - Superseded by MainActivity
- **SimplifiedMainActivity.kt** (195 lines) - Already in backup/final-traditional-activities/

**Decision**: Keep MainActivity.kt as primary, move others to backup

### 2. SimplifiedMain Variants  
- **SimplifiedMainActivityCompose.kt** (573 lines) - REMOVE - Old naming
- **SimplifiedMainComposeActivity.kt** (588 lines) - KEEP - Correct naming pattern

**Decision**: Keep SimplifiedMainComposeActivity.kt, comment out SimplifiedMainActivityCompose.kt

### 3. GSR Activity Duplicates
**In activities/ directory (REMOVE - not in manifest):**
- GSRDeviceManagementActivityCompose.kt
- GSRGalleryActivityCompose.kt  
- GSRQuickRecordingActivityCompose.kt

**In sensors/gsr/ directory (KEEP - in manifest):**
- GSRDeviceManagementComposeActivity.kt
- GSRPlotComposeActivity.kt
- GSRDataViewComposeActivity.kt
- GSRVideoPlayerComposeActivity.kt
- GSRRawImageViewComposeActivity.kt
- GSRSettingsComposeActivity.kt

**Decision**: Move activities/ GSR files to backup, use sensors/gsr/ versions

### 4. Network Test Activity Duplicates
- **NetworkClientTestActivityCompose.kt** - REMOVE - Old naming
- **NetworkClientTestComposeActivity.kt** - KEEP - Correct naming
- **SimpleNetworkTestActivityCompose.kt** - KEEP if used
- **compose/testing/SimpleNetworkTestComposeActivity.kt** - Check usage

### 5. SensorDashboard Test Duplicates
- **SensorDashboardTestActivityCompose.kt** (in activities/) - Check usage
- **SensorDashboardTestComposeActivity.kt** (in compose/testing/) - Check usage

## Consolidation Steps

1. Move MainActivityAlternative.kt and MainActivityLegacy.kt to backup
2. Comment out MainComposeActivity.kt (in different package)
3. Move SimplifiedMainActivityCompose.kt to backup
4. Move activities/GSR*ActivityCompose.kt files to backup
5. Comment out NetworkClientTestActivityCompose.kt
6. Update all references in code to use consolidated versions
7. Update AndroidManifest.xml
8. Test build

## References to Update

### MainActivity References
- ComposeMigrationLauncherActivity.kt: MainActivityAlternative reference
- BackgroundDeviceScanningService.kt: MainComposeActivity reference

### SimplifiedMain References  
- ComposeMigrationLauncherActivity.kt: SimplifiedMainActivityCompose reference

### GSR Activity References
- ComposeMigrationLauncherActivity.kt: GSRDeviceManagementActivityCompose reference
- MainActivityAlternative.kt: GSRQuickRecordingActivityCompose reference
