# IRCamera Clean Architecture Diagram

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        IRCamera Application                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   UI Layer (Compose)                      │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │  │
│  │  │  Main  │  │  GSR   │  │Thermal │  │Network │         │  │
│  │  │ Screen │  │ Screen │  │ Screen │  │ Screen │  ...    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Presentation Layer (ViewModels)              │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │  │
│  │  │  Main  │  │  GSR   │  │Thermal │  │Network │         │  │
│  │  │   VM   │  │   VM   │  │   VM   │  │   VM   │  ...    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Domain Layer (Use Cases)                     │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │  │
│  │  │ Record │  │Connect │  │Process │  │  Sync  │         │  │
│  │  │  Data  │  │ Sensor │  │  Data  │  │  Data  │  ...    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Data Layer (Repositories)                    │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │  │
│  │  │  GSR   │  │Thermal │  │Network │  │Settings│         │  │
│  │  │  Repo  │  │  Repo  │  │  Repo  │  │  Repo  │  ...    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Data Sources                           │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │  │
│  │  │Shimmer3│  │ TC001  │  │Network │  │  Local │         │  │
│  │  │  BLE   │  │  USB   │  │ Socket │  │   DB   │  ...    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
mpdc4gsr/
│
├── core/                          # Shared foundation
│   ├── ui/                       # Base UI components
│   │   ├── BaseComposeActivity
│   │   ├── BaseComposeFragment
│   │   ├── BaseViewModel
│   │   ├── navigation/           # Navigation system
│   │   │   ├── UnifiedNavigation
│   │   │   └── IRCameraNavigation
│   │   └── theme/                # Material Design
│   │       ├── Theme
│   │       └── Type
│   │
│   ├── domain/                   # Shared business logic
│   │   └── (Use cases)
│   │
│   ├── data/                     # Data abstractions
│   │   ├── GSRDataRepository
│   │   └── SensorDataRepository
│   │
│   └── di/                       # Dependency injection
│       └── AppContainer
│
└── feature/                       # Feature modules
    │
    ├── main/                     # Main entry point
    │   ├── ui/
    │   │   ├── MainActivity      # Single unified entry
    │   │   └── MainScreen
    │   ├── presentation/
    │   │   └── MainActivityViewModel
    │   ├── domain/
    │   └── data/
    │
    ├── gsr/                      # GSR sensor feature
    │   ├── ui/                   # 13 Activities
    │   │   ├── GSRSensorScreen
    │   │   ├── GSRSettingsComposeActivity
    │   │   ├── SessionManagerComposeActivity
    │   │   └── ... (10 more)
    │   ├── presentation/         # 6 ViewModels
    │   │   ├── GSRSettingsViewModel
    │   │   ├── SessionManagerViewModel
    │   │   └── ... (4 more)
    │   ├── domain/
    │   └── data/
    │
    ├── thermal/                  # Thermal camera
    │   ├── ui/
    │   │   ├── ThermalCameraRecorder
    │   │   ├── ThermalRecorder
    │   │   └── ... (3 more)
    │   ├── presentation/
    │   ├── domain/
    │   └── data/
    │
    ├── network/                  # Network pairing
    │   ├── ui/
    │   │   └── DevicePairingComposeActivity
    │   ├── presentation/
    │   │   └── DevicePairingViewModel
    │   ├── domain/
    │   └── data/
    │
    └── settings/                 # Settings management
        ├── ui/
        ├── presentation/
        ├── domain/
        └── data/
```

## Data Flow Example: GSR Recording

```
┌─────────────────────────────────────────────────────────────────┐
│  User Action: Start GSR Recording                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  UI Layer (feature/gsr/ui)                                       │
│  ┌────────────────────────────────────────────────────────┐     │
│  │ GSRSensorScreen                                         │     │
│  │   - User clicks "Start Recording" button               │     │
│  │   - Emits event to ViewModel                           │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Presentation Layer (feature/gsr/presentation)                   │
│  ┌────────────────────────────────────────────────────────┐     │
│  │ GSRSettingsViewModel                                    │     │
│  │   - Receives user action                               │     │
│  │   - Validates recording state                          │     │
│  │   - Updates UI state (isRecording = true)              │     │
│  │   - Calls use case: StartRecordingUseCase              │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Domain Layer (feature/gsr/domain)                               │
│  ┌────────────────────────────────────────────────────────┐     │
│  │ StartRecordingUseCase                                   │     │
│  │   - Applies business rules                             │     │
│  │   - Checks sensor connection                           │     │
│  │   - Creates session with timestamp                     │     │
│  │   - Calls repository: startRecording()                 │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Data Layer (feature/gsr/data)                                   │
│  ┌────────────────────────────────────────────────────────┐     │
│  │ GSRDataRepository                                       │     │
│  │   - Manages data sources                               │     │
│  │   - Coordinates BLE communication                      │     │
│  │   - Buffers incoming data                              │     │
│  │   - Saves to local storage                             │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Data Sources                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Shimmer3 BLE │  │  Local DB    │  │  File System │          │
│  │  - Connect   │  │  - Save      │  │  - Export    │          │
│  │  - Stream    │  │  - Query     │  │  - Share     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Navigation Flow

