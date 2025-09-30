# Architecture Migration Plan - Visual Guide

**Status**: Detailed migration roadmap  
**Timeline**: 6-8 weeks  
**Approach**: Incremental, feature-by-feature

---

## Current vs Recommended Architecture

### Current Architecture (Problems)

```
app/src/main/java/mpdc4gsr/
│
├── activities/ (41 files)           ❌ FLAT STRUCTURE
│   ├── MainActivity.kt
│   ├── MainActivityLegacy.kt        ❌ DUPLICATE
│   ├── MainActivityAlternative.kt   ❌ DUPLICATE
│   ├── SimplifiedMainActivityCompose.kt ❌ DUPLICATE
│   ├── GSRQuickRecordingActivityCompose.kt
│   ├── ThermalCameraActivity.kt
│   └── [35 more activities...]
│
├── viewmodel/ (3 files)             ❌ SEPARATED FROM FEATURES
│   ├── BaseViewModel.kt
│   ├── MainActivityViewModel.kt
│   └── SettingsViewModel.kt
│
├── sensors/gsr/ (25 files)          ❌ MIXED CONCERNS
│   ├── GSRDataViewComposeActivity.kt      (UI)
│   ├── SessionManagerViewModel.kt          (Presentation)
│   ├── GSRDataPersistence.kt              (Data)
│   ├── GSRCalculationUtils.kt             (Domain)
│   ├── GSRSensorRecorder.kt               (Data)
│   └── [20 more files...]
│
├── compose/ (77 files)              ⚠️ PARTIALLY GOOD
│   ├── screens/                     ✅ Good
│   ├── components/                  ✅ Good
│   ├── base/                        ✅ Good
│   └── [other packages]
│
├── network/ (35 files)              ❌ MIXED WITH VIEWMODELS
├── camera/ (16 files)               ❌ DUPLICATE WITH sensors/camera
└── ui_components/ (8 files)         ❌ LEGACY, DUPLICATE
```

**Problems**:
- No clear feature boundaries
- Mixed responsibilities
- Hard to navigate
- Duplicate code
- Difficult to test

### Recommended Architecture (Clean)

```
app/src/main/java/com/ircamera/
│
├── core/                            ✅ SHARED FOUNDATION
│   ├── ui/
│   │   ├── base/
│   │   │   ├── BaseActivity.kt
│   │   │   └── BaseViewModel.kt
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Typography.kt
│   │   └── components/
│   │       ├── Button.kt
│   │       ├── Card.kt
│   │       └── TextField.kt
│   ├── domain/
│   │   ├── model/
│   │   └── usecase/
│   ├── data/
│   │   └── repository/
│   ├── di/
│   │   └── CoreModule.kt
│   └── util/
│       └── Extensions.kt
│
└── feature/                         ✅ FEATURE-BASED
    │
    ├── main/                        ✅ MAIN FEATURE
    │   ├── ui/
    │   │   ├── MainActivity.kt          (Single entry point)
    │   │   ├── MainScreen.kt            (Compose)
    │   │   └── components/
    │   ├── presentation/
    │   │   ├── MainViewModel.kt
    │   │   └── MainUiState.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   └── usecase/
    │   └── data/
    │       └── repository/
    │
    ├── thermal/                     ✅ THERMAL FEATURE
    │   ├── ui/
    │   │   ├── camera/
    │   │   │   ├── ThermalCameraScreen.kt
    │   │   │   └── components/
    │   │   └── gallery/
    │   │       └── ThermalGalleryScreen.kt
    │   ├── presentation/
    │   │   ├── camera/
    │   │   │   ├── ThermalCameraViewModel.kt
    │   │   │   └── ThermalCameraUiState.kt
    │   │   └── gallery/
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── ThermalImage.kt
    │   │   │   └── ThermalSettings.kt
    │   │   ├── usecase/
    │   │   │   ├── CaptureThermalImageUseCase.kt
    │   │   │   └── ProcessThermalDataUseCase.kt
    │   │   └── repository/
    │   │       └── ThermalRepository.kt
    │   └── data/
    │       ├── repository/
    │       │   └── ThermalRepositoryImpl.kt
    │       └── source/
    │           ├── ThermalCameraSource.kt
    │           └── ThermalLocalDataSource.kt
    │
    ├── gsr/                         ✅ GSR FEATURE
    │   ├── ui/
    │   │   ├── recording/
    │   │   │   ├── GSRRecordingScreen.kt
    │   │   │   └── components/
    │   │   ├── session/
    │   │   │   ├── SessionManagerScreen.kt
    │   │   │   └── SessionDetailScreen.kt
    │   │   └── settings/
    │   │       └── GSRSettingsScreen.kt
    │   ├── presentation/
    │   │   ├── recording/
    │   │   │   ├── GSRRecordingViewModel.kt
    │   │   │   └── GSRRecordingUiState.kt
    │   │   └── session/
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── GSRData.kt
    │   │   │   └── GSRSession.kt
    │   │   ├── usecase/
    │   │   │   ├── RecordGSRDataUseCase.kt
    │   │   │   ├── CalculateMetricsUseCase.kt
    │   │   │   └── ExportSessionUseCase.kt
    │   │   └── repository/
    │   │       └── GSRRepository.kt
    │   └── data/
    │       ├── repository/
    │       │   └── GSRRepositoryImpl.kt
    │       └── source/
    │           ├── ShimmerDataSource.kt
    │           └── GSRLocalDataSource.kt
    │
    ├── network/                     ✅ NETWORK FEATURE
    │   ├── ui/
    │   ├── presentation/
    │   ├── domain/
    │   └── data/
    │
    └── settings/                    ✅ SETTINGS FEATURE
        ├── ui/
        ├── presentation/
        ├── domain/
        └── data/
```

