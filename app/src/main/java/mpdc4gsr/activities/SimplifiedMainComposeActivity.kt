package mpdc4gsr.activities

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
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
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compose version of SimplifiedMainActivity demonstrating simplified main interface.
 * Shows how to handle permissions, recording states, and connection management in Compose.
 */
class SimplifiedMainComposeActivity : BaseComposeActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    override fun Content() {
        IRCameraTheme {
            SimplifiedMainScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SimplifiedMainScreen() {
        var statusMessage by remember { mutableStateOf("Initializing...") }
        var isRecording by remember { mutableStateOf(false) }
        var isConnected by remember { mutableStateOf(false) }
        var permissionStatus by remember { mutableStateOf("Checking permissions...") }
        var recordingTime by remember { mutableStateOf("00:00:00") }

        LaunchedEffect(Unit) {
            checkAndRequestPermissions { status ->
                permissionStatus = status
                statusMessage = "Ready to start recording"
            }
        }

        // Simulate recording timer
        LaunchedEffect(isRecording) {
            if (isRecording) {
                var seconds = 0
                while (isRecording) {
                    delay(1000)
                    seconds++
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val secs = seconds % 60
                    recordingTime = "%02d:%02d:%02d".format(hours, minutes, secs)
                }
            } else {
                recordingTime = "00:00:00"
            }
        }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Simplified Main",
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
                // Status Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "System Status",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = statusMessage,
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = permissionStatus,
                            color = Color(0x80FFFFFF),
                            fontSize = 12.sp
                        )

                        if (isRecording) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recording Time: $recordingTime",
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Connection",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = if (isConnected) "Connected" else "Disconnected",
                                color = if (isConnected) Color.Green else Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow("Camera", if (isConnected) "Ready" else "Not connected")
                        InfoRow("Sensors", if (isConnected) "Active" else "Inactive")
                        InfoRow("Storage", "Available")
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Record Button
                    Button(
                        onClick = {
                            toggleRecording { recording, status ->
                                isRecording = recording
                                statusMessage = status
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRecording) "Stop Recording" else "Start Recording")
                    }

                    // Connect Button
                    Button(
                        onClick = {
                            toggleConnection { connected, status ->
                                isConnected = connected
                                statusMessage = status
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isConnected) "Disconnect" else "Connect")
                    }
                }

                // Quick Stats Card
                if (isRecording || isConnected) {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Session Information",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (isRecording) {
                                InfoRow("Status", "Recording Active")
                                InfoRow("Duration", recordingTime)
                                InfoRow("File Size", "2.3 MB")
                            }
                            
                            if (isConnected) {
                                InfoRow("Device", "TC001 Thermal Camera")
                                InfoRow("Signal", "Strong")
                                InfoRow("Battery", "85%")
                            }
                        }
                    }
                }

                // Permissions Status Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Required Permissions",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        PermissionItem("Camera", checkPermission(Manifest.permission.CAMERA))
                        PermissionItem("Audio Recording", checkPermission(Manifest.permission.RECORD_AUDIO))
                        PermissionItem("Storage", checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        PermissionItem("Location", checkPermission(Manifest.permission.ACCESS_FINE_LOCATION))
                        PermissionItem("Bluetooth", checkPermission(Manifest.permission.BLUETOOTH))
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

    @Composable
    private fun PermissionItem(name: String, granted: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = if (granted) "✓" else "✗",
                color = if (granted) Color.Green else Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions(onResult: (String) -> Unit) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PermissionChecker.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            onResult("Requesting permissions...")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            onResult("All permissions granted ✓")
        }
    }

    private fun toggleRecording(onStateChange: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                recordingState = !recordingState
                if (recordingState) {
                    onStateChange(true, "Recording started - ${getCurrentTimestamp()}")
                    Toast.makeText(this@SimplifiedMainComposeActivity, "Recording started", Toast.LENGTH_SHORT).show()
                } else {
                    onStateChange(false, "Recording stopped - ${getCurrentTimestamp()}")
                    Toast.makeText(this@SimplifiedMainComposeActivity, "Recording stopped", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                onStateChange(false, "Error: ${e.message}")
                Toast.makeText(this@SimplifiedMainComposeActivity, "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleConnection(onStateChange: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                val currentState = !getCurrentConnectionState()
                if (currentState) {
                    delay(1000) // Simulate connection time
                    onStateChange(true, "Device connected - ${getCurrentTimestamp()}")
                    Toast.makeText(this@SimplifiedMainComposeActivity, "Device connected", Toast.LENGTH_SHORT).show()
                } else {
                    onStateChange(false, "Device disconnected - ${getCurrentTimestamp()}")
                    Toast.makeText(this@SimplifiedMainComposeActivity, "Device disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                onStateChange(false, "Connection error: ${e.message}")
                Toast.makeText(this@SimplifiedMainComposeActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Simple state tracking (in real app, this would be more sophisticated)
    private var recordingState = false
    private var connectionState = false

    private fun getCurrentRecordingState(): Boolean = recordingState.also { recordingState = !recordingState }
    private fun getCurrentConnectionState(): Boolean = connectionState.also { connectionState = !connectionState }

    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
}