# AndroidX Alternatives to Utilcode Library

## Executive Summary

This document provides AndroidX and modern Android SDK alternatives to replace the `com.blankj.utilcodex` library, eliminating hidden API warnings and modernizing the codebase.

## Current Utilcode Usage Analysis

Based on codebase analysis, the following utilcode utilities are used:

| Utilcode Utility | Usage Count | Primary Purpose | AndroidX/SDK Alternative |
|-----------------|-------------|-----------------|-------------------------|
| `SizeUtils` | 32 | Screen size/dimension conversions | `Resources.getDisplayMetrics()` + Extension functions |
| `Utils` | 26 | Application context access | Dependency injection (Hilt/Koin) or Context parameter |
| `GsonUtils` | 5 | JSON serialization | Kotlin Serialization or Moshi |
| `SPUtils` | 4 | SharedPreferences wrapper | Jetpack DataStore (Preferences) |
| `TimeUtils` | 3 | Time/date utilities | `java.time` (API 26+) or ThreeTenABP |
| `ScreenUtils` | 3 | Screen dimension utilities | `WindowMetrics` API (API 30+) or DisplayMetrics |
| `UriUtils` | 2 | URI utilities | AndroidX `DocumentFile` or standard SDK |
| `FileUtils` | 2 | File operations | `java.nio.file` (API 26+) or standard File APIs |
| Others | <2 | Various utilities | See detailed mapping below |

## Detailed Migration Guide

### 1. SizeUtils → Context-Aware Extension Functions

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.SizeUtils

val dpValue = SizeUtils.dp2px(16f)
val pxValue = SizeUtils.px2dp(48f)
val spValue = SizeUtils.sp2px(14f)
```

**AndroidX Alternative (RECOMMENDED - Context-Aware):**
```kotlin
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// For Compose UI (PREFERRED)
val Int.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }

// Usage in Composable
@Composable
fun MyComponent() {
    val size = 16.dp  // Uses LocalDensity for correct configuration
}

// For traditional Views (context-aware)
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

// Usage in Views
class MyView(context: Context) : View(context) {
    private val padding = 16.dpToPx(context)
    private val strokeWidth = 0.5f.dpToPx(context)
}
```

**Benefits:**
- ✅ Context-aware: Respects device configuration, theme, and locale
- ✅ No external dependencies
- ✅ Type-safe with Kotlin extensions
- ✅ Native Compose integration with LocalDensity
- ✅ No hidden API usage
- ✅ Correct UI rendering across different configurations

**⚠️ Warning:** Avoid `Resources.getSystem().displayMetrics` as it doesn't respect the application's current configuration and can lead to incorrect UI rendering.

### 2. Utils (Context Access) → Dependency Injection

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.Utils

val context = Utils.getApp()
```

**AndroidX Alternative - Option 1: Hilt (Recommended):**
```kotlin
// In Application class
@HiltAndroidApp
class IRCameraApp : Application()

// Provide application context
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}

// Inject where needed
@Inject
lateinit var context: Context
```

**AndroidX Alternative - Option 2: Manual Context Parameter:**
```kotlin
// Pass context as parameter instead of using global accessor
class MyUtil(private val context: Context) {
    fun doSomething() {
        // Use context
    }
}
```

**Benefits:**
- Explicit dependency management
- Testable (can inject mock context)
- Follows Android best practices
- No hidden API usage

### 3. SPUtils → Jetpack DataStore

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.SPUtils

SPUtils.getInstance().put("key", "value")
val value = SPUtils.getInstance().getString("key")
```

**AndroidX Alternative:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}

// Create DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Write data
suspend fun saveValue(key: String, value: String) {
    context.dataStore.edit { preferences ->
        preferences[stringPreferencesKey(key)] = value
    }
}

// Read data
fun getValue(key: String): Flow<String?> {
    return context.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(key)]
    }
}
```

**Benefits:**
- Type-safe
- Coroutine-based (non-blocking)
- Crash-safe (atomic updates)
- Better performance
- Officially recommended by Google

### 4. GsonUtils → Kotlin Serialization or Moshi

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.GsonUtils

val json = GsonUtils.toJson(myObject)
val obj = GsonUtils.fromJson(json, MyClass::class.java)
```

**AndroidX Alternative - Option 1: Kotlin Serialization (Recommended):**
```kotlin
// build.gradle.kts
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

// Define serializable data class
@Serializable
data class MyClass(val name: String, val age: Int)

// Serialize/Deserialize
val json = Json.encodeToString(myObject)
val obj = Json.decodeFromString<MyClass>(json)
```

**AndroidX Alternative - Option 2: Moshi:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val adapter = moshi.adapter(MyClass::class.java)
val json = adapter.toJson(myObject)
val obj = adapter.fromJson(json)
```

**Benefits:**
- Better Kotlin integration
- Type-safe
- Better performance
- No reflection (with KSP for Moshi)

### 5. TimeUtils → java.time or ThreeTenABP

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.TimeUtils

val timestamp = TimeUtils.getNowMills()
val formatted = TimeUtils.millis2String(timestamp)
```

**AndroidX Alternative - Option 1: java.time (API 26+):**
```kotlin
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val timestamp = Instant.now().toEpochMilli()
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val formatted = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    .format(formatter)
```

**AndroidX Alternative - Option 2: ThreeTenABP (API < 26):**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")
}

// Initialize in Application class
AndroidThreeTen.init(this)

// Use same API as java.time
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
// ... same as above
```

**Benefits:**
- Official Java/Android API
- Much more powerful
- Better timezone handling
- Industry standard

