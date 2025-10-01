package mpdc4gsr.feature.network.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.TitleBarAction
import mpdc4gsr.compose.theme.IRCameraTheme

data class PairableDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isConnected: Boolean = false,
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Scanning for devices...",
                                color = Color.White,
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Pairing Instructions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Ensure devices are in pairing mode\n• Keep devices close to the phone\n• Thermal cameras: Connect via USB\n• GSR sensors: Enable Bluetooth",
                            fontSize = 14.sp,
                            color = Color(0xFFCCFFFFFF),
                            lineHeight = 20.sp
                        )
                    }
                }

                // Device List
                Text(
                    text = "Available Devices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
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
                    DeviceType.THERMAL_CAMERA -> Color(0xFFFF6B6B)
                    DeviceType.GSR_SENSOR -> Color(0xFF4ECDC4)
                    DeviceType.RGB_CAMERA -> Color.White
                    DeviceType.BLUETOOTH_DEVICE -> Color(0xFF6B73FF)
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
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.id,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
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
                            tint = Color(0xFF4ECDC4),
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
                            tint = if (battery > 25) Color(0xFF4ECDC4) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${battery}%",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFFFFFF)
                        )
                    }
                }
            }

            // Pair Button
            when {
                device.isConnected -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(24.dp)
                    )
                }

                device.isPairing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF6B73FF),
                        strokeWidth = 2.dp
                    )
                }

                else -> {
                    OutlinedButton(
                        onClick = onPair,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B73FF)
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
        isConnected = true
    ),
    PairableDevice(
        id = "SHIMMER-3D4E5F",
        name = "Shimmer3 GSR Sensor",
        type = DeviceType.GSR_SENSOR,
        signalStrength = 85,
        batteryLevel = 67
    ),
    PairableDevice(
        id = "RGB-CAM-123",
        name = "Built-in RGB Camera",
        type = DeviceType.RGB_CAMERA,
        isConnected = true
    ),
    PairableDevice(
        id = "BT-DEV-456",
        name = "Research Device Alpha",
        type = DeviceType.BLUETOOTH_DEVICE,
        signalStrength = 45,
        batteryLevel = 89
    )
)

@Preview(showBackground = true)
@Composable
fun DevicePairingScreenPreview() {
    DevicePairingScreen()
}