# MVVM Modernization Guide

This document outlines the modernization and consolidation of the MVVM architecture in the IRCamera
project.

## Overview

The MVVM architecture has been modernized to use current Android development best practices,
focusing on:

- StateFlow instead of LiveData for reactive UI
- Repository pattern for data management
- Coroutine-based error handling
- Lifecycle-aware components
- Type-safe state management with sealed classes

## Key Components

### 1. BaseViewModel

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/BaseViewModel.kt`

**Features**:

- StateFlow-based UI state management
- SharedFlow for one-time events
- Built-in error handling with CoroutineExceptionHandler
- Loading state management helpers
- Coroutine scope helpers (`launchWithErrorHandling`, `launchWithLoading`)

**Example Usage**:

```kotlin
class MyViewModel : BaseViewModel() {
    fun performAction() {
        launchWithLoading {
            // Your suspend function here
            repository.fetchData()
        }
    }
}
```

### 2. BaseViewModelActivity

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/BaseViewModelActivity.kt`

**Features**:

- Automatic lifecycle-aware observers using `repeatOnLifecycle`
- Built-in UI state handling (loading, error)
- Automatic ViewModel initialization
- Event handling for common UI actions

**Key Methods**:

- `handleUiState()` - Override to handle custom UI states
- `handleUiEvent()` - Override to handle custom events
- `showLoading()`, `hideLoading()` - Override for custom loading indicators

### 3. BaseViewModelFragment

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/BaseViewModelFragment.kt`

**Features**:

- Same as BaseViewModelActivity but for Fragments
- Uses `viewLifecycleOwner` for proper lifecycle management
- Automatic cleanup on destroy

### 4. ViewModelFactory

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/ViewModelFactory.kt`

**Features**:

- Dependency injection for ViewModels
- Builder pattern for easy setup
- Support for Application context and Repository injection

**Example Usage**:

```kotlin
val factory = BaseViewModelFactory.Builder(application)
    .addRepository<MyRepository>(myRepository)
    .build()

val viewModel = createViewModelWithFactory<MyViewModel>(factory)
```

### 5. BaseRepository

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/repository/BaseRepository.kt`

**Features**:

- Result wrapper for type-safe error handling
- Built-in caching mechanism
- Coroutine-based operations
- Network-bound resource pattern

**Example Usage**:

```kotlin
class MyRepository : BaseRepository() {
    fun getData(): Flow<Result<DataType>> = safeFlow {
        // Your data fetching logic
    }
}
```

## State Management Patterns

### StateFlow vs LiveData

**Old Pattern (LiveData)**:

```kotlin
private val _data = MutableLiveData<DataType>()
val data: LiveData<DataType> = _data

// In Activity/Fragment
viewModel.data.observe(this) { data ->
    // Handle data
}
```

**New Pattern (StateFlow)**:

```kotlin
private val _data = MutableStateFlow<DataType>(initialValue)
val data: StateFlow<DataType> = _data.asStateFlow()

// In Activity/Fragment
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect { data ->
            // Handle data
        }
    }
}
```

### Sealed Classes for Type Safety

**State Classes**:

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: DataType) : UiState()
    data class Error(val message: String) : UiState()
}
```

**Event Classes**:

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class Navigate(val destination: String) : UiEvent()
    object Finish : UiEvent()
}
```

## Migration Examples

### 1. MoreFragmentViewModel

**Before**: Used LiveData with direct value assignments
**After**: Uses StateFlow with proper error handling and event management

**Key Improvements**:

- SharedFlow for one-time navigation events
- Combined state objects for complex UI scenarios
- Proper error handling with coroutines
- Enhanced device state management

### 2. PdfViewModel

**Before**: Basic LiveData with CountDownLatch for async operations
**After**: StateFlow with Repository pattern and proper pagination

**Key Improvements**:

- Repository pattern for data operations
- Pagination state management
- Caching mechanism
- Type-safe error handling
- Event-driven UI updates

### 3. PolicyViewModel

**Before**: Simple ViewModel with direct repository calls
**After**: Complete modern implementation with StateFlow and proper error handling

**Key Improvements**:

- StateFlow-based state management
- Type-safe enum for policy types
- Proper error handling and retry logic
- Caching with Repository pattern
- Event-driven architecture

### 4. MainFragmentViewModel

**Before**: LiveData-based device state management
**After**: StateFlow with combined state objects and reactive updates

**Key Improvements**:

- StateFlow for reactive device state
- Combined state objects for complex UI scenarios
- SharedFlow for navigation events
- Enhanced battery monitoring with low battery warnings
- Type-safe device management

### 5. SensorDataRepository

**New Addition**: Modern repository demonstrating advanced patterns

**Features**:

- Multi-sensor data streams
- Combined data flows
- Real-time data simulation
- Device status management
- Type-safe sensor data models

## Advanced Patterns

### 1. Combined State Objects

For complex UI scenarios, use combined state objects:

```kotlin
data class ScreenState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdated: Long? = null
)
```

### 2. Multi-Stream Data Combination

Combine multiple data streams reactively:

```kotlin
val combinedData = combine(
    dataStream1,
    dataStream2,
    dataStream3
) { data1, data2, data3 ->
    CombinedData(data1, data2, data3)
}
```

### 3. Repository Caching Strategy

Implement intelligent caching based on data type:

```kotlin
// Real-time data - short cache
private const val SENSOR_DATA_TTL = 5 * 1000L

