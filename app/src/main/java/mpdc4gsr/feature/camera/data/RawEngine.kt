package mpdc4gsr.feature.camera.data

import android.content.Context

/**
 * Represents the RAW capture processing pipeline. For now the implementation focuses on
 * Stage 3 toggling and logging so higher layers can reason about the current state without
 * binding directly to low-level camera classes.
 */
class RawEngine(
    private val context: Context,
) {
    private var stage3Enabled: Boolean = SamsungDeviceCompatibility.isStage3Compatible()

    fun isStage3ProcessingEnabled(): Boolean = stage3Enabled

    fun setStage3ProcessingEnabled(enabled: Boolean) {
        stage3Enabled = enabled
    }

    fun describePipeline(): String {
        val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
        return "Stage3=$stage3Enabled, device=$deviceInfo"
    }

    fun release() {
    }

    companion object {
        private const val TAG = "RawEngine"
    }
}
