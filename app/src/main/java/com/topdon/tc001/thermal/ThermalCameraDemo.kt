package com.topdon.tc001.thermal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        // Note: You would need to create activity_thermal_demo.xml layout
        // setContentView(R.layout.activity_thermal_demo)

        requestPermissions()
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
                                // In a real app, you would update an ImageView here
                                // thermalPreview.setImageBitmap(it)
                            }
                        }
                    }
                })

                val success = thermalRecorder.initialize()

                if (success) {
                    updateStatus("✅ Thermal camera ready. Plug in TC001 for hardware mode or use simulation.")
                    
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

                    // Start thermal recording demo
                    startThermalDemo()

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
    }

    private fun disableNetworkStreaming() {
        thermalRecorder.disableNetworkStreaming()
        isNetworkEnabled = false
        updateStatus("🌐 Thermal network streaming disabled")
    }

    private fun startThermalDemo() {
        lifecycleScope.launch {
            try {
                val sessionDir = File(filesDir, "thermal_demo_${System.currentTimeMillis()}")
                sessionDir.mkdirs()

                updateStatus("🚀 Starting thermal recording demo...")
                val success = thermalRecorder.startRecording(sessionDir.absolutePath)

                if (success) {
                    updateStatus("🔴 Thermal recording started - generating frames...")
                    
                    // Let it record for 30 seconds in demo mode
                    kotlinx.coroutines.delay(30000)
                    
                    val stopSuccess = thermalRecorder.stopRecording()
                    if (stopSuccess) {
                        val stats = thermalRecorder.getRecordingStats()
                        updateStatus("✅ Demo completed! Recorded ${stats.totalSamplesRecorded} thermal frames")
                        Toast.makeText(
                            this@ThermalCameraDemo,
                            "Thermal Demo Complete!\n" +
                            "Frames: ${stats.totalSamplesRecorded}\n" +
                            "Data Rate: ${String.format("%.1f", stats.averageDataRate)} FPS\n" +
                            "Storage: ${String.format("%.2f", stats.storageUsedMB)} MB",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    updateStatus("❌ Failed to start thermal recording")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to run thermal demo", e)
                updateStatus("❌ Demo failed: ${e.message}")
            }
        }
    }

    private fun updateStatus(message: String) {
        Log.i(TAG, message)
        runOnUiThread {
            // In a real app, you would update a TextView here
            // statusText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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