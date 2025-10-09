package mpdc4gsr.presentation.screens.camera

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import mpdc4gsr.ui.theme.IRCameraTheme
import mpdc4gsr.presentation.screens.camera.DualModeCameraViewModel

class DualModeCameraComposeActivity : BaseComposeActivity<DualModeCameraViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DualModeCameraComposeActivity::class.java))
        }

        fun startWithMode(context: Context, mode: String) {
            val intent = Intent(context, DualModeCameraComposeActivity::class.java).apply {
                putExtra("INITIAL_MODE", mode)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): DualModeCameraViewModel {
        return viewModels<DualModeCameraViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DualModeCameraViewModel) {
        val localContext = this@DualModeCameraComposeActivity
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var cameraMode by remember { mutableStateOf("Dual") }
        var showSettingsDialog by remember { mutableStateOf(false) }
        IRCameraTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dual Mode Camera",
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
                                // TODO: Switch between front/back camera
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Switch camera feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch")
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    localContext,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                DualModeCameraContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    cameraMode = cameraMode,
                    onCameraModeChange = { cameraMode = it },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showSettingsDialog) {
            CameraSettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onSaveSettings = { settings ->
                    // Apply camera settings
                    showSettingsDialog = false
                }
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    viewModel: DualModeCameraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Camera Preview Area
        CameraPreviewSection(
            cameraMode = cameraMode,
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        // Camera Controls
        CameraControlsSection(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            recordingDuration = recordingDuration,
            cameraMode = cameraMode,
            onCameraModeChange = onCameraModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraPreviewSection(
    cameraMode: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        when (cameraMode) {
            "Dual" -> {
                // Dual camera view with picture-in-picture
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main thermal preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ThermalCameraPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                        // RGB camera PiP
                        Card(
                            modifier = Modifier
                                .size(120.dp, 160.dp)
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            RGBCameraPreview(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Temperature overlay
                        TemperatureOverlay(
                            centerTemp = 36.8f,
                            maxTemp = 42.1f,
                            minTemp = 28.3f,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }

            "Thermal" -> {
                ThermalCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "RGB" -> {
                RGBCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "Split" -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ThermalCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RGBCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        // Recording indicator
        if (isRecording) {
            RecordingIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        // Camera mode indicator
        CameraModeIndicator(
            mode = cameraMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraControlsSection(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
    ) {
        // Camera mode selector
        CameraModeSelector(
            selectedMode = cameraMode,
            onModeChange = onCameraModeChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Recording status
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            OutlinedButton(
                onClick = {
                    // TODO: Open gallery to view captured photos/videos
                    android.widget.Toast.makeText(
                        localContext,
                        "Gallery feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            }
            // Record button
            Button(
                onClick = onRecordingToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "STOP" else "RECORD",
                    fontWeight = FontWeight.Bold
                )
            }
            // Capture button
            OutlinedButton(
                onClick = {
                    // TODO: Capture photo from dual camera
                    android.widget.Toast.makeText(
                        localContext,
                        "Photo captured",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Capture"
                )
            }
        }
    }
}

@Composable
private fun CameraModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Dual", "Thermal", "RGB", "Split")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(mode) },
                leadingIcon = if (selectedMode == mode) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
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
                MaterialTheme.colorScheme.surfaceVariant
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
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Dual-mode ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ThermalCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Thermostat,
                contentDescription = "Thermal Camera",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFF6B35)
            )
            Text(
                text = "Thermal Camera Preview",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RGBCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for RGB camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Camera,
                contentDescription = "RGB Camera",
                modifier = Modifier.size(if (modifier == Modifier.fillMaxSize()) 64.dp else 32.dp),
                tint = Color(0xFF2196F3)
            )
            if (modifier == Modifier.fillMaxSize()) {
                Text(
                    text = "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TemperatureOverlay(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "TEMP",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${centerTemp}°C",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "H:${maxTemp}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L:${minTemp}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE53E3E)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraModeIndicator(
    mode: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Text(
            text = mode.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CameraSettingsDialog(
    onDismiss: () -> Unit,
    onSaveSettings: (Map<String, Any>) -> Unit
) {
    var videoQuality by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf(30f) }
    var enableStabilization by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HD", "FHD", "4K").forEach { quality ->
                        FilterChip(
                            selected = videoQuality == quality,
                            onClick = { videoQuality = quality },
                            label = { Text(quality) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Frame Rate: ${frameRate.toInt()} fps",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = frameRate,
                    onValueChange = { frameRate = it },
                    valueRange = 15f..60f,
                    steps = 8
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableStabilization,
                        onCheckedChange = { enableStabilization = it }
                    )
                    Text(
                        text = "Enable Image Stabilization",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(
                        mapOf(
                            "quality" to videoQuality,
                            "frameRate" to frameRate.toInt(),
                            "stabilization" to enableStabilization
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}