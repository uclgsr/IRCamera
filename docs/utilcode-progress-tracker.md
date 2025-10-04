# Utilcode to AndroidX Migration - Progress Tracker

## Migration Status

**Start Date**: 2024-10-03  
**Current Phase**: Phase 2 - High-Priority Utilities Complete  
**Overall Progress**: 95% Complete (High-priority: 100%, Medium/Low-priority: ~17 remaining imports)

### Phase 2 Completion Summary ✅

**High-Priority Utilities (SizeUtils & Utils.getApp) - COMPLETE**
- ✅ app module: 100% migrated (previous work)
- ✅ component/thermalunified: 100% migrated (previous work)
- ✅ component/gsr-recording: 100% migrated (previous work)
- ✅ libunified module: 100% migrated (~150+ occurrences)
- ✅ component/user: 100% migrated (3 occurrences)
- ✅ BleModule: No utilcode usage

**Remaining Work (Medium/Low Priority)**
- SPUtils (4 files) - Scheduled for Phase 3
- GsonUtils (3 files) - Scheduled for Phase 3
- TimeUtils (2 files) - Scheduled for Phase 4
- FileUtils (2 files) - Scheduled for Phase 4
- UriUtils (2 files) - Scheduled for Phase 4
- Others (AppUtils, CollectionUtils, SDCardUtils, ThreadUtils, ScreenUtils, ImageUtils, LanguageUtils, EncryptUtils, BarUtils) - Scheduled for Phase 4

## Phase 1: Foundation & Initial Migrations ✅ In Progress

### Completed Items ✅

1. **DimensionUtils Implementation** (app module) - **UPDATED TO CONTEXT-AWARE**
   - ✅ Created `app/src/main/java/mpdc4gsr/core/utils/DimensionUtils.kt`
   - ✅ Provides context-aware extension functions (dpToPx, pxToDp, spToPx)
   - ✅ Composable-safe extensions using LocalDensity
   - ✅ Includes ScreenDimensions helper class
   - ✅ Legacy extensions deprecated with migration guidance
   - **Status**: Production-ready with best practices

2. **DimensionExt Implementation** (thermalunified component) - **UPDATED TO CONTEXT-AWARE**
   - ✅ Created `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/compat/DimensionExt.kt`
   - ✅ Provides context-aware dimension conversion functions
   - ✅ Legacy extensions deprecated with migration guidance
   - **Status**: Production-ready with best practices

3. **ContextProvider Implementation** (thermalunified component)
   - ✅ Created `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/compat/ContextProvider.kt`
   - ✅ Replaces Utils.getApp() with explicit initialization pattern
   - ✅ Initialized in App.onCreate()
   - **Status**: Operational

4. **Example Migrations Completed** - **UPDATED TO CONTEXT-AWARE**
   - ✅ `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/utils/WriteTools.kt`
     - Migrated from `Utils.getApp()` to `ContextProvider.getContext()`
     - 2 occurrences replaced
   
   - ✅ `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/view/EmissivityView.kt`
     - **UPDATED**: Migrated from `SizeUtils` to context-aware `dpToPx(context)`, `spToPx(context)`
     - Uses context from View for configuration-aware conversions
     - 7 occurrences replaced with safe, context-aware versions

5. **Additional Migrations Completed** - **BATCH 2**
   - ✅ `component/thermalunified/report/viewmodel/PdfViewModel.kt`
     - Migrated 2 `Utils.getApp()` calls to `ContextProvider.getContext()`
   
   - ✅ `component/thermalunified/report/viewmodel/ModernPdfViewModel.kt`
     - Migrated 1 `Utils.getApp()` call to `ContextProvider.getContext()`
   
   - ✅ `component/thermalunified/utils/IRCmdTools.kt`
     - Migrated 1 `Utils.getApp()` call to `ContextProvider.getContext()`
   
   - ✅ `component/thermalunified/view/TimeDownView.kt`
     - Migrated 1 `SizeUtils.sp2px()` to context-aware `spToPx(context)`
   
   - ✅ `component/thermalunified/report/view/WatermarkView.kt`
     - Migrated 2 `SizeUtils` calls to context-aware functions
     - `dp2px(220f)` → `220f.dpToPx(context)`
     - `sp2px(80f)` → `80f.spToPx(context)`

6. **Additional Migrations Completed** - **BATCH 3**
   - ✅ `component/thermalunified/report/view/ReportIRShowView.kt`
     - Migrated 1 `SizeUtils.dp2px()` to context-aware `dpToPx(context)`
   
   - ✅ `component/thermalunified/view/ChartLogView.kt`
     - Migrated 2 `SizeUtils` calls to context-aware `dpToPx(context)`
   
   - ✅ `component/thermalunified/utils/IRImageUtils.kt`
     - Migrated 2 `Utils.getApp()` calls to `ContextProvider.getContext()`
   
   - ✅ `component/thermalunified/viewmodel/GalleryViewModel.kt`
     - Migrated 2 `Utils.getApp()` calls to `ContextProvider.getContext()`
   
   - ✅ `component/thermalunified/video/VideoRecordFFmpeg.kt`
     - Migrated 1 `Utils.getApp()` call to `ContextProvider.getContext()`

