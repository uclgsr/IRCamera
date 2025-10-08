package mpdc4gsr.presentation.screens.camera

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.deferAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToDualMode: () -> Unit,
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
                onNavigateToDualMode = onNavigateToDualMode,
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
    onNavigateToDualMode: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showToast: (String) -> Unit = { message ->
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Status Card
        CameraStatusCard()
        // Camera Modes Card
        CameraModesCard(
            onNavigateToDualMode = onNavigateToDualMode,
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToTimeLapse = onNavigateToTimeLapse,
            showToast = showToast
        )
        // Recording Controls Card
        RecordingControlsCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera
        )
        // Camera Settings Card
        CameraSettingsCard()
        // Preview and Gallery Card
        PreviewGalleryCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToGallery = onNavigateToGallery,
            showToast = showToast
        )
    }
}

@Composable
private fun CameraStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Camera Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Camera Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            // Camera availability indicators
            CameraStatusRow("Front Camera", true)
            CameraStatusRow("Back Camera", true)
            CameraStatusRow("External Camera", false)
            // Current camera info
            CameraInfoRow("Active Camera", "Back Camera")
            CameraInfoRow("Resolution", "1920x1080")
            CameraInfoRow("Frame Rate", "30 FPS")
            CameraInfoRow("Focus Mode", "Auto")
        }
    }
}

@Composable
private fun CameraStatusRow(
    cameraName: String,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cameraName,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isAvailable) "Camera Available" else "Camera Unavailable",
                tint = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isAvailable) "Available" else "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CameraInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraModesCard(
    onNavigateToDualMode: () -> Unit,
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
            // Dual Camera Mode
            CameraModeItem(
                title = "Dual Camera Mode",
                description = "Simultaneous RGB and thermal capture",
                icon = Icons.Default.CameraAlt,
                isActive = true,
                onClick = onNavigateToDualMode
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
private fun RecordingControlsCard(
    onNavigateToSingleCamera: (() -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Recording status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recording Status",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                        contentDescription = if (isRecording) "Recording" else "Stopped",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (isRecording) "Recording" else "Stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { isRecording = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { isRecording = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Quick Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Flash setting
            SettingRow(
                title = "Flash",
                value = "Auto",
                icon = Icons.Default.FlashOn
            )
            // Quality setting
            SettingRow(
                title = "Video Quality",
                value = "1080p",
                icon = Icons.Default.HighQuality
            )
            // Storage location
            SettingRow(
                title = "Storage",
                value = "Internal",
                icon = Icons.Default.Storage
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
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