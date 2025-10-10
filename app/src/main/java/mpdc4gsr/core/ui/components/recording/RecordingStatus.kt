package mpdc4gsr.core.ui.components.recording

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mpdc4gsr.feature.network.data.SensorStatusSummary

@Composable
fun RecordingStatusIndicator(
    session: RecordingSessionSummary,
    activeSensors: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    RecordingStatusIndicator(
        isRecording = session.state == RecordingUiState.Recording,
        sessionId = session.sessionId,
        activeSensors = activeSensors,
        startTimeMillis = session.startTimeMillis,
        modifier = modifier,
    )
}

@Composable
fun RecordingStatusIndicator(
    isRecording: Boolean,
    sessionId: String = "",
    activeSensors: Set<String> = emptySet(),
    startTimeMillis: Long = 0L,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status Icon with animation
        RecordingStatusIcon(isRecording)
        Spacer(modifier = Modifier.height(4.dp))
        // Status Text
        Text(
            text = if (isRecording) "[REC]" else "[STOP]",
            style = MaterialTheme.typography.bodySmall,
            color = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        // Duration
        if (isRecording && startTimeMillis > 0) {
            RecordingDuration(startTimeMillis)
        }
        // Sensors
        if (activeSensors.isNotEmpty()) {
            Text(
                text = activeSensors.joinToString(" | "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecordingStatusIcon(isRecording: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 0.3f else 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(
                    color = if (isRecording) Color.Red.copy(alpha = alpha) else Color.Gray,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
            contentDescription = if (isRecording) "Recording" else "Stopped",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun RecordingDuration(startTimeMillis: Long) {
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startTimeMillis) {
        while (true) {
            duration = (System.currentTimeMillis() - startTimeMillis) / 1000
            delay(1000)
        }
    }
    val minutes = duration / 60
    val seconds = duration % 60
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun RecordingStatusWithSensorSummary(
    summary: SensorStatusSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status Icon
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color = if (summary.isSessionActive) Color.Red else Color.Gray,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Status Text
        Text(
            text =
                if (summary.isSessionActive) {
                    "[REC]"
                } else {
                    when {
                        summary.totalSensorsInitialized == 0 -> "[ERR] NO SENSORS"
                        summary.totalSensorsInitialized < summary.totalSensorsConfigured -> "[WARN] PARTIAL SETUP"
                        else -> "[RDY] READY"
                    }
                },
            style = MaterialTheme.typography.bodySmall,
            color = if (summary.isSessionActive) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        // Sensor Status
        if (summary.sensors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val sensorDisplay =
                summary.sensors
                    .map { sensorStatus ->
                        val icon =
                            when {
                                sensorStatus.sensorType.contains("RGB", ignoreCase = true) -> "[CAM]"
                                sensorStatus.sensorType.contains("Thermal", ignoreCase = true) -> "[THM]"
                                sensorStatus.sensorType.contains("GSR", ignoreCase = true) -> "[GSR]"
                                else -> "[SEN]"
                            }
                        val statusIcon =
                            when {
                                sensorStatus.isRecording -> "[OK]"
                                sensorStatus.isInitialized -> "[RDY]"
                                else -> "[ERR]"
                            }
                        "$icon$statusIcon"
                    }.joinToString(" ")
            Text(
                text = if (summary.isSessionActive) sensorDisplay else "$sensorDisplay ready",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else if (!summary.isSessionActive && summary.totalSensorsInitialized == 0) {
            Text(
                text = "Check sensor connections",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun RecordingStatusBadge(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRecording) "REC" else "STOP",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
