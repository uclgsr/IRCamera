package mpdc4gsr.feature.network.presentation

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.network.data.NetworkClient

class DevicePairingViewModel : AppBaseViewModel(), NetworkClient.NetworkEventListener {
    // StateFlow for reactive state management
    private val _discoveredControllers =
        MutableStateFlow<List<NetworkClient.ControllerInfo>>(emptyList())
    val discoveredControllers: StateFlow<List<NetworkClient.ControllerInfo>> =
        _discoveredControllers.asStateFlow()
    private val _connectedController = MutableStateFlow<NetworkClient.ControllerInfo?>(null)
    val connectedController: StateFlow<NetworkClient.ControllerInfo?> =
        _connectedController.asStateFlow()
    private val _pairingConnectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val pairingConnectionState: StateFlow<ConnectionState> = _pairingConnectionState.asStateFlow()
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<PairingEvent>()
    val events: SharedFlow<PairingEvent> = _events.asSharedFlow()

    // Flash overlay state
    private val _flashState = MutableStateFlow(FlashState())
    val flashState: StateFlow<FlashState> = _flashState.asStateFlow()

    // Combined state for complex UI scenarios
    private val _pairingScreenState = MutableStateFlow(PairingScreenState())
    val pairingScreenState: StateFlow<PairingScreenState> = _pairingScreenState.asStateFlow()
    private lateinit var networkClient: NetworkClient
    private val controllers = mutableListOf<NetworkClient.ControllerInfo>()

