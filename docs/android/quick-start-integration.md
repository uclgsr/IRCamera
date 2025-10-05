# Quick Start Integration Guide

This guide provides step-by-step instructions for integrating the core priorities framework into your development workflow.

## 1. Enable StrictMode (Already Done)

StrictMode is automatically enabled in debug builds via `App.kt`. No action needed.

**Verify it's working:**
```bash
# Run app in debug mode and check logs for StrictMode violations
adb logcat | grep StrictMode
```

## 2. Start Tracking Performance Metrics

### In Your Activity

```kotlin
import mpdc4gsr.core.monitoring.PerformanceMetrics
import mpdc4gsr.core.monitoring.measureTime

class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Track cold start completion
        PerformanceMetrics.recordColdStartComplete()
        
        setContent {
            MyScreen()
        }
    }
}
```

### In Your ViewModel

```kotlin
import mpdc4gsr.core.monitoring.measureTime
import mpdc4gsr.core.monitoring.PerformanceMetrics

class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            // Measure operation time
            val data = measureTime("load_user_data") {
                repository.loadData()
            }
            
            // Track success
            PerformanceMetrics.incrementCounter("data_load_success")
        }
    }
    
    fun handleError(error: Throwable) {
        // Track failures
        PerformanceMetrics.incrementCounter("data_load_failure")
    }
}
```

## 3. Start Tracking User Events

### In Your Activity

```kotlin
import mpdc4gsr.core.monitoring.TelemetryManager

class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Track screen view
        TelemetryManager.trackScreenView(
            screenName = "My Screen",
            screenClass = this::class.java.simpleName
        )
        
        setContent {
            MyScreen()
        }
    }
}
```

### In Your ViewModel

```kotlin
import mpdc4gsr.core.monitoring.TelemetryManager

class RecordingViewModel : ViewModel() {
    fun startRecording() {
        viewModelScope.launch {
            try {
                // Start recording
                recorder.start()
                
                // Track event
                TelemetryManager.trackEvent("recording_started")
                TelemetryManager.incrementCounter("recording_sessions")
                
            } catch (e: Exception) {
                // Track error
                TelemetryManager.trackError(
                    error = "Failed to start recording",
                    exception = e,
                    fatal = false
                )
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            val duration = recorder.getDuration()
            recorder.stop()
            
            // Track recording session
            TelemetryManager.trackRecordingSession(
                recordingId = recorder.getId(),
                durationMs = duration,
                success = true
            )
        }
    }
}
```

## 4. Enable CI/CD Quality Gates

The GitHub Actions workflow is already configured at `.github/workflows/android-quality-gates.yml`.

**To enable:**
1. Commit and push your changes
2. Open a Pull Request
3. CI/CD will automatically run lint, tests, and build checks

**To run locally before pushing:**
```bash
# Run all quality checks
./gradlew lint detekt testDebugUnitTest assembleDebug

# Or individually
./gradlew lint
./gradlew detekt
./gradlew testDebugUnitTest
```

## 5. Track Permissions

```kotlin
import mpdc4gsr.core.monitoring.TelemetryManager

class PermissionHandler {
    fun requestCameraPermission(activity: Activity) {
        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // Track result
            TelemetryManager.trackPermissionRequest(
                permission = Manifest.permission.CAMERA,
                granted = granted
            )
            
            if (granted) {
                startCamera()
            }
        }
        
        launcher.launch(Manifest.permission.CAMERA)
    }
}
```

## 6. Monitor Network Requests

```kotlin
import mpdc4gsr.core.monitoring.TelemetryManager
import mpdc4gsr.core.monitoring.measureTime

class ApiClient {
    suspend fun uploadFile(file: File): Result<Unit> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = httpClient.post("/api/upload") {
                setBody(file)
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // Track network request
            TelemetryManager.trackNetworkRequest(
                endpoint = "/api/upload",
                method = "POST",
                statusCode = response.status.value,
                durationMs = duration
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            TelemetryManager.trackError("Upload failed", e)
            Result.failure(e)
        }
    }
}
```

## 7. Review Performance Metrics

Check metrics periodically:

```kotlin
import mpdc4gsr.core.monitoring.PerformanceMetrics

// In your debug menu or developer settings
fun showPerformanceReport() {
    PerformanceMetrics.logSummary()
    
    val jankyFrames = PerformanceMetrics.getJankyFramePercentage()
    if (jankyFrames > 5.0f) {
        Log.w("Performance", "Janky frames exceeds 5% target: $jankyFrames%")
    }
}
```

