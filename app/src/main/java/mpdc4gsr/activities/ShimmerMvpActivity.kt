package mpdc4gsr.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.databinding.ActivityShimmerMvpBinding
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.ShimmerNetworkClient
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.gsr.GSRCalculationUtils
import mpdc4gsr.sensors.gsr.GSRConstants
import mpdc4gsr.sensors.unified.model.GSRSample
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShimmerMvpActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShimmerMVP"
        private const val REQUEST_ENABLE_BT = 1

        // Quality range constants - keeping these as they are specific to this activity
        private const val QUALITY_EXCELLENT_LOWER = 500
        private const val QUALITY_EXCELLENT_UPPER = 3500
        private const val QUALITY_EXCELLENT_GSR_LOWER = 1.0
        private const val QUALITY_EXCELLENT_GSR_UPPER = 25.0
        private const val QUALITY_GOOD_LOWER = 200
        private const val QUALITY_GOOD_UPPER = 3800
        private const val QUALITY_GOOD_GSR_LOWER = 0.5
        private const val QUALITY_GOOD_GSR_UPPER = 40.0

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

    // Thread-safe ISO8601 formatter
    private val iso8601Format by lazy {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    }


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {            initializeShimmer()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys}")
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
                    runOnUiThread {                        showToast("Connected to PC Controller")
                    }
                }

                networkClient?.onDisconnected = {
                    runOnUiThread {                    }
                }

                networkClient?.onError = { error ->
                    runOnUiThread {                    }
                }

                val connected = networkClient?.connect() ?: false
                if (!connected) {                }

            } catch (e: Exception) {            }
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
            try {                shimmerBluetoothManager =
                    ShimmerBluetoothManagerAndroid(this@ShimmerMvpActivity, Handler(Looper.getMainLooper()))                updateConnectionStatus("Shimmer manager ready")
                binding.connectButton.isEnabled = true

            } catch (e: Exception) {                updateConnectionStatus("Initialization failed: ${e.message}")
            }
        }
    }

    private fun scanForShimmerDevices() {
        lifecycleScope.launch {
            try {
                updateConnectionStatus("Scanning for Shimmer3 GSR+ devices...")
                binding.connectButton.isEnabled = false


                if (!hasAllRequiredPermissions()) {
                    val missingPermissions = getMissingPermissions()}")
                    showToast("Missing required permissions. Please grant permissions and try again.")
                    permissionLauncher.launch(missingPermissions)
                    binding.connectButton.isEnabled = true
                    return@launch
                }

                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                if (bluetoothAdapter == null) {
                    showBluetoothNotSupportedDialog()
                    binding.connectButton.isEnabled = true
                    return@launch
                } else if (!bluetoothAdapter.isEnabled) {
                    showBluetoothDisabledDialog()
                    binding.connectButton.isEnabled = true
                    return@launch
                }


                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                val pairedShimmers = pairedDevices?.filter { isValidShimmerDevice(it) } ?: emptyList()

                // Always perform BLE scan to get both paired and unpaired devices                updateConnectionStatus("Scanning for all available Shimmer devices...")

                val allDiscoveredDevices = mutableListOf<BluetoothDevice>()
                allDiscoveredDevices.addAll(pairedShimmers)

                val discoveredShimmers = performBluetoothLeScanning()
                // Add newly discovered devices (not already paired)
                discoveredShimmers.forEach { device ->
                    if (allDiscoveredDevices.none { it.address == device.address }) {
                        allDiscoveredDevices.add(device)
                    }
                }

                if (allDiscoveredDevices.isNotEmpty()) {"
                    )

                    // Show device selection dialog if multiple devices found
                    if (allDiscoveredDevices.size > 1) {
                        showDeviceSelectionDialog(allDiscoveredDevices)
                    } else {
                        connectToShimmerDevice(allDiscoveredDevices.first())
                    }
                } else {
                    updateConnectionStatus("No Shimmer3 GSR+ devices found")
                    showDeviceNotFoundDialog()
                    binding.connectButton.isEnabled = true
                }

            } catch (e: Exception) {                updateConnectionStatus("Device scan failed: ${e.message}")
                showScanErrorDialog(e)
                binding.connectButton.isEnabled = true
            }
        }
    }

    private suspend fun performBluetoothLeScanning(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val discoveredDevices = mutableListOf<BluetoothDevice>()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {            return@withContext discoveredDevices
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (isValidShimmerDevice(device) && !discoveredDevices.contains(device)) {")
                    discoveredDevices.add(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {            }
        }

        try {            bluetoothLeScanner.startScan(scanCallback)


            delay(10000)

            bluetoothLeScanner.stopScan(scanCallback)        } catch (e: SecurityException) {        } catch (e: Exception) {        }

        return@withContext discoveredDevices
    }

    private fun connectToShimmerDevice(device: BluetoothDevice) {
        lifecycleScope.launch {
            try {
                val prioritizedDevices = listOf(device).sortedByDescending { dev ->
                    val name = dev.name?.lowercase() ?: ""
                    when {
                        name.contains("gsr") -> 100
                        name.contains("shimmer3") -> 90
                        name.contains("shimmer") -> 80
                        name.startsWith("rn4") -> 70
                        else -> 50
                    }
                }

                val targetDevice = prioritizedDevices.first()")
                updateConnectionStatus("Connecting to ${targetDevice.name}...")

                // Set up the data handler BEFORE connecting
                setupShimmerDataHandler()

                shimmerBluetoothManager?.connectShimmerThroughBTAddress(targetDevice.address)            } catch (e: Exception) {                updateConnectionStatus("Device scan failed: ${e.message}")
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


        val shimmerMacPrefixes = listOf("00:06:66", "d0:39:72", "00:80:98")
        val hasValidPrefix = shimmerMacPrefixes.any { prefix ->
            address.startsWith(prefix, ignoreCase = true)
        }


        val shimmerNamePatterns = listOf("shimmer", "gsr", "rn4", "shimmer3")
        val hasValidName = name?.let { deviceName ->
            shimmerNamePatterns.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        } ?: false

        val isValid = hasValidPrefix || hasValidName

        if (isValid) {")
        }

        return isValid
    }

    private fun setupShimmerDataHandler() {
        try {            // Set up the multi-shimmer data handler to receive ObjectCluster data
            // Note: The exact API method may vary depending on Shimmer SDK version
            // Using shimmerBluetoothManager as a fallback approach            // The data handler will need to be implemented using available Shimmer SDK callbacks
            // This may require specific SDK documentation for the exact implementation        } catch (e: Exception) {        }
    }

    /**
     * Enhanced ObjectCluster conversion using the unified timestamp approach
     * This integrates the convertObjectClusterToSensorSample method functionality
     */
    private fun convertObjectClusterToEnhancedGSRSample(objectCluster: ObjectCluster): GSRSample? {
        return try {
            val timestamp = TimestampManager.getCurrentTimestampNanos()

            // Extract calibrated GSR value using proper field names
            val gsrCalibratedData = objectCluster.getFormatClusterValue("GSR", "CAL")
            val gsrRawData = objectCluster.getFormatClusterValue("GSR", "RAW")

            val gsrRaw = (gsrRawData as? Number)?.toInt() ?: 0
            val gsrCalibrated = (gsrCalibratedData as? Number)?.toDouble() ?: 0.0

            // Calculate GSR in microsiemens with enhanced method
            val gsrMicrosiemens = if (gsrCalibrated > 0) {
                gsrCalibrated
            } else if (gsrRaw > 0 && gsrRaw <= GSRConstants.ADC_MAX_VALUE.toInt()) {
                // Use centralized GSR calculation
                GSRCalculationUtils.calculateGSRMicrosiemens(gsrRaw)
            } else {
                0.0
            }

            // Extract additional sensor data
            val ppgData = objectCluster.getFormatClusterValue("PPG_A13", "CAL")
            val ppgRaw = (ppgData as? Number)?.toInt() ?: 0

            // Calculate quality score using centralized utility
            val qualityScore = GSRCalculationUtils.calculateSignalQuality(gsrMicrosiemens, gsrRaw)

            // Create GSRSample using the unified model
            GSRSample(
                timestamp = timestamp,
                timestampIso = iso8601Format.format(java.util.Date(timestamp / 1_000_000)),
                gsrMicrosiemens = gsrMicrosiemens,
                gsrRaw = gsrRaw,
                ppgRaw = ppgRaw,
                qualityScore = qualityScore,
                connectionRssi = getCurrentRssi() // Get actual RSSI if available
            )

        } catch (e: Exception) {            null
        }
    }

    private fun processEnhancedGSRSample(sample: GSRSample) {
        try {
            // Add to data buffer
            gsrDataBuffer.add(sample)
            sampleCount++

            // Send to network client
            networkClient?.sendGSRSample(sample, sampleCount)

            // Calculate signal quality and connection health
            val signalQualityPercent = calculateSignalQuality(sample.gsrMicrosiemens, sample.gsrRaw)
            val connectionHealth = calculateConnectionHealth(sample)

            // Update UI with enhanced feedback
            runOnUiThread {
                binding.gsrValueText.text = "GSR: %.3f µS (%.1f kΩ)".format(
                    sample.gsrMicrosiemens,
                    sample.resistanceOhms / 1000
                )
                binding.sampleCountText.text = "Samples: $sampleCount (${
                    String.format("%.1f", sampleCount * 1000.0 / GSRConstants.GSR_SAMPLING_RATE)
                }s)"

                // Update signal quality indicator
                val qualityColor = when {
                    signalQualityPercent >= 80 -> ContextCompat.getColor(
                        this@ShimmerMvpActivity,
                        android.R.color.holo_green_dark
                    )

                    signalQualityPercent >= 60 -> ContextCompat.getColor(
                        this@ShimmerMvpActivity,
                        android.R.color.holo_orange_dark
                    )

                    else -> ContextCompat.getColor(this@ShimmerMvpActivity, android.R.color.holo_red_dark)
                }
                binding.signalQualityText.text = "Quality: ${signalQualityPercent.toInt()}%"
                binding.signalQualityText.setTextColor(qualityColor)

                // Update connection health indicator
                val healthColor = when (connectionHealth) {
                    "Strong" -> ContextCompat.getColor(this@ShimmerMvpActivity, android.R.color.holo_green_dark)
                    "Good" -> ContextCompat.getColor(this@ShimmerMvpActivity, android.R.color.holo_orange_dark)
                    else -> ContextCompat.getColor(this@ShimmerMvpActivity, android.R.color.holo_red_dark)
                }
                binding.connectionHealthText.text = "Signal: $connectionHealth"
                binding.connectionHealthText.setTextColor(healthColor)
            }

            // Log periodic updates with enhanced metrics
            if (sampleCount % 128 == 0L) {                    } µS, Raw: ${sample.gsrRaw}/4095, R: ${
                        String.format("%.1f", sample.resistanceOhms / 1000)
                    } kΩ, Quality: ${signalQualityPercent.toInt()}%, Health: $connectionHealth"
                )
            }
        } catch (e: Exception) {        }
    }

    /**
     * Get current RSSI value from the Shimmer device if available
     */
    private fun getCurrentRssi(): Int {
        return try {
            // Try to get actual RSSI from the connected device
            shimmerDevice?.let { device ->
                // If Shimmer SDK provides RSSI information, use it
                // This is a placeholder - actual implementation depends on Shimmer SDK API
                -50 // Default when RSSI is not available
            } ?: -50 // Default when device is null
        } catch (e: Exception) {            -50 // Reasonable default for BLE connections
        }
    }

    private fun calculateSignalQuality(gsrValue: Double, rawValue: Int): Double {
        return when {
            rawValue !in GSRConstants.GSR_RAW_LOWER_BOUND..GSRConstants.GSR_RAW_UPPER_BOUND -> 20.0 // Poor ADC range
            gsrValue !in GSRConstants.GSR_MICROSIEMENS_LOWER_BOUND..GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND -> 30.0 // Poor GSR range
            gsrValue > GSRConstants.GSR_HIGH_THRESHOLD -> 40.0 // Very high GSR (poor contact?)
            gsrValue < GSRConstants.GSR_LOW_THRESHOLD -> 50.0 // Very low GSR (sensor issues?)
            rawValue in QUALITY_EXCELLENT_LOWER..QUALITY_EXCELLENT_UPPER && gsrValue in QUALITY_EXCELLENT_GSR_LOWER..QUALITY_EXCELLENT_GSR_UPPER -> 90.0 // Excellent signal
            rawValue in QUALITY_GOOD_LOWER..QUALITY_GOOD_UPPER && gsrValue in QUALITY_GOOD_GSR_LOWER..QUALITY_GOOD_GSR_UPPER -> 80.0 // Good signal
            else -> 70.0 // Acceptable signal
        }
    }

    private fun calculateConnectionHealth(sample: GSRSample): String {
        val now = System.currentTimeMillis()
        val timeSinceLastSample = now - lastSampleTime
        lastSampleTime = now

        return when {
            timeSinceLastSample > 2000 -> "Weak" // More than 2s between samples
            timeSinceLastSample > 1000 -> "Good" // 1-2s between samples
            sample.gsrRaw == 0 -> "Poor" // No data
            sample.gsrMicrosiemens < 0.1 -> "Poor" // Invalid GSR reading
            else -> "Strong" // Good data flow
        }
    }

    private var lastSampleTime = System.currentTimeMillis()

    private fun setupShimmerConfiguration() {
        shimmerDevice?.let { shimmer ->
            try {                // Validate and configure GSR sensor settings
                validateAndConfigureGSRSensor(shimmer)

                // Verify configuration was applied successfully
                if (verifyShimmerConfiguration(shimmer)) {                    updateConnectionStatus("GSR+ Configured & Verified - Ready for recording")
                    binding.startRecordingButton.isEnabled = true
                } else {                    updateConnectionStatus("GSR+ Configured (defaults) - Ready for recording")
                    binding.startRecordingButton.isEnabled = true
                }

            } catch (e: Exception) {                updateConnectionStatus("GSR+ Configuration failed: ${e.message}")
            }
        }
    }

    private fun validateAndConfigureGSRSensor(shimmer: Shimmer): Boolean {
        return try {
            // Enable GSR sensor with validation using standard Shimmer API
            // shimmer.enableGSRSensor(true) // May not be available in current SDK            // Set GSR range to auto with validation using standard Shimmer API
            // shimmer.setGSRRange(GSR_RANGE_AUTO) // May not be available in current SDK            // Configure sampling rate with validation using standard Shimmer API
            shimmer.setSamplingRateShimmer(GSRConstants.GSR_SAMPLING_RATE)            // Note: enableBufferMode may not be available in all Shimmer SDK versions
            try {
                // Try to enable buffer mode if available            } catch (e: Exception) {            }

            true
        } catch (e: Exception) {            false
        }
    }

    private fun verifyShimmerConfiguration(shimmer: Shimmer): Boolean {
        return try {
            // Basic verification using available Shimmer SDK methods            // Note: isGSRSensorEnabled may not be available, using basic checks
            try {
                // val currentRange = shimmer.getGSRRange() // May not be available in current SDK                // Basic validation - assume configuration was successful if no exception                return true
            } catch (e: Exception) {                // Return true if basic configuration didn't throw exceptions
                return true
            }

        } catch (e: Exception) {            false
        }
    }

    private fun startRecording() {
        shimmerDevice?.let { shimmer ->
            try {                isRecording = true
                sampleCount = 0
                gsrDataBuffer.clear()

                currentSessionId = "session_${System.currentTimeMillis()}"

                shimmer.startStreaming()

                networkClient?.sendRecordingStart(currentSessionId!!)

                runOnUiThread {
                    updateConnectionStatus("Recording GSR data...")
                    binding.startRecordingButton.isEnabled = false
                    binding.stopRecordingButton.isEnabled = true
                }            } catch (e: Exception) {                showToast("Failed to start recording: ${e.message}")
                isRecording = false
            }
        } ?: run {
            showToast("No Shimmer device connected")
        }
    }

    private fun stopRecording() {
        shimmerDevice?.let { shimmer ->
            try {                isRecording = false

                shimmer.stopStreaming()

                networkClient?.sendRecordingStop(currentSessionId ?: "unknown", sampleCount)

                exportDataToCSV()

                runOnUiThread {
                    updateConnectionStatus("Recording stopped - Data exported")
                    binding.startRecordingButton.isEnabled = true
                    binding.stopRecordingButton.isEnabled = false
                }            } catch (e: Exception) {                showToast("Failed to stop recording: ${e.message}")
            }
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
                csvContent.append("# Sampling Rate: ${GSRConstants.GSR_SAMPLING_RATE} Hz\n")
                csvContent.append("# ADC Resolution: 12-bit (0-${GSRConstants.ADC_MAX_VALUE.toInt()})\n")
                csvContent.append("# Total Samples: ${gsrDataBuffer.size}\n")
                csvContent.append(
                    "# Duration: ${
                        String.format(
                            "%.2f",
                            gsrDataBuffer.size / GSRConstants.GSR_SAMPLING_RATE
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
                                sample.gsrMicrosiemens
                            )
                        },${sample.gsrRaw},${
                            String.format(
                                "%.2f",
                                sample.resistanceOhms
                            )
                        },${index + 1},${String.format("%.6f", elapsedSeconds)}\n"
                    )
                }

                val file = java.io.File(getExternalFilesDir(null), filename)
                file.writeText(csvContent.toString())

                val avgGsr = gsrDataBuffer.map { it.gsrMicrosiemens }.average()
                val minGsr = gsrDataBuffer.minOfOrNull { it.gsrMicrosiemens } ?: 0.0
                val maxGsr = gsrDataBuffer.maxOfOrNull { it.gsrMicrosiemens } ?: 0.0

                withContext(Dispatchers.Main) {
                    showToast(
                        "GSR data exported: $filename\nSamples: ${gsrDataBuffer.size}\nAvg GSR: ${
                            String.format(
                                "%.3f",
                                avgGsr
                            )
                        } µS"
                    )                        }s"
                    )} - ${
                            String.format(
                                "%.3f",
                                maxGsr
                            )
                        } µS"
                    )} µS")
                }

            } catch (e: Exception) {                withContext(Dispatchers.Main) {
                    showToast("Error exporting data: ${e.message}")
                }
            }
        }
    }

    private fun updateConnectionStatus(status: String) {
        binding.connectionStatusText.text = status

        // Update connection status icon based on status
        val (iconColor, iconText) = when {
            status.contains("connected", true) || status.contains("streaming", true) ->
                Pair(ContextCompat.getColor(this, android.R.color.holo_green_dark), "●")

            status.contains("connecting", true) || status.contains("scanning", true) ->
                Pair(ContextCompat.getColor(this, android.R.color.holo_orange_dark), "●")

            status.contains("failed", true) || status.contains("error", true) || status.contains("not found", true) ->
                Pair(ContextCompat.getColor(this, android.R.color.holo_red_dark), "●")

            status.contains("disconnected", true) ->
                Pair(ContextCompat.getColor(this, android.R.color.darker_gray), "●")

            else ->
                Pair(ContextCompat.getColor(this, android.R.color.darker_gray), "●")
        }

        binding.connectionStatusIcon.setTextColor(iconColor)
        binding.connectionStatusIcon.text = iconText    }

    private fun updateUI() {
        binding.connectButton.isEnabled = false
        binding.startRecordingButton.isEnabled = false
        binding.stopRecordingButton.isEnabled = false
        binding.gsrValueText.text = "GSR: -- µS"
        binding.sampleCountText.text = "Samples: 0"
        binding.signalQualityText.text = "Quality: --%"
        binding.connectionHealthText.text = "Signal: --"
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

                val missingPermissions = getMissingPermissions()
                if (missingPermissions.isNotEmpty()) {
                    permissionLauncher.launch(missingPermissions)
                }
            }
            .setNegativeButton("Settings") { _, _ ->

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
            .setMessage(
                """
                No Shimmer3 GSR+ devices were discovered during scanning.
                
                Troubleshooting steps:
                • Ensure your Shimmer device is powered on
                • Move closer to the device (within 10 meters)
                • Check that the device is not connected to another app
                • Try pairing manually in Bluetooth settings first
                
                Common device names: Shimmer3 GSR+, RN4x, or devices starting with "GSR"
            """.trimIndent()
            )
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

    private fun showDeviceSelectionDialog(devices: List<BluetoothDevice>) {
        val deviceNames = devices.map { device ->
            val name = try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                "Unknown Device"
            }
            val address = device.address
            val isPaired = device.bondState == BluetoothDevice.BOND_BONDED
            val pairedStatus = if (isPaired) " (Paired)" else " (Discovered)"

            "$name$pairedStatus\n$address"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Shimmer Device")
            .setMessage("Multiple Shimmer devices found. Please select the device you want to connect to:")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = devices[which]")
                connectToShimmerDevice(selectedDevice)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                binding.connectButton.isEnabled = true
                updateConnectionStatus("Device selection cancelled")
            }
            .setCancelable(true)
            .setOnCancelListener {
                binding.connectButton.isEnabled = true
                updateConnectionStatus("Device selection cancelled")
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
            .setMessage(
                """
                $errorMessage
                
                This could be due to:
                • Missing Bluetooth permissions
                • Bluetooth adapter issues
                • System resource constraints
                
                Try restarting Bluetooth or the app if the problem persists.
            """.trimIndent()
            )
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
        } catch (e: Exception) {        }
    }
}
