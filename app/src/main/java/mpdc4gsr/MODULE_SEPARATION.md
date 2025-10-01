# Module Separation - Sensor Architecture

This document clarifies the distinct responsibilities of each sensor module in the application.

## Three Sensor Modules

### 1. RGB Module (`feature/camera/`)
**Hardware**: Samsung Main Camera (Built-in device camera)

**Responsibilities**:
- RGB video recording
- RAW DNG footage capture
- Standard photo capture
- Camera configuration and settings
- Video encoding and quality settings

**Key Components**:
- `DualModeCameraActivityCompose` - Main camera interface
- `DualModeCameraViewModel` - Camera state management
- Camera stream handling
- Video/photo file management

**SDK/APIs Used**:
- Android CameraX API
- Android Camera2 API
- MediaRecorder for video
- DNG format support

---

### 2. GSR Module (`feature/gsr/`)
**Hardware**: Shimmer3 GSR+ Device (Bluetooth wearable sensor)

**Responsibilities**:
- Galvanic Skin Response (GSR) data collection
- Shimmer3 device pairing and connection
- Real-time GSR data streaming
- GSR data recording and export
- Device battery monitoring
- BLE communication management

**Key Components**:
- 18 Compose Activities for GSR workflows
- 6 ViewModels for state management
- `ShimmerRepository` - Data layer
- `ShimmerDataSource` - SDK wrapper
- 7 Use Cases for GSR operations

**SDK/APIs Used**:
- Shimmer Android Instrument Driver (shimmerandroidinstrumentdriver-3.2.4_beta.aar)
- Shimmer Driver JARs
- ShimmerBluetoothManager
- Bluetooth Low Energy (BLE) APIs

---

### 3. Thermal Module (`feature/thermal/`)
**Hardware**: Topdon TC001/TC007 (USB/WiFi thermal camera)

**Responsibilities**:
- Thermal imaging and temperature measurement
- TC001/TC007 device connection
- Thermal video/image capture
- Temperature range configuration
- Thermal calibration
- Report generation with thermal data

**Key Components**:
- 6 Thermal Activities
- `ThermalCameraViewModel` - State management
- `ThermalRepository` - Data layer
- `TopdonDataSource` - SDK wrapper
- 9 Use Cases for thermal operations

**SDK/APIs Used**:
- Topdon AAR (topdon.aar)
- USB device communication
- WiFi thermal streaming

---

## Module Independence

Each module is **completely independent**:

1. **No Cross-Dependencies**: RGB module doesn't depend on GSR or Thermal
2. **Separate Data Flows**: Each has its own repositories and data sources
3. **Independent SDKs**: Each wraps its own hardware SDK
4. **Feature Isolation**: Can be developed, tested, and deployed independently

## Architecture Compliance

All three modules follow Clean Architecture:

```
UI (Compose Activities)
    ↓
Presentation (ViewModels)
    ↓
Domain (Use Cases + Repository Interfaces)
    ↓
Data (Repository Implementations + Data Sources)
    ↓
Hardware SDK (Camera2/Shimmer/Topdon)
```

## Common Mistake to Avoid

❌ **Don't mix GSR and RGB**: They use completely different hardware
- RGB = Samsung camera sensor
- GSR = Shimmer3 wearable device

❌ **Don't confuse thermal with camera**: They are separate devices
- Thermal = Topdon TC001 external device
- Camera = Built-in Samsung camera

## File Organization

```
feature/
├── camera/          # RGB - Samsung Camera Module
│   ├── ui/
│   └── presentation/
│
├── gsr/            # GSR - Shimmer3 Device Module
│   ├── ui/
│   ├── presentation/
│   ├── domain/
│   └── data/
│
└── thermal/        # Thermal - Topdon TC001 Module
    ├── ui/
    ├── presentation/
    ├── domain/
    └── data/
```

## Summary

- **RGB**: Samsung camera for video/photo (Android APIs)
- **GSR**: Shimmer3 wearable for skin response (Shimmer SDK)
- **Thermal**: Topdon TC001 for temperature imaging (Topdon SDK)

Each module is a complete vertical slice with its own UI, ViewModels, Use Cases, Repositories, and SDK integration.