**Benefits**:
- Clear feature boundaries
- Testable layers
- Easy navigation
- Single responsibility
- Scalable

---

## Migration Steps - Detailed

### Step 1: Create Core Foundation

```bash
# Create core package structure
mkdir -p app/src/main/java/com/ircamera/core/{ui/base,ui/theme,ui/components,domain,data,di,util}

# Move base classes
mv app/src/main/java/mpdc4gsr/compose/base/BaseComposeActivity.kt \
   app/src/main/java/com/ircamera/core/ui/base/BaseActivity.kt

mv app/src/main/java/mpdc4gsr/viewmodel/BaseViewModel.kt \
   app/src/main/java/com/ircamera/core/ui/base/BaseViewModel.kt

# Move theme
mv app/src/main/java/mpdc4gsr/compose/theme/* \
   app/src/main/java/com/ircamera/core/ui/theme/

# Move reusable components
mv app/src/main/java/mpdc4gsr/compose/components/* \
   app/src/main/java/com/ircamera/core/ui/components/
```

### Step 2: Migrate Main Feature (Example)

```bash
# Create feature structure
mkdir -p app/src/main/java/com/ircamera/feature/main/{ui,presentation,domain,data}

# Move MainActivity
mv app/src/main/java/mpdc4gsr/activities/MainActivity.kt \
   app/src/main/java/com/ircamera/feature/main/ui/MainActivity.kt

# Move MainScreen
mv app/src/main/java/mpdc4gsr/compose/screens/MainScreen.kt \
   app/src/main/java/com/ircamera/feature/main/ui/MainScreen.kt

# Move ViewModel
mv app/src/main/java/mpdc4gsr/viewmodel/MainActivityViewModel.kt \
   app/src/main/java/com/ircamera/feature/main/presentation/MainViewModel.kt

# Delete duplicate variants
rm app/src/main/java/mpdc4gsr/activities/MainActivityLegacy.kt
rm app/src/main/java/mpdc4gsr/activities/MainActivityAlternative.kt
rm app/src/main/java/mpdc4gsr/activities/SimplifiedMainActivityCompose.kt
```

### Step 3: Refactor Code References

**Before**:
```kotlin
package mpdc4gsr.activities

import mpdc4gsr.viewmodel.MainActivityViewModel
import mpdc4gsr.compose.screens.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}
```

**After**:
```kotlin
package com.ircamera.feature.main.ui

import com.ircamera.feature.main.presentation.MainViewModel
import com.ircamera.core.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    
    private val viewModel: MainViewModel by viewModels { viewModelFactory }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onAction = viewModel::handleAction
            )
        }
    }
}
```

