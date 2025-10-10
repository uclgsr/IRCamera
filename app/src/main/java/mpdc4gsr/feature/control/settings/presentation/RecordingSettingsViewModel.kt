package mpdc4gsr.feature.control.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.feature.capture.camera.data.CameraConfigurationManager
import mpdc4gsr.feature.control.settings.data.RecordingQuality
import mpdc4gsr.feature.control.settings.data.RecordingSettings
import mpdc4gsr.feature.control.settings.data.RecordingSettingsRepository
import javax.inject.Inject

private const val MIN_FRAME_RATE = 15
private const val DEFAULT_MAX_FRAME_RATE = 30

@HiltViewModel
class RecordingSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: RecordingSettingsRepository
) : ViewModel() {
    private val cameraConfigurationManager = CameraConfigurationManager(context)

    private val _uiState = MutableStateFlow(RecordingSettingsUiState())
    val uiState: StateFlow<RecordingSettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        refreshCameraCapabilities()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        isLoading = false
                    )
                }
                enforceFrameRateBounds(settings)
            }
        }
    }

    private fun refreshCameraCapabilities() {
        viewModelScope.launch {
            val capabilities = withContext(Dispatchers.Default) {
                cameraConfigurationManager.detectDeviceCapabilities()
            }
            val maxFrameRate = computeMaxFrameRate(capabilities)
            _uiState.update { current ->
                current.copy(
                    maxFrameRate = maxFrameRate,
                    isLoading = false
                )
            }
            enforceFrameRateBounds(_uiState.value.settings)
        }
    }

    private fun enforceFrameRateBounds(settings: RecordingSettings) {
        val maxFrameRate = _uiState.value.maxFrameRate.coerceAtLeast(DEFAULT_MAX_FRAME_RATE)
        val desired = settings.videoFrameRate
            .coerceAtLeast(MIN_FRAME_RATE)
            .coerceAtMost(maxFrameRate)
        if (desired != settings.videoFrameRate) {
            updateVideoFrameRate(desired)
        }
    }

    fun updateAutoRecording(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoRecording(enabled)
        }
    }

    fun updateRecordingQuality(quality: RecordingQuality) {
        viewModelScope.launch {
            repository.updateRecordingQuality(quality)
        }
    }

    fun updateRecordingQuality(displayName: String) {
        RecordingQuality.fromDisplayName(displayName)?.let { updateRecordingQuality(it) }
    }

    fun updateVideoFrameRate(frameRate: Int) {
        viewModelScope.launch {
            repository.updateVideoFrameRate(frameRate)
        }
    }

    fun updateAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudioEnabled(enabled)
        }
    }

    fun updateSimultaneousRecording(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSimultaneousRecording(enabled)
        }
    }

    fun updateTimestampSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateTimestampSync(enabled)
        }
    }

    private fun computeMaxFrameRate(
        capabilities: CameraConfigurationManager.DeviceCapabilities
    ): Int {
        val highestSupported = capabilities.supportedFpsRanges
            .maxOfOrNull { it.upper }
            ?: DEFAULT_MAX_FRAME_RATE
        return highestSupported.coerceAtLeast(DEFAULT_MAX_FRAME_RATE)
    }
}

data class RecordingSettingsUiState(
    val settings: RecordingSettings = RecordingSettings(),
    val maxFrameRate: Int = DEFAULT_MAX_FRAME_RATE,
    val isLoading: Boolean = true
) {
    val supports60Fps: Boolean get() = maxFrameRate >= 60
    val qualityOptions: List<RecordingQuality> get() = RecordingQuality.entries
    val frameRateRange: ClosedFloatingPointRange<Float>
        get() = MIN_FRAME_RATE.toFloat()..maxFrameRate.toFloat()
}

