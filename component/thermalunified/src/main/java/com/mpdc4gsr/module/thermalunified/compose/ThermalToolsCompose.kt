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
                val tempText = "${currentTemp.roundToInt()}°C"
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
        val infoText = "$label\n${temperature.roundToInt()}°C"
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