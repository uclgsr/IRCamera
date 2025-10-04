# Quick Fix Guide - Anti-Patterns and Leaks

This is a quick reference guide for fixing the most common issues found in the IRCamera repository.

---

## 🔴 CRITICAL: GlobalScope Leaks

### ❌ WRONG
```kotlin
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
GlobalScope.launch {
    while (isActive.get()) {
        doWork()
    }
}
```

### ✅ CORRECT
```kotlin
// Option 1: Use ViewModel scope
viewModelScope.launch {
    while (isActive) {
        doWork()
    }
}

// Option 2: Use lifecycle scope
lifecycleScope.launch {
    doWork()
}

// Option 3: Inject custom scope
class MyService(private val serviceScope: CoroutineScope) {
    fun start() {
        serviceScope.launch {
            doWork()
        }
    }
    
    fun stop() {
        serviceScope.cancel()
    }
}
```

**Files to Fix:**
- `FileUploadService.kt:279`
- `WebSocketClient.kt:1024`
- `EnhancedThermalRecorder.kt:46,74,123`
- `SessionManager.kt:135,307`

---

## 🔴 CRITICAL: runBlocking in Lifecycle Methods

### ❌ WRONG
```kotlin
override fun onCleared() {
    super.onCleared()
    runBlocking {
        cleanup()
    }
}

fun release() {
    if (isRecording) {
        runBlocking { stopRecording() }
    }
}
```

### ✅ CORRECT
```kotlin
override fun onCleared() {
    super.onCleared()
    // Just cancel the scope - non-blocking
    viewModelScope.cancel()
    
    // Resources will clean up on their own
    // Or if you need explicit cleanup:
    viewModelScope.launch {
        cleanup()
    }
}

fun release() {
    if (isRecording) {
        // Launch async - don't block
        CoroutineScope(Dispatchers.IO).launch {
            stopRecording()
        }
    }
    // Release synchronous resources
    cameraController.close()
}
```

**Files to Fix:**
- `ThermalCameraViewModel.kt:182`
- `Camera2System.kt:192`
- `CrashSafeSupervisor.kt:429`

---

## 🔴 CRITICAL: ExecutorService Leaks

### ❌ WRONG
```kotlin
class RgbCameraRecorder {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // No shutdown method!
}
```

### ✅ CORRECT
```kotlin
class RgbCameraRecorder {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    fun cleanup() {
        // Graceful shutdown
        cameraExecutor.shutdown()
        try {
            if (!cameraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                // Force shutdown if needed
                cameraExecutor.shutdownNow()
                
                // Wait again for tasks to respond to cancellation
                if (!cameraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    AppLogger.e(TAG, "Executor did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            cameraExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

// Better: Use coroutines instead
class RgbCameraRecorder {
    private val cameraScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun cleanup() {
        cameraScope.cancel()
    }
}
```

**Files to Fix:**
- `RgbCameraRecorder.kt:142`
- `EasyBLEBuilder.java:11`
- `PosterDispatcher.java:14`

---

## 🟠 HIGH: Socket Resource Leaks

### ❌ WRONG
```kotlin
val socket = Socket()
socket.connect(InetSocketAddress(host, port), 2000)
val output = DataOutputStream(socket.getOutputStream())
val input = DataInputStream(socket.getInputStream())
// If exception occurs, socket never closed!
processData(input, output)
```

### ✅ CORRECT
```kotlin
// Use 'use' extension for automatic cleanup
Socket().use { socket ->
    socket.connect(InetSocketAddress(host, port), 2000)
    DataOutputStream(socket.getOutputStream()).use { output ->
        DataInputStream(socket.getInputStream()).use { input ->
            processData(input, output)
            // Automatically closed even on exception
        }
    }
}

// Or with finally
var socket: Socket? = null
var output: DataOutputStream? = null
var input: DataInputStream? = null
try {
    socket = Socket()
    socket.connect(InetSocketAddress(host, port), 2000)
    output = DataOutputStream(socket.getOutputStream())
    input = DataInputStream(socket.getInputStream())
    processData(input, output)
} finally {
    input?.close()
    output?.close()
    socket?.close()
}
```

**Files to Fix:**
- `NetworkClient.kt:807-811` and other socket usage

