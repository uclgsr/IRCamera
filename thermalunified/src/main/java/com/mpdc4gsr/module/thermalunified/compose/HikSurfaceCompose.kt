package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.sin

@Composable
fun HikSurfaceCompose(
    thermalImageData: ByteArray? = null,
    rotateAngle: Int = 270,
    isOpenAmplify: Boolean = false,
    limitTempMin: Float = Float.MIN_VALUE,
    limitTempMax: Float = Float.MAX_VALUE,
    alarmSettings: ThermalAlarmSettings = ThermalAlarmSettings(),
    onTemperatureAlarm: (Float, ThermalAlarmType) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Calculate dimensions based on rotation and amplification
    val baseDimensions = getThermalDimensions(rotateAngle, isOpenAmplify)
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Thermal image surface
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)),
            ) {
                drawThermalSurface(
                    thermalData = thermalImageData,
                    rotateAngle = rotateAngle,
                    isAmplified = isOpenAmplify,
                    dimensions = baseDimensions,
                    alarmSettings = alarmSettings,
                )
            }
            // Overlay controls and indicators
            ThermalOverlayControls(
                rotateAngle = rotateAngle,
                isAmplified = isOpenAmplify,
                alarmSettings = alarmSettings,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            // Temperature range indicator
            if (limitTempMin != Float.MIN_VALUE || limitTempMax != Float.MAX_VALUE) {
                TemperatureRangeIndicator(
                    minTemp = if (limitTempMin != Float.MIN_VALUE) limitTempMin else null,
                    maxTemp = if (limitTempMax != Float.MAX_VALUE) limitTempMax else null,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

@Composable
private fun ThermalOverlayControls(
    rotateAngle: Int,
    isAmplified: Boolean,
    alarmSettings: ThermalAlarmSettings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Rotation indicator
        if (rotateAngle != 0) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotation",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$rotateAngle°",
                        fontSize = 10.sp,
                    )
                }
            }
        }
        // Amplification indicator
        if (isAmplified) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Amplified",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "2x",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        // Alarm indicator
        if (alarmSettings.isEnabled) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (alarmSettings.isAlarmActive) {
                                Color.Red.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            },
                    ),
            ) {
                Icon(
                    imageVector = if (alarmSettings.isAlarmActive) Icons.Default.Warning else Icons.Default.NotificationsActive,
                    contentDescription = "Thermal Alarm",
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(12.dp),
                    tint = if (alarmSettings.isAlarmActive) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TemperatureRangeIndicator(
    minTemp: Float?,
    maxTemp: Float?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = "Range",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            minTemp?.let { temp ->
                Text(
                    text = "Min: ${temp.toInt()}°C",
                    fontSize = 9.sp,
                    color = Color.Blue,
                )
            }
            maxTemp?.let { temp ->
                Text(
                    text = "Max: ${temp.toInt()}°C",
                    fontSize = 9.sp,
                    color = Color.Red,
                )
            }
        }
    }
}

private fun DrawScope.drawThermalSurface(
    thermalData: ByteArray?,
    rotateAngle: Int,
    isAmplified: Boolean,
    dimensions: ThermalDimensions,
    alarmSettings: ThermalAlarmSettings,
) {
    // Draw background
    drawRect(
        color = Color.Black,
        size = size,
    )
    if (thermalData == null) {
        // Draw placeholder thermal pattern
        drawPlaceholderThermalImage(dimensions, rotateAngle, isAmplified)
    } else {
        // Draw actual thermal data (simplified representation)
        drawThermalData(thermalData, dimensions, rotateAngle, isAmplified)
    }
    // Draw alarm overlay if active
    if (alarmSettings.isEnabled && alarmSettings.isAlarmActive) {
        drawAlarmOverlay(alarmSettings)
    }
}

private fun DrawScope.drawPlaceholderThermalImage(
    dimensions: ThermalDimensions,
    rotateAngle: Int,
    isAmplified: Boolean,
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val scale = if (isAmplified) 2f else 1f
    rotate(rotateAngle.toFloat(), pivot = Offset(centerX, centerY)) {
        // Create a gradient thermal pattern
        val gradientBrush =
            Brush.radialGradient(
                colors =
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Blue,
                    ),
                center = Offset(centerX, centerY),
                radius = kotlin.math.min(size.width, size.height) / 4f * scale,
            )
        drawRect(
            brush = gradientBrush,
            topLeft =
                Offset(
                    centerX - dimensions.width * scale / 2f,
                    centerY - dimensions.height * scale / 2f,
                ),
            size =
                Size(
                    dimensions.width * scale,
                    dimensions.height * scale,
                ),
        )
        // Add some thermal "hotspots"
        repeat(5) { i ->
            val hotspotX = centerX + (kotlin.math.cos(i * 1.2) * 30 * scale).toFloat()
            val hotspotY = centerY + (kotlin.math.sin(i * 1.2) * 30 * scale).toFloat()
            drawCircle(
                color = Color.Red.copy(alpha = 0.6f),
                radius = 15f * scale,
                center = Offset(hotspotX, hotspotY),
            )
        }
    }
}

