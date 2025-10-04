# Utilcode Migration - Next Steps

## Status: Phase 2 Complete

All high-priority utilcode utilities (SizeUtils and Utils.getApp) have been successfully migrated to AndroidX alternatives across the entire codebase.

## Remaining Work

### Phase 3: Medium-Priority Utilities

#### 1. SPUtils (4 files) - Estimated: 6-8 hours

**Files:**
- `libunified/app/comm/bean/SaveSettingBean.kt`
- `libunified/app/common/WifiSaveSettingUtils.kt`
- `libunified/app/common/SharedManager.kt`
- `libunified/app/common/SaveSettingUtils.kt`

**Migration Strategy:**
```kotlin
// Option 1: Migrate to DataStore (Recommended)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Option 2: Use SharedPreferences directly (Quick)
val sharedPrefs = context.getSharedPreferences("name", Context.MODE_PRIVATE)
```

**Considerations:**
- DataStore is the modern, recommended approach
- Provides type-safety and coroutine support
- Need to handle data migration from existing SharedPreferences
- May require significant refactoring of existing code

#### 2. GsonUtils (3 files) - Estimated: 4-6 hours

**Files:**
- `libunified/app/lms/network/ResponseBean.java`
- `libunified/app/common/SharedManager.kt`
- `libunified/app/common/SaveSettingUtils.kt`

**Migration Strategy:**
```kotlin
// Already using Gson in dependencies, just import directly
import com.google.gson.Gson

val gson = Gson()
val json = gson.toJson(myObject)
val obj = gson.fromJson(json, MyClass::class.java)
```

**Considerations:**
- Gson is already a project dependency (via Retrofit)
- Simple replacement: Use Gson directly instead of through GsonUtils
- No functional changes required

#### 3. TimeUtils (2 files) - Estimated: 2-3 hours

**Files:**
- `libunified/app/db/entity/HouseBase.kt`
- `libunified/app/comm/ExcelUtils.java`

**Migration Strategy:**
```kotlin
// Use java.time with desugaring (already enabled)
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val timestamp = System.currentTimeMillis()
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val formatted = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(timestamp), 
    ZoneId.systemDefault()
).format(formatter)
```

**Considerations:**
- Core library desugaring already enabled in build.gradle
- java.time is the standard Java API
- More powerful and flexible than TimeUtils

### Phase 4: Low-Priority Utilities - Estimated: 6-8 hours

#### Utilities to Migrate:

1. **FileUtils** (2 files)
   - Use standard Java File APIs or java.nio.file

2. **UriUtils** (2 files)
   - Use AndroidX DocumentFile or standard ContentResolver

3. **ScreenUtils** (2 files)
   - Use context.resources.displayMetrics or WindowMetrics

4. **ImageUtils** (1 file - libunified/app/utils/ImageUtils.kt)
   - Use Coil or Glide utilities directly

5. **LanguageUtils** (1 file - libunified/app/BaseApplication.kt)
   - Use standard Android locale APIs

6. **EncryptUtils** (1 file)
   - Use standard Java security APIs

7. **Others** (thermalunified component)
   - AppUtils, CollectionUtils, SDCardUtils, ThreadUtils, StringUtils
   - Each can be replaced with standard Kotlin/Java/Android APIs

### Phase 5: Cleanup & Verification - Estimated: 2-3 hours

1. **Remove utilcode dependency**
   ```kotlin
   // Remove from these files:
   - BleModule/build.gradle.kts: api("com.blankj:utilcodex:1.31.1")
   - libunified/build.gradle.kts: api(libs.utilcode)
   - component/thermalunified/build.gradle.kts: implementation(libs.utilcode)
   - component/user/build.gradle.kts: implementation(libs.utilcode)
   ```

2. **Verify no remaining imports**
   ```bash
   grep -r "import com.blankj.utilcode" --include="*.kt" --include="*.java"
   ```

3. **Run full build and tests**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

4. **Update documentation**
   - Mark migration as complete in UTILCODE_PROGRESS_TRACKER.md
   - Update ANDROIDX_ALTERNATIVES_TO_UTILCODE.md with lessons learned

## Recommended Approach

### Quick Win Strategy (Recommended)

Focus on replacing utilities that require minimal code changes:

1. **GsonUtils** (4-6 hours) - Just import Gson directly
2. **TimeUtils** (2-3 hours) - Standard java.time API
3. **Low-priority utilities** (6-8 hours) - Replace one-by-one
4. **SPUtils** (6-8 hours) - More complex, do last
5. **Cleanup** (2-3 hours)

**Total Estimated Time: 20-28 hours**

### Benefits Upon Completion

1. ✅ No hidden API warnings
2. ✅ Reduced dependency count
3. ✅ Modern Android architecture
4. ✅ Better performance (DataStore, java.time)
5. ✅ Improved testability
6. ✅ Better Jetpack Compose integration
7. ✅ Long-term maintainability

## Migration Guidelines

### For Each Utility Replacement:

1. **Check usage context**
   - Does it have access to Context?
   - Is it in a static field?
   - Can it use ContextProvider?

2. **Find AndroidX alternative**
   - Check ANDROIDX_ALTERNATIVES_TO_UTILCODE.md
   - Search Android documentation
   - Look for Jetpack libraries

3. **Make minimal changes**
   - Replace only the utilcode usage
   - Don't refactor surrounding code
   - Keep the same behavior

4. **Test thoroughly**
   - Build the module
   - Run related tests
   - Verify UI still works

5. **Document the change**
   - Update UTILCODE_PROGRESS_TRACKER.md
   - Add comments if the replacement is non-obvious

## Contact

For questions or assistance with the migration, refer to the existing documentation:
- ANDROIDX_ALTERNATIVES_TO_UTILCODE.md - Detailed alternatives guide
- UTILCODE_PROGRESS_TRACKER.md - Progress tracking
- UTILCODE_LIBRARY_ANALYSIS.md - Original analysis