---

## 🟠 HIGH: Context Leaks

### ❌ WRONG
```kotlin
class MyViewModel : ViewModel() {
    private var context: Context? = null
    
    fun setContext(ctx: Context) {
        context = ctx // Activity context leaks!
    }
}
```

### ✅ CORRECT
```kotlin
// Use AndroidViewModel for Application context
class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    
    // Or pass context as method parameter
    fun doWork(context: Context) {
        // Use context only within method scope
    }
}
```

**Files to Fix:**
- `DualModeCameraViewModel.kt:42`

---

## 🟠 HIGH: Callback Leaks

### ❌ WRONG
```kotlin
class NetworkClient {
    private var eventListener: NetworkEventListener? = null
    
    fun setEventListener(listener: NetworkEventListener) {
        eventListener = listener
        // Never cleared!
    }
}
```

### ✅ CORRECT
```kotlin
class NetworkClient {
    private var eventListener: NetworkEventListener? = null
    
    fun setEventListener(listener: NetworkEventListener) {
        eventListener = listener
    }
    
    fun clearEventListener() {
        eventListener = null
    }
    
    fun cleanup() {
        clearEventListener()
        // Other cleanup
    }
}

// Or use WeakReference
class NetworkClient {
    private var eventListener: WeakReference<NetworkEventListener>? = null
    
    fun setEventListener(listener: NetworkEventListener) {
        eventListener = WeakReference(listener)
    }
    
    private fun notifyEvent(event: String) {
        eventListener?.get()?.onEvent(event)
    }
}
```

**Files to Fix:**
- `NetworkClient.kt:97`
- `TcpClient.kt:42,43`
- `ThermalRecorder.kt:54`
- And 10+ other files

---

## 🟠 HIGH: Thread.sleep Usage

### ❌ WRONG
```kotlin
fun doWork() {
    Thread.sleep(1000) // Blocks thread!
}
```

### ✅ CORRECT
```kotlin
suspend fun doWork() {
    delay(1000) // Suspends coroutine, doesn't block
}

// Or in regular function
fun doWork() {
    viewModelScope.launch {
        delay(1000)
        continueWork()
    }
}
```

**Files to Fix:**
- `AdaptiveThermalStreamer.kt:254`
- `CameraPerformanceManager.kt:271`
- `StructuredLogger.kt:245`

---

## 🟠 HIGH: Bare Exception Catching

### ❌ WRONG
```python
try:
    socket.close()
except:
    pass  # Silently ignores ALL exceptions
```

```java
try {
    method.invoke(owner, params);
} catch (Exception ignore) {
    // Silently ignored
}
```

### ✅ CORRECT
```python
try:
    socket.close()
except (OSError, socket.error) as e:
    logger.error(f"Error closing socket: {e}")
except Exception as e:
    logger.error(f"Unexpected error: {e}", exc_info=True)
```

```java
try {
    method.invoke(owner, params);
} catch (IllegalAccessException | InvocationTargetException e) {
    Log.e(TAG, "Failed to invoke method: " + method.getName(), e);
}
```

**Files to Fix:**
- `PosterDispatcher.java:113`
- Python PC controller files

---

## 🟡 MEDIUM: lateinit Without Checks

### ❌ WRONG
```kotlin
class MyClass {
    private lateinit var recorder: Recorder
    
    fun doWork() {
        recorder.start() // Crashes if not initialized!
    }
}
```

### ✅ CORRECT
```kotlin
class MyClass {
    private lateinit var recorder: Recorder
    
    fun doWork() {
        if (::recorder.isInitialized) {
            recorder.start()
        } else {
            AppLogger.e(TAG, "Recorder not initialized")
        }
    }
}

// Or use lazy initialization
class MyClass {
    private val recorder by lazy { 
        Recorder() 
    }
}

// Or use nullable
class MyClass {
    private var recorder: Recorder? = null
    
    fun doWork() {
        recorder?.start()
    }
}
```

---

## 🟡 MEDIUM: Hardcoded Colors

### ❌ WRONG
```kotlin
@Composable
fun MyScreen() {
    Box(
        modifier = Modifier.background(Color(0xFF16131E))
    )
}
```

