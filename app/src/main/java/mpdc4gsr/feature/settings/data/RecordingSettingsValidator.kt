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
