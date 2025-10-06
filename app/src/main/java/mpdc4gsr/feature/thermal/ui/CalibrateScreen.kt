package mpdc4gsr.feature.thermal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun CalibrateScreen(
    onBackClick: (() -> Unit)? = null,
    onCalibrationComplete: () -> Unit = {},
    onCalibrationCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var calibrationStep by remember { mutableIntStateOf(1) }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }
    // Simulate calibration progress
    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            while (calibrationProgress < 1f && isCalibrating) {
                kotlinx.coroutines.delay(100)
                calibrationProgress += 0.05f
            }
            if (calibrationProgress >= 1f) {
                onCalibrationComplete()
                isCalibrating = false
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar with completion actions
        TitleBar(
            title = "Camera Calibration",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            if (!isCalibrating) {
                TitleBarAction(
                    icon = Icons.Default.Close,
                    contentDescription = "Cancel calibration",
                    onClick = onCalibrationCancel
                )
                TitleBarAction(
                    icon = Icons.Default.Check,
                    contentDescription = "Complete calibration",
                    onClick = { isCalibrating = true }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Calibration instructions
            CalibrationInstructions(
                step = calibrationStep,
                isCalibrating = isCalibrating
            )
            // Dual camera preview with alignment overlay
            DualCameraPreview(
                step = calibrationStep,
                isCalibrating = isCalibrating,
                progress = calibrationProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            // Progress indicator
            if (isCalibrating) {
                CalibrationProgress(progress = calibrationProgress)
            } else {
                // Step controls
                CalibrationStepControls(
                    currentStep = calibrationStep,
                    onStepChange = { calibrationStep = it },
                    onStartCalibration = { isCalibrating = true }
                )
            }
        }
    }
}

@Composable
private fun CalibrationInstructions(
    step: Int,
    isCalibrating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isCalibrating) "Calibrating..." else "Step $step of 3",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val instruction = when {
                isCalibrating -> "Please wait while the thermal and RGB cameras are being aligned. Keep the device steady."
                step == 1 -> "Point both cameras at a distinctive object with clear temperature contrast (e.g., a warm cup on a cool table)."
                step == 2 -> "Align the crosshairs with the same point on the object in both camera views."
                step == 3 -> "Verify the alignment and tap the check button to complete calibration."
                else -> "Calibration complete. The thermal and RGB cameras are now aligned."
            }
            Text(
                text = instruction,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun DualCameraPreview(
    step: Int,
    isCalibrating: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thermal camera view
            ThermalCameraView(
                step = step,
                isCalibrating = isCalibrating,
                progress = progress,
                modifier = Modifier.weight(1f)
            )
            // RGB camera view
            RGBCameraView(
                step = step,
                isCalibrating = isCalibrating,
                progress = progress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThermalCameraView(
    step: Int,
    isCalibrating: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF1A1A2E))
    ) {
        // Thermal camera preview with realistic thermal imaging
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isCalibrating) 0.7f else 1f)
        ) {
            // Draw sample thermal image with gradient
            drawRect(
                color = Color(0xFF1A1A2E),
                size = size
            )
            // Draw thermal hotspots
            drawCircle(
                color = Color.Red,
                radius = 30f,
                center = Offset(size.width * 0.3f, size.height * 0.4f)
            )
            drawCircle(
                color = Color.Yellow,
                radius = 20f,
                center = Offset(size.width * 0.7f, size.height * 0.6f)
            )
            drawCircle(
                color = primaryColor,
                radius = 15f,
                center = Offset(size.width * 0.5f, size.height * 0.2f)
            )
            // Draw crosshair
            val centerX = size.width / 2
            val centerY = size.height / 2
            drawLine(
                color = Color.White,
                start = Offset(centerX - 20, centerY),
                end = Offset(centerX + 20, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY - 20),
                end = Offset(centerX, centerY + 20),
                strokeWidth = 2f
            )
            // Sample thermal hotspot
            drawCircle(
                color = Color.Red,
                radius = 40f,
                center = Offset(size.width * 0.6f, size.height * 0.4f)
            )
            // Draw calibration overlays
            drawCalibrationOverlay(step, isCalibrating, progress, size)
        }
        // Camera label
        Text(
            text = "Thermal",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        )
    }
}

