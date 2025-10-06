package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRVisualizationCard(
    gsrData: GSRData,
    connectionState: GSRConnectionState,
    modifier: Modifier = Modifier,
    onExportData: () -> Unit = {},
    onResetStatistics: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with connection and battery status
            GSRSensorHeader(
                connectionState = connectionState,
                batteryLevel = gsrData.batteryLevel,
                sampleRate = gsrData.sampleRate
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Real-time data chart
            GSRDataChart(
                recentReadings = gsrData.recentReadings,
                currentValue = gsrData.currentValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Current reading and statistics
            GSRStatistics(
                currentValue = gsrData.currentValue,
                averageValue = gsrData.averageValue,
                minValue = gsrData.minValue,
                maxValue = gsrData.maxValue
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Data export controls
            GSRDataControls(
                onExportData = onExportData,
                onResetStatistics = onResetStatistics
            )
        }
    }
}

@Composable
private fun GSRSensorHeader(
    connectionState: GSRConnectionState,
    batteryLevel: Int,
    sampleRate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GSR Sensor (Shimmer3)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sample Rate: $sampleRate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status
            Icon(
                if (connectionState.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (connectionState.isConnected) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Battery level
            val batteryIcon = when {
                batteryLevel > 80 -> Icons.Default.BatteryFull
                batteryLevel > 30 -> Icons.Default.Battery3Bar
                else -> Icons.Default.Battery1Bar
            }
            val batteryColor = when {
                batteryLevel > 50 -> Color.Green
                batteryLevel > 20 -> Color.Yellow
                else -> Color.Red
            }
            Icon(
                batteryIcon,
                contentDescription = null,
                tint = batteryColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$batteryLevel%",
                style = MaterialTheme.typography.labelSmall,
                color = batteryColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun GSRDataChart(
    recentReadings: List<Float>,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Real-time GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Chart visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val width = size.width
                val height = size.height
                val strokeWidth = 3.dp.toPx()
                if (recentReadings.isNotEmpty()) {
                    val maxValue = recentReadings.maxOrNull() ?: 1f
                    val minValue = recentReadings.minOrNull() ?: 0f
                    val range = maxValue - minValue
                    val path = Path()
                    recentReadings.forEachIndexed { index, value ->
                        val x = (index.toFloat() / (recentReadings.size - 1)) * width
                        val y = height - ((value - minValue) / range) * height
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4), // Teal color for GSR data
                        style = Stroke(width = strokeWidth)
                    )
                    // Current value indicator
                    val currentX = width
                    val currentY = height - ((currentValue - minValue) / range) * height
                    drawCircle(
                        color = Color(0xFF4ECDC4),
                        radius = 6.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRStatistics(
    currentValue: Float,
    averageValue: Float,
    minValue: Float,
    maxValue: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Current", currentValue, "kΩ")
                StatisticItem("Average", averageValue, "kΩ")
                StatisticItem("Min", minValue, "kΩ")
                StatisticItem("Max", maxValue, "kΩ")
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: Float,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "${String.format("%.2f", value)} $unit",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GSRDataControls(
    onExportData: () -> Unit,
    onResetStatistics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onExportData,
            modifier = Modifier.weight(1f)
        ) {
            Text("Export Data")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onResetStatistics,
            modifier = Modifier.weight(1f)
        ) {
            Text("Reset Stats")
        }
    }
}

// Data classes for GSR visualization
data class GSRData(
    val currentValue: Float,
    val batteryLevel: Int,
    val sampleRate: String = "51.2Hz",
    val recentReadings: List<Float> = emptyList(),
    val averageValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 0f
)

data class GSRConnectionState(
    val isConnected: Boolean,
    val deviceName: String = "",
    val connectionStrength: Int = 0
)