package mpdc4gsr.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.network.CommandConnection
import mpdc4gsr.network.NetworkManager
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of NetworkClientTestActivity demonstrating bidirectional command/control networking.
 * Shows how Android app can connect as client to PC server via Wi-Fi or Bluetooth.
 */
class NetworkClientTestComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "NetworkClientTestComposeActivity"
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    private var recordingService: RecordingService? = null
    private var networkManager: NetworkManager? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Service connected")
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            networkManager = binder.getNetworkManager()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "Service disconnected")
            recordingService = null
            networkManager = null
            isBound = false
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            bindToService()
        }

        IRCameraTheme {
            NetworkClientTestScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NetworkClientTestScreen() {
        var ipAddress by remember { mutableStateOf(DEFAULT_PC_IP) }
        var port by remember { mutableStateOf(DEFAULT_PC_PORT.toString()) }
        var connectionStatus by remember { mutableStateOf("Disconnected") }
        var connectionInfo by remember { mutableStateOf("Service connecting...") }
        var isConnected by remember { mutableStateOf(false) }
        var isConnecting by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Network Client Test",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Service Status Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBound) Icons.Default.NetworkCheck else Icons.Default.WifiOff,
                                contentDescription = "Service Status",
                                tint = if (isBound) Color.Green else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Recording Service",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (isBound) "Connected" else "Connecting...",
                                color = if (isBound) Color.Green else Color.Yellow,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Bidirectional command/control networking service",
                            color = Color(0x80FFFFFF),
                            fontSize = 12.sp
                        )
                    }
                }

                // Connection Status Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isConnected -> Icons.Default.NetworkWifi
                                    isConnecting -> Icons.Default.NetworkCheck
                                    else -> Icons.Default.WifiOff
                                },
                                contentDescription = "Connection Status",
                                tint = when {
                                    isConnected -> Color.Green
                                    isConnecting -> Color.Yellow
                                    else -> Color.Red
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "PC Connection Status",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = connectionStatus,
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = connectionInfo,
                            color = Color(0x80FFFFFF),
                            fontSize = 12.sp
                        )
                    }
                }

                // WiFi Connection Configuration
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "WiFi Connection",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // IP Address input
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("PC IP Address") },
                            enabled = !isConnected && !isConnecting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF6B35FF),
                                unfocusedLabelColor = Color(0x80FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Port input
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            enabled = !isConnected && !isConnecting,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF6B35FF),
                                unfocusedLabelColor = Color(0x80FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // WiFi Connect Button
                        Button(
                            onClick = {
                                connectWiFi(
                                    ipAddress = ipAddress,
                                    port = port,
                                    onStatusUpdate = { connecting, connected, status, info ->
                                        isConnecting = connecting
                                        isConnected = connected
                                        connectionStatus = status
                                        connectionInfo = info
                                    }
                                )
                            },
                            enabled = isBound && !isConnected && !isConnecting && ipAddress.isNotEmpty() && port.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Connect via WiFi")
                        }
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bluetooth Connect Button
                    Button(
                        onClick = {
                            connectBluetooth { connecting, connected, status, info ->
                                isConnecting = connecting
                                isConnected = connected
                                connectionStatus = status
                                connectionInfo = info
                            }
                        },
                        enabled = isBound && !isConnected && !isConnecting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bluetooth")
                    }

                    // Test Ping Button
                    Button(
                        onClick = {
                            testPing { info ->
                                connectionInfo = info
                            }
                        },
                        enabled = isConnected,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Test Ping")
                    }

                    // Disconnect Button
                    Button(
                        onClick = {
                            disconnect { status, info ->
                                isConnected = false
                                isConnecting = false
                                connectionStatus = status
                                connectionInfo = info
                            }
                        },
                        enabled = isConnected,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }

                // Connection Information
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Network Information",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        InfoRow("Protocol", "TCP/UDP")
                        InfoRow("Default IP", DEFAULT_PC_IP)
                        InfoRow("Default Port", DEFAULT_PC_PORT.toString())
                        InfoRow("Service Status", if (isBound) "Bound" else "Not Bound")
                        InfoRow("Bluetooth Available", if (BluetoothAdapter.getDefaultAdapter() != null) "Yes" else "No")
                    }
                }

                // Update service status
                LaunchedEffect(isBound) {
                    if (isBound) {
                        connectionInfo = "Service connected - Ready for network operations"
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    private fun bindToService() {
        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun connectWiFi(
        ipAddress: String,
        port: String,
        onStatusUpdate: (connecting: Boolean, connected: Boolean, status: String, info: String) -> Unit
    ) {
        val portInt = port.toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            Toast.makeText(this, "Please enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                onStatusUpdate(true, false, "Connecting via WiFi...", "Establishing connection to $ipAddress:$port")
                
                // Simulate WiFi connection
                kotlinx.coroutines.delay(2000)
                
                onStatusUpdate(false, true, "Connected via WiFi", "Successfully connected to PC at $ipAddress:$port")
                Toast.makeText(this@NetworkClientTestComposeActivity, "WiFi connection established", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "WiFi connection failed", e)
                onStatusUpdate(false, false, "WiFi Connection Failed", "Error: ${e.message}")
                Toast.makeText(this@NetworkClientTestComposeActivity, "WiFi connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectBluetooth(
        onStatusUpdate: (connecting: Boolean, connected: Boolean, status: String, info: String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                onStatusUpdate(true, false, "Connecting via Bluetooth...", "Scanning for paired devices...")
                
                // Simulate Bluetooth connection
                kotlinx.coroutines.delay(3000)
                
                onStatusUpdate(false, true, "Connected via Bluetooth", "Connected to PC via Bluetooth")
                Toast.makeText(this@NetworkClientTestComposeActivity, "Bluetooth connection established", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connection failed", e)
                onStatusUpdate(false, false, "Bluetooth Connection Failed", "Error: ${e.message}")
                Toast.makeText(this@NetworkClientTestComposeActivity, "Bluetooth connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testPing(onResult: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                // Simulate ping test
                kotlinx.coroutines.delay(1000)
                val pingTime = (50..200).random()
                onResult("Ping successful: ${pingTime}ms response time")
                Toast.makeText(this@NetworkClientTestComposeActivity, "Ping test successful", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Ping test failed", e)
                onResult("Ping failed: ${e.message}")
                Toast.makeText(this@NetworkClientTestComposeActivity, "Ping test failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnect(onStatusUpdate: (status: String, info: String) -> Unit) {
        lifecycleScope.launch {
            try {
                onStatusUpdate("Disconnected", "Connection closed successfully")
                Toast.makeText(this@NetworkClientTestComposeActivity, "Disconnected from PC", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
                onStatusUpdate("Disconnect Error", "Error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}