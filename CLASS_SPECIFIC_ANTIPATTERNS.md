# Class-Specific Anti-Pattern Analysis

## Overview

This document provides detailed analysis of specific classes exhibiting anti-patterns and code smells. Each entry includes the issue, impact, and specific refactoring recommendations.

---

## 1. ThermalCameraRecorder.kt (3,742 lines)

**Location**: `app/src/main/java/mpdc4gsr/sensors/thermal/ThermalCameraRecorder.kt`

### Anti-Patterns Identified

#### 1.1 God Class
- **Lines**: 3,742 (15x recommended maximum)
- **Methods**: Likely 50+
- **Responsibilities**: Recording, preview, processing, serialization, error handling, calibration

#### 1.2 Multiple Responsibilities
```kotlin
class ThermalCameraRecorder {
    // Responsibility 1: Recording management
    fun startRecording() { }
    fun stopRecording() { }
    
    // Responsibility 2: Frame processing
    fun processFrame() { }
    fun convertTemperature() { }
    
    // Responsibility 3: Preview management
    fun updatePreview() { }
    fun throttlePreview() { }
    
    // Responsibility 4: Data serialization
    fun serializeFrame() { }
    fun writeToCsv() { }
    
    // Responsibility 5: Calibration
    fun calibrate() { }
    fun loadCalibration() { }
    
    // Responsibility 6: Error handling
    fun handleError() { }
    fun recoverFromError() { }
}
```

#### 1.3 Magic Numbers
```kotlin
private const val PREVIEW_UPDATE_FRAME_INTERVAL = 10
private const val PREVIEW_THROTTLE_MODULO = 100
private const val TEMPERATURE_OFFSET = 273.15
private const val DEFAULT_EMISSIVITY = 0.95
```

### Refactoring Recommendation

**Split into 6 focused classes:**

```kotlin
// 1. Core recorder (300-400 lines)
class ThermalCameraRecorder(
    private val frameProcessor: ThermalFrameProcessor,
    private val dataWriter: ThermalDataWriter,
    private val previewManager: ThermalPreviewManager,
    private val calibrationManager: ThermalCalibrationManager,
    private val errorHandler: ThermalErrorHandler
) : SensorRecorder {
    override fun startRecording(sessionDir: File): Result<Unit>
    override fun stopRecording(): Result<RecordingStats>
}

// 2. Frame processing (500-600 lines)
class ThermalFrameProcessor {
    fun processFrame(rawData: ByteArray): ThermalFrame
    fun convertTemperature(raw: Int): Float
    fun applyCalibration(frame: ThermalFrame): ThermalFrame
}

// 3. Data serialization (400-500 lines)
class ThermalDataWriter(private val sessionDir: File) {
    fun writeFrame(frame: ThermalFrame)
    fun writeMetadata(metadata: Map<String, Any>)
    fun flush()
    fun close()
}

// 4. Preview management (300-400 lines)
class ThermalPreviewManager {
    fun updatePreview(frame: ThermalFrame)
    fun throttlePreview(frameNumber: Int): Boolean
    fun setPreviewCallback(callback: (Bitmap) -> Unit)
}

// 5. Calibration (400-500 lines)
class ThermalCalibrationManager {
    fun loadCalibration(): CalibrationData
    fun saveCalibration(data: CalibrationData)
    fun applyCalibration(frame: ThermalFrame): ThermalFrame
}

// 6. Error handling (300-400 lines)
class ThermalErrorHandler {
    fun handleConnectionError(error: Exception)
    fun handleFrameDropError(error: Exception)
    fun recoverFromError(errorType: ErrorType): Result<Unit>
}
```

**Benefits:**
- Each class < 600 lines (manageable)
- Single responsibility per class
- Easier to test
- Easier to understand and modify
- Reusable components

---

## 2. RecordingController.kt (2,320 lines)

**Location**: `app/src/main/java/mpdc4gsr/controller/RecordingController.kt`

### Anti-Patterns Identified

