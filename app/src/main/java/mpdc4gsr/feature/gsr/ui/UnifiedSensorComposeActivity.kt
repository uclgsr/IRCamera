package mpdc4gsr.feature.gsr.ui

import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.SessionQuality
import mpdc4gsr.core.data.model.SessionStatus
import mpdc4gsr.core.data.model.SessionType
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class UnifiedSensorType(
    val displayName: String,
    val icon: ImageVector,
    val description: String
) {
    THERMAL("Thermal Camera", Icons.Default.Thermostat, "TC001/TS004 thermal imaging sensors"),
    GSR("GSR Sensor", Icons.Default.Sensors, "Galvanic skin response monitoring"),
    RGB_CAMERA("RGB Camera", Icons.Default.Camera, "High-resolution RGB camera recording"),
    AUDIO("Audio", Icons.Default.Audiotrack, "Audio recording"),
    NETWORK("Network", Icons.Default.NetworkCheck, "Network connectivity and data transmission")
}

data class SensorStatus(
    val type: UnifiedSensorType,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val quality: String = "Unknown",
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never"
)

data class UnifiedSessionInfo(
    val name: String = "New Session",
    val type: SessionType = SessionType.RESEARCH,
    val quality: SessionQuality = SessionQuality(),
    val status: SessionStatus = SessionStatus.IDLE,
    val duration: String = "00:00:00",
    val dataSize: String = "0 MB"
)

class UnifiedSensorViewModel : AppBaseViewModel() {
    private val _sensorStatuses = mutableStateOf(
        UnifiedSensorType.values().map { type ->
            SensorStatus(
                type = type,
                isConnected = false,
                quality = "Disconnected"
            )
        }
    )
    val sensorStatuses: State<List<SensorStatus>> = _sensorStatuses
    private val _sessionInfo = mutableStateOf(UnifiedSessionInfo())
    val sessionInfo: State<UnifiedSessionInfo> = _sessionInfo
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording
    private val _connectedDevices = mutableStateOf<List<DeviceInfo>>(emptyList())
    val connectedDevices: State<List<DeviceInfo>> = _connectedDevices
    fun connectSensor(sensorType: UnifiedSensorType) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Simulate connection process
            delay(2000)
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.type == sensorType) {
                    status.copy(
                        isConnected = true,
                        quality = "Good",
                        dataRate = when (sensorType) {
                            UnifiedSensorType.THERMAL -> "125 KB/s"
                            UnifiedSensorType.GSR -> "2 KB/s"
                            UnifiedSensorType.RGB_CAMERA -> "1.2 MB/s"
                            UnifiedSensorType.AUDIO -> "64 KB/s"
                            UnifiedSensorType.NETWORK -> "10 MB/s"
                        },
                        lastUpdate = "Just now"
                    )
                } else status
            }
        }
    }

    fun disconnectSensor(sensorType: UnifiedSensorType) {
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            if (status.type == sensorType) {
                status.copy(
                    isConnected = false,
                    isRecording = false,
                    quality = "Disconnected",
                    dataRate = "0 KB/s",
                    lastUpdate = "Disconnected"
                )
            } else status
        }
    }

    fun startRecording() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRecording.value = true
            _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.RECORDING)
            // Update sensor recording status
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.isConnected) {
                    status.copy(isRecording = true)
                } else status
            }
            // Simulate recording time updates
            var seconds = 0
            while (_isRecording.value) {
                delay(1000)
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val duration = String.format("%02d:%02d:%02d", hours, minutes, secs)
                val dataSize = "${(seconds * 0.5).toInt()} MB" // Simulate growing data
                _sessionInfo.value = _sessionInfo.value.copy(
                    duration = duration,
                    dataSize = dataSize
                )
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.IDLE)
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            status.copy(isRecording = false)
        }
    }

    fun updateSessionName(name: String) {
        _sessionInfo.value = _sessionInfo.value.copy(name = name)
    }
}

@AndroidEntryPoint
class UnifiedSensorComposeActivity : BaseComposeActivity<UnifiedSensorViewModel>() {
    override fun createViewModel(): UnifiedSensorViewModel =
        viewModels<UnifiedSensorViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: UnifiedSensorViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val sensorStatuses by viewModel.sensorStatuses
            val sessionInfo by viewModel.sessionInfo
            val isRecording by viewModel.isRecording
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Unified Sensor Control",
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Session info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (isRecording)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sessionInfo.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${sessionInfo.status} • ${sessionInfo.duration} • ${sessionInfo.dataSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isRecording) {
                                    Button(
                                        onClick = { viewModel.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Stop")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startRecording() },
                                        enabled = sensorStatuses.any { it.isConnected }
                                    ) {
                                        Text("Start Recording")
                                    }
                                }
                            }
                        }
                    }
                    // RGB Camera preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PreviewView(context).apply {
                                        // Camera preview will be initialized here
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay when camera is not connected
                            val rgbCameraStatus =
                                sensorStatuses.find { it.type == UnifiedSensorType.RGB_CAMERA }
                            if (rgbCameraStatus?.isConnected != true) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black.copy(alpha = 0.8f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideocamOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Preview",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Connect RGB camera to view",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Sensor status cards
                    Text(
                        text = "Sensor Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sensorStatuses.forEach { sensorStatus ->
                        SensorStatusCard(
                            sensorStatus = sensorStatus,
                            onConnect = { viewModel.connectSensor(it) },
                            onDisconnect = { viewModel.disconnectSensor(it) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Quick actions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.connectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Connect All")
                                }
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.disconnectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disconnect All")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusCard(
    sensorStatus: SensorStatus,
    onConnect: (UnifiedSensorType) -> Unit,
    onDisconnect: (UnifiedSensorType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                sensorStatus.isRecording -> MaterialTheme.colorScheme.errorContainer
                sensorStatus.isConnected -> MaterialTheme.colorScheme.primaryContainer
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
            Icon(
                imageVector = sensorStatus.type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                    sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorStatus.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sensorStatus.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Quality: ${sensorStatus.quality} • Rate: ${sensorStatus.dataRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = when {
                        sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                        sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            sensorStatus.isRecording -> "Recording"
                            sensorStatus.isConnected -> "Connected"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            sensorStatus.isRecording -> MaterialTheme.colorScheme.onError
                            sensorStatus.isConnected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (sensorStatus.isConnected) {
                    OutlinedButton(
                        onClick = { onDisconnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Disconnect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Button(
                        onClick = { onConnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
