# Android App Architecture Recommendations

**Analysis Date**: 2024-09-30  
**Current Status**: Mixed architecture with ongoing Compose migration  
**Migration Progress**: 95% complete

---

## Executive Summary

The IRCamera Android app shows a **mixed architecture** with both modern (Compose, MVVM) and legacy patterns. While the Compose migration is 95% complete, the file structure and architecture can be significantly improved for better maintainability and scalability.

**Current Issues**:
- 41 activities in flat `/activities/` directory
- ViewModels scattered across multiple packages
- Mixed responsibility in sensor packages
- Unclear separation between UI and business logic
- Redundant activity naming patterns

**Recommended Architecture**: **Clean Architecture + MVVM + Compose**

---

## Current Architecture Analysis

### Current Structure (Problems Highlighted)

```
app/src/main/java/mpdc4gsr/
├── activities/              (41 files) ❌ TOO FLAT, MIXED CONCERNS
│   ├── MainActivity.kt
│   ├── MainActivityLegacy.kt    ❌ LEGACY VARIANT
│   ├── MainActivityAlternative.kt ❌ ALTERNATIVE VARIANT
│   ├── SimplifiedMainActivityCompose.kt
│   └── [38 more activities...]
│
├── viewmodel/               (3 files) ❌ ISOLATED, SHOULD BE WITH FEATURES
│   ├── BaseViewModel.kt
│   └── MainActivityViewModel.kt
│
├── sensors/                 (52 files) ❌ MIXED CONCERNS
│   ├── gsr/                (25 files - Activities + ViewModels + Utils)
│   ├── thermal/            (15 files)
│   ├── camera/             (8 files)
│   └── unified/            (4 files)
│
├── compose/                 (77 files) ⚠️ BETTER, BUT STILL IMPROVABLE
│   ├── screens/            (32 files) ✅ GOOD
│   ├── components/         (7 files)  ✅ GOOD
│   ├── base/               (2 files)  ✅ GOOD
│   └── [other packages]
│
├── network/                 (35 files) ⚠️ MIXED WITH VM
├── camera/                  (16 files) ⚠️ DUPLICATE WITH sensors/camera
├── ui_components/           (8 files)  ⚠️ LEGACY, SHOULD BE IN compose/
└── [other packages]

component/
├── thermalunified/          ⚠️ SEPARATE MODULE, GOOD CONCEPT
├── gsr-recording/           ⚠️ SEPARATE MODULE, GOOD CONCEPT
└── user/                    ⚠️ SEPARATE MODULE
```

### Key Problems

1. **Flat Activity Directory** (41 files)
   - No feature-based grouping
   - Hard to find related activities
   - Mix of old and new implementations

2. **Scattered ViewModels**
   - Some in `/viewmodel/`
   - Some in `/activities/`
   - Some in `/sensors/gsr/`
   - Some in `/network/`
   - No consistent pattern

3. **Duplicate Concerns**
   - `/camera/` and `/sensors/camera/`
   - `/ui_components/` and `/compose/components/`
   - Multiple main activities (MainActivity, MainActivityLegacy, MainActivityAlternative)

4. **Mixed Responsibilities**
   - Activities contain business logic
   - Sensors package has Activities, ViewModels, Utils, and Repository code
   - Network package has ViewModels mixed with network code

---

## Recommended Architecture

### 1. Feature-Based Clean Architecture

Organize by **features/domains**, not by technical layers:

