package com.mpdc4gsr.component.thermal.compose

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
    modifier: Modifier = Modifier,
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
        modifier =
            modifier
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
                },
    ) {
        canvasHeight = size.height
        if (isInitialized) {
            drawDistanceLines(line1Y, line2Y)
        }
    }
}

private fun DrawScope.drawDistanceLines(
    line1Y: Float,
    line2Y: Float,
) {
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
        pathEffect = dashEffect,
    )
    // Draw second dashed line
    drawLine(
        color = lineColor,
        start = Offset(margin, line2Y),
        end = Offset(size.width - margin, line2Y),
        strokeWidth = strokeWidth,
        pathEffect = dashEffect,
    )
}

@Composable
fun DistanceMeasureComposePreview() {
    var distance by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier.background(Color.Black),
    ) {
        DistanceMeasureCompose(
            onDistanceChanged = { distance = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
        )
        // Show distance value for testing
        androidx.compose.material3.Text(
            text = "Distance: ${distance.toInt()}px",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

