package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder

/**
 * ViewModel for Thermal Camera functionality with full ThermalCameraRecorder integration
 *
 * Manages thermal camera state following MVVM architecture and connects
 * ThermalCameraRecorder to the UI for live preview and recording.
 */
class ThermalCameraViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ThermalCameraViewModel"
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

    private var thermalRecorder: ThermalCameraRecorder? = null
    private var recordingStartTime: Long = 0L

    init {
        initializeThermalRecorder()
    }

    private fun initializeThermalRecorder() {
        viewModelScope.launch {
            try {
                thermalRecorder = ThermalCameraRecorder(context, "thermal_preview_1")
                
                // Set preview callback to receive thermal frames
                thermalRecorder?.setThermalPreviewCallback(object : ThermalCameraRecorder.ThermalPreviewCallback {
                    override fun onThermalFrame(
                        bitmap: Bitmap?,
                        temperatureData: ThermalCameraRecorder.ThermalFrameData?
                    ) {
                        // Update UI state with new thermal frame and temperature data
                        // Use update() for thread-safe state updates from background thread
                        _uiState.update { currentState ->
                            currentState.copy(
                                previewBitmap = bitmap,
                                // Retain previous values if temperatureData is null
                                minTemperature = temperatureData?.minTemperature ?: currentState.minTemperature,
                                maxTemperature = temperatureData?.maxTemperature ?: currentState.maxTemperature,
                                avgTemperature = temperatureData?.avgTemperature ?: currentState.avgTemperature,
                                centerTemperature = temperatureData?.centerTemperature ?: currentState.centerTemperature,
                                currentTemperature = temperatureData?.centerTemperature ?: currentState.currentTemperature
                            )
                        }
                    }
                })

                // Initialize the thermal camera
                val success = thermalRecorder?.initialize() ?: false
                
                // Update connection status after initialization
                val status = thermalRecorder?.getThermalSystemStatus()
                _uiState.value = _uiState.value.copy(
                    isConnected = status?.isConnected ?: false,
                    isSimulationMode = status?.isSimulationMode ?: false
                )
                
                if (success) {
                    AppLogger.i(TAG, "Thermal camera initialized successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to initialize thermal camera"
                    )
                    AppLogger.e(TAG, "Failed to initialize thermal camera")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error initializing thermal recorder", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun connectToDevice() {
        viewModelScope.launch {
            val status = thermalRecorder?.getThermalSystemStatus()
            _uiState.value = _uiState.value.copy(
                isConnected = status?.isConnected ?: false,
                isSimulationMode = status?.isSimulationMode ?: false
            )
        }
    }

    fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata) {
        viewModelScope.launch {
            try {
                val success = thermalRecorder?.startRecording(sessionDirectory, sessionMetadata) ?: false
                if (success) {
                    recordingStartTime = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        isRecording = true,
                        recordingDuration = 0L
                    )
                    AppLogger.i(TAG, "Thermal recording started")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to start recording"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting recording", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Recording error: ${e.message}"
                )
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                thermalRecorder?.stopRecording()
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingDuration = 0L
                )
                AppLogger.i(TAG, "Thermal recording stopped")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping recording", e)
            }
        }
    }

    fun updateRecordingDuration() {
        if (_uiState.value.isRecording) {
            val duration = System.currentTimeMillis() - recordingStartTime
            _uiState.value = _uiState.value.copy(recordingDuration = duration)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Use runBlocking to ensure cleanup completes before the ViewModel is destroyed.
        // Launching a coroutine in viewModelScope from onCleared is unsafe as the scope is
        // cancelled immediately, which can interrupt critical cleanup tasks like saving data.
        runBlocking {
            try {
                // Call the comprehensive cleanup method to ensure all resources are released.
                thermalRecorder?.cleanup()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error cleaning up thermal recorder", e)
            }
        }
    }
}