```
app/src/main/java/com/ircamera/
│
├── core/                           ✅ SHARED FOUNDATION
│   ├── ui/
│   │   ├── theme/                  (Theme, Colors, Typography)
│   │   ├── components/             (Reusable UI components)
│   │   └── base/                   (BaseActivity, BaseViewModel)
│   ├── data/
│   │   ├── repository/             (Base repository interfaces)
│   │   └── local/                  (Room, SharedPreferences)
│   ├── domain/
│   │   ├── model/                  (Domain models)
│   │   └── usecase/                (Base use case)
│   ├── di/                         (Dependency Injection - Hilt/Koin)
│   ├── network/                    (Retrofit, API interfaces)
│   └── util/                       (Extensions, helpers)
│
├── feature/                        ✅ FEATURE MODULES
│   │
│   ├── main/                       (Main screen/dashboard)
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainScreen.kt       (Compose)
│   │   │   └── components/         (Feature-specific components)
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   └── usecase/            (GetSensorStatus, etc.)
│   │   ├── data/
│   │   │   ├── repository/         (MainRepositoryImpl)
│   │   │   └── mapper/             (DTO to Domain)
│   │   └── presentation/
│   │       ├── MainViewModel.kt
│   │       └── MainUiState.kt
│   │
│   ├── thermal/                    (Thermal camera feature)
│   │   ├── ui/
│   │   │   ├── ThermalCameraActivity.kt
│   │   │   ├── ThermalCameraScreen.kt
│   │   │   ├── ThermalGalleryScreen.kt
│   │   │   └── components/
│   │   ├── domain/
│   │   │   ├── model/              (ThermalImage, ThermalSettings)
│   │   │   ├── usecase/            (CaptureThermalImage, etc.)
│   │   │   └── repository/         (ThermalRepository interface)
│   │   ├── data/
│   │   │   ├── repository/         (ThermalRepositoryImpl)
│   │   │   ├── source/
│   │   │   │   ├── ThermalCameraSource.kt
│   │   │   │   └── ThermalLocalDataSource.kt
│   │   │   └── mapper/
│   │   └── presentation/
│   │       ├── camera/
│   │       │   ├── ThermalCameraViewModel.kt
│   │       │   └── ThermalCameraUiState.kt
│   │       └── gallery/
│   │           ├── ThermalGalleryViewModel.kt
│   │           └── ThermalGalleryUiState.kt
│   │
│   ├── gsr/                        (GSR sensor feature)
│   │   ├── ui/
│   │   │   ├── recording/
│   │   │   │   ├── GSRRecordingActivity.kt
│   │   │   │   └── GSRRecordingScreen.kt
│   │   │   ├── session/
│   │   │   │   ├── SessionManagerScreen.kt
│   │   │   │   └── SessionDetailScreen.kt
│   │   │   ├── settings/
│   │   │   │   └── GSRSettingsScreen.kt
│   │   │   └── components/
│   │   ├── domain/
│   │   │   ├── model/              (GSRData, GSRSession)
│   │   │   ├── usecase/            (RecordGSRData, ExportSession)
│   │   │   └── repository/         (GSRRepository interface)
│   │   ├── data/
│   │   │   ├── repository/         (GSRRepositoryImpl)
│   │   │   ├── source/
│   │   │   │   ├── ShimmerDataSource.kt
│   │   │   │   └── GSRLocalDataSource.kt
│   │   │   └── mapper/
│   │   └── presentation/
│   │       ├── recording/
│   │       │   ├── GSRRecordingViewModel.kt
│   │       │   └── GSRRecordingUiState.kt
│   │       ├── session/
│   │       │   ├── SessionManagerViewModel.kt
│   │       │   └── SessionUiState.kt
│   │       └── settings/
│   │           └── GSRSettingsViewModel.kt
│   │
│   ├── multimodal/                 (Multi-sensor recording)
│   │   ├── ui/
│   │   ├── domain/
│   │   ├── data/
│   │   └── presentation/
│   │
│   ├── network/                    (Device pairing/networking)
│   │   ├── ui/
│   │   ├── domain/
│   │   ├── data/
│   │   └── presentation/
│   │
│   ├── settings/                   (App settings)
│   │   ├── ui/
│   │   ├── domain/
│   │   ├── data/
│   │   └── presentation/
│   │
│   └── testing/                    (Testing utilities)
│       ├── ui/
│       └── presentation/
│
└── app/
    ├── IRCameraApplication.kt      (Application class)
    └── navigation/                  (App-level navigation)
```

---

## Detailed Recommendations

### 1. Restructure Activities (HIGH PRIORITY)

**Current Problem**: 41 activities in flat `/activities/` directory

**Action**: Organize by feature

