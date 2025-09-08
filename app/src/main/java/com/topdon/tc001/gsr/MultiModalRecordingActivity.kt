package com.topdon.tc001.gsr

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.service.SessionManager
import com.topdon.gsr.util.TimeUtil
import com.topdon.tc001.camera.RGBCameraRecorder
import kotlinx.coroutines.launch

// Note: EnhancedRecordingService is referenced with full package name since it's in a different module

/**
 * Full multi-modal recording interface with GSR and thermal coordination
 * Navigation: Use NavigationManager.getInstance().build(RouterConfig.GSR_MULTI_MODAL).navigation(context)
 */
class MultiModalRecordingActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MultiModalActivity"
        private const val REQUEST_PERMISSIONS = 100

        private val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                // Android 12+ Bluetooth permissions for Shimmer3 GSR devices
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )

        fun start(context: Context) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java)
            context.startActivity(intent)
        }

        fun startWithTemplate(
            context: Context,
            templateId: String,
        ) {
            val intent =
                Intent(context, MultiModalRecordingActivity::class.java).apply {
                    putExtra("template_id", templateId)
                }
            context.startActivity(intent)
        }
    }

    // UI Components
    private lateinit var sessionIdEdit: EditText
    private lateinit var participantIdEdit: EditText
    private lateinit var studyNameEdit: EditText
    private lateinit var startStopButton: Button
    private lateinit var syncEventButton: Button
    private lateinit var statusText: TextView
    private lateinit var sampleCountText: TextView
    private lateinit var syncCountText: TextView
    private lateinit var sessionDurationText: TextView
    private lateinit var fileLocationText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cameraPreview: android.view.TextureView
    
    // Network status UI components
    private lateinit var networkStatusText: TextView
    private lateinit var discoveredDevicesText: TextView
    private lateinit var streamingQueueText: TextView
    private lateinit var networkMetricsText: TextView
    private lateinit var connectToDeviceButton: Button
    private lateinit var startDiscoveryButton: Button

    // Recording options
    private lateinit var enableVideoSwitch: Switch
    private lateinit var enable4KSwitch: Switch
    private lateinit var enableRawCaptureSwitch: Switch
    private lateinit var rawFrameRateSpinner: Spinner

    // Recording components
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var rgbCameraRecorder: RGBCameraRecorder? = null
    private var networkClient: com.topdon.gsr.network.NetworkClient? = null
    private var isRecording = false
    private var isStartingRecording = false // Guard against double taps
    private var currentSession: SessionInfo? = null
    private var sampleCount = 0L
    private var syncMarkCount = 0
    
    // Enhanced service integration
    private var enhancedRecordingService: com.topdon.gsr.service.EnhancedRecordingService? = null
    private var isServiceBound = false
    private var discoveredDevices = mutableListOf<com.topdon.gsr.network.NetworkClient.ControllerInfo>()
    
    // UI update timer
    private var uiUpdateJob: kotlinx.coroutines.Job? = null

    // Service connection for enhanced recording service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? com.topdon.gsr.service.EnhancedRecordingService.EnhancedRecordingBinder
            enhancedRecordingService = binder?.getService()
            isServiceBound = true
            Log.i(TAG, "Enhanced recording service connected")
            updateNetworkStatusUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            enhancedRecordingService = null
            isServiceBound = false
            Log.i(TAG, "Enhanced recording service disconnected")
            updateNetworkStatusUI()
        }
    }

    private val gsrListener =
        object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                runOnUiThread {
                    isRecording = true
                    currentSession = sessionInfo
                    updateUI()
                    statusText.text = "Recording GSR data at 128 Hz..."
                    progressBar.visibility = View.VISIBLE

                    val sessionDir = gsrRecorder.getSessionDirectory()?.absolutePath ?: "Unknown"
                    fileLocationText.text = "Files: $sessionDir"
                }
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                runOnUiThread {
                    isRecording = false
                    currentSession = null
                    updateUI()
                    statusText.text = "Recording completed. ${sessionInfo.sampleCount} samples recorded."
                    progressBar.visibility = View.GONE

                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "Recording saved: ${sessionInfo.sessionId}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }

            override fun onSampleRecorded(sample: GSRSample) {
                sampleCount = sample.sampleIndex

                // Send data to PC Controller if connected
                networkClient?.let { client ->
                    if (client.isConnected()) {
                        currentSession?.let { session ->
                            lifecycleScope.launch {
                                val data =
                                    org.json.JSONObject().apply {
                                        put("gsr_conductance", sample.conductance)
                                        put("gsr_resistance", sample.resistance)
                                        put("raw_value", sample.rawValue)
                                        put("timestamp", sample.timestamp)
                                        put("sample_index", sample.sampleIndex)
                                    }
                                client.sendMeasurementData(session.sessionId, data)
                            }
                        }
                    }
                }

                // Update UI every second (128 samples)
                if (sampleCount % 128 == 0L) {
                    runOnUiThread {
                        sampleCountText.text = "Samples: $sampleCount"
                        currentSession?.let { session ->
                            val duration = (System.currentTimeMillis() - session.startTime) / 1000
                            sessionDurationText.text = "Duration: ${duration}s"
                        }
                    }
                }
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                syncMarkCount++
                runOnUiThread {
                    syncCountText.text = "Sync Events: $syncMarkCount"
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "Sync: ${syncMark.eventType}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    statusText.text = "Error: $error"
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "GSR Error: $error",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

    // Service connection for EnhancedRecordingService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? com.topdon.gsr.service.EnhancedRecordingService.LocalBinder
            enhancedRecordingService = binder?.getService()
            isServiceBound = true
            Log.d(TAG, "Enhanced recording service connected")
            updateNetworkStatusUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            enhancedRecordingService = null
            isServiceBound = false
            Log.d(TAG, "Enhanced recording service disconnected")
            updateNetworkStatusUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple layout programmatically since we don't have access to layout files
        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

        // Title
        val title =
            TextView(this).apply {
                text = "Multi-Modal Physiological Recording"
                textSize = 20f
                setPadding(0, 0, 0, 16)
            }
        layout.addView(title)

        // Camera preview section
        val cameraSection =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 16)
            }

        val cameraTitle =
            TextView(this).apply {
                text = "RGB Camera Preview"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        cameraSection.addView(cameraTitle)

        cameraPreview =
            android.view.TextureView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        400,
                    )
            }
        cameraSection.addView(cameraPreview)
        layout.addView(cameraSection)

        // Recording options section
        val optionsSection =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 16)
            }

        val optionsTitle =
            TextView(this).apply {
                text = "Recording Options"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        optionsSection.addView(optionsTitle)

        // Video recording toggle
        val videoLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        videoLayout.addView(
            TextView(this).apply {
                text = "Record Video: "
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        enableVideoSwitch = Switch(this).apply { isChecked = true }
        videoLayout.addView(enableVideoSwitch)
        optionsSection.addView(videoLayout)

        // 4K recording toggle
        val resolution4KLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        resolution4KLayout.addView(
            TextView(this).apply {
                text = "4K Recording: "
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        enable4KSwitch = Switch(this).apply { isChecked = false }
        resolution4KLayout.addView(enable4KSwitch)
        optionsSection.addView(resolution4KLayout)

        // RAW capture toggle
        val rawLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rawLayout.addView(
            TextView(this).apply {
                text = "RAW Image Capture: "
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        enableRawCaptureSwitch =
            Switch(this).apply {
                isChecked = false
                setOnCheckedChangeListener { _, isChecked ->
                    rawFrameRateSpinner.isEnabled = isChecked
                }
            }
        rawLayout.addView(enableRawCaptureSwitch)
        optionsSection.addView(rawLayout)

        // RAW frame rate selection
        val frameRateLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        frameRateLayout.addView(
            TextView(this).apply {
                text = "RAW Frame Rate: "
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        rawFrameRateSpinner =
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@MultiModalRecordingActivity,
                        android.R.layout.simple_spinner_item,
                        listOf("30 fps", "15 fps", "10 fps", "5 fps"),
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                isEnabled = false
            }
        frameRateLayout.addView(rawFrameRateSpinner)
        optionsSection.addView(frameRateLayout)
        layout.addView(optionsSection)

        // Session ID input
        layout.addView(TextView(this).apply { text = "Session ID:" })
        sessionIdEdit =
            EditText(this).apply {
                setText(TimeUtil.generateSessionId("MultiModal"))
            }
        layout.addView(sessionIdEdit)

        // Participant ID input
        layout.addView(TextView(this).apply { text = "Participant ID (optional):" })
        participantIdEdit = EditText(this)
        layout.addView(participantIdEdit)

        // Study name input
        layout.addView(TextView(this).apply { text = "Study Name (optional):" })
        studyNameEdit = EditText(this)
        layout.addView(studyNameEdit)

        // Start/Stop button
        startStopButton =
            Button(this).apply {
                text = "Start Recording"
                setOnClickListener { toggleRecording() }
            }
        layout.addView(startStopButton)

        // Sync event button
        syncEventButton =
            Button(this).apply {
                text = "Trigger Sync Event"
                isEnabled = false
                setOnClickListener { triggerSyncEvent() }
            }
        layout.addView(syncEventButton)

        // Status and progress
        progressBar =
            ProgressBar(this).apply {
                visibility = View.GONE
            }
        layout.addView(progressBar)

        statusText =
            TextView(this).apply {
                text = "Ready to record"
            }
        layout.addView(statusText)

        // Statistics
        sampleCountText = TextView(this).apply { text = "Samples: 0" }
        layout.addView(sampleCountText)

        syncCountText = TextView(this).apply { text = "Sync Events: 0" }
        layout.addView(syncCountText)

        sessionDurationText = TextView(this).apply { text = "Duration: 0s" }
        layout.addView(sessionDurationText)

        fileLocationText =
            TextView(this).apply {
                text = "Files: Not started"
                textSize = 12f
            }
        layout.addView(fileLocationText)

        // Network Status Section
        val networkSection =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
                setBackgroundColor(android.graphics.Color.LTGRAY)
            }

        val networkTitle =
            TextView(this).apply {
                text = "Network & Device Status"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 8, 8, 8)
            }
        networkSection.addView(networkTitle)

        // Network status display
        networkStatusText =
            TextView(this).apply {
                text = "Network: Disconnected"
                textSize = 14f
                setPadding(8, 4, 8, 4)
            }
        networkSection.addView(networkStatusText)

        // Discovered devices display
        discoveredDevicesText =
            TextView(this).apply {
                text = "Discovered Devices: None"
                textSize = 14f
                setPadding(8, 4, 8, 4)
            }
        networkSection.addView(discoveredDevicesText)

        // Streaming queue status
        streamingQueueText =
            TextView(this).apply {
                text = "Streaming Queue: 0 items"
                textSize = 14f
                setPadding(8, 4, 8, 4)
            }
        networkSection.addView(streamingQueueText)

        // Network metrics
        networkMetricsText =
            TextView(this).apply {
                text = "Latency: -- ms | Throughput: -- KB/s"
                textSize = 12f
                setPadding(8, 4, 8, 4)
            }
        networkSection.addView(networkMetricsText)

        // Device discovery button
        startDiscoveryButton =
            Button(this).apply {
                text = "Start Device Discovery"
                setOnClickListener { startDeviceDiscovery() }
            }
        networkSection.addView(startDiscoveryButton)

        // Connect to device button
        connectToDeviceButton =
            Button(this).apply {
                text = "Connect to PC Controller"
                isEnabled = false
                setOnClickListener { connectToSelectedDevice() }
            }
        networkSection.addView(connectToDeviceButton)

        layout.addView(networkSection)

        setContentView(layout)

        // Initialize recording components
        gsrRecorder = GSRRecorder(this)
        sessionManager = SessionManager.getInstance(this)

        // Initialize network client for PC Controller communication
        networkClient =
            com.topdon.gsr.network.NetworkClient(this).apply {
                setEventListener(
                    object : com.topdon.gsr.network.NetworkClient.NetworkEventListener {
                        override fun onControllerDiscovered(controller: com.topdon.gsr.network.NetworkClient.ControllerInfo) {
                            runOnUiThread {
                                discoveredDevices.add(controller)
                                updateNetworkStatusUI()
                                connectToDeviceButton.isEnabled = true
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Found PC Controller: ${controller.name} (${controller.address})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onConnected(controller: com.topdon.gsr.network.NetworkClient.ControllerInfo) {
                            runOnUiThread {
                                updateNetworkStatusUI()
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Connected to ${controller.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onDisconnected(reason: String) {
                            runOnUiThread {
                                updateNetworkStatusUI()
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Disconnected: $reason",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                            runOnUiThread {
                                // Auto-fill session info from remote request
                                sessionIdEdit.setText(sessionInfo.sessionId)
                                participantIdEdit.setText(sessionInfo.participantId)
                                studyNameEdit.setText(sessionInfo.studyName)

                                // Auto-start recording if requested
                                if (!isRecording) {
                                    startRecording()
                                }
                            }
                        }

                        override fun onSyncFlash(durationMs: Int) {
                            runOnUiThread {
                                // Flash screen for sync
                                val overlay =
                                    android.view.View(this@MultiModalRecordingActivity).apply {
                                        setBackgroundColor(android.graphics.Color.WHITE)
                                        alpha = 1.0f
                                    }

                                val frameLayout = findViewById<android.widget.FrameLayout>(android.R.id.content)
                                frameLayout.addView(
                                    overlay,
                                    android.widget.FrameLayout.LayoutParams(
                                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                    ),
                                )

                                overlay.animate()
                                    .alpha(0.0f)
                                    .setDuration(durationMs.toLong())
                                    .withEndAction { frameLayout.removeView(overlay) }
                                    .start()
                            }
                        }

                        override fun onError(
                            operation: String,
                            error: String,
                        ) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Network error: $error", Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }

                        // Additional methods for enhanced NetworkClient
                        override fun onTimeSynchronized(offsetNanoseconds: Long) {
                            runOnUiThread {
                                statusText.text = "Time synchronized with PC Controller (offset: ${offsetNanoseconds}ns)"
                            }
                        }

                        override fun onDataStreamingStarted() {
                            runOnUiThread {
                                statusText.text = "Real-time data streaming active"
                            }
                        }

                        override fun onDataStreamingStopped() {
                            runOnUiThread {
                                statusText.text = "Data streaming stopped"
                            }
                        }

                        override fun onPairingRequested(controllerId: String, controllerName: String) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Pairing requested by: $controllerName",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onPairingCompleted(controllerId: String, success: Boolean) {
                            runOnUiThread {
                                val message = if (success) "Device pairing successful" else "Device pairing failed"
                                Toast.makeText(this@MultiModalRecordingActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onAuthenticationRequired(controllerId: String) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Authentication required for PC Controller",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                )
            }

        // Initialize RGB camera recorder
        rgbCameraRecorder =
            RGBCameraRecorder(this, cameraPreview).apply {
                onRecordingStarted = {
                    runOnUiThread {
                        statusText.text = "Recording RGB video + GSR..."
                    }
                }
                onRecordingStopped = { videoFile ->
                    runOnUiThread {
                        statusText.text = "RGB recording stopped. Video: ${videoFile?.name ?: "None"}"
                    }
                }
                onRawImageCaptured = { dngFile ->
                    runOnUiThread {
                        Log.d(TAG, "RAW image captured: ${dngFile.name}")
                    }
                }
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(
                            this@MultiModalRecordingActivity,
                            "Camera Error: $error", Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }

        // Initialize camera
        rgbCameraRecorder?.initialize()
        gsrRecorder.addListener(gsrListener)

        // Check permissions
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val basePermissions =
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                // Critical: Camera permission for RGB video recording
                Manifest.permission.CAMERA,
            )

        // Check base permissions
        val baseGranted =
            basePermissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }

        // Check Android 12+ Bluetooth permissions for Shimmer3 GSR devices
        val bluetoothGranted =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val bluetoothPermissions =
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                bluetoothPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                }
            } else {
                // Legacy Bluetooth permissions
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            }

        return baseGranted && bluetoothGranted
    }

    private fun requestPermissions() {
        val permissionsToRequest =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12+ permissions including Camera and Bluetooth for Shimmer3 GSR devices
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    // Critical: Camera permission for RGB video recording
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                // Legacy permissions including Camera
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    // Critical: Camera permission for RGB video recording
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                )
            }

        ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                statusText.text = "All permissions granted. GSR recording with Shimmer3 devices ready."
            } else {
                statusText.text = "Permissions required for GSR recording and Shimmer3 device access."
                val missingPermissions = mutableListOf<String>()

                // Check which specific permissions are missing
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        when (permission) {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            -> missingPermissions.add("Storage")
                            Manifest.permission.RECORD_AUDIO -> missingPermissions.add("Audio")
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            -> missingPermissions.add("Bluetooth (for Shimmer3 GSR)")
                        }
                    }
                }

                Toast.makeText(
                    this,
                    "Missing permissions: ${missingPermissions.joinToString(", ")}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun toggleRecording() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }

        // Guard against concurrent toggling
        if (isStartingRecording) {
            Log.d(TAG, "Recording start already in progress, ignoring additional taps")
            return
        }

        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        // Set guard flag and disable button immediately to prevent double taps
        isStartingRecording = true
        startStopButton.isEnabled = false
        startStopButton.text = "Starting..."

        val sessionId =
            sessionIdEdit.text.toString().trim().ifEmpty {
                TimeUtil.generateSessionId("MultiModal")
            }
        val participantId = participantIdEdit.text.toString().trim().takeIf { it.isNotEmpty() }
        val studyName = studyNameEdit.text.toString().trim().takeIf { it.isNotEmpty() }

        // Start RGB camera recording if enabled
        if (enableVideoSwitch.isChecked) {
            val resolution =
                if (enable4KSwitch.isChecked) {
                    RGBCameraRecorder.VideoResolution.UHD_4K
                } else {
                    RGBCameraRecorder.VideoResolution.HD_1080P
                }

            val rawFrameRate =
                when (rawFrameRateSpinner.selectedItemPosition) {
                    0 -> 30
                    1 -> 15
                    2 -> 10
                    3 -> 5
                    else -> 30
                }

            val cameraSettings =
                RGBCameraRecorder.RecordingSettings(
                    resolution = resolution,
                    // Video frame rate
                    frameRate = 60,
                    bitRate = if (resolution == RGBCameraRecorder.VideoResolution.UHD_4K) 12_000_000 else 8_000_000,
                    enableStabilization = true,
                    enableFlash = false,
                    audioEnabled = true,
                    enableRawCapture = enableRawCaptureSwitch.isChecked,
                    rawCaptureFrameRate = rawFrameRate,
                )

            rgbCameraRecorder?.updateSettings(cameraSettings)

            val cameraStarted = rgbCameraRecorder?.startRecording(sessionId) ?: false
            if (!cameraStarted) {
                // Reset guard flags on failure
                isStartingRecording = false
                startStopButton.isEnabled = true
                startStopButton.text = "Start Recording"
                statusText.text = "Failed to start camera recording"
                Toast.makeText(this, "Failed to start RGB camera recording", Toast.LENGTH_LONG).show()
                return
            }
        }

        // Start GSR recording asynchronously
        lifecycleScope.launch {
            try {
                val success = gsrRecorder.startRecording(sessionId, participantId, studyName)

                if (success) {
                    // Reset counters
                    sampleCount = 0
                    syncMarkCount = 0

                    // Atomic state update
                    isRecording = true
                    isStartingRecording = false

                    // Start enhanced recording service for background operation
                    try {
                        com.topdon.gsr.service.EnhancedRecordingService.startRecording(
                            this@MultiModalRecordingActivity,
                            sessionId,
                            participantId,
                            studyName
                        )
                        Log.i(TAG, "Enhanced recording service started")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to start enhanced recording service", e)
                        // Continue without service - not critical
                    }

                    runOnUiThread {
                        updateUI()
                        startStopButton.isEnabled = true
                        startStopButton.text = "Stop Recording"
                    }

                    val recordingModes = mutableListOf<String>()
                    if (enableVideoSwitch.isChecked) {
                        recordingModes.add(if (enable4KSwitch.isChecked) "4K Video" else "1080p Video")
                        if (enableRawCaptureSwitch.isChecked) {
                            recordingModes.add("RAW Images (${rawFrameRateSpinner.selectedItem})")
                        }
                    }
                    recordingModes.add("GSR (128Hz)")

                    runOnUiThread {
                        statusText.text = "Recording: ${recordingModes.joinToString(", ")}"
                    }

                    Log.i(TAG, "Multi-modal recording started: $sessionId")
                } else {
                    // Reset guard flags on GSR failure
                    isStartingRecording = false

                    runOnUiThread {
                        startStopButton.isEnabled = true
                        startStopButton.text = "Start Recording"
                        statusText.text = "Failed to start recording"
                    }

                    // Stop camera if GSR fails
                    rgbCameraRecorder?.stopRecording()
                    Toast.makeText(this@MultiModalRecordingActivity, "Failed to start GSR recording", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Reset guard flags on exception
                isStartingRecording = false

                runOnUiThread {
                    startStopButton.isEnabled = true
                    startStopButton.text = "Start Recording"
                    statusText.text = "Error starting recording"
                }

                Log.e(TAG, "Error starting recording", e)
                rgbCameraRecorder?.stopRecording()
                Toast.makeText(this@MultiModalRecordingActivity, "Error starting recording: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopRecording() {
        // Stop enhanced recording service
        try {
            com.topdon.gsr.service.EnhancedRecordingService.stopRecording(this)
            Log.i(TAG, "Enhanced recording service stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop enhanced recording service", e)
            // Continue - not critical
        }

        // Stop RGB camera recording
        val videoFile = rgbCameraRecorder?.stopRecording()

        val session = gsrRecorder.stopRecording()
        session?.let {
            Log.i(TAG, "Multi-modal recording stopped: ${it.sessionId}")

            val recordingInfo = mutableListOf<String>()
            videoFile?.let { file -> recordingInfo.add("Video: ${file.name}") }
            rgbCameraRecorder?.getRawImagesDirectory()?.let { dir ->
                val rawCount = rgbCameraRecorder?.getRawCaptureCount() ?: 0
                recordingInfo.add("RAW images: $rawCount in ${dir.name}")
            }
            recordingInfo.add("GSR samples: ${it.sampleCount}")

            statusText.text = "Recording completed. ${recordingInfo.joinToString(", ")}"
        }

        isRecording = false
        updateUI()
    }

    private fun triggerSyncEvent() {
        lifecycleScope.launch {
            if (gsrRecorder.addSyncMark("USER_TRIGGER", "Manual sync event triggered from UI")) {
                Log.d(TAG, "User sync event triggered successfully")
                statusText.text = "Sync event added at ${System.currentTimeMillis()}"
            } else {
                Log.w(TAG, "Failed to trigger sync event")
                statusText.text = "Failed to add sync event"
            }
        }
    }

    private fun updateUI() {
        startStopButton.text = if (isRecording) "Stop Recording" else "Start Recording"
        syncEventButton.isEnabled = isRecording

        sessionIdEdit.isEnabled = !isRecording
        participantIdEdit.isEnabled = !isRecording
        studyNameEdit.isEnabled = !isRecording
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.multi_modal_recording_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gallery -> {
                openGallery()
                true
            }
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_session_manager -> {
                openSessionManager()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openGallery() {
        GSRGalleryActivity.startActivity(this)
    }

    private fun openSettings() {
        GSRSettingsActivity.startActivity(this)
    }

    private fun openSessionManager() {
        SessionManagerActivity.startActivity(this)
    }

    // Network status UI update method
    private fun updateNetworkStatusUI() {
        runOnUiThread {
            // Update network connection status
            val connectionStatus = when {
                networkClient?.isConnected() == true -> "Connected"
                isServiceBound -> "Service Bound"
                else -> "Disconnected"
            }
            networkStatusText.text = "Network: $connectionStatus"

            // Update discovered devices
            val deviceCount = discoveredDevices.size
            val deviceText = if (deviceCount > 0) {
                val firstDevice = discoveredDevices.first()
                "Devices: $deviceCount found (${firstDevice.name})"
            } else {
                "Discovered Devices: None"
            }
            discoveredDevicesText.text = deviceText

            // Update streaming queue (get total from all queue types)
            enhancedRecordingService?.let { service ->
                val queueSizes = service.getQueueSizes()
                val totalItems = queueSizes.values.sum()
                val queueText = if (queueSizes.isNotEmpty()) {
                    val details = queueSizes.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    "Streaming Queue: $totalItems items ($details)"
                } else {
                    "Streaming Queue: 0 items"
                }
                streamingQueueText.text = queueText
            } ?: run {
                streamingQueueText.text = "Streaming Queue: Service not bound"
            }

            // Update network metrics (simulate metrics)
            networkClient?.let { client ->
                if (client.isConnected()) {
                    val latency = client.getLatencyMs()
                    val throughput = client.getThroughputKBps()
                    networkMetricsText.text = "Latency: ${latency} ms | Throughput: ${throughput} KB/s"
                } else {
                    networkMetricsText.text = "Latency: -- ms | Throughput: -- KB/s"
                }
            }
        }
    }

    // Start device discovery
    private fun startDeviceDiscovery() {
        discoveredDevices.clear()
        connectToDeviceButton.isEnabled = false
        startDiscoveryButton.text = "Searching..."
        startDiscoveryButton.isEnabled = false

        networkClient?.startDiscovery { success ->
            runOnUiThread {
                startDiscoveryButton.text = "Start Device Discovery"
                startDiscoveryButton.isEnabled = true
                if (!success) {
                    Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show()
                }
                updateNetworkStatusUI()
            }
        }
    }

    // Connect to selected device
    private fun connectToSelectedDevice() {
        if (discoveredDevices.isNotEmpty()) {
            val selectedDevice = discoveredDevices.first() // For simplicity, connect to first device
            networkClient?.connectToController(selectedDevice.address, selectedDevice.port) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Connection successful", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                    updateNetworkStatusUI()
                }
            }
        }
    }

    // Service binding methods
    private fun bindEnhancedRecordingService() {
        try {
            val intent = Intent(this, com.topdon.gsr.service.EnhancedRecordingService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind enhanced recording service", e)
            Toast.makeText(this, "Enhanced recording service not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unbindEnhancedRecordingService() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
            enhancedRecordingService = null
        }
    }

    override fun onStart() {
        super.onStart()
        bindEnhancedRecordingService()
        startUIUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopUIUpdates()
        unbindEnhancedRecordingService()
    }

    // Start periodic UI updates
    private fun startUIUpdates() {
        uiUpdateJob?.cancel()
        uiUpdateJob = lifecycleScope.launch {
            while (true) {
                updateNetworkStatusUI()
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }

    // Stop periodic UI updates
    private fun stopUIUpdates() {
        uiUpdateJob?.cancel()
        uiUpdateJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUIUpdates()
        gsrRecorder.removeListener(gsrListener)
        if (isRecording) {
            stopRecording()
        }
        rgbCameraRecorder?.cleanup()
        networkClient?.cleanup()
        unbindEnhancedRecordingService()
    }
}
