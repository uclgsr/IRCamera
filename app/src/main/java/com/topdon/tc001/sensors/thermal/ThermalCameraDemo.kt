package com.topdon.tc001.sensors.thermal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.R
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.network.NetworkServer
import kotlinx.coroutines.launch
import java.io.File

/**
 * Demo activity to showcase the USB permission flow, thermal camera integration,
 * and network streaming for the Topdon TC001 thermal camera.
 */
class ThermalCameraDemo : AppCompatActivity() {

    companion object {
        private const val TAG = "ThermalCameraDemo"
        private const val NETWORK_PORT = 8080
    }

    private lateinit var thermalRecorder: ThermalCameraRecorder
    private lateinit var networkServer: NetworkServer
    private lateinit var statusText: TextView
    private lateinit var thermalPreview: ImageView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var networkButton: Button
    private lateinit var configButton: Button
    private lateinit var exportButton: Button

    private var isNetworkEnabled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            Log.i(TAG, "All permissions granted")
            initializeThermalCamera()
        } else {
            Log.w(TAG, "Permissions denied")
            updateStatus("Permissions required for thermal camera operation")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermal_demo)

        // Initialize UI components
        statusText = findViewById(R.id.statusText)
        thermalPreview = findViewById(R.id.thermalPreview)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        networkButton = findViewById(R.id.networkButton)
        configButton = findViewById(R.id.configButton)
        exportButton = findViewById(R.id.exportButton)

        // Set up button click listeners
        setupButtonListeners()

        requestPermissions()
    }

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            lifecycleScope.launch {
                startThermalRecording()
            }
        }

        stopButton.setOnClickListener {
            lifecycleScope.launch {
                stopThermalRecording()
            }
        }

        networkButton.setOnClickListener {
            toggleNetworkStreaming()
        }

        configButton.setOnClickListener {
            showConfigurationDialog()
        }

        exportButton.setOnClickListener {
            lifecycleScope.launch {
                exportThermalData()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeThermalCamera()
        }
    }

    private fun initializeThermalCamera() {
        lifecycleScope.launch {
            try {
                updateStatus("Initializing thermal camera and network server...")

                // Initialize network server
                networkServer = NetworkServer(this@ThermalCameraDemo, NETWORK_PORT)

                // Initialize thermal camera recorder
                thermalRecorder = ThermalCameraRecorder(this@ThermalCameraDemo)

                // Set up thermal preview callback
                thermalRecorder.setThermalPreviewCallback(object :
                    ThermalCameraRecorder.ThermalPreviewCallback {
                    override fun onThermalFrame(
                        bitmap: Bitmap?,
                        temperatureData: ThermalCameraRecorder.ThermalFrameData?
                    ) {
                        runOnUiThread {
                            // Update UI with thermal preview
                            bitmap?.let {
                                Log.d(TAG, "Thermal frame received: ${it.width}x${it.height}")
                                thermalPreview.setImageBitmap(it)
                            }

                            // Update temperature display
                            temperatureData?.let { data ->
                                findViewById<TextView>(R.id.minTempText)?.text =
                                    String.format("%.1f°C", data.minTemperature)
                                findViewById<TextView>(R.id.maxTempText)?.text =
                                    String.format("%.1f°C", data.maxTemperature)

                                // Update performance metrics
                                val metrics = thermalRecorder.getPerformanceMetrics()
                                findViewById<TextView>(R.id.fpsText)?.text =
                                    String.format("%.1f", metrics.averageFrameRate)
                                findViewById<TextView>(R.id.cpuText)?.text =
                                    String.format("%.1f%%", metrics.cpuUsagePercent)
                            }
                        }
                    }
                })

                val success = thermalRecorder.initialize()

                if (success) {
                    updateStatus("✅ Thermal camera ready. Plug in TC001 for hardware mode or use simulation.")

                    // Enable UI controls
                    runOnUiThread {
                        startButton.isEnabled = true
                        networkButton.isEnabled = true
                        configButton.isEnabled = true
                        exportButton.isEnabled = true
                    }

                    // Start network server
                    networkServer.start()
                    updateStatus("✅ Network server started on port $NETWORK_PORT")

                    // Monitor thermal camera status and errors
                    lifecycleScope.launch {
                        thermalRecorder.getStatusFlow().collect { status ->
                            updateStatus("${status.sensorType}: ${if (status.isRecording) "🔴 Recording" else "⚪ Idle"} - Frames: ${status.samplesRecorded}")
                        }
                    }

                    lifecycleScope.launch {
                        thermalRecorder.getErrorFlow().collect { error ->
                            updateStatus("❌ Error: ${error.errorMessage}")
                            Toast.makeText(
                                this@ThermalCameraDemo,
                                "Thermal Error: ${error.errorMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // Monitor network server connection
                    lifecycleScope.launch {
                        networkServer.connectionStateFlow.collect { connected ->
                            if (connected) {
                                updateStatus("🌐 PC client connected - network streaming available")
                                if (!isNetworkEnabled) {
                                    enableNetworkStreaming()
                                }
                            } else {
                                updateStatus("🌐 No PC client connected")
                                if (isNetworkEnabled) {
                                    disableNetworkStreaming()
                                }
                            }
                        }
                    }

                } else {
                    updateStatus("❌ Failed to initialize thermal camera")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize thermal camera", e)
                updateStatus("❌ Initialization failed: ${e.message}")
            }
        }
    }

    private fun enableNetworkStreaming() {
        thermalRecorder.enableNetworkStreaming(networkServer)
        isNetworkEnabled = true
        updateStatus("🌐 Thermal network streaming enabled (~2 FPS)")
        runOnUiThread {
            networkButton.text = "Disable Network Streaming"
        }
    }

    private fun disableNetworkStreaming() {
        thermalRecorder.disableNetworkStreaming()
        isNetworkEnabled = false
        updateStatus("🌐 Thermal network streaming disabled")
        runOnUiThread {
            networkButton.text = "Enable Network Streaming"
        }
    }

    private fun toggleNetworkStreaming() {
        if (isNetworkEnabled) {
            disableNetworkStreaming()
        } else {
            enableNetworkStreaming()
        }
    }

    private suspend fun startThermalRecording() {
        try {
            val sessionDir = File(filesDir, "thermal_demo_${System.currentTimeMillis()}")
            sessionDir.mkdirs()

            updateStatus("🚀 Starting thermal recording...")
            val success = thermalRecorder.startRecording(sessionDir.absolutePath)

            if (success) {
                updateStatus("🔴 Thermal recording started - generating frames...")
                runOnUiThread {
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                }
            } else {
                updateStatus("❌ Failed to start thermal recording")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            updateStatus("❌ Recording failed: ${e.message}")
        }
    }

    private suspend fun stopThermalRecording() {
        try {
            val stopSuccess = thermalRecorder.stopRecording()
            if (stopSuccess) {
                val stats = thermalRecorder.getRecordingStats()
                updateStatus("✅ Recording stopped! Recorded ${stats.totalSamplesRecorded} thermal frames")

                runOnUiThread {
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                }

                Toast.makeText(
                    this@ThermalCameraDemo,
                    "Recording Complete!\n" +
                            "Frames: ${stats.totalSamplesRecorded}\n" +
                            "Data Rate: ${String.format("%.1f", stats.averageDataRate)} FPS\n" +
                            "Storage: ${String.format("%.2f", stats.storageUsedMB)} MB",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                updateStatus("❌ Failed to stop recording")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            updateStatus("❌ Stop recording failed: ${e.message}")
        }
    }

    private fun showConfigurationDialog() {
        val configItems = arrayOf(
            "Emissivity: 0.95",
            "Temperature Range: -20°C to 400°C",
            "Atmospheric Temp: 25°C",
            "Humidity: 50%",
            "Distance: 1.0m",
            "Palette: IRON",
            "Noise Reduction: ON",
            "Auto Gain: ON"
        )

        AlertDialog.Builder(this)
            .setTitle("Thermal Camera Configuration")
            .setItems(configItems) { _, which ->
                when (which) {
                    0 -> showEmissivityDialog()
                    1 -> showTemperatureRangeDialog()
                    2 -> showAtmosphericTempDialog()
                    5 -> showPaletteDialog()
                    else -> {
                        Toast.makeText(
                            this,
                            "Configuration option selected: ${configItems[which]}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmissivityDialog() {
        val input = EditText(this)
        input.setText("0.95")

        AlertDialog.Builder(this)
            .setTitle("Set Emissivity")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                try {
                    val emissivity = input.text.toString().toDouble()
                    if (emissivity in 0.1..1.0) {
                        thermalRecorder.updateCalibration(25.0, emissivity, 23.0)
                        updateStatus("✅ Emissivity updated to $emissivity")
                    } else {
                        Toast.makeText(
                            this,
                            "Emissivity must be between 0.1 and 1.0",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid emissivity value", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTemperatureRangeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Temperature Range")
            .setMessage("Current range: -20°C to 400°C\n\nThis setting affects measurement accuracy for different temperature ranges.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAtmosphericTempDialog() {
        val input = EditText(this)
        input.setText("25.0")

        AlertDialog.Builder(this)
            .setTitle("Set Atmospheric Temperature (°C)")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                try {
                    val temp = input.text.toString().toDouble()
                    if (temp in -40.0..80.0) {
                        thermalRecorder.updateCalibration(temp, 0.95, temp - 2.0)
                        updateStatus("✅ Atmospheric temperature updated to ${temp}°C")
                    } else {
                        Toast.makeText(
                            this,
                            "Temperature must be between -40°C and 80°C",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid temperature value", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPaletteDialog() {
        val palettes = arrayOf("IRON", "RAINBOW", "GRAYSCALE", "HOT", "COOL", "JET")

        AlertDialog.Builder(this)
            .setTitle("Select Color Palette")
            .setItems(palettes) { _, which ->
                val selectedPalette = palettes[which]
                updateStatus("✅ Color palette changed to $selectedPalette")
                Toast.makeText(this, "Palette: $selectedPalette", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun exportThermalData() {
        try {
            updateStatus("📁 Exporting thermal data...")

            val exportDir = File(filesDir, "thermal_exports")
            val success = thermalRecorder.exportThermalData(
                exportDir.absolutePath,
                ThermalCameraRecorder.ThermalExportFormat.CSV,
                includeImages = true
            )

            if (success) {
                updateStatus("✅ Thermal data exported successfully")
                val metrics = thermalRecorder.getPerformanceMetrics()

                Toast.makeText(
                    this@ThermalCameraDemo,
                    "Export Complete!\n" +
                            "Location: ${exportDir.absolutePath}\n" +
                            "Avg FPS: ${String.format("%.1f", metrics.averageFrameRate)}\n" +
                            "Memory: ${String.format("%.1f", metrics.memoryUsageMB)} MB",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                updateStatus("❌ Failed to export thermal data")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export thermal data", e)
            updateStatus("❌ Export failed: ${e.message}")
        }
    }

    private fun updateStatus(message: String) {
        Log.i(TAG, message)
        runOnUiThread {
            statusText.text = message
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            thermalRecorder.cleanup()
            networkServer.stop()
        }
    }
}
