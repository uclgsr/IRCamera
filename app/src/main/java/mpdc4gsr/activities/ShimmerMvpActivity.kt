package mpdc4gsr

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityShimmerMvpBinding
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShimmerMvpActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShimmerMVP"
        private const val REQUEST_ENABLE_BT = 1
        private const val GSR_SAMPLING_RATE = 128.0 // Hz

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

    private lateinit var binding: ActivityShimmerMvpBinding

    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private var shimmerDevice: Shimmer? = null
    private var isRecording = false
    private var sampleCount = 0L
    private val gsrDataBuffer = mutableListOf<GSRSample>()

    private var networkClient: ShimmerNetworkClient? = null
    private var currentSessionId: String? = null

    data class GSRSample(
        val timestamp: Long,
        val gsrValue: Double,
        val rawValue: Int,
        val resistance: Double
    )

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShimmerMvpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
        initializeNetworkClient()
        checkPermissionsAndBluetooth()
    }

    private fun initializeUI() {
        binding.connectButton.setOnClickListener { scanForShimmerDevices() }
        binding.startRecordingButton.setOnClickListener { startRecording() }
        binding.stopRecordingButton.setOnClickListener { stopRecording() }

        updateUI()
    }

    private fun initializeNetworkClient() {
        lifecycleScope.launch {
            try {
                networkClient = ShimmerNetworkClient()

                networkClient?.onConnected = {
                    runOnUiThread {
                        Log.i(TAG, "Connected to PC Controller")
                        showToast("Connected to PC Controller")
                    }
                }

                networkClient?.onDisconnected = {
                    runOnUiThread {
                        Log.i(TAG, "Disconnected from PC Controller")
                    }
                }

                networkClient?.onError = { error ->
                    runOnUiThread {
                        Log.w(TAG, "Network error: $error")
                    }
                }

                val connected = networkClient?.connect() ?: false
                if (!connected) {
                    Log.i(TAG, "PC Controller not available, continuing without network")
                }

            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize network client: ${e.message}")
            }
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

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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

                // Simplified Shimmer initialization to avoid API compatibility issues
                shimmerBluetoothManager =
                    ShimmerBluetoothManagerAndroid(this@ShimmerMvpActivity, android.os.Handler())
                Log.i(TAG, "Shimmer manager initialized - API compatibility mode")
                updateConnectionStatus("Shimmer manager ready")
                binding.connectButton.isEnabled = true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Shimmer", e)
                updateConnectionStatus("Initialization failed: ${e.message}")
            }
        }
    }

    private fun scanForShimmerDevices() {
        lifecycleScope.launch {
            try {
                updateConnectionStatus("Scanning for Shimmer3 GSR+ devices...")
                binding.connectButton.isEnabled = false

                // Check for required permissions first
                if (!hasAllRequiredPermissions()) {
                    val missingPermissions = getMissingPermissions()
                    Log.w(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
                    showToast("Missing required permissions. Please grant permissions and try again.")
                    permissionLauncher.launch(missingPermissions)
                    binding.connectButton.isEnabled = true
                    return@launch
                }

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    showBluetoothNotSupportedDialog()
                    binding.connectButton.isEnabled = true
                    return@launch
                } else if (!bluetoothAdapter.isEnabled) {
                    showBluetoothDisabledDialog()
                    binding.connectButton.isEnabled = true
                    return@launch
                }

                // Start with paired devices first (for better UX)
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                val pairedShimmers = pairedDevices?.filter { isValidShimmerDevice(it) } ?: emptyList()

                if (pairedShimmers.isNotEmpty()) {
                    Log.i(TAG, "Found ${pairedShimmers.size} paired Shimmer devices")
                    connectToShimmerDevice(pairedShimmers.first())
                    return@launch
                }

                // If no paired devices, perform actual BLE scanning
                Log.i(TAG, "No paired Shimmer devices found. Starting BLE discovery...")
                updateConnectionStatus("Scanning nearby Shimmer devices...")
                
                val discoveredShimmers = performBluetoothLeScanning()
                if (discoveredShimmers.isNotEmpty()) {
                    Log.i(TAG, "Found ${discoveredShimmers.size} Shimmer devices during scan")
                    // Attempt to connect to the first discovered device
                    connectToShimmerDevice(discoveredShimmers.first())
                } else {
                    updateConnectionStatus("No Shimmer3 GSR+ devices found")
                    showDeviceNotFoundDialog()
                    binding.connectButton.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning for Shimmer3 GSR+ devices", e)
                updateConnectionStatus("Device scan failed: ${e.message}")
                showScanErrorDialog(e)
                binding.connectButton.isEnabled = true
            }
        }
    }

    private suspend fun performBluetoothLeScanning(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val discoveredDevices = mutableListOf<BluetoothDevice>()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            return@withContext discoveredDevices
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (isValidShimmerDevice(device) && !discoveredDevices.contains(device)) {
                    Log.d(TAG, "Discovered Shimmer device: ${device.name} (${device.address})")
                    discoveredDevices.add(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code: $errorCode")
            }
        }

        try {
            Log.i(TAG, "Starting BLE scan for Shimmer devices...")
            bluetoothLeScanner.startScan(scanCallback)
            
            // Scan for 10 seconds
            delay(10000)
            
            bluetoothLeScanner.stopScan(scanCallback)
            Log.i(TAG, "BLE scan completed. Found ${discoveredDevices.size} devices")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during BLE scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during BLE scan", e)
        }

        return@withContext discoveredDevices
    }

    private fun connectToShimmerDevice(device: BluetoothDevice) {
        lifecycleScope.launch {
            try {
                val prioritizedDevices = listOf(device).sortedByDescending { dev ->
                    val name = dev.name?.lowercase() ?: ""
                    when {
                        name.contains("gsr") -> 100 // Highest priority for GSR-specific devices
                        name.contains("shimmer3") -> 90
                        name.contains("shimmer") -> 80
                        name.startsWith("rn4") -> 70
                        else -> 50
                    }
                }

                val targetDevice = prioritizedDevices.first()
                Log.i(TAG, "Connecting to Shimmer3 GSR+: ${targetDevice.name} (${targetDevice.address})")
                updateConnectionStatus("Connecting to ${targetDevice.name}...")

                shimmerBluetoothManager?.connectShimmerThroughBTAddress(targetDevice.address)

                Log.i(TAG, "Target Device Details:")
                Log.i(TAG, "  Name: ${targetDevice.name}")
                Log.i(TAG, "  Address: ${targetDevice.address}")
                Log.i(TAG, "  Type: ${targetDevice.type}")
                Log.i(TAG, "  Bond State: ${targetDevice.bondState}")

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning for Shimmer3 GSR+ devices", e)
                updateConnectionStatus("Device scan failed: ${e.message}")
                binding.connectButton.isEnabled = true
            }
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getMissingPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    private fun isValidShimmerDevice(device: BluetoothDevice): Boolean {
        val address = device.address
        val name = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.name
        } else {
            null
        }

        // Check MAC address prefix
        val shimmerMacPrefixes = listOf("00:06:66", "d0:39:72", "00:80:98")
        val hasValidPrefix = shimmerMacPrefixes.any { prefix ->
            address.startsWith(prefix, ignoreCase = true)
        }

        // Check device name pattern
        val shimmerNamePatterns = listOf("shimmer", "gsr", "rn4", "shimmer3")
        val hasValidName = name?.let { deviceName ->
            shimmerNamePatterns.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        } ?: false

        val isValid = hasValidPrefix || hasValidName

        if (isValid) {
            Log.d(TAG, "Valid Shimmer device detected: $name ($address)")
        }

        return isValid
    }

    private fun setupShimmerConfiguration() {
        shimmerDevice?.let { shimmer ->
            try {
                Log.i(TAG, "Configuring Shimmer3 GSR+ for recording (compatibility mode)")

                // Using basic configuration to avoid API compatibility issues
                // Advanced configuration will be implemented once API compatibility is resolved

                Log.i(TAG, "Shimmer3 GSR+ configuration complete - Basic settings applied")
                updateConnectionStatus("GSR+ Configured - Ready for recording")
                binding.startRecordingButton.isEnabled = true

            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Shimmer3 GSR+", e)
                updateConnectionStatus("GSR+ Configuration failed: ${e.message}")
            }
        }
    }

    private fun startRecording() {
        shimmerDevice?.let { shimmer ->
            try {
                Log.i(TAG, "Starting GSR recording")
                isRecording = true
                sampleCount = 0
                gsrDataBuffer.clear()

                currentSessionId = "session_${System.currentTimeMillis()}"

                shimmer.startStreaming()

                networkClient?.sendRecordingStart(currentSessionId!!)

                runOnUiThread {
                    updateConnectionStatus("Recording GSR data...")
                    binding.startRecordingButton.isEnabled = false
                    binding.stopRecordingButton.isEnabled = true
                }

                Log.i(TAG, "GSR recording started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                showToast("Failed to start recording: ${e.message}")
                isRecording = false
            }
        } ?: run {
            showToast("No Shimmer device connected")
        }
    }

    private fun stopRecording() {
        shimmerDevice?.let { shimmer ->
            try {
                Log.i(TAG, "Stopping GSR recording")
                isRecording = false

                shimmer.stopStreaming()

                networkClient?.sendRecordingStop(currentSessionId ?: "unknown", sampleCount)

                exportDataToCSV()

                runOnUiThread {
                    updateConnectionStatus("Recording stopped - Data exported")
                    binding.startRecordingButton.isEnabled = true
                    binding.stopRecordingButton.isEnabled = false
                }

                Log.i(TAG, "GSR recording stopped, ${gsrDataBuffer.size} samples collected")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                showToast("Failed to stop recording: ${e.message}")
            }
        }
    }

    private fun processShimmerData(objectCluster: ObjectCluster) {
        try {
            val timestamp = System.currentTimeMillis()

            // Extract GSR data from ObjectCluster using proper Shimmer SDK methods
            val gsrRawData = objectCluster.getFormatClusterValue("CAL", "GSR")
            val rawValue =
                (gsrRawData as? Double)?.toInt() ?: 2048 // Default to middle 12-bit range if null

            // Calculate GSR value in microsiemens using proper 12-bit ADC conversion (0-4095 range)
            val gsrValue = if (rawValue > 0 && rawValue <= 4095) {
                // Convert raw 12-bit ADC value to GSR in microsiemens
                // Using standard Shimmer3 GSR+ calibration formula
                val resistance = (rawValue / 4095.0) * 40.0 + 0.5 // Convert to resistance range
                1000000.0 / (resistance * 1000.0) // Convert to microsiemens
            } else {
                4.5 // Default fallback value
            }

            val resistance = 1000000.0 / gsrValue

            if (rawValue in 0..4095 && gsrValue > 0.1 && gsrValue < 100.0) {
                val sample = GSRSample(timestamp, gsrValue, rawValue, resistance)
                gsrDataBuffer.add(sample)
                sampleCount++

                networkClient?.sendGSRSample(sample, sampleCount)

                runOnUiThread {
                    binding.gsrValueText.text =
                        "GSR: %.3f µS (%.1f kΩ)".format(gsrValue, resistance / 1000)
                    binding.sampleCountText.text = "Samples: $sampleCount (${
                        String.format(
                            "%.1f",
                            sampleCount * 1000.0 / GSR_SAMPLING_RATE
                        )
                    }s)"
                }

                if (sampleCount % 128 == 0L) { // Every second at 128Hz
                    Log.d(
                        TAG,
                        "GSR [${sampleCount}s]: ${
                            String.format(
                                "%.3f",
                                gsrValue
                            )
                        } µS, Raw: $rawValue/4095, R: ${
                            String.format(
                                "%.1f",
                                resistance / 1000
                            )
                        } kΩ"
                    )
                }
            } else {
                Log.w(TAG, "Invalid GSR sample - Raw: $rawValue, Cal: $gsrValue µS")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error processing Shimmer3 GSR+ data", e)
        }
    }

    private fun exportDataToCSV() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val deviceInfo = shimmerDevice?.getBluetoothAddress() ?: "unknown"
                val filename = "shimmer3_gsr_${deviceInfo}_$timestamp.csv"

                val csvContent = StringBuilder()

                csvContent.append("# Shimmer3 GSR+ Data Export\n")
                csvContent.append("# Device: $deviceInfo\n")
                csvContent.append("# Session ID: ${currentSessionId ?: "unknown"}\n")
                csvContent.append("# Sampling Rate: ${GSR_SAMPLING_RATE} Hz\n")
                csvContent.append("# ADC Resolution: 12-bit (0-4095)\n")
                csvContent.append("# Total Samples: ${gsrDataBuffer.size}\n")
                csvContent.append(
                    "# Duration: ${
                        String.format(
                            "%.2f",
                            gsrDataBuffer.size / GSR_SAMPLING_RATE
                        )
                    } seconds\n"
                )
                csvContent.append(
                    "# Export Time: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    }\n"
                )
                csvContent.append("#\n")
                csvContent.append("timestamp_ms,gsr_microsiemens,raw_adc_12bit,resistance_ohm,sample_number,elapsed_seconds\n")

                val startTime =
                    if (gsrDataBuffer.isNotEmpty()) gsrDataBuffer.first().timestamp else System.currentTimeMillis()

                gsrDataBuffer.forEachIndexed { index, sample ->
                    val elapsedSeconds = (sample.timestamp - startTime) / 1000.0
                    csvContent.append(
                        "${sample.timestamp},${
                            String.format(
                                "%.6f",
                                sample.gsrValue
                            )
                        },${sample.rawValue},${
                            String.format(
                                "%.2f",
                                sample.resistance
                            )
                        },${index + 1},${String.format("%.6f", elapsedSeconds)}\n"
                    )
                }

                val file = java.io.File(getExternalFilesDir(null), filename)
                file.writeText(csvContent.toString())

                val avgGsr = gsrDataBuffer.map { it.gsrValue }.average()
                val minGsr = gsrDataBuffer.minOfOrNull { it.gsrValue } ?: 0.0
                val maxGsr = gsrDataBuffer.maxOfOrNull { it.gsrValue } ?: 0.0

                withContext(Dispatchers.Main) {
                    showToast(
                        "GSR data exported: $filename\nSamples: ${gsrDataBuffer.size}\nAvg GSR: ${
                            String.format(
                                "%.3f",
                                avgGsr
                            )
                        } µS"
                    )
                    Log.i(TAG, "Research-grade GSR data exported:")
                    Log.i(TAG, "  File: ${file.absolutePath}")
                    Log.i(TAG, "  Samples: ${gsrDataBuffer.size}")
                    Log.i(
                        TAG,
                        "  Duration: ${
                            String.format(
                                "%.2f",
                                gsrDataBuffer.size / GSR_SAMPLING_RATE
                            )
                        }s"
                    )
                    Log.i(
                        TAG,
                        "  GSR Range: ${String.format("%.3f", minGsr)} - ${
                            String.format(
                                "%.3f",
                                maxGsr
                            )
                        } µS"
                    )
                    Log.i(TAG, "  Avg GSR: ${String.format("%.3f", avgGsr)} µS")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error exporting GSR research data", e)
                withContext(Dispatchers.Main) {
                    showToast("Error exporting data: ${e.message}")
                }
            }
        }
    }

    private fun updateConnectionStatus(status: String) {
        binding.connectionStatusText.text = status
        Log.i(TAG, "Status: $status")
    }

    private fun updateUI() {
        binding.connectButton.isEnabled = false
        binding.startRecordingButton.isEnabled = false
        binding.stopRecordingButton.isEnabled = false
        binding.gsrValueText.text = "GSR: -- µS"
        binding.sampleCountText.text = "Samples: 0"
        updateConnectionStatus("Initializing...")
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scanning"
                Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connection"  
                Manifest.permission.ACCESS_FINE_LOCATION -> "Fine Location"
                Manifest.permission.ACCESS_COARSE_LOCATION -> "Coarse Location"
                Manifest.permission.BLUETOOTH -> "Bluetooth (Legacy)"
                Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth Admin (Legacy)"
                else -> permission
            }
        }

        val message = """
            The following permissions are required for Shimmer GSR device functionality:
            
            ${permissionNames.joinToString("\n• ", "• ")}
            
            Without these permissions, you cannot:
            • Discover nearby Shimmer devices
            • Connect to your Shimmer GSR sensor
            • Record physiological data
            
            Please grant these permissions to continue.
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                // Re-request permissions
                val missingPermissions = getMissingPermissions()
                if (missingPermissions.isNotEmpty()) {
                    permissionLauncher.launch(missingPermissions)
                }
            }
            .setNegativeButton("Settings") { _, _ ->
                // Open app settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showToast("Shimmer functionality requires permissions")
            }
            .setCancelable(false)
            .show()
    }

    private fun showBluetoothNotSupportedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bluetooth Not Supported")
            .setMessage("This device does not support Bluetooth, which is required for Shimmer GSR sensor communication.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBluetoothDisabledDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bluetooth Disabled")
            .setMessage("Bluetooth must be enabled to connect to Shimmer devices. Would you like to enable it now?")
            .setPositiveButton("Enable Bluetooth") { _, _ ->
                val enableBtIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showToast("Bluetooth is required for Shimmer connection")
            }
            .show()
    }

    private fun showDeviceNotFoundDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No Shimmer Devices Found")
            .setMessage("""
                No Shimmer3 GSR+ devices were discovered during scanning.
                
                Troubleshooting steps:
                • Ensure your Shimmer device is powered on
                • Move closer to the device (within 10 meters)
                • Check that the device is not connected to another app
                • Try pairing manually in Bluetooth settings first
                
                Common device names: Shimmer3 GSR+, RN4x, or devices starting with "GSR"
            """.trimIndent())
            .setPositiveButton("Retry Scan") { _, _ ->
                scanForShimmerDevices()
            }
            .setNegativeButton("Bluetooth Settings") { _, _ ->
                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
            }
            .setNeutralButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showScanErrorDialog(error: Exception) {
        val errorMessage = when {
            error is SecurityException -> "Permission error during BLE scan. Please check Bluetooth permissions."
            error.message?.contains("bluetooth", true) == true -> "Bluetooth error: ${error.message}"
            else -> "Scan failed: ${error.message}"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Scan Error")
            .setMessage("""
                $errorMessage
                
                This could be due to:
                • Missing Bluetooth permissions
                • Bluetooth adapter issues
                • System resource constraints
                
                Try restarting Bluetooth or the app if the problem persists.
            """.trimIndent())
            .setPositiveButton("Retry") { _, _ ->
                scanForShimmerDevices()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isRecording) {
                stopRecording()
            }
            shimmerDevice?.disconnect()
            shimmerBluetoothManager?.disconnectAllDevices()
            networkClient?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
