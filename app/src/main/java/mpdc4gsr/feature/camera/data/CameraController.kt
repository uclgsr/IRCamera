package mpdc4gsr.feature.camera.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import android.view.Surface
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CameraController(private val context: Context) {
    companion object {
        private const val TAG = "CameraController"
        private const val CAMERA_OPEN_TIMEOUT_MS = 2500L
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId: String = "0"
    private var deviceCaps: DeviceCaps? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)

    var onCameraOpened: ((DeviceCaps) -> Unit)? = null
    var onCameraError: ((String) -> Unit)? = null

    init {
        startBackgroundThread()
    }

    fun openCamera(cameraId: String = "0") {
        AppLogger.i(TAG, "Opening camera $cameraId")

        var lockAcquired = false
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraIdList = try {
                manager.cameraIdList
            } catch (e: CameraAccessException) {
                AppLogger.e(TAG, "Failed to get camera list", e)
                onCameraError?.invoke("Camera service unavailable: ${e.message}")
                return
            }

            if (cameraIdList.isEmpty()) {
                AppLogger.e(TAG, "No cameras available on device")
                onCameraError?.invoke("No cameras found on this device")
                return
            }

            if (!cameraIdList.contains(cameraId)) {
                AppLogger.e(TAG, "Camera $cameraId not found. Available cameras: ${cameraIdList.joinToString()}")
                onCameraError?.invoke("Camera $cameraId not available. Available: ${cameraIdList.joinToString()}")
                return
            }

            val characteristics = manager.getCameraCharacteristics(cameraId)

            deviceCaps = detectCapabilities(characteristics)
            AppLogger.i(TAG, "Device capabilities: $deviceCaps")

            if (!cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                val errorMsg = "Timeout waiting to lock camera opening. Camera may be in use by another process"
                AppLogger.e(TAG, errorMsg)
                onCameraError?.invoke(errorMsg)
                return
            }
            lockAcquired = true

            AppLogger.d(TAG, "Requesting camera open with ID: $cameraId")
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            currentCameraId = cameraId
        } catch (e: CameraAccessException) {
            if (lockAcquired) {
                cameraOpenCloseLock.release()
            }
            val reason = when (e.reason) {
                CameraAccessException.CAMERA_DISABLED -> "Camera disabled by device policy"
                CameraAccessException.CAMERA_DISCONNECTED -> "Camera disconnected"
                CameraAccessException.CAMERA_ERROR -> "Camera service error"
                CameraAccessException.CAMERA_IN_USE -> "Camera already in use by another app"
                CameraAccessException.MAX_CAMERAS_IN_USE -> "Maximum number of cameras in use"
                else -> "Unknown camera access error (${e.reason})"
            }
            AppLogger.e(TAG, "Failed to open camera $cameraId: $reason", e)
            onCameraError?.invoke("Failed to open camera: $reason")
        } catch (e: SecurityException) {
            if (lockAcquired) {
                cameraOpenCloseLock.release()
            }
            AppLogger.e(TAG, "Camera permission not granted", e)
            onCameraError?.invoke("Camera permission required. Please grant camera permission in Settings")
        } catch (e: IllegalArgumentException) {
            if (lockAcquired) {
                cameraOpenCloseLock.release()
            }
            AppLogger.e(TAG, "Invalid camera ID: $cameraId", e)
            onCameraError?.invoke("Invalid camera ID: $cameraId")
        } catch (e: Exception) {
            if (lockAcquired) {
                cameraOpenCloseLock.release()
            }
            AppLogger.e(TAG, "Unexpected error opening camera $cameraId", e)
            onCameraError?.invoke("Failed to open camera: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    fun createCaptureSession(
        surfaces: List<Surface>,
        callback: CameraCaptureSession.StateCallback,
    ) {
        cameraDevice?.let { device ->
            try {

                captureSession?.close()
                captureSession = null

                AppLogger.i(TAG, "Creating capture session with ${surfaces.size} surfaces")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val outputConfigs = surfaces.map { OutputConfiguration(it) }
                    val sessionConfig = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputConfigs,
                        object : Executor {
                            override fun execute(command: Runnable) {
                                backgroundHandler?.post(command)
                            }
                        },
                        callback
                    )
                    device.createCaptureSession(sessionConfig)
                } else {
                    @Suppress("DEPRECATION")
                    device.createCaptureSession(surfaces, callback, backgroundHandler)
                }
            } catch (e: CameraAccessException) {
                AppLogger.e(TAG, "Failed to create capture session", e)
                onCameraError?.invoke("Failed to create capture session: ${e.message}")
            }
        } ?: run {
            AppLogger.e(TAG, "Cannot create session - camera device is null")
            onCameraError?.invoke("Camera not opened")
        }
    }

    fun createCaptureRequest(template: Int): CaptureRequest.Builder? {
        return try {
            cameraDevice?.createCaptureRequest(template)
        } catch (e: CameraAccessException) {
            AppLogger.e(TAG, "Failed to create capture request", e)
            null
        }
    }

    fun getDeviceCaps(): DeviceCaps? = deviceCaps

    fun isOpen(): Boolean = cameraDevice != null

    fun setCaptureSession(session: CameraCaptureSession) {
        captureSession = session
    }

    fun getCaptureSession(): CameraCaptureSession? = captureSession

    fun getCameraCharacteristics(): CameraCharacteristics? {
        return try {
            if (currentCameraId.isNotEmpty()) {
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                manager.getCameraCharacteristics(currentCameraId)
            } else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get camera characteristics", e)
            null
        }
    }

    fun close() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }

        stopBackgroundThread()
    }

    private fun detectCapabilities(characteristics: CameraCharacteristics): DeviceCaps {
        val capabilities =
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: IntArray(0)
        val supportsRaw =
            capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR) ?: arrayOf(Size(0, 0))
        val rawSize = rawSizes.maxByOrNull { it.width * it.height } ?: Size(0, 0)

        var supports4k60 = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                map?.getHighSpeedVideoSizes()?.forEach { size ->
                    if (size.width == 3840 && size.height == 2160) {

                        map.getHighSpeedVideoFpsRangesFor(size)?.forEach { range ->
                            if (range.upper >= 60) {
                                supports4k60 = true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "High-speed video detection failed: ${e.message}")
        }

        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        return DeviceCaps(
            supportsRaw = supportsRaw,
            rawSize = rawSize,
            supports4k60 = supports4k60,
            sensorOrientation = sensorOrientation,
        )
    }

    private val stateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraOpenCloseLock.release()
                cameraDevice = camera
                AppLogger.i(TAG, "Camera opened successfully")

                deviceCaps?.let { caps ->
                    onCameraOpened?.invoke(caps)
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraOpenCloseLock.release()
                camera.close()
                cameraDevice = null
                AppLogger.w(TAG, "Camera disconnected")
                onCameraError?.invoke("Camera disconnected")
            }

            override fun onError(
                camera: CameraDevice,
                error: Int,
            ) {
                cameraOpenCloseLock.release()
                camera.close()
                cameraDevice = null

                val errorMessage =
                    when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera is in use by another app"
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Too many cameras in use"
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled by device policy"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                        else -> "Unknown camera error: $error"
                    }

                AppLogger.e(TAG, "Camera error: $errorMessage")
                onCameraError?.invoke("Camera error: $errorMessage")
            }
        }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            AppLogger.e(TAG, "Error stopping background thread", e)
        }
    }
}
