package mpdc4gsr.feature.capture.gsr.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.hardware.api.RecordingStatus
import mpdc4gsr.core.hardware.gsr.model.DeviceInfo
import mpdc4gsr.core.hardware.gsr.model.GSRSample
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.feature.capture.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.connectivity.data.NetworkClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MultiModalRecordingViewModel
@Inject
constructor(
    @ApplicationContext private val application: Application,
) : BaseViewModel() {
    data class RecordingState(
        val isRecording: Boolean = false,
        val isStartingRecording: Boolean = false,
        val sampleCount: Long = 0,
        val syncMarkCount: Int = 0,
        val recordingDuration: Long = 0,
        val sessionId: String = "",
        val participantId: String? = null,
    )

    data class GSRState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val sampleRate: Int = 128,
        val lastSample: GSRSample? = null,
        val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
        val deviceBattery: Int? = null,
    )

    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val videoEnabled: Boolean = true,
        val is4KEnabled: Boolean = false,
        val rawCaptureEnabled: Boolean = false,
        val frameRate: Int = 30,
        val resolution: String = "1080p",
    )

    data class NetworkState(
        val isConnected: Boolean = false,
        val controllerInfo: NetworkClient.ControllerInfo? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
    )

    data class SensorDeviceInfo(
        val info: DeviceInfo,
        val isConnected: Boolean = false,
        val lastSeen: Long = System.currentTimeMillis(),
    ) {
        val displayName: String = info.displayName
        val signalStrength: Int = info.rssi
    }

    data class RecordingConfiguration(
        val enableVideo: Boolean = true,
        val enable4K: Boolean = false,
        val enableRawCapture: Boolean = false,
        val rawFrameRate: Int = 30,
        val gsrSampleRate: Int = 128,
        val participantId: String = "",
        val sessionTemplate: String? = null,
    )

    data class RecordingSession(
        val sessionId: String,
        val participantId: String?,
        val startTime: Long,
        var endTime: Long? = null,
        val metadata: MutableMap<String, String> = mutableMapOf(),
        val syncMarks: MutableList<SessionSyncMark> = mutableListOf(),
    )

    data class SessionSyncMark(
        val timestamp: Long,
        val eventType: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    data class CombinedRecordingState(
        val gsrState: GSRState,
        val cameraState: CameraState,
        val networkState: NetworkState,
    ) {
        val allSystemsReady: Boolean
            get() = gsrState.isConnected && cameraState.isInitialized
        val anySystemRecording: Boolean
            get() = gsrState.isRecording || cameraState.isRecording
    }

    enum class SignalQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        UNKNOWN,
    }

    data class RecordingAction(
        val type: ActionType,
        val message: String? = null,
        val data: Any? = null,
    )

    enum class ActionType {
        RECORDING_STARTED,
        RECORDING_STOPPED,
        SYNC_EVENT_TRIGGERED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        SESSION_EXPORTED,
        ERROR_OCCURRED,
        PERMISSION_REQUIRED,
    }

    private val context: Context = application.applicationContext
    private var gsrRecorder: GSRSensorRecorder? = null
    private var recorderJobs: MutableList<Job> = mutableListOf()
    private var rgbCameraRecorder: RgbCameraRecorder? = null

    private val _recordingState = MutableLiveData(RecordingState())
    val recordingState: LiveData<RecordingState> = _recordingState
    private val _sessionInfo = MutableLiveData<RecordingSession?>()
    val sessionInfo: LiveData<RecordingSession?> = _sessionInfo

    private val _gsrState = MutableStateFlow(GSRState())
    val gsrState: StateFlow<GSRState> = _gsrState.asStateFlow()
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _recordingConfig = MutableStateFlow(RecordingConfiguration())
    val recordingConfig: StateFlow<RecordingConfiguration> = _recordingConfig.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _recordingAction = MutableStateFlow<RecordingAction?>(null)
    val recordingAction: StateFlow<RecordingAction?> = _recordingAction.asStateFlow()

    private val _discoveredDevices = MutableLiveData<List<SensorDeviceInfo>>(emptyList())
    val discoveredDevices: LiveData<List<SensorDeviceInfo>> = _discoveredDevices
    private val _connectedDevices = MutableLiveData<List<SensorDeviceInfo>>(emptyList())
    val connectedDevices: LiveData<List<SensorDeviceInfo>> = _connectedDevices

    private val _combinedRecordingState: StateFlow<CombinedRecordingState> =
        combine(_gsrState, _cameraState, _networkState) { gsr, camera, network ->
            CombinedRecordingState(gsr, camera, network)
        }.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            CombinedRecordingState(GSRState(), CameraState(), NetworkState()),
        )
    val combinedRecordingState: StateFlow<CombinedRecordingState> = _combinedRecordingState

    init {
        initializeGsrRecorder()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    fun initialize() {
        initializeGsrRecorder()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    private fun initializeGsrRecorder() {
        if (gsrRecorder != null) return
        viewModelScope.launch {
            try {
                val recorder = GSRSensorRecorder(context)
                val initialized = recorder.initialize()
                if (!initialized) {
                    _error.value = "Failed to initialise GSR recorder"
                    return@launch
                }
                gsrRecorder = recorder
                observeRecorder(recorder)
                _statusMessage.value = "GSR system initialised"
            } catch (t: Throwable) {
                _error.value = "GSR initialisation error: ${t.message}"
                AppLogger.e(TAG, "Failed to initialise GSRSensorRecorder", t)
            }
        }
    }

    private fun observeRecorder(recorder: GSRSensorRecorder) {
        recorderJobs +=
            viewModelScope.launch {
                recorder.isConnectedFlow.collect { connected ->
                    _gsrState.value = _gsrState.value.copy(isConnected = connected)
                    updateSystemReadiness()
                }
            }
        recorderJobs +=
            viewModelScope.launch {
                recorder.latestSample.collect { sample ->
                    if (sample != null) {
                        _gsrState.value = _gsrState.value.copy(lastSample = sample)
                        val current = _recordingState.value
                        if (current?.isRecording == true) {
                            _recordingState.value = current.copy(sampleCount = current.sampleCount + 1)
                        }
                    }
                }
            }
        recorderJobs +=
            viewModelScope.launch {
                recorder.connectionQualityFlow().collect { quality ->
                    _gsrState.value =
                        _gsrState.value.copy(signalQuality = mapConnectionQuality(quality))
                }
            }
        recorderJobs +=
            viewModelScope.launch {
                recorder.getStatusFlow().collect { status ->
                    handleRecorderStatus(status)
                }
            }
        recorderJobs +=
            viewModelScope.launch {
                recorder.getErrorFlow().collect { sensorError ->
                    _error.value = "GSR error: ${sensorError.errorMessage}"
                    AppLogger.e(TAG, "GSR error ${sensorError.errorType}", null)
                }
            }
    }

    private fun handleRecorderStatus(status: RecordingStatus) {
        _gsrState.value =
            _gsrState.value.copy(
                isRecording = status.isRecording,
                sampleRate = status.currentDataRate.toInt().coerceAtLeast(1),
            )
        val current = _recordingState.value
        if (current != null) {
            val updatedDuration =
                if (status.isRecording) {
                    System.currentTimeMillis() - (_sessionInfo.value?.startTime ?: System.currentTimeMillis())
                } else {
                    current.recordingDuration
                }
            _recordingState.value =
                current.copy(
                    isRecording = status.isRecording,
                    recordingDuration = updatedDuration,
                )
        }
    }

    fun initializeCameraRecorder(rgbCameraRecorder: RgbCameraRecorder) {
        this.rgbCameraRecorder = rgbCameraRecorder
        viewModelScope.launch {
            try {
                val initialized = rgbCameraRecorder.initialize()
                _cameraState.value = _cameraState.value.copy(isInitialized = initialized)
                if (initialized) {
                    _statusMessage.value = "Camera system initialised"
                } else {
                    _error.value = "Camera initialisation failed"
                }
                updateSystemReadiness()
            } catch (t: Throwable) {
                _error.value = "Camera initialisation error: ${t.message}"
                AppLogger.e(TAG, "Camera initialisation failed", t)
            }
        }
    }

    fun updateRecordingConfiguration(config: RecordingConfiguration) {
        _recordingConfig.value = config
        _cameraState.value =
            _cameraState.value.copy(
                videoEnabled = config.enableVideo,
                is4KEnabled = config.enable4K,
                rawCaptureEnabled = config.enableRawCapture,
                frameRate = config.rawFrameRate,
            )
    }

    fun startRecording() {
        val currentState = _recordingState.value ?: RecordingState()
        if (currentState.isRecording || currentState.isStartingRecording) return
        val recorder =
            gsrRecorder ?: run {
                _error.value = "GSR recorder unavailable"
                return
            }
        viewModelScope.launch {
            try {
                _recordingState.value = currentState.copy(isStartingRecording = true)
                _statusMessage.value = "Starting multimodal recording..."
                val config = _recordingConfig.value
                val sessionId = generateSessionId("MultiModal")
                val sessionDirectory = createSessionDirectory(sessionId)
                val recordingStarted = recorder.startRecording(sessionDirectory.absolutePath)
                if (!recordingStarted) {
                    _recordingState.value = currentState.copy(isStartingRecording = false)
                    _error.value = "Failed to start GSR recording"
                    return@launch
                }
                if (config.enableVideo) {
                    val videoStarted = rgbCameraRecorder?.startRecording(sessionId) ?: true
                    _cameraState.value = _cameraState.value.copy(isRecording = videoStarted)
                }
                val session =
                    RecordingSession(
                        sessionId = sessionId,
                        participantId = config.participantId.takeIf { it.isNotBlank() },
                        startTime = System.currentTimeMillis(),
                    )
                _sessionInfo.value = session
                _recordingState.value =
                    RecordingState(
                        isRecording = true,
                        isStartingRecording = false,
                        sessionId = session.sessionId,
                        participantId = session.participantId,
                    )
                _gsrState.value = _gsrState.value.copy(isRecording = true)
                _recordingAction.value =
                    RecordingAction(
                        type = ActionType.RECORDING_STARTED,
                        message = "Recording started for session ${session.sessionId}",
                        data = session,
                    )
                _statusMessage.value = "Recording in progress"
            } catch (t: Throwable) {
                _recordingState.value = currentState.copy(isStartingRecording = false)
                _error.value = "Failed to start recording: ${t.message}"
                AppLogger.e(TAG, "Failed to start recording", t)
            }
        }
    }

    fun stopRecording() {
        val recorder = gsrRecorder ?: return
        viewModelScope.launch {
            try {
                _statusMessage.value = "Stopping multimodal recording..."
                recorder.stopRecording()
                rgbCameraRecorder?.stopRecording()
                val finalSession =
                    _sessionInfo.value?.copy(endTime = System.currentTimeMillis())?.also { session ->
                        session.syncMarks.add(
                            SessionSyncMark(
                                timestamp = session.endTime!!,
                                eventType = "SESSION_END",
                            ),
                        )
                        _sessionInfo.value = session
                    }
                _recordingState.value = RecordingState()
                _gsrState.value = _gsrState.value.copy(isRecording = false)
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                _recordingAction.value =
                    RecordingAction(
                        type = ActionType.RECORDING_STOPPED,
                        message = "Recording stopped. Session saved.",
                        data = finalSession,
                    )
                _statusMessage.value = "Recording stopped"
            } catch (t: Throwable) {
                _error.value = "Failed to stop recording: ${t.message}"
                AppLogger.e(TAG, "Failed to stop recording", t)
            }
        }
    }

    fun triggerSyncEvent() {
        val currentState = _recordingState.value
        if (currentState?.isRecording != true) {
            _error.value = "Cannot trigger sync event when not recording"
            return
        }
        val recorder = gsrRecorder ?: return
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                recorder.addSyncMarker(
                    markerType = "USER_TRIGGER",
                    timestampNs = timestamp * 1_000_000,
                    metadata = emptyMap(),
                )
                if (_cameraState.value.isRecording) {
                    rgbCameraRecorder?.addSyncMarker(
                        markerType = "USER_TRIGGER",
                        timestampNs = timestamp * 1_000_000,
                        metadata = emptyMap(),
                    )
                }
                val session = _sessionInfo.value
                session?.let {
                    it.syncMarks.add(SessionSyncMark(timestamp, "USER_TRIGGER"))
                    _sessionInfo.value = it
                }
                _recordingState.value =
                    currentState.copy(syncMarkCount = currentState.syncMarkCount + 1)
                _recordingAction.value =
                    RecordingAction(
                        type = ActionType.SYNC_EVENT_TRIGGERED,
                        message = "Sync event USER_TRIGGER triggered",
                    )
            } catch (t: Throwable) {
                _error.value = "Failed to trigger sync event: ${t.message}"
                AppLogger.e(TAG, "Failed to trigger sync event", t)
            }
        }
    }

    fun discoverDevices() {
        val recorder =
            gsrRecorder ?: run {
                _error.value = "GSR recorder unavailable"
                return
            }
        viewModelScope.launch {
            try {
                _statusMessage.value = "Scanning for GSR devices..."
                val success = recorder.startDeviceDiscovery()
                delay(500) // allow discovery results to propagate
                val devices = recorder.getDiscoveredDevices()
                val deviceInfos =
                    devices.map { device ->
                        SensorDeviceInfo(
                            info = device,
                            isConnected = false,
                            lastSeen = System.currentTimeMillis(),
                        )
                    }
                _discoveredDevices.value = deviceInfos
                _statusMessage.value =
                    if (success) {
                        "Found ${deviceInfos.size} device(s)"
                    } else {
                        "GSR device discovery failed"
                    }
            } catch (t: Throwable) {
                _error.value = "Device discovery failed: ${t.message}"
                AppLogger.e(TAG, "Device discovery failed", t)
            }
        }
    }

    fun connectToDevice(deviceInfo: SensorDeviceInfo) {
        val recorder =
            gsrRecorder ?: run {
                _error.value = "GSR recorder unavailable"
                return
            }
        viewModelScope.launch {
            try {
                _statusMessage.value = "Connecting to ${deviceInfo.displayName}..."
                val connected = recorder.connectToDevice(deviceInfo.info)
                if (connected) {
                    val updatedConnected =
                        (_connectedDevices.value ?: emptyList())
                            .filterNot { it.info.address == deviceInfo.info.address }
                            .plus(deviceInfo.copy(isConnected = true))
                    _connectedDevices.value = updatedConnected
                    _gsrState.value = _gsrState.value.copy(isConnected = true)
                    _recordingAction.value =
                        RecordingAction(
                            type = ActionType.DEVICE_CONNECTED,
                            message = "Connected to ${deviceInfo.displayName}",
                            data = deviceInfo,
                        )
                    _statusMessage.value = "Connected to ${deviceInfo.displayName}"
                } else {
                    _error.value = "Failed to connect to ${deviceInfo.displayName}"
                    _statusMessage.value = "Connection failed"
                }
                updateSystemReadiness()
            } catch (t: Throwable) {
                _error.value = "Failed to connect to device: ${t.message}"
                AppLogger.e(TAG, "Failed to connect to device", t)
            }
        }
    }

    private fun updateSystemReadiness() {
        val gsrReady = _gsrState.value.isConnected
        val cameraReady = _cameraState.value.isInitialized
        _statusMessage.value =
            when {
                gsrReady && cameraReady -> "All systems ready for recording"
                gsrReady -> "GSR ready, initialising camera..."
                cameraReady -> "Camera ready, connect GSR device..."
                else -> "Initialising recording systems..."
            }
    }

    private fun generateDefaultSessionId() {
        val current = _recordingConfig.value
        val defaultParticipantId = generateSessionId("MultiModal")
        _recordingConfig.value = current.copy(participantId = defaultParticipantId)
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
                gsrRecorder?.cleanup()
                recorderJobs.forEach { it.cancel() }
                recorderJobs.clear()
                rgbCameraRecorder?.cleanup()
            } catch (t: Throwable) {
                AppLogger.w(TAG, "Cleanup error in MultiModalRecordingViewModel", t)
            }
        }
    }

    private fun mapConnectionQuality(quality: Double): SignalQuality =
        when {
            quality >= 0.9 -> SignalQuality.EXCELLENT
            quality >= 0.7 -> SignalQuality.GOOD
            quality >= 0.5 -> SignalQuality.FAIR
            quality > 0.0 -> SignalQuality.POOR
            else -> SignalQuality.UNKNOWN
        }

    private fun generateSessionId(prefix: String): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "${prefix}_${formatter.format(Date())}"
    }

    private fun createSessionDirectory(sessionId: String): File {
        val root = context.getExternalFilesDir("multimodal_sessions") ?: context.filesDir
        return File(root, sessionId).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    companion object {
        private const val TAG = "MultiModalRecordingViewModel"
    }
}

