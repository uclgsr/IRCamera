# Anti-Patterns Remediation Action Plan

Prioritized action plan for addressing identified anti-patterns in the IRCamera application.

## Priority Classification

- **P0 (Critical)**: Security vulnerabilities, data loss risks, crashes
- **P1 (High)**: Memory leaks, ANR risks, poor user experience
- **P2 (Medium)**: Code quality, maintainability, technical debt
- **P3 (Low)**: Minor optimizations, style improvements

## P0 - Critical Security Issues (Immediate Action Required)

### 1. Remove Hardcoded Credentials

**Issue**: Keystore passwords and authentication credentials in source code
**Risk**: Security breach, unauthorized access
**Files**:
- `app/build.gradle.kts` (Lines 66-69)
- `app/src/main/java/mpdc4gsr/core/data/AdvancedAuthenticationManager.kt` (Lines 231, 520-526)

**Action Items**:

#### Step 1: Move Keystore Credentials (1-2 hours)

Create `keystore.properties` (add to .gitignore):
```properties
storePassword=your_actual_password
keyPassword=your_actual_password
keyAlias=Artibox
storeFile=artibox_key/ArtiBox.jks
```

Update `app/build.gradle.kts`:
```kotlin
// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            keyAlias = keystoreProperties["keyAlias"] as String
            storePassword = keystoreProperties["storePassword"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            enableV1Signing = true
            enableV2Signing = true
        }
    }
}
```

#### Step 2: Remove Hardcoded Auth Credentials (2-3 hours)

Replace with encrypted SharedPreferences or Android Keystore:

```kotlin
class AdvancedAuthenticationManager(private val context: Context) {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "auth_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private fun verifyCredentials(username: String, password: String): Boolean {
        val storedHash = encryptedPrefs.getString("user_${username}_hash", null)
        return storedHash == hashPassword(password)
    }
}
```

**Verification**: Grep for hardcoded passwords should return zero results
**Estimated Time**: 3-5 hours
**Assignee**: Security lead

---

### 2. Enable ProGuard/R8 Obfuscation

**Issue**: Release builds not obfuscated
**Risk**: Easy reverse engineering, IP theft
**File**: `app/build.gradle.kts` (Line 78)

**Action Items**:

```kotlin
buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true          // ENABLE
        isShrinkResources = true        // ENABLE
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }
}
```

**Testing Required**:
1. Build release APK
2. Test all features thoroughly
3. Check crash reports for obfuscation issues
4. Update proguard-rules.pro as needed

**Estimated Time**: 4-8 hours (including testing)
**Assignee**: Android lead

---

## P1 - High Priority Memory & Performance Issues

### 3. Fix ViewModel Context References

**Issue**: ViewModels holding Context causing memory leaks
**Risk**: Activity leaks, OutOfMemoryError
**Files**:
- `app/src/main/java/mpdc4gsr/feature/device/presentation/DiagnosticsViewModel.kt`
- `app/src/main/java/mpdc4gsr/feature/settings/presentation/NetworkSettingsViewModel.kt`
- `app/src/main/java/mpdc4gsr/feature/settings/presentation/StorageSettingsViewModel.kt`
- `app/src/main/java/mpdc4gsr/feature/gsr/presentation/MultiModalRecordingViewModel.kt`

**Action Plan**:

#### DiagnosticsViewModel Fix

```kotlin
// Before
class DiagnosticsViewModel : AppBaseViewModel() {
    private lateinit var context: Context
    
    fun initialize(ctx: Context) {
        context = ctx
        updateSystemStatus()
    }
}

// After
class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    
    fun initialize() {
        updateSystemStatus()
    }
    
    private fun updateSystemStatus() {
        viewModelScope.launch {
            val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) 
                as BatteryManager
            // Use appContext instead of activity context
        }
    }
}
```

**Verification**: Run LeakCanary to detect remaining leaks
**Estimated Time**: 2-3 hours per ViewModel (8-12 hours total)
**Assignee**: Android developer

---

### 4. Fix Resource Leaks (File Streams)

**Issue**: FileWriter and FileOutputStream not properly closed
**Risk**: File descriptor exhaustion, memory leaks
**Files**:
- `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalRecorder.kt`
- `app/src/main/java/mpdc4gsr/feature/thermal/data/source/TopdonDataSourceImpl.kt`

