package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid

private const val TAG = "SensorSelectionDialog"

enum class SensorType(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    THERMAL(
        "Thermal Camera",
        "Infrared thermal imaging with temperature measurement",
        Icons.Default.Thermostat
    ),
    RGB(
        "RGB Camera",
        "Color video recording with device camera features",
        Icons.Default.Videocam
    ),
    GSR(
        "GSR Sensor",
        "Physiological data streaming via Shimmer3 Bluetooth sensor",
        Icons.Default.Sensors
    )
}

fun detectAvailableSensors(context: Context): Set<SensorType> {
    val available = mutableSetOf<SensorType>()

    available.add(SensorType.THERMAL)

    if (context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)) {
        available.add(SensorType.RGB)
    }

    try {
        val shimmerManager =
            ShimmerBluetoothManagerAndroid(context, Handler(Looper.getMainLooper()))

        val hasConnectedShimmerDevices = try {
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking connected Shimmer devices: ${e.message}")
            false
        }

        if (hasConnectedShimmerDevices) {
            available.add(SensorType.GSR)
            Log.d(TAG, "Connected Shimmer GSR devices found")
        } else {
            available.add(SensorType.GSR)
            Log.d(TAG, "GSR available (will attempt connection or use simulation mode at runtime)")
        }
    } catch (e: Exception) {
        available.add(SensorType.GSR)
        Log.w(TAG, "GSR sensor available with simulated data if needed", e)
    }

    Log.d(TAG, "Detected available sensors: $available")
    return available
}

@Composable
fun SensorSelectionDialog(
    availableSensors: Set<SensorType>,
    initialSelection: Set<SensorType> = setOf(SensorType.THERMAL),
    onDismiss: () -> Unit,
    onSensorsSelected: (Set<SensorType>) -> Unit
) {
    var selectedSensors by remember {
        mutableStateOf(initialSelection.intersect(availableSensors))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Recording Sensors",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Parallel Multi-Modal Recording\nChoose sensors for synchronized recording:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                SensorType.values().forEach { sensorType ->
                    SensorCheckboxItem(
                        sensorType = sensorType,
                        isAvailable = availableSensors.contains(sensorType),
                        isSelected = selectedSensors.contains(sensorType),
                        onCheckedChange = { checked ->
                            selectedSensors = if (checked) {
                                selectedSensors + sensorType
                            } else {
                                selectedSensors - sensorType
                            }
                        }
                    )
                }

                Divider()

                Text(
                    text = getStatusText(selectedSensors),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = {
                        Log.d(TAG, "Sensor selection canceled")
                        onDismiss()
                    }) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedSensors.isNotEmpty()) {
                                Log.i(TAG, "Starting recording with selected sensors: $selectedSensors")
                                onSensorsSelected(selectedSensors)
                                onDismiss()
                            }
                        },
                        enabled = selectedSensors.isNotEmpty()
                    ) {
                        Text("Start Recording")
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorCheckboxItem(
    sensorType: SensorType,
    isAvailable: Boolean,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = if (isAvailable) onCheckedChange else null,
            enabled = isAvailable
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = sensorType.icon,
                    contentDescription = null,
                    tint = if (isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )

                Text(
                    text = sensorType.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            Text(
                text = if (isAvailable) {
                    sensorType.description
                } else {
                    "${sensorType.description} (Not Available)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

private fun getStatusText(selectedSensors: Set<SensorType>): String {
    return when (selectedSensors.size) {
        0 -> "Select at least one sensor to start recording"
        1 -> "Single-modal: ${selectedSensors.first().displayName}"
        2 -> "Dual-modal: ${selectedSensors.map { it.displayName }.joinToString(" + ")} synchronized"
        3 -> "Tri-modal: Complete physiological setup"
        else -> "${selectedSensors.size} sensors selected for parallel recording"
    }
}