// Configuration data - longer cache
private const val CONFIG_DATA_TTL = 60 * 60 * 1000L

// Session data - no expiration until manually cleared
private const val SESSION_DATA_TTL = Long.MAX_VALUE
```

### 4. Type-Safe Event Handling

Use sealed classes for events with data:

```kotlin
sealed class NavigationEvent {
    data class Navigate(val route: String, val extras: Bundle = Bundle()) : NavigationEvent()
    data class ShowDialog(val title: String, val message: String) : NavigationEvent()
    object GoBack : NavigationEvent()
}
```

### 5. Error Recovery Patterns

Implement retry and fallback mechanisms:

```kotlin
fun retry() {
    val currentState = _state.value
    if (currentState is State.Error) {
        loadData(forceRefresh = true)
    }
}
```

## Best Practices

### 1. State Management

- Use StateFlow for data that represents current state
- Use SharedFlow for one-time events
- Create sealed classes for type-safe state representation
- Always use `asStateFlow()` and `asSharedFlow()` to expose read-only flows

### 2. Error Handling

- Use the built-in `launchWithErrorHandling` for simple operations
- Use `launchWithLoading` for operations that need loading states
- Always provide meaningful error messages
- Handle network connectivity checks

### 3. Repository Pattern

- Extend BaseRepository for data operations
- Use the Result wrapper for type-safe error handling
- Implement caching where appropriate
- Use coroutines for async operations

### 4. Lifecycle Management

- Always use `repeatOnLifecycle` with StateFlow
- Use `viewLifecycleOwner` in Fragments
- Properly handle ViewModel cleanup

### 5. Testing

- StateFlow makes testing easier with immediate value access
- Use TestCoroutineDispatcher for testing coroutines
- Mock repositories for unit testing ViewModels

### 6. Performance Optimization

- Use appropriate cache TTL based on data volatility
- Combine related data streams to reduce observer count
- Use `distinctUntilChanged()` for expensive UI updates
- Implement proper data pagination for large datasets

## Migration Checklist

When modernizing an existing ViewModel:

- [ ] Extend BaseViewModel instead of ViewModel/AndroidViewModel
- [ ] Replace MutableLiveData with MutableStateFlow
- [ ] Create sealed classes for states and events
- [ ] Use SharedFlow for one-time events
- [ ] Implement Repository pattern if needed
- [ ] Add proper error handling with try-catch or safeCall
- [ ] Update Activity/Fragment to use repeatOnLifecycle
- [ ] Add caching if appropriate
- [ ] Create meaningful state classes
- [ ] Add loading states
- [ ] Handle network connectivity
- [ ] Add retry mechanisms
- [ ] Implement proper data validation
- [ ] Add performance optimizations

## Common Pitfalls

1. **Not using repeatOnLifecycle**: This can cause memory leaks and unnecessary processing
2. **Mixing LiveData and StateFlow**: Stick to one pattern for consistency
3. **Not handling loading states**: Always provide user feedback for async operations
4. **Ignoring error handling**: Always handle exceptions properly
5. **Not using sealed classes**: This reduces type safety and makes debugging harder
6. **Excessive state updates**: Use `distinctUntilChanged()` for expensive operations
7. **Poor cache management**: Set appropriate TTL values for different data types
8. **Not handling edge cases**: Consider offline scenarios, low battery, etc.

## Testing Strategies

### 1. ViewModel Testing

```kotlin
@Test
fun `test data loading success`() = runTest {
        // Given
        val mockRepository = mockk<MyRepository>()
        val viewModel = MyViewModel(mockRepository)

        // When
        viewModel.loadData()

        // Then
        assertEquals(LoadingState.Success, viewModel.state.value)
    }
```

### 2. Repository Testing

```kotlin
@Test
fun `test repository caching`() = runTest {
        val repository = MyRepository()

        // First call should fetch from network
        val result1 = repository.getData().first()

        // Second call should use cache
        val result2 = repository.getData().first()

        assertEquals(result1, result2)
    }
```

### 3. Flow Testing

```kotlin
@Test
fun `test combined data flow`() = runTest {
        val repository = MyRepository()

        repository.getCombinedData(listOf("device1", "device2"))
            .test {
                val emission = awaitItem()
                assertTrue(emission is Result.Success)
                assertEquals(2, emission.data.devices.size)
            }
    }
```

## Future Enhancements

1. **Dependency Injection**: Consider using Hilt for more sophisticated DI
2. **Navigation Component**: Integrate with Navigation Component for type-safe navigation
3. **Data Binding**: Update data binding expressions to work with StateFlow
4. **Compose Integration**: Prepare for Jetpack Compose migration
5. **Performance Monitoring**: Add performance monitoring for state changes
6. **Offline Support**: Implement comprehensive offline data management
7. **Real-time Synchronization**: Add real-time data synchronization capabilities
8. **Advanced Analytics**: Integrate analytics for user behavior tracking

## Conclusion

The modernized MVVM architecture provides:

- Better reactive programming with StateFlow
- Improved error handling and user experience
- More maintainable and testable code
- Consistent patterns across the application
- Better separation of concerns with Repository pattern
- Advanced caching and performance optimizations
- Type-safe state management
- Comprehensive error recovery mechanisms

This modernization maintains backward compatibility where possible while providing a clear path
forward for new
development and establishes patterns that scale well with application growth.