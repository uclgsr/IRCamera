# Utilcode Library Analysis and Hidden API Usage

## Current Status

**Library Version**: `com.blankj:utilcodex:1.31.1` (Latest available version)

**Status**: ✅ Already at latest version - No update needed

## Analysis of Hidden API Warnings

The ANR logs showed warnings about hidden API access from `com.blankj.utilcode`:

```
hiddenapi: Accessing hidden field ... from Lcom/blankj/utilcode/util/KeyboardUtils; ... using reflection: denied
```

### Investigation Results

1. **KeyboardUtils NOT Used**: After comprehensive code analysis, `KeyboardUtils` is **not actually used** anywhere in the application codebase.

2. **Library Usage**: The application uses utilcode for:
   - `SizeUtils` - Screen size and dimension conversions
   - `Utils` - Application context access
   - `SPUtils` - SharedPreferences wrapper
   - `TimeUtils` - Time/date utilities
   - `ImageUtils` - Image processing utilities
   - `GsonUtils` - JSON serialization
   - `ScreenUtils` - Screen dimension utilities
   - `LanguageUtils` - Localization support

3. **Hidden API Impact**: While the library attempts to access hidden APIs (likely for backwards compatibility), this has **zero impact** on the application since:
   - KeyboardUtils class is not instantiated or used
   - The warnings are non-fatal and don't affect app functionality
   - Modern Android versions gracefully deny the access

## Usage Locations

The utilcode library is used primarily in the `libunified` module for legacy compatibility:

```
libunified/src/main/java/com/mpdc4gsr/libunified/
├── ir/utils/FileUtils.java (Utils)
├── ir/utils/TempDrawHelper.kt (SizeUtils)
├── ir/view/ZoomCaliperView.kt (SizeUtils)
├── ir/view/TemperatureView.java (SizeUtils)
├── app/db/AppDatabase.kt (Utils)
├── app/db/entity/ (Utils, TimeUtils)
├── app/socket/WebSocketProxy.kt (Utils)
├── app/utils/ (SizeUtils, ImageUtils, Utils)
└── app/comm/ (SPUtils, TimeUtils, UriUtils, GsonUtils)
```

## Recommendation: No Action Required

### Why No Changes Are Needed

1. **Latest Version**: Already using the most recent version (1.31.1)
2. **No Actual Impact**: KeyboardUtils is not used in the application
3. **Non-Critical Warnings**: The hidden API warnings are informational and don't affect functionality
4. **Legacy Compatibility**: The library provides stable utility functions for the legacy `libunified` module
5. **Risk vs Benefit**: Replacing the library would require extensive refactoring of the `libunified` module with minimal benefit

### Alternative Approach (Not Recommended)

If these warnings become problematic in the future (e.g., Play Store policy changes), consider:

1. **Migrate Specific Functions**: Replace utilcode functions one-by-one with Android SDK equivalents:
   - `SizeUtils` → Use `Resources.getDisplayMetrics()` directly
   - `SPUtils` → Use `SharedPreferences` directly or DataStore
   - `TimeUtils` → Use `java.time` package (API 26+) or ThreeTenABP
   - `ImageUtils` → Use Coil or Glide utilities

2. **Fork and Patch**: Create a custom fork without hidden API access

3. **Modern Alternatives**:
   - For size/dimension: Direct Android SDK APIs
   - For preferences: Jetpack DataStore
   - For JSON: Kotlin Serialization or Moshi
   - For time: java.time (ThreeTenABP for older APIs)

However, this would be significant work for minimal benefit since the warnings are harmless.

## Testing and Verification

### Verification Steps

1. ✅ Confirmed latest version is in use (1.31.1)
2. ✅ Verified KeyboardUtils is not imported or used anywhere
3. ✅ Checked that application functions normally despite warnings
4. ✅ Confirmed hidden API access is gracefully denied without crashes

### Build Verification

The application builds successfully with no errors related to utilcode:

```bash
./gradlew clean :app:assembleDebug
# Result: BUILD SUCCESSFUL
```

## Conclusion

**No action is required** for the utilcode library. The hidden API warnings are:
- Non-fatal
- Not caused by code we're actually using
- Properly handled by the Android framework
- Already at the latest library version

The ANR issue was caused by main thread blocking in `CameraPerformanceManager`, which has been resolved by moving frame processing to a background thread. The utilcode hidden API warnings were coincidental and unrelated to the ANR problem.

## Future Considerations

Monitor for:
- Play Store policy changes regarding hidden API usage
- New versions of utilcode that may remove hidden API access
- Migration opportunities when refactoring the `libunified` module

Until then, the current implementation is stable and production-ready.
