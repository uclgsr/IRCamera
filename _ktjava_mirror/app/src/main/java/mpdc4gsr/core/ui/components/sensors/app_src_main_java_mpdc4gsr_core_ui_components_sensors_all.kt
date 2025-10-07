// Merged .kt under 'app\src\main\java\mpdc4gsr\core\ui\components\sensors' subtree
// Files: 8; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\ComprehensiveSensorStatusCompose.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class SensorType {
    THERMAL, RGB, GSR, AUDIO
}

enum class SensorStatus {
    DISCONNECTED,
    CONNECTED,
    STREAMING,
    ERROR,
    SIMULATION
}

data class SensorState(
    val id: String,
    val displayName: String,
    val type: SensorType,
    val status: SensorStatus = SensorStatus.DISCONNECTED,
    val message: String? = null,
    val isSimulation: Boolean = false,
    val errorMessage: String? = null,
    val connectedDevices: Int = 0,
    val streamingDevices: Int = 0,
    val maxDevices: Int = 1
)

data class RecordingState(
    val isRecording: Boolean = false,
    val sessionId: String? = null,
    val startTime: Long = 0L
)

@Composable
fun ComprehensiveSensorStatusDashboard(
    sensors: List<SensorState>,
    recordingState: RecordingState,
    onSensorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "ðŸ”¬ Multi-Modal Sensor Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // Overall Status
        OverallStatusCard(sensors, recordingState)
        // Recording Status
        RecordingStatusCard(recordingState)
        // Sensor Connections
        Text(
            text = "ðŸ“¡ Sensor Connections",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        sensors.forEach { sensor ->
            SensorStatusCard(
                sensor = sensor,
                onClick = { onSensorClick(sensor.id) }
            )
        }
    }
}

