package mpdc4gsr.feature.capture.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.theme.IRCameraTheme
import mpdc4gsr.feature.capture.camera.presentation.TimeLapseCameraViewModel
import mpdc4gsr.feature.capture.camera.presentation.TimeLapseMode

@Composable
fun TimeLapseCameraScreen(
    viewModel: TimeLapseCameraViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val timeLapseState by viewModel.timeLapseState.collectAsState()
    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Time-Lapse Camera",
                    showBackButton = true,
                    onBackClick = onBackClick,
                )
            },
            containerColor = Color(0xFF16131e),
        ) { paddingValues ->
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CameraStatusCard(
                    isPreviewActive = true,
                    isRecording = timeLapseState.isRecording,
                    resolution = timeLapseState.resolution,
                    frameRate = 30,
                    exposureTime = "${timeLapseState.intervalSeconds}s",
                    iso = timeLapseState.quality,
                    focusMode = timeLapseState.mode.displayName,
                    whiteBalance = "Auto",
                )
                RecordingControlsCard(
                    isRecording = timeLapseState.isRecording,
                    isPreviewActive = true,
                    recordingDuration = timeLapseState.totalDuration,
                    capturedFrames = timeLapseState.capturedFrames,
                    onToggleRecording = {
                        if (timeLapseState.isRecording) {
                            viewModel.stopTimeLapse()
                        } else {
                            viewModel.startTimeLapse()
                        }
                    },
                    onTogglePreview = {
                        android.widget.Toast
                            .makeText(
                                context,
                                "Live preview handled by RGB camera",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    },
                    onCapturePhoto = { viewModel.captureFrame() },
                )
                TimeLapseMetricsCard(timeLapseState)
                // Mode Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TimeLapseMode.entries.forEach { mode ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = timeLapseState.mode == mode,
                                    onClick = { viewModel.setMode(mode) },
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
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Custom Interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${timeLapseState.intervalSeconds} seconds")
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds - 1,
                                            )
                                        },
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds + 1,
                                            )
                                        },
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                            Slider(
                                value = timeLapseState.intervalSeconds.toFloat(),
                                onValueChange = { viewModel.updateInterval(it.toInt()) },
                                valueRange = 1f..60f,
                                steps = 58,
                            )
                        }
                    }
                }
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!timeLapseState.isRecording) {
                        Button(
                            onClick = { viewModel.startTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
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
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeLapseMetricsCard(state: TimeLapseCameraViewModel.TimeLapseState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Recording Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            InfoRow("Frames Captured", state.capturedFrames.toString())
            InfoRow("Interval", "${state.intervalSeconds}s")
            InfoRow("Est. Video Length", "${state.estimatedVideoLength}s")
            InfoRow("Elapsed Duration", "${state.totalDuration}s")
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            fontWeight = FontWeight.Medium,
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
