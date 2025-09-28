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

### **Auto Shutter Settings Popup** (`layout_shut.xml`)
A small ConstraintLayout panel for configuring camera shutter timing with:
- **Four numeric input fields:** "Min Interval", "Max Interval", "OOC", and "B" (temperature thresholds)
- **ToggleButton:** For Auto mode enabling/disabling
- **Layout Pattern:** One row per parameter with labels above fields using constraints
- **ID Naming:** Concise naming (`min`, `max`, `ooc`, `b` with corresponding `mintext`, etc.)
- **Usage:** Shown on long-press of shutter button, sends camera commands on field changes
- **Compose Equivalent:** `AlertDialog` with `Column` of `OutlinedTextField` and `Switch` components

### **Temperature Calibration Popup** (`layout_calibration.xml`)
A comprehensive vertical LinearLayout containing multiple calibration sections:

**Single-Point Calibration:**
- EditText input field + "Calibrate" Button

**Double-Point Calibration:**
- Two EditTexts for low/high temperature 
- Two-step submit process: `doiblepointsumit` then `endpointsumit`

**Calibration Management:**
- Cancel button to reset calibration
- Revise/Recover section with `gain` input and "Ready"/"Begin" buttons

**Bad Pixel Correction:**
- X/Y coordinate inputs + "Submit"/"Cancel" buttons for pixel management

**Configuration Management:**
- Save/Restore buttons (`savecfg`, `restorecfg`) for factory settings

**Compose Migration Pattern:**
```kotlin
@Composable
fun CalibrationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        content = {
            LazyColumn {
                item { SinglePointCalibration(...) }
                item { DoublePointCalibration(...) }
                item { BadPixelCorrection(...) }
                item { ConfigurationButtons(...) }
            }
        }
    )
}
```

## Third-Party Widgets

### **LottieAnimationView**
- **Usage:** Used on the connection screen to show animated indicators (loading or searching animation)
- **Implementation:** Shows looping animation while device connection is being established
- **Example:** `animation_view` in `fragment_thermal_ir.xml` with `TDAnimationJSON.json`
- **Attributes:** 
  - `app:lottie_fileName="TDAnimationJSON.json"`
  - `app:lottie_imageAssetsFolder="images/"`
  - `app:lottie_autoPlay="true"`
  - `app:lottie_loop="true"`
- **Compose Equivalent:** `LottieComposition` with Lottie Compose library

### **RecyclerView/Chart Components**
- **Usage:** Data visualization and list display
- **Chart Library:** MPAndroidChart components like `Legend` (referenced in `libui` module)
- **List Usage:** Device lists, session management, gallery views
- **Examples:** Chart views for temperature monitoring (`monitor_create_chart` string suggests chart integration)
- **Compose Equivalent:** 
  - `LazyColumn`/`LazyRow` for lists
  - Custom composables with `Canvas` for charts
  - Third-party chart libraries like `compose-charts`

## State Management and Behavior Patterns

### **EventBus Integration**
- **ThermalActionEvent:** Used for thermal overlay mode switching (point/line/area selection)
- **Device Events:** USB connect/disconnect handling for camera state
- **Event Codes:** Standardized action codes (e.g., 2001 for point mode, 2002 for line mode)
- **Fragment Communication:** Decoupled state flow where UI reacts to events rather than direct callbacks

### **Two-State UI Patterns**
Common pattern across multiple screens:

**Image Pick/Edit Screen:**
- **Preview State:** Fragment preview + capture button visible
- **Edit State:** Static image + annotation tools visible
- **Toggle Mechanism:** `switchPhotoState(true/false)` controls visibility

**Connection Screens:**
- **Disconnected State:** `cl_not_connect` visible with Lottie animation
- **Connected State:** `cl_connect` visible with "Open Thermal" button
- **State Switching:** Based on device connection events

**Monitor Screen:**
- **Setup State:** "Create Chart" button visible
- **Active State:** "Start" button replaces chart button

### **Overlay Patterns**
Stacking pattern used throughout the app:

**Camera + Temperature Overlay:**
```xml
<FrameLayout>
    <CameraView /> <!-- Base camera feed -->
    <TemperatureView /> <!-- Temperature data overlay -->
</FrameLayout>
```

**Camera + Alignment Overlay:**
```xml
<FrameLayout>
    <SurfaceView /> <!-- Thermal preview -->
    <MoveImageView /> <!-- Draggable alignment overlay -->
</FrameLayout>
```

**Image + Annotation Overlay:**
```xml
<ConstraintLayout>
    <FragmentContainerView /> <!-- Live preview (initial) -->
    <ImageEditView visibility="gone" /> <!-- Annotation overlay (after capture) -->
</ConstraintLayout>
```

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

### **Gradient System**
The app uses a consistent gradient theming system:

