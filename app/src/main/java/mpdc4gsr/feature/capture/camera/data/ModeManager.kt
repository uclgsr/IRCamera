package mpdc4gsr.feature.capture.camera.data

import android.os.Build

/**
 * Simple state holder responsible for validating and tracking camera mode transitions.
 */
class ModeManager {
    enum class CameraMode {
        PREVIEW_ONLY,
        RAW_50MP,
        VIDEO_4K,
        VIDEO_1080P,
        PHOTO_BURST,
        NIGHT_MODE,
    }

    private var currentMode: CameraMode = CameraMode.PREVIEW_ONLY

    fun currentMode(): CameraMode = currentMode

    fun requestModeSwitch(target: CameraMode): Boolean {
        val supported = isModeSupported(target)
        if (supported) {
            currentMode = target
        }
        return supported
    }

    fun isModeSupported(mode: CameraMode): Boolean =
        when (mode) {
            CameraMode.PREVIEW_ONLY -> true
            CameraMode.RAW_50MP -> SamsungDeviceCompatibility.supportsRawCapture()
            CameraMode.VIDEO_4K -> SamsungDeviceCompatibility.supports4kVideo()
            CameraMode.VIDEO_1080P -> true
            CameraMode.PHOTO_BURST -> SamsungDeviceCompatibility.isSamsungDevice()
            CameraMode.NIGHT_MODE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        }
}
