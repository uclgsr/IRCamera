# Architecture Reorganization Plan

## Executive Summary

This document outlines the plan to reorganize the IRCamera Android application from a core/feature split architecture to a Clean Architecture with clear domain boundaries and separation of concerns.

## Current Architecture Problems

### 1. Bloated Core Module (88 files)

The `core` package contains too many unrelated concerns:
- Application infrastructure (App.kt, Services)
- Business logic (SessionManager, RecordingService)
- Data access (Repositories, Recorders)
- UI components (BaseViewModel, Compose components)
- Cross-cutting concerns (Monitoring, Security, Threading)
- Domain models mixed with data models

### 2. Unclear Feature Boundaries (207 files)

Features have inconsistent structure:
- Some features follow data/domain/presentation layers (gsr, thermal, network)
- Others have flat structure (camera, settings)
- Circular dependencies between features
- Shared functionality duplicated across features

### 3. Mixed Responsibilities

Examples of mixed concerns:
- `core/data/` contains: networking, GSR recording, thermal recording, RGB camera, security, auth, time sync
- `RecordingService` in core mixes Android framework with business logic
- UI components scattered between core/ui and feature/*/ui

### 4. Poor Dependency Management

- DI modules in both core/di and feature/*/di
- No clear dependency flow
- Core depends on feature modules (anti-pattern)
- Circular dependencies between modules

## Proposed Architecture

### Overview

Restructure using Clean Architecture principles with clear separation:

```
app/src/main/java/mpdc4gsr/
├── domain/              # Business logic (pure Kotlin, no Android deps)
├── data/                # Data layer implementations
├── presentation/        # UI and ViewModels (organized by feature)
├── infrastructure/      # Cross-cutting Android framework concerns
├── ui/                  # Design system and reusable UI
└── di/                  # Centralized dependency injection
```

### Layer Responsibilities

#### 1. Domain Layer (`domain/`)

Pure Kotlin, no Android dependencies. Contains:
- **Business models**: Pure data classes representing business concepts
- **Repository interfaces**: Contracts for data access
- **Use cases**: Business logic operations

```
domain/
├── model/
│   ├── session/
│   │   ├── Session.kt
│   │   ├── SessionMetadata.kt
│   │   └── SessionState.kt
│   ├── sensor/
│   │   ├── GSRSample.kt
│   │   ├── ThermalFrame.kt
│   │   └── RGBFrame.kt
│   ├── device/
│   │   ├── Device.kt
│   │   └── DeviceConnection.kt
│   └── network/
│       ├── NetworkMessage.kt
│       └── NetworkStatus.kt
├── repository/
│   ├── SessionRepository.kt
│   ├── SensorRepository.kt
│   ├── DeviceRepository.kt
│   └── NetworkRepository.kt
└── usecase/
    ├── session/
    │   ├── StartSessionUseCase.kt
    │   ├── StopSessionUseCase.kt
    │   └── ExportSessionUseCase.kt
    ├── sensor/
    │   ├── RecordGSRDataUseCase.kt
    │   ├── RecordThermalDataUseCase.kt
    │   └── RecordRGBDataUseCase.kt
    └── network/
        ├── ConnectToDeviceUseCase.kt
        └── SyncTimeUseCase.kt
```

#### 2. Data Layer (`data/`)

Implementation of domain contracts. Contains:
- **Repository implementations**: Concrete data access
- **Data sources**: Local and remote data sources
- **Mappers**: Convert between data and domain models

