// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose' subtree
// Files: 25; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ChartLogCompose.kt =====

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
                text = "${chartType.displayName} â€¢ $dataCount points",
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
            "${temp.roundToInt()}Â°",
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ChartTrendCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun ChartTrendCompose(
    dataPoints: List<Float>,
    minValue: Float = 0f,
    maxValue: Float = 50f,
    title: String = "Temperature Trend",
    lineColor: Color = Color(0xFF2196F3),
    gridColor: Color = Color.Gray.copy(alpha = 0.3f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
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
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Chart area
            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    drawTrendChart(
                        dataPoints = dataPoints,
                        minValue = minValue,
                        maxValue = maxValue,
                        lineColor = lineColor,
                        gridColor = gridColor,
                        textColor = textColor
                    )
                }
            }
            // Legend/Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${dataPoints.size} points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "End",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawTrendChart(
    dataPoints: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    gridColor: Color,
    textColor: Color
) {
    if (dataPoints.isEmpty()) return
    val padding = 40.dp.toPx()
    val chartWidth = size.width - 2 * padding
    val chartHeight = size.height - 2 * padding
    val valueRange = maxValue - minValue
    if (valueRange <= 0) return
    // Draw grid lines
    val gridLineCount = 5
    for (i in 0..gridLineCount) {
        val y = padding + (chartHeight * i / gridLineCount)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(size.width - padding, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    // Draw vertical grid lines
    val verticalGridCount = 4
    for (i in 0..verticalGridCount) {
        val x = padding + (chartWidth * i / verticalGridCount)
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, size.height - padding),
            strokeWidth = 0.5.dp.toPx()
        )
    }
    // Draw Y-axis labels
    val textPaint = android.graphics.Paint().apply {
        color = textColor.value.toInt()
        textSize = 11.sp.toPx()
        isAntiAlias = true
    }
    for (i in 0..gridLineCount) {
        val y = padding + (chartHeight * i / gridLineCount)
        val value = maxValue - (valueRange * i / gridLineCount)
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.0f", value),
            5.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
    // Draw trend line
    if (dataPoints.size >= 2) {
        val path = Path()
        var isFirstPoint = true
        dataPoints.forEachIndexed { index, value ->
            val x =
                if (dataPoints.size > 1) padding + (chartWidth * index / (dataPoints.size - 1)) else padding + chartWidth / 2f
            val normalizedValue = ((value - minValue) / valueRange).coerceIn(0f, 1f)
            val y = padding + chartHeight - (normalizedValue * chartHeight)
            if (isFirstPoint) {
                path.moveTo(x, y)
                isFirstPoint = false
            } else {
                path.lineTo(x, y)
            }
            // Draw data points
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
        // Draw the trend line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun TemperatureTrendCompose(
    temperatures: List<Float>,
    unit: String = "Â°C",
    lowTemp: Float = 20f,
    highTemp: Float = 40f,
    modifier: Modifier = Modifier
) {
    val minTemp = temperatures.minOrNull() ?: 0f
    val maxTemp = temperatures.maxOrNull() ?: 50f
    val adjustedMin = (minTemp - 5).coerceAtLeast(0f)
    val adjustedMax = (maxTemp + 5).coerceAtMost(100f)
    ChartTrendCompose(
        dataPoints = temperatures,
        minValue = adjustedMin,
        maxValue = adjustedMax,
        title = "Temperature Trend ($unit)",
        lineColor = when {
            temperatures.lastOrNull() ?: 0f < lowTemp -> Color.Blue
            temperatures.lastOrNull() ?: 0f > highTemp -> Color.Red
            else -> Color.Green
        },
        modifier = modifier
    )
}

@Composable
fun ChartTrendComposePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sample temperature data
        val sampleTemps = listOf(22f, 24f, 26f, 25f, 28f, 30f, 27f, 29f, 31f, 28f)
        TemperatureTrendCompose(
            temperatures = sampleTemps,
            lowTemp = 25f,
            highTemp = 30f
        )
        // Empty state
        ChartTrendCompose(
            dataPoints = emptyList(),
            title = "No Data Example"
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\CompassCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompassCompose(
    bearing: Float,
    declination: Float = 0f,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val adjustedBearing = (bearing + declination) % 360f
    Card(
        modifier = modifier.size(size),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCompass(
                    bearing = adjustedBearing,
                    center = this.center,
                    radius = this.size.minDimension / 2f - 20.dp.toPx()
                )
            }
            // Bearing text
            Text(
                text = "${adjustedBearing.roundToInt()}Â°",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (size * 0.25f))
            )
            // Cardinal direction
            Text(
                text = getCardinalDirection(adjustedBearing),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.offset(y = (size * 0.35f))
            )
        }
    }
}

@Composable
fun LinearCompassCompose(
    bearing: Float,
    declination: Float = 0f,
    modifier: Modifier = Modifier
) {
    val adjustedBearing = (bearing + declination) % 360f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawLinearCompass(
                    bearing = adjustedBearing,
                    centerY = size.height / 2f
                )
            }
            // Center indicator
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(Color.Red)
                    .align(Alignment.Center)
            )
            // Bearing text
            Text(
                text = "${adjustedBearing.roundToInt()}Â° ${getCardinalDirection(adjustedBearing)}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )
        }
    }
}

private fun DrawScope.drawCompass(
    bearing: Float,
    center: Offset,
    radius: Float
) {
    // Draw compass circle
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    // Draw cardinal directions
    val cardinalDirections = listOf("N", "E", "S", "W")
    val cardinalAngles = listOf(0f, 90f, 180f, 270f)
    cardinalDirections.forEachIndexed { index, direction ->
        val angle = cardinalAngles[index]
        val textRadius = radius + 15.dp.toPx()
        val x = center.x + cos(Math.toRadians(angle - 90.0)).toFloat() * textRadius
        val y = center.y + sin(Math.toRadians(angle - 90.0)).toFloat() * textRadius
        val textPaint = android.graphics.Paint().apply {
            color = Color.White.value.toInt()
            textSize = 16.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            direction,
            x,
            y + 6.dp.toPx(),
            textPaint
        )
    }
    // Draw tick marks
    for (i in 0 until 36) {
        val angle = i * 10f
        val tickLength = if (i % 9 == 0) 20.dp.toPx() else 10.dp.toPx()
        val innerRadius = radius - tickLength
        val startX = center.x + cos(Math.toRadians(angle - 90.0)).toFloat() * innerRadius
        val startY = center.y + sin(Math.toRadians(angle - 90.0)).toFloat() * innerRadius
        val endX = center.x + cos(Math.toRadians(angle - 90.0)).toFloat() * radius
        val endY = center.y + sin(Math.toRadians(angle - 90.0)).toFloat() * radius
        drawLine(
            color = Color.White,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.dp.toPx()
        )
    }
    // Draw bearing needle
    rotate(bearing, center) {
        val needleLength = radius * 0.8f
        drawLine(
            color = Color.Red,
            start = center,
            end = Offset(center.x, center.y - needleLength),
            strokeWidth = 3.dp.toPx()
        )
        // Draw needle tip
        drawCircle(
            color = Color.Red,
            radius = 4.dp.toPx(),
            center = Offset(center.x, center.y - needleLength)
        )
    }
}

private fun DrawScope.drawLinearCompass(
    bearing: Float,
    centerY: Float
) {
    val width = size.width
    val tickSpacing = width / 12f // Show about 12 ticks across the width
    val degreesPerTick = 30f // Each tick represents 30 degrees
    // Calculate the offset to center the current bearing
    val centerBearing = bearing
    val startBearing = centerBearing - 180f
    for (i in -6..6) {
        val tickBearing = (startBearing + i * degreesPerTick + 360f) % 360f
        val x = width / 2f + i * tickSpacing
        if (x >= 0 && x <= width) {
            // Draw tick mark
            val tickHeight = if (tickBearing % 90f == 0f) 30.dp.toPx() else 15.dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(x, centerY - tickHeight / 2),
                end = Offset(x, centerY + tickHeight / 2),
                strokeWidth = 1.dp.toPx()
            )
            // Draw degree label for major ticks
            if (tickBearing % 90f == 0f) {
                val textPaint = android.graphics.Paint().apply {
                    color = Color.White.value.toInt()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val label = getCardinalDirection(tickBearing)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    centerY + 25.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

private fun getCardinalDirection(bearing: Float): String {
    val normalizedBearing = (bearing + 360f) % 360f
    return when {
        normalizedBearing < 22.5f || normalizedBearing >= 337.5f -> "N"
        normalizedBearing < 67.5f -> "NE"
        normalizedBearing < 112.5f -> "E"
        normalizedBearing < 157.5f -> "SE"
        normalizedBearing < 202.5f -> "S"
        normalizedBearing < 247.5f -> "SW"
        normalizedBearing < 292.5f -> "W"
        normalizedBearing < 337.5f -> "NW"
        else -> "N"
    }
}

@Composable
fun CompassStatusCompose(
    bearing: Float,
    quality: CompassQuality = CompassQuality.GOOD,
    isCalibrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Compass",
            tint = quality.color,
            modifier = Modifier
                .size(20.dp)
                .rotate(bearing)
        )
        Column {
            Text(
                text = "${bearing.roundToInt()}Â° ${getCardinalDirection(bearing)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCalibrating) "Calibrating..." else quality.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = quality.color
            )
        }
    }
}

enum class CompassQuality(
    val displayName: String,
    val color: Color
) {
    EXCELLENT("Excellent", Color.Green),
    GOOD("Good", Color.Green),
    FAIR("Fair", Color.Yellow),
    POOR("Poor", Color(0xFFFF6600)),
    UNRELIABLE("Unreliable", Color.Red)
}

@Composable
fun CompassComposePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var bearing by remember { mutableFloatStateOf(45f) }
        CompassCompose(
            bearing = bearing,
            size = 150.dp
        )
        LinearCompassCompose(
            bearing = bearing
        )
        CompassStatusCompose(
            bearing = bearing,
            quality = CompassQuality.GOOD
        )
        Slider(
            value = bearing,
            onValueChange = { bearing = it },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Bearing: ${bearing.roundToInt()}Â°",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ConfigGuideDialogCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.libunified.app.compose.theme.Spacing

@Composable
fun ConfigGuideDialogCompose(
    isTC007: Boolean,
    initialStep: Int = 1,
    onDismiss: () -> Unit,
    onComplete: () -> Unit = {}
) {
    var currentStep by remember(initialStep) { mutableIntStateOf(initialStep) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .blur(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (targetState > initialState) 300 else -300 }
                    ) + fadeIn() togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { if (targetState > initialState) -300 else 300 }
                            ) + fadeOut()
                },
                label = "config_step"
            ) { step ->
                when (step) {
                    1 -> ConfigStep1Content(
                        isTC007 = isTC007,
                        onNext = { currentStep = 2 }
                    )

                    2 -> ConfigStep2Content(
                        isTC007 = isTC007,
                        onComplete = {
                            onComplete()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigStep1Content(
    isTC007: Boolean,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.small),
        shape = RoundedCornerShape(Spacing.normal)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            Text(
                text = "Thermal Configuration Guide",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Step 1: Basic Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_environment)} (-10~${if (isTC007) 50 else 55}Â°C)",
                description = "Set the ambient temperature for accurate thermal measurements"
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)",
                description = "Configure the distance to target for proper calibration"
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTarget)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ConfigStep2Content(
    isTC007: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.small),
        shape = RoundedCornerShape(Spacing.normal)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            Text(
                text = "Step 2: Emissivity Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)",
                description = "Select appropriate emissivity value for your target material"
            )
            Text(
                text = "Common Materials",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(getEmissivityPresets(isTC007)) { preset ->
                    EmissivityPresetCard(preset = preset)
                }
            }
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTarget)
            ) {
                Text("I Know")
            }
        }
    }
}

