package mpdc4gsr.presentation.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.feature.network.domain.model.ControllerInfo
import mpdc4gsr.feature.network.domain.usecase.ConnectToControllerUseCase
import mpdc4gsr.feature.network.domain.usecase.DiscoverControllersUseCase
import mpdc4gsr.feature.network.domain.usecase.DisconnectUseCase
import mpdc4gsr.feature.network.domain.usecase.ObserveConnectionStateUseCase
import javax.inject.Inject

@HiltViewModel
class DevicePairingViewModel @Inject constructor(
    private val discoverControllersUseCase: DiscoverControllersUseCase,
    private val connectToControllerUseCase: ConnectToControllerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    observeConnectionStateUseCase: ObserveConnectionStateUseCase
) : ViewModel() {

    private val _discoveredControllers = MutableStateFlow<List<ControllerInfo>>(emptyList())
    val discoveredControllers: StateFlow<List<ControllerInfo>> = _discoveredControllers.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _events = MutableSharedFlow<PairingEvent>()
    val events: SharedFlow<PairingEvent> = _events.asSharedFlow()

    val connectionState = observeConnectionStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mpdc4gsr.feature.network.domain.model.ConnectionState.Disconnected
        )

    enum class ScanState {
        IDLE,
        SCANNING,
        COMPLETED,
        FAILED
    }

    sealed class PairingEvent {
        data class ShowError(val message: String) : PairingEvent()
        data class ShowSuccess(val message: String) : PairingEvent()
        data class NavigateToController(val controller: ControllerInfo) : PairingEvent()
    }

    fun startDiscovery() {
        viewModelScope.launch {
            _scanState.value = ScanState.SCANNING
            _statusMessage.value = "Scanning for PC Controllers..."

            discoverControllersUseCase()
                .onSuccess { controllers ->
                    _discoveredControllers.value = controllers
                    _scanState.value = ScanState.COMPLETED
                    _statusMessage.value = "Found ${controllers.size} controller(s)"
                }
                .onFailure { error ->
                    _scanState.value = ScanState.FAILED
                    _statusMessage.value = "Discovery failed: ${error.message}"
                    _events.emit(PairingEvent.ShowError("Failed to discover controllers"))
                }
        }
    }

    fun connectToController(controller: ControllerInfo, useSecure: Boolean = true) {
        viewModelScope.launch {
            _statusMessage.value = "Connecting to ${controller.deviceName}..."

            connectToControllerUseCase(controller.ipAddress, controller.port, useSecure)
                .onSuccess {
                    _statusMessage.value = "Connected to ${controller.deviceName}"
                    _events.emit(PairingEvent.ShowSuccess("Connection successful"))
                    _events.emit(PairingEvent.NavigateToController(controller))
                }
                .onFailure { error ->
                    _statusMessage.value = "Connection failed"
                    _events.emit(PairingEvent.ShowError("Failed to connect: ${error.message}"))
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectUseCase()
                .onSuccess {
                    _statusMessage.value = "Disconnected"
                }
                .onFailure { error ->
                    _events.emit(PairingEvent.ShowError("Failed to disconnect: ${error.message}"))
                }
        }
    }
}
