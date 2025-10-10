package mpdc4gsr.core.ui.components.sensors

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import mpdc4gsr.core.ui.model.SensorInfo
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.ui.model.SystemAction
import mpdc4gsr.core.ui.model.UnifiedSystemState
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun UnifiedSensorStatus(
    systemState: UnifiedSystemState,
    activeSensors: List<SensorInfo>,
    onSystemAction: (SystemAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (systemState) {
                        UnifiedSystemState.Recording -> Color(0xFF2A1A1A) // Dark red tint
                        UnifiedSystemState.Active -> Color(0xFF1A2A1A) // Dark green tint
                        UnifiedSystemState.Error -> Color(0xFF2A1A1A) // Dark red tint
                        UnifiedSystemState.Inactive -> Color(0xFF2A2A2A) // Neutral
                    },
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
            // System status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Multi-Modal System",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = getSystemStatusText(systemState, activeSensors.size),
                        color = getSystemStatusColor(systemState),
                        fontSize = 14.sp,
                    )
                }
                Surface(
                    color = getSystemStatusColor(systemState).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = systemState.name,
                        color = getSystemStatusColor(systemState),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            // Sensor status visualization
            SensorStatusVisualization(
                sensors = activeSensors,
                systemState = systemState,
            )
            // System metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val connectedSensors =
                    activeSensors.count {
                        it.state == SensorState.Connected || it.state == SensorState.Streaming
                    }
                val streamingSensors = activeSensors.count { it.state == SensorState.Streaming }
                MetricItem(
                    label = "Connected",
                    value = "$connectedSensors/${activeSensors.size}",
                    color = if (connectedSensors == activeSensors.size) Color.Green else Color.Yellow,
                )
                MetricItem(
                    label = "Streaming",
                    value = streamingSensors.toString(),
                    color = if (streamingSensors > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                )
                MetricItem(
                    label = "Sync Status",
                    value = if (systemState == UnifiedSystemState.Active) "OK" else "---",
                    color = if (systemState == UnifiedSystemState.Active) Color.Green else Color.Gray,
                )
            }
            // System control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                when (systemState) {
                    UnifiedSystemState.Inactive -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Sync All")
                        }
                    }

                    UnifiedSystemState.Active -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.StartRecording) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Record All")
                        }
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Re-sync")
                        }
                    }

                    UnifiedSystemState.Recording -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.StopRecording) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop All")
                        }
                        // Show recording duration
                        var recordingTime by remember { mutableIntStateOf(0) }
                        LaunchedEffect(systemState) {
                            while (systemState == UnifiedSystemState.Recording) {
                                kotlinx.coroutines.delay(1000)
                                recordingTime++
                            }
                        }
                        Text(
                            text = "Recording: ${recordingTime}s",
                            color = Color.Red,
                            fontSize = 14.sp,
                        )
                    }

                    UnifiedSystemState.Error -> {
                        Button(
                            onClick = { onSystemAction(SystemAction.Synchronize) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                        ) {
                            Text("Recover")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusVisualization(
    sensors: List<SensorInfo>,
    systemState: UnifiedSystemState,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2
                // Draw connection lines between sensors
                if (sensors.size > 1) {
                    val sensorSpacing = width / (sensors.size + 1)
                    for (i in 0 until sensors.size - 1) {
                        val startX = sensorSpacing * (i + 1)
                        val endX = sensorSpacing * (i + 2)
                        val connectionColor =
                            if (systemState == UnifiedSystemState.Active) {
                                Color.Green
                            } else {
                                Color.Gray
                            }
                        drawLine(
                            color = connectionColor,
                            start = Offset(startX, centerY),
                            end = Offset(endX, centerY),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
                // Draw sensor nodes
                sensors.forEachIndexed { index, sensor ->
                    val x = width / (sensors.size + 1) * (index + 1)
                    val nodeColor =
                        when (sensor.state) {
                            SensorState.Connected -> Color.Green
                            SensorState.Streaming -> primaryColor
                            SensorState.Error -> Color.Red
                            SensorState.Disconnected -> Color.Gray
                            SensorState.Connecting -> Color.Yellow
                            SensorState.Simulation -> Color.Magenta
                        }
                    // Draw sensor node
                    drawCircle(
                        color = nodeColor,
                        radius = 12.dp.toPx(),
                        center = Offset(x, centerY),
                    )
                    // Draw sensor type indicator
                    val innerColor =
                        when (sensor.type) {
                            SensorType.GSR -> Color.Cyan
                            SensorType.ThermalIR -> Color.Red
                            SensorType.RGBCamera -> Color.White
                        }
                    drawCircle(
                        color = innerColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, centerY),
                    )
                }
            }
            // Sensor labels
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                sensors.forEach { sensor ->
                    Text(
                        text =
                            when (sensor.type) {
                                SensorType.GSR -> "GSR"
                                SensorType.ThermalIR -> "IR"
                                SensorType.RGBCamera -> "RGB"
                            },
                        color = Color.White,
                        fontSize = 10.sp,
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

private fun getSystemStatusText(
    state: UnifiedSystemState,
    sensorCount: Int,
): String =
    when (state) {
        UnifiedSystemState.Inactive -> "$sensorCount sensors available"
        UnifiedSystemState.Active -> "All systems operational"
        UnifiedSystemState.Recording -> "Multi-modal recording active"
        UnifiedSystemState.Error -> "System error - check sensors"
    }

private fun getSystemStatusColor(state: UnifiedSystemState): Color =
    when (state) {
        UnifiedSystemState.Inactive -> Color.Gray
        UnifiedSystemState.Active -> Color.Green
        UnifiedSystemState.Recording -> Color.Red
        UnifiedSystemState.Error -> Color.Red
    }

@Preview(showBackground = true)
@Composable
private fun UnifiedSensorStatusPreview() {
    IRCameraTheme {
        UnifiedSensorStatus(
            systemState = UnifiedSystemState.Active,
            activeSensors =
                listOf(
                    SensorInfo(SensorType.GSR, SensorState.Streaming),
                    SensorInfo(SensorType.ThermalIR, SensorState.Connected),
                    SensorInfo(SensorType.RGBCamera, SensorState.Streaming),
                ),
            onSystemAction = {},
        )
    }
}