@Composable
private fun ConfigParameterCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmissivityPresetCard(
    preset: EmissivityPreset,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = preset.material,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (preset.description.isNotEmpty()) {
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = preset.emissivity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun getEmissivityPresets(isTC007: Boolean): List<EmissivityPreset> {
    return listOf(
        EmissivityPreset("Human Skin", 0.98f, "Human body temperature measurement"),
        EmissivityPreset("Water", 0.96f, "Liquid water surface"),
        EmissivityPreset("Concrete", 0.95f, "Concrete surfaces and walls"),
        EmissivityPreset("Plastic", 0.94f, "Most plastic materials"),
        EmissivityPreset("Wood", 0.90f, "Dry wood surfaces"),
        EmissivityPreset("Paint", 0.90f, "Painted surfaces (non-metallic)"),
        EmissivityPreset("Brick", 0.85f, "Red brick and ceramic"),
        EmissivityPreset("Stainless Steel", 0.16f, "Polished stainless steel"),
        EmissivityPreset("Aluminum", if (isTC007) 0.1f else 0.05f, "Oxidized aluminum"),
        EmissivityPreset("Copper", 0.04f, "Polished copper surface")
    ).filter {
        it.emissivity >= (if (isTC007) 0.1f else 0.01f) && it.emissivity <= 1.0f
    }
}

data class EmissivityPreset(
    val material: String,
    val emissivity: Float,
    val description: String = ""
)

@Composable
fun ConfigGuideDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        ConfigGuideDialogCompose(
            isTC007 = false,
            initialStep = 1,
            onDismiss = { showDialog = false },
            onComplete = {
                println("Configuration guide completed")
                showDialog = false
            }
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\DistanceMeasureCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun DistanceMeasureCompose(
    onDistanceChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var line1Y by remember { mutableFloatStateOf(0f) }
    var line2Y by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var isInitialized by remember { mutableStateOf(false) }
    // Initialize line positions when canvas size is known
    LaunchedEffect(canvasHeight) {
        if (canvasHeight > 0 && !isInitialized) {
            val lineHeight = 50f
            val margin = (canvasHeight - lineHeight) / 2f
            line1Y = margin
            line2Y = margin + lineHeight
            isInitialized = true
            onDistanceChanged(lineHeight)
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newY = change.position.y.coerceIn(0f, canvasHeight)
                    // Determine which line is closer to touch point
                    if (abs(newY - line1Y) < abs(newY - line2Y)) {
                        // Moving line1
                        val difference = line1Y - newY
                        line1Y = newY
                        line2Y += difference
                    } else {
                        // Moving line2
                        val difference = newY - line2Y
                        line2Y = newY
                        line1Y -= difference
                    }
                    // Update distance
                    val distance = abs(line2Y - line1Y)
                    onDistanceChanged(distance)
                }
            }
    ) {
        canvasHeight = size.height
        if (isInitialized) {
            drawDistanceLines(line1Y, line2Y)
        }
    }
}

private fun DrawScope.drawDistanceLines(line1Y: Float, line2Y: Float) {
    val strokeWidth = 4.dp.toPx()
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val lineColor = Color.Green
    val margin = 50.dp.toPx()
    // Draw first dashed line
    drawLine(
        color = lineColor,
        start = Offset(margin, line1Y),
        end = Offset(size.width - margin, line1Y),
        strokeWidth = strokeWidth,
        pathEffect = dashEffect
    )
    // Draw second dashed line
    drawLine(
        color = lineColor,
        start = Offset(margin, line2Y),
        end = Offset(size.width - margin, line2Y),
        strokeWidth = strokeWidth,
        pathEffect = dashEffect
    )
}

@Composable
fun DistanceMeasureComposePreview() {
    var distance by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier.background(Color.Black)
    ) {
        DistanceMeasureCompose(
            onDistanceChanged = { distance = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        // Show distance value for testing
        androidx.compose.material3.Text(
            text = "Distance: ${distance.toInt()}px",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\EmissivityCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmissivityCompose(
    textList: List<String>,
    isAlignTop: Boolean = false,
    drawTopLine: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 0.5.dp.toPx() }
    val lineColor = Color(0xff5b5961)
    if (textList.isEmpty()) {
        return
    }
    Box(modifier = modifier) {
        // Background canvas for custom borders
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            // Draw top line if needed
            if (drawTopLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, strokeWidth / 2),
                    end = Offset(width, strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            // Draw bottom line
            drawLine(
                color = lineColor,
                start = Offset(0f, height - strokeWidth / 2),
                end = Offset(width, height - strokeWidth / 2),
                strokeWidth = strokeWidth
            )
            // Draw left line
            drawLine(
                color = lineColor,
                start = Offset(strokeWidth / 2, 0f),
                end = Offset(strokeWidth / 2, height),
                strokeWidth = strokeWidth
            )
            // Draw vertical separators
            if (textList.size > 1) {
                val firstColumnWidth = width * 135f / 335f
                val remainingWidth = width - firstColumnWidth
                val columnWidth = remainingWidth / 2f
                var x = firstColumnWidth
                repeat(textList.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x - strokeWidth / 2, 0f),
                        end = Offset(x - strokeWidth / 2, height),
                        strokeWidth = strokeWidth
                    )
                    x += columnWidth
                }
            }
        }
        // Content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = if (isAlignTop) Alignment.Top else Alignment.CenterVertically
        ) {
            textList.forEachIndexed { index, text ->
                val weight = if (textList.size == 1) {
                    1f
                } else {
                    if (index == 0) 135f / 335f else (200f / 335f) / 2f
                }
                Text(
                    text = text,
                    modifier = Modifier
                        .weight(weight)
                        .padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center,
                    fontSize = if (textList.size == 1) 12.sp else 11.sp,
                    color = if (textList.size == 1) Color.White else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun EmissivityComposePreview() {
    Column(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Single item
        EmissivityCompose(
            textList = listOf("Single Value: 0.95"),
            modifier = Modifier.height(40.dp)
        )
        // Multiple items
        EmissivityCompose(
            textList = listOf("Label", "Value 1", "Value 2"),
            drawTopLine = true,
            modifier = Modifier.height(40.dp)
        )
        // Aligned top
        EmissivityCompose(
            textList = listOf("Long Label Text", "Short", "Medium Value"),
            isAlignTop = true,
            modifier = Modifier.height(60.dp)
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\HikSurfaceCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.sin

@Composable
fun HikSurfaceCompose(
    thermalImageData: ByteArray? = null,
    rotateAngle: Int = 270,
    isOpenAmplify: Boolean = false,
    limitTempMin: Float = Float.MIN_VALUE,
    limitTempMax: Float = Float.MAX_VALUE,
    alarmSettings: ThermalAlarmSettings = ThermalAlarmSettings(),
    onTemperatureAlarm: (Float, ThermalAlarmType) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Calculate dimensions based on rotation and amplification
    val baseDimensions = getThermalDimensions(rotateAngle, isOpenAmplify)
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thermal image surface
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
            ) {
                drawThermalSurface(
                    thermalData = thermalImageData,
                    rotateAngle = rotateAngle,
                    isAmplified = isOpenAmplify,
                    dimensions = baseDimensions,
                    alarmSettings = alarmSettings
                )
            }
            // Overlay controls and indicators
            ThermalOverlayControls(
                rotateAngle = rotateAngle,
                isAmplified = isOpenAmplify,
                alarmSettings = alarmSettings,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            // Temperature range indicator
            if (limitTempMin != Float.MIN_VALUE || limitTempMax != Float.MAX_VALUE) {
                TemperatureRangeIndicator(
                    minTemp = if (limitTempMin != Float.MIN_VALUE) limitTempMin else null,
                    maxTemp = if (limitTempMax != Float.MAX_VALUE) limitTempMax else null,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun ThermalOverlayControls(
    rotateAngle: Int,
    isAmplified: Boolean,
    alarmSettings: ThermalAlarmSettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Rotation indicator
        if (rotateAngle != 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotation",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${rotateAngle}Â°",
                        fontSize = 10.sp
                    )
                }
            }
        }
        // Amplification indicator
        if (isAmplified) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Amplified",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "2x",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        // Alarm indicator
        if (alarmSettings.isEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (alarmSettings.isAlarmActive) {
                        Color.Red.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    }
                )
            ) {
                Icon(
                    imageVector = if (alarmSettings.isAlarmActive) Icons.Default.Warning else Icons.Default.NotificationsActive,
                    contentDescription = "Thermal Alarm",
                    modifier = Modifier
                        .padding(4.dp)
                        .size(12.dp),
                    tint = if (alarmSettings.isAlarmActive) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TemperatureRangeIndicator(
    minTemp: Float?,
    maxTemp: Float?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Range",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            minTemp?.let { temp ->
                Text(
                    text = "Min: ${temp.toInt()}Â°C",
                    fontSize = 9.sp,
                    color = Color.Blue
                )
            }
            maxTemp?.let { temp ->
                Text(
                    text = "Max: ${temp.toInt()}Â°C",
                    fontSize = 9.sp,
                    color = Color.Red
                )
            }
        }
    }
}

private fun DrawScope.drawThermalSurface(
    thermalData: ByteArray?,
    rotateAngle: Int,
    isAmplified: Boolean,
    dimensions: ThermalDimensions,
    alarmSettings: ThermalAlarmSettings
) {
    // Draw background
    drawRect(
        color = Color.Black,
        size = size
    )
    if (thermalData == null) {
        // Draw placeholder thermal pattern
        drawPlaceholderThermalImage(dimensions, rotateAngle, isAmplified)
    } else {
        // Draw actual thermal data (simplified representation)
        drawThermalData(thermalData, dimensions, rotateAngle, isAmplified)
    }
    // Draw alarm overlay if active
    if (alarmSettings.isEnabled && alarmSettings.isAlarmActive) {
        drawAlarmOverlay(alarmSettings)
    }
}

private fun DrawScope.drawPlaceholderThermalImage(
    dimensions: ThermalDimensions,
    rotateAngle: Int,
    isAmplified: Boolean
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val scale = if (isAmplified) 2f else 1f
    rotate(rotateAngle.toFloat(), pivot = Offset(centerX, centerY)) {
        // Create a gradient thermal pattern
        val gradientBrush = Brush.radialGradient(
            colors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Blue
            ),
            center = Offset(centerX, centerY),
            radius = kotlin.math.min(size.width, size.height) / 4f * scale
        )
        drawRect(
            brush = gradientBrush,
            topLeft = Offset(
                centerX - dimensions.width * scale / 2f,
                centerY - dimensions.height * scale / 2f
            ),
            size = Size(
                dimensions.width * scale,
                dimensions.height * scale
            )
        )
        // Add some thermal "hotspots"
        repeat(5) { i ->
            val hotspotX = centerX + (kotlin.math.cos(i * 1.2) * 30 * scale).toFloat()
            val hotspotY = centerY + (kotlin.math.sin(i * 1.2) * 30 * scale).toFloat()
            drawCircle(
                color = Color.Red.copy(alpha = 0.6f),
                radius = 15f * scale,
                center = Offset(hotspotX, hotspotY)
            )
        }
    }
}

private fun DrawScope.drawThermalData(
    thermalData: ByteArray,
    dimensions: ThermalDimensions,
    rotateAngle: Int,
    isAmplified: Boolean
) {
    // Simplified thermal data rendering
    // In a real implementation, this would process the thermal byte array
    // and convert it to temperature values and corresponding colors
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val scale = if (isAmplified) 2f else 1f
    rotate(rotateAngle.toFloat(), pivot = Offset(centerX, centerY)) {
        // Simulate thermal data visualization
        val pixelSize = 2f * scale
        val dataWidth = dimensions.width.toInt()
        val dataHeight = dimensions.height.toInt()
        for (y in 0 until dataHeight step 4) {
            for (x in 0 until dataWidth step 4) {
                // Simulate temperature value from data
                val dataIndex = (y * dataWidth + x) * 2 // 2 bytes per pixel
                if (dataIndex + 1 < thermalData.size) {
                    val tempValue = ((thermalData[dataIndex].toInt() and 0xFF) or
                            ((thermalData[dataIndex + 1].toInt() and 0xFF) shl 8))
                    // Convert to color (simplified)
                    val normalizedTemp = (tempValue % 256) / 255f
                    val color = Color.hsv(
                        hue = (1f - normalizedTemp) * 240f, // Blue to red
                        saturation = 1f,
                        value = 1f
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(
                            centerX - dataWidth * scale / 2f + x * scale,
                            centerY - dataHeight * scale / 2f + y * scale
                        ),
                        size = Size(pixelSize * 4, pixelSize * 4)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAlarmOverlay(alarmSettings: ThermalAlarmSettings) {
    // Draw pulsing red overlay for alarm
    val alpha = (sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.4).toFloat()
    drawRect(
        color = Color.Red.copy(alpha = alpha.coerceIn(0.1f, 0.7f)),
        size = size
    )
    // Draw alarm border
    drawRect(
        color = Color.Red,
        size = size,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
    )
}

private fun getThermalDimensions(rotateAngle: Int, isAmplified: Boolean): ThermalDimensions {
    val multiplier = if (isAmplified) 2 else 1
    val isPortrait = rotateAngle == 90 || rotateAngle == 270
    return ThermalDimensions(
        width = (if (isPortrait) 192 else 256) * multiplier,
        height = (if (isPortrait) 256 else 192) * multiplier
    )
}

// Data classes
data class ThermalDimensions(
    val width: Int,
    val height: Int
)

data class ThermalAlarmSettings(
    val isEnabled: Boolean = false,
    val isAlarmActive: Boolean = false,
    val alarmType: ThermalAlarmType = ThermalAlarmType.HIGH_TEMPERATURE,
    val threshold: Float = 50f
)

enum class ThermalAlarmType {
    HIGH_TEMPERATURE,
    LOW_TEMPERATURE,
    TEMPERATURE_RANGE
}

@Composable
fun HikSurfaceWithAndroidView(
    rotateAngle: Int = 270,
    isOpenAmplify: Boolean = false,
    onSurfaceReady: (android.view.SurfaceView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // In a real implementation, this would create the actual HikSurfaceView
            android.view.SurfaceView(context).apply {
                // Configure the surface view
                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                        onSurfaceReady(this@apply)
                    }

                    override fun surfaceChanged(
                        holder: android.view.SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
                })
            }
        },
        update = { surfaceView ->
            // Update surface view properties
            // In real implementation, would update rotation, amplification, etc.
        }
    )
}

@Composable
fun HikSurfaceComposePreview() {
    var rotateAngle by remember { mutableIntStateOf(270) }
    var isAmplified by remember { mutableStateOf(false) }
    var alarmActive by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Surface Display",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    rotateAngle = (rotateAngle + 90) % 360
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate")
                Text("Rotate")
            }
            Button(
                onClick = { isAmplified = !isAmplified }
            ) {
                Icon(
                    if (isAmplified) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                    contentDescription = if (isAmplified) "Zoom Out" else "Zoom In"
                )
                Text(if (isAmplified) "1x" else "2x")
            }
            Button(
                onClick = { alarmActive = !alarmActive },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (alarmActive) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Alarm")
                Text("Alarm")
            }
        }
        // Thermal surface
        HikSurfaceCompose(
            rotateAngle = rotateAngle,
            isOpenAmplify = isAmplified,
            limitTempMin = 20f,
            limitTempMax = 80f,
            alarmSettings = ThermalAlarmSettings(
                isEnabled = true,
                isAlarmActive = alarmActive,
                threshold = 50f
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        // Status display
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Surface Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rotation: ${rotateAngle}Â°")
                Text("Amplification: ${if (isAmplified) "2x" else "1x"}")
                Text("Alarm: ${if (alarmActive) "ACTIVE" else "Inactive"}")
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\HomeGuideDialogCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.compose.utils.deferAction

@Composable
fun HomeGuideDialogCompose(
    initialStep: Int = 1,
    onNextStep: (step: Int) -> Unit = {},
    onSkinClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentStep by remember(initialStep) { mutableIntStateOf(initialStep) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .blur(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (targetState > initialState) 300 else -300 }
                    ) + fadeIn() togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { if (targetState > initialState) -300 else 300 }
                            ) + fadeOut()
                },
                label = "guide_step"
            ) { step ->
                GuideStepContent(
                    step = step,
                    onNext = {
                        when (step) {
                            1 -> {
                                currentStep = 2
                                onNextStep(1)
                            }

                            2 -> {
                                currentStep = 3
                                onNextStep(2)
                            }

                            3 -> {
                                onNextStep(3)
                                onDismiss()
                            }
                        }
                    },
                    onSkinClick = {
                        onSkinClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun GuideStepContent(
    step: Int,
    onNext: () -> Unit,
    onSkinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> GuideStep1Content(onNext, onSkinClick)
                2 -> GuideStep2Content(onNext, onSkinClick)
                3 -> GuideStep3Content(onNext)
            }
        }
    }
}

@Composable
private fun GuideStep1Content(onNext: () -> Unit, onSkinClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 1: Getting Started",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Welcome to the thermal camera guide. This will help you get started with thermal imaging.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = deferAction { onSkinClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skin Detection")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun GuideStep2Content(onNext: () -> Unit, onSkinClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 2: Camera Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Position the camera properly and adjust the focus for optimal thermal imaging results.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = deferAction { onSkinClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skin Detection")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun GuideStep3Content(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 3: Ready to Go!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You're all set! Start using the thermal camera to capture and analyze thermal images.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = deferAction { onNext() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I Know")
        }
    }
}

@Composable
fun HomeGuideDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        HomeGuideDialogCompose(
            initialStep = 1,
            onNextStep = { step ->
                println("Guide step: $step")
            },
            onSkinClick = {
                println("Skin detection clicked")
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\IRConfigInputDialogCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.R

@Composable
fun IRConfigInputDialogCompose(
    type: IRConfigInputType,
    isTC007: Boolean,
    initialValue: Float? = null,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialValue?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    val dialogData = remember(type, isTC007) {
        when (type) {
            IRConfigInputType.TEMP -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_environment)} ${
                    UnitTools.showConfigC(-10, if (isTC007) 50 else 55)
                }",
                unit = UnitTools.showUnit(),
                showUnit = true,
                validator = { value ->
                    value in UnitTools.showUnitValue(-10f)..UnitTools.showUnitValue(
                        if (isTC007) 50f else 55f
                    )
                }
            )

            IRConfigInputType.DIS -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)",
                unit = "m",
                showUnit = true,
                validator = { value -> value in 0.2f..(if (isTC007) 4f else 5f) }
            )

            IRConfigInputType.EM -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)",
                unit = "",
                showUnit = false,
                validator = { value -> value in (if (isTC007) 0.1f else 0.01f)..1f }
            )
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.73f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = dialogData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Input field with unit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                handleConfirm(
                                    inputText,
                                    dialogData.validator,
                                    context,
                                    onConfirm,
                                    onDismiss
                                )
                            }
                        ),
                        singleLine = true
                    )
                    if (dialogData.showUnit) {
                        Text(
                            text = dialogData.unit,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            handleConfirm(
                                inputText,
                                dialogData.validator,
                                context,
                                onConfirm,
                                onDismiss
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

private fun handleConfirm(
    inputText: String,
    validator: (Float) -> Boolean,
    context: android.content.Context,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val input = inputText.toFloat()
        if (validator(input)) {
            onConfirm(input)
            onDismiss()
        } else {
            TToast.shortToast(context, R.string.tip_input_format)
        }
    } catch (e: NumberFormatException) {
        TToast.shortToast(context, R.string.tip_input_format)
    }
}

enum class IRConfigInputType {
    TEMP, DIS, EM
}

private data class IRConfigDialogData(
    val title: String,
    val unit: String,
    val showUnit: Boolean,
    val validator: (Float) -> Boolean
)


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\LayoutComponentsCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonitorControlPanel(
    onLogQuery: () -> Unit = {},
    onCreateChart: () -> Unit = {},
    onStartMonitoring: () -> Unit = {},
    isMonitoring: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onLogQuery,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Query Log", fontSize = 14.sp)
            }
            Button(
                onClick = onCreateChart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Chart", fontSize = 14.sp)
            }
            Button(
                onClick = onStartMonitoring,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color.Red else MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMonitoring) "Stop" else "Start", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ChartInfoPanel(
    currentValue: String,
    maxValue: String,
    minValue: String,
    averageValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChartInfoItem(
                label = "Current",
                value = currentValue,
                color = Color.Red,
                icon = Icons.Default.FiberManualRecord
            )
            ChartInfoItem(
                label = "Max",
                value = maxValue,
                color = Color.Green,
                icon = Icons.Default.KeyboardArrowUp
            )
            ChartInfoItem(
                label = "Min",
                value = minValue,
                color = Color.Blue,
                icon = Icons.Default.KeyboardArrowDown
            )
            ChartInfoItem(
                label = "Avg",
                value = averageValue,
                color = Color(0xFFFF6600),
                icon = Icons.Default.Timeline
            )
        }
    }
}

@Composable
private fun ChartInfoItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ReportInfoSection(
    reportName: String,
    deviceModel: String,
    timestamp: String,
    temperature: String,
    humidity: String,
    onEditReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditReport) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Report"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ReportInfoRow("Report Name", reportName)
            ReportInfoRow("Device Model", deviceModel)
            ReportInfoRow("Timestamp", timestamp)
            ReportInfoRow("Temperature", temperature)
            ReportInfoRow("Humidity", humidity)
        }
    }
}

