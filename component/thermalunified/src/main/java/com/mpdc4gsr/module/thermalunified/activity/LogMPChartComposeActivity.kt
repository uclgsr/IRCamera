package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
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

class LogMPChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isLogging by remember { mutableStateOf(false) }
        var logEntries by remember { mutableIntStateOf(0) }
        var dataPoints by remember { mutableIntStateOf(125) }
        var selectedTimeRange by remember { mutableStateOf("1 Hour") }
        var showExportDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        LibUnifiedTheme {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Data Log Chart",
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
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logging status card
                    item {
                        LoggingStatusCard(
                            isLogging = isLogging,
                            logEntries = logEntries,
                            dataPoints = dataPoints,
                            onToggleLogging = {
                                isLogging = !isLogging
                                if (isLogging) logEntries = 0
                            }
                        )
                    }

                    // Chart display
                    item {
                        ChartDisplayCard(
                            selectedTimeRange = selectedTimeRange,
                            dataPoints = dataPoints
                        )
                    }

                    // Time range selector
                    item {
                        TimeRangeSelector(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { selectedTimeRange = it }
                        )
                    }

                    // Chart statistics
                    item {
                        ChartStatisticsCard()
                    }

                    // Export and management
                    item {
                        DataManagementCard(
                            onExportCsv = {
                                scope.launch {
                                    viewModel.exportData(this@LogMPChartComposeActivity, ThermalViewModel.ExportFormat.CSV)
                                    snackbarHostState.showSnackbar("Exporting data as CSV...")
                                }
                            },
                            onExportPdf = {
                                kotlinx.coroutines.GlobalScope.launch {
                                    viewModel.exportData(this@LogMPChartComposeActivity, ThermalViewModel.ExportFormat.PDF)
                                    snackbarHostState.showSnackbar("Exporting data as PDF...")
                                }
                            },
                            onClearData = {
                                logEntries = 0
                                dataPoints = 0
                            }
                        )
                    }
                }
            }
        }

        // Simulate logging
        LaunchedEffect(isLogging) {
            if (isLogging) {
                while (isLogging) {
                    kotlinx.coroutines.delay(1000L)
                    logEntries++
                    dataPoints++
                }
            }
        }
        
        // Export dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Chart Data") },
                text = {
                    Column {
                        Text("Select export format:")
                        Spacer(modifier = Modifier.height(16.dp))
                        listOf("Image (PNG)", "CSV Data", "PDF Report").forEach { format ->
                            TextButton(
                                onClick = {
                                    kotlinx.coroutines.GlobalScope.launch {
                                        snackbarHostState.showSnackbar("Exporting as $format...")
                                    }
                                    showExportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(format)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Settings dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Chart Settings") },
                text = {
                    Column {
                        Text("Configure chart display settings")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Time range", style = MaterialTheme.typography.bodySmall)
                        Text("• Data refresh rate", style = MaterialTheme.typography.bodySmall)
                        Text("• Chart colors", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@Composable
private fun LoggingStatusCard(
    isLogging: Boolean,
    logEntries: Int,
    dataPoints: Int,
    onToggleLogging: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Data Logging",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isLogging) "Recording..." else "Stopped",
                        color = if (isLogging) Color(0xFF00FF00) else Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = isLogging,
                    onCheckedChange = { onToggleLogging() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF6B35),
                        uncheckedThumbColor = Color(0xFF7D8590),
                        uncheckedTrackColor = Color(0xFF16131E)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LogStatItem("Log Entries", logEntries.toString())
                LogStatItem("Data Points", dataPoints.toString())
                LogStatItem("Duration", if (isLogging) "${logEntries}s" else "0s")
            }
        }
    }
}

@Composable
private fun LogStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color(0xFFFF6B35),
            fontSize = 18.sp,
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
private fun ChartDisplayCard(
    selectedTimeRange: String,
    dataPoints: Int
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
                "Temperature Chart - $selectedTimeRange",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Chart",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Temperature Trend Chart",
                        color = Color(0xFF7D8590),
                        fontSize = 16.sp
                    )
                    Text(
                        "$dataPoints data points",
                        color = Color(0xFFFF6B35),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit
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
                "Time Range",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timeRanges = listOf("15 Min", "1 Hour", "6 Hours", "24 Hours")

                timeRanges.forEach { range ->
                    FilterChip(
                        onClick = { onRangeSelected(range) },
                        label = { Text(range, fontSize = 12.sp) },
                        selected = selectedRange == range,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartStatisticsCard() {
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
                "Chart Statistics",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Peak", "47.2°C", Color(0xFFFF4444))
                StatItem("Valley", "18.1°C", Color(0xFF4444FF))
                StatItem("Average", "32.7°C", Color(0xFFFFAA00))
                StatItem("Variance", "±4.8°C", Color(0xFF7D8590))
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = color,
            fontSize = 14.sp,
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
private fun DataManagementCard(
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onClearData: () -> Unit
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
                "Data Management",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("Export CSV", fontSize = 12.sp)
                }

                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B7280)
                    )
                ) {
                    Text("Export PDF", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onClearData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
    }
}