package mpdc4gsr.presentation.screens.gsr

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.ui.components.TitleBar
import mpdc4gsr.ui.components.TitleBarAction
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MultiModalRecordingScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var gsrEnabled by remember { mutableStateOf(true) }
    var thermalEnabled by remember { mutableStateOf(true) }
    var rgbEnabled by remember { mutableStateOf(true) }
    var syncStatus by remember { mutableStateOf(SyncStatus.SYNCED) }
    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Multi-Modal Recording",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Recording Settings",
                onClick = {
                    // TODO: Implement recording settings screen
                    // Open settings for multi-modal recording configuration
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Status Card
            RecordingStatusCard(
                isRecording = isRecording,
                duration = recordingDuration,
                syncStatus = syncStatus
            )
            // Sensor Status Cards
            SensorStatusSection(
                gsrEnabled = gsrEnabled,
                thermalEnabled = thermalEnabled,
                rgbEnabled = rgbEnabled,
                onGsrToggle = { gsrEnabled = it },
                onThermalToggle = { thermalEnabled = it },
                onRgbToggle = { rgbEnabled = it },
                isRecording = isRecording
            )
            // Live Data Preview
            if (isRecording) {
                LiveDataPreviewSection()
            }
            // Recording Controls
            RecordingControlsSection(
                isRecording = isRecording,
                onStartStop = {
                    isRecording = !isRecording
                    if (!isRecording) recordingDuration = 0
                },
                canRecord = gsrEnabled || thermalEnabled || rgbEnabled
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Int,
    syncStatus: SyncStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color.Red.copy(alpha = 0.1f) else Color(0xFF2A2A2A)
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
                    text = if (isRecording) "RECORDING" else "READY",
                    color = if (isRecording) Color.Red else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sync: ${syncStatus.displayName}",
                    color = syncStatus.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SensorStatusSection(
    gsrEnabled: Boolean,
    thermalEnabled: Boolean,
    rgbEnabled: Boolean,
    onGsrToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRgbToggle: (Boolean) -> Unit,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sensor Configuration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SensorToggleItem(
                name = "GSR Sensor",
                description = "Galvanic Skin Response (Shimmer3)",
                enabled = gsrEnabled,
                onToggle = onGsrToggle,
                color = Color.Cyan,
                isRecording = isRecording,
                status = "Connected • 128 Hz"
            )
            SensorToggleItem(
                name = "Thermal Camera",
                description = "TOPDON TC001 Thermal Imaging",
                enabled = thermalEnabled,
                onToggle = onThermalToggle,
                color = Color.Red,
                isRecording = isRecording,
                status = "Connected • 256×192"
            )
            SensorToggleItem(
                name = "RGB Camera",
                description = "Built-in Camera",
                enabled = rgbEnabled,
                onToggle = onRgbToggle,
                color = Color.White,
                isRecording = isRecording,
                status = "Ready • 1080p@30fps"
            )
        }
    }
}

@Composable
private fun SensorToggleItem(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    color: Color,
    isRecording: Boolean,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (enabled) color else Color.Gray,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = status,
                color = if (enabled) color else Color.Gray,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = if (!isRecording) onToggle else {
                {}
            },
            enabled = !isRecording,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = color.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun LiveDataPreviewSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // GSR Waveform
            Text(
                text = "GSR Signal",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LiveGSRWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Sensor Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveMetricItem("GSR", "0.42 μS", Color.Cyan)
                LiveMetricItem("Thermal", "36.8°C", Color.Red)
                LiveMetricItem("RGB", "Recording", Color.White)
            }
        }
    }
}

@Composable
private fun LiveGSRWaveform(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            phase += 0.2f
        }
    }
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val amplitude = height * 0.3f
        val points = (0..100).map { i ->
            val x = (i / 100f) * width
            val y = centerY + amplitude * sin((i * 0.2f) + phase + Random.nextFloat() * 0.1f)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.Cyan,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx()
            )
        }
        // Grid lines
        for (i in 0..4) {
            val y = (height / 4) * i
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun LiveMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
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
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecordingControlsSection(
    isRecording: Boolean,
    onStartStop: () -> Unit,
    canRecord: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Main Record Button
            FloatingActionButton(
                onClick = onStartStop,
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Tap to stop recording" else "Tap to start synchronized recording",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (!canRecord && !isRecording) {
                Text(
                    text = "Enable at least one sensor to record",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            // Additional Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement pause recording functionality
                        // Pause the multi-modal recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement add marker functionality
                        // Add timestamp marker to recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark")
                }
            }
        }
    }
}

enum class SyncStatus(val displayName: String, val color: Color) {
    SYNCED("Synced", Color.Green),
    SYNCING("Syncing", Color.Yellow),
    OUT_OF_SYNC("Out of Sync", Color.Red),
    DISABLED("Disabled", Color.Gray)
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%d:%02d", minutes, remainingSeconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiModalRecordingScreenPreview() {
    IRCameraTheme {
        MultiModalRecordingScreen()
    }
}