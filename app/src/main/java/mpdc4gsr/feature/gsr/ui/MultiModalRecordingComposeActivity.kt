package mpdc4gsr.feature.gsr.ui

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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.MultiModalRecordingViewModel

class MultiModalRecordingComposeActivity : BaseComposeActivity<MultiModalRecordingViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, MultiModalRecordingComposeActivity::class.java))
        }

        fun startWithTemplate(context: Context, templateId: String) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("template_id", templateId)
            }
            context.startActivity(intent)
        }

        fun startRecording(context: Context, sessionInfo: SessionInfo) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("session_info", sessionInfo)
                putExtra("auto_start", true)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): MultiModalRecordingViewModel {
        return viewModels<MultiModalRecordingViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MultiModalRecordingViewModel) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var selectedSensors by remember { mutableStateOf(setOf("gsr", "thermal", "rgb")) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Multi-Modal Recording",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Open recording templates
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Recording templates coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Templates")
                            }
                            IconButton(onClick = {
                                // TODO: Open recording settings
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Opening recording settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                MultiModalRecordingContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    selectedSensors = selectedSensors,
                    onSensorToggle = { sensor ->
                        selectedSensors = if (selectedSensors.contains(sensor)) {
                            selectedSensors - sensor
                        } else {
                            selectedSensors + sensor
                        }
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun MultiModalRecordingContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    selectedSensors: Set<String>,
    onSensorToggle: (String) -> Unit,
    viewModel: MultiModalRecordingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Recording Status Card
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Sensor Selection Cards
        Text(
            text = "Active Sensors",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SensorCard(
            title = "GSR Sensor",
            subtitle = "Shimmer3 GSR+ Device",
            icon = Icons.Default.MonitorHeart,
            isEnabled = selectedSensors.contains("gsr"),
            isConnected = false,
            onToggle = { onSensorToggle("gsr") },
            statusText = "128 Hz",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "Thermal Camera",
            subtitle = "TOPDON TC001 Device",
            icon = Icons.Default.Thermostat,
            isEnabled = selectedSensors.contains("thermal"),
            isConnected = false,
            onToggle = { onSensorToggle("thermal") },
            statusText = "25 FPS",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "RGB Camera",
            subtitle = "Device Camera",
            icon = Icons.Default.CameraAlt,
            isEnabled = selectedSensors.contains("rgb"),
            isConnected = false,
            onToggle = { onSensorToggle("rgb") },
            statusText = "30 FPS",
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // Recording Controls
        RecordingControls(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            canRecord = selectedSensors.isNotEmpty(),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Live Data Preview (if recording)
        if (isRecording) {
            LiveDataPreview(
                selectedSensors = selectedSensors,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                Color(0xFFE53E3E).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Tap record to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isConnected: Boolean,
    onToggle: () -> Unit,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.tertiaryContainer
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
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) statusText else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                enabled = isConnected
            )
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    canRecord: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main record button
        Button(
            onClick = onRecordingToggle,
            enabled = canRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Stop" else "Start",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) "STOP RECORDING" else "START RECORDING",
                fontWeight = FontWeight.Bold
            )
        }
        // Pause button (only show when recording)
        if (isRecording) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    // TODO: Implement pause recording logic
                    android.widget.Toast.makeText(
                        context,
                        "Pause recording feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause"
                )
            }
        }
    }
}

@Composable
private fun LiveDataPreview(
    selectedSensors: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            selectedSensors.forEach { sensor ->
                when (sensor) {
                    "gsr" -> {
                        Text(
                            text = "GSR: 2.45 µS (Normal)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "thermal" -> {
                        Text(
                            text = "Thermal: 36.8°C (Body temp detected)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "rgb" -> {
                        Text(
                            text = "RGB: 1920x1080 @ 30fps",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}