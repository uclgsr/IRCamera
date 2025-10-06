package mpdc4gsr.feature.thermal.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.thermal.presentation.ThermalCameraViewModel
import mpdc4gsr.feature.thermal.presentation.ThermalCameraViewModelFactory
private const val CAMERA_RESCAN_DELAY_MS = 500L

@Composable
fun ThermalMonitorScreen(
    viewModel: ThermalCameraViewModel = viewModel(
        factory = ThermalCameraViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    ),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onRecordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showAdvancedControls by remember { mutableStateOf(false) }
    // Trigger immediate rescan when screen appears to catch already-connected devices
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(CAMERA_RESCAN_DELAY_MS)
        viewModel.rescanForThermalCamera()
    }
    // Update recording duration periodically
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            while (uiState.isRecording) {
                kotlinx.coroutines.delay(1000)
                viewModel.updateRecordingDuration()
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Full-screen thermal camera preview with actual bitmap from ThermalCameraRecorder
        ThermalCameraPreview(
            bitmap = uiState.previewBitmap,
            modifier = Modifier.fillMaxSize()
        )
        // Temperature overlay always visible on preview
        TemperatureOverlay(
            currentTemp = uiState.currentTemperature ?: uiState.centerTemperature,
            maxTemp = uiState.maxTemperature,
            minTemp = uiState.minTemperature,
            avgTemp = uiState.avgTemperature,
            modifier = Modifier.fillMaxSize()
        )
        // Top overlay with back button and status
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ThermalTopBar(
                isConnected = uiState.isConnected,
                isRecording = uiState.isRecording,
                isSimulationMode = uiState.isSimulationMode,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
        }
        // Bottom overlay with recording controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ThermalBottomControls(
                isRecording = uiState.isRecording,
                isConnected = uiState.isConnected,
                recordingDuration = uiState.recordingDuration,
                onRecordClick = {
                    onRecordClick()
                },
                onAdvancedClick = { showAdvancedControls = !showAdvancedControls }
            )
        }
        // Toggle controls visibility with tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        )
        // Advanced controls overlay
        if (showAdvancedControls) {
            AdvancedControlsPanel(
                onDismiss = { showAdvancedControls = false }
            )
        }
    }
}

@Composable
private fun ThermalTopBar(
    isConnected: Boolean,
    isRecording: Boolean,
    isSimulationMode: Boolean = false,
    onBackClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "REC",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when {
                            !isConnected -> "Disconnected"
                            isSimulationMode -> "Simulation"
                            else -> "Connected"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ThermalBottomControls(
    isRecording: Boolean,
    isConnected: Boolean,
    recordingDuration: Long = 0L,
    onRecordClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Advanced settings button
                FilledTonalButton(
                    onClick = onAdvancedClick,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Advanced",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
                // Record button - larger, centered
                FilledIconButton(
                    onClick = onRecordClick,
                    enabled = isConnected,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color.Red else Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else Color.Red,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Spacer for symmetry with settings button width
                Spacer(modifier = Modifier.width(120.dp))
            }
        }
    }
}

@Composable
private fun ThermalCameraPreview(
    bitmap: android.graphics.Bitmap?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Display actual thermal bitmap from camera
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Thermal Camera Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            // Placeholder when no bitmap available
            Text(
                text = "Waiting for thermal camera...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun TemperatureOverlay(
    currentTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    avgTemp: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Current temperature display (center)
        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.7f),
            shape = CircleShape
        ) {
            Text(
                text = "${currentTemp}°C",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
        }
        // Max temperature (top-right)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            color = Color.Red.copy(alpha = 0.8f),
            shape = CircleShape
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "${maxTemp}°C",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // Min temperature (bottom-left)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            shape = CircleShape
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "${minTemp}°C",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusIndicator(
            label = "Camera",
            isActive = isConnected,
            color = if (isConnected) Color.Green else Color.Gray
        )
        StatusIndicator(
            label = "Recording",
            isActive = false, // Will be connected to actual recording state
            color = Color.Red
        )
        StatusIndicator(
            label = "Storage",
            isActive = true,
            color = Color.Green
        )
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isActive) color else Color.Gray)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ControlPanel(
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Record button
        FloatingActionButton(
            onClick = onRecordClick,
            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.VideoCall,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = Color.White
            )
        }
        // Advanced controls button
        Button(
            onClick = onAdvancedClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Text(
                text = "Advanced",
                color = Color.White
            )
        }
    }
}

@Composable
private fun AdvancedControlsPanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Advanced Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Sample controls - will be replaced with actual thermal camera controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Focus Lock", color = Color.White)
                Switch(
                    checked = false,
                    onCheckedChange = { }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto Exposure", color = Color.White)
                Switch(
                    checked = true,
                    onCheckedChange = { }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun ThermalMonitorScreenPreview() {
    IRCameraTheme {
        ThermalMonitorScreen()
    }
}