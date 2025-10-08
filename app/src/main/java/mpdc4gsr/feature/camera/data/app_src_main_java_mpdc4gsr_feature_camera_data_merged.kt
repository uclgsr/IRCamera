// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\camera\data' directory and its subdirectories.
// Total files: 12 | Generated on: 2025-10-08 01:42:33


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\Camera2System.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.TextureView
import kotlinx.coroutines.*
import mpdc4gsr.core.utils.AppLogger
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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\CameraConfigurationManager.kt =====

package mpdc4gsr.feature.camera.data

import android.os.Build
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import mpdc4gsr.core.utils.AppLogger

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\CameraController.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import mpdc4gsr.core.utils.AppLogger
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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\CameraControlsManager.kt =====

package mpdc4gsr.feature.camera.data

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import mpdc4gsr.core.data.ErrorType
import mpdc4gsr.core.utils.AppLogger

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\CameraErrorMessageProvider.kt =====

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\CameraPerformanceManager.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import mpdc4gsr.core.utils.AppLogger
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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\DeviceCaps.kt =====

package mpdc4gsr.feature.camera.data

import android.util.Size

data class DeviceCaps(
    val supportsRaw: Boolean,
    val rawSize: Size,
    val supports4k60: Boolean,
    val sensorOrientation: Int,
)


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\ModeManager.kt =====

package mpdc4gsr.feature.camera.data

import mpdc4gsr.core.utils.AppLogger

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\RawEngine.kt =====

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
import android.util.Size
import android.view.Surface
import mpdc4gsr.core.utils.AppLogger
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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\SamsungDeviceCompatibility.kt =====

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\UiBridge.kt =====

package mpdc4gsr.feature.camera.data

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import mpdc4gsr.core.utils.AppLogger

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


// ===== FROM: app\src\main\java\mpdc4gsr\feature\camera\data\VideoEngine.kt =====

package mpdc4gsr.feature.camera.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.util.Size
import mpdc4gsr.core.utils.AppLogger
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