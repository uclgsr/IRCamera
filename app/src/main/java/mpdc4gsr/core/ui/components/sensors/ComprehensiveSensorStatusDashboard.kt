package mpdc4gsr.core.ui.components.sensors

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.components.recording.RecordingSessionSummary
import mpdc4gsr.core.ui.components.recording.RecordingStatusIndicator
import mpdc4gsr.core.ui.components.recording.RecordingUiState

data class SensorOverview(
    val id: String,
    val option: SensorOption,
    val displayName: String = option.displayName,
    val status: SensorStatusUi = SensorStatusUi.Disconnected,
    val message: String? = null,
    val isSimulation: Boolean = false,
    val errorMessage: String? = null,
    val connectedDevices: Int = 0,
    val streamingDevices: Int = 0,
    val maxDevices: Int = 1,
)

@Composable
fun ComprehensiveSensorStatusDashboard(
    sensors: List<SensorOverview>,
    recording: RecordingSessionSummary,
    onSensorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Multi-Modal Sensor Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        OverallStatusCard(sensors = sensors, recording = recording)
        RecordingStatusIndicator(
            session = recording,
            activeSensors = sensors.filter { it.status == SensorStatusUi.Streaming }.map { it.displayName }.toSet(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Sensor Connections",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        sensors.forEach { overview ->
            SensorStatusCard(
                sensor = overview,
                onClick = { onSensorClick(overview.id) },
            )
        }
    }
}

@Composable
private fun OverallStatusCard(
    sensors: List<SensorOverview>,
    recording: RecordingSessionSummary,
) {
    val activeSensors = sensors.count { it.status == SensorStatusUi.Connected || it.status == SensorStatusUi.Streaming }
    val errorSensors = sensors.count { it.status == SensorStatusUi.Error }
    val statusText =
        when {
            recording.state == RecordingUiState.Recording -> "Recording - Session ${recording.sessionId.ifBlank { "unknown" }}"
            errorSensors > 0 -> "$errorSensors sensor error${if (errorSensors > 1) "s" else ""} detected"
            activeSensors == 0 -> "No sensors connected"
            activeSensors < sensors.size -> "$activeSensors of ${sensors.size} sensors connected"
            else -> "All sensors connected"
        }
    val statusColor =
        when {
            recording.state == RecordingUiState.Recording -> MaterialTheme.colorScheme.error
            errorSensors > 0 -> MaterialTheme.colorScheme.error
            activeSensors == 0 -> MaterialTheme.colorScheme.surfaceVariant
            activeSensors < sensors.size -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = statusColor.copy(alpha = 0.1f),
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        )
    }
}

@Composable
private fun SensorStatusCard(
    sensor: SensorOverview,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (sensor.status) {
                        SensorStatusUi.Error -> MaterialTheme.colorScheme.errorContainer
                        SensorStatusUi.Streaming -> MaterialTheme.colorScheme.primaryContainer
                        SensorStatusUi.Connected -> MaterialTheme.colorScheme.secondaryContainer
                        SensorStatusUi.Simulation -> MaterialTheme.colorScheme.tertiaryContainer
                        SensorStatusUi.Connecting -> MaterialTheme.colorScheme.surfaceVariant
                        SensorStatusUi.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        color = statusColor(sensor.status).copy(alpha = 0.18f),
                    ) {
                        IconWithStatus(
                            color = statusColor(sensor.status),
                            contentDescription = sensor.displayName,
                            icon = sensor.option.icon,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sensor.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = sensorStatusLabel(sensor),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (sensor.maxDevices > 1) {
                            Text(
                                text = "Connected: ${sensor.connectedDevices}/${sensor.maxDevices}, streaming: ${sensor.streamingDevices}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                StatusDot(color = statusColor(sensor.status))
            }
            if (sensor.isSimulation) {
                SimulationBanner()
            }
            sensor.errorMessage?.let { error ->
                ErrorBanner(message = error)
            }
            sensor.message?.let { note ->
                NoteBanner(message = note)
            }
        }
    }
}

@Composable
private fun IconWithStatus(
    color: Color,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sensorPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label = "sensorPulseAlpha",
    )
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color.copy(alpha = alpha),
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(12.dp)
                .background(color, CircleShape),
    )
}

@Composable
private fun SimulationBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "Simulation mode active",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun NoteBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

private fun sensorStatusLabel(sensor: SensorOverview): String =
    when (sensor.status) {
        SensorStatusUi.Disconnected -> "Disconnected"
        SensorStatusUi.Connecting -> "Connecting..."
        SensorStatusUi.Connected -> sensor.message ?: "Connected"
        SensorStatusUi.Streaming -> sensor.message ?: "Streaming data"
        SensorStatusUi.Error -> sensor.errorMessage ?: "Error"
        SensorStatusUi.Simulation -> "Simulation mode"
    }

private fun statusColor(status: SensorStatusUi): Color =
    when (status) {
        SensorStatusUi.Connected -> Color(0xFF4CAF50)
        SensorStatusUi.Streaming -> Color(0xFF2196F3)
        SensorStatusUi.Error -> Color(0xFFF44336)
        SensorStatusUi.Simulation -> Color(0xFFFFB300)
        SensorStatusUi.Connecting -> Color(0xFFFF9800)
        SensorStatusUi.Disconnected -> Color(0xFF9E9E9E)
    }
