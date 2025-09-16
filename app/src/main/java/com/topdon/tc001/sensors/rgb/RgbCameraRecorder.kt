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
import com.topdon.tc001.sensors.*
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.util.CSVBufferedWriter
import com.topdon.tc001.util.SessionDirectoryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RgbCameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null
) : SensorRecorder {

    companion object {
        private const val TAG = "RgbCameraRecorder"
        
        // 4K60fps recording constants as per requirements
        private const val VIDEO_WIDTH = 3840  // 4K UHD width
        private const val VIDEO_HEIGHT = 2160 // 4K UHD height  
        private const val VIDEO_FPS = 60      // Enhanced to 60fps capability
        private const val VIDEO_BITRATE = 50_000_000  // 50 Mbps for 4K60 quality
        private const val AUDIO_BITRATE = 256_000     // 256 kbps for high-quality audio
        
        // Image capture constants
        private const val JPEG_QUALITY = 100   // Maximum quality for analysis frames
        private const val CAPTURE_FPS = 30     // ~30 FPS still frame capture
    }

    override val sensorId: String = "rgb_camera_${System.currentTimeMillis()}"
    override val sensorType: String = "RGB_Camera_CameraX"
    override val samplingRate: Double = VIDEO_FPS.toDouble()

    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    // CameraX components
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var activeRecording: Recording? = null
    
    // Executor for camera operations
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val statusFlow = MutableStateFlow(createInitialStatus())
    private val errorFlow = MutableSharedFlow<SensorError>()

    private var sessionDirectory: String = ""
    private var csvWriter: CSVWriter? = null
    private var videoFile: File? = null
    private var csvBufferedWriter: CSVBufferedWriter? = null
    private var csvFile: File? = null

    private val samplesRecorded = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private var droppedFrames = AtomicLong(0)
    private val syncMarkersRecorded = AtomicLong(0)
    private val framesCaptured = AtomicLong(0)

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Frame capture job for continuous JPEG frames
    private var frameCaptureJob: Job? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Initializing CameraX for RGB recording")

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            // Check if we have a back camera
            if (!cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                emitError(ErrorType.INITIALIZATION_FAILED, "Back camera not available")
                return@withContext false
            }

            Log.i(TAG, "CameraX provider initialized successfully")
            updateStatus(isInitialized = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CameraX", e)
            emitError(ErrorType.INITIALIZATION_FAILED, "CameraX initialization failed: ${e.message}")
            false
        }
    }
    
    /**
     * Initialize CameraX with preview, video and image capture use cases
     */
    private suspend fun initializeCameraX(): Boolean = withContext(Dispatchers.Main) {
        try {
            cameraProvider?.unbindAll()

            // Preview use case - bind to PreviewView if available
            preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080)) // 1080p preview for performance
                .build()

            // Video capture use case - 4K recording with high quality
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.UHD, // 4K quality
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD) // Fallback to 1080p
                    )
                )
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Image capture use case - high resolution JPEG frames
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(VIDEO_WIDTH, VIDEO_HEIGHT)) // Match video resolution
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
                CameraSelector.DEFAULT_BACK_CAMERA,
                *useCases.toTypedArray()
            )

            Log.i(TAG, "CameraX use cases bound successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CameraX use cases", e)
            emitError(ErrorType.INITIALIZATION_FAILED, "Failed to bind camera use cases: ${e.message}")
            false
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

            // Initialize CameraX use cases
            if (!initializeCameraX()) {
                Log.e(TAG, "Failed to initialize CameraX")
                return false
            }

            setupOutputFiles()
            
            // Start video recording
            val videoRecordingStarted = startVideoRecording()
            if (!videoRecordingStarted) {
                Log.e(TAG, "Failed to start video recording")
                return false
            }

            // Initialize CSV writer asynchronously
            recordingScope.launch {
                initializeCsvWriter()
            }

            // Start continuous frame capture
            startFrameCapture()

            _isRecording.set(true)
            sessionStartTime.set(System.nanoTime())
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
        // Use standard directory structure
        val rgbDir = File(sessionDirectory, "RGB")
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
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
                                    emitError(ErrorType.RECORDING_FAILED, "Video recording failed: ${recordEvent.error}")
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
     * Start continuous frame capture at ~30 FPS
     */
    private fun startFrameCapture() {
        frameCaptureJob = recordingScope.launch {
            val framesDir = File(sessionDirectory, "RGB/frames")
            val captureInterval = 1000L / CAPTURE_FPS // ~33ms for 30fps
            
            Log.i(TAG, "Starting continuous frame capture at ${CAPTURE_FPS} FPS")
            
            while (_isRecording.get()) {
                try {
                    val timestamp = System.nanoTime()
                    val frameNumber = framesCaptured.incrementAndGet()
                    val outputFile = File(framesDir, "frame_${timestamp}.jpg")
                    
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    
                    imageCapture?.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                recordingScope.launch {
                                    logFrameCapture(timestamp, frameNumber, outputFile)
                                }
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                Log.w(TAG, "Frame capture failed: ${exception.message}")
                                droppedFrames.incrementAndGet()
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
     * Log frame capture event to CSV
     */
    private fun logFrameCapture(timestampNs: Long, frameNumber: Long, outputFile: File) {
        try {
            csvBufferedWriter?.let { writer ->
                val sessionTimeMs = (timestampNs - sessionStartTime.get()) / 1_000_000
                
                writer.writeNext(
                    arrayOf(
                        timestampNs.toString(),
                        frameNumber.toString(),
                        sessionTimeMs.toString(),
                        "frame_capture",
                        "filename=${outputFile.name},size=${outputFile.length()}"
                    )
                )
            }
            
            samplesRecorded.incrementAndGet()
            lastFrameTime.set(timestampNs)
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
                            "sample_number", 
                            "session_time_ms",
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
            
            // Close CSV writer
            csvWriter?.close()
            csvWriter = null
            
            // Stop buffered writer properly
            csvBufferedWriter?.stop()
            csvBufferedWriter = null

            Log.i(TAG, "RGB CameraX recording stopped successfully")
            Log.i(TAG, "Session stats - Frames captured: ${framesCaptured.get()}, Dropped frames: ${droppedFrames.get()}")
            
            updateStatus(isRecording = false)
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
                val sessionTimeMs = (timestampNs - sessionStartTime.get()) / 1_000_000
                val metadataStr = metadata.entries.joinToString(",") { "${it.key}=${it.value}" }

                val row = listOf(
                    timestampNs,
                    samplesRecorded.get(),
                    sessionTimeMs,
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
            // Stop recording if in progress
            if (_isRecording.get()) {
                stopRecording()
            }

            // Unbind all use cases
            cameraProvider?.unbindAll()
            cameraProvider = null
            
            // Shutdown camera executor
            cameraExecutor.shutdown()
            
            // Cancel coroutine scope
            recordingScope.cancel()

            Log.i(TAG, "RGB CameraX recorder cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during CameraX cleanup", e)
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
        return when {
            !hasCameraPermission() -> "Camera Permission Required"
            cameraProvider == null -> "Camera Not Initialized"
            _isRecording.get() -> "Recording (${framesCaptured.get()} frames)"
            else -> "Ready"
        }
    }
}