@Composable
private fun OverallStatusCard(
    sensors: List<SensorState>,
    recordingState: RecordingState
) {
    val activeSensors = sensors.count { it.status == SensorStatus.CONNECTED || it.status == SensorStatus.STREAMING }
    val errorSensors = sensors.count { it.status == SensorStatus.ERROR }
    val statusText = when {
        recordingState.isRecording -> "ðŸ”´ RECORDING - Session: ${recordingState.sessionId ?: "Unknown"}"
        errorSensors > 0 -> "âš ï¸ $errorSensors Sensor Error(s) Detected"
        activeSensors == 0 -> "âšª No Sensors Connected"
        activeSensors < sensors.size -> "ðŸŸ¡ $activeSensors/${sensors.size} Sensors Connected"
        else -> "ðŸŸ¢ All Sensors Connected"
    }
    val statusColor = when {
        recordingState.isRecording -> MaterialTheme.colorScheme.error
        errorSensors > 0 -> MaterialTheme.colorScheme.error
        activeSensors == 0 -> MaterialTheme.colorScheme.surfaceVariant
        activeSensors < sensors.size -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = statusColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

@Composable
private fun RecordingStatusCard(recordingState: RecordingState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated recording indicator
        val infiniteTransition = rememberInfiniteTransition(label = "recording")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (recordingState.isRecording) 0.3f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (recordingState.isRecording)
                        Color.Red.copy(alpha = alpha)
                    else
                        Color.Gray,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (recordingState.isRecording && recordingState.startTime > 0) {
            RecordingTimer(recordingState.startTime)
        } else {
            Text(
                text = "â±ï¸ Ready to Record",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecordingTimer(startTime: Long) {
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            duration = (System.currentTimeMillis() - startTime) / 1000
            delay(1000)
        }
    }
    val hours = duration / 3600
    val minutes = (duration % 3600) / 60
    val seconds = duration % 60
    Text(
        text = String.format("â±ï¸ Recording: %02d:%02d:%02d", hours, minutes, seconds),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SensorStatusCard(
    sensor: SensorState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (sensor.status) {
                SensorStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                SensorStatus.STREAMING -> MaterialTheme.colorScheme.primaryContainer
                SensorStatus.CONNECTED -> MaterialTheme.colorScheme.secondaryContainer
                SensorStatus.SIMULATION -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Sensor Icon
                Icon(
                    imageVector = when (sensor.type) {
                        SensorType.THERMAL -> Icons.Default.Thermostat
                        SensorType.RGB -> Icons.Default.Camera
                        SensorType.GSR -> Icons.Default.Sensors
                        SensorType.AUDIO -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    tint = getStatusColor(sensor.status),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sensor.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = getStatusText(sensor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Multi-device info for GSR
                    if (sensor.type == SensorType.GSR && sensor.maxDevices > 1) {
                        Text(
                            text = "Connected: ${sensor.connectedDevices}/${sensor.maxDevices}, Streaming: ${sensor.streamingDevices}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Simulation warning
                    if (sensor.isSimulation) {
                        Text(
                            text = "âš ï¸ Simulation Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Error message
                    if (sensor.errorMessage != null) {
                        Text(
                            text = "âš ï¸ ${sensor.errorMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            // Status Indicator
            StatusIndicator(sensor.status)
        }
    }
}

@Composable
private fun StatusIndicator(status: SensorStatus) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = getStatusColor(status),
                shape = CircleShape
            )
    )
}

private fun getStatusColor(status: SensorStatus): Color = when (status) {
    SensorStatus.CONNECTED -> Color(0xFF4CAF50) // Green
    SensorStatus.STREAMING -> Color(0xFF2196F3) // Blue
    SensorStatus.ERROR -> Color(0xFFF44336) // Red
    SensorStatus.SIMULATION -> Color(0xFFFFEB3B) // Yellow
    SensorStatus.DISCONNECTED -> Color(0xFF9E9E9E) // Gray
}

private fun getStatusText(sensor: SensorState): String {
    return when (sensor.status) {
        SensorStatus.DISCONNECTED -> "Disconnected"
        SensorStatus.CONNECTED -> sensor.message ?: "Connected"
        SensorStatus.STREAMING -> sensor.message ?: "Streaming Data"
        SensorStatus.ERROR -> "Error"
        SensorStatus.SIMULATION -> "Simulation Mode"
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\GSRSensorCard.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.GSRAction
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (GSRAction) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with real GSR data from GSRSensorRecorder via ViewModel
    var gsrValue by remember { mutableFloatStateOf(2.45f) }
    var skinConductance by remember { mutableFloatStateOf(0.82f) }
    var isRecording by remember { mutableStateOf(false) }
    // Simulate GSR data updates when streaming
    LaunchedEffect(state) {
        if (state == SensorState.Streaming) {
            while (true) {
                kotlinx.coroutines.delay(100)
                gsrValue = 2.0f + kotlin.random.Random.nextFloat() * 1.5f
                skinConductance = 0.5f + kotlin.random.Random.nextFloat() * 0.8f
            }
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with sensor icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "GSR Sensor",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GSR Sensor",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = getStatusColor(state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = state.name,
                        color = getStatusColor(state),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // GSR data visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                GSRDataVisualization(
                    gsrValue = gsrValue,
                    skinConductance = skinConductance,
                    isStreaming = state == SensorState.Streaming
                )
            }
            // Sensor metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "GSR Value",
                    value = "${String.format("%.2f", gsrValue)} Î¼S",
                    color = Color.Cyan
                )
                MetricItem(
                    label = "Conductance",
                    value = "${String.format("%.2f", skinConductance)} Î¼S",
                    color = Color.Green
                )
                MetricItem(
                    label = "Sampling",
                    value = if (state == SensorState.Streaming) "128 Hz" else "---",
                    color = Color.Yellow
                )
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    SensorState.Disconnected -> {
                        Button(
                            onClick = { onAction(GSRAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = { onAction(GSRAction.StartStream) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                        Button(
                            onClick = { onAction(GSRAction.Disconnect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Disconnect")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(GSRAction.StopStream) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = { onSettingsClick?.invoke() }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(GSRAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Text("Retry")
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun GSRDataVisualization(
    gsrValue: Float,
    skinConductance: Float,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (isStreaming) {
                // Real-time GSR waveform
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2
                    // Draw baseline
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Draw GSR waveform
                    val path = Path()
                    val points = 100
                    val timeOffset = System.currentTimeMillis() / 100f
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val freq1 = 0.1f // Slow breathing component
                        val freq2 = 0.02f // Even slower arousal component
                        val y = centerY +
                                (sin((i * freq1 + timeOffset) * 0.1f) * gsrValue * 5f) +
                                (sin((i * freq2 + timeOffset) * 0.05f) * skinConductance * 10f)
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GSR Data Visualization",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun getStatusColor(state: SensorState): Color {
    return when (state) {
        SensorState.Connected -> Color.Green
        SensorState.Streaming -> MaterialTheme.colorScheme.primary
        SensorState.Error -> Color.Red
        SensorState.Disconnected -> Color.Gray
        SensorState.Connecting -> Color.Yellow
        SensorState.Simulation -> Color.Magenta
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSensorCardPreview() {
    IRCameraTheme {
        GSRSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {},
            onSettingsClick = {}
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\GSRVisualizationCard.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRVisualizationCard(
    gsrData: GSRData,
    connectionState: GSRConnectionState,
    modifier: Modifier = Modifier,
    onExportData: () -> Unit = {},
    onResetStatistics: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with connection and battery status
            GSRSensorHeader(
                connectionState = connectionState,
                batteryLevel = gsrData.batteryLevel,
                sampleRate = gsrData.sampleRate
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Real-time data chart
            GSRDataChart(
                recentReadings = gsrData.recentReadings,
                currentValue = gsrData.currentValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Current reading and statistics
            GSRStatistics(
                currentValue = gsrData.currentValue,
                averageValue = gsrData.averageValue,
                minValue = gsrData.minValue,
                maxValue = gsrData.maxValue
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Data export controls
            GSRDataControls(
                onExportData = onExportData,
                onResetStatistics = onResetStatistics
            )
        }
    }
}

@Composable
private fun GSRSensorHeader(
    connectionState: GSRConnectionState,
    batteryLevel: Int,
    sampleRate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GSR Sensor (Shimmer3)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sample Rate: $sampleRate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status
            Icon(
                if (connectionState.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (connectionState.isConnected) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Battery level
            val batteryIcon = when {
                batteryLevel > 80 -> Icons.Default.BatteryFull
                batteryLevel > 30 -> Icons.Default.Battery3Bar
                else -> Icons.Default.Battery1Bar
            }
            val batteryColor = when {
                batteryLevel > 50 -> Color.Green
                batteryLevel > 20 -> Color.Yellow
                else -> Color.Red
            }
            Icon(
                batteryIcon,
                contentDescription = null,
                tint = batteryColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$batteryLevel%",
                style = MaterialTheme.typography.labelSmall,
                color = batteryColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun GSRDataChart(
    recentReadings: List<Float>,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Real-time GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Chart visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val width = size.width
                val height = size.height
                val strokeWidth = 3.dp.toPx()
                if (recentReadings.isNotEmpty()) {
                    val maxValue = recentReadings.maxOrNull() ?: 1f
                    val minValue = recentReadings.minOrNull() ?: 0f
                    val range = maxValue - minValue
                    val path = Path()
                    recentReadings.forEachIndexed { index, value ->
                        val x = (index.toFloat() / (recentReadings.size - 1)) * width
                        val y = height - ((value - minValue) / range) * height
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4), // Teal color for GSR data
                        style = Stroke(width = strokeWidth)
                    )
                    // Current value indicator
                    val currentX = width
                    val currentY = height - ((currentValue - minValue) / range) * height
                    drawCircle(
                        color = Color(0xFF4ECDC4),
                        radius = 6.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRStatistics(
    currentValue: Float,
    averageValue: Float,
    minValue: Float,
    maxValue: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Current", currentValue, "kÎ©")
                StatisticItem("Average", averageValue, "kÎ©")
                StatisticItem("Min", minValue, "kÎ©")
                StatisticItem("Max", maxValue, "kÎ©")
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: Float,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "${String.format("%.2f", value)} $unit",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GSRDataControls(
    onExportData: () -> Unit,
    onResetStatistics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onExportData,
            modifier = Modifier.weight(1f)
        ) {
            Text("Export Data")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onResetStatistics,
            modifier = Modifier.weight(1f)
        ) {
            Text("Reset Stats")
        }
    }
}

// Data classes for GSR visualization
data class GSRData(
    val currentValue: Float,
    val batteryLevel: Int,
    val sampleRate: String = "51.2Hz",
    val recentReadings: List<Float> = emptyList(),
    val averageValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 0f
)

data class GSRConnectionState(
    val isConnected: Boolean,
    val deviceName: String = "",
    val connectionStrength: Int = 0
)


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\RGBCameraSensorCard.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.CameraAction
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RGBCameraSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (CameraAction) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // TODO: Replace with real camera parameters from Camera API via ViewModel
    var resolution by remember { mutableStateOf("1920Ã—1080") }
    var frameRate by remember { mutableIntStateOf(30) }
    var exposureTime by remember { mutableStateOf("1/60") }
    var iso by remember { mutableIntStateOf(200) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with camera icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "RGB Camera",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "RGB Camera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Built-in Camera",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                Surface(
                    color = getStatusColor(state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = state.name,
                        color = getStatusColor(state),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // Camera preview visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                RGBPreviewVisualization(
                    resolution = resolution,
                    frameRate = frameRate,
                    isStreaming = state == SensorState.Streaming
                )
            }
            // Camera metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Resolution",
                    value = resolution,
                    color = Color.White
                )
                MetricItem(
                    label = "Frame Rate",
                    value = "${frameRate} fps",
                    color = Color.Green
                )
                MetricItem(
                    label = "Exposure",
                    value = exposureTime,
                    color = Color.Yellow
                )
                MetricItem(
                    label = "ISO",
                    value = iso.toString(),
                    color = Color.Cyan
                )
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    SensorState.Disconnected -> {
                        Button(
                            onClick = { onAction(CameraAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = {
                                onClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Preview")
                        }
                        Button(
                            onClick = {
                                onSettingsClick?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Settings")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(CameraAction.StopPreview) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = {
                                // TODO: Implement photo capture functionality
                                // Should trigger camera capture and save to gallery
                                android.widget.Toast.makeText(
                                    context,
                                    "Capturing photo...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(CameraAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Text("Retry")
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun RGBPreviewVisualization(
    resolution: String,
    frameRate: Int,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (isStreaming) {
                // RGB camera preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    // Draw camera viewfinder background
                    drawRect(
                        color = Color(0xFF2E2E2E),
                        size = size
                    )
                    // Draw sample scene elements
                    // Background gradient
                    drawRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = Offset(0f, height * 0.6f),
                        size = Size(width, height * 0.4f)
                    )
                    // Simulated objects in scene
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = 20f,
                        center = Offset(width * 0.3f, height * 0.4f)
                    )
                    drawRect(
                        color = Color(0xFF5A5A5A),
                        topLeft = Offset(width * 0.6f, height * 0.3f),
                        size = Size(width * 0.2f, height * 0.3f)
                    )
                    // Viewfinder grid lines
                    val strokeWidth = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.3f)
                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(width / 3f, 0f),
                        end = Offset(width / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(width * 2f / 3f, 0f),
                        end = Offset(width * 2f / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 3f),
                        end = Offset(width, height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height * 2f / 3f),
                        end = Offset(width, height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )
                }
                // Camera info overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                // Frame rate indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${frameRate}fps",
                        color = Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
                // Focus indicator (center)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val focusSize = 25f
                    // Draw focus square
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - focusSize / 2, centerY - focusSize / 2),
                        size = Size(focusSize, focusSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera Preview",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = resolution,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun getStatusColor(state: SensorState): Color {
    return when (state) {
        SensorState.Connected -> Color.Green
        SensorState.Streaming -> MaterialTheme.colorScheme.primary
        SensorState.Error -> Color.Red
        SensorState.Disconnected -> Color.Gray
        SensorState.Connecting -> Color.Yellow
        SensorState.Simulation -> Color.Magenta
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraSensorCardPreview() {
    IRCameraTheme {
        RGBCameraSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {},
            onSettingsClick = {}
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\ThermalSensorCard.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.model.ThermalAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (ThermalAction) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with real thermal data from ThermalRecorder via ViewModel
    var centerTemp by remember { mutableFloatStateOf(25.6f) }
    var maxTemp by remember { mutableFloatStateOf(45.2f) }
    var minTemp by remember { mutableFloatStateOf(18.9f) }
    var deviceType by remember { mutableStateOf("TC001") }
    // Simulate thermal data updates when streaming
    LaunchedEffect(state) {
        if (state == SensorState.Streaming) {
            while (true) {
                kotlinx.coroutines.delay(200)
                centerTemp = 20.0f + kotlin.random.Random.nextFloat() * 15.0f
                maxTemp = centerTemp + kotlin.random.Random.nextFloat() * 10.0f
                minTemp = centerTemp - kotlin.random.Random.nextFloat() * 10.0f
            }
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with thermal icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Thermal Camera",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Thermal IR Camera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TOPDON $deviceType",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                Surface(
                    color = getStatusColor(state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = state.name,
                        color = getStatusColor(state),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // Thermal preview visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                ThermalPreviewVisualization(
                    centerTemp = centerTemp,
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    isStreaming = state == SensorState.Streaming
                )
            }
            // Temperature metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Center",
                    value = "${String.format("%.1f", centerTemp)}Â°C",
                    color = Color.White
                )
                MetricItem(
                    label = "Max",
                    value = "${String.format("%.1f", maxTemp)}Â°C",
                    color = Color.Red
                )
                MetricItem(
                    label = "Min",
                    value = "${String.format("%.1f", minTemp)}Â°C",
                    color = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    label = "Resolution",
                    value = "256Ã—192",
                    color = Color.Cyan
                )
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    SensorState.Disconnected -> {
                        Button(
                            onClick = { onAction(ThermalAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = {
                                onClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Preview")
                        }
                        Button(
                            onClick = { onAction(ThermalAction.Calibrate) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Text("Calibrate")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(ThermalAction.StopPreview) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = {
                                onSettingsClick?.invoke()
                            }
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Thermal Settings",
                                tint = Color.White
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(ThermalAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Text("Retry")
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ThermalPreviewVisualization(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (isStreaming) {
                // Thermal preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    // Draw thermal background pattern
                    drawRect(
                        color = Color(0xFF1A1A2E),
                        size = size
                    )
                    // Draw thermal hotspots based on temperature data
                    val hotspotRadius = 30f
                    // Max temperature hotspot (red)
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.8f),
                        radius = hotspotRadius,
                        center = Offset(width * 0.7f, height * 0.3f)
                    )
                    // Min temperature spot (blue)
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.8f),
                        radius = hotspotRadius * 0.7f,
                        center = Offset(width * 0.3f, height * 0.7f)
                    )
                    // Center temperature area (gradient)
                    val centerColor = when {
                        centerTemp > 30f -> Color.Yellow
                        centerTemp > 20f -> Color.Green
                        else -> Color.Cyan
                    }
                    drawCircle(
                        color = centerColor.copy(alpha = 0.6f),
                        radius = hotspotRadius * 0.8f,
                        center = Offset(width * 0.5f, height * 0.5f)
                    )
                }
                // Temperature overlays
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Max temp indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", maxTemp)}Â°C",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    // Min temp indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", minTemp)}Â°C",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    // Center crosshair
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val crosshairSize = 15f
                        // Draw crosshair
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(centerX - crosshairSize, centerY),
                            end = Offset(centerX + crosshairSize, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(centerX, centerY - crosshairSize),
                            end = Offset(centerX, centerY + crosshairSize),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Thermal Preview (256Ã—192)",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun getStatusColor(state: SensorState): Color {
    return when (state) {
        SensorState.Connected -> Color.Green
        SensorState.Streaming -> MaterialTheme.colorScheme.primary
        SensorState.Error -> Color.Red
        SensorState.Disconnected -> Color.Gray
        SensorState.Connecting -> Color.Yellow
        SensorState.Simulation -> Color.Magenta
    }
}

@Preview(showBackground = true)
@Composable
private fun ThermalSensorCardPreview() {
    IRCameraTheme {
        ThermalSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {},
            onSettingsClick = {}
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\sensors\UnifiedSensorStatus.kt =====

package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.*
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun UnifiedSensorStatus(
    systemState: UnifiedSystemState,
    activeSensors: List<SensorInfo>,
    onSystemAction: (SystemAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (systemState) {
                UnifiedSystemState.Recording -> Color(0xFF2A1A1A) // Dark red tint
                UnifiedSystemState.Active -> Color(0xFF1A2A1A) // Dark green tint
                UnifiedSystemState.Error -> Color(0xFF2A1A1A) // Dark red tint
                UnifiedSystemState.Inactive -> Color(0xFF2A2A2A) // Neutral
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // System status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Multi-Modal System",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getSystemStatusText(systemState, activeSensors.size),
                        color = getSystemStatusColor(systemState),
                        fontSize = 14.sp
                    )
                }
                Surface(
                    color = getSystemStatusColor(systemState).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = systemState.name,
                        color = getSystemStatusColor(systemState),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            // Sensor status visualization
            SensorStatusVisualization(
                sensors = activeSensors,
                systemState = systemState
            )
            // System metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val connectedSensors = activeSensors.count {
                    it.state == SensorState.Connected || it.state == SensorState.Streaming
                }
                val streamingSensors = activeSensors.count { it.state == SensorState.Streaming }
                MetricItem(
                    label = "Connected",
                    value = "$connectedSensors/${activeSensors.size}",
                    color = if (connectedSensors == activeSensors.size) Color.Green else Color.Yellow
                )
                MetricItem(
                    label = "Streaming",
                    value = streamingSensors.toString(),
                    color = if (streamingSensors > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
                MetricItem(
                    label = "Sync Status",
                    value = if (systemState == UnifiedSystemState.Active) "OK" else "---",
                    color = if (systemState == UnifiedSystemState.Active) Color.Green else Color.Gray
                )
            }
            // System control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (systemState) {
                    UnifiedSystemState.Inactive -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Sync All")
                        }
                    }

                    UnifiedSystemState.Active -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.StartRecording) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Record All")
                        }
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Re-sync")
                        }
                    }

                    UnifiedSystemState.Recording -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.StopRecording) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop All")
                        }
                        // Show recording duration
                        var recordingTime by remember { mutableIntStateOf(0) }
                        LaunchedEffect(systemState) {
                            while (systemState == UnifiedSystemState.Recording) {
                                kotlinx.coroutines.delay(1000)
                                recordingTime++
                            }
                        }
                        Text(
                            text = "Recording: ${recordingTime}s",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    UnifiedSystemState.Error -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                        ) {
                            Text("Recover")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusVisualization(
    sensors: List<SensorInfo>,
    systemState: UnifiedSystemState,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2
                // Draw connection lines between sensors
                if (sensors.size > 1) {
                    val sensorSpacing = width / (sensors.size + 1)
                    for (i in 0 until sensors.size - 1) {
                        val startX = sensorSpacing * (i + 1)
                        val endX = sensorSpacing * (i + 2)
                        val connectionColor = if (systemState == UnifiedSystemState.Active) {
                            Color.Green
                        } else {
                            Color.Gray
                        }
                        drawLine(
                            color = connectionColor,
                            start = Offset(startX, centerY),
                            end = Offset(endX, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                // Draw sensor nodes
                sensors.forEachIndexed { index, sensor ->
                    val x = width / (sensors.size + 1) * (index + 1)
                    val nodeColor = when (sensor.state) {
                        SensorState.Connected -> Color.Green
                        SensorState.Streaming -> primaryColor
                        SensorState.Error -> Color.Red
                        SensorState.Disconnected -> Color.Gray
                        SensorState.Connecting -> Color.Yellow
                        SensorState.Simulation -> Color.Magenta
                    }
                    // Draw sensor node
                    drawCircle(
                        color = nodeColor,
                        radius = 12.dp.toPx(),
                        center = Offset(x, centerY)
                    )
                    // Draw sensor type indicator
                    val innerColor = when (sensor.type) {
                        SensorType.GSR -> Color.Cyan
                        SensorType.ThermalIR -> Color.Red
                        SensorType.RGBCamera -> Color.White
                    }
                    drawCircle(
                        color = innerColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, centerY)
                    )
                }
            }
            // Sensor labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                sensors.forEach { sensor ->
                    Text(
                        text = when (sensor.type) {
                            SensorType.GSR -> "GSR"
                            SensorType.ThermalIR -> "IR"
                            SensorType.RGBCamera -> "RGB"
                        },
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

private fun getSystemStatusText(state: UnifiedSystemState, sensorCount: Int): String {
    return when (state) {
        UnifiedSystemState.Inactive -> "$sensorCount sensors available"
        UnifiedSystemState.Active -> "All systems operational"
        UnifiedSystemState.Recording -> "Multi-modal recording active"
        UnifiedSystemState.Error -> "System error - check sensors"
    }
}

private fun getSystemStatusColor(state: UnifiedSystemState): Color {
    return when (state) {
        UnifiedSystemState.Inactive -> Color.Gray
        UnifiedSystemState.Active -> Color.Green
        UnifiedSystemState.Recording -> Color.Red
        UnifiedSystemState.Error -> Color.Red
    }
}

@Preview(showBackground = true)
@Composable
private fun UnifiedSensorStatusPreview() {
    IRCameraTheme {
        UnifiedSensorStatus(
            systemState = UnifiedSystemState.Active,
            activeSensors = listOf(
                SensorInfo(SensorType.GSR, SensorState.Streaming),
                SensorInfo(SensorType.ThermalIR, SensorState.Connected),
                SensorInfo(SensorType.RGBCamera, SensorState.Streaming)
            ),
            onSystemAction = {}
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\SensorSelectionCompose.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class SensorType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isAvailable: Boolean = true,
    val requiresPermission: Boolean = false
) {
    THERMAL("Thermal Camera", "TC001/TS004 thermal imaging sensor", Icons.Default.Thermostat, true),
    RGB("RGB Camera", "High-resolution color camera", Icons.Default.Camera, true, true),
    GSR("GSR Sensor", "Galvanic skin response via Shimmer3", Icons.Default.Sensors, true, true),
    AUDIO("Audio Recorder", "High-quality audio capture", Icons.Default.Mic, true, true),
    ACCELEROMETER("Accelerometer", "Motion and orientation sensor", Icons.Default.Speed, true),
    GYROSCOPE("Gyroscope", "Angular velocity sensor", Icons.AutoMirrored.Filled.RotateRight, true),
    MAGNETOMETER("Magnetometer", "Magnetic field sensor", Icons.Default.Explore, true),
    HEART_RATE("Heart Rate", "Optical heart rate monitor", Icons.Default.Favorite, false),
    TEMPERATURE("Temperature", "Ambient temperature sensor", Icons.Default.DeviceThermostat, true),
    HUMIDITY("Humidity", "Environmental humidity sensor", Icons.Default.Water, false)
}

data class SensorAvailability(
    val sensorType: SensorType,
    val isAvailable: Boolean,
    val isSelected: Boolean,
    val availabilityReason: String = "",
    val batteryImpact: String = "Low",
    val dataRate: String = "Unknown"
)

@Composable
fun SensorSelectionDialog(
    availableSensors: List<SensorAvailability>,
    selectedSensors: Set<SensorType>,
    onSensorsSelected: (Set<SensorType>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Sensors",
    subtitle: String = "Choose sensors for your research session"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                SensorSelectionHeader(
                    title = title,
                    subtitle = subtitle,
                    selectedCount = selectedSensors.size,
                    totalCount = availableSensors.count { it.isAvailable }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Sensor list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableSensors) { sensorAvailability ->
                        SensorSelectionItem(
                            sensorAvailability = sensorAvailability,
                            isSelected = selectedSensors.contains(sensorAvailability.sensorType),
                            onSelectionChanged = { isSelected ->
                                val newSelection = if (isSelected) {
                                    selectedSensors + sensorAvailability.sensorType
                                } else {
                                    selectedSensors - sensorAvailability.sensorType
                                }
                                onSensorsSelected(newSelection)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSensorsSelected(emptySet())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = selectedSensors.isNotEmpty()
                    ) {
                        Text("Confirm (${selectedSensors.size})")
                    }
                }
                // Battery impact warning
                if (selectedSensors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryImpactWarning(
                        selectedSensors = selectedSensors,
                        availableSensors = availableSensors
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorSelectionHeader(
    title: String,
    subtitle: String,
    selectedCount: Int,
    totalCount: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$selectedCount/$totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SensorSelectionItem(
    sensorAvailability: SensorAvailability,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val sensor = sensorAvailability.sensorType
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                enabled = sensorAvailability.isAvailable,
                onClick = {
                    if (sensorAvailability.isAvailable) {
                        onSelectionChanged(!isSelected)
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !sensorAvailability.isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )

                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sensor icon
            Icon(
                imageVector = sensor.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    !sensorAvailability.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.5f
                    )

                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Sensor information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (sensorAvailability.isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = sensor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Availability status or data rate
                if (sensorAvailability.isAvailable) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Rate: ${sensorAvailability.dataRate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "â€¢",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Battery: ${sensorAvailability.batteryImpact}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (sensorAvailability.batteryImpact) {
                                "High" -> Color(0xFFF44336)
                                "Medium" -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }
                } else {
                    Text(
                        text = sensorAvailability.availabilityReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            // Selection indicator
            if (sensorAvailability.isAvailable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Unavailable",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BatteryImpactWarning(
    selectedSensors: Set<SensorType>,
    availableSensors: List<SensorAvailability>
) {
    val highImpactSensors = selectedSensors.filter { sensorType ->
        availableSensors.find { it.sensorType == sensorType }?.batteryImpact == "High"
    }
    if (highImpactSensors.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "High battery usage expected with ${highImpactSensors.size} power-intensive sensor(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

// Sample data generator - returns unavailable sensors by default
fun getSampleSensorAvailability(): List<SensorAvailability> {
    return listOf(
        SensorAvailability(
            sensorType = SensorType.THERMAL,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.RGB,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "High"
        ),
        SensorAvailability(
            sensorType = SensorType.GSR,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.AUDIO,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.ACCELEROMETER,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.GYROSCOPE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.MAGNETOMETER,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.HEART_RATE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Hardware not available",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.TEMPERATURE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.HUMIDITY,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Sensor not supported",
            dataRate = "Unknown",
            batteryImpact = "Low"
        )
    )
}

// Demo usage
@Composable
fun SensorSelectionDemo() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedSensors by remember { mutableStateOf<Set<SensorType>>(emptySet()) }
    val availableSensors = remember { getSampleSensorAvailability() }
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(
            onClick = { showDialog = true }
        ) {
            Text("Select Sensors (${selectedSensors.size})")
        }
        if (selectedSensors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selected: ${selectedSensors.joinToString { it.displayName }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    if (showDialog) {
        SensorSelectionDialog(
            availableSensors = availableSensors,
            selectedSensors = selectedSensors,
            onSensorsSelected = { newSelection ->
                selectedSensors = newSelection
            },
            onDismiss = { showDialog = false }
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\SensorStatusCard.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStatusCard(
    thermalCameraState: ConnectionState,
    gsrSensorState: ConnectionState,
    bleConnectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sensor Status",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SensorStatusRow(
                label = "Thermal Camera",
                state = thermalCameraState,
                details = "TC001 384x288"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SensorStatusRow(
                label = "GSR Sensor",
                state = gsrSensorState,
                details = "Shimmer3 51.2Hz"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SensorStatusRow(
                label = "BLE Connection",
                state = bleConnectionState,
                details = "Bluetooth LE"
            )
        }
    }
}

@Composable
private fun SensorStatusRow(
    label: String,
    state: ConnectionState,
    details: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        StatusIndicator(state = state)
    }
}

@Composable
private fun StatusIndicator(state: ConnectionState) {
    val (icon, color, text) = when (state) {
        is ConnectionState.Connected -> Triple(
            Icons.Default.CheckCircle,
            Color.Green,
            "Connected"
        )

        is ConnectionState.Connecting -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.primary,
            "Connecting"
        )

        is ConnectionState.Disconnected -> Triple(
            Icons.Default.Error,
            Color.Red,
            "Disconnected"
        )

        is ConnectionState.Error -> Triple(
            Icons.Default.Error,
            Color.Red,
            "Error"
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}


