package mpdc4gsr.core.designsystem.components.recording

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

@Composable
fun RecordingControls(
    session: RecordingSessionSummary,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmergencyStop: () -> Unit = {},
    modifier: Modifier = Modifier,
    showAdvancedControls: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (session.state) {
                        RecordingUiState.Recording -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        RecordingUiState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            RecordingHeader(session = session)
            Spacer(modifier = Modifier.height(16.dp))
            RecordingControlsRow(
                session = session,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onEmergencyStop = onEmergencyStop,
            )
            if (showAdvancedControls && session.state != RecordingUiState.Idle) {
                Spacer(modifier = Modifier.height(16.dp))
                SessionDetails(session = session)
            }
        }
    }
}

@Deprecated(
    message = "Renamed for clarity",
    replaceWith =
        ReplaceWith(
            "RecordingControls(session, onStartRecording, onStopRecording, onEmergencyStop, modifier, showAdvancedControls)",
        ),
)
@Composable
fun RecordingControlsCompose(
    session: RecordingSessionSummary,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmergencyStop: () -> Unit = {},
    modifier: Modifier = Modifier,
    showAdvancedControls: Boolean = true,
) = RecordingControls(
    session = session,
    onStartRecording = onStartRecording,
    onStopRecording = onStopRecording,
    onEmergencyStop = onEmergencyStop,
    modifier = modifier,
    showAdvancedControls = showAdvancedControls,
)

@Composable
private fun RecordingHeader(session: RecordingSessionSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Recording Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = getStatusMessage(session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RecordingStatusIndicator(state = session.state)
    }
}

@Composable
private fun RecordingStatusIndicator(state: RecordingUiState) {
    val color by animateColorAsState(
        targetValue =
            when (state) {
                RecordingUiState.Idle -> Color(0xFF9E9E9E)
                RecordingUiState.Starting -> Color(0xFFFF9800)
                RecordingUiState.Recording -> Color(0xFFF44336)
                RecordingUiState.Stopping -> Color(0xFFFF9800)
                RecordingUiState.Error -> Color(0xFFF44336)
            },
        label = "recordingIndicator",
    )
    val scale by animateFloatAsState(
        targetValue = if (state == RecordingUiState.Recording) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "recordingIndicatorScale",
    )
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector =
                when (state) {
                    RecordingUiState.Idle -> Icons.Default.Stop
                    RecordingUiState.Starting -> Icons.Default.Refresh
                    RecordingUiState.Recording -> Icons.Default.FiberManualRecord
                    RecordingUiState.Stopping -> Icons.Default.Pause
                    RecordingUiState.Error -> Icons.Default.Error
                },
            contentDescription = null,
            tint = color,
            modifier =
                Modifier
                    .size(18.dp)
                    .scale(scale),
        )
    }
}

@Composable
private fun RecordingControlsRow(
    session: RecordingSessionSummary,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onEmergencyStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (session.state) {
            RecordingUiState.Idle, RecordingUiState.Error -> {
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording")
                }
            }

            RecordingUiState.Starting -> {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Starting...")
                }
            }

            RecordingUiState.Recording -> {
                Button(
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                        ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording")
                }

                IconButton(
                    onClick = onEmergencyStop,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency stop",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            RecordingUiState.Stopping -> {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stopping...")
                }
            }
        }
    }
}

@Composable
private fun SessionDetails(session: RecordingSessionSummary) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SessionDetailItem("Duration", session.duration, Icons.Default.Timer)
            SessionDetailItem("Data Size", session.dataSize, Icons.Default.Storage)
            SessionDetailItem("Frames", session.frameCount.toString(), Icons.Default.VideoLibrary)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerSourceChip(triggerSource = session.triggerSource)
            if (session.sessionId.isNotEmpty()) {
                Text(
                    text = "ID: ${session.sessionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        session.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
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
    icon: ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TriggerSourceChip(triggerSource: RecordingTriggerSource) {
    val (text, color) =
        when (triggerSource) {
            RecordingTriggerSource.Local -> "Local" to MaterialTheme.colorScheme.primary
            RecordingTriggerSource.RemotePc -> "PC Remote" to Color(0xFF2196F3)
            RecordingTriggerSource.RemoteMobile -> "Mobile Remote" to Color(0xFF4CAF50)
            RecordingTriggerSource.Scheduled -> "Scheduled" to Color(0xFFFF9800)
        }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon =
                when (triggerSource) {
                    RecordingTriggerSource.Local -> Icons.Default.TouchApp
                    RecordingTriggerSource.RemotePc -> Icons.Default.Computer
                    RecordingTriggerSource.RemoteMobile -> Icons.Default.PhoneAndroid
                    RecordingTriggerSource.Scheduled -> Icons.Default.Schedule
                }
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(12.dp),
                tint = color,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp,
            )
        }
    }
}

private fun getStatusMessage(session: RecordingSessionSummary): String =
    when (session.state) {
        RecordingUiState.Idle -> "Ready to start recording"
        RecordingUiState.Starting -> "Initializing recording systems..."
        RecordingUiState.Recording -> "Recording in progress - ${session.duration}"
        RecordingUiState.Stopping -> "Finalizing recording..."
        RecordingUiState.Error -> session.errorMessage ?: "Recording error occurred"
    }

@Composable
fun RecordingControlsDemo(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var session by remember {
        mutableStateOf(
            RecordingSessionSummary(
                state = RecordingUiState.Idle,
                triggerSource = RecordingTriggerSource.Local,
            ),
        )
    }
    LaunchedEffect(session.state) {
        if (session.state == RecordingUiState.Recording) {
            val startTime = System.currentTimeMillis()
            while (session.state == RecordingUiState.Recording) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / 60000) % 60
                val hours = elapsed / 3600000
                session =
                    session.copy(
                        duration = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        dataSize = "${elapsed / 10000} MB",
                        frameCount = (elapsed / 33).toInt(),
                    )
            }
        }
    }
    RecordingControls(
        session = session,
        onStartRecording = {
            session =
                session.copy(
                    state = RecordingUiState.Starting,
                    sessionId = "REC_${System.currentTimeMillis()}",
                    startTimeMillis = System.currentTimeMillis(),
                )
            scope.launch {
                delay(2000)
                session = session.copy(state = RecordingUiState.Recording)
            }
        },
        onStopRecording = {
            session = session.copy(state = RecordingUiState.Stopping)
            scope.launch {
                delay(1500)
                session =
                    RecordingSessionSummary(
                        state = RecordingUiState.Idle,
                        triggerSource = RecordingTriggerSource.Local,
                    )
            }
        },
        onEmergencyStop = {
            session =
                RecordingSessionSummary(
                    state = RecordingUiState.Idle,
                    triggerSource = RecordingTriggerSource.Local,
                )
        },
        modifier = modifier,
    )
}