## 8. Monitor Frame Rendering (Optional)

For activities with custom animations or heavy UI:

```kotlin
import mpdc4gsr.core.monitoring.PerformanceMetrics

class AnimatedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable frame metrics monitoring
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.addOnFrameMetricsAvailableListener(
                { _, frameMetrics, _ ->
                    val totalDuration = frameMetrics.getMetric(
                        FrameMetrics.TOTAL_DURATION
                    )
                    PerformanceMetrics.recordFrameTime(totalDuration)
                },
                Handler(Looper.getMainLooper())
            )
        }
    }
}
```

## 9. Background Work with WorkManager

For file uploads or periodic tasks:

```kotlin
import mpdc4gsr.core.background.WorkManagerConfiguration
import androidx.work.*

class FileUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val fileUri = inputData.getString("file_uri") ?: return Result.failure()
        
        return try {
            // Track operation time
            measureTimeSuspend("file_upload") {
                uploadFile(Uri.parse(fileUri))
            }
            
            TelemetryManager.logMetric("upload_success", 1)
            Result.success()
            
        } catch (e: Exception) {
            TelemetryManager.trackError("Upload failed", e)
            
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule upload
fun scheduleUpload(context: Context, fileUri: Uri) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    
    val uploadRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
        .setConstraints(constraints)
        .setInputData(workDataOf("file_uri" to fileUri.toString()))
        .build()
    
    WorkManager.getInstance(context).enqueue(uploadRequest)
}
```

## 10. Run Tests

The framework includes unit tests for the monitoring components:

```bash
# Run all tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.PerformanceMetricsTest"
./gradlew testDebugUnitTest --tests "*.TelemetryManagerTest"
```

## Common Patterns

### Pattern 1: Track Feature Usage

```kotlin
fun onFeatureUsed(featureName: String, action: String) {
    TelemetryManager.trackFeatureUsage(featureName, action)
}

// Usage
onFeatureUsed("thermal_camera", "start_preview")
onFeatureUsed("gsr_sensor", "connect")
```

### Pattern 2: Track User Journey

```kotlin
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        TelemetryManager.trackEvent("onboarding_started")
        
        setContent {
            OnboardingFlow(
                onComplete = {
                    TelemetryManager.trackEvent("onboarding_completed")
                    navigateToMain()
                }
            )
        }
    }
}
```

### Pattern 3: Monitor Critical Operations

```kotlin
class CriticalOperation {
    suspend fun execute(): Result<Data> {
        PerformanceMetrics.startMeasurement("critical_operation")
        
        return try {
            val result = performOperation()
            
            val duration = PerformanceMetrics.endMeasurement("critical_operation")
            if (duration > 1000) {
                Log.w("Performance", "Critical operation took ${duration}ms")
            }
            
            PerformanceMetrics.incrementCounter("critical_operation_success")
            Result.success(result)
            
        } catch (e: Exception) {
            PerformanceMetrics.endMeasurement("critical_operation")
            PerformanceMetrics.incrementCounter("critical_operation_failure")
            TelemetryManager.trackError("Critical operation failed", e)
            Result.failure(e)
        }
    }
}
```

## Next Steps

1. **Week 1:** Integrate basic tracking in main flows
2. **Week 2:** Add tracking to all features
3. **Week 3:** Review metrics and optimize bottlenecks
4. **Week 4:** Set up dashboards and alerts

## Resources

- [android-core-priorities.md](android-core-priorities.md) - Complete guide
- [performance-optimization-guide.md](performance-optimization-guide.md) - Performance strategies
- [accessibility-guidelines.md](accessibility-guidelines.md) - Accessibility requirements
- [android-code-quality-guide.md](android-code-quality-guide.md) - Code quality checklist

## Troubleshooting

### StrictMode violations in production

StrictMode is only enabled in debug builds. Check `BuildConfig.DEBUG` flag.

### Tests failing

Ensure Robolectric is configured correctly for tests that need Android context.

### CI/CD failing

Check the workflow logs in GitHub Actions for specific errors. Run checks locally first.

### Missing metrics

Verify that `PerformanceMetrics.initialize()` and `TelemetryManager.initialize()` are called in `App.onCreate()`.

---

**Questions?** Check the comprehensive guides in `docs/android/` or file an issue on GitHub.