**Active State Gradient:**
```xml
<!-- bg_corners05_solid_theme equivalent -->
<gradient
    android:startColor="#FFFFBA42" 
    android:endColor="#FFE74380"
    android:angle="0" />
```

**Disabled State Gradient:**
```xml
<!-- bg_corners05_solid_50_theme equivalent -->
<gradient
    android:startColor="#80FFBA42" 
    android:endColor="#80E74380"
    android:angle="0" />
```

**Special Backgrounds:**
- `bg_open_thermal`: Orange-to-pink gradient for prominent actions
- `bg_camera_setting`: Context-specific backgrounds for camera controls
- Shape variations: `bg_corners50_solid_*` for more rounded elements

### **Icon System**
Consistent iconography across the app:

**Navigation Icons:**
- `ic_back_white_svg` – Universal back navigation
- `ic_main_arrow` – Forward/next navigation
- Arrow variants: `svg_arrow_right_ff`, `ir_svg_arrow_right_ff`

**Function Icons:**
- `ic_camera_grey_svg` – Camera/capture actions
- `ic_main_tips` – Help/information indicators
- `ic_main_connect_bg` – Connection status illustrations

**Tool Icons (Image Editing):**
- `svg_image_pick_color` – Color picker tool
- `svg_image_pick_clear` – Clear/erase tool
- `selector_image_pick_*` – State-aware tool selectors (circle, rect, arrow)

### **State-Aware Drawables**
Many drawables use selector patterns for interactive states:

```xml
<!-- Example: selector_image_pick_circle -->
<selector>
    <item android:state_selected="true" 
          android:drawable="@drawable/svg_image_pick_circle_select_yes" />
    <item android:drawable="@drawable/svg_image_pick_circle_select_not" />
</selector>
```

## Thermal Camera Specific Components

### **Temperature Region Modes**
The `TemperatureView` supports multiple measurement modes:

**Point Mode (Action Code 2001):**
- Single point temperature measurement
- Crosshair display at selected location
- Real-time temperature readout

**Line Mode (Action Code 2002):**
- Line-based temperature measurement
- Min/max temperature along the line
- Visual line indicator with endpoint markers

**Area/Rectangle Mode (Action Code 2003):**
- Rectangular region temperature analysis
- Average, min, max temperature display
- Draggable rectangle selection

**Implementation Pattern:**
```kotlin
// Event-driven mode switching
@Subscribe(threadMode = ThreadMode.MAIN)
fun action(event: ThermalActionEvent) {
    temperatureView.isEnabled = true
    when (event.action) {
        2001 -> {
            temperatureView.visibility = View.VISIBLE
            temperatureView.temperatureRegionMode = REGION_MODE_POINT
        }
        2002 -> {
            temperatureView.visibility = View.VISIBLE
            temperatureView.temperatureRegionMode = REGION_MODE_LINE
        }
        // ... additional modes
    }
}
```

### **Camera Integration Patterns**

**Dual Camera Alignment:**
For devices with both thermal and visible light cameras:
- `MoveImageView` provides drag functionality for alignment
- `SeekBar` for fine angle adjustment (-10° to +10°)
- Real-time preview overlay for alignment verification

**Thermal Data Processing:**
- `SynchronizedBitmap` mechanism for frame synchronization
- Temperature data extraction from IR sensor
- Color mapping and pseudocolor display modes

**State Persistence:**
- `isPick` flag for image capture mode vs live preview
- Device connection state management
- Temperature calibration data storage

### **Compose Migration for Thermal Components**

**TemperatureView → Custom Canvas:**
```kotlin
@Composable
fun TemperatureOverlay(
    temperatureData: TemperatureData,
    regionMode: TemperatureRegionMode,
    onRegionSelect: (Region) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (regionMode) {
            REGION_MODE_POINT -> drawPointIndicator(/* ... */)
            REGION_MODE_LINE -> drawLineIndicator(/* ... */)
            REGION_MODE_AREA -> drawRectangleIndicator(/* ... */)
        }
    }
}
```

**MoveImageView → Draggable Composable:**
```kotlin
@Composable
fun DraggableAlignmentOverlay(
    offset: Offset,
    onOffsetChange: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onOffsetChange(offset + change)
                }
            }
    ) {
        // Alignment overlay content
    }
}
```

## Agent Prompt Guidelines

**When expanding or modifying the IRCamera UI, AI agents should follow these comprehensive guidelines to maintain consistency and architectural integrity:**

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

### **Thermal-Specific Guidelines**
1. **Temperature Overlays:** Always use `TemperatureView` for thermal data display, not custom overlays
2. **Event Integration:** Connect thermal UI changes to `ThermalActionEvent` with proper action codes
3. **Camera Stacking:** Maintain camera + overlay pattern using FrameLayout or Box in Compose
4. **State Synchronization:** Use `isPick` flags and visibility toggles for mode switching
5. **Device Connection:** Implement two-state UIs for connected/disconnected device states

