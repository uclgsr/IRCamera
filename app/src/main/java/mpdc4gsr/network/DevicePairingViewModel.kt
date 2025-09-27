package mpdc4gsr.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.launch

class DevicePairingViewModel : BaseViewModel(), NetworkClient.NetworkEventListener {

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

    private lateinit var networkClient: NetworkClient
    private val controllers = mutableListOf<NetworkClient.ControllerInfo>()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CONNECTION_FAILED
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

    fun initialize(context: NetworkClient.NetworkEventListener) {
        networkClient = NetworkClient(context)
        networkClient.setEventListener(this)
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _scanState.value = ScanState.IDLE
        _statusMessage.value = "Ready to scan for PC Controllers"
        _discoveredControllers.value = emptyList()
    }

    fun startControllerScan() {
        if (_scanState.value == ScanState.SCANNING) {
            return // Already scanning
        }

        _scanState.value = ScanState.SCANNING
        _statusMessage.value = "Scanning for PC Controllers..."
        controllers.clear()
        _discoveredControllers.value = emptyList()

        viewModelScope.launch {
            try {
                val foundControllers = networkClient.discoverControllers()
                controllers.addAll(foundControllers)
                _discoveredControllers.value = controllers.toList()

                _statusMessage.value = if (foundControllers.isNotEmpty()) {
                    "Found ${foundControllers.size} PC Controller(s)"
                } else {
                    "No PC Controllers found. Make sure you're on the same network."
                }
                
                _scanState.value = ScanState.COMPLETED
                
            } catch (e: Exception) {
                _statusMessage.value = "Scan failed: ${e.message}"
                _scanState.value = ScanState.FAILED
                _error.value = "Failed to scan for controllers: ${e.message}"
            }
        }
    }

    fun connectToController(controller: NetworkClient.ControllerInfo) {
        if (_connectionState.value == ConnectionState.CONNECTING) {
            return // Already connecting
        }

        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value = "Connecting to ${controller.deviceName}..."

        viewModelScope.launch {
            try {
                val success = networkClient.connectToController(controller.ipAddress, controller.port)
                if (success) {
                    _connectedController.value = controller
                    _connectionState.value = ConnectionState.CONNECTED
                    _statusMessage.value = "Connected to ${controller.deviceName}"
                } else {
                    _connectionState.value = ConnectionState.CONNECTION_FAILED
                    _statusMessage.value = "Failed to connect to ${controller.deviceName}"
                    _error.value = "Connection failed"
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.CONNECTION_FAILED
                _statusMessage.value = "Connection error: ${e.message}"
                _error.value = "Connection error: ${e.message}"
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
            } catch (e: Exception) {
                _error.value = "Disconnect error: ${e.message}"
            }
        }
    }

    fun startRecordingSession() {
        val controller = _connectedController.value
        if (controller == null) {
            _error.value = "No controller connected"
            return
        }

        viewModelScope.launch {
            try {
                val sessionInfo = networkClient.startRecordingSession()
                _navigationEvent.value = NavigationEvent(
                    action = "START_RECORDING",
                    sessionInfo = sessionInfo
                )
            } catch (e: Exception) {
                _error.value = "Failed to start recording session: ${e.message}"
            }
        }
    }

    // NetworkClient.NetworkEventListener implementation
    override fun onControllerConnected(controller: NetworkClient.ControllerInfo) {
        _connectedController.value = controller
        _connectionState.value = ConnectionState.CONNECTED
        _statusMessage.value = "Connected to ${controller.deviceName}"
    }

    override fun onControllerDisconnected() {
        _connectedController.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _statusMessage.value = "Controller disconnected"
    }

    override fun onConnectionError(error: String) {
        _connectionState.value = ConnectionState.CONNECTION_FAILED
        _error.value = "Connection error: $error"
    }

    override fun onRecordingSessionStarted(sessionInfo: SessionInfo) {
        _navigationEvent.value = NavigationEvent(
            action = "RECORDING_STARTED",
            sessionInfo = sessionInfo
        )
    }

    override fun onRecordingSessionStopped() {
        _statusMessage.value = "Recording session stopped"
    }

    override fun onDataReceived(data: String) {
        // Handle data received from controller
        _statusMessage.value = "Data received from controller"
    }

    fun clearError() {
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
                networkClient.disconnect()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val TAG = "DevicePairingViewModel"
    }
}