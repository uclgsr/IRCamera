// Merged .kt under 'core\ui\components' subtree
// Files: 16; Generated 2025-10-07 19:59:55


// ===== core\ui\components\DeleteConfirmationDialog.kt =====

package mpdc4gsr.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.ConnectionState
import mpdc4gsr.core.ui.model.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.network.data.SensorStatusSummary
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.contains
import kotlin.collections.count
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.maxOrNull
import kotlin.collections.minOrNull
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.math.sin

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


// ===== core\ui\components\NavigationBreadcrumb.kt =====

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


// ===== core\ui\components\RecordingControlsCompose.kt =====

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


// ===== core\ui\components\RecordingStatusCompose.kt =====

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


// ===== core\ui\components\SensorDashboardCompose.kt =====

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


// ===== core\ui\components\sensors\ComprehensiveSensorStatusCompose.kt =====

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


// ===== core\ui\components\sensors\GSRSensorCard.kt =====

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


// ===== core\ui\components\sensors\GSRVisualizationCard.kt =====

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


// ===== core\ui\components\sensors\RGBCameraSensorCard.kt =====

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


// ===== core\ui\components\sensors\ThermalSensorCard.kt =====

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


// ===== core\ui\components\sensors\UnifiedSensorStatus.kt =====

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


// ===== core\ui\components\SensorSelectionCompose.kt =====

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


// ===== core\ui\components\SensorStatusCard.kt =====

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


// ===== core\ui\components\settings\SettingsComponents.kt =====

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


// ===== core\ui\components\ThermalVisualizationCard.kt =====

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


// ===== core\ui\components\TitleBar.kt =====

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


