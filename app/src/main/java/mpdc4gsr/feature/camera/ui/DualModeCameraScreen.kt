package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mpdc4gsr.feature.camera.presentation.FocusMode
import mpdc4gsr.feature.camera.presentation.WhiteBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualModeCameraScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Thermal + RGB Camera",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Tune, contentDescription = "Camera Settings")
                        }
                        var viewMode by remember { mutableStateOf("split") }
                        IconButton(onClick = {
                            viewMode = if (viewMode == "split") "overlay" else "split"
                            // TODO: Toggle between split and overlay view modes
                        }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap View")
                        }
                    }
                )
            }
        ) { paddingValues ->
            DualModeCameraContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CameraMode.DUAL_VIEW) }
    var rgbCameraActive by remember { mutableStateOf(true) }
    var thermalCameraActive by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var syncEnabled by remember { mutableStateOf(true) }
    var resolution by remember { mutableStateOf("1920A-1080") }
    var frameRate by remember { mutableStateOf(30) }
    var exposureTime by remember { mutableStateOf("1/60") }
    var iso by remember { mutableStateOf(200) }
    var focusMode by remember { mutableStateOf(FocusMode.AUTO) }
    var whiteBalance by remember { mutableStateOf(WhiteBalance.AUTO) }
    var recordingDuration by remember { mutableStateOf(0) }
    var capturedFrames by remember { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(frameRate) {
        exposureTime = if (frameRate > 30) "1/120" else "1/60"
        iso = if (frameRate > 30) 400 else 200
    }

    LaunchedEffect(isRecording, frameRate) {
        if (!isRecording) {
            recordingDuration = 0
            capturedFrames = 0
            return@LaunchedEffect
        }
        recordingDuration = 0
        capturedFrames = 0
        while (isActive) {
            delay(1000)
            recordingDuration += 1
            capturedFrames += frameRate
        }
    }

    val combinedResolution = when (selectedMode) {
        CameraMode.RGB_ONLY -> "RGB $resolution"
        CameraMode.THERMAL_ONLY -> "Thermal 384x288"
        CameraMode.DUAL_VIEW, CameraMode.OVERLAY -> "RGB $resolution / Thermal 384x288"
    }
    val isPreviewActive = rgbCameraActive || thermalCameraActive

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Mode Selector
        CameraModeSelector(
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it }
        )
        // Dual Camera Preview
        DualCameraPreviewCard(
            mode = selectedMode,
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            syncEnabled = syncEnabled
        )
        // Combined status card to visualize metrics from both cameras
        CameraStatusCard(
            isPreviewActive = isPreviewActive,
            isRecording = isRecording,
            resolution = combinedResolution,
            frameRate = frameRate,
            exposureTime = exposureTime,
            iso = iso,
            focusMode = focusMode.displayName,
            whiteBalance = whiteBalance.displayName
        )
        // Camera Status and Controls
        CameraControlsCard(
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            syncEnabled = syncEnabled,
            onRGBToggle = { rgbCameraActive = it },
            onThermalToggle = { thermalCameraActive = it },
            onSyncToggle = { syncEnabled = it }
        )
        RecordingControlsCard(
            isRecording = isRecording,
            isPreviewActive = isPreviewActive,
            recordingDuration = recordingDuration,
            capturedFrames = capturedFrames,
            onToggleRecording = {
                if (isPreviewActive) {
                    isRecording = !isRecording
                }
            },
            onTogglePreview = {
                if (isPreviewActive) {
                    isRecording = false
                    rgbCameraActive = false
                    thermalCameraActive = false
                } else {
                    rgbCameraActive = true
                    thermalCameraActive = true
                }
            },
            onCapturePhoto = {
                android.widget.Toast.makeText(
                    context,
                    "Snapshot captured",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
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
            onFrameRateChange = { frameRate = it },
            onFocusModeChange = { focusMode = it },
            onWhiteBalanceChange = { whiteBalance = it }
        )
        // Calibration Tools
        CalibrationToolsCard()
    }
}

enum class CameraMode {
    RGB_ONLY,
    THERMAL_ONLY,
    DUAL_VIEW,
    OVERLAY
}

@Composable
private fun CameraModeSelector(
    selectedMode: CameraMode,
    onModeChange: (CameraMode) -> Unit
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
                "Camera Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.RGB_ONLY,
                    label = "RGB Only",
                    selected = selectedMode == CameraMode.RGB_ONLY,
                    onClick = { onModeChange(CameraMode.RGB_ONLY) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.THERMAL_ONLY,
                    label = "Thermal Only",
                    selected = selectedMode == CameraMode.THERMAL_ONLY,
                    onClick = { onModeChange(CameraMode.THERMAL_ONLY) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.DUAL_VIEW,
                    label = "Dual View",
                    selected = selectedMode == CameraMode.DUAL_VIEW,
                    onClick = { onModeChange(CameraMode.DUAL_VIEW) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.OVERLAY,
                    label = "Overlay",
                    selected = selectedMode == CameraMode.OVERLAY,
                    onClick = { onModeChange(CameraMode.OVERLAY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    mode: CameraMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        },
        selected = selected,
        modifier = modifier
    )
}

@Composable
private fun DualCameraPreviewCard(
    mode: CameraMode,
    rgbActive: Boolean,
    thermalActive: Boolean,
    syncEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    "Camera Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (syncEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Synced",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Synced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Preview area based on mode
            when (mode) {
                CameraMode.RGB_ONLY -> {
                    RGBPreviewArea(active = rgbActive)
                }

                CameraMode.THERMAL_ONLY -> {
                    ThermalPreviewArea(active = thermalActive)
                }

                CameraMode.DUAL_VIEW -> {
                    DualViewPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }

                CameraMode.OVERLAY -> {
                    OverlayPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }
            }
        }
    }
}

@Composable
private fun RGBPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) Color.Black else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "RGB Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThermalPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "25.6°C - 31.2°C",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.DeviceThermostat,
                    contentDescription = "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DualViewPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (rgbActive) Color.Black else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (rgbActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (rgbActive) "RGB Camera Active" else "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "RGB",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (thermalActive) MaterialTheme.colorScheme.primary else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (thermalActive) Icons.Default.Thermostat else Icons.Default.DeviceThermostat,
                    contentDescription = if (thermalActive) "Thermal Camera Active" else "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Thermal",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OverlayPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color.Black,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rgbActive && thermalActive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "RGB and Thermal Overlay",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB + Thermal Overlay",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Opacity: 60%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                "Both cameras required for overlay",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CameraControlsCard(
    rgbActive: Boolean,
    thermalActive: Boolean,
    syncEnabled: Boolean,
    onRGBToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onSyncToggle: (Boolean) -> Unit
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
                "Camera Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Camera toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "RGB Camera")
                    Text("RGB Camera")
                }
                Switch(
                    checked = rgbActive,
                    onCheckedChange = onRGBToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Thermostat, contentDescription = "Thermal Camera")
                    Text("Thermal Camera")
                }
                Switch(
                    checked = thermalActive,
                    onCheckedChange = onThermalToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync Cameras")
                    Text("Sync Cameras")
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncToggle
                )
            }
        }
    }
}

@Composable
private fun CalibrationToolsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Calibration Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Start camera alignment process
                        android.widget.Toast.makeText(
                            context,
                            "Starting alignment...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Align Cameras")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Align")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Start color calibration
                        android.widget.Toast.makeText(
                            context,
                            "Starting color calibration...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Calibrate Colors")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
            }
        }
    }
}