private fun DrawScope.drawThermalData(
    thermalData: ByteArray,
    dimensions: ThermalDimensions,
    rotateAngle: Int,
    isAmplified: Boolean,
) {
    // Simplified thermal data rendering
    // In a real implementation, this would process the thermal byte array
    // and convert it to temperature values and corresponding colors
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val scale = if (isAmplified) 2f else 1f
    rotate(rotateAngle.toFloat(), pivot = Offset(centerX, centerY)) {
        // Simulate thermal data visualization
        val pixelSize = 2f * scale
        val dataWidth = dimensions.width.toInt()
        val dataHeight = dimensions.height.toInt()
        for (y in 0 until dataHeight step 4) {
            for (x in 0 until dataWidth step 4) {
                // Simulate temperature value from data
                val dataIndex = (y * dataWidth + x) * 2 // 2 bytes per pixel
                if (dataIndex + 1 < thermalData.size) {
                    val tempValue = (
                        (thermalData[dataIndex].toInt() and 0xFF) or
                            ((thermalData[dataIndex + 1].toInt() and 0xFF) shl 8)
                    )
                    // Convert to color (simplified)
                    val normalizedTemp = (tempValue % 256) / 255f
                    val color =
                        Color.hsv(
                            hue = (1f - normalizedTemp) * 240f, // Blue to red
                            saturation = 1f,
                            value = 1f,
                        )
                    drawRect(
                        color = color,
                        topLeft =
                            Offset(
                                centerX - dataWidth * scale / 2f + x * scale,
                                centerY - dataHeight * scale / 2f + y * scale,
                            ),
                        size = Size(pixelSize * 4, pixelSize * 4),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAlarmOverlay(alarmSettings: ThermalAlarmSettings) {
    // Draw pulsing red overlay for alarm
    val alpha = (sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.4).toFloat()
    drawRect(
        color = Color.Red.copy(alpha = alpha.coerceIn(0.1f, 0.7f)),
        size = size,
    )
    // Draw alarm border
    drawRect(
        color = Color.Red,
        size = size,
        style =
            androidx.compose.ui.graphics.drawscope
                .Stroke(width = 4.dp.toPx()),
    )
}

private fun getThermalDimensions(
    rotateAngle: Int,
    isAmplified: Boolean,
): ThermalDimensions {
    val multiplier = if (isAmplified) 2 else 1
    val isPortrait = rotateAngle == 90 || rotateAngle == 270
    return ThermalDimensions(
        width = (if (isPortrait) 192 else 256) * multiplier,
        height = (if (isPortrait) 256 else 192) * multiplier,
    )
}

// Data classes
data class ThermalDimensions(
    val width: Int,
    val height: Int,
)

data class ThermalAlarmSettings(
    val isEnabled: Boolean = false,
    val isAlarmActive: Boolean = false,
    val alarmType: ThermalAlarmType = ThermalAlarmType.HIGH_TEMPERATURE,
    val threshold: Float = 50f,
)

enum class ThermalAlarmType {
    HIGH_TEMPERATURE,
    LOW_TEMPERATURE,
    TEMPERATURE_RANGE,
}

@Composable
fun HikSurfaceWithAndroidView(
    rotateAngle: Int = 270,
    isOpenAmplify: Boolean = false,
    onSurfaceReady: (android.view.SurfaceView) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // In a real implementation, this would create the actual HikSurfaceView
            android.view.SurfaceView(context).apply {
                // Configure the surface view
                holder.addCallback(
                    object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            onSurfaceReady(this@apply)
                        }

                        override fun surfaceChanged(
                            holder: android.view.SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                        }

                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
                    },
                )
            }
        },
        update = { surfaceView ->
            // Update surface view properties
            // In real implementation, would update rotation, amplification, etc.
        },
    )
}

@Composable
fun HikSurfaceComposePreview() {
    var rotateAngle by remember { mutableIntStateOf(270) }
    var isAmplified by remember { mutableStateOf(false) }
    var alarmActive by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Thermal Surface Display",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    rotateAngle = (rotateAngle + 90) % 360
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate")
                Text("Rotate")
            }
            Button(
                onClick = { isAmplified = !isAmplified },
            ) {
                Icon(
                    if (isAmplified) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                    contentDescription = if (isAmplified) "Zoom Out" else "Zoom In",
                )
                Text(if (isAmplified) "1x" else "2x")
            }
            Button(
                onClick = { alarmActive = !alarmActive },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (alarmActive) Color.Red else MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Alarm")
                Text("Alarm")
            }
        }
        // Thermal surface
        HikSurfaceCompose(
            rotateAngle = rotateAngle,
            isOpenAmplify = isAmplified,
            limitTempMin = 20f,
            limitTempMax = 80f,
            alarmSettings =
                ThermalAlarmSettings(
                    isEnabled = true,
                    isAlarmActive = alarmActive,
                    threshold = 50f,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
        )
        // Status display
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Surface Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rotation: $rotateAngle°")
                Text("Amplification: ${if (isAmplified) "2x" else "1x"}")
                Text("Alarm: ${if (alarmActive) "ACTIVE" else "Inactive"}")
            }
        }
    }
}
