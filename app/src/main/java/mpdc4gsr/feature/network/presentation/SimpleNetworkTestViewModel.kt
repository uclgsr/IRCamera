package mpdc4gsr.feature.network.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.feature.network.domain.usecase.ConnectToControllerUseCase
import mpdc4gsr.feature.network.domain.usecase.DisconnectUseCase
import mpdc4gsr.feature.network.domain.usecase.ObserveConnectionStateUseCase
import javax.inject.Inject

@HiltViewModel
class SimpleNetworkTestViewModel @Inject constructor(
    private val connectToControllerUseCase: ConnectToControllerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    observeConnectionStateUseCase: ObserveConnectionStateUseCase
) : ViewModel() {

    private val _testResult = MutableStateFlow("")
    val testResult: StateFlow<String> = _testResult.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    val connectionState = observeConnectionStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mpdc4gsr.feature.network.domain.model.ConnectionState.Disconnected
        )

    fun runConnectionTest(ipAddress: String, port: Int) {
        viewModelScope.launch {
            _isRunning.value = true
            _testResult.value = "Testing connection to $ipAddress:$port..."

            connectToControllerUseCase(ipAddress, port, true)
                .onSuccess {
                    _testResult.value = "Connection successful!"
                }
                .onFailure { error ->
                    _testResult.value = "Connection failed: ${error.message}"
                }

            _isRunning.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectUseCase()
        }
    }
}
