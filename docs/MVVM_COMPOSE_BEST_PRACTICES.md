# MVVM + Compose Architecture Best Practices

This guide outlines best practices for implementing MVVM architecture with Jetpack Compose in this project.

## Architecture Overview

### Unidirectional Data Flow (UDF)

* `ViewModel` exposes `StateFlow<UiState>` - single source of truth
* UI sends actions back via function calls
* State flows down, events flow up

### State Management

#### ViewModel State Updates

ALWAYS use `StateFlow.update()` for thread-safe state mutations:

```kotlin
// Good - thread-safe
_uiState.update { it.copy(isLoading = true) }

// Bad - not thread-safe, race conditions possible
_uiState.value = _uiState.value.copy(isLoading = true)
```

#### Lifecycle-Aware Collection

In Composables, use `collectAsStateWithLifecycle()`:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

This ensures proper lifecycle handling and prevents memory leaks.

### ViewModel Pattern

#### UiState and UiAction

```kotlin
data class UiState(
  val items: List<Post> = emptyList(),
  val loading: Boolean = false,
  val error: String? = null
)

sealed interface UiAction {
  data object Refresh : UiAction
  data class Open(val id: Long) : UiAction
}
```

#### ViewModel Implementation

```kotlin
class FeedViewModel @Inject constructor(
  private val repo: PostRepository,
  private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {
  private val _state = MutableStateFlow(UiState(loading = true))
  val state: StateFlow<UiState> = _state.asStateFlow()

  fun onAction(a: UiAction) = when (a) {
    UiAction.Refresh -> refresh()
    is UiAction.Open -> openPost(a.id)
  }

  private fun refresh() = viewModelScope.launch {
    _state.update { it.copy(loading = true, error = null) }
    runCatching { repo.fetch() }
      .onSuccess { posts -> _state.update { it.copy(items = posts, loading = false) } }
      .onFailure { e -> _state.update { it.copy(error = e.message, loading = false) } }
  }
}
```

### Composable Pattern

#### Route Composable

```kotlin
@Composable
fun FeedRoute(
  vm: FeedViewModel = hiltViewModel(),
  onOpen: (Long) -> Unit
) {
  val ui by vm.state.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }

  FeedScreen(
    ui = ui,
    snackbarHostState = snackbar,
    onRefresh = { vm.onAction(UiAction.Refresh) },
    onOpen = onOpen
  )
}
```

#### Screen Composable

```kotlin
@Composable
fun FeedScreen(
  ui: UiState,
  snackbarHostState: SnackbarHostState,
  onRefresh: () -> Unit,
  onOpen: (Long) -> Unit
) {
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    if (ui.loading) CircularProgressIndicator()
    else if (ui.error != null) Text(ui.error)
    else LazyColumn(contentPadding = padding) {
      items(ui.items, key = { it.id }) { post ->
        PostRow(post, onClick = { onOpen(post.id) })
      }
    }
  }
}
```

## Side Effects

### LaunchedEffect

Use for suspending work triggered by key changes:

```kotlin
LaunchedEffect(userId) {
  viewModel.loadUser(userId)
}
```

### DisposableEffect

Use for register/cleanup lifecycle:

```kotlin
DisposableEffect(Unit) {
  val listener = ...
  registerListener(listener)
  onDispose {
    unregisterListener(listener)
  }
}
```

### snapshotFlow

Convert Compose State to Flow:

```kotlin
LaunchedEffect(Unit) {
  snapshotFlow { scrollState.value }
    .collect { viewModel.updateScrollPosition(it) }
}
```

## State Hoisting

Hoist state to the lowest common owner:

* Pass state down as parameters
* Pass events up as lambdas
* Keep Composables stateless when possible
* Use `remember` for ephemeral UI state
* Use `rememberSaveable` for config/process death survival

## Performance

### Lazy Lists

* Provide stable `key` parameters
* Use immutable data models
* Keep heavy work out of composition

```kotlin
LazyColumn {
  items(
    items = list,
    key = { it.id }  // Stable key
  ) { item ->
    ItemRow(item)
  }
}
```

### Modifier Order

Modifier order affects layout and draw performance:

```kotlin
// Good - padding before size
Modifier.padding(16.dp).size(48.dp)

// Different behavior
Modifier.size(48.dp).padding(16.dp)
```

## Testing

### ViewModel Tests

Test ViewModels in isolation:

```kotlin
@Test
fun `refresh updates state correctly`() = runTest {
  val viewModel = FeedViewModel(fakeRepo)
  viewModel.onAction(UiAction.Refresh)
  
  assertEquals(false, viewModel.state.value.loading)
  assertTrue(viewModel.state.value.items.isNotEmpty())
}
```

### UI Tests

Test via semantics tree:

```kotlin
@Test
fun feedScreen_displaysItems() {
  composeTestRule.setContent {
    FeedScreen(
      ui = UiState(items = testItems),
      onRefresh = {},
      onOpen = {}
    )
  }
  
  composeTestRule.onNodeWithText("Test Item").assertExists()
}
```

## Accessibility

* Use `semantics {}` modifier for custom semantics
* Provide `contentDescription` for images
* Support focus order and keyboard navigation
* Respect text scaling preferences
* Support RTL layouts
* Test with TalkBack enabled

## References

* [Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations)
* [State Hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
* [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
* [Side Effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
* [Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation)
* [Performance Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
