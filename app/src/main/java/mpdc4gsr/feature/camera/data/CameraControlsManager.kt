package mpdc4gsr.feature.camera.data

import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import mpdc4gsr.core.data.ErrorType

class CameraControlsManager(
    private val onError: ((ErrorType, String) -> Unit)?
) {
    companion object {
    }

    fun setManualExposureMode(camera: Camera?, enabled: Boolean) {
            camera?.cameraControl?.let { cameraControl ->
                if (enabled) {
                    // For manual exposure, we'd need to use Camera2 interop 
                    // This is a simplified implementation that locks exposure
                    cameraControl.enableTorch(false) // Ensure torch is off for consistent exposure
                } else {
                    // Return to auto exposure
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for exposure control"
                )
            }
        }
    }

    fun setExposureCompensation(camera: Camera?, evValue: Float) {
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
                } ?: run {
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
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
            )
        }
    }

    fun setAutoExposureLock(camera: Camera?, locked: Boolean) {
            camera?.cameraControl?.let { cameraControl ->
                // CameraX doesn't have direct AE lock, but we can implement via Camera2 interop
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for exposure control"
                )
            }
        }
    }

    fun setManualFocusMode(camera: Camera?, enabled: Boolean) {
            camera?.cameraControl?.let { cameraControl ->
                if (enabled) {
                    // Cancel any ongoing autofocus
                    cameraControl.cancelFocusAndMetering()
                } else {
                    // Return to continuous autofocus
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        }
    }

    fun setFocusDistance(camera: Camera?, distance: Float) {
            camera?.let { cam ->
                val clampedDistance = distance.coerceIn(0.0f, 1.0f)
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
                                TAG,
                                "Manual focus distance set to: $focusDistanceText (normalized: $clampedDistance, actual: $actualFocusDistance)"
                            )
                        } else {
                            onError?.invoke(
                                ErrorType.FEATURE_NOT_SUPPORTED,
                                "Manual focus distance not supported on this device"
                            )
                        }
                    } ?: run {
                        onError?.invoke(
                            ErrorType.FEATURE_NOT_SUPPORTED,
                            "Focus distance characteristics not available"
                        )
                    }
                    // Fallback to basic CameraX focus control
                    cam.cameraControl.cancelFocusAndMetering()
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
            )
        }
    }

    fun setAutoFocusLock(camera: Camera?, locked: Boolean) {
            camera?.cameraControl?.let { cameraControl ->
                if (locked) {
                    // Lock focus at current position
                } else {
                    // Unlock and resume continuous AF
                }
            } ?: run {
                onError?.invoke(
                    ErrorType.HARDWARE_UNAVAILABLE,
                    "Camera not available for focus control"
                )
            }
        }
    }

    fun triggerTapToFocus(camera: Camera?, previewView: PreviewView?, x: Float, y: Float) {
            camera?.cameraControl?.let { cameraControl ->
                previewView?.let { preview ->
                    val factory = preview.meteringPointFactory
                    val point = factory.createPoint(x * preview.width, y * preview.height)
                    val action = FocusMeteringAction.Builder(point)
                        .disableAutoCancel()
                        .build()
                    cameraControl.startFocusAndMetering(action)
                } ?: run {
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
            onError?.invoke(
                ErrorType.OPERATION_FAILED,
            )
        }
    }
}