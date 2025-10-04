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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

/**
 * Dual Mode Camera Screen - Advanced Camera Integration
 *
 * Modern implementation of dual-mode camera functionality:
 * - Simultaneous RGB and thermal camera operation
 * - Real-time preview synchronization
 * - Advanced recording controls
 * - Camera calibration and alignment tools
 */
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
                        IconButton(onClick = { /* TODO: Implement toggle view mode
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
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

        // Camera Status and Controls
        CameraControlsCard(
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            isRecording = isRecording,
            syncEnabled = syncEnabled,
            onRGBToggle = { rgbCameraActive = it },
            onThermalToggle = { thermalCameraActive = it },
            onRecordingToggle = { isRecording = it },
            onSyncToggle = { syncEnabled = it }
        )

        // Recording Settings
        RecordingSettingsCard()

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
                            contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
                    contentDescription = null,
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
    isRecording: Boolean,
    syncEnabled: Boolean,
    onRGBToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRecordingToggle: (Boolean) -> Unit,
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
                    Icon(Icons.Default.Videocam, contentDescription = null)
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
                    Icon(Icons.Default.Thermostat, contentDescription = null)
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
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Text("Sync Cameras")
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncToggle
                )
            }

            HorizontalDivider()

            // Recording controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { onRecordingToggle(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = { onRecordingToggle(true) },
                        modifier = Modifier.weight(1f),
                        enabled = rgbActive || thermalActive
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Recording")
                    }
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement snapshot capture
                     *   - Capture current frame/view state
                     *   - Save snapshot to file system
                     *   - Show save confirmation with location
                     */ },
                    modifier = Modifier.weight(1f),
                    enabled = rgbActive || thermalActive
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snapshot")
                }
            }
        }
    }
}

@Composable
private fun RecordingSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Quality settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RGB Quality")
                Text("1080p @ 30fps", fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thermal Quality")
                Text("384x288 @ 25fps", fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Synchronization")
                Text("Hardware Sync", fontWeight = FontWeight.Medium)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement start alignment
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Align")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement calibrate colors
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
            }
        }
    }
}