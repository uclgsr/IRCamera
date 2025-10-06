package com.mpdc4gsr.module.thermalunified.compose
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
@Composable
fun ChartLogCompose(
    thermalData: List<ThermalDataEntry>,
    chartType: ThermalChartType = ThermalChartType.POINT,
    timeFormat: TimeFormat = TimeFormat.HOURS,
    showGrid: Boolean = true,
    showLegend: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Chart header
            ChartHeader(
                title = "Thermal Log Chart",
                chartType = chartType,
                dataCount = thermalData.size
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Chart area
            if (thermalData.isEmpty()) {
                EmptyChartState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    drawThermalChart(
                        data = thermalData,
                        chartType = chartType,
                        timeFormat = timeFormat,
                        showGrid = showGrid
                    )
                }
                if (showLegend) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ChartLegend(chartType = chartType)
                }
            }
        }
    }
}
@Composable
private fun ChartHeader(
    title: String,
    chartType: ThermalChartType,
    dataCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${chartType.displayName} • $dataCount points",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = chartType.icon,
            contentDescription = chartType.displayName,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
@Composable
private fun EmptyChartState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = "No data",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No thermal data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun ChartLegend(
    chartType: ThermalChartType
) {
    val legendItems = when (chartType) {
        ThermalChartType.POINT -> listOf(
            LegendItem("Point Temperature", Color(0xFF2196F3))
        )
        ThermalChartType.LINE -> listOf(
            LegendItem("Max Temperature", Color(0xFFFF5722)),
            LegendItem("Min Temperature", Color(0xFF2196F3))
        )
        ThermalChartType.AREA -> listOf(
            LegendItem("Max Temperature", Color(0xFFFF5722)),
            LegendItem("Center Temperature", Color(0xFF4CAF50)),
            LegendItem("Min Temperature", Color(0xFF2196F3))
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        legendItems.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(item.color, RoundedCornerShape(2.dp))
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
private fun DrawScope.drawThermalChart(
    data: List<ThermalDataEntry>,
    chartType: ThermalChartType,
    timeFormat: TimeFormat,
    showGrid: Boolean
) {
    if (data.isEmpty()) return
    val padding = 40.dp.toPx()
    val chartWidth = size.width - 2 * padding
    val chartHeight = size.height - 2 * padding
    // Calculate time and temperature ranges
    val minTime = data.minOfOrNull { it.timestamp } ?: 0L
    val maxTime = data.maxOfOrNull { it.timestamp } ?: 0L
    val timeRange = maxTime - minTime
    val allTemps = data.flatMap { entry ->
        when (chartType) {
            ThermalChartType.POINT -> listOf(entry.temperature)
            ThermalChartType.LINE -> listOf(entry.temperatureMax, entry.temperatureMin)
            ThermalChartType.AREA -> listOf(entry.temperatureMax, entry.temperatureCenter, entry.temperatureMin)
        }
    }
    val minTemp = allTemps.minOrNull() ?: 0f
    val maxTemp = allTemps.maxOrNull() ?: 100f
    val tempRange = maxTemp - minTemp
    if (timeRange <= 0 || tempRange <= 0) return
    // Draw grid
    if (showGrid) {
        drawGrid(
            padding = padding,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            minTemp = minTemp,
            maxTemp = maxTemp,
            minTime = minTime,
            maxTime = maxTime,
            timeFormat = timeFormat
        )
    }
    // Draw data based on chart type
    when (chartType) {
        ThermalChartType.POINT -> {
            drawPointSeries(
                data = data,
                padding = padding,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                minTime = minTime,
                timeRange = timeRange,
                minTemp = minTemp,
                tempRange = tempRange,
                color = Color(0xFF2196F3)
            )
        }
        ThermalChartType.LINE -> {
            drawLineSeries(
                data = data,
                padding = padding,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                minTime = minTime,
                timeRange = timeRange,
                minTemp = minTemp,
                tempRange = tempRange
            )
        }
        ThermalChartType.AREA -> {
            drawAreaSeries(
                data = data,
                padding = padding,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                minTime = minTime,
                timeRange = timeRange,
                minTemp = minTemp,
                tempRange = tempRange
            )
        }
    }
}
private fun DrawScope.drawGrid(
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minTemp: Float,
    maxTemp: Float,
    minTime: Long,
    maxTime: Long,
    timeFormat: TimeFormat
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val textPaint = android.graphics.Paint().apply {
        color = Color.Gray.value.toInt()
        textSize = 10.sp.toPx()
        isAntiAlias = true
    }
    // Horizontal grid lines (temperature)
    val tempGridCount = 5
    for (i in 0..tempGridCount) {
        val y = padding + (chartHeight * i / tempGridCount)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(size.width - padding, y),
            strokeWidth = 1.dp.toPx()
        )
        // Temperature labels
        val temp = maxTemp - ((maxTemp - minTemp) * i / tempGridCount)
        drawContext.canvas.nativeCanvas.drawText(
            "${temp.roundToInt()}°",
            5.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
    // Vertical grid lines (time)
    val timeGridCount = 4
    val dateFormat = SimpleDateFormat(
        when (timeFormat) {
            TimeFormat.SECONDS -> "HH:mm:ss"
            TimeFormat.MINUTES -> "HH:mm"
            TimeFormat.HOURS -> "HH:mm"
        },
        Locale.getDefault()
    )
    for (i in 0..timeGridCount) {
        val x = padding + (chartWidth * i / timeGridCount)
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, size.height - padding),
            strokeWidth = 0.5.dp.toPx()
        )
        // Time labels
        val time = minTime + ((maxTime - minTime) * i / timeGridCount)
        val timeLabel = dateFormat.format(Date(time))
        drawContext.canvas.nativeCanvas.drawText(
            timeLabel,
            x - 20.dp.toPx(),
            size.height - 10.dp.toPx(),
            textPaint
        )
    }
}
private fun DrawScope.drawPointSeries(
    data: List<ThermalDataEntry>,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minTime: Long,
    timeRange: Long,
    minTemp: Float,
    tempRange: Float,
    color: Color
) {
    val path = Path()
    var isFirst = true
    data.forEach { entry ->
        val x = padding + chartWidth * ((entry.timestamp - minTime).toFloat() / timeRange)
        val y = padding + chartHeight - (chartHeight * ((entry.temperature - minTemp) / tempRange))
        if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
        } else {
            path.lineTo(x, y)
        }
        // Draw data points
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = Offset(x, y)
        )
    }
    // Draw connecting line
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx())
    )
}
private fun DrawScope.drawLineSeries(
    data: List<ThermalDataEntry>,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minTime: Long,
    timeRange: Long,
    minTemp: Float,
    tempRange: Float
) {
    val maxPath = Path()
    val minPath = Path()
    var isFirst = true
    data.forEach { entry ->
        val x = padding + chartWidth * ((entry.timestamp - minTime).toFloat() / timeRange)
        val maxY = padding + chartHeight - (chartHeight * ((entry.temperatureMax - minTemp) / tempRange))
        val minY = padding + chartHeight - (chartHeight * ((entry.temperatureMin - minTemp) / tempRange))
        if (isFirst) {
            maxPath.moveTo(x, maxY)
            minPath.moveTo(x, minY)
            isFirst = false
        } else {
            maxPath.lineTo(x, maxY)
            minPath.lineTo(x, minY)
        }
        // Draw data points
        drawCircle(
            color = Color(0xFFFF5722),
            radius = 2.dp.toPx(),
            center = Offset(x, maxY)
        )
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 2.dp.toPx(),
            center = Offset(x, minY)
        )
    }
    // Draw lines
    drawPath(
        path = maxPath,
        color = Color(0xFFFF5722),
        style = Stroke(width = 2.dp.toPx())
    )
    drawPath(
        path = minPath,
        color = Color(0xFF2196F3),
        style = Stroke(width = 2.dp.toPx())
    )
}
private fun DrawScope.drawAreaSeries(
    data: List<ThermalDataEntry>,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    minTime: Long,
    timeRange: Long,
    minTemp: Float,
    tempRange: Float
) {
    val maxPath = Path()
    val centerPath = Path()
    val minPath = Path()
    var isFirst = true
    data.forEach { entry ->
        val x = padding + chartWidth * ((entry.timestamp - minTime).toFloat() / timeRange)
        val maxY = padding + chartHeight - (chartHeight * ((entry.temperatureMax - minTemp) / tempRange))
        val centerY = padding + chartHeight - (chartHeight * ((entry.temperatureCenter - minTemp) / tempRange))
        val minY = padding + chartHeight - (chartHeight * ((entry.temperatureMin - minTemp) / tempRange))
        if (isFirst) {
            maxPath.moveTo(x, maxY)
            centerPath.moveTo(x, centerY)
            minPath.moveTo(x, minY)
            isFirst = false
        } else {
            maxPath.lineTo(x, maxY)
            centerPath.lineTo(x, centerY)
            minPath.lineTo(x, minY)
        }
        // Draw data points
        drawCircle(color = Color(0xFFFF5722), radius = 2.dp.toPx(), center = Offset(x, maxY))
        drawCircle(color = Color(0xFF4CAF50), radius = 2.dp.toPx(), center = Offset(x, centerY))
        drawCircle(color = Color(0xFF2196F3), radius = 2.dp.toPx(), center = Offset(x, minY))
    }
    // Draw lines
    drawPath(path = maxPath, color = Color(0xFFFF5722), style = Stroke(width = 2.dp.toPx()))
    drawPath(path = centerPath, color = Color(0xFF4CAF50), style = Stroke(width = 2.dp.toPx()))
    drawPath(path = minPath, color = Color(0xFF2196F3), style = Stroke(width = 2.dp.toPx()))
}
// Data classes and enums
data class ThermalDataEntry(
    val timestamp: Long,
    val temperature: Float,
    val temperatureMax: Float = temperature,
    val temperatureMin: Float = temperature,
    val temperatureCenter: Float = temperature,
    val type: String = "point"
)
enum class ThermalChartType(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    POINT("Point Temperature", Icons.Default.Place),
    LINE("Line Temperature", Icons.Default.Timeline),
    AREA("Area Temperature", Icons.Default.CropFree)
}
enum class TimeFormat {
    SECONDS, MINUTES, HOURS
}
private data class LegendItem(
    val label: String,
    val color: Color
)
@Composable
fun ChartLogComposePreview() {
    val sampleData = remember {
        val currentTime = System.currentTimeMillis()
        (0..50).map { i ->
            ThermalDataEntry(
                timestamp = currentTime + i * 60000L, // 1 minute intervals
                temperature = 25f + (Math.sin(i * 0.2) * 5).toFloat(),
                temperatureMax = 30f + (Math.sin(i * 0.15) * 8).toFloat(),
                temperatureMin = 20f + (Math.sin(i * 0.25) * 3).toFloat(),
                temperatureCenter = 25f + (Math.sin(i * 0.18) * 4).toFloat()
            )
        }
    }
    var selectedType by remember { mutableStateOf(ThermalChartType.POINT) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart type selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThermalChartType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.displayName) }
                )
            }
        }
        ChartLogCompose(
            thermalData = sampleData,
            chartType = selectedType,
            timeFormat = TimeFormat.MINUTES,
            modifier = Modifier.fillMaxWidth()
        )
        // Empty state example
        ChartLogCompose(
            thermalData = emptyList(),
            chartType = selectedType,
            modifier = Modifier.fillMaxWidth()
        )
    }
}