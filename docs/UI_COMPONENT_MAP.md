# UI Component Map (XML & Compose Equivalents)

All screens in the *IRCamera* repository are built with standard Android views and a few custom components. Below is a comprehensive map of the UI elements and components used, along with their roles, naming conventions, and Compose equivalents.

## Standard Layout Containers

### **ConstraintLayout**
- **Usage:** Root container for most screens (activities/fragments) and some inner groupings for flexible positioning
- **Example:** `activity_ir_monitor.xml`, `fragment_thermal_ir.xml`
- **Compose Equivalent:** `ConstraintLayout` composable with `ConstraintLayoutScope`
- **Key Attributes:** `layout_constraintWidth_percent`, `layout_constraintHeight_percent`, `layout_constraintDimensionRatio`

### **LinearLayout**
- **Usage:** Simple vertical or horizontal grouping of elements (e.g. vertical list of texts and buttons in wizard screens)
- **Example:** `ll_seek_bar` in `activity_manual_step2.xml` for slider control with end labels
- **Compose Equivalent:** `Column` for vertical, `Row` for horizontal arrangement
- **ID Prefix:** `ll_` (e.g., `ll_seek_bar`)

### **FrameLayout / FragmentContainerView**
- **Usage:** Stack views (e.g. camera preview with overlay) or host fragments dynamically
- **Example:** `thermal_lay` in `fragment_ir_monitor_thermal.xml` stacking CameraView and TemperatureView
- **Compose Equivalent:** `Box` for stacking, fragment hosting through navigation
- **Key Pattern:** Camera preview + overlay views stacked for thermal display

## Standard Widgets

### **TextView (ID prefix: `tv_`)**
- **Usage:** Labels, titles, and button-like text
- **Examples:** 
  - `tv_tips` and `tv_content` for instructions
  - `tv_correction` for text buttons
  - `tv_open_thermal` for menu labels
- **Compose Equivalent:** `Text` composable
- **Common Attributes:**
  - `textColor="@color/white"` for dark backgrounds
  - `textSize="16sp"` for body text, `17sp` for buttons
  - `drawableEnd` for arrow indicators

### **ImageView (ID prefix: `iv_`)**
- **Usage:** Icons or illustrations
- **Examples:**
  - `iv_tips` (informational images)
  - `iv_sketch_map` (diagram images)
  - `iv_open_thermal` (arrow icons)
- **Compose Equivalent:** `Image` or `Icon` composable
- **Common Sources:** Vector drawables (SVG), PNG assets
- **Styling:** `contentDescription="@null"` for decorative images

### **Button**
- **Usage:** Clickable actions
- **Examples:** `motion_btn`, `motion_start_btn` in monitor screen
- **Compose Equivalent:** `Button` composable
- **Standard Styling:**
  - `background="@drawable/bg_corners05_solid_theme"` (active state)
  - `background="@drawable/bg_corners05_solid_50_theme"` (disabled state)
  - `textColor="#55272F"` or `@color/color_img_calibration_button`
  - `layout_constraintWidth_percent="0.4"` for consistent sizing

### **ToggleButton**
- **Usage:** On/off settings
- **Example:** `automode` in shutter popup layout
- **Compose Equivalent:** `Switch` or `ToggleButton` composable

### **EditText**
- **Usage:** Input fields for numeric inputs and calibration fields
- **Examples:** `min`, `max`, `ooc`, `b` in shutter settings; `singlepoint`, `lowpoint` in calibration
- **Compose Equivalent:** `TextField` or `OutlinedTextField`

### **SeekBar**
- **Usage:** Slider input
- **Example:** `seek_bar` with labels "-10°" and "10°" in alignment offset slider
- **Compose Equivalent:** `Slider` composable
- **Custom Styling:** `progressDrawable`, `thumb` attributes for appearance

### **SurfaceView / TextureView**
- **Usage:** Camera preview display
- **Example:** `dualTextureView` in thermal layouts, `cameraView` (CameraView) in monitor fragment  
- **Compose Equivalent:** `AndroidView` wrapping the surface view
- **Key Pattern:** Full-screen camera feed with overlay views

