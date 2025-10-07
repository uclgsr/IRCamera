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