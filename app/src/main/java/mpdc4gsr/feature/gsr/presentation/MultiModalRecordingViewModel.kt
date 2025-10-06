package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.service.GSRRecorder
import com.mpdc4gsr.gsr.service.SessionManager
import com.mpdc4gsr.gsr.util.TimeUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.shimmerresearch.android.Shimmer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory

class MultiModalRecordingViewModel(application: Application) : BaseViewModel() {
    data class RecordingState(
        val isRecording: Boolean = false,
        val isStartingRecording: Boolean = false,
        val sampleCount: Long = 0,
        val syncMarkCount: Int = 0,
        val recordingDuration: Long = 0,
        val sessionId: String = "",
        val participantId: String? = null
    )

    data class GSRState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val sampleRate: Int = 128,
        val lastSample: GSRSample? = null,
        val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
        val deviceBattery: Int? = null
    )

    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val videoEnabled: Boolean = true,
        val is4KEnabled: Boolean = false,
        val rawCaptureEnabled: Boolean = false,
        val frameRate: Int = 30,
        val resolution: String = "1080p"
    )

    data class NetworkState(
        val isConnected: Boolean = false,
        val controllerInfo: NetworkClient.ControllerInfo? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null
    )

    data class ShimmerDeviceInfo(
        val shimmer: Shimmer?,
        val deviceName: String,
        val macAddress: String,
        val batteryLevel: Int? = null,
        val signalStrength: Int = 0,
        val isConnected: Boolean = false
    )

    data class RecordingConfiguration(
        val enableVideo: Boolean = true,
        val enable4K: Boolean = false,
        val enableRawCapture: Boolean = false,
        val rawFrameRate: Int = 30,
        val gsrSampleRate: Int = 128,
        val participantId: String = "",
        val sessionTemplate: String? = null
    )

    data class CombinedRecordingState(
        val gsrState: GSRState,
        val cameraState: CameraState,
        val networkState: NetworkState
    ) {
        val allSystemsReady: Boolean
            get() = gsrState.isConnected && cameraState.isInitialized
        val anySystemRecording: Boolean
            get() = gsrState.isRecording || cameraState.isRecording
    }

    enum class SignalQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    data class RecordingAction(
        val type: ActionType,
        val message: String? = null,
        val data: Any? = null
    )

    private val context: Context = application.applicationContext
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var networkClient: NetworkClient? = null

    // Recording State Management
    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState
    private val _sessionInfo = MutableLiveData<SessionInfo?>()
    val sessionInfo: LiveData<SessionInfo?> = _sessionInfo

    // Multimodal Sensor States
    private val _gsrState = MutableStateFlow<GSRState>(GSRState())
    val gsrState: StateFlow<GSRState> = _gsrState
    private val _cameraState = MutableStateFlow<CameraState>(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState

    // Device Management
    private val _discoveredDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val discoveredDevices: LiveData<List<ShimmerDeviceInfo>> = _discoveredDevices
    private val _connectedDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val connectedDevices: LiveData<List<ShimmerDeviceInfo>> = _connectedDevices

    // UI State and Actions
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    private val _recordingAction = MutableLiveData<RecordingAction?>()
    val recordingAction: LiveData<RecordingAction?> = _recordingAction

    // Configuration
    private val _recordingConfig =
        MutableStateFlow<RecordingConfiguration>(RecordingConfiguration())
    val recordingConfig: StateFlow<RecordingConfiguration> = _recordingConfig

    // Combined state for UI optimization
    val combinedRecordingState = combine(
        _gsrState, _cameraState, _networkState
    ) { gsrState, cameraState, networkState ->
        CombinedRecordingState(gsrState, cameraState, networkState)
    }

    enum class ActionType {
        RECORDING_STARTED,
        RECORDING_STOPPED,
        SYNC_EVENT_TRIGGERED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        SESSION_EXPORTED,
        ERROR_OCCURRED,
        PERMISSION_REQUIRED
    }

    init {
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    fun initialize() {
        // Kept for compatibility, but initialization now happens in init block
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    private fun initializeRecorders() {
        viewModelScope.launch {
            try {
                // Initialize GSR Recorder
                gsrRecorder = GSRRecorder(context, RealShimmerDeviceFactory(context))
                gsrRecorder.addListener(createGSRListener())
                // Initialize Session Manager
                sessionManager = SessionManager.getInstance(context)
                _statusMessage.value = "Initializing multimodal recording system..."
                // Set initial states
                _recordingState.value = RecordingState()
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to initialize recording system: ${e.message}"
            }
        }
    }

    fun initializeCameraRecorder(rgbCameraRecorder: RgbCameraRecorder) {
        this.rgbCameraRecorder = rgbCameraRecorder
        viewModelScope.launch {
            try {
                val initialized = rgbCameraRecorder.initialize()
                _cameraState.value = _cameraState.value.copy(isInitialized = initialized)
                if (initialized) {
                    _statusMessage.value = "Camera system initialized"
                } else {
                    _error.value = "Camera initialization failed"
                }
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Camera initialization error: ${e.message}"
            }
        }
    }

    fun updateRecordingConfiguration(config: RecordingConfiguration) {
        _recordingConfig.value = config
        // Update camera configuration
        _cameraState.value = _cameraState.value.copy(
            videoEnabled = config.enableVideo,
            is4KEnabled = config.enable4K,
            rawCaptureEnabled = config.enableRawCapture,
            frameRate = config.rawFrameRate
        )
    }

    fun startRecording() {
        val currentState = _recordingState.value
        if (currentState?.isRecording == true || currentState?.isStartingRecording == true) {
            return
        }
        viewModelScope.launch {
            try {
                _recordingState.value = currentState?.copy(isStartingRecording = true)
                _statusMessage.value = "Starting multimodal recording..."
                // Generate session info
                val config = _recordingConfig.value
                val sessionInfo = SessionInfo(
                    sessionId = TimeUtils.generateSessionId("MultiModal"),
                    participantId = config.participantId.takeIf { it.isNotEmpty() },
                    startTime = System.currentTimeMillis()
                )
                // Start GSR recording
                gsrRecorder.startRecording(sessionInfo.sessionId, sessionInfo.participantId)
                // Start camera recording if enabled
                if (config.enableVideo) {
                    rgbCameraRecorder?.startRecording(sessionInfo.sessionId)
                    _cameraState.value = _cameraState.value.copy(isRecording = true)
                }
                // Update states
                _sessionInfo.value = sessionInfo
                _recordingState.value = RecordingState(
                    isRecording = true,
                    isStartingRecording = false,
                    sessionId = sessionInfo.sessionId,
                    participantId = sessionInfo.participantId
                )
                _gsrState.value = _gsrState.value.copy(isRecording = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STARTED,
                    message = "Recording started for session ${sessionInfo.sessionId}"
                )
            } catch (e: Exception) {
                _recordingState.value = currentState?.copy(isStartingRecording = false)
                _error.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Stopping multimodal recording..."
                // Stop GSR recording
                gsrRecorder.stopRecording()
                // Stop camera recording
                rgbCameraRecorder?.stopRecording()
                // Update final session info
                val finalSession = _sessionInfo.value?.copy(
                    endTime = System.currentTimeMillis()
                )
                // Update states
                _recordingState.value = RecordingState()
                _gsrState.value = _gsrState.value.copy(isRecording = false)
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                _sessionInfo.value = finalSession
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STOPPED,
                    message = "Recording stopped. Session saved.",
                    data = finalSession
                )
            } catch (e: Exception) {
                _error.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun triggerSyncEvent() {
        val currentState = _recordingState.value
        if (currentState?.isRecording != true) {
            _error.value = "Cannot trigger sync event when not recording"
            return
        }
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val syncMark = SyncMark(
                    timestamp = timestamp,
                    utcTimestamp = timestamp,
                    eventType = "USER_TRIGGER",
                    sessionId = currentState.sessionId
                )
                // Add sync mark to GSR data
                gsrRecorder.addSyncMark("USER_TRIGGER", "Manual sync event")
                // Add sync mark to camera data if recording
                if (_cameraState.value.isRecording) {
                    rgbCameraRecorder?.addSyncMarker(
                        "USER_TRIGGER",
                        timestamp * 1_000_000,
                        emptyMap()
                    )
                }
                // Update state
                _recordingState.value = currentState.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
                _recordingAction.value = RecordingAction(
                    type = ActionType.SYNC_EVENT_TRIGGERED,
                    message = "Sync event USER_TRIGGER triggered"
                )
            } catch (e: Exception) {
                _error.value = "Failed to trigger sync event: ${e.message}"
            }
        }
    }

    fun discoverDevices() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Discovering Shimmer devices..."
                // Simulate device discovery
                val devices = discoverShimmerDevices()
                _discoveredDevices.value = devices
                _statusMessage.value = "Found ${devices.size} Shimmer device(s)"
            } catch (e: Exception) {
                _error.value = "Device discovery failed: ${e.message}"
            }
        }
    }

    fun connectToDevice(deviceInfo: ShimmerDeviceInfo) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Connecting to ${deviceInfo.deviceName}..."
                // Simulate device connection
                kotlinx.coroutines.delay(2000)
                val connectedDevice = deviceInfo.copy(isConnected = true)
                val currentConnected = _connectedDevices.value?.toMutableList() ?: mutableListOf()
                currentConnected.add(connectedDevice)
                _connectedDevices.value = currentConnected
                _gsrState.value = _gsrState.value.copy(isConnected = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.DEVICE_CONNECTED,
                    message = "Connected to ${deviceInfo.deviceName}"
                )
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to connect to device: ${e.message}"
            }
        }
    }

    private suspend fun discoverShimmerDevices(): List<ShimmerDeviceInfo> {
        // Simulate device discovery - use null placeholders for now as this is discovery phase
        return listOf(
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #001",
                macAddress = "00:11:22:AA:BB:CC",
                batteryLevel = 85,
                signalStrength = 75
            ),
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #002",
                macAddress = "00:11:22:AA:BB:DD",
                batteryLevel = 92,
                signalStrength = 88
            )
        )
    }

    private fun createGSRListener(): GSRRecorder.GSRRecordingListener {
        return object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording started"
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording stopped"
            }

            override fun onSampleRecorded(sample: GSRSample) {
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    sampleCount = currentState.sampleCount + 1
                )
                _gsrState.value = _gsrState.value.copy(lastSample = sample)
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                // Handle sync mark addition
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
            }

            override fun onError(error: String) {
                _error.value = "GSR recording error: $error"
            }
        }
    }

    private fun updateSystemReadiness() {
        val gsrReady = _gsrState.value.isConnected
        val cameraReady = _cameraState.value.isInitialized
        _statusMessage.value = when {
            gsrReady && cameraReady -> "All systems ready for recording"
            gsrReady -> "GSR ready, initializing camera..."
            cameraReady -> "Camera ready, connect GSR device..."
            else -> "Initializing recording systems..."
        }
    }

    private fun generateDefaultSessionId() {
        val config = _recordingConfig.value
        val defaultParticipantId = TimeUtils.generateSessionId("MultiModal")
        _recordingConfig.value = config.copy(participantId = defaultParticipantId)
    }

    fun clearAction() {
        _recordingAction.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                if (_recordingState.value?.isRecording == true) {
                    stopRecording()
                }
                rgbCameraRecorder?.cleanup()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val TAG = "MultiModalRecordingViewModel"
    }
}