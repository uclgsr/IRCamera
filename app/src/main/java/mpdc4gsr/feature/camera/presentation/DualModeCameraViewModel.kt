package mpdc4gsr.feature.camera.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Dual Mode Camera functionality
 * 
 * Manages RGB and thermal camera synchronization following MVVM architecture
 */
class DualModeCameraViewModel : ViewModel() {
    
    data class DualModeCameraUiState(
        val isRgbCameraReady: Boolean = false,
        val isThermalCameraReady: Boolean = false,
        val isRecording: Boolean = false,
        val isSynced: Boolean = false,
        val recordingDuration: Long = 0L,
        val captureMode: CaptureMode = CaptureMode.DUAL,
        val errorMessage: String? = null
    )
    
    enum class CaptureMode {
        RGB_ONLY,
        THERMAL_ONLY,
        DUAL
    }
    
    private val _uiState = MutableStateFlow(DualModeCameraUiState())
    val uiState: StateFlow<DualModeCameraUiState> = _uiState.asStateFlow()
    
    fun initializeCameras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRgbCameraReady = true,
                isThermalCameraReady = true,
                isSynced = true
            )
        }
    }
    
    fun setCaptureMode(mode: CaptureMode) {
        _uiState.value = _uiState.value.copy(captureMode = mode)
    }
    
    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }
    
    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false, recordingDuration = 0L)
    }
    
    fun capturePhoto() {
        viewModelScope.launch {
            // TODO: Implement through repository
        }
    }
}
