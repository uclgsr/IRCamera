package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.device.presentation.DiagnosticsViewModel
import mpdc4gsr.feature.device.presentation.DiagnosticsViewModelFactory

/**
 * Diagnostics Screen - System diagnostics and troubleshooting
 * Integrated with DiagnosticsViewModel for real-time system monitoring
 */
@Composable
fun DiagnosticsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: DiagnosticsViewModel = viewModel(
        factory = DiagnosticsViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemStatus by viewModel.systemStatus.collectAsState()
    val sensorStatus by viewModel.sensorStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Diagnostics",
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
            // System Status
            SettingsCard(
                title = "System Status",
                icon = Icons.Default.Computer
            ) {
                SettingsRow(
                    label = "System Health",
                    value = systemStatus.systemHealth
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Battery",
                    value = systemStatus.battery
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Temperature",
                    value = systemStatus.temperature
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Memory Usage",
                    value = systemStatus.memoryUsage
                )
            }

            // Sensor Status
            SettingsCard(
                title = "Sensor Status",
                icon = Icons.Default.Sensors
            ) {
                SettingsRow(
                    label = "GSR Sensor",
                    value = sensorStatus.gsrSensor
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Thermal Camera",
                    value = sensorStatus.thermalCamera
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "RGB Camera",
                    value = sensorStatus.rgbCamera
                )
            }

            // Diagnostic Tools
            SettingsCard(
                title = "Diagnostic Tools",
                icon = Icons.Default.Build
            ) {
                Button(
                    onClick = { viewModel.runFullDiagnostics() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Full Diagnostics")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.testAllSensors() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test All Sensors")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.exportDiagnosticLogs() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Diagnostic Logs")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsScreenPreview() {
    IRCameraTheme {
        DiagnosticsScreen()
    }
}