```
data/
├── repository/
│   ├── SessionRepositoryImpl.kt
│   ├── SensorRepositoryImpl.kt
│   ├── DeviceRepositoryImpl.kt
│   └── NetworkRepositoryImpl.kt
├── source/
│   ├── local/
│   │   ├── SessionLocalDataSource.kt
│   │   ├── SensorLocalDataSource.kt
│   │   └── PreferencesDataSource.kt
│   ├── remote/
│   │   ├── NetworkRemoteDataSource.kt
│   │   └── PCControllerDataSource.kt
│   └── hardware/
│       ├── GSRHardwareDataSource.kt
│       ├── ThermalHardwareDataSource.kt
│       └── RGBCameraDataSource.kt
├── mapper/
│   ├── SessionMapper.kt
│   ├── SensorDataMapper.kt
│   └── DeviceMapper.kt
└── model/
    ├── SessionEntity.kt
    ├── SensorDataEntity.kt
    └── DeviceEntity.kt
```

#### 3. Presentation Layer (`presentation/`)

UI and ViewModels organized by feature. Contains:
- **Screens**: Composable screens per feature
- **ViewModels**: Screen state management
- **Navigation**: Feature-level navigation

```
presentation/
├── screens/
│   ├── camera/
│   │   ├── CameraScreen.kt
│   │   ├── CameraViewModel.kt
│   │   └── CameraUiState.kt
│   ├── gsr/
│   │   ├── GSRMonitorScreen.kt
│   │   ├── GSRMonitorViewModel.kt
│   │   ├── GSRDeviceScreen.kt
│   │   └── GSRDeviceViewModel.kt
│   ├── thermal/
│   │   ├── ThermalMonitorScreen.kt
│   │   ├── ThermalViewModel.kt
│   │   └── ThermalCalibrationScreen.kt
│   ├── network/
│   │   ├── NetworkDashboardScreen.kt
│   │   ├── NetworkViewModel.kt
│   │   └── DevicePairingScreen.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   └── PreferencesScreen.kt
│   └── main/
│       ├── MainScreen.kt
│       ├── MainViewModel.kt
│       └── DashboardScreen.kt
├── navigation/
│   ├── AppNavigation.kt
│   ├── NavigationRoutes.kt
│   └── NavigationAnimations.kt
└── common/
    ├── BaseViewModel.kt
    └── UiState.kt
```

#### 4. Infrastructure Layer (`infrastructure/`)

Android framework concerns and cross-cutting functionality:

```
infrastructure/
├── service/
│   ├── RecordingService.kt
│   ├── BackgroundScanService.kt
│   └── TimeSyncService.kt
├── monitoring/
│   ├── PerformanceMetrics.kt
│   ├── TelemetryManager.kt
│   └── CrashReporting.kt
├── security/
│   ├── AuthenticationManager.kt
│   ├── SecurityMonitor.kt
│   └── RoleBasedAccessControl.kt
├── sync/
│   ├── TimeSyncManager.kt
│   ├── TimeManager.kt
│   └── TimestampManager.kt
└── platform/
    ├── App.kt
    ├── PermissionManager.kt
    └── NotificationManager.kt
```

#### 5. UI Layer (`ui/`)

Design system and reusable UI components:

```
ui/
├── components/
│   ├── buttons/
│   │   ├── PrimaryButton.kt
│   │   └── SecondaryButton.kt
│   ├── cards/
│   │   ├── SensorCard.kt
│   │   ├── StatusCard.kt
│   │   └── InfoCard.kt
│   ├── dialogs/
│   │   ├── ConfirmationDialog.kt
│   │   └── InputDialog.kt
│   ├── charts/
│   │   ├── LineChart.kt
│   │   └── RealTimePlot.kt
│   └── indicators/
│       ├── LoadingIndicator.kt
│       └── StatusIndicator.kt
├── theme/
│   ├── Theme.kt
│   ├── Color.kt
│   ├── Typography.kt
│   └── Shape.kt
└── utils/
    ├── ComposeExtensions.kt
    ├── PermissionUtils.kt
    └── PreviewUtils.kt
```

#### 6. Dependency Injection (`di/`)

Centralized DI configuration:

```
di/
├── AppModule.kt              # Application-level dependencies
├── DomainModule.kt           # Use cases
├── DataModule.kt             # Repositories and data sources
├── InfrastructureModule.kt   # Services and monitoring
└── PresentationModule.kt     # ViewModels
```

