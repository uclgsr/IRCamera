package mpdc4gsr.feature.capture.thermal.ui

import android.content.Context
import android.os.SystemClock
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.module.thermalunified.feature.device.ThermalDeviceConfig
import com.mpdc4gsr.module.thermalunified.feature.device.TopdonThermalDeviceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.ErrorHandler
import mpdc4gsr.core.common.logging.StructuredLogger
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.hardware.api.ErrorType
import mpdc4gsr.core.hardware.api.RecordingStats
import mpdc4gsr.core.hardware.api.RecordingStatus
import mpdc4gsr.core.hardware.api.SensorError
import mpdc4gsr.core.hardware.api.SensorRecorder
import mpdc4gsr.feature.connectivity.data.NetworkServer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin adapter that bridges the legacy thermal module (`thermalunified`) into the
 * new `SensorRecorder` abstraction. The implementation focuses on lifecycle coordination
 * and status propagation – the heavy lifting remains inside `TopdonThermalDeviceManager`.
 *
 * The vendor SDK interaction lives inside `thermalunified`, which keeps this class small
 * and testable while still exposing a familiar API to the rest of the app.
 */
class ThermalCameraRecorder(
    context: Context,
    private val sensorIdParam: String = "thermal_camera",
) : SensorRecorder {
    companion object {
        private const val TAG = "ThermalCameraRecorder"
        private const val DEFAULT_FRAME_RATE_HZ = 9f
        private const val STORAGE_BYTES_PER_SAMPLE = 512L // quick heuristic for stats
    }

    override val sensorId: String = sensorIdParam
    override val sensorType: String = "Thermal Camera"
    override val samplingRate: Double = DEFAULT_FRAME_RATE_HZ.toDouble()

    override val isRecording: Boolean
        get() = isRecordingFlag.get()

    private val applicationContext = context.applicationContext
    private val logger = StructuredLogger.getInstance(applicationContext)
    private val deviceManager = TopdonThermalDeviceManager(applicationContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusFlow =
        MutableStateFlow(
            RecordingStatus(
                sensorId = sensorId,
                sensorType = sensorType,
                isRecording = false,
                samplesRecorded = 0,
                currentDataRate = 0.0,
                storageUsedMB = 0.0,
                timestampNs = System.nanoTime(),
            ),
        )
    private val errorFlow = MutableSharedFlow<SensorError>(extraBufferCapacity = 8)

    private val isRecordingFlag = AtomicBoolean(false)
    private val sampleCounter = AtomicLong(0)
    private var statusPollingJob: Job? = null
    private var sessionDirectory: File? = null
    private var sessionMetadata: SessionMetadata? = null
    private var recordingStartElapsedMs: Long = 0L

    private var latestStats =
        RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = 0,
            totalSamplesRecorded = 0,
            averageDataRate = 0.0,
            droppedSamples = 0,
            storageUsedMB = 0.0,
            syncMarkersCount = 0,
            lastSampleTimestampNs = 0,
        )

    private val networkServerRef = AtomicReference<NetworkServer?>(null)

    override suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                logger.i(TAG, "Initialising Topdon thermal device manager")
                val result = deviceManager.connect()
                if (result.isSuccess) {
                    DeviceEventManager.broadcastThermalCameraConnected(applicationContext)
                    true
                } else {
                    emitError("Thermal device connect failed", ErrorType.INITIALIZATION_FAILED)
                    false
                }
            }.getOrElse { error ->
                emitError("Initialisation exception: ${error.message}", ErrorType.INITIALIZATION_FAILED)
                false
            }
        }

    override suspend fun startRecording(sessionDirectory: String): Boolean = startRecording(sessionDirectory, sessionMetadata = null)

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata,
    ): Boolean {
        this.sessionMetadata = sessionMetadata
        return startRecordingInternal(File(sessionDirectory))
    }

    private suspend fun startRecordingInternal(dir: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                sessionDirectory = dir
                recordingStartElapsedMs = SystemClock.elapsedRealtime()
                sampleCounter.set(0)
                val config =
                    ThermalDeviceConfig(
                        requestedFrameRateHz = DEFAULT_FRAME_RATE_HZ,
                    )
                val result = deviceManager.startStream(config)
                if (result.isSuccess) {
                    isRecordingFlag.set(true)
                    startStatusPolling()
                    updateStatus(isRecording = true)
                    true
                } else {
                    emitError("Thermal stream failed to start", ErrorType.OPERATION_FAILED)
                    false
                }
            }.getOrElse { error ->
                emitError("Start recording error: ${error.message}", ErrorType.OPERATION_FAILED)
                false
            }
        }

    override suspend fun stopRecording(): Boolean =
        withContext(Dispatchers.IO) {
            if (!isRecording) return@withContext true
            runCatching {
                deviceManager.stopStream()
                isRecordingFlag.set(false)
                stopStatusPolling()
                updateStatus(isRecording = false)
                true
            }.getOrElse { error ->
                emitError("Stop recording error: ${error.message}", ErrorType.OPERATION_FAILED)
                false
            }
        }

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>,
    ) {
        // The underlying SDK has no explicit sync marker support.
        // We log the event so that downstream tooling can align timelines.
        logger.i(
            TAG,
            "Sync marker received: type=$markerType timestamp=$timestampNs metadata=$metadata",
        )
    }

    override suspend fun cleanup() {
        runCatching { stopRecording() }
        runCatching { deviceManager.disconnect() }
        stopStatusPolling()
        scope.cancel()
        sessionDirectory = null
        sessionMetadata = null
    }

    override fun getStatusFlow() = statusFlow.asStateFlow()

    override fun getErrorFlow() = errorFlow.asSharedFlow()

    override fun getRecordingStats(): RecordingStats =
        latestStats.copy(
            sessionDurationMs =
                if (isRecording) {
                    (SystemClock.elapsedRealtime() - recordingStartElapsedMs)
                } else {
                    latestStats.sessionDurationMs
                },
            totalSamplesRecorded = sampleCounter.get(),
            averageDataRate = if (isRecording) samplingRate else 0.0,
            lastSampleTimestampNs = statusFlow.value.timestampNs,
            storageUsedMB = computeStorageUsageMb(sampleCounter.get()),
        )

    fun enableNetworkStreaming(server: NetworkServer) {
        networkServerRef.set(server)
        logger.i(TAG, "Thermal network streaming enabled")
    }

    fun disableNetworkStreaming() {
        networkServerRef.set(null)
        logger.i(TAG, "Thermal network streaming disabled")
    }

    private fun startStatusPolling() {
        stopStatusPolling()
        statusPollingJob =
            scope.launch {
                while (isActive) {
                    val samples = sampleCounter.addAndGet(DEFAULT_FRAME_RATE_HZ.toLong())
                    updateStatus(isRecording = true, samplesRecorded = samples)
                    delay(1_000L)
                }
            }
    }

    private fun stopStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = null
    }

    private fun updateStatus(
        isRecording: Boolean,
        samplesRecorded: Long = sampleCounter.get(),
    ) {
        val status =
            RecordingStatus(
                sensorId = sensorId,
                sensorType = sensorType,
                isRecording = isRecording,
                samplesRecorded = samplesRecorded,
                currentDataRate = if (isRecording) samplingRate else 0.0,
                storageUsedMB = computeStorageUsageMb(samplesRecorded),
                timestampNs = System.nanoTime(),
            )
        statusFlow.value = status
        latestStats =
            latestStats.copy(
                sessionDurationMs =
                    if (isRecording) {
                        (SystemClock.elapsedRealtime() - recordingStartElapsedMs)
                    } else {
                        latestStats.sessionDurationMs
                    },
                totalSamplesRecorded = samplesRecorded,
                averageDataRate = if (isRecording) samplingRate else 0.0,
                storageUsedMB = computeStorageUsageMb(samplesRecorded),
                lastSampleTimestampNs = status.timestampNs,
            )
    }

    private fun computeStorageUsageMb(samplesRecorded: Long): Double =
        (samplesRecorded * STORAGE_BYTES_PER_SAMPLE).toDouble() / (1024 * 1024).toDouble()

    private suspend fun emitError(
        message: String,
        errorType: ErrorType,
    ) {
        val error =
            SensorError(
                sensorId = sensorId,
                sensorType = sensorType,
                errorType = errorType,
                errorMessage = message,
                timestampNs = System.nanoTime(),
                isRecoverable = errorType != ErrorType.HARDWARE_DISCONNECTED,
            )
        errorFlow.emit(error)
        ErrorHandler.default().handle(IllegalStateException(message))
    }
}