```kotlin
// BEFORE (Current)
mpdc4gsr/activities/
├── MainActivity.kt
├── MainActivityLegacy.kt
├── MainActivityAlternative.kt
├── GSRQuickRecordingActivityCompose.kt
├── ThermalCameraActivity.kt
└── [36 more...]

// AFTER (Recommended)
com/ircamera/feature/
├── main/ui/MainActivity.kt
├── thermal/ui/ThermalCameraActivity.kt
├── gsr/ui/recording/GSRRecordingActivity.kt
└── settings/ui/SettingsActivity.kt
```

**Benefits**:
- Clear feature ownership
- Easier to find related code
- Better encapsulation
- Scalable structure

### 2. Consolidate ViewModels with Features (HIGH PRIORITY)

**Current Problem**: ViewModels scattered across 5+ packages

**Action**: Co-locate ViewModels with their features

```kotlin
// BEFORE (Current)
mpdc4gsr/viewmodel/MainActivityViewModel.kt
mpdc4gsr/activities/SimplifiedMainViewModel.kt
mpdc4gsr/sensors/gsr/SessionManagerViewModel.kt
mpdc4gsr/network/DevicePairingViewModel.kt

// AFTER (Recommended)
com/ircamera/feature/main/presentation/MainViewModel.kt
com/ircamera/feature/gsr/presentation/session/SessionManagerViewModel.kt
com/ircamera/feature/network/presentation/DevicePairingViewModel.kt
```

**Benefits**:
- Clear ownership
- Easier refactoring
- Better testability
- Follows single responsibility

### 3. Implement Clean Architecture Layers (MEDIUM PRIORITY)

**Structure each feature with 3 layers**:

```
feature/[feature_name]/
├── ui/                 (Compose UI + Activities)
├── presentation/       (ViewModels + UiState)
├── domain/            (Use Cases + Domain Models + Repository Interfaces)
└── data/              (Repository Implementations + Data Sources + Mappers)
```

**Example - GSR Recording Feature**:

```kotlin
// Domain Layer (Business Logic)
com/ircamera/feature/gsr/domain/
├── model/
│   ├── GSRData.kt              // Pure domain model
│   └── GSRSession.kt
├── usecase/
│   ├── RecordGSRDataUseCase.kt  // Single responsibility
│   └── ExportSessionUseCase.kt
└── repository/
    └── GSRRepository.kt         // Interface

// Data Layer (Implementation)
com/ircamera/feature/gsr/data/
├── repository/
│   └── GSRRepositoryImpl.kt     // Implements interface
├── source/
│   ├── ShimmerDataSource.kt     // Hardware access
│   └── GSRLocalDataSource.kt    // Database access
└── mapper/
    └── GSRMapper.kt             // DTO to Domain

// Presentation Layer (UI State)
com/ircamera/feature/gsr/presentation/
├── GSRRecordingViewModel.kt
└── GSRRecordingUiState.kt       // Immutable UI state

// UI Layer (Compose)
com/ircamera/feature/gsr/ui/
├── recording/
│   ├── GSRRecordingScreen.kt    // Compose UI
│   └── components/              // Feature-specific components
└── GSRRecordingActivity.kt      // Entry point
```

**Benefits**:
- Clear separation of concerns
- Testable layers independently
- Easier to understand code flow
- Follows SOLID principles

### 4. Remove Redundant Code (HIGH PRIORITY)

**Consolidate Duplicate Main Activities**:

```kotlin
// REMOVE:
├── MainActivityLegacy.kt        ❌ Remove
├── MainActivityAlternative.kt   ❌ Remove
└── SimplifiedMainActivityCompose.kt ❌ Remove

// KEEP:
└── MainActivity.kt              ✅ Single entry point
```

**Consolidate Camera Code**:

```kotlin
// BEFORE (Duplicate)
mpdc4gsr/camera/           (16 files)
mpdc4gsr/sensors/camera/   (8 files)

// AFTER (Unified)
com/ircamera/feature/camera/
└── [all camera-related code]
```

**Consolidate UI Components**:

```kotlin
// BEFORE (Duplicate)
mpdc4gsr/ui_components/    (Legacy)
mpdc4gsr/compose/components/

// AFTER (Unified)
com/ircamera/core/ui/components/  (Shared)
com/ircamera/feature/[x]/ui/components/  (Feature-specific)
```

