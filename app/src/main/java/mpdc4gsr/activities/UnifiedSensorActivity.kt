package mpdc4gsr.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.sensors.RgbCameraRecorder
import mpdc4gsr.sensors.unified.UnifiedGSRRecorder
import mpdc4gsr.sensors.unified.UnifiedNetworkController
import mpdc4gsr.sensors.unified.UnifiedSessionManager
import mpdc4gsr.sensors.unified.adapters.DeviceAdapter
import mpdc4gsr.sensors.unified.adapters.PCControllerAdapter
import mpdc4gsr.sensors.unified.model.DeviceInfo
import mpdc4gsr.sensors.unified.model.PCControllerInfo
import mpdc4gsr.sensors.unified.model.SessionConfig
import mpdc4gsr.sensors.unified.model.SessionQuality
import mpdc4gsr.sensors.unified.model.SessionStatus
import mpdc4gsr.sensors.unified.model.SessionType

class UnifiedSensorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UnifiedSensorActivity"

        private const val STATUS_UPDATE_INTERVAL_MS = 1000L
        private const val QUALITY_UPDATE_INTERVAL_MS = 5000L
    }

    private lateinit var gsrRecorder: UnifiedGSRRecorder
    private lateinit var networkController: UnifiedNetworkController
    private lateinit var sessionManager: UnifiedSessionManager
    private lateinit var recordingController: RecordingController
    private lateinit var rgbCameraRecorder: RgbCameraRecorder

    private lateinit var statusText: TextView
    private lateinit var qualityIndicator: ProgressBar
    private lateinit var gsrStatusText: TextView
    private lateinit var networkStatusText: TextView
    private lateinit var sessionStatusText: TextView
    private lateinit var cameraStatusText: TextView
    private lateinit var previewView: PreviewView
    private lateinit var switchCameraButton: Button
    private lateinit var cameraTypeText: TextView

    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var discoverButton: Button
    private lateinit var connectButton: Button

    private lateinit var pcRecyclerView: RecyclerView
    private lateinit var pcAdapter: PCControllerAdapter
    private lateinit var discoverPCButton: Button
    private lateinit var connectPCButton: Button

    private lateinit var sessionNameEdit: EditText
    private lateinit var participantIdEdit: EditText
    private lateinit var startSessionButton: Button
    private lateinit var stopSessionButton: Button
    private lateinit var addMarkerButton: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeComponents()
        } else {
            showPermissionError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unified_sensor)

        Log.i(TAG, "Starting Unified Sensor Activity - IRCamera Extension")

        initializeUI()
        checkPermissionsAndInitialize()
    }

    private fun initializeUI() {
        Log.d(TAG, "Initializing UI components")

        statusText = findViewById(R.id.statusText)
        qualityIndicator = findViewById(R.id.qualityIndicator)
        gsrStatusText = findViewById(R.id.gsrStatusText)
        networkStatusText = findViewById(R.id.networkStatusText)
        sessionStatusText = findViewById(R.id.sessionStatusText)
        cameraStatusText = findViewById(R.id.cameraStatusText)
        previewView = findViewById(R.id.previewView)
        switchCameraButton = findViewById(R.id.switchCameraButton)
        cameraTypeText = findViewById(R.id.cameraTypeText)

        // Setup camera switching
        switchCameraButton.setOnClickListener { switchCamera() }

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        deviceAdapter = DeviceAdapter { device -> connectToDevice(device) }
        deviceRecyclerView.adapter = deviceAdapter
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)

        discoverButton = findViewById(R.id.discoverButton)
        connectButton = findViewById(R.id.connectButton)

        discoverButton.setOnClickListener { startDeviceDiscovery() }
        connectButton.setOnClickListener { connectToSelectedDevice() }

        pcRecyclerView = findViewById(R.id.pcRecyclerView)
        pcAdapter = PCControllerAdapter { controller -> connectToPC(controller) }
        pcRecyclerView.adapter = pcAdapter
        pcRecyclerView.layoutManager = LinearLayoutManager(this)

        discoverPCButton = findViewById(R.id.discoverPCButton)
        connectPCButton = findViewById(R.id.connectPCButton)

        discoverPCButton.setOnClickListener { startPCDiscovery() }
        connectPCButton.setOnClickListener { connectToSelectedPC() }

        sessionNameEdit = findViewById(R.id.sessionNameEdit)
        participantIdEdit = findViewById(R.id.participantIdEdit)
        startSessionButton = findViewById(R.id.startSessionButton)
        stopSessionButton = findViewById(R.id.stopSessionButton)
        addMarkerButton = findViewById(R.id.addMarkerButton)

        startSessionButton.setOnClickListener { startSession() }
        stopSessionButton.setOnClickListener { stopSession() }
        addMarkerButton.setOnClickListener { addSyncMarker() }

        updateUIState(false, false, false)
    }

    private fun checkPermissionsAndInitialize() {
        val requiredPermissions = UnifiedGSRRecorder.getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.i(TAG, "Requesting missing permissions: ${missingPermissions.joinToString()}")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        Log.i(TAG, "Initializing core components")

        try {

            recordingController = RecordingController(this, this)

            gsrRecorder = UnifiedGSRRecorder(this, this)

            networkController = UnifiedNetworkController(this, this)

            // Initialize RGB Camera Recorder with PreviewView
            rgbCameraRecorder = RgbCameraRecorder(
                context = this,
                lifecycleOwner = this,
                previewView = previewView,
                useFrontCamera = false
            )

            sessionManager = UnifiedSessionManager(
                context = this,
                lifecycleOwner = this,
                recordingController = recordingController,
                networkController = networkController,
                gsrRecorder = gsrRecorder
            )

            lifecycleScope.launch {
                val gsrInitialized = gsrRecorder.initialize()
                val networkInitialized = networkController.initialize()
                val cameraInitialized = rgbCameraRecorder.initialize()

                // Register camera with RecordingController for unified session management
                if (cameraInitialized) {
                    recordingController.registerRgbCameraWithPreview(rgbCameraRecorder)
                    Log.i(TAG, "RGB camera registered with RecordingController")
                }

                // Initialize other sensors (thermal, GSR) in RecordingController
                val sensorsInitialized = recordingController.initializeSensors(skipRgbCamera = true)

                if (gsrInitialized && networkInitialized && cameraInitialized && sensorsInitialized) {
                    statusText.text = "All components initialized successfully"
                    updateUIState(true, false, false)
                    observeComponentStates()
                } else {
                    statusText.text = "Component initialization failed"
                    showInitializationError(gsrInitialized, networkInitialized, cameraInitialized)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            statusText.text = "Initialization error: ${e.message}"
        }
    }

    private fun observeComponentStates() {
        Log.d(TAG, "Setting up component state observers")

        lifecycleScope.launch {
            gsrRecorder.deviceStatus.collect { status ->
                gsrStatusText.text = "GSR: $status"
            }
        }

        lifecycleScope.launch {
            networkController.networkStatus.collect { status ->
                networkStatusText.text = "Network: ${status.displayName}"
            }
        }

        lifecycleScope.launch {
            rgbCameraRecorder.cameraStatus.collect { status ->
                cameraStatusText.text = "Camera: $status"

                // Update camera type display
                try {
                    val cameraInfo = rgbCameraRecorder.getCurrentCameraInfo()
                    cameraTypeText.text =
                        if (cameraInfo.isUsingFrontCamera) "Front Camera" else "Back Camera"
                    switchCameraButton.isEnabled = cameraInfo.canSwitch
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get camera info", e)
                }
            }
        }

        lifecycleScope.launch {
            sessionManager.sessionStatus.collect { status ->
                sessionStatusText.text = "Session: ${status.displayName}"
                updateSessionUI(status)
            }
        }

        lifecycleScope.launch {
            sessionManager.sessionQuality.collect { quality ->
                qualityIndicator.progress = (quality.overallQuality * 100).toInt()
                updateQualityIndicator(quality)
            }
        }

        lifecycleScope.launch {
            networkController.discoveredControllersFlow.collect { controllers ->
                pcAdapter.updateControllers(controllers)
            }
        }
    }

    private fun startDeviceDiscovery() {
        Log.i(TAG, "Starting Shimmer device discovery")

        discoverButton.isEnabled = false
        discoverButton.text = "Discovering..."

        lifecycleScope.launch {
            val success = gsrRecorder.startDeviceDiscovery()

            if (success) {
                val devices = gsrRecorder.getDiscoveredDevices()
                deviceAdapter.updateDevices(devices)

                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Found ${devices.size} Shimmer devices",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Device discovery failed",
                    Toast.LENGTH_SHORT
                ).show()
            }

            discoverButton.isEnabled = true
            discoverButton.text = "Discover Devices"
        }
    }

    private fun startPCDiscovery() {
        Log.i(TAG, "Starting PC controller discovery")

        discoverPCButton.isEnabled = false
        discoverPCButton.text = "Discovering..."

        lifecycleScope.launch {
            val success = networkController.startDiscovery()

            if (success) {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "PC discovery started",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "PC discovery failed",
                    Toast.LENGTH_SHORT
                ).show()
            }

            discoverPCButton.isEnabled = true
            discoverPCButton.text = "Discover PCs"
        }
    }

    private fun connectToDevice(device: DeviceInfo) {
        Log.i(TAG, "Connecting to Shimmer device: ${device.name}")

        lifecycleScope.launch {
            val connected = gsrRecorder.connectToDevice(device)

            if (connected) {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Connected to ${device.name}",
                    Toast.LENGTH_SHORT
                ).show()
                updateUIState(true, true, false)
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Failed to connect to ${device.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun connectToPC(controller: PCControllerInfo) {
        Log.i(TAG, "Connecting to PC controller: ${controller.name}")

        lifecycleScope.launch {
            val connected = networkController.connectToController(controller)

            if (connected) {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Connected to ${controller.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Failed to connect to ${controller.displayName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startSession() {
        Log.i(TAG, "Starting recording session")

        val sessionName = sessionNameEdit.text.toString().trim()
        val participantId = participantIdEdit.text.toString().trim()

        if (sessionName.isEmpty() || participantId.isEmpty()) {
            Toast.makeText(this, "Please enter session name and participant ID", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val sessionConfig = SessionConfig(
            sessionName = sessionName,
            studyName = "IRCamera Unified Recording",
            participantId = participantId,
            enabledSensors = listOf("gsr", "thermal", "rgb"),
            sessionType = SessionType.HYBRID,
            metadata = mapOf(
                "device_type" to "Android",
                "app_version" to "2.0.0",
                "unified_extension" to true
            )
        )

        lifecycleScope.launch {
            val session = sessionManager.createSession(sessionConfig)
            if (session != null) {
                val started = sessionManager.startSession()
                if (started) {
                    Toast.makeText(
                        this@UnifiedSensorActivity,
                        "Session started: ${session.sessionId}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUIState(true, true, true)
                } else {
                    Toast.makeText(
                        this@UnifiedSensorActivity,
                        "Failed to start session",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun stopSession() {
        Log.i(TAG, "Stopping recording session")

        lifecycleScope.launch {
            val stopped = sessionManager.stopSession()
            if (stopped) {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Session stopped successfully",
                    Toast.LENGTH_SHORT
                ).show()
                updateUIState(true, true, false)
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Failed to stop session",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addSyncMarker() {
        Log.i(TAG, "Adding sync marker")

        lifecycleScope.launch {
            val added = sessionManager.addSyncMarker(
                markerType = "manual_marker",
                markerData = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "source" to "user_button"
                )
            )

            if (added) {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Sync marker added",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@UnifiedSensorActivity,
                    "Failed to add sync marker",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun connectToSelectedDevice() {
        val selectedDevice = deviceAdapter.getSelectedDevice()
        if (selectedDevice != null) {
            connectToDevice(selectedDevice)
        } else {
            Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToSelectedPC() {
        val selectedPC = pcAdapter.getSelectedController()
        if (selectedPC != null) {
            connectToPC(selectedPC)
        } else {
            Toast.makeText(this, "Please select a PC controller first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIState(
        initialized: Boolean,
        deviceConnected: Boolean,
        sessionActive: Boolean
    ) {

        discoverButton.isEnabled = initialized && !sessionActive
        connectButton.isEnabled = initialized && !sessionActive

        discoverPCButton.isEnabled = initialized && !sessionActive
        connectPCButton.isEnabled = initialized && !sessionActive

        startSessionButton.isEnabled = initialized && deviceConnected && !sessionActive
        stopSessionButton.isEnabled = sessionActive
        addMarkerButton.isEnabled = sessionActive

        sessionNameEdit.isEnabled = !sessionActive
        participantIdEdit.isEnabled = !sessionActive
    }

    private fun updateSessionUI(status: SessionStatus) {
        when (status) {
            SessionStatus.RECORDING -> {
                statusText.text = "Recording session active"
                qualityIndicator.visibility = ProgressBar.VISIBLE
            }

            SessionStatus.COMPLETED -> {
                statusText.text = "Session completed successfully"
                qualityIndicator.visibility = ProgressBar.INVISIBLE
            }

            SessionStatus.ERROR -> {
                statusText.text = "Session error occurred"
                qualityIndicator.visibility = ProgressBar.INVISIBLE
            }

            else -> {
                statusText.text = "Ready for recording"
                qualityIndicator.visibility = ProgressBar.INVISIBLE
            }
        }
    }

    private fun updateQualityIndicator(quality: SessionQuality) {
        val color = when (quality.qualityLevel) {
            SessionQuality.QualityLevel.EXCELLENT -> android.graphics.Color.GREEN
            SessionQuality.QualityLevel.GOOD -> android.graphics.Color.BLUE
            SessionQuality.QualityLevel.FAIR -> android.graphics.Color.YELLOW
            SessionQuality.QualityLevel.POOR -> android.graphics.Color.rgb(255, 165, 0)
            SessionQuality.QualityLevel.CRITICAL -> android.graphics.Color.RED
        }

        qualityIndicator.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun switchCamera() {
        lifecycleScope.launch {
            try {
                val currentInfo = rgbCameraRecorder.getCurrentCameraInfo()
                if (!currentInfo.canSwitch) {
                    cameraStatusText.text = "Camera: Cannot switch during recording"
                    return@launch
                }

                switchCameraButton.isEnabled = false
                switchCameraButton.text = "Switching..."

                val success = if (currentInfo.isUsingFrontCamera) {
                    rgbCameraRecorder.switchToBackCamera()
                } else {
                    rgbCameraRecorder.switchToFrontCamera()
                }

                if (success) {
                    val newInfo = rgbCameraRecorder.getCurrentCameraInfo()
                    cameraTypeText.text =
                        if (newInfo.isUsingFrontCamera) "Front Camera" else "Back Camera"
                    cameraStatusText.text = "Camera: Switched successfully"
                } else {
                    cameraStatusText.text = "Camera: Switch failed"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error switching camera", e)
                cameraStatusText.text = "Camera: Switch error"
            } finally {
                switchCameraButton.isEnabled = true
                switchCameraButton.text = "Switch Camera"
            }
        }
    }

    private fun showPermissionError() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires Bluetooth and location permissions to connect to Shimmer devices and discover PC controllers.")
            .setPositiveButton("Settings") { _, _ ->

                finish()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showInitializationError(
        gsrInitialized: Boolean,
        networkInitialized: Boolean,
        cameraInitialized: Boolean = true
    ) {
        val message = buildString {
            append("Component initialization failed:\n\n")
            if (!gsrInitialized) append("• GSR Recorder: Failed\n")
            if (!networkInitialized) append("• Network Controller: Failed\n")
            if (!cameraInitialized) append("• RGB Camera: Failed\n")
            append("\nPlease check Bluetooth, Wi-Fi, and Camera permissions.")
        }

        AlertDialog.Builder(this)
            .setTitle("Initialization Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                initializeComponents()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Cleaning up Unified Sensor Activity")

        lifecycleScope.launch {
            try {
                sessionManager.cleanup()
                gsrRecorder.cleanup()
                networkController.cleanup()
                rgbCameraRecorder.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}
