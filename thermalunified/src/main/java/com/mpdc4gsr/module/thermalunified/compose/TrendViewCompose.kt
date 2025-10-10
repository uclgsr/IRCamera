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
        "${maxTemp.toInt()}°",
        padding,
        padding,
        textPaint
    )
    // Min temp label
    drawContext.canvas.nativeCanvas.drawText(
        "${minTemp.toInt()}°",
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