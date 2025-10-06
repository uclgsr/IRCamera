package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
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
        viewModel.settingsEvents.collect { event ->
            when (event) {
                is GSRSettingsViewModel.SettingsEvent.ShowToast,
                is GSRSettingsViewModel.SettingsEvent.CalibrationStarted,
                is GSRSettingsViewModel.SettingsEvent.CalibrationCompleted,
                is GSRSettingsViewModel.SettingsEvent.ShowError -> {
                    val message = when (event) {
                        is GSRSettingsViewModel.SettingsEvent.ShowToast -> event.message
                        is GSRSettingsViewModel.SettingsEvent.CalibrationStarted -> event.message
                        is GSRSettingsViewModel.SettingsEvent.CalibrationCompleted -> event.message
                        is GSRSettingsViewModel.SettingsEvent.ShowError -> event.message
                        else -> ""
                    }
                    android.widget.Toast.makeText(
                        context,
                        message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
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

                if (deviceSettings.autoReconnect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Attempts",
                        value = deviceSettings.reconnectionAttempts.toFloat(),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionAttempts = it.toInt())
                            )
                        },
                        unit = " attempts"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Delay",
                        value = (deviceSettings.reconnectionBaseDelayMs / 1000f),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionBaseDelayMs = (it * 1000).toLong())
                            )
                        },
                        unit = " seconds"
                    )
                }
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
                    onClick = { viewModel.startCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Button(
                    onClick = { viewModel.resetToDefaults() },
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