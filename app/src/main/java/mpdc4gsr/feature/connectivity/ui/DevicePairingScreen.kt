package mpdc4gsr.feature.connectivity.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.components.common.TitleBarAction
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

data class PairableDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isConnected: Boolean? = null,
    val isPairing: Boolean = false,
    val signalStrength: Int = 0, // 0-100
    val batteryLevel: Int? = null
)

enum class DeviceType {
    THERMAL_CAMERA,
    GSR_SENSOR,
    RGB_CAMERA,
    BLUETOOTH_DEVICE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    onNavigateBack: () -> Unit = {},
    onPairDevice: (PairableDevice) -> Unit = {}
) {
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf(getSampleDevices()) }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Device Pairing",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                    contentDescription = if (isScanning) "Stop scanning" else "Start scanning",
                    onClick = {
                        isScanning = !isScanning
                        // Simulate device discovery
                        if (isScanning) {
                            devices = devices + PairableDevice(
                                id = "new_device_${System.currentTimeMillis()}",
                                name = "Shimmer3 GSR Unit",
                                type = DeviceType.GSR_SENSOR,
                                signalStrength = 75
                            )
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Scanning Status
                if (isScanning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Scanning for devices...",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                // Instructions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Pairing Instructions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Ensure devices are in pairing mode\n• Keep devices close to the phone\n• Thermal cameras: Connect via USB\n• GSR sensors: Enable Bluetooth",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
                // Device List
                Text(
                    text = "Available Devices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DevicePairingItem(
                            device = device,
                            onPair = { onPairDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DevicePairingItem(
    device: PairableDevice,
    onPair: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Type Icon
            Icon(
                imageVector = when (device.type) {
                    DeviceType.THERMAL_CAMERA -> Icons.Default.Camera
                    DeviceType.GSR_SENSOR -> Icons.Default.Sensors
                    DeviceType.RGB_CAMERA -> Icons.Default.VideoCall
                    DeviceType.BLUETOOTH_DEVICE -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = when (device.type) {
                    DeviceType.THERMAL_CAMERA -> MaterialTheme.colorScheme.error
                    DeviceType.GSR_SENSOR -> MaterialTheme.colorScheme.tertiary
                    DeviceType.RGB_CAMERA -> MaterialTheme.colorScheme.onSurfaceVariant
                    DeviceType.BLUETOOTH_DEVICE -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.id,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (device.signalStrength > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = when {
                                device.signalStrength > 75 -> Icons.Default.SignalCellularAlt
                                device.signalStrength > 50 -> Icons.Default.SignalCellularAlt
                                device.signalStrength > 25 -> Icons.Default.SignalCellularAlt
                                else -> Icons.Default.SignalCellularAlt
                            },
                            contentDescription = "Signal strength",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    device.batteryLevel?.let { battery ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = when {
                                battery > 75 -> Icons.Default.BatteryFull
                                battery > 50 -> Icons.Default.Battery6Bar
                                battery > 25 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery2Bar
                            },
                            contentDescription = "Battery level",
                            tint = if (battery > 25) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${battery}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            // Pair Button
            when {
                device.isConnected == true -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                device.isPairing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }

                device.isConnected == null -> {
                    Text(
                        text = "N/A",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                else -> {
                    OutlinedButton(
                        onClick = onPair,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Pair")
                    }
                }
            }
        }
    }
}

private fun getSampleDevices() = listOf(
    PairableDevice(
        id = "TC001-A5B2C1",
        name = "TOPDON TC001 Thermal Camera",
        type = DeviceType.THERMAL_CAMERA,
        isConnected = false
    ),
    PairableDevice(
        id = "SHIMMER-3D4E5F",
        name = "Shimmer3 GSR Sensor",
        type = DeviceType.GSR_SENSOR,
        signalStrength = 85,
        batteryLevel = 67,
        isConnected = false
    ),
    PairableDevice(
        id = "RGB-CAM-123",
        name = "Built-in RGB Camera",
        type = DeviceType.RGB_CAMERA,
        isConnected = false
    ),
    PairableDevice(
        id = "BT-DEV-456",
        name = "Research Device Alpha",
        type = DeviceType.BLUETOOTH_DEVICE,
        signalStrength = 45,
        batteryLevel = 89,
        isConnected = false
    )
)

@Preview(showBackground = true)
@Composable
fun DevicePairingScreenPreview() {
    DevicePairingScreen()
}

