# Deprecated Java Patterns Analysis

## Overview

This document identifies deprecated Java patterns and Android API usage in the IRCamera repository that should be
migrated to modern Kotlin/AndroidX alternatives.

## Summary Statistics

- Total Java files in repository: **408**
- Java files with `@Deprecated` annotations: **30+**
- Java files with `@SuppressWarnings("deprecation")`: **2**

## Priority 1: Critical Deprecated Android APIs

### 1. Resources.updateConfiguration() - **HIGH PRIORITY**

**Location**: `libunified/src/main/java/com/mpdc4gsr/libunified/app/tools/AppLanguageUtils.java`

**Current Implementation**:

```java
@SuppressWarnings("deprecation")
public static void changeAppLanguage(Context context, String newLanguage) {
    Resources resources = context.getResources();
    Configuration configuration = resources.getConfiguration();
    Locale locale = Locale.ENGLISH;
    configuration.setLocale(locale);
    DisplayMetrics dm = resources.getDisplayMetrics();
    resources.updateConfiguration(configuration, dm);  // DEPRECATED
}
```

**Issue**: `Resources.updateConfiguration()` deprecated in API 25+

**Recommended Migration**:

```kotlin
fun changeAppLanguage(context: Context, newLanguage: String) {
    val locale = Locale.ENGLISH
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
}
```

**Benefits**:

- Uses modern Configuration Context API
- Properly handles multi-window and split-screen scenarios
- No deprecation warnings

---

### 2. Resources.getDrawable() without theme - **MEDIUM PRIORITY**

**Locations**:

- `libunified/src/main/java/com/mpdc4gsr/libunified/ui/components/MarkerImage.java`
- `libunified/src/main/java/com/mpdc4gsr/libunified/ui/widget/seekbar/Utils.java`
- `libunified/src/main/java/com/mpdc4gsr/libunified/ui/widget/seekbar/SeekBar.java`

**Current Pattern**:

```java
// API 21 approach - doesn't respect theme
mDrawable = mContext.getResources().getDrawable(drawableResourceId);

// API 21+ approach - still deprecated
mDrawable = mContext.getResources().getDrawable(drawableResourceId, null);
```

**Issue**: Both variants are deprecated. The first doesn't respect themes; the second is still deprecated.

**Recommended Migration**:

```kotlin
// Use ResourcesCompat for backward compatibility
import androidx.core.content.res.ResourcesCompat
val drawable = ResourcesCompat.getDrawable(context.resources, drawableResourceId, context.theme)
```

**Benefits**:

- Properly respects application theme
- Backward compatible to API 14
- No deprecation warnings
- Part of AndroidX

---

## Priority 2: Library-Internal Deprecations

### 3. Chart Library Deprecations - ✅ **ANALYSIS COMPLETE - NO ACTION REQUIRED**

**Locations**: Multiple files in `libunified/src/main/java/com/mpdc4gsr/libunified/ui/`

The chart library (appears to be MPAndroidChart fork) contains many `@Deprecated` annotations:

- `ui/interfaces/datasets/ILineDataSet.java` - deprecated methods (`isDrawCubicEnabled()`, `isDrawSteppedEnabled()`)
- `ui/components/AxisBase.java` - deprecated methods (`setAxisMinValue()`, `setAxisMaxValue()`)
- `ui/components/YAxis.java` - deprecated methods (`setStartAtZero()`, `isUseAutoScaleMinRestriction()`, etc.)
- `ui/data/LineDataSet.java` - deprecated methods (`getCircleSize()`, `setCircleSize()`, `isDrawCubicEnabled()`, etc.)
- `ui/formatter/IValueFormatter.java` - deprecated interface (replaced by `ValueFormatter` abstract class)
- `ui/formatter/IAxisValueFormatter.java` - deprecated interface (replaced by `ValueFormatter` abstract class)

**Status**: ✅ **VERIFIED - Already Using Modern APIs**

**Analysis Results**:

- Comprehensive codebase search confirms **ZERO usage** of deprecated methods
- All chart code already uses modern APIs:
    - `dataSet.getMode() == LineDataSet.Mode.STEPPED` instead of `isDrawSteppedEnabled()`
    - `setAxisMinimum()` / `setAxisMaximum()` instead of `setAxisMinValue()` / `setAxisMaxValue()`
    - `getCircleRadius()` / `setCircleRadius()` instead of `getCircleSize()` / `setCircleSize()`
- No direct implementations of deprecated interfaces (`IValueFormatter`, `IAxisValueFormatter`)
- All code extends `ValueFormatter` abstract class (modern API)

**Conclusion**: These deprecations exist only for backward compatibility within the library itself. The application code
follows best practices. No migration needed.

---

### 4. BLE Module Deprecations - ✅ **ANALYSIS COMPLETE - NO ACTION REQUIRED**

**Location**: `BleModule/src/main/java/com/topdon/ble/callback/ScanListener.java`

Deprecated method: `onScanResult(Device)` → Modern: `onScanResult(Device, boolean)`

