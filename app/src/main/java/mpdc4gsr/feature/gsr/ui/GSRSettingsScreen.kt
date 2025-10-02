package mpdc4gsr.feature.gsr.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.NavigationBreadcrumb
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

/**
 * GSR Settings Screen - Configure GSR sensor parameters and Shimmer3 device
 * Integrated with GSRSettingsViewModel and GSRSettingsRepository
 */
@Composable
fun GSRSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: GSRSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsUiState.collectAsState()
    val gsrSettings = uiState.gsrSettings
    val deviceSettings = uiState.deviceSettings

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )

        NavigationBreadcrumb(
            currentScreen = "GSR Settings",
            previousScreen = "Sensor Overview"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Configuration
            SettingsCard(
                title = "Device Configuration",
                icon = Icons.Default.DeviceHub
            ) {
                deviceSettings.deviceName?.let { name ->
                    SettingsRow(
                        label = "Device Name",
                        value = name
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SettingsSlider(
                    label = "Sampling Rate",
                    value = gsrSettings.samplingRate.toFloat(),
                    valueRange = 1f..512f,
                    onValueChange = { viewModel.updateSamplingRate(it.toInt()) },
                    unit = " Hz"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Reconnect",
                    description = "Automatically reconnect to devices after disconnection",
                    checked = deviceSettings.autoReconnect,
                    onCheckedChange = {
                        viewModel.updateDeviceSettings(
                            deviceSettings.copy(autoReconnect = it)
                        )
                    }
                )
            }

            // Data Collection
            SettingsCard(
                title = "Data Collection",
                icon = Icons.Default.DataUsage
            ) {
                SettingsToggle(
                    label = "Real-Time Monitoring",
                    description = "Enable real-time data monitoring",
                    checked = gsrSettings.enableRealTimeMonitoring,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableRealTimeMonitoring = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Data Filtering",
                    description = "Apply filtering to GSR data",
                    checked = gsrSettings.enableFiltering,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableFiltering = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Notifications",
                    description = "Show data collection notifications",
                    checked = gsrSettings.notificationEnabled,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(notificationEnabled = it)
                        )
                    }
                )
            }

            // Calibration
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = { /* Start calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Button(
                    onClick = { /* Reset to defaults */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSettingsScreenPreview() {
    IRCameraTheme {
        GSRSettingsScreen()
    }
}