package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import mpdc4gsr.core.ui.theme.Green
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.Orange
import mpdc4gsr.core.ui.theme.Purple
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModel

@Composable
fun RGBCameraScreen(
    viewModel: RGBCameraViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraRecorder by viewModel.cameraRecorder.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    // Initialize camera on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(lifecycleOwner)
    }
    // Show error if present
    LaunchedEffect(cameraState.error) {
        showError = cameraState.error != null
    }
    // Use real data from ViewModel
    val isPreviewActive = cameraState.isPreviewActive
    val isRecording = cameraState.isRecording
    val resolution = cameraState.resolution
    val frameRate = cameraState.frameRate
    val recordingDuration = cameraState.recordingDuration
    val capturedFrames = cameraState.capturedFrames
    val cameraChangeCounter = cameraState.cameraChangeCounter
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // Full-screen camera preview - now properly reactive to cameraRecorder StateFlow
        if (cameraRecorder != null) {
            FullScreenCameraPreview(
                cameraRecorder = cameraRecorder!!,
                isRecording = isRecording,
                cameraChangeCounter = cameraChangeCounter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FullScreenCameraPreviewSimulated(
                isActive = isPreviewActive,
                isRecording = isRecording,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Top overlay with back button and status
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            CameraTopBar(
                resolution = resolution,
                frameRate = frameRate,
                isRecording = isRecording,
                lensFacing = cameraState.lensFacingDescription,
                hardwareLevel = cameraState.hardwareLevelDescription,
                supports4K = cameraState.supports4K,
                supportsRaw = cameraState.supportsRaw,
                supports60Fps = cameraState.supports60Fps,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick,
            )
        }
        // Bottom overlay with camera controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CameraBottomControls(
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
                onCapturePhoto = {
                    viewModel.capturePhoto()
                    onCapturePhoto()
                },
                onSwitchCamera = {
                    viewModel.switchCamera()
                },
            )
        }
        // Toggle controls visibility with tap
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showControls = !showControls
                    },
        )
        // Error message display with retry option
        if (showError && cameraState.error != null) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Camera Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cameraState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.dismissError() },
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                viewModel.dismissError()
                                viewModel.reinitializeCamera(lifecycleOwner)
                            },
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTopBar(
    resolution: String,
    frameRate: Int,
    isRecording: Boolean,
    lensFacing: String,
    hardwareLevel: String,
    supports4K: Boolean,
    supportsRaw: Boolean,
    supports60Fps: Boolean,
    onBackClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capabilityBadges =
        remember(
            lensFacing,
            hardwareLevel,
            supports4K,
            supportsRaw,
            supports60Fps,
        ) {
            buildList {
                add(
                    CapabilityBadge(
                        label = lensFacing,
                        background = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                    ),
                )
                val hardwareKnown = !hardwareLevel.equals("Level Unknown", ignoreCase = true)
                if (hardwareKnown) {
                    add(
                        CapabilityBadge(
                            label = hardwareLevel,
                            background = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White,
                        ),
                    )
                }
                if (supports4K) {
                    add(CapabilityBadge(label = "4K", background = Orange, contentColor = Color.Black))
                }
                if (supports60Fps) {
                    add(CapabilityBadge(label = "60 FPS", background = Green, contentColor = Color.Black))
                }
                if (supportsRaw) {
                    add(CapabilityBadge(label = "RAW", background = Purple))
                }
            }
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isRecording) {
                        Surface(
                            color = Color.Red,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                )
                                Text(
                                    text = "REC",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "$resolution · ${frameRate}fps",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                if (capabilityBadges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        capabilityBadges.forEach { badge ->
                            CapabilityChip(badge)
                        }
                    }
                }
            }
            IconButton(
                onClick = onSettingsClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                )
            }
        }
    }
}

private data class CapabilityBadge(
    val label: String,
    val background: Color,
    val contentColor: Color = Color.White,
)

@Composable
private fun CapabilityChip(
    badge: CapabilityBadge,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = badge.background,
        contentColor = badge.contentColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier,
    ) {
        Text(
            text = badge.label,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CameraBottomControls(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onCapturePhoto: () -> Unit,
    onSwitchCamera: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isRecording) {
                Text(
                    text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Photo capture button
                FilledIconButton(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Photo",
                        tint = Color.White,
                    )
                }
                // Video record button - larger, centered
                FilledIconButton(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    modifier = Modifier.size(72.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) Color.Red else Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f),
                        ),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else Color.Red,
                        modifier = Modifier.size(36.dp),
                    )
                }
                // Camera switch button
                FilledIconButton(
                    onClick = onSwitchCamera,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    cameraChangeCounter: Int,
    modifier: Modifier = Modifier,
) {
    // Use key to force recreation when camera switches
    key(cameraChangeCounter) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                // Bind preview when the view updates - ensures preview is connected
                cameraRecorder.bindPreview(previewView)
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun FullScreenCameraPreviewSimulated(
    isActive: Boolean,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawRect(color = Color(0xFF2E2E2E), size = size)
                drawRect(
                    color = Color(0xFF4A4A4A),
                    topLeft = Offset(0f, height * 0.6f),
                    size = Size(width, height * 0.4f),
                )
                drawCircle(
                    color = Color(0xFF6A6A6A),
                    radius = width * 0.1f,
                    center = Offset(width * 0.3f, height * 0.4f),
                )
                drawRect(
                    color = Color(0xFF5A5A5A),
                    topLeft = Offset(width * 0.6f, height * 0.2f),
                    size = Size(width * 0.25f, height * 0.4f),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Off",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "Camera Preview Off",
                    color = Color.Gray,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraScreenPreview() {
    IRCameraTheme {
        RGBCameraScreen()
    }
}
