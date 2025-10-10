package mpdc4gsr.core.designsystem.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.designsystem.model.SensorState
import mpdc4gsr.core.designsystem.model.ThermalAction
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (ThermalAction) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // TODO: Replace with real thermal data from ThermalCaptureCoordinator via ViewModel
    var centerTemp by remember { mutableFloatStateOf(25.6f) }
    var maxTemp by remember { mutableFloatStateOf(45.2f) }
    var minTemp by remember { mutableFloatStateOf(18.9f) }
    var deviceType by remember { mutableStateOf("TC001") }
    // Simulate thermal data updates when streaming
    LaunchedEffect(state) {
        if (state == SensorState.Streaming) {
            while (true) {
                kotlinx.coroutines.delay(200)
                centerTemp = 20.0f + kotlin.random.Random.nextFloat() * 15.0f
                maxTemp = centerTemp + kotlin.random.Random.nextFloat() * 10.0f
                minTemp = centerTemp - kotlin.random.Random.nextFloat() * 10.0f
            }
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header with thermal icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Thermal Camera",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "Thermal IR Camera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "TOPDON $deviceType",
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                }
                Surface(
                    color = getStatusColor(state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = state.name,
                        color = getStatusColor(state),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            // Thermal preview visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                ThermalPreviewVisualization(
                    centerTemp = centerTemp,
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    isStreaming = state == SensorState.Streaming,
                )
            }
            // Temperature metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem(
                    label = "Center",
                    value = "${String.format("%.1f", centerTemp)}°C",
                    color = Color.White,
                )
                MetricItem(
                    label = "Max",
                    value = "${String.format("%.1f", maxTemp)}°C",
                    color = Color.Red,
                )
                MetricItem(
                    label = "Min",
                    value = "${String.format("%.1f", minTemp)}°C",
                    color = MaterialTheme.colorScheme.primary,
                )
                MetricItem(
                    label = "Resolution",
                    value = "256×192",
                    color = Color.Cyan,
                )
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                when (state) {
                    SensorState.Disconnected -> {
                        Button(
                            onClick = { onAction(ThermalAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = {
                                onClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Preview")
                        }
                        Button(
                            onClick = { onAction(ThermalAction.Calibrate) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                        ) {
                            Text("Calibrate")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(ThermalAction.StopPreview) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = {
                                onSettingsClick?.invoke()
                            },
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Thermal Settings",
                                tint = Color.White,
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(ThermalAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                        ) {
                            Text("Retry")
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ThermalPreviewVisualization(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
        ) {
            if (isStreaming) {
                // Thermal preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val width = size.width
                    val height = size.height
                    // Draw thermal background pattern
                    drawRect(
                        color = Color(0xFF1A1A2E),
                        size = size,
                    )
                    // Draw thermal hotspots based on temperature data
                    val hotspotRadius = 30f
                    // Max temperature hotspot (red)
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.8f),
                        radius = hotspotRadius,
                        center = Offset(width * 0.7f, height * 0.3f),
                    )
                    // Min temperature spot (blue)
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.8f),
                        radius = hotspotRadius * 0.7f,
                        center = Offset(width * 0.3f, height * 0.7f),
                    )
                    // Center temperature area (gradient)
                    val centerColor =
                        when {
                            centerTemp > 30f -> Color.Yellow
                            centerTemp > 20f -> Color.Green
                            else -> Color.Cyan
                        }
                    drawCircle(
                        color = centerColor.copy(alpha = 0.6f),
                        radius = hotspotRadius * 0.8f,
                        center = Offset(width * 0.5f, height * 0.5f),
                    )
                }
                // Temperature overlays
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Max temp indicator
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "${String.format("%.1f", maxTemp)}°C",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    // Min temp indicator
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "${String.format("%.1f", minTemp)}°C",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    // Center crosshair
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val crosshairSize = 15f
                        // Draw crosshair
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(centerX - crosshairSize, centerY),
                            end = Offset(centerX + crosshairSize, centerY),
                            strokeWidth = 2.dp.toPx(),
                        )
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(centerX, centerY - crosshairSize),
                            end = Offset(centerX, centerY + crosshairSize),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Thermal Preview (256×192)",
                        color = Color.Gray,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun getStatusColor(state: SensorState): Color =
    when (state) {
        SensorState.Connected -> Color.Green
        SensorState.Streaming -> MaterialTheme.colorScheme.primary
        SensorState.Error -> Color.Red
        SensorState.Disconnected -> Color.Gray
        SensorState.Connecting -> Color.Yellow
        SensorState.Simulation -> Color.Magenta
    }

@Preview(showBackground = true)
@Composable
private fun ThermalSensorCardPreview() {
    IRCameraTheme {
        ThermalSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {},
            onSettingsClick = {},
        )
    }
}
