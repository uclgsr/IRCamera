package com.topdon.tc001.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.R
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.data.SessionMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity for testing synchronization between multiple sensor modalities.
 * 
 * This activity implements the "Flash Sync" test mentioned in the requirements:
 * - Performs a test where a simultaneous event is captured by all sensors
 * - Triggers a flash on the screen while recording to create a sync marker
 * - Verifies alignment by checking timestamps across video, thermal, and GSR data
 * 
 * The test helps validate that the synchronization system achieves the target
 * accuracy of within 50-100ms alignment across all modalities.
 */
class SynchronizationTestActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "SynchronizationTest"
        private const val FLASH_DURATION_MS = 500L
        private const val TEST_RECORDING_DURATION_MS = 10000L // 10 seconds
    }
    
    private lateinit var recordingController: RecordingController
    private lateinit var statusText: TextView
    private lateinit var startTestButton: Button
    private lateinit var flashSyncButton: Button
    private lateinit var stopTestButton: Button
    
    private var isTestRunning = false
    private var currentSessionMetadata: SessionMetadata? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_synchronization_test)
        
        initializeViews()
        initializeRecordingController()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        startTestButton = findViewById(R.id.startTestButton)
        flashSyncButton = findViewById(R.id.flashSyncButton)
        stopTestButton = findViewById(R.id.stopTestButton)
        
        updateStatus("Ready to start synchronization test")
        flashSyncButton.isEnabled = false
        stopTestButton.isEnabled = false
    }
    
    private fun initializeRecordingController() {
        recordingController = RecordingController(this, this)
        
        lifecycleScope.launch {
            val initialized = recordingController.initializeSensors()
            if (initialized) {
                updateStatus("Sensors initialized. Ready for sync test.")
            } else {
                updateStatus("Error: Failed to initialize sensors")
            }
        }
    }
    
    private fun setupClickListeners() {
        startTestButton.setOnClickListener {
            if (!isTestRunning) {
                startSynchronizationTest()
            }
        }
        
        flashSyncButton.setOnClickListener {
            triggerFlashSyncEvent()
        }
        
        stopTestButton.setOnClickListener {
            stopSynchronizationTest()
        }
    }
    
    private fun startSynchronizationTest() {
        lifecycleScope.launch {
            try {
                isTestRunning = true
                updateButtonStates()
                
                updateStatus("Starting synchronization test...")
                
                // Create test session directory
                val testSessionDir = File(getExternalFilesDir(null), "sync_test_${System.currentTimeMillis()}")
                testSessionDir.mkdirs()
                
                // Start recording with session metadata
                val sessionId = "SYNC_TEST_${System.currentTimeMillis()}"
                val success = recordingController.startRecording(testSessionDir.absolutePath, sessionId)
                
                if (success) {
                    updateStatus("Recording started. You can now trigger flash sync events.")
                    
                    // Schedule automatic sync events for testing
                    scheduleAutomaticSyncEvents()
                    
                    // Auto-stop after test duration
                    lifecycleScope.launch {
                        delay(TEST_RECORDING_DURATION_MS)
                        if (isTestRunning) {
                            stopSynchronizationTest()
                        }
                    }
                } else {
                    updateStatus("Error: Failed to start recording")
                    isTestRunning = false
                    updateButtonStates()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting synchronization test", e)
                updateStatus("Error: ${e.message}")
                isTestRunning = false
                updateButtonStates()
            }
        }
    }
    
    private fun scheduleAutomaticSyncEvents() {
        lifecycleScope.launch {
            // Schedule sync events at regular intervals
            val eventTimes = listOf(2000L, 4000L, 6000L, 8000L) // 2, 4, 6, 8 seconds
            
            for (eventTime in eventTimes) {
                delay(eventTime)
                if (isTestRunning) {
                    triggerAutomaticSyncEvent("AUTO_SYNC_${eventTime}ms")
                }
            }
        }
    }
    
    private fun triggerFlashSyncEvent() {
        if (!isTestRunning) return
        
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Triggering manual flash sync event")
                
                // Flash the screen white
                flashScreen()
                
                // Add sync marker to all sensors
                recordingController.addSyncMarker(
                    "MANUAL_FLASH_SYNC",
                    android.os.SystemClock.elapsedRealtimeNanos(),
                    mapOf(
                        "trigger_type" to "manual",
                        "test_event" to "screen_flash",
                        "expected_visibility" to "all_modalities"
                    )
                )
                
                updateStatus("Flash sync event triggered - check for simultaneous detection across all sensors")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering flash sync", e)
                updateStatus("Error triggering sync event: ${e.message}")
            }
        }
    }
    
    private fun triggerAutomaticSyncEvent(eventName: String) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Triggering automatic sync event: $eventName")
                
                // Brief screen flash for automatic sync
                flashScreen(duration = 200L)
                
                // Add sync marker
                recordingController.addSyncMarker(
                    eventName,
                    android.os.SystemClock.elapsedRealtimeNanos(),
                    mapOf(
                        "trigger_type" to "automatic",
                        "test_event" to "screen_flash",
                        "timing" to eventName
                    )
                )
                
                updateStatus("Auto sync event: $eventName")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering automatic sync event", e)
            }
        }
    }
    
    private fun flashScreen(duration: Long = FLASH_DURATION_MS) {
        lifecycleScope.launch {
            // Change background to white
            window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
            
            delay(duration)
            
            // Restore original background
            window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        }
    }
    
    private fun stopSynchronizationTest() {
        lifecycleScope.launch {
            try {
                updateStatus("Stopping synchronization test...")
                
                val success = recordingController.stopRecording()
                
                if (success) {
                    updateStatus("Test completed. Check recorded data for sync event alignment.")
                    
                    // Generate test report
                    generateSyncTestReport()
                } else {
                    updateStatus("Warning: Some sensors may not have stopped cleanly")
                }
                
                isTestRunning = false
                updateButtonStates()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping synchronization test", e)
                updateStatus("Error stopping test: ${e.message}")
                isTestRunning = false
                updateButtonStates()
            }
        }
    }
    
    private fun generateSyncTestReport() {
        lifecycleScope.launch {
            try {
                val stats = recordingController.getRecordingStatistics()
                
                val report = buildString {
                    appendLine("=== Synchronization Test Report ===")
                    appendLine("Test Duration: ${stats.sessionDurationSeconds:.1f}s")
                    appendLine("Active Sensors: ${stats.activeSensors}")
                    appendLine("Total Samples: ${stats.totalSamplesRecorded}")
                    appendLine("Storage Used: ${stats.totalStorageUsedMB:.2f}MB")
                    appendLine()
                    appendLine("Sensor Details:")
                    stats.sensorStatistics.forEach { sensor ->
                        appendLine("  ${sensor.sensorType}: ${sensor.totalSamplesRecorded} samples")
                    }
                    appendLine()
                    appendLine("To verify synchronization:")
                    appendLine("1. Check video for screen flashes at sync event times")
                    appendLine("2. Look for corresponding temperature spikes in thermal data")
                    appendLine("3. Verify GSR readings show sync markers at same times")
                    appendLine("4. Use sync_data_streams.py script for automated alignment")
                }
                
                Log.i(TAG, report)
                updateStatus("Test report generated. Check logs for details.")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating test report", e)
            }
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.i(TAG, "Status: $message")
        }
    }
    
    private fun updateButtonStates() {
        runOnUiThread {
            startTestButton.isEnabled = !isTestRunning
            flashSyncButton.isEnabled = isTestRunning
            stopTestButton.isEnabled = isTestRunning
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isTestRunning) {
            lifecycleScope.launch {
                recordingController.stopRecording()
                recordingController.cleanup()
            }
        }
    }
}