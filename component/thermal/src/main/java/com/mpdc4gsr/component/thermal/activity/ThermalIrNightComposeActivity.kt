package com.mpdc4gsr.component.thermal.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.viewmodel.ThermalIrNightViewModel

class ThermalIrNightComposeActivity : BaseComposeActivity<ThermalIrNightViewModel>() {
    override fun createViewModel(): ThermalIrNightViewModel = viewModels<ThermalIrNightViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalIrNightViewModel) {
        val selectedMode by viewModel.selectedMode.collectAsState()
        val nightModeEnabled by viewModel.nightModeEnabled.collectAsState()
        val showOverlay by viewModel.showOverlay.collectAsState()
        val isRecording by viewModel.isRecording.collectAsState()
        var showInfoDialog by remember { mutableStateOf(false) }
        var showPaletteDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showRangeEditDialog by remember { mutableStateOf(false) }
        var showMoreOptions by remember { mutableStateOf(false) }
        var rangeLocked by remember { mutableStateOf(false) }
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectMode(0) },
                                    colors =
                                        ButtonDefaults.textButtonColors(
                                            contentColor =
                                                if (selectedMode == 0) {
                                                    Color.White
                                                } else {
                                                    Color.White.copy(
                                                        alpha = 0.6f,
                                                    )
                                                },
                                        ),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Temperature", fontSize = 16.sp)
                                        if (selectedMode == 0) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(4.dp)
                                                        .background(MaterialTheme.colorScheme.primary),
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.selectMode(1) },
                                    colors =
                                        ButtonDefaults.textButtonColors(
                                            contentColor =
                                                if (selectedMode == 1) {
                                                    Color.White
                                                } else {
                                                    Color.White.copy(
                                                        alpha = 0.6f,
                                                    )
                                                },
                                        ),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Observe", fontSize = 16.sp)
                                        if (selectedMode == 1) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(4.dp)
                                                        .background(MaterialTheme.colorScheme.primary),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.toggleNightMode() }) {
                                Icon(
                                    if (nightModeEnabled) Icons.Default.Brightness3 else Icons.Default.Brightness7,
                                    contentDescription = "Night Mode",
                                    tint = if (nightModeEnabled) Color.Yellow else Color.White,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF16131E),
                            ),
                    )
                },
                containerColor = Color(0xFF16131E),
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                ) {
                    // Main thermal display with night mode styling
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .aspectRatio(192f / 256f)
                                .background(Color.Black),
                    ) {
                        // Thermal camera view
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (nightModeEnabled) Icons.Default.Brightness3 else Icons.Default.Brightness7,
                                    contentDescription = if (nightModeEnabled) "Night Mode" else "Day Mode",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White.copy(alpha = 0.3f),
                                )
                                Text(
                                    if (nightModeEnabled) "Night Mode Active" else "Day Mode",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                        // Night mode specific overlays
                        if (nightModeEnabled && showOverlay) {
                            Column(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp),
                            ) {
                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = Color(0x80000000),
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.NightsStay,
                                            contentDescription = "Night Mode",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Yellow,
                                        )
                                        Text(
                                            "Night Mode",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                            }
                        }
                        // Temperature controls
                        Column(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                        ) {
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = Color(0x80000000),
                                    ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    IconButton(onClick = { rangeLocked = !rangeLocked }) {
                                        Icon(
                                            if (rangeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = if (rangeLocked) "Unlock" else "Lock",
                                            tint = if (rangeLocked) Color.Yellow else Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    IconButton(onClick = { showRangeEditDialog = true }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }
                        // Recording status
                        if (isRecording) {
                            Card(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = Color(0xCC000000),
                                    ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(8.dp)
                                                .background(Color.Red, androidx.compose.foundation.shape.CircleShape),
                                    )
                                    Text("Recording", color = Color.White, fontSize = 14.sp)
                                    Text("00:00", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    // Bottom menu optimized for night mode
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF16131E),
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                        ) {
                            // Secondary menu row
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                IconButton(onClick = { viewModel.toggleOverlay() }) {
                                    Icon(
                                        if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        "Overlay",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(onClick = { showPaletteDialog = true }) {
                                    Icon(Icons.Default.Palette, "Palette", tint = Color.White)
                                }
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            // Primary controls
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                IconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@ThermalIrNightComposeActivity,
                                            ThermalGalleryComposeActivity::class.java,
                                        ),
                                    )
                                }) {
                                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                                }
                                FloatingActionButton(
                                    onClick = { viewModel.toggleRecording() },
                                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                                ) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        contentDescription = "Record",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(onClick = { showMoreOptions = true }) {
                                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            // Info Dialog
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = { Text("Night Vision Mode") },
                    text = {
                        Column {
                            Text("Enhanced thermal imaging optimized for low-light conditions:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Increased sensitivity")
                            Text("• Adjusted color palettes")
                            Text("• Reduced noise processing")
                            Text("• Optimized range settings")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("OK")
                        }
                    },
                )
            }
            // Palette Dialog
            if (showPaletteDialog) {
                AlertDialog(
                    onDismissRequest = { showPaletteDialog = false },
                    title = { Text("Select Color Palette") },
                    text = {
                        Column {
                            listOf("Ironbow", "Rainbow", "Grayscale", "Night Vision", "Hot Metal").forEach { palette ->
                                TextButton(onClick = { showPaletteDialog = false }) {
                                    Text(palette, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPaletteDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
            // Settings Dialog (reuse from IRThermalNightComposeActivity)
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Night Vision Settings") },
                    text = {
                        Column {
                            Text("Sensitivity: High")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Mode: Enhanced")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Temperature Range: Auto")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
            // Range Edit Dialog
            if (showRangeEditDialog) {
                AlertDialog(
                    onDismissRequest = { showRangeEditDialog = false },
                    title = { Text("Edit Temperature Range") },
                    text = {
                        Column {
                            Text("Min Temperature: 0°C")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Max Temperature: 100°C")
                            Spacer(modifier = Modifier.height(8.dp))
                            if (rangeLocked) {
                                Text("Range is locked", color = Color.Yellow)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRangeEditDialog = false }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRangeEditDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
            // More Options Menu
            if (showMoreOptions) {
                AlertDialog(
                    onDismissRequest = { showMoreOptions = false },
                    title = { Text("More Options") },
                    text = {
                        Column {
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Export Recording", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Share", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Calibrate", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Advanced Settings", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMoreOptions = false }) {
                            Text("Close")
                        }
                    },
                )
            }
        }
    }
}




