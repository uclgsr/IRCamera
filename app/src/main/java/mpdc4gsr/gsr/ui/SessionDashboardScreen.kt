package mpdc4gsr.gsr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.FaultSeverity
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.model.SessionFault
import mpdc4gsr.gsr.model.SessionSnapshot
import mpdc4gsr.gsr.model.TelemetryState

@Composable
fun SessionDashboardScreen(
    sessionState: StateFlow<SessionSnapshot?>,
    telemetryState: StateFlow<Map<String, TelemetryState>>,
    onStartSimulation: () -> Unit,
    onStop: () -> Unit,
) {
    val snapshot by sessionState.collectAsState()
    val telemetry by telemetryState.collectAsState()

    LibSharedTheme {
        Scaffold(
            topBar = { DashboardTopBar(snapshot) },
            floatingActionButton = {
                DashboardFab(
                    snapshot = snapshot,
                    onStartSimulation = onStartSimulation,
                    onStop = onStop,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val state = snapshot
                if (state == null) {
                    item { EmptyStateCard(onStartSimulation) }
                } else {
                    item { SessionHeadlineCard(state) }
                    item { TimelineCard(state) }
                    item { RecorderStateGrid(state.recorderStates) }
                    item { DeviceTelemetrySection(state.connectedDevices, telemetry) }
                    if (state.faults.isNotEmpty()) {
                        item { FaultsSection(state.faults) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(snapshot: SessionSnapshot?) {
    TopAppBar(
        title = {
            Text(snapshot?.label ?: "GSR Capture Dashboard")
        },
        actions = {
            snapshot?.let { state ->
                StatusChip(
                    label = if (state.isRecording) "Recording" else "Idle",
                    tone =
                        if (state.isRecording) StatusTone.Positive else StatusTone.Neutral,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
    )
}

@Composable
private fun DashboardFab(
    snapshot: SessionSnapshot?,
    onStartSimulation: () -> Unit,
    onStop: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = if (snapshot == null) onStartSimulation else onStop,
        icon = {
            Icon(
                imageVector = if (snapshot == null) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = null,
            )
        },
        text = { Text(if (snapshot == null) "Start Simulation Session" else "Stop Session") },
    )
}

@Composable
private fun EmptyStateCard(onStartSimulation: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("No session active", style = MaterialTheme.typography.titleMedium)
            Text(
                "Launch a simulation run to verify sensor readiness or wait for the PC orchestrator to begin a session.",
                style = MaterialTheme.typography.bodyMedium,
            )
            androidx.compose.material3.FilledTonalButton(onClick = onStartSimulation) {
                Text("Start Simulation")
            }
        }
    }
}

@Composable
private fun SessionHeadlineCard(snapshot: SessionSnapshot) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = snapshot.label,
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoColumn(
                    title = "Session ID",
                    value = snapshot.sessionId,
                )
                InfoColumn(
                    title = "Elapsed",
                    value = snapshot.elapsedMillis.formatElapsed(),
                )
                InfoColumn(
                    title = "Started",
                    value = snapshot.startedAt.formatAsTime(),
                )
            }
        }
    }
}

@Composable
private fun TimelineCard(snapshot: SessionSnapshot) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Timeline", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InfoColumn("Offset", "${snapshot.globalTimeline.offsetMillis.format(2)} ms")
                InfoColumn("Drift", "${snapshot.globalTimeline.driftPpm.format(2)} ppm")
                InfoColumn("Pending Commands", snapshot.commandsPending.toString())
                snapshot.plannedDurationSeconds?.let {
                    InfoColumn("Planned", "${it / 60} min")
                }
            }
        }
    }
}

