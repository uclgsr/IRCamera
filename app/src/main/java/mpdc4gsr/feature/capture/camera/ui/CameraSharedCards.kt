package mpdc4gsr.feature.capture.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.designsystem.theme.Green
import mpdc4gsr.core.designsystem.theme.Orange
import mpdc4gsr.core.designsystem.theme.Purple

@Composable
fun CameraStatusCard(
    isPreviewActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Camera Status",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color =
                        when {
                            isRecording -> Color.Red.copy(alpha = 0.2f)
                            isPreviewActive -> Color.Green.copy(alpha = 0.2f)
                            else -> Color.Gray.copy(alpha = 0.2f)
                        },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text =
                            when {
                                isRecording -> "RECORDING"
                                isPreviewActive -> "PREVIEW"
                                else -> "STANDBY"
                            },
                        color =
                            when {
                                isRecording -> Color.Red
                                isPreviewActive -> Color.Green
                                else -> Color.Gray
                            },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem("Resolution", resolution, Color.White)
                MetricItem("Frame Rate", "${frameRate}fps", Color.Green)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem("Exposure", exposureTime, Color.Yellow)
                MetricItem("ISO", iso.toString(), Color.Cyan)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem("Focus", focusMode, MaterialTheme.colorScheme.primary)
                MetricItem("White Balance", whiteBalance, Color.Magenta)
            }
        }
    }
}

@Composable
fun RecordingControlsCard(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onTogglePreview: () -> Unit,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            if (isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MetricItem("Duration", "${recordingDuration}s", Color.Red)
                    MetricItem("Frames", capturedFrames.toString(), Color.Green)
                    MetricItem("File Size", "${(recordingDuration * 2.5f).toInt()}MB", Color.Cyan)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = onTogglePreview,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (isPreviewActive) Orange else Green,
                        ),
                ) {
                    Text(if (isPreviewActive) "Stop Preview" else "Start Preview")
                }
                Button(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.VideoCall,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                Button(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Photo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
fun CameraSettingsCard(
    resolution: String,
    frameRate: Int,
    focusMode: String,
    whiteBalance: String,
    currentFocusMode: mpdc4gsr.feature.capture.camera.presentation.FocusMode,
    currentWhiteBalance: mpdc4gsr.feature.capture.camera.presentation.WhiteBalance,
    onResolutionChange: (String) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onFocusModeChange: (mpdc4gsr.feature.capture.camera.presentation.FocusMode) -> Unit,
    onWhiteBalanceChange: (mpdc4gsr.feature.capture.camera.presentation.WhiteBalance) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Camera Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { onResolutionChange("1920A-1080") },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (resolution == "1920A-1080") MaterialTheme.colorScheme.primary else Color.Gray,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("1080p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onResolutionChange("1280A-720") },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (resolution == "1280A-720") MaterialTheme.colorScheme.primary else Color.Gray,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("720p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onFrameRateChange(if (frameRate == 30) 60 else 30) },
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${frameRate}fps", fontSize = 10.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { onFocusModeChange(currentFocusMode.getNext()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Focus: $focusMode", fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onWhiteBalanceChange(currentWhiteBalance.getNext()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("WB: $whiteBalance", fontSize = 9.sp)
                }
            }
            Text(
                text = "Advanced exposure and ISO controls available in camera settings menu",
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
        )
    }
}

