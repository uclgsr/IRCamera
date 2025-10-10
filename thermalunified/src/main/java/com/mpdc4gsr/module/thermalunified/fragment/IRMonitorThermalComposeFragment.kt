package com.mpdc4gsr.module.thermalunified.fragment

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThermostatAuto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

class IRMonitorThermalComposeFragment : BaseComposeFragment<ThermalFragmentViewModel>() {
    override fun createViewModel(): ThermalFragmentViewModel {
        return viewModels<ThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalFragmentViewModel) {
        val context = LocalContext.current
        val uiState by viewModel.thermalUiState.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "IR Thermal Monitor",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.showSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                IRMonitorThermalContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRMonitorThermalContent(
        viewModel: ThermalFragmentViewModel,
        uiState: ThermalFragmentViewModel.ThermalMonitoringUiState,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thermal camera view integration
            ThermalCameraSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            // Monitoring controls
            MonitoringControlsSection(
                onStartMonitoring = { viewModel.startMonitoring() },
                onStopMonitoring = { viewModel.stopMonitoring() },
                onConfigureRegions = { viewModel.configureRegions() },
                isMonitoring = uiState.isMonitoring
            )
            // Temperature data display
            TemperatureDataSection(
                currentTemp = uiState.currentTemperature,
                minTemp = uiState.minTemperature,
                maxTemp = uiState.maxTemperature,
                avgTemp = uiState.averageTemperature
            )
            // Monitoring status and alerts
            MonitoringStatusSection(
                isConnected = uiState.isDeviceConnected,
                isRecording = uiState.isRecording,
                alertCount = uiState.alertCount
            )
        }
    }

    @Composable
    private fun ThermalCameraSection(
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Integration with native thermal camera views
                AndroidView(
                    factory = { context: android.content.Context ->
                        // This would integrate with the actual CameraView and TemperatureView
                        // from the legacy implementation
                        FrameLayout(context).apply {
                            // Add CameraView and TemperatureView here
                            // For now, placeholder that shows integration point
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay for camera integration status
                if (true) { // Replace with actual camera status
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ThermostatAuto,
                                contentDescription = "Thermal Camera",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "IR Thermal Monitor View",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Integration with CameraView & TemperatureView",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MonitoringControlsSection(
        onStartMonitoring: () -> Unit,
        onStopMonitoring: () -> Unit,
        onConfigureRegions: () -> Unit,
        isMonitoring: Boolean
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Monitoring Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = if (isMonitoring) onStopMonitoring else onStartMonitoring,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isMonitoring) Icons.Default.RecordVoiceOver else Icons.Default.MonitorHeart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMonitoring) "Stop" else "Start")
                    }
                    OutlinedButton(
                        onClick = onConfigureRegions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Configure Regions")
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureDataSection(
        currentTemp: Float?,
        minTemp: Float?,
        maxTemp: Float?,
        avgTemp: Float?
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Temperature Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TemperatureCard("Current", currentTemp, MaterialTheme.colorScheme.primary)
                    TemperatureCard("Min", minTemp, MaterialTheme.colorScheme.secondary)
                    TemperatureCard("Max", maxTemp, MaterialTheme.colorScheme.error)
                    TemperatureCard("Avg", avgTemp, MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: Float?,
        color: Color
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
                Text(
                    temperature?.let { "%.1f°C".format(it) } ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun MonitoringStatusSection(
        isConnected: Boolean,
        isRecording: Boolean,
        alertCount: Int
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusRow(
                    "Device Connected",
                    isConnected,
                    if (isConnected) Color.Green else Color.Red
                )
                StatusRow(
                    "Recording",
                    isRecording,
                    if (isRecording) Color.Red else Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alerts", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (alertCount > 0)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            alertCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (alertCount > 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusRow(
        label: String,
        status: Boolean,
        color: Color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Text(
                    if (status) "Active" else "Inactive",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}