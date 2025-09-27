package mpdc4gsr.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.launch
import mpdc4gsr.network.NetworkSettings
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of NetworkConfigActivity for configuring network connection settings.
 * Shows how to handle Bluetooth permissions and device discovery in Compose.
 */
class NetworkConfigComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "NetworkConfigComposeActivity"
    }

    private lateinit var networkSettings: NetworkSettings
    private lateinit var permissionManager: PermissionManager
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val bluetoothEnableResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth enabled, can proceed with device discovery
        } else {
            Toast.makeText(this, "Bluetooth is required for device connection", Toast.LENGTH_SHORT)
                .show()
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            initializeComponents()
        }

        IRCameraTheme {
            NetworkConfigScreen(
                onRefresh = { refreshNetworkStatus() },
                onTestBluetooth = { testBluetoothConfiguration() }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NetworkConfigScreen(
        onRefresh: () -> Unit,
        onTestBluetooth: () -> Unit
    ) {
        var networkStatus by remember { mutableStateOf("Initializing...") }
        var bluetoothEnabled by remember { mutableStateOf(false) }
        var availableDevices by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            networkStatus = networkSettings?.getConnectionSummary() ?: "Not initialized"
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Network Configuration",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Network Status Card
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Network",
                                        tint = Color(0xFF6B35FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Network Status",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                IconButton(onClick = onRefresh) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = networkStatus,
                                color = Color(0xCCFFFFFF),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                item {
                    // Bluetooth Configuration Card
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                                        imageVector = if (bluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                        contentDescription = "Bluetooth",
                                        tint = if (bluetoothEnabled) Color(0xFF6B35FF) else Color(0xFF808080),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Bluetooth Configuration",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (bluetoothEnabled) "Bluetooth is enabled" else "Bluetooth is disabled",
                                color = Color(0xCCFFFFFF),
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onTestBluetooth,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Test Bluetooth Configuration")
                                }
                            }
                        }
                    }
                }

                // Available Devices
                if (availableDevices.isNotEmpty()) {
                    item {
                        CommonComponents.SectionHeader(
                            text = "Available Devices"
                        )
                    }

                    items(availableDevices) { device ->
                        CommonComponents.IRCameraCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    Toast.makeText(
                                        this@NetworkConfigComposeActivity,
                                        "Selected: $device",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Device",
                                    tint = Color(0xFF6B35FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = device,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun initializeComponents() {
        networkSettings = NetworkSettings(this)
        permissionManager = PermissionManager(this, PermissionController(this))
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        Log.i(TAG, "Network configuration activity opened")
    }

    private fun refreshNetworkStatus() {
        // Update network status
        // This would typically refresh the actual network settings
        Toast.makeText(this, "Network status refreshed", Toast.LENGTH_SHORT).show()
    }

    private fun testBluetoothConfiguration() {
        lifecycleScope.launch {
            try {
                val hasPermission = permissionManager.requestBluetoothPermissions()
                if (hasPermission) {
                    Log.i(TAG, "Bluetooth permissions granted")
                    showBluetoothDevices()
                } else {
                    Log.w(TAG, "Bluetooth permissions denied")
                    Toast.makeText(
                        this@NetworkConfigComposeActivity,
                        "Bluetooth permissions required for device connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error testing Bluetooth configuration", e)
                Toast.makeText(
                    this@NetworkConfigComposeActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showBluetoothDevices() {
        bluetoothAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
                pairedDevices?.forEach { device ->
                    Log.i(TAG, "Paired device: ${device.name} - ${device.address}")
                }
                
                Toast.makeText(
                    this,
                    "Found ${pairedDevices?.size ?: 0} paired devices",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Request to enable Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableResult.launch(enableBtIntent)
            }
        }
    }
}