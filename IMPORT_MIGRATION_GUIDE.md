# Import Path Migration Guide

This document summarizes the import path changes made after the repository restructuring.

## Overview

After the repository consolidation, many packages were moved and renamed for better organization. This guide documents all the import path changes that were applied.

**Latest Update:** Fixed remaining incorrect imports for `mpdc4gsr.compose.base.*` and `mpdc4gsr.utils.*` (44 files corrected).

## Package Migration Summary

### 0. Base Compose Activity

#### BaseComposeActivity
**Old Path:**
```kotlin
import mpdc4gsr.compose.base.BaseComposeActivity
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.BaseComposeActivity
```

**Affected Files:** 36 files

**Note:** The BaseComposeActivity class was moved from the old `mpdc4gsr.compose.base` package to `mpdc4gsr.core.ui` as part of the UI component consolidation.

### 1. Compose UI Components

#### Theme
**Old Path:**
```kotlin
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.compose.theme.*
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.*
```

**Affected Files:** 67 files

#### Components
**Old Path:**
```kotlin
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.TitleBarAction
import mpdc4gsr.compose.components.*
import mpdc4gsr.compose.components.sensors.*
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.components.*
import mpdc4gsr.core.ui.components.sensors.*
```

**Affected Files:** 67 files

### 2. Sensor Data Classes

#### Core Sensor Interfaces
**Old Path:**
```kotlin
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.RecordingStatus
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.ErrorType
import mpdc4gsr.sensors.RecordingStats
import mpdc4gsr.sensors.RgbCameraRecorder
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.TimestampRecord
import mpdc4gsr.sensors.TimeSynchronizationService
```

**New Path:**
```kotlin
import mpdc4gsr.core.data.SensorRecorder
import mpdc4gsr.core.data.RecordingStatus
import mpdc4gsr.core.data.SensorError
import mpdc4gsr.core.data.ErrorType
import mpdc4gsr.core.data.RecordingStats
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.data.TimestampManager
import mpdc4gsr.core.data.TimestampRecord
import mpdc4gsr.core.data.TimeSynchronizationService
```

**Affected Files:** 53 files

#### Thermal Sensor
**Old Path:**
```kotlin
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder
import mpdc4gsr.sensors.thermal.ThermalRecorder
```

**New Path:**
```kotlin
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import mpdc4gsr.feature.thermal.ui.ThermalRecorder
```

**Affected Files:** 15 files

#### GSR Sensor
**Old Path:**
```kotlin
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import mpdc4gsr.sensors.gsr.GSRConstants
import mpdc4gsr.sensors.gsr.GSRCalculationUtils
import mpdc4gsr.sensors.gsr.ShimmerDeviceManager
import mpdc4gsr.sensors.gsr.RealShimmerDeviceFactory
```

**New Path:**
```kotlin
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.gsr.data.GSRConstants
import mpdc4gsr.feature.gsr.data.GSRCalculationUtils
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory
```

**Affected Files:** 20 files

**Note:** `ShimmerDeviceManager` was consolidated from multiple locations (`mpdc4gsr.sensors.gsr.ShimmerDeviceManager` and `mpdc4gsr.sensors.unified.ShimmerDeviceManager`) into a single location at `mpdc4gsr.core.data.ShimmerDeviceManager`.

#### Camera Managers
**Old Path:**
```kotlin
import mpdc4gsr.sensors.camera.CameraConfigurationManager
import mpdc4gsr.sensors.camera.CameraControlsManager
import mpdc4gsr.sensors.camera.CameraPerformanceManager
```

**New Path:**
```kotlin
import mpdc4gsr.feature.camera.data.CameraConfigurationManager
import mpdc4gsr.feature.camera.data.CameraControlsManager
import mpdc4gsr.feature.camera.data.CameraPerformanceManager
```

**Affected Files:** 5 files

### 3. Unified Models

**Old Path:**
```kotlin
import mpdc4gsr.sensors.unified.model.DeviceInfo
import mpdc4gsr.sensors.unified.model.GSRSample
import mpdc4gsr.sensors.unified.model.NetworkStatus
import mpdc4gsr.sensors.unified.model.PCControllerInfo
import mpdc4gsr.sensors.unified.model.SessionConfig
import mpdc4gsr.sensors.unified.model.SessionInfo
import mpdc4gsr.sensors.unified.model.SessionQuality
import mpdc4gsr.sensors.unified.model.SessionStatistics
import mpdc4gsr.sensors.unified.model.SessionStatus
import mpdc4gsr.sensors.unified.model.SessionSummary
import mpdc4gsr.sensors.unified.model.SessionType
```

**New Path:**
```kotlin
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.core.data.model.NetworkStatus
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.core.data.model.SessionConfig
import mpdc4gsr.core.data.model.SessionInfo
import mpdc4gsr.core.data.model.SessionQuality
import mpdc4gsr.core.data.model.SessionStatistics
import mpdc4gsr.core.data.model.SessionStatus
import mpdc4gsr.core.data.model.SessionSummary
import mpdc4gsr.core.data.model.SessionType
```

