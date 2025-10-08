package mpdc4gsr.feature.thermal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.thermal.presentation.ThermalSettingsViewModel

@Composable
fun ThermalSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: ThermalSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val settings by viewModel.thermalSettings.collectAsStateWithLifecycle()
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
            // Recording Settings
            SettingsCard(
                title = "Recording Settings",
                icon = Icons.Default.Videocam
            ) {
                SettingsSlider(
                    label = "Frame Rate",
                    value = settings.frameRate.toFloat(),
                    valueRange = 10f..30f,
                    onValueChange = { viewModel.updateFrameRate(it.toInt()) },
                    unit = " fps"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Save Raw Images",
                    description = "Save individual thermal frames during recording",
                    checked = settings.saveRawImages,
                    onCheckedChange = { viewModel.updateSaveRawImages(it) }
                )
            }
            // Display Settings
            SettingsCard(
                title = "Display Settings",
                icon = Icons.Default.Palette
            ) {
                SettingsDropdown(
                    label = "Color Palette",
                    value = settings.palette,
                    options = listOf("Iron", "Rainbow", "Gray", "Hot", "Cool"),
                    onValueChange = { viewModel.updatePalette(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Temperature Unit",
                    value = settings.temperatureUnit,
                    options = listOf("Celsius", "Fahrenheit", "Kelvin"),
                    onValueChange = { viewModel.updateTemperatureUnit(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Temperature Range",
                    value = settings.temperatureRange,
                    options = listOf("Auto", "-20°C to 120°C", "0°C to 100°C", "Custom"),
                    onValueChange = { viewModel.updateTemperatureRange(it) }
                )
            }
            // Measurement Settings
            SettingsCard(
                title = "Measurement",
                icon = Icons.Default.Straighten
            ) {
                SettingsSlider(
                    label = "Emissivity",
                    value = settings.emissivity,
                    valueRange = 0.1f..1.0f,
                    onValueChange = { viewModel.updateEmissivity(it) },
                    unit = ""
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Scale",
                    description = "Automatically adjust temperature scale",
                    checked = settings.autoScale,
                    onCheckedChange = { viewModel.updateAutoScale(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Show Crosshair",
                    description = "Display center point crosshair",
                    checked = settings.showCrosshair,
                    onCheckedChange = { viewModel.updateShowCrosshair(it) }
                )
            }
            // Calibration Controls
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = {
                        // TODO: Start flat field calibration process
                        android.widget.Toast.makeText(
                            context,
                            "Starting flat field calibration...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Flat Field Calibration")
                }
                Button(
                    onClick = {
                        // TODO: Start temperature calibration process
                        android.widget.Toast.makeText(
                            context,
                            "Starting temperature calibration...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
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