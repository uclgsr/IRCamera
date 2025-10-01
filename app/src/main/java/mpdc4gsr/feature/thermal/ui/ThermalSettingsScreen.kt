package mpdc4gsr.feature.thermal.ui

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
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Thermal Settings Screen - Configure thermal camera parameters
 */
@Composable
fun ThermalSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var palette by remember { mutableStateOf("Iron") }
    var temperatureUnit by remember { mutableStateOf("Celsius") }
    var emissivity by remember { mutableFloatStateOf(0.95f) }
    var autoScale by remember { mutableStateOf(true) }
    var showCrosshair by remember { mutableStateOf(true) }
    var temperatureRange by remember { mutableStateOf("Auto") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Thermal Settings",
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
            // Display Settings
            SettingsCard(
                title = "Display Settings",
                icon = Icons.Default.Palette
            ) {
                SettingsDropdown(
                    label = "Color Palette",
                    value = palette,
                    options = listOf("Iron", "Rainbow", "Gray", "Hot", "Cool"),
                    onValueChange = { palette = it }
                )
                SettingsDropdown(
                    label = "Temperature Unit",
                    value = temperatureUnit,
                    options = listOf("Celsius", "Fahrenheit", "Kelvin"),
                    onValueChange = { temperatureUnit = it }
                )
                SettingsDropdown(
                    label = "Temperature Range",
                    value = temperatureRange,
                    options = listOf("Auto", "-20°C to 120°C", "0°C to 100°C", "Custom"),
                    onValueChange = { temperatureRange = it }
                )
            }

            // Measurement Settings
            SettingsCard(
                title = "Measurement",
                icon = Icons.Default.Straighten
            ) {
                SettingsSlider(
                    label = "Emissivity",
                    value = emissivity,
                    valueRange = 0.1f..1.0f,
                    onValueChange = { emissivity = it }
                )
                SettingsToggle(
                    label = "Auto Scale",
                    description = "Automatically adjust temperature scale",
                    checked = autoScale,
                    onCheckedChange = { autoScale = it }
                )
                SettingsToggle(
                    label = "Show Crosshair",
                    description = "Display center point crosshair",
                    checked = showCrosshair,
                    onCheckedChange = { showCrosshair = it }
                )
            }

            // Calibration Controls
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = { /* Start flat field calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Flat Field Calibration")
                }
                Button(
                    onClick = { /* Start temperature calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Thermostat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Temperature Calibration")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThermalSettingsScreenPreview() {
    IRCameraTheme {
        ThermalSettingsScreen()
    }
}