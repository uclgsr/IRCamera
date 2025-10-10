package mpdc4gsr.feature.control.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<RecordingSettings> = _settings.asStateFlow()

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key !in PREFERENCE_KEYS) return@OnSharedPreferenceChangeListener
            _settings.value = loadSettings()
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    companion object {
        private const val KEY_AUTO_RECORDING = "recording_auto_recording"
        private const val KEY_RECORDING_QUALITY = "recording_quality"
        private const val KEY_VIDEO_FRAME_RATE = "recording_video_frame_rate"
        private const val KEY_AUDIO_ENABLED = "recording_audio_enabled"
        private const val KEY_SIMULTANEOUS = "recording_simultaneous"
        private const val KEY_TIMESTAMP_SYNC = "recording_timestamp_sync"
        private val PREFERENCE_KEYS = setOf(
            KEY_AUTO_RECORDING,
            KEY_RECORDING_QUALITY,
            KEY_VIDEO_FRAME_RATE,
            KEY_AUDIO_ENABLED,
            KEY_SIMULTANEOUS,
            KEY_TIMESTAMP_SYNC
        )

        @Deprecated(
            message = "Use dependency injection instead",
            replaceWith = ReplaceWith("Inject RecordingSettingsRepository")
        )
        @Volatile
        private var instance: RecordingSettingsRepository? = null

        @Deprecated(
            message = "Use dependency injection instead",
            replaceWith = ReplaceWith("Inject RecordingSettingsRepository")
        )
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
            recordingQuality = RecordingQuality.fromPreference(
                prefs.getString(KEY_RECORDING_QUALITY, null)
            ),
            videoFrameRate = prefs.getInt(KEY_VIDEO_FRAME_RATE, 30),
            audioEnabled = prefs.getBoolean(KEY_AUDIO_ENABLED, true),
            simultaneousRecording = prefs.getBoolean(KEY_SIMULTANEOUS, true),
            timestampSync = prefs.getBoolean(KEY_TIMESTAMP_SYNC, true)
        )
    }

    fun getSettings(): RecordingSettings = _settings.value

    fun updateAutoRecording(enabled: Boolean) = updatePreference(
        persist = { putBoolean(KEY_AUTO_RECORDING, enabled) },
        mutate = { copy(autoRecording = enabled) }
    )

    fun updateRecordingQuality(quality: RecordingQuality) = updatePreference(
        persist = { putString(KEY_RECORDING_QUALITY, quality.preferenceValue) },
        mutate = { copy(recordingQuality = quality) }
    )

    fun updateVideoFrameRate(frameRate: Int) = updatePreference(
        persist = { putInt(KEY_VIDEO_FRAME_RATE, frameRate) },
        mutate = { copy(videoFrameRate = frameRate) }
    )

    fun updateAudioEnabled(enabled: Boolean) = updatePreference(
        persist = { putBoolean(KEY_AUDIO_ENABLED, enabled) },
        mutate = { copy(audioEnabled = enabled) }
    )

    fun updateSimultaneousRecording(enabled: Boolean) = updatePreference(
        persist = { putBoolean(KEY_SIMULTANEOUS, enabled) },
        mutate = { copy(simultaneousRecording = enabled) }
    )

    fun updateTimestampSync(enabled: Boolean) = updatePreference(
        persist = { putBoolean(KEY_TIMESTAMP_SYNC, enabled) },
        mutate = { copy(timestampSync = enabled) }
    )

    fun getQualityConfig(quality: RecordingQuality): QualityConfig {
        return when (quality) {
            RecordingQuality.ULTRA -> QualityConfig(
                videoBitrate = 50_000_000,
                videoWidth = 3840,
                videoHeight = 2160,
                preferredFps = 60
            )

            RecordingQuality.HIGH -> QualityConfig(
                videoBitrate = 20_000_000,
                videoWidth = 1920,
                videoHeight = 1080,
                preferredFps = 30
            )

            RecordingQuality.MEDIUM -> QualityConfig(
                videoBitrate = 10_000_000,
                videoWidth = 1280,
                videoHeight = 720,
                preferredFps = 30
            )

            RecordingQuality.LOW -> QualityConfig(
                videoBitrate = 5_000_000,
                videoWidth = 854,
                videoHeight = 480,
                preferredFps = 24
            )
        }
    }

    private inline fun updatePreference(
        crossinline persist: SharedPreferences.Editor.() -> Unit,
        crossinline mutate: RecordingSettings.() -> RecordingSettings
    ) {
        prefs.edit(commit = false) {
            persist()
        }
        _settings.update { current -> current.mutate() }
    }
}

