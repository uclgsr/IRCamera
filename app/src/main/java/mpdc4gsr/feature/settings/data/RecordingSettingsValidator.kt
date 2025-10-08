package mpdc4gsr.feature.settings.data

import android.content.Context

object RecordingSettingsValidator {
    @Deprecated("This method no longer performs validation or logging", ReplaceWith(""))
    fun validateAndLogSettings(context: Context) {
        // No-op: Method retained for API compatibility only
        // Validation and logging have been removed per refactoring goals
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
        return audioMatch && resolutionMatch && fpsMatch
    }
}