```
MainActivity (feature/main/ui)
      │
      ├─→ UnifiedNavHost (core/ui/navigation)
      │        │
      │        ├─→ MainScreen
      │        │
      │        ├─→ GSR Feature
      │        │    ├─→ GSRSensorScreen
      │        │    ├─→ GSRSettingsScreen
      │        │    ├─→ SessionManagerScreen
      │        │    └─→ ...
      │        │
      │        ├─→ Thermal Feature
      │        │    ├─→ ThermalMonitorScreen
      │        │    ├─→ ThermalCalibrateScreen
      │        │    └─→ ...
      │        │
      │        └─→ Network Feature
      │             ├─→ DevicePairingScreen
      │             └─→ ...
      │
      └─→ Legacy Navigation (backward compatibility)
           ├─→ MainActivityLegacy (deprecated)
           └─→ MainActivityAlternative (deprecated)
```

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                      Dependency Rules                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  UI Layer              →  Can depend on: Presentation            │
│  (Activities/Screens)     Cannot depend on: Domain, Data         │
│                                                                   │
│  Presentation Layer    →  Can depend on: Domain                  │
│  (ViewModels)             Cannot depend on: UI, Data             │
│                                                                   │
│  Domain Layer          →  Can depend on: Nothing (pure logic)    │
│  (Use Cases)              Cannot depend on: UI, Presentation, Data│
│                                                                   │
│  Data Layer            →  Can depend on: Domain (interfaces)     │
│  (Repositories)           Cannot depend on: UI, Presentation     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Testing Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                         Test Pyramid                             │
│                                                                   │
│                            ┌───┐                                 │
│                            │E2E│                                 │
│                            └───┘                                 │
│                      ┌─────────────┐                             │
│                      │ Integration │                             │
│                      └─────────────┘                             │
│              ┌───────────────────────────┐                       │
│              │       Unit Tests          │                       │
│              └───────────────────────────┘                       │
│                                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Unit Tests:                                                     │
│    - Domain layer use cases (pure business logic)                │
│    - ViewModels (with test repositories)                         │
│    - Repositories (with mock data sources)                       │
│                                                                   │
│  Integration Tests:                                              │
│    - Feature flows (UI → ViewModel → Repository)                 │
│    - Data source integrations                                    │
│                                                                   │
│  E2E Tests:                                                      │
│    - Complete user journeys                                      │
│    - Multi-modal recording scenarios                             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Benefits Visualization

```
┌──────────────────────────────────────────────────────────────┐
│                    Before Migration                           │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  activities/                                                   │
│    ├── MainActivity.kt                                         │
│    ├── MainActivityLegacy.kt         ← Duplicates             │
│    ├── MainActivityAlternative.kt    ← Duplicates             │
│    ├── GSRSettingsActivity.kt                                 │
│    ├── ThermalActivity.kt                                     │
│    └── ... (41 activities)                                    │
│                                                                │
│  viewmodel/                                                    │
│    ├── MainActivityViewModel.kt                               │
│    └── BaseViewModel.kt                                       │
│                                                                │
│  sensors/gsr/                                                  │
│    ├── GSRSettingsViewModel.kt       ← Mixed concerns         │
│    ├── GSRSettingsActivity.kt        ← Mixed concerns         │
│    └── ... (ViewModels + Activities)                          │
│                                                                │
│  ui_components/                                                │
│    └── MainFragmentViewModel.kt      ← Scattered              │
│                                                                │
│  Problems:                                                     │
│    ✗ Flat structure                                           │
│    ✗ Duplication                                              │
│    ✗ Mixed concerns                                           │
│    ✗ Unclear dependencies                                     │
│                                                                │
└──────────────────────────────────────────────────────────────┘

                              ↓↓↓
                        MIGRATION
                              ↓↓↓

┌──────────────────────────────────────────────────────────────┐
│                     After Migration                           │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  core/                                                         │
│    ├── ui/             ← Shared UI components                 │
│    ├── domain/         ← Shared business logic                │
│    ├── data/           ← Repository abstractions              │
│    └── di/             ← Dependency injection                 │
│                                                                │
│  feature/                                                      │
│    ├── main/           ← Single MainActivity                  │
│    │   ├── ui/                                                │
│    │   ├── presentation/                                      │
│    │   ├── domain/                                            │
│    │   └── data/                                              │
│    │                                                           │
│    ├── gsr/            ← GSR feature isolated                 │
│    │   ├── ui/                                                │
│    │   ├── presentation/                                      │
│    │   ├── domain/                                            │
│    │   └── data/                                              │
│    │                                                           │
│    ├── thermal/        ← Thermal feature isolated             │
│    │   └── ... (same structure)                               │
│    │                                                           │
│    └── network/        ← Network feature isolated             │
│        └── ... (same structure)                               │
│                                                                │
│  Benefits:                                                     │
│    ✓ Feature-based organization                               │
│    ✓ No duplication                                           │
│    ✓ Clear separation of concerns                             │
│    ✓ Explicit dependencies                                    │
│    ✓ Easy to test                                             │
│    ✓ Scalable architecture                                    │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```