### ✅ CORRECT
```kotlin
// Define in theme
object AppColors {
    val Background = Color(0xFF16131E)
    val Primary = Color(0xFF1E3A8A)
}

@Composable
fun MyScreen() {
    Box(
        modifier = Modifier.background(AppColors.Background)
    )
}

// Or use MaterialTheme
@Composable
fun MyScreen() {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )
}
```

---

## 🟡 MEDIUM: Magic Numbers

### ❌ WRONG
```kotlin
socket.connect(InetSocketAddress(host, port), 2000)
withTimeout(10000) {
    doWork()
}
if (memoryRatio > 0.9f) {
    // ...
}
```

### ✅ CORRECT
```kotlin
companion object {
    private const val CONNECTION_TIMEOUT_MS = 2000
    private const val OPERATION_TIMEOUT_MS = 10000
    private const val MEMORY_CRITICAL_THRESHOLD = 0.9f
}

socket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS)
withTimeout(OPERATION_TIMEOUT_MS) {
    doWork()
}
if (memoryRatio > MEMORY_CRITICAL_THRESHOLD) {
    // ...
}
```

---

## Testing Your Fixes

### 1. Memory Leak Testing

```kotlin
// Add to debug build.gradle
debugImplementation "com.squareup.leakcanary:leakcanary-android:2.12"
```

### 2. Coroutine Testing

```kotlin
@Test
fun testNoMemoryLeak() = runTest {
    val viewModel = MyViewModel()
    val job = viewModel.startWork()
    
    viewModel.onCleared()
    
    // Assert job is cancelled
    assertTrue(job.isCancelled)
}
```

### 3. Resource Testing

```kotlin
@Test
fun testResourcesReleased() {
    val recorder = RgbCameraRecorder(context)
    recorder.initialize()
    
    recorder.cleanup()
    
    // Check executor is shutdown
    assertTrue(recorder.isCleanedUp())
}
```

### 4. Manual Testing

```bash
# Check open file descriptors
adb shell lsof -p $(adb shell pidof com.your.app) | wc -l

# Monitor memory
adb shell dumpsys meminfo com.your.app

# Check threads
adb shell ps -T -p $(adb shell pidof com.your.app) | wc -l
```

---

## Checklist for Code Review

Before submitting code, verify:

- [ ] No GlobalScope usage
- [ ] No runBlocking in lifecycle methods
- [ ] All ExecutorService instances have shutdown()
- [ ] All sockets use .use {} or have finally cleanup
- [ ] No Context stored in ViewModels
- [ ] All callbacks cleared on cleanup
- [ ] No Thread.sleep in hot paths
- [ ] Specific exception types (not bare catch)
- [ ] No magic numbers
- [ ] Resources have cleanup methods
- [ ] Tests verify no leaks
- [ ] LeakCanary shows no leaks

---

## Quick Commands

```bash
# Find GlobalScope usage
grep -r "GlobalScope" --include="*.kt" app/src/main

# Find runBlocking
grep -r "runBlocking" --include="*.kt" app/src/main

# Find Thread.sleep
grep -r "Thread.sleep" --include="*.kt" app/src/main

# Find ExecutorService without shutdown
grep -r "ExecutorService" --include="*.kt" --include="*.java" app/src/main | grep -v shutdown

# Find lateinit vars
grep -r "lateinit var" --include="*.kt" app/src/main

# Find bare except (Python)
grep -r "except:" --include="*.py" pc-controller/
```

---

## Priority Order

1. **Week 1:** Fix all GlobalScope, runBlocking, ExecutorService leaks
2. **Week 2:** Fix Context leaks, callback leaks, socket leaks
3. **Week 3:** Replace Thread.sleep, fix exception handling
4. **Week 4:** Polish, testing, documentation

---

## Need Help?

- See [REPOSITORY_ANALYSIS.md](./REPOSITORY_ANALYSIS.md) for detailed analysis
- See [android/code-quality-analysis.md](./android/code-quality-analysis.md) for Android details
- See [pc-controller/docs/code_review.md](../pc-controller/docs/code_review.md) for Python details

---

**Remember:** It's better to fix issues gradually with tests than to rush and introduce new bugs!
