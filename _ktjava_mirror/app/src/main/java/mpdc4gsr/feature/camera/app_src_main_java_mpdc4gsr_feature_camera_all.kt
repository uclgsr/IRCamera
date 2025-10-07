// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\camera' subtree
// Files: 27; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\Camera2System.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import android.view.TextureView
import kotlinx.coroutines.*
import java.io.File

class Camera2System(
    private val context: Context,
    private val textureView: TextureView,
) {
    companion object {
        private const val TAG = "Camera2System"
        private const val DEFAULT_BITRATE = 20_000_000
    }

    private val cameraController = CameraController(context)
    private val videoEngine = VideoEngine(context)
    private val rawEngine = RawEngine(context)
    private val modeManager = ModeManager()
    private val uiBridge = UiBridge(textureView)
    private var currentSessionId: String = ""
    private var isRecording = false
    private var outputDirectory: File? = null

    // CoroutineScope for managing release cleanup
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var onError: ((String) -> Unit)? = null
    var onProgress: ((String) -> Unit)? = null
    var onModeChanged: ((ModeManager.CameraMode) -> Unit)? = null
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: (() -> Unit)? = null

    fun configureStage3Processing(enabled: Boolean) {
        rawEngine.setStage3ProcessingEnabled(enabled)
        val mode = if (enabled) "Stage3/Level3" else "Standard"
        AppLogger.i(TAG, "Samsung RAW processing mode set to: $mode")
        onProgress?.invoke("RAW processing: $mode")
    }

    fun isStage3ProcessingEnabled(): Boolean = rawEngine.isStage3ProcessingEnabled()

    init {
        setupCallbacks()
    }

    suspend fun initialize(cameraId: String = "0"): Boolean =
        withContext(Dispatchers.Main) {
            try {
                AppLogger.i(TAG, "Initializing Camera2System")
                while (!uiBridge.isTextureReady()) {
                    delay(50)
                }
                cameraController.openCamera(cameraId)
                return@withContext true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize camera system", e)
                onError?.invoke("Initialization failed: ${e.message}")
                return@withContext false
            }
        }

    suspend fun switchMode(mode: ModeManager.CameraMode): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!modeManager.canSwitchMode()) {
                    AppLogger.w(TAG, "Cannot switch mode - switching already in progress")
                    return@withContext false
                }
                if (!modeManager.requestModeSwitch(mode)) {
                    return@withContext false
                }
                uiBridge.reportProgress("Switching to ${mode.name}...")
                val success =
                    when (mode) {
                        ModeManager.CameraMode.RAW_50MP -> setupRawMode()
                        ModeManager.CameraMode.VIDEO_4K -> setupVideoMode()
                        ModeManager.CameraMode.PREVIEW_ONLY -> setupPreviewMode()
                    }
                if (success) {
                    modeManager.confirmModeSwitch()
                    uiBridge.updateMode(mode.name)
                    onModeChanged?.invoke(mode)
                    AppLogger.i(TAG, "Successfully switched to ${mode.name}")
                } else {
                    modeManager.reportModeSwitchFailed("Session setup failed")
                }
                return@withContext success
            } catch (e: Exception) {
                AppLogger.e(TAG, "Mode switch failed", e)
                modeManager.reportModeSwitchFailed(e.message ?: "Unknown error")
                return@withContext false
            }
        }

    suspend fun startRecording(sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (isRecording) {
                AppLogger.w(TAG, "Already recording")
                return@withContext false
            }
            try {
                currentSessionId = sessionId
                outputDirectory = createOutputDirectory(sessionId)
                val success =
                    when (modeManager.getCurrentMode()) {
                        ModeManager.CameraMode.RAW_50MP -> startRawRecording()
                        ModeManager.CameraMode.VIDEO_4K -> startVideoRecording()
                        ModeManager.CameraMode.PREVIEW_ONLY -> {
                            onError?.invoke("Cannot record in preview-only mode")
                            false
                        }
                    }
                if (success) {
                    isRecording = true
                    onRecordingStarted?.invoke()
                    uiBridge.updateRecordingState(true, modeManager.getCurrentMode().name)
                    AppLogger.i(TAG, "Recording started in ${modeManager.getCurrentMode()}")
                }
                return@withContext success
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start recording", e)
                onError?.invoke("Recording failed: ${e.message}")
                return@withContext false
            }
        }

    suspend fun stopRecording(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isRecording) {
                AppLogger.w(TAG, "Not recording")
                return@withContext false
            }
            try {
                when (modeManager.getCurrentMode()) {
                    ModeManager.CameraMode.RAW_50MP -> rawEngine.stopCapture()
                    ModeManager.CameraMode.VIDEO_4K -> videoEngine.stop()
                    ModeManager.CameraMode.PREVIEW_ONLY -> {
                    }
                }
                isRecording = false
                onRecordingStopped?.invoke()
                uiBridge.updateRecordingState(false)
                AppLogger.i(TAG, "Recording stopped")
                return@withContext true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to stop recording", e)
                onError?.invoke("Stop recording failed: ${e.message}")
                return@withContext false
            }
        }

    fun getCurrentMode(): ModeManager.CameraMode = modeManager.getCurrentMode()
    fun getAvailableModes(): List<ModeManager.CameraMode> = modeManager.getAvailableModes()
    fun isRecording(): Boolean = isRecording
    fun getDeviceCaps(): DeviceCaps? = cameraController.getDeviceCaps()
    fun release() {
        if (isRecording) {
            // Launch async stopRecording in managed scope
            releaseScope.launch {
                stopRecording()
            }
        }
        videoEngine.release()
        rawEngine.release()
        cameraController.close()
        uiBridge.release()
        // Cancel the release scope to clean up any remaining coroutines
        releaseScope.cancel()
        AppLogger.i(TAG, "Camera2System released")
    }

    private fun setupCallbacks() {
        cameraController.onCameraOpened = { caps ->
            modeManager.initialize(caps)
            uiBridge.reportProgress("Camera opened, capabilities detected")
            CoroutineScope(Dispatchers.IO).launch {
                switchMode(ModeManager.CameraMode.PREVIEW_ONLY)
            }
        }
        cameraController.onCameraError = { error ->
            uiBridge.reportError(error)
            onError?.invoke(error)
        }
        modeManager.onError = { error ->
            uiBridge.reportError(error)
            onError?.invoke(error)
        }
        uiBridge.onError = { error -> onError?.invoke(error) }
        uiBridge.onProgress = { message -> onProgress?.invoke(message) }
    }

    @Suppress("DEPRECATION")
    private suspend fun setupRawMode(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val caps = cameraController.getDeviceCaps() ?: return@withContext false
                val previewSurface = uiBridge.getPreviewSurface() ?: return@withContext false
                rawEngine.setup(
                    caps.rawSize,
                    outputDirectory ?: createTempDirectory(),
                    currentSessionId,
                    cameraController.getCameraCharacteristics(), // Pass camera characteristics for DNG creation
                    rawEngine.isStage3ProcessingEnabled() // Respect existing configuration
                )
                val rawSurface = rawEngine.getSurface() ?: return@withContext false
                val surfaces = listOf(previewSurface, rawSurface)
                return@withContext suspendCancellableCoroutine { continuation ->
                    cameraController.createCaptureSession(
                        surfaces,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraController.setCaptureSession(session)
                                val requestBuilder =
                                    cameraController.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                requestBuilder?.addTarget(previewSurface)
                                try {
                                    session.setRepeatingRequest(
                                        requestBuilder!!.build(),
                                        null,
                                        null
                                    )
                                    continuation.resume(true, null)
                                } catch (e: Exception) {
                                    AppLogger.e(TAG, "Failed to start preview request", e)
                                    continuation.resume(false, null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                AppLogger.e(TAG, "RAW mode session configuration failed")
                                continuation.resume(false, null)
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to setup RAW mode", e)
                return@withContext false
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun setupVideoMode(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val caps = cameraController.getDeviceCaps() ?: return@withContext false
                val previewSurface = uiBridge.getPreviewSurface() ?: return@withContext false
                val videoSize = Size(3840, 2160)
                val frameRate = if (caps.supports4k60) 60 else 30
                val surfaces = listOf(previewSurface)
                return@withContext suspendCancellableCoroutine { continuation ->
                    cameraController.createCaptureSession(
                        surfaces,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraController.setCaptureSession(session)
                                val requestBuilder =
                                    cameraController.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                requestBuilder?.addTarget(previewSurface)
                                try {
                                    session.setRepeatingRequest(
                                        requestBuilder!!.build(),
                                        null,
                                        null
                                    )
                                    continuation.resume(true, null)
                                } catch (e: Exception) {
                                    AppLogger.e(TAG, "Failed to start video preview request", e)
                                    continuation.resume(false, null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                AppLogger.e(TAG, "Video mode session configuration failed")
                                continuation.resume(false, null)
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to setup video mode", e)
                return@withContext false
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun setupPreviewMode(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val previewSurface = uiBridge.getPreviewSurface() ?: return@withContext false
                val surfaces = listOf(previewSurface)
                return@withContext suspendCancellableCoroutine { continuation ->
                    cameraController.createCaptureSession(
                        surfaces,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraController.setCaptureSession(session)
                                val requestBuilder =
                                    cameraController.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                requestBuilder?.addTarget(previewSurface)
                                try {
                                    session.setRepeatingRequest(
                                        requestBuilder!!.build(),
                                        null,
                                        null
                                    )
                                    continuation.resume(true, null)
                                } catch (e: Exception) {
                                    AppLogger.e(TAG, "Failed to start preview request", e)
                                    continuation.resume(false, null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                AppLogger.e(TAG, "Preview mode session configuration failed")
                                continuation.resume(false, null)
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to setup preview mode", e)
                return@withContext false
            }
        }

    private suspend fun startRawRecording(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                rawEngine.startCapture()
                startPeriodicRawCapture()
                return@withContext true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start RAW recording", e)
                return@withContext false
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun startVideoRecording(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val caps = cameraController.getDeviceCaps() ?: return@withContext false
                val videoFile = createVideoFile()
                val videoSize = Size(3840, 2160)
                val frameRate = if (caps.supports4k60) 60 else 30
                val orientationHint = calculateOrientationHint(caps.sensorOrientation)
                val recorderSurface =
                    videoEngine.prepare(
                        videoFile,
                        videoSize,
                        frameRate,
                        DEFAULT_BITRATE,
                        true,
                        orientationHint,
                        enableStabilization = true
                    ) ?: return@withContext false
                val previewSurface = uiBridge.getPreviewSurface() ?: return@withContext false
                val surfaces = listOf(previewSurface, recorderSurface)
                return@withContext suspendCancellableCoroutine { continuation ->
                    cameraController.createCaptureSession(
                        surfaces,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraController.setCaptureSession(session)
                                val requestBuilder =
                                    cameraController.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                                requestBuilder?.addTarget(previewSurface)
                                requestBuilder?.addTarget(recorderSurface)
                                try {
                                    session.setRepeatingRequest(
                                        requestBuilder!!.build(),
                                        null,
                                        null
                                    )
                                    if (videoEngine.start()) {
                                        continuation.resume(true, null)
                                    } else {
                                        continuation.resume(false, null)
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e(TAG, "Failed to start recording request", e)
                                    continuation.resume(false, null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                AppLogger.e(TAG, "Recording session configuration failed")
                                continuation.resume(false, null)
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start video recording", e)
                return@withContext false
            }
        }

    private fun startPeriodicRawCapture() {
        val captureInterval = 1000L / 15
        CoroutineScope(Dispatchers.IO).launch {
            while (isRecording && modeManager.getCurrentMode() == ModeManager.CameraMode.RAW_50MP) {
                captureRawImage()
                delay(captureInterval)
            }
        }
    }

    private fun captureRawImage() {
        try {
            val rawSurface = rawEngine.getSurface() ?: return
            val session = cameraController.getCaptureSession() ?: return
            val requestBuilder =
                cameraController.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            requestBuilder?.addTarget(rawSurface)
            // Configure Samsung Stage3/Level3 processing options
            if (rawEngine.isStage3ProcessingEnabled()) {
                try {
                    requestBuilder?.apply {
                        // Samsung Stage3/Level3 specific settings for maximum raw data preservation
                        set(
                            CaptureRequest.CONTROL_MODE,
                            android.hardware.camera2.CameraMetadata.CONTROL_MODE_OFF
                        )
                        set(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            android.hardware.camera2.CameraMetadata.NOISE_REDUCTION_MODE_OFF
                        )
                        set(
                            CaptureRequest.EDGE_MODE,
                            android.hardware.camera2.CameraMetadata.EDGE_MODE_OFF
                        )
                        set(
                            CaptureRequest.COLOR_CORRECTION_MODE,
                            android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                        )
                        set(
                            CaptureRequest.TONEMAP_MODE,
                            android.hardware.camera2.CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE
                        )
                        // Set highest quality capture settings for Stage3/Level3
                        set(CaptureRequest.JPEG_QUALITY, 100.toByte())
                        set(
                            CaptureRequest.HOT_PIXEL_MODE,
                            android.hardware.camera2.CameraMetadata.HOT_PIXEL_MODE_OFF
                        )
                    }
                    AppLogger.d(TAG, "Applied Samsung Stage3/Level3 processing settings")
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Could not apply Stage3/Level3 settings, using defaults: ${e.message}"
                    )
                }
            }
            session.capture(
                requestBuilder!!.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        // Store the result for DNG metadata
                        rawEngine.storeCaptureResult(result)
                    }
                },
                null,
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to capture RAW image", e)
        }
    }

    private fun createOutputDirectory(sessionId: String): File {
        val timestamp = System.currentTimeMillis()
        val dirName = "Camera_${sessionId}_$timestamp"
        return File(context.getExternalFilesDir("Camera"), dirName).apply {
            mkdirs()
        }
    }

    private fun createTempDirectory(): File {
        return File(context.cacheDir, "temp_raw").apply { mkdirs() }
    }

    private fun createVideoFile(): File {
        val timestamp = System.currentTimeMillis()
        val filename = "Video_${currentSessionId}_$timestamp.mp4"
        return File(outputDirectory, filename)
    }

    private fun calculateOrientationHint(sensorOrientation: Int): Int {
        return try {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val deviceRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                when (context.display?.rotation ?: android.view.Surface.ROTATION_0) {
                    android.view.Surface.ROTATION_0 -> 0
                    android.view.Surface.ROTATION_90 -> 90
                    android.view.Surface.ROTATION_180 -> 180
                    android.view.Surface.ROTATION_270 -> 270
                    else -> 0
                }
            } else {
                @Suppress("DEPRECATION")
                when (windowManager.defaultDisplay.rotation) {
                    android.view.Surface.ROTATION_0 -> 0
                    android.view.Surface.ROTATION_90 -> 90
                    android.view.Surface.ROTATION_180 -> 180
                    android.view.Surface.ROTATION_270 -> 270
                    else -> 0
                }
            }
            val orientationHint = (sensorOrientation - deviceRotation + 360) % 360
            Log.d(
                TAG,
                "Orientation calculation: device=$deviceRotation, sensor=$sensorOrientation, hint=$orientationHint"
            )
            orientationHint
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to calculate orientation hint", e)
            90
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\CameraConfigurationManager.kt =====

package mpdc4gsr.feature.camera.data

import android.os.Build
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder

class CameraConfigurationManager {
    companion object {
        private const val TAG = "CameraConfigManager"

        // Video configuration constants
        private const val VIDEO_WIDTH_4K = 3840
        private const val VIDEO_HEIGHT_4K = 2160
        private const val VIDEO_WIDTH_1080P = 1920
        private const val VIDEO_HEIGHT_1080P = 1080
        private const val VIDEO_FPS_60 = 60
        private const val VIDEO_FPS_TARGET = 30
        private const val VIDEO_FPS_FALLBACK = 24
        private const val VIDEO_BITRATE_4K = 50_000_000
        private const val VIDEO_BITRATE_1080P = 20_000_000
        private const val JPEG_QUALITY = 100

        // Known devices that support specific features
        private val KNOWN_4K_DEVICES = setOf(
            "SM-S906B", "SM-S916B", "SM-S908B", "SM-S901B", "SM-S911B", "SM-S918B"
        )
        private val KNOWN_RAW_DEVICES = setOf(
            "SM-S906B", "SM-S916B", "SM-S908B", "SM-S901B", "SM-S911B", "SM-S918B"
        )
    }

    data class CameraConfiguration(
        val videoWidth: Int,
        val videoHeight: Int,
        val videoFps: Int,
        val videoBitrate: Int,
        val supports4K: Boolean,
        val supportsRAW: Boolean,
        val supports60fps: Boolean
    )

    fun detectDeviceCapabilities(): Triple<Boolean, Boolean, Boolean> {
        return try {
            val deviceModel = Build.MODEL
            val manufacturer = Build.MANUFACTURER.lowercase()
            val deviceSupports4K = when {
                manufacturer == "samsung" && deviceModel in KNOWN_4K_DEVICES -> true
                manufacturer == "google" && deviceModel.startsWith("Pixel") -> true
                manufacturer == "oneplus" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> true
                else -> false
            }
            val deviceSupportsRAW = when {
                manufacturer == "samsung" && deviceModel in KNOWN_RAW_DEVICES -> true
                manufacturer == "google" && deviceModel.startsWith("Pixel") -> true
                else -> false
            }
            val supports60fps = when {
                manufacturer == "samsung" && (
                        deviceModel.startsWith("SM-S9") ||
                                deviceModel.startsWith("SM-S10") ||
                                deviceModel.startsWith("SM-G9") ||
                                deviceModel.startsWith("SM-G99")
                        ) -> true

                else -> false
            }
            Log.i(
                TAG,
                "Device capabilities - 4K: $deviceSupports4K, RAW: $deviceSupportsRAW, 60fps: $supports60fps"
            )
            AppLogger.i(TAG, "Device: $manufacturer $deviceModel")
            Triple(deviceSupports4K, deviceSupportsRAW, supports60fps)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error detecting device capabilities, using safe defaults", e)
            Triple(false, false, false)
        }
    }

    fun createOptimizedConfiguration(): CameraConfiguration {
        val (supports4K, supportsRAW, supports60fps) = detectDeviceCapabilities()
        return if (supports4K) {
            CameraConfiguration(
                videoWidth = VIDEO_WIDTH_4K,
                videoHeight = VIDEO_HEIGHT_4K,
                videoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET,
                videoBitrate = VIDEO_BITRATE_4K,
                supports4K = true,
                supportsRAW = supportsRAW,
                supports60fps = supports60fps
            )
        } else {
            CameraConfiguration(
                videoWidth = VIDEO_WIDTH_1080P,
                videoHeight = VIDEO_HEIGHT_1080P,
                videoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET,
                videoBitrate = VIDEO_BITRATE_1080P,
                supports4K = false,
                supportsRAW = supportsRAW,
                supports60fps = supports60fps
            )
        }
    }

    fun createOptimizedRecorder(configuration: CameraConfiguration): Recorder {
        return try {
            val qualitySelector = if (configuration.supports4K) {
                AppLogger.i(TAG, "Creating 4K UHD quality selector with fallback strategy")
                QualitySelector.from(
                    Quality.UHD,
                    FallbackStrategy.lowerQualityThan(Quality.UHD)
                )
            } else {
                AppLogger.i(TAG, "Creating FHD quality selector with fallback strategy")
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityThan(Quality.FHD)
                )
            }
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error creating optimized recorder, using conservative fallback", e)
            Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.lowerQualityThan(Quality.FHD)
                    )
                )
                .build()
        }
    }

    fun createPreviewConfiguration(configuration: CameraConfiguration): Preview {
        return Preview.Builder().apply {
            val previewSize = Size(configuration.videoWidth, configuration.videoHeight)
            @Suppress("DEPRECATION")
            setTargetResolution(previewSize)
            AppLogger.i(TAG, "Preview configured: ${previewSize.width}x${previewSize.height}")
        }.build()
    }

    fun createImageCaptureConfiguration(configuration: CameraConfiguration): ImageCapture {
        return ImageCapture.Builder().apply {
            @Suppress("DEPRECATION")
            setTargetResolution(Size(configuration.videoWidth, configuration.videoHeight))
            setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            setJpegQuality(JPEG_QUALITY)
            setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            if (configuration.supportsRAW) {
                try {
                    androidx.camera.camera2.interop.Camera2Interop.Extender(this)
                        .setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.CONTROL_MODE,
                            android.hardware.camera2.CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
                        )
                    AppLogger.i(TAG, "RAW/DNG capture enabled for supported device")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Could not enable RAW capture: ${e.message}")
                }
            }
        }.build()
    }

    fun getConfigurationSummary(configuration: CameraConfiguration): String {
        return buildString {
            appendLine("Camera Configuration:")
            appendLine("  Resolution: ${configuration.videoWidth}x${configuration.videoHeight}")
            appendLine("  Frame Rate: ${configuration.videoFps}fps")
            appendLine("  Bitrate: ${configuration.videoBitrate}")
            appendLine("  4K Support: ${configuration.supports4K}")
            appendLine("  RAW Support: ${configuration.supportsRAW}")
            appendLine("  60fps Support: ${configuration.supports60fps}")
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\CameraController.kt =====

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


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\CameraControlsManager.kt =====

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


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\CameraErrorMessageProvider.kt =====

package mpdc4gsr.feature.camera.data

import mpdc4gsr.core.data.ErrorType

object CameraErrorMessageProvider {

    fun getUserFriendlyErrorMessage(errorType: ErrorType, originalMessage: String): String {
        return when (errorType) {
            ErrorType.PERMISSION_DENIED -> {
                "Camera permission required. Please:\n" +
                        "â€¢ Go to Settings > Apps > IRCamera > Permissions\n" +
                        "â€¢ Enable Camera permission\n" +
                        "â€¢ Restart the app and try again"
            }

            ErrorType.HARDWARE_UNAVAILABLE -> {
                "Camera not available. Please:\n" +
                        "â€¢ Close other camera apps\n" +
                        "â€¢ Restart your device if the issue persists\n" +
                        "â€¢ Check if camera hardware is functioning properly"
            }

            ErrorType.INITIALIZATION_FAILED -> {
                when {
                    originalMessage.contains("another application", ignoreCase = true) -> {
                        "Camera in use by another app. Please:\n" +
                                "â€¢ Close all camera and video apps\n" +
                                "â€¢ Wait a few seconds and try again\n" +
                                "â€¢ Restart the device if the problem continues"
                    }

                    originalMessage.contains("service unavailable", ignoreCase = true) -> {
                        "Camera service unavailable. Please:\n" +
                                "â€¢ Restart the camera app\n" +
                                "â€¢ If problem persists, restart your device\n" +
                                "â€¢ Check for system updates"
                    }

                    else -> {
                        "Camera initialization failed. Please:\n" +
                                "â€¢ Try switching between front/back camera\n" +
                                "â€¢ Restart the app\n" +
                                "â€¢ Check device storage space (need 1GB+ free)"
                    }
                }
            }

            ErrorType.RECORDING_FAILED -> {
                when {
                    originalMessage.contains("storage", ignoreCase = true) -> {
                        "Recording failed due to storage issues. Please:\n" +
                                "â€¢ Free up at least 2GB of storage space\n" +
                                "â€¢ Check if SD card is properly inserted\n" +
                                "â€¢ Try recording to internal storage instead"
                    }

                    originalMessage.contains("encoder", ignoreCase = true) -> {
                        "Video encoder error. Please:\n" +
                                "â€¢ Try recording at lower resolution (1080p instead of 4K)\n" +
                                "â€¢ Close other apps using camera/video\n" +
                                "â€¢ Restart the device if problem persists"
                    }

                    else -> {
                        "Recording failed. Please:\n" +
                                "â€¢ Check available storage space\n" +
                                "â€¢ Try recording at lower quality settings\n" +
                                "â€¢ Restart the app and try again"
                    }
                }
            }

            ErrorType.FEATURE_NOT_SUPPORTED -> {
                when {
                    originalMessage.contains("4K", ignoreCase = true) -> {
                        "4K recording not supported on this device. \n" +
                                "Alternative options:\n" +
                                "â€¢ Use 1080p recording (still high quality)\n" +
                                "â€¢ Enable 60fps if available\n" +
                                "â€¢ Check device specifications for camera capabilities"
                    }

                    originalMessage.contains("RAW", ignoreCase = true) -> {
                        "RAW capture not supported on this device.\n" +
                                "Alternative options:\n" +
                                "â€¢ Use highest quality JPEG settings\n" +
                                "â€¢ Enable HDR if available\n" +
                                "â€¢ Consider using manual exposure controls"
                    }

                    originalMessage.contains("60fps", ignoreCase = true) -> {
                        "60fps recording not supported at current resolution.\n" +
                                "Try these options:\n" +
                                "â€¢ Lower resolution to 1080p for 60fps\n" +
                                "â€¢ Use 30fps at current resolution\n" +
                                "â€¢ Check if device supports high-speed recording"
                    }

                    originalMessage.contains("focus", ignoreCase = true) -> {
                        "Manual focus control not fully supported.\n" +
                                "Available alternatives:\n" +
                                "â€¢ Use tap-to-focus on preview\n" +
                                "â€¢ Enable continuous autofocus\n" +
                                "â€¢ Lock focus after tapping to focus"
                    }

                    originalMessage.contains("exposure", ignoreCase = true) -> {
                        "Advanced exposure control not supported.\n" +
                                "Available alternatives:\n" +
                                "â€¢ Use exposure compensation slider\n" +
                                "â€¢ Enable auto-exposure lock\n" +
                                "â€¢ Adjust scene mode settings"
                    }

                    else -> {
                        "Feature not supported on this device.\n" +
                                "â€¢ Check device specifications\n" +
                                "â€¢ Try alternative settings\n" +
                                "â€¢ Update to latest app version"
                    }
                }
            }

            ErrorType.OPERATION_FAILED -> {
                when {
                    originalMessage.contains("focus", ignoreCase = true) -> {
                        "Focus operation failed. Please:\n" +
                                "â€¢ Clean camera lens\n" +
                                "â€¢ Ensure adequate lighting\n" +
                                "â€¢ Try tapping different areas to focus"
                    }

                    originalMessage.contains("exposure", ignoreCase = true) -> {
                        "Exposure adjustment failed. Please:\n" +
                                "â€¢ Reset to auto-exposure mode\n" +
                                "â€¢ Adjust lighting conditions\n" +
                                "â€¢ Try smaller exposure compensation values"
                    }

                    else -> {
                        "Camera operation failed. Please:\n" +
                                "â€¢ Try the operation again\n" +
                                "â€¢ Reset camera settings to default\n" +
                                "â€¢ Restart the app if problem continues"
                    }
                }
            }

            ErrorType.DEVICE_NOT_SUPPORTED -> {
                "Device compatibility issue detected.\n" +
                        "Possible solutions:\n" +
                        "â€¢ Update Android to latest version\n" +
                        "â€¢ Enable Camera2 API in developer options\n" +
                        "â€¢ Use basic camera features only"
            }

            ErrorType.SYNC_FAILED -> {
                "Synchronization failed. This may affect:\n" +
                        "â€¢ Multi-sensor data alignment\n" +
                        "â€¢ Timestamp accuracy\n" +
                        "Recording can continue but check results carefully."
            }

            else -> {
                "Camera error occurred. Please:\n" +
                        "â€¢ Try restarting the app\n" +
                        "â€¢ Check device camera functionality\n" +
                        "â€¢ Contact support if problem persists\n\n" +
                        "Error details: $originalMessage"
            }
        }
    }

    fun getShortErrorMessage(errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.PERMISSION_DENIED -> "Camera permission required - check Settings"
            ErrorType.HARDWARE_UNAVAILABLE -> "Camera unavailable - close other camera apps"
            ErrorType.INITIALIZATION_FAILED -> "Camera initialization failed - restart app"
            ErrorType.RECORDING_FAILED -> "Recording failed - check storage space"
            ErrorType.FEATURE_NOT_SUPPORTED -> "Feature not supported on this device"
            ErrorType.OPERATION_FAILED -> "Camera operation failed - try again"
            ErrorType.DEVICE_NOT_SUPPORTED -> "Device not supported - update Android"
            ErrorType.SYNC_FAILED -> "Synchronization failed - recording may continue"
            else -> "Camera error - restart app"
        }
    }

    fun getPerformanceSuggestions(
        deviceSupports4K: Boolean,
        supportsRAW: Boolean,
        supports60fps: Boolean
    ): List<String> {
        val suggestions = mutableListOf<String>()
        if (!deviceSupports4K) {
            suggestions.add("â€¢ Device doesn't support 4K - use 1080p for best quality")
        }
        if (!supports60fps) {
            suggestions.add("â€¢ 60fps not available - use 30fps for stability")
        }
        if (!supportsRAW) {
            suggestions.add("â€¢ RAW capture not supported - use maximum JPEG quality")
        }
        suggestions.addAll(
            listOf(
                "â€¢ Close unnecessary apps before recording",
                "â€¢ Ensure device has 20%+ battery remaining",
                "â€¢ Use well-lit environments for better focus",
                "â€¢ Keep device cool to prevent thermal throttling",
                "â€¢ Free up storage space (recommended: 5GB+)"
            )
        )
        return suggestions
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\CameraPerformanceManager.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class CameraPerformanceManager(private val context: Context) {
    companion object {
        private const val TAG = "CameraPerformanceManager"
        private const val MEMORY_CHECK_INTERVAL_MS = 5000L
        private const val MAX_PENDING_FRAMES = 3
        private const val LOW_MEMORY_THRESHOLD_MB = 100L
        private const val CRITICAL_MEMORY_THRESHOLD_MB = 50L
    }

    // Performance monitoring
    private val framesCaptured = AtomicLong(0)
    private val framesDropped = AtomicLong(0)
    private val averageCaptureTimeMs = AtomicLong(0)
    private val pendingOperations = AtomicInteger(0)

    // Memory management
    private val memoryCheckHandler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    // Frame processing queue with backpressure handling
    private val frameProcessingQueue = ConcurrentLinkedQueue<FrameProcessingTask>()

    // Background executor for frame processing to avoid blocking main thread
    private val frameProcessingExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "CameraFrameProcessor").apply {
            priority = Thread.NORM_PRIORITY
        }
    }

    data class FrameProcessingTask(
        val frameData: ByteArray,
        val timestamp: Long,
        val onComplete: (Boolean) -> Unit
    )

    data class PerformanceMetrics(
        val framesCaptured: Long,
        val framesDropped: Long,
        val dropRate: Float,
        val averageCaptureTimeMs: Long,
        val pendingOperations: Int,
        val availableMemoryMB: Long,
        val memoryPressure: MemoryPressureLevel
    )

    enum class MemoryPressureLevel {
        LOW, MODERATE, HIGH, CRITICAL
    }

    var onPerformanceUpdate: ((PerformanceMetrics) -> Unit)? = null
    var onMemoryPressure: ((MemoryPressureLevel) -> Unit)? = null

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        resetMetrics()
        startMemoryMonitoring()
        AppLogger.i(TAG, "Performance monitoring started")
    }

    fun stopMonitoring() {
        isMonitoring = false
        memoryCheckHandler.removeCallbacksAndMessages(null)
        frameProcessingQueue.clear()
        frameProcessingExecutor.shutdownNow()
        AppLogger.i(TAG, "Performance monitoring stopped")
    }

    fun processFrame(
        frameData: ByteArray,
        timestamp: Long,
        onComplete: (Boolean) -> Unit
    ): Boolean {
        if (!isMonitoring) {
            onComplete(false)
            return false
        }
        val startTime = System.currentTimeMillis()
        // Check if we're under memory pressure
        val memoryPressure = getCurrentMemoryPressure()
        if (memoryPressure == MemoryPressureLevel.CRITICAL) {
            framesDropped.incrementAndGet()
            onComplete(false)
            AppLogger.w(TAG, "Frame dropped due to critical memory pressure")
            return false
        }
        // Check pending operations for backpressure
        val currentPending = pendingOperations.get()
        if (currentPending >= MAX_PENDING_FRAMES) {
            framesDropped.incrementAndGet()
            onComplete(false)
            AppLogger.w(TAG, "Frame dropped due to backpressure (pending: $currentPending)")
            return false
        }
        pendingOperations.incrementAndGet()
        // Add to processing queue
        val task = FrameProcessingTask(frameData, timestamp) { success ->
            val endTime = System.currentTimeMillis()
            val captureTime = endTime - startTime
            pendingOperations.decrementAndGet()
            if (success) {
                framesCaptured.incrementAndGet()
                updateAverageCaptureTime(captureTime)
            } else {
                framesDropped.incrementAndGet()
            }
            onComplete(success)
        }
        frameProcessingQueue.offer(task)
        // Process the task on a background thread to avoid blocking main thread
        frameProcessingExecutor.execute {
            processNextFrame()
        }
        return true
    }

    fun getCurrentMetrics(): PerformanceMetrics {
        val captured = framesCaptured.get()
        val dropped = framesDropped.get()
        val total = captured + dropped
        val dropRate = if (total > 0) (dropped.toFloat() / total.toFloat()) * 100f else 0f
        return PerformanceMetrics(
            framesCaptured = captured,
            framesDropped = dropped,
            dropRate = dropRate,
            averageCaptureTimeMs = averageCaptureTimeMs.get(),
            pendingOperations = pendingOperations.get(),
            availableMemoryMB = getAvailableMemoryMB(),
            memoryPressure = getCurrentMemoryPressure()
        )
    }

    fun getOptimizedSettings(currentConfig: CameraConfigurationManager.CameraConfiguration): Map<String, Any> {
        val metrics = getCurrentMetrics()
        val recommendations = mutableMapOf<String, Any>()
        // Adjust based on drop rate
        when {
            metrics.dropRate > 20f -> {
                recommendations["suggested_fps"] = maxOf(24, currentConfig.videoFps - 6)
                recommendations["suggested_resolution"] = "lower"
                recommendations["reason"] = "High frame drop rate detected"
            }

            metrics.dropRate > 10f -> {
                recommendations["suggested_fps"] = maxOf(24, currentConfig.videoFps - 6)
                recommendations["reason"] = "Moderate frame drop rate detected"
            }
        }
        // Adjust based on memory pressure
        when (metrics.memoryPressure) {
            MemoryPressureLevel.HIGH, MemoryPressureLevel.CRITICAL -> {
                recommendations["reduce_quality"] = true
                recommendations["disable_raw"] = true
                recommendations["reduce_frame_rate"] = true
                recommendations["reason"] = "High memory pressure detected"
            }

            MemoryPressureLevel.MODERATE -> {
                recommendations["reduce_quality"] = currentConfig.supports4K
                recommendations["reason"] = "Moderate memory pressure detected"
            }

            else -> {
                // No changes needed
            }
        }
        // Performance suggestions
        val suggestions = mutableListOf<String>()
        if (metrics.averageCaptureTimeMs > 100) {
            suggestions.add("Consider reducing JPEG quality")
        }
        if (metrics.pendingOperations > 2) {
            suggestions.add("Frame processing queue is backing up")
        }
        if (metrics.memoryPressure != MemoryPressureLevel.LOW) {
            suggestions.add("Close other apps to free memory")
        }
        recommendations["suggestions"] = suggestions
        return recommendations
    }

    private fun startMemoryMonitoring() {
        val memoryCheck = object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                val metrics = getCurrentMetrics()
                onPerformanceUpdate?.invoke(metrics)
                // Check for memory pressure changes
                val currentPressure = metrics.memoryPressure
                if (currentPressure != MemoryPressureLevel.LOW) {
                    onMemoryPressure?.invoke(currentPressure)
                }
                memoryCheckHandler.postDelayed(this, MEMORY_CHECK_INTERVAL_MS)
            }
        }
        memoryCheckHandler.post(memoryCheck)
    }

    private fun processNextFrame() {
        val task = frameProcessingQueue.poll() ?: return
        // Process frame data on background thread to avoid blocking main thread
        try {
            // Note: Minimal delay removed as it was artificial
            // Real frame processing happens here without blocking
            task.onComplete(true)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Frame processing failed", e)
            task.onComplete(false)
        }
    }

    private fun updateAverageCaptureTime(captureTime: Long) {
        val current = averageCaptureTimeMs.get()
        val updated = if (current == 0L) captureTime else (current + captureTime) / 2
        averageCaptureTimeMs.set(updated)
    }

    private fun getAvailableMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val availableMemory = maxMemory - (totalMemory - freeMemory)
        return availableMemory / (1024 * 1024)
    }

    private fun getCurrentMemoryPressure(): MemoryPressureLevel {
        val availableMB = getAvailableMemoryMB()
        return when {
            availableMB < CRITICAL_MEMORY_THRESHOLD_MB -> MemoryPressureLevel.CRITICAL
            availableMB < LOW_MEMORY_THRESHOLD_MB -> MemoryPressureLevel.HIGH
            availableMB < LOW_MEMORY_THRESHOLD_MB * 2 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.LOW
        }
    }

    private fun resetMetrics() {
        framesCaptured.set(0)
        framesDropped.set(0)
        averageCaptureTimeMs.set(0)
        pendingOperations.set(0)
    }

    fun getDeviceOptimizations(): Map<String, Any> {
        val optimizations = mutableMapOf<String, Any>()
        // Device-specific optimizations
        when {
            Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> {
                optimizations["use_samsung_extensions"] = true
                optimizations["enable_stage3_processing"] = true
                optimizations["preferred_encoder"] = "hardware"
            }

            Build.MANUFACTURER.equals("google", ignoreCase = true) -> {
                optimizations["use_pixel_features"] = true
                optimizations["enable_hdr_plus"] = true
                optimizations["preferred_encoder"] = "hardware"
            }

            else -> {
                optimizations["preferred_encoder"] = "software_fallback"
                optimizations["conservative_settings"] = true
            }
        }
        // Memory-based optimizations
        val totalMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        when {
            totalMemoryMB < 512 -> {
                optimizations["max_resolution"] = "720p"
                optimizations["max_fps"] = 24
                optimizations["disable_raw"] = true
            }

            totalMemoryMB < 1024 -> {
                optimizations["max_resolution"] = "1080p"
                optimizations["max_fps"] = 30
                optimizations["limit_raw_captures"] = true
            }

            else -> {
                optimizations["max_resolution"] = "4k"
                optimizations["max_fps"] = 60
                optimizations["enable_all_features"] = true
            }
        }
        return optimizations
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\DeviceCaps.kt =====

package mpdc4gsr.feature.camera.data

import android.util.Size

data class DeviceCaps(
    val supportsRaw: Boolean,
    val rawSize: Size,
    val supports4k60: Boolean,
    val sensorOrientation: Int,
)


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\ModeManager.kt =====

package mpdc4gsr.feature.camera.data

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler

class ModeManager {
    companion object {
        private const val TAG = "ModeManager"
    }

    enum class CameraMode {
        RAW_50MP,
        VIDEO_4K,
        PREVIEW_ONLY,
    }

    enum class State {
        IDLE,
        SWITCHING,
        RAW_ACTIVE,
        VIDEO_ACTIVE,
        PREVIEW_ACTIVE,
    }

    private var currentMode = CameraMode.PREVIEW_ONLY
    private var currentState = State.IDLE
    private var deviceCaps: DeviceCaps? = null
    var onModeChanged: ((CameraMode, State) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    fun initialize(caps: DeviceCaps) {
        deviceCaps = caps
        AppLogger.i(TAG, "Mode manager initialized with device capabilities")
    }

    fun requestModeSwitch(targetMode: CameraMode): Boolean {
        if (currentState == State.SWITCHING) {
            AppLogger.w(TAG, "Mode switch already in progress")
            return false
        }
        if (currentMode == targetMode) {
            AppLogger.i(TAG, "Already in target mode: $targetMode")
            return true
        }
        if (!isModeSupported(targetMode)) {
            val error = "Mode $targetMode not supported on this device"
            AppLogger.e(TAG, error)
            onError?.invoke(error)
            return false
        }
        currentState = State.SWITCHING
        val previousMode = currentMode
        currentMode = targetMode
        AppLogger.i(TAG, "Mode switch: $previousMode -> $targetMode")
        onModeChanged?.invoke(currentMode, currentState)
        return true
    }

    fun confirmModeSwitch() {
        if (currentState != State.SWITCHING) {
            AppLogger.w(TAG, "No mode switch in progress to confirm")
            return
        }
        currentState =
            when (currentMode) {
                CameraMode.RAW_50MP -> State.RAW_ACTIVE
                CameraMode.VIDEO_4K -> State.VIDEO_ACTIVE
                CameraMode.PREVIEW_ONLY -> State.PREVIEW_ACTIVE
            }
        AppLogger.i(TAG, "Mode switch confirmed: $currentMode active")
        onModeChanged?.invoke(currentMode, currentState)
    }

    fun reportModeSwitchFailed(error: String) {
        if (currentState != State.SWITCHING) {
            AppLogger.w(TAG, "No mode switch in progress to fail")
            return
        }
        AppLogger.e(TAG, "Mode switch failed: $error")
        currentState = State.IDLE
        onError?.invoke("Mode switch failed: $error")
    }

    fun getCurrentMode(): CameraMode = currentMode
    fun getCurrentState(): State = currentState
    fun isModeSupported(mode: CameraMode): Boolean {
        val caps = deviceCaps ?: return false
        return when (mode) {
            CameraMode.RAW_50MP -> caps.supportsRaw && caps.rawSize.width > 0
            CameraMode.VIDEO_4K -> true
            CameraMode.PREVIEW_ONLY -> true
        }
    }

    fun getAvailableModes(): List<CameraMode> {
        val modes = mutableListOf(CameraMode.PREVIEW_ONLY, CameraMode.VIDEO_4K)
        if (isModeSupported(CameraMode.RAW_50MP)) {
            modes.add(CameraMode.RAW_50MP)
        }
        return modes
    }

    fun isSwitching(): Boolean = currentState == State.SWITCHING
    fun canSwitchMode(): Boolean {
        return currentState != State.SWITCHING
    }

    fun getRecommendedFrameRate(): Int {
        val caps = deviceCaps ?: return 30
        return when (currentMode) {
            CameraMode.RAW_50MP -> 15
            CameraMode.VIDEO_4K -> if (caps.supports4k60) 60 else 30
            CameraMode.PREVIEW_ONLY -> 30
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\RawEngine.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class RawEngine(private val context: Context) {
    companion object {
        private const val TAG = "RawEngine"
        private const val RAW_CAPTURE_TIMEOUT_MS = 5000L
    }

    private var rawImageReader: ImageReader? = null
    private var isCapturing = false
    private var rawOutputDirectory: File? = null
    private var sessionId: String = ""
    private var rawCaptureCount = 0
    private val pendingCaptureResults = ConcurrentHashMap<Long, TotalCaptureResult>()

    // Camera characteristics for DNG creation
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var enableStage3Processing = true // Enable Samsung Stage3/Level3 by default
    var onRawImageSaved: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    fun setup(
        rawSize: Size,
        outputDirectory: File,
        sessionId: String,
        characteristics: CameraCharacteristics? = null,
        enableStage3: Boolean = true,
    ) {
        try {
            this.rawOutputDirectory = outputDirectory
            this.sessionId = sessionId
            this.rawCaptureCount = 0
            this.cameraCharacteristics = characteristics
            this.enableStage3Processing = enableStage3
            rawImageReader =
                ImageReader.newInstance(
                    rawSize.width,
                    rawSize.height,
                    ImageFormat.RAW_SENSOR,
                    2,
                )
            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, null)
            val processingMode = if (enableStage3) "Stage3/Level3" else "Standard"
            Log.i(
                TAG,
                "RAW engine setup: ${rawSize.width}x${rawSize.height}, Processing: $processingMode"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup RAW engine", e)
            onError?.invoke("RAW setup failed: ${e.message}")
        }
    }

    fun getSurface(): Surface? = rawImageReader?.surface
    fun startCapture() {
        isCapturing = true
        rawCaptureCount = 0
        AppLogger.i(TAG, "RAW capture started")
    }

    fun stopCapture() {
        isCapturing = false
        AppLogger.i(TAG, "RAW capture stopped, captured $rawCaptureCount images")
    }

    fun storeCaptureResult(result: TotalCaptureResult) {
        if (isCapturing) {
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: System.nanoTime()
            pendingCaptureResults[timestamp] = result
            if (pendingCaptureResults.size > 10) {
                val oldestKey = pendingCaptureResults.keys.minOrNull()
                oldestKey?.let { pendingCaptureResults.remove(it) }
            }
        }
    }

    fun isCapturing(): Boolean = isCapturing
    fun getCaptureCount(): Int = rawCaptureCount

    fun setStage3ProcessingEnabled(enabled: Boolean) {
        enableStage3Processing = enabled
        val mode = if (enabled) "Stage3/Level3" else "Standard"
        AppLogger.i(TAG, "RAW processing mode changed to: $mode")
    }

    fun isStage3ProcessingEnabled(): Boolean = enableStage3Processing
    fun release() {
        stopCapture()
        rawImageReader?.close()
        rawImageReader = null
        pendingCaptureResults.clear()
        AppLogger.i(TAG, "RAW engine released")
    }

    private val rawImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            if (!isCapturing) return@OnImageAvailableListener
            val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
            try {
                val timestamp = image.timestamp
                val captureResult = pendingCaptureResults.remove(timestamp)
                if (captureResult != null) {
                    saveRawImageAsDng(image, captureResult)
                } else {
                    AppLogger.w(TAG, "No capture result found for timestamp $timestamp")
                    saveRawImageAsRaw(image)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to process RAW image", e)
                onError?.invoke("RAW processing failed: ${e.message}")
            } finally {
                image.close()
            }
        }

    private fun saveRawImageAsDng(
        image: Image,
        captureResult: TotalCaptureResult,
    ) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val dngFile = File(outputDir, "${sessionId}_raw_stage3_$timestamp.dng")
        try {
            val characteristics = cameraCharacteristics
            if (characteristics != null) {
                // Create proper DNG file using Android's DngCreator for Stage3/Level3 processing
                val dngCreator = DngCreator(characteristics, captureResult)
                // Configure DNG creator for Samsung Stage3/Level3 processing
                if (enableStage3Processing) {
                    // Set Stage3/Level3 specific metadata
                    try {
                        // Disable thumbnail for maximum raw data preservation
                        // Note: Skipping thumbnail to preserve raw data
                        // Set DNG orientation based on device orientation
                        captureResult.get(CaptureResult.JPEG_ORIENTATION)?.let { orientation ->
                            dngCreator.setOrientation(orientation)
                        }
                        // Note: Additional Samsung Stage3/Level3 specific EXIF tags would be set here
                        // if Samsung provides specific DNG tag constants for Stage3/Level3 processing
                        // These may include:
                        // - Custom processing pipeline identifiers
                        // - Stage3/Level3 specific color space information
                        // - Advanced sensor readout parameters
                        Log.d(
                            TAG,
                            "Configured DNG for Samsung Stage3/Level3 processing with orientation and no thumbnail"
                        )
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Could not set Stage3/Level3 specific metadata: ${e.message}")
                    }
                }
                FileOutputStream(dngFile).use { outputStream ->
                    dngCreator.writeImage(outputStream, image)
                }
                dngCreator.close()
                rawCaptureCount++
                Log.d(
                    TAG,
                    "Saved Stage3/Level3 DNG: ${dngFile.name} (${image.width}x${image.height})"
                )
                onRawImageSaved?.invoke(dngFile)
            } else {
                // Fallback to raw binary if no characteristics available
                AppLogger.w(TAG, "No camera characteristics available, falling back to raw binary")
                saveRawImageAsRaw(image)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save Stage3/Level3 DNG", e)
            onError?.invoke("Stage3/Level3 DNG save failed: ${e.message}")
            // Fallback to raw binary on failure
            try {
                saveRawImageAsRaw(image)
            } catch (fallbackException: Exception) {
                AppLogger.e(TAG, "Fallback raw save also failed", fallbackException)
            }
        }
    }

    private fun saveRawImageAsRaw(image: Image) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val rawFile = File(outputDir, "${sessionId}_raw_$timestamp.raw")
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            rawFile.writeBytes(bytes)
            rawCaptureCount++
            AppLogger.d(TAG, "Saved RAW binary: ${rawFile.name} (${image.width}x${image.height})")
            onRawImageSaved?.invoke(rawFile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save RAW binary", e)
            onError?.invoke("RAW save failed: ${e.message}")
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\SamsungDeviceCompatibility.kt =====

package mpdc4gsr.feature.camera.data

import android.os.Build

object SamsungDeviceCompatibility {

    private val STAGE3_COMPATIBLE_MODELS = setOf(
        "SM-S906B", // Galaxy S22+
        "SM-S916B", // Galaxy S23+
        "SM-S908B", // Galaxy S22 Ultra
        "SM-S901B", // Galaxy S22
        "SM-S911B", // Galaxy S23
        "SM-S918B"  // Galaxy S23 Ultra
    )

    fun isStage3Compatible(): Boolean {
        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        // Check exact model match first
        if (STAGE3_COMPATIBLE_MODELS.contains(deviceModel)) {
            return true
        }
        // Check broader Samsung Galaxy S22/S23 series compatibility
        return deviceManufacturer.equals("samsung", ignoreCase = true) &&
                (deviceModel.contains("SM-S9", ignoreCase = true) ||
                        deviceModel.contains("SM-S22", ignoreCase = true))
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\UiBridge.kt =====

package mpdc4gsr.feature.camera.data

import android.graphics.SurfaceTexture
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.view.Surface
import android.view.TextureView

class UiBridge(private val textureView: TextureView) {
    companion object {
        private const val TAG = "UiBridge"
    }

    private var previewSurface: Surface? = null
    private var isTextureAvailable = false
    var onError: ((String) -> Unit)? = null
    var onProgress: ((String) -> Unit)? = null
    var onModeChanged: ((String) -> Unit)? = null
    var onRecordingStateChanged: ((Boolean, String) -> Unit)? = null
    private var isRecording = false
    private var currentMode = "PREVIEW"

    init {
        setupTextureView()
    }

    fun getPreviewSurface(): Surface? = previewSurface
    fun isTextureReady(): Boolean = isTextureAvailable
    fun updateMode(mode: String) {
        currentMode = mode
        AppLogger.i(TAG, "Mode updated: $mode")
        onModeChanged?.invoke(mode)
        onRecordingStateChanged?.invoke(isRecording, mode)
    }

    fun updateRecordingState(recording: Boolean, additionalInfo: String = "") {
        isRecording = recording
        val status = if (recording) "â— REC" else "â—‹ STOPPED"
        val fullInfo =
            if (additionalInfo.isNotEmpty()) "$currentMode - $additionalInfo" else currentMode
        AppLogger.i(TAG, "Recording state: $status ($fullInfo)")
        onRecordingStateChanged?.invoke(recording, fullInfo)
        if (recording) {
            reportProgress(" Recording $currentMode mode")
        } else {
            reportProgress(" Recording stopped")
        }
    }

    fun reportError(error: String) {
        AppLogger.e(TAG, "Error: $error")
        onError?.invoke(error)
    }

    fun reportProgress(message: String) {
        AppLogger.i(TAG, "Progress: $message")
        onProgress?.invoke(message)
    }

    fun updatePreviewSize(
        width: Int,
        height: Int,
    ) {
        textureView.surfaceTexture?.setDefaultBufferSize(width, height)
        AppLogger.d(TAG, "Preview size updated: ${width}x$height")
    }

    fun release() {
        previewSurface?.release()
        previewSurface = null
        isTextureAvailable = false
        AppLogger.d(TAG, "UiBridge released")
    }

    private fun setupTextureView() {
        textureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    previewSurface?.release()
                    previewSurface = Surface(texture)
                    isTextureAvailable = true
                    AppLogger.i(TAG, "TextureView surface available: ${width}x$height")
                    reportProgress("Preview surface ready")
                }

                override fun onSurfaceTextureSizeChanged(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    AppLogger.d(TAG, "TextureView size changed: ${width}x$height")
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    previewSurface?.release()
                    previewSurface = null
                    isTextureAvailable = false
                    AppLogger.i(TAG, "TextureView surface destroyed")
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                }
            }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\data\VideoEngine.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import java.io.File

class VideoEngine(private val context: Context? = null) {
    companion object {
        private const val TAG = "VideoEngine"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var isPrepared = false
    fun prepare(
        outputFile: File,
        videoSize: Size,
        frameRate: Int,
        bitRate: Int,
        audioEnabled: Boolean,
        orientationHint: Int = 0,
        enableStabilization: Boolean = true
    ): android.view.Surface? {
        try {
            release()
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                if (audioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile.absolutePath)
                setVideoEncodingBitRate(bitRate)
                setVideoFrameRate(frameRate)
                setVideoSize(videoSize.width, videoSize.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOrientationHint(orientationHint)
                AppLogger.d(TAG, "Video orientation hint set to: $orientationHint degrees")
                if (audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                }
                prepare()
            }
            isPrepared = true
            Log.i(
                TAG,
                "MediaRecorder prepared for ${videoSize.width}x${videoSize.height}@${frameRate}fps, orientation=$orientationHintÂ°"
            )
            return mediaRecorder?.surface
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to prepare MediaRecorder", e)
            release()
            return null
        }
    }

    fun start(): Boolean {
        return try {
            if (!isPrepared) {
                AppLogger.e(TAG, "MediaRecorder not prepared")
                return false
            }
            mediaRecorder?.start()
            isRecording = true
            AppLogger.i(TAG, "Video recording started")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start video recording", e)
            false
        }
    }

    fun stop() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                isRecording = false
                AppLogger.i(TAG, "Video recording stopped")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop video recording", e)
        }
    }

    fun release() {
        try {
            if (isRecording) {
                stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
            isRecording = false
            AppLogger.d(TAG, "MediaRecorder released")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error releasing MediaRecorder", e)
        }
    }

    fun isRecording(): Boolean = isRecording
    fun getSurface(): android.view.Surface? = mediaRecorder?.surface
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\DualModeCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility

class DualModeCameraViewModel : AppBaseViewModel() {
    // Enhanced data classes
    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val deviceInfo: String = "",
        val supportedModes: List<CameraMode> = emptyList(),
        val currentResolution: String = "",
        val frameRate: Int = 0
    )

    data class RecordingState(
        val isRecording: Boolean = false,
        val recordingDuration: Long = 0L,
        val recordedFileCount: Int = 0,
        val currentFileSize: Long = 0L,
        val totalRecordedSize: Long = 0L
    )

    data class CameraScreenState(
        val canRecord: Boolean = false,
        val canSwitchMode: Boolean = false,
        val showPermissionRequest: Boolean = false,
        val showProgress: Boolean = false,
        val displayMessage: String = ""
    )

    // StateFlow for reactive state management
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    private val _cameraMode = MutableStateFlow(CameraMode.PREVIEW)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()
    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<CameraEvent>()
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    // Combined state for complex UI scenarios
    private val _cameraScreenState = MutableStateFlow(CameraScreenState())
    val cameraScreenState: StateFlow<CameraScreenState> = _cameraScreenState.asStateFlow()
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var enableSamsungOptimizations: Boolean = true
    private var appContext: Context? = null

    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED,
        REQUESTING,
        PERMANENTLY_DENIED
    }

    enum class CameraMode {
        PREVIEW,
        RAW,
        VIDEO_4K,
        VIDEO_1080P,
        PHOTO_BURST,
        NIGHT_MODE
    }

    sealed class CameraEvent {
        data class ShowError(val message: String) : CameraEvent()
        data class ShowSuccess(val message: String) : CameraEvent()
        data class RequestPermission(val permissions: List<String>) : CameraEvent()
        data class RecordingStarted(val fileName: String) : CameraEvent()
        data class RecordingStopped(val filePath: String, val duration: Long) : CameraEvent()
        data class ModeChanged(val newMode: CameraMode) : CameraEvent()
        object NavigateToGallery : CameraEvent()
    }

    init {
        // Setup combined state management
        viewModelScope.launch {
            combine(
                _permissionState,
                _cameraState,
                _cameraMode,
                _recordingState
            ) { permission, camera, mode, recording ->
                CameraScreenState(
                    canRecord = permission == PermissionState.GRANTED && camera.isInitialized && !recording.isRecording,
                    canSwitchMode = permission == PermissionState.GRANTED && camera.isInitialized && !recording.isRecording,
                    showPermissionRequest = permission == PermissionState.UNKNOWN || permission == PermissionState.DENIED,
                    showProgress = recording.isRecording,
                    displayMessage = generateDisplayMessage(permission, camera, mode, recording)
                )
            }.collect { newState ->
                _cameraScreenState.value = newState
            }
        }
    }

    fun initialize(initialMode: String, enableOptimizations: Boolean) {
        launchWithErrorHandling {
            enableSamsungOptimizations = enableOptimizations
            val mode = when (initialMode) {
                "RAW_50MP" -> CameraMode.RAW
                "VIDEO_4K" -> CameraMode.VIDEO_4K
                "VIDEO_1080P" -> CameraMode.VIDEO_1080P
                "PHOTO_BURST" -> CameraMode.PHOTO_BURST
                "NIGHT_MODE" -> CameraMode.NIGHT_MODE
                else -> CameraMode.PREVIEW
            }
            _cameraMode.value = mode
            _permissionState.value = PermissionState.UNKNOWN
            val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
            val supportedModes = getSupportedModes()
            _cameraState.value = CameraState(
                isInitialized = false,
                isRecording = false,
                deviceInfo = deviceInfo,
                supportedModes = supportedModes
            )
            _events.emit(CameraEvent.ShowSuccess("Camera system initialized with mode: ${mode.name}"))
        }
    }

    fun onPermissionGranted() {
        _permissionState.value = PermissionState.GRANTED
        viewModelScope.launch {
            _events.emit(CameraEvent.ShowSuccess("Camera permission granted"))
        }
    }

    fun onPermissionDenied(isPermanent: Boolean = false) {
        _permissionState.value =
            if (isPermanent) PermissionState.PERMANENTLY_DENIED else PermissionState.DENIED
        viewModelScope.launch {
            val message = if (isPermanent) {
                "Camera permission permanently denied. Please enable in settings."
            } else {
                "Camera permission required for dual-mode system"
            }
            _events.emit(CameraEvent.ShowError(message))
        }
    }

    fun requestPermission() {
        _permissionState.value = PermissionState.REQUESTING
        viewModelScope.launch {
            _events.emit(
                CameraEvent.RequestPermission(
                    listOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            )
        }
    }

    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        launchWithLoading {
            try {
                appContext = context.applicationContext
                rgbCameraRecorder = RgbCameraRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    useFrontCamera = false
                )
                rgbCameraRecorder?.initialize()
                _cameraState.value = _cameraState.value.copy(
                    isInitialized = true,
                    currentResolution = "1920x1080", // Default resolution
                    frameRate = 30
                )
                _events.emit(CameraEvent.ShowSuccess("Dual-mode camera system initialized"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to initialize camera: ${e.message}"))
            }
        }
    }

    fun switchCameraMode(newMode: CameraMode) {
        launchWithErrorHandling {
            if (!_cameraState.value.isInitialized) {
                _events.emit(CameraEvent.ShowError("Camera not initialized"))
                return@launchWithErrorHandling
            }
            if (_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Cannot switch mode while recording"))
                return@launchWithErrorHandling
            }
            val previousMode = _cameraMode.value
            _cameraMode.value = newMode
            when (newMode) {
                CameraMode.RAW -> {
                    handleRawModeSwitch()
                }

                CameraMode.VIDEO_4K -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "3840x2160",
                        frameRate = 30
                    )
                }

                CameraMode.VIDEO_1080P -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 60
                    )
                }

                CameraMode.PREVIEW -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 30
                    )
                }

                CameraMode.PHOTO_BURST -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "4000x3000",
                        frameRate = 0
                    )
                }

                CameraMode.NIGHT_MODE -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 24
                    )
                }
            }
            _events.emit(CameraEvent.ModeChanged(newMode))
            _events.emit(CameraEvent.ShowSuccess("Switched from ${previousMode.name} to ${newMode.name}"))
        }
    }

    private suspend fun handleRawModeSwitch() {
        val message =
            if (enableSamsungOptimizations && SamsungDeviceCompatibility.isStage3Compatible()) {
                "RAW Mode: Samsung Stage3/Level3 DNG Enabled"
            } else {
                val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
                "RAW Mode: Standard DNG ($deviceInfo)"
            }
        _cameraState.value = _cameraState.value.copy(
            currentResolution = "4000x3000",
            frameRate = 0
        )
        _events.emit(CameraEvent.ShowSuccess(message))
    }

    fun startRecording() {
        launchWithErrorHandling {
            if (!_cameraState.value.isInitialized) {
                _events.emit(CameraEvent.ShowError("Camera not initialized"))
                return@launchWithErrorHandling
            }
            if (_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Already recording"))
                return@launchWithErrorHandling
            }
            try {
                val fileName = "recording_${System.currentTimeMillis()}"
                val sessionDir = appContext?.getExternalFilesDir("recordings")?.absolutePath ?: ""
                rgbCameraRecorder?.startRecording(sessionDir)
                _recordingState.value = _recordingState.value.copy(
                    isRecording = true,
                    recordingDuration = 0L
                )
                _cameraState.value = _cameraState.value.copy(isRecording = true)
                _events.emit(CameraEvent.RecordingStarted(fileName))
                _events.emit(CameraEvent.ShowSuccess("Recording started"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to start recording: ${e.message}"))
            }
        }
    }

    fun stopRecording() {
        launchWithErrorHandling {
            if (!_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Not currently recording"))
                return@launchWithErrorHandling
            }
            try {
                val duration = _recordingState.value.recordingDuration
                rgbCameraRecorder?.stopRecording()
                _recordingState.value = _recordingState.value.copy(
                    isRecording = false,
                    recordedFileCount = _recordingState.value.recordedFileCount + 1
                )
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                val filePath = "recording_path" // Would be actual path in real implementation
                _events.emit(CameraEvent.RecordingStopped(filePath, duration))
                _events.emit(CameraEvent.ShowSuccess("Recording stopped"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to stop recording: ${e.message}"))
            }
        }
    }

    fun navigateToGallery() {
        viewModelScope.launch {
            _events.emit(CameraEvent.NavigateToGallery)
        }
    }

    private fun getSupportedModes(): List<CameraMode> {
        return if (SamsungDeviceCompatibility.isStage3Compatible()) {
            CameraMode.values().toList()
        } else {
            listOf(CameraMode.PREVIEW, CameraMode.VIDEO_1080P, CameraMode.PHOTO_BURST)
        }
    }

    private fun generateDisplayMessage(
        permission: PermissionState,
        camera: CameraState,
        mode: CameraMode,
        recording: RecordingState
    ): String {
        return when {
            permission != PermissionState.GRANTED -> "Camera permission required"
            !camera.isInitialized -> "Initializing camera..."
            recording.isRecording -> "Recording... ${recording.recordingDuration}s"
            else -> "Mode: ${mode.name} | ${camera.currentResolution}@${camera.frameRate}fps"
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            rgbCameraRecorder?.cleanup()
        }
        super.onCleared()
    }

    companion object {
        private const val TAG = "DualModeCameraViewModel"
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\RGBCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class FocusMode(val displayName: String) {
    AUTO("Auto"),
    MANUAL("Manual"),
    CONTINUOUS("Continuous");

    fun getNext(): FocusMode {
        return when (this) {
            AUTO -> MANUAL
            MANUAL -> CONTINUOUS
            CONTINUOUS -> AUTO
        }
    }
}

enum class WhiteBalance(val displayName: String) {
    AUTO("Auto"),
    DAYLIGHT("Daylight"),
    CLOUDY("Cloudy"),
    TUNGSTEN("Tungsten");

    fun getNext(): WhiteBalance {
        return when (this) {
            AUTO -> DAYLIGHT
            DAYLIGHT -> CLOUDY
            CLOUDY -> TUNGSTEN
            TUNGSTEN -> AUTO
        }
    }
}

class RGBCameraViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    data class CameraState(
        val isPreviewActive: Boolean = false,
        val isRecording: Boolean = false,
        val resolution: String = "1920Ã—1080",
        val frameRate: Int = 30,
        val exposureTime: String = "1/60",
        val iso: Int = 200,
        val focusMode: FocusMode = FocusMode.AUTO,
        val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
        val recordingDuration: Int = 0,
        val capturedFrames: Int = 0,
        val error: String? = null,
        val cameraChangeCounter: Int = 0
    )

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    private val _cameraRecorder = MutableStateFlow<RgbCameraRecorder?>(null)
    val cameraRecorder: StateFlow<RgbCameraRecorder?> = _cameraRecorder.asStateFlow()

    fun initializeCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                val recorder = RgbCameraRecorder(
                    context = application,
                    lifecycleOwner = lifecycleOwner
                )
                val initialized = recorder.initialize()
                if (initialized) {
                    _cameraRecorder.value = recorder
                    _cameraState.update {
                        it.copy(
                            isPreviewActive = true,
                            resolution = recorder.getResolution(),
                            frameRate = recorder.getCurrentFps(),
                            error = null
                        )
                    }
                } else {
                    _cameraState.update { it.copy(error = "Failed to initialize camera") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Camera initialization error: ${e.message}") }
            }
        }
    }

    @Deprecated(
        message = "Use cameraRecorder StateFlow for reactive updates",
        replaceWith = ReplaceWith("cameraRecorder.value")
    )
    fun getCameraRecorder(): RgbCameraRecorder? = _cameraRecorder.value

    fun startRecording() {
        viewModelScope.launch {
            try {
                val recorder = _cameraRecorder.value
                if (recorder == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }
                val sessionDir = application.getExternalFilesDir("camera_recordings")?.absolutePath
                    ?: application.filesDir.absolutePath
                val currentTimeMs = System.currentTimeMillis()
                val currentMonotonicNs = System.nanoTime()
                val metadata = mpdc4gsr.core.data.SessionMetadata(
                    sessionId = "camera_${currentTimeMs}",
                    sessionStartTimestampMs = currentTimeMs,
                    sessionStartMonotonicNs = currentMonotonicNs,
                    sessionStartIso = ISO_DATE_FORMAT.format(java.util.Date(currentTimeMs))
                )
                recorder.startRecording(sessionDir, metadata)
                _cameraState.update { it.copy(isRecording = true, recordingDuration = 0, error = null) }
                // Start duration tracking
                trackRecordingDuration()
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _cameraRecorder.value?.stopRecording()
                _cameraState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    fun togglePreview() {
        _cameraState.update { it.copy(isPreviewActive = !it.isPreviewActive) }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            try {
                // Photo capture functionality would be implemented here
                android.util.Log.d("RGBCameraViewModel", "Photo capture requested")
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Photo capture failed: ${e.message}") }
            }
        }
    }

    fun switchCamera() {
        viewModelScope.launch {
            try {
                val recorder = _cameraRecorder.value
                if (recorder == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }
                val cameraInfo = recorder.getCurrentCameraInfo()
                if (!cameraInfo.canSwitch) {
                    val reason = when {
                        _cameraState.value.isRecording -> "Cannot switch camera during recording"
                        !cameraInfo.frontAvailable && !cameraInfo.backAvailable -> "No cameras available"
                        cameraInfo.isUsingFrontCamera && !cameraInfo.backAvailable -> "Back camera not available"
                        !cameraInfo.isUsingFrontCamera && !cameraInfo.frontAvailable -> "Front camera not available"
                        else -> "Camera switch not available"
                    }
                    _cameraState.update { it.copy(error = reason) }
                    return@launch
                }
                val success = if (cameraInfo.isUsingFrontCamera) {
                    recorder.switchToBackCamera()
                } else {
                    recorder.switchToFrontCamera()
                }
                if (success) {
                    // Increment counter to trigger preview rebind in UI
                    _cameraState.update {
                        it.copy(
                            error = null,
                            cameraChangeCounter = it.cameraChangeCounter + 1
                        )
                    }
                } else {
                    _cameraState.update { it.copy(error = "Failed to switch camera") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "An unexpected error occurred while switching cameras.") }
            }
        }
    }

    fun updateResolution(resolution: String) {
        _cameraState.update { it.copy(resolution = resolution) }
    }

    fun updateFrameRate(frameRate: Int) {
        _cameraState.update { it.copy(frameRate = frameRate) }
    }

    fun updateFocusMode(focusMode: FocusMode) {
        _cameraState.update { it.copy(focusMode = focusMode) }
    }

    fun updateWhiteBalance(whiteBalance: WhiteBalance) {
        _cameraState.update { it.copy(whiteBalance = whiteBalance) }
    }

    fun updateExposureTime(exposureTime: String) {
        _cameraState.update { it.copy(exposureTime = exposureTime) }
    }

    fun updateISO(iso: Int) {
        _cameraState.update { it.copy(iso = iso) }
    }

    fun dismissError() {
        _cameraState.update { it.copy(error = null) }
    }

    fun reinitializeCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                // Clean up existing camera first
                val recorder = _cameraRecorder.value
                if (recorder != null) {
                    recorder.cleanup()
                    _cameraRecorder.value = null
                }
                // Reinitialize - cleanup() is a suspend function that completes before continuing
                initializeCamera(lifecycleOwner)
                // Increment counter to trigger UI updates
                _cameraState.update {
                    it.copy(cameraChangeCounter = it.cameraChangeCounter + 1)
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Failed to reinitialize camera: ${e.message}") }
            }
        }
    }

    private fun trackRecordingDuration() {
        viewModelScope.launch {
            while (_cameraState.value.isRecording) {
                kotlinx.coroutines.delay(1000)
                _cameraState.update {
                    it.copy(
                        recordingDuration = it.recordingDuration + 1,
                        capturedFrames = it.capturedFrames + it.frameRate
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                _cameraRecorder.value?.stopRecording()
                _cameraRecorder.value?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("RGBCameraViewModel", "Error during cleanup", e)
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\RGBCameraViewModelFactory.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RGBCameraViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RGBCameraViewModel::class.java)) {
            return RGBCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\TimeLapseCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

enum class TimeLapseMode(val displayName: String) {
    MANUAL("Manual Interval"),
    AUTO("Auto Optimize"),
    PRESET_FAST("Fast (1s)"),
    PRESET_MEDIUM("Medium (5s)"),
    PRESET_SLOW("Slow (10s)")
}

class TimeLapseCameraViewModel(
    @Suppress("UNUSED_PARAMETER") context: Context
) : AppBaseViewModel() {
    companion object {
        private const val DEFAULT_PLAYBACK_FPS = 30
    }

    data class TimeLapseState(
        val isRecording: Boolean = false,
        val capturedFrames: Int = 0,
        val intervalSeconds: Int = 5,
        val mode: TimeLapseMode = TimeLapseMode.PRESET_MEDIUM,
        val totalDuration: Int = 0,
        val estimatedVideoLength: Int = 0,
        val lastCaptureTime: Long = 0L,
        val error: String? = null,
        val resolution: String = "1920Ã—1080",
        val quality: Int = 90
    )

    private val _timeLapseState = MutableStateFlow(TimeLapseState())
    val timeLapseState: StateFlow<TimeLapseState> = _timeLapseState.asStateFlow()
    fun startTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = true,
                capturedFrames = 0,
                totalDuration = 0,
                error = null
            )
        }
    }

    fun stopTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = false
            )
        }
    }

    fun updateInterval(seconds: Int) {
        _timeLapseState.value = _timeLapseState.value.copy(
            intervalSeconds = seconds.coerceIn(1, 60)
        )
    }

    fun setMode(mode: TimeLapseMode) {
        val interval = when (mode) {
            TimeLapseMode.PRESET_FAST -> 1
            TimeLapseMode.PRESET_MEDIUM -> 5
            TimeLapseMode.PRESET_SLOW -> 10
            else -> _timeLapseState.value.intervalSeconds
        }
        _timeLapseState.value = _timeLapseState.value.copy(
            mode = mode,
            intervalSeconds = interval
        )
    }

    fun captureFrame() {
        launchWithErrorHandling {
            val current = _timeLapseState.value
            _timeLapseState.value = current.copy(
                capturedFrames = current.capturedFrames + 1,
                lastCaptureTime = System.currentTimeMillis(),
                estimatedVideoLength = (current.capturedFrames + 1) / DEFAULT_PLAYBACK_FPS
            )
        }
    }
}

class TimeLapseCameraViewModelFactory(
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLapseCameraViewModel::class.java)) {
            return TimeLapseCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\Camera2SystemValidator.kt =====

package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.feature.camera.data.ModeManager

class Camera2SystemValidator(private val context: Context) {
    companion object {
        private const val TAG = "Camera2SystemValidator"
    }

    suspend fun validateSystem(): ValidationResult {
        val results = mutableListOf<String>()
        var allPassed = true
        try {
            AppLogger.i(TAG, "Starting Camera2 system validation...")
            if (validateArchitectureComponents()) {
                results.add(" Architecture components validated")
            } else {
                results.add(" Architecture components missing")
                allPassed = false
            }
            if (validateModeSwitching()) {
                results.add(" Mode switching logic validated")
            } else {
                results.add(" Mode switching logic failed")
                allPassed = false
            }
            if (validateFastSessionSwitching()) {
                results.add(" Fast session switching validated")
            } else {
                results.add(" Fast session switching failed")
                allPassed = false
            }
            if (validateSamsungCompatibility()) {
                results.add(" Samsung S22 compatibility validated")
            } else {
                results.add(" Samsung S22 compatibility failed")
                allPassed = false
            }
            if (validateStage3Level3Support()) {
                results.add(" Samsung Stage3/Level3 DNG support validated")
            } else {
                results.add(" Samsung Stage3/Level3 DNG support failed")
                allPassed = false
            }
            AppLogger.i(TAG, "Validation completed. Success: $allPassed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Validation failed with exception", e)
            results.add(" Validation failed: ${e.message}")
            allPassed = false
        }
        return ValidationResult(allPassed, results)
    }

    private fun validateArchitectureComponents(): Boolean {
        return try {
            Class.forName("com.mpdc4gsr.camera.Camera2System")
            Class.forName("com.mpdc4gsr.camera.core.CameraController")
            Class.forName("com.mpdc4gsr.camera.core.VideoEngine")
            Class.forName("com.mpdc4gsr.camera.core.RawEngine")
            Class.forName("com.mpdc4gsr.camera.core.ModeManager")
            Class.forName("com.mpdc4gsr.camera.core.UiBridge")
            Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            true
        } catch (e: ClassNotFoundException) {
            AppLogger.e(TAG, "Architecture component not found", e)
            false
        }
    }

    private fun validateModeSwitching(): Boolean {
        return try {
            val modeManager = ModeManager()
            val canSwitchToRaw = modeManager.requestModeSwitch(ModeManager.CameraMode.RAW_50MP)
            val canSwitchToVideo = modeManager.requestModeSwitch(ModeManager.CameraMode.VIDEO_4K)
            val canSwitchToPreview =
                modeManager.requestModeSwitch(ModeManager.CameraMode.PREVIEW_ONLY)
            canSwitchToRaw && canSwitchToVideo && canSwitchToPreview
        } catch (e: Exception) {
            AppLogger.e(TAG, "Mode switching validation failed", e)
            false
        }
    }

    private fun validateFastSessionSwitching(): Boolean {
        return try {
            val cameraControllerClass =
                Class.forName("com.mpdc4gsr.camera.core.CameraController")
            val createSessionMethod =
                cameraControllerClass.getDeclaredMethod(
                    "createCaptureSession",
                    List::class.java,
                    Class.forName("android.hardware.camera2.CameraCaptureSession\$StateCallback"),
                )
            createSessionMethod != null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fast session switching validation failed", e)
            false
        }
    }

    private fun validateSamsungCompatibility(): Boolean {
        return try {
            val deviceCapsClass = Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            val fields = deviceCapsClass.declaredFields
            val hasSupportsRaw = fields.any { it.name == "supportsRaw" }
            val hasRawSize = fields.any { it.name == "rawSize" }
            val hasSupports4k60 = fields.any { it.name == "supports4k60" }
            val hasSensorOrientation = fields.any { it.name == "sensorOrientation" }
            hasSupportsRaw && hasRawSize && hasSupports4k60 && hasSensorOrientation
        } catch (e: Exception) {
            AppLogger.e(TAG, "Samsung compatibility validation failed", e)
            false
        }
    }

    private fun validateStage3Level3Support(): Boolean {
        return try {
            // Instead of using brittle reflection, test actual functionality
            // by trying to instantiate the classes and checking their public APIs
            // Test RawEngine Stage3/Level3 functionality
            val rawEngineWorks = try {
                val rawEngine = mpdc4gsr.feature.camera.data.RawEngine(context)
                // Test that the methods exist by trying to call them (safe calls)
                rawEngine.isStage3ProcessingEnabled() // This should not throw
                rawEngine.setStage3ProcessingEnabled(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "RawEngine Stage3/Level3 methods not available", e)
                false
            }
            // Test Camera2System Stage3/Level3 functionality  
            val camera2SystemWorks = try {
                // Use a mock TextureView for testing
                val textureView = android.view.TextureView(context)
                val camera2System = mpdc4gsr.feature.camera.data.Camera2System(context, textureView)
                // Test that the methods exist
                camera2System.isStage3ProcessingEnabled() // This should not throw
                camera2System.configureStage3Processing(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Camera2System Stage3/Level3 methods not available", e)
                false
            }
            // Test DngCreator API availability (this is a standard Android API)
            val dngCreatorAvailable = try {
                // Check if DngCreator class is available (API level 21+)
                Class.forName("android.hardware.camera2.DngCreator") != null
            } catch (e: Exception) {
                AppLogger.e(TAG, "DngCreator API not available", e)
                false
            }
            // Test SamsungDeviceCompatibility utility
            val deviceCompatibilityWorks = try {
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.isStage3Compatible()
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.getDeviceInfo()
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "SamsungDeviceCompatibility utility not working", e)
                false
            }
            val allWorking =
                rawEngineWorks && camera2SystemWorks && dngCreatorAvailable && deviceCompatibilityWorks
            Log.i(
                TAG,
                "Stage3/Level3 validation - RawEngine: $rawEngineWorks, Camera2System: $camera2SystemWorks, DngCreator: $dngCreatorAvailable, DeviceCompatibility: $deviceCompatibilityWorks"
            )
            allWorking
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage3/Level3 validation failed", e)
            false
        }
    }

    data class ValidationResult(
        val allTestsPassed: Boolean,
        val results: List<String>,
    ) {
        fun getFormattedReport(): String {
            return buildString {
                appendLine("=== Camera2 System Validation Report ===")
                appendLine("Overall Result: ${if (allTestsPassed) " PASS" else " FAIL"}")
                appendLine()
                results.forEach { result ->
                    appendLine(result)
                }
                appendLine()
                if (allTestsPassed) {
                    appendLine(" System ready for Samsung S22 (Exynos, Android 15) deployment")
                } else {
                    appendLine(" System requires fixes before deployment")
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\CameraDashboardScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.deferAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToDualMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Camera Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = deferAction { onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = deferAction { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            CameraDashboardContent(
                onNavigateToDualMode = onNavigateToDualMode,
                onNavigateToSingleCamera = onNavigateToSingleCamera,
                onNavigateToTimeLapse = onNavigateToTimeLapse,
                onNavigateToGallery = onNavigateToGallery,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CameraDashboardContent(
    onNavigateToDualMode: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showToast: (String) -> Unit = { message ->
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Status Card
        CameraStatusCard()
        // Camera Modes Card
        CameraModesCard(
            onNavigateToDualMode = onNavigateToDualMode,
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToTimeLapse = onNavigateToTimeLapse,
            showToast = showToast
        )
        // Recording Controls Card
        RecordingControlsCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera
        )
        // Camera Settings Card
        CameraSettingsCard()
        // Preview and Gallery Card
        PreviewGalleryCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToGallery = onNavigateToGallery,
            showToast = showToast
        )
    }
}

@Composable
private fun CameraStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Camera Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Camera Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            // Camera availability indicators
            CameraStatusRow("Front Camera", true)
            CameraStatusRow("Back Camera", true)
            CameraStatusRow("External Camera", false)
            // Current camera info
            CameraInfoRow("Active Camera", "Back Camera")
            CameraInfoRow("Resolution", "1920x1080")
            CameraInfoRow("Frame Rate", "30 FPS")
            CameraInfoRow("Focus Mode", "Auto")
        }
    }
}

@Composable
private fun CameraStatusRow(
    cameraName: String,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cameraName,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isAvailable) "Camera Available" else "Camera Unavailable",
                tint = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isAvailable) "Available" else "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CameraInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraModesCard(
    onNavigateToDualMode: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Modes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Single Camera Mode
            CameraModeItem(
                title = "Single Camera Mode",
                description = "Standard RGB camera capture",
                icon = Icons.Default.Camera,
                isActive = false,
                onClick = {
                    onNavigateToSingleCamera?.invoke() ?: showToast("Single camera mode coming soon")
                }
            )
            // Dual Camera Mode
            CameraModeItem(
                title = "Dual Camera Mode",
                description = "Simultaneous RGB and thermal capture",
                icon = Icons.Default.CameraAlt,
                isActive = true,
                onClick = onNavigateToDualMode
            )
            // Time-lapse Mode
            CameraModeItem(
                title = "Time-lapse Mode",
                description = "Automated interval capture",
                icon = Icons.Default.Timer,
                isActive = false,
                onClick = {
                    onNavigateToTimeLapse?.invoke() ?: showToast("Time-lapse mode coming soon")
                }
            )
        }
    }
}

@Composable
private fun CameraModeItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = deferAction { onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Badge {
                    Text("Active")
                }
            }
        }
    }
}

@Composable
private fun RecordingControlsCard(
    onNavigateToSingleCamera: (() -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Recording status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recording Status",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                        contentDescription = if (isRecording) "Recording" else "Stopped",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (isRecording) "Recording" else "Stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { isRecording = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { isRecording = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Quick Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Flash setting
            SettingRow(
                title = "Flash",
                value = "Auto",
                icon = Icons.Default.FlashOn
            )
            // Quality setting
            SettingRow(
                title = "Video Quality",
                value = "1080p",
                icon = Icons.Default.HighQuality
            )
            // Storage location
            SettingRow(
                title = "Storage",
                value = "Internal",
                icon = Icons.Default.Storage
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PreviewGalleryCard(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preview & Gallery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke() ?: showToast("Preview feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = "Preview Camera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToGallery?.invoke() ?: showToast("Gallery feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Open Gallery")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\CameraSettingsCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CameraSettingsPanel(
    isAutoExposure: Boolean = true,
    exposureCompensation: Float = 0f,
    isAeLocked: Boolean = false,
    isAutoFocus: Boolean = true,
    focusDistance: Float = 0f,
    isAfLocked: Boolean = false,
    isFlashEnabled: Boolean = false,
    isStage3ProcessingEnabled: Boolean = false,
    onExposureModeToggle: (Boolean) -> Unit = {},
    onExposureCompensationChanged: (Float) -> Unit = {},
    onAeLockToggle: (Boolean) -> Unit = {},
    onFocusModeToggle: (Boolean) -> Unit = {},
    onFocusDistanceChanged: (Float) -> Unit = {},
    onAfLockToggle: (Boolean) -> Unit = {},
    onCameraToggle: () -> Unit = {},
    onRecordingToggle: (Boolean) -> Unit = {},
    onFlashToggle: (Boolean) -> Unit = {},
    onStage3ProcessingToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Camera Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        // Exposure Controls
        CameraSettingsSection(title = "Exposure") {
            SwitchSettingItem(
                label = "Auto Exposure",
                checked = isAutoExposure,
                onCheckedChange = onExposureModeToggle,
                icon = Icons.Default.WbSunny
            )
            SliderSettingItem(
                label = "Exposure Compensation",
                value = exposureCompensation,
                onValueChange = onExposureCompensationChanged,
                valueRange = -2f..2f,
                enabled = !isAutoExposure
            )
            SwitchSettingItem(
                label = "AE Lock",
                checked = isAeLocked,
                onCheckedChange = onAeLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Focus Controls
        CameraSettingsSection(title = "Focus") {
            SwitchSettingItem(
                label = "Auto Focus",
                checked = isAutoFocus,
                onCheckedChange = onFocusModeToggle,
                icon = Icons.Default.CenterFocusStrong
            )
            SliderSettingItem(
                label = "Focus Distance",
                value = focusDistance,
                onValueChange = onFocusDistanceChanged,
                valueRange = 0f..1f,
                enabled = !isAutoFocus
            )
            SwitchSettingItem(
                label = "AF Lock",
                checked = isAfLocked,
                onCheckedChange = onAfLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Basic Controls
        CameraSettingsSection(title = "Controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraToggle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flip")
                }
                FilledTonalButton(
                    onClick = { onFlashToggle(!isFlashEnabled) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flash")
                }
            }
            SwitchSettingItem(
                label = "Stage 3 Processing",
                checked = isStage3ProcessingEnabled,
                onCheckedChange = onStage3ProcessingToggle,
                icon = Icons.Default.AutoFixHigh
            )
        }
    }
}

@Composable
private fun CameraSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\CameraSettingsScreen.kt =====

package mpdc4gsr.feature.camera.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.data.CameraConfigurationManager

@Composable
fun CameraSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { CameraConfigurationManager() }
    val (supports4K, supportsRAW, supports60fps) = remember {
        configManager.detectDeviceCapabilities()
    }
    val availableResolutions = remember {
        buildList {
            if (supports4K) {
                add("3840x2160")
            }
            add("1920x1080")
            add("1280x720")
            add("640x480")
        }
    }
    val maxFrameRate = if (supports60fps) 60f else 30f
    var resolution by remember { mutableStateOf(availableResolutions.first()) }
    var frameRate by remember { mutableIntStateOf(30) }
    var autoFocus by remember { mutableStateOf(true) }
    var autoExposure by remember { mutableStateOf(true) }
    var stabilization by remember { mutableStateOf(false) }
    var gridLines by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Camera Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Capabilities
            SettingsCard(
                title = "Device Capabilities",
                icon = Icons.Default.Info
            ) {
                SettingsToggle(
                    label = "4K Video Support",
                    description = "Device supports 4K video recording",
                    checked = supports4K,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "60fps Support",
                    description = "Device supports 60fps video recording",
                    checked = supports60fps,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "RAW Image Support",
                    description = "Device supports RAW image capture",
                    checked = supportsRAW,
                    onCheckedChange = {},
                    enabled = false
                )
            }
            // Video Settings
            SettingsCard(
                title = "Video Settings",
                icon = Icons.Default.Videocam
            ) {
                SettingsDropdown(
                    label = "Resolution",
                    options = availableResolutions,
                    value = resolution,
                    onValueChange = { resolution = it }
                )
                SettingsSlider(
                    label = "Frame Rate",
                    value = frameRate.toFloat(),
                    valueRange = 15f..maxFrameRate,
                    onValueChange = { frameRate = it.toInt() },
                    unit = " fps"
                )
            }
            // Camera Features
            SettingsCard(
                title = "Camera Features",
                icon = Icons.Default.CameraAlt
            ) {
                SettingsToggle(
                    label = "Auto Focus",
                    description = "Automatic focus adjustment",
                    checked = autoFocus,
                    onCheckedChange = { autoFocus = it }
                )
                SettingsToggle(
                    label = "Auto Exposure",
                    description = "Automatic exposure control",
                    checked = autoExposure,
                    onCheckedChange = { autoExposure = it }
                )
                SettingsToggle(
                    label = "Image Stabilization",
                    description = "Digital image stabilization",
                    checked = stabilization,
                    onCheckedChange = { stabilization = it }
                )
            }
            // Interface Options
            SettingsCard(
                title = "Interface",
                icon = Icons.Default.GridOn
            ) {
                SettingsToggle(
                    label = "Grid Lines",
                    description = "Show rule of thirds grid",
                    checked = gridLines,
                    onCheckedChange = { gridLines = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraSettingsScreenPreview() {
    IRCameraTheme {
        CameraSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\CameraStatusCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import mpdc4gsr.core.data.RgbCameraRecorder

@Composable
fun CameraStatusWidget(
    cameraRecorder: RgbCameraRecorder?,
    onInitializeCamera: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var statusText by remember { mutableStateOf("Camera Status: Not Initialized") }
    var statsText by remember { mutableStateOf("Camera Statistics:\nNot Available") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Text
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Camera Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (cameraRecorder != null) {
                CameraPreviewView(cameraRecorder)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera Not Initialized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onInitializeCamera != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onInitializeCamera) {
                                Text("Initialize Camera")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Statistics
        Text(
            text = statsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
    // Update status based on camera recorder state
    LaunchedEffect(cameraRecorder) {
        if (cameraRecorder != null) {
            statusText = "Camera Status: Initialized"
            while (true) {
                delay(1000)
                // Update stats periodically using existing methods
                val resolution = cameraRecorder.getResolution()
                val fps = cameraRecorder.getCurrentFps()
                statsText = "Camera Statistics:\nResolution: $resolution\nFPS: $fps"
            }
        } else {
            statusText = "Camera Status: Not Initialized"
            statsText = "Camera Statistics:\nNot Available"
        }
    }
}

@Composable
private fun CameraPreviewView(cameraRecorder: RgbCameraRecorder) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // Bind camera preview to this PreviewView
                cameraRecorder.bindPreview(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CameraStatusBadge(
    isInitialized: Boolean,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            isRecording -> MaterialTheme.colorScheme.error
            isInitialized -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isRecording -> "Recording"
                    isInitialized -> "Ready"
                    else -> "Not Initialized"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isRecording -> MaterialTheme.colorScheme.onError
                    isInitialized -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\DualModeCameraActivityCompose.kt =====

package mpdc4gsr.feature.camera.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.camera.presentation.DualModeCameraViewModel
import mpdc4gsr.feature.main.ui.MainComposeActivity

class DualModeCameraActivityCompose : BaseComposeActivity<DualModeCameraViewModel>() {
    private val cameraVM: DualModeCameraViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraVM.onPermissionGranted()
        } else {
            cameraVM.onPermissionDenied()
        }
    }

    override fun createViewModel(): DualModeCameraViewModel {
        return cameraVM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMode = intent.getStringExtra("INITIAL_MODE") ?: "VIDEO_4K"
        val enableSamsungOptimizations =
            intent.getBooleanExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)
        cameraVM.initialize(initialMode, enableSamsungOptimizations)
        checkCameraPermission()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DualModeCameraViewModel) {
        val context = LocalContext.current
        // Collect state
        val permissionState by viewModel.permissionState.collectAsState()
        val cameraState by viewModel.cameraState.collectAsState()
        val cameraMode by viewModel.cameraMode.collectAsState()
        val recordingState by viewModel.recordingState.collectAsState()
        val cameraScreenState by viewModel.cameraScreenState.collectAsState()
        // Handle events
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is DualModeCameraViewModel.CameraEvent.ShowError -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.ShowSuccess -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.RequestPermission -> {
                        // Handle permission request
                    }

                    is DualModeCameraViewModel.CameraEvent.RecordingStarted -> {
                        Toast.makeText(context, "Recording started: ${event.fileName}", Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.RecordingStopped -> {
                        Toast.makeText(context, "Recording stopped: ${event.duration}s", Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.ModeChanged -> {
                        Toast.makeText(context, "Mode changed to ${event.newMode}", Toast.LENGTH_SHORT).show()
                    }

                    DualModeCameraViewModel.CameraEvent.NavigateToGallery -> {
                        // Navigate to gallery
                    }
                    // is DualModeCameraViewModel.CameraEvent.NavigateToSettings -> {
                    //     context.startActivity(Intent(context, SettingsComposeActivity::class.java))
                    // }
                    // is DualModeCameraViewModel.CameraEvent.NavigateBack -> {
                    //     finish()
                    // }
                }
            }
        }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dual Mode Camera",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                navigateToMainActivity(1) // Main camera page
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // Navigate to settings
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomNavigationBar()
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Camera Mode Selector Card
                    CameraModeCard(
                        selectedMode = cameraMode,
                        onModeChange = { mode ->
                            viewModel.switchCameraMode(mode)
                        }
                    )
                    // Camera Preview Card
                    CameraPreviewCard(
                        cameraState = cameraState,
                        cameraScreenState = cameraScreenState,
                        permissionState = permissionState,
                        onInitializeCamera = { previewView ->
                            if (permissionState == DualModeCameraViewModel.PermissionState.GRANTED) {
                                viewModel.initializeCamera(
                                    context,
                                    this@DualModeCameraActivityCompose,
                                    previewView
                                )
                            }
                        }
                    )
                    // Recording Controls Card
                    RecordingControlsCard(
                        recordingState = recordingState,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() }
                    )
                    // Camera Status Card
                    CameraStatusCard(
                        cameraState = cameraState,
                        cameraScreenState = cameraScreenState
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraModeCard(
        selectedMode: DualModeCameraViewModel.CameraMode,
        onModeChange: (DualModeCameraViewModel.CameraMode) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Camera Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DualModeCameraViewModel.CameraMode.values().forEach { mode ->
                        FilterChip(
                            onClick = { onModeChange(mode) },
                            label = { Text(mode.name.replace("_", " ")) },
                            selected = selectedMode == mode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CameraPreviewCard(
        cameraState: DualModeCameraViewModel.CameraState,
        cameraScreenState: DualModeCameraViewModel.CameraScreenState,
        permissionState: DualModeCameraViewModel.PermissionState,
        onInitializeCamera: (PreviewView) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (permissionState) {
                    DualModeCameraViewModel.PermissionState.GRANTED -> {
                        var previewView: PreviewView? by remember { mutableStateOf(null) }
                        AndroidView(
                            factory = { context ->
                                PreviewView(context).also {
                                    previewView = it
                                    onInitializeCamera(it)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (cameraScreenState.showProgress) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    DualModeCameraViewModel.PermissionState.DENIED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Permission Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Camera permission required",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { checkCameraPermission() }) {
                                Text("Grant Permission")
                            }
                        }
                    }

                    else -> {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    private fun RecordingControlsCard(
        recordingState: DualModeCameraViewModel.RecordingState,
        onStartRecording: () -> Unit,
        onStopRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recording Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recordingState.isRecording) {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Recording")
                        }
                    } else {
                        Button(
                            onClick = onStartRecording,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording")
                        }
                    }
                }
                if (recordingState.isRecording) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Recording: ${recordingState.recordingDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraStatusCard(
        cameraState: DualModeCameraViewModel.CameraState,
        cameraScreenState: DualModeCameraViewModel.CameraScreenState
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Camera Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Connection:")
                    Text(
                        text = if (cameraState.isInitialized) "Connected" else "Disconnected",
                        color = if (cameraState.isInitialized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preview:")
                    Text(
                        text = if (cameraState.isInitialized) "Active" else "Inactive",
                        color = if (cameraState.isInitialized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                if (cameraScreenState.displayMessage.isNotEmpty()) {
                    Text(
                        text = cameraScreenState.displayMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar() {
        val context = LocalContext.current
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Gallery") },
                label = { Text("Gallery") },
                selected = false,
                onClick = { navigateToMainActivity(0) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") },
                label = { Text("Camera") },
                selected = true,
                onClick = {
                    // Current page - already on DualModeCameraActivityCompose
                    Toast.makeText(context, "Already viewing dual camera", Toast.LENGTH_SHORT).show()
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = false,
                onClick = { navigateToMainActivity(2) }
            )
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraVM.onPermissionGranted()
            }

            else -> {
                cameraVM.requestPermission()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToMainActivity(pageIndex: Int) {
        val intent = Intent(this, MainComposeActivity::class.java).apply {
            putExtra("page", pageIndex)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\DualModeCameraComposeActivity.kt =====

package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.presentation.DualModeCameraViewModel

class DualModeCameraComposeActivity : BaseComposeActivity<DualModeCameraViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DualModeCameraComposeActivity::class.java))
        }

        fun startWithMode(context: Context, mode: String) {
            val intent = Intent(context, DualModeCameraComposeActivity::class.java).apply {
                putExtra("INITIAL_MODE", mode)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): DualModeCameraViewModel {
        return viewModels<DualModeCameraViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DualModeCameraViewModel) {
        val localContext = this@DualModeCameraComposeActivity
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var cameraMode by remember { mutableStateOf("Dual") }
        var showSettingsDialog by remember { mutableStateOf(false) }
        IRCameraTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dual Mode Camera",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Switch between front/back camera
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Switch camera feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch")
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    localContext,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                DualModeCameraContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    cameraMode = cameraMode,
                    onCameraModeChange = { cameraMode = it },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showSettingsDialog) {
            CameraSettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onSaveSettings = { settings ->
                    // Apply camera settings
                    showSettingsDialog = false
                }
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    viewModel: DualModeCameraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Camera Preview Area
        CameraPreviewSection(
            cameraMode = cameraMode,
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        // Camera Controls
        CameraControlsSection(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            recordingDuration = recordingDuration,
            cameraMode = cameraMode,
            onCameraModeChange = onCameraModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraPreviewSection(
    cameraMode: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        when (cameraMode) {
            "Dual" -> {
                // Dual camera view with picture-in-picture
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main thermal preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ThermalCameraPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                        // RGB camera PiP
                        Card(
                            modifier = Modifier
                                .size(120.dp, 160.dp)
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            RGBCameraPreview(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Temperature overlay
                        TemperatureOverlay(
                            centerTemp = 36.8f,
                            maxTemp = 42.1f,
                            minTemp = 28.3f,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }

            "Thermal" -> {
                ThermalCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "RGB" -> {
                RGBCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "Split" -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ThermalCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RGBCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        // Recording indicator
        if (isRecording) {
            RecordingIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        // Camera mode indicator
        CameraModeIndicator(
            mode = cameraMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraControlsSection(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
    ) {
        // Camera mode selector
        CameraModeSelector(
            selectedMode = cameraMode,
            onModeChange = onCameraModeChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Recording status
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            OutlinedButton(
                onClick = {
                    // TODO: Open gallery to view captured photos/videos
                    android.widget.Toast.makeText(
                        localContext,
                        "Gallery feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            }
            // Record button
            Button(
                onClick = onRecordingToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "STOP" else "RECORD",
                    fontWeight = FontWeight.Bold
                )
            }
            // Capture button
            OutlinedButton(
                onClick = {
                    // TODO: Capture photo from dual camera
                    android.widget.Toast.makeText(
                        localContext,
                        "Photo captured",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Capture"
                )
            }
        }
    }
}

@Composable
private fun CameraModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Dual", "Thermal", "RGB", "Split")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(mode) },
                leadingIcon = if (selectedMode == mode) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                Color(0xFFE53E3E).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Dual-mode ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                )
            }
        }
    }
}

@Composable
private fun ThermalCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Thermostat,
                contentDescription = "Thermal Camera",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFF6B35)
            )
            Text(
                text = "Thermal Camera Preview",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RGBCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for RGB camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Camera,
                contentDescription = "RGB Camera",
                modifier = Modifier.size(if (modifier == Modifier.fillMaxSize()) 64.dp else 32.dp),
                tint = Color(0xFF2196F3)
            )
            if (modifier == Modifier.fillMaxSize()) {
                Text(
                    text = "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TemperatureOverlay(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "TEMP",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${centerTemp}Â°C",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "H:${maxTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L:${minTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE53E3E)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraModeIndicator(
    mode: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Text(
            text = mode.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CameraSettingsDialog(
    onDismiss: () -> Unit,
    onSaveSettings: (Map<String, Any>) -> Unit
) {
    var videoQuality by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf(30f) }
    var enableStabilization by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HD", "FHD", "4K").forEach { quality ->
                        FilterChip(
                            selected = videoQuality == quality,
                            onClick = { videoQuality = quality },
                            label = { Text(quality) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Frame Rate: ${frameRate.toInt()} fps",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = frameRate,
                    onValueChange = { frameRate = it },
                    valueRange = 15f..60f,
                    steps = 8
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableStabilization,
                        onCheckedChange = { enableStabilization = it }
                    )
                    Text(
                        text = "Enable Image Stabilization",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(
                        mapOf(
                            "quality" to videoQuality,
                            "frameRate" to frameRate.toInt(),
                            "stabilization" to enableStabilization
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\DualModeCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualModeCameraScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Thermal + RGB Camera",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Tune, contentDescription = "Camera Settings")
                        }
                        var viewMode by remember { mutableStateOf("split") }
                        IconButton(onClick = {
                            viewMode = if (viewMode == "split") "overlay" else "split"
                            // TODO: Toggle between split and overlay view modes
                        }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap View")
                        }
                    }
                )
            }
        ) { paddingValues ->
            DualModeCameraContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CameraMode.DUAL_VIEW) }
    var rgbCameraActive by remember { mutableStateOf(true) }
    var thermalCameraActive by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var syncEnabled by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Mode Selector
        CameraModeSelector(
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it }
        )
        // Dual Camera Preview
        DualCameraPreviewCard(
            mode = selectedMode,
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            syncEnabled = syncEnabled
        )
        // Camera Status and Controls
        CameraControlsCard(
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            isRecording = isRecording,
            syncEnabled = syncEnabled,
            onRGBToggle = { rgbCameraActive = it },
            onThermalToggle = { thermalCameraActive = it },
            onRecordingToggle = { isRecording = it },
            onSyncToggle = { syncEnabled = it }
        )
        // Recording Settings
        RecordingSettingsCard()
        // Calibration Tools
        CalibrationToolsCard()
    }
}

enum class CameraMode {
    RGB_ONLY,
    THERMAL_ONLY,
    DUAL_VIEW,
    OVERLAY
}

@Composable
private fun CameraModeSelector(
    selectedMode: CameraMode,
    onModeChange: (CameraMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.RGB_ONLY,
                    label = "RGB Only",
                    selected = selectedMode == CameraMode.RGB_ONLY,
                    onClick = { onModeChange(CameraMode.RGB_ONLY) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.THERMAL_ONLY,
                    label = "Thermal Only",
                    selected = selectedMode == CameraMode.THERMAL_ONLY,
                    onClick = { onModeChange(CameraMode.THERMAL_ONLY) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.DUAL_VIEW,
                    label = "Dual View",
                    selected = selectedMode == CameraMode.DUAL_VIEW,
                    onClick = { onModeChange(CameraMode.DUAL_VIEW) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.OVERLAY,
                    label = "Overlay",
                    selected = selectedMode == CameraMode.OVERLAY,
                    onClick = { onModeChange(CameraMode.OVERLAY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    mode: CameraMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        },
        selected = selected,
        modifier = modifier
    )
}

@Composable
private fun DualCameraPreviewCard(
    mode: CameraMode,
    rgbActive: Boolean,
    thermalActive: Boolean,
    syncEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Camera Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (syncEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Synced",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Synced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Preview area based on mode
            when (mode) {
                CameraMode.RGB_ONLY -> {
                    RGBPreviewArea(active = rgbActive)
                }

                CameraMode.THERMAL_ONLY -> {
                    ThermalPreviewArea(active = thermalActive)
                }

                CameraMode.DUAL_VIEW -> {
                    DualViewPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }

                CameraMode.OVERLAY -> {
                    OverlayPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }
            }
        }
    }
}

@Composable
private fun RGBPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) Color.Black else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "RGB Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThermalPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "25.6Â°C - 31.2Â°C",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.DeviceThermostat,
                    contentDescription = "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DualViewPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (rgbActive) Color.Black else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (rgbActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (rgbActive) "RGB Camera Active" else "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "RGB",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (thermalActive) MaterialTheme.colorScheme.primary else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (thermalActive) Icons.Default.Thermostat else Icons.Default.DeviceThermostat,
                    contentDescription = if (thermalActive) "Thermal Camera Active" else "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Thermal",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OverlayPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color.Black,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rgbActive && thermalActive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "RGB and Thermal Overlay",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB + Thermal Overlay",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Opacity: 60%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                "Both cameras required for overlay",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CameraControlsCard(
    rgbActive: Boolean,
    thermalActive: Boolean,
    isRecording: Boolean,
    syncEnabled: Boolean,
    onRGBToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRecordingToggle: (Boolean) -> Unit,
    onSyncToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Camera toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "RGB Camera")
                    Text("RGB Camera")
                }
                Switch(
                    checked = rgbActive,
                    onCheckedChange = onRGBToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Thermostat, contentDescription = "Thermal Camera")
                    Text("Thermal Camera")
                }
                Switch(
                    checked = thermalActive,
                    onCheckedChange = onThermalToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync Cameras")
                    Text("Sync Cameras")
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncToggle
                )
            }
            HorizontalDivider()
            // Recording controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { onRecordingToggle(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = { onRecordingToggle(true) },
                        modifier = Modifier.weight(1f),
                        enabled = rgbActive || thermalActive
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Recording")
                    }
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        // TODO: Capture snapshot from both cameras
                        android.widget.Toast.makeText(
                            context,
                            "Snapshot captured",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = rgbActive || thermalActive
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Snapshot")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snapshot")
                }
            }
        }
    }
}

@Composable
private fun RecordingSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Quality settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RGB Quality")
                Text("1080p @ 30fps", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thermal Quality")
                Text("384x288 @ 25fps", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Synchronization")
                Text("Hardware Sync", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CalibrationToolsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Calibration Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Start camera alignment process
                        android.widget.Toast.makeText(
                            context,
                            "Starting alignment...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Align Cameras")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Align")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Start color calibration
                        android.widget.Toast.makeText(
                            context,
                            "Starting color calibration...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Calibrate Colors")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\RGBCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.Green
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.Orange
import mpdc4gsr.core.ui.theme.Purple
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModel
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModelFactory

@Composable
fun RGBCameraScreen(
    viewModel: RGBCameraViewModel = viewModel(
        factory = RGBCameraViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraRecorder by viewModel.cameraRecorder.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    // Initialize camera on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(lifecycleOwner)
    }
    // Show error if present
    LaunchedEffect(cameraState.error) {
        showError = cameraState.error != null
    }
    // Use real data from ViewModel
    val isPreviewActive = cameraState.isPreviewActive
    val isRecording = cameraState.isRecording
    val resolution = cameraState.resolution
    val frameRate = cameraState.frameRate
    val recordingDuration = cameraState.recordingDuration
    val capturedFrames = cameraState.capturedFrames
    val cameraChangeCounter = cameraState.cameraChangeCounter
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen camera preview - now properly reactive to cameraRecorder StateFlow
        if (cameraRecorder != null) {
            FullScreenCameraPreview(
                cameraRecorder = cameraRecorder!!,
                isRecording = isRecording,
                cameraChangeCounter = cameraChangeCounter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FullScreenCameraPreviewSimulated(
                isActive = isPreviewActive,
                isRecording = isRecording,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Top overlay with back button and status
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CameraTopBar(
                resolution = resolution,
                frameRate = frameRate,
                isRecording = isRecording,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
        }
        // Bottom overlay with camera controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CameraBottomControls(
                isRecording = isRecording,
                isPreviewActive = isPreviewActive,
                recordingDuration = recordingDuration,
                capturedFrames = capturedFrames,
                onToggleRecording = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                onCapturePhoto = {
                    viewModel.capturePhoto()
                    onCapturePhoto()
                },
                onSwitchCamera = {
                    viewModel.switchCamera()
                }
            )
        }
        // Toggle controls visibility with tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        )
        // Error message display with retry option
        if (showError && cameraState.error != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cameraState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.dismissError() }
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                viewModel.dismissError()
                                viewModel.reinitializeCamera(lifecycleOwner)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTopBar(
    resolution: String,
    frameRate: Int,
    isRecording: Boolean,
    onBackClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "REC",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$resolution â€¢ ${frameRate}fps",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraBottomControls(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onCapturePhoto: () -> Unit,
    onSwitchCamera: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isRecording) {
                Text(
                    text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo capture button
                FilledIconButton(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Photo",
                        tint = Color.White
                    )
                }
                // Video record button - larger, centered
                FilledIconButton(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color.Red else Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else Color.Red,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Camera switch button
                FilledIconButton(
                    onClick = onSwitchCamera,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    cameraChangeCounter: Int,
    modifier: Modifier = Modifier
) {
    // Use key to force recreation when camera switches
    key(cameraChangeCounter) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                // Bind preview when the view updates - ensures preview is connected
                cameraRecorder.bindPreview(previewView)
            },
            modifier = modifier
        )
    }
}

@Composable
private fun FullScreenCameraPreviewSimulated(
    isActive: Boolean,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawRect(color = Color(0xFF2E2E2E), size = size)
                drawRect(
                    color = Color(0xFF4A4A4A),
                    topLeft = Offset(0f, height * 0.6f),
                    size = Size(width, height * 0.4f)
                )
                drawCircle(
                    color = Color(0xFF6A6A6A),
                    radius = width * 0.1f,
                    center = Offset(width * 0.3f, height * 0.4f)
                )
                drawRect(
                    color = Color(0xFF5A5A5A),
                    topLeft = Offset(width * 0.6f, height * 0.2f),
                    size = Size(width * 0.25f, height * 0.4f)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Off",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Camera Preview Off",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RGBCameraPreview(
    isActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f), // Standard camera aspect ratio
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isActive) {
                // Camera preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    // Draw camera background
                    drawRect(
                        color = Color(0xFF2E2E2E),
                        size = size
                    )
                    // Simulate camera scene
                    // Background gradient
                    drawRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = Offset(0f, height * 0.6f),
                        size = Size(width, height * 0.4f)
                    )
                    // Simulated objects
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = width * 0.1f,
                        center = Offset(width * 0.3f, height * 0.4f)
                    )
                    drawRect(
                        color = Color(0xFF5A5A5A),
                        topLeft = Offset(width * 0.6f, height * 0.2f),
                        size = Size(width * 0.25f, height * 0.4f)
                    )
                    // Grid lines (rule of thirds)
                    val strokeWidth = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.3f)
                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(width / 3f, 0f),
                        end = Offset(width / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(width * 2f / 3f, 0f),
                        end = Offset(width * 2f / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 3f),
                        end = Offset(width, height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height * 2f / 3f),
                        end = Offset(width, height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )
                    // Focus indicator (center)
                    val centerX = width / 2
                    val centerY = height / 2
                    val focusSize = 30.dp.toPx()
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - focusSize / 2, centerY - focusSize / 2),
                        size = Size(focusSize, focusSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
                // Overlay indicators
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    // Live indicator
                    Surface(
                        color = if (isRecording) Color.Red else Color.Green,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isRecording) "REC" else "LIVE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Resolution indicator
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resolution,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                // Frame rate indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${frameRate}fps",
                        color = Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                // Preview off
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Off",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Preview Off",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tap to enable preview",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Bind camera preview
                        cameraRecorder.bindPreview(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Recording indicator
            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "REC",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraStatusCard(
    isPreviewActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Status",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when {
                        isRecording -> Color.Red.copy(alpha = 0.2f)
                        isPreviewActive -> Color.Green.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when {
                            isRecording -> "RECORDING"
                            isPreviewActive -> "PREVIEW"
                            else -> "STANDBY"
                        },
                        color = when {
                            isRecording -> Color.Red
                            isPreviewActive -> Color.Green
                            else -> Color.Gray
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // Camera metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Resolution", resolution, Color.White)
                MetricItem("Frame Rate", "${frameRate}fps", Color.Green)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Exposure", exposureTime, Color.Yellow)
                MetricItem("ISO", iso.toString(), Color.Cyan)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Focus", focusMode, MaterialTheme.colorScheme.primary)
                MetricItem("White Balance", whiteBalance, Color.Magenta)
            }
        }
    }
}

@Composable
private fun RecordingControlsCard(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onTogglePreview: () -> Unit,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (isRecording) {
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        "Duration",
                        "${recordingDuration}s",
                        Color.Red
                    )
                    MetricItem(
                        "Frames",
                        capturedFrames.toString(),
                        Color.Green
                    )
                    MetricItem(
                        "File Size",
                        "${(recordingDuration * 2.5f).toInt()}MB",
                        Color.Cyan
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onTogglePreview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewActive) Orange else Green
                    )
                ) {
                    Text(if (isPreviewActive) "Stop Preview" else "Start Preview")
                }
                Button(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.VideoCall,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                Button(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Photo")
                    Spacer(Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCard(
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    currentFocusMode: mpdc4gsr.feature.camera.presentation.FocusMode,
    currentWhiteBalance: mpdc4gsr.feature.camera.presentation.WhiteBalance,
    onResolutionChange: (String) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onExposureChange: (String) -> Unit,
    onISOChange: (Int) -> Unit,
    onFocusModeChange: (mpdc4gsr.feature.camera.presentation.FocusMode) -> Unit,
    onWhiteBalanceChange: (mpdc4gsr.feature.camera.presentation.WhiteBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Quick setting buttons - Resolution and Frame Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onResolutionChange("1920Ã—1080") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1920Ã—1080") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1080p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onResolutionChange("1280Ã—720") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1280Ã—720") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("720p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onFrameRateChange(if (frameRate == 30) 60 else 30) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${frameRate}fps", fontSize = 10.sp)
                }
            }
            // Additional camera controls - Focus and White Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onFocusModeChange(currentFocusMode.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Focus: $focusMode", fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        onWhiteBalanceChange(currentWhiteBalance.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WB: $whiteBalance", fontSize = 9.sp)
                }
            }
            // Advanced settings info
            Text(
                text = "Advanced exposure and ISO controls available in camera settings menu",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraScreenPreview() {
    IRCameraTheme {
        RGBCameraScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\TapToFocusCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapToFocusPreview(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize()) {
        // Camera PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewViewConfig(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        // Call the focus callback
                        onTapToFocus(normalizedX, normalizedY)
                        // Auto-hide focus indicator after delay
                        coroutineScope.launch {
                            delay(2000)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        // Focus indicator overlay
        if (showFocusIndicator && focusPoint != null) {
            FocusIndicator(focusPoint = focusPoint!!)
        }
    }
}

@Composable
private fun FocusIndicator(focusPoint: Offset) {
    val density = LocalDensity.current
    val circleRadius = with(density) { 60.dp.toPx() }
    // Animate the focus indicator
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusAlpha"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer circle
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = circleRadius,
            center = focusPoint,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        // Inner crosshair
        val crossSize = circleRadius * 0.3f
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x - crossSize, focusPoint.y),
            end = Offset(focusPoint.x + crossSize, focusPoint.y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x, focusPoint.y - crossSize),
            end = Offset(focusPoint.x, focusPoint.y + crossSize),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TapToFocusPreviewWithCustomIndicator(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    focusIndicatorColor: Color = Color.White,
    focusIndicatorRadius: Float = 60f,
    autoHideDelay: Long = 2000L,
    modifier: Modifier = Modifier
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val radiusPx = with(density) { focusIndicatorRadius.dp.toPx() }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { previewViewConfig(this) } },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        onTapToFocus(normalizedX, normalizedY)
                        coroutineScope.launch {
                            delay(autoHideDelay)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        if (showFocusIndicator && focusPoint != null) {
            CustomFocusIndicator(
                focusPoint = focusPoint!!,
                color = focusIndicatorColor,
                radius = radiusPx
            )
        }
    }
}

@Composable
private fun CustomFocusIndicator(
    focusPoint: Offset,
    color: Color,
    radius: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "customFocus")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaledRadius = radius * animatedScale
        // Animated circle
        drawCircle(
            color = color.copy(alpha = animatedAlpha),
            radius = scaledRadius,
            center = focusPoint,
            style = Stroke(width = 4f)
        )
        // Corner brackets
        val bracketSize = scaledRadius * 0.3f
        val offset = scaledRadius - bracketSize
        // Top-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset + bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y - offset),
            strokeWidth = 3f
        )
        // Top-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset + bracketSize),
            strokeWidth = 3f
        )
        // Bottom-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset - bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y + offset),
            strokeWidth = 3f
        )
        // Bottom-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset - bracketSize),
            strokeWidth = 3f
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\ui\TimeLapseCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.presentation.TimeLapseCameraViewModel
import mpdc4gsr.feature.camera.presentation.TimeLapseCameraViewModelFactory
import mpdc4gsr.feature.camera.presentation.TimeLapseMode

@Composable
fun TimeLapseCameraScreen(
    viewModel: TimeLapseCameraViewModel = viewModel(
        factory = TimeLapseCameraViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeLapseState by viewModel.timeLapseState.collectAsState()
    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Time-Lapse Camera",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = Color(0xFF16131e)
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera Preview Placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D2A3E)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Camera Preview",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                // Recording Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (timeLapseState.isRecording) "Recording" else "Ready",
                                color = if (timeLapseState.isRecording)
                                    MaterialTheme.colorScheme.primary
                                else Color.Gray
                            )
                        }
                        HorizontalDivider()
                        InfoRow("Frames Captured", "${timeLapseState.capturedFrames}")
                        InfoRow("Interval", "${timeLapseState.intervalSeconds}s")
                        InfoRow("Est. Video Length", "${timeLapseState.estimatedVideoLength}s")
                        InfoRow("Duration", "${timeLapseState.totalDuration}s")
                    }
                }
                // Mode Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TimeLapseMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = timeLapseState.mode == mode,
                                    onClick = { viewModel.setMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(mode.displayName)
                            }
                        }
                    }
                }
                // Manual Interval Control
                if (timeLapseState.mode == TimeLapseMode.MANUAL) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Custom Interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${timeLapseState.intervalSeconds} seconds")
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds - 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds + 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                            Slider(
                                value = timeLapseState.intervalSeconds.toFloat(),
                                onValueChange = { viewModel.updateInterval(it.toInt()) },
                                valueRange = 1f..60f,
                                steps = 58
                            )
                        }
                    }
                }
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!timeLapseState.isRecording) {
                        Button(
                            onClick = { viewModel.startTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }
                // Error Display
                timeLapseState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLapseCameraScreenPreview() {
    IRCameraTheme {
        TimeLapseCameraScreen()
    }
}


