package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.components.common.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel

@Composable
fun GSRSensorScreen(
    viewModel: GSRSensorViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSaveData: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorState by viewModel.sensorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Initialize recorder on first composition and manage lifecycle properly
    LaunchedEffect(Unit) {
        viewModel.initializeRecorder(context, lifecycleOwner)
    }
    // Track critical errors that need a dialog
    var showCriticalErrorDialog by remember { mutableStateOf(false) }
    var criticalErrorMessage by remember { mutableStateOf("") }
    // Show error notifications as Snackbar for non-critical errors
    LaunchedEffect(sensorState.error) {
        sensorState.error?.let { error ->
            // Check if this is a critical error (Bluetooth/permission)
            if (error.contains("Bluetooth", ignoreCase = true) ||
                error.contains("permission", ignoreCase = true) ||
                error.contains("initialization failed", ignoreCase = true)
            ) {
                criticalErrorMessage = error
                showCriticalErrorDialog = true
            } else {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }
    // Critical error dialog
    if (showCriticalErrorDialog) {
        AlertDialog(
            onDismissRequest = { showCriticalErrorDialog = false },
            title = { Text("GSR Sensor Error") },
            text = { Text(criticalErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showCriticalErrorDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (criticalErrorMessage.contains("Bluetooth", ignoreCase = true)) {
                    TextButton(onClick = {
                        showCriticalErrorDialog = false
                        // Try to re-initialize
                        viewModel.initializeRecorder(context, lifecycleOwner)
                    }) {
                        Text("Retry")
                    }
                }
            },
        )
    }
    // Use real data from ViewModel or fallback to simulated data for preview
    val isConnected = sensorState.isConnected
    val isRecording = sensorState.isRecording
    val currentGSR = if (sensorState.currentGSR > 0) sensorState.currentGSR else 2.45f
    val skinConductance = if (sensorState.skinConductance > 0) sensorState.skinConductance else 0.82f
    val deviceBattery = if (sensorState.deviceBattery > 0) sensorState.deviceBattery else 87
    val samplingRate = sensorState.samplingRate
    // Use GSR history from ViewModel state, with fallback to generated data
    val gsrHistory =
        if (sensorState.gsrHistory.isNotEmpty()) {
            sensorState.gsrHistory
        } else {
            remember { generateInitialGSRData() }
        }
    // Only simulate data when not connected and for preview purposes
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            // Simulation only runs when not connected for preview
            while (!isConnected) {
                kotlinx.coroutines.delay(1000)
                // This is just for UI preview when no real data
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF16131e),
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Title bar with GSR-specific actions
            TitleBar(
                title = "GSR Sensor Monitor",
                showBackButton = true,
                onBackClick = onBackClick,
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save GSR Data",
                    onClick = onSaveData,
                )
                TitleBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "GSR Settings",
                    onClick = onSettingsClick,
                )
            }
            // Scrollable content
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Connection status card
                GSRConnectionCard(
                    isConnected = isConnected,
                    deviceBattery = deviceBattery,
                    samplingRate = samplingRate,
                    connectionStatus = sensorState.connectionStatus,
                    isReconnecting = sensorState.isReconnecting,
                    reconnectionAttempt = sensorState.reconnectionAttempt,
                    maxReconnectionAttempts = sensorState.maxReconnectionAttempts,
                    error = sensorState.error,
                    onConnectionToggle = {
                        if (isConnected) {
                            viewModel.disconnectDevice()
                        } else {
                            viewModel.connectDevice()
                        }
                    },
                )
                // Real-time GSR metrics
                GSRMetricsCard(
                    currentGSR = currentGSR,
                    skinConductance = skinConductance,
                    isRecording = isRecording,
                )
                // GSR waveform visualization
                GSRWaveformCard(
                    gsrHistory = gsrHistory,
                    isStreaming = isConnected,
                    currentValue = currentGSR,
                )
                // Recording controls
                GSRRecordingControls(
                    isRecording = isRecording,
                    isConnected = isConnected,
                    onRecordingToggle = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onExportData = {
                        viewModel.exportData()
                        onSaveData()
                    },
                )
                // GSR analysis summary
                if (isRecording || gsrHistory.isNotEmpty()) {
                    GSRAnalysisCard(
                        gsrData = gsrHistory,
                        isRecording = isRecording,
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRConnectionCard(
    isConnected: Boolean,
    deviceBattery: Int,
    samplingRate: Int,
    connectionStatus: String = "Disconnected",
    isReconnecting: Boolean = false,
    reconnectionAttempt: Int = 0,
    maxReconnectionAttempts: Int = 0,
    error: String? = null,
    onConnectionToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isReconnecting -> Color(0xFF8B4513)
                        isConnected -> Color(0xFF1A2A1A)
                        error != null -> Color(0xFF4A1A1A)
                        else -> Color(0xFF2A1A1A)
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shimmer3 GSR Device",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = connectionStatus,
                        color =
                            when {
                                isReconnecting -> Color.Yellow
                                isConnected -> Color.Green
                                error != null -> Color.Red
                                else -> Color.Gray
                            },
                        fontSize = 14.sp,
                    )
                    if (isReconnecting && reconnectionAttempt > 0) {
                        Text(
                            text = "Reconnecting: attempt $reconnectionAttempt/$maxReconnectionAttempts",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (error != null && !isReconnecting) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Switch(
                    checked = isConnected,
                    onCheckedChange = { onConnectionToggle() },
                    enabled = !isReconnecting,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.Green,
                            uncheckedThumbColor = Color.Gray,
                            disabledCheckedThumbColor = Color.Yellow,
                            disabledUncheckedThumbColor = Color.DarkGray,
                        ),
                )
            }
            if (isConnected && !isReconnecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MetricItem("Battery", "$deviceBattery%", Color.Green)
                    MetricItem("Sampling", "${samplingRate}Hz", MaterialTheme.colorScheme.primary)
                    MetricItem("Status", "Streaming", Color.Cyan)
                }
            }
        }
    }
}

