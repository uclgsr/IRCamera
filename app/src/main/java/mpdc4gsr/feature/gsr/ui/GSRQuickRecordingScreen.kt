package mpdc4gsr.feature.gsr.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.components.common.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.math.sqrt

data class GSRReading(
    val timestamp: Long,
    val value: Double, // in microsiemens
    val quality: SignalQuality = SignalQuality.GOOD,
)

enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
}

enum class RecordingState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECORDING,
    PAUSED,
    COMPLETED,
    ERROR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(
    onNavigateBack: () -> Unit = {},
    onSaveRecording: () -> Unit = {},
) {
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var recordingDuration by remember { mutableStateOf(0) } // in seconds
    var gsrReadings by remember { mutableStateOf(listOf<GSRReading>()) }
    var currentGSRValue by remember { mutableStateOf(12.5) }
    var batteryLevel by remember { mutableStateOf(85) }
    var signalQuality by remember { mutableStateOf(SignalQuality.GOOD) }
    // Simulate GSR data updates
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (recordingState == RecordingState.RECORDING) {
                delay(100) // Update every 100ms
                recordingDuration += 1
                // Simulate GSR reading
                val newValue =
                    12.0 + 4.0 * sin(recordingDuration * 0.01) +
                            (Math.random() - 0.5) * 2.0
                currentGSRValue = newValue
                val newReading =
                    GSRReading(
                        timestamp = System.currentTimeMillis(),
                        value = newValue,
                        quality = signalQuality,
                    )
                gsrReadings = (gsrReadings + newReading).takeLast(200) // Keep last 200 readings
            }
        }
    }
    IRCameraTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
        ) {
            TitleBar(
                title = "Quick GSR Recording",
                showBackButton = true,
                onBackClick = onNavigateBack,
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save recording",
                    onClick = {
                        if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                            onSaveRecording()
                        }
                    },
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Device Status Card
                DeviceStatusCard(
                    recordingState = recordingState,
                    batteryLevel = batteryLevel,
                    signalQuality = signalQuality,
                    onConnect = { recordingState = RecordingState.CONNECTING },
                )
                // Real-time GSR Display
                if (recordingState == RecordingState.CONNECTED ||
                    recordingState == RecordingState.RECORDING ||
                    recordingState == RecordingState.PAUSED
                ) {
                    GSRDisplayCard(
                        currentValue = currentGSRValue,
                        readings = gsrReadings,
                        signalQuality = signalQuality,
                    )
                }
                // Recording Controls
                RecordingControlsCard(
                    recordingState = recordingState,
                    duration = recordingDuration,
                    onStartRecording = {
                        if (recordingState == RecordingState.CONNECTED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onPauseRecording = {
                        if (recordingState == RecordingState.RECORDING) {
                            recordingState = RecordingState.PAUSED
                        }
                    },
                    onResumeRecording = {
                        if (recordingState == RecordingState.PAUSED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onStopRecording = {
                        if (recordingState == RecordingState.RECORDING ||
                            recordingState == RecordingState.PAUSED
                        ) {
                            recordingState = RecordingState.COMPLETED
                        }
                    },
                )
                // Session Summary (when completed)
                if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                    SessionSummaryCard(readings = gsrReadings)
                }
                // Quick Setup Instructions
                if (recordingState == RecordingState.IDLE) {
                    QuickSetupCard(
                        onStartSetup = { recordingState = RecordingState.CONNECTING },
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    recordingState: RecordingState,
    batteryLevel: Int,
    signalQuality: SignalQuality,
    onConnect: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Shimmer3 GSR Device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                RecordingStateBadge(state = recordingState)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (recordingState != RecordingState.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Battery Level
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector =
                                when {
                                    batteryLevel > 75 -> Icons.Default.BatteryFull
                                    batteryLevel > 50 -> Icons.Default.Battery6Bar
                                    batteryLevel > 25 -> Icons.Default.Battery3Bar
                                    else -> Icons.Default.Battery2Bar
                                },
                            contentDescription = "Battery",
                            tint = if (batteryLevel > 25) Color(0xFF4ECDC4) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$batteryLevel%",
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                    // Signal Quality
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Signal",
                            tint =
                                when (signalQuality) {
                                    SignalQuality.EXCELLENT -> Color(0xFF4ECDC4)
                                    SignalQuality.GOOD -> Color(0xFF4ECDC4)
                                    SignalQuality.FAIR -> Color(0xFFFFB74D)
                                    SignalQuality.POOR -> Color(0xFFFF6B6B)
                                },
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text =
                                signalQuality.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B73FF),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Device")
                }
            }
        }
    }
}

@Composable
fun RecordingStateBadge(state: RecordingState) {
    val (color, text, icon) =
        when (state) {
            RecordingState.IDLE -> Triple(Color(0xFF9E9E9E), "Idle", Icons.Default.PowerOff)
            RecordingState.CONNECTING ->
                Triple(
                    Color(0xFFFFB74D),
                    "Connecting",
                    Icons.Default.Bluetooth,
                )

            RecordingState.CONNECTED ->
                Triple(
                    Color(0xFF4ECDC4),
                    "Connected",
                    Icons.Default.CheckCircle,
                )

            RecordingState.RECORDING ->
                Triple(
                    Color(0xFFFF6B6B),
                    "Recording",
                    Icons.Default.FiberManualRecord,
                )

            RecordingState.PAUSED -> Triple(Color(0xFFFFB74D), "Paused", Icons.Default.Pause)
            RecordingState.COMPLETED -> Triple(Color(0xFF4ECDC4), "Completed", Icons.Default.Done)
            RecordingState.ERROR -> Triple(Color(0xFFFF6B6B), "Error", Icons.Default.Error)
        }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

@Composable
fun GSRDisplayCard(
    currentValue: Double,
    readings: List<GSRReading>,
    signalQuality: SignalQuality,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "Live GSR Reading",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // Current Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = String.format("%.2f", currentValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ECDC4),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "μS",
                    fontSize = 16.sp,
                    color = Color(0xFFCCFFFFFF),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Waveform Display
            if (readings.isNotEmpty()) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                ) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val minValue = readings.minOf { it.value }
                    val maxValue = readings.maxOf { it.value }
                    val valueRange = if (maxValue > minValue) maxValue - minValue else 1.0
                    readings.forEachIndexed { index, reading ->
                        val x = (index.toFloat() / (readings.size - 1)) * width
                        val normalizedValue = ((reading.value - minValue) / valueRange)
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
@Composable
fun RecordingControlsCard(
    recordingState: RecordingState,
    duration: Int,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (recordingState == RecordingState.RECORDING ||
                recordingState == RecordingState.PAUSED ||
                recordingState == RecordingState.COMPLETED
            ) {
                Text(
                    text = formatDuration(duration),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (recordingState) {
                    RecordingState.CONNECTED -> {
                        FloatingActionButton(
                            onClick = onStartRecording,
                            containerColor = Color(0xFFFF6B6B),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Start recording",
                                tint = Color.White,
                            )
                        }
                    }

                    RecordingState.RECORDING -> {
                        FloatingActionButton(
                            onClick = onPauseRecording,
                            containerColor = Color(0xFFFFB74D),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause recording",
                                tint = Color.White,
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White,
                            )
                        }
                    }

                    RecordingState.PAUSED -> {
                        FloatingActionButton(
                            onClick = onResumeRecording,
                            containerColor = Color(0xFF6B73FF),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume recording",
                                tint = Color.White,
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White,
                            )
                        }
                    }

                    else -> {
                        FloatingActionButton(
                            onClick = {
                                Toast
                                    .makeText(
                                        context,
                                        "Connect a GSR device to start recording",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            },
                            containerColor = Color(0xFF404040),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Record (disabled)",
                                tint = Color(0xFF6B6B6B),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (recordingState != RecordingState.IDLE) {
                Text(
                    text =
                        when (recordingState) {
                            RecordingState.CONNECTING -> "Connecting to device..."
                            RecordingState.CONNECTED -> "Ready to record"
                            RecordingState.RECORDING -> "Recording in progress"
                            RecordingState.PAUSED -> "Recording paused"
                            RecordingState.COMPLETED -> "Recording completed"
                            RecordingState.ERROR -> "Connection error"
                            else -> ""
                        },
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF),
                )
            } else {
                Text(
                    text = "Connect your GSR device to begin recording.",
                    fontSize = 14.sp,
                    color = Color(0xFF777777),
                )
            }
        }
    }
}

fun SessionSummaryCard(readings: List<GSRReading>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "Session Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            val meanValue = readings.map { it.value }.average()
            val minValue = readings.minOf { it.value }
            val maxValue = readings.maxOf { it.value }
            val stdDev = calculateStandardDeviation(readings.map { it.value })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryMetric(
                    label = "Mean",
                    value = "${String.format("%.2f", meanValue)} μS",
                    color = Color(0xFF4ECDC4),
                )
                SummaryMetric(
                    label = "Min",
                    value = "${String.format("%.2f", minValue)} μS",
                    color = Color(0xFF6B73FF),
                )
                SummaryMetric(
                    label = "Max",
                    value = "${String.format("%.2f", maxValue)} μS",
                    color = Color(0xFFFF6B6B),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Data points: ${readings.size}",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
            )
            Text(
                text = "Standard deviation: ${String.format("%.2f", stdDev)} μS",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
            )
        }
    }
}

@Composable
fun SummaryMetric(
    label: String,
    value: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF),
        )
    }
}

@Composable
fun QuickSetupCard(onStartSetup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "Quick Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                text =
                    "1. Turn on your Shimmer3 GSR device\n" +
                            "2. Ensure Bluetooth is enabled\n" +
                            "3. Attach GSR electrodes to fingers\n" +
                            "4. Tap 'Connect Device' to begin",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Button(
                onClick = onStartSetup,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF),
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Quick Recording")
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun calculateStandardDeviation(values: List<Double>): Double {
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance)
}

@Preview(showBackground = true)
@Composable
fun GSRQuickRecordingScreenPreview() {
    GSRQuickRecordingScreen()
}