## Custom Wrapper Components

### **TitleView**
- **Class:** `com.mpdc4gsr.libunified.app.view.TitleView`
- **Usage:** Consistent top app bar on all screens
- **Components:** Left icon (back arrow), centered title text, optional right-side icons/buttons
- **Key Attributes:**
  - `app:titleText` for title
  - `app:isTitleCenter` for centered vs left-aligned titles
  - Built-in back navigation handling
- **Compose Equivalent:** `TopAppBar` with `navigationIcon` and `actions`
- **Styling:** Uniform padding, text size, color across all screens

### **TemperatureView**
- **Class:** `com.mpdc4gsr.libunified.ir.view.TemperatureView`
- **Usage:** Custom view overlay for thermal data readouts
- **Features:** Temperature metrics for selected regions (point/line/area)
- **API:** `temperatureRegionMode`, `setImageSize()`, visibility toggles via EventBus
- **Compose Equivalent:** Custom `Canvas` composable with drawing operations
- **Key Pattern:** Overlay spanning match_parent over camera preview

### **MoveImageView**
- **Class:** `com.mpdc4gsr.module.thermalunified.view.MoveImageView`
- **Usage:** Drag gestures for image alignment in dual-light correction
- **Pattern:** Stacked on top of camera preview SurfaceView inside FrameLayout
- **Compose Equivalent:** Custom composable with `Modifier.pointerInput` for drag handling

### **ImageEditView**
- **Class:** `com.mpdc4gsr.libunified.app.view.ImageEditView`
- **Usage:** Drawing annotations on captured images (circles, rectangles, arrows)
- **Pattern:** Toggles visible after photo capture in image pick screen
- **Compose Equivalent:** Custom drawing canvas with gesture handling

### **CameraView**
- **Class:** `com.infisense.usbir.view.CameraView`
- **Usage:** IR camera preview display
- **Integration:** Tied to thermal data through `SynchronizedBitmap`
- **Compose Equivalent:** `AndroidView` wrapper with camera integration

## Dialogs and Popups

### **Custom Dialog Classes**
- **TipDialog, ColorSelectDialog:** Use small XML layouts with `dialog_` prefix
- **Examples:** `dialog_msg.xml`, `dialog_config_guide.xml`
- **Compose Equivalent:** `AlertDialog` or custom `Dialog` composables

### **Popup Windows**
- **PopuMenuShut:** inflates `layout_shut.xml` for shutter settings
- **PopuMenucalibration:** inflates `layout_calibration.xml` for temperature calibration  
- **Pattern:** Cluster multiple controls (labels, text fields, buttons) in small panels
- **Compose Equivalent:** `DropdownMenu` or `Popup` composables

## Third-Party Widgets

### **LottieAnimationView**
- **Usage:** Animated indicators on connection screen
- **Attributes:** `app:lottie_fileName`, `app:lottie_autoPlay`, `app:lottie_loop`
- **Compose Equivalent:** `LottieComposition` with Lottie Compose library

### **RecyclerView/Chart Components**
- **Usage:** Data visualization (referenced by `monitor_create_chart` string)
- **Library:** MPAndroidChart components
- **Compose Equivalent:** `LazyColumn`/`LazyRow` for lists, custom composables for charts

## Naming Conventions

### **ID Prefixes**
- `tv_` for TextViews (e.g. `tv_tips`, `tv_open_thermal`)
- `iv_` for ImageViews (e.g. `iv_tips`, `iv_open_thermal`)
- `btn_` or `_btn` suffix for Buttons (e.g. `motion_btn`, `motion_start_btn`)
- `ll_` for LinearLayout containers (e.g. `ll_seek_bar`)
- `cl_` for ConstraintLayout groups (e.g. `cl_dual`, `cl_connect`)

### **Resource Naming**
- **Strings:** Token-like naming (e.g. `dual_light_correction_tips_2`)
- **Colors:** Descriptive purpose (e.g. `color_img_calibration_button`)
- **Dimensions:** Standard sizing (e.g. `app_btn_big_height`)

