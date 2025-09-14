package com.topdon.tc001.sensors.shimmer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.topdon.tc001.sensors.shimmer.model.GSRSample
import com.topdon.tc001.sensors.shimmer.model.ShimmerDeviceInfo
import kotlinx.coroutines.launch
import java.io.File

class ShimmerIntegrationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShimmerIntegration"
        private const val OUTPUT_DIRECTORY = "shimmer_recordings"
    }

    private lateinit var statusText: TextView
    private lateinit var connectionQualityText: TextView
    private lateinit var gsrValueText: TextView
    private lateinit var sampleCountText: TextView
    private lateinit var qualityScoreText: TextView

    private lateinit var scanButton: Button
    private lateinit var connectButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button

    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var deviceAdapter: ShimmerDeviceAdapter

    private lateinit var progressBar: ProgressBar

    private lateinit var deviceManager: ShimmerDeviceManager
    private lateinit var gsrRecorder: Shimmer3GSRRecorder

    private var selectedDevice: ShimmerDeviceInfo? = null
    private var isRecording = false
    private var recordingStartTime = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeShimmerComponents()
            updateUI()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val bluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            initializeShimmerComponents()
            updateUI()
        } else {
            showBluetoothRequiredDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_integration)

        initializeUI()
        checkPermissionsAndInitialize()
    }

    private fun initializeUI() {

        statusText = findViewById(R.id.statusText)
        connectionQualityText = findViewById(R.id.connectionQualityText)
        gsrValueText = findViewById(R.id.gsrValueText)
        sampleCountText = findViewById(R.id.sampleCountText)
        qualityScoreText = findViewById(R.id.qualityScoreText)

        scanButton = findViewById(R.id.scanButton)
        connectButton = findViewById(R.id.connectButton)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        deviceAdapter = ShimmerDeviceAdapter { device -> onDeviceSelected(device) }
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceRecyclerView.adapter = deviceAdapter

        progressBar = findViewById(R.id.progressBar)

        scanButton.setOnClickListener { startDeviceScanning() }
        connectButton.setOnClickListener { connectToSelectedDevice() }
        startRecordingButton.setOnClickListener { startGSRRecording() }
        stopRecordingButton.setOnClickListener { stopGSRRecording() }

        updateUI()
    }

    private fun checkPermissionsAndInitialize() {
        val requiredPermissions = ShimmerDeviceManager.getRequiredPermissions()

        if (ShimmerDeviceManager.hasDiscoveryPermissions(this)) {
            initializeShimmerComponents()
            updateUI()
        } else {

            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun initializeShimmerComponents() {
        try {

            deviceManager = ShimmerDeviceManager(this, this)

            gsrRecorder = Shimmer3GSRRecorder(this, this, samplingRateHz = 128)

            observeShimmerState()

            statusText.text = "Shimmer components initialized"
            Log.d(TAG, "Shimmer components initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer components", e)
            statusText.text = "Initialization failed: ${e.message}"
        }
    }

    private fun observeShimmerState() {

        lifecycleScope.launch {
            deviceManager.discoveredDeviceList.collect { devices ->
                deviceAdapter.updateDevices(devices)
                statusText.text = "Found ${devices.size} Shimmer devices"
            }
        }

        lifecycleScope.launch {
            deviceManager.isScanning.collect { scanning ->
                progressBar.visibility = if (scanning) View.VISIBLE else View.GONE
                scanButton.isEnabled = !scanning
            }
        }

        lifecycleScope.launch {
            gsrRecorder.connectionQuality.collect { quality ->
                connectionQualityText.text = "Quality: ${quality.displayName}"
                connectionQualityText.setTextColor(
                    android.graphics.Color.parseColor(quality.color)
                )
            }
        }

        lifecycleScope.launch {
            gsrRecorder.getGSRDataFlow().collect { gsrSample ->
                updateGSRDisplay(gsrSample)
            }
        }

        lifecycleScope.launch {
            gsrRecorder.samplesCollected.collect { count ->
                sampleCountText.text = "Samples: $count"
            }
        }

        lifecycleScope.launch {
            gsrRecorder.deviceStatus.collect { status ->
                if (selectedDevice != null) {
                    statusText.text = "Device: $status"
                }
            }
        }
    }

    private fun startDeviceScanning() {
        lifecycleScope.launch {
            try {
                statusText.text = "Scanning for Shimmer devices..."
                deviceAdapter.clearDevices()

                deviceManager.startDeviceDiscovery(durationMs = 10000L).collect { devices ->
                    Log.d(TAG, "Discovered ${devices.size} Shimmer devices")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during device scanning", e)
                statusText.text = "Scan error: ${e.message}"
            }
        }
    }

    private fun onDeviceSelected(device: ShimmerDeviceInfo) {
        selectedDevice = device
        connectButton.isEnabled = device.isReadyForConnection()

        statusText.text = "Selected: ${device.name}"
        Log.d(TAG, "Device selected: ${device}")
    }

    private fun connectToSelectedDevice() {
        selectedDevice?.let { device ->
            lifecycleScope.launch {
                try {
                    statusText.text = "Connecting to ${device.name}..."
                    connectButton.isEnabled = false

                    val success = gsrRecorder.connectToDevice(device)

                    if (success) {
                        statusText.text = "Connected to ${device.name}"
                        startRecordingButton.isEnabled = true
                    } else {
                        statusText.text = "Connection failed"
                        connectButton.isEnabled = true
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to device", e)
                    statusText.text = "Connection error: ${e.message}"
                    connectButton.isEnabled = true
                }
            }
        } ?: run {
            Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGSRRecording() {
        lifecycleScope.launch {
            try {
                val outputDir = File(getExternalFilesDir(null), OUTPUT_DIRECTORY)
                outputDir.mkdirs()

                val success = gsrRecorder.startRecording(outputDir)

                if (success) {
                    isRecording = true
                    recordingStartTime = System.currentTimeMillis()
                    statusText.text = "Recording GSR data..."

                    startRecordingButton.isEnabled = false
                    stopRecordingButton.isEnabled = true
                } else {
                    statusText.text = "Failed to start recording"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                statusText.text = "Recording error: ${e.message}"
            }
        }
    }

    private fun stopGSRRecording() {
        lifecycleScope.launch {
            try {
                val success = gsrRecorder.stopRecording()

                if (success) {
                    isRecording = false
                    val duration = (System.currentTimeMillis() - recordingStartTime) / 1000.0
                    statusText.text = "Recording stopped (${String.format("%.1f", duration)}s)"

                    startRecordingButton.isEnabled = true
                    stopRecordingButton.isEnabled = false
                } else {
                    statusText.text = "Failed to stop recording"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                statusText.text = "Stop recording error: ${e.message}"
            }
        }
    }

    private fun updateGSRDisplay(gsrSample: GSRSample) {
        runOnUiThread {
            gsrValueText.text = String.format("GSR: %.2f µS", gsrSample.gsrMicrosiemens)
            qualityScoreText.text = String.format(
                "Quality: %.1f%% (%s)",
                gsrSample.qualityScore * 100, gsrSample.getQualityLevel()
            )
        }
    }

    private fun updateUI() {
        val hasPermissions = ShimmerDeviceManager.hasDiscoveryPermissions(this)

        scanButton.isEnabled = hasPermissions && !isRecording
        connectButton.isEnabled =
            hasPermissions && selectedDevice?.isReadyForConnection() == true && !isRecording
        startRecordingButton.isEnabled = selectedDevice != null && !isRecording
        stopRecordingButton.isEnabled = isRecording

        if (!hasPermissions) {
            statusText.text = "Permissions required for Shimmer integration"
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "Shimmer GSR integration requires Bluetooth and location permissions. " +
                        "Please grant these permissions to use Shimmer devices."
            )
            .setPositiveButton("Grant Permissions") { _, _ ->

                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showBluetoothRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Required")
            .setMessage(
                "Shimmer GSR sensors require Bluetooth connectivity. " +
                        "Please enable Bluetooth to continue."
            )
            .setPositiveButton("Enable Bluetooth") { _, _ ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::gsrRecorder.isInitialized) {
            gsrRecorder.cleanup()
        }

        if (::deviceManager.isInitialized) {
            deviceManager.cleanup()
        }

        Log.d(TAG, "Shimmer Integration Activity destroyed")
    }
}
