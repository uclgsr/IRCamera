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
