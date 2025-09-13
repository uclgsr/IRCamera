package com.topdon.tc001.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.camera.ui.RecordingStatusIndicator
import com.topdon.tc001.controller.RecordingController
import kotlinx.coroutines.launch
import java.io.File

/**
 * Test activity to demonstrate the robust parallel recording functionality.
 *
 * This activity allows testing of the enhanced RecordingController that can handle
 * individual sensor failures gracefully without aborting the entire session.
 *
 * Features tested:
 * - Sensor initialization with partial failures
 * - Recording start with some sensors failing
 * - Real-time status monitoring
 * - Graceful session continuation with available sensors
 */
class ParallelRecordingTestActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ParallelRecordingTest"
    }

    private lateinit var recordingController: RecordingController
    private lateinit var statusIndicator: RecordingStatusIndicator
    private lateinit var statusText: TextView
    private lateinit var initializeButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple test UI programmatically (since we don't have layout files in this PR)
        createTestUI()

        // Initialize recording controller
        recordingController = RecordingController(this, this)

        setupEventHandlers()
        updateUI()
    }

    private fun createTestUI() {
        // Create simple vertical layout programmatically
        val layout =
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

        // Title
        val titleText =
            TextView(this).apply {
                text = "Parallel Recording Test"
                textSize = 20f
                setPadding(0, 0, 0, 24)
            }
        layout.addView(titleText)

        // Status indicator
        statusIndicator = RecordingStatusIndicator(this)
        layout.addView(statusIndicator)

        // Status text
        statusText =
            TextView(this).apply {
                text = "Press 'Initialize Sensors' to begin"
                setPadding(0, 16, 0, 16)
            }
        layout.addView(statusText)

        // Buttons
        initializeButton =
            Button(this).apply {
                text = "Initialize Sensors"
            }
        layout.addView(initializeButton)

        startButton =
            Button(this).apply {
                text = "Start Recording"
                isEnabled = false
            }
        layout.addView(startButton)

        stopButton =
            Button(this).apply {
                text = "Stop Recording"
                isEnabled = false
            }
        layout.addView(stopButton)

        testButton =
            Button(this).apply {
                text = "Test Sensor Connections"
                isEnabled = false
            }
        layout.addView(testButton)

        setContentView(layout)
    }

    private fun setupEventHandlers() {
        initializeButton.setOnClickListener {
            initializeSensors()
        }

        startButton.setOnClickListener {
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecording()
        }

        testButton.setOnClickListener {
            testSensorConnections()
        }
    }

    private fun initializeSensors() {
        lifecycleScope.launch {
            try {
                statusText.text = "Initializing sensors..."
                Log.i(TAG, "Starting sensor initialization")

                val success = recordingController.initializeSensors()

                if (success) {
                    val summary = recordingController.getSensorStatusSummary()
                    statusText.text = "Initialization complete: ${summary.totalSensorsInitialized}/3 sensors available\n${summary.statusMessage}"

                    statusIndicator.updateWithSensorSummary(summary)

                    // Enable controls
                    startButton.isEnabled = true
                    testButton.isEnabled = true
                    initializeButton.isEnabled = false

                    Log.i(TAG, "Sensor initialization successful: ${summary.totalSensorsInitialized} sensors")
                } else {
                    statusText.text = "Sensor initialization failed - no sensors available"
                    Log.e(TAG, "All sensor initialization failed")
                }
            } catch (e: Exception) {
                statusText.text = "Initialization error: ${e.message}"
                Log.e(TAG, "Sensor initialization error", e)
            }
        }
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                statusText.text = "Starting recording..."

                // Create session directory
                val sessionDir = File(getExternalFilesDir(null), "test_session_${System.currentTimeMillis()}")

                val success = recordingController.startRecording(sessionDir.absolutePath)

                if (success) {
                    val summary = recordingController.getSensorStatusSummary()
                    statusText.text = "Recording started!\n${summary.statusMessage}"

                    statusIndicator.updateWithSensorSummary(summary)

                    // Update button states
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                    testButton.isEnabled = false

                    Log.i(TAG, "Recording started with ${summary.totalSensorsRecording} sensors")
                } else {
                    statusText.text = "Failed to start recording - no sensors available"
                    Log.e(TAG, "Recording start failed")
                }
            } catch (e: Exception) {
                statusText.text = "Recording start error: ${e.message}"
                Log.e(TAG, "Recording start error", e)
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            try {
                statusText.text = "Stopping recording..."

                val success = recordingController.stopRecording()

                if (success) {
                    val summary = recordingController.getSensorStatusSummary()
                    statusText.text = "Recording stopped.\n${summary.statusMessage}"

                    statusIndicator.updateWithSensorSummary(summary)

                    // Update button states
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                    testButton.isEnabled = true

                    Log.i(TAG, "Recording stopped successfully")
                } else {
                    statusText.text = "Warning: Some sensors may not have stopped cleanly"
                    Log.w(TAG, "Recording stop had issues")
                }
            } catch (e: Exception) {
                statusText.text = "Recording stop error: ${e.message}"
                Log.e(TAG, "Recording stop error", e)
            }
        }
    }

    private fun testSensorConnections() {
        lifecycleScope.launch {
            try {
                statusText.text = "Testing sensor connections..."

                val testResults = recordingController.testSensorConnections()

                val resultText =
                    buildString {
                        appendLine("Sensor Connection Test Results:")
                        testResults.forEach { (sensorId, success) ->
                            val status = if (success) "✅ OK" else "❌ FAILED"
                            appendLine("$sensorId: $status")
                        }
                    }

                statusText.text = resultText
                Log.i(TAG, "Sensor test complete: $testResults")
            } catch (e: Exception) {
                statusText.text = "Sensor test error: ${e.message}"
                Log.e(TAG, "Sensor test error", e)
            }
        }
    }

    private fun updateUI() {
        // Initial UI state
        statusIndicator.visibility = android.view.View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up recording controller
        lifecycleScope.launch {
            try {
                recordingController.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error", e)
            }
        }
    }
}
