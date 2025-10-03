# Ripple Crash Fix: Before and After Examples

This document shows real-world examples of how to fix ripple animation crashes in various scenarios.

## Example 1: IconButton with Activity Finish

### Before (Crash-prone)
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
override fun Content(viewModel: DevicePairingViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Pairing") },
                navigationIcon = {
                    IconButton(onClick = { finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Content
    }
}
```

### After (Fixed)
```kotlin
import mpdc4gsr.core.ui.deferAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
override fun Content(viewModel: DevicePairingViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Pairing") },
                navigationIcon = {
                    IconButton(onClick = deferAction { finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Content
    }
}
```

**Key Change**: Added `deferAction` wrapper to delay finish() by one frame.

---

## Example 2: Dialog Buttons

### Before (Crash-prone)
```kotlin
@Composable
private fun GuideStep3Content(onNext: () -> Unit) {
    Column {
        Text("Step 3: Ready to Go!")
        
        Button(
            onClick = onNext,  // onNext dismisses dialog
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I Know")
        }
    }
}
```

### After (Fixed)
```kotlin
@Composable
private fun GuideStep3Content(onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    Column {
        Text("Step 3: Ready to Go!")
        
        Button(
            onClick = {
                scope.launch {
                    withFrameNanos { }
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Text("I Know")
        }
    }
}
```

**Key Changes**: 
1. Added `rememberCoroutineScope` for launching coroutine
2. Wrapped action in `launch { withFrameNanos { } }`
3. Added explicit `interactionSource` for better control

---

## Example 3: Navigation Card in List

### Before (Crash-prone)
```kotlin
@Composable
private fun LauncherCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,  // Immediately starts new activity
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null)
            Column {
                Text(title)
                Text(subtitle)
            }
        }
    }
}

// Usage
LauncherCard(
    title = "Camera Dashboard",
    onClick = { 
        startActivity(Intent(this, CameraDashboardActivity::class.java))
    }
)
```

### After (Fixed)
```kotlin
import mpdc4gsr.core.ui.deferAction

@Composable
private fun LauncherCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = deferAction(onClick),  // Defers onClick by one frame
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null)
            Column {
                Text(title)
                Text(subtitle)
            }
        }
    }
}

// Usage - same as before
LauncherCard(
    title = "Camera Dashboard",
    onClick = { 
        startActivity(Intent(this, CameraDashboardActivity::class.java))
    }
)
```

**Key Change**: Wrapped onClick with `deferAction` in the Card component.

---

## Example 4: Multiple Navigation IconButtons

### Before (Crash-prone)
```kotlin
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    )
}
```

### After (Fixed)
```kotlin
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                withFrameNanos { }
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                withFrameNanos { }
                                onNavigateToSettings()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    )
}
```

**Key Change**: Each IconButton gets its own scope and deferred execution inline.

---

## Example 5: Custom Clickable Modifier

### Before (Crash-prone)
```kotlin
@Composable
private fun CameraModeItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }  // Direct navigation
                .padding(12.dp)
        ) {
            Text(title)
        }
    }
}
```

### After (Fixed - Option 1: Using safeClickableDeferred)
```kotlin
import mpdc4gsr.core.ui.safeClickableDeferred

@Composable
private fun CameraModeItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .safeClickableDeferred { onClick() }
                .padding(12.dp)
        ) {
            Text(title)
        }
    }
}
```

### After (Fixed - Option 2: Using Card onClick with deferAction)
```kotlin
import mpdc4gsr.core.ui.deferAction

@Composable
private fun CameraModeItem(
    title: String,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    Card(
        onClick = {
            scope.launch {
                withFrameNanos { }
                onClick()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text(title)
        }
    }
}
```

**Key Change**: Either use `safeClickableDeferred` modifier or move click to Card level with deferred execution.

---

## Example 6: No Ripple Option (For Immediate Actions)

### When to Use
Use when you don't want ripple visual feedback and need immediate action execution.

### Before
```kotlin
Box(
    modifier = Modifier.clickable { performImmediateAction() }
)
```

### After
```kotlin
import mpdc4gsr.core.ui.safeClickableNoRipple

Box(
    modifier = Modifier.safeClickableNoRipple { performImmediateAction() }
)
```

---

## Decision Matrix

| Scenario | Solution | Import |
|----------|----------|--------|
| IconButton with finish() | `deferAction` | `mpdc4gsr.core.ui.deferAction` |
| Dialog dismiss button | `deferAction` or inline defer | See examples |
| Card navigation | `deferAction` | `mpdc4gsr.core.ui.deferAction` |
| Custom clickable navigation | `safeClickableDeferred` | `mpdc4gsr.core.ui.safeClickableDeferred` |
| List item with navigation | `safeClickable` | `mpdc4gsr.core.ui.safeClickable` |
| No ripple needed | `safeClickableNoRipple` | `mpdc4gsr.core.ui.safeClickableNoRipple` |

---

## Testing Your Fixes

After applying fixes, test:

1. **Rapid Tap Test**: Tap the button 5-10 times rapidly
2. **During Load**: Tap while app is loading data
3. **Back Button**: System back during ripple animation
4. **Rotation**: Rotate device during ripple
5. **Low Memory**: Test when system is under memory pressure

---

## Common Mistakes to Avoid

### ❌ Don't do this:
```kotlin
// Creating new scope on every recomposition
IconButton(onClick = {
    CoroutineScope(Dispatchers.Main).launch {
        withFrameNanos { }
        action()
    }
})
```

### ✅ Do this instead:
```kotlin
val scope = rememberCoroutineScope()
IconButton(onClick = {
    scope.launch {
        withFrameNanos { }
        action()
    }
})
```

### ❌ Don't do this:
```kotlin
// Wrapping non-navigation actions
Button(onClick = deferAction { updateState() })  // Unnecessary
```

### ✅ Do this instead:
```kotlin
// Only defer navigation/dismissal actions
Button(onClick = { updateState() })  // No defer needed
Button(onClick = deferAction { finish() })  // Defer navigation
```

---

## Summary

The key principle: **Defer any action that causes view detachment by one frame.**

This gives the ripple animation time to settle before the view is removed from the window hierarchy, preventing the crash.
