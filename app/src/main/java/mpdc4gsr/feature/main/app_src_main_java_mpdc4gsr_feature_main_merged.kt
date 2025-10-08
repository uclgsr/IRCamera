// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\main' directory and its subdirectories.
// Total files: 10 | Generated on: 2025-10-08 01:42:32


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\presentation\MainActivityViewModel.kt =====

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
                        "Thermal frame processed: ${stats.frameSequence} - T=${stats.minTemp}Â°C to ${stats.maxTemp}Â°C"
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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\ComponentShowcaseScreen.kt =====

package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.compose.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcaseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Component Showcase",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            ComponentShowcaseContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ComponentShowcaseContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.normal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        // Typography showcase
        TypographyShowcase()
        // Color palette showcase
        ColorPaletteShowcase()
        // Interactive components
        InteractiveComponentsShowcase()
        // Status indicators
        StatusIndicatorsShowcase()
        // Card layouts
        CardLayoutsShowcase()
        // Navigation components
        NavigationComponentsShowcase()
    }
}

@Composable
private fun TypographyShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Typography Styles",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "Headline Large",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "Headline Medium",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Headline Small",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Title Large",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Title Medium",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Body Large - This is the standard body text for longer content and descriptions.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Body Medium - This is commonly used for secondary information.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Body Small - Used for captions and supplementary text.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ColorPaletteShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Thermal Imaging Color Palette",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                ColorSwatch("Primary", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                ColorSwatch("Secondary", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                ColorSwatch("Tertiary", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                ColorSwatch("Error", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                ColorSwatch("Background", MaterialTheme.colorScheme.background, Modifier.weight(1f))
                ColorSwatch("Surface", MaterialTheme.colorScheme.surface, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            name,
            modifier = Modifier
                .padding(Spacing.small)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
private fun InteractiveComponentsShowcase() {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    var switchState by remember { mutableStateOf(true) }
    var selectedChip by remember { mutableStateOf("Option 1") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Interactive Components",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Button")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Outlined")
                }
                TextButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Text")
                }
            }
            // Slider
            Column {
                Text("Slider: ${(sliderValue * 100).toInt()}%")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Switch Control")
                Switch(
                    checked = switchState,
                    onCheckedChange = { switchState = it }
                )
            }
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                listOf("Option 1", "Option 2", "Option 3").forEach { option ->
                    FilterChip(
                        onClick = { selectedChip = option },
                        label = { Text(option) },
                        selected = selectedChip == option
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Status Indicators",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Progress indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text("Linear Progress")
                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Circular Progress")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.normal)
                ) {
                    CircularProgressIndicator(
                        progress = { 0.75f }
                    )
                    CircularProgressIndicator()
                }
            }
            // Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Badges:")
                Badge { Text("New") }
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) { Text("Error") }
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) { Text("Warning") }
            }
            // Status icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusIcon(Icons.Default.CheckCircle, "Success", MaterialTheme.colorScheme.primary)
                StatusIcon(Icons.Default.Warning, "Warning", MaterialTheme.colorScheme.tertiary)
                StatusIcon(Icons.Default.Error, "Error", MaterialTheme.colorScheme.error)
                StatusIcon(Icons.Default.Info, "Info", MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(Spacing.large)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CardLayoutsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Card Layouts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Information card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Component Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Information Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "This is an example of an information card layout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Action card
            Card(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Touch Interaction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Clickable Action Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "This card responds to tap interactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Component",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationComponentsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Navigation Components",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "The unified navigation system provides:",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                NavigationFeature("Type-safe navigation routes")
                NavigationFeature("Smooth page transitions")
                NavigationFeature("Deep linking support")
                NavigationFeature("State preservation")
                NavigationFeature("Back stack management")
            }
            Text(
                "All navigation is handled through the UnifiedNavigation system, " +
                        "providing consistent behavior across the entire application.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationFeature(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Feature Available",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.normal)
        )
        Text(
            feature,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\ComposeScreens.kt =====

package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mpdc4gsr.libunified.app.compose.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        item {
            Text(
                text = "System Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ConnectionStatusCard()
        }
        item {
            QuickActionsCard(navController = navController)
        }
        item {
            RecentSessionsCard()
        }
        item {
            SystemHealthCard()
        }
    }
}

