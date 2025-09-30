package mpdc4gsr.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope

/**
 * GSRDeviceManagementActivityCompose - Enhanced Compose GSR Device Management
 *
 * Modern interface for managing GSR sensor devices with:
 * - Real-time device discovery and scanning
 * - Interactive device connection management
 * - Battery level monitoring and device status
 * - Permission management with user-friendly explanations
 * - Automated reconnection capabilities
 */
class GSRDeviceManagementActivityCompose : BaseComposeActivity<GSRDeviceManagementViewModel>() {

    override fun createViewModel(): GSRDeviceManagementViewModel = GSRDeviceManagementViewModel()

    @Composable
    override fun Content(viewModel: GSRDeviceManagementViewModel) {
        IRCameraTheme {
            GSRDeviceManagementScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRDeviceManagementScreen(viewModel: GSRDeviceManagementViewModel) {
    val uiState by viewModel.deviceState.collectAsState()
    val context = LocalContext.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDeviceDetails by remember { mutableStateOf<GSRDevice?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GSR Device Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { viewModel.startScanning(context) },
                enabled = uiState.isBluetoothEnabled && uiState.hasPermissions
            ) {
                Icon(
                    imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                    contentDescription = if (uiState.isScanning) "Stop Scanning" else "Start Scanning"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Status Card
            item {
                PermissionStatusCard(
                    hasPermissions = uiState.hasPermissions,
                    onRequestPermissions = { showPermissionDialog = true }
                )
            }

            // Bluetooth Status Card
            item {
                BluetoothStatusCard(
                    isEnabled = uiState.isBluetoothEnabled,
                    onEnableBluetooth = { viewModel.enableBluetooth(context) }
                )
            }

            // Scanning Status Card
            if (uiState.isScanning) {
                item {
                    ScanningStatusCard(
                        devicesFound = uiState.discoveredDevices.size,
                        onStopScanning = { viewModel.stopScanning() }
                    )
                }
            }

            // Connected Devices Section
            if (uiState.connectedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected Devices",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.connectedDevices) { device ->
                    GSRDeviceCard(
                        device = device,
                        isConnected = true,
                        onConnect = { /* Already connected */ },
                        onDisconnect = { viewModel.disconnectDevice(device) },
                        onShowDetails = { showDeviceDetails = device }
                    )
                }
            }

            // Discovered Devices Section
            if (uiState.discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Discovered Devices",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.discoveredDevices) { device ->
                    GSRDeviceCard(
                        device = device,
                        isConnected = false,
                        onConnect = { viewModel.connectDevice(device) },
                        onDisconnect = { /* Not connected */ },
                        onShowDetails = { showDeviceDetails = device }
                    )
                }
            }

            // Empty State
            if (!uiState.isScanning && uiState.discoveredDevices.isEmpty() && uiState.connectedDevices.isEmpty()) {
                item {
                    EmptyDevicesState(
                        onStartScanning = { viewModel.startScanning(context) },
                        canScan = uiState.hasPermissions && uiState.isBluetoothEnabled
                    )
                }
            }
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        PermissionExplanationDialog(
            onDismiss = { showPermissionDialog = false },
            onRequestPermissions = {
                showPermissionDialog = false
                viewModel.requestPermissions(context)
            }
        )
    }

    // Device Details Dialog
    showDeviceDetails?.let { device ->
        DeviceDetailsDialog(
            device = device,
            onDismiss = { showDeviceDetails = null }
        )
    }
}

@Composable
fun PermissionStatusCard(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermissions) Color(0xFF4CAF50).copy(alpha = 0.1f)
            else Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasPermissions) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasPermissions) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPermissions) "Permissions Granted" else "Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (hasPermissions) "Bluetooth scanning is enabled"
                    else "Bluetooth permissions needed for device discovery",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!hasPermissions) {
                Button(onClick = onRequestPermissions) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
fun BluetoothStatusCard(
    isEnabled: Boolean,
    onEnableBluetooth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFF2196F3).copy(alpha = 0.1f)
            else Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isEnabled) Color(0xFF2196F3) else Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Bluetooth Enabled" else "Bluetooth Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isEnabled) "Ready for device discovery"
                    else "Enable Bluetooth to discover GSR devices",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!isEnabled) {
                Button(onClick = onEnableBluetooth) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
fun ScanningStatusCard(
    devicesFound: Int,
    onStopScanning: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scanning for Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$devicesFound devices found",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedButton(onClick = onStopScanning) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun GSRDeviceCard(
    device: GSRDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onShowDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (isConnected && device.batteryLevel != null) {
                        Text(
                            text = "Battery: ${device.batteryLevel}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column {
                    if (isConnected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(onClick = onConnect) {
                            Text("Connect")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(onClick = onShowDetails) {
                        Text("Details")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDevicesState(
    onStartScanning: () -> Unit,
    canScan: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No GSR Devices Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (canScan) "Start scanning to discover nearby GSR sensors"
                else "Enable permissions and Bluetooth to discover devices",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (canScan) {
                Button(onClick = onStartScanning) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Scanning")
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationDialog(
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth Permissions Required") },
        text = {
            Text("GSR device management requires Bluetooth permissions to discover and connect to nearby sensors. This enables real-time physiological monitoring capabilities.")
        },
        confirmButton = {
            Button(onClick = onRequestPermissions) {
                Text("Grant Permissions")
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
fun DeviceDetailsDialog(
    device: GSRDevice,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Details") },
        text = {
            Column {
                Text("Name: ${device.name ?: "Unknown"}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Address: ${device.address}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Type: ${device.type}")
                if (device.batteryLevel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Battery: ${device.batteryLevel}%")
                }
                if (device.signalStrength != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Signal: ${device.signalStrength} dBm")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Data classes
data class GSRDevice(
    val address: String,
    val name: String?,
    val type: String = "Shimmer GSR",
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null,
    val isConnected: Boolean = false
)

data class GSRDeviceManagementUiState(
    val hasPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val discoveredDevices: List<GSRDevice> = emptyList(),
    val connectedDevices: List<GSRDevice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel placeholder
class GSRDeviceManagementViewModel : BaseViewModel() {
    private val _deviceState = MutableStateFlow(GSRDeviceManagementUiState())
    val deviceState: StateFlow<GSRDeviceManagementUiState> = _deviceState.asStateFlow()

    private var scanningJob: Job? = null

    fun checkPermissions(context: Context) {
        val hasPermissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val isBluetoothEnabled = bluetoothManager.adapter?.isEnabled == true

        _deviceState.value = _deviceState.value.copy(
            hasPermissions = hasPermissions,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }

    fun startScanning(context: Context) {
        _deviceState.value = _deviceState.value.copy(isScanning = true)

        // Cancel any existing scanning job
        scanningJob?.cancel()

        // Simulate device discovery on main dispatcher
        scanningJob = viewModelScope.launch(Dispatchers.Main) {
            delay(2000)
            val mockDevices = listOf(
                GSRDevice("00:11:22:33:44:55", "Shimmer GSR #1", batteryLevel = 85),
                GSRDevice("66:77:88:99:AA:BB", "Shimmer GSR #2", batteryLevel = 92),
                GSRDevice("CC:DD:EE:FF:11:22", "Unknown Device", batteryLevel = null)
            )
            _deviceState.value = _deviceState.value.copy(
                discoveredDevices = mockDevices,
                isScanning = false
            )
        }
    }

    fun stopScanning() {
        _deviceState.value = _deviceState.value.copy(isScanning = false)
        scanningJob?.cancel()
        scanningJob = null
    }

    override fun onCleared() {
        super.onCleared()
        scanningJob?.cancel()
    }

    fun connectDevice(device: GSRDevice) {
        val updatedDevice = device.copy(isConnected = true, batteryLevel = 87)
        _deviceState.value = _deviceState.value.copy(
            connectedDevices = _deviceState.value.connectedDevices + updatedDevice,
            discoveredDevices = _deviceState.value.discoveredDevices.filter { it.address != device.address }
        )
    }

    fun disconnectDevice(device: GSRDevice) {
        _deviceState.value = _deviceState.value.copy(
            connectedDevices = _deviceState.value.connectedDevices.filter { it.address != device.address },
            discoveredDevices = _deviceState.value.discoveredDevices + device.copy(isConnected = false)
        )
    }

    fun enableBluetooth(context: Context) {
        // Implementation would enable Bluetooth
    }

    fun requestPermissions(context: Context) {
        // Implementation would request relevant permissions
    }
}