package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of DevicePairingActivity demonstrating network device discovery and pairing.
 * Shows how to handle device scanning, connection management, and network status in Compose.
 */
class DevicePairingComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "DevicePairingComposeActivity"

        fun start(context: Context) {
            val intent = Intent(context, DevicePairingComposeActivity::class.java)
            context.startActivity(intent)
        }
    }

    @Composable
    override fun Content() {
        IRCameraTheme {
            DevicePairingScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DevicePairingScreen() {
        var isScanning by remember { mutableStateOf(false) }
        var connectedController by remember { mutableStateOf<ControllerInfo?>(null) }
        var discoveredControllers by remember { mutableStateOf<List<ControllerInfo>>(emptyList()) }
        var connectionStatus by remember { mutableStateOf("Not connected") }
        var lastScanTime by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Device Pairing",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Connection Status Card
                item {
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
                                    imageVector = if (connectedController != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Connection Status",
                                    tint = if (connectedController != null) Color.Green else Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Connection Status",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = connectionStatus,
                                    color = if (connectedController != null) Color.Green else Color.Red,
                                    fontSize = 12.sp
                                )
                            }

                            connectedController?.let { controller ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF3A3A3A)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Connected Device",
                                            color = Color(0xFF6B35FF),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        InfoRow("Name", controller.name)
                                        InfoRow("IP Address", controller.ipAddress)
                                        InfoRow("Port", controller.port.toString())
                                        InfoRow("Type", controller.type)
                                    }
                                }
                            }
                        }
                    }
                }

                // Control Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Button
                        Button(
                            onClick = {
                                startControllerScan { scanning, controllers, scanTime ->
                                    isScanning = scanning
                                    if (!scanning) {
                                        discoveredControllers = controllers
                                        lastScanTime = scanTime
                                    }
                                }
                            },
                            enabled = !isScanning && connectedController == null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Scan",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isScanning) "Scanning..." else "Scan for Devices")
                        }

                        // Disconnect Button
                        Button(
                            onClick = {
                                disconnectFromController { controller, status ->
                                    connectedController = controller
                                    connectionStatus = status
                                }
                            },
                            enabled = connectedController != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect")
                        }
                    }
                }

                // Scan Results
                if (discoveredControllers.isNotEmpty()) {
                    item {
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
                                    Text(
                                        text = "Discovered Devices",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    lastScanTime?.let { time ->
                                        Text(
                                            text = "Last scan: $time",
                                            color = Color(0x80FFFFFF),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                discoveredControllers.forEach { controller ->
                                    ControllerItem(
                                        controller = controller,
                                        isConnected = connectedController?.id == controller.id
                                    ) {
                                        connectToController(controller) { connected, status ->
                                            if (connected) {
                                                connectedController = controller
                                                connectionStatus = "Connected to ${controller.name}"
                                            } else {
                                                connectionStatus = status
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // Instructions Card
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Instructions",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val instructions = listOf(
                                "1. Ensure PC Controller is running on the target device",
                                "2. Make sure both devices are on the same network",
                                "3. Click 'Scan for Devices' to discover available controllers",
                                "4. Select a controller from the list to connect",
                                "5. Once connected, you can start multi-modal recording"
                            )

                            instructions.forEach { instruction ->
                                Text(
                                    text = instruction,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun ControllerItem(
        controller: ControllerInfo,
        isConnected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFF2A4A2A) else Color(0xFF3A3A3A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (controller.type) {
                        "PC Controller" -> Icons.Default.Computer
                        "Mobile Hub" -> Icons.Default.PhoneAndroid
                        else -> Icons.Default.DeviceUnknown
                    },
                    contentDescription = controller.type,
                    tint = if (isConnected) Color.Green else Color(0xFF6B35FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = controller.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${controller.ipAddress}:${controller.port}",
                        color = Color(0x80FFFFFF),
                        fontSize = 12.sp
                    )
                    Text(
                        text = controller.type,
                        color = Color(0x80FFFFFF),
                        fontSize = 12.sp
                    )
                }
                if (isConnected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Connect",
                        tint = Color(0x80FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    private fun startControllerScan(onResult: (Boolean, List<ControllerInfo>, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(true, emptyList(), null)
                delay(3000) // Simulate scanning
                
                val mockControllers = listOf(
                    ControllerInfo(
                        id = "ctrl1",
                        name = "Research PC",
                        ipAddress = "192.168.1.100",
                        port = 8080,
                        type = "PC Controller"
                    ),
                    ControllerInfo(
                        id = "ctrl2",
                        name = "Lab Controller",
                        ipAddress = "192.168.1.101",
                        port = 8080,
                        type = "PC Controller"
                    ),
                    ControllerInfo(
                        id = "ctrl3",
                        name = "Mobile Hub",
                        ipAddress = "192.168.1.102",
                        port = 9090,
                        type = "Mobile Hub"
                    )
                )
                
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                
                onResult(false, mockControllers, currentTime)
                showToast("Found ${mockControllers.size} devices")
            } catch (e: Exception) {
                onResult(false, emptyList(), null)
                showToast("Scan failed: ${e.message}")
            }
        }
    }

    private fun connectToController(
        controller: ControllerInfo,
        onResult: (Boolean, String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                // Simulate connection attempt
                delay(1500)
                onResult(true, "Connected")
                showToast("Connected to ${controller.name}")
            } catch (e: Exception) {
                onResult(false, "Connection failed: ${e.message}")
                showToast("Failed to connect to ${controller.name}")
            }
        }
    }

    private fun disconnectFromController(onResult: (ControllerInfo?, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(null, "Not connected")
                showToast("Disconnected from controller")
            } catch (e: Exception) {
                onResult(null, "Disconnect failed")
                showToast("Failed to disconnect")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class ControllerInfo(
        val id: String,
        val name: String,
        val ipAddress: String,
        val port: Int,
        val type: String
    )
}