@Composable
fun ThermalCameraScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        Text(
            text = "Thermal Camera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Thermal Camera View")
            }
        }
        ThermalControlsPanel()
    }
}

@Composable
fun GSRSensorScreen(navController: NavController) {
    mpdc4gsr.feature.gsr.ui.GSRSensorScreen(
        onBackClick = { navController.popBackStack() },
        onSettingsClick = { navController.navigate("gsr_settings") },
        onSaveData = {
            // Navigate to data export screen if available
            // For now, the GSRSensorScreen has a default empty implementation
        }
    )
}

@Composable
private fun GSRSensorScreenDeprecated(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        item {
            Text(
                text = "GSR Sensor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            GSRConnectionCard()
        }
        item {
            GSRDataVisualizationCard()
        }
        item {
            GSRRecordingControlsCard()
        }
        item {
            GSRCalibrationCard()
        }
    }
}

@Composable
fun SensorDashboardScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        item {
            Text(
                text = "Sensor Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            AllSensorsStatusCard()
        }
        item {
            SensorMetricsCard()
        }
        item {
            DataSynchronizationCard()
        }
        item {
            AdvancedAnalyticsCard()
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            GeneralSettingsCard()
        }
        item {
            ThermalCameraSettingsCard()
        }
        item {
            GSRSensorSettingsCard()
        }
        item {
            NetworkSettingsCard()
        }
        item {
            AboutCard()
        }
    }
}

// Reusable component cards
@Composable
fun ConnectionStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConnectionStatusItem("Thermal Camera", true)
                ConnectionStatusItem("GSR Sensor", true)
                ConnectionStatusItem("Network", false)
            }
        }
    }
}

@Composable
fun ConnectionStatusItem(name: String, connected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (connected) "$name Connected" else "$name Disconnected",
            tint = if (connected) Color.Green else Color.Red,
            modifier = Modifier.size(Spacing.large)
        )
        Spacer(modifier = Modifier.height(Spacing.extraSmall))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = if (connected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.bodySmall,
            color = if (connected) Color.Green else Color.Red
        )
    }
}

@Composable
fun QuickActionsCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = { navController.navigate("thermal") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Thermal Camera")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Thermal")
                }
                Button(
                    onClick = { navController.navigate("gsr") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = "GSR Sensor")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("GSR")
                }
                OutlinedButton(
                    onClick = {
                        android.widget.Toast.makeText(
                            navController.context,
                            "Start recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Record")
                }
            }
        }
    }
}

@Composable
fun RecentSessionsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            val sessions = listOf(
                "Session 2024-01-15 14:30",
                "Session 2024-01-15 10:15",
                "Session 2024-01-14 16:45"
            )
            sessions.forEach { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.extraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = {
                        // TODO: Open session details
                        android.widget.Toast.makeText(
                            context,
                            "Open session: $session",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Open session"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemHealthCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "System Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthMetric("CPU", "45%", Color.Green)
                HealthMetric("Memory", "62%", Color.Yellow)
                HealthMetric("Battery", "89%", Color.Green)
                HealthMetric("Storage", "71%", Color.Yellow)
            }
        }
    }
}

@Composable
fun HealthMetric(name: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThermalControlsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Thermal Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Capture thermal image",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Capture Image")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Capture")
                }
                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Start thermal recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Record Video")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Record")
                }
                OutlinedButton(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Open thermal settings",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
fun GSRConnectionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GSR Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = Color.Green,
                    modifier = Modifier.size(Spacing.large)
                )
            }
            Text(
                text = "Shimmer3 GSR - Device ID: SH001",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Battery: 89% | Signal: Strong",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GSRDataVisualizationCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-time GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Placeholder for GSR data visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GSR Waveform Visualization")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: 2.45 Î¼S",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sampling: 51.2 Hz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GSRRecordingControlsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Recording Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Start GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start GSR Recording")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Start")
                }
                OutlinedButton(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Stop GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop GSR Recording")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Stop")
                }
                OutlinedButton(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Pause GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause GSR Recording")
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Pause")
                }
            }
        }
    }
}

