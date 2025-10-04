# Android Anti-Patterns Prevention Checklist

Quick reference checklist for code reviews and development. See anti-patterns-analysis.md for detailed explanations.

## Pre-Commit Checklist

### Security

- [ ] No hardcoded passwords, API keys, or credentials
- [ ] No sensitive data in log statements
- [ ] ProGuard/R8 enabled for release builds
- [ ] Input validation on all user inputs
- [ ] No implicit intents for sensitive operations
- [ ] Proper permission checks before accessing protected resources

### Memory Management

- [ ] ViewModels do not hold Activity/Fragment context references
- [ ] File streams closed using `.use{}` or try-with-resources
- [ ] Cursors properly closed after use
- [ ] No static references to Context, Activity, or View
- [ ] Bitmap recycling where appropriate
- [ ] Collections cleared when no longer needed

### Threading & Concurrency

- [ ] No blocking operations on main thread
- [ ] Database operations use suspend/Flow/LiveData
- [ ] File I/O operations use Dispatchers.IO
- [ ] Heavy computations use Dispatchers.Default
- [ ] Handler created with explicit Looper
- [ ] No GlobalScope usage (use viewModelScope, lifecycleScope)
- [ ] Proper synchronization for shared mutable state

### Null Safety

- [ ] Minimize use of !! operator
- [ ] Use safe calls (?.) and Elvis operator (?:)
- [ ] Use scope functions (let, also, run) for null handling
- [ ] lateinit only for properties guaranteed to be initialized

### Resource Management

- [ ] Files opened with .use{}
- [ ] Streams properly closed in finally blocks or .use{}
- [ ] ContentProvider cursors closed
- [ ] Broadcast receivers unregistered
- [ ] Service connections unbound
- [ ] Animation listeners removed when done

### Architecture

- [ ] ViewModels extend ViewModel or AndroidViewModel
- [ ] No business logic in Activities/Fragments
- [ ] Repository pattern for data access
- [ ] Single responsibility principle followed
- [ ] Dependency injection instead of singletons

### Error Handling

- [ ] No empty catch blocks
- [ ] Exceptions logged with context
- [ ] User-facing error messages provided
- [ ] Graceful degradation on errors
- [ ] No swallowed exceptions

### Code Quality

- [ ] Lint warnings addressed
- [ ] No deprecated API usage without suppression justification
- [ ] Proper logging levels (debug/info/warning/error)
- [ ] Code comments only where necessary
- [ ] Consistent code style

## Common Anti-Patterns to Avoid

### ❌ DON'T

```kotlin
// DON'T: Store context in ViewModel
class MyViewModel : ViewModel() {
    private lateinit var context: Context  // MEMORY LEAK
}

// DON'T: Hardcode credentials
val password = "admin123"

// DON'T: Leave resources open
val writer = FileWriter(file)
writer.write("data")
// Might not be closed if exception occurs

// DON'T: Block main thread
fun loadData() {
    val data = database.query()  // Blocking call on main thread
}

// DON'T: Use !! unnecessarily
val length = someString!!.length

// DON'T: Use GlobalScope
GlobalScope.launch {
    // No lifecycle awareness
}

// DON'T: Hold static Activity reference
companion object {
    lateinit var activity: Activity
}

// DON'T: Empty catch blocks
try {
    riskyOperation()
} catch (e: Exception) {
    // Silent failure
}
```

### ✅ DO

```kotlin
// DO: Use AndroidViewModel for Application context
class MyViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
}

// DO: Use environment variables or secure storage
val password = BuildConfig.API_KEY  // From gradle.properties

// DO: Use .use{} for automatic closing
FileWriter(file).use { writer ->
    writer.write("data")
}  // Automatically closed

// DO: Use suspend functions with proper dispatcher
suspend fun loadData() = withContext(Dispatchers.IO) {
    database.query()
}

// DO: Use safe calls
val length = someString?.length ?: 0

// DO: Use appropriate coroutine scope
viewModelScope.launch {
    // Lifecycle aware
}

// DO: Use dependency injection
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
}

// DO: Handle errors properly
try {
    riskyOperation()
} catch (e: Exception) {
    Log.e(TAG, "Operation failed: ${e.message}", e)
    showErrorToUser()
}
```

