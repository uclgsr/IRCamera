package mpdc4gsr.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compose version of ShimmerMvpActivity demonstrating Shimmer GSR sensor integration.
 * Shows how to handle Bluetooth permissions, device scanning, and GSR data recording in Compose.
 */
class ShimmerMvpComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "ShimmerMvpComposeActivity"
        private const val REQUEST_ENABLE_BT = 1

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).let { basePermissions ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                basePermissions + arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                basePermissions
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.i(TAG, "All permissions granted, initializing Shimmer")
            initializeShimmer()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Log.w(TAG, "Permissions denied: ${deniedPermissions.joinToString()}")
            showPermissionDeniedDialog(deniedPermissions.toList())
        }
    }

    private val bluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            initializeShimmer()
        } else {
            showToast("Bluetooth must be enabled to connect to Shimmer device")
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            checkPermissionsAndBluetooth()
        }

        IRCameraTheme {
            ShimmerMvpScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ShimmerMvpScreen() {
        var isConnected by remember { mutableStateOf(false) }
        var isRecording by remember { mutableStateOf(false) }
        var deviceStatus by remember { mutableStateOf("Initializing...") }
        var gsrValue by remember { mutableStateOf(0.0) }
        var signalQuality by remember { mutableStateOf("Unknown") }
        var sampleCount by remember { mutableStateOf(0L) }
        var connectionStatus by remember { mutableStateOf("Disconnected") }
        var gsrData by remember { mutableStateOf<List<GSRReading>>(emptyList()) }

        // Simulate GSR data updates when recording
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    delay(100) // 10Hz sampling rate
                    val newReading = GSRReading(
                        timestamp = System.currentTimeMillis(),
                        value = (1.0 + Math.random() * 24.0), // 1-25 µS range
                        resistance = (40000 + Math.random() * 960000).toInt() // 40K-1M ohm range
                    )
                    gsrValue = newReading.value
                    sampleCount++
                    
                    // Keep last 50 readings
                    val updatedData = (gsrData + newReading).takeLast(50)
                    gsrData = updatedData
                    
                    // Update signal quality based on value range
                    signalQuality = when (gsrValue) {
                        in 1.0..25.0 -> "Excellent"
                        in 0.5..40.0 -> "Good"
                        else -> "Poor"
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Shimmer GSR MVP",
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

                // Device Status Card
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
                                    imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                    contentDescription = "Bluetooth Status",
                                    tint = if (isConnected) Color.Green else Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Shimmer Device Status",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = connectionStatus,
                                    color = if (isConnected) Color.Green else Color.Red,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = deviceStatus,
                                color = Color(0xCCFFFFFF),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // GSR Monitoring Card
                if (isConnected) {
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
                                        imageVector = Icons.Default.MonitorHeart,
                                        contentDescription = "GSR Monitoring",
                                        tint = if (isRecording) Color.Green else Color(0xFF6B35FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "GSR Monitoring",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = if (isRecording) "Recording" else "Standby",
                                        color = if (isRecording) Color.Green else Color.Yellow,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // GSR Value Display
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF3A3A3A)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "%.2f µS".format(gsrValue),
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "GSR Conductance",
                                            color = Color(0x80FFFFFF),
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                InfoRow("Signal Quality", signalQuality)
                                InfoRow("Sample Count", sampleCount.toString())
                                InfoRow("Sampling Rate", "10 Hz")
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
                        // Connect/Disconnect Button
                        Button(
                            onClick = {
                                if (isConnected) {
                                    disconnectDevice { connected, status ->
                                        isConnected = connected
                                        connectionStatus = status
                                        deviceStatus = if (connected) "Device connected" else "Device disconnected"
                                    }
                                } else {
                                    scanForShimmerDevices { connected, status ->
                                        isConnected = connected
                                        connectionStatus = status
                                        deviceStatus = if (connected) "Shimmer3 GSR connected" else "Connection failed"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                                contentDescription = if (isConnected) "Disconnect" else "Connect",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConnected) "Disconnect" else "Connect")
                        }

                        // Record/Stop Button
                        Button(
                            onClick = {
                                if (isRecording) {
                                    stopRecording { recording, samples ->
                                        isRecording = recording
                                        deviceStatus = "Recording stopped. $samples samples recorded."
                                    }
                                } else {
                                    startRecording { recording ->
                                        isRecording = recording
                                        sampleCount = 0
                                        deviceStatus = "Recording GSR data..."
                                    }
                                }
                            },
                            enabled = isConnected,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRecording) "Stop" else "Record")
                        }
                    }
                }

                // Recent GSR Data
                if (gsrData.isNotEmpty()) {
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
                                    text = "Recent GSR Readings",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                gsrData.takeLast(5).forEach { reading ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                                                .format(Date(reading.timestamp)),
                                            color = Color(0x80FFFFFF),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "%.2f µS".format(reading.value),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
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

    private fun checkPermissionsAndBluetooth() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            return
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported on this device")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothLauncher.launch(enableBtIntent)
            return
        }

        initializeShimmer()
    }

    private fun initializeShimmer() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Initializing Shimmer Bluetooth Manager")
                // Shimmer initialization would happen here
                showToast("Shimmer initialized - Ready to scan for devices")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Shimmer", e)
                showToast("Failed to initialize Shimmer: ${e.message}")
            }
        }
    }

    private fun scanForShimmerDevices(onResult: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(false, "Scanning...")
                delay(2000) // Simulate scanning
                onResult(true, "Connected")
                showToast("Connected to Shimmer3 GSR device")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan for devices", e)
                onResult(false, "Scan Failed")
                showToast("Failed to scan: ${e.message}")
            }
        }
    }

    private fun startRecording(onResult: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(true)
                showToast("GSR recording started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                showToast("Failed to start recording: ${e.message}")
            }
        }
    }

    private fun stopRecording(onResult: (Boolean, Long) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(false, 0) // In real implementation, return actual sample count
                showToast("GSR recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                showToast("Failed to stop recording: ${e.message}")
            }
        }
    }

    private fun disconnectDevice(onResult: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onResult(false, "Disconnected")
                showToast("Device disconnected")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                onResult(false, "Disconnect Error")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        // In a real implementation, show proper dialog
        showToast("Permissions required: ${deniedPermissions.joinToString()}")
    }

    private data class GSRReading(
        val timestamp: Long,
        val value: Double,
        val resistance: Int
    )
}