**Action Plan**:

#### ThermalRecorder Fix

```kotlin
// Before
private var csvWriter: FileWriter? = null

suspend fun startRecording(...): Boolean {
    val csvFile = File(...)
    csvWriter = FileWriter(csvFile, false)
    csvWriter?.write(...)
    // May not be closed
}

// After
suspend fun startRecording(...): Boolean = withContext(Dispatchers.IO) {
    val csvFile = File(...)
    
    try {
        FileWriter(csvFile, false).use { writer ->
            writer.write(sessionMetadata.createTimingHeader())
            writer.write("# THERMAL FRAME DATA\n")
            // Additional writes...
        }
        true
    } catch (e: IOException) {
        Log.e(TAG, "Failed to start recording", e)
        false
    }
}

// Update stop recording to not rely on stored writer
suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
    isRecording.set(false)
    true
}
```

**Testing**: Monitor file descriptor count under stress
**Estimated Time**: 3-4 hours
**Assignee**: Thermal module developer

---

### 5. Convert Blocking DAO Methods

**Issue**: Database operations not using suspend/Flow
**Risk**: ANR on main thread
**Files**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/db/dao/*.kt`

**Action Plan**:

```kotlin
// Before
@Dao
interface ThermalDao {
    @Insert
    fun insert(entity: ThermalEntity): Long
    
    @Query("SELECT * FROM thermal_table")
    fun queryRecordList(): List<Record>
}

// After
@Dao
interface ThermalDao {
    @Insert
    suspend fun insert(entity: ThermalEntity): Long
    
    @Query("SELECT * FROM thermal_table")
    fun queryRecordList(): Flow<List<Record>>
    
    @Query("SELECT * FROM thermal_table")
    suspend fun queryRecordListOnce(): List<Record>
}
```

**Impact**: All callers must be updated to use coroutines
**Estimated Time**: 6-8 hours (including caller updates)
**Assignee**: Database module owner

---

## P2 - Medium Priority Code Quality Issues

### 6. Improve Error Handling

**Issue**: Inadequate exception handling and logging
**Risk**: Silent failures, poor debugging

**Action Plan**:

Create error handling utility:

```kotlin
object ErrorHandler {
    inline fun <T> runSafely(
        tag: String,
        operation: String,
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Log.e(tag, "Failed to $operation: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// Usage
val result = ErrorHandler.runSafely("ThermalRecorder", "start recording") {
    startRecordingInternal()
}

result.onFailure { error ->
    showErrorToUser("Recording failed: ${error.message}")
}
```

**Estimated Time**: 4-6 hours
**Assignee**: Any developer

---

### 7. Reduce !! Operator Usage

**Issue**: Excessive non-null assertions
**Risk**: Runtime NullPointerException

**Action Plan**: Review and refactor hot paths

```kotlin
// Before
val directory = sessionDirectory!!
val exists = directory.exists()

// After
val exists = sessionDirectory?.exists() ?: false
// Or
sessionDirectory?.let { directory ->
    if (directory.exists()) {
        // Safe to use directory
    }
}
```

**Tool**: Add Detekt rule to flag excessive !! usage
**Estimated Time**: 8-10 hours (throughout codebase)
**Assignee**: Code quality team

---

### 8. Improve Lint Configuration

**Issue**: Too many lint checks disabled
**File**: `app/build.gradle.kts`

**Action Plan**:

```kotlin
lint {
    abortOnError = true
    checkReleaseBuilds = true
    warningsAsErrors = false  // Enable gradually
    
    // Only disable with justification
    disable += listOf(
        "MissingTranslation"  // Internationalization not required yet
    )
    
    // Monitor these warnings
    warning += listOf(
        "StringFormatInvalid",
        "ResourceType"
    )
}
```

**Estimated Time**: 2-3 hours + fixing new warnings
**Assignee**: Android lead

---

## P3 - Low Priority Improvements

### 9. Refactor Singleton Pattern

**Issue**: Using double-checked locking
**File**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/comm/util/SingletonHolder.kt`

**Action Plan**: Use Kotlin lazy delegate or dependency injection
**Estimated Time**: 2-3 hours
**Assignee**: Architecture team

---

### 10. Remove Application.getInstance()

**Issue**: Static application reference
**File**: `app/src/main/java/mpdc4gsr/core/App.kt`

**Action Plan**: Implement Hilt dependency injection
**Estimated Time**: 16-24 hours (major refactor)
**Assignee**: Architecture team

---

## Implementation Schedule

### Sprint 1 (Week 1-2) - P0 Security Issues

- [ ] Remove hardcoded keystore credentials
- [ ] Remove hardcoded auth credentials  
- [ ] Enable ProGuard obfuscation
- [ ] Security audit and testing

**Goal**: All P0 issues resolved

### Sprint 2 (Week 3-4) - P1 Memory Issues

- [ ] Fix ViewModel context references
- [ ] Fix file stream resource leaks
- [ ] Convert blocking DAO methods
- [ ] Run LeakCanary tests

**Goal**: Zero memory leaks detected

### Sprint 3 (Week 5-6) - P1 Performance

- [ ] Complete DAO migration
- [ ] Performance testing
- [ ] ANR monitoring
- [ ] Update documentation

**Goal**: No ANR issues under normal load

### Sprint 4 (Week 7-8) - P2 Code Quality

- [ ] Improve error handling
- [ ] Reduce !! operator usage
- [ ] Improve lint configuration
- [ ] Code review process updates

**Goal**: Improved code quality metrics

### Sprint 5+ (Future) - P3 & Architecture

- [ ] Singleton refactoring
- [ ] Dependency injection implementation
- [ ] Continue incremental improvements
- [ ] Maintain quality standards

---

## Success Metrics

### Security
- [ ] Zero hardcoded credentials in source
- [ ] ProGuard enabled on all release builds
- [ ] Security scan passes with no critical issues

### Performance
- [ ] ANR rate < 0.1%
- [ ] Memory usage stable over time
- [ ] No file descriptor leaks

### Code Quality
- [ ] Lint warnings < 10
- [ ] Code coverage > 60%
- [ ] Technical debt reduced by 30%

### Team Process
- [ ] Code review checklist adopted
- [ ] Anti-pattern training completed
- [ ] Documentation up to date

---

## Risk Mitigation

### Testing Strategy
1. Unit tests for refactored components
2. Integration tests for critical paths
3. Manual QA testing before release
4. Staged rollout (10% → 50% → 100%)

### Rollback Plan
1. Keep git tags for stable versions
2. Feature flags for major changes
3. Quick revert procedures documented
4. Emergency hotfix process established

---

## Resources Needed

### Tools
- LeakCanary for memory leak detection
- Detekt for static analysis
- Android Studio Profiler
- Firebase Crashlytics

### Documentation
- Anti-patterns training materials
- Updated code review guidelines
- Architecture decision records

### Personnel
- Security lead: 40 hours
- Android lead: 60 hours  
- Developers: 120 hours total
- QA: 40 hours

**Total Estimated Effort**: 260 hours (6.5 weeks with 1 FTE)

---

## Progress Tracking

Track progress in project management tool:

- [ ] P0.1: Keystore credentials moved
- [ ] P0.2: Auth credentials secured
- [ ] P0.3: ProGuard enabled
- [ ] P1.1: DiagnosticsViewModel fixed
- [ ] P1.2: NetworkSettingsViewModel fixed
- [ ] P1.3: StorageSettingsViewModel fixed
- [ ] P1.4: MultiModalRecordingViewModel fixed
- [ ] P1.5: ThermalRecorder resource leak fixed
- [ ] P1.6: TopdonDataSourceImpl resource leak fixed
- [ ] P1.7: ThermalDao converted
- [ ] P1.8: All DAO interfaces converted
- [ ] P2.1: ErrorHandler utility created
- [ ] P2.2: !! operator usage reduced by 50%
- [ ] P2.3: Lint configuration improved
- [ ] P3.1: Singleton pattern refactored
- [ ] P3.2: Dependency injection implemented

---

## Contact & Escalation

**Technical Questions**: Android team lead
**Security Concerns**: Security team
**Schedule Conflicts**: Project manager
**Resource Needs**: Engineering manager
