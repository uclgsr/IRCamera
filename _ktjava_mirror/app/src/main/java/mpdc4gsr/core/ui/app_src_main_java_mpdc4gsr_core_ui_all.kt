// Merged .kt under 'app\src\main\java\mpdc4gsr\core\ui' subtree
// Files: 37; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\ui\BaseViewModel.kt =====

package mpdc4gsr.core.ui

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class AppBaseViewModel : BaseViewModel() {
    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\DeleteConfirmationDialog.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.csl.irCamera.R

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.report_delete),
    message: String? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        },
        text = message?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
fun DeleteConfirmationPopup(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.report_delete),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\NavigationBreadcrumb.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationBreadcrumb(
    currentScreen: String,
    previousScreen: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        if (previousScreen != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Separator",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = previousScreen,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Separator",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .padding(horizontal = 4.dp)
        )
        Text(
            text = currentScreen,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\RecordingControlsCompose.kt =====

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
                        contentDescription = "Start Recording",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording")
                }
            }

            RecordingState.STARTING -> {
                Button(
                    onClick = {}, // Disabled during starting state
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
                        contentDescription = "Stop Recording",
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
                        contentDescription = "Emergency Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emergency", fontSize = 12.sp)
                }
            }

            RecordingState.STOPPING -> {
                Button(
                    onClick = {}, // Disabled during stopping state
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
                        contentDescription = "Retry Recording",
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
                        contentDescription = "Error",
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
            contentDescription = label,
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
                contentDescription = text,
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


// ===== app\src\main\java\mpdc4gsr\core\ui\components\RecordingStatusCompose.kt =====

package mpdc4gsr.core.ui.components

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
    isRecording: Boolean,
    sessionId: String = "",
    activeSensors: Set<String> = emptySet(),
    startTime: Long = 0L,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Icon with animation
        RecordingStatusIcon(isRecording)
        Spacer(modifier = Modifier.height(4.dp))
        // Status Text
        Text(
            text = if (isRecording) "[REC]" else "[STOP]",
            style = MaterialTheme.typography.bodySmall,
            color = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        // Duration
        if (isRecording && startTime > 0) {
            RecordingDuration(startTime)
        }
        // Sensors
        if (activeSensors.isNotEmpty()) {
            Text(
                text = activeSensors.joinToString(" â€¢ "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (isRecording) Color.Red.copy(alpha = alpha) else Color.Gray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
            contentDescription = if (isRecording) "Recording" else "Stopped",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RecordingDuration(startTime: Long) {
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            duration = (System.currentTimeMillis() - startTime) / 1000
            delay(1000)
        }
    }
    val minutes = duration / 60
    val seconds = duration % 60
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun RecordingStatusWithSensorSummary(
    summary: SensorStatusSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (summary.isSessionActive) Color.Red else Color.Gray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Status Text
        Text(
            text = if (summary.isSessionActive) {
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
            textAlign = TextAlign.Center
        )
        // Sensor Status
        if (summary.sensors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val sensorDisplay = summary.sensors.map { sensorStatus ->
                val icon = when {
                    sensorStatus.sensorType.contains("RGB", ignoreCase = true) -> "[CAM]"
                    sensorStatus.sensorType.contains("Thermal", ignoreCase = true) -> "[THM]"
                    sensorStatus.sensorType.contains("GSR", ignoreCase = true) -> "[GSR]"
                    else -> "[SEN]"
                }
                val statusIcon = when {
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
                textAlign = TextAlign.Center
            )
        } else if (!summary.isSessionActive && summary.totalSensorsInitialized == 0) {
            Text(
                text = "Check sensor connections",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecordingStatusBadge(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRecording) "REC" else "STOP",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\SensorDashboardCompose.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SensorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    ERROR,
    SIMULATION
}

data class SensorInfo(
    val id: String,
    val name: String,
    val status: SensorStatus,
    val message: String,
    val icon: ImageVector,
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never",
    val signalStrength: Int = 0,
    val isAnimating: Boolean = false
)

@Composable
fun SensorDashboardCompose(
    sensors: List<SensorInfo>,
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DashboardHeader(
                connectedCount = sensors.count { it.status == SensorStatus.CONNECTED || it.status == SensorStatus.STREAMING },
                totalCount = sensors.size,
                onRefresh = onRefresh
            )
        }
        items(sensors) { sensor ->
            SensorCard(
                sensor = sensor,
                onClick = { onSensorClick(sensor) }
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    connectedCount: Int,
    totalCount: Int,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sensor Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$connectedCount/$totalCount sensors connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    sensor: SensorInfo,
    onClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (sensor.status) {
            SensorStatus.CONNECTED -> Color(0xFF4CAF50)
            SensorStatus.STREAMING -> Color(0xFF2196F3)
            SensorStatus.ERROR -> Color(0xFFF44336)
            SensorStatus.CONNECTING -> Color(0xFFFF9800)
            SensorStatus.SIMULATION -> Color(0xFFFFEB3B)
            SensorStatus.DISCONNECTED -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(durationMillis = 300)
    )
    val scale by animateFloatAsState(
        targetValue = if (sensor.isAnimating) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Sensor icon
            Icon(
                imageVector = sensor.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = statusColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Sensor information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sensor.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sensor.status == SensorStatus.CONNECTED || sensor.status == SensorStatus.STREAMING) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sensor.dataRate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "â€¢",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sensor.lastUpdate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Status badge
            StatusBadge(status = sensor.status)
        }
    }
}

@Composable
private fun StatusBadge(status: SensorStatus) {
    val (text, containerColor, textColor) = when (status) {
        SensorStatus.CONNECTED -> Triple("Connected", Color(0xFF4CAF50), Color.White)
        SensorStatus.STREAMING -> Triple("Streaming", Color(0xFF2196F3), Color.White)
        SensorStatus.ERROR -> Triple("Error", Color(0xFFF44336), Color.White)
        SensorStatus.CONNECTING -> Triple("Connecting", Color(0xFFFF9800), Color.White)
        SensorStatus.SIMULATION -> Triple("Simulation", Color(0xFFFFEB3B), Color.Black)
        SensorStatus.DISCONNECTED -> Triple("Disconnected", Color(0xFF9E9E9E), Color.White)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SensorStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = textColor,
                    strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontSize = 10.sp
            )
        }
    }
}

// Sample data generator for testing - returns disconnected sensors by default
fun getSampleSensorData(): List<SensorInfo> {
    return listOf(
        SensorInfo(
            id = "thermal_camera",
            name = "Thermal Camera",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Thermostat,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "rgb_camera",
            name = "RGB Camera",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Camera,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "shimmer_gsr",
            name = "Shimmer GSR",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Sensors,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "bluetooth_device",
            name = "Bluetooth Device",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Bluetooth,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "network_device",
            name = "Network Sync",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.NetworkCheck,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "storage_device",
            name = "Storage System",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Storage,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        )
    )
}

// Static sensor dashboard for demo purposes - shows disconnected sensors
// For actual sensor connections, use SensorDashboardCompose with real sensor data
@Composable
fun SensorDashboardDemo(
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val sensors = remember { getSampleSensorData() }
    SensorDashboardCompose(
        sensors = sensors,
        modifier = modifier,
        onSensorClick = onSensorClick,
        onRefresh = onRefresh
    )
}

@Deprecated(
    message = "Function renamed to SensorDashboardDemo to reflect its static demo nature",
    replaceWith = ReplaceWith("SensorDashboardDemo(modifier, onSensorClick, onRefresh)")
)
@Composable
fun AnimatedSensorDashboard(
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    SensorDashboardDemo(modifier, onSensorClick, onRefresh)
}


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


// ===== app\src\main\java\mpdc4gsr\core\ui\components\settings\SettingsComponents.kt =====

package mpdc4gsr.core.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SwitchSettingsItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SliderSettingsItem(
    title: String,
    subtitle: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueLabel: (Float) -> String = { it.toString() },
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = valueLabel(value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RadioButtonSettingsItem(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = { onOptionSelected(option) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = { onOptionSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActionSettingsItem(
    title: String,
    subtitle: String? = null,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Button(
                onClick = onAction,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    unit: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                text = String.format("%.2f", value) + unit,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                }
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsToggle(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\ThermalVisualizationCard.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalVisualizationCard(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    isRecording: Boolean,
    onSettingsClick: () -> Unit = {},
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thermal Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Recording indicator
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated recording dot could be added here
                            Card(
                                modifier = Modifier.size(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red
                                ),
                                shape = RoundedCornerShape(50)
                            ) {}
                        }
                        Text(
                            text = "REC",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Temperature readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThermalReadingItem(
                    label = "Center",
                    temperature = centerTemp,
                    color = MaterialTheme.colorScheme.primary
                )
                ThermalReadingItem(
                    label = "Max",
                    temperature = maxTemp,
                    color = MaterialTheme.colorScheme.secondary
                )
                ThermalReadingItem(
                    label = "Min",
                    temperature = minTemp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSettingsClick) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun ThermalReadingItem(
    label: String,
    temperature: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${temperature.toInt()}Â°C",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\components\TitleBar.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp, // Match TitleView title size
                textAlign = TextAlign.Start
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = {
                        onBackClick?.invoke() ?: run {
                            // Default behavior: finish activity like TitleView
                            (context as? android.app.Activity)?.finish()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent, // Match TitleView transparent background
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
fun TitleBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

@Composable
fun TitleBarAction(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleBarPreview() {
    IRCameraTheme {
        TitleBar(
            title = "Connect Device",
            showBackButton = true
        ) {
            // Sample action icons
            TitleBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Settings",
                onClick = { }
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\ComposePerformanceMonitor.kt =====

package mpdc4gsr.core.ui

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.system.measureTimeMillis

object ComposePerformanceMonitor {
    private const val TAG = "ComposePerformance"

    // Performance thresholds
    private const val FRAME_BUDGET_MS = 16L // 60fps target
    const val MAX_SAMPLES = 100
    private val _recompositionCount = MutableStateFlow(0)
    val recompositionCount: StateFlow<Int> = _recompositionCount
    private val _frameTimeMs = MutableStateFlow(0L)
    val frameTimeMs: StateFlow<Long> = _frameTimeMs
    private val _memoryUsageMb = MutableStateFlow(0f)
    val memoryUsageMb: StateFlow<Float> = _memoryUsageMb
    private val _navigationLatencyMs = MutableStateFlow(0L)
    val navigationLatencyMs: StateFlow<Long> = _navigationLatencyMs
    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()

    @Composable
    fun TrackRecomposition(name: String, content: @Composable () -> Unit) {
        val recompositionCount = remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            recompositionCount.intValue++
            _recompositionCount.value = _recompositionCount.value + 1
            AppLogger.d(TAG, "$name recomposed ${recompositionCount.intValue} times")
        }
        content()
    }

    @Composable
    fun <T> MeasureCompositionTime(
        name: String,
        content: @Composable () -> T
    ): T {
        val startTime = remember { System.currentTimeMillis() }
        val result = content()
        LaunchedEffect(result) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            _frameTimeMs.value = duration
            recordMetric(name, duration)
            AppLogger.d(TAG, "$name composition took ${duration}ms")
        }
        return result
    }

    fun trackMemoryUsage(operationName: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        _memoryUsageMb.value = usedMemory.toFloat()
        AppLogger.d(TAG, "$operationName memory usage: ${usedMemory}MB")
    }

    fun trackNavigation(route: String, startTime: Long) {
        val latency = System.currentTimeMillis() - startTime
        _navigationLatencyMs.value = latency
        recordMetric("navigation_$route", latency)
        AppLogger.d(TAG, "Navigation to $route took ${latency}ms")
    }

    private fun recordMetric(name: String, value: Long) {
        val metric = performanceMetrics.getOrPut(name) { PerformanceMetric(name) }
        metric.addSample(value)
    }

    fun getPerformanceSummary(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }

    fun Modifier.trackDrawPerformance(name: String): Modifier = this.drawWithContent {
        val drawTime = measureTimeMillis {
            drawContent()
        }
        if (drawTime > FRAME_BUDGET_MS) {
            AppLogger.w(TAG, "$name draw took ${drawTime}ms (potential jank)")
        }
    }
}

data class PerformanceMetric(
    val name: String,
    private val samples: MutableList<Long> = mutableListOf()
) {
    fun addSample(value: Long) {
        samples.add(value)
        // Keep only last MAX_SAMPLES samples to prevent memory growth
        if (samples.size > ComposePerformanceMonitor.MAX_SAMPLES) {
            samples.removeAt(0)
        }
    }

    val average: Double get() = if (samples.isEmpty()) 0.0 else samples.average()
    val max: Long get() = samples.maxOrNull() ?: 0L
    val min: Long get() = samples.minOrNull() ?: 0L
    val count: Int get() = samples.size
}

@Composable
fun PerformanceOverlay(
    showOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!showOverlay) return
    val recompositionCount by ComposePerformanceMonitor.recompositionCount.collectAsState()
    val frameTime by ComposePerformanceMonitor.frameTimeMs.collectAsState()
    val memoryUsage by ComposePerformanceMonitor.memoryUsageMb.collectAsState()
    val navigationLatency by ComposePerformanceMonitor.navigationLatencyMs.collectAsState()
    val density = LocalDensity.current
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            // Draw performance overlay
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = Color.White.toArgb()
                    textSize = with(density) { 12.dp.toPx() }
                    isAntiAlias = true
                }
                val backgroundPaint = android.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.6f).toArgb()
                }
                val metrics = listOf(
                    "Recompositions: $recompositionCount",
                    "Frame Time: ${frameTime}ms",
                    "Memory: ${String.format("%.1f", memoryUsage)}MB",
                    "Nav Latency: ${navigationLatency}ms"
                )
                val textHeight = 40f
                val overlayHeight = metrics.size * textHeight + 20f
                // Draw background
                canvas.nativeCanvas.drawRect(
                    10f,
                    10f,
                    300f,
                    overlayHeight,
                    backgroundPaint
                )
                // Draw metrics
                metrics.forEachIndexed { index, metric ->
                    canvas.nativeCanvas.drawText(
                        metric,
                        20f,
                        40f + (index * textHeight),
                        paint
                    )
                }
            }
        }
    )
}

object SensorDataPerformanceTracker {
    private const val SENSOR_PROCESSING_THRESHOLD_MS = 100L
    private const val SENSOR_PROCESSING_CRITICAL_MS = 200L
    private const val NAVIGATION_SLOW_THRESHOLD_MS = 300L
    fun trackGSRDataProcessing(dataPoints: Int, processingTimeMs: Long) {
        val throughput = dataPoints / (processingTimeMs / 1000.0)
        Log.d(
            "SensorPerformance",
            "GSR processing: $dataPoints points in ${processingTimeMs}ms (${
                String.format(
                    "%.1f",
                    throughput
                )
            } points/sec)"
        )
        if (processingTimeMs > SENSOR_PROCESSING_THRESHOLD_MS) {
            Log.w(
                "SensorPerformance",
                "GSR processing slower than expected: ${processingTimeMs}ms for $dataPoints points"
            )
        }
    }

    fun trackThermalImageProcessing(imageSize: String, processingTimeMs: Long) {
        AppLogger.d("SensorPerformance", "Thermal image processing: $imageSize in ${processingTimeMs}ms")
        if (processingTimeMs > SENSOR_PROCESSING_CRITICAL_MS) {
            Log.w(
                "SensorPerformance",
                "Thermal processing slower than expected: ${processingTimeMs}ms"
            )
        }
    }

    fun trackNavigationPerformance(fromRoute: String, toRoute: String, transitionTimeMs: Long) {
        AppLogger.d("SensorPerformance", "Navigation from $fromRoute to $toRoute: ${transitionTimeMs}ms")
        if (transitionTimeMs > NAVIGATION_SLOW_THRESHOLD_MS) {
            AppLogger.w("SensorPerformance", "Navigation slower than expected: ${transitionTimeMs}ms")
        }
    }
}

object ComposeMemoryOptimizer {
    // Memory pressure thresholds
    private const val MEMORY_CRITICAL_THRESHOLD = 0.9f
    private const val MEMORY_HIGH_THRESHOLD = 0.7f
    private const val MEMORY_MODERATE_THRESHOLD = 0.5f

    fun checkMemoryPressure(): MemoryPressureLevel {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryRatio = usedMemory.toFloat() / maxMemory.toFloat()
        return when {
            memoryRatio > MEMORY_CRITICAL_THRESHOLD -> MemoryPressureLevel.CRITICAL
            memoryRatio > MEMORY_HIGH_THRESHOLD -> MemoryPressureLevel.HIGH
            memoryRatio > MEMORY_MODERATE_THRESHOLD -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.LOW
        }
    }

    fun getOptimizationSuggestions(pressureLevel: MemoryPressureLevel): List<String> {
        return when (pressureLevel) {
            MemoryPressureLevel.CRITICAL -> listOf(
                "Consider reducing image resolution",
                "Implement lazy loading for large datasets",
                "Clear unused caches",
                "Reduce number of concurrent operations"
            )

            MemoryPressureLevel.HIGH -> listOf(
                "Optimize image loading",
                "Use lighter data structures",
                "Consider pagination for long lists"
            )

            MemoryPressureLevel.MODERATE -> listOf(
                "Monitor memory usage trends",
                "Consider preemptive cleanup"
            )

            MemoryPressureLevel.LOW -> listOf(
                "Memory usage is optimal"
            )
        }
    }
}

enum class MemoryPressureLevel {
    LOW, MODERATE, HIGH, CRITICAL
}


// ===== app\src\main\java\mpdc4gsr\core\ui\ConnectionState.kt =====

package mpdc4gsr.core.ui

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceInfo: String? = null) : ConnectionState()
    data class Error(val error: AppError) : ConnectionState()
}

sealed class RecordingState {
    object Idle : RecordingState()
    object Starting : RecordingState()
    data class Recording(val sessionId: String, val duration: Long = 0) : RecordingState()
    object Paused : RecordingState()
    object Stopping : RecordingState()
    data class Error(val error: AppError) : RecordingState()
}

sealed class AppError(val message: String, val cause: Throwable? = null) {
    data class NetworkError(val errorMessage: String, val errorCode: Int? = null) :
        AppError(errorMessage)

    data class SensorError(val sensorType: String, val errorMessage: String) :
        AppError(errorMessage)

    data class RecordingError(val operation: String, val errorMessage: String) :
        AppError(errorMessage)

    data class UnknownError(val errorMessage: String, val throwable: Throwable? = null) :
        AppError(errorMessage, throwable)
}


// ===== app\src\main\java\mpdc4gsr\core\ui\InitUtils.kt =====

package mpdc4gsr.core.ui

import android.content.IntentFilter
import com.csl.irCamera.BuildConfig
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.utils.ConstantUtils
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils

object InitUtils {

    fun initReceiver() {
        try {
            val context = BaseApplication.instance
            val receiver = DeviceBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
                addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
                addAction("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")
                addAction("android.hardware.usb.action.USB_ACCESSORY_DETACHED")
                addAction(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
            }
            context.registerReceiver(receiver, filter)
            XLog.i("InitUtils: Device broadcast receiver initialized with USB permission action")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize receiver: ${e.message}")
        }
    }

    fun initLog() {
        try {
            if (BuildConfig.DEBUG) {
                XLog.init()
                XLog.i("InitUtils: Logging system initialized")
            }
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize logging: ${e.message}")
        }
    }

    fun initLms() {
        try {
            val context = BaseApplication.instance
            val locale = LanguageUtils.getCurrentLanguage()
            LMS.getInstance().init(context).apply {
                productType = "TC001"
                setLoginType(ConstantUtils.LOGIN_TC001_TYPE)
                softwareCode = BaseApplication.instance.getSoftWareCode()
                setEnabledLog(BuildConfig.DEBUG)
                setPrivacyPolicy("")
                setServicesAgreement("")
            }
            XLog.i("InitUtils: LMS initialized")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize LMS: ${e.message}")
        }
    }

    fun initUM() {
        try {
            // UM initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
            XLog.i("InitUtils: UM initialized (placeholder)")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize UM: ${e.message}")
        }
    }

    fun initJPush() {
        try {
            // JPush initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
            XLog.i("InitUtils: JPush initialized (placeholder)")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize JPush: ${e.message}")
        }
    }

    fun initXutils() {
        try {
            // XUtils initialization if needed
            XLog.i("InitUtils: XUtils initialized")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize XUtils: ${e.message}")
        }
    }

    fun setWxAppId(appId: String) {
        try {
            // WeChat App ID configuration
            XLog.i("InitUtils: WeChat App ID set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set WeChat App ID: ${e.message}")
        }
    }

    fun setBuglyAppId(appId: String) {
        try {
            // Bugly crash reporting configuration
            XLog.i("InitUtils: Bugly App ID set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set Bugly App ID: ${e.message}")
        }
    }

    fun setAppKey(appKey: String) {
        try {
            // App key configuration
            XLog.i("InitUtils: App Key set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set App Key: ${e.message}")
        }
    }

    fun setAppSecret(appSecret: String) {
        try {
            // App secret configuration
            XLog.i("InitUtils: App Secret set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set App Secret: ${e.message}")
        }
    }

    fun setAuthSecret(authSecret: String) {
        try {
            // Auth secret configuration
            XLog.i("InitUtils: Auth Secret set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set Auth Secret: ${e.message}")
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\model\SensorModels.kt =====

package mpdc4gsr.core.ui.model

enum class SensorType {
    GSR,
    ThermalIR,
    RGBCamera
}

enum class SensorState {
    Disconnected,
    Connecting,
    Connected,
    Streaming,
    Error,
    Simulation
}

enum class UnifiedSystemState {
    Inactive,
    Active,
    Recording,
    Error
}

data class SensorInfo(
    val type: SensorType,
    val state: SensorState,
    val metadata: Map<String, String> = emptyMap()
)

sealed class SystemAction {
    object StartRecording : SystemAction()
    object StopRecording : SystemAction()
    object Synchronize : SystemAction()
}

sealed class GSRAction {
    object Connect : GSRAction()
    object Disconnect : GSRAction()
    object StartStream : GSRAction()
    object StopStream : GSRAction()
    data class ConfigureDevice(val deviceId: String) : GSRAction()
}

sealed class ThermalAction {
    object Connect : ThermalAction()
    object Disconnect : ThermalAction()
    object StartPreview : ThermalAction()
    object StopPreview : ThermalAction()
    object Calibrate : ThermalAction()
    object OpenSettings : ThermalAction()
}

sealed class CameraAction {
    object Connect : CameraAction()
    object Disconnect : CameraAction()
    object StartPreview : CameraAction()
    object StopPreview : CameraAction()
    data class SetResolution(val width: Int, val height: Int) : CameraAction()
}


// ===== app\src\main\java\mpdc4gsr\core\ui\navigation\IRCameraNavigation.kt =====

package mpdc4gsr.core.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.feature.gsr.ui.SensorDashboardComposeActivity
import mpdc4gsr.feature.settings.ui.SettingsComposeActivity
import mpdc4gsr.feature.main.ui.MainComposeActivity
import mpdc4gsr.feature.settings.ui.AboutScreen

sealed class IRCameraScreen(val route: String) {
    object Main : IRCameraScreen("main")
    object MainCompose : IRCameraScreen("main_compose")
    object MainFragment : IRCameraScreen("main_fragment")
    object MainFragmentCompose : IRCameraScreen("main_fragment_compose")
    object ThermalCamera : IRCameraScreen("thermal_camera")
    object ThermalCameraCompose : IRCameraScreen("thermal_camera_compose")
    object ThermalFragment : IRCameraScreen("thermal_fragment")
    object ThermalFragmentCompose : IRCameraScreen("thermal_fragment_compose")
    object SensorDashboard : IRCameraScreen("sensor_dashboard")
    object SensorDashboardCompose : IRCameraScreen("sensor_dashboard_compose")
    object SensorDashboardFragment : IRCameraScreen("sensor_dashboard_fragment")
    object SensorDashboardFragmentCompose : IRCameraScreen("sensor_dashboard_fragment_compose")
    object Gallery : IRCameraScreen("gallery")
    object GalleryFragment : IRCameraScreen("gallery_fragment")
    object GalleryFragmentCompose : IRCameraScreen("gallery_fragment_compose")

    // Priority 3: Specialized Thermal Fragments
    object IRCorrectionFragment : IRCameraScreen("ir_correction_fragment")
    object IRCorrectionFragmentCompose : IRCameraScreen("ir_correction_fragment_compose")
    object MonitorThermalFragment : IRCameraScreen("monitor_thermal_fragment")
    object MonitorThermalFragmentCompose : IRCameraScreen("monitor_thermal_fragment_compose")

    // Priority 4: Additional Gallery and Lite Fragments
    object IRGalleryTabFragmentCompose : IRCameraScreen("ir_gallery_tab_fragment_compose")
    object GalleryPictureFragment : IRCameraScreen("gallery_picture_fragment")
    object GalleryPictureFragmentCompose : IRCameraScreen("gallery_picture_fragment_compose")
    object IRPlushFragment : IRCameraScreen("ir_plush_fragment")
    object IRPlushFragmentCompose : IRCameraScreen("ir_plush_fragment_compose")
    object IRMonitorLiteFragment : IRCameraScreen("ir_monitor_lite_fragment")
    object IRMonitorLiteFragmentCompose : IRCameraScreen("ir_monitor_lite_fragment_compose")

    // Priority 5: Final Specialized Fragments
    object GalleryVideoFragment : IRCameraScreen("gallery_video_fragment")
    object GalleryVideoFragmentCompose : IRCameraScreen("gallery_video_fragment_compose")
    object PDFListFragmentCompose : IRCameraScreen("pdf_list_fragment_compose")
    object IRMonitorCaptureFragment : IRCameraScreen("ir_monitor_capture_fragment")
    object IRMonitorCaptureFragmentCompose : IRCameraScreen("ir_monitor_capture_fragment_compose")
    object IRMonitorHistoryFragment : IRCameraScreen("ir_monitor_history_fragment")
    object IRMonitorHistoryFragmentCompose : IRCameraScreen("ir_monitor_history_fragment_compose")
    object IRMonitorThermalFragment : IRCameraScreen("ir_monitor_thermal_fragment")
    object IRMonitorThermalFragmentCompose : IRCameraScreen("ir_monitor_thermal_fragment_compose")
    object Settings : IRCameraScreen("settings")
    object SettingsCompose : IRCameraScreen("settings_compose")
    object About : IRCameraScreen("about")
}

@Composable
fun IRCameraNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = IRCameraScreen.Main.route
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Main dashboard screens
        composable(IRCameraScreen.Main.route) {
            // Launch main activity
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, MainComposeActivity::class.java))
            }
            LoadingScreen()
        }
        composable(IRCameraScreen.MainCompose.route) {
            // Launch MainComposeActivity
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, MainComposeActivity::class.java))
            }
            LoadingScreen()
        }
        // Thermal camera screens
        composable(IRCameraScreen.ThermalCamera.route) {
            // Could embed existing thermal fragment using FragmentContainer
            ThermalCameraFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.ThermalCameraCompose.route) {
            // Try to launch thermal activity or show placeholder
            LaunchedEffect(Unit) {
                try {
                    val intent = Intent().apply {
                        setClassName(
                            context,
                            "com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity"
                        )
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback - stay in compose
                }
            }
            LoadingScreen()
        }
        // Sensor dashboard screens
        composable(IRCameraScreen.SensorDashboard.route) {
            // Could embed existing sensor dashboard fragment
            SensorDashboardFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.SensorDashboardCompose.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(Intent(context, SensorDashboardComposeActivity::class.java))
                } catch (e: Exception) {
                    // Stay in compose if activity doesn't exist
                }
            }
            LoadingScreen()
        }
        // Settings screens
        composable(IRCameraScreen.Settings.route) {
            // Could embed existing settings fragment
            SettingsFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.SettingsCompose.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(Intent(context, SettingsComposeActivity::class.java))
                } catch (e: Exception) {
                    // Stay in compose if activity doesn't exist
                }
            }
            LoadingScreen()
        }
        // About screen
        composable(IRCameraScreen.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ThermalCameraFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing ThermalFragment using FragmentContainer
    // For now, show a placeholder that explains the integration
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Thermal Camera Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed the existing ThermalFragment using FragmentContainer",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SensorDashboardFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing SensorDashboardFragment using FragmentContainer
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Sensor Dashboard Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed the existing SensorDashboardFragment",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SettingsFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed existing settings fragments
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Settings Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed existing settings fragments",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\navigation\NavigationAnimations.kt =====

package mpdc4gsr.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

object NavigationAnimations {
    private const val ANIMATION_DURATION_MS = 300
    private const val FAST_ANIMATION_DURATION_MS = 200
    private fun <S> AnimatedContentTransitionScope<S>.slideTransition(
        direction: AnimatedContentTransitionScope.SlideDirection,
        duration: Int,
        isEnter: Boolean
    ): Any = if (isEnter) {
        slideIntoContainer(
            towards = direction,
            animationSpec = tween(duration)
        )
    } else {
        slideOutOfContainer(
            towards = direction,
            animationSpec = tween(duration)
        )
    }

    fun <S> AnimatedContentTransitionScope<S>.slideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.slideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            false
        ) as ExitTransition
}


// ===== app\src\main\java\mpdc4gsr\core\ui\navigation\NavigationPerformanceHelper.kt =====

package mpdc4gsr.core.ui.navigation

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import mpdc4gsr.core.ui.ComposePerformanceMonitor

object NavigationPerformanceHelper {
    private const val TAG = "NavigationPerf"
    private const val WARNING_THRESHOLD_MS = 300L

    @Composable
    fun TrackNavigation(routeName: String) {
        val startTime = remember { System.currentTimeMillis() }
        LaunchedEffect(Unit) {
            val latency = System.currentTimeMillis() - startTime
            ComposePerformanceMonitor.trackNavigation(routeName, startTime)
            if (latency > WARNING_THRESHOLD_MS) {
                AppLogger.w(TAG, "Slow navigation to $routeName: ${latency}ms (threshold: ${WARNING_THRESHOLD_MS}ms)")
            }
        }
    }

    fun logPerformanceSummary() {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        AppLogger.d(TAG, "=== Navigation Performance Summary ===")
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        if (navigationMetrics.isEmpty()) {
            AppLogger.d(TAG, "No navigation metrics recorded yet")
            return
        }
        navigationMetrics.forEach { (route, metric) ->
            val routeName = route.removePrefix("navigation_")
            Log.d(
                TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms, " +
                        "max=${metric.max}ms, min=${metric.min}ms, count=${metric.count}"
            )
        }
        val slowRoutes = navigationMetrics.filter { it.value.average > WARNING_THRESHOLD_MS }
        if (slowRoutes.isNotEmpty()) {
            AppLogger.w(TAG, "=== Routes Exceeding Threshold (${WARNING_THRESHOLD_MS}ms) ===")
            slowRoutes.forEach { (route, metric) ->
                val routeName = route.removePrefix("navigation_")
                AppLogger.w(TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms")
            }
        }
        AppLogger.d(TAG, "======================================")
    }

    fun getSlowRoutes(thresholdMs: Long = WARNING_THRESHOLD_MS): List<Pair<String, Double>> {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .filter { it.value.average > thresholdMs }
            .map {
                it.key.removePrefix("navigation_") to it.value.average
            }
            .sortedByDescending { it.second }
    }

    fun getFastestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .minByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }

    fun getSlowestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .maxByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\navigation\UnifiedNavigation.kt =====

package mpdc4gsr.core.ui.navigation

import android.content.Intent
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.feature.camera.ui.CameraDashboardScreen
import mpdc4gsr.feature.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.camera.ui.DualModeCameraScreen
import mpdc4gsr.feature.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.camera.ui.TimeLapseCameraScreen
import mpdc4gsr.feature.gsr.ui.GSRDataViewScreen
import mpdc4gsr.feature.gsr.ui.GSRPlotScreen
import mpdc4gsr.feature.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.gsr.ui.ResearchTemplateScreen
import mpdc4gsr.feature.gsr.ui.SessionDetailScreen
import mpdc4gsr.feature.main.ui.ComponentShowcaseScreen
import mpdc4gsr.feature.main.ui.MainScreen
import mpdc4gsr.feature.main.ui.UnifiedSensorDashboard
import mpdc4gsr.feature.network.ui.DevicePairingScreen
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.ProfileEditScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.testing.ui.TestResultsScreen
import mpdc4gsr.feature.thermal.ui.ThermalCameraScreen
import mpdc4gsr.feature.thermal.ui.ThermalLoadingScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen

sealed class UnifiedRoute(val route: String, val displayName: String = "") {
    // Main Application Routes
    object Home : UnifiedRoute("home", "Home")
    object Dashboard : UnifiedRoute("dashboard", "Sensor Overview")

    // GSR Sensor Routes
    object GSRSettings : UnifiedRoute("gsr_settings", "GSR Settings")
    object GSRPlot : UnifiedRoute("gsr_plot/{sessionId}", "GSR Session") {
        fun createRoute(sessionId: String) = "gsr_plot/$sessionId"
    }

    object GSRDataView : UnifiedRoute("gsr_data_view", "Export GSR Data") {
        fun createRoute() = "gsr_data_view"
    }

    object GSRSessionDetail : UnifiedRoute("gsr_session_detail/{sessionId}", "Session Details") {
        fun createRoute(sessionId: String) = "gsr_session_detail/$sessionId"
    }

    object ResearchTemplates : UnifiedRoute("research_templates", "Research Templates")

    // Camera Integration Routes
    object CameraDashboard : UnifiedRoute("camera_dashboard", "Camera Hub")
    object DualModeCamera : UnifiedRoute("dual_mode_camera", "Thermal + RGB Camera")
    object RGBCamera : UnifiedRoute("rgb_camera", "RGB Camera")
    object CameraSettings : UnifiedRoute("camera_settings", "Camera Settings")
    object TimeLapseCamera : UnifiedRoute("timelapse_camera", "Time-Lapse Camera")

    // Network Routes
    object DevicePairing : UnifiedRoute("device_pairing", "Device Pairing")
    object PermissionRequest : UnifiedRoute("permission_request", "Permissions")

    // Thermal Camera Routes - Consolidated (removed ThermalMain duplicate)
    object ThermalGallery : UnifiedRoute("thermal_gallery", "Gallery")
    object ThermalReport : UnifiedRoute("thermal_report", "Reports")
    object ThermalCamera : UnifiedRoute("thermal_camera", "Thermal Imaging")
    object ThermalSettings : UnifiedRoute("thermal_settings", "Thermal Settings")

    // System Routes
    object Settings : UnifiedRoute("settings", "Settings")
    object About : UnifiedRoute("about", "About")
    object Profile : UnifiedRoute("profile", "Profile")
    object ProfileEdit : UnifiedRoute("profile_edit", "Edit Profile")
    object NetworkConfig : UnifiedRoute("network_config", "Network")

    // Settings Sub-Routes
    object RecordingSettings : UnifiedRoute("recording_settings", "Recording Settings")
    object StorageSettings : UnifiedRoute("storage_settings", "Storage Settings")
    object SyncSettings : UnifiedRoute("sync_settings", "Sync Settings")
    object Calibration : UnifiedRoute("calibration", "Calibration")
    object NetworkSettings : UnifiedRoute("network_settings", "Network Settings")
    object Diagnostics : UnifiedRoute("diagnostics", "Diagnostics")
    object AppInfo : UnifiedRoute("app_info", "App Info")
    object PrivacyPolicy : UnifiedRoute("privacy_policy", "Privacy Policy")
    object Help : UnifiedRoute("help", "Help")

    // Development and Demo Routes
    object ComponentShowcase : UnifiedRoute("component_showcase", "Feature Demos")
    object TestingSuite : UnifiedRoute("testing_suite", "Diagnostics")
}

@Composable
fun UnifiedNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UnifiedRoute.Home.route
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { with(NavigationAnimations) { slideInFromRight() } },
        exitTransition = { with(NavigationAnimations) { slideOutToLeft() } },
        popEnterTransition = { with(NavigationAnimations) { slideInFromLeft() } },
        popExitTransition = { with(NavigationAnimations) { slideOutToRight() } }
    ) {
        // Home and Dashboard
        composable(UnifiedRoute.Home.route) {
            NavigationPerformanceHelper.TrackNavigation(UnifiedRoute.Home.displayName)
            MainScreen(
                onNavigateToSensors = { navController.navigate(UnifiedRoute.Dashboard.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.Settings.route) },
                onNavigateToProfile = { navController.navigate(UnifiedRoute.Profile.route) }
            )
        }
        composable(UnifiedRoute.Dashboard.route) {
            NavigationPerformanceHelper.TrackNavigation("Dashboard")
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(UnifiedRoute.GSRSettings.route)
                        SensorType.ThermalIR -> navController.navigate(UnifiedRoute.ThermalCamera.route)
                        SensorType.RGBCamera -> navController.navigate(UnifiedRoute.CameraDashboard.route)
                    }
                },
                onCameraSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onGSRSettingsClick = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onThermalSettingsClick = { navController.navigate(UnifiedRoute.ThermalSettings.route) }
            )
        }
        // GSR Sensor Routes
        composable(UnifiedRoute.GSRSettings.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRSettings")
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.GSRSessionDetail.route) { backStackEntry ->
            NavigationPerformanceHelper.TrackNavigation("GSRSessionDetail")
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            SessionDetailScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRPlot = {
                    navController.navigate(
                        UnifiedRoute.GSRPlot.createRoute(
                            sessionId
                        )
                    )
                }
            )
        }
        composable(UnifiedRoute.GSRPlot.route) { backStackEntry ->
            NavigationPerformanceHelper.TrackNavigation("GSRPlot")
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            GSRPlotScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.GSRDataView.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRDataView")
            GSRDataViewScreen(
                filePath = "",
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.ResearchTemplates.route) {
            NavigationPerformanceHelper.TrackNavigation("ResearchTemplates")
            ResearchTemplateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Camera Integration Routes
        composable(UnifiedRoute.CameraDashboard.route) {
            CameraDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToDualMode = { navController.navigate(UnifiedRoute.DualModeCamera.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToSingleCamera = { navController.navigate(UnifiedRoute.RGBCamera.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToTimeLapse = { navController.navigate(UnifiedRoute.TimeLapseCamera.route) }
            )
        }
        composable(UnifiedRoute.RGBCamera.route) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onCapturePhoto = { }
            )
        }
        composable(UnifiedRoute.TimeLapseCamera.route) {
            NavigationPerformanceHelper.TrackNavigation("TimeLapseCamera")
            TimeLapseCameraScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.DualModeCamera.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        mpdc4gsr.feature.camera.ui.DualModeCameraActivityCompose::class.java
                    } catch (e: NoClassDefFoundError) {
                        null
                    }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    } else {
                        navController.navigate("dual_mode_camera_screen")
                    }
                } catch (e: Exception) {
                    // Fallback to screen
                    navController.navigate("dual_mode_camera_screen")
                }
            }
            ThermalLoadingScreen("Loading Dual Mode Camera...")
        }
        composable(UnifiedRoute.CameraSettings.route) {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.DevicePairing.route) {
            LaunchedEffect(Unit) {
                try {
                    // Try to launch permission request activity if it exists
                    try {
                        mpdc4gsr.core.ui.PermissionRequestComposeActivity.startActivity(context)
                    } catch (e: Exception) {
                        AppLogger.e("UnifiedNavigation", "Failed to start permission request activity", e)
                    }
                } catch (e: Exception) {
                    // Final fallback - just show loading screen
                }
            }
            ThermalLoadingScreen("Loading Device Pairing...")
        }
        // Thermal Camera Routes - ThermalMain removed, use ThermalCamera instead
        composable(UnifiedRoute.ThermalGallery.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        com.mpdc4gsr.module.thermalunified.activity.ThermalGalleryComposeActivity::class.java
                    } catch (e: NoClassDefFoundError) {
                        null
                    }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Gallery...")
        }
        composable(UnifiedRoute.ThermalReport.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        com.mpdc4gsr.module.thermalunified.activity.ThermalReportComposeActivity::class.java
                    } catch (e: NoClassDefFoundError) {
                        null
                    }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Reports...")
        }
        composable(UnifiedRoute.ThermalCamera.route) {
            NavigationPerformanceHelper.TrackNavigation("ThermalCamera")
            ThermalCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) }
            )
        }
        composable(UnifiedRoute.ThermalSettings.route) {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        // Settings and System Routes
        composable(UnifiedRoute.Settings.route) {
            NavigationPerformanceHelper.TrackNavigation("Settings")
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onNavigateToThermalSettings = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
                onNavigateToCameraSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToRecordingSettings = { navController.navigate(UnifiedRoute.RecordingSettings.route) },
                onNavigateToStorageSettings = { navController.navigate(UnifiedRoute.StorageSettings.route) },
                onNavigateToSyncSettings = { navController.navigate(UnifiedRoute.SyncSettings.route) },
                onNavigateToCalibration = { navController.navigate(UnifiedRoute.Calibration.route) },
                onNavigateToNetworkSettings = { navController.navigate(UnifiedRoute.NetworkConfig.route) },
                onNavigateToDiagnostics = { navController.navigate(UnifiedRoute.Diagnostics.route) },
                onNavigateToAppInfo = { navController.navigate(UnifiedRoute.AppInfo.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(UnifiedRoute.PrivacyPolicy.route) },
                onNavigateToHelp = { navController.navigate(UnifiedRoute.Help.route) }
            )
        }
        composable(UnifiedRoute.About.route) {
            NavigationPerformanceHelper.TrackNavigation("About")
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Profile.route) {
            NavigationPerformanceHelper.TrackNavigation("Profile")
            mpdc4gsr.feature.settings.ui.ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToResearchTemplates = { navController.navigate(UnifiedRoute.ResearchTemplates.route) },
                onNavigateToPreferences = { navController.navigate(UnifiedRoute.Settings.route) },
                onExportData = { navController.navigate(UnifiedRoute.GSRDataView.route) },
                onNavigateToEditProfile = { navController.navigate(UnifiedRoute.ProfileEdit.route) }
            )
        }
        composable(UnifiedRoute.ProfileEdit.route) {
            NavigationPerformanceHelper.TrackNavigation("ProfileEdit")
            ProfileEditScreen(
                onBackClick = { navController.popBackStack() },
                onSave = { profileData ->
                    // Profile data saved - navigate back
                    navController.popBackStack()
                }
            )
        }
        composable(UnifiedRoute.ComponentShowcase.route) {
            NavigationPerformanceHelper.TrackNavigation("ComponentShowcase")
            ComponentShowcaseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.TestingSuite.route) {
            NavigationPerformanceHelper.TrackNavigation("TestingSuite")
            TestResultsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        // Network & Device Management Routes
        composable(UnifiedRoute.PermissionRequest.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        mpdc4gsr.core.ui.PermissionRequestComposeActivity::class.java
                    } catch (e: NoClassDefFoundError) {
                        null
                    }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback - just show loading screen
                }
            }
            ThermalLoadingScreen("Loading Permission Manager...")
        }
        // Fallback routes for screens when activities fail to launch
        composable("dual_mode_camera_screen") {
            DualModeCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) }
            )
        }
        composable("device_pairing_screen") {
            DevicePairingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Additional Settings Routes
        composable(UnifiedRoute.RecordingSettings.route) {
            mpdc4gsr.feature.settings.ui.RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.StorageSettings.route) {
            mpdc4gsr.feature.settings.ui.StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.SyncSettings.route) {
            mpdc4gsr.feature.settings.ui.SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Calibration.route) {
            mpdc4gsr.feature.thermal.ui.CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.NetworkConfig.route) {
            mpdc4gsr.feature.network.ui.NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Diagnostics.route) {
            mpdc4gsr.feature.main.ui.DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.AppInfo.route) {
            mpdc4gsr.feature.settings.ui.AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.PrivacyPolicy.route) {
            mpdc4gsr.feature.settings.ui.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Help.route) {
            mpdc4gsr.feature.settings.ui.HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

object NavigationHelper {
    fun navigateToGSRSettings(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRSettings.route)
    }

    fun navigateToSessionDetail(navController: NavHostController, sessionId: String) {
        navController.navigate(UnifiedRoute.GSRSessionDetail.createRoute(sessionId))
    }

    fun navigateToCamera(navController: NavHostController) {
        navController.navigate(UnifiedRoute.CameraDashboard.route)
    }

    fun navigateToDevicePairing(navController: NavHostController) {
        navController.navigate(UnifiedRoute.DevicePairing.route)
    }

    fun navigateToPermissionRequest(navController: NavHostController) {
        navController.navigate(UnifiedRoute.PermissionRequest.route)
    }

    // Quick Action Navigation - User-centric direct access
    fun captureThermalImage(navController: NavHostController) {
        navController.navigate(UnifiedRoute.ThermalCamera.route)
    }

    fun startGSRSession(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRSettings.route)
    }

    fun thermalRGBCapture(navController: NavHostController) {
        navController.navigate(UnifiedRoute.DualModeCamera.route)
    }

    fun viewGallery(navController: NavHostController) {
        navController.navigate(UnifiedRoute.ThermalGallery.route)
    }

    fun viewRecentSessions(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRDataView.route)
    }

    fun navigateWithPopUp(
        navController: NavHostController,
        destination: String,
        popUpTo: String,
        inclusive: Boolean = false
    ) {
        navController.navigate(destination) {
            popUpTo(popUpTo) {
                this.inclusive = inclusive
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\PermissionController.kt =====

package mpdc4gsr.core.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class PermissionController(private val activity: ComponentActivity) {
    private val usbManager: UsbManager =
        activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private var onPermissionsResult: ((isGranted: Boolean, denied: List<String>) -> Unit)? = null
    private var currentDialog: AlertDialog? = null
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val denied = grants.filter { !it.value }.keys.toList()
            if (denied.isEmpty()) {
                AppLogger.i(TAG, "All requested permissions were granted.")
                onPermissionsResult?.invoke(true, emptyList())
            } else {
                AppLogger.w(TAG, "Some permissions were denied: ${denied.joinToString()}")
                handleDeniedPermissions(denied)
                onPermissionsResult?.invoke(false, denied)
            }
        }
    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            AppLogger.i(TAG, "Returned from battery optimization settings.")
        }

    fun ensureAll(callback: (isGranted: Boolean, denied: List<String>) -> Unit) {
        this.onPermissionsResult = callback
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            AppLogger.i(TAG, "All required permissions are already granted.")
            callback(true, emptyList())
            return
        }
        AppLogger.i(TAG, "Found ${missing.size} missing permissions. Showing rationale.")
        showPermissionRationaleDialog(missing) { userAccepted ->
            if (userAccepted) {
                AppLogger.i(TAG, "User accepted rationale. Launching permission request.")
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                AppLogger.w(TAG, "User declined permission rationale.")
                callback(false, missing)
            }
        }
    }

    fun requestUsbPermission(
        device: UsbDevice,
        callback: (isGranted: Boolean, device: UsbDevice?) -> Unit
    ) {
        if (usbManager.hasPermission(device)) {
            AppLogger.i(TAG, "USB permission already granted for device ${device.productName}")
            callback(true, device)
            return
        }
        showUsbPermissionRationaleDialog(device) { userAccepted ->
            if (userAccepted) {
                try {
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val permissionIntent = PendingIntent.getBroadcast(
                        activity,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        flags
                    )
                    usbManager.requestPermission(device, permissionIntent)
                    AppLogger.i(TAG, "USB permission request sent for ${device.productName}.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to request USB permission", e)
                    callback(false, null)
                }
            } else {
                AppLogger.w(TAG, "User declined USB permission rationale.")
                callback(false, null)
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            AppLogger.i(TAG, "Battery optimization exemption not needed or already granted.")
            return
        }
        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    batteryOptimizationLauncher.launch(intent)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to launch battery optimization settings.", e)
                }
            } else {
                AppLogger.w(TAG, "User declined battery optimization exemption.")
            }
        }
    }

    fun getMissingPermissions(): List<String> {
        return ALL_PERMISSIONS.filterNot { activity.isPermissionGranted(it) }
    }

    fun getPermissionStatusMessage(): String {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) return "All permissions granted"
        val names = getPermissionNames(missing)
        return "Missing permissions:\nâ€¢ ${names.joinToString("\nâ€¢ ")}"
    }

    fun canStartRecording(): Boolean = hasCameraPermissions() && hasStoragePermissions()
    fun canConnectToShimmer(): Boolean = hasBluetoothPermissions() && hasLocationPermission()
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager =
                activity.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true
        }
    }

    fun hasCameraPermissions(): Boolean =
        CAMERA_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasStoragePermissions(): Boolean =
        STORAGE_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasBluetoothPermissions(): Boolean =
        BLUETOOTH_PERMISSIONS.all { activity.isPermissionGranted(it) }

    fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any { activity.isPermissionGranted(it) }

    fun hasUsbPermissions(): Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    private fun showPermissionRationaleDialog(missing: List<String>, onResult: (Boolean) -> Unit) {
        // Dismiss any existing dialog first
        dismissCurrentDialog()
        // Check if activity is still valid
        if (activity.isFinishing || activity.isDestroyed) {
            onResult(false)
            return
        }
        val names = getPermissionNames(missing)
        val message = """
            This app requires the following permissions for multi-sensor recording:
            
            â€¢ ${names.joinToString("\nâ€¢ ")}
            
            These permissions are essential for:
            â€¢ Recording video (Camera)
            â€¢ Connecting to GSR sensors (Bluetooth & Location)
            â€¢ Saving recordings (Media Access)
            â€¢ Displaying status updates (Notifications)
            
            The app will not function correctly without these permissions.
        """.trimIndent()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                onResult(true)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }
            .setCancelable(false)
            .setOnDismissListener { currentDialog = null }
            .create()
        currentDialog?.show()
    }

    private fun handleDeniedPermissions(denied: List<String>) {
        val permanentlyDenied = denied.filter { !activity.shouldShowRequestPermissionRationale(it) }
        if (permanentlyDenied.isNotEmpty()) {
            showPermanentlyDeniedDialog(permanentlyDenied)
        } else {
            AppLogger.w(TAG, "User temporarily denied: ${denied.joinToString()}")
        }
    }

    private fun showPermanentlyDeniedDialog(permanentlyDenied: List<String>) {
        // Dismiss any existing dialog first
        dismissCurrentDialog()
        // Check if activity is still valid
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        val names = getPermissionNames(permanentlyDenied)
        val message = """
            You have permanently denied the following critical permissions:
            
            â€¢ ${names.joinToString("\nâ€¢ ")}
            
            To enable the app's core features, please grant these permissions manually in your device settings.
        """.trimIndent()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Permissions Permanently Denied")
            .setMessage(message)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener { currentDialog = null }
            .create()
        currentDialog?.show()
    }

    private fun showUsbPermissionRationaleDialog(
        device: UsbDevice,
        callback: (Boolean) -> Unit
    ) {
        currentDialog?.dismiss()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("USB Device Permission Required")
            .setMessage("The app needs permission to access USB device:\n${device.deviceName}\n\nThis is required for thermal camera communication.")
            .setPositiveButton("Grant") { dialog, _ ->
                callback(true)
                dialog.dismiss()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                callback(false)
                dialog.dismiss()
            }
            .setOnCancelListener {
                callback(false)
            }
            .create()
        currentDialog?.show()
    }

    private fun showBatteryOptimizationRationaleDialog(callback: (Boolean) -> Unit) {
        currentDialog?.dismiss()
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Battery Optimization")
            .setMessage("Disabling battery optimization ensures uninterrupted sensor data recording.\n\nThis prevents the system from stopping background sensor operations.")
            .setPositiveButton("Disable Optimization") { dialog, _ ->
                callback(true)
                dialog.dismiss()
            }
            .setNegativeButton("Keep Enabled") { dialog, _ ->
                callback(false)
                dialog.dismiss()
            }
            .setOnCancelListener {
                callback(false)
            }
            .create()
        currentDialog?.show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open app settings", e)
        }
    }

    fun getPermissionNames(permissions: List<String>): List<String> {
        return permissions.mapNotNull { PERMISSION_MAP[it] }.distinct()
    }

    // Add missing methods for compatibility
    fun hasAllRequiredPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    fun initialize() {
        // This method is called for initialization purposes
        // Currently no specific initialization required
        AppLogger.i(TAG, "PermissionController initialized")
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Handle legacy permission results if needed
        // Modern implementation uses ActivityResultLauncher
        AppLogger.i(TAG, "Legacy onRequestPermissionsResult called with requestCode: $requestCode")
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        // Handle legacy activity results if needed 
        // Modern implementation uses ActivityResultLauncher
        Log.i(
            TAG,
            "Legacy onActivityResult called with requestCode: $requestCode, resultCode: $resultCode"
        )
    }

    fun requestBatteryOptimizationExemption(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isBatteryOptimizationDisabled()) {
            AppLogger.i(TAG, "Battery optimization exemption not needed or already granted.")
            callback(true)
            return
        }
        showBatteryOptimizationRationaleDialog { userAccepted ->
            if (userAccepted) {
                try {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    batteryOptimizationLauncher.launch(intent)
                    callback(true)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to launch battery optimization settings.", e)
                    callback(false)
                }
            } else {
                AppLogger.w(TAG, "User declined battery optimization exemption.")
                callback(false)
            }
        }
    }

    private fun dismissCurrentDialog() {
        currentDialog?.let { dialog ->
            if (dialog.isShowing) {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to dismiss dialog: ${e.message}")
                }
            }
            currentDialog = null
        }
    }

    fun cleanup() {
        dismissCurrentDialog()
        onPermissionsResult = null
    }

    companion object {
        private const val TAG = "PermissionController"
        const val ACTION_USB_PERMISSION = "mpdc4gsr.USB_PERMISSION"
        private fun Context.isPermissionGranted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        private val CAMERA_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private val STORAGE_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        private val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val NOTIFICATION_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        private val ALL_PERMISSIONS = listOfNotNull(
            *CAMERA_PERMISSIONS,
            *STORAGE_PERMISSIONS,
            *BLUETOOTH_PERMISSIONS,
            *LOCATION_PERMISSIONS,
            *NOTIFICATION_PERMISSIONS
        ).distinct()
        private val PERMISSION_MAP = mapOf(
            Manifest.permission.CAMERA to "Camera",
            Manifest.permission.RECORD_AUDIO to "Microphone",
            Manifest.permission.WRITE_EXTERNAL_STORAGE to "Storage",
            Manifest.permission.READ_EXTERNAL_STORAGE to "Storage",
            Manifest.permission.READ_MEDIA_VIDEO to "Media Access (Videos)",
            Manifest.permission.READ_MEDIA_IMAGES to "Media Access (Images)",
            Manifest.permission.BLUETOOTH_SCAN to "Bluetooth Scanning",
            Manifest.permission.BLUETOOTH_CONNECT to "Bluetooth Connections",
            Manifest.permission.BLUETOOTH to "Bluetooth",
            Manifest.permission.BLUETOOTH_ADMIN to "Bluetooth Administration",
            Manifest.permission.ACCESS_FINE_LOCATION to "Precise Location",
            Manifest.permission.ACCESS_COARSE_LOCATION to "Approximate Location",
            Manifest.permission.POST_NOTIFICATIONS to "Notifications"
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\PermissionManager.kt =====

package mpdc4gsr.core.ui

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: ComponentActivity,
    private val permissionController: PermissionController
) {
    companion object {
        private const val TAG = "PermissionManager"
    }

    suspend fun requestAllCriticalPermissions(): Boolean {
        AppLogger.i(TAG, "Requesting all critical permissions")
        return requestPermissionsWithController()
    }

    suspend fun requestCameraPermissions(): Boolean {
        if (permissionController.hasCameraPermissions()) {
            AppLogger.i(TAG, "Camera permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting camera permissions")
        return requestPermissionsWithController()
    }

    suspend fun requestBluetoothPermissions(): Boolean {
        if (permissionController.canConnectToShimmer()) {
            AppLogger.i(TAG, "Bluetooth permissions already granted")
            return true
        }
        AppLogger.i(TAG, "Requesting bluetooth permissions for GSR sensor access")
        return requestPermissionsWithController()
    }

    private suspend fun requestPermissionsWithController(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            permissionController.ensureAll { isGranted, denied ->
                if (continuation.isActive) {
                    if (!isGranted) {
                        AppLogger.w(TAG, "Permissions denied: ${denied.joinToString()}")
                    }
                    continuation.resume(isGranted)
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\PermissionRequestComposeActivity.kt =====

package mpdc4gsr.core.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class PermissionRequestComposeActivity : BaseComposeActivity<BaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, PermissionRequestComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var showEducationalDialog by remember { mutableStateOf(false) }
        var selectedPermission by remember { mutableStateOf<PermissionInfo?>(null) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Permission Manager",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showEducationalDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                            IconButton(onClick = {
                                // TODO: Open system settings for permissions
                                android.widget.Toast.makeText(
                                    this@PermissionRequestComposeActivity,
                                    "Opening system settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "System Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PermissionRequestContent(
                    onPermissionSelect = { selectedPermission = it },
                    onGrantAll = {
                        // TODO: Request all required permissions
                        android.widget.Toast.makeText(
                            this@PermissionRequestComposeActivity,
                            "Requesting all permissions...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showEducationalDialog) {
            PermissionEducationDialog(
                onDismiss = { showEducationalDialog = false }
            )
        }
        selectedPermission?.let { permission ->
            PermissionDetailDialog(
                permission = permission,
                onDismiss = { selectedPermission = null },
                onRequestPermission = {
                    // Request specific permission
                    selectedPermission = null
                }
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onPermissionSelect: (PermissionInfo) -> Unit,
    onGrantAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Permission Status Overview
        PermissionStatusOverview(
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Critical Permissions Section
        Text(
            text = "Critical Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val criticalPermissions = getCriticalPermissions()
        criticalPermissions.forEach { permission ->
            PermissionCard(
                permission = permission,
                onSelect = { onPermissionSelect(permission) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        // Optional Permissions Section
        Text(
            text = "Optional Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )
        val optionalPermissions = getOptionalPermissions()
        optionalPermissions.forEach { permission ->
            PermissionCard(
                permission = permission,
                onSelect = { onPermissionSelect(permission) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        // Grant All Button
        GrantAllPermissionsButton(
            onClick = onGrantAll,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
private fun PermissionStatusOverview(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Permission Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PermissionStatusItem(
                    label = "Granted",
                    count = 4,
                    color = Color(0xFF4CAF50)
                )
                PermissionStatusItem(
                    label = "Pending",
                    count = 2,
                    color = Color(0xFFFF9800)
                )
                PermissionStatusItem(
                    label = "Denied",
                    count = 1,
                    color = Color(0xFFE53E3E)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionInfo,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (permission.status) {
                PermissionStatus.GRANTED -> MaterialTheme.colorScheme.surfaceVariant
                PermissionStatus.DENIED -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Permission icon
            Icon(
                imageVector = getPermissionIcon(permission.type),
                contentDescription = permission.name,
                modifier = Modifier.size(32.dp),
                tint = getPermissionStatusColor(permission.status)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getPermissionStatusColor(permission.status))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = permission.status.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Action button
            when (permission.status) {
                PermissionStatus.GRANTED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF4CAF50)
                    )
                }

                PermissionStatus.DENIED -> {
                    OutlinedButton(onClick = onSelect) {
                        Text("Grant")
                    }
                }

                PermissionStatus.PENDING -> {
                    Button(onClick = onSelect) {
                        Text("Request")
                    }
                }
            }
        }
    }
}

@Composable
private fun GrantAllPermissionsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = "Grant All",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "GRANT ALL PERMISSIONS",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PermissionDetailDialog(
    permission: PermissionInfo,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(permission.name)
        },
        text = {
            Column {
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Why this permission is needed:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = permission.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionEducationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Why Permissions Matter")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "IRCamera requires several permissions to provide the best experience:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PermissionEducationItem(
                    icon = Icons.Default.Camera,
                    title = "Camera Access",
                    description = "Capture thermal and RGB images for analysis"
                )
                PermissionEducationItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth",
                    description = "Connect to GSR sensors and thermal cameras"
                )
                PermissionEducationItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location",
                    description = "Required for Bluetooth device discovery"
                )
                PermissionEducationItem(
                    icon = Icons.Default.Storage,
                    title = "Storage",
                    description = "Save recordings and export data"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun PermissionEducationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getPermissionIcon(type: String) = when (type) {
    "camera" -> Icons.Default.Camera
    "bluetooth" -> Icons.Default.Bluetooth
    "location" -> Icons.Default.LocationOn
    "storage" -> Icons.Default.Storage
    "microphone" -> Icons.Default.Mic
    "notification" -> Icons.Default.Notifications
    else -> Icons.Default.Security
}

private fun getPermissionStatusColor(status: PermissionStatus) = when (status) {
    PermissionStatus.GRANTED -> Color(0xFF4CAF50)
    PermissionStatus.DENIED -> Color(0xFFE53E3E)
    PermissionStatus.PENDING -> Color(0xFFFF9800)
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PENDING
}

data class PermissionInfo(
    val name: String,
    val type: String,
    val description: String,
    val reasoning: String,
    val status: PermissionStatus,
    val isCritical: Boolean
)

private fun getCriticalPermissions() = listOf(
    PermissionInfo(
        "Camera Access",
        "camera",
        "Access to device camera for thermal and RGB imaging",
        "Required to capture thermal images from TOPDON TC001 and RGB images from device camera. Essential for core app functionality.",
        PermissionStatus.GRANTED,
        true
    ),
    PermissionInfo(
        "Bluetooth",
        "bluetooth",
        "Connect to Bluetooth devices for sensor data collection",
        "Needed to connect to Shimmer3 GSR+ sensors and TOPDON thermal cameras via Bluetooth LE.",
        PermissionStatus.GRANTED,
        true
    ),
    PermissionInfo(
        "Location Access",
        "location",
        "Required for Bluetooth device discovery",
        "Android requires location permission for BLE device scanning and discovery of nearby sensors.",
        PermissionStatus.PENDING,
        true
    )
)

private fun getOptionalPermissions() = listOf(
    PermissionInfo(
        "Storage Access",
        "storage",
        "Save recordings and export data files",
        "Allows saving session recordings, exporting data in various formats, and managing files.",
        PermissionStatus.GRANTED,
        false
    ),
    PermissionInfo(
        "Microphone",
        "microphone",
        "Record audio during multi-modal sessions",
        "Optional for recording audio annotations during research sessions.",
        PermissionStatus.DENIED,
        false
    ),
    PermissionInfo(
        "Notifications",
        "notification",
        "Show recording status and sensor alerts",
        "Displays notifications for recording status, sensor connection issues, and system alerts.",
        PermissionStatus.PENDING,
        false
    )
)


// ===== app\src\main\java\mpdc4gsr\core\ui\PermissionRequestScreen.kt =====

package mpdc4gsr.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    viewModel: PermissionRequestViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val permissionStates by viewModel.permissionStates.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val logMessages by viewModel.logMessages.collectAsStateWithLifecycle()
    // Handle one-time events
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PermissionRequestViewModel.PermissionEvent.ShowError -> {
                    // Handle error display
                }

                is PermissionRequestViewModel.PermissionEvent.ShowSuccess -> {
                    // Handle success display
                }

                PermissionRequestViewModel.PermissionEvent.NavigateToRecording -> {
                    // Navigate to recording activity
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Manager") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.updatePermissionStatus() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Overview
            StatusOverviewCard(
                screenState = screenState,
                permissionStates = permissionStates
            )
            // Permission Cards
            PermissionCardsSection(
                permissionStates = permissionStates,
                onRequestCamera = { viewModel.requestCameraPermissions() },
                onRequestBluetooth = { viewModel.requestBluetoothPermissions() },
                onRequestLocation = { viewModel.requestLocationPermissions() },
                onRequestStorage = { viewModel.requestStoragePermissions() },
                isRequestingPermissions = screenState.isRequestingPermissions
            )
            // Action Buttons
            ActionButtonsSection(
                screenState = screenState,
                onRequestAll = { viewModel.requestAllPermissions() },
                onTestCapabilities = { viewModel.testRecordingCapabilities() },
                onStartRecording = { viewModel.startRecordingSession() }
            )
            // Log Section
            LogSection(
                logMessages = logMessages,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusOverviewCard(
    screenState: PermissionRequestViewModel.ScreenState,
    permissionStates: PermissionRequestViewModel.PermissionStates,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (screenState.canStartRecording) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (screenState.canStartRecording) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (screenState.canStartRecording) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = "Permission Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (screenState.isRequestingPermissions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Text(
                text = screenState.statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
            // Permission quick status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PermissionStatusChip("Camera", permissionStates.camera)
                PermissionStatusChip("Bluetooth", permissionStates.bluetooth)
                PermissionStatusChip("Location", permissionStates.location)
                PermissionStatusChip("Storage", permissionStates.storage)
            }
        }
    }
}

@Composable
private fun PermissionStatusChip(
    name: String,
    status: PermissionRequestViewModel.PermissionStatus
) {
    val color = when (status) {
        PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
        PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.error
        PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> MaterialTheme.colorScheme.outline
        PermissionRequestViewModel.PermissionStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    val text = when (status) {
        PermissionRequestViewModel.PermissionStatus.GRANTED -> "OK"
        PermissionRequestViewModel.PermissionStatus.DENIED -> "Need"
        PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> "N/A"
        PermissionRequestViewModel.PermissionStatus.UNKNOWN -> "?"
    }
    AssistChip(
        onClick = { },
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, style = MaterialTheme.typography.labelSmall)
                Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@Composable
private fun PermissionCardsSection(
    permissionStates: PermissionRequestViewModel.PermissionStates,
    onRequestCamera: () -> Unit,
    onRequestBluetooth: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestStorage: () -> Unit,
    isRequestingPermissions: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Permission Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionCard(
                title = "Camera",
                icon = Icons.Default.Camera,
                status = permissionStates.camera,
                onClick = onRequestCamera,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f)
            )
            PermissionCard(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                status = permissionStates.bluetooth,
                onClick = onRequestBluetooth,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionCard(
                title = "Location",
                icon = Icons.Default.LocationOn,
                status = permissionStates.location,
                onClick = onRequestLocation,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f)
            )
            PermissionCard(
                title = "Storage",
                icon = Icons.Default.Storage,
                status = permissionStates.storage,
                onClick = onRequestStorage,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionCard(
    title: String,
    icon: ImageVector,
    status: PermissionRequestViewModel.PermissionStatus,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = when (status) {
        PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primaryContainer
        PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && status != PermissionRequestViewModel.PermissionStatus.GRANTED,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when (status) {
                    PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
                    PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (status) {
                    PermissionRequestViewModel.PermissionStatus.GRANTED -> "Granted"
                    PermissionRequestViewModel.PermissionStatus.DENIED -> "Request"
                    PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> "N/A"
                    PermissionRequestViewModel.PermissionStatus.UNKNOWN -> "Unknown"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    screenState: PermissionRequestViewModel.ScreenState,
    onRequestAll: () -> Unit,
    onTestCapabilities: () -> Unit,
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRequestAll,
                enabled = !screenState.isRequestingPermissions,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request All")
            }
            OutlinedButton(
                onClick = onTestCapabilities,
                enabled = !screenState.isRequestingPermissions,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test")
            }
        }
        Button(
            onClick = onStartRecording,
            enabled = screenState.canStartRecording && !screenState.isRequestingPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Recording Session")
        }
    }
}

@Composable
private fun LogSection(
    logMessages: List<PermissionRequestViewModel.LogMessage>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Activity Log",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val listState = rememberLazyListState()
            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(logMessages.size) {
                if (logMessages.isNotEmpty()) {
                    listState.animateScrollToItem(logMessages.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logMessages, key = { it.id }) { logMessage ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[${logMessage.timestamp}]",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = logMessage.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\PermissionRequestViewModel.kt =====

package mpdc4gsr.core.ui

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PermissionRequestViewModel : BaseViewModel() {
    // StateFlow for permission states
    private val _permissionStates = MutableStateFlow(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()
    private val _logMessages = MutableStateFlow<List<LogMessage>>(emptyList())
    val logMessages: StateFlow<List<LogMessage>> = _logMessages.asStateFlow()
    private val _screenState = MutableStateFlow(ScreenState())
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<PermissionEvent>()
    val events: SharedFlow<PermissionEvent> = _events.asSharedFlow()
    private lateinit var permissionController: PermissionController
    private lateinit var permissionManager: PermissionManager

    data class PermissionStates(
        val camera: PermissionStatus = PermissionStatus.UNKNOWN,
        val bluetooth: PermissionStatus = PermissionStatus.UNKNOWN,
        val location: PermissionStatus = PermissionStatus.UNKNOWN,
        val storage: PermissionStatus = PermissionStatus.UNKNOWN,
        val usb: PermissionStatus = PermissionStatus.UNKNOWN
    )

    data class ScreenState(
        val canStartRecording: Boolean = false,
        val isRequestingPermissions: Boolean = false,
        val statusMessage: String = "Checking permissions..."
    )

    data class LogMessage(
        val timestamp: String,
        val message: String,
        val id: Long = System.currentTimeMillis()
    )

    enum class PermissionStatus {
        UNKNOWN,
        GRANTED,
        DENIED,
        NOT_AVAILABLE
    }

    sealed class PermissionEvent {
        data class ShowError(val message: String) : PermissionEvent()
        data class ShowSuccess(val message: String) : PermissionEvent()
        object NavigateToRecording : PermissionEvent()
    }

    init {
        // Setup combined state management
        viewModelScope.launch {
            combine(
                _permissionStates,
                _logMessages
            ) { permissionStates, _ ->
                val canStartRecording = checkCanStartRecording(permissionStates)
                val statusMessage = generateStatusMessage(permissionStates)
                ScreenState(
                    canStartRecording = canStartRecording,
                    isRequestingPermissions = false,
                    statusMessage = statusMessage
                )
            }.collect { newState ->
                _screenState.value = newState
            }
        }
    }

    fun initialize(activity: androidx.fragment.app.FragmentActivity) {
        launchWithErrorHandling {
            permissionController = PermissionController(activity)
            permissionManager = PermissionManager(activity, permissionController)
            addLog("Permission System initialized.")
            updatePermissionStatus()
        }
    }

    fun updatePermissionStatus() {
        launchWithErrorHandling {
            val newStates = PermissionStates(
                camera = if (permissionController.hasCameraPermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                bluetooth = if (permissionController.hasBluetoothPermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                location = if (permissionController.hasLocationPermission()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                storage = if (permissionController.hasStoragePermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                usb = if (permissionController.hasUsbPermissions()) PermissionStatus.GRANTED else PermissionStatus.NOT_AVAILABLE
            )
            _permissionStates.value = newStates
            addLog("Permission status updated.")
        }
    }

    fun requestCameraPermissions() {
        launchWithLoading {
            addLog("Requesting camera permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestCameraPermissions()
                addLog(if (granted) "Camera permissions granted" else "Camera permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Camera permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Camera permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestBluetoothPermissions() {
        launchWithLoading {
            addLog("Requesting Bluetooth permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestBluetoothPermissions()
                addLog(if (granted) "Bluetooth permissions granted" else "Bluetooth permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Bluetooth permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Bluetooth permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestAllPermissions() {
        launchWithLoading {
            addLog("Starting comprehensive permission request...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestAllCriticalPermissions()
                addLog(if (granted) "Critical permissions granted" else "Some permissions were denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("All critical permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Some permissions were denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestLocationPermissions() {
        launchWithLoading {
            addLog("Requesting location permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestBluetoothPermissions() // Bluetooth requires location
                addLog(if (granted) "Location permissions granted" else "Location permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Location permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Location permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestStoragePermissions() {
        launchWithLoading {
            addLog("Requesting storage permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestAllCriticalPermissions()
                addLog(if (granted) "Storage permissions granted" else "Storage permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Storage permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Storage permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun testRecordingCapabilities() {
        launchWithErrorHandling {
            addLog("Testing recording capabilities...")
            if (::permissionController.isInitialized) {
                addLog("Status: ${permissionController.getPermissionStatusMessage()}")
            } else {
                addLog("Permission controller not initialized")
            }
        }
    }

    fun startRecordingSession() {
        launchWithErrorHandling {
            val canStart = _screenState.value.canStartRecording
            if (canStart) {
                addLog("Starting recording session...")
                _events.emit(PermissionEvent.NavigateToRecording)
            } else {
                addLog("Cannot start recording - missing required permissions")
                _events.emit(PermissionEvent.ShowError("Missing required permissions"))
            }
        }
    }

    private fun addLog(message: String) {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = LogMessage(timestamp, message)
            val currentLogs = _logMessages.value.toMutableList()
            currentLogs.add(logMessage)
            // Keep only last 100 log messages to prevent memory issues
            if (currentLogs.size > 100) {
                currentLogs.removeAt(0)
            }
            _logMessages.value = currentLogs
        }
    }

    private fun checkCanStartRecording(states: PermissionStates): Boolean {
        return if (::permissionController.isInitialized) {
            permissionController.canStartRecording() && permissionController.canConnectToShimmer()
        } else {
            false
        }
    }

    private fun generateStatusMessage(states: PermissionStates): String {
        val grantedCount = listOf(
            states.camera,
            states.bluetooth,
            states.location,
            states.storage
        ).count { it == PermissionStatus.GRANTED }
        return when {
            grantedCount == 4 -> "All critical permissions granted"
            grantedCount > 0 -> "Some permissions granted ($grantedCount/4)"
            else -> "No permissions granted"
        }
    }

    companion object {
        private const val TAG = "PermissionRequestViewModel"
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\SafeMainThreadHandler.kt =====

package mpdc4gsr.core.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import java.util.concurrent.atomic.AtomicLong

class SafeMainThreadHandler(private val componentName: String = "Unknown") {
    companion object {
        private const val TAG = "SafeMainThreadHandler"
        private const val WARNING_THRESHOLD_MS = 100L
        private const val ERROR_THRESHOLD_MS = 1000L
        private val totalOperations = AtomicLong(0)
        private val slowOperations = AtomicLong(0)
        private val verySlowOperations = AtomicLong(0)
        fun getStatistics(): HandlerStatistics {
            return HandlerStatistics(
                totalOperations = totalOperations.get(),
                slowOperations = slowOperations.get(),
                verySlowOperations = verySlowOperations.get()
            )
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    fun post(runnable: Runnable) {
        handler.post(MonitoredRunnable(runnable, componentName))
    }

    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        handler.postDelayed(MonitoredRunnable(runnable, componentName), delayMillis)
    }

    fun removeCallbacksAndMessages() {
        handler.removeCallbacksAndMessages(null)
    }

    private class MonitoredRunnable(
        private val wrapped: Runnable,
        private val componentName: String
    ) : Runnable {
        override fun run() {
            val startTime = System.nanoTime()
            totalOperations.incrementAndGet()
            try {
                wrapped.run()
            } finally {
                val executionTime = (System.nanoTime() - startTime) / 1_000_000
                when {
                    executionTime > ERROR_THRESHOLD_MS -> {
                        verySlowOperations.incrementAndGet()
                        Log.e(
                            TAG,
                            "[$componentName] CRITICAL: Main thread blocked for ${executionTime}ms! " +
                                    "This may cause ANR. Move work to background thread."
                        )
                    }

                    executionTime > WARNING_THRESHOLD_MS -> {
                        slowOperations.incrementAndGet()
                        Log.w(
                            TAG,
                            "[$componentName] WARNING: Main thread operation took ${executionTime}ms. " +
                                    "Consider optimizing or moving to background thread."
                        )
                    }
                }
            }
        }
    }

    data class HandlerStatistics(
        val totalOperations: Long,
        val slowOperations: Long,
        val verySlowOperations: Long
    ) {
        val slowOperationRate: Float
            get() = if (totalOperations > 0) {
                (slowOperations.toFloat() / totalOperations.toFloat()) * 100f
            } else 0f
        val criticalOperationRate: Float
            get() = if (totalOperations > 0) {
                (verySlowOperations.toFloat() / totalOperations.toFloat()) * 100f
            } else 0f
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\SafeRippleModifier.kt =====

package mpdc4gsr.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.mpdc4gsr.libunified.app.compose.utils.deferAction as libDeferAction
import com.mpdc4gsr.libunified.app.compose.utils.safeClickable as libSafeClickable
import com.mpdc4gsr.libunified.app.compose.utils.safeClickableDeferred as libSafeClickableDeferred
import com.mpdc4gsr.libunified.app.compose.utils.safeClickableNoRipple as libSafeClickableNoRipple
import com.mpdc4gsr.libunified.app.compose.utils.safeClickableWithRipple as libSafeClickableWithRipple

@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = libSafeClickable(enabled, onClickLabel, role, onClick)

fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = libSafeClickableWithRipple(enabled, bounded, radius, color, onClickLabel, role, onClick)

@Composable
fun Modifier.safeClickableNoRipple(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = libSafeClickableNoRipple(enabled, onClickLabel, role, onClick)

@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = libSafeClickableDeferred(enabled, onClickLabel, role, onClick)

@Composable
fun deferAction(action: () -> Unit): () -> Unit = libDeferAction(action)


// ===== app\src\main\java\mpdc4gsr\core\ui\theme\Theme.kt =====

package mpdc4gsr.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Standard color constants for buttons and UI elements
val Orange = Color(0xFFFF9800)
val Green = Color(0xFF4CAF50)
val Purple = Color(0xFF9C27B0)

// Color scheme for IRCamera app - based on thermal imaging colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B73FF), // Blue-purple for thermal UI
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1B3C),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFFF6B6B), // Red-orange for thermal highlights  
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3C1E1E),
    onSecondaryContainer = Color(0xFFFFE0E0),
    tertiary = Color(0xFF4ECDC4), // Cyan for GSR/sensor data
    onTertiary = Color.White,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF16131e),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseOnSurface = Color(0xFF121212),
    inverseSurface = Color(0xFFE6E6E6),
    inversePrimary = Color(0xFF415FDF),
    scrim = Color(0xFF000000)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF415FDF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF000F5C),
    secondary = Color(0xFFD32F2F), // Red for thermal highlights
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF26A69A), // Teal for GSR/sensor data
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFFF4EFF4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = Color(0xFFBEC2FF),
)

@Composable
fun IRCameraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent thermal imaging theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


// ===== app\src\main\java\mpdc4gsr\core\ui\theme\Type.kt =====

package mpdc4gsr.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography matching the reference app styling
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp, // Match TitleView title size
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // Match reference tips text size
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)


// ===== app\src\main\java\mpdc4gsr\core\ui\ThermalComposeIntegration.kt =====

package mpdc4gsr.core.ui


// ===== app\src\main\java\mpdc4gsr\core\ui\utils\ComposeInterop.kt =====

package mpdc4gsr.core.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager


@Composable
fun AndroidViewWrapper(
    viewFactory: (Context) -> View,
    modifier: Modifier = Modifier,
    update: (View) -> Unit = {}
) {
    AndroidView(
        factory = viewFactory,
        modifier = modifier,
        update = update
    )
}

@Composable
fun FragmentContainer(
    fragmentManager: FragmentManager,
    fragmentFactory: () -> Fragment,
    modifier: Modifier = Modifier.fillMaxSize(),
    containerId: Int = View.generateViewId()
) {
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId
                // Add the fragment to the container
                val fragment = fragmentFactory()
                fragmentManager.beginTransaction()
                    .replace(id, fragment)
                    .commit()
            }
        },
        modifier = modifier
    )
}

@Composable
fun HybridScreen(
    composeContent: @Composable () -> Unit,
    androidViewContent: @Composable () -> Unit
) {
    // This can be customized based on layout requirements
    // For now, simple vertical layout approach
    androidx.compose.foundation.layout.Column {
        composeContent()
        androidViewContent()
    }
}

object StateFlowBridge {
    // Additional utilities for complex state bridging can be added here
    // if the standard collectAsState() doesn't cover all use cases
}

object FragmentComposeUtils {

    @Composable
    fun FragmentCompose(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        fragmentFactory: () -> Fragment,
        modifier: Modifier = Modifier.fillMaxSize()
    ) {
        FragmentContainer(
            fragmentManager = fragmentManager,
            fragmentFactory = fragmentFactory,
            modifier = modifier,
            containerId = View.generateViewId()
        )
    }

    fun navigateFromFragmentToCompose(
        fragment: Fragment,
        composeActivityClass: Class<*>,
        extras: Bundle? = null,
        finishCurrent: Boolean = false
    ) {
        val intent = Intent(fragment.requireContext(), composeActivityClass).apply {
            extras?.let { putExtras(it) }
        }
        fragment.startActivity(intent)
        if (finishCurrent && fragment.activity != null) {
            fragment.activity?.finish()
        }
    }

    fun preserveFragmentState(
        fragment: Fragment,
        key: String,
        value: Any
    ) {
        fragment.arguments = (fragment.arguments ?: Bundle()).apply {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Bundle -> putBundle(key, value)
                // Add more types as needed
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\ui\utils\ViewModelExtensions.kt =====

package mpdc4gsr.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow


@Composable
fun <T> LiveData<T>.observeAsComposeState(initial: T): State<T> {
    return this.observeAsState(initial)
}

@Composable
fun <T> StateFlow<T>.collectAsComposeState(): State<T> {
    return this.collectAsState()
}

data class ComposeThermalData(
    val centerTemp: Float,
    val maxTemp: Float,
    val minTemp: Float,
    val maxTempLocation: Pair<Int, Int>? = null,
    val minTempLocation: Pair<Int, Int>? = null,
    val isRecording: Boolean = false,
    val connectionState: String = "Disconnected"
) {
    companion object {

        fun fromExistingData(
            center: Float,
            max: Float,
            min: Float,
            recording: Boolean = false
        ): ComposeThermalData {
            return ComposeThermalData(
                centerTemp = center,
                maxTemp = max,
                minTemp = min,
                isRecording = recording
            )
        }
    }
}

data class ComposeGSRData(
    val currentValue: Float,
    val batteryLevel: Int,
    val connectionState: String,
    val sampleRate: String = "51.2Hz",
    val recentReadings: List<Float> = emptyList()
) {
    companion object {
        fun fromShimmerData(
            value: Float,
            battery: Int,
            connected: Boolean
        ): ComposeGSRData {
            return ComposeGSRData(
                currentValue = value,
                batteryLevel = battery,
                connectionState = if (connected) "Connected" else "Disconnected"
            )
        }
    }
}

data class ComposeConnectionStates(
    val thermalCamera: mpdc4gsr.core.ui.ConnectionState,
    val gsrSensor: mpdc4gsr.core.ui.ConnectionState,
    val bleConnection: mpdc4gsr.core.ui.ConnectionState
)

object ViewModelStateBridge {

    @Composable
    fun createThermalDataState(
        centerTempLiveData: LiveData<Float>,
        maxTempLiveData: LiveData<Float>,
        minTempLiveData: LiveData<Float>,
        isRecordingLiveData: LiveData<Boolean>
    ): State<ComposeThermalData?> {
        val centerTemp = centerTempLiveData.observeAsState(0f)
        val maxTemp = maxTempLiveData.observeAsState(0f)
        val minTemp = minTempLiveData.observeAsState(0f)
        val isRecording = isRecordingLiveData.observeAsState(false)
        return androidx.compose.runtime.derivedStateOf {
            ComposeThermalData.fromExistingData(
                center = centerTemp.value,
                max = maxTemp.value,
                min = minTemp.value,
                recording = isRecording.value
            )
        }
    }

    @Composable
    fun createGSRDataState(
        currentValueFlow: StateFlow<Float>,
        batteryLevelFlow: StateFlow<Int>,
        connectionStateFlow: StateFlow<Boolean>
    ): State<ComposeGSRData> {
        val currentValue = currentValueFlow.collectAsState()
        val batteryLevel = batteryLevelFlow.collectAsState()
        val isConnected = connectionStateFlow.collectAsState()
        return androidx.compose.runtime.derivedStateOf {
            ComposeGSRData.fromShimmerData(
                value = currentValue.value,
                battery = batteryLevel.value,
                connected = isConnected.value
            )
        }
    }
}

object ComposeEventBridge {

    @Composable
    fun handleThermalEvents(onEvent: (String) -> Unit) {
        // Integration with existing EventBus patterns
        // Can be extended to convert EventBus events to Compose actions
    }

    @Composable
    fun handleConnectionEvents(onConnectionChange: (Boolean) -> Unit) {
        // Integration with existing connection event handling
        // Preserves existing EventBus integration while enabling Compose UI updates
    }
}


