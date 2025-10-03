package mpdc4gsr.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SensorStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    ERROR,
    SIMULATION
}

data class SensorInfo(
    val id: String,
    val name: String,
    val status: SensorStatus,
    val message: String,
    val icon: ImageVector,
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never",
    val signalStrength: Int = 0,
    val isAnimating: Boolean = false
)

@Composable
fun SensorDashboardCompose(
    sensors: List<SensorInfo>,
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DashboardHeader(
                connectedCount = sensors.count { it.status == SensorStatus.CONNECTED || it.status == SensorStatus.STREAMING },
                totalCount = sensors.size,
                onRefresh = onRefresh
            )
        }

        items(sensors) { sensor ->
            SensorCard(
                sensor = sensor,
                onClick = { onSensorClick(sensor) }
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    connectedCount: Int,
    totalCount: Int,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sensor Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$connectedCount/$totalCount sensors connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    sensor: SensorInfo,
    onClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (sensor.status) {
            SensorStatus.CONNECTED -> Color(0xFF4CAF50)
            SensorStatus.STREAMING -> Color(0xFF2196F3)
            SensorStatus.ERROR -> Color(0xFFF44336)
            SensorStatus.CONNECTING -> Color(0xFFFF9800)
            SensorStatus.SIMULATION -> Color(0xFFFFEB3B)
            SensorStatus.DISCONNECTED -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(durationMillis = 300)
    )

    val scale by animateFloatAsState(
        targetValue = if (sensor.isAnimating) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Sensor icon
            Icon(
                imageVector = sensor.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = statusColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Sensor information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sensor.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sensor.status == SensorStatus.CONNECTED || sensor.status == SensorStatus.STREAMING) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sensor.dataRate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sensor.lastUpdate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status badge
            StatusBadge(status = sensor.status)
        }
    }
}

@Composable
private fun StatusBadge(status: SensorStatus) {
    val (text, containerColor, textColor) = when (status) {
        SensorStatus.CONNECTED -> Triple("Connected", Color(0xFF4CAF50), Color.White)
        SensorStatus.STREAMING -> Triple("Streaming", Color(0xFF2196F3), Color.White)
        SensorStatus.ERROR -> Triple("Error", Color(0xFFF44336), Color.White)
        SensorStatus.CONNECTING -> Triple("Connecting", Color(0xFFFF9800), Color.White)
        SensorStatus.SIMULATION -> Triple("Simulation", Color(0xFFFFEB3B), Color.Black)
        SensorStatus.DISCONNECTED -> Triple("Disconnected", Color(0xFF9E9E9E), Color.White)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SensorStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = textColor,
                    strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontSize = 10.sp
            )
        }
    }
}

// Sample data generator for testing - returns disconnected sensors by default
fun getSampleSensorData(): List<SensorInfo> {
    return listOf(
        SensorInfo(
            id = "thermal_camera",
            name = "Thermal Camera",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Thermostat,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "rgb_camera",
            name = "RGB Camera",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Camera,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "shimmer_gsr",
            name = "Shimmer GSR",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Sensors,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "bluetooth_device",
            name = "Bluetooth Device",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Bluetooth,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "network_device",
            name = "Network Sync",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.NetworkCheck,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        ),
        SensorInfo(
            id = "storage_device",
            name = "Storage System",
            status = SensorStatus.DISCONNECTED,
            message = "Not connected - tap to connect",
            icon = Icons.Default.Storage,
            dataRate = "0 KB/s",
            lastUpdate = "Never",
            signalStrength = 0
        )
    )
}

// Static sensor dashboard for demo purposes - shows disconnected sensors
// For actual sensor connections, use SensorDashboardCompose with real sensor data
@Composable
fun SensorDashboardDemo(
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val sensors = remember { getSampleSensorData() }

    SensorDashboardCompose(
        sensors = sensors,
        modifier = modifier,
        onSensorClick = onSensorClick,
        onRefresh = onRefresh
    )
}

@Deprecated(
    message = "Function renamed to SensorDashboardDemo to reflect its static demo nature",
    replaceWith = ReplaceWith("SensorDashboardDemo(modifier, onSensorClick, onRefresh)")
)
@Composable
fun AnimatedSensorDashboard(
    modifier: Modifier = Modifier,
    onSensorClick: (SensorInfo) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    SensorDashboardDemo(modifier, onSensorClick, onRefresh)
}