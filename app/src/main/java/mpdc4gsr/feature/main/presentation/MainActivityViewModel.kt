package mpdc4gsr.feature.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.domain.model.UiEvent
import mpdc4gsr.core.ui.model.CameraAction
import mpdc4gsr.core.ui.model.GSRAction
import mpdc4gsr.core.ui.model.ThermalAction
import mpdc4gsr.feature.main.domain.model.SensorStatus
import mpdc4gsr.feature.main.domain.model.SessionState
import mpdc4gsr.feature.main.domain.repository.GsrConnectionState
import mpdc4gsr.feature.main.domain.repository.GsrRepository
import mpdc4gsr.feature.main.domain.repository.NetworkConnectionState
import mpdc4gsr.feature.main.domain.repository.NetworkRepository
import mpdc4gsr.feature.main.domain.repository.SessionRepository
import mpdc4gsr.feature.main.domain.usecase.ConnectGsrSensorUseCase
import mpdc4gsr.feature.main.domain.usecase.StartNetworkDiscoveryUseCase
import mpdc4gsr.feature.main.domain.usecase.StartRecordingSessionUseCase
import mpdc4gsr.feature.main.domain.usecase.StopRecordingSessionUseCase
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel
@Inject
constructor(
    private val startRecordingSessionUseCase: StartRecordingSessionUseCase,
    private val stopRecordingSessionUseCase: StopRecordingSessionUseCase,
    private val connectGsrSensorUseCase: ConnectGsrSensorUseCase,
    private val startNetworkDiscoveryUseCase: StartNetworkDiscoveryUseCase,
    private val gsrRepository: GsrRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        observeGsrState()
        observeNetworkState()
        observeSessionState()
    }

    fun onAction(action: MainUiAction) {
        when (action) {
            is MainUiAction.SelectPage -> setPage(action.index)
            MainUiAction.BackPressed -> onBackPressed()
            MainUiAction.StartGsrConnection -> beginGsrConnection()
            is MainUiAction.PerformGsrAction -> handleGsrAction(action.action)
            MainUiAction.StartNetworkDiscovery -> beginNetworkDiscovery()
            is MainUiAction.PerformThermalAction -> handleThermalAction(action.action)
            is MainUiAction.PerformCameraAction -> handleCameraAction(action.action)
            is MainUiAction.StartRecording -> startRecordingSession(action.config)
            MainUiAction.StopRecording -> stopRecordingSession()
            is MainUiAction.SetRemoteTriggered -> setRemoteTriggered(action.isRemote)
            is MainUiAction.LockExposure -> setExposureLocked(action.locked)
            is MainUiAction.LockFocus -> setFocusLocked(action.locked)
            is MainUiAction.SetExposureCompensation -> setExposureCompensation(action.value)
            MainUiAction.ResetCameraControls -> resetCameraControls()
        }
    }

    private fun onBackPressed() {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowExitDialog)
        }
    }

    private fun setPage(index: Int) {
        val page = index.coerceIn(MainUiState.PAGE_GALLERY, MainUiState.PAGE_PROFILE)
        _uiState.update { it.copy(currentPage = page) }
    }

    private fun beginGsrConnection() {
        viewModelScope.launch {
            updateGsrSensorState(
                status = SensorStatus.CONNECTING,
                message = "Searching for GSR sensor...",
            )
            updateGsrConnectionState(GsrConnectionState.DISCOVERING)
            emitToast("Searching for GSR sensor...")

            val success = runCatching { connectGsrSensorUseCase() }.getOrElse { false }
            if (success) {
                updateGsrConnectionState(GsrConnectionState.CONNECTED)
                updateGsrSensorState(
                    status = SensorStatus.CONNECTED,
                    message = "GSR sensor connected",
                )
                emitToast("GSR sensor connected")
            } else {
                updateGsrConnectionState(GsrConnectionState.ERROR)
                updateGsrSensorState(
                    status = SensorStatus.ERROR,
                    message = "Failed to connect GSR sensor",
                )
                emitToast("Failed to connect GSR sensor", true)
            }
        }
    }

    private fun beginNetworkDiscovery() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(networkConnectionState = NetworkConnectionState.DISCOVERING)
            }
            emitToast("Searching for PC controllers...")

            val controllers = runCatching { startNetworkDiscoveryUseCase() }.getOrElse { emptyList() }
            if (controllers.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        networkConnectionState = NetworkConnectionState.CONNECTED,
                        connectedControllerInfo = controllers.first(),
                    )
                }
                emitToast("Connected to PC controller")
            } else {
                _uiState.update {
                    it.copy(
                        networkConnectionState = NetworkConnectionState.DISCONNECTED,
                        connectedControllerInfo = null,
                    )
                }
                emitToast("No PC controllers found")
            }
        }
    }

    private fun startRecordingSession(config: RecordingSessionConfig) {
        viewModelScope.launch {
            if (_uiState.value.sessionState == SessionState.RECORDING ||
                _uiState.value.sessionState == SessionState.STARTING
            ) {
                return@launch
            }
            _uiState.update { it.copy(sessionState = SessionState.STARTING) }
            emitToast("Starting recording session...")

            runCatching {
                startRecordingSessionUseCase(
                    sessionId = config.sessionId,
                    participantId = config.participantId,
                    studyName = config.studyName,
                    metadata = config.metadata,
                )
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        currentSession = session,
                        sessionState = SessionState.RECORDING,
                    )
                }
                emitToast("Recording session started: ${session.sessionId}")
            }.onFailure { throwable ->
                _uiState.update { it.copy(sessionState = SessionState.ERROR) }
                emitToast("Failed to start recording: ${throwable.message ?: "Unknown error"}", true)
            }
        }
    }

    private fun stopRecordingSession() {
        viewModelScope.launch {
            val sessionId =
                _uiState.value.currentSession?.sessionId ?: run {
                    emitToast("No active session to stop")
                    return@launch
                }
            _uiState.update { it.copy(sessionState = SessionState.STOPPING) }
            emitToast("Stopping recording session...")

            runCatching { stopRecordingSessionUseCase(sessionId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            currentSession = null,
                            sessionState = SessionState.IDLE,
                        )
                    }
                    emitToast("Recording session stopped")
                }.onFailure { throwable ->
                    _uiState.update { it.copy(sessionState = SessionState.ERROR) }
                    emitToast("Failed to stop session: ${throwable.message ?: "Unknown error"}", true)
                }
        }
    }

    private fun handleGsrAction(action: GSRAction) {
        when (action) {
            GSRAction.Connect -> beginGsrConnection()
            GSRAction.Disconnect ->
                viewModelScope.launch {
                    runCatching { gsrRepository.disconnect() }
                    updateGsrConnectionState(GsrConnectionState.DISCONNECTED)
                    updateGsrSensorState(
                        status = SensorStatus.DISCONNECTED,
                        message = "Disconnected",
                    )
                    emitToast("GSR sensor disconnected")
                }

            GSRAction.StartStream ->
                updateGsrSensorState(
                    status = SensorStatus.STREAMING,
                    message = "Streaming GSR data",
                    isRecording = true,
                )

            GSRAction.StopStream ->
                updateGsrSensorState(
                    status = SensorStatus.CONNECTED,
                    message = "Stream stopped",
                    isRecording = false,
                )

            is GSRAction.ConfigureDevice ->
                viewModelScope.launch {
                    emitToast("Device configuration is not available yet")
                }
        }
    }

    private fun handleThermalAction(action: ThermalAction) {
        when (action) {
            ThermalAction.Connect ->
                updateThermalSensorState(
                    status = SensorStatus.CONNECTING,
                    message = "Connecting thermal camera...",
                )

            ThermalAction.Disconnect ->
                updateThermalSensorState(
                    status = SensorStatus.DISCONNECTED,
                    message = "Thermal camera disconnected",
                )

            ThermalAction.StartPreview ->
                updateThermalSensorState(
                    status = SensorStatus.STREAMING,
                    message = "Thermal preview active",
                )

            ThermalAction.StopPreview ->
                updateThermalSensorState(
                    status = SensorStatus.CONNECTED,
                    message = "Thermal preview stopped",
                )

            ThermalAction.Calibrate -> emitToast("Thermal calibration started")
            ThermalAction.OpenSettings -> emitToast("Opening thermal settings")
        }
    }

    private fun handleCameraAction(action: CameraAction) {
        when (action) {
            CameraAction.Connect ->
                updateRgbSensorState(
                    status = SensorStatus.CONNECTING,
                    message = "Connecting RGB camera...",
                )

            CameraAction.Disconnect ->
                updateRgbSensorState(
                    status = SensorStatus.DISCONNECTED,
                    message = "RGB camera disconnected",
                )

            CameraAction.StartPreview ->
                updateRgbSensorState(
                    status = SensorStatus.STREAMING,
                    message = "RGB preview active",
                )

            CameraAction.StopPreview ->
                updateRgbSensorState(
                    status = SensorStatus.CONNECTED,
                    message = "RGB preview stopped",
                )

            is CameraAction.SetResolution ->
                emitToast(
                    "Resolution set to ${action.width}x${action.height}",
                )
        }
    }

    private fun setRemoteTriggered(isRemote: Boolean) {
        _uiState.update { it.copy(isRemoteTriggered = isRemote) }
    }

    private fun setExposureLocked(locked: Boolean) {
        _uiState.update { it.copy(exposureLocked = locked) }
        emitToast(if (locked) "Exposure locked" else "Exposure auto mode enabled")
    }

    private fun setFocusLocked(locked: Boolean) {
        _uiState.update { it.copy(focusLocked = locked) }
        emitToast(if (locked) "Focus locked" else "Focus auto mode enabled")
    }

    private fun setExposureCompensation(value: Float) {
        _uiState.update { it.copy(exposureCompensation = value) }
        val prefix = if (value > 0f) "+" else ""
        emitToast("Exposure compensation: $prefix$value EV")
    }

    private fun resetCameraControls() {
        _uiState.update {
            it.copy(
                exposureLocked = false,
                focusLocked = false,
                exposureCompensation = 0f,
            )
        }
        emitToast("Camera controls reset to auto")
    }

    private fun observeGsrState() {
        collectInViewModelScope {
            launch {
                gsrRepository
                    .getConnectionState()
                    .distinctUntilChanged()
                    .collect { state ->
                        updateGsrConnectionState(state)
                        val message =
                            when (state) {
                                GsrConnectionState.DISCONNECTED -> "GSR sensor disconnected"
                                GsrConnectionState.DISCOVERING -> "Discovering GSR sensors"
                                GsrConnectionState.CONNECTING -> "Connecting to GSR sensor"
                                GsrConnectionState.CONNECTED -> "GSR sensor ready"
                                GsrConnectionState.ERROR -> "GSR sensor error"
                            }
                        updateGsrSensorState(
                            status = mapGsrConnectionState(state),
                            message = message,
                        )
                    }
            }
            launch {
                gsrRepository
                    .getBatteryLevel()
                    .collect { level ->
                        _uiState.update {
                            it.copy(
                                gsrBatteryLevel = level,
                                gsrDataState =
                                    it.gsrDataState.copy(
                                        batteryLevel = level ?: it.gsrDataState.batteryLevel,
                                    ),
                            )
                        }
                    }
            }
            launch {
                gsrRepository
                    .getDeviceStatus()
                    .collect { status ->
                        updateGsrSensorState(
                            status = _uiState.value.sensorOverview.gsr.status,
                            message = status,
                        )
                    }
            }
            launch {
                gsrRepository
                    .getConnectionQuality()
                    .collect { quality ->
                        _uiState.update {
                            val history = (it.gsrDataState.recentReadings + quality).takeLast(20)
                            it.copy(
                                gsrDataState =
                                    it.gsrDataState.copy(
                                        averageValue = quality,
                                        recentReadings = history,
                                    ),
                            )
                        }
                    }
            }
        }
    }

    private fun observeNetworkState() {
        collectInViewModelScope {
            launch {
                networkRepository
                    .getConnectionState()
                    .distinctUntilChanged()
                    .collect { state ->
                        _uiState.update { it.copy(networkConnectionState = state) }
                    }
            }
            launch {
                networkRepository
                    .getConnectedController()
                    .collect { controller ->
                        _uiState.update { it.copy(connectedControllerInfo = controller) }
                    }
            }
        }
    }

    private fun observeSessionState() {
        collectInViewModelScope {
            launch {
                sessionRepository
                    .getCurrentSession()
                    .collect { session ->
                        _uiState.update {
                            it.copy(
                                currentSession = session,
                                sessionState =
                                    when {
                                        session != null -> SessionState.RECORDING
                                        else -> SessionState.IDLE
                                    },
                            )
                        }
                    }
            }
        }
    }

    private fun updateGsrConnectionState(state: GsrConnectionState) {
        _uiState.update { it.copy(gsrConnectionState = state) }
    }

    private fun updateGsrSensorState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean? = null,
    ) {
        updateSensorOverview { overview ->
            overview.copy(
                gsr =
                    overview.gsr.copy(
                        status = status,
                        message = message ?: overview.gsr.message,
                        isRecording = isRecording ?: overview.gsr.isRecording,
                        lastUpdate = System.currentTimeMillis(),
                    ),
            )
        }
    }

    private fun updateThermalSensorState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean? = null,
    ) {
        updateSensorOverview { overview ->
            overview.copy(
                thermal =
                    overview.thermal.copy(
                        status = status,
                        message = message ?: overview.thermal.message,
                        isRecording = isRecording ?: overview.thermal.isRecording,
                        lastUpdate = System.currentTimeMillis(),
                    ),
            )
        }
    }

    private fun updateRgbSensorState(
        status: SensorStatus,
        message: String? = null,
        isRecording: Boolean? = null,
    ) {
        updateSensorOverview { overview ->
            overview.copy(
                rgb =
                    overview.rgb.copy(
                        status = status,
                        message = message ?: overview.rgb.message,
                        isRecording = isRecording ?: overview.rgb.isRecording,
                        lastUpdate = System.currentTimeMillis(),
                    ),
            )
        }
    }

    private fun updateSensorOverview(transform: (SensorOverviewState) -> SensorOverviewState) {
        _uiState.update { current ->
            current.copy(sensorOverview = transform(current.sensorOverview))
        }
    }

    private fun mapGsrConnectionState(state: GsrConnectionState): SensorStatus =
        when (state) {
            GsrConnectionState.DISCONNECTED -> SensorStatus.DISCONNECTED
            GsrConnectionState.DISCOVERING, GsrConnectionState.CONNECTING -> SensorStatus.CONNECTING
            GsrConnectionState.CONNECTED -> SensorStatus.CONNECTED
            GsrConnectionState.ERROR -> SensorStatus.ERROR
        }

    private fun emitToast(
        message: String,
        isLong: Boolean = false,
    ) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowToast(message, isLong))
        }
    }

    private fun collectInViewModelScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}