### 6. ScreenUtils → WindowMetrics API

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.ScreenUtils

val width = ScreenUtils.getScreenWidth()
val height = ScreenUtils.getScreenHeight()
val density = ScreenUtils.getScreenDensity()
```

**AndroidX Alternative:**
```kotlin
// API 30+
import androidx.window.layout.WindowMetricsCalculator

val windowMetrics = WindowMetricsCalculator.getOrCreate()
    .computeCurrentWindowMetrics(activity)
val bounds = windowMetrics.bounds
val width = bounds.width()
val height = bounds.height()

// For older APIs, use DisplayMetrics
val displayMetrics = resources.displayMetrics
val width = displayMetrics.widthPixels
val height = displayMetrics.heightPixels
val density = displayMetrics.density
```

**Benefits:**
- Official AndroidX API
- Multi-window and foldable support
- Better future-proofing

### 7. UriUtils → AndroidX DocumentFile

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.UriUtils

val file = UriUtils.uri2File(uri)
```

**AndroidX Alternative:**
```kotlin
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri

val documentFile = DocumentFile.fromSingleUri(context, uri)
val fileName = documentFile?.name
val fileSize = documentFile?.length()

// For file access, use ContentResolver
context.contentResolver.openInputStream(uri)?.use { inputStream ->
    // Read file
}
```

**Benefits:**
- Handles Storage Access Framework properly
- Works with all URI types
- Official AndroidX library

### 8. ImageUtils → Coil or Glide

**Current Usage:**
```kotlin
import com.blankj.utilcode.util.ImageUtils

val bitmap = ImageUtils.getBitmap(file)
```

**AndroidX Alternative - Option 1: Coil (Recommended for Compose):**
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
}

// In Compose
AsyncImage(
    model = file,
    contentDescription = null
)

// For Bitmap access
val imageLoader = ImageLoader(context)
val request = ImageRequest.Builder(context)
    .data(file)
    .build()
val result = imageLoader.execute(request)
val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
```

**Benefits:**
- Modern, Kotlin-first
- Excellent Compose integration
- Better memory management
- Coroutine-based

## Implementation Strategy

### Phase 1: Create Utility Extensions (Low Risk)
1. Create `SizeUtils.kt` with extension functions
2. Create `ContextUtils.kt` for context helpers
3. Add to `core/utils` package

### Phase 2: Migrate SharedPreferences to DataStore (Medium Risk)
1. Create DataStore implementation
2. Migrate one preference category at a time
3. Keep backwards compatibility during migration
4. Verify data migration works correctly

### Phase 3: Replace JSON Library (Medium Risk)
1. Add Kotlin Serialization plugin
2. Add `@Serializable` to data classes
3. Replace GsonUtils calls incrementally
4. Test serialization/deserialization thoroughly

### Phase 4: Update Time Handling (Low Risk)
1. Add ThreeTenABP for backwards compatibility
2. Replace TimeUtils calls
3. Consider creating extension functions for common patterns

### Phase 5: Remove Utilcode Dependency (After All Migrations)
1. Verify all imports removed
2. Remove from `build.gradle.kts`
3. Test thoroughly
4. Monitor for any runtime issues

## Migration Priority

**High Priority (Immediate):**
- `SizeUtils` → Extension functions (most used, easiest to replace)
- `Utils.getApp()` → Context parameter passing (improves architecture)

**Medium Priority (Next Sprint):**
- `SPUtils` → DataStore (modern best practice)
- `GsonUtils` → Kotlin Serialization (better integration)

**Low Priority (Future):**
- `TimeUtils` → java.time (works fine as-is)
- `ScreenUtils` → WindowMetrics (works fine as-is)
- Others (low usage, low impact)

## Sample Migration Code

Create `app/src/main/java/mpdc4gsr/core/utils/DimensionUtils.kt`:

```kotlin
package mpdc4gsr.core.utils

import android.content.res.Resources

/**
 * Modern replacement for utilcode SizeUtils
 * Provides type-safe dimension conversions without external dependencies
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.sp: Int
    get() = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

val Float.sp: Float
    get() = this * Resources.getSystem().displayMetrics.scaledDensity

// Usage:
// val widthInDp = 100.dp  // Converts 100dp to pixels
// val heightInPx = 300.px // Converts 300px to dp
```

## Benefits of Migration

1. **No Hidden API Warnings**: Eliminates Play Store compliance concerns
2. **Better Performance**: Modern libraries are more optimized
3. **Type Safety**: Kotlin-first libraries provide better type safety
4. **Maintainability**: Using official Android libraries ensures long-term support
5. **Modern Best Practices**: Aligns with current Android development standards
6. **Testing**: Dependency injection makes code more testable
7. **Compose Ready**: Many alternatives have first-class Compose support

## Risks and Mitigation

| Risk | Mitigation |
|------|-----------|
| Breaking existing functionality | Incremental migration with thorough testing |
| Data loss from SPUtils → DataStore | Implement migration code to copy existing preferences |
| Performance regression | Benchmark before and after |
| Increased binary size | Use ProGuard/R8 to optimize; modern libraries are well-optimized |

## Conclusion

While utilcode is functional, migrating to AndroidX and modern alternatives provides:
- ✅ Elimination of hidden API warnings
- ✅ Better long-term maintainability
- ✅ Improved performance
- ✅ Modern architecture alignment
- ✅ Better Compose integration

**Recommendation**: Begin migration with high-priority items (SizeUtils and Utils.getApp()) in the next development cycle. The migration can be done incrementally without disrupting existing functionality.
