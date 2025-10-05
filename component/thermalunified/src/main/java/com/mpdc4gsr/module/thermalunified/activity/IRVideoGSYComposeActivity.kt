package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
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

class IRVideoGSYComposeActivity : BaseComposeActivity<IRVideoGSYViewModel>() {

    override fun createViewModel(): IRVideoGSYViewModel {
        return IRVideoGSYViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRVideoGSYViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Video Player",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFF6B35)
                        )
                    )
                }
            ) { paddingValues ->
                IRVideoGSYContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRVideoGSYContent(
        viewModel: IRVideoGSYViewModel,
        modifier: Modifier = Modifier
    ) {
        var isPlaying by remember { mutableStateOf(false) }
        var currentTime by remember { mutableStateOf(0f) }
        var totalTime by remember { mutableStateOf(120f) }
        var playbackSpeed by remember { mutableStateOf(1f) }
        var showThermalOverlay by remember { mutableStateOf(true) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video player area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
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
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Thermal Video Playback",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "GSY Player with thermal analysis",
                            color = Color(0xFF9E9E9E),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Thermal overlay indicator
                    if (showThermalOverlay) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35))
                        ) {
                            Text(
                                "THERMAL",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Video controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress slider
                    Column {
                        Slider(
                            value = currentTime,
                            onValueChange = { currentTime = it },
                            valueRange = 0f..totalTime,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${(currentTime / 60).toInt()}:${
                                    (currentTime % 60).toInt().toString().padStart(2, '0')
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${(totalTime / 60).toInt()}:${(totalTime % 60).toInt().toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentTime = (currentTime - 10f).coerceAtLeast(0f) }
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Back 10s")
                        }

                        FloatingActionButton(
                            onClick = { isPlaying = !isPlaying },
                            containerColor = Color(0xFFFF6B35)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { currentTime = (currentTime + 10f).coerceAtMost(totalTime) }
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                        }
                    }

                    // Speed and overlay controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback speed
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed: ")
                            val speeds = listOf(0.5f, 1f, 1.5f, 2f)
                            speeds.forEach { speed ->
                                FilterChip(
                                    onClick = { playbackSpeed = speed },
                                    label = { Text("${speed}x") },
                                    selected = playbackSpeed == speed,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF6B35),
                                        selectedLabelColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        // Thermal overlay toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thermal Overlay")
                            Switch(
                                checked = showThermalOverlay,
                                onCheckedChange = { showThermalOverlay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF6B35),
                                    checkedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Temperature analysis
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBE0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Frame Temperature Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Max", style = MaterialTheme.typography.bodySmall)
                            Text("42.3°C", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Min", style = MaterialTheme.typography.bodySmall)
                            Text("18.7°C", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg", style = MaterialTheme.typography.bodySmall)
                            Text("28.5°C", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

class IRVideoGSYViewModel : BaseViewModel()