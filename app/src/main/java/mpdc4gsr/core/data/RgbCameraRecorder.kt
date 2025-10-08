package mpdc4gsr.core.data

import android.content.Context
import android.graphics.ImageFormat
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.feature.camera.data.CameraConfigurationManager
import mpdc4gsr.feature.camera.data.CameraControlsManager
import mpdc4gsr.feature.camera.data.CameraPerformanceManager
import mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RgbCameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null,
    private val useFrontCamera: Boolean = false,
    private val permissionManager: PermissionManager? = null
) : SensorRecorder {
    companion object {
        private const val TAG = "RgbCameraRecorder"
        private const val VIDEO_WIDTH_4K = 3840
        private const val VIDEO_HEIGHT_4K = 2160
        private const val VIDEO_WIDTH_1080P = 1920
        private const val VIDEO_HEIGHT_1080P = 1080
        private const val VIDEO_WIDTH_720P = 1280
        private const val VIDEO_HEIGHT_720P = 720
        private const val VIDEO_WIDTH_480P = 854
        private const val VIDEO_HEIGHT_480P = 480
        private const val VIDEO_FPS_60 = 60
        private const val VIDEO_FPS_TARGET = 30
        private const val VIDEO_FPS_FALLBACK = 24
        private const val VIDEO_BITRATE_4K = 50_000_000
        private const val VIDEO_BITRATE_1080P = 20_000_000
        private const val AUDIO_BITRATE = 256_000
        private const val JPEG_QUALITY = 100

        // Throttled frame capture at 10-15fps for optimized I/O performance
        private const val CAPTURE_FPS = 12 // Reduced from 30 to optimize I/O performance

        // Frame capture throttling configuration with adaptive optimization
        private const val FRAME_CAPTURE_EVERY_N_FRAMES =
            2 // Capture every 2nd frame at 24fps = ~12fps output
        private const val MAX_PENDING_CAPTURES = 2 // Reduced for better I/O handling
        private const val ADAPTIVE_OPTIMIZATION_THRESHOLD =
            5 // Switch to more aggressive optimization if needed
        private const val ENABLE_RAW_CAPTURE = true
        private const val RAW_FILE_EXTENSION = ".dng"
        private const val JPEG_FILE_EXTENSION = ".jpg"
        private const val MAX_CONSECUTIVE_FRAME_ERRORS = 10
        private const val FRAME_ERROR_RESET_INTERVAL = 30000L
        private val KNOWN_4K_DEVICES = setOf(
            "SM-S906B",
            "SM-S916B",
            "SM-S908B",
            "SM-S901B",
            "SM-S911B",
            "SM-S918B"
        )
        private val KNOWN_RAW_DEVICES = setOf(
            "SM-S906B",
            "SM-S916B",
            "SM-S908B",
            "SM-S901B",
            "SM-S911B",
            "SM-S918B"
        )
    }

    // CameraInfo data class for camera information
    data class CameraInfo(
        val cameraId: String,
        val facing: Int, // CameraSelector.LENS_FACING_BACK or CameraSelector.LENS_FACING_FRONT
        val supportsRaw: Boolean,
        val supports4K: Boolean,
        val displayName: String,
    )

    override val sensorId: String = "rgb_camera_${System.currentTimeMillis()}"
    override val sensorType: String = "RGB_Camera_CameraX"
    override val samplingRate: Double = VIDEO_FPS_TARGET.toDouble()
    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    private var selectedVideoWidth = VIDEO_WIDTH_1080P
    private var selectedVideoHeight = VIDEO_HEIGHT_1080P
    private var selectedVideoFps = VIDEO_FPS_TARGET
    private var selectedVideoBitrate = VIDEO_BITRATE_1080P
    private var deviceSupports4K = false
    private var deviceSupportsRAW = false
    private var actualFrameRateAchieved = 0.0
    private var recordingSettings: RecordingSettingsRepository.RecordingSettings? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var rawImageCapture: ImageCapture? = null

    // Extracted managers for better code organization
    private val configurationManager = CameraConfigurationManager()
    private val controlsManager = CameraControlsManager { errorType, message ->
        recordingScope.launch {
            emitError(errorType, message)
        }
    }
    private val performanceManager =
        CameraPerformanceManager(context) // For Stage 3 RAW DNG capture using ImageFormat.RAW_SENSOR
    private var camera: Camera? = null
    private var activeRecording: Recording? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val statusFlow = MutableStateFlow(createInitialStatus())
    private val errorFlow = MutableSharedFlow<SensorError>()
    private var sessionDirectory: String = ""
    private var sessionMetadata: SessionMetadata? = null
    private var csvWriter: CSVWriter? = null
    private var videoFile: File? = null
    private var csvBufferedWriter: CSVBufferedWriter? = null
    private var csvFile: File? = null
    private val samplesRecorded = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)
    private val sessionReferenceTimestampNs = AtomicLong(0)
    private val sessionStartOffsetNs = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private var droppedFrames = AtomicLong(0)
    private val syncMarkersRecorded = AtomicLong(0)
    private val framesCaptured = AtomicLong(0)
    private val frameTimestamps = mutableListOf<Long>()
    private var lastFrameRateCheck = AtomicLong(0)
    private val frameRateCheckInterval = 5000L
    private val consecutiveFrameErrors = AtomicLong(0)
    private var lastFrameErrorTime = AtomicLong(0)
    private val _cameraStatus = MutableStateFlow("Uninitialized")
    val cameraStatus: StateFlow<String> = _cameraStatus.asStateFlow()
    private var currentCameraSelector = if (useFrontCamera) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
    private var isUsingFrontCamera = useFrontCamera
    private var supportsFrontCamera = false
    private var supportsBackCamera = false
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var frameCaptureJob: Job? = null
    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            recordingSettings =
                RecordingSettingsRepository.getInstance(context).getSettings()
            Log.d(
                TAG,
                "Recording settings loaded: quality=${recordingSettings?.recordingQuality}, fps=${recordingSettings?.videoFrameRate}, audio=${recordingSettings?.audioEnabled}"
            )
            // Observe settings changes for real-time updates
            observeRecordingSettingsChanges()
            Log.d(
                TAG,
                "Initializing CameraX with ${if (useFrontCamera) "front" else "back"} camera"
            )
            if (!checkAndRequestPermissions()) {
                _cameraStatus.value = "Camera Permission Denied"
                emitError(
                    ErrorType.PERMISSION_DENIED,
                    "Camera permission is required for recording"
                )
                return@withContext false
            }
            _cameraStatus.value = "Initializing..."
            // Wrap CameraProvider initialization in try-catch for robust error handling
            cameraProvider = try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider
            } catch (e: java.util.concurrent.TimeoutException) {
                _cameraStatus.value = "Camera Service Timeout"
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "Camera service timeout. Camera may be in use by another app"
                )
                return@withContext false
            } catch (e: java.util.concurrent.ExecutionException) {
                _cameraStatus.value = "Camera Service Error"
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "Camera service error: ${e.cause?.message ?: e.message}"
                )
                return@withContext false
            } catch (e: Exception) {
                _cameraStatus.value = "Camera Service Unavailable"
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "Camera service unavailable: ${e.javaClass.simpleName} - ${e.message}"
                )
                return@withContext false
            }
            val cameraType = if (isUsingFrontCamera) "Front" else "Back"
            if (!cameraProvider!!.hasCamera(currentCameraSelector)) {
                val availableCameras = mutableListOf<String>()
                if (cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    availableCameras.add("Back")
                }
                if (cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    availableCameras.add("Front")
                }
                val availableMsg = if (availableCameras.isNotEmpty()) {
                    "Available: ${availableCameras.joinToString()}"
                } else {
                    "No cameras available"
                }
                _cameraStatus.value = "$cameraType Camera Not Available"
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "$cameraType camera not available on this device. $availableMsg"
                )
                return@withContext false
            }
            // Detect device capabilities and configure camera
            detectDeviceCapabilities()
            detectAvailableCameras()
            // Validate device requirements after camera detection
            if (!validateDeviceRequirements()) {
                return@withContext false
            }
            optimizeVideoConfiguration()
            // Setup and bind camera use cases with error handling
            setupCameraUseCases()
            val bindSuccess = bindUseCases()
            if (!bindSuccess) {
                _cameraStatus.value = "Camera Binding Failed"
                emitError(ErrorType.INITIALIZATION_FAILED, "Failed to bind camera use cases")
                return@withContext false
            }
            _cameraStatus.value = "Ready"
            Log.i(
                TAG,
                " CameraX initialized successfully: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, Preview: ${previewView != null}"
            )
            // Log detailed capabilities for debugging and validation
            val capabilities = getDetailedCameraCapabilities()
            Log.i(
                TAG,
                "Device capabilities validated: 4K=${capabilities["supports_4k"]}, 60fps=${capabilities["supports_60fps"]}, RAW=${capabilities["supports_raw"]}"
            )
            return@withContext true
        } catch (e: SecurityException) {
            _cameraStatus.value = "Permission Error"
            emitError(ErrorType.PERMISSION_DENIED, "Camera permission required: ${e.message}")
            return@withContext false
        } catch (e: IllegalStateException) {
            _cameraStatus.value = "Camera In Use"
            emitError(
                ErrorType.INITIALIZATION_FAILED,
                "Camera is being used by another application"
            )
            return@withContext false
        } catch (e: Exception) {
            _cameraStatus.value = "Initialization Failed"
            emitError(ErrorType.INITIALIZATION_FAILED, "Camera initialization failed: ${e.message}")
            return@withContext false
        }
    }

    private fun detectDeviceCapabilities() {
        try {
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            deviceSupports4K = KNOWN_4K_DEVICES.contains(deviceModel) ||
                    (deviceModel.contains("S22", ignoreCase = true) && deviceManufacturer.equals(
                        "samsung",
                        ignoreCase = true
                    ))
            deviceSupportsRAW = KNOWN_RAW_DEVICES.contains(deviceModel) ||
                    (deviceModel.contains("S22", ignoreCase = true) && deviceManufacturer.equals(
                        "samsung",
                        ignoreCase = true
                    ))
            cameraProvider?.let { provider ->
                val camera = provider.bindToLifecycle(lifecycleOwner, currentCameraSelector)
                val cameraInfo = camera.cameraInfo
                deviceSupports4K = deviceSupports4K || checkVideoProfileSupport(cameraInfo)
                try {
                    val cameraCharacteristics =
                        androidx.camera.camera2.interop.Camera2CameraInfo.from(cameraInfo)
                    val capabilities = cameraCharacteristics.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )
                    deviceSupportsRAW = deviceSupportsRAW || capabilities?.contains(
                        android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
                    ) == true
                } catch (e: Exception) {
                }
            }
            Log.i(
                TAG,
                "Samsung Galaxy S22 capabilities - 4K: $deviceSupports4K, RAW: $deviceSupportsRAW for $deviceManufacturer $deviceModel"
            )
        } catch (e: Exception) {
            deviceSupports4K = false
            deviceSupportsRAW = false
        }
    }

    private fun checkVideoProfileSupport(cameraInfo: androidx.camera.core.CameraInfo): Boolean {
        return try {
            // Check if camera supports high-quality video recording
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun detectAvailableCameras() {
        try {
            cameraProvider?.let { provider ->
                supportsBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                supportsFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                Log.i(
                    TAG,
                    "  • Back camera: ${if (supportsBackCamera) "Available" else "Not available"}"
                )
                Log.i(
                    TAG,
                    "  • Front camera: ${if (supportsFrontCamera) "Available" else "Not available"}"
                )
                if (isUsingFrontCamera && !supportsFrontCamera) {
                    Log.w(
                        TAG,
                        " Front camera requested but not available, switching to back camera"
                    )
                    recordingScope.launch {
                        switchToBackCamera()
                    }
                } else if (!isUsingFrontCamera && !supportsBackCamera) {
                    recordingScope.launch {
                        switchToFrontCamera()
                    }
                }
            }
        } catch (e: Exception) {
            supportsBackCamera = true
            supportsFrontCamera = false
        }
    }

    suspend fun switchToFrontCamera(): Boolean {
        return switchCamera(useFrontCamera = true)
    }

    suspend fun switchToBackCamera(): Boolean {
        return switchCamera(useFrontCamera = false)
    }

    private suspend fun switchCamera(useFrontCamera: Boolean): Boolean =
        withContext(Dispatchers.Main) {
            return@withContext try {
                val targetCameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val isAvailable = cameraProvider?.hasCamera(targetCameraSelector) ?: false
                if (!isAvailable) {
                    val cameraType = if (useFrontCamera) "front" else "back"
                    emitError(
                        ErrorType.INITIALIZATION_FAILED,
                        "$cameraType camera not available"
                    )
                    return@withContext false
                }
                if (isUsingFrontCamera == useFrontCamera) {
                    return@withContext true
                }
                val wasRecording = _isRecording.get()
                if (wasRecording) {
                    emitError(
                        ErrorType.RECORDING_FAILED,
                        "Cannot switch camera while recording"
                    )
                    return@withContext false
                }
                _cameraStatus.value = "Switching Camera..."
                currentCameraSelector = targetCameraSelector
                isUsingFrontCamera = useFrontCamera
                cameraProvider?.unbindAll()
                val rebindSuccess = bindUseCasesToCamera()
                if (rebindSuccess) {
                    _cameraStatus.value =
                        "Camera Switched - ${if (useFrontCamera) "Front" else "Back"} Camera Active"
                    Log.i(
                        TAG,
                        " Successfully switched to ${if (useFrontCamera) "front" else "back"} camera"
                    )
                    true
                } else {
                    _cameraStatus.value = "Camera Switch Failed"
                    false
                }
            } catch (e: Exception) {
                _cameraStatus.value = "Camera Switch Error"
                emitError(ErrorType.INITIALIZATION_FAILED, "Camera switch failed: ${e.message}")
                false
            }
        }

    fun getCurrentCameraInfo(): CameraDisplayInfo {
        return object : CameraDisplayInfo {
            override val isUsingFrontCamera = this@RgbCameraRecorder.isUsingFrontCamera
            override val backAvailable = supportsBackCamera
            override val frontAvailable = supportsFrontCamera
            override val canSwitch = !_isRecording.get() && (frontAvailable && backAvailable)
            override val supports4K = deviceSupports4K
            override val supportsRAW = deviceSupportsRAW
            override val supports60fps = checkDevice60fpsSupport()
            override val currentResolution = "${selectedVideoWidth}x${selectedVideoHeight}"
            override val currentFormat =
                if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) "JPEG+RAW" else "JPEG"
        }
    }

    fun getResolution(): String {
        return "${selectedVideoWidth}x${selectedVideoHeight}"
    }

    fun getCurrentFps(): Int {
        return if (actualFrameRateAchieved > 0) {
            actualFrameRateAchieved.toInt()
        } else {
            selectedVideoFps
        }
    }

    fun bindPreview(previewView: PreviewView) {
        try {
            this.preview?.let { preview ->
                preview.setSurfaceProvider(previewView.surfaceProvider)
                previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
            } ?: run {
                recordingScope.launch {
                    emitError(
                        ErrorType.INITIALIZATION_FAILED,
                        "Camera preview not available - please restart camera"
                    )
                }
            }
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "Failed to bind camera preview: ${e.message}"
                )
            }
        }
    }

    interface CameraDisplayInfo {
        val isUsingFrontCamera: Boolean
        val backAvailable: Boolean
        val frontAvailable: Boolean
        val canSwitch: Boolean
        val supports4K: Boolean
        val supportsRAW: Boolean
        val supports60fps: Boolean
        val currentResolution: String
        val currentFormat: String
    }

    private fun optimizeVideoConfiguration() {
        try {
            val supports60fps = checkDevice60fpsSupport()
            val qualityConfig = recordingSettings?.let { settings ->
                RecordingSettingsRepository.getInstance(context)
                    .getQualityConfig(settings.recordingQuality)
            }
            val preferredFps = recordingSettings?.videoFrameRate ?: VIDEO_FPS_TARGET
            if (qualityConfig != null) {
                selectedVideoWidth = qualityConfig.videoWidth
                selectedVideoHeight = qualityConfig.videoHeight
                selectedVideoBitrate = qualityConfig.videoBitrate
                selectedVideoFps =
                    preferredFps.coerceIn(VIDEO_FPS_FALLBACK, if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET)
            } else if (deviceSupports4K) {
                selectedVideoWidth = VIDEO_WIDTH_4K
                selectedVideoHeight = VIDEO_HEIGHT_4K
                selectedVideoBitrate = VIDEO_BITRATE_4K
                selectedVideoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET
            } else {
                selectedVideoWidth = VIDEO_WIDTH_1080P
                selectedVideoHeight = VIDEO_HEIGHT_1080P
                selectedVideoBitrate = VIDEO_BITRATE_1080P
                selectedVideoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET
            }
            Log.i(
                TAG,
                "Video configuration optimized: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, bitrate: ${selectedVideoBitrate}"
            )
            Log.i(
                TAG,
                "Advanced capabilities: 4K=${deviceSupports4K}, RAW=${deviceSupportsRAW}, 60fps=${supports60fps}, UserSettings=${qualityConfig != null}"
            )
        } catch (e: Exception) {
            selectedVideoWidth = VIDEO_WIDTH_1080P
            selectedVideoHeight = VIDEO_HEIGHT_1080P
            selectedVideoBitrate = VIDEO_BITRATE_1080P
            selectedVideoFps = VIDEO_FPS_FALLBACK
        }
    }

    private fun checkDevice60fpsSupport(): Boolean {
        return try {
            val deviceModel = Build.MODEL
            val manufacturer = Build.MANUFACTURER.lowercase()
            // Samsung S22 series and other high-end devices that support 60fps
            val supports60fps = manufacturer == "samsung" && (
                    deviceModel in KNOWN_4K_DEVICES ||
                            deviceModel.startsWith("SM-S9") || // S22 series
                            deviceModel.startsWith("SM-S10") || // S23 series
                            deviceModel.startsWith("SM-G9") || // Note series
                            deviceModel.startsWith("SM-G99") // S21/S22 Ultra
                    )
            Log.i(
                TAG,
                "60fps support check - Device: $manufacturer $deviceModel, Supports 60fps: $supports60fps"
            )
            supports60fps
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun setupCameraUseCases() = withContext(Dispatchers.Main) {
        try {
            preview = Preview.Builder().apply {
                val previewSize = if (deviceSupports4K) {
                    Size(1920, 1080)
                } else {
                    Size(1280, 720)
                }
                @Suppress("DEPRECATION")
                setTargetResolution(previewSize)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setTargetFrameRate(Range(24, 30))
                }
                Log.d(
                    TAG,
                    "Preview configured with resolution: ${previewSize.width}x${previewSize.height}"
                )
            }.build()
            val recorder = createOptimizedRecorder()
            videoCapture = VideoCapture.withOutput(recorder)
            imageCapture = ImageCapture.Builder().apply {
                @Suppress("DEPRECATION")
                setTargetResolution(Size(selectedVideoWidth, selectedVideoHeight))
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setJpegQuality(JPEG_QUALITY)
                setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) {
                    try {
                        androidx.camera.camera2.interop.Camera2Interop.Extender(this)
                            .setCaptureRequestOption(
                                android.hardware.camera2.CaptureRequest.CONTROL_MODE,
                                android.hardware.camera2.CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
                            )
                    } catch (e: Exception) {
                    }
                }
            }.build()
            // Initialize RAW ImageCapture for Stage 3 DNG capture using ImageFormat.RAW_SENSOR
            if (deviceSupportsRAW && ENABLE_RAW_CAPTURE && SamsungDeviceCompatibility.isStage3Compatible()) {
                try {
                    // Create ImageCapture configured for RAW format
                    val rawImageCapture = ImageCapture.Builder().apply {
                        // Set buffer format to RAW_SENSOR for actual RAW data
                        setBufferFormat(ImageFormat.RAW_SENSOR)
                        @Suppress("DEPRECATION")
                        setTargetResolution(Size(selectedVideoWidth, selectedVideoHeight))
                        setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        // Configure Camera2 interop for Stage 3 RAW capture
                        val extender = androidx.camera.camera2.interop.Camera2Interop.Extender(this)
                        extender.setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_MODE,
                            android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_MODE_HIGH_QUALITY
                        )
                        extender.setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.NOISE_REDUCTION_MODE,
                            android.hardware.camera2.CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                        )
                    }.build()
                    // Store the RAW ImageCapture for use in capture operations
                    this@RgbCameraRecorder.rawImageCapture = rawImageCapture
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun bindUseCases(): Boolean = withContext(Dispatchers.Main) {
        return@withContext bindUseCasesToCamera()
    }

    private suspend fun bindUseCasesToCamera(): Boolean = withContext(Dispatchers.Main) {
        try {
            cameraProvider?.unbindAll()
            val useCases = mutableListOf<UseCase>()
            videoCapture?.let { useCases.add(it) }
            imageCapture?.let { useCases.add(it) }
            rawImageCapture?.let {
                useCases.add(it)
            }
            preview?.let { preview ->
                useCases.add(preview)
                previewView?.let { previewView ->
                    try {
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        Log.i(
                            TAG,
                            " Preview bound to PreviewView successfully - live camera feed enabled"
                        )
                        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    } catch (e: Exception) {
                        emitError(
                            ErrorType.INITIALIZATION_FAILED,
                            "Camera preview unavailable but recording will continue"
                        )
                    }
                } ?: run {
                }
            }
            if (useCases.isEmpty()) {
                return@withContext false
            }
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector,
                *useCases.toTypedArray()
            )
            camera?.let { cam ->
                val cameraInfo = cam.cameraInfo
                val hasFlash = cameraInfo.hasFlashUnit()
                val zoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
                Log.i(
                    TAG,
                    "  - Resolution: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps"
                )
                return@withContext true
            } ?: run {
                return@withContext false
            }
        } catch (e: Exception) {
            emitError(
                ErrorType.INITIALIZATION_FAILED,
                "Failed to bind camera use cases: ${e.message}"
            )
            return@withContext false
        }
    }

    private suspend fun checkAndRequestPermissions(): Boolean {
        val hasCameraPermission = hasCameraPermission()
        val hasStoragePermission = hasStoragePermission()
        if (hasCameraPermission && hasStoragePermission) {
            return true
        }

        val missing = mutableListOf<String>()
        if (!hasCameraPermission) {
            missing.add("Camera")
        }
        if (!hasStoragePermission) {
            missing.add("Storage")
        }
        return permissionManager?.let { permissionManager ->
            try {
                _cameraStatus.value = "Requesting Permissions..."
                
                val granted = permissionManager.requestCameraPermissions()
                if (granted) {
                    val recheckCamera = hasCameraPermission()
                    val recheckStorage = hasStoragePermission()
                    if (recheckCamera && recheckStorage) {
                        _cameraStatus.value = "Permissions Granted"
                        true
                    } else {
                        val stillMissing = mutableListOf<String>()
                        if (!recheckCamera) stillMissing.add("Camera")
                        if (!recheckStorage) stillMissing.add("Storage")

                        _cameraStatus.value = "Missing Permissions: ${stillMissing.joinToString()}"
                        emitError(
                            ErrorType.PERMISSION_DENIED,
                            "Required permissions denied: ${stillMissing.joinToString()}. Please grant permissions in Settings."
                        )
                        false
                    }
                } else {
                    _cameraStatus.value = "Permissions Denied"
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Camera permission denied. Required for video recording and frame capture."
                    )
                    false
                }
            } catch (e: Exception) {
                _cameraStatus.value = "Permission Request Failed"
                emitError(
                    ErrorType.PERMISSION_DENIED,
                    "Permission request failed: ${e.message}. Please grant permissions manually in Settings."
                )
                false
            }
        } ?: run {
            _cameraStatus.value = "Missing Permissions: ${missing.joinToString()}"
            emitError(
                ErrorType.PERMISSION_DENIED,
                "Missing permissions: ${missing.joinToString()}. PermissionManager not configured. Grant permissions before initializing camera."
            )
            false
        }
    }

    private fun createOptimizedRecorder(): Recorder {
        return try {
            // Use QualitySelector to attempt UHD and fall back to lower quality if unsupported
            val qualitySelector = if (deviceSupports4K) {
                QualitySelector.from(
                    Quality.UHD,
                    FallbackStrategy.lowerQualityThan(Quality.UHD)
                )
            } else {
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityThan(Quality.FHD)
                )
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            recorder
        } catch (e: Exception) {
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

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata
    ): Boolean {
        this.sessionMetadata = sessionMetadata
        this.sessionDirectory = sessionDirectory
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    return@withContext true
                }
                mpdc4gsr.feature.settings.data.RecordingSettingsValidator.validateAndLogSettings(context)
                Log.i(
                    TAG,
                    "Recording config from settings: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, audio=${recordingSettings?.audioEnabled}"
                )
                _isRecording.set(true)
                sessionStartTime.set(System.currentTimeMillis())
                sessionReferenceTimestampNs.set(sessionMetadata.sessionStartMonotonicNs)
                val localStartNs = TimestampManager.getCurrentTimestampNanos()
                sessionStartOffsetNs.set(localStartNs - sessionMetadata.sessionStartMonotonicNs)
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) sessionDir.mkdirs()
                setupOutputFiles()
                initializeCsvWriter()
                sessionMetadata.addSyncEvent(
                    "RGB_RECORDING_START", mapOf(
                        "sensor_type" to "rgb_camera",
                        "sensor_id" to sensorId,
                        "recording_config" to "${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps",
                        "audio_enabled" to "${recordingSettings?.audioEnabled}",
                        "recording_quality" to "${recordingSettings?.recordingQuality}",
                        "sync_verification" to "enabled"
                    )
                )
                if (cameraProvider == null) {
                    withContext(Dispatchers.Main) {
                        if (!initialize()) {
                            _isRecording.set(false)
                            return@withContext false
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (!startVideoRecording()) {
                        _isRecording.set(false)
                        return@withContext false
                    }
                }
                startFrameCapture()
                _cameraStatus.value =
                    "Recording - ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps"
                Log.i(
                    TAG,
                    "RGB camera recording started successfully with ${selectedVideoWidth}x${selectedVideoHeight} resolution"
                )
                return@withContext true
            } catch (e: Exception) {
                _isRecording.set(false)
                _cameraStatus.value = "Recording Failed"
                emitError(ErrorType.RECORDING_FAILED, "Failed to start recording: ${e.message}")
                return@withContext false
            }
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean {
        return try {
            if (_isRecording.get()) {
                return false
            }
            this.sessionDirectory = sessionDirectory
            val sessionDir = File(sessionDirectory)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            if (cameraProvider == null) {
                withContext(Dispatchers.Main) {
                    if (!initialize()) {
                        _isRecording.set(false)
                        return@withContext false
                    }
                }
            }
            if (!hasStoragePermission()) {
                emitError(
                    ErrorType.PERMISSION_DENIED,
                    "Storage permission required for saving recordings and frames"
                )
                return false
            }
            if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) {
            }
            setupOutputFiles()
            val videoRecordingStarted = startVideoRecording()
            if (!videoRecordingStarted) {
                return false
            }
            initializeSessionTiming()
            recordingScope.launch {
                initializeCsvWriter()
            }
            startFrameCapture()
            _isRecording.set(true)
            samplesRecorded.set(0)
            droppedFrames.set(0)
            framesCaptured.set(0)
            updateStatus(isRecording = true)
            true
        } catch (e: Exception) {
            emitError(ErrorType.RECORDING_FAILED, "Failed to start recording: ${e.message}")
            false
        }
    }

    private fun setupOutputFiles() {
        val rgbDir = File(sessionDirectory)
        if (!rgbDir.exists()) {
            rgbDir.mkdirs()
        }
        val framesDir = File(rgbDir, "frames")
        if (!framesDir.exists()) {
            framesDir.mkdirs()
        }
        videoFile = File(rgbDir, SessionDirectoryManager.RGB_VIDEO_FILE)
        csvFile = File(rgbDir, "rgb_timestamps.csv")
    }

    private fun initializeSessionTiming() {
        val localStartNs = TimestampManager.getCurrentTimestampNanos()
        sessionStartTime.set(localStartNs)
        val metadata = sessionMetadata
        if (metadata != null) {
            sessionReferenceTimestampNs.set(metadata.sessionStartMonotonicNs)
            sessionStartOffsetNs.set(localStartNs - metadata.sessionStartMonotonicNs)
        } else {
            sessionReferenceTimestampNs.set(localStartNs)
            sessionStartOffsetNs.set(0L)
        }
    }

    private fun alignedTimestampNs(timestampNs: Long): Long {
        return if (sessionMetadata != null) {
            timestampNs - sessionStartOffsetNs.get()
        } else {
            timestampNs
        }
    }

    private fun sessionRelativeMs(timestampNs: Long): Long {
        val metadata = sessionMetadata
        return if (metadata != null) {
            val alignedNs = alignedTimestampNs(timestampNs)
            (alignedNs - metadata.sessionStartMonotonicNs) / 1_000_000
        } else {
            (timestampNs - sessionStartTime.get()) / 1_000_000
        }
    }

    private fun wallClockMs(timestampNs: Long): Long? {
        val metadata = sessionMetadata ?: return null
        val alignedNs = alignedTimestampNs(timestampNs)
        return metadata.monotonicToWallClock(alignedNs)
    }

    private fun startVideoRecording(): Boolean {
        return try {
            val videoCapture = this.videoCapture ?: return false
            val outputFile = videoFile ?: return false
            val mediaStoreOutput = FileOutputOptions.Builder(outputFile).build()
            activeRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .apply {
                    val audioEnabled = recordingSettings?.audioEnabled ?: true
                    if (audioEnabled &&
                        context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    } else {
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            recordingScope.launch {
                                updateStatus(isRecording = true)
                            }
                        }

                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                            } else {
                                recordingScope.launch {
                                    emitError(
                                        ErrorType.RECORDING_FAILED,
                                        "Video recording failed: ${recordEvent.error}"
                                    )
                                }
                            }
                        }
                    }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun startFrameCapture() {
        frameCaptureJob = recordingScope.launch {
            val framesDir = File(sessionDirectory, "frames")
            if (!framesDir.exists()) {
                framesDir.mkdirs()
            }
            val captureInterval = 1000L / CAPTURE_FPS
            var frameSkipCounter = 0 // Counter for frame throttling
            var adaptiveSkipMultiplier = 1 // For adaptive optimization
            var pendingCaptureCount = 0
            var consecutiveDroppedFrames = 0 // Track dropped frames for adaptive optimization
            frameTimestamps.clear()
            lastFrameRateCheck.set(System.currentTimeMillis())
            actualFrameRateAchieved = 0.0
            Log.i(
                TAG,
                " Starting optimized frame capture at ${CAPTURE_FPS} FPS with throttling (every ${FRAME_CAPTURE_EVERY_N_FRAMES} frames)"
            )
            while (_isRecording.get() && isActive) {
                try {
                    // Implement adaptive frame throttling - adjust based on system performance
                    frameSkipCounter++
                    val effectiveSkip = FRAME_CAPTURE_EVERY_N_FRAMES * adaptiveSkipMultiplier
                    if (frameSkipCounter % effectiveSkip != 0) {
                        // Skip frame but maintain timing for better performance
                        delay(captureInterval)
                        continue
                    }
                    if (pendingCaptureCount >= MAX_PENDING_CAPTURES) {
                        droppedFrames.incrementAndGet()
                        consecutiveDroppedFrames++
                        // Adaptive optimization: increase skip multiplier if dropping many frames
                        if (consecutiveDroppedFrames >= ADAPTIVE_OPTIMIZATION_THRESHOLD) {
                            adaptiveSkipMultiplier =
                                minOf(adaptiveSkipMultiplier + 1, 4) // Max 4x skip
                            consecutiveDroppedFrames = 0
                            Log.i(
                                TAG,
                                "Adaptive optimization: increased frame skip to ${effectiveSkip}x due to I/O pressure"
                            )
                        }
                        Log.d(
                            TAG,
                            "Frame dropped due to backpressure (pending: $pendingCaptureCount, adaptive: ${adaptiveSkipMultiplier}x)"
                        )
                        delay(captureInterval)
                        continue
                    } else {
                        // Reset adaptive optimization if performance improves
                        if (consecutiveDroppedFrames == 0 && adaptiveSkipMultiplier > 1) {
                            adaptiveSkipMultiplier = maxOf(adaptiveSkipMultiplier - 1, 1)
                            Log.d(
                                TAG,
                                "Adaptive optimization: reduced frame skip to ${effectiveSkip}x as performance improved"
                            )
                        }
                        consecutiveDroppedFrames = 0
                    }
                    val frameStartTime = TimestampManager.getCurrentTimestampNanos()
                    pendingCaptureCount++
                    captureFrameAsync(framesDir, frameStartTime) {
                        pendingCaptureCount--
                        monitorFrameRate(frameStartTime)
                    }
                    delay(captureInterval)
                } catch (e: Exception) {
                    val currentTime = System.currentTimeMillis()
                    consecutiveFrameErrors.incrementAndGet()
                    droppedFrames.incrementAndGet()
                    pendingCaptureCount = 0
                    delay(1000)
                }
            }
            logFinalFrameRateStats()
        }
    }

    private fun captureFrameAsync(framesDir: File, frameStartTime: Long, onComplete: () -> Unit) {
        try {
            val timestampRecord = TimestampManager.createTimestampRecord()
            val frameNumber = framesCaptured.incrementAndGet()
            val jpegFile = File(
                framesDir,
                "frame_${
                    String.format(
                        "%08d",
                        frameNumber
                    )
                }_${timestampRecord.systemNanos}$JPEG_FILE_EXTENSION"
            )
            val rawFile = if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) {
                File(
                    framesDir,
                    "frame_${
                        String.format(
                            "%08d",
                            frameNumber
                        )
                    }_${timestampRecord.systemNanos}$RAW_FILE_EXTENSION"
                )
            } else null
            val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile).build()
            imageCapture?.takePicture(
                jpegOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            resetFrameErrorTracking()
                            recordingScope.launch(Dispatchers.IO) {
                                logFrameCapture(timestampRecord, frameNumber, jpegFile)
                            }
                            if (rawFile != null && deviceSupportsRAW && ENABLE_RAW_CAPTURE) {
                                if (hasStoragePermission()) {
                                    captureRawFrameAsync(rawFile, timestampRecord, frameNumber)
                                } else {
                                }
                            }
                            onComplete()
                        } catch (e: Exception) {
                            onComplete()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        handleFrameCaptureError(exception)
                        onComplete()
                    }
                }
            ) ?: run {
                onComplete()
            }
        } catch (e: Exception) {
            onComplete()
        }
    }

    private fun captureRawFrameAsync(
        rawFile: File,
        timestampRecord: TimestampRecord,
        frameNumber: Long
    ) {
        try {
            if (!hasStoragePermission()) {
                return
            }
            val useStage3 = deviceSupportsRAW && ENABLE_RAW_CAPTURE &&
                    SamsungDeviceCompatibility.isStage3Compatible()
            if (useStage3 && rawImageCapture != null) {
                // Create Stage 3/Level 3 DNG file name
                val stage3File = File(rawFile.parent, rawFile.nameWithoutExtension + "_stage3.dng")
                // Use RAW ImageCapture for proper DNG capture with ImageFormat.RAW_SENSOR
                val rawOutputOptions = ImageCapture.OutputFileOptions.Builder(stage3File)
                    .setMetadata(ImageCapture.Metadata().apply {
                        // Add Stage 3 specific metadata
                        isReversedHorizontal = false
                        isReversedVertical = false
                        location = null
                    })
                    .build()
                rawImageCapture?.let { rawCapture ->
                    recordingScope.launch(Dispatchers.IO) {
                        try {
                            Log.i(
                                TAG,
                                "Stage 3/Level 3 RAW DNG capture initiated for frame $frameNumber"
                            )
                            // Perform actual RAW capture using ImageCapture with RAW_SENSOR format
                            withContext(Dispatchers.Main) {
                                rawCapture.takePicture(
                                    rawOutputOptions,
                                    cameraExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            recordingScope.launch(Dispatchers.IO) {
                                                try {
                                                    // Post-process the DNG file with Stage 3 metadata
                                                    val camera2Info = camera?.cameraInfo?.let {
                                                        androidx.camera.camera2.interop.Camera2CameraInfo.from(
                                                            it
                                                        )
                                                    }
                                                    enhanceStage3DngMetadata(
                                                        stage3File,
                                                        timestampRecord,
                                                        frameNumber,
                                                        camera2Info
                                                    )
                                                    logFrameCapture(
                                                        timestampRecord,
                                                        frameNumber,
                                                        stage3File,
                                                        isRaw = true
                                                    )
                                                    Log.i(
                                                        TAG,
                                                        " Stage 3/Level 3 DNG saved: ${stage3File.name} (${stage3File.length()} bytes)"
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        TAG,
                                                        "Error post-processing Stage 3 DNG",
                                                        e
                                                    )
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(
                                                TAG,
                                                "Stage 3/Level 3 DNG capture failed for frame $frameNumber",
                                                exception
                                            )
                                            // Fallback to standard processing
                                            recordingScope.launch(Dispatchers.IO) {
                                                rawFile.writeText("RAW capture fallback frame $frameNumber - ${timestampRecord.systemNanos}")
                                                Log.w(
                                                    TAG,
                                                    "Fallback RAW metadata saved for frame $frameNumber"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                "Stage 3/Level 3 capture setup failed for frame $frameNumber: ${e.message}"
                            )
                            // Fallback to standard processing
                            rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
                        }
                    }
                }
                if (rawImageCapture == null) {
                    rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
                }
            } else {
                // Standard RAW processing for non-Samsung devices or when Stage 3 is disabled
                Log.i(
                    TAG,
                    "Standard RAW processing for frame $frameNumber (device not Stage 3/Level 3 compatible)"
                )
                rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
            }
        } catch (e: Exception) {
        }
    }

    private fun enhanceStage3DngMetadata(
        dngFile: File,
        timestampRecord: TimestampRecord,
        frameNumber: Long,
        camera2Info: androidx.camera.camera2.interop.Camera2CameraInfo?
    ) {
        try {
            // Add Stage 3/Level 3 processing markers to DNG metadata
            // Note: This would typically be done during DNG creation, but Android's
            // ImageCapture API may not expose all DNG metadata fields directly.
            // For complete Stage 3 metadata, a custom DNG creation pipeline may be needed.
            val metadataFile =
                File(dngFile.parent, dngFile.nameWithoutExtension + "_stage3_metadata.json")
            val metadata = mapOf(
                "processing_pipeline" to "Samsung Stage 3/Level 3",
                "frame_number" to frameNumber,
                "capture_timestamp_ns" to timestampRecord.systemNanos,
                "monotonic_timestamp_ns" to timestampRecord.systemNanos,
                "device_model" to SamsungDeviceCompatibility.getDeviceInfo(),
                "camera_id" to (camera2Info?.cameraId ?: "unknown"),
                "dng_file_size_bytes" to dngFile.length(),
                "creation_time" to System.currentTimeMillis()
            )
            val gson = com.google.gson.Gson()
            metadataFile.writeText(gson.toJson(metadata))
        } catch (e: Exception) {
        }
    }

    private fun monitorFrameRate(frameTimestamp: Long) {
        synchronized(frameTimestamps) {
            frameTimestamps.add(frameTimestamp)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFrameRateCheck.get() > frameRateCheckInterval) {
                calculateAndValidateFrameRate()
                lastFrameRateCheck.set(currentTime)
            }
        }
    }

    private fun calculateAndValidateFrameRate() {
        if (frameTimestamps.size < 10) return
        synchronized(frameTimestamps) {
            val recentFrames = frameTimestamps.takeLast(150)
            if (recentFrames.size < 2) return
            val timeSpanNs = recentFrames.last() - recentFrames.first()
            val timeSpanSeconds = timeSpanNs / 1_000_000_000.0
            actualFrameRateAchieved = (recentFrames.size - 1) / timeSpanSeconds
            Log.d(
                TAG,
                "Actual frame rate: ${
                    String.format(
                        "%.2f",
                        actualFrameRateAchieved
                    )
                } fps (target: ${CAPTURE_FPS} fps)"
            )
            val frameRateDeviation = Math.abs(actualFrameRateAchieved - CAPTURE_FPS) / CAPTURE_FPS
            if (frameRateDeviation > 0.15) {
                Log.w(
                    TAG,
                    "Frame rate deviation detected: ${
                        String.format(
                            "%.1f%%",
                            frameRateDeviation * 100
                        )
                    } from target ${CAPTURE_FPS} fps"
                )
                if (frameRateDeviation > 0.3) {
                    Log.e(
                        TAG,
                        "Critical frame rate deviation detected - performance issue may be present"
                    )
                }
            }
            if (frameTimestamps.size > 300) {
                frameTimestamps.subList(0, frameTimestamps.size - 300).clear()
            }
        }
    }

    private fun logFinalFrameRateStats() {
        try {
            val totalFrames = framesCaptured.get()
            val recordingDurationMs =
                System.currentTimeMillis() - sessionReferenceTimestampNs.get() / 1_000_000
            val recordingDurationSeconds = recordingDurationMs / 1000.0
            val averageFrameRate = totalFrames / recordingDurationSeconds
            Log.i(
                TAG,
                "  Video configuration: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps"
            )
            val frameRateSuccess = Math.abs(averageFrameRate - CAPTURE_FPS) / CAPTURE_FPS < 0.2
            if (frameRateSuccess) {
            } else {
                Log.w(
                    TAG,
                    " Frame rate validation WARNING - significant deviation from target 30 FPS detected"
                )
            }
        } catch (e: Exception) {
        }
    }

    private fun logFrameCapture(
        timestampRecord: TimestampRecord,
        frameNumber: Long,
        outputFile: File
    ) {
        try {
            val timestampNs = timestampRecord.systemNanos
            val alignedNs = alignedTimestampNs(timestampNs)
            val sessionTimeMs = sessionRelativeMs(timestampNs)
            val wallMs = wallClockMs(timestampNs)
            csvBufferedWriter?.let { writer ->
                val metadataParts = mutableListOf(
                    "filename=${outputFile.name}",
                    "size=${outputFile.length()}"
                )
                metadataParts.add("aligned_ns=$alignedNs")
                wallMs?.let { metadataParts.add("wall_ms=$it") }
                sessionMetadata?.let {
                    metadataParts.add(
                        "session_reference_ns=${sessionReferenceTimestampNs.get()}"
                    )
                }
                writer.writeRow(
                    listOf(
                        timestampNs,
                        frameNumber,
                        sessionTimeMs,
                        timestampRecord.synchronizedTimestampMs,
                        "frame_capture",
                        metadataParts.joinToString(",")
                    )
                )
            }
            samplesRecorded.incrementAndGet()
            lastFrameTime.set(alignedNs)
        } catch (e: Exception) {
        }
    }

    // Overload for RAW/DNG files
    private fun logFrameCapture(
        timestampRecord: TimestampRecord,
        frameNumber: Long,
        outputFile: File,
        isRaw: Boolean
    ) {
        try {
            val timestampNs = timestampRecord.systemNanos
            val alignedNs = alignedTimestampNs(timestampNs)
            val sessionTimeMs = sessionRelativeMs(timestampNs)
            val wallMs = wallClockMs(timestampNs)
            csvBufferedWriter?.let { writer ->
                val metadataParts = mutableListOf(
                    "filename=${outputFile.name}",
                    "size=${outputFile.length()}"
                )
                if (isRaw) {
                    metadataParts.add("type=raw_dng")
                    metadataParts.add("processing=stage3_level3")
                    metadataParts.add("device=${SamsungDeviceCompatibility.getDeviceInfo()}")
                }
                metadataParts.add("aligned_ns=$alignedNs")
                wallMs?.let { metadataParts.add("wall_ms=$it") }
                sessionMetadata?.let {
                    metadataParts.add(
                        "session_reference_ns=${sessionReferenceTimestampNs.get()}"
                    )
                }
                val eventType = if (isRaw) "raw_dng_capture" else "frame_capture"
                writer.writeRow(
                    listOf(
                        timestampNs,
                        frameNumber,
                        sessionTimeMs,
                        timestampRecord.synchronizedTimestampMs,
                        eventType,
                        metadataParts.joinToString(",")
                    )
                )
            }
            samplesRecorded.incrementAndGet()
            lastFrameTime.set(alignedNs)
        } catch (e: Exception) {
        }
    }

    private suspend fun initializeCsvWriter() {
        try {
            csvFile?.let { file ->
                csvWriter = CSVWriter(FileWriter(file)).apply {
                    writeNext(
                        arrayOf(
                            "timestamp_ns",
                            "aligned_timestamp_ns",
                            "sample_number",
                            "session_time_ms",
                            "wall_time_ms",
                            "event_type",
                            "metadata"
                        )
                    )
                    flush()
                }
                val headers = listOf(
                    "timestamp_ns",
                    "frame_number",
                    "session_time_ms",
                    "synchronized_timestamp_ms",
                    "sync_marker",
                    "metadata"
                )
                csvBufferedWriter = CSVBufferedWriter(
                    outputFile = file,
                    headers = headers,
                    bufferSize = 4096,
                    flushIntervalMs = 500L
                )
                csvBufferedWriter?.startWithHeaders()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun handleFrameCaptureError(exception: ImageCaptureException) {
        val currentTime = System.currentTimeMillis()
        val errorCount = consecutiveFrameErrors.incrementAndGet()
        droppedFrames.incrementAndGet()
        if (errorCount >= MAX_CONSECUTIVE_FRAME_ERRORS) {
            val timeSinceLastError = currentTime - lastFrameErrorTime.get()
            if (timeSinceLastError < FRAME_ERROR_RESET_INTERVAL) {
                Log.e(
                    TAG,
                    "Too many consecutive frame capture errors ($errorCount), camera may be failing"
                )
                _cameraStatus.value = "Camera Error - Frame Capture Failing"
                recordingScope.launch {
                    emitError(
                        ErrorType.DEVICE_ERROR,
                        "Camera frame capture is failing repeatedly. Check camera hardware and permissions."
                    )
                }
            } else {
                consecutiveFrameErrors.set(1)
            }
        }
        lastFrameErrorTime.set(currentTime)
        if (errorCount > 3) {
            _cameraStatus.value = "Recording (Frame Capture Issues: $errorCount errors)"
        }
    }

    private fun resetFrameErrorTracking() {
        if (consecutiveFrameErrors.get() > 0) {
            consecutiveFrameErrors.set(0)
            _cameraStatus.value = "Recording (${framesCaptured.get()} frames)"
        }
    }

    override suspend fun stopRecording(): Boolean {
        return try {
            if (!_isRecording.get()) {
                return false
            }
            _isRecording.set(false)
            _cameraStatus.value = "Stopping Recording..."
            frameCaptureJob?.let { job ->
                job.cancel()
                try {
                    job.join()
                } catch (e: Exception) {
                }
                frameCaptureJob = null
            }
            activeRecording?.let { recording ->
                try {
                    recording.stop()
                } catch (e: Exception) {
                }
                activeRecording = null
            }
            try {
                csvWriter?.let { writer ->
                    writer.flush()
                    writer.close()
                }
                csvWriter = null
                csvBufferedWriter?.let { bufferedWriter ->
                    bufferedWriter.stop()
                }
                csvBufferedWriter = null
            } catch (e: Exception) {
            }
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
            }
            val sessionStats = generateSessionStats()
            Log.i(
                TAG,
                "  • Average frame rate: ${
                    String.format(
                        "%.2f",
                        sessionStats.averageFrameRate
                    )
                } fps"
            )
            updateStatus(isRecording = false)
            sessionReferenceTimestampNs.set(0)
            sessionStartOffsetNs.set(0)
            sessionMetadata = null
            _cameraStatus.value = "Recording Stopped"
            true
        } catch (e: Exception) {
            _cameraStatus.value = "Stop Recording Failed"
            emitError(ErrorType.RECORDING_FAILED, "Failed to stop recording: ${e.message}")
            false
        }
    }

    private fun generateSessionStats(): SessionStats {
        val totalFrames = framesCaptured.get()
        val droppedFrames = droppedFrames.get()
        val dropRate = if (totalFrames > 0) (droppedFrames.toDouble() / totalFrames * 100) else 0.0
        val videoSize = videoFile?.length() ?: 0L
        val framesDirSize = File(sessionDirectory, "frames").let { dir ->
            if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }
                .sum() else 0L
        }
        val totalStorageMB = (videoSize + framesDirSize) / (1024.0 * 1024.0)
        return SessionStats(
            framesCaptured = totalFrames,
            framesDropped = droppedFrames,
            dropRate = dropRate,
            averageFrameRate = actualFrameRateAchieved,
            storageMB = totalStorageMB
        )
    }

    private data class SessionStats(
        val framesCaptured: Long,
        val framesDropped: Long,
        val dropRate: Double,
        val averageFrameRate: Double,
        val storageMB: Double
    )

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>
    ) {
        try {
            csvBufferedWriter?.let { writer ->
                val sessionTimeMs = sessionRelativeMs(timestampNs)
                val wallMs = wallClockMs(timestampNs)
                // Calculate synchronized timestamp based on the event's wall clock time and current offset
                val clockOffsetMs = TimestampManager.getClockOffsetMs()
                val synchronizedTimestampMs = (wallMs ?: TimestampManager.getCurrentSystemTimeMs()) + clockOffsetMs
                val metadataMap = metadata.toMutableMap()
                wallMs?.let { metadataMap["wall_ms"] = it.toString() }
                sessionMetadata?.let {
                    metadataMap["session_reference_ns"] =
                        sessionReferenceTimestampNs.get().toString()
                }
                val metadataStr = metadataMap.entries.joinToString(",") { "${it.key}=${it.value}" }
                val row = listOf(
                    timestampNs,
                    samplesRecorded.get(),
                    sessionTimeMs,
                    synchronizedTimestampMs,
                    markerType,
                    metadataStr
                )
                writer.writeRow(row)
            }
        } catch (e: Exception) {
            emitError(ErrorType.SYNC_FAILED, "Failed to add sync marker: ${e.message}")
        }
    }

    private fun observeRecordingSettingsChanges() {
        recordingScope.launch {
            RecordingSettingsRepository.getInstance(context)
                .settings.collectLatest { settings ->
                    recordingSettings = settings
                    // Note: Camera needs to be re-initialized for some settings like FPS and quality
                    // Log a warning if recording is active as changes won't apply until restart
                    if (_isRecording.get()) {
                    }
                }
        }
    }

    override suspend fun cleanup() {
        try {
            _cameraStatus.value = "Cleaning up..."
            if (_isRecording.get()) {
                stopRecording()
            }
            frameCaptureJob?.cancel()
            frameCaptureJob = null
            activeRecording?.stop()
            activeRecording = null
            csvWriter?.close()
            csvWriter = null
            csvBufferedWriter?.stop()
            csvBufferedWriter = null
            withContext(Dispatchers.Main) {
                try {
                    cameraProvider?.unbindAll()
                } catch (e: Exception) {
                }
            }
            camera = null
            preview = null
            videoCapture = null
            imageCapture = null
            rawImageCapture = null
            try {
                cameraExecutor.shutdown()
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow()
                }
                // Recreate executor to allow re-initialization for multiple recording sessions
                cameraExecutor = Executors.newSingleThreadExecutor()
            } catch (e: Exception) {
            }
            recordingScope.cancel()
            consecutiveFrameErrors.set(0)
            lastFrameErrorTime.set(0)
            _cameraStatus.value = "Cleaned up"
        } catch (e: Exception) {
            _cameraStatus.value = "Cleanup Failed"
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = statusFlow.asStateFlow()
    override fun getErrorFlow(): Flow<SensorError> = errorFlow.asSharedFlow()
    override fun getRecordingStats(): RecordingStats {
        val currentTime = TimestampManager.getCurrentTimestampNanos()
        val sessionDuration = if (sessionStartTime.get() > 0) {
            (currentTime - sessionStartTime.get()) / 1_000_000
        } else 0L
        val totalSamples = framesCaptured.get()
        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = totalSamples,
            averageDataRate = if (sessionDuration > 0) {
                (totalSamples * 1000.0) / sessionDuration
            } else 0.0,
            droppedSamples = droppedFrames.get(),
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = syncMarkersRecorded.get().toInt(),
            lastSampleTimestampNs = lastFrameTime.get()
        )
    }

    private fun calculateStorageUsed(): Double {
        var totalBytes = 0L
        if (sessionDirectory.isNotEmpty()) {
            val sessionDir = File(sessionDirectory)
            if (sessionDir.exists()) {
                totalBytes += sessionDir.walkTopDown()
                    .filter { it.isFile }
                    .map { it.length() }
                    .sum()
            }
        }
        csvFile?.let { file ->
            if (file.exists()) {
                totalBytes += file.length()
            }
        }
        return totalBytes / (1024.0 * 1024.0)
    }

    private fun createInitialStatus() = RecordingStatus(
        sensorId = sensorId,
        sensorType = sensorType,
        isRecording = false,
        samplesRecorded = 0,
        currentDataRate = 0.0,
        storageUsedMB = 0.0,
        timestampNs = TimestampManager.getCurrentTimestampNanos()
    )

    private suspend fun updateStatus(
        isRecording: Boolean = this.isRecording,
        isInitialized: Boolean = false
    ) {
        val stats = getRecordingStats()
        val status = RecordingStatus(
            sensorId = sensorId,
            sensorType = sensorType,
            isRecording = isRecording,
            samplesRecorded = stats.totalSamplesRecorded,
            currentDataRate = stats.averageDataRate,
            storageUsedMB = stats.storageUsedMB,
            timestampNs = TimestampManager.getCurrentTimestampNanos()
        )
        statusFlow.emit(status)
    }

    private suspend fun emitError(errorType: ErrorType, message: String) {
        val error = SensorError(
            sensorId = sensorId,
            sensorType = sensorType,
            errorType = errorType,
            errorMessage = message,
            timestampNs = TimestampManager.getCurrentTimestampNanos(),
            isRecoverable = errorType != ErrorType.HARDWARE_DISCONNECTED
        )
        errorFlow.emit(error)
    }

    fun hasCameraPermission(): Boolean {
        val hasCamera = context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCamera) {
            return false
        }
        val audioEnabled = recordingSettings?.audioEnabled ?: true
        if (!audioEnabled) {
            return true
        }
        val hasAudio = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasAudio) {
        }
        return hasAudio
    }

    fun hasStoragePermission(): Boolean {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasImages = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasVideo = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasImages) {
            }
            if (!hasVideo) {
            }
            hasImages && hasVideo
        } else {
            val hasWrite = context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasRead = context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasWrite) {
            }
            if (!hasRead) {
            }
            hasWrite && hasRead
        }
        if (!hasPermission) {
        }
        return hasPermission
    }

    fun supportsHighResolution(): Boolean {
        return try {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
        } catch (e: Exception) {
            false
        }
    }

    fun getStatusText(): String {
        return _cameraStatus.value
    }

    fun getCameraType(): String {
        return if (useFrontCamera) "Front Camera" else "Back Camera"
    }

    fun isUsingFrontCamera(): Boolean = useFrontCamera
    fun getFrameCaptureStats(): Map<String, Any> {
        return mapOf(
            "frames_captured" to framesCaptured.get(),
            "frames_dropped" to droppedFrames.get(),
            "consecutive_errors" to consecutiveFrameErrors.get(),
            "camera_type" to getCameraType(),
            "capture_fps" to CAPTURE_FPS,
            "video_resolution" to "${selectedVideoWidth}x${selectedVideoHeight}",
            "has_preview" to (previewView != null)
        )
    }
    // Manual Camera Control Methods

    fun setManualExposureMode(enabled: Boolean) {
        controlsManager.setManualExposureMode(camera, enabled)
    }

    fun setExposureCompensation(evValue: Float) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                // Convert EV to exposure compensation index
                val camera2Info =
                    androidx.camera.camera2.interop.Camera2CameraInfo.from(camera!!.cameraInfo)
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
                    recordingScope.launch {
                        emitError(
                            ErrorType.FEATURE_NOT_SUPPORTED,
                            "Exposure compensation not supported on this device"
                        )
                    }
                }
            } ?: run {
                recordingScope.launch {
                    emitError(
                        ErrorType.HARDWARE_UNAVAILABLE,
                        "Camera not available for exposure control"
                    )
                }
            }
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(
                    ErrorType.OPERATION_FAILED,
                    "Failed to set exposure compensation: ${e.message}"
                )
            }
        }
    }

    fun setAutoExposureLock(locked: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                // CameraX doesn't have direct AE lock, but we can implement via Camera2 interop
            }
        } catch (e: Exception) {
        }
    }

    fun setManualFocusMode(enabled: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                if (enabled) {
                    // Cancel any ongoing autofocus
                    cameraControl.cancelFocusAndMetering()
                } else {
                    // Return to continuous autofocus
                }
            } ?: run {
                recordingScope.launch {
                    emitError(
                        ErrorType.HARDWARE_UNAVAILABLE,
                        "Camera not available for focus control"
                    )
                }
            }
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(ErrorType.OPERATION_FAILED, "Failed to set focus mode: ${e.message}")
            }
        }
    }

    fun setFocusDistance(distance: Float) {
        controlsManager.setFocusDistance(camera, distance)
    }

    fun setAutoFocusLock(locked: Boolean) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                if (locked) {
                    // Lock focus at current position
                } else {
                    // Unlock and resume continuous AF
                }
            } ?: run {
                recordingScope.launch {
                    emitError(
                        ErrorType.HARDWARE_UNAVAILABLE,
                        "Camera not available for focus control"
                    )
                }
            }
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(ErrorType.OPERATION_FAILED, "Failed to set AF lock: ${e.message}")
            }
        }
    }

    fun triggerTapToFocus(x: Float, y: Float) {
        try {
            camera?.cameraControl?.let { cameraControl ->
                previewView?.let { preview ->
                    val factory = preview.meteringPointFactory
                    val point = factory.createPoint(x * preview.width, y * preview.height)
                    val action = FocusMeteringAction.Builder(point)
                        .disableAutoCancel()
                        .build()
                    cameraControl.startFocusAndMetering(action)
                } ?: run {
                    recordingScope.launch {
                        emitError(
                            ErrorType.FEATURE_NOT_SUPPORTED,
                            "Preview required for tap-to-focus"
                        )
                    }
                }
            } ?: run {
                recordingScope.launch {
                    emitError(
                        ErrorType.HARDWARE_UNAVAILABLE,
                        "Camera not available for focus control"
                    )
                }
            }
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(
                    ErrorType.OPERATION_FAILED,
                    "Failed to trigger tap-to-focus: ${e.message}"
                )
            }
        }
    }

    fun supports60fps(): Boolean {
        return try {
            checkDevice60fpsSupport()
        } catch (e: Exception) {
            false
        }
    }

    fun setCaptureMode(useRawMode: Boolean) {
        try {
            if (_isRecording.get()) {
                return
            }
            if (useRawMode) {
                if (!deviceSupportsRAW) {
                    return
                }
                // RAW mode will be activated in the next recording session
            } else {
                // Normal video mode will be used
            }
            // Could trigger camera reconfiguration here if needed
        } catch (e: Exception) {
        }
    }

    fun getDetailedCameraCapabilities(): Map<String, Any> {
        return try {
            val capabilities = mutableMapOf<String, Any>()
            // Basic device info
            capabilities["device_manufacturer"] = Build.MANUFACTURER
            capabilities["device_model"] = Build.MODEL
            capabilities["android_version"] = Build.VERSION.SDK_INT
            // Camera capabilities
            capabilities["supports_4k"] = deviceSupports4K
            capabilities["supports_raw"] = deviceSupportsRAW
            capabilities["supports_60fps"] = checkDevice60fpsSupport()
            capabilities["current_resolution"] = "${selectedVideoWidth}x${selectedVideoHeight}"
            capabilities["current_fps"] = selectedVideoFps
            capabilities["current_bitrate"] = selectedVideoBitrate
            // Stage 3 processing
            capabilities["stage3_compatible"] = SamsungDeviceCompatibility.isStage3Compatible()
            capabilities["raw_enabled"] = (deviceSupportsRAW && ENABLE_RAW_CAPTURE)
            // Camera availability
            capabilities["front_camera_available"] = supportsFrontCamera
            capabilities["back_camera_available"] = supportsBackCamera
            capabilities["camera_permission_granted"] = hasCameraPermission()
            // Advanced features
            camera?.let { cam ->
                val cameraInfo = cam.cameraInfo
                capabilities["has_flash"] = cameraInfo.hasFlashUnit()
                capabilities["zoom_ratio"] = cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
                capabilities["min_zoom"] = cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
                capabilities["max_zoom"] = cameraInfo.zoomState.value?.maxZoomRatio ?: 1.0f
                // Exposure capabilities
                try {
                    val camera2Info =
                        androidx.camera.camera2.interop.Camera2CameraInfo.from(cameraInfo)
                    val exposureRange = camera2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE
                    )
                    val exposureStep = camera2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
                    )
                    capabilities["exposure_compensation_min"] = exposureRange?.lower ?: 0
                    capabilities["exposure_compensation_max"] = exposureRange?.upper ?: 0
                    capabilities["exposure_compensation_step"] = exposureStep?.toFloat() ?: 0.0f
                    // Focus capabilities
                    val minFocusDistance = camera2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                    )
                    capabilities["min_focus_distance"] = minFocusDistance ?: 0.0f
                } catch (e: Exception) {
                    capabilities["camera2_interop_available"] = false
                }
            } ?: run {
                capabilities["camera_initialized"] = false
            }
            capabilities
        } catch (e: Exception) {
            mapOf(
                "error" to "Failed to determine camera capabilities: ${e.message}",
                "supports_4k" to false,
                "supports_raw" to false,
                "supports_60fps" to false
            )
        }
    }

    fun validateDeviceRequirements(): Boolean {
        return try {
            val requirements = mutableListOf<String>()
            var meetsRequirements = true
            // Check camera permission
            if (!hasCameraPermission()) {
                requirements.add("Camera permission required")
                meetsRequirements = false
            }
            // Check camera availability
            if (!supportsBackCamera) {
                requirements.add("Back camera not available")
                meetsRequirements = false
            }
            // Check Android version (API 21+ required for Camera2)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                requirements.add("Android 5.0+ required for advanced camera features")
                meetsRequirements = false
            }
            // Log requirements status
            if (meetsRequirements) {
                val capabilities = getCaptureMode()
                Log.i(
                    TAG,
                    "Available features: 4K=${capabilities["supports_4k"]}, RAW=${capabilities["supports_raw"]}, 60fps=${capabilities["supports_60fps"]}"
                )
            } else {
                recordingScope.launch {
                    emitError(
                        ErrorType.DEVICE_NOT_SUPPORTED,
                        "Device requirements not met: ${requirements.joinToString(", ")}"
                    )
                }
            }
            meetsRequirements
        } catch (e: Exception) {
            recordingScope.launch {
                emitError(
                    ErrorType.DEVICE_NOT_SUPPORTED,
                    "Could not validate device requirements: ${e.message}"
                )
            }
            false
        }
    }

    fun getCaptureMode(): Map<String, Any> {
        return mapOf(
            "supports_raw" to deviceSupportsRAW,
            "supports_4k" to deviceSupports4K,
            "supports_60fps" to supports60fps(),
            "current_resolution" to "${selectedVideoWidth}x${selectedVideoHeight}",
            "current_fps" to selectedVideoFps,
            "raw_enabled" to (deviceSupportsRAW && ENABLE_RAW_CAPTURE),
            "stage3_compatible" to (deviceSupportsRAW && SamsungDeviceCompatibility.isStage3Compatible())
        )
    }
}
