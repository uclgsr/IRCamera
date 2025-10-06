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
                            Text("Point: ${measurement.temperature}°C at (${measurement.position.x.toInt()}, ${measurement.position.y.toInt()})")
                        }
                        is TemperatureMeasurement.Line -> {
                            Text("Line: Avg ${measurement.averageTemperature}°C, Range ${measurement.minTemperature}°C - ${measurement.maxTemperature}°C")
                        }
                        is TemperatureMeasurement.Rectangle -> {
                            Text("Rect: Avg ${measurement.averageTemperature}°C, Range ${measurement.minTemperature}°C - ${measurement.maxTemperature}°C")
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