#### 2.1 God Class
- **Lines**: 2,320
- **Manages**: Multiple sensors, sync, storage, errors, metadata

#### 2.2 Too Many Dependencies
```kotlin
class RecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val sensorRecorders: ConcurrentHashMap<String, SensorRecorder>
    private val sessionDirectoryManager: SessionDirectoryManager
    private val timeSynchronizationService: TimeSynchronizationService
    private var currentSessionDirectory: SessionDirectory?
    private var sessionMetadata: SessionMetadata?
    // Many more fields...
}
```

#### 2.3 Mixed Concerns
- Recording control
- Sensor management
- Time synchronization
- Session management
- Storage management
- Error monitoring

### Refactoring Recommendation

```kotlin
// 1. Core controller (500-600 lines)
class RecordingController(
    private val sensorRegistry: SensorRegistry,
    private val sessionService: SessionService,
    private val syncService: TimeSynchronizationService,
    private val storageService: StorageService
) {
    fun startRecording(sessionId: String): Result<Unit>
    fun stopRecording(): Result<RecordingStats>
    fun getStatus(): RecordingStatus
}

// 2. Sensor registry (300-400 lines)
class SensorRegistry {
    fun register(sensor: SensorRecorder)
    fun unregister(sensorId: String)
    fun getAll(): List<SensorRecorder>
    fun startAll(): Result<Unit>
    fun stopAll(): Result<Map<String, RecordingStats>>
}

// 3. Session service (400-500 lines)
class SessionService(private val sessionDirectoryManager: SessionDirectoryManager) {
    fun createSession(metadata: SessionMetadata): Session
    fun finalizeSession(sessionId: String): Result<Unit>
    fun getSession(sessionId: String): Session?
}

// 4. Storage service (300-400 lines)
class StorageService {
    fun checkAvailableSpace(): StorageStatus
    fun estimateRequiredSpace(sensors: List<SensorRecorder>): Long
    fun validateStorage(): Result<Unit>
}
```

---

## 3. WebSocketClient.kt (1,581 lines)

**Location**: `app/src/main/java/mpdc4gsr/network/WebSocketClient.kt`

### Anti-Patterns Identified

#### 3.1 GlobalScope Usage
```kotlin
GlobalScope.launch {
    // Network operations that may outlive component
}
```

#### 3.2 Mixed Responsibilities
- WebSocket connection
- Service discovery (Zeroconf)
- Time synchronization
- Authentication
- Certificate management
- Message handling

#### 3.3 Nested Anonymous Classes
```kotlin
setListener(object : TimeSyncService.TimeSyncListener {
    override fun onSyncComplete() {
        object : AdvancedAuthenticationManager.AuthenticationListener {
            override fun onAuthSuccess() {
                // Deeply nested logic
            }
        }
    }
})
```

### Refactoring Recommendation

```kotlin
// 1. WebSocket client (400-500 lines)
class WebSocketClient(
    private val connectionManager: WebSocketConnectionManager,
    private val messageHandler: WebSocketMessageHandler,
    private val authManager: AuthenticationManager,
    private val scope: CoroutineScope
) {
    fun connect(url: String): Result<Unit>
    fun disconnect()
    fun send(message: String)
}

// 2. Connection manager (300-400 lines)
class WebSocketConnectionManager(private val scope: CoroutineScope) {
    fun connect(url: String): Result<WebSocket>
    fun reconnect(): Result<Unit>
    fun disconnect()
    fun isConnected(): Boolean
}

// 3. Message handler (300-400 lines)
class WebSocketMessageHandler {
    fun handleMessage(message: String)
    fun registerHandler(type: String, handler: MessageHandler)
}

// 4. Authentication manager (400-500 lines)
class AuthenticationManager(
    private val certificateManager: CertificateManager
) {
    fun authenticate(): Result<Token>
    fun refreshToken(): Result<Token>
}
```

**Key Improvements:**
- Replace GlobalScope with injected CoroutineScope
- Separate concerns into focused classes
- Reduce nesting with better abstractions

---

## 4. AppHolder.java (319 lines)

