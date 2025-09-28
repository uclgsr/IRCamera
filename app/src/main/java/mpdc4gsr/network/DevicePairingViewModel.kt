package mpdc4gsr.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevicePairingViewModel : BaseViewModel(), NetworkClient.NetworkEventListener {

    // LiveData for compatibility with existing observers
    private val _discoveredControllers = MutableLiveData<List<NetworkClient.ControllerInfo>>()
    val discoveredControllers: LiveData<List<NetworkClient.ControllerInfo>> = _discoveredControllers

    private val _connectedController = MutableLiveData<NetworkClient.ControllerInfo?>()
    val connectedController: LiveData<NetworkClient.ControllerInfo?> = _connectedController

    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _scanState = MutableLiveData<ScanState>()
    val scanState: LiveData<ScanState> = _scanState

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    // StateFlow for modern reactive programming
    private val _availableControllers = MutableStateFlow<List<NetworkClient.ControllerInfo>>(emptyList())
    val availableControllers: StateFlow<List<NetworkClient.ControllerInfo>> = _availableControllers.asStateFlow()

    private val _pairingEvents = MutableSharedFlow<PairingEvent>()
    val pairingEvents: SharedFlow<PairingEvent> = _pairingEvents.asSharedFlow()

    private val _pairingUiState = MutableStateFlow(PairingUiState())
    val pairingUiState: StateFlow<PairingUiState> = _pairingUiState.asStateFlow()

    private lateinit var networkClient: NetworkClient
    private val controllers = mutableListOf<NetworkClient.ControllerInfo>()

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
        FAILED
    }

    data class NavigationEvent(
        val action: String,
        val sessionInfo: SessionInfo? = null
    )

    sealed class PairingEvent {
        data class ShowToast(val message: String) : PairingEvent()
        data class ShowError(val message: String) : PairingEvent()
        data class NavigateToRecording(val sessionInfo: SessionInfo) : PairingEvent()
        data class ControllerConnected(val controller: NetworkClient.ControllerInfo) : PairingEvent()
        object NavigateBack : PairingEvent()
        data class NavigateToSession(val sessionInfo: SessionInfo) : PairingEvent()
        data class ShowConnectionDialog(val controller: NetworkClient.ControllerInfo) : PairingEvent()
        data class ShowSuccess(val message: String) : PairingEvent()
    }

    data class PairingUiState(
        val isScanning: Boolean = false,
        val isConnecting: Boolean = false,
        val isLoading: Boolean = false,
        val statusMessage: String = "Ready to scan for PC Controllers",
        val deviceCount: Int = 0
    )

    fun initialize(context: android.content.Context) {
        networkClient = NetworkClient(context)
        networkClient.setEventListener(this)

        _connectionState.value = ConnectionState.DISCONNECTED
        _scanState.value = ScanState.IDLE
        _statusMessage.value = "Ready to scan for PC Controllers"
        _discoveredControllers.value = emptyList()
        
        updateUiState()
    }

    fun startControllerScan() {
        if (_scanState.value == ScanState.SCANNING) {
            return // Already scanning
        }

        _scanState.value = ScanState.SCANNING
        _statusMessage.value = "Scanning for PC Controllers..."
        controllers.clear()
        _discoveredControllers.value = emptyList()
        _availableControllers.value = emptyList()
        
        updateUiState()

        viewModelScope.launch {
            try {
                val foundControllers = networkClient.discoverControllers()
                controllers.addAll(foundControllers)
                _discoveredControllers.value = controllers.toList()
                _availableControllers.value = controllers.toList()

                _statusMessage.value = if (foundControllers.isNotEmpty()) {
                    "Found ${foundControllers.size} PC Controller(s)"
                } else {
                    "No PC Controllers found. Make sure you're on the same network."
                }

                _scanState.value = ScanState.COMPLETED
                updateUiState()

            } catch (e: Exception) {
                _statusMessage.value = "Scan failed: ${e.message}"
                _scanState.value = ScanState.FAILED
                _error.value = "Failed to scan for controllers: ${e.message}"
                updateUiState()
            }
        }
    }

    fun connectToController(controller: NetworkClient.ControllerInfo) {
        if (_connectionState.value == ConnectionState.CONNECTING) {
            return // Already connecting
        }

        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value = "Connecting to ${controller.deviceName}..."
        updateUiState()

        viewModelScope.launch {
            try {
                val success = networkClient.connectToController(controller.ipAddress, controller.port)
                if (success) {
                    _connectedController.value = controller
                    _connectionState.value = ConnectionState.CONNECTED
                    _statusMessage.value = "Connected to ${controller.deviceName}"
                    
                    _pairingEvents.emit(PairingEvent.ControllerConnected(controller))
                } else {
                    _connectionState.value = ConnectionState.CONNECTION_FAILED
                    _statusMessage.value = "Failed to connect to ${controller.deviceName}"
                    _error.value = "Connection failed"
                }
                updateUiState()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.CONNECTION_FAILED
                _statusMessage.value = "Connection error: ${e.message}"
                _error.value = "Connection error: ${e.message}"
                updateUiState()
            }
        }
    }

    fun disconnectFromController() {
        viewModelScope.launch {
            try {
                networkClient.disconnect()
                _connectedController.value = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _statusMessage.value = "Disconnected from controller"
                updateUiState()
            } catch (e: Exception) {
                _error.value = "Disconnect error: ${e.message}"
            }
        }
    }

    fun startSession(sessionInfo: SessionInfo) {
        viewModelScope.launch {
            try {
                // For now, just trigger navigation event
                _navigationEvent.value = NavigationEvent(
                    action = "START_RECORDING",
                    sessionInfo = sessionInfo
                )
                _pairingEvents.emit(PairingEvent.NavigateToSession(sessionInfo))
            } catch (e: Exception) {
                _error.value = "Failed to start session: ${e.message}"
            }
        }
    }

    private fun updateUiState() {
        val scanState = _scanState.value ?: ScanState.IDLE
        val connectionState = _connectionState.value ?: ConnectionState.DISCONNECTED
        val statusMessage = _statusMessage.value ?: ""
        val deviceCount = controllers.size

        _pairingUiState.value = PairingUiState(
            isScanning = scanState == ScanState.SCANNING,
            isConnecting = connectionState == ConnectionState.CONNECTING,
            isLoading = scanState == ScanState.SCANNING || connectionState == ConnectionState.CONNECTING,
            statusMessage = statusMessage,
            deviceCount = deviceCount
        )
    }

    // NetworkClient.NetworkEventListener implementation
    override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
        viewModelScope.launch {
            if (!controllers.contains(controller)) {
                controllers.add(controller)
                _discoveredControllers.value = controllers.toList()
                _availableControllers.value = controllers.toList()
                updateUiState()
            }
        }
    }

    override fun onConnected(controller: NetworkClient.ControllerInfo) {
        _connectedController.value = controller
        _connectionState.value = ConnectionState.CONNECTED
        _statusMessage.value = "Connected to ${controller.deviceName}"
        updateUiState()
    }

    override fun onDisconnected(reason: String) {
        _connectedController.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _statusMessage.value = "Controller disconnected: $reason"
        updateUiState()
    }

    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        _navigationEvent.value = NavigationEvent(
            action = "RECORDING_STARTED",
            sessionInfo = sessionInfo
        )
    }

    override fun onSyncFlash(durationMs: Int) {
        _statusMessage.value = "Sync flash triggered (${durationMs}ms)"
    }

    override fun onTimeSynchronized(offsetNanoseconds: Long) {
        _statusMessage.value = "Time synchronized (offset: ${offsetNanoseconds / 1_000_000}ms)"
    }

    override fun onDataStreamingStarted() {
        _statusMessage.value = "Data streaming started"
    }

    override fun onDataStreamingStopped() {
        _statusMessage.value = "Data streaming stopped"
    }

    override fun onError(operation: String, error: String) {
        _connectionState.value = ConnectionState.CONNECTION_FAILED
        _error.value = "Error in $operation: $error"
        updateUiState()
    }

    fun clearPairingError() {
        _error.value = null
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun canStartScan(): Boolean {
        return _scanState.value != ScanState.SCANNING
    }

    fun canConnect(): Boolean {
        return _connectionState.value != ConnectionState.CONNECTING
    }

    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                if (::networkClient.isInitialized) {
                    networkClient.disconnect()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val TAG = "DevicePairingViewModel"
    }
}
