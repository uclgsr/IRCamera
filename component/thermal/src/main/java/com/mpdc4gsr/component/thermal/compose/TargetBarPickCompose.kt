package com.mpdc4gsr.component.thermal.compose

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
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var currentProgress by remember(progress) { mutableIntStateOf(progress.coerceIn(min, max)) }
    val barSizeDp = with(density) { barSize.toDp() }
    val thumbSize = 24.dp
    val trackHeight = 8.dp
    Box(
        modifier =
            modifier
                .then(
                    if (isVertical) {
                        Modifier
                            .height(barSizeDp)
                            .width(thumbSize + 16.dp)
                    } else {
                        Modifier
                            .width(barSizeDp)
                            .height(thumbSize + 16.dp)
                    },
                ).pointerInput(min, max) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val newProgress =
                                calculateProgressFromOffset(
                                    offset = offset,
                                    size = Size(size.width.toFloat(), size.height.toFloat()),
                                    isVertical = isVertical,
                                    min = min,
                                    max = max,
                                    thumbSize = thumbSize.toPx(),
                                )
                            currentProgress = newProgress
                            onStartTrackingTouch(newProgress, max)
                            onProgressChanged(newProgress, max)
                        },
                        onDrag = { _, dragAmount ->
                            val totalSize = if (isVertical) size.height else size.width
                            val thumbSizePx = thumbSize.toPx()
                            val availableSize = totalSize - thumbSizePx
                            val progressChange =
                                if (isVertical) {
                                    -dragAmount.y / availableSize * (max - min)
                                } else {
                                    dragAmount.x / availableSize * (max - min)
                                }
                            val newProgress =
                                (currentProgress + progressChange)
                                    .roundToInt()
                                    .coerceIn(min, max)
                            if (newProgress != currentProgress) {
                                currentProgress = newProgress
                                onProgressChanged(newProgress, max)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onStopTrackingTouch(currentProgress, max)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        // Draw track and thumb
        Canvas(
            modifier = Modifier.matchParentSize(),
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
                isDragging = isDragging,
            )
        }
        // Value display
        if (label.isNotEmpty() || valueFormatter(currentProgress).isNotEmpty()) {
            val displayText =
                if (label.isNotEmpty()) {
                    "$label: ${valueFormatter(currentProgress)}"
                } else {
                    valueFormatter(currentProgress)
                }
            Card(
                modifier =
                    Modifier
                        .padding(8.dp)
                        .then(
                            if (isVertical) {
                                Modifier.offset(x = 40.dp)
                            } else {
                                Modifier.offset(y = (-40).dp)
                            },
                        ),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
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
    isDragging: Boolean,
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
            isDragging = isDragging,
        )
    } else {
        drawHorizontalBar(
            progressRatio = progressRatio,
            trackHeight = trackHeight,
            thumbSize = thumbSize,
            progressColor = progressColor,
            backgroundColor = backgroundColor,
            isDragging = isDragging,
        )
    }
}

private fun DrawScope.drawHorizontalBar(
    progressRatio: Float,
    trackHeight: Float,
    thumbSize: Float,
    progressColor: Color,
    backgroundColor: Color,
    isDragging: Boolean,
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
        cornerRadius = CornerRadius(trackHeight / 2f),
    )
    // Draw progress track
    if (progressRatio > 0) {
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(thumbSize / 2f, trackTop),
            size = Size(availableWidth * progressRatio, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )
    }
    // Draw thumb
    val thumbRadius = thumbSize / 2f
    val thumbColor = if (isDragging) progressColor.copy(alpha = 0.8f) else progressColor
    drawCircle(
        color = thumbColor,
        radius = thumbRadius,
        center = Offset(thumbCenterX, centerY),
    )
    // Draw thumb border
    drawCircle(
        color = Color.White,
        radius = thumbRadius,
        center = Offset(thumbCenterX, centerY),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun DrawScope.drawVerticalBar(
    progressRatio: Float,
    trackHeight: Float,
    thumbSize: Float,
    progressColor: Color,
    backgroundColor: Color,
    isDragging: Boolean,
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
        cornerRadius = CornerRadius(trackHeight / 2f),
    )
    // Draw progress track (from bottom up)
    if (progressRatio > 0) {
        val progressHeight = availableHeight * progressRatio
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(trackLeft, size.height - thumbSize / 2f - progressHeight),
            size = Size(trackHeight, progressHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )
    }
    // Draw thumb
    val thumbRadius = thumbSize / 2f
    val thumbColor = if (isDragging) progressColor.copy(alpha = 0.8f) else progressColor
    drawCircle(
        color = thumbColor,
        radius = thumbRadius,
        center = Offset(centerX, thumbCenterY),
    )
    // Draw thumb border
    drawCircle(
        color = Color.White,
        radius = thumbRadius,
        center = Offset(centerX, thumbCenterY),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun calculateProgressFromOffset(
    offset: Offset,
    size: androidx.compose.ui.geometry.Size,
    isVertical: Boolean,
    min: Int,
    max: Int,
    thumbSize: Float,
): Int {
    val totalRange = max - min
    if (totalRange <= 0) return min
    val ratio =
        if (isVertical) {
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TargetBarPickComposePreview() {
    var horizontalProgress by remember { mutableIntStateOf(50) }
    var verticalProgress by remember { mutableIntStateOf(75) }
    var sliderValue by remember { mutableFloatStateOf(25f) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            text = "Target Bar Components",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        // Horizontal bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Horizontal Target Bar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
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
                valueFormatter = { "$it°C" },
            )
        }
        // Vertical bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Vertical",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
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
                    progressColor = Color.Green,
                )
            }
            // Simple slider for comparison
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Simple Slider",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                SimpleTargetBarCompose(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    label = "Opacity",
                    modifier = Modifier.width(200.dp),
                )
            }
        }
        // Display values
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Current Values",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Horizontal: $horizontalProgress°C")
                Text("Vertical: $verticalProgress%")
                Text("Slider: ${String.format("%.1f", sliderValue)}")
            }
        }
    }
}

