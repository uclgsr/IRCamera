# Material Icons Usage Examples

This document provides practical examples of using Material Icons with the updated UI components in the IRCamera
application.

## Component Examples

### 1. SettingsCompose - Creating Settings Screens

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mpdc4gsr.libunified.app.compose.components.SettingItemData
import com.mpdc4gsr.libunified.app.compose.components.SettingsSection

@Composable
fun MySettingsScreen() {
    Column {
        SettingsSection(
            title = "General Settings",
            items = listOf(
                SettingItemData(
                    text = "Share Data",
                    icon = Icons.Default.Share,  // Material Icon
                    showIcon = true,
                    showMoreArrow = true
                ),
                SettingItemData(
                    text = "Delete History",
                    icon = Icons.Default.Delete,  // Material Icon
                    showIcon = true,
                    showMoreArrow = true
                ),
                SettingItemData(
                    text = "Storage Settings",
                    icon = Icons.Default.Storage,  // Material Icon
                    showIcon = true,
                    showMoreArrow = true
                )
            ),
            onItemClick = { index ->
                when (index) {
                    0 -> handleShare()
                    1 -> handleDelete()
                    2 -> handleStorage()
                }
            }
        )
        
        // Specialized device settings with custom icons
        SettingsSection(
            title = "Device Settings",
            items = listOf(
                SettingItemData(
                    text = "GSR Sensor Configuration",
                    iconRes = R.drawable.ic_gsr_sensor,  // Custom icon for specialized hardware
                    showIcon = true,
                    showMoreArrow = true
                ),
                SettingItemData(
                    text = "Thermal Camera Settings",
                    iconRes = R.drawable.ic_thermal_camera,  // Custom icon for specialized hardware
                    showIcon = true,
                    showMoreArrow = true
                )
            ),
            onItemClick = { index ->
                when (index) {
                    0 -> handleGSRSettings()
                    1 -> handleThermalSettings()
                }
            }
        )
    }
}
```

### 2. MenuCompose - Tab Navigation

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mpdc4gsr.libunified.app.compose.components.MenuTabItem
import com.mpdc4gsr.libunified.app.compose.components.MenuTabBar

@Composable
fun MyTabNavigation() {
    var selectedTab by remember { mutableStateOf(0) }
    
    MenuTabBar(
        items = listOf(
            MenuTabItem(
                icon = Icons.Default.Home,  // Material Icon
                label = "Home"
            ),
            MenuTabItem(
                icon = Icons.Default.PhotoLibrary,  // Material Icon
                label = "Gallery"
            ),
            MenuTabItem(
                icon = Icons.Default.Settings,  // Material Icon
                label = "Settings"
            ),
            // Mixed with specialized icon
            MenuTabItem(
                iconRes = R.drawable.ic_thermal_camera,  // Custom thermal icon
                label = "Thermal"
            )
        ),
        selectedIndex = selectedTab,
        onTabSelected = { selectedTab = it }
    )
}
```

### 3. TitleBar - Toolbar Actions

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction

@Composable
fun MyScreen() {
    Scaffold(
        topBar = {
            TitleBar(
                title = "My Screen",
                showBackButton = true
            ) {
                // Material Icons for common actions
                TitleBarAction(
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    onClick = { handleSearch() }
                )
                
                TitleBarAction(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    onClick = { handleShare() }
                )
                
                TitleBarAction(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    onClick = { showMenu() }
                )
            }
        }
    ) { paddingValues ->
        // Screen content
    }
}
```

### 4. MenuViewsCompose - Camera Controls

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mpdc4gsr.libunified.app.compose.components.CameraMenuView

@Composable
fun MyCameraScreen() {
    CameraMenuView(
        // Using Material Icons (preferred)
        galleryIconVector = Icons.Default.PhotoLibrary,
        moreIconVector = Icons.Default.MoreVert,
        // Or use custom drawable if needed
        // galleryIcon = R.drawable.ic_custom_gallery,
        isVideoMode = false,
        canSwitchMode = true,
        onPhotoClick = { capturePhoto() },
        onVideoToggle = { toggleVideo() },
        onGalleryClick = { openGallery() },
        onMoreClick = { showMoreOptions() }
    )
}
```

## Complete Screen Example

Here's a complete example of a settings screen using Material Icons:

```kotlin
package com.example.mysettings

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.components.SettingItemData
import com.mpdc4gsr.libunified.app.compose.components.SettingsSection

class MySettingsActivity : BaseComposeActivity<MyViewModel>() {
    
    override fun createViewModel(): MyViewModel {
        return viewModels<MyViewModel>().value
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MyViewModel) {
        val settingsState by viewModel.settings.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSection(
                        title = "General",
                        items = listOf(
                            SettingItemData(
                                text = "Notifications",
                                icon = Icons.Default.Notifications,
                                showIcon = true,
                                showMoreArrow = true
                            ),
                            SettingItemData(
                                text = "Language",
                                icon = Icons.Default.Language,
                                showIcon = true,
                                showMoreArrow = true
                            ),
                            SettingItemData(
                                text = "Privacy",
                                icon = Icons.Default.Lock,
                                showIcon = true,
                                showMoreArrow = true
                            )
                        ),
                        onItemClick = { index -> handleGeneralSetting(index) }
                    )
                }
                
                item {
                    SettingsSection(
                        title = "Data",
                        items = listOf(
                            SettingItemData(
                                text = "Export Data",
                                icon = Icons.Default.Upload,
                                showIcon = true,
                                showMoreArrow = true
                            ),
                            SettingItemData(
                                text = "Clear Cache",
                                icon = Icons.Default.Delete,
                                showIcon = true,
                                showMoreArrow = true
                            ),
                            SettingItemData(
                                text = "Backup",
                                icon = Icons.Default.Backup,
                                showIcon = true,
                                showMoreArrow = true
                            )
                        ),
                        onItemClick = { index -> handleDataSetting(index) }
                    )
                }
                
                item {
                    SettingsSection(
                        title = "About",
                        items = listOf(
                            SettingItemData(
                                text = "App Version",
                                icon = Icons.Default.Info,
                                showIcon = true,
                                showMoreArrow = false
                            ),
                            SettingItemData(
                                text = "Help & Feedback",
                                icon = Icons.Default.Help,
                                showIcon = true,
                                showMoreArrow = true
                            )
                        ),
                        onItemClick = { index -> handleAboutSetting(index) }
                    )
                }
            }
        }
    }
    
    private fun handleGeneralSetting(index: Int) {
        // Handle general settings
    }
    
    private fun handleDataSetting(index: Int) {
        // Handle data settings
    }
    
    private fun handleAboutSetting(index: Int) {
        // Handle about settings
    }
}
```

## Icon Selection Guidelines

### When to Use Material Icons

- Common UI actions (save, share, delete, search, etc.)
- Navigation elements (back, forward, menu, etc.)
- Standard system concepts (settings, info, warning, etc.)
- Media controls (play, pause, volume, etc.)

### When to Use Custom Drawables

- Device-specific hardware (GSR sensors, thermal cameras)
- Domain-specific concepts (measurement types, specialized modes)
- Branding elements (company logos, product icons)
- Custom visualizations (waveforms, calibration targets)

## Available Material Icons

Commonly used Material Icons in IRCamera:

| Category       | Icons                                                          |
|----------------|----------------------------------------------------------------|
| **Actions**    | Share, Delete, Save, Upload, Download, Edit, Add, Remove       |
| **Navigation** | ArrowBack, ArrowForward, Menu, Close, MoreVert, MoreHoriz      |
| **Media**      | PlayArrow, Pause, Stop, FastForward, FastRewind, Mic, Videocam |
| **Device**     | Camera, CameraAlt, PhotoLibrary, VideoLibrary, Bluetooth       |
| **UI**         | Search, Settings, Info, Warning, Error, CheckCircle, Cancel    |
| **Data**       | Folder, FolderOpen, InsertChart, Assessment, Timeline          |

For a complete list, see: [Material Icons](https://fonts.google.com/icons)

## Migration Tips

1. **Start with common screens**: Settings and navigation screens are easiest to migrate
2. **Test incrementally**: Migrate one screen at a time and test thoroughly
3. **Preserve specialized icons**: Don't replace domain-specific custom icons
4. **Use consistent styles**: Stick to either Filled, Outlined, or Rounded throughout the app
5. **Consider accessibility**: Material Icons include proper content descriptions

## Resources

- [Material Design 3 Icons](https://m3.material.io/styles/icons)
- [Jetpack Compose Icons](https://developer.android.com/jetpack/compose/graphics/images/material)
- [Material Icons Extended Library](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary)