**Location**: `BleModule/src/main/java/com/topdon/commons/base/AppHolder.java`

### Anti-Patterns Identified

#### 4.1 Reflection to Access Private APIs
```java
@SuppressLint("PrivateApi")
private Application tryGetApplication() {
    try {
        Class<?> cls = Class.forName("android.app.ActivityThread");
        Method catMethod = cls.getMethod("currentActivityThread");
        catMethod.setAccessible(true);
        Object aThread = catMethod.invoke(null);
        Method method = aThread.getClass().getMethod("getApplication");
        return (Application) method.invoke(aThread);
    } catch (Exception e) {
        return null;
    }
}
```

**Issues:**
- Uses internal Android APIs
- May break on different Android versions
- Suppresses lint warnings
- Violates Android guidelines

#### 4.2 Static Singleton with Mutable State
```java
public class AppHolder implements Application.ActivityLifecycleCallbacks {
    private final List<RunningActivity> runningActivities = new CopyOnWriteArrayList<>();
    private boolean isCompleteExit = false;
    private Application application;
    private RunningActivity topActivity;
    
    public static AppHolder getInstance() {
        return Holder.INSTANCE;
    }
}
```

**Issues:**
- Global mutable state
- Difficult to test
- Potential memory leaks
- Thread-safety concerns

#### 4.3 Activity Reference Retention
```java
private static class RunningActivity {
    String name;
    WeakReference<Activity> weakActivity;  // WeakReference helps but still problematic
}
```

### Refactoring Recommendation

```kotlin
// 1. Replace with proper initialization
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ActivityLifecycleManager.initialize(this)
    }
}

// 2. Use modern Android APIs
class ActivityLifecycleManager private constructor(
    private val application: Application
) : DefaultLifecycleObserver {
    
    private val _currentActivity = MutableStateFlow<Activity?>(null)
    val currentActivity: StateFlow<Activity?> = _currentActivity.asStateFlow()
    
    companion object {
        @Volatile
        private var instance: ActivityLifecycleManager? = null
        
        fun initialize(application: Application) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = ActivityLifecycleManager(application)
                    }
                }
            }
        }
        
        fun getInstance(): ActivityLifecycleManager {
            return instance ?: throw IllegalStateException("Not initialized")
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity) {
            _currentActivity.value = owner
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        if (owner is Activity && _currentActivity.value == owner) {
            _currentActivity.value = null
        }
    }
}
```

**Benefits:**
- No reflection
- Uses official Android APIs
- Type-safe
- Testable
- Modern Kotlin patterns

---

## 5. NavigationManager.kt

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/navigation/NavigationManager.kt`

### Anti-Patterns Identified

#### 5.1 Object Singleton
```kotlin
object NavigationManager {
    // Singleton pattern
}
```

#### 5.2 String-Based Class Loading
```kotlin
private fun createIntent(context: Context, route: String): Intent {
    val activityClass = when (route) {
        RouterConfig.MAIN -> getClassByName("mpdc4gsr.activities.MainActivity")
        RouterConfig.CLAUSE -> getClassByName("mpdc4gsr.activities.ClauseActivity")
        // Many more...
    }
}

private fun getClassByName(className: String): Class<*> {
    return Class.forName(className)
}
```

**Issues:**
- Reflection overhead
- No compile-time checking
- Typos cause runtime crashes
- Proguard/R8 issues

#### 5.3 Mixed with Modern Compose Navigation
- Legacy intent-based system coexists with modern Compose navigation
- Duplication of route definitions
- Inconsistent navigation patterns

### Refactoring Recommendation

```kotlin
// 1. Replace with type-safe navigation
sealed class Destination {
    object Main : Destination()
    object Clause : Destination()
    data class ThermalCamera(val cameraId: String) : Destination()
}

