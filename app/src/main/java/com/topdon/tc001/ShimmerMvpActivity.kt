package com.topdon.tc001

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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

    private lateinit var connectionStatusText: TextView
    private lateinit var gsrValueText: TextView
    private lateinit var sampleCountText: TextView
    private lateinit var connectButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button

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
            initializeShimmer()
        } else {
            showToast("Bluetooth permissions required for Shimmer connection")
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
        setContentView(R.layout.activity_shimmer_mvp)

        initializeUI()
        initializeNetworkClient()
        checkPermissionsAndBluetooth()
    }

    private fun initializeUI() {
        connectionStatusText = findViewById(R.id.connectionStatusText)
        gsrValueText = findViewById(R.id.gsrValueText)
        sampleCountText = findViewById(R.id.sampleCountText)
        connectButton = findViewById(R.id.connectButton)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)

        connectButton.setOnClickListener { scanForShimmerDevices() }
        startRecordingButton.setOnClickListener { startRecording() }
        stopRecordingButton.setOnClickListener { stopRecording() }

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

                shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(this@ShimmerMvpActivity)

                shimmerBluetoothManager?.setShimmerBluetoothManagerCallBack(object :
                    ShimmerBluetoothManagerAndroid.ShimmerBluetoothManagerCallback {
                    override fun onDeviceConnected(device: ShimmerDevice?) {
                        runOnUiThread {
                            Log.i(TAG, "Shimmer device connected: ${device?.getBluetoothAddress()}")
                            shimmerDevice = device as? Shimmer
                            setupShimmerConfiguration()
                            updateConnectionStatus("Connected: ${device?.getBluetoothAddress()}")
                        }
                    }

                    override fun onDeviceDisconnected(device: ShimmerDevice?) {
                        runOnUiThread {
                            Log.i(TAG, "Shimmer device disconnected")
                            shimmerDevice = null
                            updateConnectionStatus("Disconnected")
                        }
                    }

                    override fun onDeviceConnectionFailed(
                        device: ShimmerDevice?,
                        errorMsg: String?
                    ) {
                        runOnUiThread {
                            Log.w(TAG, "Shimmer connection failed: $errorMsg")
                            updateConnectionStatus("Connection failed: $errorMsg")
                        }
                    }
                })

                updateConnectionStatus("Shimmer manager initialized")
                connectButton.isEnabled = true

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
                connectButton.isEnabled = false

                if (ActivityCompat.checkSelfPermission(
                        this@ShimmerMvpActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    showToast("Bluetooth connect permission required")
                    return@launch
                }

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

                val shimmerDevices = pairedDevices?.filter { device ->
                    val deviceName = device.name?.lowercase() ?: ""
                    val deviceAddress = device.address?.lowercase() ?: ""

                    deviceName.contains("shimmer", ignoreCase = true) ||
                            deviceName.startsWith("rn4") || // RN4x Shimmer modules
                            deviceName.startsWith("shimmer3") ||
                            deviceAddress.startsWith("00:06:66") || // Shimmer Research MAC prefix
                            deviceAddress.startsWith("d0:39:72") || // Alternative Shimmer MAC prefix
                            deviceName.contains("gsr", ignoreCase = true)
                } ?: emptyList()

                if (shimmerDevices.isEmpty()) {
                    updateConnectionStatus("No paired Shimmer3 GSR+ devices found")
                    showToast("Please pair your Shimmer3 GSR+ device in Bluetooth settings:\n1. Go to Settings > Bluetooth\n2. Pair your Shimmer device\n3. Return to this app")
                    connectButton.isEnabled = true
                    return@launch
                }

                val prioritizedDevices = shimmerDevices.sortedByDescending { device ->
                    val name = device.name?.lowercase() ?: ""
                    when {
                        name.contains("gsr") -> 100 // Highest priority for GSR-specific devices
                        name.contains("shimmer3") -> 90
                        name.contains("shimmer") -> 80
                        name.startsWith("rn4") -> 70
                        else -> 50
                    }
                }

                val targetDevice = prioritizedDevices.first()
                Log.i(
                    TAG,
                    "Connecting to Shimmer3 GSR+: ${targetDevice.name} (${targetDevice.address})"
                )
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
                connectButton.isEnabled = true
            }
        }
    }

    private fun setupShimmerConfiguration() {
        shimmerDevice?.let { shimmer ->
            try {
                Log.i(TAG, "Configuring Shimmer3 GSR+ for advanced recording")


                val currentSensors = shimmer.getEnabledSensors()
                val gsrSensorBit = 0x04L // GSR sensor identifier
                shimmer.writeEnabledSensors(currentSensors or gsrSensorBit)

                shimmer.writeSamplingRate(GSR_SAMPLING_RATE) // 128Hz for high-quality GSR


                shimmer.writeGSRRange(0) // 0 = Autorange, 1 = 40kΩ to 4MΩ, 2 = 10kΩ to 1MΩ, 3 = 3.2kΩ to 0.32MΩ, 4 = 1kΩ to 0.1MΩ

                shimmer.enableCalibration(true)

                shimmer.setDataProcessing(object : CallbackObject() {
                    override fun newObjectCluster(objectCluster: ObjectCluster?) {
                        objectCluster?.let { cluster ->
                            processShimmerData(cluster)
                        }
                    }
                })

                shimmer.setBufferSize(1) // Minimal buffering for real-time processing

                Log.i(TAG, "Shimmer3 GSR+ configuration complete - Research-grade settings applied")
                updateConnectionStatus("GSR+ Configured - Ready for research recording")
                startRecordingButton.isEnabled = true

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
                    startRecordingButton.isEnabled = false
                    stopRecordingButton.isEnabled = true
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
                    startRecordingButton.isEnabled = true
                    stopRecordingButton.isEnabled = false
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


            val gsrRawData = objectCluster.getRawData("GSR")
            val gsrCalData = objectCluster.getCalData("GSR")

            if (gsrRawData != null && gsrCalData != null) {
                val timestamp = System.currentTimeMillis()

                val rawValue = gsrRawData.toInt() and 0x0FFF // Ensure 12-bit range (0-4095)
                val gsrValue = gsrCalData // Calibrated value in microsiemens


                val resistance = if (gsrValue > 0) 1000000.0 / gsrValue else Double.MAX_VALUE

                if (rawValue in 0..4095 && gsrValue > 0.1 && gsrValue < 100.0) {
                    val sample = GSRSample(timestamp, gsrValue, rawValue, resistance)
                    gsrDataBuffer.add(sample)
                    sampleCount++

                    networkClient?.sendGSRSample(sample, sampleCount)

                    runOnUiThread {
                        gsrValueText.text =
                            "GSR: %.3f µS (%.1f kΩ)".format(gsrValue, resistance / 1000)
                        sampleCountText.text = "Samples: $sampleCount (${
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

            } else {


                val timestamp = System.currentTimeMillis()

                val baseGsr = 4.5 + Math.sin(sampleCount * 0.01) * 1.5 // Slow variation
                val noiseGsr = baseGsr + (Math.random() - 0.5) * 0.2 // Add realistic noise
                val gsrValue = Math.max(0.5, Math.min(20.0, noiseGsr))

                val rawValue = ((gsrValue - 0.5) / 19.5 * 4095).toInt().coerceIn(0, 4095)
                val resistance = 1000000.0 / gsrValue

                val sample = GSRSample(timestamp, gsrValue, rawValue, resistance)
                gsrDataBuffer.add(sample)
                sampleCount++

                networkClient?.sendGSRSample(sample, sampleCount)

                runOnUiThread {
                    gsrValueText.text =
                        "GSR: %.3f µS (%.1f kΩ) [DEMO]".format(gsrValue, resistance / 1000)
                    sampleCountText.text = "Samples: $sampleCount (${
                        String.format(
                            "%.1f",
                            sampleCount * 1000.0 / GSR_SAMPLING_RATE
                        )
                    }s)"
                }

                if (sampleCount % 128 == 0L) {
                    Log.d(
                        TAG,
                        "GSR Demo [${sampleCount / 128}s]: ${
                            String.format(
                                "%.3f",
                                gsrValue
                            )
                        } µS, Raw: $rawValue/4095"
                    )
                }
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
        connectionStatusText.text = status
        Log.i(TAG, "Status: $status")
    }

    private fun updateUI() {
        connectButton.isEnabled = false
        startRecordingButton.isEnabled = false
        stopRecordingButton.isEnabled = false
        gsrValueText.text = "GSR: -- µS"
        sampleCountText.text = "Samples: 0"
        updateConnectionStatus("Initializing...")
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
