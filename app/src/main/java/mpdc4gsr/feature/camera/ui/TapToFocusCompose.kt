package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapToFocusPreview(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize()) {
        // Camera PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewViewConfig(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        // Call the focus callback
                        onTapToFocus(normalizedX, normalizedY)
                        // Auto-hide focus indicator after delay
                        coroutineScope.launch {
                            delay(2000)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        // Focus indicator overlay
        if (showFocusIndicator && focusPoint != null) {
            FocusIndicator(focusPoint = focusPoint!!)
        }
    }
}

@Composable
private fun FocusIndicator(focusPoint: Offset) {
    val density = LocalDensity.current
    val circleRadius = with(density) { 60.dp.toPx() }
    // Animate the focus indicator
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusAlpha"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer circle
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = circleRadius,
            center = focusPoint,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        // Inner crosshair
        val crossSize = circleRadius * 0.3f
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x - crossSize, focusPoint.y),
            end = Offset(focusPoint.x + crossSize, focusPoint.y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x, focusPoint.y - crossSize),
            end = Offset(focusPoint.x, focusPoint.y + crossSize),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TapToFocusPreviewWithCustomIndicator(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    focusIndicatorColor: Color = Color.White,
    focusIndicatorRadius: Float = 60f,
    autoHideDelay: Long = 2000L,
    modifier: Modifier = Modifier
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val radiusPx = with(density) { focusIndicatorRadius.dp.toPx() }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { previewViewConfig(this) } },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        onTapToFocus(normalizedX, normalizedY)
                        coroutineScope.launch {
                            delay(autoHideDelay)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        if (showFocusIndicator && focusPoint != null) {
            CustomFocusIndicator(
                focusPoint = focusPoint!!,
                color = focusIndicatorColor,
                radius = radiusPx
            )
        }
    }
}

@Composable
private fun CustomFocusIndicator(
    focusPoint: Offset,
    color: Color,
    radius: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "customFocus")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaledRadius = radius * animatedScale
        // Animated circle
        drawCircle(
            color = color.copy(alpha = animatedAlpha),
            radius = scaledRadius,
            center = focusPoint,
            style = Stroke(width = 4f)
        )
        // Corner brackets
        val bracketSize = scaledRadius * 0.3f
        val offset = scaledRadius - bracketSize
        // Top-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset + bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y - offset),
            strokeWidth = 3f
        )
        // Top-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset + bracketSize),
            strokeWidth = 3f
        )
        // Bottom-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset - bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y + offset),
            strokeWidth = 3f
        )
        // Bottom-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset - bracketSize),
            strokeWidth = 3f
        )
    }
}
