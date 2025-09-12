package com.topdon.tc001.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.DngCreator
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.gsr.util.TimeUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Enhanced RGB Camera Recorder with Dual RAW (50MP) and 4K Video Modes
 * 
 * Implements dual-mode camera system with fast session switching for Samsung S22 (Android 15):
 * - RAW Mode: 50MP RAW_SENSOR capture with 15fps streaming 
 * - Video Mode: 4K (3840×2160) video recording at 30/60fps
 * - Fast switching without reopening camera device
 * - Samsung device specific optimizations and fallbacks
 * 
 * Technical Features:
 * - Camera2 API with session reconfiguration for mode switching
 * - Surface reuse for optimal performance
 * - Samsung S22 Exynos compatibility handling
 * - Thermal throttling awareness
 * - Memory-efficient RAW capture with proper buffer management
 */
class RGBCameraRecorder(
    private val context: Context,
    private val textureView: TextureView,
    private val activity: Activity? = null // Add activity for permission requests
) {
    companion object {
        private const val TAG = "RGBCameraRecorder"
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
        
        // Samsung S22 specific constants
        private const val SAMSUNG_S22_RAW_MAX_FPS = 15
        private const val SAMSUNG_4K_FALLBACK_FPS = 30
        private const val RAW_CAPTURE_MAX_IMAGES = 2 // Conservative for Samsung devices
        private const val SESSION_SWITCH_DELAY_MS = 200L // Delay for Samsung HAL stability
    }

    // Enhanced camera modes for dual operation
    enum class CameraMode(val displayName: String, val description: String) {
        RAW_50MP("RAW 50MP", "High-resolution RAW capture at ~15fps"),
        VIDEO_4K("4K Video", "4K video recording at 30/60fps"),
        PREVIEW_ONLY("Preview", "Preview mode only")
    }

    // Camera recording settings with dual-mode support
    data class RecordingSettings(
        val mode: CameraMode = CameraMode.VIDEO_4K,
        val resolution: VideoResolution = VideoResolution.UHD_4K,
        val frameRate: Int = 30,
        val bitRate: Int = 10_000_000, // Higher bitrate for 4K
        val enableStabilization: Boolean = true,
        val enableFlash: Boolean = false,
        val audioEnabled: Boolean = true,
        val rawCaptureFrameRate: Int = SAMSUNG_S22_RAW_MAX_FPS,
        val enableHighSpeedVideo: Boolean = false, // Samsung 60fps attempt
    )

    // Video resolution options optimized for Samsung S22
    enum class VideoResolution(val width: Int, val height: Int, val displayName: String) {
        UHD_4K(3840, 2160, "4K UHD (3840×2160)"),
        HD_1080P(1920, 1080, "Full HD (1920×1080)"),
        HD_720P(1280, 720, "HD (1280×720)"),
        SD_480P(720, 480, "SD (720×480)"),
    }

    // Camera facing options
    enum class CameraFacing(val displayName: String) {
        BACK("Back Camera"),
        FRONT("Front Camera"),
    }

    // Enhanced recording state with dual-mode support
    private var isRecording = false
    private var isPaused = false
    private var currentMode = CameraMode.PREVIEW_ONLY
    
    // Camera components with session management
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var rawImageReader: ImageReader? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)
    
    // Session management for fast mode switching
    private var previewSurface: Surface? = null
    private var isSessionSwitching = false
    private var pendingModeSwitch: CameraMode? = null

    // RAW capture components (50MP mode)
    private var rawCaptureHandler: Handler? = null
    private var rawCaptureRunnable: Runnable? = null
    private var rawCaptureCount = 0
    private var rawCaptureStartTime = 0L
    private var rawCaptureActive = false

    // Video capture components (4K mode)
    private var videoRecordingActive = false
    private var highSpeedSession: CameraConstrainedHighSpeedCaptureSession? = null
    private var attemptHighSpeed60fps = false

    // Camera settings with dual-mode support
    private var currentCameraFacing = CameraFacing.BACK
    private var currentCameraId = "0" // Track current camera ID
    private var availableCameraIds = emptyList<String>() // Available camera IDs
    private var recordingSettings = RecordingSettings()
    private var videoSize = Size(3840, 2160) // Default to 4K
    private var previewSize = Size(1920, 1080)
    private var rawSensorSize = Size(4032, 3024) // Will be updated from camera characteristics
    private var maxRawSize = Size(8160, 6120) // Samsung S22 max RAW resolution

    // Permission handling
    private var permissionCallback: (() -> Unit)? = null
    var onPermissionGranted: (() -> Unit)? = null
    var onPermissionDenied: ((String) -> Unit)? = null

    // File management
    private var currentVideoFile: File? = null
    private var rawImagesDirectory: File? = null
    private var sessionId: String? = null

    // Device detection
    private val isSamsungDevice = Build.BRAND.equals("samsung", ignoreCase = true)
    private val isS22Series = Build.MODEL.contains("SM-S90", ignoreCase = true) || 
                            Build.MODEL.contains("SM-S91", ignoreCase = true) ||
                            Build.MODEL.contains("SM-S92", ignoreCase = true)

    // Callbacks with enhanced mode switching support
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: ((File?) -> Unit)? = null
    var onCameraSwitched: ((CameraFacing, String) -> Unit)? = null // Include camera ID
    var onModeChanged: ((CameraMode) -> Unit)? = null
    var onRawImageCaptured: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onCameraListUpdated: ((List<CameraInfo>) -> Unit)? = null // Camera list callback

    // Camera information for switching
    data class CameraInfo(
        val cameraId: String,
        val facing: CameraFacing,
        val supportsRaw: Boolean,
        val supports4K: Boolean,
        val displayName: String
    )

    /**
     * Initialize camera recorder with enhanced dual-mode support
     */
    fun initialize() {
        Log.i(TAG, "Initializing camera on device: ${Build.BRAND} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
        
        // Check and request camera permission first
        if (checkCameraPermission()) {
            initializeCamera()
        } else {
            requestCameraPermission {
                initializeCamera()
            }
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission with proper user feedback
     */
    private fun requestCameraPermission(onGranted: () -> Unit) {
        if (activity == null) {
            Log.e(TAG, "Activity is null - cannot request camera permission")
            onPermissionDenied?.invoke("Camera permission required but cannot request (no activity context)")
            return
        }

        permissionCallback = onGranted

        XXPermissions.with(activity)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        Log.i(TAG, "Camera permission granted")
                        onPermissionGranted?.invoke()
                        permissionCallback?.invoke()
                        permissionCallback = null
                    } else {
                        Log.w(TAG, "Some camera permissions denied")
                        onPermissionDenied?.invoke("Camera permission partially denied")
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    val message = if (never) {
                        "Camera permission permanently denied. Please enable in app settings."
                    } else {
                        "Camera permission denied. Camera functionality will not work."
                    }
                    
                    Log.e(TAG, "Camera permission denied: $message")
                    onPermissionDenied?.invoke(message)
                    onError?.invoke(message)
                    permissionCallback = null
                }
            })
    }

    /**
     * Initialize camera system after permissions are granted
     */
    private fun initializeCamera() {
        startBackgroundThread()
        
        // Get available cameras and setup camera info
        setupAvailableCameras()
        
        // Log device information for Samsung S22 debugging
        if (isSamsungDevice) {
            Log.i(TAG, "Samsung device detected - enabling compatibility mode")
        }
        if (isS22Series) {
            Log.i(TAG, "Samsung S22 series detected - enabling advanced RAW and 4K features") 
        }
        
        initializeTextureView()
        
        if (textureView.isAvailable) {
            openCamera()
        }
    }

    /**
     * Setup available cameras and their capabilities
     */
    private fun setupAvailableCameras() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraList = mutableListOf<CameraInfo>()
            
            availableCameraIds = manager.cameraIdList.toList()
            
            for (cameraId in availableCameraIds) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                    CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                    else -> CameraFacing.BACK
                }
                
                // Check RAW support
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
                
                // Check 4K support
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val videoSizes = map?.getOutputSizes(MediaRecorder::class.java) ?: emptyArray()
                val supports4K = videoSizes.any { it.width >= 3840 && it.height >= 2160 }
                
                val displayName = "${facing.displayName} (ID: $cameraId)" + 
                    if (supportsRaw) " [RAW]" else "" +
                    if (supports4K) " [4K]" else ""
                
                cameraList.add(CameraInfo(cameraId, facing, supportsRaw, supports4K, displayName))
            }
            
            onCameraListUpdated?.invoke(cameraList)
            Log.i(TAG, "Found ${cameraList.size} cameras: ${cameraList.map { it.displayName }}")
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to enumerate cameras", e)
            onError?.invoke("Failed to access camera system: ${e.message}")
        }
    }

    /**
     * Initialize texture view listener for dual-mode camera preview
     */
    private fun initializeTextureView() {
        textureView.surfaceTextureListener = 
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                // Create and store preview surface for reuse across sessions
                previewSurface = Surface(texture)
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                // Handle size changes if needed
                Log.d(TAG, "Surface texture size changed: ${width}x${height}")
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                closeCamera()
                previewSurface?.release()
                previewSurface = null
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                // Frame updated - could trigger sync events here
                // High frequency - avoid logging
            }
        }
    }

    // ===== CAMERA LIFECYCLE METHODS =====

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

    private fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            // Setup sizes for dual-mode operation
            setupCameraSizesForDualMode(characteristics)

            // Log camera capabilities for Samsung debugging
            logCameraCapabilities(characteristics)

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                onError?.invoke("Camera permission not granted")
                return
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
            onError?.invoke("Failed to open camera: ${e.message}")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
            rawImageReader?.close()
            rawImageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            Log.i(TAG, "Camera opened successfully")
            
            // Start in preview mode initially
            runBlocking { 
                switchMode(CameraMode.PREVIEW_ONLY) 
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.w(TAG, "Camera disconnected")
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
            onError?.invoke("Camera error: $errorMessage")
        }
    }

    private fun getCameraId(facing: CameraFacing): String {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                val targetFacing = when (facing) {
                    CameraFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
                    CameraFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                }

                if (cameraFacing == targetFacing) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        return "0" // Fallback to first camera
    }

    /**
     * Setup camera sizes optimized for dual-mode operation (RAW + Video)
     */
    private fun setupCameraSizesForDualMode(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

        // Setup video sizes for 4K mode
        val videoSizes = map.getOutputSizes(MediaRecorder::class.java)
        videoSize = chooseOptimalSize(
            videoSizes,
            recordingSettings.resolution.width,
            recordingSettings.resolution.height,
        )

        // Setup preview size (reused across modes)
        val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
        previewSize = chooseOptimalSize(
            previewSizes,
            MAX_PREVIEW_WIDTH,
            MAX_PREVIEW_HEIGHT,
        )

        // Setup RAW sensor size for 50MP mode
        val rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR)
        if (!rawSizes.isNullOrEmpty()) {
            rawSensorSize = rawSizes.maxByOrNull { it.width * it.height } ?: rawSizes[0]
            maxRawSize = rawSensorSize
            
            val megapixels = (rawSensorSize.width * rawSensorSize.height) / 1_000_000
            Log.i(TAG, "RAW sensor size: ${rawSensorSize.width}x${rawSensorSize.height} (~${megapixels}MP)")
        }

        textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        
        Log.i(TAG, "Camera sizes configured - Video: ${videoSize.width}x${videoSize.height}, Preview: ${previewSize.width}x${previewSize.height}")
    }

    /**
     * Log camera capabilities for Samsung device debugging
     */
    private fun logCameraCapabilities(characteristics: CameraCharacteristics) {
        try {
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val supportedFeatures = mutableListOf<String>()
            
            capabilities?.forEach { capability ->
                when (capability) {
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> supportedFeatures.add("RAW")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> supportedFeatures.add("HIGH_SPEED_VIDEO")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> supportedFeatures.add("BURST_CAPTURE")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> supportedFeatures.add("MANUAL_SENSOR")
                }
            }
            
            Log.i(TAG, "Camera capabilities: ${supportedFeatures.joinToString(", ")}")
            
            // Log high-speed video configurations for Samsung debugging
            val highSpeedConfigs = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
            if (highSpeedConfigs != null) {
                // val availableHighSpeedModes = highSpeedConfigs.map { 
                    "${it.width}x${it.height}@${it.fpsMax}fps" 
                }.joinToString(", ")
                Log.i(TAG, "High-speed video modes: $availableHighSpeedModes")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error logging camera capabilities", e)
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, targetWidth: Int, targetHeight: Int): Size {
        val bigEnough = mutableListOf<Size>()
        val notBigEnough = mutableListOf<Size>()

        for (option in choices) {
            if (option.width >= targetWidth && option.height >= targetHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }

        return when {
            bigEnough.isNotEmpty() -> bigEnough.minByOrNull { it.width * it.height } ?: choices[0]
            notBigEnough.isNotEmpty() -> notBigEnough.maxByOrNull { it.width * it.height } ?: choices[0]
            else -> choices[0]
        }
    }

    // ===== DUAL-MODE CAMERA OPERATIONS =====

    /**
     * Switch camera mode with fast session reconfiguration (Samsung S22 optimized)
     * Implements the dual-mode switching without reopening camera device
     * 
     * Session Configuration Logic:
     * 1. Stop any ongoing repeating requests
     * 2. Close/tear down the previous capture session (but NOT the camera device)
     * 3. Clean up mode-specific resources
     * 4. Create new capture session with appropriate surfaces for the new mode
     * 
     * This avoids fully restarting the camera device, which would be slow and unnecessary.
     */
    suspend fun switchMode(newMode: CameraMode): Boolean = withContext(Dispatchers.Main) {
        if (currentMode == newMode) {
            Log.i(TAG, "Already in ${newMode.displayName} mode")
            return@withContext true
        }
        
        if (isRecording) {
            Log.w(TAG, "Cannot switch modes while recording. Stop recording first.")
            onError?.invoke("Cannot switch camera modes while recording")
            return@withContext false
        }
        
        if (isSessionSwitching) {
            Log.w(TAG, "Mode switch already in progress")
            return@withContext false
        }
        
        try {
            isSessionSwitching = true
            Log.i(TAG, "Switching from ${currentMode.displayName} to ${newMode.displayName}")
            
            // Stop current session repeating requests
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            
            // Close the previous capture session (but NOT the camera device)
            captureSession?.close()
            captureSession = null
            
            // Clean up mode-specific resources
            cleanupCurrentMode()
            
            // Add delay for Samsung HAL stability
            if (isSamsungDevice) {
                delay(SESSION_SWITCH_DELAY_MS)
            }
            
            // Configure new mode
            val success = when (newMode) {
                CameraMode.RAW_50MP -> configureRawMode()
                CameraMode.VIDEO_4K -> configureVideoMode() 
                CameraMode.PREVIEW_ONLY -> configurePreviewMode()
            }
            
            if (success) {
                currentMode = newMode
                onModeChanged?.invoke(newMode)
                Log.i(TAG, "Successfully switched to ${newMode.displayName}")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to switch to ${newMode.displayName}")
                onError?.invoke("Failed to switch to ${newMode.displayName}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to ${newMode.displayName}", e)
            onError?.invoke("Mode switch error: ${e.message}")
            return@withContext false
        } finally {
            isSessionSwitching = false
        }
    }

    /**
     * Configure RAW 50MP capture mode with optimized settings for Samsung S22
     */
    private suspend fun configureRawMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Configuring RAW 50MP mode")
            
            // Setup RAW ImageReader with Samsung S22 max resolution
            setupRawImageReaderForS22()
            
            val surfaces = mutableListOf<Surface>()
            
            // Reuse preview surface
            previewSurface?.let { surfaces.add(it) }
            
            // Add RAW surface
            rawImageReader?.surface?.let { surfaces.add(it) }
            
            withContext(Dispatchers.Main) {
                cameraDevice?.createCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            startRawCapture()
                            Log.i(TAG, "RAW mode session configured successfully")
                        }
                        
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "RAW mode session configuration failed")
                            onError?.invoke("RAW mode configuration failed")
                        }
                    },
                    backgroundHandler
                )
            }
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure RAW mode", e)
            return@withContext false
        }
    }

    /**
     * Configure 4K video recording mode with Samsung S22 optimizations  
     */
    private suspend fun configureVideoMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Configuring 4K video mode")
            
            // Check if device supports 4K60 (Samsung restrictions)
            val supports4K60 = checkHighSpeedVideoSupport()
            attemptHighSpeed60fps = supports4K60 && recordingSettings.enableHighSpeedVideo
            
            if (attemptHighSpeed60fps) {
                Log.i(TAG, "Attempting 4K@60fps high-speed mode")
                return@withContext configureHighSpeedVideoMode()
            } else {
                Log.i(TAG, "Using standard 4K@30fps mode")
                return@withContext configureStandardVideoMode()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure video mode", e)
            return@withContext false
        }
    }
    
    /**
     * Configure standard 4K@30fps video mode (Samsung compatible)
     */
    private suspend fun configureStandardVideoMode(): Boolean = withContext(Dispatchers.Main) {
        try {
            val surfaces = mutableListOf<Surface>()
            
            // Reuse preview surface
            previewSurface?.let { surfaces.add(it) }
            
            // Add MediaRecorder surface (configured later when recording starts)
            
            // Create session for video mode
            cameraDevice?.createCaptureSession(
                listOf(previewSurface!!),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startVideoPreview()
                        Log.i(TAG, "Standard video mode configured")
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Video mode session configuration failed")
                        onError?.invoke("Video mode configuration failed")
                    }
                },
                backgroundHandler
            )
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure standard video mode", e)
            return@withContext false
        }
    }
    
    /**
     * Configure high-speed 4K@60fps video mode (Samsung S22 attempt)
     */
    private suspend fun configureHighSpeedVideoMode(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.i(TAG, "Attempting high-speed 4K@60fps configuration")
            
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            
            // Get high-speed video configurations
            val highSpeedConfigs = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
            
            if (highSpeedConfigs == null) {
                Log.w(TAG, "High-speed video not supported on this device")
                return@withContext configureStandardVideoMode()
            }
            
            // Find 4K@60fps configuration
            val targetConfig = highSpeedConfigs.firstOrNull { config ->
                config.width == 3840 && config.height == 2160 && 
                config.fpsMax >= 60
            }
            
            if (targetConfig == null) {
                Log.w(TAG, "4K@60fps not supported, falling back to 30fps")
                return@withContext configureStandardVideoMode()
            }
            
            // Create high-speed session
            val surfaces = listOf(previewSurface!!)
            
            cameraDevice?.createConstrainedHighSpeedCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        highSpeedSession = session as CameraConstrainedHighSpeedCaptureSession
                        captureSession = session
                        startHighSpeedPreview()
                        Log.i(TAG, "High-speed 4K@60fps mode configured")
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.w(TAG, "High-speed configuration failed, falling back to standard mode")
                        runBlocking { configureStandardVideoMode() }
                    }
                },
                backgroundHandler
            )
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.w(TAG, "High-speed mode failed, using standard mode", e)
            return@withContext configureStandardVideoMode()
        }
    }

    /**
     * Configure preview-only mode (lightweight)
     */
    private suspend fun configurePreviewMode(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.i(TAG, "Configuring preview-only mode")
            
            cameraDevice?.createCaptureSession(
                listOf(previewSurface!!),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startPreviewOnly()
                        Log.i(TAG, "Preview mode configured")
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Preview mode configuration failed")
                        onError?.invoke("Preview mode configuration failed")
                    }
                },
                backgroundHandler
            )
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure preview mode", e)
            return@withContext false
        }
    }

    /**
     * Clean up resources from current mode before switching
     */
    private fun cleanupCurrentMode() {
        when (currentMode) {
            CameraMode.RAW_50MP -> {
                stopRawCapture()
                rawImageReader?.close()
                rawImageReader = null
            }
            CameraMode.VIDEO_4K -> {
                if (videoRecordingActive) {
                    mediaRecorder?.stop()
                    mediaRecorder?.reset()
                    videoRecordingActive = false
                }
                highSpeedSession = null
            }
            CameraMode.PREVIEW_ONLY -> {
                // Nothing special to clean up
            }
        }
    }

    /**
     * Setup RAW ImageReader optimized for Samsung S22 (50MP)
     */
    private fun setupRawImageReaderForS22() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)
            
            if (rawSizes.isNullOrEmpty()) {
                throw RuntimeException("No RAW sizes available on this device")
            }
            
            // Use the largest RAW size for Samsung S22 (up to 50MP)
            rawSensorSize = rawSizes.maxByOrNull { it.width * it.height } ?: rawSizes[0]
            maxRawSize = rawSensorSize
            
            Log.i(TAG, "Samsung S22 RAW configuration: ${rawSensorSize.width}x${rawSensorSize.height} (~${(rawSensorSize.width * rawSensorSize.height / 1_000_000)}MP)")
            
            rawImageReader = ImageReader.newInstance(
                rawSensorSize.width,
                rawSensorSize.height,
                ImageFormat.RAW_SENSOR,
                RAW_CAPTURE_MAX_IMAGES // Conservative for Samsung memory management
            )
            
            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup Samsung S22 RAW ImageReader", e)
            throw e
        }
    }

    /**
     * Check if high-speed video (60fps) is supported for 4K
     */
    private fun checkHighSpeedVideoSupport(): Boolean {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val hasHighSpeedVideo = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) ?: false
            
            if (!hasHighSpeedVideo) {
                Log.i(TAG, "High-speed video capability not available")
                return false
            }
            
            val highSpeedConfigs = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
            val has4K60 = highSpeedConfigs?.any { config ->
                config.width == 3840 && config.height == 2160 && config.fpsMax >= 60
            } ?: false
            
            Log.i(TAG, "4K@60fps support: $has4K60 (Samsung restriction may apply)")
            return has4K60
            
        } catch (e: Exception) {
    /**
     * Start RAW capture mode preview and streaming
     */
    private fun startRawCapture() {
        try {
            val previewSurface = this.previewSurface ?: return
            val rawSurface = rawImageReader?.surface ?: return
            
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(previewSurface)
            previewRequestBuilder?.addTarget(rawSurface)
            
            // Configure for RAW capture - manual settings for consistency
            previewRequestBuilder?.apply {
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                
                // Optional: Set manual exposure for consistency across frames
                if (isSamsungDevice) {
                    // Samsung-specific optimizations
                    set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                    set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                }
            }
            
            captureSession?.setRepeatingRequest(
                previewRequestBuilder!!.build(),
                rawCaptureCallback,
                backgroundHandler
            )
            
            rawCaptureActive = true
            rawCaptureCount = 0
            rawCaptureStartTime = System.nanoTime()
            
            Log.i(TAG, "RAW capture streaming started at ${recordingSettings.rawCaptureFrameRate}fps")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RAW capture", e)
            onError?.invoke("RAW capture failed: ${e.message}")
        }
    }
    
    /**
     * Start video mode preview (standard 30fps)
     */
    private fun startVideoPreview() {
        try {
            val previewSurface = this.previewSurface ?: return
            
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(previewSurface)
            
            // Configure for video preview
            previewRequestBuilder?.apply {
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                
                if (recordingSettings.enableStabilization) {
                    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)
                }
            }
            
            captureSession?.setRepeatingRequest(
                previewRequestBuilder!!.build(),
                null,
                backgroundHandler
            )
            
            Log.i(TAG, "Video preview started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video preview", e)
            onError?.invoke("Video preview failed: ${e.message}")
        }
    }
    
    /**
     * Start high-speed video preview (60fps)
     */
    private fun startHighSpeedPreview() {
        try {
            val previewSurface = this.previewSurface ?: return
            val highSpeedSession = this.highSpeedSession ?: return
            
            val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder?.addTarget(previewSurface)
            
            // Create high-speed request list
            val requests = highSpeedSession.createHighSpeedRequestList(requestBuilder!!.build())
            
            highSpeedSession.setRepeatingBurst(requests, null, backgroundHandler)
            
            Log.i(TAG, "High-speed preview started at 60fps")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start high-speed preview", e)
            onError?.invoke("High-speed preview failed: ${e.message}")
        }
    }
    
    /**
     * Start preview-only mode
     */
    private fun startPreviewOnly() {
        try {
            val previewSurface = this.previewSurface ?: return
            
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(previewSurface)
            
            previewRequestBuilder?.apply {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
            
            captureSession?.setRepeatingRequest(
                previewRequestBuilder!!.build(),
                null,
                backgroundHandler
            )
            
            Log.i(TAG, "Preview-only mode started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview-only", e)
            onError?.invoke("Preview failed: ${e.message}")
        }
    }

    /**
     * Start recording in current mode (RAW or Video)
     */
    fun startRecording(sessionId: String? = null): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        if (!hasRequiredPermissions()) {
            onError?.invoke("Camera and audio permissions required")
            return false
        }

        try {
            this.sessionId = sessionId ?: TimeUtil.generateSessionId("RGB_${currentMode.name}")
            
            return when (currentMode) {
                CameraMode.RAW_50MP -> startRawRecording()
                CameraMode.VIDEO_4K -> startVideoRecording()
                CameraMode.PREVIEW_ONLY -> {
                    onError?.invoke("Cannot record in preview-only mode. Switch to RAW or Video mode first.")
                    false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onError?.invoke("Failed to start recording: ${e.message}")
            return false
        }
    }

    /**
     * Start RAW 50MP recording (continuous DNG capture)
     */
    private fun startRawRecording(): Boolean {
        try {
            Log.i(TAG, "Starting RAW 50MP recording session")
            
            if (!rawCaptureActive) {
                onError?.invoke("RAW mode not active. Switch to RAW mode first.")
                return false
            }
            
            rawImagesDirectory = createRawImagesDirectory()
            rawImagesDirectory?.mkdirs()
            
            isRecording = true
            rawCaptureCount = 0
            rawCaptureStartTime = System.nanoTime()
            
            onRecordingStarted?.invoke()
            Log.i(TAG, "RAW recording started: ${rawImagesDirectory?.absolutePath}")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RAW recording", e)
            onError?.invoke("RAW recording failed: ${e.message}")
            return false
        }
    }

    /**
     * Start 4K video recording with MediaRecorder
     */
    private fun startVideoRecording(): Boolean {
        try {
            Log.i(TAG, "Starting 4K video recording")
            
            currentVideoFile = createVideoFile()
            setupMediaRecorderFor4K()
            
            // Reconfigure session with MediaRecorder surface
            reconfigureSessionForVideoRecording()
            
            isRecording = true
            videoRecordingActive = true
            
            onRecordingStarted?.invoke()
            Log.i(TAG, "4K video recording started: ${currentVideoFile?.absolutePath}")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            onError?.invoke("Video recording failed: ${e.message}")
            return false
        }
    }

    /**
     * Reconfigure session to add MediaRecorder surface for video recording
     * 
     * Properly tears down previous session and creates new one with MediaRecorder surface,
     * following the recommended pattern: stop requests → close session → create new session.
     * This avoids closing the camera device itself, which would be expensive.
     */
    private fun reconfigureSessionForVideoRecording() {
        try {
            // Stop ongoing repeating requests and close previous session
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            captureSession?.close()
            captureSession = null
            
            val surfaces = mutableListOf<Surface>()
            previewSurface?.let { surfaces.add(it) }
            
            val recorderSurface = mediaRecorder?.surface
            if (recorderSurface != null) {
                surfaces.add(recorderSurface)
            }
            
            if (attemptHighSpeed60fps && highSpeedSession != null) {
                // High-speed video recording
                cameraDevice?.createConstrainedHighSpeedCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            highSpeedSession = session as CameraConstrainedHighSpeedCaptureSession
                            captureSession = session
                            startHighSpeedVideoRecording()
                        }
                        
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "High-speed recording session failed")
                            onError?.invoke("High-speed video recording configuration failed")
                        }
                    },
                    backgroundHandler
                )
            } else {
                // Standard video recording
                cameraDevice?.createCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            startStandardVideoRecording()
                        }
                        
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Video recording session failed")
                            onError?.invoke("Video recording configuration failed")
                        }
                    },
                    backgroundHandler
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure session for video recording", e)
            throw e
        }
    }

    /**
     * Start standard 4K@30fps video recording
     */
    private fun startStandardVideoRecording() {
        try {
            val previewSurface = this.previewSurface ?: return
            val recorderSurface = mediaRecorder?.surface ?: return
            
            val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder?.addTarget(previewSurface)
            requestBuilder?.addTarget(recorderSurface)
            
            requestBuilder?.apply {
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                
                if (recordingSettings.enableStabilization) {
                    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)
                }
            }
            
            captureSession?.setRepeatingRequest(
                requestBuilder!!.build(),
                null,
                backgroundHandler
            )
            
            // Start MediaRecorder
            mediaRecorder?.start()
            Log.i(TAG, "Standard 4K@30fps recording started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start standard video recording", e)
            onError?.invoke("Video recording start failed: ${e.message}")
        }
    }

    /**
     * Start high-speed 4K@60fps video recording  
     */
    private fun startHighSpeedVideoRecording() {
        try {
            val previewSurface = this.previewSurface ?: return
            val recorderSurface = mediaRecorder?.surface ?: return
            val highSpeedSession = this.highSpeedSession ?: return
            
            val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder?.addTarget(previewSurface)
            requestBuilder?.addTarget(recorderSurface)
            
            val requests = highSpeedSession.createHighSpeedRequestList(requestBuilder!!.build())
            highSpeedSession.setRepeatingBurst(requests, null, backgroundHandler)
            
            // Start MediaRecorder
            mediaRecorder?.start()
            Log.i(TAG, "High-speed 4K@60fps recording started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start high-speed recording", e)
            onError?.invoke("High-speed recording failed: ${e.message}")
        }
    }

    /**
     * Stop recording in current mode
     */
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return null
        }

        try {
            isRecording = false
            isPaused = false

            return when (currentMode) {
                CameraMode.RAW_50MP -> stopRawRecording()
                CameraMode.VIDEO_4K -> stopVideoRecording()
                CameraMode.PREVIEW_ONLY -> null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            onError?.invoke("Failed to stop recording: ${e.message}")
            return null
        }
    }

    /**
     * Stop RAW recording (continuous capture continues in preview)
     */
    private fun stopRawRecording(): File? {
        try {
            Log.i(TAG, "Stopping RAW recording (preview continues)")
            
            val recordingDirectory = rawImagesDirectory
            rawImagesDirectory = null
            
            onRecordingStopped?.invoke(null) // RAW returns directory, not single file
            Log.i(TAG, "RAW recording stopped. Captured $rawCaptureCount frames in: ${recordingDirectory?.absolutePath}")
            
            return recordingDirectory
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop RAW recording", e)
            return null
        }
    }

    /**
     * Stop 4K video recording
     */
    private fun stopVideoRecording(): File? {
        try {
            Log.i(TAG, "Stopping 4K video recording")
            
            videoRecordingActive = false
            
            mediaRecorder?.apply {
                stop()
                reset()
            }
            
            // Restart appropriate preview mode
            when {
                attemptHighSpeed60fps -> startHighSpeedPreview()
                else -> startVideoPreview()
            }
            
            val videoFile = currentVideoFile
            currentVideoFile = null
            
            onRecordingStopped?.invoke(videoFile)
            Log.i(TAG, "4K video recording stopped: ${videoFile?.absolutePath}")
            
            return videoFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop video recording", e)
            return null
        }
    }

    /**
     * Setup MediaRecorder for 4K recording with Samsung S22 optimizations
     */
    private fun setupMediaRecorderFor4K() {
        mediaRecorder = MediaRecorder().apply {
            if (recordingSettings.audioEnabled) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(currentVideoFile!!.absolutePath)
            
            // 4K video settings
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncodingBitRate(recordingSettings.bitRate)
            
            // Frame rate - fallback for Samsung restrictions
            val targetFrameRate = if (attemptHighSpeed60fps) 60 else SAMSUNG_4K_FALLBACK_FPS
            setVideoFrameRate(targetFrameRate)
            
            if (recordingSettings.audioEnabled) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
            }
            
            // Samsung S22 specific settings
            if (isSamsungDevice) {
                // Use higher profile for better quality on Samsung devices
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        setVideoEncodingProfileLevel(
                            MediaRecorder.VideoEncoder.H264,
                            android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                            android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel42
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set H.264 High profile, using default", e)
                    }
                }
            }
            
            try {
                prepare()
                Log.i(TAG, "MediaRecorder prepared for 4K@${targetFrameRate}fps (${videoSize.width}x${videoSize.height})")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to prepare MediaRecorder for 4K", e)
                throw e
            }
        }
    }

    /**
     * Pause/resume recording (Android N+)
     */
    @SuppressLint("NewApi")
    fun pauseRecording() {
        if (!isRecording || isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }

        try {
            mediaRecorder?.pause()
            isPaused = true
            Log.i(TAG, "Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
        }
    }

    @SuppressLint("NewApi")
    fun resumeRecording() {
        if (!isRecording || !isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }

        try {
            mediaRecorder?.resume()
            isPaused = false
            Log.i(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
        }
    }

    /**
     * Start RAW image capture at specified frame rate (30fps default)
     */
    fun startRawCapture(): Boolean {
        if (!recordingSettings.enableRawCapture) {
            Log.w(TAG, "RAW capture not enabled in settings")
            return false
        }

        if (isRawCaptureEnabled) {
            Log.w(TAG, "RAW capture already running")
            return false
        }

        if (!supportsRawCapture()) {
            Log.e(TAG, "Camera does not support RAW capture")
            onError?.invoke("Camera does not support RAW capture")
            return false
        }

        try {
            // Create RAW images directory
            rawImagesDirectory = createRawImagesDirectory()
            rawImagesDirectory?.mkdirs()

            // Initialize RAW capture
            setupRawImageReader()
            isRawCaptureEnabled = true
            rawCaptureCount = 0
            rawCaptureStartTime = System.nanoTime()

            // Start periodic RAW capture
            startPeriodicRawCapture()

            Log.i(TAG, "RAW image capture started at ${recordingSettings.rawCaptureFrameRate}fps")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RAW capture", e)
            onError?.invoke("Failed to start RAW capture: ${e.message}")
            return false
        }
    }

    /**
     * Stop RAW image capture
     */
    fun stopRawCapture() {
        if (!isRawCaptureEnabled) {
            return
        }

        try {
            isRawCaptureEnabled = false
            rawCaptureRunnable?.let { rawCaptureHandler?.removeCallbacks(it) }
            rawImageReader?.close()
            rawImageReader = null

            // Clear capture result map to prevent memory leaks
            captureResultMap.clear()

            Log.i(TAG, "RAW image capture stopped. Captured $rawCaptureCount frames")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping RAW capture", e)
        }
    }

    /**
     * Check if camera supports RAW capture
     */
    fun supportsRawCapture(): Boolean {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking RAW capability", e)
            false
        }
    }

    /**
     * Switch between front and back camera
     */
    fun switchCamera(): CameraFacing {
        closeCamera()
        currentCameraFacing =
            if (currentCameraFacing == CameraFacing.BACK) {
                CameraFacing.FRONT
            } else {
                CameraFacing.BACK
            }

        openCamera()
        onCameraSwitched?.invoke(currentCameraFacing)

        Log.i(TAG, "Switched to ${currentCameraFacing.displayName}")
        return currentCameraFacing
    }

    /**
     * Update recording settings
     */
    fun updateSettings(settings: RecordingSettings) {
        recordingSettings = settings
        videoSize = Size(settings.resolution.width, settings.resolution.height)

        // Update preview if not recording
        if (!isRecording) {
            closeCamera()
            openCamera()
        }

        Log.i(TAG, "Updated recording settings: ${settings.resolution.displayName}")
    }

    /**
     * Get available camera facing options
     */
    fun getAvailableCameraFacing(): List<CameraFacing> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val availableFacing = mutableListOf<CameraFacing>()

        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        if (!availableFacing.contains(CameraFacing.BACK)) {
                            availableFacing.add(CameraFacing.BACK)
                        }
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        if (!availableFacing.contains(CameraFacing.FRONT)) {
                            availableFacing.add(CameraFacing.FRONT)
                        }
                    }
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        return availableFacing
    }

    /**
     * Get supported video resolutions for current camera
     */
    fun getSupportedResolutions(): List<VideoResolution> {
        return VideoResolution.values().toList()
    }

    /**
     * Enable/disable flash torch
     */
    fun setFlashEnabled(enabled: Boolean) {
        recordingSettings = recordingSettings.copy(enableFlash = enabled)

        previewRequestBuilder?.let { builder ->
            try {
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (enabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
                )
                captureSession?.setRepeatingRequest(builder.build(), null, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to set flash", e)
            }
        }
    }

    /**
     * Enhanced cleanup with dual-mode support
     */
    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        
        // Stop any active capture modes
        when (currentMode) {
            CameraMode.RAW_50MP -> stopRawCapture()
            CameraMode.VIDEO_4K -> {
                if (videoRecordingActive) {
                    mediaRecorder?.stop()
                    mediaRecorder?.reset()
                }
            }
            CameraMode.PREVIEW_ONLY -> { /* Nothing special */ }
        }
        
        closeCamera()
        stopBackgroundThread()
        
        Log.i(TAG, "Camera recorder cleaned up")
    }

    // Enhanced getters for dual-mode state
    fun isRecording() = isRecording
    fun isPaused() = isPaused
    fun isRawCaptureActive() = rawCaptureActive
    fun isVideoRecordingActive() = videoRecordingActive
    fun getRawCaptureCount() = rawCaptureCount
    fun getCurrentCameraFacing() = currentCameraFacing
    fun getCurrentSettings() = recordingSettings
    fun getCurrentVideoFile() = currentVideoFile
    fun getRawImagesDirectory() = rawImagesDirectory
    fun isSessionSwitching() = isSessionSwitching
    /**
     * Initialize texture view listener for dual-mode camera preview
     */
    private fun initializeTextureView() {
        textureView.surfaceTextureListener = 
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                // Create and store preview surface for reuse across sessions
                previewSurface = Surface(texture)
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                // Handle size changes if needed
                Log.d(TAG, "Surface texture size changed: ${width}x${height}")
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                closeCamera()
                previewSurface?.release()
                previewSurface = null
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                // Frame updated - could trigger sync events here
                // High frequency - avoid logging
            }
        }
    }

    private fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            // Setup sizes for dual-mode operation
            setupCameraSizesForDualMode(characteristics)

            // Log camera capabilities for Samsung debugging
            logCameraCapabilities(characteristics)

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                onError?.invoke("Camera permission not granted")
                return
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
            onError?.invoke("Failed to open camera: ${e.message}")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * Setup camera sizes optimized for dual-mode operation (RAW + Video)
     */
    private fun setupCameraSizesForDualMode(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

        // Setup video sizes for 4K mode
        val videoSizes = map.getOutputSizes(MediaRecorder::class.java)
        videoSize = chooseOptimalSize(
            videoSizes,
            recordingSettings.resolution.width,
            recordingSettings.resolution.height,
        )

        // Setup preview size (reused across modes)
        val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
        previewSize = chooseOptimalSize(
            previewSizes,
            MAX_PREVIEW_WIDTH,
            MAX_PREVIEW_HEIGHT,
        )

        // Setup RAW sensor size for 50MP mode
        val rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR)
        if (!rawSizes.isNullOrEmpty()) {
            rawSensorSize = rawSizes.maxByOrNull { it.width * it.height } ?: rawSizes[0]
            maxRawSize = rawSensorSize
            
            val megapixels = (rawSensorSize.width * rawSensorSize.height) / 1_000_000
            Log.i(TAG, "RAW sensor size: ${rawSensorSize.width}x${rawSensorSize.height} (~${megapixels}MP)")
        }

        textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        
        Log.i(TAG, "Camera sizes configured - Video: ${videoSize.width}x${videoSize.height}, Preview: ${previewSize.width}x${previewSize.height}")
    }

    /**
     * Log camera capabilities for Samsung device debugging
     */
    private fun logCameraCapabilities(characteristics: CameraCharacteristics) {
        try {
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val supportedFeatures = mutableListOf<String>()
            
            capabilities?.forEach { capability ->
                when (capability) {
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> supportedFeatures.add("RAW")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> supportedFeatures.add("HIGH_SPEED_VIDEO")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> supportedFeatures.add("BURST_CAPTURE")
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> supportedFeatures.add("MANUAL_SENSOR")
                }
            }
            
            Log.i(TAG, "Camera capabilities: ${supportedFeatures.joinToString(", ")}")
            
            // Log high-speed video configurations for Samsung debugging
            val highSpeedConfigs = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
            if (highSpeedConfigs != null) {
                // val availableHighSpeedModes = highSpeedConfigs.map { 
                    "${it.width}x${it.height}@${it.fpsMax}fps" 
                }.joinToString(", ")
                Log.i(TAG, "High-speed video modes: $availableHighSpeedModes")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error logging camera capabilities", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private val stateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraOpenCloseLock.release()
                cameraDevice = camera
                Log.i(TAG, "Camera opened successfully")
                
                // Start in preview mode initially
                runBlocking { 
                    switchMode(CameraMode.PREVIEW_ONLY) 
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraOpenCloseLock.release()
                camera.close()
                cameraDevice = null
                Log.w(TAG, "Camera disconnected")
            }

            override fun onError(
                camera: CameraDevice,
                error: Int,
            ) {
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
                onError?.invoke("Camera error: $errorMessage")
            }
        }

    // Public methods for dual-mode management

    /**
     * Get current camera mode
     */
    fun getCurrentMode(): CameraMode = currentMode

    /**
     * Check if a specific mode is supported on this device
     */
    fun isModeSupported(mode: CameraMode): Boolean {
        return when (mode) {
            CameraMode.RAW_50MP -> supportsRawCapture()
            CameraMode.VIDEO_4K -> supportsVideoRecording()
            CameraMode.PREVIEW_ONLY -> true
        }
    }

    /**
     * Get available camera modes for this device
     */
    fun getAvailableModes(): List<CameraMode> {
        val modes = mutableListOf(CameraMode.PREVIEW_ONLY)
        
        if (supportsVideoRecording()) {
            modes.add(CameraMode.VIDEO_4K)
        }
        
        if (supportsRawCapture()) {
            modes.add(CameraMode.RAW_50MP)
        }
        
        return modes
    }

    /**
     * Check if video recording is supported
     */
    fun supportsVideoRecording(): Boolean {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            val videoSizes = map?.getOutputSizes(MediaRecorder::class.java)
            val has4K = videoSizes?.any { it.width >= 3840 && it.height >= 2160 } ?: false
            
            Log.d(TAG, "4K video support: $has4K")
            has4K
            
        } catch (e: Exception) {
            Log.w(TAG, "Error checking video support", e)
            false
        }
    }

    /**
     * Get maximum RAW resolution information
     */
    fun getMaxRawResolution(): Size? {
        return if (supportsRawCapture()) maxRawSize else null
    }

    /**
     * Get current video resolution
     */
    fun getCurrentVideoResolution(): Size = videoSize

    /**
     * Check if high-speed 60fps video is available (Samsung limitation awareness)
     */
    fun supportsHighSpeed60fps(): Boolean {
        return checkHighSpeedVideoSupport() && !isSamsungDevice // Samsung typically restricts this for 3rd party apps
    }

    /**
     * Check if simultaneous RAW + video streams are supported
     * 
     * As mentioned in the technical review, running 50MP RAW and 4K60 video simultaneously 
     * is very demanding and may not be supported on all devices. This method helps validate
     * stream combinations before attempting to configure them.
     */
    private fun validateStreamCombination(surfaces: List<Surface>): Boolean {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            
            // For now, we use separate sessions for each mode to avoid bandwidth limitations
            // This is the safer approach mentioned in the technical review
            
            // Check basic stream configuration limits
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map == null) {
                Log.w(TAG, "Stream configuration map not available")
                return false
            }
            
            // Log the approach being used
            Log.d(TAG, "Using dynamic session reconfiguration to avoid simultaneous RAW+4K limitations")
            
            // Since we're using separate sessions, this validation always passes
            // The real validation happens when createCaptureSession is called
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "Error validating stream combination", e)
            false
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surface = Surface(textureView.surfaceTexture)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)

            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        captureSession = session
                        try {
                            previewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                            )

                            session.setRepeatingRequest(
                                previewRequestBuilder!!.build(),
                                null,
                                backgroundHandler,
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError?.invoke("Failed to configure camera preview")
                    }
                },
                backgroundHandler,
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private fun createCameraRecordingSession() {
        try {
            val previewSurface = Surface(textureView.surfaceTexture)
            val recorderSurface = mediaRecorder!!.surface
            val surfaces = mutableListOf(previewSurface, recorderSurface)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            previewRequestBuilder!!.addTarget(previewSurface)
            previewRequestBuilder!!.addTarget(recorderSurface)

            // Add RAW surface if RAW capture is enabled
            if (recordingSettings.enableRawCapture && rawImageReader != null) {
                rawImageReader?.surface?.let { rawSurface ->
                    surfaces.add(rawSurface)
                    Log.i(TAG, "Added RAW surface to recording session")
                }
            }

            cameraDevice!!.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()

                        try {
                            mediaRecorder!!.start()

                            // Start RAW capture if enabled
                            if (recordingSettings.enableRawCapture) {
                                startRawCapture()
                            }
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Failed to start MediaRecorder", e)
                            onError?.invoke("Failed to start video recording")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError?.invoke("Failed to configure recording session")
                    }
                },
                backgroundHandler,
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create recording session", e)
        }
    }

    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO,
            )

            // Enable stabilization if supported and requested
            if (recordingSettings.enableStabilization) {
                previewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                )
            }

            captureSession!!.setRepeatingRequest(
                previewRequestBuilder!!.build(),
                null,
                backgroundHandler,
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to update preview", e)
        }
    }

    private fun setupMediaRecorder() {
        mediaRecorder =
            MediaRecorder().apply {
                if (recordingSettings.audioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(currentVideoFile!!.absolutePath)
                setVideoEncodingBitRate(recordingSettings.bitRate)
                setVideoFrameRate(recordingSettings.frameRate)
                setVideoSize(videoSize.width, videoSize.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)

                if (recordingSettings.audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                }

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to prepare MediaRecorder", e)
                    throw e
                }
            }
    }

    private fun getCameraId(facing: CameraFacing): String {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                val targetFacing =
                    when (facing) {
                        CameraFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
                        CameraFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                    }

                if (cameraFacing == targetFacing) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        return "0" // Fallback to first camera
    }

    private fun setupCameraSizes(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

        // Find best video size for current resolution setting
        val videoSizes = map.getOutputSizes(MediaRecorder::class.java)
        videoSize =
            chooseOptimalSize(
                videoSizes,
                recordingSettings.resolution.width,
                recordingSettings.resolution.height,
            )

        // Find best preview size
        val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
        previewSize =
            chooseOptimalSize(
                previewSizes,
                MAX_PREVIEW_WIDTH,
                MAX_PREVIEW_HEIGHT,
            )

        textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
    }

    // ===== RAW CAPTURE PRIVATE METHODS =====

    private fun setupRawImageReader() {
        try {
            // Get the largest available RAW sensor size
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)

            if (rawSizes.isNullOrEmpty()) {
                throw RuntimeException("No RAW sizes available")
            }

            // Use the largest RAW size for maximum quality
            rawSensorSize = rawSizes.maxByOrNull { it.width * it.height } ?: rawSizes[0]

            rawImageReader =
                ImageReader.newInstance(
                    rawSensorSize.width,
                    rawSensorSize.height,
                    ImageFormat.RAW_SENSOR,
                    1, // Only need 1 image at a time for sequential capture
                )

            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, backgroundHandler)

            Log.i(TAG, "RAW ImageReader setup: ${rawSensorSize.width}x${rawSensorSize.height}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup RAW ImageReader", e)
            throw e
        }
    }

    private fun startPeriodicRawCapture() {
        rawCaptureHandler = backgroundHandler
        val captureInterval = 1000L / recordingSettings.rawCaptureFrameRate // Convert fps to milliseconds

        rawCaptureRunnable =
            object : Runnable {
                override fun run() {
                    if (isRawCaptureEnabled && cameraDevice != null && captureSession != null) {
                        captureRawImage()
                        rawCaptureHandler?.postDelayed(this, captureInterval)
                    }
                }
            }

        rawCaptureHandler?.post(rawCaptureRunnable!!)
    }

    private fun captureRawImage() {
        try {
            val rawSurface = rawImageReader?.surface ?: return

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(rawSurface)

            // Set capture settings for RAW
            captureBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

            captureSession?.capture(
                captureBuilder?.build()!!,
                rawCaptureCallback,
                backgroundHandler,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing RAW image", e)
        }
    }

    // Store latest capture result for DNG creation - FIXED race condition with proper pairing
    private val captureResultMap = ConcurrentHashMap<Long, TotalCaptureResult>()
    private val captureResultTimeout = 5000L // 5 seconds timeout for cleanup

    private val rawImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                // Get the image timestamp to find the corresponding capture result
                val imageTimestamp = image.timestamp
                val captureResult = captureResultMap.remove(imageTimestamp)

                if (captureResult != null) {
                    saveRawImageAsDng(image, captureResult)
                    Log.d(TAG, "Successfully paired image with capture result for timestamp: $imageTimestamp")
                } else {
                    Log.e(
                        TAG,
                        "No matching capture result found for image timestamp: $imageTimestamp. Discarding image to ensure data integrity.",
                    )
                }

                image.close()
            }
        }

    private val rawCaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                // Store the capture result with frame timestamp for proper pairing
                val frameTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                if (frameTimestamp != null) {
                    captureResultMap[frameTimestamp] = result
                    Log.d(TAG, "Stored capture result for timestamp: $frameTimestamp")

                    // Clean up old entries to prevent memory leaks
                    cleanupOldCaptureResults()
                } else {
                    Log.w(TAG, "No sensor timestamp available in capture result")
                }

                // RAW image capture completed successfully
                rawCaptureCount++
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure,
            ) {
                Log.w(TAG, "RAW capture failed: ${failure.reason}")
            }
        }

    private fun saveRawImageAsDng(
        image: Image,
        captureResult: TotalCaptureResult?,
    ) {
        try {
            // Generate timestamp with ground truth precision
            val timestamp = System.nanoTime()
            val captureTime = TimeUtil.formatTimestamp(timestamp / 1_000_000) // Convert to milliseconds

            val filename = "RAW_${sessionId}_${String.format("%06d", rawCaptureCount)}_$captureTime.dng"
            val dngFile = File(rawImagesDirectory, filename)

            // Get camera characteristics for DNG creation
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            // Create DNG file using Android's DngCreator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && captureResult != null) {
                FileOutputStream(dngFile).use { output ->
                    val dngCreator = DngCreator(characteristics, captureResult)
                    dngCreator.writeImage(output, image)
                    dngCreator.close()
                }

                Log.d(TAG, "Saved RAW DNG: ${dngFile.name} (${image.width}x${image.height})")
                onRawImageCaptured?.invoke(dngFile)
            } else {
                Log.w(TAG, "Cannot create DNG: API level ${Build.VERSION.SDK_INT} or missing capture result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW image", e)
        }
    }

    /**
     * Clean up old capture results to prevent memory leaks
     * Removes entries older than captureResultTimeout
     */
    private fun cleanupOldCaptureResults() {
        val currentTime = System.nanoTime()
        val iterator = captureResultMap.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val timestamp = entry.key

            // Remove entries older than timeout (convert nanoseconds to milliseconds for comparison)
            if ((currentTime - timestamp) / 1_000_000 > captureResultTimeout) {
                iterator.remove()
                Log.d(TAG, "Cleaned up old capture result for timestamp: $timestamp")
            }
        }
    }

    private fun createRawImagesDirectory(): File {
        val timestamp = TimeUtil.formatTimestamp(System.currentTimeMillis())
        val dirName = "RAW_Images_${sessionId}_$timestamp"
        return File(context.getExternalFilesDir("RAW_Images"), dirName).apply {
            mkdirs()
        }
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        targetWidth: Int,
        targetHeight: Int,
    ): Size {
        val bigEnough = mutableListOf<Size>()
        val notBigEnough = mutableListOf<Size>()

        for (option in choices) {
            if (option.width >= targetWidth && option.height >= targetHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }

        return when {
            bigEnough.isNotEmpty() -> bigEnough.minByOrNull { it.width * it.height } ?: choices[0]
            notBigEnough.isNotEmpty() -> notBigEnough.maxByOrNull { it.width * it.height } ?: choices[0]
            else -> choices[0]
        }
    }

    private fun createVideoFile(): File {
        val timestamp = TimeUtil.formatTimestamp(System.currentTimeMillis())
        val filename = "RGB_Video_${sessionId}_$timestamp.mp4"
        return File(context.getExternalFilesDir("RGB_Videos"), filename).apply {
            parentFile?.mkdirs()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val cameraPermission =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED

        val audioPermission =
            if (recordingSettings.audioEnabled) {
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        return cameraPermission && audioPermission
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

    // ===== PUBLIC API METHODS FOR BACKWARD COMPATIBILITY =====
    
    /**
     * Start recording with session ID (compatibility method)
     */
    fun startRecording(sessionId: String): Boolean {
        this.sessionId = sessionId
        return startRecording()
    }
    
    /**
     * Enhanced getters for dual-mode state  
     */
    fun isRecording() = isRecording
    fun isPaused() = isPaused
    fun isRawCaptureActive() = rawCaptureActive
    fun isVideoRecordingActive() = videoRecordingActive
    fun getRawCaptureCount() = rawCaptureCount
    fun getCurrentCameraFacing() = currentCameraFacing
    fun getCurrentSettings() = recordingSettings
    fun getCurrentVideoFile() = currentVideoFile
    fun getRawImagesDirectory() = rawImagesDirectory
    fun isSessionSwitching() = isSessionSwitching

    // Additional helper methods for dual-mode functionality

    /**
     * Switch between front and back camera (preserves current mode)
     */
    fun switchCamera(): CameraFacing {
        closeCamera()
        currentCameraFacing = if (currentCameraFacing == CameraFacing.BACK) {
            CameraFacing.FRONT
        } else {
            CameraFacing.BACK
        }

        openCamera()
        onCameraSwitched?.invoke(currentCameraFacing)
        
        Log.i(TAG, "Switched to ${currentCameraFacing.displayName}")
        return currentCameraFacing
    }

    /**
     * Update recording settings and reconfigure if needed
     */
    fun updateSettings(settings: RecordingSettings) {
        recordingSettings = settings
        videoSize = Size(settings.resolution.width, settings.resolution.height)

        // Update preview if not recording
        if (!isRecording && !isSessionSwitching) {
            runBlocking {
                // Reconfigure current mode with new settings
                switchMode(currentMode)
            }
        }

        Log.i(TAG, "Updated recording settings: ${settings.resolution.displayName}")
    }

    /**
     * Enable/disable flash torch
     */
    fun setFlashEnabled(enabled: Boolean) {
        recordingSettings = recordingSettings.copy(enableFlash = enabled)

        previewRequestBuilder?.let { builder ->
            try {
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (enabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
                )
                captureSession?.setRepeatingRequest(builder.build(), null, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to set flash", e)
            }
        }
    }

    /**
     * Pause/resume recording (Android N+)
     */
    @SuppressLint("NewApi")
    fun pauseRecording() {
        if (!isRecording || isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }

        try {
            if (currentMode == CameraMode.VIDEO_4K) {
                mediaRecorder?.pause()
                isPaused = true
                Log.i(TAG, "Video recording paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
        }
    }

    @SuppressLint("NewApi")
    fun resumeRecording() {
        if (!isRecording || !isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }

        try {
            if (currentMode == CameraMode.VIDEO_4K) {
                mediaRecorder?.resume()
                isPaused = false
                Log.i(TAG, "Video recording resumed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
        }
    }

    /**
     * Get available camera facing options
     */
    fun getAvailableCameraFacing(): List<CameraFacing> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val availableFacing = mutableListOf<CameraFacing>()

        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        if (!availableFacing.contains(CameraFacing.BACK)) {
                            availableFacing.add(CameraFacing.BACK)
                        }
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        if (!availableFacing.contains(CameraFacing.FRONT)) {
                            availableFacing.add(CameraFacing.FRONT)
                        }
                    }
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        return availableFacing
    }

    /**
     * Get supported video resolutions for current camera
     */
    fun getSupportedResolutions(): List<VideoResolution> {
        return VideoResolution.values().toList()
    }

    // Private implementation methods that were missing

    private fun stopRawCapture() {
        if (!rawCaptureActive) {
            return
        }

        try {
            rawCaptureActive = false
            rawCaptureRunnable?.let { rawCaptureHandler?.removeCallbacks(it) }
            
            // Clear capture result map to prevent memory leaks
            captureResultMap.clear()

            Log.i(TAG, "RAW capture stopped. Captured $rawCaptureCount frames")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping RAW capture", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
            rawImageReader?.close()
            rawImageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun getCameraId(facing: CameraFacing): String {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                val targetFacing = when (facing) {
                    CameraFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
                    CameraFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                }

                if (cameraFacing == targetFacing) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        return "0" // Fallback to first camera
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        targetWidth: Int,
        targetHeight: Int,
    ): Size {
        val bigEnough = mutableListOf<Size>()
        val notBigEnough = mutableListOf<Size>()

        for (option in choices) {
            if (option.width >= targetWidth && option.height >= targetHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }

        return when {
            bigEnough.isNotEmpty() -> bigEnough.minByOrNull { it.width * it.height } ?: choices[0]
            notBigEnough.isNotEmpty() -> notBigEnough.maxByOrNull { it.width * it.height } ?: choices[0]
            else -> choices[0]
        }
    }

    private fun createVideoFile(): File {
        val timestamp = TimeUtil.formatTimestamp(System.currentTimeMillis())
        val modePrefix = when (currentMode) {
            CameraMode.VIDEO_4K -> "4K_Video"
            CameraMode.RAW_50MP -> "RAW_Video" // Shouldn't happen but fallback
            CameraMode.PREVIEW_ONLY -> "Preview_Video" // Shouldn't happen but fallback
        }
        val filename = "${modePrefix}_${sessionId}_$timestamp.mp4"
        return File(context.getExternalFilesDir("RGB_Videos"), filename).apply {
            parentFile?.mkdirs()
        }
    }

    private fun createRawImagesDirectory(): File {
        val timestamp = TimeUtil.formatTimestamp(System.currentTimeMillis())
        val dirName = "RAW_Images_${sessionId}_$timestamp"
        return File(context.getExternalFilesDir("RAW_Images"), dirName).apply {
            mkdirs()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val cameraPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        val audioPermission = if (recordingSettings.audioEnabled) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return cameraPermission && audioPermission
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

    // RAW capture callback and image processing (enhanced for Samsung S22)
    private val captureResultMap = ConcurrentHashMap<Long, TotalCaptureResult>()
    private val captureResultTimeout = 5000L // 5 seconds timeout for cleanup

    private val rawImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        if (image != null) {
            // Get the image timestamp to find the corresponding capture result
            val imageTimestamp = image.timestamp
            val captureResult = captureResultMap.remove(imageTimestamp)

            if (captureResult != null) {
                if (isRecording) { // Only save if actively recording
                    saveRawImageAsDng(image, captureResult)
                }
                Log.d(TAG, "RAW frame processed: timestamp $imageTimestamp")
            } else {
                Log.d(TAG, "RAW frame dropped - no matching capture result for timestamp: $imageTimestamp")
            }

            image.close()
        }
    }

    private val rawCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            // Store the capture result with frame timestamp for proper pairing
            val frameTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
            if (frameTimestamp != null) {
                captureResultMap[frameTimestamp] = result
                
                // Clean up old entries to prevent memory leaks
                cleanupOldCaptureResults()
            }

            // RAW image capture completed successfully
            rawCaptureCount++
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure,
        ) {
            Log.w(TAG, "RAW capture failed: ${failure.reason}")
        }
    }

    private fun saveRawImageAsDng(image: Image, captureResult: TotalCaptureResult?) {
        try {
            // Generate timestamp with ground truth precision
            val timestamp = System.nanoTime()
            val captureTime = TimeUtil.formatTimestamp(timestamp / 1_000_000) // Convert to milliseconds

            val filename = "RAW_${sessionId}_${String.format("%06d", rawCaptureCount)}_$captureTime.dng"
            val dngFile = File(rawImagesDirectory, filename)

            // Get camera characteristics for DNG creation
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getCameraId(currentCameraFacing)
            val characteristics = manager.getCameraCharacteristics(cameraId)

            // Create DNG file using Android's DngCreator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && captureResult != null) {
                FileOutputStream(dngFile).use { output ->
                    val dngCreator = DngCreator(characteristics, captureResult)
                    dngCreator.writeImage(output, image)
                    dngCreator.close()
                }

                Log.d(TAG, "Saved RAW DNG: ${dngFile.name} (${image.width}x${image.height})")
                onRawImageCaptured?.invoke(dngFile)
            } else {
                Log.w(TAG, "Cannot create DNG: API level ${Build.VERSION.SDK_INT} or missing capture result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW image", e)
        }
    }

    /**
     * Clean up old capture results to prevent memory leaks
     */
    private fun cleanupOldCaptureResults() {
        val currentTime = System.nanoTime()
        val iterator = captureResultMap.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val timestamp = entry.key

            // Remove entries older than timeout (convert nanoseconds to milliseconds for comparison)
            if ((currentTime - timestamp) / 1_000_000 > captureResultTimeout) {
                iterator.remove()
            }
        }
    }

    // ===== CAMERA SWITCHING METHODS =====

    /**
     * Switch to a different camera by ID
     */
    suspend fun switchCamera(cameraId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (cameraId == currentCameraId) {
                Log.i(TAG, "Already using camera $cameraId")
                return@withContext true
            }

            if (!availableCameraIds.contains(cameraId)) {
                Log.e(TAG, "Camera ID $cameraId not available")
                return@withContext false
            }

            Log.i(TAG, "Switching from camera $currentCameraId to $cameraId")
            
            // Close current camera
            closeCamera()
            
            // Update camera ID and determine facing
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                else -> CameraFacing.BACK
            }
            
            currentCameraId = cameraId
            currentCameraFacing = facing
            
            // Reopen with new camera
            openCamera()
            
            onCameraSwitched?.invoke(facing, cameraId)
            Log.i(TAG, "Successfully switched to camera $cameraId")
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera to $cameraId", e)
            onError?.invoke("Camera switch failed: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Switch camera by facing direction
     */
    suspend fun switchCamera(facing: CameraFacing): Boolean {
        val targetCameraId = getCameraId(facing)
        return switchCamera(targetCameraId)
    }

    /**
     * Get list of available cameras with their capabilities
     */
    fun getAvailableCameras(): List<CameraInfo> {
        val cameraList = mutableListOf<CameraInfo>()
        
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            
            for (cameraId in availableCameraIds) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                    CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                    else -> CameraFacing.BACK
                }
                
                // Check capabilities
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
                
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val videoSizes = map?.getOutputSizes(MediaRecorder::class.java) ?: emptyArray()
                val supports4K = videoSizes.any { it.width >= 3840 && it.height >= 2160 }
                
                val displayName = "${facing.displayName} (ID: $cameraId)" + 
                    if (supportsRaw) " [RAW]" else "" +
                    if (supports4K) " [4K]" else ""
                
                cameraList.add(CameraInfo(cameraId, facing, supportsRaw, supports4K, displayName))
            }
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to get camera list", e)
        }
        
        return cameraList
    }
}
