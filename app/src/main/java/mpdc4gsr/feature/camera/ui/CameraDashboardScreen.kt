package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mpdc4gsr.core.ui.deferAction
import mpdc4gsr.feature.camera.presentation.FocusMode
import mpdc4gsr.feature.camera.presentation.WhiteBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Camera Dashboard",
                            fontWeight = FontWeight.Bold,
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
                    },
                )
            },
        ) { paddingValues ->
            CameraDashboardContent(
                onNavigateToSingleCamera = onNavigateToSingleCamera,
                onNavigateToTimeLapse = onNavigateToTimeLapse,
                onNavigateToGallery = onNavigateToGallery,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun CameraDashboardContent(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showToast: (String) -> Unit = { message ->
        android.widget.Toast
            .makeText(
                context,
                message,
                android.widget.Toast.LENGTH_SHORT,
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
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardStatusCard(
            isPreviewActive = isPreviewActive,
            isRecording = isRecording,
            resolution = resolution,
            frameRate = frameRate,
            exposureTime = exposureTime,
            iso = iso,
            focusMode = focusMode.displayName,
            whiteBalance = whiteBalance.displayName,
        )
        // Camera Modes Card
        CameraModesCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToTimeLapse = onNavigateToTimeLapse,
            showToast = showToast,
        )
        DashboardRecordingControlsCard(
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
            },
        )
        DashboardSettingsCard(
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
            onWhiteBalanceChange = { whiteBalance = it },
        )
        // Preview and Gallery Card
        PreviewGalleryCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToGallery = onNavigateToGallery,
            showToast = showToast,
        )
    }
}

@Composable
private fun DashboardStatusCard(
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
    val statusLabel =
        when {
            isRecording -> "Recording"
            isPreviewActive -> "Preview"
            else -> "Standby"
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    "Camera Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AssistChip(
                    onClick = {},
                    label = { Text(statusLabel) },
                    leadingIcon = {
                        val icon =
                            when {
                                isRecording -> Icons.Default.FiberManualRecord
                                isPreviewActive -> Icons.Default.Visibility
                                else -> Icons.Default.PowerSettingsNew
                            }
                        Icon(icon, contentDescription = null)
                    },
                    enabled = false,
                )
            }
            StatusMetricRow(
                "Resolution" to resolution,
                "Frame Rate" to "${frameRate}fps",
            )
            StatusMetricRow(
                "Exposure" to exposureTime,
                "ISO" to "$iso",
            )
            StatusMetricRow(
                "Focus" to focusMode,
                "White Balance" to whiteBalance,
            )
        }
    }
}

@Composable
private fun StatusMetricRow(
    left: Pair<String, String>,
    right: Pair<String, String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MetricColumn(title = left.first, value = left.second)
        MetricColumn(title = right.first, value = right.second)
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashboardRecordingControlsCard(
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Recording Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Duration: ${recordingDuration}s • Frames: $capturedFrames",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = deferAction { onTogglePreview() },
                    modifier = Modifier.weight(1f),
                ) {
                    val label = if (isPreviewActive) "Stop Preview" else "Start Preview"
                    Icon(Icons.Default.Visibility, contentDescription = label)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label)
                }
                Button(
                    onClick = deferAction { onToggleRecording() },
                    enabled = isPreviewActive,
                    modifier = Modifier.weight(1f),
                    colors =
                        if (isRecording) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
            OutlinedButton(
                onClick = deferAction { onCapturePhoto() },
                enabled = isPreviewActive && !isRecording,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture Photo")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capture Photo")
            }
        }
    }
}

@Composable
private fun DashboardSettingsCard(
    resolution: String,
    frameRate: Int,
    focusMode: String,
    whiteBalance: String,
    currentFocusMode: FocusMode,
    currentWhiteBalance: WhiteBalance,
    onResolutionChange: (String) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onFocusModeChange: (FocusMode) -> Unit,
    onWhiteBalanceChange: (WhiteBalance) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Quick Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SettingsRow(
                title = "Resolution",
                current = resolution,
                options = listOf("1920A-1080", "1280A-720"),
                onOptionSelected = onResolutionChange,
            )
            SettingsRow(
                title = "Frame Rate",
                current = "${frameRate}fps",
                options = listOf(30, 60).map { "${it}fps" },
                onOptionSelected = { option ->
                    val numeric = option.removeSuffix("fps").toIntOrNull() ?: frameRate
                    onFrameRateChange(numeric)
                },
            )
            val focusModeOptions = FocusMode.values()
            SettingsRow(
                title = "Focus Mode",
                current = focusMode,
                options = focusModeOptions.map { it.displayName },
                onOptionSelected = { selected ->
                    focusModeOptions.firstOrNull { it.displayName == selected }?.let(onFocusModeChange)
                },
                highlighted = currentFocusMode.displayName,
            )
            val whiteBalanceOptions = WhiteBalance.values()
            SettingsRow(
                title = "White Balance",
                current = whiteBalance,
                options = whiteBalanceOptions.map { it.displayName },
                onOptionSelected = { selected ->
                    whiteBalanceOptions.firstOrNull { it.displayName == selected }?.let(onWhiteBalanceChange)
                },
                highlighted = currentWhiteBalance.displayName,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    current: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    highlighted: String = current,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == highlighted,
                    onClick = deferAction { onOptionSelected(option) },
                    label = { Text(option) },
                )
            }
        }
        Text(
            "Current: $current",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CameraModesCard(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    showToast: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Camera Modes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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
                },
            )
            // Time-lapse Mode
            CameraModeItem(
                title = "Time-lapse Mode",
                description = "Automated interval capture",
                icon = Icons.Default.Timer,
                isActive = false,
                onClick = {
                    onNavigateToTimeLapse?.invoke() ?: showToast("Time-lapse mode coming soon")
                },
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
    onClick: () -> Unit,
) {
    Card(
        onClick = deferAction { onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors =
            if (isActive) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    showToast: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Preview & Gallery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke() ?: showToast("Preview feature coming soon")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Preview, contentDescription = "Preview Camera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToGallery?.invoke() ?: showToast("Gallery feature coming soon")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Open Gallery")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }
            }
        }
    }
}
