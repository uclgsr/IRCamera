# Material Design Icons Integration Guide

## Overview

This guide documents the integration of Material Design icons from Android Jetpack Compose into the IRCamera
application. All core UI components now support both Material Icons (ImageVector) and custom drawable resources.

## Updated Components

### 1. MenuCompose.kt

**MenuTabItem** data class and composable now support both icon types:

```kotlin
// Using Material Icon (recommended for common icons)
MenuTabItem(
    icon = Icons.Default.Settings,
    label = "Settings"
)

// Using custom drawable (for specialized icons)
MenuTabItem(
    iconRes = R.drawable.ic_thermal_camera,
    label = "Thermal"
)
```

### 2. SettingsCompose.kt

**SettingItem** and **SettingItemData** support both icon types:

```kotlin
// Using Material Icon
SettingItemData(
    text = "Share",
    icon = Icons.Default.Share
)

// Using custom drawable
SettingItemData(
    text = "GSR Sensor",
    iconRes = R.drawable.ic_gsr_sensor
)
```

### 3. MenuViewsCompose.kt

**CameraMenuView** accepts both icon parameter types:

```kotlin
CameraMenuView(
    galleryIconVector = Icons.Default.PhotoLibrary,
    moreIconVector = Icons.Default.MoreVert
)
```

### 4. TitleBar.kt

Already supports both via overloading:

```kotlin
TitleBarAction(
    icon = Icons.Default.Settings,
    contentDescription = "Settings",
    onClick = { }
)
```

## Common Icon Mappings

| Custom Drawable      | Material Icon              | Usage    |
|----------------------|----------------------------|----------|
| ic_settings.xml      | Icons.Default.Settings     | Settings |
| ic_share.xml         | Icons.Default.Share        | Share    |
| ic_delete.xml        | Icons.Default.Delete       | Delete   |
| ic_camera_alt.xml    | Icons.Default.CameraAlt    | Camera   |
| ic_videocam.xml      | Icons.Default.Videocam     | Video    |
| ic_photo_library.xml | Icons.Default.PhotoLibrary | Gallery  |
| ic_info.xml          | Icons.Default.Info         | Info     |
| ic_warning.xml       | Icons.Default.Warning      | Warning  |
| ic_microphone.xml    | Icons.Default.Mic          | Audio    |
| ic_folder_open.xml   | Icons.Default.FolderOpen   | Folder   |
| ic_file_download.xml | Icons.Default.FileDownload | Download |

## Domain-Specific Icons (Keep as Custom)

- ic_gsr_sensor.xml - GSR device icon
- ic_gsr_pulse.xml - GSR waveform
- ic_thermal_camera.xml - Thermal camera device
- ic_camera_raw.xml - RAW format indicator
- All thermal menu icons (ic_menu_thermal*)
- All target icons (ic_target_*)

## Benefits

1. **Reduced APK Size** - Material Icons are part of Compose library
2. **Better Performance** - ImageVectors render efficiently
3. **Automatic Theming** - Icons adapt to theme colors
4. **Consistent Design** - Material Design 3 compliance
5. **Future-Proof** - Official Jetpack support

## Best Practices

- Prefer Material Icons for common UI elements
- Keep custom drawables for specialized visuals
- Always provide meaningful content descriptions
- Use appropriate icon variants (Filled, Outlined, etc.)
