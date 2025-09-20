package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.service.RecordingService
import kotlinx.coroutines.launch
import java.io.File


class FaultTolerantSessionDemoActivity : ComponentActivity() {
    companion object {
        private const val TAG = "FaultTolerantDemo"
    }

    private lateinit var recordingController: RecordingController
    private lateinit var statusText: TextView
    private lateinit var diagnosticsText: TextView
    private lateinit var scrollView: ScrollView


    private lateinit var initButton: Button
    private lateinit var startSessionButton: Button
    private lateinit var stopSessionButton: Button
    private lateinit var validateStateButton: Button
    private lateinit var showDiagnosticsButton: Button
    private lateinit var showStatusReportButton: Button
    private lateinit var simulateFailureButton: Button
    private lateinit var startServiceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createDemoUI()

        recordingController = RecordingController(this, this)

        setupEventHandlers()
        updateUI()
    }

    private fun createDemoUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleText = TextView(this).apply {
            text = "Fault-Tolerant Session Management Demo"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }

        statusText = TextView(this).apply {
            text = "Status: Not initialized"
            textSize = 14f
            setBackgroundColor(0xFFE0E0E0.toInt())
            setPadding(16, 16, 16, 16)
        }


        initButton = Button(this).apply { text = "Initialize Sensors" }
        startSessionButton = Button(this).apply {
            text = "Start Session (Fault Tolerant)"
            isEnabled = false
        }
        stopSessionButton = Button(this).apply {
            text = "Stop Session"
            isEnabled = false
        }
        validateStateButton = Button(this).apply { text = "Validate Session State" }
        showDiagnosticsButton = Button(this).apply { text = "Show Diagnostics" }
        showStatusReportButton = Button(this).apply { text = "Show Status Report" }
        simulateFailureButton = Button(this).apply {
            text = "Simulate Sensor Failure"
            isEnabled = false
        }
        startServiceButton = Button(this).apply { text = "Start Recording Service" }


        diagnosticsText = TextView(this).apply {
            text = "Diagnostics will appear here..."
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(0xFFF0F0F0.toInt())
            setPadding(16, 16, 16, 16)
        }

        scrollView = ScrollView(this).apply {
            addView(diagnosticsText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(initButton)
        layout.addView(startSessionButton)
        layout.addView(stopSessionButton)
        layout.addView(validateStateButton)
        layout.addView(showDiagnosticsButton)
        layout.addView(showStatusReportButton)
        layout.addView(simulateFailureButton)
        layout.addView(startServiceButton)
        layout.addView(scrollView)

        setContentView(layout)
    }

    private fun setupEventHandlers() {
        initButton.setOnClickListener {
            lifecycleScope.launch {
                initializeSensors()
            }
        }

        startSessionButton.setOnClickListener {
            lifecycleScope.launch {
                startFaultTolerantSession()
            }
        }

        stopSessionButton.setOnClickListener {
            lifecycleScope.launch {
                stopSession()
            }
        }

        validateStateButton.setOnClickListener {
            validateSessionState()
        }

        showDiagnosticsButton.setOnClickListener {
            showSessionDiagnostics()
        }

        showStatusReportButton.setOnClickListener {
            showStatusReport()
        }

        simulateFailureButton.setOnClickListener {
            simulateSensorFailure()
        }

        startServiceButton.setOnClickListener {
            startRecordingService()
        }
    }

    private suspend fun initializeSensors() {
        try {
            updateStatus("Initializing sensors with fault tolerance...")
            Log.i(TAG, "Starting sensor initialization")

            val success = recordingController.initializeSensors()

            if (success) {
                val availableSensors = recordingController.getAvailableSensors()
                updateStatus("✓ Initialization complete: ${availableSensors.size} sensors available")
                Log.i(TAG, "Sensors initialized: ${availableSensors.map { it.sensorId }}")

                startSessionButton.isEnabled = true
                simulateFailureButton.isEnabled = false

                showMessage(
                    "Fault-tolerant initialization completed!\n" +
                            "Available sensors: ${availableSensors.joinToString(", ") { it.sensorId }}\n" +
                            "The system can now start sessions even if some sensors fail."
                )
            } else {
                updateStatus("✗ Initialization failed: No sensors available")
                Log.e(TAG, "Sensor initialization failed completely")

                showMessage(
                    "Initialization failed - no sensors are available.\n" +
                            "This may indicate hardware or permission issues."
                )
            }
        } catch (e: Exception) {
            updateStatus("✗ Initialization error: ${e.message}")
            Log.e(TAG, "Error during initialization", e)
        }
    }

    private suspend fun startFaultTolerantSession() {
        try {
            updateStatus("Starting session with fault tolerance...")

            val sessionDir = File(
                getExternalFilesDir(null),
                "demo_sessions/session_${System.currentTimeMillis()}"
            )
            sessionDir.mkdirs()

            Log.i(TAG, "Starting session: ${sessionDir.absolutePath}")

            val success = recordingController.startRecording(sessionDir.absolutePath)

            if (success) {
                val activeSensors = recordingController.getActiveSensorCount()
                val totalSensors = recordingController.getAvailableSensors().size

                updateStatus("✓ Session started: $activeSensors/$totalSensors sensors active")
                Log.i(TAG, "Session started successfully with partial sensors")

                startSessionButton.isEnabled = false
                stopSessionButton.isEnabled = true
                simulateFailureButton.isEnabled = true

                showMessage(
                    "Fault-tolerant session started!\n" +
                            "Active sensors: $activeSensors out of $totalSensors\n" +
                            "Session will continue even if individual sensors fail."
                )
            } else {
                updateStatus("✗ Session failed: No sensors could start")
                Log.e(TAG, "Session start failed - all sensors failed")

                showMessage(
                    "Session start failed.\n" +
                            "All sensors failed to start. Check sensor connections."
                )
            }
        } catch (e: Exception) {
            updateStatus("✗ Session start error: ${e.message}")
            Log.e(TAG, "Error starting session", e)
        }
    }

    private suspend fun stopSession() {
        try {
            updateStatus("Stopping session...")

            val success = recordingController.stopSession()

            if (success) {
                updateStatus("✓ Session stopped successfully")
                Log.i(TAG, "Session stopped with smart cleanup")

                startSessionButton.isEnabled = true
                stopSessionButton.isEnabled = false
                simulateFailureButton.isEnabled = false

                showMessage(
                    "Session stopped successfully!\n" +
                            "Smart cleanup ensured only active sensors were stopped."
                )
            } else {
                updateStatus("⚠ Session stop completed with issues")
                Log.w(TAG, "Session stop had some issues")
            }
        } catch (e: Exception) {
            updateStatus("✗ Session stop error: ${e.message}")
            Log.e(TAG, "Error stopping session", e)
        }
    }

    private fun validateSessionState() {
        val validation = recordingController.validateSessionState()

        val message = buildString {
            appendLine("Session State Validation:")
            appendLine("Status: ${validation.summary}")
            appendLine("Valid: ${validation.isValid}")

            if (validation.hasIssues) {
                appendLine("\nIssues:")
                validation.issues.forEach { appendLine("• $it") }
            }

            if (validation.hasWarnings) {
                appendLine("\nWarnings:")
                validation.warnings.forEach { appendLine("• $it") }
            }

            appendLine(
                "\nChecked at: ${
                    java.text.SimpleDateFormat("HH:mm:ss").format(validation.checkedAt)
                }"
            )
        }

        showMessage(message)
        Log.i(TAG, "Session validation: ${validation.summary}")
    }

    private fun showSessionDiagnostics() {
        val diagnostics = recordingController.getSessionDiagnostics()

        val message = buildString {
            appendLine("=== Session Diagnostics ===")
            appendLine("Status: ${diagnostics.statusSummary}")
            appendLine(
                "Health Score: ${
                    String.format(
                        "%.1f",
                        diagnostics.sessionHealthScore * 100
                    )
                }%"
            )
            appendLine("Recording: ${diagnostics.isRecording}")
            appendLine("State: ${diagnostics.sessionState}")
            appendLine("Duration: ${diagnostics.sessionDurationMs}ms")
            appendLine()
            appendLine("Sensors:")
            appendLine("  Configured: ${diagnostics.totalSensorsConfigured}")
            appendLine("  Initialized: ${diagnostics.totalSensorsInitialized}")
            appendLine("  Active: ${diagnostics.totalSensorsActive}")
            appendLine("  Available: ${diagnostics.availableSensorNames.joinToString(", ")}")
            appendLine("  Active: ${diagnostics.activeSensorNames.joinToString(", ")}")
            appendLine()
            appendLine("Fault Tolerance:")
            appendLine("  Partial Start: ${if (diagnostics.partialStartCapable) "✓" else "✗"}")
            appendLine("  Mid-Session Recovery: ${if (diagnostics.midSessionRecoveryEnabled) "✓" else "✗"}")
            appendLine("  Smart Cleanup: ${if (diagnostics.smartCleanupEnabled) "✓" else "✗"}")
            appendLine()
            appendLine("Session Directory: ${diagnostics.sessionDirectory ?: "None"}")
            if (diagnostics.sessionStartTimestamp > 0) {
                appendLine("Start Timestamp: ${diagnostics.sessionStartTimestamp}ms")
                appendLine("Reference Timestamp: ${diagnostics.referenceTimestampNs}ns")
            }
        }

        diagnosticsText.text = message
        Log.i(TAG, "Diagnostics displayed")
    }

    private fun showStatusReport() {
        val report = recordingController.getStatusReport()
        diagnosticsText.text = report
        Log.i(TAG, "Status report displayed")
    }

    private fun simulateSensorFailure() {
        showMessage(
            "Sensor failure simulation would demonstrate:\n" +
                    "• Mid-session error handling\n" +
                    "• Session continuation with remaining sensors\n" +
                    "• Error recovery attempts\n" +
                    "• Graceful degradation\n\n" +
                    "In a real implementation, this would trigger sensor disconnection."
        )
        Log.i(TAG, "Sensor failure simulation requested")
    }

    private fun startRecordingService() {
        try {
            val sessionDir = File(
                getExternalFilesDir(null),
                "service_sessions/service_${System.currentTimeMillis()}"
            )
            sessionDir.mkdirs()

            RecordingService.startRecording(this, sessionDir.absolutePath)

            updateStatus("Recording service started")
            showMessage(
                "Recording service started with fault-tolerant session management.\n" +
                        "The service will handle sensor failures gracefully."
            )
            Log.i(TAG, "Recording service started")
        } catch (e: Exception) {
            updateStatus("Service start failed: ${e.message}")
            Log.e(TAG, "Failed to start recording service", e)
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = "Status: $status"
        }
    }

    private fun updateUI() {

        startSessionButton.isEnabled = false
        stopSessionButton.isEnabled = false
        simulateFailureButton.isEnabled = false
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
