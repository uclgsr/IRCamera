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
                        text = "${measurement.temperature.roundToInt()}°C",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = measurement.color
                    )
                }

                is TemperatureMeasurementResult.Line -> {
                    Column {
                        Text("Max: ${measurement.temperatureMax.roundToInt()}°C", fontSize = 12.sp)
                        Text("Avg: ${measurement.temperatureAvg.roundToInt()}°C", fontSize = 12.sp)
                        Text("Min: ${measurement.temperatureMin.roundToInt()}°C", fontSize = 12.sp)
                    }
                }

                is TemperatureMeasurementResult.Rectangle -> {
                    Column {
                        Text("Max: ${measurement.temperatureMax.roundToInt()}°C", fontSize = 12.sp)
                        Text("Avg: ${measurement.temperatureAvg.roundToInt()}°C", fontSize = 12.sp)
                        Text("Min: ${measurement.temperatureMin.roundToInt()}°C", fontSize = 12.sp)
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