---

## Feature-by-Feature Migration Guide

### Feature 1: Main/Dashboard (Week 3)

**Complexity**: Low  
**Dependencies**: None  
**Files to migrate**: 5

**Current Files**:
```
mpdc4gsr/activities/
├── MainActivity.kt
├── MainActivityLegacy.kt        (DELETE)
├── MainActivityAlternative.kt   (DELETE)
└── SimplifiedMainActivityCompose.kt (DELETE)

mpdc4gsr/viewmodel/
└── MainActivityViewModel.kt

mpdc4gsr/compose/screens/
└── MainScreen.kt
```

**New Structure**:
```
com/ircamera/feature/main/
├── ui/
│   ├── MainActivity.kt
│   ├── MainScreen.kt
│   └── components/
│       └── StatusCard.kt
├── presentation/
│   ├── MainViewModel.kt
│   └── MainUiState.kt
├── domain/
│   ├── usecase/
│   │   └── GetSystemStatusUseCase.kt
│   └── model/
│       └── SystemStatus.kt
└── data/
    └── repository/
        └── SystemRepositoryImpl.kt
```

**Steps**:
1. Create feature structure
2. Move MainActivity (keep only one)
3. Create MainUiState data class
4. Refactor MainViewModel to use UiState
5. Add Hilt annotations
6. Update imports throughout project
7. Delete duplicate main activities
8. Test thoroughly

### Feature 2: Settings (Week 3)

**Complexity**: Low  
**Dependencies**: None  
**Files to migrate**: 3

**Current Files**:
```
mpdc4gsr/activities/
└── SettingsComposeActivity.kt

mpdc4gsr/compose/screens/
└── SettingsScreen.kt
```

**New Structure**:
```
com/ircamera/feature/settings/
├── ui/
│   ├── SettingsActivity.kt
│   ├── SettingsScreen.kt
│   └── components/
│       ├── SettingItem.kt
│       └── SettingSection.kt
├── presentation/
│   ├── SettingsViewModel.kt
│   └── SettingsUiState.kt
├── domain/
│   ├── usecase/
│   │   ├── UpdateSettingUseCase.kt
│   │   └── GetSettingsUseCase.kt
│   └── model/
│       └── AppSettings.kt
└── data/
    ├── repository/
    │   └── SettingsRepositoryImpl.kt
    └── source/
        └── SettingsDataSource.kt
```

### Feature 3: GSR (Week 5)

**Complexity**: High  
**Dependencies**: Shimmer SDK, BleModule  
**Files to migrate**: 25+

**Current Files** (scattered):
```
mpdc4gsr/activities/
└── GSRQuickRecordingActivityCompose.kt

mpdc4gsr/sensors/gsr/ (25 files)
├── GSRDataViewComposeActivity.kt
├── SessionManagerViewModel.kt
├── GSRDataPersistence.kt
├── GSRCalculationUtils.kt
└── [21 more files...]
```

**New Structure** (organized):
```
com/ircamera/feature/gsr/
├── ui/
│   ├── recording/
│   │   ├── GSRRecordingActivity.kt
│   │   ├── GSRRecordingScreen.kt
│   │   └── components/
│   ├── session/
│   │   ├── SessionManagerScreen.kt
│   │   ├── SessionDetailScreen.kt
│   │   └── components/
│   ├── settings/
│   │   └── GSRSettingsScreen.kt
│   └── data/
│       └── GSRDataViewScreen.kt
│
├── presentation/
│   ├── recording/
│   │   ├── GSRRecordingViewModel.kt
│   │   └── GSRRecordingUiState.kt
│   ├── session/
│   │   ├── SessionManagerViewModel.kt
│   │   └── SessionUiState.kt
│   └── settings/
│       └── GSRSettingsViewModel.kt
│
├── domain/
│   ├── model/
│   │   ├── GSRData.kt
│   │   ├── GSRSession.kt
│   │   └── GSRMetrics.kt
│   ├── usecase/
│   │   ├── RecordGSRDataUseCase.kt
│   │   ├── CalculateGSRMetricsUseCase.kt
│   │   ├── ExportSessionUseCase.kt
│   │   ├── GetSessionsUseCase.kt
│   │   └── SaveSessionUseCase.kt
│   └── repository/
│       └── GSRRepository.kt (interface)
│
└── data/
    ├── repository/
    │   └── GSRRepositoryImpl.kt
    ├── source/
    │   ├── local/
    │   │   └── GSRLocalDataSource.kt
    │   └── remote/
    │       └── ShimmerDataSource.kt
    └── mapper/
        └── GSRMapper.kt
```

