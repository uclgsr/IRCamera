package com.topdon.tc001.camera.core

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Camera2-only module for Samsung S22 dual-mode camera system
 * 
 * Implements the clean architecture requested:
 * - One camera client only (no CameraX conflicts)
 * - Fast switching without closing CameraDevice  
 * - Deterministic state machine
 * - Capabilities detection once at camera open
 */
class CameraController(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraController"
        private const val CAMERA_OPEN_TIMEOUT_MS = 2500L
    }
    
    // Camera state
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId: String = "0"
    private var deviceCaps: DeviceCaps? = null
    
    // Background thread for camera operations
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)
    
    // State callbacks
    var onCameraOpened: ((DeviceCaps) -> Unit)? = null
    var onCameraError: ((String) -> Unit)? = null
    
    init {
        startBackgroundThread()
    }
    
    /**
     * Open camera and detect capabilities once
     */
    fun openCamera(cameraId: String = "0") {
        Log.i(TAG, "Opening camera $cameraId")
        
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId)
            
            // Detect capabilities once at camera open (as specified)
            deviceCaps = detectCapabilities(characteristics)
            Log.i(TAG, "Device capabilities: $deviceCaps")
            
            if (!cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            currentCameraId = cameraId
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera $cameraId", e)
            onCameraError?.invoke("Failed to open camera: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted", e)
            onCameraError?.invoke("Camera permission required")
        }
    }
    
    /**
     * Create capture session for specific mode
     * Fast switching without closing CameraDevice
     */
    fun createCaptureSession(surfaces: List<Surface>, callback: CameraCaptureSession.StateCallback) {
        cameraDevice?.let { device ->
            try {
                // Close previous session but keep camera device open
                captureSession?.close()
                captureSession = null
                
                Log.i(TAG, "Creating capture session with ${surfaces.size} surfaces")
                device.createCaptureSession(surfaces, callback, backgroundHandler)
                
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to create capture session", e)
                onCameraError?.invoke("Failed to create capture session: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "Cannot create session - camera device is null")
            onCameraError?.invoke("Camera not opened")
        }
    }
    
    /**
     * Create capture request builder for specific template
     */
    fun createCaptureRequest(template: Int): CaptureRequest.Builder? {
        return try {
            cameraDevice?.createCaptureRequest(template)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture request", e)
            null
        }
    }
    
    /**
     * Get device capabilities (detected once at open)
     */
    fun getDeviceCaps(): DeviceCaps? = deviceCaps
    
    /**
     * Check if camera is open
     */
    fun isOpen(): Boolean = cameraDevice != null
    
    /**
     * Set current capture session
     */
    fun setCaptureSession(session: CameraCaptureSession) {
        captureSession = session
    }
    
    /**
     * Get current capture session
     */
    fun getCaptureSession(): CameraCaptureSession? = captureSession
    
    /**
     * Close camera and cleanup
     */
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
    
    /**
     * Capabilities detection (once, at camera open)
     */
    private fun detectCapabilities(characteristics: CameraCharacteristics): DeviceCaps {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: IntArray(0)
        val supportsRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        
        // RAW sizes: from SCALER_STREAM_CONFIGURATION_MAP.getOutputSizes(RAW_SENSOR). Pick max.
        val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR) ?: arrayOf(Size(0, 0))
        val rawSize = rawSizes.maxByOrNull { it.width * it.height } ?: Size(0, 0)
        
        // High-speed: Check 3840×2160 with fpsRange including 60
        var supports4k60 = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val highSpeedConfigs = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
                highSpeedConfigs?.forEach { config ->
                    if (config.width == 3840 && config.height == 2160) {
                        // Check if any fps range includes 60
                        map?.getHighSpeedVideoFpsRangesFor(Size(3840, 2160))?.forEach { range ->
                            if (range.upper >= 60) {
                                supports4k60 = true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "High-speed video detection failed: ${e.message}")
        }
        
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        
        return DeviceCaps(
            supportsRaw = supportsRaw,
            rawSize = rawSize,
            supports4k60 = supports4k60,
            sensorOrientation = sensorOrientation
        )
    }
    
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            Log.i(TAG, "Camera opened successfully")
            
            deviceCaps?.let { caps ->
                onCameraOpened?.invoke(caps)
            }
        }
        
        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.w(TAG, "Camera disconnected")
            onCameraError?.invoke("Camera disconnected")
        }
        
        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            
            val errorMessage = when (error) {
                CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera is in use by another app"
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Too many cameras in use"
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled by device policy"
                CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                else -> "Unknown camera error: $error"
            }
            
            Log.e(TAG, "Camera error: $errorMessage")
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
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
}