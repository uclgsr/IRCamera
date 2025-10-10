package mpdc4gsr.feature.capture.camera.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.view.TextureView

/**
 * High-level helper that bridges Camera2 configuration and feature toggles used across
 * Compose screens and diagnostics tooling.
 */
class Camera2System(
    private val context: Context,
    private val previewTextureView: TextureView? = null,
) {
    private val configurationManager = CameraConfigurationManager(context)
    private val rawEngine = RawEngine(context)

    fun isStage3ProcessingEnabled(): Boolean = rawEngine.isStage3ProcessingEnabled()

    fun configureStage3Processing(enabled: Boolean) {
        rawEngine.setStage3ProcessingEnabled(enabled)
        previewTextureView?.let {
            // App logic could update TextureView specific state here; for now we simply
            // request a re-draw so callers see immediate feedback.
            it.postInvalidate()
        }
    }

    fun getDeviceCapabilities(useFrontCamera: Boolean = false): CameraConfigurationManager.DeviceCapabilities =
        configurationManager.detectDeviceCapabilities(useFrontCamera)

    fun supportsFastSessionSwitching(): Boolean {
        val capabilities = configurationManager.detectDeviceCapabilities()
        val hardwareLevel = capabilities.hardwareLevel ?: return false
        return hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
    }

    fun release() {
        rawEngine.release()
    }
}

