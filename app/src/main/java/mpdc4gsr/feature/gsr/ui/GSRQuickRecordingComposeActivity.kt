package mpdc4gsr.feature.gsr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.random.Random

@AndroidEntryPoint
class GSRQuickRecordingComposeActivity : BaseComposeActivity<GSRQuickRecordingViewModel>() {
    override fun createViewModel(): GSRQuickRecordingViewModel = GSRQuickRecordingViewModel()

    @Composable
    override fun Content(viewModel: GSRQuickRecordingViewModel) {
        IRCameraTheme {
            GSRQuickRecordingScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(viewModel: GSRQuickRecordingViewModel) {
    val uiState by viewModel.recordingState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initializeQuickRecording()
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Quick GSR Recording",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Recording Status Card
            item {
                QuickRecordingStatusCard(
                    isRecording = uiState.isRecording,
                    recordingDuration = uiState.recordingDuration,
                    samplesCollected = uiState.samplesCollected,
                    onStartRecording = { viewModel.startQuickRecording() },
                    onStopRecording = { viewModel.stopQuickRecording() },
                )
            }
            // GSR Device Status
            item {
                GSRDeviceStatusCard(
                    deviceStatus = uiState.deviceStatus,
                    signalQuality = uiState.signalQuality,
                    batteryLevel = uiState.batteryLevel,
                )
            }
            // Live GSR Data Visualization
            if (uiState.isRecording) {
                item {
                    LiveGSRDataCard(
                        currentValue = uiState.currentGSRValue,
                        averageValue = uiState.averageGSRValue,
                        recentValues = uiState.recentGSRValues,
                    )
                }
            }
            // Quick Settings
            item {
                QuickSettingsCard(
                    sampleRate = uiState.sampleRate,
                    autoSave = uiState.autoSave,
                    onSampleRateChange = { viewModel.setSampleRate(it) },
                    onAutoSaveToggle = { viewModel.toggleAutoSave() },
                )
            }
            // Recent Sessions
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Quick Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(uiState.recentSessions.take(3)) { session ->
                    QuickSessionCard(
                        session = session,
                        onView = { viewModel.viewSession(session) },
                        onExport = { viewModel.exportSession(session) },
                    )
                }
            }
        }
    }
}

@Composable
fun QuickRecordingStatusCard(
    isRecording: Boolean,
    recordingDuration: Long,
    samplesCollected: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isRecording) {
                        Color(0xFFF44336).copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isRecording) {
                // Recording indicator with animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF44336),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Recording Active",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickStatItem("Duration", formatDuration(recordingDuration))
                    QuickStatItem("Samples", samplesCollected.toString())
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStopRecording,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Ready to Record",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ready to Record",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tap to start quick GSR recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartRecording,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun GSRDeviceStatusCard(
    deviceStatus: String,
    signalQuality: Int,
    batteryLevel: Int?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Sensor Status: $deviceStatus",
                    tint =
                        when (deviceStatus) {
                            "Connected" -> Color(0xFF4CAF50)
                            "Connecting" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GSR Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = deviceStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            when (deviceStatus) {
                                "Connected" -> Color(0xFF4CAF50)
                                "Connecting" -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            },
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Quality: $signalQuality%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (batteryLevel != null) {
                        Text(
                            text = "Battery: $batteryLevel%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (deviceStatus == "Connected") {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { signalQuality / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        when {
                            signalQuality > 80 -> Color(0xFF4CAF50)
                            signalQuality > 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                )
            }
        }
    }
}

