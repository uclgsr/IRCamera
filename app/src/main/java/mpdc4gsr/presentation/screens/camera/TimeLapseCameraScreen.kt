package mpdc4gsr.presentation.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.presentation.TimeLapseCameraViewModel
import mpdc4gsr.feature.camera.presentation.TimeLapseMode

@Composable
fun TimeLapseCameraScreen(
    viewModel: TimeLapseCameraViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeLapseState by viewModel.timeLapseState.collectAsState()
    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Time-Lapse Camera",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = Color(0xFF16131e)
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera Preview Placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D2A3E)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Camera Preview",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                // Recording Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (timeLapseState.isRecording) "Recording" else "Ready",
                                color = if (timeLapseState.isRecording)
                                    MaterialTheme.colorScheme.primary
                                else Color.Gray
                            )
                        }
                        HorizontalDivider()
                        InfoRow("Frames Captured", "${timeLapseState.capturedFrames}")
                        InfoRow("Interval", "${timeLapseState.intervalSeconds}s")
                        InfoRow("Est. Video Length", "${timeLapseState.estimatedVideoLength}s")
                        InfoRow("Duration", "${timeLapseState.totalDuration}s")
                    }
                }
                // Mode Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TimeLapseMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = timeLapseState.mode == mode,
                                    onClick = { viewModel.setMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(mode.displayName)
                            }
                        }
                    }
                }
                // Manual Interval Control
                if (timeLapseState.mode == TimeLapseMode.MANUAL) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Custom Interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${timeLapseState.intervalSeconds} seconds")
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds - 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds + 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                            Slider(
                                value = timeLapseState.intervalSeconds.toFloat(),
                                onValueChange = { viewModel.updateInterval(it.toInt()) },
                                valueRange = 1f..60f,
                                steps = 58
                            )
                        }
                    }
                }
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!timeLapseState.isRecording) {
                        Button(
                            onClick = { viewModel.startTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }
                // Error Display
                timeLapseState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLapseCameraScreenPreview() {
    IRCameraTheme {
        TimeLapseCameraScreen()
    }
}
