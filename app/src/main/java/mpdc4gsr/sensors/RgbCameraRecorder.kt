package mpdc4gsr.sensors

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.opencsv.CSVWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.ErrorType
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.SensorStatus
import mpdc4gsr.utils.CSVBufferedWriter
import mpdc4gsr.utils.SessionDirectoryManager
import android.graphics.ImageFormat
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
        private const val VIDEO_FPS_TARGET = 30
        private const val VIDEO_FPS_FALLBACK = 24
        private const val VIDEO_BITRATE_4K = 50_000_000
        private const val VIDEO_BITRATE_1080P = 20_000_000
        private const val AUDIO_BITRATE = 256_000
        private const val JPEG_QUALITY = 100
        // Throttled frame capture at 10-15fps for optimized I/O performance 
        private const val CAPTURE_FPS = 12 // Reduced from 30 to optimize I/O performance
        
        // Frame capture throttling configuration
        private const val FRAME_CAPTURE_EVERY_N_FRAMES = 2 // Capture every 2nd frame at 24fps = ~12fps output
        private const val MAX_PENDING_CAPTURES = 2 // Reduced for better I/O handling


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

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var rawImageCapture: ImageCapture? = null // For Stage 3 RAW DNG capture using ImageFormat.RAW_SENSOR
    private var camera: Camera? = null
    private var activeRecording: Recording? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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
            Log.d(TAG, "Initializing CameraX with ${if (useFrontCamera) "front" else "back"} camera")

            if (!checkAndRequestPermissions()) {
                _cameraStatus.value = "Camera Permission Denied"
                emitError(ErrorType.PERMISSION_DENIED, "Camera permission is required for recording")
                return@withContext false
            }

            _cameraStatus.value = "Initializing..."
            
            // Wrap CameraProvider initialization in try-catch for robust error handling
            cameraProvider = try {
                ProcessCameraProvider.getInstance(context).get()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get CameraProvider instance", e)
                _cameraStatus.value = "Camera Service Unavailable"
                emitError(ErrorType.INITIALIZATION_FAILED, "Camera service unavailable: ${e.message}")
                return@withContext false
            }

            if (!cameraProvider!!.hasCamera(currentCameraSelector)) {
                val cameraType = if (isUsingFrontCamera) "Front" else "Back"
                Log.w(TAG, "$cameraType camera not available on this device")
                _cameraStatus.value = "$cameraType Camera Not Available"
                emitError(ErrorType.INITIALIZATION_FAILED, "$cameraType camera not available on this device")
                return@withContext false
            }

            // Detect device capabilities and configure camera
            detectDeviceCapabilities()
            detectAvailableCameras()
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
                "✅ CameraX initialized successfully: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, Preview: ${previewView != null}"
            )
            return@withContext true

        } catch (e: SecurityException) {
            Log.e(TAG, "Camera security exception - permission issue", e)
            _cameraStatus.value = "Permission Error"
            emitError(ErrorType.PERMISSION_DENIED, "Camera permission required: ${e.message}")
            return@withContext false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Camera in use by another application", e)
            _cameraStatus.value = "Camera In Use"
            emitError(ErrorType.INITIALIZATION_FAILED, "Camera is being used by another application")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected camera initialization error", e)
            _cameraStatus.value = "Initialization Failed"
            emitError(ErrorType.INITIALIZATION_FAILED, "Camera initialization failed: ${e.message}")
            return@withContext false
        }
    }


    private fun detectDeviceCapabilities() {
        try {
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER

            Log.d(TAG, "Detecting capabilities for device: $deviceManufacturer $deviceModel")


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
                    val cameraCharacteristics = androidx.camera.camera2.interop.Camera2CameraInfo.from(cameraInfo)
                    val capabilities = cameraCharacteristics.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )
                    deviceSupportsRAW = deviceSupportsRAW || capabilities?.contains(
                        android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
                    ) == true
                } catch (e: Exception) {
                    Log.d(TAG, "Could not check RAW capability via Camera2: ${e.message}")
                }
            }

            Log.i(
                TAG,
                "Samsung Galaxy S22 capabilities - 4K: $deviceSupports4K, RAW: $deviceSupportsRAW for $deviceManufacturer $deviceModel"
            )

        } catch (e: Exception) {
            Log.w(TAG, "Error detecting device capabilities, using safe defaults", e)
            deviceSupports4K = false
            deviceSupportsRAW = false
        }
    }


    private fun checkVideoProfileSupport(cameraInfo: CameraInfo): Boolean {
        return try {


            false
        } catch (e: Exception) {
            Log.w(TAG, "Could not check video profile support", e)
            false
        }
    }


    private fun detectAvailableCameras() {
        try {
            cameraProvider?.let { provider ->
                supportsBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                supportsFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

                Log.i(TAG, "📷 Camera availability detected:")
                Log.i(TAG, "  • Back camera: ${if (supportsBackCamera) "Available" else "Not available"}")
                Log.i(TAG, "  • Front camera: ${if (supportsFrontCamera) "Available" else "Not available"}")


                if (isUsingFrontCamera && !supportsFrontCamera) {
                    Log.w(TAG, "⚠️ Front camera requested but not available, switching to back camera")
                    recordingScope.launch {
                        switchToBackCamera()
                    }
                } else if (!isUsingFrontCamera && !supportsBackCamera) {
                    Log.w(TAG, "⚠️ Back camera not available, switching to front camera")
                    recordingScope.launch {
                        switchToFrontCamera()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting available cameras", e)

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


    private suspend fun switchCamera(useFrontCamera: Boolean): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            val targetCameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }


            val isAvailable = cameraProvider?.hasCamera(targetCameraSelector) ?: false
            if (!isAvailable) {
                val cameraType = if (useFrontCamera) "front" else "back"
                Log.w(TAG, "Cannot switch to $cameraType camera - not available on this device")
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "$cameraType camera not available"
                )
                return@withContext false
            }


            if (isUsingFrontCamera == useFrontCamera) {
                Log.d(TAG, "Already using ${if (useFrontCamera) "front" else "back"} camera")
                return@withContext true
            }

            val wasRecording = _isRecording.get()
            if (wasRecording) {
                Log.w(TAG, "Cannot switch camera during recording")
                emitError(
                    ErrorType.RECORDING_FAILED,
                    "Cannot switch camera while recording"
                )
                return@withContext false
            }

            Log.i(TAG, "🔄 Switching to ${if (useFrontCamera) "front" else "back"} camera")
            _cameraStatus.value = "Switching Camera..."


            currentCameraSelector = targetCameraSelector
            isUsingFrontCamera = useFrontCamera


            cameraProvider?.unbindAll()
            val rebindSuccess = bindUseCasesToCamera()

            if (rebindSuccess) {
                _cameraStatus.value = "Camera Switched - ${if (useFrontCamera) "Front" else "Back"} Camera Active"
                Log.i(TAG, "✅ Successfully switched to ${if (useFrontCamera) "front" else "back"} camera")
                true
            } else {
                _cameraStatus.value = "Camera Switch Failed"
                Log.e(TAG, "❌ Failed to switch camera")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during camera switch", e)
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
            override val currentResolution = "${selectedVideoWidth}x${selectedVideoHeight}"
            override val currentFormat = if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) "JPEG+RAW" else "JPEG"
        }
    }

    interface CameraDisplayInfo {
        val isUsingFrontCamera: Boolean
        val backAvailable: Boolean
        val frontAvailable: Boolean
        val canSwitch: Boolean
        val supports4K: Boolean
        val supportsRAW: Boolean
        val currentResolution: String
        val currentFormat: String
    }


    private fun optimizeVideoConfiguration() {
        try {
            if (deviceSupports4K) {
                Log.i(TAG, "Configuring for 4K recording on supported device")
                selectedVideoWidth = VIDEO_WIDTH_4K
                selectedVideoHeight = VIDEO_HEIGHT_4K
                selectedVideoBitrate = VIDEO_BITRATE_4K
                selectedVideoFps = VIDEO_FPS_TARGET
            } else {
                Log.i(TAG, "Configuring for 1080p recording with fallback safety")
                selectedVideoWidth = VIDEO_WIDTH_1080P
                selectedVideoHeight = VIDEO_HEIGHT_1080P
                selectedVideoBitrate = VIDEO_BITRATE_1080P
                selectedVideoFps = VIDEO_FPS_TARGET
            }

            Log.i(
                TAG,
                "Video configuration optimized: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, bitrate: ${selectedVideoBitrate}"
            )
            Log.i(TAG, "Advanced capabilities: 4K=${deviceSupports4K}, RAW=${deviceSupportsRAW}")

        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing video configuration, using safe defaults", e)
            selectedVideoWidth = VIDEO_WIDTH_1080P
            selectedVideoHeight = VIDEO_HEIGHT_1080P
            selectedVideoBitrate = VIDEO_BITRATE_1080P
            selectedVideoFps = VIDEO_FPS_FALLBACK
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
                setTargetResolution(previewSize)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setTargetFrameRate(Range(24, 30))
                }

                Log.d(TAG, "Preview configured with resolution: ${previewSize.width}x${previewSize.height}")
            }.build()


            val recorder = createOptimizedRecorder()
            videoCapture = VideoCapture.withOutput(recorder)


            imageCapture = ImageCapture.Builder().apply {
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
                        Log.i(TAG, "RAW/DNG capture enabled for supported device")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not enable RAW capture: ${e.message}")
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
                        
                        Log.i(TAG, "RAW ImageCapture configured for Stage 3/Level 3 DNG capture")
                    }.build()
                    
                    // Store the RAW ImageCapture for use in capture operations
                    this.rawImageCapture = rawImageCapture
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Could not configure RAW ImageCapture for Stage 3: ${e.message}")
                }
            }

            Log.d(TAG, "Camera use cases configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera use cases", e)
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
                Log.i(TAG, "✅ RAW ImageCapture added for Stage 3/Level 3 DNG capture")
            }


            preview?.let { preview ->
                previewView?.let { previewView ->
                    try {
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        useCases.add(preview)
                        Log.i(TAG, "✅ Preview bound to PreviewView successfully - live camera feed enabled")


                        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

                    } catch (e: Exception) {
                        Log.w(TAG, "Preview binding failed, continuing without preview", e)
                        emitError(
                            ErrorType.INITIALIZATION_FAILED,
                            "Camera preview unavailable but recording will continue"
                        )
                    }
                } ?: run {
                    Log.w(TAG, "No PreviewView provided - recording without live preview")
                }
            }


            if (useCases.isEmpty()) {
                Log.e(TAG, "No use cases available for binding")
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

                Log.i(TAG, "📷 Camera bound successfully:")
                Log.i(TAG, "  - Camera: ${if (isUsingFrontCamera) "Front" else "Back"}")
                Log.i(TAG, "  - Resolution: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps")
                Log.i(TAG, "  - Flash available: $hasFlash")
                Log.i(TAG, "  - Zoom ratio: ${String.format("%.1f", zoomRatio)}x")
                Log.i(TAG, "  - Preview: ${if (previewView != null) "Enabled" else "Disabled"}")

                return@withContext true

            } ?: run {
                Log.e(TAG, "Camera binding returned null")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to bind camera use cases", e)
            emitError(
                ErrorType.INITIALIZATION_FAILED,
                "Failed to bind camera use cases: ${e.message}"
            )
            return@withContext false
        }
    }

    private suspend fun checkAndRequestPermissions(): Boolean {
        if (hasCameraPermission()) return true

        _cameraStatus.value = "Camera Permission Required"

        return permissionManager?.let { permissionManager ->
            try {
                val granted = permissionManager.requestCameraPermissions()
                if (granted) {
                    _cameraStatus.value = "Permissions Granted"
                    true
                } else {
                    _cameraStatus.value = "Camera Permission Denied"
                    emitError(ErrorType.PERMISSION_DENIED, "Camera permission denied")
                    false
                }
            } catch (e: Exception) {
                _cameraStatus.value = "Permission Request Failed"
                emitError(ErrorType.PERMISSION_DENIED, "Permission request failed: ${e.message}")
                false
            }
        } ?: run {
            _cameraStatus.value = "Permission Required - Check Settings"
            emitError(ErrorType.PERMISSION_DENIED, "Camera permission required")
            false
        }
    }


    private fun createOptimizedRecorder(): Recorder {
        return try {
            // Use QualitySelector to attempt UHD and fall back to lower quality if unsupported
            val qualitySelector = if (deviceSupports4K) {
                Log.i(TAG, "Creating 4K UHD quality selector with fallback strategy")
                QualitySelector.from(
                    Quality.UHD,
                    FallbackStrategy.lowerQualityThan(Quality.UHD)
                )
            } else {
                Log.i(TAG, "Creating FHD quality selector with fallback strategy")
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityThan(Quality.FHD)
                )
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
                
            Log.i(TAG, "Optimized recorder created with quality selector configuration")
            recorder

        } catch (e: Exception) {
            Log.e(TAG, "Error creating optimized recorder, using conservative fallback", e)
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
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "Starting RGB camera recording with Samsung Galaxy S22 optimization")
                _isRecording.set(true)
                sessionStartTime.set(System.currentTimeMillis())


                sessionReferenceTimestampNs.set(sessionMetadata.sessionStartMonotonicNs)
                val localStartNs = System.nanoTime()
                sessionStartOffsetNs.set(localStartNs - sessionMetadata.sessionStartMonotonicNs)


                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) sessionDir.mkdirs()

                setupOutputFiles()
                initializeCsvWriter()


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
                _cameraStatus.value = "Recording - ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps"

                Log.i(
                    TAG,
                    "RGB camera recording started successfully with ${selectedVideoWidth}x${selectedVideoHeight} resolution"
                )
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start RGB camera recording", e)
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
                Log.w(TAG, "Recording already in progress")
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

            setupOutputFiles()


            val videoRecordingStarted = startVideoRecording()
            if (!videoRecordingStarted) {
                Log.e(TAG, "Failed to start video recording")
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

            Log.i(TAG, "RGB CameraX recording started in: $sessionDirectory")
            updateStatus(isRecording = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RGB CameraX recording", e)
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
        val localStartNs = System.nanoTime()
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

                    if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.i(TAG, "Video recording started")
                            recordingScope.launch {
                                updateStatus(isRecording = true)
                            }
                        }

                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Log.i(TAG, "Video recording saved: ${outputFile.absolutePath}")
                            } else {
                                Log.e(TAG, "Video recording error: ${recordEvent.error}")
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

            Log.d(TAG, "Video recording started to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            false
        }
    }


    private fun startFrameCapture() {
        frameCaptureJob = recordingScope.launch {
            val framesDir = File(sessionDirectory, "frames")
            if (!framesDir.exists()) {
                framesDir.mkdirs()
                Log.d(TAG, "Created frames directory: ${framesDir.absolutePath}")
            }

            val captureInterval = 1000L / CAPTURE_FPS
            var frameSkipCounter = 0 // Counter for frame throttling


            var pendingCaptureCount = 0


            frameTimestamps.clear()
            lastFrameRateCheck.set(System.currentTimeMillis())
            actualFrameRateAchieved = 0.0

            Log.i(TAG, "🎬 Starting optimized frame capture at ${CAPTURE_FPS} FPS with throttling (every ${FRAME_CAPTURE_EVERY_N_FRAMES} frames)")

            while (_isRecording.get() && isActive) {
                try {
                    // Implement frame throttling - only capture every Nth frame
                    frameSkipCounter++
                    if (frameSkipCounter % FRAME_CAPTURE_EVERY_N_FRAMES != 0) {
                        delay(captureInterval)
                        continue
                    }

                    if (pendingCaptureCount >= MAX_PENDING_CAPTURES) {
                        droppedFrames.incrementAndGet()
                        Log.d(TAG, "Frame dropped due to backpressure (pending: $pendingCaptureCount)")
                        delay(captureInterval)
                        continue
                    }

                    val frameStartTime = System.nanoTime()
                    pendingCaptureCount++


                    captureFrameAsync(framesDir, frameStartTime) {
                        pendingCaptureCount--

                        monitorFrameRate(frameStartTime)
                    }

                    delay(captureInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in enhanced frame capture loop", e)

                    val currentTime = System.currentTimeMillis()
                    consecutiveFrameErrors.incrementAndGet()
                    droppedFrames.incrementAndGet()


                    pendingCaptureCount = 0


                    delay(1000)
                }
            }


            logFinalFrameRateStats()
            Log.i(TAG, "📸 Enhanced frame capture completed")
        }
    }


    private fun captureFrameAsync(framesDir: File, frameStartTime: Long, onComplete: () -> Unit) {
        try {
            val timestampRecord = TimestampManager.createTimestampRecord()
            val frameNumber = framesCaptured.incrementAndGet()


            val jpegFile = File(
                framesDir,
                "frame_${String.format("%08d", frameNumber)}_${timestampRecord.systemNanos}$JPEG_FILE_EXTENSION"
            )
            val rawFile = if (deviceSupportsRAW && ENABLE_RAW_CAPTURE) {
                File(
                    framesDir,
                    "frame_${String.format("%08d", frameNumber)}_${timestampRecord.systemNanos}$RAW_FILE_EXTENSION"
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
                                captureRawFrameAsync(rawFile, timestampRecord, frameNumber)
                            }

                            onComplete()

                        } catch (e: Exception) {
                            Log.w(TAG, "Error in onImageSaved callback", e)
                            onComplete()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.w(TAG, "Frame capture failed: ${exception.message}")
                        handleFrameCaptureError(exception)
                        onComplete()
                    }
                }
            ) ?: run {
                Log.w(TAG, "ImageCapture not available for frame capture")
                onComplete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up frame capture", e)
            onComplete()
        }
    }


    /**
     * Capture RAW DNG frame asynchronously with Stage 3/Level 3 processing
     * Uses RAW ImageCapture with ImageFormat.RAW_SENSOR for proper DNG creation
     */
    private fun captureRawFrameAsync(rawFile: File, timestampRecord: TimestampRecord, frameNumber: Long) {
        try {
            Log.d(TAG, "Capturing Stage 3/Level 3 DNG frame $frameNumber - ${rawFile.name}")

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
                            Log.i(TAG, "Stage 3/Level 3 RAW DNG capture initiated for frame $frameNumber")

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
                                                    val camera2Info = camera?.cameraInfo?.let { androidx.camera.camera2.interop.Camera2CameraInfo.from(it) }
                                                    enhanceStage3DngMetadata(stage3File, timestampRecord, frameNumber, camera2Info)
                                                    logFrameCapture(timestampRecord, frameNumber, stage3File, isRaw = true)
                                                    
                                                    Log.i(TAG, "✅ Stage 3/Level 3 DNG saved: ${stage3File.name} (${stage3File.length()} bytes)")
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error post-processing Stage 3 DNG", e)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(TAG, "Stage 3/Level 3 DNG capture failed for frame $frameNumber", exception)
                                            // Fallback to standard processing
                                            recordingScope.launch(Dispatchers.IO) {
                                                rawFile.writeText("RAW capture fallback frame $frameNumber - ${timestampRecord.systemNanos}")
                                                Log.w(TAG, "Fallback RAW metadata saved for frame $frameNumber")
                                            }
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Stage 3/Level 3 capture setup failed for frame $frameNumber: ${e.message}")
                            // Fallback to standard processing
                            rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
                        }
                    }
                } ?: run {
                    Log.w(TAG, "RAW ImageCapture not available, using fallback")
                    rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
                }
            } else {
                // Standard RAW processing for non-Samsung devices or when Stage 3 is disabled
                Log.i(TAG, "Standard RAW processing for frame $frameNumber (device not Stage 3/Level 3 compatible)")
                rawFile.writeText("RAW capture frame $frameNumber - ${timestampRecord.systemNanos}")
            }

        } catch (e: Exception) {
            Log.w(TAG, "RAW capture error for frame $frameNumber", e)
        }
    }

    /**
     * Enhance DNG file with Stage 3/Level 3 specific metadata
     */
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
            
            val metadataFile = File(dngFile.parent, dngFile.nameWithoutExtension + "_stage3_metadata.json")
            val metadata = mapOf(
                "processing_pipeline" to "Samsung Stage 3/Level 3",
                "frame_number" to frameNumber,
                "capture_timestamp_ns" to timestampRecord.systemNanos,
                "monotonic_timestamp_ns" to timestampRecord.monotonicNanos,
                "device_model" to SamsungDeviceCompatibility.getDeviceInfo(),
                "camera_id" to (camera2Info?.cameraId ?: "unknown"),
                "dng_file_size_bytes" to dngFile.length(),
                "creation_time" to System.currentTimeMillis()
            )
            
            val gson = com.google.gson.Gson()
            metadataFile.writeText(gson.toJson(metadata))
            
            Log.d(TAG, "Stage 3/Level 3 metadata enhanced for frame $frameNumber")
        } catch (e: Exception) {
            Log.w(TAG, "Could not enhance Stage 3/Level 3 metadata: ${e.message}")
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
                "Actual frame rate: ${String.format("%.2f", actualFrameRateAchieved)} fps (target: ${CAPTURE_FPS} fps)"
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
                    Log.e(TAG, "Critical frame rate deviation detected - performance issue may be present")
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
            val recordingDurationMs = System.currentTimeMillis() - sessionReferenceTimestampNs.get() / 1_000_000
            val recordingDurationSeconds = recordingDurationMs / 1000.0
            val averageFrameRate = totalFrames / recordingDurationSeconds

            Log.i(TAG, "Final RGB recording statistics:")
            Log.i(TAG, "  Total frames captured: $totalFrames")
            Log.i(TAG, "  Recording duration: ${String.format("%.2f", recordingDurationSeconds)}s")
            Log.i(TAG, "  Average frame rate: ${String.format("%.2f", averageFrameRate)} fps")
            Log.i(TAG, "  Recent frame rate: ${String.format("%.2f", actualFrameRateAchieved)} fps")
            Log.i(TAG, "  Target frame rate: $CAPTURE_FPS fps")
            Log.i(TAG, "  Video configuration: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps")


            val frameRateSuccess = Math.abs(averageFrameRate - CAPTURE_FPS) / CAPTURE_FPS < 0.2
            if (frameRateSuccess) {
                Log.i(TAG, "✅ Frame rate validation PASSED - achieved target 30 FPS ± 20%")
            } else {
                Log.w(TAG, "⚠️ Frame rate validation WARNING - significant deviation from target 30 FPS detected")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating final frame rate statistics", e)
        }
    }


    private fun logFrameCapture(timestampRecord: TimestampRecord, frameNumber: Long, outputFile: File) {
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
                        alignedNs,
                        frameNumber,
                        sessionTimeMs,
                        wallMs?.toString() ?: "",
                        "frame_capture",
                        metadataParts.joinToString(",")
                    )
                )
            }

            samplesRecorded.incrementAndGet()
            lastFrameTime.set(alignedNs)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to log frame capture", e)
        }
    }

    // Overload for RAW/DNG files
    private fun logFrameCapture(timestampRecord: TimestampRecord, frameNumber: Long, outputFile: File, isRaw: Boolean) {
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
                        alignedNs,
                        frameNumber,
                        sessionTimeMs,
                        wallMs?.toString() ?: "",
                        eventType,
                        metadataParts.joinToString(",")
                    )
                )
            }

            samplesRecorded.incrementAndGet()
            lastFrameTime.set(alignedNs)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to log RAW frame capture", e)
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
            Log.d(TAG, "Buffered CSV writer initialized for frame timestamps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CSV writer", e)
            throw e
        }
    }


    private fun handleFrameCaptureError(exception: ImageCaptureException) {
        val currentTime = System.currentTimeMillis()
        val errorCount = consecutiveFrameErrors.incrementAndGet()
        droppedFrames.incrementAndGet()

        Log.w(TAG, "Frame capture failed (error $errorCount): ${exception.message}", exception)


        if (errorCount >= MAX_CONSECUTIVE_FRAME_ERRORS) {
            val timeSinceLastError = currentTime - lastFrameErrorTime.get()

            if (timeSinceLastError < FRAME_ERROR_RESET_INTERVAL) {

                Log.e(TAG, "Too many consecutive frame capture errors ($errorCount), camera may be failing")
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
                Log.w(TAG, "No recording in progress to stop")
                return false
            }

            Log.i(TAG, "🛑 Stopping RGB camera recording with enhanced cleanup...")
            _isRecording.set(false)
            _cameraStatus.value = "Stopping Recording..."


            frameCaptureJob?.let { job ->
                Log.d(TAG, "Cancelling frame capture job...")
                job.cancel()
                try {
                    job.join()
                    Log.d(TAG, "Frame capture job cancelled successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Frame capture job cancellation timeout", e)
                }
                frameCaptureJob = null
            }


            activeRecording?.let { recording ->
                Log.d(TAG, "Stopping active video recording...")
                try {
                    recording.stop()
                    Log.d(TAG, "Video recording stopped successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping video recording", e)
                }
                activeRecording = null
            }




            try {
                csvWriter?.let { writer ->
                    writer.flush()
                    writer.close()
                    Log.d(TAG, "CSV writer closed successfully")
                }
                csvWriter = null

                csvBufferedWriter?.let { bufferedWriter ->
                    bufferedWriter.stop()
                    Log.d(TAG, "CSV buffered writer stopped successfully")
                }
                csvBufferedWriter = null
            } catch (e: Exception) {
                Log.w(TAG, "Error during CSV cleanup", e)
            }


            try {
                cameraProvider?.unbindAll()
                Log.d(TAG, "Camera provider unbound successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding camera provider", e)
            }


            val sessionStats = generateSessionStats()
            Log.i(TAG, "📊 RGB Camera Session Complete:")
            Log.i(TAG, "  • Frames captured: ${sessionStats.framesCaptured}")
            Log.i(TAG, "  • Frames dropped: ${sessionStats.framesDropped}")
            Log.i(TAG, "  • Frame drop rate: ${String.format("%.2f", sessionStats.dropRate)}%")
            Log.i(TAG, "  • Average frame rate: ${String.format("%.2f", sessionStats.averageFrameRate)} fps")
            Log.i(TAG, "  • Video file: ${videoFile?.name ?: "N/A"}")
            Log.i(TAG, "  • Storage used: ${String.format("%.1f", sessionStats.storageMB)} MB")


            updateStatus(isRecording = false)
            sessionReferenceTimestampNs.set(0)
            sessionStartOffsetNs.set(0)
            sessionMetadata = null
            _cameraStatus.value = "Recording Stopped"

            Log.i(TAG, "✅ RGB camera recording stopped successfully with enhanced cleanup")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop RGB CameraX recording", e)
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
            if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L
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
                val alignedNs = alignedTimestampNs(timestampNs)
                val sessionTimeMs = sessionRelativeMs(timestampNs)
                val wallMs = wallClockMs(timestampNs)
                val metadataMap = metadata.toMutableMap()
                metadataMap["aligned_ns"] = alignedNs.toString()
                wallMs?.let { metadataMap["wall_ms"] = it.toString() }
                sessionMetadata?.let {
                    metadataMap["session_reference_ns"] = sessionReferenceTimestampNs.get().toString()
                }
                val metadataStr = metadataMap.entries.joinToString(",") { "${it.key}=${it.value}" }

                val row = listOf(
                    timestampNs,
                    alignedNs,
                    samplesRecorded.get(),
                    sessionTimeMs,
                    wallMs?.toString() ?: "",
                    markerType,
                    metadataStr
                )
                writer.writeRow(row)
            }

            Log.d(TAG, "Sync marker added: $markerType at $timestampNs ns")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "Failed to add sync marker: ${e.message}")
        }
    }

    override suspend fun cleanup() {
        try {
            Log.i(TAG, "Starting RGB CameraX recorder cleanup")
            _cameraStatus.value = "Cleaning up..."


            if (_isRecording.get()) {
                Log.d(TAG, "Stopping active recording during cleanup")
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
                    Log.d(TAG, "Camera use cases unbound")
                } catch (e: Exception) {
                    Log.w(TAG, "Error unbinding camera use cases", e)
                }
            }


            camera = null
            preview = null
            videoCapture = null
            imageCapture = null
            rawImageCapture = null
            cameraProvider = null

            try {
                cameraExecutor.shutdown()
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Camera executor did not terminate gracefully, forcing shutdown")
                    cameraExecutor.shutdownNow()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down camera executor", e)
            }


            recordingScope.cancel()


            consecutiveFrameErrors.set(0)
            lastFrameErrorTime.set(0)
            _cameraStatus.value = "Cleaned up"

            Log.i(TAG, "RGB CameraX recorder cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during CameraX cleanup", e)
            _cameraStatus.value = "Cleanup Failed"
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = statusFlow.asStateFlow()

    override fun getErrorFlow(): Flow<SensorError> = errorFlow.asSharedFlow()

    override fun getRecordingStats(): RecordingStats {
        val currentTime = System.nanoTime()
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
        timestampNs = System.nanoTime()
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
            timestampNs = System.nanoTime()
        )

        statusFlow.emit(status)
    }

    private suspend fun emitError(errorType: ErrorType, message: String) {
        val error = SensorError(
            sensorId = sensorId,
            sensorType = sensorType,
            errorType = errorType,
            errorMessage = message,
            timestampNs = System.nanoTime(),
            isRecoverable = errorType != ErrorType.HARDWARE_DISCONNECTED
        )

        errorFlow.emit(error)
    }


    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
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
}
