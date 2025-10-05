# Android Core Development Priorities

This document establishes the core priorities and implementation guidelines for Android development in the IRCamera project, aligned with modern Android best practices and Google's recommended architecture.

## Overview

Core priorities in Android development focus on delivering reliable, performant, secure, and maintainable applications that provide excellent user experiences across diverse device configurations.

## 1. Correctness and User Experience

### Lifecycle Management

**Priority:** CRITICAL

**Requirements:**
- Handle Activity/Fragment lifecycle transitions correctly
- Survive process death and configuration changes
- Use lifecycle-aware components (ViewModel, LiveData, StateFlow)
- Implement proper state restoration

**Implementation Guidelines:**
```kotlin
// Use ViewModel for UI state
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Cleanup in onCleared
    override fun onCleared() {
        super.onCleared()
        // Cancel coroutines, clean resources
        viewModelScope.cancel()
    }
}

// In Activity/Fragment
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore state if needed
        savedInstanceState?.let { /* restore UI state */ }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save UI state
    }
}
```

**Status:** In Progress
- ViewModels implemented: ✓
- State restoration: Partial
- Process death handling: Needs verification

### Navigation and Back Behavior

**Requirements:**
- Consistent navigation patterns using Navigation Component or manual back stack management
- Predictable back button behavior
- Deep linking support where applicable

**Status:** Implemented (using manual navigation)

## 2. Performance

### Cold Start Time

**Priority:** HIGH

**Target:** < 2 seconds to first frame

**Optimization Strategies:**
1. Lazy initialization of non-critical components
2. Defer heavy SDK initialization to background
3. Use baseline profiles (Android 12+)
4. Optimize Application.onCreate()
5. Remove blocking operations from main thread

**Current Status:** Needs measurement and optimization

**Implementation:**
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Critical initialization only
        initializeLogging()
        
        // Defer non-critical initialization
        lifecycleScope.launch(Dispatchers.Default) {
            delayInit()
        }
    }
    
    private suspend fun delayInit() {
        // Heavy SDK initialization
        initAnalytics()
        initCrashReporting()
    }
}
```

### Rendering Performance

**Target:** 
- 60 FPS (16.67ms per frame) minimum
- 90/120 FPS on capable devices

**Requirements:**
- No UI jank (avoid long main thread operations)
- Efficient Compose recomposition
- RecyclerView optimization where used
- Profile with GPU rendering tools

**Status:** Needs profiling

### Memory Management

**Requirements:**
- Low GC churn (< 5MB allocations per second in steady state)
- No memory leaks (verified with LeakCanary in debug)
- Efficient bitmap handling
- Proper resource cleanup

**Current Issues:**
- GlobalScope usage causes potential memory leaks (CRITICAL)
- Some resources not properly cleaned up (HIGH)

See: [android-code-quality-guide.md](android-code-quality-guide.md)

### I/O Operations

**Requirements:**
- No I/O on main thread (enforced by StrictMode in debug)
- Use coroutines with Dispatchers.IO for disk/network operations
- Batch operations where possible

## 3. Reliability

### Crash and ANR Rates

**Priority:** CRITICAL

**Targets:**
- Crash-free sessions: > 99.5%
- ANR rate: < 0.1%

**Requirements:**
1. No runBlocking in lifecycle methods (causes ANRs)
2. Proper exception handling with specific catch blocks
3. Defensive programming (null checks, lateinit verification)
4. Timeout for network operations

**Current Issues:**
- runBlocking in lifecycle methods (CRITICAL - ANR risk)
- Bare exception catching in multiple locations (MEDIUM)

**Implementation:**
```kotlin
// WRONG - blocks main thread
override fun onCleared() {
    runBlocking { cleanup() }
}

// CORRECT - non-blocking
override fun onCleared() {
    super.onCleared()
    viewModelScope.cancel() // Cancels all child coroutines
}

// WRONG - catches everything
try {
    doWork()
} catch (e: Exception) { }