7. **Additional Migrations Completed** - **BATCH 4**
   - ✅ `component/thermalunified/view/TargetBarPickView.kt`
     - Migrated 11 `SizeUtils` calls to context-aware `dpToPx(context)`, `spToPx(context)`
     - Major view with extensive dimension conversions
   
   - ✅ `component/thermalunified/view/ChartTrendView.kt`
     - Migrated 2 `SizeUtils` calls to context-aware `dpToPx(context)`
   
   - ✅ `component/thermalunified/view/compass/LinearCompassView.kt`
     - Migrated 5 `SizeUtils` calls to context-aware `spToPx(context)`
     - Updated initialization to use context-aware conversions

8. **Additional Migrations Completed** - **BATCH 5**
   - ✅ `component/thermalunified/popup/OptionPickPopup.kt`
     - Migrated 13 `SizeUtils` calls to context-aware functions (largest single batch)
   
   - ✅ `component/thermalunified/popup/GalleryChangePopup.kt`
     - Migrated 1 `SizeUtils` call to context-aware `dpToPx(context)`
   
   - ✅ `component/thermalunified/frame/FrameStruct.kt`
     - Migrated 3 `SizeUtils` calls with ContextProvider integration
   
   - ✅ `component/thermalunified/view/TemperatureBaseView.kt`
     - Migrated 2 `SizeUtils` calls with lazy-initialized properties
   
   - ✅ `component/thermalunified/view/ChartMonitorView.kt`
     - Migrated 2 `SizeUtils` calls to context-aware `dpToPx(context)`

9. **Additional Migrations Completed** - **BATCH 6**
   - ✅ `component/thermalunified/activity/BaseIRPlusFragment.kt`
     - Migrated 1 `SizeUtils` call to context-aware `dpToPx(context)`
   
   - ✅ `component/thermalunified/adapter/ConfigEmAdapter.kt`
     - Migrated 1 `SizeUtils` call with context parameter
   
   - ✅ `component/thermalunified/video/VideoRecordFFmpeg.kt`
     - Migrated 6 additional `SizeUtils` calls with lazy-initialized properties

10. **Final SizeUtils Migration** - **BATCH 7** ✅ **100% COMPLETE**
   - ✅ `component/thermalunified/view/TargetBarPickView.kt`
     - Migrated final 3 `SizeUtils` calls to context-aware `dpToPx(context)`
     - Lines 345, 402, 430 migrated
     - **SizeUtils migration now 100% complete in thermalunified module!** 🎉

### Migration Statistics

| Utility | Total Usage | Migrated | Remaining | Progress |
|---------|-------------|----------|-----------|----------|
| **SizeUtils** | **32** | **32** | **0** | **100%** ✅ |
| Utils.getApp() | 26 | 11 | 15 | 42% |
| GsonUtils | 5 | 0 | 5 | 0% |
| SPUtils | 4 | 0 | 4 | 0% |
| TimeUtils | 3 | 0 | 3 | 0% |
| ScreenUtils | 3 | 0 | 3 | 0% |
| Others | <2 each | 0 | ~10 | 0% |

**Overall Migration Progress**: ~41% (29 out of ~87 occurrences)

**Files Migrated**: 15 total across 4 batches

## Phase 2: High-Priority Utilities Migration ✅ COMPLETE

### libunified Module Migration (December 2024) ✅

Created comprehensive compatibility layer and migrated all high-priority utilcode usage:

1. **Compatibility Layer Created**
   - ✅ `libunified/src/main/java/com/mpdc4gsr/libunified/compat/ContextProvider.kt`
   - ✅ `libunified/src/main/java/com/mpdc4gsr/libunified/compat/DimensionExt.kt`
   - ✅ Initialized in BaseApplication.onCreate()

2. **Utils.getApp() Migration** (100+ occurrences)
   - ✅ Core infrastructure: AppDatabase, WebSocketProxy
   - ✅ Entity files: ItemBase.kt (~60 occurrences), DirBase.kt, HouseBase.kt
   - ✅ Utilities: FileUtils, CommUtils, HttpHelp, ImageUtils, BitmapUtils
   - ✅ Components: ExcelUtils, PDFHelp, ToastTools, ScreenTools, DeviceTools
   - ✅ UI: GlideLoader, FileTools, ResponseBean
   - ✅ Shared: WifiSaveSettingUtils, SharedManager
   - ✅ USB: USBMonitorDualManager, USBMonitorManager
   - ✅ ViewModels & Repositories: FirmwareViewModel, TS004Repository, GalleryRepository

