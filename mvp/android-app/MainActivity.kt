package com.ircamera.mvp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * MVP Main Activity - Simple Multi-Modal Physiological Data Recording
 * 
 * Core functionality:
 * - GSR data via Shimmer3 BLE
 * - RGB camera recording
 * - Basic thermal integration
 * - Timestamped data export
 */
class MainActivity : AppCompatActivity() {
    
    // Core components
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var cameraRecorder: CameraRecorder
    private lateinit var networkClient: NetworkClient
    private lateinit var dataLogger: DataLogger
    
    // UI components
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var gsrValueText: TextView
    
    // Recording state
    private var isRecording = false
    private var recordingJob: Job? = null
    
    // Session info
    private var currentSessionId: String? = null
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeUI()
        checkPermissions()
        initializeComponents()
    }
    
    private fun initializeUI() {
        statusText = findViewById(R.id.status_text)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        gsrValueText = findViewById(R.id.gsr_value_text)
        
        startButton.setOnClickListener { startRecording() }
        stopButton.setOnClickListener { stopRecording() }
        
        updateUI()
    }
    
    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, 
                missingPermissions.toTypedArray(), 
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun initializeComponents() {
        try {
            // Initialize core recording components
            gsrRecorder = GSRRecorder(this) { gsrValue ->
                runOnUiThread {
                    gsrValueText.text = "GSR: ${String.format("%.2f", gsrValue)} μS"
                }
            }
            
            cameraRecorder = CameraRecorder(this)
            networkClient = NetworkClient()
            dataLogger = DataLogger(this)
            
            statusText.text = "Ready - Components initialized"
            
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
            Toast.makeText(this, "Initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startRecording() {
        if (isRecording) return
        
        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                isRecording = true
                currentSessionId = "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                
                statusText.text = "Starting recording session: $currentSessionId"
                
                // Start all recording components
                gsrRecorder.startRecording(currentSessionId!!)
                cameraRecorder.startRecording(currentSessionId!!)
                dataLogger.startSession(currentSessionId!!)
                
                // Connect to PC Controller if available
                try {
                    networkClient.connect()
                    networkClient.sendSessionStart(currentSessionId!!)
                } catch (e: Exception) {
                    // Continue recording even if PC connection fails
                    statusText.text = "Recording (offline mode): ${e.message}"
                }
                
                statusText.text = "Recording active - Session: $currentSessionId"
                updateUI()
                
            } catch (e: Exception) {
                isRecording = false
                statusText.text = "Failed to start recording: ${e.message}"
                Toast.makeText(this@MainActivity, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
                updateUI()
            }
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                isRecording = false
                statusText.text = "Stopping recording..."
                
                // Stop all recording components
                gsrRecorder.stopRecording()
                cameraRecorder.stopRecording()
                
                // Finalize data
                val exportPath = dataLogger.finalizeSession()
                
                // Notify PC Controller
                try {
                    currentSessionId?.let { networkClient.sendSessionStop(it) }
                    networkClient.disconnect()
                } catch (e: Exception) {
                    // Ignore network errors during stop
                }
                
                statusText.text = "Recording complete - Data saved to: $exportPath"
                currentSessionId = null
                updateUI()
                
                Toast.makeText(this@MainActivity, "Recording saved to: $exportPath", Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                statusText.text = "Error stopping recording: ${e.message}"
                Toast.makeText(this@MainActivity, "Stop error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateUI() {
        startButton.isEnabled = !isRecording
        stopButton.isEnabled = isRecording
        
        if (!isRecording) {
            gsrValueText.text = "GSR: -- μS"
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this, 
                    "Required permissions denied: ${deniedPermissions.joinToString()}", 
                    Toast.LENGTH_LONG
                ).show()
                statusText.text = "Error: Missing required permissions"
            } else {
                initializeComponents()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        recordingJob?.cancel()
        
        if (isRecording) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    gsrRecorder.stopRecording()
                    cameraRecorder.stopRecording()
                    dataLogger.finalizeSession()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        
        try {
            networkClient.disconnect()
        } catch (e: Exception) {
            // Ignore network cleanup errors
        }
    }
}