### 5. Introduce Dependency Injection (MEDIUM PRIORITY)

**Current**: Manual instantiation everywhere

**Recommended**: Use Hilt (recommended) or Koin

```kotlin
// Add to build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

**Example - Feature Module DI**:

```kotlin
// Module for GSR feature
@Module
@InstallIn(SingletonComponent::class)
object GSRModule {
    
    @Provides
    @Singleton
    fun provideGSRRepository(
        shimmerDataSource: ShimmerDataSource,
        localDataSource: GSRLocalDataSource
    ): GSRRepository {
        return GSRRepositoryImpl(shimmerDataSource, localDataSource)
    }
    
    @Provides
    fun provideRecordGSRDataUseCase(
        repository: GSRRepository
    ): RecordGSRDataUseCase {
        return RecordGSRDataUseCase(repository)
    }
}

// ViewModel with DI
@HiltViewModel
class GSRRecordingViewModel @Inject constructor(
    private val recordGSRData: RecordGSRDataUseCase,
    private val exportSession: ExportSessionUseCase
) : ViewModel() {
    // Implementation
}
```

**Benefits**:
- Loose coupling
- Easier testing (mock dependencies)
- Centralized dependency management
- Compile-time safety

### 6. Standardize Naming Conventions (LOW PRIORITY)

**Current Issues**:
- Inconsistent activity naming: `*Compose.kt` vs `*ComposeActivity.kt`
- Mixed ViewModel location
- Unclear package naming

**Recommended Standards**:

```kotlin
// Activities
MainActivity.kt                  (not MainActivityCompose)
ThermalCameraActivity.kt
GSRRecordingActivity.kt

// Screens (Compose)
MainScreen.kt
ThermalCameraScreen.kt
GSRRecordingScreen.kt

// ViewModels
MainViewModel.kt                 (not MainActivityViewModel)
ThermalCameraViewModel.kt
GSRRecordingViewModel.kt

// UI State
MainUiState.kt
ThermalCameraUiState.kt

// Use Cases
RecordGSRDataUseCase.kt
ExportSessionUseCase.kt

// Repositories
GSRRepository.kt                 (interface)
GSRRepositoryImpl.kt            (implementation)
```

### 7. Component Module Strategy (MEDIUM PRIORITY)

**Current**: 3 component modules (thermalunified, gsr-recording, user)

**Assessment**: Good concept, needs better organization

**Recommended Structure**:

```
component/
├── core/                        (Shared utilities across components)
│   ├── network/
│   ├── bluetooth/
│   └── storage/
│
├── sensor-thermal/              (Thermal camera SDK wrapper)
│   ├── api/                    (Public interfaces)
│   └── impl/                   (Implementation)
│
├── sensor-gsr/                 (GSR/Shimmer SDK wrapper)
│   ├── api/
│   └── impl/
│
└── sensor-rgb/                 (RGB camera wrapper)
    ├── api/
    └── impl/

// App depends on component APIs only
app/
└── build.gradle.kts
    dependencies {
        implementation(project(":component:sensor-thermal:api"))
        implementation(project(":component:sensor-gsr:api"))
    }
