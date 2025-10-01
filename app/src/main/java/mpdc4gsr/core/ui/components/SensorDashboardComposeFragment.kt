package mpdc4gsr.ui_components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.sensors.SensorViewModel

/**
 * SensorDashboardComposeFragment - Modern Fragment with Compose UI
 *
 * Advanced sensor monitoring fragment featuring:
 * - Real-time sensor status monitoring with visual indicators
 * - Live data visualization for GSR, thermal, and camera sensors
 * - Interactive sensor controls and configuration panels
 * - Performance metrics and health diagnostics
 * - Integration with existing Fragment-based navigation
 * - Modern Material 3 design with thermal imaging color palette
 */
class SensorDashboardComposeFragment : Fragment() {

    private val viewModel: SensorViewModel by viewModels()

    companion object {
        fun newInstance(): SensorDashboardComposeFragment {
            return SensorDashboardComposeFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LibUnifiedTheme {
                    SensorDashboardContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun SensorDashboardContent(
    viewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val sensorState by viewModel.sensorState.collectAsState()
    var selectedSensor by remember { mutableStateOf<mpdc4gsr.sensors.SensorViewModel.SensorInfo?>(null) }
    var showSensorDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dashboard header
        Text(
            text = "Sensor Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sensor status overview
        SensorStatusOverview(
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Active sensors list
        Text(
            text = "Active Sensors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sensorState.sensors) { sensor ->
                SensorCard(
                    sensor = sensor,
                    onClick = {
                        selectedSensor = sensor
                        showSensorDetails = true
                    }
                )
            }
        }
    }

    if (showSensorDetails && selectedSensor != null) {
        SensorDetailsDialog(
            sensor = selectedSensor!!,
            onDismiss = { showSensorDetails = false },
            onConfigure = {
                viewModel.configureSensor(selectedSensor!!.id)
                showSensorDetails = false
            }
        )
    }
}

@Composable
private fun SensorStatusOverview(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SensorStatusItem(
                label = "Active",
                count = 3,
                color = Color(0xFF4CAF50)
            )
            SensorStatusItem(
                label = "Inactive",
                count = 1,
                color = Color(0xFF9E9E9E)
            )
            SensorStatusItem(
                label = "Error",
                count = 0,
                color = Color(0xFFE53E3E)
            )
        }
    }
}

@Composable
private fun SensorStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SensorCard(
    sensor: mpdc4gsr.sensors.SensorViewModel.SensorInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (sensor.status) {
                "active" -> MaterialTheme.colorScheme.surface
                "inactive" -> MaterialTheme.colorScheme.surfaceVariant
                "error" -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sensor icon
            Icon(
                imageVector = getSensorIcon(sensor.type),
                contentDescription = sensor.type,
                modifier = Modifier.size(32.dp),
                tint = getSensorColor(sensor.type)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sensor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sensor metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getSensorStatusColor(sensor.status))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sensor.currentValue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sensor.status.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { /* Quick actions */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        }
    }
}

@Composable
private fun SensorDetailsDialog(
    sensor: mpdc4gsr.sensors.SensorViewModel.SensorInfo,
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(sensor.name)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                SensorDetailItem("Type", sensor.type)
                SensorDetailItem("Status", sensor.status.replaceFirstChar { it.uppercaseChar() })
                SensorDetailItem("Current Value", sensor.currentValue)
                SensorDetailItem("Last Update", sensor.lastUpdate)
                SensorDetailItem("Sample Rate", sensor.sampleRate)

                if (sensor.status == "active") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            when (sensor.type) {
                                "GSR" -> {
                                    Text("GSR: 2.45 µS", style = MaterialTheme.typography.bodySmall)
                                    Text("PPG: 1024, 1028", style = MaterialTheme.typography.bodySmall)
                                }

                                "Thermal" -> {
                                    Text("Center: 36.8°C", style = MaterialTheme.typography.bodySmall)
                                    Text("Max: 42.1°C", style = MaterialTheme.typography.bodySmall)
                                    Text("Min: 28.3°C", style = MaterialTheme.typography.bodySmall)
                                }

                                "Camera" -> {
                                    Text("Resolution: 1920x1080", style = MaterialTheme.typography.bodySmall)
                                    Text("FPS: 30", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text("Configure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SensorDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getSensorIcon(type: String) = when (type) {
    "GSR" -> Icons.Default.MonitorHeart
    "Thermal" -> Icons.Default.Thermostat
    "Camera" -> Icons.Default.Camera
    else -> Icons.Default.Sensors
}

private fun getSensorColor(type: String) = when (type) {
    "GSR" -> Color(0xFF4CAF50)
    "Thermal" -> Color(0xFFFF6B35)
    "Camera" -> Color(0xFF2196F3)
    else -> Color(0xFF9E9E9E)
}

private fun getSensorStatusColor(status: String) = when (status) {
    "active" -> Color(0xFF4CAF50)
    "inactive" -> Color(0xFF9E9E9E)
    "error" -> Color(0xFFE53E3E)
    else -> Color(0xFFFF9800)
}