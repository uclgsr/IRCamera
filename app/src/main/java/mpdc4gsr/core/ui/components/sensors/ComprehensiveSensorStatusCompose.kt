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
            text = "🔬 Multi-Modal Sensor Dashboard",
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
            text = "📡 Sensor Connections",
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
        recordingState.isRecording -> "🔴 RECORDING - Session: ${recordingState.sessionId ?: "Unknown"}"
        errorSensors > 0 -> "⚠️ $errorSensors Sensor Error(s) Detected"
        activeSensors == 0 -> "⚪ No Sensors Connected"
        activeSensors < sensors.size -> "🟡 $activeSensors/${sensors.size} Sensors Connected"
        else -> "🟢 All Sensors Connected"
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
                text = "⏱️ Ready to Record",
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
        text = String.format("⏱️ Recording: %02d:%02d:%02d", hours, minutes, seconds),
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
                            text = "⚠️ Simulation Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Error message
                    if (sensor.errorMessage != null) {
                        Text(
                            text = "⚠️ ${sensor.errorMessage}",
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
