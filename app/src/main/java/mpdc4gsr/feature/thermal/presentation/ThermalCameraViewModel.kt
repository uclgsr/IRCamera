package mpdc4gsr.feature.thermal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Thermal Camera functionality
 *
 * Manages thermal camera state following MVVM architecture
 */
class ThermalCameraViewModel : ViewModel() {

    data class ThermalCameraUiState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float = 0f,
        val maxTemperature: Float = 100f,
        val isPaused: Boolean = false,
        val recordingDuration: Long = 0L,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(ThermalCameraUiState())
    val uiState: StateFlow<ThermalCameraUiState> = _uiState.asStateFlow()

    fun connectToDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnected = true)
        }
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }
}