@Composable
private fun RecorderStateGrid(states: Map<RecorderKind, RecorderState>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Modalities", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                states.entries.sortedBy { it.key.ordinal }.forEach { (kind, state) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(kind.displayName, style = MaterialTheme.typography.bodyMedium)
                        StatusChip(
                            label = state.displayName,
                            tone = state.toTone(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceTelemetrySection(
    devices: List<DeviceDescriptor>,
    telemetry: Map<String, TelemetryState>,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Connected Devices", style = MaterialTheme.typography.titleMedium)
            if (devices.isEmpty()) {
                Text(
                    "No devices reported by the PC coordinator yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    devices.forEach { device ->
                        DeviceRow(
                            descriptor = device,
                            telemetry = telemetry[device.id],
                        )
                        if (device != devices.last()) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    descriptor: DeviceDescriptor,
    telemetry: TelemetryState?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(descriptor.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            StatusChip(
                label = descriptor.connectionState.displayName,
                tone = descriptor.connectionState.toTone(),
            )
        }
        Text(
            descriptor.type.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        telemetry?.let { state ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TelemetryItem("Battery", state.batteryPercent?.let { "$it%" } ?: "—")
                TelemetryItem("Frame rate", state.frameRate?.format(1)?.let { "$it fps" } ?: "—")
                TelemetryItem("Drops", state.droppedFrames.toString())
                TelemetryItem("ISP", state.ispActive?.let { if (it) "Active" else "Standby" } ?: "Unknown")
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FaultsSection(faults: List<SessionFault>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Recent Faults", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                faults.take(6).forEach { fault ->
                    Column {
                        Text(
                            fault.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatusChip(
                                label = fault.severity.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                tone = fault.severity.toTone(),
                            )
                            Text(
                                fault.occurredAt.formatAsTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (fault != faults.take(6).last()) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = tone.containerColor(MaterialTheme.colorScheme),
        contentColor = tone.contentColor(MaterialTheme.colorScheme),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class StatusTone {
    Positive,
    Neutral,
    Caution,
    Danger,
}

private fun RecorderState.toTone(): StatusTone =
    when (this) {
        RecorderState.RECORDING -> StatusTone.Positive
        RecorderState.PREPARING, RecorderState.STOPPING -> StatusTone.Caution
        RecorderState.FAILED -> StatusTone.Danger
        RecorderState.IDLE -> StatusTone.Neutral
    }

private fun ConnectionState.toTone(): StatusTone =
    when (this) {
        ConnectionState.RECORDING -> StatusTone.Positive
        ConnectionState.CONNECTING, ConnectionState.READY, ConnectionState.CONNECTED -> StatusTone.Caution
        ConnectionState.DISCONNECTED -> StatusTone.Neutral
        ConnectionState.ERROR -> StatusTone.Danger
        ConnectionState.DISCOVERED -> StatusTone.Neutral
    }

private fun FaultSeverity.toTone(): StatusTone =
    when (this) {
        FaultSeverity.INFO -> StatusTone.Neutral
        FaultSeverity.WARNING -> StatusTone.Caution
        FaultSeverity.ERROR -> StatusTone.Danger
    }

private fun StatusTone.containerColor(colors: androidx.compose.material3.ColorScheme) =
    when (this) {
        StatusTone.Positive -> colors.primaryContainer
        StatusTone.Neutral -> colors.surfaceVariant
        StatusTone.Caution -> colors.tertiaryContainer
        StatusTone.Danger -> colors.errorContainer
    }

private fun StatusTone.contentColor(colors: androidx.compose.material3.ColorScheme) =
    when (this) {
        StatusTone.Positive -> colors.onPrimaryContainer
        StatusTone.Neutral -> colors.onSurfaceVariant
        StatusTone.Caution -> colors.onTertiaryContainer
        StatusTone.Danger -> colors.onErrorContainer
    }

private fun Long.formatElapsed(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun Double.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)

private fun Instant.formatAsTime(): String =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(this)

private val RecorderKind.displayName: String
    get() =
        when (this) {
            RecorderKind.GSR -> "GSR"
            RecorderKind.RGB_VIDEO -> "RGB Video"
            RecorderKind.THERMAL_VIDEO -> "Thermal"
            RecorderKind.AUDIO -> "Audio"
        }

private val RecorderState.displayName: String
    get() =
        when (this) {
            RecorderState.IDLE -> "Idle"
            RecorderState.PREPARING -> "Preparing"
            RecorderState.RECORDING -> "Recording"
            RecorderState.STOPPING -> "Stopping"
            RecorderState.FAILED -> "Failed"
        }

private val ConnectionState.displayName: String
    get() =
        when (this) {
            ConnectionState.DISCOVERED -> "Discovered"
            ConnectionState.CONNECTING -> "Connecting"
            ConnectionState.CONNECTED -> "Connected"
            ConnectionState.READY -> "Ready"
            ConnectionState.RECORDING -> "Recording"
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.ERROR -> "Error"
        }

