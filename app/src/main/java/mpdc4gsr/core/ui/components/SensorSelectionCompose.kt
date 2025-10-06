package mpdc4gsr.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class SensorType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isAvailable: Boolean = true,
    val requiresPermission: Boolean = false
) {
    THERMAL("Thermal Camera", "TC001/TS004 thermal imaging sensor", Icons.Default.Thermostat, true),
    RGB("RGB Camera", "High-resolution color camera", Icons.Default.Camera, true, true),
    GSR("GSR Sensor", "Galvanic skin response via Shimmer3", Icons.Default.Sensors, true, true),
    AUDIO("Audio Recorder", "High-quality audio capture", Icons.Default.Mic, true, true),
    ACCELEROMETER("Accelerometer", "Motion and orientation sensor", Icons.Default.Speed, true),
    GYROSCOPE("Gyroscope", "Angular velocity sensor", Icons.AutoMirrored.Filled.RotateRight, true),
    MAGNETOMETER("Magnetometer", "Magnetic field sensor", Icons.Default.Explore, true),
    HEART_RATE("Heart Rate", "Optical heart rate monitor", Icons.Default.Favorite, false),
    TEMPERATURE("Temperature", "Ambient temperature sensor", Icons.Default.DeviceThermostat, true),
    HUMIDITY("Humidity", "Environmental humidity sensor", Icons.Default.Water, false)
}

data class SensorAvailability(
    val sensorType: SensorType,
    val isAvailable: Boolean,
    val isSelected: Boolean,
    val availabilityReason: String = "",
    val batteryImpact: String = "Low",
    val dataRate: String = "Unknown"
)

@Composable
fun SensorSelectionDialog(
    availableSensors: List<SensorAvailability>,
    selectedSensors: Set<SensorType>,
    onSensorsSelected: (Set<SensorType>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Sensors",
    subtitle: String = "Choose sensors for your research session"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                SensorSelectionHeader(
                    title = title,
                    subtitle = subtitle,
                    selectedCount = selectedSensors.size,
                    totalCount = availableSensors.count { it.isAvailable }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Sensor list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableSensors) { sensorAvailability ->
                        SensorSelectionItem(
                            sensorAvailability = sensorAvailability,
                            isSelected = selectedSensors.contains(sensorAvailability.sensorType),
                            onSelectionChanged = { isSelected ->
                                val newSelection = if (isSelected) {
                                    selectedSensors + sensorAvailability.sensorType
                                } else {
                                    selectedSensors - sensorAvailability.sensorType
                                }
                                onSensorsSelected(newSelection)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSensorsSelected(emptySet())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = selectedSensors.isNotEmpty()
                    ) {
                        Text("Confirm (${selectedSensors.size})")
                    }
                }
                // Battery impact warning
                if (selectedSensors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryImpactWarning(
                        selectedSensors = selectedSensors,
                        availableSensors = availableSensors
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorSelectionHeader(
    title: String,
    subtitle: String,
    selectedCount: Int,
    totalCount: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$selectedCount/$totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SensorSelectionItem(
    sensorAvailability: SensorAvailability,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val sensor = sensorAvailability.sensorType
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                enabled = sensorAvailability.isAvailable,
                onClick = {
                    if (sensorAvailability.isAvailable) {
                        onSelectionChanged(!isSelected)
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !sensorAvailability.isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )

                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sensor icon
            Icon(
                imageVector = sensor.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    !sensorAvailability.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.5f
                    )

                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Sensor information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (sensorAvailability.isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = sensor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Availability status or data rate
                if (sensorAvailability.isAvailable) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Rate: ${sensorAvailability.dataRate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Battery: ${sensorAvailability.batteryImpact}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (sensorAvailability.batteryImpact) {
                                "High" -> Color(0xFFF44336)
                                "Medium" -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }
                } else {
                    Text(
                        text = sensorAvailability.availabilityReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            // Selection indicator
            if (sensorAvailability.isAvailable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Unavailable",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BatteryImpactWarning(
    selectedSensors: Set<SensorType>,
    availableSensors: List<SensorAvailability>
) {
    val highImpactSensors = selectedSensors.filter { sensorType ->
        availableSensors.find { it.sensorType == sensorType }?.batteryImpact == "High"
    }
    if (highImpactSensors.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "High battery usage expected with ${highImpactSensors.size} power-intensive sensor(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

// Sample data generator - returns unavailable sensors by default
fun getSampleSensorAvailability(): List<SensorAvailability> {
    return listOf(
        SensorAvailability(
            sensorType = SensorType.THERMAL,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.RGB,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "High"
        ),
        SensorAvailability(
            sensorType = SensorType.GSR,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.AUDIO,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.ACCELEROMETER,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.GYROSCOPE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.MAGNETOMETER,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.HEART_RATE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Hardware not available",
            dataRate = "Unknown",
            batteryImpact = "Medium"
        ),
        SensorAvailability(
            sensorType = SensorType.TEMPERATURE,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Not connected",
            dataRate = "Unknown",
            batteryImpact = "Low"
        ),
        SensorAvailability(
            sensorType = SensorType.HUMIDITY,
            isAvailable = false,
            isSelected = false,
            availabilityReason = "Sensor not supported",
            dataRate = "Unknown",
            batteryImpact = "Low"
        )
    )
}

// Demo usage
@Composable
fun SensorSelectionDemo() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedSensors by remember { mutableStateOf<Set<SensorType>>(emptySet()) }
    val availableSensors = remember { getSampleSensorAvailability() }
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(
            onClick = { showDialog = true }
        ) {
            Text("Select Sensors (${selectedSensors.size})")
        }
        if (selectedSensors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selected: ${selectedSensors.joinToString { it.displayName }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    if (showDialog) {
        SensorSelectionDialog(
            availableSensors = availableSensors,
            selectedSensors = selectedSensors,
            onSensorsSelected = { newSelection ->
                selectedSensors = newSelection
            },
            onDismiss = { showDialog = false }
        )
    }
}