**Steps**:
1. Create complete feature structure
2. Migrate domain models (pure Kotlin)
3. Create repository interface
4. Implement repository with existing code
5. Extract use cases from ViewModels
6. Refactor ViewModels to use use cases
7. Move UI files
8. Update all imports
9. Add DI modules
10. Test each layer independently

### Feature 4: Thermal (Week 6)

**Complexity**: Very High  
**Dependencies**: TC001 SDK, thermalunified module  
**Files to migrate**: 30+

**Strategy**: Similar to GSR but more complex due to hardware integration

---

## Code Example: Before & After

### Before (Current - Mixed Concerns)

```kotlin
// mpdc4gsr/sensors/gsr/SessionManagerViewModel.kt
class SessionManagerViewModel : ViewModel() {
    
    // UI State (presentation)
    val sessions = MutableLiveData<List<Session>>()
    val isLoading = MutableLiveData<Boolean>()
    
    // Business Logic (domain)
    fun loadSessions() {
        viewModelScope.launch {
            isLoading.value = true
            
            // Data access (data layer)
            val db = Room.databaseBuilder(/*...*/).build()
            val dao = db.sessionDao()
            
            // Complex calculation (domain)
            val rawData = dao.getAllSessions()
            val processed = rawData.map { session ->
                val avgGSR = session.dataPoints.map { it.value }.average()
                val duration = session.endTime - session.startTime
                // ... more calculations
                ProcessedSession(session, avgGSR, duration)
            }
            
            sessions.value = processed
            isLoading.value = false
        }
    }
    
    // More mixed concerns...
}
```

**Problems**:
- ViewModel knows about database
- Business logic in ViewModel
- Hard to test
- Tight coupling

### After (Recommended - Clean Architecture)

```kotlin
// feature/gsr/domain/usecase/GetSessionsUseCase.kt
class GetSessionsUseCase @Inject constructor(
    private val repository: GSRRepository
) {
    suspend operator fun invoke(): Result<List<GSRSession>> {
        return repository.getSessions()
    }
}

// feature/gsr/domain/usecase/CalculateSessionMetricsUseCase.kt
class CalculateSessionMetricsUseCase {
    operator fun invoke(session: GSRSession): SessionMetrics {
        val avgGSR = session.dataPoints.map { it.value }.average()
        val duration = session.endTime - session.startTime
        return SessionMetrics(avgGSR, duration)
    }
}

// feature/gsr/presentation/session/SessionManagerViewModel.kt
@HiltViewModel
class SessionManagerViewModel @Inject constructor(
    private val getSessions: GetSessionsUseCase,
    private val calculateMetrics: CalculateSessionMetricsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SessionManagerUiState())
    val uiState: StateFlow<SessionManagerUiState> = _uiState.asStateFlow()
    
    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getSessions()
                .onSuccess { sessions ->
                    val withMetrics = sessions.map { session ->
                        SessionWithMetrics(
                            session = session,
                            metrics = calculateMetrics(session)
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            sessions = withMetrics,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

// feature/gsr/presentation/session/SessionManagerUiState.kt
data class SessionManagerUiState(
    val sessions: List<SessionWithMetrics> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// feature/gsr/data/repository/GSRRepositoryImpl.kt
class GSRRepositoryImpl @Inject constructor(
    private val localDataSource: GSRLocalDataSource,
    private val mapper: GSRMapper
) : GSRRepository {
    
    override suspend fun getSessions(): Result<List<GSRSession>> {
        return try {
            val entities = localDataSource.getAllSessions()
            val sessions = entities.map { mapper.toDomain(it) }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Benefits**:
- Clear separation of concerns
- Each component testable independently
- Reusable use cases
- Loosely coupled
- Easy to maintain

---

## Testing Strategy by Layer

### Domain Layer (Pure Logic)

```kotlin
// No Android dependencies
class CalculateSessionMetricsUseCaseTest {
    
