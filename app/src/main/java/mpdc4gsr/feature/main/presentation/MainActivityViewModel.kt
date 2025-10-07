package mpdc4gsr.feature.main.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.RecordingService
import mpdc4gsr.core.SessionManager
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.NetworkController
import mpdc4gsr.feature.thermal.ui.ThermalRecorder
import com.mpdc4gsr.gsr.service.SessionManager as GSRSessionManager

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainActivityViewModel"
    }

    // A single UiState class can be used to hold all UI-related state.
    // However, to align with the existing structure, individual flows are maintained.
    // For simplicity, a new state for the current navigation page is added.
    private val _currentPage =
        MutableStateFlow(1) // PAGE_MAIN = 1 (0: Gallery, 1: Main, 2: Settings, 3: Mine)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // State properties using StateFlow for better coroutine support and lifecycle awareness
    private val _gsrConnectionState = MutableStateFlow(GSRConnectionState.DISCONNECTED)
    val gsrConnectionState: StateFlow<GSRConnectionState> = _gsrConnectionState.asStateFlow()
    private val _gsrBatteryLevel = MutableStateFlow<Int?>(null)
    val gsrBatteryLevel: StateFlow<Int?> = _gsrBatteryLevel.asStateFlow()

    // GSR Data StateFlow for real-time sensor values
    data class GSRDataState(
        val currentValue: Float = 0f,
        val batteryLevel: Int = 0,
        val recentReadings: List<Float> = emptyList(),
        val averageValue: Float = 0f,
        val minValue: Float = 0f,
        val maxValue: Float = 0f
    )

    private val _gsrData = MutableStateFlow(GSRDataState())
    val gsrData: StateFlow<GSRDataState> = _gsrData.asStateFlow()
    private val _networkConnectionState = MutableStateFlow(NetworkConnectionState.DISCONNECTED)
    val networkConnectionState: StateFlow<NetworkConnectionState> =
        _networkConnectionState.asStateFlow()
    private val _connectedControllerInfo = MutableStateFlow<NetworkClient.ControllerInfo?>(null)
    val connectedControllerInfo: StateFlow<NetworkClient.ControllerInfo?> =
        _connectedControllerInfo.asStateFlow()
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    val currentSession: StateFlow<SessionInfo?> = _currentSession.asStateFlow()

    // Use SharedFlow for one-time events like showing dialogs or toasts.
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    // Sensor state tracking with StateFlow
    private val _rgbCameraState = MutableStateFlow(SensorState())
    val rgbCameraState: StateFlow<SensorState> = _rgbCameraState.asStateFlow()
    private val _thermalCameraState = MutableStateFlow(SensorState())
    val thermalCameraState: StateFlow<SensorState> = _thermalCameraState.asStateFlow()
    private val _gsrSensorState = MutableStateFlow(SensorState())
    val gsrSensorState: StateFlow<SensorState> = _gsrSensorState.asStateFlow()

    // Recording control states with StateFlow
    private val _isRemoteTriggered = MutableStateFlow(false)
    val isRemoteTriggered: StateFlow<Boolean> = _isRemoteTriggered.asStateFlow()

    // Camera manual control states with StateFlow
    private val _exposureLocked = MutableStateFlow(false)
    val exposureLocked: StateFlow<Boolean> = _exposureLocked.asStateFlow()
    private val _focusLocked = MutableStateFlow(false)
    val focusLocked: StateFlow<Boolean> = _focusLocked.asStateFlow()
    private val _exposureCompensation = MutableStateFlow(0.0f)
    val exposureCompensation: StateFlow<Float> = _exposureCompensation.asStateFlow()

    // --- Component Properties (to be managed by Repositories in a larger app) ---
    private var serviceBinder: RecordingService.RecordingServiceBinder? = null
    private var gsrSensorRecorder: GSRSensorRecorder? = null
    private var unifiedGSRRecorder: UnifiedGSRRecorder? = null
    private var thermalRecorder: ThermalRecorder? = null
    private var networkClient: NetworkClient? = null
    private var networkController: NetworkController? = null
    private var sessionManager: SessionManager? = null
    private var gsrSessionManager: GSRSessionManager? = null

    // --- Enums and Data Classes ---
    enum class GSRConnectionState { DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, ERROR }
    enum class NetworkConnectionState { DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, ERROR }
    enum class SessionState { IDLE, STARTING, RECORDING, PAUSED, STOPPING, ERROR }
    enum class SensorStatus { DISCONNECTED, CONNECTING, CONNECTED, STREAMING, ERROR, SIMULATION }
    data class SensorState(
        val status: SensorStatus = SensorStatus.DISCONNECTED,
        val message: String? = null,
        val isRecording: Boolean = false,
        val lastUpdate: Long = System.currentTimeMillis()
    )

    data class StatusMessage(
        val message: String,
        val level: Level,
        val timestampMs: Long = System.currentTimeMillis()
    ) {
        enum class Level { INFO, WARNING, ERROR }
    }

    sealed class Event {
        object ShowExitDialog : Event()
        data class ShowToast(val message: String, val isLong: Boolean = false) : Event()
        // Add other events for navigation, specific dialogs etc.
    }

    data class SessionConfig(
        val sessionId: String? = null,
        val participantId: String? = null,
        val studyName: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val modalities: List<String> = listOf("thermal", "GSR"),
        val saveImages: Boolean = false
    )

    init {
        AppLogger.d(TAG, "MainActivityViewModel initialized.")
        initializeComponents()
    }

    // --- Event Handlers from UI ---
    fun onNavigationItemSelected(index: Int) {
        _currentPage.value = index
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _events.emit(Event.ShowExitDialog)
        }
    }

    fun onPermissionsGranted() {
        AppLogger.i(TAG, "All permissions granted. Ready to initialize sensors.")
        // Trigger sensor-related initializations that depend on permissions.
    }

    // --- Service Lifecycle Handlers ---
    fun onServiceConnected(binder: RecordingService.RecordingServiceBinder) {
        this.serviceBinder = binder
        this.networkClient = binder.getNetworkClient()
        // Here you would set up listeners and observers for network state
        // that were previously in MainActivity's serviceConnection.
        AppLogger.i(TAG, "Service connected and handled in ViewModel.")
    }

    fun onServiceDisconnected() {
        this.serviceBinder = null
        this.networkClient = null
        AppLogger.i(TAG, "Service disconnected and handled in ViewModel.")
    }

    // --- Business Logic ---
    fun initializeComponents() {
        viewModelScope.launch {
            try {
                initializeGSRComponents()
                initializeThermalComponents()
                initializeNetworkComponents()
                initializeSessionComponents()
                AppLogger.i(TAG, "All components initialized successfully")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize components", e)
                _events.emit(Event.ShowToast("Initialization failed: ${e.message}", true))
            }
        }
    }

    private suspend fun initializeGSRComponents() = withContext(Dispatchers.IO) {
        try {
            gsrSessionManager = GSRSessionManager.getInstance(getApplication())
            _gsrConnectionState.value = GSRConnectionState.DISCONNECTED
            Log.d(
                TAG,
                "GSR components initialized (UnifiedGSRRecorder will be initialized on connection)"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize GSR components", e)
            _gsrConnectionState.value = GSRConnectionState.ERROR
            throw e
        }
    }

    private suspend fun initializeThermalComponents() = withContext(Dispatchers.IO) {
        try {
            thermalRecorder = ThermalRecorder(getApplication())
            thermalRecorder?.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
                override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                    Log.d(
                        TAG,
                        "Thermal frame processed: ${stats.frameSequence} - T=${stats.minTemp}°C to ${stats.maxTemp}°C"
                    )
                }

                override fun onError(error: String) {
                    AppLogger.e(TAG, "Thermal recorder error: $error")
                    viewModelScope.launch {
                        _events.emit(Event.ShowToast("Thermal recording error: $error", true))
                    }
                }
            })
            AppLogger.d(TAG, "Thermal components initialized")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize thermal components", e)
            throw e
        }
    }

    private suspend fun initializeNetworkComponents() = withContext(Dispatchers.IO) {
        try {
            networkClient = NetworkClient(getApplication())
            networkController = NetworkController(getApplication())
            networkController?.setEventListener(object :
                NetworkController.NetworkControllerListener {
                override fun onStartRecordingCommand(
                    sessionId: String,
                    modalities: List<String>,
                    options: Map<String, Any>
                ) {
                    Log.i(
                        TAG,
                        "Remote start recording command: sessionId=$sessionId, modalities=$modalities"
                    )
                    val config = SessionConfig(
                        sessionId = sessionId,
                        participantId = options["participantId"] as? String,
                        studyName = options["studyName"] as? String,
                        modalities = modalities,
                        saveImages = options["saveImages"] as? Boolean ?: false
                    )
                    viewModelScope.launch {
                        startRecordingSession(config)
                    }
                }

                override fun onStopRecordingCommand() {
                    AppLogger.i(TAG, "Remote stop recording command received")
                    viewModelScope.launch {
                        stopRecordingSession()
                    }
                }

                override fun onClientConnected(clientId: String, clientInfo: String) {
                    AppLogger.i(TAG, "PC client connected: $clientId ($clientInfo)")
                    viewModelScope.launch { _events.emit(Event.ShowToast("PC client connected: $clientInfo")) }
                }

                override fun onClientDisconnected(clientId: String, reason: String) {
                    AppLogger.i(TAG, "PC client disconnected: $clientId - $reason")
                    viewModelScope.launch { _events.emit(Event.ShowToast("PC client disconnected: $reason")) }
                }

                override fun onError(operation: String, error: String) {
                    AppLogger.e(TAG, "NetworkController error in $operation: $error")
                    viewModelScope.launch {
                        _events.emit(
                            Event.ShowToast(
                                "PC control error: $error",
                                true
                            )
                        )
                    }
                }
            })
            viewModelScope.launch {
                val serverStarted = networkController?.start()
                if (serverStarted == true) {
                    val actualPort =
                        networkController?.getServerPort() ?: NetworkController.DEFAULT_PORT
                    AppLogger.i(TAG, "NetworkController server started on port $actualPort")
                    _events.emit(Event.ShowToast("PC remote control ready on port $actualPort"))
                } else {
                    AppLogger.w(TAG, "Failed to start NetworkController server")
                    _events.emit(Event.ShowToast("Failed to start PC remote control server", true))
                }
            }
            networkClient?.setEventListener(object : NetworkClient.NetworkEventListener {
                override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.value = NetworkConnectionState.DISCOVERING
                    AppLogger.d(TAG, "PC Controller discovered: ${controller.deviceName}")
                }

                override fun onConnected(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.value = NetworkConnectionState.CONNECTED
                    _connectedControllerInfo.value = controller
                    viewModelScope.launch { _events.emit(Event.ShowToast("Connected to PC: ${controller.deviceName}")) }
                    AppLogger.i(TAG, "Connected to PC controller: ${controller.deviceName}")
                }

                override fun onDisconnected(reason: String) {
                    _networkConnectionState.value = NetworkConnectionState.DISCONNECTED
                    _connectedControllerInfo.value = null
                    viewModelScope.launch { _events.emit(Event.ShowToast("Disconnected from PC: $reason")) }
                    AppLogger.w(TAG, "Disconnected from PC controller: $reason")
                }

                override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                    handleRemoteRecordingRequest(sessionInfo)
                }

                override fun onSyncFlash(durationMs: Int) {
                    AppLogger.d(TAG, "Sync flash requested: ${durationMs}ms")
                }

                override fun onTimeSynchronized(offsetNanoseconds: Long) {
                    AppLogger.d(TAG, "Time synchronized with offset: ${offsetNanoseconds}ns")
                }

                override fun onDataStreamingStarted() {
                    AppLogger.d(TAG, "Data streaming to PC started")
                }

                override fun onDataStreamingStopped() {
                    AppLogger.d(TAG, "Data streaming to PC stopped")
                }

                override fun onError(operation: String, error: String) {
                    viewModelScope.launch {
                        _events.emit(
                            Event.ShowToast(
                                "Network error in $operation: $error",
                                true
                            )
                        )
                    }
                    AppLogger.e(TAG, "Network error in $operation: $error")
                }
            })
            _networkConnectionState.value = NetworkConnectionState.DISCONNECTED
            AppLogger.d(TAG, "Network components initialized")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize network components", e)
            _networkConnectionState.value = NetworkConnectionState.ERROR
            throw e
        }
    }

    private suspend fun initializeSessionComponents() = withContext(Dispatchers.IO) {
        try {
            sessionManager = SessionManager(
                getApplication(),
                mpdc4gsr.core.StructuredLogger.getInstance(getApplication())
            )
            _sessionState.value = SessionState.IDLE
            AppLogger.d(TAG, "Session components initialized")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize session components", e)
            _sessionState.value = SessionState.ERROR
            throw e
        }
    }

    fun startGSRConnection() {
        viewModelScope.launch {
            try {
                _gsrConnectionState.value = GSRConnectionState.DISCOVERING
                _events.emit(Event.ShowToast("Searching for GSR sensor..."))
                withContext(Dispatchers.IO) {
                    try {
                        if (unifiedGSRRecorder == null) {
                            Log.w(
                                TAG,
                                "UnifiedGSRRecorder requires LifecycleOwner - should be initialized in Activity context"
                            )
                            _gsrConnectionState.value = GSRConnectionState.CONNECTING
                            viewModelScope.launch { _events.emit(Event.ShowToast("Connecting to GSR sensor...")) }
                            kotlinx.coroutines.delay(2000)
                            _gsrConnectionState.value = GSRConnectionState.CONNECTED
                            viewModelScope.launch { _events.emit(Event.ShowToast("GSR sensor connected (simulated)")) }
                            return@withContext
                        }
                        val recorder = unifiedGSRRecorder!!
                        val initSuccess = recorder.initialize()
                        if (!initSuccess) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR
                            viewModelScope.launch {
                                _events.emit(
                                    Event.ShowToast(
                                        "Failed to initialize GSR recorder",
                                        true
                                    )
                                )
                            }
                            return@withContext
                        }
                        _gsrConnectionState.value = GSRConnectionState.CONNECTING
                        viewModelScope.launch { _events.emit(Event.ShowToast("Starting device discovery...")) }
                        val discoverySuccess = recorder.startDeviceDiscovery()
                        if (!discoverySuccess) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR
                            viewModelScope.launch {
                                _events.emit(
                                    Event.ShowToast(
                                        "No GSR devices found",
                                        true
                                    )
                                )
                            }
                            return@withContext
                        }
                        val devices = recorder.getDiscoveredDevices()
                        if (devices.isEmpty()) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR
                            viewModelScope.launch {
                                _events.emit(
                                    Event.ShowToast(
                                        "No compatible GSR devices detected",
                                        true
                                    )
                                )
                            }
                            return@withContext
                        }
                        val targetDevice = devices.first()
                        viewModelScope.launch { _events.emit(Event.ShowToast("Connecting to ${targetDevice.name}...")) }
                        val connectionSuccess = recorder.connectToDevice(targetDevice)
                        if (connectionSuccess) {
                            _gsrConnectionState.value = GSRConnectionState.CONNECTED
                            viewModelScope.launch { _events.emit(Event.ShowToast("Connected to ${targetDevice.name}")) }
                            monitorGSRStatus(recorder)
                        } else {
                            _gsrConnectionState.value = GSRConnectionState.ERROR
                            viewModelScope.launch {
                                _events.emit(
                                    Event.ShowToast(
                                        "Failed to connect to ${targetDevice.name}",
                                        true
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error during GSR connection", e)
                        _gsrConnectionState.value = GSRConnectionState.ERROR
                        viewModelScope.launch {
                            _events.emit(
                                Event.ShowToast(
                                    "GSR connection error: ${e.message}",
                                    true
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start GSR connection", e)
                _gsrConnectionState.value = GSRConnectionState.ERROR
                _events.emit(Event.ShowToast("GSR connection failed: ${e.message}", true))
            }
        }
    }

    private fun monitorGSRStatus(recorder: UnifiedGSRRecorder) {
        viewModelScope.launch {
            try {
                recorder.deviceStatus.collect { status ->
                    AppLogger.d(TAG, "GSR device status: $status")
                    if (status.contains("Connected")) {
                        _gsrBatteryLevel.value = 85
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error monitoring GSR status", e)
            }
        }
        viewModelScope.launch {
            try {
                recorder.connectionQuality.collect { quality ->
                    AppLogger.d(TAG, "GSR connection quality: $quality")
                    if (quality < 0.3) {
                        _events.emit(Event.ShowToast("GSR connection quality low"))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error monitoring GSR connection quality", e)
            }
        }
    }

    fun startNetworkDiscovery() {
        viewModelScope.launch {
            try {
                _networkConnectionState.value = NetworkConnectionState.DISCOVERING
                _events.emit(Event.ShowToast("Searching for PC controllers..."))
                withContext(Dispatchers.IO) {
                    networkClient?.let { client ->
                        val controllers = client.discoverControllers()
                        if (controllers.isNotEmpty()) {
                            val controller = controllers.first()
                            val connected =
                                client.connectToController(controller.ipAddress, controller.port)
                            if (connected) {
                                _networkConnectionState.value = NetworkConnectionState.CONNECTED
                                _connectedControllerInfo.value = controller
                            }
                        } else {
                            _networkConnectionState.value = NetworkConnectionState.DISCONNECTED
                            viewModelScope.launch { _events.emit(Event.ShowToast("No PC controllers found")) }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start network discovery", e)
                _networkConnectionState.value = NetworkConnectionState.ERROR
                _events.emit(Event.ShowToast("Network discovery failed: ${e.message}", true))
            }
        }
    }

    fun startRecordingSession(sessionConfig: SessionConfig = SessionConfig()) {
        viewModelScope.launch {
            try {
                if (_sessionState.value == SessionState.RECORDING) {
                    AppLogger.w(TAG, "Recording already in progress")
                    return@launch
                }
                _sessionState.value = SessionState.STARTING
                _events.emit(Event.ShowToast("Starting recording session..."))
                withContext(Dispatchers.IO) {
                    gsrSessionManager?.let { manager ->
                        val session = manager.createSession(
                            sessionId = sessionConfig.sessionId,
                            participantId = sessionConfig.participantId,
                            studyName = sessionConfig.studyName,
                            metadata = sessionConfig.metadata
                        )
                        if (sessionConfig.modalities.contains("thermal")) {
                            val sessionDir =
                                "/storage/emulated/0/IRCamera/Sessions/${session.sessionId}"
                            val thermalStarted = thermalRecorder?.startRecording(
                                sessionDir,
                                sessionConfig.saveImages
                            )
                            if (thermalStarted == true) {
                                Log.i(
                                    TAG,
                                    "Thermal recording started for session: ${session.sessionId}"
                                )
                            } else {
                                AppLogger.w(TAG, "Failed to start thermal recording")
                            }
                        }
                        if (sessionConfig.modalities.contains("GSR")) {
                            val gsrStarted = gsrSensorRecorder?.initialize()
                            if (gsrStarted == true) {
                                Log.i(
                                    TAG,
                                    "GSR recording started for session: ${session.sessionId}"
                                )
                            } else {
                                AppLogger.w(TAG, "Failed to start GSR recording")
                            }
                        }
                        _currentSession.value = session
                        _sessionState.value = SessionState.RECORDING
                        viewModelScope.launch { _events.emit(Event.ShowToast("Recording session started: ${session.sessionId}")) }
                        AppLogger.i(TAG, "Recording session started: ${session.sessionId}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start recording session", e)
                _sessionState.value = SessionState.ERROR
                _events.emit(Event.ShowToast("Failed to start recording: ${e.message}", true))
            }
        }
    }

    fun stopRecordingSession() {
        viewModelScope.launch {
            try {
                if (_sessionState.value != SessionState.RECORDING) {
                    AppLogger.w(TAG, "No recording session in progress")
                    return@launch
                }
                _sessionState.value = SessionState.STOPPING
                _events.emit(Event.ShowToast("Stopping recording session..."))
                withContext(Dispatchers.IO) {
                    _currentSession.value?.let { session ->
                        val thermalStopped = thermalRecorder?.stopRecording()
                        if (thermalStopped == true) {
                            Log.i(
                                TAG,
                                "Thermal recording stopped for session: ${session.sessionId}"
                            )
                        }
                        gsrSessionManager?.completeSession(session.sessionId)
                        _currentSession.value = null
                        _sessionState.value = SessionState.IDLE
                        viewModelScope.launch { _events.emit(Event.ShowToast("Recording session stopped")) }
                        AppLogger.i(TAG, "Recording session stopped: ${session.sessionId}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to stop recording session", e)
                _sessionState.value = SessionState.ERROR
                _events.emit(Event.ShowToast("Failed to stop recording: ${e.message}", true))
            }
        }
    }

    private fun handleRemoteRecordingRequest(sessionInfo: SessionInfo) {
        viewModelScope.launch {
            AppLogger.i(TAG, "Remote recording request received: ${sessionInfo.sessionId}")
            val config = SessionConfig(
                sessionId = sessionInfo.sessionId,
                participantId = sessionInfo.participantId,
                studyName = sessionInfo.studyName,
                metadata = sessionInfo.metadata
            )
            startRecordingSession(config)
        }
    }

    fun processThermalFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        minTempRange: Float,
        maxTempRange: Float,
        timestampNs: Long = System.nanoTime()
    ) {
        thermalRecorder?.processFrameFromIntensity(
            frameData,
            width,
            height,
            minTempRange,
            maxTempRange,
            timestampNs
        )
    }

    // Enhanced sensor state management methods
    fun updateRGBCameraState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _rgbCameraState.value = SensorState(status, message, isRecording)
    }

    fun updateThermalCameraState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _thermalCameraState.value = SensorState(status, message, isRecording)
    }

    fun updateGSRSensorState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _gsrSensorState.value = SensorState(status, message, isRecording)
    }

    // Manual camera controls
    fun lockExposure(locked: Boolean) {
        _exposureLocked.value = locked
        viewModelScope.launch { _events.emit(Event.ShowToast(if (locked) "Exposure locked" else "Exposure auto mode enabled")) }
    }

    fun lockFocus(locked: Boolean) {
        _focusLocked.value = locked
        viewModelScope.launch { _events.emit(Event.ShowToast(if (locked) "Focus locked" else "Focus auto mode enabled")) }
    }

    fun setExposureCompensation(compensation: Float) {
        _exposureCompensation.value = compensation
        viewModelScope.launch { _events.emit(Event.ShowToast("Exposure compensation: ${if (compensation > 0) "+" else ""}$compensation EV")) }
    }

    fun setRemoteTriggered(isRemote: Boolean) {
        _isRemoteTriggered.value = isRemote
    }

    fun resetCameraControlsToAuto() {
        _exposureLocked.value = false
        _focusLocked.value = false
        _exposureCompensation.value = 0.0f
        viewModelScope.launch { _events.emit(Event.ShowToast("Camera controls reset to auto")) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            networkController?.stop()
        }
        AppLogger.d(TAG, "MainActivityViewModel cleared")
    }
}
