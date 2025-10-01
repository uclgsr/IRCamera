package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.*
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Camera Settings Screen - Configure RGB camera parameters
 */
@Composable
fun CameraSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var resolution by remember { mutableStateOf("1920x1080") }
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
            // Video Settings
            SettingsCard(
                title = "Video Settings",
                icon = Icons.Default.Videocam
            ) {
                SettingsDropdown(
                    label = "Resolution",
                    value = resolution,
                    options = listOf("1920x1080", "1280x720", "640x480"),
                    onValueChange = { resolution = it }
                )
                SettingsSlider(
                    label = "Frame Rate",
                    value = frameRate.toFloat(),
                    valueRange = 15f..60f,
                    onValueChange = { frameRate = it.toInt() },
                    unit = "fps"
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