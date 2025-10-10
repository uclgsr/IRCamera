package mpdc4gsr.feature.capture.thermal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalCoreUseCases
import javax.inject.Inject

@HiltViewModel
class ThermalCameraViewModel
    @Inject
    constructor(
        private val thermalUseCases: ThermalCoreUseCases,
    ) : ViewModel() {
        private val exceptionHandler =
            CoroutineExceptionHandler { _, exception ->
                _uiState.value =
                    ThermalUiState.Error(
                        message = exception.message ?: "Unknown error occurred",
                        isRecoverable = true,
                    )
            }

        private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
        val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()
        private var recordingStartTime: Long = 0L

        init {
            onEvent(ThermalUiEvent.ConnectCamera)
        }

        fun onEvent(event: ThermalUiEvent) {
            when (event) {
                ThermalUiEvent.ConnectCamera -> connectToCamera()
                ThermalUiEvent.DisconnectCamera -> disconnectFromCamera()
                ThermalUiEvent.StartRecording -> startRecording()
                ThermalUiEvent.StopRecording -> stopRecording()
                ThermalUiEvent.CaptureSnapshot -> captureSnapshot()
                ThermalUiEvent.PauseRecording -> pauseRecording()
                ThermalUiEvent.ResumeRecording -> resumeRecording()
                ThermalUiEvent.ClearError -> clearError()
                is ThermalUiEvent.SetTemperatureRange -> setTemperatureRange(event.minTemp, event.maxTemp)
            }
        }

        private fun startThermalStream() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .startStreaming()
                    .catch { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Stream error: ${e.message}",
                                isRecoverable = true,
                            )
                    }.collect { frameData ->
                        val currentState = _uiState.value
                        if (currentState is ThermalUiState.Success) {
                            _uiState.value =
                                currentState.copy(
                                    minTemperature = frameData.minTemp,
                                    maxTemperature = frameData.maxTemp,
                                    avgTemperature = frameData.avgTemp,
                                    centerTemperature = frameData.centerTemp,
                                    currentTemperature = frameData.centerTemp,
                                    frameCount = currentState.frameCount + 1,
                                )
                        }
                    }
            }
        }

        private fun connectToCamera() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .connectCamera()
                    .onSuccess {
                        _uiState.value = ThermalUiState.Success(isConnected = true)
                        startThermalStream()
                    }.onFailure { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Connection failed: ${e.message}",
                                isRecoverable = true,
                            )
                    }
            }
        }

        private fun disconnectFromCamera() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases.stopStreaming()
                thermalUseCases.disconnectCamera()
                val currentState = _uiState.value
                if (currentState is ThermalUiState.Success) {
                    _uiState.value = currentState.copy(isConnected = false)
                }
            }
        }

        private fun startRecording() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .startRecording()
                    .onSuccess {
                        recordingStartTime = System.currentTimeMillis()
                        val currentState = _uiState.value
                        if (currentState is ThermalUiState.Success) {
                            _uiState.value =
                                currentState.copy(
                                    isRecording = true,
                                    recordingDuration = 0L,
                                )
                        }
                    }.onFailure { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Recording start failed: ${e.message}",
                                isRecoverable = true,
                            )
                    }
            }
        }

        private fun stopRecording() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .stopRecording()
                    .onSuccess {
                        val currentState = _uiState.value
                        if (currentState is ThermalUiState.Success) {
                            _uiState.value =
                                currentState.copy(
                                    isRecording = false,
                                    recordingDuration = 0L,
                                )
                        }
                    }.onFailure { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Recording stop failed: ${e.message}",
                                isRecoverable = true,
                            )
                    }
            }
        }

        private fun captureSnapshot() {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .captureSnapshot()
                    .onFailure { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Snapshot failed: ${e.message}",
                                isRecoverable = true,
                            )
                    }
            }
        }

        private fun pauseRecording() {
            val currentState = _uiState.value
            if (currentState is ThermalUiState.Success && currentState.isRecording) {
                _uiState.value = currentState.copy(isPaused = true)
            }
        }

        private fun resumeRecording() {
            val currentState = _uiState.value
            if (currentState is ThermalUiState.Success && currentState.isRecording) {
                _uiState.value = currentState.copy(isPaused = false)
            }
        }

        private fun clearError() {
            val currentState = _uiState.value
            if (currentState is ThermalUiState.Error && currentState.isRecoverable) {
                _uiState.value = ThermalUiState.Success()
            }
        }

        private fun setTemperatureRange(
            minTemp: Float,
            maxTemp: Float,
        ) {
            viewModelScope.launch(exceptionHandler) {
                thermalUseCases
                    .setTemperatureRange(minTemp, maxTemp)
                    .onFailure { e ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = "Failed to set temperature range: ${e.message}",
                                isRecoverable = true,
                            )
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
