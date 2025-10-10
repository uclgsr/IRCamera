package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import kotlinx.coroutines.launch

class MonitorChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var showSettings by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Chart",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            )
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
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0D1117),
                            ),
                    )
                },
                floatingActionButton = {
                    var isRecording by remember { mutableStateOf(false) }
                    FloatingActionButton(
                        onClick = { isRecording = !isRecording },
                        containerColor = if (isRecording) Color(0xFFDC2626) else Color(0xFFFF6B35),
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "Stop" else "Start",
                            tint = Color.White,
                        )
                    }
                },
            ) { paddingValues ->
                MonitorChartContent(
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }

    @Composable
    private fun MonitorChartContent(
        scope: kotlinx.coroutines.CoroutineScope,
        snackbarHostState: SnackbarHostState,
        modifier: Modifier = Modifier,
    ) {
        var timeRange by remember { mutableStateOf("1hr") }
        var alertThreshold by remember { mutableFloatStateOf(35f) }
        var showAlerts by remember { mutableStateOf(true) }
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Live Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCard("Current", "28.5°C", Color(0xFF7D8590))
                    StatCard("Peak", "42.1°C", Color(0xFFFF6B35))
                    StatCard("Average", "31.2°C", Color(0xFF4A90E2))
                    StatCard("Sensors", "4", Color(0xFF238636))
                }
            }
            // Chart Display Area
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Chart",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Real-time Temperature Chart",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Multi-sensor monitoring with threshold alerts",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Chart legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LegendItem("Sensor 1", Color(0xFFFF6B35))
                            LegendItem("Sensor 2", Color(0xFF4A90E2))
                            LegendItem("Sensor 3", Color(0xFF238636))
                            LegendItem("Sensor 4", Color(0xFFFFD700))
                        }
                    }
                }
            }
            // Time Range Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Time Range",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TimeRangeChip("5min", timeRange == "5min") { timeRange = "5min" }
                        TimeRangeChip("15min", timeRange == "15min") { timeRange = "15min" }
                        TimeRangeChip("1hr", timeRange == "1hr") { timeRange = "1hr" }
                        TimeRangeChip("6hr", timeRange == "6hr") { timeRange = "6hr" }
                        TimeRangeChip("24hr", timeRange == "24hr") { timeRange = "24hr" }
                    }
                }
            }
            // Alert Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Temperature Alerts",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Switch(
                            checked = showAlerts,
                            onCheckedChange = { showAlerts = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF6B35),
                                    checkedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f),
                                ),
                        )
                    }
                    if (showAlerts) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Threshold: ${alertThreshold.toInt()}°C",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp,
                        )
                        Slider(
                            value = alertThreshold,
                            onValueChange = { alertThreshold = it },
                            valueRange = 0f..100f,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF6B35),
                                    activeTrackColor = Color(0xFFFF6B35),
                                ),
                        )
                    }
                }
            }
            // Chart Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Chart data exported")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF7D8590),
                        ),
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Chart data cleared")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF7D8590),
                        ),
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Zoom to fit applied")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35),
                        ),
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Zoom")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zoom")
                }
            }
        }
    }

    @Composable
    private fun StatCard(
        label: String,
        value: String,
        color: Color,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 12.sp,
            )
        }
    }

    @Composable
    private fun TimeRangeChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        FilterChip(
            onClick = onClick,
            label = { Text(label) },
            selected = selected,
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF6B35),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF0D1117),
                    labelColor = Color(0xFF7D8590),
                ),
        )
    }

    @Composable
    private fun LegendItem(
        label: String,
        color: Color,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(2.dp)),
            )
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 12.sp,
            )
        }
    }
}