@Composable
fun GSRCalibrationCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Sensor Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Calibration: 2024-01-15",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Start GSR calibration",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.heightIn(min = Spacing.touchTarget)
                ) {
                    Text("Calibrate")
                }
            }
            LinearProgressIndicator(
                progress = { 0.85f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Calibration Quality: 85%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Additional placeholder composables for other screens
@Composable
fun AllSensorsStatusCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("All Sensors Status")
        }
    }
}

@Composable
fun SensorMetricsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sensor Metrics")
        }
    }
}

@Composable
fun DataSynchronizationCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data Synchronization")
        }
    }
}

@Composable
fun AdvancedAnalyticsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Advanced Analytics")
        }
    }
}

@Composable
fun GeneralSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("General Settings")
        }
    }
}

@Composable
fun ThermalCameraSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Thermal Camera Settings")
        }
    }
}

@Composable
fun GSRSensorSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("GSR Sensor Settings")
        }
    }
}

@Composable
fun NetworkSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Network Settings")
        }
    }
}

@Composable
fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = "About IRCamera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Version 2.0.0\nBuild 2024.01.15",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\DeviceTypeComposeActivity.kt =====

package mpdc4gsr.feature.main.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class IRDeviceType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isTS004: Boolean
) {
    TS004(
        "TS004 Thermal Camera",
        "High-precision thermal imaging device",
        Icons.Default.Thermostat,
        true
    ),
    TC007(
        "TC007 Thermal Camera",
        "Compact thermal imaging solution",
        Icons.Default.CameraAlt,
        false
    )
}

class DeviceTypeViewModel : AppBaseViewModel() {
    private val _selectedDevice = mutableStateOf<IRDeviceType?>(null)
    val selectedDevice: State<IRDeviceType?> = _selectedDevice
    private val _availableDevices = mutableStateOf(IRDeviceType.values().toList())
    val availableDevices: State<List<IRDeviceType>> = _availableDevices
    fun selectDevice(device: IRDeviceType) {
        _selectedDevice.value = device
    }

    fun getDeviceList(): List<IRDeviceType> {
        return listOf(
            IRDeviceType.TS004,
            IRDeviceType.TC007
        )
    }
}

