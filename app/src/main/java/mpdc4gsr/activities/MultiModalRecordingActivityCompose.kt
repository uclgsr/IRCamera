package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.*

/**
 * MultiModalRecordingActivityCompose - Enhanced Compose Multi-Modal Recording
 *
 * Advanced interface for coordinated multi-sensor recording with:
 * - Real-time sensor status monitoring and synchronization
 * - Session management with live statistics and duration tracking
 * - Multi-modal data coordination (GSR, thermal, RGB camera, audio)
 * - Research protocol support with template-driven workflows
 * - Comprehensive recording controls with error recovery
 */
class MultiModalRecordingActivityCompose : BaseComposeActivity<MultiModalRecordingViewModel>() {

    override fun createViewModel(): MultiModalRecordingViewModel = MultiModalRecordingViewModel()

    @Composable
    override fun Content(viewModel: MultiModalRecordingViewModel) {
        IRCameraTheme {
            MultiModalRecordingScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiModalRecordingScreen(viewModel: MultiModalRecordingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSessionDialog by remember { mutableStateOf(false) }
    var showProtocolSelector by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initializeSensors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Session Info
        SessionHeader(
            sessionInfo = uiState.currentSession,
            isRecording = uiState.isRecording,
            onNewSession = { showSessionDialog = true },
            onSelectProtocol = { showProtocolSelector = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recording Controls Card
            item {
                RecordingControlsCard(
                    isRecording = uiState.isRecording,
                    canRecord = uiState.canStartRecording,
                    sessionDuration = uiState.sessionDuration,
                    dataSize = uiState.totalDataSize,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecording() },
                    onPauseRecording = { viewModel.pauseRecording() },
                    onResumeRecording = { viewModel.resumeRecording() }
                )
            }

            // Sensor Status Cards
            item {
                Text(
                    text = "Sensor Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(uiState.sensorStates) { sensorState ->
                SensorStatusCard(
                    sensorState = sensorState,
                    onToggleEnabled = { viewModel.toggleSensor(sensorState.type) },
                    onConfigure = { viewModel.configureSensor(sensorState.type) }
                )
            }

            // RGB Camera Preview (if enabled)
            if (uiState.sensorStates.any { it.type == SensorType.RGB_CAMERA && it.isEnabled }) {
                item {
                    CameraPreviewCard()
                }
            }

            // Session Statistics Card
            if (uiState.isRecording || uiState.sessionStatistics.totalSamples > 0) {
                item {
                    SessionStatisticsCard(
                        statistics = uiState.sessionStatistics,
                        isRecording = uiState.isRecording
                    )
                }
            }

            // Data Quality Monitoring
            if (uiState.qualityMetrics.isNotEmpty()) {
                item {
                    DataQualityCard(
                        qualityMetrics = uiState.qualityMetrics
                    )
                }
            }
        }
    }

    // New Session Dialog
    if (showSessionDialog) {
        NewSessionDialog(
            onDismiss = { showSessionDialog = false },
            onCreateSession = { sessionName, participantId ->
                viewModel.createNewSession(sessionName, participantId)
                showSessionDialog = false
            }
        )
    }

    // Protocol Selector Dialog
    if (showProtocolSelector) {
        ProtocolSelectorDialog(
            protocols = uiState.availableProtocols,
            onDismiss = { showProtocolSelector = false },
            onSelectProtocol = { protocol ->
                viewModel.selectProtocol(protocol)
                showProtocolSelector = false
            }
        )
    }
}

@Composable
fun SessionHeader(
    sessionInfo: SessionInfo?,
    isRecording: Boolean,
    onNewSession: () -> Unit,
    onSelectProtocol: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFFF44336).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Multi-Modal Recording",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onSelectProtocol) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Select Protocol"
                        )
                    }
                    IconButton(onClick = onNewSession) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Session"
                        )
                    }
                }
            }

            if (sessionInfo != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Session: ${sessionInfo.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (sessionInfo.participantId.isNotEmpty()) {
                    Text(
                        text = "Participant: ${sessionInfo.participantId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (sessionInfo.protocol.isNotEmpty()) {
                    Text(
                        text = "Protocol: ${sessionInfo.protocol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingControlsCard(
    isRecording: Boolean,
    canRecord: Boolean,
    sessionDuration: Long,
    dataSize: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFFF44336).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRecording) "Recording Active" else "Recording Controls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRecording) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
                    )

                    if (isRecording) {
                        Text(
                            text = formatDuration(sessionDuration),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "Data: ${formatDataSize(dataSize)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (isRecording) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.size(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isRecording) {
                    Button(
                        onClick = onStartRecording,
                        enabled = canRecord,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
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
                } else {
                    OutlinedButton(
                        onClick = onPauseRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }

                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Composable
fun SensorStatusCard(
    sensorState: SensorState,
    onToggleEnabled: () -> Unit,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (sensorState.type) {
                    SensorType.GSR -> Icons.Default.Sensors
                    SensorType.THERMAL_CAMERA -> Icons.Default.Thermostat
                    SensorType.RGB_CAMERA -> Icons.Default.Camera
                    SensorType.AUDIO -> Icons.Default.Mic
                },
                contentDescription = null,
                tint = if (sensorState.isEnabled && sensorState.isConnected) Color(0xFF4CAF50)
                else if (sensorState.isEnabled) Color(0xFFFF9800)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorState.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        !sensorState.isEnabled -> "Disabled"
                        !sensorState.isConnected -> "Not Connected"
                        else -> "Connected - ${sensorState.sampleRate} Hz"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !sensorState.isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        !sensorState.isConnected -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                )
                if (sensorState.isRecording) {
                    Text(
                        text = "Recording: ${sensorState.samplesRecorded} samples",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF44336)
                    )
                }
            }

            Column {
                Switch(
                    checked = sensorState.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )

                if (sensorState.isEnabled) {
                    TextButton(onClick = onConfigure) {
                        Text("Configure")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "RGB Camera Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Placeholder for camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera Preview",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SessionStatisticsCard(
    statistics: SessionStatistics,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Samples",
                    value = statistics.totalSamples.toString(),
                    isHighlighted = isRecording
                )
                StatisticItem(
                    label = "Data Rate",
                    value = "${statistics.dataRate}/s",
                    isHighlighted = isRecording
                )
                StatisticItem(
                    label = "Quality",
                    value = "${statistics.dataQuality}%",
                    isHighlighted = statistics.dataQuality > 90
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DataQualityCard(
    qualityMetrics: List<QualityMetric>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Quality Monitoring",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            qualityMetrics.forEach { metric ->
                QualityMetricItem(metric = metric)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun QualityMetricItem(metric: QualityMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metric.sensorName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        LinearProgressIndicator(
            progress = metric.quality / 100f,
            modifier = Modifier.weight(2f),
            color = when {
                metric.quality > 90 -> Color(0xFF4CAF50)
                metric.quality > 70 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        )

        Text(
            text = "${metric.quality}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreateSession: (String, String) -> Unit
) {
    var sessionName by remember { mutableStateOf("") }
    var participantId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Recording Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = participantId,
                    onValueChange = { participantId = it },
                    label = { Text("Participant ID") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateSession(sessionName, participantId) },
                enabled = sessionName.isNotBlank()
            ) {
                Text("Create")
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
fun ProtocolSelectorDialog(
    protocols: List<ResearchProtocol>,
    onDismiss: () -> Unit,
    onSelectProtocol: (ResearchProtocol) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Research Protocol") },
        text = {
            LazyColumn {
                items(protocols) { protocol ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onSelectProtocol(protocol) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = protocol.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = protocol.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Duration: ${protocol.duration}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
}

private fun formatDataSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

// Data classes
data class SessionInfo(
    val name: String,
    val participantId: String,
    val protocol: String = "",
    val startTime: Long = System.currentTimeMillis()
)

enum class SensorType {
    GSR, THERMAL_CAMERA, RGB_CAMERA, AUDIO
}

data class SensorState(
    val type: SensorType,
    val name: String,
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val sampleRate: Int = 0,
    val samplesRecorded: Long = 0
)

data class SessionStatistics(
    val totalSamples: Long = 0,
    val dataRate: Int = 0,
    val dataQuality: Int = 0
)

data class QualityMetric(
    val sensorName: String,
    val quality: Int
)

data class ResearchProtocol(
    val name: String,
    val description: String,
    val duration: String
)

data class MultiModalRecordingUiState(
    val currentSession: SessionInfo? = null,
    val isRecording: Boolean = false,
    val canStartRecording: Boolean = false,
    val sessionDuration: Long = 0,
    val totalDataSize: Long = 0,
    val sensorStates: List<SensorState> = emptyList(),
    val sessionStatistics: SessionStatistics = SessionStatistics(),
    val qualityMetrics: List<QualityMetric> = emptyList(),
    val availableProtocols: List<ResearchProtocol> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel placeholder
class MultiModalRecordingViewModel : BaseViewModel() {
    private val _uiState = androidx.compose.runtime.mutableStateOf(MultiModalRecordingUiState())
    val uiState: androidx.compose.runtime.State<MultiModalRecordingUiState> = _uiState

    private var recordingJob: Job? = null

    fun initializeSensors() {
        val sensors = listOf(
            SensorState(
                SensorType.GSR,
                "Shimmer GSR",
                isEnabled = true,
                isConnected = true,
                sampleRate = 256
            ),
            SensorState(
                SensorType.THERMAL_CAMERA,
                "Thermal Camera",
                isEnabled = true,
                isConnected = true,
                sampleRate = 30
            ),
            SensorState(
                SensorType.RGB_CAMERA,
                "RGB Camera",
                isEnabled = false,
                isConnected = true,
                sampleRate = 30
            ),
            SensorState(
                SensorType.AUDIO,
                "Audio Recorder",
                isEnabled = false,
                isConnected = true,
                sampleRate = 44100
            )
        )

        val protocols = listOf(
            ResearchProtocol("Stress Response", "Baseline + stress task + recovery", "30 min"),
            ResearchProtocol("Cognitive Load", "Math tasks with increasing difficulty", "20 min"),
            ResearchProtocol("Emotion Recognition", "Video stimuli with rating tasks", "45 min")
        )

        _uiState.value = _uiState.value.copy(
            sensorStates = sensors,
            availableProtocols = protocols,
            canStartRecording = sensors.any { it.isEnabled && it.isConnected }
        )
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)

        // Cancel any existing recording job
        recordingJob?.cancel()

        // Start recording statistics updates on main dispatcher
        recordingJob = viewModelScope.launch(Dispatchers.Main) {
            while (_uiState.value.isRecording) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    sessionDuration = _uiState.value.sessionDuration + 1000,
                    totalDataSize = _uiState.value.totalDataSize + 1024,
                    sessionStatistics = _uiState.value.sessionStatistics.copy(
                        totalSamples = _uiState.value.sessionStatistics.totalSamples + 256,
                        dataRate = 256,
                        dataQuality = (85..98).random()
                    ),
                    qualityMetrics = listOf(
                        QualityMetric("GSR", (85..98).random()),
                        QualityMetric("Thermal", (90..100).random())
                    )
                )
            }
        }
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }

    fun pauseRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }

    fun resumeRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun toggleSensor(sensorType: SensorType) {
        val updatedSensors = _uiState.value.sensorStates.map { sensor ->
            if (sensor.type == sensorType) {
                sensor.copy(isEnabled = !sensor.isEnabled)
            } else sensor
        }
        _uiState.value = _uiState.value.copy(sensorStates = updatedSensors)
    }

    fun configureSensor(sensorType: SensorType) {
        // Implementation for sensor configuration
    }

    fun createNewSession(sessionName: String, participantId: String) {
        _uiState.value = _uiState.value.copy(
            currentSession = SessionInfo(sessionName, participantId)
        )
    }

    fun selectProtocol(protocol: ResearchProtocol) {
        _uiState.value = _uiState.value.copy(
            currentSession = _uiState.value.currentSession?.copy(protocol = protocol.name)
        )
    }
}
}