    // Enhanced state enums
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CONNECTION_FAILED,
        AUTHENTICATING,
        AUTHENTICATED
    }

    enum class ScanState {
        IDLE,
        SCANNING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    // Sealed classes for type-safe event handling
    sealed class PairingEvent {
        data class ShowError(val message: String) : PairingEvent()
        data class ShowSuccess(val message: String) : PairingEvent()
        data class NavigateToSession(val sessionInfo: SessionInfo) : PairingEvent()
        data class ShowConnectionDialog(val controller: NetworkClient.ControllerInfo) :
            PairingEvent()

        object NavigateBack : PairingEvent()
    }

    data class PairingScreenState(
        val isInitialized: Boolean = false,
        val canScan: Boolean = true,
        val canConnect: Boolean = false,
        val showProgress: Boolean = false,
        val discoveredCount: Int = 0,
        val lastScanTime: Long? = null
    )

    data class FlashState(
        val isVisible: Boolean = false,
        val durationMs: Int = 0
    )

    init {
        // Setup combined state management
        viewModelScope.launch {
            combine(
                _scanState,
                _pairingConnectionState,
                _discoveredControllers
            ) { scanState, connectionState, controllers ->
                PairingScreenState(
                    isInitialized = ::networkClient.isInitialized,
                    canScan = scanState != ScanState.SCANNING && connectionState != ConnectionState.CONNECTING,
                    canConnect = controllers.isNotEmpty() && connectionState == ConnectionState.DISCONNECTED,
                    showProgress = scanState == ScanState.SCANNING || connectionState == ConnectionState.CONNECTING,
                    discoveredCount = controllers.size,
                    lastScanTime = if (scanState == ScanState.COMPLETED) System.currentTimeMillis() else null
                )
            }.collect { newState ->
                _pairingScreenState.value = newState
            }
        }
    }

    fun initialize(context: android.content.Context) {
        launchWithErrorHandling {
            networkClient = NetworkClient(context)
            networkClient.setEventListener(this@DevicePairingViewModel)
            _pairingConnectionState.value = ConnectionState.DISCONNECTED
            _scanState.value = ScanState.IDLE
            _statusMessage.value = "Ready to scan for PC Controllers"
            _discoveredControllers.value = emptyList()
            _events.emit(PairingEvent.ShowSuccess("Network client initialized successfully"))
        }
    }

    fun startControllerScan(forceRefresh: Boolean = false) {
        launchWithErrorHandling {
            val currentScanState = _scanState.value
            if (currentScanState == ScanState.SCANNING) {
                return@launchWithErrorHandling // Already scanning
            }
            _scanState.value = ScanState.SCANNING
            _statusMessage.value = "Scanning for PC Controllers..."
            if (forceRefresh) {
                controllers.clear()
                _discoveredControllers.value = emptyList()
            }
                val foundControllers = networkClient.discoverControllers()
                controllers.clear()
                controllers.addAll(foundControllers)
                _discoveredControllers.value = controllers.toList()
                val message = if (foundControllers.isNotEmpty()) {
                    "Found ${foundControllers.size} PC Controller(s)"
                } else {
                    "No PC Controllers found. Make sure you're on the same network."
                }
                _statusMessage.value = message
                _scanState.value = ScanState.COMPLETED
                if (foundControllers.isNotEmpty()) {
                    _events.emit(PairingEvent.ShowSuccess(message))
                } else {
                    _events.emit(PairingEvent.ShowError("No controllers found"))
                }
                _statusMessage.value = errorMessage
                _scanState.value = ScanState.FAILED
            }
        }
    }

    fun connectToController(controller: NetworkClient.ControllerInfo) {
        launchWithLoading {
            val currentConnectionState = _pairingConnectionState.value
            if (currentConnectionState == ConnectionState.CONNECTING) {
                return@launchWithLoading // Already connecting
            }
                _pairingConnectionState.value = ConnectionState.CONNECTING
                _statusMessage.value = "Connecting to ${controller.deviceName}..."
                // Show connection dialog
                _events.emit(PairingEvent.ShowConnectionDialog(controller))
                val success = networkClient.connectToController(
                    ipAddress = controller.ipAddress,
                    port = controller.port
                )
                if (success) {
                    _connectedController.value = controller
                    _pairingConnectionState.value = ConnectionState.CONNECTED
                    _statusMessage.value = "Connected to ${controller.deviceName}"
                    _events.emit(PairingEvent.ShowSuccess("Successfully connected to ${controller.deviceName}"))
                } else {
                    _pairingConnectionState.value = ConnectionState.CONNECTION_FAILED
                    _statusMessage.value = "Failed to connect to ${controller.deviceName}"
                    _events.emit(PairingEvent.ShowError("Connection failed"))
                }
                _pairingConnectionState.value = ConnectionState.CONNECTION_FAILED
            }
        }
    }

    fun disconnectFromController() {
        launchWithErrorHandling {
            val currentController = _connectedController.value
            if (currentController != null) {
                    networkClient.disconnect()
                    _connectedController.value = null
                    _pairingConnectionState.value = ConnectionState.DISCONNECTED
                    _statusMessage.value = "Disconnected from ${currentController.deviceName}"
                    _events.emit(PairingEvent.ShowSuccess("Disconnected successfully"))
                }
            }
        }
    }

    fun retryConnection() {
        val lastController = _connectedController.value
        if (lastController != null) {
            connectToController(lastController)
        } else {
            startControllerScan(forceRefresh = true)
        }
    }

    fun startSession(sessionInfo: SessionInfo) {
        launchWithErrorHandling {
            val currentController = _connectedController.value
            if (currentController != null && _pairingConnectionState.value == ConnectionState.CONNECTED) {
                // Navigate to session directly - NetworkClient handles data streaming
                _events.emit(PairingEvent.NavigateToSession(sessionInfo))
            } else {
                _events.emit(PairingEvent.ShowError("No active connection"))
            }
        }
    }

    // NetworkClient.NetworkEventListener implementation
    override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
        viewModelScope.launch {
            if (!controllers.contains(controller)) {
                controllers.add(controller)
                _discoveredControllers.value = controllers.toList()
                _statusMessage.value = "Found ${controllers.size} controller(s)"
            }
        }
    }

    override fun onConnected(controller: NetworkClient.ControllerInfo) {
        viewModelScope.launch {
            _connectedController.value = controller
            _pairingConnectionState.value = ConnectionState.CONNECTED
            _statusMessage.value = "Connected to ${controller.deviceName}"
            _events.emit(PairingEvent.ShowSuccess("Connection established"))
        }
    }

    override fun onDisconnected(reason: String) {
        viewModelScope.launch {
            _connectedController.value = null
            _pairingConnectionState.value = ConnectionState.DISCONNECTED
            _statusMessage.value = "Connection lost: $reason"
            _events.emit(PairingEvent.ShowError("Connection lost: $reason"))
        }
    }

    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        viewModelScope.launch {
            _events.emit(PairingEvent.NavigateToSession(sessionInfo))
        }
    }

    override fun onSyncFlash(durationMs: Int) {
        triggerSyncFlash(durationMs)
    }

    override fun onTimeSynchronized(offsetNanoseconds: Long) {
        viewModelScope.launch {
            _statusMessage.value = "Time synchronized (offset: ${offsetNanoseconds}ns)"
        }
    }

    override fun onDataStreamingStarted() {
        viewModelScope.launch {
            _statusMessage.value = "Data streaming started"
        }
    }

    override fun onDataStreamingStopped() {
        viewModelScope.launch {
            _statusMessage.value = "Data streaming stopped"
        }
    }

    override fun onError(operation: String, error: String) {
        viewModelScope.launch {
            _events.emit(PairingEvent.ShowError("$operation: $error"))
        }
    }

    fun triggerSyncFlash(durationMs: Int) {
        viewModelScope.launch {
            _flashState.value = FlashState(isVisible = true, durationMs = durationMs)
            kotlinx.coroutines.delay(durationMs.toLong())
            _flashState.value = FlashState(isVisible = false, durationMs = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::networkClient.isInitialized) {
            networkClient.disconnect()
        }
    }

    companion object {
    }
}