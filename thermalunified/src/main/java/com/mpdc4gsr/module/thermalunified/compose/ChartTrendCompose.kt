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
    unit: String = "°C",
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