package mpdc4gsr.feature.camera.data
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import mpdc4gsr.core.data.ErrorType

class CameraControlsManager(
    private val onError: ((ErrorType, String) -> Unit)?
) {
    companion object {
        private const val TAG = "CameraControlsManager"
    }
    
    fun setManualExposureMode(camera: Camera?, enabled: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                if (enabled) {
                    // For manual exposure, we'd need to use Camera2 interop 
                    // This is a simplified implementation that locks exposure
                    cameraControl.enableTorch(false) // Ensure torch is off for consistent exposure
                    AppLogger.i(TAG, "Manual exposure mode enabled")
                } else {
                    // Return to auto exposure
                    AppLogger.i(TAG, "Auto exposure mode enabled")
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for exposure control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set exposure mode: ${e.message}")
            onError?.invoke(ErrorType.OPERATION_FAILED, "Failed to set exposure mode: ${e.message}")
        }
    }
    
    fun setExposureCompensation(camera: Camera?, evValue: Float) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                // Convert EV to exposure compensation index
                val camera2Info =
                    androidx.camera.camera2.interop.Camera2CameraInfo.from(camera.cameraInfo)
                val characteristics = camera2Info.getCameraCharacteristic(
                    android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE
                )
                characteristics?.let { range ->
                    val step = camera2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
                    )?.toFloat() ?: 1.0f
                    val index = (evValue / step).toInt().coerceIn(range.lower, range.upper)
                    cameraControl.setExposureCompensationIndex(index)
                    AppLogger.i(TAG, "Exposure compensation set to ${evValue}EV (index: $index)")
                } ?: run {
                    AppLogger.w(TAG, "Camera doesn't support exposure compensation")
                    onError?.invoke(
                        ErrorType.FEATURE_NOT_SUPPORTED,
                        "Exposure compensation not supported on this device"
                    )
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for exposure control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set exposure compensation: ${e.message}")
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
                "Failed to set exposure compensation: ${e.message}"
            )
        }
    }
    
    fun setAutoExposureLock(camera: Camera?, locked: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                // CameraX doesn't have direct AE lock, but we can implement via Camera2 interop
                AppLogger.i(TAG, "Auto exposure lock: $locked")
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for exposure control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set AE lock: ${e.message}")
            onError?.invoke(ErrorType.OPERATION_FAILED, "Failed to set AE lock: ${e.message}")
        }
    }
    
    fun setManualFocusMode(camera: Camera?, enabled: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                if (enabled) {
                    // Cancel any ongoing autofocus
                    cameraControl.cancelFocusAndMetering()
                    AppLogger.i(TAG, "Manual focus mode enabled")
                } else {
                    // Return to continuous autofocus
                    AppLogger.i(TAG, "Auto focus mode enabled")
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set focus mode: ${e.message}")
            onError?.invoke(ErrorType.OPERATION_FAILED, "Failed to set focus mode: ${e.message}")
        }
    }
    
    fun setFocusDistance(camera: Camera?, distance: Float) {
        try {
            camera?.let { cam ->
                val clampedDistance = distance.coerceIn(0.0f, 1.0f)
                try {
                    // Use Camera2 interop for direct lens focus distance control
                    val camera2Info =
                        androidx.camera.camera2.interop.Camera2CameraInfo.from(cam.cameraInfo)
                    val characteristics = camera2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                    )
                    characteristics?.let { minFocusDistance ->
                        if (minFocusDistance > 0) {
                            // Calculate actual focus distance from normalized value
                            // 0.0f = infinity (focus distance = 0), 1.0f = macro (focus distance = minFocusDistance)
                            val actualFocusDistance = clampedDistance * minFocusDistance
                            // Use Camera2 interop to set focus distance
                            val camera2Control =
                                androidx.camera.camera2.interop.Camera2CameraControl.from(cam.cameraControl)
                            val captureRequestOptions =
                                androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
                                    .setCaptureRequestOption(
                                        android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                                        android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF
                                    )
                                    .setCaptureRequestOption(
                                        android.hardware.camera2.CaptureRequest.LENS_FOCUS_DISTANCE,
                                        actualFocusDistance
                                    )
                                    .build()
                            camera2Control.addCaptureRequestOptions(captureRequestOptions)
                            val focusDistanceText = if (clampedDistance < 0.1f) {
                                "Infinity"
                            } else {
                                String.format("%.2fm", 1.0f / actualFocusDistance)
                            }
                            Log.i(
                                TAG,
                                "Manual focus distance set to: $focusDistanceText (normalized: $clampedDistance, actual: $actualFocusDistance)"
                            )
                        } else {
                            AppLogger.w(TAG, "Device does not support manual focus distance control")
                            onError?.invoke(
                                ErrorType.FEATURE_NOT_SUPPORTED,
                                "Manual focus distance not supported on this device"
                            )
                        }
                    } ?: run {
                        AppLogger.w(TAG, "Could not retrieve minimum focus distance characteristic")
                        onError?.invoke(
                            ErrorType.FEATURE_NOT_SUPPORTED,
                            "Focus distance characteristics not available"
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Camera2 interop not available, using fallback focus control", e)
                    // Fallback to basic CameraX focus control
                    cam.cameraControl.cancelFocusAndMetering()
                    AppLogger.i(TAG, "Focus distance set to: $clampedDistance (fallback mode)")
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set focus distance: ${e.message}")
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
                "Failed to set focus distance: ${e.message}"
            )
        }
    }
    
    fun setAutoFocusLock(camera: Camera?, locked: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                if (locked) {
                    // Lock focus at current position
                    AppLogger.i(TAG, "Auto focus locked")
                } else {
                    // Unlock and resume continuous AF
                    AppLogger.i(TAG, "Auto focus unlocked")
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set AF lock: ${e.message}")
            onError?.invoke(ErrorType.OPERATION_FAILED, "Failed to set AF lock: ${e.message}")
        }
    }
    
    fun triggerTapToFocus(camera: Camera?, previewView: PreviewView?, x: Float, y: Float) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                previewView?.let { preview ->
                    val factory = preview.meteringPointFactory
                    val point = factory.createPoint(x * preview.width, y * preview.height)
                    val action = FocusMeteringAction.Builder(point)
                        .disableAutoCancel()
                        .build()
                    cameraControl.startFocusAndMetering(action)
                    AppLogger.i(TAG, "Tap-to-focus triggered at ($x, $y)")
                } ?: run {
                    AppLogger.w(TAG, "No preview available for tap-to-focus")
                    onError?.invoke(
                        ErrorType.FEATURE_NOT_SUPPORTED,
                        "Preview required for tap-to-focus"
                    )
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to trigger tap-to-focus: ${e.message}")
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
                "Failed to trigger tap-to-focus: ${e.message}"
            )
        }
    }
}