// CORRECT - specific exceptions
try {
    doWork()
} catch (e: IOException) {
    handleNetworkError(e)
} catch (e: IllegalStateException) {
    handleInvalidState(e)
}
```

### Idempotent Work

**Requirements:**
- Background operations should be retryable without side effects
- Use WorkManager for deferrable work with retry policies
- Proper transaction handling for database operations

## 4. Security and Privacy

### Permissions

**Priority:** HIGH

**Requirements:**
1. Request runtime permissions using Activity Result API
2. Explain permission rationale to users
3. Graceful degradation if permission denied
4. Minimum required permissions only

**Current Status:** Implemented with PermissionManager

**Permissions Declared:**
- Camera: Required for thermal/RGB recording
- Bluetooth: Required for GSR sensor connectivity
- Location: Required for Bluetooth scanning (Android 12+)
- Storage: Scoped storage (Android 13+)
- Foreground Service: For recording service

### Storage Security

**Requirements:**
1. Use scoped storage (Android 10+)
2. Encrypt sensitive data at rest
3. Use Content Providers for sharing files
4. Clear cache periodically

**Status:** Partially implemented (scoped storage used)

### Network Security

**Configuration:** `network_security_config.xml`

**Current Issues:**
- Cleartext traffic permitted (required for local network communication)
- Should restrict to specific domains in production

**Recommendation:**
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <!-- Only allow cleartext for local network -->
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">192.168.0.0/16</domain>
        <domain includeSubdomains="false">10.0.0.0/8</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### Code Integrity

**Requirements:**
1. ProGuard/R8 in release builds
2. APK signing v2/v3
3. Play App Signing
4. Code obfuscation for sensitive logic

**Status:** To be verified in build configuration

## 5. Background Execution

### WorkManager Usage

**Priority:** HIGH

**Requirements:**
1. Use WorkManager for deferrable background work
2. Use Foreground Services only for user-initiated, time-critical work
3. Declare proper foreground service types (Android 14+)
4. Respect Doze and App Standby

**Current Implementation:**
- RecordingService: Foreground Service with CAMERA + DATA_SYNC types ✓
- File uploads: Should migrate to WorkManager
- Periodic sync: Should use WorkManager with periodic constraints

**Foreground Service Types (Android 14+):**
```xml
<service android:name=".RecordingService"
    android:foregroundServiceType="camera|dataSync" />
```

**WorkManager Example:**
```kotlin
val uploadWorkRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build())
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS)
    .build()

WorkManager.getInstance(context).enqueue(uploadWorkRequest)
```

**Status:** RecordingService compliant, file operations need review

## 6. Compatibility

### SDK Version Support

**Current Configuration:**
- minSdk: 26 (Android 8.0)
- targetSdk: 35 (Android 15)
- compileSdk: 36 (Android 15 Preview)

**Requirements:**
1. Target latest stable SDK (35 currently)
2. Feature-gate by API level using Build.VERSION.SDK_INT
3. Handle behavioral changes for each SDK level
4. Test on minSdk device

**Implementation:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Android 13+ granular media permissions
    requestPermissions(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
} else {
    // Legacy storage permissions
    requestPermissions(READ_EXTERNAL_STORAGE)
}
```

### Device Form Factors

**Considerations:**
- Phones: Primary target ✓
- Tablets: Should be tested
- Foldables: Should handle configuration changes
- Wear/TV/Auto: Not applicable

**Status:** Phone-optimized, tablet compatibility needs verification

## 7. Architecture and Maintainability

### Architecture Pattern

**Current:** MVVM with Repository pattern (Hybrid XML/Compose)

**Requirements:**
1. Clear separation of concerns (UI, Business Logic, Data)
2. Unidirectional data flow
3. Dependency injection (Manual, should migrate to Hilt)
4. Testable architecture

**Structure:**
```
app/
├── core/          # Cross-cutting concerns
├── feature/       # Feature modules
│   ├── ui/        # Compose/XML UI
│   ├── viewmodel/ # ViewModels
│   └── data/      # Repositories
└── sensors/       # Sensor integrations
```

**Status:** Good foundation, DI migration recommended

### Compose Migration

**Status:** In Progress (Hybrid approach)

**Guidelines:**
1. New features: Compose-first
2. Legacy screens: Migrate opportunistically
3. Use Material3 design system
4. State hoisting pattern
5. Composition over inheritance

