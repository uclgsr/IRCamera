package mpdc4gsr.feature.network.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.feature.network.domain.model.ControllerInfo
import mpdc4gsr.feature.network.domain.usecase.ConnectToControllerUseCase
import mpdc4gsr.feature.network.domain.usecase.DisconnectUseCase
import mpdc4gsr.feature.network.domain.usecase.ObserveConnectionStateUseCase
import mpdc4gsr.feature.network.domain.usecase.SendMessageUseCase
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class NetworkClientTestViewModel @Inject constructor(
    private val connectToControllerUseCase: ConnectToControllerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    observeConnectionStateUseCase: ObserveConnectionStateUseCase
) : ViewModel() {

    private val _ipAddress = MutableStateFlow("192.168.1.100")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow("8080")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _connectionInfo = MutableStateFlow("")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val connectionState = observeConnectionStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mpdc4gsr.feature.network.domain.model.ConnectionState.Disconnected
        )

    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
    }

    fun updatePort(port: String) {
        _port.value = port
    }

    fun connect() {
        viewModelScope.launch {
            _statusMessage.value = "Connecting to ${_ipAddress.value}:${_port.value}..."
            
            val port = _port.value.toIntOrNull() ?: 8080
            connectToControllerUseCase(_ipAddress.value, port, true)
                .onSuccess {
                    _statusMessage.value = "Connected successfully"
                    _connectionInfo.value = "Connected to ${_ipAddress.value}:${port}"
                }
                .onFailure { error ->
                    _statusMessage.value = "Connection failed: ${error.message}"
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectUseCase()
                .onSuccess {
                    _statusMessage.value = "Disconnected"
                    _connectionInfo.value = ""
                }
                .onFailure { error ->
                    _statusMessage.value = "Disconnect failed: ${error.message}"
                }
        }
    }

    fun sendTestMessage(message: String) {
        viewModelScope.launch {
            val jsonMessage = JSONObject().apply {
                put("type", "test")
                put("message", message)
                put("timestamp", System.currentTimeMillis())
            }
            
            sendMessageUseCase(jsonMessage)
                .onSuccess {
                    _statusMessage.value = "Message sent successfully"
                }
                .onFailure { error ->
                    _statusMessage.value = "Send failed: ${error.message}"
                }
        }
    }
}