class NavigationController(private val context: Context) {
    fun navigate(destination: Destination) {
        val intent = when (destination) {
            is Destination.Main -> Intent(context, MainActivity::class.java)
            is Destination.Clause -> Intent(context, ClauseActivity::class.java)
            is Destination.ThermalCamera -> {
                Intent(context, ThermalCameraActivity::class.java).apply {
                    putExtra("camera_id", destination.cameraId)
                }
            }
        }
        context.startActivity(intent)
    }
}

// 2. Or use Compose Navigation exclusively
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("clause") { ClauseScreen(navController) }
        composable("thermal/{cameraId}") { backStackEntry ->
            ThermalCameraScreen(
                cameraId = backStackEntry.arguments?.getString("cameraId"),
                navController = navController
            )
        }
    }
}
```

---

## 6. FileSchemaManager.kt

**Location**: `app/src/main/java/mpdc4gsr/data/FileSchemaManager.kt`

### Anti-Patterns Identified

#### 6.1 Code Duplication
Multiple schema classes with nearly identical structure:

```kotlin
class ThermalSchema : SensorSchema {
    override fun getRequiredColumns(): List<String> = listOf(...)
    override fun getOptionalColumns(): List<String> = listOf(...)
    override fun getFileExtensions(): List<String> = listOf(...)
    override fun getUnits(): Map<String, String> = mapOf(...)
    override fun validateData(data: Map<String, Any>): ValidationResult { }
}

class RgbSchema : SensorSchema {
    // Almost identical structure
}

class AudioSchema : SensorSchema {
    // Almost identical structure
}
```

#### 6.2 Hard-Coded Validation Rules
```kotlin
override fun validateData(data: Map<String, Any>): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    
    // Hard-coded validation logic
    val sampleRate = data["sample_rate_hz"] as? Int
    if (sampleRate != null && sampleRate < 8000) {
        warnings.add("Sample rate ($sampleRate Hz) is below recommended minimum (8000 Hz)")
    }
    // More hard-coded rules...
}
```

### Refactoring Recommendation

```kotlin
// 1. Data-driven schema definition
data class SchemaDefinition(
    val sensorType: String,
    val requiredColumns: List<ColumnDefinition>,
    val optionalColumns: List<ColumnDefinition>,
    val fileExtensions: List<String>,
    val validationRules: List<ValidationRule>
)

data class ColumnDefinition(
    val name: String,
    val type: ColumnType,
    val unit: String?,
    val description: String
)

sealed class ValidationRule {
    data class Range(
        val column: String,
        val min: Number?,
        val max: Number?,
        val severity: Severity
    ) : ValidationRule()
    
    data class MinValue(
        val column: String,
        val minValue: Number,
        val severity: Severity
    ) : ValidationRule()
}

enum class Severity { ERROR, WARNING }

// 2. Generic schema validator
class SchemaValidator(private val definition: SchemaDefinition) {
    fun validate(data: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Generic validation using rules
        definition.validationRules.forEach { rule ->
            when (rule) {
                is ValidationRule.Range -> validateRange(data, rule, errors, warnings)
                is ValidationRule.MinValue -> validateMinValue(data, rule, errors, warnings)
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
}

// 3. Load schemas from configuration
object SchemaRegistry {
    private val schemas = mutableMapOf<String, SchemaDefinition>()
    
    fun loadSchemas() {
        // Load from JSON/YAML configuration
        schemas["thermal"] = loadFromConfig("schemas/thermal.json")
        schemas["rgb"] = loadFromConfig("schemas/rgb.json")
        schemas["audio"] = loadFromConfig("schemas/audio.json")
    }
    
    fun getSchema(sensorType: String): SchemaDefinition? = schemas[sensorType]
}
```

**Benefits:**
- No code duplication
- Easy to add new schemas
- Configuration can be updated without code changes
- Testable validation logic
- Reusable validation rules

---

## 7. Multiple MainActivity Variants

### Files
```
app/src/main/java/mpdc4gsr/activities/MainActivity.kt (34 lines)
app/src/main/java/mpdc4gsr/activities/MainActivityLegacy.kt (394 lines)
app/src/main/java/mpdc4gsr/activities/MainActivityAlternative.kt (667 lines)
app/src/main/java/mpdc4gsr/activities/SimplifiedMainActivity.kt (195 lines)
app/src/main/java/mpdc4gsr/activities/SimplifiedMainActivityCompose.kt (571 lines)
```

### Anti-Patterns Identified

#### 7.1 Multiple Entry Points
- 5 different MainActivity implementations
- Unclear which is production
- Duplicate logic across variants
- Confusing for developers

#### 7.2 Inconsistent Features
- Some use Compose, some use Fragments
- Different navigation approaches
- Different feature sets
- Testing nightmare

### Consolidation Strategy

```kotlin
// 1. Single production MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                AppNavigation()
            }
        }
    }
}