### **Expansion Patterns**
When adding new screens or components:

**New Thermal Screen Template:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:background="@color/color_16131E">
    
    <com.mpdc4gsr.libunified.app.view.TitleView
        android:id="@+id/title_view"
        app:titleText="@string/new_screen_title" />
    
    <!-- Content area -->
    <FrameLayout android:id="@+id/content_area">
        <!-- Camera preview if needed -->
        <!-- Thermal overlay if needed -->
    </FrameLayout>
    
    <!-- Bottom actions if needed -->
    <ConstraintLayout android:id="@+id/action_lay">
        <Button 
            android:background="@drawable/bg_corners05_solid_theme"
            android:textColor="@color/color_img_calibration_button" />
    </ConstraintLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**New Dialog Template:**
```xml
<LinearLayout 
    android:orientation="vertical"
    android:background="@drawable/bg_corners05_solid_theme">
    
    <!-- Form sections -->
    <LinearLayout android:orientation="horizontal">
        <TextView android:text="Label:" />
        <EditText android:id="@+id/input_field" />
        <Button android:text="Action" />
    </LinearLayout>
    
    <!-- Action buttons -->
    <LinearLayout android:orientation="horizontal">
        <Button android:text="Cancel" />
        <Button android:text="Save" />
    </LinearLayout>
</LinearLayout>
```

### **Migration Strategy**
For converting existing XML to Compose:

1. **Preserve State Logic:** Maintain existing EventBus integration and state management
2. **Gradual Migration:** Convert leaf components first, then containers
3. **Interop Support:** Use `AndroidView` for complex custom views during transition
4. **Theme Consistency:** Map XML colors/dimens to Compose theme system
5. **Testing Approach:** Maintain parallel XML/Compose implementations during transition

### **Component Patterns**
- **Two-state UIs:** Preview mode vs editing mode (like image annotation)
- **Overlay patterns:** Camera + temperature/annotation overlays  
- **Button clusters:** Consistent styling with theme backgrounds
- **Form inputs:** Label + EditText + Button mini-patterns
- **Reusable Clusters:** Common UI patterns that can be modularized

**Label + Input + Button Pattern:**
```xml
<!-- Recurring pattern in calibration forms -->
<LinearLayout android:orientation="horizontal">
    <TextView android:text="Temperature:" />
    <EditText android:id="@+id/temp_input" />
    <Button android:text="Submit" />
</LinearLayout>
```

**Coordinate Input Pattern:**
```xml
<!-- X/Y input for pixel coordinates -->
<LinearLayout android:orientation="horizontal">
    <EditText android:hint="X" />
    <EditText android:hint="Y" />
</LinearLayout>
```

**Action Button Row Pattern:**
```xml
<!-- Bottom action buttons in popups -->
<LinearLayout android:orientation="horizontal">
    <Button android:text="Cancel" />
    <Button android:text="Save" />
    <Button android:text="Restore" />
</LinearLayout>
```

**Compose Modularization:**
```kotlin
@Composable
fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        OutlinedTextField(value, onValueChange)
        Button(onClick = onSubmit) { Text("Submit") }
    }
}

@Composable
fun CoordinateInput(
    x: String, 
    y: String,
    onXChange: (String) -> Unit,
    onYChange: (String) -> Unit
) {
    Row {
        OutlinedTextField(x, onXChange, Modifier.weight(1f), placeholder = { Text("X") })
        OutlinedTextField(y, onYChange, Modifier.weight(1f), placeholder = { Text("Y") })
    }
}
```

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

## Summary and Best Practices

### **Key Architectural Principles**
1. **Consistency:** All screens follow the same TitleView + Content + Actions pattern
2. **Modularity:** Reusable components (TitleView, TemperatureView) ensure uniform behavior
3. **State Management:** EventBus-driven updates allow decoupled UI reactions
4. **Theming:** Centralized color/dimension tokens enable easy theme updates
5. **Thermal Integration:** Specialized components handle IR camera data and user interactions

### **Critical Success Factors for UI Expansion**
- **Always** use existing wrapper components rather than creating new ones
- **Follow** established naming conventions for consistency across the codebase  
- **Leverage** the gradient theming system for visual consistency
- **Maintain** the overlay patterns for camera-based screens
- **Integrate** with EventBus for thermal functionality
- **Test** with actual thermal hardware when modifying camera components

### **Migration Roadmap**
For teams planning Compose migration:

1. **Phase 1:** Convert static screens (settings, about, help)
2. **Phase 2:** Migrate list-based screens using LazyColumn
3. **Phase 3:** Convert dialog and popup systems
4. **Phase 4:** Tackle camera preview screens with AndroidView wrappers
5. **Phase 5:** Implement full Compose thermal overlays and drawing components

This documentation serves as the definitive guide for maintaining architectural consistency while enabling modern UI development practices in the IRCamera application.