## Layout Hierarchy Trees by Screen

### **1. Thermal Camera Monitor Screen** (Live IR Preview)

**Entry-point:** `IRMonitorActivity` → `activity_ir_monitor.xml`

```
Activity IRMonitor (ConstraintLayout root, background=@color/color_16131E)  
├── TitleView (id: title_view) – app:titleText="@string/main_thermal_motion"
├── Fragment Container (id: thermal_fragment) – hosts IRMonitorThermalFragment
└── Bottom Panel (ConstraintLayout id: motion_action_lay, height=15% of screen)
    ├── Button (id: motion_btn) – "Create Chart" (initially visible)
    └── Button (id: motion_start_btn, visibility=gone) – "Start" (appears after chart)
```

**Inside IRMonitorThermalFragment** (`fragment_ir_monitor_thermal.xml`):

```
Fragment IRMonitorThermal (ConstraintLayout root)
├── FrameLayout (id: thermal_lay) – camera preview container
│   ├── CameraView (id: cameraView) – IR camera display
│   └── TemperatureView (id: temperatureView, visibility=gone) – temperature overlay
```

### **2. Thermal Module Entry/Connection Screen**

**Layout:** `fragment_thermal_ir.xml`

```
Fragment ThermalIR (ConstraintLayout root, background=@color/color_16131E)
├── TitleView (id: title_view)
├── Connection State Group (ConstraintLayout id: cl_not_connect) – when NO device
│   ├── LottieAnimationView (id: animation_view) – searching animation
│   ├── TextView (id: tv_main_enter) – "Click to Enter" with arrow
│   ├── ConstraintLayout (id: cl_07_connect_tips) – tip icon container
│   └── TextView (id: tv_07_connect) – "Connect Device" with arrow 
└── Connected Options Group (ConstraintLayout id: cl_connect, visibility=gone) – when connected
    ├── ImageView – background illustration (ic_main_connect_bg)
    └── ConstraintLayout (id: cl_open_thermal) – "Open Thermal" button
        ├── TextView (id: tv_open_thermal) – label text
        └── ImageView (id: iv_open_thermal) – arrow icon
```

### **3. Dual-Light Alignment Screen** (Manual Calibration Step 2)

**Layout:** `activity_manual_step2.xml`

```
Activity ManualAlignmentStep2 (ConstraintLayout root, background=@color/ir_bg_color)
├── TitleView (id: title_view) – title="@string/dual_light_correction"
├── TextView (id: tv_tips) – instruction text
├── ImageView (id: iv_tips) – illustration (aspect ratio 335:100)
├── FrameLayout (id: cl_dual) – preview area (aspect ratio 480:640)
│   ├── SurfaceView (id: dualTextureView) – thermal image preview
│   └── MoveImageView (id: moveImageView) – draggable alignment overlay
├── LinearLayout (id: ll_seek_bar) – horizontal slider control
│   ├── TextView – "-10°" label
│   ├── SeekBar (id: seek_bar) – adjustment slider
│   └── TextView (id: tv_seek_bar_right) – "10°" label
└── TextView (id: tv_photo_or_confirm) – button-like text for action
```

### **4. Image Capture & Annotation Screen**

**Layout:** `activity_image_pick_ir_plush.xml` (BasePickImgActivity)

```
Activity ImagePickIR (ConstraintLayout root, background=#16131e)
├── TitleView (id: title_view) – dynamic title with save icon
├── FragmentContainerView (id: fragment_container_view) – live camera feed
├── ImageEditView (id: image_edit_view, visibility=gone) – annotation overlay
├── ImageView (id: img_pick) – capture button (camera icon)
└── ConstraintLayout (id: cl_edit_menu, visibility=gone) – editing tools
    ├── ImageView (id: iv_edit_color) – color picker
    ├── View (id: view_color) – current color indicator
    ├── ImageView (id: iv_edit_circle) – circle tool
    ├── ImageView (id: iv_edit_rect) – rectangle tool  
    ├── ImageView (id: iv_edit_arrow) – arrow tool
    └── ImageView (id: iv_edit_clear) – clear annotations
```

