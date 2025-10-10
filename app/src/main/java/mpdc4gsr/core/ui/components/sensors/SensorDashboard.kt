package mpdc4gsr.core.ui.components.sensors

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SensorDashboard(
    sensors: List<SensorDashboardItem>,
    modifier: Modifier = Modifier,
    onSensorClick: (SensorDashboardItem) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            DashboardHeader(
                connectedCount =
                    sensors.count {
                        it.status == SensorStatusUi.Connected || it.status == SensorStatusUi.Streaming
                    },
                totalCount = sensors.size,
                onRefresh = onRefresh,
            )
        }
        items(sensors) { sensor ->
            SensorCard(
                sensor = sensor,
                onClick = { onSensorClick(sensor) },
            )
        }
    }
}

@Deprecated(
    message = "Renamed for consistency with other dashboards",
    replaceWith = ReplaceWith("SensorDashboard(sensors, modifier, onSensorClick, onRefresh)"),
)
@Composable
fun SensorDashboardCompose(
    sensors: List<SensorDashboardItem>,
    modifier: Modifier = Modifier,
    onSensorClick: (SensorDashboardItem) -> Unit = {},
    onRefresh: () -> Unit = {},
) = SensorDashboard(sensors, modifier, onSensorClick, onRefresh)

@Composable
private fun DashboardHeader(
    connectedCount: Int,
    totalCount: Int,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Sensor Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "$connectedCount/$totalCount sensors connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh sensor status",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    sensor: SensorDashboardItem,
    onClick: () -> Unit,
) {
    val statusColor by animateColorAsState(
        targetValue = statusColors(sensor.status),
        animationSpec = tween(300),
        label = "sensorStatusColor",
    )
    val pulse by animateFloatAsState(
        targetValue = if (sensor.status == SensorStatusUi.Streaming && sensor.isAnimating) 1.05f else 1f,
        animationSpec = tween(600),
        label = "sensorPulse",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .scale(pulse)
                                .clip(RoundedCornerShape(12.dp)),
                        color = statusColor.copy(alpha = 0.15f),
                    ) {
                        Icon(
                            imageVector = sensor.icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier =
                                Modifier
                                    .padding(10.dp)
                                    .scale(pulse),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sensor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = sensor.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SensorStatusChip(status = sensor.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            SensorMetricsRow(sensor = sensor, statusColor = statusColor)
        }
    }
}

@Composable
private fun SensorMetricsRow(
    sensor: SensorDashboardItem,
    statusColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Metric(
            label = "Data Rate",
            value = sensor.dataRate,
            icon = sensor.icon,
            tint = statusColor,
        )
        Metric(
            label = "Last Update",
            value = sensor.lastUpdate,
            icon = sensor.icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Metric(
            label = "Signal",
            value = "${sensor.signalStrength}%",
            icon = sensor.icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SensorStatusChip(status: SensorStatusUi) {
    val (label, background, textColor) =
        when (status) {
            SensorStatusUi.Connected -> Triple("Connected", Color(0xFF4CAF50), Color.White)
            SensorStatusUi.Streaming -> Triple("Streaming", Color(0xFF2196F3), Color.White)
            SensorStatusUi.Error -> Triple("Error", Color(0xFFF44336), Color.White)
            SensorStatusUi.Connecting -> Triple("Connecting", Color(0xFFFF9800), Color.Black)
            SensorStatusUi.Simulation -> Triple("Simulation", Color(0xFFFFEB3B), Color.Black)
            SensorStatusUi.Disconnected -> Triple("Disconnected", Color(0xFF9E9E9E), Color.White)
        }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun statusColors(status: SensorStatusUi): Color =
    when (status) {
        SensorStatusUi.Connected -> Color(0xFF4CAF50)
        SensorStatusUi.Streaming -> Color(0xFF2196F3)
        SensorStatusUi.Error -> Color(0xFFF44336)
        SensorStatusUi.Connecting -> Color(0xFFFF9800)
        SensorStatusUi.Simulation -> Color(0xFFFFEB3B)
        SensorStatusUi.Disconnected -> Color(0xFF9E9E9E)
    }

fun sampleSensors(): List<SensorDashboardItem> =
    listOf(
        SensorDashboardItem(
            id = "thermal_camera",
            name = "Thermal Camera",
            status = SensorStatusUi.Disconnected,
            message = "Tap to connect thermal module",
            icon = SensorOption.Thermal.icon,
        ),
        SensorDashboardItem(
            id = "rgb_camera",
            name = "RGB Camera",
            status = SensorStatusUi.Disconnected,
            message = "Ready for pairing",
            icon = SensorOption.Rgb.icon,
        ),
        SensorDashboardItem(
            id = "gsr_sensor",
            name = "Shimmer GSR",
            status = SensorStatusUi.Disconnected,
            message = "Awaiting device",
            icon = SensorOption.Gsr.icon,
        ),
        SensorDashboardItem(
            id = "network_sync",
            name = "Network Sync",
            status = SensorStatusUi.Disconnected,
            message = "No active session",
            icon = SensorOption.NetworkSync.icon,
        ),
        SensorDashboardItem(
            id = "storage_system",
            name = "Storage System",
            status = SensorStatusUi.Disconnected,
            message = "Storage not mounted",
            icon = SensorOption.Storage.icon,
        ),
    )

@Composable
fun SensorDashboardDemo(
    modifier: Modifier = Modifier,
    onSensorClick: (SensorDashboardItem) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val sensors = remember { mutableStateListOf<SensorDashboardItem>().apply { addAll(sampleSensors()) } }
    SensorDashboard(
        sensors = sensors,
        modifier = modifier,
        onSensorClick = onSensorClick,
        onRefresh = onRefresh,
    )
}
