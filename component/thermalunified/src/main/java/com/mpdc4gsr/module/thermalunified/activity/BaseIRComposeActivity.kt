package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class BaseIRComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var connectionStatus by remember { mutableStateOf("Disconnected") }
        var cameraReady by remember { mutableStateOf(false) }
        var thermalMode by remember { mutableIntStateOf(1) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Base Control",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                containerColor = Color.Black
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Status bar
                    ThermalStatusBar(
                        connectionStatus = connectionStatus,
                        cameraReady = cameraReady,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Main thermal view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Thermal camera surface
                        ThermalCameraSurface(
                            modifier = Modifier.fillMaxSize()
                        )

                        // Control overlay
                        ThermalControlOverlay(
                            thermalMode = thermalMode,
                            onModeChange = { thermalMode = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Initialize camera connection
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000L)
            connectionStatus = "Connected"
            cameraReady = true
        }
    }
}

@Composable
private fun ThermalStatusBar(
    connectionStatus: String,
    cameraReady: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (cameraReady) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = "Status",
                    tint = if (cameraReady) Color(0xFF00FF00) else Color(0xFFFF4444),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    connectionStatus,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Camera info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusItem("Resolution", "384x288")
                StatusItem("FPS", "9")
                StatusItem("Mode", "IR")
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ThermalCameraSurface(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera surface
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Camera",
                    tint = Color(0xFF7D8590),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Thermal Camera Feed",
                    color = Color(0xFF7D8590),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Base IR Camera Control",
                    color = Color(0xFFFF6B35),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ThermalControlOverlay(
    thermalMode: Int,
    onModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode selector
            ThermalModeSelector(
                selectedMode = thermalMode,
                onModeSelected = onModeChange
            )

            // Quick actions
            QuickActionButtons()
        }
    }
}

@Composable
private fun ThermalModeSelector(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    val modes = getThermalModes()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Thermal Mode",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEachIndexed { index, mode ->
                FilterChip(
                    onClick = { onModeSelected(index + 1) },
                    label = { Text(mode, fontSize = 12.sp) },
                    selected = selectedMode == index + 1,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF21262D),
                        labelColor = Color(0xFF7D8590)
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickActionButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.CameraAlt,
            text = "Capture",
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Videocam,
            text = "Record",
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Palette,
            text = "Palette",
            onClick = { },
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            icon = Icons.Default.Tune,
            text = "Adjust",
            onClick = { },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                fontSize = 10.sp
            )
        }
    }
}

private fun getThermalModes(): List<String> {
    return listOf("Standard", "High Gain", "Low Gain", "Manual")
}