**Current Progress:**
- Settings screens: Migrated ✓
- Camera screens: Partial
- Legacy screens: XML with gradual migration

## 8. Testing and Quality Assurance

### Test Coverage

**Requirements:**
1. Unit tests: Business logic, ViewModels, Repositories (Target: 70%+)
2. Integration tests: Feature flows
3. UI tests: Critical user journeys (Compose UI Testing, Espresso)
4. Contract tests: API responses, database schemas

**Current Status:**
- Test infrastructure exists
- Coverage needs improvement

### Static Analysis

**Tools:**
1. Android Lint (built-in)
2. Detekt (Kotlin static analysis) - configured ✓
3. ktlint (code style) - to be added

**CI Integration:**
- Should run lint checks on every PR
- Fail build on critical issues
- Generate reports

**Lint Configuration:**
```gradle
android {
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        
        disable += listOf(
            "ContentDescription", // Accessibility - reviewed separately
        )
    }
}
```

**Status:** Detekt configured, CI integration needed

### StrictMode (Debug Only)

**Purpose:** Detect performance issues during development

**Implementation:**
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }
    
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
```

**Status:** Not implemented (HIGH PRIORITY)

## 9. Accessibility and Localization

### Accessibility

**Requirements:**
1. TalkBack support (content descriptions)
2. Minimum touch target size: 48dp x 48dp
3. Color contrast ratio: 4.5:1 minimum
4. Dynamic type support
5. Semantic markup in Compose

**Implementation:**
```kotlin
// Compose accessibility
Button(
    onClick = { /* action */ },
    modifier = Modifier
        .semantics { 
            contentDescription = "Start recording"
            role = Role.Button
        }
        .minimumInteractiveComponentSize()
) {
    Icon(Icons.Default.PlayArrow, contentDescription = null)
    Text("Record")
}
```

**Status:** Needs audit and implementation

### Localization

**Requirements:**
1. Externalize all user-facing strings
2. Support RTL layouts
3. Handle plurals correctly
4. Date/time formatting with locale

**Current Status:** Partial string externalization

## 10. Observability and Operations

### Telemetry and Monitoring

**Requirements:**
1. Crash reporting (Firebase Crashlytics or similar)
2. ANR tracking
3. Performance monitoring (startup time, frame drops)
4. Custom metrics (recording success rate, sensor connectivity)

**Status:** Logging infrastructure exists, crash reporting TBD

### Feature Flags

**Purpose:** Enable/disable features remotely, gradual rollouts, A/B testing

**Current Status:** FeatureFlags class exists in codebase

**Enhancement:**
```kotlin
object FeatureFlags {
    // Local flags (hardcoded for now)
    var enableNewCameraUI: Boolean = false
    var enableAdvancedThermalAnalysis: Boolean = false
    
    // TODO: Integrate with Firebase Remote Config
    fun initialize(remoteConfig: RemoteConfig) {
        enableNewCameraUI = remoteConfig.getBoolean("enable_new_camera_ui")
        // ...
    }
}
```

### Metrics and KPIs

**Key Performance Indicators:**

| KPI | Target | Measurement |
|-----|--------|-------------|
| Crash-free sessions | > 99.5% | Firebase Crashlytics |
| ANR rate | < 0.1% | Play Console Vitals |
| Cold start time | < 2s | Firebase Performance, custom instrumentation |
| Janky frames | < 5% | FrameMetrics API |
| Battery impact | < 5%/hour during recording | BatteryManager, profiling |
| Install size | < 150MB | APK Analyzer |
| Permission grant rate | > 90% for critical permissions | Custom analytics |
| User retention (30-day) | > 60% | Analytics platform |

**Implementation:**
```kotlin
// Track cold start time
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val startTime = SystemClock.elapsedRealtime()
        
        // ... initialization ...
        
        val coldStartDuration = SystemClock.elapsedRealtime() - startTime
        logPerformanceMetric("app_cold_start_ms", coldStartDuration)
    }
}