    private lateinit var useCase: CalculateSessionMetricsUseCase
    
    @Before
    fun setup() {
        useCase = CalculateSessionMetricsUseCase()
    }
    
    @Test
    fun `calculate metrics correctly`() {
        val session = GSRSession(
            dataPoints = listOf(
                GSRDataPoint(value = 100f),
                GSRDataPoint(value = 200f),
                GSRDataPoint(value = 300f)
            ),
            startTime = 0L,
            endTime = 1000L
        )
        
        val metrics = useCase(session)
        
        assertEquals(200f, metrics.averageGSR, 0.01f)
        assertEquals(1000L, metrics.duration)
    }
}
```

### Data Layer (Mock Dependencies)

```kotlin
class GSRRepositoryImplTest {
    
    @Mock
    private lateinit var localDataSource: GSRLocalDataSource
    
    @Mock
    private lateinit var mapper: GSRMapper
    
    private lateinit var repository: GSRRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GSRRepositoryImpl(localDataSource, mapper)
    }
    
    @Test
    fun `getSessions returns mapped data`() = runTest {
        val entity = SessionEntity(id = 1)
        val domain = GSRSession(id = 1)
        
        whenever(localDataSource.getAllSessions()).thenReturn(listOf(entity))
        whenever(mapper.toDomain(entity)).thenReturn(domain)
        
        val result = repository.getSessions()
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }
}
```

### Presentation Layer (ViewModel)

```kotlin
class SessionManagerViewModelTest {
    
    @Mock
    private lateinit var getSessions: GetSessionsUseCase
    
    @Mock
    private lateinit var calculateMetrics: CalculateSessionMetricsUseCase
    
    private lateinit var viewModel: SessionManagerViewModel
    
    @Test
    fun `loadSessions updates state correctly`() = runTest {
        val session = GSRSession(id = 1)
        val metrics = SessionMetrics(averageGSR = 200f)
        
        whenever(getSessions()).thenReturn(Result.success(listOf(session)))
        whenever(calculateMetrics(session)).thenReturn(metrics)
        
        viewModel = SessionManagerViewModel(getSessions, calculateMetrics)
        viewModel.loadSessions()
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.sessions.size)
    }
}
```

---

## File Movement Script

```bash
#!/bin/bash

# Architecture migration helper script

echo "IRCamera Architecture Migration"
echo "================================"
echo ""

# Phase 1: Core Foundation
echo "Phase 1: Creating core foundation..."

# Create core structure
mkdir -p app/src/main/java/com/ircamera/core/{ui/base,ui/theme,ui/components,domain,data,di,util}

# Move base classes
cp app/src/main/java/mpdc4gsr/compose/base/BaseComposeActivity.kt \
   app/src/main/java/com/ircamera/core/ui/base/BaseActivity.kt

cp app/src/main/java/mpdc4gsr/viewmodel/BaseViewModel.kt \
   app/src/main/java/com/ircamera/core/ui/base/BaseViewModel.kt

# Move theme
cp -r app/src/main/java/mpdc4gsr/compose/theme/* \
   app/src/main/java/com/ircamera/core/ui/theme/

echo "Core foundation created ✓"
echo ""

# Phase 2: Feature structure
echo "Phase 2: Creating feature structure..."

# Create feature directories
for feature in main thermal gsr network settings; do
    mkdir -p app/src/main/java/com/ircamera/feature/$feature/{ui,presentation,domain,data}
    echo "  - Created $feature feature structure"
done

echo "Feature structure created ✓"
echo ""

echo "Migration foundation ready!"
echo "Next: Start migrating features one by one"
```

---

## Summary

**Current State**: Mixed architecture, unclear boundaries

**Recommended State**: Clean Architecture with feature-based organization

**Migration Approach**: Incremental, feature-by-feature

**Timeline**: 6-8 weeks

**Risk**: Low (gradual migration, comprehensive testing)

**Impact**: High (maintainability, scalability, testability)

---

*Follow this plan to transform the IRCamera app into a modern, maintainable Android application.*
