# IRCamera Application Architecture

## Clean Architecture Structure

This application follows Clean Architecture principles with clear layer separation.

```
mpdc4gsr/
├── domain/              # Business logic (pure Kotlin, no Android)
│   ├── model/           # Domain models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business use cases
│
├── data/                # Data access layer
│   ├── repository/      # Repository implementations
│   ├── source/          # Data sources (local, remote, hardware)
│   ├── mapper/          # Data-to-domain mappers
│   └── model/           # Data entities
│
├── presentation/        # UI layer
│   ├── screens/         # Feature screens (camera, gsr, thermal, etc.)
│   ├── navigation/      # App navigation
│   └── common/          # Shared presentation components
│
├── infrastructure/      # Android framework & cross-cutting concerns
│   ├── service/         # Android services
│   ├── monitoring/      # Performance & telemetry
│   ├── security/        # Authentication & security
│   ├── sync/            # Time synchronization
│   └── platform/        # App initialization & permissions
│
├── ui/                  # Design system
│   ├── components/      # Reusable UI components
│   ├── theme/           # App theme & styling
│   └── utils/           # UI utilities
│
└── di/                  # Dependency injection
    └── *Module.kt       # Hilt DI modules

[OLD STRUCTURE - TO BE REMOVED]
├── core/                # ⚠️ DEPRECATED - Being migrated to new structure
└── feature/             # ⚠️ DEPRECATED - Being migrated to new structure
```

## Quick Start

### For Developers

1. **Read the architecture guide**: [docs/NEW_ARCHITECTURE_GUIDE.md](../../../../docs/NEW_ARCHITECTURE_GUIDE.md)
2. **Understand the layers**: Each layer has a specific purpose and dependency rules
3. **Follow the patterns**: Use existing code as examples

### Adding a New Feature

1. **Domain first**: Create models, repository interface, and use cases in `domain/`
2. **Data implementation**: Implement repository and data sources in `data/`
3. **UI layer**: Create screens and ViewModels in `presentation/screens/your-feature/`
4. **Wire it up**: Add DI module in `di/`

### Dependency Rules

**CRITICAL**: Always follow the dependency flow:

```
Presentation → Domain ← Data
     ↓                   ↑
    UI             Infrastructure
     ↓                   ↑
    DI ← (wires everything)
```

- **Domain** = No dependencies (pure Kotlin)
- **Data** = Depends on domain only
- **Presentation** = Depends on domain (use cases)
- **Infrastructure** = Can depend on domain and data
- **UI** = No domain dependencies
- **DI** = Depends on all (wires them together)

## Layer Details

### Domain Layer
- **Purpose**: Pure business logic
- **No Android**: Must be testable without Android framework
- **Contains**: Models, repository interfaces, use cases
- **Example**: `domain/usecase/gsr/StartRecordingUseCase.kt`

### Data Layer
- **Purpose**: Data access and persistence
- **Implements**: Domain repository interfaces
- **Contains**: Repository implementations, data sources, mappers
- **Example**: `data/repository/SessionRepositoryImpl.kt`

### Presentation Layer
- **Purpose**: UI screens and state management
- **Organized by feature**: Camera, GSR, Thermal, Network, Settings
- **Contains**: Composables, ViewModels, Activities
- **Example**: `presentation/screens/gsr/GSRMonitorScreen.kt`

### Infrastructure Layer
- **Purpose**: Android framework code and cross-cutting concerns
- **Contains**: Services, monitoring, security, time sync
- **Example**: `infrastructure/service/RecordingService.kt`

### UI Layer
- **Purpose**: Reusable design system
- **Contains**: Generic components, theme, utilities
- **Example**: `ui/components/SensorStatusCard.kt`

### DI Layer
- **Purpose**: Dependency injection configuration
- **Contains**: Hilt modules for each layer
- **Example**: `di/DomainModule.kt`

## Testing Strategy

### Domain Tests (Unit)
```kotlin
// Fast, no Android dependencies
class StartRecordingUseCaseTest {
    @Test
    fun `should start recording successfully`() {
        // Pure Kotlin test
    }
}
```

### Data Tests (Integration)
```kotlin
// May use Android Test framework
@RunWith(AndroidJUnit4::class)
class SessionRepositoryImplTest {
    // Test with Room, SharedPreferences, etc.
}
```

### Presentation Tests (UI)
```kotlin
// Test ViewModels and UI
class GSRMonitorViewModelTest {
    // Test state management
}
```

## Migration Status

**Current Status**: Phase 1 Complete

- ✅ New structure created (190 files)
- ✅ Package declarations updated
- ⏳ Import statements being updated
- ⏳ Old structure being removed

**Old Structure**: `core/` and `feature/` directories are deprecated and will be removed once migration is complete.

## Common Patterns

### Use Case Pattern
```kotlin
class DoSomethingUseCase @Inject constructor(
    private val repository: SomeRepository
) {
    suspend operator fun invoke(params: Params): Result<Output> {
        return repository.doSomething(params)
    }
}
```

### Repository Pattern
```kotlin
// Interface in domain
interface SomeRepository {
    suspend fun getData(): Result<Data>
}

// Implementation in data
class SomeRepositoryImpl @Inject constructor(
    private val dataSource: DataSource
) : SomeRepository {
    override suspend fun getData() = dataSource.fetch()
}
```

### ViewModel Pattern
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: UseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    
    fun action() = viewModelScope.launch {
        useCase().collect { result ->
            _state.update { it.copy(data = result) }
        }
    }
}
```

## Documentation

- **Architecture Guide**: [docs/NEW_ARCHITECTURE_GUIDE.md](../../../../docs/NEW_ARCHITECTURE_GUIDE.md)
- **Reorganization Plan**: [docs/ARCHITECTURE_REORGANIZATION.md](../../../../docs/ARCHITECTURE_REORGANIZATION.md)
- **Migration Status**: [docs/ARCHITECTURE_MIGRATION_STATUS.md](../../../../docs/ARCHITECTURE_MIGRATION_STATUS.md)
- **Android Dev Guide**: [docs/android/README.md](../../../../docs/android/README.md)

## Getting Help

1. Read the [NEW_ARCHITECTURE_GUIDE.md](../../../../docs/NEW_ARCHITECTURE_GUIDE.md)
2. Look at similar features for examples
3. Follow the dependency rules
4. Ask the team

---

**Remember**: Clean Architecture = Testable + Maintainable + Scalable code!
