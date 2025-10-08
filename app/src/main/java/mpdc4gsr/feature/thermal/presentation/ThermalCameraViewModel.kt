package mpdc4gsr.feature.thermal.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.feature.thermal.di.ThermalUseCases
import javax.inject.Inject

@HiltViewModel
class ThermalCameraViewModel @Inject constructor(
    private val thermalUseCases: ThermalUseCases
) : ViewModel() {

    companion object {
        private const val TAG = "ThermalCameraViewModel"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(errorMessage = "Error: ${exception.message}") }
    }

    data class ThermalCameraUiState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float = 0f,
        val maxTemperature: Float = 100f,
        val avgTemperature: Float = 0f,
        val centerTemperature: Float = 0f,
        val isPaused: Boolean = false,
        val recordingDuration: Long = 0L,
        val errorMessage: String? = null,
        val previewBitmap: Bitmap? = null,
        val isSimulationMode: Boolean = false,
        val frameCount: Long = 0L
    )

    private val _uiState = MutableStateFlow(ThermalCameraUiState())
    val uiState: StateFlow<ThermalCameraUiState> = _uiState.asStateFlow()
    private var recordingStartTime: Long = 0L

    init {
        connectToCamera()
        startThermalStream()
    }

    private fun startThermalStream() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.startStreaming()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Stream error: ${e.message}") }
                }
                .collect { frameData ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            minTemperature = frameData.minTemp,
                            maxTemperature = frameData.maxTemp,
                            avgTemperature = frameData.avgTemp,
                            centerTemperature = frameData.centerTemp,
                            currentTemperature = frameData.centerTemp,
                            frameCount = currentState.frameCount + 1
                        )
                    }
                }
        }
    }

    fun connectToCamera() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.connectCamera()
                .onSuccess {
                    _uiState.update { it.copy(isConnected = true, errorMessage = null) }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isConnected = false, 
                            errorMessage = "Connection failed: ${e.message}"
                        ) 
                    }
                }
        }
    }

    fun disconnectFromCamera() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.disconnectCamera()
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    fun startRecording() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.startRecording()
                .onSuccess {
                    recordingStartTime = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            isRecording = true,
                            recordingDuration = 0L,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Recording start failed: ${e.message}")
                    }
                }
        }
    }

    fun stopRecording() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.stopRecording()
                .onSuccess { filePath ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            recordingDuration = 0L,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Recording stop failed: ${e.message}")
                    }
                }
        }
    }

    fun captureSnapshot() {
        viewModelScope.launch(exceptionHandler) {
            thermalUseCases.captureSnapshot()
                .onSuccess { snapshot ->
                    // Handle successful snapshot capture
                    _uiState.update { it.copy(errorMessage = null) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Snapshot failed: ${e.message}")
                    }
                }
        }
    }

    fun updateRecordingDuration() {
        if (_uiState.value.isRecording) {
            val duration = System.currentTimeMillis() - recordingStartTime
            _uiState.update {
                it.copy(recordingDuration = duration)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            thermalUseCases.stopStreaming()
            thermalUseCases.disconnectCamera()
        }
    }
}
