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
import com.topdon.tc001.data.SessionMetadata
import com.topdon.tc001.sensors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple RGB Camera Recorder for MVP
 */
class SimpleRgbCameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null
) : SensorRecorder {

    companion object {
        private const val TAG = "SimpleRgbCamera"
    }

    // Basic recording state
    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    override val sensorId: String = "RGB_Camera"
    override val sensorType: String = "Camera"

    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var activeRecording: Recording? = null
    private var sessionDirectory: String = ""
    
    // Flow emissions
    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()
    override val statusFlow: SharedFlow<RecordingStatus> = _statusFlow.asSharedFlow()
    override val errorFlow: SharedFlow<SensorError> = _errorFlow.asSharedFlow()

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            if (!hasCameraPermission()) return@withContext false
            
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            
            // Simple preview setup
            preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()

            // Simple video capture setup
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Simple image capture setup
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()

            // Bind to lifecycle
            bindUseCases()
            
            Log.i(TAG, "Camera initialized successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            return@withContext false
        }
    }

    private fun bindUseCases() {
        val useCases = mutableListOf<UseCase>()
        
        videoCapture?.let { useCases.add(it) }
        imageCapture?.let { useCases.add(it) }
        
        preview?.let { preview ->
            previewView?.let { 
                preview.setSurfaceProvider(it.surfaceProvider)
                useCases.add(preview)
            }
        }

        camera = cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            *useCases.toTypedArray()
        )
    }

    override suspend fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata): Boolean {
        return startRecording(sessionDirectory)
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean {
        if (_isRecording.get()) return true
        
        this.sessionDirectory = sessionDirectory
        
        return try {
            val videoFile = File(sessionDirectory, "video.mp4")
            val outputOptions = FileOutputOptions.Builder(videoFile).build()
            
            activeRecording = videoCapture?.output
                ?.prepareRecording(context, outputOptions)
                ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _isRecording.set(true)
                            Log.i(TAG, "Video recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.set(false)
                            Log.i(TAG, "Video recording finished")
                        }
                    }
                }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            false
        }
    }

    override suspend fun stopRecording(): Boolean {
        return try {
            activeRecording?.stop()
            activeRecording = null
            _isRecording.set(false)
            Log.i(TAG, "Recording stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            false
        }
    }

    override suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String>) {
        // Simple implementation - just log for MVP
        Log.d(TAG, "Sync marker: $markerType at $timestampNs")
    }

    override fun getStatus(): RecordingStatus? {
        return RecordingStatus(
            sensorId = sensorId,
            sensorType = sensorType,
            isRecording = _isRecording.get(),
            samplesRecorded = 0,
            currentDataRate = 0.0,
            storageUsedMB = 0.0,
            timestampNs = System.nanoTime()
        )
    }

    override fun getRecordingStats(): RecordingStats {
        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            totalSamplesRecorded = 0,
            averageDataRate = 0.0,
            storageUsedMB = 0.0,
            recordingDurationMs = 0
        )
    }

    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}