class DeviceTypeComposeActivity : BaseComposeActivity<DeviceTypeViewModel>() {
    private val deviceTypeVM: DeviceTypeViewModel by viewModels()
    override fun createViewModel(): DeviceTypeViewModel = deviceTypeVM

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DeviceTypeViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val selectedDevice by viewModel.selectedDevice
            val devices = viewModel.getDeviceList()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = stringResource(R.string.device_type_selection),
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeviceHub,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select Device Type",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose the thermal camera device you want to connect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    // Device list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(devices) { device ->
                            DeviceTypeCard(
                                device = device,
                                isSelected = selectedDevice == device,
                                onDeviceClick = { selectedDevice ->
                                    viewModel.selectDevice(selectedDevice)
                                    // Navigate based on device type
                                    when (selectedDevice) {
                                        IRDeviceType.TS004 -> {
                                            NavigationManager.getInstance()
                                                .build(RouterConfig.IR_DEVICE_ADD)
                                                .withBoolean("isTS004", true)
                                                .navigation(context as DeviceTypeComposeActivity)
                                        }

                                        IRDeviceType.TC007 -> {
                                            NavigationManager.getInstance()
                                                .build(RouterConfig.IR_DEVICE_ADD)
                                                .withBoolean("isTS004", false)
                                                .navigation(context as DeviceTypeComposeActivity)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Information section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Device Information",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Make sure your thermal camera device is powered on and ready for connection before proceeding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceTypeCard(
    device: IRDeviceType,
    isSelected: Boolean,
    onDeviceClick: (IRDeviceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDeviceClick(device) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = device.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Device info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\DiagnosticsScreen.kt =====

package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.device.presentation.DiagnosticsViewModel
import mpdc4gsr.feature.device.presentation.DiagnosticsViewModelFactory

@Composable
fun DiagnosticsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: DiagnosticsViewModel = viewModel(
        factory = DiagnosticsViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemStatus by viewModel.systemStatus.collectAsState()
    val sensorStatus by viewModel.sensorStatus.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Diagnostics",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Status
            SettingsCard(
                title = "System Status",
                icon = Icons.Default.Computer
            ) {
                SettingsRow(
                    label = "System Health",
                    value = systemStatus.systemHealth
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Battery",
                    value = systemStatus.battery
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Temperature",
                    value = systemStatus.temperature
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Memory Usage",
                    value = systemStatus.memoryUsage
                )
            }
            // Sensor Status
            SettingsCard(
                title = "Sensor Status",
                icon = Icons.Default.Sensors
            ) {
                SettingsRow(
                    label = "GSR Sensor",
                    value = sensorStatus.gsrSensor
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Thermal Camera",
                    value = sensorStatus.thermalCamera
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "RGB Camera",
                    value = sensorStatus.rgbCamera
                )
            }
            // Diagnostic Tools
            SettingsCard(
                title = "Diagnostic Tools",
                icon = Icons.Default.Build
            ) {
                Button(
                    onClick = { viewModel.runFullDiagnostics() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Full Diagnostics")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.testAllSensors() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test All Sensors")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.exportDiagnosticLogs() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Diagnostic Logs")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsScreenPreview() {
    IRCameraTheme {
        DiagnosticsScreen()
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\MainActivity.kt =====

package mpdc4gsr.feature.main.ui

typealias MainActivity = MainComposeActivity


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\MainComposeActivity.kt =====

package mpdc4gsr.feature.main.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.network.ui.ConnectScreen
import mpdc4gsr.feature.network.ui.NetworkSettingsScreen
import mpdc4gsr.feature.settings.ui.*
import mpdc4gsr.feature.thermal.ui.*

private object MainNavRoutes {
    const val MAIN = "main"
    const val UNIFIED_DASHBOARD = "unified_dashboard"
    const val GSR_SENSOR = "gsr_sensor"
    const val RGB_CAMERA = "rgb_camera"
    const val THERMAL_CONNECT = "thermal_connect"
    const val THERMAL_MONITOR = "thermal_monitor"
    const val THERMAL_CALIBRATE = "thermal_calibrate"
    const val THERMAL_ANNOTATE = "thermal_annotate"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val GSR_SETTINGS = "gsr_settings"
    const val CAMERA_SETTINGS = "camera_settings"
    const val THERMAL_SETTINGS = "thermal_settings"
    const val RECORDING_SETTINGS = "recording_settings"
    const val STORAGE_SETTINGS = "storage_settings"
    const val SYNC_SETTINGS = "sync_settings"
    const val CALIBRATION = "calibration"
    const val NETWORK_SETTINGS = "network_settings"
    const val DIAGNOSTICS = "diagnostics"
    const val APP_INFO = "app_info"
    const val PRIVACY_POLICY = "privacy_policy"
    const val HELP = "help"
}

class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IRCameraTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = MainNavRoutes.MAIN
    ) {
        // Main screen with bottom navigation
        composable(MainNavRoutes.MAIN) {
            MainScreen(
                onNavigateToSensors = { navController.navigate(MainNavRoutes.UNIFIED_DASHBOARD) },
                onNavigateToGallery = { navController.navigate(MainNavRoutes.GALLERY) },
                onNavigateToSettings = { navController.navigate(MainNavRoutes.SETTINGS) },
                onNavigateToProfile = { navController.navigate(MainNavRoutes.PROFILE) },
                onNavigateToSensor = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(MainNavRoutes.GSR_SENSOR)
                        SensorType.ThermalIR -> navController.navigate(MainNavRoutes.THERMAL_CONNECT)
                        SensorType.RGBCamera -> navController.navigate(MainNavRoutes.RGB_CAMERA)
                    }
                }
            )
        }
        // Unified sensor dashboard
        composable(MainNavRoutes.UNIFIED_DASHBOARD) {
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.SETTINGS) },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(MainNavRoutes.GSR_SENSOR)
                        SensorType.ThermalIR -> navController.navigate(MainNavRoutes.THERMAL_CONNECT)
                        SensorType.RGBCamera -> navController.navigate(MainNavRoutes.RGB_CAMERA)
                    }
                },
                onCameraSettingsClick = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onGSRSettingsClick = { navController.navigate(MainNavRoutes.GSR_SETTINGS) },
                onThermalSettingsClick = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) }
            )
        }
        // Individual sensor screens
        composable(MainNavRoutes.GSR_SENSOR) {
            GSRSensorScreen(
                navController = navController
            )
        }
        composable(MainNavRoutes.RGB_CAMERA) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onCapturePhoto = {
                    // Capture photo functionality
                }
            )
        }
        // Thermal camera workflow
        composable(MainNavRoutes.THERMAL_CONNECT) {
            ConnectScreen(
                onDeviceSelected = { device ->
                    navController.navigate(MainNavRoutes.THERMAL_MONITOR)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_MONITOR) {
            ThermalMonitorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) },
                onRecordClick = {
                    // Recording functionality
                }
            )
        }
        composable(MainNavRoutes.THERMAL_CALIBRATE) {
            CalibrateScreen(
                onBackClick = { navController.popBackStack() },
                onCalibrationComplete = {
                    navController.popBackStack()
                },
                onCalibrationCancel = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_ANNOTATE) {
            AnnotateScreen(
                onBackClick = { navController.popBackStack() },
                onSave = {
                    // Save functionality
                },
                onShare = {
                    // Share functionality
                }
            )
        }
        // Additional screens
        composable(MainNavRoutes.GALLERY) {
            GalleryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate(MainNavRoutes.GSR_SETTINGS) },
                onNavigateToThermalSettings = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) },
                onNavigateToCameraSettings = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onNavigateToRecordingSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.RecordingSettings.route) },
                onNavigateToStorageSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.StorageSettings.route) },
                onNavigateToSyncSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.SyncSettings.route) },
                onNavigateToCalibration = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Calibration.route) },
                onNavigateToNetworkSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.NetworkSettings.route) },
                onNavigateToDiagnostics = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Diagnostics.route) },
                onNavigateToAppInfo = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.AppInfo.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.PrivacyPolicy.route) },
                onNavigateToHelp = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Help.route) }
            )
        }
        composable(MainNavRoutes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        // Settings screens
        composable(MainNavRoutes.GSR_SETTINGS) {
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.CAMERA_SETTINGS) {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_SETTINGS) {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.RECORDING_SETTINGS) {
            RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.STORAGE_SETTINGS) {
            StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.SYNC_SETTINGS) {
            SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.CALIBRATION) {
            CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.NETWORK_SETTINGS) {
            NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.APP_INFO) {
            AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.HELP) {
            HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\MainScreen.kt =====

package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun MainScreen(
    onNavigateToSensors: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSensor: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar
        TitleBar(
            title = "IR Camera",
            showBackButton = false
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onNavigateToSettings
            )
        }
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> SensorDashboardTab(
                    onSensorClick = onNavigateToSensor
                )

                1 -> GalleryTab(onNavigateToGallery = onNavigateToGallery)
                2 -> ProfileTab(onNavigateToProfile = onNavigateToProfile)
            }
        }
        // Bottom navigation
        NavigationBar(
            containerColor = Color(0xFF2A2A2A)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Sensors") },
                label = { Text("Sensors") },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Photo, contentDescription = "Gallery") },
                label = { Text("Gallery") },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = false,
                onClick = { onNavigateToSettings() },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun SensorDashboardTab(
    onSensorClick: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Use remember for stable state holders to prevent unnecessary recomposition
    var gsrState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    var thermalState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    var rgbState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    // Memoize action handlers to prevent recreating lambdas on every recomposition
    val gsrActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.GSRAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.GSRAction.Connect ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Connecting

                is mpdc4gsr.core.ui.model.GSRAction.Disconnect ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Disconnected

                is mpdc4gsr.core.ui.model.GSRAction.StartStream ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Streaming

                is mpdc4gsr.core.ui.model.GSRAction.StopStream ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Connected

                is mpdc4gsr.core.ui.model.GSRAction.ConfigureDevice -> {}
            }
        }
    }
    val thermalActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.ThermalAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.ThermalAction.Connect ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Connecting

                is mpdc4gsr.core.ui.model.ThermalAction.Disconnect ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Disconnected

                is mpdc4gsr.core.ui.model.ThermalAction.StartPreview ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Streaming

                is mpdc4gsr.core.ui.model.ThermalAction.StopPreview ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Connected

                is mpdc4gsr.core.ui.model.ThermalAction.Calibrate -> {}
                is mpdc4gsr.core.ui.model.ThermalAction.OpenSettings -> {}
            }
        }
    }
    val rgbActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.CameraAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.CameraAction.Connect ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Connecting

                is mpdc4gsr.core.ui.model.CameraAction.Disconnect ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Disconnected

                is mpdc4gsr.core.ui.model.CameraAction.StartPreview ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Streaming

                is mpdc4gsr.core.ui.model.CameraAction.StopPreview ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Connected

                is mpdc4gsr.core.ui.model.CameraAction.SetResolution -> {}
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "MPDC4GSR",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-sensor data collection platform for GSR",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        // System status overview
        SystemStatusOverview()
        // Sensor cards for direct access
        // Using memoized handlers to prevent unnecessary recomposition
        mpdc4gsr.core.ui.components.sensors.GSRSensorCard(
            state = gsrState,
            onStateChange = { gsrState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.GSR) },
            onAction = gsrActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.ThermalSensorCard(
            state = thermalState,
            onStateChange = { thermalState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.ThermalIR) },
            onAction = thermalActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.RGBCameraSensorCard(
            state = rgbState,
            onStateChange = { rgbState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.RGBCamera) },
            onAction = rgbActionHandler
        )
    }
}

