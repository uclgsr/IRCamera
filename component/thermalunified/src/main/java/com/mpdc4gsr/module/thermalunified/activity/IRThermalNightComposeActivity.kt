package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRThermalNightComposeActivity : BaseComposeActivity<IRThermalNightViewModel>() {

    override fun createViewModel(): IRThermalNightViewModel {
        return IRThermalNightViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalNightViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Brightness2,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Night Vision Thermal",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117) // Dark night theme
                        )
                    )
                }
            ) { paddingValues ->
                IRThermalNightContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRThermalNightContent(
        viewModel: IRThermalNightViewModel,
        modifier: Modifier = Modifier
    ) {
        var isRecording by remember { mutableStateOf(false) }
        var nightMode by remember { mutableStateOf("Enhanced") }
        var sensitivity by remember { mutableStateOf(75f) }
        var currentTemp by remember { mutableStateOf(22.5f) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Night mode status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Brightness2,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Night Vision Active",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enhanced sensitivity for low-light conditions",
                            color = Color(0xFF7D8590),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Thermal camera view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Night Thermal Feed",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enhanced night vision thermal imaging",
                            color = Color(0xFF7D8590),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "${currentTemp}°C",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Recording indicator
                    if (isRecording) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDA3633))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "● REC",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Night controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Night Vision Controls",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Night mode selection
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Standard", "Enhanced", "Ultra")
                        modes.forEach { mode ->
                            FilterChip(
                                onClick = { nightMode = mode },
                                label = {
                                    Text(
                                        mode,
                                        color = if (nightMode == mode) Color.White else Color(0xFF7D8590)
                                    )
                                },
                                selected = nightMode == mode,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF58A6FF),
                                    containerColor = Color(0xFF161B22)
                                )
                            )
                        }
                    }

                    // Sensitivity control
                    Column {
                        Text(
                            "Sensitivity: ${sensitivity.toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = sensitivity,
                            onValueChange = { sensitivity = it },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF58A6FF),
                                activeTrackColor = Color(0xFF58A6FF),
                                inactiveTrackColor = Color(0xFF30363D)
                            )
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF58A6FF)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF58A6FF))
                    )
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }

                Button(
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFDA3633) else Color(0xFF58A6FF)
                    )
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
        }
    }
}

class IRThermalNightViewModel : BaseViewModel()