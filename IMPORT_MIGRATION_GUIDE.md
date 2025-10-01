# Import Path Migration Guide

This document summarizes the import path changes made after the repository restructuring.

## Overview

After the repository consolidation, many packages were moved and renamed for better organization. This guide documents all the import path changes that were applied.

## Package Migration Summary

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
```

**New Path:**
```kotlin
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.data.utils.BufferedDataWriter
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.data.utils.TimeManager
```

**Affected Files:** 18 files

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

**Affected Files:** 12 files

### 7. Permissions

**Old Path:**
```kotlin
import mpdc4gsr.core.permissions.PermissionManager
import mpdc4gsr.permissions.PermissionManager
```

**New Path:**
```kotlin
import mpdc4gsr.core.ui.PermissionManager
```

**Affected Files:** 7 files

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

## Quick Reference Table

| Old Package | New Package | Content Type |
|-------------|-------------|--------------|
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
| `mpdc4gsr.network.*` | `mpdc4gsr.feature.network.data.*` | Network components |
| `mpdc4gsr.permissions.*` | `mpdc4gsr.core.ui.*` | Permission managers |
| `mpdc4gsr.compose.screens.*` | `mpdc4gsr.feature.*.ui.*` | Screen implementations |

**Note on Consolidations:**
- `ShimmerDeviceManager` from both `mpdc4gsr.sensors.gsr` and `mpdc4gsr.sensors.unified` has been consolidated to `mpdc4gsr.core.data.ShimmerDeviceManager`
- All session-related models (SessionConfig, SessionInfo, SessionQuality, SessionStatistics, SessionStatus, SessionSummary, SessionType) are now in `mpdc4gsr.core.data.model`

## Statistics

- **Total files modified:** 120+
- **Total import statements fixed:** 300+
- **Component modules build status:** All passing
- **App module status:** Most imports fixed, some remaining issues with Settings components and ViewModels

## Remaining Issues

The following issues still need to be addressed:

1. **Settings Components**: Some files reference `SettingsCard`, `SettingsToggle`, `SettingsSlider` which may need to be mapped to `SettingsItem`, `SwitchSettingsItem`, `SliderSettingsItem`
2. **ViewModels**: Some ViewModel imports may need path updates
3. **Navigation**: Some complex navigation files may have additional screen import issues

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
