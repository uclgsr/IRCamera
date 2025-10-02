package mpdc4gsr.feature.settings.data

import android.content.Context
import android.util.Log

object RecordingSettingsValidator {
    private const val TAG = "RecordingSettingsValidator"

    fun validateAndLogSettings(context: Context) {
        val repository = RecordingSettingsRepository.getInstance(context)
        val settings = repository.getSettings()

        Log.i(TAG, "========== Recording Settings Validation ==========")
        Log.i(TAG, "Auto Recording: ${settings.autoRecording}")
        Log.i(TAG, "Recording Quality: ${settings.recordingQuality}")
        Log.i(TAG, "Video Frame Rate: ${settings.videoFrameRate} fps")
        Log.i(TAG, "Audio Enabled: ${settings.audioEnabled}")
        Log.i(TAG, "Simultaneous Recording: ${settings.simultaneousRecording}")
        Log.i(TAG, "Timestamp Sync: ${settings.timestampSync}")
        Log.i(TAG, "Video Format: ${settings.videoFormat}")
        Log.i(TAG, "Audio Format: ${settings.audioFormat}")
        Log.i(TAG, "Sensor Data Format: ${settings.sensorDataFormat}")

        val qualityConfig = repository.getQualityConfig(settings.recordingQuality)
        Log.i(TAG, "Quality Config - Resolution: ${qualityConfig.videoWidth}x${qualityConfig.videoHeight}")
        Log.i(TAG, "Quality Config - Bitrate: ${qualityConfig.videoBitrate}")
        Log.i(TAG, "Quality Config - Preferred FPS: ${qualityConfig.preferredFps}")
        Log.i(TAG, "==================================================")
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

        Log.i(TAG, "========== Settings Application Verification ==========")
        Log.i(TAG, "Audio Setting Match: $audioMatch (Expected: ${settings.audioEnabled}, Actual: $actualAudioEnabled)")
        Log.i(
            TAG,
            "Resolution Match: $resolutionMatch (Expected: ${qualityConfig.videoWidth}x${qualityConfig.videoHeight}, Actual: ${actualVideoWidth}x${actualVideoHeight})"
        )
        Log.i(TAG, "FPS Match: $fpsMatch (Expected: ${settings.videoFrameRate}, Actual: $actualVideoFps)")
        Log.i(TAG, "Overall Settings Applied: ${audioMatch && resolutionMatch && fpsMatch}")
        Log.i(TAG, "======================================================")

        return audioMatch && resolutionMatch && fpsMatch
    }
}