**Affected Files:** 27 files

**Note:** All session-related models (SessionConfig, SessionInfo, SessionQuality, SessionStatistics, SessionStatus, SessionSummary, SessionType) have been consolidated into `mpdc4gsr.core.data.model`.

### 4. Unified Controllers

**Old Path:**
```kotlin
import mpdc4gsr.sensors.unified.UnifiedGSRRecorder
import mpdc4gsr.sensors.unified.UnifiedNetworkController
import mpdc4gsr.sensors.unified.UnifiedSessionManager
import mpdc4gsr.sensors.unified.ShimmerDeviceManager
```

**New Path:**
```kotlin
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.data.UnifiedNetworkController
import mpdc4gsr.core.data.UnifiedSessionManager
import mpdc4gsr.core.data.ShimmerDeviceManager
```

**Affected Files:** 15 files

### 5. Data Utilities

#### Session Data
**Old Path:**
```kotlin
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.data.BufferedDataWriter
import mpdc4gsr.data.CSVBufferedWriter
import mpdc4gsr.data.SessionDirectoryManager
import mpdc4gsr.data.TimeManager
import mpdc4gsr.utils.BufferedDataWriter
import mpdc4gsr.utils.CSVBufferedWriter
import mpdc4gsr.utils.SessionDirectoryManager
import mpdc4gsr.utils.TimeManager
import mpdc4gsr.utils.AppVersionUtil
```

**New Path:**
```kotlin
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.data.utils.BufferedDataWriter
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.data.utils.TimeManager
import mpdc4gsr.core.data.utils.AppVersionUtil
```

**Affected Files:** 18 files (9 additional files fixed for mpdc4gsr.utils.* imports)

### 6. Network Components

**Old Path:**
```kotlin
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.network.NetworkClient
import mpdc4gsr.network.CommandConnection
import mpdc4gsr.network.ProtocolHandler
```

**New Path:**
```kotlin
import mpdc4gsr.feature.network.data.NetworkServer
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.CommandConnection
import mpdc4gsr.feature.network.data.ProtocolHandler
```

**Affected Files:** 12 files (5 additional files fixed in continuation)

### 7. Permissions

**Old Path:**
```kotlin
import mpdc4gsr.core.permissions.PermissionManager
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.permissions.PermissionController
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.core.ui.PermissionController
```

**Affected Files:** 7 files (All 7 files fixed in continuation)

### 8. Navigation Screens

**Old Path:**
```kotlin
import mpdc4gsr.compose.screens.*
import mpdc4gsr.compose.testing.TestResultsScreen
```

**New Path:**
```kotlin
import mpdc4gsr.feature.settings.ui.*
import mpdc4gsr.feature.thermal.ui.*
import mpdc4gsr.feature.gsr.ui.*
import mpdc4gsr.feature.camera.ui.*
import mpdc4gsr.feature.network.ui.*
```

**Affected Files:** 2 files

### 9. Sensor UI Models

**Old Path:**
```kotlin
import mpdc4gsr.compose.screens.SensorState
import mpdc4gsr.compose.screens.SensorType
import mpdc4gsr.compose.screens.GSRAction
import mpdc4gsr.compose.screens.ThermalAction
import mpdc4gsr.compose.screens.CameraAction
import mpdc4gsr.compose.screens.UnifiedSystemState
import mpdc4gsr.compose.screens.SystemAction
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.ui.model.GSRAction
import mpdc4gsr.core.ui.model.ThermalAction
import mpdc4gsr.core.ui.model.CameraAction
import mpdc4gsr.core.ui.model.UnifiedSystemState
import mpdc4gsr.core.ui.model.SystemAction
```

**Affected Files:** 7 files

**Note:** All sensor UI models including states and actions have been centralized in `mpdc4gsr.core.ui.model` for better organization and reusability.

## Quick Reference Table

