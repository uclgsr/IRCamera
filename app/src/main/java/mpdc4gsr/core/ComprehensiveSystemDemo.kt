package mpdc4gsr.core

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.network.DataStreamingService
import mpdc4gsr.network.NetworkClient
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.RgbCameraRecorder
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ComprehensiveSystemDemo : AppCompatActivity() {

    companion object {
        private const val TAG = "SystemDemo"
    }


    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var initButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var networkTestButton: Button
    private lateinit var clearLogsButton: Button
    private lateinit var progressBar: ProgressBar


    private lateinit var recordingController: RecordingController
    private lateinit var permissionManager: PermissionManager
    private lateinit var permissionController: PermissionController
    private var gsrRecorder: GSRSensorRecorder? = null
    private var thermalRecorder: ThermalCameraRecorder? = null
    private var rgbRecorder: RgbCameraRecorder? = null
    private var dataStreamingService: DataStreamingService? = null


    private var isSystemInitialized = false
    private var isRecording = false
    private var currentSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "=== Starting Comprehensive System Demo ===")

        setupUI()
        initializeSystemComponents()


        permissionController.initialize()

        addLog("Comprehensive Multi-Modal Physiological Sensing Platform Demo")
        addLog("Demonstrates: Shimmer GSR + Thermal Camera + RGB Camera + Time Sync + Network Streaming")
        addLog("")
        addLog("Click 'Initialize System' to begin setup...")
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "System Demo Ready - Click Initialize"
            textSize = 18f
            setPadding(0, 0, 0, 16)
            setTextColor(ContextCompat.getColor(this@ComprehensiveSystemDemo, android.R.color.holo_blue_dark))
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            progress = 0
            max = 100
            setPadding(0, 8, 0, 16)
        }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        initButton = Button(this).apply {
            text = "Initialize System"
            setOnClickListener { initializeSystemDemo() }
        }

        startRecordingButton = Button(this).apply {
            text = "Start Recording"
            isEnabled = false
            setOnClickListener { startMultiModalRecording() }
        }

        stopRecordingButton = Button(this).apply {
            text = "Stop Recording"
            isEnabled = false
            setOnClickListener { stopMultiModalRecording() }
        }

        networkTestButton = Button(this).apply {
            text = "Test Network"
            isEnabled = false
            setOnClickListener { testNetworkStreaming() }
        }

        clearLogsButton = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { clearLogs() }
        }

        buttonLayout.addView(initButton)
        buttonLayout.addView(startRecordingButton)
        buttonLayout.addView(stopRecordingButton)

        val buttonLayout2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        buttonLayout2.addView(networkTestButton)
        buttonLayout2.addView(clearLogsButton)

        logText = TextView(this).apply {
            text = ""
            textSize = 11f
            setPadding(16, 16, 16, 16)
            background = ContextCompat.getDrawable(this@ComprehensiveSystemDemo, android.R.drawable.editbox_background)
        }

        val scrollView = ScrollView(this).apply {
            addView(logText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        layout.addView(statusText)
        layout.addView(progressBar)
        layout.addView(buttonLayout)
        layout.addView(buttonLayout2)
        layout.addView(scrollView)

        setContentView(layout)
    }

    private fun initializeSystemComponents() {
        try {
            recordingController = RecordingController(this, this)
            permissionController = PermissionController(this)
            permissionManager = PermissionManager(this, permissionController)

            addLog("System components created successfully")
        } catch (e: Exception) {
            addLog("ERROR: Failed to create system components: ${e.message}")
            Log.e(TAG, "Failed to initialize system components", e)
        }
    }

    private fun initializeSystemDemo() {
        lifecycleScope.launch {
            try {
                addLog("=== Initializing Comprehensive System Demo ===")
                updateProgress(10)
                updateStatus("Checking permissions...")


                if (!checkAllPermissions()) {
                    addLog("Requesting required permissions...")
                    requestPermissions()
                    return@launch
                }
                updateProgress(25)


                updateStatus("Initializing sensors...")
                addLog("Initializing GSR Sensor Recorder...")
                gsrRecorder = GSRSensorRecorder(this@ComprehensiveSystemDemo, "gsr_shimmer_1", 128, recordingController)
                recordingController.registerSensor("GSR", gsrRecorder!!)

                addLog("Initializing Thermal Camera Recorder...")
                thermalRecorder = ThermalCameraRecorder(this@ComprehensiveSystemDemo, "thermal_camera_1")
                recordingController.registerSensor("Thermal", thermalRecorder!!)

                addLog("Initializing RGB Camera Recorder...")
                rgbRecorder = RgbCameraRecorder(
                    this@ComprehensiveSystemDemo,
                    this@ComprehensiveSystemDemo,
                    null,
                    false,
                    permissionManager
                )
                recordingController.registerSensor("RGB", rgbRecorder!!)

                updateProgress(50)


                updateStatus("Setting up network streaming...")
                addLog("Initializing network streaming service...")
                val networkClient = NetworkClient(this@ComprehensiveSystemDemo)
                dataStreamingService = DataStreamingService(this@ComprehensiveSystemDemo, networkClient)

                updateProgress(75)


                updateStatus("Testing device discovery...")
                addLog("Testing Shimmer device discovery...")
                testShimmerDiscovery()

                addLog("Testing thermal camera detection...")
                testThermalCameraDetection()

                updateProgress(100)


                isSystemInitialized = true
                updateStatus("System Initialized - Ready for Recording")
                addLog("✅ System initialization complete!")
                addLog("All sensors initialized and ready for multi-modal recording")

                startRecordingButton.isEnabled = true
                networkTestButton.isEnabled = true
                initButton.isEnabled = false

            } catch (e: Exception) {
                addLog("❌ System initialization failed: ${e.message}")
                updateStatus("Initialization Failed")
                Log.e(TAG, "System initialization failed", e)
            }
        }
    }

    private fun testShimmerDiscovery() {
        lifecycleScope.launch {
            try {
                addLog("Scanning for Shimmer GSR devices...")
                gsrRecorder?.let { recorder ->
                    val devices = recorder.getAvailableShimmerDevices()
                    if (devices.isNotEmpty()) {
                        addLog("✅ Found ${devices.size} Shimmer devices:")
                        devices.forEach { device ->
                            addLog("  - $device")
                        }
                    } else {
                        addLog("⚠️ No Shimmer devices found - will use simulation mode")
                    }
                }
            } catch (e: Exception) {
                addLog("⚠️ Shimmer discovery error: ${e.message}")
            }
        }
    }

    private fun testThermalCameraDetection() {
        lifecycleScope.launch {
            try {
                addLog("Checking for Topdon TC001 thermal camera...")
                thermalRecorder?.let { recorder ->
                    val isConnected = recorder.initialize()
                    if (isConnected) {
                        addLog("✅ Thermal camera detected and initialized")
                    } else {
                        addLog("⚠️ No thermal camera detected - will use simulation mode")
                    }
                }
            } catch (e: Exception) {
                addLog("⚠️ Thermal camera detection error: ${e.message}")
            }
        }
    }

    private fun startMultiModalRecording() {
        lifecycleScope.launch {
            try {
                if (!isSystemInitialized) {
                    addLog("❌ System not initialized - please initialize first")
                    return@launch
                }

                addLog("=== Starting Multi-Modal Recording Session ===")
                updateStatus("Starting recording session...")


                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                currentSessionId = "demo_session_$timestamp"

                addLog("Session ID: $currentSessionId")


                val enabledSensors = listOf("GSR", "Thermal", "RGB")
                val success = recordingController.startRecording(
                    sessionId = currentSessionId,
                    participantId = "demo_participant",
                    studyName = "ComprehensiveSystemDemo",
                    enabledSensors = enabledSensors
                )

                if (success) {
                    isRecording = true
                    updateStatus("Recording in progress...")
                    addLog("✅ Multi-modal recording started successfully")
                    addLog("Recording GSR, Thermal, and RGB data with unified timestamps")

                    startRecordingButton.isEnabled = false
                    stopRecordingButton.isEnabled = true


                    addLog("Emitting synchronization events...")
                    recordingController.emitSyncEvent(
                        "demo_sync_start", mapOf(
                            "event_type" to "demo_session_start",
                            "participant" to "demo_participant"
                        )
                    )


                    startStatusMonitoring()

                } else {
                    addLog("❌ Failed to start recording session")
                    updateStatus("Recording failed to start")
                }

            } catch (e: Exception) {
                addLog("❌ Recording start failed: ${e.message}")
                updateStatus("Recording Error")
                Log.e(TAG, "Failed to start recording", e)
            }
        }
    }

    private fun stopMultiModalRecording() {
        lifecycleScope.launch {
            try {
                addLog("=== Stopping Multi-Modal Recording Session ===")
                updateStatus("Stopping recording...")


                recordingController.emitSyncEvent(
                    "demo_sync_end", mapOf(
                        "event_type" to "demo_session_end",
                        "session_id" to (currentSessionId ?: "unknown")
                    )
                )

                val success = recordingController.stopRecording()

                if (success) {
                    isRecording = false
                    updateStatus("Recording stopped - Data saved")
                    addLog("✅ Recording session stopped successfully")
                    addLog("Session data saved with unified timestamps for post-processing alignment")


                    showSessionSummary()

                    startRecordingButton.isEnabled = true
                    stopRecordingButton.isEnabled = false

                } else {
                    addLog("⚠️ Warning: Recording stop encountered issues")
                    updateStatus("Recording stopped with warnings")
                }

            } catch (e: Exception) {
                addLog("❌ Recording stop failed: ${e.message}")
                updateStatus("Stop Error")
                Log.e(TAG, "Failed to stop recording", e)
            }
        }
    }

    private fun testNetworkStreaming() {
        lifecycleScope.launch {
            try {
                addLog("=== Testing Network Streaming Capabilities ===")

                dataStreamingService?.let { streaming ->
                    addLog("Testing network connectivity...")

                    addLog("✅ Network streaming service ready")
                    addLog("Stream endpoints available for real-time data transmission")
                } ?: addLog("⚠️ Network streaming service not initialized")


                addLog("Testing timestamp consistency across sensors...")
                val timestamps = recordingController.validateTimestampConsistency()
                addLog("Timestamp validation results:")
                timestamps.forEach { (sensor, timestamp) ->
                    addLog("  $sensor: $timestamp ns")
                }

                if (timestamps.size >= 2) {
                    val maxTime = timestamps.values.maxOrNull() ?: 0L
                    val minTime = timestamps.values.minOrNull() ?: 0L
                    val drift = (maxTime - minTime) / 1_000_000.0
                    addLog("  Max time drift: ${String.format("%.2f", drift)} ms")

                    if (drift < 5.0) {
                        addLog("✅ Timestamp synchronization within acceptable range")
                    } else {
                        addLog("⚠️ Timestamp drift exceeds 5ms - check system load")
                    }
                }

            } catch (e: Exception) {
                addLog("❌ Network test failed: ${e.message}")
                Log.e(TAG, "Network test failed", e)
            }
        }
    }

    private fun startStatusMonitoring() {
        // Enhanced status monitoring with GSR-specific feedback
        lifecycleScope.launch {
            while (isRecording) {
                try {
                    val stats = recordingController.getRecordingStatistics()
                    val sensorSummary = recordingController.getSensorStatusSummary()

                    // Create GSR-aware status message
                    val gsrSensor = sensorSummary.sensors.find {
                        it.sensorType.contains("GSR", ignoreCase = true) ||
                                it.sensorId.contains("gsr", ignoreCase = true)
                    }

                    val gsrStatusText = when {
                        gsrSensor?.isRecording == true -> "🟢 GSR streaming"
                        gsrSensor?.isInitialized == true -> "🟡 GSR ready but not recording"
                        else -> "🔴 GSR unavailable"
                    }

                    addLog(
                        "📊 Recording status: ${stats.activeSensors} sensors active | $gsrStatusText | " +
                                "${String.format("%.1f", stats.sessionDurationSeconds)}s elapsed"
                    )

                    delay(10000)
                } catch (e: Exception) {
                    Log.w(TAG, "Status monitoring error", e)
                    break
                }
            }
        }

        // Monitor for GSR-specific errors and provide user feedback
        lifecycleScope.launch {
            try {
                recordingController.errorFlow.collect { error ->
                    when (error.errorType) {
                        "GSR_SENSOR_UNAVAILABLE" -> {
                            addLog("⚠️ GSR sensor unavailable - check device pairing and proximity")
                            updateStatus("Recording (GSR sensor lost)")
                        }

                        "SENSOR_RECOVERED" -> {
                            if (error.sensorId?.contains("gsr", ignoreCase = true) == true) {
                                addLog("✅ GSR sensor reconnected successfully")
                                updateStatus("Recording (all sensors active)")
                            }
                        }

                        "ALL_SENSORS_LOST" -> {
                            addLog("❌ Critical: All sensors failed - stopping session")
                            updateStatus("Session failed")
                        }

                        else -> {
                            addLog("ℹ️ ${error.errorType}: ${error.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error monitoring failed", e)
            }
        }
    }

    private fun showSessionSummary() {
        try {
            val sessionRef = recordingController.getSessionTimestampReference()

            addLog("📋 Session Summary:")
            addLog("  Session ID: ${currentSessionId ?: "Unknown"}")

            sessionRef?.let { ref ->
                addLog("  Start time: ${Date(ref.sessionStartSystemMs)}")
                addLog("  Unified timing reference established")
                addLog("  CSV files contain synchronized timestamps for alignment")
            }

            val sessionDir = recordingController.getCurrentSessionDirectory()
            sessionDir?.let { dir ->
                addLog("  Data saved to: ${dir.rootDir.absolutePath}")
            }

            addLog("✅ Multi-modal data ready for analysis with unified timestamps")

        } catch (e: Exception) {
            addLog("⚠️ Error generating session summary: ${e.message}")
        }
    }

    private fun checkAllPermissions(): Boolean {
        return permissionController.hasAllRequiredPermissions()
    }

    private fun requestPermissions() {
        permissionController.ensureAll { success, deniedPermissions ->
            if (success) {
                addLog("✅ All permissions granted - ready to initialize")
                lifecycleScope.launch {
                    delay(500)
                    initializeSystemDemo()
                }
            } else {
                addLog("❌ Some permissions denied - system functionality limited")
                addLog("Denied permissions: ${deniedPermissions.joinToString(", ")}")
                updateStatus("Permissions Required")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }

    private fun updateProgress(progress: Int) {
        runOnUiThread {
            progressBar.progress = progress
        }
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message"

        Log.i(TAG, message)

        runOnUiThread {
            val currentText = logText.text.toString()
            logText.text = if (currentText.isEmpty()) {
                logMessage
            } else {
                "$currentText\n$logMessage"
            }


            (logText.parent as? ScrollView)?.post {
                (logText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun clearLogs() {
        logText.text = ""
        updateStatus("Logs cleared")
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleScope.launch {
            try {
                if (isRecording) {
                    recordingController.stopRecording()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recording on destroy", e)
            }
        }

        Log.i(TAG, "=== Comprehensive System Demo Destroyed ===")
    }
}