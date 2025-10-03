# ANR Prevention Guide

## Overview

This document provides guidelines for preventing Application Not Responding (ANR) errors in the IR Camera application.

## What is an ANR?

An ANR occurs when the Android UI thread is blocked for more than 5 seconds (for input events) or 10 seconds (for BroadcastReceivers). When this happens, Android displays an "Application Not Responding" dialog to the user.

## Common Causes of ANR

1. **Blocking I/O Operations on Main Thread**
   - File read/write operations
   - Network requests
   - Database queries

2. **Heavy Computations**
   - Image processing
   - Data transformation
   - Complex calculations

3. **Synchronization Issues**
   - Deadlocks
   - Long-held locks
   - Wait/notify misuse

4. **UI Thread Overload**
   - Too many UI updates
   - Complex layouts
   - Inefficient recomposition

## Fixed Issues

### 1. CameraPerformanceManager Thread Blocking

**Problem**: Frame processing was happening synchronously on the main thread, causing 10+ second blocks.

**Solution**: 
```kotlin
// Background executor for frame processing
private val frameProcessingExecutor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "CameraFrameProcessor").apply {
        priority = Thread.NORM_PRIORITY
    }
}

// Process frames asynchronously
frameProcessingExecutor.execute {
    processNextFrame()
}
```

**Files Changed**:
- `app/src/main/java/mpdc4gsr/feature/camera/data/CameraPerformanceManager.kt`

### 2. Ripple Animation on Detached Views

**Problem**: Material Design ripple animations were trying to start on views that had been detached from the window, causing crashes after ANR recovery.

**Solution**: Created safe composable modifiers that properly manage interaction sources and ripple lifecycle:

```kotlin
@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick
    )
}
```

**Files Added**:
- `app/src/main/java/mpdc4gsr/core/ui/SafeRippleModifier.kt`

## Best Practices

### 1. Use Coroutines for Async Operations

Always use coroutines with appropriate dispatchers:

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    // I/O operations
    val data = loadFromDatabase()
    
    withContext(Dispatchers.Main) {
        // Update UI
        updateUI(data)
    }
}
```

### 2. Use SafeMainThreadHandler for Monitoring

Use the `SafeMainThreadHandler` to detect slow operations:

```kotlin
val safeHandler = SafeMainThreadHandler("MyComponent")
safeHandler.post {
    // UI operation - will log warning if > 100ms
}
```

**Files Added**:
- `app/src/main/java/mpdc4gsr/core/ui/SafeMainThreadHandler.kt`

### 3. Move Heavy Work to Background Threads

For non-UI operations, always use background threads:

```kotlin
// Good
val executor = Executors.newSingleThreadExecutor()
executor.execute {
    performHeavyComputation()
}

// Also good with coroutines
CoroutineScope(Dispatchers.Default).launch {
    performHeavyComputation()
}
```

### 4. Optimize Compose Recomposition

Use `remember` and derivedStateOf to avoid unnecessary recomposition:

```kotlin
@Composable
fun MyScreen() {
    val expensiveValue = remember(input) {
        computeExpensiveValue(input)
    }
    
    val derivedState = remember(state1, state2) {
        derivedStateOf { 
            state1.value + state2.value 
        }
    }
}
```

### 5. Check View Attachment Before Animations

The View extension functions already include safety checks:

```kotlin
fun View?.visibleAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.VISIBLE
        return
    }
    // Safe to animate
    this.startAnimation(...)
}
```

**Files Already Fixed**:
- `libunified/src/main/java/com/mpdc4gsr/libunified/ir/extension/View.kt`

## Monitoring and Debugging

### 1. Android Studio Profiler

Use the CPU Profiler to identify main thread bottlenecks:
- Record a method trace
- Look for long-running methods in red/orange
- Focus on the "Main" thread

### 2. StrictMode

Enable StrictMode in debug builds to catch violations early:

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
}
```

### 3. SafeMainThreadHandler Statistics

Check handler statistics periodically:

```kotlin
val stats = SafeMainThreadHandler.getStatistics()
Log.d(TAG, "Slow operations: ${stats.slowOperationRate}%")
Log.d(TAG, "Critical operations: ${stats.criticalOperationRate}%")
```

## Testing for ANR Prevention

1. **Profile with realistic data**: Test with large datasets and slow network conditions
2. **Stress test**: Rapidly navigate between screens while loading data
3. **Monitor logs**: Check for warnings from SafeMainThreadHandler
4. **Use ANR tracking**: Enable ANR tracking in Firebase or similar tools

## Checklist for New Code

Before committing UI-related code, ensure:

- [ ] No blocking I/O on main thread
- [ ] Heavy computations use background threads/coroutines
- [ ] Database queries use Room with coroutines or LiveData
- [ ] Network requests use Retrofit with suspend functions
- [ ] UI updates happen on main thread via proper dispatchers
- [ ] Click handlers don't perform long operations
- [ ] Animations check view attachment status

## Additional Resources

- [Android Developer Guide - ANR](https://developer.android.com/topic/performance/vitals/anr)
- [Kotlin Coroutines Best Practices](https://kotlinlang.org/docs/coroutines-best-practices.html)
- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
