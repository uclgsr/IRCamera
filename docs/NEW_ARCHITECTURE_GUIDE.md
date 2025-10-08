# New Architecture Guide

## Overview

The IRCamera Android application has been reorganized from a core/feature split to **Clean Architecture** with clear layer separation and unidirectional dependency flow.

## Why the Change?

### Problems with Old Structure

1. **Core module was bloated** (88 files mixing concerns)
   - Services, repositories, UI, monitoring all together
   - Hard to understand what belongs where
   - Circular dependencies

2. **Inconsistent feature organization** (207 files)
   - Some features followed data/domain/presentation
   - Others were flat structures
   - Duplicated code across features

3. **Unclear boundaries**
   - Core depended on features (anti-pattern)
   - Business logic mixed with Android framework
   - Hard to test domain logic

### Benefits of New Structure

1. **Clear Separation**: Each layer has one responsibility
2. **Testability**: Pure domain logic with no Android dependencies
3. **Maintainability**: Changes localized to specific layers
4. **Scalability**: Easy pattern to follow for new features
5. **Team Collaboration**: Clear boundaries reduce merge conflicts

## New Architecture

```
mpdc4gsr/
├── domain/              # Pure business logic (no Android)
├── data/                # Data access implementations
├── presentation/        # UI and ViewModels (by feature)
├── infrastructure/      # Android services & cross-cutting
├── ui/                  # Design system components
└── di/                  # Dependency injection
```

## Layer Details

### 1. Domain Layer (`domain/`)

**Purpose**: Pure business logic and rules

**No Android Dependencies**: This layer must remain Android-free for easy testing

```kotlin
// Example domain model
package mpdc4gsr.domain.model

data class Session(
    val id: String,
    val startTime: Long,
    val metadata: SessionMetadata,
    val sensors: List<SensorType>
)

// Example repository interface
package mpdc4gsr.domain.repository

interface SessionRepository {
    suspend fun startSession(metadata: SessionMetadata): Result<Session>
    suspend fun stopSession(sessionId: String): Result<Unit>
}

// Example use case
package mpdc4gsr.domain.usecase.session

class StartSessionUseCase(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(metadata: SessionMetadata): Result<Session> {
        // Business logic here
        return repository.startSession(metadata)
    }
}
```

**Contents**:
- `model/` - Domain models (Session, Sensor data, Device info)
- `repository/` - Repository interfaces (contracts)
- `usecase/` - Business use cases

**Rules**:
- ✅ Pure Kotlin data classes
- ✅ Business logic in use cases
- ✅ Repository interfaces (not implementations)
- ❌ No Android imports (no Context, no Android classes)
- ❌ No framework dependencies

### 2. Data Layer (`data/`)

**Purpose**: Implement data access defined in domain layer

```kotlin
// Example repository implementation
package mpdc4gsr.data.repository

class SessionRepositoryImpl(
    private val localDataSource: SessionLocalDataSource,
    private val mapper: SessionMapper
) : SessionRepository {
    override suspend fun startSession(metadata: SessionMetadata): Result<Session> {
        val entity = localDataSource.createSession(metadata)
        return Result.success(mapper.toDomain(entity))
    }
}

// Example data source
package mpdc4gsr.data.source.local

class SessionLocalDataSource(
    private val database: SessionDatabase
) {
    suspend fun createSession(metadata: SessionMetadata): SessionEntity {
        // Database operations
    }
}
```

**Contents**:
- `repository/` - Repository implementations
- `source/local/` - Local data sources (database, preferences)
- `source/remote/` - Remote data sources (network, PC controller)
- `source/hardware/` - Hardware data sources (sensors, cameras)
- `mapper/` - Convert between data entities and domain models
- `model/` - Data entities (different from domain models)

