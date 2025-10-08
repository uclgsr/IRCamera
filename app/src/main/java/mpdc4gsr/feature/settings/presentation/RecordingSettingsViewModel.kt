package mpdc4gsr.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository
import javax.inject.Inject

@HiltViewModel
class RecordingSettingsViewModel @Inject constructor(
    private val repository: RecordingSettingsRepository
) : ViewModel() {
    private val _recordingSettings = MutableStateFlow(RecordingSettings())
    val recordingSettings: StateFlow<RecordingSettings> = _recordingSettings.asStateFlow()

    data class RecordingSettings(
        val autoRecording: Boolean = false,
        val recordingQuality: String = "High",
        val videoFrameRate: Int = 30,
        val audioEnabled: Boolean = true,
        val simultaneousRecording: Boolean = true,
        val timestampSync: Boolean = true,
        val videoFormat: String = "MP4 (H.264)",
        val audioFormat: String = "AAC",
        val sensorDataFormat: String = "CSV"
    )

    init {
        loadSettings()
        viewModelScope.launch {
            repository.settings.collect { repoSettings ->
                _recordingSettings.value = RecordingSettings(
                    autoRecording = repoSettings.autoRecording,
                    recordingQuality = repoSettings.recordingQuality,
                    videoFrameRate = repoSettings.videoFrameRate,
                    audioEnabled = repoSettings.audioEnabled,
                    simultaneousRecording = repoSettings.simultaneousRecording,
                    timestampSync = repoSettings.timestampSync,
                    videoFormat = repoSettings.videoFormat,
                    audioFormat = repoSettings.audioFormat,
                    sensorDataFormat = repoSettings.sensorDataFormat
                )
            }
        }
    }

    private fun loadSettings() {
        val settings = repository.getSettings()
        _recordingSettings.value = RecordingSettings(
            autoRecording = settings.autoRecording,
            recordingQuality = settings.recordingQuality,
            videoFrameRate = settings.videoFrameRate,
            audioEnabled = settings.audioEnabled,
            simultaneousRecording = settings.simultaneousRecording,
            timestampSync = settings.timestampSync,
            videoFormat = settings.videoFormat,
            audioFormat = settings.audioFormat,
            sensorDataFormat = settings.sensorDataFormat
        )
    }

    fun updateAutoRecording(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoRecording(enabled)
        }
    }

    fun updateRecordingQuality(quality: String) {
        viewModelScope.launch {
            repository.updateRecordingQuality(quality)
        }
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
}
