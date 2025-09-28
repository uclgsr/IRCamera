# Unified Multi-Modal Sensor UI - Jetpack Compose Implementation

## Overview

This directory contains a comprehensive Jetpack Compose implementation for all sensor modalities in the IR Camera application. The implementation provides a unified, rationalized structure that includes GSR (Galvanic Skin Response), RGB Camera, and Thermal IR sensors.

## Architecture

```
compose/
├── activity/                    # Demo and integration activities
├── components/                  # Reusable UI components
│   ├── sensors/                # Sensor-specific components
│   ├── dashboard/              # Dashboard components  
│   └── preview/                # Preview components
├── screens/                    # Main screen composables
├── sensors/                    # Sensor-specific screens
│   ├── gsr/                   # GSR sensor screens
│   ├── thermal/               # Thermal IR screens
│   ├── camera/                # RGB camera screens
│   └── unified/               # Multi-modal screens
├── theme/                      # Material3 theme system
└── integration/                # ViewModel integration examples
```

## Key Components

### 🎯 Unified Sensor Dashboard
- **UnifiedSensorDashboard**: Master dashboard showing all sensor modalities
- **UnifiedSensorStatus**: System-wide status and control component
- Real-time sensor status visualization with connection diagrams
- Multi-modal recording and synchronization controls

### 🧩 Sensor-Specific Cards
- **GSRSensorCard**: GSR sensor status with real-time waveform
- **ThermalSensorCard**: Thermal camera with temperature visualization  
- **RGBCameraSensorCard**: RGB camera with preview and controls
- Interactive controls for each sensor type
- Status indicators and metrics displays

### 📱 Individual Sensor Screens
- **GSRSensorScreen**: Dedicated GSR monitoring with data analysis
- **ThermalMonitorScreen**: Thermal camera preview with overlays
- **RGBCameraScreen**: RGB camera control and recording
- **CalibrateScreen**: Dual-camera alignment interface
- **AnnotateScreen**: Report preview and annotation

### 🔧 Reusable Components
- **TitleBar**: Material3 TopAppBar replacing custom TitleView
- **StatusIndicator**: Color-coded sensor status displays
- **MetricItem**: Standardized metric display component
- **Canvas-based visualizations**: Real-time data plotting

## Features

### ✅ Multi-Modal Integration
- Unified dashboard for all sensor types (GSR, Thermal IR, RGB)
- Real-time status monitoring with visual connection diagrams  
- Synchronized recording and data collection across sensors
- System-wide controls for multi-modal operations

### ✅ Real-Time Data Visualization
- **GSR**: Live waveform with skin conductance analysis
- **Thermal IR**: Temperature overlays with hotspot detection
- **RGB Camera**: Live preview with grid overlay and focus indicators
- Interactive data displays with zoom and analysis features

### ✅ Professional Sensor Controls
- Individual sensor configuration and calibration
- Recording controls with duration and file size tracking
- Export functionality for data analysis
- Professional camera controls (exposure, ISO, focus modes)

### ✅ Consistent Design System
- Dark theme (#16131e) matching reference implementation
- Material3 components with thermal-focused color scheme
- Standardized spacing and typography throughout
- Unified navigation patterns across all screens

## Usage

### Demo Activity
```kotlin
// Launch the unified demo
val intent = Intent(context, ComposeUnifiedDemoActivity::class.java)
startActivity(intent)
```

### Individual Screen Integration
```kotlin
// Use individual screens in existing Activities
setContent {
    IRCameraTheme {
        UnifiedSensorDashboard(
            onSensorClick = { sensorType ->
                // Navigate to specific sensor screen
            }
        )
    }
}
```

### ViewModel Integration Examples
```kotlin
// Connect existing ViewModels with Compose state
@Composable
fun IntegratedGSRScreen(viewModel: GSRViewModel = viewModel()) {
    val gsrState by viewModel.gsrState.collectAsStateWithLifecycle()
    
    GSRSensorScreen(
        onSaveData = { viewModel.saveGSRData() },
        onSettingsClick = { viewModel.openSettings() }
    )
}
```

## Sensor Type Support

### 🔬 GSR (Galvanic Skin Response)
- Shimmer3 device integration ready
- Real-time waveform visualization  
- Skin conductance analysis
- Data export for research applications

### 🌡️ Thermal IR Camera
- TOPDON TC001/TC007 support
- Temperature overlay visualization
- Calibration and measurement tools
- Report generation with annotations

### 📷 RGB Camera
- Built-in camera integration
- Professional recording controls
- Photo capture functionality
- Resolution and frame rate settings

## Migration from XML

This Compose implementation provides direct replacements for:
- `SensorDashboardFragment` → `UnifiedSensorDashboard`
- `GSR*Activity` classes → `GSRSensorScreen`
- `MonitorThermalFragment` → `ThermalMonitorScreen`
- Camera Activities → `RGBCameraScreen`
- `TitleView` → `TitleBar` composable

## Benefits

- **Unified Architecture**: Single framework for all sensor modalities
- **Modern UI**: Declarative Compose patterns with Material3 design
- **Real-time Updates**: Efficient state management with StateFlow integration
- **Maintainable Code**: Modular composables with clear separation of concerns  
- **Consistent UX**: Standardized patterns across all sensor interfaces
- **Professional Features**: Advanced sensor controls and data visualization

This implementation provides a complete foundation for multi-modal sensor data collection and visualization in a modern, maintainable architecture.