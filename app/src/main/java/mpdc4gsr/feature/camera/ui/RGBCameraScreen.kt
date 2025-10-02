package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.Green
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.Orange
import mpdc4gsr.core.ui.theme.Purple
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModel

/**
 * RGB Camera Screen - Dedicated interface for RGB camera control and recording
 * Now connected to RgbCameraRecorder via ViewModel
 */
@Composable
fun RGBCameraScreen(
    viewModel: RGBCameraViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()

    // Initialize camera on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(lifecycleOwner)
    }

    // Use real data from ViewModel
    val isPreviewActive = cameraState.isPreviewActive
    val isRecording = cameraState.isRecording
    val resolution = cameraState.resolution
    val frameRate = cameraState.frameRate
    val exposureTime = cameraState.exposureTime
    val iso = cameraState.iso
    val focusMode = cameraState.focusMode.displayName
    val whiteBalance = cameraState.whiteBalance.displayName
    val recordingDuration = cameraState.recordingDuration
    val capturedFrames = cameraState.capturedFrames

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar with camera-specific actions
        TitleBar(
            title = "RGB Camera",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.CameraAlt,
                contentDescription = "Capture Photo",
                onClick = onCapturePhoto
            )
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Camera Settings",
                onClick = onSettingsClick
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera preview - use real camera if available
            if (viewModel.getCameraRecorder() != null) {
                RealCameraPreview(
                    cameraRecorder = viewModel.getCameraRecorder()!!,
                    isRecording = isRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                // Fallback to simulated preview during initialization
                RGBCameraPreview(
                    isActive = isPreviewActive,
                    isRecording = isRecording,
                    resolution = resolution,
                    frameRate = frameRate
                )
            }

            // Camera status and metrics
            CameraStatusCard(
                isPreviewActive = isPreviewActive,
                isRecording = isRecording,
                resolution = resolution,
                frameRate = frameRate,
                exposureTime = exposureTime,
                iso = iso,
                focusMode = focusMode,
                whiteBalance = whiteBalance
            )

            // Recording controls
            RecordingControlsCard(
                isRecording = isRecording,
                isPreviewActive = isPreviewActive,
                recordingDuration = recordingDuration,
                capturedFrames = capturedFrames,
                onToggleRecording = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                onTogglePreview = { viewModel.togglePreview() },
                onCapturePhoto = {
                    viewModel.capturePhoto()
                    onCapturePhoto()
                }
            )

            // Camera settings
            CameraSettingsCard(
                resolution = resolution,
                frameRate = frameRate,
                exposureTime = exposureTime,
                iso = iso,
                focusMode = focusMode,
                whiteBalance = whiteBalance,
                currentFocusMode = cameraState.focusMode,
                currentWhiteBalance = cameraState.whiteBalance,
                onResolutionChange = { viewModel.updateResolution(it) },
                onFrameRateChange = { viewModel.updateFrameRate(it) },
                onExposureChange = { viewModel.updateExposureTime(it) },
                onISOChange = { viewModel.updateISO(it) },
                onFocusModeChange = { viewModel.updateFocusMode(it) },
                onWhiteBalanceChange = { viewModel.updateWhiteBalance(it) }
            )
        }
    }
}

/**
 * RGB camera preview component
 */
