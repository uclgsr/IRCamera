package mpdc4gsr.feature.gsr.data

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.sensors.api.ErrorType
import mpdc4gsr.core.sensors.api.RecordingStatus
import mpdc4gsr.core.sensors.api.RecordingStats
import mpdc4gsr.core.sensors.api.SensorError
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.sensors.api.SensorRecorder
import mpdc4gsr.core.sensors.gsr.DefaultGsrRecorder
import mpdc4gsr.core.sensors.gsr.GsrRecorder
import mpdc4gsr.core.sensors.gsr.model.DeviceInfo
import mpdc4gsr.core.sensors.gsr.model.GSRSample
import mpdc4gsr.feature.network.data.RecordingController
import java.io.File

/**
 * Thin wrapper around [DefaultGsrRecorder] that keeps feature-level code
 * independent from the heavy core implementation while also managing network
 * streaming and latest-sample exposure for previews.
 */
class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = DEFAULT_SENSOR_ID,
    private val samplingRateHz: Int = DEFAULT_SAMPLING_RATE,
    private val recordingController: RecordingController? = null,
    lifecycleOwnerOverride: LifecycleOwner? = recordingController?.lifecycleOwner,
    private val backendFactory: (Context, LifecycleOwner, String, Int) -> GsrRecorder = { ctx, owner, id, rate ->
        DefaultGsrRecorder(ctx, owner, id, rate)
    },
    private val networkStreamerFactory: (Context, String, RecordingController) -> GSRNetworkStreamer = { ctx, sessionId, controller ->
        GSRNetworkStreamer(ctx, sessionId, controller)
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SensorRecorder {

    companion object {
        private const val DEFAULT_SENSOR_ID = "gsr_shimmer_1"
        private const val DEFAULT_SAMPLING_RATE = 128
        private const val CONNECTED_TOKEN = "connected"
    }

    override val sensorType: String = "GSR Shimmer3"
    override val samplingRate: Double
        get() = backend.samplingRate
    override val isRecording: Boolean
        get() = backend.isRecording

    private val lifecycleOwner: LifecycleOwner =
        lifecycleOwnerOverride ?: createFallbackLifecycleOwner()

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val backend: GsrRecorder =
        backendFactory(context, lifecycleOwner, sensorId, samplingRateHz)

    private val _latestSample = MutableStateFlow<GSRSample?>(null)
    val latestSample: StateFlow<GSRSample?> = _latestSample.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnectedFlow: StateFlow<Boolean> = _isConnected.asStateFlow()
    val isConnected: Boolean
        get() = _isConnected.value

    private var dataCollectionJob: Job? = null
    private var statusMirroringJob: Job? = null
    private var networkStreamer: GSRNetworkStreamer? = null
    private var sessionId: String? = null
    private val networkErrors = MutableSharedFlow<SensorError>(extraBufferCapacity = 8)

    override suspend fun initialize(): Boolean = withContext(dispatcher) {
        val initialized = backend.initialize()
        if (initialized) {
            startStatusMirroring()
        }
        initialized
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        startRecordingInternal(sessionDirectory, null) {
            backend.startRecording(sessionDirectory)
        }

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata
    ): Boolean = startRecordingInternal(sessionDirectory, sessionMetadata) {
        backend.startRecording(sessionDirectory, sessionMetadata)
    }

    private suspend fun startRecordingInternal(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata?,
        startAction: suspend () -> Boolean
    ): Boolean = withContext(dispatcher) {
        if (!startAction()) {
            return@withContext false
        }
        sessionId = sessionMetadata?.sessionId ?: deriveSessionId(sessionDirectory)
        startCollectingSamples()
        startNetworkStreaming()
        true
    }

    private fun startCollectingSamples() {
        dataCollectionJob?.cancel()
        dataCollectionJob = scope.launch {
            backend.dataStream.collect { sample ->
                _latestSample.value = sample
                networkStreamer?.addSample(sample)
            }
        }
    }

    private suspend fun startNetworkStreaming() {
        val controller = recordingController ?: return
        val activeSessionId = sessionId ?: return
        val streamer = networkStreamerFactory(context, activeSessionId, controller)
        networkStreamer = streamer
        val initialized = try {
            streamer.initialize()
        } catch (t: Throwable) {
            emitNetworkError("NETWORK_INIT_FAILED", "Failed to initialise network streaming: ${t.message}")
            false
        }
        if (!initialized) {
            return
        }
        val started = try {
            streamer.startStreaming()
        } catch (t: Throwable) {
            emitNetworkError("NETWORK_START_FAILED", "Failed to start network streaming: ${t.message}")
            false
        }
        if (!started) {
            emitNetworkError("NETWORK_START_FAILED", "GSR network streaming could not be started")
        }
    }

    override suspend fun stopRecording(): Boolean = withContext(dispatcher) {
        val stopped = backend.stopRecording()
        backend.flushAndCloseFiles()
        stopNetworkStreaming()
        dataCollectionJob?.cancelAndJoin()
        dataCollectionJob = null
        sessionId = null
        stopped
    }

    private suspend fun stopNetworkStreaming() {
        val streamer = networkStreamer ?: return
        try {
            streamer.stopStreaming()
        } catch (t: Throwable) {
            emitNetworkError("NETWORK_STOP_FAILED", "Failed to stop network streaming: ${t.message}")
        } finally {
            networkStreamer = null
        }
    }

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>
    ) {
        backend.addSyncMarker(markerType, timestampNs, metadata)
    }

    override suspend fun cleanup() = withContext(dispatcher) {
        try {
            if (backend.isRecording) {
                backend.stopRecording()
            }
            backend.flushAndCloseFiles()
            stopNetworkStreaming()
            dataCollectionJob?.cancelAndJoin()
            statusMirroringJob?.cancelAndJoin()
            backend.cleanup()
        } finally {
            scope.cancel()
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = backend.getStatusFlow()

    override fun getErrorFlow(): Flow<SensorError> =
        merge(backend.getErrorFlow(), networkErrors)

    override fun getRecordingStats(): RecordingStats = backend.getRecordingStats()

    fun getDataStream(): Flow<GSRSample> = backend.dataStream
    fun connectionQualityFlow(): StateFlow<Double> = backend.connectionQuality
    fun deviceStatusFlow(): StateFlow<String> = backend.deviceStatus

    suspend fun startDeviceDiscovery(): Boolean = backend.startDeviceDiscovery()
    fun getDiscoveredDevices(): List<DeviceInfo> = backend.getDiscoveredDevices()
    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = backend.connectToDevice(deviceInfo)
    suspend fun disconnectDevice(): Boolean = backend.disconnectDevice()

    fun currentGsrValue(): Double? = latestSample.value?.gsrMicrosiemens

    private fun startStatusMirroring() {
        statusMirroringJob?.cancel()
        statusMirroringJob = scope.launch {
            backend.deviceStatus.collect { status ->
                _isConnected.value = status.contains(CONNECTED_TOKEN, ignoreCase = true)
            }
        }
    }

    private suspend fun emitNetworkError(type: String, message: String) {
        networkErrors.emit(
            SensorError(
                sensorId = sensorId,
                sensorType = sensorType,
                errorType = ErrorType.DEVICE_ERROR,
                errorMessage = "$type: $message",
                timestampNs = System.nanoTime(),
                isRecoverable = true
            )
        )
    }

    private fun deriveSessionId(sessionDirectory: String): String {
        val folderName = File(sessionDirectory).name
        return folderName.ifBlank { "gsr_session_${System.currentTimeMillis()}" }
    }

    private fun createFallbackLifecycleOwner(): LifecycleOwner {
        return object : LifecycleOwner {
            private val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }

            override val lifecycle: Lifecycle
                get() = registry
        }
    }
}
