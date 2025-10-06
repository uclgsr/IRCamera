package mpdc4gsr.feature.testing.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.network.data.RecordingState

enum class SensorConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class RecordingSessionInfo(
    val sessionName: String = "Recording Session",
    val duration: String = "00:00:00",
    val dataSize: String = "0 MB",
    val frameCount: Int = 0,
    val thermalFrames: Int = 0,
    val gsrSamples: Int = 0
)

private const val DEFAULT_PLAYBACK_FPS = 30
private const val THERMAL_FPS = 9
private const val GSR_SAMPLING_RATE_HZ = 128

data class SensorInfo(
    val name: String,
    val status: SensorConnectionStatus,
    val lastUpdate: String = "Never",
    val dataRate: String = "0 KB/s",
    val errorMessage: String? = null
)

class FaultTolerantRecordingViewModel : AppBaseViewModel() {
    private val _recordingState = mutableStateOf(RecordingState.IDLE)
    val recordingState: State<RecordingState> = _recordingState
    private val _sessionInfo = mutableStateOf(RecordingSessionInfo())
    val sessionInfo: State<RecordingSessionInfo> = _sessionInfo
    private val _sensorInfoList = mutableStateOf(
        listOf(
            SensorInfo("Thermal Camera", SensorConnectionStatus.DISCONNECTED),
            SensorInfo("GSR Sensor", SensorConnectionStatus.DISCONNECTED),
            SensorInfo("RGB Camera", SensorConnectionStatus.DISCONNECTED),
            SensorInfo("Audio Recorder", SensorConnectionStatus.DISCONNECTED)
        )
    )
    val sensorInfoList: State<List<SensorInfo>> = _sensorInfoList
    private val _systemStatus = mutableStateOf("System initialized. Ready to start recording.")
    val systemStatus: State<String> = _systemStatus
    private val _isInitializing = mutableStateOf(false)
    val isInitializing: State<Boolean> = _isInitializing
    fun initializeSystem() {
        viewModelScope.launch {
            _isInitializing.value = true
            _systemStatus.value = "Initializing enhanced recording system..."
            delay(1000)
            // Simulate sensor initialization
            val sensors = listOf("Thermal Camera", "GSR Sensor", "RGB Camera", "Audio Recorder")
            sensors.forEachIndexed { index, sensorName ->
                _systemStatus.value = "Connecting to $sensorName..."
                delay(800)
                _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
                    if (sensor.name == sensorName) {
                        sensor.copy(
                            status = SensorConnectionStatus.CONNECTING
                        )
                    } else sensor
                }
                delay(1200)
                // Simulate successful connection (90% success rate)
                val isConnected = kotlin.random.Random.nextFloat() > 0.1f
                _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
                    if (sensor.name == sensorName) {
                        sensor.copy(
                            status = if (isConnected) SensorConnectionStatus.CONNECTED else SensorConnectionStatus.ERROR,
                            lastUpdate = if (isConnected) "Just connected" else "Connection failed",
                            dataRate = if (isConnected) when (sensorName) {
                                "Thermal Camera" -> "120 KB/s"
                                "GSR Sensor" -> "2 KB/s"
                                "RGB Camera" -> "1.5 MB/s"
                                "Audio Recorder" -> "64 KB/s"
                                else -> "0 KB/s"
                            } else "0 KB/s",
                            errorMessage = if (!isConnected) "Failed to establish connection" else null
                        )
                    } else sensor
                }
            }
            val connectedSensors =
                _sensorInfoList.value.count { it.status == SensorConnectionStatus.CONNECTED }
            _systemStatus.value = "System ready. $connectedSensors sensors connected."
            _isInitializing.value = false
        }
    }

    fun startRecording() {
        if (_sensorInfoList.value.none { it.status == SensorConnectionStatus.CONNECTED }) {
            _systemStatus.value = "Error: No sensors connected. Cannot start recording."
            return
        }
        viewModelScope.launch {
            _recordingState.value = RecordingState.RECORDING
            _systemStatus.value = "Recording started with fault-tolerant mode enabled."
            // Simulate recording progress
            var seconds = 0
            var frameCount = 0
            var thermalFrames = 0
            var gsrSamples = 0
            while (_recordingState.value == RecordingState.RECORDING) {
                delay(1000)
                seconds++
                frameCount += DEFAULT_PLAYBACK_FPS
                thermalFrames += THERMAL_FPS
                gsrSamples += GSR_SAMPLING_RATE_HZ
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val duration = String.format("%02d:%02d:%02d", hours, minutes, secs)
                val dataSize = "${(seconds * 0.8).toInt()} MB"
                _sessionInfo.value = _sessionInfo.value.copy(
                    duration = duration,
                    dataSize = dataSize,
                    frameCount = frameCount,
                    thermalFrames = thermalFrames,
                    gsrSamples = gsrSamples
                )
                // Update sensor data rates
                _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
                    if (sensor.status == SensorConnectionStatus.CONNECTED) {
                        sensor.copy(lastUpdate = "Recording - ${duration}")
                    } else sensor
                }
            }
        }
    }

    fun stopRecording() {
        _recordingState.value = RecordingState.IDLE
        _systemStatus.value = "Recording stopped. Data saved successfully."
        // Reset sensor status
        _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
            if (sensor.status == SensorConnectionStatus.CONNECTED) {
                sensor.copy(lastUpdate = "Connected - Idle")
            } else sensor
        }
    }

    fun reconnectSensor(sensorName: String) {
        viewModelScope.launch {
            _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
                if (sensor.name == sensorName) {
                    sensor.copy(status = SensorConnectionStatus.CONNECTING, errorMessage = null)
                } else sensor
            }
            delay(2000)
            // Simulate reconnection attempt (70% success rate)
            val isConnected = kotlin.random.Random.nextFloat() > 0.3f
            _sensorInfoList.value = _sensorInfoList.value.map { sensor ->
                if (sensor.name == sensorName) {
                    sensor.copy(
                        status = if (isConnected) SensorConnectionStatus.CONNECTED else SensorConnectionStatus.ERROR,
                        lastUpdate = if (isConnected) "Reconnected" else "Reconnection failed",
                        errorMessage = if (!isConnected) "Reconnection attempt failed" else null
                    )
                } else sensor
            }
        }
    }
}

