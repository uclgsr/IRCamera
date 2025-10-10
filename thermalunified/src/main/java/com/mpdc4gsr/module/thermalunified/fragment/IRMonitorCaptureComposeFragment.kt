package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorCaptureViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorCaptureViewModel.*
import java.text.SimpleDateFormat
import java.util.*

class IRMonitorCaptureComposeFragment : BaseComposeFragment<IRMonitorCaptureViewModel>() {
    override fun createViewModel(): IRMonitorCaptureViewModel {
        return viewModels<IRMonitorCaptureViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorCaptureViewModel) {
        // Observe ViewModel state
        val captureState by viewModel.captureState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val captureHistory by viewModel.captureHistory.collectAsStateWithLifecycle()
        val deviceConnectionState by viewModel.deviceConnectionState.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Capture status bar
                CaptureStatusBar(
                    captureState = captureState,
                    deviceConnectionState = deviceConnectionState,
                    onToggleCapture = { viewModel.toggleCapture() }
                )
                // Main capture interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal capture view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalCaptureView(
                            temperatureData = temperatureData,
                            deviceConnectionState = deviceConnectionState
                        )
                        // Temperature overlay
                        TemperatureCaptureOverlay(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        // Capture controls overlay
                        CaptureControlsOverlay(
                            captureState = captureState,
                            onCapture = { viewModel.captureFrame() },
                            onContinuousToggle = { viewModel.toggleContinuousCapture() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    // Capture history and controls panel
                    CaptureHistoryPanel(
                        captureState = captureState,
                        captureHistory = captureHistory,
                        onClearHistory = { viewModel.clearCaptureHistory() },
                        onExportCaptures = { viewModel.exportCaptures() },
                        onDeleteCapture = { capture ->
                            viewModel.deleteCapture(capture)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun CaptureStatusBar(
        captureState: CaptureState,
        deviceConnectionState: DeviceConnectionState,
        onToggleCapture: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (captureState) {
                    CaptureState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    CaptureState.CONTINUOUS -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IR Monitor Capture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getConnectionStatusText(deviceConnectionState),
                            color = getConnectionStatusColor(deviceConnectionState)
                        )
                        StatusChip(
                            text = getCaptureStatusText(captureState),
                            color = getCaptureStatusColor(captureState)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = captureState != CaptureState.INACTIVE,
                        onCheckedChange = { onToggleCapture() },
                        enabled = deviceConnectionState == DeviceConnectionState.CONNECTED
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun ThermalCaptureView(
        temperatureData: TemperatureData?,
        deviceConnectionState: DeviceConnectionState
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (deviceConnectionState == DeviceConnectionState.CONNECTED) {
                    // Temperature view integration for capture
                    AndroidView(
                        factory = { context ->
                            TemperatureView(context).apply {
                                // Configure for capture mode
                                temperatureRegionMode = TemperatureView.REGION_MODE_RECTANGLE
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Connection placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "No connection",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (deviceConnectionState) {
                                    DeviceConnectionState.DISCONNECTED -> "Device Not Connected"
                                    DeviceConnectionState.CONNECTING -> "Connecting..."
                                    DeviceConnectionState.ERROR -> "Connection Error"
                                    else -> "No Signal"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (deviceConnectionState == DeviceConnectionState.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureCaptureOverlay(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        temperatureData?.let { data ->
            Column(
                modifier = modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CaptureTemperatureCard(
                    label = "Center",
                    temperature = "${data.centerTemp}°C",
                    isMain = true
                )
                CaptureTemperatureCard(
                    label = "Max",
                    temperature = "${data.maxTemp}°C",
                    color = Color.Red
                )
                CaptureTemperatureCard(
                    label = "Min",
                    temperature = "${data.minTemp}°C",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    private fun CaptureTemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun CaptureControlsOverlay(
        captureState: CaptureState,
        onCapture: () -> Unit,
        onContinuousToggle: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Single capture button
                FloatingActionButton(
                    onClick = onCapture,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Continuous capture toggle
                FilterChip(
                    onClick = onContinuousToggle,
                    label = {
                        Text(
                            if (captureState == CaptureState.CONTINUOUS) "Stop Auto" else "Auto Capture",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = captureState == CaptureState.CONTINUOUS,
                    leadingIcon = {
                        Icon(
                            if (captureState == CaptureState.CONTINUOUS) Icons.Default.Stop else Icons.Default.Timer,
                            contentDescription = "Continuous Capture",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun CaptureHistoryPanel(
        captureState: CaptureState,
        captureHistory: List<CaptureData>,
        onClearHistory: () -> Unit,
        onExportCaptures: () -> Unit,
        onDeleteCapture: (CaptureData) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capture History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${captureHistory.size} captures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                // Capture list
                if (captureHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "No captures",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No captures yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(captureHistory) { capture ->
                            CaptureHistoryItem(
                                capture = capture,
                                onDeleteCapture = { onDeleteCapture(capture) }
                            )
                        }
                    }
                }
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportCaptures,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = captureHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All")
                    }
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = captureHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear History")
                    }
                }
            }
        }
    }

    @Composable
    private fun CaptureHistoryItem(
        capture: CaptureData,
        onDeleteCapture: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Capture ${capture.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(capture.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${capture.temperature}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDeleteCapture,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Helper functions
    private fun getConnectionStatusText(state: DeviceConnectionState): String = when (state) {
        DeviceConnectionState.CONNECTED -> "Connected"
        DeviceConnectionState.CONNECTING -> "Connecting"
        DeviceConnectionState.DISCONNECTED -> "Disconnected"
        DeviceConnectionState.ERROR -> "Error"
    }

    private fun getConnectionStatusColor(state: DeviceConnectionState): Color = when (state) {
        DeviceConnectionState.CONNECTED -> Color.Green
        DeviceConnectionState.CONNECTING -> Color(0xFFFFA500)
        DeviceConnectionState.DISCONNECTED -> Color.Gray
        DeviceConnectionState.ERROR -> Color.Red
    }

    private fun getCaptureStatusText(state: CaptureState): String = when (state) {
        CaptureState.INACTIVE -> "Inactive"
        CaptureState.ACTIVE -> "Ready"
        CaptureState.CONTINUOUS -> "Auto Capture"
        CaptureState.CAPTURING -> "Capturing"
    }

    @Composable
    private fun getCaptureStatusColor(state: CaptureState): Color = when (state) {
        CaptureState.INACTIVE -> Color.Gray
        CaptureState.ACTIVE -> Color.Green
        CaptureState.CONTINUOUS -> MaterialTheme.colorScheme.primary
        CaptureState.CAPTURING -> Color(0xFFFFA500)
    }
}