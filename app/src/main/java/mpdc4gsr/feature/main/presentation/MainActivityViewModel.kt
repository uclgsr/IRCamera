package mpdc4gsr.feature.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.feature.main.domain.model.*
import mpdc4gsr.feature.main.domain.repository.GSRConnectionState
import mpdc4gsr.feature.main.domain.repository.NetworkConnectionState
import mpdc4gsr.feature.main.domain.usecase.*
import mpdc4gsr.feature.network.data.NetworkClient
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val startRecordingSessionUseCase: StartRecordingSessionUseCase,
    private val stopRecordingSessionUseCase: StopRecordingSessionUseCase,
    private val connectGSRSensorUseCase: ConnectGSRSensorUseCase,
    private val startNetworkDiscoveryUseCase: StartNetworkDiscoveryUseCase
) : ViewModel() {

    // A single UiState class can be used to hold all UI-related state.
    // However, to align with the existing structure, individual flows are maintained.
    // For simplicity, a new state for the current navigation page is added.
    private val _currentPage =
        MutableStateFlow(1) // PAGE_MAIN = 1 (0: Gallery, 1: Main, 2: Settings, 3: Mine)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _gsrConnectionState = MutableStateFlow(GSRConnectionState.DISCONNECTED)
    val gsrConnectionState: StateFlow<GSRConnectionState> = _gsrConnectionState.asStateFlow()
    private val _gsrBatteryLevel = MutableStateFlow<Int?>(null)
    val gsrBatteryLevel: StateFlow<Int?> = _gsrBatteryLevel.asStateFlow()

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

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

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

    fun onNavigationItemSelected(index: Int) {
        _currentPage.value = index
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowExitDialog)
        }
    }

    fun startGSRConnection() {
        viewModelScope.launch {
            _gsrConnectionState.value = GSRConnectionState.DISCOVERING
            _events.emit(UiEvent.ShowToast("Searching for GSR sensor..."))
            
            val success = connectGSRSensorUseCase()
            if (success) {
                _gsrConnectionState.value = GSRConnectionState.CONNECTED
                _events.emit(UiEvent.ShowToast("GSR sensor connected"))
            } else {
                _gsrConnectionState.value = GSRConnectionState.ERROR
                _events.emit(UiEvent.ShowToast("Failed to connect GSR sensor", true))
            }
        }
    }

    fun startNetworkDiscovery() {
        viewModelScope.launch {
            _networkConnectionState.value = NetworkConnectionState.DISCOVERING
            _events.emit(UiEvent.ShowToast("Searching for PC controllers..."))
            
            val controllers = startNetworkDiscoveryUseCase()
            if (controllers.isNotEmpty()) {
                _connectedControllerInfo.value = controllers.first()
                _networkConnectionState.value = NetworkConnectionState.CONNECTED
                _events.emit(UiEvent.ShowToast("Connected to PC controller"))
            } else {
                _networkConnectionState.value = NetworkConnectionState.DISCONNECTED
                _events.emit(UiEvent.ShowToast("No PC controllers found"))
            }
        }
    }

    fun startRecordingSession(sessionConfig: SessionConfig = SessionConfig()) {
        viewModelScope.launch {
            if (_sessionState.value == SessionState.RECORDING) {
                return@launch
            }
            _sessionState.value = SessionState.STARTING
            _events.emit(UiEvent.ShowToast("Starting recording session..."))
            
            val session = startRecordingSessionUseCase(
                sessionId = sessionConfig.sessionId,
                participantId = sessionConfig.participantId,
                studyName = sessionConfig.studyName,
                metadata = sessionConfig.metadata
            )
            _currentSession.value = session
            _sessionState.value = SessionState.RECORDING
            _events.emit(UiEvent.ShowToast("Recording session started: ${session.sessionId}"))
        }
    }

    fun stopRecordingSession() {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                _sessionState.value = SessionState.STOPPING
                _events.emit(UiEvent.ShowToast("Stopping recording session..."))
                
                stopRecordingSessionUseCase(session.sessionId)
                _currentSession.value = null
                _sessionState.value = SessionState.IDLE
                _events.emit(UiEvent.ShowToast("Recording session stopped"))
            }
        }
    }

    // Sensor state management
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

}
