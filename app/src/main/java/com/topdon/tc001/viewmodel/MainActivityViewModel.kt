package com.topdon.tc001.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.topdon.tc001.sensors.thermal.ThermalRecorder
import com.topdon.tc001.network.NetworkClient
import com.topdon.tc001.network.NetworkController
import com.topdon.tc001.sync.SessionManager
import com.topdon.gsr.service.SessionManager as GSRSessionManager
import com.topdon.gsr.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for MainActivity managing GSR sensor status, network connectivity, and session control.
 * Implements the UI state management requirements from the problem statement.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "MainActivityViewModel"
    }
    
    // GSR Sensor State
    private val _gsrConnectionState = MutableLiveData<GSRConnectionState>()
    val gsrConnectionState: LiveData<GSRConnectionState> = _gsrConnectionState
    
    private val _gsrBatteryLevel = MutableLiveData<Int?>()
    val gsrBatteryLevel: LiveData<Int?> = _gsrBatteryLevel
    
    // Network State
    private val _networkConnectionState = MutableLiveData<NetworkConnectionState>()
    val networkConnectionState: LiveData<NetworkConnectionState> = _networkConnectionState
    
    private val _connectedControllerInfo = MutableLiveData<NetworkClient.ControllerInfo?>()
    val connectedControllerInfo: LiveData<NetworkClient.ControllerInfo?> = _connectedControllerInfo
    
    // Session State
    private val _sessionState = MutableLiveData<SessionState>()
    val sessionState: LiveData<SessionState> = _sessionState
    
    private val _currentSession = MutableLiveData<SessionInfo?>()
    val currentSession: LiveData<SessionInfo?> = _currentSession
    
    // Error and Status Messages
    private val _statusMessage = MutableLiveData<StatusMessage?>()
    val statusMessage: LiveData<StatusMessage?> = _statusMessage
    
    // Component instances
    private var gsrSensorRecorder: GSRSensorRecorder? = null
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
        // Initialize with disconnected states
        _gsrConnectionState.value = GSRConnectionState.DISCONNECTED
        _networkConnectionState.value = NetworkConnectionState.DISCONNECTED
        _sessionState.value = SessionState.IDLE
        
        Log.d(TAG, "MainActivityViewModel initialized")
    }
    
    fun initializeComponents() {
        viewModelScope.launch {
            try {
                // Initialize GSR components
                initializeGSRComponents()
                
                // Initialize Thermal components
                initializeThermalComponents()
                
                // Initialize Network components
                initializeNetworkComponents()
                
                // Initialize Session management
                initializeSessionComponents()
                
                Log.i(TAG, "All components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize components", e)
                _statusMessage.value = StatusMessage(
                    "Failed to initialize components: ${e.message}",
                    StatusMessage.Level.ERROR
                )
            }
        }
    }
    
    private suspend fun initializeGSRComponents() = withContext(Dispatchers.IO) {
        try {
            gsrSensorRecorder = GSRSensorRecorder(getApplication())
            gsrSessionManager = GSRSessionManager.getInstance(getApplication())
            
            // Set up GSR state monitoring
            _gsrConnectionState.postValue(GSRConnectionState.DISCONNECTED)
            
            Log.d(TAG, "GSR components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR components", e)
            _gsrConnectionState.postValue(GSRConnectionState.ERROR)
            throw e
        }
    }
    
    private suspend fun initializeThermalComponents() = withContext(Dispatchers.IO) {
        try {
            thermalRecorder = ThermalRecorder(getApplication())
            
            // Set up thermal frame listener
            thermalRecorder?.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
                override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                    Log.d(TAG, "Thermal frame processed: ${stats.frameSequence} - T=${stats.minTemp}°C to ${stats.maxTemp}°C")
                }
                
                override fun onError(error: String) {
                    Log.e(TAG, "Thermal recorder error: $error")
                    _statusMessage.postValue(
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
            
            // Set up NetworkController event listener for PC remote commands
            networkController?.setEventListener(object : NetworkController.NetworkControllerListener {
                override fun onStartRecordingCommand(sessionId: String, modalities: List<String>, options: Map<String, Any>) {
                    Log.i(TAG, "Remote start recording command: sessionId=$sessionId, modalities=$modalities")
                    
                    val config = SessionConfig(
                        sessionId = sessionId,
                        participantId = options["participantId"] as? String,
                        studyName = options["studyName"] as? String,
                        modalities = modalities,
                        saveImages = options["saveImages"] as? Boolean ?: false
                    )
                    
                    // Start recording session via coroutine
                    viewModelScope.launch {
                        startRecordingSession(config)
                    }
                }
                
                override fun onStopRecordingCommand() {
                    Log.i(TAG, "Remote stop recording command received")
                    
                    // Stop recording session via coroutine
                    viewModelScope.launch {
                        stopRecordingSession()
                    }
                }
                
                override fun onClientConnected(clientId: String, clientInfo: String) {
                    Log.i(TAG, "PC client connected: $clientId ($clientInfo)")
                    _statusMessage.postValue(
                        StatusMessage("PC client connected: $clientInfo", StatusMessage.Level.INFO)
                    )
                }
                
                override fun onClientDisconnected(clientId: String, reason: String) {
                    Log.i(TAG, "PC client disconnected: $clientId - $reason")
                    _statusMessage.postValue(
                        StatusMessage("PC client disconnected: $reason", StatusMessage.Level.WARNING)
                    )
                }
                
                override fun onError(operation: String, error: String) {
                    Log.e(TAG, "NetworkController error in $operation: $error")
                    _statusMessage.postValue(
                        StatusMessage("PC control error: $error", StatusMessage.Level.ERROR)
                    )
                }
            })
            
            // Start NetworkController server
            viewModelScope.launch {
                val serverStarted = networkController?.start(NetworkController.DEFAULT_PORT)
                if (serverStarted == true) {
                    Log.i(TAG, "NetworkController server started on port ${NetworkController.DEFAULT_PORT}")
                    _statusMessage.postValue(
                        StatusMessage("PC remote control ready on port ${NetworkController.DEFAULT_PORT}", StatusMessage.Level.INFO)
                    )
                } else {
                    Log.w(TAG, "Failed to start NetworkController server")
                }
            }
            
            // Set up NetworkClient event listener for discovery
            networkClient?.setEventListener(object : NetworkClient.NetworkEventListener {
                override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.postValue(NetworkConnectionState.DISCOVERING)
                    Log.d(TAG, "PC Controller discovered: ${controller.deviceName}")
                }
                
                override fun onConnected(controller: NetworkClient.ControllerInfo) {
                    _networkConnectionState.postValue(NetworkConnectionState.CONNECTED)
                    _connectedControllerInfo.postValue(controller)
                    _statusMessage.postValue(
                        StatusMessage("Connected to PC: ${controller.deviceName}", StatusMessage.Level.INFO)
                    )
                    Log.i(TAG, "Connected to PC controller: ${controller.deviceName}")
                }
                
                override fun onDisconnected(reason: String) {
                    _networkConnectionState.postValue(NetworkConnectionState.DISCONNECTED)
                    _connectedControllerInfo.postValue(null)
                    _statusMessage.postValue(
                        StatusMessage("Disconnected from PC: $reason", StatusMessage.Level.WARNING)
                    )
                    Log.w(TAG, "Disconnected from PC controller: $reason")
                }
                
                override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                    handleRemoteRecordingRequest(sessionInfo)
                }
                
                override fun onSyncFlash(durationMs: Int) {
                    // Handle sync flash for timestamp alignment
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
                    _statusMessage.postValue(
                        StatusMessage("Network error in $operation: $error", StatusMessage.Level.ERROR)
                    )
                    Log.e(TAG, "Network error in $operation: $error")
                }
            })
            
            _networkConnectionState.postValue(NetworkConnectionState.DISCONNECTED)
            Log.d(TAG, "Network components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize network components", e)
            _networkConnectionState.postValue(NetworkConnectionState.ERROR)
            throw e
        }
    }
    
    private suspend fun initializeSessionComponents() = withContext(Dispatchers.IO) {
        try {
            sessionManager = SessionManager(
                getApplication(),
                com.topdon.tc001.logging.StructuredLogger(getApplication())
            )
            
            _sessionState.postValue(SessionState.IDLE)
            Log.d(TAG, "Session components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize session components", e)
            _sessionState.postValue(SessionState.ERROR)
            throw e
        }
    }
    
    /**
     * Start GSR sensor discovery and connection
     */
    fun startGSRConnection() {
        viewModelScope.launch {
            try {
                _gsrConnectionState.value = GSRConnectionState.DISCOVERING
                _statusMessage.value = StatusMessage("Searching for GSR sensor...", StatusMessage.Level.INFO)
                
                // TODO: Implement actual GSR connection logic
                withContext(Dispatchers.IO) {
                    gsrSensorRecorder?.let { recorder ->
                        val success = recorder.initialize()
                        if (success) {
                            _gsrConnectionState.postValue(GSRConnectionState.CONNECTED)
                            _statusMessage.postValue(
                                StatusMessage("GSR sensor connected", StatusMessage.Level.INFO)
                            )
                        } else {
                            _gsrConnectionState.postValue(GSRConnectionState.ERROR)
                            _statusMessage.postValue(
                                StatusMessage("Failed to connect GSR sensor", StatusMessage.Level.ERROR)
                            )
                        }
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
    
    /**
     * Start network discovery for PC controllers
     */
    fun startNetworkDiscovery() {
        viewModelScope.launch {
            try {
                _networkConnectionState.value = NetworkConnectionState.DISCOVERING
                _statusMessage.value = StatusMessage("Searching for PC controllers...", StatusMessage.Level.INFO)
                
                withContext(Dispatchers.IO) {
                    networkClient?.let { client ->
                        val controllers = client.discoverControllers()
                        if (controllers.isNotEmpty()) {
                            // Auto-connect to first discovered controller
                            val controller = controllers.first()
                            val connected = client.connectToController(controller)
                            if (connected) {
                                _networkConnectionState.postValue(NetworkConnectionState.CONNECTED)
                                _connectedControllerInfo.postValue(controller)
                            }
                        } else {
                            _networkConnectionState.postValue(NetworkConnectionState.DISCONNECTED)
                            _statusMessage.postValue(
                                StatusMessage("No PC controllers found", StatusMessage.Level.WARNING)
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
    
    /**
     * Start a new recording session
     */
    fun startRecordingSession(sessionConfig: SessionConfig = SessionConfig()) {
        viewModelScope.launch {
            try {
                if (_sessionState.value == SessionState.RECORDING) {
                    Log.w(TAG, "Recording already in progress")
                    return@launch
                }
                
                _sessionState.value = SessionState.STARTING
                _statusMessage.value = StatusMessage("Starting recording session...", StatusMessage.Level.INFO)
                
                withContext(Dispatchers.IO) {
                    // Create session via GSR session manager
                    gsrSessionManager?.let { manager ->
                        val session = manager.createSession(
                            sessionId = sessionConfig.sessionId,
                            participantId = sessionConfig.participantId,
                            studyName = sessionConfig.studyName,
                            metadata = sessionConfig.metadata
                        )
                        
                        // Start thermal recording if requested
                        if (sessionConfig.modalities.contains("thermal")) {
                            val sessionDir = "/storage/emulated/0/IRCamera/Sessions/${session.sessionId}"
                            val thermalStarted = thermalRecorder?.startRecording(sessionDir, sessionConfig.saveImages)
                            if (thermalStarted == true) {
                                Log.i(TAG, "Thermal recording started for session: ${session.sessionId}")
                            } else {
                                Log.w(TAG, "Failed to start thermal recording")
                            }
                        }
                        
                        // Start GSR recording if requested
                        if (sessionConfig.modalities.contains("GSR")) {
                            val gsrStarted = gsrSensorRecorder?.initialize()
                            if (gsrStarted == true) {
                                Log.i(TAG, "GSR recording started for session: ${session.sessionId}")
                            } else {
                                Log.w(TAG, "Failed to start GSR recording")
                            }
                        }
                        
                        _currentSession.postValue(session)
                        _sessionState.postValue(SessionState.RECORDING)
                        _statusMessage.postValue(
                            StatusMessage("Recording session started: ${session.sessionId}", StatusMessage.Level.INFO)
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
    
    /**
     * Stop the current recording session
     */
    fun stopRecordingSession() {
        viewModelScope.launch {
            try {
                if (_sessionState.value != SessionState.RECORDING) {
                    Log.w(TAG, "No recording session in progress")
                    return@launch
                }
                
                _sessionState.value = SessionState.STOPPING
                _statusMessage.value = StatusMessage("Stopping recording session...", StatusMessage.Level.INFO)
                
                withContext(Dispatchers.IO) {
                    _currentSession.value?.let { session ->
                        // Stop thermal recording
                        val thermalStopped = thermalRecorder?.stopRecording()
                        if (thermalStopped == true) {
                            Log.i(TAG, "Thermal recording stopped for session: ${session.sessionId}")
                        }
                        
                        // Stop GSR recording (if applicable)
                        // Note: GSR stopping is handled by completing the session
                        
                        // Complete the session
                        gsrSessionManager?.completeSession(session.sessionId)
                        
                        _currentSession.postValue(null)
                        _sessionState.postValue(SessionState.IDLE)
                        _statusMessage.postValue(
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
    
    /**
     * Handle remote recording request from PC
     */
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
    
    /**
     * Clear status message (typically called after UI shows it)
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
    
    /**
     * Process thermal frame data through ThermalRecorder
     * This method bridges the existing camera system with our unified logging
     */
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
        
        // Clean up NetworkController
        viewModelScope.launch {
            networkController?.stop()
        }
        
        Log.d(TAG, "MainActivityViewModel cleared")
    }
    
    /**
     * Configuration for recording sessions
     */
    data class SessionConfig(
        val sessionId: String? = null,
        val participantId: String? = null,
        val studyName: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val modalities: List<String> = listOf("thermal", "GSR"),
        val saveImages: Boolean = false
    )
}
