package mpdc4gsr.feature.settings.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.settings.*

/**
 * Task D: Complete Settings Activity using Compose
 *
 * This activity demonstrates:
 * - Modern settings UI with Material 3 components
 * - Device configuration settings
 * - Recording preferences
 * - Display options
 * - Data export preferences
 */
class SettingsComposeActivity : BaseComposeActivity<SettingsViewModel>() {

    override fun createViewModel(): SettingsViewModel {
        return viewModels<SettingsViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SettingsViewModel) {
        // Settings state
        var thermalCameraEnabled by remember { mutableStateOf(true) }
        var gsrSensorEnabled by remember { mutableStateOf(true) }
        var autoRecording by remember { mutableStateOf(false) }
        var recordingQuality by remember { mutableStateOf("High") }
        var frameRate by remember { mutableFloatStateOf(10f) }
        var sampleRate by remember { mutableStateOf("51.2Hz") }
        var darkMode by remember { mutableStateOf(false) }
        var exportFormat by remember { mutableStateOf("CSV") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Configuration Section
                SettingsSection(title = "Device Configuration") {
                    SwitchSettingsItem(
                        title = "Thermal Camera",
                        subtitle = "Enable TOPDON TC001 thermal camera",
                        checked = thermalCameraEnabled,
                        onCheckedChange = { thermalCameraEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SwitchSettingsItem(
                        title = "GSR Sensor",
                        subtitle = "Enable Shimmer3 GSR sensor via BLE",
                        checked = gsrSensorEnabled,
                        onCheckedChange = { gsrSensorEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Device Calibration",
                        subtitle = "Calibrate thermal camera and sensors",
                        onClick = { }
                    )
                }

                // Recording Preferences Section
                SettingsSection(title = "Recording Preferences") {
                    SwitchSettingsItem(
                        title = "Auto Recording",
                        subtitle = "Start recording automatically when devices connect",
                        checked = autoRecording,
                        onCheckedChange = { autoRecording = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RadioButtonSettingsItem(
                        title = "Recording Quality",
                        options = listOf("Low", "Medium", "High", "Ultra"),
                        selectedOption = recordingQuality,
                        onOptionSelected = { recordingQuality = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SliderSettingsItem(
                        title = "Thermal Camera Frame Rate",
                        subtitle = "Adjust thermal camera capture rate",
                        value = frameRate,
                        onValueChange = { frameRate = it },
                        valueRange = 1f..30f,
                        valueLabel = { "${it.toInt()} Hz" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RadioButtonSettingsItem(
                        title = "GSR Sample Rate",
                        options = listOf("25.6Hz", "51.2Hz", "128Hz", "256Hz"),
                        selectedOption = sampleRate,
                        onOptionSelected = { sampleRate = it }
                    )
                }

                // Display Options Section
                SettingsSection(title = "Display Options") {
                    SwitchSettingsItem(
                        title = "Dark Mode",
                        subtitle = "Use dark theme optimized for thermal imaging",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Thermal Color Palette",
                        subtitle = "Choose thermal imaging color scheme",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Temperature Units",
                        subtitle = "Celsius, Fahrenheit, or Kelvin",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Display Resolution",
                        subtitle = "Adjust thermal image display resolution",
                        onClick = { }
                    )
                }

                // Data Export Section
                SettingsSection(title = "Data Export") {
                    RadioButtonSettingsItem(
                        title = "Export Format",
                        options = listOf("CSV", "JSON", "Excel", "HDF5"),
                        selectedOption = exportFormat,
                        onOptionSelected = { exportFormat = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Export Location",
                        subtitle = "Choose where to save exported data",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionSettingsItem(
                        title = "Export All Data",
                        subtitle = "Export all recorded sensor data",
                        actionText = "Export",
                        onAction = { }
                    )
                }

                // Network Settings Section
                SettingsSection(title = "Network Settings") {
                    SettingsItem(
                        title = "PC Controller Connection",
                        subtitle = "Configure connection to PC controller",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Network Discovery",
                        subtitle = "Enable automatic PC discovery",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionSettingsItem(
                        title = "Test Connection",
                        subtitle = "Test connection to PC controller",
                        actionText = "Test",
                        onAction = { }
                    )
                }

                // Advanced Settings Section
                SettingsSection(title = "Advanced Settings") {
                    SettingsItem(
                        title = "Developer Options",
                        subtitle = "Advanced configuration options",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Logging Settings",
                        subtitle = "Configure application logging",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionSettingsItem(
                        title = "Reset All Settings",
                        subtitle = "Reset all settings to default values",
                        actionText = "Reset",
                        onAction = { },
                        isDestructive = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionSettingsItem(
                        title = "Clear All Data",
                        subtitle = "Delete all recorded sensor data",
                        actionText = "Clear",
                        onAction = { },
                        isDestructive = true
                    )
                }

                // About Section
                SettingsSection(title = "About") {
                    SettingsItem(
                        title = "App Version",
                        subtitle = "IRCamera v1.10.000",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "View privacy policy and terms",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        title = "Help & Support",
                        subtitle = "Get help and contact support",
                        onClick = { }
                    )
                }
            }
        }
    }
}

/**
 * SettingsViewModel - Manages settings state
 */
class SettingsViewModel : AppBaseViewModel() {
    // Settings-specific state management
    private val _settingsState = mutableStateOf(SettingsState())
    val settingsState: State<SettingsState> = _settingsState

    data class SettingsState(
        val darkModeEnabled: Boolean = false,
        val networkDiscoveryEnabled: Boolean = true,
        val autoSaveEnabled: Boolean = true
    )
}