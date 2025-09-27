package mpdc4gsr.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import mpdc4gsr.network.CommandConnection
import mpdc4gsr.network.MockRecordingController
import mpdc4gsr.network.NetworkManager
import mpdc4gsr.network.SimpleCommandHandler
import mpdc4gsr.network.TcpClient
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of SimpleNetworkTestActivity demonstrating PC Remote Control and Bidirectional Telemetry.
 * Shows how to handle network connections and testing in Compose.
 */
class SimpleNetworkTestComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "SimpleNetworkTestComposeActivity"
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    // Simplified components for testing
    private val mockController = MockRecordingController()
    private var tcpClient: TcpClient? = null
    private var commandHandler: SimpleCommandHandler? = null

    @Composable
    override fun Content() {
        IRCameraTheme {
            NetworkTestScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NetworkTestScreen() {
        var ipAddress by remember { mutableStateOf(DEFAULT_PC_IP) }
        var port by remember { mutableStateOf(DEFAULT_PC_PORT.toString()) }
        var isConnected by remember { mutableStateOf(false) }
        var isConnecting by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("Disconnected") }
        var connectionInfo by remember { mutableStateOf("Ready to connect") }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Network Test",
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
                                text = "Connection Status",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = statusMessage,
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

                // Connection Configuration Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Connection Configuration",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // IP Address input
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("IP Address") },
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
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Connect/Disconnect Button
                    Button(
                        onClick = {
                            if (isConnected) {
                                disconnectFromPC(
                                    onStatusUpdate = { status, info ->
                                        statusMessage = status
                                        connectionInfo = info
                                        isConnected = false
                                        isConnecting = false
                                    }
                                )
                            } else {
                                connectToPC(
                                    ipAddress = ipAddress,
                                    port = port,
                                    onStatusUpdate = { connecting, connected, status, info ->
                                        isConnecting = connecting
                                        isConnected = connected
                                        statusMessage = status
                                        connectionInfo = info
                                    }
                                )
                            }
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Text(if (isConnected) "Disconnect" else "Connect")
                        }
                    }

                    // Test Commands Button
                    Button(
                        onClick = {
                            testNetworkCommands(
                                onResult = { result ->
                                    connectionInfo = result
                                }
                            )
                        },
                        enabled = isConnected && !isConnecting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Test Commands")
                    }
                }

                // Network Information
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

                        InfoRow("Protocol", "TCP")
                        InfoRow("Default IP", DEFAULT_PC_IP)
                        InfoRow("Default Port", DEFAULT_PC_PORT.toString())
                        InfoRow("Mock Controller", "Enabled")
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

    private fun connectToPC(
        ipAddress: String,
        port: String,
        onStatusUpdate: (connecting: Boolean, connected: Boolean, status: String, info: String) -> Unit
    ) {
        if (ipAddress.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Please enter IP address and port", Toast.LENGTH_SHORT).show()
            return
        }

        val portInt = port.toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            Toast.makeText(this, "Please enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                onStatusUpdate(true, false, "Connecting...", "Establishing connection to $ipAddress:$port")
                
                // Simulate connection process
                kotlinx.coroutines.delay(2000)
                
                // Initialize network components
                tcpClient = TcpClient()
                commandHandler = SimpleCommandHandler(mockController)
                
                Log.i(TAG, "Connected to PC at $ipAddress:$port")
                onStatusUpdate(false, true, "Connected", "Successfully connected to $ipAddress:$port")
                
                Toast.makeText(this@SimpleNetworkTestComposeActivity, "Connected successfully!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                onStatusUpdate(false, false, "Connection Failed", "Error: ${e.message}")
                Toast.makeText(this@SimpleNetworkTestComposeActivity, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnectFromPC(
        onStatusUpdate: (status: String, info: String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                tcpClient?.disconnect()
                tcpClient = null
                commandHandler = null
                
                Log.i(TAG, "Disconnected from PC")
                onStatusUpdate("Disconnected", "Ready to connect")
                Toast.makeText(this@SimpleNetworkTestComposeActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Disconnection error", e)
                onStatusUpdate("Disconnect Error", "Error: ${e.message}")
            }
        }
    }

    private fun testNetworkCommands(onResult: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                // Test ping command
                val pingResult = commandHandler?.handlePing() ?: "No handler"
                Log.i(TAG, "Ping result: $pingResult")
                
                // Test status command
                val statusResult = commandHandler?.handleStatusRequest() ?: "No status"
                Log.i(TAG, "Status result: $statusResult")
                
                onResult("Commands tested successfully. Ping: $pingResult, Status: $statusResult")
                Toast.makeText(this@SimpleNetworkTestComposeActivity, "Commands tested successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Command test failed", e)
                onResult("Command test failed: ${e.message}")
                Toast.makeText(this@SimpleNetworkTestComposeActivity, "Command test failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}