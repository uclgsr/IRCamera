package mpdc4gsr.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.SessionManager
import mpdc4gsr.network.NetworkClient
import mpdc4gsr.network.NetworkController
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import mpdc4gsr.sensors.thermal.ThermalRecorder
import mpdc4gsr.sensors.unified.UnifiedGSRRecorder
import com.mpdc4gsr.gsr.service.SessionManager as GSRSessionManager


class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainActivityViewModel"
    }


    // State properties using StateFlow for better coroutine support and lifecycle awareness
    private val _gsrConnectionState = MutableStateFlow(GSRConnectionState.DISCONNECTED)
    val gsrConnectionState: StateFlow<GSRConnectionState> = _gsrConnectionState.asStateFlow()

    private val _gsrBatteryLevel = MutableStateFlow<Int?>(null)
    val gsrBatteryLevel: StateFlow<Int?> = _gsrBatteryLevel.asStateFlow()

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

    // Use SharedFlow for one-time events like status messages
    private val _statusMessage = MutableSharedFlow<StatusMessage>(replay = 0)
    val statusMessage: SharedFlow<StatusMessage> = _statusMessage.asSharedFlow()

    // Enhanced sensor state tracking with StateFlow
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


    private var gsrSensorRecorder: GSRSensorRecorder? = null
    private var unifiedGSRRecorder: UnifiedGSRRecorder? = null
    private var thermalRecorder: ThermalRecorder? = null
    private var networkClient: NetworkClient? = null
    private var networkController: NetworkController? = null
    private var sessionManager: SessionManager? = null
    private var gsrSessionManager: GSRSessionManager? = null

    enum class GSRConnectionState {
        DISCONNECTED,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    enum class NetworkConnectionState {
        DISCONNECTED,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    enum class SessionState {
        IDLE,
        STARTING,
        RECORDING,
        PAUSED,
        STOPPING,
        ERROR
    }

    // Enhanced sensor status tracking
    enum class SensorStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        ERROR,
        SIMULATION
    }

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
        enum class Level {
            INFO, WARNING, ERROR
        }
    }

    init {
        Log.d(TAG, "MainActivityViewModel initialized with StateFlow for reactive state management")
    }

    fun initializeComponents() {
        viewModelScope.launch {
            try {

                initializeGSRComponents()


                initializeThermalComponents()


                initializeNetworkComponents()


                initializeSessionComponents()

                Log.i(TAG, "All components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize components", e)
                _statusMessage.tryEmit(
                    StatusMessage(
                        "Failed to initialize components: ${e.message}",
                        StatusMessage.Level.ERROR
                    )
                )
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
            Log.e(TAG, "Failed to initialize GSR components", e)
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
                    Log.e(TAG, "Thermal recorder error: $error")
                    _statusMessage.value =
                        StatusMessage("Thermal recording error: $error", StatusMessage.Level.ERROR)
                    )
                }
            })

            Log.d(TAG, "Thermal components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thermal components", e)
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
                    Log.i(TAG, "Remote stop recording command received")


                    viewModelScope.launch {
                        stopRecordingSession()
                    }
                }

                override fun onClientConnected(clientId: String, clientInfo: String) {
                    Log.i(TAG, "PC client connected: $clientId ($clientInfo)")
                    _statusMessage.value =
                        StatusMessage("PC client connected: $clientInfo", StatusMessage.Level.INFO)
                    )
                }

                override fun onClientDisconnected(clientId: String, reason: String) {
                    Log.i(TAG, "PC client disconnected: $clientId - $reason")
                    _statusMessage.value =
                        StatusMessage(
                            "PC client disconnected: $reason",
                            StatusMessage.Level.WARNING
                        )
                    )
                }

                override fun onError(operation: String, error: String) {
                    Log.e(TAG, "NetworkController error in $operation: $error")
                    _statusMessage.value =
                        StatusMessage("PC control error: $error", StatusMessage.Level.ERROR)
                    )
                }
            })


            viewModelScope.launch {
                val serverStarted = networkController?.start(NetworkController.DEFAULT_PORT)
                if (serverStarted == true) {
                    Log.i(
                        TAG,
                        "NetworkController server started on port ${NetworkController.DEFAULT_PORT}"
                    )
                    _statusMessage.value =
                        StatusMessage(
                            "PC remote control ready on port ${NetworkController.DEFAULT_PORT}",
                            StatusMessage.Level.INFO
                        )
                    )
                } else {
                    Log.w(TAG, "Failed to start NetworkController server")
                }
            }


            networkClient?.setEventListener(object : NetworkClient.NetworkEventListener {
                override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.value = NetworkConnectionState.DISCOVERING)
                    Log.d(TAG, "PC Controller discovered: ${controller.deviceName}")
                }

                override fun onConnected(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.value = NetworkConnectionState.CONNECTED)
                    _connectedControllerInfo.value = controller)
                    _statusMessage.value =
                        StatusMessage(
                            "Connected to PC: ${controller.deviceName}",
                            StatusMessage.Level.INFO
                        )
                    )
                    Log.i(TAG, "Connected to PC controller: ${controller.deviceName}")
                }

                override fun onDisconnected(reason: String) {
                    _networkConnectionState.value = NetworkConnectionState.DISCONNECTED)
                    _connectedControllerInfo.value = null)
                    _statusMessage.value =
                        StatusMessage("Disconnected from PC: $reason", StatusMessage.Level.WARNING)
                    )
                    Log.w(TAG, "Disconnected from PC controller: $reason")
                }

                override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                    handleRemoteRecordingRequest(sessionInfo)
                }

                override fun onSyncFlash(durationMs: Int) {

                    Log.d(TAG, "Sync flash requested: ${durationMs}ms")
                }

                override fun onTimeSynchronized(offsetNanoseconds: Long) {
                    Log.d(TAG, "Time synchronized with offset: ${offsetNanoseconds}ns")
                }

                override fun onDataStreamingStarted() {
                    Log.d(TAG, "Data streaming to PC started")
                }

                override fun onDataStreamingStopped() {
                    Log.d(TAG, "Data streaming to PC stopped")
                }

                override fun onError(operation: String, error: String) {
                    _statusMessage.value =
                        StatusMessage(
                            "Network error in $operation: $error",
                            StatusMessage.Level.ERROR
                        )
                    )
                    Log.e(TAG, "Network error in $operation: $error")
                }
            })

            _networkConnectionState.value = NetworkConnectionState.DISCONNECTED)
            Log.d(TAG, "Network components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize network components", e)
            _networkConnectionState.value = NetworkConnectionState.ERROR)
            throw e
        }
    }

    private suspend fun initializeSessionComponents() = withContext(Dispatchers.IO) {
        try {
            sessionManager = SessionManager(
                getApplication(),
                mpdc4gsr.core.StructuredLogger.getInstance(getApplication())
            )

            _sessionState.value = SessionState.IDLE)
            Log.d(TAG, "Session components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize session components", e)
            _sessionState.value = SessionState.ERROR)
            throw e
        }
    }


    fun startGSRConnection() {
        viewModelScope.launch {
            try {
                _gsrConnectionState.value = GSRConnectionState.DISCOVERING
                _statusMessage.value =
                    StatusMessage("Searching for GSR sensor...", StatusMessage.Level.INFO)


                withContext(Dispatchers.IO) {


                    try {

                        if (unifiedGSRRecorder == null) {


                            Log.w(
                                TAG,
                                "UnifiedGSRRecorder requires LifecycleOwner - should be initialized in Activity context"
                            )


                            _gsrConnectionState.value = GSRConnectionState.CONNECTING)
                            _statusMessage.value =
                                StatusMessage(
                                    "Connecting to GSR sensor...",
                                    StatusMessage.Level.INFO
                                )
                            )


                            kotlinx.coroutines.delay(2000)

                            _gsrConnectionState.value = GSRConnectionState.CONNECTED)
                            _statusMessage.value =
                                StatusMessage(
                                    "GSR sensor connected (simulated)",
                                    StatusMessage.Level.INFO
                                )
                            )

                            return@withContext
                        }


                        val recorder = unifiedGSRRecorder!!


                        val initSuccess = recorder.initialize()
                        if (!initSuccess) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR)
                            _statusMessage.value =
                                StatusMessage(
                                    "Failed to initialize GSR recorder",
                                    StatusMessage.Level.ERROR
                                )
                            )
                            return@withContext
                        }

                        _gsrConnectionState.value = GSRConnectionState.CONNECTING)
                        _statusMessage.value =
                            StatusMessage("Starting device discovery...", StatusMessage.Level.INFO)
                        )


                        val discoverySuccess = recorder.startDeviceDiscovery()
                        if (!discoverySuccess) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR)
                            _statusMessage.value =
                                StatusMessage("No GSR devices found", StatusMessage.Level.ERROR)
                            )
                            return@withContext
                        }


                        val devices = recorder.getDiscoveredDevices()
                        if (devices.isEmpty()) {
                            _gsrConnectionState.value = GSRConnectionState.ERROR)
                            _statusMessage.value =
                                StatusMessage(
                                    "No compatible GSR devices detected",
                                    StatusMessage.Level.ERROR
                                )
                            )
                            return@withContext
                        }


                        val targetDevice = devices.first()
                        _statusMessage.value =
                            StatusMessage(
                                "Connecting to ${targetDevice.name}...",
                                StatusMessage.Level.INFO
                            )
                        )

                        val connectionSuccess = recorder.connectToDevice(targetDevice)
                        if (connectionSuccess) {
                            _gsrConnectionState.value = GSRConnectionState.CONNECTED)
                            _statusMessage.value =
                                StatusMessage(
                                    "Connected to ${targetDevice.name}",
                                    StatusMessage.Level.INFO
                                )
                            )


                            monitorGSRStatus(recorder)
                        } else {
                            _gsrConnectionState.value = GSRConnectionState.ERROR)
                            _statusMessage.value =
                                StatusMessage(
                                    "Failed to connect to ${targetDevice.name}",
                                    StatusMessage.Level.ERROR
                                )
                            )
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error during GSR connection", e)
                        _gsrConnectionState.value = GSRConnectionState.ERROR)
                        _statusMessage.value =
                            StatusMessage(
                                "GSR connection error: ${e.message}",
                                StatusMessage.Level.ERROR
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR connection", e)
                _gsrConnectionState.value = GSRConnectionState.ERROR
                _statusMessage.value = StatusMessage(
                    "GSR connection failed: ${e.message}",
                    StatusMessage.Level.ERROR
                )
            }
        }
    }


    private fun monitorGSRStatus(recorder: UnifiedGSRRecorder) {
        viewModelScope.launch {
            try {

                recorder.deviceStatus.collect { status ->
                    Log.d(TAG, "GSR device status: $status")


                    if (status.contains("Connected")) {
                        _gsrBatteryLevel.value = 85)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring GSR status", e)
            }
        }

        viewModelScope.launch {
            try {

                recorder.connectionQuality.collect { quality ->
                    Log.d(TAG, "GSR connection quality: $quality")


                    if (quality < 0.3) {
                        _statusMessage.value =
                            StatusMessage("GSR connection quality low", StatusMessage.Level.WARNING)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring GSR connection quality", e)
            }
        }
    }


    fun startNetworkDiscovery() {
        viewModelScope.launch {
            try {
                _networkConnectionState.value = NetworkConnectionState.DISCOVERING
                _statusMessage.value =
                    StatusMessage("Searching for PC controllers...", StatusMessage.Level.INFO)

                withContext(Dispatchers.IO) {
                    networkClient?.let { client ->
                        val controllers = client.discoverControllers()
                        if (controllers.isNotEmpty()) {

                            val controller = controllers.first()
                            val connected =
                                client.connectToController(controller.ipAddress, controller.port)
                            if (connected) {
                                _networkConnectionState.value = NetworkConnectionState.CONNECTED)
                                _connectedControllerInfo.value = controller)
                            }
                        } else {
                            _networkConnectionState.value = NetworkConnectionState.DISCONNECTED)
                            _statusMessage.value =
                                StatusMessage(
                                    "No PC controllers found",
                                    StatusMessage.Level.WARNING
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start network discovery", e)
                _networkConnectionState.value = NetworkConnectionState.ERROR
                _statusMessage.value = StatusMessage(
                    "Network discovery failed: ${e.message}",
                    StatusMessage.Level.ERROR
                )
            }
        }
    }


    fun startRecordingSession(sessionConfig: SessionConfig = SessionConfig()) {
        viewModelScope.launch {
            try {
                if (_sessionState.value == SessionState.RECORDING) {
                    Log.w(TAG, "Recording already in progress")
                    return@launch
                }

                _sessionState.value = SessionState.STARTING
                _statusMessage.value =
                    StatusMessage("Starting recording session...", StatusMessage.Level.INFO)

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
                                Log.w(TAG, "Failed to start thermal recording")
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
                                Log.w(TAG, "Failed to start GSR recording")
                            }
                        }

                        _currentSession.value = session)
                        _sessionState.value = SessionState.RECORDING)
                        _statusMessage.value =
                            StatusMessage(
                                "Recording session started: ${session.sessionId}",
                                StatusMessage.Level.INFO
                            )
                        )

                        Log.i(TAG, "Recording session started: ${session.sessionId}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording session", e)
                _sessionState.value = SessionState.ERROR
                _statusMessage.value = StatusMessage(
                    "Failed to start recording: ${e.message}",
                    StatusMessage.Level.ERROR
                )
            }
        }
    }


    fun stopRecordingSession() {
        viewModelScope.launch {
            try {
                if (_sessionState.value != SessionState.RECORDING) {
                    Log.w(TAG, "No recording session in progress")
                    return@launch
                }

                _sessionState.value = SessionState.STOPPING
                _statusMessage.value =
                    StatusMessage("Stopping recording session...", StatusMessage.Level.INFO)

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

                        _currentSession.value = null)
                        _sessionState.value = SessionState.IDLE)
                        _statusMessage.value =
                            StatusMessage("Recording session stopped", StatusMessage.Level.INFO)
                        )

                        Log.i(TAG, "Recording session stopped: ${session.sessionId}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording session", e)
                _sessionState.value = SessionState.ERROR
                _statusMessage.value = StatusMessage(
                    "Failed to stop recording: ${e.message}",
                    StatusMessage.Level.ERROR
                )
            }
        }
    }


    private fun handleRemoteRecordingRequest(sessionInfo: SessionInfo) {
        viewModelScope.launch {
            Log.i(TAG, "Remote recording request received: ${sessionInfo.sessionId}")

            val config = SessionConfig(
                sessionId = sessionInfo.sessionId,
                participantId = sessionInfo.participantId,
                studyName = sessionInfo.studyName,
                metadata = sessionInfo.metadata
            )

            startRecordingSession(config)
        }
    }


    fun clearStatusMessage() {
        _statusMessage.value = null
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
            frameData, width, height, minTempRange, maxTempRange, timestampNs
        )
    }

    override fun onCleared() {
        super.onCleared()


        viewModelScope.launch {
            networkController?.stop()
        }

        Log.d(TAG, "MainActivityViewModel cleared")
    }


    data class SessionConfig(
        val sessionId: String? = null,
        val participantId: String? = null,
        val studyName: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val modalities: List<String> = listOf("thermal", "GSR"),
        val saveImages: Boolean = false
    )

    // Enhanced sensor state management methods
    fun updateRGBCameraState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _rgbCameraState.value = SensorState(status, message, isRecording))
    }

    fun updateThermalCameraState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _thermalCameraState.value = SensorState(status, message, isRecording))
    }

    fun updateGSRSensorState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean = false
    ) {
        _gsrSensorState.value = SensorState(status, message, isRecording))
    }

    // Manual camera controls
    fun lockExposure(locked: Boolean) {
        _exposureLocked.value = locked)
        _statusMessage.value =
            StatusMessage(
                if (locked) "Exposure locked" else "Exposure auto mode enabled",
                StatusMessage.Level.INFO
            )
        )
    }

    fun lockFocus(locked: Boolean) {
        _focusLocked.value = locked)
        _statusMessage.value =
            StatusMessage(
                if (locked) "Focus locked" else "Focus auto mode enabled",
                StatusMessage.Level.INFO
            )
        )
    }

    fun setExposureCompensation(compensation: Float) {
        _exposureCompensation.value = compensation)
        _statusMessage.value =
            StatusMessage(
                "Exposure compensation: ${if (compensation > 0) "+" else ""}$compensation EV",
                StatusMessage.Level.INFO
            )
        )
    }

    // Recording trigger indication
    fun setRemoteTriggered(isRemote: Boolean) {
        _isRemoteTriggered.value = isRemote)
    }

    // Reset manual controls to auto
    fun resetCameraControlsToAuto() {
        _exposureLocked.value = false)
        _focusLocked.value = false)
        _exposureCompensation.value = 0.0f)
        _statusMessage.value =
            StatusMessage("Camera controls reset to auto", StatusMessage.Level.INFO)
        )
    }
}
