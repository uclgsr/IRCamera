package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay

data class BluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val deviceType: String,
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val services: List<String> = emptyList()
)

class BluetoothScannerComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            BluetoothScannerScreen()
        }
    }

    @Composable
    private fun BluetoothScannerScreen() {
        var isScanning by remember { mutableStateOf(false) }
        var discoveredDevices by remember { mutableStateOf(generateSampleDevices()) }
        var pairedDevices by remember { mutableStateOf(generatePairedDevices()) }
        var selectedTab by remember { mutableStateOf(0) }
        var scanDuration by remember { mutableStateOf(0) }

        // Simulate scanning
        LaunchedEffect(isScanning) {
            if (isScanning) {
                scanDuration = 0
                while (isScanning && scanDuration < 30) {
                    delay(1000)
                    scanDuration++
                    
                    // Occasionally add new devices during scan
                    if (scanDuration % 3 == 0) {
                        val newDevice = generateRandomDevice()
                        if (discoveredDevices.none { it.address == newDevice.address }) {
                            discoveredDevices = discoveredDevices + newDevice
                        }
                    }
                }
                isScanning = false
            }
        }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "Bluetooth Scanner",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(
                            onClick = { 
                                isScanning = !isScanning
                                if (isScanning) {
                                    discoveredDevices = emptyList()
                                }
                            }
                        ) {
                            Icon(
                                if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                                contentDescription = if (isScanning) "Stop Scan" else "Start Scan"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!isScanning) {
                    FloatingActionButton(
                        onClick = { 
                            isScanning = true
                            discoveredDevices = emptyList()
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Scan for Devices")
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Scanning status
                if (isScanning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scanning for devices...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${scanDuration}s elapsed • ${discoveredDevices.size} devices found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Device type filter tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Discovered (${discoveredDevices.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Paired (${pairedDevices.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("GSR Sensors") }
                    )
                }

                // Device list
                when (selectedTab) {
                    0 -> DeviceList(
                        devices = discoveredDevices,
                        onDeviceClick = { device ->
                            // Handle device connection
                            discoveredDevices = discoveredDevices.map {
                                if (it.address == device.address) {
                                    it.copy(isConnected = !it.isConnected)
                                } else it
                            }
                        },
                        onDevicePair = { device ->
                            pairedDevices = pairedDevices + device.copy(isPaired = true)
                            discoveredDevices = discoveredDevices.filter { it.address != device.address }
                        }
                    )
                    1 -> DeviceList(
                        devices = pairedDevices,
                        onDeviceClick = { device ->
                            pairedDevices = pairedDevices.map {
                                if (it.address == device.address) {
                                    it.copy(isConnected = !it.isConnected)
                                } else it
                            }
                        },
                        onDevicePair = null
                    )
                    2 -> GSRSensorsList(
                        devices = (discoveredDevices + pairedDevices).filter { 
                            it.deviceType == "GSR Sensor" || it.name.contains("Shimmer", ignoreCase = true)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceList(
        devices: List<BluetoothDevice>,
        onDeviceClick: (BluetoothDevice) -> Unit,
        onDevicePair: ((BluetoothDevice) -> Unit)? = null
    ) {
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start scanning to discover devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device) },
                        onPair = if (onDevicePair != null) { { onDevicePair(device) } } else null
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceCard(
        device: BluetoothDevice,
        onClick: () -> Unit,
        onPair: (() -> Unit)? = null
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                getDeviceIcon(device.deviceType),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = device.name.ifEmpty { "Unknown Device" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${device.deviceType} • RSSI: ${device.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        // Connection status
                        Surface(
                            color = if (device.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (device.isConnected) "Connected" else "Disconnected",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        
                        // Pair button for discovered devices
                        if (onPair != null && !device.isPaired) {
                            TextButton(
                                onClick = onPair,
                                modifier = Modifier.size(width = 60.dp, height = 32.dp)
                            ) {
                                Text("Pair", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        
                        // Signal strength indicator
                        SignalStrengthIndicator(rssi = device.rssi)
                    }
                }
                
                // Services (if available)
                if (device.services.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        device.services.take(3).forEach { service ->
                            AssistChip(
                                onClick = { },
                                label = { Text(service, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        if (device.services.size > 3) {
                            Text(
                                text = "+${device.services.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GSRSensorsList(devices: List<BluetoothDevice>) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                GSRSensorCard(device = device)
            }
        }
    }

    @Composable
    private fun GSRSensorCard(device: BluetoothDevice) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GSR Sensor • Battery: 85%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    
                    Button(
                        onClick = { /* Connect to GSR sensor */ },
                        enabled = !device.isConnected
                    ) {
                        Text(if (device.isConnected) "Connected" else "Connect")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // GSR specific info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("Sampling Rate", "128 Hz")
                    InfoChip("Signal Quality", "Good")
                    InfoChip("Range", "0-40 μS")
                }
            }
        }
    }

    @Composable
    private fun InfoChip(label: String, value: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun SignalStrengthIndicator(rssi: Int) {
        val strength = when {
            rssi >= -50 -> 4
            rssi >= -60 -> 3
            rssi >= -70 -> 2
            rssi >= -80 -> 1
            else -> 0
        }
        
        Row {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = (4 + index * 2).dp)
                        .padding(end = 1.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = if (index < strength) Color.Green else Color.Gray,
                            size = size
                        )
                    }
                }
            }
        }
    }

    private fun getDeviceIcon(deviceType: String) = when (deviceType) {
        "GSR Sensor" -> Icons.Default.Sensors
        "Smartphone" -> Icons.Default.Smartphone
        "Headphones" -> Icons.Default.Headphones
        "Speaker" -> Icons.Default.Speaker
        "Computer" -> Icons.Default.Computer
        else -> Icons.Default.BluetoothConnected
    }

    private fun generateSampleDevices(): List<BluetoothDevice> {
        return listOf(
            BluetoothDevice("Shimmer3 GSR", "00:11:22:33:44:55", -65, "GSR Sensor", services = listOf("GSR", "Accelerometer")),
            BluetoothDevice("John's iPhone", "AA:BB:CC:DD:EE:FF", -45, "Smartphone"),
            BluetoothDevice("Sony WH-1000XM4", "11:22:33:44:55:66", -55, "Headphones"),
        )
    }

    private fun generatePairedDevices(): List<BluetoothDevice> {
        return listOf(
            BluetoothDevice("GSR Device #1", "00:11:22:33:44:56", -70, "GSR Sensor", isPaired = true, isConnected = true),
            BluetoothDevice("Lab Computer", "FF:EE:DD:CC:BB:AA", -40, "Computer", isPaired = true),
        )
    }

    private fun generateRandomDevice(): BluetoothDevice {
        val names = listOf("Unknown Device", "Bluetooth Speaker", "Wireless Mouse", "Keyboard", "Fitness Tracker")
        val types = listOf("Unknown", "Speaker", "Input Device", "Input Device", "Wearable")
        val randomIndex = (0 until names.size).random()
        
        return BluetoothDevice(
            name = names[randomIndex],
            address = "${(10..99).random()}:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}",
            rssi = (-90..-30).random(),
            deviceType = types[randomIndex]
        )
    }
}