```

**Benefits**:
- Better separation between SDK wrapper and app logic
- Easier to swap implementations
- Cleaner API boundaries
- Better testability

---

## Migration Strategy

### Phase 1: Foundation (Week 1-2) - HIGH IMPACT

**Goal**: Set up new architecture foundation

1. **Create core package structure**
   ```bash
   mkdir -p app/src/main/java/com/ircamera/core/{ui,data,domain,di,util}
   ```

2. **Move and refactor base classes**
   - BaseComposeActivity → core/ui/base/
   - BaseViewModel → core/ui/base/
   - Theme files → core/ui/theme/

3. **Set up Dependency Injection**
   - Add Hilt dependencies
   - Create @HiltAndroidApp application class
   - Create core DI modules

4. **Create feature package structure**
   ```bash
   mkdir -p app/src/main/java/com/ircamera/feature/{main,thermal,gsr,network,settings}
   ```

### Phase 2: Feature Migration (Week 3-6) - INCREMENTAL

**Goal**: Migrate one feature at a time

**Order of Migration** (by priority):

1. **Main/Dashboard** (Week 3)
   - Simplest feature
   - Tests new architecture
   - Removes MainActivity variants

2. **Settings** (Week 3)
   - Simple feature
   - No hardware dependencies
   - Good learning experience

3. **Network/Device Pairing** (Week 4)
   - Moderate complexity
   - Clear boundaries

4. **GSR** (Week 5)
   - Complex feature
   - Many activities
   - Good test of architecture

5. **Thermal** (Week 6)
   - Most complex
   - Hardware integration
   - Requires careful migration

### Phase 3: Cleanup (Week 7-8) - LOW RISK

**Goal**: Remove old code and optimize

1. **Delete old packages**
   - Remove `/activities/` (after migration)
   - Remove `/viewmodel/` (after migration)
   - Remove duplicate packages

2. **Update documentation**
   - Architecture guide
   - Development guidelines
   - Package structure docs

3. **Run comprehensive tests**
   - Unit tests for each layer
   - Integration tests for features
   - UI tests for critical flows

---

## Example: GSR Feature Refactoring

### Before (Current Structure)

```
mpdc4gsr/
├── activities/
│   └── GSRQuickRecordingActivityCompose.kt  (UI + Logic)
├── sensors/gsr/
│   ├── GSRDataViewComposeActivity.kt        (UI)
│   ├── SessionManagerViewModel.kt            (Presentation)
│   ├── GSRDataPersistence.kt                (Data)
│   ├── GSRCalculationUtils.kt               (Domain?)
│   └── GSRSensorRecorder.kt                 (Data)
└── viewmodel/
    └── [no GSR viewmodels here]
```

**Problems**:
- UI scattered across 2 packages
- No clear layer separation
- Business logic mixed with UI
- Hard to test independently

### After (Recommended Structure)

```kotlin
com/ircamera/feature/gsr/

// UI Layer (Compose)
ui/
├── recording/
│   ├── GSRRecordingActivity.kt
│   ├── GSRRecordingScreen.kt
│   └── components/
│       ├── GSRDataDisplay.kt
│       └── RecordingControls.kt
├── session/
│   ├── SessionManagerScreen.kt
│   ├── SessionDetailScreen.kt
│   └── components/
└── settings/
    └── GSRSettingsScreen.kt

// Presentation Layer (ViewModels + State)
presentation/
├── recording/
│   ├── GSRRecordingViewModel.kt
│   └── GSRRecordingUiState.kt
├── session/
│   ├── SessionManagerViewModel.kt
│   └── SessionUiState.kt
└── settings/
    └── GSRSettingsViewModel.kt

// Domain Layer (Business Logic)
domain/
├── model/
│   ├── GSRData.kt              // Pure data class
│   ├── GSRSession.kt
│   └── GSRSettings.kt
├── usecase/
│   ├── RecordGSRDataUseCase.kt  // Single responsibility
│   ├── CalculateGSRMetricsUseCase.kt
│   ├── ExportSessionUseCase.kt
│   └── GetSessionsUseCase.kt
└── repository/
    └── GSRRepository.kt         // Interface only

// Data Layer (Implementation)
data/
├── repository/
│   └── GSRRepositoryImpl.kt     // Implements interface
├── source/
│   ├── local/
│   │   └── GSRLocalDataSource.kt  // Database
│   └── remote/
│       └── ShimmerDataSource.kt   // Hardware
└── mapper/
    └── GSRMapper.kt             // DTO <-> Domain
```

**Benefits**:
- Clear layer separation
- Easy to test each layer
- Reusable use cases
- Maintainable code

---

## Testing Strategy

### Unit Tests by Layer

```kotlin
// Domain Layer Tests (Pure Logic)
class CalculateGSRMetricsUseCaseTest {
    @Test
    fun `calculate average GSR correctly`() {
        val useCase = CalculateGSRMetricsUseCase()
        val data = listOf(100f, 200f, 300f)
        
        val result = useCase(data)
        
        assertEquals(200f, result.average)
    }
}

