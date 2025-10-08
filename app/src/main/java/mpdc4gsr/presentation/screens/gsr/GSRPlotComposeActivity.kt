package mpdc4gsr.presentation.screens.gsr
import dagger.hilt.android.AndroidEntryPoint

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel
import kotlin.math.sin

@AndroidEntryPoint
class GSRPlotComposeActivity : BaseComposeActivity<GSRPlotViewModel>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_DATA_PATH = "data_path"
        fun startActivity(
            context: Context,
            sessionId: String,
            dataPath: String? = null
        ) {
            val intent = Intent(context, GSRPlotComposeActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                dataPath?.let { putExtra(EXTRA_DATA_PATH, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRPlotViewModel {
        return viewModels<GSRPlotViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRPlotViewModel) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "unknown"
        val dataPath = intent.getStringExtra(EXTRA_DATA_PATH)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Data Analysis",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                // TODO: Implement data export
                                android.widget.Toast.makeText(
                                    context,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Implement plot sharing
                                android.widget.Toast.makeText(
                                    context,
                                    "Share plot feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Open plot settings
                                android.widget.Toast.makeText(
                                    context,
                                    "Plot settings feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Tune, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRPlotContent(
                    sessionId = sessionId,
                    dataPath = dataPath,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRPlotContent(
    sessionId: String,
    dataPath: String?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedVisualization by remember { mutableStateOf(VisualizationType.LINE_CHART) }
    var timeRange by remember { mutableStateOf(TimeRange.ALL) }
    var showStatistics by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visualization Controls
        VisualizationControlsCard(
            selectedVisualization = selectedVisualization,
            selectedTimeRange = timeRange,
            onVisualizationChange = { selectedVisualization = it },
            onTimeRangeChange = { timeRange = it }
        )
        // Main Plot Area
        MainPlotCard(
            visualizationType = selectedVisualization,
            timeRange = timeRange,
            sessionId = sessionId,
            context = context
        )
        // Statistics Panel
        if (showStatistics) {
            StatisticsCard(sessionId = sessionId)
        }
        // Data Analysis Tools
        DataAnalysisToolsCard(context = context)
        // Export Options
        ExportOptionsCard(context = context)
    }
}

enum class VisualizationType {
    LINE_CHART,
    SCATTER_PLOT,
    HISTOGRAM,
    HEATMAP
}

enum class TimeRange {
    ALL,
    LAST_MINUTE,
    LAST_5_MINUTES,
    LAST_10_MINUTES,
    CUSTOM
}

@Composable
private fun VisualizationControlsCard(
    selectedVisualization: VisualizationType,
    selectedTimeRange: TimeRange,
    onVisualizationChange: (VisualizationType) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit
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
                "Visualization Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Visualization Type Selection
            Text(
                "Chart Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().take(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().drop(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Time Range Selection
            Text(
                "Time Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.values().take(3).forEach { range ->
                    FilterChip(
                        onClick = { onTimeRangeChange(range) },
                        label = {
                            Text(
                                when (range) {
                                    TimeRange.ALL -> "All"
                                    TimeRange.LAST_MINUTE -> "1m"
                                    TimeRange.LAST_5_MINUTES -> "5m"
                                    TimeRange.LAST_10_MINUTES -> "10m"
                                    TimeRange.CUSTOM -> "Custom"
                                }
                            )
                        },
                        selected = selectedTimeRange == range,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPlotCard(
    visualizationType: VisualizationType,
    timeRange: TimeRange,
    sessionId: String,
    context: android.content.Context
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
                    "GSR Signal Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        // TODO: Implement zoom in functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom in feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = {
                        // TODO: Implement zoom out functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom out feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(onClick = {
                        // TODO: Implement reset zoom functionality
                        android.widget.Toast.makeText(
                            context,
                            "Reset zoom feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset")
                    }
                }
            }
            HorizontalDivider()
            // Plot Area
            when (visualizationType) {
                VisualizationType.LINE_CHART -> {
                    GSRLineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.SCATTER_PLOT -> {
                    GSRScatterPlot(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HISTOGRAM -> {
                    GSRHistogram(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HEATMAP -> {
                    GSRHeatmap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
            // Plot Legend
            PlotLegend()
        }
    }
}

@Composable
private fun GSRLineChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRLineChart(this, primaryColor, secondaryColor)
    }
}

private fun drawGSRLineChart(drawScope: DrawScope, primaryColor: Color, secondaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample GSR data
        val dataPoints = generateSampleGSRData(100)
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        // Draw the GSR signal line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f)
        )
        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            drawCircle(
                color = secondaryColor,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRScatterPlot(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRScatterPlot(this, primaryColor)
    }
}

private fun drawGSRScatterPlot(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample scatter data
        val dataPoints = generateSampleGSRData(50)
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            // Vary point size based on value
            val radius = 3f + (value * 5f)
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRHistogram(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHistogram(this, primaryColor)
    }
}

private fun drawGSRHistogram(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate histogram data
        val binCount = 15
        val binWidth = (width - 2 * padding) / binCount
        val histogramData = generateHistogramData(binCount)
        histogramData.forEachIndexed { index, value ->
            val x = padding + index * binWidth
            val barHeight = value * (height - 2 * padding)
            drawRect(
                color = primaryColor.copy(alpha = 0.8f),
                topLeft = Offset(x, height - padding - barHeight),
                size = androidx.compose.ui.geometry.Size(binWidth * 0.8f, barHeight)
            )
        }
    }
}

@Composable
private fun GSRHeatmap(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHeatmap(this, primaryColor)
    }
}

private fun drawGSRHeatmap(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val cellSize = 20f
        val cols = (width / cellSize).toInt()
        val rows = (height / cellSize).toInt()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val intensity = (sin((row + col) * 0.3) + 1) / 2
                val color = primaryColor.copy(alpha = intensity.toFloat())
                drawRect(
                    color = color,
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}

@Composable
private fun PlotLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("GSR Signal", MaterialTheme.colorScheme.primary)
        LegendItem("Data Points", MaterialTheme.colorScheme.secondary)
        LegendItem("Threshold", MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatisticsCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Statistical Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Mean", "12.5 μS")
                StatisticItem("Std Dev", "3.2 μS")
                StatisticItem("Min", "8.1 μS")
                StatisticItem("Max", "18.9 μS")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Peaks", "24")
                StatisticItem("Frequency", "0.8 Hz")
                StatisticItem("Trend", "↗ Rising")
                StatisticItem("Quality", "95%")
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataAnalysisToolsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analysis Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data filter
                        android.widget.Toast.makeText(
                            context,
                            "Applying filter...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data smoothing
                        android.widget.Toast.makeText(
                            context,
                            "Smoothing data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Smooth Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Smooth")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement peak detection algorithm
                        android.widget.Toast.makeText(
                            context,
                            "Peak detection feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Detect Peaks")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peaks")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement trend analysis
                        android.widget.Toast.makeText(
                            context,
                            "Trend analysis feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analyze Trends")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trends")
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Export data to CSV
                        android.widget.Toast.makeText(
                            context,
                            "Exporting to CSV...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Save plot as image
                        android.widget.Toast.makeText(
                            context,
                            "Saving plot...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Save Plot as Image")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Plot")
                }
            }
        }
    }
}

// Helper functions for generating sample data
private fun generateSampleGSRData(points: Int): List<Float> {
    return (0 until points).map { i ->
        val baseValue = 0.3f + sin(i * 0.1f) * 0.2f
        val noise = (kotlin.random.Random.nextFloat() - 0.5f) * 0.1f
        (baseValue + noise).coerceIn(0f, 1f)
    }
}

private fun generateHistogramData(bins: Int): List<Float> {
    return (0 until bins).map { i ->
        val centerValue = i.toFloat() / bins
        kotlin.math.exp(-((centerValue - 0.5f) * (centerValue - 0.5f)) / 0.2f).toFloat()
    }
}

class GSRPlotViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing plot data, zoom state, filters, etc.
    // Future implementation would include:
    // - Data loading from files or database
    // - Real-time data updates
    // - Zoom and pan state management
    // - Filter state management
    // - Export functionality
}