package mpdc4gsr.feature.capture.thermal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import mpdc4gsr.feature.capture.thermal.data.ThermalStatus
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalCoreUseCases
import javax.inject.Inject

@HiltViewModel
class ThermalCameraViewModel
    @Inject
    constructor(
        private val thermalCoordinator: ThermalCaptureCoordinator,
        private val thermalUseCases: ThermalCoreUseCases,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ThermalUiState>(ThermalUiState.Loading)
        val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()

        private var latestStatus: ThermalStatus = ThermalStatus()
        private var latestPreview: ThermalFrameData? = null

        init {
            observeStatus()
            observePreview()
            observeErrors()
            viewModelScope.launch {
                thermalCoordinator.ensureConnected()
            }
        }

        fun onEvent(event: ThermalUiEvent) {
            when (event) {
                ThermalUiEvent.ConnectCamera -> connect()
                ThermalUiEvent.DisconnectCamera -> disconnect()
                ThermalUiEvent.StartRecording -> startRecording()
                ThermalUiEvent.StopRecording -> stopRecording()
                ThermalUiEvent.CaptureSnapshot -> captureSnapshot()
                ThermalUiEvent.PauseRecording -> pauseRecording()
                ThermalUiEvent.ResumeRecording -> resumeRecording()
                ThermalUiEvent.ClearError -> clearError()
                is ThermalUiEvent.SetTemperatureRange -> setTemperatureRange(event.minTemp, event.maxTemp)
            }
        }

        private fun observeStatus() {
            viewModelScope.launch {
                thermalCoordinator
                    .statusFlow()
                    .collect { status ->
                        latestStatus = status
                        publishState(status, latestPreview)
                    }
            }
        }

        private fun observePreview() {
            viewModelScope.launch {
                thermalCoordinator
                    .previewFlow()
                    .collect { frame ->
                        latestPreview = frame
                        publishState(latestStatus, frame)
                    }
            }
        }

        private fun observeErrors() {
            viewModelScope.launch {
                thermalCoordinator
                    .errorFlow()
                    .collect { message ->
                        _uiState.value =
                            ThermalUiState.Error(
                                message = message,
                                isRecoverable = true,
                            )
                    }
            }
        }

        private fun connect() {
            viewModelScope.launch {
                val success = thermalCoordinator.ensureConnected()
                if (!success) {
                    _uiState.value =
                        ThermalUiState.Error(
                            message = "Unable to connect to the thermal camera.",
                            isRecoverable = true,
                        )
                }
            }
        }

        private fun disconnect() {
            viewModelScope.launch {
                thermalCoordinator.disconnect()
            }
        }

        private fun startRecording() {
            viewModelScope.launch {
                val success = thermalCoordinator.startManualRecording()
                if (!success) {
                    _uiState.value =
                        ThermalUiState.Error(
                            message = "Recording start failed. Check connections and try again.",
                            isRecoverable = true,
                        )
                }
            }
        }

        private fun stopRecording() {
            viewModelScope.launch {
                thermalCoordinator.stopManualRecording()
            }
        }

        private fun captureSnapshot() {
            viewModelScope.launch {
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
            latestStatus = latestStatus.copy(lastError = null)
            publishState(latestStatus, latestPreview)
        }

        private fun setTemperatureRange(
            minTemp: Float,
            maxTemp: Float,
        ) {
            viewModelScope.launch {
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

        private fun publishState(
            status: ThermalStatus,
            frame: ThermalFrameData?,
        ) {
            if (!status.isConnected && status.lastError != null) {
                _uiState.value =
                    ThermalUiState.Error(
                        message = status.lastError,
                        isRecoverable = true,
                    )
                return
            }

            val minTemp = status.minTemperature ?: frame?.minTemp ?: 0f
            val maxTemp = status.maxTemperature ?: frame?.maxTemp ?: 0f
            val avgTemp =
                when {
                    status.minTemperature != null && status.maxTemperature != null ->
                        (status.minTemperature + status.maxTemperature) / 2f
                    frame != null ->
                        (frame.minTemp + frame.maxTemp) / 2f
                    else -> status.currentTemperature ?: 0f
                }

            val previousPaused =
                (_uiState.value as? ThermalUiState.Success)?.isPaused ?: false

            _uiState.value =
                ThermalUiState.Success(
                    isConnected = status.isConnected,
                    isRecording = status.isRecording,
                    currentTemperature = status.currentTemperature ?: frame?.centerTemp,
                    minTemperature = minTemp,
                    maxTemperature = maxTemp,
                    avgTemperature = avgTemp,
                    centerTemperature = status.currentTemperature ?: frame?.centerTemp ?: 0f,
                    isPaused = previousPaused,
                    recordingDuration = status.recordingDurationMs,
                    previewBitmap = frame?.bitmap,
                    isSimulationMode = status.isSimulation,
                    frameCount = status.frameCount,
                    lastRecordingPath = status.lastRecordingPath,
                )
        }
    }
