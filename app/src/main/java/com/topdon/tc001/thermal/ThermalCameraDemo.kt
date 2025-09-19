package com.topdon.tc001.thermal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
 * 
 * NOTE: This is a simplified stub version to ensure build compilation.
 * Full implementation requires additional thermal API integration.
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
            Log.i(TAG, "All permissions granted - initializing thermal camera")
            initializeThermalSystem()
        } else {
            Log.w(TAG, "Permissions denied - some features may not work")
            statusText.text = "Permissions required for full functionality"
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
            showCalibrationDialog()
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
            initializeThermalSystem()
        }
    }

    private fun initializeThermalSystem() {
        try {
            // Initialize network server
            networkServer = NetworkServer(this@ThermalCameraDemo, NETWORK_PORT)

            // Initialize thermal camera recorder
            thermalRecorder = ThermalCameraRecorder(this@ThermalCameraDemo)

            statusText.text = "Thermal system initialized successfully (stub)"
            startButton.isEnabled = true

            Log.i(TAG, "Thermal system initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thermal system", e)
            statusText.text = "Failed to initialize: ${e.message}"
        }
    }

    private suspend fun startThermalRecording() {
        try {
            Log.i(TAG, "Starting thermal recording...")
            statusText.text = "Starting thermal recording..."

            val sessionDir = File(getExternalFilesDir("thermal"), "session_${System.currentTimeMillis()}")
            
            if (thermalRecorder.startRecording(sessionDir.absolutePath)) {
                startButton.isEnabled = false
                stopButton.isEnabled = true
                statusText.text = "Recording thermal data..."
                Log.i(TAG, "Thermal recording started successfully")
            } else {
                statusText.text = "Failed to start thermal recording"
                Log.e(TAG, "Failed to start thermal recording")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting thermal recording", e)
            statusText.text = "Error: ${e.message}"
        }
    }

    private suspend fun stopThermalRecording() {
        try {
            Log.i(TAG, "Stopping thermal recording...")
            statusText.text = "Stopping thermal recording..."

            thermalRecorder.stopRecording()

            startButton.isEnabled = true
            stopButton.isEnabled = false
            statusText.text = "Recording stopped"
            Log.i(TAG, "Thermal recording stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal recording", e)
            statusText.text = "Error stopping: ${e.message}"
        }
    }

    private fun toggleNetworkStreaming() {
        try {
            if (!isNetworkEnabled) {
                networkButton.text = "Disable Streaming"
                isNetworkEnabled = true
                Log.i(TAG, "Network streaming enabled (stub)")
                statusText.text = "${statusText.text}\nNetwork streaming enabled"
            } else {
                networkButton.text = "Enable Streaming"
                isNetworkEnabled = false
                Log.i(TAG, "Network streaming disabled (stub)")
                statusText.text = statusText.text.toString().replace("\nNetwork streaming enabled", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling network streaming", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCalibrationDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Thermal Calibration")
            .setMessage("Configure thermal camera calibration parameters (stub implementation)")
            .setPositiveButton("Apply") { _, _ ->
                applyCalibration(0.95f, 20.0f)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun applyCalibration(emissivity: Float, reflectedTemp: Float) {
        try {
            Log.i(TAG, "Applying calibration: emissivity=$emissivity, reflectedTemp=$reflectedTemp")
            
            statusText.text = "Calibration applied (stub)"
            Toast.makeText(this, "Calibration applied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying calibration", e)
            Toast.makeText(this, "Calibration failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun exportThermalData() {
        try {
            Log.i(TAG, "Exporting thermal data...")
            statusText.text = "Exporting data..."

            val exportDir = File(getExternalFilesDir("exports"), "thermal_export_${System.currentTimeMillis()}")
            
            // Stub implementation
            val success = true
            
            if (success) {
                statusText.text = "Data exported successfully (stub)"
            } else {
                statusText.text = "Export failed"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
            statusText.text = "Export error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        try {
            if (::thermalRecorder.isInitialized) {
                lifecycleScope.launch {
                    thermalRecorder.stopRecording()
                }
            }
            
            if (::networkServer.isInitialized) {
                lifecycleScope.launch {
                    networkServer.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}