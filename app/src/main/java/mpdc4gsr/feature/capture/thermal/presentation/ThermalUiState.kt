package mpdc4gsr.feature.capture.thermal.presentation

import android.graphics.Bitmap

sealed interface ThermalUiState {
    data object Loading : ThermalUiState

    data class Success(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float = 0f,
        val maxTemperature: Float = 100f,
        val avgTemperature: Float = 0f,
        val centerTemperature: Float = 0f,
        val isPaused: Boolean = false,
        val recordingDuration: Long = 0L,
        val previewBitmap: Bitmap? = null,
        val isSimulationMode: Boolean = false,
        val frameCount: Long = 0L,
    ) : ThermalUiState

    data class Error(
        val message: String,
        val isRecoverable: Boolean = true,
    ) : ThermalUiState
}

sealed interface ThermalUiEvent {
    data object ConnectCamera : ThermalUiEvent

    data object DisconnectCamera : ThermalUiEvent

    data object StartRecording : ThermalUiEvent

    data object StopRecording : ThermalUiEvent

    data object CaptureSnapshot : ThermalUiEvent

    data object PauseRecording : ThermalUiEvent

    data object ResumeRecording : ThermalUiEvent

    data object ClearError : ThermalUiEvent

    data class SetTemperatureRange(
        val minTemp: Float,
        val maxTemp: Float,
    ) : ThermalUiEvent
}
