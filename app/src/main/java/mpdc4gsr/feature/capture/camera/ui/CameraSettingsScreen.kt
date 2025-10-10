package mpdc4gsr.feature.capture.camera.ui

import android.hardware.camera2.CameraCharacteristics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.components.settings.SettingsCard
import mpdc4gsr.core.designsystem.components.settings.SettingsDropdown
import mpdc4gsr.core.designsystem.components.settings.SettingsRow
import mpdc4gsr.core.designsystem.components.settings.SettingsSlider
import mpdc4gsr.core.designsystem.components.settings.SettingsToggle
import mpdc4gsr.core.designsystem.theme.IRCameraTheme
import mpdc4gsr.feature.capture.camera.data.CameraConfigurationManager

@Composable
fun CameraSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configManager = remember(context) { CameraConfigurationManager(context) }
    val capabilities =
        remember {
            configManager.detectDeviceCapabilities()
        }
    val supports4K = capabilities.supports4K
    val supportsRAW = capabilities.supportsRaw
    val supports60fps = capabilities.supports60Fps
    val lensDescription = remember(capabilities) { describeLensFacing(capabilities.lensFacing) }
    val hardwareDescription = remember(capabilities) { describeHardwareLevel(capabilities.hardwareLevel) }
    val resolutionSummary =
        remember(capabilities) {
            val sizes = capabilities.supportedVideoSizes
            if (sizes.isEmpty()) {
                "Not available"
            } else {
                sizes.take(3).joinToString(", ") { size -> "${size.width}x${size.height}" }
            }
        }
    val availableResolutions =
        remember(capabilities) {
            val sizes = capabilities.supportedVideoSizes
            if (sizes.isNotEmpty()) {
                sizes.map { size -> "${size.width}x${size.height}" }
            } else {
                listOf("1920x1080", "1280x720", "640x480")
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
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF16131e)),
    ) {
        TitleBar(
            title = "Camera Settings",
            showBackButton = true,
            onBackClick = onBackClick,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Device Capabilities
            SettingsCard(
                title = "Device Capabilities",
                icon = Icons.Default.Info,
            ) {
                SettingsRow(
                    label = "Active Lens",
                    value = lensDescription,
                )
                SettingsRow(
                    label = "Hardware Level",
                    value = hardwareDescription,
                )
                SettingsRow(
                    label = "Top Resolutions",
                    value = resolutionSummary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "4K Video Support",
                    description = "Device supports 4K video recording",
                    checked = supports4K,
                    onCheckedChange = {},
                    enabled = false,
                )
                SettingsToggle(
                    label = "60fps Support",
                    description = "Device supports 60fps video recording",
                    checked = supports60fps,
                    onCheckedChange = {},
                    enabled = false,
                )
                SettingsToggle(
                    label = "RAW Image Support",
                    description = "Device supports RAW image capture",
                    checked = supportsRAW,
                    onCheckedChange = {},
                    enabled = false,
                )
            }
            // Video Settings
            SettingsCard(
                title = "Video Settings",
                icon = Icons.Default.Videocam,
            ) {
                SettingsDropdown(
                    label = "Resolution",
                    options = availableResolutions,
                    value = resolution,
                    onValueChange = { resolution = it },
                )
                SettingsSlider(
                    label = "Frame Rate",
                    value = frameRate.toFloat(),
                    valueRange = 15f..maxFrameRate,
                    onValueChange = { frameRate = it.toInt() },
                    unit = " fps",
                )
            }
            // Camera Features
            SettingsCard(
                title = "Camera Features",
                icon = Icons.Default.CameraAlt,
            ) {
                SettingsToggle(
                    label = "Auto Focus",
                    description = "Automatic focus adjustment",
                    checked = autoFocus,
                    onCheckedChange = { autoFocus = it },
                )
                SettingsToggle(
                    label = "Auto Exposure",
                    description = "Automatic exposure control",
                    checked = autoExposure,
                    onCheckedChange = { autoExposure = it },
                )
                SettingsToggle(
                    label = "Image Stabilization",
                    description = "Digital image stabilization",
                    checked = stabilization,
                    onCheckedChange = { stabilization = it },
                )
            }
            // Interface Options
            SettingsCard(
                title = "Interface",
                icon = Icons.Default.GridOn,
            ) {
                SettingsToggle(
                    label = "Grid Lines",
                    description = "Show rule of thirds grid",
                    checked = gridLines,
                    onCheckedChange = { gridLines = it },
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

private fun describeLensFacing(lensFacing: Int?): String =
    when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front Lens"
        CameraCharacteristics.LENS_FACING_BACK -> "Back Lens"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Lens"
        else -> "Unknown Lens"
    }

private fun describeHardwareLevel(level: Int?): String =
    when (level) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Level Full"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Level Limited"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Level Legacy"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "Level External"
        else -> "Level Unknown"
    }