**Status**: ✅ **VERIFIED - Not Used**

**Analysis Results**:

- No implementations of `ScanListener` found in application code
- Deprecated method has default implementation (no breaking changes)
- Module appears to be internal library

**Conclusion**: No action required. The deprecated method is not actively used.

---

## Priority 3: Potential Improvements (Not Deprecated Yet)

### 5. Consider Kotlin Coroutines Migration

Many Java classes could benefit from Kotlin coroutines instead of callback patterns:

- BLE callback patterns
- Threading utilities
- Async operations

### 6. Consider AndroidX Preferences Migration

If using old preference APIs, migrate to AndroidX Preference library.

---

## Migration Strategy

### Phase 1: Critical Android API Deprecations (1-2 days)

1. ✅ **COMPLETED**: Migrate `scaledDensity` to `TypedValue.applyDimension()` (PR #560)
2. ✅ **COMPLETED**: Migrate `Resources.updateConfiguration()` to `createConfigurationContext()` (this PR)
3. ✅ **COMPLETED**: Migrate `Resources.getDrawable()` to `ResourcesCompat.getDrawable()` (this PR)

### Phase 2: Library-Specific Deprecations (Completed)

1. ✅ **ANALYSIS COMPLETE**: Chart library deprecated methods are NOT used in application code
2. ✅ **ANALYSIS COMPLETE**: BLE module deprecated methods are NOT used in application code
3. ✅ **VERIFIED**: All chart code already uses modern APIs (getMode(), setCircleRadius(), etc.)

**Finding**: All library-internal deprecations exist only for backward compatibility. The codebase already follows best
practices and uses modern APIs throughout. No code changes required.

### Phase 3: Kotlin Migration (Optional, Long-term)

1. Convert utility classes to Kotlin
2. Migrate callback patterns to coroutines
3. Modernize architecture patterns

---

## Detailed File Inventory

### Files with @Deprecated Annotations (30+)

```
BleModule/src/main/java/com/topdon/ble/callback/ScanListener.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/utils/Utils.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/interfaces/datasets/ILineDataSet.java (2 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/components/AxisBase.java (2 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/components/YAxis.java (5 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/data/RadarEntry.java (2 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/data/LineDataSet.java (4 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/data/PieEntry.java (2 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/data/BarEntry.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/data/CombinedData.java (3 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/formatter/IValueFormatter.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/formatter/ValueFormatter.java (2 methods)
libunified/src/main/java/com/mpdc4gsr/libunified/ui/formatter/IAxisValueFormatter.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/charts/PieChart.java
```

### Files with @SuppressWarnings("deprecation") (2)

```
libunified/src/main/java/com/mpdc4gsr/libunified/app/tools/AppLanguageUtils.java
libunified/src/main/java/com/mpdc4gsr/libunified/ui/utils/Utils.java
```

---

## Next Steps

1. **Create Issue**: Create a GitHub issue for each priority level
2. **Create PRs**: Create separate PRs for:
    - Priority 1 items (Android API deprecations)
    - Priority 2 items (Library deprecations)
3. **Review**: Get code review and testing for each migration
4. **Document**: Update migration documentation

---

## Testing Recommendations

For each migration:

1. **Build Test**: Ensure project builds without new warnings
2. **Unit Tests**: Add/update unit tests for migrated code
3. **Integration Tests**: Test affected features end-to-end
4. **Manual Testing**: Test on different Android versions (especially API 21-34)
5. **Regression Testing**: Ensure existing functionality still works

---

## References

- [Android Deprecation Policy](https://developer.android.com/about/versions)
- [AndroidX Migration Guide](https://developer.android.com/jetpack/androidx/migrate)
- [Kotlin Migration Guide](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
- [TypedValue Documentation](https://developer.android.com/reference/android/util/TypedValue)

---

## Phase 2 Completion Summary

**Status**: ✅ **COMPLETE**  
**Date Completed**: 2025-01-03

### Work Performed

1. Comprehensive analysis of all library-internal deprecations
2. Searched entire codebase for usage of deprecated methods and interfaces
3. Verified that application code already uses modern APIs
4. Documented findings and recommendations

### Key Findings

- **Chart Library**: All deprecated methods exist only for backward compatibility. Application code already uses modern
  alternatives (getMode(), setCircleRadius(), setAxisMinimum(), ValueFormatter class, etc.)
- **BLE Module**: Deprecated ScanListener method is not implemented anywhere in the codebase
- **Utils.java**: Application correctly uses Utils.init(Context) instead of deprecated Utils.init(Resources)

### Conclusion

No code changes required for Phase 2. The codebase demonstrates best practices by already using modern APIs throughout.
The deprecated methods serve only as backward compatibility layer within the libraries themselves.

---

**Document Version**: 2.0  
**Date**: 2025-01-03  
**Author**: GitHub Copilot  
**Related PRs**: #560 (Phase 1 - scaledDensity), #[current PR] (Phase 1 - updateConfiguration/getDrawable + Phase 2
analysis)