class FaultTolerantRecordingComposeActivity :
    BaseComposeActivity<FaultTolerantRecordingViewModel>() {
    override fun createViewModel(): FaultTolerantRecordingViewModel =
        viewModels<FaultTolerantRecordingViewModel>().value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModels<FaultTolerantRecordingViewModel>().value.initializeSystem()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: FaultTolerantRecordingViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val recordingState by viewModel.recordingState
            val sessionInfo by viewModel.sessionInfo
            val sensorInfoList by viewModel.sensorInfoList
            val systemStatus by viewModel.systemStatus
            val isInitializing by viewModel.isInitializing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Fault-Tolerant Recording",
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // System status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (recordingState) {
                                RecordingState.RECORDING -> MaterialTheme.colorScheme.errorContainer
                                RecordingState.IDLE -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isInitializing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = when (recordingState) {
                                        RecordingState.RECORDING -> Icons.Default.FiberManualRecord
                                        RecordingState.IDLE -> Icons.Default.Circle
                                        else -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = when (recordingState) {
                                        RecordingState.RECORDING -> MaterialTheme.colorScheme.error
                                        RecordingState.IDLE -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (recordingState) {
                                        RecordingState.RECORDING -> "Recording Active"
                                        RecordingState.IDLE -> "System Ready"
                                        else -> "System Status"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = systemStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Recording session info
                    if (recordingState == RecordingState.RECORDING) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Recording Session",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InfoColumn("Duration", sessionInfo.duration)
                                    InfoColumn("Data Size", sessionInfo.dataSize)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InfoColumn("RGB Frames", sessionInfo.frameCount.toString())
                                    InfoColumn(
                                        "Thermal Frames",
                                        sessionInfo.thermalFrames.toString()
                                    )
                                    InfoColumn("GSR Samples", sessionInfo.gsrSamples.toString())
                                }
                            }
                        }
                    }
                    // Sensor status
                    Text(
                        text = "Sensor Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sensorInfoList.forEach { sensor ->
                        SensorStatusCard(
                            sensor = sensor,
                            onReconnect = { viewModel.reconnectSensor(sensor.name) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (recordingState == RecordingState.RECORDING) {
                            Button(
                                onClick = { viewModel.stopRecording() },
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
                        } else {
                            Button(
                                onClick = { viewModel.startRecording() },
                                modifier = Modifier.weight(1f),
                                enabled = !isInitializing && sensorInfoList.any { it.status == SensorConnectionStatus.CONNECTED }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Recording")
                            }
                            OutlinedButton(
                                onClick = { viewModel.initializeSystem() },
                                modifier = Modifier.weight(1f),
                                enabled = !isInitializing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reinitialize")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Help information
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fault-Tolerant Features",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Automatic sensor reconnection on failure\n• Data integrity verification during recording\n• Graceful degradation if sensors disconnect\n• Comprehensive error logging and recovery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SensorStatusCard(
    sensor: SensorInfo,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (sensor.status) {
                SensorConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                SensorConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                SensorConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
                SensorConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sensor.status == SensorConnectionStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = when (sensor.status) {
                        SensorConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                        SensorConnectionStatus.ERROR -> Icons.Default.Error
                        else -> Icons.Default.Circle
                    },
                    contentDescription = null,
                    tint = when (sensor.status) {
                        SensorConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                        SensorConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${
                        sensor.status.name.lowercase().replaceFirstChar { it.uppercase() }
                    } • ${sensor.dataRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sensor.lastUpdate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sensor.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (sensor.status == SensorConnectionStatus.ERROR) {
                OutlinedButton(
                    onClick = onReconnect
                ) {
                    Text("Reconnect")
                }
            }
        }
    }
}