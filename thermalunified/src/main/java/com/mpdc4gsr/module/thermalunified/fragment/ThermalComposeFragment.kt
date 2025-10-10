package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.matrix.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

class ThermalComposeFragment : BaseComposeFragment<ThermalFragmentViewModel>() {
    override fun createViewModel(): ThermalFragmentViewModel = viewModels<ThermalFragmentViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalFragmentViewModel) {
        LibUnifiedTheme {
            // Observe thermal data from ViewModel
            val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
            val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
            val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
            val processingMode by viewModel.processingMode.collectAsStateWithLifecycle()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                // Status bar with connection and recording status
                StatusBar(
                    connectionStatus = connectionStatus,
                    isRecording = isRecording,
                    processingMode = processingMode,
                )
                // Main thermal camera view
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp),
                ) {
                    ThermalCameraView(
                        modifier = Modifier.fillMaxSize(),
                        onSurfaceReady = { surfaceView ->
                            viewModel.initializeThermalCamera(surfaceView)
                        },
                    )
                    // Temperature overlays
                    TemperatureOverlays(
                        temperatureData = temperatureData,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                // Control panel
                ControlPanel(
                    isRecording = isRecording,
                    onCapturePhoto = { viewModel.capturePhoto() },
                    onToggleRecording = { viewModel.toggleRecording() },
                    onOpenSettings = { viewModel.openSettings() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )
            }
        }
    }

    @Composable
    private fun StatusBar(
        connectionStatus: String,
        isRecording: Boolean,
        processingMode: String,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        when {
                            isRecording -> MaterialTheme.colorScheme.errorContainer
                            connectionStatus == "Connected" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "TC001 Thermal Camera",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            when (connectionStatus) {
                                "Connected" -> Color.Green
                                "Connecting" -> Color(0xFFFFA500)
                                else -> Color.Red
                            },
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (isRecording) {
                        Text(
                            text = "RECORDING",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "Mode: $processingMode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun ThermalCameraView(
        modifier: Modifier = Modifier,
        onSurfaceReady: (IrSurfaceView) -> Unit,
    ) {
        AndroidView(
            factory = { context ->
                IrSurfaceView(context).apply {
                    onSurfaceReady(this)
                }
            },
            modifier =
                modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
        )
    }

    @Composable
    private fun TemperatureOverlays(
        temperatureData: ThermalFragmentViewModel.TemperatureData?,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            temperatureData?.let { data ->
                // Center temperature
                TemperatureCard(
                    label = "Center",
                    temperature = data.centerTemp,
                    isMain = true,
                )
                // Max temperature
                TemperatureCard(
                    label = "Max",
                    temperature = data.maxTemp,
                    color = Color.Red,
                )
                // Min temperature
                TemperatureCard(
                    label = "Min",
                    temperature = data.minTemp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } ?: run {
                // Placeholder when no data
                TemperatureCard(
                    label = "Center",
                    temperature = "--°C",
                    isMain = true,
                )
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White,
    ) {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isMain) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        },
                ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style =
                        if (isMain) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                    color = color,
                )
                Text(
                    text = temperature,
                    style =
                        if (isMain) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
    }

    @Composable
    private fun ControlPanel(
        isRecording: Boolean,
        onCapturePhoto: () -> Unit,
        onToggleRecording: () -> Unit,
        onOpenSettings: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Card(
            modifier = modifier,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Capture photo button
                Button(
                    onClick = onCapturePhoto,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }
                // Recording toggle button
                Button(
                    onClick = onToggleRecording,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isRecording) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        ),
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Record")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                // Settings button
                OutlinedButton(
                    onClick = onOpenSettings,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
            }
        }
    }
}