@Composable
private fun ReportInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TargetModeItem(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ConfigurationItem(
    title: String,
    currentValue: String,
    unit: String = "",
    range: String = "",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (range.isNotEmpty()) {
                        Text(
                            text = range,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Configure",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateComponent(
    title: String = "No Data Available",
    description: String = "There's nothing to show here yet",
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onActionClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun LayoutComponentsPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MonitorControlPanel(
            isMonitoring = false
        )
        ChartInfoPanel(
            currentValue = "25.4Â°C",
            maxValue = "28.1Â°C",
            minValue = "22.3Â°C",
            averageValue = "25.0Â°C"
        )
        TargetModeItem(
            title = "Point Measurement",
            description = "Measure temperature at a specific point",
            icon = Icons.Default.Place,
            isSelected = true
        )
        ConfigurationItem(
            title = "Environment Temperature",
            currentValue = "25.0",
            unit = "Â°C",
            range = "(-10~55Â°C)"
        )
        EmptyStateComponent(
            title = "No measurements",
            description = "Start by taking some temperature measurements",
            actionText = "Start Measuring"
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\TargetBarPickCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun TargetBarPickCompose(
    progress: Int,
    min: Int = 0,
    max: Int = 100,
    barSize: Int = 200,
    isVertical: Boolean = false,
    label: String = "",
    onProgressChanged: (progress: Int, max: Int) -> Unit = { _, _ -> },
    onStartTrackingTouch: (progress: Int, max: Int) -> Unit = { _, _ -> },
    onStopTrackingTouch: (progress: Int, max: Int) -> Unit = { _, _ -> },
    valueFormatter: (progress: Int) -> String = { it.toString() },
    progressColor: Color = Color.White,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var currentProgress by remember(progress) { mutableIntStateOf(progress.coerceIn(min, max)) }
    val barSizeDp = with(density) { barSize.toDp() }
    val thumbSize = 24.dp
    val trackHeight = 8.dp
    Box(
        modifier = modifier
            .then(
                if (isVertical) {
                    Modifier
                        .height(barSizeDp)
                        .width(thumbSize + 16.dp)
                } else {
                    Modifier
                        .width(barSizeDp)
                        .height(thumbSize + 16.dp)
                }
            )
            .pointerInput(min, max) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newProgress = calculateProgressFromOffset(
                            offset = offset,
                            size = Size(size.width.toFloat(), size.height.toFloat()),
                            isVertical = isVertical,
                            min = min,
                            max = max,
                            thumbSize = thumbSize.toPx()
                        )
                        currentProgress = newProgress
                        onStartTrackingTouch(newProgress, max)
                        onProgressChanged(newProgress, max)
                    },
                    onDrag = { _, dragAmount ->
                        val totalSize = if (isVertical) size.height else size.width
                        val thumbSizePx = thumbSize.toPx()
                        val availableSize = totalSize - thumbSizePx
                        val progressChange = if (isVertical) {
                            -dragAmount.y / availableSize * (max - min)
                        } else {
                            dragAmount.x / availableSize * (max - min)
                        }
                        val newProgress = (currentProgress + progressChange).roundToInt()
                            .coerceIn(min, max)
                        if (newProgress != currentProgress) {
                            currentProgress = newProgress
                            onProgressChanged(newProgress, max)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        onStopTrackingTouch(currentProgress, max)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw track and thumb
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawTargetBar(
                progress = currentProgress,
                min = min,
                max = max,
                isVertical = isVertical,
                trackHeight = trackHeight.toPx(),
                thumbSize = thumbSize.toPx(),
                progressColor = progressColor,
                backgroundColor = backgroundColor,
                isDragging = isDragging
            )
        }
        // Value display
        if (label.isNotEmpty() || valueFormatter(currentProgress).isNotEmpty()) {
            val displayText = if (label.isNotEmpty()) {
                "$label: ${valueFormatter(currentProgress)}"
            } else {
                valueFormatter(currentProgress)
            }
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .then(
                        if (isVertical) {
                            Modifier.offset(x = 40.dp)
                        } else {
                            Modifier.offset(y = (-40).dp)
                        }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun DrawScope.drawTargetBar(
    progress: Int,
    min: Int,
    max: Int,
    isVertical: Boolean,
    trackHeight: Float,
    thumbSize: Float,
    progressColor: Color,
    backgroundColor: Color,
    isDragging: Boolean
) {
    val totalRange = max - min
    if (totalRange <= 0) return
    val progressRatio = (progress - min).toFloat() / totalRange
    if (isVertical) {
        drawVerticalBar(
            progressRatio = progressRatio,
            trackHeight = trackHeight,
            thumbSize = thumbSize,
            progressColor = progressColor,
            backgroundColor = backgroundColor,
            isDragging = isDragging
        )
    } else {
        drawHorizontalBar(
            progressRatio = progressRatio,
            trackHeight = trackHeight,
            thumbSize = thumbSize,
            progressColor = progressColor,
            backgroundColor = backgroundColor,
            isDragging = isDragging
        )
    }
}

private fun DrawScope.drawHorizontalBar(
    progressRatio: Float,
    trackHeight: Float,
    thumbSize: Float,
    progressColor: Color,
    backgroundColor: Color,
    isDragging: Boolean
) {
    val centerY = size.height / 2f
    val trackTop = centerY - trackHeight / 2f
    val trackBottom = centerY + trackHeight / 2f
    val availableWidth = size.width - thumbSize
    val thumbCenterX = thumbSize / 2f + availableWidth * progressRatio
    // Draw track background
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(thumbSize / 2f, trackTop),
        size = Size(availableWidth, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2f)
    )
    // Draw progress track
    if (progressRatio > 0) {
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(thumbSize / 2f, trackTop),
            size = Size(availableWidth * progressRatio, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )
    }
    // Draw thumb
    val thumbRadius = thumbSize / 2f
    val thumbColor = if (isDragging) progressColor.copy(alpha = 0.8f) else progressColor
    drawCircle(
        color = thumbColor,
        radius = thumbRadius,
        center = Offset(thumbCenterX, centerY)
    )
    // Draw thumb border
    drawCircle(
        color = Color.White,
        radius = thumbRadius,
        center = Offset(thumbCenterX, centerY),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawVerticalBar(
    progressRatio: Float,
    trackHeight: Float,
    thumbSize: Float,
    progressColor: Color,
    backgroundColor: Color,
    isDragging: Boolean
) {
    val centerX = size.width / 2f
    val trackLeft = centerX - trackHeight / 2f
    val trackRight = centerX + trackHeight / 2f
    val availableHeight = size.height - thumbSize
    // Invert for vertical - progress goes from bottom to top
    val thumbCenterY = size.height - (thumbSize / 2f + availableHeight * progressRatio)
    // Draw track background
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(trackLeft, thumbSize / 2f),
        size = Size(trackHeight, availableHeight),
        cornerRadius = CornerRadius(trackHeight / 2f)
    )
    // Draw progress track (from bottom up)
    if (progressRatio > 0) {
        val progressHeight = availableHeight * progressRatio
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(trackLeft, size.height - thumbSize / 2f - progressHeight),
            size = Size(trackHeight, progressHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )
    }
    // Draw thumb
    val thumbRadius = thumbSize / 2f
    val thumbColor = if (isDragging) progressColor.copy(alpha = 0.8f) else progressColor
    drawCircle(
        color = thumbColor,
        radius = thumbRadius,
        center = Offset(centerX, thumbCenterY)
    )
    // Draw thumb border
    drawCircle(
        color = Color.White,
        radius = thumbRadius,
        center = Offset(centerX, thumbCenterY),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun calculateProgressFromOffset(
    offset: Offset,
    size: androidx.compose.ui.geometry.Size,
    isVertical: Boolean,
    min: Int,
    max: Int,
    thumbSize: Float
): Int {
    val totalRange = max - min
    if (totalRange <= 0) return min
    val ratio = if (isVertical) {
        val availableHeight = size.height - thumbSize
        val adjustedY = size.height - offset.y - thumbSize / 2f
        (adjustedY / availableHeight).coerceIn(0f, 1f)
    } else {
        val availableWidth = size.width - thumbSize
        val adjustedX = offset.x - thumbSize / 2f
        (adjustedX / availableWidth).coerceIn(0f, 1f)
    }
    return (min + ratio * totalRange).roundToInt().coerceIn(min, max)
}

@Composable
fun SimpleTargetBarCompose(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    label: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TargetBarPickComposePreview() {
    var horizontalProgress by remember { mutableIntStateOf(50) }
    var verticalProgress by remember { mutableIntStateOf(75) }
    var sliderValue by remember { mutableFloatStateOf(25f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "Target Bar Components",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Horizontal bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Horizontal Target Bar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TargetBarPickCompose(
                progress = horizontalProgress,
                min = 0,
                max = 100,
                barSize = 300,
                isVertical = false,
                label = "Temperature",
                onProgressChanged = { progress, _ ->
                    horizontalProgress = progress
                },
                valueFormatter = { "$itÂ°C" }
            )
        }
        // Vertical bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vertical",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TargetBarPickCompose(
                    progress = verticalProgress,
                    min = 0,
                    max = 100,
                    barSize = 200,
                    isVertical = true,
                    label = "Level",
                    onProgressChanged = { progress, _ ->
                        verticalProgress = progress
                    },
                    valueFormatter = { "$it%" },
                    progressColor = Color.Green
                )
            }
            // Simple slider for comparison
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Simple Slider",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SimpleTargetBarCompose(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    label = "Opacity",
                    modifier = Modifier.width(200.dp)
                )
            }
        }
        // Display values
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Values",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Horizontal: $horizontalProgressÂ°C")
                Text("Vertical: $verticalProgress%")
                Text("Slider: ${String.format("%.1f", sliderValue)}")
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\Temperature07Compose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Temperature07Compose(
    mode: TemperatureMeasurementMode = TemperatureMeasurementMode.POINT,
    onMeasurement: (TemperatureMeasurement) -> Unit = {},
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isTouching by remember { mutableStateOf(false) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }
    var currentLine by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var currentRect by remember { mutableStateOf<Rect?>(null) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(mode, isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        isTouching = true
                        startPoint = offset
                        when (mode) {
                            TemperatureMeasurementMode.POINT -> {
                                currentPoint = offset
                                onMeasurement(
                                    TemperatureMeasurement.Point(
                                        position = offset,
                                        temperature = 25.0f
                                    )
                                )
                            }

                            TemperatureMeasurementMode.LINE -> {
                                // For line, we need start and end points
                                // This is simplified - in real implementation would need drag handling
                                currentLine = Pair(offset, offset)
                            }

                            TemperatureMeasurementMode.RECT -> {
                                // For rect, we need drag to define rectangle
                                currentRect = Rect(offset, offset)
                            }

                            TemperatureMeasurementMode.TREND -> {
                                // Trend mode - could accumulate points over time
                            }
                        }
                        tryAwaitRelease()
                        isTouching = false
                    }
                )
            }
    ) {
        // Overlay canvas for drawing measurements
        if (isTouching && isEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (mode) {
                    TemperatureMeasurementMode.POINT -> {
                        currentPoint?.let { point ->
                            drawMeasurementPoint(point)
                        }
                    }

                    TemperatureMeasurementMode.LINE -> {
                        currentLine?.let { (start, end) ->
                            drawMeasurementLine(start, end)
                        }
                    }

                    TemperatureMeasurementMode.RECT -> {
                        currentRect?.let { rect ->
                            drawMeasurementRect(rect)
                        }
                    }

                    TemperatureMeasurementMode.TREND -> {
                        // Draw trend indicators
                    }
                }
            }
        }
        // Mode indicator
        MeasurementModeIndicator(
            mode = mode,
            isActive = isTouching,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

private fun DrawScope.drawMeasurementPoint(point: Offset) {
    // Draw crosshair
    val crosshairSize = 20.dp.toPx()
    drawLine(
        color = Color.Red,
        start = Offset(point.x - crosshairSize, point.y),
        end = Offset(point.x + crosshairSize, point.y),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = Color.Red,
        start = Offset(point.x, point.y - crosshairSize),
        end = Offset(point.x, point.y + crosshairSize),
        strokeWidth = 2.dp.toPx()
    )
    // Draw center point
    drawCircle(
        color = Color.Red,
        radius = 4.dp.toPx(),
        center = point
    )
}

private fun DrawScope.drawMeasurementLine(start: Offset, end: Offset) {
    drawLine(
        color = Color.Green,
        start = start,
        end = end,
        strokeWidth = 3.dp.toPx()
    )
    // Draw endpoints
    drawCircle(color = Color.Green, radius = 6.dp.toPx(), center = start)
    drawCircle(color = Color.Green, radius = 6.dp.toPx(), center = end)
}

private fun DrawScope.drawMeasurementRect(rect: Rect) {
    drawRect(
        color = Color.Blue,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = 3.dp.toPx())
    )
    // Draw corner indicators
    val cornerSize = 8.dp.toPx()
    listOf(
        rect.topLeft,
        Offset(rect.right, rect.top),
        rect.bottomRight,
        Offset(rect.left, rect.bottom)
    ).forEach { corner ->
        drawCircle(
            color = Color.Blue,
            radius = cornerSize / 2,
            center = corner
        )
    }
}

@Composable
private fun MeasurementModeIndicator(
    mode: TemperatureMeasurementMode,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = mode.displayName,
                modifier = Modifier.size(16.dp),
                tint = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
fun TemperatureModeSelector(
    selectedMode: TemperatureMeasurementMode,
    onModeSelected: (TemperatureMeasurementMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TemperatureMeasurementMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = {
                        Text(
                            text = mode.displayName,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.displayName,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

// Data classes and enums
enum class TemperatureMeasurementMode(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    POINT("Point", Icons.Default.Place),
    LINE("Line", Icons.Default.Timeline),
    RECT("Rectangle", Icons.Default.CropFree),
    TREND("Trend", Icons.AutoMirrored.Filled.TrendingUp)
}

sealed class TemperatureMeasurement {
    data class Point(
        val position: Offset,
        val temperature: Float
    ) : TemperatureMeasurement()

    data class Line(
        val start: Offset,
        val end: Offset,
        val averageTemperature: Float,
        val minTemperature: Float,
        val maxTemperature: Float
    ) : TemperatureMeasurement()

    data class Rectangle(
        val rect: Rect,
        val averageTemperature: Float,
        val minTemperature: Float,
        val maxTemperature: Float
    ) : TemperatureMeasurement()

    data class Trend(
        val points: List<Offset>,
        val temperatures: List<Float>
    ) : TemperatureMeasurement()
}

@Composable
fun Temperature07ComposePreview() {
    var selectedMode by remember { mutableStateOf(TemperatureMeasurementMode.POINT) }
    var lastMeasurement by remember { mutableStateOf<TemperatureMeasurement?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        TemperatureModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Temperature07Compose(
                mode = selectedMode,
                onMeasurement = { measurement ->
                    lastMeasurement = measurement
                },
                isEnabled = true,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "Thermal Camera View\n(Tap to measure)",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Show last measurement
        lastMeasurement?.let { measurement ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Last Measurement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    when (measurement) {
                        is TemperatureMeasurement.Point -> {
                            Text("Point: ${measurement.temperature}Â°C at (${measurement.position.x.toInt()}, ${measurement.position.y.toInt()})")
                        }

                        is TemperatureMeasurement.Line -> {
                            Text("Line: Avg ${measurement.averageTemperature}Â°C, Range ${measurement.minTemperature}Â°C - ${measurement.maxTemperature}Â°C")
                        }

                        is TemperatureMeasurement.Rectangle -> {
                            Text("Rect: Avg ${measurement.averageTemperature}Â°C, Range ${measurement.minTemperature}Â°C - ${measurement.maxTemperature}Â°C")
                        }

                        is TemperatureMeasurement.Trend -> {
                            Text("Trend: ${measurement.points.size} points")
                        }
                    }
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\TemperatureEditCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.roundToInt

@Composable
fun TemperatureEditCompose(
    mode: TemperatureMode = TemperatureMode.POINT,
    isShowName: Boolean = true,
    onMeasurementAdded: (TemperatureMeasurementResult) -> Unit = {},
    onMeasurementCleared: () -> Unit = {},
    measurements: List<TemperatureMeasurementResult> = emptyList(),
    modifier: Modifier = Modifier
) {
    var currentMeasurements by remember { mutableStateOf(measurements) }
    var isDrawing by remember { mutableStateOf(false) }
    var drawStart by remember { mutableStateOf<Offset?>(null) }
    var drawEnd by remember { mutableStateOf<Offset?>(null) }
    // Update measurements when prop changes
    LaunchedEffect(measurements) {
        currentMeasurements = measurements
    }
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Main measurement area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.1f))
                .pointerInput(mode) {
                    detectTapGestures(
                        onPress = { offset ->
                            when (mode) {
                                TemperatureMode.POINT -> {
                                    val temp = simulateTemperatureReading(offset)
                                    val measurement = TemperatureMeasurementResult.Point(
                                        pointId = java.util.UUID.randomUUID().toString(),
                                        position = offset,
                                        temperature = temp
                                    )
                                    onMeasurementAdded(measurement)
                                }

                                TemperatureMode.LINE -> {
                                    if (drawStart == null) {
                                        drawStart = offset
                                        isDrawing = true
                                    } else {
                                        drawEnd = offset
                                        val temp1 = simulateTemperatureReading(drawStart!!)
                                        val temp2 = simulateTemperatureReading(offset)
                                        val measurement = TemperatureMeasurementResult.Line(
                                            lineId = UUID.randomUUID().toString(),
                                            start = drawStart!!,
                                            end = offset,
                                            temperatureMax = maxOf(temp1, temp2),
                                            temperatureMin = minOf(temp1, temp2),
                                            temperatureAvg = (temp1 + temp2) / 2f
                                        )
                                        onMeasurementAdded(measurement)
                                        drawStart = null
                                        drawEnd = null
                                        isDrawing = false
                                    }
                                }

                                TemperatureMode.RECTANGLE -> {
                                    if (drawStart == null) {
                                        drawStart = offset
                                        isDrawing = true
                                    } else {
                                        val rect = Rect(drawStart!!, offset)
                                        val temp = simulateAreaTemperatureReading(rect)
                                        val measurement = TemperatureMeasurementResult.Rectangle(
                                            rectId = UUID.randomUUID().toString(),
                                            rect = rect,
                                            temperatureMax = temp + 5f,
                                            temperatureMin = temp - 5f,
                                            temperatureAvg = temp
                                        )
                                        onMeasurementAdded(measurement)
                                        drawStart = null
                                        isDrawing = false
                                    }
                                }

                                TemperatureMode.CLEAR -> {
                                    onMeasurementCleared()
                                }
                            }
                        }
                    )
                }
        ) {
            // Thermal camera overlay (placeholder)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw thermal background pattern
                drawThermalBackground()
                // Draw existing measurements
                currentMeasurements.forEach { measurement ->
                    drawMeasurement(measurement, isShowName)
                }
                // Draw current drawing operation
                if (isDrawing && drawStart != null) {
                    when (mode) {
                        TemperatureMode.LINE -> {
                            drawEnd?.let { end ->
                                drawLine(
                                    color = Color.Yellow,
                                    start = drawStart!!,
                                    end = end,
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }

                        TemperatureMode.RECTANGLE -> {
                            drawEnd?.let { end ->
                                val rect = Rect(drawStart!!, end)
                                drawRect(
                                    color = Color.Yellow,
                                    topLeft = rect.topLeft,
                                    size = rect.size,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
            // Mode indicator
            TemperatureModeIndicator(
                mode = mode,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
        // Measurement panel
        MeasurementPanel(
            measurements = currentMeasurements,
            onClearAll = onMeasurementCleared,
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun TemperatureModeIndicator(
    mode: TemperatureMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = mode.displayName,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = mode.displayName,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MeasurementPanel(
    measurements: List<TemperatureMeasurementResult>,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Measurements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (measurements.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (measurements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "No measurements",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap to measure",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(measurements) { measurement ->
                        MeasurementCard(measurement = measurement)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    measurement: TemperatureMeasurementResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = measurement.icon,
                    contentDescription = measurement.type,
                    modifier = Modifier.size(16.dp),
                    tint = measurement.color
                )
                Text(
                    text = measurement.type,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            when (measurement) {
                is TemperatureMeasurementResult.Point -> {
                    Text(
                        text = "${measurement.temperature.roundToInt()}Â°C",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = measurement.color
                    )
                }

                is TemperatureMeasurementResult.Line -> {
                    Column {
                        Text("Max: ${measurement.temperatureMax.roundToInt()}Â°C", fontSize = 12.sp)
                        Text("Avg: ${measurement.temperatureAvg.roundToInt()}Â°C", fontSize = 12.sp)
                        Text("Min: ${measurement.temperatureMin.roundToInt()}Â°C", fontSize = 12.sp)
                    }
                }

                is TemperatureMeasurementResult.Rectangle -> {
                    Column {
                        Text("Max: ${measurement.temperatureMax.roundToInt()}Â°C", fontSize = 12.sp)
                        Text("Avg: ${measurement.temperatureAvg.roundToInt()}Â°C", fontSize = 12.sp)
                        Text("Min: ${measurement.temperatureMin.roundToInt()}Â°C", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawThermalBackground() {
    // Draw a simplified thermal pattern background
    val colors = listOf(
        Color.Blue, Color.Green, Color.Yellow, Color(0xFFFF6600), Color.Red
    )
    val cellSize = 20.dp.toPx()
    val cols = (size.width / cellSize).toInt()
    val rows = (size.height / cellSize).toInt()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val colorIndex = ((row + col) % colors.size)
            val alpha = 0.1f + (kotlin.math.sin((row + col) * 0.5) * 0.1f).toFloat()
            drawRect(
                color = colors[colorIndex].copy(alpha = alpha),
                topLeft = Offset(col * cellSize, row * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
        }
    }
}

private fun DrawScope.drawMeasurement(
    measurement: TemperatureMeasurementResult,
    showName: Boolean
) {
    when (measurement) {
        is TemperatureMeasurementResult.Point -> {
            // Draw crosshair
            val crossSize = 15.dp.toPx()
            drawLine(
                color = measurement.color,
                start = Offset(measurement.position.x - crossSize, measurement.position.y),
                end = Offset(measurement.position.x + crossSize, measurement.position.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = measurement.color,
                start = Offset(measurement.position.x, measurement.position.y - crossSize),
                end = Offset(measurement.position.x, measurement.position.y + crossSize),
                strokeWidth = 2.dp.toPx()
            )
            // Draw center circle
            drawCircle(
                color = measurement.color,
                radius = 4.dp.toPx(),
                center = measurement.position
            )
            if (showName) {
                // Temperature label would go here in a real implementation
            }
        }

        is TemperatureMeasurementResult.Line -> {
            drawLine(
                color = measurement.color,
                start = measurement.start,
                end = measurement.end,
                strokeWidth = 3.dp.toPx()
            )
            // Draw endpoints
            drawCircle(
                color = measurement.color,
                radius = 6.dp.toPx(),
                center = measurement.start
            )
            drawCircle(
                color = measurement.color,
                radius = 6.dp.toPx(),
                center = measurement.end
            )
        }

        is TemperatureMeasurementResult.Rectangle -> {
            drawRect(
                color = measurement.color,
                topLeft = measurement.rect.topLeft,
                size = measurement.rect.size,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private fun simulateTemperatureReading(position: Offset): Float {
    // Simulate temperature based on position (warmer toward center)
    val centerX = 400f // Assume center of thermal image
    val centerY = 300f
    val distance = kotlin.math.sqrt(
        (position.x - centerX) * (position.x - centerX) +
                (position.y - centerY) * (position.y - centerY)
    ).toFloat()
    return 25f + (100f - distance * 0.1f).coerceIn(15f, 45f)
}

private fun simulateAreaTemperatureReading(rect: Rect): Float {
    // Simulate average temperature in rectangle area
    val centerX = rect.center.x
    val centerY = rect.center.y
    return simulateTemperatureReading(Offset(centerX, centerY))
}

// Data classes and enums
enum class TemperatureMode(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    POINT("Point", Icons.Default.Place),
    LINE("Line", Icons.Default.Timeline),
    RECTANGLE("Rectangle", Icons.Default.CropFree),
    CLEAR("Clear", Icons.Default.Clear)
}

sealed class TemperatureMeasurementResult(
    val id: String,
    val type: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data class Point(
        val pointId: String,
        val position: Offset,
        val temperature: Float
    ) : TemperatureMeasurementResult(
        pointId, "Point", Color(0xFF2196F3), Icons.Default.Place
    )

    data class Line(
        val lineId: String,
        val start: Offset,
        val end: Offset,
        val temperatureMax: Float,
        val temperatureMin: Float,
        val temperatureAvg: Float
    ) : TemperatureMeasurementResult(
        lineId, "Line", Color(0xFF4CAF50), Icons.Default.Timeline
    )

    data class Rectangle(
        val rectId: String,
        val rect: Rect,
        val temperatureMax: Float,
        val temperatureMin: Float,
        val temperatureAvg: Float
    ) : TemperatureMeasurementResult(
        rectId, "Rectangle", Color(0xFFFF9800), Icons.Default.CropFree
    )
}

@Composable
fun TemperatureEditComposePreview() {
    var mode by remember { mutableStateOf(TemperatureMode.POINT) }
    var measurements by remember { mutableStateOf(emptyList<TemperatureMeasurementResult>()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Mode selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            TemperatureMode.values().forEach { tempMode ->
                FilterChip(
                    selected = mode == tempMode,
                    onClick = { mode = tempMode },
                    label = { Text(tempMode.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = tempMode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        // Temperature edit interface
        TemperatureEditCompose(
            mode = mode,
            isShowName = true,
            onMeasurementAdded = { measurement ->
                measurements = measurements + measurement
            },
            onMeasurementCleared = {
                measurements = emptyList()
            },
            measurements = measurements,
            modifier = Modifier.fillMaxSize()
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalAdaptersCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mpdc4gsr.module.thermalunified.model.AlbumItem

@Composable
fun CameraItemListCompose(
    items: List<CameraItem>,
    onItemClick: (Int, CameraItem) -> Unit = { _, _ -> },
    onTimerFinish: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            CameraItemCard(
                item = item,
                onClick = { onItemClick(index, item) },
                onTimerFinish = { onTimerFinish(index) }
            )
        }
    }
}

@Composable
private fun CameraItemCard(
    item: CameraItem,
    onClick: () -> Unit,
    onTimerFinish: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (item.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (item.type) {
                CameraItemType.DELAY -> {
                    if (item.delayTime == 0) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "No Delay",
                            modifier = Modifier.size(32.dp),
                            tint = if (item.isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    } else {
                        TimeDownCompose(
                            initialSeconds = item.delayTime,
                            onFinish = onTimerFinish,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                CameraItemType.AUTO_FOCUS -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.CenterFocusStrong else Icons.Default.CenterFocusWeak,
                        contentDescription = "Auto Focus",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                CameraItemType.FLASH -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                CameraItemType.HDR -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.WbSunny else Icons.Default.WbCloudy,
                        contentDescription = "HDR",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MeasureItemGridCompose(
    items: List<MeasureItem>,
    selectedIndex: Int = -1,
    onItemClick: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            MeasureItemCard(
                item = item,
                isSelected = selectedIndex == index,
                onClick = { onItemClick(index, item.code) }
            )
        }
    }
}

@Composable
private fun MeasureItemCard(
    item: MeasureItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (item.description.isNotEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GalleryAlbumListCompose(
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit = {},
    onDeleteAlbum: (AlbumItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(albums) { album ->
            GalleryAlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onDelete = { onDeleteAlbum(album) }
            )
        }
    }
}

@Composable
private fun GalleryAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album thumbnail
            AsyncImage(
                model = album.imagePath,
                contentDescription = album.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Album info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.description.isNotEmpty()) {
                    Text(
                        text = album.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${album.imageCount} images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Album",
                    tint = Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTabBarCompose(
    tabs: List<MenuTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = tab.title,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selectedIndex == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun SettingOptionsListCompose(
    options: List<SettingOption>,
    selectedOptions: Set<Int> = emptySet(),
    onOptionToggle: (Int, SettingOption) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(options) { index, option ->
            SettingOptionCard(
                option = option,
                isSelected = selectedOptions.contains(index),
                onToggle = { onOptionToggle(index, option) }
            )
        }
    }
}

@Composable
private fun SettingOptionCard(
    option: SettingOption,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (option.description.isNotEmpty()) {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (option.type == SettingOptionType.CHECKBOX) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            } else if (option.type == SettingOptionType.SWITCH) {
                Switch(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

// Data classes for adapters
data class CameraItem(
    val type: CameraItemType,
    val delayTime: Int = 0,
    val isSelected: Boolean = false
)

enum class CameraItemType {
    DELAY, AUTO_FOCUS, FLASH, HDR
}

data class MeasureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val code: Int
)

data class MenuTab(
    val title: String,
    val icon: ImageVector
)

data class SettingOption(
    val title: String,
    val description: String = "",
    val icon: ImageVector,
    val type: SettingOptionType = SettingOptionType.SIMPLE
)

enum class SettingOptionType {
    SIMPLE, CHECKBOX, SWITCH
}

@Composable
fun ThermalAdaptersPreview() {
    val sampleCameraItems = listOf(
        CameraItem(CameraItemType.DELAY, delayTime = 0, isSelected = true),
        CameraItem(CameraItemType.AUTO_FOCUS, isSelected = false),
        CameraItem(CameraItemType.FLASH, isSelected = false),
        CameraItem(CameraItemType.HDR, isSelected = false)
    )
    val sampleMeasureItems = listOf(
        MeasureItem("Person", "1.8m", Icons.Default.Person, 1001),
        MeasureItem("Animal", "1.0m", Icons.Default.Pets, 1002),
        MeasureItem("Object", "0.5m", Icons.Default.Category, 1003),
        MeasureItem("Small", "0.2m", Icons.Default.Circle, 1004)
    )
    val sampleTabs = listOf(
        MenuTab("Camera", Icons.Default.CameraAlt),
        MenuTab("Gallery", Icons.Default.PhotoLibrary),
        MenuTab("Settings", Icons.Default.Settings)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Adapters Preview",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Camera items
        Text("Camera Controls:", style = MaterialTheme.typography.titleMedium)
        CameraItemListCompose(
            items = sampleCameraItems,
            onItemClick = { index, item -> }
        )
        // Menu tabs
        Text("Menu Tabs:", style = MaterialTheme.typography.titleMedium)
        MenuTabBarCompose(
            tabs = sampleTabs,
            selectedIndex = 0,
            onTabSelected = { }
        )
        // Measure items
        Text("Measure Items:", style = MaterialTheme.typography.titleMedium)
        MeasureItemGridCompose(
            items = sampleMeasureItems,
            selectedIndex = 0,
            onItemClick = { _, _ -> },
            modifier = Modifier.height(200.dp)
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalFragmentPatterns.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ThermalCameraFragment(
    onCapturePhoto: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    isRecording: Boolean = false,
    temperatureData: List<Float> = emptyList(),
    currentTemperature: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Main camera view area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(8.dp))
        ) {
            // Placeholder for thermal camera surface
            // In real implementation, this would be AndroidView with thermal surface
            AndroidViewPlaceholder(
                viewType = "ThermalCameraSurface",
                modifier = Modifier.fillMaxSize()
            )
            // Temperature overlay
            if (currentTemperature > 0f) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "${String.format("%.1f", currentTemperature)}Â°C",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Camera controls
        ThermalCameraControls(
            onCapturePhoto = onCapturePhoto,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            isRecording = isRecording
        )
        // Temperature trend
        if (temperatureData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TrendViewStatefulCompose(
                temperatureData = temperatureData,
                initiallyExpanded = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThermalCameraControls(
    onCapturePhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Capture photo button
        FloatingActionButton(
            onClick = onCapturePhoto,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Photo"
            )
        }
        // Record video button
        FloatingActionButton(
            onClick = if (isRecording) onStopRecording else onStartRecording,
            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
            )
        }
        // Settings button
        val context = androidx.compose.ui.platform.LocalContext.current
        FloatingActionButton(
            onClick = {
                // TODO: Implement thermal camera settings navigation
                // Should open settings screen for temperature range, palette, etc.
                android.widget.Toast.makeText(
                    context,
                    "Opening thermal camera settings...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            containerColor = MaterialTheme.colorScheme.tertiary
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
fun ThermalGalleryFragment(
    images: List<ThermalGalleryItem>,
    onImageClick: (ThermalGalleryItem) -> Unit = {},
    onDeleteImage: (ThermalGalleryItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        EmptyStateComponent(
            title = "No Images",
            description = "Take some thermal photos to see them here",
            icon = Icons.Default.PhotoLibrary,
            actionText = "Take Photo",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        ThermalGalleryItemCard(
                            item = item,
                            onClick = { onImageClick(item) },
                            onDelete = { onDeleteImage(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space for odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalGalleryItemCard(
    item: ThermalGalleryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.temperature != null) {
                        Text(
                            text = "${String.format("%.1f", item.temperature)}Â°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun MonitorCaptureFragment(
    isMonitoring: Boolean,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onCaptureFrame: () -> Unit = {},
    capturedFrames: List<MonitorFrame> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Monitor status
        MonitorStatusCard(
            isMonitoring = isMonitoring,
            frameCount = capturedFrames.size,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = if (isMonitoring) onStopMonitoring else onStartMonitoring,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isMonitoring) "Stop Monitor" else "Start Monitor"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMonitoring) "Stop Monitor" else "Start Monitor")
            }
            Button(
                onClick = onCaptureFrame,
                enabled = isMonitoring,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture Frame"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Capture")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Captured frames
        if (capturedFrames.isNotEmpty()) {
            Text(
                text = "Captured Frames",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capturedFrames) { frame ->
                    MonitorFrameCard(frame = frame)
                }
            }
        }
    }
}

@Composable
private fun MonitorStatusCard(
    isMonitoring: Boolean,
    frameCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isMonitoring) Color.Green else Color.Gray)
                )
                Text(
                    text = if (isMonitoring) "Monitoring Active" else "Monitoring Inactive",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$frameCount frames",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonitorFrameCard(
    frame: MonitorFrame,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = frame.imagePath,
                contentDescription = "Monitor Frame",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = frame.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (frame.temperature != null) {
                    Text(
                        text = "${String.format("%.1f", frame.temperature)}Â°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AndroidViewPlaceholder(
    viewType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.1f))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Preview",
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewType,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Data classes for fragment patterns
data class ThermalGalleryItem(
    val id: String,
    val imagePath: String,
    val title: String,
    val timestamp: String,
    val temperature: Float? = null,
    val type: String = "thermal"
)

data class MonitorFrame(
    val id: String,
    val imagePath: String,
    val timestamp: String,
    val temperature: Float? = null
)

@Composable
fun ThermalFragmentPatternsPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Fragment Patterns",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Monitor status example
        MonitorStatusCard(
            isMonitoring = true,
            frameCount = 15
        )
        // Camera controls example
        ThermalCameraControls(
            onCapturePhoto = {},
            onStartRecording = {},
            onStopRecording = {},
            isRecording = false
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalInputDialogCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ThermalInputDialogCompose(
    message: String,
    maxTemp: Float = 100f,
    minTemp: Float = 0f,
    maxColor: Color = Color.Red,
    minColor: Color = Color.Blue,
    positiveButtonText: String = "OK",
    negativeButtonText: String = "Cancel",
    onConfirm: (Float, Float, Int, Int) -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var maxTempInput by remember { mutableStateOf(maxTemp.toString()) }
    var minTempInput by remember { mutableStateOf(minTemp.toString()) }
    val maxTempFocusRequester = remember { FocusRequester() }
    val minTempFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        maxTempFocusRequester.requestFocus()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Thermal Parameters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Message
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Temperature Range Inputs
                Column {
                    // Max Temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(maxColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = maxTempInput,
                            onValueChange = { maxTempInput = it },
                            label = { Text("Max Temperature") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(maxTempFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { minTempFocusRequester.requestFocus() }
                            ),
                            singleLine = true,
                            suffix = { Text("Â°C") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Min Temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(minColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = minTempInput,
                            onValueChange = { minTempInput = it },
                            label = { Text("Min Temperature") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(minTempFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    handleConfirm(
                                        maxTempInput,
                                        minTempInput,
                                        maxColor,
                                        minColor,
                                        onConfirm,
                                        onDismiss
                                    )
                                }
                            ),
                            singleLine = true,
                            suffix = { Text("Â°C") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(negativeButtonText)
                    }
                    Button(
                        onClick = {
                            handleConfirm(
                                maxTempInput,
                                minTempInput,
                                maxColor,
                                minColor,
                                onConfirm,
                                onDismiss
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(positiveButtonText)
                    }
                }
            }
        }
    }
}

private fun handleConfirm(
    maxTempInput: String,
    minTempInput: String,
    maxColor: Color,
    minColor: Color,
    onConfirm: (Float, Float, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val maxTemp = maxTempInput.toFloat()
        val minTemp = minTempInput.toFloat()
        if (maxTemp > minTemp) {
            onConfirm(
                maxTemp,
                minTemp,
                maxColor.toArgb(),
                minColor.toArgb()
            )
            onDismiss()
        }
    } catch (e: NumberFormatException) {
        // Invalid input - stay open for correction
    }
}

class ThermalInputDialogComposeBuilder {
    private var message: String = ""
    private var maxTemp: Float = 100f
    private var minTemp: Float = 0f
    private var maxColor: Color = Color.Red
    private var minColor: Color = Color.Blue
    private var positiveButtonText: String = "OK"
    private var negativeButtonText: String = "Cancel"
    private var onConfirm: ((Float, Float, Int, Int) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    fun setMessage(message: String): ThermalInputDialogComposeBuilder {
        this.message = message
        return this
    }

    fun setTemperatureRange(max: Float, min: Float): ThermalInputDialogComposeBuilder {
        this.maxTemp = max
        this.minTemp = min
        return this
    }

    fun setColors(maxColor: Color, minColor: Color): ThermalInputDialogComposeBuilder {
        this.maxColor = maxColor
        this.minColor = minColor
        return this
    }

    fun setButtonTexts(positive: String, negative: String): ThermalInputDialogComposeBuilder {
        this.positiveButtonText = positive
        this.negativeButtonText = negative
        return this
    }

    fun setPositiveListener(listener: (Float, Float, Int, Int) -> Unit): ThermalInputDialogComposeBuilder {
        this.onConfirm = listener
        return this
    }

    fun setCancelListener(listener: () -> Unit): ThermalInputDialogComposeBuilder {
        this.onCancel = listener
        return this
    }

    @Composable
    fun show(onDismiss: () -> Unit) {
        ThermalInputDialogCompose(
            message = message,
            maxTemp = maxTemp,
            minTemp = minTemp,
            maxColor = maxColor,
            minColor = minColor,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            onConfirm = onConfirm ?: { _, _, _, _ -> },
            onCancel = onCancel ?: {},
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ThermalInputDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        ThermalInputDialogCompose(
            message = "Configure thermal parameters for optimal imaging",
            maxTemp = 80f,
            minTemp = 20f,
            onConfirm = { maxTemp, minTemp, maxColor, minColor ->
                println("Confirmed: Max=$maxTemp, Min=$minTemp, MaxColor=$maxColor, MinColor=$minColor")
            },
            onCancel = {
                println("Cancelled")
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalNavigationCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalNavigationDrawer(
    selectedDestination: ThermalDestination,
    onNavigate: (ThermalDestination) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thermal Imaging",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Professional Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
            // Navigation items
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(ThermalDestination.values()) { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        selected = selectedDestination == destination,
                        onClick = {
                            onNavigate(destination)
                            onClose()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
            // Footer
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    // TODO: Implement navigation drawer settings
                    android.widget.Toast.makeText(
                        context,
                        "Opening settings...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = {
                    // TODO: Implement help screen or documentation
                    android.widget.Toast.makeText(
                        context,
                        "Opening help...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                }
                IconButton(onClick = {
                    // TODO: Implement about/info dialog
                    android.widget.Toast.makeText(
                        context,
                        "Showing app info...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.Info, contentDescription = "About")
                }
            }
        }
    }
}

@Composable
fun ThermalActionMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onActionSelected: (ThermalAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Action items
        if (isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                ThermalAction.values().forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = action.title,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallFloatingActionButton(
                            onClick = {
                                onActionSelected(action)
                                onToggle()
                            },
                            containerColor = action.color
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (isExpanded) "Close Menu" else "Open Menu"
            )
        }
    }
}

@Composable
fun ThermalBottomNavigation(
    destinations: List<ThermalDestination>,
    selectedDestination: ThermalDestination,
    onNavigate: (ThermalDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun ThermalMenuGrid(
    menuItems: List<ThermalMenuItem>,
    onItemClick: (ThermalMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(menuItems.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    ThermalMenuCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space for odd number of items
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThermalMenuCard(
    item: ThermalMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(48.dp),
                tint = item.iconColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = item.textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = item.textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ThermalStatusBar(
    status: ThermalStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status.level) {
                ThermalStatusLevel.NORMAL -> MaterialTheme.colorScheme.surface
                ThermalStatusLevel.WARNING -> Color(0xFFFFF3E0)
                ThermalStatusLevel.CRITICAL -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (status.level) {
                            ThermalStatusLevel.NORMAL -> Color.Green
                            ThermalStatusLevel.WARNING -> Color(0xFFFF6600)
                            ThermalStatusLevel.CRITICAL -> Color.Red
                        }
                    )
            )
            // Status text
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            // Temperature display
            if (status.currentTemp != null) {
                Text(
                    text = "${String.format("%.1f", status.currentTemp)}Â°C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status.level) {
                        ThermalStatusLevel.NORMAL -> MaterialTheme.colorScheme.primary
                        ThermalStatusLevel.WARNING -> Color(0xFFFF8F00)
                        ThermalStatusLevel.CRITICAL -> Color(0xFFD32F2F)
                    }
                )
            }
        }
    }
}

// Data classes and enums
enum class ThermalDestination(
    val title: String,
    val icon: ImageVector
) {
    CAMERA("Camera", Icons.Default.CameraAlt),
    GALLERY("Gallery", Icons.Default.PhotoLibrary),
    ANALYSIS("Analysis", Icons.Default.Analytics),
    MEASUREMENT("Measurement", Icons.Default.Straighten),
    MONITOR("Monitor", Icons.Default.Monitor),
    REPORTS("Reports", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings)
}

enum class ThermalAction(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    CAPTURE("Capture", Icons.Default.CameraAlt, Color(0xFF4CAF50)),
    RECORD("Record", Icons.Default.Videocam, Color(0xFFFF5722)),
    MEASURE("Measure", Icons.Default.Straighten, Color(0xFF2196F3)),
    ANALYZE("Analyze", Icons.Default.Analytics, Color(0xFF9C27B0))
}

data class ThermalMenuItem(
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val backgroundColor: Color = Color.Transparent,
    val iconColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified
)

data class ThermalStatus(
    val message: String,
    val level: ThermalStatusLevel,
    val currentTemp: Float? = null
)

enum class ThermalStatusLevel {
    NORMAL, WARNING, CRITICAL
}

@Composable
fun ThermalNavigationPreview() {
    val sampleMenuItems = listOf(
        ThermalMenuItem(
            title = "Live Camera",
            subtitle = "Real-time",
            icon = Icons.Default.CameraAlt,
            backgroundColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1976D2),
            textColor = Color(0xFF1976D2)
        ),
        ThermalMenuItem(
            title = "Gallery",
            subtitle = "View saved",
            icon = Icons.Default.PhotoLibrary,
            backgroundColor = Color(0xFFE8F5E8),
            iconColor = Color(0xFF388E3C),
            textColor = Color(0xFF388E3C)
        ),
        ThermalMenuItem(
            title = "Analysis",
            subtitle = "Process data",
            icon = Icons.Default.Analytics,
            backgroundColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFFF8F00),
            textColor = Color(0xFFFF8F00)
        ),
        ThermalMenuItem(
            title = "Reports",
            subtitle = "Generate",
            icon = Icons.Default.Description,
            backgroundColor = Color(0xFFF3E5F5),
            iconColor = Color(0xFF7B1FA2),
            textColor = Color(0xFF7B1FA2)
        )
    )
    val sampleStatus = ThermalStatus(
        message = "Thermal camera connected and calibrated",
        level = ThermalStatusLevel.NORMAL,
        currentTemp = 25.4f
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Navigation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        ThermalStatusBar(status = sampleStatus)
        Text("Menu Grid:", style = MaterialTheme.typography.titleMedium)
        ThermalMenuGrid(
            menuItems = sampleMenuItems,
            onItemClick = { },
            modifier = Modifier.weight(1f)
        )
        ThermalBottomNavigation(
            destinations = listOf(
                ThermalDestination.CAMERA,
                ThermalDestination.GALLERY,
                ThermalDestination.ANALYSIS,
                ThermalDestination.SETTINGS
            ),
            selectedDestination = ThermalDestination.CAMERA,
            onNavigate = { }
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalPopupsCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun SeekBarPopupCompose(
    visible: Boolean,
    title: String = "",
    progress: Float,
    maxValue: Float = 100f,
    onProgressChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    isRealTimeTrigger: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(200)
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(150)
        )
    ) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.8f)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Progress value display
                    Text(
                        text = "${progress.roundToInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Modern Slider
                    Slider(
                        value = progress,
                        onValueChange = { newValue ->
                            if (isRealTimeTrigger) {
                                onProgressChange(newValue)
                            }
                        },
                        onValueChangeFinished = {
                            if (!isRealTimeTrigger) {
                                onProgressChange(progress)
                            }
                        },
                        valueRange = 0f..maxValue,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onProgressChange(progress)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionPickPopupCompose(
    visible: Boolean,
    options: List<String>,
    icons: List<ImageVector>? = null,
    selectedIndex: Int = -1,
    onOptionSelected: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(200)
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(150)
        )
    ) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .width(280.dp)
                    .heightIn(max = 300.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp)
                ) {
                    itemsIndexed(options) { index, option ->
                        OptionItemCompose(
                            text = option,
                            icon = icons?.getOrNull(index),
                            isSelected = index == selectedIndex,
                            onClick = {
                                onOptionSelected(index, option)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItemCompose(
    text: String,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200), label = "background"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                lerp(
                    Color.Transparent,
                    MaterialTheme.colorScheme.primaryContainer,
                    backgroundColor
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun GalleryChangePopupCompose(
    visible: Boolean,
    currentGallery: String,
    availableGalleries: List<String>,
    onGallerySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(200)
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(150)
        )
    ) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .width(320.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Select Gallery",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Select Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Gallery options
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        itemsIndexed(availableGalleries) { index, gallery ->
                            GalleryOptionItemCompose(
                                galleryName = gallery,
                                isSelected = gallery == currentGallery,
                                onClick = {
                                    onGallerySelected(gallery)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryOptionItemCompose(
    galleryName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200), label = "gallery_background"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                lerp(
                    Color.Transparent,
                    MaterialTheme.colorScheme.primaryContainer,
                    backgroundColor
                )
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = galleryName,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = galleryName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun SeekBarPopupPreview() {
    MaterialTheme {
        SeekBarPopupCompose(
            visible = true,
            title = "Brightness",
            progress = 75f,
            onProgressChange = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionPickPopupPreview() {
    MaterialTheme {
        OptionPickPopupCompose(
            visible = true,
            options = listOf("Option 1", "Option 2", "Option 3"),
            icons = listOf(Icons.Default.Settings, Icons.Default.Camera, Icons.Default.Photo),
            selectedIndex = 1,
            onOptionSelected = { _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryChangePopupPreview() {
    MaterialTheme {
        GalleryChangePopupCompose(
            visible = true,
            currentGallery = "Thermal Images",
            availableGalleries = listOf("Thermal Images", "Regular Photos", "Screenshots"),
            onGallerySelected = {},
            onDismiss = {}
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalReportCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

// Data classes for report components
data class ReportData(
    val id: String,
    val title: String,
    val description: String,
    val createdDate: String,
    val modifiedDate: String,
    val images: List<String>,
    val measurements: List<MeasurementData>,
    val metadata: ReportMetadata
)

data class MeasurementData(
    val id: Int,
    val name: String,
    val value: String,
    val unit: String,
    val type: MeasurementType,
    val isEditable: Boolean = true
)

data class ReportMetadata(
    val author: String,
    val location: String,
    val equipment: String,
    val conditions: String,
    val notes: String
)

enum class MeasurementType {
    TEMPERATURE, HUMIDITY, PRESSURE, DISTANCE, EMISSIVITY
}

data class WatermarkData(
    val text: String,
    val position: WatermarkPosition,
    val opacity: Float = 0.3f,
    val fontSize: Float = 14f,
    val color: Color = Color.Gray
)

enum class WatermarkPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIRInputCompose(
    reportData: ReportData,
    onReportUpdated: (ReportData) -> Unit,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentReport by remember { mutableStateOf(reportData) }
    LaunchedEffect(reportData) {
        currentReport = reportData
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header
        item {
            ReportHeaderCompose(
                report = currentReport,
                onTitleChanged = { newTitle ->
                    currentReport = currentReport.copy(title = newTitle)
                    onReportUpdated(currentReport)
                },
                onDescriptionChanged = { newDescription ->
                    currentReport = currentReport.copy(description = newDescription)
                    onReportUpdated(currentReport)
                }
            )
        }
        // Images section
        item {
            ReportImagesCompose(
                images = currentReport.images,
                onImageAdded = onImageAdded,
                onImageRemoved = onImageRemoved
            )
        }
        // Measurements section
        item {
            ReportMeasurementsCompose(
                measurements = currentReport.measurements,
                onMeasurementUpdated = { updatedMeasurement ->
                    val updatedMeasurements = currentReport.measurements.map { measurement ->
                        if (measurement.id == updatedMeasurement.id) updatedMeasurement else measurement
                    }
                    currentReport = currentReport.copy(measurements = updatedMeasurements)
                    onReportUpdated(currentReport)
                }
            )
        }
        // Metadata section
        item {
            ReportMetadataCompose(
                metadata = currentReport.metadata,
                onMetadataUpdated = { updatedMetadata ->
                    currentReport = currentReport.copy(metadata = updatedMetadata)
                    onReportUpdated(currentReport)
                }
            )
        }
    }
}

@Composable
private fun ReportHeaderCompose(
    report: ReportData,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = report.title,
                onValueChange = onTitleChanged,
                label = { Text("Report Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = report.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created: ${report.createdDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "Modified: ${report.modifiedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ReportImagesCompose(
    images: List<String>,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Images (${images.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { onImageAdded("") }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { imagePath ->
                        ReportImageItem(
                            imagePath = imagePath,
                            onRemoved = { onImageRemoved(imagePath) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onImageAdded("") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ImageSearch,
                            contentDescription = "Add images",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to add images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportImageItem(
    imagePath: String,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imagePath)
                .crossfade(true)
                .build(),
            contentDescription = "Report image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemoved,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun ReportMeasurementsCompose(
    measurements: List<MeasurementData>,
    onMeasurementUpdated: (MeasurementData) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Measurements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            measurements.forEach { measurement ->
                MeasurementItemCompose(
                    measurement = measurement,
                    onUpdated = onMeasurementUpdated
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MeasurementItemCompose(
    measurement: MeasurementData,
    onUpdated: (MeasurementData) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(measurement.value) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (measurement.type) {
                MeasurementType.TEMPERATURE -> Icons.Default.Thermostat
                MeasurementType.HUMIDITY -> Icons.Default.WaterDrop
                MeasurementType.PRESSURE -> Icons.Default.Speed
                MeasurementType.DISTANCE -> Icons.Default.Straighten
                MeasurementType.EMISSIVITY -> Icons.Default.Opacity
            },
            contentDescription = measurement.type.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = measurement.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (measurement.isEditable) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    value = newValue
                    onUpdated(measurement.copy(value = newValue))
                },
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = measurement.unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ReportMetadataCompose(
    metadata: ReportMetadata,
    onMetadataUpdated: (ReportMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMetadata by remember { mutableStateOf(metadata) }
    LaunchedEffect(metadata) {
        currentMetadata = metadata
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Report Metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = currentMetadata.author,
                onValueChange = { newAuthor ->
                    currentMetadata = currentMetadata.copy(author = newAuthor)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Author")
                }
            )
            OutlinedTextField(
                value = currentMetadata.location,
                onValueChange = { newLocation ->
                    currentMetadata = currentMetadata.copy(location = newLocation)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location")
                }
            )
            OutlinedTextField(
                value = currentMetadata.equipment,
                onValueChange = { newEquipment ->
                    currentMetadata = currentMetadata.copy(equipment = newEquipment)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Equipment") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Build, contentDescription = "Equipment")
                }
            )
            OutlinedTextField(
                value = currentMetadata.conditions,
                onValueChange = { newConditions ->
                    currentMetadata = currentMetadata.copy(conditions = newConditions)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Conditions") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = "Conditions")
                }
            )
            OutlinedTextField(
                value = currentMetadata.notes,
                onValueChange = { newNotes ->
                    currentMetadata = currentMetadata.copy(notes = newNotes)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes")
                }
            )
        }
    }
}

@Composable
fun ReportIRShowCompose(
    reportData: ReportData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = reportData.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reportData.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Created: ${reportData.createdDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Modified: ${reportData.modifiedDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        // Images display
        item {
            if (reportData.images.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Images (${reportData.images.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(reportData.images) { imagePath ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imagePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Report image",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
        // Measurements display (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    reportData.measurements.forEach { measurement ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (measurement.type) {
                                    MeasurementType.TEMPERATURE -> Icons.Default.Thermostat
                                    MeasurementType.HUMIDITY -> Icons.Default.WaterDrop
                                    MeasurementType.PRESSURE -> Icons.Default.Speed
                                    MeasurementType.DISTANCE -> Icons.Default.Straighten
                                    MeasurementType.EMISSIVITY -> Icons.Default.Opacity
                                },
                                contentDescription = measurement.type.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = measurement.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${measurement.value} ${measurement.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        // Metadata display (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Report Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    MetadataRow("Author", reportData.metadata.author, Icons.Default.Person)
                    MetadataRow("Location", reportData.metadata.location, Icons.Default.LocationOn)
                    MetadataRow("Equipment", reportData.metadata.equipment, Icons.Default.Build)
                    MetadataRow("Conditions", reportData.metadata.conditions, Icons.Default.Cloud)
                    if (reportData.metadata.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = reportData.metadata.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    if (value.isNotEmpty()) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun WatermarkCompose(
    watermarkData: WatermarkData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = when (watermarkData.position) {
            WatermarkPosition.TOP_LEFT -> Alignment.TopStart
            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
            WatermarkPosition.CENTER -> Alignment.Center
        }
    ) {
        Text(
            text = watermarkData.text,
            color = watermarkData.color.copy(alpha = watermarkData.opacity),
            fontSize = watermarkData.fontSize.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun ReportIRInputPreview() {
    LibUnifiedTheme {
        val sampleReport = ReportData(
            id = "1",
            title = "Thermal Analysis Report",
            description = "Comprehensive thermal analysis of equipment",
            createdDate = "2023-12-01",
            modifiedDate = "2023-12-02",
            images = listOf("image1.jpg", "image2.jpg"),
            measurements = listOf(
                MeasurementData(1, "Max Temperature", "85.5", "Â°C", MeasurementType.TEMPERATURE),
                MeasurementData(2, "Humidity", "45", "%", MeasurementType.HUMIDITY)
            ),
            metadata = ReportMetadata(
                author = "John Doe",
                location = "Factory Floor A",
                equipment = "FLIR T640",
                conditions = "Ambient 20Â°C",
                notes = "Regular inspection"
            )
        )
        ReportIRInputCompose(
            reportData = sampleReport,
            onReportUpdated = {},
            onImageAdded = {},
            onImageRemoved = {}
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalStubsCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

// Data classes for component state
data class MonitorOption(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true
)

data class TipDialogData(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val type: TipType = TipType.INFO
)

enum class TipType {
    INFO, WARNING, ERROR, SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorSelectDialogCompose(
    showDialog: Boolean,
    options: List<MonitorOption>,
    selectedOption: MonitorOption?,
    onOptionSelected: (MonitorOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Dialog title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Monitor",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Options list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options) { option ->
                            MonitorOptionItem(
                                option = option,
                                isSelected = option.id == selectedOption?.id,
                                onSelected = { onOptionSelected(option) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedOption?.let { onOptionSelected(it) }
                                onDismiss()
                            },
                            enabled = selectedOption != null
                        ) {
                            Text("Select")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorOptionItem(
    option: MonitorOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = option.isEnabled) { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.name,
                tint = if (option.isEnabled) {
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (option.isEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                if (option.description.isNotEmpty()) {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TipDialogCompose(
    showDialog: Boolean,
    tipData: TipDialogData,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = tipData.icon,
                    contentDescription = tipData.type.name,
                    tint = when (tipData.type) {
                        TipType.INFO -> MaterialTheme.colorScheme.primary
                        TipType.WARNING -> MaterialTheme.colorScheme.error
                        TipType.ERROR -> MaterialTheme.colorScheme.error
                        TipType.SUCCESS -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = tipData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = tipData.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm?.invoke()
                        onDismiss()
                    }
                ) {
                    Text(if (onConfirm != null) "OK" else "Dismiss")
                }
            },
            dismissButton = if (onConfirm != null) {
                {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            } else null,
            modifier = modifier
        )
    }
}

@Composable
fun FenceViewsCompose(
    fences: List<FenceData>,
    onFenceSelected: (FenceData) -> Unit,
    onFenceDeleted: (FenceData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(fences) { fence ->
            FenceItemCompose(
                fence = fence,
                onSelected = { onFenceSelected(fence) },
                onDeleted = { onFenceDeleted(fence) }
            )
        }
    }
}

@Composable
private fun FenceItemCompose(
    fence: FenceData,
    onSelected: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CropFree,
                contentDescription = "Fence",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fence.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Points: ${fence.points.size} | Temp: ${fence.temperature}Â°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDeleted) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete fence",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun GuideStubsCompose(
    guideSteps: List<GuideStep>,
    currentStep: Int,
    onStepChanged: (Int) -> Unit,
    onGuideCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1f) / guideSteps.size },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Current step content
            if (currentStep < guideSteps.size) {
                val step = guideSteps[currentStep]
                Text(
                    text = "Step ${currentStep + 1} of ${guideSteps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (currentStep > 0) onStepChanged(currentStep - 1)
                        },
                        enabled = currentStep > 0
                    ) {
                        Text("Previous")
                    }
                    Button(
                        onClick = {
                            if (currentStep < guideSteps.size - 1) {
                                onStepChanged(currentStep + 1)
                            } else {
                                onGuideCompleted()
                            }
                        }
                    ) {
                        Text(if (currentStep < guideSteps.size - 1) "Next" else "Complete")
                    }
                }
            }
        }
    }
}

@Composable
fun UIWidgetsCompose(
    widgets: List<WidgetData>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(widgets) { widget ->
            WidgetItemCompose(widget = widget)
        }
    }
}

@Composable
private fun WidgetItemCompose(
    widget: WidgetData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = widget.icon,
                    contentDescription = widget.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = widget.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (widget.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = widget.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Data classes for components
data class FenceData(
    val id: Int,
    val name: String,
    val points: List<Pair<Float, Float>>,
    val temperature: Float
)

data class GuideStep(
    val title: String,
    val description: String,
    val imageRes: Int? = null
)

data class WidgetData(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)

// Preview functions
@Preview(showBackground = true)
@Composable
private fun MonitorSelectDialogPreview() {
    LibUnifiedTheme {
        val sampleOptions = listOf(
            MonitorOption(1, "Temperature Monitor", "Real-time temperature tracking", Icons.Default.Thermostat),
            MonitorOption(2, "Pressure Monitor", "Pressure level monitoring", Icons.Default.Speed),
            MonitorOption(3, "Humidity Monitor", "Humidity level tracking", Icons.Default.WaterDrop, false)
        )
        MonitorSelectDialogCompose(
            showDialog = true,
            options = sampleOptions,
            selectedOption = sampleOptions[0],
            onOptionSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TipDialogPreview() {
    LibUnifiedTheme {
        TipDialogCompose(
            showDialog = true,
            tipData = TipDialogData(
                title = "Calibration Required",
                message = "Please calibrate the thermal sensor before proceeding with measurements.",
                icon = Icons.Default.Warning,
                type = TipType.WARNING
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FenceViewsPreview() {
    LibUnifiedTheme {
        val sampleFences = listOf(
            FenceData(1, "Temperature Zone 1", listOf(0f to 0f, 100f to 100f), 25.5f),
            FenceData(2, "Critical Area", listOf(50f to 50f, 150f to 150f), 85.2f)
        )
        FenceViewsCompose(
            fences = sampleFences,
            onFenceSelected = {},
            onFenceDeleted = {}
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalToolsCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalTextApi::class)
@Composable
fun FenceCompose(
    bounds: List<Offset>,
    temperatureRange: ClosedFloatingPointRange<Float>,
    currentTemp: Float,
    isActive: Boolean = true,
    onBoundsChanged: (List<Offset>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var dragIndex by remember { mutableIntStateOf(-1) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,
        animationSpec = tween(300), label = "fence_alpha"
    )
    val fenceColor = when {
        currentTemp < temperatureRange.start -> Color.Blue
        currentTemp > temperatureRange.endInclusive -> Color.Red
        else -> Color.Green
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bounds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragIndex = bounds.indexOfFirst { point ->
                            (offset - point).getDistance() < 50f
                        }
                    },
                    onDragEnd = {
                        dragIndex = -1
                    }
                ) { change, dragAmount ->
                    if (dragIndex >= 0) {
                        val newBounds = bounds.toMutableList()
                        newBounds[dragIndex] = bounds[dragIndex] + dragAmount
                        onBoundsChanged(newBounds)
                    }
                }
            }
    ) {
        if (bounds.size >= 2) {
            // Draw fence boundary
            val path = Path().apply {
                moveTo(bounds.first().x, bounds.first().y)
                bounds.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
                close()
            }
            // Fill area
            drawPath(
                path = path,
                color = fenceColor.copy(alpha = 0.2f * animatedAlpha),
                style = Fill
            )
            // Draw boundary
            drawPath(
                path = path,
                color = fenceColor.copy(alpha = animatedAlpha),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
            // Draw control points
            bounds.forEachIndexed { index, point ->
                val pointColor =
                    if (index == dragIndex) fenceColor.copy(alpha = 0.8f) else fenceColor.copy(alpha = 0.6f)
                val pointSize = if (index == dragIndex) 12.dp.toPx() else 8.dp.toPx()
                drawCircle(
                    color = pointColor,
                    radius = pointSize,
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = pointSize - 2.dp.toPx(),
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Draw temperature info
            if (bounds.isNotEmpty()) {
                val centerPoint = bounds.reduce { acc, point ->
                    Offset(acc.x + point.x, acc.y + point.y)
                } / bounds.size.toFloat()
                val tempText = "${currentTemp.roundToInt()}Â°C"
                val textLayoutResult = textMeasurer.measure(
                    text = tempText,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                // Background for text
                drawRoundRect(
                    color = fenceColor.copy(alpha = 0.8f * animatedAlpha),
                    topLeft = centerPoint - Offset(
                        textLayoutResult.size.width / 2f + 8.dp.toPx(),
                        textLayoutResult.size.height / 2f + 4.dp.toPx()
                    ),
                    size = Size(
                        textLayoutResult.size.width + 16.dp.toPx(),
                        textLayoutResult.size.height + 8.dp.toPx()
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                // Temperature text
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = centerPoint - Offset(
                        textLayoutResult.size.width / 2f,
                        textLayoutResult.size.height / 2f
                    )
                )
            }
        }
    }
}

@Composable
fun ThermalToolCompose(
    selectedTool: ThermalToolType,
    onToolSelected: (ThermalToolType) -> Unit,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tools = remember {
        listOf(
            ThermalToolType.POINT to Icons.Default.Place,
            ThermalToolType.LINE to Icons.Default.Timeline,
            ThermalToolType.RECTANGLE to Icons.Default.CropFree,
            ThermalToolType.CIRCLE to Icons.Default.RadioButtonUnchecked,
            ThermalToolType.FENCE to Icons.Default.BorderAll,
            ThermalToolType.MEASURE to Icons.Default.Straighten
        )
    }
    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tools) { (tool, icon) ->
                    ThermalToolItemCompose(
                        tool = tool,
                        icon = icon,
                        isSelected = tool == selectedTool,
                        onClick = { onToolSelected(tool) }
                    )
                }
            }
        }
    }
    // Tool toggle button
    FloatingActionButton(
        onClick = onExpandToggle,
        modifier = Modifier.size(56.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Build,
            contentDescription = "Toggle tools",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ThermalToolItemCompose(
    tool: ThermalToolType,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200), label = "tool_background"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200), label = "tool_icon"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tool.name,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun FenceToolCompose(
    fences: List<ThermalFence>,
    selectedFenceId: String? = null,
    onFenceSelected: (String) -> Unit = {},
    onFenceCreated: (ThermalFence) -> Unit = {},
    onFenceUpdated: (ThermalFence) -> Unit = {},
    onFenceDeleted: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var isCreatingFence by remember { mutableStateOf(false) }
    var newFencePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(fences, isCreatingFence) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isCreatingFence) {
                            newFencePoints = newFencePoints + offset
                        } else {
                            // Select fence
                            val clickedFence = fences.find { fence ->
                                fence.bounds.any { point ->
                                    (offset - point).getDistance() < 50f
                                }
                            }
                            clickedFence?.let { onFenceSelected(it.id) }
                        }
                    }
                ) { _, _ -> }
            }
    ) {
        // Draw existing fences
        fences.forEach { fence ->
            val isSelected = fence.id == selectedFenceId
            val fenceColor = when (fence.status) {
                ThermalFenceStatus.NORMAL -> Color.Green
                ThermalFenceStatus.WARNING -> Color.Yellow
                ThermalFenceStatus.ALARM -> Color.Red
            }
            drawFence(
                bounds = fence.bounds,
                color = fenceColor,
                isSelected = isSelected,
                temperature = fence.averageTemperature,
                label = fence.label,
                textMeasurer = textMeasurer
            )
        }
        // Draw fence being created
        if (isCreatingFence && newFencePoints.size >= 2) {
            drawFence(
                bounds = newFencePoints,
                color = Color.Blue,
                isSelected = false,
                temperature = 0f,
                label = "New Fence",
                textMeasurer = textMeasurer,
                alpha = 0.7f
            )
        }
    }
    // Fence creation controls
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                isCreatingFence = !isCreatingFence
                if (!isCreatingFence) {
                    newFencePoints = emptyList()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCreatingFence) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isCreatingFence) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (isCreatingFence) "Cancel" else "Add Fence",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isCreatingFence) "Cancel" else "Add Fence")
        }
        if (isCreatingFence && newFencePoints.size >= 3) {
            Button(
                onClick = {
                    val newFence = ThermalFence(
                        id = "fence_${UUID.randomUUID()}",
                        bounds = newFencePoints,
                        label = "Fence ${fences.size + 1}",
                        averageTemperature = 0f,
                        status = ThermalFenceStatus.NORMAL
                    )
                    onFenceCreated(newFence)
                    isCreatingFence = false
                    newFencePoints = emptyList()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Create Fence",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create")
            }
        }
        selectedFenceId?.let {
            OutlinedButton(
                onClick = { onFenceDeleted(it) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Fence",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawFence(
    bounds: List<Offset>,
    color: Color,
    isSelected: Boolean,
    temperature: Float,
    label: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    alpha: Float = 1f
) {
    if (bounds.size < 2) return
    val path = Path().apply {
        moveTo(bounds.first().x, bounds.first().y)
        bounds.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
        if (bounds.size > 2) close()
    }
    // Fill area
    drawPath(
        path = path,
        color = color.copy(alpha = 0.2f * alpha),
        style = Fill
    )
    // Draw boundary
    val strokeWidth = if (isSelected) 4.dp.toPx() else 2.dp.toPx()
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    )
    // Draw control points
    bounds.forEach { point ->
        val pointSize = if (isSelected) 10.dp.toPx() else 6.dp.toPx()
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = pointSize,
            center = point
        )
        drawCircle(
            color = Color.White,
            radius = pointSize - 2.dp.toPx(),
            center = point,
            style = Stroke(width = 1.dp.toPx())
        )
    }
    // Draw label and temperature
    if (bounds.isNotEmpty()) {
        val centerPoint = bounds.reduce { acc, point ->
            Offset(acc.x + point.x, acc.y + point.y)
        } / bounds.size.toFloat()
        val infoText = "$label\n${temperature.roundToInt()}Â°C"
        val textLayoutResult = textMeasurer.measure(
            text = infoText,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        // Background for text
        drawRoundRect(
            color = color.copy(alpha = 0.8f * alpha),
            topLeft = centerPoint - Offset(
                textLayoutResult.size.width / 2f + 6.dp.toPx(),
                textLayoutResult.size.height / 2f + 3.dp.toPx()
            ),
            size = Size(
                textLayoutResult.size.width + 12.dp.toPx(),
                textLayoutResult.size.height + 6.dp.toPx()
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Text
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = centerPoint - Offset(
                textLayoutResult.size.width / 2f,
                textLayoutResult.size.height / 2f
            )
        )
    }
}

// Data classes
enum class ThermalToolType {
    POINT, LINE, RECTANGLE, CIRCLE, FENCE, MEASURE
}

data class ThermalFence(
    val id: String,
    val bounds: List<Offset>,
    val label: String,
    val averageTemperature: Float,
    val status: ThermalFenceStatus
)

enum class ThermalFenceStatus {
    NORMAL, WARNING, ALARM
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun FenceComposePreview() {
    MaterialTheme {
        FenceCompose(
            bounds = listOf(
                Offset(100f, 100f),
                Offset(200f, 100f),
                Offset(200f, 200f),
                Offset(100f, 200f)
            ),
            temperatureRange = 20f..40f,
            currentTemp = 35f,
            modifier = Modifier.size(300.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThermalToolComposePreview() {
    MaterialTheme {
        ThermalToolCompose(
            selectedTool = ThermalToolType.POINT,
            onToolSelected = {},
            isExpanded = true
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\ThermalUtilsCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.utils.UnifiedMathUtils
import kotlin.math.roundToInt

@Composable
fun TemperatureIndicator(
    temperature: Float,
    unit: String = "Â°C",
    minTemp: Float = 0f,
    maxTemp: Float = 100f,
    coldColor: Color = Color.Blue,
    hotColor: Color = Color.Red,
    modifier: Modifier = Modifier
) {
    val normalizedTemp = ((temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
    val backgroundColor = Color(
        UnifiedMathUtils.lerp(coldColor.red, hotColor.red, normalizedTemp),
        UnifiedMathUtils.lerp(coldColor.green, hotColor.green, normalizedTemp),
        UnifiedMathUtils.lerp(coldColor.blue, hotColor.blue, normalizedTemp),
        UnifiedMathUtils.lerp(coldColor.alpha, hotColor.alpha, normalizedTemp)
    )
    val textColor = if (normalizedTemp > 0.5f) Color.White else Color.Black
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "${temperature.roundToInt()}$unit",
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ThermalGradientBar(
    minTemp: Float,
    maxTemp: Float,
    unit: String = "Â°C",
    coldColor: Color = Color.Blue,
    hotColor: Color = Color.Red,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(coldColor, hotColor)
                    )
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Temperature labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minTemp.roundToInt()}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${maxTemp.roundToInt()}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThermalStatusIndicator(
    level: ThermalStatusLevel,
    message: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (level) {
        ThermalStatusLevel.NORMAL -> Color.Green
        ThermalStatusLevel.WARNING -> Color(0xFFFF9800) // Orange
        ThermalStatusLevel.CRITICAL -> Color.Red
    }
    val statusIcon = when (level) {
        ThermalStatusLevel.NORMAL -> Icons.Default.CheckCircle
        ThermalStatusLevel.WARNING -> Icons.Default.Warning
        ThermalStatusLevel.CRITICAL -> Icons.Default.Error
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Icon(
            imageVector = statusIcon,
            contentDescription = message,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MeasurementPoint(
    label: String,
    temperature: Float,
    unit: String = "Â°C",
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isActive) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${temperature.roundToInt()}$unit",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun ThermalToolbar(
    selectedTool: ThermalTool?,
    onToolSelected: (ThermalTool?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThermalTool.entries.forEach { tool ->
            val isSelected = selectedTool == tool
            FilterChip(
                selected = isSelected,
                onClick = {
                    onToolSelected(if (isSelected) null else tool)
                },
                label = {
                    Text(
                        text = tool.displayName,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.displayName,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

// Data classes and enums for the utilities
enum class ThermalTool(
    val displayName: String,
    val icon: ImageVector
) {
    POINT("Point", Icons.Default.Place),
    LINE("Line", Icons.Default.Timeline),
    RECTANGLE("Rect", Icons.Default.CropFree),
    CIRCLE("Circle", Icons.Default.RadioButtonUnchecked),
    AREA("Area", Icons.Default.CropFree)
}

@Composable
fun ThermalUtilsComposePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Temperature indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemperatureIndicator(
                temperature = 25f,
                modifier = Modifier.weight(1f)
            )
            TemperatureIndicator(
                temperature = 75f,
                modifier = Modifier.weight(1f)
            )
        }
        // Gradient bar
        ThermalGradientBar(
            minTemp = 0f,
            maxTemp = 100f
        )
        // Status indicators
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ThermalStatusIndicator(
                level = ThermalStatusLevel.NORMAL,
                message = "Connected"
            )
            ThermalStatusIndicator(
                level = ThermalStatusLevel.WARNING,
                message = "High Temperature"
            )
            ThermalStatusIndicator(
                level = ThermalStatusLevel.CRITICAL,
                message = "Critical Temperature"
            )
        }
        // Measurement points
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeasurementPoint(
                label = "P1",
                temperature = 36.5f,
                isActive = true,
                modifier = Modifier.weight(1f)
            )
            MeasurementPoint(
                label = "P2",
                temperature = 42.1f,
                modifier = Modifier.weight(1f)
            )
        }
        // Toolbar
        var selectedTool by remember { mutableStateOf<ThermalTool?>(null) }
        ThermalToolbar(
            selectedTool = selectedTool,
            onToolSelected = { selectedTool = it }
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\TimeDownCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TimeDownCompose(
    initialSeconds: Int,
    isVisible: Boolean = true,
    onFinish: () -> Unit = {},
    onTimeChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    var isRunning by remember(initialSeconds) { mutableStateOf(initialSeconds > 0) }
    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isRunning && isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isRunning && isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    // Countdown effect
    LaunchedEffect(initialSeconds, isVisible) {
        if (initialSeconds > 0 && isVisible) {
            currentTime = initialSeconds
            isRunning = true
            repeat(initialSeconds) { index ->
                val remainingTime = initialSeconds - index
                currentTime = remainingTime
                onTimeChange(remainingTime)
                if (remainingTime > 1) {
                    delay(1000)
                } else {
                    delay(1000)
                    isRunning = false
                    onFinish()
                    return@LaunchedEffect
                }
            }
        } else {
            isRunning = false
            if (initialSeconds == 0) {
                onFinish()
            }
        }
    }
    AnimatedVisibility(
        visible = isVisible && isRunning,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
        ) {
            Text(
                text = currentTime.toString(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}

@Composable
fun TimeDownStatefulCompose(
    seconds: Int,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember(seconds) { mutableStateOf(seconds > 0) }
    TimeDownCompose(
        initialSeconds = seconds,
        isVisible = isVisible,
        onFinish = {
            isVisible = false
            onFinish()
        },
        modifier = modifier
    )
}

@Composable
fun TimeDownComposePreview() {
    var seconds by remember { mutableIntStateOf(5) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TimeDownStatefulCompose(
            seconds = seconds,
            onFinish = {
                // Reset for preview
                seconds = 5
            },
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { seconds = 5 }
        ) {
            Text("Start Countdown")
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\TrendViewCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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

@Composable
fun TrendViewCompose(
    temperatureData: List<Float>,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Header - always visible
            TrendHeader(
                isExpanded = isExpanded,
                onExpandToggle = onExpandToggle,
                dataCount = temperatureData.size
            )
            // Chart content - expandable
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TrendChart(
                    temperatureData = temperatureData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TrendHeader(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    dataCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Temperature Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$dataCount data points",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TrendChart(
    temperatureData: List<Float>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    if (temperatureData.isEmpty()) {
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
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    Canvas(modifier = modifier) {
        drawTrendChart(temperatureData)
    }
}

private fun DrawScope.drawTrendChart(temperatureData: List<Float>) {
    if (temperatureData.size < 2) return
    val minTemp = temperatureData.minOrNull() ?: 0f
    val maxTemp = temperatureData.maxOrNull() ?: 100f
    val tempRange = maxTemp - minTemp
    if (tempRange == 0f) return
    val width = size.width
    val height = size.height
    val padding = 20.dp.toPx()
    val chartWidth = width - 2 * padding
    val chartHeight = height - 2 * padding
    // Draw grid lines
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val gridLineCount = 5
    for (i in 0..gridLineCount) {
        val y = padding + (chartHeight * i / gridLineCount)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(width - padding, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    // Draw temperature line
    val path = Path()
    var isFirstPoint = true
    temperatureData.forEachIndexed { index, temp ->
        val x = padding + (chartWidth * index / (temperatureData.size - 1))
        val y = padding + chartHeight - ((temp - minTemp) / tempRange * chartHeight)
        if (isFirstPoint) {
            path.moveTo(x, y)
            isFirstPoint = false
        } else {
            path.lineTo(x, y)
        }
        // Draw data points
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
    // Draw the trend line
    drawPath(
        path = path,
        color = Color(0xFF2196F3),
        style = Stroke(width = 2.dp.toPx())
    )
    // Draw temperature labels
    val textPaint = android.graphics.Paint().apply {
        textSize = 12.dp.toPx()
        color = android.graphics.Color.GRAY
        isAntiAlias = true
    }
    // Max temp label
    drawContext.canvas.nativeCanvas.drawText(
        "${maxTemp.toInt()}Â°",
        padding,
        padding,
        textPaint
    )
    // Min temp label
    drawContext.canvas.nativeCanvas.drawText(
        "${minTemp.toInt()}Â°",
        padding,
        height - padding + 15.dp.toPx(),
        textPaint
    )
}

@Composable
fun TrendViewStatefulCompose(
    temperatureData: List<Float>,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    TrendViewCompose(
        temperatureData = temperatureData,
        isExpanded = isExpanded,
        onExpandToggle = { isExpanded = !isExpanded },
        modifier = modifier
    )
}

@Composable
fun TrendViewComposePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sample temperature data
        val sampleData = listOf(20f, 22f, 25f, 23f, 28f, 30f, 27f, 26f, 29f, 31f)
        TrendViewStatefulCompose(
            temperatureData = sampleData,
            initiallyExpanded = false
        )
        TrendViewStatefulCompose(
            temperatureData = emptyList(),
            initiallyExpanded = true
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compose\VideoPlayerCompose.kt =====

package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun VideoPlayerCompose(
    url: String?,
    title: String? = null,
    isAutoRotate: Boolean = true,
    isLockLandscape: Boolean = false,
    showFullAnimation: Boolean = true,
    needLockFull: Boolean = true,
    cacheWithPlay: Boolean = false,
    isTouchWidget: Boolean = true,
    onVideoCallback: VideoPlayerCallback? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var volume by remember { mutableStateOf(1f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface placeholder (would integrate with actual video player)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (url.isNullOrEmpty()) {
                EmptyVideoStateCompose()
            } else {
                // In actual implementation, this would be the video surface
                VideoSurfaceCompose(
                    url = url,
                    isPlaying = isPlaying,
                    isLoading = isLoading
                )
            }
        }
        // Video controls overlay
        if (isTouchWidget) {
            VideoControlsOverlayCompose(
                isPlaying = isPlaying,
                isLoading = isLoading,
                isFullscreen = isFullscreen,
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                title = title,
                onPlayPause = {
                    isPlaying = !isPlaying
                    onVideoCallback?.onPlayStateChanged(isPlaying)
                },
                onSeek = { position ->
                    currentPosition = position
                    onVideoCallback?.onSeekTo(position)
                },
                onVolumeChange = { newVolume ->
                    volume = newVolume
                    onVideoCallback?.onVolumeChanged(newVolume)
                },
                onFullscreenToggle = {
                    if (!isLockLandscape || !needLockFull) {
                        isFullscreen = !isFullscreen
                        onVideoCallback?.onFullscreenChanged(isFullscreen)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyVideoStateCompose() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideocamOff,
            contentDescription = "No video",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No video source",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun VideoSurfaceCompose(
    url: String,
    isPlaying: Boolean,
    isLoading: Boolean
) {
    // Placeholder for video surface - in actual implementation would use ExoPlayer or similar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoading) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPlaying) "Playing" else "Paused",
                modifier = Modifier.size(80.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
        // URL display for preview
        Text(
            text = "Video: ${url.takeLast(30)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

@Composable
private fun VideoControlsOverlayCompose(
    isPlaying: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    title: String?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            // Auto-hide controls after 3 seconds when playing
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    Box(
        modifier = modifier.clickable { showControls = !showControls }
    ) {
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            // Top bar with title and fullscreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.6f)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title ?: "Video",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onFullscreenToggle
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
        // Center play/pause button
        AnimatedVisibility(
            visible = showControls && !isLoading,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = scaleOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            FloatingActionButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
        // Bottom controls
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VideoBottomControlsCompose(
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                showVolumeSlider = showVolumeSlider,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onVolumeToggle = { showVolumeSlider = !showVolumeSlider }
            )
        }
    }
}

@Composable
private fun VideoBottomControlsCompose(
    currentPosition: Long,
    duration: Long,
    volume: Float,
    showVolumeSlider: Boolean,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.6f)
            )
            .padding(16.dp)
    ) {
        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(48.dp)
            )
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { progress ->
                    onSeek((progress * duration).toLong())
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End
            )
        }
        // Volume controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = showVolumeSlider,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.width(100.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            IconButton(
                onClick = onVolumeToggle
            ) {
                Icon(
                    imageVector = when {
                        volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                        volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ThermalImageLoaderCompose(
    url: String?,
    contentDescription: String? = null,
    placeholderIcon: ImageVector = Icons.Default.Image,
    errorIcon: ImageVector = Icons.Default.BrokenImage,
    modifier: Modifier = Modifier,
    onImageLoad: (() -> Unit)? = null,
    onImageError: (() -> Unit)? = null
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        onSuccess = { onImageLoad?.invoke() },
        onError = { onImageError?.invoke() }
    )
}

@Composable
fun MediaUtilsCompose(
    mediaFiles: List<MediaFile>,
    selectedFile: MediaFile? = null,
    onFileSelected: (MediaFile) -> Unit = {},
    onFileDelete: (MediaFile) -> Unit = {},
    onFileShare: (MediaFile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaFiles) { file ->
            MediaFileItemCompose(
                file = file,
                isSelected = file == selectedFile,
                onSelect = { onFileSelected(file) },
                onDelete = { onFileDelete(file) },
                onShare = { onFileShare(file) }
            )
        }
    }
}

@Composable
private fun MediaFileItemCompose(
    file: MediaFile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (file.type) {
                    MediaFileType.VIDEO -> Icons.Default.VideoFile
                    MediaFileType.IMAGE -> Icons.Default.Image
                    MediaFileType.AUDIO -> Icons.Default.AudioFile
                },
                contentDescription = file.type.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${formatFileSize(file.size)} â€¢ ${formatTime(file.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Helper functions
private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
    } else {
        "%02d:%02d".format(minutes, seconds % 60)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}

// Data classes and interfaces
interface VideoPlayerCallback {
    fun onPlayStateChanged(isPlaying: Boolean)
    fun onSeekTo(position: Long)
    fun onVolumeChanged(volume: Float)
    fun onFullscreenChanged(isFullscreen: Boolean)
}

data class MediaFile(
    val id: String,
    val name: String,
    val path: String,
    val type: MediaFileType,
    val size: Long,
    val duration: Long,
    val thumbnailPath: String? = null
)

enum class MediaFileType {
    VIDEO, IMAGE, AUDIO
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun VideoPlayerComposePreview() {
    MaterialTheme {
        VideoPlayerCompose(
            url = "https://example.com/video.mp4",
            title = "Thermal Video Recording",
            modifier = Modifier.size(400.dp, 300.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaUtilsComposePreview() {
    MaterialTheme {
        MediaUtilsCompose(
            mediaFiles = listOf(
                MediaFile("1", "thermal_video_1.mp4", "/path/1", MediaFileType.VIDEO, 1024000, 30000),
                MediaFile("2", "thermal_image_1.jpg", "/path/2", MediaFileType.IMAGE, 512000, 0),
                MediaFile("3", "thermal_audio_1.mp3", "/path/3", MediaFileType.AUDIO, 256000, 45000)
            ),
            selectedFile = null,
            modifier = Modifier.height(300.dp)
        )
    }
}


