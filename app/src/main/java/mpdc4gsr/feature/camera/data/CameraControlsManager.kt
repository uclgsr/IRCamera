package mpdc4gsr.feature.camera.data

import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.camera2.interop.Camera2CameraControl
import com.google.common.util.concurrent.ListenableFuture
import mpdc4gsr.core.sensors.api.ErrorType
import java.util.concurrent.Executor

/**
 * Centralises Camera2 specific manual control wiring so that recorder/view-model code
 * can stay focused on state management instead of low-level interop calls.
 */
class CameraControlsManager(
    private val onError: (ErrorType, String) -> Unit
) {

    fun setManualExposureMode(camera: Camera?, enabled: Boolean) {
        val camera2Control = resolveCameraControl(camera) ?: return
        try {
            val futures = if (enabled) {
                listOf(
                    camera2Control.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    ),
                    camera2Control.setCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_AUTO
                    ),
                    camera2Control.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        DEFAULT_EXPOSURE_TIME_NS
                    )
                )
            } else {
                listOf(
                    camera2Control.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    ),
                    camera2Control.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_LOCK,
                        false
                    )
                )
            }
            futures.forEach { future -> future.addListener({ }, DirectExecutor) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to toggle manual exposure", t)
            onError(
                ErrorType.OPERATION_FAILED,
                "Manual exposure is not available on this device: ${t.message}"
            )
        }
    }

    fun setFocusDistance(camera: Camera?, distance: Float) {
        val camera2Control = resolveCameraControl(camera) ?: return
        try {
            val clampedDistance = distance.coerceIn(0f, 1f)
            val futures = listOf(
                camera2Control.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                ),
                camera2Control.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    clampedDistance
                )
            )
            futures.forEach { future -> future.addListener({ }, DirectExecutor) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set manual focus distance", t)
            onError(
                ErrorType.OPERATION_FAILED,
                "Manual focus failed: ${t.message ?: "unknown error"}"
            )
        }
    }

    private fun resolveCameraControl(camera: Camera?): Camera2CameraControl? {
        if (camera == null) {
            onError(ErrorType.HARDWARE_UNAVAILABLE, "Camera not available")
            return null
        }
        return runCatching {
            Camera2CameraControl.from(camera.cameraControl)
        }.getOrElse {
            Log.w(TAG, "Camera2 interop is not available", it)
            onError(ErrorType.FEATURE_NOT_SUPPORTED, "Camera2 interop not available")
            null
        }
    }

    companion object {
        private const val TAG = "CameraControlsManager"
        private const val DEFAULT_EXPOSURE_TIME_NS = 16_666_667L // ~1/60s
        private val DirectExecutor = Executor { command -> command.run() }
    }
}
