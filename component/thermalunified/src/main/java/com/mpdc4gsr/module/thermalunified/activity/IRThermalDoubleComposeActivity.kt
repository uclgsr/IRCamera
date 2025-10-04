package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRThermalDoubleViewModel

class IRThermalDoubleComposeActivity : BaseComposeActivity<IRThermalDoubleViewModel>() {

    override fun createViewModel(): IRThermalDoubleViewModel {
        return viewModels<IRThermalDoubleViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalDoubleViewModel) {
        val selectedMode by viewModel.selectedMode.collectAsState()
        val showOverlay by viewModel.showOverlay.collectAsState()
        val showTrendChart by viewModel.showTrendChart.collectAsState()
        val showCompass by viewModel.showCompass.collectAsState()
        val isRecording by viewModel.isRecording.collectAsState()

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectMode(0) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 0) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Temperature", fontSize = 16.sp)
                                        if (selectedMode == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.selectMode(1) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 1) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Observe", fontSize = 16.sp)
                                        if (selectedMode == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
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
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.showInfo() }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.toggleTISR() }) {
                                Icon(Icons.Default.Settings, contentDescription = "TISR", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Main thermal display area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .aspectRatio(192f / 256f)
                            .background(Color.Black)
                    ) {
                        // Thermal camera view placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Thermal Camera View",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }

                        // Overlay controls
                        if (showOverlay) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                // Temperature range controls would go here
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0x80000000)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = { viewModel.lockTemperatureRange() }) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Lock",
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            "Temp Range",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                        IconButton(onClick = { viewModel.editTemperatureSettings() }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recording indicator
                        if (isRecording) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x80000000)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red)
                                    )
                                    Text("00:00", color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }

                        // Trend chart overlay
                        if (showTrendChart) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(264f / 158f),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xE6 + 0x16131E)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Trend", color = Color.White, fontSize = 14.sp)
                                        IconButton(
                                            onClick = { if (showTrendChart) viewModel.toggleTrendChart() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Chart",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom menu controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16131E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // Secondary menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = { viewModel.toggleTrendChart() }) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, "Trend", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleCompass() }) {
                                    Icon(Icons.Default.Explore, "Compass", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleOverlay() }) {
                                    Icon(
                                        if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        "Toggle Overlay",
                                        tint = Color.White
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Primary menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = { viewModel.openGallery() }) {
                                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleRecording() }) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        "Record",
                                        tint = if (isRecording) Color.Red else Color.White
                                    )
                                }
                                IconButton(onClick = { viewModel.captureCamera() }) {
                                    Icon(Icons.Default.CameraAlt, "Camera", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.showMoreOptions() }) {
                                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