// Data Layer Tests (Mock Dependencies)
class GSRRepositoryImplTest {
    @Mock lateinit var localDataSource: GSRLocalDataSource
    @Mock lateinit var shimmerDataSource: ShimmerDataSource
    
    private lateinit var repository: GSRRepository
    
    @Before
    fun setup() {
        repository = GSRRepositoryImpl(localDataSource, shimmerDataSource)
    }
    
    @Test
    fun `save GSR data to local storage`() = runTest {
        val data = GSRData(...)
        
        repository.saveGSRData(data)
        
        verify(localDataSource).save(any())
    }
}

// Presentation Layer Tests (ViewModel)
class GSRRecordingViewModelTest {
    @Mock lateinit var recordGSRData: RecordGSRDataUseCase
    
    private lateinit var viewModel: GSRRecordingViewModel
    
    @Test
    fun `start recording updates state`() = runTest {
        viewModel = GSRRecordingViewModel(recordGSRData)
        
        viewModel.startRecording()
        
        assertEquals(RecordingState.RECORDING, viewModel.uiState.value.recordingState)
    }
}

// UI Layer Tests (Compose)
class GSRRecordingScreenTest {
    @Test
    fun `displays recording indicator when recording`() {
        composeTestRule.setContent {
            GSRRecordingScreen(
                uiState = GSRRecordingUiState(recordingState = RecordingState.RECORDING)
            )
        }
        
        composeTestRule.onNodeWithText("Recording...").assertIsDisplayed()
    }
}
```

---

## Benefits Summary

### Short-term Benefits (Immediate)

1. **Better Organization**
   - Clear feature boundaries
   - Easier to find code
   - Reduced cognitive load

2. **Improved Maintainability**
   - Changes isolated to features
   - Less risk of breaking unrelated code
   - Easier code reviews

3. **Better Onboarding**
   - Clear structure for new developers
   - Consistent patterns
   - Self-documenting organization

### Long-term Benefits (Strategic)

1. **Scalability**
   - Easy to add new features
   - Can split features into modules
   - Supports team growth

2. **Testability**
   - Each layer testable independently
   - Mock dependencies easily
   - Higher test coverage

3. **Code Reuse**
   - Shared core components
   - Reusable use cases
   - DRY principles

4. **Team Collaboration**
   - Multiple developers can work on different features
   - Clear ownership
   - Fewer merge conflicts

---

## Implementation Checklist

### Preparation
- [ ] Review and approve architecture plan
- [ ] Set up feature flags for gradual migration
- [ ] Create migration tracking document
- [ ] Brief team on new architecture

### Phase 1: Foundation (Week 1-2)
- [ ] Create core package structure
- [ ] Move base classes to core
- [ ] Set up Hilt dependency injection
- [ ] Create feature package skeleton
- [ ] Update build configuration

### Phase 2: Feature Migration (Week 3-6)
- [ ] Migrate Main/Dashboard feature
- [ ] Migrate Settings feature
- [ ] Migrate Network feature
- [ ] Migrate GSR feature
- [ ] Migrate Thermal feature

### Phase 3: Cleanup (Week 7-8)
- [ ] Remove old packages
- [ ] Delete redundant code
- [ ] Update all documentation
- [ ] Run comprehensive tests
- [ ] Performance testing

### Post-Migration
- [ ] Monitor for issues
- [ ] Gather team feedback
- [ ] Update development guidelines
- [ ] Plan next improvements

---

## Conclusion

**Current State**: Mixed architecture with 95% Compose migration but unclear structure

**Recommended State**: Clean Architecture with feature-based organization

**Effort**: 6-8 weeks for full migration (can be done incrementally)

**Risk**: Low (gradual migration, comprehensive testing)

**Impact**: High (better maintainability, scalability, testability)

**Next Steps**:
1. Review and approve this architecture plan
2. Start with Phase 1 (Foundation)
3. Migrate features one at a time
4. Monitor and adjust as needed

---

*This architecture follows Android best practices and will position the project for long-term success.*
