package mpdc4gsr.activities

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AppFeature(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true
) {
    THERMAL_CAMERA("Thermal Camera", "TC001/TS004 thermal imaging", Icons.Default.Thermostat),
    GSR_SENSOR("GSR Monitoring", "Galvanic skin response data", Icons.Default.Sensors),
    RGB_CAMERA("RGB Camera", "High-resolution image capture", Icons.Default.Camera),
    AUDIO_RECORDING("Audio Recording", "High-quality audio capture", Icons.Default.Mic),
    DATA_EXPORT("Data Export", "Export collected data", Icons.Default.FileDownload),
    NETWORK_SYNC("Network Sync", "Cloud synchronization", Icons.Default.CloudSync, false)
}

data class SystemInfo(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val buildType: String = BuildConfig.BUILD_TYPE,
    val timestamp: String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    ).format(Date()),
    val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    val androidVersion: String = android.os.Build.VERSION.RELEASE
)

data class PermissionStatus(
    val name: String,
    val isGranted: Boolean,
    val description: String
)

class SimplifiedMainActivityViewModel : BaseViewModel() {
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val _systemInfo = mutableStateOf(SystemInfo())
    val systemInfo: State<SystemInfo> = _systemInfo

    private val _recordingDuration = mutableStateOf("00:00:00")
    val recordingDuration: State<String> = _recordingDuration

    private val _connectionStatus = mutableStateOf("Disconnected")
    val connectionStatus: State<String> = _connectionStatus

    private val _permissionStatuses = mutableStateOf<List<PermissionStatus>>(emptyList())
    val permissionStatuses: State<List<PermissionStatus>> = _permissionStatuses

    private val _selectedFeatures = mutableStateOf(
        AppFeature.values().filter { it.isEnabled }.toSet()
    )
    val selectedFeatures: State<Set<AppFeature>> = _selectedFeatures

    fun updatePermissionStatuses(permissions: Map<String, Boolean>) {
        _permissionStatuses.value = listOf(
            PermissionStatus(
                "Camera",
                permissions[Manifest.permission.CAMERA] ?: false,
                "Required for RGB camera"
            ),
            PermissionStatus(
                "Audio",
                permissions[Manifest.permission.RECORD_AUDIO] ?: false,
                "Required for audio recording"
            ),
            PermissionStatus(
                "Storage",
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false,
                "Required for data storage"
            ),
            PermissionStatus(
                "Location",
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false,
                "Required for GPS metadata"
            ),
            PermissionStatus(
                "Bluetooth",
                permissions[Manifest.permission.BLUETOOTH] ?: false,
                "Required for sensor connection"
            )
        )
    }

    fun startRecording() {
        if (!_isConnected.value) {
            // First connect, then start recording
            connectDevices {
                if (_isConnected.value) {
                    beginRecording()
                }
            }
        } else {
            beginRecording()
        }
    }

    private fun beginRecording() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _isRecording.value = true
            var seconds = 0

            while (_isRecording.value) {
                delay(1000)
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val duration = String.format("%02d:%02d:%02d", hours, minutes, secs)
                _recordingDuration.value = duration
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _recordingDuration.value = "00:00:00"
    }

    fun connectDevices(onComplete: () -> Unit = {}) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _connectionStatus.value = "Connecting to devices..."

            // Simulate device connection process
            val devices = listOf("Thermal Camera", "GSR Sensor", "Audio Interface")
            devices.forEach { device ->
                _connectionStatus.value = "Connecting to $device..."
                delay(1500)
            }

            delay(1000)
            _isConnected.value = true
            _connectionStatus.value = "All devices connected"
            onComplete()
        }
    }

    fun disconnectDevices() {
        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
        if (_isRecording.value) {
            stopRecording()
        }
    }

    fun toggleFeature(feature: AppFeature) {
        _selectedFeatures.value = if (_selectedFeatures.value.contains(feature)) {
            _selectedFeatures.value - feature
        } else {
            _selectedFeatures.value + feature
        }
    }
}

class SimplifiedMainActivityCompose : BaseComposeActivity<SimplifiedMainActivityViewModel>() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModels<SimplifiedMainActivityViewModel>().value.updatePermissionStatuses(permissions)
    }

    override fun createViewModel(): SimplifiedMainActivityViewModel =
        viewModels<SimplifiedMainActivityViewModel>().value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request permissions on startup
        permissionLauncher.launch(requiredPermissions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SimplifiedMainActivityViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val isRecording by viewModel.isRecording
            val isConnected by viewModel.isConnected
            val systemInfo by viewModel.systemInfo
            val recordingDuration by viewModel.recordingDuration
            val connectionStatus by viewModel.connectionStatus
            val permissionStatuses by viewModel.permissionStatuses
            val selectedFeatures by viewModel.selectedFeatures

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Simplified Interface",
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Check Permissions",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Quick status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isRecording -> MaterialTheme.colorScheme.errorContainer
                                isConnected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when {
                                    isRecording -> Icons.Default.FiberManualRecord
                                    isConnected -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Circle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = when {
                                    isRecording -> MaterialTheme.colorScheme.error
                                    isConnected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when {
                                    isRecording -> "Recording Active"
                                    isConnected -> "Ready to Record"
                                    else -> "Disconnected"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRecording) recordingDuration else connectionStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isRecording) {
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
                                enabled = permissionStatuses.isNotEmpty() && permissionStatuses.all { it.isGranted }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isConnected) "Start Recording" else "Connect & Record")
                            }

                            if (isConnected) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectDevices() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Disconnect")
                                }
                            }
                        }
                    }

                    // Feature selection
                    Text(
                        text = "Available Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AppFeature.values().forEach { feature ->
                        FeatureCard(
                            feature = feature,
                            isSelected = selectedFeatures.contains(feature),
                            onToggle = { viewModel.toggleFeature(feature) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission status
                    if (permissionStatuses.isNotEmpty()) {
                        Text(
                            text = "Permission Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                permissionStatuses.forEach { permission ->
                                    PermissionRow(
                                        permission = permission,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // System information
                    Text(
                        text = "System Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            InfoRow("App Version", systemInfo.appVersion)
                            InfoRow("Build Type", systemInfo.buildType)
                            InfoRow("Device", systemInfo.deviceModel)
                            InfoRow("Android", systemInfo.androidVersion)
                            InfoRow("Timestamp", systemInfo.timestamp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: AppFeature,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && feature.isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected && feature.isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (feature.isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!feature.isEnabled) {
                    Text(
                        text = "Coming soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Switch(
                checked = isSelected,
                onCheckedChange = { if (feature.isEnabled) onToggle() },
                enabled = feature.isEnabled
            )
        }
    }
}

@Composable
private fun PermissionRow(
    permission: PermissionStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (permission.isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (permission.isGranted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (permission.isGranted) "Granted" else "Denied",
            style = MaterialTheme.typography.labelSmall,
            color = if (permission.isGranted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}