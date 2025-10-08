// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\feature\network\ui' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\network\ui\app_src_main_java_mpdc4gsr_feature_network_ui_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\network\ui' subtree
// Files: 8; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\ConnectScreen.kt =====

package mpdc4gsr.feature.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun ConnectScreen(
    onDeviceSelected: (ConnectedDevice) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val deviceConnectionState =
        com.mpdc4gsr.libunified.app.event.DeviceEventManager.deviceConnectionState.collectAsState()
    val socketConnectionState =
        com.mpdc4gsr.libunified.app.event.DeviceEventManager.socketConnectionState.collectAsState()

    val deviceTypes = remember(deviceConnectionState.value, socketConnectionState.value) {
        val tc001Status = try {
            val hasUsbDevice = com.mpdc4gsr.libunified.app.tools.DeviceTools.findUsbDevice() != null
            if (hasUsbDevice) {
                deviceConnectionState.value?.isConnected ?: false
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        val tc007Status: Boolean? = null

        listOf(
            ConnectedDevice(
                "TC001",
                "TOPDON TC001 Thermal Camera",
                tc001Status
            ),
            ConnectedDevice(
                "TC007",
                "TOPDON TC007 Thermal Camera",
                tc007Status
            )
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title bar replacing TitleView
        TitleBar(
            title = "Connect Device", // Match @string/tc_connect_device
            showBackButton = true,
            onBackClick = onBackClick
        )
        // Tips text with matching margins and styling
        Text(
            text = "Select your thermal camera device to connect", // Match @string/tc_connect_tips 
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(
                    start = 20.dp, // Match layout_marginStart
                    end = 20.dp, // Match layout_marginEnd  
                    top = 30.dp // Match layout_marginTop
                )
        )
        // Device list replacing RecyclerView
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp), // Match layout_marginTop
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deviceTypes) { device ->
                DeviceItem(
                    device = device,
                    onSelected = { onDeviceSelected(device) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: ConnectedDevice,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelected,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                Text(
                    text = when (device.isConnected) {
                        true -> "Connected"
                        false -> "Not connected"
                        null -> "N/A"
                    },
                    color = when (device.isConnected) {
                        true -> MaterialTheme.colorScheme.tertiary
                        false -> MaterialTheme.colorScheme.onSurfaceVariant
                        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            // Connection status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = when (device.isConnected) {
                    true -> MaterialTheme.colorScheme.tertiary
                    false -> MaterialTheme.colorScheme.outline
                    null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }
            ) {}
        }
    }
}

data class ConnectedDevice(
    val id: String,
    val name: String,
    val isConnected: Boolean?
)

@Preview(showBackground = true)
@Composable
private fun ConnectScreenPreview() {
    IRCameraTheme {
        ConnectScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\DevicePairingComposeActivity.kt =====

package mpdc4gsr.feature.network.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.network.presentation.DevicePairingViewModel
import mpdc4gsr.core.ui.deferAction

class DevicePairingComposeActivity : BaseComposeActivity<DevicePairingViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DevicePairingComposeActivity::class.java))
        }
    }

    override fun createViewModel(): DevicePairingViewModel {
        return viewModels<DevicePairingViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DevicePairingViewModel) {
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
        var showPairingDialog by remember { mutableStateOf(false) }
        var deviceFilter by remember { mutableStateOf("All") }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Device Pairing",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = deferAction { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                                    contentDescription = if (isScanning) "Stop Scan" else "Start Scan"
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Open pairing settings
                                android.widget.Toast.makeText(
                                    this@DevicePairingComposeActivity,
                                    "Pairing settings coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                DevicePairingContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = { selectedDevice = it },
                    onPairDevice = { showPairingDialog = true },
                    deviceFilter = deviceFilter,
                    onFilterChange = { deviceFilter = it },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showPairingDialog && selectedDevice != null) {
            DevicePairingDialog(
                device = selectedDevice!!,
                onDismiss = { showPairingDialog = false },
                onPair = { device ->
                    // Initiate pairing
                    showPairingDialog = false
                }
            )
        }
    }
}

@Composable
private fun DevicePairingContent(
    isScanning: Boolean,
    selectedDevice: BluetoothDeviceInfo?,
    onDeviceSelect: (BluetoothDeviceInfo?) -> Unit,
    onPairDevice: () -> Unit,
    deviceFilter: String,
    onFilterChange: (String) -> Unit,
    viewModel: DevicePairingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scanning Status Card
        ScanningStatusCard(
            isScanning = isScanning,
            devicesFound = 8, // Replace with actual count
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Device Filter Row
        DeviceFilterRow(
            selectedFilter = deviceFilter,
            onFilterChange = onFilterChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Device List
        Text(
            text = "Discovered Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val mockDevices = getMockBluetoothDevices().filter { device ->
                deviceFilter == "All" || device.type == deviceFilter
            }
            items(mockDevices) { device ->
                DeviceCard(
                    device = device,
                    isSelected = selectedDevice?.address == device.address,
                    onSelect = { onDeviceSelect(device) },
                    onPair = {
                        onDeviceSelect(device)
                        onPairDevice()
                    }
                )
            }
        }
        // Connection Status Footer
        selectedDevice?.let { device ->
            ConnectionStatusFooter(
                device = device,
                onPair = onPairDevice,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ScanningStatusCard(
    isScanning: Boolean,
    devicesFound: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                    text = if (isScanning) "Scanning..." else "Scan Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$devicesFound devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Scan complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceFilterRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Thermal", "GSR", "Camera", "Unknown")
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter) },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name.ifEmpty { "Unknown Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Device type and signal strength
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Device type chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getDeviceTypeColor(device.type),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = device.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        // Signal strength
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Signal strength",
                            modifier = Modifier.size(16.dp),
                            tint = getSignalStrengthColor(device.rssi)
                        )
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                // Connection status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (device.connectionStatus) {
                                "connected" -> MaterialTheme.colorScheme.primary
                                "paired" -> MaterialTheme.colorScheme.tertiary
                                "available" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }
            if (isSelected) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                // Device actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            // TODO: Connect to selected device
                            android.widget.Toast.makeText(
                                context,
                                "Connecting to device...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                    Button(
                        onClick = onPair,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Pair",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pair")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusFooter(
    device: BluetoothDeviceInfo,
    onPair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "${device.name} (${device.address})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Status: ${device.connectionStatus.replaceFirstChar { it.uppercaseChar() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onPair,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.BluetoothConnected,
                    contentDescription = "Pair Device",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair Device")
            }
        }
    }
}

