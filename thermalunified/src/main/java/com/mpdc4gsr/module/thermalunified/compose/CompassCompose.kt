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
    modifier: Modifier = Modifier,
) {
    val adjustedBearing = (bearing + declination) % 360f
    Card(
        modifier = modifier.size(size),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                drawCompass(
                    bearing = adjustedBearing,
                    center = this.center,
                    radius = this.size.minDimension / 2f - 20.dp.toPx(),
                )
            }
            // Bearing text
            Text(
                text = "${adjustedBearing.roundToInt()}°",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (size * 0.25f)),
            )
            // Cardinal direction
            Text(
                text = getCardinalDirection(adjustedBearing),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.offset(y = (size * 0.35f)),
            )
        }
    }
}

@Composable
fun LinearCompassCompose(
    bearing: Float,
    declination: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val adjustedBearing = (bearing + declination) % 360f
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(60.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                drawLinearCompass(
                    bearing = adjustedBearing,
                    centerY = size.height / 2f,
                )
            }
            // Center indicator
            Box(
                modifier =
                    Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color.Red)
                        .align(Alignment.Center),
            )
            // Bearing text
            Text(
                text = "${adjustedBearing.roundToInt()}° ${getCardinalDirection(adjustedBearing)}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
            )
        }
    }
}

private fun DrawScope.drawCompass(
    bearing: Float,
    center: Offset,
    radius: Float,
) {
    // Draw compass circle
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx()),
    )
    // Draw cardinal directions
    val cardinalDirections = listOf("N", "E", "S", "W")
    val cardinalAngles = listOf(0f, 90f, 180f, 270f)
    cardinalDirections.forEachIndexed { index, direction ->
        val angle = cardinalAngles[index]
        val textRadius = radius + 15.dp.toPx()
        val x = center.x + cos(Math.toRadians(angle - 90.0)).toFloat() * textRadius
        val y = center.y + sin(Math.toRadians(angle - 90.0)).toFloat() * textRadius
        val textPaint =
            android.graphics.Paint().apply {
                color = Color.White.value.toInt()
                textSize = 16.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
        drawContext.canvas.nativeCanvas.drawText(
            direction,
            x,
            y + 6.dp.toPx(),
            textPaint,
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
            strokeWidth = 1.dp.toPx(),
        )
    }
    // Draw bearing needle
    rotate(bearing, center) {
        val needleLength = radius * 0.8f
        drawLine(
            color = Color.Red,
            start = center,
            end = Offset(center.x, center.y - needleLength),
            strokeWidth = 3.dp.toPx(),
        )
        // Draw needle tip
        drawCircle(
            color = Color.Red,
            radius = 4.dp.toPx(),
            center = Offset(center.x, center.y - needleLength),
        )
    }
}

private fun DrawScope.drawLinearCompass(
    bearing: Float,
    centerY: Float,
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
                strokeWidth = 1.dp.toPx(),
            )
            // Draw degree label for major ticks
            if (tickBearing % 90f == 0f) {
                val textPaint =
                    android.graphics.Paint().apply {
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
                    textPaint,
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Compass",
            tint = quality.color,
            modifier =
                Modifier
                    .size(20.dp)
                    .rotate(bearing),
        )
        Column {
            Text(
                text = "${bearing.roundToInt()}° ${getCardinalDirection(bearing)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isCalibrating) "Calibrating..." else quality.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = quality.color,
            )
        }
    }
}

enum class CompassQuality(
    val displayName: String,
    val color: Color,
) {
    EXCELLENT("Excellent", Color.Green),
    GOOD("Good", Color.Green),
    FAIR("Fair", Color.Yellow),
    POOR("Poor", Color(0xFFFF6600)),
    UNRELIABLE("Unreliable", Color.Red),
}

@Composable
fun CompassComposePreview() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var bearing by remember { mutableFloatStateOf(45f) }
        CompassCompose(
            bearing = bearing,
            size = 150.dp,
        )
        LinearCompassCompose(
            bearing = bearing,
        )
        CompassStatusCompose(
            bearing = bearing,
            quality = CompassQuality.GOOD,
        )
        Slider(
            value = bearing,
            onValueChange = { bearing = it },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Bearing: ${bearing.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
