package mpdc4gsr.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of UnifiedSensorActivity demonstrating multi-modal sensor coordination.
 * Shows how to handle multiple sensors, session management, and network integration in Compose.
 */
class UnifiedSensorComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "UnifiedSensorComposeActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeComponents()
        } else {
            showPermissionError()
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            checkPermissionsAndInitialize()
        }

        IRCameraTheme {
            UnifiedSensorScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun UnifiedSensorScreen() {
        var sessionStatus by remember { mutableStateOf("Ready") }
        var sessionQuality by remember { mutableStateOf(85f) }
        var gsrStatus by remember { mutableStateOf("Disconnected") }
        var networkStatus by remember { mutableStateOf("Offline") }
        var cameraStatus by remember { mutableStateOf("Not initialized") }
        var isSessionActive by remember { mutableStateOf(false) }
        var sessionName by remember { mutableStateOf("") }
        var participantId by remember { mutableStateOf("") }
        var connectedDevices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
        var pcControllers by remember { mutableStateOf<List<PCControllerInfo>>(emptyList()) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Unified Sensor Platform",
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

                // System Status Overview
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
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "System Status",
                                    tint = Color(0xFF6B35FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "System Status",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = sessionStatus,
                                    color = if (isSessionActive) Color.Green else Color.White,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quality Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Session Quality",
                                    color = Color(0x80FFFFFF),
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${sessionQuality.toInt()}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            LinearProgressIndicator(
                                progress = sessionQuality / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                color = when {
                                    sessionQuality >= 80 -> Color.Green
                                    sessionQuality >= 60 -> Color.Yellow
                                    else -> Color.Red
                                }
                            )
                        }
                    }
                }

                // Sensor Status Section
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
                                text = "Sensor Status",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            SensorStatusItem(Icons.Default.Sensors, "GSR Sensor", gsrStatus, gsrStatus == "Connected")
                            SensorStatusItem(Icons.Default.Camera, "RGB Camera", cameraStatus, cameraStatus == "Active")
                            SensorStatusItem(Icons.Default.Wifi, "Network", networkStatus, networkStatus == "Connected")
                            SensorStatusItem(Icons.Default.Thermostat, "Thermal Camera", "Available", true)
                        }
                    }
                }

                // Session Management Section
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
                                text = "Session Management",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Session Name Input
                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = { sessionName = it },
                                label = { Text("Session Name") },
                                enabled = !isSessionActive,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF6B35FF),
                                    unfocusedLabelColor = Color(0x80FFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Participant ID Input
                            OutlinedTextField(
                                value = participantId,
                                onValueChange = { participantId = it },
                                label = { Text("Participant ID") },
                                enabled = !isSessionActive,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF6B35FF),
                                    unfocusedLabelColor = Color(0x80FFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Session Control Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isSessionActive) {
                                            stopSession { active, status, quality ->
                                                isSessionActive = active
                                                sessionStatus = status
                                                sessionQuality = quality
                                            }
                                        } else {
                                            startSession(sessionName, participantId) { active, status, quality ->
                                                isSessionActive = active
                                                sessionStatus = status
                                                sessionQuality = quality
                                            }
                                        }
                                    },
                                    enabled = sessionName.isNotEmpty() && participantId.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSessionActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isSessionActive) "Stop" else "Start",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isSessionActive) "Stop Session" else "Start Session")
                                }

                                Button(
                                    onClick = { addSyncMarker() },
                                    enabled = isSessionActive,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Add Marker",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Marker")
                                }
                            }
                        }
                    }
                }

                // Device Discovery Section
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
                                    text = "Device Discovery",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        startDeviceDiscovery { devices ->
                                            connectedDevices = devices
                                            gsrStatus = if (devices.isNotEmpty()) "Connected" else "Disconnected"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Discover",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Discover")
                                }
                            }

                            if (connectedDevices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                connectedDevices.forEach { device ->
                                    DeviceItem(device) { 
                                        connectToDevice(device)
                                    }
                                }
                            }
                        }
                    }
                }

                // PC Controller Section
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
                                    text = "PC Controllers",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        startPCDiscovery { controllers ->
                                            pcControllers = controllers
                                            networkStatus = if (controllers.isNotEmpty()) "Connected" else "Offline"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = "Discover PC",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Discover PC")
                                }
                            }

                            if (pcControllers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                pcControllers.forEach { controller ->
                                    PCControllerItem(controller) {
                                        connectToPC(controller)
                                    }
                                }
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
    private fun SensorStatusItem(
        icon: ImageVector,
        name: String,
        status: String,
        isConnected: Boolean
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isConnected) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = status,
                color = if (isConnected) Color.Green else Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun DeviceItem(device: DeviceInfo, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3A3A3A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Device",
                    tint = Color(0xFF6B35FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = device.name,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = device.type,
                    color = Color(0x80FFFFFF),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    private fun PCControllerItem(controller: PCControllerInfo, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3A3A3A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = "PC Controller",
                    tint = Color(0xFF6B35FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = controller.name,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = controller.ipAddress,
                    color = Color(0x80FFFFFF),
                    fontSize = 12.sp
                )
            }
        }
    }

    private fun checkPermissionsAndInitialize() {
        // Simulate permission check
        initializeComponents()
    }

    private fun initializeComponents() {
        lifecycleScope.launch {
            try {
                // Simulate component initialization
                delay(1000)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Unified sensor platform initialized", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                showPermissionError()
            }
        }
    }

    private fun startSession(name: String, participantId: String, onResult: (Boolean, String, Float) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(true, "Session Active: $name", 88f)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Session started: $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onResult(false, "Session Failed", 0f)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Failed to start session", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopSession(onResult: (Boolean, String, Float) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(false, "Session Stopped", 0f)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Session stopped", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onResult(false, "Stop Failed", 0f)
            }
        }
    }

    private fun addSyncMarker() {
        Toast.makeText(this, "Sync marker added", Toast.LENGTH_SHORT).show()
    }

    private fun startDeviceDiscovery(onResult: (List<DeviceInfo>) -> Unit) {
        lifecycleScope.launch {
            try {
                delay(2000) // Simulate discovery
                val mockDevices = listOf(
                    DeviceInfo("Shimmer3 GSR #1234", "GSR Sensor"),
                    DeviceInfo("TC001 Camera", "Thermal Camera")
                )
                onResult(mockDevices)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Found ${mockDevices.size} devices", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onResult(emptyList())
                Toast.makeText(this@UnifiedSensorComposeActivity, "Device discovery failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPCDiscovery(onResult: (List<PCControllerInfo>) -> Unit) {
        lifecycleScope.launch {
            try {
                delay(1500) // Simulate discovery
                val mockControllers = listOf(
                    PCControllerInfo("Research PC", "192.168.1.100"),
                    PCControllerInfo("Lab Controller", "192.168.1.101")
                )
                onResult(mockControllers)
                Toast.makeText(this@UnifiedSensorComposeActivity, "Found ${mockControllers.size} PC controllers", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onResult(emptyList())
                Toast.makeText(this@UnifiedSensorComposeActivity, "PC discovery failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToDevice(device: DeviceInfo) {
        Toast.makeText(this, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
    }

    private fun connectToPC(controller: PCControllerInfo) {
        Toast.makeText(this, "Connecting to ${controller.name}...", Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionError() {
        Toast.makeText(this, "Permissions required for unified sensor platform", Toast.LENGTH_LONG).show()
    }

    private data class DeviceInfo(
        val name: String,
        val type: String
    )

    private data class PCControllerInfo(
        val name: String,
        val ipAddress: String
    )
}