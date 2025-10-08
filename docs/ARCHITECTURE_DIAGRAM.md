# Architecture Diagram

## Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         IRCamera Application                             │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                              │
│  (UI Screens & ViewModels organized by feature)                         │
│                                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │  Camera  │  │   GSR    │  │ Thermal  │  │ Network  │  │ Settings │ │
│  │ Screens  │  │ Screens  │  │ Screens  │  │ Screens  │  │ Screens  │ │
│  │   (14)   │  │   (35)   │  │   (25)   │  │   (8)    │  │   (23)   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
│                                                                          │
│  Navigation (4) │ Common Components (3)                                 │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ depends on ↓
                                │
┌───────────────────────────────▼──────────────────────────────────────────┐
│                          UI DESIGN SYSTEM                                │
│  (Reusable Components - No Business Logic)                              │
│                                                                          │
│  Components (sensors, cards, dialogs) │ Theme │ Utils                  │
│                       (21 files)                                         │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                   │
│  (Pure Business Logic - No Android Dependencies)                        │
│                                                                          │
│  ┌──────────────┐    ┌───────────────┐    ┌──────────────┐            │
│  │    Models    │    │  Repositories │    │  Use Cases   │            │
│  │   (5 files)  │    │  (Interfaces) │    │  (5 files)   │            │
│  │              │    │   (3 files)   │    │              │            │
│  │ • Session    │    │ • GSRDevice   │    │ • GSR        │            │
│  │ • GSRSample  │    │ • Thermal     │    │ • Thermal    │            │
│  │ • Device     │    │ • Diagnostics │    │ • Device     │            │
│  │ • Network    │    │               │    │              │            │
│  └──────────────┘    └───────────────┘    └──────────────┘            │
│                                                                          │
│  Total: 13 files (Pure Kotlin, 100% testable)                          │
└───────────────────────────┬──────────────────────────────────────────────┘
                            │ implemented by ↓
                            │
┌───────────────────────────▼──────────────────────────────────────────────┐
│                          DATA LAYER                                      │
│  (Data Access & Persistence)                                            │
│                                                                          │
│  ┌──────────────┐    ┌──────────────────────────────────────┐          │
│  │ Repositories │    │         Data Sources                 │          │
│  │  (5 impls)   │    │                                      │          │
│  │              │    │  ┌─────────┐ ┌────────┐ ┌─────────┐ │          │
│  │ • GSRData    │───▶│  │Hardware │ │ Remote │ │  Local  │ │          │
│  │ • Sensor     │    │  │  (4)    │ │  (2)   │ │   (0)   │ │          │
│  │ • GSRDevice  │    │  └─────────┘ └────────┘ └─────────┘ │          │
│  │ • Thermal    │    │                                      │          │
│  │ • Diagnostics│    │  • GSR sensor access                │          │
│  └──────────────┘    │  • Thermal camera access            │          │
│                      │  • Network communication            │          │
│  Mappers (TBD)       │  • Database (future)                │          │
│  Models (TBD)        └──────────────────────────────────────┘          │
│                                                                          │
│  Total: 11 files                                                        │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE LAYER                                │
│  (Android Framework & Cross-Cutting Concerns)                           │
│                                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ Services │  │Monitoring│  │ Security │  │Time Sync │  │ Platform │ │
│  │   (3)    │  │   (2)    │  │   (3)    │  │   (4)    │  │   (3)    │ │
│  │          │  │          │  │          │  │          │  │          │ │
│  │Recording │  │Perf      │  │Auth      │  │Manager   │  │App Init  │ │
│  │Background│  │Telemetry │  │RBAC      │  │Timestamp │  │Perms     │ │
│  │Session   │  │          │  │Monitor   │  │Service   │  │          │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
│                                                                          │
│  Total: 15 files                                                        │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────────────┐
│                    DEPENDENCY INJECTION LAYER                            │
│  (Hilt - Wires Everything Together)                                     │
│                                                                          │
│  AppModule │ DomainModule │ DataModule │ InfrastructureModule           │
│  NetworkModule │ GSRModule │ ThermalModule │ UseCaseModule              │
│                                                                          │
│  Total: 8 files                                                         │
└──────────────────────────────────────────────────────────────────────────┘
```

## Dependency Flow

```
┌────────────────┐
│  Presentation  │
│   (122 files)  │
└────────┬───────┘
         │
         │ uses
         │
         ▼