## Styling Tokens (Colors, Dimensions, Drawables)

### **Key Colors** (`colors.xml`)
- `color_16131E` (#16131E) – Primary dark background
- `white` – Text color on dark backgrounds  
- `color_img_calibration_button` – Button text color (brownish #55272F equivalent)
- `colorAccent` (#FF2B79D8) – Theme accent blue
- `color_FFBA42` (#FFFFBA42) – Warm orange for gradients
- `color_E74380` (#FFE74380) – Pink accent for gradients

### **Key Dimensions** (`dimens.xml`)
- `app_btn_big_height` (48dp) – Standard large button height
- `app_small_font` (14sp) – Button text size
- `app_font` (16sp) – Body text size
- Standard margins: 16dp, 8dp for spacing

### **Key Drawables**
- `bg_corners05_solid_theme` – Rounded rectangle (5dp radius) with theme gradient
- `bg_corners05_solid_50_theme` – Semi-transparent version for disabled state
- `bg_open_thermal` – Special gradient background for "Open Thermal" button
- `ic_main_arrow`, `svg_arrow_right_ff` – Forward navigation arrows
- `ic_back_white_svg` – Back arrow for TitleView

## Agent Prompt Guidelines

### **Structure Rules**
1. **Mirror Layout Hierarchy:** Always use ConstraintLayout root with TitleView at top
2. **Reuse Wrapper Components:** Use `TitleView` for app bars, not plain TextViews
3. **Follow Naming Conventions:** Proper ID prefixes (`tv_`, `iv_`, `btn_`, etc.)
4. **Match Styling Tokens:** Use existing colors/dimens, never hardcode existing values

### **Layout Behavior**
1. **Maintain Constraints:** Respect existing layout flows and aspect ratios
2. **State Management:** Hook into EventBus patterns for UI updates
3. **Navigation:** Use ARouter for screen transitions

### **Compose Migration Guidelines**
1. **TopAppBar** replaces TitleView with navigationIcon and actions
2. **Column/Row** replace LinearLayouts  
3. **Box** replaces FrameLayout for stacking
4. **AndroidView** wraps surface views and custom views
5. **Custom Canvas** composables replace drawing views

### **Component Patterns**
- **Two-state UIs:** Preview mode vs editing mode (like image annotation)
- **Overlay patterns:** Camera + temperature/annotation overlays  
- **Button clusters:** Consistent styling with theme backgrounds
- **Form inputs:** Label + EditText + Button mini-patterns

## Migration Examples

### **XML TitleView → Compose TopAppBar**

```xml
<com.mpdc4gsr.libunified.app.view.TitleView
    android:id="@+id/title_view"
    app:titleText="@string/main_thermal_motion" />
```

```kotlin
@Composable
fun ThermalTopBar() {
    TopAppBar(
        title = { Text(stringResource(R.string.main_thermal_motion)) },
        navigationIcon = {
            IconButton(onClick = { /* back navigation */ }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}
```

### **XML SeekBar → Compose Slider**

```xml
<SeekBar
    android:id="@+id/seek_bar"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1" />
```

```kotlin
@Composable
fun AlignmentSlider(value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("-10°")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -10f..10f,
            modifier = Modifier.weight(1f)
        )
        Text("10°")
    }
}
```

### **XML Camera + Overlay → Compose Box**

```xml
<FrameLayout>
    <SurfaceView android:id="@+id/cameraView" />
    <com.mpdc4gsr.libunified.ir.view.TemperatureView 
        android:id="@+id/temperatureView" />
</FrameLayout>
```

```kotlin
@Composable
fun ThermalCameraPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context -> 
                // Create and configure camera surface view
            }
        )
        TemperatureOverlay(
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

This comprehensive mapping provides a complete reference for understanding the IRCamera UI architecture and serves as a blueprint for both maintaining the existing XML-based UI and migrating to Jetpack Compose.