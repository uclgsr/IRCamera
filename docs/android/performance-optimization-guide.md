# Performance Optimization Guide for IRCamera

## Overview

This guide provides actionable strategies for optimizing IRCamera performance across key metrics: cold start time, rendering smoothness, memory usage, and battery efficiency.

## Cold Start Optimization

**Target:** < 2 seconds to first frame

### 1. Application Initialization

**Current Issues:**
- Multiple SDK initializations in `onCreate()`
- Synchronous initialization blocks startup
- Heavy operations on main thread

**Optimizations:**

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Measure cold start time
        PerformanceMetrics.initialize()
        
        // Critical initialization only
        initializeCriticalComponents()
        
        // Defer non-critical initialization
        lifecycleScope.launch(Dispatchers.Default) {
            initializeNonCriticalComponents()
        }
    }
    
    private fun initializeCriticalComponents() {
        // Must complete before first activity
        ContextProvider.init(this)
        initializeAppLogger()
        setupGlobalExceptionHandler()
    }
    
    private suspend fun initializeNonCriticalComponents() {
        // Can be deferred
        withContext(Dispatchers.IO) {
            initLog()
            initReceiver()
            initLms()
            initUM()
            initJPush()
        }
    }
}
```

**Checklist:**
- [ ] Identify critical vs non-critical initialization
- [ ] Move non-critical work to background threads
- [ ] Use lazy initialization where possible
- [ ] Defer SDK initialization until needed

### 2. Lazy Initialization

```kotlin
class SessionManager {
    // Lazy initialization - only created when first accessed
    private val database by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "ircamera.db")
            .build()
    }
    
    // Use only when needed
    fun getSession(id: String): Session? {
        return database.sessionDao().getById(id)
    }
}
```

### 3. Content Provider Initialization

Move non-critical ContentProvider initialization to lazy loading:

```kotlin
// In AndroidManifest.xml
<provider
    android:name=".MyContentProvider"
    android:authorities="com.csl.ircamera.provider"
    android:enabled="true"
    android:exported="false" />

// In ContentProvider
override fun onCreate(): Boolean {
    // Defer heavy initialization
    initializeAsync()
    return true
}

private fun initializeAsync() {
    CoroutineScope(Dispatchers.IO).launch {
        // Heavy initialization here
    }
}
```

### 4. Baseline Profiles

Generate baseline profiles for Android 12+ to pre-compile critical code paths:

```gradle
// Add to app/build.gradle.kts
plugins {
    id("androidx.baselineprofile")
}

dependencies {
    baselineProfile(project(":benchmark"))
}
```

Create macrobenchmark module to generate profiles:

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.csl.irCamera",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

**Status:** Not implemented (MEDIUM PRIORITY)

## Rendering Performance

**Target:** 60 FPS (16.67ms per frame), 90/120 FPS on capable devices

### 1. Compose Recomposition Optimization

**Problem:** Unnecessary recompositions cause jank

**Solutions:**

```kotlin
// WRONG - recomposes entire screen on every state change
@Composable
fun RecordingScreen(viewModel: RecordingViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column {
        Header(state.title)
        RecordingIndicator(state.isRecording)
        Timer(state.elapsedTime)  // Recomposes every second!
    }
}

// CORRECT - isolate frequently changing state
@Composable
fun RecordingScreen(viewModel: RecordingViewModel) {
    val title by remember { viewModel.title }
    val isRecording by viewModel.isRecording.collectAsState()
    
    Column {
        Header(title)  // Stable, won't recompose
        RecordingIndicator(isRecording)  // Only recomposes when recording state changes
        TimerDisplay(viewModel.timerFlow)  // Isolated recomposition
    }
}