## Migration Strategy

### Phase 1: Preparation (Day 1)

1. **Document current dependencies**
   - Map all current imports and dependencies
   - Identify circular dependencies
   - Create dependency graph

2. **Create new package structure**
   - Create new directory structure
   - Add package-info files with documentation

3. **Set up testing infrastructure**
   - Ensure existing tests still work
   - Add tests for migration validation

### Phase 2: Domain Layer Migration (Day 2-3)

1. **Extract domain models**
   - Move pure data classes to domain/model
   - Remove Android dependencies
   - Create repository interfaces

2. **Create use cases**
   - Extract business logic from ViewModels
   - Create dedicated use case classes
   - Write unit tests for use cases

### Phase 3: Data Layer Migration (Day 4-5)

1. **Implement repositories**
   - Move repository implementations to data/repository
   - Create data sources
   - Implement mappers

2. **Organize data sources**
   - Separate local, remote, and hardware data sources
   - Clean up data access code

### Phase 4: Presentation Layer Migration (Day 6-7)

1. **Reorganize screens**
   - Move screens to presentation/screens by feature
   - Update ViewModels to use use cases
   - Fix navigation

2. **Update DI**
   - Centralize DI modules
   - Remove circular dependencies
   - Verify injection works

### Phase 5: Infrastructure Migration (Day 8-9)

1. **Move services**
   - Move Android services to infrastructure
   - Clean up service dependencies

2. **Consolidate cross-cutting concerns**
   - Move monitoring, security, sync to infrastructure

### Phase 6: UI Layer Migration (Day 10)

1. **Extract design system**
   - Move reusable components to ui/components
   - Organize by component type
   - Update usages

### Phase 7: Testing and Validation (Day 11-12)

1. **Run full test suite**
2. **Build verification**
3. **Manual testing**
4. **Performance verification**

### Phase 8: Cleanup (Day 13)

1. **Remove old packages**
2. **Update documentation**
3. **Update README**

## Benefits

1. **Clear Separation of Concerns**: Each layer has a single responsibility
2. **Testability**: Pure domain logic is easily testable
3. **Maintainability**: Changes are localized to specific layers
4. **Scalability**: Easy to add new features following the pattern
5. **Team Collaboration**: Clear boundaries reduce conflicts
6. **Dependency Management**: Unidirectional dependency flow

## Dependency Rules

1. **Domain** has no dependencies on other layers
2. **Data** depends only on domain
3. **Presentation** depends on domain (for use cases)
4. **Infrastructure** depends on domain and data
5. **UI** has no domain dependencies (pure UI)
6. **DI** depends on all layers (wires everything together)

## File Count Impact

Current:
- core/: 88 files
- feature/: 207 files
- Total: 295 files

After reorganization:
- domain/: ~60 files (models, repositories, use cases)
- data/: ~80 files (implementations, sources, mappers)
- presentation/: ~100 files (screens, ViewModels)
- infrastructure/: ~30 files (services, monitoring)
- ui/: ~20 files (design system)
- di/: ~5 files (DI modules)
- Total: ~295 files (same count, better organized)

## Risks and Mitigation

### Risk 1: Breaking Changes
**Mitigation**: Thorough testing at each phase, incremental migration

### Risk 2: Import Hell
**Mitigation**: Use IDE refactoring tools, automated import updates

### Risk 3: Circular Dependencies
**Mitigation**: Strict dependency rules, architecture tests

### Risk 4: Time Investment
**Mitigation**: Phased approach allows for incremental progress

## Success Metrics

1. ✓ No circular dependencies between packages
2. ✓ Domain layer has zero Android dependencies
3. ✓ All tests pass
4. ✓ Build time not increased
5. ✓ Code coverage maintained or improved
6. ✓ No performance regressions

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Clean Architecture Guide](https://developer.android.com/topic/architecture)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