@Composable
private fun DevicePairingDialog(
    device: BluetoothDeviceInfo,
    onDismiss: () -> Unit,
    onPair: (BluetoothDeviceInfo) -> Unit
) {
    var isPairing by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pair with ${device.name}")
        },
        text = {
            Column {
                Text(text = "Device Address: ${device.address}")
                Text(text = "Device Type: ${device.type}")
                Text(text = "Signal Strength: ${device.rssi} dBm")
                if (isPairing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pairing in progress...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isPairing = true
                    onPair(device)
                },
                enabled = !isPairing
            ) {
                Text("Pair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun getDeviceTypeColor(type: String) = when (type) {
    "Thermal" -> MaterialTheme.colorScheme.error
    "GSR" -> MaterialTheme.colorScheme.primary
    "Camera" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun getSignalStrengthColor(rssi: Int) = when {
    rssi > -50 -> MaterialTheme.colorScheme.primary
    rssi > -60 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val type: String,
    val rssi: Int,
    val connectionStatus: String
)

private fun getMockBluetoothDevices() = listOf(
    BluetoothDeviceInfo("TOPDON TC001", "00:11:22:33:44:55", "Thermal", -45, "available"),
    BluetoothDeviceInfo("Shimmer3 GSR+", "AA:BB:CC:DD:EE:FF", "GSR", -52, "paired"),
    BluetoothDeviceInfo("RGB Camera Pro", "12:34:56:78:90:AB", "Camera", -38, "connected"),
    BluetoothDeviceInfo("Unknown Device", "FF:EE:DD:CC:BB:AA", "Unknown", -68, "available"),
    BluetoothDeviceInfo("Samsung Galaxy", "11:22:33:44:55:66", "Camera", -42, "available")
)


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\DevicePairingScreen.kt =====

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

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
                            text = "â€¢ Ensure devices are in pairing mode\nâ€¢ Keep devices close to the phone\nâ€¢ Thermal cameras: Connect via USB\nâ€¢ GSR sensors: Enable Bluetooth",
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


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\NetworkClientTestActivityCompose.kt =====

package mpdc4gsr.feature.network.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.network.data.CommandConnection
import mpdc4gsr.feature.network.data.NetworkManager

class NetworkClientTestViewModel : AppBaseViewModel() {
    private val _networkConnectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    val networkConnectionState: StateFlow<CommandConnection.ConnectionState> = _networkConnectionState.asStateFlow()
    private val _ipAddress = MutableStateFlow("192.168.1.100")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()
    private val _port = MutableStateFlow("8080")
    val port: StateFlow<String> = _port.asStateFlow()
    private val _connectionInfo = MutableStateFlow("")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    // Data classes for network testing (shared with NetworkClientTestComposeActivity)
    enum class TestStatus(val displayName: String) {
        PASS("Pass"),
        FAIL("Fail"),
        WARNING("Warning"),
        PENDING("Pending")
    }

    enum class NetworkTestType { CONNECTION, LATENCY, THROUGHPUT, RELIABILITY }
    data class NetworkConfiguration(
        val serverAddress: String = "192.168.1.100",
        val port: Int = 8080,
        val timeoutMs: Long = 5000,
        val retryAttempts: Int = 3
    )

    data class NetworkTestCategory(
        val name: String,
        val description: String,
        val type: NetworkTestType,
        val testCount: Int,
        val lastResult: TestStatus
    )

    data class NetworkTestResult(
        val testName: String,
        val status: TestStatus,
        val timestamp: String,
        val duration: Long,
        val details: String
    )

    // UI State for NetworkClientTestComposeActivity
    data class NetworkTestUiState(
        val isTestRunning: Boolean = false,
        val currentTest: String = "",
        val testProgress: Float = 0f,
        val networkStatus: String = "Disconnected",
        val testCategories: List<NetworkTestCategory> = emptyList(),
        val testResults: List<NetworkTestResult> = emptyList(),
        val networkConfiguration: NetworkConfiguration = NetworkConfiguration(),
        val error: String? = null
    )

    private val _networkTestUiState = MutableStateFlow(NetworkTestUiState())
    val networkTestUiState: StateFlow<NetworkTestUiState> = _networkTestUiState.asStateFlow()
    fun updateConnectionState(state: CommandConnection.ConnectionState) {
        _networkConnectionState.value = state
    }

    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
    }

    fun updatePort(port: String) {
        _port.value = port
    }

    fun updateConnectionInfo(info: String) {
        _connectionInfo.value = info
    }

    // Methods for NetworkClientTestComposeActivity
    fun startComprehensiveTest() {
        _networkTestUiState.value = _networkTestUiState.value.copy(isTestRunning = true)
    }

    fun stopTest() {
        _networkTestUiState.value = _networkTestUiState.value.copy(isTestRunning = false)
    }

    fun refreshNetworkStatus() {
        _networkTestUiState.value = _networkTestUiState.value.copy(
            networkStatus = when (_networkConnectionState.value) {
                CommandConnection.ConnectionState.CONNECTED -> "Connected"
                CommandConnection.ConnectionState.CONNECTING -> "Connecting"
                CommandConnection.ConnectionState.ERROR -> "Error"
                else -> "Disconnected"
            }
        )
    }

    fun runQuickNetworkTest() {
        // Stub implementation
    }

    fun runCategoryTest(category: NetworkTestCategory) {
        // Stub implementation
    }

    fun viewTestDetails(result: NetworkTestResult) {
        // Stub implementation
    }

    fun updateNetworkConfiguration(config: NetworkConfiguration) {
        _networkTestUiState.value = _networkTestUiState.value.copy(networkConfiguration = config)
        // Update IP and port from configuration
        _ipAddress.value = config.serverAddress
        _port.value = config.port.toString()
    }

    override fun clearError() {
        _networkTestUiState.value = _networkTestUiState.value.copy(error = null)
    }
}

class NetworkClientTestActivityCompose : BaseComposeActivity<NetworkClientTestViewModel>() {
    companion object {
        private const val TAG = "NetworkClientTestActivityCompose"
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    private lateinit var testViewModel: NetworkClientTestViewModel
    private var recordingService: RecordingService? = null
    private var networkManager: NetworkManager? = null
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            AppLogger.i(TAG, "Service connected")
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            networkManager = binder.getNetworkManager()
            isBound = true
            observeConnectionState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            AppLogger.i(TAG, "Service disconnected")
            recordingService = null
            networkManager = null
            isBound = false
        }
    }

    override fun createViewModel(): NetworkClientTestViewModel {
        testViewModel = NetworkClientTestViewModel()
        return testViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bind to RecordingService
        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun observeConnectionState() {
        networkManager?.let { manager ->
            lifecycleScope.launch {
                manager.connectionState.collect { state ->
                    AppLogger.i(TAG, "Connection state changed: $state")
                    testViewModel.updateConnectionState(state)
                    updateConnectionInfo()
                }
            }
        }
    }

    private fun updateConnectionInfo() {
        val info = networkManager?.let { manager ->
            when (val state = testViewModel.networkConnectionState.value) {
                CommandConnection.ConnectionState.CONNECTED -> {
                    "Connected to ${testViewModel.ipAddress.value}:${testViewModel.port.value}"
                }

                CommandConnection.ConnectionState.CONNECTING -> {
                    "Connecting to ${testViewModel.ipAddress.value}:${testViewModel.port.value}..."
                }

                CommandConnection.ConnectionState.ERROR -> {
                    "Connection failed to ${testViewModel.ipAddress.value}:${testViewModel.port.value}"
                }

                else -> "Not connected"
            }
        } ?: "Service not available"
        testViewModel.updateConnectionInfo(info)
    }

    private fun testWifiConnection(ip: String, port: Int) {
        lifecycleScope.launch {
            try {
                networkManager?.connectWifi(ip, port)
            } catch (e: Exception) {
                AppLogger.e(TAG, "WiFi connection failed", e)
                testViewModel.updateConnectionState(CommandConnection.ConnectionState.ERROR)
            }
        }
    }

    private fun testSendMessage() {
        lifecycleScope.launch {
            try {
                networkManager?.sendResponse("ping")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to send message", e)
            }
        }
    }

    private fun testBluetoothConnection() {
        // Placeholder for Bluetooth connection logic
        AppLogger.i(TAG, "Bluetooth connection test - implementation needed")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: NetworkClientTestViewModel) {
        val connectionState by testViewModel.networkConnectionState.collectAsState()
        val ipAddress by testViewModel.ipAddress.collectAsState()
        val port by testViewModel.port.collectAsState()
        val connectionInfo by viewModel.connectionInfo.collectAsState()
        val scrollState = rememberScrollState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Network Client Test",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status Card
                ConnectionStatusCard(
                    connectionState = connectionState,
                    connectionInfo = connectionInfo
                )
                // Connection Configuration Card
                ConnectionConfigCard(
                    ipAddress = ipAddress,
                    port = port,
                    onIpAddressChange = viewModel::updateIpAddress,
                    onPortChange = viewModel::updatePort
                )
                // Action Buttons Card
                ActionButtonsCard(
                    connectionState = connectionState,
                    onConnectWifi = {
                        val portInt = port.toIntOrNull() ?: DEFAULT_PC_PORT
                        if (ipAddress.isNotEmpty() && portInt in 1..65535) {
                            testWifiConnection(ipAddress, portInt)
                        }
                    },
                    onTestPing = ::testSendMessage,
                    onConnectBluetooth = ::testBluetoothConnection,
                    onDisconnect = {
                        lifecycleScope.launch {
                            networkManager?.disconnect()
                        }
                    }
                )
                // Test Information Card
                TestInfoCard()
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: CommandConnection.ConnectionState,
    connectionInfo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                CommandConnection.ConnectionState.CONNECTED ->
                    MaterialTheme.colorScheme.primaryContainer

                CommandConnection.ConnectionState.ERROR ->
                    MaterialTheme.colorScheme.errorContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                        CommandConnection.ConnectionState.CONNECTING -> Icons.Default.Sync
                        CommandConnection.ConnectionState.ERROR -> Icons.Default.Error
                        else -> Icons.Default.Circle
                    },
                    contentDescription = "Connection Status",
                    tint = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.tertiary
                        CommandConnection.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary
                        CommandConnection.ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
                Text(
                    text = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> "Connected"
                        CommandConnection.ConnectionState.CONNECTING -> "Connecting..."
                        CommandConnection.ConnectionState.ERROR -> "Connection Error"
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (connectionInfo.isNotEmpty()) {
                Text(
                    text = connectionInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionConfigCard(
    ipAddress: String,
    port: String,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = ipAddress,
                onValueChange = onIpAddressChange,
                label = { Text("PC IP Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // Focus moves to next field automatically
                    }
                )
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                placeholder = { Text("8080") },
                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
        }
    }
}

@Composable
private fun ActionButtonsCard(
    connectionState: CommandConnection.ConnectionState,
    onConnectWifi: () -> Unit,
    onTestPing: () -> Unit,
    onConnectBluetooth: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Network Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnectWifi,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.DISCONNECTED
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WiFi")
                }
                Button(
                    onClick = onConnectBluetooth,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.DISCONNECTED
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bluetooth")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTestPing,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.CONNECTED
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Ping")
                }
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.CONNECTED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun TestInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Test Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This activity tests bidirectional network communication between the Android app and PC server. " +
                        "Use WiFi for high-speed data transfer or Bluetooth for reliable short-range communication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\NetworkClientTestComposeActivity.kt =====

package mpdc4gsr.feature.network.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme

class NetworkClientTestComposeActivity : BaseComposeActivity<NetworkClientTestViewModel>() {
    override fun createViewModel(): NetworkClientTestViewModel =
        viewModels<NetworkClientTestViewModel>().value

    @Composable
    override fun Content(viewModel: NetworkClientTestViewModel) {
        IRCameraTheme {
            NetworkClientTestScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkClientTestScreen(
    viewModel: NetworkClientTestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.networkTestUiState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Network Client Test") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.startComprehensiveTest() }) {
                    Icon(
                        imageVector = if (uiState.isTestRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isTestRunning) "Stop test" else "Start test",
                        tint = if (uiState.isTestRunning) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshNetworkStatus() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh network"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Status Overview
            item {
                NetworkStatusOverviewCard(
                    networkStatus = NetworkTestStatus(
                        overallStatus = when (uiState.networkStatus) {
                            "Connected" -> TestStatus.PASS
                            "Connecting" -> TestStatus.PENDING
                            "Error" -> TestStatus.FAIL
                            else -> TestStatus.WARNING
                        },
                        latency = 0,
                        bandwidth = 0f,
                        packetLoss = 0f,
                        connectedDevices = if (uiState.networkStatus == "Connected") 1 else 0
                    ),
                    onRunQuickTest = { viewModel.runQuickNetworkTest() }
                )
            }
            // Test Progress (if running)
            if (uiState.isTestRunning) {
                item {
                    TestProgressCard(
                        currentTest = uiState.currentTest,
                        progress = uiState.testProgress,
                        onStopTest = { viewModel.stopTest() }
                    )
                }
            }
            // Test Categories
            item {
                Text(
                    text = "Network Test Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(uiState.testCategories) { category ->
                TestCategoryCard(
                    category = category,
                    onRunCategoryTest = { viewModel.runCategoryTest(category) }
                )
            }
            // Test Results
            if (uiState.testResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(uiState.testResults.take(5)) { result ->
                    TestResultCard(
                        result = result,
                        onViewDetails = { viewModel.viewTestDetails(result) }
                    )
                }
            }
            // Network Configuration
            item {
                NetworkConfigurationCard(
                    configuration = uiState.networkConfiguration,
                    onUpdateConfiguration = { config ->
                        viewModel.updateNetworkConfiguration(config)
                    }
                )
            }
            // Error Display
            uiState.error?.let { errorMessage ->
                item {
                    ErrorCard(
                        error = errorMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusOverviewCard(
    networkStatus: NetworkTestStatus,
    onRunQuickTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (networkStatus.overallStatus) {
                TestStatus.PASS -> MaterialTheme.colorScheme.primaryContainer
                TestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer
                TestStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            }
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
                        text = "Network Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (networkStatus.overallStatus) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = networkStatus.overallStatus.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (networkStatus.overallStatus) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Button(onClick = onRunQuickTest) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Test")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NetworkMetric(
                    label = "Latency",
                    value = "${networkStatus.latency}ms",
                    status = if (networkStatus.latency < 100) TestStatus.PASS else TestStatus.WARNING
                )
                NetworkMetric(
                    label = "Bandwidth",
                    value = "${networkStatus.bandwidth} Mbps",
                    status = if (networkStatus.bandwidth > 10) TestStatus.PASS else TestStatus.WARNING
                )
                NetworkMetric(
                    label = "Packet Loss",
                    value = "${networkStatus.packetLoss}%",
                    status = if (networkStatus.packetLoss < 5) TestStatus.PASS else TestStatus.FAIL
                )
            }
            if (networkStatus.connectedDevices > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected Devices: ${networkStatus.connectedDevices}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (networkStatus.overallStatus) {
                        TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                        TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                        TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun NetworkMetric(
    label: String,
    value: String,
    status: TestStatus
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = when (status) {
                TestStatus.PASS -> Color.Green
                TestStatus.WARNING -> Color.Yellow
                TestStatus.FAIL -> Color.Red
                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TestProgressCard(
    currentTest: String,
    progress: Float,
    onStopTest: () -> Unit
) {
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
                        text = "Testing in Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = currentTest,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Button(
                    onClick = onStopTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestCategoryCard(
    category: NetworkTestCategory,
    onRunCategoryTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (category.lastResult) {
                TestStatus.PASS -> MaterialTheme.colorScheme.secondaryContainer
                TestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer
                TestStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (category.type) {
                            NetworkTestType.CONNECTION -> Icons.Default.NetworkCheck
                            NetworkTestType.LATENCY -> Icons.Default.Speed
                            NetworkTestType.THROUGHPUT -> Icons.Default.Tune
                            NetworkTestType.RELIABILITY -> Icons.Default.Security
                        },
                        contentDescription = "Test type",
                        modifier = Modifier.size(24.dp),
                        tint = when (category.lastResult) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = when (category.lastResult) {
                                TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                                TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                                TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "${category.testCount} tests",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (category.lastResult) {
                                TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                                TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                                TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Icon(
                    imageVector = when (category.lastResult) {
                        TestStatus.PASS -> Icons.Default.CheckCircle
                        TestStatus.FAIL -> Icons.Default.Error
                        TestStatus.WARNING -> Icons.Default.Warning
                        TestStatus.PENDING -> Icons.Default.Schedule
                    },
                    contentDescription = "Test result",
                    tint = when (category.lastResult) {
                        TestStatus.PASS -> Color.Green
                        TestStatus.FAIL -> Color.Red
                        TestStatus.WARNING -> Color.Yellow
                        TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = when (category.lastResult) {
                    TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                    TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                    TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                    TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRunCategoryTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run ${category.name} Tests")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestResultCard(
    result: NetworkTestResult,
    onViewDetails: () -> Unit
) {
    Card(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (result.status) {
                    TestStatus.PASS -> Icons.Default.CheckCircle
                    TestStatus.FAIL -> Icons.Default.Error
                    TestStatus.WARNING -> Icons.Default.Warning
                    TestStatus.PENDING -> Icons.Default.Schedule
                },
                contentDescription = "Test result",
                modifier = Modifier.size(24.dp),
                tint = when (result.status) {
                    TestStatus.PASS -> Color.Green
                    TestStatus.FAIL -> Color.Red
                    TestStatus.WARNING -> Color.Yellow
                    TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.testName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${result.timestamp} - ${result.duration}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.details.isNotEmpty()) {
                    Text(
                        text = result.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkConfigurationCard(
    configuration: NetworkConfiguration,
    onUpdateConfiguration: (NetworkConfiguration) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigurationItem(
                        label = "Server Address",
                        value = configuration.serverAddress
                    )
                    ConfigurationItem(
                        label = "Port",
                        value = configuration.port.toString()
                    )
                    ConfigurationItem(
                        label = "Timeout",
                        value = "${configuration.timeoutMs}ms"
                    )
                    ConfigurationItem(
                        label = "Retry Attempts",
                        value = configuration.retryAttempts.toString()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // TODO: Implement network configuration editor dialog
                        android.widget.Toast.makeText(
                            localContext,
                            "Network configuration editor",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Configuration")
                }
            }
        }
    }
}

@Composable
private fun ConfigurationItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
// Type aliases to use ViewModel types
typealias NetworkTestCategory = NetworkClientTestViewModel.NetworkTestCategory
typealias NetworkTestResult = NetworkClientTestViewModel.NetworkTestResult
typealias TestStatus = NetworkClientTestViewModel.TestStatus
typealias NetworkTestType = NetworkClientTestViewModel.NetworkTestType
typealias NetworkConfiguration = NetworkClientTestViewModel.NetworkConfiguration

// Data classes specific to this activity
data class NetworkTestStatus(
    val overallStatus: TestStatus,
    val latency: Int,
    val bandwidth: Float,
    val packetLoss: Float,
    val connectedDevices: Int
)


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\NetworkConfigComposeActivity.kt =====

package mpdc4gsr.feature.network.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.PermissionController
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.network.data.NetworkSettings

enum class ConnectionType(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    WIFI("WiFi Connection", "Connect devices over wireless network", Icons.Default.Wifi),
    BLUETOOTH("Bluetooth Connection", "Connect devices via Bluetooth", Icons.Default.Bluetooth),
    ETHERNET("Wired Connection", "Connect devices via Ethernet cable", Icons.Default.Cable),
    HOTSPOT("Mobile Hotspot", "Create hotspot for device connection", Icons.Default.WifiTethering)
}

data class NetworkDevice(
    val name: String,
    val address: String,
    val type: ConnectionType,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0
)

class NetworkConfigViewModel : AppBaseViewModel() {
    private val _selectedConnectionType = mutableStateOf<ConnectionType?>(null)
    val selectedConnectionType: State<ConnectionType?> = _selectedConnectionType
    private val _availableDevices = mutableStateOf<List<NetworkDevice>>(emptyList())
    val availableDevices: State<List<NetworkDevice>> = _availableDevices
    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning
    private val _connectedDevice = mutableStateOf<NetworkDevice?>(null)
    val connectedDevice: State<NetworkDevice?> = _connectedDevice
    private val _connectionStatus = mutableStateOf("Disconnected")
    val connectionStatus: State<String> = _connectionStatus
    fun selectConnectionType(type: ConnectionType) {
        _selectedConnectionType.value = type
        startDeviceDiscovery()
    }

    private fun startDeviceDiscovery() {
        launchWithErrorHandling {
            _isScanning.value = true
            _availableDevices.value = emptyList()
            // Simulate device discovery
            delay(1000)
            val mockDevices = when (_selectedConnectionType.value) {
                ConnectionType.WIFI -> listOf(
                    NetworkDevice(
                        "TC001-WiFi-Device",
                        "192.168.1.100",
                        ConnectionType.WIFI,
                        signalStrength = 85
                    ),
                    NetworkDevice(
                        "TS004-WiFi-Device",
                        "192.168.1.101",
                        ConnectionType.WIFI,
                        signalStrength = 72
                    ),
                    NetworkDevice(
                        "IRCamera-Hub",
                        "192.168.1.102",
                        ConnectionType.WIFI,
                        signalStrength = 91
                    )
                )

                ConnectionType.BLUETOOTH -> listOf(
                    NetworkDevice(
                        "TC001-BT",
                        "00:11:22:33:44:55",
                        ConnectionType.BLUETOOTH,
                        signalStrength = 78
                    ),
                    NetworkDevice(
                        "TS004-BT",
                        "00:11:22:33:44:56",
                        ConnectionType.BLUETOOTH,
                        signalStrength = 65
                    ),
                    NetworkDevice(
                        "GSR-Sensor-BT",
                        "00:11:22:33:44:57",
                        ConnectionType.BLUETOOTH,
                        signalStrength = 82
                    )
                )

                ConnectionType.ETHERNET -> listOf(
                    NetworkDevice(
                        "Wired-TC001",
                        "192.168.0.50",
                        ConnectionType.ETHERNET,
                        signalStrength = 100
                    ),
                    NetworkDevice(
                        "Wired-TS004",
                        "192.168.0.51",
                        ConnectionType.ETHERNET,
                        signalStrength = 100
                    )
                )

                ConnectionType.HOTSPOT -> listOf(
                    NetworkDevice(
                        "Mobile-TC001",
                        "192.168.43.100",
                        ConnectionType.HOTSPOT,
                        signalStrength = 88
                    ),
                    NetworkDevice(
                        "Mobile-TS004",
                        "192.168.43.101",
                        ConnectionType.HOTSPOT,
                        signalStrength = 75
                    )
                )

                null -> emptyList()
            }
            delay(2000) // Simulate scanning time
            _availableDevices.value = mockDevices
            _isScanning.value = false
        }
    }

    fun connectToDevice(device: NetworkDevice) {
        launchWithErrorHandling {
            _connectionStatus.value = "Connecting..."
            delay(3000) // Simulate connection time
            // Simulate connection success
            _connectedDevice.value = device.copy(isConnected = true)
            _connectionStatus.value = "Connected to ${device.name}"
            // Update the device list to show connected status
            _availableDevices.value = _availableDevices.value.map {
                if (it.address == device.address) it.copy(isConnected = true) else it.copy(
                    isConnected = false
                )
            }
        }
    }

    fun disconnectDevice() {
        _connectedDevice.value = null
        _connectionStatus.value = "Disconnected"
        _availableDevices.value = _availableDevices.value.map { it.copy(isConnected = false) }
    }
}

class NetworkConfigComposeActivity : BaseComposeActivity<NetworkConfigViewModel>() {
    private lateinit var networkSettings: NetworkSettings
    private lateinit var permissionManager: PermissionManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothEnableResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth enabled
        }
    }

    override fun createViewModel(): NetworkConfigViewModel =
        viewModels<NetworkConfigViewModel>().value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkSettings = NetworkSettings(this)
        permissionManager = PermissionManager(this, PermissionController(this))
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: NetworkConfigViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val selectedConnectionType by viewModel.selectedConnectionType
            val availableDevices by viewModel.availableDevices
            val isScanning by viewModel.isScanning
            val connectedDevice by viewModel.connectedDevice
            val connectionStatus by viewModel.connectionStatus
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TitleBar(
                    title = "Network Configuration",
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Connection status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (connectedDevice != null)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (connectedDevice != null) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = if (connectedDevice != null) "Connected" else "Not connected",
                                tint = if (connectedDevice != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connection Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = connectionStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (connectedDevice != null) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectDevice() }
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                    // Connection type selection
                    if (selectedConnectionType == null) {
                        Text(
                            text = "Select Connection Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(ConnectionType.values()) { connectionType ->
                                ConnectionTypeCard(
                                    connectionType = connectionType,
                                    onSelect = { viewModel.selectConnectionType(it) }
                                )
                            }
                        }
                    } else {
                        // Device discovery and connection
                        selectedConnectionType?.let { currentConnectionType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = currentConnectionType.icon,
                                    contentDescription = "${currentConnectionType.name} connection",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentConnectionType.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.selectConnectionType(currentConnectionType) }
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                        if (isScanning) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Scanning for devices...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else if (availableDevices.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = "No devices found",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No devices found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Make sure your devices are powered on and in pairing mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableDevices) { device ->
                                    DeviceCard(
                                        device = device,
                                        onConnect = { viewModel.connectToDevice(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionTypeCard(
    connectionType: ConnectionType,
    onSelect: (ConnectionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(connectionType) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = connectionType.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connectionType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connectionType.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: NetworkDevice,
    onConnect: (NetworkDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (device.isConnected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = device.type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (device.isConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (device.signalStrength > 0) {
                    Text(
                        text = "Signal: ${device.signalStrength}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (device.isConnected) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onConnect(device) }
                ) {
                    Text("Connect")
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\NetworkSettingsScreen.kt =====

package mpdc4gsr.feature.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.NetworkSettingsViewModel
import mpdc4gsr.feature.settings.presentation.NetworkSettingsViewModelFactory

@Composable
fun NetworkSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: NetworkSettingsViewModel = viewModel(
        factory = NetworkSettingsViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.networkSettings.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TitleBar(
            title = "Network Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WiFi Settings
            SettingsCard(
                title = "WiFi",
                icon = Icons.Default.Wifi
            ) {
                SettingsToggle(
                    label = "WiFi",
                    description = "Enable WiFi connectivity",
                    checked = settings.wifiEnabled,
                    onCheckedChange = { viewModel.refreshWifiInfo() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Network",
                    value = networkInfo.wifiNetwork
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "IP Address",
                    value = networkInfo.ipAddress
                )
            }
            // Bluetooth Settings
            SettingsCard(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth
            ) {
                SettingsToggle(
                    label = "Bluetooth",
                    description = "Enable Bluetooth connectivity",
                    checked = settings.bluetoothEnabled,
                    onCheckedChange = { viewModel.refreshBluetoothInfo() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Connect",
                    description = "Automatically connect to known devices",
                    checked = settings.autoConnect,
                    onCheckedChange = { viewModel.updateAutoConnect(it) }
                )
            }
            // Paired Devices
            SettingsCard(
                title = "Paired Devices",
                icon = Icons.Default.Devices
            ) {
                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No paired devices found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    pairedDevices.forEach { device ->
                        SettingsRow(
                            label = device.name,
                            value = if (device.isConnected) "Connected" else "Disconnected"
                        )
                        if (device != pairedDevices.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.scanForDevices() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan for Devices")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkSettingsScreenPreview() {
    IRCameraTheme {
        NetworkSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\network\ui\SimpleNetworkTestComposeActivity.kt =====

package mpdc4gsr.feature.network.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.csl.irCamera.BuildConfig
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class NetworkConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class NetworkTestCommand(
    val name: String,
    val description: String,
    val command: String,
    val expectedResponse: String
)

data class TestResult(
    val command: String,
    val response: String,
    val success: Boolean,
    val timestamp: Long
)

class SimpleNetworkTestViewModel : AppBaseViewModel() {
    private val _connectionStatus = mutableStateOf(NetworkConnectionStatus.DISCONNECTED)
    val connectionStatus: State<NetworkConnectionStatus> = _connectionStatus
    private val _ipAddress = mutableStateOf("192.168.1.100")
    val ipAddress: State<String> = _ipAddress
    private val _port = mutableStateOf("8080")
    val port: State<String> = _port
    private val _ipAddressError = mutableStateOf<String?>(null)
    val ipAddressError: State<String?> = _ipAddressError
    private val _portError = mutableStateOf<String?>(null)
    val portError: State<String?> = _portError
    private val _statusMessage = mutableStateOf("Ready to connect to PC Remote Control")
    val statusMessage: State<String> = _statusMessage
    private val _testResults = mutableStateOf<List<TestResult>>(emptyList())
    val testResults: State<List<TestResult>> = _testResults
    private val _isRunningTests = mutableStateOf(false)
    val isRunningTests: State<Boolean> = _isRunningTests
    private val _testCommands = mutableStateOf(
        listOf(
            NetworkTestCommand(
                "Start Recording",
                "Initiates recording on the mobile device",
                "START_RECORDING",
                "RECORDING_STARTED"
            ),
            NetworkTestCommand(
                "Stop Recording",
                "Stops recording on the mobile device",
                "STOP_RECORDING",
                "RECORDING_STOPPED"
            ),
            NetworkTestCommand(
                "Get Status",
                "Retrieves current device status",
                "GET_STATUS",
                "STATUS_OK"
            ),
            NetworkTestCommand(
                "Get Battery",
                "Retrieves battery level information",
                "GET_BATTERY",
                "BATTERY_85"
            ),
            NetworkTestCommand(
                "Connect Thermal",
                "Connect to thermal camera device",
                "CONNECT_THERMAL",
                "THERMAL_CONNECTED"
            ),
            NetworkTestCommand(
                "Connect GSR",
                "Connect to GSR sensor device",
                "CONNECT_GSR",
                "GSR_CONNECTED"
            )
        )
    )
    val testCommands: State<List<NetworkTestCommand>> = _testCommands
    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
        _ipAddressError.value = validateIpAddress(ip)
    }

    fun updatePort(portStr: String) {
        _port.value = portStr
        _portError.value = validatePort(portStr)
    }

    private fun validateIpAddress(ip: String): String? {
        if (ip.isBlank()) {
            return "IP address cannot be empty"
        }
        val parts = ip.split(".")
        if (parts.size != 4) {
            return "Invalid IP address format"
        }
        for (part in parts) {
            val num = part.toIntOrNull()
            if (num == null || num < 0 || num > 255) {
                return "Invalid IP address range"
            }
        }
        return null
    }

    private fun validatePort(portStr: String): String? {
        if (portStr.isBlank()) {
            return "Port cannot be empty"
        }
        val port = portStr.toIntOrNull()
        if (port == null) {
            return "Port must be a number"
        }
        if (port < 1 || port > 65535) {
            return "Port must be between 1 and 65535"
        }
        return null
    }

    fun connect() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _connectionStatus.value = NetworkConnectionStatus.CONNECTING
            _statusMessage.value = "Connecting to ${_ipAddress.value}:${_port.value}..."
            delay(2000) // Simulate connection time
            // Simulate connection result (80% success rate)
            val success = kotlin.random.Random.nextFloat() > 0.2f
            if (success) {
                _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                _statusMessage.value = "Connected to PC Remote Control successfully"
            } else {
                _connectionStatus.value = NetworkConnectionStatus.ERROR
                _statusMessage.value = "Failed to connect. Check IP address and port."
            }
        }
    }

    fun disconnect() {
        _connectionStatus.value = NetworkConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected from PC Remote Control"
        _testResults.value = emptyList()
    }

    fun runAllTests() {
        if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRunningTests.value = true
            _testResults.value = emptyList()
            _statusMessage.value = "Running automated test suite..."
            _testCommands.value.forEach { testCommand ->
                _statusMessage.value = "Testing: ${testCommand.name}"
                delay(1500)
                val success = kotlin.random.Random.nextFloat() > 0.15f // 85% success rate
                val response = if (success) {
                    testCommand.expectedResponse
                } else {
                    "ERROR_${kotlin.random.Random.nextInt(100, 999)}"
                }
                val result = TestResult(
                    command = testCommand.command,
                    response = response,
                    success = success,
                    timestamp = System.currentTimeMillis()
                )
                _testResults.value = _testResults.value + result
                delay(500)
            }
            val successCount = _testResults.value.count { it.success }
            val totalCount = _testResults.value.size
            _statusMessage.value = "Test suite complete: $successCount/$totalCount tests passed"
            _isRunningTests.value = false
        }
    }

    fun runSingleTest(command: NetworkTestCommand) {
        if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _statusMessage.value = "Testing: ${command.name}"
            delay(1000)
            val success = kotlin.random.Random.nextFloat() > 0.2f // 80% success rate
            val response = if (success) {
                command.expectedResponse
            } else {
                "ERROR_TIMEOUT"
            }
            val result = TestResult(
                command = command.command,
                response = response,
                success = success,
                timestamp = System.currentTimeMillis()
            )
            _testResults.value = _testResults.value + result
            _statusMessage.value = if (success) {
                "${command.name} test passed"
            } else {
                "${command.name} test failed"
            }
        }
    }

    fun clearResults() {
        _testResults.value = emptyList()
        _statusMessage.value = "Test results cleared"
    }
}

class SimpleNetworkTestActivityCompose : BaseComposeActivity<SimpleNetworkTestViewModel>() {
    override fun createViewModel(): SimpleNetworkTestViewModel =
        viewModels<SimpleNetworkTestViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SimpleNetworkTestViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val connectionStatus by viewModel.connectionStatus
            val ipAddress by viewModel.ipAddress
            val port by viewModel.port
            val ipAddressError by viewModel.ipAddressError
            val portError by viewModel.portError
            val statusMessage by viewModel.statusMessage
            val testResults by viewModel.testResults
            val isRunningTests by viewModel.isRunningTests
            val testCommands by viewModel.testCommands
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Network Test Interface",
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = { viewModel.clearResults() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Connection status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (connectionStatus) {
                                NetworkConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                                NetworkConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                                NetworkConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (connectionStatus) {
                                NetworkConnectionStatus.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }

                                NetworkConnectionStatus.CONNECTED -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                NetworkConnectionStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = connectionStatus.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Connection settings
                    if (connectionStatus == NetworkConnectionStatus.DISCONNECTED) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "PC Remote Control Connection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                OutlinedTextField(
                                    value = ipAddress,
                                    onValueChange = { viewModel.updateIpAddress(it) },
                                    label = { Text("IP Address") },
                                    placeholder = { Text("192.168.1.100") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    singleLine = true,
                                    isError = ipAddressError != null,
                                    supportingText = ipAddressError?.let { { Text(it) } },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            // Focus moves to next field automatically
                                        }
                                    )
                                )
                                OutlinedTextField(
                                    value = port,
                                    onValueChange = { viewModel.updatePort(it) },
                                    label = { Text("Port") },
                                    placeholder = { Text("8080") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    singleLine = true,
                                    isError = portError != null,
                                    supportingText = portError?.let { { Text(it) } },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                        }
                                    )
                                )
                                Button(
                                    onClick = { viewModel.connect() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = ipAddressError == null && portError == null
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Connect",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect to PC")
                                }
                            }
                        }
                    } else if (connectionStatus == NetworkConnectionStatus.CONNECTED) {
                        // Test controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runAllTests() },
                                modifier = Modifier.weight(1f),
                                enabled = !isRunningTests
                            ) {
                                if (isRunningTests) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run All Tests")
                            }
                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect")
                            }
                        }
                        // Individual test commands
                        Text(
                            text = "Test Commands",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        testCommands.forEach { command ->
                            NetworkTestCommandCard(
                                command = command,
                                onTest = { viewModel.runSingleTest(command) },
                                isTestingEnabled = !isRunningTests,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Test results
                        if (testResults.isNotEmpty()) {
                            Text(
                                text = "Test Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    testResults.takeLast(10).forEach { result ->
                                        TestResultRow(
                                            result = result,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (connectionStatus == NetworkConnectionStatus.ERROR) {
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Connection")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Information card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PC Remote Control Testing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This interface tests bidirectional communication with PC Remote Control. Commands are sent to the PC and responses are validated. App version: ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTestCommandCard(
    command: NetworkTestCommand,
    onTest: () -> Unit,
    isTestingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Command: ${command.command}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onTest,
                enabled = isTestingEnabled
            ) {
                Text("Test")
            }
        }
    }
}

@Composable
private fun TestResultRow(
    result: TestResult,
    modifier: Modifier = Modifier
) {
    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.command,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.response,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.success)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = timeFormat.format(java.util.Date(result.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}