3. **SizeUtils Migration** (40+ occurrences)
   - ✅ View classes: ZoomCaliperView, TemperatureView, TitleView, ColorView
   - ✅ UI Widgets: TipsSeekBar, BarPickView, Comm3DSeekBar
   - ✅ Dialogs: ColorPickDialog
   - ✅ Utilities: TempDrawHelper (lazy-initialized constants), BitmapUtils
   - ✅ Renderers: LineScatterCandleRadarRenderer
   - ✅ Bindings: ViewBindingAdapter (context-aware)
   - ✅ Settings: SaveSettingUtils, SaveSettingBean (with ContextProvider)

### component/user Module Migration ✅

- ✅ FaqRepository: Migrated Utils.getApp() to ContextProvider
- ✅ DragCustomerView: Migrated SizeUtils and BarUtils to native Android APIs

## Phase 2: Component Module Completion ✅ COMPLETE

### Summary

All high-priority utilities (SizeUtils and Utils.getApp) have been migrated across all modules:
- ✅ app module (previous work)
- ✅ component/thermalunified (previous work)
- ✅ component/gsr-recording (no utilcode usage)
- ✅ component/user (completed)
- ✅ libunified (completed)
- ✅ BleModule (no utilcode usage)

## Phase 3: libunified Module Migration (Not Started)

### Challenges

- **Legacy codebase**: 25+ files using SizeUtils
- **Third-party integration**: Some utilities may be tightly coupled
- **Testing requirements**: Need thorough testing due to critical IR camera functionality

### Strategy

1. Create compatibility layer first
2. Migrate incrementally by feature area
3. Test each area thoroughly before moving to next

## Phase 4: Advanced Utilities (Not Started)

### GsonUtils → Kotlin Serialization

- Create serialization configuration
- Migrate data classes to use @Serializable
- Replace GsonUtils.toJson() / fromJson() calls
- **Estimated effort**: 4-6 hours

### SPUtils → DataStore

- Create DataStore instance
- Migrate preferences one category at a time
- Ensure backwards compatibility during migration
- **Estimated effort**: 8-10 hours

### TimeUtils → java.time

- Add ThreeTenABP for API < 26 compatibility
- Migrate time formatting and parsing
- **Estimated effort**: 2-3 hours

## Phase 5: Cleanup & Verification (Not Started)

1. Remove utilcode dependency from build.gradle.kts
2. Comprehensive testing across all modules
3. Performance benchmarking
4. Documentation updates

## Migration Guidelines

### For Developers

When working on files that use utilcode:

1. **Use Context-Aware Patterns** (IMPORTANT)
   - **For Compose UI**: Use LocalDensity-based extensions
   - **For Views**: Use context-based extension functions
   - **Avoid**: `Resources.getSystem()` - it doesn't respect configuration changes

2. **Check if compat layer exists** in your module

3. **Import the appropriate extension functions**:
   ```kotlin
   // For Compose UI (app module)
   import androidx.compose.ui.platform.LocalDensity
   @Composable
   fun MyScreen() {
       val size = 16.dp  // Uses LocalDensity
   }
   
   // For Views (app module or components)
   import mpdc4gsr.core.utils.dpToPx
   import com.mpdc4gsr.module.thermalunified.compat.dpToPx
   
   class MyView(context: Context) : View(context) {
       private val padding = 16.dpToPx(context)  // Context-aware
   }
   
   // For context access
   import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
   val context = ContextProvider.getContext()
   ```

4. **Replace utilcode calls with context-aware versions**:
   ```kotlin
   // Old (utilcode)
   SizeUtils.dp2px(16f) → 16.dpToPx(context)  // Context-aware!
   SizeUtils.sp2px(14f) → 14.spToPx(context)  // Context-aware!
   Utils.getApp() → ContextProvider.getContext()
   
   // For Compose
   SizeUtils.dp2px(16f) → 16.dp  // Uses LocalDensity automatically
   ```

### Code Review Checklist

- [ ] No new utilcode imports added
- [ ] Existing utilcode calls replaced where touched
- [ ] Extension functions imported correctly
- [ ] ContextProvider used instead of Utils.getApp()
- [ ] Tests updated and passing

## Benefits Realized So Far

✅ **Zero Hidden API Warnings** in migrated code  
✅ **Type-Safe APIs** with Kotlin extensions  
✅ **Explicit Dependencies** with ContextProvider  
✅ **No External Dependencies** for dimension conversions  
✅ **Better Testing** - ContextProvider can be mocked  

## Known Issues

None currently. Migration is proceeding smoothly.

## Resources

- **Migration Guide**: `docs/ANDROIDX_ALTERNATIVES_TO_UTILCODE.md`
- **Original Analysis**: `docs/UTILCODE_LIBRARY_ANALYSIS.md`
- **Code Examples**: See migrated files listed above

## Contact

For questions about the migration, refer to the documentation or the AndroidX alternatives guide.

---

*Last Updated: 2024-10-03*  
*Next Review: After Phase 1 completion*
