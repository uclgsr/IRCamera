package com.topdon.tc001.gsr

// Enhanced unified BLE integration for comprehensive cross-modal coordination
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMultiModalRecordingBinding
import com.topdon.ble.UnifiedBleManager
import com.topdon.ble.UnifiedDevice
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.service.SessionManager
import com.topdon.gsr.util.TimeUtil
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.tc001.camera.RGBCameraRecorder
import com.topdon.tc001.permissions.PermissionController
import kotlinx.coroutines.launch

// Note: EnhancedRecordingService is referenced with full package name since it's in a different module

class MultiModalRecordingActivity : BaseBindingActivity<ActivityMultiModalRecordingBinding>() {
    companion object {
        private const val TAG = "MultiModalActivity"

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

    // Session information
    private var sessionId: String = ""
    private var participantId: String? = null

    // Permission handling
    private lateinit var permissionController: PermissionController

    // Enhanced unified BLE management for cross-modal coordination
    private var unifiedBleManager: UnifiedBleManager? = null
    private var discoveredBleDevices = mutableListOf<UnifiedDevice>()
    private var connectedBleDevices = mutableListOf<UnifiedDevice>()

    // Enhanced service integration
    private var enhancedRecordingService: com.topdon.gsr.service.EnhancedRecordingService? = null
    private var isServiceBound = false
    private var discoveredDevices =
        mutableListOf<com.topdon.gsr.network.NetworkClient.ControllerInfo>()

    // UI update timer
    private var uiUpdateJob: kotlinx.coroutines.Job? = null

    override fun initContentLayoutId() = R.layout.activity_multi_modal_recording

    // Service connection for enhanced recording service
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                val binder =
                    service as? com.topdon.gsr.service.EnhancedRecordingService.EnhancedRecordingBinder
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
                    binding.statusText.text = "Recording GSR data at 128 Hz..."
                    binding.progressBar.visibility = View.VISIBLE

                    val sessionDir = gsrRecorder.getSessionDirectory()?.absolutePath ?: "Unknown"
                    binding.dataText.text = "Files: $sessionDir"
                }
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                runOnUiThread {
                    isRecording = false
                    currentSession = null
                    updateUI()
                    binding.statusText.text =
                        "Recording completed. ${sessionInfo.sampleCount} samples recorded."
                    binding.progressBar.visibility = View.GONE

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
                        binding.dataText.text = "Samples: $sampleCount"
                        currentSession?.let { session ->
                            val duration = (System.currentTimeMillis() - session.startTime) / 1000
                            binding.dataText.text =
                                "${binding.dataText.text} | Duration: ${duration}s"
                        }
                    }
                }
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                syncMarkCount++
                runOnUiThread {
                    binding.dataText.text = "${binding.dataText.text} | Sync Events: $syncMarkCount"
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "Sync: ${syncMark.eventType}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    binding.statusText.text = "Error: $error"
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "GSR Error: $error",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission controller first
        permissionController = PermissionController(this)
        permissionController.initialize()

        // Initialize recording components
        gsrRecorder = GSRRecorder(this)
        sessionManager = SessionManager.getInstance(this)

        // Set up view references using binding
        with(binding) {
            // Configure session ID input
            participantIdInput.setText(TimeUtil.generateSessionId("MultiModal"))

            // Configure switches
            enableVideoSwitch.isChecked = true
            enable4kSwitch.isChecked = false
            enableRawCaptureSwitch.isChecked = false

            // Set up raw frame rate spinner
            val frameRateAdapter =
                ArrayAdapter(
                    this@MultiModalRecordingActivity,
                    android.R.layout.simple_spinner_item,
                    listOf("30 fps", "15 fps", "10 fps", "5 fps"),
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            rawFrameRateSpinner.adapter = frameRateAdapter
            rawFrameRateSpinner.isEnabled = false

            // Configure raw capture switch listener
            enableRawCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
                rawFrameRateSpinner.isEnabled = isChecked
            }

            // Set up control buttons
            startButton.setOnClickListener { toggleRecording() }
            stopButton.setOnClickListener { stopRecording() }
            syncButton.setOnClickListener { triggerSyncEvent() }
            flashSyncButton.setOnClickListener { triggerFlashSync() }

            // Network control buttons
            binding.startDiscoveryButton.setOnClickListener { startDeviceDiscovery() }
            binding.connectToDeviceButton.setOnClickListener { connectToSelectedDevice() }

            // Initial UI state
            binding.statusText.text = "Ready to record"
            binding.dataText.text = "No data recorded yet"
            binding.networkStatusText.text = "Network: Disconnected"
            binding.discoveredDevicesText.text = "Discovered Devices: None"
            binding.streamingQueueText.text = "Streaming Queue: 0 items"
            networkMetricsText.text = "Latency: -- ms | Throughput: -- KB/s"
        }

        // Initialize network client for PC Controller communication
        networkClient =
            com.topdon.gsr.network.NetworkClient(this).apply {
                setEventListener(
                    object : com.topdon.gsr.network.NetworkClient.NetworkEventListener {
                        override fun onControllerDiscovered(controller: com.topdon.gsr.network.NetworkClient.ControllerInfo) {
                            runOnUiThread {
                                discoveredDevices.add(controller)
                                updateNetworkStatusUI()
                                binding.connectToDeviceButton.isEnabled = true
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Found PC Controller: ${controller.deviceName} (${controller.ipAddress})",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }

                        override fun onConnected(controller: com.topdon.gsr.network.NetworkClient.ControllerInfo) {
                            runOnUiThread {
                                updateNetworkStatusUI()
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Connected to ${controller.deviceName}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }

                        override fun onDisconnected(reason: String) {
                            runOnUiThread {
                                updateNetworkStatusUI()
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Disconnected: $reason",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }

                        override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                            runOnUiThread {
                                // Auto-fill session info from remote request
                                binding.participantIdInput.setText(sessionInfo.sessionId)
                                binding.participantIdInput.setText(sessionInfo.participantId)

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

                                val frameLayout =
                                    findViewById<android.widget.FrameLayout>(android.R.id.content)
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
                                binding.statusText.text =
                                    "Time synchronized with PC Controller (offset: ${offsetNanoseconds}ns)"
                            }
                        }

                        override fun onDataStreamingStarted() {
                            runOnUiThread {
                                binding.statusText.text = "Real-time data streaming active"
                            }
                        }

                        override fun onDataStreamingStopped() {
                            runOnUiThread {
                                binding.statusText.text = "Data streaming stopped"
                            }
                        }

                        override fun onPairingRequested(
                            controllerId: String,
                            controllerName: String,
                        ) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Pairing requested by: $controllerName",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }

                        override fun onPairingCompleted(
                            controllerId: String,
                            success: Boolean,
                        ) {
                            runOnUiThread {
                                val message =
                                    if (success) "Device pairing successful" else "Device pairing failed"
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onAuthenticationRequired(controllerId: String) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MultiModalRecordingActivity,
                                    "Authentication required for PC Controller",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )
            }

        // Initialize RGB camera recorder
        // Camera preview not available in this layout - skip RGBCameraRecorder initialization
        rgbCameraRecorder = null
        Log.i(TAG, "RGBCameraRecorder skipped - no preview available in this layout")

        // Initialize camera
        // rgbCameraRecorder?.initialize() // Skipped since rgbCameraRecorder is null
        gsrRecorder.addListener(gsrListener)

        // Check permissions using new PermissionController
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!permissionController.hasAllRequiredPermissions()) {
            permissionController.requestAllPermissions { allGranted, deniedPermissions ->
                if (allGranted) {
                    binding.statusText.text =
                        "All permissions granted. Multi-sensor recording ready."
                    Log.i(TAG, "All permissions granted successfully")
                } else {
                    val permissionNames = permissionController.getPermissionNames(deniedPermissions)
                    binding.statusText.text =
                        "Some permissions denied. Limited functionality available."

                    Log.w(TAG, "Some permissions denied: ${deniedPermissions.joinToString(", ")}")
                    Toast.makeText(
                        this,
                        "Missing permissions: ${permissionNames.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            binding.statusText.text = "All permissions granted. Multi-sensor recording ready."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun toggleRecording() {
        if (!permissionController.hasAllRequiredPermissions()) {
            checkAndRequestPermissions()
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
        binding.startButton.isEnabled = false
        binding.startButton.text = "Starting..."

        // Request battery optimization exemption for reliable background recording
        permissionController.requestBatteryOptimizationExemption { exemptionGranted ->
            if (!exemptionGranted) {
                Log.w(
                    TAG,
                    "Battery optimization exemption not granted - recording may be interrupted"
                )
                Toast.makeText(
                    this,
                    "Warning: Battery optimization not disabled. Recording may be interrupted.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Continue with recording start
            proceedWithRecordingStart()
        }
    }

    private fun proceedWithRecordingStart() {
        sessionId = binding.participantIdInput.text.toString().trim().ifEmpty {
            TimeUtil.generateSessionId("MultiModal")
        }
        participantId = binding.participantIdInput.text.toString().trim().takeIf { it.isNotEmpty() }

        // Start RGB camera recording if enabled
        if (binding.enableVideoSwitch.isChecked) {
            val resolution =
                if (binding.enable4kSwitch.isChecked) {
                    RGBCameraRecorder.VideoResolution.UHD_4K
                } else {
                    RGBCameraRecorder.VideoResolution.HD_1080P
                }

            val rawFrameRate =
                when (binding.rawFrameRateSpinner.selectedItemPosition) {
                    0 -> 30
                    1 -> 15
                    2 -> 10
                    3 -> 5
                    else -> 30
                }

            val cameraSettings =
                RGBCameraRecorder.RecordingSettings(
                    resolution = resolution,
                    frameRate = 60, // Video frame rate
                    bitRate = if (resolution == RGBCameraRecorder.VideoResolution.UHD_4K) 12_000_000 else 8_000_000,
                    enableStabilization = true,
                    enableFlash = false,
                    audioEnabled = true,
                    rawCaptureFrameRate = rawFrameRate,
                )

            rgbCameraRecorder?.updateSettings(cameraSettings)

            lifecycleScope.launch {
                val cameraStarted = rgbCameraRecorder?.startRecording(sessionId) ?: false
                if (!cameraStarted) {
                    // Reset guard flags on failure
                    runOnUiThread {
                        isStartingRecording = false
                        binding.startButton.isEnabled = true
                        binding.startButton.text = "Start Recording"
                        binding.statusText.text = "Failed to start camera recording"
                        Toast.makeText(
                            this@MultiModalRecordingActivity,
                            "Failed to start RGB camera recording",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
            }
        }

        // Start GSR recording asynchronously
        lifecycleScope.launch {
            try {
                val success = gsrRecorder.startRecording(sessionId, participantId, null)

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
                            null,
                        )
                        Log.i(TAG, "Enhanced recording service started")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to start enhanced recording service", e)
                    }

                    runOnUiThread {
                        binding.startButton.text = "Stop Recording"
                        binding.startButton.isEnabled = true
                        updateUI()
                    }
                } else {
                    // Recording failed, reset state
                    isStartingRecording = false
                    runOnUiThread {
                        binding.startButton.isEnabled = true
                        binding.startButton.text = "Start Recording"
                        binding.statusText.text = "Failed to start GSR recording"
                        Toast.makeText(
                            this@MultiModalRecordingActivity,
                            "Failed to start GSR recording",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                isStartingRecording = false
                runOnUiThread {
                    binding.startButton.isEnabled = true
                    binding.startButton.text = "Start Recording"
                    binding.statusText.text = "Error starting recording: ${e.message}"
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startRecordingInternal() {
        // Start GSR recording asynchronously
        lifecycleScope.launch {
            try {
                val success = gsrRecorder.startRecording(sessionId, participantId, null)

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
                            null,
                        )
                        Log.i(TAG, "Enhanced recording service started")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to start enhanced recording service", e)
                        // Continue without service - not critical
                    }

                    runOnUiThread {
                        updateUI()
                        binding.startButton.isEnabled = true
                        binding.startButton.text = "Start Recording"
                        binding.stopButton.isEnabled = true
                    }

                    val recordingModes = mutableListOf<String>()
                    if (binding.enableVideoSwitch.isChecked) {
                        recordingModes.add(if (binding.enable4kSwitch.isChecked) "4K Video" else "1080p Video")
                        if (binding.enableRawCaptureSwitch.isChecked) {
                            recordingModes.add("RAW Images (${binding.rawFrameRateSpinner.selectedItem})")
                        }
                    }
                    recordingModes.add("GSR (128Hz)")

                    runOnUiThread {
                        binding.statusText.text = "Recording: ${recordingModes.joinToString(", ")}"
                    }

                    Log.i(TAG, "Multi-modal recording started: $sessionId")
                } else {
                    // Reset guard flags on GSR failure
                    isStartingRecording = false

                    runOnUiThread {
                        binding.startButton.isEnabled = true
                        binding.startButton.text = "Start Recording"
                        binding.statusText.text = "Failed to start recording"
                    }

                    // Stop camera if GSR fails
                    rgbCameraRecorder?.stopRecording()
                    Toast.makeText(
                        this@MultiModalRecordingActivity,
                        "Failed to start GSR recording",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Reset guard flags on exception
                isStartingRecording = false

                runOnUiThread {
                    binding.startButton.isEnabled = true
                    binding.startButton.text = "Start Recording"
                    binding.statusText.text = "Error starting recording"
                }

                Log.e(TAG, "Error starting recording", e)
                rgbCameraRecorder?.stopRecording()
                Toast.makeText(
                    this@MultiModalRecordingActivity,
                    "Error starting recording: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            // Stop enhanced recording service
            try {
                com.topdon.gsr.service.EnhancedRecordingService.stopRecording(this@MultiModalRecordingActivity)
                Log.i(TAG, "Enhanced recording service stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop enhanced recording service", e)
                // Continue - not critical
            }

            // Stop RGB camera recording
            val videoStopped = rgbCameraRecorder?.stopRecording() ?: false

            val session = gsrRecorder.stopRecording()
            session?.let {
                Log.i(TAG, "Multi-modal recording stopped: ${it.sessionId}")

                val recordingInfo = mutableListOf<String>()
                if (videoStopped) {
                    recordingInfo.add("Video recording completed")
                }
                rgbCameraRecorder?.getRawImagesDirectory()?.let { dir ->
                    val rawCount = rgbCameraRecorder?.getRawCaptureCount() ?: 0
                    recordingInfo.add("RAW images: $rawCount in ${dir.name}")
                }
                recordingInfo.add("GSR samples: ${it.sampleCount}")

                runOnUiThread {
                    binding.statusText.text =
                        "Recording completed. ${recordingInfo.joinToString(", ")}"
                    isRecording = false
                    updateUI()
                }
            }

        }

        runOnUiThread {
            if (!isRecording) {
                isRecording = false
                updateUI()
            }
        }
    }

    private fun triggerSyncEvent() {
        lifecycleScope.launch {
            if (gsrRecorder.addSyncMark("USER_TRIGGER", "Manual sync event triggered from UI")) {
                Log.d(TAG, "User sync event triggered successfully")
                binding.statusText.text = "Sync event added at ${System.currentTimeMillis()}"
            } else {
                Log.w(TAG, "Failed to trigger sync event")
                binding.statusText.text = "Failed to add sync event"
            }
        }
    }

    private fun triggerFlashSync() {
        // Trigger a visual flash for synchronization
        val overlay =
            android.view.View(this).apply {
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
            .setDuration(200)
            .withEndAction {
                frameLayout.removeView(overlay)
                // Also add a sync mark
                triggerSyncEvent()
            }
            .start()
    }

    private fun updateUI() {
        binding.startButton.text = if (isRecording) "Recording..." else "Start Recording"
        binding.stopButton.isEnabled = isRecording
        binding.syncButton.isEnabled = isRecording
        binding.flashSyncButton.isEnabled = isRecording

        binding.participantIdInput.isEnabled = !isRecording
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

            R.id.action_sync_test -> {
                openSynchronizationTest()
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

    private fun openSynchronizationTest() {
        val intent = Intent(this, com.topdon.tc001.test.SynchronizationTestActivity::class.java)
        startActivity(intent)
    }

    // Network status UI update method
    private fun updateNetworkStatusUI() {
        runOnUiThread {
            // Update network connection status
            val connectionStatus =
                when {
                    networkClient?.isConnected() == true -> "Connected"
                    isServiceBound -> "Service Bound"
                    else -> "Disconnected"
                }
            binding.networkStatusText.text = "Network: $connectionStatus"

            // Update discovered devices
            val deviceCount = discoveredDevices.size
            val deviceText =
                if (deviceCount > 0) {
                    val firstDevice = discoveredDevices.first()
                    "Devices: $deviceCount found (${firstDevice.deviceName})"
                } else {
                    "Discovered Devices: None"
                }
            binding.discoveredDevicesText.text = deviceText

            // Update streaming queue (get total from all queue types)
            enhancedRecordingService?.let { service ->
                val queueSizes = service.getQueueSizes()
                val totalItems = queueSizes.values.sum()
                val queueText =
                    if (queueSizes.isNotEmpty()) {
                        val details =
                            queueSizes.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                        "Streaming Queue: $totalItems items ($details)"
                    } else {
                        "Streaming Queue: 0 items"
                    }
                binding.streamingQueueText.text = queueText
            } ?: run {
                binding.streamingQueueText.text = "Streaming Queue: Service not bound"
            }

            // Update network metrics (simulate metrics)
            networkClient?.let { client ->
                if (client.isConnected()) {
                    val latency = client.getLatencyMs()
                    val throughput = client.getThroughputKBps()
                    binding.networkMetricsText.text =
                        "Latency: $latency ms | Throughput: $throughput KB/s"
                } else {
                    binding.networkMetricsText.text = "Latency: -- ms | Throughput: -- KB/s"
                }
            }
        }
    }

    // Start device discovery
    private fun startDeviceDiscovery() {
        discoveredDevices.clear()
        binding.connectToDeviceButton.isEnabled = false
        binding.startDiscoveryButton.text = "Searching..."
        binding.startDiscoveryButton.isEnabled = false

        networkClient?.startDiscovery { success ->
            runOnUiThread {
                binding.startDiscoveryButton.text = "Start Device Discovery"
                binding.startDiscoveryButton.isEnabled = true
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
            val selectedDevice =
                discoveredDevices.first() // For simplicity, connect to first device
            networkClient?.connectToController(
                selectedDevice.ipAddress,
                selectedDevice.port
            ) { success ->
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
            Toast.makeText(this, "Enhanced recording service not available", Toast.LENGTH_SHORT)
                .show()
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
        uiUpdateJob =
            lifecycleScope.launch {
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