@Composable
fun LiveGSRDataCard(
    currentValue: Double,
    averageValue: Double,
    recentValues: List<Double>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Live GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickStatItem("Current", String.format("%.2f µS", currentValue))
                QuickStatItem("Average", String.format("%.2f µS", averageValue))
                QuickStatItem(
                    "Range",
                    String.format(
                        "%.1f",
                        recentValues.maxOrNull()?.minus(recentValues.minOrNull() ?: 0.0) ?: 0.0,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Simple data visualization placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Live GSR Signal Visualization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
fun QuickSettingsCard(
    sampleRate: Int,
    autoSave: Boolean,
    onSampleRateChange: (Int) -> Unit,
    onAutoSaveToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sample Rate: $sampleRate Hz")
                Row {
                    listOf(128, 256, 512).forEach { rate ->
                        FilterChip(
                            onClick = { onSampleRateChange(rate) },
                            label = { Text("$rate") },
                            selected = sampleRate == rate,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Auto-save Sessions")
                Switch(
                    checked = autoSave,
                    onCheckedChange = { onAutoSaveToggle() },
                )
            }
        }
    }
}

@Composable
fun QuickSessionCard(
    session: QuickSession,
    onView: () -> Unit,
    onExport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "GSR Data",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatDuration(session.duration)} • ${session.sampleCount} samples",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onView) {
                Icon(Icons.Default.Visibility, contentDescription = "View")
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Download, contentDescription = "Export")
            }
        }
    }
}

@Composable
fun QuickStatItem(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format("%d:%02d", minutes, seconds % 60)
}

// Data classes
data class QuickSession(
    val name: String,
    val duration: Long,
    val sampleCount: Int,
    val timestamp: Long,
)

data class GSRQuickRecordingUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val samplesCollected: Int = 0,
    val deviceStatus: String = "Disconnected",
    val signalQuality: Int = 95,
    val batteryLevel: Int? = 87,
    val currentGSRValue: Double = 0.0,
    val averageGSRValue: Double = 0.0,
    val recentGSRValues: List<Double> = emptyList(),
    val sampleRate: Int = 256,
    val autoSave: Boolean = true,
    val recentSessions: List<QuickSession> = emptyList(),
)

// ViewModel
class GSRQuickRecordingViewModel : AppBaseViewModel() {
    private val _recordingState = MutableStateFlow(GSRQuickRecordingUiState())
    val recordingState: StateFlow<GSRQuickRecordingUiState> = _recordingState.asStateFlow()
    private var recordingJob: Job? = null

    fun initializeQuickRecording() {
        val mockSessions =
            listOf(
                QuickSession("Quick Session 1", 180000, 46080, System.currentTimeMillis() - 3600000),
                QuickSession("Quick Session 2", 120000, 30720, System.currentTimeMillis() - 7200000),
                QuickSession("Quick Session 3", 240000, 61440, System.currentTimeMillis() - 10800000),
            )
        _recordingState.value = _recordingState.value.copy(recentSessions = mockSessions)
    }

    fun startQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = true)
        // Cancel any existing recording job
        recordingJob?.cancel()
        // Start recording simulation on main dispatcher
        recordingJob =
            viewModelScope.launch(Dispatchers.Main) {
                while (_recordingState.value.isRecording) {
                    delay(1000)
                    val currentState = _recordingState.value
                    _recordingState.value =
                        currentState.copy(
                            recordingDuration = currentState.recordingDuration + 1000,
                            samplesCollected = currentState.samplesCollected + currentState.sampleRate,
                            currentGSRValue = Random.nextDouble(5.0, 15.0),
                            averageGSRValue = Random.nextDouble(8.0, 12.0),
                            recentGSRValues = currentState.recentGSRValues.takeLast(10) + Random.nextDouble(5.0, 15.0),
                        )
                }
            }
    }

    fun stopQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = false)
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }

    fun setSampleRate(rate: Int) {
        _recordingState.value = _recordingState.value.copy(sampleRate = rate)
    }

    fun toggleAutoSave() {
        _recordingState.value = _recordingState.value.copy(autoSave = !_recordingState.value.autoSave)
    }

    fun viewSession(session: QuickSession) {
        // Implementation for viewing session
    }

    fun exportSession(session: QuickSession) {
        // Implementation for exporting session
    }
}