## Code Review Red Flags

Look for these patterns during code reviews:

1. `lateinit var context: Context` in ViewModels
2. `!!` operator used multiple times in a row
3. `FileWriter` or `FileOutputStream` without `.use{}`
4. Passwords or API keys as string literals
5. `fun query()` in @Dao without suspend/Flow
6. `Thread.sleep()` on main thread
7. `GlobalScope.launch` anywhere
8. `companion object { lateinit var instance }`
9. Empty catch blocks `} catch (e: Exception) { }`
10. `isMinifyEnabled = false` in release builds

## Testing Checklist

- [ ] Unit tests for ViewModels
- [ ] Integration tests for data layer
- [ ] No Context required for unit tests (use Robolectric if needed)
- [ ] Mock external dependencies
- [ ] Test error handling paths
- [ ] Test edge cases and null inputs

## Build Configuration Checklist

### app/build.gradle.kts

```kotlin
android {
    // ✅ Enable proper lint checking
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
    
    buildTypes {
        release {
            // ✅ Enable obfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            
            // ✅ Use proper signing config
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### gradle.properties (DO NOT COMMIT)

```properties
# Keystore credentials (not in source control)
KEYSTORE_PASSWORD=your_password_here
KEY_ALIAS=your_alias
KEY_PASSWORD=your_key_password
```

## Performance Checklist

- [ ] No nested lists in RecyclerView
- [ ] ViewHolder pattern used correctly
- [ ] Images scaled appropriately before loading
- [ ] Database queries optimized with indexes
- [ ] No overdraw in layouts
- [ ] Proper lifecycle handling for LiveData/Flow

## Debugging Tools

Enable these in debug builds:

```kotlin
if (BuildConfig.DEBUG) {
    // Detect main thread violations
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
    
    // Detect memory leaks
    LeakCanary.install(this)
}
```

## Quick Fixes

### Fix: ViewModel Context Reference

```kotlin
// Before
class MyViewModel : ViewModel() {
    private lateinit var context: Context
    fun init(ctx: Context) { context = ctx }
}

// After
class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    // Or inject repository that needs context
}
```

### Fix: Resource Leak

```kotlin
// Before
val writer = FileWriter(file)
try {
    writer.write("data")
} finally {
    writer.close()  // Might be forgotten
}

// After
FileWriter(file).use { writer ->
    writer.write("data")
}
```

### Fix: Blocking Database Call

```kotlin
// Before
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getUsers(): List<User>  // Blocks caller thread
}

// After
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getUsers(): Flow<List<User>>  // Reactive
    
    @Query("SELECT * FROM users")
    suspend fun getUsersOnce(): List<User>  // Suspending
}
```

## Static Analysis Tools

### Detekt (Recommended)

A configuration file has been created at `detekt-config.yml` to help catch anti-patterns automatically.

To add Detekt to the project:

```kotlin
// In build.gradle.kts (root level)
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

// In app/build.gradle.kts
plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    config = files("../detekt-config.yml")
    buildUponDefaultConfig = true
    allRules = false
}
```

Run analysis:
```bash
./gradlew detekt
```

### Lint

Improved lint configuration has been applied:
- `abortOnError = true` - Fails build on errors
- `checkReleaseBuilds = true` - Checks release builds
- Only essential rules disabled with justification

## Resources

- Full analysis: `docs/anti-patterns-analysis.md`
- ANR prevention: `docs/anr-prevention-guide.md`
- Migration history: `MIGRATION_COMPLETE_SUMMARY.md`
- Detekt config: `detekt-config.yml`
