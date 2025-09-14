package com.topdon.tc001.sensors.rgb

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.opencsv.CSVWriter
import com.topdon.tc001.sensors.*
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
    private val lifecycleOwner: LifecycleOwner
) : SensorRecorder {

    companion object {
        private const val TAG = "RgbCameraRecorder"

        private const val VIDEO_WIDTH = 1920
        private const val VIDEO_HEIGHT = 1080
        private const val VIDEO_FPS = 30
        private const val IMAGE_WIDTH = 4000  // Samsung S22 max resolution
        private const val IMAGE_HEIGHT = 3000

        private const val VIDEO_BITRATE = 8_000_000  // 8 Mbps for high quality
        private const val AUDIO_BITRATE = 128_000    // 128 kbps for audio
    }

    override val sensorId: String = "rgb_camera_${System.currentTimeMillis()}"
    override val sensorType: String = "RGB_Camera"
    override val samplingRate: Double = VIDEO_FPS.toDouble()

    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var activeRecording: Recording? = null

    private val statusFlow = MutableStateFlow(createInitialStatus())
    private val errorFlow = MutableSharedFlow<SensorError>()

    private var sessionDirectory: String = ""
    private var videoFile: File? = null
    private var csvWriter: CSVWriter? = null
    private var csvFile: File? = null

    private val samplesRecorded = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private var droppedFrames = AtomicLong(0)

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Initializing RGB camera recorder")

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()

            setupCamera()

            Log.i(TAG, "RGB camera recorder initialized successfully")
            updateStatus(isInitialized = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RGB camera recorder", e)
            emitError(ErrorType.INITIALIZATION_FAILED, "Camera initialization failed: ${e.message}")
            false
        }
    }

    private suspend fun setupCamera() = withContext(Dispatchers.Main) {
        try {

            val deviceModel = android.os.Build.MODEL
            val manufacturer = android.os.Build.MANUFACTURER
            Log.d(TAG, "Device model logging for debugging: $manufacturer $deviceModel")

            val isSamsungDevice = manufacturer.equals("samsung", ignoreCase = true)
            Log.d(TAG, "Samsung device compatibility detected: $isSamsungDevice")

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Conservative settings for compatibility

            if (isSamsungDevice) {
                Log.d(TAG, "Applying Samsung-specific camera optimizations")

                imageCaptureBuilder.setTargetResolution(
                    android.util.Size(
                        1920,
                        1080
                    )
                ) // Reduced resolution for compatibility
            } else {
                imageCaptureBuilder.setTargetResolution(
                    android.util.Size(
                        IMAGE_WIDTH,
                        IMAGE_HEIGHT
                    )
                )
            }

            imageCapture = imageCaptureBuilder.build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.unbindAll()

            try {
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture,
                    imageCapture
                )
                Log.i(TAG, "Camera use cases bound successfully")
            } catch (e: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Samsung CameraX IllegalArgumentException handling: Failed to bind with image capture, trying video-only fallback mode",
                    e
                )

                try {
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        videoCapture
                    )

                    imageCapture = null
                    Log.w(
                        TAG,
                        "Image capture disabled for compatibility - using fallback video only mode"
                    )
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Even fallback video only mode failed", fallbackException)
                    throw fallbackException
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Samsung CameraX exception handling: Failed to bind with image capture, trying video-only fallback mode",
                    e
                )

                try {
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        videoCapture
                    )

                    imageCapture = null
                    Log.w(
                        TAG,
                        "Image capture disabled for compatibility - using fallback video only mode"
                    )
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Even fallback video only mode failed", fallbackException)
                    throw fallbackException
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup camera", e)
            throw e
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

            setupOutputFiles()

            startVideoRecording()

            initializeCsvWriter()

            _isRecording.set(true)
            sessionStartTime.set(System.nanoTime())
            samplesRecorded.set(0)
            droppedFrames.set(0)

            Log.i(TAG, "RGB camera recording started in: $sessionDirectory")
            updateStatus(isRecording = true)

            startPeriodicImageCapture()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RGB camera recording", e)
            emitError(ErrorType.RECORDING_FAILED, "Failed to start recording: ${e.message}")
            false
        }
    }

    private fun setupOutputFiles() {

        val timestamp = System.currentTimeMillis()
        videoFile = File(sessionDirectory, "rgb_video_${timestamp}.mp4")

        csvFile = File(sessionDirectory, "rgb_timestamps_${timestamp}.csv")
    }

    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
    private suspend fun startVideoRecording() = withContext(Dispatchers.Main) {
        val videoCapture = this@RgbCameraRecorder.videoCapture
            ?: throw IllegalStateException("VideoCapture not initialized")

        val videoFile = this@RgbCameraRecorder.videoFile
            ?: throw IllegalStateException("Video file not configured")

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {

                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                handleRecordingEvent(recordEvent)
            }
    }

    private fun handleRecordingEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Video recording started")
            }

            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    Log.e(TAG, "Video recording error: ${event.error}")
                    recordingScope.launch {
                        emitError(
                            ErrorType.RECORDING_FAILED,
                            "Video recording failed: ${event.cause?.message}"
                        )
                    }
                } else {
                    Log.i(TAG, "Video recording completed successfully")
                }
            }

            is VideoRecordEvent.Status -> {

                val currentTime = System.nanoTime()
                lastFrameTime.set(currentTime)
                samplesRecorded.incrementAndGet()

                recordingScope.launch {
                    updateStatus()
                }
            }

            is VideoRecordEvent.Pause -> Log.d(TAG, "Video recording paused")
            is VideoRecordEvent.Resume -> Log.d(TAG, "Video recording resumed")
        }
    }

    private fun initializeCsvWriter() {
        try {
            csvFile?.let { file ->
                csvWriter = CSVWriter(FileWriter(file)).apply {

                    writeNext(
                        arrayOf(
                            "timestamp_ns",
                            "frame_number",
                            "session_time_ms",
                            "sync_marker",
                            "metadata"
                        )
                    )
                    flush()
                }
            }
            Log.d(TAG, "CSV writer initialized for frame timestamps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CSV writer", e)
            throw e
        }
    }

    private fun startPeriodicImageCapture() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {

                    if (imageCapture != null) {
                        captureHighResolutionImage()
                    } else {
                        Log.d(
                            TAG,
                            "Image capture disabled for compatibility - skipping periodic capture"
                        )
                    }
                    delay(1000) // Capture high-res image every second for analysis
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to capture periodic image", e)
                }
            }
        }
    }

    private suspend fun captureHighResolutionImage() = withContext(Dispatchers.Main) {
        val imageCapture = this@RgbCameraRecorder.imageCapture ?: return@withContext

        val timestamp = System.nanoTime()
        val imageFile = File(sessionDirectory, "rgb_image_${timestamp}.jpg")

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    recordingScope.launch {
                        logImageCapture(timestamp, imageFile.name)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Image capture failed", exception)
                }
            }
        )
    }

    private fun logImageCapture(timestampNs: Long, filename: String) {
        try {
            csvWriter?.let { writer ->
                val sessionTimeMs = (timestampNs - sessionStartTime.get()) / 1_000_000
                writer.writeNext(
                    arrayOf(
                        timestampNs.toString(),
                        samplesRecorded.get().toString(),
                        sessionTimeMs.toString(),
                        "image_capture",
                        "filename=$filename"
                    )
                )
                writer.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log image capture", e)
        }
    }

    override suspend fun stopRecording(): Boolean {
        return try {
            if (!_isRecording.get()) {
                Log.w(TAG, "No recording in progress to stop")
                return false
            }

            _isRecording.set(false)

            activeRecording?.stop()
            activeRecording = null

            csvWriter?.close()
            csvWriter = null

            Log.i(TAG, "RGB camera recording stopped")
            updateStatus(isRecording = false)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop RGB camera recording", e)
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
            csvWriter?.let { writer ->
                val sessionTimeMs = (timestampNs - sessionStartTime.get()) / 1_000_000
                val metadataStr = metadata.entries.joinToString(",") { "${it.key}=${it.value}" }

                writer.writeNext(
                    arrayOf(
                        timestampNs.toString(),
                        samplesRecorded.get().toString(),
                        sessionTimeMs.toString(),
                        markerType,
                        metadataStr
                    )
                )
                writer.flush()
            }

            Log.d(TAG, "Sync marker added: $markerType at $timestampNs ns")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "Failed to add sync marker: ${e.message}")
        }
    }

    override suspend fun cleanup() {
        try {

            if (_isRecording.get()) {
                stopRecording()
            }

            withContext(Dispatchers.Main) {
                cameraProvider?.unbindAll()
                cameraProvider = null
            }

            cameraExecutor.shutdown()

            recordingScope.cancel()

            Log.i(TAG, "RGB camera recorder cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = statusFlow.asStateFlow()

    override fun getErrorFlow(): Flow<SensorError> = errorFlow.asSharedFlow()

    override fun getRecordingStats(): RecordingStats {
        val currentTime = System.nanoTime()
        val sessionDuration = if (sessionStartTime.get() > 0) {
            (currentTime - sessionStartTime.get()) / 1_000_000
        } else 0L

        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = samplesRecorded.get(),
            averageDataRate = if (sessionDuration > 0) {
                (samplesRecorded.get() * 1000.0) / sessionDuration
            } else 0.0,
            droppedSamples = droppedFrames.get(),
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = 0, // TODO: Track sync markers
            lastSampleTimestampNs = lastFrameTime.get()
        )
    }

    private fun calculateStorageUsed(): Double {
        var totalBytes = 0L

        videoFile?.let { file ->
            if (file.exists()) {
                totalBytes += file.length()
            }
        }

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
}
