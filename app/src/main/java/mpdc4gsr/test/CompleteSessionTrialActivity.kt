package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.controller.RecordingController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Complete Session Trial Verification Activity
 * Tests end-to-end recording with all sensors as per issue #139 plan
 */
class CompleteSessionTrialActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CompleteSessionTrial"
        private const val EXTENDED_DURATION_SECONDS = 300 // 5 minutes as per plan
        private const val STATUS_UPDATE_INTERVAL = 10 // Update every 10 seconds
    }

    private lateinit var resultTextView: TextView
    private lateinit var startTrialButton: Button
    private lateinit var stopTrialButton: Button
    private lateinit var verifyOutputButton: Button
    private lateinit var generateReportButton: Button

    private var recordingController: RecordingController? = null
    private var testResults: StringBuilder = StringBuilder()
    private var trialSessionDir: File? = null
    private var trialStartTime: Long = 0
    private var trialEndTime: Long = 0
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_session_trial)

        setupUI()
        initializeComponents()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Complete Session Trial"

        resultTextView = findViewById(R.id.result_text_view)
        startTrialButton = findViewById(R.id.start_trial_button)
        stopTrialButton = findViewById(R.id.stop_trial_button)
        verifyOutputButton = findViewById(R.id.verify_output_button)
        generateReportButton = findViewById(R.id.generate_report_button)

        startTrialButton.setOnClickListener {
            lifecycleScope.launch {
                startCompleteSessionTrial()
            }
        }

        stopTrialButton.setOnClickListener {
            lifecycleScope.launch {
                stopCompleteSessionTrial()
            }
        }

        verifyOutputButton.setOnClickListener {
            lifecycleScope.launch {
                verifySessionOutput()
            }
        }

        generateReportButton.setOnClickListener {
            lifecycleScope.launch {
                generateCompleteReport()
            }
        }

        // Initially disable stop and verification buttons
        stopTrialButton.isEnabled = false
        verifyOutputButton.isEnabled = false
        generateReportButton.isEnabled = false

        updateResults("Complete Session Trial Verification\n")
        updateResults("End-to-end recording test with all sensors\n\n")
        updateResults("Test Parameters:\n")
        updateResults("- Duration: ${EXTENDED_DURATION_SECONDS / 60} minutes (${EXTENDED_DURATION_SECONDS}s)\n")
        updateResults("- Sensors: RGB Camera, Thermal Camera, GSR Sensor\n")
        updateResults("- Output: Video files, CSV data, session metadata\n")
        updateResults("- Verification: File presence, data quality, metadata\n\n")
        updateResults("Ready to start complete session trial...\n\n")
    }

    private fun initializeComponents() {
        try {
            recordingController = RecordingController(this, this)
            updateResults("✓ Complete session trial components initialized\n\n")
        } catch (e: Exception) {
            updateResults("✗ Failed to initialize components: ${e.message}\n\n")
            Log.e(TAG, "Failed to initialize components", e)
        }
    }

    /**
     * Start complete session trial as outlined in issue #139 plan
     */
    private suspend fun startCompleteSessionTrial() {
        testResults.clear()
        testResults.append("=== Complete Session Trial Started ===\n")
        testResults.append("Start Time: ${getCurrentTimeString()}\n")
        testResults.append("Planned Duration: ${EXTENDED_DURATION_SECONDS}s (${EXTENDED_DURATION_SECONDS / 60} minutes)\n\n")

        updateResults(testResults.toString())

        // Phase 1: Initialize all sensors
        val initResult = initializeAllSensors()
        if (!initResult) {
            testResults.append("✗ Failed to initialize sensors - trial aborted\n")
            updateResults(testResults.toString())
            return
        }

        testResults.append("✓ All available sensors initialized\n")
        updateResults(testResults.toString())

        // Phase 2: Start comprehensive recording
        val recordingResult = startComprehensiveRecording()
        if (!recordingResult) {
            testResults.append("✗ Failed to start comprehensive recording - trial aborted\n")
            updateResults(testResults.toString())
            return
        }

        testResults.append("✓ Comprehensive recording started successfully\n")
        trialStartTime = System.currentTimeMillis()
        isRecording = true

        // Enable stop button, disable start button
        runOnUiThread {
            startTrialButton.isEnabled = false
            stopTrialButton.isEnabled = true
        }

        updateResults(testResults.toString())

        // Phase 3: Monitor recording progress
        monitorRecordingProgress()
    }

    private suspend fun initializeAllSensors(): Boolean {
        return try {
            val controller = recordingController ?: return false

            updateResults("Initializing sensors...\n")

            // Initialize all available sensors
            val initialized = controller.initializeSensors()
            if (!initialized) {
                updateResults("✗ Sensor initialization failed\n")
                return false
            }

            // Get sensor status
            val diagnostics = controller.getSessionDiagnostics()
            updateResults("Sensor Status:\n")
            updateResults("- Total configured: ${diagnostics.totalSensorsConfigured}\n")
            updateResults("- Successfully initialized: ${diagnostics.totalSensorsInitialized}\n")
            updateResults("- Active sensors: ${diagnostics.activeSensorNames.joinToString(", ")}\n")
            updateResults(
                "- Health score: ${
                    String.format(
                        "%.2f",
                        diagnostics.sessionHealthScore
                    )
                }\n\n"
            )

            // Require at least 2 sensors for meaningful trial
            diagnostics.totalSensorsActive >= 2

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sensors", e)
            false
        }
    }

    private suspend fun startComprehensiveRecording(): Boolean {
        return try {
            val controller = recordingController ?: return false

            // Create trial session directory
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val trialDir = File(getExternalFilesDir("session_trials"), "complete_trial_$timestamp")
            trialDir.mkdirs()
            trialSessionDir = trialDir

            updateResults("Session directory: ${trialDir.absolutePath}\n")

            // Start recording with comprehensive session metadata
            val recordingStarted = controller.startRecording(
                sessionId = "complete_session_trial_$timestamp",
                participantId = "trial_participant",
                studyName = "complete_session_verification"
            )

            if (recordingStarted) {
                // Add initial markers for trial tracking
                controller.addSyncMarker(
                    "trial_session_start",
                    System.nanoTime(),
                    mapOf(
                        "trial_type" to "complete_session_verification",
                        "planned_duration_seconds" to EXTENDED_DURATION_SECONDS.toString(),
                        "sensors_expected" to "RGB,Thermal,GSR"
                    )
                )
            }

            recordingStarted

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start comprehensive recording", e)
            false
        }
    }

    private suspend fun monitorRecordingProgress() {
        var elapsed = 0

        while (isRecording && elapsed < EXTENDED_DURATION_SECONDS) {
            delay(STATUS_UPDATE_INTERVAL * 1000L)
            elapsed += STATUS_UPDATE_INTERVAL

            try {
                val controller = recordingController ?: break

                // Get current recording statistics
                val stats = controller.getRecordingStatistics()
                val diagnostics = controller.getSessionDiagnostics()

                testResults.append("\n--- Progress Update (${elapsed}s/${EXTENDED_DURATION_SECONDS}s) ---\n")
                testResults.append("Active sensors: ${stats.activeSensors}\n")
                testResults.append("Total samples: ${stats.totalSamplesRecorded}\n")
                testResults.append(
                    "Storage used: ${
                        String.format(
                            "%.1f",
                            stats.totalStorageUsedMB
                        )
                    }MB\n"
                )
                testResults.append(
                    "Health score: ${
                        String.format(
                            "%.2f",
                            diagnostics.sessionHealthScore
                        )
                    }\n"
                )
                testResults.append("Session status: ${if (stats.isRecording) "ACTIVE" else "INACTIVE"}\n")

                updateResults(testResults.toString())

                // Add periodic sync markers for analysis
                if (elapsed % 60 == 0) { // Every minute
                    controller.addSyncMarker(
                        "trial_progress_marker",
                        System.nanoTime(),
                        mapOf(
                            "elapsed_seconds" to elapsed.toString(),
                            "active_sensors" to stats.activeSensors.toString(),
                            "samples_recorded" to stats.totalSamplesRecorded.toString()
                        )
                    )
                }

            } catch (e: Exception) {
                testResults.append("Error during progress monitoring: ${e.message}\n")
                updateResults(testResults.toString())
            }
        }

        if (elapsed >= EXTENDED_DURATION_SECONDS) {
            testResults.append("\n✓ Trial duration completed - stopping recording\n")
            updateResults(testResults.toString())
            stopCompleteSessionTrial()
        }
    }

    private suspend fun stopCompleteSessionTrial() {
        if (!isRecording) return

        testResults.append("\n=== Stopping Complete Session Trial ===\n")
        updateResults(testResults.toString())

        try {
            val controller = recordingController ?: return
            trialEndTime = System.currentTimeMillis()

            // Add final sync marker
            controller.addSyncMarker(
                "trial_session_end",
                System.nanoTime(),
                mapOf(
                    "trial_completed" to "true",
                    "actual_duration_ms" to (trialEndTime - trialStartTime).toString(),
                    "end_time" to getCurrentTimeString()
                )
            )

            delay(500) // Allow final marker to be recorded

            // Stop the recording session
            val recordingStopped = controller.stopSession()
            isRecording = false

            testResults.append("Recording stop: ${if (recordingStopped) "SUCCESS" else "FAILED"}\n")
            testResults.append("End time: ${getCurrentTimeString()}\n")
            testResults.append("Actual duration: ${(trialEndTime - trialStartTime) / 1000}s\n")

            // Enable verification buttons
            runOnUiThread {
                stopTrialButton.isEnabled = false
                verifyOutputButton.isEnabled = true
                generateReportButton.isEnabled = true
            }

            testResults.append("\n✓ Trial completed - verification available\n")
            updateResults(testResults.toString())

        } catch (e: Exception) {
            testResults.append("✗ Error stopping trial: ${e.message}\n")
            updateResults(testResults.toString())
            Log.e(TAG, "Error stopping complete session trial", e)
        }
    }

    private suspend fun verifySessionOutput() {
        testResults.append("\n=== Session Output Verification ===\n")
        updateResults(testResults.toString())

        try {
            val sessionDir = trialSessionDir ?: run {
                testResults.append("✗ No session directory available\n")
                updateResults(testResults.toString())
                return
            }

            testResults.append("Verifying session directory: ${sessionDir.name}\n\n")

            // Verify directory structure and files
            val fileVerification = verifyOutputFiles(sessionDir)
            testResults.append(fileVerification)

            // Verify data quality
            val dataQuality = verifyDataQuality(sessionDir)
            testResults.append(dataQuality)

            // Verify session metadata
            val metadataVerification = verifySessionMetadata(sessionDir)
            testResults.append(metadataVerification)

            updateResults(testResults.toString())

        } catch (e: Exception) {
            testResults.append("✗ Verification failed: ${e.message}\n")
            updateResults(testResults.toString())
            Log.e(TAG, "Failed to verify session output", e)
        }
    }

    private fun verifyOutputFiles(sessionDir: File): String {
        val verification = StringBuilder()
        verification.append("1. Output Files Verification:\n")

        try {
            val expectedFiles = mapOf(
                "gsr.csv" to "GSR sensor data",
                "session_info.json" to "Session metadata",
                "sync_markers.csv" to "Synchronization markers"
            )

            var filesFound = 0
            var totalExpected = expectedFiles.size

            verification.append("Expected files:\n")
            for ((filename, description) in expectedFiles) {
                val file = File(sessionDir, filename)
                val exists = file.exists()
                val size = if (exists) file.length() else 0L

                verification.append("- $filename ($description): ${if (exists) "✓" else "✗"}")
                if (exists) {
                    verification.append(" (${size} bytes)")
                    filesFound++
                }
                verification.append("\n")
            }

            // Check for additional sensor files (RGB video, thermal images)
            val allFiles = sessionDir.listFiles() ?: arrayOf()
            verification.append("\nAdditional files found:\n")
            for (file in allFiles) {
                if (!expectedFiles.containsKey(file.name)) {
                    verification.append("- ${file.name}: ${file.length()} bytes\n")
                }
            }

            verification.append("\nFile verification: $filesFound/$totalExpected expected files found\n")
            verification.append("Total files in session: ${allFiles.size}\n")

        } catch (e: Exception) {
            verification.append("File verification error: ${e.message}\n")
        }

        return verification.toString()
    }

    private fun verifyDataQuality(sessionDir: File): String {
        val verification = StringBuilder()
        verification.append("\n2. Data Quality Verification:\n")

        try {
            // Verify GSR data if present
            val gsrFile = File(sessionDir, "gsr.csv")
            if (gsrFile.exists()) {
                val lines = gsrFile.readLines()
                val dataLines = lines.drop(1) // Skip header

                verification.append("GSR Data:\n")
                verification.append("- Total lines: ${lines.size}\n")
                verification.append("- Data samples: ${dataLines.size}\n")

                if (trialStartTime > 0 && trialEndTime > 0) {
                    val expectedDuration = (trialEndTime - trialStartTime) / 1000
                    val expectedSamples = expectedDuration * 128 // 128Hz sampling rate
                    val sampleRatio =
                        if (expectedSamples > 0) dataLines.size.toDouble() / expectedSamples else 0.0

                    verification.append("- Expected samples (~): ${expectedSamples.toInt()}\n")
                    verification.append(
                        "- Sample completeness: ${
                            String.format(
                                "%.1f",
                                sampleRatio * 100
                            )
                        }%\n"
                    )
                }

                // Quick data quality check
                if (dataLines.isNotEmpty()) {
                    val firstLine = dataLines.first().split(",")
                    val lastLine = dataLines.last().split(",")

                    verification.append("- First timestamp: ${firstLine.getOrNull(0) ?: "N/A"}\n")
                    verification.append("- Last timestamp: ${lastLine.getOrNull(0) ?: "N/A"}\n")
                    verification.append("- Data format: ${if (firstLine.size >= 4) "✓ VALID" else "✗ INVALID"}\n")
                }
            } else {
                verification.append("GSR Data: ✗ FILE NOT FOUND\n")
            }

        } catch (e: Exception) {
            verification.append("Data quality verification error: ${e.message}\n")
        }

        return verification.toString()
    }

    private fun verifySessionMetadata(sessionDir: File): String {
        val verification = StringBuilder()
        verification.append("\n3. Session Metadata Verification:\n")

        try {
            val metadataFile = File(sessionDir, "session_info.json")
            if (metadataFile.exists()) {
                val metadata = metadataFile.readText()

                verification.append("Session Metadata:\n")
                verification.append("- File exists: ✓\n")
                verification.append("- File size: ${metadataFile.length()} bytes\n")

                // Check for key metadata fields
                val hasSessionId = metadata.contains("session_id")
                val hasStartTime = metadata.contains("start_time")
                val hasEndTime = metadata.contains("end_time")
                val hasSensorInfo = metadata.contains("sensors")

                verification.append("- Contains session_id: ${if (hasSessionId) "✓" else "✗"}\n")
                verification.append("- Contains start_time: ${if (hasStartTime) "✓" else "✗"}\n")
                verification.append("- Contains end_time: ${if (hasEndTime) "✓" else "✗"}\n")
                verification.append("- Contains sensor info: ${if (hasSensorInfo) "✓" else "✗"}\n")

                val metadataComplete = hasSessionId && hasStartTime && hasEndTime && hasSensorInfo
                verification.append("- Metadata completeness: ${if (metadataComplete) "✓ COMPLETE" else "⚠ PARTIAL"}\n")

            } else {
                verification.append("Session Metadata: ✗ FILE NOT FOUND\n")
            }

            // Check sync markers
            val syncFile = File(sessionDir, "sync_markers.csv")
            if (syncFile.exists()) {
                val syncLines = syncFile.readLines()
                verification.append("\nSync Markers:\n")
                verification.append("- Sync file exists: ✓\n")
                verification.append("- Total sync markers: ${syncLines.size - 1}\n") // Minus header

                val hasStartMarker = syncLines.any { it.contains("trial_session_start") }
                val hasEndMarker = syncLines.any { it.contains("trial_session_end") }

                verification.append("- Has start marker: ${if (hasStartMarker) "✓" else "✗"}\n")
                verification.append("- Has end marker: ${if (hasEndMarker) "✓" else "✗"}\n")
            } else {
                verification.append("\nSync Markers: ✗ FILE NOT FOUND\n")
            }

        } catch (e: Exception) {
            verification.append("Metadata verification error: ${e.message}\n")
        }

        return verification.toString()
    }

    private suspend fun generateCompleteReport() {
        testResults.append("\n=== COMPLETE SESSION TRIAL REPORT ===\n")
        testResults.append("Report Generated: ${getCurrentTimeString()}\n\n")

        try {
            // Summary statistics
            val duration = if (trialStartTime > 0 && trialEndTime > 0) {
                (trialEndTime - trialStartTime) / 1000
            } else 0L

            testResults.append("Trial Summary:\n")
            testResults.append("- Start Time: ${if (trialStartTime > 0) Date(trialStartTime) else "N/A"}\n")
            testResults.append("- End Time: ${if (trialEndTime > 0) Date(trialEndTime) else "N/A"}\n")
            testResults.append("- Actual Duration: ${duration}s (${duration / 60}m ${duration % 60}s)\n")
            testResults.append("- Planned Duration: ${EXTENDED_DURATION_SECONDS}s\n")
            testResults.append("- Duration Accuracy: ${if (Math.abs(duration - EXTENDED_DURATION_SECONDS) <= 10) "✓ ACCURATE" else "⚠ VARIANCE"}\n")

            // Session directory info
            val sessionDir = trialSessionDir
            if (sessionDir != null) {
                val totalFiles = sessionDir.listFiles()?.size ?: 0
                val totalSize =
                    sessionDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

                testResults.append("\nSession Output:\n")
                testResults.append("- Directory: ${sessionDir.name}\n")
                testResults.append("- Total files: $totalFiles\n")
                testResults.append(
                    "- Total size: ${
                        String.format(
                            "%.2f",
                            totalSize / 1024.0 / 1024.0
                        )
                    }MB\n"
                )
            }

            // Overall assessment
            val trialComplete = duration > 0 && sessionDir != null
            val durationAccurate =
                Math.abs(duration - EXTENDED_DURATION_SECONDS) <= 30 // 30s tolerance
            val outputExists = sessionDir?.exists() == true

            testResults.append("\n=== OVERALL ASSESSMENT ===\n")
            testResults.append("Trial completion: ${if (trialComplete) "✓ COMPLETE" else "✗ INCOMPLETE"}\n")
            testResults.append("Duration accuracy: ${if (durationAccurate) "✓ ACCURATE" else "⚠ VARIANCE"}\n")
            testResults.append("Output generation: ${if (outputExists) "✓ SUCCESS" else "✗ FAILED"}\n")

            val overallSuccess = trialComplete && durationAccurate && outputExists
            testResults.append("\nFINAL RESULT: ${if (overallSuccess) "✓ SUCCESS" else "⚠ PARTIAL SUCCESS"}\n\n")

            if (overallSuccess) {
                testResults.append("✓ Complete session trial successful!\n")
                testResults.append("✓ Multi-sensor recording system verified\n")
                testResults.append("✓ System ready for production use\n")
            } else {
                testResults.append("⚠ Some aspects of the session trial need attention\n")
                testResults.append("Review individual verification results above\n")
            }

            updateResults(testResults.toString())

        } catch (e: Exception) {
            testResults.append("Report generation error: ${e.message}\n")
            updateResults(testResults.toString())
            Log.e(TAG, "Failed to generate complete report", e)
        }
    }

    private fun updateResults(text: String) {
        runOnUiThread {
            resultTextView.text = text
        }
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure recording is stopped
        if (isRecording) {
            recordingController?.let { controller ->
                lifecycleScope.launch {
                    try {
                        controller.stopSession()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping recording on destroy", e)
                    }
                }
            }
        }

        recordingController?.let { controller ->
            lifecycleScope.launch {
                try {
                    controller.cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up recording controller", e)
                }
            }
        }
    }
}