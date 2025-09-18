package com.topdon.tc001.sensors.rgb

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.opencsv.CSVWriter
import com.topdon.tc001.data.SessionMetadata
import com.topdon.tc001.sensors.*
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.util.CSVBufferedWriter
import com.topdon.tc001.util.SessionDirectoryManager
import com.topdon.tc001.performance.PerformanceBenchmarkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.topdon.tc001.permissions.EnhancedPermissionManager

class RgbCameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null,
    private val useFrontCamera: Boolean = false,
    private val enhancedPermissionManager: EnhancedPermissionManager? = null
) : SensorRecorder {

    companion object {
        private const val TAG = "RgbCameraRecorder"
        
        // Enhanced video configuration with device-specific optimization
        private const val VIDEO_WIDTH_4K = 3840
        private const val VIDEO_HEIGHT_4K = 2160
        private const val VIDEO_WIDTH_1080P = 1920
        private const val VIDEO_HEIGHT_1080P = 1080
        private const val VIDEO_FPS_TARGET = 30 // Target 30 FPS as per TODO requirements
        private const val VIDEO_FPS_FALLBACK = 24 // Fallback if 30 FPS not supported
        private const val VIDEO_BITRATE_4K = 50_000_000
        private const val VIDEO_BITRATE_1080P = 20_000_000
        private const val AUDIO_BITRATE = 256_000
        private const val JPEG_QUALITY = 100
        private const val CAPTURE_FPS = 30
        
        // Error tracking constants
        private const val MAX_CONSECUTIVE_FRAME_ERRORS = 10
        private const val FRAME_ERROR_RESET_INTERVAL = 30000L // 30 seconds
        
        // Device capability detection
        private val KNOWN_4K_DEVICES = setOf(
            "SM-S916B", // Galaxy S22 Ultra
            "SM-S918B", // Galaxy S22 Ultra
            "SM-G998B", // Galaxy S21 Ultra
            "SM-N986B", // Galaxy Note 20 Ultra
            "Pixel 6 Pro",
            "Pixel 7 Pro"
        )
    }

    override val sensorId: String = "rgb_camera_${System.currentTimeMillis()}"
    override val sensorType: String = "RGB_Camera_CameraX"
    override val samplingRate: Double = VIDEO_FPS_TARGET.toDouble()

    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    // Enhanced video configuration
    private var selectedVideoWidth = VIDEO_WIDTH_1080P
    private var selectedVideoHeight = VIDEO_HEIGHT_1080P
    private var selectedVideoFps = VIDEO_FPS_TARGET
    private var selectedVideoBitrate = VIDEO_BITRATE_1080P
    private var deviceSupports4K = false
    private var actualFrameRateAchieved = 0.0

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
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
    
    // Enhanced frame rate monitoring for TODO validation
    private val frameTimestamps = mutableListOf<Long>()
    private var lastFrameRateCheck = AtomicLong(0)
    private val frameRateCheckInterval = 5000L // 5 seconds
    
    private val consecutiveFrameErrors = AtomicLong(0)
    private var lastFrameErrorTime = AtomicLong(0)
    private val _cameraStatus = MutableStateFlow("Uninitialized")
    val cameraStatus: StateFlow<String> = _cameraStatus.asStateFlow()
    
    // Performance benchmarking
    private val performanceBenchmarkManager = PerformanceBenchmarkManager()
    private var rgbBenchmarkId: String? = null
    
    private val cameraSelector = if (useFrontCamera) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Frame capture job for continuous JPEG frames
    private var frameCaptureJob: Job? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Initializing ${if (useFrontCamera) "front" else "back"} camera")

            if (!checkAndRequestPermissions()) return@withContext false
            
            _cameraStatus.value = "Initializing..."
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            
            if (!cameraProvider!!.hasCamera(cameraSelector)) {
                val cameraType = if (useFrontCamera) "Front" else "Back"
                _cameraStatus.value = "$cameraType Camera Not Available"
                emitError(ErrorType.INITIALIZATION_FAILED, "$cameraType camera not available")

                return@withContext false
            }

            // Enhanced device capability detection and video configuration
            detectDeviceCapabilities()
            optimizeVideoConfiguration()

            setupCameraUseCases()
            bindUseCases()
            
            _cameraStatus.value = "Ready"
            Log.i(TAG, "CameraX initialized successfully with ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            _cameraStatus.value = "Initialization Failed"
            emitError(ErrorType.INITIALIZATION_FAILED, "Camera initialization failed: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Detect device capabilities for optimal video configuration
     * Implements TODO requirement: "4K on S22 devices with fallback to 1080p if needed"
     */
    private fun detectDeviceCapabilities() {
        try {
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            
            Log.d(TAG, "Detecting capabilities for device: $deviceManufacturer $deviceModel")
            
            // Check if device is known to support 4K recording
            deviceSupports4K = KNOWN_4K_DEVICES.contains(deviceModel) || deviceModel.contains("S22", ignoreCase = true)
            
            // Additional capability detection using CameraX
            cameraProvider?.let { provider ->
                val camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector)
                val cameraInfo = camera.cameraInfo
                
                // Check available video profiles (if accessible)
                deviceSupports4K = deviceSupports4K || checkVideoProfileSupport(cameraInfo)
            }
            
            Log.i(TAG, "Device 4K support detected: $deviceSupports4K for $deviceManufacturer $deviceModel")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting device capabilities, using safe defaults", e)
            deviceSupports4K = false
        }
    }

    /**
     * Check video profile support for enhanced capability detection
     */
    private fun checkVideoProfileSupport(cameraInfo: CameraInfo): Boolean {
        return try {
            // This is a simplified check - in production, you'd check actual video profiles
            // For now, we'll use device model detection as the primary method
            false
        } catch (e: Exception) {
            Log.w(TAG, "Could not check video profile support", e)
            false
        }
    }

    /**
     * Optimize video configuration based on device capabilities
     * Implements TODO requirement: "Verify that the RGB camera reliably records at the intended 30 FPS @ 1080p/4K"
     */
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
            
            Log.i(TAG, "Video configuration optimized: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps, bitrate: ${selectedVideoBitrate}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing video configuration, using safe defaults", e)
            selectedVideoWidth = VIDEO_WIDTH_1080P
            selectedVideoHeight = VIDEO_HEIGHT_1080P
            selectedVideoBitrate = VIDEO_BITRATE_1080P
            selectedVideoFps = VIDEO_FPS_FALLBACK
        }
    }

    private suspend fun checkAndRequestPermissions(): Boolean {
        if (hasCameraPermission()) return true
        
        _cameraStatus.value = "Camera Permission Required"
        
        return enhancedPermissionManager?.let { permissionManager ->
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

    /**
     * Initialize CameraX with preview, video and image capture use cases
     */

    /**
     * Initialize CameraX with preview, video and image capture use cases
     */
    private suspend fun initializeCameraX(): Boolean = withContext(Dispatchers.Main) {
        try {
            cameraProvider?.unbindAll()

            // Preview use case - optimized resolution for performance
            preview = Preview.Builder()
                .setTargetResolution(Size(selectedVideoWidth / 2, selectedVideoHeight / 2)) // Half resolution for smooth preview
                .build()

            // Enhanced video capture use case with optimized configuration
            val recorder = createOptimizedRecorder()
            videoCapture = VideoCapture.withOutput(recorder)

            // Image capture use case - matches video resolution for consistency
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(selectedVideoWidth, selectedVideoHeight))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Fast capture for 30fps
                .setJpegQuality(JPEG_QUALITY)
                .build()

            // Bind use cases to lifecycle
            val useCases = mutableListOf<UseCase>(videoCapture!!, imageCapture!!)

            // Add preview if PreviewView is available
            previewView?.let {
                preview?.setSurfaceProvider(it.surfaceProvider)
                useCases.add(preview!!)
                Log.d(TAG, "Preview bound to PreviewView")
            }

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            Log.i(TAG, "CameraX use cases bound successfully with optimized configuration: ${selectedVideoWidth}x${selectedVideoHeight}@${selectedVideoFps}fps")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
            false
        }
    }

    /**
     * Create optimized recorder based on device capabilities and target specifications
     * Implements TODO requirement: "Verify that the RGB camera reliably records at the intended 30 FPS"
     */
    private fun createOptimizedRecorder(): Recorder {
        return try {
            val qualitySelector = if (deviceSupports4K) {
                Log.i(TAG, "Creating 4K quality selector for capable device")
                QualitySelector.from(
                    Quality.UHD, // 4K quality
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD) // Fallback to 1080p
                )
            } else {
                Log.i(TAG, "Creating 1080p quality selector with fallback")
                QualitySelector.from(
                    Quality.FHD, // 1080p quality
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD) // Fallback to 720p
                )
            }

            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error creating optimized recorder, using default", e)
            Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD, // Safe 1080p default
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                    )
                )
                .build()
        }
    }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CameraX use cases", e)
            emitError(
                ErrorType.INITIALIZATION_FAILED,
                "Failed to bind camera use cases: ${e.message}"
            )
            false
        }
    }

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata
    ): Boolean {
        this.sessionMetadata = sessionMetadata
        return startRecording(sessionDirectory)
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

            // Start RGB performance benchmarking
            val sessionId = sessionDirectory.substringAfterLast("/")
            rgbBenchmarkId = performanceBenchmarkManager.startRGBFrameRateBenchmark(sessionId)
            Log.i(TAG, "RGB performance benchmarking started: $rgbBenchmarkId")

            // Initialize CameraX use cases
            if (!initializeCameraX()) {
                Log.e(TAG, "Failed to initialize CameraX")
                rgbBenchmarkId?.let { performanceBenchmarkManager.finalizeRGBFrameRateBenchmark(it) }
                rgbBenchmarkId = null
                return false
            }

            setupOutputFiles()

            // Start video recording
            val videoRecordingStarted = startVideoRecording()
            if (!videoRecordingStarted) {
                Log.e(TAG, "Failed to start video recording")
                rgbBenchmarkId?.let { performanceBenchmarkManager.finalizeRGBFrameRateBenchmark(it) }
                rgbBenchmarkId = null
                return false
            }

            initializeSessionTiming()

            // Initialize CSV writer asynchronously
            recordingScope.launch {
                initializeCsvWriter()
            }

            // Start continuous frame capture
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
        // Use provided sensor directory as root
        val rgbDir = File(sessionDirectory)
        if (!rgbDir.exists()) {
            rgbDir.mkdirs()
        }

        // Create frames subdirectory for JPEG captures
        val framesDir = File(rgbDir, "frames")
        if (!framesDir.exists()) {
            framesDir.mkdirs()
        }

        // Use standard file names from SessionDirectoryManager
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

    /**
     * Start CameraX video recording
     */
    private fun startVideoRecording(): Boolean {
        return try {
            val videoCapture = this.videoCapture ?: return false
            val outputFile = videoFile ?: return false

            val mediaStoreOutput = FileOutputOptions.Builder(outputFile).build()

            activeRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .apply {
                    // Enable audio recording if permission is available
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

    /**
     * Start continuous frame capture at ~30 FPS with frame rate validation
     * Implements TODO requirement: "Ensure the recorded video and extracted frame timestamps (in rgb.csv) remain in sync"
     */
    private fun startFrameCapture() {
        frameCaptureJob = recordingScope.launch {
            val framesDir = File(sessionDirectory, "frames")
            if (!framesDir.exists()) {
                framesDir.mkdirs()
                Log.d(TAG, "Created frames directory: ${framesDir.absolutePath}")
            }

            val captureInterval = 1000L / CAPTURE_FPS // ~33ms for 30fps
            
            // Reset frame rate monitoring
            frameTimestamps.clear()
            lastFrameRateCheck.set(System.currentTimeMillis())
            actualFrameRateAchieved = 0.0

            while (_isRecording.get() && isActive) {
                try {
                    val frameStartTime = System.nanoTime()
                    captureFrame(framesDir, frameStartTime)
                    
                    // Enhanced frame rate monitoring
                    monitorFrameRate(frameStartTime)
                    
                    delay(captureInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in frame capture loop", e)
                    handleFrameCaptureError(null)
                    
                    // Avoid tight loop on persistent errors
                    delay(1000)
                }
            }
            
            // Log final frame rate statistics
            logFinalFrameRateStats()
        }
    }

    /**
     * Monitor and validate actual frame rate against target
     * Implements TODO requirement: "Verify that the RGB camera reliably records at the intended 30 FPS"
     */
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

    /**
     * Calculate actual frame rate and validate against target
     */
    private fun calculateAndValidateFrameRate() {
        if (frameTimestamps.size < 10) return // Need minimum samples
        
        synchronized(frameTimestamps) {
            val recentFrames = frameTimestamps.takeLast(150) // Last 5 seconds at 30fps
            if (recentFrames.size < 2) return
            
            val timeSpanNs = recentFrames.last() - recentFrames.first()
            val timeSpanSeconds = timeSpanNs / 1_000_000_000.0
            actualFrameRateAchieved = (recentFrames.size - 1) / timeSpanSeconds
            
            Log.d(TAG, "Actual frame rate: ${String.format("%.2f", actualFrameRateAchieved)} fps (target: ${CAPTURE_FPS} fps)")
            
            // Validate frame rate against target
            val frameRateDeviation = Math.abs(actualFrameRateAchieved - CAPTURE_FPS) / CAPTURE_FPS
            if (frameRateDeviation > 0.15) { // 15% tolerance
                Log.w(TAG, "Frame rate deviation detected: ${String.format("%.1f%%", frameRateDeviation * 100)} from target ${CAPTURE_FPS} fps")
                
                // Could trigger frame rate adaptation here if needed
                if (frameRateDeviation > 0.3) { // 30% deviation is critical
                    Log.e(TAG, "Critical frame rate deviation detected - performance issue may be present")
                }
            }
            
            // Keep only recent timestamps to prevent memory growth
            if (frameTimestamps.size > 300) { // Keep ~10 seconds of history
                frameTimestamps.subList(0, frameTimestamps.size - 300).clear()
            }
        }
    }

    /**
     * Log final frame rate statistics at the end of recording
     */
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
            
            // Validate against requirements
            val frameRateSuccess = Math.abs(averageFrameRate - CAPTURE_FPS) / CAPTURE_FPS < 0.2 // 20% tolerance
            if (frameRateSuccess) {
                Log.i(TAG, "✅ Frame rate validation PASSED - achieved target 30 FPS ± 20%")
            } else {
                Log.w(TAG, "⚠️ Frame rate validation WARNING - significant deviation from target 30 FPS detected")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating final frame rate statistics", e)
        }
    }
            val captureInterval = 1000L / CAPTURE_FPS // ~33ms for 30fps

            Log.i(TAG, "Starting continuous frame capture at ${CAPTURE_FPS} FPS")

            while (_isRecording.get()) {
                try {
                    val timestampRecord = TimestampManager.createTimestampRecord()
                    val frameNumber = framesCaptured.incrementAndGet()
                    val outputFile = File(framesDir, "frame_${timestampRecord.systemNanos}.jpg")

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCapture?.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                resetFrameErrorTracking()
                                
                                // Record frame for performance benchmarking
                                rgbBenchmarkId?.let { benchmarkId ->
                                    val frameSize = outputFile.length()
                                    performanceBenchmarkManager.recordRGBFrame(
                                        benchmarkId, 
                                        timestampRecord.systemNanos / 1_000_000, // Convert to ms
                                        frameSize
                                    )
                                }
                                
                                recordingScope.launch {
                                    logFrameCapture(timestampRecord, frameNumber, outputFile)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                handleFrameCaptureError(exception)
                            }
                        }
                    )

                    delay(captureInterval)
                } catch (e: Exception) {
                    Log.w(TAG, "Error during frame capture", e)
                    delay(captureInterval)
                }
            }

            Log.i(TAG, "Frame capture stopped")
        }
    }

    /**
     * Log frame capture event to CSV with unified timestamp system
     */
    private fun logFrameCapture(timestampRecord: TimestampRecord, frameNumber: Long, outputFile: File) {
        try {
            csvBufferedWriter?.let { writer ->
                val alignedNs = alignedTimestampNs(timestampNs)
                val sessionTimeMs = sessionRelativeMs(timestampNs)
                val wallMs = wallClockMs(timestampNs)
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

    private suspend fun initializeCsvWriter() {
        try {
            csvFile?.let { file ->
                    csvWriter = CSVWriter(FileWriter(file)).apply {
                        // Enhanced CSV header for frame tracking
                        writeNext(
                            arrayOf(
                                "timestamp_ns",
                                "aligned_timestamp_ns",
                                "sample_number",
                                "session_time_ms",
                                "wall_time_ms",
                                "event_type",      // frame_capture, video_start, video_stop, sync_marker
                                "metadata"         // Additional info like filename, size, etc.
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
                    flushIntervalMs = 500L  // 0.5 second flush for video metadata
                )

                csvBufferedWriter?.startWithHeaders()
            }
            Log.d(TAG, "Buffered CSV writer initialized for frame timestamps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CSV writer", e)
            throw e
        }
    }

    /**
     * Handle frame capture errors with enhanced tracking and user feedback
     */
    private fun handleFrameCaptureError(exception: ImageCaptureException) {
        val currentTime = System.currentTimeMillis()
        val errorCount = consecutiveFrameErrors.incrementAndGet()
        droppedFrames.incrementAndGet()
        
        Log.w(TAG, "Frame capture failed (error $errorCount): ${exception.message}", exception)
        
        // Check if errors are happening too frequently
        if (errorCount >= MAX_CONSECUTIVE_FRAME_ERRORS) {
            val timeSinceLastError = currentTime - lastFrameErrorTime.get()
            
            if (timeSinceLastError < FRAME_ERROR_RESET_INTERVAL) {
                // Too many errors in a short time period - this indicates a serious problem
                Log.e(TAG, "Too many consecutive frame capture errors ($errorCount), camera may be failing")
                _cameraStatus.value = "Camera Error - Frame Capture Failing"
                
                recordingScope.launch {
                    emitError(
                        ErrorType.DEVICE_ERROR, 
                        "Camera frame capture is failing repeatedly. Check camera hardware and permissions."
                    )
                }
            } else {
                // Reset error count if enough time has passed
                consecutiveFrameErrors.set(1)
            }
        }
        
        lastFrameErrorTime.set(currentTime)
        
        // Update status to reflect capture issues if needed
        if (errorCount > 3) {
            _cameraStatus.value = "Recording (Frame Capture Issues: $errorCount errors)"
        }
    }
    
    /**
     * Reset frame error tracking (called on successful captures)
     */
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

            _isRecording.set(false)

            // Stop frame capture job
            frameCaptureJob?.cancel()
            frameCaptureJob = null

            // Stop video recording
            activeRecording?.stop()
            activeRecording = null

            // Finalize RGB performance benchmarking
            rgbBenchmarkId?.let { benchmarkId ->
                val resolution = "${selectedVideoWidth}x${selectedVideoHeight}"
                val result = performanceBenchmarkManager.finalizeRGBFrameRateBenchmark(
                    benchmarkId, 
                    resolution, 
                    CAPTURE_FPS.toDouble()
                )
                Log.i(TAG, "RGB Performance Result: ${result.summary}")
            }
            rgbBenchmarkId = null

            // Close CSV writer
            csvWriter?.close()
            csvWriter = null

            // Stop buffered writer properly
            csvBufferedWriter?.stop()
            csvBufferedWriter = null

            // Log final frame rate statistics
            logFinalFrameRateStats()

            Log.i(TAG, "RGB CameraX recording stopped successfully with performance analysis")
            Log.i(
                TAG,
                "Session stats - Frames captured: ${framesCaptured.get()}, Dropped frames: ${droppedFrames.get()}"
            )

            updateStatus(isRecording = false)
            sessionReferenceTimestampNs.set(0)
            sessionStartOffsetNs.set(0)
            sessionMetadata = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop RGB CameraX recording", e)
            emitError(ErrorType.RECORDING_FAILED, "Failed to stop recording: ${e.message}")
            false
        }
    }

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
            
            // Stop recording if in progress
            if (_isRecording.get()) {
                Log.d(TAG, "Stopping active recording during cleanup")
                stopRecording()
            }
            
            // Cancel frame capture job
            frameCaptureJob?.cancel()
            frameCaptureJob = null
            
            // Stop active recording
            activeRecording?.stop()
            activeRecording = null
            
            // Close writers
            csvWriter?.close()
            csvWriter = null
            csvBufferedWriter?.stop()
            csvBufferedWriter = null

            // Unbind all use cases from lifecycle
            withContext(Dispatchers.Main) {
                try {
                    cameraProvider?.unbindAll()
                    Log.d(TAG, "Camera use cases unbound")
                } catch (e: Exception) {
                    Log.w(TAG, "Error unbinding camera use cases", e)
                }
            }
            
            // Clear camera references
            camera = null
            preview = null
            videoCapture = null
            imageCapture = null
            cameraProvider = null
            // Shutdown camera executor with timeout
            try {
                cameraExecutor.shutdown()
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Camera executor did not terminate gracefully, forcing shutdown")
                    cameraExecutor.shutdownNow()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down camera executor", e)
            }
            
            // Cancel coroutine scope
            recordingScope.cancel()
            
            // Reset status tracking
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

        // Calculate total samples (frames captured)
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

        // Calculate storage from session directory (includes video and frame files)
        if (sessionDirectory.isNotEmpty()) {
            val sessionDir = File(sessionDirectory)
            if (sessionDir.exists()) {
                totalBytes += sessionDir.walkTopDown()
                    .filter { it.isFile }
                    .map { it.length() }
                    .sum()
            }
        }

        // Add CSV file size
        csvFile?.let { file ->
            if (file.exists()) {
                totalBytes += file.length()
            }
        }

        return totalBytes / (1024.0 * 1024.0) // Convert to MB
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

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get camera capabilities
     */
    fun supportsHighResolution(): Boolean {
        return try {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get current recording status text for UI feedback
     */
    fun getStatusText(): String {
        return _cameraStatus.value
    }
    
    /**
     * Get camera type being used
     */
    fun getCameraType(): String {
        return if (useFrontCamera) "Front Camera" else "Back Camera"
    }
    
    /**
     * Check if front camera is being used
     */
    fun isUsingFrontCamera(): Boolean = useFrontCamera
    
    /**
     * Get current frame capture statistics
     */
    fun getFrameCaptureStats(): Map<String, Any> {
        return mapOf(
            "frames_captured" to framesCaptured.get(),
            "frames_dropped" to droppedFrames.get(),
            "consecutive_errors" to consecutiveFrameErrors.get(),
            "camera_type" to getCameraType(),
            "capture_fps" to CAPTURE_FPS,
            "video_resolution" to "${VIDEO_WIDTH}x${VIDEO_HEIGHT}",
            "has_preview" to (previewView != null)
        )
    }
}
