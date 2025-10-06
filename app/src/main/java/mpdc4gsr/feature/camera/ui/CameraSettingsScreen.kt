package mpdc4gsr.feature.camera.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.data.CameraConfigurationManager

@Composable
fun CameraSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { CameraConfigurationManager() }
    val (supports4K, supportsRAW, supports60fps) = remember {
        configManager.detectDeviceCapabilities()
    }
    val availableResolutions = remember {
        buildList {
            if (supports4K) {
                add("3840x2160")
            }
            add("1920x1080")
            add("1280x720")
            add("640x480")
        }
    }
    val maxFrameRate = if (supports60fps) 60f else 30f
    var resolution by remember { mutableStateOf(availableResolutions.first()) }
    var frameRate by remember { mutableIntStateOf(30) }
    var autoFocus by remember { mutableStateOf(true) }
    var autoExposure by remember { mutableStateOf(true) }
    var stabilization by remember { mutableStateOf(false) }
    var gridLines by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Camera Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Capabilities
            SettingsCard(
                title = "Device Capabilities",
                icon = Icons.Default.Info
            ) {
                SettingsToggle(
                    label = "4K Video Support",
                    description = "Device supports 4K video recording",
                    checked = supports4K,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "60fps Support",
                    description = "Device supports 60fps video recording",
                    checked = supports60fps,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "RAW Image Support",
                    description = "Device supports RAW image capture",
                    checked = supportsRAW,
                    onCheckedChange = {},
                    enabled = false
                )
            }
            // Video Settings
            SettingsCard(
                title = "Video Settings",
                icon = Icons.Default.Videocam
            ) {
                SettingsDropdown(
                    label = "Resolution",
                    options = availableResolutions,
                    value = resolution,
                    onValueChange = { resolution = it }
                )
                SettingsSlider(
                    label = "Frame Rate",
                    value = frameRate.toFloat(),
                    valueRange = 15f..maxFrameRate,
                    onValueChange = { frameRate = it.toInt() },
                    unit = " fps"
                )
            }
            // Camera Features
            SettingsCard(
                title = "Camera Features",
                icon = Icons.Default.CameraAlt
            ) {
                SettingsToggle(
                    label = "Auto Focus",
                    description = "Automatic focus adjustment",
                    checked = autoFocus,
                    onCheckedChange = { autoFocus = it }
                )
                SettingsToggle(
                    label = "Auto Exposure",
                    description = "Automatic exposure control",
                    checked = autoExposure,
                    onCheckedChange = { autoExposure = it }
                )
                SettingsToggle(
                    label = "Image Stabilization",
                    description = "Digital image stabilization",
                    checked = stabilization,
                    onCheckedChange = { stabilization = it }
                )
            }
            // Interface Options
            SettingsCard(
                title = "Interface",
                icon = Icons.Default.GridOn
            ) {
                SettingsToggle(
                    label = "Grid Lines",
                    description = "Show rule of thirds grid",
                    checked = gridLines,
                    onCheckedChange = { gridLines = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraSettingsScreenPreview() {
    IRCameraTheme {
        CameraSettingsScreen()
    }
}