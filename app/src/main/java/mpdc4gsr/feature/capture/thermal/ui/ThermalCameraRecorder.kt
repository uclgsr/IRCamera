package mpdc4gsr.feature.capture.thermal.ui

import android.content.Context
import android.os.SystemClock
import com.mpdc4gsr.component.shared.app.event.DeviceEventManager
import com.mpdc4gsr.component.thermal.feature.device.ThermalDeviceConfig
import com.mpdc4gsr.component.thermal.feature.device.TopdonThermalDeviceManager
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.core.common.logging.StructuredLogger
import mpdc4gsr.core.common.logging.StructuredLogger.LogLevel
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.data.utils.TimeManager
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
 * Thin adapter that bridges the legacy thermal component (`thermal`) into the
 * new `SensorRecorder` abstraction. The implementation focuses on lifecycle coordination
 * and status propagation – the heavy lifting remains inside `TopdonThermalDeviceManager`.
 *
 * The vendor SDK interaction lives inside `thermal`, which keeps this class small
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
    private val timeManager = TimeManager.getInstance(applicationContext)

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
                timestampNs = timeManager.getCurrentTimestampNs(),
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
                logger.log(
                    LogLevel.INFO,
                    TAG,
                    "initialize_device",
                    mapOf("sensor_id" to sensorId),
                )
                val result = deviceManager.connect()
                if (result.isSuccess) {
                    DeviceEventManager.emitDeviceConnectionSync(isConnected = true, device = null)
                    logger.log(
                        LogLevel.INFO,
                        TAG,
                        "thermal_device_connected",
                        mapOf("sensor_id" to sensorId),
                    )
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

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        startRecordingInternal(File(sessionDirectory), null)

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata,
    ): Boolean {
        return startRecordingInternal(File(sessionDirectory), sessionMetadata)
    }

    private suspend fun startRecordingInternal(
        dir: File,
        metadata: SessionMetadata?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                sessionDirectory = dir
                sessionMetadata = metadata
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
        logger.log(
            LogLevel.INFO,
            TAG,
            "sync_marker",
            mapOf(
                "marker_type" to markerType,
                "timestamp_ns" to timestampNs,
                "metadata" to metadata,
            ),
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
        logger.log(
            LogLevel.INFO,
            TAG,
            "network_streaming_enabled",
            mapOf("sensor_id" to sensorId),
        )
    }

    fun disableNetworkStreaming() {
        networkServerRef.set(null)
        logger.log(
            LogLevel.INFO,
            TAG,
            "network_streaming_disabled",
            mapOf("sensor_id" to sensorId),
        )
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
                timestampNs = timeManager.getCurrentTimestampNs(),
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
                timestampNs = timeManager.getCurrentTimestampNs(),
                isRecoverable = errorType != ErrorType.HARDWARE_DISCONNECTED,
            )
        errorFlow.emit(error)
        AppLogger.e(
            TAG,
            message,
            IllegalStateException(message),
            component = sensorId,
        )
    }
}