@Composable
private fun RGBCameraView(
    step: Int,
    isCalibrating: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF2E2E2E))
    ) {
        // RGB camera preview with realistic camera view
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isCalibrating) 0.7f else 1f)
        ) {
            // Draw sample RGB camera background
            drawRect(
                color = Color(0xFF4A4A4A),
                size = size
            )
            // Draw some sample objects
            drawRoundRect(
                color = Color(0xFF6A6A6A),
                size = Size(size.width * 0.3f, size.height * 0.2f),
                topLeft = Offset(size.width * 0.1f, size.height * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            drawCircle(
                color = Color(0xFF8A8A8A),
                radius = 25f,
                center = Offset(size.width * 0.8f, size.height * 0.3f)
            )
            // Draw calibration crosshair
            val centerX = size.width / 2
            val centerY = size.height / 2
            drawLine(
                color = Color.Green,
                start = Offset(centerX - 20, centerY),
                end = Offset(centerX + 20, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Green,
                start = Offset(centerX, centerY - 20),
                end = Offset(centerX, centerY + 20),
                strokeWidth = 2f
            )
            drawRect(
                color = Color(0xFF2E2E2E),
                size = size
            )
            // Sample RGB features
            drawRect(
                color = Color.Gray,
                topLeft = Offset(size.width * 0.5f, size.height * 0.3f),
                size = Size(size.width * 0.2f, size.height * 0.2f)
            )
            // Draw calibration overlays
            drawCalibrationOverlay(step, isCalibrating, progress, size)
        }
        // Camera label
        Text(
            text = "RGB",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        )
    }
}

private fun DrawScope.drawCalibrationOverlay(
    step: Int,
    isCalibrating: Boolean,
    progress: Float,
    canvasSize: Size
) {
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    when {
        isCalibrating -> {
            // Draw progress indicator
            val radius = 50f
            drawCircle(
                color = Color.Yellow.copy(alpha = 0.3f),
                radius = radius,
                center = center
            )
            drawArc(
                color = Color.Yellow,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx()),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        step >= 1 -> {
            // Draw crosshair for alignment
            val crosshairSize = 30f
            val strokeWidth = 2.dp.toPx()
            // Horizontal line
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x - crosshairSize, center.y),
                end = Offset(center.x + crosshairSize, center.y),
                strokeWidth = strokeWidth
            )
            // Vertical line
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x, center.y - crosshairSize),
                end = Offset(center.x, center.y + crosshairSize),
                strokeWidth = strokeWidth
            )
            // Center dot
            drawCircle(
                color = Color.Yellow,
                radius = 3f,
                center = center
            )
        }
    }
    if (step >= 2 && !isCalibrating) {
        // Draw alignment grid
        val gridSpacing = canvasSize.width / 6
        for (i in 1..5) {
            val x = i * gridSpacing
            drawLine(
                color = Color.Yellow.copy(alpha = 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, canvasSize.height),
                strokeWidth = 1.dp.toPx()
            )
        }
        val gridSpacingY = canvasSize.height / 6
        for (i in 1..5) {
            val y = i * gridSpacingY
            drawLine(
                color = Color.Yellow.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(canvasSize.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun CalibrationProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calibrating... ${(progress * 100).toInt()}%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Color.Yellow,
            trackColor = Color.Gray,
        )
    }
}

@Composable
private fun CalibrationStepControls(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onStartCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { step ->
                val stepNumber = step + 1
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(50),
                    color = if (stepNumber <= currentStep) Color.Yellow else Color.Gray
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber.toString(),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (currentStep > 1) {
                Button(
                    onClick = { onStepChange(currentStep - 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Previous")
                }
            }
            if (currentStep < 3) {
                Button(
                    onClick = { onStepChange(currentStep + 1) }
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onStartCalibration,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text("Start Calibration")
                }
            }
        }
    }
}

@Composable
fun CalibrationOverlay(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = Color(0xFF16131e),
            shape = RoundedCornerShape(12.dp)
        ) {
            CalibrateScreen(
                onBackClick = onDismiss,
                onCalibrationComplete = onComplete,
                onCalibrationCancel = onDismiss
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalibrateScreenPreview() {
    IRCameraTheme {
        CalibrateScreen()
    }
}