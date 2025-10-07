// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\settings\data' subtree
// Files: 2; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\settings\data\RecordingSettingsRepository.kt =====

package mpdc4gsr.feature.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordingSettingsRepository(context: Context) {
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

        @Volatile
        private var instance: RecordingSettingsRepository? = null
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


// ===== app\src\main\java\mpdc4gsr\feature\settings\data\RecordingSettingsValidator.kt =====

package mpdc4gsr.feature.settings.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler

object RecordingSettingsValidator {
    private const val TAG = "RecordingSettingsValidator"
    fun validateAndLogSettings(context: Context) {
        val repository = RecordingSettingsRepository.getInstance(context)
        val settings = repository.getSettings()
        AppLogger.i(TAG, "========== Recording Settings Validation ==========")
        AppLogger.i(TAG, "Auto Recording: ${settings.autoRecording}")
        AppLogger.i(TAG, "Recording Quality: ${settings.recordingQuality}")
        AppLogger.i(TAG, "Video Frame Rate: ${settings.videoFrameRate} fps")
        AppLogger.i(TAG, "Audio Enabled: ${settings.audioEnabled}")
        AppLogger.i(TAG, "Simultaneous Recording: ${settings.simultaneousRecording}")
        AppLogger.i(TAG, "Timestamp Sync: ${settings.timestampSync}")
        AppLogger.i(TAG, "Video Format: ${settings.videoFormat}")
        AppLogger.i(TAG, "Audio Format: ${settings.audioFormat}")
        AppLogger.i(TAG, "Sensor Data Format: ${settings.sensorDataFormat}")
        val qualityConfig = repository.getQualityConfig(settings.recordingQuality)
        AppLogger.i(TAG, "Quality Config - Resolution: ${qualityConfig.videoWidth}x${qualityConfig.videoHeight}")
        AppLogger.i(TAG, "Quality Config - Bitrate: ${qualityConfig.videoBitrate}")
        AppLogger.i(TAG, "Quality Config - Preferred FPS: ${qualityConfig.preferredFps}")
        AppLogger.i(TAG, "==================================================")
    }

    fun verifySettingsApplied(
        context: Context,
        actualAudioEnabled: Boolean,
        actualVideoWidth: Int,
        actualVideoHeight: Int,
        actualVideoFps: Int
    ): Boolean {
        val repository = RecordingSettingsRepository.getInstance(context)
        val settings = repository.getSettings()
        val qualityConfig = repository.getQualityConfig(settings.recordingQuality)
        val audioMatch = actualAudioEnabled == settings.audioEnabled
        val resolutionMatch = actualVideoWidth == qualityConfig.videoWidth &&
                actualVideoHeight == qualityConfig.videoHeight
        val fpsMatch = actualVideoFps >= (settings.videoFrameRate - 5) &&
                actualVideoFps <= (settings.videoFrameRate + 5)
        AppLogger.i(TAG, "========== Settings Application Verification ==========")
        AppLogger.i(
            TAG,
            "Audio Setting Match: $audioMatch (Expected: ${settings.audioEnabled}, Actual: $actualAudioEnabled)"
        )
        AppLogger.i(
            TAG,
            "Resolution Match: $resolutionMatch (Expected: ${qualityConfig.videoWidth}x${qualityConfig.videoHeight}, Actual: ${actualVideoWidth}x${actualVideoHeight})"
        )
        AppLogger.i(TAG, "FPS Match: $fpsMatch (Expected: ${settings.videoFrameRate}, Actual: $actualVideoFps)")
        AppLogger.i(TAG, "Overall Settings Applied: ${audioMatch && resolutionMatch && fpsMatch}")
        AppLogger.i(TAG, "======================================================")
        return audioMatch && resolutionMatch && fpsMatch
    }
}


