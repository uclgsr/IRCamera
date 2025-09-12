package com.topdon.tc001.sensors.rgb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.topdon.tc001.sensors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Legacy RGB Camera recorder using CameraX API - DEPRECATED
 * 
 * This implementation has been replaced by RGBCameraRecorder which uses Camera2 API
 * for dual RAW (50MP) and 4K video modes with fast session switching.
 * 
 * Kept for backward compatibility only.
 * 
 * @deprecated Use RGBCameraRecorder instead for dual-mode camera functionality
 */
@Deprecated("Use RGBCameraRecorder for dual-mode functionality", replaceWith = ReplaceWith("RGBCameraRecorder"))
class RgbCameraRecorderLegacy(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    override val sensorId: String = "rgb_camera_1",
    private val targetVideoResolution: Size = Size(1920, 1080),
    private val targetImageResolution: Size = Size(4032, 3024), // Max resolution
    private val videoFrameRate: Int = 30
) : SensorRecorder {

    companion object {
        private const val TAG = "RgbCameraRecorder"
        private const val VIDEO_FILENAME = "rgb_video.mp4"
        private const val IMAGES_SUBDIRECTORY = "rgb_images"
        private const val IMAGE_CAPTURE_INTERVAL_MS = 100L // 10fps for analysis frames
    }

    override val sensorType: String = "RGB Camera"
    override val samplingRate: Double = videoFrameRate.toDouble()
    
    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    // CameraX components
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null
    
    // Threading
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Data flows
    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()
    
    // Recording state
    private var sessionDirectory: String = ""
    private var videoFile: File? = null
    private var imagesDirectory: File? = null
    private var frameCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var imageCapturJob: Job? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.i(TAG, "Initializing RGB camera for sensor $sensorId")
            
            // Check camera permission with detailed error reporting
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                emitError(ErrorType.PERMISSION_DENIED, "Camera permission not granted. Please grant camera permission in app settings.")
                return@withContext false
            }
            
            // Initialize CameraX with Samsung device considerations
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            // Check camera availability before configuration
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e(TAG, "No cameras available on device")
                emitError(ErrorType.INITIALIZATION_FAILED, "No cameras found on device")
                return@withContext false
            }
            
            // Configure video capture with Samsung-compatible settings
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    // Use simple quality selector that works on Samsung devices
                    QualitySelector.from(Quality.FHD)
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Configure image capture with conservative settings for Samsung compatibility
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(targetImageResolution)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(90) // Slightly lower quality for better compatibility
                .setTargetRotation(android.view.Surface.ROTATION_0)
                .build()
            
            Log.i(TAG, "RGB camera initialized successfully on device: ${android.os.Build.MODEL}")
            emitStatus()
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RGB camera on device: ${android.os.Build.MODEL}", e)
            val errorMessage = when {
                e.message?.contains("camera", ignoreCase = true) == true -> 
                    "Camera hardware access failed: ${e.message}. This may be a Samsung device compatibility issue."
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Camera permission issue: ${e.message}"
                else -> 
                    "Camera initialization failed: ${e.message}. Try restarting the app or device."
            }
            emitError(ErrorType.INITIALIZATION_FAILED, errorMessage)
            return@withContext false
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean = withContext(Dispatchers.Main) {
        try {
            if (_isRecording.get()) {
                Log.w(TAG, "RGB camera already recording")
                return@withContext true
            }
            
            // Re-check camera permission before starting recording
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission lost during recording attempt")
                emitError(ErrorType.PERMISSION_DENIED, "Camera permission required. Please grant permission and try again.")
                return@withContext false
            }
            
            this@RgbCameraRecorder.sessionDirectory = sessionDirectory
            recordingStartTime = System.nanoTime()
            
            // Create output files
            videoFile = File(sessionDirectory, VIDEO_FILENAME)
            imagesDirectory = File(sessionDirectory, IMAGES_SUBDIRECTORY).apply { mkdirs() }
            
            // Verify we have a video capture instance
            val videoCapture = this@RgbCameraRecorder.videoCapture
            if (videoCapture == null) {
                Log.e(TAG, "VideoCapture not initialized")
                emitError(ErrorType.RECORDING_FAILED, "Camera not properly initialized. Please restart the app.")
                return@withContext false
            }
            
            // Start video recording
            val mediaStoreOutput = FileOutputOptions.Builder(videoFile!!).build()
            recording = videoCapture.output
                ?.prepareRecording(context, mediaStoreOutput)
                ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    handleVideoRecordEvent(recordEvent)
                }
            
            if (recording == null) {
                Log.e(TAG, "Failed to start video recording - recording object is null")
                emitError(ErrorType.RECORDING_FAILED, "Failed to start video recording. Camera may be in use by another app.")
                return@withContext false
            }
            
            // Bind camera with both video and image capture
            bindCamera()
            
            // Start periodic image capture (if available)
            startImageCapture()
            
            _isRecording.set(true)
            frameCount.set(0)
            
            Log.i(TAG, "RGB camera recording started on device: ${android.os.Build.MODEL}")
            emitStatus()
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RGB camera recording on device: ${android.os.Build.MODEL}", e)
            val errorMessage = when {
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Camera permission error: ${e.message}"
                e.message?.contains("busy", ignoreCase = true) == true ->
                    "Camera is busy or in use by another app. Please close other camera apps and try again."
                e.message?.contains("hardware", ignoreCase = true) == true ->
                    "Camera hardware error. Try restarting the device."
                else ->
                    "Failed to start camera recording: ${e.message}. This may be a Samsung device compatibility issue."
            }
            emitError(ErrorType.RECORDING_FAILED, errorMessage)
            return@withContext false
        }
    }

    private suspend fun bindCamera() = withContext(Dispatchers.Main) {
        try {
            cameraProvider?.unbindAll()
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Create preview for monitoring (optional) - use lower resolution for Samsung compatibility
            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()
            
            // Try to bind use cases with fallback for Samsung devices
            try {
                // Attempt full binding with all use cases
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture,
                    imageCapture
                )
                Log.i(TAG, "Successfully bound all camera use cases")
                
            } catch (e: IllegalArgumentException) {
                // Samsung devices may not support all use cases simultaneously
                Log.w(TAG, "Failed to bind all use cases, trying video-only mode", e)
                
                // Fallback to video-only mode
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                
                // Disable image capture for this session
                imageCapture = null
                Log.w(TAG, "Running in video-only mode due to device limitations")
                emitError(ErrorType.INITIALIZATION_FAILED, 
                    "Device does not support simultaneous video and image capture. Running video-only mode.", 
                    isRecoverable = true)
            }
            
            // Configure auto-focus and exposure
            camera?.cameraControl?.apply {
                enableTorch(false) // Ensure flash is off initially
                
                // Set conservative camera settings for Samsung devices
                try {
                    // Enable auto-focus if available
                    setZoomRatio(1.0f)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set camera controls", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera on device: ${android.os.Build.MODEL}", e)
            val errorMessage = when {
                e.message?.contains("use case", ignoreCase = true) == true ->
                    "Camera configuration not supported on this Samsung device. Try using a simpler camera mode."
                e.message?.contains("concurrent", ignoreCase = true) == true ->
                    "Multiple camera streams not supported. Try single-stream mode."
                else ->
                    "Camera binding failed: ${e.message}. Device may need a restart."
            }
            emitError(ErrorType.INITIALIZATION_FAILED, errorMessage)
        }
    }

    private fun startImageCapture() {
        imageCapturJob = recordingScope.launch {
            while (_isRecording.get() && isActive) {
                if (imageCapture != null) {
                    captureAnalysisFrame()
                } else {
                    // Skip image capture if not available (video-only mode)
                    Log.d(TAG, "Skipping image capture - video-only mode active")
                }
                delay(IMAGE_CAPTURE_INTERVAL_MS)
            }
        }
    }

    private suspend fun captureAnalysisFrame() {
        try {
            val imageCapture = this.imageCapture
            if (imageCapture == null) {
                Log.d(TAG, "Image capture not available - running in video-only mode")
                return
            }
            
            val timestamp = System.nanoTime()
            val frameNumber = frameCount.incrementAndGet()
            val imageFile = File(imagesDirectory, "frame_${frameNumber}_${timestamp}.jpg")
            
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile)
                .build()
            
            withContext(Dispatchers.Main) {
                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Analysis frame saved: ${imageFile.name}")
                            recordingScope.launch { emitStatus() }
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            Log.w(TAG, "Failed to capture analysis frame - this is normal on some Samsung devices", exception)
                            // Don't treat this as a critical error for Samsung devices
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Image capture error - continuing with video recording", e)
        }
    }

    private fun handleVideoRecordEvent(recordEvent: VideoRecordEvent) {
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Log.i(TAG, "Video recording started")
            }
            is VideoRecordEvent.Finalize -> {
                if (recordEvent.hasError()) {
                    Log.e(TAG, "Video recording failed: ${recordEvent.cause}")
                    recordingScope.launch {
                        emitError(ErrorType.RECORDING_FAILED, "Video recording error: ${recordEvent.cause?.message}")
                    }
                } else {
                    Log.i(TAG, "Video recording completed successfully")
                }
            }
            is VideoRecordEvent.Status -> {
                // Update recording statistics
                recordingScope.launch { emitStatus() }
            }
        }
    }

    override suspend fun stopRecording(): Boolean {
        try {
            if (!_isRecording.get()) {
                Log.w(TAG, "RGB camera not recording")
                return true
            }
            
            // Stop image capture
            imageCapturJob?.cancel()
            
            // Stop video recording
            recording?.stop()
            recording = null
            
            _isRecording.set(false)
            
            Log.i(TAG, "RGB camera recording stopped")
            emitStatus()
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop RGB camera recording", e)
            emitError(ErrorType.RECORDING_FAILED, "Failed to stop recording: ${e.message}")
            return false
        }
    }

    override suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String>) {
        try {
            // Create sync marker file in images directory
            val syncFile = File(imagesDirectory, "sync_${markerType}_${timestampNs}.txt")
            syncFile.writeText("marker_type=$markerType\ntimestamp_ns=$timestampNs\n" + 
                metadata.map { "${it.key}=${it.value}" }.joinToString("\n"))
            
            Log.i(TAG, "Sync marker added: $markerType at $timestampNs")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "Sync marker failed: ${e.message}")
        }
    }

    override suspend fun cleanup() {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }
            
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            recordingScope.cancel()
            
            Log.i(TAG, "RGB camera cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = _statusFlow.asSharedFlow()
    override fun getErrorFlow(): Flow<SensorError> = _errorFlow.asSharedFlow()

    override fun getRecordingStats(): com.topdon.tc001.sensors.RecordingStats {
        val currentTime = System.nanoTime()
        val sessionDuration = if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L
        
        return com.topdon.tc001.sensors.RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = frameCount.get(),
            averageDataRate = if (sessionDuration > 0) frameCount.get() * 1000.0 / sessionDuration else 0.0,
            droppedSamples = 0L, // CameraX handles frame drops internally
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = getSyncMarkerCount(),
            lastSampleTimestampNs = currentTime
        )
    }

    private fun calculateStorageUsed(): Double {
        val videoSize = videoFile?.length() ?: 0L
        val imagesSize = imagesDirectory?.listFiles()?.sumOf { it.length() } ?: 0L
        return (videoSize + imagesSize) / (1024.0 * 1024.0)
    }

    private fun getSyncMarkerCount(): Int {
        return imagesDirectory?.listFiles { _, name -> name.startsWith("sync_") }?.size ?: 0
    }

    private suspend fun emitStatus() {
        val status = RecordingStatus(
            sensorId = sensorId,
            sensorType = sensorType,
            isRecording = _isRecording.get(),
            samplesRecorded = frameCount.get(),
            currentDataRate = samplingRate,
            storageUsedMB = calculateStorageUsed(),
            timestampNs = System.nanoTime()
        )
        _statusFlow.emit(status)
    }

    private suspend fun emitError(errorType: ErrorType, message: String, isRecoverable: Boolean = true) {
        val error = SensorError(
            sensorId = sensorId,
            sensorType = sensorType,
            errorType = errorType,
            errorMessage = message,
            timestampNs = System.nanoTime(),
            isRecoverable = isRecoverable
        )
        _errorFlow.emit(error)
    }
}