| Old Package | New Package | Content Type |
|-------------|-------------|--------------|
| `mpdc4gsr.compose.base.*` | `mpdc4gsr.core.ui.*` | Base Compose activities |
| `mpdc4gsr.compose.theme` | `mpdc4gsr.core.ui.theme` | Theme definitions |
| `mpdc4gsr.compose.components` | `mpdc4gsr.core.ui.components` | UI components |
| `mpdc4gsr.sensors.*` | `mpdc4gsr.core.data.*` | Core sensor interfaces |
| `mpdc4gsr.sensors.thermal.*` | `mpdc4gsr.feature.thermal.ui.*` | Thermal sensor implementation |
| `mpdc4gsr.sensors.gsr.*` (except ShimmerDeviceManager) | `mpdc4gsr.feature.gsr.data.*` | GSR sensor implementation |
| `mpdc4gsr.sensors.gsr.ShimmerDeviceManager` | `mpdc4gsr.core.data.ShimmerDeviceManager` | Shimmer device manager (consolidated) |
| `mpdc4gsr.sensors.camera.*` | `mpdc4gsr.feature.camera.data.*` | Camera managers |
| `mpdc4gsr.sensors.unified.model.*` | `mpdc4gsr.core.data.model.*` | Data models (all session models included) |
| `mpdc4gsr.sensors.unified.*` (controllers) | `mpdc4gsr.core.data.*` | Unified controllers |
| `mpdc4gsr.sensors.unified.ShimmerDeviceManager` | `mpdc4gsr.core.data.ShimmerDeviceManager` | Shimmer device manager (consolidated) |
| `mpdc4gsr.data.*` | `mpdc4gsr.core.data.*` or `mpdc4gsr.core.data.utils.*` | Data utilities |
| `mpdc4gsr.utils.*` | `mpdc4gsr.core.data.utils.*` | Utility classes |
| `mpdc4gsr.network.*` | `mpdc4gsr.feature.network.data.*` | Network components |
| `mpdc4gsr.permissions.*` | `mpdc4gsr.core.ui.*` | Permission managers |
| `mpdc4gsr.compose.screens.*` (actions/states) | `mpdc4gsr.core.ui.model.*` | Sensor UI models and actions |
| `mpdc4gsr.compose.screens.*` (screens) | `mpdc4gsr.feature.*.ui.*` | Screen implementations |

**Note on Consolidations:**
- `ShimmerDeviceManager` from both `mpdc4gsr.sensors.gsr` and `mpdc4gsr.sensors.unified` has been consolidated to `mpdc4gsr.core.data.ShimmerDeviceManager`
- All session-related models (SessionConfig, SessionInfo, SessionQuality, SessionStatistics, SessionStatus, SessionSummary, SessionType) are now in `mpdc4gsr.core.data.model`
- All sensor UI models (SensorState, SensorType, GSRAction, ThermalAction, CameraAction, etc.) are now in `mpdc4gsr.core.ui.model`

## Statistics

- **Total files modified:** 187+
- **Total import statements fixed:** 382+
- **Files fixed in initial update:** 44 files (36 for compose.base, 9 for utils, plus package declaration fixes)
- **Files fixed in continuation:** 13 files (7 for permissions, 5 for network, plus additional network fixes)
- **New model files created:** 1 (SensorModels.kt)
- **Component modules build status:** All passing
- **App module status:** All old compose.screens, compose.base, utils, permissions, and network imports eliminated
- **Duplicate definitions removed:** Sensor models consolidated from UnifiedSensorDashboard.kt to core.ui.model package

## Key Improvements

1. **Eliminated Old Imports**: All `mpdc4gsr.compose.screens`, `mpdc4gsr.compose.base`, `mpdc4gsr.utils`, `mpdc4gsr.permissions`, and `mpdc4gsr.network` imports have been replaced with proper package references
2. **Created Centralized Models**: New `mpdc4gsr.core.ui.model` package for sensor UI models and actions
3. **Reduced Duplication**: Sensor state and action definitions consolidated from multiple files into single source
4. **Improved Organization**: Clear separation between data models (core.data.model) and UI models (core.ui.model)
5. **Fixed Base Activity**: BaseComposeActivity moved to `mpdc4gsr.core.ui` package (36 files updated)
6. **Fixed Utilities**: All utility classes moved to `mpdc4gsr.core.data.utils` package (9 files updated)
7. **Fixed Package Declarations**: Updated Java files with incorrect package declarations (AppVersionUtil.java)
8. **Fixed Permissions**: All permission classes moved to `mpdc4gsr.core.ui` package (7 files updated)
9. **Fixed Network Components**: All network classes moved to `mpdc4gsr.feature.network.data` package (5 files updated)

## Remaining Issues

The following issues still need to be addressed:

1. **Settings Components**: Some files reference `SettingsCard`, `SettingsToggle`, `SettingsSlider` which may need to be mapped to `SettingsItem`, `SwitchSettingsItem`, `SliderSettingsItem`
2. **ViewModels**: Some ViewModel imports may need path updates
3. **Navigation**: Some complex navigation files may have additional screen import issues
4. **Other Unrelated Compilation Errors**: There are some unrelated compilation issues in test files and other areas that are not part of this import migration

## How to Use This Guide

When you encounter an unresolved import error:

1. Find the old import path in this document
2. Replace it with the corresponding new path
3. Rebuild to verify the fix

## Automated Fix Script

The fixes were applied using Python scripts that:
1. Scanned all `.kt` files in `app/src`
2. Applied regex-based replacements for each import pattern
3. Preserved all other code unchanged
4. Logged all changes made

## Next Steps

To complete the migration:
1. Address remaining Settings component references
2. Update any ViewModel import paths
3. Fix any remaining navigation issues
4. Run full build verification
5. Run lint checks
6. Run test suite

---

Generated: 2024-10-01
Last Updated: After repository restructuring
