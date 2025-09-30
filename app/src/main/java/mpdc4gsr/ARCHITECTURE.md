# IRCamera Clean Architecture

## Overview

This application follows Clean Architecture principles with MVVM pattern and Jetpack Compose for UI.

## Architecture Layers

### Core Module (`core/`)
Shared foundation used across all features.

- **core/ui**: Shared UI components, base activities, navigation, and theme
  - `BaseComposeActivity.kt`: Base class for Compose activities
  - `navigation/`: Navigation system and routing
  - `theme/`: Material Design theme configuration

- **core/domain**: Shared business logic and domain models
  - Use cases that operate on domain models
  - Domain entities and business rules

- **core/data**: Data layer abstractions and implementations
  - Repository interfaces and implementations
  - Data source abstractions
  - Network and local data management

- **core/di**: Dependency injection setup
  - `AppContainer.kt`: Manual DI container
  - (Future: Hilt modules)

### Feature Modules (`feature/`)
Feature-based organization with clear boundaries.

Each feature follows this structure:
```
feature/{feature-name}/
├── ui/              # Compose screens and UI components
├── presentation/    # ViewModels and UI state
├── domain/          # Feature-specific use cases and models
└── data/            # Feature-specific repositories
```

#### Current Features:

1. **feature/main**: Main application entry point and dashboard
   - `ui/MainActivity.kt`: Primary activity (consolidated from 3 variants)
   - `ui/MainScreen.kt`: Main dashboard screen
   - `presentation/MainActivityViewModel.kt`: Main screen state management

2. **feature/gsr**: GSR sensor integration
   - UI: GSR sensor screens and activities
   - Presentation: GSR ViewModels
   - Domain: GSR-specific business logic
   - Data: Shimmer3 device communication

3. **feature/thermal**: Thermal camera integration
   - UI: Thermal camera screens
   - Presentation: Thermal ViewModels
   - Domain: Temperature processing logic
   - Data: TC001/TC007 device communication

4. **feature/network**: Network device pairing
   - UI: Device pairing screens
   - Presentation: Network ViewModels
   - Domain: Network discovery logic
   - Data: Network repository

5. **feature/settings**: Application settings
   - UI: Settings screens
   - Presentation: Settings ViewModels
   - Domain: Configuration logic
   - Data: Settings repository

## Migration Status

### Completed
- [x] Created core module structure
- [x] Created feature module structure
- [x] Moved base UI components to core/ui
- [x] Moved navigation to core/ui/navigation
- [x] Consolidated MainActivity (3 variants → 1)
- [x] Organized ViewModels by feature
- [x] Created backward compatibility layer
- [x] Set up basic DI infrastructure

### In Progress
- [ ] Migrate remaining activities to feature modules
- [ ] Extract domain logic into use cases
- [ ] Create repository abstractions in core/data
- [ ] Add comprehensive documentation

### Backward Compatibility

During the migration phase, backward compatibility is maintained through:

1. **Type aliases**: `mpdc4gsr.activities.MainActivity` redirects to `mpdc4gsr.feature.main.ui.MainActivity`
2. **Package compatibility**: `mpdc4gsr.compose.navigation` provides type aliases to `mpdc4gsr.core.ui.navigation`
3. **Deprecated annotations**: Old implementations marked with `@Deprecated` to guide migration

## Key Principles

1. **Separation of Concerns**: Each layer has clear responsibilities
2. **Dependency Rule**: Dependencies flow inward (UI → Presentation → Domain → Data)
3. **Feature-based Organization**: Code organized by feature, not technical layer
4. **Single Responsibility**: Each class has one reason to change
5. **Testability**: Clean separation enables easy unit and integration testing

## Navigation

The application uses a unified navigation system:
- **Entry point**: `MainActivity` in `feature/main/ui`
- **Navigation host**: `UnifiedNavHost` in `core/ui/navigation`
- **Route definitions**: Sealed class `UnifiedRoute` for type-safe navigation

## Testing Strategy

- **Unit tests**: Domain layer use cases (pure business logic)
- **Integration tests**: Repository implementations with mock data sources
- **UI tests**: Compose screens with test ViewModels
- **E2E tests**: Full application flows in testing suite

## Future Enhancements

1. **Hilt Integration**: Replace manual DI with Hilt for better compile-time safety
2. **Use Case Extraction**: Extract business logic from ViewModels into domain use cases
3. **Repository Pattern**: Complete repository implementation for all data sources
4. **Modularization**: Consider multi-module approach for better build performance
5. **Offline Support**: Add local database with Room for offline functionality

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
