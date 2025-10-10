package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.deferAction
import mpdc4gsr.feature.camera.presentation.FocusMode
import mpdc4gsr.feature.camera.presentation.WhiteBalance
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Camera Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = deferAction { onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = deferAction { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            CameraDashboardContent(
                onNavigateToSingleCamera = onNavigateToSingleCamera,
                onNavigateToTimeLapse = onNavigateToTimeLapse,
                onNavigateToGallery = onNavigateToGallery,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CameraDashboardContent(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showToast: (String) -> Unit = { message ->
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    var isPreviewActive by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var resolution by remember { mutableStateOf("1920A-1080") }
    var frameRate by remember { mutableStateOf(30) }
    var exposureTime by remember { mutableStateOf("1/60") }
    var iso by remember { mutableStateOf(200) }
    var focusMode by remember { mutableStateOf(FocusMode.AUTO) }
    var whiteBalance by remember { mutableStateOf(WhiteBalance.AUTO) }
    var recordingDuration by remember { mutableStateOf(0) }
    var capturedFrames by remember { mutableStateOf(0) }
    LaunchedEffect(frameRate) {
        exposureTime = if (frameRate > 30) "1/120" else "1/60"
        iso = if (frameRate > 30) 400 else 200
    }
    LaunchedEffect(isRecording, frameRate) {
        if (!isRecording) {
            return@LaunchedEffect
        }
        while (isActive && isRecording) {
            delay(1000)
            recordingDuration += 1
            capturedFrames += frameRate
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CameraStatusCard(
            isPreviewActive = isPreviewActive,
            isRecording = isRecording,
            resolution = resolution,
            frameRate = frameRate,
            exposureTime = exposureTime,
            iso = iso,
            focusMode = focusMode.displayName,
            whiteBalance = whiteBalance.displayName
        )
        // Camera Modes Card
        CameraModesCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToTimeLapse = onNavigateToTimeLapse,
            showToast = showToast
        )
        RecordingControlsCard(
            isRecording = isRecording,
            isPreviewActive = isPreviewActive,
            recordingDuration = recordingDuration,
            capturedFrames = capturedFrames,
            onToggleRecording = {
                if (isPreviewActive) {
                    isRecording = !isRecording
                    if (isRecording) {
                        recordingDuration = 0
                        capturedFrames = 0
                    }
                } else {
                    onNavigateToSingleCamera?.invoke()
                }
            },
            onTogglePreview = {
                isPreviewActive = !isPreviewActive
                if (!isPreviewActive) {
                    isRecording = false
                }
            },
            onCapturePhoto = {
                onNavigateToSingleCamera?.invoke() ?: showToast("Launching RGB capture")
            }
        )
        CameraSettingsCard(
            resolution = resolution,
            frameRate = frameRate,
            focusMode = focusMode.displayName,
            whiteBalance = whiteBalance.displayName,
            currentFocusMode = focusMode,
            currentWhiteBalance = whiteBalance,
            onResolutionChange = { resolution = it },
            onFrameRateChange = {
                frameRate = it
                exposureTime = if (it > 30) "1/120" else "1/60"
            },
            onFocusModeChange = { focusMode = it },
            onWhiteBalanceChange = { whiteBalance = it }
        )
        // Preview and Gallery Card
        PreviewGalleryCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToGallery = onNavigateToGallery,
            showToast = showToast
        )
    }
}

@Composable
private fun CameraModesCard(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Modes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Single Camera Mode
            CameraModeItem(
                title = "Single Camera Mode",
                description = "Standard RGB camera capture",
                icon = Icons.Default.Camera,
                isActive = false,
                onClick = {
                    onNavigateToSingleCamera?.invoke() ?: showToast("Single camera mode coming soon")
                }
            )
            // Time-lapse Mode
            CameraModeItem(
                title = "Time-lapse Mode",
                description = "Automated interval capture",
                icon = Icons.Default.Timer,
                isActive = false,
                onClick = {
                    onNavigateToTimeLapse?.invoke() ?: showToast("Time-lapse mode coming soon")
                }
            )
        }
    }
}

@Composable
private fun CameraModeItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = deferAction { onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Badge {
                    Text("Active")
                }
            }
        }
    }
}

@Composable
private fun PreviewGalleryCard(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preview & Gallery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke() ?: showToast("Preview feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = "Preview Camera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToGallery?.invoke() ?: showToast("Gallery feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Open Gallery")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }
            }
        }
    }
}