// 2. Optional: Feature flag for legacy support
class MainActivityWithFeatureFlags : ComponentActivity() {
    private val useLegacyUI by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("use_legacy_ui", false)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (useLegacyUI) {
            setContentView(R.layout.activity_main_legacy)
            // Legacy fragment-based UI
        } else {
            setContent {
                IRCameraTheme {
                    AppNavigation()
                }
            }
        }
    }
}

// 3. Development/Testing activities should be in debug source set
// app/src/debug/java/mpdc4gsr/activities/MainActivityTesting.kt
class MainActivityTesting : ComponentActivity() {
    // Testing features only available in debug builds
}
```

**Action Plan:**
1. Choose ONE MainActivity as production
2. Archive others in backup directory
3. Move test variants to debug source set
4. Update AndroidManifest.xml to point to single entry point
5. Add feature flags if legacy support needed

---

## 8. Summary of Refactoring Priorities

### Immediate (Week 1-2)
1. **ThermalCameraRecorder** - Split into 6 classes
2. **GlobalScope replacement** - All network and sensor classes
3. **AppHolder.java** - Replace with proper lifecycle management

### Short-term (Week 3-4)
4. **RecordingController** - Split into 4 focused services
5. **WebSocketClient** - Separate concerns, fix GlobalScope
6. **MainActivity consolidation** - Reduce to 1-2 variants

### Medium-term (Week 5-8)
7. **NavigationManager** - Replace with type-safe navigation
8. **FileSchemaManager** - Data-driven schema system
9. **Exception handling** - Specific exception types throughout

### Long-term (Week 9-12)
10. **Testing structure** - Move tests to appropriate source sets
11. **Naming conventions** - Standardize across codebase
12. **Documentation** - Update after refactoring

---

## 9. Testing Strategy for Refactored Code

### Unit Tests
```kotlin
class ThermalFrameProcessorTest {
    private lateinit var processor: ThermalFrameProcessor
    
    @Before
    fun setup() {
        processor = ThermalFrameProcessor()
    }
    
    @Test
    fun `processFrame converts raw data correctly`() {
        val rawData = byteArrayOf(/* test data */)
        val frame = processor.processFrame(rawData)
        
        assertThat(frame.width).isEqualTo(256)
        assertThat(frame.height).isEqualTo(192)
    }
}
```

### Integration Tests
```kotlin
class ThermalCameraRecorderIntegrationTest {
    @Test
    fun `full recording workflow`() = runBlocking {
        val recorder = ThermalCameraRecorder(/* dependencies */)
        
        recorder.startRecording(testSessionDir)
        delay(1000)
        val stats = recorder.stopRecording()
        
        assertThat(stats.framesRecorded).isGreaterThan(0)
    }
}
```

---

## 10. Metrics to Track

### Code Quality Metrics
- Lines per class (target: < 500)
- Methods per class (target: < 20)
- Cyclomatic complexity (target: < 10 per method)
- Test coverage (target: > 70%)

### Anti-Pattern Reduction
- God Classes: 8 → 0
- GlobalScope usages: 10+ → 0
- Generic exception catches: 1,065 → < 100
- Reflection usages: 47 → < 10

### Performance Metrics
- App startup time
- Memory usage
- Frame processing time
- Battery consumption

---

This document should be updated as refactoring progresses to track improvements and document decisions.
