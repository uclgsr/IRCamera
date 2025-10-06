package com.mpdc4gsr.module.thermalunified.activity

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRLogMPChartComposeActivity : BaseComposeActivity<IRLogMPChartViewModel>() {
    override fun createViewModel(): IRLogMPChartViewModel {
        return IRLogMPChartViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRLogMPChartViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Data Logging",
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
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast.makeText(
                                    this@IRLogMPChartComposeActivity,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFF6B35)
                        )
                    )
                }
            ) { paddingValues ->
                IRLogMPChartContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRLogMPChartContent(
        viewModel: IRLogMPChartViewModel,
        modifier: Modifier = Modifier
    ) {
        var isLogging by remember { mutableStateOf(false) }
        var dataPoints by remember { mutableStateOf(247) }
        var loggingDuration by remember { mutableStateOf("00:04:07") }
        var chartType by remember { mutableStateOf("Line Chart") }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logging status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLogging) Color(0xFFE8F5E8) else Color(0xFFF8F9FA)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLogging) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (isLogging) Color(0xFF4CAF50) else Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isLogging) "Logging Active" else "Logging Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLogging) Color(0xFF4CAF50) else Color(0xFF666666)
                        )
                        Text(
                            "$dataPoints data points | Duration: $loggingDuration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isLogging,
                        onCheckedChange = { isLogging = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                        )
                    )
                }
            }
            // Chart type selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBE0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Chart Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chartTypes = listOf("Line Chart", "Bar Chart", "Area Chart", "Scatter Plot")
                        chartTypes.forEach { type ->
                            FilterChip(
                                onClick = { chartType = type },
                                label = { Text(type) },
                                selected = chartType == type,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B35),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            // Chart area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Temperature Trend Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "MP Android Chart Integration",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35)
                            )
                            Text(
                                "Real-time thermal data visualization",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
            // Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Data Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem("Peak", "45.2°C", Color(0xFFF44336))
                        StatisticItem("Valley", "18.7°C", Color(0xFF2196F3))
                        StatisticItem("Average", "28.9°C", Color(0xFF4CAF50))
                        StatisticItem("Variance", "±3.4°C", Color(0xFFFF9800))
                    }
                }
            }
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement clear data functionality
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Clear data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = {
                        // TODO: Implement CSV export
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Export CSV feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
                Button(
                    onClick = {
                        // TODO: Implement PDF export
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Export PDF feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }
        }
    }

    @Composable
    private fun StatisticItem(
        label: String,
        value: String,
        color: Color
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

class IRLogMPChartViewModel : BaseViewModel()