┌────────────────┐        ┌────────────────┐
│     Domain     │◄───────│      Data      │
│   (13 files)   │        │   (11 files)   │
└────────────────┘        └────────┬───────┘
         ▲                         │
         │                         │
         │ defines interface       │ implements
         │                         │
         └─────────────────────────┘

┌────────────────┐
│Infrastructure  │
│   (15 files)   │ ───────► depends on Domain & Data
└────────────────┘

┌────────────────┐
│       UI       │
│   (21 files)   │ ───────► No business dependencies
└────────────────┘

┌────────────────┐
│       DI       │
│   (8 files)    │ ───────► Connects all layers
└────────────────┘
```

## Feature Breakdown

### Presentation Layer (122 files)

```
presentation/screens/
├── camera/     (14 files)  - Camera screens, dual mode, settings
├── gsr/        (35 files)  - GSR monitoring, device config, data viewer
├── thermal/    (25 files)  - Thermal camera, calibration, gallery
├── network/    (8 files)   - Connection management, device pairing
├── settings/   (23 files)  - App settings, profile, storage, sync
├── main/       (9 files)   - Dashboard, session management
├── device/     (1 file)    - Device diagnostics
├── navigation/ (4 files)   - App-wide navigation
└── common/     (3 files)   - BaseViewModel, UiState, etc.
```

### Domain Layer (13 files)

```
domain/
├── model/         (5 files)
│   ├── DeviceInfo.kt
│   ├── GSRSample.kt
│   ├── NetworkStatus.kt
│   ├── PCControllerInfo.kt
│   └── SessionModels.kt
│
├── repository/    (3 files)
│   ├── DiagnosticsRepository.kt
│   ├── GSRDeviceRepository.kt
│   └── ThermalRepository.kt
│
└── usecase/       (5 files)
    ├── gsr/       - GSRDeviceUseCases.kt
    ├── thermal/   - ThermalCoreUseCases.kt, ThermalHardwareUseCases.kt
    └── device/    - ExportDiagnosticLogsUseCase.kt, RunFullDiagnosticsUseCase.kt
```

### Data Layer (11 files)

```
data/
├── repository/    (5 files)
│   ├── DiagnosticsRepositoryImpl.kt
│   ├── GSRDataRepository.kt
│   ├── GSRDeviceRepositoryImpl.kt
│   ├── SensorDataRepository.kt
│   └── ThermalRepositoryImpl.kt
│
└── source/        (6 files)
    ├── hardware/  (4 files) - GSR & Thermal hardware access
    └── remote/    (2 files) - Network data sources
```

### Infrastructure Layer (15 files)

```
infrastructure/
├── service/       (3 files)  - RecordingService, BackgroundScanService, SessionManager
├── monitoring/    (2 files)  - PerformanceMetrics, TelemetryManager
├── security/      (3 files)  - Auth, RBAC, SecurityMonitor
├── sync/          (4 files)  - TimeSyncManager, TimestampManager, etc.
└── platform/      (3 files)  - App, PermissionManager, PermissionController
```

### UI Layer (21 files)

```
ui/
├── components/    - Reusable sensors, cards, dialogs, buttons
├── theme/         - Theme, colors, typography
└── utils/         - Compose utilities, view extensions
```

### DI Layer (8 files)

```
di/
├── AppModule.kt
├── DomainModule.kt (UseCaseModule.kt)
├── DataModule.kt (AppRepositoryModule.kt)
├── NetworkModule.kt
├── GSRModule.kt (GSRDeviceModule.kt)
└── ThermalModule.kt
```

## Benefits Visualization

```
┌───────────────────────────────────────────────────────────────┐
│                    OLD STRUCTURE                               │
│                                                                │
│  core/ (88 files - MIXED CONCERNS)                            │
│  ├── Services + Repositories + UI + Data + Monitoring         │
│  └── Everything tangled together                              │
│                                                                │
│  feature/ (207 files - INCONSISTENT)                          │
│  ├── camera/  - Flat structure                                │
│  ├── gsr/     - Has data/domain/presentation                  │
│  ├── thermal/ - Has data/domain/presentation                  │
│  └── Circular dependencies                                    │
│                                                                │
│  Problems:                                                     │
│  ❌ Hard to test (Android dependencies everywhere)            │
│  ❌ Unclear boundaries                                         │
│  ❌ Circular dependencies                                      │
│  ❌ Hard to maintain                                           │
└───────────────────────────────────────────────────────────────┘

                              ↓ REORGANIZE ↓

