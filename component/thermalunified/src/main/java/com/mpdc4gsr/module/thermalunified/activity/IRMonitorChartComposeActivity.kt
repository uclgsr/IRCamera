package com.mpdc4gsr.module.thermalunified.activity
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
class IRMonitorChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingTime by remember { mutableLongStateOf(0L) }
        var maxTemp by remember { mutableFloatStateOf(25.0f) }
        var minTemp by remember { mutableFloatStateOf(20.0f) }
        var avgTemp by remember { mutableFloatStateOf(22.5f) }
        var showTemperatureOverlay by remember { mutableStateOf(true) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Chart",
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
                            IconButton(onClick = { showTemperatureOverlay = !showTemperatureOverlay }) {
                                Icon(
                                    if (showTemperatureOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showTemperatureOverlay) "Hide Overlay" else "Show Overlay",
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
                val context = androidx.compose.ui.platform.LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Main thermal camera view with overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    ) {
                        // Thermal camera view
                        ThermalCameraView(
                            modifier = Modifier.fillMaxSize()
                        )
                        // Temperature overlay
                        if (showTemperatureOverlay) {
                            TemperatureOverlay(
                                maxTemp = maxTemp,
                                minTemp = minTemp,
                                avgTemp = avgTemp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            )
                        }
                        // Recording indicator
                        if (isRecording) {
                            RecordingIndicator(
                                recordingTime = recordingTime,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )
                        }
                    }
                    // Control panel and chart data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Recording controls
                        item {
                            RecordingControls(
                                isRecording = isRecording,
                                onStartStop = {
                                    isRecording = !isRecording
                                    if (!isRecording) recordingTime = 0L
                                },
                                context = context
                            )
                        }
                        // Temperature statistics
                        item {
                            TemperatureStatsCard(
                                maxTemp = maxTemp,
                                minTemp = minTemp,
                                avgTemp = avgTemp
                            )
                        }
                        // Chart controls
                        item {
                            ChartControlsCard(context = context)
                        }
                    }
                }
            }
        }
        // Recording timer
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    kotlinx.coroutines.delay(1000L)
                    recordingTime++
                    // Simulate temperature changes
                    maxTemp = 20f + (recordingTime % 10) * 0.5f
                    minTemp = 15f + (recordingTime % 8) * 0.3f
                    avgTemp = (maxTemp + minTemp) / 2f
                }
            }
        }
    }
}
@Composable
private fun ThermalCameraView(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera view
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
            Text(
                "Thermal Camera Feed\n(Real-time monitoring)",
                color = Color(0xFF7D8590),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
private fun TemperatureOverlay(
    maxTemp: Float,
    minTemp: Float,
    avgTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Temperature",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            TemperatureItem("Max", maxTemp, Color(0xFFFF4444))
            TemperatureItem("Min", minTemp, Color(0xFF4444FF))
            TemperatureItem("Avg", avgTemp, Color(0xFFFFAA00))
        }
    }
}
@Composable
private fun TemperatureItem(
    label: String,
    temperature: Float,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
        Text(
            String.format("%.1f°C", temperature),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
private fun RecordingIndicator(
    recordingTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = "Recording",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            val minutes = recordingTime / 60
            val seconds = recordingTime % 60
            Text(
                String.format("REC %02d:%02d", minutes, seconds),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun RecordingControls(
    isRecording: Boolean,
    onStartStop: () -> Unit,
    context: android.content.Context
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop" else "Start",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isRecording) "Stop" else "Start",
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    // TODO: Save chart as image
                    android.widget.Toast.makeText(
                        context,
                        "Saving chart...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
private fun TemperatureStatsCard(
    maxTemp: Float,
    minTemp: Float,
    avgTemp: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Temperature Statistics",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Maximum", maxTemp, Color(0xFFFF4444))
                StatItem("Minimum", minTemp, Color(0xFF4444FF))
                StatItem("Average", avgTemp, Color(0xFFFFAA00))
            }
        }
    }
}
@Composable
private fun StatItem(
    label: String,
    value: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            String.format("%.1f°C", value),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
    }
}
@Composable
private fun ChartControlsCard(context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Chart Controls",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export monitoring data
                        android.widget.Toast.makeText(
                            context,
                            "Exporting data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Export", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Clear monitoring data
                        android.widget.Toast.makeText(
                            context,
                            "Clearing data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Open monitoring settings
                        android.widget.Toast.makeText(
                            context,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Settings", fontSize = 12.sp)
                }
            }
        }
    }
}