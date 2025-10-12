package mpdc4gsr.gsr.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder as CameraRecorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.session.TimelineClock

@Suppress("UnsafeOptInUsageError")
class VideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val dispatcher: CoroutineDispatcher,
    private val clock: TimelineClock,
) : Recorder {

    override val kind: RecorderKind = RecorderKind.RGB_VIDEO
    private val _state = MutableStateFlow(RecorderState.IDLE)
    override val state: StateFlow<RecorderState> = _state

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var provider: ProcessCameraProvider? = null
    private var capture: VideoCapture<CameraRecorder>? = null
    private var activeRecording: Recording? = null
    private lateinit var videoDirectory: File
    private var recordingContext: RecordingContext? = null

    override suspend fun prepare(context: RecordingContext) {
        recordingContext = context
        _state.value = RecorderState.PREPARING
        videoDirectory = File(context.sessionDirectory, "video_rgb").apply { mkdirs() }
        provider = obtainCameraProvider()
        configureUseCases()
    }

    override suspend fun start(context: RecordingContext) {
        recordingContext = context
        _state.value = RecorderState.RECORDING
        val outputFile = createOutputFile(context.sessionId)
        val videoCapture =
            capture ?: error("VideoCapture not initialised")
        val outputOptions =
            FileOutputOptions.Builder(outputFile)
                .setFileSizeLimit(FILE_SEGMENT_BYTES)
                .build()
        val canRecordAudio =
            !context.simulationEnabled &&
                ContextCompat.checkSelfPermission(
                    this.context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
        val pendingRecording =
            videoCapture.output
                .prepareRecording(context.appContext, outputOptions)
                .let { pending ->
                    if (canRecordAudio) {
                        pending.withAudioEnabled()
                    } else {
                        pending
                    }
                }
        activeRecording =
            pendingRecording.start(mainExecutor) { event ->
                handleRecordEvent(event, context)
            }
    }

    override suspend fun stop() {
        _state.value = RecorderState.STOPPING
        activeRecording?.stop()
        activeRecording = null
        _state.value = RecorderState.IDLE
    }

    override fun close() {
        scope.cancel()
        activeRecording?.close()
        provider?.unbindAll()
        backgroundExecutor.shutdown()
    }

    private suspend fun configureUseCases() {
        val cameraProvider = provider ?: return
        withContext(dispatcher) {
            cameraProvider.unbindAll()
            val recorder =
                CameraRecorder.Builder()
                    .setExecutor(backgroundExecutor)
                    .setQualitySelector(
                        QualitySelector.fromOrderedList(
                            listOf(Quality.FHD, Quality.HD),
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD),
                        ),
                    )
                    .build()
            capture = VideoCapture.withOutput(recorder)
            val preview =
                Preview.Builder()
                    .setTargetResolution(Size(640, 360))
                    .build()
            preview.setSurfaceProvider { request ->
                val surfaceTexture = android.graphics.SurfaceTexture(0).apply {
                    setDefaultBufferSize(request.resolution.width, request.resolution.height)
                }
                val surface = android.view.Surface(surfaceTexture)
                request.provideSurface(surface, backgroundExecutor) {
                    surface.release()
                    surfaceTexture.release()
                }
            }
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture,
            )
        }
    }

    private suspend fun obtainCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (ex: Exception) {
                        continuation.resumeWithException(ex)
                    }
                },
                mainExecutor,
            )
        }

    private fun handleRecordEvent(event: VideoRecordEvent, context: RecordingContext) {
        when (event) {
            is VideoRecordEvent.Status -> {
                if (event.recordingStats.numBytesRecorded >= FILE_SEGMENT_BYTES - SEGMENT_MARGIN_BYTES) {
                    scope.launch { rollSegment(context) }
                }
            }

            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    _state.value = RecorderState.FAILED
                }
            }
        }
    }

    private suspend fun rollSegment(context: RecordingContext) {
        activeRecording?.stop()
        val newFile = createOutputFile(context.sessionId)
        val videoCapture = capture ?: return
        val options = FileOutputOptions.Builder(newFile).build()
        val canRecordAudio =
            !context.simulationEnabled &&
                ContextCompat.checkSelfPermission(
                    this.context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
        val pendingRecording =
            videoCapture.output
                .prepareRecording(context.appContext, options)
                .let { pending ->
                    if (canRecordAudio) {
                        pending.withAudioEnabled()
                    } else {
                        pending
                    }
                }
        activeRecording =
            pendingRecording.start(mainExecutor) { event ->
                handleRecordEvent(event, context)
            }
    }

    private fun createOutputFile(sessionId: String): File {
        val timestamp =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.US)
                .withZone(java.time.ZoneId.systemDefault())
                .format(clock.nowInstant())
        return File(videoDirectory, "${sessionId}_rgb_$timestamp.mp4")
    }

    companion object {
        private const val FILE_SEGMENT_BYTES = 1_000_000_000L
        private const val SEGMENT_MARGIN_BYTES = 32_000_000L
    }
}