@Composable
fun TimerDisplay(timerFlow: StateFlow<Long>) {
    val time by timerFlow.collectAsState()
    Text(formatTime(time))  // Only this recomposes
}
```

**Best Practices:**
- Use `remember` for stable values
- Split large composables into smaller ones
- Use `derivedStateOf` for computed values
- Mark stable data classes with `@Immutable` or `@Stable`

### 2. List Optimization

```kotlin
// Use keys for LazyColumn items
LazyColumn {
    items(
        items = recordings,
        key = { recording -> recording.id }  // Important for recomposition
    ) { recording ->
        RecordingItem(recording)
    }
}
```

### 3. Image Loading

```kotlin
// Use Coil for efficient image loading
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .size(Size.ORIGINAL)
        .build(),
    contentDescription = "Recording thumbnail",
    modifier = Modifier.size(100.dp)
)
```

### 4. Frame Metrics Monitoring

Track rendering performance in production:

```kotlin
class MainActivity : ComponentActivity() {
    private val frameMetricsListener = FrameMetricsAggregator()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsListener.add(this)
        }
    }
    
    override fun onStop() {
        super.onStop()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = frameMetricsListener.getMetrics()
            val slowFrames = metrics?.count { it > 16 } ?: 0
            val totalFrames = metrics?.size ?: 0
            
            val jankyPercentage = if (totalFrames > 0) {
                (slowFrames.toFloat() / totalFrames) * 100
            } else 0f
            
            PerformanceMetrics.logMetric("janky_frame_percentage", jankyPercentage.toLong())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsListener.remove(this)
        }
    }
}
```

## Memory Optimization

**Target:** Low GC churn, no memory leaks, efficient bitmap handling

### 1. Memory Leak Prevention

**Critical Issues to Fix:**
- GlobalScope usage (see android-code-quality-guide.md)
- Context leaks in ViewModels
- Unclosed resources

**Solutions:**

```kotlin
// WRONG - leaks Activity context
class MyViewModel(private val context: Context) : ViewModel() {
    // Activity reference prevents GC
}

// CORRECT - use Application context
class MyViewModel(private val appContext: Context) : ViewModel() {
    init {
        require(appContext is Application) {
            "Must pass Application context, not Activity context"
        }
    }
}

// BETTER - inject dependencies instead of context
class MyViewModel(
    private val repository: RecordingRepository,
    private val logger: AppLogger
) : ViewModel()
```

### 2. Bitmap Optimization

```kotlin
// Decode bitmap with proper sampling
fun decodeSampledBitmap(
    file: File,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, this)
        
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        
        BitmapFactory.decodeFile(file.absolutePath, this)
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        
        while (halfHeight / inSampleSize >= reqHeight &&
               halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    
    return inSampleSize
}
```

### 3. Resource Cleanup

```kotlin
class RecordingService : Service() {
    private val executorService = Executors.newSingleThreadExecutor()
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}
```

### 4. LeakCanary Integration

Add LeakCanary for debug builds to detect memory leaks:

```gradle
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

## Battery Optimization

**Target:** < 5% per hour during active recording

### 1. Wakelocks

Use wakelocks judiciously:

```kotlin
class RecordingService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    
    override fun onCreate() {
        super.onCreate()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IRCamera::RecordingWakeLock"
        )
    }
    
    private fun startRecording() {
        // Acquire only when recording
        wakeLock.acquire(10 * 60 * 1000L /* 10 minutes timeout */)
    }
    
    private fun stopRecording() {
        // Release immediately when done
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
```

### 2. Location Updates

Request location updates efficiently:

```kotlin
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
    10000 // 10 seconds
).apply {
    setMinUpdateIntervalMillis(5000)
    setMaxUpdateDelayMillis(20000)
}.build()
```

### 3. Bluetooth Scanning

Scan for Bluetooth devices efficiently:

```kotlin
class BluetoothScanner(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    
    fun startScan(callback: ScanCallback) {
        if (isScanning) return
        
        isScanning = true
        bluetoothLeScanner.startScan(callback)
        
        // Auto-stop after 10 seconds
        handler.postDelayed({
            stopScan(callback)
        }, 10_000)
    }
    
    fun stopScan(callback: ScanCallback) {
        if (!isScanning) return
        
        bluetoothLeScanner.stopScan(callback)
        isScanning = false
        handler.removeCallbacksAndMessages(null)
    }
}
```

### 4. Network Requests

Batch network requests and use appropriate timeouts:

```kotlin
class NetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Batch multiple requests
    suspend fun uploadSessionData(sessionId: String) {
        val files = getSessionFiles(sessionId)
        
        // Upload in batches, not one by one
        files.chunked(5).forEach { batch ->
            uploadBatch(batch)
        }
    }
}
```

## I/O Optimization

**Target:** No I/O on main thread, fast database operations

### 1. Database Optimization

```kotlin
@Database(entities = [Session::class, Recording::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ircamera.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Create indices for performance
                        db.execSQL("CREATE INDEX idx_session_timestamp ON sessions(timestamp)")
                    }
                })
                .build()
        }
    }
}

@Dao
interface SessionDao {
    // Use suspend for coroutines
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): Session?
    
    // Use Flow for reactive updates
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Session>>
    
    // Batch operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<Session>)
}
```

### 2. File Operations

```kotlin
// Always use coroutines for file I/O
suspend fun saveRecording(file: File, data: ByteArray) {
    withContext(Dispatchers.IO) {
        file.writeBytes(data)
    }
}

// Use buffered streams for large files
suspend fun copyLargeFile(source: File, dest: File) {
    withContext(Dispatchers.IO) {
        source.inputStream().buffered().use { input ->
            dest.outputStream().buffered().use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
    }
}
```

## Profiling Tools

### 1. Android Profiler

Use Android Studio Profiler to identify bottlenecks:
- CPU Profiler: Find slow methods
- Memory Profiler: Detect leaks and high allocations
- Network Profiler: Monitor network activity
- Energy Profiler: Track battery usage

### 2. Systrace

Capture system trace for deep analysis:

```bash
adb shell atrace --async_start gfx input view webview wm am sm audio video camera hal res dalvik rs bionic power pm ss database network adb
adb shell atrace --async_stop > trace.html
```

### 3. Method Tracing

Add method tracing for specific sections:

```kotlin
fun criticalOperation() {
    Trace.beginSection("CriticalOperation")
    try {
        // Your code here
    } finally {
        Trace.endSection()
    }
}
```

## Optimization Checklist

### Cold Start
- [ ] Defer non-critical initialization
- [ ] Use lazy initialization
- [ ] Generate baseline profiles
- [ ] Optimize Application.onCreate()
- [ ] Measure and log cold start time

### Rendering
- [ ] Optimize Compose recompositions
- [ ] Use proper keys in LazyColumn
- [ ] Monitor frame metrics
- [ ] Profile with GPU rendering tools
- [ ] Target 60 FPS minimum

### Memory
- [ ] Fix GlobalScope usages
- [ ] Prevent context leaks
- [ ] Optimize bitmap loading
- [ ] Clean up resources properly
- [ ] Use LeakCanary in debug

### Battery
- [ ] Minimize wakelock usage
- [ ] Efficient location updates
- [ ] Batch Bluetooth scans
- [ ] Batch network requests
- [ ] Monitor battery impact

### I/O
- [ ] All I/O on background threads
- [ ] Optimize database queries
- [ ] Use indices for common queries
- [ ] Buffered file operations
- [ ] Enforce with StrictMode in debug

## Performance Testing

Create performance benchmarks:

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmark {
    @Test
    fun benchmarkDatabaseQuery() {
        val startTime = System.nanoTime()
        
        repeat(1000) {
            database.sessionDao().getById("test-id")
        }
        
        val duration = (System.nanoTime() - startTime) / 1_000_000
        assertTrue("Database query too slow: ${duration}ms", duration < 1000)
    }
}
```

## Continuous Monitoring

Set up monitoring in production:

1. **Firebase Performance Monitoring**
   - Cold start time
   - Screen rendering time
   - Network request duration

2. **Custom Metrics**
   - Recording success rate
   - Frame drop rate
   - Memory usage during recording

3. **Alerts**
   - Cold start > 3 seconds
   - Crash rate > 1%
   - Jank rate > 10%

## Resources

- [Android Performance Documentation](https://developer.android.com/topic/performance)
- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Android Profiler Guide](https://developer.android.com/studio/profile)
- [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles)

---

**Last Updated:** 2024
**Status:** Living document - updated as new optimizations are implemented