// Track jank
activity.window.decorView.viewTreeObserver.addOnDrawListener {
    // Measure frame time
}
```

## 11. Distribution

### App Bundle Configuration

**Requirements:**
1. Use Android App Bundle (.aab) for Play Store
2. Enable language and density splits
3. Dynamic feature modules for optional features
4. Optimize resources

**Current Configuration:**
```gradle
bundle {
    language {
        enableSplit = false  // Should be true for size optimization
    }
    abi {
        enableSplit = true
    }
}
```

**Recommendation:** Enable language splits

### Size Optimization

**Strategies:**
1. Enable shrinking and obfuscation (R8)
2. Remove unused resources
3. Compress images (WebP)
4. Use vector drawables
5. Analyze with APK Analyzer

**Current Status:** To be verified

### Baseline Profiles

**Purpose:** Optimize cold start and runtime performance (Android 12+)

**Implementation:** Generate baseline profiles using Macrobenchmark library

**Status:** Not implemented (MEDIUM PRIORITY)

## 12. Tooling and CI/CD

### Build Configuration

**Requirements:**
1. Reproducible builds
2. Build caching enabled
3. Parallel execution
4. Dependency locking

**Current Status:** Basic configuration exists

### CI/CD Pipeline

**Recommended Pipeline:**

```yaml
name: Android CI

on: [pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Lint
        run: ./gradlew lint
      - name: Run Detekt
        run: ./gradlew detekt
        
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: '**/build/reports/tests'
          
  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Release APK
        run: ./gradlew assembleRelease
```

**Status:** Not implemented (HIGH PRIORITY)

### Code Quality Gates

**Requirements:**
1. Lint: No critical errors
2. Tests: All passing
3. Coverage: Minimum threshold (70%)
4. Code review: Required
5. Security scan: No high/critical vulnerabilities

## Immediate Action Plan

### Phase 1: Critical Issues (Week 1)

1. **Eliminate GlobalScope** - Replace with lifecycle-aware scopes
   - Estimated effort: 8 hours
   - Impact: HIGH (prevents memory leaks)
   
2. **Remove runBlocking from lifecycle methods** - Replace with proper async patterns
   - Estimated effort: 4 hours
   - Impact: HIGH (prevents ANRs)
   
3. **Add ExecutorService shutdown** - Ensure proper resource cleanup
   - Estimated effort: 2 hours
   - Impact: MEDIUM (resource leaks)

4. **Implement StrictMode in debug builds**
   - Estimated effort: 1 hour
   - Impact: MEDIUM (catch issues early)

### Phase 2: High Priority (Week 2-3)

5. **Set up CI/CD pipeline** with quality gates
   - Estimated effort: 8 hours
   - Impact: HIGH (prevent regressions)

6. **Add crash and ANR monitoring**
   - Estimated effort: 4 hours
   - Impact: HIGH (production reliability)

7. **Optimize cold start time**
   - Profile current: 2 hours
   - Implement optimizations: 8 hours
   - Impact: HIGH (UX improvement)

8. **Implement WorkManager for background tasks**
   - Estimated effort: 6 hours
   - Impact: MEDIUM (battery, reliability)

### Phase 3: Medium Priority (Week 4)

9. **Generate baseline profiles**
10. **Accessibility audit and fixes**
11. **Enhanced network security config**
12. **Telemetry and metrics dashboard**

## Monitoring and Continuous Improvement

1. **Weekly:** Review crash/ANR reports, address top issues
2. **Sprint:** Review performance metrics, plan optimizations
3. **Release:** Staged rollout with monitoring, rollback plan
4. **Quarterly:** Architecture review, dependency updates, security audit

## References

- [Android Developer Documentation](https://developer.android.com/docs)
- [Android Jetpack](https://developer.android.com/jetpack)
- [Modern Android Development](https://developer.android.com/modern-android-development)
- [Android Performance Patterns](https://developer.android.com/topic/performance)
- [Material Design Guidelines](https://m3.material.io/)

## Related Documentation

- [android-code-quality-guide.md](android-code-quality-guide.md) - Specific code quality issues
- [design-decisions-table.md](../thesis/chapter3/design-decisions-table.md) - Architecture decisions
- Build configuration: `app/build.gradle.kts`, `build.gradle.kts`

---

**Last Updated:** 2024
**Status:** Living document - updated as priorities evolve