**Rules**:
- ✅ Implements domain repository interfaces
- ✅ Can use Android framework (Room, SharedPreferences)
- ✅ Handles data persistence and retrieval
- ❌ No business logic (that's in domain)
- ❌ No UI code

### 3. Presentation Layer (`presentation/`)

**Purpose**: UI screens and ViewModels organized by feature

```kotlin
// Example ViewModel
package mpdc4gsr.presentation.screens.gsr

class GSRMonitorViewModel(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GSRMonitorUiState())
    val uiState = _uiState.asStateFlow()
    
    fun startRecording() = viewModelScope.launch {
        startRecordingUseCase()
            .onSuccess { /* update UI state */ }
            .onFailure { /* handle error */ }
    }
}

// Example screen
package mpdc4gsr.presentation.screens.gsr

@Composable
fun GSRMonitorScreen(
    viewModel: GSRMonitorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI code
}
```

**Contents**:
- `screens/camera/` - Camera-related screens and ViewModels
- `screens/gsr/` - GSR monitoring screens and ViewModels
- `screens/thermal/` - Thermal camera screens and ViewModels
- `screens/network/` - Network configuration screens
- `screens/settings/` - Settings screens
- `screens/main/` - Main dashboard screens
- `navigation/` - Navigation configuration
- `common/` - Shared presentation components (BaseViewModel, UiState)

**Rules**:
- ✅ Organized by feature, not by type
- ✅ ViewModels use domain use cases
- ✅ UI state management
- ✅ Activity/Fragment/Composable screens
- ❌ No direct repository access (use use cases)
- ❌ No business logic (delegate to use cases)

### 4. Infrastructure Layer (`infrastructure/`)

**Purpose**: Android framework concerns and cross-cutting functionality

```kotlin
// Example service
package mpdc4gsr.infrastructure.service

class RecordingService : Service() {
    // Android service implementation
    // Uses domain use cases through DI
}

// Example monitoring
package mpdc4gsr.infrastructure.monitoring

object PerformanceMetrics {
    fun trackOperation(name: String, block: () -> Unit) {
        // Performance tracking
    }
}
```

**Contents**:
- `service/` - Android services (RecordingService, BackgroundScanService)
- `monitoring/` - Performance metrics, telemetry
- `security/` - Authentication, access control, security monitoring
- `sync/` - Time synchronization services
- `platform/` - App class, permission management

**Rules**:
- ✅ Android framework code
- ✅ Cross-cutting concerns
- ✅ Services, receivers, managers
- ✅ Can depend on domain and data
- ❌ No UI code
- ❌ No feature-specific logic

### 5. UI Layer (`ui/`)

**Purpose**: Reusable design system components

```kotlin
// Example reusable component
package mpdc4gsr.ui.components

@Composable
fun SensorStatusCard(
    sensorName: String,
    status: SensorStatus,
    onClick: () -> Unit
) {
    // Reusable card component
}

// Example theme
package mpdc4gsr.ui.theme

val IRCameraColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    // ...
)
```

**Contents**:
- `components/` - Reusable UI components (buttons, cards, dialogs)
- `theme/` - App theme, colors, typography
- `utils/` - UI utilities and extensions

**Rules**:
- ✅ Reusable across features
- ✅ No business logic
- ✅ Composable functions and view components
- ❌ No domain dependencies
- ❌ No feature-specific code

### 6. Dependency Injection (`di/`)

**Purpose**: Wire everything together with Hilt

```kotlin
package mpdc4gsr.di

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideStartSessionUseCase(
        repository: SessionRepository
    ): StartSessionUseCase {
        return StartSessionUseCase(repository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideSessionRepository(
        localDataSource: SessionLocalDataSource
    ): SessionRepository {
        return SessionRepositoryImpl(localDataSource)
    }
}
```

**Contents**:
- `AppModule.kt` - Application-level dependencies
- `DomainModule.kt` - Use case providers
- `DataModule.kt` - Repository and data source providers
- `InfrastructureModule.kt` - Service providers
- Feature-specific modules as needed

**Rules**:
- ✅ Centralized dependency configuration
- ✅ One module per layer
- ✅ Provides instances for DI
- ❌ No business logic

## Dependency Rules

**CRITICAL**: Always follow these dependency rules:

```
┌──────────────┐
│ Presentation │ ──► domain (use cases)
└──────────────┘
       │
       ▼
   ┌──────┐
   │  UI  │ (no dependencies)
   └──────┘

┌──────┐         ┌────────┐
│ Data │ ──────► │ Domain │ (no dependencies)
└──────┘         └────────┘
   ▲
   │
┌────────────────┐
│ Infrastructure │
└────────────────┘
       │
       ▼
   ┌──────┐
   │  DI  │ ──► All layers
   └──────┘
```

**Rules**:
1. **Domain** has NO dependencies on other layers
2. **Data** depends only on domain
3. **Presentation** depends on domain (for use cases)
4. **Infrastructure** can depend on domain and data
5. **UI** has no domain dependencies
6. **DI** wires all layers together

## Migration Status

### ✅ Completed
- New directory structure created
- 190 files copied to new structure
- Package declarations updated
- Documentation created

### ⏳ In Progress
- Updating imports in moved files (Phase 2)
- Fixing compilation errors

### 📋 To Do
- Remove old core/feature directories
- Add data mappers
- Add architecture tests
- Update manifest references
- Full testing

## Developer Guidelines

### Adding a New Feature

1. **Start with domain**:
   ```kotlin
   // domain/model/YourModel.kt
   // domain/repository/YourRepository.kt
   // domain/usecase/your-feature/YourUseCase.kt
   ```

2. **Implement data layer**:
   ```kotlin
   // data/repository/YourRepositoryImpl.kt
   // data/source/.../YourDataSource.kt
   // data/mapper/YourMapper.kt
   ```

3. **Create UI**:
   ```kotlin
   // presentation/screens/your-feature/YourScreen.kt
   // presentation/screens/your-feature/YourViewModel.kt
   ```

4. **Wire with DI**:
   ```kotlin
   // di/YourFeatureModule.kt
   ```

### Testing Strategy

1. **Domain tests** - Pure unit tests, no Android:
   ```kotlin
   class StartSessionUseCaseTest {
       @Test
       fun `should start session successfully`() {
           // No Android dependencies needed
       }
   }
   ```

2. **Data tests** - Test repository implementations:
   ```kotlin
   @RunWith(AndroidJUnit4::class)
   class SessionRepositoryImplTest {
       // Can use Room, Android Test
   }
   ```

3. **Presentation tests** - Test ViewModels:
   ```kotlin
   class GSRMonitorViewModelTest {
       // Test state management
   }
   ```

### Common Patterns

#### 1. Use Case Pattern
```kotlin
class DoSomethingUseCase(
    private val repository: SomeRepository
) {
    suspend operator fun invoke(params: Params): Result<Output> {
        // Business logic here
        return repository.doSomething(params)
    }
}
```

#### 2. Repository Pattern
```kotlin
interface SomeRepository {
    suspend fun getData(): Result<Data>
}

class SomeRepositoryImpl(
    private val dataSource: DataSource,
    private val mapper: Mapper
) : SomeRepository {
    override suspend fun getData(): Result<Data> {
        return dataSource.fetchData()
            .map { mapper.toDomain(it) }
    }
}
```

#### 3. ViewModel Pattern
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: UseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    
    fun doAction() = viewModelScope.launch {
        useCase()
            .onSuccess { data -> 
                _state.update { it.copy(data = data) }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error) }
            }
    }
}
```

## FAQs

**Q: Why not use the old structure?**  
A: The old structure had circular dependencies, mixed concerns, and made testing difficult.

**Q: Can I still access old files?**  
A: Yes, during migration both old (core/feature) and new structures exist. Once migration is complete, old structure will be removed.

**Q: How do I know which layer to put code in?**  
A: Follow this rule:
- Business logic → domain
- Data access → data
- UI/ViewModel → presentation
- Android services → infrastructure
- Reusable UI → ui
- Wiring → di

**Q: What about performance?**  
A: Clean Architecture doesn't impact performance. The extra abstraction is negligible and benefits (testability, maintainability) far outweigh any minimal overhead.

**Q: Do I need to change my feature code?**  
A: Files have been moved but logic unchanged. Once imports are updated, everything will work as before, just better organized.

## References

- [Clean Architecture Documentation](docs/ARCHITECTURE_REORGANIZATION.md)
- [Migration Status](docs/ARCHITECTURE_MIGRATION_STATUS.md)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Clean Architecture by Uncle Bob](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

## Support

For questions about the new architecture:
1. Read this guide
2. Check [ARCHITECTURE_REORGANIZATION.md](ARCHITECTURE_REORGANIZATION.md)
3. Review example code in respective layers
4. Ask team for clarification

---

**Remember**: The goal is **maintainable, testable, scalable** code. Follow the architecture, and the benefits will follow!
