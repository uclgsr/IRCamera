package mpdc4gsr.gsr.recording

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.gsr.capture.SimulationSource
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.GsrSample
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles Shimmer GSR recording and simulation pipelines. Streams samples to disk in CSV format,
 * exposes telemetry for previews, and fulfils FR1/FR5 requirements.
 */
class GsrRecorder(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
    private val shimmerController: ShimmerDeviceController,
    private val simulationSource: SimulationSource,
    private val timelineClock: mpdc4gsr.gsr.session.TimelineClock,
) : Recorder {

    override val kind: RecorderKind = RecorderKind.GSR
    private val _state = MutableStateFlow(RecorderState.IDLE)
    override val state: StateFlow<RecorderState> = _state

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var collectionJob: Job? = null
    private val writers = ConcurrentHashMap<String, BufferedWriter>()
    private lateinit var gsrFolder: File

    @Volatile
    private var sessionContext: RecordingContext? = null

    private val _latestSample = MutableSharedFlow<GsrSample>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val latestSample = _latestSample

    override suspend fun prepare(context: RecordingContext) {
        sessionContext = context
        _state.value = RecorderState.PREPARING
        withContext(dispatcher) {
            gsrFolder = File(context.sessionDirectory, "gsr").apply { mkdirs() }
        }
    }

    override suspend fun start(context: RecordingContext) {
        sessionContext = context
        _state.value = RecorderState.RECORDING
        if (context.simulationEnabled) {
            shimmerController.enableSimulation(context.sessionId)
        }
        collectionJob?.cancel()
        collectionJob =
            scope.launch {
                shimmerController.samples.collectLatest { sample ->
                    handleSample(sample)
                    _latestSample.tryEmit(sample)
                }
            }
    }

    override suspend fun stop() {
        _state.value = RecorderState.STOPPING
        collectionJob?.cancel()
        simulationSource.stop()
        withContext(dispatcher) {
            writers.values.forEach { runCatching { it.flush() } }
            writers.values.forEach { runCatching { it.close() } }
            writers.clear()
        }
        _state.value = RecorderState.IDLE
    }

    override fun close() {
        scope.coroutineContext.cancel()
    }

    private suspend fun handleSample(sample: GsrSample) {
        withContext(dispatcher) {
            val writer = writers.getOrPut(sample.deviceId) {
                val file = File(gsrFolder, "${sample.deviceId}_${sessionContext?.sessionId ?: "session"}.csv")
                file.parentFile?.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                    FileWriter(file, false).use { header ->
                        header.appendLine("timestamp_ms,device_id,microsiemens,resistance_ohms,skin_temp_c,sequence")
                    }
                }
                BufferedWriter(FileWriter(file, true))
            }
            try {
                writer.appendLine(
                    listOf(
                        sample.timestampMillis,
                        sample.deviceId,
                        "%.6f".format(sample.gsrMicrosiemens),
                        sample.resistanceOhms?.let { "%.2f".format(it) } ?: "",
                        sample.skinTemperatureCelsius?.let { "%.2f".format(it) } ?: "",
                        sample.sequenceNumber,
                    ).joinToString(","),
                )
            } catch (io: IOException) {
                _state.value = RecorderState.FAILED
                throw io
            }
        }
    }
}
