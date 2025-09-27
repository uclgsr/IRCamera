package mpdc4gsr.sensors.gsr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMultiModalRecordingBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.util.TimeUtil
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import kotlinx.coroutines.launch
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.sensors.RgbCameraRecorder

/**
 * MultiModalRecordingActivity - Advanced MVVM Implementation
 * Demonstrates complex multimodal sensor coordination with proper architectural separation
 */
class MultiModalRecordingActivity : BaseViewModelActivity<MultiModalRecordingViewModel>() {
    
    companion object {
        private const val TAG = "MultiModalActivity"

        fun start(context: Context) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java)
            context.startActivity(intent)
        }

        fun startWithTemplate(context: Context, templateId: String) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java).apply {
                putExtra("template_id", templateId)
            }
            context.startActivity(intent)
        }

        fun startRecording(context: Context, sessionInfo: SessionInfo) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java).apply {
                putExtra("session_info", sessionInfo)
                putExtra("auto_start", true)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityMultiModalRecordingBinding
    private lateinit var permissionController: PermissionController
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    
    // Service connection for enhanced recording
    private var enhancedRecordingService: com.mpdc4gsr.gsr.service.EnhancedRecordingService? = null
    private var isServiceBound = false

    override fun providerVMClass(): Class<MultiModalRecordingViewModel> = MultiModalRecordingViewModel::class.java

    override fun initContentView() = R.layout.activity_multi_modal_recording

    override fun initView() {
        binding = ActivityMultiModalRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializePermissions()
        initializeCamera()
        setupUI()
        setupObservers()
        bindToEnhancedRecordingService()
        
        viewModel.initialize(this)
        
        // Handle auto-start recording if requested
        handleAutoStart()
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    private fun initializePermissions() {
        permissionController = PermissionController(this)
        permissionController.initialize()
    }

    private fun initializeCamera() {
        lifecycleScope.launch {
            try {
                val previewView = binding.previewView.previewView
                rgbCameraRecorder = RgbCameraRecorder(
                    context = this@MultiModalRecordingActivity,
                    lifecycleOwner = this@MultiModalRecordingActivity,
                    previewView = previewView,
                    useFrontCamera = false
                )

                // Setup tap-to-focus on the preview
                binding.previewView.onTapToFocus = { normalizedX, normalizedY ->
                    rgbCameraRecorder?.triggerTapToFocus(normalizedX, normalizedY)
                }

                // Initialize camera and notify ViewModel
                rgbCameraRecorder?.let { camera ->
                    viewModel.initializeCameraRecorder(camera)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                Toast.makeText(this@MultiModalRecordingActivity, "Camera initialization failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUI() {
        setupParticipantInput()
        setupVideoControls()
        setupRecordingControls()
        setupDeviceControls()
    }

    private fun setupParticipantInput() {
        // Set default participant ID
        binding.participantIdInput.setText(TimeUtil.generateSessionId("MultiModal"))
    }

    private fun setupVideoControls() {
        with(binding) {
            // Initialize switches
            enableVideoSwitch.isChecked = true
            enable4kSwitch.isChecked = false
            enableRawCaptureSwitch.isChecked = false

            // Setup frame rate spinner
            val frameRateAdapter = ArrayAdapter(
                this@MultiModalRecordingActivity,
                android.R.layout.simple_spinner_item,
                listOf("30 fps", "15 fps", "10 fps", "5 fps")
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            rawFrameRateSpinner.adapter = frameRateAdapter
            rawFrameRateSpinner.isEnabled = false

            // Setup listeners
            enableVideoSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateRecordingConfiguration()
            }
            
            enable4kSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateRecordingConfiguration()
            }
            
            enableRawCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
                rawFrameRateSpinner.isEnabled = isChecked
                updateRecordingConfiguration()
            }
            
            rawFrameRateSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    updateRecordingConfiguration()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            })
        }
    }

    private fun setupRecordingControls() {
        with(binding) {
            startButton.setOnClickListener { 
                updateRecordingConfiguration()
                viewModel.startRecording() 
            }
            stopButton.setOnClickListener { 
                viewModel.stopRecording() 
            }
            syncButton.setOnClickListener { 
                viewModel.triggerSyncEvent() 
            }
        }
    }

    private fun setupDeviceControls() {
        binding.scanDevicesButton?.setOnClickListener {
            viewModel.discoverDevices()
        }
    }

    private fun setupObservers() {
        // Recording state observer
        viewModel.recordingState.observe(this) { recordingState ->
            updateRecordingUI(recordingState)
        }

        // Session info observer
        viewModel.sessionInfo.observe(this) { sessionInfo ->
            updateSessionInfoUI(sessionInfo)
        }

        // Combined sensor states observer
        viewModel.combinedRecordingState.asLiveData().observe(this) { combinedState ->
            updateSensorStatesUI(combinedState)
        }

        // Device management observers
        viewModel.discoveredDevices.observe(this) { devices ->
            updateDiscoveredDevicesUI(devices)
        }

        viewModel.connectedDevices.observe(this) { devices ->
            updateConnectedDevicesUI(devices)
        }

        // Status and error observers
        viewModel.statusMessage.observe(this) { message ->
            binding.statusText.text = message
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Recording action observer
        viewModel.recordingAction.observe(this) { action ->
            action?.let {
                handleRecordingAction(it)
                viewModel.clearAction()
            }
        }

        // Recording configuration observer
        viewModel.recordingConfig.asLiveData().observe(this) { config ->
            updateConfigurationUI(config)
        }
    }

    private fun updateRecordingConfiguration() {
        val config = MultiModalRecordingViewModel.RecordingConfiguration(
            enableVideo = binding.enableVideoSwitch.isChecked,
            enable4K = binding.enable4kSwitch.isChecked,
            enableRawCapture = binding.enableRawCaptureSwitch.isChecked,
            rawFrameRate = getSelectedFrameRate(),
            participantId = binding.participantIdInput.text.toString(),
            sessionTemplate = intent.getStringExtra("template_id")
        )
        viewModel.updateRecordingConfiguration(config)
    }

    private fun getSelectedFrameRate(): Int {
        return when (binding.rawFrameRateSpinner.selectedItemPosition) {
            0 -> 30
            1 -> 15
            2 -> 10
            3 -> 5
            else -> 30
        }
    }

    private fun updateRecordingUI(recordingState: MultiModalRecordingViewModel.RecordingState) {
        with(binding) {
            startButton.isEnabled = !recordingState.isRecording && !recordingState.isStartingRecording
            stopButton.isEnabled = recordingState.isRecording
            syncButton.isEnabled = recordingState.isRecording
            
            progressBar.isVisible = recordingState.isStartingRecording || recordingState.isRecording
            
            // Update sample count and sync marks
            sampleCountText?.text = "Samples: ${recordingState.sampleCount}"
            syncMarkCountText?.text = "Sync marks: ${recordingState.syncMarkCount}"
            
            // Update session info
            sessionIdText?.text = "Session: ${recordingState.sessionId.takeIf { it.isNotEmpty() } ?: "Not started"}"
            
            // Disable configuration during recording
            val configEnabled = !recordingState.isRecording && !recordingState.isStartingRecording
            enableVideoSwitch.isEnabled = configEnabled
            enable4kSwitch.isEnabled = configEnabled
            enableRawCaptureSwitch.isEnabled = configEnabled
            rawFrameRateSpinner.isEnabled = configEnabled && enableRawCaptureSwitch.isChecked
            participantIdInput.isEnabled = configEnabled
        }
    }

    private fun updateSessionInfoUI(sessionInfo: SessionInfo?) {
        sessionInfo?.let { session ->
            binding.sessionDetailsText?.text = buildString {
                append("Session: ${session.sessionId}\n")
                append("Participant: ${session.participantId ?: "N/A"}\n")
                append("Started: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(session.startTime))}")
                session.endTime?.let { endTime ->
                    append("\nEnded: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(endTime))}")
                    append("\nDuration: ${(endTime - session.startTime) / 1000}s")
                }
            }
        }
    }

    private fun updateSensorStatesUI(combinedState: MultiModalRecordingViewModel.CombinedRecordingState) {
        with(binding) {
            // GSR state
            gsrStatusText?.text = "GSR: ${if (combinedState.gsrState.isConnected) "Connected" else "Disconnected"}"
            gsrStatusText?.setTextColor(
                if (combinedState.gsrState.isConnected) 
                    android.graphics.Color.parseColor("#4caf50") 
                else 
                    android.graphics.Color.parseColor("#f44336")
            )
            
            // Camera state
            cameraStatusText?.text = "Camera: ${if (combinedState.cameraState.isInitialized) "Ready" else "Initializing"}"
            cameraStatusText?.setTextColor(
                if (combinedState.cameraState.isInitialized) 
                    android.graphics.Color.parseColor("#4caf50") 
                else 
                    android.graphics.Color.parseColor("#ff9800")
            )
            
            // Overall system status
            systemStatusText?.text = when {
                combinedState.allSystemsReady -> "All systems ready"
                combinedState.anySystemRecording -> "Recording in progress"
                else -> "Systems initializing"
            }
        }
    }

    private fun updateDiscoveredDevicesUI(devices: List<MultiModalRecordingViewModel.ShimmerDeviceInfo>) {
        binding.deviceCountText?.text = "Discovered devices: ${devices.size}"
        
        // Update device list (simplified for demo)
        val deviceNames = devices.map { "${it.deviceName} (${it.macAddress})" }
        // In a real implementation, this would update a RecyclerView adapter
    }

    private fun updateConnectedDevicesUI(devices: List<MultiModalRecordingViewModel.ShimmerDeviceInfo>) {
        binding.connectedDeviceCountText?.text = "Connected devices: ${devices.size}"
        
        devices.firstOrNull()?.let { device ->
            binding.connectedDeviceText?.text = "Connected: ${device.deviceName}"
            binding.batteryLevelText?.text = "Battery: ${device.batteryLevel ?: "Unknown"}%"
        }
    }

    private fun updateConfigurationUI(config: MultiModalRecordingViewModel.RecordingConfiguration) {
        // Update UI to reflect configuration changes
        binding.configurationSummaryText?.text = buildString {
            append("Video: ${if (config.enableVideo) "Enabled" else "Disabled"}")
            if (config.enable4K) append(" (4K)")
            if (config.enableRawCapture) append(" + RAW @ ${config.rawFrameRate}fps")
            append("\nGSR: ${config.gsrSampleRate} Hz")
        }
    }

    private fun handleRecordingAction(action: MultiModalRecordingViewModel.RecordingAction) {
        when (action.type) {
            MultiModalRecordingViewModel.ActionType.RECORDING_STARTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
                binding.recordingIndicator?.isVisible = true
            }
            MultiModalRecordingViewModel.ActionType.RECORDING_STOPPED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
                binding.recordingIndicator?.isVisible = false
                
                // Optionally navigate to session details
                val sessionInfo = action.data as? SessionInfo
                sessionInfo?.let {
                    // Could launch SessionDetailActivity here
                }
            }
            MultiModalRecordingViewModel.ActionType.SYNC_EVENT_TRIGGERED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
                // Visual feedback for sync event
                binding.syncIndicator?.apply {
                    isVisible = true
                    postDelayed({ isVisible = false }, 1000)
                }
            }
            MultiModalRecordingViewModel.ActionType.DEVICE_CONNECTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Handle other actions
            }
        }
    }

    private fun handleAutoStart() {
        if (intent.getBooleanExtra("auto_start", false)) {
            // Auto-start recording after brief delay
            binding.root.postDelayed({
                viewModel.startRecording()
            }, 2000)
        }
    }

    private fun bindToEnhancedRecordingService() {
        val serviceIntent = Intent(this, com.mpdc4gsr.gsr.service.EnhancedRecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? com.mpdc4gsr.gsr.service.EnhancedRecordingService.EnhancedRecordingBinder
            enhancedRecordingService = binder?.getService()
            isServiceBound = true
            Log.i(TAG, "Enhanced recording service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            enhancedRecordingService = null
            isServiceBound = false
            Log.i(TAG, "Enhanced recording service disconnected")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
