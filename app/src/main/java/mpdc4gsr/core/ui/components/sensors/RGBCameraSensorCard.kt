package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.model.CameraAction
import mpdc4gsr.core.ui.model.SensorState
import mpdc4gsr.core.ui.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RGBCameraSensorCard(
    state: SensorState,
    onStateChange: (SensorState) -> Unit,
    onClick: () -> Unit,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Replace with real camera parameters from Camera API via ViewModel
    var resolution by remember { mutableStateOf("1920×1080") }
    var frameRate by remember { mutableIntStateOf(30) }
    var exposureTime by remember { mutableStateOf("1/60") }
    var iso by remember { mutableIntStateOf(200) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with camera icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "RGB Camera",
                        tint = getStatusColor(state),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "RGB Camera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Built-in Camera",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = getStatusColor(state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = state.name,
                        color = getStatusColor(state),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Camera preview visualization
            if (state == SensorState.Streaming || state == SensorState.Connected) {
                RGBPreviewVisualization(
                    resolution = resolution,
                    frameRate = frameRate,
                    isStreaming = state == SensorState.Streaming
                )
            }

            // Camera metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Resolution",
                    value = resolution,
                    color = Color.White
                )
                MetricItem(
                    label = "Frame Rate",
                    value = "${frameRate} fps",
                    color = Color.Green
                )
                MetricItem(
                    label = "Exposure",
                    value = exposureTime,
                    color = Color.Yellow
                )
                MetricItem(
                    label = "ISO",
                    value = iso.toString(),
                    color = Color.Cyan
                )
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    SensorState.Disconnected -> {
                        Button(
                            onClick = { onAction(CameraAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("Connect")
                        }
                    }

                    SensorState.Connected -> {
                        Button(
                            onClick = {
                                onClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Preview")
                        }
                        Button(
                            onClick = {
                                onAction(CameraAction.SetResolution(1920, 1080))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Settings")
                        }
                    }

                    SensorState.Streaming -> {
                        Button(
                            onClick = { onAction(CameraAction.StopPreview) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        IconButton(
                            onClick = { /* TODO: Implement capture photo
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White
                            )
                        }
                    }

                    SensorState.Error -> {
                        Button(
                            onClick = { onAction(CameraAction.Connect) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
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
private fun RGBPreviewVisualization(
    resolution: String,
    frameRate: Int,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (isStreaming) {
                // RGB camera preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw camera viewfinder background
                    drawRect(
                        color = Color(0xFF2E2E2E),
                        size = size
                    )

                    // Draw sample scene elements
                    // Background gradient
                    drawRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = Offset(0f, height * 0.6f),
                        size = Size(width, height * 0.4f)
                    )

                    // Simulated objects in scene
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = 20f,
                        center = Offset(width * 0.3f, height * 0.4f)
                    )

                    drawRect(
                        color = Color(0xFF5A5A5A),
                        topLeft = Offset(width * 0.6f, height * 0.3f),
                        size = Size(width * 0.2f, height * 0.3f)
                    )

                    // Viewfinder grid lines
                    val strokeWidth = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.3f)

                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(width / 3f, 0f),
                        end = Offset(width / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(width * 2f / 3f, 0f),
                        end = Offset(width * 2f / 3f, height),
                        strokeWidth = strokeWidth
                    )

                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 3f),
                        end = Offset(width, height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height * 2f / 3f),
                        end = Offset(width, height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )
                }

                // Camera info overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Frame rate indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${frameRate}fps",
                        color = Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Focus indicator (center)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val focusSize = 25f

                    // Draw focus square
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - focusSize / 2, centerY - focusSize / 2),
                        size = Size(focusSize, focusSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            } else {
                // Static placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera Preview",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = resolution,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun getStatusColor(state: SensorState): Color {
    return when (state) {
        SensorState.Connected -> Color.Green
        SensorState.Streaming -> MaterialTheme.colorScheme.primary
        SensorState.Error -> Color.Red
        SensorState.Disconnected -> Color.Gray
        SensorState.Connecting -> Color.Yellow
        SensorState.Simulation -> Color.Magenta
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraSensorCardPreview() {
    IRCameraTheme {
        RGBCameraSensorCard(
            state = SensorState.Streaming,
            onStateChange = {},
            onClick = {},
            onAction = {}
        )
    }
}