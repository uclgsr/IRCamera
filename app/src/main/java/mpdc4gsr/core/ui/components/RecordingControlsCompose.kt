package mpdc4gsr.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RecordingState {
    IDLE,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}

enum class TriggerSource {
    LOCAL,
    REMOTE_PC,
    REMOTE_MOBILE,
    SCHEDULED
}

data class RecordingSession(
    val state: RecordingState,
    val triggerSource: TriggerSource,
    val duration: String = "00:00:00",
    val sessionId: String = "",
    val dataSize: String = "0 MB",
    val frameCount: Int = 0,
    val errorMessage: String? = null,
    val startTime: Long = 0L
)

@Composable
fun RecordingControlsCompose(
    session: RecordingSession,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmergencyStop: () -> Unit = {},
    modifier: Modifier = Modifier,
    showAdvancedControls: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (session.state) {
                RecordingState.RECORDING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                RecordingState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status
            RecordingHeader(session = session)

            Spacer(modifier = Modifier.height(16.dp))

            // Main recording controls
            RecordingControls(
                session = session,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onEmergencyStop = onEmergencyStop
            )

            if (showAdvancedControls && session.state != RecordingState.IDLE) {
                Spacer(modifier = Modifier.height(16.dp))

                // Session details
                SessionDetails(session = session)
            }
        }
    }
}

@Composable
private fun RecordingHeader(session: RecordingSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Recording Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = getStatusMessage(session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status indicator
        RecordingStatusIndicator(state = session.state)
    }
}

@Composable
private fun RecordingStatusIndicator(state: RecordingState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            RecordingState.IDLE -> Color(0xFF9E9E9E)
            RecordingState.STARTING -> Color(0xFFFF9800)
            RecordingState.RECORDING -> Color(0xFFF44336)
            RecordingState.STOPPING -> Color(0xFFFF9800)
            RecordingState.ERROR -> Color(0xFFF44336)
        },
        animationSpec = tween(durationMillis = 300)
    )

    val scale by animateFloatAsState(
        targetValue = if (state == RecordingState.RECORDING) 1.0f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .scale(if (state == RecordingState.RECORDING) scale else 1f)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun RecordingControls(
    session: RecordingSession,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (session.state) {
            RecordingState.IDLE -> {
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording")
                }
            }

            RecordingState.STARTING -> {
                Button(
                    onClick = { /* Disabled during starting state */ },
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Starting...")
                }
            }

            RecordingState.RECORDING -> {
                Button(
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording")
                }

                OutlinedButton(
                    onClick = onEmergencyStop,
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emergency", fontSize = 12.sp)
                }
            }

            RecordingState.STOPPING -> {
                Button(
                    onClick = { /* Disabled during stopping state */ },
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stopping...")
                }
            }

            RecordingState.ERROR -> {
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SessionDetails(session: RecordingSession) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(12.dp))

        // Session info grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SessionDetailItem(
                label = "Duration",
                value = session.duration,
                icon = Icons.Default.Timer
            )

            SessionDetailItem(
                label = "Data Size",
                value = session.dataSize,
                icon = Icons.Default.Storage
            )

            SessionDetailItem(
                label = "Frames",
                value = session.frameCount.toString(),
                icon = Icons.Default.VideoLibrary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Trigger source and session ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriggerSourceChip(triggerSource = session.triggerSource)

            if (session.sessionId.isNotEmpty()) {
                Text(
                    text = "ID: ${session.sessionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Error message if any
        session.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TriggerSourceChip(triggerSource: TriggerSource) {
    val (text, color) = when (triggerSource) {
        TriggerSource.LOCAL -> "Local" to MaterialTheme.colorScheme.primary
        TriggerSource.REMOTE_PC -> "PC Remote" to Color(0xFF2196F3)
        TriggerSource.REMOTE_MOBILE -> "Mobile Remote" to Color(0xFF4CAF50)
        TriggerSource.SCHEDULED -> "Scheduled" to Color(0xFFFF9800)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (triggerSource) {
                    TriggerSource.LOCAL -> Icons.Default.TouchApp
                    TriggerSource.REMOTE_PC -> Icons.Default.Computer
                    TriggerSource.REMOTE_MOBILE -> Icons.Default.PhoneAndroid
                    TriggerSource.SCHEDULED -> Icons.Default.Schedule
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

private fun getStatusMessage(session: RecordingSession): String {
    return when (session.state) {
        RecordingState.IDLE -> "Ready to start recording"
        RecordingState.STARTING -> "Initializing recording systems..."
        RecordingState.RECORDING -> "Recording in progress - ${session.duration}"
        RecordingState.STOPPING -> "Finalizing recording..."
        RecordingState.ERROR -> session.errorMessage ?: "Recording error occurred"
    }
}

// Demo component with simulated recording
@Composable
fun RecordingControlsDemo(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var session by remember {
        mutableStateOf(
            RecordingSession(
                state = RecordingState.IDLE,
                triggerSource = TriggerSource.LOCAL
            )
        )
    }

    // Simulate recording timer
    LaunchedEffect(session.state) {
        if (session.state == RecordingState.RECORDING) {
            val startTime = System.currentTimeMillis()
            while (session.state == RecordingState.RECORDING) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / 60000) % 60
                val hours = elapsed / 3600000

                session = session.copy(
                    duration = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                    dataSize = "${(elapsed / 10000)} MB",
                    frameCount = (elapsed / 33).toInt() // ~30 FPS
                )
            }
        }
    }

    RecordingControlsCompose(
        session = session,
        onStartRecording = {
            session = session.copy(
                state = RecordingState.STARTING,
                sessionId = "REC_${System.currentTimeMillis()}",
                startTime = System.currentTimeMillis()
            )

            // Simulate start delay
            scope.launch {
                delay(2000)
                session = session.copy(state = RecordingState.RECORDING)
            }
        },
        onStopRecording = {
            session = session.copy(state = RecordingState.STOPPING)

            // Simulate stop delay
            scope.launch {
                delay(1500)
                session = RecordingSession(
                    state = RecordingState.IDLE,
                    triggerSource = TriggerSource.LOCAL
                )
            }
        },
        onEmergencyStop = {
            session = RecordingSession(
                state = RecordingState.IDLE,
                triggerSource = TriggerSource.LOCAL
            )
        },
        modifier = modifier
    )
}