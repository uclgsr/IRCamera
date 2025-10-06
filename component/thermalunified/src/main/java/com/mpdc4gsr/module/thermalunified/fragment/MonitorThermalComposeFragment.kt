package com.mpdc4gsr.module.thermalunified.fragment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.stubs.FenceLineView
import com.mpdc4gsr.module.thermalunified.stubs.FencePointView
import com.mpdc4gsr.module.thermalunified.stubs.FenceView
import com.mpdc4gsr.module.thermalunified.stubs.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.viewmodel.MonitorThermalViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.MonitorThermalViewModel.*
import java.text.SimpleDateFormat
import java.util.*
class MonitorThermalComposeFragment : BaseComposeFragment<MonitorThermalViewModel>() {
    override fun createViewModel(): MonitorThermalViewModel {
        return viewModels<MonitorThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MonitorThermalViewModel) {
        // Observe ViewModel state
        val monitoringState by viewModel.monitoringState.collectAsStateWithLifecycle()
        val thermalData by viewModel.thermalData.collectAsStateWithLifecycle()
        val recordingStatus by viewModel.recordingStatus.collectAsStateWithLifecycle()
        val monitoringAlerts by viewModel.monitoringAlerts.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Monitoring status bar
                MonitoringStatusBar(
                    monitoringState = monitoringState,
                    recordingStatus = recordingStatus,
                    onToggleMonitoring = { viewModel.toggleMonitoring() },
                    onToggleRecording = { viewModel.toggleRecording() }
                )
                // Main monitoring interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal monitoring view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalMonitorView(
                            thermalData = thermalData,
                            onFenceUpdate = { fence ->
                                viewModel.updateMonitoringFence(fence)
                            }
                        )
                        // Monitoring overlays
                        MonitoringOverlays(
                            thermalData = thermalData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        // Alert notifications
                        AlertNotifications(
                            alerts = monitoringAlerts,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                    // Monitoring controls and data panel
                    MonitoringControlsPanel(
                        monitoringState = monitoringState,
                        thermalData = thermalData,
                        onThresholdChange = { threshold ->
                            viewModel.updateTemperatureThreshold(threshold)
                        },
                        onAlertSettingsChange = { settings ->
                            viewModel.updateAlertSettings(settings)
                        },
                        onExportData = { viewModel.exportMonitoringData() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
    @Composable
    private fun MonitoringStatusBar(
        monitoringState: MonitoringState,
        recordingStatus: RecordingStatus,
        onToggleMonitoring: () -> Unit,
        onToggleRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (monitoringState) {
                    MonitoringState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    MonitoringState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Thermal Monitoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getMonitoringStatusText(monitoringState),
                            color = getMonitoringStatusColor(monitoringState)
                        )
                        if (recordingStatus == RecordingStatus.RECORDING) {
                            StatusChip(
                                text = "Recording",
                                color = Color.Red
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording toggle
                    IconButton(
                        onClick = onToggleRecording,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (recordingStatus == RecordingStatus.RECORDING)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (recordingStatus == RecordingStatus.RECORDING)
                                Icons.Default.Stop
                            else
                                Icons.Default.FiberManualRecord,
                            contentDescription = "Toggle Recording",
                            tint = Color.White
                        )
                    }
                    // Monitoring toggle
                    Switch(
                        checked = monitoringState == MonitoringState.ACTIVE,
                        onCheckedChange = { onToggleMonitoring() }
                    )
                }
            }
        }
    }
    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
    @Composable
    private fun ThermalMonitorView(
        thermalData: ThermalData?,
        onFenceUpdate: (FenceData) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main thermal surface view
                AndroidView(
                    factory = { context ->
                        IrSurfaceView(context).apply {
                            // Configure surface view for monitoring
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Fence overlays for monitoring regions
                AndroidView(
                    factory = { context ->
                        FenceView(context).apply {
                            listener = object : FenceView.CallBack {
                                override fun callback(
                                    startPoint: IntArray,
                                    endPoint: IntArray,
                                    srcRect: IntArray
                                ) {
                                    // Convert fence callback data to FenceData and notify viewmodel
                                    val fenceData =
                                        "start:${startPoint.contentToString()},end:${endPoint.contentToString()},rect:${srcRect.contentToString()}"
                                    onFenceUpdate(FenceData(fenceData))
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { context ->
                        FencePointView(context)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { context ->
                        FenceLineView(context)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    @Composable
    private fun MonitoringOverlays(
        thermalData: ThermalData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            thermalData?.let { data ->
                TemperatureMonitorCard(
                    label = "Current",
                    temperature = "${data.currentTemp}°C",
                    isAlarm = data.isAlarmTriggered,
                    isMain = true
                )
                TemperatureMonitorCard(
                    label = "Max",
                    temperature = "${data.maxTemp}°C",
                    color = Color.Red
                )
                TemperatureMonitorCard(
                    label = "Min",
                    temperature = "${data.minTemp}°C",
                    color = MaterialTheme.colorScheme.primary
                )
                TemperatureMonitorCard(
                    label = "Avg",
                    temperature = "${data.avgTemp}°C",
                    color = Color.Gray
                )
            }
        }
    }
    @Composable
    private fun TemperatureMonitorCard(
        label: String,
        temperature: String,
        isAlarm: Boolean = false,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isAlarm -> MaterialTheme.colorScheme.errorContainer
                    isMain -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                }
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAlarm) MaterialTheme.colorScheme.onErrorContainer else color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlarm) MaterialTheme.colorScheme.onErrorContainer else color
                )
            }
        }
    }
    @Composable
    private fun AlertNotifications(
        alerts: List<MonitoringAlert>,
        modifier: Modifier = Modifier
    ) {
        if (alerts.isNotEmpty()) {
            LazyColumn(
                modifier = modifier
                    .width(300.dp)
                    .heightIn(max = 200.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }
    @Composable
    private fun AlertCard(alert: MonitoringAlert) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (alert.severity) {
                    AlertSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                    AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                    AlertSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (alert.severity) {
                        AlertSeverity.HIGH -> Icons.Default.Error
                        AlertSeverity.MEDIUM -> Icons.Default.Warning
                        AlertSeverity.LOW -> Icons.Default.Info
                    },
                    contentDescription = "Alert",
                    modifier = Modifier.size(16.dp),
                    tint = when (alert.severity) {
                        AlertSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                        AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                        AlertSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    @Composable
    private fun MonitoringControlsPanel(
        monitoringState: MonitoringState,
        thermalData: ThermalData?,
        onThresholdChange: (TemperatureThreshold) -> Unit,
        onAlertSettingsChange: (AlertSettings) -> Unit,
        onExportData: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Monitoring Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    ThresholdControls(
                        onThresholdChange = onThresholdChange,
                        enabled = monitoringState == MonitoringState.ACTIVE
                    )
                }
                item {
                    HorizontalDivider()
                }
                item {
                    AlertSettingsSection(
                        onAlertSettingsChange = onAlertSettingsChange
                    )
                }
                item {
                    HorizontalDivider()
                }
                item {
                    MonitoringDataSection(
                        thermalData = thermalData
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Button(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Data")
                    }
                }
            }
        }
    }
    @Composable
    private fun ThresholdControls(
        onThresholdChange: (TemperatureThreshold) -> Unit,
        enabled: Boolean
    ) {
        var highThreshold by remember { mutableFloatStateOf(50f) }
        var lowThreshold by remember { mutableFloatStateOf(10f) }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Temperature Thresholds",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            // High threshold slider
            Text(
                text = "High Alert: ${String.format("%.1f", highThreshold)}°C",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = highThreshold,
                onValueChange = {
                    highThreshold = it
                    onThresholdChange(TemperatureThreshold(it, lowThreshold))
                },
                valueRange = 20f..100f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            // Low threshold slider
            Text(
                text = "Low Alert: ${String.format("%.1f", lowThreshold)}°C",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = lowThreshold,
                onValueChange = {
                    lowThreshold = it
                    onThresholdChange(TemperatureThreshold(highThreshold, it))
                },
                valueRange = -20f..40f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    @Composable
    private fun AlertSettingsSection(
        onAlertSettingsChange: (AlertSettings) -> Unit
    ) {
        var enableSound by remember { mutableStateOf(true) }
        var enableVibration by remember { mutableStateOf(true) }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Alert Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Alerts", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = enableSound,
                    onCheckedChange = {
                        enableSound = it
                        onAlertSettingsChange(AlertSettings(it, enableVibration))
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = enableVibration,
                    onCheckedChange = {
                        enableVibration = it
                        onAlertSettingsChange(AlertSettings(enableSound, it))
                    }
                )
            }
        }
    }
    @Composable
    private fun MonitoringDataSection(
        thermalData: ThermalData?
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Session Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            thermalData?.let { data ->
                DataRow("Duration", data.sessionDuration)
                DataRow("Samples", "${data.sampleCount}")
                DataRow("Alerts", "${data.alertCount}")
                DataRow("Data Size", data.dataSize)
            }
        }
    }
    @Composable
    private fun DataRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
    // Helper functions
    private fun getMonitoringStatusText(state: MonitoringState): String = when (state) {
        MonitoringState.ACTIVE -> "Active"
        MonitoringState.PAUSED -> "Paused"
        MonitoringState.STOPPED -> "Stopped"
    }
    private fun getMonitoringStatusColor(state: MonitoringState): Color = when (state) {
        MonitoringState.ACTIVE -> Color.Green
        MonitoringState.PAUSED -> Color(0xFFFFA500)
        MonitoringState.STOPPED -> Color.Gray
    }
}