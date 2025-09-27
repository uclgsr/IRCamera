package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay

class CameraPreviewComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            CameraPreviewScreen()
        }
    }

    @Composable
    private fun CameraPreviewScreen() {
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var thermalOverlayEnabled by remember { mutableStateOf(true) }
        var currentTemperature by remember { mutableStateOf(25.4f) }
        var flashMode by remember { mutableStateOf("Auto") }
        var zoomLevel by remember { mutableStateOf(1.0f) }

        // Simulate recording timer
        LaunchedEffect(isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "Camera Preview",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { thermalOverlayEnabled = !thermalOverlayEnabled }) {
                            Icon(
                                if (thermalOverlayEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Thermal Overlay"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Camera Preview (placeholder)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera Preview\n${if (thermalOverlayEnabled) "Thermal Overlay ON" else "RGB Mode"}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                // Thermal overlay info
                if (thermalOverlayEnabled) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Temperature",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${currentTemperature}°C",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Recording status
                if (isRecording) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REC ${formatDuration(recordingDuration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Camera controls
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Zoom control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zoom",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Slider(
                                value = zoomLevel,
                                onValueChange = { zoomLevel = it },
                                valueRange = 1.0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${zoomLevel.toInt()}x",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Control buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flash mode
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = { 
                                        flashMode = when (flashMode) {
                                            "Auto" -> "On"
                                            "On" -> "Off"
                                            else -> "Auto"
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        when (flashMode) {
                                            "Auto" -> Icons.Default.FlashAuto
                                            "On" -> Icons.Default.FlashOn
                                            else -> Icons.Default.FlashOff
                                        },
                                        contentDescription = "Flash",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = flashMode,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Capture button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clickable { /* Take photo */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }

                            // Record button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = { 
                                        if (isRecording) {
                                            recordingDuration = 0
                                        }
                                        isRecording = !isRecording 
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isRecording) Color.Red else Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = if (isRecording) "Stop" else "Record",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Camera info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1920x1080 • 30fps",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = if (thermalOverlayEnabled) "Thermal Mode" else "RGB Mode",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
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
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}