@Composable
private fun RGBCameraPreview(
    isActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f), // Standard camera aspect ratio
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isActive) {
                // Camera preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw camera background
                    drawRect(
                        color = Color(0xFF2E2E2E),
                        size = size
                    )

                    // Simulate camera scene
                    // Background gradient
                    drawRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = Offset(0f, height * 0.6f),
                        size = Size(width, height * 0.4f)
                    )

                    // Simulated objects
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = width * 0.1f,
                        center = Offset(width * 0.3f, height * 0.4f)
                    )

                    drawRect(
                        color = Color(0xFF5A5A5A),
                        topLeft = Offset(width * 0.6f, height * 0.2f),
                        size = Size(width * 0.25f, height * 0.4f)
                    )

                    // Grid lines (rule of thirds)
                    val strokeWidth = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.3f)

                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(width / 3f, 0f),
                        end = Offset(width / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(width * 2f / 3f, 0f),
                        end = Offset(width * 2f / 3f, height),
                        strokeWidth = strokeWidth
                    )

                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 3f),
                        end = Offset(width, height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height * 2f / 3f),
                        end = Offset(width, height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )

                    // Focus indicator (center)
                    val centerX = width / 2
                    val centerY = height / 2
                    val focusSize = 30.dp.toPx()

                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - focusSize / 2, centerY - focusSize / 2),
                        size = Size(focusSize, focusSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Overlay indicators
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    // Live indicator
                    Surface(
                        color = if (isRecording) Color.Red else Color.Green,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isRecording) "REC" else "LIVE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Resolution indicator
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resolution,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Frame rate indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${frameRate}fps",
                        color = Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                // Preview off
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Preview Off",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tap to enable preview",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real camera preview using actual RgbCameraRecorder
 */
@Composable
private fun RealCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Bind camera preview
                        cameraRecorder.bindPreview(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Recording indicator
            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "REC",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Camera status and metrics card
 */
@Composable
private fun CameraStatusCard(
    isPreviewActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Status",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = when {
                        isRecording -> Color.Red.copy(alpha = 0.2f)
                        isPreviewActive -> Color.Green.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when {
                            isRecording -> "RECORDING"
                            isPreviewActive -> "PREVIEW"
                            else -> "STANDBY"
                        },
                        color = when {
                            isRecording -> Color.Red
                            isPreviewActive -> Color.Green
                            else -> Color.Gray
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Camera metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Resolution", resolution, Color.White)
                MetricItem("Frame Rate", "${frameRate}fps", Color.Green)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Exposure", exposureTime, Color.Yellow)
                MetricItem("ISO", iso.toString(), Color.Cyan)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Focus", focusMode, MaterialTheme.colorScheme.primary)
                MetricItem("White Balance", whiteBalance, Color.Magenta)
            }
        }
    }
}

/**
 * Recording controls card
 */
@Composable
private fun RecordingControlsCard(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onTogglePreview: () -> Unit,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (isRecording) {
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        "Duration",
                        "${recordingDuration}s",
                        Color.Red
                    )
                    MetricItem(
                        "Frames",
                        capturedFrames.toString(),
                        Color.Green
                    )
                    MetricItem(
                        "File Size",
                        "${(recordingDuration * 2.5f).toInt()}MB",
                        Color.Cyan
                    )
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onTogglePreview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewActive) Orange else Green
                    )
                ) {
                    Text(if (isPreviewActive) "Stop Preview" else "Start Preview")
                }

                Button(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.VideoCall,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }

                Button(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

/**
 * Camera settings card
 */
@Composable
private fun CameraSettingsCard(
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    currentFocusMode: mpdc4gsr.feature.camera.presentation.FocusMode,
    currentWhiteBalance: mpdc4gsr.feature.camera.presentation.WhiteBalance,
    onResolutionChange: (String) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onExposureChange: (String) -> Unit,
    onISOChange: (Int) -> Unit,
    onFocusModeChange: (mpdc4gsr.feature.camera.presentation.FocusMode) -> Unit,
    onWhiteBalanceChange: (mpdc4gsr.feature.camera.presentation.WhiteBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Quick setting buttons - Resolution and Frame Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onResolutionChange("1920×1080") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1920×1080") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1080p", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = { onResolutionChange("1280×720") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1280×720") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("720p", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = { onFrameRateChange(if (frameRate == 30) 60 else 30) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${frameRate}fps", fontSize = 10.sp)
                }
            }

            // Additional camera controls - Focus and White Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { 
                        onFocusModeChange(currentFocusMode.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Focus: $focusMode", fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = { 
                        onWhiteBalanceChange(currentWhiteBalance.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WB: $whiteBalance", fontSize = 9.sp)
                }
            }

            // Advanced settings info
            Text(
                text = "Advanced exposure and ISO controls available in camera settings menu",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Metric display item
 */
@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraScreenPreview() {
    IRCameraTheme {
        RGBCameraScreen()
    }
}