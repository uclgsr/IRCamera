package mpdc4gsr.presentation.screens.network

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
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
import mpdc4gsr.presentation.screens.network.DevicePairingViewModel
import mpdc4gsr.ui.deferAction

@AndroidEntryPoint
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
