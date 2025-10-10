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
    modifier: Modifier = Modifier,
) {
    var currentTime by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    var isRunning by remember(initialSeconds) { mutableStateOf(initialSeconds > 0) }
    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isRunning && isVisible) 1f else 0.8f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isRunning && isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha",
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
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha,
                    ),
        ) {
            Text(
                text = currentTime.toString(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
            )
        }
    }
}

@Composable
fun TimeDownStatefulCompose(
    seconds: Int,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isVisible by remember(seconds) { mutableStateOf(seconds > 0) }
    TimeDownCompose(
        initialSeconds = seconds,
        isVisible = isVisible,
        onFinish = {
            isVisible = false
            onFinish()
        },
        modifier = modifier,
    )
}

@Composable
fun TimeDownComposePreview() {
    var seconds by remember { mutableIntStateOf(5) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimeDownStatefulCompose(
            seconds = seconds,
            onFinish = {
                // Reset for preview
                seconds = 5
            },
            modifier = Modifier.size(100.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { seconds = 5 },
        ) {
            Text("Start Countdown")
        }
    }
}