@Composable
private fun GalleryTab(
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Media Gallery",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View thermal images, recordings, and data exports",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Open Gallery")
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "User account, research templates, and data management",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("View Profile")
                }
            }
        }
    }
}

@Composable
private fun SystemStatusOverview(
    modifier: Modifier = Modifier
) {
    // Memoize colors to prevent recomposition when theme changes
    val connectedColor = Color.Green
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Status",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("GSR", "Connected", connectedColor)
                StatusItem("Thermal", "Ready", primaryColor)
                StatusItem("RGB", "Active", connectedColor)
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = status,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    IRCameraTheme {
        MainScreen()
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\UnifiedComposeActivity.kt =====

package mpdc4gsr.feature.main.ui

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import mpdc4gsr.core.ui.navigation.UnifiedNavHost

class UnifiedComposeActivity : BaseComposeActivity<UnifiedComposeViewModel>() {
    override fun createViewModel(): UnifiedComposeViewModel {
        return viewModels<UnifiedComposeViewModel>().value
    }

    @Composable
    override fun Content(viewModel: UnifiedComposeViewModel) {
        UnifiedNavHost()
    }
}

class UnifiedComposeViewModel : BaseViewModel() {
    // This activity primarily handles navigation, so minimal ViewModel needed
    // Future enhancements could include:
    // - Global app state management
    // - User preferences
    // - Authentication state
    // - Network connectivity monitoring
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\main\ui\UnifiedSensorDashboard.kt =====

package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.components.NavigationBreadcrumb
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.components.sensors.GSRSensorCard
import mpdc4gsr.core.ui.components.sensors.RGBCameraSensorCard
import mpdc4gsr.core.ui.components.sensors.ThermalSensorCard
import mpdc4gsr.core.ui.components.sensors.UnifiedSensorStatus
import mpdc4gsr.core.ui.model.*
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun UnifiedSensorDashboard(
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSensorClick: (SensorType) -> Unit = {},
    onCameraSettingsClick: () -> Unit = {},
    onGSRSettingsClick: () -> Unit = {},
    onThermalSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sensor states - showing disconnected until actual ViewModels are connected
    var gsrState by remember { mutableStateOf(SensorState.Disconnected) }
    var thermalState by remember { mutableStateOf(SensorState.Disconnected) }
    var rgbState by remember { mutableStateOf(SensorState.Disconnected) }
    var unifiedState by remember { mutableStateOf(UnifiedSystemState.Inactive) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar with user-friendly name
        TitleBar(
            title = "Sensor Overview",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Sensor Settings",
                onClick = onSettingsClick
            )
        }
        // Breadcrumb navigation for context
        NavigationBreadcrumb(
            currentScreen = "Sensor Overview",
            previousScreen = "Home"
        )
        // Scrollable sensor content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unified system status overview
            UnifiedSensorStatus(
                systemState = unifiedState,
                activeSensors = listOf(
                    SensorInfo(SensorType.GSR, gsrState),
                    SensorInfo(SensorType.ThermalIR, thermalState),
                    SensorInfo(SensorType.RGBCamera, rgbState)
                ),
                onSystemAction = { action ->
                    when (action) {
                        is SystemAction.StartRecording -> {
                            unifiedState = UnifiedSystemState.Recording
                        }

                        is SystemAction.StopRecording -> {
                            unifiedState = UnifiedSystemState.Active
                        }

                        is SystemAction.Synchronize -> {
                            // Trigger sensor synchronization
                        }
                    }
                }
            )
            // Individual sensor cards
            GSRSensorCard(
                state = gsrState,
                onStateChange = { gsrState = it },
                onClick = { onSensorClick(SensorType.GSR) },
                onAction = { action ->
                    when (action) {
                        is GSRAction.Connect -> gsrState = SensorState.Connecting
                        is GSRAction.Disconnect -> gsrState = SensorState.Disconnected
                        is GSRAction.StartStream -> gsrState = SensorState.Streaming
                        is GSRAction.StopStream -> gsrState = SensorState.Connected
                        is GSRAction.ConfigureDevice -> {
                        }
                    }
                },
                onSettingsClick = onGSRSettingsClick
            )
            ThermalSensorCard(
                state = thermalState,
                onStateChange = { thermalState = it },
                onClick = { onSensorClick(SensorType.ThermalIR) },
                onAction = { action ->
                    when (action) {
                        is ThermalAction.Connect -> thermalState = SensorState.Connecting
                        is ThermalAction.Disconnect -> thermalState = SensorState.Disconnected
                        is ThermalAction.StartPreview -> thermalState = SensorState.Streaming
                        is ThermalAction.StopPreview -> thermalState = SensorState.Connected
                        is ThermalAction.Calibrate -> {
                        }

                        is ThermalAction.OpenSettings -> {
                        }
                    }
                },
                onSettingsClick = onThermalSettingsClick
            )
            RGBCameraSensorCard(
                state = rgbState,
                onStateChange = { rgbState = it },
                onClick = { onSensorClick(SensorType.RGBCamera) },
                onAction = { action ->
                    when (action) {
                        is CameraAction.Connect -> rgbState = SensorState.Connecting
                        is CameraAction.Disconnect -> rgbState = SensorState.Disconnected
                        is CameraAction.StartPreview -> rgbState = SensorState.Streaming
                        is CameraAction.StopPreview -> rgbState = SensorState.Connected
                        is CameraAction.SetResolution -> {
                        }
                    }
                },
                onSettingsClick = onCameraSettingsClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnifiedSensorDashboardPreview() {
    IRCameraTheme {
        UnifiedSensorDashboard()
    }
}