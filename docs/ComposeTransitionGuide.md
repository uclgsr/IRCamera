# XML to Compose Transition Guide

This document tracks the transition from legacy XML-based components to Jetpack Compose.

## Transition Status

### Completed Transitions

All major components have Compose equivalents available in the codebase.

#### Activities

- ✅ ThermalActivity → ThermalComposeActivity
- ✅ ThermalCameraActivity → ThermalCameraComposeActivity
- ✅ ThermalGalleryActivity → ThermalGalleryComposeActivity
- ✅ ThermalMonitoringActivity → ThermalMonitoringComposeActivity
- ✅ ThermalVideoActivity → ThermalVideoComposeActivity
- ✅ IRThermalDoubleActivity → IRThermalDoubleComposeActivity
- ✅ IRThermalNightActivity → IRThermalNightComposeActivity
- ✅ IRThermalPlusActivity → IRThermalPlusComposeActivity
- ✅ IRCameraSettingActivity → IRCameraSettingComposeActivity

#### Dialogs

- ✅ IRConfigInputDialog → IRConfigInputDialogCompose (component/thermalunified/compose/)
- ✅ ConfigGuideDialog → ConfigGuideDialogCompose (component/thermalunified/compose/)
- ✅ HomeGuideDialog → HomeGuideDialogCompose (component/thermalunified/compose/)
- ✅ ThermalInputDialog → ThermalInputDialogCompose (component/thermalunified/compose/)
- ✅ All libunified dialogs transitioned (see docs/ComposeGuide.md)

#### UI Components

- ✅ MenuFirstTabView → MenuCompose.kt components
- ✅ view_camera_menu.xml → CameraMenuView (libunified/app/compose/components/MenuViewsCompose.kt)
- ✅ view_menu_edit.xml → MenuEditView (libunified/app/compose/components/MenuViewsCompose.kt)
- ✅ toolbar_lay.xml → TopAppBar in Compose activities

### Legacy XML Components (Deprecated)

The following XML layouts are deprecated and should not be used in new code:

#### Layouts

- ❌ libunified/src/main/res/layout/toolbar_lay.xml
    - **Replacement**: Use Material3 `TopAppBar` with `navigationIcon`
    - **Usage**: All Compose activities use TopAppBar directly

- ❌ libunified/src/main/res/layout/view_camera_menu.xml
    - **Replacement**: `CameraMenuView()` in MenuViewsCompose.kt
    - **How to use**: Use Compose function with action/gallery/more icons

- ❌ libunified/src/main/res/layout/dialog_thermal_input.xml
    - **Replacement**: `ThermalInputDialogCompose()`
    - **How to use**: Use Compose dialog with temperature/color pickers

- ❌ component/thermalunified/res/layout/dialog_ir_config_input.xml
    - **Replacement**: `IRConfigInputDialogCompose()`
    - **How to use**: Use Compose dialog with type parameter

- ❌ component/thermalunified/res/layout/dialog_config_guide.xml
    - **Replacement**: `ConfigGuideDialogCompose()`

- ❌ component/thermalunified/res/layout/dialog_home_guide.xml
    - **Replacement**: `HomeGuideDialogCompose()`

#### Drawable Resources (XML-only)

The following drawables were created for XML layout compatibility but are not needed in Compose:

- libunified/src/main/res/drawable/ui_dialog_input_press_bg.xml
    - Compose uses: Modifier.background() with Color

- libunified/src/main/res/drawable/ic_back_white_svg.xml
    - Compose uses: Icons.AutoMirrored.Filled.ArrowBack

- libunified/src/main/res/drawable/svg_camera_photo_normal.xml
    - Compose uses: Box() with CircleShape and background color

- libunified/src/main/res/drawable/shape_oval_33.xml
    - Compose uses: Modifier.clip(CircleShape).background()

## Transition Steps

### For Activities

1. Change base class from `BaseActivity`/`BaseBindingActivity` to `BaseComposeActivity`
2. Override `Content()` composable function instead of `onCreate()`
3. Replace XML toolbar with Material3 `TopAppBar`
4. Use Compose UI components instead of XML views
5. Remove `setContentView()` and XML layout references

**Example:**

```kotlin
// Old XML-based
class MyActivity : BaseBindingActivity<ActivityMyBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}

// New Compose
class MyComposeActivity : BaseComposeActivity<MyViewModel>() {
    @Composable
    override fun Content(viewModel: MyViewModel) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Activity") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Your compose UI
        }
    }
}
```

### For Dialogs

1. Create composable function instead of Dialog class
2. Use Material3 Dialog with `DialogProperties`
3. Replace XML views with Compose components
4. Handle state with `remember` and `mutableStateOf`

**Example:**

```kotlin
// Old XML-based
class MyDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.dialog_my)
        // ... findViewById, click listeners
    }
}

// New Compose
@Composable
fun MyDialogCompose(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            // Your compose UI
        }
    }
}
```

### For Custom Views

1. Replace custom View classes with `@Composable` functions
2. Use Compose modifiers instead of XML attributes
3. Handle clicks with `Modifier.clickable { }`
4. Use `remember` for state management

## Benefits of Transition

- ✅ **Less boilerplate**: No need for findViewById, ViewBinding, or DataBinding
- ✅ **Better performance**: Compose optimizes recomposition automatically
- ✅ **Modern UI**: Material3 design system built-in
- ✅ **Type safety**: Compile-time checking of UI properties
- ✅ **Easier testing**: Composables are easier to unit test
- ✅ **Declarative**: UI updates automatically when state changes

## Resources

- [ComposeGuide.md](./ComposeGuide.md) - Comprehensive guide for libunified Compose components
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material3 Components](https://m3.material.io/components)
- [Compose Samples](https://github.com/android/compose-samples)

## Deprecation Timeline

- **Phase 1 (Completed)**: All Compose equivalents created
- **Phase 2 (Completed)**: Mark XML components as @Deprecated
- **Phase 3 (In Progress)**: Remove deprecated dialog classes with Compose equivalents
    - ✅ Removed: ConfigGuideDialog, HomeGuideDialog, IRConfigInputDialog
    - ✅ Removed: ColorSelectDialog, ConfirmSelectDialog, EmissivityTipPopup
    - ✅ Removed: FirmwareUpDialog, LongTextDialog, NotTipsSelectDialog
    - ✅ Removed: TipChangeDeviceDialog, TipEmissivityDialog, TipObserveDialog
    - ✅ Removed: TipOtgDialog, TipProgressDialog, TipShutterDialog
    - ✅ Removed: TipTargetColorDialog, TipWaterMarkDialog, DownloadProDialog
    - ⏳ Remaining: Legacy base classes still using LoadingDialog, MsgDialog, TipDialog, TipCameraProgressDialog,
      CarDetectDialog
- **Phase 4 (Future)**: Remove XML layouts and legacy drawable resources
- **Phase 5 (Final)**: Complete transition, XML system removed

## Notes

- Keep XML layouts for backward compatibility temporarily
- New features should only use Compose
- Gradual transition of existing features encouraged
- Test thoroughly when transitioning critical paths