┌───────────────────────────────────────────────────────────────┐
│                    NEW STRUCTURE                               │
│                                                                │
│  Clear Layers with Unidirectional Dependencies:               │
│                                                                │
│  domain/          (13 files)  - Pure business logic            │
│  data/            (11 files)  - Data access                    │
│  presentation/    (122 files) - UI by feature                  │
│  infrastructure/  (15 files)  - Android framework              │
│  ui/              (21 files)  - Design system                  │
│  di/              (8 files)   - Dependency injection           │
│                                                                │
│  Benefits:                                                     │
│  ✅ Testable (domain has no Android)                          │
│  ✅ Clear boundaries                                           │
│  ✅ No circular dependencies                                   │
│  ✅ Easy to maintain and extend                                │
└───────────────────────────────────────────────────────────────┘
```

## Testing Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                     TESTING PYRAMID                          │
│                                                              │
│                         ┌───┐                                │
│                         │UI │  Few UI tests                  │
│                         └───┘  (Espresso, Compose)           │
│                      ┌─────────┐                             │
│                      │ViewModel│  More ViewModel tests       │
│                      └─────────┘  (JUnit)                    │
│                  ┌─────────────────┐                         │
│                  │   Use Cases     │  Many use case tests    │
│                  └─────────────────┘  (Pure Kotlin, fast)    │
│              ┌───────────────────────────┐                   │
│              │    Domain Models         │  Unit tests        │
│              └───────────────────────────┘  (Fast, isolated) │
│                                                              │
│  Domain layer = 100% testable (no Android dependencies)     │
│  Data layer = Integration tests (with mocks)                │
│  Presentation = ViewModel + UI tests                         │
└─────────────────────────────────────────────────────────────┘
```

## Migration Status

```
Phase 1: ✅ COMPLETE
├── New structure created
├── 190 files copied
├── Package declarations updated
└── Documentation created

Phase 2: ⏳ IN PROGRESS
├── Update imports in all files
├── Fix compilation errors
└── Verify build

Phase 3: 📋 TODO
├── Remove old core/feature directories
├── Update manifests
├── Full testing
└── Performance validation
```

## File Statistics

```
Total organized: 190 files
Old structure:   295 files (still exists during migration)

Distribution:
- Presentation:    64% (122 files) - Largest layer (screens by feature)
- Domain:          7%  (13 files)  - Pure business logic
- UI:              11% (21 files)  - Reusable design system
- Infrastructure:  8%  (15 files)  - Android services
- Data:            6%  (11 files)  - Data access
- DI:              4%  (8 files)   - Dependency injection
```

## Summary

The new architecture provides:

1. **Clear Separation**: Each layer has one purpose
2. **Testability**: Domain layer is 100% testable without Android
3. **Maintainability**: Changes are localized
4. **Scalability**: Easy pattern to follow
5. **Team Collaboration**: Clear boundaries

**See**: [NEW_ARCHITECTURE_GUIDE.md](NEW_ARCHITECTURE_GUIDE.md) for detailed developer guide.
