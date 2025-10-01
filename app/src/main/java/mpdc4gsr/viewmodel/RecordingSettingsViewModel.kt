package mpdc4gsr.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.BaseViewModel

/**
 * Recording Settings ViewModel - MVVM Integration
 * Manages multi-modal recording configuration with SharedPreferences persistence
 */
class RecordingSettingsViewModel : BaseViewModel() {

    private lateinit var prefs: SharedPreferences

    // Recording Settings State
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

    companion object {
        private const val KEY_AUTO_RECORDING = "recording_auto_recording"
        private const val KEY_RECORDING_QUALITY = "recording_quality"
        private const val KEY_VIDEO_FRAME_RATE = "recording_video_frame_rate"
        private const val KEY_AUDIO_ENABLED = "recording_audio_enabled"
        private const val KEY_SIMULTANEOUS = "recording_simultaneous"
        private const val KEY_TIMESTAMP_SYNC = "recording_timestamp_sync"
    }

    fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        loadSettings()
    }

    private fun loadSettings() {
        _recordingSettings.value = RecordingSettings(
            autoRecording = prefs.getBoolean(KEY_AUTO_RECORDING, false),
            recordingQuality = prefs.getString(KEY_RECORDING_QUALITY, "High") ?: "High",
            videoFrameRate = prefs.getInt(KEY_VIDEO_FRAME_RATE, 30),
            audioEnabled = prefs.getBoolean(KEY_AUDIO_ENABLED, true),
            simultaneousRecording = prefs.getBoolean(KEY_SIMULTANEOUS, true),
            timestampSync = prefs.getBoolean(KEY_TIMESTAMP_SYNC, true)
        )
    }

    fun updateAutoRecording(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_RECORDING, enabled).apply()
            _recordingSettings.value = _recordingSettings.value.copy(autoRecording = enabled)
        }
    }

    fun updateRecordingQuality(quality: String) {
        viewModelScope.launch {
            prefs.edit().putString(KEY_RECORDING_QUALITY, quality).apply()
            _recordingSettings.value = _recordingSettings.value.copy(recordingQuality = quality)
        }
    }

    fun updateVideoFrameRate(frameRate: Int) {
        viewModelScope.launch {
            prefs.edit().putInt(KEY_VIDEO_FRAME_RATE, frameRate).apply()
            _recordingSettings.value = _recordingSettings.value.copy(videoFrameRate = frameRate)
        }
    }

    fun updateAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply()
            _recordingSettings.value = _recordingSettings.value.copy(audioEnabled = enabled)
        }
    }

    fun updateSimultaneousRecording(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_SIMULTANEOUS, enabled).apply()
            _recordingSettings.value = _recordingSettings.value.copy(simultaneousRecording = enabled)
        }
    }

    fun updateTimestampSync(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_TIMESTAMP_SYNC, enabled).apply()
            _recordingSettings.value = _recordingSettings.value.copy(timestampSync = enabled)
        }
    }
}
