package mpdc4gsr.feature.settings.data

import android.content.Context

object RecordingSettingsValidator {
    fun validateAndLogSettings(context: Context) {
        val repository = RecordingSettingsRepository.getInstance(context)
        val settings = repository.getSettings()
        val qualityConfig = repository.getQualityConfig(settings.recordingQuality)
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
            TAG,
            "Audio Setting Match: $audioMatch (Expected: ${settings.audioEnabled}, Actual: $actualAudioEnabled)"
        )
            TAG,
            "Resolution Match: $resolutionMatch (Expected: ${qualityConfig.videoWidth}x${qualityConfig.videoHeight}, Actual: ${actualVideoWidth}x${actualVideoHeight})"
        )
        return audioMatch && resolutionMatch && fpsMatch
    }
}
