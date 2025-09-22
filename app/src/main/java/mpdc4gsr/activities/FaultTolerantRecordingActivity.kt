package mpdc4gsr.activities

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.permissions.PermissionManager

class FaultTolerantRecordingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "FaultTolerantRecording"
    }
    
    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var permissionManager: PermissionManager
    
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var sensorStatusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Starting Enhanced Fault-Tolerant Recording Activity")
        
        // Create simple UI programmatically for production use
        createUI()
        
        // Initialize the enhanced recording system
        initializeRecordingSystem()
    }
    
    private fun createUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        layout.addView(TextView(this).apply {
            text = "Enhanced Fault-Tolerant Recording"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        })
        
        // Status text
        statusText = TextView(this).apply {
            text = "Initializing enhanced recording system..."
            setPadding(0, 0, 0, 16)
        }
        layout.addView(statusText)
        
        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.GONE
        }
        layout.addView(progressBar)
        
        // Sensor status
        sensorStatusText = TextView(this).apply {
            text = "Sensor Status: Initializing..."
            setPadding(0, 16, 0, 16)
        }
        layout.addView(sensorStatusText)
        
        // Start button
        startButton = Button(this).apply {
            text = "Start Enhanced Recording"
            isEnabled = false
            setOnClickListener { startRecording() }
        }
        layout.addView(startButton)
        
        // Stop button  
        stopButton = Button(this).apply {
            text = "Stop Recording"
            isEnabled = false
            setOnClickListener { stopRecording() }
        }
        layout.addView(stopButton)
        
        // Back button
        layout.addView(Button(this).apply {
            text = "Back"
            setOnClickListener { finish() }
        })
        
        setContentView(layout)
    }
    
    private fun initializeRecordingSystem() {
        try {
            permissionManager = PermissionManager(this)
            recordingController = ComprehensiveRecordingController(this, this, permissionManager)
            
            lifecycleScope.launch {
                // Check for any crashed sessions first
                val hasCrashedSessions = recordingController.checkForCrashedSessions()
                if (hasCrashedSessions) {
                    runOnUiThread {
                        showCrashRecoveryDialog()
                    }
                }
                
                // Initialize sensors and update UI
                runOnUiThread {
                    statusText.text = "Enhanced fault-tolerant recording ready"
                    sensorStatusText.text = "Sensors: RGB Camera + GSR + Thermal (with fault isolation)"
                    startButton.isEnabled = true
                }
            }
            
            // Monitor recording state
            lifecycleScope.launch {
                recordingController.recordingStateFlow.collect { state ->
                    runOnUiThread {
                        when (state) {
                            mpdc4gsr.controller.RecordingState.STARTING -> {
                                statusText.text = "Starting sensors with fault tolerance..."
                                startButton.isEnabled = false
                                progressBar.visibility = View.VISIBLE
                            }
                            mpdc4gsr.controller.RecordingState.RECORDING -> {
                                statusText.text = "Recording in progress with health monitoring"
                                startButton.isEnabled = false
                                stopButton.isEnabled = true
                                progressBar.visibility = View.GONE
                            }
                            mpdc4gsr.controller.RecordingState.STOPPING -> {
                                statusText.text = "Stopping recording and finalizing session..."
                                stopButton.isEnabled = false
                                progressBar.visibility = View.VISIBLE
                            }
                            mpdc4gsr.controller.RecordingState.IDLE -> {
                                statusText.text = "Ready for enhanced fault-tolerant recording"
                                startButton.isEnabled = true
                                stopButton.isEnabled = false
                                progressBar.visibility = View.GONE
                            }
                            mpdc4gsr.controller.RecordingState.ERROR -> {
                                statusText.text = "Recording error handled gracefully"
                                startButton.isEnabled = true
                                stopButton.isEnabled = false
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing recording system", e)
            statusText.text = "Failed to initialize: ${e.message}"
        }
    }
    
    private fun showCrashRecoveryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Session Recovery")
            .setMessage("The app detected a previous recording session that didn't end properly. The session data has been recovered and preserved.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun startRecording() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Starting enhanced fault-tolerant recording session")
                
                val success = recordingController.startRecording(
                    sessionId = "FaultTolerant_${System.currentTimeMillis()}",
                    enabledSensors = listOf("RGB", "GSR", "Thermal"),
                    estimatedDurationMinutes = 30
                )
                
                runOnUiThread {
                    if (success) {
                        showToast("Enhanced recording started with fault tolerance")
                    } else {
                        showToast("Recording start failed - check logs for details")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                runOnUiThread {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun stopRecording() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Stopping fault-tolerant recording with graceful teardown")
                
                val success = recordingController.stopRecording()
                
                runOnUiThread {
                    if (success) {
                        showToast("Recording stopped and session finalized")
                    } else {
                        showToast("Recording stop encountered issues - check logs")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                runOnUiThread {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}