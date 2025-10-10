package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.GSRAction
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (GSRAction) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // TODO: Replace with real GSR data from GSRSensorRecorder via ViewModel
    var gsrValue by remember { mutableFloatStateOf(2.45f) }
    var skinConductance by remember { mutableFloatStateOf(0.82f) }
    var isRecording by remember { mutableStateOf(false) }
    // Simulate GSR data updates when streaming
    LaunchedEffect(state) {
        if (state == SensorState.Streaming) {
            while (true) {
                kotlinx.coroutines.delay(100)
                gsrValue = 2.0f + kotlin.random.Random.nextFloat() * 1.5f
                skinConductance = 0.5f + kotlin.random.Random.nextFloat() * 0.8f
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
            // Header with sensor icon and status
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
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "GSR Sensor",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "GSR Sensor",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
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
            // GSR data visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                GSRDataVisualization(
                    gsrValue = gsrValue,
                    skinConductance = skinConductance,
                    isStreaming = state == SensorState.Streaming,
                )
            }
            // Sensor metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem(
                    label = "GSR Value",
                    value = "${String.format("%.2f", gsrValue)} μS",
                    color = Color.Cyan,
                )
                MetricItem(
                    label = "Conductance",
                    value = "${String.format("%.2f", skinConductance)} μS",
                    color = Color.Green,
                )
                MetricItem(
                    label = "Sampling",
                    value = if (state == SensorState.Streaming) "128 Hz" else "---",
                    color = Color.Yellow,
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
                            onClick = { onAction(GSRAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = { onAction(GSRAction.StartStream) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                        Button(
                            onClick = { onAction(GSRAction.Disconnect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        ) {
                            Text("Disconnect")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(GSRAction.StopStream) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = { onSettingsClick?.invoke() },
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(GSRAction.Connect) },
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
private fun GSRDataVisualization(
    gsrValue: Float,
    skinConductance: Float,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
        ) {
            if (isStreaming) {
                // Real-time GSR waveform
                Canvas(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2
                    // Draw baseline
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1.dp.toPx(),
                    )
                    // Draw GSR waveform
                    val path = Path()
                    val points = 100
                    val timeOffset = System.currentTimeMillis() / 100f
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val freq1 = 0.1f // Slow breathing component
                        val freq2 = 0.02f // Even slower arousal component
                        val y =
                            centerY +
                                (sin((i * freq1 + timeOffset) * 0.1f) * gsrValue * 5f) +
                                (sin((i * freq2 + timeOffset) * 0.05f) * skinConductance * 10f)
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan,
                        style =
                            androidx.compose.ui.graphics.drawscope
                                .Stroke(width = 2.dp.toPx()),
                    )
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "GSR Data Visualization",
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
            fontSize = 16.sp,
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
private fun GSRSensorCardPreview() {
    IRCameraTheme {
        GSRSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {},
            onSettingsClick = {},
        )
    }
}
