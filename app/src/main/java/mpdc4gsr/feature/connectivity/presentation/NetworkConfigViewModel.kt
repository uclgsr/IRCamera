package mpdc4gsr.feature.connectivity.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.feature.connectivity.domain.usecase.DiscoverControllersUseCase
import javax.inject.Inject

@HiltViewModel
class NetworkConfigViewModel @Inject constructor(
    private val discoverControllersUseCase: DiscoverControllersUseCase
) : ViewModel() {

    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _connectionStatus = mutableStateOf("Disconnected")
    val connectionStatus: State<String> = _connectionStatus

    fun startDiscovery() {
        viewModelScope.launch {
            _isScanning.value = true
            _connectionStatus.value = "Scanning..."

            discoverControllersUseCase()
                .onSuccess { controllers ->
                    _connectionStatus.value = "Found ${controllers.size} device(s)"
                }
                .onFailure { error ->
                    _connectionStatus.value = "Discovery failed: ${error.message}"
                }
                .also {
                    delay(500)
                    _isScanning.value = false
                }
        }
    }
}