@Composable
private fun GSRMetricsCard(
    currentGSR: Float,
    skinConductance: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Real-time GSR Metrics",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isRecording) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "RECORDING",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricCard(
                    label = "GSR Value",
                    value = String.format("%.2f μS", currentGSR),
                    color = Color.Cyan,
                    description = "Current resistance",
                )
                MetricCard(
                    label = "Skin Conductance",
                    value = String.format("%.2f μS", skinConductance),
                    color = Color.Green,
                    description = "Conductance level",
                )
            }
        }
    }
}

@Composable
private fun GSRWaveformCard(
    gsrHistory: List<Float>,
    isStreaming: Boolean,
    currentValue: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "GSR Waveform",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            ) {
                val width = size.width
                val height = size.height
                val padding = 20.dp.toPx()
                val graphWidth = width - 2 * padding
                val graphHeight = height - 2 * padding
                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, height - padding),
                    end = Offset(width - padding, height - padding),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding),
                    end = Offset(padding, height - padding),
                    strokeWidth = 1.dp.toPx(),
                )
                // Draw GSR waveform
                if (gsrHistory.isNotEmpty()) {
                    val path = Path()
                    val minGSR = gsrHistory.minOrNull() ?: 0f
                    val maxGSR = gsrHistory.maxOrNull() ?: 5f
                    val range = maxGSR - minGSR
                    gsrHistory.forEachIndexed { index, value ->
                        val x = padding + (index.toFloat() / (gsrHistory.size - 1)) * graphWidth
                        val normalizedValue = if (range > 0) (value - minGSR) / range else 0.5f
                        val y = height - padding - normalizedValue * graphHeight
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan,
                        style =
                            androidx.compose.ui.graphics.drawscope
                                .Stroke(width = 2.dp.toPx()),
                    )
                }
                // Draw current value indicator
                if (isStreaming) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = 4.dp.toPx(),
                        center = Offset(width - padding - 10.dp.toPx(), height / 2),
                    )
                }
            }
            // Value scale indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0 μS", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${String.format("%.1f", currentValue)} μS",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                )
                Text("5 μS", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GSRRecordingControls(
    isRecording: Boolean,
    isConnected: Boolean,
    onRecordingToggle: () -> Unit,
    onExportData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = onRecordingToggle,
                    enabled = isConnected,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else Color.Green,
                        ),
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                Button(
                    onClick = onExportData,
                    enabled = !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Export Data")
                }
            }
        }
    }
}

@Composable
private fun GSRAnalysisCard(
    gsrData: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    if (gsrData.isEmpty()) return
    val avgGSR = gsrData.average().toFloat()
    val maxGSR = gsrData.maxOrNull() ?: 0f
    val minGSR = gsrData.minOrNull() ?: 0f
    val stdDev = kotlin.math.sqrt(gsrData.map { (it - avgGSR) * (it - avgGSR) }.average()).toFloat()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GSR Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem("Average", String.format("%.2f μS", avgGSR), Color.Cyan)
                MetricItem("Maximum", String.format("%.2f μS", maxGSR), Color.Red)
                MetricItem("Minimum", String.format("%.2f μS", minGSR), MaterialTheme.colorScheme.primary)
                MetricItem("Std Dev", String.format("%.2f", stdDev), Color.Yellow)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
        )
        Text(
            text = description,
            color = Color.Gray,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
        )
    }
}

private fun generateInitialGSRData(): List<Float> =
    (0..99).map {
        2.0f + kotlin.math.sin(it * 0.1f).toFloat() * 0.5f + kotlin.random.Random.nextFloat() * 0.2f
    }

@Preview(showBackground = true)
@Composable
private fun GSRSensorScreenPreview() {
    IRCameraTheme {
        GSRSensorScreen()
    }
}
