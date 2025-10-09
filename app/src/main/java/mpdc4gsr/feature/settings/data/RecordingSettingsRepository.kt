package mpdc4gsr.feature.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<RecordingSettings> = _settings.asStateFlow()

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

        @Deprecated("Use dependency injection instead", ReplaceWith("Inject RecordingSettingsRepository"))
        @Volatile
        private var instance: RecordingSettingsRepository? = null

        @Deprecated("Use dependency injection instead", ReplaceWith("Inject RecordingSettingsRepository"))
        fun getInstance(context: Context): RecordingSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: RecordingSettingsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private fun loadSettings(): RecordingSettings {
        return RecordingSettings(
            autoRecording = prefs.getBoolean(KEY_AUTO_RECORDING, false),
            recordingQuality = prefs.getString(KEY_RECORDING_QUALITY, "High") ?: "High",
            videoFrameRate = prefs.getInt(KEY_VIDEO_FRAME_RATE, 30),
            audioEnabled = prefs.getBoolean(KEY_AUDIO_ENABLED, true),
            simultaneousRecording = prefs.getBoolean(KEY_SIMULTANEOUS, true),
            timestampSync = prefs.getBoolean(KEY_TIMESTAMP_SYNC, true)
        )
    }

    fun getSettings(): RecordingSettings {
        return _settings.value
    }

    fun updateAutoRecording(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECORDING, enabled).apply()
        _settings.value = _settings.value.copy(autoRecording = enabled)
    }

    fun updateRecordingQuality(quality: String) {
        prefs.edit().putString(KEY_RECORDING_QUALITY, quality).apply()
        _settings.value = _settings.value.copy(recordingQuality = quality)
    }

    fun updateVideoFrameRate(frameRate: Int) {
        prefs.edit().putInt(KEY_VIDEO_FRAME_RATE, frameRate).apply()
        _settings.value = _settings.value.copy(videoFrameRate = frameRate)
    }

    fun updateAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(audioEnabled = enabled)
    }

    fun updateSimultaneousRecording(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULTANEOUS, enabled).apply()
        _settings.value = _settings.value.copy(simultaneousRecording = enabled)
    }

    fun updateTimestampSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIMESTAMP_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(timestampSync = enabled)
    }

    fun getQualityConfig(quality: String): QualityConfig {
        return when (quality) {
            "Ultra" -> QualityConfig(
                videoBitrate = 50_000_000,
                videoWidth = 3840,
                videoHeight = 2160,
                preferredFps = 60
            )

            "High" -> QualityConfig(
                videoBitrate = 20_000_000,
                videoWidth = 1920,
                videoHeight = 1080,
                preferredFps = 30
            )

            "Medium" -> QualityConfig(
                videoBitrate = 10_000_000,
                videoWidth = 1280,
                videoHeight = 720,
                preferredFps = 30
            )

            "Low" -> QualityConfig(
                videoBitrate = 5_000_000,
                videoWidth = 854,
                videoHeight = 480,
                preferredFps = 24
            )

            else -> QualityConfig(
                videoBitrate = 20_000_000,
                videoWidth = 1920,
                videoHeight = 1080,
                preferredFps = 30
            )
        }
    }

    data class QualityConfig(
        val videoBitrate: Int,
        val videoWidth: Int,
        val videoHeight: Int,
        val preferredFps: Int
    )
}
